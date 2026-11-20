package vexlife.capability

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import vexlife.artifact.VexLifeLocalArtifact
import vexlife.artifact.VexLifeLocalArtifactStore
import vexlife.conversation.DirectConversationStore
import vexlife.home.CanonicalVexLifeHomeFixture

class VexLifeAttachmentCapabilityBridgeTest {
    private val tokens: (String) -> Int = {
        (it.toByteArray(StandardCharsets.UTF_8).size + 3) / 4 + 4
    }

    private fun request(
        json: String,
    ): VexLifeCapabilityRequest.Parsed =
        assertNotNull(
            VexLifeCapabilityRequest.parseOrNull(
                VexLifeCapabilityRequest.RequestHeader +
                    "\n" +
                    json,
            ),
        )

    @Test
    fun attachmentsListAndReadAreImplicitlyBoundToTheCurrentDurableRequest() {
        val parent =
            Files.createTempDirectory(
                "vexlife-attachment-capability-",
            )
                .toAbsolutePath()
        val home =
            parent.resolve(
                "VexLife-Prod",
            )

        try {
            CanonicalVexLifeHomeFixture.form(
                home = home,
                deviceSuffix = "attachment-capability",
                lineageSuffix = "attachment-capability",
            )
            val store =
                VexLifeLocalArtifactStore(
                    home,
                )
            val original =
                store.storeOriginal(
                    "alpha\nbeta\ngamma\n"
                        .toByteArray(
                            StandardCharsets.UTF_8,
                        ),
                )
            val projection =
                store.storeTextProjection(
                    artifactRef = original.artifactRef,
                    converterRef = "converter.vexlife-prod.plain-text.v1",
                    text = "alpha\nbeta\ngamma\n",
                )
            val currentRequest =
                DirectConversationStore(
                    home,
                )
                    .withWriterLease {
                        beginRequest(
                            turnRef = "turn.attachment-capability.001",
                            requestMessageRef = "message.attachment-capability.human.001",
                            content = "Read my attached note.",
                        )
                    }
            store.bindRequest(
                request = currentRequest,
                attachments =
                    listOf(
                        VexLifeLocalArtifact.RequestAttachment(
                            artifactRef = original.artifactRef,
                            projectionRef = projection.projectionRef,
                            displayName = "note.txt",
                        ),
                    ),
            )

            fun admission(
                requestEventHash: String?,
                number: Int,
            ) =
                VexLifeCapabilityBridge.Admission(
                    productHome = home,
                    availableCapabilityRefs =
                        setOf(
                            VexLifeReachDefinitions.AttachmentRef,
                        ),
                    requestNumber = number,
                    tokensAvailable = 20_000,
                    requestEventHash = requestEventHash,
                )

            val listed =
                VexLifeCapabilityBridge.perform(
                    parsed =
                        request(
                            """{"capability":"attachments","action":"list","arguments":{}}""",
                        ),
                    admission =
                        admission(
                            requestEventHash = currentRequest.eventHash,
                            number = 1,
                        ),
                    estimateTokens = tokens,
                )

            assertEquals(
                VexLifeCapabilityBridge.ResultState.RETURNED,
                listed.entry.state,
            )
            assertEquals(
                "attachments",
                listed.entry.handle,
            )
            assertEquals(
                "attachments:current-message",
                listed.use.target,
            )
            assertTrue(
                listed.resultMessage.content.contains(
                    "note.txt",
                ),
            )
            assertTrue(
                !listed.entry.untrustedContent,
                "attachment metadata is Product-authored projection",
            )
            assertTrue(
                listed.entry.sourceRefs.contains(
                    original.artifactRef,
                ),
            )

            val resourceRef =
                assertNotNull(
                    Regex(
                        "request-resource\\.vexlife-prod\\.[0-9a-f]{64}\\.1",
                    )
                        .find(
                            listed.resultMessage.content,
                        )
                        ?.value,
                )

            val read =
                VexLifeCapabilityBridge.perform(
                    parsed =
                        request(
                            """{"capability":"attachments","action":"read","arguments":{"resourceRef":"$resourceRef","startLine":2,"maxLines":1}}""",
                        ),
                    admission =
                        admission(
                            requestEventHash = currentRequest.eventHash,
                            number = 2,
                        ),
                    estimateTokens = tokens,
                )

            assertEquals(
                VexLifeCapabilityBridge.ResultState.RETURNED,
                read.entry.state,
            )
            assertTrue(
                read.entry.untrustedContent,
                "human-supplied file text is material to read, never provider instruction",
            )
            assertTrue(
                read.resultMessage.content.contains(
                    "beta",
                ),
            )
            assertTrue(
                read.resultMessage.content.contains(
                    "\"startLine\":\"2\"",
                ),
            )

            val noTurnBinding =
                VexLifeCapabilityBridge.perform(
                    parsed =
                        request(
                            """{"capability":"attachments","action":"list","arguments":{}}""",
                        ),
                    admission =
                        admission(
                            requestEventHash = null,
                            number = 1,
                        ),
                    estimateTokens = tokens,
                )
            assertEquals(
                VexLifeCapabilityBridge.ResultState.NOT_AVAILABLE,
                noTurnBinding.entry.state,
            )

            val modelTriesToChooseAnotherRequest =
                VexLifeCapabilityBridge.perform(
                    parsed =
                        request(
                            """{"capability":"attachments","action":"list","arguments":{"requestEventHash":"ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff"}}""",
                        ),
                    admission =
                        admission(
                            requestEventHash = currentRequest.eventHash,
                            number = 3,
                        ),
                    estimateTokens = tokens,
                )
            assertEquals(
                VexLifeCapabilityBridge.ResultState.INVALID_REQUEST,
                modelTriesToChooseAnotherRequest.entry.state,
            )
            assertTrue(
                modelTriesToChooseAnotherRequest.resultMessage.content.contains(
                    "unknown field",
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
