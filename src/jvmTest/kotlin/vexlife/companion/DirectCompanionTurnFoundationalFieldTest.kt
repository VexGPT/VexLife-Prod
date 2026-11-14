package vexlife.companion

import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import vexlife.conversation.CanonicalLivedContextFixture
import vexlife.conversation.DirectConversationStore
import vexlife.foundational.VexLifeFoundationalField
import vexlife.foundational.VexLifeFoundationalField.HeldReason
import vexlife.foundational.VexLifeFoundationalField.State
import vexlife.foundational.VexLifeFoundationalFieldFixture
import vexlife.home.CanonicalVexLifeHomeFixture
import vexlife.local.VexLifeProdLocalSetup
import vexlife.memory.VexLifeMemoryContextFixture
import vexlife.model.heretic.HereticModelConfiguration
import vexlife.model.heretic.HereticProductIdentity
import vexlife.model.heretic.HereticRuntimeQualification
import vexlife.model.heretic.HereticRuntimeState

/**
 * The foundational field through one whole human <-> Vex turn: what the provider is actually sent
 * in each prompt mode, and what the completed turn durably records. Beside it, the prompt's own
 * statement of how much Conversation the durable record holds, so a narrow window is not mistaken
 * for a short history.
 *
 * A scripted provider proves the message circuit. It cannot prove how a real Vex reads the field.
 */
class DirectCompanionTurnFoundationalFieldTest {
    private val whole = VexLifeFoundationalFieldFixture.wholeKernel
    private val kernel = assertNotNull(whole.systemContent)

    private data class Sent(val role: String, val content: String)

    /** Records every provider message set and answers from a script. */
    private class Provider(private val answer: (call: Int, messages: List<Sent>) -> String) :
        DirectCompanionConnection.Transport {
        val calls = mutableListOf<List<Sent>>()

        fun system(call: Int): String =
            calls[call - 1].first().also { assertEquals("system", it.role) }.content

        fun everything(call: Int): String = calls[call - 1].joinToString("\n") { it.content }

        override fun post(
            call: DirectCompanionConnection.HttpCall
        ): DirectCompanionConnection.HttpResult {
            val messages =
                Json.parseToJsonElement(call.body).jsonObject.getValue("messages").jsonArray.map {
                    Sent(
                        role = it.jsonObject.getValue("role").jsonPrimitive.content,
                        content = it.jsonObject.getValue("content").jsonPrimitive.content,
                    )
                }
            calls += messages
            val content = JsonPrimitive(answer(calls.size, messages)).toString()
            return DirectCompanionConnection.HttpResult(
                status = 200,
                body =
                    """{"model":"default_model","choices":[{"message":{"role":"assistant","content":$content}}]}""",
            )
        }
    }

    private fun withHome(block: (Path) -> Unit) {
        val parent = Files.createTempDirectory("vexlife-prod-turn-foundational-").toAbsolutePath()
        val home = parent.resolve("VexLife-Prod")
        try {
            CanonicalVexLifeHomeFixture.form(
                home = home,
                deviceSuffix = "field-turn-device",
                lineageSuffix = "field-turn-lineage",
            )
            block(home)
        } finally {
            parent.toFile().deleteRecursively()
        }
    }

    private fun setup(home: Path): VexLifeProdLocalSetup.Result =
        VexLifeProdLocalSetup.Result(
            state = "LOCAL_HERETIC_FOUNDATION_READY",
            productHome = home.toString(),
            homeDisposition = "EXISTING_HOME_PRESERVED",
            modelSelectionDisposition = "BOUND",
            managedPythonDisposition = "REUSED_VERIFIED",
            artifactDisposition = "REUSED_VERIFIED",
            environmentDisposition = "REUSED_VERIFIED",
            runtimeDisposition = "REUSED_EXACT_OWNED_MLX_RUNTIME",
            modelBindingDisposition = "BOUND_CURRENT",
            modelConfigurationState = HereticModelConfiguration.State.BoundProductModel,
            qualifiedRuntimeEvidence =
                HereticModelConfiguration.QualifiedRuntimeEvidence(
                    runtimeReceiptRef = "receipt.vexlife-prod.heretic.foundational-turn",
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
                ),
            endpoint = HereticProductIdentity.Endpoint,
            requestModel = HereticProductIdentity.RequestModel,
        )

