package vexlife.model.heretic

/**
 * Exact accepted product identity for the certified local Heretic relationship.
 *
 * This owner answers only "which model/product relationship is this?" Runtime realization,
 * acquisition and process lifetime remain separate owners.
 */
object HereticProductIdentity {
    const val ModelBundleSchema =
        "vexlife.model-bundle/v2"

    const val ModelBundleRef =
        "model-bundle.vexlife.heretic.qwen3.8-27b.mlx4.329658ee8f27.001"

    const val GenerationRef =
        "generation.devex.heretic27b4.preexisting-certified-foundation.normalized.20261001A"

    const val ModelRef =
        "model.devex.heretic.qwen3.8-27b.mlx4.329658ee8f27"

    /** Human-facing name for setup copy; [ModelRef] remains the identity. */
    const val HumanModelLabel =
        "Heretic 27B (4-bit, MLX)"

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
     * Existing lived VexLife browser-Companion endpoint-profile identity.
     *
     * Canonical Conversation RESPONSE events persist this profile ref separately from the Heretic
     * runtime-adapter identity. New Beginnings ports that accepted distinction rather than minting
     * a product-specific endpoint-profile namespace.
     */
    const val CompanionEndpointProfileRef =
        "model-profile.vexlife.browser-companion.local"

    /**
     * Fresh product-owned host execution profile.
     *
     * Its eligibility meaning is faithfully ported from current VexLife Heretic setup rather than
     * retaining the legacy repository-scoped profile identity as a runtime dependency.
     */
    const val HostExecutionProfileRef =
        "profile.repository.vexlife-prod.macos-v1"

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

    const val StableRuntimeReceiptRelativePath =
        "runtime/heretic/persistent-runtime-owner-receipt.json"

    const val ProductRuntimeReceiptRelativePath =
        "runtime/vexlife-prod/heretic-persistent-runtime-owner-receipt.json"

    /**
     * Existing product-model bindings formed during continuity realignment borrowed the stable
     * VexLife receipt coordinate. Keep the compatibility alias explicit rather than silently moving
     * those durable product-owned bindings.
     */
    const val RuntimeReceiptRelativePath =
        StableRuntimeReceiptRelativePath

    const val ReleaseState =
        "RELEASE_QUALIFIED"

    data class Value(
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

    val Current =
        Value(
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

    init {
        requireSha256(
            ArtifactContentSetSha256,
            "artifactContentSetSha256",
        )
        requireGitId(
            TrainerSourceCommit,
            "trainerSourceCommit",
        )
    }
}

// [VXG RealForever]
