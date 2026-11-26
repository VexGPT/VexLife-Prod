package vexlife.conversation

/**
 * Volatile exact-coordinate accelerator for Direct Conversation send-attempt identity.
 *
 * The index contains no Conversation body and owns no canonical truth. It is rebuilt from one
 * already-validated [DirectConversationStore.Snapshot] at Product service construction, then the
 * sole production Conversation event writer advances it in memory as REQUEST/RESPONSE/head effects
 * become durable.
 *
 * Permanent:
 * - ATTEMPT INDEX != CONVERSATION
 * - INDEX MISS != SOURCE ABSENCE unless the generation is current
 * - INDEX LOSS != CONVERSATION LOSS
 * - INDEX HIT != SOURCE VERIFIED
 *
 * A Product restart may discard this entire object and rebuild it from canonical Conversation.
 */
internal class DirectConversationAttemptIndex {
    data class Entry internal constructor(
        val requestMessageRef: String,
        val turnRef: String,
        val request: DirectConversationStore.EventAddress,
        val response: DirectConversationStore.EventAddress?,
        val committed: Boolean,
    )

    data class Generation internal constructor(
        val sourceConversationHeadSha256: String?,
        val sourceConversationSequence: Long?,
        val sourceEventCount: Int,
        val entriesByRequestMessageRef: Map<String, Entry>,
    )

    sealed interface Load {
        data object Absent : Load

        data class Ready(
            val generation: Generation,
        ) : Load
    }

    data class RebuildReceipt(
        val sourceConversationHeadSha256: String?,
        val sourceEventCount: Int,
        val attemptCount: Int,
        val wholeHistoryEventEnumerationPerformed: Boolean,
        val canonicalSourceMutationPerformed: Boolean = false,
        val derivedIndexMutationPerformed: Boolean = true,
    )

    private val lock =
        Any()

    private var generation: Generation? =
        null

    fun load(): Load =
        synchronized(
            lock,
        ) {
            generation
                ?.let(
                    Load::Ready,
                )
                ?: Load.Absent
        }

    fun rebuild(
        snapshot: DirectConversationStore.Snapshot,
    ): RebuildReceipt =
        synchronized(
            lock,
        ) {
            require(
                snapshot.canonicalEvents.size %
                    2 ==
                    0,
            ) {
                "Conversation attempt index rebuild requires complete canonical pairs"
            }

            val entries =
                linkedMapOf<String, Entry>()

            var index =
                0
            while (
                index <
                    snapshot.canonicalEvents.size
            ) {
                val request =
                    snapshot.canonicalEvents[
                        index
                    ]
                val response =
                    snapshot.canonicalEvents[
                        index +
                            1
                    ]
                requirePair(
                    request = request,
                    response = response,
                )
                putUnique(
                    entries = entries,
                    entry =
                        Entry(
                            requestMessageRef = request.messageRef,
                            turnRef = request.turnRef,
                            request =
                                address(
                                    request,
                                ),
                            response =
                                address(
                                    response,
                                ),
                            committed = true,
                        ),
                )
                index +=
                    2
            }

            snapshot.abandonedAttempts.forEach {
                attempt ->
                attempt.responseEventOrNull
                    ?.let {
                        response ->
                        requirePair(
                            request = attempt.requestEvent,
                            response = response,
                        )
                    }
                putUnique(
                    entries = entries,
                    entry =
                        Entry(
                            requestMessageRef =
                                attempt.requestEvent.messageRef,
                            turnRef =
                                attempt.requestEvent.turnRef,
                            request =
                                address(
                                    attempt.requestEvent,
                                ),
                            response =
                                attempt.responseEventOrNull
                                    ?.let(
                                        ::address,
                                    ),
                            committed = false,
                        ),
                )
            }

            val rebuilt =
                Generation(
                    sourceConversationHeadSha256 =
                        snapshot.head
                            ?.conversationHeadSha256,
                    sourceConversationSequence =
                        snapshot.head
                            ?.sequence,
                    sourceEventCount =
                        snapshot.events.size,
                    entriesByRequestMessageRef =
                        entries.toMap(),
                )
            generation =
                rebuilt

            RebuildReceipt(
                sourceConversationHeadSha256 =
                    rebuilt.sourceConversationHeadSha256,
                sourceEventCount =
                    rebuilt.sourceEventCount,
                attemptCount =
                    rebuilt.entriesByRequestMessageRef.size,
                wholeHistoryEventEnumerationPerformed = true,
            )
        }

