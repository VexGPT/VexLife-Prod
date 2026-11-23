package vexlife.model.heretic.runtime

import kotlinx.serialization.Serializable

import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.security.MessageDigest
import vexlife.logging.VexLogger
import vexlife.model.heretic.HereticModelCatalog
import vexlife.model.heretic.HereticModelConfiguration
import vexlife.model.heretic.HereticProductIdentity
import vexlife.model.heretic.HereticRuntimeConfiguration
import vexlife.model.heretic.HereticRuntimeEffectBoundary
import vexlife.model.heretic.HereticRuntimeQualification
import vexlife.model.heretic.HereticRuntimeRealization
import vexlife.model.heretic.HereticRuntimeState
import vexlife.platform.macos.MacUserLaunchdProcess

/**
 * Heretic persistent-runtime semantic/effect owner.
 *
 * Binding is formed only from exact outputs of the artifact + Python environment owners. Platform
 * process census, launchd construction, liveness and HTTP qualification are injected effects.
 */
object HereticPersistentRuntime {
    const val BindingSchema =
        "vexlife-prod.heretic-persistent-runtime-owner-binding/v1"

    const val ReceiptSchema =
        "vexlife-prod.heretic-persistent-runtime-owner-receipt/v1"

    const val OwnerRef =
        "owner.vexlife-prod.heretic-persistent-runtime.001"

    const val StopActionRef =
        "action.vexlife-prod.heretic-persistent-runtime.stop-exact-owned-runtime"

    val Port: Int =
        HereticProductIdentity.Endpoint
            .substringAfterLast(
                ":",
            )
            .toInt()
            .also {
                require(
                    it in
                        1024..65535,
                ) {
                    "Heretic product endpoint must expose one unprivileged runtime port"
                }
            }

    const val OwnershipTimeoutMs =
        30_000L

    const val OwnershipPollMs =
        50L

    /**
     * Safety-floor cache count for a Home that has not explicitly chosen another admitted value.
     *
     * Lived A002/A007 evidence showed one entry preserves only the stable leading system prefix
     * (~2,939 tokens), not the large immediate continuation. Higher counts remain explicit
     * experimental choices while provider cache formation and memory behavior are qualified.
     *
     * The floor remains grounded in the earlier 10-sequence/~20 GB unified-memory failure; changing
     * provider cache formation does not silently promote a larger default.
     */
    const val PromptCacheEntryLimit =
        1

    /**
     * Exact upstream provider source this Product-owned adapter is allowed to patch.
     *
     * Git blob identity for mlx_lm/server.py at mlx-lm v0.32.0:
     * https://github.com/ml-explore/mlx-lm/blob/v0.32.0/mlx_lm/server.py
     */
    private const val MlxServerBlobShaV0320 =
        "9462d6e57822df160adc260d9414838179b71907"

    /**
     * Keep the final pre-generation user/context cache that mlx-lm normally skips at end_of_prompt.
     *
     * This is deliberately an argv-bound in-memory adapter, not a site-packages mutation. The
     * installed provider file must match the exact pinned Git blob above, and the expected exclusion
     * must occur exactly once before the method is replaced. The normal mlx-lm server main then runs
     * unchanged.
     *
     * No prompt text is changed and no second prefill is performed.
     */
    private val FinalPromptCacheAdapterPython =
        listOf(
            "import hashlib,inspect,pathlib,textwrap,mlx_lm.server as s",
            "b=pathlib.Path(s.__file__).read_bytes()",
            "g=hashlib.sha1(b\"blob \"+str(len(b)).encode()+b\"\\0\"+b).hexdigest()",
            "assert g==\"$MlxServerBlobShaV0320\",\"VexLife provider adapter: mlx_lm.server source drift\"",
            "src=textwrap.dedent(inspect.getsource(s.ResponseGenerator._generate))",
            "needle=\"and not r.end_of_prompt\"",
            "assert src.count(needle)==1,\"VexLife provider adapter: expected cache exclusion not found exactly once\"",
            "ns={}",
            "exec(compile(src.replace(needle,\"and True  # VexLife preserves final pre-generation cache\"),\"<vexlife-final-prompt-cache>\",\"exec\"),s.__dict__,ns)",
            "s.ResponseGenerator._generate=ns[\"_generate\"]",
            "s.main()",
        )
            .joinToString(
                separator = ";",
            )

    private val RefPattern =
        Regex(
            "[A-Za-z0-9][A-Za-z0-9._:/-]*",
        )

    private val Sha256Pattern =
        Regex(
            "[0-9a-f]{64}",
        )

    const val ImplementationClassResource =
        "vexlife/model/heretic/runtime/HereticPersistentRuntime.class"

    @Serializable
    @ConsistentCopyVisibility
    data class ImplementationIdentity internal constructor(
        val repository: String,
        val classResource: String,
        val classSha256: String,
    ) {
        init {
            require(
                repository ==
                    "VexGPT/VexLife-Prod",
            ) {
                "Heretic runtime implementation identity must belong to VexLife-Prod"
            }
            require(
                classResource ==
                    ImplementationClassResource,
            ) {
                "Heretic runtime implementation class identity drift"
            }
            require(
                Sha256Pattern.matches(
                    classSha256,
                ),
            ) {
                "Heretic runtime implementation identity requires lowercase SHA-256"
            }
        }
    }

    fun currentImplementationIdentity(): ImplementationIdentity {
        val bytes =
            requireNotNull(
                HereticPersistentRuntime::class.java.classLoader
                    .getResourceAsStream(
                        ImplementationClassResource,
                    ),
            ) {
                "packaged Heretic runtime implementation class bytes are unavailable"
            }
                .use {
                    it.readBytes()
                }

        return ImplementationIdentity(
            repository = "VexGPT/VexLife-Prod",
            classResource = ImplementationClassResource,
            classSha256 = sha256(
                bytes,
            ),
        )
    }

