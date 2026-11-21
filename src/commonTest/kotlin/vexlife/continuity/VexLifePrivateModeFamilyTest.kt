package vexlife.continuity

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

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
    fun displayKeepsControlAndBannerCopyOnTheirOwnKeys() {
        val kept =
            VexLifePrivateModeFamily.displayStateFor(
                VexLifePrivateModeFamily.Kept,
            )
        assertEquals(
            "workspace.status.private-kept",
            kept.label.key,
        )
        assertEquals(
            "workspace.description.private-kept",
            kept.detail.key,
        )
        assertEquals(
            "workspace.button.private-begin",
            kept.primaryLabel.key,
        )
        assertEquals(
            VexLifePrivateModeFamily.ControlBegin,
            kept.primaryControlRef,
        )
        assertNull(
            kept.banner,
        )

        val private =
            VexLifePrivateModeFamily.displayStateFor(
                VexLifePrivateModeFamily.Private(
                    0,
                    0,
                ),
            )
        assertEquals(
            "workspace.status.private-active",
            private.label.key,
        )
        assertEquals(
            "workspace.description.private-active",
            private.detail.key,
        )
        assertEquals(
            "workspace.button.private-end",
            private.primaryLabel.key,
        )
        assertEquals(
            "workspace.description.private-banner",
            private.banner?.key,
        )
        assertEquals(
            true,
            private.private,
        )
    }
}

// [VXG RealForever]
