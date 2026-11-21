package vexlife.service

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import vexlife.artifact.VexLifeLocalArtifact
import vexlife.artifact.VexLifeLocalArtifactStore
import vexlife.conversation.CanonicalLivedContextFixture
import vexlife.conversation.DirectConversationStore
import vexlife.home.CanonicalVexLifeHomeFixture

class VexLifeSentResourceReviewTest {
    @Test
    fun canonicalRequestKeepsLightweightResourcesAndLazilyReviewsTextAndImage() {
        val parent =
            Files.createTempDirectory(
                "vexlife-sent-resource-review-",
            )
                .toAbsolutePath()
        val home =
            parent.resolve(
                "VexLife-Prod",
            )

        try {
            CanonicalVexLifeHomeFixture.form(
                home = home,
                deviceSuffix = "sent-resource-review",
                lineageSuffix = "sent-resource-review",
            )
            val artifacts =
                VexLifeLocalArtifactStore(
                    home,
                )
            val textOriginal =
                artifacts.storeOriginal(
                    "alpha\nbeta\n"
                        .toByteArray(
                            StandardCharsets.UTF_8,
                        ),
                )
            val textProjection =
                artifacts.storeTextProjection(
                    artifactRef = textOriginal.artifactRef,
                    converterRef = "converter.vexlife-prod.plain-text.v1",
                    text = "alpha\nbeta\n",
                )
            val imageBytes =
                byteArrayOf(
                    1,
                    2,
                    3,
                    4,
                )
            val imageOriginal =
                artifacts.storeOriginal(
                    imageBytes,
                )
            val imageProjection =
                artifacts.storeStaticImageProjection(
                    artifactRef = imageOriginal.artifactRef,
                    adapterRef = "adapter.vexlife-prod.static-image.png.v1",
                    mediaType = "image/png",
                    widthPixels = 1170,
                    heightPixels = 2532,
                )

            lateinit var request: DirectConversationStore.Event
            DirectConversationStore(
                home,
            )
                .withWriterLease {
                    request =
                        beginRequest(
                            turnRef = "turn.sent-resource-review.001",
                            requestMessageRef =
                                "message.sent-resource-review.human.001",
                            content = "Compare the note and screenshot I sent.",
                            formedAt =
                                Instant.parse(
                                    "2026-11-21T10:00:00Z",
                                ),
                        )
                    artifacts.bindRequest(
                        request = request,
                        attachments =
                            listOf(
                                VexLifeLocalArtifact.RequestAttachment(
                                    artifactRef = textOriginal.artifactRef,
                                    projectionRef = textProjection.projectionRef,
                                    displayName = "notes.txt",
                                ),
                                VexLifeLocalArtifact.RequestAttachment(
                                    artifactRef = imageOriginal.artifactRef,
                                    projectionRef = imageProjection.projectionRef,
                                    displayName = "screenshot.png",
                                ),
                            ),
                    )
                    val response =
                        appendResponse(
                            request = request,
                            responseMessageRef =
                                "message.sent-resource-review.vex.001",
                            content = "I can compare them.",
                            model = "default_model",
                            endpointOrigin = "http://127.0.0.1:8765",
                            runtimeObservationRef =
                                "observation.vexlife-prod.model-runtime.sent-resource-review.001",
                            invocationEvidenceRef =
                                "evidence.vexlife-prod.model-runtime-invocation.sent-resource-review.001",
                            formedAt =
                                Instant.parse(
                                    "2026-11-21T10:00:01Z",
                                ),
                        )
                    val context =
                        CanonicalLivedContextFixture.write(
                            productHome = home,
                            request = request,
                            response = response,
                            formedAt =
                                Instant.parse(
                                    "2026-11-21T10:00:02Z",
                                ),
                        )
                    commitCompletedTurn(
                        request = request,
                        response = response,
                        contextSha256 = context.sha256,
                        formedAt =
                            Instant.parse(
                                "2026-11-21T10:00:03Z",
                            ),
                    )
                }

            val review =
                VexLifeSentResourceReview(
                    home,
                )
            val timeline =
                review.timelineResources(
                    request,
                )

            assertEquals(
                listOf(
                    "notes.txt",
                    "screenshot.png",
                ),
                timeline.map {
                    it.displayName
                },
            )
            assertEquals(
                listOf(
                    VexLifeLocalArtifact.requestResourceRef(
                        requestEventHash = request.eventHash,
                        ordinal = 1,
                    ),
                    VexLifeLocalArtifact.requestResourceRef(
                        requestEventHash = request.eventHash,
                        ordinal = 2,
                    ),
                ),
                timeline.map {
                    it.resourceRef
                },
            )

            val textMetadata =
                assertIs<
                    VexLifeSentResourceReview.Lookup.Found<
                        VexLifeProdServiceContract.ConversationResourceMetadata
                    >
                >(
                    review.metadata(
                        requestSequence = request.sequence,
                        requestEventRef = request.eventRef,
                        requestEventHash = request.eventHash,
                        resourceRef = timeline[0].resourceRef,
                    ),
                )
                    .value
            assertEquals(
                VexLifeProdServiceContract.ConversationResourceRepresentation.PARSED_TEXT,
                textMetadata.representation,
            )
            assertEquals(
                VexLifeProdServiceContract.ConversationResourceCustody.EXACT,
                textMetadata.originalCustody,
            )
            assertEquals(
                VexLifeProdServiceContract.ConversationResourceCustody.EXACT,
                textMetadata.projectionCustody,
            )
            assertTrue(
                textMetadata.contentAvailable,
            )
            assertEquals(
                null,
                textMetadata.status,
                "exact reviewable content needs no extra status prose",
            )

            val textContent =
                assertIs<
                    VexLifeSentResourceReview.Lookup.Found<
                        VexLifeSentResourceReview.Content
                    >
                >(
                    review.content(
                        requestSequence = request.sequence,
                        requestEventRef = request.eventRef,
                        requestEventHash = request.eventHash,
                        resourceRef = timeline[0].resourceRef,
                    ),
                )
                    .value
            assertEquals(
                "text/plain; charset=utf-8",
                textContent.contentType,
            )
            assertEquals(
                "alpha\nbeta\n",
                textContent.bytes.toString(
                    StandardCharsets.UTF_8,
                ),
            )

            val imageMetadata =
                assertIs<
                    VexLifeSentResourceReview.Lookup.Found<
                        VexLifeProdServiceContract.ConversationResourceMetadata
                    >
                >(
                    review.metadata(
                        requestSequence = request.sequence,
                        requestEventRef = request.eventRef,
                        requestEventHash = request.eventHash,
                        resourceRef = timeline[1].resourceRef,
                    ),
                )
                    .value
            assertEquals(
                VexLifeProdServiceContract.ConversationResourceRepresentation.STATIC_IMAGE,
                imageMetadata.representation,
            )
            assertEquals(
                "image/png",
                imageMetadata.mediaType,
            )
            assertEquals(
                1170,
                imageMetadata.widthPixels,
            )
            assertEquals(
                2532,
                imageMetadata.heightPixels,
            )
            assertTrue(
                imageMetadata.contentAvailable,
            )

            val imageContent =
                assertIs<
                    VexLifeSentResourceReview.Lookup.Found<
                        VexLifeSentResourceReview.Content
                    >
                >(
                    review.content(
                        requestSequence = request.sequence,
                        requestEventRef = request.eventRef,
                        requestEventHash = request.eventHash,
                        resourceRef = timeline[1].resourceRef,
                    ),
                )
                    .value
            assertEquals(
                "image/png",
                imageContent.contentType,
            )
            assertContentEquals(
                imageBytes,
                imageContent.bytes,
            )
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun inlineTextReviewIsBoundedBeforeBrowserPresentation() {
        val parent =
            Files.createTempDirectory(
                "vexlife-sent-resource-review-bounded-text-",
            )
                .toAbsolutePath()
        val home =
            parent.resolve(
                "VexLife-Prod",
            )

        try {
            CanonicalVexLifeHomeFixture.form(
                home = home,
                deviceSuffix = "sent-resource-review-bounded-text",
                lineageSuffix = "sent-resource-review-bounded-text",
            )
            val artifacts =
                VexLifeLocalArtifactStore(
                    home,
                )
            val prefix =
                "x".repeat(
                    VexLifeSentResourceReview.MaximumInlineTextReviewCharacters,
                )
            val fullText =
                prefix +
                    "😀" +
                    "tail".repeat(
                        1_000,
                    )
            val original =
                artifacts.storeOriginal(
                    fullText.toByteArray(
                        StandardCharsets.UTF_8,
                    ),
                )
            val projection =
                artifacts.storeTextProjection(
                    artifactRef = original.artifactRef,
                    converterRef = "converter.vexlife-prod.plain-text.v1",
                    text = fullText,
                )

            lateinit var request: DirectConversationStore.Event
            DirectConversationStore(
                home,
            )
                .withWriterLease {
                    request =
                        beginRequest(
                            turnRef = "turn.sent-resource-review-bounded-text.001",
                            requestMessageRef =
                                "message.sent-resource-review-bounded-text.human.001",
                            content = "Review this long note.",
                        )
                    artifacts.bindRequest(
                        request = request,
                        attachments =
                            listOf(
                                VexLifeLocalArtifact.RequestAttachment(
                                    artifactRef = original.artifactRef,
                                    projectionRef = projection.projectionRef,
                                    displayName = "long-note.txt",
                                ),
                            ),
                    )
                    val response =
                        appendResponse(
                            request = request,
                            responseMessageRef =
                                "message.sent-resource-review-bounded-text.vex.001",
                            content = "Received.",
                            model = "default_model",
                            endpointOrigin = "http://127.0.0.1:8765",
                            runtimeObservationRef =
                                "observation.vexlife-prod.model-runtime.sent-resource-review-bounded-text.001",
                            invocationEvidenceRef =
                                "evidence.vexlife-prod.model-runtime-invocation.sent-resource-review-bounded-text.001",
                        )
                    val context =
                        CanonicalLivedContextFixture.write(
                            productHome = home,
                            request = request,
                            response = response,
                        )
                    commitCompletedTurn(
                        request = request,
                        response = response,
                        contextSha256 = context.sha256,
                    )
                }

            val review =
                VexLifeSentResourceReview(
                    home,
                )
            val resource =
                review.timelineResources(
                    request,
                )
                    .single()
            val metadata =
                assertIs<
                    VexLifeSentResourceReview.Lookup.Found<
                        VexLifeProdServiceContract.ConversationResourceMetadata
                    >
                >(
                    review.metadata(
                        requestSequence = request.sequence,
                        requestEventRef = request.eventRef,
                        requestEventHash = request.eventHash,
                        resourceRef = resource.resourceRef,
                    ),
                )
                    .value
            val content =
                assertIs<
                    VexLifeSentResourceReview.Lookup.Found<
                        VexLifeSentResourceReview.Content
                    >
                >(
                    review.content(
                        requestSequence = request.sequence,
                        requestEventRef = request.eventRef,
                        requestEventHash = request.eventHash,
                        resourceRef = resource.resourceRef,
                    ),
                )
                    .value
            val preview =
                content.bytes.toString(
                    StandardCharsets.UTF_8,
                )

            assertEquals(
                fullText
                    .toByteArray(
                        StandardCharsets.UTF_8,
                    )
                    .size
                    .toLong(),
                metadata.readableBytes,
                "metadata must keep describing the full immutable projection",
            )
            assertEquals(
                VexLifeSentResourceReview.MaximumInlineTextReviewCharacters,
                preview.length,
                "inline review must stop before a split surrogate pair at the bound",
            )
            assertEquals(
                prefix,
                preview,
                "bounded review must preserve an exact Unicode-safe source prefix",
            )
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun missingImageBytesKeepTheCanonicalRelationshipButRefuseContentReview() {
        val parent =
            Files.createTempDirectory(
                "vexlife-sent-resource-review-missing-",
            )
                .toAbsolutePath()
        val home =
            parent.resolve(
                "VexLife-Prod",
            )

        try {
            CanonicalVexLifeHomeFixture.form(
                home = home,
                deviceSuffix = "sent-resource-review-missing",
                lineageSuffix = "sent-resource-review-missing",
            )
            val artifacts =
                VexLifeLocalArtifactStore(
                    home,
                )
            val original =
                artifacts.storeOriginal(
                    byteArrayOf(
                        9,
                        8,
                        7,
                    ),
                )
            val projection =
                artifacts.storeStaticImageProjection(
                    artifactRef = original.artifactRef,
                    adapterRef = "adapter.vexlife-prod.static-image.jpeg.v1",
                    mediaType = "image/jpeg",
                    widthPixels = 640,
                    heightPixels = 480,
                )

            lateinit var request: DirectConversationStore.Event
            DirectConversationStore(
                home,
            )
                .withWriterLease {
                    request =
                        beginRequest(
                            turnRef = "turn.sent-resource-review-missing.001",
                            requestMessageRef =
                                "message.sent-resource-review-missing.human.001",
                            content = "Keep this historical image relation.",
                        )
                    artifacts.bindRequest(
                        request = request,
                        attachments =
                            listOf(
                                VexLifeLocalArtifact.RequestAttachment(
                                    artifactRef = original.artifactRef,
                                    projectionRef = projection.projectionRef,
                                    displayName = "history.jpg",
                                ),
                            ),
                    )
                    val response =
                        appendResponse(
                            request = request,
                            responseMessageRef =
                                "message.sent-resource-review-missing.vex.001",
                            content = "Received.",
                            model = "default_model",
                            endpointOrigin = "http://127.0.0.1:8765",
                            runtimeObservationRef =
                                "observation.vexlife-prod.model-runtime.sent-resource-review-missing.001",
                            invocationEvidenceRef =
                                "evidence.vexlife-prod.model-runtime-invocation.sent-resource-review-missing.001",
                        )
                    val context =
                        CanonicalLivedContextFixture.write(
                            productHome = home,
                            request = request,
                            response = response,
                        )
                    commitCompletedTurn(
                        request = request,
                        response = response,
                        contextSha256 = context.sha256,
                    )
                }

            Files.delete(
                home.resolve(
                    original.custodyRelativePath,
                ),
            )

            val review =
                VexLifeSentResourceReview(
                    home,
                )
            val resource =
                review.timelineResources(
                    request,
                )
                    .single()
            val metadata =
                assertIs<
                    VexLifeSentResourceReview.Lookup.Found<
                        VexLifeProdServiceContract.ConversationResourceMetadata
                    >
                >(
                    review.metadata(
                        requestSequence = request.sequence,
                        requestEventRef = request.eventRef,
                        requestEventHash = request.eventHash,
                        resourceRef = resource.resourceRef,
                    ),
                )
                    .value

            assertEquals(
                VexLifeProdServiceContract.ConversationResourceCustody.MISSING,
                metadata.originalCustody,
            )
            assertEquals(
                VexLifeProdServiceContract.ConversationResourceCustody.EXACT,
                metadata.projectionCustody,
            )
            assertEquals(
                VexLifeProdServiceContract.ConversationResourceRepresentation.STATIC_IMAGE,
                metadata.representation,
            )
            assertEquals(
                false,
                metadata.contentAvailable,
            )
            assertEquals(
                VexLifeProdServiceContract.ConversationResourceUnavailableTextKey,
                metadata.status?.key,
            )
            assertIs<VexLifeSentResourceReview.Lookup.Refused>(
                review.content(
                    requestSequence = request.sequence,
                    requestEventRef = request.eventRef,
                    requestEventHash = request.eventHash,
                    resourceRef = resource.resourceRef,
                ),
            )

            Files.write(
                home.resolve(
                    original.custodyRelativePath,
                ),
                byteArrayOf(
                    1,
                    2,
                    3,
                ),
            )
            val invalid =
                assertIs<
                    VexLifeSentResourceReview.Lookup.Found<
                        VexLifeProdServiceContract.ConversationResourceMetadata
                    >
                >(
                    review.metadata(
                        requestSequence = request.sequence,
                        requestEventRef = request.eventRef,
                        requestEventHash = request.eventHash,
                        resourceRef = resource.resourceRef,
                    ),
                )
                    .value
            assertEquals(
                VexLifeProdServiceContract.ConversationResourceCustody.INVALID,
                invalid.originalCustody,
            )
            assertEquals(
                VexLifeProdServiceContract.ConversationResourceInvalidTextKey,
                invalid.status?.key,
            )
            assertEquals(
                false,
                invalid.contentAvailable,
            )
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun offHeadRequestCannotUseItsBindingAsHistoricalReviewAuthority() {
        val parent =
            Files.createTempDirectory(
                "vexlife-sent-resource-review-off-head-",
            )
                .toAbsolutePath()
        val home =
            parent.resolve(
                "VexLife-Prod",
            )

        try {
            CanonicalVexLifeHomeFixture.form(
                home = home,
                deviceSuffix = "sent-resource-review-off-head",
                lineageSuffix = "sent-resource-review-off-head",
            )
            val artifacts =
                VexLifeLocalArtifactStore(
                    home,
                )
            val original =
                artifacts.storeOriginal(
                    "not canonical history"
                        .toByteArray(
                            StandardCharsets.UTF_8,
                        ),
                )
            val projection =
                artifacts.storeTextProjection(
                    artifactRef = original.artifactRef,
                    converterRef = "converter.vexlife-prod.plain-text.v1",
                    text = "not canonical history",
                )
            val request =
                DirectConversationStore(
                    home,
                )
                    .withWriterLease {
                        beginRequest(
                            turnRef = "turn.sent-resource-review-off-head.001",
                            requestMessageRef =
                                "message.sent-resource-review-off-head.human.001",
                            content = "This request never completed.",
                        )
                    }
            artifacts.bindRequest(
                request = request,
                attachments =
                    listOf(
                        VexLifeLocalArtifact.RequestAttachment(
                            artifactRef = original.artifactRef,
                            projectionRef = projection.projectionRef,
                            displayName = "off-head.txt",
                        ),
                    ),
            )

            val review =
                VexLifeSentResourceReview(
                    home,
                )
            val resource =
                review.timelineResources(
                    request,
                )
                    .single()

            assertIs<VexLifeSentResourceReview.Lookup.NotFound>(
                review.metadata(
                    requestSequence = request.sequence,
                    requestEventRef = request.eventRef,
                    requestEventHash = request.eventHash,
                    resourceRef = resource.resourceRef,
                ),
                "a RequestBinding does not make an abandoned/off-head request canonical history",
            )
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }
}

// [VXG RealForever]
