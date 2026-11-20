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
import vexlife.companion.DirectPromptContext
import vexlife.model.VexLifeVisualInputQualification
import vexlife.model.heretic.HereticModelCatalog

class VexLifeRequestArtifactManifestTest {
    private fun project(
        store: VexLifeLocalArtifactStore,
        request: DirectConversationStore.Event,
        maximumInlineContentTokens: Int =
            8_000,
    ) =
        VexLifeRequestArtifactManifest.project(
            store = store,
            request = request,
            maximumInlineContentTokens = maximumInlineContentTokens,
            estimateTokens = DirectPromptContext::estimatedMessageTokens,
        )

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
                project(
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
                    project(
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
                    "participation=DIRECT",
                ),
            )
            assertFalse(
                manifest.systemContent.contains(
                    sentinel,
                ),
                "routing metadata stays separate from file content",
            )
            val inlineContent =
                requireNotNull(
                    manifest.inlineContent,
                )
            assertTrue(
                inlineContent.startsWith(
                    VexLifeRequestArtifactManifest.ContentHeader,
                ),
            )
            assertTrue(
                inlineContent.contains(
                    sentinel,
                ),
                "small readable attachment text should enter the current turn directly",
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

    @Test
    fun oversizedReadableProjectionStaysOutOfInlineContextAndKeepsItsResourceRef() {
        val parent =
            Files.createTempDirectory(
                "vexlife-request-artifact-manifest-large-",
            )
                .toAbsolutePath()
        val home =
            parent.resolve(
                "VexLife-Prod",
            )

        try {
            CanonicalVexLifeHomeFixture.form(
                home = home,
                deviceSuffix = "request-artifact-manifest-large",
                lineageSuffix = "request-artifact-manifest-large",
            )
            val store =
                VexLifeLocalArtifactStore(
                    home,
                )
            val text =
                buildString {
                    repeat(
                        12_000,
                    ) {
                        append(
                            'x',
                        )
                    }
                    append(
                        '\n',
                    )
                }
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
                            turnRef = "turn.request-artifact-manifest.large.001",
                            requestMessageRef = "message.request-artifact-manifest.large.human.001",
                            content = "Use the large file.",
                        )
                    }
            store.bindRequest(
                request = request,
                attachments =
                    listOf(
                        VexLifeLocalArtifact.RequestAttachment(
                            artifactRef = original.artifactRef,
                            projectionRef = projection.projectionRef,
                            displayName = "large.txt",
                        ),
                    ),
            )

            val manifest =
                requireNotNull(
                    project(
                        store = store,
                        request = request,
                        maximumInlineContentTokens = 256,
                    ),
                )
            assertTrue(
                manifest.systemContent.contains(
                    "participation=ON_DEMAND",
                ),
            )
            assertNull(
                manifest.inlineContent,
                "large file text should stay behind bounded attachment reach",
            )
            assertTrue(
                manifest.systemContent.contains(
                    "resourceRef=request-resource.vexlife-prod.",
                ),
            )
            assertEquals(
                VexLifeRequestArtifactManifest.Participation.ON_DEMAND,
                manifest.resources.single().participation,
            )
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }
    @Test
    fun qualifiedStaticImageInterpretationUsesTheSameDirectAndOnDemandTokenAdmission() {
        val parent =
            Files.createTempDirectory(
                "vexlife-request-artifact-visual-",
            )
                .toAbsolutePath()
        val home =
            parent.resolve(
                "VexLife-Prod",
            )
        try {
            CanonicalVexLifeHomeFixture.form(
                home = home,
                deviceSuffix = "request-artifact-visual",
                lineageSuffix = "request-artifact-visual",
            )
            val store =
                VexLifeLocalArtifactStore(
                    home,
                )
            val original =
                store.storeOriginal(
                    byteArrayOf(
                        9,
                        8,
                        7,
                    ),
                )
            val projection =
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
                            turnRef = "turn.request-artifact.visual.001",
                            requestMessageRef = "message.request-artifact.visual.001",
                            content = "Describe this image.",
                        )
                    }
            val attachment =
                VexLifeLocalArtifact.RequestAttachment(
                    artifactRef = original.artifactRef,
                    projectionRef = projection.projectionRef,
                    displayName = "scene.png",
                )
            store.bindRequest(
                request = request,
                attachments = listOf(attachment),
            )
            val interpretation =
                store.storeVisualInterpretation(
                    request = request,
                    attachment = attachment,
                    evidence =
                        VexLifeVisualInputQualification.QualifiedRuntimeEvidence(
                            evidenceRef = "evidence.vexlife-prod.visual.manifest-test.001",
                            modelRef = HereticModelCatalog.Models.Qwen4B.modelRef,
                            artifactRef =
                                HereticModelCatalog.Models.Qwen4B.identity.artifactSetRef,
                            artifactContentSetSha256 =
                                HereticModelCatalog.Models.Qwen4B.identity.artifactContentSetSha256,
                            environmentFingerprintSha256 =
                                "b".repeat(
                                    64,
                                ),
                        ),
                    text = "A blue circle centered on a white background.",
                )

            val direct =
                requireNotNull(
                    project(
                        store = store,
                        request = request,
                        maximumInlineContentTokens = 8_000,
                    ),
                )
            assertTrue(
                direct.systemContent.contains(
                    "image/png 640x480",
                ),
            )
            assertTrue(
                direct.systemContent.contains(
                    "participation=DIRECT",
                ),
            )
            assertTrue(
                requireNotNull(
                    direct.inlineContent,
                )
                    .contains(
                        "A blue circle centered on a white background.",
                    ),
            )
            assertTrue(
                direct.sourceRefs.contains(
                    interpretation.interpretationRef,
                ),
            )

            val onDemand =
                requireNotNull(
                    project(
                        store = store,
                        request = request,
                        maximumInlineContentTokens = 1,
                    ),
                )
            assertTrue(
                onDemand.systemContent.contains(
                    "participation=ON_DEMAND",
                ),
            )
            assertNull(
                onDemand.inlineContent,
            )
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }

}

// [VXG RealForever]
