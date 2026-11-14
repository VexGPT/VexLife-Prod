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
        assertEquals("setup.button.update-restart", display.primaryLabel?.key)
        assertEquals(VexLifeProdEntryFamily.ControlOpenCurrent, display.secondaryControlRef)
        assertEquals(sourceSha, effect.desiredSourceSha)
        assertFalse(effect.allowAcquisition)
    }

    @Test
    fun repairAndUpdateRepairShareConvergenceMeaningButNotDisplayMeaning() {
        val repair = VexLifeProdEntryFamily.RepairRequired(sourceSha)
        val updateRepair = VexLifeProdEntryFamily.UpdateRepairRequired(sourceSha)

        assertEquals(
            "setup.button.repair-restart",
            VexLifeProdEntryFamily.Semantics.displayStateFor(repair).primaryLabel?.key,
        )
        assertEquals(
            "setup.button.update-restart",
            VexLifeProdEntryFamily.Semantics.displayStateFor(updateRepair).primaryLabel?.key,
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

        assertEquals("setup.button.create-home", display.primaryLabel?.key)
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
        assertEquals(
            "setup.button.close",
            VexLifeProdEntryFamily.Semantics.displayStateFor(state).cancelLabel.key,
        )
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
            assertEquals("setup.button.close", display.cancelLabel.key, state.stateRef)
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

        assertEquals("setup.button.start-vexlife", display.primaryLabel?.key)
        assertEquals("setup.description.start-required-newer-version", display.detail.key)
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
        val observed = detail.parts.getValue("observed")
        val requirement = detail.parts.getValue("requirement")
        assertEquals("setup.description.host-reason-chip", detail.parts.getValue("reason").key)
        assertEquals("Apple M2", observed.args["chip"])
        assertEquals(16L shl 30, observed.parts.getValue("memory").gib["memory"])
        assertEquals("Apple M4 Pro", requirement.args["chip"])
        assertEquals(12L shl 30, requirement.gib["memory"])
        assertEquals("setup.description.host-nothing-created", detail.parts.getValue("outcome").key)
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
        assertEquals(mapOf("required" to (17L shl 30), "free" to (9L shl 30)), detail.gib)
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
                        .parts
                        .getValue("reason")
                        .key
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
    fun displayStateNamesTextByKeyAndNeverByWords() {
        fun keys(text: vexlife.text.VexLifeText): List<String> =
            listOf(text.key) + text.parts.values.flatMap(::keys)
        for (state in allStates()) {
            val display = VexLifeProdEntryFamily.Semantics.displayStateFor(state)
            val all =
                listOfNotNull(
                        display.title,
                        display.detail,
                        display.primaryLabel,
                        display.secondaryLabel,
                        display.cancelLabel,
                        display.homeChoiceLabel,
                        display.recheckLabel,
                    )
                    .flatMap(::keys) + display.journey.map { it.label.key }
            assertTrue(all.all { it.startsWith("setup.") }, state.stateRef)
        }
    }
}

// [VXG RealForever]
