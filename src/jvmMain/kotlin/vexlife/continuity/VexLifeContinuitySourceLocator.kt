package vexlife.continuity

import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import vexlife.conversation.DirectConversationStore

/**
 * Derived candidate locator for source-addressed continuity reads.
 *
 * This owner answers only which canonical source coordinates are plausible candidates for one
 * bounded lexical query. Conversation remains the source of truth: every returned candidate is
 * reopened through [DirectConversationStore.Locked.exactCommittedConversationEvents] before its
 * content is exposed.
 *
 * The backend is a realization seam, not semantic identity. A backend may be volatile, file-backed
 * or replaced by another qualified local realization later without changing Conversation identity.
 *
 * Permanent:
 * - LOCATOR != CONVERSATION
 * - LOCATOR != MEMORY
 * - LOCATOR HIT != SEMANTIC ACCEPTANCE
 * - INDEX DELETE != SOURCE DELETE
 * - STALE / CORRUPT / UNSUPPORTED LOCATOR != NOT_FOUND
 */
object VexLifeContinuitySourceLocator {
    const val GenerationSchema =
        "vexlife-prod.continuity-source-locator-generation/v1"

    const val UnboundSourceLifecycleBindingRef =
        "lifecycle.unbound"

    const val MaximumQueryTerms = 6
    const val MaximumQueryTermCharacters = 160
    const val MaximumExactCandidateReopens = 48
    const val MaximumReturnedTurns = 3

    enum class SpeakerScope {
        ANY,
        HUMAN,
        VEX,
    }

    enum class FallbackReason {
        ABSENT,
        STALE,
        CORRUPT,
        UNSUPPORTED_QUERY,
        CANDIDATE_SET_TOO_LARGE,
        FINGERPRINT_BINDING_MISMATCH,
        SOURCE_LIFECYCLE_BINDING_MISMATCH,
        EXACT_VERIFICATION_MISS,
        SOURCE_COORDINATE_MISMATCH,
    }

    data class SourceCoordinates(
        val request: DirectConversationStore.EventAddress,
        val response: DirectConversationStore.EventAddress,
    )

    /**
     * Derived source-addressed row. It intentionally contains no Conversation body or Memory
     * content. Fingerprints belong only to this backend realization.
     */
    data class Entry(
        val coordinates: SourceCoordinates,
        val requestFingerprints: Set<String>,
        val responseFingerprints: Set<String>,
    )

    /**
     * One derived generation bound to the exact completed Conversation head it observed.
     *
     * A generation is useful only while [sourceConversationHeadSha256] still equals the current
     * completed Conversation head. Incremental catch-up is a later integration concern; this first
     * substrate fails closed to canonical fallback when the generation is stale.
     */
    data class Generation(
        val schemaVersion: String,
        val sourceConversationHeadSha256: String?,
        val sourceConversationSequence: Long?,
        val fingerprintBindingRef: String,
        val sourceEventCount: Int,
        val completedTurnCount: Int,
        val entries: List<Entry>,
        val sourceLifecycleBindingRef: String =
            UnboundSourceLifecycleBindingRef,
    )

    sealed interface BackendLoad {
        data object Absent : BackendLoad

        data class Ready(
            val generation: Generation,
        ) : BackendLoad

        data class Corrupt(
            val reason: String,
        ) : BackendLoad
    }

    /**
     * Construction/provider seam for derived locator custody.
     *
     * [fingerprint] is backend-owned so a persistent realization can use its own protected key
     * without making that key, database or file format part of Product semantics.
     */
    interface Backend {
        /**
         * Non-secret identity of the exact algorithm/key-version binding that formed fingerprints.
         *
         * A persistent backend must change this ref when its fingerprint key or algorithm changes.
         */
        val fingerprintBindingRef: String

        fun fingerprint(value: String): String

        fun load(): BackendLoad

        fun replace(generation: Generation)

        fun clear()
    }

    data class IndexedCandidateLookup(
        val entryIndexes: Set<Int>,
        val postingLookupCount: Int,
    )

    /**
     * Optional realization capability for a true inverted lexical index.
     *
     * Product semantics do not require this exact data structure. A backend that does not implement
     * it preserves the prior linear derived-entry scan; a qualified backend may instead answer
     * postings and append one new entry without rebuilding lifetime derived state.
     */
    interface IndexedBackend : Backend {
        fun candidateEntryIndexes(
            queryFingerprints: List<Set<String>>,
            speakerScope: SpeakerScope,
        ): IndexedCandidateLookup

