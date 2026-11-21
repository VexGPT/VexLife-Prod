package vexlife.service

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import vexlife.artifact.VexLifeLocalArtifact
import vexlife.artifact.VexLifeLocalArtifactStore
import vexlife.conversation.CanonicalLivedContextFixture
import vexlife.conversation.DirectConversationStore
import vexlife.home.CanonicalVexLifeHomeFixture

class VexLifeConversationServiceProjectionTest {
    @Test
    fun canonicalHumanWordsAndSentResourceRelationshipSurviveLaterCustodyDamage() {
        val parent =
            Files.createTempDirectory(
                "vexlife-conversation-resource-projection-",
            )
                .toAbsolutePath()
        val home =
            parent.resolve(
                "VexLife-Prod",
            )

        try {
            CanonicalVexLifeHomeFixture.form(
                home = home,
                deviceSuffix = "conversation-resource-projection",
                lineageSuffix = "conversation-resource-projection",
            )
            val artifacts =
                VexLifeLocalArtifactStore(
                    home,
                )
            val original =
                artifacts.storeOriginal(
                    byteArrayOf(
                        1,
                        3,
                        5,
                        7,
                    ),
                )
            val imageProjection =
                artifacts.storeStaticImageProjection(
                    artifactRef = original.artifactRef,
                    adapterRef = "adapter.vexlife-prod.static-image.png.v1",
                    mediaType = "image/png",
                    widthPixels = 800,
                    heightPixels = 600,
                )

            lateinit var request: DirectConversationStore.Event
            DirectConversationStore(
                home,
            )
                .withWriterLease {
                    request =
                        beginRequest(
                            turnRef = "turn.conversation-resource-projection.001",
                            requestMessageRef =
                                "message.conversation-resource-projection.human.001",
                            content = "What do you see in this screenshot?",
                            formedAt =
                                Instant.parse(
                                    "2026-11-21T11:00:00Z",
                                ),
                        )
                    artifacts.bindRequest(
                        request = request,
                        attachments =
                            listOf(
                                VexLifeLocalArtifact.RequestAttachment(
                                    artifactRef = original.artifactRef,
                                    projectionRef = imageProjection.projectionRef,
                                    displayName = "screen.png",
                                ),
                            ),
                    )
                    val response =
                        appendResponse(
                            request = request,
                            responseMessageRef =
                                "message.conversation-resource-projection.vex.001",
                            content = "I can see the screenshot.",
                            model = "default_model",
                            endpointOrigin = "http://127.0.0.1:8765",
                            runtimeObservationRef =
                                "observation.vexlife-prod.model-runtime.conversation-resource-projection.001",
                            invocationEvidenceRef =
                                "evidence.vexlife-prod.model-runtime-invocation.conversation-resource-projection.001",
                            formedAt =
                                Instant.parse(
                                    "2026-11-21T11:00:01Z",
                                ),
                        )
                    val context =
                        CanonicalLivedContextFixture.write(
                            productHome = home,
                            request = request,
                            response = response,
                            formedAt =
                                Instant.parse(
                                    "2026-11-21T11:00:02Z",
                                ),
                        )
                    commitCompletedTurn(
                        request = request,
                        response = response,
                        contextSha256 = context.sha256,
                        formedAt =
                            Instant.parse(
                                "2026-11-21T11:00:03Z",
                            ),
                    )
                }

            Files.delete(
                home.resolve(
                    original.custodyRelativePath,
                ),
            )
            val projectionIdentity =
                requireNotNull(
                    VexLifeLocalArtifact.projectionIdentitySha256FromRef(
                        imageProjection.projectionRef,
                    ),
                )
            Files.writeString(
                home.resolve(
                    VexLifeLocalArtifact.projectionRelativePath(
                        artifactSha256 = original.sha256,
                        projectionIdentitySha256 = projectionIdentity,
                    ),
                ),
                "{\"schemaVersion\":\"corrupt\"}\n",
                StandardCharsets.UTF_8,
            )

            val projection =
                VexLifeConversationServiceProjection.observe(
                    home,
                )

            assertEquals(
                VexLifeProdServiceContract.ConversationProjectionSchema,
                projection.schemaVersion,
            )
            assertEquals(
                listOf(
                    "What do you see in this screenshot?",
                    "I can see the screenshot.",
                ),
                projection.messages.map {
                    it.content
                },
                "historical resource projection must never rewrite canonical Conversation words",
            )
            assertEquals(
                listOf(
                    "human",
                    "vex",
                ),
                projection.messages.map {
                    it.role
                },
            )
            val human =
                projection.messages.first()
            assertEquals(
                request.eventHash,
                human.eventSha256,
            )
            assertEquals(
                1,
                human.resources.size,
            )
            assertEquals(
                "screen.png",
                human.resources.single()
                    .displayName,
            )
            assertEquals(
                VexLifeLocalArtifact.requestResourceRef(
                    requestEventHash = request.eventHash,
                    ordinal = 1,
                ),
                human.resources.single()
                    .resourceRef,
            )
            assertTrue(
                projection.messages.last()
                    .resources
                    .isEmpty(),
                "Vex replies must not inherit the human REQUEST resource relationship",
            )
            assertEquals(
                0,
                projection.abandonedAttemptCount,
            )
            assertEquals(
                false,
                projection.effectsPerformed,
            )

            // Relationship corruption is different from later artifact-custody damage. Conversation
            // remains healthy, but this message's resource-history projection becomes unavailable
            // rather than pretending that no binding ever existed.
            Files.writeString(
                home.resolve(
                    VexLifeLocalArtifact.requestBindingRelativePath(
                        request.eventHash,
                    ),
                ),
                "{\"schemaVersion\":\"corrupt\"}\n",
                StandardCharsets.UTF_8,
            )

            val unavailable =
                VexLifeConversationServiceProjection.observe(
                    home,
                )

            assertEquals(
                listOf(
                    "What do you see in this screenshot?",
                    "I can see the screenshot.",
                ),
                unavailable.messages.map {
                    it.content
                },
                "unreadable resource history must not erase canonical Conversation",
            )
            assertTrue(
                unavailable.messages.first()
                    .resources
                    .isEmpty(),
                "unreadable resource history must not invent resource descriptors",
            )
            assertEquals(
                VexLifeProdServiceContract.ConversationResourceHistoryState.UNAVAILABLE,
                unavailable.messages.first()
                    .resourceHistoryState,
            )
            assertEquals(
                VexLifeProdServiceContract.ConversationResourceHistoryState.AVAILABLE,
                unavailable.messages.last()
                    .resourceHistoryState,
            )
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }
}

// [VXG RealForever]
