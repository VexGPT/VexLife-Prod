package vexlife.vision

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class VexLifeVisionFamilyTest {
    @Test
    fun heldVisionCannotInventReachabilityOrCaptureAuthority() {
        for (control in listOf(
            VexLifeVisionFamily.Control.Enable,
            VexLifeVisionFamily.Control.Disable,
            VexLifeVisionFamily.Control.LookNow,
        )) {
            assertNull(
                VexLifeVisionFamily.resolve(
                    currentState = VexLifeVisionFamily.State.Held,
                    control = control,
                ),
            )
        }
        assertEquals(
            VexLifeVisionFamily.DisplayState.Held,
            VexLifeVisionFamily.displayStateFor(
                VexLifeVisionFamily.State.Held,
            ),
        )
    }

    @Test
    fun enableIsLocalArmingStateAndPerformsNoOutsideEffect() {
        val act =
            assertNotNull(
                VexLifeVisionFamily.resolve(
                    currentState = VexLifeVisionFamily.State.Ready,
                    control = VexLifeVisionFamily.Control.Enable,
                ),
            )

        assertEquals(
            VexLifeVisionFamily.State.Enabled,
            act.admittedState,
        )
        assertNull(
            act.effect,
        )
    }

    @Test
    fun lookNowRequestsOneSnapshotWithoutClaimingFrameArrival() {
        val act =
            assertNotNull(
                VexLifeVisionFamily.resolve(
                    currentState = VexLifeVisionFamily.State.Enabled,
                    control = VexLifeVisionFamily.Control.LookNow,
                ),
            )

        assertNull(
            act.admittedState,
        )
        assertEquals(
            VexLifeVisionFamily.Effect.RequestSnapshot,
            act.effect,
        )
    }

    @Test
    fun disableOnlyChangesTheLocalArmedSemanticState() {
        val act =
            assertNotNull(
                VexLifeVisionFamily.resolve(
                    currentState = VexLifeVisionFamily.State.Enabled,
                    control = VexLifeVisionFamily.Control.Disable,
                ),
            )

        assertEquals(
            VexLifeVisionFamily.State.Ready,
            act.admittedState,
        )
        assertNull(
            act.effect,
        )
        assertNull(
            VexLifeVisionFamily.resolve(
                currentState = VexLifeVisionFamily.State.Looking,
                control = VexLifeVisionFamily.Control.Disable,
            ),
        )
    }
}

// [VXG RealForever]