        fun replaceAfterAppend(
            generation: Generation,
            appendedEntry: Entry,
        )
    }

    /**
     * Volatile reference realization. It deliberately persists nothing to Home, which keeps the
     * first substrate proof out of the unresolved durable-index leakage/encryption decision.
     */
    class InMemoryBackend(
        secret: ByteArray = SecureRandom().generateSeed(32),
        override val fingerprintBindingRef: String =
            "fingerprint.in-memory." +
                UUID.randomUUID()
                    .toString()
                    .replace(
                        "-",
                        "",
                    ),
    ) : IndexedBackend {
        private val secret: ByteArray =
            secret.copyOf().also {
                require(it.size >= 32) {
                    "continuity source locator fingerprint secret must contain at least 32 bytes"
                }
                requirePortableRef(
                    fingerprintBindingRef,
                    "fingerprintBindingRef",
                )
            }

        private var generation: Generation? = null

        private val requestPostings =
            linkedMapOf<String, MutableSet<Int>>()

        private val responsePostings =
            linkedMapOf<String, MutableSet<Int>>()

        override fun fingerprint(value: String): String {
            val mac =
                Mac.getInstance("HmacSHA256")
            mac.init(
                SecretKeySpec(
                    secret,
                    "HmacSHA256",
                ),
            )
            return mac
                .doFinal(
                    value.toByteArray(
                        StandardCharsets.UTF_8,
                    ),
                )
                .take(8)
                .joinToString("") {
                    "%02x".format(
                        it.toInt() and 0xff,
                    )
                }
        }

        override fun load(): BackendLoad =
            generation
                ?.let {
                    BackendLoad.Ready(
                        it,
                    )
                }
                ?: BackendLoad.Absent

        override fun replace(generation: Generation) {
            this.generation =
                generation
            requestPostings.clear()
            responsePostings.clear()
            generation.entries.forEachIndexed(
                ::indexEntry,
            )
        }

        override fun replaceAfterAppend(
            generation: Generation,
            appendedEntry: Entry,
        ) {
            val prior =
                requireNotNull(
                    this.generation,
                ) {
                    "indexed locator append requires one prior generation"
                }
            require(
                generation.entries.size ==
                    prior.entries.size +
                        1 &&
                    generation.entries.dropLast(
                        1,
                    ) ==
                    prior.entries &&
                    generation.entries.last() ==
                    appendedEntry
            ) {
                "indexed locator append does not extend the exact prior generation"
            }

            this.generation =
                generation
            indexEntry(
                generation.entries.lastIndex,
                appendedEntry,
            )
        }

        override fun candidateEntryIndexes(
            queryFingerprints: List<Set<String>>,
            speakerScope: SpeakerScope,
        ): IndexedCandidateLookup {
            var postingLookups =
                0

            fun posting(
                fingerprint: String,
            ): Set<Int> {
                postingLookups +=
                    1
                return when (
                    speakerScope
                ) {
                    SpeakerScope.HUMAN ->
                        requestPostings[
                            fingerprint
                        ]
                            .orEmpty()

                    SpeakerScope.VEX ->
                        responsePostings[
                            fingerprint
                        ]
                            .orEmpty()

                    SpeakerScope.ANY ->
                        requestPostings[
                            fingerprint
                        ]
                            .orEmpty() +
                            responsePostings[
                                fingerprint
                            ]
                                .orEmpty()
                }
            }

            val candidates =
                linkedSetOf<Int>()
            queryFingerprints.forEach {
                fingerprints ->
                if (
                    fingerprints.isEmpty()
                ) {
                    return@forEach
                }
                var intersection: Set<Int>? =
                    null
                for (
                    fingerprint in
                    fingerprints
                ) {
                    val next =
                        posting(
                            fingerprint,
                        )
                    if (
                        next.isEmpty()
                    ) {
                        intersection =
                            emptySet()
                        break
                    }
                    intersection =
                        intersection
                            ?.intersect(
                                next,
                            )
                            ?: next.toSet()
                    if (
                        intersection.isEmpty()
                    ) {
                        break
                    }
                }
                candidates +=
                    intersection.orEmpty()
            }

            return IndexedCandidateLookup(
                entryIndexes =
                    candidates,
                postingLookupCount =
                    postingLookups,
            )
        }

        override fun clear() {
            generation =
                null
            requestPostings.clear()
            responsePostings.clear()
        }

        private fun indexEntry(
            index: Int,
            entry: Entry,
        ) {
            entry.requestFingerprints.forEach {
                fingerprint ->
                requestPostings
                    .getOrPut(
                        fingerprint,
                    ) {
                        linkedSetOf()
                    }
                    .add(
                        index,
                    )
            }
            entry.responseFingerprints.forEach {
                fingerprint ->
                responsePostings
                    .getOrPut(
                        fingerprint,
                    ) {
                        linkedSetOf()
                    }
                    .add(
                        index,
                    )
            }
        }
    }

