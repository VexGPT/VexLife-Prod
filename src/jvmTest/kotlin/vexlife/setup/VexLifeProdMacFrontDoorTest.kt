package vexlife.setup

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vexlife.browser.VexLifeProdBrowserContract
import vexlife.entry.VexLifeProdEntryFamily
import vexlife.home.VexLifeProdHome
import vexlife.home.VexLifeProdHomeFiles
import vexlife.platform.macos.MacUserLaunchdProcess
import vexlife.service.VexLifeProdMacServiceLifetime
import vexlife.service.VexLifeProdServiceContract
import vexlife.text.VexLifeStrings
import vexlife.text.VexLifeText

class VexLifeProdMacFrontDoorTest {
    private fun en(text: VexLifeText?): String = VexLifeStrings.resolve(requireNotNull(text), "en")

    private val selectedHome = Path.of("/tmp/.vexlife")

    private val desired = "a".repeat(40)

    private val prior = "b".repeat(40)

    private fun currentState(): VexLifeProdServiceContract.CurrentStateProjection =
        VexLifeProdServiceContract.CurrentStateProjection(
            schemaVersion = VexLifeProdServiceContract.CurrentStateProjectionSchema,
            observedAt = "2026-11-13T00:00:00Z",
            homePath = "/tmp/.vexlife",
            homeDisposition = VexLifeProdHome.LocationDisposition.Canonical.wireValue,
            homeRef = "home.vexlife.front-door-test",
            deviceRef = "device.vexlife.front-door-test",
            companionLineageRef = "companion.vexlife.front-door-test",
            platform = "macOS",
            architecture = "arm64",
            chipModel = "Apple M4 Pro",
            hostEligibilityState = "ELIGIBLE",
            hostEligibilityReason = null,
            totalMemoryBytes = 48L * 1024L * 1024L * 1024L,
            modelMinimumMemoryBytes = 16L * 1024L * 1024L * 1024L,
            freeStorageBytes = 3L * 1024L * 1024L * 1024L,
            modelSelectionState = "BOUND_HERETIC_PRODUCT_MODEL",
            activeModelRef = "model.heretic.front-door-test",
            activeModelProfileRef = "profile.heretic.front-door-test",
            runtimeState = "BOUND_PRODUCT_MODEL",
            runtimeCurrentness = "CONFIGURATION_ONLY_NOT_REOBSERVED",
            runtimeReceiptPath = "/tmp/.vexlife/runtime/heretic.json",
            modelArtifactState = "COMPLETE_BY_SIZE_NOT_HASHED",
            modelArtifactTotalBytes = 100L,
            modelArtifactPresentBySizeBytes = 100L,
            modelArtifactMissingBytes = 0L,
            modelArtifactInvalidSizeBytes = 0L,
            effectsPerformed = false,
        )

    private fun absentHomeCurrentState(): VexLifeProdServiceContract.CurrentStateProjection =
        VexLifeProdServiceContract.CurrentStateProjection(
            schemaVersion = VexLifeProdServiceContract.CurrentStateProjectionSchema,
            observedAt = "2026-11-13T00:00:00Z",
            homePath = "/tmp/.vexlife",
            homeDisposition = "ABSENT_CANONICAL_VEXLIFE_HOME",
            homeRef = null,
            deviceRef = null,
            companionLineageRef = null,
            platform = "darwin",
            architecture = "arm64",
            chipModel = "Apple M4 Pro",
            hostEligibilityState = "ELIGIBLE",
            hostEligibilityReason = null,
            totalMemoryBytes = 48L * 1024L * 1024L * 1024L,
            modelMinimumMemoryBytes = null,
            freeStorageBytes = 100L * 1024L * 1024L * 1024L,
            modelSelectionState = "UNCONFIGURED",
            activeModelRef = null,
            activeModelProfileRef = null,
            runtimeState = "NOT_SELECTED",
            runtimeCurrentness = "NOT_APPLICABLE",
            runtimeReceiptPath = null,
            modelArtifactState = "NOT_SELECTED",
            modelArtifactTotalBytes = null,
            modelArtifactPresentBySizeBytes = null,
            modelArtifactMissingBytes = null,
            modelArtifactInvalidSizeBytes = null,
            effectsPerformed = false,
        )

