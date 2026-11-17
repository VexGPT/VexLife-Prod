package vexlife.platform.windows

import com.sun.management.OperatingSystemMXBean
import java.lang.management.ManagementFactory
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import vexlife.companion.DirectCompanionTurn
import vexlife.contract.VexContractJson
import vexlife.conversation.DirectConversationStore
import vexlife.home.VexLifeProdHome
import vexlife.home.VexLifeProdHomeFiles
import vexlife.local.VexLifeProdRuntimeSession
import vexlife.model.heretic.HereticModelCatalog
import vexlife.model.heretic.HereticModelConfiguration
import vexlife.model.heretic.runtime.FileHereticModelConfigurationStore
import vexlife.service.VexLifeProdLocalService
import vexlife.service.VexLifeProdServiceContract

/**
 * Temporary lived Windows 4B carrier.
 *
 * This composes the existing Product owners rather than creating a Windows application beside them:
 *
 * Home -> current catalog selection -> Windows runtime realization -> shared local service ->
 * canonical Conversation -> stop/restart -> same Home/Conversation.
 *
 * The default target is one disposable technical Home supplied by the harness. It never discovers or
 * falls back to Victor's personal Home implicitly.
 */
object Windows4BPatientZeroProcess {
    data class Arguments(
        val sourceSha: String,
        val productHome: Path,
        val modelPath: Path,
        val llamaServerPath: Path,
        val checkpointRoot: Path,
    )

    private val client =
        HttpClient
            .newBuilder()
            .build()

    fun parse(
        args: Array<String>,
    ): Arguments {
        require(
            args.size ==
                10 &&
                args[0] ==
                "--source-sha" &&
                args[2] ==
                "--home" &&
                args[4] ==
                "--model" &&
                args[6] ==
                "--llama-server" &&
                args[8] ==
                "--checkpoints",
        ) {
            "usage: --source-sha <sha> --home <absolute> --model <absolute> --llama-server <absolute> --checkpoints <absolute>"
        }

        val sourceSha =
            args[1]
        require(
            sourceSha.matches(
                Regex(
                    "[0-9a-f]{40,64}",
                ),
            ),
        ) {
            "source SHA must be lowercase hex"
        }

        fun absolute(
            value: String,
            label: String,
        ): Path =
            Path
                .of(
                    value,
                )
                .also {
                    require(
                        it.isAbsolute,
                    ) {
                        label +
                            " must be absolute"
                    }
                }
                .normalize()

        return Arguments(
            sourceSha = sourceSha,
            productHome =
                absolute(
                    args[3],
                    "Home",
                ),
            modelPath =
                absolute(
                    args[5],
                    "model",
                ),
            llamaServerPath =
                absolute(
                    args[7],
                    "llama-server",
                ),
            checkpointRoot =
                absolute(
                    args[9],
                    "checkpoints",
                ),
        )
    }

