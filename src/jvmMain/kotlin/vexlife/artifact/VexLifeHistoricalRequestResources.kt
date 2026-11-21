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
 *
 * [inspect] performs exact custody validation and may therefore read/hash bounded artifact bytes and
 * decode a bounded derived projection. It is a detail/review operation for one exact request, not a
 * whole-timeline rendering primitive. Timeline discovery should use
 * [VexLifeLocalArtifactStore.historicalBindingFor] only.
 */
object VexLifeHistoricalRequestResources {
    enum class Representation {
        PARSED_TEXT,
        STATIC_IMAGE,
        UNKNOWN,
    }

    /**
     * Read-time observation only. This is not a durable retention/delete state.
     *
     * MISSING means the exact local realization needed for this observation is absent.
     * INVALID means present metadata/bytes fail the existing integrity contract.
     * Neither means "removed by user" until a future rightful retention Control records that intent.
     */
    enum class CustodyObservation {
        EXACT,
        MISSING,
        INVALID,
    }

    data class Resource(
        val artifactRef: String,
        val projectionRef: String,
        val displayName: String,
        val representation: Representation,
        val originalCustody: CustodyObservation,
        val projectionCustody: CustodyObservation,
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
                        projectionCustody == CustodyObservation.EXACT &&
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
                        projectionCustody == CustodyObservation.EXACT &&
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
                        projectionCustody != CustodyObservation.EXACT &&
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

    fun inspect(
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

        val originalObservations =
            mutableMapOf<String, CustodyObservation>()
        val projectionObservations =
            mutableMapOf<Pair<String, String>, ProjectionObservation>()

        return Projection(
            requestEventRef = binding.requestEventRef,
            requestEventHash = binding.requestEventHash,
            requestContentHash = binding.requestContentHash,
            turnRef = binding.turnRef,
            requestMessageRef = binding.requestMessageRef,
            resources =
                binding.attachments.map {
                    attachment ->
                    val originalCustody =
                        originalObservations.getOrPut(
                            attachment.artifactRef,
                        ) {
                            observeOriginalCustody(
                                store = store,
                                artifactRef = attachment.artifactRef,
                            )
                        }
                    val projectionKey =
                        attachment.artifactRef to
                            attachment.projectionRef
                    val projectionObservation =
                        projectionObservations.getOrPut(
                            projectionKey,
                        ) {
                            observeProjection(
                                store = store,
                                artifactRef = attachment.artifactRef,
                                projectionRef = attachment.projectionRef,
                            )
                        }
                    resource(
                        attachment = attachment,
                        originalCustody = originalCustody,
                        projectionObservation = projectionObservation,
                    )
                },
        )
    }

    private data class ProjectionObservation(
        val custody: CustodyObservation,
        val projection: VexLifeLocalArtifact.RequestProjection?,
    ) {
        init {
            require(
                (custody == CustodyObservation.EXACT) ==
                    (projection != null),
            ) {
                "historical projection observation is internally inconsistent"
            }
        }
    }

    private fun observeOriginalCustody(
        store: VexLifeLocalArtifactStore,
        artifactRef: String,
    ): CustodyObservation =
        try {
            when (
                store.historicalOriginalFileClassification(
                    artifactRef,
                )
            ) {
                null,
                is ExactArtifactFile.Classification.Missing ->
                    CustodyObservation.MISSING

                is ExactArtifactFile.Classification.InvalidSize,
                is ExactArtifactFile.Classification.InvalidHash ->
                    CustodyObservation.INVALID

                is ExactArtifactFile.Classification.VerifiedReusable ->
                    CustodyObservation.EXACT
            }
        } catch (
            _: IllegalArgumentException,
        ) {
            CustodyObservation.INVALID
        } catch (
            _: IllegalStateException,
        ) {
            CustodyObservation.INVALID
        }

    private fun observeProjection(
        store: VexLifeLocalArtifactStore,
        artifactRef: String,
        projectionRef: String,
    ): ProjectionObservation =
        try {
            val projection =
                store.historicalRequestProjection(
                    artifactRef = artifactRef,
                    projectionRef = projectionRef,
                )
            if (
                projection ==
                    null
            ) {
                ProjectionObservation(
                    custody = CustodyObservation.MISSING,
                    projection = null,
                )
            } else {
                ProjectionObservation(
                    custody = CustodyObservation.EXACT,
                    projection = projection,
                )
            }
        } catch (
            _: IllegalArgumentException,
        ) {
            ProjectionObservation(
                custody = CustodyObservation.INVALID,
                projection = null,
            )
        } catch (
            _: IllegalStateException,
        ) {
            ProjectionObservation(
                custody = CustodyObservation.INVALID,
                projection = null,
            )
        }

    private fun resource(
        attachment: VexLifeLocalArtifact.RequestAttachment,
        originalCustody: CustodyObservation,
        projectionObservation: ProjectionObservation,
    ): Resource {
        val projection =
            projectionObservation.projection

        return when (projection) {
            is VexLifeLocalArtifact.DerivedProjection ->
                Resource(
                    artifactRef = attachment.artifactRef,
                    projectionRef = attachment.projectionRef,
                    displayName = attachment.displayName,
                    representation = Representation.PARSED_TEXT,
                    originalCustody = originalCustody,
                    projectionCustody = projectionObservation.custody,
                    readableBytes = projection.byteCount,
                    lineCount = projection.lineCount,
                )

            is VexLifeLocalArtifact.StaticImageProjection ->
                Resource(
                    artifactRef = attachment.artifactRef,
                    projectionRef = attachment.projectionRef,
                    displayName = attachment.displayName,
                    representation = Representation.STATIC_IMAGE,
                    originalCustody = originalCustody,
                    projectionCustody = projectionObservation.custody,
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
                    originalCustody = originalCustody,
                    projectionCustody = projectionObservation.custody,
                )
        }
    }
}

// [VXG RealForever]
