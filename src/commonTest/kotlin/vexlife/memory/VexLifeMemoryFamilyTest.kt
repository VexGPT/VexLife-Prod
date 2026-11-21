package vexlife.memory

import kotlin.test.Test
import kotlin.test.assertEquals

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
    fun displayKeepsMemoryMeaningInOwnerSelectedKeys() {
        assertEquals(
            "workspace.description.memory-behind",
            VexLifeMemoryFamily
                .displayStateFor(
                    VexLifeMemoryFamily.Behind,
                )
                .detail
                .key,
        )
        assertEquals(
            "workspace.description.memory-not-formed",
            VexLifeMemoryFamily
                .displayStateFor(
                    VexLifeMemoryFamily.NotFormed,
                )
                .detail
                .key,
        )
        assertEquals(
            "workspace.description.memory-current-empty",
            VexLifeMemoryFamily
                .displayStateFor(
                    VexLifeMemoryFamily.Current(
                        0,
                    ),
                )
                .detail
                .key,
        )
        assertEquals(
            "workspace.description.memory-current-one",
            VexLifeMemoryFamily
                .displayStateFor(
                    VexLifeMemoryFamily.Current(
                        1,
                    ),
                )
                .detail
                .key,
        )
        val many =
            VexLifeMemoryFamily
                .displayStateFor(
                    VexLifeMemoryFamily.Current(
                        3,
                    ),
                )
                .detail
        assertEquals(
            "workspace.description.memory-current-many",
            many.key,
        )
        assertEquals(
            "3",
            many.args["count"],
        )
    }
}

// [VXG RealForever]
