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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vexlife.browser.VexLifeProdBrowserContract
import vexlife.companion.DirectCompanionConnection
import vexlife.companion.DirectCompanionTurn
import vexlife.conversation.CanonicalLivedContextFixture
import vexlife.conversation.DirectConversationStore
import vexlife.home.CanonicalVexLifeHomeFixture
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
        CanonicalVexLifeHomeFixture.form(
            home = root,
            deviceSuffix = "test-service",
            lineageSuffix = "test-service",
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
                    assertTrue(
                        source.processPid > 0,
                    )
                    assertEquals(
                        ProcessHandle.current().pid(),
                        source.processPid,
                    )
                    assertEquals(
                        service.frameOrigin,
                        source.frameHostOrigin,
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

                    val presentation =
                        get(
                            service.origin,
                            VexLifeProdServiceContract.PresentationIndexPath,
                        )
                    assertEquals(
                        200,
                        presentation.statusCode(),
                    )
                    assertTrue(
                        presentation.body()
                            .contains(
                                "data-vex-screen=\"screen.vexlife-workspace\"",
                            ),
                    )
                    val runtimeJavascript =
                        get(
                            service.origin,
                            VexLifeProdServiceContract.PresentationJavascriptPath,
                        )
                    assertEquals(
                        200,
                        runtimeJavascript.statusCode(),
                    )
                    assertTrue(
                        runtimeJavascript.body()
                            .contains(
                                VexLifeProdServiceContract.ConversationSendPath,
                            ),
                    )
                    assertTrue(
                        runtimeJavascript.body()
                            .contains(
                                VexLifeProdServiceContract.ContextPath,
                            ),
                    )
                    assertTrue(
                        runtimeJavascript.body()
                            .contains(
                                "Converse with Vex",
                            ),
                    )
                    assertTrue(
                        runtimeJavascript.body()
                            .contains(
                                "Include this page",
                            ),
                    )
                    assertTrue(
                        presentation.body()
                            .contains(
                                "Start clean chat",
                            ),
                    )
                    assertTrue(
                        runtimeJavascript.body()
                            .contains(
                                VexLifeProdBrowserContract.InitialPageUrl,
                            ),
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

                    val contextResponse =
                        get(
                            service.origin,
                            VexLifeProdServiceContract.ContextPath,
                        )
                    assertEquals(
                        200,
                        contextResponse.statusCode(),
                    )
                    val contextProjection =
                        json.decodeFromString<VexLifeProdServiceContract.ContextProjection>(
                            contextResponse.body(),
                        )
                    assertNull(
                        contextProjection.currentConversationHeadSha256,
                    )
                    assertNull(
                        contextProjection.turnRef,
                    )
                    assertNull(
                        contextProjection.contextPath,
                    )
                    assertNull(
                        contextProjection.promptContext,
                    )
                    assertFalse(
                        contextProjection.effectsPerformed,
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
    fun historicalCanonicalHeadProjectsWithoutInventingProdPromptEvidence() {
        val parent =
            Files.createTempDirectory(
                "vexlife-prod-service-historical-context-",
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
            val store =
                DirectConversationStore(
                    root,
                )
            var expectedHeadSha256 =
                ""
            var expectedContextPath =
                ""

            store.withWriterLease {
                val request =
                    beginRequest(
                        turnRef =
                            "turn.self-development.historical-context",
                        requestMessageRef =
                            "message.self-development.human.historical-context",
                        content =
                            "Historical request",
                        formedAt =
                            Instant.parse(
                                "2026-11-06T23:30:00Z",
                            ),
                    )
                val response =
                    appendResponse(
                        request = request,
                        responseMessageRef =
                            "message.self-development.companion.historical-context",
                        content =
                            "Historical response",
                        model =
                            "default_model",
                        endpointOrigin =
                            "http://127.0.0.1:8765",
                        runtimeObservationRef =
                            "observation.vexlife-prod.model-runtime.historical-context",
                        invocationEvidenceRef =
                            "evidence.vexlife-prod.model-runtime-invocation.historical-context",
                        formedAt =
                            Instant.parse(
                                "2026-11-06T23:30:01Z",
                            ),
                    )
                val context =
                    CanonicalLivedContextFixture.write(
                        productHome =
                            root,
                        request =
                            request,
                        response =
                            response,
                        formedAt =
                            Instant.parse(
                                "2026-11-06T23:30:02Z",
                            ),
                    )
                val head =
                    commitCompletedTurn(
                        request =
                            request,
                        response =
                            response,
                        contextSha256 =
                            context.sha256,
                        formedAt =
                            Instant.parse(
                                "2026-11-06T23:30:03Z",
                            ),
                    )
                expectedHeadSha256 =
                    head.conversationHeadSha256
                expectedContextPath =
                    head.contextPath
            }

            VexLifeProdLocalService
                .startForTest(
                    sourceSha =
                        "d".repeat(
                            40,
                        ),
                    setup =
                        setup(
                            root,
                        ),
                    turnExecutor =
                        VexLifeProdLocalService.TurnExecutor {
                            _,
                            _,
                            ->
                            error(
                                "historical Context GET must not run a turn",
                            )
                        },
                )
                .use {
                    service ->
                    val response =
                        get(
                            service.origin,
                            VexLifeProdServiceContract.ContextPath,
                        )
                    assertEquals(
                        200,
                        response.statusCode(),
                    )
                    val projection =
                        json.decodeFromString<VexLifeProdServiceContract.ContextProjection>(
                            response.body(),
                        )
                    assertEquals(
                        expectedHeadSha256,
                        projection.currentConversationHeadSha256,
                    )
                    assertEquals(
                        "turn.self-development.historical-context",
                        projection.turnRef,
                    )
                    assertEquals(
                        expectedContextPath,
                        projection.contextPath,
                    )
                    assertNull(
                        projection.promptContext,
                    )
                    assertFalse(
                        projection.effectsPerformed,
                    )
                }
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
                                    "content": "Hello from Vex.",
                                    "reasoning": "ephemeral service reasoning"
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
                            idempotencyKey = "send.vexlife.browser.service.001",
                            content = "Hello Vex",
                            browserContext =
                                VexLifeProdServiceContract.BrowserContextInput(
                                    pageUrl = VexLifeProdBrowserContract.InitialPageUrl,
                                    title = "Vextreme Terrain",
                                    text = "Current direct-contact terrain context",
                                    capture = VexLifeProdBrowserContract.CaptureRendered,
                                ),
                            viewableThoughts = true,
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
                        "PRESENT_EPHEMERAL",
                        firstResult.humanReasoning.state,
                    )
                    assertEquals(
                        "ephemeral service reasoning",
                        firstResult.humanReasoning.text,
                    )
                    assertFalse(
                        firstResult.humanReasoning.persisted,
                    )
                    assertEquals(
                        "ephemeral service reasoning".length,
                        firstResult.humanReasoning.characterCount,
                    )
                    assertTrue(
                        firstResult.promptContext.browserContextIncluded,
                    )
                    assertEquals(
                        VexLifeProdBrowserContract.InitialPageUrl,
                        firstResult.promptContext.browserPageUrl,
                    )
                    assertEquals(
                        VexLifeProdBrowserContract.CaptureRendered,
                        firstResult.promptContext.browserContextCapture,
                    )
                    assertEquals(
                        "Current direct-contact terrain context".length,
                        firstResult.promptContext.browserContextCharacters,
                    )
                    assertTrue(
                        firstResult.promptContext.browserContextRef
                            ?.startsWith(
                                "browser-context.vexlife-prod.",
                            ) ==
                            true,
                    )
                    assertTrue(
                        firstResult.promptContext.capabilityFrameIncluded,
                    )
                    val firstCapabilityFrame =
                        assertNotNull(
                            firstResult.promptContext.capabilityFrame,
                        )
                    assertTrue(
                        firstCapabilityFrame.availableCapabilityRefs.contains(
                            "capability.vexlife.companion.local-inference",
                        ),
                    )
                    assertTrue(
                        firstCapabilityFrame.availableCapabilityRefs.contains(
                            "capability.vexlife.context.browser-current",
                        ),
                    )
                    assertTrue(
                        firstCapabilityFrame.heldCapabilityRefs.contains(
                            "capability.vexlife.context.working-continuity",
                        ),
                    )
                    assertFalse(
                        firstCapabilityFrame.effectAuthorityGranted,
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
                    assertFalse(
                        projectionResponse.body()
                            .contains(
                                "ephemeral service reasoning",
                            ),
                        "canonical Conversation projection must not expose raw reasoning",
                    )

                    val contextResponse =
                        get(
                            service.origin,
                            VexLifeProdServiceContract.ContextPath,
                        )
                    assertEquals(
                        200,
                        contextResponse.statusCode(),
                    )
                    val currentContext =
                        json.decodeFromString<VexLifeProdServiceContract.ContextProjection>(
                            contextResponse.body(),
                        )
                    assertEquals(
                        firstResult.currentConversationHeadSha256,
                        currentContext.currentConversationHeadSha256,
                    )
                    assertEquals(
                        firstResult.turnRef,
                        currentContext.turnRef,
                    )
                    assertEquals(
                        firstResult.contextPath,
                        currentContext.contextPath,
                    )
                    val currentPrompt =
                        assertNotNull(
                            currentContext.promptContext,
                        )
                    assertEquals(
                        firstResult.promptContext.receiptRef,
                        currentPrompt.receiptRef,
                    )
                    assertEquals(
                        firstResult.promptContext.capabilityFrame,
                        currentPrompt.capabilityFrame,
                    )
                    assertFalse(
                        currentContext.effectsPerformed,
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
                        "UNAVAILABLE_AFTER_IDEMPOTENT_RECOVERY",
                        duplicateResult.humanReasoning.state,
                    )
                    assertNull(
                        duplicateResult.humanReasoning.text,
                    )
                    assertFalse(
                        duplicateResult.humanReasoning.persisted,
                    )
                    assertEquals(
                        firstResult.humanReasoning.contentSha256,
                        duplicateResult.humanReasoning.contentSha256,
                    )
                    assertEquals(
                        firstResult.humanReasoning.characterCount,
                        duplicateResult.humanReasoning.characterCount,
                    )
                    assertEquals(
                        firstResult.currentConversationHeadSha256,
                        duplicateResult.currentConversationHeadSha256,
                    )
                    assertEquals(
                        1,
                        providerCalls,
                    )

                    val mismatchedReasoningMode =
                        postJson(
                            service.origin,
                            VexLifeProdServiceContract.ConversationSendPath,
                            json.encodeToString(
                                request.copy(
                                    viewableThoughts = false,
                                ),
                            ),
                        )
                    assertEquals(
                        400,
                        mismatchedReasoningMode.statusCode(),
                    )
                    val mismatchError =
                        json.decodeFromString<VexLifeProdServiceContract.ErrorResponse>(
                            mismatchedReasoningMode.body(),
                        )
                    assertEquals(
                        "REQUEST_NOT_ADMITTED",
                        mismatchError.code,
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
                              "idempotencyKey": "send.vexlife.browser.service.bad",
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

    @Test
    fun duplicateWithoutReasoningRemainsAbsentInsteadOfClaimingLostRawValue() {
        val parent =
            Files.createTempDirectory(
                "vexlife-prod-service-no-reasoning-duplicate-",
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
                                  "message": {
                                    "role": "assistant",
                                    "content": "No reasoning field here."
                                  }
                                }
                              ]
                            }
                            """.trimIndent(),
                    )
                }

            VexLifeProdLocalService
                .startForTest(
                    sourceSha = "e".repeat(
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
                                    ),
                            )
                        },
                )
                .use {
                    service ->
                    val request =
                        VexLifeProdServiceContract.SendRequest(
                            idempotencyKey =
                                "send.vexlife.browser.service.no-reasoning",
                            content =
                                "Answer without reasoning",
                            viewableThoughts =
                                true,
                        )
                    val body =
                        json.encodeToString(
                            request,
                        )

                    val first =
                        json.decodeFromString<VexLifeProdServiceContract.SendResponse>(
                            postJson(
                                service.origin,
                                VexLifeProdServiceContract.ConversationSendPath,
                                body,
                            )
                                .body(),
                        )
                    assertEquals(
                        "ABSENT",
                        first.humanReasoning.state,
                    )
                    assertNull(
                        first.humanReasoning.text,
                    )
                    assertNull(
                        first.humanReasoning.contentSha256,
                    )
                    assertEquals(
                        0,
                        first.humanReasoning.characterCount,
                    )

                    val duplicate =
                        json.decodeFromString<VexLifeProdServiceContract.SendResponse>(
                            postJson(
                                service.origin,
                                VexLifeProdServiceContract.ConversationSendPath,
                                body,
                            )
                                .body(),
                        )
                    assertTrue(
                        duplicate.recoveredCompletedTurn,
                    )
                    assertEquals(
                        "ABSENT",
                        duplicate.humanReasoning.state,
                    )
                    assertNull(
                        duplicate.humanReasoning.text,
                    )
                    assertNull(
                        duplicate.humanReasoning.contentSha256,
                    )
                    assertEquals(
                        0,
                        duplicate.humanReasoning.characterCount,
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

}

// [VXG RealForever]
