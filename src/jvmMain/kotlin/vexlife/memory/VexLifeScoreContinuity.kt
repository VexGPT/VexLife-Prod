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
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
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
 * Product/runtime mutation owner for current G02 Score continuity.
 *
 * This object may form one non-authoritative semantic candidate from exact canonical Conversation
 * source. It may commit Score only after [VexLifeCurrentMemoryReader] has resolved one exact
 * acceptance from the separately-owned current semantic-authority ledger.
 *
 * It never mints classification evidence, consent, semantic acceptance, or semantic-authority heads.
 */
object VexLifeScoreContinuity {
    const val SemanticCandidateSchema =
        "vextreme.score-semantic-candidate/v1"

    const val ScoreEventSchema =
        "vexlife.score-context-event/v1"

    const val ScoreHeadSchema =
        "vexlife.score-context-head/v1"

    private const val ScoreSharedDisposition =
        "github.issue.vextreme-sdk.350.comment.5215288414"

    private const val LiveSemanticContract =
        "contract.multivex.score.live-semantic-acceptance/v1"

    private const val LiveSemanticDisposition =
        "github.issue.vextreme-sdk.350.comment.5216924433"

    private const val LiveSemanticExecutableAddendum =
        "github.issue.vextreme-sdk.350.comment.5217097749"

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

