package vexlife.setup

import java.net.ConnectException
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vexlife.entry.VexLifeProdEntryFamily
import vexlife.home.CanonicalVexLifeHomeFixture
import vexlife.home.VexLifeProdHome
import vexlife.home.VexLifeProdHomeFiles
import vexlife.local.VexLifeProdSetupProgress
import vexlife.model.heretic.runtime.HereticArtifactMaterialization
import vexlife.model.heretic.runtime.HereticMacHostProfile
import vexlife.model.heretic.runtime.HereticSetupStorage
import vexlife.platform.macos.MacUserLaunchdProcess
import vexlife.service.VexLifeProdCurrentState
import vexlife.service.VexLifeProdMacServiceLifetime
import vexlife.service.VexLifeProdServiceContract
import vexlife.text.VexLifeStrings
import vexlife.text.VexLifeText

/**
 * Setup Journey edge cases: does the person already have VexLife, may they choose the Home
 * directory, and does this Mac / drive meet the requirements — each answered by fresh no-effect
 * observation before any Control is offered.
 */
class VexLifeProdSetupJourneyTest {
    private val desired = "a".repeat(40)

    private val prior = "b".repeat(40)

    private val gib = 1024L * 1024L * 1024L

    private val totalModelBytes = HereticArtifactMaterialization.plan().totalBytes

    private fun host(
        chip: String = "Apple M4 Pro",
        memoryGiB: Long = 48L,
    ): HereticMacHostProfile.Result =
        HereticMacHostProfile.evaluate(
            HereticMacHostProfile.Observation(
                platform = "darwin",
                architecture = "arm64",
                chipModel = chip,
                totalMemoryBytes = memoryGiB * gib,
            )
        )

    /** Real Home-location classification + real current-state composition; stubbed host/space. */
    private fun realLocationHooks(
        host: HereticMacHostProfile.Result = host(),
        freeBytes: Long = 200L * gib,
        userHome: Path = Path.of("/Users/vex"),
    ): VexLifeProdMacFrontDoor.Hooks =
        VexLifeProdMacFrontDoor.Hooks(
            observeCurrentState = { productHome ->
                VexLifeProdCurrentState.observeWithHooks(
                    VexLifeProdCurrentState.Hooks(
                        resolveProductHome = { productHome },
                        observeHost = { host },
                        observeFreeStorageBytes = { freeBytes },
                        now = { Instant.parse("2026-11-13T12:00:00Z") },
                    )
                )
            },
            observeExistingService = {
                VexLifeProdMacServiceLifetime.ExistingObservation.Absent
            },
            observeRuntimeEnvironmentPresent = { false },
            userHome = { userHome },
        )

    private fun existingLocation(home: Path): VexLifeProdHomeFiles.Location {
        val plan =
            VexLifeProdHome.plan(
                personRef = "person.local-user",
                familyRef = "vex-family.local-user",
                deviceName = "Journey Test Mac",
                platform = "darwin",
                architecture = "arm64",
                ids =
                    VexLifeProdHome.InstallationIds(
                        deviceRef = "device.vexlife.00000000-0000-0000-0000-000000000011",
                        companionLineageRef =
                            "companion-lineage.vexlife.00000000-0000-0000-0000-000000000012",
                    ),
                createdAt = "2026-11-13T04:00:00-08:00",
            )
        return VexLifeProdHomeFiles.Location.Existing(
            VexLifeProdHomeFiles.Result(
                state = VexLifeProdHome.LocationDisposition.Canonical.wireValue,
                productHome = home.toString(),
                manifest = plan.manifest,
                installation = plan.installation,
                effectsPerformed = false,
            )
        )
    }

    private val existingHome = Path.of("/tmp/.vexlife-journey")

