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
import vexlife.contract.VexContractJson
import vexlife.contract.VexDurableContractFile
import vexlife.conversation.DirectConversationIdentity
import vexlife.conversation.DirectConversationStore
import vexlife.filesystem.VexExclusiveFileLease
import vexlife.filesystem.VexOwnedPath

/**
 * Product-side materializer for one explicitly human-authorized semantic Memory acceptance.
 *
 * This owner does not decide whether a proposal is meaningful and is intentionally not wired to the
 * model/provider path. It receives typed classification meaning plus an exact external human-decision
 * source, re-forms the Score candidate against the then-current Conversation while preserving exact
 * requested source hashes, materializes the already-accepted Memory/Safety contract, advances the
 * semantic-authority head last, and uses [VexLifeCurrentMemoryReader] as the post-write oracle.
 *
 * It does not append Score, run Daily Dream, mutate a model, or grant standing consent.
 */
internal object VexLifeSemanticAcceptanceWriter {
    private const val ClassificationSchema =
        "vextreme.score-classification-evidence/v1"
    private const val ConsentSchema =
        "vextreme.score-consent-disposition/v1"
    private const val AcceptanceSchema =
        "vextreme.score-semantic-acceptance/v1"
    private const val AuthorityHeadSchema =
        "vextreme.score-semantic-authority-head/v1"
    private const val AuthorityBindingSchema =
        "vextreme.score-consent-authority-binding/v1"

    private const val LiveSemanticContract =
        "contract.multivex.score.live-semantic-acceptance.v1"
    private const val LiveSemanticDisposition =
        "github.issue.vextreme-sdk.350.comment.5216924433"
    private const val LiveSemanticExecutableAddendum =
        "github.issue.vextreme-sdk.350.comment.5217097749"
    private const val MemoryOwnerProjectRef =
        "project.multivex.memory"
    private const val SafetyOwnerProjectRef =
        "project.multivex.safety"
    private const val MemoryDispositionRef =
        "github.issue.vextreme-sdk.225.comment.5217085830"
    private const val SafetyDispositionRef =
        "github.issue.vextreme-sdk.226.comment.5217090896"
    private const val MemoryClassifierIssuerRef =
        "owner.multivex.memory.score-semantic-classifier.001"
    private const val MemoryClassifierIssuerClass =
        "SOURCE_MANAGED_MEMORY_CLASSIFIER"
    private const val SafetyIssuerRef =
        "owner.multivex.safety.score-consent-authority.001"
    private const val SafetyIssuerClass =
        "SOURCE_MANAGED_SAFETY_CONSENT"
    private const val SharedIssuerRef =
        "owner.multivex.score-semantic-authority.materializer.001"
    private const val SharedIssuerClass =
        "SOURCE_MANAGED_MEMORY_SAFETY_COMPOSITION"

    private const val LiveAcceptancePurposeRef =
        "purpose.score.live-semantic-acceptance"
    private const val ContinuityPurposeRef =
        "purpose.score.device-private-continuity"
    private const val ContinuityUseRef =
        "use.score.device-private-continuity"
    private const val FirstPersonUseRef =
        "use.score.first-person"
    private const val DevicePrivateRetentionRef =
        "retention.score.device-private"
    private const val NoRedisclosureRef =
        "redisclosure.score.not-admitted"
    private const val NoFirstPersonRef =
        "boundary.score.first-person.not-admitted"
    private const val PrivacyClass =
        "DEVICE_PRIVATE"

    enum class MemoryRelation(
        internal val wireValue: String,
    ) {
        CURRENT_LINEAGE_AUTOBIOGRAPHY(
            "CURRENT_LINEAGE_AUTOBIOGRAPHY",
        ),
        SHARED_RELATIONSHIP_HISTORY(
            "SHARED_RELATIONSHIP_HISTORY",
        ),
        PREDECESSOR_WITNESS_HISTORY(
            "PREDECESSOR_WITNESS_HISTORY",
        ),
        INHERITED_CONTEXT(
            "INHERITED_CONTEXT",
        ),
        EXTERNAL_EVIDENCE(
            "EXTERNAL_EVIDENCE",
        ),
        DISPUTED_OR_UNRESOLVED(
            "DISPUTED_OR_UNRESOLVED",
        ),
    }

    enum class InitialStatementState(
        internal val wireValue: String,
    ) {
        OBSERVED(
            "OBSERVED",
        ),
        HUMAN_CONFIRMED(
            "HUMAN_CONFIRMED",
        ),
        INFERRED(
            "INFERRED",
        ),
        CONFLICTED(
            "CONFLICTED",
        ),
        UNKNOWN(
            "UNKNOWN",
        ),
    }

    private enum class EvidenceClass(
        val wireValue: String,
    ) {
        STRUCTURAL_OBSERVATION(
            "STRUCTURAL_OBSERVATION",
        ),
        HUMAN_CONFIRMATION(
            "HUMAN_CONFIRMATION",
        ),
        LINEAGE_INFERENCE(
            "LINEAGE_INFERENCE",
        ),
        CONFLICT_PRESERVATION(
            "CONFLICT_PRESERVATION",
        ),
        UNKNOWN_HOLD(
            "UNKNOWN_HOLD",
        ),
        CORRECTION_ACCEPTANCE(
            "CORRECTION_ACCEPTANCE",
        ),
        SUPERSESSION_ACCEPTANCE(
            "SUPERSESSION_ACCEPTANCE",
        ),
    }

    private enum class AcceptedStatementState(
        val wireValue: String,
    ) {
        CORRECTED(
            "CORRECTED",
        ),
        SUPERSEDED(
            "SUPERSEDED",
        ),
    }

    private enum class TransitionKind(
        val wireValue: String,
    ) {
        NONE(
            "NONE",
        ),
        CORRECTS(
            "CORRECTS",
        ),
        SUPERSEDES(
            "SUPERSEDES",
        ),
    }

