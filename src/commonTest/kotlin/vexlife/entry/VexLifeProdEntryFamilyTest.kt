package vexlife.entry

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vexlife.home.VexLifeProdHome

class VexLifeProdEntryFamilyTest {
    private val sourceSha = "a".repeat(40)

    @Test
    fun updateAvailableProjectsHumanChoiceAndConvergesWithoutAcquisition() {
        val state = VexLifeProdEntryFamily.UpdateAvailable(sourceSha)
        val display = VexLifeProdEntryFamily.Semantics.displayStateFor(state)
        val act =
            VexLifeProdEntryFamily.Semantics.resolve(state, VexLifeProdEntryFamily.ConvergeDesired)
        val effect = act?.effect as VexLifeProdEntryFamily.ConvergeProduct

        assertEquals(VexLifeProdEntryFamily.ControlConvergeDesired, display.primaryControlRef)
        assertEquals("Update & Restart", display.primaryLabel)
        assertEquals(VexLifeProdEntryFamily.ControlOpenCurrent, display.secondaryControlRef)
        assertEquals(sourceSha, effect.desiredSourceSha)
        assertFalse(effect.allowAcquisition)
    }

    @Test
    fun repairAndUpdateRepairShareConvergenceMeaningButNotDisplayMeaning() {
        val repair = VexLifeProdEntryFamily.RepairRequired(sourceSha)
        val updateRepair = VexLifeProdEntryFamily.UpdateRepairRequired(sourceSha)

        assertEquals(
            "Repair & Restart",
            VexLifeProdEntryFamily.Semantics.displayStateFor(repair).primaryLabel,
        )
        assertEquals(
            "Update & Restart",
            VexLifeProdEntryFamily.Semantics.displayStateFor(updateRepair).primaryLabel,
        )

        for (state in listOf(repair, updateRepair)) {
            val effect =
                VexLifeProdEntryFamily.Semantics.resolve(
                        state,
                        VexLifeProdEntryFamily.ConvergeDesired,
                    )
                    ?.effect as VexLifeProdEntryFamily.ConvergeProduct
            assertFalse(effect.allowAcquisition)
        }
    }

    @Test
    fun HomeFormationIsDistinctFromProductAcquisition() {
        val state = VexLifeProdEntryFamily.HomeFormationRequired(sourceSha)
        val display = VexLifeProdEntryFamily.Semantics.displayStateFor(state)
        val effect =
            VexLifeProdEntryFamily.Semantics.resolve(state, VexLifeProdEntryFamily.FormHome)?.effect

        assertEquals("Create Vex Home", display.primaryLabel)
        assertEquals(VexLifeProdEntryFamily.FormCanonicalHome, effect)
        assertNull(VexLifeProdEntryFamily.Semantics.resolve(state, VexLifeProdEntryFamily.SetUp))
    }

    @Test
    fun setupIsTheOnlyEntryStateThatCanAuthorizeAcquisition() {
        val state = VexLifeProdEntryFamily.SetupRequired(sourceSha)
        val effect =
            VexLifeProdEntryFamily.Semantics.resolve(state, VexLifeProdEntryFamily.SetUp)?.effect
                as VexLifeProdEntryFamily.ConvergeProduct

        assertTrue(effect.allowAcquisition)
        assertNull(
            VexLifeProdEntryFamily.Semantics.resolve(
                VexLifeProdEntryFamily.HomeFormationRequired(sourceSha),
                VexLifeProdEntryFamily.SetUp,
            )
        )
        assertNull(
            VexLifeProdEntryFamily.Semantics.resolve(
                VexLifeProdEntryFamily.UpdateAvailable(sourceSha),
                VexLifeProdEntryFamily.SetUp,
            )
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
            VexLifeProdEntryFamily.Semantics.resolve(state, VexLifeProdEntryFamily.ConvergeDesired)
        )
        assertNull(VexLifeProdEntryFamily.Semantics.displayStateFor(state).primaryControlRef)
        assertEquals("Close", VexLifeProdEntryFamily.Semantics.displayStateFor(state).cancelLabel)
    }