    private fun existingState(
        hostResult: HereticMacHostProfile.Result = host(),
        modelSelectionState: String = "BOUND_HERETIC_PRODUCT_MODEL",
        artifactState: String = "COMPLETE_BY_SIZE_NOT_HASHED",
        presentBytes: Long = totalModelBytes,
        invalidBytes: Long = 0L,
        freeBytes: Long? = 100L * gib,
    ): VexLifeProdServiceContract.CurrentStateProjection {
        val unconfigured = modelSelectionState == "UNCONFIGURED"
        return VexLifeProdServiceContract.CurrentStateProjection(
            schemaVersion = VexLifeProdServiceContract.CurrentStateProjectionSchema,
            observedAt = "2026-11-13T12:00:00Z",
            homePath = existingHome.toString(),
            homeDisposition = VexLifeProdHome.LocationDisposition.Canonical.wireValue,
            homeRef = "vex-home.device.vexlife.journey",
            deviceRef = "device.vexlife.journey",
            companionLineageRef = "companion-lineage.vexlife.journey",
            platform = hostResult.observation.platform,
            architecture = hostResult.observation.architecture,
            chipModel = hostResult.observation.chipModel,
            hostEligibilityState = hostResult.state.name.uppercase(),
            hostEligibilityReason = hostResult.reason,
            totalMemoryBytes = hostResult.observation.totalMemoryBytes,
            modelMinimumMemoryBytes =
                if (unconfigured) null else HereticMacHostProfile.MinimumMemoryBytes,
            freeStorageBytes = freeBytes,
            modelSelectionState = modelSelectionState,
            activeModelRef = if (unconfigured) null else "model.journey",
            activeModelProfileRef = if (unconfigured) null else "profile.journey",
            runtimeState =
                when (modelSelectionState) {
                    "UNCONFIGURED" -> "NOT_SELECTED"
                    "HERETIC_ESTABLISHMENT_PENDING" -> "ESTABLISHMENT_PENDING"
                    else -> "BOUND_PRODUCT_MODEL"
                },
            runtimeCurrentness =
                if (unconfigured) "NOT_APPLICABLE" else "CONFIGURATION_ONLY_NOT_REOBSERVED",
            runtimeReceiptPath = if (unconfigured) null else "runtime/vexlife-prod/heretic.json",
            modelArtifactState = if (unconfigured) "NOT_SELECTED" else artifactState,
            modelArtifactTotalBytes = if (unconfigured) null else totalModelBytes,
            modelArtifactPresentBySizeBytes = if (unconfigured) null else presentBytes,
            modelArtifactMissingBytes =
                if (unconfigured) null else totalModelBytes - presentBytes - invalidBytes,
            modelArtifactInvalidSizeBytes = if (unconfigured) null else invalidBytes,
            effectsPerformed = false,
        )
    }

    private fun receipt(sourceSha: String): VexLifeProdMacServiceLifetime.Receipt =
        VexLifeProdMacServiceLifetime.Receipt(
            schemaVersion = VexLifeProdMacServiceLifetime.ReceiptSchema,
            sourceSha = sourceSha,
            imagePayloadSha256 = "c".repeat(64),
            imagePath = "$existingHome/runtime/product-service/images/$sourceSha",
            serviceOrigin = VexLifeProdServiceContract.Origin,
            launchd =
                MacUserLaunchdProcess.Receipt(
                    schemaVersion = MacUserLaunchdProcess.Schema,
                    substrateRef = MacUserLaunchdProcess.SubstrateRef,
                    label = VexLifeProdMacServiceLifetime.Label,
                    domainTarget = "gui/501",
                    plistPath =
                        "$existingHome/runtime/product-service/launchd/com.vexlife.prod.service.plist",
                    programPath =
                        "$existingHome/runtime/product-service/images/$sourceSha/bin/vexlife-prod-service",
                    programArguments =
                        listOf("--source-sha", sourceSha, "--home", existingHome.toString()),
                    workingDirectory = "$existingHome/runtime/product-service/images/$sourceSha",
                    stdoutPath = "$existingHome/runtime/product-service/logs/service.out.log",
                    stderrPath = "$existingHome/runtime/product-service/logs/service.err.log",
                    environment = emptyMap(),
                    keepAlive = false,
                    pid = 4242L,
                ),
        )

    private fun existingHooks(
        current: VexLifeProdServiceContract.CurrentStateProjection = existingState(),
        service: VexLifeProdMacServiceLifetime.ExistingObservation =
            VexLifeProdMacServiceLifetime.ExistingObservation.Absent,
        runtimeEnvironmentPresent: Boolean = true,
    ): VexLifeProdMacFrontDoor.Hooks =
        VexLifeProdMacFrontDoor.Hooks(
            observeCurrentState = { current },
            observeHomeLocation = { existingLocation(existingHome) },
            observeExistingService = { service },
            observeRuntimeEnvironmentPresent = { runtimeEnvironmentPresent },
            userHome = { Path.of("/Users/vex") },
        )

    private fun observe(
        home: Path,
        hooks: VexLifeProdMacFrontDoor.Hooks,
    ): VexLifeProdMacFrontDoor.Projection =
        VexLifeProdMacFrontDoor.observeWithHooks(
            desiredSourceSha = desired,
            productHome = home,
            hooks = hooks,
        )

