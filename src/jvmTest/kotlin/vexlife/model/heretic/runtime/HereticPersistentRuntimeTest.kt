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

    private inner class Platform(
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
        var generationReadiness =
            HereticPersistentRuntime.GenerationReadiness.Available
        var cancel = false
        var clock = 0L
        var retireCalls = 0
        var provisionalRetireCalls = 0
        val processIdentityArgs = mutableListOf<List<String>>()

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
            processIdentityArgs += args
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

        override fun generationReadiness(
            binding: HereticPersistentRuntime.Binding,
        ): HereticPersistentRuntime.GenerationReadiness {
            trace +=
                "GENERATION_READINESS_" +
                    generationReadiness.name
            return generationReadiness
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

    private fun binding(
        promptCacheEntries: Int = HereticPersistentRuntime.PromptCacheEntryLimit,
    ): HereticPersistentRuntime.Binding =
        HereticPersistentRuntime.bind(
            authorityRef = "authority.vexlife-prod.heretic-runtime.test",
            materialization = HereticArtifactMaterialization.Result(                modelPath = "/tmp/vexlife-prod-test/model",
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
            promptCacheEntryLimit = promptCacheEntries,
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
        environment = emptyMap(),
        keepAlive = false,
        pid = pid,
    )

    private fun receipt(
        binding: HereticPersistentRuntime.Binding,
        state: HereticRuntimeState = HereticRuntimeState.Qualified,
        implementationSha: String = binding.implementationIdentity.classSha256,
        pid: Long = 4242L,
        runtimeArgs: List<String> = HereticPersistentRuntime.serverArgs(binding),
    ): HereticPersistentRuntime.Receipt {
        val identity =
            binding.model.identity
        return HereticPersistentRuntime.Receipt(
            schemaVersion = HereticPersistentRuntime.ReceiptSchema,
            receiptRef = "receipt.vexlife-prod.heretic-persistent-runtime.test",
            state = state,
            formedAt = "2026-11-04T17:00:00.000-08:00",
            authorityRef = binding.authorityRef,
            modelRef = identity.modelRef,
            modelProfileRef = identity.modelProfileRef,
            artifactRef = identity.artifactSetRef,
            artifactCustodyEvidenceRef = identity.artifactCustodyEvidenceRef,
            artifactContentSetSha256 = identity.artifactContentSetSha256,
            runtimeAdapterRef = identity.runtimeAdapterRef,
            runtimeClass = identity.runtimeClass,
            transportClass = identity.transportClass,
            hostExecutionProfileRef = identity.hostExecutionProfileRef,
            pythonVersion = identity.pythonVersion,
            mlxLmVersion = identity.mlxLmVersion,
            trainerSourceCommit = identity.trainerSourceCommit,
            modelPath = binding.modelPath,
            pythonPath = binding.pythonPath,
            pythonEnvironmentRoot = binding.pythonEnvironmentRoot,
            endpoint = identity.endpoint,
            requestModel = identity.requestModel,
            implementationIdentity = implementationIdentity(implementationSha),
            runtime = HereticPersistentRuntime.RuntimeProcess(
                pid = pid,
                processGroupId = pid,
                exactOwnedDetachedGroup = true,
                args = runtimeArgs,
                processSubstrate = substrate(pid),
            ),
            effects = HereticRuntimeEffectBoundary.QualifiedRuntime,
            qualification = if (state == HereticRuntimeState.Qualified) {
                HereticRuntimeQualification.qualified()
            } else {
                null
            },
        )
    }

    @Test
    fun currentQualifiedObservationProvesSameProcessWithoutReadinessOrLifecycleMutation() {
        val trace =
            mutableListOf<String>()
        val current =
            binding()
        val existing =
            receipt(
                current,
            )
        val store =
            Store(
                initial = existing,
                trace = trace,
            )
        val platform =
            Platform(
                trace,
            )
        platform.alivePids +=
            existing.runtime.pid
        platform.processGroupId =
            existing.runtime.processGroupId
        platform.matching =
            listOf(
                HereticPersistentRuntime.ProcessIdentity(
                    pid = existing.runtime.pid,
                    processGroupId = existing.runtime.processGroupId,
                    exactOwnedDetachedGroup = true,
                ),
            )

        val evidence =
            HereticPersistentRuntime.observeCurrentQualified(
                binding = current,
                store = store,
                platform = platform,
            )

        assertEquals(
            existing.receiptRef,
            evidence.runtimeReceiptRef,
        )
        assertEquals(
            HereticRuntimeState.Qualified,
            evidence.runtimeState,
        )
        assertTrue(
            "ENVIRONMENT_CURRENT" in
                trace,
        )
        assertTrue(
            "STORE_READ" in
                trace,
        )
        assertTrue(
            "PROCESS_ALIVE_" +
                existing.runtime.pid in
                trace,
        )
        assertTrue(
            "MATCHING_PROCESSES" in
                trace,
        )
        assertTrue(
            "PROCESS_IDENTITY_" +
                existing.runtime.pid in
                trace,
        )
        assertTrue(
            "SUBSTRATE_CURRENT_" +
                existing.runtime.pid in
                trace,
        )
        assertFalse(
            trace.any {
                it.startsWith(
                    "GENERATION_READINESS_",
                )
            },
        )
        assertFalse(
            "WAIT_HEALTHY" in
                trace,
        )
        assertFalse(
            trace.any {
                it.startsWith(
                    "STORE_WRITE_",
                ) ||
                    it ==
                    "SPAWN" ||
                    it.startsWith(
                        "RETIRE",
                    ) ||
                    it ==
                    "QUALIFY"
            },
        )
    }

    @Test
    fun currentQualifiedObservationFailsClosedWhenReceiptProcessIsNotTheOnlyMatch() {
        val trace =
            mutableListOf<String>()
        val current =
            binding()
        val existing =
            receipt(
                current,
            )
        val store =
            Store(
                initial = existing,
                trace = trace,
            )
        val platform =
            Platform(
                trace,
            )
        platform.alivePids +=
            existing.runtime.pid
        platform.matching =
            listOf(
                HereticPersistentRuntime.ProcessIdentity(
                    pid = existing.runtime.pid,
                    processGroupId = existing.runtime.processGroupId,
                    exactOwnedDetachedGroup = true,
                ),
                HereticPersistentRuntime.ProcessIdentity(
                    pid = 5151L,
                    processGroupId = 5151L,
                    exactOwnedDetachedGroup = true,
                ),
            )

        assertFailsWith<IllegalArgumentException> {
            HereticPersistentRuntime.observeCurrentQualified(
                binding = current,
                store = store,
                platform = platform,
            )
        }

        assertFalse(
            "WAIT_HEALTHY" in
                trace,
        )
        assertFalse(
            "QUALIFY" in
                trace,
        )
    }

    @Test
    fun currentQualifiedObservationKeepsReadinessSeparateFromCurrentness() {
        val trace =
            mutableListOf<String>()
        val current =
            binding()
        val existing =
            receipt(
                current,
            )
        val store =
            Store(
                initial = existing,
                trace = trace,
            )
        val platform =
            Platform(
                trace,
            )
                .also {
                    it.alivePids +=
                        existing.runtime.pid
                    it.processGroupId =
                        existing.runtime.processGroupId
                    it.matching =
                        listOf(
                            HereticPersistentRuntime.ProcessIdentity(
                                pid = existing.runtime.pid,
                                processGroupId = existing.runtime.processGroupId,
                                exactOwnedDetachedGroup = true,
                            ),
                        )
                    it.generationReadiness =
                        HereticPersistentRuntime.GenerationReadiness.Unknown
                }

        val evidence =
            HereticPersistentRuntime.observeCurrentQualified(
                binding = current,
                store = store,
                platform = platform,
            )

        assertEquals(
            existing.receiptRef,
            evidence.runtimeReceiptRef,
        )
        assertFalse(
            trace.any {
                it.startsWith(
                    "GENERATION_READINESS_",
                )
            },
        )
        assertFalse(
            "WAIT_HEALTHY" in
                trace,
        )
        assertFalse(
            "QUALIFY" in
                trace,
        )
        assertFalse(
            trace.any {
                it.startsWith(
                    "RETIRE_",
                )
            },
        )
    }

    @Test
    fun liveQualifiedRuntimeQualificationFailureDoesNotAuthorizeRetirement() {
        val trace =
            mutableListOf<String>()
        val current =
            binding()
        val prior =
            receipt(
                current,
            )
        val store =
            Store(
                prior,
                trace,
            )
        val platform =
            Platform(
                trace,
            )
                .also {
                    it.alivePids +=
                        prior.runtime.pid
                    it.generationReadiness =
                        HereticPersistentRuntime.GenerationReadiness.Available
                    it.qualificationFailure =
                        true
                    it.spawnedPid =
                        prior.runtime.pid +
                            1
                }

        assertFailsWith<IllegalStateException> {
            HereticPersistentRuntime.startOrReuse(
                current,
                store,
                platform,
            )
        }

        assertEquals(
            0,
            platform.retireCalls,
        )
        assertEquals(
            prior,
            store.value,
        )
        assertTrue(
            prior.runtime.pid in
                platform.alivePids,
        )
        assertTrue(
            trace.any {
                it.startsWith(
                    "GENERATION_READINESS_",
                )
            },
        )
        assertTrue(
            "QUALIFY" in
                trace,
        )
        assertFalse(
            "SPAWN" in
                trace,
        )
    }

    @Test
    fun explicitGenerationUnavailableRotatesExactOwnedRuntimeBeforeReplacement() {
        val trace = mutableListOf<String>()
        val current = binding()
        val prior = receipt(current)
        val store = Store(prior, trace)
        val platform =
            Platform(trace).also {
                it.alivePids += prior.runtime.pid
                it.generationReadiness =
                    HereticPersistentRuntime.GenerationReadiness.Unavailable
                it.spawnedPid = prior.runtime.pid + 1
            }

        val result = HereticPersistentRuntime.startOrReuse(current, store, platform)

        assertEquals(
            "RECOVERED_UNAVAILABLE_GENERATION_AND_STARTED_NEW_EXACT_MLX_RUNTIME",
            result.disposition,
        )
        assertEquals(1, platform.retireCalls)
        assertTrue(trace.indexOf("RETIRE_Qualified") < trace.indexOf("SPAWN"))
        assertTrue(trace.contains("STORE_DELETE"))
        assertTrue(trace.contains("GENERATION_READINESS_Unavailable"))
        assertEquals(prior.runtime.pid + 1, result.pid)
        assertEquals(HereticRuntimeState.Qualified, store.value?.state)
    }

    @Test
    fun unknownGenerationReadinessPreservesExactOwnedRuntimeWithoutReplacement() {
        val trace = mutableListOf<String>()
        val current = binding()
        val prior = receipt(current)
        val store = Store(prior, trace)
        val platform =
            Platform(trace).also {
                it.alivePids += prior.runtime.pid
                it.generationReadiness =
                    HereticPersistentRuntime.GenerationReadiness.Unknown
                it.spawnedPid = prior.runtime.pid + 1
            }

        val error =
            assertFailsWith<IllegalStateException> {
                HereticPersistentRuntime.startOrReuse(current, store, platform)
            }

        assertTrue(error.message?.contains("generation readiness is unknown") == true)
        assertEquals(0, platform.retireCalls)
        assertEquals(prior, store.value)
        assertTrue(prior.runtime.pid in platform.alivePids)
        assertFalse(trace.contains("SPAWN"))
        assertTrue(trace.contains("GENERATION_READINESS_Unknown"))
    }

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
    fun exactOwnedPrePromptCacheRuntimeRotatesIntoCurrentLaunchContract() {
        val trace = mutableListOf<String>()
        val current = binding()
        val currentArgs = HereticPersistentRuntime.serverArgs(current)
        val predecessorArgs = currentArgs.dropLast(2)
        assertFalse("--prompt-cache-size" in predecessorArgs)

        val staleSha =
            if (current.implementationIdentity.classSha256 == "a".repeat(64)) {
                "b".repeat(64)
            } else {
                "a".repeat(64)
            }
        val prior =
            receipt(
                binding = current,
                implementationSha = staleSha,
                runtimeArgs = predecessorArgs,
            )
        val store = Store(prior, trace)
        val platform = Platform(trace).also { it.alivePids += prior.runtime.pid }

        val result = HereticPersistentRuntime.startOrReuse(current, store, platform)

        assertEquals(
            "ROTATED_STALE_IMPLEMENTATION_AND_STARTED_NEW_EXACT_MLX_RUNTIME",
            result.disposition,
        )
        assertTrue(trace.indexOf("RETIRE_Qualified") < trace.indexOf("SPAWN"))
        assertTrue(predecessorArgs in platform.processIdentityArgs)
        assertTrue(currentArgs in platform.processIdentityArgs)
        assertEquals(currentArgs, result.receipt.runtime.args)
        assertTrue(trace.contains("STORE_DELETE"))
    }

    @Test
    fun exactOwnedRuntimeForNoLongerOfferedModelRetiresWithoutRequiringThatModel() {
        val trace = mutableListOf<String>()
        val current = binding()
        val obsoleteModelPath = "/tmp/vexlife-prod-test/models/heretic/obsolete-gpt-oss"
        val obsoleteArgs =
            HereticPersistentRuntime
                .serverArgs(
                    current,
                )
                .map {
                    if (
                        it ==
                            current.modelPath
                    ) {
                        obsoleteModelPath
                    } else {
                        it
                    }
                }
        val prior =
            receipt(
                binding = current,
                runtimeArgs = obsoleteArgs,
            )
                .copy(
                    modelRef = "model.vexlife-prod.experimental.gpt-oss.removed",
                    modelProfileRef = "model-profile.vexlife-prod.experimental.gpt-oss.removed",
                    artifactRef = "artifact.vexlife-prod.experimental.gpt-oss.removed",
                    artifactCustodyEvidenceRef =
                        "resolution.vexlife-prod.experimental.gpt-oss.removed",
                    artifactContentSetSha256 = "b".repeat(64),
                    modelPath = obsoleteModelPath,
                    trainerSourceCommit = "c".repeat(40),
                )
        val store = Store(prior, trace)
        val platform = Platform(trace).also { it.alivePids += prior.runtime.pid }

        val result =
            HereticPersistentRuntime.startOrReuse(
                current,
                store,
                platform,
            )

        assertEquals(
            "SWITCHED_MODEL_AND_STARTED_NEW_EXACT_MLX_RUNTIME",
            result.disposition,
        )
        assertTrue(trace.indexOf("RETIRE_Qualified") < trace.indexOf("SPAWN"))
        assertTrue(obsoleteArgs in platform.processIdentityArgs)
        assertTrue(HereticPersistentRuntime.serverArgs(current) in platform.processIdentityArgs)
        assertTrue(trace.contains("STORE_DELETE"))
        assertEquals(1, platform.retireCalls)
        assertEquals(current.model.modelRef, result.receipt.modelRef)
    }

    @Test
    fun arbitraryPriorRuntimeArgsNeverAuthorizeRetirement() {
        val trace = mutableListOf<String>()
        val current = binding()
        val prior =
            receipt(
                binding = current,
                implementationSha = "a".repeat(64),
                runtimeArgs = listOf("-m", "mlx_lm.server", "--unexpected-runtime-contract"),
            )
        val store = Store(prior, trace)
        val platform = Platform(trace).also { it.alivePids += prior.runtime.pid }

        val error =
            assertFailsWith<IllegalStateException> {
                HereticPersistentRuntime.startOrReuse(current, store, platform)
            }

        assertTrue(error.message?.contains("does not match the current binding") == true)
        assertEquals(0, platform.retireCalls)
        assertFalse(trace.contains("STORE_DELETE"))
        assertFalse(trace.contains("SPAWN"))
    }

    private fun bindingFor(
        model: vexlife.model.heretic.HereticModelCatalog.Entry,
        promptCacheEntries: Int = HereticPersistentRuntime.PromptCacheEntryLimit,
    ): HereticPersistentRuntime.Binding =
        HereticPersistentRuntime.bind(
            authorityRef = "authority.vexlife-prod.heretic-runtime.test",
            materialization =
                HereticArtifactMaterialization.Result(                    modelPath = "/tmp/vexlife-prod-test/" + model.handle,
                    providerRevision = model.providerRevision,
                    contentSetSha256 = model.identity.artifactContentSetSha256,
                    memberCount = model.members.size,
                    totalBytes = model.totalBytes,
                    reusedMemberCount = model.members.size,
                    acquiredMemberCount = 0,
                    acquisitionPerformed = false,
                    artifactMutationPerformed = false,
                ),
            environment = binding().let {
                HereticPythonEnvironment.Result(
                    state = "REUSED_VERIFIED",
                    pythonEnvironmentRoot = it.pythonEnvironmentRoot,
                    pythonPath = it.pythonPath,
                    pythonVersion = HereticProductIdentity.PythonVersion,
                    mlxLmVersion = HereticProductIdentity.MlxLmVersion,
                    pipReportPath = "/tmp/vexlife-prod-test/runtime/python/vexlife-mlx-lm-install-report.json",
                    environmentMutationPerformed = false,
                )
            },
            promptCacheEntryLimit = promptCacheEntries,
        )

    @Test
    fun defaultServerLaunchKeepsTheExistingOneEntrySafetyFloor() {
        val args = HereticPersistentRuntime.serverArgs(binding())

        val cacheFlag = args.indexOf("--prompt-cache-size")
        assertTrue(cacheFlag >= 0)
        assertEquals(
            HereticPersistentRuntime.PromptCacheEntryLimit.toString(),
            args[cacheFlag + 1],
        )
        assertEquals(1, HereticPersistentRuntime.PromptCacheEntryLimit)

        // This first safety step is deliberately narrow. Do not silently claim active-KV or
        // prefill tuning that has not yet been earned by one-cache lived stress evidence.
        assertFalse("--prompt-cache-bytes" in args)
        assertFalse("--kv-bits" in args)
        assertFalse("--prefill-step-size" in args)
    }

    @Test
    fun fourBLaunchCanUseItsEarnedThreeEntryCandidate() {
        val four =
            bindingFor(
                model = vexlife.model.heretic.HereticModelCatalog.Models.Qwen4B,
                promptCacheEntries = 3,
            )
        val args =
            HereticPersistentRuntime.serverArgs(
                four,
            )
        val cacheFlag =
            args.indexOf(
                "--prompt-cache-size",
            )

        assertTrue(
            cacheFlag >= 0,
        )
        assertEquals(
            "3",
            args[cacheFlag + 1],
        )
    }

    @Test
    fun changingOnlyPromptCacheEntriesRotatesTheExactOwnedRuntime() {
        val trace =
            mutableListOf<String>()
        val priorBinding =
            bindingFor(
                model = vexlife.model.heretic.HereticModelCatalog.Models.Qwen4B,
                promptCacheEntries = 1,
            )
        val currentBinding =
            bindingFor(
                model = vexlife.model.heretic.HereticModelCatalog.Models.Qwen4B,
                promptCacheEntries = 3,
            )
        val prior =
            receipt(
                priorBinding,
            )
        val store =
            Store(
                prior,
                trace,
            )
        val platform =
            Platform(
                trace,
            )
                .also {
                    it.alivePids += prior.runtime.pid
                }

        val result =
            HereticPersistentRuntime.startOrReuse(
                currentBinding,
                store,
                platform,
            )

        assertEquals(
            "ROTATED_RUNTIME_CONFIGURATION_AND_STARTED_NEW_EXACT_MLX_RUNTIME",
            result.disposition,
        )
        assertTrue(
            trace.indexOf("RETIRE_Qualified") < trace.indexOf("SPAWN"),
        )
        assertTrue(
            HereticPersistentRuntime.serverArgs(priorBinding) in platform.processIdentityArgs,
        )
        assertEquals(
            "3",
            result.receipt.runtime.args[
                result.receipt.runtime.args.indexOf("--prompt-cache-size") + 1
            ],
        )
    }

    @Test
    fun aBindingNamesTheCatalogModelItsFilesAre() {
        val nine = bindingFor(vexlife.model.heretic.HereticModelCatalog.Models.Qwen9B)
        assertEquals(vexlife.model.heretic.HereticModelCatalog.Models.Qwen9B.modelRef, nine.model.modelRef)
        assertEquals(vexlife.model.heretic.HereticModelCatalog.Models.Qwen27B.modelRef, binding().model.modelRef)
        assertTrue(HereticPersistentRuntime.serverArgs(nine).contains("/tmp/vexlife-prod-test/heretic-9b"))
    }

    @Test
    fun aModelSwitchRetiresTheExactOwnedRuntimeBeforeStartingTheChosenModel() {
        val trace = mutableListOf<String>()
        val prior = receipt(binding())
        val store = Store(prior, trace)
        val platform = Platform(trace).also { it.alivePids += prior.runtime.pid }
        val nine = bindingFor(vexlife.model.heretic.HereticModelCatalog.Models.Qwen9B)

        val result = HereticPersistentRuntime.startOrReuse(nine, store, platform)

        assertEquals("SWITCHED_MODEL_AND_STARTED_NEW_EXACT_MLX_RUNTIME", result.disposition)
        assertTrue(trace.indexOf("RETIRE_Qualified") < trace.indexOf("SPAWN"))
        assertEquals(vexlife.model.heretic.HereticModelCatalog.Models.Qwen9B.modelRef, result.receipt.modelRef)
        assertEquals(
            vexlife.model.heretic.HereticModelCatalog.Models.Qwen9B.identity.artifactContentSetSha256,
            result.receipt.artifactContentSetSha256,
        )
        assertEquals(
            vexlife.model.heretic.HereticModelCatalog.Models.Qwen9B.modelRef,
            HereticPersistentRuntime.configurationEvidence(nine, result.receipt).modelRef,
        )
    }

    @Test
    fun aReceiptForAnotherModelThatIsNotProvablyOursIsNeverRetired() {
        val trace = mutableListOf<String>()
        val foreign = receipt(binding()).copy(authorityRef = "authority.someone-else.heretic")
        val store = Store(foreign, trace)
        val platform = Platform(trace).also { it.alivePids += foreign.runtime.pid }

        assertFailsWith<IllegalStateException> {
            HereticPersistentRuntime.startOrReuse(bindingFor(vexlife.model.heretic.HereticModelCatalog.Models.Qwen4B), store, platform)
        }
        assertEquals(0, platform.retireCalls)
        assertFalse(trace.contains("SPAWN"))
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
