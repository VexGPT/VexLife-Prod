package vexlife.model.heretic

/**
 * Faithful, effect-inert product identity for the current certified Heretic model relationship.
 *
 * This package intentionally owns only model identity, certified artifact composition, and the
 * Home-scoped configuration relationship. Artifact acquisition, Python/MLX environment creation,
 * process lifetime, health qualification, and stop/recovery effects are platform/runtime owners
 * and must remain outside this source.
 *
 * The values here were ported from the current VexLife Heretic runtime-link, establishment,
 * model-bundle, and artifact registries. They are product source after acceptance; the historical
 * VexLife locations are provenance, not runtime currentness dependencies.
 */
object HereticProductModel {
    const val ModelConfigurationSchema =
        "vexlife.heretic-model-configuration/v1"

    const val ModelBundleSchema =
        "vexlife.model-bundle/v2"

    const val ModelBundleRef =
        "model-bundle.vexlife.heretic.qwen3.8-27b.mlx4.329658ee8f27.001"

    const val GenerationRef =
        "generation.devex.heretic27b4.preexisting-certified-foundation.normalized.20261001A"

    const val ModelRef =
        "model.devex.heretic.qwen3.8-27b.mlx4.329658ee8f27"

    const val ModelProfileRef =
        "model-profile.devex.qwen38-27b-heretic-mlx4.certified.20260928A"

    const val ArtifactSetRef =
        "artifact.devex.qwen3.8-27b-heretic-mlx-4bit.808b82a2"

    const val ArtifactCustodyEvidenceRef =
        "resolution.model-preservation.artifact-custody.devex.qwen38-27b-heretic-mlx4.20260928A"

    const val ArtifactContentSetSha256 =
        "329658ee8f270056bc29d8a6cfd6e9f1b7d9d14c659452c9e4dd46fc7c78d780"

    const val RuntimeAdapterRef =
        "adapter.runtime.mlx.macos-victor.001"

    const val RuntimeClass =
        "MLX_LM_DIRECT_RUNTIME"

    const val TransportClass =
        "MLX_LM_OPENAI_NUMERIC_LOOPBACK"

    /**
     * Current VexLife-owned product host profile.
     *
     * The historical runtime-link also recognizes the prior SDK profile for legacy receipt
     * compatibility. The fresh product contract does not collapse that legacy identity into the
     * current product identity.
     */
    const val HostExecutionProfileRef =
        "profile.repository.vexlife.macos-v1"

    const val PythonVersion =
        "3.14.7"

    const val MlxLmVersion =
        "0.32.0"

    const val TrainerSourceCommit =
        "170a11c58ce7d22a720901a2535adb0c218b871b"

    const val Endpoint =
        "http://127.0.0.1:8765"

    const val RequestModel =
        "default_model"

    const val RuntimeReceiptRelativePath =
        "runtime/heretic/persistent-runtime-owner-receipt.json"

    const val ReleaseState =
        "RELEASE_QUALIFIED"

    const val CertifiedArtifactMemberCount =
        11

    const val CertifiedArtifactBytes =
        15_153_242_506L

    const val ProviderSourceRef =
        "hf.donedynamics.qwen3.8-27b-heretic-mlx-4bit.808b82a2bd04b2bac362b5d0b7a1197aeea59e57"

    const val LicenseRef =
        "license.apache-2.0.qwen3.8-heretic"

    data class Identity(
        val modelBundleRef: String,
        val generationRef: String,
        val modelRef: String,
        val modelProfileRef: String,
        val artifactSetRef: String,
        val artifactCustodyEvidenceRef: String,
        val artifactContentSetSha256: String,
        val runtimeAdapterRef: String,
        val runtimeClass: String,
        val transportClass: String,
        val hostExecutionProfileRef: String,
        val pythonVersion: String,
        val mlxLmVersion: String,
        val trainerSourceCommit: String,
        val endpoint: String,
        val requestModel: String,
        val runtimeReceiptRelativePath: String,
    )

    val CurrentIdentity =
        Identity(
            modelBundleRef = ModelBundleRef,
            generationRef = GenerationRef,
            modelRef = ModelRef,
            modelProfileRef = ModelProfileRef,
            artifactSetRef = ArtifactSetRef,
            artifactCustodyEvidenceRef = ArtifactCustodyEvidenceRef,
            artifactContentSetSha256 = ArtifactContentSetSha256,
            runtimeAdapterRef = RuntimeAdapterRef,
            runtimeClass = RuntimeClass,
            transportClass = TransportClass,
            hostExecutionProfileRef = HostExecutionProfileRef,
            pythonVersion = PythonVersion,
            mlxLmVersion = MlxLmVersion,
            trainerSourceCommit = TrainerSourceCommit,
            endpoint = Endpoint,
            requestModel = RequestModel,
            runtimeReceiptRelativePath = RuntimeReceiptRelativePath,
        )

