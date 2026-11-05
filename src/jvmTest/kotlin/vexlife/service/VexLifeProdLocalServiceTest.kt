package vexlife.service

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vexlife.companion.DirectCompanionConnection
import vexlife.companion.DirectCompanionTurn
import vexlife.home.VexLifeProdHome
import vexlife.home.VexLifeProdHomeFiles
import vexlife.local.VexLifeProdLocalSetup
import vexlife.model.heretic.HereticModelConfiguration
import vexlife.model.heretic.HereticProductIdentity
import vexlife.model.heretic.HereticRuntimeQualification
import vexlife.model.heretic.HereticRuntimeState

class VexLifeProdLocalServiceTest {
    private val json =
        Json {
            encodeDefaults = true
            explicitNulls = true
            ignoreUnknownKeys = false
            isLenient = false
        }

    private val client =
        HttpClient.newHttpClient()

    private fun bootstrapHome(
        root: Path,
    ) {
        VexLifeProdHomeFiles.bootstrap(
            productHome = root,
            plan =
                VexLifeProdHome.plan(
                    personRef = VexLifeProdHome.DefaultPersonRef,
                    familyRef = VexLifeProdHome.DefaultFamilyRef,
                    deviceName = "Test Mac",
                    platform = "darwin",
                    architecture = "arm64",
                    ids =
                        VexLifeProdHome.InstallationIds(
                            deviceRef = "device.vexlife-prod.test-service",
                            companionLineageRef = "companion-lineage.vexlife-prod.test-service",
                        ),
                    createdAt = "2026-11-05T04:30:00-08:00",
                ),
        )
    }

