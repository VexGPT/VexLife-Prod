package vexlife.harness

import java.nio.file.Path
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import vexlife.conversation.DirectConversationStore
import vexlife.home.VexLifeProdHomeFiles
import vexlife.local.VexLifeProdLocalSetup
import vexlife.model.heretic.HereticModelCatalog
import vexlife.model.heretic.runtime.FileHereticModelConfigurationStore

/**
 * Host-proof precondition only.
 *
 * This is deliberately outside the continuity-pressure experiment. It selects the existing
 * catalog-owned Qwen4B relationship through the Product model-configuration owner, then may realize
 * that Product runtime only from already-present prerequisites. Acquisition is forbidden. The
 * canonical Conversation writer must already be IDLE; this command never repairs it.
 */
object ContinuityHostRuntimePrecondition {
    @JvmStatic
    fun main(args: Array<String>) {
        val map = mutableMapOf<String, String>()
        var index = 0
        while (index < args.size) {
            val key = args[index]
            require(key.startsWith("--")) { "arguments are --key value" }
            val name = key.removePrefix("--")
            val value = args.getOrNull(index + 1)
            require(value != null && !value.startsWith("--")) { "--$name requires a value" }
            map[name] = value
            index += 2
        }

        val runtimeHome =
            Path.of(requireNotNull(map["runtime-home"]) { "--runtime-home is required" })
                .also { require(it.isAbsolute) { "--runtime-home must be absolute" } }
                .normalize()

        val beforeWriter = DirectConversationStore(runtimeHome).threadWriterCurrentness()
        require(beforeWriter.state == "IDLE" && !beforeWriter.recoveryRequired) {
            "canonical Conversation writer must be IDLE before runtime precondition; no recovery performed"
        }

        val home =
            requireNotNull(VexLifeProdHomeFiles.observeExisting(runtimeHome)) {
                "selected canonical Vex Home is absent"
            }
        val modelConfiguration = FileHereticModelConfigurationStore(runtimeHome)
        val selectedBefore =
            modelConfiguration.read(home.manifest)
                as? FileHereticModelConfigurationStore.Snapshot.Heretic
                ?: error("selected Vex Home has no Product runtime model selection")
        val targetModel = HereticModelCatalog.Models.Qwen4B
        val selection =
            modelConfiguration.selectModel(
                manifest = home.manifest,
                model = targetModel,
            )

        /*
         * The host-proof precondition is explicitly selecting the Product-owned runtime route.
         * A non-borrowable historical stable predecessor remains untouched as evidence; it is not
         * repaired, deleted, retired or reclassified here. Product-owned runtime start still
         * performs its own exact process census and port-occupancy checks before any spawn.
         */
        val result =
            VexLifeProdLocalSetup.establishAt(
                productHome = runtimeHome,
                allowAcquisition = false,
                allowRuntimeStart = true,
                hooks =
                    VexLifeProdLocalSetup.Hooks(
                        observeStableRuntime = { _, _ -> null },
                    ),
            )
        val current = result.qualifiedRuntimeEvidence
        require(current.modelRef == targetModel.modelRef) {
            "runtime precondition did not realize the exact target Product model"
        }

        val afterWriter = DirectConversationStore(runtimeHome).threadWriterCurrentness()
        require(afterWriter.state == "IDLE" && !afterWriter.recoveryRequired) {
            "canonical Conversation writer changed during runtime precondition"
        }

        println(
            buildJsonObject {
                put("state", "PASS")
                put("effectClass", "HOST_SEMANTIC_MODEL_RUNTIME_PRECONDITION")
                put("productHome", result.productHome)
                put("runtimeDisposition", result.runtimeDisposition)
                put("modelBindingDisposition", result.modelBindingDisposition)
                put("runtimeReceiptRef", current.runtimeReceiptRef)
                put("modelRef", current.modelRef)
                put("targetModelRef", targetModel.modelRef)
                put("targetModelProfileRef", targetModel.identity.modelProfileRef)
                put("targetArtifactSetRef", targetModel.identity.artifactSetRef)
                put("targetCompanionProfileRef", targetModel.companionProfile.profileRef)
                put("priorModelRef", selectedBefore.value.modelRef)
                put("modelSelectionDisposition", selection.state)
                put("endpoint", current.endpoint)
                put("stablePredecessorBorrowAttempted", false)
                put("stablePredecessorMutationPerformed", false)
                put("acquisitionAllowed", false)
                put("runtimeStartAllowed", true)
                put("conversationWriterBefore", beforeWriter.state)
                put("conversationWriterAfter", afterWriter.state)
                put("continuityExperimentPerformed", false)
            }
        )
    }
}

fun main(args: Array<String>) = ContinuityHostRuntimePrecondition.main(args)

// [VXG RealForever]
