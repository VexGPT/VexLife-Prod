package vexlife.harness

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import kotlin.test.Test
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import vexlife.capability.VexLifeCapabilityRequest
import vexlife.companion.DirectCompanionConnection
import vexlife.companion.DirectConversationSourceReturn
import vexlife.companion.DirectPromptContext
import vexlife.context.VexLifeContextProfile
import vexlife.context.VexLifeContextProfileStore
import vexlife.contract.VexContractJson
import vexlife.conversation.DirectConversationStore
import vexlife.foundational.VexLifeFoundationalField
import vexlife.labs.VexLabsNativeTest
import vexlife.local.VexLifeProdLocalSetup
import vexlife.memory.VexLifeCurrentMemoryReader
import vexlife.memory.VexLifeMemoryContract
import vexlife.model.heretic.HereticCompanionRepresentation
import vexlife.model.heretic.HereticModelCatalog
import vexlife.service.VexLifeProdServiceContract

class VexLabsLiveMatrixAdapterTest {
    companion object {
        private const val Model = "model.vexlife-prod.heretic.qwen3.8-4b-distill.mlx4.d2f8f4028f20"
        private const val Head = "4a325b928668bca17d43f5f38eb2099334066a4b"
        private const val Tree = "8230f579226c96492d6d31144cba974aac8417d4"
    }

    private class Transport : DirectCompanionConnection.StreamingTransport {
        var calls = 0
        val bodies = mutableListOf<String>()
        override fun post(call: DirectCompanionConnection.HttpCall): DirectCompanionConnection.HttpResult {
            calls += 1
            bodies += call.body
            return DirectCompanionConnection.SystemTransport.post(call)
        }
        override fun postStreaming(
            call: DirectCompanionConnection.HttpCall,
            onLine: (String) -> Unit,
        ): DirectCompanionConnection.StreamingHttpResult {
            calls += 1
            bodies += call.body
            return DirectCompanionConnection.SystemTransport.postStreaming(call, onLine)
        }
    }