    @ConsistentCopyVisibility
    data class Binding internal constructor(
        val schemaVersion: String,
        val authorityRef: String,
        val modelPath: String,
        val pythonPath: String,
        val pythonEnvironmentRoot: String,
        val implementationIdentity: ImplementationIdentity,
        /** The catalog model this runtime serves; its identity is what receipts must carry. */
        val model: HereticModelCatalog.Entry = HereticModelCatalog.Default,
        /** Exact Product-owned mlx-lm prompt-cache entry limit for this runtime launch. */
        val promptCacheEntryLimit: Int = PromptCacheEntryLimit,
    )

    data class ProcessIdentity(
        val pid: Long,
        val processGroupId: Long,
        val exactOwnedDetachedGroup: Boolean,
    )

    @Serializable
    data class RuntimeProcess(
        val pid: Long,
        val processGroupId: Long,
        val exactOwnedDetachedGroup: Boolean,
        val args: List<String>,
        val processSubstrate: MacUserLaunchdProcess.Receipt,
    )

    @Serializable
    data class Receipt(
        val schemaVersion: String,
        val receiptRef: String,
        val state: HereticRuntimeState,
        val formedAt: String,
        val authorityRef: String,
        val modelRef: String,
        val modelProfileRef: String,
        val artifactRef: String,
        val artifactCustodyEvidenceRef: String,
        val artifactContentSetSha256: String,
        val runtimeAdapterRef: String,
        val runtimeClass: String,
        val transportClass: String,
        val hostExecutionProfileRef: String,
        val pythonVersion: String,
        val mlxLmVersion: String,
        val trainerSourceCommit: String,
        val modelPath: String,
        val pythonPath: String,
        val pythonEnvironmentRoot: String,
        val endpoint: String,
        val requestModel: String,
        val implementationIdentity: ImplementationIdentity,
        val runtime: RuntimeProcess,
        val effects: HereticRuntimeEffectBoundary.Value,
        val qualification: HereticRuntimeQualification?,
    )

    interface ReceiptStore {
        fun read(): Receipt?

        fun write(
            receipt: Receipt,
        )

        fun delete()
    }

    data class Spawned(
        val pid: Long,
        val processSubstrate: MacUserLaunchdProcess.Receipt,
    )

    data class Retirement(
        val exactOwnershipVerified: Boolean,
        val verifiedNotLive: Boolean,
    )

    /**
     * Immediate generation-engine observation, separate from process/substrate currentness.
     *
     * Available means mlx-lm explicitly reports that generation can accept work. Unavailable is a
     * positive engine-level failure (for current mlx-lm, exact /health 503 + status=unavailable).
     * Unknown means transport/timeout/malformed observation and never authorizes retirement.
     */
    enum class GenerationReadiness {
        Available,
        Unavailable,
        Unknown,
    }

    interface Platform {
        val platform: String
        val architecture: String

        fun environmentCurrent(
            binding: Binding,
        ): Boolean

        fun processAlive(
            pid: Long,
        ): Boolean

        fun processIdentity(
            pid: Long,
            args: List<String>,
        ): ProcessIdentity?

        fun processSubstrateCurrent(
            receipt: MacUserLaunchdProcess.Receipt,
            expectedPid: Long,
        ): Boolean

        fun matchingProcesses(
            args: List<String>,
        ): List<ProcessIdentity>

        fun portOccupied(
            port: Int,
        ): Boolean

        fun spawn(
            binding: Binding,
            args: List<String>,
        ): Spawned

        fun retire(
            receipt: Receipt,
        ): Retirement

        /**
         * Retire a process that has a source-owned construction receipt but has not yet earned
         * semantic Heretic runtime ownership.
         */
        fun retireProvisional(
            spawned: Spawned,
            args: List<String>,
        ): Retirement

        /**
         * One immediate generation-readiness observation.
         *
         * This is deliberately separate from exact process/substrate currentness. Unknown is not
         * permission to mutate lifecycle; only Unavailable is positive generation-loss truth.
         */
        fun generationReadiness(
            binding: Binding,
        ): GenerationReadiness =
            GenerationReadiness.Available

        fun waitUntilHealthy(
            binding: Binding,
            pid: Long,
        )

        fun qualify(
            binding: Binding,
        ): HereticRuntimeQualification

        fun cancellationRequested(): Boolean =
            false

        fun now(): String

        fun nowMs(): Long =
            System.currentTimeMillis()

        fun sleep(
            milliseconds: Long,
        ) {
            Thread.sleep(
                milliseconds,
            )
        }
    }

    data class StartResult(
        val disposition: String,
        val pid: Long,
        val endpoint: String,
        val requestModel: String,
        val receipt: Receipt,
        val receiptWritePerformed: Boolean,
        val qualification: HereticRuntimeQualification,
    )

    data class StopResult(
        val disposition: String,
        val pid: Long?,
        val processTerminationPerformed: Boolean,
        val verifiedNotLive: Boolean,
    )

    fun bind(
        authorityRef: String,
        materialization: HereticArtifactMaterialization.Result,
        environment: HereticPythonEnvironment.Result,
        promptCacheEntryLimit: Int = PromptCacheEntryLimit,
        model: HereticModelCatalog.Entry? = null,
    ): Binding {
        require(
            RefPattern.matches(
                authorityRef,
            ),
        ) {
            "Heretic runtime authorityRef must be one stable ref"
        }
        val selectedModel =
            model
                ?: HereticModelCatalog.Entries.firstOrNull {
                    it.identity.artifactContentSetSha256 == materialization.contentSetSha256
                }
        require(
            materialization.state ==
                HereticArtifactMaterialization.MaterializedExactState &&
                selectedModel != null &&
                materialization.contentSetSha256 ==
                    selectedModel.identity.artifactContentSetSha256 &&
                materialization.providerRevision ==
                    selectedModel.providerRevision &&
                materialization.memberCount ==
                    selectedModel.members.size,
        ) {
            "Heretic runtime binding requires one exact selected-model materialization"
        }
        require(
            (
                environment.state ==
                    "REUSED_VERIFIED" ||
                    environment.state ==
                    "CREATED_VERIFIED"
            ) &&
                environment.pythonVersion ==
                HereticProductIdentity.PythonVersion &&
                environment.mlxLmVersion ==
                HereticProductIdentity.MlxLmVersion,
        ) {
            "Heretic runtime binding requires exact verified Python/mlx-lm environment"
        }

        val modelDirectory =
            absolute(
                materialization.modelPath,
                "modelPath",
            )
        val environmentRoot =
            absolute(
                environment.pythonEnvironmentRoot,
                "pythonEnvironmentRoot",
            )
        val python =
            absolute(
                environment.pythonPath,
                "pythonPath",
            )

        require(
            python.startsWith(
                environmentRoot,
            ),
        ) {
            "Heretic runtime Python executable must belong to the verified environment root"
        }

        return Binding(
            schemaVersion = BindingSchema,
            authorityRef = authorityRef,
            modelPath = modelDirectory.toString(),
            pythonPath = python.toString(),
            pythonEnvironmentRoot = environmentRoot.toString(),
            implementationIdentity = currentImplementationIdentity(),
            model = selectedModel,
            promptCacheEntryLimit =
                HereticRuntimeConfiguration.requirePromptCacheEntries(
                    promptCacheEntryLimit,
                ),
        )
    }

