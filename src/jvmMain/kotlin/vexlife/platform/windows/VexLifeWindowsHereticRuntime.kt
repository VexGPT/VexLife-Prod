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
import vexlife.contract.VexDurableContractFile
import vexlife.filesystem.VexOwnedPath
import vexlife.home.VexLifeProdHomeFiles
import vexlife.model.heretic.HereticModelConfiguration
import vexlife.model.heretic.HereticRuntimeEffectBoundary
import vexlife.model.heretic.HereticRuntimeQualification
import vexlife.model.heretic.HereticRuntimeRealization
import vexlife.platform.VexLifePlatformRealization

/**
 * Product-owned Windows Heretic runtime process + qualification leaf.
 *
 * This owner starts only an already-materialized exact [HereticRuntimeRealization.Value], performs
 * its declared bounded qualification, writes one realization-scoped runtime receipt, and can
 * re-observe that same started process. It does not choose/download a model, mutate Product model
 * binding, start the Product service, or own Windows packaging.
 */
object VexLifeWindowsHereticRuntime {
    const val ReceiptSchema =
        "vexlife-prod.windows-heretic-runtime-receipt/v1"

    private const val QualifiedState =
        "QUALIFIED"

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

    @Serializable
    data class QualifiedRealizationEvidence(
        val runtimeReceiptRef: String,
        val realizationRef: String,
        val state: String,
        val modelRef: String,
        val modelProfileRef: String,
        val artifactRef: String,
        val artifactContentSetSha256: String,
        val runtimeBinaryRef: String,
        val runtimeArchiveSha256: String,
        val runtimeExecutableSha256: String,
        val runtimeAdapterRef: String,
        val runtimeClass: String,
        val endpoint: String,
        val requestModel: String,
        val qualification: HereticRuntimeQualification,
    ) {
        init {
            require(
                runtimeReceiptRef.isNotBlank() &&
                    realizationRef.isNotBlank() &&
                    state == QualifiedState &&
                    modelRef.isNotBlank() &&
                    modelProfileRef.isNotBlank() &&
                    artifactRef.isNotBlank() &&
                    artifactContentSetSha256.matches(Sha256) &&
                    runtimeBinaryRef.isNotBlank() &&
                    runtimeArchiveSha256.matches(Sha256) &&
                    runtimeExecutableSha256.matches(Sha256) &&
                    runtimeAdapterRef.isNotBlank() &&
                    runtimeClass.isNotBlank() &&
                    endpoint.isNotBlank() &&
                    requestModel.isNotBlank(),
            ) {
                "Windows qualified realization evidence is invalid"
            }
        }
    }

    /**
     * Projects one exact Windows runtime qualification into the common Product model-binding
     * evidence contract. No lifecycle or persistence effect occurs here.
     */
    fun modelConfigurationEvidence(
        evidence: QualifiedRealizationEvidence,
    ): HereticModelConfiguration.QualifiedRuntimeEvidence =
        HereticModelConfiguration.QualifiedRuntimeEvidence(
            runtimeReceiptRef = evidence.runtimeReceiptRef,
            runtimeState = vexlife.model.heretic.HereticRuntimeState.Qualified,
            modelRef = evidence.modelRef,
            realizationRef = evidence.realizationRef,
            modelProfileRef = evidence.modelProfileRef,
            artifactRef = evidence.artifactRef,
            artifactContentSetSha256 = evidence.artifactContentSetSha256,
            runtimeAdapterRef = evidence.runtimeAdapterRef,
            runtimeClass = evidence.runtimeClass,
            endpoint = evidence.endpoint,
            requestModel = evidence.requestModel,
            qualification = evidence.qualification,
        )

