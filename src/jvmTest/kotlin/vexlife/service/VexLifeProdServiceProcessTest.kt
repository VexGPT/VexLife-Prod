package vexlife.service

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

class VexLifeProdServiceProcessTest {
    private fun runtimeEvidence(): HereticModelConfiguration.QualifiedRuntimeEvidence =
        HereticModelConfiguration.QualifiedRuntimeEvidence(
            runtimeReceiptRef = "receipt.vexlife-prod.heretic.service-process-test",
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

    private fun setup(): VexLifeProdLocalSetup.Result {
        val runtime =
            runtimeEvidence()

        return VexLifeProdLocalSetup.Result(
            state = "LOCAL_HERETIC_FOUNDATION_READY",
            productHome = "/tmp/vexlife-prod-service-process-test",
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
    fun processArgumentsCarryExactSourceAndSelectedHomeIdentity() {
        val sha =
            "a".repeat(
                40,
            )

        val parsed =
            VexLifeProdServiceProcess.parse(
                arrayOf(
                    "--source-sha",
                    sha,
                    "--home",
                    "/tmp/vexlife-prod-service-process-test",
                ),
            )

        assertEquals(
            sha,
            parsed.sourceSha,
        )
        assertEquals(
            java.nio.file.Path.of(
                "/tmp/vexlife-prod-service-process-test",
            ),
            parsed.productHome,
        )

        for (
            args in
            listOf(
                emptyArray(),
                arrayOf(
                    "--source-sha",
                ),
                arrayOf(
                    "--port",
                    "18210",
                ),
                arrayOf(
                    "--source-sha",
                    "main",
                ),
                arrayOf(
                    "--source-sha",
                    sha,
                    "--home",
                    "relative-home",
                ),
                arrayOf(
                    "--source-sha",
                    sha,
                    "--endpoint",
                    "http://example.com",
                ),
            )
        ) {
            assertFailsWith<IllegalArgumentException> {
                VexLifeProdServiceProcess.parse(
                    args,
                )
            }
        }
    }

    @Test
    fun serviceProcessReusesExistingLocalFoundationWithoutAcquisitionAuthority() {
        val trace =
            mutableListOf<String>()
        val result =
            setup()
        val session =
            VexLifeProdLocalSetup.Session(
                result = result,
                currentRuntimeEvidence = {
                    result.qualifiedRuntimeEvidence
                },
            )
        val service =
            AutoCloseable {
                trace +=
                    "CLOSE"
            }
        val arguments =
            VexLifeProdServiceProcess.Arguments(
                sourceSha =
                    "b".repeat(
                        40,
                    ),
                productHome =
                    java.nio.file.Path.of(
                        result.productHome,
                    ),
            )

        VexLifeProdServiceProcess.runWithHooks(
            arguments = arguments,
            hooks =
                VexLifeProdServiceProcess.Hooks(
                    resolveVisionRuntimeExecutablePath = {
                        sourceSha ->
                        trace +=
                            "OBSERVE_IMAGE"
                        assertEquals(
                            arguments.sourceSha,
                            sourceSha,
                        )
                        "/tmp/vexlife-prod-service-process-test/vision-runtime/bin/vexlife-vision-runtime"
                    },
                    establishSession = {
                        productHome,
                        allowAcquisition,
                        ->
                        trace +=
                            "ESTABLISH"
                        assertEquals(
                            java.nio.file.Path.of(
                                result.productHome,
                            ),
                            productHome,
                        )
                        assertFalse(
                            allowAcquisition,
                            "long-lived service process must never grant acquisition authority",
                        )
                        session
                    },
                    startService = {
                        sourceSha,
                        currentSession,
                        visionRuntimeExecutablePathOrNull,
                        ->
                        trace +=
                            "START"
                        assertEquals(
                            arguments.sourceSha,
                            sourceSha,
                        )
                        assertSame(
                            session,
                            currentSession,
                        )
                        assertEquals(
                            "/tmp/vexlife-prod-service-process-test/vision-runtime/bin/vexlife-vision-runtime",
                            visionRuntimeExecutablePathOrNull,
                        )
                        service
                    },
                    reportReady = {
                        ready ->
                        trace +=
                            "READY"
                        assertEquals(
                            arguments,
                            ready,
                        )
                    },
                    awaitTermination = {
                        running ->
                        trace +=
                            "AWAIT"
                        assertSame(
                            service,
                            running,
                        )
                        running.close()
                    },
                ),
        )

        assertEquals(
            listOf(
                "OBSERVE_IMAGE",
                "ESTABLISH",
                "START",
                "READY",
                "AWAIT",
                "CLOSE",
            ),
            trace,
        )
    }
}

// [VXG RealForever]
