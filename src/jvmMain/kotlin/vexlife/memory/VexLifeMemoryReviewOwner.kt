package vexlife.memory

import java.nio.charset.StandardCharsets
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import java.time.Instant
import java.time.format.DateTimeFormatterBuilder
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import vexlife.consideration.VexLifeConsiderationContract
import vexlife.contract.VexContractJson
import vexlife.contract.VexDurableContractFile
import vexlife.conversation.DirectConversationIdentity
import vexlife.conversation.DirectConversationStore
import vexlife.filesystem.VexExclusiveFileLease
import vexlife.filesystem.VexOwnedPath
import vexlife.home.VexLifeProdHomeFiles

/**
 * Durable owner for one human-correction Memory review.
 *
 * This owner stores only pending-review / decision evidence. It is not a second Memory truth store.
 * Keep delegates semantic acceptance to [VexLifeSemanticAcceptanceWriter] and current statement
 * membership to [VexLifeScoreContinuity]. Not now / Decline never call either owner.
 *
 * A pending review is cleared only after the requested effect is fully proven. Retry reconciles an
 * already-current semantic acceptance and an already-current deterministic Score statement before
 * attempting either mutation again.
 */
internal object VexLifeMemoryReviewOwner {
    private const val ReviewSchema =
        "vexlife-prod.memory-review-candidate/v1"
    private const val DecisionSchema =
        "vexlife-prod.memory-review-decision/v1"
    private const val StateSchema =
        "vexlife-prod.memory-review-state/v1"

    private const val StatusPending =
        "PENDING"
    private const val StatusIdle =
        "IDLE"

    private const val ProposerRef =
        "owner.vexlife-prod.memory-review.human-correction"

    data class ProposeHumanCorrectionInvocation(
        val semanticSubjectRef: String,
        val salience: VexLifeConsiderationContract.Salience,
        val sourceEventHashes: List<String>,
        val formedAt: Instant,
    )

    data class PendingReview(
        val candidateRef: VexLifeMemoryReviewFamily.CandidateRef,
        val reviewSha256: String,
        val summary: String,
        val sourceConversationHeadSha256: String,
        val proposalCandidateRef: String,
        val proposalCandidateSha256: String,
        val semanticSubjectFingerprint: String,
        val expectedSemanticAuthorityHeadSha256: String?,
    )

    sealed interface ActionResult {
        val decisionRef: String
        val decisionSha256: String

        data class Kept(
            override val decisionRef: String,
            override val decisionSha256: String,
            val reviewRef: String,
            val semanticAcceptanceRef: String,
            val semanticAcceptanceSha256: String,
            val statementRef: String,
            val scoreHeadSha256: String,
            val semanticAcceptanceRecovered: Boolean,
            val scoreStatementRecovered: Boolean,
        ) : ActionResult

        data class Deferred(
            override val decisionRef: String,
            override val decisionSha256: String,
            val reviewRef: String,
        ) : ActionResult

        data class Declined(
            override val decisionRef: String,
            override val decisionSha256: String,
            val reviewRef: String,
        ) : ActionResult
    }

    internal data class Faults(
        val failAfterDecisionReceipt: Boolean =
            false,
        val failAfterSemanticAcceptance: Boolean =
            false,
        val failAfterScore: Boolean =
            false,
    )

    @Serializable
    private data class StateFile(
        val schemaVersion: String,
        val status: String,
        val reviewRefOrNull: String?,
        val reviewSha256OrNull: String?,
        val lastDecisionRefOrNull: String?,
        val lastDecisionSha256OrNull: String?,
        val updatedAt: String,
    )

    private data class Identity(
        val lineageRef: String,
        val threadRef: String,
    )

    private data class DecisionReceipt(
        val decisionRef: String,
        val decisionSha256: String,
        val decidedAt: Instant,
        val value: JsonObject,
    )

    private data class AcceptanceBinding(
        val acceptanceRef: String,
        val acceptanceSha256: String,
    )

