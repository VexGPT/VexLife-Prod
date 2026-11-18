package vexlife.model.heretic

import vexlife.platform.VexLifePlatformRealization

/**
 * Exact runtime realization for one Product model on one platform target.
 *
 * The model catalog answers what models VexLife offers. [VexLifePlatformRealization] answers what
 * host target was selected. This owner answers the missing composition question: which exact model
 * artifact/runtime/process relationship realizes that model on that target.
 *
 * Presence in [HereticModelCatalog] never implies support on every target. Resolution is exact only;
 * no nearest-target, model or platform fallback exists.
 */
object HereticRuntimeRealization {
    private val PortableRef =
        Regex(
            "[a-z][a-z0-9._-]*",
        )

    data class ArtifactProfile(
        val artifactRef: String,
        val providerRepository: String,
        val providerRevision: String,
        val primaryFilename: String?,
        val expectedPrimaryBytes: Long?,
        val contentSetSha256: String,
        val materializationRootRelativePath: String,
        val directHttpsUrl: String?,
    )

    data class RuntimeBinaryProfile(
        val binaryRef: String,
        val providerRepository: String,
        val providerReleaseTag: String,
        val providerSourceCommit: String,
        val providerPackagingSourceRef: String,
        val archiveFilename: String,
        val archiveBytes: Long,
        val archiveSha256: String,
        val archiveUrl: String,
        val executableRelativePath: String,
        val executableSha256: String,
        val materializationRootRelativePath: String,
    )

    data class RuntimeLaunchProfile(
        val contextTokens: Int,
        val jinja: Boolean,
        val gpuOffloadLayers: Int?,
    ) {
        init {
            require(contextTokens > 0) {
                "runtime launch context must be positive"
            }
            require(gpuOffloadLayers == null || gpuOffloadLayers >= 0) {
                "runtime GPU offload layers cannot be negative"
            }
        }
    }

    data class RuntimeProfile(
        val runtimeAdapterRef: String,
        val runtimeClass: String,
        val transportClass: String,
        val endpoint: String,
        val requestModel: String,
        val livedLaunchProfile: RuntimeLaunchProfile?,
    )

    data class ProcessProfile(
        val processSubstrateRef: String,
        val lifetimeOwnerRef: String,
    )

    enum class ModelExposure {
        EXACT_MATERIALIZATION_PATH,
        EXACT_REQUEST_MODEL_ALIAS,
    }

    data class QualificationProfile(
        val qualificationRef: String,
        val healthPath: String,
        val modelsPath: String,
        val completionPath: String,
        val modelExposure: ModelExposure,
        val maxTokens: Int,
        val temperature: Double,
        val enableThinking: Boolean,
        val stream: Boolean,
    )

    data class Value(
        val realizationRef: String,
        val modelRef: String,
        val modelProfileRef: String,
        val target: VexLifePlatformRealization.Target,
        val artifact: ArtifactProfile,
        val runtimeBinary: RuntimeBinaryProfile?,
        val runtime: RuntimeProfile,
        /** Runtime-configuration relationship admitted for this exact realization, when one exists. */
        val runtimeConfigurationProfileRef: String?,
        val process: ProcessProfile,
        val qualification: QualificationProfile,
        val runtimeReceiptRelativePath: String,
    ) {
        init {
            require(realizationRef.matches(PortableRef)) {
                "runtime realization ref must be portable"
            }
            require(HereticModelCatalog.entry(modelRef)?.identity?.modelProfileRef == modelProfileRef) {
                "runtime realization model/profile relationship is not current"
            }
            require(runtimeReceiptRelativePath.isNotBlank()) {
                "runtime realization receipt path must be non-blank"
            }
        }

        val targetRef: String
            get() = targetRef(target)
    }

    val MacArm64 =
        VexLifePlatformRealization.Target(
            platform = VexLifePlatformRealization.PlatformFamily.MACOS,
            architecture = VexLifePlatformRealization.Architecture.ARM64,
        )

    val WindowsX64 =
        VexLifePlatformRealization.Target(
            platform = VexLifePlatformRealization.PlatformFamily.WINDOWS,
            architecture = VexLifePlatformRealization.Architecture.X86_64,
        )