    private fun receipt(
        sourceSha: String,
        pid: Long = 4242L,
    ): VexLifeProdMacServiceLifetime.Receipt =
        VexLifeProdMacServiceLifetime.Receipt(
            schemaVersion = VexLifeProdMacServiceLifetime.ReceiptSchema,
            sourceSha = sourceSha,
            imagePayloadSha256 = "c".repeat(64),
            imagePath = "/tmp/.vexlife/runtime/product-service/images/" + sourceSha,
            serviceOrigin = VexLifeProdServiceContract.Origin,
            launchd =
                MacUserLaunchdProcess.Receipt(
                    schemaVersion = MacUserLaunchdProcess.Schema,
                    substrateRef = MacUserLaunchdProcess.SubstrateRef,
                    label = VexLifeProdMacServiceLifetime.Label,
                    domainTarget = "gui/501",
                    plistPath =
                        "/tmp/.vexlife/runtime/product-service/launchd/com.vexlife.prod.service.plist",
                    programPath =
                        "/tmp/.vexlife/runtime/product-service/images/" +
                            sourceSha +
                            "/bin/vexlife-prod-service",
                    programArguments =
                        listOf("--source-sha", sourceSha, "--home", selectedHome.toString()),
                    workingDirectory = "/tmp/.vexlife/runtime/product-service/images/" + sourceSha,
                    stdoutPath = "/tmp/.vexlife/runtime/product-service/logs/service.out.log",
                    stderrPath = "/tmp/.vexlife/runtime/product-service/logs/service.err.log",
                    environment = emptyMap(),
                    keepAlive = false,
                    pid = pid,
                ),
        )

    private fun currentness(
        sourceSha: String,
        pid: Long = 4242L,
    ): VexLifeProdServiceContract.SourceCurrentness =
        VexLifeProdServiceContract.SourceCurrentness(
            schemaVersion = VexLifeProdServiceContract.SourceCurrentnessSchema,
            serviceRef = VexLifeProdServiceContract.ServiceRef,
            productRef = VexLifeProdServiceContract.ProductRef,
            serviceState = "SOURCE_BOUND_LISTENING",
            sourceSha = sourceSha,
            processPid = pid,
            frameHostOrigin = VexLifeProdBrowserContract.FrameHostOrigin,
            runtimeCurrentness = "NOT_REOBSERVED_BY_SERVICE",
            startupQualifiedRuntimeReceiptRef = "receipt.vexlife-prod.front-door-test",
            currentConversationHeadSha256 = null,
            effectsPerformed = false,
        )

    private val testPlan =
        VexLifeProdHome.plan(
            personRef = "person.local-user",
            familyRef = "vex-family.local-user",
            deviceName = "Front Door Test Mac",
            platform = "darwin",
            architecture = "arm64",
            ids =
                VexLifeProdHome.InstallationIds(
                    deviceRef = "device.vexlife.00000000-0000-0000-0000-000000000001",
                    companionLineageRef =
                        "companion-lineage.vexlife.00000000-0000-0000-0000-000000000002",
                ),
            createdAt = "2026-11-13T04:00:00-08:00",
        )

    private val testManifest = testPlan.manifest

    private val testInstallation = testPlan.installation

    private fun existingLocation(): VexLifeProdHomeFiles.Location =
        VexLifeProdHomeFiles.Location.Existing(
            VexLifeProdHomeFiles.Result(
                state = VexLifeProdHome.LocationDisposition.Canonical.wireValue,
                productHome = selectedHome.toString(),
                manifest = testManifest,
                installation = testInstallation,
                effectsPerformed = false,
            )
        )