    private enum class ConsentDisposition(
        val wireValue: String,
    ) {
        PERMITTED(
            "PERMITTED",
        ),
    }

    enum class ProposerClass(
        internal val wireValue: String,
    ) {
        LINEAGE(
            "LINEAGE",
        ),
        HUMAN(
            "HUMAN",
        ),
        SYSTEM_CLASSIFIER(
            "SYSTEM_CLASSIFIER",
        ),
    }

    data class Proposal(
        val semanticSubjectRef: String,
        val proposedSummary: String,
        val memoryRelation: MemoryRelation,
        val proposedStatementState: InitialStatementState,
        val proposerRef: String,
        val proposerClass: ProposerClass,
        val sourceEventHashes: List<String>,
        val subjectScopeRef: String =
            "scope.score.thread",
    )

    sealed interface Classification {
        val acceptedSummary: String
        val memoryRelation: MemoryRelation

        data class Initial(
            override val acceptedSummary: String,
            override val memoryRelation: MemoryRelation,
            val statementState: InitialStatementState,
        ) : Classification

        data class Corrects(
            override val acceptedSummary: String,
            override val memoryRelation: MemoryRelation,
            val targetStatementRef: String,
            val targetAcceptanceSha256: String,
        ) : Classification

        data class Supersedes(
            override val acceptedSummary: String,
            override val memoryRelation: MemoryRelation,
            val targetStatementRef: String,
            val targetAcceptanceSha256: String,
        ) : Classification
    }

    data class HumanDecisionSource(
        val humanSubjectRef: String,
        val decisionRef: String,
        val decisionSha256: String,
        val decidedAt: Instant,
    )

    data class KeepInvocation(
        val instanceRef: String,
        val proposal: Proposal,
        val classification: Classification,
        val humanDecision: HumanDecisionSource,
        val expectedSemanticAuthorityHeadSha256: String?,
        val formedAt: Instant,
    )

    data class Result(
        val candidateRef: String,
        val candidateSha256: String,
        val acceptanceRef: String,
        val acceptanceSha256: String,
        val consentDispositionRef: String,
        val consentDispositionSha256: String,
        val semanticAuthorityHeadRef: String,
        val semanticAuthorityHeadSha256: String,
        val sequence: Long,
        val priorSemanticAuthorityHeadSha256: String?,
        val reusedImmutableObjectCount: Int,
        val newImmutableObjectCount: Int,
        val semanticAuthorityDomainWritePerformed: Boolean = true,
        val vexLifeScoreWritePerformed: Boolean = false,
        val dreamEffectPerformed: Boolean = false,
        val trainingEffectPerformed: Boolean = false,
        val modelMutationPerformed: Boolean = false,
        val publicationPerformed: Boolean = false,
    )

    internal data class Faults(
        val failBeforeHeadAdvance: Boolean =
            false,
    )

    private data class ClassificationShape(
        val evidenceClass: EvidenceClass,
        val statementState: String,
        val transitionKind: TransitionKind,
        val transitionTargetRef: String?,
        val transitionTargetAcceptanceSha256: String?,
    )

    private data class WriteCount(
        val reused: Int,
        val created: Int,
    ) {
        operator fun plus(
            other: WriteCount,
        ): WriteCount =
            WriteCount(
                reused = reused + other.reused,
                created = created + other.created,
            )
    }

    fun keep(
        productHome: Path,
        invocation: KeepInvocation,
        faults: Faults =
            Faults(),
    ): Result {
        validateInvocation(
            invocation,
        )
        val home =
            VexOwnedPath.requireRoot(
                productHome,
                "canonical VexLife Home",
            )
        val conversationStore =
            DirectConversationStore(
                home,
            )

        return conversationStore.withWriterLease(
            instanceRef =
                invocation.instanceRef,
        ) {
            val expectedConversationHeadSha256 =
                requireNotNull(
                    current().head,
                ) {
                    "semantic acceptance requires one current completed Conversation head"
                }
                    .conversationHeadSha256
            keepWithConversationLease(
                productHome = home,
                conversation = this,
                expectedConversationHeadSha256 =
                    expectedConversationHeadSha256,
                invocation = invocation,
                faults = faults,
            )
        }
    }

    /**
     * Materialize one semantic acceptance while the caller already owns the exact Conversation
     * writer lease. This exists so a higher Product owner can keep Conversation current across a
     * multi-owner effect such as semantic acceptance -> Score append.
     *
     * The caller must pass the [DirectConversationStore.Locked] receiver obtained from the same
     * canonical Home. This method never acquires or releases that lease itself.
     */
    internal fun keepWithConversationLease(
        productHome: Path,
        conversation: DirectConversationStore.Locked,
        expectedConversationHeadSha256: String,
        invocation: KeepInvocation,
        faults: Faults =
            Faults(),
    ): Result {
        validateInvocation(
            invocation,
        )
        requireSha256(
            expectedConversationHeadSha256,
            "expectedConversationHeadSha256",
        )
        val home =
            VexOwnedPath.requireRoot(
                productHome,
                "canonical VexLife Home",
            )
        val before =
            requireNotNull(
                conversation.current().head,
            ) {
                "semantic acceptance requires one current completed Conversation head"
            }
        require(
            before.conversationHeadSha256 ==
                expectedConversationHeadSha256,
        ) {
            "semantic acceptance expected Conversation head is not current"
        }
        val candidate =
            VexLifeScoreContinuity.formCandidate(
                productHome = home,
                input =
                    VexLifeScoreContinuity.CandidateInput(
                        semanticSubjectRef =
                            invocation.proposal.semanticSubjectRef,
                        proposedSummary =
                            invocation.proposal.proposedSummary,
                        proposedMemoryRelation =
                            invocation.proposal.memoryRelation.wireValue,
                        proposedStatementState =
                            invocation.proposal.proposedStatementState.wireValue,
                        proposerRef =
                            invocation.proposal.proposerRef,
                        proposerClass =
                            invocation.proposal.proposerClass.wireValue,
                        formedAt =
                            invocation.formedAt,
                        subjectScopeRef =
                            invocation.proposal.subjectScopeRef,
                        sourceEventHashes =
                            invocation.proposal.sourceEventHashes,
                    ),
            )
        require(
            candidate.sourceConversationHeadSha256 ==
                expectedConversationHeadSha256 &&
                conversation.current().head?.conversationHeadSha256 ==
                    expectedConversationHeadSha256,
        ) {
            "semantic acceptance candidate is not bound to the exact expected Conversation head"
        }

        return materialize(
            home = home,
            candidate = candidate,
            invocation = invocation,
            faults = faults,
        )
    }

