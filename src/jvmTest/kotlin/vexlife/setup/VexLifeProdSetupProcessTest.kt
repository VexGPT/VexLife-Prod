package vexlife.setup

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue
import vexlife.entry.VexLifeProdEntryFamily
import vexlife.home.VexLifeProdHome
import vexlife.home.VexLifeProdHomeFiles
import vexlife.local.VexLifeProdLocalSetup
import vexlife.model.heretic.HereticModelConfiguration
import vexlife.model.heretic.HereticProductIdentity
import vexlife.model.heretic.HereticRuntimeQualification
import vexlife.model.heretic.HereticRuntimeState
import vexlife.packaging.VexLifeProdPrivateServiceImage
import vexlife.packaging.VexLifeProdPrivateServiceImageInstaller
import vexlife.service.VexLifeProdMacServiceLifetime

class VexLifeProdSetupProcessTest {
    private fun runtimeEvidence(): HereticModelConfiguration.QualifiedRuntimeEvidence =
        HereticModelConfiguration.QualifiedRuntimeEvidence(
            runtimeReceiptRef = "receipt.vexlife-prod.heretic.setup-process-test",
            runtimeState = HereticRuntimeState.Qualified,
            modelRef = HereticProductIdentity.ModelRef,
            modelProfileRef = HereticProductIdentity.ModelProfileRef,
            artifactRef = HereticProductIdentity.ArtifactSetRef,
            artifactContentSetSha256 = HereticProductIdentity.ArtifactContentSetSha256,
            runtimeAdapterRef = HereticProductIdentity.RuntimeAdapterRef,
            runtimeClass = HereticProductIdentity.RuntimeClass,
            endpoint = HereticProductIdentity.Endpoint,
            requestModel = HereticProductIdentity.RequestModel,
            qualification = HereticRuntimeQualification.qualified(),
        )

    private fun setupResult(
        productHome: String,
    ): VexLifeProdLocalSetup.Result {
        val runtime =
            runtimeEvidence()

        return VexLifeProdLocalSetup.Result(
            state = "LOCAL_HERETIC_FOUNDATION_READY",
            productHome = productHome,
            homeDisposition = "TEST_CURRENT",
            modelSelectionDisposition = "TEST_BOUND",
            managedPythonDisposition = "TEST_REUSED",
            artifactDisposition = "TEST_REUSED",
            environmentDisposition = "TEST_REUSED",
            runtimeDisposition = "TEST_REUSED",
            modelBindingDisposition = "TEST_BOUND",
            modelConfigurationState = HereticModelConfiguration.State.BoundProductModel,
            qualifiedRuntimeEvidence = runtime,
            endpoint = runtime.endpoint,
            requestModel = runtime.requestModel,
        )
    }

    @Test
    fun processArgumentsCarryOnlyExpectedSourceAndExplicitAcquisitionAuthorization() {
        val sha =
            "a".repeat(
                40,
            )

        val parsed =
            VexLifeProdSetupProcess.parse(
                arrayOf(
                    "--expected-source-sha",
                    sha,
                    "--allow-acquisition",
                ),
            )

        assertEquals(
            sha,
            parsed.expectedSourceSha,
        )
        assertEquals(
            true,
            parsed.allowAcquisition,
        )

        val reuseExisting =
            VexLifeProdSetupProcess.parse(
                arrayOf(
                    "--expected-source-sha",
                    sha,
                    "--reuse-existing",
                ),
            )
        assertEquals(
            false,
            reuseExisting.allowAcquisition,
        )

        for (
            args in
            listOf(
                emptyArray(),
                arrayOf(
                    "--expected-source-sha",
                    sha,
                ),
                arrayOf(
                    "--expected-source-sha",
                    "main",
                    "--allow-acquisition",
                ),
                arrayOf(
                    "--expected-source-sha",
                    sha,
                    "--allow-acquisition",
                    "--home",
                    "/tmp/alternate",
                ),
                arrayOf(
                    "--expected-source-sha",
                    sha,
                    "--repair-anything",
                ),
            )
        ) {
            assertFailsWith<IllegalArgumentException> {
                VexLifeProdSetupProcess.parse(
                    args,
                )
            }
        }
    }

