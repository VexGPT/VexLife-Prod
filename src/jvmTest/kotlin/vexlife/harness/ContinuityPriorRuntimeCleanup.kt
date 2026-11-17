package vexlife.harness

import java.nio.file.Path
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import vexlife.conversation.DirectConversationStore
import vexlife.local.VexLifeProdLocalSetup
import vexlife.model.heretic.HereticModelCatalog
import vexlife.model.heretic.HereticProductIdentity
import vexlife.model.heretic.runtime.FileHereticRuntimeReceiptStore
import vexlife.model.heretic.runtime.HereticArtifactMaterialization
import vexlife.model.heretic.runtime.HereticPersistentRuntime
import vexlife.model.heretic.runtime.HereticPythonEnvironment
import vexlife.model.heretic.runtime.MacHereticRuntimePlatform
import vexlife.platform.python.ManagedPython

/**
 * Host-proof recovery for the exact Product runtime generation that created the current stale 27B
 * receipt. It performs no model selection, acquisition or Conversation effect. The exact old owner
 * must prove the receipt belongs to its certified 27B binding before it can retire/delete it.
 */
object ContinuityPriorRuntimeCleanup {
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
            "canonical Conversation writer must be IDLE before prior-runtime cleanup"
        }

        val store = FileHereticRuntimeReceiptStore(runtimeHome)
        val prior = requireNotNull(store.read()) { "no Product runtime receipt exists to clean up" }
        val targetModel = HereticModelCatalog.Certified27B
        require(prior.modelRef == targetModel.modelRef) {
            "prior-runtime cleanup is bound only to the exact certified 27B receipt; observed=" +
                prior.modelRef
        }

        val managedPython =
            ManagedPython.ensure(
                productHome = runtimeHome,
                pythonVersion = HereticProductIdentity.PythonVersion,
                allowAcquisition = false,
            )
        val materialized =
            HereticArtifactMaterialization.materializeCurrent(
                productHome = runtimeHome,
                allowAcquisition = false,
                acquirer =
                    HereticArtifactMaterialization.Acquirer { _, _ ->
                        error("prior-runtime cleanup never acquires model artifacts")
                    },
                model = targetModel,
            )
        val environment =
            HereticPythonEnvironment.ensure(
                managedPython = managedPython,
                productHome = runtimeHome,
                allowCreation = false,
            )
        val binding =
            HereticPersistentRuntime.bind(
                authorityRef = VexLifeProdLocalSetup.RuntimeAuthorityRef,
                materialization = materialized,
                environment = environment,
            )

        val stopped =
            HereticPersistentRuntime.stop(
                binding = binding,
                store = store,
                platform = MacHereticRuntimePlatform(productHome = runtimeHome),
                requireCurrentImplementation = true,
            )

        val afterWriter = DirectConversationStore(runtimeHome).threadWriterCurrentness()
        require(afterWriter.state == "IDLE" && !afterWriter.recoveryRequired) {
            "canonical Conversation writer changed during prior-runtime cleanup"
        }

        println(
            buildJsonObject {
                put("state", "PASS")
                put("effectClass", "EXACT_PRIOR_PRODUCT_RUNTIME_CLEANUP")
                put("priorReceiptRef", prior.receiptRef)
                put("priorModelRef", prior.modelRef)
                put("priorPid", prior.runtime.pid)
                put("stopDisposition", stopped.disposition)
                put("processTerminationPerformed", stopped.processTerminationPerformed)
                put("verifiedNotLive", stopped.verifiedNotLive)
                put("receiptRemaining", store.read() != null)
                put("acquisitionAllowed", false)
                put("modelSelectionChanged", false)
                put("conversationWriterBefore", beforeWriter.state)
                put("conversationWriterAfter", afterWriter.state)
            }
        )
    }
}

fun main(args: Array<String>) = ContinuityPriorRuntimeCleanup.main(args)

// [VXG RealForever]