    fun proposeHumanCorrection(
        productHome: Path,
        invocation: ProposeHumanCorrectionInvocation,
    ): PendingReview {
        requireSafeRef(
            invocation.semanticSubjectRef,
            "semanticSubjectRef",
        )
        require(
            invocation.sourceEventHashes.size >=
                2 &&
                invocation.sourceEventHashes.size %
                2 ==
                0 &&
                invocation.sourceEventHashes.distinct().size ==
                invocation.sourceEventHashes.size,
        ) {
            "Memory review source requires unique complete Conversation pairs"
        }
        invocation.sourceEventHashes.forEach {
            requireSha256(
                it,
                "sourceEventHash",
            )
        }

        val salience =
            VexLifeConsiderationContract.validateSalience(
                invocation.salience,
            )
        val candidate =
            salience.memoryCandidate
        require(
            candidate.disposition ==
                VexLifeConsiderationContract.MemoryCandidateDisposition.CANDIDATE &&
                candidate.candidateClass ==
                    VexLifeConsiderationContract.MemoryCandidateClass.CORRECTION &&
                salience.continuitySignals.humanCorrectionObserved,
        ) {
            "first Memory review owner admits only one bounded observed human-correction candidate"
        }
        val routed =
            VexLifeMemoryReviewPolicy.route(
                salience,
            )
        require(
            routed.disposition ==
                VexLifeMemoryReviewPolicy.Disposition.PROMPT_NOW &&
                !routed.semanticAcceptanceGranted &&
                !routed.memoryWriteAuthorityGranted,
        ) {
            "human-correction review must remain one explicit review prompt with no acceptance authority"
        }

        val home =
            VexOwnedPath.requireRoot(
                productHome,
                "canonical VexLife Home",
            )
        val identity =
            currentIdentity(
                home,
            )
        val root =
            reviewRoot(
                home = home,
                identity = identity,
            )
        VexOwnedPath.ensureDirectory(
            home,
            root,
        )

        return VexExclusiveFileLease
            .acquire(
                root.resolve(
                    ".review.lock",
                ),
            )
            .use {
                val semanticCandidate =
                    VexLifeScoreContinuity.formCandidate(
                        productHome = home,
                        input =
                            VexLifeScoreContinuity.CandidateInput(
                                semanticSubjectRef =
                                    invocation.semanticSubjectRef,
                                proposedSummary =
                                    requireNotNull(
                                        candidate.summary,
                                    ),
                                proposedMemoryRelation =
                                    VexLifeSemanticAcceptanceWriter.MemoryRelation.SHARED_RELATIONSHIP_HISTORY.wireValue,
                                proposedStatementState =
                                    VexLifeSemanticAcceptanceWriter.InitialStatementState.HUMAN_CONFIRMED.wireValue,
                                proposerRef =
                                    ProposerRef,
                                proposerClass =
                                    VexLifeSemanticAcceptanceWriter.ProposerClass.LINEAGE.wireValue,
                                formedAt =
                                    invocation.formedAt,
                                sourceEventHashes =
                                    invocation.sourceEventHashes,
                            ),
                    )
                val currentAuthority =
                    VexLifeCurrentMemoryReader
                        .readCurrentSemanticAuthorityHead(
                            home,
                        )
                val expectedAuthoritySha256 =
                    currentAuthority
                        ?.string(
                            "semanticAuthorityHeadSha256",
                        )
                val review =
                    formReview(
                        identity = identity,
                        semanticSubjectRef =
                            invocation.semanticSubjectRef,
                        summary =
                            requireNotNull(
                                candidate.summary,
                            ),
                        semanticCandidate =
                            semanticCandidate,
                        expectedSemanticAuthorityHeadSha256 =
                            expectedAuthoritySha256,
                        formedAt =
                            invocation.formedAt,
                    )
                val reviewRef =
                    review.string(
                        "reviewRef",
                    )
                val reviewSha256 =
                    review.string(
                        "reviewSha256",
                    )

                val state =
                    readState(
                        home = home,
                        root = root,
                    )
                if (
                    state !=
                    null &&
                    state.status ==
                    StatusPending
                ) {
                    require(
                        state.reviewRefOrNull ==
                            reviewRef &&
                            state.reviewSha256OrNull ==
                            reviewSha256,
                    ) {
                        "MEMORY_REVIEW_PENDING: another review already awaits a human decision"
                    }
                    return@use pendingReview(
                        review,
                    )
                }

                writeImmutable(
                    home = home,
                    path =
                        reviewPath(
                            root = root,
                            reviewSha256 =
                                reviewSha256,
                        ),
                    value = review,
                    label =
                        "Memory review candidate",
                )
                writeState(
                    home = home,
                    root = root,
                    value =
                        StateFile(
                            schemaVersion =
                                StateSchema,
                            status =
                                StatusPending,
                            reviewRefOrNull =
                                reviewRef,
                            reviewSha256OrNull =
                                reviewSha256,
                            lastDecisionRefOrNull =
                                null,
                            lastDecisionSha256OrNull =
                                null,
                            updatedAt =
                                canonicalUtc(
                                    invocation.formedAt,
                                ),
                        ),
                )
                pendingReview(
                    review,
                )
            }
    }

    fun observe(
        productHome: Path,
    ): VexLifeMemoryReviewFamily.State {
        val home =
            VexOwnedPath.requireRoot(
                productHome,
                "canonical VexLife Home",
            )
        val identity =
            currentIdentity(
                home,
            )
        val root =
            reviewRoot(
                home = home,
                identity = identity,
            )
        if (
            VexOwnedPath.observeDirectory(
                home,
                root,
            ) ==
            VexOwnedPath.DirectoryState.Missing
        ) {
            return VexLifeMemoryReviewFamily.Idle
        }
        val state =
            readState(
                home = home,
                root = root,
            )
                ?: return VexLifeMemoryReviewFamily.Idle
        if (
            state.status ==
            StatusIdle
        ) {
            return VexLifeMemoryReviewFamily.Idle
        }
        require(
            state.status ==
                StatusPending
        ) {
            "Memory review state is invalid"
        }
        val review =
            readReview(
                home = home,
                root = root,
                reviewRef =
                    requireNotNull(
                        state.reviewRefOrNull,
                    ),
                reviewSha256 =
                    requireNotNull(
                        state.reviewSha256OrNull,
                    ),
            )
        return pendingState(
            review,
        )
    }