    data class RebuildReceipt(
        val sourceConversationHeadSha256: String?,
        val sourceEventCount: Int,
        val completedTurnCount: Int,
        val wholeHistoryEventEnumerationPerformed: Boolean,
        val canonicalSourceMutationPerformed: Boolean,
        val derivedBackendMutationPerformed: Boolean,
    )

    enum class CatchUpState {
        UPDATED,
        FULL_REBUILD_REQUIRED,
    }

    data class CatchUpReceipt(
        val state: CatchUpState,
        val sourceConversationHeadSha256: String?,
        val sourceEventCount: Int,
        val completedTurnCount: Int,
        val wholeHistoryEventEnumerationPerformed: Boolean = false,
        val canonicalSourceMutationPerformed: Boolean = false,
        val derivedBackendMutationPerformed: Boolean,
    )

    data class ResolvedCandidate(
        val matchedTerms: List<String>,
        val requestEvent: DirectConversationStore.Event,
        val responseEvent: DirectConversationStore.Event,
    )

    sealed interface LocateOutcome {
        val currentConversationHeadSha256: String?
        val wholeHistoryEventEnumerationPerformed: Boolean
        val canonicalSourceMutationPerformed: Boolean

        data class Found(
            override val currentConversationHeadSha256: String?,
            val generationConversationHeadSha256: String?,
            val candidates: List<ResolvedCandidate>,
            val exactEventFileReadCount: Int,
            val exactHeadFileReadCount: Int,
            val wholeHistoryHeadTraversalPerformed: Boolean,
            val derivedEntryScanCount: Int = 0,
            val derivedPostingLookupCount: Int = 0,
            override val wholeHistoryEventEnumerationPerformed: Boolean = false,
            override val canonicalSourceMutationPerformed: Boolean = false,
        ) : LocateOutcome

        data class CurrentMiss(
            override val currentConversationHeadSha256: String?,
            val generationConversationHeadSha256: String?,
            val derivedEntryScanCount: Int = 0,
            val derivedPostingLookupCount: Int = 0,
            override val wholeHistoryEventEnumerationPerformed: Boolean = false,
            override val canonicalSourceMutationPerformed: Boolean = false,
        ) : LocateOutcome

        data class FallbackRequired(
            val reason: FallbackReason,
            override val currentConversationHeadSha256: String?,
            val generationConversationHeadSha256: String?,
            override val wholeHistoryEventEnumerationPerformed: Boolean = false,
            override val canonicalSourceMutationPerformed: Boolean = false,
        ) : LocateOutcome
    }

