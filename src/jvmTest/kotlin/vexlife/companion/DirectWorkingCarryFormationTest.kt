package vexlife.companion

import java.time.Instant
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import vexlife.continuity.VexLifeWorkingCarryContract
import vexlife.conversation.DirectConversationIdentity
import vexlife.model.heretic.HereticModelConfiguration
import vexlife.model.heretic.HereticProductIdentity
import vexlife.model.heretic.HereticRuntimeQualification
import vexlife.model.heretic.HereticRuntimeState

class DirectWorkingCarryFormationTest {
    private fun runtimeEvidence() =
        HereticModelConfiguration.QualifiedRuntimeEvidence(
            runtimeReceiptRef = "receipt.vexlife-prod.heretic.working-carry",
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
            eventRef =
                "event.vexlife." +
                    suffix,
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

    private fun event(
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

    private fun priorCarry(): VexLifeWorkingCarryContract.Draft {
        val source =
            binding(
                0,
                "prior.request",
            )
        val item =
            VexLifeWorkingCarryContract.CarryItem(
                kind = "OPEN_UNDERSTANDING",
                content = "I am still trying to understand the earlier distinction.",
                sourceBindings =
                    listOf(
                        source,
                    ),
            )
        return VexLifeWorkingCarryContract.Draft(
            schemaVersion = VexLifeWorkingCarryContract.Schema,
            truthClass = VexLifeWorkingCarryContract.TruthClass,
            perspective = VexLifeWorkingCarryContract.Perspective,
            companionLineageRef = "companion-lineage.vexlife.working-carry-test",
            threadRef = DirectConversationIdentity.ThreadRef,
            authoredByRef = DirectConversationIdentity.CompanionRoleRef,
            sourceBindings =
                listOf(
                    source,
                ),
            disposition = VexLifeWorkingCarryContract.Carry,
            carryItems =
                listOf(
                    item,
                ),
            releaseReason = null,
            carryTokenCap = 512,
            estimatedCarryTokens =
                VexLifeWorkingCarryContract.estimateCarryContentTokens(
                    listOf(
                        item,
                    ),
                ),
        )
    }

    private fun refresh(
        carryTokenCap: Int =
            512,
        previousCarry: VexLifeWorkingCarryContract.Draft? =
            priorCarry(),
    ) =
        VexLifeWorkingCarryContract.RefreshInput(
            schemaVersion = VexLifeWorkingCarryContract.RefreshInputSchema,
            perspective = VexLifeWorkingCarryContract.Perspective,
            companionLineageRef = "companion-lineage.vexlife.working-carry-test",
            threadRef = DirectConversationIdentity.ThreadRef,
            previousCarry = previousCarry,
            newlyLeavingEvents =
                listOf(
                    event(
                        sequence = 2,
                        suffix = "leaving.request",
                        kind = "REQUEST",
                        content = "Please keep this distinction open; we have not resolved it yet.",
                    ),
                    event(
                        sequence = 3,
                        suffix = "leaving.response",
                        kind = "RESPONSE",
                        content = "I understand that the distinction remains unresolved.",
                    ),
                ),
            retainedDirectEvents =
                listOf(
                    event(
                        sequence = 4,
                        suffix = "retained.request",
                        kind = "REQUEST",
                        content = "We are still working through the same subject.",
                    ),
                    event(
                        sequence = 5,
                        suffix = "retained.response",
                        kind = "RESPONSE",
                        content = "The later exchange has not resolved the earlier distinction.",
                    ),
                ),
            currentRequest =
                event(
                    sequence = 6,
                    suffix = "current.request",
                    kind = "REQUEST",
                    content = "Continue with what is still alive now.",
                ),
            carryTokenCap = carryTokenCap,
        )

    private fun transport(
        content: String,
        inspectRequest: (
            DirectCompanionConnection.HttpCall,
        ) -> Unit = {},
    ) =
        DirectCompanionConnection.Transport {
            call ->
            inspectRequest(
                call,
            )
            val responseBody =
                """
                {
                  "model": "default_model",
                  "choices": [
                    {
                      "message": {
                        "role": "assistant",
                        "reasoning_content": "private continuity reasoning that must not become a human answer",
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
                            content,
                        )
                            .toString(),
                    )

            DirectCompanionConnection.HttpResult(
                status = 200,
                body = responseBody,
            )
        }

    @Test
    fun localVexFormsBoundedSelfRelativeCarryWithoutHumanAnswerOrRawReasoningPersistence() {
        val input =
            refresh()
        val leavingRef =
            input.newlyLeavingEvents
                .first()
                .source
                .eventRef
        val output =
            """
            {
              "disposition": "CARRY",
              "carryItems": [
                {
                  "kind": "DISTINCTION_TO_PRESERVE",
                  "content": "I am still carrying that this distinction remains unresolved.",
                  "sourceEventRefs": ["__LEAVING_REF__"]
                }
              ],
              "releaseReason": null
            }
            """.trimIndent()
                .replace(
                    "__LEAVING_REF__",
                    leavingRef,
                )

        val formation =
            DirectWorkingCarryFormation.form(
                runtime = runtimeEvidence(),
                refreshInput = input,
                transport =
                    transport(
                        output,
                    ) {
                        call ->
                        assertTrue(
                            call.body.contains(
                                "DO_NOT_ANSWER_THE_HUMAN",
                            ),
                        )
                        assertTrue(
                            call.body.contains(
                                "Continue with what is still alive now.",
                            ),
                        )
                        assertFalse(
                            call.body.contains(
                                "VEXLIFE_CURRENT_MEMORY_CONTEXT/v1",
                            ),
                        )
                    },
                now = {
                    Instant.parse(
                        "2026-11-10T21:00:00Z",
                    )
                },
            )

        assertEquals(
            VexLifeWorkingCarryContract.Carry,
            formation.draft.disposition,
        )
        assertEquals(
            DirectConversationIdentity.CompanionRoleRef,
            formation.draft.authoredByRef,
        )
        assertEquals(
            listOf(
                input.newlyLeavingEvents
                    .first()
                    .source,
            ),
            formation.draft.sourceBindings,
        )
        assertFalse(
            formation.rawProviderOutputPersisted,
        )
        assertFalse(
            formation.rawReasoningPersisted,
        )
        assertFalse(
            formation.humanAnswerProduced,
        )
        assertFalse(
            formation.runtimeObservation.rawProviderResponsePersisted,
        )
        assertFalse(
            formation.runtimeObservation.reasoningTrace.rawPersisted,
        )
        assertTrue(
            formation.invocationEvidence.actualHttpCall,
        )
        assertTrue(
            formation.invocationEvidence.loopbackOnly,
        )
        assertFalse(
            formation.invocationEvidence.observedEffects.nativeToolExecutionObserved,
        )
        assertFalse(
            formation.invocationEvidence.observedEffects.trainingEffectObserved,
        )
        assertFalse(
            formation.invocationEvidence.observedEffects.modelWeightEffectObserved,
        )
    }

    @Test
    fun releaseOutputCarriesNoSourceAndUsesExactReleaseMeaning() {
        val formation =
            DirectWorkingCarryFormation.form(
                runtime = runtimeEvidence(),
                refreshInput =
                    refresh(
                        carryTokenCap = 0,
                    ),
                transport =
                    transport(
                        """
                        {
                          "disposition": "RELEASE",
                          "carryItems": [],
                          "releaseReason": "NO_PRESENT_CARRY_REMAINS"
                        }
                        """.trimIndent(),
                    ),
            )

        assertEquals(
            VexLifeWorkingCarryContract.Release,
            formation.draft.disposition,
        )
        assertTrue(
            formation.draft.sourceBindings.isEmpty(),
        )
        assertTrue(
            formation.draft.carryItems.isEmpty(),
        )
        assertEquals(
            0,
            formation.draft.estimatedCarryTokens,
        )
    }

    @Test
    fun retainedDirectSourceCannotBecomeCarryProvenanceBeforeItLeaves() {
        val input =
            refresh(
                previousCarry = null,
            )
        val retainedRef =
            input.retainedDirectEvents
                .first()
                .source
                .eventRef
        val output =
            """
            {
              "disposition": "CARRY",
              "carryItems": [
                {
                  "kind": "OPEN_UNDERSTANDING",
                  "content": "This source is still directly present and cannot become carry provenance yet.",
                  "sourceEventRefs": ["__RETAINED_REF__"]
                }
              ],
              "releaseReason": null
            }
            """.trimIndent()
                .replace(
                    "__RETAINED_REF__",
                    retainedRef,
                )

        assertFailsWith<IllegalArgumentException> {
            DirectWorkingCarryFormation.form(
                runtime = runtimeEvidence(),
                refreshInput = input,
                transport =
                    transport(
                        output,
                    ),
            )
        }
    }

    @Test
    fun modelCarryLargerThanReservedAllowanceFailsInsteadOfSilentlyTruncatingMeaning() {
        val input =
            refresh(
                carryTokenCap = 1,
                previousCarry = null,
            )
        val leavingRef =
            input.newlyLeavingEvents
                .first()
                .source
                .eventRef
        val output =
            """
            {
              "disposition": "CARRY",
              "carryItems": [
                {
                  "kind": "OWED_CONTINUATION",
                  "content": "This output intentionally exceeds the tiny reserved carry allowance.",
                  "sourceEventRefs": ["__LEAVING_REF__"]
                }
              ],
              "releaseReason": null
            }
            """.trimIndent()
                .replace(
                    "__LEAVING_REF__",
                    leavingRef,
                )

        assertFailsWith<IllegalArgumentException> {
            DirectWorkingCarryFormation.form(
                runtime = runtimeEvidence(),
                refreshInput = input,
                transport =
                    transport(
                        output,
                    ),
            )
        }
    }
}

// [VXG RealForever]
