package vexlife.vision

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vexlife.vision.VexLifeVisionFamily.HoldReason

class VexLifeVisionFamilyTest {
    private val family = VexLifeVisionFamily

    private val allStates =
        listOf(
            VexLifeVisionFamily.Held(HoldReason.RouteUnreachable),
            VexLifeVisionFamily.Held(HoldReason.RuntimeUnreadable),
            VexLifeVisionFamily.Held(HoldReason.ScreenSafetyNotCurrent),
            VexLifeVisionFamily.Ready,
            VexLifeVisionFamily.Selecting,
            VexLifeVisionFamily.Engaged,
            VexLifeVisionFamily.Looking,
        )

    private val allControls = listOf(VexLifeVisionFamily.Engage, VexLifeVisionFamily.Stop)

    @Test
    fun observationReconcilesRouteRuntimeAndSdkScopeStates() {
        assertEquals(
            VexLifeVisionFamily.Held(HoldReason.RouteUnreachable),
            family.observe(false, false, null),
        )
        assertEquals(
            VexLifeVisionFamily.Held(HoldReason.RuntimeUnreadable),
            family.observe(true, false, null),
        )
        assertEquals(
            VexLifeVisionFamily.Ready,
            family.observe(true, true, null),
            "no scope session yet",
        )
        assertEquals(VexLifeVisionFamily.Ready, family.observe(true, true, VexLifeVisionFamily.ScopeState.fromWire("ENDED")))
        assertEquals(VexLifeVisionFamily.Selecting, family.observe(true, true, VexLifeVisionFamily.ScopeState.fromWire("SELECTING")))
        assertEquals(VexLifeVisionFamily.Engaged, family.observe(true, true, VexLifeVisionFamily.ScopeState.fromWire("ENGAGED")))
        assertEquals(VexLifeVisionFamily.Looking, family.observe(true, true, VexLifeVisionFamily.ScopeState.fromWire("LOOKING")))
        assertEquals(
            VexLifeVisionFamily.Held(HoldReason.ScreenSafetyNotCurrent),
            family.observe(true, true, VexLifeVisionFamily.ScopeState.fromWire("HELD")),
        )
        assertEquals(
            VexLifeVisionFamily.Held(HoldReason.RuntimeUnreadable),
            family.observe(true, true, VexLifeVisionFamily.ScopeState.fromWire("SOMETHING_NEW")),
            "an unknown SDK state is held, never guessed",
        )
        assertEquals(null, VexLifeVisionFamily.ScopeState.fromWire(null), "no scope session yet")
    }

    @Test
    fun onlyReadyEngagesAndOnlyAnEngagedOrSafetyHeldScopeStops() {
        val lawful = allStates.flatMap { state ->
            allControls.mapNotNull { control ->
                family.resolve(state, control)?.let { state to control }
            }
        }
        assertEquals(
            setOf(
                VexLifeVisionFamily.Ready to VexLifeVisionFamily.Engage,
                VexLifeVisionFamily.Engaged to VexLifeVisionFamily.Stop,
                VexLifeVisionFamily.Held(HoldReason.ScreenSafetyNotCurrent) to
                    VexLifeVisionFamily.Stop,
            ),
            lawful.toSet(),
        )
        assertEquals(
            VexLifeVisionFamily.Effect.EngageScope,
            family.resolve(VexLifeVisionFamily.Ready, VexLifeVisionFamily.Engage)?.effect,
        )
        assertEquals(
            VexLifeVisionFamily.Effect.StopScope,
            family.resolve(VexLifeVisionFamily.Engaged, VexLifeVisionFamily.Stop)?.effect,
        )
    }

    @Test
    fun displayOffersExactlyTheLawfulControl() {
        for (state in allStates) {
            val display = family.displayStateFor(state)
            val lawful =
                allControls.filter { family.resolve(state, it) != null }.map { it.controlRef }
            assertEquals(lawful, listOfNotNull(display.primaryControlRef), state.toString())
        }
    }

    @Test
    fun engagedScopeNeverClaimsCaptureAndLookIsNotOffered() {
        val engaged = family.displayStateFor(VexLifeVisionFamily.Engaged)
        assertTrue(engaged.detail.contains("Nothing is being captured"))
        assertTrue(engaged.detail.contains("can't look"))
        assertEquals(VexLifeVisionFamily.Indicator.Scope.name, engaged.indicatorRef)
        assertNull(VexLifeVisionFamily.controlForRef("LOOK_NOW"))
        for (state in allStates) {
            assertTrue(
                family.displayStateFor(state).primaryLabel?.contains("Look") != true,
                state.toString(),
            )
        }
    }
}

// [VXG RealForever]
