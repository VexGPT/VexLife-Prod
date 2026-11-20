package vexlife.companion

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import vexlife.artifact.VexLifeLocalArtifact
import vexlife.artifact.VexLifeLocalArtifactStore
import vexlife.artifact.VexLifeRequestArtifactManifest
import vexlife.capability.reach.VexLifeAttachmentReach
import vexlife.home.CanonicalVexLifeHomeFixture
import vexlife.local.VexLifeProdLocalSetup
import vexlife.model.heretic.HereticModelConfiguration
import vexlife.model.heretic.HereticProductIdentity
import vexlife.model.heretic.HereticRuntimeQualification
import vexlife.model.heretic.HereticRuntimeState
import vexlife.conversation.DirectConversationStore

class DirectCompanionAttachmentTurnTest {
    private fun setup(
        home: Path,
    ) =
        VexLifeProdLocalSetup.Result(
            state = "LOCAL_HERETIC_FOUNDATION_READY",
            productHome = home.toString(),
            homeDisposition = "EXISTING_HOME_PRESERVED",
            modelSelectionDisposition = "BOUND",
            managedPythonDisposition = "REUSED_VERIFIED",
            artifactDisposition = "REUSED_VERIFIED",
            environmentDisposition = "REUSED_VERIFIED",
            runtimeDisposition = "REUSED_EXACT_OWNED_MLX_RUNTIME",
            modelBindingDisposition = "BOUND_CURRENT",
            modelConfigurationState =
                HereticModelConfiguration.State.BoundProductModel,
            qualifiedRuntimeEvidence =
                HereticModelConfiguration.QualifiedRuntimeEvidence(
                    runtimeReceiptRef = "receipt.vexlife-prod.heretic.attachment-turn",
                    runtimeState = HereticRuntimeState.Qualified,
                    modelRef = HereticProductIdentity.ModelRef,
                    modelProfileRef = HereticProductIdentity.ModelProfileRef,
                    artifactRef = HereticProductIdentity.ArtifactSetRef,
                    artifactContentSetSha256 =
                        HereticProductIdentity.ArtifactContentSetSha256,
                    runtimeAdapterRef = HereticProductIdentity.RuntimeAdapterRef,
                    runtimeClass = HereticProductIdentity.RuntimeClass,
                    endpoint = HereticProductIdentity.Endpoint,
                    requestModel = HereticProductIdentity.RequestModel,
                    qualification = HereticRuntimeQualification.qualified(),
                ),
            endpoint = HereticProductIdentity.Endpoint,
            requestModel = HereticProductIdentity.RequestModel,
        )

    private fun reply(
        content: String,
    ) =
        DirectCompanionConnection.HttpResult(
            status = 200,
            body =
                buildString {
                    append(
                        """{"model":"default_model","choices":[{"message":{"role":"assistant","content":""",
                    )
                    append(
                        Json.encodeToString(
                            kotlinx.serialization.json.JsonPrimitive.serializer(),
                            kotlinx.serialization.json.JsonPrimitive(
                                content,
                            ),
                        ),
                    )
                    append(
                        "}}]}",
                    )
                },
        )