    /**
     * Build one derived generation from canonical Conversation.
     *
     * This is intentionally the expensive path: it consumes the owner's validated complete snapshot.
     * The caller can schedule/trigger rebuild under its own lifecycle; the locator does not become a
     * Conversation writer or daemon.
     */
    fun rebuild(
        conversationOwner: DirectConversationStore.Locked,
        backend: Backend,
        sourceLifecycleBindingRef: String =
            UnboundSourceLifecycleBindingRef,
        excludedEventRefs: Set<String> =
            emptySet(),
    ): RebuildReceipt {
        requirePortableRef(
            sourceLifecycleBindingRef,
            "sourceLifecycleBindingRef",
        )
        val snapshot =
            conversationOwner.current()
        val events =
            snapshot.canonicalEvents
        require(
            events.size % 2 ==
                0,
        ) {
            "continuity source locator rebuild requires complete canonical Conversation pairs"
        }

        val entries =
            buildList {
                var index =
                    0
                while (
                    index <
                    events.size
                ) {
                    val request =
                        events[index]
                    val response =
                        events[index + 1]
                    requireCompletedPair(
                        request = request,
                        response = response,
                    )
                    if (
                        request.eventRef !in
                            excludedEventRefs &&
                        response.eventRef !in
                            excludedEventRefs
                    ) {
                        add(
                            Entry(
                                coordinates =
                                    SourceCoordinates(
                                        request =
                                            address(
                                                request,
                                            ),
                                        response =
                                            address(
                                                response,
                                            ),
                                    ),
                                requestFingerprints =
                                    fingerprints(
                                        request.content,
                                        backend,
                                    ),
                                responseFingerprints =
                                    fingerprints(
                                        response.content,
                                        backend,
                                    ),
                            ),
                        )
                    }
                    index +=
                        2
                }
            }

        val generation =
            Generation(
                schemaVersion = GenerationSchema,
                sourceConversationHeadSha256 =
                    snapshot.head
                        ?.conversationHeadSha256,
                sourceConversationSequence =
                    snapshot.head
                        ?.sequence,
                fingerprintBindingRef =
                    backend.fingerprintBindingRef,
                sourceLifecycleBindingRef =
                    sourceLifecycleBindingRef,
                sourceEventCount =
                    entries.size *
                        2,
                completedTurnCount =
                    entries.size,
                entries = entries,
            )
        requireGeneration(
            generation,
        )
        backend.replace(
            generation,
        )

        return RebuildReceipt(
            sourceConversationHeadSha256 =
                generation.sourceConversationHeadSha256,
            sourceEventCount =
                generation.sourceEventCount,
            completedTurnCount =
                generation.completedTurnCount,
            wholeHistoryEventEnumerationPerformed = true,
            canonicalSourceMutationPerformed = false,
            derivedBackendMutationPerformed = true,
        )
    }

    /**
     * Advance one already-current derived generation after the canonical Conversation commits one
     * exact completed turn.
     *
     * The locator is never allowed to guess across a missing predecessor. If the backend is absent
     * for a non-genesis Conversation, corrupt, rebound to another fingerprint profile, or not bound
     * to the exact prior Conversation head, this returns [CatchUpState.FULL_REBUILD_REQUIRED]
     * without mutating the backend. The next request may then use the canonical rebuild/fallback
     * path.
     */
    fun catchUpCommittedTurn(
        committedHead: DirectConversationStore.Head,
        request: DirectConversationStore.Event,
        response: DirectConversationStore.Event,
        backend: Backend,
    ): CatchUpReceipt {
        requireCompletedPair(
            request = request,
            response = response,
        )
        require(
            committedHead.requestEventHash ==
                request.eventHash &&
                committedHead.eventHash ==
                response.eventHash &&
                committedHead.sequence ==
                response.sequence &&
                committedHead.turnRef ==
                request.turnRef
        ) {
            "continuity source locator catch-up requires the exact committed Conversation pair/head"
        }

        val priorGeneration =
            when (
                val load =
                    backend.load()
            ) {
                BackendLoad.Absent ->
                    if (
                        committedHead.priorConversationHeadSha256 ==
                            null &&
                        request.sequence ==
                            0L
                    ) {
                        Generation(
                            schemaVersion = GenerationSchema,
                            sourceConversationHeadSha256 = null,
                            sourceConversationSequence = null,
                            fingerprintBindingRef =
                                backend.fingerprintBindingRef,
                            sourceLifecycleBindingRef =
                                UnboundSourceLifecycleBindingRef,
                            sourceEventCount = 0,
                            completedTurnCount = 0,
                            entries = emptyList(),
                        )
                    } else {
                        return catchUpRebuildRequired()
                    }

                is BackendLoad.Corrupt ->
                    return catchUpRebuildRequired()

                is BackendLoad.Ready ->
                    try {
                        requireGeneration(
                            load.generation,
                        )
                        load.generation
                    } catch (
                        _: RuntimeException,
                    ) {
                        return catchUpRebuildRequired()
                    }
            }

        if (
            priorGeneration.fingerprintBindingRef !=
                backend.fingerprintBindingRef ||
            priorGeneration.sourceConversationHeadSha256 !=
                committedHead.priorConversationHeadSha256 ||
            (
                committedHead.priorConversationHeadSha256 ==
                    null &&
                    priorGeneration.sourceConversationSequence !=
                    null
            ) ||
            (
                committedHead.priorConversationHeadSha256 !=
                    null &&
                    priorGeneration.sourceConversationSequence !=
                    request.sequence -
                    1
            )
        ) {
            return catchUpRebuildRequired()
        }

        val appendedEntry =
            Entry(
                coordinates =
                    SourceCoordinates(
                        request =
                            address(
                                request,
                            ),
                        response =
                            address(
                                response,
                            ),
                    ),
                requestFingerprints =
                    fingerprints(
                        request.content,
                        backend,
                    ),
                responseFingerprints =
                    fingerprints(
                        response.content,
                        backend,
                    ),
            )
        val updated =
            Generation(
                schemaVersion = GenerationSchema,
                sourceConversationHeadSha256 =
                    committedHead.conversationHeadSha256,
                sourceConversationSequence =
                    committedHead.sequence,
                fingerprintBindingRef =
                    backend.fingerprintBindingRef,
                sourceLifecycleBindingRef =
                    priorGeneration.sourceLifecycleBindingRef,
                sourceEventCount =
                    priorGeneration.sourceEventCount +
                        2,
                completedTurnCount =
                    priorGeneration.completedTurnCount +
                        1,
                entries =
                    priorGeneration.entries +
                        appendedEntry,
            )
        requireGeneration(
            updated,
        )
        if (
            backend is
            IndexedBackend
        ) {
            backend.replaceAfterAppend(
                generation =
                    updated,
                appendedEntry =
                    appendedEntry,
            )
        } else {
            backend.replace(
                updated,
            )
        }
        return CatchUpReceipt(
            state = CatchUpState.UPDATED,
            sourceConversationHeadSha256 =
                updated.sourceConversationHeadSha256,
            sourceEventCount =
                updated.sourceEventCount,
            completedTurnCount =
                updated.completedTurnCount,
            derivedBackendMutationPerformed = true,
        )
    }