    private val MacMlxRuntime =
        RuntimeProfile(
            // Current runtime behavior is declared here deliberately. The parity test against
            // HereticProductIdentity prevents either owner from drifting silently.
            runtimeAdapterRef =
                "adapter.runtime.mlx.macos-victor.001",
            runtimeClass =
                "MLX_LM_DIRECT_RUNTIME",
            transportClass =
                "MLX_LM_OPENAI_NUMERIC_LOOPBACK",
            endpoint =
                "http://127.0.0.1:8765",
            requestModel =
                "default_model",
            livedLaunchProfile =
                null,
        )

    private fun openAiLoopbackQualification(
        qualificationRef: String,
        modelExposure: ModelExposure,
    ) =
        QualificationProfile(
            qualificationRef = qualificationRef,
            healthPath = "/health",
            modelsPath = "/v1/models",
            completionPath = "/v1/chat/completions",
            modelExposure = modelExposure,
            maxTokens = 1,
            temperature = 0.0,
            enableThinking = false,
            stream = false,
        )

    private fun macMlx(
        model: HereticModelCatalog.Entry,
    ): Value {
        val identity = model.identity
        return Value(
            realizationRef =
                "realization.vexlife-prod." +
                    model.handle +
                    ".macos-arm64.mlx.001",
            modelRef = model.modelRef,
            modelProfileRef = identity.modelProfileRef,
            target = MacArm64,
            artifact =
                ArtifactProfile(
                    artifactRef = identity.artifactSetRef,
                    providerRepository = model.providerRepository,
                    providerRevision = model.providerRevision,
                    primaryFilename = null,
                    expectedPrimaryBytes = null,
                    contentSetSha256 = identity.artifactContentSetSha256,
                    materializationRootRelativePath =
                        model.modelRootRelativePath + "/" + model.providerRevision,
                    directHttpsUrl = null,
                ),
            runtimeBinary = null,
            runtime = MacMlxRuntime,
            runtimeConfigurationProfileRef =
                HereticRuntimeConfiguration.MlxPromptCacheProfileRef,
            process =
                ProcessProfile(
                    processSubstrateRef =
                        "substrate.vexlife.macos-user-launchd-process.001",
                    lifetimeOwnerRef =
                        "owner.vexlife-prod.heretic-persistent-runtime.001",
                ),
            qualification =
                openAiLoopbackQualification(
                    qualificationRef =
                        "qualification.vexlife-prod.heretic.mlx-openai.v1",
                    modelExposure =
                        ModelExposure.EXACT_MATERIALIZATION_PATH,
                ),
            runtimeReceiptRelativePath =
                "runtime/vexlife-prod/heretic-persistent-runtime-owner-receipt.json",
        )
    }

    private val MacEntries =
        HereticModelCatalog.Entries.map(::macMlx)