    @Test
    fun requestWordsStayExactAndResourcesAreBoundBeforeTheProviderRuns() {
        val parent =
            Files.createTempDirectory(
                "vexlife-attachment-turn-",
            )
                .toAbsolutePath()
        val home =
            parent.resolve(
                "VexLife-Prod",
            )

        try {
            CanonicalVexLifeHomeFixture.form(
                home = home,
                deviceSuffix = "attachment-turn",
                lineageSuffix = "attachment-turn",
            )
            val store =
                VexLifeLocalArtifactStore(
                    home,
                )
            val original =
                store.storeOriginal(
                    "evidence line one\nevidence line two\n"
                        .toByteArray(
                            StandardCharsets.UTF_8,
                        ),
                )
            val projection =
                store.storeTextProjection(
                    artifactRef = original.artifactRef,
                    converterRef = "converter.vexlife-prod.plain-text.v1",
                    text = "evidence line one\nevidence line two\n",
                )
            val attachment =
                VexLifeLocalArtifact.RequestAttachment(
                    artifactRef = original.artifactRef,
                    projectionRef = projection.projectionRef,
                    displayName = "evidence.txt",
                )
            val humanWords =
                "Please use the attached evidence."

            var providerObservedBinding =
                false
            val transport =
                DirectCompanionConnection.Transport {
                    call ->
                    assertTrue(
                        call.body.contains(
                            VexLifeRequestArtifactManifest.Header,
                        ),
                        "provider request must discover the current REQUEST's Local Artifacts",
                    )
                    assertTrue(
                        call.body.contains(
                            "evidence.txt",
                        ),
                    )
                    assertTrue(
                        call.body.contains(
                            "converter.vexlife-prod.plain-text.v1",
                        ),
                    )
                    val currentRequest =
                        DirectConversationStore(
                            home,
                        )
                            .current()
                            .events
                            .single {
                                it.eventKind ==
                                    "REQUEST" &&
                                    it.content ==
                                    humanWords
                            }
                    val resourceRef =
                        VexLifeAttachmentReach.resourceRefFor(
                            requestEventHash = currentRequest.eventHash,
                            ordinal = 1,
                        )
                    assertTrue(
                        call.body.contains(
                            resourceRef,
                        ),
                        "provider input must carry the exact request-local artifact resource identity",
                    )
                    assertTrue(
                        call.body.contains(
                            "\"attachments_read\"",
                        ),
                        "native tool-capable models must receive the provider-native read function schema",
                    )
                    assertTrue(
                        call.body.contains(
                            VexLifeRequestArtifactManifest.ContentHeader,
                        ),
                    )
                    assertTrue(
                        call.body.contains(
                            "evidence line one",
                        ),
                        "small attached-file text should be present in the current-turn context before Vex answers",
                    )
                    val request =
                        currentRequest
                    assertEquals(
                        humanWords,
                        request.content,
                    )
                    assertFalse(
                        request.content.contains(
                            "Attached files",
                            ignoreCase = true,
                        ),
                    )
                    val binding =
                        assertNotNull(
                            store.bindingFor(
                                request.eventHash,
                            ),
                        )
                    assertEquals(
                        listOf(
                            attachment,
                        ),
                        binding.attachments,
                    )
                    assertEquals(
                        request.contentHash,
                        binding.requestContentHash,
                    )
                    providerObservedBinding =
                        true
                    reply(
                        "I used the evidence.",
                    )
                }

            val result =
                DirectCompanionTurn.performWithHooks(
                    setup = setup(
                        home,
                    ),
                    intent =
                        DirectCompanionTurn.Intent(
                            idempotencyKey = "send.attachment-turn.001",
                            content = humanWords,
                            requestAttachments =
                                listOf(
                                    attachment,
                                ),
                        ),
                    hooks =
                        DirectCompanionTurn.Hooks(
                            transport = transport,
                        ),
                )

            assertTrue(
                providerObservedBinding,
            )
            assertEquals(
                "I used the evidence.",
                result.content,
            )
            val artifactComponent =
                result.promptContextReceipt.contextComponents
                    .single {
                        it.componentRef ==
                            DirectPromptContext.ComponentRefs.CurrentRequestArtifacts
                    }
            assertTrue(
                artifactComponent.sourceRefs.contains(
                    original.artifactRef,
                ),
            )
            assertTrue(
                artifactComponent.sourceRefs.contains(
                    projection.projectionRef,
                ),
            )
            val requestComponent =
                result.promptContextReceipt.contextComponents
                    .single {
                        it.componentRef ==
                            DirectPromptContext.ComponentRefs.CurrentRequest
                    }
            assertEquals(
                listOf(
                    DirectConversationStore(
                        home,
                    )
                        .current()
                        .canonicalEvents
                        .single {
                            it.eventKind ==
                                "REQUEST"
                        }
                        .eventRef,
                ),
                requestComponent.sourceRefs,
            )
            val request =
                DirectConversationStore(
                    home,
                )
                    .current()
                    .canonicalEvents
                    .single {
                        it.eventKind ==
                            "REQUEST"
                    }
            assertEquals(
                humanWords,
                request.content,
            )
            assertEquals(
                listOf(
                    attachment,
                ),
                assertNotNull(
                    store.bindingFor(
                        request.eventHash,
                    ),
                )
                    .attachments,
            )
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }
    @Test
    fun sameIdempotencyKeyCannotRebindDifferentLocalArtifacts() {
        val parent =
            Files.createTempDirectory(
                "vexlife-attachment-turn-rebind-",
            )
                .toAbsolutePath()
        val home =
            parent.resolve(
                "VexLife-Prod",
            )

        try {
            CanonicalVexLifeHomeFixture.form(
                home = home,
                deviceSuffix = "attachment-rebind",
                lineageSuffix = "attachment-rebind",
            )
            val store =
                VexLifeLocalArtifactStore(
                    home,
                )
            fun attachment(
                text: String,
                name: String,
            ): VexLifeLocalArtifact.RequestAttachment {
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
                return VexLifeLocalArtifact.RequestAttachment(
                    artifactRef = original.artifactRef,
                    projectionRef = projection.projectionRef,
                    displayName = name,
                )
            }

            val first =
                attachment(
                    "first evidence",
                    "first.txt",
                )
            val second =
                attachment(
                    "second evidence",
                    "second.txt",
                )
            var providerCalls =
                0
            val transport =
                DirectCompanionConnection.Transport {
                    providerCalls +=
                        1
                    reply(
                        "Completed once.",
                    )
                }
            val intent =
                DirectCompanionTurn.Intent(
                    idempotencyKey = "send.attachment-turn.rebind.001",
                    content = "Use this evidence.",
                    requestAttachments =
                        listOf(
                            first,
                        ),
                )

            DirectCompanionTurn.performWithHooks(
                setup = setup(
                    home,
                ),
                intent = intent,
                hooks =
                    DirectCompanionTurn.Hooks(
                        transport = transport,
                    ),
            )
            assertEquals(
                1,
                providerCalls,
            )

            assertFailsWith<IllegalArgumentException> {
                DirectCompanionTurn.performWithHooks(
                    setup = setup(
                        home,
                    ),
                    intent =
                        intent.copy(
                            requestAttachments =
                                listOf(
                                    second,
                                ),
                        ),
                    hooks =
                        DirectCompanionTurn.Hooks(
                            transport = transport,
                        ),
                )
            }
            assertEquals(
                1,
                providerCalls,
                "resource mismatch must be rejected before another provider call",
            )
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }

}

// [VXG RealForever]