    enum class ConfigurationState(
        val wireValue: String,
    ) {
        EstablishmentPending(
            "HERETIC_ESTABLISHMENT_PENDING",
        ),
        BoundProductModel(
            "BOUND_HERETIC_PRODUCT_MODEL",
        ),
    }

    data class HomeIdentity(
        val homeRef: String,
        val companionLineageRef: String,
    ) {
        init {
            requirePortableRef(
                homeRef,
                "homeRef",
            )
            requirePortableRef(
                companionLineageRef,
                "companionLineageRef",
            )
        }
    }

    /**
     * Home-scoped product selection.
     *
     * These authority flags remain false by construction. Selecting Heretic therefore cannot
     * silently grant fallback, artifact acquisition, or runtime activation authority.
     */
    data class Configuration(
        val schemaVersion: String,
        val state: ConfigurationState,
        val homeRef: String,
        val companionLineageRef: String,
        val modelRef: String,
        val modelProfileRef: String,
        val artifactRef: String,
        val artifactContentSetSha256: String,
        val runtimeAdapterRef: String,
        val runtimeClass: String,
        val endpoint: String,
        val requestModel: String,
        val runtimeReceiptPath: String,
        val automaticFallback: Boolean,
        val automaticDownload: Boolean,
        val automaticActivation: Boolean,
    )

    data class ArtifactMember(
        val roleRef: String,
        val artifactRef: String,
        val filename: String,
        val mediaType: String,
        val sha256: String,
        val expectedBytes: Long,
        val sourceRef: String,
        val licenseRef: String,
    ) {
        init {
            requirePortableRef(
                roleRef,
                "roleRef",
            )
            requirePortableRef(
                artifactRef,
                "artifactRef",
            )
            require(
                filename.isNotBlank() &&
                    !filename.contains("/") &&
                    !filename.contains("\\"),
            ) {
                "Heretic artifact filename must be one safe leaf name: $filename"
            }
            require(
                mediaType.isNotBlank(),
            )
            requireSha256(
                sha256,
                "artifact.sha256",
            )
            require(
                expectedBytes > 0,
            ) {
                "Heretic artifact expectedBytes must be positive"
            }
            requirePortableRef(
                sourceRef,
                "sourceRef",
            )
            requirePortableRef(
                licenseRef,
                "licenseRef",
            )
        }
    }