    fun run(
        args: Array<String>,
    ) {
        val arguments =
            parse(
                args,
            )
        Files.createDirectories(
            arguments.checkpointRoot,
        )

        val attemptRef =
            "windows-4b-patient-zero-" +
                System
                    .currentTimeMillis()
                    .toString()

        println(
            "[1/8] Form or reuse disposable technical Vex Home",
        )
        val home =
            technicalHome(
                arguments.productHome,
            )
        checkpoint(
            arguments.checkpointRoot,
            "01-home.json",
            buildJsonObject {
                put(
                    "stage",
                    "TECHNICAL_HOME_READY",
                )
                put(
                    "homePath",
                    home.productHome,
                )
                put(
                    "homeDisposition",
                    home.state,
                )
                put(
                    "effectsPerformed",
                    true,
                )
            },
        )
        println(
            "PASS: technical Home ready",
        )

        val store =
            FileHereticModelConfigurationStore(
                arguments.productHome,
            )
        val model =
            HereticModelCatalog.Candidate4B

        println(
            "[2/8] Verify and qualify exact Windows heretic-4b runtime",
        )
        var firstRuntimePid: Long? =
            null

        Windows4BPatientZeroRuntime
            .start(
                modelPath =
                    arguments.modelPath,
                llamaServerPath =
                    arguments.llamaServerPath,
                logPath =
                    arguments.checkpointRoot.resolve(
                        "llama-server-first.log",
                    ),
            )
            .use {
                firstRuntime ->
                firstRuntimePid =
                    firstRuntime.process.pid()
                checkpoint(
                    arguments.checkpointRoot,
                    "02-runtime-qualified.json",
                    runtimeCheckpoint(
                        firstRuntime,
                    ),
                )
                println(
                    "PASS: exact Windows 4B runtime qualified",
                )

                println(
                    "[3/8] Select current Product heretic-4b and bind qualified Windows realization",
                )
                val selected =
                    store.selectModel(
                        home.manifest,
                        model,
                    )
                val firstReceiptPath =
                    Windows4BPatientZeroRuntime.writeReceipt(
                        productHome =
                            arguments.productHome,
                        started =
                            firstRuntime,
                    )
                val bound =
                    store.bindQualified(
                        manifest =
                            home.manifest,
                        qualifiedRuntime =
                            firstRuntime.evidence,
                        runtimeReceiptPath =
                            Windows4BPatientZeroRuntime.RuntimeReceiptRelativePath,
                    )
                checkpoint(
                    arguments.checkpointRoot,
                    "03-product-binding.json",
                    buildJsonObject {
                        put(
                            "stage",
                            "CURRENT_PRODUCT_4B_BOUND",
                        )
                        put(
                            "selectionDisposition",
                            selected.state,
                        )
                        put(
                            "bindingDisposition",
                            bound.state,
                        )
                        put(
                            "modelRef",
                            bound.value.modelRef,
                        )
                        put(
                            "runtimeAdapterRef",
                            bound.value.runtimeAdapterRef,
                        )
                        put(
                            "runtimeReceiptPath",
                            Windows4BPatientZeroRuntime.RuntimeReceiptRelativePath,
                        )
                        put(
                            "receiptExists",
                            Files.isRegularFile(
                                firstReceiptPath,
                            ),
                        )
                        put(
                            "predecessorModelConfigurationTouched",
                            false,
                        )
                    },
                )
                println(
                    "PASS: current Product binding owns heretic-4b Windows realization",
                )

                val firstSession =
                    session(
                        home =
                            home,
                        runtime =
                            firstRuntime,
                    )

                println(
                    "[4/8] Start shared VexLife-Prod local service and send first canonical Conversation turn",
                )
                startSharedService(
                    sourceSha =
                        arguments.sourceSha,
                    session =
                        firstSession,
                    home =
                        home,
                    model =
                        model,
                    modelPath =
                        arguments.modelPath,
                )
                    .use {
                        service ->
                        val before =
                            sourceCurrentness(
                                service.origin,
                            )
                        val first =
                            send(
                                origin =
                                    service.origin,
                                idempotencyKey =
                                    "send." +
                                        attemptRef +
                                        ".one",
                                content =
                                    "Technical Windows parity test. Remember exactly the token VEXWINDOWS417 for the immediately following turn. Reply briefly.",
                            )
                        val after =
                            sourceCurrentness(
                                service.origin,
                            )

                        require(
                            first.modelCallPerformed &&
                                first.actualHttpCallPerformedNow &&
                                first.content.isNotBlank() &&
                                !after.currentConversationHeadSha256.isNullOrBlank() &&
                                after.currentConversationHeadSha256 !=
                                before.currentConversationHeadSha256,
                        ) {
                            "first Windows VexLife canonical Conversation turn did not complete"
                        }

                        checkpoint(
                            arguments.checkpointRoot,
                            "04-first-turn.json",
                            buildJsonObject {
                                put(
                                    "stage",
                                    "FIRST_CANONICAL_TURN_COMPLETED",
                                )
                                put(
                                    "serviceOrigin",
                                    service.origin,
                                )
                                put(
                                    "modelCallPerformed",
                                    first.modelCallPerformed,
                                )
                                put(
                                    "actualHttpCallPerformedNow",
                                    first.actualHttpCallPerformedNow,
                                )
                                put(
                                    "responseCharacters",
                                    first.content.length,
                                )
                                put(
                                    "conversationHeadBefore",
                                    before.currentConversationHeadSha256
                                        ?: "",
                                )
                                put(
                                    "conversationHeadAfter",
                                    after.currentConversationHeadSha256
                                        ?: "",
                                )
                            },
                        )
                        println(
                            "PASS: first canonical Conversation turn completed",
                        )
                    }

            }

        val firstPid =
            requireNotNull(
                firstRuntimePid,
            )
        val firstRuntimeStillLive =
            ProcessHandle
                .of(
                    firstPid,
                )
                .map {
                    it.isAlive
                }
                .orElse(
                    false,
                )
        require(
            !firstRuntimeStillLive,
        ) {
            "first Windows patient-zero runtime remained live after close"
        }
        checkpoint(
            arguments.checkpointRoot,
            "05-first-stop.json",
            buildJsonObject {
                put(
                    "stage",
                    "FIRST_SERVICE_AND_RUNTIME_STOPPED",
                )
                put(
                    "runtimeStillLive",
                    false,
                )
                put(
                    "effectsPerformed",
                    true,
                )
            },
        )

        println(
            "[5/8] First runtime stopped; restart same Home + same current Product selection",
        )

        Windows4BPatientZeroRuntime
            .start(
                modelPath =
                    arguments.modelPath,
                llamaServerPath =
                    arguments.llamaServerPath,
                logPath =
                    arguments.checkpointRoot.resolve(
                        "llama-server-second.log",
                    ),
            )
            .use {
                secondRuntime ->
                val secondReceiptPath =
                    Windows4BPatientZeroRuntime.writeReceipt(
                        productHome =
                            arguments.productHome,
                        started =
                            secondRuntime,
                    )
                val rebound =
                    store.bindQualified(
                        manifest =
                            home.manifest,
                        qualifiedRuntime =
                            secondRuntime.evidence,
                        runtimeReceiptPath =
                            Windows4BPatientZeroRuntime.RuntimeReceiptRelativePath,
                    )

                require(
                    FileHereticModelConfigurationStore(
                        arguments.productHome,
                    )
                        .selectedModel(
                            home.manifest,
                        ) ==
                        model,
                ) {
                    "restart changed the current Product model selection"
                }

                checkpoint(
                    arguments.checkpointRoot,
                    "06-restart-runtime.json",
                    buildJsonObject {
                        put(
                            "stage",
                            "SAME_HOME_RUNTIME_REQUALIFIED",
                        )
                        put(
                            "bindingDisposition",
                            rebound.state,
                        )
                        put(
                            "receiptExists",
                            Files.isRegularFile(
                                secondReceiptPath,
                            ),
                        )
                        put(
                            "modelRef",
                            model.modelRef,
                        )
                    },
                )
                println(
                    "PASS: runtime requalified without changing Product model choice",
                )

                val secondSession =
                    session(
                        home =
                            home,
                        runtime =
                            secondRuntime,
                    )

                println(
                    "[6/8] Restart shared VexLife-Prod service and verify Conversation survived",
                )
                startSharedService(
                    sourceSha =
                        arguments.sourceSha,
                    session =
                        secondSession,
                    home =
                        home,
                    model =
                        model,
                    modelPath =
                        arguments.modelPath,
                )
                    .use {
                        service ->
                        val beforeSecond =
                            sourceCurrentness(
                                service.origin,
                            )
                        require(
                            !beforeSecond.currentConversationHeadSha256.isNullOrBlank(),
                        ) {
                            "restart lost prior canonical Conversation head"
                        }

                        checkpoint(
                            arguments.checkpointRoot,
                            "07-restart-continuity.json",
                            buildJsonObject {
                                put(
                                    "stage",
                                    "CONVERSATION_SURVIVED_RESTART",
                                )
                                put(
                                    "conversationHead",
                                    beforeSecond.currentConversationHeadSha256
                                        ?: "",
                                )
                            },
                        )
                        println(
                            "PASS: canonical Conversation survived restart",
                        )

                        println(
                            "[7/8] Send second canonical turn through same shared Product service",
                        )
                        val second =
                            send(
                                origin =
                                    service.origin,
                                idempotencyKey =
                                    "send." +
                                        attemptRef +
                                        ".two",
                                content =
                                    "What exact technical token did I ask you to remember in the immediately prior turn? Reply with the token.",
                            )
                        val finalCurrentness =
                            sourceCurrentness(
                                service.origin,
                            )

                        require(
                            second.modelCallPerformed &&
                                second.actualHttpCallPerformedNow &&
                                second.content.isNotBlank() &&
                                second.content.contains(
                                    "VEXWINDOWS417",
                                    ignoreCase =
                                        true,
                                ) &&
                                finalCurrentness.currentConversationHeadSha256 !=
                                beforeSecond.currentConversationHeadSha256,
                        ) {
                            "second Windows VexLife turn did not prove restart Conversation continuity"
                        }

                        checkpoint(
                            arguments.checkpointRoot,
                            "08-second-turn.json",
                            buildJsonObject {
                                put(
                                    "stage",
                                    "SECOND_CANONICAL_TURN_COMPLETED_WITH_CONTINUITY",
                                )
                                put(
                                    "modelCallPerformed",
                                    second.modelCallPerformed,
                                )
                                put(
                                    "actualHttpCallPerformedNow",
                                    second.actualHttpCallPerformedNow,
                                )
                                put(
                                    "rememberedTechnicalToken",
                                    second.content.contains(
                                        "VEXWINDOWS417",
                                        ignoreCase =
                                            true,
                                    ),
                                )
                                put(
                                    "responseCharacters",
                                    second.content.length,
                                )
                                put(
                                    "conversationHeadBefore",
                                    beforeSecond.currentConversationHeadSha256
                                        ?: "",
                                )
                                put(
                                    "conversationHeadAfter",
                                    finalCurrentness.currentConversationHeadSha256
                                        ?: "",
                                )
                            },
                        )
                        println(
                            "PASS: second canonical turn completed with restart continuity",
                        )
                    }
            }

        println(
            "[8/8] Windows heretic-4b patient-zero end-to-end PASS",
        )
        checkpoint(
            arguments.checkpointRoot,
            "09-terminal.json",
            buildJsonObject {
                put(
                    "stage",
                    "WINDOWS_4B_PATIENT_ZERO_END_TO_END_PASS",
                )
                put(
                    "productModelHandle",
                    HereticModelCatalog.Candidate4B.handle,
                )
                put(
                    "technicalHome",
                    arguments.productHome.toString(),
                )
                put(
                    "sharedVexLifeServiceUsed",
                    true,
                )
                put(
                    "realHomeTouched",
                    false,
                )
                put(
                    "visionTested",
                    false,
                )
                put(
                    "otherModelsTested",
                    false,
                )
            },
        )
    }

