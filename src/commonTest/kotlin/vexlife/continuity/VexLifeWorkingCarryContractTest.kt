package vexlife.continuity

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class VexLifeWorkingCarryContractTest {
    private val first =
        VexLifeWorkingCarryContract.SourceBinding(
            eventRef = "event.vexlife.request.001",
            eventHash = "1".repeat(
                64,
            ),
        )

    private val second =
        VexLifeWorkingCarryContract.SourceBinding(
            eventRef = "event.vexlife.response.001",
            eventHash = "2".repeat(
                64,
            ),
        )

    private fun draft(
        disposition: String,
        items: List<VexLifeWorkingCarryContract.CarryItem>,
        releaseReason: String?,
    ) =
        VexLifeWorkingCarryContract.Draft(
            schemaVersion = VexLifeWorkingCarryContract.Schema,
            truthClass = VexLifeWorkingCarryContract.TruthClass,
            companionLineageRef = "companion-lineage.vexlife.test",
            threadRef = "thread.self-development.open-conversation",
            authoredByRef = "companion.vex.current",
            observedSourceBindings =
                listOf(
                    first,
                    second,
                ),
            disposition = disposition,
            carryItems = items,
            releaseReason = releaseReason,
        )

    @Test
    fun releaseMeansNothingFromTheLeavingRangeStillBearsOnPresentCarry() {
        val value =
            VexLifeWorkingCarryContract.validateDraft(
                draft(
                    disposition = VexLifeWorkingCarryContract.Release,
                    items = emptyList(),
                    releaseReason =
                        VexLifeWorkingCarryContract.ReleaseReasonNoPresentClaim,
                ),
            )

        assertEquals(
            VexLifeWorkingCarryContract.Release,
            value.disposition,
        )
        assertFalse(
            value.durableMemoryAuthorityGranted,
        )
        assertFalse(
            value.humanProfileAuthorityGranted,
        )
        assertFalse(
            value.relationshipAgreementAuthorityGranted,
        )
        assertFalse(
            value.canonicalConversationAuthorityGranted,
        )
        assertFalse(
            value.memoryPromotionPerformed,
        )
        assertFalse(
            value.effectsPerformed,
        )
    }

    @Test
    fun carryAdmitsOnlySelfRelativeUnfinishedKindsBoundToExactLeavingSource() {
        val items =
            VexLifeWorkingCarryContract.CarryKinds
                .sorted()
                .map {
                    kind ->
                    VexLifeWorkingCarryContract.CarryItem(
                        kind = kind,
                        content =
                            "Vex still carries this " +
                                kind +
                                " relationship to the present Conversation.",
                        sourceBindings =
                            listOf(
                                first,
                            ),
                    )
                }

        val value =
            VexLifeWorkingCarryContract.validateDraft(
                draft(
                    disposition = VexLifeWorkingCarryContract.Carry,
                    items = items,
                    releaseReason = null,
                ),
            )

        assertEquals(
            items,
            value.carryItems,
        )
    }

    @Test
    fun profileLikeOrUnregisteredCarryKindIsNotAdmitted() {
        assertFailsWith<IllegalArgumentException> {
            VexLifeWorkingCarryContract.validateDraft(
                draft(
                    disposition = VexLifeWorkingCarryContract.Carry,
                    items =
                        listOf(
                            VexLifeWorkingCarryContract.CarryItem(
                                kind = "HUMAN_PROFILE_FACT",
                                content = "Define the human from above.",
                                sourceBindings =
                                    listOf(
                                        first,
                                    ),
                            ),
                        ),
                    releaseReason = null,
                ),
            )
        }
    }

    @Test
    fun carryCannotUseSourceOutsideTheObservedLeavingRange() {
        val outside =
            VexLifeWorkingCarryContract.SourceBinding(
                eventRef = "event.vexlife.request.outside",
                eventHash = "3".repeat(
                    64,
                ),
            )

        assertFailsWith<IllegalArgumentException> {
            VexLifeWorkingCarryContract.validateDraft(
                draft(
                    disposition = VexLifeWorkingCarryContract.Carry,
                    items =
                        listOf(
                            VexLifeWorkingCarryContract.CarryItem(
                                kind = "OPEN_UNDERSTANDING",
                                content = "I am still trying to understand this.",
                                sourceBindings =
                                    listOf(
                                        outside,
                                    ),
                            ),
                        ),
                    releaseReason = null,
                ),
            )
        }
    }

    @Test
    fun releaseCannotHideCarryItemsAndCarryCannotBeEmpty() {
        val item =
            VexLifeWorkingCarryContract.CarryItem(
                kind = "OWED_CONTINUATION",
                content = "I still owe a continuation.",
                sourceBindings =
                    listOf(
                        first,
                    ),
            )

        assertFailsWith<IllegalArgumentException> {
            VexLifeWorkingCarryContract.validateDraft(
                draft(
                    disposition = VexLifeWorkingCarryContract.Release,
                    items =
                        listOf(
                            item,
                        ),
                    releaseReason =
                        VexLifeWorkingCarryContract.ReleaseReasonNoPresentClaim,
                ),
            )
        }

        assertFailsWith<IllegalArgumentException> {
            VexLifeWorkingCarryContract.validateDraft(
                draft(
                    disposition = VexLifeWorkingCarryContract.Carry,
                    items = emptyList(),
                    releaseReason = null,
                ),
            )
        }
    }

    @Test
    fun workingCarryCannotPromoteItselfIntoMemoryProfileAgreementOrEffectAuthority() {
        val base =
            draft(
                disposition = VexLifeWorkingCarryContract.Release,
                items = emptyList(),
                releaseReason =
                    VexLifeWorkingCarryContract.ReleaseReasonNoPresentClaim,
            )

        listOf(
            base.copy(
                durableMemoryAuthorityGranted = true,
            ),
            base.copy(
                humanProfileAuthorityGranted = true,
            ),
            base.copy(
                relationshipAgreementAuthorityGranted = true,
            ),
            base.copy(
                canonicalConversationAuthorityGranted = true,
            ),
            base.copy(
                memoryPromotionPerformed = true,
            ),
            base.copy(
                effectsPerformed = true,
            ),
        )
            .forEach {
                invalid ->
                assertFailsWith<IllegalArgumentException> {
                    VexLifeWorkingCarryContract.validateDraft(
                        invalid,
                    )
                }
            }
    }
}

// [VXG RealForever]