    fun configurationEvidence(
        binding: Binding,
        receipt: Receipt,
    ): HereticModelConfiguration.QualifiedRuntimeEvidence {
        validateBinding(
            binding,
        )
        require(
            receiptBinds(
                binding = binding,
                receipt = receipt,
                requireImplementationSha = true,
            ) &&
                receipt.state ==
                HereticRuntimeState.Qualified &&
                receipt.qualification !=
                null,
        ) {
            "Heretic model configuration evidence requires one exact current qualified runtime receipt"
        }

        return HereticModelConfiguration.QualifiedRuntimeEvidence(
            runtimeReceiptRef = receipt.receiptRef,
            runtimeState = receipt.state,
            modelRef = receipt.modelRef,
            realizationRef =
                requireNotNull(
                    HereticRuntimeRealization.resolve(
                        model = binding.model,
                        target = HereticRuntimeRealization.MacArm64,
                    ),
                ) {
                    "qualified Mac runtime binding has no exact realization"
                }
                    .realizationRef,
            modelProfileRef = receipt.modelProfileRef,
            artifactRef = receipt.artifactRef,
            artifactContentSetSha256 = receipt.artifactContentSetSha256,
            runtimeAdapterRef = receipt.runtimeAdapterRef,
            runtimeClass = receipt.runtimeClass,
            endpoint = receipt.endpoint,
            requestModel = receipt.requestModel,
            qualification =
                requireNotNull(
                    receipt.qualification,
                ),
        )
    }

    /**
     * Re-observe one already-qualified runtime without lifecycle mutation or another qualification
     * inference. This is the currentness gate for long-lived consumers such as the local product
     * service.
     */
    fun observeCurrentQualified(
        binding: Binding,
        store: ReceiptStore,
        platform: Platform,
    ): HereticModelConfiguration.QualifiedRuntimeEvidence {
        validateBinding(
            binding,
        )
        require(
            platform.platform ==
                "darwin" &&
                platform.architecture ==
                "arm64",
        ) {
            "Heretic runtime currentness requires darwin/arm64"
        }
        require(
            platform.environmentCurrent(
                binding,
            ),
        ) {
            "Heretic runtime currentness requires the exact current environment"
        }

        val receipt =
            requireNotNull(
                store.read(),
            ) {
                "Heretic runtime currentness requires one existing receipt"
            }

        require(
            receiptBinds(
                binding = binding,
                receipt = receipt,
                requireImplementationSha = true,
            ) &&
                receipt.state ==
                HereticRuntimeState.Qualified &&
                receipt.qualification !=
                null,
        ) {
            "Heretic runtime currentness requires one exact qualified current receipt"
        }

        val args =
            serverArgs(
                binding,
            )

        require(
            platform.processAlive(
                receipt.runtime.pid,
            ),
        ) {
            "qualified Heretic runtime receipt process is not live"
        }

        val matching =
            platform.matchingProcesses(
                args,
            )
        require(
            matching.size ==
                1 &&
                matching.single().pid ==
                receipt.runtime.pid &&
                matching.single().processGroupId ==
                receipt.runtime.processGroupId &&
                matching.single().exactOwnedDetachedGroup,
        ) {
            "qualified Heretic runtime is not the one exact matching process"
        }

        requireExactOwnedProcess(
            receipt = receipt,
            args = args,
            platform = platform,
        )

        // Currentness is exact receipt/process/substrate truth. A live qualified runtime may
        // temporarily be busy or slow to answer its generation-health endpoint; that absence of a
        // readiness observation is not lifecycle authority to replace the process.
        return configurationEvidence(
            binding = binding,
            receipt = receipt,
        )
    }

    enum class ServerLaunchContract {
        /** Current Product launch: exact pinned provider + final pre-generation prompt-cache adapter. */
        CURRENT,

        /** Prior Product launch: direct mlx_lm.server with Product-owned --prompt-cache-size. */
        PRE_FINAL_PROMPT_CACHE,

        /** Oldest accepted Product launch: direct mlx_lm.server before prompt-cache sizing. */
        PRE_PROMPT_CACHE_LIMIT,
    }

    fun promptCacheEntryLimit(
        args: List<String>,
    ): Int? {
        val index = args.indexOf("--prompt-cache-size")
        if (index < 0 || index + 1 >= args.size) {
            return null
        }
        return args[index + 1]
            .toIntOrNull()
            ?.let {
                runCatching {
                    HereticRuntimeConfiguration.requirePromptCacheEntries(it)
                }.getOrNull()
            }
    }

    fun serverArgs(
        binding: Binding,
    ): List<String> =
        serverArgs(
            binding = binding,
            contract = ServerLaunchContract.CURRENT,
        )

    internal fun serverArgs(
        binding: Binding,
        contract: ServerLaunchContract,
    ): List<String> {
        validateBinding(
            binding,
        )
        return serverArgs(
            modelPath = binding.modelPath,
            contract = contract,
            promptCacheEntryLimit = binding.promptCacheEntryLimit,
        )
    }