    private fun technicalHome(
        productHome: Path,
    ): VexLifeProdHomeFiles.Result {
        val observed =
            VexLifeProdHomeFiles.observeLocation(
                productHome,
            )
        if (
            observed is
                VexLifeProdHomeFiles.Location.Existing
        ) {
            return observed.home
        }

        require(
            observed is
                VexLifeProdHomeFiles.Location.Formable
        ) {
            "technical patient-zero Home is not safely formable"
        }

        val plan =
            VexLifeProdHome.plan(
                personRef =
                    VexLifeProdHome.DefaultPersonRef,
                familyRef =
                    VexLifeProdHome.DefaultFamilyRef,
                deviceName =
                    "VexLife Windows 4B Patient Zero",
                platform =
                    "windows",
                architecture =
                    "x86_64",
                ids =
                    VexLifeProdHomeFiles.newInstallationIds(),
                createdAt =
                    Instant
                        .now()
                        .toString(),
            )

        return VexLifeProdHomeFiles.bootstrap(
            productHome =
                productHome,
            plan =
                plan,
        )
    }

    private fun session(
        home: VexLifeProdHomeFiles.Result,
        runtime: Windows4BPatientZeroRuntime.Started,
    ): VexLifeProdRuntimeSession {
        val evidence =
            runtime.evidence
        val foundation =
            VexLifeProdRuntimeSession.Foundation(
                state =
                    VexLifeProdRuntimeSession.ReadyState,
                productHome =
                    home.productHome,
                qualifiedRuntimeEvidence =
                    evidence,
            )

        return object :
            VexLifeProdRuntimeSession {
            override val foundation: VexLifeProdRuntimeSession.Foundation =
                foundation

            override fun currentQualifiedRuntimeEvidence(
                onRecovery: (() -> Unit)?,
            ): HereticModelConfiguration.QualifiedRuntimeEvidence =
                Windows4BPatientZeroRuntime.observeCurrent(
                    runtime,
                )

            override fun observeCurrentQualifiedRuntimeEvidence():
                HereticModelConfiguration.QualifiedRuntimeEvidence =
                Windows4BPatientZeroRuntime.observeCurrent(
                    runtime,
                )
        }
    }

