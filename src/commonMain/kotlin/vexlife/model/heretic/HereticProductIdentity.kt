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
     * Historical VexLife source also recognizes an older SDK profile for legacy receipt
     * compatibility. The fresh product does not collapse that provenance into current identity.
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