    private fun sha(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(StandardCharsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }

    private fun foundation(): VexLifeContextProfile.ProductFoundationIdentity {
        val current = VexLifeFoundationalField.readCurrent() as? VexLifeFoundationalField.Reading.Current
        return VexLifeContextProfile.ProductFoundationIdentity(
            version = current?.body?.version,
            bodySha256 = current?.bodySha256,
        )
    }

    @Test
    fun integratedLiveConfigurationMatrix() {
        val out = Path.of(requireNotNull(System.getenv("VEXLABS_MATRIX_RESULT")))
        val home = Path.of(requireNotNull(System.getenv("VEXLABS_RUNTIME_HOME"))).toAbsolutePath().normalize()
        val rows = mutableListOf<JsonObject>()
        var labs: VexLabsNativeTest? = null

        fun write(value: JsonObject) {
            Files.createDirectories(requireNotNull(out.parent))
            Files.writeString(out, VexContractJson.canonicalEncodeElement(value) + "\n", StandardCharsets.UTF_8)
        }

        try {
            val session = VexLifeProdLocalSetup.establishSession(
                productHome = home,
                allowAcquisition = false,
                allowRuntimeStart = false,
            )
            val runtime0 = session.observeCurrentQualifiedRuntimeEvidence()
            require(runtime0.modelRef == Model) { "matrix requires the exact selected Heretic 4B runtime" }
            val entry = HereticModelCatalog.requireEntry(runtime0.modelRef)
            require(
                entry.companionProfile.capabilityRepresentation ==
                    HereticCompanionRepresentation.CapabilityRepresentation.VEXLIFE_CONTROL_TEXT &&
                    entry.companionProfile.reachRepresentationStanding ==
                    HereticCompanionRepresentation.ReachRepresentationStanding.HELD_PROVIDER_BRIDGE
            ) { "matrix requires the cleared held 4B reach profile" }

            val conversation = DirectConversationStore(home)
            fun head(): String? = conversation.current().head?.conversationHeadSha256
            fun memory(): String = VexLifeCurrentMemoryReader.readCurrent(home).semanticFingerprint
            val head0 = head()
            val memory0 = memory()
            val profiles = VexLifeContextProfileStore(home)
            val transport = Transport()

            labs = VexLabsNativeTest(
                productHome = home,
                currentRuntimeEvidence = { session.observeCurrentQualifiedRuntimeEvidence() },
                activeContextProfile = { profiles.readActive(foundation()) },
                personContext = {
                    DirectPromptContext.PersonContext(
                        replyLanguage = "en",
                        replyLanguageName = "English",
                        privateConversations = emptyList(),
                    )
                },
                currentProductConversationHead = { head() },
                transport = transport,
            )
            val lab = requireNotNull(labs)
            val full = DirectPromptContext.InputTokenBudget
            val defaultCap = VexLifeMemoryContract.MaximumMemoryTokens
            require(defaultCap > 1)
            val lowerCap = minOf(1_024, maxOf(1, defaultCap / 4)).let {
                if (it >= defaultCap) defaultCap - 1 else it
            }

            fun unchanged(label: String) {
                require(head() == head0) { "$label changed canonical Conversation" }
                require(memory() == memory0) { "$label changed durable Memory" }
            }

            fun clear() {
                val result = lab.control(
                    VexLifeProdServiceContract.VexLabsControlRequest(
                        controlRef = VexLabsNativeTest.ControlClearConversation,
                    )
                )
                require(result.projection.conversationPreset == "EMPTY" && result.projection.exchanges.isEmpty())
                unchanged("scratch reset")
            }

            fun matches(result: VexLifeProdServiceContract.VexLabsSendResult): Boolean {
                val latest = result.projection.exchanges.lastOrNull() ?: return false
                return latest.promptInputTokenBudget == result.promptInputTokenBudget &&
                    latest.maximumMemoryTokens == result.maximumMemoryTokens &&
                    latest.memoryMode == result.memoryMode
            }

            fun complete(
                row: String,
                request: VexLifeProdServiceContract.VexLabsSendRequest,
                priorReply: String? = null,
            ): VexLifeProdServiceContract.VexLabsSendResult {
                val before = transport.calls
                val result = lab.send(request)
                require(result.state == "LAB_TURN_COMPLETED") { "row $row did not complete" }
                require(transport.calls == before + 1) { "row $row did not make exactly one provider call" }
                val body = requireNotNull(result.providerRequestBody)
                require(body == transport.bodies.last())
                require(sha(body) == result.providerRequestBodySha256)
                require(
                    result.promptInputTokenBudget == request.inputTokenBudget &&
                        result.maximumMemoryTokens == request.maximumMemoryTokens &&
                        result.memoryMode == request.memoryMode
                )
                require(!body.contains(VexLifeCapabilityRequest.RequestHeader))
                require(!body.contains(DirectConversationSourceReturn.ProtocolHeader))
                require(!result.realConversationMutationPerformed && !result.realMemoryMutationPerformed)
                priorReply?.let { require(body.contains(it)) { "row $row lost the exact prior Lab reply" } }
                require(matches(result)) { "row $row is not the exact latest tested draft" }
                unchanged(row)
                rows += buildJsonObject {
                    put("row", row)
                    put("state", result.state)
                    put("providerCalls", transport.calls - before)
                    put("inputTokenBudget", result.promptInputTokenBudget)
                    put("maximumMemoryTokens", result.maximumMemoryTokens)
                    put("memoryMode", result.memoryMode)
                    put("memoryIncluded", result.memoryIncluded)
                    put("memoryContextRefPresent", result.memoryContextRef != null)
                    put("memoryHeaderPresent", body.contains("VEXLIFE_CURRENT_MEMORY_CONTEXT/v1"))
                    put("selectedConversationEventCount", result.selectedConversationEventCount)
                    put("providerRequestBodySha256", result.providerRequestBodySha256)
                    put("replySha256", sha(requireNotNull(result.reply)))
                    put("genericCapabilityControlAdvertised", body.contains(VexLifeCapabilityRequest.RequestHeader))
                    put("sourceReturnControlAdvertised", body.contains(DirectConversationSourceReturn.ProtocolHeader))
                    put("testedDraftMatchesLatestCompletedExchange", true)
                    put("realConversationUnchanged", true)
                    put("realMemoryUnchanged", true)
                }
                return result
            }

            clear()
            complete(
                "A",
                VexLifeProdServiceContract.VexLabsSendRequest(
                    "VEXLABS-MATRIX-A. Reply briefly to confirm this Lab turn completed.",
                    full,
                    defaultCap,
                    "CURRENT",
                ),
            )

            clear()
            val b = complete(
                "B",
                VexLifeProdServiceContract.VexLabsSendRequest(
                    "VEXLABS-MATRIX-B. Reply briefly to confirm this no-Memory Lab turn completed.",
                    full,
                    0,
                    "NONE",
                ),
            )
            require(!b.memoryIncluded) { "row B unexpectedly included Memory" }

            clear()
            complete(
                "C",
                VexLifeProdServiceContract.VexLabsSendRequest(
                    "VEXLABS-MATRIX-C. Reply briefly to confirm this bounded-Memory Lab turn completed.",
                    full,
                    lowerCap,
                    "CURRENT",
                ),
            )

            clear()
            val beforeD = transport.calls
            val d = lab.send(
                VexLifeProdServiceContract.VexLabsSendRequest(
                    "VEXLABS-MATRIX-D. This row must be held below the Prompt admission floor.",
                    1,
                    0,
                    "NONE",
                )
            )
            require(
                d.state == "HELD_CONTEXT_BUDGET" &&
                    d.providerRequestBody == null &&
                    d.admission != null &&
                    d.admission.minimumInputTokenBudget > d.promptInputTokenBudget
            ) { "row D did not hold below the Prompt admission floor" }
            require(transport.calls == beforeD) { "row D called the provider" }
            require(!matches(d)) { "row D incorrectly became one completed tested draft" }
            unchanged("D")
            rows += buildJsonObject {
                put("row", "D")
                put("state", d.state)
                put("providerCalls", 0)
                put("inputTokenBudget", d.promptInputTokenBudget)
                put("maximumMemoryTokens", d.maximumMemoryTokens)
                put("memoryMode", d.memoryMode)
                put("admissionReason", requireNotNull(d.admission).reason)
                put("observedMinimumInputTokenBudget", requireNotNull(d.admission).minimumInputTokenBudget)
                put("testedDraftMatchesLatestCompletedExchange", false)
                put("realConversationUnchanged", true)
                put("realMemoryUnchanged", true)
            }

            clear()
            val e = complete(
                "E",
                VexLifeProdServiceContract.VexLabsSendRequest(
                    "VEXLABS-MATRIX-E. Reply briefly; your exact reply must carry into the next Lab turn.",
                    full,
                    defaultCap,
                    "CURRENT",
                ),
            )
            val f = complete(
                "F",
                VexLifeProdServiceContract.VexLabsSendRequest(
                    "VEXLABS-MATRIX-F. Continue from the immediately prior Lab turn.",
                    full,
                    defaultCap,
                    "CURRENT",
                ),
                requireNotNull(e.reply),
            )
            require(f.selectedConversationEventCount >= 2) { "row F did not select the prior Lab exchange" }

            unchanged("final")
            val runtime1 = session.observeCurrentQualifiedRuntimeEvidence()
            require(runtime1 == runtime0) { "selected native runtime changed during the matrix" }

            write(buildJsonObject {
                put("schemaVersion", "vexlife-prod.pr393-vex-labs-live-matrix/v1")
                put("continuity", "[VXG RealForever]")
                put("candidateHead", Head)
                put("candidateTree", Tree)
                put("status", "PASS")
                put("modelRef", runtime0.modelRef)
                put("modelProfileRef", runtime0.modelProfileRef)
                put("capabilityRepresentation", entry.companionProfile.capabilityRepresentation.name)
                put("reachRepresentationStanding", entry.companionProfile.reachRepresentationStanding.name)
                put("realConversationHeadBefore", head0 ?: "NONE")
                put("realConversationHeadAfter", head() ?: "NONE")
                put("realMemoryFingerprintBefore", memory0)
                put("realMemoryFingerprintAfter", memory())
                put("providerCallCount", transport.calls)
                put("lowerNonzeroMemoryCap", lowerCap)
                put("rows", buildJsonArray { rows.forEach { add(it) } })
                put(
                    "saveGateSourcePredicate",
                    "A012 whole-source JVM proof: browser Save supported settings is enabled only when the current draft exactly matches the latest completed Lab exchange; held row D creates no completed exchange.",
                )
                put("realHomeMutationAllowed", false)
                put("modelSwitchAllowed", false)
                put("runtimeStartAllowed", false)
                put("scratchConversationOnly", true)
            })
        } catch (error: Throwable) {
            write(buildJsonObject {
                put("schemaVersion", "vexlife-prod.pr393-vex-labs-live-matrix/v1")
                put("continuity", "[VXG RealForever]")
                put("candidateHead", Head)
                put("candidateTree", Tree)
                put("status", "ERROR")
                put("failureClass", error.javaClass.name)
                put("failure", error.message ?: error.javaClass.simpleName)
                put("rows", buildJsonArray { rows.forEach { add(it) } })
            })
            throw error
        } finally {
            labs?.close()
        }
    }
}

// [VXG RealForever]