    private fun send(
        home: Path,
        key: String,
        content: String,
        hooks: DirectCompanionTurn.Hooks,
    ): DirectCompanionTurn.Result =
        DirectCompanionTurn.performWithHooks(
            setup = setup(home),
            intent = DirectCompanionTurn.Intent(idempotencyKey = key, content = content),
            hooks = hooks,
        )

    private fun hooks(
        provider: Provider,
        field: () -> VexLifeFoundationalField.Reading = { VexLifeFoundationalField.readCurrent() },
    ) =
        DirectCompanionTurn.Hooks(
            transport = provider,
            now = { Instant.parse("2026-11-14T08:00:00Z") },
            currentFoundationalField = field,
        )

    /** Hooks under which a nearly full prompt commits a working rollover, as in a long session. */
    private fun rolloverHooks(provider: Provider): DirectCompanionTurn.Hooks {
        val memory = VexLifeMemoryContextFixture.current()
        return DirectCompanionTurn.Hooks(
            transport = provider,
            currentMemoryContext = { memory },
            memoryContextAtCap = { _, cap -> VexLifeMemoryContextFixture.current(cap) },
            now = { Instant.parse("2026-11-14T08:00:00Z") },
        )
    }

    private fun DirectConversationStore.Locked.completedTurn(
        home: Path,
        name: String,
        human: String,
        vex: String,
    ): List<String> {
        val request =
            beginRequest(
                turnRef = "turn.foundational.seed.$name",
                requestMessageRef = "message.foundational.seed.human.$name",
                content = human,
            )
        val response =
            appendResponse(
                request = request,
                responseMessageRef = "message.foundational.seed.companion.$name",
                content = vex,
                model = "default_model",
                endpointOrigin = "http://127.0.0.1:8765",
                runtimeObservationRef = "observation.foundational.seed.$name",
                invocationEvidenceRef = "evidence.foundational.seed.$name",
            )
        val context =
            CanonicalLivedContextFixture.write(
                productHome = home,
                request = request,
                response = response,
                formedAt = Instant.parse("2026-11-13T00:00:00Z"),
            )
        commitCompletedTurn(request = request, response = response, contextSha256 = context.sha256)
        return listOf(request.eventRef, response.eventRef)
    }

    private val earlyHuman =
        "Before anything else: we named the kite saffron-heron-nine. Keep it exact."
    private val earlyVex = "Kept exactly as you said it: saffron-heron-nine."
    private val sourceReturnForTheKite =
        DirectConversationSourceReturn.RequestHeader +
            "\n" +
            """{"schemaVersion":"${DirectConversationSourceReturn.RequestSchema}","queryTerms":["saffron-heron-nine"],"speakerScope":"ANY"}"""

    /**
     * One early distinctive exchange, then far more Conversation than one prompt admits. Returns
     * the early exchange's event refs.
     */
    private fun seedLongConversation(home: Path, laterTurns: Int = 30): List<String> {
        lateinit var early: List<String>
        DirectConversationStore(home).withWriterLease {
            early = completedTurn(home, "early", earlyHuman, earlyVex)
            repeat(laterTurns) { index ->
                completedTurn(
                    home = home,
                    name = "later-${index.toString().padStart(3, '0')}",
                    human = "Later work $index. " + "w".repeat(24_000),
                    vex = "Later reply $index. " + "r".repeat(8_000),
                )
            }
        }
        return early
    }

