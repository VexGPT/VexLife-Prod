package vexlife.companion

import java.nio.file.Files
import java.time.Duration
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vexlife.conversation.CanonicalLivedContextFixture
import vexlife.conversation.DirectConversationStore
import vexlife.home.CanonicalVexLifeHomeFixture
import vexlife.continuity.ContinuityTurnFixture
import vexlife.continuity.ContinuityTurnFixture.Line
import vexlife.continuity.VexLifeWorkingContinuity
import vexlife.continuity.VexLifeWorkingContinuityOwner
import vexlife.model.heretic.HereticModelConfiguration
import vexlife.model.heretic.HereticProductIdentity
import vexlife.model.heretic.HereticRuntimeQualification
import vexlife.model.heretic.HereticRuntimeState

class DirectPromptGroundPresenceTest {
    private val start =
        Instant.parse(
            "2026-11-22T16:00:00Z",
        )

    private fun at(
        minutes: Long,
    ) =
        start.plus(
            Duration.ofMinutes(
                minutes,
            ),
        )

    private fun rollover(
        fixture: ContinuityTurnFixture,
    ) {
        val newest =
            fixture.store
                .current()
                .canonicalEvents
                .last {
                    it.eventKind ==
                        "REQUEST"
                }
        val workingSet =
            VexLifeWorkingContinuity.WorkingSet(
                lineageRef = newest.companionLineageRef,
                threadRef = newest.threadRef,
                cursorEventRef = newest.eventRef,
                activeWorkNodeRefs = emptyList(),
                currentIntentReceiptRefs = emptyList(),
                openLoopRefs =
                    listOf(
                        "loop.ground-presence.response-bridge",
                    ),
                currentStatementRefs = emptyList(),
                currentContinuityRecordRefs = emptyList(),
                currentDailyStratumRefOrNull = null,
                sourceRefs =
                    listOf(
                        "source.ground-presence.response-bridge",
                    ),
            )
        val pressure =
            VexLifeWorkingContinuity.PromptPressureInput(
                promptReceiptRef =
                    "receipt.vexlife.prompt-context.ground-presence-rollover",
                providerMaterializedInputTokenEstimate = 176_000,
                inputTokenBudget = 192_000,
                reservedOutputTokens = 16_000,
                hardTokenLimit = 208_000,
                memoryBoundedOutStatementCount = 0,
                memoryTokenBoundedOutPageCount = 0,
                sourceRefs =
                    listOf(
                        "source.ground-presence.pressure",
                    ),
            )
        VexLifeWorkingContinuityOwner(
            fixture.home,
        )
            .commitPlan(
                plan =
                    VexLifeWorkingContinuity.formPlan(
                        pressure,
                        workingSet,
                    ),
                pressureInput = pressure,
                workingSet = workingSet,
                committedAt = at(1_000),
            )
    }

