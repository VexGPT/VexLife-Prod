package vexlife.companion

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vexlife.continuity.VexLifeWorkingCarryContract

class DirectWorkingCarryPromptContextTest {
    private val source =
        VexLifeWorkingCarryContract.SourceBinding(
            eventRef = "event.vexlife.request.prompt-carry.001",
            eventHash = "a".repeat(
                64,
            ),
            sequence = 2,
        )

    private fun carry(
        carryTokenCap: Int =
            4_096,
    ): VexLifeWorkingCarryContract.Draft {
        val item =
            VexLifeWorkingCarryContract.CarryItem(
                kind = "DISTINCTION_TO_PRESERVE",
                content = "I am still carrying that this distinction remains unresolved.",
                sourceBindings =
                    listOf(
                        source,
                    ),
            )
        return VexLifeWorkingCarryContract.Draft(
            schemaVersion = VexLifeWorkingCarryContract.Schema,
            truthClass = VexLifeWorkingCarryContract.TruthClass,
            perspective = VexLifeWorkingCarryContract.Perspective,
            companionLineageRef = "companion-lineage.vexlife.prompt-carry-test",
            threadRef = "thread.self-development.open-conversation",
            authoredByRef = "role.vex.companion",
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
            carryTokenCap = carryTokenCap,
            estimatedCarryTokens =
                VexLifeWorkingCarryContract.estimateCarryContentTokens(
                    listOf(
                        item,
                    ),
                ),
        )
    }

    private fun release() =
        VexLifeWorkingCarryContract.Draft(
            schemaVersion = VexLifeWorkingCarryContract.Schema,
            truthClass = VexLifeWorkingCarryContract.TruthClass,
            perspective = VexLifeWorkingCarryContract.Perspective,
            companionLineageRef = "companion-lineage.vexlife.prompt-carry-test",
            threadRef = "thread.self-development.open-conversation",
            authoredByRef = "role.vex.companion",
            sourceBindings = emptyList(),
            disposition = VexLifeWorkingCarryContract.Release,
            carryItems = emptyList(),
            releaseReason =
                VexLifeWorkingCarryContract.ReleaseReasonNoPresentCarry,
            carryTokenCap = 0,
            estimatedCarryTokens = 0,
        )

    @Test
    fun currentCarryProjectsAsOneBoundedSelfRelativeSystemContext() {
        val projection =
            DirectWorkingCarryPromptContext.materialize(
                draft = carry(),
                sourceState = DirectWorkingCarryPromptContext.OwnerCurrent,
            )

        assertTrue(
            projection.included,
        )
        assertNotNull(
            projection.systemContent,
        )
        assertTrue(
            projection.systemContent.startsWith(
                DirectWorkingCarryPromptContext.Header,
            ),
        )
        assertTrue(
            projection.systemContent.contains(
                "exact current human request",
            ),
        )
        assertTrue(
            projection.systemContent.contains(
                source.eventRef,
            ),
        )
        assertFalse(
            projection.systemContent.contains(
                "VEXLIFE_CURRENT_MEMORY_CONTEXT/v1",
            ),
        )
        assertTrue(
            projection.estimatedSystemMessageTokens <=
                carry().carryTokenCap,
        )
        assertEquals(
            listOf(
                source.eventRef,
            ),
            projection.sourceEventRefs,
        )
        assertNotNull(
            projection.systemMessageSha256,
        )
    }

    @Test
    fun releaseOccupiesNoHumanResponsePromptSpace() {
        val projection =
            DirectWorkingCarryPromptContext.materialize(
                draft = release(),
                sourceState = DirectWorkingCarryPromptContext.OwnerCurrent,
            )

        assertFalse(
            projection.included,
        )
        assertNull(
            projection.systemContent,
        )
        assertNull(
            projection.systemMessageSha256,
        )
        assertEquals(
            0,
            projection.estimatedSystemMessageTokens,
        )
        assertTrue(
            projection.sourceEventRefs.isEmpty(),
        )
    }

    @Test
    fun fullProviderEnvelopeMustFitEvenWhenCarryContentItselfFits() {
        val base =
            carry()
        val contentOnly =
            base.estimatedCarryTokens
        assertTrue(
            contentOnly >
                0,
        )

        val tightened =
            base.copy(
                carryTokenCap = contentOnly,
            )
        VexLifeWorkingCarryContract.requireDraftShape(
            tightened,
        )

        assertFailsWith<IllegalArgumentException> {
            DirectWorkingCarryPromptContext.materialize(
                draft = tightened,
                sourceState = DirectWorkingCarryPromptContext.TurnCandidate,
            )
        }
    }

    @Test
    fun livePromptProjectionCannotInventASecondCarryScaleAboveOutputCeiling() {
        val oversized =
            carry(
                carryTokenCap =
                    DirectCompanionConnection.OutputTokenLimit +
                        1,
            )

        assertFailsWith<IllegalArgumentException> {
            DirectWorkingCarryPromptContext.materialize(
                draft = oversized,
                sourceState = DirectWorkingCarryPromptContext.OwnerCurrent,
            )
        }
    }
}

// [VXG RealForever]
