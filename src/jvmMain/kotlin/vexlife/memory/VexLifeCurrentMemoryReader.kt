package vexlife.memory

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.security.MessageDigest
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import vexlife.companion.DirectPromptContext
import vexlife.continuity.VexLifeContinuityObservation
import vexlife.contract.VexContractJson
import vexlife.contract.VexDurableContractFile
import vexlife.conversation.DirectConversationIdentity
import vexlife.conversation.DirectConversationStore
import vexlife.conversation.VexLifeConversationContract
import vexlife.filesystem.VexOwnedPath
import vexlife.home.VexLifeProdHomeFiles

/**
 * Read-only owner for current source-attributed Memory admitted to a Companion prompt.
 *
 * It reads the existing canonical VexLife Home. It does not append Score, commit Daily Dream,
 * create semantic acceptance, mutate Memory, or grant first-person authority.
 */
object VexLifeCurrentMemoryReader {
    const val LivingJournalMemoryOwnerRef =
        "github.issue.vextreme-sdk.225"

    const val LivingJournalDailyRecordOwnerRef =
        "github.issue.vextreme-sdk.416"

    private const val ScoreEventSchema =
        "vexlife.score-context-event/v1"
    private const val ScoreHeadSchema =
        "vexlife.score-context-head/v1"
    private const val ScoreSharedDisposition =
        "github.issue.vextreme-sdk.350.comment.5215288414"
    private const val LiveSemanticContract =
        "contract.multivex.score.live-semantic-acceptance.v1"
    private const val LiveSemanticDisposition =
        "github.issue.vextreme-sdk.350.comment.5216924433"
    private const val LiveSemanticExecutableAddendum =
        "github.issue.vextreme-sdk.350.comment.5217097749"
    private const val SemanticAuthorityHeadSchema =
        "vextreme.score-semantic-authority-head/v1"

    private val positiveConsent =
        setOf(
            "PERMITTED",
            "NARROWED",
        )

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

    private val consentStates =
        setOf(
            "PERMITTED",
            "NARROWED",
            "DEFERRED",
            "DENIED",
            "UNKNOWN",
            "WITHDRAWN",
        )

    private data class Identity(
        val homeRef: String,
        val deviceRef: String,
        val lineageRef: String,
        val threadRef: String,
    )

    private data class Statement(
        val statementRef: String,
        val semanticSubjectFingerprint: String,
        val memoryRelation: String,
        val recordedState: String,
        var effectiveState: String,
        val summary: String,
        val summaryHash: String,
        val sourceBindings: JsonArray,
        val sourceConversationHeadSha256: String,
        val acceptedForContinuity: Boolean,
        val consentState: String,
        val semanticAuthorityHeadSha256: String,
        val acceptanceRef: String,
        val acceptanceSha256: String,
        val candidateRef: String,
        val candidateSha256: String,
        var current: Boolean,
    )

    private data class OpenLoop(
        val ref: String,
        val state: String,
    )

    private data class ScoreSnapshot(
        val head: JsonObject,
        val authority: JsonObject,
        val statements: List<Statement>,
        val openLoops: List<OpenLoop>,
    )

    private data class DailySnapshot(
        val stratum: JsonObject,
        val sourceConversationHeadSha256: String,
        val sourceScoreHeadSha256: String,
        val sourceSemanticAuthorityHeadSha256: String,
        val activeStatementRefs: List<String>,
        val heldStatementRefs: List<String>,
    )

    @Serializable
    private data class PageIdentity(
        val dailyStratumSha256: String,
        val statementRef: String,
    )

    @Serializable
    private data class ProviderPacket(
        val schemaVersion: String,
        val truthClass: String,
        val projectionState: String,
        val projectionCurrentness: String,
        val sourceOwnerRefs: List<String>,
        val currentDailyStratumRef: String?,
        val currentDailyStratumSha256: String?,
        val sourceConversationHeadSha256: String?,
        val sourceScoreHeadSha256: String?,
        val sourceSemanticAuthorityHeadSha256: String?,
        val pages: List<VexLifeMemoryContract.Page>,
        val heldOrDeferredStatementRefs: List<String>,
        val boundedOutStatementRefs: List<String>,
        val tokenBoundedOutPageRefs: List<String>,
        val reasons: List<String>,
        val memoryTokenCap: Int,
    )

    internal fun promptFrontierCurrentness(
        sourceConversationHeadSha256: String,
        currentConversationHeadSha256: String,
    ): String {
        requireSha256(
            sourceConversationHeadSha256,
            "sourceConversationHeadSha256",
        )
        requireSha256(
            currentConversationHeadSha256,
            "currentConversationHeadSha256",
        )
        return if (
            sourceConversationHeadSha256 ==
            currentConversationHeadSha256
        ) {
            "CURRENT"
        } else {
            "HELD"
        }
    }

    fun readCurrent(
        productHome: Path,
        threadRef: String =
            DirectConversationIdentity.ThreadRef,
    ): VexLifeMemoryContract.Context {
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
                "Memory reader requires one current canonical VexLife Home"
            }
        val identity =
            Identity(
                homeRef = currentHome.manifest.homeRef,
                deviceRef = currentHome.manifest.currentDeviceRef,
                lineageRef = currentHome.manifest.currentCompanionLineageRef,
                threadRef = threadRef,
            )

        val dailyHead =
            inside(
                home,
                "daily-memory-dream/" +
                    identity.lineageRef +
                    "/" +
                    identity.threadRef +
                    "/head.json",
            )
        if (
            !Files.exists(
                dailyHead,
                LinkOption.NOFOLLOW_LINKS,
            )
        ) {
            return held(
                listOf(
                    "NO_COMMITTED_DAILY_MEMORY",
                ),
            )
        }

        val conversationStore =
            DirectConversationStore(
                home,
            )
        val conversation =
            conversationStore.current()
        val conversationHead =
            requireNotNull(
                conversation.head,
            ) {
                "committed Daily Memory requires one current lived Conversation head"
            }

        val score =
            loadScore(
                home = home,
                identity = identity,
                conversationStore = conversationStore,
                conversation = conversation,
            )
        val daily =
            loadDaily(
                home = home,
                identity = identity,
                score = score,
                conversationStore = conversationStore,
            )

        if (
            promptFrontierCurrentness(
                sourceConversationHeadSha256 =
                    daily.sourceConversationHeadSha256,
                currentConversationHeadSha256 =
                    conversationHead.conversationHeadSha256,
            ) ==
            "HELD"
        ) {
            return held(
                listOf(
                    "DAILY_MEMORY_SOURCE_FRONTIER_STALE",
                ),
            )
        }

