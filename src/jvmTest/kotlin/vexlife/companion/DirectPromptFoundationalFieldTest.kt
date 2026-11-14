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
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import vexlife.contract.VexContractJson
import vexlife.conversation.CanonicalLivedContextFixture
import vexlife.conversation.DirectConversationStore
import vexlife.foundational.VexLifeFoundationalField
import vexlife.foundational.VexLifeFoundationalField.HeldReason
import vexlife.foundational.VexLifeFoundationalField.State
import vexlife.foundational.VexLifeFoundationalFieldFixture
import vexlife.home.CanonicalVexLifeHomeFixture
import vexlife.memory.VexLifeMemoryContextFixture
import vexlife.model.heretic.HereticModelConfiguration
import vexlife.model.heretic.HereticProductIdentity
import vexlife.model.heretic.HereticRuntimeQualification
import vexlife.model.heretic.HereticRuntimeState

/** How the prompt owner gives the foundational field room, places it and receipts it. */
class DirectPromptFoundationalFieldTest {
    private val shipped = VexLifeFoundationalFieldFixture.shipped
    private val whole = VexLifeFoundationalFieldFixture.wholeKernel
    private val kernel = assertNotNull(whole.systemContent)

    private fun withHome(block: (Path) -> Unit) {
        val parent = Files.createTempDirectory("vexlife-prod-prompt-foundational-").toAbsolutePath()
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
            runtimeReceiptRef = "receipt.vexlife-prod.heretic.prompt-foundational",
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

    private fun DirectConversationStore.Locked.completedTurn(
        home: Path,
        name: String,
        human: String,
        vex: String,
    ) {
        val request =
            beginRequest(
                turnRef = "turn.foundational.$name",
                requestMessageRef = "message.foundational.human.$name",
                content = human,
            )
        val response =
            appendResponse(
                request = request,
                responseMessageRef = "message.foundational.companion.$name",
                content = vex,
                model = "default_model",
                endpointOrigin = "http://127.0.0.1:8765",
                runtimeObservationRef = "observation.foundational.$name",
                invocationEvidenceRef = "evidence.foundational.$name",
            )
        val context =
            CanonicalLivedContextFixture.write(
                productHome = home,
                request = request,
                response = response,
                formedAt = Instant.parse("2026-11-12T00:00:00Z"),
            )
        commitCompletedTurn(request = request, response = response, contextSha256 = context.sha256)
    }

    private fun tokens(content: String) = DirectPromptContext.estimatedMessageTokens(content)

    /** Text whose estimate is exactly [estimatedTokens]. */
    private fun textOf(estimatedTokens: Int) = "p".repeat((estimatedTokens - 4) * 4)

    @Test
    fun theFieldIsTheFirstSystemContentAheadOfMemoryCapabilityAndPersonContext() =
        withHome { home ->
            DirectConversationStore(home).withWriterLease {
                val request =
                    beginRequest(
                        turnRef = "turn.foundational.first",
                        requestMessageRef = "message.foundational.human.first",
                        content = "Hello Vex",
                    )
                val memory = VexLifeMemoryContextFixture.current()
                val person =
                    DirectPromptContext.PersonContext(
                        replyLanguage = "en",
                        replyLanguageName = "English",
                        privateConversations = emptyList(),
                    )
                fun materialize(field: VexLifeFoundationalField.Reading?) =
                    DirectPromptContext.materialize(
                        prior = null,
                        currentRequest = request,
                        memoryContext = memory,
                        personContext = person,
                        runtimeEvidence = runtimeEvidence(),
                        foundationalField = field,
                    )

                val with = materialize(shipped)
                val without = materialize(null)

                assertEquals(listOf("system", "user"), with.messages.map { it.role })
                val system = with.messages.first().content
                assertTrue(system.startsWith(kernel + "\n\n"))
                val order =
                    listOf(
                            VexLifeFoundationalField.ProviderHeader,
                            "VEXLIFE_CURRENT_MEMORY_CONTEXT/v1",
                            "VEXLIFE_SELF_CAPABILITY_FRAME/v1",
                            "VEXLIFE_PERSON_CONTEXT/v1",
                        )
                        .map { system.indexOf(it) }
                assertTrue(order.all { it >= 0 } && order == order.sorted(), "system order: $order")
                assertEquals(without.messages.first().content, system.removePrefix(kernel + "\n\n"))
                assertEquals("Hello Vex", with.messages.last().content)

                val receipt = with.receipt
                assertEquals(DirectPromptContext.ReceiptSchema, receipt.schemaVersion)
                assertEquals("FIRST_TURN", receipt.contextMode)
                assertEquals(whole.evidence, receipt.foundationalField)
                assertEquals(
                    with.messages.sumOf { tokens(it.content) },
                    receipt.providerMaterializedInputTokenEstimate,
                )
                // The provider is sent one coalesced system message, so the field costs the prompt
                // what
                // its bytes add to that message: never more than its own standalone estimate.
                val added =
                    receipt.providerMaterializedInputTokenEstimate -
                        without.receipt.providerMaterializedInputTokenEstimate
                assertEquals(tokens(system) - tokens(without.messages.first().content), added)
                assertTrue(added in 1..whole.evidence.estimatedSystemMessageTokens)
                assertNull(without.receipt.foundationalField)

                // The field is context only: what Vex can reach is exactly what it was without it.
                assertEquals(
                    without.receipt.capabilityFrameSnapshot,
                    receipt.capabilityFrameSnapshot,
                )
                assertEquals(
                    without.receipt.capabilityFrameFingerprint,
                    receipt.capabilityFrameFingerprint,
                )
                assertFalse(assertNotNull(receipt.foundationalField).effectAuthorityGranted)
                assertFalse(receipt.effectsPerformed)

                val fixed =
                    DirectPromptContext.prepareFixed(
                        prior = null,
                        currentRequest = request,
                        foundationalField = shipped,
                    )
                assertEquals(whole.evidence, fixed.foundationalField)
                assertEquals(kernel, fixed.prefixMessages.first().content)
                assertEquals(
                    (fixed.prefixMessages + fixed.suffixMessages).sumOf { tokens(it.content) },
                    fixed.fixedInputTokenEstimate,
                )
            }
        }

    @Test
    fun receiptsWithoutTheFieldKeepTheirHistoricalShapeAndV7StillRoundTrips() = withHome { home ->
        DirectConversationStore(home).withWriterLease {
            val request =
                beginRequest(
                    turnRef = "turn.foundational.v7",
                    requestMessageRef = "message.foundational.human.v7",
                    content = "Historical v7 receipt compatibility",
                )
            val without =
                VexContractJson.Strict.encodeToJsonElement(
                        DirectPromptContext.materialize(prior = null, currentRequest = request)
                            .receipt
                    )
                    .jsonObject
            assertTrue(
                without.keys.none {
                    it.startsWith("foundational") ||
                        it.startsWith("conversationExtent") ||
                        it.startsWith("priorCompleted")
                }
            )

            val historical =
                JsonObject(
                    without.toMutableMap().also {
                        it["schemaVersion"] = JsonPrimitive(DirectPromptContext.ReceiptSchemaV7)
                    }
                )
            val decoded = VexContractJson.decode<DirectPromptContext.Receipt>(historical.toString())
            assertEquals(DirectPromptContext.ReceiptSchemaV7, decoded.schemaVersion)
            assertNull(decoded.foundationalField)
            assertEquals(
                VexContractJson.canonicalEncodeElement(historical),
                VexContractJson.canonicalEncode(decoded),
            )

            val with =
                DirectPromptContext.materialize(
                        prior = null,
                        currentRequest = request,
                        foundationalField = shipped,
                        conversationExtentIncluded = true,
                    )
                    .receipt
            val encoded = VexContractJson.encode(with)
            assertTrue(
                VexContractJson.Strict.parseToJsonElement(encoded)
                    .jsonObject
                    .keys
                    .containsAll(
                        listOf(
                            "foundationalField",
                            "priorCompletedExchangeCount",
                            "conversationExtentSystemMessageSha256",
                            "conversationExtentEstimatedSystemMessageTokens",
                        )
                    )
            )
            assertEquals(0L, with.priorCompletedExchangeCount)
            assertEquals(with, VexContractJson.decode<DirectPromptContext.Receipt>(encoded))
        }
    }

    @Test
    fun theFieldGivesWayBeforeTheExactImmediatePriorTurnAndIsTheSameInBothInferences() {
        val protocolTokens = tokens(DirectConversationSourceReturn.protocolMessage().content)
        val currentHuman = "Current request"
        val priorVex = "Prior response"
        val wholeTokens = whole.evidence.estimatedSystemMessageTokens
        fun priorHumanLeaving(room: Int) =
            textOf(
                DirectPromptContext.InputTokenBudget -
                    tokens(currentHuman) -
                    protocolTokens -
                    tokens(priorVex) -
                    room
            )

        fun prompts(room: Int, check: (initial: DirectPromptContext.Materialization) -> Unit) =
            withHome { home ->
                val store = DirectConversationStore(home)
                store.withWriterLease {
                    completedTurn(home, "prior", priorHumanLeaving(room), priorVex)
                }
                store.withWriterLease {
                    val prior = requireNotNull(immediatePriorCompletedTurn())
                    val request =
                        beginRequest(
                            turnRef = "turn.foundational.current",
                            requestMessageRef = "message.foundational.human.current",
                            content = currentHuman,
                        )
                    val initial =
                        DirectPromptContext.materializeContinuity(
                            conversationOwner = this,
                            prior = prior,
                            currentRequest = request,
                            sourceReturnProtocolEnabled = true,
                            foundationalField = shipped,
                        )
                    val final =
                        DirectPromptContext.materializeContinuity(
                            conversationOwner = this,
                            prior = prior,
                            currentRequest = request,
                            sourceReturnDiscovery =
                                DirectConversationSourceReturn.discover(
                                    conversationOwner = this,
                                    request =
                                        DirectConversationSourceReturn.Request(
                                            schemaVersion =
                                                DirectConversationSourceReturn.RequestSchema,
                                            queryTerms = listOf("a term nobody said"),
                                            speakerScope =
                                                DirectConversationSourceReturn.AnySpeaker,
                                        ),
                                    excludedEventRefs =
                                        initial.receipt.selectedConversationEventRefs.toSet(),
                                ),
                            foundationalField = shipped,
                        )

                    // Carrying the field never costs the turn its exact immediate prior exchange.
                    assertEquals("IMMEDIATE_PRIOR_COMPLETED_TURN", initial.receipt.contextMode)
                    assertEquals(
                        listOf(prior.requestEvent.eventRef, prior.responseEvent.eventRef),
                        initial.receipt.selectedConversationEventRefs,
                    )
                    assertTrue(
                        initial.receipt.providerMaterializedInputTokenEstimate <=
                            DirectPromptContext.InputTokenBudget
                    )
                    assertEquals("SOURCE_RETURN", final.receipt.contextMode)
                    assertEquals(
                        initial.receipt.foundationalField,
                        final.receipt.foundationalField,
                    )
                    check(initial)
                }
            }

        prompts(room = wholeTokens) { initial ->
            // Exactly enough room: the same whole kernel, with the smaller room it was offered
            // recorded.
            val evidence = assertNotNull(initial.receipt.foundationalField)
            assertEquals(State.CARRIED, evidence.state)
            assertEquals(wholeTokens, evidence.tokenBudget)
            assertEquals(whole.evidence.systemMessageSha256, evidence.systemMessageSha256)
            assertEquals(whole.evidence.carriedModules, evidence.carriedModules)
            assertTrue(evidence.omittedModuleRefs.isEmpty())
            assertTrue(initial.messages.first().content.startsWith(kernel + "\n\n"))
        }

        prompts(room = wholeTokens - 1) { initial ->
            val evidence = assertNotNull(initial.receipt.foundationalField)
            val refs = shipped.body.modules.map { it.moduleRef }
            assertEquals(State.CARRIED_PARTIAL, evidence.state)
            assertEquals(refs.dropLast(1), evidence.carriedModules.map { it.moduleRef })
            assertEquals(refs.takeLast(1), evidence.omittedModuleRefs)
            assertEquals(
                listOf(VexLifeFoundationalField.OmittedForTokenBudget),
                evidence.omissionReasons,
            )
            assertEquals(wholeTokens - 1, evidence.tokenBudget)
            val system = initial.messages.first().content
            assertTrue(system.startsWith(VexLifeFoundationalField.ProviderHeader))
            assertTrue(system.contains("Not carried in this prompt for room: " + refs.last()))
        }

        prompts(room = 120) { initial ->
            val evidence = assertNotNull(initial.receipt.foundationalField)
            assertEquals(State.HELD, evidence.state)
            assertEquals(HeldReason.TOKEN_BUDGET, evidence.heldReason)
            assertEquals(shipped.bodySha256, evidence.bodySha256)
            assertTrue(evidence.carriedModules.isEmpty())
            assertTrue(initial.messages.first().content.contains("state=HELD reason=TOKEN_BUDGET"))
        }

        prompts(room = 0) { initial ->
            val evidence = assertNotNull(initial.receipt.foundationalField)
            assertEquals(State.HELD, evidence.state)
            assertNull(evidence.systemMessageSha256)
            assertEquals(0, evidence.estimatedSystemMessageTokens)
            assertFalse(
                initial.messages.any {
                    it.content.contains(VexLifeFoundationalField.ProviderHeader)
                }
            )
        }
    }

    @Test
    fun aFieldThatIsNoLongerTheSameAtTheProviderBoundaryFailsReverification() = withHome { home ->
        DirectConversationStore(home).withWriterLease {
            val request =
                beginRequest(
                    turnRef = "turn.foundational.reverify",
                    requestMessageRef = "message.foundational.human.reverify",
                    content = "Is the ground still the same?",
                )
            val materialization =
                DirectPromptContext.materializeContinuity(
                    conversationOwner = this,
                    prior = null,
                    currentRequest = request,
                    sourceReturnProtocolEnabled = true,
                    foundationalField = shipped,
                )

            DirectPromptContext.reverifyContinuity(
                materialization = materialization,
                conversationOwner = this,
                prior = null,
                currentRequest = request,
                sourceReturnProtocolEnabled = true,
                foundationalField = shipped,
            )

            listOf(
                    VexLifeFoundationalFieldFixture.revised(),
                    VexLifeFoundationalFieldFixture.missing,
                    null,
                )
                .forEach { changed ->
                    val failure =
                        assertFailsWith<IllegalArgumentException> {
                            DirectPromptContext.reverifyContinuity(
                                materialization = materialization,
                                conversationOwner = this,
                                prior = null,
                                currentRequest = request,
                                sourceReturnProtocolEnabled = true,
                                foundationalField = changed,
                            )
                        }
                    assertTrue(failure.message.orEmpty().contains("no longer current"))
                }
        }
    }

    @Test
    fun anUnavailableFieldIsCarriedAsItsHeldNoticeNotAsImprovisedContent() = withHome { home ->
        DirectConversationStore(home).withWriterLease {
            val request =
                beginRequest(
                    turnRef = "turn.foundational.held",
                    requestMessageRef = "message.foundational.human.held",
                    content = "Hello",
                )
            val held =
                DirectPromptContext.materialize(
                    prior = null,
                    currentRequest = request,
                    foundationalField = VexLifeFoundationalFieldFixture.missing,
                )
            val system = held.messages.first().content
            assertTrue(system.startsWith(VexLifeFoundationalField.ProviderHeader + "\n"))
            assertTrue(system.contains("state=HELD reason=BODY_MISSING"))
            shipped.body.modules.flatMap { it.kernel }.forEach { assertFalse(system.contains(it)) }
            val evidence = assertNotNull(held.receipt.foundationalField)
            assertEquals(State.HELD, evidence.state)
            assertEquals(HeldReason.BODY_MISSING, evidence.heldReason)
            assertNull(evidence.bodyRef)
            assertTrue(evidence.carriedModules.isEmpty())
        }
    }
}

// [VXG RealForever]
