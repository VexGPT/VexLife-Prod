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
import vexlife.packaging.VexLifeProdPrivateServiceImageRetention
import vexlife.service.VexLifeProdMacJavaLifetime
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

    private fun setupResult(productHome: String): VexLifeProdLocalSetup.Result {
        val runtime = runtimeEvidence()

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
                )
            )

        assertEquals(home, parsed.productHome)
        assertEquals(sha, parsed.expectedSourceSha)
        assertEquals(true, parsed.allowAcquisition)

        val reuseExisting =
            VexLifeProdSetupProcess.parse(
                arrayOf("--home", home.toString(), "--expected-source-sha", sha, "--reuse-existing")
            )
        assertEquals(home, reuseExisting.productHome)
        assertEquals(false, reuseExisting.allowAcquisition)

        for (args in
            listOf(
                emptyArray(),
                arrayOf("--expected-source-sha", sha, "--allow-acquisition"),
                arrayOf(
                    "--home",
                    "relative-home",
                    "--expected-source-sha",
                    sha,
                    "--allow-acquisition",
                ),
                arrayOf(
                    "--home",
                    home.toString(),
                    "--expected-source-sha",
                    "main",
                    "--allow-acquisition",
                ),
                arrayOf(
                    "--home",
                    home.toString(),
                    "--expected-source-sha",
                    sha,
                    "--repair-anything",
                ),
            )) {
            assertFailsWith<IllegalArgumentException> {
                VexLifeProdSetupProcess.parse(args)
            }
        }
    }

    @Test
    fun HomeFormationVerifiesImageAndStopsBeforeAcquisitionOrProductEffects() {
        val trace = mutableListOf<String>()
        val sourceSha = "a".repeat(40)
        val sourceImage = Path.of("/tmp/vexlife-prod-home-formation-source-image")
        val observation =
            VexLifeProdPrivateServiceImage.Observation(
                imageRef = "image.vexlife-prod.private-service.home-test",
                sourceSha = sourceSha,
                payloadSha256 = "b".repeat(64),
                launcherPath = sourceImage.resolve("bin/vexlife-prod-service").toString(),
                setupLauncherPath = sourceImage.resolve("bin/vexlife-prod-setup").toString(),
                setupWindowPath =
                    sourceImage.resolve("setup/vexlife-prod-setup-window.applescript").toString(),
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
                )
            )
        assertEquals(sourceSha, parsed.expectedSourceSha)

        VexLifeProdSetupProcess.formHomeWithHooks(
            arguments = parsed,
            hooks =
                VexLifeProdSetupProcess.Hooks(
                    locateImageRoot = {
                        trace += "LOCATE"
                        sourceImage
                    },
                    observeImage = { imageRoot, expected ->
                        trace += "VERIFY_IMAGE"
                        assertEquals(sourceImage, imageRoot)
                        assertEquals(sourceSha, expected)
                        observation
                    },
                    formFreshHome = { selectedHome ->
                        trace += "FORM_HOME"
                        assertEquals(Path.of(home.productHome), selectedHome)
                        home
                    },
                    establishSession = { _, _, _ ->
                        error("Home-only realization must not enter model/runtime setup")
                    },
                    installImage = { _, _, _ ->
                        error("Home-only realization must not install Product image")
                    },
                    startService = { _, _, _ ->
                        error("Home-only realization must not start Product service")
                    },
                    reportImageVerified = {
                        trace += "REPORT_IMAGE"
                    },
                    reportHomeReady = { arguments, formed ->
                        trace += "HOME_READY"
                        assertEquals(sourceSha, arguments.expectedSourceSha)
                        assertSame(home, formed)
                    },
                ),
        )

        assertEquals(
            listOf("LOCATE", "VERIFY_IMAGE", "REPORT_IMAGE", "FORM_HOME", "HOME_READY"),
            trace,
        )
    }

    @Test
    fun imageVerificationPrecedesAuthorizedSetupAndInstalledImagePrecedesReady() {
        val trace = mutableListOf<String>()
        val sourceSha = "a".repeat(40)
        val sourceImage = Path.of("/tmp/vexlife-prod-setup-source-image")
        val productHome = "/tmp/VexLife-Prod"
        val payloadSha = "b".repeat(64)
        val observation =
            VexLifeProdPrivateServiceImage.Observation(
                imageRef = "image.vexlife-prod.private-service.test",
                sourceSha = sourceSha,
                payloadSha256 = payloadSha,
                launcherPath = sourceImage.resolve("bin/vexlife-prod-service").toString(),
                setupLauncherPath = sourceImage.resolve("bin/vexlife-prod-setup").toString(),
                setupWindowPath =
                    sourceImage.resolve("setup/vexlife-prod-setup-window.applescript").toString(),
                javaPath = sourceImage.resolve("runtime/bin/java").toString(),
                runtimeModules = listOf("java.base", "java.net.http", "jdk.httpserver"),
                effectsPerformed = false,
            )
        val setup = setupResult(productHome)
        val session =
            VexLifeProdLocalSetup.Session(
                result = setup,
                currentRuntimeEvidence = {
                    setup.qualifiedRuntimeEvidence
                },
                releaseRuntime = {
                    error("setup fixture must not close runtime")
                },
            )
        val installed =
            VexLifeProdPrivateServiceImageInstaller.Result(
                state = "INSTALLED_VERIFIED_PRIVATE_SERVICE_IMAGE",
                sourceSha = sourceSha,
                payloadSha256 = payloadSha,
                imagePath = "$productHome/runtime/product-service/images/$sourceSha",
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
                javaProviderSourceSha = sourceSha,
            )
        val javaBinding =
            VexLifeProdMacJavaLifetime.Binding(
                receipt =
                    VexLifeProdMacJavaLifetime.Receipt(
                        providerSourceSha = sourceSha,
                        providerImagePayloadSha256 = payloadSha,
                        runtimeFingerprintSha256 = "c".repeat(64),
                        javaRuntimeMajor = VexLifeProdPrivateServiceImage.JavaRuntimeMajor,
                        runtimeModules = observation.runtimeModules,
                        providerImagePath = "/tmp/vexlife-provider-image",
                        runtimePath = "/tmp/vexlife-stable-java",
                        javaPath = "/tmp/vexlife-stable-java/bin/java",
                    ),
                javaPath = "/tmp/vexlife-stable-java/bin/java",
                providerSourceSha = sourceSha,
                effectsPerformed = true,
            )

        VexLifeProdSetupProcess.runWithHooks(
            arguments =
                VexLifeProdSetupProcess.Arguments(
                    productHome = Path.of(productHome),
                    expectedSourceSha = sourceSha,
                    allowAcquisition = true,
                ),
            hooks =
                VexLifeProdSetupProcess.Hooks(
                    locateImageRoot = {
                        trace += "LOCATE"
                        sourceImage
                    },
                    observeImage = { imageRoot, expected ->
                        trace += "VERIFY_IMAGE"
                        assertEquals(sourceImage, imageRoot)
                        assertEquals(sourceSha, expected)
                        observation
                    },
                    establishSession = { selectedHome, allowAcquisition, _ ->
                        assertEquals(Path.of(productHome), selectedHome)
                        trace += "SETUP"
                        assertEquals(true, allowAcquisition)
                        session
                    },
                    installImage = { imageRoot, home, expected ->
                        trace += "INSTALL"
                        assertEquals(sourceImage, imageRoot)
                        assertEquals(Path.of(productHome), home)
                        assertEquals(sourceSha, expected)
                        installed
                    },
                    convergeJavaRuntime = { home, providerSource, requiredImage ->
                        trace += "JAVA"
                        assertEquals(Path.of(productHome), home)
                        assertEquals(sourceSha, providerSource)
                        assertSame(observation, requiredImage)
                        javaBinding
                    },
                    startService = { home, expected, _ ->
                        trace += "SERVICE"
                        assertEquals(Path.of(productHome), home)
                        assertEquals(sourceSha, expected)
                        service
                    },
                    reclaimRetiredImages = { home, currentSource, currentPayload, javaProviderSource ->
                        trace += "RECLAIM"
                        assertEquals(Path.of(productHome), home)
                        assertEquals(sourceSha, currentSource)
                        assertEquals(payloadSha, currentPayload)
                        assertEquals(sourceSha, javaProviderSource)
                        VexLifeProdPrivateServiceImageRetention.Result(
                            currentSourceSha = currentSource,
                            currentPayloadSha256 = currentPayload,
                            runtimeModules = observation.runtimeModules,
                            entries =
                                listOf(
                                    VexLifeProdPrivateServiceImageRetention.Entry(
                                        name = currentSource,
                                        sourceSha = currentSource,
                                        disposition =
                                            VexLifeProdPrivateServiceImageRetention
                                                .EntryDisposition
                                                .RetainedCurrent,
                                        bytes = 1L,
                                    )
                                ),
                            reclaimedBytes = 0L,
                            effectsPerformed = false,
                        )
                    },
                    reportImageVerified = { value ->
                        trace += "REPORT_IMAGE"
                        assertSame(observation, value)
                    },
                    reportReady = { arguments, currentSession, currentInstalled, currentService ->
                        trace += "READY"
                        assertEquals(sourceSha, arguments.expectedSourceSha)
                        assertSame(session, currentSession)
                        assertSame(installed, currentInstalled)
                        assertSame(service, currentService)
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
                "JAVA",
                "SERVICE",
                "RECLAIM",
                "READY",
            ),
            trace,
        )
    }

    @Test
    fun priorServiceRetirementReestablishesRuntimeBeforeReplacementServiceContinues() {
        val trace = mutableListOf<String>()
        val sourceSha = "a".repeat(40)
        val sourceImage = Path.of("/tmp/vexlife-prod-setup-runtime-rotation-source-image")
        val productHome = "/tmp/vexlife-prod-setup-runtime-rotation-home"
        val payloadSha = "b".repeat(64)
        val observation =
            VexLifeProdPrivateServiceImage.Observation(
                imageRef = "image.vexlife-prod.private-service.runtime-rotation-test",
                sourceSha = sourceSha,
                payloadSha256 = payloadSha,
                launcherPath = sourceImage.resolve("bin/vexlife-prod-service").toString(),
                setupLauncherPath = sourceImage.resolve("bin/vexlife-prod-setup").toString(),
                setupWindowPath =
                    sourceImage.resolve("setup/vexlife-prod-setup-window.applescript").toString(),
                javaPath = sourceImage.resolve("runtime/bin/java").toString(),
                runtimeModules = listOf("java.base", "java.net.http", "jdk.httpserver"),
                effectsPerformed = false,
            )
        val setup = setupResult(productHome)
        val firstSession =
            VexLifeProdLocalSetup.Session(
                result = setup,
                currentRuntimeEvidence = {
                    setup.qualifiedRuntimeEvidence
                },
                releaseRuntime = {},
            )
        val replacementSession =
            VexLifeProdLocalSetup.Session(
                result = setup,
                currentRuntimeEvidence = {
                    setup.qualifiedRuntimeEvidence
                },
                releaseRuntime = {},
            )
        var sessionFormationCount = 0
        val installed =
            VexLifeProdPrivateServiceImageInstaller.Result(
                state = "INSTALLED_VERIFIED_PRIVATE_SERVICE_IMAGE",
                sourceSha = sourceSha,
                payloadSha256 = payloadSha,
                imagePath = "$productHome/runtime/product-service/images/$sourceSha",
                imageMutationPerformed = true,
            )
        val javaBinding =
            VexLifeProdMacJavaLifetime.Binding(
                receipt =
                    VexLifeProdMacJavaLifetime.Receipt(
                        providerSourceSha = sourceSha,
                        providerImagePayloadSha256 = payloadSha,
                        runtimeFingerprintSha256 = "c".repeat(64),
                        javaRuntimeMajor = VexLifeProdPrivateServiceImage.JavaRuntimeMajor,
                        runtimeModules = observation.runtimeModules,
                        providerImagePath = "/tmp/vexlife-provider-image",
                        runtimePath = "/tmp/vexlife-stable-java",
                        javaPath = "/tmp/vexlife-stable-java/bin/java",
                    ),
                javaPath = "/tmp/vexlife-stable-java/bin/java",
                providerSourceSha = sourceSha,
                effectsPerformed = false,
            )
        val service =
            VexLifeProdMacServiceLifetime.Result(
                disposition = "STARTED_CURRENT_SERVICE",
                sourceSha = sourceSha,
                imagePayloadSha256 = payloadSha,
                pid = 5252L,
                serviceOrigin = "http://127.0.0.1:18210",
                processStarted = true,
                priorProcessRetired = true,
                receiptWritePerformed = true,
                javaProviderSourceSha = sourceSha,
            )

        VexLifeProdSetupProcess.runWithHooks(
            arguments =
                VexLifeProdSetupProcess.Arguments(
                    productHome = Path.of(productHome),
                    expectedSourceSha = sourceSha,
                    allowAcquisition = false,
                    allowRuntimeStart = true,
                ),
            hooks =
                VexLifeProdSetupProcess.Hooks(
                    locateImageRoot = {
                        sourceImage
                    },
                    observeImage = { _, _ ->
                        observation
                    },
                    establishSession = { selectedHome, allowAcquisition, allowRuntimeStart ->
                        assertEquals(
                            Path.of(productHome),
                            selectedHome,
                        )
                        assertFalse(
                            allowAcquisition,
                        )
                        assertEquals(
                            true,
                            allowRuntimeStart,
                        )
                        sessionFormationCount +=
                            1
                        trace +=
                            "SESSION_$sessionFormationCount"
                        if (
                            sessionFormationCount ==
                            1
                        ) {
                            firstSession
                        } else {
                            replacementSession
                        }
                    },
                    installImage = { _, _, _ ->
                        trace +=
                            "INSTALL"
                        installed
                    },
                    convergeJavaRuntime = { _, _, _ ->
                        trace +=
                            "JAVA"
                        javaBinding
                    },
                    startService = { _, _, afterPriorRetired ->
                        trace +=
                            "PRIOR_SERVICE_RETIRED"
                        afterPriorRetired()
                        trace +=
                            "REPLACEMENT_SERVICE_START"
                        service
                    },
                    reclaimRetiredImages = { _, _, _, _ ->
                        trace +=
                            "RECLAIM"
                        VexLifeProdPrivateServiceImageRetention.Result(
                            currentSourceSha = sourceSha,
                            currentPayloadSha256 = payloadSha,
                            runtimeModules = observation.runtimeModules,
                            entries =
                                listOf(
                                    VexLifeProdPrivateServiceImageRetention.Entry(
                                        name = sourceSha,
                                        sourceSha = sourceSha,
                                        disposition =
                                            VexLifeProdPrivateServiceImageRetention
                                                .EntryDisposition
                                                .RetainedCurrent,
                                        bytes = 1L,
                                    ),
                                ),
                            reclaimedBytes = 0L,
                            effectsPerformed = false,
                        )
                    },
                    reportImageVerified = {},
                    reportReady = { _, currentSession, _, _ ->
                        trace +=
                            "READY"
                        assertSame(
                            replacementSession,
                            currentSession,
                            "replacement service must receive the runtime Session re-established after prior-service retirement",
                        )
                    },
                ),
        )

        assertEquals(
            listOf(
                "SESSION_1",
                "INSTALL",
                "JAVA",
                "PRIOR_SERVICE_RETIRED",
                "SESSION_2",
                "REPLACEMENT_SERVICE_START",
                "RECLAIM",
                "READY",
            ),
            trace,
        )
        assertEquals(
            2,
            sessionFormationCount,
        )
    }

    @Test
    fun failedServiceStartReleasesSetupRuntimeBeforeServiceCustodyTransfer() {
        val sourceSha = "a".repeat(40)
        val sourceImage = Path.of("/tmp/vexlife-prod-setup-service-failure-source-image")
        val productHome = "/tmp/vexlife-prod-setup-service-failure-home"
        val payloadSha = "b".repeat(64)
        val observation =
            VexLifeProdPrivateServiceImage.Observation(
                imageRef = "image.vexlife-prod.private-service.service-failure-test",
                sourceSha = sourceSha,
                payloadSha256 = payloadSha,
                launcherPath = sourceImage.resolve("bin/vexlife-prod-service").toString(),
                setupLauncherPath = sourceImage.resolve("bin/vexlife-prod-setup").toString(),
                setupWindowPath =
                    sourceImage.resolve("setup/vexlife-prod-setup-window.applescript").toString(),
                javaPath = sourceImage.resolve("runtime/bin/java").toString(),
                runtimeModules = listOf("java.base", "java.net.http", "jdk.httpserver"),
                effectsPerformed = false,
            )
        val setup = setupResult(productHome)
        var runtimeReleaseCount = 0
        val session =
            VexLifeProdLocalSetup.Session(
                result = setup,
                currentRuntimeEvidence = {
                    setup.qualifiedRuntimeEvidence
                },
                releaseRuntime = {
                    runtimeReleaseCount += 1
                },
            )
        val installed =
            VexLifeProdPrivateServiceImageInstaller.Result(
                state = "INSTALLED_VERIFIED_PRIVATE_SERVICE_IMAGE",
                sourceSha = sourceSha,
                payloadSha256 = payloadSha,
                imagePath = "$productHome/runtime/product-service/images/$sourceSha",
                imageMutationPerformed = true,
            )
        val javaBinding =
            VexLifeProdMacJavaLifetime.Binding(
                receipt =
                    VexLifeProdMacJavaLifetime.Receipt(
                        providerSourceSha = sourceSha,
                        providerImagePayloadSha256 = payloadSha,
                        runtimeFingerprintSha256 = "c".repeat(64),
                        javaRuntimeMajor = VexLifeProdPrivateServiceImage.JavaRuntimeMajor,
                        runtimeModules = observation.runtimeModules,
                        providerImagePath = "/tmp/vexlife-provider-image",
                        runtimePath = "/tmp/vexlife-stable-java",
                        javaPath = "/tmp/vexlife-stable-java/bin/java",
                    ),
                javaPath = "/tmp/vexlife-stable-java/bin/java",
                providerSourceSha = sourceSha,
                effectsPerformed = false,
            )

        val failure =
            assertFailsWith<IllegalStateException> {
                VexLifeProdSetupProcess.runWithHooks(
                    arguments =
                        VexLifeProdSetupProcess.Arguments(
                            productHome = Path.of(productHome),
                            expectedSourceSha = sourceSha,
                            allowAcquisition = true,
                        ),
                    hooks =
                        VexLifeProdSetupProcess.Hooks(
                            locateImageRoot = {
                                sourceImage
                            },
                            observeImage = { _, _ ->
                                observation
                            },
                            establishSession = { _, _, _ ->
                                session
                            },
                            installImage = { _, _, _ ->
                                installed
                            },
                            convergeJavaRuntime = { _, _, _ ->
                                javaBinding
                            },
                            startService = { _, _, _ ->
                                throw IllegalStateException(
                                    "service process did not become source/PID-current before timeout",
                                )
                            },
                            reportImageVerified = {},
                        ),
                )
            }

        assertEquals(
            "service process did not become source/PID-current before timeout",
            failure.message,
        )
        assertEquals(
            1,
            runtimeReleaseCount,
            "setup retains runtime custody until one CURRENT service has successfully returned",
        )
    }

    @Test
    fun reuseExistingModeCarriesNoAcquisitionAuthorityIntoLocalSetup() {
        val sourceSha = "a".repeat(40)
        val sourceImage = Path.of("/tmp/vexlife-prod-setup-reuse-existing")
        val observation =
            VexLifeProdPrivateServiceImage.Observation(
                imageRef = "image.vexlife-prod.private-service.reuse-test",
                sourceSha = sourceSha,
                payloadSha256 = "b".repeat(64),
                launcherPath = sourceImage.resolve("bin/vexlife-prod-service").toString(),
                setupLauncherPath = sourceImage.resolve("bin/vexlife-prod-setup").toString(),
                setupWindowPath =
                    sourceImage.resolve("setup/vexlife-prod-setup-window.applescript").toString(),
                javaPath = sourceImage.resolve("runtime/bin/java").toString(),
                runtimeModules = listOf("java.base", "java.net.http", "jdk.httpserver"),
                effectsPerformed = false,
            )

        assertFailsWith<IllegalStateException> {
            VexLifeProdSetupProcess.runWithHooks(
                arguments =
                    VexLifeProdSetupProcess.Arguments(
                        productHome = Path.of("/tmp/vexlife-prod-setup-reuse-existing-home"),
                        expectedSourceSha = sourceSha,
                        allowAcquisition = false,
                    ),
                hooks =
                    VexLifeProdSetupProcess.Hooks(
                        locateImageRoot = {
                            sourceImage
                        },
                        observeImage = { _, _ ->
                            observation
                        },
                        reportImageVerified = {},
                        establishSession = { productHome, allowAcquisition, _ ->
                            assertEquals(
                                Path.of("/tmp/vexlife-prod-setup-reuse-existing-home"),
                                productHome,
                            )
                            assertFalse(allowAcquisition)
                            throw IllegalStateException("stop after authority projection")
                        },
                    ),
            )
        }
    }
}

// [VXG RealForever]