    private fun setup(
        root: Path,
    ): VexLifeProdLocalSetup.Result {
        val runtime =
            HereticModelConfiguration.QualifiedRuntimeEvidence(
                runtimeReceiptRef = "receipt.vexlife-prod.heretic.service-test",
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

        return VexLifeProdLocalSetup.Result(
            state = "LOCAL_HERETIC_FOUNDATION_READY",
            productHome = root.toString(),
            homeDisposition = "TEST_CURRENT",
            modelSelectionDisposition = "TEST_BOUND",
            managedPythonDisposition = "TEST_REUSED",
            artifactDisposition = "TEST_REUSED",
            environmentDisposition = "TEST_REUSED",
            runtimeDisposition = "TEST_REUSED",
            modelBindingDisposition = "TEST_BOUND",
            modelConfigurationState = HereticModelConfiguration.State.BoundProductModel,
            qualifiedRuntimeEvidence = runtime,
            endpoint = runtime.endpoint,
            requestModel = runtime.requestModel,
        )
    }

    private fun get(
        origin: String,
        path: String,
    ): HttpResponse<String> =
        client.send(
            HttpRequest
                .newBuilder(
                    URI(
                        origin +
                            path,
                    ),
                )
                .GET()
                .build(),
            HttpResponse.BodyHandlers.ofString(
                StandardCharsets.UTF_8,
            ),
        )

    private fun postJson(
        origin: String,
        path: String,
        body: String,
    ): HttpResponse<String> =
        client.send(
            HttpRequest
                .newBuilder(
                    URI(
                        origin +
                            path,
                    ),
                )
                .header(
                    "content-type",
                    "application/json",
                )
                .POST(
                    HttpRequest.BodyPublishers.ofString(
                        body,
                        StandardCharsets.UTF_8,
                    ),
                )
                .build(),
            HttpResponse.BodyHandlers.ofString(
                StandardCharsets.UTF_8,
            ),
        )

    @Test
    fun sourceCurrentnessAndEmptyConversationAreReadOnlyProjections() {
        val parent =
            Files.createTempDirectory(
                "vexlife-prod-service-empty-",
            )
                .toAbsolutePath()
        val root =
            parent.resolve(
                "VexLife-Prod",
            )

        try {
            bootstrapHome(
                root,
            )
            val setup =
                setup(
                    root,
                )

            VexLifeProdLocalService
                .startForTest(
                    sourceSha = "a".repeat(
                        40,
                    ),
                    setup = setup,
                    turnExecutor =
                        VexLifeProdLocalService.TurnExecutor {
                            _,
                            _,
                            ->
                            error(
                                "turn must not run during read-only projection",
                            )
                        },
                )
                .use {
                    service ->
                    val currentness =
                        get(
                            service.origin,
                            VexLifeProdServiceContract.SourceCurrentnessPath,
                        )
                    assertEquals(
                        200,
                        currentness.statusCode(),
                    )
                    val source =
                        json.decodeFromString<VexLifeProdServiceContract.SourceCurrentness>(
                            currentness.body(),
                        )
                    assertEquals(
                        "a".repeat(
                            40,
                        ),
                        source.sourceSha,
                    )
                    assertEquals(
                        "SOURCE_BOUND_LISTENING",
                        source.serviceState,
                    )
                    assertEquals(
                        "NOT_REOBSERVED_BY_SERVICE",
                        source.runtimeCurrentness,
                    )
                    assertEquals(
                        setup.qualifiedRuntimeEvidence.runtimeReceiptRef,
                        source.startupQualifiedRuntimeReceiptRef,
                    )
                    assertNull(
                        source.currentConversationHeadSha256,
                    )
                    assertFalse(
                        source.effectsPerformed,
                    )

                    val conversation =
                        get(
                            service.origin,
                            VexLifeProdServiceContract.ConversationPath,
                        )
                    assertEquals(
                        200,
                        conversation.statusCode(),
                    )
                    val projection =
                        json.decodeFromString<VexLifeProdServiceContract.ConversationProjection>(
                            conversation.body(),
                        )
                    assertTrue(
                        projection.messages.isEmpty(),
                    )
                    assertNull(
                        projection.currentConversationHeadSha256,
                    )
                    assertEquals(
                        0,
                        projection.abandonedAttemptCount,
                    )
                    assertFalse(
                        projection.effectsPerformed,
                    )
                }

            assertFalse(
                Files.exists(
                    root.resolve(
                        "conversations",
                    ),
                ),
                "read-only service projections must not create Conversation storage",
            )
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun sendDelegatesToCanonicalTurnAndDuplicateAttemptDoesNotCallProviderTwice() {
        val parent =
            Files.createTempDirectory(
                "vexlife-prod-service-send-",
            )
                .toAbsolutePath()
        val root =
            parent.resolve(
                "VexLife-Prod",
            )
        var providerCalls =
            0

        try {
            bootstrapHome(
                root,
            )
            val setup =
                setup(
                    root,
                )
            val transport =
                DirectCompanionConnection.Transport {
                    providerCalls +=
                        1
                    DirectCompanionConnection.HttpResult(
                        status = 200,
                        body =
                            """
                            {
                              "model": "default_model",
                              "choices": [
                                {
                                  "finish_reason": "stop",
                                  "message": {
                                    "role": "assistant",
                                    "content": "Hello from Vex."
                                  }
                                }
                              ]
                            }
                            """.trimIndent(),
                    )
                }

            VexLifeProdLocalService
                .startForTest(
                    sourceSha = "b".repeat(
                        40,
                    ),
                    setup = setup,
                    turnExecutor =
                        VexLifeProdLocalService.TurnExecutor {
                            currentSetup,
                            intent,
                            ->
                            DirectCompanionTurn.performWithHooks(
                                setup = currentSetup,
                                intent = intent,
                                hooks =
                                    DirectCompanionTurn.Hooks(
                                        transport = transport,
                                        now = {
                                            Instant.parse(
                                                "2026-11-05T12:31:00Z",
                                            )
                                        },
                                    ),
                            )
                        },
                )
                .use {
                    service ->
                    val request =
                        VexLifeProdServiceContract.SendRequest(
                            sendAttemptRef = "send-attempt.vexlife-prod.service.001",
                            content = "Hello Vex",
                        )
                    val body =
                        json.encodeToString(
                            request,
                        )

                    val first =
                        postJson(
                            service.origin,
                            VexLifeProdServiceContract.ConversationSendPath,
                            body,
                        )
                    assertEquals(
                        200,
                        first.statusCode(),
                    )
                    val firstResult =
                        json.decodeFromString<VexLifeProdServiceContract.SendResponse>(
                            first.body(),
                        )
                    assertEquals(
                        "TURN_COMPLETED",
                        firstResult.state,
                    )
                    assertEquals(
                        "Hello from Vex.",
                        firstResult.content,
                    )
                    assertFalse(
                        firstResult.recoveredCompletedTurn,
                    )
                    assertTrue(
                        firstResult.actualHttpCallPerformedNow,
                    )
                    assertEquals(
                        1,
                        providerCalls,
                    )

                    val projectionResponse =
                        get(
                            service.origin,
                            VexLifeProdServiceContract.ConversationPath,
                        )
                    val projection =
                        json.decodeFromString<VexLifeProdServiceContract.ConversationProjection>(
                            projectionResponse.body(),
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
                    assertEquals(
                        listOf(
                            "Hello Vex",
                            "Hello from Vex.",
                        ),
                        projection.messages.map {
                            it.content
                        },
                    )
                    assertEquals(
                        firstResult.currentConversationHeadSha256,
                        projection.currentConversationHeadSha256,
                    )

                    val duplicate =
                        postJson(
                            service.origin,
                            VexLifeProdServiceContract.ConversationSendPath,
                            body,
                        )
                    assertEquals(
                        200,
                        duplicate.statusCode(),
                    )
                    val duplicateResult =
                        json.decodeFromString<VexLifeProdServiceContract.SendResponse>(
                            duplicate.body(),
                        )
                    assertTrue(
                        duplicateResult.recoveredCompletedTurn,
                    )
                    assertFalse(
                        duplicateResult.actualHttpCallPerformedNow,
                    )
                    assertEquals(
                        firstResult.currentConversationHeadSha256,
                        duplicateResult.currentConversationHeadSha256,
                    )
                    assertEquals(
                        1,
                        providerCalls,
                    )
                }
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun unknownSendFieldsFailBeforeTurnEffect() {
        val parent =
            Files.createTempDirectory(
                "vexlife-prod-service-strict-json-",
            )
                .toAbsolutePath()
        val root =
            parent.resolve(
                "VexLife-Prod",
            )
        var turns =
            0

        try {
            bootstrapHome(
                root,
            )

            VexLifeProdLocalService
                .startForTest(
                    sourceSha = "c".repeat(
                        40,
                    ),
                    setup = setup(
                        root,
                    ),
                    turnExecutor =
                        VexLifeProdLocalService.TurnExecutor {
                            _,
                            _,
                            ->
                            turns +=
                                1
                            error(
                                "turn must not run for invalid wire input",
                            )
                        },
                )
                .use {
                    service ->
                    val response =
                        postJson(
                            service.origin,
                            VexLifeProdServiceContract.ConversationSendPath,
                            """
                            {
                              "sendAttemptRef": "send-attempt.vexlife-prod.service.bad",
                              "content": "Hello",
                              "endpoint": "http://example.com"
                            }
                            """.trimIndent(),
                        )

                    assertEquals(
                        400,
                        response.statusCode(),
                    )
                    assertEquals(
                        0,
                        turns,
                    )
                    val error =
                        json.decodeFromString<VexLifeProdServiceContract.ErrorResponse>(
                            response.body(),
                        )
                    assertEquals(
                        "REQUEST_JSON_INVALID",
                        error.code,
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