    private fun serverArgs(
        modelPath: String,
        contract: ServerLaunchContract,
        promptCacheEntryLimit: Int = PromptCacheEntryLimit,
    ): List<String> {
        require(
            modelPath.isNotBlank(),
        ) {
            "Heretic runtime launch contract requires one model path"
        }

        val providerEntry =
            when (contract) {
                ServerLaunchContract.CURRENT ->
                    listOf(
                        "-c",
                        FinalPromptCacheAdapterPython,
                    )

                ServerLaunchContract.PRE_FINAL_PROMPT_CACHE,
                ServerLaunchContract.PRE_PROMPT_CACHE_LIMIT,
                ->
                    listOf(
                        "-m",
                        "mlx_lm.server",
                    )
            }
        val base =
            providerEntry +
                listOf(
                    "--model",
                    modelPath,
                    "--host",
                    "127.0.0.1",
                    "--port",
                    Port.toString(),
                    "--chat-template-args",
                    "{\"enable_thinking\":false}",
                )
        return when (contract) {
            ServerLaunchContract.CURRENT,
            ServerLaunchContract.PRE_FINAL_PROMPT_CACHE,
            ->
                base +
                    listOf(
                        "--prompt-cache-size",
                        HereticRuntimeConfiguration
                            .requirePromptCacheEntries(
                                promptCacheEntryLimit,
                            )
                            .toString(),
                    )

            ServerLaunchContract.PRE_PROMPT_CACHE_LIMIT ->
                base
        }
    }

    /**
     * Exact launch argv from a runtime contract this owner previously emitted. This is migration
     * recognition only: arbitrary argv drift remains unowned and cannot authorize retirement.
     */
    internal fun serverLaunchContract(
        binding: Binding,
        args: List<String>,
    ): ServerLaunchContract? =
        serverLaunchContract(
            modelPath = binding.modelPath,
            args = args,
        )

    private fun serverLaunchContract(
        modelPath: String,
        args: List<String>,
    ): ServerLaunchContract? {
        val prePromptCache =
            serverArgs(
                modelPath = modelPath,
                contract = ServerLaunchContract.PRE_PROMPT_CACHE_LIMIT,
            )
        if (args == prePromptCache) {
            return ServerLaunchContract.PRE_PROMPT_CACHE_LIMIT
        }

        fun matchesSizedLaunch(
            base: List<String>,
        ): Boolean =
            args.size ==
                base.size +
                    2 &&
                args.dropLast(
                    2,
                ) ==
                base &&
                args[
                    args.lastIndex -
                        1
                ] ==
                "--prompt-cache-size" &&
                args.last()
                    .toIntOrNull()
                    ?.let {
                        runCatching {
                            HereticRuntimeConfiguration.requirePromptCacheEntries(
                                it,
                            )
                        }
                            .isSuccess
                    } ==
                true

        if (
            matchesSizedLaunch(
                prePromptCache,
            )
        ) {
            return ServerLaunchContract.PRE_FINAL_PROMPT_CACHE
        }

        val currentBase =
            serverArgs(
                modelPath = modelPath,
                contract = ServerLaunchContract.CURRENT,
            )
                .dropLast(
                    2,
                )
        if (
            matchesSizedLaunch(
                currentBase,
            )
        ) {
            return ServerLaunchContract.CURRENT
        }

        return null
    }

    private fun acceptedPriorServerArgs(
        receipt: Receipt,
    ): List<String>? =
        serverLaunchContract(
            modelPath = receipt.modelPath,
            args = receipt.runtime.args,
        )
            ?.let {
                receipt.runtime.args
            }

    /**
     * Retirement-only ownership for a Product runtime whose model is no longer offered.
     *
     * Serving eligibility is intentionally not part of this proof. A historical model may be
     * ineligible to serve now while the process/lifetime it left behind remains exactly owned and
     * therefore safely retireable. Model-specific identity is not promoted to current truth.
     */
    private fun receiptOwnsRetirableRuntimeLineage(
        binding: Binding,
        receipt: Receipt,
        expectedArgs: List<String>,
    ): Boolean {
        val current =
            binding.model.identity

        return receipt.schemaVersion ==
            ReceiptSchema &&
            receipt.authorityRef ==
            binding.authorityRef &&
            receipt.runtimeAdapterRef ==
            current.runtimeAdapterRef &&
            receipt.runtimeClass ==
            current.runtimeClass &&
            receipt.transportClass ==
            current.transportClass &&
            receipt.hostExecutionProfileRef ==
            current.hostExecutionProfileRef &&
            receipt.pythonVersion ==
            current.pythonVersion &&
            receipt.mlxLmVersion ==
            current.mlxLmVersion &&
            receipt.pythonPath ==
            binding.pythonPath &&
            receipt.pythonEnvironmentRoot ==
            binding.pythonEnvironmentRoot &&
            receipt.endpoint ==
            current.endpoint &&
            receipt.requestModel ==
            current.requestModel &&
            receipt.implementationIdentity.repository ==
            binding.implementationIdentity.repository &&
            receipt.implementationIdentity.classResource ==
            binding.implementationIdentity.classResource &&
            receipt.runtime.pid >
            0 &&
            receipt.runtime.processGroupId >
            0 &&
            receipt.runtime.exactOwnedDetachedGroup &&
            receipt.runtime.args ==
            expectedArgs &&
            receipt.runtime.processSubstrate.pid ==
            receipt.runtime.pid &&
            receipt.effects ==
            HereticRuntimeEffectBoundary.QualifiedRuntime
    }