    fun act(
        productHome: Path,
        control: VexLifeMemoryReviewFamily.Control,
        humanSubjectRef: String,
        decidedAt: Instant,
        faults: Faults =
            Faults(),
    ): ActionResult {
        requireSafeRef(
            humanSubjectRef,
            "humanSubjectRef",
        )
        val home =
            VexOwnedPath.requireRoot(
                productHome,
                "canonical VexLife Home",
            )
        val identity =
            currentIdentity(
                home,
            )
        val root =
            reviewRoot(
                home = home,
                identity = identity,
            )
        require(
            VexOwnedPath.observeDirectory(
                home,
                root,
            ) ==
                VexOwnedPath.DirectoryState.Present,
        ) {
            "Memory review has no current pending candidate"
        }

        return VexExclusiveFileLease
            .acquire(
                root.resolve(
                    ".review.lock",
                ),
            )
            .use {
                val state =
                    requireNotNull(
                        readState(
                            home = home,
                            root = root,
                        ),
                    ) {
                        "Memory review has no current state"
                    }
                require(
                    state.status ==
                        StatusPending,
                ) {
                    "Memory review has no current pending candidate"
                }
                val review =
                    readReview(
                        home = home,
                        root = root,
                        reviewRef =
                            requireNotNull(
                                state.reviewRefOrNull,
                            ),
                        reviewSha256 =
                            requireNotNull(
                                state.reviewSha256OrNull,
                            ),
                    )
                val familyState =
                    pendingState(
                        review,
                    )
                val resolved =
                    requireNotNull(
                        VexLifeMemoryReviewFamily.resolve(
                            currentState =
                                familyState,
                            control =
                                control,
                        ),
                    ) {
                        "Memory review Control is not lawful for the current candidate"
                    }
                val effect =
                    requireNotNull(
                        resolved.effect,
                    )
                if (
                    effect is
                    VexLifeMemoryReviewFamily.Effect.Accept
                ) {
                    requireReviewConversationCurrent(
                        home = home,
                        review = review,
                    )
                }

                val existingDecision =
                    readDecisionForReview(
                        home = home,
                        root = root,
                        review = review,
                    )
                val decision =
                    existingDecision
                        ?.also {
                            require(
                                it.value.string(
                                    "humanSubjectRef",
                                ) ==
                                    humanSubjectRef,
                            ) {
                                "MEMORY_REVIEW_ALREADY_DECIDED: review belongs to an existing human decision"
                            }
                            require(
                                it.value.string(
                                    "controlRef",
                                ) ==
                                    control.controlRef,
                            ) {
                                "MEMORY_REVIEW_ALREADY_DECIDED: review already has a different durable decision"
                            }
                        }
                        ?: formDecision(
                            review = review,
                            control = control,
                            humanSubjectRef =
                                humanSubjectRef,
                            decidedAt =
                                decidedAt,
                        )
                when (
                    effect
                ) {
                    is VexLifeMemoryReviewFamily.Effect.Defer -> {
                        writeDecisionReceipt(
                            home = home,
                            root = root,
                            decision = decision,
                            faults = faults,
                        )
                        writeDecisionAndResolve(
                            home = home,
                            root = root,
                            decision = decision,
                            decidedAt = decision.decidedAt,
                        )
                        ActionResult.Deferred(
                            decisionRef =
                                decision.decisionRef,
                            decisionSha256 =
                                decision.decisionSha256,
                            reviewRef =
                                review.string(
                                    "reviewRef",
                                ),
                        )
                    }

                    is VexLifeMemoryReviewFamily.Effect.Dismiss -> {
                        writeDecisionReceipt(
                            home = home,
                            root = root,
                            decision = decision,
                            faults = faults,
                        )
                        writeDecisionAndResolve(
                            home = home,
                            root = root,
                            decision = decision,
                            decidedAt = decision.decidedAt,
                        )
                        ActionResult.Declined(
                            decisionRef =
                                decision.decisionRef,
                            decisionSha256 =
                                decision.decisionSha256,
                            reviewRef =
                                review.string(
                                    "reviewRef",
                                ),
                        )
                    }

                    is VexLifeMemoryReviewFamily.Effect.Accept ->
                        keep(
                            home = home,
                            root = root,
                            review = review,
                            decision = decision,
                            faults =
                                faults,
                        )
                }
            }
    }

    private fun keep(
        home: Path,
        root: Path,
        review: JsonObject,
        decision: DecisionReceipt,
        faults: Faults,
    ): ActionResult.Kept {
        val conversationStore =
            DirectConversationStore(
                home,
            )

        return conversationStore.withWriterLease(
            instanceRef =
                "instance.vexlife-prod.memory-review.keep-transaction",
        ) {
            val conversationHead =
                requireNotNull(
                    current().head,
                ) {
                    "Memory Keep requires one current completed Conversation"
                }
            val expectedConversationHeadSha256 =
                review.string(
                    "sourceConversationHeadSha256",
                )
            require(
                conversationHead.conversationHeadSha256 ==
                    expectedConversationHeadSha256,
            ) {
                "MEMORY_REVIEW_STALE: Conversation advanced after this review was formed"
            }
            writeDecisionReceipt(
                home = home,
                root = root,
                decision = decision,
                faults = faults,
            )

            keepWhileConversationLocked(
                home = home,
                root = root,
                review = review,
                decision = decision,
                conversation = this,
                expectedConversationHeadSha256 =
                    expectedConversationHeadSha256,
                faults = faults,
            )
        }
    }

