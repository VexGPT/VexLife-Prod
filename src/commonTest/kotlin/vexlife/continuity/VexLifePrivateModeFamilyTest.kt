package vexlife.continuity

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VexLifePrivateModeFamilyTest {
    @Test
    fun onlyKeptCanGoPrivateAndOnlyPrivateCanLeave() {
        val private = VexLifePrivateModeFamily.Private(exchangeCount = 2, keptNoteCount = 0)
        assertEquals(
            VexLifePrivateModeFamily.Effect.BeginPrivate,
            VexLifePrivateModeFamily.resolve(
                    VexLifePrivateModeFamily.Kept,
                    VexLifePrivateModeFamily.Begin,
                )
                ?.effect,
        )
        assertEquals(
            VexLifePrivateModeFamily.Effect.EndPrivate,
            VexLifePrivateModeFamily.resolve(private, VexLifePrivateModeFamily.End)?.effect,
        )
        assertNull(VexLifePrivateModeFamily.resolve(private, VexLifePrivateModeFamily.Begin))
        assertNull(
            VexLifePrivateModeFamily.resolve(
                VexLifePrivateModeFamily.Kept,
                VexLifePrivateModeFamily.End,
            )
        )
    }

    @Test
    fun displayTellsThePersonWhatIsKept() {
        val kept = VexLifePrivateModeFamily.displayStateFor(VexLifePrivateModeFamily.Kept)
        assertEquals("Kept", kept.label)
        assertTrue("saved in your archives" in kept.detail)
        assertEquals(VexLifePrivateModeFamily.ControlBegin, kept.primaryControlRef)
        val private =
            VexLifePrivateModeFamily.displayStateFor(VexLifePrivateModeFamily.Private(0, 0))
        assertTrue(private.private)
        assertTrue("saved conversation history isn't included here" in private.detail)
        assertTrue("Memory that is currently available may still be used" in private.detail)
    }
}

// [VXG RealForever]