    fun startOrReuse(
        binding: Binding,
        store: ReceiptStore,
        platform: Platform,
    ): StartResult {
        validateBinding(
            binding,
        )
        require(
            platform.platform ==
                "darwin" &&
                platform.architecture ==
                "arm64",
        ) {
            "Heretic persistent runtime requires darwin/arm64"
        }
        require(
            platform.environmentCurrent(
                binding,
            ),
        ) {
            "Heretic persistent runtime requires a fresh exact environment observation"
        }
        heldIfCancelled(
            platform,
        )

        val args =
            serverArgs(
                binding,
            )
        val prior =
            store.read()
        var staleImplementationRotated =
            false
        var runtimeConfigurationRotated =
            false
        var switchedModel =
            false
        var generationUnavailableRotated =
            false

        // Model switch: the Home chose another catalog model than the one this exactly owned runtime
        // serves. The prior receipt is still this Product's own (same authority, environment and
        // implementation lineage), so it is retired through the same exact-ownership path before the
        // chosen model starts. A receipt that is not provably ours still fails below.
        val priorModel =
            prior?.let { receipt ->
                if (receipt.modelRef == binding.model.modelRef) {
                    binding.model
                } else {
                    HereticModelCatalog.entry(receipt.modelRef)
                }
            }

        // A Product-owned runtime may outlive the catalog entry that selected it (for example an
        // experimental model removed by a later/narrower build). "No longer offered" means it may
        // not serve now; it does not erase lifecycle ownership needed to retire the exact process.
        val unsupportedPriorArgs =
            if (
                prior !=
                    null &&
                priorModel ==
                    null
            ) {
                serverLaunchContract(
                    modelPath = prior.modelPath,
                    args = prior.runtime.args,
                )
                    ?.let {
                        prior.runtime.args
                    }
            } else {
                null
            }
        if (
            prior !=
                null &&
                priorModel ==
                    null &&
                unsupportedPriorArgs !=
                    null &&
                receiptOwnsRetirableRuntimeLineage(
                    binding = binding,
                    receipt = prior,
                    expectedArgs = unsupportedPriorArgs,
                )
        ) {
            if (
                platform.processAlive(
                    prior.runtime.pid,
                )
            ) {
                requireExactOwnedProcess(
                    prior,
                    unsupportedPriorArgs,
                    platform,
                )
            }
            val retired =
                platform.retire(
                    prior,
                )
            require(
                retired.exactOwnershipVerified &&
                    retired.verifiedNotLive,
            ) {
                "unsupported-model exact Heretic runtime could not be retired before current model start"
            }
            store.delete()
            switchedModel =
                true
            VexLogger.record(
                VexLogger.Fact.RuntimeModelSwitched,
                mapOf(
                    "from" to prior.modelRef,
                    "to" to binding.model.handle,
                    "reason" to "PRIOR_MODEL_NOT_OFFERED",
                ),
            )
        }

        val priorModelBinding =
            if (
                prior !=
                    null &&
                    priorModel !=
                    null
            ) {
                binding.copy(
                    model = priorModel,
                    modelPath = prior.modelPath,
                )
            } else {
                null
            }
        val priorModelArgs =
            if (
                prior !=
                    null &&
                    priorModelBinding !=
                    null
            ) {
                acceptedPriorServerArgs(
                    receipt = prior,
                )
            } else {
                null
            }
        if (
            prior !=
                null &&
                priorModel !=
                null &&
                priorModelBinding !=
                null &&
                priorModelArgs !=
                null &&
                priorModel.modelRef !=
                binding.model.modelRef &&
                receiptBinds(
                    binding = priorModelBinding,
                    receipt = prior,
                    requireImplementationSha = false,
                    expectedArgs = priorModelArgs,
                )
        ) {
            if (
                platform.processAlive(
                    prior.runtime.pid,
                )
            ) {
                requireExactOwnedProcess(
                    prior,
                    priorModelArgs,
                    platform,
                )
            }
            val retired =
                platform.retire(
                    prior,
                )
            require(
                retired.exactOwnershipVerified &&
                    retired.verifiedNotLive,
            ) {
                "the runtime serving ${priorModel.handle} could not be retired before switching to ${binding.model.handle}"
            }
            store.delete()
            switchedModel =
                true
            VexLogger.record(
                VexLogger.Fact.RuntimeModelSwitched,
                mapOf("from" to priorModel.handle, "to" to binding.model.handle),
            )
        }

        val acceptedPriorArgs =
            prior?.let {
                acceptedPriorServerArgs(
                    receipt = it,
                )
            }
        if (
            !switchedModel &&
            prior != null &&
            acceptedPriorArgs != null &&
            acceptedPriorArgs != args &&
            receiptBinds(
                binding = binding,
                receipt = prior,
                requireImplementationSha = true,
                expectedArgs = acceptedPriorArgs,
            )
        ) {
            if (platform.processAlive(prior.runtime.pid)) {
                requireExactOwnedProcess(
                    prior,
                    acceptedPriorArgs,
                    platform,
                )
            }
            val retired =
                platform.retire(
                    prior,
                )
            require(
                retired.exactOwnershipVerified &&
                    retired.verifiedNotLive,
            ) {
                "changed-config exact Heretic runtime could not be retired before configured restart"
            }
            store.delete()
            runtimeConfigurationRotated = true
            VexLogger.record(
                VexLogger.Fact.RuntimeModelSwitched,
                mapOf(
                    "from" to binding.model.handle,
                    "to" to binding.model.handle,
                    "reason" to "RUNTIME_CONFIGURATION_CHANGED",
                ),
            )
        }
        if (
            !switchedModel &&
            !runtimeConfigurationRotated &&
            prior !=
                null &&
                !receiptBinds(
                    binding = binding,
                    receipt = prior,
                    requireImplementationSha = true,
                ) &&
                acceptedPriorArgs !=
                null &&
                receiptBinds(
                    binding = binding,
                    receipt = prior,
                    requireImplementationSha = false,
                    expectedArgs = acceptedPriorArgs,
                )
        ) {
            if (
                platform.processAlive(
                    prior.runtime.pid,
                )
            ) {
                requireExactOwnedProcess(
                    prior,
                    acceptedPriorArgs,
                    platform,
                )
            }

            val retired =
                platform.retire(
                    prior,
                )
            require(
                retired.exactOwnershipVerified &&
                    retired.verifiedNotLive,
            ) {
                "stale-implementation Heretic runtime could not be retired before current-implementation restart"
            }
            store.delete()
            staleImplementationRotated =
                true
        } else if (
            !switchedModel &&
                !runtimeConfigurationRotated &&
                prior !=
                null &&
                !receiptBinds(
                    binding,
                    prior,
                    requireImplementationSha = true,
                )
        ) {
            error(
                "existing Heretic runtime receipt does not match the current binding",
            )
        }

        val currentPrior =
            if (
                staleImplementationRotated ||
                    runtimeConfigurationRotated ||
                    switchedModel
            ) {
                null
            } else {
                prior
            }

        if (
            currentPrior !=
                null
        ) {
            val pid =
                currentPrior.runtime.pid

            if (
                platform.processAlive(
                    pid,
                )
            ) {
                val currentIdentity =
                    requireExactOwnedProcess(
                        currentPrior,
                        args,
                        platform,
                    )

                if (
                    currentPrior.state ==
                        HereticRuntimeState.Starting
                ) {
                    try {
                        platform.waitUntilHealthy(
                            binding,
                            pid,
                        )
                    } catch (
                        error: Exception,
                    ) {
                        val cleanup =
                            platform.retire(
                                currentPrior,
                            )
                        require(
                            cleanup.exactOwnershipVerified &&
                                cleanup.verifiedNotLive,
                        ) {
                            "STARTING runtime recovery failed and cleanup was not verified"
                        }
                        store.delete()
                        throw error
                    }
                }

                if (
                    currentPrior.state ==
                        HereticRuntimeState.Qualified
                ) {
                    when (
                        platform.generationReadiness(
                            binding,
                        )
                    ) {
                        GenerationReadiness.Available ->
                            Unit

                        GenerationReadiness.Unavailable -> {
                            // Safe lifecycle boundary: exact ownership is already proven and mlx-lm
                            // positively says its generation worker is unavailable. Recover the
                            // working predecessor behavior without treating timeout/ambiguity as
                            // destruction authority.
                            VexLogger.record(
                                VexLogger.Fact.RuntimeGenerationUnavailable,
                                mapOf("pid" to pid.toString()),
                            )
                            val retired =
                                platform.retire(
                                    currentPrior,
                                )
                            require(
                                retired.exactOwnershipVerified &&
                                    retired.verifiedNotLive,
                            ) {
                                "generation-unavailable exact Heretic runtime could not be retired before recovery"
                            }
                            store.delete()
                            generationUnavailableRotated =
                                true
                        }

                        GenerationReadiness.Unknown -> {
                            VexLogger.record(
                                VexLogger.Fact.RuntimeGenerationUnknown,
                                mapOf("pid" to pid.toString()),
                            )
                            error(
                                "qualified Heretic runtime generation readiness is unknown; preserving exact-owned runtime",
                            )
                        }
                    }
                }

                if (
                    !generationUnavailableRotated
                ) {
                    if (
                        platform.cancellationRequested()
                    ) {
                        val cleanup =
                            platform.retire(
                                currentPrior,
                            )
                        require(
                            cleanup.exactOwnershipVerified &&
                                cleanup.verifiedNotLive,
                        ) {
                            "cancelled runtime reuse could not be cleaned up"
                        }
                        store.delete()
                        error(
                            "Heretic runtime start was cancelled",
                        )
                    }

                    val qualification =
                        platform.qualify(
                            binding,
                        )
                    val qualified =
                        formReceipt(
                            binding = binding,
                            pid = pid,
                            identity =
                                currentIdentity,
                            state =
                                HereticRuntimeState.Qualified,
                            formedAt =
                                platform.now(),
                            receiptRef =
                                currentPrior.receiptRef,
                            processSubstrate =
                                currentPrior.runtime.processSubstrate,
                            qualification =
                                qualification,
                        )

                    store.write(
                        qualified,
                    )

                    return StartResult(
                        disposition =
                            if (
                                currentPrior.state ==
                                    HereticRuntimeState.Starting
                            ) {
                                "RECOVERED_EXACT_STARTING_MLX_RUNTIME"
                            } else {
                                "REUSED_EXACT_OWNED_MLX_RUNTIME"
                            },
                        pid = pid,
                        endpoint = qualified.endpoint,
                        requestModel = qualified.requestModel,
                        receipt = qualified,
                        receiptWritePerformed = true,
                        qualification = qualification,
                    )
                }
            } else {
                val retired =
                    platform.retire(
                        currentPrior,
                    )
                require(
                    retired.exactOwnershipVerified &&
                        retired.verifiedNotLive,
                ) {
                    "dead Heretic runtime substrate could not be retired with exact substrate ownership"
                }
                store.delete()
            }
        }

        heldIfCancelled(
            platform,
        )

        require(
            platform
                .matchingProcesses(
                    args,
                )
                .isEmpty(),
        ) {
            "matching mlx_lm.server exists without a current exact receipt"
        }
        require(
            !platform.portOccupied(
                Port,
            ),
        ) {
            "Heretic endpoint is occupied without exact ownership"
        }

        VexLogger.record(VexLogger.Fact.RuntimeStartRequested)
        val spawned =
            platform.spawn(
                binding,
                args,
            )
        require(
            spawned.pid >
                0,
        ) {
            "mlx_lm.server returned no PID"
        }

        val identity =
            waitForExactOwnership(
                spawned,
                args,
                platform,
            )

        if (
            identity ==
                null
        ) {
            val cleanup =
                platform.retireProvisional(
                    spawned,
                    args,
                )
            require(
                cleanup.exactOwnershipVerified &&
                    cleanup.verifiedNotLive,
            ) {
                "provisional Heretic process cleanup was not verified"
            }

            if (
                platform.cancellationRequested()
            ) {
                error(
                    "Heretic runtime start was cancelled before exact process ownership",
                )
            }

            error(
                "new Heretic process ownership could not be proven",
            )
        }

        val receiptRef =
            "receipt.vexlife-prod.heretic-persistent-runtime." +
                sha256(
                    binding.implementationIdentity.classSha256 +
                        "|" +
                        spawned.pid +
                        "|" +
                        platform.now(),
                )
                    .take(
                        24,
                    )

        val starting =
            formReceipt(
                binding = binding,
                pid = spawned.pid,
                identity = identity,
                state = HereticRuntimeState.Starting,
                formedAt = platform.now(),
                receiptRef = receiptRef,
                processSubstrate = spawned.processSubstrate,
                qualification = null,
            )

        try {
            store.write(
                starting,
            )
        } catch (
            error: Exception,
        ) {
            val cleanup =
                platform.retire(
                    starting,
                )
            require(
                cleanup.verifiedNotLive,
            ) {
                "STARTING receipt write failed and exact cleanup was not verified"
            }
            throw error
        }

        val qualification =
            try {
                heldIfCancelled(
                    platform,
                )
                platform.waitUntilHealthy(
                    binding,
                    spawned.pid,
                )
                heldIfCancelled(
                    platform,
                )
                platform.qualify(
                    binding,
                )
            } catch (
                error: Exception,
            ) {
                val cleanup =
                    platform.retire(
                        starting,
                    )
                require(
                    cleanup.verifiedNotLive,
                ) {
                    "qualification failed and exact cleanup was not verified"
                }
                store.delete()
                throw error
            }

        val qualified =
            formReceipt(
                binding = binding,
                pid = spawned.pid,
                identity = identity,
                state = HereticRuntimeState.Qualified,
                formedAt = platform.now(),
                receiptRef = receiptRef,
                processSubstrate = spawned.processSubstrate,
                qualification = qualification,
            )

        store.write(
            qualified,
        )
        // Recorded after qualification passed and the receipt was committed, not at spawn.
        VexLogger.record(VexLogger.Fact.RuntimeQualified, mapOf("pid" to spawned.pid.toString()))

        return StartResult(
            disposition =
                if (
                    switchedModel
                ) {
                    "SWITCHED_MODEL_AND_STARTED_NEW_EXACT_MLX_RUNTIME"
                } else if (
                    runtimeConfigurationRotated
                ) {
                    "ROTATED_RUNTIME_CONFIGURATION_AND_STARTED_NEW_EXACT_MLX_RUNTIME"
                } else if (
                    staleImplementationRotated
                ) {
                    "ROTATED_STALE_IMPLEMENTATION_AND_STARTED_NEW_EXACT_MLX_RUNTIME"
                } else if (
                    generationUnavailableRotated
                ) {
                    "RECOVERED_UNAVAILABLE_GENERATION_AND_STARTED_NEW_EXACT_MLX_RUNTIME"
                } else {
                    "STARTED_NEW_EXACT_MLX_RUNTIME"
                },
            pid = spawned.pid,
            endpoint = qualified.endpoint,
            requestModel = qualified.requestModel,
            receipt = qualified,
            receiptWritePerformed = true,
            qualification = qualification,
        )
    }

