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
import java.util.UUID
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.put
import vexlife.continuity.VexLifeContinuityObservation
import vexlife.contract.VexContractJson
import vexlife.contract.VexDurableContractFile
import vexlife.conversation.DirectConversationIdentity
import vexlife.conversation.DirectConversationStore
import vexlife.conversation.VexLifeConversationContract
import vexlife.filesystem.VexOwnedPath
import vexlife.home.VexLifeProdHomeFiles

/**
 * Product/runtime side of current G02 Score continuity.
 *
 * VexLife-Prod may form a non-authoritative semantic candidate from exact canonical Conversation
 * source, and may append Score only after the separately-owned semantic-authority ledger has made one
 * exact acceptance current. It never mints classification evidence, positive consent, semantic
 * acceptance, or the semantic-authority head.
 */
object VexLifeScoreContinuity {
    const val SemanticCandidateSchema =
        "vextreme.score-semantic-candidate/v1"

    const val ScoreEventSchema =
        "vexlife.score-context-event/v1"

    const val ScoreHeadSchema =
        "vexlife.score-context-head/v1"

    const val SemanticAuthorityHeadSchema =
        "vextreme.score-semantic-authority-head/v1"

    const val LiveSemanticContract =
        "contract.multivex.score.live-semantic-acceptance.v1"

    private const val ScoreSharedDisposition =
        "github.issue.vextreme-sdk.350.comment.5215288414"

    private const val LiveSemanticDisposition =
        "github.issue.vextreme-sdk.350.comment.5216924433"

    private const val LiveSemanticExecutableAddendum =
        "github.issue.vextreme-sdk.350.comment.5217097749"

    private const val DevicePrivateContinuityUseRef =
        "use.score.device-private-continuity"

    private const val MemoryOwnerRef =
        "project.multivex.memory"

    private const val SafetyOwnerRef =
        "project.multivex.safety"

    private const val WriterSchema =
        "vexlife.score-context-writer-lease/v1"

    private val memoryRelations =
        setOf(
            "CURRENT_LINEAGE_AUTOBIOGRAPHY",
            "SHARED_RELATIONSHIP_HISTORY",
            "PREDECESSOR_WITNESS_HISTORY",
            "INHERITED_CONTEXT",
            "EXTERNAL_EVIDENCE",
            "DISPUTED_OR_UNRESOLVED",
        )

    private val statementStates =
        setOf(
            "OBSERVED",
            "HUMAN_CONFIRMED",
            "INFERRED",
            "CONFLICTED",
            "UNKNOWN",
            "CORRECTED",
            "SUPERSEDED",
            "RELEASED_OR_TOMBSTONED",
        )

    private val evidenceClasses =
        setOf(
            "STRUCTURAL_OBSERVATION",
            "HUMAN_CONFIRMATION",
            "LINEAGE_INFERENCE",
            "CONFLICT_PRESERVATION",
            "UNKNOWN_HOLD",
            "CORRECTION_ACCEPTANCE",
            "SUPERSESSION_ACCEPTANCE",
            "RELEASE_TOMBSTONE_AUTHORITY",
        )

    private val consentStates =
        setOf(
            "PERMITTED",
            "NARROWED",
            "DEFERRED",
            "DENIED",
            "UNKNOWN",
            "WITHDRAWN",
        )

    private val proposerClasses =
        setOf(
            "LINEAGE",
            "HUMAN",
            "SYSTEM_CLASSIFIER",
        )

    private val transitionKinds =
        setOf(
            "NONE",
            "CORRECTS",
            "SUPERSEDES",
            "RELEASES_OR_TOMBSTONES",
        )

    @Serializable
    data class SourceBinding(
        val eventRef: String,
        val eventHash: String,
        val eventKind: String,
        val sequence: Long,
        val turnRef: String,
        val messageRef: String,
        val contentHash: String,
    )

    data class CandidateInput(
        val semanticSubjectRef: String,
        val proposedSummary: String,
        val proposedMemoryRelation: String,
        val proposedStatementState: String,
        val proposerRef: String,
        val proposerClass: String,
        val formedAt: Instant,
        val subjectScopeRef: String =
            "scope.score.thread",
        val sourceEventHashes: List<String> =
            emptyList(),
    )

    data class CandidateResult(
        val candidate: JsonObject,
        val candidateRef: String,
        val candidateSha256: String,
        val sourceConversationHeadSha256: String,
        val sourceEventHashes: List<String>,
    )

    data class AppendStatementInvocation(
        val instanceRef: String,
        val statementRef: String,
        val semanticAcceptanceRef: String,
        val semanticAcceptanceSha256: String,
        val expectedScoreHeadSha256: String?,
        val formedAt: Instant,
    )

    data class AppendOpenLoopInvocation(
        val instanceRef: String,
        val openLoopRef: String,
        val sourceStatementRefs: List<String>,
        val expectedScoreHeadSha256: String?,
        val expectedConversationHeadSha256: String,
        val formedAt: Instant,
        val sourceEventHashes: List<String> =
            emptyList(),
        val summaryRef: String? =
            null,
    )

    data class CommitResult(
        val state: String,
        val scoreEventRef: String,
        val scoreEventHash: String,
        val scoreHeadSha256: String,
        val sourceConversationHeadSha256: String,
        val semanticAuthorityHeadSha256: String?,
        val currentStatementRefs: List<String>,
        val openLoopRefs: List<String>,
    )

    internal data class Faults(
        val failAfterEventWrite: Boolean =
            false,
        val failBeforeHeadAdvance: Boolean =
            false,
    )

    @Serializable
    private data class WriterLeaseCore(
        val schemaVersion: String,
        val companionLineageRef: String,
        val threadRef: String,
        val instanceRef: String,
        val pid: Long,
        val lockToken: String,
        val formedAt: String,
    )

    @Serializable
    private data class WriterLease(
        val schemaVersion: String,
        val companionLineageRef: String,
        val threadRef: String,
        val instanceRef: String,
        val pid: Long,
        val lockToken: String,
        val formedAt: String,
        val leaseSha256: String,
    )

    private data class Identity(
        val homeRef: String,
        val deviceRef: String,
        val lineageRef: String,
        val threadRef: String,
    )

    private data class ResolvedAcceptance(
        val authorityHead: JsonObject,
        val binding: JsonObject,
        val candidate: JsonObject,
        val evidence: List<JsonObject>,
        val consent: JsonObject,
        val acceptance: JsonObject,
    )

    private data class MutableScoreState(
        val identity: Identity,
        val coordinates: VexLifeCurrentMemoryReader.CurrentScoreCoordinates,
        val head: JsonObject?,
        val events: List<JsonObject>,
    )

    private class HeldWriter(
        private val home: Path,
        private val path: Path,
        private val lease: WriterLease,
    ) : AutoCloseable {
        override fun close() {
            val current =
                requireNotNull(
                    VexDurableContractFile.read<WriterLease>(
                        ownedRoot = home,
                        path = path,
                    ),
                ) {
                    "Score writer lease disappeared before release"
                }
            require(
                current ==
                    lease &&
                    leaseSha256(current) ==
                    current.leaseSha256,
            ) {
                "Score writer lease changed before release"
            }
            require(
                VexDurableContractFile.delete(
                    ownedRoot = home,
                    path = path,
                ),
            ) {
                "Score writer lease could not be released"
            }
        }
    }