    val CertifiedArtifactMembers: List<ArtifactMember> =
        listOf(
            ArtifactMember(
                roleRef = "artifact-role.devex.heretic.qwen3.8-27b.mlx4.member.gitattributes",
                artifactRef = "artifact.devex.qwen3.8-27b-heretic-mlx-4bit.808b82a2.member.gitattributes",
                filename = ".gitattributes",
                mediaType = "text/plain",
                sha256 = "34448b82c17d60fec9b65b1f093c115ddbaadc04beb1b0140b6bfed2e012a930",
                expectedBytes = 1_570L,
                sourceRef = ProviderSourceRef,
                licenseRef = LicenseRef,
            ),
            ArtifactMember(
                roleRef = "artifact-role.devex.heretic.qwen3.8-27b.mlx4.member.readme",
                artifactRef = "artifact.devex.qwen3.8-27b-heretic-mlx-4bit.808b82a2.member.readme",
                filename = "README.md",
                mediaType = "text/markdown",
                sha256 = "43d4f27187bedb6668bee8e29578e3279fdef77b76ad176580c2e60cb1953b1a",
                expectedBytes = 3_686L,
                sourceRef = ProviderSourceRef,
                licenseRef = LicenseRef,
            ),
            ArtifactMember(
                roleRef = "artifact-role.devex.heretic.qwen3.8-27b.mlx4.member.chat-template",
                artifactRef = "artifact.devex.qwen3.8-27b-heretic-mlx-4bit.808b82a2.member.chat-template",
                filename = "chat_template.jinja",
                mediaType = "text/plain",
                sha256 = "c3cf9e34abf4f9e36c2d72165aa9c132d3e2a725b6c2586aaa3a8af9d7a81041",
                expectedBytes = 8_952L,
                sourceRef = ProviderSourceRef,
                licenseRef = LicenseRef,
            ),
            ArtifactMember(
                roleRef = "artifact-role.devex.heretic.qwen3.8-27b.mlx4.member.config",
                artifactRef = "artifact.devex.qwen3.8-27b-heretic-mlx-4bit.808b82a2.member.config",
                filename = "config.json",
                mediaType = "application/json",
                sha256 = "21ec916f33652b2433e289cc999a0a75ea42958b2770c0973d31d6006941bb94",
                expectedBytes = 4_192L,
                sourceRef = ProviderSourceRef,
                licenseRef = LicenseRef,
            ),
            ArtifactMember(
                roleRef = "artifact-role.devex.heretic.qwen3.8-27b.mlx4.member.generation-config",
                artifactRef = "artifact.devex.qwen3.8-27b-heretic-mlx-4bit.808b82a2.member.generation-config",
                filename = "generation_config.json",
                mediaType = "application/json",
                sha256 = "d0d0ed2e37cdfafef4a5067d5ea2407b05f4fb50526e47c008a5b235d50240fb",
                expectedBytes = 214L,
                sourceRef = ProviderSourceRef,
                licenseRef = LicenseRef,
            ),
            ArtifactMember(
                roleRef = "artifact-role.devex.heretic.qwen3.8-27b.mlx4.member.model-00001-of-00003-safetensors",
                artifactRef = "artifact.devex.qwen3.8-27b-heretic-mlx-4bit.808b82a2.member.model-00001-of-00003-safetensors",
                filename = "model-00001-of-00003.safetensors",
                mediaType = "application/octet-stream",
                sha256 = "075eac5fbba3951bc4870c1ac65d684c32e0abb24284201bd27f49df56735963",
                expectedBytes = 5_328_325_648L,
                sourceRef = ProviderSourceRef,
                licenseRef = LicenseRef,
            ),
            ArtifactMember(
                roleRef = "artifact-role.devex.heretic.qwen3.8-27b.mlx4.member.model-00002-of-00003-safetensors",
                artifactRef = "artifact.devex.qwen3.8-27b-heretic-mlx-4bit.808b82a2.member.model-00002-of-00003-safetensors",
                filename = "model-00002-of-00003.safetensors",
                mediaType = "application/octet-stream",
                sha256 = "d936fce99536decc4e0584590305acf2af461d867d8c9a79e2dfbdd328b14e5c",
                expectedBytes = 5_354_185_130L,
                sourceRef = ProviderSourceRef,
                licenseRef = LicenseRef,
            ),
            ArtifactMember(
                roleRef = "artifact-role.devex.heretic.qwen3.8-27b.mlx4.member.model-00003-of-00003-safetensors",
                artifactRef = "artifact.devex.qwen3.8-27b-heretic-mlx-4bit.808b82a2.member.model-00003-of-00003-safetensors",
                filename = "model-00003-of-00003.safetensors",
                mediaType = "application/octet-stream",
                sha256 = "139547bce74259fa4181a9c1b6664c1d14ffa3763e998ea52f0be7fdf12955bb",
                expectedBytes = 4_450_532_735L,
                sourceRef = ProviderSourceRef,
                licenseRef = LicenseRef,
            ),
            ArtifactMember(
                roleRef = "artifact-role.devex.heretic.qwen3.8-27b.mlx4.member.model-safetensors-index",
                artifactRef = "artifact.devex.qwen3.8-27b-heretic-mlx-4bit.808b82a2.member.model-safetensors-index",
                filename = "model.safetensors.index.json",
                mediaType = "application/json",
                sha256 = "a2161f6a6c9f7c434145f97a4a4262a74eb3e16a95088e713754eb108b198511",
                expectedBytes = 189_789L,
                sourceRef = ProviderSourceRef,
                licenseRef = LicenseRef,
            ),
            ArtifactMember(
                roleRef = "artifact-role.devex.heretic.qwen3.8-27b.mlx4.member.tokenizer",
                artifactRef = "artifact.devex.qwen3.8-27b-heretic-mlx-4bit.808b82a2.member.tokenizer",
                filename = "tokenizer.json",
                mediaType = "application/json",
                sha256 = "06b9509352d2af50381ab2247e083b80d32d5c0aba91c272ca9ff729b6a0e523",
                expectedBytes = 19_989_325L,
                sourceRef = ProviderSourceRef,
                licenseRef = LicenseRef,
            ),
            ArtifactMember(
                roleRef = "artifact-role.devex.heretic.qwen3.8-27b.mlx4.member.tokenizer-config",
                artifactRef = "artifact.devex.qwen3.8-27b-heretic-mlx-4bit.808b82a2.member.tokenizer-config",
                filename = "tokenizer_config.json",
                mediaType = "application/json",
                sha256 = "5d9c0d3314a253b714ddbbfeef7a55e9295c45c3da9f5fd9907dae6ebe6cc731",
                expectedBytes = 1_265L,
                sourceRef = ProviderSourceRef,
                licenseRef = LicenseRef,
            ),
        )

