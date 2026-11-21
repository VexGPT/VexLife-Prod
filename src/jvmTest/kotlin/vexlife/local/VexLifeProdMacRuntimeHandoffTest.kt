package vexlife.local

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vexlife.model.heretic.HereticModelConfiguration
import vexlife.model.heretic.HereticProductIdentity
import vexlife.model.heretic.HereticRuntimeEffectBoundary
import vexlife.model.heretic.HereticRuntimeQualification
import vexlife.model.heretic.HereticRuntimeState
import vexlife.model.heretic.runtime.FileHereticRuntimeReceiptStore
import vexlife.model.heretic.runtime.HereticPersistentRuntime
import vexlife.platform.macos.MacUserLaunchdProcess
import vexlife.service.VexLifeProdMacServiceLifetime
import vexlife.service.VexLifeProdServiceContract

class VexLifeProdMacRuntimeHandoffTest {
    private fun runtimeReceipt(
        productHome: java.nio.file.Path,
        pid: Long = 5151L,
    ): HereticPersistentRuntime.Receipt {
        val python =
            productHome.resolve(
                "runtime/heretic/python/bin/python",
            )
        val environment =
            requireNotNull(
                python.parent,
            )
                .parent
        val model =
            productHome.resolve(
                "models/heretic/test-model",
            )
        val substrate =
            MacUserLaunchdProcess.Receipt(
                schemaVersion = MacUserLaunchdProcess.Schema,
                substrateRef = MacUserLaunchdProcess.SubstrateRef,
                label = "com.vexlife.prod.heretic.handoff-test",
                domainTarget = "gui/501",
                plistPath =
                    productHome.resolve(
                        "runtime/heretic/launchd/handoff-test.plist",
                    )
                        .toString(),
                programPath = python.toString(),
                programArguments =
                    listOf(
                        "-m",
                        "mlx_lm.server",
                    ),
                workingDirectory = environment.toString(),
                stdoutPath =
                    productHome.resolve(
                        "runtime/heretic/handoff-test.out.log",
                    )
                        .toString(),
                stderrPath =
                    productHome.resolve(
                        "runtime/heretic/handoff-test.err.log",
                    )
                        .toString(),
                environment = emptyMap(),
                keepAlive = false,
                pid = pid,
            )

        return HereticPersistentRuntime.Receipt(
            schemaVersion = HereticPersistentRuntime.ReceiptSchema,
            receiptRef = "receipt.vexlife-prod.heretic-persistent-runtime.handoff-test",
            state = HereticRuntimeState.Qualified,
            formedAt = "2026-11-21T19:00:00Z",
            authorityRef = "authority.vexlife-prod.heretic-runtime.handoff-test",
            modelRef = HereticProductIdentity.ModelRef,
            modelProfileRef = HereticProductIdentity.ModelProfileRef,
            artifactRef = HereticProductIdentity.ArtifactSetRef,
            artifactCustodyEvidenceRef = HereticProductIdentity.ArtifactCustodyEvidenceRef,
            artifactContentSetSha256 = HereticProductIdentity.ArtifactContentSetSha256,
            runtimeAdapterRef = HereticProductIdentity.RuntimeAdapterRef,
            runtimeClass = HereticProductIdentity.RuntimeClass,
            transportClass = HereticProductIdentity.TransportClass,
            hostExecutionProfileRef = HereticProductIdentity.HostExecutionProfileRef,
            pythonVersion = HereticProductIdentity.PythonVersion,
            mlxLmVersion = HereticProductIdentity.MlxLmVersion,
            trainerSourceCommit = HereticProductIdentity.TrainerSourceCommit,
            modelPath = model.toString(),
            pythonPath = python.toString(),
            pythonEnvironmentRoot = environment.toString(),
            endpoint = HereticProductIdentity.Endpoint,
            requestModel = HereticProductIdentity.RequestModel,
            implementationIdentity =
                HereticPersistentRuntime.currentImplementationIdentity(),
            runtime =
                HereticPersistentRuntime.RuntimeProcess(
                    pid = pid,
                    processGroupId = pid,
                    exactOwnedDetachedGroup = true,
                    args =
                        listOf(
                            "-m",
                            "mlx_lm.server",
                        ),
                    processSubstrate = substrate,
                ),
            effects = HereticRuntimeEffectBoundary.QualifiedRuntime,
            qualification = HereticRuntimeQualification.qualified(),
        )
    }

