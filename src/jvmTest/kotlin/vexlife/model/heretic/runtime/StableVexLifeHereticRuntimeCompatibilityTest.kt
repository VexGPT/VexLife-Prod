package vexlife.model.heretic.runtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import vexlife.model.heretic.HereticProductIdentity
import vexlife.model.heretic.HereticRuntimeEffectBoundary
import vexlife.model.heretic.HereticRuntimeQualification
import vexlife.model.heretic.HereticRuntimeState
import vexlife.platform.macos.MacUserLaunchdProcess

class StableVexLifeHereticRuntimeCompatibilityTest {
    private class Platform(
        private val expectedPid: Long,
        private val expectedProcessGroupId: Long,
    ) : HereticPersistentRuntime.Platform {
        val trace =
            mutableListOf<String>()

        override val platform =
            "darwin"

        override val architecture =
            "arm64"

        override fun environmentCurrent(
            binding: HereticPersistentRuntime.Binding,
        ): Boolean {
            trace +=
                "ENVIRONMENT_CURRENT"
            return true
        }

        override fun processAlive(
            pid: Long,
        ): Boolean {
            trace +=
                "PROCESS_ALIVE"
            return pid ==
                expectedPid
        }

        override fun processIdentity(
            pid: Long,
            args: List<String>,
        ): HereticPersistentRuntime.ProcessIdentity? {
            trace +=
                "PROCESS_IDENTITY"
            return HereticPersistentRuntime.ProcessIdentity(
                pid = pid,
                processGroupId = expectedProcessGroupId,
                exactOwnedDetachedGroup = true,
            )
        }

        override fun processSubstrateCurrent(
            receipt: MacUserLaunchdProcess.Receipt,
            expectedPid: Long,
        ): Boolean {
            trace +=
                "SUBSTRATE_CURRENT"
            return receipt.pid ==
                expectedPid
        }

        override fun matchingProcesses(
            args: List<String>,
        ): List<HereticPersistentRuntime.ProcessIdentity> =
            error(
                "borrow observation must not census orphan product processes",
            )

        override fun portOccupied(
            port: Int,
        ): Boolean =
            error(
                "borrow observation must not reduce ownership to port occupancy",
            )

        override fun spawn(
            binding: HereticPersistentRuntime.Binding,
            args: List<String>,
        ): HereticPersistentRuntime.Spawned =
            error(
                "borrow observation must not spawn",
            )

        override fun retire(
            receipt: HereticPersistentRuntime.Receipt,
        ): HereticPersistentRuntime.Retirement =
            error(
                "borrow observation must not retire",
            )

        override fun retireProvisional(
            spawned: HereticPersistentRuntime.Spawned,
            args: List<String>,
        ): HereticPersistentRuntime.Retirement =
            error(
                "borrow observation must not retire provisional runtime",
            )

        override fun waitUntilHealthy(
            binding: HereticPersistentRuntime.Binding,
            pid: Long,
        ) =
            error(
                "borrow observation must not perform startup waiting",
            )

        override fun qualify(
            binding: HereticPersistentRuntime.Binding,
        ): HereticRuntimeQualification =
            error(
                "borrow observation must not perform a new qualification inference",
            )

        override fun now(): String =
            "2026-11-05T19:45:00-08:00"
    }

    private fun binding(): HereticPersistentRuntime.Binding =
        HereticPersistentRuntime.bind(
            authorityRef = "authority.vexlife-prod.heretic-runtime.compatibility-test",
            materialization =
                HereticArtifactMaterialization.Result(
                    state = "HERETIC_ARTIFACTS_MATERIALIZED_EXACT",
                    modelPath = "/tmp/vexlife-runtime-compat/model",
                    providerRevision = "8".repeat(
                        40,
                    ),
                    contentSetSha256 = HereticProductIdentity.ArtifactContentSetSha256,
                    memberCount = 11,
                    totalBytes = 15_153_242_506L,
                    reusedMemberCount = 11,
                    acquiredMemberCount = 0,
                    acquisitionPerformed = false,
                    artifactMutationPerformed = false,
                ),
            environment =
                HereticPythonEnvironment.Result(
                    state = "REUSED_VERIFIED",
                    pythonEnvironmentRoot = "/tmp/vexlife-runtime-compat/python",
                    pythonPath = "/tmp/vexlife-runtime-compat/python/bin/python",
                    pythonVersion = HereticProductIdentity.PythonVersion,
                    mlxLmVersion = HereticProductIdentity.MlxLmVersion,
                    pipReportPath = "/tmp/vexlife-runtime-compat/python/vexlife-mlx-lm-install-report.json",
                    environmentMutationPerformed = false,
                ),
        )

