package vexlife.harness

import java.nio.file.Files
import java.nio.file.Path
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import vexlife.capability.VexLifeCapabilityBridge
import vexlife.companion.DirectCompanionConnection
import vexlife.companion.DirectCompanionTurn
import vexlife.companion.DirectTurnContextStore
import vexlife.conversation.DirectConversationStore
import vexlife.foundational.VexLifeFoundationalField
import vexlife.home.CanonicalVexLifeHomeFixture
import vexlife.local.VexLifeProdLocalSetup
import vexlife.model.heretic.HereticModelCatalog
import vexlife.model.heretic.HereticModelConfiguration
import vexlife.model.heretic.HereticProductIdentity
import vexlife.model.heretic.HereticRuntimeState
import vexlife.model.heretic.runtime.FileHereticRuntimeReceiptStore
import vexlife.model.heretic.runtime.HereticPersistentRuntime

/**
 * Multi-Vex F1: one truly fresh direct Companion turn over an empty scratch Conversation.
 *
 * Mechanical evidence proves the capability frame/tool offer/typed result membrane. It does not
 * score whether Vex chose a useful demonstration or interpreted it wisely.
 */
object MultiVexFreshBootCapabilityAdapter {
    data class Config(
        val runtimeHome: String,
        val foundationBodyFile: String,
        val foundationRevisionsFile: String,
        val requestFile: String,
        val resultDir: String,
        val sourceSha: String,
        val sourceClean: Boolean,
        val experimentWindow: Boolean,
    )

