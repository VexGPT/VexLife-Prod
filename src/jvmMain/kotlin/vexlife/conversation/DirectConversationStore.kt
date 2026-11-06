package vexlife.conversation

import java.net.URI
import java.nio.charset.StandardCharsets
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import java.time.Instant
import java.util.UUID
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import vexlife.contract.VexContractJson
import vexlife.contract.VexDurableContractFile
import vexlife.filesystem.VexOwnedPath
import vexlife.home.VexLifeProdHomeFiles
import vexlife.model.heretic.HereticProductIdentity

/**
 * Durable direct personal Conversation owner over the canonical lived VexLife Home.
 *
 * This class preserves the accepted VexLife event/context/head vocabulary and lineage on disk while
 * exposing a Kotlin convenience projection to callers. It does not own Memory, provider invocation,
 * runtime evidence meaning or presentation.
 *
 * Current head advances only after the exact REQUEST, RESPONSE and bounded canonical context are
 * durable. Historical semantic-relay v2 events remain explicitly held until their exact relay
 * validator is ported; they are never silently downgraded to v1.
 */
class DirectConversationStore(
    productHome: Path,
) {
    companion object {
        const val EventSchema =
            VexLifeConversationContract.EventSchemaV1

        const val HeadSchema =
            VexLifeConversationContract.HeadSchema

        const val ContextSchema =
            VexLifeConversationContract.ContextSchema

        const val ThreadWriterLeaseSchema =
            "vexlife.thread-writer-lease/v1"

        private const val InstancePrefix =
            "instance.vexlife.local-process"
    }

    /**
     * Kotlin convenience projection of one canonical durable event.
     *
     * Compatibility aliases ending in Sha256 retain the pre-realignment caller surface only; the
     * durable VexLife field is eventHash/priorEventHash/contentHash.
     */
    data class Event internal constructor(
        val schemaVersion: String,
        val eventKind: String,
        val eventRef: String,
        val eventHash: String,
        val homeRef: String,
        val deviceRef: String,
        val companionLineageRef: String,
        val instanceRef: String,
        val threadRef: String,
        val channelRef: String,
        val turnRef: String,
        val messageRef: String,
        val speakerRef: String,
        val recipientRefs: List<String>,
        val sequence: Long,
        val priorEventHash: String?,
        val content: String,
        val contentHash: String,
        val endpointProfileRef: String?,
        val sanitizedEndpointOrigin: String?,
        val modelNameOrBoundedTestProfileRef: String?,
        val privacyClass: String,
        val formedAt: String,
    ) {
        val eventSha256: String
            get() =
                eventHash

        val priorEventSha256: String?
            get() =
                priorEventHash

        val contentSha256: String
            get() =
                contentHash

        val endpointOrigin: String?
            get() =
                sanitizedEndpointOrigin

        val model: String?
            get() =
                modelNameOrBoundedTestProfileRef
    }

    /**
     * Kotlin projection of the canonical head plus the exact request event hash discovered from the
     * bound event pair. requestEventHash is not a persisted Head field in current VexLife.
     */
    data class Head internal constructor(
        val schemaVersion: String,
        val conversationHeadSha256: String,
        val homeRef: String,
        val deviceRef: String,
        val companionLineageRef: String,
        val instanceRef: String,
        val threadRef: String,
        val turnRef: String,
        val requestMessageRef: String,
        val responseMessageRef: String,
        val requestEventHash: String,
        val eventHash: String,
        val contextSha256: String,
        val contextPath: String,
        val sequence: Long,
        val priorConversationHeadSha256: String?,
        val formedAt: String,
    ) {
        val requestEventSha256: String
            get() =
                requestEventHash

        val responseEventSha256: String
            get() =
                eventHash
    }

    @ConsistentCopyVisibility
    data class AbandonedAttempt internal constructor(
        val requestEvent: Event,
        val responseEventOrNull: Event?,
    )

    data class Snapshot(
        val head: Head?,
        val events: List<Event>,
        val canonicalEvents: List<Event>,
        val abandonedAttempts: List<AbandonedAttempt>,
    )

    @ConsistentCopyVisibility
    data class ImmediatePriorCompletedTurn internal constructor(
        val head: Head,
        val requestEvent: Event,
        val responseEvent: Event,
        val exactEventFileReadCount: Int,
        val wholeHistoryEventEnumerationPerformed: Boolean,
    )

    @ConsistentCopyVisibility
    data class HistoricalCompletedTurn internal constructor(
        val head: Head,
        val requestEvent: Event,
        val responseEvent: Event,
        val exactEventFileReadCount: Int,
        val currentCommittedLineageVerified: Boolean,
    )

    @Serializable
    private data class ThreadWriterLeaseCore(
        val schemaVersion: String,
        val companionLineageRef: String,
        val threadRef: String,
        val instanceRef: String,
        val lockToken: String,
        val pid: Long,
        val formedAt: String,
    )

    @Serializable
    private data class ThreadWriterLease(
        val schemaVersion: String,
        val companionLineageRef: String,
        val threadRef: String,
        val instanceRef: String,
        val lockToken: String,
        val pid: Long,
        val formedAt: String,
        val leaseSha256: String,
    )

    private inner class HeldThreadWriterLease(
        private val lease: ThreadWriterLease,
    ) : AutoCloseable {
        private var closed =
            false

        override fun close() {
            if (
                closed
            ) {
                return
            }
            closed =
                true

            val current =
                requireNotNull(
                    VexDurableContractFile.read<ThreadWriterLease>(
                        ownedRoot = productHome,
                        path = lockPath,
                    ),
                ) {
                    "canonical thread writer lease disappeared before release"
                }
            require(
                current ==
                    lease &&
                    validateThreadLease(
                        current,
                    )
            ) {
                "canonical thread writer lease changed before release"
            }
            require(
                Files.deleteIfExists(
                    lockPath,
                ),
            ) {
                "canonical thread writer lease could not be released"
            }
        }
    }

    inner class Locked internal constructor(
        val instanceRef: String,
    ) {
        fun current(): Snapshot =
            readCurrentComplete()

        fun immediatePriorCompletedTurn(): ImmediatePriorCompletedTurn? =
            readImmediatePriorCompletedTurn()

        fun beginRequest(
            turnRef: String,
            requestMessageRef: String,
            content: String,
            formedAt: Instant = Instant.now(),
        ): Event {
            requireSafeRef(
                turnRef,
                "turnRef",
            )
            requireNonEmpty(
                requestMessageRef,
                "requestMessageRef",
            )
            require(
                content.isNotEmpty(),
            ) {
                "direct Conversation request content must be non-empty"
            }

            val before =
                readCurrentComplete()

            require(
                before.events.none {
                    it.turnRef ==
                        turnRef ||
                        it.messageRef ==
                        requestMessageRef
                },
            ) {
                "direct Conversation turn/message identity is already durable; same failed attempt must not replay"
            }

            val sequence =
                before.head
                    ?.sequence
                    ?.plus(
                        1,
                    )
                    ?: 0L
            val prior =
                before.head
                    ?.eventHash
            val eventRef =
                "event.vexlife.request." +
                    UUID.randomUUID()
                        .toString()
                        .lowercase()
            val core =
                VexLifeConversationContract.RequestCoreV1(
                    schemaVersion = VexLifeConversationContract.EventSchemaV1,
                    eventRef = eventRef,
                    eventKind = VexLifeConversationContract.RequestKind,
                    homeRef = manifest.homeRef,
                    deviceRef = manifest.currentDeviceRef,
                    companionLineageRef = manifest.currentCompanionLineageRef,
                    instanceRef = instanceRef,
                    threadRef = DirectConversationIdentity.ThreadRef,
                    channelRef = DirectConversationIdentity.ChannelRef,
                    turnRef = turnRef,
                    messageRef = requestMessageRef,
                    speakerRef = installation.personRef,
                    recipientRefs =
                        listOf(
                            DirectConversationIdentity.CompanionRoleRef,
                        ),
                    sequence = sequence,
                    priorEventHash = prior,
                    content = content,
                    contentHash =
                        contentHash(
                            content,
                        ),
                    privacyClass = VexLifeConversationContract.PrivacyClass,
                    formedAt = formedAt.toString(),
                )
                    .let(
                        VexLifeConversationContract::validateRequestCore,
                    )
            val value =
                VexLifeConversationContract.RequestV1(
                    schemaVersion = core.schemaVersion,
                    eventRef = core.eventRef,
                    eventKind = core.eventKind,
                    homeRef = core.homeRef,
                    deviceRef = core.deviceRef,
                    companionLineageRef = core.companionLineageRef,
                    instanceRef = core.instanceRef,
                    threadRef = core.threadRef,
                    channelRef = core.channelRef,
                    turnRef = core.turnRef,
                    messageRef = core.messageRef,
                    speakerRef = core.speakerRef,
                    recipientRefs = core.recipientRefs,
                    sequence = core.sequence,
                    priorEventHash = core.priorEventHash,
                    content = core.content,
                    contentHash = core.contentHash,
                    privacyClass = core.privacyClass,
                    formedAt = core.formedAt,
                    eventHash =
                        semanticHash(
                            core,
                        ),
                )
            val event =
                requestProjection(
                    value,
                )

            validateEvent(
                event,
            )
            writeEvent(
                event,
            )

            return event
        }

        /**
         * Canonical RESPONSE formation. Runtime-observation/invocation refs remain arguments only so
         * the caller can bind the same evidence into the canonical context owner immediately after
         * this append; current VexLife RESPONSE bytes do not persist those product-only fields.
         */
        fun appendResponse(
            request: Event,
            responseMessageRef: String,
            content: String,
            model: String,
            endpointOrigin: String,
            runtimeObservationRef: String,
            invocationEvidenceRef: String,
            endpointProfileRef: String =
                HereticProductIdentity.CompanionEndpointProfileRef,
            formedAt: Instant = Instant.now(),
        ): Event {
            validateRequestAsPending(
                request,
            )
            requireNonEmpty(
                responseMessageRef,
                "responseMessageRef",
            )
            requireNonEmpty(
                runtimeObservationRef,
                "runtimeObservationRef",
            )
            requireNonEmpty(
                invocationEvidenceRef,
                "invocationEvidenceRef",
            )
            requireNonEmpty(
                endpointProfileRef,
                "endpointProfileRef",
            )
            require(
                content.isNotEmpty(),
            ) {
                "direct Conversation response content must be non-empty"
            }
            requireNonEmpty(
                model,
                "model",
            )
            requireLoopbackOrigin(
                endpointOrigin,
            )

            val eventRef =
                "event.vexlife.response." +
                    UUID.randomUUID()
                        .toString()
                        .lowercase()
            val core =
                VexLifeConversationContract.ResponseCoreV1(
                    schemaVersion = VexLifeConversationContract.EventSchemaV1,
                    eventRef = eventRef,
                    eventKind = VexLifeConversationContract.ResponseKind,
                    homeRef = manifest.homeRef,
                    deviceRef = manifest.currentDeviceRef,
                    companionLineageRef = manifest.currentCompanionLineageRef,
                    instanceRef = request.instanceRef,
                    threadRef = request.threadRef,
                    channelRef = request.channelRef,
                    turnRef = request.turnRef,
                    messageRef = responseMessageRef,
                    speakerRef = DirectConversationIdentity.CompanionRoleRef,
                    recipientRefs =
                        listOf(
                            installation.personRef,
                        ),
                    sequence =
                        request.sequence +
                            1,
                    priorEventHash = request.eventHash,
                    content = content,
                    contentHash =
                        contentHash(
                            content,
                        ),
                    endpointProfileRef = endpointProfileRef,
                    sanitizedEndpointOrigin = endpointOrigin,
                    modelNameOrBoundedTestProfileRef = model,
                    privacyClass = VexLifeConversationContract.PrivacyClass,
                    formedAt = formedAt.toString(),
                )
                    .let(
                        VexLifeConversationContract::validateResponseCore,
                    )
            val value =
                VexLifeConversationContract.ResponseV1(
                    schemaVersion = core.schemaVersion,
                    eventRef = core.eventRef,
                    eventKind = core.eventKind,
                    homeRef = core.homeRef,
                    deviceRef = core.deviceRef,
                    companionLineageRef = core.companionLineageRef,
                    instanceRef = core.instanceRef,
                    threadRef = core.threadRef,
                    channelRef = core.channelRef,
                    turnRef = core.turnRef,
                    messageRef = core.messageRef,
                    speakerRef = core.speakerRef,
                    recipientRefs = core.recipientRefs,
                    sequence = core.sequence,
                    priorEventHash = core.priorEventHash,
                    content = core.content,
                    contentHash = core.contentHash,
                    endpointProfileRef = core.endpointProfileRef,
                    sanitizedEndpointOrigin = core.sanitizedEndpointOrigin,
                    modelNameOrBoundedTestProfileRef = core.modelNameOrBoundedTestProfileRef,
                    privacyClass = core.privacyClass,
                    formedAt = core.formedAt,
                    eventHash =
                        semanticHash(
                            core,
                        ),
                )
            val response =
                responseProjection(
                    value,
                )

            validateEvent(
                response,
            )
            writeEvent(
                response,
            )

            return response
        }

        /**
         * Advance the canonical current head only after the context owner has durably formed the
         * exact canonical context at its current VexLife path.
         *
         */
        fun commitCompletedTurn(
            request: Event,
            response: Event,
            contextSha256: String,
            formedAt: Instant = Instant.now(),
        ): Head {
            validateRequestAsPending(
                request,
            )
            validateEvent(
                response,
            )
            require(
                response.eventKind ==
                    VexLifeConversationContract.ResponseKind &&
                    response.turnRef ==
                    request.turnRef &&
                    response.instanceRef ==
                    request.instanceRef &&
                    response.channelRef ==
                    request.channelRef &&
                    response.sequence ==
                    request.sequence +
                    1 &&
                    response.priorEventHash ==
                    request.eventHash,
            ) {
                "direct Conversation completed head requires the exact response to its pending request"
            }
            requireSha256(
                contextSha256,
                "contextSha256",
            )

            val durableResponse =
                readEventAt(
                    eventPath(
                        response,
                    ),
                )
            require(
                durableResponse ==
                    response,
            ) {
                "direct Conversation response is not durably exact before head commit"
            }

            val contextRelative =
                contextRelativePath(
                    request.turnRef,
                )
            validateContextBinding(
                relativePath = contextRelative,
                expectedContextSha256 = contextSha256,
                request = request,
                response = response,
            )

            val priorHead =
                readHeadRecord()
            val core =
                VexLifeConversationContract.HeadCore(
                    schemaVersion = VexLifeConversationContract.HeadSchema,
                    homeRef = manifest.homeRef,
                    deviceRef = manifest.currentDeviceRef,
                    companionLineageRef = manifest.currentCompanionLineageRef,
                    instanceRef = request.instanceRef,
                    threadRef = request.threadRef,
                    turnRef = request.turnRef,
                    requestMessageRef = request.messageRef,
                    responseMessageRef = response.messageRef,
                    eventHash = response.eventHash,
                    contextSha256 = contextSha256,
                    contextPath = contextRelative,
                    sequence = response.sequence,
                    priorConversationHeadSha256 =
                        priorHead
                            ?.conversationHeadSha256,
                    formedAt = formedAt.toString(),
                )
                    .let(
                        VexLifeConversationContract::validateHeadCore,
                    )
            val value =
                VexLifeConversationContract.Head(
                    schemaVersion = core.schemaVersion,
                    homeRef = core.homeRef,
                    deviceRef = core.deviceRef,
                    companionLineageRef = core.companionLineageRef,
                    instanceRef = core.instanceRef,
                    threadRef = core.threadRef,
                    turnRef = core.turnRef,
                    requestMessageRef = core.requestMessageRef,
                    responseMessageRef = core.responseMessageRef,
                    eventHash = core.eventHash,
                    contextSha256 = core.contextSha256,
                    contextPath = core.contextPath,
                    sequence = core.sequence,
                    priorConversationHeadSha256 = core.priorConversationHeadSha256,
                    formedAt = core.formedAt,
                    conversationHeadSha256 =
                        semanticHash(
                            core,
                        ),
                )
                    .let(
                        VexLifeConversationContract::validateHead,
                    )

            writeImmutableHead(
                value,
            )
            VexDurableContractFile.write(
                ownedRoot = productHome,
                path = currentHeadPath,
                value = value,
            )

            val current =
                readCurrentComplete()
            require(
                current.head
                    ?.conversationHeadSha256 ==
                    value.conversationHeadSha256,
            ) {
                "canonical Conversation current head did not commit exactly"
            }

            return requireNotNull(
                current.head,
            )
        }
    }

    private val productHome: Path =
        VexOwnedPath.requireRoot(
            productHome,
            "canonical VexLife Home",
        )

    private val home =
        requireNotNull(
            VexLifeProdHomeFiles.observeExisting(
                this.productHome,
            ),
        ) {
            "direct Conversation requires one current canonical VexLife Home"
        }

    private val manifest =
        home.manifest

    private val installation =
        home.installation

    private val conversationRoot: Path =
        VexOwnedPath.resolveInside(
            this.productHome,
            "conversations/" +
                manifest.currentCompanionLineageRef +
                "/" +
                DirectConversationIdentity.ThreadRef,
        )

    private val eventsDirectory: Path =
        VexOwnedPath.resolveInside(
            this.productHome,
            "conversations/" +
                manifest.currentCompanionLineageRef +
                "/" +
                DirectConversationIdentity.ThreadRef +
                "/events",
        )

    private val headsDirectory: Path =
        VexOwnedPath.resolveInside(
            this.productHome,
            "conversations/" +
                manifest.currentCompanionLineageRef +
                "/" +
                DirectConversationIdentity.ThreadRef +
                "/heads",
        )

    private val currentHeadPath: Path =
        VexOwnedPath.resolveInside(
            this.productHome,
            "conversations/" +
                manifest.currentCompanionLineageRef +
                "/" +
                DirectConversationIdentity.ThreadRef +
                "/head.json",
        )

    private val lockDirectory: Path =
        VexOwnedPath.resolveInside(
            this.productHome,
            "runtime/thread-writer-locks/" +
                manifest.currentCompanionLineageRef,
        )

    private val lockPath: Path =
        VexOwnedPath.resolveInside(
            this.productHome,
            "runtime/thread-writer-locks/" +
                manifest.currentCompanionLineageRef +
                "/" +
                DirectConversationIdentity.ThreadRef +
                ".lock",
        )

    fun <T> withWriterLease(
        instanceRef: String =
            currentProcessInstanceRef(),
        block: Locked.() -> T,
    ): T {
        prepareOwnedDirectories()
        requireSafeRef(
            instanceRef,
            "instanceRef",
        )

        return acquireThreadWriterLease(
            instanceRef,
        )
            .use {
                Locked(
                    instanceRef,
                )
                    .block()
            }
    }

    /**
     * Explicit stale-writer recovery. Absence/active ownership never becomes delete authority.
     */
    fun recoverAbandonedWriterLease(
        expectedAbandonedInstanceRef: String,
    ): Boolean {
        requireSafeRef(
            expectedAbandonedInstanceRef,
            "expectedAbandonedInstanceRef",
        )

        if (
            !Files.exists(
                lockPath,
                LinkOption.NOFOLLOW_LINKS,
            )
        ) {
            return false
        }

        val lease =
            requireNotNull(
                VexDurableContractFile.read<ThreadWriterLease>(
                    ownedRoot = productHome,
                    path = lockPath,
                ),
            )
        require(
            validateThreadLease(
                lease,
            ) &&
                lease.instanceRef ==
                expectedAbandonedInstanceRef,
        ) {
            "thread writer recovery evidence does not match the exact abandoned owner"
        }
        require(
            processState(
                lease.pid,
            ) ==
                "ABSENT",
        ) {
            "thread writer recovery requires an exactly absent prior process"
        }

        return Files.deleteIfExists(
            lockPath,
        )
    }

    fun current(): Snapshot =
        readCurrentComplete()

    /**
     * Verify one immutable historical completed head against the exact current committed lineage.
     *
     * This is a read-only Conversation question. Memory/Score consumers borrow the answer rather
     * than re-owning Conversation chain/context validation.
     */
    fun historicalCompletedTurn(
        conversationHeadSha256: String,
    ): HistoricalCompletedTurn {
        requireSha256(
            conversationHeadSha256,
            "conversationHeadSha256",
        )

        val current =
            requireNotNull(
                readHeadRecord(),
            ) {
                "historical Conversation verification requires one current completed head"
            }

        val lineage =
            mutableSetOf<String>()
        var cursor: String? =
            current.conversationHeadSha256
        while (
            cursor !=
                null
        ) {
            require(
                lineage.add(
                    cursor,
                ),
            ) {
                "canonical immutable head lineage contains a cycle"
            }
            val historical =
                requireNotNull(
                    VexDurableContractFile.read<VexLifeConversationContract.Head>(
                        ownedRoot = productHome,
                        path =
                            immutableHeadPath(
                                cursor,
                            ),
                    ),
                ) {
                    "canonical immutable head lineage is missing a historical receipt"
                }
            validateCanonicalHeadRecord(
                historical,
            )
            cursor =
                historical.priorConversationHeadSha256
        }

        require(
            conversationHeadSha256 in
                lineage,
        ) {
            "requested historical Conversation head is not in the exact current committed lineage"
        }

        val historical =
            requireNotNull(
                VexDurableContractFile.read<VexLifeConversationContract.Head>(
                    ownedRoot = productHome,
                    path =
                        immutableHeadPath(
                            conversationHeadSha256,
                        ),
                ),
            )
                .also(
                    ::validateCanonicalHeadRecord,
                )
        val response =
            requireNotNull(
                readEventAt(
                    eventPath(
                        sequence = historical.sequence,
                        eventHash = historical.eventHash,
                    ),
                ),
            ) {
                "historical Conversation response event is missing"
            }
        val request =
            requireNotNull(
                response.priorEventHash
                    ?.let {
                        requestHash ->
                        readEventAt(
                            eventPath(
                                sequence =
                                    historical.sequence -
                                        1,
                                eventHash = requestHash,
                            ),
                        )
                    },
            ) {
                "historical Conversation request event is missing"
            }

        validateHeadAgainstPair(
            head = historical,
            request = request,
            response = response,
        )
        validateContextBinding(
            relativePath = historical.contextPath,
            expectedContextSha256 = historical.contextSha256,
            request = request,
            response = response,
        )

        return HistoricalCompletedTurn(
            head =
                headProjection(
                    value = historical,
                    requestEventHash = request.eventHash,
                ),
            requestEvent = request,
            responseEvent = response,
            exactEventFileReadCount = 2,
            currentCommittedLineageVerified = true,
        )
    }

    private fun prepareOwnedDirectories() {
        for (
            directory in
            listOf(
                conversationRoot,
                eventsDirectory,
                headsDirectory,
                lockDirectory,
            )
        ) {
            VexOwnedPath.ensureDirectory(
                productHome,
                directory,
            )
        }
    }

    private fun readCurrentComplete(): Snapshot {
        val events =
            readEvents()
        val headRecord =
            readHeadRecord()

        if (
            headRecord ==
                null
        ) {
            val abandoned =
                classifyAbandonedAttempts(
                    events = events,
                    canonicalChain = emptyList(),
                )

            return Snapshot(
                head = null,
                events = events,
                canonicalEvents = emptyList(),
                abandonedAttempts = abandoned,
            )
        }

        val byHash =
            events.associateBy(
                Event::eventHash,
            )
        require(
            byHash.size ==
                events.size,
        ) {
            "canonical Conversation contains duplicate event hashes"
        }

        val chain =
            mutableListOf<Event>()
        val visited =
            mutableSetOf<String>()
        var cursor: String? =
            headRecord.eventHash

        while (
            cursor !=
                null
        ) {
            require(
                visited.add(
                    cursor,
                ),
            ) {
                "canonical Conversation event chain contains a cycle"
            }
            val event =
                requireNotNull(
                    byHash[
                        cursor
                    ],
                ) {
                    "canonical Conversation head references a missing event"
                }
            chain +=
                event
            cursor =
                event.priorEventHash
        }

        chain.reverse()
        validateCompletedChain(
            chain,
        )

        val finalResponse =
            chain.last()
        val finalRequest =
            chain[
                chain.lastIndex -
                    1
            ]

        validateHeadAgainstPair(
            head = headRecord,
            request = finalRequest,
            response = finalResponse,
        )
        validateContextBinding(
            relativePath = headRecord.contextPath,
            expectedContextSha256 = headRecord.contextSha256,
            request = finalRequest,
            response = finalResponse,
        )

        val immutable =
            requireNotNull(
                VexDurableContractFile.read<VexLifeConversationContract.Head>(
                    ownedRoot = productHome,
                    path =
                        immutableHeadPath(
                            headRecord.conversationHeadSha256,
                        ),
                ),
            ) {
                "canonical Conversation current head lacks its immutable receipt"
            }
                .also {
                    validateCanonicalHeadRecord(
                        it,
                    )
                }
        require(
            immutable ==
                headRecord,
        ) {
            "canonical Conversation current head differs from its immutable receipt"
        }

        validateImmutableHeadLineage(
            currentHead = headRecord,
            chain = chain,
        )

        val abandoned =
            classifyAbandonedAttempts(
                events = events,
                canonicalChain = chain,
            )

        return Snapshot(
            head =
                headProjection(
                    value = headRecord,
                    requestEventHash = finalRequest.eventHash,
                ),
            events = events,
            canonicalEvents = chain,
            abandonedAttempts = abandoned,
        )
    }

    private fun validateCompletedChain(
        chain: List<Event>,
    ) {
        require(
            chain.size >=
                2 &&
                chain.size %
                2 ==
                0,
        ) {
            "canonical completed Conversation must contain complete request/response pairs"
        }
        require(
            chain.first()
                .sequence ==
                0L &&
                chain.first()
                    .priorEventHash ==
                null,
        ) {
            "canonical completed Conversation must begin at genesis sequence zero"
        }

        chain.forEachIndexed {
            index,
            event,
            ->
            validateEvent(
                event,
            )
            require(
                event.sequence ==
                    index.toLong() &&
                    event.priorEventHash ==
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
                            .eventHash
                    },
            ) {
                "canonical Conversation sequence/prior-event lineage is not contiguous"
            }
        }

        for (
            index in
            chain.indices step
                2
        ) {
            val request =
                chain[
                    index
                ]
            val response =
                chain[
                    index +
                        1
                ]

            require(
                request.eventKind ==
                    VexLifeConversationContract.RequestKind &&
                    response.eventKind ==
                    VexLifeConversationContract.ResponseKind &&
                    request.turnRef ==
                    response.turnRef &&
                    request.instanceRef ==
                    response.instanceRef &&
                    request.channelRef ==
                    response.channelRef &&
                    response.speakerRef ==
                    request.recipientRefs.first() &&
                    response.recipientRefs ==
                    listOf(
                        request.speakerRef,
                    ),
            ) {
                "canonical Conversation request/response pair semantics are invalid"
            }
        }
    }

    private fun classifyAbandonedAttempts(
        events: List<Event>,
        canonicalChain: List<Event>,
    ): List<AbandonedAttempt> {
        val canonicalHashes =
            canonicalChain
                .map(
                    Event::eventHash,
                )
                .toSet()
        val extras =
            events.filter {
                it.eventHash !in
                    canonicalHashes
            }

        if (
            extras.isEmpty()
        ) {
            return emptyList()
        }

        val canonicalResponseBySequence =
            canonicalChain
                .filter {
                    it.eventKind ==
                        VexLifeConversationContract.ResponseKind
                }
                .associateBy(
                    Event::sequence,
                )

        val canonicalTurnRefs =
            canonicalChain
                .map(
                    Event::turnRef,
                )
                .toSet()

        return extras
            .groupBy(
                Event::turnRef,
            )
            .values
            .map {
                group ->
                group.forEach(
                    ::validateEvent,
                )
                require(
                    group.size in
                        1..2,
                ) {
                    "canonical failed branch may contain only one request and optional response"
                }

                val request =
                    requireNotNull(
                        group.singleOrNull {
                            it.eventKind ==
                                VexLifeConversationContract.RequestKind
                        },
                    ) {
                        "canonical off-head branch requires one REQUEST"
                    }
                val response =
                    group.singleOrNull {
                        it.eventKind ==
                            VexLifeConversationContract.ResponseKind
                    }

                require(
                    request.turnRef !in
                        canonicalTurnRefs
                ) {
                    "canonical off-head request reuses one committed turn identity"
                }

                val expectedPrior =
                    if (
                        request.sequence ==
                            0L
                    ) {
                        null
                    } else {
                        requireNotNull(
                            canonicalResponseBySequence[
                                request.sequence -
                                    1
                            ],
                        ) {
                            "canonical failed request does not branch from one committed response"
                        }
                            .eventHash
                    }

                require(
                    request.priorEventHash ==
                        expectedPrior,
                ) {
                    "canonical failed request does not extend one exact historical completed head"
                }

                if (
                    response !=
                        null
                ) {
                    require(
                        response.sequence ==
                            request.sequence +
                                1 &&
                            response.priorEventHash ==
                            request.eventHash &&
                            response.turnRef ==
                            request.turnRef &&
                            response.instanceRef ==
                            request.instanceRef &&
                            response.speakerRef ==
                            request.recipientRefs.first() &&
                            response.recipientRefs ==
                            listOf(
                                request.speakerRef,
                            ),
                    ) {
                        "canonical failed response does not bind its exact off-head request"
                    }
                }

                AbandonedAttempt(
                    requestEvent = request,
                    responseEventOrNull = response,
                )
            }
            .sortedWith(
                compareBy(
                    { it.requestEvent.sequence },
                    { it.requestEvent.eventHash },
                ),
            )
    }

    private fun validateImmutableHeadLineage(
        currentHead: VexLifeConversationContract.Head,
        chain: List<Event>,
    ) {
        val pairCount =
            chain.size /
                2
        val reversed =
            mutableListOf<VexLifeConversationContract.Head>()
        var cursor: String? =
            currentHead.conversationHeadSha256

        while (
            cursor !=
                null
        ) {
            require(
                reversed.none {
                    it.conversationHeadSha256 ==
                        cursor
                },
            ) {
                "canonical immutable head lineage contains a cycle"
            }

            val historical =
                requireNotNull(
                    VexDurableContractFile.read<VexLifeConversationContract.Head>(
                        ownedRoot = productHome,
                        path =
                            immutableHeadPath(
                                cursor,
                            ),
                    ),
                ) {
                    "canonical immutable head lineage is missing a historical receipt"
                }
                    .also {
                        validateCanonicalHeadRecord(
                            it,
                        )
                    }
            require(
                historical.conversationHeadSha256 ==
                    cursor,
            ) {
                "canonical immutable head is stored under the wrong identity"
            }

            reversed +=
                historical
            cursor =
                historical.priorConversationHeadSha256
        }

        reversed.reverse()
        require(
            reversed.size ==
                pairCount,
        ) {
            "canonical immutable head lineage count does not match completed turns"
        }

        reversed.forEachIndexed {
            index,
            historical,
            ->
            val request =
                chain[
                    index *
                        2
                ]
            val response =
                chain[
                    index *
                        2 +
                        1
                ]
            val expectedPrior =
                if (
                    index ==
                        0
                ) {
                    null
                } else {
                    reversed[
                        index -
                            1
                    ]
                        .conversationHeadSha256
                }

            require(
                historical.sequence ==
                    response.sequence &&
                    historical.eventHash ==
                    response.eventHash &&
                    historical.turnRef ==
                    request.turnRef &&
                    historical.instanceRef ==
                    request.instanceRef &&
                    historical.requestMessageRef ==
                    request.messageRef &&
                    historical.responseMessageRef ==
                    response.messageRef &&
                    historical.priorConversationHeadSha256 ==
                    expectedPrior,
            ) {
                "canonical immutable head lineage does not bind the exact completed pair"
            }
        }
    }

    private fun readImmediatePriorCompletedTurn(): ImmediatePriorCompletedTurn? {
        val headRecord =
            readHeadRecord()
                ?: return null

        val immutable =
            requireNotNull(
                VexDurableContractFile.read<VexLifeConversationContract.Head>(
                    ownedRoot = productHome,
                    path =
                        immutableHeadPath(
                            headRecord.conversationHeadSha256,
                        ),
                ),
            )
        validateCanonicalHeadRecord(
            immutable,
        )
        require(
            immutable ==
                headRecord,
        ) {
            "canonical current head differs from its immutable receipt"
        }

        val response =
            requireNotNull(
                readEventAt(
                    eventPath(
                        sequence = headRecord.sequence,
                        eventHash = headRecord.eventHash,
                    ),
                ),
            ) {
                "canonical immediate prior response event is missing"
            }
        val requestHash =
            requireNotNull(
                response.priorEventHash,
            ) {
                "canonical current response has no request predecessor"
            }
        val request =
            requireNotNull(
                readEventAt(
                    eventPath(
                        sequence =
                            headRecord.sequence -
                                1,
                        eventHash = requestHash,
                    ),
                ),
            ) {
                "canonical immediate prior request event is missing"
            }

        validateHeadAgainstPair(
            head = headRecord,
            request = request,
            response = response,
        )
        validateContextBinding(
            relativePath = headRecord.contextPath,
            expectedContextSha256 = headRecord.contextSha256,
            request = request,
            response = response,
        )

        return ImmediatePriorCompletedTurn(
            head =
                headProjection(
                    value = headRecord,
                    requestEventHash = request.eventHash,
                ),
            requestEvent = request,
            responseEvent = response,
            exactEventFileReadCount = 2,
            wholeHistoryEventEnumerationPerformed = false,
        )
    }

    private fun validateRequestAsPending(
        request: Event,
    ) {
        validateEvent(
            request,
        )
        require(
            request.eventKind ==
                VexLifeConversationContract.RequestKind,
        ) {
            "canonical response requires one request event"
        }

        val beforeHead =
            readHeadRecord()
        val expectedSequence =
            beforeHead
                ?.sequence
                ?.plus(
                    1,
                )
                ?: 0L
        val expectedPrior =
            beforeHead
                ?.eventHash

        require(
            request.sequence ==
                expectedSequence &&
                request.priorEventHash ==
                expectedPrior,
        ) {
            "canonical request no longer extends the exact current completed head"
        }

        val durable =
            readEventAt(
                eventPath(
                    request,
                ),
            )
        require(
            durable ==
                request,
        ) {
            "canonical pending request is not the exact durable event"
        }

        val all =
            readEvents()
        val completedHashes =
            readCompletedEventHashes(
                beforeHead,
                all,
            )
        val canonical =
            all.filter {
                it.eventHash in
                    completedHashes
            }
        val abandoned =
            classifyAbandonedAttempts(
                events = all,
                canonicalChain = canonical,
            )
        require(
            abandoned.singleOrNull {
                it.requestEvent.eventHash ==
                    request.eventHash
            } !=
                null,
        ) {
            "canonical pending request is not one exact off-head attempt"
        }
    }

    private fun readCompletedEventHashes(
        head: VexLifeConversationContract.Head?,
        events: List<Event>,
    ): Set<String> {
        if (
            head ==
                null
        ) {
            return emptySet()
        }
        val byHash =
            events.associateBy(
                Event::eventHash,
            )
        val result =
            mutableSetOf<String>()
        var cursor: String? =
            head.eventHash

        while (
            cursor !=
                null
        ) {
            require(
                result.add(
                    cursor,
                ),
            ) {
                "canonical Conversation event chain contains a cycle"
            }
            val event =
                requireNotNull(
                    byHash[
                        cursor
                    ],
                ) {
                    "canonical completed head references a missing event"
                }
            cursor =
                event.priorEventHash
        }

        return result
    }

    private fun writeEvent(
        event: Event,
    ) {
        validateEvent(
            event,
        )
        val path =
            eventPath(
                event,
            )

        val existing =
            readEventAt(
                path,
            )

        if (
            existing !=
                null
        ) {
            require(
                existing ==
                    event,
            ) {
                "canonical event address already contains different bytes"
            }
            return
        }

        when (
            event.eventKind
        ) {
            VexLifeConversationContract.RequestKind ->
                VexDurableContractFile.write(
                    ownedRoot = productHome,
                    path = path,
                    value =
                        requestRecord(
                            event,
                        ),
                )

            VexLifeConversationContract.ResponseKind ->
                VexDurableContractFile.write(
                    ownedRoot = productHome,
                    path = path,
                    value =
                        responseRecord(
                            event,
                        ),
                )

            else ->
                error(
                    "unsupported canonical event kind",
                )
        }
    }

    private fun writeImmutableHead(
        head: VexLifeConversationContract.Head,
    ) {
        validateCanonicalHeadRecord(
            head,
        )
        val path =
            immutableHeadPath(
                head.conversationHeadSha256,
            )
        val existing =
            VexDurableContractFile.read<VexLifeConversationContract.Head>(
                ownedRoot = productHome,
                path = path,
            )

        if (
            existing !=
                null
        ) {
            validateCanonicalHeadRecord(
                existing,
            )
            require(
                existing ==
                    head,
            ) {
                "canonical immutable head address already contains different bytes"
            }
            return
        }

        VexDurableContractFile.write(
            ownedRoot = productHome,
            path = path,
            value = head,
        )
    }

    private fun readHeadRecord(): VexLifeConversationContract.Head? =
        VexDurableContractFile
            .read<VexLifeConversationContract.Head>(
                ownedRoot = productHome,
                path = currentHeadPath,
            )
            ?.also(
                ::validateCanonicalHeadRecord,
            )

    private fun readEvents(): List<Event> {
        if (
            VexOwnedPath.observeDirectory(
                productHome,
                eventsDirectory,
            ) ==
            VexOwnedPath.DirectoryState.Missing
        ) {
            return emptyList()
        }

        return Files
            .list(
                eventsDirectory,
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
                require(
                    Files.isRegularFile(
                        path,
                        LinkOption.NOFOLLOW_LINKS,
                    ) &&
                        !Files.isSymbolicLink(
                            path,
                        ),
                ) {
                    "canonical Conversation event directory contains a non-regular entry"
                }

                requireNotNull(
                    readEventAt(
                        path,
                    ),
                )
                    .also {
                        event ->
                        require(
                            path.fileName
                                .toString() ==
                                eventFilename(
                                    event,
                                ),
                        ) {
                            "canonical Conversation event is stored under the wrong address"
                        }
                    }
            }
    }

    private fun readEventAt(
        path: Path,
    ): Event? {
        val text =
            VexDurableContractFile.readText(
                ownedRoot = productHome,
                path = path,
            )
                ?: return null
        val objectValue =
            VexContractJson.Strict
                .parseToJsonElement(
                    text,
                )
                .jsonObject
        val schema =
            objectValue[
                "schemaVersion"
            ]
                ?.jsonPrimitive
                ?.content
        val kind =
            objectValue[
                "eventKind"
            ]
                ?.jsonPrimitive
                ?.content

        require(
            schema !=
                VexLifeConversationContract.EventSchemaV2,
        ) {
            "canonical semantic-relay v2 Conversation event is present; exact semantic-relay validation must be ported before this event is admitted"
        }
        require(
            schema ==
                VexLifeConversationContract.EventSchemaV1,
        ) {
            "canonical Conversation event schema is not admitted"
        }

        val event =
            when (
                kind
            ) {
                VexLifeConversationContract.RequestKind ->
                    requestProjection(
                        VexContractJson.decode(
                            text,
                        ),
                    )

                VexLifeConversationContract.ResponseKind ->
                    responseProjection(
                        VexContractJson.decode(
                            text,
                        ),
                    )

                else ->
                    error(
                        "canonical Conversation event kind is not admitted",
                    )
            }

        validateEvent(
            event,
        )
        return event
    }

    private fun validateEvent(
        event: Event,
    ) {
        require(
            event.homeRef ==
                manifest.homeRef &&
                event.deviceRef ==
                manifest.currentDeviceRef &&
                event.companionLineageRef ==
                manifest.currentCompanionLineageRef &&
                event.threadRef ==
                DirectConversationIdentity.ThreadRef &&
                event.privacyClass ==
                VexLifeConversationContract.PrivacyClass &&
                event.content.isNotEmpty() &&
                event.contentHash ==
                contentHash(
                    event.content,
                ),
        ) {
            "canonical Conversation event identity/content is invalid"
        }
        requireSafeRef(
            event.instanceRef,
            "event.instanceRef",
        )
        requireSafeRef(
            event.turnRef,
            "event.turnRef",
        )
        requireSha256(
            event.eventHash,
            "event.eventHash",
        )

        when (
            event.eventKind
        ) {
            VexLifeConversationContract.RequestKind -> {
                val core =
                    requestCore(
                        event,
                    )
                        .let(
                            VexLifeConversationContract::validateRequestCore,
                        )
                require(
                    semanticHash(
                        core,
                    ) ==
                        event.eventHash &&
                        event.speakerRef ==
                        installation.personRef &&
                        event.recipientRefs ==
                        listOf(
                            DirectConversationIdentity.CompanionRoleRef,
                        ) &&
                        event.endpointProfileRef ==
                        null &&
                        event.sanitizedEndpointOrigin ==
                        null &&
                        event.modelNameOrBoundedTestProfileRef ==
                        null,
                ) {
                    "canonical REQUEST hash/participant semantics are invalid"
                }
            }

            VexLifeConversationContract.ResponseKind -> {
                val core =
                    responseCore(
                        event,
                    )
                        .let(
                            VexLifeConversationContract::validateResponseCore,
                        )
                require(
                    semanticHash(
                        core,
                    ) ==
                        event.eventHash &&
                        event.speakerRef ==
                        DirectConversationIdentity.CompanionRoleRef &&
                        event.recipientRefs ==
                        listOf(
                            installation.personRef,
                        ),
                ) {
                    "canonical RESPONSE hash/participant semantics are invalid"
                }
            }

            else ->
                error(
                    "canonical Conversation event kind is not admitted",
                )
        }
    }

    private fun validateCanonicalHeadRecord(
        head: VexLifeConversationContract.Head,
    ) {
        VexLifeConversationContract.validateHead(
            head,
        )
        require(
            head.homeRef ==
                manifest.homeRef &&
                head.deviceRef ==
                manifest.currentDeviceRef &&
                head.companionLineageRef ==
                manifest.currentCompanionLineageRef &&
                head.threadRef ==
                DirectConversationIdentity.ThreadRef &&
                semanticHash(
                    VexLifeConversationContract.HeadCore(
                        schemaVersion = head.schemaVersion,
                        homeRef = head.homeRef,
                        deviceRef = head.deviceRef,
                        companionLineageRef = head.companionLineageRef,
                        instanceRef = head.instanceRef,
                        threadRef = head.threadRef,
                        turnRef = head.turnRef,
                        requestMessageRef = head.requestMessageRef,
                        responseMessageRef = head.responseMessageRef,
                        eventHash = head.eventHash,
                        contextSha256 = head.contextSha256,
                        contextPath = head.contextPath,
                        sequence = head.sequence,
                        priorConversationHeadSha256 = head.priorConversationHeadSha256,
                        formedAt = head.formedAt,
                    ),
                ) ==
                head.conversationHeadSha256,
        ) {
            "canonical Conversation head hash/address/identity is invalid"
        }
    }

    private fun validateHeadAgainstPair(
        head: VexLifeConversationContract.Head,
        request: Event,
        response: Event,
    ) {
        validateCanonicalHeadRecord(
            head,
        )
        require(
            response.eventKind ==
                VexLifeConversationContract.ResponseKind &&
                request.eventKind ==
                VexLifeConversationContract.RequestKind &&
                response.eventHash ==
                head.eventHash &&
                response.sequence ==
                head.sequence &&
                response.turnRef ==
                head.turnRef &&
                request.turnRef ==
                head.turnRef &&
                response.instanceRef ==
                head.instanceRef &&
                request.instanceRef ==
                head.instanceRef &&
                response.messageRef ==
                head.responseMessageRef &&
                request.messageRef ==
                head.requestMessageRef &&
                response.priorEventHash ==
                request.eventHash,
        ) {
            "canonical Conversation head does not bind its exact final request/response pair"
        }
    }

    private fun validateContextBinding(
        relativePath: String,
        expectedContextSha256: String,
        request: Event,
        response: Event,
    ) {
        require(
            relativePath ==
                contextRelativePath(
                    request.turnRef,
                ),
        ) {
            "canonical Conversation head does not reference the exact current context coordinate"
        }
        val contextPath =
            VexOwnedPath.resolveInside(
                productHome,
                relativePath,
            )
        val contextText =
            requireNotNull(
                VexDurableContractFile.readText(
                    ownedRoot = productHome,
                    path = contextPath,
                ),
            ) {
                "canonical bounded context record is missing"
            }
        val contextJson =
            VexContractJson.Strict
                .parseToJsonElement(
                    contextText,
                )
                .jsonObject
        val observedContextHash =
            requireNotNull(
                contextJson[
                    "serializedContextSha256"
                ]
                    ?.jsonPrimitive
                    ?.content,
            ) {
                "canonical bounded context has no serializedContextSha256"
            }
        val contextCoreJson =
            JsonObject(
                contextJson.filterKeys {
                    it !=
                        "serializedContextSha256"
                },
            )
        require(
            sha256(
                VexContractJson.canonicalEncodeElement(
                    contextCoreJson,
                ),
            ) ==
                observedContextHash,
        ) {
            "canonical bounded context hash does not match its exact durable JSON preimage"
        }
        val context =
            VexContractJson.decode<VexLifeConversationContract.Context>(
                contextText,
            )
                .also(
                    VexLifeConversationContract::validateContext,
                )
        require(
            context.serializedContextSha256 ==
                observedContextHash &&
                context.serializedContextSha256 ==
                expectedContextSha256 &&
                context.homeRef ==
                manifest.homeRef &&
                context.deviceRef ==
                manifest.currentDeviceRef &&
                context.companionLineageRef ==
                manifest.currentCompanionLineageRef &&
                context.instanceRef ==
                request.instanceRef &&
                context.threadRef ==
                request.threadRef &&
                context.turnRef ==
                request.turnRef &&
                context.requestEventHash ==
                request.eventHash &&
                context.responseEventHash ==
                response.eventHash &&
                context.contextSourceRefs.size >=
                2 &&
                context.contextSourceRefs[
                    context.contextSourceRefs.lastIndex -
                        1
                ] ==
                request.eventRef &&
                context.contextSourceRefs.last() ==
                response.eventRef,
        ) {
            "canonical bounded context does not bind the exact completed turn"
        }
    }

    private fun requestProjection(
        value: VexLifeConversationContract.RequestV1,
    ): Event =
        Event(
            schemaVersion = value.schemaVersion,
            eventKind = value.eventKind,
            eventRef = value.eventRef,
            eventHash = value.eventHash,
            homeRef = value.homeRef,
            deviceRef = value.deviceRef,
            companionLineageRef = value.companionLineageRef,
            instanceRef = value.instanceRef,
            threadRef = value.threadRef,
            channelRef = value.channelRef,
            turnRef = value.turnRef,
            messageRef = value.messageRef,
            speakerRef = value.speakerRef,
            recipientRefs = value.recipientRefs,
            sequence = value.sequence,
            priorEventHash = value.priorEventHash,
            content = value.content,
            contentHash = value.contentHash,
            endpointProfileRef = null,
            sanitizedEndpointOrigin = null,
            modelNameOrBoundedTestProfileRef = null,
            privacyClass = value.privacyClass,
            formedAt = value.formedAt,
        )

    private fun responseProjection(
        value: VexLifeConversationContract.ResponseV1,
    ): Event =
        Event(
            schemaVersion = value.schemaVersion,
            eventKind = value.eventKind,
            eventRef = value.eventRef,
            eventHash = value.eventHash,
            homeRef = value.homeRef,
            deviceRef = value.deviceRef,
            companionLineageRef = value.companionLineageRef,
            instanceRef = value.instanceRef,
            threadRef = value.threadRef,
            channelRef = value.channelRef,
            turnRef = value.turnRef,
            messageRef = value.messageRef,
            speakerRef = value.speakerRef,
            recipientRefs = value.recipientRefs,
            sequence = value.sequence,
            priorEventHash = value.priorEventHash,
            content = value.content,
            contentHash = value.contentHash,
            endpointProfileRef = value.endpointProfileRef,
            sanitizedEndpointOrigin = value.sanitizedEndpointOrigin,
            modelNameOrBoundedTestProfileRef = value.modelNameOrBoundedTestProfileRef,
            privacyClass = value.privacyClass,
            formedAt = value.formedAt,
        )

    private fun requestCore(
        event: Event,
    ): VexLifeConversationContract.RequestCoreV1 =
        VexLifeConversationContract.RequestCoreV1(
            schemaVersion = event.schemaVersion,
            eventRef = event.eventRef,
            eventKind = event.eventKind,
            homeRef = event.homeRef,
            deviceRef = event.deviceRef,
            companionLineageRef = event.companionLineageRef,
            instanceRef = event.instanceRef,
            threadRef = event.threadRef,
            channelRef = event.channelRef,
            turnRef = event.turnRef,
            messageRef = event.messageRef,
            speakerRef = event.speakerRef,
            recipientRefs = event.recipientRefs,
            sequence = event.sequence,
            priorEventHash = event.priorEventHash,
            content = event.content,
            contentHash = event.contentHash,
            privacyClass = event.privacyClass,
            formedAt = event.formedAt,
        )

    private fun responseCore(
        event: Event,
    ): VexLifeConversationContract.ResponseCoreV1 =
        VexLifeConversationContract.ResponseCoreV1(
            schemaVersion = event.schemaVersion,
            eventRef = event.eventRef,
            eventKind = event.eventKind,
            homeRef = event.homeRef,
            deviceRef = event.deviceRef,
            companionLineageRef = event.companionLineageRef,
            instanceRef = event.instanceRef,
            threadRef = event.threadRef,
            channelRef = event.channelRef,
            turnRef = event.turnRef,
            messageRef = event.messageRef,
            speakerRef = event.speakerRef,
            recipientRefs = event.recipientRefs,
            sequence = event.sequence,
            priorEventHash =
                requireNotNull(
                    event.priorEventHash,
                ),
            content = event.content,
            contentHash = event.contentHash,
            endpointProfileRef =
                requireNotNull(
                    event.endpointProfileRef,
                ),
            sanitizedEndpointOrigin =
                requireNotNull(
                    event.sanitizedEndpointOrigin,
                ),
            modelNameOrBoundedTestProfileRef =
                requireNotNull(
                    event.modelNameOrBoundedTestProfileRef,
                ),
            privacyClass = event.privacyClass,
            formedAt = event.formedAt,
        )

    private fun requestRecord(
        event: Event,
    ): VexLifeConversationContract.RequestV1 {
        val core =
            requestCore(
                event,
            )
        return VexLifeConversationContract.RequestV1(
            schemaVersion = core.schemaVersion,
            eventRef = core.eventRef,
            eventKind = core.eventKind,
            homeRef = core.homeRef,
            deviceRef = core.deviceRef,
            companionLineageRef = core.companionLineageRef,
            instanceRef = core.instanceRef,
            threadRef = core.threadRef,
            channelRef = core.channelRef,
            turnRef = core.turnRef,
            messageRef = core.messageRef,
            speakerRef = core.speakerRef,
            recipientRefs = core.recipientRefs,
            sequence = core.sequence,
            priorEventHash = core.priorEventHash,
            content = core.content,
            contentHash = core.contentHash,
            privacyClass = core.privacyClass,
            formedAt = core.formedAt,
            eventHash = event.eventHash,
        )
    }

    private fun responseRecord(
        event: Event,
    ): VexLifeConversationContract.ResponseV1 {
        val core =
            responseCore(
                event,
            )
        return VexLifeConversationContract.ResponseV1(
            schemaVersion = core.schemaVersion,
            eventRef = core.eventRef,
            eventKind = core.eventKind,
            homeRef = core.homeRef,
            deviceRef = core.deviceRef,
            companionLineageRef = core.companionLineageRef,
            instanceRef = core.instanceRef,
            threadRef = core.threadRef,
            channelRef = core.channelRef,
            turnRef = core.turnRef,
            messageRef = core.messageRef,
            speakerRef = core.speakerRef,
            recipientRefs = core.recipientRefs,
            sequence = core.sequence,
            priorEventHash = core.priorEventHash,
            content = core.content,
            contentHash = core.contentHash,
            endpointProfileRef = core.endpointProfileRef,
            sanitizedEndpointOrigin = core.sanitizedEndpointOrigin,
            modelNameOrBoundedTestProfileRef = core.modelNameOrBoundedTestProfileRef,
            privacyClass = core.privacyClass,
            formedAt = core.formedAt,
            eventHash = event.eventHash,
        )
    }

    private fun headProjection(
        value: VexLifeConversationContract.Head,
        requestEventHash: String,
    ): Head =
        Head(
            schemaVersion = value.schemaVersion,
            conversationHeadSha256 = value.conversationHeadSha256,
            homeRef = value.homeRef,
            deviceRef = value.deviceRef,
            companionLineageRef = value.companionLineageRef,
            instanceRef = value.instanceRef,
            threadRef = value.threadRef,
            turnRef = value.turnRef,
            requestMessageRef = value.requestMessageRef,
            responseMessageRef = value.responseMessageRef,
            requestEventHash = requestEventHash,
            eventHash = value.eventHash,
            contextSha256 = value.contextSha256,
            contextPath = value.contextPath,
            sequence = value.sequence,
            priorConversationHeadSha256 = value.priorConversationHeadSha256,
            formedAt = value.formedAt,
        )

    private fun eventPath(
        event: Event,
    ): Path =
        eventPath(
            sequence = event.sequence,
            eventHash = event.eventHash,
        )

    private fun eventPath(
        sequence: Long,
        eventHash: String,
    ): Path =
        VexOwnedPath.resolveInside(
            eventsDirectory,
            eventFilename(
                sequence = sequence,
                eventHash = eventHash,
            ),
        )

    private fun eventFilename(
        event: Event,
    ): String =
        eventFilename(
            sequence = event.sequence,
            eventHash = event.eventHash,
        )

    private fun eventFilename(
        sequence: Long,
        eventHash: String,
    ): String =
        sequence
            .toString()
            .padStart(
                8,
                '0',
            ) +
            "-" +
            eventHash +
            ".json"

    private fun immutableHeadPath(
        hash: String,
    ): Path =
        VexOwnedPath.resolveInside(
            headsDirectory,
            "$hash.json",
        )

    private fun contextRelativePath(
        turnRef: String,
    ): String =
        "context/" +
            manifest.currentCompanionLineageRef +
            "/" +
            DirectConversationIdentity.ThreadRef +
            "/" +
            turnRef +
            ".json"

    private fun acquireThreadWriterLease(
        instanceRef: String,
    ): HeldThreadWriterLease {
        requireSafeRef(
            instanceRef,
            "instanceRef",
        )
        VexOwnedPath.ensureDirectory(
            productHome,
            lockDirectory,
        )

        val core =
            ThreadWriterLeaseCore(
                schemaVersion = ThreadWriterLeaseSchema,
                companionLineageRef = manifest.currentCompanionLineageRef,
                threadRef = DirectConversationIdentity.ThreadRef,
                instanceRef = instanceRef,
                lockToken =
                    UUID.randomUUID()
                        .toString()
                        .lowercase(),
                pid = ProcessHandle.current().pid(),
                formedAt = Instant.now().toString(),
            )
        val lease =
            ThreadWriterLease(
                schemaVersion = core.schemaVersion,
                companionLineageRef = core.companionLineageRef,
                threadRef = core.threadRef,
                instanceRef = core.instanceRef,
                lockToken = core.lockToken,
                pid = core.pid,
                formedAt = core.formedAt,
                leaseSha256 =
                    semanticHash(
                        core,
                    ),
            )

        try {
            Files.newOutputStream(
                lockPath,
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE,
            )
                .bufferedWriter(
                    StandardCharsets.UTF_8,
                )
                .use {
                    writer ->
                    writer.write(
                        VexContractJson.encode(
                            lease,
                        ),
                    )
                    writer.newLine()
                }
            VexDurableContractFile.secureFile(
                lockPath,
            )
        } catch (
            _: FileAlreadyExistsException,
        ) {
            val observed =
                requireNotNull(
                    VexDurableContractFile.read<ThreadWriterLease>(
                        ownedRoot = productHome,
                        path = lockPath,
                    ),
                ) {
                    "thread writer lease exists but is unreadable"
                }
            require(
                validateThreadLease(
                    observed,
                ),
            ) {
                "thread writer lease is invalid"
            }
            when (
                processState(
                    observed.pid,
                )
            ) {
                "ABSENT" ->
                    error(
                        "THREAD_WRITER_RECOVERY_REQUIRED: prior thread writer process is absent; preserve the lease for explicit recovery",
                    )

                "ACTIVE" ->
                    error(
                        "THREAD_WRITER_CONFLICT: another active thread writer owns this Conversation",
                    )

                else ->
                    error(
                        "THREAD_WRITER_CONFLICT: prior thread writer ownership is unverifiable",
                    )
            }
        }

        return HeldThreadWriterLease(
            lease,
        )
    }

    private fun validateThreadLease(
        lease: ThreadWriterLease,
    ): Boolean {
        if (
            lease.schemaVersion !=
                ThreadWriterLeaseSchema ||
            lease.companionLineageRef !=
                manifest.currentCompanionLineageRef ||
            lease.threadRef !=
                DirectConversationIdentity.ThreadRef ||
            lease.pid <=
                0
        ) {
            return false
        }
        return try {
            requireSafeRef(
                lease.instanceRef,
                "thread lease instanceRef",
            )
            lease.leaseSha256 ==
                semanticHash(
                    ThreadWriterLeaseCore(
                        schemaVersion = lease.schemaVersion,
                        companionLineageRef = lease.companionLineageRef,
                        threadRef = lease.threadRef,
                        instanceRef = lease.instanceRef,
                        lockToken = lease.lockToken,
                        pid = lease.pid,
                        formedAt = lease.formedAt,
                    ),
                )
        } catch (
            _: Exception,
        ) {
            false
        }
    }

    private fun processState(
        pid: Long,
    ): String =
        try {
            ProcessHandle
                .of(
                    pid,
                )
                .map {
                    process ->
                    if (
                        process.isAlive
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

    private fun currentProcessInstanceRef(): String =
        InstancePrefix +
            "." +
            ProcessHandle.current()
                .pid()

    private fun contentHash(
        value: String,
    ): String =
        sha256(
            VexContractJson.canonicalEncode(
                value,
            ),
        )

    private inline fun <reified T> semanticHash(
        value: T,
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

    private fun requireSafeRef(
        value: String,
        label: String,
    ) {
        require(
            value.matches(
                Regex(
                    "[a-z0-9](?:[a-z0-9._-]{0,126}[a-z0-9])?",
                ),
            ),
        ) {
            "$label must be one lowercase portable canonical ref"
        }
    }

    private fun requireNonEmpty(
        value: String,
        label: String,
    ) {
        require(
            value.isNotEmpty(),
        ) {
            "$label must be non-empty"
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

    private fun requireLoopbackOrigin(
        value: String,
    ) {
        val uri =
            URI(
                value,
            )

        require(
            uri.scheme ==
                "http" &&
                uri.userInfo ==
                null &&
                uri.query ==
                null &&
                uri.fragment ==
                null &&
                (
                    uri.host ==
                        "127.0.0.1" ||
                        uri.host ==
                        "::1"
                ) &&
                uri.port >
                0 &&
                (
                    uri.path.isNullOrEmpty() ||
                        uri.path ==
                        "/"
                ),
        ) {
            "canonical Conversation response origin must be numeric-loopback HTTP"
        }
    }
}

// [VXG RealForever]
