package vexlife.context

import kotlin.test.Test
import kotlin.test.assertEquals

class VexLifeContextProfileFamilyTest {
    private val foundation =
        VexLifeContextProfile.ProductFoundationIdentity(
            version = "2026.11",
            bodySha256 = null,
        )

    @Test
    fun productDefaultDisplayKeepsBadgeAndSummaryKeyed() {
        val display =
            VexLifeContextProfileFamily.displayStateFor(
                VexLifeContextProfileFamily.ProductDefault(
                    foundation,
                ),
            )

        assertEquals(
            "workspace.label.context-profile-product-default",
            display.label.key,
        )
        assertEquals(
            "workspace.description.context-profile-product-foundation-version",
            display.detail.key,
        )
        assertEquals(
            "2026.11",
            display.detail.args["version"],
        )
    }

    @Test
    fun savedProfileNameRemainsLiteralDataInsideStableDisplayIdentity() {
        val state =
            VexLifeContextProfileFamily.Saved(
                VexLifeContextProfile.Effective(
                    profileRef = "profile.vexlife.context.victor",
                    revision = 7,
                    definition =
                        VexLifeContextProfile.Definition(
                            name = "Victor's own words",
                        ),
                    basedOnProductFoundation = foundation,
                    fingerprint = "a".repeat(64),
                    formedAt = null,
                    productDefault = false,
                    productFoundationChangedSinceSave = true,
                ),
            )
        val display =
            VexLifeContextProfileFamily.displayStateFor(
                state,
            )

        assertEquals(
            "workspace.label.context-profile-saved",
            display.label.key,
        )
        assertEquals(
            "Victor's own words",
            display.label.args["name"],
        )
        assertEquals(
            "workspace.description.context-profile-saved-foundation-changed",
            display.detail.key,
        )
        assertEquals(
            "7",
            display.detail.args["revision"],
        )
    }
}

// [VXG RealForever]
