package vexlife.platform.windows

import java.nio.file.Path
import kotlinx.serialization.Serializable
import vexlife.continuity.VexLifeContinuity
import vexlife.contract.VexContractJson
import vexlife.contract.VexDurableContractFile
import vexlife.filesystem.VexOwnedPath

/**
 * Read-only compatibility observation for the release-qualified Windows runtime relationship that
 * predates VexLife-Prod's Product-scoped model binding.
 *
 * This owner exists because the canonical VexLife Home may already carry
 * `config/model.json = vexlife.model-configuration/v1 / BOUND_QUALIFIED`. Treating the absence of
 * `runtime/vexlife-prod/model-binding.json` as simply "unconfigured" would erase earned Windows
 * runtime continuity.
 *
 * The exact relationship below comes from [VexLifeContinuity.ReferenceRepository] at
 * [VexLifeContinuity.ReferenceRevision]:
 *
 * - model bundle: Qwen3.5 4B Q4_K_M;
 * - runtime: llama.cpp b10107 Windows x64 CUDA 12.4;
 * - endpoint: 127.0.0.1:18080;
 * - source state: RELEASE_QUALIFIED.
 *
 * Observation does not prove the process is alive, hash multi-GiB model artifacts, start/stop a
 * runtime, write a Product binding, or grant migration authority.
 */
object VexLifeWindowsPredecessorRuntimeBinding {
    const val SchemaV1 =
        "vexlife.model-configuration/v1"

    const val QualifiedState =
        "BOUND_QUALIFIED"

    const val WindowsProfileRef =
        "profile.vexlife.operational.qwen3.5-4b.llama-cpp-b10107.windows-x64-nvidia.001"

    const val ModelBundleRef =
        "model-bundle.vexlife.g0.qwen3.5-4b.q4-k-m.001"

    const val GenerationRef =
        "generation.vexlife.g0.001"

    const val ModelProfileRef =
        "model-profile.vexlife.g0.qwen3.5-4b.q4-k-m.001"

    const val Endpoint =
        "http://127.0.0.1:18080"

    const val RequestModel =
        "Qwen3.5-4B-Q4_K_M"

    const val ActiveArtifactRef =
        "model.qwen3.5-4b.q4-k-m.ba06320255db2dbec194dad738d066be90dabf29"

    const val RuntimeDependencyRef =
        "runtime.llama-cpp.server.b10107.windows-x86-64.cuda12.4"

    const val RuntimeExecutableSha256 =
        "af3e56d6bdb84a9b6bd50f6ad748809370bc29be42d58088041408b2933d80f7"

    const val SourceStanding =
        "RELEASE_QUALIFIED"

    enum class Currentness {
        EXACT_PREDECESSOR_CURRENT_SOURCE_BINDING,
        LEGACY_QUALIFIED_NEEDS_CURRENTNESS_RECONCILIATION,
    }

    @Serializable
    data class Value(
        val schemaVersion: String,
        val state: String,
        val profileRef: String,
        val activeModelBundleRef: String? = null,
        val generationRef: String? = null,
        val modelProfileRef: String? = null,
        val endpoint: String,
        val requestModel: String,
        val activeArtifactRef: String,
        val runtimeDependencyRef: String,
        val runtimePid: Long,
        val runtimeExecutablePath: String,
        val runtimeExecutableSha256: String,
        val runtimeExecutableSha256SourcePinned: String,
        val runtimeArguments: List<String>,
        val runtimeMaterializationRoot: String,
        val modelPath: String,
        val projectorPath: String,
        val qualificationReceiptRef: String,
        val automaticDownload: Boolean,
        val automaticActivation: Boolean,
    )

    sealed interface Observation {
        val effectsPerformed: Boolean

        data object Absent : Observation {
            override val effectsPerformed: Boolean = false
        }

        data class OtherSchema(
            val schemaVersion: String,
        ) : Observation {
            override val effectsPerformed: Boolean = false
        }

        data class Recognized(
            val value: Value,
            val currentness: Currentness,
            val sourceRepository: String = VexLifeContinuity.ReferenceRepository,
            val sourceRevision: String = VexLifeContinuity.ReferenceRevision,
            val sourceStanding: String = SourceStanding,
        ) : Observation {
            override val effectsPerformed: Boolean = false
        }

        data class HeldMismatch(
            val reason: String,
        ) : Observation {
            override val effectsPerformed: Boolean = false
        }
    }

    fun observe(
        productHome: Path,
    ): Observation {
        require(productHome.isAbsolute) {
            "Windows predecessor runtime binding requires one absolute Vex Home"
        }
        val home =
            productHome
                .normalize()
        val path =
            VexOwnedPath.resolveInside(
                home,
                "config/model.json",
            )
        val text =
            VexDurableContractFile.readText(
                ownedRoot = home,
                path = path,
            )
                ?: return Observation.Absent

        val schema =
            VexContractJson.schemaVersion(
                text,
            )
        if (
            schema !=
            SchemaV1
        ) {
            return Observation.OtherSchema(
                schemaVersion = schema,
            )
        }

        val value =
            runCatching {
                    VexContractJson.decode<Value>(
                        text,
                    )
                }
                .getOrElse {
                    return Observation.HeldMismatch(
                        reason = "V1_BINDING_DOES_NOT_MATCH_KNOWN_CONTRACT_SHAPE",
                    )
                }

        return evaluate(
            value,
        )
    }

    internal fun evaluate(
        value: Value,
    ): Observation {
        val fixedIdentityMatches =
            value.schemaVersion == SchemaV1 &&
                value.state == QualifiedState &&
                value.profileRef == WindowsProfileRef &&
                value.endpoint == Endpoint &&
                value.requestModel == RequestModel &&
                value.activeArtifactRef == ActiveArtifactRef &&
                value.runtimeDependencyRef == RuntimeDependencyRef &&
                value.runtimeExecutableSha256SourcePinned == RuntimeExecutableSha256 &&
                value.automaticDownload == false &&
                value.automaticActivation == false

        if (
            !fixedIdentityMatches
        ) {
            return Observation.HeldMismatch(
                reason = "V1_BINDING_DOES_NOT_MATCH_RELEASE_QUALIFIED_WINDOWS_RELATIONSHIP",
            )
        }

        val currentnessFields =
            listOf(
                value.activeModelBundleRef,
                value.generationRef,
                value.modelProfileRef,
            )

        val currentness =
            when {
                currentnessFields.all {
                    it == null
                } ->
                    Currentness.LEGACY_QUALIFIED_NEEDS_CURRENTNESS_RECONCILIATION

                value.activeModelBundleRef == ModelBundleRef &&
                    value.generationRef == GenerationRef &&
                    value.modelProfileRef == ModelProfileRef ->
                    Currentness.EXACT_PREDECESSOR_CURRENT_SOURCE_BINDING

                else ->
                    return Observation.HeldMismatch(
                        reason = "V1_BINDING_CURRENTNESS_FIELDS_ARE_PARTIAL_OR_MISMATCHED",
                    )
            }

        return Observation.Recognized(
            value = value,
            currentness = currentness,
        )
    }
}

// [VXG RealForever]