    val WindowsQwen4B =
        Value(
            realizationRef =
                "realization.vexlife-prod.heretic-4b.windows-x64.llama-cpp.cpu.b10107.001",
            modelRef =
                HereticModelCatalog.Models.Qwen4B.modelRef,
            modelProfileRef =
                HereticModelCatalog.Models.Qwen4B.identity.modelProfileRef,
            target =
                WindowsX64,
            artifact =
                ArtifactProfile(
                    artifactRef =
                        "artifact.vexlife-prod.qwen3.8-4b-distill-heretic-gguf-q4-k-m.c0807aa5",
                    providerRepository =
                        "yachen4ever/Qwen3.8-4B-Distill-Heretic-Abliterated-GGUF",
                    providerRevision =
                        "c0807aa5c3a6e689a4dda5db58ab1d10eb40ab61",
                    primaryFilename =
                        "model-Q4_K_M.gguf",
                    expectedPrimaryBytes =
                        2_790_000_800L,
                    contentSetSha256 =
                        "7b95847ce1684f98d96f13bad2daaeffcfd2f95ea2ff3821ee68943590a10159",
                    materializationRootRelativePath =
                        "models/heretic/qwen3.8-4b-distill-gguf/" +
                            "c0807aa5c3a6e689a4dda5db58ab1d10eb40ab61",
                    directHttpsUrl =
                        "https://huggingface.co/" +
                            "yachen4ever/Qwen3.8-4B-Distill-Heretic-Abliterated-GGUF/" +
                            "resolve/c0807aa5c3a6e689a4dda5db58ab1d10eb40ab61/" +
                            "model-Q4_K_M.gguf",
                ),
            runtimeBinary =
                RuntimeBinaryProfile(
                    binaryRef =
                        "runtime-binary.vexlife-prod.llama-cpp.windows-x64.cpu.b10107",
                    providerRepository =
                        "ggml-org/llama.cpp",
                    providerReleaseTag =
                        "b10107",
                    providerSourceCommit =
                        "c0bc8591e8815c63cb01dd3f051a8b0df02501c9",
                    providerPackagingSourceRef =
                        "ggml-org/llama.cpp@c0bc8591e8815c63cb01dd3f051a8b0df02501c9:" +
                            ".github/workflows/release.yml#windows-cpu",
                    archiveFilename =
                        "llama-b10107-bin-win-cpu-x64.zip",
                    archiveBytes =
                        18_213_827L,
                    archiveSha256 =
                        "52133a0a5a8f6035b1bdd2f89c3425ea8b742413d9bdb9a2dee30e3a1681b18c",
                    archiveUrl =
                        "https://github.com/ggml-org/llama.cpp/releases/download/b10107/" +
                            "llama-b10107-bin-win-cpu-x64.zip",
                    executableRelativePath =
                        "llama-server.exe",
                    executableSha256 =
                        "af3e56d6bdb84a9b6bd50f6ad748809370bc29be42d58088041408b2933d80f7",
                    materializationRootRelativePath =
                        "runtime/vexlife-prod/llama-cpp/b10107/windows-x64-cpu",
                ),
            runtime =
                RuntimeProfile(
                    runtimeAdapterRef =
                        "adapter.runtime.llama-cpp.windows-x64.cpu.b10107.001",
                    runtimeClass =
                        "LLAMA_CPP_OPENAI_RUNTIME",
                    transportClass =
                        "LLAMA_CPP_OPENAI_NUMERIC_LOOPBACK",
                    endpoint =
                        "http://127.0.0.1:18080",
                    requestModel =
                        "default_model",
                    livedLaunchProfile =
                        RuntimeLaunchProfile(
                            contextTokens =
                                16_384,
                            jinja =
                                true,
                            gpuOffloadLayers =
                                null,
                        ),
                ),
            runtimeConfigurationProfileRef =
                null,
            process =
                ProcessProfile(
                    processSubstrateRef =
                        "substrate.vexlife.windows-local-process.001",
                    lifetimeOwnerRef =
                        "owner.vexlife-prod.windows-heretic-runtime.001",
                ),
            qualification =
                openAiLoopbackQualification(
                    qualificationRef =
                        "qualification.vexlife-prod.heretic.llama-cpp-openai.v1",
                    modelExposure =
                        ModelExposure.EXACT_REQUEST_MODEL_ALIAS,
                ),
            runtimeReceiptRelativePath =
                "runtime/vexlife-prod/heretic-windows-runtime-owner-receipt.json",
        )

    val Entries: List<Value> =
        (MacEntries + WindowsQwen4B)
            .also { entries ->
                require(entries.map { it.realizationRef }.distinct().size == entries.size) {
                    "runtime realization refs must be unique"
                }
                require(entries.map { it.modelRef to it.target }.distinct().size == entries.size) {
                    "runtime realization model/target coordinates must be unique"
                }
            }

    /**
     * Stable Product-facing result of resolving one selected model against one freshly observed
     * platform target.
     *
     * Callers do not branch on Mac/Windows. Construction supplies [TargetSource]; this owner returns
     * the exact source-declared realization or an explicit unsupported relationship.
     */
    sealed interface Resolution {
        val modelRef: String
        val target: VexLifePlatformRealization.Target

