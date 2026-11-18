package vexlife.service

import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import vexlife.local.VexLifeProdRuntimeSession
import vexlife.model.heretic.HereticModelConfiguration
import vexlife.model.heretic.HereticProductIdentity
import vexlife.model.heretic.HereticRuntimeQualification
import vexlife.model.heretic.HereticRuntimeState

class VexLifeProdServiceProcessTest {
    private val productHomePath: Path =
        Path.of(
                System.getProperty(
                    "java.io.tmpdir",
                ),
            )
            .resolve(
                "vexlife-prod-service-process-test",
            )
            .toAbsolutePath()
            .normalize()

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

    private fun session(): VexLifeProdRuntimeSession {
        val evidence =
            runtimeEvidence()
        return object :
            VexLifeProdRuntimeSession {
            override val foundation =
                VexLifeProdRuntimeSession.Foundation(
                    state =
                        VexLifeProdRuntimeSession.ReadyState,
                    productHome =
                        productHomePath.toString(),
                    qualifiedRuntimeEvidence =
                        evidence,
                )

            override fun currentQualifiedRuntimeEvidence(
                onRecovery: (() -> Unit)?,
            ): HereticModelConfiguration.QualifiedRuntimeEvidence =
                evidence

            override fun observeCurrentQualifiedRuntimeEvidence():
                HereticModelConfiguration.QualifiedRuntimeEvidence =
                evidence
        }
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
                    productHomePath.toString(),
                ),
            )

        assertEquals(
            sha,
            parsed.sourceSha,
        )
        assertEquals(
            productHomePath,
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
    fun serviceProcessConsumesPortableSessionWithoutAcquisitionAuthority() {
        val trace =
            mutableListOf<String>()
        val session =
            session()
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
                    productHomePath,
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
                        productHomePath
                            .resolve(
                                "vision-runtime/bin/vexlife-vision-runtime",
                            )
                            .toString()
                    },
                    establishSession = {
                        productHome,
                        allowAcquisition,
                        ->
                        trace +=
                            "ESTABLISH"
                        assertEquals(
                            productHomePath,
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
                            productHomePath
                                .resolve(
                                    "vision-runtime/bin/vexlife-vision-runtime",
                                )
                                .toString(),
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
