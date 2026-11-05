package vexlife.model.heretic

/**
 * Protected effect boundary carried by a qualified Heretic persistent-runtime receipt.
 *
 * The runtime owner may create/reuse one local model runtime. It does not thereby gain authority
 * over model acquisition, training, Memory, activation/succession, publication, repository source,
 * or Vex Home semantics.
 */
object HereticRuntimeEffectBoundary {
    data class Value(
        val modelRuntime: Boolean,
        val providerNetwork: Boolean,
        val modelDownload: Boolean,
        val artifactMutation: Boolean,
        val training: Boolean,
        val weightMutation: Boolean,
        val memoryMutation: Boolean,
        val activation: Boolean,
        val succession: Boolean,
        val publication: Boolean,
        val repositoryMutation: Boolean,
        val homeMutation: Boolean,
    )

    val QualifiedRuntime =
        Value(
            modelRuntime = true,
            providerNetwork = false,
            modelDownload = false,
            artifactMutation = false,
            training = false,
            weightMutation = false,
            memoryMutation = false,
            activation = false,
            succession = false,
            publication = false,
            repositoryMutation = false,
            homeMutation = false,
        )
}

// [VXG RealForever]
