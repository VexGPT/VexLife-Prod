package vexlife.setup

import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
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
    fun processArgumentsCarrySelectedHomeExpectedSourceAndExplicitAcquisitionAuthorization() {
        val sha = "a".repeat(40)
        val home = Path.of("/tmp/vexlife-selected-home")

        val parsed =
            VexLifeProdSetupProcess.parse(
                arrayOf(
                    "--home",
                    home.toString(),
                    "--expected-source-sha",
                    sha,
                    "--allow-acquisition",
                ),
            )

        assertEquals(home, parsed.productHome)
        assertEquals(sha, parsed.expectedSourceSha)
        assertEquals(true, parsed.allowAcquisition)

        val reuseExisting =
            VexLifeProdSetupProcess.parse(
                arrayOf(
                    "--home",
                    home.toString(),
                    "--expected-source-sha",
                    sha,
                    "--reuse-existing",
                ),
            )
        assertEquals(home, reuseExisting.productHome)
        assertEquals(false, reuseExisting.allowAcquisition)

        for (
            args in
            listOf(
                emptyArray(),
                arrayOf("--expected-source-sha", sha, "--allow-acquisition"),
                arrayOf("--home", "relative-home", "--expected-source-sha", sha, "--allow-acquisition"),
                arrayOf("--home", home.toString(), "--expected-source-sha", "main", "--allow-acquisition"),
                arrayOf("--home", home.toString(), "--expected-source-sha", sha, "--repair-anything"),
            )
        ) {
            assertFailsWith<IllegalArgumentException> {
                VexLifeProdSetupProcess.parse(args)
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
                    "--home",
                    "/tmp/.vexlife",
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
                        selectedHome ->
                        trace += "FORM_HOME"
                        assertEquals(
                            Path.of(
                                home.productHome,
                            ),
                            selectedHome,
                        )
                        home
                    },
                    establishSession = {
                        _,
                        _,
                        _,
                        ->
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
                    productHome =
                        Path.of(
                            productHome,
                        ),
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
                        selectedHome,
                        allowAcquisition,
                        _,
                        ->
                        assertEquals(
                            Path.of(
                                productHome,
                            ),
                            selectedHome,
                        )
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
                        productHome =
                            Path.of(
                                "/tmp/vexlife-prod-setup-reuse-existing-home",
                            ),
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
                            productHome,
                            allowAcquisition,
                            _,
                            ->
                            assertEquals(
                                Path.of(
                                    "/tmp/vexlife-prod-setup-reuse-existing-home",
                                ),
                                productHome,
                            )
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


}

// [VXG RealForever]
