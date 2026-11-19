package vexlife.localasr

import java.net.InetAddress
import java.net.ServerSocket
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.security.MessageDigest
import java.time.Duration
import java.util.UUID
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

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

    /** Ensure the selected provider/model realization is loaded before microphone capture proceeds. */
    fun prepare()

    fun transcribe(
        wavBytes: ByteArray,
        languageCode: String,
        composerOutputMode: String,
        timeout: Duration,
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
            runtime: VexLifeLocalAsrRuntime,
        ): VexLifeLocalAsrProvider =
            WhisperCppServerProvider.observeHome(
                productHome = productHome,
                selection = selection,
                runtime = runtime,
            )
    }
}

/**
 * Service-lifetime whisper.cpp realization.
 *
 * Progressive dictation must not spawn a new native process and reload the model for every partial
 * hypothesis. One Product service therefore owns one warm loopback whisper-server process for the
 * currently used speech model. Provider/model/language meaning remains outside this runtime.
 */
internal class VexLifeLocalAsrRuntime : AutoCloseable {
    private data class Running(
        val executablePath: Path,
        val modelRef: String,
        val modelPath: Path,
        val process: Process,
        val port: Int,
    )

    private val http =
        HttpClient
            .newBuilder()
            .connectTimeout(
                Duration.ofSeconds(
                    2,
                ),
            )
            .build()

    private var running: Running? =
        null

    @Synchronized
    fun prepare(
        executable: Path,
        model: Path,
        modelRef: String,
        realization: VexLifeLocalAsrProviderRealization.Value,
    ) {
        val current =
            running
        if (
            current !=
                null &&
            current.executablePath ==
                executable &&
            current.modelRef ==
                modelRef &&
            current.modelPath ==
                model &&
            current.process.isAlive &&
            healthy(
                current.port,
            )
        ) {
            return
        }

        stopRunning()

        val port =
            ephemeralLoopbackPort()
        val processBuilder =
            ProcessBuilder(
                whisperCppServerCommand(
                    executablePath =
                        executable,
                    modelPath =
                        model,
                    port =
                        port,
                )
            )
        processBuilder.directory(
            requireNotNull(
                executable.parent,
            )
                .toFile(),
        )
        realization.libraryPathEnvironmentVariable
            ?.let {
                variable ->
                processBuilder
                    .environment()[variable] =
                    requireNotNull(
                        executable.parent,
                    )
                        .toString()
            }
        processBuilder.redirectOutput(
            ProcessBuilder.Redirect.DISCARD,
        )
        processBuilder.redirectError(
            ProcessBuilder.Redirect.DISCARD,
        )

        val process =
            processBuilder.start()
        val candidate =
            Running(
                executablePath =
                    executable,
                modelRef =
                    modelRef,
                modelPath =
                    model,
                process =
                    process,
                port =
                    port,
            )
        running =
            candidate

        try {
            waitUntilReady(
                candidate,
            )
        } catch (
            error: Throwable,
        ) {
            stopRunning()
            throw error
        }
    }

    @Synchronized
    fun transcribe(
        executable: Path,
        model: Path,
        modelRef: String,
        realization: VexLifeLocalAsrProviderRealization.Value,
        wavBytes: ByteArray,
        languageCode: String,
        composerOutputMode: String,
        timeout: Duration,
    ): String {
        prepare(
            executable =
                executable,
            model =
                model,
            modelRef =
                modelRef,
            realization =
                realization,
        )
        val active =
            requireNotNull(
                running,
            )
        val multipart =
            whisperCppServerMultipart(
                wavBytes =
                    wavBytes,
                languageCode =
                    languageCode,
                translateToEnglish =
                    composerOutputMode ==
                        VexLifeLocalAsrComposerOutput.TranslateToEnglish,
            )
        val request =
            HttpRequest
                .newBuilder(
                    URI.create(
                        "http://127.0.0.1:" +
                            active.port +
                            "/inference",
                    ),
                )
                .timeout(
                    timeout,
                )
                .header(
                    "content-type",
                    multipart.contentType,
                )
                .header(
                    "accept",
                    "application/json",
                )
                .POST(
                    HttpRequest.BodyPublishers.ofByteArray(
                        multipart.body,
                    ),
                )
                .build()
        val response =
            http.send(
                request,
                HttpResponse.BodyHandlers.ofString(
                    StandardCharsets.UTF_8,
                ),
            )
        require(
            response.statusCode() in
                200..299,
        ) {
            "local ASR provider returned HTTP " +
                response.statusCode() +
                ": " +
                response.body()
                    .replace(
                        Regex(
                            "\\s+",
                        ),
                        " ",
                    )
                    .take(
                        320,
                    )
        }

        val transcript =
            requireNotNull(
                Json
                    .parseToJsonElement(
                        response.body(),
                    )
                    .jsonObject["text"],
            ) {
                "local ASR provider response has no transcript"
            }
                .jsonPrimitive
                .content
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

        return transcript
    }