    private fun keepWhileConversationLocked(
        home: Path,
        root: Path,
        review: JsonObject,
        decision: DecisionReceipt,
        conversation: DirectConversationStore.Locked,
        expectedConversationHeadSha256: String,
        faults: Faults,
    ): ActionResult.Kept {
        val existingAcceptance =
            currentAcceptanceForReview(
                home = home,
                review = review,
                decision = decision,
            )
        val acceptance =
            if (
                existingAcceptance !=
                null
            ) {
                existingAcceptance
            } else {
                val result =
                    VexLifeSemanticAcceptanceWriter.keepWithConversationLease(
                        productHome = home,
                        conversation = conversation,
                        expectedConversationHeadSha256 =
                            expectedConversationHeadSha256,
                        invocation =
                            VexLifeSemanticAcceptanceWriter.KeepInvocation(
                                instanceRef =
                                    "instance.vexlife-prod.memory-review.keep",
                                proposal =
                                    VexLifeSemanticAcceptanceWriter.Proposal(
                                        semanticSubjectRef =
                                            review.string(
                                                "semanticSubjectRef",
                                            ),
                                        proposedSummary =
                                            review.string(
                                                "summary",
                                            ),
                                        memoryRelation =
                                            VexLifeSemanticAcceptanceWriter.MemoryRelation.SHARED_RELATIONSHIP_HISTORY,
                                        proposedStatementState =
                                            VexLifeSemanticAcceptanceWriter.InitialStatementState.HUMAN_CONFIRMED,
                                        proposerRef =
                                            ProposerRef,
                                        proposerClass =
                                            VexLifeSemanticAcceptanceWriter.ProposerClass.LINEAGE,
                                        sourceEventHashes =
                                            review.stringList(
                                                "sourceEventHashes",
                                            ),
                                    ),
                                classification =
                                    VexLifeSemanticAcceptanceWriter.Classification.Initial(
                                        acceptedSummary =
                                            review.string(
                                                "summary",
                                            ),
                                        memoryRelation =
                                            VexLifeSemanticAcceptanceWriter.MemoryRelation.SHARED_RELATIONSHIP_HISTORY,
                                        statementState =
                                            VexLifeSemanticAcceptanceWriter.InitialStatementState.HUMAN_CONFIRMED,
                                    ),
                                humanDecision =
                                    VexLifeSemanticAcceptanceWriter.HumanDecisionSource(
                                        humanSubjectRef =
                                            humanSubjectRef,
                                        decisionRef =
                                            decision.decisionRef,
                                        decisionSha256 =
                                            decision.decisionSha256,
                                        decidedAt =
                                            decision.decidedAt,
                                    ),
                                expectedSemanticAuthorityHeadSha256 =
                                    review.nullableString(
                                        "expectedSemanticAuthorityHeadSha256",
                                    ),
                                formedAt =
                                    decision.decidedAt,
                            ),
                    )
                val resolvedAcceptance =
                    VexLifeCurrentMemoryReader
                        .readCurrentScoreAcceptance(
                            productHome = home,
                            acceptanceRef =
                                result.acceptanceRef,
                            acceptanceSha256 =
                                result.acceptanceSha256,
                        )
                requireAcceptedCandidateMatchesReview(
                    review = review,
                    candidate =
                        resolvedAcceptance.candidate,
                )
                AcceptanceBinding(
                    acceptanceRef =
                        result.acceptanceRef,
                    acceptanceSha256 =
                        result.acceptanceSha256,
                )
            }

        if (
            faults.failAfterSemanticAcceptance
        ) {
            error(
                "SIMULATED_MEMORY_REVIEW_FAILURE_AFTER_ACCEPTANCE",
            )
        }

        val statementRef =
            statementRef(
                review,
            )
        val coordinates =
            VexLifeCurrentMemoryReader
                .readCurrentScoreCoordinates(
                    home,
                )
        val statementIsCurrent =
            statementRef in
                coordinates.currentStatementRefs
        val existingStatement =
            if (
                statementIsCurrent
            ) {
                requireNotNull(
                    VexLifeCurrentMemoryReader
                        .readCurrentScoreStatementBinding(
                            productHome = home,
                            statementRef =
                                statementRef,
                        ),
                ) {
                    "current Score coordinates name a statement whose binding cannot be resolved"
                }
            } else {
                null
            }
        val scoreRecovered =
            existingStatement !=
                null
        val scoreHeadSha256 =
            if (
                existingStatement !=
                null
            ) {
                require(
                    existingStatement.semanticAcceptanceRef ==
                        acceptance.acceptanceRef &&
                        existingStatement.semanticAcceptanceSha256 ==
                            acceptance.acceptanceSha256,
                ) {
                    "current Score statement exists with different semantic acceptance"
                }
                requireNotNull(
                    coordinates.scoreHeadSha256,
                ) {
                    "current Score statement has no current Score head"
                }
            } else {
                VexLifeScoreContinuity
                    .appendAcceptedStatement(
                        productHome = home,
                        invocation =
                            VexLifeScoreContinuity.AppendStatementInvocation(
                                instanceRef =
                                    "instance.vexlife-prod.memory-review.score",
                                statementRef =
                                    statementRef,
                                semanticAcceptanceRef =
                                    acceptance.acceptanceRef,
                                semanticAcceptanceSha256 =
                                    acceptance.acceptanceSha256,
                                expectedScoreHeadSha256 =
                                    coordinates.scoreHeadSha256,
                                formedAt =
                                    decision.decidedAt.plusMillis(
                                        1,
                                    ),
                            ),
                    )
                    .scoreHeadSha256
            }

        if (
            faults.failAfterScore
        ) {
            error(
                "SIMULATED_MEMORY_REVIEW_FAILURE_AFTER_SCORE",
            )
        }

        writeState(
            home = home,
            root = root,
            value =
                StateFile(
                    schemaVersion =
                        StateSchema,
                    status =
                        StatusIdle,
                    reviewRefOrNull =
                        null,
                    reviewSha256OrNull =
                        null,
                    lastDecisionRefOrNull =
                        decision.decisionRef,
                    lastDecisionSha256OrNull =
                        decision.decisionSha256,
                    updatedAt =
                        canonicalUtc(
                            decision.decidedAt.plusMillis(
                                2,
                            ),
                        ),
                ),
        )

        return ActionResult.Kept(
            decisionRef =
                decision.decisionRef,
            decisionSha256 =
                decision.decisionSha256,
            reviewRef =
                review.string(
                    "reviewRef",
                ),
            semanticAcceptanceRef =
                acceptance.acceptanceRef,
            semanticAcceptanceSha256 =
                acceptance.acceptanceSha256,
            statementRef =
                statementRef,
            scoreHeadSha256 =
                scoreHeadSha256,
            semanticAcceptanceRecovered =
                existingAcceptance !=
                    null,
            scoreStatementRecovered =
                scoreRecovered,
        )
    }

