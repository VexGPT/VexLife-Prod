package vexlife.setup

import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
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
    fun runWithHooksStillRefusesImplicitAcquisitionAuthority() {
        var effects =
            0

        assertFailsWith<IllegalArgumentException> {
            VexLifeProdSetupProcess.runWithHooks(
                arguments =
                    VexLifeProdSetupProcess.Arguments(
                        expectedSourceSha =
                            "a".repeat(
                                40,
                            ),
                        allowAcquisition = false,
                    ),
                hooks =
                    VexLifeProdSetupProcess.Hooks(
                        locateImageRoot = {
                            effects +=
                                1
                            Path.of(
                                "/tmp/should-not-run",
                            )
                        },
                    ),
            )
        }

        assertEquals(
            0,
            effects,
        )
        assertFalse(
            effects >
                0,
        )
    }
}

// [VXG RealForever]