    @Serializable
    data class Receipt(
        val schemaVersion: String,
        val state: String,
        val processPid: Long,
        val startedAt: String,
        val qualified: QualifiedRealizationEvidence,
        val modelPath: String,
        val runtimeImagePath: String,
        val runtimeExecutablePath: String,
        val workingDirectory: String,
        val launchArguments: List<String>,
        val logPath: String,
        val effects: HereticRuntimeEffectBoundary.Value,
    ) {
        init {
            require(
                schemaVersion == ReceiptSchema &&
                    state == QualifiedState &&
                    processPid > 0 &&
                    startedAt.isNotBlank() &&
                    modelPath.isNotBlank() &&
                    runtimeImagePath.isNotBlank() &&
                    runtimeExecutablePath.isNotBlank() &&
                    workingDirectory.isNotBlank() &&
                    launchArguments.isNotEmpty() &&
                    launchArguments.first() == runtimeExecutablePath &&
                    logPath.isNotBlank() &&
                    qualified.state == QualifiedState,
            ) {
                "Windows Heretic runtime receipt is invalid"
            }
        }
    }

    data class Started internal constructor(
        val productHome: Path,
        val process: Process,
        val realization: HereticRuntimeRealization.Value,
        val materialization: VexLifeWindowsRuntimeMaterialization.Observation,
        val evidence: QualifiedRealizationEvidence,
        val receiptPath: Path,
        val logPath: Path,
        val startedAt: String,
    ) : AutoCloseable {
        override fun close() {
            stopProcess(
                process,
            )
        }
    }

    internal enum class ExistingProcessDisposition {
        ABSENT,
        ACTIVE_EXACT,
        ABANDONED_EXACT,
        HELD_UNKNOWN,
    }

    internal fun classifyExistingProcess(
        endpointOccupied: Boolean,
        receiptExact: Boolean,
        processLive: Boolean,
        commandExact: Boolean,
        argumentsExact: Boolean,
        startInstantExact: Boolean,
        parentLive: Boolean,
        healthExact: Boolean,
        modelExposureExact: Boolean,
    ): ExistingProcessDisposition {
        if (
            !endpointOccupied &&
                !processLive
        ) {
            return ExistingProcessDisposition.ABSENT
        }

        val exact =
            endpointOccupied &&
                receiptExact &&
                processLive &&
                commandExact &&
                argumentsExact &&
                startInstantExact &&
                healthExact &&
                modelExposureExact

        return when {
            exact && parentLive ->
                ExistingProcessDisposition.ACTIVE_EXACT

            exact ->
                ExistingProcessDisposition.ABANDONED_EXACT

            else ->
                ExistingProcessDisposition.HELD_UNKNOWN
        }
    }

