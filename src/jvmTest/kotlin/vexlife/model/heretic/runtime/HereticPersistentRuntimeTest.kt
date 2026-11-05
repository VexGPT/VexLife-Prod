package vexlife.model.heretic.runtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import vexlife.model.heretic.HereticProductIdentity
import vexlife.model.heretic.HereticRuntimeEffectBoundary
import vexlife.model.heretic.HereticRuntimeQualification
import vexlife.model.heretic.HereticRuntimeState
import vexlife.platform.macos.MacUserLaunchdProcess

class HereticPersistentRuntimeTest {
    private class Store(
        initial: HereticPersistentRuntime.Receipt? = null,
        private val trace: MutableList<String>,
    ) : HereticPersistentRuntime.ReceiptStore {
        var value: HereticPersistentRuntime.Receipt? = initial

        override fun read(): HereticPersistentRuntime.Receipt? {
            trace += "STORE_READ"
            return value
        }

        override fun write(receipt: HereticPersistentRuntime.Receipt) {
            trace += "STORE_WRITE_" + receipt.state.name
            value = receipt
        }

        override fun delete() {
            trace += "STORE_DELETE"
            value = null
        }
    }

    private class Platform(
        private val trace: MutableList<String>,
    ) : HereticPersistentRuntime.Platform {
        override val platform = "darwin"
        override val architecture = "arm64"

        val alivePids = mutableSetOf<Long>()
        var environmentIsCurrent = true
        var identityAvailable = true
        var processGroupId = 4242L
        var substrateCurrent = true
        var matching = emptyList<HereticPersistentRuntime.ProcessIdentity>()
        var occupied = false
        var spawnedPid = 4242L
        var spawnedAlive = true
        var qualificationFailure = false
        var healthFailure = false
        var cancel = false
        var clock = 0L
        var retireCalls = 0
        var provisionalRetireCalls = 0

        override fun environmentCurrent(binding: HereticPersistentRuntime.Binding): Boolean {
            trace += "ENVIRONMENT_CURRENT"
            return environmentIsCurrent
        }

        override fun processAlive(pid: Long): Boolean {
            trace += "PROCESS_ALIVE_$pid"
            return pid in alivePids
        }

        override fun processIdentity(
            pid: Long,
            args: List<String>,
        ): HereticPersistentRuntime.ProcessIdentity? {
            trace += "PROCESS_IDENTITY_$pid"
            if (!identityAvailable) return null
            return HereticPersistentRuntime.ProcessIdentity(
                pid = pid,
                processGroupId = processGroupId,
                exactOwnedDetachedGroup = true,
            )
        }

        override fun processSubstrateCurrent(
            receipt: MacUserLaunchdProcess.Receipt,
            expectedPid: Long,
        ): Boolean {
            trace += "SUBSTRATE_CURRENT_$expectedPid"
            return substrateCurrent && receipt.pid == expectedPid
        }

        override fun matchingProcesses(
            args: List<String>,
        ): List<HereticPersistentRuntime.ProcessIdentity> {
            trace += "MATCHING_PROCESSES"
            return matching
        }

        override fun portOccupied(port: Int): Boolean {
            trace += "PORT_OCCUPIED_$port"
            return occupied
        }

        override fun spawn(
            binding: HereticPersistentRuntime.Binding,
            args: List<String>,
        ): HereticPersistentRuntime.Spawned {
            trace += "SPAWN"
            if (spawnedAlive) alivePids += spawnedPid
            processGroupId = spawnedPid
            return HereticPersistentRuntime.Spawned(
                pid = spawnedPid,
                processSubstrate = substrate(spawnedPid),
            )
        }

        override fun retire(
            receipt: HereticPersistentRuntime.Receipt,
        ): HereticPersistentRuntime.Retirement {
            trace += "RETIRE_" + receipt.state.name
            retireCalls += 1
            alivePids -= receipt.runtime.pid
            return HereticPersistentRuntime.Retirement(
                exactOwnershipVerified = true,
                verifiedNotLive = true,
            )
        }

        override fun retireProvisional(
            spawned: HereticPersistentRuntime.Spawned,
            args: List<String>,
        ): HereticPersistentRuntime.Retirement {
            trace += "RETIRE_PROVISIONAL"
            provisionalRetireCalls += 1
            alivePids -= spawned.pid
            return HereticPersistentRuntime.Retirement(
                exactOwnershipVerified = true,
                verifiedNotLive = true,
            )
        }

        override fun waitUntilHealthy(
            binding: HereticPersistentRuntime.Binding,
            pid: Long,
        ) {
            trace += "WAIT_HEALTHY"
            if (healthFailure) error("health failed")
        }

        override fun qualify(
            binding: HereticPersistentRuntime.Binding,
        ): HereticRuntimeQualification {
            trace += "QUALIFY"
            if (qualificationFailure) error("qualification failed")
            return HereticRuntimeQualification.qualified()
        }

        override fun cancellationRequested(): Boolean = cancel

        override fun now(): String {
            clock += 1
            return "2026-11-04T17:00:00." + clock.toString().padStart(3, '0') + "-08:00"
        }

        override fun nowMs(): Long = clock

        override fun sleep(milliseconds: Long) {
            clock += milliseconds
        }
    }