    private fun currentAcceptanceForReview(
        home: Path,
        review: JsonObject,
        decision: DecisionReceipt,
    ): AcceptanceBinding? {
        val authority =
            VexLifeCurrentMemoryReader
                .readCurrentSemanticAuthorityHead(
                    home,
                )
                ?: return null
        val binding =
            authority
                .objectList(
                    "currentAcceptanceBindings",
                )
                .singleOrNull {
                    it.string(
                        "semanticSubjectFingerprint",
                    ) ==
                        review.string(
                            "semanticSubjectFingerprint",
                        )
                }
                ?: return null
        val acceptance =
            VexLifeCurrentMemoryReader
                .readCurrentScoreAcceptance(
                    productHome = home,
                    acceptanceRef =
                        binding.string(
                            "acceptanceRef",
                        ),
                    acceptanceSha256 =
                        binding.string(
                            "acceptanceSha256",
                        ),
                )
        requireAcceptedCandidateMatchesReview(
            review = review,
            candidate =
                acceptance.candidate,
        )
        val decisionBound =
            acceptance
                .classificationEvidence
                .flatMap {
                    it.objectList(
                        "sourceEvidenceBindings",
                    )
                }
                .any {
                    it.string(
                        "sourceRef",
                    ) ==
                        decision.decisionRef &&
                        it.string(
                            "sourceSha256",
                        ) ==
                            decision.decisionSha256
                }
        require(
            decisionBound,
        ) {
            "current semantic acceptance for this candidate is bound to a different human decision"
        }
        return AcceptanceBinding(
            acceptanceRef =
                binding.string(
                    "acceptanceRef",
                ),
            acceptanceSha256 =
                binding.string(
                    "acceptanceSha256",
                ),
        )
    }

    private fun requireAcceptedCandidateMatchesReview(
        review: JsonObject,
        candidate: JsonObject,
    ) {
        require(
            candidate.string(
                "semanticSubjectRef",
            ) ==
                review.string(
                    "semanticSubjectRef",
                ) &&
                candidate.string(
                    "semanticSubjectFingerprint",
                ) ==
                    review.string(
                        "semanticSubjectFingerprint",
                    ) &&
                candidate.string(
                    "sourceConversationHeadSha256",
                ) ==
                    review.string(
                        "sourceConversationHeadSha256",
                    ) &&
                candidate
                    .objectList(
                        "sourceBindings",
                    )
                    .map {
                        it.string(
                            "eventHash",
                        )
                    } ==
                    review.stringList(
                        "sourceEventHashes",
                    ) &&
                candidate.string(
                    "proposedSummary",
                ) ==
                    review.string(
                        "summary",
                    ) &&
                candidate.string(
                    "proposedMemoryRelation",
                ) ==
                    review.string(
                        "memoryRelation",
                    ) &&
                candidate.string(
                    "proposedStatementState",
                ) ==
                    review.string(
                        "statementState",
                    ) &&
                candidate.string(
                    "proposerRef",
                ) ==
                    review.string(
                        "proposerRef",
                    ) &&
                candidate.string(
                    "proposerClass",
                ) ==
                    review.string(
                        "proposerClass",
                    ),
        ) {
            "current semantic acceptance does not match the pending review meaning/source"
        }
    }

    private fun formReview(
        identity: Identity,
        semanticSubjectRef: String,
        summary: String,
        semanticCandidate: VexLifeScoreContinuity.CandidateResult,
        expectedSemanticAuthorityHeadSha256: String?,
        formedAt: Instant,
    ): JsonObject =
        contentAddressed(
            prefix =
                "review.vexlife-prod.memory",
            refField =
                "reviewRef",
            hashField =
                "reviewSha256",
            core =
                buildJsonObject {
                    put(
                        "schemaVersion",
                        ReviewSchema,
                    )
                    put(
                        "sourceLineageRef",
                        identity.lineageRef,
                    )
                    put(
                        "sourceThreadRef",
                        identity.threadRef,
                    )
                    put(
                        "sourceConversationHeadSha256",
                        semanticCandidate.sourceConversationHeadSha256,
                    )
                    put(
                        "sourceEventHashes",
                        JsonArray(
                            semanticCandidate
                                .sourceEventHashes
                                .map(
                                    ::JsonPrimitive,
                                ),
                        ),
                    )
                    put(
                        "semanticSubjectRef",
                        semanticSubjectRef,
                    )
                    put(
                        "summary",
                        summary,
                    )
                    put(
                        "candidateClass",
                        VexLifeConsiderationContract.MemoryCandidateClass.CORRECTION.name,
                    )
                    put(
                        "proposalCandidateRef",
                        semanticCandidate.candidateRef,
                    )
                    put(
                        "proposalCandidateSha256",
                        semanticCandidate.candidateSha256,
                    )
                    put(
                        "semanticSubjectFingerprint",
                        semanticCandidate.candidate.string(
                            "semanticSubjectFingerprint",
                        ),
                    )
                    putNullable(
                        "expectedSemanticAuthorityHeadSha256",
                        expectedSemanticAuthorityHeadSha256,
                    )
                    put(
                        "memoryRelation",
                        VexLifeSemanticAcceptanceWriter.MemoryRelation.SHARED_RELATIONSHIP_HISTORY.wireValue,
                    )
                    put(
                        "statementState",
                        VexLifeSemanticAcceptanceWriter.InitialStatementState.HUMAN_CONFIRMED.wireValue,
                    )
                    put(
                        "proposerRef",
                        ProposerRef,
                    )
                    put(
                        "proposerClass",
                        VexLifeSemanticAcceptanceWriter.ProposerClass.LINEAGE.wireValue,
                    )
                    put(
                        "formedAt",
                        canonicalUtc(
                            formedAt,
                        ),
                    )
                },
        )

