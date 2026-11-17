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

        fun fromHome(
            productHome: Path,
        ): VexLifeLocalAsrProvider =
            WhisperCppCliProvider.observeHome(
                productHome,
            )
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
        val processBuilder =
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
        // The Home-owned provider bundle keeps the exact native dylibs beside whisper-cli.
        // Provider library discovery is concrete realization, not Product/Family meaning.
        processBuilder
            .environment()["DYLD_LIBRARY_PATH"] =
            requireNotNull(
                executablePath.parent,
            )
                .toString()
        val process =
            processBuilder.start()

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

        const val ProviderRootRelativePath =
            "runtime/vexlife-prod/local-asr/whispercpp/v1.9.5"

        const val ExecutableRelativePath =
            ProviderRootRelativePath +
                "/bin/whisper-cli"

        const val ModelRelativePath =
            ProviderRootRelativePath +
                "/models/ggml-base.en.bin"

        const val SourceIdentityRelativePath =
            ProviderRootRelativePath +
                "/provider-source.txt"

        fun observeHome(
            productHome: Path,
        ): WhisperCppCliProvider {
            val home =
                productHome
                    .toAbsolutePath()
                    .normalize()
            val executable =
                home
                    .resolve(
                        ExecutableRelativePath,
                    )
                    .normalize()
            val model =
                home
                    .resolve(
                        ModelRelativePath,
                    )
                    .normalize()
            val sourceIdentity =
                home
                    .resolve(
                        SourceIdentityRelativePath,
                    )
                    .normalize()

            fun insideHome(
                path: Path,
            ): Boolean =
                path.startsWith(
                    home,
                )

            require(
                insideHome(
                    executable,
                ) &&
                    insideHome(
                        model,
                    ) &&
                    insideHome(
                        sourceIdentity,
                    ),
            ) {
                "local ASR provider paths must remain inside Vex Home"
            }

            val sourceSha =
                if (
                    Files.isRegularFile(
                        sourceIdentity,
                        LinkOption.NOFOLLOW_LINKS,
                    )
                ) {
                    runCatching {
                        Files.readString(
                            sourceIdentity,
                            StandardCharsets.UTF_8,
                        )
                            .trim()
                    }
                        .getOrNull()
                } else {
                    null
                }

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