    private fun implementationIdentity(
        sha: String =
            HereticPersistentRuntime.currentImplementationIdentity()
                .classSha256,
    ) = HereticPersistentRuntime.ImplementationIdentity(
        repository = "VexGPT/VexLife-Prod",
        classResource = HereticPersistentRuntime.ImplementationClassResource,
        classSha256 = sha,
    )

    private fun binding(): HereticPersistentRuntime.Binding =
        HereticPersistentRuntime.bind(
            authorityRef = "authority.vexlife-prod.heretic-runtime.test",
            materialization = HereticArtifactMaterialization.Result(
                state = "HERETIC_ARTIFACTS_MATERIALIZED_EXACT",
                modelPath = "/tmp/vexlife-prod-test/model",
                providerRevision = "8".repeat(40),
                contentSetSha256 = HereticProductIdentity.ArtifactContentSetSha256,
                memberCount = 11,
                totalBytes = 15_153_242_506L,
                reusedMemberCount = 11,
                acquiredMemberCount = 0,
                acquisitionPerformed = false,
                artifactMutationPerformed = false,
            ),
            environment = HereticPythonEnvironment.Result(
                state = "REUSED_VERIFIED",
                pythonEnvironmentRoot = "/tmp/vexlife-prod-test/runtime/python",
                pythonPath = "/tmp/vexlife-prod-test/runtime/python/bin/python",
                pythonVersion = HereticProductIdentity.PythonVersion,
                mlxLmVersion = HereticProductIdentity.MlxLmVersion,
                pipReportPath = "/tmp/vexlife-prod-test/runtime/python/vexlife-mlx-lm-install-report.json",
                environmentMutationPerformed = false,
            ),
        )

    private fun substrate(pid: Long) = MacUserLaunchdProcess.Receipt(
        schemaVersion = MacUserLaunchdProcess.Schema,
        substrateRef = MacUserLaunchdProcess.SubstrateRef,
        label = "com.vexlife.heretic.test",
        domainTarget = "gui/501",
        plistPath = "/tmp/vexlife-prod-test/runtime/heretic.plist",
        programPath = "/tmp/vexlife-prod-test/runtime/python/bin/python",
        programArguments = emptyList(),
        workingDirectory = "/tmp/vexlife-prod-test/runtime/python",
        stdoutPath = "/tmp/vexlife-prod-test/runtime/heretic.out.log",
        stderrPath = "/tmp/vexlife-prod-test/runtime/heretic.err.log",
        keepAlive = false,
        pid = pid,
    )

