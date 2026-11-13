package vexlife.entry

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VexLifeProdEntryFamilyTest {
    private val sourceSha =
        "a".repeat(
            40,
        )

    @Test
    fun updateAvailableProjectsHumanChoiceAndConvergesWithoutAcquisition() {
        val state =
            VexLifeProdEntryFamily.UpdateAvailable(
                sourceSha,
            )
        val display =
            VexLifeProdEntryFamily.Semantics.displayStateFor(
                state,
            )
        val act =
            VexLifeProdEntryFamily.Semantics.resolve(
                state,
                VexLifeProdEntryFamily.ConvergeDesired,
            )
        val effect =
            act
                ?.effect as VexLifeProdEntryFamily.ConvergeProduct

        assertEquals(
            VexLifeProdEntryFamily.ControlConvergeDesired,
            display.primaryControlRef,
        )
        assertEquals(
            "Update & Restart",
            display.primaryLabel,
        )
        assertEquals(
            VexLifeProdEntryFamily.ControlOpenCurrent,
            display.secondaryControlRef,
        )
        assertEquals(
            sourceSha,
            effect.desiredSourceSha,
        )
        assertFalse(
            effect.allowAcquisition,
        )
    }

    @Test
    fun repairAndUpdateRepairShareConvergenceMeaningButNotDisplayMeaning() {
        val repair =
            VexLifeProdEntryFamily.RepairRequired(
                sourceSha,
            )
        val updateRepair =
            VexLifeProdEntryFamily.UpdateRepairRequired(
                sourceSha,
            )

        assertEquals(
            "Repair & Restart",
            VexLifeProdEntryFamily.Semantics
                .displayStateFor(
                    repair,
                )
                .primaryLabel,
        )
        assertEquals(
            "Update & Restart",
            VexLifeProdEntryFamily.Semantics
                .displayStateFor(
                    updateRepair,
                )
                .primaryLabel,
        )

        for (
            state in
            listOf(
                repair,
                updateRepair,
            )
        ) {
            val effect =
                VexLifeProdEntryFamily.Semantics
                    .resolve(
                        state,
                        VexLifeProdEntryFamily.ConvergeDesired,
                    )
                    ?.effect as VexLifeProdEntryFamily.ConvergeProduct
            assertFalse(
                effect.allowAcquisition,
            )
        }
    }

    @Test
    fun HomeFormationIsDistinctFromProductAcquisition() {
        val state =
            VexLifeProdEntryFamily.HomeFormationRequired(
                sourceSha,
            )
        val display =
            VexLifeProdEntryFamily.Semantics.displayStateFor(
                state,
            )
        val effect =
            VexLifeProdEntryFamily.Semantics
                .resolve(
                    state,
                    VexLifeProdEntryFamily.FormHome,
                )
                ?.effect

        assertEquals(
            "Create Vex Home",
            display.primaryLabel,
        )
        assertEquals(
            VexLifeProdEntryFamily.FormCanonicalHome,
            effect,
        )
        assertNull(
            VexLifeProdEntryFamily.Semantics.resolve(
                state,
                VexLifeProdEntryFamily.SetUp,
            ),
        )
    }

    @Test
    fun setupIsTheOnlyEntryStateThatCanAuthorizeAcquisition() {
        val state =
            VexLifeProdEntryFamily.SetupRequired(
                sourceSha,
            )
        val effect =
            VexLifeProdEntryFamily.Semantics
                .resolve(
                    state,
                    VexLifeProdEntryFamily.SetUp,
                )
                ?.effect as VexLifeProdEntryFamily.ConvergeProduct

        assertTrue(
            effect.allowAcquisition,
        )
        assertNull(
            VexLifeProdEntryFamily.Semantics.resolve(
                VexLifeProdEntryFamily.HomeFormationRequired(
                    sourceSha,
                ),
                VexLifeProdEntryFamily.SetUp,
            ),
        )
        assertNull(
            VexLifeProdEntryFamily.Semantics.resolve(
                VexLifeProdEntryFamily.UpdateAvailable(
                    sourceSha,
                ),
                VexLifeProdEntryFamily.SetUp,
            ),
        )
    }

    @Test
    fun heldOwnershipAllowsNoReplacementControl() {
        val state =
            VexLifeProdEntryFamily.HeldUnknown(
                desiredSourceSha = sourceSha,
                detail = "receipt/PID ownership is not exact",
            )

        assertNull(
            VexLifeProdEntryFamily.Semantics.resolve(
                state,
                VexLifeProdEntryFamily.ConvergeDesired,
            ),
        )
        assertNull(
            VexLifeProdEntryFamily.Semantics
                .displayStateFor(
                    state,
                )
                .primaryControlRef,
        )
        assertEquals(
            "Close",
            VexLifeProdEntryFamily.Semantics
                .displayStateFor(
                    state,
                )
                .cancelLabel,
        )
    }
}

// [VXG RealForever]
