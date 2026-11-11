package vexlife.companion

import java.nio.file.Files
import java.time.Instant
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vexlife.continuity.VexLifeWorkingCarryContract
import vexlife.continuity.VexLifeWorkingCarryOwner
import vexlife.conversation.CanonicalLivedContextFixture
import vexlife.conversation.DirectConversationIdentity
import vexlife.conversation.DirectConversationStore
import vexlife.home.CanonicalVexLifeHomeFixture
import vexlife.model.heretic.HereticModelConfiguration
import vexlife.model.heretic.HereticProductIdentity
import vexlife.model.heretic.HereticRuntimeQualification
import vexlife.model.heretic.HereticRuntimeState

class DirectWorkingCarryPromptContextTest {
    private fun runtimeEvidence() =
        HereticModelConfiguration.QualifiedRuntimeEvidence(
            runtimeReceiptRef = "receipt.vexlife-prod.heretic.working-carry-prompt",
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

    private fun binding(
        sequence: Long,
        suffix: String,
    ) =
        VexLifeWorkingCarryContract.SourceBinding(
            eventRef = "event.vexlife.$suffix",
            eventHash =
                (
                    sequence +
                        1
                )
                    .toString(
                        16,
                    )
                    .padStart(
                        64,
                        '0',
                    ),
            sequence = sequence,
        )

    private fun sourceEvent(
        sequence: Long,
        suffix: String,
        kind: String,
        content: String,
    ) =
        VexLifeWorkingCarryContract.SourceEvent(
            source =
                binding(
                    sequence,
                    suffix,
                ),
            eventKind = kind,
            speakerRef =
                if (
                    kind ==
                        "REQUEST"
                ) {
                    "person.victor"
                } else {
                    DirectConversationIdentity.CompanionRoleRef
                },
            recipientRefs =
                listOf(
                    if (
                        kind ==
                            "REQUEST"
                    ) {
                        DirectConversationIdentity.CompanionRoleRef
                    } else {
                        "person.victor"
                    },
                ),
            content = content,
        )

    private fun syntheticRefresh() =
        VexLifeWorkingCarryContract.RefreshInput(
            schemaVersion = VexLifeWorkingCarryContract.RefreshInputSchema,
            perspective = VexLifeWorkingCarryContract.Perspective,
            companionLineageRef = "companion-lineage.vexlife.working-carry-prompt-test",
            threadRef = DirectConversationIdentity.ThreadRef,
            previousCarry = null,
            newlyLeavingEvents =
                listOf(
                    sourceEvent(
                        0,
                        "leaving.request",
                        "REQUEST",
                        "Please keep this unfinished distinction alive.",
                    ),
                    sourceEvent(
                        1,
                        "leaving.response",
                        "RESPONSE",
                        "I still owe a continuation on the unfinished distinction.",
                    ),
                ),
            retainedDirectEvents =
                listOf(
                    sourceEvent(
                        2,
                        "retained.request",
                        "REQUEST",
                        "We are continuing the same conversation.",
                    ),
                    sourceEvent(
                        3,
                        "retained.response",
                        "RESPONSE",
                        "The distinction is still relevant.",
                    ),
                ),
            currentRequest =
                sourceEvent(
                    4,
                    "current.request",
                    "REQUEST",
                    "Continue.",
                ),
            carryTokenCap = 512,
        )

    private fun transport(
        modelOutput: String,
    ) =
        DirectCompanionConnection.Transport {
            DirectCompanionConnection.HttpResult(
                status = 200,
                body =
                    """
                    {
                      "model": "default_model",
                      "choices": [
                        {
                          "message": {
                            "role": "assistant",
                            "reasoning_content": "private working carry reasoning",
                            "content": __CONTENT_JSON__
                          },
                          "finish_reason": "stop"
                        }
                      ]
                    }
                    """.trimIndent()
                        .replace(
                            "__CONTENT_JSON__",
                            JsonPrimitive(
                                modelOutput,
                            )
                                .toString(),
                        ),
            )
        }

    private fun carryOutput(
        sourceEventRef: String,
    ): String =
        """
        {
          "disposition": "CARRY",
          "carryItems": [
            {
              "kind": "OWED_CONTINUATION",
              "content": "I still owe a continuation on the unfinished distinction.",
              "sourceEventRefs": ["$sourceEventRef"]
            }
          ],
          "releaseReason": null
        }
        """.trimIndent()

    private val releaseOutput =
        """
        {
          "disposition": "RELEASE",
          "carryItems": [],
          "releaseReason": "NO_PRESENT_CARRY_REMAINS"
        }
        """.trimIndent()

    private fun source(
        event: DirectConversationStore.Event,
    ) =
        VexLifeWorkingCarryContract.SourceEvent(
            source =
                VexLifeWorkingCarryContract.SourceBinding(
                    eventRef = event.eventRef,
                    eventHash = event.eventHash,
                    sequence = event.sequence,
                ),
            eventKind = event.eventKind,
            speakerRef = event.speakerRef,
            recipientRefs = event.recipientRefs,
            content = event.content,
        )

    @Test
    fun candidateCarryUsesOnlyCallerAllowanceAndNeverTruncates() {
        val formation =
            DirectWorkingCarryFormation.form(
                runtime = runtimeEvidence(),
                refreshInput = syntheticRefresh(),
                transport =
                    transport(
                        carryOutput(
                            "event.vexlife.leaving.response",
                        ),
                    ),
                now = {
                    Instant.parse(
                        "2026-11-11T05:00:00Z",
                    )
                },
            )

        val included =
            DirectWorkingCarryPromptContext.projectCandidate(
                formation = formation,
                availableTokens = DirectPromptContext.InputTokenBudget,
            )

        assertEquals(
            DirectWorkingCarryPromptContext.Included,
            included.state,
        )
        assertTrue(
            included.included,
        )
        assertNotNull(
            included.systemContent,
        )
        assertTrue(
            requireNotNull(
                included.systemContent,
            )
                .startsWith(
                    "VEXLIFE_PRESENT_WORKING_CARRY/v1\n",
                ),
        )
        assertTrue(
            included.estimatedSystemMessageTokens >
                0,
        )
        assertFalse(
            included.effectAuthorityGranted,
        )
        assertFalse(
            included.effectsPerformed,
        )

        val held =
            DirectWorkingCarryPromptContext.projectCandidate(
                formation = formation,
                availableTokens =
                    included.estimatedSystemMessageTokens -
                        1,
            )

        assertEquals(
            DirectWorkingCarryPromptContext.HeldTokenBudget,
            held.state,
        )
        assertFalse(
            held.included,
        )
        assertNull(
            held.systemContent,
        )
        assertEquals(
            included.estimatedSystemMessageTokens,
            held.estimatedSystemMessageTokens,
        )
        assertEquals(
            included.systemMessageSha256,
            held.systemMessageSha256,
        )
        assertEquals(
            included.draftSha256,
            held.draftSha256,
        )
    }

    @Test
    fun releaseAndAbsentCurrentCarryConsumeNoPromptSpace() {
        val release =
            DirectWorkingCarryFormation.form(
                runtime = runtimeEvidence(),
                refreshInput = syntheticRefresh(),
                transport =
                    transport(
                        releaseOutput,
                    ),
                now = {
                    Instant.parse(
                        "2026-11-11T05:01:00Z",
                    )
                },
            )

        val released =
            DirectWorkingCarryPromptContext.projectCandidate(
                formation = release,
                availableTokens = 0,
            )

        assertEquals(
            DirectWorkingCarryPromptContext.Released,
            released.state,
        )
        assertFalse(
            released.included,
        )
        assertEquals(
            0,
            released.estimatedSystemMessageTokens,
        )
        assertNull(
            released.systemContent,
        )

        val absent =
            DirectWorkingCarryPromptContext.projectCurrent(
                current = null,
                availableTokens = 0,
            )

        assertEquals(
            DirectWorkingCarryPromptContext.NoCurrentCarry,
            absent.state,
        )
        assertFalse(
            absent.included,
        )
        assertEquals(
            0,
            absent.estimatedSystemMessageTokens,
        )
        assertNull(
            absent.systemContent,
        )
    }

    @Test
    fun durableCurrentCarryProjectsFromRevisionIdentityRatherThanFormationCandidateIdentity() {
        val parent =
            Files.createTempDirectory(
                "vexlife-prod-working-carry-prompt-current-",
            )
                .toAbsolutePath()
        val home =
            parent.resolve(
                "VexLife-Prod",
            )

        try {
            val formed =
                CanonicalVexLifeHomeFixture.form(
                    home = home,
                    deviceSuffix = "working-carry-prompt-device",
                    lineageSuffix = "working-carry-prompt-lineage",
                )
            val store =
                DirectConversationStore(
                    home,
                )
            val owner =
                VexLifeWorkingCarryOwner(
                    home,
                )

            store.withWriterLease {
                val firstRequest =
                    beginRequest(
                        turnRef = "turn.working-carry.prompt.001",
                        requestMessageRef = "message.working-carry.prompt.human.001",
                        content = "Keep this unfinished point alive.",
                        formedAt = Instant.parse(
                            "2026-11-11T05:02:00Z",
                        ),
                    )
                val firstResponse =
                    appendResponse(
                        request = firstRequest,
                        responseMessageRef = "message.working-carry.prompt.companion.001",
                        content = "I still owe a continuation on this point.",
                        model = "default_model",
                        endpointOrigin = "http://127.0.0.1:8765",
                        runtimeObservationRef = "observation.working-carry.prompt.runtime.001",
                        invocationEvidenceRef = "evidence.working-carry.prompt.invocation.001",
                        formedAt = Instant.parse(
                            "2026-11-11T05:02:01Z",
                        ),
                    )
                val firstContext =
                    CanonicalLivedContextFixture.write(
                        productHome = home,
                        request = firstRequest,
                        response = firstResponse,
                        formedAt = Instant.parse(
                            "2026-11-11T05:02:02Z",
                        ),
                    )
                val firstHead =
                    commitCompletedTurn(
                        request = firstRequest,
                        response = firstResponse,
                        contextSha256 = firstContext.sha256,
                        formedAt = Instant.parse(
                            "2026-11-11T05:02:03Z",
                        ),
                    )

                val secondRequest =
                    beginRequest(
                        turnRef = "turn.working-carry.prompt.002",
                        requestMessageRef = "message.working-carry.prompt.human.002",
                        content = "Continue with what is still alive.",
                        formedAt = Instant.parse(
                            "2026-11-11T05:03:00Z",
                        ),
                    )
                val refresh =
                    VexLifeWorkingCarryContract.RefreshInput(
                        schemaVersion = VexLifeWorkingCarryContract.RefreshInputSchema,
                        perspective = VexLifeWorkingCarryContract.Perspective,
                        companionLineageRef = formed.manifest.currentCompanionLineageRef,
                        threadRef = secondRequest.threadRef,
                        previousCarry = null,
                        newlyLeavingEvents =
                            listOf(
                                source(
                                    firstRequest,
                                ),
                                source(
                                    firstResponse,
                                ),
                            ),
                        retainedDirectEvents = emptyList(),
                        currentRequest =
                            source(
                                secondRequest,
                            ),
                        carryTokenCap = 512,
                    )
                val formation =
                    DirectWorkingCarryFormation.form(
                        runtime = runtimeEvidence(),
                        refreshInput = refresh,
                        transport =
                            transport(
                                carryOutput(
                                    firstResponse.eventRef,
                                ),
                            ),
                        now = {
                            Instant.parse(
                                "2026-11-11T05:03:01Z",
                            )
                        },
                    )
                val prepared =
                    owner.prepare(
                        priorConversationHeadSha256 = firstHead.conversationHeadSha256,
                        currentRequest = secondRequest,
                        refreshInput = formation.refreshInput,
                        carry = formation.draft,
                        formationBinding = formation.binding,
                    )

                val secondResponse =
                    appendResponse(
                        request = secondRequest,
                        responseMessageRef = "message.working-carry.prompt.companion.002",
                        content = "The continuation is now part of this completed turn.",
                        model = "default_model",
                        endpointOrigin = "http://127.0.0.1:8765",
                        runtimeObservationRef = "observation.working-carry.prompt.runtime.002",
                        invocationEvidenceRef = "evidence.working-carry.prompt.invocation.002",
                        formedAt = Instant.parse(
                            "2026-11-11T05:03:02Z",
                        ),
                    )
                val secondContext =
                    CanonicalLivedContextFixture.write(
                        productHome = home,
                        request = secondRequest,
                        response = secondResponse,
                        formedAt = Instant.parse(
                            "2026-11-11T05:03:03Z",
                        ),
                    )
                val secondHead =
                    commitCompletedTurn(
                        request = secondRequest,
                        response = secondResponse,
                        contextSha256 = secondContext.sha256,
                        formedAt = Instant.parse(
                            "2026-11-11T05:03:04Z",
                        ),
                    )
                val committed =
                    owner.commitCompletedTurn(
                        prepared = prepared,
                        completedHead = secondHead,
                    )
                val projected =
                    DirectWorkingCarryPromptContext.projectCurrent(
                        current = committed.current,
                        availableTokens = DirectPromptContext.InputTokenBudget,
                    )

                assertEquals(
                    DirectWorkingCarryPromptContext.Included,
                    projected.state,
                )
                assertEquals(
                    DirectWorkingCarryPromptContext.CurrentRevisionSource,
                    projected.sourceClass,
                )
                assertEquals(
                    committed.current.revision.revisionRef,
                    projected.sourceRef,
                )
                assertEquals(
                    committed.current.revision.semanticFingerprint,
                    projected.sourceFingerprint,
                )
                assertNotEquals(
                    formation.binding.formationRef,
                    projected.sourceRef,
                )
                assertTrue(
                    projected.included,
                )
                assertNotNull(
                    projected.systemContent,
                )
            }
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }
}

// [VXG RealForever]