    private val proposerClasses =
        setOf(
            "LINEAGE",
            "HUMAN",
            "SYSTEM_CLASSIFIER",
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

    private data class MutableState(
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
                    leaseSha256(
                        current,
                    ) ==
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
        validateCandidateInput(
            input,
        )
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

        val source =
            selectSourceEvents(
                conversation = conversation,
                requestedHashes =
                    input.sourceEventHashes,
            )
        val sourceBindings =
            source.map(
                ::sourceBinding,
            )
        val formedAt =
            canonicalUtc(
                input.formedAt,
            )
        val semanticSubjectFingerprint =
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
        val proposedSummarySha256 =
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
                    semanticSubjectFingerprint,
                )
                put(
                    "proposedSummary",
                    input.proposedSummary,
                )
                put(
                    "proposedSummarySha256",
                    proposedSummarySha256,
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

        return acquireWriter(
            home = home,
            identity = identity,
            instanceRef = invocation.instanceRef,
        )
            .use {
                val state =
                    loadState(
                        home = home,
                        identity = identity,
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
                    "SCORE_LINK_INVALID: statementRef already exists"
                }

                val resolved =
                    VexLifeCurrentMemoryReader
                        .readCurrentScoreAcceptance(
                            productHome = home,
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
                val currentConversation =
                    requireNotNull(
                        conversation.head,
                    ) {
                        "SCORE_SOURCE_INVALID: accepted Score statement requires current Conversation"
                    }
                require(
                    resolved.candidate.string(
                        "sourceConversationHeadSha256",
                    ) ==
                        currentConversation.conversationHeadSha256,
                ) {
                    "SCORE_SOURCE_INVALID: accepted semantic candidate is not bound to the exact current Conversation head"
                }
                val source =
                    validateCandidateSource(
                        candidate = resolved.candidate,
                        conversation = conversation,
                    )
                validateTransition(
                    state = state,
                    acceptance =
                        resolved.acceptance,
                )

                val formedAt =
                    canonicalUtc(
                        invocation.formedAt,
                    )
                val eventCore =
                    statementEventCore(
                        identity = identity,
                        state = state,
                        invocation = invocation,
                        acceptance = resolved,
                        source = source,
                        formedAt = formedAt,
                    )
                val currentStatementRefs =
                    state.coordinates
                        .currentStatementRefs
                        .toMutableSet()
                resolved.acceptance
                    .nullableString(
                        "transitionTargetRef",
                    )
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

                commitEvent(
                    home = home,
                    identity = identity,
                    prior = state.coordinates,
                    eventCore = eventCore,
                    sourceConversationHeadSha256 =
                        resolved.candidate.string(
                            "sourceConversationHeadSha256",
                        ),
                    semanticAuthorityHeadSha256 =
                        resolved.semanticAuthorityHeadSha256,
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

        return acquireWriter(
            home = home,
            identity = identity,
            instanceRef = invocation.instanceRef,
        )
            .use {
                val state =
                    loadState(
                        home = home,
                        identity = identity,
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
                        "SCORE_SOURCE_INVALID: open loop requires current Conversation"
                    }
                require(
                    head.conversationHeadSha256 ==
                        invocation.expectedConversationHeadSha256,
                ) {
                    "SCORE_SOURCE_INVALID: expected Conversation head differs from current Conversation"
                }
                val source =
                    selectSourceEvents(
                        conversation = conversation,
                        requestedHashes =
                            invocation.sourceEventHashes,
                    )
                val formedAt =
                    canonicalUtc(
                        invocation.formedAt,
                    )
                val eventCore =
                    openLoopEventCore(
                        identity = identity,
                        state = state,
                        invocation = invocation,
                        source = source,
                        formedAt = formedAt,
                    )

                commitEvent(
                    home = home,
                    identity = identity,
                    prior = state.coordinates,
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

    private fun statementEventCore(
        identity: Identity,
        state: MutableState,
        invocation: AppendStatementInvocation,
        acceptance: VexLifeCurrentMemoryReader.CurrentScoreAcceptance,
        source: List<DirectConversationStore.Event>,
        formedAt: String,
    ): JsonObject {
        val accepted =
            acceptance.acceptance
        val transition =
            accepted.string(
                "transitionKind",
            )
        val target =
            accepted.nullableString(
                "transitionTargetRef",
            )
        return eventCore(
            identity = identity,
            state = state,
            instanceRef = invocation.instanceRef,
            sourceConversationHeadSha256 =
                acceptance.candidate.string(
                    "sourceConversationHeadSha256",
                ),
            source = source,
            formedAt = formedAt,
        ) {
            put(
                "eventKind",
                "STATEMENT",
            )
            put(
                "statementRef",
                invocation.statementRef,
            )
            put(
                "subjectRef",
                accepted.string(
                    "semanticSubjectRef",
                ),
            )
            put(
                "semanticSubjectFingerprint",
                accepted.string(
                    "semanticSubjectFingerprint",
                ),
            )
            put(
                "memoryRelation",
                accepted.string(
                    "memoryRelation",
                ),
            )
            put(
                "statementState",
                accepted.string(
                    "statementState",
                ),
            )
            put(
                "summary",
                accepted.string(
                    "acceptedSummary",
                ),
            )
            put(
                "summaryHash",
                accepted.string(
                    "acceptedSummarySha256",
                ),
            )
            put(
                "acceptedForContinuity",
                accepted.boolean(
                    "acceptedForContinuity",
                ),
            )
            put(
                "consentState",
                acceptance.consent.string(
                    "disposition",
                ),
            )
            put(
                "transitionKind",
                transition,
            )
            putNullable(
                "correctsStatementRef",
                if (
                    transition ==
                    "CORRECTS"
                ) {
                    target
                } else {
                    null
                },
            )
            putNullable(
                "supersedesStatementRef",
                if (
                    transition ==
                    "SUPERSEDES"
                ) {
                    target
                } else {
                    null
                },
            )
            putNullable(
                "releasesOrTombstonesStatementRef",
                if (
                    transition ==
                    "RELEASES_OR_TOMBSTONES"
                ) {
                    target
                } else {
                    null
                },
            )
            putNullable(
                "transitionTargetAcceptanceSha256",
                accepted.nullableString(
                    "transitionTargetAcceptanceSha256",
                ),
            )
            put(
                "semanticAuthorityHeadSha256",
                acceptance.semanticAuthorityHeadSha256,
            )
            put(
                "semanticAcceptanceRef",
                accepted.string(
                    "acceptanceRef",
                ),
            )
            put(
                "semanticAcceptanceSha256",
                accepted.string(
                    "acceptanceSha256",
                ),
            )
            put(
                "semanticCandidateRef",
                acceptance.candidate.string(
                    "candidateRef",
                ),
            )
            put(
                "semanticCandidateSha256",
                acceptance.candidate.string(
                    "candidateSha256",
                ),
            )
            put(
                "classificationEvidenceBindings",
                acceptance.authorityBinding[
                    "classificationEvidenceBindings"
                ]!!,
            )
            put(
                "consentDispositionRef",
                acceptance.consent.string(
                    "consentDispositionRef",
                ),
            )
            put(
                "consentDispositionSha256",
                acceptance.consent.string(
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
        }
    }

    private fun openLoopEventCore(
        identity: Identity,
        state: MutableState,
        invocation: AppendOpenLoopInvocation,
        source: List<DirectConversationStore.Event>,
        formedAt: String,
    ): JsonObject =
        eventCore(
            identity = identity,
            state = state,
            instanceRef = invocation.instanceRef,
            sourceConversationHeadSha256 =
                invocation.expectedConversationHeadSha256,
            source = source,
            formedAt = formedAt,
        ) {
            put(
                "eventKind",
                "OPEN_LOOP",
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
        }

    private fun eventCore(
        identity: Identity,
        state: MutableState,
        instanceRef: String,
        sourceConversationHeadSha256: String,
        source: List<DirectConversationStore.Event>,
        formedAt: String,
        extras: kotlinx.serialization.json.JsonObjectBuilder.() -> Unit,
    ): JsonObject {
        val bindings =
            source.map(
                ::sourceBinding,
            )
        val observation =
            continuityObservation(
                identity = identity,
                sourceEvents = source,
                instanceRef = instanceRef,
                formedAt = formedAt,
            )
        return buildJsonObject {
            put(
                "schemaVersion",
                ScoreEventSchema,
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
                instanceRef,
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
                sourceConversationHeadSha256,
            )
            put(
                "sourceBindings",
                VexContractJson.Strict.encodeToJsonElement(
                    bindings,
                ),
            )
            put(
                "continuityObservation",
                VexContractJson.Strict.encodeToJsonElement(
                    observation,
                ),
            )
            extras()
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
    }

    private fun commitEvent(
        home: Path,
        identity: Identity,
        prior: VexLifeCurrentMemoryReader.CurrentScoreCoordinates,
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
        val scoreRoot =
            scoreRoot(
                home = home,
                identity = identity,
            )
        val eventPath =
            scoreRoot.resolve(
                "events/" +
                    event.long(
                        "sequence",
                    )
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
                    "threadRef",
                    identity.threadRef,
                )
                put(
                    "eventHash",
                    scoreEventHash,
                )
                put(
                    "sequence",
                    event.long(
                        "sequence",
                    ),
                )
                putStrings(
                    "currentStatementRefs",
                    currentStatementRefs,
                )
                putStrings(
                    "openLoopRefs",
                    openLoopRefs,
                )
                put(
                    "sourceConversationHeadSha256",
                    sourceConversationHeadSha256,
                )
                putNullable(
                    "priorScoreHeadSha256",
                    prior.scoreHeadSha256,
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
                "SCORE_HEAD_MISMATCH: simulated failure before Score head advance",
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
                currentStatementRefs &&
                current.openLoopRefs ==
                openLoopRefs,
        ) {
            "Score current readback differs from committed event/head"
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

    private fun loadState(
        home: Path,
        identity: Identity,
    ): MutableState {
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
            "Score current identity changed while writer lease is held"
        }

        val root =
            scoreRoot(
                home = home,
                identity = identity,
            )
        val headPath =
            root.resolve(
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
        val events =
            scoreEventFiles(
                home = home,
                root = root,
            )
                .map {
                    readObject(
                        home = home,
                        path = it,
                        label = "Score event",
                    )
                }
        val committedCount =
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
                committedCount,
        ) {
            "SCORE_TAIL_ATTENTION: uncommitted Score tail requires source-managed recovery before mutation"
        }

        return MutableState(
            identity = identity,
            coordinates = coordinates,
            head = head,
            events = events,
        )
    }

    private fun validateTransition(
        state: MutableState,
        acceptance: JsonObject,
    ) {
        val kind =
            acceptance.string(
                "transitionKind",
            )
        val target =
            acceptance.nullableString(
                "transitionTargetRef",
            )
        if (
            kind ==
                "NONE"
        ) {
            require(
                target ==
                    null,
            ) {
                "SCORE_LINK_INVALID: non-transition acceptance carries a target"
            }
            return
        }
        val requiredTarget =
            requireNotNull(
                target,
            ) {
                "SCORE_LINK_INVALID: transition acceptance has no predecessor"
            }
        require(
            requiredTarget in
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
                            requiredTarget
                    },
            ) {
                "SCORE_LINK_INVALID: transition predecessor Score event is absent"
            }
        require(
            prior.string(
                "semanticSubjectFingerprint",
            ) ==
                acceptance.string(
                    "semanticSubjectFingerprint",
                ) &&
                prior.string(
                    "memoryRelation",
                ) ==
                acceptance.string(
                    "memoryRelation",
                ) &&
                prior.string(
                    "semanticAcceptanceSha256",
                ) ==
                acceptance.string(
                    "transitionTargetAcceptanceSha256",
                ),
        ) {
            "SCORE_LINK_INVALID: transition does not bind the exact current predecessor"
        }
    }

    private fun validateCandidateSource(
        candidate: JsonObject,
        conversation: DirectConversationStore.Snapshot,
    ): List<DirectConversationStore.Event> {
        val byHash =
            conversation.canonicalEvents
                .associateBy(
                    DirectConversationStore.Event::eventHash,
                )
        val source =
            candidate.objectList(
                "sourceBindings",
            )
                .map {
                    binding ->
                    val observed =
                        requireNotNull(
                            byHash[
                                binding.string(
                                    "eventHash",
                                )
                            ],
                        ) {
                            "SCORE_SOURCE_INVALID: semantic candidate source is absent from canonical Conversation"
                        }
                    require(
                        sourceBinding(
                            observed,
                        ) ==
                            SourceBinding(
                                eventRef =
                                    binding.string(
                                        "eventRef",
                                    ),
                                eventHash =
                                    binding.string(
                                        "eventHash",
                                    ),
                                eventKind =
                                    binding.string(
                                        "eventKind",
                                    ),
                                sequence =
                                    binding.long(
                                        "sequence",
                                    ),
                                turnRef =
                                    binding.string(
                                        "turnRef",
                                    ),
                                messageRef =
                                    binding.string(
                                        "messageRef",
                                    ),
                                contentHash =
                                    binding.string(
                                        "contentHash",
                                    ),
                            ),
                    ) {
                        "SCORE_SOURCE_INVALID: semantic candidate substitutes canonical Conversation source"
                    }
                    observed
                }
        validatePairs(
            source,
        )
        return source
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
            "Score source selection requires completed Conversation pairs"
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
                "source event hash",
            )
        }
        val byHash =
            canonical.associateBy(
                DirectConversationStore.Event::eventHash,
            )
        val source =
            hashes.map {
                hash ->
                requireNotNull(
                    byHash[
                        hash
                    ],
                ) {
                    "Score source selection is outside canonical Conversation"
                }
            }
        validatePairs(
            source,
        )
        return source
    }

    private fun validatePairs(
        source: List<DirectConversationStore.Event>,
    ) {
        source.chunked(
            2,
        )
            .forEach {
                pair ->
                require(
                    pair.size ==
                        2 &&
                        pair[
                            0
                        ].eventKind ==
                        VexLifeConversationContract.RequestKind &&
                        pair[
                            1
                        ].eventKind ==
                        VexLifeConversationContract.ResponseKind &&
                        pair[
                            1
                        ].sequence ==
                        pair[
                            0
                        ].sequence +
                        1 &&
                        pair[
                            1
                        ].priorEventHash ==
                        pair[
                            0
                        ].eventHash &&
                        pair[
                            1
                        ].turnRef ==
                        pair[
                            0
                        ].turnRef,
                ) {
                    "Score source selection is not exact completed Conversation pairs"
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
                            VexLifeContinuityObservation.ConversationEventFact(
                                eventRef =
                                    it.eventRef,
                                eventHash =
                                    it.eventHash,
                                speakerRef =
                                    it.speakerRef,
                                recipientRefs =
                                    it.recipientRefs,
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

    private fun currentIdentity(
        home: Path,
    ): Identity {
        val current =
            requireNotNull(
                VexLifeProdHomeFiles.observeExisting(
                    home,
                ),
            ) {
                "Score continuity requires one current canonical VexLife Home"
            }
        return Identity(
            homeRef =
                current.manifest.homeRef,
            deviceRef =
                current.manifest.currentDeviceRef,
            lineageRef =
                current.manifest.currentCompanionLineageRef,
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

    private fun writerLock(
        home: Path,
        identity: Identity,
    ): Path =
        VexOwnedPath.resolveInside(
            home,
            "runtime/score-writer-locks/" +
                identity.lineageRef +
                "/" +
                identity.threadRef +
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
                identity = identity,
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
                    "SCORE_WRITER_RECOVERY_REQUIRED: prior Score writer is absent; source-managed recovery is required",
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

    private fun scoreEventFiles(
        home: Path,
        root: Path,
    ): List<Path> {
        val directory =
            root.resolve(
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

    private fun validateCandidateInput(
        input: CandidateInput,
    ) {
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
    }

    private fun validateStatementInvocation(
        value: AppendStatementInvocation,
    ) {
        requirePortableRef(
            value.instanceRef,
            "instanceRef",
        )
        requirePortableRef(
            value.statementRef,
            "statementRef",
        )
        requirePortableRef(
            value.semanticAcceptanceRef,
            "semanticAcceptanceRef",
        )
        requireSha256(
            value.semanticAcceptanceSha256,
            "semanticAcceptanceSha256",
        )
        value.expectedScoreHeadSha256
            ?.let {
                requireSha256(
                    it,
                    "expectedScoreHeadSha256",
                )
            }
    }

    private fun validateOpenLoopInvocation(
        value: AppendOpenLoopInvocation,
    ) {
        requirePortableRef(
            value.instanceRef,
            "instanceRef",
        )
        requirePortableRef(
            value.openLoopRef,
            "openLoopRef",
        )
        require(
            value.sourceStatementRefs.isNotEmpty(),
        ) {
            "OPEN_LOOP_INVALID: open loop requires source statement refs"
        }
        value.sourceStatementRefs
            .forEach {
                requirePortableRef(
                    it,
                    "sourceStatementRef",
                )
            }
        value.expectedScoreHeadSha256
            ?.let {
                requireSha256(
                    it,
                    "expectedScoreHeadSha256",
                )
            }
        requireSha256(
            value.expectedConversationHeadSha256,
            "expectedConversationHeadSha256",
        )
        value.summaryRef
            ?.let {
                requirePortableRef(
                    it,
                    "summaryRef",
                )
            }
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
                values
                    .distinct()
                    .sorted()
                    .map(
                        ::JsonPrimitive,
                    ),
            ),
        )
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
}

// [VXG RealForever]