    private fun allStates(): List<VexLifeProdEntryFamily.EntryState> =
        listOf(
            VexLifeProdEntryFamily.Current(sourceSha),
            VexLifeProdEntryFamily.UpdateAvailable(sourceSha),
            VexLifeProdEntryFamily.RepairRequired(sourceSha),
            VexLifeProdEntryFamily.UpdateRepairRequired(sourceSha),
            VexLifeProdEntryFamily.StartRequired(sourceSha, priorServiceSourceSha = null),
            VexLifeProdEntryFamily.HomeFormationRequired(sourceSha),
            VexLifeProdEntryFamily.HomeHeld(
                sourceSha,
                VexLifeProdHome.LocationDisposition.HeldNoncanonicalContent,
                "folder has other files",
            ),
            VexLifeProdEntryFamily.HostHeld(
                desiredSourceSha = sourceSha,
                reasonRef = "CHIP_MODEL_UNSUPPORTED",
                chipModel = "Apple M2",
                totalMemoryBytes = 16L * 1024L * 1024L * 1024L,
                requiredChipModel = "Apple M4 Pro",
                requiredMemoryBytes = 12L * 1024L * 1024L * 1024L,
                homePresent = false,
            ),
            VexLifeProdEntryFamily.SetupRequired(sourceSha),
            VexLifeProdEntryFamily.StorageHeld(
                sourceSha,
                requiredFreeBytes = 20L shl 30,
                freeBytes = 5L shl 30,
            ),
            VexLifeProdEntryFamily.ModelFilesHeld(sourceSha, invalidBytes = 1L shl 30),
            VexLifeProdEntryFamily.HeldUnknown(sourceSha, "receipt/PID ownership is not exact"),
        )

    private val allControls =
        listOf(
            VexLifeProdEntryFamily.OpenCurrent,
            VexLifeProdEntryFamily.ConvergeDesired,
            VexLifeProdEntryFamily.Start,
            VexLifeProdEntryFamily.FormHome,
            VexLifeProdEntryFamily.SetUp,
            VexLifeProdEntryFamily.Cancel,
        )

    @Test
    fun everyStateRefIsDeclaredAndEveryStateProjectsOneValidJourney() {
        val states = allStates()
        assertEquals(
            VexLifeProdEntryFamily.StateRefs,
            states.map { it.stateRef }.toSet(),
        )
        for (state in states) {
            val display = VexLifeProdEntryFamily.Semantics.displayStateFor(state)
            assertEquals(
                VexLifeProdEntryFamily.JourneyStepRefs,
                display.journey.map { it.stepRef },
                state.stateRef,
            )
            assertEquals(
                1,
                display.journey.count {
                    it.statusRef == VexLifeProdEntryFamily.JourneyCurrent ||
                        it.statusRef == VexLifeProdEntryFamily.JourneyHeld
                },
                "exactly one rail step is where the person is: ${state.stateRef}",
            )
        }
    }

    @Test
    fun displayedControlsAreExactlyTheControlsTheFamilyAdmits() {
        for (state in allStates()) {
            val display = VexLifeProdEntryFamily.Semantics.displayStateFor(state)
            val displayed = setOfNotNull(display.primaryControlRef, display.secondaryControlRef)
            val admitted =
                allControls
                    .filter { it != VexLifeProdEntryFamily.Cancel }
                    .filter { VexLifeProdEntryFamily.Semantics.resolve(state, it) != null }
                    .map { it.controlRef }
                    .toSet()
            assertEquals(
                admitted,
                displayed,
                "presentation must neither invent nor hide a lawful Control: ${state.stateRef}",
            )
            assertTrue(
                VexLifeProdEntryFamily.Semantics.resolve(state, VexLifeProdEntryFamily.Cancel) !=
                    null
            )
        }
    }

    @Test
    fun heldStatesAdmitNoEffectAndCloseRatherThanCancel() {
        for (state in
            allStates().filter {
                it.stateRef.contains("HELD") ||
                    it.stateRef == VexLifeProdEntryFamily.StateHeldUnknown
            }) {
            val display = VexLifeProdEntryFamily.Semantics.displayStateFor(state)
            assertNull(display.primaryControlRef, state.stateRef)
            assertNull(display.secondaryControlRef, state.stateRef)
            assertEquals("Close", display.cancelLabel, state.stateRef)
            for (control in allControls.filter { it != VexLifeProdEntryFamily.Cancel }) {
                assertNull(
                    VexLifeProdEntryFamily.Semantics.resolve(state, control),
                    "${state.stateRef} must not admit ${control.controlRef}",
                )
            }
        }
    }

    @Test
    fun startAfterRestartStartsTheRuntimeWithoutAcquisitionAuthority() {
        val state =
            VexLifeProdEntryFamily.StartRequired(
                desiredSourceSha = sourceSha,
                priorServiceSourceSha = "b".repeat(40),
            )
        val display = VexLifeProdEntryFamily.Semantics.displayStateFor(state)
        val effect =
            VexLifeProdEntryFamily.Semantics.resolve(state, VexLifeProdEntryFamily.Start)?.effect
                as VexLifeProdEntryFamily.ConvergeProduct

        assertEquals("Start VexLife", display.primaryLabel)
        assertTrue(display.detail.contains("Nothing will be downloaded"))
        assertTrue(display.detail.contains("newer version"))
        assertFalse(effect.allowAcquisition)
        assertTrue(effect.allowRuntimeStart)
        assertNull(VexLifeProdEntryFamily.Semantics.resolve(state, VexLifeProdEntryFamily.SetUp))
    }

