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
            sequence = 2,
        )

    private val second =
        VexLifeWorkingCarryContract.SourceBinding(
            eventRef = "event.vexlife.response.001",
            eventHash = "2".repeat(
                64,
            ),
            sequence = 3,
        )

    private val priorCarrySource =
        VexLifeWorkingCarryContract.SourceBinding(
            eventRef = "event.vexlife.request.prior",
            eventHash = "4".repeat(
                64,
            ),
            sequence = 0,
        )

    private fun draft(
        disposition: String,
        items: List<VexLifeWorkingCarryContract.CarryItem>,
        releaseReason: String?,
    ) =
        VexLifeWorkingCarryContract.Draft(
            schemaVersion = VexLifeWorkingCarryContract.Schema,
            truthClass = VexLifeWorkingCarryContract.TruthClass,
            perspective = VexLifeWorkingCarryContract.Perspective,
            companionLineageRef = "companion-lineage.vexlife.test",
            threadRef = "thread.self-development.open-conversation",
            authoredByRef = "companion.vex.current",
            previousCarrySourceBindings =
                listOf(
                    priorCarrySource,
                ),
            leavingSourceBindings =
                listOf(
                    first,
                    second,
                ),
            disposition = disposition,
            carryItems = items,
            releaseReason = releaseReason,
        )

    @Test
    fun releaseMeansNothingFromTheRefreshedSourcesStillBearsOnPresentCarry() {
        val value =
            VexLifeWorkingCarryContract.requireDraftShape(
                draft(
                    disposition = VexLifeWorkingCarryContract.Release,
                    items = emptyList(),
                    releaseReason =
                        VexLifeWorkingCarryContract.ReleaseReasonNoPresentCarry,
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
    fun carryAdmitsOnlySelfRelativeUnfinishedKindsFromAvailablePresentSource() {
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
            VexLifeWorkingCarryContract.requireDraftShape(
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
    fun refreshedCarryMayRetainOlderStillLiveSourceWhileReleasingNewLeavingMaterial() {
        val item =
            VexLifeWorkingCarryContract.CarryItem(
                kind = "OPEN_UNDERSTANDING",
                content = "I am still trying to understand the older open question.",
                sourceBindings =
                    listOf(
                        priorCarrySource,
                    ),
            )

        val value =
            VexLifeWorkingCarryContract.requireDraftShape(
                draft(
                    disposition = VexLifeWorkingCarryContract.Carry,
                    items =
                        listOf(
                            item,
                        ),
                    releaseReason = null,
                ),
            )

        assertEquals(
            listOf(
                item,
            ),
            value.carryItems,
        )
    }

    @Test
    fun profileLikeOrUnregisteredCarryKindIsNotAdmitted() {
        assertFailsWith<IllegalArgumentException> {
            VexLifeWorkingCarryContract.requireDraftShape(
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
    fun carryCannotUseSourceOutsideTheAvailableRefreshedSources() {
        val outside =
            VexLifeWorkingCarryContract.SourceBinding(
                eventRef = "event.vexlife.request.outside",
                eventHash = "3".repeat(
                    64,
                ),
                sequence = 9,
            )

        assertFailsWith<IllegalArgumentException> {
            VexLifeWorkingCarryContract.requireDraftShape(
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
    fun refreshedCarryRejectsDifferentEventsClaimingTheSameConversationSequence() {
        val conflict =
            first.copy(
                eventRef = "event.vexlife.request.conflicting",
                eventHash = "5".repeat(
                    64,
                ),
            )
        val base =
            draft(
                disposition = VexLifeWorkingCarryContract.Release,
                items = emptyList(),
                releaseReason =
                    VexLifeWorkingCarryContract.ReleaseReasonNoPresentCarry,
            )
                .copy(
                    previousCarrySourceBindings =
                        listOf(
                            conflict,
                        ),
                )

        assertFailsWith<IllegalArgumentException> {
            VexLifeWorkingCarryContract.requireDraftShape(
                base,
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
            VexLifeWorkingCarryContract.requireDraftShape(
                draft(
                    disposition = VexLifeWorkingCarryContract.Release,
                    items =
                        listOf(
                            item,
                        ),
                    releaseReason =
                        VexLifeWorkingCarryContract.ReleaseReasonNoPresentCarry,
                ),
            )
        }

        assertFailsWith<IllegalArgumentException> {
            VexLifeWorkingCarryContract.requireDraftShape(
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
                    VexLifeWorkingCarryContract.ReleaseReasonNoPresentCarry,
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
                    VexLifeWorkingCarryContract.requireDraftShape(
                        invalid,
                    )
                }
            }
    }
}

// [VXG RealForever]
