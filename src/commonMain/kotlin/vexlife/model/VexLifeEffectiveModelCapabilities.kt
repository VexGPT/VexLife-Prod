package vexlife.model

import kotlinx.serialization.Serializable
import vexlife.model.heretic.HereticCompanionRepresentation
import vexlife.model.heretic.HereticModelConfiguration
import vexlife.model.heretic.HereticRuntimeRealization

/**
 * Read-only effective capability projection for one offered local model on the current Product
 * target.
 *
 * Capability meaning and effects remain with their existing semantic owners. This projection only
 * answers whether one already-owned relationship is available *now* after exact provider
 * representation, current-target runtime realization and qualified serving evidence are joined.
 *
 * Permanent:
 *
 * ```text
 * MODEL NAME != CAPABILITY
 * PROVIDER REPRESENTATION != RUNTIME QUALIFICATION
 * RUNTIME REALIZABLE != RUNTIME SERVING
 * VISUAL MODEL CANDIDATE != QUALIFIED VISUAL INPUT
 * CAPABILITY PROJECTION != CAPABILITY AUTHORITY
 * ```
 */
object VexLifeEffectiveModelCapabilities {
    @Serializable
    enum class RuntimeStanding {
        QUALIFIED_SERVING,
        REALIZABLE_NOT_QUALIFIED,
        UNSUPPORTED_CURRENT_TARGET,
    }

    @Serializable
    enum class Standing {
        AVAILABLE,
        HELD,
        NOT_APPLICABLE,
    }

    @Serializable
    data class Projection(
        val modelRef: String,
        val profileRef: String,
        val runtimeRealizationRef: String?,
        val qualifiedRuntimeReceiptRef: String?,
        val runtime: RuntimeStanding,
        val vexReach: Standing,
        val providerNativeTools: Standing,
        val reasoningEffortControl: Standing,
        val visualInput: Standing,
    )

    fun project(
        modelRef: String,
        profile: HereticCompanionRepresentation.Profile,
        realization: HereticRuntimeRealization.Value?,
        qualifiedRuntime: HereticModelConfiguration.QualifiedRuntimeEvidence?,
        qualifiedVisualInput: VexLifeVisualInputQualification.QualifiedRuntimeEvidence? = null,
    ): Projection {
        val exactQualifiedRuntime =
            realization?.takeIf {
                qualifiedRuntime != null &&
                    qualifiedRuntime.modelRef == modelRef &&
                    qualifiedRuntime.realizationRef == it.realizationRef &&
                    qualifiedRuntime.modelProfileRef == it.modelProfileRef &&
                    qualifiedRuntime.artifactRef == it.artifact.artifactRef &&
                    qualifiedRuntime.artifactContentSetSha256 == it.artifact.contentSetSha256 &&
                    qualifiedRuntime.runtimeAdapterRef == it.runtime.runtimeAdapterRef &&
                    qualifiedRuntime.runtimeClass == it.runtime.runtimeClass &&
                    qualifiedRuntime.endpoint == it.runtime.endpoint &&
                    qualifiedRuntime.requestModel == it.runtime.requestModel
            }

        val exactQualifiedVisualInput =
            qualifiedVisualInput
                ?.takeIf {
                    exactQualifiedRuntime != null &&
                        VexLifeVisualInputQualification.matches(
                            evidence = it,
                            modelRef = modelRef,
                            artifactRef = exactQualifiedRuntime.artifactRef,
                            artifactContentSetSha256 =
                                exactQualifiedRuntime.artifactContentSetSha256,
                        )
                }

        val runtimeStanding =
            when {
                exactQualifiedRuntime != null -> RuntimeStanding.QUALIFIED_SERVING
                realization != null -> RuntimeStanding.REALIZABLE_NOT_QUALIFIED
                else -> RuntimeStanding.UNSUPPORTED_CURRENT_TARGET
            }

        fun represented(
            represented: Boolean,
        ): Standing =
            when {
                !represented -> Standing.NOT_APPLICABLE
                exactQualifiedRuntime != null -> Standing.AVAILABLE
                else -> Standing.HELD
            }

        return Projection(
            modelRef = modelRef,
            profileRef = profile.profileRef,
            runtimeRealizationRef = realization?.realizationRef,
            qualifiedRuntimeReceiptRef =
                qualifiedRuntime
                    ?.runtimeReceiptRef
                    ?.takeIf {
                        exactQualifiedRuntime != null
                    },
            runtime = runtimeStanding,
            vexReach =
                when (profile.reachRepresentationStanding) {
                    HereticCompanionRepresentation.ReachRepresentationStanding.QUALIFIED ->
                        represented(true)

                    HereticCompanionRepresentation.ReachRepresentationStanding.HELD_PROVIDER_BRIDGE ->
                        Standing.HELD
                },
            providerNativeTools =
                represented(
                    profile.capabilityRepresentation ==
                        HereticCompanionRepresentation.CapabilityRepresentation.PROVIDER_NATIVE_TOOLS,
                ),
            reasoningEffortControl =
                represented(
                    profile.generation.reasoningEffort != null,
                ),
            visualInput =
                if (exactQualifiedVisualInput != null) {
                    Standing.AVAILABLE
                } else {
                    Standing.HELD
                },
        )
    }
}

// [VXG RealForever]