    private fun materialize(
        home: Path,
        candidate: VexLifeScoreContinuity.CandidateResult,
        invocation: KeepInvocation,
        faults: Faults,
    ): Result {
        val candidateValue =
            candidate.candidate
        val lineageRef =
            candidateValue.string(
                "sourceLineageRef",
            )
        val threadRef =
            candidateValue.string(
                "sourceThreadRef",
            )
        require(
            threadRef ==
                DirectConversationIdentity.ThreadRef,
        ) {
            "semantic acceptance candidate thread differs from current Product Conversation"
        }
        val root =
            VexOwnedPath.resolveInside(
                home,
                "semantic-authority/score/" +
                    lineageRef +
                    "/" +
                    threadRef,
            )
        VexOwnedPath.ensureDirectory(
            home,
            root,
        )
        val lockPath =
            root.resolve(
                ".materialize.lock",
            )

        return VexExclusiveFileLease
            .acquire(
                lockPath,
            )
            .use {
                val currentAuthority =
                    VexLifeCurrentMemoryReader
                        .readCurrentSemanticAuthorityHead(
                            productHome = home,
                            threadRef = threadRef,
                        )
                val observedCurrentSha =
                    currentAuthority
                        ?.string(
                            "semanticAuthorityHeadSha256",
                        )
                require(
                    observedCurrentSha ==
                        invocation.expectedSemanticAuthorityHeadSha256,
                ) {
                    "SEMANTIC_AUTHORITY_STALE: current semantic authority head differs from expected"
                }

                val classificationShape =
                    classificationShape(
                        classification =
                            invocation.classification,
                        currentAuthority =
                            currentAuthority,
                        home = home,
                        candidate = candidateValue,
                    )
                val sourceEvidence =
                    sourceEvidence(
                        candidate = candidateValue,
                        humanDecision =
                            invocation.humanDecision,
                    )
                val assertedSummarySha256 =
                    semanticHashString(
                        invocation.classification.acceptedSummary,
                    )
                val classificationEvidence =
                    formClassificationEvidence(
                        candidate = candidateValue,
                        classification =
                            invocation.classification,
                        shape = classificationShape,
                        assertedSummarySha256 =
                            assertedSummarySha256,
                        sourceEvidence =
                            sourceEvidence,
                        humanDecision =
                            invocation.humanDecision,
                        formedAt =
                            invocation.formedAt,
                    )
                val consent =
                    formConsent(
                        candidate = candidateValue,
                        sourceEvidence =
                            sourceEvidence,
                        humanDecision =
                            invocation.humanDecision,
                        formedAt =
                            invocation.formedAt,
                    )
                val acceptance =
                    formAcceptance(
                        candidate = candidateValue,
                        classification =
                            invocation.classification,
                        shape = classificationShape,
                        classificationEvidence =
                            classificationEvidence,
                        consent = consent,
                        acceptedSummarySha256 =
                            assertedSummarySha256,
                        formedAt =
                            invocation.formedAt,
                    )
                val binding =
                    acceptanceBinding(
                        candidate = candidateValue,
                        classificationEvidence =
                            classificationEvidence,
                        consent = consent,
                        acceptance = acceptance,
                    )

                val previousBindings =
                    currentAuthority
                        ?.objectList(
                            "currentAcceptanceBindings",
                        )
                        .orEmpty()
                val fingerprint =
                    candidateValue.string(
                        "semanticSubjectFingerprint",
                    )
                val currentBindings =
                    (
                        previousBindings.filter {
                            it.string(
                                "semanticSubjectFingerprint",
                            ) !=
                                fingerprint
                        } +
                            binding
                    )
                        .sortedBy {
                            it.string(
                                "semanticSubjectFingerprint",
                            )
                        }

                val headCore =
                    buildJsonObject {
                        put(
                            "schemaVersion",
                            AuthorityHeadSchema,
                        )
                        put(
                            "sourceLineageRef",
                            lineageRef,
                        )
                        put(
                            "sourceThreadRef",
                            threadRef,
                        )
                        put(
                            "sequence",
                            currentAuthority
                                ?.long(
                                    "sequence",
                                )
                                ?.plus(
                                    1,
                                )
                                ?: 0L,
                        )
                        putNullable(
                            "priorSemanticAuthorityHeadSha256",
                            observedCurrentSha,
                        )
                        put(
                            "currentAcceptanceBindings",
                            JsonArray(
                                currentBindings,
                            ),
                        )
                        put(
                            "formedAt",
                            canonicalUtc(
                                invocation.formedAt,
                            ),
                        )
                        put(
                            "contractRef",
                            LiveSemanticContract,
                        )
                        put(
                            "semanticExecutableAddendumRef",
                            LiveSemanticExecutableAddendum,
                        )
                        putStrings(
                            "ownerRefs",
                            listOf(
                                MemoryOwnerProjectRef,
                                SafetyOwnerProjectRef,
                            ),
                        )
                    }
                val head =
                    contentAddressed(
                        prefix =
                            "score-semantic-authority-head",
                        refField =
                            "semanticAuthorityHeadRef",
                        hashField =
                            "semanticAuthorityHeadSha256",
                        core =
                            headCore,
                    )

                var writes =
                    WriteCount(
                        reused = 0,
                        created = 0,
                    )
                writes +=
                    writeImmutable(
                        home = home,
                        path =
                            root.resolve(
                                "candidates/" +
                                    candidate.candidateSha256 +
                                    ".json",
                            ),
                        value =
                            candidateValue,
                        label =
                            "semantic candidate",
                    )
                writes +=
                    writeImmutable(
                        home = home,
                        path =
                            root.resolve(
                                "classification-evidence/" +
                                    classificationEvidence.string(
                                        "classificationEvidenceSha256",
                                    ) +
                                    ".json",
                            ),
                        value =
                            classificationEvidence,
                        label =
                            "classification evidence",
                    )
                writes +=
                    writeImmutable(
                        home = home,
                        path =
                            root.resolve(
                                "consents/" +
                                    consent.string(
                                        "consentDispositionSha256",
                                    ) +
                                    ".json",
                            ),
                        value =
                            consent,
                        label =
                            "consent disposition",
                    )
                writes +=
                    writeImmutable(
                        home = home,
                        path =
                            root.resolve(
                                "acceptances/" +
                                    acceptance.string(
                                        "acceptanceSha256",
                                    ) +
                                    ".json",
                            ),
                        value =
                            acceptance,
                        label =
                            "semantic acceptance",
                    )
                writes +=
                    writeImmutable(
                        home = home,
                        path =
                            root.resolve(
                                "heads/" +
                                    head.string(
                                        "semanticAuthorityHeadSha256",
                                    ) +
                                    ".json",
                            ),
                        value =
                            head,
                        label =
                            "semantic authority head",
                    )

                if (
                    faults.failBeforeHeadAdvance
                ) {
                    error(
                        "SEMANTIC_AUTHORITY_HEAD_ADVANCE_SIMULATED_FAILURE",
                    )
                }

                VexDurableContractFile.write(
                    ownedRoot = home,
                    path =
                        root.resolve(
                            "head.json",
                        ),
                    value =
                        head,
                )

                val roundTrip =
                    VexLifeCurrentMemoryReader
                        .readCurrentScoreAcceptance(
                            productHome = home,
                            acceptanceRef =
                                acceptance.string(
                                    "acceptanceRef",
                                ),
                            acceptanceSha256 =
                                acceptance.string(
                                    "acceptanceSha256",
                                ),
                            threadRef =
                                threadRef,
                        )
                require(
                    roundTrip.semanticAuthorityHeadSha256 ==
                        head.string(
                            "semanticAuthorityHeadSha256",
                        ) &&
                        roundTrip.candidate ==
                        candidateValue &&
                        roundTrip.classificationEvidence ==
                        listOf(
                            classificationEvidence,
                        ) &&
                        roundTrip.consent ==
                        consent &&
                        roundTrip.acceptance ==
                        acceptance,
                ) {
                    "semantic acceptance reader round-trip differs from written authority generation"
                }

                Result(
                    candidateRef =
                        candidate.candidateRef,
                    candidateSha256 =
                        candidate.candidateSha256,
                    acceptanceRef =
                        acceptance.string(
                            "acceptanceRef",
                        ),
                    acceptanceSha256 =
                        acceptance.string(
                            "acceptanceSha256",
                        ),
                    consentDispositionRef =
                        consent.string(
                            "consentDispositionRef",
                        ),
                    consentDispositionSha256 =
                        consent.string(
                            "consentDispositionSha256",
                        ),
                    semanticAuthorityHeadRef =
                        head.string(
                            "semanticAuthorityHeadRef",
                        ),
                    semanticAuthorityHeadSha256 =
                        head.string(
                            "semanticAuthorityHeadSha256",
                        ),
                    sequence =
                        head.long(
                            "sequence",
                        ),
                    priorSemanticAuthorityHeadSha256 =
                        observedCurrentSha,
                    reusedImmutableObjectCount =
                        writes.reused,
                    newImmutableObjectCount =
                        writes.created,
                )
            }
    }