    private fun resolve(
        home: Path,
        controlRef: String,
        hooks: VexLifeProdMacFrontDoor.Hooks,
    ): VexLifeProdMacFrontDoor.ResolvedControl =
        VexLifeProdMacFrontDoor.resolveTypedControlWithHooks(
            desiredSourceSha = desired,
            controlRef = controlRef,
            productHome = home,
            hooks = hooks,
        )

    private fun en(text: VexLifeText): String = VexLifeStrings.resolve(text, "en")

    private fun assertCardMirrorsFamily(projection: VexLifeProdMacFrontDoor.Projection) {
        val display = projection.displayState
        val cardControls =
            projection.card.buttons
                .map { it.actionRef }
                .filter { it.startsWith(VexLifeProdMacSetupCard.ActionControlPrefix) }
                .map { it.removePrefix(VexLifeProdMacSetupCard.ActionControlPrefix) }
        assertEquals(
            listOfNotNull(display.primaryControlRef, display.secondaryControlRef),
            cardControls,
            "native card must carry exactly the Family's displayed Controls",
        )
        assertEquals(en(display.title), projection.card.title)
        assertEquals(display.title.key, projection.card.titleKey)
        assertTrue(projection.card.body.startsWith(en(display.detail)))
        for (button in projection.card.buttons) {
            assertEquals(
                VexLifeProdSetupElements.dataKeyForAction(button.actionRef),
                VexLifeStrings.catalog.dataKey(button.key),
                "a button's element key maps to the action it returns: ${button.key}",
            )
        }
        assertEquals(VexLifeProdMacSetupCard.ActionClose, projection.card.buttons.last().actionRef)
    }

    // ---------------------------------------------------------------- 1. Vex Home location

    @Test
    fun emptyFolderTheHumanCreatedIsFormableInsteadOfCrashing() {
        val parent = Files.createTempDirectory("vexlife-journey-empty-").toAbsolutePath()
        val home = Files.createDirectory(parent.resolve("My Vex Home"))
        try {
            val projection = observe(home, realLocationHooks())

            assertEquals(
                VexLifeProdEntryFamily.StateHomeFormationRequired,
                projection.semanticStateRef,
            )
            assertEquals(
                VexLifeProdHome.LocationDisposition.EmptyDirectory.wireValue,
                projection.currentState.homeDisposition,
            )
            assertTrue(
                projection.card.body.contains("an empty folder VexLife can set up as your Home")
            )
            assertTrue(projection.card.rail.startsWith("✓ This Mac   ● Vex Home"))
            assertCardMirrorsFamily(projection)
        } finally {
            parent.toFile().deleteRecursively()
        }
    }

    @Test
    fun folderWithOtherFilesIsHeldPreservedAndOffersOnlyNavigation() {
        val home = Files.createTempDirectory("vexlife-journey-noncanonical-").toAbsolutePath()
        val keep = Files.writeString(home.resolve("thesis.docx"), "precious")
        try {
            val hooks = realLocationHooks()
            val projection = observe(home, hooks)

            assertEquals(VexLifeProdEntryFamily.StateHomeHeld, projection.semanticStateRef)
            assertNull(projection.displayState.primaryControlRef)
            assertEquals(
                listOf(
                    VexLifeProdMacSetupCard.ActionChooseHome,
                    VexLifeProdMacSetupCard.ActionRecheck,
                    VexLifeProdMacSetupCard.ActionChooseLanguage,
                    VexLifeProdMacSetupCard.ActionClose,
                ),
                projection.card.buttons.map { it.actionRef },
            )
            assertFalse(resolve(home, VexLifeProdEntryFamily.ControlFormHome, hooks).lawful)
            assertEquals("precious", Files.readString(keep))
            assertEquals(listOf(keep), Files.list(home).use { it.toList() })
            assertCardMirrorsFamily(projection)
        } finally {
            home.toFile().deleteRecursively()
        }
    }

    @Test
    fun fileInsteadOfFolderIsHeldNotThrown() {
        val parent = Files.createTempDirectory("vexlife-journey-file-").toAbsolutePath()
        val file = Files.writeString(parent.resolve("vexlife"), "not a folder")
        try {
            val projection = observe(file, realLocationHooks())
            assertEquals(VexLifeProdEntryFamily.StateHomeHeld, projection.semanticStateRef)
            assertTrue(en(projection.displayState.detail).contains("is a file"))
        } finally {
            parent.toFile().deleteRecursively()
        }
    }

