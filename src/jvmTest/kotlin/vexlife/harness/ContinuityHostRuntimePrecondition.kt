package vexlife.harness

import java.nio.file.Path
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import vexlife.conversation.DirectConversationStore
import vexlife.local.VexLifeProdLocalSetup

/**
 * Host-proof precondition only.
 *
 * This is deliberately outside the continuity-pressure experiment. It may realize only the
 * already-selected Product runtime from already-present prerequisites. Acquisition is forbidden.
 * The canonical Conversation writer must already be IDLE; this command never repairs it.
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

        val session =
            VexLifeProdLocalSetup.establishSession(
                productHome = runtimeHome,
                allowAcquisition = false,
                allowRuntimeStart = true,
            )

        val current = session.observeCurrentQualifiedRuntimeEvidence()
        val afterWriter = DirectConversationStore(runtimeHome).threadWriterCurrentness()
        require(afterWriter.state == "IDLE" && !afterWriter.recoveryRequired) {
            "canonical Conversation writer changed during runtime precondition"
        }

        println(
            buildJsonObject {
                put("state", "PASS")
                put("effectClass", "HOST_SEMANTIC_MODEL_RUNTIME_PRECONDITION")
                put("productHome", session.result.productHome)
                put("runtimeDisposition", session.result.runtimeDisposition)
                put("modelBindingDisposition", session.result.modelBindingDisposition)
                put("runtimeReceiptRef", current.runtimeReceiptRef)
                put("modelRef", current.modelRef)
                put("endpoint", current.endpoint)
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