    fun stop(
        binding: Binding,
        store: ReceiptStore,
        platform: Platform,
        requireCurrentImplementation: Boolean = true,
    ): StopResult {
        validateBinding(
            binding,
        )

        val receipt =
            store.read()
                ?: return StopResult(
                    disposition = "NO_HERETIC_RUNTIME_RECEIPT",
                    pid = null,
                    processTerminationPerformed = false,
                    verifiedNotLive = true,
                )

        require(
            receiptBinds(
                binding,
                receipt,
                requireImplementationSha =
                    requireCurrentImplementation,
            ),
        ) {
            "exact current Heretic runtime receipt is required to stop"
        }

        if (
            platform.processAlive(
                receipt.runtime.pid,
            )
        ) {
            requireExactOwnedProcess(
                receipt,
                serverArgs(
                    binding,
                ),
                platform,
            )
        }

        val retired =
            platform.retire(
                receipt,
            )
        require(
            retired.exactOwnershipVerified &&
                retired.verifiedNotLive,
        ) {
            "exact Heretic runtime stop could not be verified"
        }

        store.delete()

        return StopResult(
            disposition = "STOPPED_EXACT_OWNED_MLX_RUNTIME",
            pid = receipt.runtime.pid,
            processTerminationPerformed = true,
            verifiedNotLive = true,
        )
    }