    @Test
    fun HomeFormationVerifiesImageAndStopsBeforeAcquisitionOrProductEffects() {
        val trace =
            mutableListOf<String>()
        val sourceSha =
            "a".repeat(
                40,
            )
        val sourceImage =
            Path.of(
                "/tmp/vexlife-prod-home-formation-source-image",
            )
        val observation =
            VexLifeProdPrivateServiceImage.Observation(
                imageRef = "image.vexlife-prod.private-service.home-test",
                sourceSha = sourceSha,
                payloadSha256 =
                    "b".repeat(
                        64,
                    ),
                launcherPath = sourceImage.resolve("bin/vexlife-prod-service").toString(),
                setupLauncherPath = sourceImage.resolve("bin/vexlife-prod-setup").toString(),
                setupWindowPath = sourceImage.resolve("setup/vexlife-prod-setup-window.applescript").toString(),
                javaPath = sourceImage.resolve("runtime/bin/java").toString(),
                runtimeModules = listOf("java.base", "java.net.http", "jdk.httpserver"),
                effectsPerformed = false,
            )
        val plan =
            VexLifeProdHome.plan(
                personRef = VexLifeProdHome.DefaultPersonRef,
                familyRef = VexLifeProdHome.DefaultFamilyRef,
                deviceName = "Test Mac",
                platform = "darwin",
                architecture = "arm64",
                ids =
                    VexLifeProdHome.InstallationIds(
                        deviceRef = "device.vexlife.home-process-test",
                        companionLineageRef = "companion-lineage.vexlife.home-process-test",
                    ),
                createdAt = "2026-11-13T00:00:00Z",
            )
        val home =
            VexLifeProdHomeFiles.Result(
                state = "FRESH_CANONICAL_VEXLIFE_HOME_FORMED",
                productHome = "/tmp/.vexlife",
                manifest = plan.manifest,
                installation = plan.installation,
                effectsPerformed = true,
            )

        val parsed =
            VexLifeProdSetupProcess.parseHome(
                arrayOf(
                    "--form-home",
                    "--expected-source-sha",
                    sourceSha,
                ),
            )
        assertEquals(
            sourceSha,
            parsed.expectedSourceSha,
        )

        VexLifeProdSetupProcess.formHomeWithHooks(
            arguments = parsed,
            hooks =
                VexLifeProdSetupProcess.Hooks(
                    locateImageRoot = {
                        trace += "LOCATE"
                        sourceImage
                    },
                    observeImage = {
                        imageRoot,
                        expected,
                        ->
                        trace += "VERIFY_IMAGE"
                        assertEquals(
                            sourceImage,
                            imageRoot,
                        )
                        assertEquals(
                            sourceSha,
                            expected,
                        )
                        observation
                    },
                    formFreshHome = {
                        trace += "FORM_HOME"
                        home
                    },
                    establishSession = {
                        error(
                            "Home-only realization must not enter model/runtime setup",
                        )
                    },
                    installImage = {
                        _,
                        _,
                        _,
                        ->
                        error(
                            "Home-only realization must not install Product image",
                        )
                    },
                    startService = {
                        _,
                        _,
                        ->
                        error(
                            "Home-only realization must not start Product service",
                        )
                    },
                    reportImageVerified = {
                        trace += "REPORT_IMAGE"
                    },
                    reportHomeReady = {
                        arguments,
                        formed,
                        ->
                        trace += "HOME_READY"
                        assertEquals(
                            sourceSha,
                            arguments.expectedSourceSha,
                        )
                        assertSame(
                            home,
                            formed,
                        )
                    },
                ),
        )

        assertEquals(
            listOf(
                "LOCATE",
                "VERIFY_IMAGE",
                "REPORT_IMAGE",
                "FORM_HOME",
                "HOME_READY",
            ),
            trace,
        )
    }

