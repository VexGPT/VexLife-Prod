package vexlife.localasr

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.security.MessageDigest
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

/**
 * Concrete local speech-to-text provider boundary.
 *
 * Provider mechanics own inference only. They do not own VoiceInput State, selection semantics,
 * Composer text, Conversation, or raw-audio retention.
 */
internal interface VexLifeLocalAsrProvider {
    data class Current(
        val providerRef: String,
        val modelRef: String,
        val state: String,
        val detailRef: String,
        val sourceRef: String?,
        val modelSha256: String?,
    ) {
        val ready: Boolean
            get() =
                state ==
                    StateReady
    }

    data class Result(
        val providerRef: String,
        val modelRef: String,
        val transcript: String,
        val inferenceMillis: Long,
    )

    fun current(): Current

    fun transcribe(
        wavBytes: ByteArray,
        languageCode: String,
    ): Result

    companion object {
        const val StateReady = "READY"
        const val StateUnavailable = "UNAVAILABLE"

        fun fromEnvironment(): VexLifeLocalAsrProvider =
            WhisperCppCliProvider.observeEnvironment()
    }
}

/**
 * A004 realization: the Product service feeds an exact pinned whisper.cpp executable WAV bytes over
 * stdin. A003 already proved direct JVM/JNA; this process realization is intentionally used only to
 * finish the browser->service->ASR->Composer lived circuit without vendoring the upstream Java
 * binding into Product source first.
 */
