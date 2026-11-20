package vexlife.artifact

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vexlife.conversation.DirectConversationStore
import vexlife.home.CanonicalVexLifeHomeFixture
import vexlife.capability.reach.VexLifeAttachmentReach

class VexLifeRequestArtifactManifestTest {
    @Test
    fun manifestNamesBoundResourcesWithoutCarryingTheirText() {
        val parent =
            Files.createTempDirectory(
                "vexlife-request-artifact-manifest-",
            )
                .toAbsolutePath()
        val home =
            parent.resolve(
                "VexLife-Prod",
            )

        try {
            CanonicalVexLifeHomeFixture.form(
                home = home,
                deviceSuffix = "request-artifact-manifest",
                lineageSuffix = "request-artifact-manifest",
            )
            val store =
                VexLifeLocalArtifactStore(
                    home,
                )
            val sentinel =
                "LOCAL-ARTIFACT-CONTENT-MUST-NOT-BE-PROMPT-RESIDENT"
            val original =
                store.storeOriginal(
                    (
                        sentinel +
                            "\nsecond line\n"
                    )
                        .toByteArray(
                            StandardCharsets.UTF_8,
                        ),
                )
            val projection =
                store.storeTextProjection(
                    artifactRef = original.artifactRef,
                    converterRef =
                        "converter.vexlife-prod.plain-text.v1",
                    text =
                        sentinel +
                            "\nsecond line\n",
                )
            val request =
                DirectConversationStore(
                    home,
                )
                    .withWriterLease {
                        beginRequest(
                            turnRef =
                                "turn.request-artifact-manifest.001",
                            requestMessageRef =
                                "message.request-artifact-manifest.human.001",
                            content =
                                "Use my local note.",
                        )
                    }

            assertNull(
                VexLifeRequestArtifactManifest.project(
                    store = store,
                    request = request,
                ),
                "staged custody alone must not become prompt discovery",
            )

            store.bindRequest(
                request = request,
                attachments =
                    listOf(
                        VexLifeLocalArtifact.RequestAttachment(
                            artifactRef =
                                original.artifactRef,
                            projectionRef =
                                projection.projectionRef,
                            displayName =
                                "evidence.txt",
                        ),
                    ),
            )

            val manifest =
                requireNotNull(
                    VexLifeRequestArtifactManifest.project(
                        store = store,
                        request = request,
                    ),
                )
            assertEquals(
                request.eventRef,
                manifest.requestEventRef,
            )
            assertEquals(
                1,
                manifest.attachmentCount,
            )
            assertTrue(
                manifest.systemContent.contains(
                    "evidence.txt",
                ),
            )
            assertTrue(
                manifest.systemContent.contains(
                    "converter.vexlife-prod.plain-text.v1",
                ),
            )
            val resourceRef =
                VexLifeAttachmentReach.resourceRefFor(
                    requestEventHash = request.eventHash,
                    ordinal = 1,
                )
            assertTrue(
                manifest.systemContent.contains(
                    "resourceRef=$resourceRef",
                ),
            )
            assertTrue(
                manifest.systemContent.contains(
                    "use its read action with that resourceRef",
                ),
            )
            assertTrue(
                manifest.systemContent.contains(
                    "Conversation Source Return searches older canonical Conversation only",
                ),
            )
            assertFalse(
                manifest.systemContent.contains(
                    "attachments.list",
                ),
                "artifact discovery metadata must not teach one provider wire spelling",
            )
            assertFalse(
                manifest.systemContent.contains(
                    "attachments.read",
                ),
                "artifact discovery metadata must stay provider-neutral",
            )
            assertTrue(
                manifest.systemContent.contains(
                    "file contents are not in this prompt",
                ),
            )
            assertFalse(
                manifest.systemContent.contains(
                    sentinel,
                ),
            )
            assertTrue(
                manifest.sourceRefs.contains(
                    request.eventRef,
                ),
            )
            assertTrue(
                manifest.sourceRefs.contains(
                    original.artifactRef,
                ),
            )
            assertTrue(
                manifest.sourceRefs.contains(
                    projection.projectionRef,
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