    private fun absentLocation(): VexLifeProdHomeFiles.Location =
        VexLifeProdHomeFiles.Location.Formable(
            productHome = selectedHome.toString(),
            disposition = VexLifeProdHome.LocationDisposition.Absent,
            nearestExistingAncestor = "/tmp",
            missingAncestorCount = 0,
        )

    private fun hooks(
        existing: VexLifeProdMacServiceLifetime.ExistingObservation,
        runtimeEnvironmentPresent: Boolean = true,
        current: VexLifeProdServiceContract.CurrentStateProjection = currentState(),
    ): VexLifeProdMacFrontDoor.Hooks =
        VexLifeProdMacFrontDoor.Hooks(
            observeCurrentState = { productHome ->
                assertEquals(selectedHome, productHome)
                current
            },
            observeHomeLocation = {
                existingLocation()
            },
            observeExistingService = {
                existing
            },
            observeRuntimeEnvironmentPresent = {
                runtimeEnvironmentPresent
            },
            userHome = {
                Path.of("/Users/vex")
            },
        )

    @Test
    fun absentHomeMapsToHomeFormationBeforeServiceLifetimeObservation() {
        var serviceObserved = false
        val hooks =
            VexLifeProdMacFrontDoor.Hooks(
                observeCurrentState = { productHome ->
                    assertEquals(selectedHome, productHome)
                    absentHomeCurrentState()
                },
                observeHomeLocation = {
                    absentLocation()
                },
                observeExistingService = {
                    serviceObserved = true
                    error("absent Home must not descend into service lifetime observation")
                },
            )

        val projection =
            VexLifeProdMacFrontDoor.observeWithHooks(
                desiredSourceSha = desired,
                productHome = selectedHome,
                hooks = hooks,
            )
        val resolved =
            VexLifeProdMacFrontDoor.resolveTypedControlWithHooks(
                desiredSourceSha = desired,
                productHome = selectedHome,
                controlRef = VexLifeProdEntryFamily.ControlFormHome,
                hooks = hooks,
            )

        assertEquals(VexLifeProdEntryFamily.StateHomeFormationRequired, projection.semanticStateRef)
        assertEquals("Create Vex Home", en(projection.displayState.primaryLabel))
        assertEquals(VexLifeProdEntryFamily.FormCanonicalHome, resolved.effect)
        assertFalse(serviceObserved)
    }

    @Test
    fun exactRunningSourceMapsToCommonCurrentStateAndOpenDisplay() {
        val value =
            VexLifeProdMacFrontDoor.observeWithHooks(
                desiredSourceSha = desired,
                productHome = selectedHome,
                hooks =
                    hooks(
                        VexLifeProdMacServiceLifetime.ExistingObservation.RunningOwned(
                            receipt = receipt(desired),
                            sourceCurrentness = currentness(desired),
                        )
                    ),
            )

        assertEquals(VexLifeProdEntryFamily.StateCurrent, value.semanticStateRef)
        assertEquals(
            VexLifeProdEntryFamily.ControlOpenCurrent,
            value.displayState.primaryControlRef,
        )
        assertEquals("Open VexLife", en(value.displayState.primaryLabel))
        assertFalse(value.effectsPerformed)
    }

    @Test
    fun exactOwnedPriorSourceMapsToSharedUpdateStateAndNoAcquisitionEffect() {
        val observed =
            VexLifeProdMacServiceLifetime.ExistingObservation.RunningOwned(
                receipt = receipt(prior),
                sourceCurrentness = currentness(prior),
            )
        val projection =
            VexLifeProdMacFrontDoor.observeWithHooks(
                desiredSourceSha = desired,
                productHome = selectedHome,
                hooks = hooks(observed),
            )
        val resolved =
            VexLifeProdMacFrontDoor.resolveTypedControlWithHooks(
                desiredSourceSha = desired,
                productHome = selectedHome,
                controlRef = VexLifeProdEntryFamily.ControlConvergeDesired,
                hooks = hooks(observed),
            )

        assertEquals(VexLifeProdEntryFamily.StateUpdateAvailable, projection.semanticStateRef)
        assertEquals(prior, projection.currentServiceSourceSha)
        assertEquals("Update & Restart", en(projection.displayState.primaryLabel))
        val converge = requireNotNull(resolved.effect as? VexLifeProdEntryFamily.ConvergeProduct)
        assertFalse(converge.allowAcquisition)
    }