    private fun startSharedService(
        sourceSha: String,
        session: VexLifeProdRuntimeSession,
        home: VexLifeProdHomeFiles.Result,
        model: HereticModelCatalog.Entry,
        modelPath: Path,
    ): VexLifeProdLocalService =
        VexLifeProdLocalService.start(
            sourceSha =
                sourceSha,
            session =
                session,
            visionRuntimeExecutablePathOrNull =
                null,
            platformProjection =
                object :
                    VexLifeProdLocalService.PlatformProjection {
                    override fun observeCurrentState(
                        productHome: Path,
                    ): VexLifeProdServiceContract.CurrentStateProjection {
                        require(
                            productHome ==
                                Path.of(
                                    home.productHome,
                                ),
                        ) {
                            "Windows Product service changed the selected technical Home"
                        }
                        return windowsCurrentState(
                            home =
                                home,
                            model =
                                model,
                            modelPath =
                                modelPath,
                        )
                    }

                    override fun totalMemoryBytes(): Long? =
                        currentTotalMemoryBytes()
                },
        )

    private fun windowsCurrentState(
        home: VexLifeProdHomeFiles.Result,
        model: HereticModelCatalog.Entry,
        modelPath: Path,
    ): VexLifeProdServiceContract.CurrentStateProjection {
        val configuration =
            FileHereticModelConfigurationStore(
                Path.of(
                    home.productHome,
                ),
            )
                .read(
                    home.manifest,
                )
        val bound =
            requireNotNull(
                (
                    configuration as? FileHereticModelConfigurationStore.Snapshot.Heretic
                )
                    ?.value,
            ) {
                "Windows patient-zero current state requires one Product model binding"
            }

        val total =
            currentTotalMemoryBytes()
        val freeStorage =
            runCatching {
                    Files
                        .getFileStore(
                            Path.of(
                                home.productHome,
                            ),
                        )
                        .usableSpace
                }
                .getOrNull()

        return VexLifeProdServiceContract.CurrentStateProjection(
            schemaVersion =
                VexLifeProdServiceContract.CurrentStateProjectionSchema,
            observedAt =
                Instant
                    .now()
                    .toString(),
            homePath =
                home.productHome,
            homeDisposition =
                home.state,
            homeRef =
                home.manifest.homeRef,
            deviceRef =
                home.installation.deviceRef,
            companionLineageRef =
                home.installation.companionLineageRef,
            platform =
                "windows",
            architecture =
                "x86_64",
            chipModel =
                System.getenv(
                    "PROCESSOR_IDENTIFIER",
                ),
            hostEligibilityState =
                "ELIGIBLE",
            hostEligibilityReason =
                null,
            totalMemoryBytes =
                total,
            modelMinimumMemoryBytes =
                model.recommendedMemoryBytes,
            freeStorageBytes =
                freeStorage,
            modelSelectionState =
                bound.state.wireValue,
            activeModelRef =
                bound.modelRef,
            activeModelProfileRef =
                bound.modelProfileRef,
            runtimeState =
                "BOUND_PRODUCT_MODEL",
            runtimeCurrentness =
                "CONFIGURATION_ONLY_NOT_REOBSERVED",
            runtimeReceiptPath =
                bound.runtimeReceiptPath,
            modelArtifactState =
                "COMPLETE_BY_SIZE_NOT_HASHED",
            modelArtifactTotalBytes =
                Windows4BPatientZeroRuntime.ModelExpectedBytes,
            modelArtifactPresentBySizeBytes =
                if (
                    runCatching {
                            Files.size(
                                modelPath,
                            )
                        }
                        .getOrNull() ==
                    Windows4BPatientZeroRuntime.ModelExpectedBytes
                ) {
                    Windows4BPatientZeroRuntime.ModelExpectedBytes
                } else {
                    0L
                },
            modelArtifactMissingBytes =
                0L,
            modelArtifactInvalidSizeBytes =
                0L,
            effectsPerformed =
                false,
        )
    }

