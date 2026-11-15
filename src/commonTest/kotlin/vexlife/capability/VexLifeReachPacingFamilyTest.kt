package vexlife.capability

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The reach pacing experiment: three budgets, two wordings, the old threshold as the default. */
class VexLifeReachPacingFamilyTest {
    private val family = VexLifeReachPacingFamily

    @Test
    fun theDefaultIsTheThresholdBeforeTheExperiment() {
        assertEquals(4, family.Default.budget)
        assertEquals(VexLifeReachPacingFamily.Counter.EXACT, family.Default.counter)
        assertEquals(24_000, family.Default.exchangeAllowanceTokens)
        assertEquals(
            listOf(24_000, 36_000, 48_000),
            family.Budgets.map {
                VexLifeReachPacingFamily.State(it, family.Default.counter).exchangeAllowanceTokens
            },
        )
    }

    @Test
    fun choosingSavesOnlyARealChange() {
        val chosen = family.resolve(family.Default, VexLifeReachPacingFamily.SetBudget(20))
        assertEquals(
            VexLifeReachPacingFamily.State(20, VexLifeReachPacingFamily.Counter.EXACT),
            chosen?.admittedState,
        )
        assertTrue(chosen?.effect is VexLifeReachPacingFamily.Effect.Save)
        assertNull(family.resolve(family.Default, VexLifeReachPacingFamily.SetBudget(4)))
        assertNull(family.resolve(family.Default, VexLifeReachPacingFamily.SetBudget(7)))
        assertEquals(VexLifeReachPacingFamily.SetBudget(10), family.controlForRef("BUDGET_10"))
        assertNull(family.controlForRef("BUDGET_7"))
        assertEquals(
            VexLifeReachPacingFamily.SetCounter(VexLifeReachPacingFamily.Counter.BOUNDED),
            family.controlForRef("COUNTER_BOUNDED"),
        )
        val display =
            family.displayStateFor(
                VexLifeReachPacingFamily.State(10, VexLifeReachPacingFamily.Counter.BOUNDED)
            )
        assertEquals("workspace.status.reach-pacing-bounded", display.status.key)
        assertEquals(listOf(false, true, false), display.budgets.map { it.selected })
        assertEquals(listOf(false, true), display.counters.map { it.selected })
    }
}