    @Test
    fun imageVerificationPrecedesAuthorizedSetupAndInstalledImagePrecedesReady() {
        val trace =
            mutableListOf<String>()
        val sourceSha =
            "a".repeat(
                40,
            )
        val sourceImage =
            Path.of(
                "/tmp/vexlife-prod-setup-source-image",
            )
        val productHome =
            "/tmp/VexLife-Prod"
        val payloadSha =
            "b".repeat(
                64,
            )
        val observation =
            VexLifeProdPrivateServiceImage.Observation(
                imageRef = "image.vexlife-prod.private-service.test",
                sourceSha = sourceSha,
                payloadSha256 = payloadSha,
                launcherPath =
                    sourceImage.resolve(
                        "bin/vexlife-prod-service",
                    )
                        .toString(),
                setupLauncherPath =
                    sourceImage.resolve(
                        "bin/vexlife-prod-setup",
                    )
                        .toString(),
                setupWindowPath =
                    sourceImage.resolve(
                        "setup/vexlife-prod-setup-window.applescript",
                    )
                        .toString(),
                javaPath =
                    sourceImage.resolve(
                        "runtime/bin/java",
                    )
                        .toString(),
                runtimeModules =
                    listOf(
                        "java.base",
                        "java.net.http",
                        "jdk.httpserver",
                    ),
                effectsPerformed = false,
            )
        val setup =
            setupResult(
                productHome,
            )
        val session =
            VexLifeProdLocalSetup.Session(
                result = setup,
                currentRuntimeEvidence = {
                    setup.qualifiedRuntimeEvidence
                },
            )
        val installed =
            VexLifeProdPrivateServiceImageInstaller.Result(
                state = "INSTALLED_VERIFIED_PRIVATE_SERVICE_IMAGE",
                sourceSha = sourceSha,
                payloadSha256 = payloadSha,
                imagePath =
                    "$productHome/runtime/product-service/images/$sourceSha",
                imageMutationPerformed = true,
            )
        val service =
            VexLifeProdMacServiceLifetime.Result(
                disposition = "STARTED_CURRENT_SERVICE",
                sourceSha = sourceSha,
                imagePayloadSha256 = payloadSha,
                pid = 4242L,
                serviceOrigin = "http://127.0.0.1:18210",
                processStarted = true,
                priorProcessRetired = false,
                receiptWritePerformed = true,
            )

        VexLifeProdSetupProcess.runWithHooks(
            arguments =
                VexLifeProdSetupProcess.Arguments(
                    expectedSourceSha = sourceSha,
                    allowAcquisition = true,
                ),
            hooks =
                VexLifeProdSetupProcess.Hooks(
                    locateImageRoot = {
                        trace +=
                            "LOCATE"
                        sourceImage
                    },
                    observeImage = {
                        imageRoot,
                        expected,
                        ->
                        trace +=
                            "VERIFY_IMAGE"
                        assertEquals(
                            sourceImage,
                            imageRoot,
                        )
                        assertEquals(
                            sourceSha,
                            expected,
                        )
                        observation
                    },
                    establishSession = {
                        allowAcquisition ->
                        trace +=
                            "SETUP"
                        assertEquals(
                            true,
                            allowAcquisition,
                        )
                        session
                    },
                    installImage = {
                        imageRoot,
                        home,
                        expected,
                        ->
                        trace +=
                            "INSTALL"
                        assertEquals(
                            sourceImage,
                            imageRoot,
                        )
                        assertEquals(
                            Path.of(
                                productHome,
                            ),
                            home,
                        )
                        assertEquals(
                            sourceSha,
                            expected,
                        )
                        installed
                    },
                    startService = {
                        home,
                        expected,
                        ->
                        trace +=
                            "SERVICE"
                        assertEquals(
                            Path.of(
                                productHome,
                            ),
                            home,
                        )
                        assertEquals(
                            sourceSha,
                            expected,
                        )
                        service
                    },
                    reportImageVerified = {
                        value ->
                        trace +=
                            "REPORT_IMAGE"
                        assertSame(
                            observation,
                            value,
                        )
                    },
                    reportReady = {
                        arguments,
                        currentSession,
                        currentInstalled,
                        currentService,
                        ->
                        trace +=
                            "READY"
                        assertEquals(
                            sourceSha,
                            arguments.expectedSourceSha,
                        )
                        assertSame(
                            session,
                            currentSession,
                        )
                        assertSame(
                            installed,
                            currentInstalled,
                        )
                        assertSame(
                            service,
                            currentService,
                        )
                    },
                ),
        )

        assertEquals(
            listOf(
                "LOCATE",
                "VERIFY_IMAGE",
                "REPORT_IMAGE",
                "SETUP",
                "INSTALL",
                "SERVICE",
                "READY",
            ),
            trace,
        )
    }