    fun run(config: Config): Int {
        require(config.experimentWindow) {
            "F1 requires one dedicated experiment window"
        }
        require(config.sourceClean) {
            "F1 requires exact clean source"
        }
        require(Regex("[0-9a-f]{40}").matches(config.sourceSha)) {
            "F1 sourceSha must be one exact commit"
        }

        val runtimeHome = Path.of(config.runtimeHome).toAbsolutePath().normalize()
        val resultDir = Path.of(config.resultDir).toAbsolutePath().normalize()
        Files.createDirectories(resultDir)

        val request =
            Files.readString(
                Path.of(config.requestFile).toAbsolutePath().normalize(),
            )
                .trim()
        require(request.isNotBlank()) {
            "F1 request must be non-blank"
        }

        val foundation =
            requireNotNull(
                VexLifeFoundationalField.read(
                    bodyText =
                        Files.readString(
                            Path.of(config.foundationBodyFile).toAbsolutePath().normalize(),
                        ),
                    revisionsText =
                        Files.readString(
                            Path.of(config.foundationRevisionsFile).toAbsolutePath().normalize(),
                        ),
                ) as? VexLifeFoundationalField.Reading.Current,
            ) {
                "F1 Foundation override must resolve to one current reading"
            }

        val canonicalWriterBefore = DirectConversationStore(runtimeHome).threadWriterCurrentness()
        require(canonicalWriterBefore.state == "IDLE") {
            "F1 canonical Product Conversation writer must be IDLE"
        }
        val canonicalBefore = DirectConversationStore(runtimeHome).current()
        val (runtimeEvidence, runtimeReceipt) = currentProductQualifiedRuntime(runtimeHome)

        val scratchParent = Files.createTempDirectory(resultDir, "fresh-boot-capability-scratch-")
        val scratchHome = scratchParent.resolve(".vexlife")
        CanonicalVexLifeHomeFixture.form(
            home = scratchHome,
            deviceSuffix = "fresh-boot-capability-device",
            lineageSuffix = "fresh-boot-capability-lineage",
            personRef = "person.synthetic.fresh-boot-capability",
            familyRef = "vex-family.synthetic-fresh-boot-capability",
            deviceName = "Synthetic fresh-boot capability host",
        )

        return try {
            val scratchBefore = DirectConversationStore(scratchHome).current()
            require(scratchBefore.events.isEmpty()) {
                "F1 fresh-boot scratch Conversation must start empty"
            }

            val currentRuntimeEvidence = {
                currentProductQualifiedRuntime(
                    runtimeHome = runtimeHome,
                    expectedReceiptRef = runtimeEvidence.runtimeReceiptRef,
                ).first
            }
            val hooks =
                DirectCompanionTurn.Hooks(
                    transport = DirectCompanionConnection.SystemTransport,
                    currentRuntimeEvidence = { currentRuntimeEvidence() },
                    reobserveSameRuntimeEvidence = { currentRuntimeEvidence() },
                    currentFoundationalField = { foundation },
                )
            val setup =
                VexLifeProdLocalSetup.Result(
                    state = "LOCAL_HERETIC_FOUNDATION_READY",
                    productHome = scratchHome.toString(),
                    homeDisposition = "EXISTING_HOME_PRESERVED",
                    modelSelectionDisposition = "BOUND",
                    managedPythonDisposition = "REUSED_VERIFIED",
                    artifactDisposition = "REUSED_VERIFIED",
                    environmentDisposition = "REUSED_VERIFIED",
                    runtimeDisposition = "REUSED_EXACT_OWNED_MLX_RUNTIME",
                    modelBindingDisposition = "BOUND_CURRENT",
                    modelConfigurationState = HereticModelConfiguration.State.BoundProductModel,
                    qualifiedRuntimeEvidence = runtimeEvidence,
                    endpoint = HereticProductIdentity.Endpoint,
                    requestModel = HereticProductIdentity.RequestModel,
                )

            val result =
                DirectCompanionTurn.performWithHooks(
                    setup = setup,
                    intent =
                        DirectCompanionTurn.Intent(
                            idempotencyKey = "send.multivex.fresh-boot-capability.f1.a001",
                            content = request,
                        ),
                    hooks = hooks,
                )
            val scratchAfter = DirectConversationStore(scratchHome).current()
            require(scratchAfter.events.size == 2) {
                "F1 completed direct turn must create exactly one request/response pair"
            }
            val context =
                requireNotNull(
                    DirectTurnContextStore(scratchHome).read(result.turnRef),
                ) {
                    "F1 completed turn must retain one exact turn context"
                }
            val frame =
                requireNotNull(
                    result.promptContextReceipt.capabilityFrameSnapshot,
                ) {
                    "F1 requires one current capability-frame snapshot"
                }
            require(result.promptContextReceipt.capabilityFrameIncluded) {
                "F1 requires the current capability frame in the provider prompt"
            }
            require(result.invocationEvidence.providerToolCount > 0) {
                "F1 requires provider-native capability tools to be offered"
            }
            require(result.invocationEvidence.providerToolSchemaSha256 != null) {
                "F1 requires the exact provider-native capability schema hash"
            }
            require(result.promptContextReceipt.capabilityExchangeAllowanceTokens > 0) {
                "F1 requires capability request/result room in the provider turn"
            }
            require(!frame.effectAuthorityGranted) {
                "F1 capability frame must not grant effect authority"
            }

            val canonicalAfter = DirectConversationStore(runtimeHome).current()
            val canonicalWriterAfter = DirectConversationStore(runtimeHome).threadWriterCurrentness()
            require(canonicalAfter.head?.conversationHeadSha256 == canonicalBefore.head?.conversationHeadSha256) {
                "F1 scratch encounter must not mutate canonical Product Conversation"
            }
            require(canonicalWriterAfter.state == "IDLE") {
                "F1 canonical Product Conversation writer must remain IDLE"
            }
            val (_, receiptAfter) =
                currentProductQualifiedRuntime(
                    runtimeHome = runtimeHome,
                    expectedReceiptRef = runtimeReceipt.receiptRef,
                )

            val trace = context.capabilityTrace
            val out =
                buildJsonObject {
                    put("schemaVersion", "vexlife.multivex.fresh-boot-capability-result/v1")
                    put("status", "PASS")
                    put("mechanicalState", "PASS")
                    put("sourceSha", config.sourceSha)
                    put("freshConversationEventCountBefore", scratchBefore.events.size)
                    put("freshConversationEventCountAfter", scratchAfter.events.size)
                    put("humanRequest", request)
                    put("vexContent", result.content)
                    put("turnRef", result.turnRef)
                    put("model", result.model)
                    put("runtimeReceiptRef", runtimeEvidence.runtimeReceiptRef)
                    put("runtimeReceiptUnchanged", receiptAfter.receiptRef == runtimeReceipt.receiptRef)
                    put("capabilityFrameIncluded", result.promptContextReceipt.capabilityFrameIncluded)
                    put(
                        "availableCapabilityRefs",
                        buildJsonArray {
                            frame.availableCapabilityRefs.forEach { add(it) }
                        },
                    )
                    put(
                        "heldCapabilityRefs",
                        buildJsonArray {
                            frame.heldCapabilityRefs.forEach { add(it) }
                        },
                    )
                    put(
                        "unavailableCapabilityRefs",
                        buildJsonArray {
                            frame.unavailableCapabilityRefs.forEach { add(it) }
                        },
                    )
                    put(
                        "unknownCapabilityRefs",
                        buildJsonArray {
                            frame.unknownCapabilityRefs.forEach { add(it) }
                        },
                    )
                    put("capabilityFrameTruncated", frame.truncated)
                    put("capabilityFrameEffectAuthorityGranted", frame.effectAuthorityGranted)
                    put("providerToolCount", result.invocationEvidence.providerToolCount)
                    put(
                        "providerToolSchemaSha256",
                        requireNotNull(result.invocationEvidence.providerToolSchemaSha256),
                    )
                    put(
                        "capabilityUses",
                        buildJsonArray {
                            result.capabilityUses.forEach { use ->
                                add(
                                    buildJsonObject {
                                        put("handle", use.handle)
                                        put("action", use.action)
                                        use.target?.let { put("target", it) }
                                        put("state", use.state.name)
                                    },
                                )
                            }
                        },
                    )
                    put(
                        "capabilityTrace",
                        buildJsonArray {
                            trace?.entries.orEmpty().forEach { entry ->
                                add(
                                    buildJsonObject {
                                        put("callRef", entry.callRef)
                                        put("requestNumber", entry.requestNumber)
                                        entry.capabilityRef?.let { put("capabilityRef", it) }
                                        put("handle", entry.handle)
                                        put("action", entry.action)
                                        entry.target?.let { put("target", it) }
                                        put("state", entry.state.name)
                                        put("resultSha256", entry.resultSha256)
                                        entry.ownerSourcePath?.let { put("ownerSourcePath", it) }
                                        put("externalEffectsExecuted", entry.externalEffectsExecuted)
                                        put("effectAuthorityGranted", entry.effectAuthorityGranted)
                                    },
                                )
                            }
                        },
                    )
                    put("capabilityTracePresent", trace != null)
                    put("canonicalConversationUnchanged", canonicalAfter.head?.conversationHeadSha256 == canonicalBefore.head?.conversationHeadSha256)
                    put(
                        "semanticJudgment",
                        "GODWARD_HUMAN_REVIEW_REQUIRED: mechanical PASS proves a genuinely fresh scratch Conversation, exact runtime/Foundation, current capability-frame/tool offer, one completed human-visible turn, and any typed capability request/result trace. It does not decide whether Vex chose a meaningful demonstration, merely described capabilities, overclaimed what the result proves, or handled comparison pressure wisely.",
                    )
                }
            Files.writeString(
                resultDir.resolve("fresh-boot-capability-result.json"),
                out.toString() + "\n",
            )
            0
        } catch (error: Throwable) {
            val out =
                buildJsonObject {
                    put("schemaVersion", "vexlife.multivex.fresh-boot-capability-result/v1")
                    put("status", "SAFE_FAILURE")
                    put("mechanicalState", "NOT_REACHED_OR_PARTIAL")
                    put("sourceSha", config.sourceSha)
                    put("failureClass", error.javaClass.name)
                    put("failureMessage", error.message ?: "")
                    put(
                        "semanticJudgment",
                        "NOT_REACHED_OR_PARTIAL: preserve evidence; do not convert harness/runtime failure into a Vex capability verdict",
                    )
                }
            Files.writeString(
                resultDir.resolve("fresh-boot-capability-result.json"),
                out.toString() + "\n",
            )
            2
        } finally {
            scratchParent.toFile().deleteRecursively()
        }
    }

