package vexlife.service

import java.nio.charset.StandardCharsets
import java.nio.file.Path
import vexlife.artifact.VexLifeHistoricalRequestResources
import vexlife.artifact.VexLifeLocalArtifact
import vexlife.artifact.VexLifeLocalArtifactStore
import vexlife.conversation.DirectConversationStore
import vexlife.conversation.VexLifeConversationContract
import vexlife.text.VexLifeText

/**
 * Read-only service projection for resources already bound to canonical human Conversation requests.
 *
 * RequestBinding remains the relationship truth. Conversation remains the canonical-message truth.
 * This component joins those owners only when the person asks to review one historical resource.
 *
 * Ordinary timeline projection uses [timelineResources] and reads only the tiny immutable
 * RequestBinding. Exact artifact/projection inspection remains lazy behind [metadata] / [content].
 */
class VexLifeSentResourceReview(
    productHome: Path,
) {
    companion object {
        /**
         * Human historical review is an inline preview, not a second whole-file transport.
         *
         * The full projection byte/line counts remain in metadata. Exact source stays in Local
         * Artifact custody and can support a future explicit download/open Control if that outcome
         * is admitted separately.
         */
        const val MaximumInlineTextReviewCharacters = 8_192
    }
    private val store =
        VexLifeLocalArtifactStore(
            productHome,
        )
    private val conversation =
        DirectConversationStore(
            productHome,
        )

    sealed interface Lookup<out T> {
        data class Found<T>(
            val value: T,
        ) : Lookup<T>

        data class NotFound(
            val reason: String,
        ) : Lookup<Nothing>

        data class Refused(
            val reason: String,
        ) : Lookup<Nothing>
    }

    data class Content(
        val metadata: VexLifeProdServiceContract.ConversationResourceMetadata,
        val bytes: ByteArray,
        val contentType: String,
        val displayName: String,
    ) {
        init {
            require(
                bytes.isNotEmpty() &&
                    contentType.isNotBlank() &&
                    displayName ==
                    metadata.displayName &&
                    metadata.contentAvailable,
            ) {
                "historical Conversation resource review content is invalid"
            }
        }
    }

    private data class Resolved(
        val request: DirectConversationStore.Event,
        val resource: VexLifeHistoricalRequestResources.Resource,
        val metadata: VexLifeProdServiceContract.ConversationResourceMetadata,
    )

    /**
     * Lightweight timeline relationship only: no projection decode, raw-byte read, or hashing.
     */
    fun timelineResources(
        request: DirectConversationStore.Event,
    ): List<VexLifeProdServiceContract.ConversationResource> {
        require(
            request.eventKind ==
                VexLifeConversationContract.RequestKind,
        ) {
            "historical resource timeline projection requires one canonical REQUEST"
        }

        val binding =
            store.historicalBindingFor(
                request.eventHash,
            )
                ?: return emptyList()

        require(
            binding.requestEventRef ==
                request.eventRef &&
                binding.requestEventHash ==
                request.eventHash &&
                binding.requestContentHash ==
                request.contentHash &&
                binding.turnRef ==
                request.turnRef &&
                binding.requestMessageRef ==
                request.messageRef,
        ) {
            "historical request-resource binding does not belong to the exact Conversation REQUEST"
        }

        return binding.attachments.mapIndexed {
            index,
            attachment,
            ->
            VexLifeProdServiceContract.ConversationResource(
                resourceRef =
                    VexLifeLocalArtifact.requestResourceRef(
                        requestEventHash = request.eventHash,
                        ordinal = index + 1,
                    ),
                displayName = attachment.displayName,
            )
        }
    }

    fun metadata(
        requestSequence: Long,
        requestEventRef: String,
        requestEventHash: String,
        resourceRef: String,
    ): Lookup<VexLifeProdServiceContract.ConversationResourceMetadata> =
        when (
            val resolved =
                resolve(
                    requestSequence = requestSequence,
                    requestEventRef = requestEventRef,
                    requestEventHash = requestEventHash,
                    resourceRef = resourceRef,
                )
        ) {
            is Lookup.Found ->
                Lookup.Found(
                    resolved.value.metadata,
                )

            is Lookup.NotFound ->
                resolved

            is Lookup.Refused ->
                resolved
        }

    fun content(
        requestSequence: Long,
        requestEventRef: String,
        requestEventHash: String,
        resourceRef: String,
    ): Lookup<Content> {
        val resolved =
            when (
                val lookup =
                    resolve(
                        requestSequence = requestSequence,
                        requestEventRef = requestEventRef,
                        requestEventHash = requestEventHash,
                        resourceRef = resourceRef,
                    )
            ) {
                is Lookup.Found ->
                    lookup.value

                is Lookup.NotFound ->
                    return lookup

                is Lookup.Refused ->
                    return lookup
            }

        val metadata =
            resolved.metadata
        if (
            !metadata.contentAvailable
        ) {
            return Lookup.Refused(
                when {
                    metadata.originalCustody ==
                        VexLifeProdServiceContract.ConversationResourceCustody.INVALID ||
                        metadata.projectionCustody ==
                        VexLifeProdServiceContract.ConversationResourceCustody.INVALID ->
                        "The historical resource relationship is intact, but its current local realization failed integrity validation."

                    else ->
                        "The historical resource relationship is intact, but reviewable local content is not available now."
                },
            )
        }

        return when (
            resolved.resource.representation
        ) {
            VexLifeHistoricalRequestResources.Representation.PARSED_TEXT -> {
                val projection =
                    requireNotNull(
                        store.historicalRequestProjection(
                            artifactRef = resolved.resource.artifactRef,
                            projectionRef = resolved.resource.projectionRef,
                        ) as? VexLifeLocalArtifact.DerivedProjection,
                    ) {
                        "exact historical text projection disappeared during review"
                    }
                Lookup.Found(
                    Content(
                        metadata = metadata,
                        bytes =
                            projection.text
                                .take(
                                    MaximumInlineTextReviewCharacters +
                                        1,
                                )
                                .toByteArray(
                                    StandardCharsets.UTF_8,
                                ),
                        contentType = "text/plain; charset=utf-8",
                        displayName = metadata.displayName,
                    ),
                )
            }

            VexLifeHistoricalRequestResources.Representation.STATIC_IMAGE -> {
                val bytes =
                    requireNotNull(
                        store.rawBytes(
                            resolved.resource.artifactRef,
                        ),
                    ) {
                        "exact historical image bytes disappeared during review"
                    }
                val mediaType =
                    requireNotNull(
                        metadata.mediaType,
                    ) {
                        "exact historical image review has no media type"
                    }
                require(
                    mediaType in
                        setOf(
                            "image/png",
                            "image/jpeg",
                        ),
                ) {
                    "historical image review media type is not admitted"
                }
                Lookup.Found(
                    Content(
                        metadata = metadata,
                        bytes = bytes,
                        contentType = mediaType,
                        displayName = metadata.displayName,
                    ),
                )
            }

            VexLifeHistoricalRequestResources.Representation.UNKNOWN ->
                Lookup.Refused(
                    "The historical resource relationship is intact, but no exact review representation is available.",
                )
        }
    }

    private fun resolve(
        requestSequence: Long,
        requestEventRef: String,
        requestEventHash: String,
        resourceRef: String,
    ): Lookup<Resolved> {
        if (
            requestSequence <
                0 ||
            requestEventRef.isBlank()
        ) {
            return Lookup.Refused(
                "historical resource review requires one exact Conversation message identity",
            )
        }

        val ordinal =
            VexLifeLocalArtifact.requestResourceOrdinal(
                requestEventHash = requestEventHash,
                resourceRef = resourceRef,
            )
                ?: return Lookup.Refused(
                    "resourceRef does not belong to this exact Conversation REQUEST",
                )

        if (
            store.historicalBindingFor(
                requestEventHash,
            ) ==
                null
        ) {
            return Lookup.NotFound(
                "No local resource relationship is bound to that Conversation REQUEST.",
            )
        }

        val request =
            conversation.current()
                .canonicalEvents
                .singleOrNull {
                    event ->
                    event.sequence ==
                        requestSequence &&
                        event.eventRef ==
                        requestEventRef &&
                        event.eventHash ==
                        requestEventHash
                }
                ?: return Lookup.NotFound(
                    "That exact canonical Conversation REQUEST is not present in current history.",
                )

        if (
            request.eventKind !=
                VexLifeConversationContract.RequestKind
        ) {
            return Lookup.Refused(
                "historical resource review is available only for canonical human REQUEST events",
            )
        }

        val inspection =
            VexLifeHistoricalRequestResources.inspect(
                store = store,
                request = request,
            )
                ?: return Lookup.NotFound(
                    "No local resource relationship is bound to that Conversation REQUEST.",
                )
        val resource =
            inspection.resources
                .getOrNull(
                    ordinal - 1,
                )
                ?: return Lookup.NotFound(
                    "That request resource does not exist.",
                )

        val expectedRef =
            VexLifeLocalArtifact.requestResourceRef(
                requestEventHash = requestEventHash,
                ordinal = ordinal,
            )
        if (
            resourceRef !=
                expectedRef
        ) {
            return Lookup.Refused(
                "resourceRef does not belong to this exact Conversation REQUEST",
            )
        }

        return Lookup.Found(
            Resolved(
                request = request,
                resource = resource,
                metadata =
                    metadataFor(
                        request = request,
                        resourceRef = resourceRef,
                        resource = resource,
                    ),
            ),
        )
    }

    private fun metadataFor(
        request: DirectConversationStore.Event,
        resourceRef: String,
        resource: VexLifeHistoricalRequestResources.Resource,
    ): VexLifeProdServiceContract.ConversationResourceMetadata {
        val representation =
            when (
                resource.representation
            ) {
                VexLifeHistoricalRequestResources.Representation.PARSED_TEXT ->
                    VexLifeProdServiceContract.ConversationResourceRepresentation.PARSED_TEXT

                VexLifeHistoricalRequestResources.Representation.STATIC_IMAGE ->
                    VexLifeProdServiceContract.ConversationResourceRepresentation.STATIC_IMAGE

                VexLifeHistoricalRequestResources.Representation.UNKNOWN ->
                    VexLifeProdServiceContract.ConversationResourceRepresentation.UNKNOWN
            }

        val originalCustody =
            resource.originalCustody.toWire()
        val projectionCustody =
            resource.projectionCustody.toWire()

        val status =
            when {
                originalCustody ==
                    VexLifeProdServiceContract.ConversationResourceCustody.INVALID ||
                    projectionCustody ==
                    VexLifeProdServiceContract.ConversationResourceCustody.INVALID ->
                    VexLifeText(
                        VexLifeProdServiceContract.ConversationResourceInvalidTextKey,
                    )

                (
                    representation ==
                        VexLifeProdServiceContract.ConversationResourceRepresentation.STATIC_IMAGE &&
                        originalCustody !=
                        VexLifeProdServiceContract.ConversationResourceCustody.EXACT
                ) ||
                    projectionCustody !=
                    VexLifeProdServiceContract.ConversationResourceCustody.EXACT ->
                    VexLifeText(
                        VexLifeProdServiceContract.ConversationResourceUnavailableTextKey,
                    )

                else ->
                    null
            }

        return VexLifeProdServiceContract.ConversationResourceMetadata(
            schemaVersion =
                VexLifeProdServiceContract.ConversationResourceMetadataSchema,
            requestEventRef = request.eventRef,
            requestEventHash = request.eventHash,
            requestSequence = request.sequence,
            resourceRef = resourceRef,
            displayName = resource.displayName,
            representation = representation,
            originalCustody = originalCustody,
            projectionCustody = projectionCustody,
            readableBytes = resource.readableBytes,
            lineCount = resource.lineCount,
            mediaType = resource.mediaType,
            widthPixels = resource.widthPixels,
            heightPixels = resource.heightPixels,
            contentAvailable =
                when (
                    representation
                ) {
                    VexLifeProdServiceContract.ConversationResourceRepresentation.PARSED_TEXT ->
                        projectionCustody ==
                            VexLifeProdServiceContract.ConversationResourceCustody.EXACT

                    VexLifeProdServiceContract.ConversationResourceRepresentation.STATIC_IMAGE ->
                        projectionCustody ==
                            VexLifeProdServiceContract.ConversationResourceCustody.EXACT &&
                            originalCustody ==
                            VexLifeProdServiceContract.ConversationResourceCustody.EXACT

                    VexLifeProdServiceContract.ConversationResourceRepresentation.UNKNOWN ->
                        false
                },
            status = status,
            effectsPerformed = false,
        )
    }

    private fun VexLifeHistoricalRequestResources.CustodyObservation.toWire():
        VexLifeProdServiceContract.ConversationResourceCustody =
        when (
            this
        ) {
            VexLifeHistoricalRequestResources.CustodyObservation.EXACT ->
                VexLifeProdServiceContract.ConversationResourceCustody.EXACT

            VexLifeHistoricalRequestResources.CustodyObservation.MISSING ->
                VexLifeProdServiceContract.ConversationResourceCustody.MISSING

            VexLifeHistoricalRequestResources.CustodyObservation.INVALID ->
                VexLifeProdServiceContract.ConversationResourceCustody.INVALID
        }
}

// [VXG RealForever]