    @Test
    fun reuseExistingModeCarriesNoAcquisitionAuthorityIntoLocalSetup() {
        val sourceSha =
            "a".repeat(
                40,
            )
        val sourceImage =
            Path.of(
                "/tmp/vexlife-prod-setup-reuse-existing",
            )
        val observation =
            VexLifeProdPrivateServiceImage.Observation(
                imageRef = "image.vexlife-prod.private-service.reuse-test",
                sourceSha = sourceSha,
                payloadSha256 =
                    "b".repeat(
                        64,
                    ),
                launcherPath = sourceImage.resolve("bin/vexlife-prod-service").toString(),
                setupLauncherPath = sourceImage.resolve("bin/vexlife-prod-setup").toString(),
                setupWindowPath = sourceImage.resolve("setup/vexlife-prod-setup-window.applescript").toString(),
                javaPath = sourceImage.resolve("runtime/bin/java").toString(),
                runtimeModules = listOf("java.base", "java.net.http", "jdk.httpserver"),
                effectsPerformed = false,
            )

        assertFailsWith<IllegalStateException> {
            VexLifeProdSetupProcess.runWithHooks(
                arguments =
                    VexLifeProdSetupProcess.Arguments(
                        expectedSourceSha = sourceSha,
                        allowAcquisition = false,
                    ),
                hooks =
                    VexLifeProdSetupProcess.Hooks(
                        locateImageRoot = {
                            sourceImage
                        },
                        observeImage = {
                            _,
                            _,
                            ->
                            observation
                        },
                        reportImageVerified = {
                        },
                        establishSession = {
                            allowAcquisition ->
                            assertFalse(
                                allowAcquisition,
                            )
                            throw IllegalStateException(
                                "stop after authority projection",
                            )
                        },
                    ),
            )
        }
    }

    @Test
    fun frontDoorHomeActivationRealizesBehindProductBoundaryAndRequestsRefresh() {
        val desired =
            "a".repeat(
                40,
            )
        var homeCalls =
            0
        var setupCalls =
            0

        val result =
            VexLifeProdSetupProcess.activateFrontDoorWithHooks(
                controlRef =
                    VexLifeProdEntryFamily.ControlFormHome,
                desiredSourceSha = desired,
                hooks =
                    VexLifeProdSetupProcess.FrontDoorActivationHooks(
                        resolveControl = {
                            sourceSha,
                            controlRef,
                            ->
                            assertEquals(
                                desired,
                                sourceSha,
                            )
                            VexLifeProdMacFrontDoor.ResolvedControl(
                                observedAt = "2026-11-13T00:00:00Z",
                                semanticStateRef =
                                    VexLifeProdEntryFamily.StateHomeFormationRequired,
                                controlRef = controlRef,
                                effect =
                                    VexLifeProdEntryFamily.FormCanonicalHome,
                            )
                        },
                        formHome = {
                            sourceSha ->
                            assertEquals(
                                desired,
                                sourceSha,
                            )
                            homeCalls +=
                                1
                        },
                        convergeProduct = {
                            _,
                            _,
                            ->
                            setupCalls +=
                                1
                        },
                        observeSemanticStateRef = { _ ->
                            VexLifeProdEntryFamily.StateSetupRequired
                        },
                    ),
            )

        assertEquals(
            1,
            homeCalls,
        )
        assertEquals(
            0,
            setupCalls,
        )
        assertEquals(
            VexLifeProdSetupProcess.ManifestationRefreshEntry,
            result.manifestationRef,
        )
        assertEquals(
            VexLifeProdEntryFamily.StateSetupRequired,
            result.semanticStateRef,
        )
    }