    private fun receipt(
        binding: HereticPersistentRuntime.Binding,
        state: HereticRuntimeState = HereticRuntimeState.Qualified,
        implementationSha: String = binding.implementationIdentity.classSha256,
        pid: Long = 4242L,
    ): HereticPersistentRuntime.Receipt =
        HereticPersistentRuntime.Receipt(
            schemaVersion = HereticPersistentRuntime.ReceiptSchema,
            receiptRef = "receipt.vexlife-prod.heretic-persistent-runtime.test",
            state = state,
            formedAt = "2026-11-04T17:00:00.000-08:00",
            authorityRef = binding.authorityRef,
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
            modelPath = binding.modelPath,
            pythonPath = binding.pythonPath,
            pythonEnvironmentRoot = binding.pythonEnvironmentRoot,
            endpoint = HereticProductIdentity.Endpoint,
            requestModel = HereticProductIdentity.RequestModel,
            implementationIdentity = implementationIdentity(implementationSha),
            runtime = HereticPersistentRuntime.RuntimeProcess(
                pid = pid,
                processGroupId = pid,
                exactOwnedDetachedGroup = true,
                args = HereticPersistentRuntime.serverArgs(binding),
                processSubstrate = substrate(pid),
            ),
            effects = HereticRuntimeEffectBoundary.QualifiedRuntime,
            qualification = if (state == HereticRuntimeState.Qualified) {
                HereticRuntimeQualification.qualified()
            } else {
                null
            },
        )

    @Test
    fun newStartCommitsStartingOwnershipBeforeReadinessAndQualification() {
        val trace = mutableListOf<String>()
        val store = Store(trace = trace)
        val platform = Platform(trace)

        val result = HereticPersistentRuntime.startOrReuse(
            binding = binding(),
            store = store,
            platform = platform,
        )

        assertEquals("STARTED_NEW_EXACT_MLX_RUNTIME", result.disposition)
        assertEquals(HereticRuntimeState.Qualified, result.receipt.state)
        assertTrue(trace.indexOf("STORE_WRITE_Starting") < trace.indexOf("WAIT_HEALTHY"))
        assertTrue(trace.indexOf("STORE_WRITE_Starting") < trace.indexOf("QUALIFY"))
        assertEquals(0, platform.provisionalRetireCalls)
    }

    @Test
    fun exactCurrentRuntimeIsReusedWithoutSpawn() {
        val trace = mutableListOf<String>()
        val current = binding()
        val prior = receipt(current)
        val store = Store(prior, trace)
        val platform = Platform(trace).also { it.alivePids += prior.runtime.pid }

        val result = HereticPersistentRuntime.startOrReuse(current, store, platform)

        assertEquals("REUSED_EXACT_OWNED_MLX_RUNTIME", result.disposition)
        assertFalse(trace.contains("SPAWN"))
        assertEquals(0, platform.retireCalls)
        assertEquals(HereticRuntimeState.Qualified, store.value?.state)
    }

    @Test
    fun startingReceiptRecoversThroughReadinessAndQualification() {
        val trace = mutableListOf<String>()
        val current = binding()
        val prior = receipt(current, state = HereticRuntimeState.Starting)
        val store = Store(prior, trace)
        val platform = Platform(trace).also { it.alivePids += prior.runtime.pid }

        val result = HereticPersistentRuntime.startOrReuse(current, store, platform)

        assertEquals("RECOVERED_EXACT_STARTING_MLX_RUNTIME", result.disposition)
        assertTrue(trace.contains("WAIT_HEALTHY"))
        assertTrue(trace.contains("QUALIFY"))
        assertEquals(HereticRuntimeState.Qualified, store.value?.state)
    }

    @Test
    fun staleImplementationExactOwnedRuntimeRotatesBeforeReplacement() {
        val trace = mutableListOf<String>()
        val current = binding()
        val staleSha =
            if (
                current.implementationIdentity.classSha256 ==
                    "a".repeat(
                        64,
                    )
            ) {
                "b".repeat(
                    64,
                )
            } else {
                "a".repeat(
                    64,
                )
            }
        val prior = receipt(current, implementationSha = staleSha)
        val store = Store(prior, trace)
        val platform = Platform(trace).also { it.alivePids += prior.runtime.pid }

        val result = HereticPersistentRuntime.startOrReuse(current, store, platform)

        assertEquals("ROTATED_STALE_IMPLEMENTATION_AND_STARTED_NEW_EXACT_MLX_RUNTIME", result.disposition)
        assertTrue(trace.indexOf("RETIRE_Qualified") < trace.indexOf("SPAWN"))
        assertTrue(trace.contains("STORE_DELETE"))
    }