private class WhisperCppCliProvider private constructor(
    private val current: VexLifeLocalAsrProvider.Current,
    private val executable: Path?,
    private val model: Path?,
) : VexLifeLocalAsrProvider {
    override fun current(): VexLifeLocalAsrProvider.Current =
        current

    override fun transcribe(
        wavBytes: ByteArray,
        languageCode: String,
    ): VexLifeLocalAsrProvider.Result {
        require(
            current.ready,
        ) {
            "selected local ASR provider is not current"
        }
        require(
            languageCode ==
                "en",
        ) {
            "selected local ASR model supports English only"
        }
        require(
            wavBytes.isNotEmpty(),
        ) {
            "local ASR audio is empty"
        }

        val executablePath =
            requireNotNull(
                executable,
            )
        val modelPath =
            requireNotNull(
                model,
            )

        val started =
            System.nanoTime()
        val process =
            ProcessBuilder(
                executablePath.toString(),
                "-m",
                modelPath.toString(),
                "-f",
                "-",
                "-l",
                "en",
                "-nt",
                "-np",
                "-t",
                "4",
            )
                .start()

        val stdout =
            CompletableFuture.supplyAsync {
                process.inputStream
                    .readBytes()
                    .toString(
                        StandardCharsets.UTF_8,
                    )
            }
        val stderr =
            CompletableFuture.supplyAsync {
                process.errorStream
                    .readBytes()
                    .toString(
                        StandardCharsets.UTF_8,
                    )
            }

        try {
            process.outputStream.use {
                output ->
                output.write(
                    wavBytes,
                )
            }

            if (
                !process.waitFor(
                    45,
                    TimeUnit.SECONDS,
                )
            ) {
                process.destroyForcibly()
                throw IllegalStateException(
                    "local ASR inference timed out",
                )
            }

            val standardOutput =
                stdout
                    .get(
                        5,
                        TimeUnit.SECONDS,
                    )
            val standardError =
                stderr
                    .get(
                        5,
                        TimeUnit.SECONDS,
                    )

            require(
                process.exitValue() ==
                    0,
            ) {
                "local ASR provider exited " +
                    process.exitValue() +
                    ": " +
                    standardError
                        .lineSequence()
                        .toList()
                        .takeLast(
                            3,
                        )
                        .joinToString(
                            " ",
                        )
                        .take(
                            480,
                        )
            }

            val transcript =
                standardOutput
                    .lineSequence()
                    .map(
                        String::trim,
                    )
                    .filter(
                        String::isNotEmpty,
                    )
                    .joinToString(
                        " ",
                    )
                    .replace(
                        Regex(
                            "\\s+",
                        ),
                        " ",
                    )
                    .trim()

            require(
                transcript.isNotEmpty(),
            ) {
                "local ASR provider returned no transcript"
            }

            return VexLifeLocalAsrProvider.Result(
                providerRef = current.providerRef,
                modelRef = current.modelRef,
                transcript = transcript,
                inferenceMillis =
                    (System.nanoTime() - started) /
                        1_000_000L,
            )
        } finally {
            if (
                process.isAlive
            ) {
                process.destroyForcibly()
            }
        }
    }

    companion object {
        private const val ExpectedProviderSourceSha =
            "d1be6fde11ac6e0407606b4e42fe72d34add8037"

        private const val ExpectedModelSha256 =
            "a03779c86df3323075f5e796cb2ce5029f00ec8869eee3fdfb897afe36c6d002"

        private const val ExecutableEnvironment =
            "VEXLIFE_LOCAL_ASR_WHISPER_CLI"

        private const val ModelEnvironment =
            "VEXLIFE_LOCAL_ASR_WHISPER_MODEL"

        private const val SourceEnvironment =
            "VEXLIFE_LOCAL_ASR_WHISPER_SOURCE_SHA"

        fun observeEnvironment(): WhisperCppCliProvider {
            val executable =
                System.getenv(
                    ExecutableEnvironment,
                )
                    ?.takeIf(
                        String::isNotBlank,
                    )
                    ?.let(
                        Path::of,
                    )
                    ?.toAbsolutePath()
                    ?.normalize()
            val model =
                System.getenv(
                    ModelEnvironment,
                )
                    ?.takeIf(
                        String::isNotBlank,
                    )
                    ?.let(
                        Path::of,
                    )
                    ?.toAbsolutePath()
                    ?.normalize()
            val sourceSha =
                System.getenv(
                    SourceEnvironment,
                )
                    ?.trim()

            fun unavailable(
                detailRef: String,
                modelSha256: String? =
                    null,
            ): WhisperCppCliProvider =
                WhisperCppCliProvider(
                    current =
                        VexLifeLocalAsrProvider.Current(
                            providerRef =
                                VexLifeLocalAsrSelection.WhisperCppProviderRef,
                            modelRef =
                                VexLifeLocalAsrSelection.WhisperBaseEnglishModelRef,
                            state =
                                VexLifeLocalAsrProvider.StateUnavailable,
                            detailRef = detailRef,
                            sourceRef = sourceSha,
                            modelSha256 = modelSha256,
                        ),
                    executable = executable,
                    model = model,
                )

            if (
                sourceSha !=
                ExpectedProviderSourceSha
            ) {
                return unavailable(
                    "PROVIDER_SOURCE_NOT_CURRENT",
                )
            }

            if (
                executable ==
                    null ||
                !Files.isRegularFile(
                    executable,
                    LinkOption.NOFOLLOW_LINKS,
                ) ||
                !Files.isExecutable(
                    executable,
                )
            ) {
                return unavailable(
                    "PROVIDER_EXECUTABLE_UNAVAILABLE",
                )
            }

            if (
                model ==
                    null ||
                !Files.isRegularFile(
                    model,
                    LinkOption.NOFOLLOW_LINKS,
                )
            ) {
                return unavailable(
                    "MODEL_UNAVAILABLE",
                )
            }

            val observedModelSha =
                sha256(
                    model,
                )
            if (
                observedModelSha !=
                ExpectedModelSha256
            ) {
                return unavailable(
                    detailRef =
                        "MODEL_DIGEST_NOT_CURRENT",
                    modelSha256 =
                        observedModelSha,
                )
            }

            return WhisperCppCliProvider(
                current =
                    VexLifeLocalAsrProvider.Current(
                        providerRef =
                            VexLifeLocalAsrSelection.WhisperCppProviderRef,
                        modelRef =
                            VexLifeLocalAsrSelection.WhisperBaseEnglishModelRef,
                        state =
                            VexLifeLocalAsrProvider.StateReady,
                        detailRef =
                            "LOCAL_PROVIDER_READY",
                        sourceRef =
                            sourceSha,
                        modelSha256 =
                            observedModelSha,
                    ),
                executable = executable,
                model = model,
            )
        }

        private fun sha256(
            path: Path,
        ): String {
            val digest =
                MessageDigest.getInstance(
                    "SHA-256",
                )
            Files.newInputStream(
                path,
            )
                .use {
                    input ->
                    val buffer =
                        ByteArray(
                            1024 * 1024,
                        )
                    while (
                        true
                    ) {
                        val read =
                            input.read(
                                buffer,
                            )
                        if (
                            read <
                            0
                        ) {
                            break
                        }
                        digest.update(
                            buffer,
                            0,
                            read,
                        )
                    }
                }
            return digest
                .digest()
                .joinToString(
                    "",
                ) {
                    "%02x".format(
                        it.toInt() and
                            0xff,
                    )
                }
        }
    }
}

// [VXG RealForever]