    private fun catchUpRebuildRequired(): CatchUpReceipt =
        CatchUpReceipt(
            state = CatchUpState.FULL_REBUILD_REQUIRED,
            sourceConversationHeadSha256 = null,
            sourceEventCount = 0,
            completedTurnCount = 0,
            derivedBackendMutationPerformed = false,
        )

    /**
     * Locate through a current derived generation and reopen every nominated source through the
     * canonical Conversation owner.
     *
     * A locator miss is authoritative only for one generation that exactly matches the current
     * completed Conversation head. Every other condition returns [FallbackRequired].
     */
    fun locate(
        conversationOwner: DirectConversationStore.Locked,
        queryTerms: List<String>,
        speakerScope: SpeakerScope,
        backend: Backend,
        excludedEventRefs: Set<String> = emptySet(),
        sourceLifecycleBindingRef: String =
            UnboundSourceLifecycleBindingRef,
    ): LocateOutcome {
        requirePortableRef(
            sourceLifecycleBindingRef,
            "sourceLifecycleBindingRef",
        )
        val normalizedTerms =
            queryTerms
                .map(
                    ::normalize,
                )
                .filter {
                    it.isNotEmpty()
                }
                .distinct()
        val currentHead =
            conversationOwner
                .immediatePriorCompletedTurn()
                ?.head
        val currentHeadSha =
            currentHead
                ?.conversationHeadSha256

        if (
            normalizedTerms.isEmpty() ||
            normalizedTerms.size >
                MaximumQueryTerms ||
            normalizedTerms.any {
                it.length !in
                    3..MaximumQueryTermCharacters
            }
        ) {
            return LocateOutcome.FallbackRequired(
                reason = FallbackReason.UNSUPPORTED_QUERY,
                currentConversationHeadSha256 = currentHeadSha,
                generationConversationHeadSha256 = null,
            )
        }

        val generation =
            when (
                val load =
                    backend.load()
            ) {
                BackendLoad.Absent ->
                    return LocateOutcome.FallbackRequired(
                        reason = FallbackReason.ABSENT,
                        currentConversationHeadSha256 = currentHeadSha,
                        generationConversationHeadSha256 = null,
                    )

                is BackendLoad.Corrupt ->
                    return LocateOutcome.FallbackRequired(
                        reason = FallbackReason.CORRUPT,
                        currentConversationHeadSha256 = currentHeadSha,
                        generationConversationHeadSha256 = null,
                    )

                is BackendLoad.Ready ->
                    try {
                        requireGeneration(
                            load.generation,
                        )
                        load.generation
                    } catch (
                        _: RuntimeException,
                    ) {
                        return LocateOutcome.FallbackRequired(
                            reason = FallbackReason.CORRUPT,
                            currentConversationHeadSha256 = currentHeadSha,
                            generationConversationHeadSha256 =
                                load.generation
                                    .sourceConversationHeadSha256,
                        )
                    }
            }

        if (
            generation.fingerprintBindingRef !=
                backend.fingerprintBindingRef
        ) {
            return LocateOutcome.FallbackRequired(
                reason =
                    FallbackReason.FINGERPRINT_BINDING_MISMATCH,
                currentConversationHeadSha256 = currentHeadSha,
                generationConversationHeadSha256 =
                    generation.sourceConversationHeadSha256,
            )
        }

        if (
            generation.sourceLifecycleBindingRef !=
                sourceLifecycleBindingRef
        ) {
            return LocateOutcome.FallbackRequired(
                reason =
                    FallbackReason.SOURCE_LIFECYCLE_BINDING_MISMATCH,
                currentConversationHeadSha256 = currentHeadSha,
                generationConversationHeadSha256 =
                    generation.sourceConversationHeadSha256,
            )
        }

        if (
            generation.sourceConversationHeadSha256 !=
                currentHeadSha ||
            generation.sourceConversationSequence !=
                currentHead
                    ?.sequence
        ) {
            return LocateOutcome.FallbackRequired(
                reason = FallbackReason.STALE,
                currentConversationHeadSha256 = currentHeadSha,
                generationConversationHeadSha256 =
                    generation.sourceConversationHeadSha256,
            )
        }

        val queryFingerprints =
            normalizedTerms.associateWith {
                term ->
                trigramFingerprints(
                    normalized = term,
                    backend = backend,
                )
            }
        val indexedLookup =
            (backend as? IndexedBackend)
                ?.candidateEntryIndexes(
                    queryFingerprints =
                        normalizedTerms.map(
                            queryFingerprints::getValue,
                        ),
                    speakerScope =
                        speakerScope,
                )
        val candidateEntries =
            if (
                indexedLookup !=
                null
            ) {
                indexedLookup.entryIndexes
                    .asSequence()
                    .sorted()
                    .map {
                        index ->
                        require(
                            index in
                                generation.entries.indices
                        ) {
                            "indexed locator posting points outside the current generation"
                        }
                        generation.entries[
                            index
                        ]
                    }
            } else {
                generation.entries
                    .asSequence()
            }
        val derivedEntryScanCount =
            if (
                indexedLookup ==
                null
            ) {
                generation.entries.size
            } else {
                0
            }
        val derivedPostingLookupCount =
            indexedLookup
                ?.postingLookupCount
                ?: 0
        val approximate =
            candidateEntries
                .filter {
                    entry ->
                    entry.coordinates.request.eventRef !in
                        excludedEventRefs &&
                        entry.coordinates.response.eventRef !in
                        excludedEventRefs
                }
                .mapNotNull {
                    entry ->
                    val searchable =
                        when (
                            speakerScope
                        ) {
                            SpeakerScope.HUMAN ->
                                entry.requestFingerprints

                            SpeakerScope.VEX ->
                                entry.responseFingerprints

                            SpeakerScope.ANY ->
                                entry.requestFingerprints +
                                    entry.responseFingerprints
                        }
                    val matched =
                        normalizedTerms.filter {
                            term ->
                            searchable.containsAll(
                                queryFingerprints.getValue(
                                    term,
                                ),
                            )
                        }
                    if (
                        matched.isEmpty()
                    ) {
                        null
                    } else {
                        ApproximateCandidate(
                            entry = entry,
                            matchedTerms = matched,
                        )
                    }
                }
                .toList()

        if (
            approximate.isEmpty()
        ) {
            return LocateOutcome.CurrentMiss(
                currentConversationHeadSha256 = currentHeadSha,
                generationConversationHeadSha256 =
                    generation.sourceConversationHeadSha256,
                derivedEntryScanCount =
                    derivedEntryScanCount,
                derivedPostingLookupCount =
                    derivedPostingLookupCount,
            )
        }

        if (
            approximate.size >
                MaximumExactCandidateReopens
        ) {
            return LocateOutcome.FallbackRequired(
                reason = FallbackReason.CANDIDATE_SET_TOO_LARGE,
                currentConversationHeadSha256 = currentHeadSha,
                generationConversationHeadSha256 =
                    generation.sourceConversationHeadSha256,
            )
        }

        val addresses =
            approximate
                .flatMap {
                    listOf(
                        it.entry.coordinates.request,
                        it.entry.coordinates.response,
                    )
                }
                .distinctBy {
                    it.eventRef
                }
                .sortedBy {
                    it.sequence
                }
        val exact =
            try {
                conversationOwner.exactCommittedConversationEvents(
                    addresses,
                )
            } catch (
                _: RuntimeException,
            ) {
                return LocateOutcome.FallbackRequired(
                    reason =
                        FallbackReason.SOURCE_COORDINATE_MISMATCH,
                    currentConversationHeadSha256 = currentHeadSha,
                    generationConversationHeadSha256 =
                        generation.sourceConversationHeadSha256,
                )
            }
        val byRef =
            exact.events.associateBy(
                DirectConversationStore.Event::eventRef,
            )
        val resolved =
            approximate
                .mapNotNull {
                    candidate ->
                    val request =
                        requireNotNull(
                            byRef[
                                candidate.entry.coordinates.request.eventRef
                            ],
                        )
                    val response =
                        requireNotNull(
                            byRef[
                                candidate.entry.coordinates.response.eventRef
                            ],
                        )
                    requireCompletedPair(
                        request = request,
                        response = response,
                    )
                    val searchable =
                        normalize(
                            when (
                                speakerScope
                            ) {
                                SpeakerScope.HUMAN ->
                                    request.content

                                SpeakerScope.VEX ->
                                    response.content

                                SpeakerScope.ANY ->
                                    request.content +
                                        "\n" +
                                        response.content
                            },
                        )
                    val matched =
                        normalizedTerms.filter(
                            searchable::contains,
                        )
                    if (
                        matched.isEmpty()
                    ) {
                        null
                    } else {
                        ResolvedCandidate(
                            matchedTerms =
                                queryTerms.filter {
                                    original ->
                                    normalize(
                                        original,
                                    ) in
                                        matched
                                },
                            requestEvent = request,
                            responseEvent = response,
                        )
                    }
                }
                .sortedWith(
                    compareByDescending<ResolvedCandidate> {
                        it.matchedTerms.size
                    }
                        .thenByDescending {
                            it.matchedTerms.sumOf(
                                String::length,
                            )
                        }
                        .thenByDescending {
                            it.responseEvent.sequence
                        },
                )
                .take(
                    MaximumReturnedTurns,
                )

        if (
            resolved.isEmpty()
        ) {
            /*
             * Approximate fingerprints are deliberately allowed to nominate false positives, but an
             * exact-source mismatch must never become NOT_FOUND. Fall back to canonical scan so a
             * fingerprint collision/representation mismatch cannot create a false negative.
             */
            return LocateOutcome.FallbackRequired(
                reason =
                    FallbackReason.EXACT_VERIFICATION_MISS,
                currentConversationHeadSha256 = currentHeadSha,
                generationConversationHeadSha256 =
                    generation.sourceConversationHeadSha256,
            )
        }

        return LocateOutcome.Found(
            currentConversationHeadSha256 = currentHeadSha,
            generationConversationHeadSha256 =
                generation.sourceConversationHeadSha256,
            candidates = resolved,
            exactEventFileReadCount =
                exact.exactEventFileReadCount,
            exactHeadFileReadCount =
                exact.exactHeadFileReadCount,
            wholeHistoryHeadTraversalPerformed =
                exact.wholeHistoryHeadTraversalPerformed,
            derivedEntryScanCount =
                derivedEntryScanCount,
            derivedPostingLookupCount =
                derivedPostingLookupCount,
        )
    }

