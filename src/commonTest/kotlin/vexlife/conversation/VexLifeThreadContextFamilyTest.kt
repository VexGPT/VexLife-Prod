package vexlife.conversation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Clearing supplied thread context is a horizon, never a deletion, and is lawful only where it means
 * something.
 */
class VexLifeThreadContextFamilyTest {
    private val family = VexLifeThreadContextFamily

    @Test
    fun clearAndRestoreAreLawfulOnlyWhereTheyChangeWhatVexIsGiven() {
        assertNull(
            family.resolve(VexLifeThreadContextFamily.Empty, VexLifeThreadContextFamily.Clear)
        )
        assertEquals(
            VexLifeThreadContextFamily.Effect.SetHorizon,
            family
                .resolve(VexLifeThreadContextFamily.Whole(3), VexLifeThreadContextFamily.Clear)
                ?.effect,
        )
        assertNull(
            family.resolve(VexLifeThreadContextFamily.Whole(3), VexLifeThreadContextFamily.Restore)
        )
        val cleared = VexLifeThreadContextFamily.FromHorizon(2, 1, "2026-11-15T18:00:00Z")
        assertEquals(
            VexLifeThreadContextFamily.Effect.RemoveHorizon,
            family.resolve(cleared, VexLifeThreadContextFamily.Restore)?.effect,
        )
        assertEquals(
            VexLifeThreadContextFamily.Effect.SetHorizon,
            family.resolve(cleared, VexLifeThreadContextFamily.Clear)?.effect,
        )
    }

    @Test
    fun theDisplayIsKeyedAndOffersOnlyLawfulControls() {
        assertEquals(emptyList(), family.displayStateFor(VexLifeThreadContextFamily.Empty).controls)
        val whole = family.displayStateFor(VexLifeThreadContextFamily.Whole(4))
        assertEquals("workspace.status.thread-context-whole", whole.status.key)
        assertEquals(listOf("CLEAR_THREAD_CONTEXT"), whole.controls.map { it.controlRef })
        val justCleared =
            family.displayStateFor(
                VexLifeThreadContextFamily.FromHorizon(4, 0, "2026-11-15T18:00:00Z")
            )
        assertEquals("workspace.status.thread-context-cleared", justCleared.status.key)
        assertEquals(listOf("RESTORE_THREAD_CONTEXT"), justCleared.controls.map { it.controlRef })
        val later =
            family.displayStateFor(
                VexLifeThreadContextFamily.FromHorizon(4, 2, "2026-11-15T18:00:00Z")
            )
        assertEquals(
            listOf("CLEAR_THREAD_CONTEXT", "RESTORE_THREAD_CONTEXT"),
            later.controls.map { it.controlRef },
        )
    }
}
