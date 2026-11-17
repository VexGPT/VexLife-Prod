package vexlife.model.heretic

/**
 * Source-owned configuration topology for Heretic runtime realization.
 *
 * This is the single map from one current Product model choice + one exact platform target to the
 * dependencies that realize that choice. Callers must not infer a realization from class names,
 * artifact suffixes, runtime strings, or field-comparison heuristics.
 *
 * Static configuration belongs here. Dynamic State / Control / Effect meaning remains with
 * [HereticRuntimeAvailability]. Runtime/process owners execute the dependencies selected here.
 */
object HereticRuntimeRealization {
    enum class Platform {
        MACOS,
        WINDOWS,
    }

    enum class Architecture {
        ARM64,
        X86_64,
    }

    data class Target(
        val platform: Platform,
        val architecture: Architecture,
    ) {
        val targetRef: String =
            "target.vexlife-prod." +
                platform.name.lowercase() +
                "." +
                architecture.name.lowercase()
    }

    sealed interface Requirement<out T> {
        data class Required<T>(
            val value: T,
        ) : Requirement<T>

        data class NotRequired(
            val reasonRef: String,
        ) : Requirement<Nothing> {
            init {
                require(
                    reasonRef.matches(
                        Regex(
                            "[A-Za-z0-9][A-Za-z0-9._:/-]*",
                        ),
                    ),
                ) {
                    "not-required reasonRef must be one stable ref"
                }
            }
        }
    }

    data class ArtifactProfile(
        val artifactRef: String,
        val contentSetSha256: String,
        val providerRepository: String,
        val providerRevision: String,
        val primaryFilename: String?,
        val expectedPrimaryBytes: Long?,
    )

    data class EnvironmentProfile(
        val environmentRef: String,
    )

    data class RuntimeBinaryProfile(
        val binaryRef: String,
        val filename: String,
        val sha256: String,
    )

    data class RuntimeProfile(
        val runtimeAdapterRef: String,
        val runtimeClass: String,
        val transportClass: String,
        val endpoint: String,
        val requestModel: String,
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

    data class Dependencies(
        val artifact: Requirement<ArtifactProfile>,
        val environment: Requirement<EnvironmentProfile>,
        val runtimeBinary: Requirement<RuntimeBinaryProfile>,
        val runtime: Requirement<RuntimeProfile>,
        val process: Requirement<ProcessProfile>,
        val qualification: Requirement<QualificationProfile>,
    )

    data class Value(
        val realizationRef: String,
        val modelRef: String,
        val modelProfileRef: String,
        val target: Target,
        val runtimeReceiptRelativePath: String,
        val dependencies: Dependencies,
    ) {
        init {
            require(
                realizationRef.matches(
                    Regex(
                        "[A-Za-z0-9][A-Za-z0-9._:/-]*",
                    ),
                ) &&
                    modelRef.isNotBlank() &&
                    modelProfileRef.isNotBlank() &&
                    runtimeReceiptRelativePath.isNotBlank(),
            ) {
                "runtime realization identity is invalid"
            }
        }

        val artifact: ArtifactProfile
            get() = requireRequired(dependencies.artifact, "artifact")

        val runtime: RuntimeProfile
            get() = requireRequired(dependencies.runtime, "runtime")

        val process: ProcessProfile
            get() = requireRequired(dependencies.process, "process")

        val qualification: QualificationProfile
            get() = requireRequired(dependencies.qualification, "qualification")
    }

    val MacArm64 =
        Target(
            platform = Platform.MACOS,
            architecture = Architecture.ARM64,
        )

    val WindowsX64 =
        Target(
            platform = Platform.WINDOWS,
            architecture = Architecture.X86_64,
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
            runtimeReceiptRelativePath = HereticProductIdentity.ProductRuntimeReceiptRelativePath,
            dependencies =
                Dependencies(
                    artifact =
                        Requirement.Required(
                            ArtifactProfile(
                                artifactRef = identity.artifactSetRef,
                                contentSetSha256 = identity.artifactContentSetSha256,
                                providerRepository = model.providerRepository,
                                providerRevision = model.providerRevision,
                                primaryFilename = null,
                                expectedPrimaryBytes = null,
                            ),
                        ),
                    environment =
                        Requirement.Required(
                            EnvironmentProfile(
                                environmentRef =
                                    "environment.vexlife-prod.python-mlx." +
                                        HereticProductIdentity.PythonVersion +
                                        "." +
                                        HereticProductIdentity.MlxLmVersion,
                            ),
                        ),
                    runtimeBinary =
                        Requirement.NotRequired(
                            reasonRef =
                                "not-required.vexlife-prod.macos-mlx-runtime-binary-owned-by-python-environment",
                        ),
                    runtime =
                        Requirement.Required(
                            RuntimeProfile(
                                runtimeAdapterRef = identity.runtimeAdapterRef,
                                runtimeClass = identity.runtimeClass,
                                transportClass = identity.transportClass,
                                endpoint = identity.endpoint,
                                requestModel = identity.requestModel,
                            ),
                        ),
                    process =
                        Requirement.Required(
                            ProcessProfile(
                                processSubstrateRef =
                                    "process-substrate.vexlife-prod.macos.launchd-user",
                                lifetimeOwnerRef =
                                    "owner.vexlife-prod.macos-launchd-process",
                            ),
                        ),
                    qualification =
                        Requirement.Required(
                            QualificationProfile(
                                qualificationRef =
                                    "qualification.vexlife-prod.heretic.mlx-openai.v1",
                                healthPath = "/health",
                                modelsPath = "/v1/models",
                                completionPath = "/v1/chat/completions",
                                modelExposure = ModelExposure.EXACT_MATERIALIZATION_PATH,
                                maxTokens = 1,
                                temperature = 0.0,
                                enableThinking = false,
                                stream = false,
                            ),
                        ),
                ),
        )
    }