    private fun currentTotalMemoryBytes(): Long? =
        runCatching {
                (
                    ManagementFactory
                        .getOperatingSystemMXBean() as? OperatingSystemMXBean
                )
                    ?.totalMemorySize
            }
            .getOrNull()

    private fun sourceCurrentness(
        origin: String,
    ): VexLifeProdServiceContract.SourceCurrentness =
        VexContractJson.decode(
            get(
                origin +
                    VexLifeProdServiceContract.SourceCurrentnessPath,
            ),
        )

    private fun send(
        origin: String,
        idempotencyKey: String,
        content: String,
    ): VexLifeProdServiceContract.SendResponse {
        val requestBody =
            VexContractJson.encode(
                VexLifeProdServiceContract.SendRequest(
                    idempotencyKey =
                        idempotencyKey,
                    content =
                        content,
                ),
            )
        val request =
            HttpRequest
                .newBuilder(
                    URI.create(
                        origin +
                            VexLifeProdServiceContract.ConversationSendPath,
                    ),
                )
                .header(
                    "content-type",
                    "application/json",
                )
                .POST(
                    HttpRequest.BodyPublishers.ofString(
                        requestBody,
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
            response.statusCode() ==
                200,
        ) {
            "VexLife-Prod send returned HTTP " +
                response.statusCode() +
                ": " +
                response.body()
                    .take(
                        2_000,
                    )
        }
        return VexContractJson.decode(
            response.body(),
        )
    }

    private fun get(
        uri: String,
    ): String {
        val response =
            client.send(
                HttpRequest
                    .newBuilder(
                        URI.create(
                            uri,
                        ),
                    )
                    .GET()
                    .build(),
                HttpResponse.BodyHandlers.ofString(
                    StandardCharsets.UTF_8,
                ),
            )
        require(
            response.statusCode() ==
                200,
        ) {
            "VexLife-Prod GET returned HTTP " +
                response.statusCode()
        }
        return response.body()
    }

    private fun runtimeCheckpoint(
        runtime: Windows4BPatientZeroRuntime.Started,
    ): JsonObject =
        buildJsonObject {
            put(
                "stage",
                "WINDOWS_4B_RUNTIME_QUALIFIED",
            )
            put(
                "processPid",
                runtime.process.pid(),
            )
            put(
                "modelRef",
                runtime.evidence.modelRef,
            )
            put(
                "runtimeAdapterRef",
                runtime.evidence.runtimeAdapterRef,
            )
            put(
                "endpoint",
                runtime.evidence.endpoint,
            )
            put(
                "requestModel",
                runtime.evidence.requestModel,
            )
            put(
                "qualificationInferenceEffect",
                runtime.evidence.qualification.qualificationInferenceEffect,
            )
        }

    private fun checkpoint(
        root: Path,
        name: String,
        value: JsonObject,
    ) {
        Files.writeString(
            root.resolve(
                name,
            ),
            value.toString() +
                "\n",
            StandardCharsets.UTF_8,
        )
    }
}

fun main(
    args: Array<String>,
) {
    Windows4BPatientZeroProcess.run(
        args,
    )
}

// [VXG RealForever]
