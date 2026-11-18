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
    fun considerationFrameCarriesQuestionsWithoutAuthority() {
        val frame =
            VexLifeConsiderationContract.validateFrame(
                VexLifeConsiderationContract.Frame(
                    modules =
                        listOf(
                            VexLifeConsiderationContract.Module.CURRENT_TRUTH,
                            VexLifeConsiderationContract.Module.EVIDENCE_UNCERTAINTY,
                        ),
                    questions =
                        listOf(
                            "Is current outside truth required?",
                            "What part of my intended answer is least supported?",
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
                    questions =
                        listOf(
                            "Is current outside truth required?",
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
            frame.questions.any {
                it.contains(
                    "actually retrieved",
                )
            },
        )
        assertTrue(
            frame.questions.any {
                it.contains(
                    "Future Me",
                )
            },
        )
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