    private fun requireReviewConversationCurrent(
        home: Path,
        review: JsonObject,
    ) {
        val current =
            DirectConversationStore(
                home,
            )
                .current()
                .head
        require(
            current
                ?.conversationHeadSha256 ==
                review.string(
                    "sourceConversationHeadSha256",
                ),
        ) {
            "MEMORY_REVIEW_STALE: Conversation advanced after this review was formed"
        }
    }

    private fun readDecisionForReview(
        home: Path,
        root: Path,
        review: JsonObject,
    ): DecisionReceipt? {
        val decisionsRoot =
            root.resolve(
                "decisions",
            )
        if (
            VexOwnedPath.observeDirectory(
                home,
                decisionsRoot,
            ) ==
            VexOwnedPath.DirectoryState.Missing
        ) {
            return null
        }

        val matching =
            Files.list(
                decisionsRoot,
            )
                .use {
                    stream ->
                    stream
                        .toList()
                }
                .sortedBy {
                    it.fileName
                        .toString()
                }
                .map {
                    path ->
                    val text =
                        requireNotNull(
                            VexDurableContractFile.readText(
                                ownedRoot = home,
                                path = path,
                            ),
                        ) {
                            "Memory review decision is unreadable"
                        }
                    val value =
                        VexContractJson.Strict
                            .parseToJsonElement(
                                text,
                            )
                            .jsonObject
                    val decisionSha256 =
                        value.string(
                            "decisionSha256",
                        )
                    require(
                        path.fileName
                            .toString() ==
                            decisionSha256 +
                                ".json" &&
                            value.string(
                                "schemaVersion",
                            ) ==
                                DecisionSchema &&
                            semanticHash(
                                value.without(
                                    "decisionSha256",
                                ),
                            ) ==
                                decisionSha256,
                    ) {
                        "Memory review decision failed content-addressed validation"
                    }
                    DecisionReceipt(
                        decisionRef =
                            value.string(
                                "decisionRef",
                            ),
                        decisionSha256 =
                            decisionSha256,
                        decidedAt =
                            Instant.parse(
                                value.string(
                                    "decidedAt",
                                ),
                            ),
                        value =
                            value,
                    )
                }
                .filter {
                    it.value.string(
                        "reviewRef",
                    ) ==
                        review.string(
                            "reviewRef",
                        ) &&
                        it.value.string(
                            "reviewSha256",
                        ) ==
                            review.string(
                                "reviewSha256",
                            )
                }

        require(
            matching.size <=
                1,
        ) {
            "Memory review has multiple durable human decisions"
        }
        return matching.singleOrNull()
    }

    private fun formDecision(
        review: JsonObject,
        control: VexLifeMemoryReviewFamily.Control,
        humanSubjectRef: String,
        decidedAt: Instant,
    ): DecisionReceipt {
        val value =
            contentAddressed(
                prefix =
                    "decision.vexlife-prod.memory-review",
                refField =
                    "decisionRef",
                hashField =
                    "decisionSha256",
                core =
                    buildJsonObject {
                        put(
                            "schemaVersion",
                            DecisionSchema,
                        )
                        put(
                            "reviewRef",
                            review.string(
                                "reviewRef",
                            ),
                        )
                        put(
                            "reviewSha256",
                            review.string(
                                "reviewSha256",
                            ),
                        )
                        put(
                            "proposalCandidateRef",
                            review.string(
                                "proposalCandidateRef",
                            ),
                        )
                        put(
                            "proposalCandidateSha256",
                            review.string(
                                "proposalCandidateSha256",
                            ),
                        )
                        put(
                            "controlRef",
                            control.controlRef,
                        )
                        put(
                            "humanSubjectRef",
                            humanSubjectRef,
                        )
                        put(
                            "sourceConversationHeadSha256",
                            review.string(
                                "sourceConversationHeadSha256",
                            ),
                        )
                        put(
                            "decidedAt",
                            canonicalUtc(
                                decidedAt,
                            ),
                        )
                    },
            )
        return DecisionReceipt(
            decisionRef =
                value.string(
                    "decisionRef",
                ),
            decisionSha256 =
                value.string(
                    "decisionSha256",
                ),
            decidedAt =
                decidedAt,
            value =
                value,
        )
    }

