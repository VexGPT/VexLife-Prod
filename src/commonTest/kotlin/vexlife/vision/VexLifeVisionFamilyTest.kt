package vexlife.vision

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class VexLifeVisionFamilyTest {
    @Test
    fun heldVisionCannotInventReachabilityOrCaptureAuthority() {
        assertNull(
            VexLifeVisionFamily.resolve(
                currentState = VexLifeVisionFamily.State.Held,
                control = VexLifeVisionFamily.Control.LookNow,
            ),
        )
        assertEquals(
            VexLifeVisionFamily.DisplayState.Held,
            VexLifeVisionFamily.displayStateFor(
                VexLifeVisionFamily.State.Held,
            ),
        )
    }

    @Test
    fun lookNowEntersOneInProgressStateAndRequestsOneSnapshot() {
        val act =
            assertNotNull(
                VexLifeVisionFamily.resolve(
                    currentState = VexLifeVisionFamily.State.Ready,
                    control = VexLifeVisionFamily.Control.LookNow,
                ),
            )

        assertEquals(
            VexLifeVisionFamily.State.Looking,
            act.admittedState,
        )
        assertEquals(
            VexLifeVisionFamily.Effect.RequestSnapshot,
            act.effect,
        )
    }

    @Test
    fun lookingCannotStartAnotherConcurrentLookNow() {
        assertNull(
            VexLifeVisionFamily.resolve(
                currentState = VexLifeVisionFamily.State.Looking,
                control = VexLifeVisionFamily.Control.LookNow,
            ),
        )
        assertEquals(
            VexLifeVisionFamily.DisplayState.Looking,
            VexLifeVisionFamily.displayStateFor(
                VexLifeVisionFamily.State.Looking,
            ),
        )
    }
}

// [VXG RealForever]