    @Test
    fun ownedUnhealthyServiceMapsToRepairMeaningWithoutDuplicatingPlatformDecisionLogic() {
        val currentRepair =
            VexLifeProdMacServiceLifetime.ExistingObservation.OwnedNotReady(
                receipt = receipt(desired),
                launchdObservation = MacUserLaunchdProcess.Observation.LoadedStopped,
            )
        val priorRepair =
            VexLifeProdMacServiceLifetime.ExistingObservation.OwnedNotReady(
                receipt = receipt(prior),
                launchdObservation = MacUserLaunchdProcess.Observation.LoadedStopped,
            )

        assertEquals(
            VexLifeProdEntryFamily.StateRepairRequired,
            VexLifeProdMacFrontDoor.observeWithHooks(
                    desiredSourceSha = desired,
                    productHome = selectedHome,
                    hooks = hooks(currentRepair),
                )
                .semanticStateRef,
        )
        assertEquals(
            VexLifeProdEntryFamily.StateUpdateRepairRequired,
            VexLifeProdMacFrontDoor.observeWithHooks(
                    desiredSourceSha = desired,
                    productHome = selectedHome,
                    hooks = hooks(priorRepair),
                )
                .semanticStateRef,
        )
    }

    @Test
    fun existingHomeWithoutServiceShowsResourcePlanBeforeAcquisition() {
        val absentService = VexLifeProdMacServiceLifetime.ExistingObservation.Absent
        val projection =
            VexLifeProdMacFrontDoor.observeWithHooks(
                desiredSourceSha = desired,
                productHome = selectedHome,
                hooks = hooks(absentService, runtimeEnvironmentPresent = false),
            )
        val resolved =
            VexLifeProdMacFrontDoor.resolveTypedControlWithHooks(
                desiredSourceSha = desired,
                productHome = selectedHome,
                controlRef = VexLifeProdEntryFamily.ControlSetUp,
                hooks = hooks(absentService, runtimeEnvironmentPresent = false),
            )

        assertEquals(VexLifeProdEntryFamily.StateSetupRequired, projection.semanticStateRef)
        val setupPlan = requireNotNull(projection.setupPlan)
        assertTrue(setupPlan.modelArtifactTotalBytes > 0)
        assertTrue(setupPlan.modelMinimumMemoryBytes > 0)
        assertEquals(48L * 1024L * 1024L * 1024L, setupPlan.currentTotalMemoryBytes)
        assertEquals(3L * 1024L * 1024L * 1024L, setupPlan.currentFreeStorageBytes)
        assertFalse(setupPlan.effectsPerformed)
        val converge = requireNotNull(resolved.effect as? VexLifeProdEntryFamily.ConvergeProduct)
        assertTrue(converge.allowAcquisition)
    }

    @Test
    fun unknownOwnershipProjectsHeldAndRejectsConvergeControl() {
        val unknown =
            VexLifeProdMacServiceLifetime.ExistingObservation.HeldUnknown(
                "receipt/PID ownership is not exact"
            )
        val projection =
            VexLifeProdMacFrontDoor.observeWithHooks(
                desiredSourceSha = desired,
                productHome = selectedHome,
                hooks = hooks(unknown),
            )

        assertEquals(VexLifeProdEntryFamily.StateHeldUnknown, projection.semanticStateRef)
        assertEquals("receipt/PID ownership is not exact", projection.serviceDetail)
        assertNull(projection.displayState.primaryControlRef)
    }

