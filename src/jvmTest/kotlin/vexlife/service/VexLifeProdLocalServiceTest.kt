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
import kotlinx.serialization.json.JsonObject
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
import vexlife.memory.VexLifeDailyMemoryDream
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

    private fun currentState(
        root: Path,
    ): VexLifeProdServiceContract.CurrentStateProjection =
        VexLifeProdServiceContract.CurrentStateProjection(
            schemaVersion = VexLifeProdServiceContract.CurrentStateProjectionSchema,
            observedAt = "2026-11-12T18:00:00Z",
            homePath = root.toString(),
            homeDisposition = "TEST_CURRENT",
            homeRef = "vex-home.device.vexlife.test-service",
            deviceRef = "device.vexlife.test-service",
            companionLineageRef = "companion-lineage.vexlife.test-service",
            platform = "darwin",
            architecture = "arm64",
            chipModel = "Apple M4 Pro",
            hostEligibilityState = "ELIGIBLE",
            hostEligibilityReason = null,
            totalMemoryBytes = 36L * 1024L * 1024L * 1024L,
            modelMinimumMemoryBytes = 12L * 1024L * 1024L * 1024L,
            freeStorageBytes = 100L * 1024L * 1024L * 1024L,
            modelSelectionState = "BOUND_HERETIC_PRODUCT_MODEL",
            activeModelRef = HereticProductIdentity.ModelRef,
            activeModelProfileRef = HereticProductIdentity.ModelProfileRef,
            runtimeState = "BOUND_PRODUCT_MODEL",
            runtimeCurrentness = "CONFIGURATION_ONLY_NOT_REOBSERVED",
            runtimeReceiptPath = HereticProductIdentity.StableRuntimeReceiptRelativePath,
            modelArtifactState = "COMPLETE_BY_SIZE_NOT_HASHED",
            modelArtifactTotalBytes = 15_153_242_506L,
            modelArtifactPresentBySizeBytes = 15_153_242_506L,
            modelArtifactMissingBytes = 0,
            modelArtifactInvalidSizeBytes = 0,
            effectsPerformed = false,
        )

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

    private fun postEventStream(
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
                    "accept",
                    "text/event-stream",
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
                    currentStateObserver = {
                        currentState(
                            root,
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

                    val currentStateResponse =
                        get(
                            service.origin,
                            VexLifeProdServiceContract.CurrentStatePath,
                        )
                    assertEquals(
                        200,
                        currentStateResponse.statusCode(),
                    )
                    val currentState =
                        json.decodeFromString<VexLifeProdServiceContract.CurrentStateProjection>(
                            currentStateResponse.body(),
                        )
                    assertEquals(
                        root.toString(),
                        currentState.homePath,
                    )
                    assertEquals(
                        HereticProductIdentity.ModelRef,
                        currentState.activeModelRef,
                    )
                    assertEquals(
                        "COMPLETE_BY_SIZE_NOT_HASHED",
                        currentState.modelArtifactState,
                    )
                    assertEquals(
                        15_153_242_506L,
                        currentState.modelArtifactTotalBytes,
                    )
                    assertEquals(
                        currentState.modelArtifactTotalBytes,
                        currentState.modelArtifactPresentBySizeBytes,
                    )
                    assertEquals(
                        "CONFIGURATION_ONLY_NOT_REOBSERVED",
                        currentState.runtimeCurrentness,
                    )
                    assertFalse(
                        currentState.effectsPerformed,
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
    fun visionRouteIsHeldWithoutOneVerifiedRuntime() {
        val parent =
            Files.createTempDirectory(
                "vexlife-prod-service-vision-held-",
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

            VexLifeProdLocalService
                .startForTest(
                    sourceSha =
                        "a".repeat(
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
                                "turn must not run during Vision projection",
                            )
                        },
                )
                .use {
                    service ->
                    val vision =
                        get(
                            service.origin,
                            VexLifeProdServiceContract.VisionPath,
                        )
                    assertEquals(
                        200,
                        vision.statusCode(),
                    )
                    val projection =
                        json.decodeFromString<VexLifeProdServiceContract.VisionProjection>(
                            vision.body(),
                        )
                    assertEquals(
                        "HELD",
                        projection.state,
                    )
                    assertFalse(
                        projection.routeReachable,
                    )
                    assertFalse(
                        projection.effectsPerformed,
                    )

                    val lookNow =
                        postJson(
                            service.origin,
                            VexLifeProdServiceContract.VisionLookNowPath,
                            json.encodeToString(
                                VexLifeProdServiceContract.VisionLookNowRequest(
                                    requestRef =
                                        "request.vexlife-prod.vision.held.001",
                                ),
                            ),
                        )
                    assertEquals(
                        409,
                        lookNow.statusCode(),
                    )
                }
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun visionRouteIsReadyAndInvokesOneExactRuntimeRequest() {
        val parent =
            Files.createTempDirectory(
                "vexlife-prod-service-vision-ready-",
            )
                .toAbsolutePath()
        val root =
            parent.resolve(
                "VexLife-Prod",
            )
        val requestRef =
            "request.vexlife-prod.vision.ready.001"
        var invocationCount =
            0

        try {
            bootstrapHome(
                root,
            )

            VexLifeProdLocalService
                .startForTest(
                    sourceSha =
                        "a".repeat(
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
                                "turn must not run during Vision request",
                            )
                        },
                    visionExecutor =
                        VexLifeProdLocalService.VisionExecutor {
                            observedRequestRef ->
                            invocationCount +=
                                1
                            assertEquals(
                                requestRef,
                                observedRequestRef,
                            )
                            VexLifeProdServiceContract.VisionRuntimeResult(
                                schemaVersion =
                                    VexLifeProdServiceContract.VisionRuntimeResultSchema,
                                state = "USER_CANCELLED",
                                requestRef = observedRequestRef,
                                sessionRefOrNull =
                                    "session.macos-screen-system-picker.product-runtime.test",
                                connectionIdOrNull =
                                    "connection.macos-screen-system-picker.product-runtime.test",
                                selectionMode = "SINGLE_WINDOW",
                                osOwnsSelectionUi = true,
                                helperLaunched = true,
                                projectionOrNull =
                                    JsonObject(
                                        emptyMap(),
                                    ),
                                activeSharingWitnessOrNull = null,
                                activeVisualObservationOrNull = null,
                                rawBytesIncluded = false,
                                rawRetentionAllowed = false,
                                visualModelInputAuthorized = false,
                                memoryAccepted = false,
                                trainingEligible = false,
                                publicationEligible = false,
                                continuousCaptureAllowed = false,
                                backgroundCaptureAllowed = false,
                                persistentHostPermissionEstablished = false,
                            )
                        },
                )
                .use {
                    service ->
                    val vision =
                        get(
                            service.origin,
                            VexLifeProdServiceContract.VisionPath,
                        )
                    assertEquals(
                        200,
                        vision.statusCode(),
                    )
                    val projection =
                        json.decodeFromString<VexLifeProdServiceContract.VisionProjection>(
                            vision.body(),
                        )
                    assertEquals(
                        "READY",
                        projection.state,
                    )
                    assertTrue(
                        projection.routeReachable,
                    )
                    assertFalse(
                        projection.effectsPerformed,
                    )

                    val lookNow =
                        postJson(
                            service.origin,
                            VexLifeProdServiceContract.VisionLookNowPath,
                            json.encodeToString(
                                VexLifeProdServiceContract.VisionLookNowRequest(
                                    requestRef = requestRef,
                                ),
                            ),
                        )
                    assertEquals(
                        200,
                        lookNow.statusCode(),
                    )
                    val result =
                        json.decodeFromString<VexLifeProdServiceContract.VisionRuntimeResult>(
                            lookNow.body(),
                        )
                    assertEquals(
                        "USER_CANCELLED",
                        result.state,
                    )
                    assertEquals(
                        requestRef,
                        result.requestRef,
                    )
                    assertEquals(
                        1,
                        invocationCount,
                    )
                    assertTrue(
                        result.helperLaunched,
                    )
                    assertNull(
                        result.activeSharingWitnessOrNull,
                    )
                    assertNull(
                        result.activeVisualObservationOrNull,
                    )
                }
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun memoryDreamRouteProjectsCurrentnessAndPerformsOneExplicitManualRun() {
        val parent = Files.createTempDirectory("vexlife-prod-service-dream-").toAbsolutePath()
        val root = parent.resolve("VexLife-Prod")
        var runCount = 0
        var currentHead: String? = null
        var currentStratum: String? = null
        var sourceConversationHead = "a".repeat(64)
        var scoreHead = "b".repeat(64)
        var scoreSourceConversationHead = sourceConversationHead

        fun projection(): VexLifeProdServiceContract.MemoryDreamProjection =
            VexLifeProdServiceContract.MemoryDreamProjection(
                schemaVersion = VexLifeProdServiceContract.MemoryDreamProjectionSchema,
                state =
                    if (scoreSourceConversationHead == sourceConversationHead) {
                        "SOURCE_FRONTIER_READY"
                    } else {
                        "SCORE_NOT_CAUGHT_UP"
                    },
                sourceConversationHeadSha256 = sourceConversationHead,
                sourceScoreHeadSha256 = scoreHead,
                scoreSourceConversationHeadSha256 = scoreSourceConversationHead,
                sourceSemanticAuthorityHeadSha256 = "c".repeat(64),
                currentDailyDreamHeadSha256 = currentHead,
                currentDailyStratumSha256 = currentStratum,
                currentDayRef = if (currentHead == null) null else "day.g03.000",
                currentDayIndex = if (currentHead == null) null else 0L,
                nextDayIndex = if (currentHead == null) 0L else 1L,
                activeStatementCount = 2,
                heldStatementCount = 1,
                openLoopCount = 1,
                effectsPerformed = false,
            )

        try {
            bootstrapHome(root)
            val dreamExecutor =
                object : VexLifeProdLocalService.DreamExecutor {
                    override fun observe(
                        setup: VexLifeProdLocalSetup.Result,
                    ): VexLifeProdServiceContract.MemoryDreamProjection =
                        projection()

                    override fun perform(
                        setup: VexLifeProdLocalSetup.Result,
                        request: VexLifeProdServiceContract.MemoryDreamRunRequest,
                    ): VexLifeDailyMemoryDream.Result {
                        runCount += 1
                        assertEquals("authority.manual.g03.lived", request.restInvocationAuthorityRef)
                        assertEquals("a".repeat(64), request.expectedConversationHeadSha256)
                        assertEquals("b".repeat(64), request.expectedScoreHeadSha256)
                        assertNull(request.expectedDailyDreamHeadSha256)
                        currentHead = "d".repeat(64)
                        currentStratum = "e".repeat(64)
                        return VexLifeDailyMemoryDream.Result(
                            state = if (runCount == 1) "COMMITTED" else "IDEMPOTENT_REPLAY",
                            priorDailyDreamHeadSha256 = if (runCount == 1) null else requireNotNull(currentHead),
                            dailyDreamHeadSha256 = requireNotNull(currentHead),
                            dailyStratumSha256 = requireNotNull(currentStratum),
                            sourceConversationHeadSha256 = "a".repeat(64),
                            sourceScoreHeadSha256 = "b".repeat(64),
                            sourceSemanticAuthorityHeadSha256 = "c".repeat(64),
                            activeStatementRefs = listOf("statement.one", "statement.two"),
                            heldStatementRefs = listOf("statement.held"),
                            openLoopRefs = listOf("loop.one"),
                        )
                    }
                }

            VexLifeProdLocalService
                .startForTest(
                    sourceSha = "f".repeat(40),
                    setup = setup(root),
                    turnExecutor =
                        VexLifeProdLocalService.TurnExecutor { _, _ ->
                            error("memory Dream route must not run a Companion turn")
                        },
                    dreamExecutor = dreamExecutor,
                )
                .use { service ->
                    val beforeResponse = get(service.origin, VexLifeProdServiceContract.MemoryDreamPath)
                    assertEquals(200, beforeResponse.statusCode())
                    val before =
                        json.decodeFromString<VexLifeProdServiceContract.MemoryDreamProjection>(
                            beforeResponse.body(),
                        )
                    assertNull(before.currentDailyDreamHeadSha256)
                    assertEquals(0L, before.nextDayIndex)
                    assertFalse(before.effectsPerformed)

                    val request =
                        VexLifeProdServiceContract.MemoryDreamRunRequest(
                            restInvocationAuthorityRef = "authority.manual.g03.lived",
                            dayRef = "day.g03.000",
                            dayIndex = 0,
                            calendarDateRef = "2026-11-10",
                            timeZoneRef = "America/Los_Angeles",
                            observedAt = "2026-11-10T08:00:00Z",
                            expectedConversationHeadSha256 = "a".repeat(64),
                            expectedScoreHeadSha256 = "b".repeat(64),
                            expectedDailyDreamHeadSha256 = null,
                        )
                    val runResponse =
                        postJson(
                            service.origin,
                            VexLifeProdServiceContract.MemoryDreamPath,
                            json.encodeToString(request),
                        )
                    assertEquals(200, runResponse.statusCode())
                    val result =
                        json.decodeFromString<VexLifeProdServiceContract.MemoryDreamRunResponse>(
                            runResponse.body(),
                        )
                    assertEquals("COMMITTED", result.state)
                    assertEquals(1, runCount)
                    assertNull(result.before.currentDailyDreamHeadSha256)
                    assertEquals("d".repeat(64), result.after.currentDailyDreamHeadSha256)
                    assertEquals("e".repeat(64), result.after.currentDailyStratumSha256)
                    assertTrue(result.memoryFrontierMutationPerformed)
                    assertTrue(result.systemMemoryConsolidationPerformedNow)
                    assertFalse(result.modelCallPerformed)
                    assertFalse(result.rhythmLearned)
                    assertFalse(result.trainingRan)
                    assertFalse(result.modelWeightsChanged)
                    assertTrue(result.effectsPerformed)

                    sourceConversationHead = "1".repeat(64)
                    scoreHead = "2".repeat(64)
                    scoreSourceConversationHead = sourceConversationHead
                    val replayResponse =
                        postJson(
                            service.origin,
                            VexLifeProdServiceContract.MemoryDreamPath,
                            json.encodeToString(request),
                        )
                    assertEquals(200, replayResponse.statusCode())
                    val replay =
                        json.decodeFromString<VexLifeProdServiceContract.MemoryDreamRunResponse>(
                            replayResponse.body(),
                        )
                    assertEquals("IDEMPOTENT_REPLAY", replay.state)
                    assertEquals(2, runCount)
                    assertEquals("1".repeat(64), replay.before.sourceConversationHeadSha256)
                    assertEquals("d".repeat(64), replay.after.currentDailyDreamHeadSha256)
                    assertFalse(replay.memoryFrontierMutationPerformed)
                    assertFalse(replay.systemMemoryConsolidationPerformedNow)
                    assertFalse(replay.effectsPerformed)
                }
        } finally {
            parent.toFile().deleteRecursively()
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


    @Test
    fun streamingSendProjectsLivePhasesReasoningAndAnswerBeforeCompletedEvent() {
        val parent =
            Files.createTempDirectory(
                "vexlife-prod-service-stream-",
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
            val currentSetup =
                setup(
                    root,
                )
            val transport =
                object :
                    DirectCompanionConnection.StreamingTransport {
                    override fun post(
                        call: DirectCompanionConnection.HttpCall,
                    ): DirectCompanionConnection.HttpResult =
                        error(
                            "stream test must use the provider stream",
                        )

                    override fun postStreaming(
                        call: DirectCompanionConnection.HttpCall,
                        onLine: (String) -> Unit,
                    ): DirectCompanionConnection.StreamingHttpResult {
                        providerCalls +=
                            1
                        onLine(
                            """data: {"model":"default_model","choices":[{"delta":{"role":"assistant","reasoning":"Compare "}}]}""",
                        )
                        onLine(
                            "",
                        )
                        onLine(
                            """data: {"model":"default_model","choices":[{"delta":{"role":"assistant","reasoning":"carefully."}}]}""",
                        )
                        onLine(
                            "",
                        )
                        onLine(
                            """data: {"model":"default_model","choices":[{"delta":{"role":"assistant","content":"Choose "}}]}""",
                        )
                        onLine(
                            "",
                        )
                        onLine(
                            """data: {"model":"default_model","choices":[{"finish_reason":"stop","delta":{"role":"assistant","content":"A."}}]}""",
                        )
                        onLine(
                            "",
                        )
                        onLine(
                            "data: [DONE]",
                        )
                        return DirectCompanionConnection.StreamingHttpResult(
                            status = 200,
                            responseBodySha256 = "f".repeat(
                                64,
                            ),
                        )
                    }
                }

            VexLifeProdLocalService
                .startForTest(
                    sourceSha = "f".repeat(
                        40,
                    ),
                    setup = currentSetup,
                    turnExecutor =
                        VexLifeProdLocalService.TurnExecutor {
                            _,
                            _,
                            ->
                            error(
                                "event-stream request must not use bulk turn executor",
                            )
                        },
                    streamingTurnExecutor =
                        VexLifeProdLocalService.StreamingTurnExecutor {
                            setup,
                            intent,
                            observer,
                            ->
                            DirectCompanionTurn.performWithHooks(
                                setup = setup,
                                intent = intent,
                                hooks =
                                    DirectCompanionTurn.Hooks(
                                        transport = transport,
                                        progressObserver = observer,
                                        now = {
                                            Instant.parse(
                                                "2026-11-10T04:45:00Z",
                                            )
                                        },
                                    ),
                            )
                        },
                )
                .use {
                    service ->
                    val body =
                        json.encodeToString(
                            VexLifeProdServiceContract.SendRequest(
                                idempotencyKey = "send.vexlife.browser.streaming.001",
                                content = "Compare A and B",
                            ),
                        )
                    val response =
                        postEventStream(
                            service.origin,
                            VexLifeProdServiceContract.ConversationSendPath,
                            body,
                        )

                    assertEquals(
                        200,
                        response.statusCode(),
                    )
                    assertTrue(
                        response.headers()
                            .firstValue(
                                "content-type",
                            )
                            .orElse(
                                "",
                            )
                            .startsWith(
                                "text/event-stream",
                            ),
                    )
                    val stream =
                        response.body()
                    val checking =
                        stream.indexOf(
                            "\"phase\":\"CheckingRuntime\"",
                        )
                    val generating =
                        stream.indexOf(
                            "\"phase\":\"Generating\"",
                        )
                    val reasoning =
                        stream.indexOf(
                            "\"type\":\"REASONING_DELTA\"",
                        )
                    val content =
                        stream.indexOf(
                            "\"type\":\"CONTENT_DELTA\"",
                        )
                    val completed =
                        stream.indexOf(
                            "\"type\":\"COMPLETED\"",
                        )

                    assertTrue(
                        checking >=
                            0,
                    )
                    assertTrue(
                        generating >
                            checking,
                    )
                    assertTrue(
                        reasoning >
                            generating,
                    )
                    assertTrue(
                        content >
                            reasoning,
                    )
                    assertTrue(
                        completed >
                            content,
                    )
                    assertTrue(
                        stream.contains(
                            "data: [DONE]",
                        ),
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
