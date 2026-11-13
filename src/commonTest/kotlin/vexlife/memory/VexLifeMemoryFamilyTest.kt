package vexlife.memory

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VexLifeMemoryFamilyTest {
    private fun observe(
        readable: Boolean = true,
        current: Boolean = false,
        reasons: List<String> = emptyList(),
        count: Int = 0,
    ) = VexLifeMemoryFamily.observe(readable, current, reasons, count)

    @Test
    fun readerTruthMapsToOneHumanMeaning() {
        assertEquals(VexLifeMemoryFamily.Current(3), observe(current = true, count = 3))
        assertEquals(
            VexLifeMemoryFamily.NotFormed,
            observe(reasons = listOf(VexLifeMemoryFamily.ReasonNoCommittedDailyMemory)),
        )
        assertEquals(
            VexLifeMemoryFamily.Behind,
            observe(reasons = listOf(VexLifeMemoryFamily.ReasonDailyMemoryStale)),
        )
        assertEquals(VexLifeMemoryFamily.Unreadable, observe(readable = false))
        assertEquals(
            VexLifeMemoryFamily.Unreadable,
            observe(reasons = listOf("SOMETHING_NEW")),
            "an unknown hold is not guessed",
        )
    }

    @Test
    fun displayNeverPromisesMemoryVexIsNotGiven() {
        val behind = VexLifeMemoryFamily.displayStateFor(VexLifeMemoryFamily.Behind)
        assertTrue(
            "isn't drawing on it" in behind.detail && "Nothing in it was lost" in behind.detail
        )
        val notFormed = VexLifeMemoryFamily.displayStateFor(VexLifeMemoryFamily.NotFormed)
        assertTrue("won't recall earlier conversations" in notFormed.detail)
        assertEquals(
            "Vex's Memory is up to date and doesn't hold anything yet.",
            VexLifeMemoryFamily.displayStateFor(VexLifeMemoryFamily.Current(0)).detail,
        )
    }
}

// [VXG RealForever]