    private fun classificationShape(
        classification: Classification,
        currentAuthority: JsonObject?,
        home: Path,
        candidate: JsonObject,
    ): ClassificationShape {
        val fingerprint =
            candidate.string(
                "semanticSubjectFingerprint",
            )
        val predecessorBinding =
            currentAuthority
                ?.objectList(
                    "currentAcceptanceBindings",
                )
                ?.singleOrNull {
                    it.string(
                        "semanticSubjectFingerprint",
                    ) ==
                        fingerprint
                }

        return when (
            classification
        ) {
            is Classification.Initial -> {
                require(
                    predecessorBinding ==
                        null,
                ) {
                    "SEMANTIC_AUTHORITY_TRANSITION_REQUIRED: existing current semantic subject requires correction or supersession"
                }
                ClassificationShape(
                    evidenceClass =
                        evidenceClassFor(
                            classification.statementState,
                        ),
                    statementState =
                        classification.statementState.wireValue,
                    transitionKind =
                        TransitionKind.NONE,
                    transitionTargetRef =
                        null,
                    transitionTargetAcceptanceSha256 =
                        null,
                )
            }

            is Classification.Corrects ->
                transitionShape(
                    home = home,
                    candidate = candidate,
                    predecessorBinding =
                        predecessorBinding,
                    memoryRelation =
                        classification.memoryRelation,
                    targetStatementRef =
                        classification.targetStatementRef,
                    targetAcceptanceSha256 =
                        classification.targetAcceptanceSha256,
                    transitionKind =
                        TransitionKind.CORRECTS,
                    statementState =
                        AcceptedStatementState.CORRECTED,
                    evidenceClass =
                        EvidenceClass.CORRECTION_ACCEPTANCE,
                )

            is Classification.Supersedes ->
                transitionShape(
                    home = home,
                    candidate = candidate,
                    predecessorBinding =
                        predecessorBinding,
                    memoryRelation =
                        classification.memoryRelation,
                    targetStatementRef =
                        classification.targetStatementRef,
                    targetAcceptanceSha256 =
                        classification.targetAcceptanceSha256,
                    transitionKind =
                        TransitionKind.SUPERSEDES,
                    statementState =
                        AcceptedStatementState.SUPERSEDED,
                    evidenceClass =
                        EvidenceClass.SUPERSESSION_ACCEPTANCE,
                )
        }
    }