    private data class ApproximateCandidate(
        val entry: Entry,
        val matchedTerms: List<String>,
    )

    private fun requireGeneration(
        generation: Generation,
    ) {
        require(
            generation.schemaVersion ==
                GenerationSchema,
        ) {
            "continuity source locator generation schema is not current"
        }
        requirePortableRef(
            generation.fingerprintBindingRef,
            "generation fingerprintBindingRef",
        )
        requirePortableRef(
            generation.sourceLifecycleBindingRef,
            "generation sourceLifecycleBindingRef",
        )
        require(
            generation.sourceEventCount >=
                0 &&
                generation.sourceEventCount %
                2 ==
                0 &&
                generation.completedTurnCount ==
                generation.sourceEventCount /
                    2 &&
                generation.entries.size ==
                generation.completedTurnCount,
        ) {
            "continuity source locator generation cardinality is invalid"
        }
        if (
            generation.sourceConversationHeadSha256 ==
                null
        ) {
            require(
                generation.sourceConversationSequence ==
                    null &&
                    generation.sourceEventCount ==
                    0
            ) {
                "continuity source locator empty generation has source coordinates"
            }
        } else {
            require(
                generation.sourceConversationHeadSha256.matches(
                    Regex(
                        "[0-9a-f]{64}",
                    ),
                ) &&
                    generation.sourceConversationSequence !=
                    null &&
                    generation.sourceConversationSequence >=
                    1
            ) {
                "continuity source locator generation head coordinates are invalid"
            }
        }

        val refs =
            generation.entries.flatMap {
                listOf(
                    it.coordinates.request.eventRef,
                    it.coordinates.response.eventRef,
                )
            }
        require(
            refs.distinct().size ==
                refs.size
        ) {
            "continuity source locator generation contains duplicate source coordinates"
        }
        generation.entries.forEach {
            entry ->
            require(
                entry.coordinates.request.sequence >=
                    0 &&
                    entry.coordinates.request.sequence <
                    entry.coordinates.response.sequence &&
                    entry.coordinates.response.sequence ==
                    entry.coordinates.request.sequence +
                        1 &&
                    entry.coordinates.request.eventRef.isNotBlank() &&
                    entry.coordinates.response.eventRef.isNotBlank() &&
                    entry.coordinates.request.eventHash.matches(
                        Regex(
                            "[0-9a-f]{64}",
                        ),
                    ) &&
                    entry.coordinates.response.eventHash.matches(
                        Regex(
                            "[0-9a-f]{64}",
                        ),
                    ) &&
                    entry.requestFingerprints.all(
                        ::isFingerprint,
                    ) &&
                    entry.responseFingerprints.all(
                        ::isFingerprint,
                    )
            ) {
                "continuity source locator entry is invalid"
            }
        }
    }

