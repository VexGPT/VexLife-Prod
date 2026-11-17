package vexlife.companion

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vexlife.capability.VexLifeCapabilityRequest
import vexlife.model.heretic.HereticModelConfiguration
import vexlife.model.heretic.HereticProductIdentity
import vexlife.model.heretic.HereticRuntimeQualification
import vexlife.model.heretic.HereticRuntimeState

/** What one provider stage cost and why it stopped, as the Connection observes it. */
class DirectProviderStageMeasurementTest {
    private val runtime =
        HereticModelConfiguration.QualifiedRuntimeEvidence(
            runtimeReceiptRef = "receipt.vexlife-prod.heretic.measurement",
            runtimeState = HereticRuntimeState.Qualified,
            modelRef = HereticProductIdentity.ModelRef,
            modelProfileRef = HereticProductIdentity.ModelProfileRef,
            artifactRef = HereticProductIdentity.ArtifactSetRef,
            artifactContentSetSha256 = HereticProductIdentity.ArtifactContentSetSha256,
            runtimeAdapterRef = HereticProductIdentity.RuntimeAdapterRef,
            runtimeClass = HereticProductIdentity.RuntimeClass,
            endpoint = HereticProductIdentity.Endpoint,
            requestModel = HereticProductIdentity.RequestModel,
            qualification = HereticRuntimeQualification.qualified(),
        )

    private val messages = listOf(CompanionProviderMessage("user", "What does the page say?"))

    /** A clock that moves 100 ms each time it is read, so timings are exact. */
    private fun steppingClock(): () -> Long {
        var ticks = 0L
        return { 100_000_000L * ticks++ }
    }

    private fun stream(vararg lines: String) =
        object : DirectCompanionConnection.StreamingTransport {
            override fun post(call: DirectCompanionConnection.HttpCall) = error("streams")

            override fun postStreaming(
                call: DirectCompanionConnection.HttpCall,
                onLine: (String) -> Unit,
            ): DirectCompanionConnection.StreamingHttpResult {
                lines.forEach(onLine)
                return DirectCompanionConnection.StreamingHttpResult(200, "d".repeat(64))
            }
        }

    private fun reasoning(text: String) =
        """data: {"model":"default_model","choices":[{"delta":{"role":"assistant","reasoning":${json(text)}}}]}"""

    private fun answer(text: String, finish: String? = null) =
        """data: {"model":"default_model","choices":[{""" +
            (finish?.let { "\"finish_reason\":\"$it\"," } ?: "") +
            """"delta":{"role":"assistant","content":${json(text)}}}]}"""

    private fun finish(reason: String) =
        """data: {"model":"default_model","choices":[{"finish_reason":"$reason","delta":{}}]}"""

    private fun usage(prompt: Int, cached: Int, completion: Int) =
        """data: {"model":"default_model","choices":[],"usage":{"prompt_tokens":$prompt,""" +
            """"completion_tokens":$completion,"total_tokens":${prompt + completion},""" +
            """"prompt_tokens_details":{"cached_tokens":$cached}}}"""

    private fun json(text: String) = kotlinx.serialization.json.JsonPrimitive(text).toString()

    private fun invoke(transport: DirectCompanionConnection.Transport) =
        DirectCompanionConnection.invoke(
            runtime = runtime,
            messages = messages,
            transport = transport,
            now = { Instant.parse("2026-11-16T12:00:00Z") },
            monotonicNanos = steppingClock(),
        )

    @Test
    fun aCompletedStageReportsItsTokensCacheReuseAndTimeToFirstText() {
        val result =
            invoke(
                stream(
                    reasoning("Read it first."),
                    answer("It says to "),
                    answer("fly it on a 30 m line.", finish = "stop"),
                    usage(prompt = 51_000, cached = 49_500, completion = 120),
                    "data: [DONE]",
                )
            )
        val m = result.measurement
        assertEquals(DirectCompanionConnection.StageStop.COMPLETED, m.stop)
        assertEquals("stop", m.finishReason)
        assertEquals(51_000, m.promptTokens)
        assertEquals(49_500, m.cachedPromptTokens)
        assertEquals(1_500, m.prefillTokens)
        assertEquals(120, m.completionTokens)
        // Clock reads: start 0, first thinking 100, first answer 200, end 300.
        assertEquals(100, m.firstTokenMillis)
        assertEquals(200, m.firstAnswerMillis)
        assertEquals(300, m.totalMillis)
        assertEquals("Read it first.".length, m.reasoningCharacters)
        assertEquals("It says to fly it on a 30 m line.".length, m.answerCharacters)
        assertFalse(m.controlHeaderInReasoning)
        assertFalse(m.controlHeaderInAnswer)
    }

