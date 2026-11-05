package vexlife.conversation

import java.net.URI
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.security.MessageDigest
import java.time.Instant
import kotlinx.serialization.Serializable
import vexlife.contract.VexContractJson
import vexlife.contract.VexDurableContractFile
import vexlife.filesystem.VexExclusiveFileLease
import vexlife.filesystem.VexOwnedPath
import vexlife.home.VexLifeProdHome
import vexlife.home.VexLifeProdHomeFiles

/**
 * Durable direct personal Conversation store for the isolated VexLife-Prod Home.
 *
 * The store owns append-only REQUEST/RESPONSE event bytes, completed-head currentness and one
 * whole-turn writer lease. It does not call a model, select prompt context, own Memory or render UI.
 *
 * Current head advances only after one complete REQUEST/RESPONSE pair is durable.
 */
class DirectConversationStore(
    productHome: Path,
) {
    companion object {
        const val EventSchema =
            "vexlife-prod.direct-conversation-event/v1"

        const val HeadSchema =
            "vexlife-prod.direct-conversation-head/v1"
    }

    @Serializable
    private data class EventCore(
        val schemaVersion: String,
        val eventKind: String,
        val homeRef: String,
        val deviceRef: String,
        val companionLineageRef: String,
        val projectRef: String,
        val threadRef: String,
        val channelRef: String,
        val turnRef: String,
        val messageRef: String,
        val speakerRef: String,
        val recipientRefs: List<String>,
        val sequence: Long,
        val priorEventSha256: String?,
        val content: String,
        val contentSha256: String,
        val endpointOrigin: String?,
        val model: String?,
        val runtimeObservationRef: String?,
        val invocationEvidenceRef: String?,
        val privacyClass: String,
        val formedAt: String,
    )

    @Serializable
    data class Event(
        val schemaVersion: String,
        val eventKind: String,
        val eventRef: String,
        val eventSha256: String,
        val homeRef: String,
        val deviceRef: String,
        val companionLineageRef: String,
        val projectRef: String,
        val threadRef: String,
        val channelRef: String,
        val turnRef: String,
        val messageRef: String,
        val speakerRef: String,
        val recipientRefs: List<String>,
        val sequence: Long,
        val priorEventSha256: String?,
        val content: String,
        val contentSha256: String,
        val endpointOrigin: String?,
        val model: String?,
        val runtimeObservationRef: String?,
        val invocationEvidenceRef: String?,
        val privacyClass: String,
        val formedAt: String,
    )

    @Serializable
    private data class HeadCore(
        val schemaVersion: String,
        val homeRef: String,
        val deviceRef: String,
        val companionLineageRef: String,
        val projectRef: String,
        val threadRef: String,
        val channelRef: String,
        val turnRef: String,
        val requestMessageRef: String,
        val responseMessageRef: String,
        val requestEventSha256: String,
        val responseEventSha256: String,
        val sequence: Long,
        val priorConversationHeadSha256: String?,
        val formedAt: String,
    )

    @Serializable
    data class Head(
        val schemaVersion: String,
        val headRef: String,
        val conversationHeadSha256: String,
        val homeRef: String,
        val deviceRef: String,
        val companionLineageRef: String,
        val projectRef: String,
        val threadRef: String,
        val channelRef: String,
        val turnRef: String,
        val requestMessageRef: String,
        val responseMessageRef: String,
        val requestEventSha256: String,
        val responseEventSha256: String,
        val sequence: Long,
        val priorConversationHeadSha256: String?,
        val formedAt: String,
    )

    data class Snapshot(
        val head: Head?,
        val events: List<Event>,
    )

    class IncompleteConversationException(
        message: String,
    ) : IllegalStateException(
        message,
    )

    inner class Locked internal constructor() {
        fun current(): Snapshot =
            readCurrentComplete()

        fun beginRequest(
            turnRef: String,
            requestMessageRef: String,
            content: String,
            formedAt: Instant = Instant.now(),
        ): Event {
            requireRef(
                turnRef,
                "turnRef",
            )
            requireRef(
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
            val sequence =
                before.head
                    ?.sequence
                    ?.plus(
                        1,
                    )
                    ?: 0L
            val prior =
                before.head
                    ?.responseEventSha256
            val core =
                EventCore(
                    schemaVersion = EventSchema,
                    eventKind = "REQUEST",
                    homeRef = manifest.homeRef,
                    deviceRef = manifest.currentDeviceRef,
                    companionLineageRef = manifest.currentCompanionLineageRef,
                    projectRef = DirectConversationIdentity.ProjectRef,
                    threadRef = DirectConversationIdentity.ThreadRef,
                    channelRef = DirectConversationIdentity.ChannelRef,
                    turnRef = turnRef,
                    messageRef = requestMessageRef,
                    speakerRef = VexLifeProdHome.DefaultPersonRef,
                    recipientRefs =
                        listOf(
                            manifest.currentCompanionLineageRef,
                        ),
                    sequence = sequence,
                    priorEventSha256 = prior,
                    content = content,
                    contentSha256 =
                        sha256(
                            content,
                        ),
                    endpointOrigin = null,
                    model = null,
                    runtimeObservationRef = null,
                    invocationEvidenceRef = null,
                    privacyClass = "DEVICE_PRIVATE",
                    formedAt = formedAt.toString(),
                )
            val event =
                eventFromCore(
                    core,
                )

            writeEvent(
                event,
            )

            return event
        }

        fun completeResponse(
            request: Event,
            responseMessageRef: String,
            content: String,
            model: String,
            endpointOrigin: String,
            runtimeObservationRef: String,
            invocationEvidenceRef: String,
            formedAt: Instant = Instant.now(),
        ): Head {
            validateRequestAsPending(
                request,
            )
            requireRef(
                responseMessageRef,
                "responseMessageRef",
            )
            requireRef(
                runtimeObservationRef,
                "runtimeObservationRef",
            )
            requireRef(
                invocationEvidenceRef,
                "invocationEvidenceRef",
            )
            require(
                content.isNotEmpty(),
            ) {
                "direct Conversation response content must be non-empty"
            }
            require(
                model.isNotEmpty() &&
                    !model.contains(
                        "/",
                    ) &&
                    !model.contains(
                        "\\",
                    ),
            ) {
                "direct Conversation response model must be one safe compatibility identity"
            }
            requireLoopbackOrigin(
                endpointOrigin,
            )

            val core =
                EventCore(
                    schemaVersion = EventSchema,
                    eventKind = "RESPONSE",
                    homeRef = manifest.homeRef,
                    deviceRef = manifest.currentDeviceRef,
                    companionLineageRef = manifest.currentCompanionLineageRef,
                    projectRef = DirectConversationIdentity.ProjectRef,
                    threadRef = DirectConversationIdentity.ThreadRef,
                    channelRef = DirectConversationIdentity.ChannelRef,
                    turnRef = request.turnRef,
                    messageRef = responseMessageRef,
                    speakerRef = manifest.currentCompanionLineageRef,
                    recipientRefs =
                        listOf(
                            VexLifeProdHome.DefaultPersonRef,
                        ),
                    sequence =
                        request.sequence +
                            1,
                    priorEventSha256 = request.eventSha256,
                    content = content,
                    contentSha256 =
                        sha256(
                            content,
                        ),
                    endpointOrigin = endpointOrigin,
                    model = model,
                    runtimeObservationRef = runtimeObservationRef,
                    invocationEvidenceRef = invocationEvidenceRef,
                    privacyClass = "DEVICE_PRIVATE",
                    formedAt = formedAt.toString(),
                )
            val response =
                eventFromCore(
                    core,
                )

            writeEvent(
                response,
            )

            val priorHead =
                readHead()
            val headCore =
                HeadCore(
                    schemaVersion = HeadSchema,
                    homeRef = manifest.homeRef,
                    deviceRef = manifest.currentDeviceRef,
                    companionLineageRef = manifest.currentCompanionLineageRef,
                    projectRef = DirectConversationIdentity.ProjectRef,
                    threadRef = DirectConversationIdentity.ThreadRef,
                    channelRef = DirectConversationIdentity.ChannelRef,
                    turnRef = request.turnRef,
                    requestMessageRef = request.messageRef,
                    responseMessageRef = response.messageRef,
                    requestEventSha256 = request.eventSha256,
                    responseEventSha256 = response.eventSha256,
                    sequence = response.sequence,
                    priorConversationHeadSha256 =
                        priorHead
                            ?.conversationHeadSha256,
                    formedAt = formedAt.toString(),
                )
            val head =
                headFromCore(
                    headCore,
                )

            writeImmutableHead(
                head,
            )
            VexDurableContractFile.write(
                ownedRoot = productHome,
                path = currentHeadPath,
                value = head,
            )

            // Re-read the exact committed state before returning success.
            val current =
                readCurrentComplete()
            require(
                current.head
                    ?.conversationHeadSha256 ==
                    head.conversationHeadSha256,
            ) {
                "direct Conversation current head did not commit exactly"
            }

            return head
        }
    }

    private val productHome: Path =
        VexOwnedPath.requireRoot(
            productHome,
            "VexLife-Prod Home",
        )

    private val manifest: VexLifeProdHome.Manifest =
        requireNotNull(
            VexLifeProdHomeFiles.observeExisting(
                this.productHome,
            ),
        ) {
            "direct Conversation requires one current VexLife-Prod Home"
        }
            .manifest

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
            "runtime/conversation-writer-locks/" +
                manifest.currentCompanionLineageRef,
        )

    private val lockPath: Path =
        VexOwnedPath.resolveInside(
            this.productHome,
            "runtime/conversation-writer-locks/" +
                manifest.currentCompanionLineageRef +
                "/" +
                DirectConversationIdentity.ThreadRef +
                ".lock",
        )

    fun <T> withWriterLease(
        block: Locked.() -> T,
    ): T {
        prepareOwnedDirectories()

        return VexExclusiveFileLease
            .acquire(
                lockPath,
            )
            .use {
                Locked()
                    .block()
            }
    }

    fun current(): Snapshot =
        readCurrentComplete()

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
        val head =
            readHead()

        if (
            head ==
                null
        ) {
            if (
                events.isNotEmpty()
            ) {
                throw IncompleteConversationException(
                    "direct Conversation has durable events without one completed head; explicit recovery is required",
                )
            }
            return Snapshot(
                head = null,
                events = emptyList(),
            )
        }

        val bySha =
            events.associateBy(
                Event::eventSha256,
            )
        require(
            bySha.size ==
                events.size,
        ) {
            "direct Conversation contains duplicate event hashes"
        }

        val chain =
            mutableListOf<Event>()
        val visited =
            mutableSetOf<String>()
        var cursor: String? =
            head.responseEventSha256

        while (
            cursor !=
                null
        ) {
            require(
                visited.add(
                    cursor,
                ),
            ) {
                "direct Conversation event chain contains a cycle"
            }
            val event =
                requireNotNull(
                    bySha[
                        cursor
                    ],
                ) {
                    "direct Conversation head references a missing event"
                }
            chain +=
                event
            cursor =
                event.priorEventSha256
        }

        chain.reverse()

        require(
            chain.size ==
                events.size,
        ) {
            "direct Conversation contains durable events outside the completed head; explicit recovery is required"
        }
        require(
            chain.size >=
                2 &&
                chain.size %
                2 ==
                0,
        ) {
            "direct Conversation completed chain must contain request/response pairs"
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
                    index.toLong(),
            ) {
                "direct Conversation sequence is not contiguous from genesis"
            }
            val expectedPrior =
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
                        .eventSha256
                }
            require(
                event.priorEventSha256 ==
                    expectedPrior,
            ) {
                "direct Conversation prior-event identity is not exact"
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
                    "REQUEST" &&
                    response.eventKind ==
                    "RESPONSE" &&
                    request.turnRef ==
                    response.turnRef &&
                    response.speakerRef ==
                    request.recipientRefs.single() &&
                    response.recipientRefs ==
                    listOf(
                        request.speakerRef,
                    ),
            ) {
                "direct Conversation request/response pair semantics are invalid"
            }
        }

        val finalResponse =
            chain.last()
        val finalRequest =
            chain[
                chain.lastIndex -
                    1
            ]

        validateHead(
            head,
        )
        require(
            head.sequence ==
                finalResponse.sequence &&
                head.responseEventSha256 ==
                finalResponse.eventSha256 &&
                head.requestEventSha256 ==
                finalRequest.eventSha256 &&
                head.turnRef ==
                finalResponse.turnRef &&
                head.requestMessageRef ==
                finalRequest.messageRef &&
                head.responseMessageRef ==
                finalResponse.messageRef,
        ) {
            "direct Conversation head does not bind its exact final request/response pair"
        }

        val immutable =
            VexDurableContractFile.read<Head>(
                ownedRoot = productHome,
                path =
                    immutableHeadPath(
                        head.conversationHeadSha256,
                    ),
            )
        require(
            immutable ==
                head,
        ) {
            "direct Conversation current head differs from its immutable receipt"
        }

        validateImmutableHeadLineage(
            currentHead = head,
            chain = chain,
        )

        return Snapshot(
            head = head,
            events = chain,
        )
    }

    private fun validateImmutableHeadLineage(
        currentHead: Head,
        chain: List<Event>,
    ) {
        val pairCount =
            chain.size /
                2
        val reversed =
            mutableListOf<Head>()
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
                "direct Conversation immutable head lineage contains a cycle"
            }

            val historical =
                requireNotNull(
                    VexDurableContractFile.read<Head>(
                        ownedRoot = productHome,
                        path =
                            immutableHeadPath(
                                cursor,
                            ),
                    ),
                ) {
                    "direct Conversation immutable head lineage is missing a historical receipt"
                }
            validateHead(
                historical,
            )
            require(
                historical.conversationHeadSha256 ==
                    cursor,
            ) {
                "direct Conversation immutable head is stored under the wrong identity"
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
            "direct Conversation immutable head lineage count does not match completed turns"
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
                    historical.requestEventSha256 ==
                    request.eventSha256 &&
                    historical.responseEventSha256 ==
                    response.eventSha256 &&
                    historical.turnRef ==
                    request.turnRef &&
                    historical.priorConversationHeadSha256 ==
                    expectedPrior,
            ) {
                "direct Conversation immutable head lineage does not bind the exact completed pair"
            }
        }
    }

    private fun validateRequestAsPending(
        request: Event,
    ) {
        validateEvent(
            request,
        )
        require(
            request.eventKind ==
                "REQUEST",
        ) {
            "direct Conversation response requires one request event"
        }

        val beforeHead =
            readHead()
        val expectedSequence =
            beforeHead
                ?.sequence
                ?.plus(
                    1,
                )
                ?: 0L
        val expectedPrior =
            beforeHead
                ?.responseEventSha256

        require(
            request.sequence ==
                expectedSequence &&
                request.priorEventSha256 ==
                expectedPrior,
        ) {
            "direct Conversation request no longer extends the exact current completed head"
        }

        val durable =
            VexDurableContractFile.read<Event>(
                ownedRoot = productHome,
                path =
                    eventPath(
                        request,
                    ),
            )
        require(
            durable ==
                request,
        ) {
            "direct Conversation pending request is not the exact durable event"
        }

        val all =
            readEvents()
        val completedHashes =
            readCompletedEventHashes(
                beforeHead,
                all,
            )
        val extras =
            all.filter {
                it.eventSha256 !in
                    completedHashes
            }
        require(
            extras ==
                listOf(
                    request,
                ),
        ) {
            "direct Conversation pending request is not the only uncommitted event"
        }
    }

    private fun readCompletedEventHashes(
        head: Head?,
        events: List<Event>,
    ): Set<String> {
        if (
            head ==
                null
        ) {
            return emptySet()
        }
        val bySha =
            events.associateBy(
                Event::eventSha256,
            )
        val result =
            mutableSetOf<String>()
        var cursor: String? =
            head.responseEventSha256

        while (
            cursor !=
                null
        ) {
            require(
                result.add(
                    cursor,
                ),
            ) {
                "direct Conversation event chain contains a cycle"
            }
            val event =
                requireNotNull(
                    bySha[
                        cursor
                    ],
                ) {
                    "direct Conversation completed head references a missing event"
                }
            cursor =
                event.priorEventSha256
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
            VexDurableContractFile.read<Event>(
                ownedRoot = productHome,
                path = path,
            )

        if (
            existing !=
                null
        ) {
            require(
                existing ==
                    event,
            ) {
                "direct Conversation event address already contains different bytes"
            }
            return
        }

        VexDurableContractFile.write(
            ownedRoot = productHome,
            path = path,
            value = event,
        )
    }

    private fun writeImmutableHead(
        head: Head,
    ) {
        validateHead(
            head,
        )
        val path =
            immutableHeadPath(
                head.conversationHeadSha256,
            )
        val existing =
            VexDurableContractFile.read<Head>(
                ownedRoot = productHome,
                path = path,
            )

        if (
            existing !=
                null
        ) {
            require(
                existing ==
                    head,
            ) {
                "direct Conversation immutable head address already contains different bytes"
            }
            return
        }

        VexDurableContractFile.write(
            ownedRoot = productHome,
            path = path,
            value = head,
        )
    }

    private fun readHead(): Head? =
        VexDurableContractFile
            .read<Head>(
                ownedRoot = productHome,
                path = currentHeadPath,
            )
            ?.also(
                ::validateHead,
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
                    "direct Conversation event directory contains a non-regular entry"
                }

                requireNotNull(
                    VexDurableContractFile.read<Event>(
                        ownedRoot = productHome,
                        path = path,
                    ),
                )
                    .also {
                        event ->
                        validateEvent(
                            event,
                        )
                        require(
                            path.fileName
                                .toString() ==
                                eventFilename(
                                    event,
                                ),
                        ) {
                            "direct Conversation event is stored under the wrong address"
                        }
                    }
            }
    }

    private fun eventFromCore(
        core: EventCore,
    ): Event {
        val eventSha256 =
            semanticHash(
                core,
            )

        return Event(
            schemaVersion = core.schemaVersion,
            eventKind = core.eventKind,
            eventRef =
                "event.vexlife-prod.direct." +
                    eventSha256.take(
                        32,
                    ),
            eventSha256 = eventSha256,
            homeRef = core.homeRef,
            deviceRef = core.deviceRef,
            companionLineageRef = core.companionLineageRef,
            projectRef = core.projectRef,
            threadRef = core.threadRef,
            channelRef = core.channelRef,
            turnRef = core.turnRef,
            messageRef = core.messageRef,
            speakerRef = core.speakerRef,
            recipientRefs = core.recipientRefs,
            sequence = core.sequence,
            priorEventSha256 = core.priorEventSha256,
            content = core.content,
            contentSha256 = core.contentSha256,
            endpointOrigin = core.endpointOrigin,
            model = core.model,
            runtimeObservationRef = core.runtimeObservationRef,
            invocationEvidenceRef = core.invocationEvidenceRef,
            privacyClass = core.privacyClass,
            formedAt = core.formedAt,
        )
            .also(
                ::validateEvent,
            )
    }

    private fun headFromCore(
        core: HeadCore,
    ): Head {
        val hash =
            semanticHash(
                core,
            )

        return Head(
            schemaVersion = core.schemaVersion,
            headRef =
                "head.vexlife-prod.direct." +
                    hash.take(
                        32,
                    ),
            conversationHeadSha256 = hash,
            homeRef = core.homeRef,
            deviceRef = core.deviceRef,
            companionLineageRef = core.companionLineageRef,
            projectRef = core.projectRef,
            threadRef = core.threadRef,
            channelRef = core.channelRef,
            turnRef = core.turnRef,
            requestMessageRef = core.requestMessageRef,
            responseMessageRef = core.responseMessageRef,
            requestEventSha256 = core.requestEventSha256,
            responseEventSha256 = core.responseEventSha256,
            sequence = core.sequence,
            priorConversationHeadSha256 = core.priorConversationHeadSha256,
            formedAt = core.formedAt,
        )
            .also(
                ::validateHead,
            )
    }

    private fun validateEvent(
        event: Event,
    ) {
        require(
            event.schemaVersion ==
                EventSchema &&
                event.homeRef ==
                manifest.homeRef &&
                event.deviceRef ==
                manifest.currentDeviceRef &&
                event.companionLineageRef ==
                manifest.currentCompanionLineageRef &&
                event.projectRef ==
                DirectConversationIdentity.ProjectRef &&
                event.threadRef ==
                DirectConversationIdentity.ThreadRef &&
                event.channelRef ==
                DirectConversationIdentity.ChannelRef &&
                event.sequence >=
                0 &&
                event.content.isNotEmpty() &&
                event.contentSha256 ==
                sha256(
                    event.content,
                ) &&
                event.privacyClass ==
                "DEVICE_PRIVATE",
        ) {
            "direct Conversation event identity/content is invalid"
        }
        requireRef(
            event.turnRef,
            "event.turnRef",
        )
        requireRef(
            event.messageRef,
            "event.messageRef",
        )
        requireRef(
            event.speakerRef,
            "event.speakerRef",
        )
        require(
            event.recipientRefs.isNotEmpty(),
        ) {
            "direct Conversation event requires recipient refs"
        }
        event.recipientRefs.forEach {
            requireRef(
                it,
                "event.recipientRef",
            )
        }
        event.priorEventSha256?.let {
            requireSha256(
                it,
                "event.priorEventSha256",
            )
        }
        requireSha256(
            event.eventSha256,
            "event.eventSha256",
        )

        val expectedCore =
            EventCore(
                schemaVersion = event.schemaVersion,
                eventKind = event.eventKind,
                homeRef = event.homeRef,
                deviceRef = event.deviceRef,
                companionLineageRef = event.companionLineageRef,
                projectRef = event.projectRef,
                threadRef = event.threadRef,
                channelRef = event.channelRef,
                turnRef = event.turnRef,
                messageRef = event.messageRef,
                speakerRef = event.speakerRef,
                recipientRefs = event.recipientRefs,
                sequence = event.sequence,
                priorEventSha256 = event.priorEventSha256,
                content = event.content,
                contentSha256 = event.contentSha256,
                endpointOrigin = event.endpointOrigin,
                model = event.model,
                runtimeObservationRef = event.runtimeObservationRef,
                invocationEvidenceRef = event.invocationEvidenceRef,
                privacyClass = event.privacyClass,
                formedAt = event.formedAt,
            )
        require(
            semanticHash(
                expectedCore,
            ) ==
                event.eventSha256 &&
                event.eventRef ==
                "event.vexlife-prod.direct." +
                    event.eventSha256.take(
                        32,
                    ),
        ) {
            "direct Conversation event hash/address is invalid"
        }

        when (
            event.eventKind
        ) {
            "REQUEST" ->
                require(
                    event.speakerRef ==
                        VexLifeProdHome.DefaultPersonRef &&
                        event.recipientRefs ==
                        listOf(
                            manifest.currentCompanionLineageRef,
                        ) &&
                        event.endpointOrigin ==
                        null &&
                        event.model ==
                        null &&
                        event.runtimeObservationRef ==
                        null &&
                        event.invocationEvidenceRef ==
                        null,
                ) {
                    "direct Conversation REQUEST semantics are invalid"
                }

            "RESPONSE" -> {
                require(
                    event.speakerRef ==
                        manifest.currentCompanionLineageRef &&
                        event.recipientRefs ==
                        listOf(
                            VexLifeProdHome.DefaultPersonRef,
                        ) &&
                        event.model
                            ?.isNotEmpty() ==
                        true &&
                        event.runtimeObservationRef !=
                        null &&
                        event.invocationEvidenceRef !=
                        null,
                ) {
                    "direct Conversation RESPONSE semantics are invalid"
                }
                requireLoopbackOrigin(
                    requireNotNull(
                        event.endpointOrigin,
                    ),
                )
                requireRef(
                    requireNotNull(
                        event.runtimeObservationRef,
                    ),
                    "event.runtimeObservationRef",
                )
                requireRef(
                    requireNotNull(
                        event.invocationEvidenceRef,
                    ),
                    "event.invocationEvidenceRef",
                )
            }

            else ->
                error(
                    "direct Conversation event kind is not admitted",
                )
        }
    }

    private fun validateHead(
        head: Head,
    ) {
        require(
            head.schemaVersion ==
                HeadSchema &&
                head.homeRef ==
                manifest.homeRef &&
                head.deviceRef ==
                manifest.currentDeviceRef &&
                head.companionLineageRef ==
                manifest.currentCompanionLineageRef &&
                head.projectRef ==
                DirectConversationIdentity.ProjectRef &&
                head.threadRef ==
                DirectConversationIdentity.ThreadRef &&
                head.channelRef ==
                DirectConversationIdentity.ChannelRef &&
                head.sequence >=
                1 &&
                head.sequence %
                2 ==
                1L,
        ) {
            "direct Conversation head identity is invalid"
        }
        for (
            ref in
            listOf(
                head.turnRef,
                head.requestMessageRef,
                head.responseMessageRef,
            )
        ) {
            requireRef(
                ref,
                "head ref",
            )
        }
        requireSha256(
            head.requestEventSha256,
            "head.requestEventSha256",
        )
        requireSha256(
            head.responseEventSha256,
            "head.responseEventSha256",
        )
        head.priorConversationHeadSha256?.let {
            requireSha256(
                it,
                "head.priorConversationHeadSha256",
            )
        }

        val core =
            HeadCore(
                schemaVersion = head.schemaVersion,
                homeRef = head.homeRef,
                deviceRef = head.deviceRef,
                companionLineageRef = head.companionLineageRef,
                projectRef = head.projectRef,
                threadRef = head.threadRef,
                channelRef = head.channelRef,
                turnRef = head.turnRef,
                requestMessageRef = head.requestMessageRef,
                responseMessageRef = head.responseMessageRef,
                requestEventSha256 = head.requestEventSha256,
                responseEventSha256 = head.responseEventSha256,
                sequence = head.sequence,
                priorConversationHeadSha256 = head.priorConversationHeadSha256,
                formedAt = head.formedAt,
            )
        val expected =
            semanticHash(
                core,
            )
        require(
            expected ==
                head.conversationHeadSha256 &&
                head.headRef ==
                "head.vexlife-prod.direct." +
                    expected.take(
                        32,
                    ),
        ) {
            "direct Conversation head hash/address is invalid"
        }
    }

    private fun eventPath(
        event: Event,
    ): Path =
        VexOwnedPath.resolveInside(
            eventsDirectory,
            eventFilename(
                event,
            ),
        )

    private fun eventFilename(
        event: Event,
    ): String =
        event.sequence
            .toString()
            .padStart(
                8,
                '0',
            ) +
            "-" +
            event.eventSha256 +
            ".json"

    private fun immutableHeadPath(
        hash: String,
    ): Path =
        VexOwnedPath.resolveInside(
            headsDirectory,
            "$hash.json",
        )

    private fun semanticHash(
        value: Any,
    ): String =
        when (
            value
        ) {
            is EventCore ->
                sha256(
                    VexContractJson.encode(
                        value,
                    ),
                )

            is HeadCore ->
                sha256(
                    VexContractJson.encode(
                        value,
                    ),
                )

            else ->
                error(
                    "unsupported direct Conversation semantic hash input",
                )
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

    private fun requireRef(
        value: String,
        label: String,
    ) {
        require(
            value.matches(
                Regex(
                    "[A-Za-z0-9][A-Za-z0-9._:-]{0,220}",
                ),
            ),
        ) {
            "$label must be one portable stable ref"
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
            "$label must be lowercase SHA-256"
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
            "direct Conversation response origin must be numeric-loopback HTTP"
        }
    }
}

// [VXG RealForever]
