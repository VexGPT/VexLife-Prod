package vexlife.capability

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import vexlife.contract.VexDurableContractFile
import vexlife.home.CanonicalVexLifeHomeFixture

/** Durable reach-pacing currentness: absence is Default; invalid persisted truth is not. */
class VexLifeReachPacingTest {
    private fun withHome(block: (Path) -> Unit) {
        val parent = Files.createTempDirectory("vexlife-reach-pacing-").toAbsolutePath()
        val home = parent.resolve(".vexlife")
        try {
            CanonicalVexLifeHomeFixture.form(home)
            block(home)
        } finally {
            parent.toFile().deleteRecursively()
        }
    }

    @Test
    fun absentSettingMeansThePreExperimentDefault() = withHome { home ->
        assertEquals(VexLifeReachPacingFamily.Default, VexLifeReachPacing(home).current())
    }

    @Test
    fun selectedStatePersistsAsTheExactNextTurnState() = withHome { home ->
        val owner = VexLifeReachPacing(home)
        owner.control("BUDGET_10")
        owner.control("COUNTER_BOUNDED")
        owner.control("FRAMING_TASK_CENTERED")

        assertEquals(
            VexLifeReachPacingFamily.State(
                budget = 10,
                counter = VexLifeReachPacingFamily.Counter.BOUNDED,
                framing = VexLifeReachPacingFamily.Framing.TASK_CENTERED,
            ),
            owner.current(),
        )
    }

    @Test
    fun invalidPersistedSettingDoesNotMasqueradeAsDefault() = withHome { home ->
        val path = home.resolve(VexLifeReachPacing.RelativePath)
        VexDurableContractFile.write(
            ownedRoot = home,
            path = path,
            value =
                VexLifeReachPacing.Stored(
                    schemaVersion = "vexlife-prod.reach-pacing/obsolete",
                    budget = 4,
                    counter = VexLifeReachPacingFamily.Counter.EXACT,
                    framing = VexLifeReachPacingFamily.Framing.STANDARD,
                ),
        )

        assertFailsWith<IllegalArgumentException> {
            VexLifeReachPacing(home).current()
        }
    }
}

// [VXG RealForever]