        return formContext(
            score = score,
            daily = daily,
        )
    }

    private fun loadScore(
        home: Path,
        identity: Identity,
        conversationStore: DirectConversationStore,
        conversation: DirectConversationStore.Snapshot,
    ): ScoreSnapshot {
        val root =
            "score/" +
                identity.lineageRef +
                "/" +
                identity.threadRef
        val head =
            readObject(
                home,
                inside(
                    home,
                    root +
                        "/head.json",
                ),
                "current Score head",
            )
        validateScoreHead(
            head,
            identity,
        )
        val scoreSourceConversationHead =
            head.string(
                "sourceConversationHeadSha256",
            )
        val historicalScoreSource =
            conversationStore.historicalCompletedTurn(
                scoreSourceConversationHead,
            )
        require(
            historicalScoreSource.currentCommittedLineageVerified,
        ) {
            "current Score source Conversation head is not in the current committed lineage"
        }

        val scoreHeadSha =
            head.string(
                "scoreHeadSha256",
            )
        val immutable =
            readObject(
                home,
                inside(
                    home,
                    root +
                        "/heads/" +
                        scoreHeadSha +
                        ".json",
                ),
                "immutable Score head",
            )
        validateScoreHead(
            immutable,
            identity,
        )
        require(
            immutable ==
                head,
        ) {
            "current Score head lacks its exact immutable receipt"
        }

        val eventsRoot =
            inside(
                home,
                root +
                    "/events",
            )
        requireRealDirectory(
            eventsRoot,
            "Score events",
        )
        val allEvents =
            Files.list(
                eventsRoot,
            )
                .use {
                    it.toList()
                }
                .sortedBy {
                    it.fileName
                        .toString()
                }
                .map {
                    file ->
                    requireRegularFile(
                        file,
                        "Score event",
                    )
                    val event =
                        readObject(
                            home,
                            file,
                            "Score event",
                        )
                    validateScoreEvent(
                        event = event,
                        identity = identity,
                        filename =
                            file.fileName
                                .toString(),
                        conversationStore = conversationStore,
                        conversation = conversation,
                    )
                    event
                }

        val byHash =
            allEvents.associateBy {
                it.string(
                    "scoreEventHash",
                )
            }
        require(
            byHash.size ==
                allEvents.size,
        ) {
            "Score contains duplicate event hashes"
        }

        val reverse =
            mutableListOf<JsonObject>()
        val visited =
            mutableSetOf<String>()
        var cursor: String? =
            head.string(
                "eventHash",
            )
        while (
            cursor !=
                null
        ) {
            require(
                visited.add(
                    cursor,
                ),
            ) {
                "Score event chain contains a cycle"
            }
            val event =
                requireNotNull(
                    byHash[
                        cursor
                    ],
                ) {
                    "Score head references a missing event"
                }
            reverse +=
                event
            cursor =
                event.nullableString(
                    "priorScoreEventHash",
                )
        }
        val chain =
            reverse.reversed()
        require(
            chain.isNotEmpty() &&
                chain.size.toLong() ==
                head.long(
                    "sequence",
                ) +
                1,
        ) {
            "Score committed chain length does not match current head"
        }
        chain.forEachIndexed {
            index,
            event,
            ->
            require(
                event.long(
                    "sequence",
                ) ==
                    index.toLong() &&
                    event.nullableString(
                        "priorScoreEventHash",
                    ) ==
                    if (
                        index ==
                            0
                    ) {
                        null
                    } else {
                        chain[
                            index -
                                1
                        ]
                            .string(
                                "scoreEventHash",
                            )
                    },
            ) {
                "Score event chain is not contiguous"
            }
        }

        val statements =
            linkedMapOf<String, Statement>()
        val loops =
            linkedMapOf<String, OpenLoop>()
        for (
            event in
            chain
        ) {
            when (
                event.string(
                    "eventKind",
                )
            ) {
                "STATEMENT" ->
                    applyStatement(
                        statements,
                        event,
                    )

                "OPEN_LOOP" -> {
                    val ref =
                        event.string(
                            "openLoopRef",
                        )
                    require(
                        !loops.containsKey(
                            ref,
                        ) &&
                            event.string(
                                "openLoopState",
                            ) ==
                            "OPEN",
                    ) {
                        "Score open-loop history is invalid"
                    }
                    loops[
                        ref
                    ] =
                        OpenLoop(
                            ref = ref,
                            state = "OPEN",
                        )
                }

                else ->
                    error(
                        "Score event kind is not admitted",
                    )
            }
        }

        val currentStatementRefs =
            statements.values
                .filter(
                    Statement::current,
                )
                .map(
                    Statement::statementRef,
                )
                .sorted()
        val openLoopRefs =
            loops.values
                .filter {
                    it.state ==
                        "OPEN"
                }
                .map(
                    OpenLoop::ref,
                )
                .sorted()
        require(
            head.stringList(
                "currentStatementRefs",
            ) ==
                currentStatementRefs &&
                head.stringList(
                    "openLoopRefs",
                ) ==
                openLoopRefs,
        ) {
            "Score head projection differs from replayed committed state"
        }

        val reachable =
            chain.map {
                it.string(
                    "scoreEventHash",
                )
            }
                .toSet()
        val tail =
            allEvents.filter {
                it.string(
                    "scoreEventHash",
                ) !in
                    reachable
            }
        require(
            tail.size <=
                1,
        ) {
            "multiple competing Score tails require attention before Memory use"
        }
        tail.singleOrNull()
            ?.let {
                require(
                    it.long(
                        "sequence",
                    ) ==
                        head.long(
                            "sequence",
                        ) +
                        1 &&
                        it.nullableString(
                            "priorScoreEventHash",
                        ) ==
                        head.string(
                            "eventHash",
                        ),
                ) {
                    "uncommitted Score tail is reordered or readdressed"
                }
            }

        val authority =
            loadAuthority(
                home,
                identity,
            )
        for (
            statement in
            statements.values
        ) {
            val statementAuthority =
                loadHistoricalAuthority(
                    home = home,
                    identity = identity,
                    semanticAuthorityHeadSha256 = statement.semanticAuthorityHeadSha256,
                )
            validateStatementAuthority(
                home = home,
                identity = identity,
                statement = statement,
                authority = statementAuthority,
            )

            if (
                statement.current
            ) {
                require(
                    authorityContains(
                        authority,
                        statement,
                    )
                ) {
                    "Score statement is absent from its admitted semantic authority head"
                }
            }
        }

        return ScoreSnapshot(
            head = head,
            authority = authority,
            statements = statements.values.toList(),
            openLoops = loops.values.toList(),
        )
    }

    private fun loadAuthority(
        home: Path,
        identity: Identity,
    ): JsonObject {
        val root =
            "semantic-authority/score/" +
                identity.lineageRef +
                "/" +
                identity.threadRef
        val current =
            readAddressed(
                home = home,
                path =
                    inside(
                        home,
                        root +
                            "/head.json",
                    ),
                schemaVersion = SemanticAuthorityHeadSchema,
                refField = "semanticAuthorityHeadRef",
                hashField = "semanticAuthorityHeadSha256",
                prefix = "score-semantic-authority-head",
                label = "current semantic authority head",
            )
        validateAuthorityIdentity(
            current,
            identity,
        )

        val sha =
            current.string(
                "semanticAuthorityHeadSha256",
            )
        val immutable =
            readAddressed(
                home = home,
                path =
                    inside(
                        home,
                        root +
                            "/heads/" +
                            sha +
                            ".json",
                    ),
                schemaVersion = SemanticAuthorityHeadSchema,
                refField = "semanticAuthorityHeadRef",
                hashField = "semanticAuthorityHeadSha256",
                prefix = "score-semantic-authority-head",
                label = "immutable semantic authority head",
            )
        validateAuthorityIdentity(
            immutable,
            identity,
        )
        require(
            immutable ==
                current,
        ) {
            "current semantic authority head lacks its immutable receipt"
        }

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
            val prior =
                cursor.nullableString(
                    "priorSemanticAuthorityHeadSha256",
                )
            if (
                sequence ==
                    0L
            ) {
                require(
                    prior ==
                        null,
                ) {
                    "semantic authority genesis head has a predecessor"
                }
                break
            }
            val priorSha =
                requireNotNull(
                    prior,
                ) {
                    "semantic authority non-genesis head has no predecessor"
                }
            requireSha256(
                priorSha,
                "priorSemanticAuthorityHeadSha256",
            )
            val previous =
                readAddressed(
                    home = home,
                    path =
                        inside(
                            home,
                            root +
                                "/heads/" +
                                priorSha +
                                ".json",
                        ),
                    schemaVersion = SemanticAuthorityHeadSchema,
                    refField = "semanticAuthorityHeadRef",
                    hashField = "semanticAuthorityHeadSha256",
                    prefix = "score-semantic-authority-head",
                    label = "prior semantic authority head",
                )
            validateAuthorityIdentity(
                previous,
                identity,
            )
            require(
                previous.long(
                    "sequence",
                ) ==
                    sequence -
                    1,
            ) {
                "semantic authority head sequence is not contiguous"
            }
            cursor =
                previous
        }

        return current
    }

    private fun loadHistoricalAuthority(
        home: Path,
        identity: Identity,
        semanticAuthorityHeadSha256: String,
    ): JsonObject {
        requireSha256(
            semanticAuthorityHeadSha256,
            "semanticAuthorityHeadSha256",
        )
        val root =
            "semantic-authority/score/" +
                identity.lineageRef +
                "/" +
                identity.threadRef
        val head =
            readAddressed(
                home = home,
                path =
                    inside(
                        home,
                        root +
                            "/heads/" +
                            semanticAuthorityHeadSha256 +
                            ".json",
                    ),
                schemaVersion = SemanticAuthorityHeadSchema,
                refField = "semanticAuthorityHeadRef",
                hashField = "semanticAuthorityHeadSha256",
                prefix = "score-semantic-authority-head",
                label = "historical semantic authority head",
            )
        validateAuthorityIdentity(
            head,
            identity,
        )
        require(
            head.string(
                "semanticAuthorityHeadSha256",
            ) ==
                semanticAuthorityHeadSha256,
        ) {
            "historical semantic authority head is stored under the wrong identity"
        }
        return head
    }

    private fun validateStatementAuthority(
        home: Path,
        identity: Identity,
        statement: Statement,
        authority: JsonObject,
    ) {
        val binding =
            authority
                .objectList(
                    "currentAcceptanceBindings",
                )
                .singleOrNull {
                    it.string(
                        "acceptanceRef",
                    ) ==
                        statement.acceptanceRef &&
                        it.string(
                            "acceptanceSha256",
                        ) ==
                        statement.acceptanceSha256
                }
        requireNotNull(
            binding,
        ) {
            "current Score statement is absent from current semantic authority"
        }
        require(
            binding.string(
                "candidateRef",
            ) ==
                statement.candidateRef &&
                binding.string(
                    "candidateSha256",
                ) ==
                statement.candidateSha256,
        ) {
            "current Score statement candidate differs from authority binding"
        }

        val root =
            "semantic-authority/score/" +
                identity.lineageRef +
                "/" +
                identity.threadRef
        val candidate =
            readAddressed(
                home = home,
                path =
                    inside(
                        home,
                        root +
                            "/candidates/" +
                            statement.candidateSha256 +
                            ".json",
                    ),
                schemaVersion = "vextreme.score-semantic-candidate/v1",
                refField = "candidateRef",
                hashField = "candidateSha256",
                prefix = "score-semantic-candidate",
                label = "semantic candidate",
            )
        require(
            candidate.string(
                "candidateRef",
            ) ==
                statement.candidateRef &&
                candidate.string(
                    "semanticSubjectFingerprint",
                ) ==
                statement.semanticSubjectFingerprint &&
                candidate.string(
                    "sourceLineageRef",
                ) ==
                identity.lineageRef &&
                candidate.string(
                    "sourceThreadRef",
                ) ==
                identity.threadRef &&
                candidate.string(
                    "sourceConversationHeadSha256",
                ) ==
                statement.sourceConversationHeadSha256 &&
                candidate.string(
                    "proposedSummary",
                ) ==
                statement.summary &&
                candidate.string(
                    "proposedSummarySha256",
                ) ==
                statement.summaryHash &&
                candidate.string(
                    "proposedMemoryRelation",
                ) ==
                statement.memoryRelation &&
                candidate.string(
                    "proposedStatementState",
                ) ==
                statement.recordedState,
        ) {
            "semantic candidate differs from current Score statement"
        }

        val consent =
            readAddressed(
                home = home,
                path =
                    inside(
                        home,
                        root +
                            "/consents/" +
                            binding.string(
                                "consentDispositionSha256",
                            ) +
                            ".json",
                    ),
                schemaVersion = "vextreme.score-consent-disposition/v1",
                refField = "consentDispositionRef",
                hashField = "consentDispositionSha256",
                prefix = "score-consent-disposition",
                label = "consent disposition",
            )
        require(
            consent.string(
                "candidateRef",
            ) ==
                statement.candidateRef &&
                consent.string(
                    "candidateSha256",
                ) ==
                statement.candidateSha256 &&
                consent.string(
                    "semanticSubjectFingerprint",
                ) ==
                statement.semanticSubjectFingerprint &&
                consent.string(
                    "disposition",
                ) ==
                statement.consentState,
        ) {
            "consent disposition differs from current Score statement"
        }

        val evidenceBindings =
            binding.objectList(
                "classificationEvidenceBindings",
            )
        require(
            evidenceBindings.isNotEmpty(),
        ) {
            "current authority binding has no classification evidence"
        }
        for (
            evidenceBinding in
            evidenceBindings
        ) {
            val evidence =
                readAddressed(
                    home = home,
                    path =
                        inside(
                            home,
                            root +
                                "/classification-evidence/" +
                                evidenceBinding.string(
                                    "classificationEvidenceSha256",
                                ) +
                                ".json",
                        ),
                    schemaVersion = "vextreme.score-classification-evidence/v1",
                    refField = "classificationEvidenceRef",
                    hashField = "classificationEvidenceSha256",
                    prefix = "score-classification-evidence",
                    label = "classification evidence",
                )
            require(
                evidence.string(
                    "classificationEvidenceRef",
                ) ==
                    evidenceBinding.string(
                        "classificationEvidenceRef",
                    ) &&
                    evidence.string(
                        "candidateRef",
                    ) ==
                    statement.candidateRef &&
                    evidence.string(
                        "candidateSha256",
                    ) ==
                    statement.candidateSha256 &&
                    evidence.string(
                        "semanticSubjectFingerprint",
                    ) ==
                    statement.semanticSubjectFingerprint &&
                    evidence.string(
                        "assertedMemoryRelation",
                    ) ==
                    statement.memoryRelation &&
                    evidence.string(
                        "assertedStatementState",
                    ) ==
                    statement.recordedState &&
                    evidence.string(
                        "assertedSummarySha256",
                    ) ==
                    statement.summaryHash,
            ) {
                "classification evidence does not authorize current Score statement"
            }
        }

        val acceptance =
            readAddressed(
                home = home,
                path =
                    inside(
                        home,
                        root +
                            "/acceptances/" +
                            statement.acceptanceSha256 +
                            ".json",
                    ),
                schemaVersion = "vextreme.score-semantic-acceptance/v1",
                refField = "acceptanceRef",
                hashField = "acceptanceSha256",
                prefix = "score-semantic-acceptance",
                label = "semantic acceptance",
            )
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
                    "acceptanceRef",
                ) ==
                statement.acceptanceRef &&
                acceptance.string(
                    "candidateRef",
                ) ==
                statement.candidateRef &&
                acceptance.string(
                    "candidateSha256",
                ) ==
                statement.candidateSha256 &&
                acceptance.string(
                    "acceptedSummary",
                ) ==
                statement.summary &&
                acceptance.string(
                    "acceptedSummarySha256",
                ) ==
                statement.summaryHash &&
                acceptance.string(
                    "memoryRelation",
                ) ==
                statement.memoryRelation &&
                acceptance.string(
                    "statementState",
                ) ==
                statement.recordedState &&
                acceptance.boolean(
                    "acceptedForContinuity",
                ) ==
                statement.acceptedForContinuity &&
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
                acceptance.objectList(
                    "classificationEvidenceBindings",
                ) ==
                evidenceBindings,
        ) {
            "semantic acceptance does not exactly authorize current Score statement"
        }

        if (
            statement.acceptedForContinuity
        ) {
            require(
                consent.string(
                    "disposition",
                ) in
                    positiveConsent &&
                    consent.stringList(
                        "permittedUseRefs",
                    )
                        .contains(
                            "use.score.device-private-continuity",
                        ) &&
                    !consent.stringList(
                        "prohibitedUseRefs",
                    )
                        .contains(
                            "use.score.device-private-continuity",
                        ),
            ) {
                "accepted current statement lacks positive continuity consent"
            }
        }
    }

    private fun validateScoreHead(
        head: JsonObject,
        identity: Identity,
    ) {
        require(
            head.string(
                "schemaVersion",
            ) ==
                ScoreHeadSchema &&
                head.string(
                    "homeRef",
                ) ==
                identity.homeRef &&
                head.string(
                    "deviceRef",
                ) ==
                identity.deviceRef &&
                head.string(
                    "companionLineageRef",
                ) ==
                identity.lineageRef &&
                head.string(
                    "threadRef",
                ) ==
                identity.threadRef &&
                head.string(
                    "sharedSemanticDispositionRef",
                ) ==
                ScoreSharedDisposition &&
                head.string(
                    "liveSemanticContractRef",
                ) ==
                LiveSemanticContract &&
                head.string(
                    "liveSemanticDispositionRef",
                ) ==
                LiveSemanticDisposition &&
                head.string(
                    "liveSemanticExecutableAddendumRef",
                ) ==
                LiveSemanticExecutableAddendum &&
                head.long(
                    "sequence",
                ) >=
                0 &&
                hashWithout(
                    head,
                    "scoreHeadSha256",
                ) ==
                head.string(
                    "scoreHeadSha256",
                ),
        ) {
            "Score head content/address/identity is invalid"
        }
        requireSha256(
            head.string(
                "eventHash",
            ),
            "Score head eventHash",
        )
        requireSha256(
            head.string(
                "sourceConversationHeadSha256",
            ),
            "Score source Conversation head",
        )
        head.nullableString(
            "priorScoreHeadSha256",
        )
            ?.let {
                requireSha256(
                    it,
                    "prior Score head",
                )
            }
    }

    private fun validateScoreEvent(
        event: JsonObject,
        identity: Identity,
        filename: String,
        conversationStore: DirectConversationStore,
        conversation: DirectConversationStore.Snapshot,
    ) {
        require(
            event.string(
                "schemaVersion",
            ) ==
                ScoreEventSchema &&
                event.string(
                    "homeRef",
                ) ==
                identity.homeRef &&
                event.string(
                    "deviceRef",
                ) ==
                identity.deviceRef &&
                event.string(
                    "companionLineageRef",
                ) ==
                identity.lineageRef &&
                event.string(
                    "threadRef",
                ) ==
                identity.threadRef &&
                event.string(
                    "sharedSemanticDispositionRef",
                ) ==
                ScoreSharedDisposition &&
                event.string(
                    "privacyClass",
                ) ==
                "DEVICE_PRIVATE" &&
                !event.boolean(
                    "rawSourceContentIncluded",
                ) &&
                event.long(
                    "sequence",
                ) >=
                0 &&
                hashWithout(
                    event,
                    "scoreEventHash",
                ) ==
                event.string(
                    "scoreEventHash",
                ),
        ) {
            "Score event content/address/identity is invalid"
        }
        val hash =
            event.string(
                "scoreEventHash",
            )
        require(
            filename ==
                event.long(
                    "sequence",
                )
                    .toString()
                    .padStart(
                        8,
                        '0',
                    ) +
                "-" +
                hash +
                ".json",
        ) {
            "Score event filename is not content-addressed"
        }
        requireSha256(
            event.string(
                "sourceConversationHeadSha256",
            ),
            "Score event source Conversation head",
        )
        event.nullableString(
            "priorScoreEventHash",
        )
            ?.let {
                requireSha256(
                    it,
                    "Score prior event hash",
                )
            }
        require(
            event.array(
                "sourceBindings",
            )
                .isNotEmpty(),
        ) {
            "Score event lacks source bindings"
        }

        when (
            event.string(
                "eventKind",
            )
        ) {
            "STATEMENT" -> {
                require(
                    event.string(
                        "memoryRelation",
                    ) in
                        memoryRelations &&
                        event.string(
                            "statementState",
                        ) in
                        statementStates &&
                        event.string(
                            "consentState",
                        ) in
                        consentStates &&
                        semanticHashString(
                            event.string(
                                "summary",
                            ),
                        ) ==
                        event.string(
                            "summaryHash",
                        ),
                ) {
                    "Score statement semantic projection is invalid"
                }
                for (
                    field in
                    listOf(
                        "semanticSubjectFingerprint",
                        "semanticAuthorityHeadSha256",
                        "semanticAcceptanceSha256",
                        "semanticCandidateSha256",
                        "consentDispositionSha256",
                    )
                ) {
                    requireSha256(
                        event.string(
                            field,
                        ),
                        field,
                    )
                }
            }

            "OPEN_LOOP" ->
                require(
                    event.string(
                        "openLoopState",
                    ) ==
                        "OPEN" &&
                        event.stringList(
                            "sourceStatementRefs",
                        )
                            .isNotEmpty(),
                ) {
                    "Score open-loop event is invalid"
                }

            else ->
                error(
                    "Score event kind is not admitted",
                )
        }

        validateScoreSource(
            event = event,
            identity = identity,
            conversationStore = conversationStore,
            conversation = conversation,
        )
    }

    private fun validateScoreSource(
        event: JsonObject,
        identity: Identity,
        conversationStore: DirectConversationStore,
        conversation: DirectConversationStore.Snapshot,
    ) {
        val sourceHeadSha =
            event.string(
                "sourceConversationHeadSha256",
            )
        val historical =
            conversationStore.historicalCompletedTurn(
                sourceHeadSha,
            )
        require(
            historical.currentCommittedLineageVerified,
        ) {
            "Score source Conversation head is not in the current committed lineage"
        }

        val committedByHash =
            conversation.canonicalEvents
                .filter {
                    it.sequence <=
                        historical.head.sequence
                }
                .associateBy(
                    DirectConversationStore.Event::eventHash,
                )

        val sourceEvents =
            event.objectList(
                "sourceBindings",
            )
                .map {
                    binding ->
                    require(
                        binding.string(
                            "eventKind",
                        ) in
                            setOf(
                                VexLifeConversationContract.RequestKind,
                                VexLifeConversationContract.ResponseKind,
                            ) &&
                            binding.long(
                                "sequence",
                            ) >=
                            0,
                    ) {
                        "Score source binding is malformed"
                    }
                    val eventHash =
                        binding.string(
                            "eventHash",
                        )
                    requireSha256(
                        eventHash,
                        "Score source binding eventHash",
                    )
                    requireSha256(
                        binding.string(
                            "contentHash",
                        ),
                        "Score source binding contentHash",
                    )
                    val observed =
                        requireNotNull(
                            committedByHash[
                                eventHash
                            ],
                        ) {
                            "Score source binding is absent from the committed Conversation chain"
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
                        "Score source binding substitutes committed Conversation event identity"
                    }
                    observed
                }

        require(
            sourceEvents.isNotEmpty() &&
                sourceEvents.size %
                2 ==
                0,
        ) {
            "Score source intake requires complete committed request/response pairs"
        }
        sourceEvents.chunked(
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
                    "Score source bindings do not form exact committed Conversation pairs"
                }
            }

        val core =
            VexLifeContinuityObservation.conversationEpisodeRangeCore(
                sourceLineageRef = identity.lineageRef,
                sourceEvents =
                    sourceEvents.map {
                        source ->
                        VexLifeContinuityObservation.ConversationEventFact(
                            eventRef = source.eventRef,
                            eventHash = source.eventHash,
                            speakerRef = source.speakerRef,
                            recipientRefs = source.recipientRefs,
                        )
                    },
                threadRef = identity.threadRef,
                formedByRef =
                    event.string(
                        "instanceRef",
                    ),
                formedAt =
                    event.string(
                        "formedAt",
                    ),
            )
        val fingerprint =
            sha256(
                VexContractJson.canonicalEncode(
                    core,
                ),
            )
        val expected =
            VexLifeContinuityObservation.observationFromCore(
                core = core,
                observationRef =
                    "continuity-observation." +
                        fingerprint.take(
                            24,
                        ),
                semanticFingerprint = fingerprint,
            )
        val observed =
            VexContractJson.Strict
                .decodeFromJsonElement<VexLifeContinuityObservation.Observation>(
                    event.element(
                        "continuityObservation",
                    ),
                )
        require(
            observed ==
                expected,
        ) {
            "Score continuity observation is not exact to its committed Conversation source"
        }
    }

    private fun applyStatement(
        statements: MutableMap<String, Statement>,
        event: JsonObject,
    ) {
        val ref =
            event.string(
                "statementRef",
            )
        require(
            !statements.containsKey(
                ref,
            ),
        ) {
            "Score statementRef is duplicated"
        }

        fun retire(
            targetField: String,
            effective: String,
        ) {
            val target =
                requireNotNull(
                    event.nullableString(
                        targetField,
                    ),
                ) {
                    "Score transition has no predecessor"
                }
            val prior =
                requireNotNull(
                    statements[
                        target
                    ],
                ) {
                    "Score transition predecessor is absent"
                }
            require(
                prior.current &&
                    prior.semanticSubjectFingerprint ==
                    event.string(
                        "semanticSubjectFingerprint",
                    ) &&
                    prior.memoryRelation ==
                    event.string(
                        "memoryRelation",
                    ) &&
                    prior.acceptanceSha256 ==
                    event.string(
                        "transitionTargetAcceptanceSha256",
                    ),
            ) {
                "Score transition does not bind exact current predecessor"
            }
            prior.current =
                false
            prior.effectiveState =
                effective
        }

        when (
            event.string(
                "transitionKind",
            )
        ) {
            "NONE" ->
                Unit

            "CORRECTS" ->
                retire(
                    "correctsStatementRef",
                    "CORRECTED",
                )

            "SUPERSEDES" ->
                retire(
                    "supersedesStatementRef",
                    "SUPERSEDED",
                )

            "RELEASES_OR_TOMBSTONES" ->
                retire(
                    "releasesOrTombstonesStatementRef",
                    "RELEASED_OR_TOMBSTONED",
                )

            else ->
                error(
                    "Score transition kind is invalid",
                )
        }

        statements[
            ref
        ] =
            Statement(
                statementRef = ref,
                semanticSubjectFingerprint =
                    event.string(
                        "semanticSubjectFingerprint",
                    ),
                memoryRelation =
                    event.string(
                        "memoryRelation",
                    ),
                recordedState =
                    event.string(
                        "statementState",
                    ),
                effectiveState =
                    event.string(
                        "statementState",
                    ),
                summary =
                    event.string(
                        "summary",
                    ),
                summaryHash =
                    event.string(
                        "summaryHash",
                    ),
                sourceBindings =
                    event.array(
                        "sourceBindings",
                    ),
                sourceConversationHeadSha256 =
                    event.string(
                        "sourceConversationHeadSha256",
                    ),
                acceptedForContinuity =
                    event.boolean(
                        "acceptedForContinuity",
                    ),
                consentState =
                    event.string(
                        "consentState",
                    ),
                semanticAuthorityHeadSha256 =
                    event.string(
                        "semanticAuthorityHeadSha256",
                    ),
                acceptanceRef =
                    event.string(
                        "semanticAcceptanceRef",
                    ),
                acceptanceSha256 =
                    event.string(
                        "semanticAcceptanceSha256",
                    ),
                candidateRef =
                    event.string(
                        "semanticCandidateRef",
                    ),
                candidateSha256 =
                    event.string(
                        "semanticCandidateSha256",
                    ),
                current =
                    event.string(
                        "statementState",
                    ) !=
                    "RELEASED_OR_TOMBSTONED",
            )
    }

    private fun validateAuthorityIdentity(
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
                    "project.multivex.memory",
                    "project.multivex.safety",
                ) &&
                head.long(
                    "sequence",
                ) >=
                0,
        ) {
            "semantic authority head identity is invalid"
        }
        val seen =
            mutableSetOf<String>()
        for (
            binding in
            head.objectList(
                "currentAcceptanceBindings",
            )
        ) {
            requireSha256(
                binding.string(
                    "semanticSubjectFingerprint",
                ),
                "semantic subject fingerprint",
            )
            requireSha256(
                binding.string(
                    "acceptanceSha256",
                ),
                "acceptance SHA",
            )
            requireSha256(
                binding.string(
                    "candidateSha256",
                ),
                "candidate SHA",
            )
            requireSha256(
                binding.string(
                    "consentDispositionSha256",
                ),
                "consent SHA",
            )
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
        }
    }

    private fun loadDaily(
        home: Path,
        identity: Identity,
        score: ScoreSnapshot,
        conversationStore: DirectConversationStore,
    ): DailySnapshot {
        val root =
            "daily-memory-dream/" +
                identity.lineageRef +
                "/" +
                identity.threadRef
        val head =
            readAddressed(
                home = home,
                path =
                    inside(
                        home,
                        root +
                            "/head.json",
                    ),
                schemaVersion = "vextreme.daily-memory-dream-head/v1",
                refField = "dailyDreamHeadRef",
                hashField = "dailyDreamHeadSha256",
                prefix = "daily-dream-head",
                label = "current Daily Memory head",
            )
        val sourceConversationHead =
            head.string(
                "sourceConversationHeadSha256",
            )
        require(
            head.string(
                "homeRef",
            ) ==
                identity.homeRef &&
                head.string(
                    "deviceRef",
                ) ==
                identity.deviceRef &&
                head.string(
                    "companionLineageRef",
                ) ==
                identity.lineageRef &&
                head.string(
                    "threadRef",
                ) ==
                identity.threadRef &&
                sourceConversationHead ==
                score.head.string(
                    "sourceConversationHeadSha256",
                ) &&
                head.string(
                    "sourceScoreHeadSha256",
                ) ==
                score.head.string(
                    "scoreHeadSha256",
                ) &&
                head.string(
                    "sourceSemanticAuthorityHeadSha256",
                ) ==
                score.authority.string(
                    "semanticAuthorityHeadSha256",
                ),
        ) {
            "Daily Memory head is not exact to the current Score/authority source frontier"
        }
        val historicalDailySource =
            conversationStore.historicalCompletedTurn(
                sourceConversationHead,
            )
        require(
            historicalDailySource.currentCommittedLineageVerified,
        ) {
            "Daily Memory source Conversation head is not in the current committed lineage"
        }

        val headSha =
            head.string(
                "dailyDreamHeadSha256",
            )
        val immutable =
            readAddressed(
                home = home,
                path =
                    inside(
                        home,
                        root +
                            "/heads/" +
                            headSha +
                            ".json",
                    ),
                schemaVersion = "vextreme.daily-memory-dream-head/v1",
                refField = "dailyDreamHeadRef",
                hashField = "dailyDreamHeadSha256",
                prefix = "daily-dream-head",
                label = "immutable Daily Memory head",
            )
        require(
            immutable ==
                head,
        ) {
            "Daily Memory head lacks exact immutable receipt"
        }
        validateDailyHeadLineage(
            home = home,
            root = root,
            current = head,
        )

        val stratumSha =
            head.string(
                "dailyStratumSha256",
            )
        val stratum =
            readAddressed(
                home = home,
                path =
                    inside(
                        home,
                        root +
                            "/strata/" +
                            stratumSha +
                            ".json",
                    ),
                schemaVersion = "vextreme.daily-stratum/v1",
                refField = "dailyStratumRef",
                hashField = "dailyStratumSha256",
                prefix = "daily-stratum",
                label = "current Daily Stratum",
            )
        require(
            stratum.long(
                "sequence",
            ) ==
                head.long(
                    "sequence",
                ) &&
                stratum.string(
                    "dayRef",
                ) ==
                head.string(
                    "dayRef",
                ) &&
                stratum.long(
                    "dayIndex",
                ) ==
                head.long(
                    "dayIndex",
                ) &&
                stratum.string(
                    "sourceConversationHeadSha256",
                ) ==
                sourceConversationHead &&
                stratum.string(
                    "sourceScoreHeadSha256",
                ) ==
                score.head.string(
                    "scoreHeadSha256",
                ) &&
                stratum.string(
                    "sourceSemanticAuthorityHeadSha256",
                ) ==
                score.authority.string(
                    "semanticAuthorityHeadSha256",
                ) &&
                !stratum.boolean(
                    "trainingRan",
                ) &&
                !stratum.boolean(
                    "modelWeightsChanged",
                ),
        ) {
            "Daily Stratum is stale or violates Memory-only boundary"
        }

        val orientation =
            dailyReceipt(
                home,
                root +
                    "/orientations/" +
                    stratum.string(
                        "orientationSha256",
                    ) +
                    ".json",
                "vextreme.daily-pre-rest-orientation/v1",
                "preRestOrientationRef",
                "orientationSha256",
                "pre-rest-orientation",
            )
        val preDream =
            dailyReceipt(
                home,
                root +
                    "/pre-dream/" +
                    stratum.string(
                        "preDreamStateSha256",
                    ) +
                    ".json",
                "vextreme.daily-pre-dream-state/v1",
                "preDreamStateRef",
                "preDreamStateSha256",
                "pre-dream-state",
            )
        val closure =
            dailyReceipt(
                home,
                root +
                    "/closures/" +
                    stratum.string(
                        "dayClosureSha256",
                    ) +
                    ".json",
                "vextreme.daily-day-closure/v1",
                "dayClosureRef",
                "dayClosureSha256",
                "day-closure",
            )
        val consolidation =
            dailyReceipt(
                home,
                root +
                    "/consolidations/" +
                    stratum.string(
                        "memoryConsolidationSha256",
                    ) +
                    ".json",
                "vextreme.daily-memory-consolidation/v1",
                "memoryConsolidationRef",
                "memoryConsolidationSha256",
                "memory-consolidation",
            )
        val postDream =
            dailyReceipt(
                home,
                root +
                    "/post-dream/" +
                    stratum.string(
                        "postDreamStateSha256",
                    ) +
                    ".json",
                "vextreme.daily-post-dream-state/v1",
                "postDreamStateRef",
                "postDreamStateSha256",
                "post-dream-state",
            )
        val wake =
            dailyReceipt(
                home,
                root +
                    "/wakes/" +
                    head.string(
                        "wakeReceiptSha256",
                    ) +
                    ".json",
                "vextreme.daily-wake-receipt/v1",
                "wakeReceiptRef",
                "wakeReceiptSha256",
                "daily-wake",
            )

        val authoritySha =
            score.authority.string(
                "semanticAuthorityHeadSha256",
            )
        for (
            receipt in
            listOf(
                orientation,
                preDream,
                closure,
                consolidation,
                postDream,
                wake,
            )
        ) {
            require(
                receipt.string(
                    "sourceSemanticAuthorityHeadSha256",
                ) ==
                    authoritySha &&
                    receipt.string(
                        "dayRef",
                    ) ==
                    stratum.string(
                        "dayRef",
                    ),
            ) {
                "Daily Memory child receipt source/day identity drift"
            }
        }

        require(
            preDream.string(
                "orientationSha256",
            ) ==
                orientation.string(
                    "orientationSha256",
                ) &&
                closure.string(
                    "preDreamStateSha256",
                ) ==
                preDream.string(
                    "preDreamStateSha256",
                ) &&
                consolidation.string(
                    "dayClosureSha256",
                ) ==
                closure.string(
                    "dayClosureSha256",
                ) &&
                postDream.string(
                    "memoryConsolidationSha256",
                ) ==
                consolidation.string(
                    "memoryConsolidationSha256",
                ) &&
                stratum.string(
                    "preDreamStateSha256",
                ) ==
                preDream.string(
                    "preDreamStateSha256",
                ) &&
                stratum.string(
                    "dayClosureSha256",
                ) ==
                closure.string(
                    "dayClosureSha256",
                ) &&
                stratum.string(
                    "memoryConsolidationSha256",
                ) ==
                consolidation.string(
                    "memoryConsolidationSha256",
                ) &&
                stratum.string(
                    "postDreamStateSha256",
                ) ==
                postDream.string(
                    "postDreamStateSha256",
                ) &&
                wake.string(
                    "dailyStratumSha256",
                ) ==
                stratumSha,
        ) {
            "Daily Memory causal bundle is invalid"
        }

        for (
            receipt in
            listOf(
                preDream,
                closure,
                consolidation,
                postDream,
            )
        ) {
            require(
                receipt.string(
                    "sourceConversationHeadSha256",
                ) ==
                    sourceConversationHead &&
                    receipt.string(
                        "sourceScoreHeadSha256",
                    ) ==
                    score.head.string(
                        "scoreHeadSha256",
                    ),
            ) {
                "Daily Memory child receipt differs from its exact Daily source frontier"
            }
        }
        require(
            !preDream.boolean(
                "rawConversationContentIncluded",
            ) &&
                !closure.boolean(
                    "rawConversationContentIncluded",
                ) &&
                !consolidation.boolean(
                    "rawConversationContentIncluded",
                ) &&
                !consolidation.boolean(
                    "newSemanticAcceptanceCreated",
                ) &&
                !consolidation.boolean(
                    "firstPersonAuthorityGranted",
                ) &&
                !postDream.boolean(
                    "rawConversationContentIncluded",
                ),
        ) {
            "Daily Memory bundle violated read-only/reference-only boundary"
        }

        val currentStatements =
            score.statements
                .filter(
                    Statement::current,
                )
                .sortedBy(
                    Statement::statementRef,
                )
        val active =
            currentStatements
                .filter {
                    it.acceptedForContinuity &&
                        it.consentState in
                        positiveConsent &&
                        authorityContains(
                            score.authority,
                            it,
                        )
                }
                .map(
                    Statement::statementRef,
                )
        val held =
            currentStatements
                .filter {
                    !(
                        it.acceptedForContinuity &&
                            it.consentState in
                            positiveConsent &&
                            authorityContains(
                                score.authority,
                                it,
                            )
                    )
                }
                .map(
                    Statement::statementRef,
                )
        val loops =
            score.openLoops
                .filter {
                    it.state ==
                        "OPEN"
                }
                .map(
                    OpenLoop::ref,
                )
                .sorted()
        require(
            postDream.stringList(
                "activeContinuityStatementRefs",
            ) ==
                active &&
                postDream.stringList(
                    "heldOrDeferredStatementRefs",
                ) ==
                held &&
                postDream.stringList(
                    "openLoopRefs",
                ) ==
                loops,
        ) {
            "post-Dream Memory projection differs from current Score/authority"
        }

        return DailySnapshot(
            stratum = stratum,
            sourceConversationHeadSha256 = sourceConversationHead,
            sourceScoreHeadSha256 =
                score.head.string(
                    "scoreHeadSha256",
                ),
            sourceSemanticAuthorityHeadSha256 = authoritySha,
            activeStatementRefs = active,
            heldStatementRefs = held,
        )
    }

    private fun validateDailyHeadLineage(
        home: Path,
        root: String,
        current: JsonObject,
    ) {
        val visited =
            mutableSetOf<String>()
        var cursor =
            current
        while (
            true
        ) {
            val sha =
                cursor.string(
                    "dailyDreamHeadSha256",
                )
            require(
                visited.add(
                    sha,
                ),
            ) {
                "Daily Memory head lineage contains a cycle"
            }
            val sequence =
                cursor.long(
                    "sequence",
                )
            val prior =
                cursor.nullableString(
                    "priorDailyDreamHeadSha256",
                )
            if (
                sequence ==
                    0L
            ) {
                require(
                    prior ==
                        null,
                ) {
                    "Daily Memory genesis head has a predecessor"
                }
                break
            }
            val priorSha =
                requireNotNull(
                    prior,
                ) {
                    "Daily Memory non-genesis head has no predecessor"
                }
            val previous =
                readAddressed(
                    home = home,
                    path =
                        inside(
                            home,
                            root +
                                "/heads/" +
                                priorSha +
                                ".json",
                        ),
                    schemaVersion = "vextreme.daily-memory-dream-head/v1",
                    refField = "dailyDreamHeadRef",
                    hashField = "dailyDreamHeadSha256",
                    prefix = "daily-dream-head",
                    label = "prior Daily Memory head",
                )
            require(
                previous.long(
                    "sequence",
                ) ==
                    sequence -
                    1,
            ) {
                "Daily Memory head sequence is not contiguous"
            }
            cursor =
                previous
        }
    }

    private fun formContext(
        score: ScoreSnapshot,
        daily: DailySnapshot,
    ): VexLifeMemoryContract.Context {
        val byRef =
            score.statements.associateBy(
                Statement::statementRef,
            )
        val selectedRefs =
            daily.activeStatementRefs.take(
                VexLifeMemoryContract.MaximumPages,
            )
        val boundedOut =
            daily.activeStatementRefs.drop(
                VexLifeMemoryContract.MaximumPages,
            )
        val pages =
            mutableListOf<VexLifeMemoryContract.Page>()
        val tokenBoundedOut =
            mutableListOf<String>()

        for (
            ref in
            selectedRefs
        ) {
            val statement =
                requireNotNull(
                    byRef[
                        ref
                    ],
                ) {
                    "Daily active statement is absent from Score replay"
                }
            val page =
                VexLifeMemoryContract.Page(
                    pageRef =
                        "page.living-journal.memory." +
                            sha256(
                                VexContractJson.canonicalEncode(
                                    PageIdentity(
                                        dailyStratumSha256 =
                                            daily.stratum.string(
                                                "dailyStratumSha256",
                                            ),
                                        statementRef = statement.statementRef,
                                    ),
                                ),
                            )
                                .take(
                                    32,
                                ),
                    statementRef = statement.statementRef,
                    summary = statement.summary,
                    summaryHash = statement.summaryHash,
                    memoryRelation = statement.memoryRelation,
                    recordedStatementState = statement.recordedState,
                    effectiveState = statement.effectiveState,
                    consentState = statement.consentState,
                    semanticAcceptanceRef = statement.acceptanceRef,
                    semanticAcceptanceSha256 = statement.acceptanceSha256,
                    semanticAuthorityHeadSha256 = statement.semanticAuthorityHeadSha256,
                )
            val candidate =
                providerPacket(
                    daily = daily,
                    pages =
                        pages +
                            page,
                    boundedOut = boundedOut,
                    tokenBoundedOut = emptyList(),
                    reasons = emptyList(),
                )
            if (
                DirectPromptContext.estimatedMessageTokens(
                    memorySystemContent(
                        candidate,
                    ),
                ) >
                VexLifeMemoryContract.MaximumMemoryTokens
            ) {
                tokenBoundedOut +=
                    page.pageRef
            } else {
                pages +=
                    page
            }
        }

        val packet =
            providerPacket(
                daily = daily,
                pages = pages,
                boundedOut = boundedOut,
                tokenBoundedOut = tokenBoundedOut,
                reasons = emptyList(),
            )
        val included =
            pages.isNotEmpty()
        val systemContent =
            if (
                included
            ) {
                memorySystemContent(
                    packet,
                )
            } else {
                null
            }
        val core =
            VexLifeMemoryContract.PacketCore(
                schemaVersion = packet.schemaVersion,
                truthClass = packet.truthClass,
                projectionState = packet.projectionState,
                projectionCurrentness = packet.projectionCurrentness,
                sourceOwnerRefs = packet.sourceOwnerRefs,
                currentDailyStratumRef = packet.currentDailyStratumRef,
                currentDailyStratumSha256 = packet.currentDailyStratumSha256,
                sourceConversationHeadSha256 = packet.sourceConversationHeadSha256,
                sourceScoreHeadSha256 = packet.sourceScoreHeadSha256,
                sourceSemanticAuthorityHeadSha256 = packet.sourceSemanticAuthorityHeadSha256,
                pages = packet.pages,
                heldOrDeferredStatementRefs = packet.heldOrDeferredStatementRefs,
                boundedOutStatementRefs = packet.boundedOutStatementRefs,
                tokenBoundedOutPageRefs = packet.tokenBoundedOutPageRefs,
                reasons = packet.reasons,
                memoryTokenCap = packet.memoryTokenCap,
                systemMessageSha256 =
                    systemContent
                        ?.let(
                            ::sha256,
                        ),
                estimatedSystemMessageTokens =
                    systemContent
                        ?.let(
                            DirectPromptContext::estimatedMessageTokens,
                        )
                        ?: 0,
            )
        val fingerprint =
            sha256(
                VexContractJson.canonicalEncode(
                    core,
                ),
            )
        val refs =
            buildList {
                addAll(
                    core.sourceOwnerRefs,
                )
                core.currentDailyStratumRef
                    ?.let(
                        ::add,
                    )
                core.pages.forEach {
                    page ->
                    add(
                        page.pageRef,
                    )
                    add(
                        page.statementRef,
                    )
                    add(
                        page.semanticAcceptanceRef,
                    )
                }
            }
                .distinct()
                .sorted()

        return VexLifeMemoryContract.Context(
            schemaVersion = core.schemaVersion,
            truthClass = core.truthClass,
            projectionState = core.projectionState,
            projectionCurrentness = core.projectionCurrentness,
            sourceOwnerRefs = core.sourceOwnerRefs,
            currentDailyStratumRef = core.currentDailyStratumRef,
            currentDailyStratumSha256 = core.currentDailyStratumSha256,
            sourceConversationHeadSha256 = core.sourceConversationHeadSha256,
            sourceScoreHeadSha256 = core.sourceScoreHeadSha256,
            sourceSemanticAuthorityHeadSha256 = core.sourceSemanticAuthorityHeadSha256,
            pages = core.pages,
            heldOrDeferredStatementRefs = core.heldOrDeferredStatementRefs,
            boundedOutStatementRefs = core.boundedOutStatementRefs,
            tokenBoundedOutPageRefs = core.tokenBoundedOutPageRefs,
            reasons = core.reasons,
            memoryTokenCap = core.memoryTokenCap,
            systemMessageSha256 = core.systemMessageSha256,
            estimatedSystemMessageTokens = core.estimatedSystemMessageTokens,
            memoryContextRef =
                "memory-context.vexlife.prompt." +
                    fingerprint.take(
                        32,
                    ),
            semanticFingerprint = fingerprint,
            sourceRefs = refs,
            included = included,
        )
            .let(
                VexLifeMemoryContract::validateContext,
            )
    }

    private fun held(
        reasons: List<String>,
    ): VexLifeMemoryContract.Context {
        val owners =
            listOf(
                LivingJournalMemoryOwnerRef,
                LivingJournalDailyRecordOwnerRef,
            )
                .sorted()
        val core =
            VexLifeMemoryContract.PacketCore(
                schemaVersion = VexLifeMemoryContract.PacketSchema,
                truthClass = VexLifeMemoryContract.PacketTruthClass,
                projectionState = "HELD",
                projectionCurrentness = "HELD",
                sourceOwnerRefs = owners,
                currentDailyStratumRef = null,
                currentDailyStratumSha256 = null,
                sourceConversationHeadSha256 = null,
                sourceScoreHeadSha256 = null,
                sourceSemanticAuthorityHeadSha256 = null,
                pages = emptyList(),
                heldOrDeferredStatementRefs = emptyList(),
                boundedOutStatementRefs = emptyList(),
                tokenBoundedOutPageRefs = emptyList(),
                reasons = reasons.distinct().sorted(),
                memoryTokenCap = VexLifeMemoryContract.MaximumMemoryTokens,
                systemMessageSha256 = null,
                estimatedSystemMessageTokens = 0,
            )
        val fingerprint =
            sha256(
                VexContractJson.canonicalEncode(
                    core,
                ),
            )
        return VexLifeMemoryContract.Context(
            schemaVersion = core.schemaVersion,
            truthClass = core.truthClass,
            projectionState = core.projectionState,
            projectionCurrentness = core.projectionCurrentness,
            sourceOwnerRefs = core.sourceOwnerRefs,
            currentDailyStratumRef = null,
            currentDailyStratumSha256 = null,
            sourceConversationHeadSha256 = null,
            sourceScoreHeadSha256 = null,
            sourceSemanticAuthorityHeadSha256 = null,
            pages = emptyList(),
            heldOrDeferredStatementRefs = emptyList(),
            boundedOutStatementRefs = emptyList(),
            tokenBoundedOutPageRefs = emptyList(),
            reasons = core.reasons,
            memoryTokenCap = core.memoryTokenCap,
            systemMessageSha256 = null,
            estimatedSystemMessageTokens = 0,
            memoryContextRef =
                "memory-context.vexlife.prompt." +
                    fingerprint.take(
                        32,
                    ),
            semanticFingerprint = fingerprint,
            sourceRefs = owners,
            included = false,
        )
            .let(
                VexLifeMemoryContract::validateContext,
            )
    }

    private fun providerPacket(
        daily: DailySnapshot,
        pages: List<VexLifeMemoryContract.Page>,
        boundedOut: List<String>,
        tokenBoundedOut: List<String>,
        reasons: List<String>,
    ): ProviderPacket =
        ProviderPacket(
            schemaVersion = VexLifeMemoryContract.PacketSchema,
            truthClass = VexLifeMemoryContract.PacketTruthClass,
            projectionState = "CURRENT",
            projectionCurrentness = "CURRENT",
            sourceOwnerRefs =
                listOf(
                    LivingJournalMemoryOwnerRef,
                    LivingJournalDailyRecordOwnerRef,
                )
                    .sorted(),
            currentDailyStratumRef =
                daily.stratum.string(
                    "dailyStratumRef",
                ),
            currentDailyStratumSha256 =
                daily.stratum.string(
                    "dailyStratumSha256",
                ),
            sourceConversationHeadSha256 = daily.sourceConversationHeadSha256,
            sourceScoreHeadSha256 = daily.sourceScoreHeadSha256,
            sourceSemanticAuthorityHeadSha256 = daily.sourceSemanticAuthorityHeadSha256,
            pages = pages,
            heldOrDeferredStatementRefs = daily.heldStatementRefs.sorted(),
            boundedOutStatementRefs = boundedOut.sorted(),
            tokenBoundedOutPageRefs = tokenBoundedOut.sorted(),
            reasons = reasons.distinct().sorted(),
            memoryTokenCap = VexLifeMemoryContract.MaximumMemoryTokens,
        )

    private fun memorySystemContent(
        packet: ProviderPacket,
    ): String =
        listOf(
            "VEXLIFE_CURRENT_MEMORY_CONTEXT/v1",
            "These are current source-attributed Memory references, not proof of subjective recall or original wording by this model.",
            "Use them as bounded continuity context. If they conflict with current source or the human, preserve the conflict rather than inventing reconciliation.",
            VexContractJson.compactEncode(
                packet,
            ),
        )
            .joinToString(
                "\n",
            )

    private fun authorityContains(
        head: JsonObject,
        statement: Statement,
    ): Boolean =
        head.objectList(
            "currentAcceptanceBindings",
        )
            .any {
                it.string(
                    "acceptanceRef",
                ) ==
                    statement.acceptanceRef &&
                    it.string(
                        "acceptanceSha256",
                    ) ==
                    statement.acceptanceSha256
            }

    private fun dailyReceipt(
        home: Path,
        relative: String,
        schemaVersion: String,
        refField: String,
        hashField: String,
        prefix: String,
    ): JsonObject =
        readAddressed(
            home = home,
            path =
                inside(
                    home,
                    relative,
                ),
            schemaVersion = schemaVersion,
            refField = refField,
            hashField = hashField,
            prefix = prefix,
            label = prefix,
        )

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
                home,
                path,
                label,
            )
        require(
            value.string(
                "schemaVersion",
            ) ==
                schemaVersion,
        ) {
            label +
                " schema is not current"
        }

        val observedRef =
            value.string(
                refField,
            )
        val observedHash =
            value.string(
                hashField,
            )
        requireSha256(
            observedHash,
            label +
                " hash",
        )

        val preRef =
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
                sha256(
                    VexContractJson.canonicalEncodeElement(
                        preRef,
                    ),
                )
                    .take(
                        32,
                    )
        val withRef =
            JsonObject(
                value.filterKeys {
                    it !=
                        hashField
                },
            )
        require(
            observedRef ==
                expectedRef &&
                sha256(
                    VexContractJson.canonicalEncodeElement(
                        withRef,
                    ),
                ) ==
                observedHash,
        ) {
            label +
                " content-address identity is invalid"
        }

        return value
    }

    private fun readObject(
        home: Path,
        path: Path,
        label: String,
    ): JsonObject {
        requireRegularFile(
            path,
            label,
        )
        val text =
            requireNotNull(
                VexDurableContractFile.readText(
                    ownedRoot = home,
                    path = path,
                ),
            ) {
                label +
                    " is missing"
            }
        return VexContractJson.Strict
            .parseToJsonElement(
                text,
            )
            .jsonObject
    }

    private fun hashWithout(
        value: JsonObject,
        field: String,
    ): String =
        sha256(
            VexContractJson.canonicalEncodeElement(
                JsonObject(
                    value.filterKeys {
                        it !=
                            field
                    },
                ),
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

    private fun inside(
        home: Path,
        relative: String,
    ): Path =
        VexOwnedPath.resolveInside(
            home,
            relative,
        )

    private fun requireRealDirectory(
        path: Path,
        label: String,
    ) {
        require(
            Files.exists(
                path,
                LinkOption.NOFOLLOW_LINKS,
            ) &&
                Files.isDirectory(
                    path,
                    LinkOption.NOFOLLOW_LINKS,
                ) &&
                !Files.isSymbolicLink(
                    path,
                ),
        ) {
            label +
                " must be one real non-symlink directory"
        }
    }

    private fun requireRegularFile(
        path: Path,
        label: String,
    ) {
        require(
            Files.exists(
                path,
                LinkOption.NOFOLLOW_LINKS,
            ) &&
                Files.isRegularFile(
                    path,
                    LinkOption.NOFOLLOW_LINKS,
                ) &&
                !Files.isSymbolicLink(
                    path,
                ),
        ) {
            label +
                " must be one regular non-symlink file"
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
            label +
                " must be one lowercase SHA-256"
        }
    }

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

    private fun JsonObject.element(
        name: String,
    ): JsonElement =
        requireNotNull(
            this[
                name
            ],
        ) {
            "required JSON field is missing: " +
                name
        }

    private fun JsonObject.string(
        name: String,
    ): String =
        element(
            name,
        )
            .jsonPrimitive
            .content

    private fun JsonObject.nullableString(
        name: String,
    ): String? {
        val value =
            element(
                name,
            )
        return if (
            value is
                JsonNull
        ) {
            null
        } else {
            value.jsonPrimitive.contentOrNull
        }
    }

    private fun JsonObject.long(
        name: String,
    ): Long =
        element(
            name,
        )
            .jsonPrimitive
            .long

    private fun JsonObject.boolean(
        name: String,
    ): Boolean =
        element(
            name,
        )
            .jsonPrimitive
            .boolean

    private fun JsonObject.array(
        name: String,
    ): JsonArray =
        element(
            name,
        )
            .jsonArray

    private fun JsonObject.stringList(
        name: String,
    ): List<String> =
        array(
            name,
        )
            .map {
                it.jsonPrimitive.content
            }

    private fun JsonObject.objectList(
        name: String,
    ): List<JsonObject> =
        array(
            name,
        )
            .map {
                it.jsonObject
            }
}

// [VXG RealForever]
