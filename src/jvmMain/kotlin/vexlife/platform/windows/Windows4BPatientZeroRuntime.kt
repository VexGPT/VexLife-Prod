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
import kotlinx.serialization.Serializable
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
import vexlife.model.heretic.HereticRuntimeEffectBoundary
import vexlife.contract.VexDurableContractFile
import vexlife.filesystem.VexOwnedPath

/**
 * Temporary Windows runtime carrier for the current Product's existing heretic-4b choice.
 *
 * This is deliberately a lived-proof leaf, not a second model selector or permanent runtime owner.
 * It consumes exact already-materialized files, serves numeric loopback only, qualifies the runtime,
 * and hands the shared Product one ordinary [HereticModelConfiguration.QualifiedRuntimeEvidence].
 */
internal object Windows4BPatientZeroRuntime {
    private val Realization =
        HereticRuntimeRealization.Windows4B

    private val Artifact =
        Realization.artifact

    private val RuntimeBinary =
        Realization.runtimeBinary

    private val Runtime =
        Realization.runtime

    private val Qualification =
        Realization.qualification

    val ModelFileName: String =
        requireNotNull(
            Artifact.primaryFilename,
        )

    val ModelExpectedBytes: Long =
        requireNotNull(
            Artifact.expectedPrimaryBytes,
        )

    val ModelSha256: String =
        Artifact.contentSetSha256

    val ExistingLlamaServerSha256: String =
        RuntimeBinary.sha256

    val RuntimeReceiptRelativePath: String =
        Realization.runtimeReceiptRelativePath

    private val Port: Int =
        URI.create(
            Runtime.endpoint,
        )
            .port

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

    @Serializable
    data class Receipt(
        val schemaVersion: String,
        val state: String,
        val processPid: Long,
        val startedAt: String,
        val modelRef: String,
        val modelProfileRef: String,
        val artifactRef: String,
        val artifactContentSetSha256: String,
        val runtimeAdapterRef: String,
        val runtimeClass: String,
        val endpoint: String,
        val requestModel: String,
        val modelPath: String,
        val modelSha256: String,
        val llamaServerPath: String,
        val llamaServerSha256: String,
        val effects: HereticRuntimeEffectBoundary.Value,
    )

    fun writeReceipt(
        productHome: Path,
        started: Started,
    ): Path {
        val home =
            VexOwnedPath.requireRoot(
                productHome,
                "Windows 4B patient-zero Home",
            )
        val path =
            VexOwnedPath.resolveInside(
                home,
                RuntimeReceiptRelativePath,
            )
        val evidence =
            started.evidence

        VexDurableContractFile.write(
            ownedRoot = home,
            path = path,
            value =
                Receipt(
                    schemaVersion =
                        "vexlife-prod.windows-4b-patient-zero-runtime-receipt/v1",
                    state =
                        "QUALIFIED",
                    processPid =
                        started.process.pid(),
                    startedAt =
                        started.startedAt,
                    modelRef =
                        evidence.modelRef,
                    modelProfileRef =
                        evidence.modelProfileRef,
                    artifactRef =
                        evidence.artifactRef,
                    artifactContentSetSha256 =
                        evidence.artifactContentSetSha256,
                    runtimeAdapterRef =
                        evidence.runtimeAdapterRef,
                    runtimeClass =
                        evidence.runtimeClass,
                    endpoint =
                        evidence.endpoint,
                    requestModel =
                        evidence.requestModel,
                    modelPath =
                        started.modelPath.toString(),
                    modelSha256 =
                        ModelSha256,
                    llamaServerPath =
                        started.llamaServerPath.toString(),
                    llamaServerSha256 =
                        ExistingLlamaServerSha256,
                    effects =
                        HereticRuntimeEffectBoundary.QualifiedRuntime,
                ),
        )

        return path
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
            Realization

        val args =
            listOf(
                server.toString(),
                "-m",
                model.toString(),
                "--alias",
                Runtime.requestModel,
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
                    realizationRef =
                        realization.realizationRef,
                    artifactRef =
                        Artifact.artifactRef,
                    artifactContentSetSha256 =
                        Artifact.contentSetSha256,
                    runtimeAdapterRef =
                        Runtime.runtimeAdapterRef,
                    runtimeClass =
                        Runtime.runtimeClass,
                    endpoint =
                        Runtime.endpoint,
                    requestModel =
                        Runtime.requestModel,
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
                Qualification.healthPath,
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
                Qualification.modelsPath,
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
            HereticRuntimeRealization.Windows4B

        val body =
            buildJsonObject {
                put(
                    "model",
                    Runtime.requestModel,
                )
                put(
                    "stream",
                    Qualification.stream,
                )
                put(
                    "max_tokens",
                    Qualification.maxTokens,
                )
                put(
                    "temperature",
                    Qualification.temperature,
                )
                put(
                    "chat_template_kwargs",
                    buildJsonObject {
                        put(
                            "enable_thinking",
                            Qualification.enableThinking,
                        )
                    },
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
                        Runtime.endpoint +
                            Qualification.completionPath,
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

        val parsed =
            Json
                .parseToJsonElement(
                    response.body(),
                )
                .jsonObject

        require(
            parsed[
                "model"
            ]
                ?.jsonPrimitive
                ?.contentOrNull ==
                Runtime.requestModel,
        ) {
            "llama-server qualification completion model does not match exact request model"
        }

        val content =
            parsed[
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