    @Test
    fun HomeActivationDelegatesSetupEffectAndRequestsEntryRefresh() {
        var homeCalls = 0
        var setupCalls = 0

        val result =
            VexLifeProdMacFrontDoorProcess.activateWithHooks(
                productHome = selectedHome,
                controlRef = VexLifeProdEntryFamily.ControlFormHome,
                desiredSourceSha = desired,
                hooks =
                    VexLifeProdMacFrontDoorProcess.ActivationHooks(
                        resolveControl = { sourceSha, controlRef, productHome ->
                            assertEquals(selectedHome, productHome)
                            assertEquals(desired, sourceSha)
                            VexLifeProdMacFrontDoor.ResolvedControl(
                                semanticStateRef =
                                    VexLifeProdEntryFamily.StateHomeFormationRequired,
                                controlRef = controlRef,
                                effect = VexLifeProdEntryFamily.FormCanonicalHome,
                            )
                        },
                        formHome = { sourceSha, productHome ->
                            assertEquals(selectedHome, productHome)
                            assertEquals(desired, sourceSha)
                            homeCalls += 1
                        },
                        convergeProduct = { _, _, _, _ ->
                            setupCalls += 1
                        },
                        observeSemanticStateRef = { _, productHome ->
                            assertEquals(selectedHome, productHome)
                            VexLifeProdEntryFamily.StateSetupRequired
                        },
                    ),
            )

        assertEquals(1, homeCalls)
        assertEquals(0, setupCalls)
        assertEquals(
            VexLifeProdMacFrontDoorProcess.ManifestationRefreshEntry,
            result.manifestationRef,
        )
        assertEquals(VexLifeProdEntryFamily.StateSetupRequired, result.semanticStateRef)
    }

    @Test
    fun setupActivationCarriesAcquisitionAuthorityAndOpensOnlyAfterCurrentReobservation() {
        var observedAllowAcquisition: Boolean? = null

        val result =
            VexLifeProdMacFrontDoorProcess.activateWithHooks(
                productHome = selectedHome,
                controlRef = VexLifeProdEntryFamily.ControlSetUp,
                desiredSourceSha = desired,
                hooks =
                    VexLifeProdMacFrontDoorProcess.ActivationHooks(
                        resolveControl = { _, controlRef, productHome ->
                            assertEquals(selectedHome, productHome)
                            VexLifeProdMacFrontDoor.ResolvedControl(
                                semanticStateRef = VexLifeProdEntryFamily.StateSetupRequired,
                                controlRef = controlRef,
                                effect =
                                    VexLifeProdEntryFamily.ConvergeProduct(
                                        desiredSourceSha = desired,
                                        allowAcquisition = true,
                                        allowRuntimeStart = true,
                                    ),
                            )
                        },
                        formHome = { _, _ ->
                            error("setup activation must not form Home")
                        },
                        convergeProduct = { sourceSha, allowAcquisition, _, productHome ->
                            assertEquals(selectedHome, productHome)
                            assertEquals(desired, sourceSha)
                            observedAllowAcquisition = allowAcquisition
                        },
                        observeSemanticStateRef = { _, productHome ->
                            assertEquals(selectedHome, productHome)
                            VexLifeProdEntryFamily.StateCurrent
                        },
                    ),
            )

        assertEquals(true, observedAllowAcquisition)
        assertEquals(
            VexLifeProdMacFrontDoorProcess.ManifestationOpenProduct,
            result.manifestationRef,
        )
        assertEquals(VexLifeProdEntryFamily.StateCurrent, result.semanticStateRef)
    }

    @Test
    fun openCurrentReturnsPresentationManifestationWithoutSetupRealization() {
        val result =
            VexLifeProdMacFrontDoorProcess.activateWithHooks(
                productHome = selectedHome,
                controlRef = VexLifeProdEntryFamily.ControlOpenCurrent,
                desiredSourceSha = desired,
                hooks =
                    VexLifeProdMacFrontDoorProcess.ActivationHooks(
                        resolveControl = { _, controlRef, productHome ->
                            assertEquals(selectedHome, productHome)
                            VexLifeProdMacFrontDoor.ResolvedControl(
                                semanticStateRef = VexLifeProdEntryFamily.StateCurrent,
                                controlRef = controlRef,
                                effect = VexLifeProdEntryFamily.OpenPresentation,
                            )
                        },
                        formHome = { _, _ ->
                            error("open-current activation must not form Home")
                        },
                        convergeProduct = { _, _, _, _ ->
                            error("open-current activation must not run setup")
                        },
                        observeSemanticStateRef = { _, _ ->
                            error(
                                "open-current activation must not re-observe after a no-effect presentation request"
                            )
                        },
                    ),
            )

        assertEquals(
            VexLifeProdMacFrontDoorProcess.ManifestationOpenProduct,
            result.manifestationRef,
        )
        assertEquals(VexLifeProdEntryFamily.StateCurrent, result.semanticStateRef)
    }

