package vexlife.consideration

import kotlinx.serialization.Serializable

/**
 * Portable contracts for one bounded Vex self-consideration pass and its post-turn salience result.
 *
 * These contracts intentionally carry questions and bounded self-attribution, not hidden reasoning.
 * They grant no fact, effect, Memory-write or semantic-acceptance authority.
 */
object VexLifeConsiderationContract {
    const val FrameSchema = "vexlife-prod.consideration-frame/v1"
    const val SalienceSchema = "vexlife-prod.turn-salience/v1"

    const val MaximumModules = 7
    const val MaximumQuestions = 14
    const val MaximumQuestionCharacters = 240
    const val MaximumCandidateSummaryCharacters = 320
    const val MaximumScopeHintCharacters = 160

    @Serializable
    enum class Module {
        CURRENT_TRUTH,
        CONTINUITY,
        CORRECTION,
        MEMORY_SALIENCE,
        AUTHORITY_EFFECT,
        EVIDENCE_UNCERTAINTY,
        RELATIONAL_VANTAGE,
    }

    @Serializable
    enum class Level {
        LOW,
        MEDIUM,
        HIGH,
    }

    @Serializable
    enum class RelationshipShift {
        NONE,
        PREFERENCE,
        CORRECTION,
        BOUNDARY,
        COMMITMENT,
        OTHER,
    }

    @Serializable
    enum class ReturnNeed {
        NO,
        MAYBE,
        YES,
    }

    @Serializable
    enum class MemoryCandidateDisposition {
        NONE,
        CANDIDATE,
    }

    @Serializable
    enum class MemoryCandidateClass {
        PREFERENCE,
        CORRECTION,
        RELATIONSHIP,
        COMMITMENT,
        ONGOING_WORK,
        OTHER,
    }

    @Serializable
    data class Frame(
        val schemaVersion: String = FrameSchema,
        val modules: List<Module>,
        val questions: List<String>,
        val preservesLiteralHumanRequest: Boolean = true,
        val factAuthorityGranted: Boolean = false,
        val semanticAcceptanceGranted: Boolean = false,
        val effectAuthorityGranted: Boolean = false,
    )

    @Serializable
    data class SelfAttributedVantage(
        val feltImportance: Level,
        val feltUncertainty: Level,
        val feltRelationshipShift: RelationshipShift,
        val feltNeedToReturn: ReturnNeed,
    )

    @Serializable
    data class ContinuitySignals(
        val olderSharedContextWasNeeded: Boolean,
        val currentExternalTruthWasNeeded: Boolean,
        val humanCorrectionObserved: Boolean,
        val explicitHumanMemoryIntent: Boolean,
        val futureUsefulness: Level,
        val unresolvedOpenLoop: Boolean,
    )

    @Serializable
    data class MemoryCandidate(
        val disposition: MemoryCandidateDisposition,
        val candidateClass: MemoryCandidateClass? = null,
        val summary: String? = null,
        val scopeHint: String? = null,
    )

    /**
     * Model-authored bounded reflection. VexLife may bind exact source refs around this value later.
     *
     * rawReasoningPersisted=false is part of the contract: this is never a chain-of-thought archive.
     */
    @Serializable
    data class Salience(
        val schemaVersion: String = SalienceSchema,
        val selfAttributedVantage: SelfAttributedVantage,
        val continuitySignals: ContinuitySignals,
        val memoryCandidate: MemoryCandidate,
        val rawReasoningPersisted: Boolean = false,
        val factAuthorityGranted: Boolean = false,
        val semanticAcceptanceGranted: Boolean = false,
        val memoryWriteAuthorityGranted: Boolean = false,
    )

    fun validateFrame(value: Frame): Frame {
        require(value.schemaVersion == FrameSchema) {
            "consideration frame schema mismatch"
        }
        require(value.modules.isNotEmpty() && value.modules.size <= MaximumModules) {
            "consideration frame requires one bounded module set"
        }
        require(value.modules.distinct().size == value.modules.size) {
            "consideration frame modules must be unique"
        }
        require(value.questions.isNotEmpty() && value.questions.size <= MaximumQuestions) {
            "consideration frame requires one bounded question set"
        }
        require(
            value.questions.all {
                it.isNotBlank() &&
                    it.length <= MaximumQuestionCharacters
            },
        ) {
            "consideration frame contains an invalid question"
        }
        require(
            value.preservesLiteralHumanRequest &&
                !value.factAuthorityGranted &&
                !value.semanticAcceptanceGranted &&
                !value.effectAuthorityGranted,
        ) {
            "consideration frame may orient Vex but grants no truth or effect authority"
        }
        return value
    }

    fun validateSalience(value: Salience): Salience {
        require(value.schemaVersion == SalienceSchema) {
            "turn salience schema mismatch"
        }
        require(
            !value.rawReasoningPersisted &&
                !value.factAuthorityGranted &&
                !value.semanticAcceptanceGranted &&
                !value.memoryWriteAuthorityGranted,
        ) {
            "turn salience may report bounded self-attribution but grants no durable authority"
        }

        when (value.memoryCandidate.disposition) {
            MemoryCandidateDisposition.NONE -> {
                require(
                    value.memoryCandidate.candidateClass == null &&
                        value.memoryCandidate.summary == null &&
                        value.memoryCandidate.scopeHint == null,
                ) {
                    "no-memory-candidate salience cannot carry candidate content"
                }
            }

            MemoryCandidateDisposition.CANDIDATE -> {
                val candidate = value.memoryCandidate
                require(candidate.candidateClass != null) {
                    "memory candidate requires one candidate class"
                }
                val summary =
                    requireNotNull(
                        candidate.summary,
                    ) {
                        "memory candidate requires one bounded summary"
                    }
                require(
                    summary.isNotBlank() &&
                        summary.length <= MaximumCandidateSummaryCharacters,
                ) {
                    "memory candidate requires one bounded summary"
                }
                val scopeHint = candidate.scopeHint
                require(
                    scopeHint == null ||
                        (
                            scopeHint.isNotBlank() &&
                                scopeHint.length <= MaximumScopeHintCharacters
                        ),
                ) {
                    "memory candidate scope hint is invalid"
                }
            }
        }

        return value
    }
}

// [VXG RealForever]
