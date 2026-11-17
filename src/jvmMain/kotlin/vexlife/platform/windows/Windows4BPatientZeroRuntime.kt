package vexlife.platform.windows

import java.net.InetSocketAddress
import java.net.Socket
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import vexlife.model.heretic.HereticModelCatalog
import vexlife.model.heretic.HereticModelConfiguration
import vexlife.model.heretic.HereticRuntimeQualification
import vexlife.model.heretic.HereticRuntimeRealization
import vexlife.model.heretic.HereticRuntimeState

/**
 * Temporary Windows runtime carrier for the current Product's existing heretic-4b choice.
 *
 * This is deliberately a lived-proof leaf, not a second model selector or permanent runtime owner.
 * It consumes exact already-materialized files, serves numeric loopback only, qualifies the runtime,
 * and hands the shared Product one ordinary [HereticModelConfiguration.QualifiedRuntimeEvidence].
 */
internal object Windows4BPatientZeroRuntime {
    const val ModelFileName =
        "model-Q4_K_M.gguf"

    const val ModelExpectedBytes =
        2_790_000_800L

    const val ModelSha256 =
        "7b95847ce1684f98d96f13bad2daaeffcfd2f95ea2ff3821ee68943590a10159"

    const val ExistingLlamaServerSha256 =
        "af3e56d6bdb84a9b6bd50f6ad748809370bc29be42d58088041408b2933d80f7"

    val RuntimeReceiptRelativePath: String =
        HereticRuntimeRealization.Windows4BPatientZeroReceiptRelativePath

    private const val Port =
        18_080

    private const val QualificationTimeoutMs =
        120_000L

    private val client =
        HttpClient
            .newBuilder()
            .connectTimeout(
                Duration.ofSeconds(
                    5,
                ),
            )
            .build()

    data class Started(
        val process: Process,
        val modelPath: Path,
        val llamaServerPath: Path,
        val logPath: Path,
        val startedAt: String,
        val evidence: HereticModelConfiguration.QualifiedRuntimeEvidence,
    ) : AutoCloseable {
        override fun close() {
            if (
                process.isAlive
            ) {
                process.destroy()
                if (
                    !process.waitFor(
                        10,
                        TimeUnit.SECONDS,
                    )
                ) {
                    process.destroyForcibly()
                    process.waitFor(
                        10,
                        TimeUnit.SECONDS,
                    )
                }
            }
        }
    }

    fun start(
        modelPath: Path,
        llamaServerPath: Path,
        logPath: Path,
    ): Started {
        requireWindowsX64()

        val model =
            exactFile(
                path = modelPath,
                expectedBytes = ModelExpectedBytes,
                expectedSha256 = ModelSha256,
                label = "current heretic-4b Windows GGUF",
            )
        val server =
            exactFile(
                path = llamaServerPath,
                expectedBytes = null,
                expectedSha256 = ExistingLlamaServerSha256,
                label = "existing qualified llama-server.exe",
            )

        require(
            !portOccupied(),
        ) {
            "Windows 4B patient-zero endpoint 127.0.0.1:" + Port + " is already occupied"
        }

        requireNotNull(
            logPath.parent,
        )
            .let(
                Files::createDirectories,
            )

        val realization =
            HereticRuntimeRealization.Windows4BPatientZero

        val args =
            listOf(
                server.toString(),
                "-m",
                model.toString(),
                "--alias",
                realization.requestModel,
                "--host",
                "127.0.0.1",
                "--port",
                Port.toString(),
                "-ngl",
                "99",
                "--ctx-size",
                "16384",
                "--jinja",
            )

        val process =
            ProcessBuilder(
                args,
            )
                .redirectErrorStream(
                    true,
                )
                .redirectOutput(
                    logPath.toFile(),
                )
                .start()

        try {
            waitForHealth(
                process,
            )
            requireExactModelAlias()
            performQualificationInference()

            val modelEntry =
                HereticModelCatalog.Candidate4B
            val evidence =
                HereticModelConfiguration.QualifiedRuntimeEvidence(
                    runtimeReceiptRef =
                        "receipt.vexlife-prod.windows-4b-patient-zero." +
                            process.pid(),
                    runtimeState =
                        HereticRuntimeState.Qualified,
                    modelRef =
                        modelEntry.modelRef,
                    modelProfileRef =
                        modelEntry.identity.modelProfileRef,
                    artifactRef =
                        realization.artifactRef,
                    artifactContentSetSha256 =
                        realization.artifactContentSetSha256,
                    runtimeAdapterRef =
                        realization.runtimeAdapterRef,
                    runtimeClass =
                        realization.runtimeClass,
                    endpoint =
                        realization.endpoint,
                    requestModel =
                        realization.requestModel,
                    qualification =
                        HereticRuntimeQualification.qualified(),
                )

            return Started(
                process = process,
                modelPath = model,
                llamaServerPath = server,
                logPath = logPath,
                startedAt =
                    Instant
                        .now()
                        .toString(),
                evidence = evidence,
            )
        } catch (
            error: Throwable,
        ) {
            if (
                process.isAlive
            ) {
                process.destroyForcibly()
                process.waitFor(
                    10,
                    TimeUnit.SECONDS,
                )
            }
            throw error
        }
    }