    private fun requirePortableRef(
        value: String,
        field: String,
    ) {
        require(
            value.matches(
                Regex(
                    "[a-z][a-z0-9.-]*",
                ),
            ),
        ) {
            "$field must be one portable ref"
        }
    }

    private fun isFingerprint(
        value: String,
    ): Boolean =
        value.matches(
            Regex(
                "[0-9a-f]{16}",
            ),
        )

    private fun fingerprints(
        content: String,
        backend: Backend,
    ): Set<String> =
        trigramFingerprints(
            normalized =
                normalize(
                    content,
                ),
            backend = backend,
        )

    private fun trigramFingerprints(
        normalized: String,
        backend: Backend,
    ): Set<String> {
        if (
            normalized.length <
                3
        ) {
            return emptySet()
        }
        return normalized
            .windowed(
                size = 3,
                step = 1,
                partialWindows = false,
            )
            .map(
                backend::fingerprint,
            )
            .toSet()
    }

    private fun normalize(
        value: String,
    ): String =
        value
            .trim()
            .lowercase()
            .replace(
                Regex(
                    "\\s+",
                ),
                " ",
            )

    private fun address(
        event: DirectConversationStore.Event,
    ): DirectConversationStore.EventAddress =
        DirectConversationStore.EventAddress(
            sequence = event.sequence,
            eventRef = event.eventRef,
            eventHash = event.eventHash,
        )

    private fun requireCompletedPair(
        request: DirectConversationStore.Event,
        response: DirectConversationStore.Event,
    ) {
        require(
            request.eventKind ==
                "REQUEST" &&
                response.eventKind ==
                "RESPONSE" &&
                response.sequence ==
                request.sequence +
                    1 &&
                response.priorEventHash ==
                request.eventHash &&
                response.turnRef ==
                request.turnRef &&
                response.threadRef ==
                request.threadRef
        ) {
            "continuity source locator requires one exact canonical completed Conversation pair"
        }
    }
}

// [VXG RealForever]