    private fun runtimeEvidence(
        receipt: HereticPersistentRuntime.Receipt,
    ): HereticModelConfiguration.QualifiedRuntimeEvidence =
        HereticModelConfiguration.QualifiedRuntimeEvidence(
            runtimeReceiptRef = receipt.receiptRef,
            runtimeState = HereticRuntimeState.Qualified,
            modelRef = receipt.modelRef,
            realizationRef = "realization.vexlife-prod.handoff-test",
            modelProfileRef = receipt.modelProfileRef,
            artifactRef = receipt.artifactRef,
            artifactContentSetSha256 = receipt.artifactContentSetSha256,
            runtimeAdapterRef = receipt.runtimeAdapterRef,
            runtimeClass = receipt.runtimeClass,
            endpoint = receipt.endpoint,
            requestModel = receipt.requestModel,
            qualification = requireNotNull(receipt.qualification),
        )

    private fun serviceReceipt(
        productHome: java.nio.file.Path,
        sourceSha: String = "a".repeat(40),
        pid: Long = 7171L,
    ): VexLifeProdMacServiceLifetime.Receipt {
        val launcher =
            productHome.resolve(
                "runtime/product-service/test-java",
            )
        return VexLifeProdMacServiceLifetime.Receipt(
            schemaVersion = VexLifeProdMacServiceLifetime.ReceiptSchema,
            sourceSha = sourceSha,
            imagePayloadSha256 = "b".repeat(64),
            imagePath =
                productHome.resolve(
                    "runtime/product-service/images/$sourceSha",
                )
                    .toString(),
            serviceOrigin = VexLifeProdServiceContract.Origin,
            launchd =
                MacUserLaunchdProcess.Receipt(
                    schemaVersion = MacUserLaunchdProcess.Schema,
                    substrateRef = MacUserLaunchdProcess.SubstrateRef,
                    label = VexLifeProdMacServiceLifetime.Label,
                    domainTarget = "gui/501",
                    plistPath =
                        productHome.resolve(
                            "runtime/product-service/launchd/test.plist",
                        )
                            .toString(),
                    programPath = launcher.toString(),
                    programArguments =
                        listOf(
                            "--source-sha",
                            sourceSha,
                        ),
                    workingDirectory = productHome.toString(),
                    stdoutPath =
                        productHome.resolve(
                            "runtime/product-service/logs/test.out.log",
                        )
                            .toString(),
                    stderrPath =
                        productHome.resolve(
                            "runtime/product-service/logs/test.err.log",
                        )
                            .toString(),
                    environment = emptyMap(),
                    keepAlive = false,
                    pid = pid,
                ),
            homeBinding =
                VexLifeProdMacServiceLifetime.HomeBinding(
                    homePath = productHome.toString(),
                    homeRef = "home.vexlife-prod.handoff-test",
                    deviceRef = "device.vexlife-prod.handoff-test",
                    companionLineageRef = "lineage.vexlife-prod.handoff-test",
                ),
        )
    }

    @Test
    fun exactLiveTupleConsumesOnceWithoutDeletingRuntimeReceipt() {
        val home =
            Files.createTempDirectory(
                "vexlife-prod-runtime-handoff-",
            )
                .toAbsolutePath()
        try {
            val runtime =
                runtimeReceipt(
                    home,
                )
            val runtimeStore =
                FileHereticRuntimeReceiptStore(
                    home,
                )
            runtimeStore.write(
                runtime,
            )
            val service =
                serviceReceipt(
                    home,
                )
            val live =
                setOf(
                    runtime.runtime.pid,
                    service.launchd.pid,
                    6161L,
                )

            val prepared =
                VexLifeProdMacRuntimeHandoff.prepare(
                    productHome = home,
                    priorService = service,
                    expectedRuntime = runtimeEvidence(runtime),
                    setupPid = 6161L,
                    nowMs = 1_000L,
                    processAlive = live::contains,
                )

            assertEquals(
                "PREPARED",
                prepared.state,
            )
            assertTrue(
                prepared.effectsPerformed,
            )
            assertNotNull(
                prepared.receipt,
            )
            assertTrue(
                VexLifeProdMacRuntimeHandoff.consumeForServiceClose(
                    productHome = home,
                    serviceSourceSha = service.sourceSha,
                    servicePid = service.launchd.pid,
                    nowMs = 1_001L,
                    processAlive = live::contains,
                ),
            )
            assertFalse(
                VexLifeProdMacRuntimeHandoff.consumeForServiceClose(
                    productHome = home,
                    serviceSourceSha = service.sourceSha,
                    servicePid = service.launchd.pid,
                    nowMs = 1_002L,
                    processAlive = live::contains,
                ),
            )
            assertEquals(
                runtime,
                runtimeStore.read(),
                "handoff consumption relinquishes service release duty; it does not retire runtime",
            )
        } finally {
            home.toFile().deleteRecursively()
        }
    }