    @Test
    fun bindingMismatchFailsBeforeRuntimeEffects() {
        val trace = mutableListOf<String>()
        val current = binding()
        val mismatched = receipt(current).copy(modelPath = "/tmp/vexlife-prod-test/different-model")
        val store = Store(mismatched, trace)
        val platform = Platform(trace)

        assertFailsWith<IllegalStateException> {
            HereticPersistentRuntime.startOrReuse(current, store, platform)
        }

        assertFalse(trace.contains("SPAWN"))
        assertEquals(0, platform.retireCalls)
    }

    @Test
    fun orphanProcessAndForeignPortHoldBeforeSpawn() {
        val current = binding()

        run {
            val trace = mutableListOf<String>()
            val platform = Platform(trace).also {
                it.matching = listOf(
                    HereticPersistentRuntime.ProcessIdentity(9999, 9999, true),
                )
            }
            assertFailsWith<IllegalArgumentException> {
                HereticPersistentRuntime.startOrReuse(current, Store(trace = trace), platform)
            }
            assertFalse(trace.any { it.startsWith("PORT_OCCUPIED_") })
            assertFalse(trace.contains("SPAWN"))
        }

        run {
            val trace = mutableListOf<String>()
            val platform = Platform(trace).also { it.occupied = true }
            assertFailsWith<IllegalArgumentException> {
                HereticPersistentRuntime.startOrReuse(current, Store(trace = trace), platform)
            }
            assertFalse(trace.contains("SPAWN"))
        }
    }

    @Test
    fun provisionalProcessNeverReceivesSemanticReceiptBeforeOwnership() {
        val trace = mutableListOf<String>()
        val store = Store(trace = trace)
        val platform = Platform(trace).also {
            it.spawnedAlive = false
            it.identityAvailable = false
        }

        assertFailsWith<IllegalStateException> {
            HereticPersistentRuntime.startOrReuse(binding(), store, platform)
        }

        assertEquals(1, platform.provisionalRetireCalls)
        assertEquals(0, platform.retireCalls)
        assertTrue(trace.none { it.startsWith("STORE_WRITE_") })
    }

    @Test
    fun qualificationFailureRetiresStartingRuntimeAndDeletesReceipt() {
        val trace = mutableListOf<String>()
        val store = Store(trace = trace)
        val platform = Platform(trace).also { it.qualificationFailure = true }

        assertFailsWith<IllegalStateException> {
            HereticPersistentRuntime.startOrReuse(binding(), store, platform)
        }

        assertTrue(trace.contains("STORE_WRITE_Starting"))
        assertTrue(trace.contains("STORE_DELETE"))
        assertEquals(1, platform.retireCalls)
        assertEquals(null, store.value)
    }

    @Test
    fun stopRequiresExactOwnedCurrentReceipt() {
        val trace = mutableListOf<String>()
        val current = binding()
        val prior = receipt(current)
        val store = Store(prior, trace)
        val platform = Platform(trace).also { it.alivePids += prior.runtime.pid }

        val stopped = HereticPersistentRuntime.stop(current, store, platform)

        assertEquals("STOPPED_EXACT_OWNED_MLX_RUNTIME", stopped.disposition)
        assertTrue(stopped.verifiedNotLive)
        assertEquals(1, platform.retireCalls)
        assertEquals(null, store.value)
    }

    @Test
    fun stopHoldsBeforeRetirementWhenLiveIdentityDrifts() {
        val trace = mutableListOf<String>()
        val current = binding()
        val prior = receipt(current)
        val store = Store(prior, trace)
        val platform = Platform(trace).also {
            it.alivePids += prior.runtime.pid
            it.processGroupId = 7777
        }

        assertFailsWith<IllegalArgumentException> {
            HereticPersistentRuntime.stop(current, store, platform)
        }

        assertEquals(0, platform.retireCalls)
        assertTrue(store.value === prior)
    }

    @Test
    fun noReceiptStopIsExplicitNoEffect() {
        val trace = mutableListOf<String>()
        val stopped = HereticPersistentRuntime.stop(
            binding = binding(),
            store = Store(trace = trace),
            platform = Platform(trace),
        )

        assertEquals("NO_HERETIC_RUNTIME_RECEIPT", stopped.disposition)
        assertFalse(stopped.processTerminationPerformed)
        assertTrue(stopped.verifiedNotLive)
    }
}

// [VXG RealForever]