    private fun waitUntilReady(
        candidate: Running,
    ) {
        val deadline =
            System.nanoTime() +
                PrepareTimeout.toNanos()
        while (
            System.nanoTime() <
            deadline
        ) {
            require(
                candidate.process.isAlive,
            ) {
                "local ASR provider exited while loading the selected speech model"
            }
            if (
                healthy(
                    candidate.port,
                )
            ) {
                return
            }
            Thread.sleep(
                100,
            )
        }
        error(
            "local ASR provider did not become ready within " +
                PrepareTimeout.seconds +
                " seconds",
        )
    }

    private fun healthy(
        port: Int,
    ): Boolean =
        runCatching {
            val response =
                http.send(
                    HttpRequest
                        .newBuilder(
                            URI.create(
                                "http://127.0.0.1:" +
                                    port +
                                    "/health",
                            ),
                        )
                        .timeout(
                            HealthTimeout,
                        )
                        .GET()
                        .build(),
                    HttpResponse.BodyHandlers.discarding(),
                )
            response.statusCode() ==
                200
        }
            .getOrDefault(
                false,
            )

    private fun stopRunning() {
        val active =
            running
        running =
            null
        if (
            active ==
                null
        ) {
            return
        }

        active.process.destroy()
        runCatching {
            if (
                !active.process.waitFor(
                    2,
                    java.util.concurrent.TimeUnit.SECONDS,
                )
            ) {
                active.process.destroyForcibly()
                active.process.waitFor(
                    2,
                    java.util.concurrent.TimeUnit.SECONDS,
                )
            }
        }
    }

    override fun close() {
        synchronized(
            this,
        ) {
            stopRunning()
        }
    }

    companion object {
        private val PrepareTimeout =
            Duration.ofSeconds(
                45,
            )
        private val HealthTimeout =
            Duration.ofMillis(
                500,
            )

        private fun ephemeralLoopbackPort(): Int =
            ServerSocket(
                0,
                1,
                InetAddress.getLoopbackAddress(),
            )
                .use {
                    it.localPort
                }
    }
}

internal fun whisperCppServerCommand(
    executablePath: Path,
    modelPath: Path,
    port: Int,
): List<String> =
    listOf(
        executablePath.toString(),
        "-m",
        modelPath.toString(),
        "--host",
        "127.0.0.1",
        "--port",
        port.toString(),
        "-t",
        "4",
        "-nt",
    )

internal data class WhisperCppServerMultipart(
    val contentType: String,
    val body: ByteArray,
)

internal fun whisperCppServerMultipart(
    wavBytes: ByteArray,
    languageCode: String,
    translateToEnglish: Boolean = false,
    boundary: String =
        "vexlife-local-asr-" +
            UUID.randomUUID()
                .toString()
                .replace(
                    "-",
                    "",
                ),
): WhisperCppServerMultipart {
    require(
        wavBytes.isNotEmpty(),
    ) {
        "local ASR audio is empty"
    }
    require(
        boundary.matches(
            Regex(
                "[A-Za-z0-9._-]+",
            ),
        ),
    ) {
        "local ASR multipart boundary is invalid"
    }

    val output =
        java.io.ByteArrayOutputStream()

    fun line(value: String) {
        output.write(
            value.toByteArray(
                StandardCharsets.UTF_8,
            ),
        )
        output.write(
            "\r\n".toByteArray(
                StandardCharsets.US_ASCII,
            ),
        )
    }

    fun field(
        name: String,
        value: String,
    ) {
        line(
            "--$boundary",
        )
        line(
            "Content-Disposition: form-data; name=\"$name\"",
        )
        line(
            "",
        )
        line(
            value,
        )
    }

    line(
        "--$boundary",
    )
    line(
        "Content-Disposition: form-data; name=\"file\"; filename=\"audio.wav\"",
    )
    line(
        "Content-Type: audio/wav",
    )
    line(
        "",
    )
    output.write(
        wavBytes,
    )
    output.write(
        "\r\n".toByteArray(
            StandardCharsets.US_ASCII,
        ),
    )

    field(
        "language",
        languageCode,
    )
    field(
        "response_format",
        "json",
    )
    field(
        "translate",
        translateToEnglish.toString(),
    )
    field(
        "no_timestamps",
        "true",
    )
    field(
        "temperature",
        "0.0",
    )
    field(
        "temperature_inc",
        "0.2",
    )

    line(
        "--$boundary--",
    )

    return WhisperCppServerMultipart(
        contentType =
            "multipart/form-data; boundary=$boundary",
        body =
            output.toByteArray(),
    )
}

