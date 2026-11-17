package vexlife.companion

import java.nio.file.Files
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import vexlife.conversation.CanonicalLivedContextFixture
import vexlife.conversation.DirectConversationStore
import vexlife.home.CanonicalVexLifeHomeFixture

/**
 * Host/source pressure for the continuity workbench.
 *
 * This intentionally exercises the real DirectPromptContext rather than a replacement projector.
 * It proves only the current serialized relationship: changing Conversation extent is emitted in
 * the coalesced system prefix before otherwise-reusable historical Conversation.
 */
class DirectContinuityProjectionHostProofTest {
    @Test
    fun currentPromptPlacesChangingConversationExtentBeforeReusableHistory() {
        val parent =
            Files.createTempDirectory("vexlife-prod-continuity-host-proof-").toAbsolutePath()
        val home = parent.resolve(".vexlife")
        try {
            CanonicalVexLifeHomeFixture.form(home)
            val store = DirectConversationStore(home)

            store.withWriterLease {
                completeTurn(
                    home = home,
                    name = "one",
                    human = "Stable historical human one",
                    vex = "Stable historical Vex one",
                    second = 1,
                )
                completeTurn(
                    home = home,
                    name = "two",
                    human = "Stable historical human two",
                    vex = "Stable historical Vex two",
                    second = 2,
                )

                val priorTwo = assertNotNull(immediatePriorCompletedTurn())
                val requestThree =
                    beginRequest(
                        turnRef = "turn.continuity.host-proof.three",
                        requestMessageRef = "message.continuity.host-proof.human.three",
                        content = "Third human request",
                        formedAt = Instant.parse("2026-11-16T20:00:03Z"),
                    )
                val promptThree =
                    DirectPromptContext.materialize(
                        prior = priorTwo,
                        currentRequest = requestThree,
                        recentConversationSource = { availableTokens ->
                            recentCompletedConversation(
                                availableTokens = availableTokens,
                                estimatedMessageTokens =
                                    DirectPromptContext::estimatedMessageTokens,
                            )
                        },
                        conversationExtentIncluded = true,
                    )

                val responseThree =
                    appendResponse(
                        request = requestThree,
                        responseMessageRef = "message.continuity.host-proof.companion.three",
                        content = "Third Vex response",
                        model = "default_model",
                        endpointOrigin = "http://127.0.0.1:8765",
                        runtimeObservationRef = "observation.continuity.host-proof.three",
                        invocationEvidenceRef = "evidence.continuity.host-proof.three",
                        formedAt = Instant.parse("2026-11-16T20:00:04Z"),
                    )
                val contextThree =
                    CanonicalLivedContextFixture.write(
                        productHome = home,
                        request = requestThree,
                        response = responseThree,
                        formedAt = Instant.parse("2026-11-16T20:00:05Z"),
                    )
                commitCompletedTurn(
                    request = requestThree,
                    response = responseThree,
                    contextSha256 = contextThree.sha256,
                    formedAt = Instant.parse("2026-11-16T20:00:06Z"),
                )

                val priorThree = assertNotNull(immediatePriorCompletedTurn())
                val requestFour =
                    beginRequest(
                        turnRef = "turn.continuity.host-proof.four",
                        requestMessageRef = "message.continuity.host-proof.human.four",
                        content = "Fourth human request",
                        formedAt = Instant.parse("2026-11-16T20:00:07Z"),
                    )
                val promptFour =
                    DirectPromptContext.materialize(
                        prior = priorThree,
                        currentRequest = requestFour,
                        recentConversationSource = { availableTokens ->
                            recentCompletedConversation(
                                availableTokens = availableTokens,
                                estimatedMessageTokens =
                                    DirectPromptContext::estimatedMessageTokens,
                            )
                        },
                        conversationExtentIncluded = true,
                    )

                val serializedThree = serialize(promptThree.messages)
                val serializedFour = serialize(promptFour.messages)
                val historicalNeedle = "Stable historical human one"
                val historicalStartThree = serializedThree.indexOf(historicalNeedle)
                val historicalStartFour = serializedFour.indexOf(historicalNeedle)
                val commonPrefix = commonPrefixLength(serializedThree, serializedFour)

                assertTrue(historicalStartThree > 0)
                assertTrue(historicalStartFour > 0)
                assertTrue(
                    promptThree.messages.first().content.contains(
                        "priorCompletedExchanges=2",
                    ),
                )
                assertTrue(
                    promptFour.messages.first().content.contains(
                        "priorCompletedExchanges=3",
                    ),
                )
                assertTrue(
                    commonPrefix < minOf(historicalStartThree, historicalStartFour),
                    "current mutable system truth should diverge before historical Conversation",
                )

                println(
                    "CONTINUITY_PREFIX_PRESSURE " +
                        "commonPrefixChars=$commonPrefix " +
                        "historyStartTurn3=$historicalStartThree " +
                        "historyStartTurn4=$historicalStartFour",
                )
            }
        } finally {
            parent.toFile().deleteRecursively()
        }
    }

    private fun DirectConversationStore.Locked.completeTurn(
        home: java.nio.file.Path,
        name: String,
        human: String,
        vex: String,
        second: Int,
    ) {
        val request =
            beginRequest(
                turnRef = "turn.continuity.host-proof.$name",
                requestMessageRef = "message.continuity.host-proof.human.$name",
                content = human,
                formedAt = Instant.parse("2026-11-16T20:00:0${second}Z"),
            )
        val response =
            appendResponse(
                request = request,
                responseMessageRef = "message.continuity.host-proof.companion.$name",
                content = vex,
                model = "default_model",
                endpointOrigin = "http://127.0.0.1:8765",
                runtimeObservationRef = "observation.continuity.host-proof.$name",
                invocationEvidenceRef = "evidence.continuity.host-proof.$name",
                formedAt = Instant.parse("2026-11-16T20:00:1${second}Z"),
            )
        val context =
            CanonicalLivedContextFixture.write(
                productHome = home,
                request = request,
                response = response,
                formedAt = Instant.parse("2026-11-16T20:00:2${second}Z"),
            )
        commitCompletedTurn(
            request = request,
            response = response,
            contextSha256 = context.sha256,
            formedAt = Instant.parse("2026-11-16T20:00:3${second}Z"),
        )
    }

    private fun serialize(messages: List<CompanionProviderMessage>): String =
        messages.joinToString("\n") { "${it.role}\u0000${it.content}" }

    private fun commonPrefixLength(
        first: String,
        second: String,
    ): Int {
        val limit = minOf(first.length, second.length)
        var index = 0
        while (index < limit && first[index] == second[index]) {
            index += 1
        }
        return index
    }
}