    /**
     * Advance the volatile generation after one exact REQUEST is durable.
     *
     * Failure to preserve currentness invalidates only the derived index; it never fails or rewrites
     * the canonical REQUEST that already became durable.
     */
    fun noteRequest(
        sourceConversationHeadSha256: String?,
        request: DirectConversationStore.Event,
    ): Boolean =
        mutateOrInvalidate {
            before ->
            require(
                before.sourceConversationHeadSha256 ==
                    sourceConversationHeadSha256
            ) {
                "Conversation attempt index request source head is stale"
            }
            require(
                request.eventKind ==
                    VexLifeConversationContract.RequestKind &&
                    request.messageRef !in
                    before.entriesByRequestMessageRef
            ) {
                "Conversation attempt index request identity is not append-only"
            }

            before.copy(
                sourceEventCount =
                    before.sourceEventCount +
                        1,
                entriesByRequestMessageRef =
                    before.entriesByRequestMessageRef +
                        (
                            request.messageRef to
                                Entry(
                                    requestMessageRef =
                                        request.messageRef,
                                    turnRef =
                                        request.turnRef,
                                    request =
                                        address(
                                            request,
                                        ),
                                    response = null,
                                    committed = false,
                                )
                        ),
            )
        }

    fun noteResponse(
        request: DirectConversationStore.Event,
        response: DirectConversationStore.Event,
    ): Boolean =
        mutateOrInvalidate {
            before ->
            requirePair(
                request = request,
                response = response,
            )
            val existing =
                requireNotNull(
                    before.entriesByRequestMessageRef[
                        request.messageRef
                    ],
                ) {
                    "Conversation attempt index response has no request identity"
                }
            require(
                existing.request ==
                    address(
                        request,
                    ) &&
                    !existing.committed &&
                    (
                        existing.response ==
                            null ||
                            existing.response ==
                            address(
                                response,
                            )
                    )
            ) {
                "Conversation attempt index response does not extend its exact request"
            }

            before.copy(
                sourceEventCount =
                    before.sourceEventCount +
                        if (
                            existing.response ==
                                null
                        ) {
                            1
                        } else {
                            0
                        },
                entriesByRequestMessageRef =
                    before.entriesByRequestMessageRef +
                        (
                            request.messageRef to
                                existing.copy(
                                    response =
                                        address(
                                            response,
                                        ),
                                )
                        ),
            )
        }

    fun noteCommitted(
        head: DirectConversationStore.Head,
        request: DirectConversationStore.Event,
        response: DirectConversationStore.Event,
    ): Boolean =
        mutateOrInvalidate {
            before ->
            requirePair(
                request = request,
                response = response,
            )
            val existing =
                requireNotNull(
                    before.entriesByRequestMessageRef[
                        request.messageRef
                    ],
                ) {
                    "Conversation attempt index commit has no request identity"
                }
            require(
                existing.request ==
                    address(
                        request,
                    ) &&
                    (
                        existing.response ==
                            null ||
                            existing.response ==
                            address(
                                response,
                            )
                    ) &&
                    head.turnRef ==
                    request.turnRef &&
                    head.requestEventHash ==
                    request.eventHash &&
                    head.eventHash ==
                    response.eventHash
            ) {
                "Conversation attempt index commit does not bind its exact pair/head"
            }

            before.copy(
                sourceConversationHeadSha256 =
                    head.conversationHeadSha256,
                sourceConversationSequence =
                    head.sequence,
                entriesByRequestMessageRef =
                    before.entriesByRequestMessageRef +
                        (
                            request.messageRef to
                                existing.copy(
                                    response =
                                        address(
                                            response,
                                        ),
                                    committed = true,
                                )
                        ),
            )
        }

    fun invalidate() {
        synchronized(
            lock,
        ) {
            generation =
                null
        }
    }

    private fun mutateOrInvalidate(
        transform: (Generation) -> Generation,
    ): Boolean =
        synchronized(
            lock,
        ) {
            val before =
                generation
                    ?: return@synchronized false
            return@synchronized try {
                generation =
                    transform(
                        before,
                    )
                true
            } catch (
                _: RuntimeException,
            ) {
                generation =
                    null
                false
            }
        }

    private fun putUnique(
        entries: MutableMap<String, Entry>,
        entry: Entry,
    ) {
        require(
            entries.put(
                entry.requestMessageRef,
                entry,
            ) ==
                null
        ) {
            "Conversation attempt index rebuild observed duplicate request message identity"
        }
    }

    private fun requirePair(
        request: DirectConversationStore.Event,
        response: DirectConversationStore.Event,
    ) {
        require(
            request.eventKind ==
                VexLifeConversationContract.RequestKind &&
                response.eventKind ==
                VexLifeConversationContract.ResponseKind &&
                response.turnRef ==
                request.turnRef &&
                response.sequence ==
                request.sequence +
                1 &&
                response.priorEventHash ==
                request.eventHash
        ) {
            "Conversation attempt index requires one exact request/response pair"
        }
    }

    private fun address(
        event: DirectConversationStore.Event,
    ): DirectConversationStore.EventAddress =
        DirectConversationStore.EventAddress(
            sequence =
                event.sequence,
            eventRef =
                event.eventRef,
            eventHash =
                event.eventHash,
        )
}

// [VXG RealForever]