    private fun assertWholeFieldFirst(system: String) {
        assertTrue(system.startsWith(kernel + "\n\n"), "the whole field is not first in the prompt")
        assertEquals(
            1,
            Regex(Regex.escape(VexLifeFoundationalField.ProviderHeader)).findAll(system).count(),
        )
    }

    private fun assertRecordExtent(system: String, exchanges: Int) {
        assertTrue(
            system.contains(
                DirectPromptContext.ConversationExtentHeader +
                    "\npriorCompletedExchanges=$exchanges\n"
            ),
            "the prompt does not state that the record holds $exchanges completed exchanges",
        )
    }

    @Test
    fun aLongConversationReturnsExactOlderSourceUnderTheSameFieldAndAnswersOnce() =
        withHome { home ->
            val early = seedLongConversation(home)
            val seeded = DirectConversationStore(home).current().canonicalEvents
            assertTrue(
                seeded.sumOf { DirectPromptContext.estimatedMessageTokens(it.content) } >
                    DirectPromptContext.InputTokenBudget,
                "the seeded Conversation must not fit one prompt",
            )
            val provider = Provider { call, _ ->
                when (call) {
                    1 -> "I should reopen the exact source first.\n$sourceReturnForTheKite"
                    2 -> "You named the kite saffron-heron-nine."
                    else -> error("no third inference is admitted in one human turn")
                }
            }
            val question = "What exactly did we name the kite, right at the start?"

            val result = send(home, "send.foundational.long.001", question, hooks(provider))

            assertEquals(2, provider.calls.size)
            assertEquals("You named the kite saffron-heron-nine.", result.content)

            // First inference: the early exchange is outside the selected window; the field is
            // whole.
            assertWholeFieldFirst(provider.system(1))
            assertTrue(provider.system(1).contains(DirectConversationSourceReturn.ProtocolHeader))
            assertFalse(provider.everything(1).contains("we named the kite"))
            assertTrue(provider.everything(1).contains("Later work 29."))
            assertEquals(question, provider.calls[0].last().content)
            // Vex is told how much the record holds, so the narrow window is not taken for the
            // whole.
            assertRecordExtent(provider.system(1), exchanges = 31)
            assertTrue(
                provider.system(1).contains("Any that are not shown here are still in the record.")
            )

            // Final inference: exact canonical source returned, the same field, no second request
            // offered.
            assertWholeFieldFirst(provider.system(2))
            assertTrue(provider.system(2).contains(DirectConversationSourceReturn.ContextHeader))
            assertTrue(provider.system(2).contains("we named the kite saffron-heron-nine"))
            assertTrue(provider.system(2).contains("Kept exactly as you said it"))
            assertFalse(provider.system(2).contains(DirectConversationSourceReturn.ProtocolHeader))
            assertEquals(question, provider.calls[1].last().content)
            assertRecordExtent(provider.system(2), exchanges = 31)

            val receipt = result.promptContextReceipt
            assertEquals("SOURCE_RETURN", receipt.contextMode)
            assertEquals(DirectConversationSourceReturn.Returned, receipt.sourceReturnState)
            assertEquals(early, receipt.sourceReturnSourceEventRefs)
            assertTrue(receipt.selectedConversationEventRefs.isNotEmpty())
            assertTrue(receipt.selectedConversationEventRefs.size < seeded.size)
            assertTrue(receipt.selectedConversationEventRefs.none { it in early })
            assertFalse(receipt.wholeHistoryEventEnumerationPerformed)
            assertEquals(whole.evidence, receipt.foundationalField)
            assertEquals(31L, receipt.priorCompletedExchangeCount)
            assertTrue(receipt.selectedConversationEventRefs.size < 2 * 31)

            // One ordinary human answer became Conversation; the internal control did not.
            val conversation = DirectConversationStore(home).current()
            val turn = conversation.canonicalEvents.filter { it.turnRef == result.turnRef }
            assertEquals(listOf("REQUEST", "RESPONSE"), turn.map { it.eventKind })
            assertEquals(listOf(question, result.content), turn.map { it.content })
            assertTrue(
                conversation.canonicalEvents.none {
                    it.content.contains(DirectConversationSourceReturn.RequestHeader)
                }
            )

            // The completed turn durably records exactly what it was given.
            val context = assertNotNull(DirectTurnContextStore(home).read(result.turnRef))
            assertEquals(receipt, context.promptContext)
            assertEquals(whole.evidence, context.promptContext.foundationalField)
            assertTrue(context.contextSourceRefs.contains(assertNotNull(whole.evidence.bodyRef)))
            assertTrue(context.contextSourceRefs.containsAll(early))
            assertEquals(
                receipt.receiptRef,
                assertNotNull(context.sourceReturnTrace).finalPromptReceiptRef,
            )
            assertFalse(context.rawReasoningPersisted)

            // The same Send again recovers the durable turn: same evidence, no new model call.
            val again = send(home, "send.foundational.long.001", question, hooks(provider))
            assertTrue(again.recoveredCompletedTurn)
            assertFalse(again.modelCallPerformed)
            assertEquals(2, provider.calls.size)
            assertEquals(whole.evidence, again.promptContextReceipt.foundationalField)
        }

