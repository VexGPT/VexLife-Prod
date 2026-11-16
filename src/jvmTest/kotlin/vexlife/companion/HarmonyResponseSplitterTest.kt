package vexlife.companion

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import vexlife.model.heretic.HereticModelCatalog
import vexlife.model.heretic.HereticModelConfiguration
import vexlife.model.heretic.HereticRuntimeQualification
import vexlife.model.heretic.HereticRuntimeState

/** gpt-oss replies in harmony; the person sees its analysis as thinking and only final as Vex. */
class HarmonyResponseSplitterTest {
    private val reply =
        "<|channel|>analysis<|message|>They ask about rounds. Answer plainly.<|end|>" +
            "<|start|>assistant<|channel|>final<|message|>Rounds matter less than **confidence**."

    private fun split(chunks: List<String>): Pair<String, String> {
        val splitter = HarmonyResponseSplitter()
        val pieces = chunks.flatMap { splitter.accept(it) } + splitter.finish()
        fun of(kind: HarmonyResponseSplitter.Kind) =
            pieces.filter { it.kind == kind }.joinToString("") { it.text }
        return of(HarmonyResponseSplitter.Kind.Thinking) to of(HarmonyResponseSplitter.Kind.Answer)
    }

    @Test
    fun analysisIsThinkingAndFinalIsTheAnswerWhereverTheChunksBreak() {
        val expected =
            "They ask about rounds. Answer plainly." to "Rounds matter less than **confidence**."
        assertEquals(expected, split(listOf(reply)))
        // One character at a time cuts through every marker.
        assertEquals(expected, split(reply.map { it.toString() }))
        // Uneven chunks.
        assertEquals(expected, split(reply.chunked(7)))
    }

    @Test
    fun aCallAddressedToAToolIsACallAndNeitherThinkingNorTheAnswer() {
        fun calls(chunks: List<String>): List<HarmonyResponseSplitter.Piece> {
            val splitter = HarmonyResponseSplitter()
            return (chunks.flatMap { splitter.accept(it) } + splitter.finish()).filter {
                it.kind == HarmonyResponseSplitter.Kind.ToolCall
            }
        }
        // The two header orders gpt-oss writes; the server strips the stopping <|call|>.
        val afterAnalysis =
            "<|channel|>analysis<|message|>I need the page.<|end|>" +
                "<|start|>assistant<|channel|>commentary to=functions.web_fetch <|constrain|>json" +
                "<|message|>{\"url\":\"https://example.com/\"}"
        val firstMessage =
            " to=functions.web_fetch<|channel|>commentary json<|message|>{\"url\":\"https://example.com/\"}"
        for (reply in listOf(afterAnalysis, firstMessage)) {
            for (chunks in listOf(listOf(reply), reply.map { it.toString() }, reply.chunked(5))) {
                val call = calls(chunks)
                assertEquals(setOf("functions.web_fetch"), call.map { it.recipient }.toSet(), reply)
                assertEquals("{\"url\":\"https://example.com/\"}", call.joinToString("") { it.text })
            }
        }
        val (thinking, answer) = split(listOf(afterAnalysis, "<|call|>", "ignored after the call"))
        assertEquals("I need the page.", thinking)
        assertEquals("", answer)
    }

    @Test
    fun commentaryToNoOneIsThinkingAndAToolsOwnOutputIsNeverACall() {
        val splitter = HarmonyResponseSplitter()
        val pieces =
            splitter.accept(
                "<|channel|>commentary<|message|>Checking the page first.<|end|>" +
                    "<|start|>functions.web_fetch to=assistant<|channel|>commentary<|message|>made up<|end|>"
            ) + splitter.finish()
        assertEquals(
            listOf(HarmonyResponseSplitter.Piece(HarmonyResponseSplitter.Kind.Thinking, "Checking the page first.made up")),
            pieces,
        )
    }

    @Test
    fun textThatOpensNoMarkerIsOrdinaryAndAPlainReplyPassesThrough() {
        assertEquals(
            "" to "a <|b is text",
            split(listOf("<|channel|>final<|message|>a <|b is text<|return|>")),
        )
        assertEquals(
            "" to "Already separated by the server.",
            split(listOf("Already separated", " by the server.")),
        )
    }

    @Test
    fun aHarmonyModelStreamsItsAnalysisAsThinkingAndSendsOneSystemMessage() {
        val entry = HereticModelCatalog.CandidateGptOss20B
        val runtime =
            HereticModelConfiguration.QualifiedRuntimeEvidence(
                runtimeReceiptRef = "receipt.vexlife-prod.heretic.test",
                runtimeState = HereticRuntimeState.Qualified,
                modelRef = entry.modelRef,
                modelProfileRef = entry.identity.modelProfileRef,
                artifactRef = entry.identity.artifactSetRef,
                artifactContentSetSha256 = entry.identity.artifactContentSetSha256,
                runtimeAdapterRef = entry.identity.runtimeAdapterRef,
                runtimeClass = entry.identity.runtimeClass,
                endpoint = entry.identity.endpoint,
                requestModel = entry.identity.requestModel,
                qualification = HereticRuntimeQualification.qualified(),
            )
        var body = ""
        val progress = mutableListOf<DirectCompanionConnection.Progress>()
        val transport =
            object : DirectCompanionConnection.StreamingTransport {
                override fun post(call: DirectCompanionConnection.HttpCall) = error("streams")

                override fun postStreaming(
                    call: DirectCompanionConnection.HttpCall,
                    onLine: (String) -> Unit,
                ): DirectCompanionConnection.StreamingHttpResult {
                    body = call.body
                    for (chunk in reply.chunked(9)) {
                        onLine(
                            """data: {"model":"default_model","choices":[{"delta":{"role":"assistant","content":__C__}}]}"""
                                .replace(
                                    "__C__",
                                    kotlinx.serialization.json.JsonPrimitive(chunk).toString(),
                                )
                        )
                        onLine("")
                    }
                    onLine("data: [DONE]")
                    return DirectCompanionConnection.StreamingHttpResult(200, "c".repeat(64))
                }
            }

        val result =
            DirectCompanionConnection.invoke(
                runtime = runtime,
                messages =
                    listOf(
                        CompanionProviderMessage(role = "system", content = "Field."),
                        CompanionProviderMessage(role = "system", content = "Reach frame."),
                        CompanionProviderMessage(role = "user", content = "Was it too limiting?"),
                    ),
                transport = transport,
                progressObserver = DirectCompanionConnection.ProgressObserver { progress += it },
                now = { Instant.parse("2026-11-15T17:00:00Z") },
            )

        assertEquals("Rounds matter less than **confidence**.", result.content)
        assertEquals("They ask about rounds. Answer plainly.", result.sealedHumanReasoning)
        assertEquals(
            "Rounds matter less than **confidence**.",
            progress
                .filter { it.kind == DirectCompanionConnection.ProgressKind.Content }
                .joinToString("") { it.delta },
            "the person never sees harmony markup",
        )
        val request = Json.parseToJsonElement(body).jsonObject
        assertEquals(
            "You are Vex.",
            request["chat_template_kwargs"]!!.jsonObject["model_identity"]!!.jsonPrimitive.content,
        )
        val sent = request["messages"]!!.jsonArray
        assertEquals(2, sent.size)
        assertEquals("system", sent[0].jsonObject["role"]!!.jsonPrimitive.content)
        assertEquals(
            "Field.\n\nReach frame.",
            sent[0].jsonObject["content"]!!.jsonPrimitive.content,
        )
    }
}