    fun formCandidate(
        productHome: Path,
        input: CandidateInput,
    ): CandidateResult {
        requirePortableRef(
            input.semanticSubjectRef,
            "semanticSubjectRef",
        )
        requirePortableRef(
            input.subjectScopeRef,
            "subjectScopeRef",
        )
        requirePortableRef(
            input.proposerRef,
            "proposerRef",
        )
        require(
            input.proposedSummary.isNotBlank(),
        ) {
            "Score semantic candidate summary must be non-empty"
        }
        require(
            input.proposedMemoryRelation in
                memoryRelations,
        ) {
            "Score semantic candidate memory relation is not admitted"
        }
        require(
            input.proposedStatementState in
                statementStates,
        ) {
            "Score semantic candidate statement state is not admitted"
        }
        require(
            input.proposerClass in
                proposerClasses,
        ) {
            "Score semantic candidate proposer class is not admitted"
        }

        val home =
            VexOwnedPath.requireRoot(
                productHome,
                "canonical VexLife Home",
            )
        val currentHome =
            requireNotNull(
                VexLifeProdHomeFiles.observeExisting(
                    home,
                ),
            ) {
                "Score candidate formation requires one current canonical VexLife Home"
            }
        val conversation =
            DirectConversationStore(
                home,
            )
                .current()
        val head =
            requireNotNull(
                conversation.head,
            ) {
                "Score candidate formation requires one current completed Conversation head"
            }
        require(
            head.companionLineageRef ==
                currentHome.manifest.currentCompanionLineageRef &&
                head.threadRef ==
                DirectConversationIdentity.ThreadRef,
        ) {
            "Score candidate Conversation identity differs from current Home"
        }

        val sourceEvents =
            selectSourceEvents(
                conversation = conversation,
                requestedHashes =
                    input.sourceEventHashes,
            )
        val sourceBindings =
            sourceEvents.map(
                ::sourceBinding,
            )
        val formedAt =
            canonicalUtc(
                input.formedAt,
            )
        val subjectFingerprint =
            semanticHash(
                buildJsonObject {
                    put(
                        "schemaVersion",
                        "vextreme.score-semantic-subject/v1",
                    )
                    put(
                        "semanticSubjectRef",
                        input.semanticSubjectRef,
                    )
                    put(
                        "sourceLineageRef",
                        head.companionLineageRef,
                    )
                    put(
                        "sourceThreadRef",
                        head.threadRef,
                    )
                    put(
                        "subjectScopeRef",
                        input.subjectScopeRef,
                    )
                },
            )
        val summarySha256 =
            semanticHashString(
                input.proposedSummary,
            )
        val core =
            buildJsonObject {
                put(
                    "schemaVersion",
                    SemanticCandidateSchema,
                )
                put(
                    "sourceLineageRef",
                    head.companionLineageRef,
                )
                put(
                    "sourceThreadRef",
                    head.threadRef,
                )
                put(
                    "sourceConversationHeadSha256",
                    head.conversationHeadSha256,
                )
                put(
                    "sourceBindings",
                    VexContractJson.Strict.encodeToJsonElement(
                        sourceBindings,
                    ),
                )
                put(
                    "semanticSubjectRef",
                    input.semanticSubjectRef,
                )
                put(
                    "subjectScopeRef",
                    input.subjectScopeRef,
                )
                put(
                    "semanticSubjectFingerprint",
                    subjectFingerprint,
                )
                put(
                    "proposedSummary",
                    input.proposedSummary,
                )
                put(
                    "proposedSummarySha256",
                    summarySha256,
                )
                put(
                    "proposedMemoryRelation",
                    input.proposedMemoryRelation,
                )
                put(
                    "proposedStatementState",
                    input.proposedStatementState,
                )
                put(
                    "proposerRef",
                    input.proposerRef,
                )
                put(
                    "proposerClass",
                    input.proposerClass,
                )
                put(
                    "formedAt",
                    formedAt,
                )
                put(
                    "privacyClass",
                    "DEVICE_PRIVATE",
                )
                put(
                    "accepted",
                    false,
                )
            }
        val candidateRef =
            "score-semantic-candidate." +
                semanticHash(
                    core,
                ).take(
                    32,
                )
        val withRef =
            core.extend {
                put(
                    "candidateRef",
                    candidateRef,
                )
            }
        val candidateSha256 =
            semanticHash(
                withRef,
            )
        val candidate =
            withRef.extend {
                put(
                    "candidateSha256",
                    candidateSha256,
                )
            }

        return CandidateResult(
            candidate = candidate,
            candidateRef = candidateRef,
            candidateSha256 = candidateSha256,
            sourceConversationHeadSha256 =
                head.conversationHeadSha256,
            sourceEventHashes =
                sourceBindings.map(
                    SourceBinding::eventHash,
                ),
        )
    }

    fun appendAcceptedStatement(
        productHome: Path,
        invocation: AppendStatementInvocation,
    ): CommitResult =
        appendAcceptedStatementWithFaults(
            productHome = productHome,
            invocation = invocation,
            faults = Faults(),
        )

