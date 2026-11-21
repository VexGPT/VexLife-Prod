package vexlife.artifact

import vexlife.conversation.DirectConversationStore
import vexlife.conversation.VexLifeConversationContract

/**
 * Read-only historical projection over one canonical REQUEST's already-durable resource relationship.
 *
 * RequestBinding remains the relationship truth. This object only joins that immutable binding with
 * current local-custody observations so a presentation can keep showing what was sent even when
 * original bytes or derived projection metadata are no longer available.
 *
 * It never mutates Conversation, Local Artifact custody, Memory, or model reach.
 */
object VexLifeHistoricalRequestResources {
    enum class Representation {
        PARSED_TEXT,
        STATIC_IMAGE,
        UNKNOWN,
    }

    data class Resource(
        val artifactRef: String,
        val projectionRef: String,
        val displayName: String,
        val representation: Representation,
        val originalInExactCustody: Boolean,
        val projectionInExactCustody: Boolean,
        val readableBytes: Long? = null,
        val lineCount: Int? = null,
        val mediaType: String? = null,
        val widthPixels: Int? = null,
        val heightPixels: Int? = null,
    ) {
        init {
            when (representation) {
                Representation.PARSED_TEXT ->
                    require(
                        projectionInExactCustody &&
                            readableBytes != null &&
                            readableBytes > 0 &&
                            lineCount != null &&
                            lineCount > 0 &&
                            mediaType == null &&
                            widthPixels == null &&
                            heightPixels == null,
                    ) {
                        "historical text-resource projection is incomplete"
                    }

                Representation.STATIC_IMAGE ->
                    require(
                        projectionInExactCustody &&
                            readableBytes == null &&
                            lineCount == null &&
                            !mediaType.isNullOrBlank() &&
                            widthPixels != null &&
                            widthPixels > 0 &&
                            heightPixels != null &&
                            heightPixels > 0,
                    ) {
                        "historical image-resource projection is incomplete"
                    }

                Representation.UNKNOWN ->
                    require(
                        !projectionInExactCustody &&
                            readableBytes == null &&
                            lineCount == null &&
                            mediaType == null &&
                            widthPixels == null &&
                            heightPixels == null,
                    ) {
                        "unknown historical resource must not invent projection metadata"
                    }
            }
        }
    }

    data class Projection(
        val requestEventRef: String,
        val requestEventHash: String,
        val requestContentHash: String,
        val turnRef: String,
        val requestMessageRef: String,
        val resources: List<Resource>,
    ) {
        init {
            require(resources.isNotEmpty()) {
                "historical request-resource projection must contain at least one resource"
            }
        }
    }

    fun project(
        store: VexLifeLocalArtifactStore,
        request: DirectConversationStore.Event,
    ): Projection? {
        require(
            request.eventKind ==
                VexLifeConversationContract.RequestKind,
        ) {
            "historical request-resource projection requires one canonical Conversation REQUEST"
        }

        val binding =
            store.historicalBindingFor(
                request.eventHash,
            )
                ?: return null

        require(
            binding.requestEventRef == request.eventRef &&
                binding.requestEventHash == request.eventHash &&
                binding.requestContentHash == request.contentHash &&
                binding.turnRef == request.turnRef &&
                binding.requestMessageRef == request.messageRef,
        ) {
            "historical request-resource binding does not belong to the exact REQUEST"
        }

        return Projection(
            requestEventRef = binding.requestEventRef,
            requestEventHash = binding.requestEventHash,
            requestContentHash = binding.requestContentHash,
            turnRef = binding.turnRef,
            requestMessageRef = binding.requestMessageRef,
            resources =
                binding.attachments.map {
                    attachment ->
                    resource(
                        store = store,
                        attachment = attachment,
                    )
                },
        )
    }

    private fun resource(
        store: VexLifeLocalArtifactStore,
        attachment: VexLifeLocalArtifact.RequestAttachment,
    ): Resource {
        val originalInExactCustody =
            runCatching {
                store.original(
                    attachment.artifactRef,
                )
            }
                .getOrNull() !=
                null
        val projection =
            runCatching {
                store.historicalRequestProjection(
                    artifactRef = attachment.artifactRef,
                    projectionRef = attachment.projectionRef,
                )
            }
                .getOrNull()

        return when (projection) {
            is VexLifeLocalArtifact.DerivedProjection ->
                Resource(
                    artifactRef = attachment.artifactRef,
                    projectionRef = attachment.projectionRef,
                    displayName = attachment.displayName,
                    representation = Representation.PARSED_TEXT,
                    originalInExactCustody = originalInExactCustody,
                    projectionInExactCustody = true,
                    readableBytes = projection.byteCount,
                    lineCount = projection.lineCount,
                )

            is VexLifeLocalArtifact.StaticImageProjection ->
                Resource(
                    artifactRef = attachment.artifactRef,
                    projectionRef = attachment.projectionRef,
                    displayName = attachment.displayName,
                    representation = Representation.STATIC_IMAGE,
                    originalInExactCustody = originalInExactCustody,
                    projectionInExactCustody = true,
                    mediaType = projection.mediaType,
                    widthPixels = projection.widthPixels,
                    heightPixels = projection.heightPixels,
                )

            null ->
                Resource(
                    artifactRef = attachment.artifactRef,
                    projectionRef = attachment.projectionRef,
                    displayName = attachment.displayName,
                    representation = Representation.UNKNOWN,
                    originalInExactCustody = originalInExactCustody,
                    projectionInExactCustody = false,
                )
        }
    }
}

// [VXG RealForever]
