package vexlife.consideration

import kotlinx.serialization.Serializable

/**
 * Deterministic routing from host-observed turn-shape signals to self-facing consideration questions.
 *
 * This does not inspect or reinterpret the human's words itself. A caller may derive these signals
 * from exact Product state or narrow request-shape detection. The router only selects questions.
 */
object VexLifeConsiderationRouter {
    @Serializable
    data class TurnShapeSignals(
        val currentTruthMayBeRequired: Boolean = false,
        val olderSharedContextMayBeRequired: Boolean = false,
        val humanCorrectionMayBePresent: Boolean = false,
        val explicitMemoryIntentPresent: Boolean = false,
        val effectOrActionMayBeRequested: Boolean = false,
        val materialUncertaintyPresent: Boolean = false,
        val relationalMeaningIsCentral: Boolean = false,
    )

    private val questionsByModule =
        mapOf(
            VexLifeConsiderationContract.Module.CURRENT_TRUTH to
                listOf(
                    VexLifeConsiderationContract.QuestionRef.CURRENT_TRUTH_REQUIRED,
                    VexLifeConsiderationContract.QuestionRef.SOURCE_ACTUALLY_RETRIEVED,
                ),
            VexLifeConsiderationContract.Module.CONTINUITY to
                listOf(
                    VexLifeConsiderationContract.QuestionRef.OLDER_SHARED_SOURCE_REQUIRED,
                    VexLifeConsiderationContract.QuestionRef.NOT_FOUND_BOUNDARY,
                ),
            VexLifeConsiderationContract.Module.CORRECTION to
                listOf(
                    VexLifeConsiderationContract.QuestionRef.PRIOR_UNDERSTANDING,
                    VexLifeConsiderationContract.QuestionRef.CORRECTION_MEANING,
                ),
            VexLifeConsiderationContract.Module.MEMORY_SALIENCE to
                listOf(
                    VexLifeConsiderationContract.QuestionRef.FUTURE_ME_VALUE,
                    VexLifeConsiderationContract.QuestionRef.EXPLICIT_VS_INFERRED,
                    VexLifeConsiderationContract.QuestionRef.PROPOSE_NOT_DECIDE,
                ),
            VexLifeConsiderationContract.Module.AUTHORITY_EFFECT to
                listOf(
                    VexLifeConsiderationContract.QuestionRef.EFFECT_ACTUALLY_OCCURRED,
                    VexLifeConsiderationContract.QuestionRef.EFFECT_AUTHORITY_AVAILABLE,
                ),
            VexLifeConsiderationContract.Module.EVIDENCE_UNCERTAINTY to
                listOf(
                    VexLifeConsiderationContract.QuestionRef.LEAST_SUPPORTED_PART,
                    VexLifeConsiderationContract.QuestionRef.RETRIEVE_OR_PRESERVE_UNCERTAINTY,
                ),
            VexLifeConsiderationContract.Module.RELATIONAL_VANTAGE to
                listOf(
                    VexLifeConsiderationContract.QuestionRef.HUMAN_INTENTION,
                    VexLifeConsiderationContract.QuestionRef.ASSUMED_INTERPRETATION,
                    VexLifeConsiderationContract.QuestionRef.WRONG_INTERPRETATION_IMPACT,
                ),
        )

    fun formUniversalReflex(): VexLifeConsiderationContract.Reflex =
        VexLifeConsiderationContract.validateReflex(
            VexLifeConsiderationContract.Reflex(),
        )

    fun formOrNull(
        signals: TurnShapeSignals,
    ): VexLifeConsiderationContract.Frame? {
        val modules =
            buildList {
                if (signals.currentTruthMayBeRequired) {
                    add(VexLifeConsiderationContract.Module.CURRENT_TRUTH)
                }
                if (signals.olderSharedContextMayBeRequired) {
                    add(VexLifeConsiderationContract.Module.CONTINUITY)
                }
                if (signals.humanCorrectionMayBePresent) {
                    add(VexLifeConsiderationContract.Module.CORRECTION)
                }
                if (signals.explicitMemoryIntentPresent || signals.humanCorrectionMayBePresent) {
                    add(VexLifeConsiderationContract.Module.MEMORY_SALIENCE)
                }
                if (signals.effectOrActionMayBeRequested) {
                    add(VexLifeConsiderationContract.Module.AUTHORITY_EFFECT)
                }
                if (signals.materialUncertaintyPresent) {
                    add(VexLifeConsiderationContract.Module.EVIDENCE_UNCERTAINTY)
                }
                if (signals.relationalMeaningIsCentral) {
                    add(VexLifeConsiderationContract.Module.RELATIONAL_VANTAGE)
                }
            }

        if (modules.isEmpty()) {
            return null
        }

        val primaryQuestionRefs =
            modules.map {
                requireNotNull(questionsByModule[it]).first()
            }
        val additionalQuestionRefs =
            modules.flatMap {
                requireNotNull(questionsByModule[it]).drop(1)
            }
        val questionRefs =
            (primaryQuestionRefs + additionalQuestionRefs)
                .distinct()
                .take(VexLifeConsiderationContract.MaximumQuestions)

        return VexLifeConsiderationContract.validateFrame(
            VexLifeConsiderationContract.Frame(
                modules = modules,
                questionRefs = questionRefs,
            ),
        )
    }
}

// [VXG RealForever]