    private fun evidenceClassFor(
        state: InitialStatementState,
    ): EvidenceClass =
        when (
            state
        ) {
            InitialStatementState.OBSERVED ->
                EvidenceClass.STRUCTURAL_OBSERVATION

            InitialStatementState.HUMAN_CONFIRMED ->
                EvidenceClass.HUMAN_CONFIRMATION

            InitialStatementState.INFERRED ->
                EvidenceClass.LINEAGE_INFERENCE

            InitialStatementState.CONFLICTED ->
                EvidenceClass.CONFLICT_PRESERVATION

            InitialStatementState.UNKNOWN ->
                EvidenceClass.UNKNOWN_HOLD
        }

    private fun transitionShape(
        home: Path,
        candidate: JsonObject,
        predecessorBinding: JsonObject?,
        memoryRelation: MemoryRelation,
        targetStatementRef: String,
        targetAcceptanceSha256: String,
        transitionKind: TransitionKind,
        statementState: AcceptedStatementState,
        evidenceClass: EvidenceClass,
    ): ClassificationShape {
        val predecessor =
            requireNotNull(
                predecessorBinding,
            ) {
                "SEMANTIC_AUTHORITY_TRANSITION_TARGET_INVALID: no current same-subject predecessor"
            }
        require(
            predecessor.string(
                "acceptanceSha256",
            ) ==
                targetAcceptanceSha256,
        ) {
            "SEMANTIC_AUTHORITY_TRANSITION_TARGET_INVALID: target acceptance is not the current same-subject predecessor"
        }
        val scoreBinding =
            requireNotNull(
                VexLifeCurrentMemoryReader
                    .readCurrentScoreStatementBinding(
                        productHome = home,
                        statementRef =
                            targetStatementRef,
                    ),
            ) {
                "SEMANTIC_AUTHORITY_TRANSITION_TARGET_INVALID: target Score statement is not current"
            }
        require(
            scoreBinding.semanticSubjectFingerprint ==
                candidate.string(
                    "semanticSubjectFingerprint",
                ) &&
                scoreBinding.semanticAcceptanceSha256 ==
                targetAcceptanceSha256 &&
                scoreBinding.semanticAcceptanceRef ==
                predecessor.string(
                    "acceptanceRef",
                ) &&
                scoreBinding.memoryRelation ==
                memoryRelation.wireValue,
        ) {
            "SEMANTIC_AUTHORITY_TRANSITION_TARGET_INVALID: Score predecessor differs from semantic authority"
        }

        return ClassificationShape(
            evidenceClass =
                evidenceClass,
            statementState =
                statementState.wireValue,
            transitionKind =
                transitionKind,
            transitionTargetRef =
                targetStatementRef,
            transitionTargetAcceptanceSha256 =
                targetAcceptanceSha256,
        )
    }

    private fun formClassificationEvidence(
        candidate: JsonObject,
        classification: Classification,
        shape: ClassificationShape,
        assertedSummarySha256: String,
        sourceEvidence: List<JsonObject>,
        humanDecision: HumanDecisionSource,
        formedAt: Instant,
    ): JsonObject {
        val core =
            buildJsonObject {
                put(
                    "schemaVersion",
                    ClassificationSchema,
                )
                put(
                    "candidateRef",
                    candidate.string(
                        "candidateRef",
                    ),
                )
                put(
                    "candidateSha256",
                    candidate.string(
                        "candidateSha256",
                    ),
                )
                put(
                    "semanticSubjectRef",
                    candidate.string(
                        "semanticSubjectRef",
                    ),
                )
                put(
                    "semanticSubjectFingerprint",
                    candidate.string(
                        "semanticSubjectFingerprint",
                    ),
                )
                put(
                    "evidenceClass",
                    shape.evidenceClass.wireValue,
                )
                put(
                    "assertedMemoryRelation",
                    classification.memoryRelation.wireValue,
                )
                put(
                    "assertedStatementState",
                    shape.statementState,
                )
                put(
                    "assertedSummarySha256",
                    assertedSummarySha256,
                )
                put(
                    "transitionKind",
                    shape.transitionKind.wireValue,
                )
                putNullable(
                    "transitionTargetRef",
                    shape.transitionTargetRef,
                )
                putNullable(
                    "transitionTargetAcceptanceSha256",
                    shape.transitionTargetAcceptanceSha256,
                )
                put(
                    "issuerRef",
                    MemoryClassifierIssuerRef,
                )
                put(
                    "issuerClass",
                    MemoryClassifierIssuerClass,
                )
                put(
                    "ownerProjectRef",
                    MemoryOwnerProjectRef,
                )
                put(
                    "ownerDispositionRef",
                    MemoryDispositionRef,
                )
                put(
                    "sourceEvidenceBindings",
                    JsonArray(
                        sourceEvidence,
                    ),
                )
                put(
                    "purposeRef",
                    LiveAcceptancePurposeRef,
                )
                put(
                    "formedAt",
                    canonicalUtc(
                        formedAt,
                    ),
                )
                put(
                    "privacyClass",
                    PrivacyClass,
                )
                if (
                    shape.evidenceClass ==
                        EvidenceClass.HUMAN_CONFIRMATION
                ) {
                    put(
                        "humanConfirmation",
                        buildJsonObject {
                            put(
                                "humanSubjectRef",
                                humanDecision.humanSubjectRef,
                            )
                            put(
                                "confirmationDispositionRef",
                                humanDecision.decisionRef,
                            )
                            put(
                                "confirmationDispositionSha256",
                                humanDecision.decisionSha256,
                            )
                            put(
                                "confirmedCandidateRef",
                                candidate.string(
                                    "candidateRef",
                                ),
                            )
                            put(
                                "confirmedCandidateSha256",
                                candidate.string(
                                    "candidateSha256",
                                ),
                            )
                            put(
                                "confirmedSemanticSubjectFingerprint",
                                candidate.string(
                                    "semanticSubjectFingerprint",
                                ),
                            )
                            put(
                                "confirmedSummarySha256",
                                assertedSummarySha256,
                            )
                            put(
                                "confirmedAt",
                                canonicalUtc(
                                    humanDecision.decidedAt,
                                ),
                            )
                        },
                    )
                }
            }

        return contentAddressed(
            prefix =
                "score-classification-evidence",
            refField =
                "classificationEvidenceRef",
            hashField =
                "classificationEvidenceSha256",
            core =
                core,
        )
    }