    @Test
    fun alternateHomeUnderMissingFoldersIsFormableAndSaysWhatWillBeCreated() {
        val parent = Files.createTempDirectory("vexlife-journey-nested-").toAbsolutePath()
        val home = parent.resolve("VexLife").resolve("Homes").resolve("Victor")
        try {
            val projection = observe(home, realLocationHooks())
            assertEquals(
                VexLifeProdEntryFamily.StateHomeFormationRequired,
                projection.semanticStateRef,
            )
            assertTrue(projection.card.body.contains("will also create 2 folders"))
            assertFalse(
                Files.exists(parent.resolve("VexLife")),
                "observation must not create folders",
            )
        } finally {
            parent.toFile().deleteRecursively()
        }
    }

    @Test
    fun existingCanonicalHomeAtAlternateLocationIsRecognized() {
        val home = Files.createTempDirectory("vexlife-journey-canonical-").toAbsolutePath()
        try {
            CanonicalVexLifeHomeFixture.form(
                home = home,
                deviceSuffix = "journey",
                lineageSuffix = "journey",
            )
            val projection = observe(home, realLocationHooks())

            assertEquals(VexLifeProdEntryFamily.StateSetupRequired, projection.semanticStateRef)
            assertTrue(projection.card.body.contains("your existing Vex Home"))
            assertEquals(home.toString(), projection.currentState.homePath)
        } finally {
            home.toFile().deleteRecursively()
        }
    }

    @Test
    fun privacyProtectedHomeLocationIsExplainedBeforeFormation() {
        val parent = Files.createTempDirectory("vexlife-journey-tcc-").toAbsolutePath()
        val fakeUserHome = parent
        val home = parent.resolve("Documents").resolve("Vex")
        Files.createDirectories(parent.resolve("Documents"))
        try {
            val projection = observe(home, realLocationHooks(userHome = fakeUserHome))
            assertEquals(
                VexLifeProdEntryFamily.StateHomeFormationRequired,
                projection.semanticStateRef,
            )
            assertTrue(projection.card.body.contains("your Documents folder, which macOS protects"))
            assertEquals(
                "setup.label.place-external-drive",
                VexLifeProdMacSetupCard.protectedLocationName(
                        Path.of("/Users/vex"),
                        Path.of("/Volumes/T7/Vex"),
                    )
                    ?.key,
            )
            assertNull(
                VexLifeProdMacSetupCard.protectedLocationName(
                    Path.of("/Users/vex"),
                    Path.of("/Users/vex/.vexlife"),
                )
            )
        } finally {
            parent.toFile().deleteRecursively()
        }
    }

    @Test
    fun lowSpaceIsPreviewedAtHomeFormationWithoutBlockingTheHomeOnlyEffect() {
        val parent = Files.createTempDirectory("vexlife-journey-preview-").toAbsolutePath()
        val home = parent.resolve(".vexlife")
        try {
            val hooks = realLocationHooks(freeBytes = 6L * gib)
            val projection = observe(home, hooks)
            assertEquals(
                VexLifeProdEntryFamily.StateHomeFormationRequired,
                projection.semanticStateRef,
            )
            assertTrue(projection.card.body.contains("will need about"))
            assertTrue(resolve(home, VexLifeProdEntryFamily.ControlFormHome, hooks).lawful)
        } finally {
            parent.toFile().deleteRecursively()
        }
    }

    // ---------------------------------------------------------------- 2. This Mac

    @Test
    fun unsupportedMacIsHeldBeforeAnyHomeIsCreated() {
        val parent = Files.createTempDirectory("vexlife-journey-host-").toAbsolutePath()
        val home = parent.resolve(".vexlife")
        try {
            val hooks = realLocationHooks(host = host(chip = "Apple M2", memoryGiB = 16L))
            val projection = observe(home, hooks)

            assertEquals(VexLifeProdEntryFamily.StateHostHeld, projection.semanticStateRef)
            assertNull(projection.displayState.primaryControlRef)
            assertTrue(projection.card.rail.startsWith("⊘ This Mac"))
            assertTrue(projection.card.body.contains("needs Apple M4 Pro"))
            assertFalse(resolve(home, VexLifeProdEntryFamily.ControlFormHome, hooks).lawful)
            assertFalse(Files.exists(home))
        } finally {
            parent.toFile().deleteRecursively()
        }
    }