    @Test
    fun repairAndUpdateMayStartTheLocalRuntimeButNeverAcquire() {
        for (state in
            listOf(
                VexLifeProdEntryFamily.UpdateAvailable(sourceSha),
                VexLifeProdEntryFamily.RepairRequired(sourceSha),
                VexLifeProdEntryFamily.UpdateRepairRequired(sourceSha),
            )) {
            val effect =
                VexLifeProdEntryFamily.Semantics.resolve(
                        state,
                        VexLifeProdEntryFamily.ConvergeDesired,
                    )
                    ?.effect as VexLifeProdEntryFamily.ConvergeProduct
            assertFalse(effect.allowAcquisition, state.stateRef)
            assertTrue(effect.allowRuntimeStart, state.stateRef)
        }
    }

    @Test
    fun acquisitionWithoutRuntimeStartIsNotALawfulEffect() {
        val rejected = runCatching {
            VexLifeProdEntryFamily.ConvergeProduct(
                desiredSourceSha = sourceSha,
                allowAcquisition = true,
                allowRuntimeStart = false,
            )
        }
        assertTrue(rejected.isFailure)
    }

    @Test
    fun onlySetupRequiredAuthorizesAcquisitionAcrossEveryState() {
        for (state in allStates()) {
            val acquiring = allControls.mapNotNull {
                (VexLifeProdEntryFamily.Semantics.resolve(state, it)?.effect
                        as? VexLifeProdEntryFamily.ConvergeProduct)
                    ?.takeIf { effect -> effect.allowAcquisition }
            }
            assertEquals(
                state is VexLifeProdEntryFamily.SetupRequired,
                acquiring.isNotEmpty(),
                state.stateRef,
            )
        }
    }

    @Test
    fun homeChoiceIsOfferedWhereChoosingAnotherLocationCanHelp() {
        val offered =
            allStates()
                .filter {
                    VexLifeProdEntryFamily.Semantics.displayStateFor(it).homeChoiceLabel != null
                }
                .map { it.stateRef }
                .toSet()
        assertEquals(
            setOf(
                VexLifeProdEntryFamily.StateHomeFormationRequired,
                VexLifeProdEntryFamily.StateHomeHeld,
                VexLifeProdEntryFamily.StateStorageHeld,
                VexLifeProdEntryFamily.StateHeldUnknown,
            ),
            offered,
        )
    }

    @Test
    fun hostHoldNamesTheObservedMacAndTheCertifiedRequirement() {
        val state = allStates().filterIsInstance<VexLifeProdEntryFamily.HostHeld>().single()
        val detail = VexLifeProdEntryFamily.Semantics.displayStateFor(state).detail
        assertTrue(detail.contains("Apple M2"))
        assertTrue(detail.contains("16.0 GiB"))
        assertTrue(detail.contains("Apple M4 Pro"))
        assertTrue(detail.contains("12.0 GiB"))
        assertTrue(detail.contains("Nothing was created or downloaded"))
    }

    @Test
    fun storageHoldStatesRequiredAndAvailableSpace() {
        val detail =
            VexLifeProdEntryFamily.Semantics.displayStateFor(
                    VexLifeProdEntryFamily.StorageHeld(
                        sourceSha,
                        requiredFreeBytes = 17L shl 30,
                        freeBytes = 9L shl 30,
                    )
                )
                .detail
        assertTrue(detail.contains("17.0 GiB"))
        assertTrue(detail.contains("9.0 GiB"))
    }

    @Test
    fun everyHeldHomeDispositionHasItsOwnExplanation() {
        val details =
            VexLifeProdHome.LocationDisposition.entries
                .filter { it.held }
                .map {
                    VexLifeProdEntryFamily.Semantics.displayStateFor(
                            VexLifeProdEntryFamily.HomeHeld(sourceSha, it, "observed")
                        )
                        .detail
                }
        assertEquals(details.size, details.toSet().size)
        assertTrue(
            runCatching {
                VexLifeProdEntryFamily.HomeHeld(
                    sourceSha,
                    VexLifeProdHome.LocationDisposition.EmptyDirectory,
                    "not held",
                )
            }
                .isFailure
        )
    }

    @Test
    fun gibFormatsBinaryUnitsWithOneDecimal() {
        assertEquals("0.0", VexLifeProdEntryFamily.gib(0))
        assertEquals("1.0", VexLifeProdEntryFamily.gib(1L shl 30))
        assertEquals("14.1", VexLifeProdEntryFamily.gib(15_153_242_506L))
        assertEquals("0.5", VexLifeProdEntryFamily.gib(1L shl 29))
    }
}

// [VXG RealForever]