    /**
     * Mac coverage is explicit. A new offered model must deliberately enter this list rather than
     * inheriting Mac support merely because it exists in the model catalog.
     */
    private val MacEntries =
        listOf(
            HereticModelCatalog.Certified27B,
            HereticModelCatalog.Candidate9B,
            HereticModelCatalog.Candidate4B,
        )
            .map(::macMlx)

    val Windows4B =
        Value(
            realizationRef =
                "realization.vexlife-prod.heretic-4b.windows-x64.llama-cpp.patient-zero.001",
            modelRef = HereticModelCatalog.Candidate4B.modelRef,
            modelProfileRef = HereticModelCatalog.Candidate4B.identity.modelProfileRef,
            target = WindowsX64,
            runtimeReceiptRelativePath =
                "runtime/vexlife-prod/windows-4b-patient-zero-runtime-receipt.json",
            dependencies =
                Dependencies(
                    artifact =
                        Requirement.Required(
                            ArtifactProfile(
                                artifactRef =
                                    "artifact.vexlife-prod.qwen3.8-4b-distill-heretic-gguf-q4-k-m.c0807aa5",
                                contentSetSha256 =
                                    "7b95847ce1684f98d96f13bad2daaeffcfd2f95ea2ff3821ee68943590a10159",
                                providerRepository =
                                    "yachen4ever/Qwen3.8-4B-Distill-Heretic-Abliterated-GGUF",
                                providerRevision =
                                    "c0807aa5c3a6e689a4dda5db58ab1d10eb40ab61",
                                primaryFilename = "model-Q4_K_M.gguf",
                                expectedPrimaryBytes = 2_790_000_800L,
                            ),
                        ),
                    environment =
                        Requirement.NotRequired(
                            reasonRef =
                                "not-required.vexlife-prod.windows-4b.native-llama-cpp-no-python-environment",
                        ),
                    runtimeBinary =
                        Requirement.Required(
                            RuntimeBinaryProfile(
                                binaryRef =
                                    "runtime-binary.vexlife-prod.llama-cpp.windows-x64.b10107",
                                filename =
                                    "llama-server.exe",
                                sha256 =
                                    "af3e56d6bdb84a9b6bd50f6ad748809370bc29be42d58088041408b2933d80f7",
                            ),
                        ),
                    runtime =
                        Requirement.Required(
                            RuntimeProfile(
                                runtimeAdapterRef =
                                    "adapter.runtime.llama-cpp.windows-x64.cuda.patient-zero.001",
                                runtimeClass = "LLAMA_CPP_OPENAI_RUNTIME",
                                transportClass = "LLAMA_CPP_OPENAI_NUMERIC_LOOPBACK",
                                endpoint = "http://127.0.0.1:18080",
                                requestModel = "default_model",
                            ),
                        ),
                    process =
                        Requirement.Required(
                            ProcessProfile(
                                processSubstrateRef =
                                    "process-substrate.vexlife-prod.windows.local-process.patient-zero",
                                lifetimeOwnerRef =
                                    "owner.vexlife-prod.windows-4b-patient-zero-runtime",
                            ),
                        ),
                    qualification =
                        Requirement.Required(
                            QualificationProfile(
                                qualificationRef =
                                    "qualification.vexlife-prod.heretic.llama-cpp-openai.v1",
                                healthPath = "/health",
                                modelsPath = "/v1/models",
                                completionPath = "/v1/chat/completions",
                                modelExposure = ModelExposure.EXACT_REQUEST_MODEL_ALIAS,
                                maxTokens = 1,
                                temperature = 0.0,
                                enableThinking = false,
                                stream = false,
                            ),
                        ),
                ),
        )

    val Entries: List<Value> =
        (MacEntries + Windows4B)
            .also { entries ->
                require(
                    entries.map { it.realizationRef }.distinct().size == entries.size,
                ) {
                    "runtime realization refs must be unique"
                }
                require(
                    entries.map { it.modelRef to it.target }.distinct().size == entries.size,
                ) {
                    "runtime realization model/target coordinates must be unique"
                }
            }

    fun resolve(
        modelRef: String,
        target: Target,
    ): Value? =
        Entries.firstOrNull {
            it.modelRef == modelRef &&
                it.target == target
        }

    fun require(
        modelRef: String,
        target: Target,
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
                target.targetRef
        }

    fun require(
        realizationRef: String,
    ): Value =
        requireNotNull(
            Entries.firstOrNull {
                it.realizationRef == realizationRef
            },
        ) {
            "Heretic runtime realization is not configured: " + realizationRef
        }

    /**
     * v1 Product bindings predate realizationRef. That schema only admitted the source-known Mac
     * Product runtime, so compatibility resolves modelRef -> exact Mac target. It does not infer a
     * platform from runtime/artifact strings.
     */
    fun legacyMacV1(
        modelRef: String,
    ): Value =
        require(
            modelRef = modelRef,
            target = MacArm64,
        )

    fun receiptPaths(): Set<String> =
        Entries.map { it.runtimeReceiptRelativePath }.toSet()

    private fun <T> requireRequired(
        requirement: Requirement<T>,
        label: String,
    ): T =
        when (requirement) {
            is Requirement.Required -> requirement.value
            is Requirement.NotRequired ->
                error(
                    label +
                        " is explicitly not required for this realization: " +
                        requirement.reasonRef,
                )
        }
}

// [VXG RealForever]