    @Test
    fun unsupportedMacWithExistingHomeKeepsTheHomeAndOffersNoStart() {
        val projection =
            observe(
                existingHome,
                existingHooks(
                    current =
                        existingState(hostResult = host(chip = "Apple M3 Max", memoryGiB = 64L))
                ),
            )
        assertEquals(VexLifeProdEntryFamily.StateHostHeld, projection.semanticStateRef)
        assertTrue(
            en(projection.displayState.detail).contains("existing Vex Home was left untouched")
        )
    }

    @Test
    fun runningOwnedServiceStaysOpenableEvenWhenHostObservationIsIneligible() {
        val projection =
            observe(
                existingHome,
                existingHooks(
                    current = existingState(hostResult = host(chip = "Apple M2")),
                    service =
                        VexLifeProdMacServiceLifetime.ExistingObservation.RunningOwned(
                            receipt = receipt(desired),
                            sourceCurrentness =
                                VexLifeProdServiceContract.SourceCurrentness(
                                    schemaVersion =
                                        VexLifeProdServiceContract.SourceCurrentnessSchema,
                                    serviceRef = VexLifeProdServiceContract.ServiceRef,
                                    productRef = VexLifeProdServiceContract.ProductRef,
                                    serviceState = "SOURCE_BOUND_LISTENING",
                                    sourceSha = desired,
                                    processPid = 4242L,
                                    frameHostOrigin =
                                        vexlife.browser.VexLifeProdBrowserContract.FrameHostOrigin,
                                    runtimeCurrentness = "NOT_REOBSERVED_BY_SERVICE",
                                    startupQualifiedRuntimeReceiptRef = "receipt.journey",
                                    currentConversationHeadSha256 = null,
                                    effectsPerformed = false,
                                ),
                        ),
                ),
            )
        assertEquals(VexLifeProdEntryFamily.StateCurrent, projection.semanticStateRef)
        assertCardMirrorsFamily(projection)
    }

    // ---------------------------------------------------------------- 3. Storage + model

    @Test
    fun insufficientFreeSpaceHoldsAcquisitionAndShowsTheNumbers() {
        val hooks =
            existingHooks(
                current =
                    existingState(
                        artifactState = "ABSENT",
                        presentBytes = 0L,
                        freeBytes = 5L * gib,
                    ),
                runtimeEnvironmentPresent = false,
            )
        val projection = observe(existingHome, hooks)

        assertEquals(VexLifeProdEntryFamily.StateStorageHeld, projection.semanticStateRef)
        val plan = assertNotNull(projection.setupPlan)
        assertTrue(plan.storageShortfallObserved)
        assertEquals(
            totalModelBytes + HereticSetupStorage.RuntimeAllowanceBytes,
            plan.requiredFreeStorageBytes,
        )
        assertFalse(resolve(existingHome, VexLifeProdEntryFamily.ControlSetUp, hooks).lawful)
        assertTrue(projection.card.body.contains("What setup will do"))
        assertCardMirrorsFamily(projection)
    }

    @Test
    fun unknownFreeSpaceIsNotTreatedAsInsufficient() {
        val projection =
            observe(
                existingHome,
                existingHooks(
                    current =
                        existingState(
                            artifactState = "ABSENT",
                            presentBytes = 0L,
                            freeBytes = null,
                        ),
                    runtimeEnvironmentPresent = false,
                ),
            )
        assertEquals(VexLifeProdEntryFamily.StateSetupRequired, projection.semanticStateRef)
        assertTrue(projection.card.body.contains("free space unknown"))
    }

    @Test
    fun interruptedDownloadResumesThroughSetupWithOnlyTheRemainingBytes() {
        val present = 9L * gib
        val projection =
            observe(
                existingHome,
                existingHooks(
                    current =
                        existingState(
                            modelSelectionState = "HERETIC_ESTABLISHMENT_PENDING",
                            artifactState = "PARTIAL_BY_SIZE",
                            presentBytes = present,
                        ),
                    runtimeEnvironmentPresent = false,
                ),
            )
        assertEquals(VexLifeProdEntryFamily.StateSetupRequired, projection.semanticStateRef)
        val plan = assertNotNull(projection.setupPlan)
        assertEquals(totalModelBytes - present, plan.modelArtifactMissingBytes)
        assertTrue(projection.card.body.contains("9.0 of 14.1 GiB downloaded"))
    }