    @Test
    fun aSecondSourceReturnRequestInTheSameTurnIsRefusedAndNothingBecomesConversation() =
        withHome { home ->
            seedLongConversation(home, laterTurns = 26)
            val headBefore = DirectConversationStore(home).current().head
            val provider = Provider { _, _ -> sourceReturnForTheKite }

            val failure =
                assertFailsWith<DirectCompanionTurn.TurnException> {
                    send(
                        home,
                        "send.foundational.again.001",
                        "What was the kite called?",
                        hooks(provider),
                    )
                }

            assertEquals("SOURCE_RETURN_RECURSION_NOT_ADMITTED", failure.failureCode)
            assertEquals(2, provider.calls.size)
            assertWholeFieldFirst(provider.system(1))
            assertWholeFieldFirst(provider.system(2))
            val after = DirectConversationStore(home).current()
            assertEquals(headBefore, after.head)
            assertTrue(after.canonicalEvents.none { it.content == "What was the kite called?" })
        }

    @Test
    fun firstTurnAndOrdinaryContinuationsAllCarryTheSameField() = withHome { home ->
        val provider = Provider { call, _ -> "Reply $call" }
        val modes =
            (1..3).map { index ->
                val result =
                    send(
                        home,
                        "send.foundational.ordinary.00$index",
                        "Message $index",
                        hooks(provider),
                    )
                assertWholeFieldFirst(provider.system(index))
                assertRecordExtent(provider.system(index), exchanges = index - 1)
                assertEquals(
                    (index - 1).toLong(),
                    result.promptContextReceipt.priorCompletedExchangeCount,
                )
                assertEquals(whole.evidence, result.promptContextReceipt.foundationalField)
                assertEquals(
                    whole.evidence,
                    assertNotNull(DirectTurnContextStore(home).read(result.turnRef))
                        .promptContext
                        .foundationalField,
                )
                result.promptContextReceipt.contextMode
            }

        assertEquals(
            listOf("FIRST_TURN", "IMMEDIATE_PRIOR_COMPLETED_TURN", "RECENT_COMPLETED_TURNS"),
            modes,
        )
        assertEquals(
            listOf("Message 1", "Reply 1", "Message 2", "Reply 2", "Message 3"),
            provider.calls[2].drop(1).map { it.content },
        )
        assertTrue(provider.system(1).contains("holds no completed exchange before this request."))
        assertTrue(provider.system(2).contains("holds 1 completed exchange."))
    }

