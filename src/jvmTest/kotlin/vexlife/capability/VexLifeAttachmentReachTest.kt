package vexlife.capability.reach

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vexlife.artifact.VexLifeLocalArtifact
import vexlife.artifact.VexLifeLocalArtifactStore
import vexlife.conversation.DirectConversationStore
import vexlife.home.CanonicalVexLifeHomeFixture

class VexLifeAttachmentReachTest {
    @Test
    fun stagedButUnsentArtifactHasNoAttachmentReachRoute() {
        val parent =
            Files.createTempDirectory(
                "vexlife-attachment-reach-unsent-",
            )
                .toAbsolutePath()
        val home =
            parent.resolve(
                "VexLife-Prod",
            )

        try {
            CanonicalVexLifeHomeFixture.form(
                home = home,
                deviceSuffix = "reach-unsent",
                lineageSuffix = "reach-unsent",
            )
            val store =
                VexLifeLocalArtifactStore(
                    home,
                )
            val original =
                store.storeOriginal(
                    "staged only"
                        .toByteArray(
                            StandardCharsets.UTF_8,
                        ),
                )
            store.storeTextProjection(
                artifactRef = original.artifactRef,
                converterRef = "converter.vexlife-prod.plain-text.v1",
                text = "staged only",
            )
            val request =
                DirectConversationStore(
                    home,
                )
                    .withWriterLease {
                        beginRequest(
                            turnRef = "turn.attachment-reach.unsent.001",
                            requestMessageRef = "message.attachment-reach.human.unsent.001",
                            content = "Send without the staged file.",
                        )
                    }

            val outcome =
                VexLifeAttachmentReach.list(
                    productHome = home,
                    requestEventHash = request.eventHash,
                )

            assertIs<VexLifeAttachmentReach.Outcome.NotFound>(
                outcome,
            )
            assertNull(
                store.bindingFor(
                    request.eventHash,
                ),
            )
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun oneRequestListsOccurrenceResourcesEvenWhenTwoNamesShareTheSameBytes() {
        val parent =
            Files.createTempDirectory(
                "vexlife-attachment-reach-occurrence-",
            )
                .toAbsolutePath()
        val home =
            parent.resolve(
                "VexLife-Prod",
            )

        try {
            CanonicalVexLifeHomeFixture.form(
                home = home,
                deviceSuffix = "reach-occurrence",
                lineageSuffix = "reach-occurrence",
            )
            val store =
                VexLifeLocalArtifactStore(
                    home,
                )
            val original =
                store.storeOriginal(
                    "one\ntwo\nthree\nfour\n"
                        .toByteArray(
                            StandardCharsets.UTF_8,
                        ),
                )
            val projection =
                store.storeTextProjection(
                    artifactRef = original.artifactRef,
                    converterRef = "converter.vexlife-prod.plain-text.v1",
                    text = "one\ntwo\nthree\nfour\n",
                )
            val request =
                DirectConversationStore(
                    home,
                )
                    .withWriterLease {
                        beginRequest(
                            turnRef = "turn.attachment-reach.occurrence.001",
                            requestMessageRef = "message.attachment-reach.human.occurrence.001",
                            content = "Compare the two attached names.",
                        )
                    }
            store.bindRequest(
                request = request,
                attachments =
                    listOf(
                        VexLifeLocalArtifact.RequestAttachment(
                            artifactRef = original.artifactRef,
                            projectionRef = projection.projectionRef,
                            displayName = "first.txt",
                        ),
                        VexLifeLocalArtifact.RequestAttachment(
                            artifactRef = original.artifactRef,
                            projectionRef = projection.projectionRef,
                            displayName = "second.md",
                        ),
                    ),
            )

            val listed =
                assertIs<VexLifeAttachmentReach.Outcome.Listed>(
                    VexLifeAttachmentReach.list(
                        productHome = home,
                        requestEventHash = request.eventHash,
                    ),
                )

            assertEquals(
                listOf(
                    "first.txt",
                    "second.md",
                ),
                listed.resources.map {
                    it.displayName
                },
            )
            assertEquals(
                2,
                listed.resources.size,
            )
            assertNotEquals(
                listed.resources[0].resourceRef,
                listed.resources[1].resourceRef,
                "request-local occurrence identity must not collapse equal bytes/names",
            )
            assertTrue(
                listed.resources.all {
                    it.artifactRef ==
                        original.artifactRef &&
                        it.projectionRef ==
                        projection.projectionRef
                },
            )
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun readIsBoundToTheExactRequestResourceAndReturnsOneBoundedLineWindow() {
        val parent =
            Files.createTempDirectory(
                "vexlife-attachment-reach-lines-",
            )
                .toAbsolutePath()
        val home =
            parent.resolve(
                "VexLife-Prod",
            )

        try {
            CanonicalVexLifeHomeFixture.form(
                home = home,
                deviceSuffix = "reach-lines",
                lineageSuffix = "reach-lines",
            )
            val store =
                VexLifeLocalArtifactStore(
                    home,
                )
            val original =
                store.storeOriginal(
                    "one\ntwo\nthree\nfour\n"
                        .toByteArray(
                            StandardCharsets.UTF_8,
                        ),
                )
            val projection =
                store.storeTextProjection(
                    artifactRef = original.artifactRef,
                    converterRef = "converter.vexlife-prod.plain-text.v1",
                    text = "one\ntwo\nthree\nfour\n",
                )
            val request =
                DirectConversationStore(
                    home,
                )
                    .withWriterLease {
                        beginRequest(
                            turnRef = "turn.attachment-reach.lines.001",
                            requestMessageRef = "message.attachment-reach.human.lines.001",
                            content = "Read the middle lines.",
                        )
                    }
            store.bindRequest(
                request = request,
                attachments =
                    listOf(
                        VexLifeLocalArtifact.RequestAttachment(
                            artifactRef = original.artifactRef,
                            projectionRef = projection.projectionRef,
                            displayName = "lines.txt",
                        ),
                    ),
            )
            val resource =
                assertIs<VexLifeAttachmentReach.Outcome.Listed>(
                    VexLifeAttachmentReach.list(
                        productHome = home,
                        requestEventHash = request.eventHash,
                    ),
                )
                    .resources
                    .single()

            val read =
                assertIs<VexLifeAttachmentReach.Outcome.Lines>(
                    VexLifeAttachmentReach.read(
                        productHome = home,
                        requestEventHash = request.eventHash,
                        resourceRef = resource.resourceRef,
                        startLine = 2,
                        maxLines = 2,
                    ),
                )

            assertEquals(
                "two\nthree",
                read.text,
            )
            assertEquals(
                2,
                read.startLine,
            )
            assertEquals(
                3,
                read.endLine,
            )
            assertEquals(
                4,
                read.nextLine,
            )
            assertEquals(
                "lines.txt",
                read.descriptor.displayName,
            )

            assertIs<VexLifeAttachmentReach.Outcome.Refused>(
                VexLifeAttachmentReach.read(
                    productHome = home,
                    requestEventHash = "b".repeat(64),
                    resourceRef = resource.resourceRef,
                    startLine = 1,
                    maxLines = 1,
                ),
                "a resource occurrence from one request must not be readable under another request hash",
            )
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun oneOversizedLineIsRefusedRatherThanPartiallyReturned() {
        val parent =
            Files.createTempDirectory(
                "vexlife-attachment-reach-line-bound-",
            )
                .toAbsolutePath()
        val home =
            parent.resolve(
                "VexLife-Prod",
            )

        try {
            CanonicalVexLifeHomeFixture.form(
                home = home,
                deviceSuffix = "reach-line-bound",
                lineageSuffix = "reach-line-bound",
            )
            val store =
                VexLifeLocalArtifactStore(
                    home,
                )
            val text =
                "x".repeat(
                    VexLifeAttachmentReach.MaximumReturnBytes +
                        1,
                )
            val original =
                store.storeOriginal(
                    text.toByteArray(
                        StandardCharsets.UTF_8,
                    ),
                )
            val projection =
                store.storeTextProjection(
                    artifactRef = original.artifactRef,
                    converterRef = "converter.vexlife-prod.plain-text.v1",
                    text = text,
                )
            val request =
                DirectConversationStore(
                    home,
                )
                    .withWriterLease {
                        beginRequest(
                            turnRef = "turn.attachment-reach.line-bound.001",
                            requestMessageRef = "message.attachment-reach.human.line-bound.001",
                            content = "Read this file.",
                        )
                    }
            store.bindRequest(
                request = request,
                attachments =
                    listOf(
                        VexLifeLocalArtifact.RequestAttachment(
                            artifactRef = original.artifactRef,
                            projectionRef = projection.projectionRef,
                            displayName = "one-long-line.txt",
                        ),
                    ),
            )
            val resource =
                assertIs<VexLifeAttachmentReach.Outcome.Listed>(
                    VexLifeAttachmentReach.list(
                        productHome = home,
                        requestEventHash = request.eventHash,
                    ),
                )
                    .resources
                    .single()

            val refused =
                assertIs<VexLifeAttachmentReach.Outcome.Refused>(
                    VexLifeAttachmentReach.read(
                        productHome = home,
                        requestEventHash = request.eventHash,
                        resourceRef = resource.resourceRef,
                        startLine = 1,
                        maxLines = 1,
                    ),
                )

            assertTrue(
                refused.reason.contains(
                    "no partial line",
                ),
            )
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }
}

// [VXG RealForever]