    fun observeCurrent(
        started: Started,
    ): HereticModelConfiguration.QualifiedRuntimeEvidence {
        require(
            started.process.isAlive,
        ) {
            "Windows 4B patient-zero runtime process is not live"
        }
        requireHealth()
        requireExactModelAlias()
        return started.evidence
    }

    private fun requireWindowsX64() {
        require(
            System
                .getProperty(
                    "os.name",
                )
                .lowercase()
                .contains(
                    "windows",
                ),
        ) {
            "Windows 4B patient-zero requires Windows"
        }
        require(
            System
                .getProperty(
                    "os.arch",
                )
                .lowercase() in
                setOf(
                    "amd64",
                    "x86_64",
                    "x64",
                ),
        ) {
            "Windows 4B patient-zero requires one x64 JVM"
        }
    }

    private fun exactFile(
        path: Path,
        expectedBytes: Long?,
        expectedSha256: String,
        label: String,
    ): Path {
        val normalized =
            path
                .toAbsolutePath()
                .normalize()
        require(
            Files.isRegularFile(
                normalized,
            ),
        ) {
            label + " is missing: " + normalized
        }
        expectedBytes?.let {
            require(
                Files.size(
                    normalized,
                ) ==
                    it,
            ) {
                label + " byte length does not match pinned source"
            }
        }
        require(
            sha256(
                normalized,
            ) ==
                expectedSha256,
        ) {
            label + " SHA-256 does not match pinned source"
        }
        return normalized
    }

    private fun portOccupied(): Boolean =
        runCatching {
                Socket().use {
                    socket ->
                    socket.connect(
                        InetSocketAddress(
                            "127.0.0.1",
                            Port,
                        ),
                        300,
                    )
                }
                true
            }
            .getOrElse {
                false
            }

    private fun waitForHealth(
        process: Process,
    ) {
        val deadline =
            System.currentTimeMillis() +
                QualificationTimeoutMs

        var last =
            "not observed"

        while (
            System.currentTimeMillis() <
            deadline
        ) {
            require(
                process.isAlive,
            ) {
                "llama-server exited before Windows patient-zero qualification; see runtime log"
            }

            val observed =
                runCatching {
                    health()
                }

            if (
                observed.getOrNull() ==
                    "ok"
            ) {
                return
            }

            last =
                observed
                    .fold(
                        onSuccess = {
                            it
                        },
                        onFailure = {
                            it.message
                                ?: it::class.simpleName
                                ?: "health failure"
                        },
                    )

            Thread.sleep(
                500,
            )
        }

        error(
            "llama-server did not reach exact health status=ok within " +
                QualificationTimeoutMs +
                " ms; last=" +
                last,
        )
    }

    private fun requireHealth() {
        require(
            health() ==
                "ok",
        ) {
            "Windows 4B patient-zero runtime health is not exact status=ok"
        }
    }