    @Test
    fun nativeWindowConsumesManifestationWithoutSemanticEffectRouting() {
        val source =
            Files.readString(
                Path.of(
                    System.getProperty("user.dir"),
                    "packaging/private-service-image/setup/vexlife-prod-setup-window.applescript",
                )
            )

        for (required in
            listOf(
                "--activate-front-door-control",
                "--inspect-front-door",
                "objectForKey:\"card\"",
                "setStandardOutput:outHandle",
                "setStandardError:errHandle",
                "chromeText(\"setup.button.cancel-safely\")",
                "property progressPrefix : \"[VexLife] \"",
                "NSOpenPanel",
                "showProcessFailure(my chromeText(\"setup.heading.could-not-read-result\"), activated, true)",
                "inspected, false)",
                // Language is the first step, and every inspection and activation is told it.
                "\"--setup-language\"",
                "\"--choose-language\"",
                "\"--language\", selectedLanguage",
                "NSPopUpButton",
                "nextAction is \"CHOOSE_LANGUAGE\"",
            )) {
            assertTrue(source.contains(required), "native window must keep: $required")
        }
        for (forbidden in
            listOf(
                "effectRef",
                "FORM_CANONICAL_HOME",
                "CONVERGE_PRODUCT",
                "runHomeFormation",
                "runSetup",
                "--resolve-front-door-control",
                // Card copy, State refs and DisplayState fields are Product-owned; the window only
                // renders the card it is given.
                "primaryControlRef",
                "displayState",
                "HOME_FORMATION_REQUIRED",
                "SETUP_REQUIRED",
                "Create Vex Home",
                "Set Up VexLife",
                "Start VexLife",
                "Repair & Restart",
                "Update & Restart",
                // The window's own words come from the keyed string source, not from here.
                "Choose your Vex Home",
                "Choose Folder…",
                "Use This Folder",
                "Setup paused.",
                "Working on it.",
                "Technical detail",
                "Check Again",
                // One shared stdout/stderr pipe read only after exit can deadlock on a long setup
                // and
                // lets JVM diagnostics corrupt the JSON channel.
                "setStandardError:outputPipe",
                // An unreadable activation result cannot prove that no effect ran.
                "Nothing was changed by this window.",
            )) {
            assertFalse(
                source.contains(forbidden),
                "native window must not own semantic Effect routing: $forbidden",
            )
        }
    }

    @Test
    fun HomeActivationRefreshesWhateverStateRealityReobserves() {
        val result =
            VexLifeProdMacFrontDoorProcess.activateWithHooks(
                productHome = selectedHome,
                controlRef = VexLifeProdEntryFamily.ControlFormHome,
                desiredSourceSha = desired,
                hooks =
                    VexLifeProdMacFrontDoorProcess.ActivationHooks(
                        resolveControl = { _, controlRef, productHome ->
                            assertEquals(selectedHome, productHome)
                            VexLifeProdMacFrontDoor.ResolvedControl(
                                semanticStateRef =
                                    VexLifeProdEntryFamily.StateHomeFormationRequired,
                                controlRef = controlRef,
                                effect = VexLifeProdEntryFamily.FormCanonicalHome,
                            )
                        },
                        formHome = { _, productHome ->
                            assertEquals(selectedHome, productHome)
                        },
                        convergeProduct = { _, _, _, _ ->
                            error("Home activation must not converge Product")
                        },
                        observeSemanticStateRef = { _, productHome ->
                            assertEquals(selectedHome, productHome)
                            VexLifeProdEntryFamily.StateHeldUnknown
                        },
                    ),
            )

        assertEquals(VexLifeProdEntryFamily.StateHeldUnknown, result.semanticStateRef)
        assertEquals(
            VexLifeProdMacFrontDoorProcess.ManifestationRefreshEntry,
            result.manifestationRef,
        )
    }

