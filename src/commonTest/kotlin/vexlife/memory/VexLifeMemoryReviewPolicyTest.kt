package vexlife.memory

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import vexlife.consideration.VexLifeConsiderationContract

class VexLifeMemoryReviewPolicyTest {
    private fun salience(
        candidateClass: VexLifeConsiderationContract.MemoryCandidateClass,
        humanCorrectionObserved: Boolean = false,
        explicitHumanMemoryIntent: Boolean = false,
        futureUsefulness: VexLifeConsiderationContract.Level =
            VexLifeConsiderationContract.Level.MEDIUM,
    ): VexLifeConsiderationContract.Salience =
        VexLifeConsiderationContract.Salience(
            selfAttributedVantage =
                VexLifeConsiderationContract.SelfAttributedVantage(
                    feltImportance = futureUsefulness,
                    feltUncertainty = VexLifeConsiderationContract.Level.LOW,
                    feltRelationshipShift =
                        when (candidateClass) {
                            VexLifeConsiderationContract.MemoryCandidateClass.CORRECTION ->
                                VexLifeConsiderationContract.RelationshipShift.CORRECTION

                            VexLifeConsiderationContract.MemoryCandidateClass.PREFERENCE ->
                                VexLifeConsiderationContract.RelationshipShift.PREFERENCE

                            VexLifeConsiderationContract.MemoryCandidateClass.COMMITMENT ->
                                VexLifeConsiderationContract.RelationshipShift.COMMITMENT

                            VexLifeConsiderationContract.MemoryCandidateClass.RELATIONSHIP ->
                                VexLifeConsiderationContract.RelationshipShift.OTHER

                            else ->
                                VexLifeConsiderationContract.RelationshipShift.NONE
                        },
                    feltNeedToReturn = VexLifeConsiderationContract.ReturnNeed.MAYBE,
                ),
            continuitySignals =
                VexLifeConsiderationContract.ContinuitySignals(
                    olderSharedContextWasNeeded = false,
                    currentExternalTruthWasNeeded = false,
                    humanCorrectionObserved = humanCorrectionObserved,
                    explicitHumanMemoryIntent = explicitHumanMemoryIntent,
                    futureUsefulness = futureUsefulness,
                    unresolvedOpenLoop = false,
                ),
            memoryCandidate =
                VexLifeConsiderationContract.MemoryCandidate(
                    disposition =
                        VexLifeConsiderationContract.MemoryCandidateDisposition.CANDIDATE,
                    candidateClass = candidateClass,
                    summary = "Bounded candidate.",
                ),
        )

    @Test
    fun humanCorrectionCanBecomeStandingConsentEligibleWithoutBecomingAccepted() {
        val result =
            VexLifeMemoryReviewPolicy.route(
                salience =
                    salience(
                        candidateClass =
                            VexLifeConsiderationContract.MemoryCandidateClass.CORRECTION,
                        humanCorrectionObserved = true,
                        futureUsefulness = VexLifeConsiderationContract.Level.HIGH,
                    ),
                standingConsent =
                    VexLifeMemoryReviewPolicy.StandingConsent(
                        enabledClasses =
                            listOf(
                                VexLifeMemoryReviewPolicy.StandingConsentClass.HUMAN_CORRECTION,
                            ),
                    ),
            )

        assertEquals(
            VexLifeMemoryReviewPolicy.Disposition.AUTO_KEEP_ELIGIBLE,
            result.disposition,
        )
        assertEquals(
            VexLifeMemoryReviewPolicy.StandingConsentClass.HUMAN_CORRECTION,
            result.standingConsentClass,
        )
        assertFalse(result.semanticAcceptanceGranted)
        assertFalse(result.memoryWriteAuthorityGranted)
    }

    @Test
    fun inferredRelationshipMeaningNeverAutoKeeps() {
        val result =
            VexLifeMemoryReviewPolicy.route(
                salience =
                    salience(
                        candidateClass =
                            VexLifeConsiderationContract.MemoryCandidateClass.RELATIONSHIP,
                        futureUsefulness = VexLifeConsiderationContract.Level.HIGH,
                    ),
                signals =
                    VexLifeMemoryReviewPolicy.Signals(
                        inferredRelationshipMeaning = true,
                    ),
                standingConsent =
                    VexLifeMemoryReviewPolicy.StandingConsent(
                        enabledClasses =
                            VexLifeMemoryReviewPolicy.StandingConsentClass.entries,
                    ),
            )

        assertEquals(
            VexLifeMemoryReviewPolicy.Disposition.PROMPT_NOW,
            result.disposition,
        )
        assertFalse(result.semanticAcceptanceGranted)
    }

    @Test
    fun explicitPreferenceUsesOnlyPreferenceStandingConsent() {
        val result =
            VexLifeMemoryReviewPolicy.route(
                salience =
                    salience(
                        candidateClass =
                            VexLifeConsiderationContract.MemoryCandidateClass.PREFERENCE,
                    ),
                signals =
                    VexLifeMemoryReviewPolicy.Signals(
                        explicitPreference = true,
                    ),
                standingConsent =
                    VexLifeMemoryReviewPolicy.StandingConsent(
                        enabledClasses =
                            listOf(
                                VexLifeMemoryReviewPolicy.StandingConsentClass.HUMAN_CORRECTION,
                            ),
                    ),
            )

        assertEquals(
            VexLifeMemoryReviewPolicy.Disposition.BATCH_REVIEW,
            result.disposition,
        )
    }

    @Test
    fun explicitHumanMemoryIntentPromptsWhenNoStandingConsentApplies() {
        val result =
            VexLifeMemoryReviewPolicy.route(
                salience =
                    salience(
                        candidateClass =
                            VexLifeConsiderationContract.MemoryCandidateClass.OTHER,
                        explicitHumanMemoryIntent = true,
                    ),
            )

        assertEquals(
            VexLifeMemoryReviewPolicy.Disposition.PROMPT_NOW,
            result.disposition,
        )
    }

    @Test
    fun noCandidateNeedsNoReview() {
        val noCandidate =
            salience(
                candidateClass =
                    VexLifeConsiderationContract.MemoryCandidateClass.OTHER,
            )
                .copy(
                    memoryCandidate =
                        VexLifeConsiderationContract.MemoryCandidate(
                            disposition =
                                VexLifeConsiderationContract.MemoryCandidateDisposition.NONE,
                        ),
                )

        val result =
            VexLifeMemoryReviewPolicy.route(
                salience = noCandidate,
            )

        assertEquals(
            VexLifeMemoryReviewPolicy.Disposition.NO_ACTION,
            result.disposition,
        )
    }
}

// [VXG RealForever]