    init {
        require(
            CertifiedArtifactMembers.size ==
                CertifiedArtifactMemberCount,
        ) {
            "Heretic certified artifact member count drift"
        }
        require(
            CertifiedArtifactMembers
                .sumOf(
                    ArtifactMember::expectedBytes,
                ) ==
                CertifiedArtifactBytes,
        ) {
            "Heretic certified artifact byte total drift"
        }
        require(
            CertifiedArtifactMembers
                .map(
                    ArtifactMember::artifactRef,
                )
                .distinct()
                .size ==
                CertifiedArtifactMembers.size,
        ) {
            "Heretic certified artifact refs must remain unique"
        }
        require(
            CertifiedArtifactMembers
                .map(
                    ArtifactMember::filename,
                )
                .distinct()
                .size ==
                CertifiedArtifactMembers.size,
        ) {
            "Heretic certified artifact filenames must remain unique"
        }
        requireSha256(
            ArtifactContentSetSha256,
            "artifactContentSetSha256",
        )
        requireGitId(
            TrainerSourceCommit,
            "trainerSourceCommit",
        )
    }

    fun pendingConfiguration(
        homeIdentity: HomeIdentity,
    ): Configuration =
        configuration(
            homeIdentity = homeIdentity,
            state = ConfigurationState.EstablishmentPending,
        )

    fun boundConfiguration(
        homeIdentity: HomeIdentity,
    ): Configuration =
        configuration(
            homeIdentity = homeIdentity,
            state = ConfigurationState.BoundProductModel,
        )

    fun validateConfiguration(
        value: Configuration,
        homeIdentity: HomeIdentity,
    ): Configuration {
        require(
            value.schemaVersion ==
                ModelConfigurationSchema,
        ) {
            "Heretic model configuration schema drift"
        }
        require(
            value.homeRef ==
                homeIdentity.homeRef,
        ) {
            "Heretic model configuration Home identity drift"
        }
        require(
            value.companionLineageRef ==
                homeIdentity.companionLineageRef,
        ) {
            "Heretic model configuration Companion lineage drift"
        }
        require(
            value.modelRef ==
                ModelRef &&
                value.modelProfileRef ==
                ModelProfileRef &&
                value.artifactRef ==
                ArtifactSetRef &&
                value.artifactContentSetSha256 ==
                ArtifactContentSetSha256 &&
                value.runtimeAdapterRef ==
                RuntimeAdapterRef &&
                value.runtimeClass ==
                RuntimeClass &&
                value.endpoint ==
                Endpoint &&
                value.requestModel ==
                RequestModel &&
                value.runtimeReceiptPath ==
                RuntimeReceiptRelativePath,
        ) {
            "Heretic model configuration product identity drift"
        }
        require(
            !value.automaticFallback &&
                !value.automaticDownload &&
                !value.automaticActivation,
        ) {
            "Heretic model configuration cannot grant fallback/download/activation authority"
        }

        return value
    }

    fun isBound(
        value: Configuration,
    ): Boolean =
        value.state ==
            ConfigurationState.BoundProductModel

    private fun configuration(
        homeIdentity: HomeIdentity,
        state: ConfigurationState,
    ): Configuration =
        validateConfiguration(
            value =
                Configuration(
                    schemaVersion = ModelConfigurationSchema,
                    state = state,
                    homeRef = homeIdentity.homeRef,
                    companionLineageRef = homeIdentity.companionLineageRef,
                    modelRef = ModelRef,
                    modelProfileRef = ModelProfileRef,
                    artifactRef = ArtifactSetRef,
                    artifactContentSetSha256 = ArtifactContentSetSha256,
                    runtimeAdapterRef = RuntimeAdapterRef,
                    runtimeClass = RuntimeClass,
                    endpoint = Endpoint,
                    requestModel = RequestModel,
                    runtimeReceiptPath = RuntimeReceiptRelativePath,
                    automaticFallback = false,
                    automaticDownload = false,
                    automaticActivation = false,
                ),
            homeIdentity = homeIdentity,
        )

    private fun requirePortableRef(
        value: String,
        label: String,
    ) {
        require(
            value.matches(
                Regex(
                    "[A-Za-z0-9][A-Za-z0-9._:/-]*",
                ),
            ),
        ) {
            "$label must be one portable ref"
        }
    }

    private fun requireSha256(
        value: String,
        label: String,
    ) {
        require(
            value.matches(
                Regex(
                    "[0-9a-f]{64}",
                ),
            ),
        ) {
            "$label must be lowercase SHA-256"
        }
    }

    private fun requireGitId(
        value: String,
        label: String,
    ) {
        require(
            value.matches(
                Regex(
                    "[0-9a-f]{40,64}",
                ),
            ),
        ) {
            "$label must be one Git identity"
        }
    }
}

// [VXG RealForever]