    /**
     * Recover only a runtime this Product can prove is the exact abandoned Windows realization.
     *
     * Cross-process reuse is deliberately not admitted. A live exact runtime with a live parent is
     * preserved. An occupied endpoint with incomplete or mismatched ownership evidence is held.
     * Only an exact receipt/process whose parent Product process is gone may be retired before a
     * fresh Start effect.
     */
    fun retireAbandonedExactIfPresent(
        productHome: Path,
        realization: HereticRuntimeRealization.Value =
            HereticRuntimeRealization.WindowsQwen4B,
    ): Boolean {
        requireWindowsHost(
            realization,
        )
        val home =
            currentHome(
                productHome,
            )
        val materialization =
            VexLifeWindowsRuntimeMaterialization.observe(
                productHome = home,
                realization = realization,
            )
        require(
            materialization.ready,
        ) {
            "Windows abandoned-runtime recovery requires exact current materialization"
        }

        val endpoint =
            URI.create(
                realization.runtime.endpoint,
            )
        val endpointOccupied =
            portOccupied(
                endpoint.port,
            )
        val receiptPath =
            VexOwnedPath.resolveInside(
                home,
                realization.runtimeReceiptRelativePath,
            )
        val receipt =
            runCatching {
                    VexDurableContractFile.read<Receipt>(
                        ownedRoot = home,
                        path = receiptPath,
                    )
                }
                .getOrNull()
        val process =
            receipt
                ?.let {
                    ProcessHandle
                        .of(
                            it.processPid,
                        )
                        .orElse(
                            null,
                        )
                }
        val processLive =
            process
                ?.isAlive ==
                true
        val expectedArguments =
            launchArguments(
                realization = realization,
                materialization = materialization,
            )
        val receiptExact =
            receipt
                ?.let {
                    current ->
                    current.qualified ==
                        qualifiedEvidence(
                            realization = realization,
                            processPid = current.processPid,
                            qualification = current.qualified.qualification,
                        ) &&
                        current.modelPath ==
                            materialization.modelPath &&
                        current.runtimeImagePath ==
                            materialization.runtimeImagePath &&
                        current.runtimeExecutablePath ==
                            materialization.runtimeExecutablePath &&
                        current.workingDirectory ==
                            materialization.runtimeImagePath &&
                        current.launchArguments ==
                            expectedArguments
                }
                ?: false

        val info =
            process
                ?.info()
        val commandExact =
            runCatching {
                    val observed =
                        info
                            ?.command()
                            ?.orElse(
                                null,
                            )
                            ?: return@runCatching false
                    Path.of(
                            observed,
                        )
                        .toAbsolutePath()
                        .normalize() ==
                        Path.of(
                                requireNotNull(
                                    receipt,
                                ).runtimeExecutablePath,
                            )
                            .toAbsolutePath()
                            .normalize()
                }
                .getOrDefault(
                    false,
                )
        val argumentsExact =
            info
                ?.arguments()
                ?.orElse(
                    null,
                )
                ?.toList() ==
                expectedArguments.drop(
                    1,
                )
        val startInstantExact =
            runCatching {
                    val observed =
                        info
                            ?.startInstant()
                            ?.orElse(
                                null,
                            )
                            ?: return@runCatching false
                    val recorded =
                        Instant.parse(
                            requireNotNull(
                                receipt,
                            ).startedAt,
                        )
                    Duration
                        .between(
                            recorded,
                            observed,
                        )
                        .abs() <=
                        Duration.ofSeconds(
                            5,
                        )
                }
                .getOrDefault(
                    false,
                )
        val parentLive =
            process
                ?.parent()
                ?.filter {
                    it.isAlive
                }
                ?.isPresent ==
                true
        val healthExact =
            endpointOccupied &&
                runCatching {
                        health(
                            realization,
                        ) ==
                            "ok"
                    }
                    .getOrDefault(
                        false,
                    )
        val modelExposureExact =
            endpointOccupied &&
                runCatching {
                        requireExactModelExposure(
                            realization,
                        )
                        true
                    }
                    .getOrDefault(
                        false,
                    )

        return when (
            classifyExistingProcess(
                endpointOccupied = endpointOccupied,
                receiptExact = receiptExact,
                processLive = processLive,
                commandExact = commandExact,
                argumentsExact = argumentsExact,
                startInstantExact = startInstantExact,
                parentLive = parentLive,
                healthExact = healthExact,
                modelExposureExact = modelExposureExact,
            )
        ) {
            ExistingProcessDisposition.ABSENT ->
                false

            ExistingProcessDisposition.ABANDONED_EXACT -> {
                val abandoned =
                    requireNotNull(
                        process,
                    )
                stopProcess(
                    abandoned,
                )
                require(
                    !portOccupied(
                        endpoint.port,
                    ),
                ) {
                    "exact abandoned Windows Heretic runtime did not release its loopback endpoint"
                }
                true
            }

            ExistingProcessDisposition.ACTIVE_EXACT ->
                error(
                    "exact Windows Heretic runtime is still owned by a live parent process",
                )

            ExistingProcessDisposition.HELD_UNKNOWN ->
                error(
                    "Windows Heretic runtime endpoint is occupied without exact abandoned-process ownership",
                )
        }
    }

