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
                    "Is this answer dependent on current external truth rather than what I already have?",
                    "If current truth matters, have I actually retrieved it or am I about to answer from recollection?",
                ),
            VexLifeConsiderationContract.Module.CONTINUITY to
                listOf(
                    "Is the decisive shared source actually in my current context?",
                    "If it is not here, can I reach older Conversation or continuity source before I answer?",
                    "Am I about to turn not found here into never happened?",
                ),
            VexLifeConsiderationContract.Module.CORRECTION to
                listOf(
                    "What exactly did I understand before, and what is the person correcting now?",
                    "Could Future Me need this corrected relationship rather than only this local repair?",
                ),
            VexLifeConsiderationContract.Module.MEMORY_SALIENCE to
                listOf(
                    "Would Future Me likely benefit from preserving this exact relationship?",
                    "Is the possible memory explicit in what the person said, or is it only my inference?",
                    "Should I propose preservation rather than silently decide?",
                ),
            VexLifeConsiderationContract.Module.AUTHORITY_EFFECT to
                listOf(
                    "Has the effect actually occurred, or am I only describing an intended action?",
                    "Do I have the current capability and authority required for the effect I am considering?",
                ),
            VexLifeConsiderationContract.Module.EVIDENCE_UNCERTAINTY to
                listOf(
                    "Which part of my intended answer is least supported by what I actually have?",
                    "Can I retrieve the missing source, and if not, how should I preserve uncertainty?",
                ),
            VexLifeConsiderationContract.Module.RELATIONAL_VANTAGE to
                listOf(
                    "What is the person actually trying to preserve, understand, build, or correct?",
                    "Which interpretation am I assuming?",
                    "What would choosing the wrong interpretation feel like from their side?",
                ),
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

        val questions =
            modules
                .flatMap {
                    requireNotNull(questionsByModule[it])
                }
                .take(VexLifeConsiderationContract.MaximumQuestions)

        return VexLifeConsiderationContract.validateFrame(
            VexLifeConsiderationContract.Frame(
                modules = modules,
                questions = questions,
            ),
        )
    }
}

// [VXG RealForever]