    @Test
    fun frontDoorSetupActivationCarriesAcquisitionAuthorityAndOpensOnlyAfterCurrentReobservation() {
        val desired =
            "b".repeat(
                40,
            )
        var observedAllowAcquisition: Boolean? =
            null

        val result =
            VexLifeProdSetupProcess.activateFrontDoorWithHooks(
                controlRef =
                    VexLifeProdEntryFamily.ControlSetUp,
                desiredSourceSha = desired,
                hooks =
                    VexLifeProdSetupProcess.FrontDoorActivationHooks(
                        resolveControl = {
                            _,
                            controlRef,
                            ->
                            VexLifeProdMacFrontDoor.ResolvedControl(
                                observedAt = "2026-11-13T00:00:00Z",
                                semanticStateRef =
                                    VexLifeProdEntryFamily.StateSetupRequired,
                                controlRef = controlRef,
                                effect =
                                    VexLifeProdEntryFamily.ConvergeProduct(
                                        desiredSourceSha = desired,
                                        allowAcquisition = true,
                                    ),
                            )
                        },
                        formHome = {
                            error(
                                "setup activation must not form Home",
                            )
                        },
                        convergeProduct = {
                            sourceSha,
                            allowAcquisition,
                            ->
                            assertEquals(
                                desired,
                                sourceSha,
                            )
                            observedAllowAcquisition =
                                allowAcquisition
                        },
                        observeSemanticStateRef = { _ ->
                            VexLifeProdEntryFamily.StateCurrent
                        },
                    ),
            )

        assertEquals(
            true,
            observedAllowAcquisition,
        )
        assertEquals(
            VexLifeProdSetupProcess.ManifestationOpenProduct,
            result.manifestationRef,
        )
        assertEquals(
            VexLifeProdEntryFamily.StateCurrent,
            result.semanticStateRef,
        )
    }

    @Test
    fun frontDoorOpenCurrentReturnsPresentationWithoutSetupRealization() {
        val desired =
            "c".repeat(
                40,
            )

        val result =
            VexLifeProdSetupProcess.activateFrontDoorWithHooks(
                controlRef =
                    VexLifeProdEntryFamily.ControlOpenCurrent,
                desiredSourceSha = desired,
                hooks =
                    VexLifeProdSetupProcess.FrontDoorActivationHooks(
                        resolveControl = {
                            _,
                            controlRef,
                            ->
                            VexLifeProdMacFrontDoor.ResolvedControl(
                                observedAt = "2026-11-13T00:00:00Z",
                                semanticStateRef =
                                    VexLifeProdEntryFamily.StateCurrent,
                                controlRef = controlRef,
                                effect =
                                    VexLifeProdEntryFamily.OpenPresentation,
                            )
                        },
                        formHome = {
                            error(
                                "open-current activation must not form Home",
                            )
                        },
                        convergeProduct = {
                            _,
                            _,
                            ->
                            error(
                                "open-current activation must not run setup",
                            )
                        },
                        observeSemanticStateRef = {
                            error(
                                "open-current activation must not re-observe after a no-effect presentation request",
                            )
                        },
                    ),
            )

        assertEquals(
            VexLifeProdSetupProcess.ManifestationOpenProduct,
            result.manifestationRef,
        )
        assertEquals(
            VexLifeProdEntryFamily.StateCurrent,
            result.semanticStateRef,
        )
    }

    @Test
    fun nativeWindowConsumesManifestationWithoutSemanticEffectRouting() {
        val source =
            Files.readString(
                Path.of(
                    System.getProperty(
                        "user.dir",
                    ),
                    "packaging/private-service-image/setup/vexlife-prod-setup-window.applescript",
                ),
            )

        assertTrue(
            source.contains(
                "--activate-front-door-control",
            ),
        )
        for (
            forbidden in
            listOf(
                "effectRef",
                "FORM_CANONICAL_HOME",
                "CONVERGE_PRODUCT",
                "runHomeFormation",
                "runSetup",
            )
        ) {
            assertFalse(
                source.contains(
                    forbidden,
                ),
                "native window must not own semantic Effect routing: $forbidden",
            )
        }
    }

}

// [VXG RealForever]