    fun start(
        productHome: Path,
        realization: HereticRuntimeRealization.Value =
            HereticRuntimeRealization.WindowsQwen4B,
    ): Started {
        requireWindowsHost(
            realization,
        )
        val home =
            currentHome(
                productHome,
            )
        val materialization =
            VexLifeWindowsRuntimeMaterialization.observe(
                productHome = home,
                realization = realization,
            )
        require(
            materialization.ready,
        ) {
            "Windows Heretic runtime requires exact materialized dependencies before start"
        }

        val endpoint =
            URI.create(
                realization.runtime.endpoint,
            )
        require(
            endpoint.scheme == "http" &&
                endpoint.host == "127.0.0.1" &&
                endpoint.port > 0,
        ) {
            "Windows Heretic runtime endpoint must be one numeric loopback HTTP coordinate"
        }
        require(
            !portOccupied(
                endpoint.port,
            ),
        ) {
            "Windows Heretic runtime endpoint is already occupied: " +
                realization.runtime.endpoint
        }

        val runtimeImage =
            Path.of(
                    materialization.runtimeImagePath,
                )
                .normalize()
        val runtimeRoot =
            requireNotNull(
                runtimeImage.parent,
            ) {
                "Windows runtime image requires one realization root"
            }
        require(
            runtimeRoot.startsWith(
                home,
            ),
        ) {
            "Windows runtime image escaped the Vex Home"
        }
        val logPath =
            runtimeRoot
                .resolve(
                    "logs/llama-server.log",
                )
                .normalize()
        VexOwnedPath.ensureParent(
            home,
            logPath,
        )

        val args =
            launchArguments(
                realization = realization,
                materialization = materialization,
            )
        val process =
            ProcessBuilder(
                    args,
                )
                .directory(
                    runtimeImage.toFile(),
                )
                .redirectErrorStream(
                    true,
                )
                .redirectOutput(
                    logPath.toFile(),
                )
                .start()
        val startedAt =
            Instant
                .now()
                .toString()

        try {
            waitForHealth(
                process = process,
                realization = realization,
            )
            requireExactModelExposure(
                realization,
            )
            val qualification =
                performQualificationInference(
                    realization,
                )
            val evidence =
                qualifiedEvidence(
                    realization = realization,
                    processPid = process.pid(),
                    qualification = qualification,
                )
            val receiptPath =
                writeReceipt(
                    home = home,
                    processPid = process.pid(),
                    startedAt = startedAt,
                    realization = realization,
                    materialization = materialization,
                    evidence = evidence,
                    workingDirectory = runtimeImage,
                    launchArguments = args,
                    logPath = logPath,
                )

            return Started(
                productHome = home,
                process = process,
                realization = realization,
                materialization = materialization,
                evidence = evidence,
                receiptPath = receiptPath,
                logPath = logPath,
                startedAt = startedAt,
            )
        } catch (error: Throwable) {
            stopProcess(
                process,
            )
            throw error
        }
    }

    fun observeCurrent(
        started: Started,
    ): QualifiedRealizationEvidence {
        require(
            started.process.isAlive,
        ) {
            "Windows Heretic runtime process is not live"
        }

        val currentMaterialization =
            VexLifeWindowsRuntimeMaterialization.observe(
                productHome = started.productHome,
                realization = started.realization,
            )
        require(
            currentMaterialization.ready &&
                currentMaterialization.modelPath ==
                    started.materialization.modelPath &&
                currentMaterialization.runtimeExecutablePath ==
                    started.materialization.runtimeExecutablePath,
        ) {
            "Windows Heretic runtime dependencies are no longer the exact started realization"
        }

        val receipt =
            requireNotNull(
                VexDurableContractFile.read<Receipt>(
                    ownedRoot = started.productHome,
                    path = started.receiptPath,
                ),
            ) {
                "Windows Heretic runtime receipt is missing"
            }
        require(
            receipt.processPid == started.process.pid() &&
                receipt.qualified == started.evidence &&
                receipt.modelPath == currentMaterialization.modelPath &&
                receipt.runtimeExecutablePath ==
                    currentMaterialization.runtimeExecutablePath &&
                receipt.workingDirectory ==
                    currentMaterialization.runtimeImagePath &&
                receipt.launchArguments ==
                    launchArguments(
                        realization = started.realization,
                        materialization = currentMaterialization,
                    ) &&
                receipt.logPath ==
                    started.logPath.toString(),
        ) {
            "Windows Heretic runtime receipt no longer identifies the started process"
        }

        requireHealth(
            started.realization,
        )
        requireExactModelExposure(
            started.realization,
        )

        return started.evidence
    }

