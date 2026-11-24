package vexlife.artifact

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import vexlife.conversation.DirectConversationStore
import vexlife.home.CanonicalVexLifeHomeFixture

class VexLifeHistoricalRequestResourcesTest {
    @Test
    fun projectsTextAndImageMetadataInExactRequestOrderWithoutRewritingConversation() {
        val parent =
            Files.createTempDirectory(
                "vexlife-historical-request-resources-",
            )
                .toAbsolutePath()
        val home =
            parent.resolve(
                "VexLife-Prod",
            )

        try {
            CanonicalVexLifeHomeFixture.form(
                home = home,
                deviceSuffix = "historical-request-resources",
                lineageSuffix = "historical-request-resources",
            )
            val store =
                VexLifeLocalArtifactStore(
                    home,
                )
            val textOriginal =
                store.storeOriginal(
                    "alpha\nbeta\n"
                        .toByteArray(
                            StandardCharsets.UTF_8,
                        ),
                )
            val textProjection =
                store.storeTextProjection(
                    artifactRef = textOriginal.artifactRef,
                    converterRef = "converter.vexlife-prod.plain-text.v1",
                    text = "alpha\nbeta\n",
                )
            val imageOriginal =
                store.storeOriginal(
                    byteArrayOf(
                        1,
                        2,
                        3,
                        4,
                    ),
                )
            val imageProjection =
                store.storeStaticImageProjection(
                    artifactRef = imageOriginal.artifactRef,
                    adapterRef = "adapter.vexlife-prod.static-image.png.v1",
                    mediaType = "image/png",
                    widthPixels = 1170,
                    heightPixels = 2532,
                )
            val request =
                DirectConversationStore(
                    home,
                )
                    .withWriterLease {
                        beginRequest(
                            turnRef = "turn.historical-request-resources.001",
                            requestMessageRef =
                                "message.historical-request-resources.human.001",
                            content = "Compare the note and screenshot I sent.",
                            formedAt =
                                Instant.parse(
                                    "2026-11-21T10:00:00Z",
                                ),
                        )
                    }

            store.bindRequest(
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

            val projection =
                assertNotNull(
                    VexLifeHistoricalRequestResources.inspect(
                        store = store,
                        request = request,
                    ),
                )

            assertEquals(
                request.eventRef,
                projection.requestEventRef,
            )
            assertEquals(
                request.eventHash,
                projection.requestEventHash,
            )
            assertEquals(
                listOf(
                    "notes.txt",
                    "screenshot.png",
                ),
                projection.resources.map {
                    it.displayName
                },
            )

            val text =
                projection.resources[0]
            assertEquals(
                VexLifeHistoricalRequestResources.Representation.PARSED_TEXT,
                text.representation,
            )
            assertEquals(
                VexLifeHistoricalRequestResources.CustodyObservation.EXACT,
                text.originalCustody,
            )
            assertEquals(
                VexLifeHistoricalRequestResources.CustodyObservation.EXACT,
                text.projectionCustody,
            )
            assertEquals(
                11L,
                text.readableBytes,
            )
            assertEquals(
                2,
                text.lineCount,
            )

            val image =
                projection.resources[1]
            assertEquals(
                VexLifeHistoricalRequestResources.Representation.STATIC_IMAGE,
                image.representation,
            )
            assertEquals(
                VexLifeHistoricalRequestResources.CustodyObservation.EXACT,
                image.originalCustody,
            )
            assertEquals(
                VexLifeHistoricalRequestResources.CustodyObservation.EXACT,
                image.projectionCustody,
            )
            assertEquals(
                "image/png",
                image.mediaType,
            )
            assertEquals(
                1170,
                image.widthPixels,
            )
            assertEquals(
                2532,
                image.heightPixels,
            )
            assertEquals(
                "Compare the note and screenshot I sent.",
                request.content,
            )
            assertFalse(
                request.content.contains(
                    "notes.txt",
                ),
                "historical resource projection must not rewrite canonical human words",
            )
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun distinguishesMissingFromInvalidCustodyWithoutInventingUserDeletion() {
        val parent =
            Files.createTempDirectory(
                "vexlife-historical-resource-integrity-",
            )
                .toAbsolutePath()
        val home =
            parent.resolve(
                "VexLife-Prod",
            )

        try {
            CanonicalVexLifeHomeFixture.form(
                home = home,
                deviceSuffix = "historical-resource-integrity",
                lineageSuffix = "historical-resource-integrity",
            )
            val store =
                VexLifeLocalArtifactStore(
                    home,
                )
            val originalBytes =
                byteArrayOf(
                    9,
                    8,
                    7,
                    6,
                )
            val original =
                store.storeOriginal(
                    originalBytes,
                )
            val imageProjection =
                store.storeStaticImageProjection(
                    artifactRef = original.artifactRef,
                    adapterRef = "adapter.vexlife-prod.static-image.png.v1",
                    mediaType = "image/png",
                    widthPixels = 640,
                    heightPixels = 480,
                )
            val request =
                DirectConversationStore(
                    home,
                )
                    .withWriterLease {
                        beginRequest(
                            turnRef = "turn.historical-resource-integrity.001",
                            requestMessageRef =
                                "message.historical-resource-integrity.human.001",
                            content = "Keep the historical relationship truthful.",
                        )
                    }
            val binding =
                store.bindRequest(
                    request = request,
                    attachments =
                        listOf(
                            VexLifeLocalArtifact.RequestAttachment(
                                artifactRef = original.artifactRef,
                                projectionRef = imageProjection.projectionRef,
                                displayName = "integrity.png",
                            ),
                        ),
                )

            val rawPath =
                home.resolve(
                    original.custodyRelativePath,
                )
            Files.delete(
                rawPath,
            )

            val missing =
                assertNotNull(
                    VexLifeHistoricalRequestResources.inspect(
                        store = store,
                        request = request,
                    ),
                )
                    .resources
                    .single()

            assertEquals(
                VexLifeHistoricalRequestResources.CustodyObservation.MISSING,
                missing.originalCustody,
            )
            assertEquals(
                VexLifeHistoricalRequestResources.CustodyObservation.EXACT,
                missing.projectionCustody,
            )

            Files.write(
                rawPath,
                byteArrayOf(
                    1,
                    2,
                    3,
                    4,
                ),
            )

            val invalidOriginal =
                assertNotNull(
                    VexLifeHistoricalRequestResources.inspect(
                        store = store,
                        request = request,
                    ),
                )
                    .resources
                    .single()

            assertEquals(
                VexLifeHistoricalRequestResources.CustodyObservation.INVALID,
                invalidOriginal.originalCustody,
                "same-size wrong-hash bytes must not be reported as merely missing",
            )
            assertEquals(
                VexLifeHistoricalRequestResources.CustodyObservation.EXACT,
                invalidOriginal.projectionCustody,
            )

            assertFailsWith<IllegalArgumentException> {
                store.bindingFor(
                    request.eventHash,
                )
            }

            val projectionIdentity =
                assertNotNull(
                    VexLifeLocalArtifact.projectionIdentitySha256FromRef(
                        imageProjection.projectionRef,
                    ),
                )
            val projectionPath =
                home.resolve(
                    VexLifeLocalArtifact.projectionRelativePath(
                        artifactSha256 = original.sha256,
                        projectionIdentitySha256 = projectionIdentity,
                    ),
                )
            Files.writeString(
                projectionPath,
                Files
                    .readString(
                        projectionPath,
                        StandardCharsets.UTF_8,
                    )
                    .replace(
                        "\"image/png\"",
                        "\"image/jpeg\"",
                    ),
                StandardCharsets.UTF_8,
            )

            val invalidProjection =
                assertNotNull(
                    VexLifeHistoricalRequestResources.inspect(
                        store = store,
                        request = request,
                    ),
                )
                    .resources
                    .single()

            assertEquals(
                VexLifeHistoricalRequestResources.CustodyObservation.INVALID,
                invalidProjection.originalCustody,
            )
            assertEquals(
                VexLifeHistoricalRequestResources.CustodyObservation.INVALID,
                invalidProjection.projectionCustody,
                "identity-mismatched projection metadata must not be reported as missing",
            )
            assertEquals(
                VexLifeHistoricalRequestResources.Representation.UNKNOWN,
                invalidProjection.representation,
            )
            assertEquals(
                binding,
                store.historicalBindingFor(
                    request.eventHash,
                ),
                "integrity failure must not erase the immutable sent-resource relationship",
            )
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun keepsImageRelationshipAndMetadataAfterOriginalBytesDisappearThenDegradesTruthfully() {
        val parent =
            Files.createTempDirectory(
                "vexlife-historical-image-custody-",
            )
                .toAbsolutePath()
        val home =
            parent.resolve(
                "VexLife-Prod",
            )

        try {
            CanonicalVexLifeHomeFixture.form(
                home = home,
                deviceSuffix = "historical-image-custody",
                lineageSuffix = "historical-image-custody",
            )
            val store =
                VexLifeLocalArtifactStore(
                    home,
                )
            val original =
                store.storeOriginal(
                    byteArrayOf(
                        8,
                        6,
                        7,
                        5,
                        3,
                        0,
                        9,
                    ),
                )
            val imageProjection =
                store.storeStaticImageProjection(
                    artifactRef = original.artifactRef,
                    adapterRef = "adapter.vexlife-prod.static-image.jpeg.v1",
                    mediaType = "image/jpeg",
                    widthPixels = 2048,
                    heightPixels = 1536,
                )
            val request =
                DirectConversationStore(
                    home,
                )
                    .withWriterLease {
                        beginRequest(
                            turnRef = "turn.historical-image-custody.001",
                            requestMessageRef =
                                "message.historical-image-custody.human.001",
                            content = "Remember which image I sent here.",
                        )
                    }
            val binding =
                store.bindRequest(
                    request = request,
                    attachments =
                        listOf(
                            VexLifeLocalArtifact.RequestAttachment(
                                artifactRef = original.artifactRef,
                                projectionRef = imageProjection.projectionRef,
                                displayName = "memory-photo.jpg",
                            ),
                        ),
                )

            Files.delete(
                home.resolve(
                    original.custodyRelativePath,
                ),
            )

            val withoutOriginal =
                assertNotNull(
                    VexLifeHistoricalRequestResources.inspect(
                        store = store,
                        request = request,
                    ),
                )
                    .resources
                    .single()

            assertEquals(
                VexLifeHistoricalRequestResources.Representation.STATIC_IMAGE,
                withoutOriginal.representation,
            )
            assertEquals(
                VexLifeHistoricalRequestResources.CustodyObservation.MISSING,
                withoutOriginal.originalCustody,
            )
            assertEquals(
                VexLifeHistoricalRequestResources.CustodyObservation.EXACT,
                withoutOriginal.projectionCustody,
            )
            assertEquals(
                "image/jpeg",
                withoutOriginal.mediaType,
            )
            assertEquals(
                2048,
                withoutOriginal.widthPixels,
            )
            assertEquals(
                1536,
                withoutOriginal.heightPixels,
            )
            assertEquals(
                binding,
                store.historicalBindingFor(
                    request.eventHash,
                ),
            )

            val projectionIdentity =
                assertNotNull(
                    VexLifeLocalArtifact.projectionIdentitySha256FromRef(
                        imageProjection.projectionRef,
                    ),
                )
            Files.delete(
                home.resolve(
                    VexLifeLocalArtifact.projectionRelativePath(
                        artifactSha256 = original.sha256,
                        projectionIdentitySha256 = projectionIdentity,
                    ),
                ),
            )

            val withoutProjection =
                assertNotNull(
                    VexLifeHistoricalRequestResources.inspect(
                        store = store,
                        request = request,
                    ),
                )
                    .resources
                    .single()

            assertEquals(
                "memory-photo.jpg",
                withoutProjection.displayName,
            )
            assertEquals(
                original.artifactRef,
                withoutProjection.artifactRef,
            )
            assertEquals(
                imageProjection.projectionRef,
                withoutProjection.projectionRef,
            )
            assertEquals(
                VexLifeHistoricalRequestResources.Representation.UNKNOWN,
                withoutProjection.representation,
            )
            assertEquals(
                VexLifeHistoricalRequestResources.CustodyObservation.MISSING,
                withoutProjection.originalCustody,
            )
            assertEquals(
                VexLifeHistoricalRequestResources.CustodyObservation.MISSING,
                withoutProjection.projectionCustody,
            )
            assertEquals(
                binding,
                store.historicalBindingFor(
                    request.eventHash,
                ),
                "resource-detail loss must not erase the immutable sent-resource relationship",
            )
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }
}

// [VXG RealForever]