    private fun runtimeEvidence() =
        HereticModelConfiguration.QualifiedRuntimeEvidence(
            runtimeReceiptRef = "receipt.vexlife-prod.heretic.ground-presence",
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

    @Test
    fun groundPresenceSeparatesResidentConversationFromDurableExtentAndKeepsCurrentRequestExact() {
        val parent =
            Files.createTempDirectory(
                "vexlife-prod-direct-prompt-ground-presence-",
            )
                .toAbsolutePath()
        val productHome =
            parent.resolve(
                ".vexlife",
            )

        try {
            CanonicalVexLifeHomeFixture.form(
                productHome,
            )
            DirectConversationStore(
                productHome,
            )
                .withWriterLease {
                    fun commitPair(
                        suffix: String,
                        requestContent: String,
                        responseContent: String,
                    ) {
                        val request =
                            beginRequest(
                                turnRef =
                                    "turn.ground-presence.$suffix",
                                requestMessageRef =
                                    "message.ground-presence.human.$suffix",
                                content =
                                    requestContent,
                            )
                        val response =
                            appendResponse(
                                request = request,
                                responseMessageRef =
                                    "message.ground-presence.companion.$suffix",
                                content =
                                    responseContent,
                                model =
                                    "default_model",
                                endpointOrigin =
                                    "http://127.0.0.1:8765",
                                runtimeObservationRef =
                                    "observation.ground-presence.$suffix",
                                invocationEvidenceRef =
                                    "evidence.ground-presence.$suffix",
                            )
                        val context =
                            CanonicalLivedContextFixture.write(
                                productHome = productHome,
                                request = request,
                                response = response,
                                formedAt =
                                    Instant.parse(
                                        if (suffix == "001") {
                                            "2026-11-22T16:00:01Z"
                                        } else {
                                            "2026-11-22T16:00:02Z"
                                        },
                                    ),
                            )
                        commitCompletedTurn(
                            request = request,
                            response = response,
                            contextSha256 = context.sha256,
                        )
                    }

                    commitPair(
                        suffix = "001",
                        requestContent = "First exact human turn.",
                        responseContent = "First exact Vex response.",
                    )
                    commitPair(
                        suffix = "002",
                        requestContent = "Second exact human turn.",
                        responseContent = "Second exact Vex response.",
                    )

                    val prior =
                        assertNotNull(
                            immediatePriorCompletedTurn(),
                        )
                    val currentRequestContent =
                        "What is actually in view right now?"
                    val currentRequest =
                        beginRequest(
                            turnRef =
                                "turn.ground-presence.current",
                            requestMessageRef =
                                "message.ground-presence.human.current",
                            content =
                                currentRequestContent,
                        )
                    val materialized =
                        DirectPromptContext.materializeContinuityAt(
                            conversationOwner = this,
                            prior = prior,
                            currentRequest = currentRequest,
                            memoryAvailabilityContext = null,
                            memoryContextAtCap = null,
                            workingContinuityContext = null,
                            browserContext = null,
                            personContext = null,
                            runtimeEvidence = runtimeEvidence(),
                            sourceReturnProtocolEnabled = false,
                            sourceReturnDiscovery = null,
                            foundationalField = null,
                            contextProfile = null,
                            conversationExtentIncluded = true,
                            capabilityRequestsAdmitted = true,
                            providerNativeTools = true,
                            experimentEffectiveInputTokenBudget = null,
                        )

                    val ground =
                        materialized.messages
                            .single {
                                it.content.contains(
                                    DirectPromptContext.GroundPresenceHeader,
                                )
                            }
                            .content
                    assertTrue(
                        ground.contains(
                            "humanRequestNow=IN_VIEW",
                        ),
                    )
                    assertTrue(
                        ground.contains(
                            "recentConversationInView=COMPLETE_EXCHANGES",
                        ),
                    )
                    assertTrue(
                        ground.contains(
                            "olderConversationInView=NONE",
                        ),
                    )
                    assertTrue(
                        ground.contains(
                            "olderConversationBeyondView=NONE",
                        ),
                    )
                    assertEquals(
                        2L,
                        materialized.receipt.priorCompletedExchangeCount,
                    )
                    assertNull(
                        materialized.receipt.conversationExtentSystemMessageSha256,
                    )
                    assertEquals(
                        0,
                        materialized.receipt.conversationExtentEstimatedSystemMessageTokens,
                    )
                    assertTrue(
                        materialized.messages.none {
                            it.content.contains(
                                "VEXLIFE_CONVERSATION_EXTENT/",
                            )
                        },
                    )
                    assertEquals(
                        currentRequestContent,
                        materialized.messages.last().content,
                    )
                    assertEquals(
                        "user",
                        materialized.messages.last().role,
                    )
                    assertTrue(
                        materialized.receipt.contextComponents.any {
                            it.componentRef ==
                                "component.vexlife-prod.prompt.ground-presence"
                        },
                    )
                }
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun groundPresenceNamesResponseBridgeWithoutPretendingItIsACompletedExchange() {
        ContinuityTurnFixture().use {
            fixture ->
            fixture.commit(
                Line(
                    "Earlier.",
                    "Earlier reply.",
                    at(0),
                ),
                Line(
                    "Paste: " +
                        "token "
                            .repeat(
                                80_000,
                            )
                            .trim(),
                    "I read the whole paste.",
                    at(1),
                ),
            )
            rollover(
                fixture,
            )

            fixture.store
                .withWriterLease {
                    val prior =
                        assertNotNull(
                            immediatePriorCompletedTurn(),
                        )
                    val current =
                        beginRequest(
                            turnRef =
                                "turn.ground-presence.response-bridge.current",
                            requestMessageRef =
                                "message.ground-presence.human.response-bridge.current",
                            content =
                                "What did that paste change?",
                            formedAt = at(2),
                        )
                    val successor =
                        assertNotNull(
                            DirectWorkingContinuity.resolveSuccessorContext(
                                productHome = fixture.home,
                                prior = prior,
                                currentRequest = current,
                                issuedAt = at(3),
                            ),
                        )
                    val materialized =
                        DirectPromptContext.materializeContinuityAt(
                            conversationOwner = this,
                            prior = prior,
                            currentRequest = current,
                            memoryAvailabilityContext = null,
                            memoryContextAtCap = null,
                            workingContinuityContext = successor,
                            browserContext = null,
                            personContext = null,
                            runtimeEvidence = runtimeEvidence(),
                            sourceReturnProtocolEnabled = false,
                            sourceReturnDiscovery = null,
                            foundationalField = null,
                            contextProfile = null,
                            conversationExtentIncluded = true,
                            capabilityRequestsAdmitted = true,
                            providerNativeTools = true,
                            experimentEffectiveInputTokenBudget = null,
                        )

                    val evidence =
                        assertNotNull(
                            materialized.receipt.overlapTail,
                        )
                    assertTrue(
                        evidence.responseBridgeOnly,
                    )
                    assertEquals(
                        listOf(
                            prior.responseEvent.eventRef,
                        ),
                        materialized.receipt.selectedConversationEventRefs,
                    )

                    val ground =
                        materialized.messages
                            .single {
                                it.content.contains(
                                    DirectPromptContext.GroundPresenceHeader,
                                )
                            }
                            .content
                    assertTrue(
                        ground.contains(
                            "humanRequestNow=IN_VIEW",
                        ),
                    )
                    assertTrue(
                        ground.contains(
                            "recentConversationInView=PRIOR_VEX_RESPONSE_ONLY",
                        ),
                    )
                    assertTrue(
                        ground.contains(
                            "olderConversationInView=NONE",
                        ),
                    )
                    assertTrue(
                        ground.contains(
                            "olderConversationBeyondView=REACHABLE",
                        ),
                    )
                    assertEquals(
                        2L,
                        materialized.receipt.priorCompletedExchangeCount,
                    )
                    assertNull(
                        materialized.receipt.conversationExtentSystemMessageSha256,
                    )
                    assertEquals(
                        0,
                        materialized.receipt.conversationExtentEstimatedSystemMessageTokens,
                    )
                    assertTrue(
                        materialized.messages.none {
                            it.content.contains(
                                "VEXLIFE_CONVERSATION_EXTENT/",
                            )
                        },
                    )
                    assertEquals(
                        current.content,
                        materialized.messages.last().content,
                    )
                }
        }
    }
    @Test
    fun groundPresenceSeparatesHydratedOlderSourceFromOlderConversationStillBeyondView() {
        val parent =
            Files.createTempDirectory(
                "vexlife-prod-direct-prompt-ground-hydrated-",
            )
                .toAbsolutePath()
        val productHome =
            parent.resolve(
                ".vexlife",
            )

        try {
            CanonicalVexLifeHomeFixture.form(
                productHome,
            )
            DirectConversationStore(
                productHome,
            )
                .withWriterLease {
                    fun commitPair(
                        suffix: String,
                        requestText: String,
                        responseText: String,
                        formedAt: Instant,
                    ): Pair<DirectConversationStore.Event, DirectConversationStore.Event> {
                        val request =
                            beginRequest(
                                turnRef =
                                    "turn.ground-presence.hydrated.$suffix",
                                requestMessageRef =
                                    "message.ground-presence.human.hydrated.$suffix",
                                content =
                                    requestText,
                                formedAt =
                                    formedAt,
                            )
                        val response =
                            appendResponse(
                                request = request,
                                responseMessageRef =
                                    "message.ground-presence.companion.hydrated.$suffix",
                                content =
                                    responseText,
                                model =
                                    "default_model",
                                endpointOrigin =
                                    "http://127.0.0.1:8765",
                                runtimeObservationRef =
                                    "observation.ground-presence.hydrated.$suffix",
                                invocationEvidenceRef =
                                    "evidence.ground-presence.hydrated.$suffix",
                            )
                        val context =
                            CanonicalLivedContextFixture.write(
                                productHome = productHome,
                                request = request,
                                response = response,
                                formedAt = formedAt,
                            )
                        commitCompletedTurn(
                            request = request,
                            response = response,
                            contextSha256 = context.sha256,
                        )
                        return request to response
                    }

                    commitPair(
                        suffix = "old",
                        requestText =
                            "The older exact source says orchard-lantern is beside the stone gate.",
                        responseText =
                            "I received the orchard-lantern source.",
                        formedAt =
                            Instant.parse(
                                "2026-11-22T15:00:01Z",
                            ),
                    )
                    commitPair(
                        suffix = "recent",
                        requestText =
                            "Large recent turn. " +
                                "N".repeat(
                                    780_000,
                                ),
                        responseText =
                            "Recent turn retained.",
                        formedAt =
                            Instant.parse(
                                "2026-11-22T15:00:02Z",
                            ),
                    )

                    val prior =
                        assertNotNull(
                            immediatePriorCompletedTurn(),
                        )
                    val current =
                        beginRequest(
                            turnRef =
                                "turn.ground-presence.hydrated.current",
                            requestMessageRef =
                                "message.ground-presence.human.hydrated.current",
                            content =
                                "Where was orchard-lantern?",
                            formedAt =
                                Instant.parse(
                                    "2026-11-22T15:00:03Z",
                                ),
                        )
                    val discovery =
                        DirectConversationSourceReturn.discover(
                            conversationOwner = this,
                            request =
                                DirectConversationSourceReturn.Request(
                                    schemaVersion =
                                        DirectConversationSourceReturn.RequestSchema,
                                    queryTerms =
                                        listOf(
                                            "orchard-lantern",
                                        ),
                                    speakerScope =
                                        DirectConversationSourceReturn.AnySpeaker,
                                ),
                            excludedEventRefs =
                                setOf(
                                    prior.requestEvent.eventRef,
                                    prior.responseEvent.eventRef,
                                ),
                        )
                    val materialized =
                        DirectPromptContext.materializeContinuityAt(
                            conversationOwner = this,
                            prior = prior,
                            currentRequest = current,
                            memoryAvailabilityContext = null,
                            memoryContextAtCap = null,
                            workingContinuityContext = null,
                            browserContext = null,
                            personContext = null,
                            runtimeEvidence = runtimeEvidence(),
                            sourceReturnProtocolEnabled = true,
                            sourceReturnDiscovery = discovery,
                            foundationalField = null,
                            contextProfile = null,
                            conversationExtentIncluded = true,
                            capabilityRequestsAdmitted = true,
                            providerNativeTools = true,
                            experimentEffectiveInputTokenBudget = null,
                        )

                    assertTrue(
                        materialized.receipt.sourceReturnSourceEventRefs.isNotEmpty(),
                    )
                    val ground =
                        materialized.messages
                            .single {
                                it.content.contains(
                                    DirectPromptContext.GroundPresenceHeader,
                                )
                            }
                            .content
                    assertTrue(
                        ground.contains(
                            "humanRequestNow=IN_VIEW",
                        ),
                    )
                    assertTrue(
                        ground.contains(
                            "recentConversationInView=NONE",
                        ),
                    )
                    assertTrue(
                        ground.contains(
                            "olderConversationInView=EXACT_SOURCE_PRESENT",
                        ),
                    )
                    assertTrue(
                        ground.contains(
                            "olderConversationBeyondView=REACHABLE",
                        ),
                    )
                    assertEquals(
                        current.content,
                        materialized.messages.last().content,
                    )
                }
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }

}