    private fun writeDecisionReceipt(
        home: Path,
        root: Path,
        decision: DecisionReceipt,
        faults: Faults,
    ) {
        writeImmutable(
            home = home,
            path =
                decisionPath(
                    root = root,
                    decisionSha256 =
                        decision.decisionSha256,
                ),
            value =
                decision.value,
            label =
                "Memory review decision",
        )
        if (
            faults.failAfterDecisionReceipt
        ) {
            error(
                "SIMULATED_MEMORY_REVIEW_FAILURE_AFTER_DECISION_RECEIPT",
            )
        }
    }

    private fun writeDecisionAndResolve(
        home: Path,
        root: Path,
        decision: DecisionReceipt,
        decidedAt: Instant,
    ) {
        writeState(
            home = home,
            root = root,
            value =
                StateFile(
                    schemaVersion =
                        StateSchema,
                    status =
                        StatusIdle,
                    reviewRefOrNull =
                        null,
                    reviewSha256OrNull =
                        null,
                    lastDecisionRefOrNull =
                        decision.decisionRef,
                    lastDecisionSha256OrNull =
                        decision.decisionSha256,
                    updatedAt =
                        canonicalUtc(
                            decidedAt,
                        ),
                ),
        )
    }

    private fun pendingReview(
        review: JsonObject,
    ): PendingReview =
        PendingReview(
            candidateRef =
                VexLifeMemoryReviewFamily.CandidateRef(
                    review.string(
                        "reviewRef",
                    ),
                ),
            reviewSha256 =
                review.string(
                    "reviewSha256",
                ),
            summary =
                review.string(
                    "summary",
                ),
            sourceConversationHeadSha256 =
                review.string(
                    "sourceConversationHeadSha256",
                ),
            proposalCandidateRef =
                review.string(
                    "proposalCandidateRef",
                ),
            proposalCandidateSha256 =
                review.string(
                    "proposalCandidateSha256",
                ),
            semanticSubjectFingerprint =
                review.string(
                    "semanticSubjectFingerprint",
                ),
            expectedSemanticAuthorityHeadSha256 =
                review.nullableString(
                    "expectedSemanticAuthorityHeadSha256",
                ),
        )

    private fun pendingState(
        review: JsonObject,
    ): VexLifeMemoryReviewFamily.Pending =
        VexLifeMemoryReviewFamily.Pending(
            candidateRef =
                VexLifeMemoryReviewFamily.CandidateRef(
                    review.string(
                        "reviewRef",
                    ),
                ),
            summary =
                review.string(
                    "summary",
                ),
            candidateClass =
                VexLifeConsiderationContract.MemoryCandidateClass.valueOf(
                    review.string(
                        "candidateClass",
                    ),
                ),
        )

    private fun currentIdentity(
        home: Path,
    ): Identity {
        val currentHome =
            requireNotNull(
                VexLifeProdHomeFiles.observeExisting(
                    home,
                ),
            ) {
                "Memory review requires one current canonical VexLife Home"
            }
        return Identity(
            lineageRef =
                currentHome.manifest.currentCompanionLineageRef,
            threadRef =
                DirectConversationIdentity.ThreadRef,
        )
    }

    private fun reviewRoot(
        home: Path,
        identity: Identity,
    ): Path =
        VexOwnedPath.resolveInside(
            home,
            "memory-review/" +
                identity.lineageRef +
                "/" +
                identity.threadRef,
        )

    private fun reviewPath(
        root: Path,
        reviewSha256: String,
    ): Path =
        root.resolve(
            "reviews/" +
                reviewSha256 +
                ".json",
        )

    private fun decisionPath(
        root: Path,
        decisionSha256: String,
    ): Path =
        root.resolve(
            "decisions/" +
                decisionSha256 +
                ".json",
        )

    private fun statePath(
        root: Path,
    ): Path =
        root.resolve(
            "state.json",
        )

    private fun readState(
        home: Path,
        root: Path,
    ): StateFile? =
        VexDurableContractFile
            .read<StateFile>(
                ownedRoot = home,
                path =
                    statePath(
                        root,
                    ),
            )
            ?.also(
                ::validateState,
            )

    private fun writeState(
        home: Path,
        root: Path,
        value: StateFile,
    ) {
        validateState(
            value,
        )
        VexDurableContractFile.write(
            ownedRoot = home,
            path =
                statePath(
                    root,
                ),
            value =
                value,
        )
    }

    private fun validateState(
        value: StateFile,
    ) {
        require(
            value.schemaVersion ==
                StateSchema &&
                value.status in
                    setOf(
                        StatusPending,
                        StatusIdle,
                    ),
        ) {
            "Memory review state contract is invalid"
        }
        when (
            value.status
        ) {
            StatusPending ->
                require(
                    value.reviewRefOrNull !=
                        null &&
                        value.reviewSha256OrNull !=
                            null,
                ) {
                    "pending Memory review state requires one exact candidate"
                }

            StatusIdle ->
                require(
                    value.reviewRefOrNull ==
                        null &&
                        value.reviewSha256OrNull ==
                            null,
                ) {
                    "idle Memory review state cannot carry one pending candidate"
                }
        }
        value.reviewSha256OrNull
            ?.let {
                requireSha256(
                    it,
                    "reviewSha256",
                )
            }
        value.lastDecisionSha256OrNull
            ?.let {
                requireSha256(
                    it,
                    "lastDecisionSha256",
                )
            }
    }

