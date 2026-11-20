package vexlife.conversation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VexLifeConversationTurnFamilyTest {
    private val attempt =
        VexLifeConversationTurnFamily.Attempt(
            "send.vexlife.family.001",
        )

    @Test
    fun observedExecutionCustodyFormsOneClosedStateRelationship() {
        assertEquals(
            VexLifeConversationTurnFamily.Ready,
            VexLifeConversationTurnFamily.observe(
                activeAttemptRef = null,
                cancellationRequested = false,
            ),
        )
        assertEquals(
            VexLifeConversationTurnFamily.Active(
                attempt,
            ),
            VexLifeConversationTurnFamily.observe(
                activeAttemptRef = attempt.ref,
                cancellationRequested = false,
            ),
        )
        assertEquals(
            VexLifeConversationTurnFamily.Cancelling(
                attempt,
            ),
            VexLifeConversationTurnFamily.observe(
                activeAttemptRef = attempt.ref,
                cancellationRequested = true,
            ),
        )
    }

    @Test
    fun sendAndStopHaveExactLawfulEdges() {
        val send =
            VexLifeConversationTurnFamily.resolve(
                VexLifeConversationTurnFamily.Ready,
                VexLifeConversationTurnFamily.Send(
                    attempt,
                ),
            )
        assertEquals(
            VexLifeConversationTurnFamily.BeginTurn(
                attempt,
            ),
            send?.effect,
        )

        val stop =
            VexLifeConversationTurnFamily.resolve(
                VexLifeConversationTurnFamily.Active(
                    attempt,
                ),
                VexLifeConversationTurnFamily.Stop(
                    attempt,
                ),
            )
        assertEquals(
            VexLifeConversationTurnFamily.RequestCancel(
                attempt,
            ),
            stop?.effect,
        )

        assertNull(
            VexLifeConversationTurnFamily.resolve(
                VexLifeConversationTurnFamily.Ready,
                VexLifeConversationTurnFamily.Stop(
                    attempt,
                ),
            ),
        )
        assertNull(
            VexLifeConversationTurnFamily.resolve(
                VexLifeConversationTurnFamily.Cancelling(
                    attempt,
                ),
                VexLifeConversationTurnFamily.Stop(
                    attempt,
                ),
            ),
        )
        assertNull(
            VexLifeConversationTurnFamily.resolve(
                VexLifeConversationTurnFamily.Active(
                    attempt,
                ),
                VexLifeConversationTurnFamily.Stop(
                    VexLifeConversationTurnFamily.Attempt(
                        "send.vexlife.family.other",
                    ),
                ),
            ),
            "Stop must never cross attempt identity",
        )
    }

    @Test
    fun displayStateReturnsTheLawfulControlRelationshipToPresentation() {
        val ready =
            VexLifeConversationTurnFamily.displayStateFor(
                VexLifeConversationTurnFamily.Ready,
            )
        assertEquals(
            VexLifeConversationTurnFamily.ControlSend,
            ready.primaryControlRef,
        )
        assertEquals(
            "workspace.button.conversation-send",
            ready.primaryLabel?.key,
        )
        assertNull(
            ready.targetAttemptRef,
        )

        val active =
            VexLifeConversationTurnFamily.displayStateFor(
                VexLifeConversationTurnFamily.Active(
                    attempt,
                ),
            )
        assertEquals(
            VexLifeConversationTurnFamily.ControlStop,
            active.primaryControlRef,
        )
        assertEquals(
            attempt.ref,
            active.targetAttemptRef,
        )
        assertTrue(
            active.active,
        )

        val cancelling =
            VexLifeConversationTurnFamily.displayStateFor(
                VexLifeConversationTurnFamily.Cancelling(
                    attempt,
                ),
            )
        assertNull(
            cancelling.primaryControlRef,
        )
        assertTrue(
            cancelling.cancelling,
        )
    }

}

// [VXG RealForever]