    private fun validateBinding(
        binding: Binding,
    ) {
        require(
            binding.schemaVersion ==
                BindingSchema,
        )
        require(
            RefPattern.matches(
                binding.authorityRef,
            ),
        )
        require(
            Path.of(
                binding.modelPath,
            ).isAbsolute,
        )
        require(
            Path.of(
                binding.pythonPath,
            ).isAbsolute,
        )
        require(
            Path.of(
                binding.pythonEnvironmentRoot,
            ).isAbsolute,
        )
        HereticRuntimeConfiguration.requirePromptCacheEntries(
            binding.promptCacheEntryLimit,
        )
    }

    private fun receiptBinds(
        binding: Binding,
        receipt: Receipt,
        requireImplementationSha: Boolean,
        expectedArgs: List<String> =
            serverArgs(
                binding,
            ),
    ): Boolean {
        val identity =
            binding.model.identity
        return receipt.schemaVersion ==
            ReceiptSchema &&
            receipt.authorityRef ==
            binding.authorityRef &&
            receipt.modelRef ==
            identity.modelRef &&
            receipt.modelProfileRef ==
            identity.modelProfileRef &&
            receipt.artifactRef ==
            identity.artifactSetRef &&
            receipt.artifactCustodyEvidenceRef ==
            identity.artifactCustodyEvidenceRef &&
            receipt.artifactContentSetSha256 ==
            identity.artifactContentSetSha256 &&
            receipt.runtimeAdapterRef ==
            identity.runtimeAdapterRef &&
            receipt.runtimeClass ==
            identity.runtimeClass &&
            receipt.transportClass ==
            identity.transportClass &&
            receipt.hostExecutionProfileRef ==
            identity.hostExecutionProfileRef &&
            receipt.pythonVersion ==
            identity.pythonVersion &&
            receipt.mlxLmVersion ==
            identity.mlxLmVersion &&
            receipt.trainerSourceCommit ==
            identity.trainerSourceCommit &&
            receipt.modelPath ==
            binding.modelPath &&
            receipt.pythonPath ==
            binding.pythonPath &&
            receipt.pythonEnvironmentRoot ==
            binding.pythonEnvironmentRoot &&
            receipt.endpoint ==
            identity.endpoint &&
            receipt.requestModel ==
            identity.requestModel &&
            receipt.implementationIdentity.repository ==
            binding.implementationIdentity.repository &&
            receipt.implementationIdentity.classResource ==
            binding.implementationIdentity.classResource &&
            (
                !requireImplementationSha ||
                    receipt.implementationIdentity.classSha256 ==
                    binding.implementationIdentity.classSha256
            ) &&
            receipt.runtime.pid >
            0 &&
            receipt.runtime.processGroupId >
            0 &&
            receipt.runtime.exactOwnedDetachedGroup &&
            receipt.runtime.args ==
            expectedArgs &&
            receipt.runtime.processSubstrate.pid ==
            receipt.runtime.pid &&
            receipt.effects ==
            HereticRuntimeEffectBoundary.QualifiedRuntime
    }