    private fun readReview(
        home: Path,
        root: Path,
        reviewRef: String,
        reviewSha256: String,
    ): JsonObject {
        requireSha256(
            reviewSha256,
            "reviewSha256",
        )
        val path =
            reviewPath(
                root = root,
                reviewSha256 =
                    reviewSha256,
            )
        val text =
            requireNotNull(
                VexDurableContractFile.readText(
                    ownedRoot = home,
                    path = path,
                ),
            ) {
                "pending Memory review candidate is missing"
            }
        val value =
            VexContractJson.Strict
                .parseToJsonElement(
                    text,
                )
                .jsonObject
        require(
            value.string(
                "schemaVersion",
            ) ==
                ReviewSchema &&
                value.string(
                    "reviewRef",
                ) ==
                    reviewRef &&
                value.string(
                    "reviewSha256",
                ) ==
                    reviewSha256 &&
                semanticHash(
                    value.without(
                        "reviewSha256",
                    ),
                ) ==
                    reviewSha256,
        ) {
            "pending Memory review candidate failed content-addressed validation"
        }
        return value
    }

    private fun writeImmutable(
        home: Path,
        path: Path,
        value: JsonObject,
        label: String,
    ) {
        VexOwnedPath.ensureParent(
            home,
            path,
        )
        val expectedText =
            VexContractJson.canonicalEncodeElement(
                value,
            ) +
                "\n"
        if (
            Files.exists(
                path,
                LinkOption.NOFOLLOW_LINKS,
            )
        ) {
            val existing =
                requireNotNull(
                    VexDurableContractFile.readText(
                        ownedRoot = home,
                        path = path,
                    ),
                )
            require(
                existing ==
                    expectedText,
            ) {
                "$label content-addressed path already contains different bytes"
            }
            return
        }
        try {
            Files.writeString(
                path,
                expectedText,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE,
            )
            VexDurableContractFile.secureFile(
                path,
            )
        } catch (
            _: FileAlreadyExistsException,
        ) {
            error(
                "$label content-addressed path raced with another writer",
            )
        }
    }

    private fun contentAddressed(
        prefix: String,
        refField: String,
        hashField: String,
        core: JsonObject,
    ): JsonObject {
        val ref =
            prefix +
                "." +
                semanticHash(
                    core,
                )
                    .take(
                        32,
                    )
        val withRef =
            core.extend {
                put(
                    refField,
                    ref,
                )
            }
        return withRef.extend {
            put(
                hashField,
                semanticHash(
                    withRef,
                ),
            )
        }
    }

    private fun statementRef(
        review: JsonObject,
    ): String =
        "statement.vexlife-prod.memory-review." +
            review.string(
                "reviewSha256",
            )
                .take(
                    32,
                )

    private fun semanticHash(
        value: JsonObject,
    ): String =
        sha256(
            VexContractJson.canonicalEncodeElement(
                value,
            ),
        )

    private fun sha256(
        value: String,
    ): String =
        MessageDigest
            .getInstance(
                "SHA-256",
            )
            .digest(
                value.toByteArray(
                    StandardCharsets.UTF_8,
                ),
            )
            .joinToString(
                "",
            ) {
                "%02x".format(
                    it.toInt() and
                        0xff,
                )
            }

    private fun canonicalUtc(
        instant: Instant,
    ): String =
        DateTimeFormatterBuilder()
            .appendInstant(
                3,
            )
            .toFormatter()
            .format(
                instant,
            )

    private fun requireSafeRef(
        value: String,
        label: String,
    ) {
        require(
            value.matches(
                Regex(
                    "[a-z0-9](?:[a-z0-9._-]{0,190}[a-z0-9])?",
                ),
            ),
        ) {
            "$label must be one lowercase portable ref"
        }
    }

    private fun requireSha256(
        value: String,
        label: String,
    ) {
        require(
            value.matches(
                Regex(
                    "[0-9a-f]{64}",
                ),
            ),
        ) {
            "$label must be one lowercase SHA-256"
        }
    }

    private fun JsonObject.string(
        key: String,
    ): String =
        requireNotNull(
            this[
                key
            ],
        ) {
            "$key is missing"
        }
            .jsonPrimitive
            .content

    private fun JsonObject.nullableString(
        key: String,
    ): String? =
        this[
            key
        ]
            ?.let {
                if (
                    it is JsonNull
                ) {
                    null
                } else {
                    it.jsonPrimitive.content
                }
            }

    private fun JsonObject.stringList(
        key: String,
    ): List<String> =
        requireNotNull(
            this[
                key
            ],
        ) {
            "$key is missing"
        }
            .jsonArray
            .map {
                it.jsonPrimitive.content
            }

    private fun JsonObject.objectList(
        key: String,
    ): List<JsonObject> =
        requireNotNull(
            this[
                key
            ],
        ) {
            "$key is missing"
        }
            .jsonArray
            .map {
                it.jsonObject
            }

    private fun JsonObject.extend(
        build: JsonObjectBuilder.() -> Unit,
    ): JsonObject =
        buildJsonObject {
            for (
                (
                    key,
                    value,
                ) in
                this@extend
            ) {
                put(
                    key,
                    value,
                )
            }
            build()
        }

    private fun JsonObject.without(
        key: String,
    ): JsonObject =
        JsonObject(
            filterKeys {
                it !=
                    key
            },
        )

    private fun JsonObjectBuilder.putNullable(
        key: String,
        value: String?,
    ) {
        put(
            key,
            value
                ?.let(
                    ::JsonPrimitive,
                )
                ?: JsonNull,
        )
    }
}

// [VXG RealForever]