    @Test
    fun invalidModelFilesAreHeldRatherThanOfferingAnAcquisitionThatWouldStop() {
        val hooks =
            existingHooks(
                current =
                    existingState(
                        artifactState = "INVALID_SIZE",
                        presentBytes = 0L,
                        invalidBytes = 2L * gib,
                    )
            )
        val projection = observe(existingHome, hooks)
        assertEquals(VexLifeProdEntryFamily.StateModelFilesHeld, projection.semanticStateRef)
        assertFalse(resolve(existingHome, VexLifeProdEntryFamily.ControlSetUp, hooks).lawful)
    }

    // ---------------------------------------------------------------- 4. Already have VexLife

    @Test
    fun stoppedServiceAfterMacRestartIsStartNotRepair() {
        val stopped =
            VexLifeProdMacServiceLifetime.ExistingObservation.OwnedNotReady(
                receipt = receipt(desired),
                launchdObservation = MacUserLaunchdProcess.Observation.Absent,
            )
        val hooks = existingHooks(service = stopped)
        val projection = observe(existingHome, hooks)
        val resolved = resolve(existingHome, VexLifeProdEntryFamily.ControlStart, hooks)

        assertEquals(VexLifeProdEntryFamily.StateStartRequired, projection.semanticStateRef)
        assertEquals("Start VexLife", en(projection.displayState.primaryLabel!!))
        assertNull(projection.currentServicePid)
        val effect = resolved.effect as VexLifeProdEntryFamily.ConvergeProduct
        assertFalse(effect.allowAcquisition)
        assertTrue(effect.allowRuntimeStart)
        assertTrue(projection.card.body.contains("VexLife service: not running"))
        assertCardMirrorsFamily(projection)
    }

    @Test
    fun stoppedPriorVersionStartsTheDesiredVersionAndSaysSo() {
        val projection =
            observe(
                existingHome,
                existingHooks(
                    service =
                        VexLifeProdMacServiceLifetime.ExistingObservation.OwnedNotReady(
                            receipt = receipt(prior),
                            launchdObservation = MacUserLaunchdProcess.Observation.Absent,
                        )
                ),
            )
        assertEquals(VexLifeProdEntryFamily.StateStartRequired, projection.semanticStateRef)
        assertTrue(en(projection.displayState.detail).contains("newer version"))
    }

    @Test
    fun loadedButUnresponsiveServiceIsStillRepair() {
        val projection =
            observe(
                existingHome,
                existingHooks(
                    service =
                        VexLifeProdMacServiceLifetime.ExistingObservation.OwnedNotReady(
                            receipt = receipt(desired),
                            launchdObservation = MacUserLaunchdProcess.Observation.LoadedStopped,
                        )
                ),
            )
        assertEquals(VexLifeProdEntryFamily.StateRepairRequired, projection.semanticStateRef)
    }

    @Test
    fun unresponsiveServiceWithMissingRuntimeNeedsSetupNotARepairThatCannotAcquire() {
        val projection =
            observe(
                existingHome,
                existingHooks(
                    service =
                        VexLifeProdMacServiceLifetime.ExistingObservation.OwnedNotReady(
                            receipt = receipt(desired),
                            launchdObservation = MacUserLaunchdProcess.Observation.LoadedStopped,
                        ),
                    runtimeEnvironmentPresent = false,
                ),
            )
        assertEquals(VexLifeProdEntryFamily.StateSetupRequired, projection.semanticStateRef)
    }

    @Test
    fun completeDownloadWithPendingBindingStartsWithoutAcquisition() {
        val projection =
            observe(
                existingHome,
                existingHooks(
                    current = existingState(modelSelectionState = "HERETIC_ESTABLISHMENT_PENDING")
                ),
            )
        assertEquals(VexLifeProdEntryFamily.StateStartRequired, projection.semanticStateRef)
    }

    @Test
    fun anotherHomesServiceIsHeldWithChooseAnotherLocation() {
        val projection =
            observe(
                existingHome,
                existingHooks(
                    service =
                        VexLifeProdMacServiceLifetime.ExistingObservation.HeldUnknown(
                            "stable product-service launchd label is present without a product lifetime receipt"
                        )
                ),
            )
        assertEquals(VexLifeProdEntryFamily.StateHeldUnknown, projection.semanticStateRef)
        assertTrue(
            projection.card.buttons.any { it.actionRef == VexLifeProdMacSetupCard.ActionChooseHome }
        )
        assertTrue(en(projection.displayState.detail).contains("Vex Home somewhere else"))
    }

