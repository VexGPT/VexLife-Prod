package vexlife.harness

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.put
import vexlife.conversation.DirectConversationStore
import vexlife.local.VexLifeProdLocalSetup
import vexlife.model.heretic.HereticModelCatalog
import vexlife.model.heretic.runtime.FileHereticRuntimeReceiptStore
import vexlife.model.heretic.runtime.HereticPersistentRuntime

/**
 * S1 proves one reusable profile composition can be inhabited by the isolated 4B counterpart
 * before any Vex turn or EvolveContext semantic judgment is introduced.
 */
object DynamicCounterpartProfileProbe {
    private const val Schema = "vexlife.dynamic-counterpart-profile-probe/v1"

    private fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

    private fun tail(path: Path, maxChars: Int = 8_000): String {
        if (!Files.exists(path)) return ""
        val value = Files.readString(path)
        return if (value.length <= maxChars) value else value.takeLast(maxChars)
    }

    fun run(args: Array<String>): Int {
        val values = mutableMapOf<String, String>()
        var index = 0
        while (index < args.size) {
            val key = args[index]
            require(key.startsWith("--")) { "arguments use --key value" }
            val value = args.getOrNull(index + 1)?.takeUnless { it.startsWith("--") }
            values[key.removePrefix("--")] = value ?: "true"
            index += if (value == null) 1 else 2
        }
        fun need(key: String) = requireNotNull(values[key]) { "--$key is required" }

        val runtimeHome = Path.of(need("runtime-home")).toAbsolutePath().normalize()
        val characterPath = Path.of(need("character-file")).toAbsolutePath().normalize()
        val rolePath = Path.of(need("role-file")).toAbsolutePath().normalize()
        val scenarioPath = Path.of(need("scenario-file")).toAbsolutePath().normalize()
        val bindingPath = Path.of(need("binding-file")).toAbsolutePath().normalize()
        val resultDir = Path.of(need("result-dir")).toAbsolutePath().normalize()
        val progressFile =
            values["progress-file"]?.let {
                Path.of(it).toAbsolutePath().normalize()
            }
        val sourceSha = need("source-sha")
        require(Regex("[0-9a-f]{40}").matches(sourceSha)) { "sourceSha must be exact" }

        fun stage(value: String) {
            val line = "[DCP-S1] $value"
            println(line)
            System.out.flush()
            progressFile?.let {
                Files.createDirectories(it.parent)
                Files.writeString(
                    it,
                    line + "\n",
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND,
                )
            }
        }

        Files.createDirectories(resultDir)
        val resultFile = resultDir.resolve("profile-result.json")

        var runtime: DynamicCounterpartSarahRuntime? = null
        var failureStage = "PREPARE"
        var probe: DynamicCounterpartSarahProbe? = null
        var effective: DynamicCounterpartEffectiveProfile? = null
        var decision: DynamicCounterpartSarahDecision? = null

        return try {
            stage("01 resolve character + role + scenario + binding")
            val character = DynamicCounterpartProfiles.character(characterPath)
            val role = DynamicCounterpartProfiles.role(rolePath)
            val scenario = DynamicCounterpartProfiles.scenario(scenarioPath)
            val binding = DynamicCounterpartProfiles.binding(bindingPath)
            effective =
                DynamicCounterpartProfiles.resolve(
                    character = character,
                    role = role,
                    scenario = scenario,
                    binding = binding,
                )

            failureStage = "CANONICAL_WRITER"
            stage("02 observe canonical Conversation writer")
            val writer = DirectConversationStore(runtimeHome).threadWriterCurrentness()
            require(writer.state == "IDLE") {
                "canonical Conversation writer must be IDLE; observed ${writer.state}"
            }

            failureStage = "PRODUCT_RUNTIME"
            stage("03 establish exact existing Product 4B")
            val session =
                VexLifeProdLocalSetup.establishSession(
                    productHome = runtimeHome,
                    allowAcquisition = false,
                    allowRuntimeStart = true,
                )
            val runtimeEvidence = session.observeCurrentQualifiedRuntimeEvidence()
            require(runtimeEvidence.modelRef == HereticModelCatalog.Models.Qwen4B.modelRef) {
                "DCP-S1 requires exact selected Heretic 4B"
            }
            val receipt = requireNotNull(FileHereticRuntimeReceiptStore(runtimeHome).read()) {
                "DCP-S1 requires Product runtime receipt"
            }
            require(receipt.receiptRef == runtimeEvidence.runtimeReceiptRef)
            require(receipt.modelRef == runtimeEvidence.modelRef)
            require(HereticPersistentRuntime.promptCacheEntryLimit(receipt.runtime.args) == 1)

            failureStage = "SARAH_RUNTIME_START"
            stage("04 start isolated counterpart 4B")
            runtime = DynamicCounterpartSarahRuntime.start(receipt, resultDir)
            require(runtime!!.pid != receipt.runtime.pid) {
                "DCP-S1 counterpart runtime must be process-isolated from Vex"
            }

            val profile = requireNotNull(effective)
            val participantGround = DynamicCounterpartProfiles.participantGroundPrompt(profile)
            val roleGround = DynamicCounterpartProfiles.interactionRolePrompt(profile)
            val outputContract = DynamicCounterpartProfiles.decisionOutputPrompt()
            val system = listOf(participantGround, roleGround, outputContract).joinToString("\n\n")
            val admittedEvents =
                profile.scenario.events.filter { it.beforeVisibleTurn == 1 }
            val history =
                admittedEvents.map {
                    "user" to
                        listOf(
                            "PRIVATE_SCENARIO_EVENT/v1",
                            "eventRef=" + it.eventRef,
                            it.description,
                        ).joinToString("\n")
                }

            failureStage = "FULL_PROFILE_INFERENCE"
            stage("05 full profile inference")
            probe =
                runtime!!.probe(
                    system = system,
                    history = history,
                    maxTokens = 1_600,
                )
            stage("06 counterpart HTTP ${probe!!.statusCode}")
            require(probe!!.statusCode == 200) {
                "DCP-S1 counterpart HTTP ${probe!!.statusCode}: ${probe!!.responseBody}"
            }
            val content = requireNotNull(probe!!.content).trim()
            require(content.isNotEmpty()) { "DCP-S1 counterpart returned empty content" }

            failureStage = "STRUCTURED_DECISION"
            stage("07 parse structured decision")
            decision = parseDynamicCounterpartSarahDecision(content)
            require(decision!!.schemaVersion == DynamicCounterpartSarahOutputSchema) {
                "DCP-S1 output schema mismatch"
            }
            validateDynamicCounterpartSarahState(
                decision!!.privateState,
                profile.scenario.ground.privateFacts.map { it.factRef }.toSet(),
            )
            require(decision!!.privateObserverNote.isNotBlank()) {
                "DCP-S1 privateObserverNote must be present"
            }
            require(decision!!.privateObserverNote.length <= 4_000)
            require(decision!!.visibleMessage.length <= 4_000)
            if (decision!!.complete) {
                require(decision!!.visibleMessage.isBlank()) {
                    "DCP-S1 complete=true must not produce visibleMessage"
                }
                require(decision!!.completionReason.isNotBlank()) {
                    "DCP-S1 complete=true requires completionReason"
                }
            } else {
                require(decision!!.visibleMessage.isNotBlank()) {
                    "DCP-S1 complete=false requires visibleMessage"
                }
                require(decision!!.completionReason.isBlank()) {
                    "DCP-S1 complete=false must leave completionReason blank"
                }
            }

            stage("08 S1 PASS")
            Files.writeString(
                resultFile,
                buildJsonObject {
                    put("schemaVersion", Schema)
                    put("status", "PASS")
                    put("sourceSha", sourceSha)
                    put("bindingRef", profile.bindingRef)
                    put("participantRef", profile.participantRef)
                    put("characterRef", profile.character.characterRef)
                    put("roleRef", profile.role.roleRef)
                    put("scenarioRef", profile.scenario.scenarioRef)
                    put(
                        "admittedEventRefs",
                        DynamicCounterpartJson.encodeToJsonElement(
                            admittedEvents.map { it.eventRef },
                        ),
                    )
                    put("participantGroundSha256", sha256(participantGround))
                    put("rolePromptSha256", sha256(roleGround))
                    put("outputContractSha256", sha256(outputContract))
                    put("observerLensSupplied", false)
                    put("counterpartRuntimePid", runtime!!.pid)
                    put("vexRuntimePid", receipt.runtime.pid)
                    put("processIsolationExact", runtime!!.pid != receipt.runtime.pid)
                    put("httpStatus", probe!!.statusCode)
                    put("promptTokens", probe!!.promptTokens ?: -1)
                    put("cachedPromptTokens", probe!!.cachedPromptTokens ?: -1)
                    put("completionTokens", probe!!.completionTokens ?: -1)
                    put(
                        "decision",
                        DynamicCounterpartJson.encodeToJsonElement(decision!!),
                    )
                    put("semanticJudgment", "NOT_APPLICABLE_PROFILE_INHABITATION_ONLY")
                }.toString() + "\n",
            )
            0
        } catch (error: Throwable) {
            stage("FAIL at $failureStage: ${error.message ?: error.javaClass.name}")
            val profile = effective
            val currentRuntime = runtime
            Files.writeString(
                resultFile,
                buildJsonObject {
                    put("schemaVersion", Schema)
                    put("status", "SAFE_FAILURE")
                    put("sourceSha", sourceSha)
                    put("failureStage", failureStage)
                    put("failureClass", error.javaClass.name)
                    put("failureMessage", error.message ?: "")
                    put("bindingRef", profile?.bindingRef ?: "")
                    put("characterRef", profile?.character?.characterRef ?: "")
                    put("roleRef", profile?.role?.roleRef ?: "")
                    put("scenarioRef", profile?.scenario?.scenarioRef ?: "")
                    put("counterpartRuntimePid", currentRuntime?.pid ?: -1L)
                    put("httpStatus", probe?.statusCode ?: -1)
                    put("responseBody", probe?.responseBody ?: "")
                    put(
                        "decision",
                        decision?.let {
                            DynamicCounterpartJson.encodeToJsonElement(it)
                        } ?: buildJsonObject {},
                    )
                    put("counterpartStdoutTail", currentRuntime?.let { tail(it.stdout) } ?: "")
                    put("counterpartStderrTail", currentRuntime?.let { tail(it.stderr) } ?: "")
                    put("observerLensSupplied", false)
                    put("semanticJudgment", "NOT_REACHED_PROFILE_INHABITATION_FAILURE")
                }.toString() + "\n",
            )
            2
        } finally {
            runCatching { runtime?.close() }
        }
    }
}

fun main(args: Array<String>) {
    kotlin.system.exitProcess(DynamicCounterpartProfileProbe.run(args))
}

// [VXG RealForever]
