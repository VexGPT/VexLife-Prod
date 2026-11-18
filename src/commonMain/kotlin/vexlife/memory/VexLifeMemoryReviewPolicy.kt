package vexlife.memory

import kotlinx.serialization.Serializable
import vexlife.consideration.VexLifeConsiderationContract

/**
 * Pure review-routing policy for one bounded Memory candidate.
 *
 * This object never accepts Memory. It decides only whether one candidate should be ignored, batched,
 * shown now, or is eligible for a separately-owned standing-consent path.
 */
object VexLifeMemoryReviewPolicy {
    @Serializable
    enum class Disposition {
        NO_ACTION,
        BATCH_REVIEW,
        PROMPT_NOW,
        AUTO_KEEP_ELIGIBLE,
    }

    @Serializable
    enum class StandingConsentClass {
        EXPLICIT_PREFERENCE,
        HUMAN_CORRECTION,
        EXPLICIT_COMMITMENT,
    }

    @Serializable
    data class StandingConsent(
        val enabledClasses: List<StandingConsentClass> = emptyList(),
    ) {
        init {
            require(enabledClasses.distinct().size == enabledClasses.size) {
                "standing-consent classes must be unique"
            }
        }
    }

    @Serializable
    data class Signals(
        val explicitPreference: Boolean = false,
        val explicitCommitment: Boolean = false,
        val inferredRelationshipMeaning: Boolean = false,
    )

    @Serializable
    data class Result(
        val disposition: Disposition,
        val standingConsentClass: StandingConsentClass? = null,
        val semanticAcceptanceGranted: Boolean = false,
        val memoryWriteAuthorityGranted: Boolean = false,
    )

    fun route(
        salience: VexLifeConsiderationContract.Salience,
        signals: Signals = Signals(),
        standingConsent: StandingConsent = StandingConsent(),
    ): Result {
        val current =
            VexLifeConsiderationContract.validateSalience(
                salience,
            )
        val candidate =
            current.memoryCandidate

        if (
            candidate.disposition ==
            VexLifeConsiderationContract.MemoryCandidateDisposition.NONE
        ) {
            return noAction()
        }

        val candidateClass =
            requireNotNull(
                candidate.candidateClass,
            )

        if (
            signals.inferredRelationshipMeaning ||
            candidateClass ==
                VexLifeConsiderationContract.MemoryCandidateClass.RELATIONSHIP
        ) {
            return promptNow()
        }

        val standingClass =
            when {
                candidateClass ==
                    VexLifeConsiderationContract.MemoryCandidateClass.CORRECTION &&
                    current.continuitySignals.humanCorrectionObserved ->
                    StandingConsentClass.HUMAN_CORRECTION

                candidateClass ==
                    VexLifeConsiderationContract.MemoryCandidateClass.PREFERENCE &&
                    signals.explicitPreference ->
                    StandingConsentClass.EXPLICIT_PREFERENCE

                candidateClass ==
                    VexLifeConsiderationContract.MemoryCandidateClass.COMMITMENT &&
                    signals.explicitCommitment ->
                    StandingConsentClass.EXPLICIT_COMMITMENT

                else ->
                    null
            }

        if (
            standingClass != null &&
            standingClass in standingConsent.enabledClasses
        ) {
            return Result(
                disposition = Disposition.AUTO_KEEP_ELIGIBLE,
                standingConsentClass = standingClass,
            )
        }

        if (
            current.continuitySignals.explicitHumanMemoryIntent ||
            current.continuitySignals.humanCorrectionObserved ||
            current.continuitySignals.futureUsefulness ==
                VexLifeConsiderationContract.Level.HIGH
        ) {
            return promptNow()
        }

        return Result(
            disposition = Disposition.BATCH_REVIEW,
        )
    }

    private fun noAction(): Result =
        Result(
            disposition = Disposition.NO_ACTION,
        )

    private fun promptNow(): Result =
        Result(
            disposition = Disposition.PROMPT_NOW,
        )
}

// [VXG RealForever]