    @Test
    fun homeThatChangesBetweenTwoReadsIsHeldInsteadOfGuessed() {
        val projection =
            observe(
                existingHome,
                existingHooks()
                    .copy(
                        observeHomeLocation = {
                            VexLifeProdHomeFiles.Location.Formable(
                                productHome = existingHome.toString(),
                                disposition = VexLifeProdHome.LocationDisposition.Absent,
                                nearestExistingAncestor = "/tmp",
                                missingAncestorCount = 0,
                            )
                        }
                    ),
            )
        assertEquals(VexLifeProdEntryFamily.StateHeldUnknown, projection.semanticStateRef)
        assertTrue(projection.serviceDetail!!.contains("changed while VexLife was looking"))
    }

    // ---------------------------------------------------------------- activation boundary

    private fun activation(
        resolved: VexLifeProdMacFrontDoor.ResolvedControl,
        formHome: (String, Path) -> Unit = { _, _ -> error("unexpected Home formation") },
        convergeProduct: (String, Boolean, Boolean, Path) -> Unit = { _, _, _, _ ->
            error("unexpected convergence")
        },
        reobserved: String = VexLifeProdEntryFamily.StateCurrent,
        lines: MutableList<String> = mutableListOf(),
    ): VexLifeProdMacFrontDoorProcess.ActivationProjection =
        VexLifeProdMacFrontDoorProcess.activateWithHooks(
            controlRef = resolved.controlRef,
            desiredSourceSha = desired,
            productHome = existingHome,
            hooks =
                VexLifeProdMacFrontDoorProcess.ActivationHooks(
                    progress = VexLifeProdSetupProgress.Lines(sink = { lines += it }),
                    resolveControl = { _, _, _ -> resolved },
                    formHome = formHome,
                    convergeProduct = convergeProduct,
                    observeSemanticStateRef = { _, _ -> reobserved },
                ),
        )

    @Test
    fun controlNoLongerLawfulAfterFreshReobservationRealizesNothing() {
        val result =
            activation(
                VexLifeProdMacFrontDoor.ResolvedControl(
                    semanticStateRef = VexLifeProdEntryFamily.StateCurrent,
                    controlRef = VexLifeProdEntryFamily.ControlFormHome,
                    effect = null,
                    lawful = false,
                )
            )
        assertEquals(
            VexLifeProdMacFrontDoorProcess.ManifestationRefreshEntry,
            result.manifestationRef,
        )
        assertEquals(VexLifeProdSetupFailure.StateChanged, result.failure?.failureRef)
        assertEquals(VexLifeProdEntryFamily.StateCurrent, result.semanticStateRef)
    }

    @Test
    fun failedEffectIsReobservedAndExplainedInsteadOfPredicted() {
        val result =
            activation(
                VexLifeProdMacFrontDoor.ResolvedControl(
                    semanticStateRef = VexLifeProdEntryFamily.StateSetupRequired,
                    controlRef = VexLifeProdEntryFamily.ControlSetUp,
                    effect =
                        VexLifeProdEntryFamily.ConvergeProduct(
                            desired,
                            allowAcquisition = true,
                            allowRuntimeStart = true,
                        ),
                ),
                convergeProduct = { _, _, _, _ ->
                    throw IllegalStateException(
                        "artifact delivery stopped",
                        ConnectException("Connection refused"),
                    )
                },
                reobserved = VexLifeProdEntryFamily.StateSetupRequired,
            )
        assertEquals(
            VexLifeProdMacFrontDoorProcess.ManifestationRefreshEntry,
            result.manifestationRef,
        )
        assertEquals(VexLifeProdEntryFamily.StateSetupRequired, result.semanticStateRef)
        val failure = assertNotNull(result.failure)
        assertEquals(VexLifeProdSetupFailure.NetworkUnavailable, failure.failureRef)
        assertTrue(failure.detail.contains("resumes"))
        assertTrue(failure.technicalDetail!!.contains("ConnectException"))
        assertNull(result.productOrigin)
    }