    @Test
    fun anOutputLimitSpentWhileThinkingIsNamedAndShowsAnUnsentRequest() {
        val failure =
            assertFailsWith<DirectCompanionConnection.ConnectionException> {
                invoke(
                    stream(
                        reasoning("I should fetch it. "),
                        reasoning(
                            VexLifeCapabilityRequest.RequestHeader + "\n{\"capability\":\"web\""
                        ),
                        finish("length"),
                        usage(prompt = 40_000, cached = 0, completion = 16_000),
                        "data: [DONE]",
                    )
                )
            }
        assertEquals(DirectCompanionConnection.FailureCode.EndpointResponseInvalid, failure.code)
        val m = assertNotNull(failure.measurement)
        assertEquals(DirectCompanionConnection.StageStop.OUTPUT_LIMIT_DURING_REASONING, m.stop)
        assertEquals(40_000, m.prefillTokens)
        assertEquals(16_000, m.completionTokens)
        assertNull(m.firstAnswerMillis)
        assertTrue(m.controlHeaderInReasoning)
        assertFalse(m.controlHeaderInAnswer)
    }

    @Test
    fun aStreamThatClosesBeforeDoneIsNamedEvenAfterAnswerText() {
        val failure =
            assertFailsWith<DirectCompanionConnection.ConnectionException> {
                invoke(stream(reasoning("Thinking."), answer("The page")))
            }
        val m = assertNotNull(failure.measurement)
        assertEquals(DirectCompanionConnection.StageStop.STREAM_CLOSED_BEFORE_DONE, m.stop)
        assertNull(m.promptTokens, "no usage arrives from a stream that died")
        assertEquals("The page".length, m.answerCharacters)
    }

    @Test
    fun anAnswerCutByTheOutputLimitStillCompletesButIsNamed() {
        val result =
            invoke(
                stream(
                    answer("The page says", finish = "length"),
                    usage(prompt = 900, cached = 0, completion = 16_000),
                    "data: [DONE]",
                )
            )
        assertEquals(
            DirectCompanionConnection.StageStop.OUTPUT_LIMIT_DURING_ANSWER,
            result.measurement.stop,
        )
    }

    @Test
    fun aWholeReplyReportsUsageAndNamesARequestWrittenAsTheAnswer() {
        val control =
            VexLifeCapabilityRequest.RequestHeader +
                "\n{\"capability\":\"web\",\"action\":\"fetch\",\"arguments\":{\"url\":\"https://example.com/\"}}"
        val result =
            invoke(
                DirectCompanionConnection.Transport {
                    DirectCompanionConnection.HttpResult(
                        200,
                        """{"model":"default_model","choices":[{"finish_reason":"stop",""" +
                            """"message":{"role":"assistant","content":${json(control)}}}],""" +
                            """"usage":{"prompt_tokens":800,"completion_tokens":40,"total_tokens":840}}""",
                    )
                }
            )
        val m = result.measurement
        assertEquals(DirectCompanionConnection.StageStop.COMPLETED, m.stop)
        assertEquals(800, m.promptTokens)
        assertNull(m.cachedPromptTokens)
        assertEquals(800, m.prefillTokens)
        assertNull(m.firstTokenMillis, "a whole reply has no first-token time")
        assertTrue(m.controlHeaderInAnswer)
        assertFalse(m.controlHeaderInReasoning)
    }

    @Test
    fun theChronologyRefsAreCountsAndNamesNeverText() {
        val refs =
            invoke(
                    stream(
                        reasoning("Private thought."),
                        answer("Private answer.", finish = "stop"),
                        usage(prompt = 10, cached = 4, completion = 3),
                        "data: [DONE]",
                    )
                )
                .measurement
                .refs()
        assertEquals("COMPLETED", refs["stop"])
        assertEquals("6", refs["prefillTokens"])
        assertTrue(refs.values.none { "Private" in it })
    }
}

// [VXG RealForever]
