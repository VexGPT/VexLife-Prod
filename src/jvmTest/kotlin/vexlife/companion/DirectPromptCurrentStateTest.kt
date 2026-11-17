package vexlife.companion

import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import vexlife.conversation.CanonicalLivedContextFixture
import vexlife.conversation.DirectConversationStore
import vexlife.foundational.VexLifeFoundationalFieldFixture
import vexlife.home.CanonicalVexLifeHomeFixture
import vexlife.model.heretic.HereticModelConfiguration
import vexlife.model.heretic.HereticProductIdentity
import vexlife.model.heretic.HereticRuntimeQualification
import vexlife.model.heretic.HereticRuntimeState
import vexlife.person.VexLifePersonSettings

/**
 * Current state is placed as current: after the exact history, in VexLife's own envelope, never in
 * the leading system block. A setting change is visible on the very next prompt and changes no byte
 * of the stable block or the history before it.
 */
class DirectPromptCurrentStateTest {
    private fun withHome(block: (Path) -> Unit) {
        val parent =
            Files.createTempDirectory("vexlife-prod-prompt-current-state-").toAbsolutePath()
        val home = parent.resolve(".vexlife")
        try {
            CanonicalVexLifeHomeFixture.form(home)
            block(home)
        } finally {
            parent.toFile().deleteRecursively()
        }
    }

    private fun runtimeEvidence() =
        HereticModelConfiguration.QualifiedRuntimeEvidence(
            runtimeReceiptRef = "receipt.vexlife-prod.heretic.prompt-current-state",
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

    private fun person(language: String) =
        DirectPromptContext.PersonContext(
            replyLanguage = language,
            replyLanguageName = VexLifePersonSettings.languageName(language),
            privateConversations = emptyList(),
        )

    private fun DirectConversationStore.Locked.complete(
        home: Path,
        request: DirectConversationStore.Event,
        vex: String,
    ) {
        val response =
            appendResponse(
                request = request,
                responseMessageRef = request.eventRef.replace("request", "response") + ".message",
                content = vex,
                model = "default_model",
                endpointOrigin = "http://127.0.0.1:8765",
                runtimeObservationRef = "observation.current-state.${request.sequence}",
                invocationEvidenceRef = "evidence.current-state.${request.sequence}",
            )
        val context =
            CanonicalLivedContextFixture.write(
                productHome = home,
                request = request,
                response = response,
                formedAt = Instant.parse("2026-11-17T00:00:00Z"),
            )
        commitCompletedTurn(request = request, response = response, contextSha256 = context.sha256)
    }

    private fun DirectConversationStore.Locked.ask(name: String, content: String) =
        beginRequest(
            turnRef = "turn.current-state.$name",
            requestMessageRef = "message.current-state.human.$name",
            content = content,
        )

    private fun DirectConversationStore.Locked.prompt(
        request: DirectConversationStore.Event,
        language: String,
    ) =
        DirectPromptContext.materializeContinuity(
            conversationOwner = this,
            prior = immediatePriorCompletedTurn(),
            currentRequest = request,
            personContext = person(language),
            runtimeEvidence = runtimeEvidence(),
            sourceReturnProtocolEnabled = true,
            foundationalField = VexLifeFoundationalFieldFixture.shipped,
            conversationExtentIncluded = true,
            capabilityRequestsAdmitted = true,
        )

    private fun DirectPromptContext.Materialization.historyIndex() =
        receipt.contextComponents
            .single { it.componentRef == DirectPromptContext.ComponentRefs.ConversationHistory }
            .providerMessageIndex

    @Test
    fun aSettingChangeIsCurrentAndLeavesTheStableBlockAndHistoryByteIdentical() = withHome { home ->
        val store = DirectConversationStore(home)
        store.withWriterLease {
            complete(home, ask("one", "First message"), "First reply")
            complete(home, ask("two", "Second message"), "Second reply")
        }
        store.withWriterLease {
            val request = ask("three", "Which language now?")
            val english = prompt(request, "en")
            val chinese = prompt(request, "zh")

            // One system message, first: the only shape the pinned chat template admits.
            listOf(english, chinese).forEach { prompt ->
                assertEquals("system", prompt.messages.first().role)
                assertEquals(1, prompt.messages.count { it.role == "system" })
            }
            // The stable block and the exact history are byte-identical across the change.
            val stableAndHistory = english.historyIndex() + 4
            assertEquals(
                english.messages.take(stableAndHistory),
                chinese.messages.take(stableAndHistory),
            )
            assertTrue("VEXLIFE_PERSON_CONTEXT/v1" !in chinese.messages.first().content)
            // Only the current-state envelope differs, and it carries exactly the new setting.
            val envelope = chinese.messages[chinese.messages.size - 2]
            assertEquals("user", envelope.role)
            assertTrue(envelope.content.startsWith(DirectPromptContext.CurrentStateHeader))
            assertTrue("\"replyLanguage\":\"zh\"" in envelope.content)
            assertTrue("\"replyLanguage\":\"en\"" !in envelope.content)
            assertNotEquals(english.messages[english.messages.size - 2], envelope)
            assertEquals(request.content, chinese.messages.last().content)
            // No mutable or ephemeral component is placed ahead of the history.
            val history = chinese.historyIndex()
            chinese.receipt.contextComponents
                .filter {
                    it.stabilityClass == DirectPromptContext.ComponentStability.MUTABLE_CURRENT ||
                        it.stabilityClass ==
                            DirectPromptContext.ComponentStability.EPHEMERAL_CURRENT
                }
                .forEach { assertTrue(it.providerMessageIndex > history, it.componentRef) }

            complete(home, request, "Third reply")
            val next = prompt(ask("four", "And now?"), "zh")
            // The next turn's prompt begins with exactly this turn's stable block and history,
            // then this turn's request and reply as new history: per-turn bindings (frame ref,
            // record extent) no longer sit in the system block.
            assertEquals(
                chinese.messages.take(stableAndHistory),
                next.messages.take(stableAndHistory),
            )
            assertEquals(
                listOf("Which language now?", "Third reply"),
                next.messages.drop(stableAndHistory).take(2).map { it.content },
            )
        }
    }
}

// [VXG RealForever]
