package vexlife.companion

import java.nio.file.Files
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import vexlife.conversation.DirectConversationStore
import vexlife.home.CanonicalVexLifeHomeFixture
import vexlife.model.heretic.HereticModelConfiguration
import vexlife.model.heretic.HereticProductIdentity
import vexlife.model.heretic.HereticRuntimeQualification
import vexlife.model.heretic.HereticRuntimeState

class DirectTurnContextStoreTest {
    private fun home(
        productHome: java.nio.file.Path,
    ) {
        CanonicalVexLifeHomeFixture.form(
            home = productHome,
            deviceSuffix = "context-device",
            lineageSuffix = "context-lineage",
        )
    }

    private fun runtime(): HereticModelConfiguration.QualifiedRuntimeEvidence =
        HereticModelConfiguration.QualifiedRuntimeEvidence(
            runtimeReceiptRef = "receipt.vexlife-prod.heretic.context",
            runtimeState = HereticRuntimeState.Qualified,
            modelRef = HereticProductIdentity.ModelRef,
            modelProfileRef = HereticProductIdentity.ModelProfileRef,
            artifactRef = HereticProductIdentity.ArtifactSetRef,
            artifactContentSetSha256 = HereticProductIdentity.ArtifactContentSetSha256,
            runtimeAdapterRef = HereticProductIdentity.RuntimeAdapterRef,
            runtimeClass = HereticProductIdentity.RuntimeClass,
            endpoint = HereticProductIdentity.Endpoint,
            requestModel = HereticProductIdentity.RequestModel,
            qualification = HereticRuntimeQualification.qualified(),
        )

    @Test
    fun normalizedPromptAndInvocationEvidenceRoundTripWithoutRawProviderSecrets() {
        val parent =
            Files.createTempDirectory(
                "vexlife-prod-direct-context-",
            )
                .toAbsolutePath()
        val productHome =
            parent.resolve(
                "VexLife-Prod",
            )

        try {
            home(
                productHome,
            )
            val conversation =
                DirectConversationStore(
                    productHome,
                )
            val context =
                DirectTurnContextStore(
                    productHome,
                )

            conversation.withWriterLease {
                val request =
                    beginRequest(
                        turnRef = "turn.self-development.context",
                        requestMessageRef = "message.self-development.human.context",
                        content = "Hello",
                    )
                val prompt =
                    DirectPromptContext.materialize(
                        prior = null,
                        currentRequest = request,
                    )
                val inference =
                    DirectCompanionConnection.invoke(
                        runtime = runtime(),
                        messages = prompt.messages,
                        transport =
                            DirectCompanionConnection.Transport {
                                DirectCompanionConnection.HttpResult(
                                    status = 200,
                                    body =
                                        """
                                        {
                                          "model": "/private/model/path",
                                          "choices": [
                                            {
                                              "message": {
                                                "role": "assistant",
                                                "content": "Hi",
                                                "reasoning": "do not persist me"
                                              }
                                            }
                                          ]
                                        }
                                        """.trimIndent(),
                                )
                            },
                        now = {
                            Instant.parse(
                                "2026-11-05T04:31:00Z",
                            )
                        },
                    )
                val response =
                    appendResponse(
                        request = request,
                        responseMessageRef = "message.self-development.companion.context",
                        content = inference.content,
                        model = inference.model,
                        endpointOrigin = inference.invocationEvidence.endpointOrigin,
                        runtimeObservationRef = inference.runtimeObservation.observationRef,
                        invocationEvidenceRef = inference.invocationEvidence.invocationRef,
                    )
                val record =
                    context.record(
                        request = request,
                        response = response,
                        promptContext = prompt.receipt,
                        runtimeObservation = inference.runtimeObservation,
                        invocationEvidence = inference.invocationEvidence,
                        formedAt = Instant.parse(
                            "2026-11-05T04:31:01Z",
                        ),
                    )

                val reread =
                    assertNotNull(
                        context.read(
                            request.turnRef,
                        ),
                    )
                assertEquals(
                    record,
                    reread,
                )
                assertFalse(
                    reread.rawProviderResponsePersisted,
                )
                assertFalse(
                    reread.rawReasoningPersisted,
                )
                assertFalse(
                    reread.rawReportedModelPersisted,
                )
                assertEquals(
                    "LOCAL_PATH_LIKE",
                    reread.runtimeObservation.reportedModelField.pathClass,
                )

                val serialized =
                    Files.readString(
                        productHome
                            .resolve(
                                "context",
                            )
                            .resolve(
                                "companion-lineage.vexlife.context-lineage",
                            )
                            .resolve(
                                "thread.self-development.open-conversation",
                            )
                            .resolve(
                                "turn.self-development.context.json",
                            ),
                    )
                assertFalse(
                    serialized.contains(
                        "/private/model/path",
                    ),
                )
                assertFalse(
                    serialized.contains(
                        "do not persist me",
                    ),
                )
                assertTrue(
                    serialized.contains(
                        prompt.receipt.receiptRef,
                    ),
                )
            }
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }
}

// [VXG RealForever]