    private fun formConsent(
        candidate: JsonObject,
        sourceEvidence: List<JsonObject>,
        humanDecision: HumanDecisionSource,
        formedAt: Instant,
    ): JsonObject {
        val implicated =
            listOf(
                humanDecision.humanSubjectRef,
            )
        val permitted =
            listOf(
                ContinuityUseRef,
            )
        val prohibited =
            listOf(
                FirstPersonUseRef,
            )
        val scopeCore =
            buildJsonObject {
                put(
                    "schemaVersion",
                    "vextreme.score-consent-authority-scope/v1",
                )
                put(
                    "candidateRef",
                    candidate.string(
                        "candidateRef",
                    ),
                )
                put(
                    "candidateSha256",
                    candidate.string(
                        "candidateSha256",
                    ),
                )
                put(
                    "semanticSubjectRef",
                    candidate.string(
                        "semanticSubjectRef",
                    ),
                )
                put(
                    "semanticSubjectFingerprint",
                    candidate.string(
                        "semanticSubjectFingerprint",
                    ),
                )
                put(
                    "purposeRef",
                    ContinuityPurposeRef,
                )
                put(
                    "privacyClass",
                    PrivacyClass,
                )
                putStrings(
                    "implicatedSubjectRefs",
                    implicated,
                )
                putStrings(
                    "permittedUseRefs",
                    permitted,
                )
                putStrings(
                    "prohibitedUseRefs",
                    prohibited,
                )
                put(
                    "retentionBoundaryRef",
                    DevicePrivateRetentionRef,
                )
                put(
                    "redisclosureBoundaryRef",
                    NoRedisclosureRef,
                )
                put(
                    "firstPersonBoundaryRef",
                    NoFirstPersonRef,
                )
            }
        val scopeFingerprint =
            semanticHash(
                scopeCore,
            )
        val authorityBinding =
            humanAuthorityBinding(
                source =
                    humanDecision,
                scopeFingerprint =
                    scopeFingerprint,
            )

        val core =
            buildJsonObject {
                put(
                    "schemaVersion",
                    ConsentSchema,
                )
                put(
                    "candidateRef",
                    candidate.string(
                        "candidateRef",
                    ),
                )
                put(
                    "candidateSha256",
                    candidate.string(
                        "candidateSha256",
                    ),
                )
                put(
                    "semanticSubjectRef",
                    candidate.string(
                        "semanticSubjectRef",
                    ),
                )
                put(
                    "semanticSubjectFingerprint",
                    candidate.string(
                        "semanticSubjectFingerprint",
                    ),
                )
                put(
                    "purposeRef",
                    ContinuityPurposeRef,
                )
                put(
                    "privacyClass",
                    PrivacyClass,
                )
                putStrings(
                    "implicatedSubjectRefs",
                    implicated,
                )
                put(
                    "requiredAuthorityBindings",
                    JsonArray(
                        listOf(
                            authorityBinding,
                        ),
                    ),
                )
                put(
                    "observedAuthorityBindings",
                    JsonArray(
                        listOf(
                            authorityBinding,
                        ),
                    ),
                )
                put(
                    "disposition",
                    ConsentDisposition.PERMITTED.wireValue,
                )
                putStrings(
                    "permittedUseRefs",
                    permitted,
                )
                putStrings(
                    "prohibitedUseRefs",
                    prohibited,
                )
                put(
                    "retentionBoundaryRef",
                    DevicePrivateRetentionRef,
                )
                put(
                    "redisclosureBoundaryRef",
                    NoRedisclosureRef,
                )
                put(
                    "firstPersonBoundaryRef",
                    NoFirstPersonRef,
                )
                put(
                    "formedAt",
                    canonicalUtc(
                        formedAt,
                    ),
                )
                put(
                    "expiresAt",
                    JsonNull,
                )
                put(
                    "issuerRef",
                    SafetyIssuerRef,
                )
                put(
                    "issuerClass",
                    SafetyIssuerClass,
                )
                put(
                    "ownerProjectRef",
                    SafetyOwnerProjectRef,
                )
                put(
                    "ownerDispositionRef",
                    SafetyDispositionRef,
                )
                put(
                    "sourceEvidenceBindings",
                    JsonArray(
                        sourceEvidence,
                    ),
                )
            }

        return contentAddressed(
            prefix =
                "score-consent-disposition",
            refField =
                "consentDispositionRef",
            hashField =
                "consentDispositionSha256",
            core =
                core,
        )
    }