    private fun health(): String {
        val response =
            get(
                "/health",
            )
        require(
            response.statusCode() ==
                200,
        ) {
            "llama-server /health returned HTTP " +
                response.statusCode()
        }
        return Json
            .parseToJsonElement(
                response.body(),
            )
            .jsonObject[
                "status"
            ]
            ?.jsonPrimitive
            ?.contentOrNull
            ?: error(
                "llama-server /health has no status",
            )
    }

    private fun requireExactModelAlias() {
        val response =
            get(
                "/v1/models",
            )
        require(
            response.statusCode() ==
                200,
        ) {
            "llama-server /v1/models returned HTTP " +
                response.statusCode()
        }
        val data =
            Json
                .parseToJsonElement(
                    response.body(),
                )
                .jsonObject[
                    "data"
                ]
                ?.jsonArray
                ?: error(
                    "llama-server /v1/models has no data array",
                )

        val expected =
            HereticRuntimeRealization
                .Windows4BPatientZero
                .requestModel
        val matches =
            data.count {
                element ->
                (
                    element as? JsonObject
                )
                    ?.get(
                        "id",
                    )
                    ?.jsonPrimitive
                    ?.contentOrNull ==
                    expected
            }

        require(
            matches ==
                1,
        ) {
            "llama-server must expose exactly one model id=" +
                expected +
                "; observed=" +
                matches
        }
    }

    private fun performQualificationInference() {
        val realization =
            HereticRuntimeRealization.Windows4BPatientZero

        val body =
            buildJsonObject {
                put(
                    "model",
                    realization.requestModel,
                )
                put(
                    "stream",
                    false,
                )
                put(
                    "max_tokens",
                    16,
                )
                put(
                    "messages",
                    buildJsonArray {
                        add(
                            buildJsonObject {
                                put(
                                    "role",
                                    "user",
                                )
                                put(
                                    "content",
                                    "Reply with exactly VEX_RUNTIME_READY.",
                                )
                            },
                        )
                    },
                )
            }
                .toString()

        val request =
            HttpRequest
                .newBuilder(
                    URI.create(
                        realization.endpoint +
                            "/v1/chat/completions",
                    ),
                )
                .timeout(
                    Duration.ofMillis(
                        QualificationTimeoutMs,
                    ),
                )
                .header(
                    "content-type",
                    "application/json",
                )
                .POST(
                    HttpRequest.BodyPublishers.ofString(
                        body,
                        StandardCharsets.UTF_8,
                    ),
                )
                .build()
        val response =
            client.send(
                request,
                HttpResponse.BodyHandlers.ofString(
                    StandardCharsets.UTF_8,
                ),
            )

        require(
            response.statusCode() in
                200..299,
        ) {
            "llama-server qualification chat returned HTTP " +
                response.statusCode()
        }

        val content =
            Json
                .parseToJsonElement(
                    response.body(),
                )
                .jsonObject[
                    "choices"
                ]
                ?.let {
                    it as? JsonArray
                }
                ?.firstOrNull()
                ?.jsonObject
                ?.get(
                    "message",
                )
                ?.jsonObject
                ?.get(
                    "content",
                )
                ?.jsonPrimitive
                ?.contentOrNull

        require(
            !content.isNullOrBlank(),
        ) {
            "llama-server qualification chat returned no assistant content"
        }
    }

    private fun get(
        path: String,
    ): HttpResponse<String> {
        val endpoint =
            HereticRuntimeRealization
                .Windows4BPatientZero
                .endpoint
        val request =
            HttpRequest
                .newBuilder(
                    URI.create(
                        endpoint +
                            path,
                    ),
                )
                .timeout(
                    Duration.ofSeconds(
                        5,
                    ),
                )
                .GET()
                .build()
        return client.send(
            request,
            HttpResponse.BodyHandlers.ofString(
                StandardCharsets.UTF_8,
            ),
        )
    }

    private fun sha256(
        path: Path,
    ): String {
        val digest =
            MessageDigest.getInstance(
                "SHA-256",
            )

        Files
            .newInputStream(
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

// [VXG RealForever]