        data class Supported(
            val realization: Value,
        ) : Resolution {
            override val modelRef: String =
                realization.modelRef
            override val target: VexLifePlatformRealization.Target =
                realization.target
        }

        data class Unsupported(
            override val modelRef: String,
            override val target: VexLifePlatformRealization.Target,
        ) : Resolution
    }

    /**
     * No-effect source for the current exact platform target.
     *
     * Host/platform observation remains owned outside this model/runtime topology. A JVM, Android or
     * future host adapter can supply this interface without making downstream Product consumers know
     * which platform produced the target.
     */
    fun interface TargetSource {
        fun current(): VexLifePlatformRealization.Target
    }

    /**
     * Reusable cross-platform resolution interface.
     *
     * The same Resolver instance can be used across fresh target observations. It never caches a
     * platform decision and never falls back to another target.
     */
    class Resolver(
        private val targetSource: TargetSource,
    ) {
        fun currentTarget(): VexLifePlatformRealization.Target =
            targetSource.current()

        fun supportedModels(): List<HereticModelCatalog.Entry> =
            modelsFor(
                currentTarget(),
            )

        /**
         * Target-aware Setup recommendation.
         *
         * Catalog offering, target support and resource fit remain separate facts. Recommendation
         * can only choose from models that have an exact realization on the freshly observed target.
         */
        fun recommendedFor(
            totalMemoryBytes: Long?,
        ): HereticModelCatalog.Entry {
            val supported =
                supportedModels()
            require(
                supported.isNotEmpty(),
            ) {
                "current target has no admitted Heretic runtime realization"
            }
            val startable =
                supported.filter {
                    it.autoRecommended
                }
            require(
                startable.isNotEmpty(),
            ) {
                "current target has no auto-recommended Heretic runtime realization"
            }

            if (
                totalMemoryBytes ==
                    null
            ) {
                return HereticModelCatalog.Default
                    .takeIf {
                        it in
                            startable
                    }
                    ?: startable.minBy {
                        it.recommendedMemoryBytes
                    }
            }

            return startable.firstOrNull {
                HereticModelCatalog.fits(
                    it,
                    totalMemoryBytes,
                )
            }
                ?: startable.minBy {
                    it.recommendedMemoryBytes
                }
        }

        fun resolve(
            model: HereticModelCatalog.Entry,
        ): Resolution =
            resolve(
                model.modelRef,
            )

        fun resolve(
            modelRef: String,
        ): Resolution =
            resolutionFor(
                modelRef = modelRef,
                target = currentTarget(),
            )
    }

    fun resolutionFor(
        modelRef: String,
        target: VexLifePlatformRealization.Target,
    ): Resolution {
        HereticModelCatalog.requireEntry(
            modelRef,
        )

        return resolve(
            modelRef = modelRef,
            target = target,
        )
            ?.let(
                Resolution::Supported,
            )
            ?: Resolution.Unsupported(
                modelRef = modelRef,
                target = target,
            )
    }

    fun resolve(
        modelRef: String,
        target: VexLifePlatformRealization.Target,
    ): Value? =
        Entries.firstOrNull {
            it.modelRef == modelRef &&
                it.target == target
        }

    fun require(
        modelRef: String,
        target: VexLifePlatformRealization.Target,
    ): Value =
        requireNotNull(
            resolve(
                modelRef = modelRef,
                target = target,
            ),
        ) {
            "no Heretic runtime realization is configured for model=" +
                modelRef +
                " target=" +
                targetRef(target)
        }

    fun supports(
        modelRef: String,
        target: VexLifePlatformRealization.Target,
    ): Boolean =
        resolve(
            modelRef = modelRef,
            target = target,
        ) != null

    fun modelsFor(
        target: VexLifePlatformRealization.Target,
    ): List<HereticModelCatalog.Entry> =
        HereticModelCatalog.Entries.filter {
            supports(
                modelRef = it.modelRef,
                target = target,
            )
        }

    fun targetRef(
        target: VexLifePlatformRealization.Target,
    ): String =
        "target.vexlife-prod." +
            target.platform.name.lowercase() +
            "." +
            target.architecture.name.lowercase()

}

// [VXG RealForever]