    internal fun appendAcceptedStatementWithFaults(
        productHome: Path,
        invocation: AppendStatementInvocation,
        faults: Faults,
    ): CommitResult {
        validateStatementInvocation(
            invocation,
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
        val scoreRoot =
            scoreRoot(
                home = home,
                identity = identity,
            )

        acquireWriter(
            home = home,
            identity = identity,
            instanceRef = invocation.instanceRef,
        ).use {
            val state =
                loadMutableScoreState(
                    home = home,
                    identity = identity,
                    scoreRoot = scoreRoot,
                )
            require(
                state.coordinates.scoreHeadSha256 ==
                    invocation.expectedScoreHeadSha256,
            ) {
                "SCORE_HEAD_MISMATCH: expected current Score head differs from observed current head"
            }
            require(
                state.events.none {
                    it.string(
                        "eventKind",
                    ) ==
                        "STATEMENT" &&
                        it.string(
                            "statementRef",
                        ) ==
                        invocation.statementRef
                },
            ) {
                "SCORE_LINK_INVALID: statementRef already exists in committed Score history"
            }

            val resolved =
                resolveCurrentAcceptance(
                    home = home,
                    identity = identity,
                    acceptanceRef =
                        invocation.semanticAcceptanceRef,
                    acceptanceSha256 =
                        invocation.semanticAcceptanceSha256,
                )
            val conversation =
                DirectConversationStore(
                    home,
                )
                    .current()
            val conversationHead =
                requireNotNull(
                    conversation.head,
                ) {
                    "SCORE_SOURCE_INVALID: accepted Score statement requires one current completed Conversation head"
                }
            require(
                conversationHead.conversationHeadSha256 ==
                    resolved.candidate.string(
                        "sourceConversationHeadSha256",
                    ),
            ) {
                "SCORE_SOURCE_INVALID: accepted semantic candidate is not bound to the exact current Conversation head"
            }
            val sourceEvents =
                validateCandidateSource(
                    candidate =
                        resolved.candidate,
                    conversation =
                        conversation,
                )
            val transitionKind =
                resolved.acceptance.string(
                    "transitionKind",
                )
            val transitionTarget =
                resolved.acceptance.nullableString(
                    "transitionTargetRef",
                )
            validateScoreTransition(
                state = state,
                resolved = resolved,
                transitionKind = transitionKind,
                transitionTarget = transitionTarget,
            )

            val formedAt =
                canonicalUtc(
                    invocation.formedAt,
                )
            val canonicalBindings =
                sourceEvents.map(
                    ::sourceBinding,
                )
            val observation =
                continuityObservation(
                    identity = identity,
                    sourceEvents = sourceEvents,
                    instanceRef = invocation.instanceRef,
                    formedAt = formedAt,
                )
            val eventCore =
                buildJsonObject {
                    put(
                        "schemaVersion",
                        ScoreEventSchema,
                    )
                    put(
                        "eventKind",
                        "STATEMENT",
                    )
                    put(
                        "homeRef",
                        identity.homeRef,
                    )
                    put(
                        "deviceRef",
                        identity.deviceRef,
                    )
                    put(
                        "companionLineageRef",
                        identity.lineageRef,
                    )
                    put(
                        "instanceRef",
                        invocation.instanceRef,
                    )
                    put(
                        "threadRef",
                        identity.threadRef,
                    )
                    put(
                        "sequence",
                        nextSequence(
                            state.head,
                        ),
                    )
                    putNullable(
                        "priorScoreEventHash",
                        state.head
                            ?.string(
                                "eventHash",
                            ),
                    )
                    put(
                        "sourceConversationHeadSha256",
                        resolved.candidate.string(
                            "sourceConversationHeadSha256",
                        ),
                    )
                    put(
                        "sourceBindings",
                        VexContractJson.Strict.encodeToJsonElement(
                            canonicalBindings,
                        ),
                    )
                    put(
                        "continuityObservation",
                        VexContractJson.Strict.encodeToJsonElement(
                            observation,
                        ),
                    )
                    put(
                        "statementRef",
                        invocation.statementRef,
                    )
                    put(
                        "subjectRef",
                        resolved.acceptance.string(
                            "semanticSubjectRef",
                        ),
                    )
                    put(
                        "semanticSubjectFingerprint",
                        resolved.acceptance.string(
                            "semanticSubjectFingerprint",
                        ),
                    )
                    put(
                        "memoryRelation",
                        resolved.acceptance.string(
                            "memoryRelation",
                        ),
                    )
                    put(
                        "statementState",
                        resolved.acceptance.string(
                            "statementState",
                        ),
                    )
                    put(
                        "summary",
                        resolved.acceptance.string(
                            "acceptedSummary",
                        ),
                    )
                    put(
                        "summaryHash",
                        resolved.acceptance.string(
                            "acceptedSummarySha256",
                        ),
                    )
                    put(
                        "acceptedForContinuity",
                        resolved.acceptance.boolean(
                            "acceptedForContinuity",
                        ),
                    )
                    put(
                        "consentState",
                        resolved.consent.string(
                            "disposition",
                        ),
                    )
                    put(
                        "transitionKind",
                        transitionKind,
                    )
                    putNullable(
                        "correctsStatementRef",
                        if (
                            transitionKind ==
                            "CORRECTS"
                        ) {
                            transitionTarget
                        } else {
                            null
                        },
                    )
                    putNullable(
                        "supersedesStatementRef",
                        if (
                            transitionKind ==
                            "SUPERSEDES"
                        ) {
                            transitionTarget
                        } else {
                            null
                        },
                    )
                    putNullable(
                        "releasesOrTombstonesStatementRef",
                        if (
                            transitionKind ==
                            "RELEASES_OR_TOMBSTONES"
                        ) {
                            transitionTarget
                        } else {
                            null
                        },
                    )
                    putNullable(
                        "transitionTargetAcceptanceSha256",
                        resolved.acceptance.nullableString(
                            "transitionTargetAcceptanceSha256",
                        ),
                    )
                    put(
                        "semanticAuthorityHeadSha256",
                        resolved.authorityHead.string(
                            "semanticAuthorityHeadSha256",
                        ),
                    )
                    put(
                        "semanticAcceptanceRef",
                        resolved.acceptance.string(
                            "acceptanceRef",
                        ),
                    )
                    put(
                        "semanticAcceptanceSha256",
                        resolved.acceptance.string(
                            "acceptanceSha256",
                        ),
                    )
                    put(
                        "semanticCandidateRef",
                        resolved.candidate.string(
                            "candidateRef",
                        ),
                    )
                    put(
                        "semanticCandidateSha256",
                        resolved.candidate.string(
                            "candidateSha256",
                        ),
                    )
                    put(
                        "classificationEvidenceBindings",
                        resolved.binding[
                            "classificationEvidenceBindings"
                        ]!!,
                    )
                    put(
                        "consentDispositionRef",
                        resolved.consent.string(
                            "consentDispositionRef",
                        ),
                    )
                    put(
                        "consentDispositionSha256",
                        resolved.consent.string(
                            "consentDispositionSha256",
                        ),
                    )
                    put(
                        "liveSemanticContractRef",
                        LiveSemanticContract,
                    )
                    put(
                        "liveSemanticDispositionRef",
                        LiveSemanticDisposition,
                    )
                    put(
                        "liveSemanticExecutableAddendumRef",
                        LiveSemanticExecutableAddendum,
                    )
                    put(
                        "privacyClass",
                        "DEVICE_PRIVATE",
                    )
                    put(
                        "rawSourceContentIncluded",
                        false,
                    )
                    put(
                        "sharedSemanticDispositionRef",
                        ScoreSharedDisposition,
                    )
                    put(
                        "formedAt",
                        formedAt,
                    )
                }

            val currentStatementRefs =
                state.coordinates.currentStatementRefs
                    .toMutableSet()
            transitionTarget
                ?.let(
                    currentStatementRefs::remove,
                )
            if (
                resolved.acceptance.string(
                    "statementState",
                ) !=
                "RELEASED_OR_TOMBSTONED"
            ) {
                currentStatementRefs +=
                    invocation.statementRef
            }

            return commitEvent(
                home = home,
                state = state,
                scoreRoot = scoreRoot,
                eventCore = eventCore,
                sourceConversationHeadSha256 =
                    resolved.candidate.string(
                        "sourceConversationHeadSha256",
                    ),
                semanticAuthorityHeadSha256 =
                    resolved.authorityHead.string(
                        "semanticAuthorityHeadSha256",
                    ),
                currentStatementRefs =
                    currentStatementRefs
                        .toList()
                        .sorted(),
                openLoopRefs =
                    state.coordinates.openLoopRefs,
                formedAt = formedAt,
                faults = faults,
            )
        }
    }

    fun appendOpenLoop(
        productHome: Path,
        invocation: AppendOpenLoopInvocation,
    ): CommitResult =
        appendOpenLoopWithFaults(
            productHome = productHome,
            invocation = invocation,
            faults = Faults(),
        )

    internal fun appendOpenLoopWithFaults(
        productHome: Path,
        invocation: AppendOpenLoopInvocation,
        faults: Faults,
    ): CommitResult {
        validateOpenLoopInvocation(
            invocation,
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
        val scoreRoot =
            scoreRoot(
                home = home,
                identity = identity,
            )

        acquireWriter(
            home = home,
            identity = identity,
            instanceRef = invocation.instanceRef,
        ).use {
            val state =
                loadMutableScoreState(
                    home = home,
                    identity = identity,
                    scoreRoot = scoreRoot,
                )
            require(
                state.coordinates.scoreHeadSha256 ==
                    invocation.expectedScoreHeadSha256,
            ) {
                "SCORE_HEAD_MISMATCH: expected current Score head differs from observed current head"
            }
            require(
                invocation.openLoopRef !in
                    state.coordinates.openLoopRefs,
            ) {
                "OPEN_LOOP_INVALID: openLoopRef already exists"
            }
            require(
                invocation.sourceStatementRefs
                    .all {
                        it in
                            state.coordinates.currentStatementRefs
                    },
            ) {
                "OPEN_LOOP_INVALID: source statement is absent or non-current"
            }

            val conversation =
                DirectConversationStore(
                    home,
                )
                    .current()
            val head =
                requireNotNull(
                    conversation.head,
                ) {
                    "SCORE_SOURCE_INVALID: open loop requires one current completed Conversation head"
                }
            require(
                head.conversationHeadSha256 ==
                    invocation.expectedConversationHeadSha256,
            ) {
                "SCORE_SOURCE_INVALID: expected Conversation head differs from current Conversation"
            }
            val sourceEvents =
                selectSourceEvents(
                    conversation = conversation,
                    requestedHashes =
                        invocation.sourceEventHashes,
                )
            val formedAt =
                canonicalUtc(
                    invocation.formedAt,
                )
            val canonicalBindings =
                sourceEvents.map(
                    ::sourceBinding,
                )
            val observation =
                continuityObservation(
                    identity = identity,
                    sourceEvents = sourceEvents,
                    instanceRef = invocation.instanceRef,
                    formedAt = formedAt,
                )
            val eventCore =
                buildJsonObject {
                    put(
                        "schemaVersion",
                        ScoreEventSchema,
                    )
                    put(
                        "eventKind",
                        "OPEN_LOOP",
                    )
                    put(
                        "homeRef",
                        identity.homeRef,
                    )
                    put(
                        "deviceRef",
                        identity.deviceRef,
                    )
                    put(
                        "companionLineageRef",
                        identity.lineageRef,
                    )
                    put(
                        "instanceRef",
                        invocation.instanceRef,
                    )
                    put(
                        "threadRef",
                        identity.threadRef,
                    )
                    put(
                        "sequence",
                        nextSequence(
                            state.head,
                        ),
                    )
                    putNullable(
                        "priorScoreEventHash",
                        state.head
                            ?.string(
                                "eventHash",
                            ),
                    )
                    put(
                        "sourceConversationHeadSha256",
                        head.conversationHeadSha256,
                    )
                    put(
                        "sourceBindings",
                        VexContractJson.Strict.encodeToJsonElement(
                            canonicalBindings,
                        ),
                    )
                    put(
                        "continuityObservation",
                        VexContractJson.Strict.encodeToJsonElement(
                            observation,
                        ),
                    )
                    put(
                        "openLoopRef",
                        invocation.openLoopRef,
                    )
                    put(
                        "openLoopState",
                        "OPEN",
                    )
                    putNullable(
                        "summaryRef",
                        invocation.summaryRef,
                    )
                    putStrings(
                        "sourceStatementRefs",
                        invocation.sourceStatementRefs
                            .distinct()
                            .sorted(),
                    )
                    put(
                        "privacyClass",
                        "DEVICE_PRIVATE",
                    )
                    put(
                        "rawSourceContentIncluded",
                        false,
                    )
                    put(
                        "sharedSemanticDispositionRef",
                        ScoreSharedDisposition,
                    )
                    put(
                        "formedAt",
                        formedAt,
                    )
                }

            return commitEvent(
                home = home,
                state = state,
                scoreRoot = scoreRoot,
                eventCore = eventCore,
                sourceConversationHeadSha256 =
                    head.conversationHeadSha256,
                semanticAuthorityHeadSha256 =
                    state.coordinates.semanticAuthorityHeadSha256,
                currentStatementRefs =
                    state.coordinates.currentStatementRefs,
                openLoopRefs =
                    (
                        state.coordinates.openLoopRefs +
                            invocation.openLoopRef
                    )
                        .distinct()
                        .sorted(),
                formedAt = formedAt,
                faults = faults,
            )
        }
    }

    fun recoverAbandonedWriter(
        productHome: Path,
        expectedCompanionLineageRef: String,
        expectedThreadRef: String =
            DirectConversationIdentity.ThreadRef,
        expectedInstanceRef: String,
    ) {
        requirePortableRef(
            expectedCompanionLineageRef,
            "expectedCompanionLineageRef",
        )
        requirePortableRef(
            expectedThreadRef,
            "expectedThreadRef",
        )
        requirePortableRef(
            expectedInstanceRef,
            "expectedInstanceRef",
        )
        val home =
            VexOwnedPath.requireRoot(
                productHome,
                "canonical VexLife Home",
            )
        val lock =
            writerLock(
                home = home,
                lineageRef =
                    expectedCompanionLineageRef,
                threadRef =
                    expectedThreadRef,
            )
        val lease =
            requireNotNull(
                VexDurableContractFile.read<WriterLease>(
                    ownedRoot = home,
                    path = lock,
                ),
            ) {
                "Score writer lease is unreadable"
            }
        require(
            lease.schemaVersion ==
                WriterSchema &&
                lease.companionLineageRef ==
                expectedCompanionLineageRef &&
                lease.threadRef ==
                expectedThreadRef &&
                lease.instanceRef ==
                expectedInstanceRef &&
                leaseSha256(
                    lease,
                ) ==
                lease.leaseSha256,
        ) {
            "Score writer lease identity is invalid"
        }
        require(
            processState(
                lease.pid,
            ) ==
                "ABSENT",
        ) {
            "Score writer recovery requires an absent owning process"
        }
        require(
            VexDurableContractFile.delete(
                ownedRoot = home,
                path = lock,
            ),
        ) {
            "Score writer recovery could not remove the exact abandoned lease"
        }
    }

    private fun commitEvent(
        home: Path,
        state: MutableScoreState,
        scoreRoot: Path,
        eventCore: JsonObject,
        sourceConversationHeadSha256: String,
        semanticAuthorityHeadSha256: String?,
        currentStatementRefs: List<String>,
        openLoopRefs: List<String>,
        formedAt: String,
        faults: Faults,
    ): CommitResult {
        val scoreEventRef =
            "score-event." +
                semanticHash(
                    eventCore,
                ).take(
                    32,
                )
        val finalCore =
            eventCore.extend {
                put(
                    "scoreEventRef",
                    scoreEventRef,
                )
            }
        val scoreEventHash =
            semanticHash(
                finalCore,
            )
        val event =
            finalCore.extend {
                put(
                    "scoreEventHash",
                    scoreEventHash,
                )
            }
        val sequence =
            event.long(
                "sequence",
            )
        val eventPath =
            scoreRoot.resolve(
                "events/" +
                    sequence
                        .toString()
                        .padStart(
                            8,
                            '0',
                        ) +
                    "-" +
                    scoreEventHash +
                    ".json",
            )
        writeImmutable(
            home = home,
            path = eventPath,
            value = event,
            label = "Score event",
        )
        if (
            faults.failAfterEventWrite
        ) {
            error(
                "SCORE_HEAD_MISMATCH: simulated failure after Score event durability before head advance",
            )
        }

        val headCore =
            buildJsonObject {
                put(
                    "schemaVersion",
                    ScoreHeadSchema,
                )
                put(
                    "homeRef",
                    state.identity.homeRef,
                )
                put(
                    "deviceRef",
                    state.identity.deviceRef,
                )
                put(
                    "companionLineageRef",
                    state.identity.lineageRef,
                )
                put(
                    "threadRef",
                    state.identity.threadRef,
                )
                put(
                    "eventHash",
                    scoreEventHash,
                )
                put(
                    "sequence",
                    sequence,
                )
                putStrings(
                    "currentStatementRefs",
                    currentStatementRefs
                        .distinct()
                        .sorted(),
                )
                putStrings(
                    "openLoopRefs",
                    openLoopRefs
                        .distinct()
                        .sorted(),
                )
                put(
                    "sourceConversationHeadSha256",
                    sourceConversationHeadSha256,
                )
                putNullable(
                    "priorScoreHeadSha256",
                    state.coordinates.scoreHeadSha256,
                )
                put(
                    "sharedSemanticDispositionRef",
                    ScoreSharedDisposition,
                )
                put(
                    "liveSemanticContractRef",
                    LiveSemanticContract,
                )
                put(
                    "liveSemanticDispositionRef",
                    LiveSemanticDisposition,
                )
                put(
                    "liveSemanticExecutableAddendumRef",
                    LiveSemanticExecutableAddendum,
                )
                put(
                    "formedAt",
                    formedAt,
                )
            }
        val scoreHeadSha256 =
            semanticHash(
                headCore,
            )
        val head =
            headCore.extend {
                put(
                    "scoreHeadSha256",
                    scoreHeadSha256,
                )
            }
        writeImmutable(
            home = home,
            path =
                scoreRoot.resolve(
                    "heads/$scoreHeadSha256.json",
                ),
            value = head,
            label = "Score head",
        )
        if (
            faults.failBeforeHeadAdvance
        ) {
            error(
                "SCORE_HEAD_MISMATCH: simulated failure before atomic Score head advance",
            )
        }
        VexDurableContractFile.write(
            ownedRoot = home,
            path =
                scoreRoot.resolve(
                    "head.json",
                ),
            value = head,
        )

        val current =
            VexLifeCurrentMemoryReader
                .readCurrentScoreCoordinates(
                    home,
                )
        require(
            current.scoreHeadSha256 ==
                scoreHeadSha256 &&
                current.sourceConversationHeadSha256 ==
                sourceConversationHeadSha256 &&
                current.currentStatementRefs ==
                currentStatementRefs
                    .distinct()
                    .sorted() &&
                current.openLoopRefs ==
                openLoopRefs
                    .distinct()
                    .sorted(),
        ) {
            "Score current head readback differs from the committed event/head"
        }

        return CommitResult(
            state = "COMMITTED",
            scoreEventRef = scoreEventRef,
            scoreEventHash = scoreEventHash,
            scoreHeadSha256 = scoreHeadSha256,
            sourceConversationHeadSha256 =
                sourceConversationHeadSha256,
            semanticAuthorityHeadSha256 =
                semanticAuthorityHeadSha256,
            currentStatementRefs =
                current.currentStatementRefs,
            openLoopRefs =
                current.openLoopRefs,
        )
    }

    private fun loadMutableScoreState(
        home: Path,
        identity: Identity,
        scoreRoot: Path,
    ): MutableScoreState {
        val coordinates =
            VexLifeCurrentMemoryReader
                .readCurrentScoreCoordinates(
                    home,
                )
        require(
            coordinates.homeRef ==
                identity.homeRef &&
                coordinates.deviceRef ==
                identity.deviceRef &&
                coordinates.companionLineageRef ==
                identity.lineageRef &&
                coordinates.threadRef ==
                identity.threadRef,
        ) {
            "Score coordinate identity changed while writer lease is held"
        }

        val headPath =
            scoreRoot.resolve(
                "head.json",
            )
        val head =
            if (
                Files.exists(
                    headPath,
                    LinkOption.NOFOLLOW_LINKS,
                )
            ) {
                readObject(
                    home = home,
                    path = headPath,
                    label = "current Score head",
                )
            } else {
                null
            }
        require(
            head
                ?.string(
                    "scoreHeadSha256",
                ) ==
                coordinates.scoreHeadSha256,
        ) {
            "Score current head changed while writer lease is held"
        }

        val events =
            scoreEventFiles(
                home = home,
                scoreRoot = scoreRoot,
            )
                .map {
                    readObject(
                        home = home,
                        path = it,
                        label = "Score event",
                    )
                }
        val expectedCommittedCount =
            head
                ?.long(
                    "sequence",
                )
                ?.plus(
                    1,
                )
                ?: 0L
        require(
            events.size.toLong() ==
                expectedCommittedCount,
        ) {
            "SCORE_TAIL_ATTENTION: uncommitted Score tail requires attention before mutation"
        }

        return MutableScoreState(
            identity = identity,
            coordinates = coordinates,
            head = head,
            events = events,
        )
    }

    private fun resolveCurrentAcceptance(
        home: Path,
        identity: Identity,
        acceptanceRef: String,
        acceptanceSha256: String,
    ): ResolvedAcceptance {
        val root =
            authorityRoot(
                home = home,
                identity = identity,
            )
        val current =
            readAddressed(
                home = home,
                path =
                    root.resolve(
                        "head.json",
                    ),
                schemaVersion =
                    SemanticAuthorityHeadSchema,
                refField =
                    "semanticAuthorityHeadRef",
                hashField =
                    "semanticAuthorityHeadSha256",
                prefix =
                    "score-semantic-authority-head",
                label =
                    "current semantic authority head",
            )
        validateAuthorityHead(
            current,
            identity,
        )
        val currentSha =
            current.string(
                "semanticAuthorityHeadSha256",
            )
        val immutable =
            readAddressed(
                home = home,
                path =
                    root.resolve(
                        "heads/$currentSha.json",
                    ),
                schemaVersion =
                    SemanticAuthorityHeadSchema,
                refField =
                    "semanticAuthorityHeadRef",
                hashField =
                    "semanticAuthorityHeadSha256",
                prefix =
                    "score-semantic-authority-head",
                label =
                    "immutable current semantic authority head",
            )
        require(
            immutable ==
                current,
        ) {
            "current semantic authority head lacks its exact immutable receipt"
        }
        validateAuthorityLineage(
            home = home,
            root = root,
            current = current,
            identity = identity,
        )

        val binding =
            current.objectList(
                "currentAcceptanceBindings",
            )
                .singleOrNull {
                    it.string(
                        "acceptanceRef",
                    ) ==
                        acceptanceRef &&
                        it.string(
                            "acceptanceSha256",
                        ) ==
                        acceptanceSha256
                }
        requireNotNull(
            binding,
        ) {
            "SCORE_SEMANTIC_AUTHORITY_STALE: requested acceptance is absent from current semantic authority"
        }

        val candidate =
            readAddressed(
                home = home,
                path =
                    root.resolve(
                        "candidates/" +
                            binding.string(
                                "candidateSha256",
                            ) +
                            ".json",
                    ),
                schemaVersion =
                    SemanticCandidateSchema,
                refField =
                    "candidateRef",
                hashField =
                    "candidateSha256",
                prefix =
                    "score-semantic-candidate",
                label =
                    "semantic candidate",
            )
        validateCandidate(
            candidate = candidate,
            identity = identity,
        )
        require(
            candidate.string(
                "candidateRef",
            ) ==
                binding.string(
                    "candidateRef",
                ) &&
                candidate.string(
                    "candidateSha256",
                ) ==
                binding.string(
                    "candidateSha256",
                ) &&
                candidate.string(
                    "semanticSubjectFingerprint",
                ) ==
                binding.string(
                    "semanticSubjectFingerprint",
                ),
        ) {
            "semantic authority candidate binding differs from candidate receipt"
        }

        val evidence =
            binding.objectList(
                "classificationEvidenceBindings",
            )
                .map {
                    evidenceBinding ->
                    val item =
                        readAddressed(
                            home = home,
                            path =
                                root.resolve(
                                    "classification-evidence/" +
                                        evidenceBinding.string(
                                            "classificationEvidenceSha256",
                                        ) +
                                        ".json",
                                ),
                            schemaVersion =
                                "vextreme.score-classification-evidence/v1",
                            refField =
                                "classificationEvidenceRef",
                            hashField =
                                "classificationEvidenceSha256",
                            prefix =
                                "score-classification-evidence",
                            label =
                                "classification evidence",
                        )
                    validateClassificationEvidence(
                        evidence = item,
                        candidate = candidate,
                    )
                    require(
                        item.string(
                            "classificationEvidenceRef",
                        ) ==
                            evidenceBinding.string(
                                "classificationEvidenceRef",
                            ) &&
                            item.string(
                                "classificationEvidenceSha256",
                            ) ==
                            evidenceBinding.string(
                                "classificationEvidenceSha256",
                            ),
                    ) {
                        "semantic authority classification binding differs from evidence receipt"
                    }
                    item
                }
        require(
            evidence.isNotEmpty(),
        ) {
            "semantic authority acceptance requires classification evidence"
        }

        val consent =
            readAddressed(
                home = home,
                path =
                    root.resolve(
                        "consents/" +
                            binding.string(
                                "consentDispositionSha256",
                            ) +
                            ".json",
                    ),
                schemaVersion =
                    "vextreme.score-consent-disposition/v1",
                refField =
                    "consentDispositionRef",
                hashField =
                    "consentDispositionSha256",
                prefix =
                    "score-consent-disposition",
                label =
                    "consent disposition",
            )
        validateConsent(
            consent = consent,
            candidate = candidate,
        )
        require(
            consent.string(
                "consentDispositionRef",
            ) ==
                binding.string(
                    "consentDispositionRef",
                ) &&
                consent.string(
                    "consentDispositionSha256",
                ) ==
                binding.string(
                    "consentDispositionSha256",
                ),
        ) {
            "semantic authority consent binding differs from consent receipt"
        }

        val acceptance =
            readAddressed(
                home = home,
                path =
                    root.resolve(
                        "acceptances/" +
                            acceptanceSha256 +
                            ".json",
                    ),
                schemaVersion =
                    "vextreme.score-semantic-acceptance/v1",
                refField =
                    "acceptanceRef",
                hashField =
                    "acceptanceSha256",
                prefix =
                    "score-semantic-acceptance",
                label =
                    "semantic acceptance",
            )
        validateAcceptance(
            acceptance = acceptance,
            candidate = candidate,
            evidence = evidence,
            consent = consent,
        )
        require(
            acceptance.string(
                "acceptanceRef",
            ) ==
                acceptanceRef &&
                acceptance.string(
                    "acceptanceSha256",
                ) ==
                acceptanceSha256,
        ) {
            "semantic acceptance identity differs from requested current acceptance"
        }

        return ResolvedAcceptance(
            authorityHead = current,
            binding = binding,
            candidate = candidate,
            evidence = evidence,
            consent = consent,
            acceptance = acceptance,
        )
    }

    private fun validateCandidate(
        candidate: JsonObject,
        identity: Identity,
    ) {
        require(
            candidate.string(
                "sourceLineageRef",
            ) ==
                identity.lineageRef &&
                candidate.string(
                    "sourceThreadRef",
                ) ==
                identity.threadRef &&
                candidate.string(
                    "privacyClass",
                ) ==
                "DEVICE_PRIVATE" &&
                !candidate.boolean(
                    "accepted",
                ) &&
                candidate.string(
                    "proposedMemoryRelation",
                ) in
                memoryRelations &&
                candidate.string(
                    "proposedStatementState",
                ) in
                statementStates &&
                candidate.string(
                    "proposerClass",
                ) in
                proposerClasses &&
                candidate.array(
                    "sourceBindings",
                )
                    .isNotEmpty() &&
                semanticHashString(
                    candidate.string(
                        "proposedSummary",
                    ),
                ) ==
                candidate.string(
                    "proposedSummarySha256",
                ),
        ) {
            "semantic candidate differs from the admitted live Score contract"
        }
        requireSha256(
            candidate.string(
                "sourceConversationHeadSha256",
            ),
            "candidate source Conversation head",
        )
        require(
            semanticHash(
                buildJsonObject {
                    put(
                        "schemaVersion",
                        "vextreme.score-semantic-subject/v1",
                    )
                    put(
                        "semanticSubjectRef",
                        candidate.string(
                            "semanticSubjectRef",
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
                        "subjectScopeRef",
                        candidate.string(
                            "subjectScopeRef",
                        ),
                    )
                },
            ) ==
                candidate.string(
                    "semanticSubjectFingerprint",
                ),
        ) {
            "semantic candidate subject fingerprint is invalid"
        }
    }

    private fun validateClassificationEvidence(
        evidence: JsonObject,
        candidate: JsonObject,
    ) {
        require(
            evidence.string(
                "candidateRef",
            ) ==
                candidate.string(
                    "candidateRef",
                ) &&
                evidence.string(
                    "candidateSha256",
                ) ==
                candidate.string(
                    "candidateSha256",
                ) &&
                evidence.string(
                    "semanticSubjectRef",
                ) ==
                candidate.string(
                    "semanticSubjectRef",
                ) &&
                evidence.string(
                    "semanticSubjectFingerprint",
                ) ==
                candidate.string(
                    "semanticSubjectFingerprint",
                ) &&
                evidence.string(
                    "evidenceClass",
                ) in
                evidenceClasses &&
                evidence.string(
                    "assertedMemoryRelation",
                ) in
                memoryRelations &&
                evidence.string(
                    "assertedStatementState",
                ) in
                statementStates &&
                evidence.string(
                    "transitionKind",
                ) in
                transitionKinds &&
                evidence.array(
                    "sourceEvidenceBindings",
                )
                    .isNotEmpty() &&
                evidence.string(
                    "privacyClass",
                ) ==
                "DEVICE_PRIVATE",
        ) {
            "classification evidence differs from the admitted live Score contract"
        }
        requireSha256(
            evidence.string(
                "assertedSummarySha256",
            ),
            "classification asserted summary",
        )
    }

    private fun validateConsent(
        consent: JsonObject,
        candidate: JsonObject,
    ) {
        require(
            consent.string(
                "candidateRef",
            ) ==
                candidate.string(
                    "candidateRef",
                ) &&
                consent.string(
                    "candidateSha256",
                ) ==
                candidate.string(
                    "candidateSha256",
                ) &&
                consent.string(
                    "semanticSubjectFingerprint",
                ) ==
                candidate.string(
                    "semanticSubjectFingerprint",
                ) &&
                consent.string(
                    "disposition",
                ) in
                consentStates,
        ) {
            "consent disposition differs from semantic candidate"
        }
    }

    private fun validateAcceptance(
        acceptance: JsonObject,
        candidate: JsonObject,
        evidence: List<JsonObject>,
        consent: JsonObject,
    ) {
        require(
            acceptance.string(
                "contractRef",
            ) ==
                LiveSemanticContract &&
                acceptance.string(
                    "semanticContractDispositionRef",
                ) ==
                LiveSemanticDisposition &&
                acceptance.string(
                    "semanticExecutableAddendumRef",
                ) ==
                LiveSemanticExecutableAddendum &&
                acceptance.string(
                    "candidateRef",
                ) ==
                candidate.string(
                    "candidateRef",
                ) &&
                acceptance.string(
                    "candidateSha256",
                ) ==
                candidate.string(
                    "candidateSha256",
                ) &&
                acceptance.string(
                    "sourceLineageRef",
                ) ==
                candidate.string(
                    "sourceLineageRef",
                ) &&
                acceptance.string(
                    "sourceThreadRef",
                ) ==
                candidate.string(
                    "sourceThreadRef",
                ) &&
                acceptance.string(
                    "sourceConversationHeadSha256",
                ) ==
                candidate.string(
                    "sourceConversationHeadSha256",
                ) &&
                acceptance.string(
                    "semanticSubjectRef",
                ) ==
                candidate.string(
                    "semanticSubjectRef",
                ) &&
                acceptance.string(
                    "semanticSubjectFingerprint",
                ) ==
                candidate.string(
                    "semanticSubjectFingerprint",
                ) &&
                acceptance.string(
                    "acceptedSummary",
                ) ==
                candidate.string(
                    "proposedSummary",
                ) &&
                acceptance.string(
                    "acceptedSummarySha256",
                ) ==
                candidate.string(
                    "proposedSummarySha256",
                ) &&
                acceptance.string(
                    "memoryRelation",
                ) in
                memoryRelations &&
                acceptance.string(
                    "statementState",
                ) in
                statementStates &&
                acceptance.string(
                    "transitionKind",
                ) in
                transitionKinds &&
                acceptance.string(
                    "consentDispositionRef",
                ) ==
                consent.string(
                    "consentDispositionRef",
                ) &&
                acceptance.string(
                    "consentDispositionSha256",
                ) ==
                consent.string(
                    "consentDispositionSha256",
                ) &&
                acceptance.string(
                    "privacyClass",
                ) ==
                "DEVICE_PRIVATE",
        ) {
            "semantic acceptance differs from the admitted live Score contract"
        }
        val evidenceBindings =
            evidence.map {
                item ->
                buildJsonObject {
                    put(
                        "classificationEvidenceRef",
                        item.string(
                            "classificationEvidenceRef",
                        ),
                    )
                    put(
                        "classificationEvidenceSha256",
                        item.string(
                            "classificationEvidenceSha256",
                        ),
                    )
                }
            }
        require(
            acceptance.objectList(
                "classificationEvidenceBindings",
            ) ==
                evidenceBindings &&
                acceptance.stringList(
                    "sourceBindingHashes",
                ) ==
                candidate.objectList(
                    "sourceBindings",
                )
                    .map {
                        it.string(
                            "eventHash",
                        )
                    },
        ) {
            "semantic acceptance source/evidence bindings differ from the candidate"
        }
        require(
            evidence.all {
                it.string(
                    "assertedMemoryRelation",
                ) ==
                    acceptance.string(
                        "memoryRelation",
                    ) &&
                    it.string(
                        "assertedStatementState",
                    ) ==
                    acceptance.string(
                        "statementState",
                    ) &&
                    it.string(
                        "assertedSummarySha256",
                    ) ==
                    acceptance.string(
                        "acceptedSummarySha256",
                    )
            },
        ) {
            "semantic acceptance meaning is not authorized by its classification evidence"
        }

        val transitionKind =
            acceptance.string(
                "transitionKind",
            )
        val expectedTransitionState =
            when (
                transitionKind
            ) {
                "CORRECTS" ->
                    "CORRECTED"

                "SUPERSEDES" ->
                    "SUPERSEDED"

                "RELEASES_OR_TOMBSTONES" ->
                    "RELEASED_OR_TOMBSTONED"

                else ->
                    null
            }
        if (
            transitionKind ==
                "NONE"
        ) {
            require(
                acceptance.nullableString(
                    "transitionTargetRef",
                ) ==
                    null &&
                    acceptance.nullableString(
                        "transitionTargetAcceptanceSha256",
                    ) ==
                    null &&
                    evidence.any {
                        it.string(
                            "evidenceClass",
                        ) ==
                            expectedEvidenceClass(
                                acceptance.string(
                                    "statementState",
                                ),
                            ) &&
                            it.string(
                                "transitionKind",
                            ) ==
                            "NONE"
                    },
            ) {
                "non-transition semantic acceptance lacks exact semantic evidence"
            }
        } else {
            val target =
                acceptance.nullableString(
                    "transitionTargetRef",
                )
            val targetAcceptance =
                acceptance.nullableString(
                    "transitionTargetAcceptanceSha256",
                )
            require(
                expectedTransitionState ==
                    acceptance.string(
                        "statementState",
                    ) &&
                    target !=
                    null &&
                    targetAcceptance !=
                    null &&
                    evidence.any {
                        it.string(
                            "evidenceClass",
                        ) ==
                            transitionEvidenceClass(
                                transitionKind,
                            ) &&
                            it.string(
                                "transitionKind",
                            ) ==
                            transitionKind &&
                            it.nullableString(
                                "transitionTargetRef",
                            ) ==
                            target &&
                            it.nullableString(
                                "transitionTargetAcceptanceSha256",
                            ) ==
                            targetAcceptance
                    },
            ) {
                "transition semantic acceptance lacks exact transition authority"
            }
        }

        val positiveContinuityConsent =
            consent.string(
                "disposition",
            ) in
                setOf(
                    "PERMITTED",
                    "NARROWED",
                ) &&
                consent.stringList(
                    "permittedUseRefs",
                )
                    .contains(
                        DevicePrivateContinuityUseRef,
                    ) &&
                !consent.stringList(
                    "prohibitedUseRefs",
                )
                    .contains(
                        DevicePrivateContinuityUseRef,
                    )
        val expectedAcceptedForContinuity =
            acceptance.string(
                "statementState",
            ) !=
                "RELEASED_OR_TOMBSTONED" &&
                positiveContinuityConsent
        require(
            acceptance.boolean(
                "acceptedForContinuity",
            ) ==
                expectedAcceptedForContinuity,
        ) {
            "acceptedForContinuity differs from current semantic/Safety policy"
        }
    }

    private fun validateScoreTransition(
        state: MutableScoreState,
        resolved: ResolvedAcceptance,
        transitionKind: String,
        transitionTarget: String?,
    ) {
        if (
            transitionKind ==
                "NONE"
        ) {
            require(
                transitionTarget ==
                    null,
            ) {
                "SCORE_LINK_INVALID: non-transition acceptance carries a target"
            }
            return
        }

        val target =
            requireNotNull(
                transitionTarget,
            ) {
                "SCORE_LINK_INVALID: transition acceptance has no predecessor"
            }
        require(
            target in
                state.coordinates.currentStatementRefs,
        ) {
            "SCORE_LINK_INVALID: transition predecessor is absent or non-current"
        }
        val prior =
            requireNotNull(
                state.events
                    .lastOrNull {
                        it.string(
                            "eventKind",
                        ) ==
                            "STATEMENT" &&
                            it.string(
                                "statementRef",
                            ) ==
                            target
                    },
            ) {
                "SCORE_LINK_INVALID: transition predecessor Score event is absent"
            }
        require(
            prior.string(
                "semanticSubjectFingerprint",
            ) ==
                resolved.acceptance.string(
                    "semanticSubjectFingerprint",
                ) &&
                prior.string(
                    "memoryRelation",
                ) ==
                resolved.acceptance.string(
                    "memoryRelation",
                ) &&
                prior.string(
                    "semanticAcceptanceSha256",
                ) ==
                resolved.acceptance.string(
                    "transitionTargetAcceptanceSha256",
                ),
        ) {
            "SCORE_LINK_INVALID: owner transition does not bind the exact current predecessor"
        }
    }

    private fun validateCandidateSource(
        candidate: JsonObject,
        conversation: DirectConversationStore.Snapshot,
    ): List<DirectConversationStore.Event> {
        val head =
            requireNotNull(
                conversation.head,
            ) {
                "SCORE_SOURCE_INVALID: semantic candidate source has no current Conversation head"
            }
        require(
            candidate.string(
                "sourceConversationHeadSha256",
            ) ==
                head.conversationHeadSha256,
        ) {
            "SCORE_SOURCE_INVALID: candidate source head is not the exact current Conversation head"
        }
        val sourceByHash =
            conversation.canonicalEvents
                .associateBy(
                    DirectConversationStore.Event::eventHash,
                )
        val sourceEvents =
            candidate.objectList(
                "sourceBindings",
            )
                .map {
                    binding ->
                    val observed =
                        requireNotNull(
                            sourceByHash[
                                binding.string(
                                    "eventHash",
                                )
                            ],
                        ) {
                            "SCORE_SOURCE_INVALID: semantic candidate source binding is absent from canonical Conversation"
                        }
                    require(
                        binding.string(
                            "eventRef",
                        ) ==
                            observed.eventRef &&
                            binding.string(
                                "eventKind",
                            ) ==
                            observed.eventKind &&
                            binding.long(
                                "sequence",
                            ) ==
                            observed.sequence &&
                            binding.string(
                                "turnRef",
                            ) ==
                            observed.turnRef &&
                            binding.string(
                                "messageRef",
                            ) ==
                            observed.messageRef &&
                            binding.string(
                                "contentHash",
                            ) ==
                            observed.contentHash,
                    ) {
                        "SCORE_SOURCE_INVALID: semantic candidate substitutes canonical Conversation source"
                    }
                    observed
                }
        validateSourcePairs(
            sourceEvents,
        )
        require(
            sourceEvents.map(
                ::sourceBinding,
            ) ==
                candidate.array(
                    "sourceBindings",
                )
                    .map {
                        VexContractJson.Strict.decodeFromJsonElement<SourceBinding>(
                            it,
                        )
                    },
        ) {
            "SCORE_SOURCE_INVALID: semantic candidate source binding bytes are not canonical"
        }
        return sourceEvents
    }

    private fun selectSourceEvents(
        conversation: DirectConversationStore.Snapshot,
        requestedHashes: List<String>,
    ): List<DirectConversationStore.Event> {
        val canonical =
            conversation.canonicalEvents
        require(
            canonical.size >=
                2 &&
                canonical.size %
                2 ==
                0,
        ) {
            "Score source selection requires one or more completed Conversation pairs"
        }
        val hashes =
            if (
                requestedHashes.isEmpty()
            ) {
                canonical
                    .takeLast(
                        2,
                    )
                    .map(
                        DirectConversationStore.Event::eventHash,
                    )
            } else {
                requestedHashes
            }
        require(
            hashes.isNotEmpty() &&
                hashes.size %
                2 ==
                0 &&
                hashes.distinct().size ==
                hashes.size,
        ) {
            "Score source selection must contain unique complete request/response pairs"
        }
        hashes.forEach {
            requireSha256(
                it,
                "Score source event hash",
            )
        }
        val byHash =
            canonical.associateBy(
                DirectConversationStore.Event::eventHash,
            )
        val selected =
            hashes.map {
                hash ->
                requireNotNull(
                    byHash[
                        hash
                    ],
                ) {
                    "Score source selection is outside the current canonical Conversation"
                }
            }
        validateSourcePairs(
            selected,
        )
        return selected
    }

    private fun validateSourcePairs(
        sourceEvents: List<DirectConversationStore.Event>,
    ) {
        require(
            sourceEvents.isNotEmpty() &&
                sourceEvents.size %
                2 ==
                0,
        ) {
            "Score source intake requires complete committed request/response pairs"
        }
        sourceEvents
            .chunked(
                2,
            )
            .forEach {
                pair ->
                val request =
                    pair[
                        0
                    ]
                val response =
                    pair[
                        1
                    ]
                require(
                    request.eventKind ==
                        VexLifeConversationContract.RequestKind &&
                        response.eventKind ==
                        VexLifeConversationContract.ResponseKind &&
                        response.sequence ==
                        request.sequence +
                        1 &&
                        response.priorEventHash ==
                        request.eventHash &&
                        response.turnRef ==
                        request.turnRef,
                ) {
                    "Score source selection is not composed of exact committed Conversation pairs"
                }
            }
    }

    private fun sourceBinding(
        event: DirectConversationStore.Event,
    ): SourceBinding =
        SourceBinding(
            eventRef = event.eventRef,
            eventHash = event.eventHash,
            eventKind = event.eventKind,
            sequence = event.sequence,
            turnRef = event.turnRef,
            messageRef = event.messageRef,
            contentHash = event.contentHash,
        )

    private fun continuityObservation(
        identity: Identity,
        sourceEvents: List<DirectConversationStore.Event>,
        instanceRef: String,
        formedAt: String,
    ): VexLifeContinuityObservation.Observation {
        val core =
            VexLifeContinuityObservation
                .conversationEpisodeRangeCore(
                    sourceLineageRef =
                        identity.lineageRef,
                    sourceEvents =
                        sourceEvents.map {
                            source ->
                            VexLifeContinuityObservation.ConversationEventFact(
                                eventRef =
                                    source.eventRef,
                                eventHash =
                                    source.eventHash,
                                speakerRef =
                                    source.speakerRef,
                                recipientRefs =
                                    source.recipientRefs,
                            )
                        },
                    threadRef =
                        identity.threadRef,
                    formedByRef =
                        instanceRef,
                    formedAt =
                        formedAt,
                )
        val fingerprint =
            sha256(
                VexContractJson.canonicalEncode(
                    core,
                ),
            )
        return VexLifeContinuityObservation
            .observationFromCore(
                core = core,
                observationRef =
                    "continuity-observation." +
                        fingerprint.take(
                            24,
                        ),
                semanticFingerprint =
                    fingerprint,
            )
    }

    private fun validateAuthorityHead(
        head: JsonObject,
        identity: Identity,
    ) {
        require(
            head.string(
                "contractRef",
            ) ==
                LiveSemanticContract &&
                head.string(
                    "semanticExecutableAddendumRef",
                ) ==
                LiveSemanticExecutableAddendum &&
                head.string(
                    "sourceLineageRef",
                ) ==
                identity.lineageRef &&
                head.string(
                    "sourceThreadRef",
                ) ==
                identity.threadRef &&
                head.stringList(
                    "ownerRefs",
                ) ==
                listOf(
                    MemoryOwnerRef,
                    SafetyOwnerRef,
                ) &&
                head.long(
                    "sequence",
                ) >=
                0,
        ) {
            "semantic authority head identity is invalid"
        }
        val prior =
            head.nullableString(
                "priorSemanticAuthorityHeadSha256",
            )
        if (
            head.long(
                "sequence",
            ) ==
                0L
        ) {
            require(
                prior ==
                    null,
            ) {
                "semantic authority genesis head has a predecessor"
            }
        } else {
            requireNotNull(
                prior,
            ) {
                "semantic authority non-genesis head has no predecessor"
            }
            requireSha256(
                prior,
                "priorSemanticAuthorityHeadSha256",
            )
        }
        val seen =
            mutableSetOf<String>()
        head.objectList(
            "currentAcceptanceBindings",
        )
            .forEach {
                binding ->
                require(
                    seen.add(
                        binding.string(
                            "semanticSubjectFingerprint",
                        ),
                    ) &&
                        binding.objectList(
                            "classificationEvidenceBindings",
                        )
                            .isNotEmpty(),
                ) {
                    "semantic authority head contains duplicate/malformed current subject"
                }
                listOf(
                    "semanticSubjectFingerprint",
                    "acceptanceSha256",
                    "candidateSha256",
                    "consentDispositionSha256",
                )
                    .forEach {
                        field ->
                        requireSha256(
                            binding.string(
                                field,
                            ),
                            field,
                        )
                    }
            }
    }

    private fun validateAuthorityLineage(
        home: Path,
        root: Path,
        current: JsonObject,
        identity: Identity,
    ) {
        val visited =
            mutableSetOf<String>()
        var cursor =
            current
        while (
            true
        ) {
            val currentSha =
                cursor.string(
                    "semanticAuthorityHeadSha256",
                )
            require(
                visited.add(
                    currentSha,
                ),
            ) {
                "semantic authority head lineage contains a cycle"
            }
            val sequence =
                cursor.long(
                    "sequence",
                )
            val priorSha =
                cursor.nullableString(
                    "priorSemanticAuthorityHeadSha256",
                )
            if (
                sequence ==
                    0L
            ) {
                require(
                    priorSha ==
                        null,
                )
                return
            }
            val prior =
                readAddressed(
                    home = home,
                    path =
                        root.resolve(
                            "heads/" +
                                requireNotNull(
                                    priorSha,
                                ) +
                                ".json",
                        ),
                    schemaVersion =
                        SemanticAuthorityHeadSchema,
                    refField =
                        "semanticAuthorityHeadRef",
                    hashField =
                        "semanticAuthorityHeadSha256",
                    prefix =
                        "score-semantic-authority-head",
                    label =
                        "prior semantic authority head",
                )
            validateAuthorityHead(
                prior,
                identity,
            )
            require(
                prior.long(
                    "sequence",
                ) ==
                    sequence -
                    1,
            ) {
                "semantic authority head sequence is not contiguous"
            }
            cursor =
                prior
        }
    }

    private fun currentIdentity(
        home: Path,
    ): Identity {
        val currentHome =
            requireNotNull(
                VexLifeProdHomeFiles.observeExisting(
                    home,
                ),
            ) {
                "Score continuity requires one current canonical VexLife Home"
            }
        return Identity(
            homeRef =
                currentHome.manifest.homeRef,
            deviceRef =
                currentHome.manifest.currentDeviceRef,
            lineageRef =
                currentHome.manifest.currentCompanionLineageRef,
            threadRef =
                DirectConversationIdentity.ThreadRef,
        )
    }

    private fun scoreRoot(
        home: Path,
        identity: Identity,
    ): Path =
        VexOwnedPath.resolveInside(
            home,
            "score/" +
                identity.lineageRef +
                "/" +
                identity.threadRef,
        )

    private fun authorityRoot(
        home: Path,
        identity: Identity,
    ): Path =
        VexOwnedPath.resolveInside(
            home,
            "semantic-authority/score/" +
                identity.lineageRef +
                "/" +
                identity.threadRef,
        )

    private fun writerLock(
        home: Path,
        lineageRef: String,
        threadRef: String,
    ): Path =
        VexOwnedPath.resolveInside(
            home,
            "runtime/score-writer-locks/" +
                lineageRef +
                "/" +
                threadRef +
                ".lock",
        )

    private fun acquireWriter(
        home: Path,
        identity: Identity,
        instanceRef: String,
    ): HeldWriter {
        requirePortableRef(
            instanceRef,
            "instanceRef",
        )
        val lock =
            writerLock(
                home = home,
                lineageRef =
                    identity.lineageRef,
                threadRef =
                    identity.threadRef,
            )
        VexOwnedPath.ensureParent(
            home,
            lock,
        )
        val core =
            WriterLeaseCore(
                schemaVersion =
                    WriterSchema,
                companionLineageRef =
                    identity.lineageRef,
                threadRef =
                    identity.threadRef,
                instanceRef =
                    instanceRef,
                pid =
                    ProcessHandle.current()
                        .pid(),
                lockToken =
                    UUID.randomUUID()
                        .toString()
                        .lowercase(),
                formedAt =
                    canonicalUtc(
                        Instant.now(),
                    ),
            )
        val lease =
            WriterLease(
                schemaVersion =
                    core.schemaVersion,
                companionLineageRef =
                    core.companionLineageRef,
                threadRef =
                    core.threadRef,
                instanceRef =
                    core.instanceRef,
                pid =
                    core.pid,
                lockToken =
                    core.lockToken,
                formedAt =
                    core.formedAt,
                leaseSha256 =
                    sha256(
                        VexContractJson.canonicalEncode(
                            core,
                        ),
                    ),
            )
        try {
            Files.writeString(
                lock,
                VexContractJson.encode(
                    lease,
                ) +
                    "\n",
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE,
            )
            VexDurableContractFile.secureFile(
                lock,
            )
        } catch (
            _: FileAlreadyExistsException,
        ) {
            val observed =
                requireNotNull(
                    VexDurableContractFile.read<WriterLease>(
                        ownedRoot = home,
                        path = lock,
                    ),
                ) {
                    "Score writer lease exists but is unreadable"
                }
            require(
                leaseSha256(
                    observed,
                ) ==
                    observed.leaseSha256,
            ) {
                "Score writer lease is invalid"
            }
            if (
                processState(
                    observed.pid,
                ) ==
                "ABSENT"
            ) {
                error(
                    "SCORE_WRITER_RECOVERY_REQUIRED: prior Score writer is absent; explicit recovery is required",
                )
            }
            error(
                "SCORE_WRITER_CONFLICT: another Score writer is active or unverifiable",
            )
        }
        return HeldWriter(
            home = home,
            path = lock,
            lease = lease,
        )
    }

    private fun validateStatementInvocation(
        invocation: AppendStatementInvocation,
    ) {
        requirePortableRef(
            invocation.instanceRef,
            "instanceRef",
        )
        requirePortableRef(
            invocation.statementRef,
            "statementRef",
        )
        requirePortableRef(
            invocation.semanticAcceptanceRef,
            "semanticAcceptanceRef",
        )
        requireSha256(
            invocation.semanticAcceptanceSha256,
            "semanticAcceptanceSha256",
        )
        invocation.expectedScoreHeadSha256
            ?.let {
                requireSha256(
                    it,
                    "expectedScoreHeadSha256",
                )
            }
    }

    private fun validateOpenLoopInvocation(
        invocation: AppendOpenLoopInvocation,
    ) {
        requirePortableRef(
            invocation.instanceRef,
            "instanceRef",
        )
        requirePortableRef(
            invocation.openLoopRef,
            "openLoopRef",
        )
        invocation.sourceStatementRefs
            .forEach {
                requirePortableRef(
                    it,
                    "sourceStatementRef",
                )
            }
        require(
            invocation.sourceStatementRefs.isNotEmpty(),
        ) {
            "OPEN_LOOP_INVALID: open loop requires source statement refs"
        }
        invocation.expectedScoreHeadSha256
            ?.let {
                requireSha256(
                    it,
                    "expectedScoreHeadSha256",
                )
            }
        requireSha256(
            invocation.expectedConversationHeadSha256,
            "expectedConversationHeadSha256",
        )
        invocation.summaryRef
            ?.let {
                requirePortableRef(
                    it,
                    "summaryRef",
                )
            }
    }

    private fun scoreEventFiles(
        home: Path,
        scoreRoot: Path,
    ): List<Path> {
        val directory =
            scoreRoot.resolve(
                "events",
            )
        return when (
            VexOwnedPath.observeDirectory(
                home,
                directory,
            )
        ) {
            VexOwnedPath.DirectoryState.Missing ->
                emptyList()

            VexOwnedPath.DirectoryState.Present ->
                Files.list(
                    directory,
                )
                    .use {
                        stream ->
                        stream
                            .filter {
                                it.fileName
                                    .toString()
                                    .endsWith(
                                        ".json",
                                    )
                            }
                            .sorted()
                            .toList()
                    }
        }
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
        try {
            Files.writeString(
                path,
                VexContractJson.canonicalEncodeElement(
                    value,
                ) +
                    "\n",
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
                "$label content-addressed path already exists",
            )
        }
    }

    private fun readAddressed(
        home: Path,
        path: Path,
        schemaVersion: String,
        refField: String,
        hashField: String,
        prefix: String,
        label: String,
    ): JsonObject {
        val value =
            readObject(
                home = home,
                path = path,
                label = label,
            )
        require(
            value.string(
                "schemaVersion",
            ) ==
                schemaVersion,
        ) {
            "$label schema is not current"
        }
        val ref =
            value.string(
                refField,
            )
        val hash =
            value.string(
                hashField,
            )
        requireSha256(
            hash,
            "$label hash",
        )
        val withoutIdentity =
            JsonObject(
                value.filterKeys {
                    it !=
                        refField &&
                        it !=
                        hashField
                },
            )
        val expectedRef =
            prefix +
                "." +
                semanticHash(
                    withoutIdentity,
                )
                    .take(
                        32,
                    )
        val withoutHash =
            JsonObject(
                value.filterKeys {
                    it !=
                        hashField
                },
            )
        require(
            ref ==
                expectedRef &&
                semanticHash(
                    withoutHash,
                ) ==
                hash,
        ) {
            "$label content-addressed identity is invalid"
        }
        return value
    }

    private fun readObject(
        home: Path,
        path: Path,
        label: String,
    ): JsonObject {
        val text =
            requireNotNull(
                VexDurableContractFile.readText(
                    ownedRoot = home,
                    path = path,
                ),
            ) {
                "$label is missing"
            }
        return VexContractJson.Strict
            .parseToJsonElement(
                text,
            )
            .jsonObject
    }

    private fun nextSequence(
        head: JsonObject?,
    ): Long =
        head
            ?.long(
                "sequence",
            )
            ?.plus(
                1,
            )
            ?: 0L

    private fun expectedEvidenceClass(
        statementState: String,
    ): String =
        when (
            statementState
        ) {
            "OBSERVED" ->
                "STRUCTURAL_OBSERVATION"

            "HUMAN_CONFIRMED" ->
                "HUMAN_CONFIRMATION"

            "INFERRED" ->
                "LINEAGE_INFERENCE"

            "CONFLICTED" ->
                "CONFLICT_PRESERVATION"

            "UNKNOWN" ->
                "UNKNOWN_HOLD"

            "RELEASED_OR_TOMBSTONED" ->
                "RELEASE_TOMBSTONE_AUTHORITY"

            else ->
                error(
                    "statement state requires transition evidence",
                )
        }

    private fun transitionEvidenceClass(
        transitionKind: String,
    ): String =
        when (
            transitionKind
        ) {
            "CORRECTS" ->
                "CORRECTION_ACCEPTANCE"

            "SUPERSEDES" ->
                "SUPERSESSION_ACCEPTANCE"

            "RELEASES_OR_TOMBSTONES" ->
                "RELEASE_TOMBSTONE_AUTHORITY"

            else ->
                error(
                    "transition kind has no transition evidence class",
                )
        }

    private fun leaseSha256(
        lease: WriterLease,
    ): String =
        sha256(
            VexContractJson.canonicalEncode(
                WriterLeaseCore(
                    schemaVersion =
                        lease.schemaVersion,
                    companionLineageRef =
                        lease.companionLineageRef,
                    threadRef =
                        lease.threadRef,
                    instanceRef =
                        lease.instanceRef,
                    pid =
                        lease.pid,
                    lockToken =
                        lease.lockToken,
                    formedAt =
                        lease.formedAt,
                ),
            ),
        )

    private fun processState(
        pid: Long,
    ): String =
        try {
            ProcessHandle
                .of(
                    pid,
                )
                .map {
                    if (
                        it.isAlive
                    ) {
                        "ACTIVE"
                    } else {
                        "ABSENT"
                    }
                }
                .orElse(
                    "ABSENT",
                )
        } catch (
            _: Exception,
        ) {
            "UNVERIFIABLE"
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
                    it,
                )
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

    private fun JsonObject.extend(
        block: kotlinx.serialization.json.JsonObjectBuilder.() -> Unit,
    ): JsonObject =
        buildJsonObject {
            this@extend.forEach {
                key,
                value,
                ->
                put(
                    key,
                    value,
                )
            }
            block()
        }

    private fun kotlinx.serialization.json.JsonObjectBuilder.putNullable(
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

    private fun kotlinx.serialization.json.JsonObjectBuilder.putStrings(
        key: String,
        values: List<String>,
    ) {
        put(
            key,
            JsonArray(
                values.map(
                    ::JsonPrimitive,
                ),
            ),
        )
    }

    private fun JsonObject.string(
        field: String,
    ): String =
        requireNotNull(
            this[
                field
            ],
        )
            .jsonPrimitive
            .content

    private fun JsonObject.nullableString(
        field: String,
    ): String? {
        val value =
            this[
                field
            ]
                ?: return null
        return if (
            value ===
            JsonNull
        ) {
            null
        } else {
            value
                .jsonPrimitive
                .content
        }
    }

    private fun JsonObject.long(
        field: String,
    ): Long =
        requireNotNull(
            this[
                field
            ],
        )
            .jsonPrimitive
            .long

    private fun JsonObject.boolean(
        field: String,
    ): Boolean =
        requireNotNull(
            this[
                field
            ],
        )
            .jsonPrimitive
            .boolean

    private fun JsonObject.array(
        field: String,
    ): JsonArray =
        requireNotNull(
            this[
                field
            ],
        )
            .jsonArray

    private fun JsonObject.objectList(
        field: String,
    ): List<JsonObject> =
        array(
            field,
        )
            .map {
                it.jsonObject
            }

    private fun JsonObject.stringList(
        field: String,
    ): List<String> =
        array(
            field,
        )
            .map {
                it.jsonPrimitive
                    .content
            }
}

// [VXG RealForever]