    internal fun launchArguments(
        realization: HereticRuntimeRealization.Value,
        materialization: VexLifeWindowsRuntimeMaterialization.Observation,
    ): List<String> {
        require(
            realization.target ==
                HereticRuntimeRealization.WindowsX64 &&
                materialization.realizationRef ==
                realization.realizationRef &&
                materialization.ready,
        ) {
            "Windows Heretic launch requires one exact ready realization"
        }
        val launch =
            requireNotNull(
                realization.runtime.livedLaunchProfile,
            ) {
                "Windows realization has no lived launch profile"
            }

        val args =
            mutableListOf(
                materialization.runtimeExecutablePath,
                "-m",
                materialization.modelPath,
                "--alias",
                realization.runtime.requestModel,
                "--host",
                "127.0.0.1",
                "--port",
                URI.create(
                        realization.runtime.endpoint,
                    )
                    .port
                    .toString(),
                "--ctx-size",
                launch.contextTokens.toString(),
            )

        launch.gpuOffloadLayers?.let {
            args +=
                listOf(
                    "-ngl",
                    it.toString(),
                )
        }
        if (launch.jinja) {
            args +=
                "--jinja"
        }

        return args
    }

    internal fun qualifiedEvidence(
        realization: HereticRuntimeRealization.Value,
        processPid: Long,
        qualification: HereticRuntimeQualification =
            HereticRuntimeQualification.qualified(),
    ): QualifiedRealizationEvidence {
        require(
            processPid >
                0,
        ) {
            "Windows qualified runtime PID must be positive"
        }
        val binary =
            requireNotNull(
                realization.runtimeBinary,
            ) {
                "Windows qualified runtime requires one runtime binary"
            }

        return QualifiedRealizationEvidence(
            runtimeReceiptRef =
                "receipt.vexlife-prod.windows-heretic." +
                    processPid,
            realizationRef =
                realization.realizationRef,
            state =
                QualifiedState,
            modelRef =
                realization.modelRef,
            modelProfileRef =
                realization.modelProfileRef,
            artifactRef =
                realization.artifact.artifactRef,
            artifactContentSetSha256 =
                realization.artifact.contentSetSha256,
            runtimeBinaryRef =
                binary.binaryRef,
            runtimeArchiveSha256 =
                binary.archiveSha256,
            runtimeExecutableSha256 =
                binary.executableSha256,
            runtimeAdapterRef =
                realization.runtime.runtimeAdapterRef,
            runtimeClass =
                realization.runtime.runtimeClass,
            endpoint =
                realization.runtime.endpoint,
            requestModel =
                realization.runtime.requestModel,
            qualification =
                qualification,
        )
    }

    private fun writeReceipt(
        home: Path,
        processPid: Long,
        startedAt: String,
        realization: HereticRuntimeRealization.Value,
        materialization: VexLifeWindowsRuntimeMaterialization.Observation,
        evidence: QualifiedRealizationEvidence,
        workingDirectory: Path,
        launchArguments: List<String>,
        logPath: Path,
    ): Path {
        val path =
            VexOwnedPath.resolveInside(
                home,
                realization.runtimeReceiptRelativePath,
            )

        VexDurableContractFile.write(
            ownedRoot = home,
            path = path,
            value =
                Receipt(
                    schemaVersion =
                        ReceiptSchema,
                    state =
                        QualifiedState,
                    processPid =
                        processPid,
                    startedAt =
                        startedAt,
                    qualified =
                        evidence,
                    modelPath =
                        materialization.modelPath,
                    runtimeImagePath =
                        materialization.runtimeImagePath,
                    runtimeExecutablePath =
                        materialization.runtimeExecutablePath,
                    workingDirectory =
                        workingDirectory.toString(),
                    launchArguments =
                        launchArguments,
                    logPath =
                        logPath.toString(),
                    effects =
                        HereticRuntimeEffectBoundary.QualifiedRuntime,
                ),
        )

        return path
    }