    @Test
    fun deadSetupOrExpiredHandoffFallsBackWithoutConsumingRuntimeCustody() {
        val home =
            Files.createTempDirectory(
                "vexlife-prod-runtime-handoff-held-",
            )
                .toAbsolutePath()
        try {
            val runtime =
                runtimeReceipt(
                    home,
                )
            FileHereticRuntimeReceiptStore(
                home,
            )
                .write(
                    runtime,
                )
            val service =
                serviceReceipt(
                    home,
                )
            val liveAtPrepare =
                setOf(
                    runtime.runtime.pid,
                    service.launchd.pid,
                    6161L,
                )
            assertEquals(
                "PREPARED",
                VexLifeProdMacRuntimeHandoff.prepare(
                    productHome = home,
                    priorService = service,
                    expectedRuntime = runtimeEvidence(runtime),
                    setupPid = 6161L,
                    nowMs = 2_000L,
                    processAlive = liveAtPrepare::contains,
                )
                    .state,
            )

            val liveWithoutSetup =
                setOf(
                    runtime.runtime.pid,
                    service.launchd.pid,
                )
            assertFalse(
                VexLifeProdMacRuntimeHandoff.consumeForServiceClose(
                    productHome = home,
                    serviceSourceSha = service.sourceSha,
                    servicePid = service.launchd.pid,
                    nowMs = 2_001L,
                    processAlive = liveWithoutSetup::contains,
                ),
            )

            assertEquals(
                "PREPARED",
                VexLifeProdMacRuntimeHandoff.prepare(
                    productHome = home,
                    priorService = service,
                    expectedRuntime = runtimeEvidence(runtime),
                    setupPid = 6161L,
                    nowMs = 3_000L,
                    processAlive = liveAtPrepare::contains,
                )
                    .state,
            )
            assertFalse(
                VexLifeProdMacRuntimeHandoff.consumeForServiceClose(
                    productHome = home,
                    serviceSourceSha = service.sourceSha,
                    servicePid = service.launchd.pid,
                    nowMs =
                        3_000L +
                            VexLifeProdMacRuntimeHandoff.LifetimeMs +
                            1,
                    processAlive = liveAtPrepare::contains,
                ),
            )
        } finally {
            home.toFile().deleteRecursively()
        }
    }

    @Test
    fun runtimeReceiptDriftCannotRelinquishOldServiceReleaseDuty() {
        val home =
            Files.createTempDirectory(
                "vexlife-prod-runtime-handoff-drift-",
            )
                .toAbsolutePath()
        try {
            val runtime =
                runtimeReceipt(
                    home,
                )
            val store =
                FileHereticRuntimeReceiptStore(
                    home,
                )
            store.write(
                runtime,
            )
            val service =
                serviceReceipt(
                    home,
                )
            val live =
                setOf(
                    runtime.runtime.pid,
                    service.launchd.pid,
                    6161L,
                )
            assertEquals(
                "PREPARED",
                VexLifeProdMacRuntimeHandoff.prepare(
                    productHome = home,
                    priorService = service,
                    expectedRuntime = runtimeEvidence(runtime),
                    setupPid = 6161L,
                    nowMs = 4_000L,
                    processAlive = live::contains,
                )
                    .state,
            )
            store.write(
                runtime.copy(
                    receiptRef =
                        "receipt.vexlife-prod.heretic-persistent-runtime.changed",
                ),
            )

            assertFalse(
                VexLifeProdMacRuntimeHandoff.consumeForServiceClose(
                    productHome = home,
                    serviceSourceSha = service.sourceSha,
                    servicePid = service.launchd.pid,
                    nowMs = 4_001L,
                    processAlive = live::contains,
                ),
            )
            VexLifeProdMacRuntimeHandoff.clear(
                home,
            )
            assertNull(
                VexDurableContractFile.read<VexLifeProdMacRuntimeHandoff.Receipt>(
                    ownedRoot = home,
                    path =
                        home.resolve(
                            VexLifeProdMacRuntimeHandoff.RelativePath,
                        ),
                ),
            )
        } finally {
            home.toFile().deleteRecursively()
        }
    }
}

// [VXG RealForever]
