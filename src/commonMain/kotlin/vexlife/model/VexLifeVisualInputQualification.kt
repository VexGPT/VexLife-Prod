package vexlife.model

import kotlinx.serialization.Serializable
import vexlife.model.heretic.HereticModelCatalog

/**
 * Exact current-runtime evidence shape for model-visible static-image interpretation.
 *
 * A005 is qualification provenance, not currentness. A runtime owner must still form one fresh
 * [QualifiedRuntimeEvidence] after proving the exact model artifact and visual environment are
 * current on the host.
 *
 * Permanent:
 *
 * ```text
 * LIVED QUALIFICATION != CURRENT RUNTIME
 * STATIC IMAGE PROJECTION != VISUAL INTERPRETATION
 * VISUAL INTERPRETATION != CONVERSATION AUTHORITY
 * VISUAL MODEL != SELECTED COMPANION
 * ```
 */
object VexLifeVisualInputQualification {
    const val Schema =
        "vexlife-prod.visual-input-qualified-runtime/v1"

    const val RealizationRef =
        "realization.vexlife-prod.visual.heretic-4b.macos-arm64.mlx-vlm-0.8.0.001"

    const val RuntimeFamilyRef =
        "runtime-family.vexlife-prod.mlx-vlm-0.8.0.macos-arm64"

    const val QualificationSourceRef =
        "qualification.vexlife-prod.heretic-4b.visual-pixel-discrimination.a005"

    const val QualificationReturnSha256 =
        "810fbb055ee4a7a85959f75c6fbfe276409ae3af0809d08a268f6688870f4b73"

    const val PythonVersion = "3.14.7"
    const val MlxVlmVersion = "0.8.0"
    const val MlxVersion = "0.32.4"
    const val TransformersVersion = "5.23.0"

    private val PortableRef =
        Regex(
            "[A-Za-z0-9][A-Za-z0-9._:/-]*",
        )

    private val Sha256 =
        Regex(
            "[0-9a-f]{64}",
        )

    @Serializable
    data class QualifiedRuntimeEvidence(
        val schemaVersion: String = Schema,
        /** Fresh evidence identity formed by the current runtime owner. */
        val evidenceRef: String,
        val realizationRef: String = RealizationRef,
        val modelRef: String,
        val artifactRef: String,
        val artifactContentSetSha256: String,
        val runtimeFamilyRef: String = RuntimeFamilyRef,
        val pythonVersion: String = PythonVersion,
        val mlxVlmVersion: String = MlxVlmVersion,
        val mlxVersion: String = MlxVersion,
        val transformersVersion: String = TransformersVersion,
        /** Digest of the runtime owner's exact environment/materialization evidence. */
        val environmentFingerprintSha256: String,
        val qualificationSourceRef: String = QualificationSourceRef,
        val qualificationReturnSha256: String = QualificationReturnSha256,
    )

    fun validate(
        evidence: QualifiedRuntimeEvidence,
    ): QualifiedRuntimeEvidence {
        val model = HereticModelCatalog.Models.Qwen4B
        require(
            evidence.schemaVersion == Schema &&
                PortableRef.matches(evidence.evidenceRef) &&
                evidence.realizationRef == RealizationRef &&
                evidence.modelRef == model.modelRef &&
                evidence.artifactRef == model.identity.artifactSetRef &&
                evidence.artifactContentSetSha256 ==
                    model.identity.artifactContentSetSha256 &&
                evidence.runtimeFamilyRef == RuntimeFamilyRef &&
                evidence.pythonVersion == PythonVersion &&
                evidence.mlxVlmVersion == MlxVlmVersion &&
                evidence.mlxVersion == MlxVersion &&
                evidence.transformersVersion == TransformersVersion &&
                Sha256.matches(evidence.environmentFingerprintSha256) &&
                evidence.qualificationSourceRef == QualificationSourceRef &&
                evidence.qualificationReturnSha256 == QualificationReturnSha256
        ) {
            "visual-input qualified runtime evidence does not match the lived A005 realization"
        }
        return evidence
    }

    fun matches(
        evidence: QualifiedRuntimeEvidence?,
        modelRef: String,
        artifactRef: String,
        artifactContentSetSha256: String,
    ): Boolean =
        evidence
            ?.let {
                runCatching {
                    validate(it)
                }
                    .getOrNull()
            }
            ?.let {
                it.modelRef == modelRef &&
                    it.artifactRef == artifactRef &&
                    it.artifactContentSetSha256 == artifactContentSetSha256
            } ==
            true
}

// [VXG RealForever]
