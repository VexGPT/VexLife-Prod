package vexlife.harness

import java.nio.file.Files
import java.nio.file.Path
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import vexlife.companion.DirectCompanionConnection
import vexlife.companion.DirectPromptContext
import vexlife.conversation.DirectConversationStore
import vexlife.home.CanonicalVexLifeHomeFixture
import vexlife.local.VexLifeProdLocalSetup
import vexlife.model.heretic.HereticModelCatalog
import vexlife.model.heretic.runtime.FileHereticRuntimeReceiptStore
import vexlife.model.heretic.runtime.HereticPersistentRuntime

/**
 * Smallest lived prerequisite for DCP-1.
 *
 * This does not test continuity, EvolveContext correctness, Memory, Dream or Sarah's scenario.
 * It proves only that two isolated 4B roles can take turns through the real Product path:
 *
 * Sarah -> Vex -> same Sarah context.
 */
object DynamicCounterpartSequenceProbe {
    private const val Schema = "vexlife.dynamic-counterpart-sequence-probe/v1"

    private fun stage(name: String) {
        println("[DCP-S0] $name")
        System.out.flush()
    }

    private fun tail(path: Path, maxChars: Int = 8_000): String {
        if (!Files.exists(path)) return ""
        val text = Files.readString(path)
        return if (text.length <= maxChars) text else text.takeLast(maxChars)
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
        val resultDir = Path.of(need("result-dir")).toAbsolutePath().normalize()
        val sourceSha = need("source-sha")
        require(Regex("[0-9a-f]{40}").matches(sourceSha)) { "sourceSha must be exact" }
        Files.createDirectories(resultDir)
        val resultFile = resultDir.resolve("sequence-result.json")

        val character =
            DynamicCounterpartJson.decodeFromString<DynamicCounterpartCharacter>(
                Files.readString(characterPath),
            )

        var sarahRuntime: DynamicCounterpartSarahRuntime? = null
        var scratchParent: Path? = null
        var first: DynamicCounterpartSarahProbe? = null
        var second: DynamicCounterpartSarahProbe? = null
        var vexReply: String? = null
        var vexPid: Long? = null
        var failureStage = "PREPARE"

        return try {
            stage("01 observe canonical writer")
            val writer = DirectConversationStore(runtimeHome).threadWriterCurrentness()
            require(writer.state == "IDLE") {
                "canonical Conversation writer must be IDLE; observed ${writer.state}"
            }

            failureStage = "VEX_RUNTIME"
            stage("02 establish exact Product 4B")
            val session =
                VexLifeProdLocalSetup.establishSession(
                    productHome = runtimeHome,
                    allowAcquisition = false,
                    allowRuntimeStart = true,
                )
            val runtimeEvidence = session.observeCurrentQualifiedRuntimeEvidence()
            require(runtimeEvidence.modelRef == HereticModelCatalog.Models.Qwen4B.modelRef) {
                "DCP-S0 requires exact selected Heretic 4B"
            }
            val receipt = requireNotNull(FileHereticRuntimeReceiptStore(runtimeHome).read()) {
                "DCP-S0 requires Product runtime receipt"
            }
            require(receipt.receiptRef == runtimeEvidence.runtimeReceiptRef)
            require(receipt.modelRef == runtimeEvidence.modelRef)
            require(HereticPersistentRuntime.promptCacheEntryLimit(receipt.runtime.args) == 1)
            vexPid = receipt.runtime.pid

            failureStage = "SCRATCH_HOME"
            stage("03 form proof-owned Sarah scratch relationship")
            scratchParent = Files.createTempDirectory(resultDir, "sequence-scratch-")
            val scratchHome = scratchParent!!.resolve(".vexlife")
            CanonicalVexLifeHomeFixture.form(
                home = scratchHome,
                deviceSuffix = "dynamic-counterpart-sequence-device",
                lineageSuffix = "dynamic-counterpart-sequence-lineage",
                personRef = character.participantRef,
                familyRef = "vex-family.synthetic-dcp-sequence",
                deviceName = "Synthetic DCP sequence host",
            )
            val personContext =
                DirectPromptContext.PersonContext(
                    replyLanguage = "en",
                    replyLanguageName = "English",
                    privateConversations = emptyList(),
                    nameClaim =
                        DirectPromptContext.PersonNameClaim(
                            name = character.displayName,
                            standing =
                                DirectPromptContext.PersonNameStanding.valueOf(
                                    character.identityStanding,
                                ),
                            sourceRefs = character.identitySourceRefs,
                        ),
                )

            failureStage = "SARAH_RUNTIME_START"
            stage("04 start isolated Sarah 4B")
            sarahRuntime = DynamicCounterpartSarahRuntime.start(receipt, resultDir)
            require(sarahRuntime!!.pid != receipt.runtime.pid) {
                "Sarah and Vex must have distinct process identities"
            }
            stage("05 Sarah runtime healthy pid=${sarahRuntime!!.pid}; Vex pid=${receipt.runtime.pid}")

            val system =
                """
                DCP_SEQUENCE_PROBE/v1
                You are Sarah in a tiny transport/turn-taking probe.
                This probe is not judging Vex and carries no hidden continuity scenario.
                Speak naturally and briefly. Do not mention the test machinery unless asked.
                """.trimIndent()

            failureStage = "SARAH_FIRST_INFERENCE"
            stage("06 Sarah inference #1")
            first =
                sarahRuntime!!.probe(
                    system = system,
                    history =
                        listOf(
                            "user" to
                                "Send Vex one short natural hello as Sarah and ask him one simple question.",
                        ),
                )
            stage("07 Sarah inference #1 HTTP ${first!!.statusCode}")
            require(first!!.statusCode == 200) {
                "Sarah first inference HTTP ${first!!.statusCode}: ${first!!.responseBody}"
            }
            val firstMessage = requireNotNull(first!!.content).trim()
            require(firstMessage.isNotEmpty()) { "Sarah first inference returned empty content" }

            failureStage = "VEX_TURN"
            stage("08 Sarah -> Vex ordinary Product turn")
            val vexResult =
                performDynamicCounterpartVexTurn(
                    scratchHome = scratchHome,
                    evidence = runtimeEvidence,
                    content = firstMessage,
                    idempotencyKey = "send.dynamic-counterpart.sequence.vex.1",
                    personContext = personContext,
                    hooks =
                        dynamicCounterpartHooks(
                            session,
                            DirectCompanionConnection.SystemTransport,
                        ),
                )
            vexReply = vexResult.content
            require(vexReply!!.isNotBlank()) { "Vex sequence probe returned empty content" }
            stage("09 Vex -> Sarah visible reply received")

            failureStage = "SARAH_SECOND_INFERENCE"
            stage("10 Sarah inference #2 using same process/context history")
            second =
                sarahRuntime!!.probe(
                    system = system,
                    history =
                        listOf(
                            "user" to
                                "Send Vex one short natural hello as Sarah and ask him one simple question.",
                            "assistant" to firstMessage,
                            "user" to "VISIBLE_VEX_REPLY/v1\n$vexReply",
                        ),
                )
            stage("11 Sarah inference #2 HTTP ${second!!.statusCode}")
            require(second!!.statusCode == 200) {
                "Sarah second inference HTTP ${second!!.statusCode}: ${second!!.responseBody}"
            }
            require(!second!!.content.isNullOrBlank()) {
                "Sarah second inference returned empty content"
            }

            stage("12 sequence PASS")
            Files.writeString(
                resultFile,
                buildJsonObject {
                    put("schemaVersion", Schema)
                    put("status", "PASS")
                    put("sourceSha", sourceSha)
                    put("failureStage", "")
                    put("vexRuntimePid", vexPid!!)
                    put("sarahRuntimePid", sarahRuntime!!.pid)
                    put("processIsolationExact", sarahRuntime!!.pid != vexPid)
                    put("firstSarahHttpStatus", first!!.statusCode)
                    put("firstSarahContent", first!!.content ?: "")
                    put("vexContent", vexReply!!)
                    put("secondSarahHttpStatus", second!!.statusCode)
                    put("secondSarahContent", second!!.content ?: "")
                    put("secondSarahCachedPromptTokens", second!!.cachedPromptTokens ?: -1)
                    put("semanticJudgment", "NOT_APPLICABLE_SEQUENCEABILITY_ONLY")
                }.toString() + "\n",
            )
            0
        } catch (error: Throwable) {
            stage("FAIL at $failureStage: ${error.message ?: error.javaClass.name}")
            val runtime = sarahRuntime
            Files.writeString(
                resultFile,
                buildJsonObject {
                    put("schemaVersion", Schema)
                    put("status", "SAFE_FAILURE")
                    put("sourceSha", sourceSha)
                    put("failureStage", failureStage)
                    put("failureClass", error.javaClass.name)
                    put("failureMessage", error.message ?: "")
                    put("vexRuntimePid", vexPid ?: -1)
                    put("sarahRuntimePid", runtime?.pid ?: -1)
                    put("firstSarahHttpStatus", first?.statusCode ?: -1)
                    put("firstSarahResponseBody", first?.responseBody ?: "")
                    put("vexContent", vexReply ?: "")
                    put("secondSarahHttpStatus", second?.statusCode ?: -1)
                    put("secondSarahResponseBody", second?.responseBody ?: "")
                    put("sarahStdoutTail", runtime?.let { tail(it.stdout) } ?: "")
                    put("sarahStderrTail", runtime?.let { tail(it.stderr) } ?: "")
                    put("semanticJudgment", "NOT_REACHED_SEQUENCEABILITY_FAILURE")
                }.toString() + "\n",
            )
            2
        } finally {
            runCatching { sarahRuntime?.close() }
            scratchParent?.let { runCatching { it.toFile().deleteRecursively() } }
        }
    }
}

fun main(args: Array<String>) {
    kotlin.system.exitProcess(DynamicCounterpartSequenceProbe.run(args))
}

// [VXG RealForever]