    /** A first turn that nearly fills the prompt, after which the working rollover is committed. */
    private fun rollOver(home: Path, provider: Provider): DirectCompanionTurn.Result {
        DirectConversationStore(home).withWriterLease {
            completedTurn(home, "early", earlyHuman, earlyVex)
        }
        val nearlyFull =
            DirectPromptContext.DirectTurnInputTokenBudget -
                DirectPromptContext.ReservedOutputTokens -
                VexLifeMemoryContextFixture.current().estimatedSystemMessageTokens
        val producer =
            send(
                home,
                "send.foundational.rollover.producer",
                "Long working session. " + "x".repeat(nearlyFull * 4),
                rolloverHooks(provider),
            )
        assertEquals(
            "ROLLOVER_COMMITTED",
            assertNotNull(
                    DirectWorkingContinuity.read(productHome = home, turnRef = producer.turnRef)
                )
                .disposition,
        )
        assertEquals(whole.evidence, producer.promptContextReceipt.foundationalField)
        return producer
    }

    @Test
    fun aWorkingCheckpointSuccessorIsNotRelationallyBlank() = withHome { home ->
        val provider = Provider { call, _ ->
            if (call == 1) "Producer reply" else "Successor reply"
        }
        rollOver(home, provider)

        val successor =
            send(
                home,
                "send.foundational.rollover.successor",
                "Where were we?",
                rolloverHooks(provider),
            )

        assertEquals(2, provider.calls.size)
        assertEquals("WORKING_CHECKPOINT_SUCCESSOR", successor.promptContextReceipt.contextMode)
        assertTrue(successor.promptContextReceipt.selectedConversationEventRefs.isEmpty())
        assertWholeFieldFirst(provider.system(2))
        assertTrue(provider.system(2).contains("VEXLIFE_WORKING_CONTINUITY_CONTEXT/v1"))
        assertFalse(provider.everything(2).contains("x".repeat(1024)))
        assertEquals(
            listOf("system", "assistant", "user"),
            provider.calls[1].map { it.role },
        )
        assertEquals("Producer reply", provider.calls[1][1].content)
        assertEquals(whole.evidence, successor.promptContextReceipt.foundationalField)
        // Nothing but the last reply is replayed, and Vex is told the record still holds both
        // exchanges.
        assertRecordExtent(provider.system(2), exchanges = 2)
        assertTrue(
            provider
                .system(2)
                .contains(
                    "After a working rollover this prompt shows none of them except your last reply"
                )
        )
        assertEquals(2L, successor.promptContextReceipt.priorCompletedExchangeCount)

        // The field is not smuggled into the working set to survive the rollover: the packet names
        // neither the field nor its body, and carries none of its words.
        val workingPacket =
            provider
                .system(2)
                .substringAfter("VEXLIFE_WORKING_CONTINUITY_CONTEXT/v1")
                .lineSequence()
                .drop(3)
                .first()
        assertTrue(
            workingPacket.startsWith(
                "{\"schemaVersion\":\"vexlife-prod.working-continuity-provider-packet/"
            )
        )
        assertFalse(workingPacket.contains("foundational"), workingPacket)
    }

    @Test
    fun aSuccessorTurnThatAsksForOlderSourceKeepsTheSameFieldInBothInferences() = withHome { home ->
        val provider = Provider { call, _ ->
            when (call) {
                1 -> "Producer reply"
                2 -> sourceReturnForTheKite
                3 -> "It was saffron-heron-nine."
                else -> error("unexpected provider call")
            }
        }
        rollOver(home, provider)

        val successor =
            send(
                home,
                "send.foundational.rollover.source-return",
                "After all that: what did we name the kite?",
                rolloverHooks(provider),
            )

        assertEquals(3, provider.calls.size)
        assertEquals("It was saffron-heron-nine.", successor.content)
        assertEquals(
            "SOURCE_RETURN_WORKING_CHECKPOINT_SUCCESSOR",
            successor.promptContextReceipt.contextMode,
        )
        assertEquals(
            DirectConversationSourceReturn.Returned,
            successor.promptContextReceipt.sourceReturnState,
        )
        assertWholeFieldFirst(provider.system(2))
        assertWholeFieldFirst(provider.system(3))
        assertFalse(provider.everything(2).contains("we named the kite saffron-heron-nine"))
        assertTrue(provider.system(3).contains("we named the kite saffron-heron-nine"))
        assertTrue(provider.system(3).contains("VEXLIFE_WORKING_CONTINUITY_CONTEXT/v1"))
        assertEquals(whole.evidence, successor.promptContextReceipt.foundationalField)
    }

