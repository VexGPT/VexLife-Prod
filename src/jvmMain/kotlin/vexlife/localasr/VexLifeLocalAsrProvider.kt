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

        const val DetailReady = "LOCAL_PROVIDER_READY"
        const val DetailProviderSourceNotCurrent = "PROVIDER_SOURCE_NOT_CURRENT"
        const val DetailProviderExecutableUnavailable = "PROVIDER_EXECUTABLE_UNAVAILABLE"
        const val DetailModelUnavailable = "MODEL_UNAVAILABLE"
        const val DetailModelDigestNotCurrent = "MODEL_DIGEST_NOT_CURRENT"

        fun fromHome(
            productHome: Path,
            selection: VexLifeLocalAsrSelection.Value,
        ): VexLifeLocalAsrProvider =
            WhisperCppCliProvider.observeHome(
                productHome = productHome,
                selection = selection,
            )
    }
}

/**
 * A004 realization: the Product service feeds an exact pinned whisper.cpp executable WAV bytes over
 * stdin. A003 already proved direct JVM/JNA; this process realization is intentionally used only to
 * finish the browser->service->ASR->Composer lived circuit without vendoring the upstream Java
 * binding into Product source first.
 */
internal fun whisperCppCliCommand(
    executablePath: Path,
    modelPath: Path,
    languageCode: String,
): List<String> =
    listOf(
        executablePath.toString(),
        "-m",
        modelPath.toString(),
        "-f",
        "-",
        "-l",
        languageCode,
        "-nt",
        "-np",
        // stdin makes whisper-cli's default output target "-"; explicitly request text output so
        // stdout carries the final transcript instead of only completing inference silently.
        "-otxt",
        "-t",
        "4",
    )

private class WhisperCppCliProvider private constructor(
    private val executable: Path,
    private val model: Path,
    private val sourceIdentity: Path,
    private val modelRef: String,
    private val expectedModelSha256: String,
    private val supportedLanguageCodes: Set<String>,
) : VexLifeLocalAsrProvider {
    override fun current(): VexLifeLocalAsrProvider.Current =
        observeCurrent(
            executable = executable,
            model = model,
            sourceIdentity = sourceIdentity,
            modelRef = modelRef,
            expectedModelSha256 = expectedModelSha256,
        )

    override fun transcribe(
        wavBytes: ByteArray,
        languageCode: String,
    ): VexLifeLocalAsrProvider.Result {
        val observed =
            current()
        require(
            observed.ready,
        ) {
            "selected local ASR provider is not current"
        }
        require(
            languageCode in
                supportedLanguageCodes ||
                (
                    languageCode ==
                        "auto" &&
                        supportedLanguageCodes.size >
                        1
                ),
        ) {
            "selected local ASR model does not support requested language"
        }
        require(
            wavBytes.isNotEmpty(),
        ) {
            "local ASR audio is empty"
        }

        val executablePath =
            executable
        val modelPath =
            model

        val started =
            System.nanoTime()
        val processBuilder =
            ProcessBuilder(
                whisperCppCliCommand(
                    executablePath = executablePath,
                    modelPath = modelPath,
                    languageCode = languageCode,
                )
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
                providerRef = observed.providerRef,
                modelRef = observed.modelRef,
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

        const val ProviderRootRelativePath =
            VexLifeLocalAsrArtifacts.ProviderRootRelativePath

        const val ExecutableRelativePath =
            ProviderRootRelativePath +
                "/bin/whisper-cli"

        const val SourceIdentityRelativePath =
            ProviderRootRelativePath +
                "/provider-source.txt"

        fun observeHome(
            productHome: Path,
            selection: VexLifeLocalAsrSelection.Value,
        ): WhisperCppCliProvider {
            require(
                selection.providerRef ==
                    VexLifeLocalAsrSelection.WhisperCppProviderRef,
            ) {
                "local ASR provider is not realized by whisper.cpp"
            }
            val artifact =
                VexLifeLocalAsrArtifacts.forModelRef(
                    selection.modelRef,
                )
            val option =
                VexLifeLocalAsrSelection.option(
                    selection,
                )
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
                VexLifeLocalAsrArtifacts.modelPath(
                    productHome =
                        home,
                    artifact =
                        artifact,
                )
            val sourceIdentity =
                home
                    .resolve(
                        SourceIdentityRelativePath,
                    )
                    .normalize()

            require(
                listOf(
                    executable,
                    model,
                    sourceIdentity,
                )
                    .all {
                        it.startsWith(
                            home,
                        )
                    },
            ) {
                "local ASR provider paths must remain inside Vex Home"
            }

            return WhisperCppCliProvider(
                executable = executable,
                model = model,
                sourceIdentity = sourceIdentity,
                modelRef = artifact.modelRef,
                expectedModelSha256 = artifact.sha256,
                supportedLanguageCodes = option.languageCodes.toSet(),
            )
        }

        private fun observeCurrent(
            executable: Path,
            model: Path,
            sourceIdentity: Path,
            modelRef: String,
            expectedModelSha256: String,
        ): VexLifeLocalAsrProvider.Current {
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
            ): VexLifeLocalAsrProvider.Current =
                VexLifeLocalAsrProvider.Current(
                    providerRef =
                        VexLifeLocalAsrSelection.WhisperCppProviderRef,
                    modelRef =
                        modelRef,
                    state =
                        VexLifeLocalAsrProvider.StateUnavailable,
                    detailRef = detailRef,
                    sourceRef = sourceSha,
                    modelSha256 = modelSha256,
                )

            if (
                sourceSha !=
                ExpectedProviderSourceSha
            ) {
                return unavailable(
                    VexLifeLocalAsrProvider.DetailProviderSourceNotCurrent,
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
                    VexLifeLocalAsrProvider.DetailProviderExecutableUnavailable,
                )
            }

            if (
                !Files.isRegularFile(
                    model,
                    LinkOption.NOFOLLOW_LINKS,
                )
            ) {
                return unavailable(
                    VexLifeLocalAsrProvider.DetailModelUnavailable,
                )
            }

            val observedModelSha =
                sha256(
                    model,
                )
            if (
                observedModelSha !=
                expectedModelSha256
            ) {
                return unavailable(
                    detailRef =
                        VexLifeLocalAsrProvider.DetailModelDigestNotCurrent,
                    modelSha256 =
                        observedModelSha,
                )
            }

            return VexLifeLocalAsrProvider.Current(
                providerRef =
                    VexLifeLocalAsrSelection.WhisperCppProviderRef,
                modelRef =
                    modelRef,
                state =
                    VexLifeLocalAsrProvider.StateReady,
                detailRef =
                    VexLifeLocalAsrProvider.DetailReady,
                sourceRef =
                    sourceSha,
                modelSha256 =
                    observedModelSha,
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