    private fun humanAuthorityBinding(
        source: HumanDecisionSource,
        scopeFingerprint: String,
    ): JsonObject {
        val formedAt =
            canonicalUtc(
                source.decidedAt,
            )
        val hashCore =
            buildJsonObject {
                put(
                    "schemaVersion",
                    AuthorityBindingSchema,
                )
                put(
                    "authorityRef",
                    source.decisionRef,
                )
                put(
                    "subjectRef",
                    source.humanSubjectRef,
                )
                put(
                    "purposeRef",
                    ContinuityPurposeRef,
                )
                put(
                    "scopeFingerprint",
                    scopeFingerprint,
                )
                put(
                    "disposition",
                    ConsentDisposition.PERMITTED.wireValue,
                )
                put(
                    "formedAt",
                    formedAt,
                )
                put(
                    "expiresAt",
                    JsonNull,
                )
            }
        val authoritySha256 =
            semanticHash(
                hashCore,
            )
        return buildJsonObject {
            put(
                "authorityRef",
                source.decisionRef,
            )
            put(
                "subjectRef",
                source.humanSubjectRef,
            )
            put(
                "purposeRef",
                ContinuityPurposeRef,
            )
            put(
                "scopeFingerprint",
                scopeFingerprint,
            )
            put(
                "disposition",
                ConsentDisposition.PERMITTED.wireValue,
            )
            put(
                "formedAt",
                formedAt,
            )
            put(
                "expiresAt",
                JsonNull,
            )
            put(
                "authoritySha256",
                authoritySha256,
            )
        }
    }

    private fun formAcceptance(
        candidate: JsonObject,
        classification: Classification,
        shape: ClassificationShape,
        classificationEvidence: JsonObject,
        consent: JsonObject,
        acceptedSummarySha256: String,
        formedAt: Instant,
    ): JsonObject {
        val sourceBindingHashes =
            candidate
                .objectList(
                    "sourceBindings",
                )
                .map {
                    it.string(
                        "eventHash",
                    )
                }
        val evidenceBinding =
            buildJsonObject {
                put(
                    "classificationEvidenceRef",
                    classificationEvidence.string(
                        "classificationEvidenceRef",
                    ),
                )
                put(
                    "classificationEvidenceSha256",
                    classificationEvidence.string(
                        "classificationEvidenceSha256",
                    ),
                )
            }
        val core =
            buildJsonObject {
                put(
                    "schemaVersion",
                    AcceptanceSchema,
                )
                put(
                    "contractRef",
                    LiveSemanticContract,
                )
                put(
                    "semanticContractDispositionRef",
                    LiveSemanticDisposition,
                )
                put(
                    "semanticExecutableAddendumRef",
                    LiveSemanticExecutableAddendum,
                )
                put(
                    "candidateRef",
                    candidate.string(
                        "candidateRef",
                    ),
                )
                put(
                    "candidateSha256",
                    candidate.string(
                        "candidateSha256",
                    ),
                )
                put(
                    "sourceLineageRef",
                    candidate.string(
                        "sourceLineageRef",
                    ),
                )
                put(
                    "sourceThreadRef",
                    candidate.string(
                        "sourceThreadRef",
                    ),
                )
                put(
                    "sourceConversationHeadSha256",
                    candidate.string(
                        "sourceConversationHeadSha256",
                    ),
                )
                putStrings(
                    "sourceBindingHashes",
                    sourceBindingHashes,
                    preserveOrder = true,
                )
                put(
                    "semanticSubjectRef",
                    candidate.string(
                        "semanticSubjectRef",
                    ),
                )
                put(
                    "semanticSubjectFingerprint",
                    candidate.string(
                        "semanticSubjectFingerprint",
                    ),
                )
                put(
                    "acceptedSummary",
                    classification.acceptedSummary,
                )
                put(
                    "acceptedSummarySha256",
                    acceptedSummarySha256,
                )
                put(
                    "memoryRelation",
                    classification.memoryRelation.wireValue,
                )
                put(
                    "statementState",
                    shape.statementState,
                )
                put(
                    "acceptedForContinuity",
                    true,
                )
                put(
                    "transitionKind",
                    shape.transitionKind.wireValue,
                )
                putNullable(
                    "transitionTargetRef",
                    shape.transitionTargetRef,
                )
                putNullable(
                    "transitionTargetAcceptanceSha256",
                    shape.transitionTargetAcceptanceSha256,
                )
                put(
                    "classificationEvidenceBindings",
                    JsonArray(
                        listOf(
                            evidenceBinding,
                        ),
                    ),
                )
                put(
                    "consentDispositionRef",
                    consent.string(
                        "consentDispositionRef",
                    ),
                )
                put(
                    "consentDispositionSha256",
                    consent.string(
                        "consentDispositionSha256",
                    ),
                )
                put(
                    "issuerRef",
                    SharedIssuerRef,
                )
                put(
                    "issuerClass",
                    SharedIssuerClass,
                )
                put(
                    "ownerDispositionRef",
                    LiveSemanticExecutableAddendum,
                )
                put(
                    "formedAt",
                    canonicalUtc(
                        formedAt,
                    ),
                )
                put(
                    "privacyClass",
                    PrivacyClass,
                )
            }
        return contentAddressed(
            prefix =
                "score-semantic-acceptance",
            refField =
                "acceptanceRef",
            hashField =
                "acceptanceSha256",
            core =
                core,
        )
    }