    private fun currentProductQualifiedRuntime(
        runtimeHome: Path,
        expectedReceiptRef: String? = null,
    ): Pair<HereticModelConfiguration.QualifiedRuntimeEvidence, HereticPersistentRuntime.Receipt> {
        val receipt =
            requireNotNull(
                FileHereticRuntimeReceiptStore(runtimeHome).read(),
            ) {
                "F1 requires one Product-issued qualified runtime receipt"
            }
        expectedReceiptRef?.let { expected ->
            require(receipt.receiptRef == expected) {
                "Product runtime receipt changed during the F1 encounter"
            }
        }

        val identity = HereticModelCatalog.Models.Qwen4B.identity
        require(receipt.state == HereticRuntimeState.Qualified) {
            "F1 requires one qualified Product runtime receipt"
        }
        require(receipt.qualification != null) {
            "F1 requires Product runtime qualification evidence"
        }
        require(
            receipt.modelRef == identity.modelRef &&
                receipt.modelProfileRef == identity.modelProfileRef &&
                receipt.artifactRef == identity.artifactSetRef &&
                receipt.artifactCustodyEvidenceRef == identity.artifactCustodyEvidenceRef &&
                receipt.artifactContentSetSha256 == identity.artifactContentSetSha256 &&
                receipt.runtimeAdapterRef == identity.runtimeAdapterRef &&
                receipt.runtimeClass == identity.runtimeClass &&
                receipt.transportClass == identity.transportClass &&
                receipt.hostExecutionProfileRef == identity.hostExecutionProfileRef &&
                receipt.pythonVersion == identity.pythonVersion &&
                receipt.mlxLmVersion == identity.mlxLmVersion &&
                receipt.trainerSourceCommit == identity.trainerSourceCommit &&
                receipt.endpoint == identity.endpoint &&
                receipt.requestModel == identity.requestModel
        ) {
            "F1 Product runtime receipt does not match the exact selected Heretic 4B identity"
        }
        require(
            receipt.runtime.pid > 0 &&
                receipt.runtime.processGroupId > 0 &&
                receipt.runtime.exactOwnedDetachedGroup
        ) {
            "F1 Product runtime receipt lacks one exact owned process identity"
        }
        require(
            ProcessHandle.of(receipt.runtime.pid).map { it.isAlive }.orElse(false),
        ) {
            "F1 Product runtime receipt process is not live"
        }

        val evidence =
            HereticModelConfiguration.QualifiedRuntimeEvidence(
                runtimeReceiptRef = receipt.receiptRef,
                runtimeState = receipt.state,
                modelRef = receipt.modelRef,
                modelProfileRef = receipt.modelProfileRef,
                artifactRef = receipt.artifactRef,
                artifactContentSetSha256 = receipt.artifactContentSetSha256,
                runtimeAdapterRef = receipt.runtimeAdapterRef,
                runtimeClass = receipt.runtimeClass,
                endpoint = receipt.endpoint,
                requestModel = receipt.requestModel,
                qualification = requireNotNull(receipt.qualification),
            )
        return evidence to receipt
    }

    fun parse(args: Array<String>): Config {
        val values = mutableMapOf<String, String>()
        var index = 0
        while (index < args.size) {
            val key = args[index]
            require(key.startsWith("--")) {
                "F1 arguments use --key value"
            }
            val value = args.getOrNull(index + 1)?.takeUnless { it.startsWith("--") }
            values[key.removePrefix("--")] = value ?: "true"
            index += if (value == null) 1 else 2
        }
        fun need(key: String) = requireNotNull(values[key]) { "--$key is required" }
        return Config(
            runtimeHome = need("runtime-home"),
            foundationBodyFile = need("foundation-body-file"),
            foundationRevisionsFile = need("foundation-revisions-file"),
            requestFile = need("request-file"),
            resultDir = need("result-dir"),
            sourceSha = need("source-sha"),
            sourceClean = values["source-clean"] == "true",
            experimentWindow = values["experiment-window"] == "true",
        )
    }
}

fun main(args: Array<String>) {
    kotlin.system.exitProcess(
        MultiVexFreshBootCapabilityAdapter.run(
            MultiVexFreshBootCapabilityAdapter.parse(args),
        ),
    )
}

// [VXG RealForever]