private class WhisperCppServerProvider private constructor(
    private val productHome: Path,
    private val realization: VexLifeLocalAsrProviderRealization.Value,
    private val model: Path,
    private val modelRef: String,
    private val expectedModelSha256: String,
    private val supportedLanguageCodes: Set<String>,
    private val runtime: VexLifeLocalAsrRuntime,
) : VexLifeLocalAsrProvider {
    override fun current(): VexLifeLocalAsrProvider.Current =
        observeCurrent(
            productHome =
                productHome,
            realization =
                realization,
            model =
                model,
            modelRef =
                modelRef,
            expectedModelSha256 =
                expectedModelSha256,
        )

    override fun prepare() {
        val provider =
            VexLifeLocalAsrProviderMaterialization.observe(
                productHome =
                    productHome,
                realization =
                    realization,
            )
        require(
            current().ready &&
                provider.ready,
        ) {
            "selected local ASR provider is not current"
        }
        runtime.prepare(
            executable =
                Path.of(
                    provider.executablePath,
                ),
            model =
                model,
            modelRef =
                modelRef,
            realization =
                realization,
        )
    }

    override fun transcribe(
        wavBytes: ByteArray,
        languageCode: String,
        composerOutputMode: String,
        timeout: Duration,
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
            VexLifeLocalAsrComposerOutput.supportedBy(
                modelRef =
                    modelRef,
                mode =
                    composerOutputMode,
            ),
        ) {
            "selected local ASR model does not support requested Composer output"
        }

        val provider =
            VexLifeLocalAsrProviderMaterialization.observe(
                productHome =
                    productHome,
                realization =
                    realization,
            )
        require(
            provider.ready,
        ) {
            "selected local ASR provider is not current"
        }

        val started =
            System.nanoTime()
        val transcript =
            runtime.transcribe(
                executable =
                    Path.of(
                        provider.executablePath,
                    ),
                model =
                    model,
                modelRef =
                    modelRef,
                realization =
                    realization,
                wavBytes =
                    wavBytes,
                languageCode =
                    languageCode,
                composerOutputMode =
                    composerOutputMode,
                timeout =
                    timeout,
            )

        return VexLifeLocalAsrProvider.Result(
            providerRef =
                observed.providerRef,
            modelRef =
                observed.modelRef,
            transcript =
                transcript,
            inferenceMillis =
                (
                    System.nanoTime() -
                        started
                ) /
                    1_000_000L,
        )
    }

    companion object {
        fun observeHome(
            productHome: Path,
            selection: VexLifeLocalAsrSelection.Value,
            runtime: VexLifeLocalAsrRuntime,
        ): WhisperCppServerProvider {
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
            val realization =
                VexLifeLocalAsrProviderRealization.current()
            val model =
                VexLifeLocalAsrArtifacts.modelPath(
                    productHome =
                        home,
                    artifact =
                        artifact,
                )

            require(
                model.startsWith(
                    home,
                ),
            ) {
                "local ASR model path must remain inside Vex Home"
            }

            return WhisperCppServerProvider(
                productHome =
                    home,
                realization =
                    realization,
                model =
                    model,
                modelRef =
                    artifact.modelRef,
                expectedModelSha256 =
                    artifact.sha256,
                supportedLanguageCodes =
                    option.languageCodes.toSet(),
                runtime =
                    runtime,
            )
        }

        private fun observeCurrent(
            productHome: Path,
            realization: VexLifeLocalAsrProviderRealization.Value,
            model: Path,
            modelRef: String,
            expectedModelSha256: String,
        ): VexLifeLocalAsrProvider.Current {
            val provider =
                VexLifeLocalAsrProviderMaterialization.observe(
                    productHome =
                        productHome,
                    realization =
                        realization,
                )

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
                    detailRef =
                        detailRef,
                    sourceRef =
                        provider.sourceSha,
                    modelSha256 =
                        modelSha256,
                )

            if (
                provider.state ==
                    VexLifeLocalAsrProviderMaterialization.State.ABSENT
            ) {
                return unavailable(
                    VexLifeLocalAsrProvider.DetailProviderExecutableUnavailable,
                )
            }

            if (
                provider.sourceSha !=
                    VexLifeLocalAsrProviderRealization.ProviderSourceSha ||
                    !provider.ready
            ) {
                return unavailable(
                    VexLifeLocalAsrProvider.DetailProviderSourceNotCurrent,
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
                    provider.sourceSha,
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
                            1024 *
                                1024,
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