    private fun requireExactOwnedProcess(
        receipt: Receipt,
        args: List<String>,
        platform: Platform,
    ): ProcessIdentity {
        val identity =
            requireNotNull(
                platform.processIdentity(
                    receipt.runtime.pid,
                    args,
                ),
            ) {
                "live Heretic receipt process identity is unavailable"
            }

        require(
            identity.exactOwnedDetachedGroup &&
                identity.processGroupId ==
                receipt.runtime.processGroupId &&
                identity.pid ==
                receipt.runtime.pid &&
                platform.processSubstrateCurrent(
                    receipt.runtime.processSubstrate,
                    receipt.runtime.pid,
                ),
        ) {
            "live receipt process is not the exact owned Heretic process/lifetime substrate"
        }

        return identity
    }

    private fun waitForExactOwnership(
        spawned: Spawned,
        args: List<String>,
        platform: Platform,
    ): ProcessIdentity? {
        val deadline =
            platform.nowMs() +
                OwnershipTimeoutMs

        while (
            platform.nowMs() <
            deadline
        ) {
            if (
                platform.cancellationRequested()
            ) {
                return null
            }

            if (
                !platform.processAlive(
                    spawned.pid,
                )
            ) {
                return null
            }

            val identity =
                platform.processIdentity(
                    spawned.pid,
                    args,
                )

            if (
                identity !=
                    null &&
                    identity.exactOwnedDetachedGroup &&
                    identity.pid ==
                    spawned.pid &&
                    platform.processSubstrateCurrent(
                        spawned.processSubstrate,
                        spawned.pid,
                    )
            ) {
                return identity
            }

            platform.sleep(
                OwnershipPollMs,
            )
        }

        return null
    }

    private fun formReceipt(
        binding: Binding,
        pid: Long,
        identity: ProcessIdentity,
        state: HereticRuntimeState,
        formedAt: String,
        receiptRef: String,
        processSubstrate: MacUserLaunchdProcess.Receipt,
        qualification: HereticRuntimeQualification?,
        argsOverride: List<String>? = null,
    ): Receipt =
        Receipt(
            schemaVersion = ReceiptSchema,
            receiptRef = receiptRef,
            state = state,
            formedAt = formedAt,
            authorityRef = binding.authorityRef,
            modelRef = binding.model.identity.modelRef,
            modelProfileRef = binding.model.identity.modelProfileRef,
            artifactRef = binding.model.identity.artifactSetRef,
            artifactCustodyEvidenceRef = binding.model.identity.artifactCustodyEvidenceRef,
            artifactContentSetSha256 = binding.model.identity.artifactContentSetSha256,
            runtimeAdapterRef = binding.model.identity.runtimeAdapterRef,
            runtimeClass = binding.model.identity.runtimeClass,
            transportClass = binding.model.identity.transportClass,
            hostExecutionProfileRef = binding.model.identity.hostExecutionProfileRef,
            pythonVersion = binding.model.identity.pythonVersion,
            mlxLmVersion = binding.model.identity.mlxLmVersion,
            trainerSourceCommit = binding.model.identity.trainerSourceCommit,
            modelPath = binding.modelPath,
            pythonPath = binding.pythonPath,
            pythonEnvironmentRoot = binding.pythonEnvironmentRoot,
            endpoint = binding.model.identity.endpoint,
            requestModel = binding.model.identity.requestModel,
            implementationIdentity = binding.implementationIdentity,
            runtime =
                RuntimeProcess(
                    pid = pid,
                    processGroupId = identity.processGroupId,
                    exactOwnedDetachedGroup = identity.exactOwnedDetachedGroup,
                    args =
                        argsOverride
                            ?: serverArgs(
                                binding,
                            ),
                    processSubstrate = processSubstrate,
                ),
            effects = HereticRuntimeEffectBoundary.QualifiedRuntime,
            qualification = qualification,
        )

    private fun heldIfCancelled(
        platform: Platform,
    ) {
        if (
            platform.cancellationRequested()
        ) {
            error(
                "Heretic runtime start was cancelled",
            )
        }
    }

    private fun absolute(
        value: String,
        label: String,
    ): Path {
        val path =
            Path.of(
                value,
            )
        require(
            path.isAbsolute,
        ) {
            "$label must be absolute"
        }
        return path.normalize()
    }

    private fun sha256(
        value: ByteArray,
    ): String =
        MessageDigest
            .getInstance(
                "SHA-256",
            )
            .digest(
                value,
            )
            .joinToString(
                "",
            ) {
                "%02x".format(
                    it.toInt() and
                        0xff,
                )
            }

    private fun sha256(
        value: String,
    ): String =
        MessageDigest
            .getInstance(
                "SHA-256",
            )
            .digest(
                value.toByteArray(
                    StandardCharsets.UTF_8,
                ),
            )
            .joinToString(
                "",
            ) {
                "%02x".format(
                    it.toInt() and
                        0xff,
                )
            }
}

// [VXG RealForever]