    private fun substrate(
        pid: Long,
    ): MacUserLaunchdProcess.Receipt =
        MacUserLaunchdProcess.Receipt(
            schemaVersion = MacUserLaunchdProcess.Schema,
            substrateRef = MacUserLaunchdProcess.SubstrateRef,
            label = "com.vexlife.heretic.stable-test",
            domainTarget = "gui/501",
            plistPath = "/tmp/vexlife-runtime-compat/stable.plist",
            programPath = "/tmp/vexlife-runtime-compat/python/bin/python",
            programArguments = emptyList(),
            workingDirectory = "/tmp/vexlife-runtime-compat/python",
            stdoutPath = "/tmp/vexlife-runtime-compat/stable.out.log",
            stderrPath = "/tmp/vexlife-runtime-compat/stable.err.log",
            environment = emptyMap(),
            keepAlive = false,
            pid = pid,
        )

    @Test
    fun exactStableReceiptBecomesBorrowedEvidenceWithoutOwnershipEffects() {
        val binding =
            binding()
        val pid =
            4242L
        val platform =
            Platform(
                expectedPid = pid,
                expectedProcessGroupId = pid,
            )
        val receipt =
            StableVexLifeHereticRuntimeCompatibility.Receipt(
                schemaVersion = StableVexLifeHereticRuntimeCompatibility.ReceiptSchema,
                receiptRef = "receipt.vexlife.heretic-persistent-runtime.stable-test",
                state = StableVexLifeHereticRuntimeCompatibility.QualifiedState,
                formedAt = "2026-11-05T19:44:00-08:00",
                authorityRef = StableVexLifeHereticRuntimeCompatibility.StableAuthorityRef,
                modelRef = HereticProductIdentity.ModelRef,
                modelProfileRef = HereticProductIdentity.ModelProfileRef,
                artifactRef = HereticProductIdentity.ArtifactSetRef,
                artifactCustodyEvidenceRef = HereticProductIdentity.ArtifactCustodyEvidenceRef,
                artifactContentSetSha256 = HereticProductIdentity.ArtifactContentSetSha256,
                runtimeAdapterRef = HereticProductIdentity.RuntimeAdapterRef,
                runtimeClass = HereticProductIdentity.RuntimeClass,
                transportClass = HereticProductIdentity.TransportClass,
                hostExecutionProfileRef =
                    StableVexLifeHereticRuntimeCompatibility.StableHostExecutionProfileRef,
                pythonVersion = HereticProductIdentity.PythonVersion,
                mlxLmVersion = HereticProductIdentity.MlxLmVersion,
                trainerSourceCommit = HereticProductIdentity.TrainerSourceCommit,
                modelPath = binding.modelPath,
                pythonPath = binding.pythonPath,
                pythonEnvironmentRoot = binding.pythonEnvironmentRoot,
                endpoint = HereticProductIdentity.Endpoint,
                requestModel = HereticProductIdentity.RequestModel,
                sourceIdentity =
                    StableVexLifeHereticRuntimeCompatibility.SourceIdentity(
                        repository =
                            StableVexLifeHereticRuntimeCompatibility.StableOwnerRepository,
                        modulePath =
                            StableVexLifeHereticRuntimeCompatibility.StableOwnerModulePath,
                        moduleSha256 =
                            "a".repeat(
                                64,
                            ),
                    ),
                runtime =
                    StableVexLifeHereticRuntimeCompatibility.RuntimeProcess(
                        pid = pid,
                        processGroupId = pid,
                        exactOwnedDetachedGroup = true,
                        args =
                            HereticPersistentRuntime.serverArgs(
                                binding,
                            ),
                        processSubstrate =
                            substrate(
                                pid,
                            ),
                    ),
                effects = HereticRuntimeEffectBoundary.QualifiedRuntime,
                qualification = HereticRuntimeQualification.qualified(),
            )

        val observed =
            StableVexLifeHereticRuntimeCompatibility.observeReceipt(
                receipt = receipt,
                binding = binding,
                platform = platform,
            )

        assertEquals(
            pid,
            observed.pid,
        )
        assertEquals(
            HereticProductIdentity.StableRuntimeReceiptRelativePath,
            observed.receiptRelativePath,
        )
        assertEquals(
            receipt.receiptRef,
            observed.evidence.runtimeReceiptRef,
        )
        assertEquals(
            HereticRuntimeState.Qualified,
            observed.evidence.runtimeState,
        )
        assertEquals(
            listOf(
                "ENVIRONMENT_CURRENT",
                "PROCESS_ALIVE",
                "PROCESS_IDENTITY",
                "SUBSTRATE_CURRENT",
            ),
            platform.trace,
        )
        assertTrue(
            observed.evidence.qualification.healthAndExactModelVerified,
        )
        assertFalse(
            observed.evidence.qualification.livedConversationEffect,
        )
    }
}

// [VXG RealForever]