    private fun acceptanceBinding(
        candidate: JsonObject,
        classificationEvidence: JsonObject,
        consent: JsonObject,
        acceptance: JsonObject,
    ): JsonObject =
        buildJsonObject {
            put(
                "semanticSubjectFingerprint",
                candidate.string(
                    "semanticSubjectFingerprint",
                ),
            )
            put(
                "acceptanceRef",
                acceptance.string(
                    "acceptanceRef",
                ),
            )
            put(
                "acceptanceSha256",
                acceptance.string(
                    "acceptanceSha256",
                ),
            )
            put(
                "candidateRef",
                candidate.string(
                    "candidateRef",
                ),
            )
            put(
                "candidateSha256",
                candidate.string(
                    "candidateSha256",
                ),
            )
            put(
                "classificationEvidenceBindings",
                JsonArray(
                    listOf(
                        buildJsonObject {
                            put(
                                "classificationEvidenceRef",
                                classificationEvidence.string(
                                    "classificationEvidenceRef",
                                ),
                            )
                            put(
                                "classificationEvidenceSha256",
                                classificationEvidence.string(
                                    "classificationEvidenceSha256",
                                ),
                            )
                        },
                    ),
                ),
            )
            put(
                "consentDispositionRef",
                consent.string(
                    "consentDispositionRef",
                ),
            )
            put(
                "consentDispositionSha256",
                consent.string(
                    "consentDispositionSha256",
                ),
            )
        }

    private fun sourceEvidence(
        candidate: JsonObject,
        humanDecision: HumanDecisionSource,
    ): List<JsonObject> =
        (
            candidate
                .objectList(
                    "sourceBindings",
                )
                .map {
                    it.string(
                        "eventRef",
                    ) to
                        it.string(
                            "eventHash",
                        )
                } +
                (
                    humanDecision.decisionRef to
                        humanDecision.decisionSha256
                    )
        )
            .distinct()
            .sortedWith(
                compareBy<Pair<String, String>>(
                    Pair<String, String>::first,
                )
                    .thenBy(
                        Pair<String, String>::second,
                    ),
            )
            .map {
                (
                    sourceRef,
                    sourceSha256,
                ) ->
                buildJsonObject {
                    put(
                        "sourceRef",
                        sourceRef,
                    )
                    put(
                        "sourceSha256",
                        sourceSha256,
                    )
                }
            }

    private fun writeImmutable(
        home: Path,
        path: Path,
        value: JsonObject,
        label: String,
    ): WriteCount {
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
                ) {
                    "$label content-addressed path exists but is unreadable"
                }
            require(
                VexContractJson.Strict
                    .parseToJsonElement(
                        existing,
                    )
                    .jsonObject ==
                    value,
            ) {
                "$label content-addressed path already contains different bytes"
            }
            return WriteCount(
                reused = 1,
                created = 0,
            )
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
        return WriteCount(
            reused = 0,
            created = 1,
        )
    }

    private fun validateInvocation(
        invocation: KeepInvocation,
    ) {
        requireSafeRef(
            invocation.instanceRef,
            "instanceRef",
        )
        requireSafeRef(
            invocation.proposal.semanticSubjectRef,
            "semanticSubjectRef",
        )
        requireSafeRef(
            invocation.proposal.subjectScopeRef,
            "subjectScopeRef",
        )
        requirePortableRef(
            invocation.proposal.proposerRef,
            "proposerRef",
        )
        require(
            invocation.proposal.proposedSummary.isNotBlank() &&
                invocation.classification.acceptedSummary.isNotBlank(),
        ) {
            "semantic acceptance proposal/accepted summaries must be non-empty"
        }
        invocation.proposal.sourceEventHashes.forEach {
            requireSha256(
                it,
                "sourceEventHash",
            )
        }
        requireSafeRef(
            invocation.humanDecision.humanSubjectRef,
            "humanSubjectRef",
        )
        requireSafeRef(
            invocation.humanDecision.decisionRef,
            "humanDecisionRef",
        )
        requireSha256(
            invocation.humanDecision.decisionSha256,
            "humanDecisionSha256",
        )
        require(
            !invocation.humanDecision.decidedAt.isAfter(
                invocation.formedAt,
            ),
        ) {
            "human Keep decision cannot occur after semantic acceptance formation"
        }
        invocation.expectedSemanticAuthorityHeadSha256
            ?.let {
                requireSha256(
                    it,
                    "expectedSemanticAuthorityHeadSha256",
                )
            }
        when (
            val classification =
                invocation.classification
        ) {
            is Classification.Initial ->
                Unit

            is Classification.Corrects -> {
                requireSafeRef(
                    classification.targetStatementRef,
                    "targetStatementRef",
                )
                requireSha256(
                    classification.targetAcceptanceSha256,
                    "targetAcceptanceSha256",
                )
            }

            is Classification.Supersedes -> {
                requireSafeRef(
                    classification.targetStatementRef,
                    "targetStatementRef",
                )
                requireSha256(
                    classification.targetAcceptanceSha256,
                    "targetAcceptanceSha256",
                )
            }
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

    private fun semanticHash(
        value: JsonObject,
    ): String =
        sha256(
            VexContractJson.canonicalEncodeElement(
                value,
            ),
        )

    private fun semanticHashString(
        value: String,
    ): String =
        sha256(
            VexContractJson.canonicalEncode(
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

    private fun requirePortableRef(
        value: String,
        label: String,
    ) {
        require(
            value.matches(
                Regex(
                    "[A-Za-z0-9][A-Za-z0-9._:/-]*",
                ),
            ),
        ) {
            "$label must be one portable ref"
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

    private fun JsonObject.long(
        key: String,
    ): Long =
        string(
            key,
        )
            .toLong()

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

    private fun JsonObjectBuilder.putStrings(
        key: String,
        values: List<String>,
        preserveOrder: Boolean =
            false,
    ) {
        val selected =
            if (
                preserveOrder
            ) {
                values
            } else {
                values
                    .distinct()
                    .sorted()
            }
        put(
            key,
            JsonArray(
                selected.map(
                    ::JsonPrimitive,
                ),
            ),
        )
    }

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