    @Test
    fun startCarriesRuntimeStartWithoutAcquisitionAndOpensOnlyWhenReobservedCurrent() {
        var authority: Pair<Boolean, Boolean>? = null
        val lines = mutableListOf<String>()
        val result =
            activation(
                VexLifeProdMacFrontDoor.ResolvedControl(
                    semanticStateRef = VexLifeProdEntryFamily.StateStartRequired,
                    controlRef = VexLifeProdEntryFamily.ControlStart,
                    effect =
                        VexLifeProdEntryFamily.ConvergeProduct(
                            desired,
                            allowAcquisition = false,
                            allowRuntimeStart = true,
                        ),
                ),
                convergeProduct = { _, acquire, start, home ->
                    assertEquals(existingHome, home)
                    authority = acquire to start
                },
                lines = lines,
            )
        assertEquals(false to true, authority)
        assertEquals(
            VexLifeProdMacFrontDoorProcess.ManifestationOpenProduct,
            result.manifestationRef,
        )
        assertEquals(VexLifeProdServiceContract.Origin, result.productOrigin)
        assertTrue(lines.any { it.startsWith(VexLifeProdSetupProgress.LinePrefix + "Confirming") })
    }

    @Test
    fun convergenceThatDoesNotReachCurrentRefreshesTheActualState() {
        val result =
            activation(
                VexLifeProdMacFrontDoor.ResolvedControl(
                    semanticStateRef = VexLifeProdEntryFamily.StateStartRequired,
                    controlRef = VexLifeProdEntryFamily.ControlStart,
                    effect =
                        VexLifeProdEntryFamily.ConvergeProduct(
                            desired,
                            allowAcquisition = false,
                            allowRuntimeStart = true,
                        ),
                ),
                convergeProduct = { _, _, _, _ -> },
                reobserved = VexLifeProdEntryFamily.StateHeldUnknown,
            )
        assertEquals(
            VexLifeProdMacFrontDoorProcess.ManifestationRefreshEntry,
            result.manifestationRef,
        )
        assertEquals(VexLifeProdEntryFamily.StateHeldUnknown, result.semanticStateRef)
        assertNull(result.failure)
    }

    @Test
    fun inspectionFailureStillGivesTheWindowOneNavigableCard() {
        val failure =
            VexLifeProdMacFrontDoorProcess.inspectionFailure(
                productHome = existingHome,
                error = IllegalStateException("launchctl print timed out"),
            )
        assertTrue(
            failure.card.buttons.none {
                it.actionRef.startsWith(VexLifeProdMacSetupCard.ActionControlPrefix)
            }
        )
        assertTrue(failure.card.body.contains("launchctl print timed out"))
    }

    // ---------------------------------------------------------------- failure + progress
    // projection

    @Test
    fun failureClassificationNamesWhatWasPreserved() {
        assertEquals(
            VexLifeProdSetupFailure.SetupAlreadyRunning,
            VexLifeProdSetupFailure.classify(
                    IllegalArgumentException(
                        "another VexLife-Prod local setup is already in progress or the setup lease is invalid"
                    ),
                    "en",
                )
                .failureRef,
        )
        assertEquals(
            VexLifeProdSetupFailure.StorageExhausted,
            VexLifeProdSetupFailure.classify(java.io.IOException("No space left on device"), "en")
                .failureRef,
        )
        assertEquals(
            VexLifeProdSetupFailure.PrerequisiteMissing,
            VexLifeProdSetupFailure.classify(
                    IllegalArgumentException(
                        "Heretic artifact model.safetensors is missing and acquisition was not authorized"
                    ),
                    "en",
                )
                .failureRef,
        )
        assertEquals(
            VexLifeProdSetupFailure.StoppedSafely,
            VexLifeProdSetupFailure.classify(IllegalStateException("x".repeat(5_000)), "en")
                .failureRef,
        )
        assertTrue(
            VexLifeProdSetupFailure.classify(IllegalStateException("x".repeat(5_000)), "en")
                .technicalDetail!!
                .length <= 1_500
        )
    }

    @Test
    fun progressLinesUseThePredecessorPrefixAndBoundDownloadChatter() {
        val lines = mutableListOf<String>()
        var now = 0L
        val progress = VexLifeProdSetupProgress.Lines(sink = { lines += it }, clockMs = { now })

        progress.phase(VexLifeProdSetupProgress.Phase.CheckingModelFiles)
        val total = 10L * gib
        for (step in 0L until 1_000L) {
            progress.artifactBytes(step * (total / 2_000L), total)
        }
        now += 5_000L
        progress.artifactBytes(total, total)

        assertTrue(lines.all { it.startsWith("[VexLife] ") })
        assertTrue(lines.size < 60, "download progress must be bounded, was ${lines.size}")
        assertTrue(lines.last().contains("10.0 of 10.0 GiB (100%)"))
    }
}

// [VXG RealForever]