    @Test
    fun anActivationLeavesItsCauseAndEffectInTheChronologyInOrder() {
        val facts =
            vexlife.logging.VexLogger.capturing { records ->
                VexLifeProdMacFrontDoorProcess.activateWithHooks(
                    controlRef = VexLifeProdEntryFamily.ControlFormHome,
                    desiredSourceSha = desired,
                    productHome = selectedHome,
                    hooks =
                        VexLifeProdMacFrontDoorProcess.ActivationHooks(
                            resolveControl = { _, controlRef, _ ->
                                VexLifeProdMacFrontDoor.ResolvedControl(
                                    semanticStateRef = VexLifeProdEntryFamily.StateHomeFormationRequired,
                                    controlRef = controlRef,
                                    effect = VexLifeProdEntryFamily.FormCanonicalHome,
                                )
                            },
                            formHome = { _, _ -> },
                            observeSemanticStateRef = { _, _ -> VexLifeProdEntryFamily.StateSetupRequired },
                        ),
                )
                records.map { it.fact to (it.refs["stateRef"] ?: it.refs["effectRef"]) }
            }
        assertEquals(
            listOf(
                "SETUP_CONTROL_ACCEPTED" to VexLifeProdEntryFamily.StateHomeFormationRequired,
                "SETUP_EFFECT_STARTED" to VexLifeProdEntryFamily.EffectFormCanonicalHome,
                "SETUP_EFFECT_COMPLETED" to VexLifeProdEntryFamily.EffectFormCanonicalHome,
                // The successor is what was observed afterwards, not what the Effect hoped for.
                "ENTRY_STATE_OBSERVED" to VexLifeProdEntryFamily.StateSetupRequired,
            ),
            facts,
        )
    }

    @Test
    fun aLanguageChosenInTheWindowIsSavedOnceTheHomeExistsWhateverTheControlDid() {
        val saved = mutableListOf<Pair<Path, String>>()
        val hooks =
            VexLifeProdMacFrontDoorProcess.ActivationHooks(
                resolveControl = { _, controlRef, _ ->
                    VexLifeProdMacFrontDoor.ResolvedControl(
                        semanticStateRef = VexLifeProdEntryFamily.StateHomeFormationRequired,
                        controlRef = controlRef,
                        effect = VexLifeProdEntryFamily.FormCanonicalHome,
                    )
                },
                formHome = { _, _ -> error("the folder is held") },
                observeSemanticStateRef = { _, _ -> VexLifeProdEntryFamily.StateHomeHeld },
                persistLanguage = { home, language ->
                    saved += home to language
                    true
                },
            )

        val chosen =
            VexLifeProdMacFrontDoorProcess.activateWithHooks(
                controlRef = VexLifeProdEntryFamily.ControlFormHome,
                desiredSourceSha = desired,
                productHome = selectedHome,
                hooks = hooks,
                language = "ja",
                persistLanguage = true,
            )
        assertEquals(listOf(selectedHome to "ja"), saved)
        assertEquals(
            VexLifeStrings.text("setup.heading.failure-stopped", "ja"),
            chosen.failure?.title,
        )

        VexLifeProdMacFrontDoorProcess.activateWithHooks(
            controlRef = VexLifeProdEntryFamily.ControlFormHome,
            desiredSourceSha = desired,
            productHome = selectedHome,
            hooks = hooks,
        )
        assertEquals(1, saved.size, "no language was chosen, so nothing is saved")
    }
}

// [VXG RealForever]