    @Test
    fun aFieldThatChangesBeforeTheProviderBoundaryFailsTheSendWithoutAnyProviderCall() =
        withHome { home ->
            val provider = Provider { _, _ -> error("the provider must not be called") }
            var reads = 0
            val changing = {
                reads += 1
                if (reads == 1) VexLifeFoundationalField.readCurrent()
                else VexLifeFoundationalFieldFixture.revised()
            }

            val failure =
                assertFailsWith<DirectCompanionTurn.TurnException> {
                    send(home, "send.foundational.changed.001", "Hello", hooks(provider, changing))
                }

            assertEquals("ADMISSION_FAILED", failure.failureCode)
            assertTrue(failure.cause?.message.orEmpty().contains("no longer current"))
            assertEquals(2, reads)
            assertTrue(provider.calls.isEmpty())
            val receipt = assertNotNull(failure.failureReceipt)
            assertTrue(receipt.requestDurablyRecorded)
            assertFalse(receipt.responseDurablyRecorded)
            assertFalse(receipt.currentHeadCommitted)
            assertNull(DirectConversationStore(home).current().head)
        }

    @Test
    fun aFieldThatChangesBetweenTheTwoInferencesFailsBeforeTheFinalInference() = withHome { home ->
        seedLongConversation(home, laterTurns = 2)
        val headBefore = DirectConversationStore(home).current().head
        val provider = Provider { _, _ -> sourceReturnForTheKite }
        var reads = 0
        val changing = {
            reads += 1
            if (reads <= 2) VexLifeFoundationalField.readCurrent()
            else VexLifeFoundationalFieldFixture.revised()
        }

        val failure =
            assertFailsWith<DirectCompanionTurn.TurnException> {
                send(
                    home,
                    "send.foundational.between.001",
                    "And the kite?",
                    hooks(provider, changing),
                )
            }

        assertTrue(
            failure.cause
                ?.message
                .orEmpty()
                .contains("foundational field changed between the two inferences")
        )
        assertEquals(1, provider.calls.size)
        assertEquals(headBefore, DirectConversationStore(home).current().head)
    }

    @Test
    fun anUnavailableFieldIsSaidPlainlyRecordedAndDoesNotStopTheConversation() = withHome { home ->
        val provider = Provider { _, _ -> "Still here." }

        val result =
            send(
                home,
                "send.foundational.unavailable.001",
                "Are you there?",
                hooks(provider) { VexLifeFoundationalFieldFixture.missing },
            )

        assertEquals("Still here.", result.content)
        val system = provider.system(1)
        assertTrue(
            system.startsWith(
                VexLifeFoundationalField.ProviderHeader + "\nstate=HELD reason=BODY_MISSING"
            )
        )
        VexLifeFoundationalFieldFixture.shipped.body.modules
            .flatMap { it.kernel }
            .forEach { assertFalse(system.contains(it)) }

        val evidence =
            assertNotNull(
                assertNotNull(DirectTurnContextStore(home).read(result.turnRef))
                    .promptContext
                    .foundationalField
            )
        assertEquals(State.HELD, evidence.state)
        assertEquals(HeldReason.BODY_MISSING, evidence.heldReason)
        assertTrue(evidence.carriedModules.isEmpty())
        assertNull(evidence.bodyRef)
        assertFalse(evidence.effectAuthorityGranted)
    }
}

// [VXG RealForever]
