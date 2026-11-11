package vexlife.continuity

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VexLifeWorkingCarryContractTest {
    private fun binding(
        sequence: Long,
        suffix: String =
            sequence.toString(),
    ) =
        VexLifeWorkingCarryContract.SourceBinding(
            eventRef =
                "event.vexlife." +
                    suffix,
            eventHash =
                sequence
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
        kind: String,
        content: String =
            "Conversation source " +
                sequence,
    ) =
        VexLifeWorkingCarryContract.SourceEvent(
            source =
                binding(
                    sequence,
                ),
            eventKind = kind,
            speakerRef =
                if (
                    kind ==
                        "REQUEST"
                ) {
                    "person.victor"
                } else {
                    "companion.vex"
                },
            recipientRefs =
                listOf(
                    if (
                        kind ==
                            "REQUEST"
                    ) {
                        "companion.vex"
                    } else {
                        "person.victor"
                    },
                ),
            content = content,
        )

    private val older =
        binding(
            0,
            "prior",
        )

    private val leavingRequest =
        binding(
            2,
            "request.001",
        )

    private val leavingResponse =
        binding(
            3,
            "response.001",
        )

    private fun carryItem(
        source: VexLifeWorkingCarryContract.SourceBinding =
            leavingRequest,
        kind: String =
            "OPEN_UNDERSTANDING",
        content: String =
            "I am still trying to understand this.",
    ) =
        VexLifeWorkingCarryContract.CarryItem(
            kind = kind,
            content = content,
            sourceBindings =
                listOf(
                    source,
                ),
        )

    private fun draft(
        items: List<VexLifeWorkingCarryContract.CarryItem>,
        disposition: String =
            if (
                items.isEmpty()
            ) {
                VexLifeWorkingCarryContract.Release
            } else {
                VexLifeWorkingCarryContract.Carry
            },
        releaseReason: String? =
            if (
                items.isEmpty()
            ) {
                VexLifeWorkingCarryContract.ReleaseReasonNoPresentCarry
            } else {
                null
            },
        carryTokenCap: Int =
            16_000,
    ): VexLifeWorkingCarryContract.Draft {
        val source =
            items.flatMap {
                it.sourceBindings
            }
                .distinct()
                .sortedBy {
                    it.sequence
                }
        return VexLifeWorkingCarryContract.Draft(
            schemaVersion = VexLifeWorkingCarryContract.Schema,
            truthClass = VexLifeWorkingCarryContract.TruthClass,
            perspective = VexLifeWorkingCarryContract.Perspective,
            companionLineageRef = "companion-lineage.vexlife.test",
            threadRef = "thread.self-development.open-conversation",
            authoredByRef = "companion.vex.current",
            sourceBindings = source,
            disposition = disposition,
            carryItems = items,
            releaseReason = releaseReason,
            carryTokenCap = carryTokenCap,
            estimatedCarryTokens =
                VexLifeWorkingCarryContract.estimateCarryContentTokens(
                    items,
                ),
        )
    }

    private fun refresh(
        previousCarry: VexLifeWorkingCarryContract.Draft? =
            draft(
                listOf(
                    carryItem(
                        source = older,
                        content = "I still carry the older open question.",
                    ),
                ),
            ),
        carryTokenCap: Int =
            16_000,
    ) =
        VexLifeWorkingCarryContract.RefreshInput(
            schemaVersion = VexLifeWorkingCarryContract.RefreshInputSchema,
            perspective = VexLifeWorkingCarryContract.Perspective,
            companionLineageRef = "companion-lineage.vexlife.test",
            threadRef = "thread.self-development.open-conversation",
            previousCarry = previousCarry,
            newlyLeavingEvents =
                listOf(
                    VexLifeWorkingCarryContract.SourceEvent(
                        source = leavingRequest,
                        eventKind = "REQUEST",
                        speakerRef = "person.victor",
                        recipientRefs =
                            listOf(
                                "companion.vex",
                            ),
                        content = "A leaving human request.",
                    ),
                    VexLifeWorkingCarryContract.SourceEvent(
                        source = leavingResponse,
                        eventKind = "RESPONSE",
                        speakerRef = "companion.vex",
                        recipientRefs =
                            listOf(
                                "person.victor",
                            ),
                        content = "A leaving Vex response.",
                    ),
                ),
            retainedDirectEvents =
                listOf(
                    event(
                        4,
                        "REQUEST",
                    ),
                    event(
                        5,
                        "RESPONSE",
                    ),
                ),
            currentRequest =
                event(
                    6,
                    "REQUEST",
                    "Current human request.",
                ),
            carryTokenCap = carryTokenCap,
        )

    @Test
    fun refreshedReleaseCarriesNoSemanticOrSourceResidue() {
        val value =
            VexLifeWorkingCarryContract.requireDraftShape(
                draft(
                    items = emptyList(),
                    carryTokenCap = 0,
                ),
            )

        assertEquals(
            VexLifeWorkingCarryContract.Release,
            value.disposition,
        )
        assertTrue(
            value.sourceBindings.isEmpty(),
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
    fun carryAdmitsOnlySelfRelativeKindsAndOnlySourceStillUsedNow() {
        val items =
            VexLifeWorkingCarryContract.CarryKinds
                .sorted()
                .map {
                    kind ->
                    carryItem(
                        kind = kind,
                        content =
                            "Vex still carries this " +
                                kind +
                                " relationship to the present Conversation.",
                    )
                }

        val value =
            VexLifeWorkingCarryContract.requireDraftShape(
                draft(
                    items,
                ),
            )

        assertEquals(
            listOf(
                leavingRequest,
            ),
            value.sourceBindings,
        )
        assertEquals(
            items,
            value.carryItems,
        )

        assertFailsWith<IllegalArgumentException> {
            VexLifeWorkingCarryContract.requireDraftShape(
                draft(
                    items =
                        listOf(
                            carryItem(
                                kind = "HUMAN_PROFILE_FACT",
                            ),
                        ),
                ),
            )
        }
    }

    @Test
    fun refreshedCarryMayRetainOlderStillLiveSourceAndReleaseNewLeavingMaterial() {
        val priorItem =
            carryItem(
                source = older,
                content = "I still carry the older open question.",
            )
        val input =
            refresh(
                previousCarry =
                    draft(
                        listOf(
                            priorItem,
                        ),
                    ),
            )
        val output =
            draft(
                listOf(
                    priorItem,
                ),
            )

        assertEquals(
            output,
            VexLifeWorkingCarryContract.requireDraftFromRefresh(
                refresh = input,
                draft = output,
            ),
        )
    }

    @Test
    fun refreshReviewContextCannotSilentlyBecomeCarrySourceBeforeItLeaves() {
        val input =
            refresh(
                previousCarry = null,
            )
        val retained =
            input.retainedDirectEvents
                .first()
                .source
        val output =
            draft(
                listOf(
                    carryItem(
                        source = retained,
                    ),
                ),
            )

        assertFailsWith<IllegalArgumentException> {
            VexLifeWorkingCarryContract.requireDraftFromRefresh(
                refresh = input,
                draft = output,
            )
        }
    }

    @Test
    fun refreshInputIsConversationLocalAndExcludesDurableMemoryAndEffects() {
        val input =
            refresh()

        assertEquals(
            input,
            VexLifeWorkingCarryContract.requireRefreshInputShape(
                input,
            ),
        )

        assertFailsWith<IllegalArgumentException> {
            VexLifeWorkingCarryContract.requireRefreshInputShape(
                input.copy(
                    durableMemoryIncluded = true,
                ),
            )
        }
        assertFailsWith<IllegalArgumentException> {
            VexLifeWorkingCarryContract.requireRefreshInputShape(
                input.copy(
                    persistencePerformed = true,
                ),
            )
        }
        assertFailsWith<IllegalArgumentException> {
            VexLifeWorkingCarryContract.requireRefreshInputShape(
                input.copy(
                    effectsPerformed = true,
                ),
            )
        }
    }

    @Test
    fun newlyLeavingDirectPresenceIsOnlyTheOldestContiguousPriorPrefix() {
        val previous =
            (0L..3L)
                .map(
                    ::binding,
                )
        val current =
            (2L..5L)
                .map(
                    ::binding,
                )

        assertEquals(
            previous.take(
                2,
            ),
            VexLifeWorkingCarryContract.newlyLeavingDirectPresence(
                previousDirectPresence = previous,
                currentDirectPresence = current,
            ),
        )
        assertTrue(
            VexLifeWorkingCarryContract.newlyLeavingDirectPresence(
                previousDirectPresence =
                    previous.take(
                        2,
                    ),
                currentDirectPresence = previous,
            )
                .isEmpty(),
        )
        assertEquals(
            previous,
            VexLifeWorkingCarryContract.newlyLeavingDirectPresence(
                previousDirectPresence = previous,
                currentDirectPresence =
                    (4L..5L)
                        .map(
                            ::binding,
                        ),
            ),
        )
    }

    @Test
    fun directPresenceTransitionRejectsHolesAndSourceGenerationSubstitution() {
        val previous =
            (0L..5L)
                .map(
                    ::binding,
                )

        assertFailsWith<IllegalArgumentException> {
            VexLifeWorkingCarryContract.newlyLeavingDirectPresence(
                previousDirectPresence = previous,
                currentDirectPresence =
                    listOf(
                        binding(
                            0,
                        ),
                        binding(
                            1,
                        ),
                        binding(
                            4,
                        ),
                        binding(
                            5,
                        ),
                    ),
            )
        }

        val substituted =
            binding(
                4,
            )
                .copy(
                    eventRef =
                        "event.vexlife.substituted",
                    eventHash =
                        "f".repeat(
                            64,
                        ),
                )

        assertFailsWith<IllegalArgumentException> {
            VexLifeWorkingCarryContract.newlyLeavingDirectPresence(
                previousDirectPresence = previous,
                currentDirectPresence =
                    listOf(
                        substituted,
                        binding(
                            5,
                        ),
                    ),
            )
        }
    }

    @Test
    fun refreshedCarryMustFitTheCallerSuppliedFiniteAllowance() {
        val item =
            carryItem()
        val base =
            draft(
                listOf(
                    item,
                ),
            )

        assertFailsWith<IllegalArgumentException> {
            VexLifeWorkingCarryContract.requireDraftShape(
                base.copy(
                    carryTokenCap = 0,
                ),
            )
        }
        assertFailsWith<IllegalArgumentException> {
            VexLifeWorkingCarryContract.requireDraftShape(
                base.copy(
                    estimatedCarryTokens =
                        base.estimatedCarryTokens +
                            1,
                ),
            )
        }
    }

    @Test
    fun workingCarryCannotRetainUnusedSourceOrPromoteAuthority() {
        val item =
            carryItem()
        val base =
            draft(
                listOf(
                    item,
                ),
            )

        assertFailsWith<IllegalArgumentException> {
            VexLifeWorkingCarryContract.requireDraftShape(
                base.copy(
                    sourceBindings =
                        listOf(
                            leavingRequest,
                            leavingResponse,
                        ),
                ),
            )
        }

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