    private fun requireWindowsHost(
        realization: HereticRuntimeRealization.Value,
    ) {
        require(
            realization.target ==
                HereticRuntimeRealization.WindowsX64,
        ) {
            "Windows Heretic runtime requires the exact Windows x64 target"
        }
        val host =
            VexLifeWindowsHostObservation.current()
        val resolved =
            requireNotNull(
                host as? VexLifeWindowsHostObservation.Result.Resolved,
            ) {
                "Windows Heretic runtime requires one resolved Windows host"
            }
        require(
            resolved.target ==
                realization.target &&
                resolved.processArchitecture ==
                VexLifePlatformRealization.Architecture.X86_64,
        ) {
            "Windows Heretic runtime host/process architecture does not match the realization"
        }
    }

    private fun currentHome(
        productHome: Path,
    ): Path {
        val observed =
            requireNotNull(
                VexLifeProdHomeFiles.observeExisting(
                    productHome,
                ),
            ) {
                "Windows Heretic runtime requires one existing canonical Vex Home"
            }
        return VexOwnedPath.requireRoot(
            Path.of(
                observed.productHome,
            ),
            "VexLife-Prod Home",
        )
    }

    private fun waitForHealth(
        process: Process,
        realization: HereticRuntimeRealization.Value,
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
                "llama-server exited before Windows runtime qualification; see runtime log"
            }

            val observed =
                runCatching {
                    health(
                        realization,
                    )
                }
            if (
                observed.getOrNull() ==
                    "ok"
            ) {
                return
            }
            last =
                observed.fold(
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

    private fun requireHealth(
        realization: HereticRuntimeRealization.Value,
    ) {
        require(
            health(
                realization,
            ) ==
                "ok",
        ) {
            "Windows Heretic runtime health is not exact status=ok"
        }
    }

    private fun health(
        realization: HereticRuntimeRealization.Value,
    ): String {
        val response =
            get(
                realization = realization,
                path = realization.qualification.healthPath,
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

    private fun requireExactModelExposure(
        realization: HereticRuntimeRealization.Value,
    ) {
        val response =
            get(
                realization = realization,
                path = realization.qualification.modelsPath,
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
            realization.runtime.requestModel
        val matches =
            data.count { element ->
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

    private fun performQualificationInference(
        realization: HereticRuntimeRealization.Value,
    ): HereticRuntimeQualification {
        val qualification =
            realization.qualification
        val body =
            buildJsonObject {
                put(
                    "model",
                    realization.runtime.requestModel,
                )
                put(
                    "stream",
                    qualification.stream,
                )
                put(
                    "max_tokens",
                    qualification.maxTokens,
                )
                put(
                    "temperature",
                    qualification.temperature,
                )
                put(
                    "chat_template_kwargs",
                    buildJsonObject {
                        put(
                            "enable_thinking",
                            qualification.enableThinking,
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
                        realization.runtime.endpoint +
                            qualification.completionPath,
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
                realization.runtime.requestModel,
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

        return HereticRuntimeQualification.qualified()
    }

    private fun get(
        realization: HereticRuntimeRealization.Value,
        path: String,
    ): HttpResponse<String> {
        val request =
            HttpRequest
                .newBuilder(
                    URI.create(
                        realization.runtime.endpoint +
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

    private fun portOccupied(
        port: Int,
    ): Boolean =
        runCatching {
                Socket().use { socket ->
                    socket.connect(
                        InetSocketAddress(
                            "127.0.0.1",
                            port,
                        ),
                        300,
                    )
                }
                true
            }
            .getOrElse {
                false
            }

    private fun stopProcess(
        process: ProcessHandle,
    ) {
        if (
            process.isAlive
        ) {
            process.destroy()
            runCatching {
                process
                    .onExit()
                    .get(
                        10,
                        TimeUnit.SECONDS,
                    )
            }
            if (
                process.isAlive
            ) {
                process.destroyForcibly()
                process
                    .onExit()
                    .get(
                        10,
                        TimeUnit.SECONDS,
                    )
            }
        }
        require(
            !process.isAlive,
        ) {
            "exact abandoned Windows Heretic runtime did not terminate"
        }
    }

    private fun stopProcess(
        process: Process,
    ) {
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

    private val Sha256 =
        Regex(
            "[0-9a-f]{64}",
        )
}

// [VXG RealForever]
