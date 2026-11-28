package vexlife.consideration

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VexLifeConsiderationContractTest {
    private fun candidateSalience(): VexLifeConsiderationContract.Salience =
        VexLifeConsiderationContract.Salience(
            selfAttributedVantage =
                VexLifeConsiderationContract.SelfAttributedVantage(
                    feltImportance = VexLifeConsiderationContract.Level.HIGH,
                    feltUncertainty = VexLifeConsiderationContract.Level.LOW,
                    feltRelationshipShift =
                        VexLifeConsiderationContract.RelationshipShift.CORRECTION,
                    feltNeedToReturn = VexLifeConsiderationContract.ReturnNeed.YES,
                ),
            continuitySignals =
                VexLifeConsiderationContract.ContinuitySignals(
                    olderSharedContextWasNeeded = false,
                    currentExternalTruthWasNeeded = false,
                    humanCorrectionObserved = true,
                    explicitHumanMemoryIntent = false,
                    futureUsefulness = VexLifeConsiderationContract.Level.HIGH,
                    unresolvedOpenLoop = false,
                ),
            memoryCandidate =
                VexLifeConsiderationContract.MemoryCandidate(
                    disposition =
                        VexLifeConsiderationContract.MemoryCandidateDisposition.CANDIDATE,
                    candidateClass =
                        VexLifeConsiderationContract.MemoryCandidateClass.CORRECTION,
                    summary =
                        "The person uses tight to mean compact and causally exact.",
                ),
        )

    @Test
    fun universalReflexIsTypedAlwaysAvailableAndCarriesNoAuthority() {
        val reflex =
            VexLifeConsiderationRouter.formUniversalReflex()

        assertEquals(
            listOf(
                VexLifeConsiderationContract.QuestionRef.ACTUAL_GROUND,
                VexLifeConsiderationContract.QuestionRef.REACH_NEEDED,
                VexLifeConsiderationContract.QuestionRef.ASSUMED_INTERPRETATION,
            ),
            reflex.questionRefs,
        )
        assertTrue(reflex.preservesLiteralHumanRequest)
        assertFalse(reflex.factAuthorityGranted)
        assertFalse(reflex.semanticAcceptanceGranted)
        assertFalse(reflex.effectAuthorityGranted)
    }

    @Test
    fun considerationFrameCarriesQuestionsWithoutAuthority() {
        val frame =
            VexLifeConsiderationContract.validateFrame(
                VexLifeConsiderationContract.Frame(
                    modules =
                        listOf(
                            VexLifeConsiderationContract.Module.CURRENT_TRUTH,
                            VexLifeConsiderationContract.Module.EVIDENCE_UNCERTAINTY,
                        ),
                    questionRefs =
                        listOf(
                            VexLifeConsiderationContract.QuestionRef.CURRENT_TRUTH_REQUIRED,
                            VexLifeConsiderationContract.QuestionRef.LEAST_SUPPORTED_PART,
                        ),
                ),
            )

        assertTrue(frame.preservesLiteralHumanRequest)
        assertFalse(frame.factAuthorityGranted)
        assertFalse(frame.semanticAcceptanceGranted)
        assertFalse(frame.effectAuthorityGranted)
    }

    @Test
    fun considerationFrameCannotGrantFactAuthority() {
        assertFailsWith<IllegalArgumentException> {
            VexLifeConsiderationContract.validateFrame(
                VexLifeConsiderationContract.Frame(
                    modules =
                        listOf(
                            VexLifeConsiderationContract.Module.CURRENT_TRUTH,
                        ),
                    questionRefs =
                        listOf(
                            VexLifeConsiderationContract.QuestionRef.CURRENT_TRUTH_REQUIRED,
                        ),
                    factAuthorityGranted = true,
                ),
            )
        }
    }

    @Test
    fun turnSalienceIsBoundedSelfAttributionNotMemoryAcceptance() {
        val salience =
            VexLifeConsiderationContract.validateSalience(
                candidateSalience(),
            )

        assertEquals(
            VexLifeConsiderationContract.MemoryCandidateDisposition.CANDIDATE,
            salience.memoryCandidate.disposition,
        )
        assertFalse(salience.rawReasoningPersisted)
        assertFalse(salience.factAuthorityGranted)
        assertFalse(salience.semanticAcceptanceGranted)
        assertFalse(salience.memoryWriteAuthorityGranted)
    }

    @Test
    fun noCandidateCannotSmuggleCandidateContent() {
        assertFailsWith<IllegalArgumentException> {
            VexLifeConsiderationContract.validateSalience(
                candidateSalience()
                    .copy(
                        memoryCandidate =
                            VexLifeConsiderationContract.MemoryCandidate(
                                disposition =
                                    VexLifeConsiderationContract.MemoryCandidateDisposition.NONE,
                                summary = "Hidden candidate content.",
                            ),
                    ),
            )
        }
    }

    @Test
    fun epiloguePromptIsOptionalPostAnswerReflectionWithoutAuthority() {
        val prompt =
            VexLifeConsiderationContract.validateEpiloguePrompt(
                VexLifeConsiderationContract.EpiloguePrompt(),
            )

        assertTrue(prompt.allowBlankStems)
        assertTrue(prompt.occursAfterHumanAnswer)
        assertFalse(prompt.effectAuthorityGranted)
        assertFalse(prompt.semanticAcceptanceGranted)
    }

    @Test
    fun routerSelectsOnlyImplicatedQuestionFamilies() {
        val frame =
            requireNotNull(
                VexLifeConsiderationRouter.formOrNull(
                    VexLifeConsiderationRouter.TurnShapeSignals(
                        currentTruthMayBeRequired = true,
                        humanCorrectionMayBePresent = true,
                    ),
                ),
            )

        assertEquals(
            listOf(
                VexLifeConsiderationContract.Module.CURRENT_TRUTH,
                VexLifeConsiderationContract.Module.CORRECTION,
                VexLifeConsiderationContract.Module.MEMORY_SALIENCE,
            ),
            frame.modules,
        )
        assertTrue(
            VexLifeConsiderationContract.QuestionRef.SOURCE_ACTUALLY_RETRIEVED in
                frame.questionRefs,
        )
        assertTrue(
            VexLifeConsiderationContract.QuestionRef.FUTURE_ME_VALUE in
                frame.questionRefs,
        )
    }

    @Test
    fun saturatedRouterPreservesOneQuestionForEveryImplicatedModule() {
        val frame =
            requireNotNull(
                VexLifeConsiderationRouter.formOrNull(
                    VexLifeConsiderationRouter.TurnShapeSignals(
                        currentTruthMayBeRequired = true,
                        olderSharedContextMayBeRequired = true,
                        humanCorrectionMayBePresent = true,
                        explicitMemoryIntentPresent = true,
                        effectOrActionMayBeRequested = true,
                        materialUncertaintyPresent = true,
                        relationalMeaningIsCentral = true,
                    ),
                ),
            )

        assertEquals(VexLifeConsiderationContract.Module.entries, frame.modules)
        assertTrue(frame.questionRefs.size <= VexLifeConsiderationContract.MaximumQuestions)
        listOf(
            VexLifeConsiderationContract.QuestionRef.CURRENT_TRUTH_REQUIRED,
            VexLifeConsiderationContract.QuestionRef.OLDER_SHARED_SOURCE_REQUIRED,
            VexLifeConsiderationContract.QuestionRef.PRIOR_UNDERSTANDING,
            VexLifeConsiderationContract.QuestionRef.FUTURE_ME_VALUE,
            VexLifeConsiderationContract.QuestionRef.EFFECT_ACTUALLY_OCCURRED,
            VexLifeConsiderationContract.QuestionRef.LEAST_SUPPORTED_PART,
            VexLifeConsiderationContract.QuestionRef.HUMAN_INTENTION,
        ).forEach {
            assertTrue(it in frame.questionRefs)
        }
        assertFalse(VexLifeConsiderationContract.QuestionRef.REACH_NEEDED in frame.questionRefs)
    }

    @Test
    fun routerEmitsNothingWhenNoConsiderationSignalExists() {
        assertNull(
            VexLifeConsiderationRouter.formOrNull(
                VexLifeConsiderationRouter.TurnShapeSignals(),
            ),
        )
    }
}

// [VXG RealForever]
