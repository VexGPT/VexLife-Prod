package vexlife.service

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.time.DateTimeException
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import vexlife.diagnostics.VexLifeDiagnostics
import vexlife.logging.VexLogger
import vexlife.text.VexLifeStrings
import vexlife.browser.VexLifeProdBrowserContract
import vexlife.browser.VexLifeProdFrameHost
import vexlife.companion.DirectCompanionConnection
import vexlife.companion.DirectCompanionTurn
import vexlife.companion.DirectPromptContext
import vexlife.companion.DirectTurnContextStore
import vexlife.conversation.DirectConversationIdentity
import vexlife.conversation.DirectConversationStore
import vexlife.foundational.VexLifeFoundationalField
import vexlife.local.VexLifeProdLocalSetup
import vexlife.local.VexLifeProdSetupProgress
import vexlife.localasr.VexLifeLocalAsrComposerOutput
import vexlife.localasr.VexLifeLocalAsrComposerOutputStore
import vexlife.localasr.VexLifeLocalAsrModels
import vexlife.localasr.VexLifeLocalAsrModelsFamily
import vexlife.localasr.VexLifeLocalAsrProvider
import vexlife.localasr.VexLifeLocalAsrRuntime
import vexlife.localasr.VexLifeLocalAsrSelection
import vexlife.localasr.VexLifeLocalAsrSelectionStore
import vexlife.localasr.VexLifeLocalAsrSpokenLanguage
import vexlife.localasr.VexLifeLocalAsrSpokenLanguageStore
import vexlife.model.VexLifeModels
import vexlife.memory.VexLifeCurrentMemoryReader
import vexlife.continuity.VexLifePrivateConversation
import vexlife.continuity.VexLifePrivateModeFamily
import vexlife.context.VexLifeContextProfile
import vexlife.context.VexLifeContextProfileFamily
import vexlife.context.VexLifeContextProfileStore
import vexlife.memory.VexLifeDailyMemoryDream
import vexlife.person.VexLifePersonSettings
import vexlife.person.VexLifePersonSettingsStore
import vexlife.memory.VexLifeMemoryContract
import vexlife.release.VexLifeReleaseIdentity
import vexlife.release.VexLifeReleaseInboxFamily
import vexlife.release.VexLifeReleaseUpdates
import vexlife.memory.VexLifeMemoryFamily
import vexlife.model.heretic.HereticRuntimeState
import vexlife.vision.VexLifeProdVisionRuntime
import vexlife.vision.VexLifeVisionFamily
import vexlife.web.VexLifeWebP1RuntimeArtifacts
import vexlife.web.VexWebArtifacts

/**
 * Sustained JVM host for the local VexLife Product realization.
 *
 * Browser/presentation code consumes [VexLifeProdServiceContract]. This service keeps concrete
 * loopback delivery, frame hosting, turn delegation, diagnostics and the live model-switch
 * realization available after the initiating browser caller disappears. Those relationships keep
 * their own truth owners: this service does not own Home, Conversation, Memory, prompt selection,
 * Heretic runtime lifecycle/currentness, source update policy, process lifetime, or presentation
 * semantics.
 */
class VexLifeProdLocalService private constructor(
    val sourceSha: String,
    private val startupSetup: VexLifeProdLocalSetup.Result,
    private val server: HttpServer,
    private val frameHost: VexLifeProdFrameHost,
    private val presentation: VexWebArtifacts.Bundle,
    private val executor: ExecutorService,
    private val turnExecutor: TurnExecutor,
    private val streamingTurnExecutor: StreamingTurnExecutor?,
    private val dreamExecutor: DreamExecutor?,
    private val visionRuntime: VisionRuntime?,
    private val currentStateObserver: (() -> VexLifeProdServiceContract.CurrentStateProjection)?,
    private val memoryReader: (Path) -> VexLifeMemoryContract.Context,
    private val servingModelRef: () -> String?,
    private val servingRuntimeReceiptRef: () -> String,
    private val modelSwitchRealization: VexLifeModels.ModelSwitchRealization?,
    private val releaseAlertsProjection: () -> VexLifeReleaseInboxFamily.DisplayState,
    private val releaseAlertsControl:
        (
            String,
            VexLifeReleaseIdentity.Coordinate,
        ) -> VexLifeReleaseUpdates.ControlResult,
    privateCompletionOverride: VexLifePrivateConversation.Completion? = null,
) : AutoCloseable {
    private val privateConversation = VexLifePrivateConversation(Path.of(startupSetup.productHome))
    private val personSettings = VexLifePersonSettingsStore(Path.of(startupSetup.productHome))
    private val contextProfiles = VexLifeContextProfileStore(Path.of(startupSetup.productHome))
    private val localAsrProductHome =
        Path.of(
            startupSetup.productHome,
        )
    private val localAsrSelection =
        VexLifeLocalAsrSelectionStore(
            localAsrProductHome,
        )
    private val localAsrSpokenLanguage =
        VexLifeLocalAsrSpokenLanguageStore(
            localAsrProductHome,
        )
    private val localAsrComposerOutput =
        VexLifeLocalAsrComposerOutputStore(
            localAsrProductHome,
        )
    private val localAsrModels =
        VexLifeLocalAsrModels(
            productHome =
                localAsrProductHome,
            selectionStore =
                localAsrSelection,
        )
    private val localAsrRuntime =
        VexLifeLocalAsrRuntime()

    private fun localAsrProvider(
        selection: VexLifeLocalAsrSelection.Value,
    ): VexLifeLocalAsrProvider =
        VexLifeLocalAsrProvider.fromHome(
            productHome = localAsrProductHome,
            selection = selection,
            runtime = localAsrRuntime,
        )

    private val diagnostics =
        VexLifeDiagnostics(home = Path.of(startupSetup.productHome), sourceSha = sourceSha).also {
            it.startObserving()
        }

    /** Settings → Models projects the current serving Session; runtime effects stay outside it. */
    private val models =
        VexLifeModels(
            productHome = Path.of(startupSetup.productHome),
            servingModelRef = servingModelRef,
            totalMemoryBytes = {
                runCatching {
                    vexlife.model.heretic.runtime.HereticMacHostProfile.observeCurrent().observation.totalMemoryBytes
                }.getOrNull()
            },
            modelSwitchRealization = modelSwitchRealization,
        )

    private val privateCompletion =
        privateCompletionOverride
            ?: VexLifePrivateConversation.Completion { messages ->
                DirectCompanionConnection
                    .invoke(
                        runtime = startupSetup.qualifiedRuntimeEvidence,
                        messages = messages,
                    )
                    .let { it.content to it.sealedHumanReasoning }
            }

    companion object {
        private const val MaxRequestBytes =
            256 * 1024

        // ~2.5 minutes of 48 kHz mono 16-bit PCM WAV; raw audio remains request-scoped in memory.
        private const val MaxLocalAsrRequestBytes =
            16 * 1024 * 1024

        private val SourceShaPattern =
            Regex(
                "[0-9a-f]{40,64}",
            )

        private val JsonCodec =
            Json {
                encodeDefaults = true
                explicitNulls = true
                ignoreUnknownKeys = false
                isLenient = false
                allowSpecialFloatingPointValues = false
                allowStructuredMapKeys = false
            }

        private const val PrivateIntervalProjectionLimit = 50
        private const val PrivateGapRecentDays = 14L

        private val ProductMemoryReader: (Path) -> VexLifeMemoryContract.Context = {
            VexLifeCurrentMemoryReader.readCurrent(it)
        }

        private val ProductDreamExecutor =
            object : DreamExecutor {
                override fun observe(
                    setup: VexLifeProdLocalSetup.Result,
                ): VexLifeProdServiceContract.MemoryDreamProjection =
                    VexLifeCurrentMemoryReader
                        .readDreamSourceFrontier(
                            Path.of(
                                setup.productHome,
                            ),
                        )
                        .let(
                            ::memoryDreamProjection,
                        )

                override fun perform(
                    setup: VexLifeProdLocalSetup.Result,
                    request: VexLifeProdServiceContract.MemoryDreamRunRequest,
                ): VexLifeDailyMemoryDream.Result {
                    val observedAt =
                        try {
                            Instant.parse(
                                request.observedAt,
                            )
                        } catch (
                            _: DateTimeException,
                        ) {
                            throw IllegalArgumentException(
                                "memory Dream observedAt must be one ISO-8601 instant",
                            )
                        }
                    try {
                        LocalDate.parse(
                            request.calendarDateRef,
                        )
                        ZoneId.of(
                            request.timeZoneRef,
                        )
                    } catch (
                        _: DateTimeException,
                    ) {
                        throw IllegalArgumentException(
                            "memory Dream calendarDateRef/timeZoneRef is invalid",
                        )
                    }

                    return VexLifeDailyMemoryDream.commit(
                        productHome =
                            Path.of(
                                setup.productHome,
                            ),
                        invocation =
                            VexLifeDailyMemoryDream.Invocation(
                                instanceRef =
                                    "instance.vexlife-prod.memory-dream." +
                                        ProcessHandle.current()
                                            .pid(),
                                restInvocationAuthorityRef = request.restInvocationAuthorityRef,
                                dayRef = request.dayRef,
                                dayIndex = request.dayIndex,
                                calendarDateRef = request.calendarDateRef,
                                timeZoneRef = request.timeZoneRef,
                                observedAt = observedAt,
                                expectedConversationHeadSha256 = request.expectedConversationHeadSha256,
                                expectedScoreHeadSha256 = request.expectedScoreHeadSha256,
                                expectedDailyDreamHeadSha256 = request.expectedDailyDreamHeadSha256,
                            ),
                    )
                }
            }

        private fun memoryDreamProjection(
            frontier: VexLifeCurrentMemoryReader.DreamSourceFrontier,
        ): VexLifeProdServiceContract.MemoryDreamProjection {
            val prior = frontier.priorDaily
            return VexLifeProdServiceContract.MemoryDreamProjection(
                schemaVersion = VexLifeProdServiceContract.MemoryDreamProjectionSchema,
                state =
                    if (frontier.scoreSourceConversationHeadSha256 == frontier.conversationHeadSha256) {
                        "SOURCE_FRONTIER_READY"
                    } else {
                        "SCORE_NOT_CAUGHT_UP"
                    },
                sourceConversationHeadSha256 = frontier.conversationHeadSha256,
                sourceScoreHeadSha256 = frontier.scoreHeadSha256,
                scoreSourceConversationHeadSha256 = frontier.scoreSourceConversationHeadSha256,
                sourceSemanticAuthorityHeadSha256 = frontier.semanticAuthorityHeadSha256,
                currentDailyDreamHeadSha256 = prior?.dailyDreamHeadSha256,
                currentDailyStratumSha256 = prior?.dailyStratumSha256,
                currentDayRef = prior?.dayRef,
                currentDayIndex = prior?.dayIndex,
                nextDayIndex = prior?.dayIndex?.plus(1) ?: 0L,
                activeStatementCount = frontier.activeStatementBindings.size,
                heldStatementCount = frontier.heldStatementBindings.size,
                openLoopCount = frontier.openLoopBindings.size,
                effectsPerformed = false,
            )
        }

        /**
         * Ordinary product entry consumes the already-qualified Session at the fixed source-owned
         * loopback coordinate. Each new Send re-observes runtime currentness through the Session;
         * the service does not become a runtime or Conversation owner.
         */
        internal fun start(
            sourceSha: String,
            session: VexLifeProdLocalSetup.Session,
            visionRuntimeExecutablePathOrNull: String?,
        ): VexLifeProdLocalService {
            val visionRuntime =
                visionRuntimeExecutablePathOrNull
                    ?.let {
                        executablePath ->
                        val runtime =
                            VexLifeProdVisionRuntime.open(
                                Path.of(
                                    executablePath,
                                ),
                            )
                        object :
                            VisionRuntime {
                            override fun project(): VexLifeProdServiceContract.VisionSelectedScopeRuntimeResponse =
                                runtime.project()

                            override fun engage(
                                requestRef: String,
                            ): VexLifeProdServiceContract.VisionSelectedScopeRuntimeResponse =
                                runtime.engage(
                                    requestRef,
                                )

                            override fun stop(
                                requestRef: String,
                            ): VexLifeProdServiceContract.VisionSelectedScopeRuntimeResponse =
                                runtime.stop(
                                    requestRef,
                                )

                            override fun close() {
                                runtime.close()
                            }
                        }
                    }

            val sessionRef =
                AtomicReference(
                    session,
                )
            val sessionFence =
                ReentrantReadWriteLock(
                    true,
                )

            fun <T> withCurrentSession(
                block: (VexLifeProdLocalSetup.Session) -> T,
            ): T {
                val read =
                    sessionFence.readLock()
                read.lock()
                return try {
                    block(
                        sessionRef.get(),
                    )
                } finally {
                    read.unlock()
                }
            }

            val modelSwitchRealization =
                VexLifeModels.ModelSwitchRealization {
                    entry,
                    observe,
                    ->
                    observe(
                        VexLifeModels.SwitchPhase.WaitingForCurrentTurn,
                    )
                    val write =
                        sessionFence.writeLock()
                    write.lock()
                    try {
                        observe(
                            VexLifeModels.SwitchPhase.PreparingTarget,
                        )
                        val current =
                            sessionRef.get()
                        val next =
                            VexLifeProdLocalSetup.establishSession(
                                productHome =
                                    Path.of(
                                        current.result.productHome,
                                    ),
                                allowAcquisition = false,
                                allowRuntimeStart = true,
                                progress =
                                    object :
                                        VexLifeProdSetupProgress {
                                        override fun phase(
                                            phase: VexLifeProdSetupProgress.Phase,
                                        ) {
                                            if (
                                                phase ==
                                                    VexLifeProdSetupProgress.Phase.StartingLocalModel
                                            ) {
                                                observe(
                                                    VexLifeModels.SwitchPhase.SwitchingRuntime,
                                                )
                                            }
                                        }

                                        override fun artifactBytes(
                                            completedBytes: Long,
                                            totalBytes: Long,
                                        ) = Unit
                                    },
                            )
                        require(
                            next.result.qualifiedRuntimeEvidence.modelRef ==
                                entry.modelRef,
                        ) {
                            "model switch qualified a runtime other than the selected target"
                        }
                        sessionRef.set(
                            next,
                        )
                    } finally {
                        write.unlock()
                    }
                }

            return startAt(
                sourceSha = sourceSha,
                startupSetup = session.result,
                address =
                    InetSocketAddress(
                        VexLifeProdServiceContract.Host,
                        VexLifeProdServiceContract.Port,
                    ),
                frameAddress =
                    InetSocketAddress(
                        VexLifeProdServiceContract.Host,
                        VexLifeProdBrowserContract.FrameHostPort,
                    ),
                turnExecutor =
                    TurnExecutor {
                        _,
                        intent,
                        ->
                        withCurrentSession {
                            current ->
                            DirectCompanionTurn.perform(
                                session = current,
                                intent = intent,
                            )
                        }
                    },
                streamingTurnExecutor =
                    StreamingTurnExecutor {
                        _,
                        intent,
                        observer,
                        ->
                        withCurrentSession {
                            current ->
                            DirectCompanionTurn.perform(
                                session = current,
                                intent = intent,
                                progressObserver = observer,
                            )
                        }
                    },
                dreamExecutor = ProductDreamExecutor,
                currentStateObserver = {
                    VexLifeProdCurrentState.observeCurrentMac(
                        Path.of(
                            session.result.productHome,
                        ),
                    )
                },
                visionRuntime = visionRuntime,
                servingModelRef = {
                    sessionRef
                        .get()
                        .result
                        .qualifiedRuntimeEvidence
                        .modelRef
                },
                servingRuntimeReceiptRef = {
                    sessionRef
                        .get()
                        .result
                        .qualifiedRuntimeEvidence
                        .runtimeReceiptRef
                },
                modelSwitchRealization = modelSwitchRealization,
                privateCompletion =
                    VexLifePrivateConversation.Completion {
                        messages ->
                        withCurrentSession {
                            current ->
                            DirectCompanionConnection
                                .invoke(
                                    runtime =
                                        current.currentQualifiedRuntimeEvidence(),
                                    messages = messages,
                                )
                                .let {
                                    it.content to it.sealedHumanReasoning
                                }
                        }
                    },
            )
        }

        internal fun startForTest(
            sourceSha: String,
            setup: VexLifeProdLocalSetup.Result,
            turnExecutor: TurnExecutor,
            streamingTurnExecutor: StreamingTurnExecutor? =
                null,
            dreamExecutor: DreamExecutor? =
                null,
            visionRuntime: VisionRuntime? =
                null,
            currentStateObserver: (() -> VexLifeProdServiceContract.CurrentStateProjection)? =
                null,
            memoryReader: ((Path) -> VexLifeMemoryContract.Context)? = null,
            privateCompletion: VexLifePrivateConversation.Completion? = null,
            servingModelRef: (() -> String?)? = null,
            servingRuntimeReceiptRef: (() -> String)? = null,
            modelSwitchRealization: VexLifeModels.ModelSwitchRealization? = null,
            releaseAlertsProjection: (() -> VexLifeReleaseInboxFamily.DisplayState)? = null,
            releaseAlertsControl:
                ((
                    String,
                    VexLifeReleaseIdentity.Coordinate,
                ) -> VexLifeReleaseUpdates.ControlResult)? =
                null,
        ): VexLifeProdLocalService =
            startAt(
                sourceSha = sourceSha,
                startupSetup = setup,
                address =
                    InetSocketAddress(
                        VexLifeProdServiceContract.Host,
                        0,
                    ),
                frameAddress =
                    InetSocketAddress(
                        VexLifeProdServiceContract.Host,
                        0,
                    ),
                turnExecutor = turnExecutor,
                streamingTurnExecutor = streamingTurnExecutor,
                dreamExecutor = dreamExecutor,
                visionRuntime = visionRuntime,
                currentStateObserver = currentStateObserver,
                memoryReader = memoryReader ?: ProductMemoryReader,
                privateCompletion = privateCompletion,
                servingModelRef =
                    servingModelRef
                        ?: {
                            setup.qualifiedRuntimeEvidence.modelRef
                        },
                servingRuntimeReceiptRef =
                    servingRuntimeReceiptRef
                        ?: {
                            setup.qualifiedRuntimeEvidence.runtimeReceiptRef
                        },
                modelSwitchRealization = modelSwitchRealization,
                releaseAlertsProjection = releaseAlertsProjection,
                releaseAlertsControl = releaseAlertsControl,
            )

        private fun startAt(
            sourceSha: String,
            startupSetup: VexLifeProdLocalSetup.Result,
            address: InetSocketAddress,
            frameAddress: InetSocketAddress,
            turnExecutor: TurnExecutor,
            streamingTurnExecutor: StreamingTurnExecutor?,
            dreamExecutor: DreamExecutor?,
            visionRuntime: VisionRuntime?,
            currentStateObserver: (() -> VexLifeProdServiceContract.CurrentStateProjection)?,
            memoryReader: (Path) -> VexLifeMemoryContract.Context = ProductMemoryReader,
            privateCompletion: VexLifePrivateConversation.Completion? = null,
            servingModelRef: () -> String? = {
                startupSetup.qualifiedRuntimeEvidence.modelRef
            },
            servingRuntimeReceiptRef: () -> String = {
                startupSetup.qualifiedRuntimeEvidence.runtimeReceiptRef
            },
            modelSwitchRealization: VexLifeModels.ModelSwitchRealization? = null,
            releaseAlertsProjection: (() -> VexLifeReleaseInboxFamily.DisplayState)? = null,
            releaseAlertsControl:
                ((
                    String,
                    VexLifeReleaseIdentity.Coordinate,
                ) -> VexLifeReleaseUpdates.ControlResult)? =
                null,
        ): VexLifeProdLocalService {
            require(
                SourceShaPattern.matches(
                    sourceSha,
                ),
            ) {
                "VexLife service sourceSha must be one lowercase Git identity"
            }
            require(
                startupSetup.state ==
                    "LOCAL_HERETIC_FOUNDATION_READY" &&
                    startupSetup.qualifiedRuntimeEvidence.runtimeState ==
                    HereticRuntimeState.Qualified,
            ) {
                "VexLife service requires one qualified local runtime foundation"
            }

            val productHome =
                Path.of(
                    startupSetup.productHome,
                )
            require(
                productHome.isAbsolute,
            ) {
                "VexLife service requires one absolute canonical Home"
            }

            // Construction-time validation only. The Conversation store remains the durable owner.
            DirectConversationStore(
                productHome,
            )
                .current()

            val releaseUpdates =
                VexLifeReleaseUpdates(
                    sourceSha = sourceSha,
                    productHome = productHome,
                )
            val realizedReleaseAlertsProjection =
                releaseAlertsProjection
                    ?: {
                        releaseUpdates.projection()
                    }
            val realizedReleaseAlertsControl =
                releaseAlertsControl
                    ?: {
                        controlRef,
                        release,
                        ->
                        releaseUpdates.control(
                            controlRef = controlRef,
                            release = release,
                        )
                    }

            val presentation =
                VexLifeWebP1RuntimeArtifacts.generate(
                    presentationSourceSha = sourceSha,
                )
            val executor =
                Executors.newCachedThreadPool()
            var frameHost: VexLifeProdFrameHost? =
                null
            var server: HttpServer? =
                null

            try {
                frameHost =
                    VexLifeProdFrameHost.startAt(
                        frameAddress,
                    )
                server =
                    HttpServer.create(
                        address,
                        0,
                    )
                val service =
                    VexLifeProdLocalService(
                        sourceSha = sourceSha,
                        startupSetup = startupSetup,
                        server = server,
                        frameHost = frameHost,
                        presentation = presentation,
                        executor = executor,
                        turnExecutor = turnExecutor,
                        streamingTurnExecutor = streamingTurnExecutor,
                        dreamExecutor = dreamExecutor,
                        visionRuntime = visionRuntime,
                        currentStateObserver = currentStateObserver,
                        memoryReader = memoryReader,
                        servingModelRef = servingModelRef,
                        servingRuntimeReceiptRef = servingRuntimeReceiptRef,
                        modelSwitchRealization = modelSwitchRealization,
                        releaseAlertsProjection = realizedReleaseAlertsProjection,
                        releaseAlertsControl = realizedReleaseAlertsControl,
                        privateCompletionOverride = privateCompletion,
                    )

                server.executor =
                    executor
                server.createContext(
                    "/",
                ) {
                    exchange ->
                    service.handle(
                        exchange,
                    )
                }
                server.start()

                return service
            } catch (
                error: Throwable,
            ) {
                server
                    ?.stop(
                        0,
                    )
                frameHost
                    ?.close()
                runCatching {
                    visionRuntime
                        ?.close()
                }
                executor.shutdownNow()
                throw error
            }
        }
    }

    internal fun interface TurnExecutor {
        fun perform(
            setup: VexLifeProdLocalSetup.Result,
            intent: DirectCompanionTurn.Intent,
        ): DirectCompanionTurn.Result
    }

    internal fun interface StreamingTurnExecutor {
        fun perform(
            setup: VexLifeProdLocalSetup.Result,
            intent: DirectCompanionTurn.Intent,
            progressObserver: DirectCompanionTurn.ProgressObserver,
        ): DirectCompanionTurn.Result
    }

    internal interface DreamExecutor {
        fun observe(
            setup: VexLifeProdLocalSetup.Result,
        ): VexLifeProdServiceContract.MemoryDreamProjection

        fun perform(
            setup: VexLifeProdLocalSetup.Result,
            request: VexLifeProdServiceContract.MemoryDreamRunRequest,
        ): VexLifeDailyMemoryDream.Result
    }

    internal interface VisionRuntime :
        AutoCloseable {
        fun project(): VexLifeProdServiceContract.VisionSelectedScopeRuntimeResponse

        fun engage(
            requestRef: String,
        ): VexLifeProdServiceContract.VisionSelectedScopeRuntimeResponse

        fun stop(
            requestRef: String,
        ): VexLifeProdServiceContract.VisionSelectedScopeRuntimeResponse
    }

    val origin: String
        get() =
            "http://" +
                VexLifeProdServiceContract.Host +
                ":" +
                server.address.port

    val port: Int
        get() =
            server.address.port

    val frameOrigin: String
        get() =
            frameHost.origin

    override fun close() {
        runCatching { diagnostics.close() }
        runCatching { models.close() }
        runCatching { localAsrModels.close() }
        runCatching { localAsrRuntime.close() }
        var visionCloseFailure: Throwable? =
            null

        try {
            visionRuntime
                ?.close()
        } catch (
            error: Throwable,
        ) {
            visionCloseFailure =
                error
        }

        server.stop(
            0,
        )
        frameHost.close()
        executor.shutdownNow()

        if (
            visionCloseFailure !=
            null
        ) {
            throw IllegalStateException(
                "VexLife-Prod Vision runtime did not close with the service",
                visionCloseFailure,
            )
        }
    }

    private fun handle(
        exchange: HttpExchange,
    ) {
        try {
            if (
                !requestHostAdmitted(
                    exchange,
                )
            ) {
                respondError(
                    exchange = exchange,
                    status = 421,
                    code = "HOST_NOT_ADMITTED",
                    message = "VexLife local service accepts only its exact loopback Host",
                )
                return
            }

            if (
                !browserEffectRequestAdmitted(
                    exchange,
                )
            ) {
                respondError(
                    exchange = exchange,
                    status = 403,
                    code = "CROSS_ORIGIN_EFFECT_NOT_ADMITTED",
                    message = "cross-origin browser requests may not invoke VexLife local effects",
                )
                return
            }

            val stalePresentationDetail =
                browserPresentationCurrentnessFailure(
                    exchange,
                )
            if (
                stalePresentationDetail !=
                null
            ) {
                respondError(
                    exchange = exchange,
                    status = 409,
                    code = VexLifeProdServiceContract.StalePresentationCode,
                    message = stalePresentationDetail,
                )
                return
            }

            if (
                exchange.requestURI.rawQuery !=
                null
            ) {
                respondError(
                    exchange = exchange,
                    status = 400,
                    code = "QUERY_NOT_ADMITTED",
                    message = "VexLife local service routes do not accept query parameters",
                )
                return
            }

            when (
                exchange.requestURI.path
            ) {
                VexLifeProdServiceContract.PresentationIndexPath ->
                    handlePresentation(
                        exchange = exchange,
                        artifactPath = "index.html",
                        contentType = "text/html; charset=utf-8",
                    )

                VexLifeProdServiceContract.PresentationCssPath ->
                    handlePresentation(
                        exchange = exchange,
                        artifactPath = "vexweb.css",
                        contentType = "text/css; charset=utf-8",
                    )

                VexLifeProdServiceContract.PresentationJavascriptPath ->
                    handlePresentation(
                        exchange = exchange,
                        artifactPath = "vexweb.js",
                        contentType = "text/javascript; charset=utf-8",
                    )

                VexLifeProdServiceContract.PresentationManifestPath ->
                    handlePresentation(
                        exchange = exchange,
                        artifactPath = "vexweb-manifest.json",
                        contentType = "application/json; charset=utf-8",
                    )

                VexLifeProdServiceContract.SourceCurrentnessPath ->
                    handleSourceCurrentness(
                        exchange,
                    )

                VexLifeProdServiceContract.CurrentStatePath ->
                    handleCurrentState(
                        exchange,
                    )

                VexLifeProdServiceContract.ConversationPath ->
                    handleConversation(
                        exchange,
                    )

                VexLifeProdServiceContract.ContextPath ->
                    handleContext(
                        exchange,
                    )

                VexLifeProdServiceContract.ContextProfilesPath ->
                    handleContextProfiles(
                        exchange,
                    )

                VexLifeProdServiceContract.ContextProfilesControlPath ->
                    handleContextProfileControl(
                        exchange,
                    )

                VexLifeProdServiceContract.ContextProfileHomeRevealPath ->
                    handleContextProfileHomeReveal(
                        exchange,
                    )

                VexLifeProdServiceContract.ContextProfileExportPath ->
                    handleContextProfileExport(
                        exchange,
                    )

                VexLifeProdServiceContract.MemoryPath -> handleMemory(exchange)

                VexLifeProdServiceContract.ReleaseAlertsPath -> {
                    requireMethod(exchange, "GET")
                    respondJson(
                        exchange = exchange,
                        status = 200,
                        value =
                            VexLifeProdServiceContract.ReleaseAlertsProjection(
                                schemaVersion =
                                    VexLifeProdServiceContract.ReleaseAlertsProjectionSchema,
                                display = releaseAlertsProjection(),
                                effectsPerformed = false,
                            ),
                    )
                }

                VexLifeProdServiceContract.ReleaseAlertsControlPath ->
                    handleReleaseAlertsControl(
                        exchange,
                    )

                VexLifeProdServiceContract.FoundationalFieldPath -> handleFoundationalField(exchange)

                VexLifeProdServiceContract.SettingsPath -> handleSettings(exchange)

                VexLifeProdServiceContract.LocalAsrPath ->
                    handleLocalAsr(
                        exchange,
                    )

                VexLifeProdServiceContract.LocalAsrSpokenLanguagePath ->
                    handleLocalAsrSpokenLanguage(
                        exchange,
                    )

                VexLifeProdServiceContract.LocalAsrComposerOutputPath ->
                    handleLocalAsrComposerOutput(
                        exchange,
                    )

                VexLifeProdServiceContract.LocalAsrModelControlPath ->
                    handleLocalAsrModelControl(
                        exchange,
                    )

                VexLifeProdServiceContract.LocalAsrPreparePath ->
                    handleLocalAsrPrepare(
                        exchange,
                    )

                VexLifeProdServiceContract.LocalAsrTranscribePath ->
                    handleLocalAsrTranscribe(
                        exchange,
                    )

                VexLifeProdServiceContract.DiagnosticsPath -> {
                    requireMethod(exchange, "GET")
                    respondJson(exchange = exchange, status = 200, value = diagnostics.projection())
                }

                VexLifeProdServiceContract.DiagnosticsControlPath -> handleDiagnosticsControl(exchange)

                VexLifeProdServiceContract.ModelsPath -> {
                    requireMethod(exchange, "GET")
                    respondJson(exchange = exchange, status = 200, value = models.projection())
                }

                VexLifeProdServiceContract.ModelsControlPath -> {
                    requireMethod(exchange, "POST")
                    requireJson(exchange)
                    val request = decodeRequest<ModelControlRequest>(exchange)
                    respondJson(
                        exchange = exchange,
                        status = 200,
                        value =
                            models.control(
                                controlRef = request.controlRef,
                                handle = request.handle,
                                promptCacheEntries = request.promptCacheEntries,
                            ),
                    )
                }

                VexLifeProdServiceContract.ModelsImportCheckPath -> {
                    requireMethod(exchange, "POST")
                    requireJson(exchange)
                    val request = decodeRequest<ModelImportCheckRequest>(exchange)
                    respondJson(exchange = exchange, status = 200, value = models.checkImport(request.sourceUrl))
                }

                VexLifeProdServiceContract.ModelsImportAddPath -> {
                    requireMethod(exchange, "POST")
                    requireJson(exchange)
                    val request = decodeRequest<ModelImportAddRequest>(exchange)
                    respondJson(exchange = exchange, status = 200, value = models.addCheckedImport(request.importRef))
                }

                VexLifeProdServiceContract.DiagnosticsExportPath -> handleDiagnosticsExport(exchange)

                VexLifeProdServiceContract.ChronologyPath -> handlePresentationFact(exchange)

                VexLifeProdServiceContract.WorkspaceStringsPath -> handleStrings(exchange, scope = "workspace")

                VexLifeProdServiceContract.ReleaseAlertsStringsPath ->
                    handleStrings(
                        exchange,
                        scope = "release-alerts",
                    )

                VexLifeProdServiceContract.PrivatePath -> handlePrivate(exchange)

                VexLifeProdServiceContract.PrivateControlPath -> handlePrivateControl(exchange)

                VexLifeProdServiceContract.PrivateSendPath -> handlePrivateSend(exchange)

                VexLifeProdServiceContract.PrivateNoteProposePath -> handlePrivateNotePropose(exchange)

                VexLifeProdServiceContract.PrivateNoteKeepPath -> handlePrivateNoteKeep(exchange)

                VexLifeProdServiceContract.PrivateNoteDeclinePath -> handlePrivateNoteDecline(exchange)

                VexLifeProdServiceContract.MemoryDreamPath ->
                    handleMemoryDream(
                        exchange,
                    )

                VexLifeProdServiceContract.VisionPath ->
                    handleVision(
                        exchange,
                    )

                VexLifeProdServiceContract.VisionScopePath ->
                    handleVisionScope(
                        exchange,
                    )

                VexLifeProdServiceContract.VisionControlPath -> handleVisionControl(exchange)

                VexLifeProdServiceContract.VisionScopeEngagePath ->
                    handleVisionScopeEngage(
                        exchange,
                    )

                VexLifeProdServiceContract.VisionScopeStopPath ->
                    handleVisionScopeStop(
                        exchange,
                    )

                VexLifeProdServiceContract.VisionLookNowPath ->
                    handleVisionLookNow(
                        exchange,
                    )

                VexLifeProdServiceContract.ConversationSendPath ->
                    handleSend(
                        exchange,
                    )

                else ->
                    respondError(
                        exchange = exchange,
                        status = 404,
                        code = "ROUTE_NOT_FOUND",
                        message = "VexLife local service route not found",
                    )
            }
        } catch (
            error: DirectCompanionTurn.TurnException,
        ) {
            respondJson(
                exchange = exchange,
                status = 409,
                value =
                    VexLifeProdServiceContract.ErrorResponse(
                        schemaVersion = VexLifeProdServiceContract.ErrorSchema,
                        code = error.failureCode,
                        message = "direct Companion turn failed",
                        retrySameSendAttemptAllowed = error.retrySameSendAttemptAllowed,
                        exactNextSafeRoute = error.exactNextSafeRoute,
                    ),
            )
        } catch (
            error: RequestJsonInvalid,
        ) {
            respondError(
                exchange = exchange,
                status = 400,
                code = "REQUEST_JSON_INVALID",
                message = "request JSON does not match the current service contract",
            )
        } catch (
            error: SerializationException,
        ) {
            respondError(
                exchange = exchange,
                status = 500,
                code = "PRODUCT_CONTRACT_SERIALIZATION_FAILED",
                message = "VexLife could not read or project one Product contract",
            )
        } catch (
            error: IllegalArgumentException,
        ) {
            respondError(
                exchange = exchange,
                status = 400,
                code = "REQUEST_NOT_ADMITTED",
                message = error.message
                    ?: "request is not admitted",
            )
        } catch (
            _: Throwable,
        ) {
            respondError(
                exchange = exchange,
                status = 500,
                code = "SERVICE_FAILURE",
                message = "VexLife local service failed without a completed product result",
            )
        } finally {
            exchange.close()
        }
    }

    private fun handleSourceCurrentness(
        exchange: HttpExchange,
    ) {
        requireMethod(
            exchange,
            "GET",
        )

        val conversation =
            conversationProjection()

        respondJson(
            exchange = exchange,
            status = 200,
            value =
                VexLifeProdServiceContract.SourceCurrentness(
                    schemaVersion = VexLifeProdServiceContract.SourceCurrentnessSchema,
                    serviceRef = VexLifeProdServiceContract.ServiceRef,
                    productRef = VexLifeProdServiceContract.ProductRef,
                    serviceState = "SOURCE_BOUND_LISTENING",
                    sourceSha = sourceSha,
                    processPid = ProcessHandle.current().pid(),
                    frameHostOrigin = frameOrigin,
                    runtimeCurrentness = "NOT_REOBSERVED_BY_SERVICE",
                    // Wire name retained for v3 compatibility; after an in-process model switch
                    // this is the runtime receipt bound to the service's current Session.
                    startupQualifiedRuntimeReceiptRef =
                        servingRuntimeReceiptRef(),
                    currentConversationHeadSha256 =
                        conversation.currentConversationHeadSha256,
                    effectsPerformed = false,
                ),
        )
    }

    private fun handleCurrentState(
        exchange: HttpExchange,
    ) {
        requireMethod(
            exchange,
            "GET",
        )

        respondJson(
            exchange = exchange,
            status = 200,
            value =
                requireNotNull(
                    currentStateObserver,
                ) {
                    "current-state observer is not configured"
                }
                    .invoke(),
        )
    }

    private fun handlePresentation(
        exchange: HttpExchange,
        artifactPath: String,
        contentType: String,
    ) {
        requireMethod(
            exchange,
            "GET",
        )

        val bytes =
            presentation
                .content(
                    artifactPath,
                )
                .toByteArray(
                    StandardCharsets.UTF_8,
                )

        exchange.responseHeaders.set(
            "content-type",
            contentType,
        )
        exchange.responseHeaders.set(
            "cache-control",
            "no-store",
        )
        exchange.sendResponseHeaders(
            200,
            bytes.size
                .toLong(),
        )
        exchange.responseBody.write(
            bytes,
        )
    }

    private fun handleReleaseAlertsControl(
        exchange: HttpExchange,
    ) {
        requireMethod(
            exchange,
            "POST",
        )
        requireJson(
            exchange,
        )
        val request =
            decodeRequest<VexLifeProdServiceContract.ReleaseAlertsControlRequest>(exchange)
        val result =
            releaseAlertsControl(
                request.controlRef,
                request.release,
            )

        respondJson(
            exchange = exchange,
            status = 200,
            value =
                VexLifeProdServiceContract.ReleaseAlertsControlResult(
                    schemaVersion =
                        VexLifeProdServiceContract.ReleaseAlertsControlResultSchema,
                    controlRef = request.controlRef,
                    outcome =
                        when (
                            result.outcome
                        ) {
                            VexLifeReleaseUpdates.ControlOutcome.Realized ->
                                VexLifeProdServiceContract.ReleaseAlertsControlRealized

                            VexLifeReleaseUpdates.ControlOutcome.NotLawfulNow ->
                                VexLifeProdServiceContract.ReleaseAlertsControlNotLawfulNow
                        },
                    releaseAlerts =
                        VexLifeProdServiceContract.ReleaseAlertsProjection(
                            schemaVersion =
                                VexLifeProdServiceContract.ReleaseAlertsProjectionSchema,
                            display = result.display,
                            effectsPerformed = false,
                        ),
                    pageToOpenOrNull =
                        result.pageToOpenOrNull
                            ?.value,
                ),
        )
    }

    private fun handleVision(exchange: HttpExchange) {
        requireMethod(exchange, "GET")
        respondJson(exchange = exchange, status = 200, value = observeVision())
    }

    /**
     * Fresh no-effect Vision observation: Product route reachability + the runtime's content-absent
     * selected-scope PROJECT, reconciled by [VexLifeVisionFamily.observe]. A PROJECT that fails or
     * cannot be read becomes a held State, never a guessed one.
     */
    private fun observedVisionState(): VexLifeVisionFamily.State {
        val runtime = visionRuntime
        val projected = runtime?.let { runCatching { it.project() }.getOrNull() }
        return VexLifeVisionFamily.observe(
            routeReachable = runtime != null,
            runtimeReadable = projected?.ok == true,
            scopeState = projected?.projectionOrNull?.state,
        )
    }

    private fun observeVision(): VexLifeProdServiceContract.VisionProjection {
        val state = observedVisionState()
        return VexLifeProdServiceContract.VisionProjection(
            schemaVersion = VexLifeProdServiceContract.VisionProjectionSchema,
            state = state.stateRef,
            routeReachable = visionRuntime != null,
            display = VexLifeVisionFamily.displayStateFor(state),
            effectsPerformed = false,
        )
    }

    /**
     * Same activation shape as Product entry: re-observe, let the Family decide whether the Control
     * is lawful now, realize at most one runtime command, observe again.
     */
    private fun handleVisionControl(exchange: HttpExchange) {
        requireMethod(exchange, "POST")
        requireJson(exchange)
        val request =
            decodeRequest<VexLifeProdServiceContract.VisionControlRequest>(exchange)
        val control = requireNotNull(VexLifeVisionFamily.controlForRef(request.controlRef))
        val runtime = visionRuntime
        val act = VexLifeVisionFamily.resolve(currentState = observedVisionState(), control = control)

        fun result(outcome: String, errorCode: String? = null) =
            VexLifeProdServiceContract.VisionControlResult(
                schemaVersion = VexLifeProdServiceContract.VisionControlResultSchema,
                requestRef = request.requestRef,
                controlRef = control.controlRef,
                outcome = outcome,
                runtimeErrorCodeOrNull = errorCode,
                vision = observeVision(),
            )

        if (act == null || runtime == null) {
            respondJson(
                exchange = exchange,
                status = 409,
                value = result(VexLifeProdServiceContract.VisionControlNotLawfulNow),
            )
            return
        }
        val response =
            when (act.effect) {
                VexLifeVisionFamily.Effect.EngageScope -> runtime.engage(request.requestRef)
                VexLifeVisionFamily.Effect.StopScope -> runtime.stop(request.requestRef)
                null -> null
            }
        check(response == null || response.commandRef == request.requestRef) {
            "Vision runtime result does not match the exact Product request"
        }
        if (response != null && !response.ok) {
            respondJson(
                exchange = exchange,
                status = 200,
                value =
                    result(
                        VexLifeProdServiceContract.VisionControlRuntimeRefused,
                        requireNotNull(response.errorCodeOrNull),
                    ),
            )
            return
        }
        respondJson(
            exchange = exchange,
            status = 200,
            value = result(VexLifeProdServiceContract.VisionControlRealized),
        )
    }

    private fun handleVisionScope(
        exchange: HttpExchange,
    ) {
        requireMethod(
            exchange,
            "GET",
        )

        val currentVisionRuntime =
            visionRuntime

        if (
            currentVisionRuntime ==
                null
        ) {
            respondError(
                exchange = exchange,
                status = 409,
                code = "VISION_HELD",
                message = "VexLife Vision persistent selected-scope route is not reachable",
            )
            return
        }

        respondJson(
            exchange = exchange,
            status = 200,
            value =
                currentVisionRuntime.project(),
        )
    }

    private fun handleVisionScopeEngage(
        exchange: HttpExchange,
    ) {
        requireMethod(
            exchange,
            "POST",
        )
        requireJson(
            exchange,
        )

        val currentVisionRuntime =
            visionRuntime

        if (
            currentVisionRuntime ==
                null
        ) {
            respondError(
                exchange = exchange,
                status = 409,
                code = "VISION_HELD",
                message = "VexLife Vision persistent selected-scope route is not reachable",
            )
            return
        }

        val request =
            decodeRequest<VexLifeProdServiceContract.VisionScopeCommandRequest>(exchange)
        val result =
            currentVisionRuntime.engage(
                request.requestRef,
            )

        check(
            result.commandRef ==
                request.requestRef &&
                result.commandType ==
                "ENGAGE",
        ) {
            "Vision ENGAGE result does not match the exact Product request"
        }

        respondJson(
            exchange = exchange,
            status = 200,
            value = result,
        )
    }

    private fun handleVisionScopeStop(
        exchange: HttpExchange,
    ) {
        requireMethod(
            exchange,
            "POST",
        )
        requireJson(
            exchange,
        )

        val currentVisionRuntime =
            visionRuntime

        if (
            currentVisionRuntime ==
                null
        ) {
            respondError(
                exchange = exchange,
                status = 409,
                code = "VISION_HELD",
                message = "VexLife Vision persistent selected-scope route is not reachable",
            )
            return
        }

        val request =
            decodeRequest<VexLifeProdServiceContract.VisionScopeCommandRequest>(exchange)
        val result =
            currentVisionRuntime.stop(
                request.requestRef,
            )

        check(
            result.commandRef ==
                request.requestRef &&
                result.commandType ==
                "STOP",
        ) {
            "Vision STOP result does not match the exact Product request"
        }

        respondJson(
            exchange = exchange,
            status = 200,
            value = result,
        )
    }

    private fun handleVisionLookNow(
        exchange: HttpExchange,
    ) {
        requireMethod(
            exchange,
            "POST",
        )
        requireJson(
            exchange,
        )

        decodeRequest<VexLifeProdServiceContract.VisionLookNowRequest>(exchange)

        respondError(
            exchange = exchange,
            status = 409,
            code = "VISION_LOOK_NOT_YET_ADMITTED_FROM_PERSISTENT_SCOPE",
            message = "VexLife Vision LOOK remains held until the persistent-scope LOOK executor is accepted",
        )
    }

    private fun handleConversation(
        exchange: HttpExchange,
    ) {
        requireMethod(
            exchange,
            "GET",
        )

        respondJson(
            exchange = exchange,
            status = 200,
            value =
                conversationProjection(),
        )
    }

    private fun handleContext(
        exchange: HttpExchange,
    ) {
        requireMethod(
            exchange,
            "GET",
        )

        respondJson(
            exchange = exchange,
            status = 200,
            value =
                contextProjection(),
        )
    }

    private fun productFoundationIdentity(): VexLifeContextProfile.ProductFoundationIdentity {
        val reading =
            VexLifeFoundationalField.readCurrent()
        val current =
            reading as? VexLifeFoundationalField.Reading.Current
        return VexLifeContextProfile.ProductFoundationIdentity(
            version = current?.body?.version,
            bodySha256 = current?.bodySha256,
        )
    }

    private fun contextProfilesProjection(
        effectsPerformed: Boolean = false,
    ): VexLifeProdServiceContract.ContextProfilesProjection {
        val productFoundation =
            productFoundationIdentity()
        val active =
            contextProfiles.readActive(
                productFoundation,
            )
        val state =
            VexLifeContextProfileFamily.stateFor(
                active,
            )
        return VexLifeProdServiceContract.ContextProfilesProjection(
            schemaVersion =
                VexLifeProdServiceContract.ContextProfilesProjectionSchema,
            active = active,
            saved =
                contextProfiles.listSaved(
                    productFoundation,
                ),
            display =
                VexLifeContextProfileFamily.displayStateFor(
                    state,
                ),
            productFoundation = productFoundation,
            homeRelativePath =
                VexLifeContextProfileStore.RootRelativePath,
            effectsPerformed = effectsPerformed,
        )
    }

    private fun handleContextProfiles(
        exchange: HttpExchange,
    ) {
        requireMethod(
            exchange,
            "GET",
        )
        respondJson(
            exchange = exchange,
            status = 200,
            value = contextProfilesProjection(),
        )
    }

    private fun handleContextProfileControl(
        exchange: HttpExchange,
    ) {
        requireMethod(
            exchange,
            "POST",
        )
        requireJson(
            exchange,
        )
        val request =
            JsonCodec.decodeFromString<VexLifeProdServiceContract.ContextProfileControlRequest>(
                requestText(
                    exchange,
                ),
            )
        val productFoundation =
            productFoundationIdentity()
        val before =
            contextProfiles.readActive(
                productFoundation,
            )
        val currentState =
            VexLifeContextProfileFamily.stateFor(
                before,
            )
        val control =
            when (
                request.controlRef
            ) {
                VexLifeContextProfileFamily.ControlUse ->
                    VexLifeContextProfileFamily.Use(
                        profileRef =
                            requireNotNull(
                                request.profileRef,
                            ) {
                                "using a saved Context Profile requires its profile ref"
                            },
                    )

                VexLifeContextProfileFamily.ControlSave ->
                    VexLifeContextProfileFamily.Save(
                        profileRef =
                            request.profileRef
                                ?: before
                                    .takeUnless {
                                        it.productDefault
                                    }
                                    ?.profileRef,
                        definition =
                            requireNotNull(
                                request.definition,
                            ) {
                                "saving a Context Profile requires its definition"
                            },
                    )

                VexLifeContextProfileFamily.ControlSaveAs ->
                    VexLifeContextProfileFamily.SaveAs(
                        definition =
                            requireNotNull(
                                request.definition,
                            ) {
                                "saving a new Context Profile requires its definition"
                            },
                    )

                VexLifeContextProfileFamily.ControlRestoreDefault ->
                    VexLifeContextProfileFamily.RestoreProductDefault

                VexLifeContextProfileFamily.ControlImport ->
                    VexLifeContextProfileFamily.Import(
                        profile =
                            requireNotNull(
                                request.portable,
                            ) {
                                "importing a Context Profile requires its portable configuration"
                            },
                    )


                else ->
                    throw IllegalArgumentException(
                        "unknown Context Profile Control",
                    )
            }
        val act =
            VexLifeContextProfileFamily.resolve(
                currentState = currentState,
                control = control,
            )
        if (
            act ==
                null
        ) {
            respondJson(
                exchange = exchange,
                status = 200,
                value =
                    VexLifeProdServiceContract.ContextProfileControlResult(
                        schemaVersion =
                            VexLifeProdServiceContract.ContextProfileControlResultSchema,
                        controlRef = request.controlRef,
                        outcome = "NOT_LAWFUL_NOW",
                        effectPerformed = false,
                        profiles = contextProfilesProjection(),
                    ),
            )
            return
        }

        var effectPerformed =
            false
        var outcome =
            "REALIZED"
        when (
            val effect = act.effect
        ) {
            is VexLifeContextProfileFamily.Effect.SelectSaved -> {
                contextProfiles.selectSaved(
                    profileRef = effect.profileRef,
                    productFoundation = productFoundation,
                )
                effectPerformed =
                    true
            }

            is VexLifeContextProfileFamily.Effect.Persist -> {
                val profileRef =
                    requireNotNull(
                        effect.profileRef,
                    ) {
                        "saving the active Product default requires Save as"
                    }
                contextProfiles.save(
                    profileRef = profileRef,
                    definition = effect.definition,
                    productFoundation = productFoundation,
                )
                effectPerformed =
                    true
            }

            is VexLifeContextProfileFamily.Effect.PersistAsNew -> {
                contextProfiles.save(
                    profileRef = null,
                    definition = effect.definition,
                    productFoundation = productFoundation,
                )
                effectPerformed =
                    true
            }

            VexLifeContextProfileFamily.Effect.SelectProductDefault -> {
                contextProfiles.restoreProductDefault(
                    productFoundation,
                )
                effectPerformed =
                    true
            }

            is VexLifeContextProfileFamily.Effect.ImportAsNew -> {
                contextProfiles.importAsNew(
                    portable = effect.profile,
                    productFoundation = productFoundation,
                )
                effectPerformed =
                    true
            }

            null ->
                Unit
        }

        respondJson(
            exchange = exchange,
            status = 200,
            value =
                VexLifeProdServiceContract.ContextProfileControlResult(
                    schemaVersion =
                        VexLifeProdServiceContract.ContextProfileControlResultSchema,
                    controlRef = request.controlRef,
                    outcome = outcome,
                    effectPerformed = effectPerformed,
                    profiles =
                        contextProfilesProjection(
                            effectsPerformed = effectPerformed,
                        ),
                ),
        )
    }

    private fun handleContextProfileHomeReveal(
        exchange: HttpExchange,
    ) {
        requireMethod(
            exchange,
            "POST",
        )
        val directory =
            contextProfiles.ensureDirectory()
        val performed =
            runCatching {
                val osName =
                    System.getProperty(
                        "os.name",
                        "",
                    )
                        .lowercase()
                val command =
                    when {
                        osName.contains("mac") ->
                            listOf(
                                "/usr/bin/open",
                                directory.toString(),
                            )

                        osName.contains("win") ->
                            listOf(
                                "explorer.exe",
                                directory.toString(),
                            )

                        osName.contains("linux") ->
                            listOf(
                                "xdg-open",
                                directory.toString(),
                            )

                        else ->
                            error(
                                "this host has no admitted Vex Home file-manager command",
                            )
                    }
                ProcessBuilder(
                    command,
                )
                    .start()
                true
            }
                .getOrElse {
                    false
                }
        respondJson(
            exchange = exchange,
            status = 200,
            value =
                VexLifeProdServiceContract.ContextProfileHomeRevealResult(
                    schemaVersion =
                        VexLifeProdServiceContract.ContextProfileHomeRevealResultSchema,
                    outcome =
                        if (performed) {
                            "REALIZED"
                        } else {
                            "HELD_HOST_FILE_MANAGER_UNAVAILABLE"
                        },
                    effectPerformed = performed,
                    homeRelativePath =
                        VexLifeContextProfileStore.RootRelativePath,
                ),
        )
    }

    private fun handleContextProfileExport(
        exchange: HttpExchange,
    ) {
        requireMethod(
            exchange,
            "GET",
        )
        val active =
            contextProfiles.readActive(
                productFoundationIdentity(),
            )
        val portable =
            contextProfiles.portable(
                active,
            )
        val bytes =
            (
                JsonCodec.encodeToString(
                    portable,
                ) +
                    "\n"
                )
                .toByteArray(
                    StandardCharsets.UTF_8,
                )
        val filename =
            active.definition.name
                .lowercase()
                .replace(
                    Regex("[^a-z0-9]+"),
                    "-",
                )
                .trim('-')
                .ifBlank {
                    "vex-context"
                } +
                ".vexcontext.json"
        exchange.responseHeaders.set(
            "content-type",
            "application/json; charset=utf-8",
        )
        exchange.responseHeaders.set(
            "content-disposition",
            "attachment; filename=\"$filename\"",
        )
        exchange.responseHeaders.set(
            "cache-control",
            "no-store",
        )
        exchange.sendResponseHeaders(
            200,
            bytes.size.toLong(),
        )
        exchange.responseBody.write(
            bytes,
        )
    }

    private fun settingsProjection(): VexLifeProdServiceContract.SettingsProjection =
        VexLifeProdServiceContract.SettingsProjection(
            schemaVersion = VexLifeProdServiceContract.SettingsSchema,
            replyLanguage =
                VexLifePersonSettings.canonicalReplyLanguage(
                    personSettings.read().replyLanguage,
                ),
            languages =
                VexLifePersonSettings.ReplyLanguages.map {
                    VexLifeProdServiceContract.SettingsLanguage(
                        it.first,
                        it.second,
                    )
                },
        )

    @kotlinx.serialization.Serializable
    private data class DiagnosticsControlRequest(val controlRef: String)

    @kotlinx.serialization.Serializable
    private data class ModelControlRequest(
        val controlRef: String,
        val handle: String,
        val promptCacheEntries: Int? = null,
    )

    @kotlinx.serialization.Serializable
    private data class ModelImportCheckRequest(val sourceUrl: String)

    @kotlinx.serialization.Serializable
    private data class ModelImportAddRequest(val importRef: String)

    private fun handleDiagnosticsControl(exchange: HttpExchange) {
        requireMethod(exchange, "POST")
        requireJson(exchange)
        val request = decodeRequest<DiagnosticsControlRequest>(exchange)
        respondJson(exchange = exchange, status = 200, value = diagnostics.control(request.controlRef))
    }

    private fun handleDiagnosticsExport(exchange: HttpExchange) {
        requireMethod(exchange, "GET")
        val bytes = diagnostics.export()
        val stamp = java.time.Instant.now().toString().replace(":", "").substringBefore('.')
        exchange.responseHeaders.set("content-type", "application/zip")
        exchange.responseHeaders.set("content-disposition", "attachment; filename=\"vexlife-debug-$stamp.zip\"")
        exchange.responseHeaders.set("cache-control", "no-store")
        exchange.sendResponseHeaders(200, bytes.size.toLong())
        exchange.responseBody.write(bytes)
    }

    @kotlinx.serialization.Serializable
    private data class PresentationFact(val fact: String, val refs: Map<String, String> = emptyMap())

    /**
     * Facts the web presentation itself observed: what it actually showed and what it could prove
     * about the visible page. Only Browser and Presentation facts are admitted from it; the
     * presentation cannot report a Conversation, runtime or setup fact it does not own.
     */
    private fun handlePresentationFact(exchange: HttpExchange) {
        requireMethod(exchange, "POST")
        requireJson(exchange)
        val reported = decodeRequest<PresentationFact>(exchange)
        val fact = VexLogger.Fact.fromWire(reported.fact)
        if (
            fact == null ||
                fact.owner !in setOf(VexLogger.Owner.Browser, VexLogger.Owner.Presentation) ||
                reported.refs.size > 12
        ) {
            respondError(exchange, 422, "FACT_NOT_ADMITTED", "the presentation may report only Browser or Presentation facts")
            return
        }
        VexLogger.record(fact, reported.refs + ("reportedBy" to "web-presentation"))
        exchange.sendResponseHeaders(204, -1)
    }

    @kotlinx.serialization.Serializable
    private data class StringsProjection(
        val schemaVersion: String,
        val scope: String,
        val language: String,
        val strings: Map<String, String>,
    )

    /**
     * The keyed words of one scope in the person's language when that scope is complete in it,
     * otherwise English: a partially translated scope is never shown mixed (docs/foundation/
     * LOCALIZATION.md, "A language is offered only when complete").
     */
    private fun handleStrings(exchange: HttpExchange, scope: String) {
        requireMethod(exchange, "GET")
        require(scope in VexLifeStrings.Scopes) { "unknown string scope" }
        val requested =
            VexLifePersonSettings.localizationLanguageForReply(
                runCatching {
                    personSettings.read().replyLanguage
                }
                    .getOrDefault(
                        VexLifePersonSettings.DefaultLanguage,
                    ),
            )
        val language = VexLifeStrings.selectLanguage(scope, requested)
        val catalog = VexLifeStrings.catalog
        respondJson(
            exchange = exchange,
            status = 200,
            value =
                StringsProjection(
                    schemaVersion = "vexlife-prod.strings/v1",
                    scope = scope,
                    language = language,
                    strings = catalog.keys.filter { it.startsWith("$scope.") }.sorted().associateWith { catalog.text(it, language) },
                ),
        )
    }

    private fun handleSettings(exchange: HttpExchange) {
        when (exchange.requestMethod) {
            "GET" -> respondJson(exchange = exchange, status = 200, value = settingsProjection())
            "POST" -> {
                requireJson(exchange)
                val update = decodeRequest<VexLifeProdServiceContract.SettingsUpdate>(exchange)
                personSettings.write(VexLifePersonSettings.Value(VexLifePersonSettings.Schema, update.replyLanguage))
                respondJson(exchange = exchange, status = 200, value = settingsProjection())
            }
            else -> respondError(exchange, 405, "METHOD_NOT_ALLOWED", "settings accepts GET or POST")
        }
    }

    private fun localAsrProjection(): VexLifeProdServiceContract.LocalAsrProjection {
        val selected =
            localAsrSelection.read()
        val selectedOption =
            VexLifeLocalAsrSelection.option(
                selected,
            )
        val spokenLanguage =
            localAsrSpokenLanguage.read()
        val composerOutput =
            localAsrComposerOutput.read()
        val replyLanguage =
            personSettings.read()
                .replyLanguage
        val resolvedSpokenLanguage =
            VexLifeLocalAsrSpokenLanguage.resolvedLanguageCode(
                value = spokenLanguage,
                replyLanguage = replyLanguage,
            )

        fun supports(
            mode: String,
            languageCode: String?,
        ): Boolean =
            when (mode) {
                VexLifeLocalAsrSpokenLanguage.FollowReplyLanguage ->
                    VexLifePersonSettings.spokenLanguageCodeForReply(
                        replyLanguage,
                    ) in
                        selectedOption.languageCodes

                VexLifeLocalAsrSpokenLanguage.AutoDetect ->
                    selectedOption.languageCodes.size >
                        1

                VexLifeLocalAsrSpokenLanguage.Fixed ->
                    languageCode in
                        selectedOption.languageCodes

                else ->
                    false
            }

        val spokenLanguageChoices =
            buildList {
                add(
                    VexLifeProdServiceContract.LocalAsrSpokenLanguageChoice(
                        mode =
                            VexLifeLocalAsrSpokenLanguage.FollowReplyLanguage,
                        languageCode =
                            null,
                        labelKey =
                            "workspace.label.local-asr-spoken-language-follow",
                        languageLabel =
                            VexLifePersonSettings.replyLanguageName(
                                replyLanguage,
                            ),
                        selected =
                            spokenLanguage.mode ==
                            VexLifeLocalAsrSpokenLanguage.FollowReplyLanguage,
                        supportedBySelectedModel =
                            supports(
                                VexLifeLocalAsrSpokenLanguage.FollowReplyLanguage,
                                null,
                            ),
                    ),
                )
                add(
                    VexLifeProdServiceContract.LocalAsrSpokenLanguageChoice(
                        mode =
                            VexLifeLocalAsrSpokenLanguage.AutoDetect,
                        languageCode =
                            null,
                        labelKey =
                            "workspace.label.local-asr-spoken-language-auto",
                        languageLabel =
                            null,
                        selected =
                            spokenLanguage.mode ==
                            VexLifeLocalAsrSpokenLanguage.AutoDetect,
                        supportedBySelectedModel =
                            supports(
                                VexLifeLocalAsrSpokenLanguage.AutoDetect,
                                null,
                            ),
                    ),
                )
                VexLifePersonSettings.Languages.forEach {
                    language ->
                    add(
                        VexLifeProdServiceContract.LocalAsrSpokenLanguageChoice(
                            mode =
                                VexLifeLocalAsrSpokenLanguage.Fixed,
                            languageCode =
                                language.first,
                            labelKey =
                                null,
                            languageLabel =
                                language.second,
                            selected =
                                spokenLanguage.mode ==
                                    VexLifeLocalAsrSpokenLanguage.Fixed &&
                                    spokenLanguage.languageCode ==
                                    language.first,
                            supportedBySelectedModel =
                                supports(
                                    VexLifeLocalAsrSpokenLanguage.Fixed,
                                    language.first,
                                ),
                        ),
                    )
                }
            }

        return VexLifeProdServiceContract.LocalAsrProjection(
            schemaVersion =
                VexLifeProdServiceContract.LocalAsrProjectionSchema,
            selectedProviderRef = selected.providerRef,
            selectedModelRef = selected.modelRef,
            spokenLanguageMode =
                spokenLanguage.mode,
            spokenLanguageCode =
                spokenLanguage.languageCode,
            resolvedSpokenLanguageCode =
                resolvedSpokenLanguage,
            spokenLanguageChoices =
                spokenLanguageChoices,
            composerOutputMode =
                composerOutput.mode,
            composerOutputChoices =
                VexLifeLocalAsrComposerOutput.Modes.map {
                    mode ->
                    VexLifeProdServiceContract.LocalAsrComposerOutputChoice(
                        mode =
                            mode,
                        labelKey =
                            VexLifeLocalAsrComposerOutput.labelKey(
                                mode,
                            ),
                        selected =
                            composerOutput.mode ==
                                mode,
                        supportedBySelectedModel =
                            VexLifeLocalAsrComposerOutput.supportedBy(
                                modelRef =
                                    selected.modelRef,
                                mode =
                                    mode,
                            ),
                    )
                },
            freeStorageBytes =
                localAsrModels.freeStorageBytes(),
            options =
                VexLifeLocalAsrSelection.Options.map {
                    option ->
                    val modelStatus =
                        localAsrModels.status(
                            option.modelRef,
                        )
                    val optionSelection =
                        VexLifeLocalAsrSelection.Value(
                            schemaVersion =
                                VexLifeLocalAsrSelection.Schema,
                            providerRef =
                                option.providerRef,
                            modelRef =
                                option.modelRef,
                        )
                    val current =
                        localAsrProvider(
                            optionSelection,
                        )
                            .current()
                    VexLifeProdServiceContract.LocalAsrOption(
                        providerRef = option.providerRef,
                        modelRef = option.modelRef,
                        providerLabel = option.providerLabel,
                        modelLabel = option.modelLabel,
                        languageCodes = option.languageCodes,
                        modelBytes = option.modelBytes,
                        standing = option.standing,
                        selected =
                            option.providerRef ==
                                selected.providerRef &&
                                option.modelRef ==
                                selected.modelRef,
                        artifactState =
                            modelStatus.artifactState,
                        presentBytes =
                            modelStatus.presentBytes,
                        downloadedBytes =
                            modelStatus.downloadedBytes,
                        controls =
                            modelStatus.controls
                                .filter {
                                    controlRef ->
                                    controlRef !=
                                        VexLifeLocalAsrModelsFamily.ControlUse ||
                                        VexLifeLocalAsrComposerOutput.supportedBy(
                                            modelRef =
                                                option.modelRef,
                                            mode =
                                                composerOutput.mode,
                                        )
                                }
                                .map {
                                    controlRef ->
                                    VexLifeProdServiceContract.LocalAsrModelControl(
                                    controlRef =
                                        controlRef,
                                    labelKey =
                                        VexLifeLocalAsrModelsFamily.labelKey(
                                            controlRef,
                                        ),
                                )
                            },
                        currentState =
                            current.state,
                        currentDetailRef =
                            current.detailRef,
                    )
                },
        )
    }

    private fun handleLocalAsr(
        exchange: HttpExchange,
    ) {
        when (
            exchange.requestMethod
        ) {
            "GET" ->
                respondJson(
                    exchange = exchange,
                    status = 200,
                    value = localAsrProjection(),
                )

            "POST" -> {
                requireJson(
                    exchange,
                )
                val update =
                    decodeRequest<VexLifeProdServiceContract.LocalAsrSelectionUpdate>(exchange)
                val value =
                    VexLifeLocalAsrSelection.Value(
                        schemaVersion =
                            VexLifeLocalAsrSelection.Schema,
                        providerRef =
                            update.providerRef,
                        modelRef =
                            update.modelRef,
                    )
                require(
                    VexLifeLocalAsrComposerOutput.supportedBy(
                        modelRef =
                            value.modelRef,
                        mode =
                            localAsrComposerOutput.read().mode,
                    ),
                ) {
                    "selected speech model does not support the current Composer output"
                }
                val currentSelection =
                    localAsrSelection.read()
                if (
                    currentSelection !=
                    value
                ) {
                    require(
                        value.providerRef ==
                            VexLifeLocalAsrSelection.WhisperCppProviderRef,
                    ) {
                        "local ASR provider is not offered"
                    }
                    localAsrModels.control(
                        controlRef =
                            VexLifeLocalAsrModelsFamily.ControlUse,
                        modelRef =
                            value.modelRef,
                    )
                }
                respondJson(
                    exchange = exchange,
                    status = 200,
                    value = localAsrProjection(),
                )
            }

            else ->
                respondError(
                    exchange,
                    405,
                    "METHOD_NOT_ALLOWED",
                    "local ASR accepts GET or POST",
                )
        }
    }

    private fun handleLocalAsrModelControl(
        exchange: HttpExchange,
    ) {
        requireMethod(
            exchange,
            "POST",
        )
        requireJson(
            exchange,
        )
        val request =
            decodeRequest<VexLifeProdServiceContract.LocalAsrModelControlRequest>(exchange)
        if (
            request.controlRef ==
                VexLifeLocalAsrModelsFamily.ControlUse
        ) {
            require(
                VexLifeLocalAsrComposerOutput.supportedBy(
                    modelRef =
                        request.modelRef,
                    mode =
                        localAsrComposerOutput.read().mode,
                ),
            ) {
                "selected speech model does not support the current Composer output"
            }
        }
        localAsrModels.control(
            controlRef =
                request.controlRef,
            modelRef =
                request.modelRef,
        )
        respondJson(
            exchange = exchange,
            status = 200,
            value = localAsrProjection(),
        )
    }

    private fun handleLocalAsrSpokenLanguage(
        exchange: HttpExchange,
    ) {
        requireMethod(
            exchange,
            "POST",
        )
        requireJson(
            exchange,
        )
        val update =
            decodeRequest<VexLifeProdServiceContract.LocalAsrSpokenLanguageUpdate>(exchange)
        val value =
            VexLifeLocalAsrSpokenLanguage.Value(
                schemaVersion =
                    VexLifeLocalAsrSpokenLanguage.Schema,
                mode =
                    update.mode,
                languageCode =
                    update.languageCode,
            )

        val selectedOption =
            VexLifeLocalAsrSelection.option(
                localAsrSelection.read(),
            )
        val replyLanguage =
            personSettings.read()
                .replyLanguage
        val resolved =
            VexLifeLocalAsrSpokenLanguage.resolvedLanguageCode(
                value = value,
                replyLanguage = replyLanguage,
            )
        require(
            (
                resolved ==
                    "auto" &&
                    selectedOption.languageCodes.size >
                    1
            ) ||
                resolved in
                    selectedOption.languageCodes,
        ) {
            "selected local ASR model does not support the chosen spoken language"
        }

        localAsrSpokenLanguage.write(
            value,
        )
        VexLogger.record(
            VexLogger.Fact.LocalAsrSpokenLanguageSaved,
            refs =
                mapOf(
                    "mode" to value.mode,
                    "languageCode" to (value.languageCode ?: ""),
                ),
        )
        respondJson(
            exchange = exchange,
            status = 200,
            value = localAsrProjection(),
        )
    }

    private fun handleLocalAsrComposerOutput(
        exchange: HttpExchange,
    ) {
        requireMethod(
            exchange,
            "POST",
        )
        requireJson(
            exchange,
        )
        val update =
            JsonCodec.decodeFromString<VexLifeProdServiceContract.LocalAsrComposerOutputUpdate>(
                requestText(
                    exchange,
                ),
            )
        val value =
            VexLifeLocalAsrComposerOutput.Value(
                schemaVersion =
                    VexLifeLocalAsrComposerOutput.Schema,
                mode =
                    update.mode,
            )
        val selected =
            localAsrSelection.read()
        require(
            VexLifeLocalAsrComposerOutput.supportedBy(
                modelRef =
                    selected.modelRef,
                mode =
                    value.mode,
            ),
        ) {
            "selected speech model does not support the chosen Composer output"
        }
        localAsrComposerOutput.write(
            value,
        )
        VexLogger.record(
            VexLogger.Fact.LocalAsrComposerOutputSaved,
            refs =
                mapOf(
                    "mode" to value.mode,
                ),
        )
        respondJson(
            exchange = exchange,
            status = 200,
            value = localAsrProjection(),
        )
    }

    private fun handleLocalAsrPrepare(
        exchange: HttpExchange,
    ) {
        requireMethod(
            exchange,
            "POST",
        )

        val selected =
            localAsrSelection.read()
        val option =
            VexLifeLocalAsrSelection.option(
                selected,
            )
        val language =
            VexLifeLocalAsrSpokenLanguage.resolvedLanguageCode(
                value =
                    localAsrSpokenLanguage.read(),
                replyLanguage =
                    personSettings.read()
                        .replyLanguage,
            )
        require(
            (
                language ==
                    "auto" &&
                    option.languageCodes.size >
                    1
            ) ||
                language in
                    option.languageCodes,
        ) {
            "selected local ASR model does not support the current microphone language"
        }

        val provider =
            localAsrProvider(
                selected,
            )
        val current =
            provider.current()
        if (
            current.providerRef !=
                selected.providerRef ||
            current.modelRef !=
                selected.modelRef ||
            !current.ready
        ) {
            respondError(
                exchange,
                409,
                "LOCAL_ASR_NOT_CURRENT",
                "the selected local speech recognizer is not current on this VexLife service",
            )
            return
        }

        try {
            provider.prepare()
        } catch (
            error: Throwable,
        ) {
            respondError(
                exchange,
                503,
                "LOCAL_ASR_PREPARE_FAILED",
                "local speech recognition did not become ready",
            )
            return
        }

        exchange.sendResponseHeaders(
            204,
            -1,
        )
    }

    private fun handleLocalAsrTranscribe(
        exchange: HttpExchange,
    ) {
        requireMethod(
            exchange,
            "POST",
        )
        requireContentType(
            exchange = exchange,
            expected = "audio/wav",
        )

        val selected =
            localAsrSelection.read()
        val option =
            VexLifeLocalAsrSelection.option(
                selected,
            )
        val requestedLanguage =
            exchange.requestHeaders
                .getFirst(
                    "x-vexlife-language",
                )
                ?.trim()
                ?.lowercase()
                ?: throw IllegalArgumentException(
                    "local ASR requires x-vexlife-language",
                )
        val configuredLanguage =
            VexLifeLocalAsrSpokenLanguage.resolvedLanguageCode(
                value =
                    localAsrSpokenLanguage.read(),
                replyLanguage =
                    personSettings.read()
                        .replyLanguage,
            )
        require(
            requestedLanguage ==
                configuredLanguage,
        ) {
            "local ASR request language does not match the current microphone-language setting"
        }
        require(
            (
                configuredLanguage ==
                    "auto" &&
                    option.languageCodes.size >
                    1
            ) ||
                configuredLanguage in
                    option.languageCodes,
        ) {
            "selected local ASR model does not support the requested language"
        }
        val language =
            configuredLanguage
        val composerOutputMode =
            localAsrComposerOutput.read()
                .mode
        require(
            VexLifeLocalAsrComposerOutput.supportedBy(
                modelRef =
                    selected.modelRef,
                mode =
                    composerOutputMode,
            ),
        ) {
            "selected local ASR model does not support the current Composer output"
        }

        val dictationPass =
            exchange.requestHeaders
                .getFirst(
                    VexLifeProdServiceContract.LocalAsrDictationPassHeader,
                )
                ?.trim()
                ?.lowercase()
                ?.let {
                    VexLifeProdServiceContract.LocalAsrDictationPass.forWire(
                        it,
                    )
                }
                ?: VexLifeProdServiceContract.LocalAsrDictationPass.Final

        val provider =
            localAsrProvider(
                selected,
            )
        val current =
            provider.current()
        if (
            current.providerRef !=
                selected.providerRef ||
            current.modelRef !=
                selected.modelRef ||
            !current.ready
        ) {
            respondError(
                exchange,
                409,
                "LOCAL_ASR_NOT_CURRENT",
                "the selected local speech recognizer is not current on this VexLife service",
            )
            return
        }

        val wav =
            requestBytes(
                exchange = exchange,
                maximumBytes =
                    MaxLocalAsrRequestBytes,
            )
        require(
            wav.size >=
                44 &&
                wav.copyOfRange(
                    0,
                    4,
                )
                    .toString(
                        StandardCharsets.US_ASCII,
                    ) ==
                "RIFF" &&
                wav.copyOfRange(
                    8,
                    12,
                )
                    .toString(
                        StandardCharsets.US_ASCII,
                    ) ==
                "WAVE",
        ) {
            "local ASR request must be one PCM WAV"
        }

        VexLogger.record(
            VexLogger.Fact.LocalAsrInferenceStarted,
            refs =
                mapOf(
                    "sourceSha" to sourceSha,
                            "providerRef" to selected.providerRef,
                    "modelRef" to selected.modelRef,
                    "language" to language,
                    "composerOutputMode" to composerOutputMode,
                    "dictationPass" to dictationPass.wire,
                    "audioBytes" to wav.size.toString(),
                ),
        )

        val result =
            try {
                provider.transcribe(
                    wavBytes = wav,
                    languageCode = language,
                    composerOutputMode =
                        composerOutputMode,
                    timeout =
                        when (
                            dictationPass
                        ) {
                            VexLifeProdServiceContract.LocalAsrDictationPass.Partial ->
                                Duration.ofSeconds(
                                    8,
                                )

                            VexLifeProdServiceContract.LocalAsrDictationPass.Final ->
                                Duration.ofSeconds(
                                    45,
                                )
                        },
                )
            } catch (
                error: Throwable,
            ) {
                VexLogger.record(
                    VexLogger.Fact.LocalAsrInferenceFailed,
                    refs =
                        mapOf(
                            "sourceSha" to sourceSha,
                            "providerRef" to selected.providerRef,
                            "modelRef" to selected.modelRef,
                            "language" to language,
                            "composerOutputMode" to composerOutputMode,
                            "dictationPass" to dictationPass.wire,
                            "failureType" to error.javaClass.simpleName,
                        ),
                )
                respondError(
                    exchange,
                    503,
                    "LOCAL_ASR_FAILED",
                    "local speech recognition did not complete",
                )
                return
            }

        VexLogger.record(
            VexLogger.Fact.LocalAsrInferenceCompleted,
            refs =
                mapOf(
                    "sourceSha" to sourceSha,
                            "providerRef" to result.providerRef,
                    "modelRef" to result.modelRef,
                    "language" to language,
                    "composerOutputMode" to composerOutputMode,
                    "dictationPass" to dictationPass.wire,
                    "inferenceMillis" to result.inferenceMillis.toString(),
                ),
        )

        respondJson(
            exchange = exchange,
            status = 200,
            value =
                VexLifeProdServiceContract.LocalAsrTranscriptionResponse(
                    schemaVersion =
                        VexLifeProdServiceContract.LocalAsrTranscriptionResponseSchema,
                    providerRef = result.providerRef,
                    modelRef = result.modelRef,
                    transcript = result.transcript,
                    composerOutputMode =
                        composerOutputMode,
                    inferenceMillis = result.inferenceMillis,
                    effectsPerformed = true,
                ),
        )
    }

    /**
     * What every ordinary prompt tells Vex about the person: the reply language from their Home
     * settings, and the recent private conversations Vex does not remember.
     */
    private fun personContext(): DirectPromptContext.PersonContext {
        val language =
            VexLifePersonSettings.canonicalReplyLanguage(
                runCatching {
                    personSettings.read().replyLanguage
                }
                    .getOrDefault(
                        VexLifePersonSettings.DefaultLanguage,
                    ),
            )
        val recentCutoff = java.time.Instant.now().minus(java.time.Duration.ofDays(PrivateGapRecentDays))
        val gaps =
            runCatching { privateConversation.intervals() }
                .getOrDefault(emptyList())
                .filter { java.time.Instant.parse(it.endedAt).isAfter(recentCutoff) }
                .takeLast(DirectPromptContext.MaximumPrivateGaps)
                .map { DirectPromptContext.PrivateGap(it.startedAt, it.endedAt, it.exchangeCount, it.keptNotes) }
        return DirectPromptContext.PersonContext(
            replyLanguage = language,
            replyLanguageName = VexLifePersonSettings.replyLanguageName(language),
            privateConversations = gaps,
        )
    }

    private fun observePrivate(): VexLifeProdServiceContract.PrivateProjection {
        val state = privateConversation.state()
        val snapshot = privateConversation.snapshot()
        return VexLifeProdServiceContract.PrivateProjection(
            schemaVersion = VexLifeProdServiceContract.PrivateProjectionSchema,
            state = state.stateRef,
            display = VexLifePrivateModeFamily.displayStateFor(state),
            startedAtOrNull = snapshot?.startedAt?.toString(),
            exchanges =
                snapshot?.exchanges.orEmpty().map {
                    VexLifeProdServiceContract.PrivateExchange(
                        human = it.human,
                        vex = it.vex,
                        sealedHumanReasoning = it.sealedHumanReasoning,
                        at = it.at.toString(),
                    )
                },
            keptNotes = snapshot?.keptNotes.orEmpty(),
            proposedNoteOrNull = snapshot?.proposedNote,
            intervals =
                privateConversation.intervals().takeLast(PrivateIntervalProjectionLimit).map {
                    VexLifeProdServiceContract.PrivateInterval(it.intervalRef, it.startedAt, it.endedAt, it.exchangeCount, it.keptNotes)
                },
            effectsPerformed = false,
        )
    }

    private fun handlePrivate(exchange: HttpExchange) {
        requireMethod(exchange, "GET")
        respondJson(exchange = exchange, status = 200, value = observePrivate())
    }

    private fun handlePrivateControl(exchange: HttpExchange) {
        requireMethod(exchange, "POST")
        requireJson(exchange)
        val request = decodeRequest<VexLifeProdServiceContract.PrivateControlRequest>(exchange)
        val control = requireNotNull(VexLifePrivateModeFamily.controlForRef(request.controlRef))
        val act = VexLifePrivateModeFamily.resolve(privateConversation.state(), control)
        val outcome =
            when (act?.effect) {
                VexLifePrivateModeFamily.Effect.BeginPrivate -> {
                    privateConversation.begin()
                    VexLifeProdServiceContract.PrivateRealized
                }
                VexLifePrivateModeFamily.Effect.EndPrivate -> {
                    privateConversation.end()
                    VexLifeProdServiceContract.PrivateRealized
                }
                null -> VexLifeProdServiceContract.PrivateNotLawfulNow
            }
        respondJson(
            exchange = exchange,
            status = if (outcome == VexLifeProdServiceContract.PrivateRealized) 200 else 409,
            value =
                VexLifeProdServiceContract.PrivateControlResult(
                    schemaVersion = VexLifeProdServiceContract.PrivateControlResultSchema,
                    requestRef = request.requestRef,
                    controlRef = request.controlRef,
                    outcome = outcome,
                    private = observePrivate(),
                ),
        )
    }

    /** Runs one private effect; a state conflict or an unreachable Vex is reported, never a 500. */
    private fun privateEffect(exchange: HttpExchange, effect: () -> Unit) {
        try {
            effect()
        } catch (_: DirectCompanionConnection.ConnectionException) {
            respondError(exchange, 503, "VEX_UNAVAILABLE", "Vex couldn't reply just now. Nothing was kept.")
            return
        } catch (conflict: IllegalStateException) {
            respondError(exchange, 409, "PRIVATE_NOT_NOW", conflict.message ?: "That isn't possible in Private mode right now.")
            return
        }
        respondJson(exchange = exchange, status = 200, value = observePrivate())
    }

    private fun requirePrivateOpen(exchange: HttpExchange): Boolean {
        if (privateConversation.state() is VexLifePrivateModeFamily.Private) return true
        respondError(exchange, 409, "PRIVATE_MODE_NOT_OPEN", "Private mode is not on.")
        return false
    }

    private fun handlePrivateSend(exchange: HttpExchange) {
        requireMethod(exchange, "POST")
        if (
            models.switchInProgress() ||
                models.switchHeld()
        ) {
            respondError(
                exchange,
                409,
                "MODEL_SWITCH_NOT_CURRENT",
                "Vex's local model is changing or held; this Private message was not sent.",
            )
            return
        }
        requireJson(exchange)
        val request = decodeRequest<VexLifeProdServiceContract.PrivateSendRequest>(exchange)
        if (!requirePrivateOpen(exchange)) return
        val memory = runCatching { memoryReader(Path.of(startupSetup.productHome)) }.getOrNull()
        val memoryMessage = memory?.let { runCatching { DirectPromptContext.memorySystemMessage(it) }.getOrNull() }
        val language = runCatching { personSettings.read().replyLanguage }.getOrDefault(VexLifePersonSettings.DefaultLanguage)
        val languageNote = "Reply in " + VexLifePersonSettings.replyLanguageName(language) + " ($language) unless the person clearly wants another language."
        // A private conversation is not kept, but Vex is still standing on the same ground in it.
        val foundationalMessage =
            VexLifeFoundationalField.project(
                    VexLifeFoundationalField.readCurrent(),
                    VexLifeFoundationalField.KernelTokenBudget,
                )
                .systemContent
        privateEffect(exchange) {
            privateConversation.send(
                request.content,
                listOfNotNull(foundationalMessage, languageNote, memoryMessage).joinToString("\n\n"),
                privateCompletion,
            )
        }
    }

    private fun handlePrivateNotePropose(exchange: HttpExchange) {
        requireMethod(exchange, "POST")
        if (!requirePrivateOpen(exchange)) return
        privateEffect(exchange) { privateConversation.proposeNote(privateCompletion) }
    }

    private fun handlePrivateNoteKeep(exchange: HttpExchange) {
        requireMethod(exchange, "POST")
        requireJson(exchange)
        val request = decodeRequest<VexLifeProdServiceContract.PrivateNoteKeepRequest>(exchange)
        if (!requirePrivateOpen(exchange)) return
        privateEffect(exchange) { privateConversation.keepNote(request.text) }
    }

    private fun handlePrivateNoteDecline(exchange: HttpExchange) {
        requireMethod(exchange, "POST")
        if (!requirePrivateOpen(exchange)) return
        privateEffect(exchange) { privateConversation.declineNote() }
    }

    private fun handleFoundationalField(exchange: HttpExchange) {
        requireMethod(exchange, "GET")
        respondJson(exchange = exchange, status = 200, value = observeFoundationalField())
    }

    /**
     * The field read through its owner exactly as a turn reads it, projected with its whole budget.
     * This reports what Vex would be given now; what a completed turn was given is on that turn's
     * receipt and is never re-formed from here.
     */
    private fun observeFoundationalField(): VexLifeProdServiceContract.FoundationalFieldCurrentProjection {
        val reading = VexLifeFoundationalField.readCurrent()
        val projection = VexLifeFoundationalField.project(reading, VexLifeFoundationalField.KernelTokenBudget)
        val current = reading as? VexLifeFoundationalField.Reading.Current
        return VexLifeProdServiceContract.FoundationalFieldCurrentProjection(
            schemaVersion = VexLifeProdServiceContract.FoundationalFieldSchema,
            foundationalField = foundationalFieldProjection(projection.evidence),
            kernelText = projection.systemContent,
            unavailableDetail = (reading as? VexLifeFoundationalField.Reading.Unavailable)?.detail,
            purpose = current?.body?.purpose,
            parts =
                current?.body?.modules.orEmpty().map {
                    VexLifeProdServiceContract.FoundationalFieldPartProjection(
                        moduleRef = it.moduleRef,
                        title = it.title,
                        question = it.question,
                        origins = it.origins.map { origin -> origin.name },
                        standing = it.standing.name,
                        provenance = it.provenance,
                        sourceRefs = it.sourceRefs,
                        kernel = it.kernel,
                        depth = it.depth,
                    )
                },
            sources =
                current?.body?.sources.orEmpty().map {
                    VexLifeProdServiceContract.FoundationalFieldSourceProjection(
                        sourceRef = it.sourceRef,
                        repository = it.repository,
                        commit = it.commit,
                        path = it.path,
                        blob = it.blob,
                        coverage = it.coverage,
                    )
                },
            unresolved =
                current?.body?.unresolved.orEmpty().map {
                    VexLifeProdServiceContract.FoundationalFieldUnresolvedProjection(
                        unresolvedRef = it.unresolvedRef,
                        moduleRefs = it.moduleRefs,
                        summary = it.summary,
                        note = it.note,
                    )
                },
            revisions =
                current?.revisions.orEmpty().map {
                    VexLifeProdServiceContract.FoundationalFieldRevisionProjection(
                        version = it.version,
                        bodySha256 = it.bodySha256,
                        recordedOn = it.recordedOn,
                        recordedBy = it.recordedBy,
                        summary = it.summary,
                        why = it.why,
                    )
                },
            effectsPerformed = false,
        )
    }

    private fun foundationalFieldProjection(
        evidence: VexLifeFoundationalField.Evidence,
    ): VexLifeProdServiceContract.FoundationalFieldProjection =
        VexLifeProdServiceContract.FoundationalFieldProjection(
            state = evidence.state.name,
            heldReason = evidence.heldReason?.name,
            fieldRef = evidence.fieldRef,
            fieldVersion = evidence.fieldVersion,
            bodySha256 = evidence.bodySha256,
            systemMessageSha256 = evidence.systemMessageSha256,
            estimatedSystemMessageTokens = evidence.estimatedSystemMessageTokens,
            tokenBudget = evidence.tokenBudget,
            carriedModules =
                evidence.carriedModules.map {
                    VexLifeProdServiceContract.FoundationalFieldModuleProjection(
                        moduleRef = it.moduleRef,
                        origins = it.origins.map { origin -> origin.name },
                        standing = it.standing.name,
                        depthInSource = it.depthInSource,
                    )
                },
            omittedModuleRefs = evidence.omittedModuleRefs,
            omissionReasons = evidence.omissionReasons,
            depthNotCarriedModuleRefs = evidence.depthNotCarriedModuleRefs,
            sourceRefs = evidence.sourceRefs,
            unresolvedRefs = evidence.unresolvedRefs,
            effectAuthorityGranted = evidence.effectAuthorityGranted,
        )

    private fun handleMemory(exchange: HttpExchange) {
        requireMethod(exchange, "GET")
        respondJson(exchange = exchange, status = 200, value = observeMemory())
    }

    /**
     * Fresh no-effect Memory observation through the same reader prompt materialization uses, so
     * what the person is told is what Vex is actually given. A read failure is Unreadable, never a
     * guess.
     */
    private fun observeMemory(): VexLifeProdServiceContract.MemoryProjection {
        val context = runCatching { memoryReader(Path.of(startupSetup.productHome)) }.getOrNull()
        val state =
            VexLifeMemoryFamily.observe(
                readable = context != null,
                current = context?.projectionCurrentness == "CURRENT",
                heldReasons = context?.reasons.orEmpty(),
                rememberedCount = context?.pages?.size ?: 0,
            )
        return VexLifeProdServiceContract.MemoryProjection(
            schemaVersion = VexLifeProdServiceContract.MemoryProjectionSchema,
            state = state.stateRef,
            display = VexLifeMemoryFamily.displayStateFor(state),
            memoryContextRef = context?.memoryContextRef,
            memoryContextFingerprint = context?.semanticFingerprint,
            currentDailyStratumRef = context?.currentDailyStratumRef,
            rememberedCount = context?.pages?.size ?: 0,
            effectsPerformed = false,
        )
    }

    private fun handleMemoryDream(
        exchange: HttpExchange,
    ) {
        val executor =
            requireNotNull(
                dreamExecutor,
            ) {
                "memory Dream route is not configured"
            }

        when (
            exchange.requestMethod
        ) {
            "GET" ->
                respondJson(
                    exchange = exchange,
                    status = 200,
                    value = executor.observe(startupSetup),
                )

            "POST" -> {
                requireJson(
                    exchange,
                )
                val request =
                    decodeRequest<VexLifeProdServiceContract.MemoryDreamRunRequest>(exchange)
                val before =
                    executor.observe(
                        startupSetup,
                    )
                val sameCommittedDay =
                    before.currentDayRef ==
                        request.dayRef
                if (
                    !sameCommittedDay
                ) {
                    require(
                        before.state ==
                            "SOURCE_FRONTIER_READY" &&
                            before.sourceConversationHeadSha256 ==
                            request.expectedConversationHeadSha256 &&
                            before.sourceScoreHeadSha256 ==
                            request.expectedScoreHeadSha256 &&
                            before.currentDailyDreamHeadSha256 ==
                            request.expectedDailyDreamHeadSha256,
                    ) {
                        "new memory Dream request does not bind the currently ready source frontier"
                    }
                }
                val result =
                    executor.perform(
                        setup = startupSetup,
                        request = request,
                    )
                val after =
                    executor.observe(
                        startupSetup,
                    )
                require(
                    after.currentDailyDreamHeadSha256 == result.dailyDreamHeadSha256 &&
                        after.currentDailyStratumSha256 == result.dailyStratumSha256,
                ) {
                    "memory Dream result does not match the observed durable post-Dream head"
                }
                if (
                    result.state ==
                        "COMMITTED"
                ) {
                    require(
                        result.sourceConversationHeadSha256 == before.sourceConversationHeadSha256 &&
                            result.sourceScoreHeadSha256 == before.sourceScoreHeadSha256 &&
                            result.sourceSemanticAuthorityHeadSha256 == before.sourceSemanticAuthorityHeadSha256,
                    ) {
                        "new memory Dream result does not bind the observed pre-Dream source frontier"
                    }
                }
                require(
                    !result.lineageAwareGenerativeDreamRan &&
                        !result.rhythmLearned &&
                        !result.trainingRan &&
                        !result.modelWeightsChanged &&
                        !result.adapterChanged &&
                        !result.synchronizationActivated &&
                        !result.poweredDown &&
                        !result.publicationPerformed &&
                        !result.firstPersonAuthorityGranted,
                ) {
                    "memory Dream crossed one of its held later-effect boundaries"
                }
                val mutated =
                    result.state ==
                        "COMMITTED"
                respondJson(
                    exchange = exchange,
                    status = 200,
                    value =
                        VexLifeProdServiceContract.MemoryDreamRunResponse(
                            schemaVersion = VexLifeProdServiceContract.MemoryDreamRunResponseSchema,
                            state = result.state,
                            before = before,
                            after = after,
                            dailyDreamHeadSha256 = result.dailyDreamHeadSha256,
                            dailyStratumSha256 = result.dailyStratumSha256,
                            memoryFrontierMutationPerformed = mutated,
                            systemMemoryConsolidationPerformedNow = mutated && result.systemMemoryConsolidationRan,
                            modelCallPerformed = false,
                            lineageAwareGenerativeDreamRan = false,
                            rhythmLearned = false,
                            trainingRan = false,
                            modelWeightsChanged = false,
                            adapterChanged = false,
                            synchronizationActivated = false,
                            poweredDown = false,
                            publicationPerformed = false,
                            firstPersonAuthorityGranted = false,
                            effectsPerformed = mutated,
                        ),
                )
            }

            else ->
                throw IllegalArgumentException(
                    "memory Dream route requires HTTP GET or POST",
                )
        }
    }
    private fun handleSend(
        exchange: HttpExchange,
    ) {
        requireMethod(
            exchange,
            "POST",
        )
        if (
            models.switchInProgress()
        ) {
            respondError(
                exchange,
                409,
                "MODEL_SWITCH_IN_PROGRESS",
                "Vex is switching local models; this message was not sent.",
            )
            return
        }
        if (
            models.switchHeld()
        ) {
            respondError(
                exchange,
                409,
                "MODEL_SWITCH_HELD",
                "The last model switch did not complete. Retry the chosen model or choose the currently serving model.",
            )
            return
        }
        // A message written while Private mode is on must never reach canonical Conversation.
        if (privateConversation.state() is VexLifePrivateModeFamily.Private) {
            respondError(exchange, 409, "PRIVATE_MODE_ON", "Private mode is on; this message was not kept.")
            return
        }
        requireJson(
            exchange,
        )

        val request =
            decodeRequest<VexLifeProdServiceContract.SendRequest>(exchange)

        val intent =
            sendIntent(
                request,
            )
        val streaming =
            streamingTurnExecutor
        if (
            acceptsEventStream(
                exchange,
            ) &&
                streaming !=
                null
        ) {
            handleStreamingSend(
                exchange = exchange,
                intent = intent,
                executor = streaming,
            )
            return
        }

        val result =
            turnExecutor.perform(
                startupSetup,
                intent,
            )

        respondJson(
            exchange = exchange,
            status = 200,
            value =
                sendResponse(
                    result,
                ),
        )
    }

    private fun sendIntent(
        request: VexLifeProdServiceContract.SendRequest,
    ): DirectCompanionTurn.Intent =
        DirectCompanionTurn.Intent(
            idempotencyKey = request.idempotencyKey,
            content = request.content,
            personContext = personContext(),
            contextProfile =
                contextProfiles.readActive(
                    productFoundationIdentity(),
                ),
            browserContext =
                request.browserContext
                    ?.let {
                        DirectPromptContext.BrowserContext(
                            pageUrl = it.pageUrl,
                            title = it.title,
                            text = it.text,
                            capture = it.capture,
                        )
                    },
        )

    private fun sendResponse(
        result: DirectCompanionTurn.Result,
    ): VexLifeProdServiceContract.SendResponse =
        VexLifeProdServiceContract.SendResponse(
            schemaVersion = VexLifeProdServiceContract.SendResponseSchema,
            state = result.state,
            recoveredCompletedTurn = result.recoveredCompletedTurn,
            modelCallPerformed = result.modelCallPerformed,
            actualHttpCallPerformedNow = result.actualHttpCallPerformedNow,
            turnRef = result.turnRef,
            requestMessageRef = result.requestMessageRef,
            responseMessageRef = result.responseMessageRef,
            requestEventRef = result.requestEventRef,
            responseEventRef = result.responseEventRef,
            content = result.content,
            model = result.model,
            currentConversationHeadSha256 = result.currentConversationHeadSha256,
            contextPath = result.contextPath,
            promptContext =
                promptContextProjection(
                    result.promptContextReceipt,
                ),
            humanReasoning =
                humanReasoningProjection(
                    result,
                ),
            providerRequests =
                result.providerRequests.map {
                    request ->
                    VexLifeProdServiceContract.ProviderRequestProjection(
                        stageIndex = request.stageIndex,
                        requestBodySha256 = request.requestBodySha256,
                        requestBody = request.requestBody,
                        persisted = false,
                    )
                },
            capabilityUses =
                result.capabilityUses.map {
                    VexLifeProdServiceContract.CapabilityUse(
                        handle = it.handle,
                        action = it.action,
                        target = it.target,
                        state = it.state.name,
                    )
                },
        )

    private fun acceptsEventStream(
        exchange: HttpExchange,
    ): Boolean =
        exchange.requestHeaders[
            "accept"
        ]
            ?.any {
                it
                    .split(
                        ",",
                    )
                    .any {
                        value ->
                        value
                            .substringBefore(
                                ";",
                            )
                            .trim()
                            .equals(
                                "text/event-stream",
                                ignoreCase = true,
                            )
                    }
            } ==
            true

    private fun handleStreamingSend(
        exchange: HttpExchange,
        intent: DirectCompanionTurn.Intent,
        executor: StreamingTurnExecutor,
    ) {
        exchange.responseHeaders.set(
            "content-type",
            "text/event-stream; charset=utf-8",
        )
        exchange.responseHeaders.set(
            "cache-control",
            "no-store",
        )
        exchange.responseHeaders.set(
            "x-accel-buffering",
            "no",
        )
        exchange.sendResponseHeaders(
            200,
            0,
        )

        // The browser is an observer of canonical turn progress, not part of provider execution
        // custody. DirectCompanionConnection consumes the provider stream synchronously, so writing
        // to the browser from its progress callback would let a slow/stalled browser socket
        // backpressure model-stream ingestion. Keep one bounded presentation queue and one separate
        // socket writer instead. Live deltas may be lost under presentation pressure; canonical
        // execution, durable Conversation truth, and the terminal result may not be stalled by it.
        val projectionQueue =
            LinkedBlockingQueue<VexLifeProdServiceContract.SendStreamEvent>(
                16_384,
            )
        val projectionOpen =
            AtomicBoolean(
                true,
            )
        val producerDone =
            AtomicBoolean(
                false,
            )

        fun write(
            event: VexLifeProdServiceContract.SendStreamEvent,
        ) {
            val payload =
                JsonCodec.encodeToString(
                    event,
                )
            val bytes =
                (
                    "data: " +
                        payload +
                        "\n\n"
                )
                    .toByteArray(
                        StandardCharsets.UTF_8,
                    )
            exchange.responseBody.write(
                bytes,
            )
            exchange.responseBody.flush()
        }

        fun offerProgress(
            event: VexLifeProdServiceContract.SendStreamEvent,
        ) {
            if (
                projectionOpen.get()
            ) {
                // Progress is ephemeral presentation. Queue saturation must never become provider
                // backpressure; the final canonical response remains recoverable through the Send
                // idempotency contract.
                projectionQueue.offer(
                    event,
                )
            }
        }

        fun offerTerminal(
            event: VexLifeProdServiceContract.SendStreamEvent,
        ) {
            if (
                !projectionOpen.get()
            ) {
                return
            }

            // If an abandoned/slow browser filled the bounded queue, prefer the terminal projection
            // over old live deltas. This affects only what that browser observed, never turn truth.
            while (
                !projectionQueue.offer(
                    event,
                )
            ) {
                projectionQueue.poll()
            }
        }

        val writer =
            this.executor.submit {
                try {
                    while (
                        true
                    ) {
                        val event =
                            projectionQueue.poll(
                                50,
                                TimeUnit.MILLISECONDS,
                            )
                        if (
                            event !=
                                null
                        ) {
                            write(
                                event,
                            )
                            continue
                        }
                        if (
                            producerDone.get()
                        ) {
                            break
                        }
                    }

                    if (
                        projectionOpen.get()
                    ) {
                        exchange.responseBody.write(
                            "data: [DONE]\n\n"
                                .toByteArray(
                                    StandardCharsets.UTF_8,
                                ),
                        )
                        exchange.responseBody.flush()
                    }
                } catch (
                    _: Throwable,
                ) {
                    // Presentation transport failure is observational. It must not fail, retry, or
                    // replay the human turn.
                    projectionOpen.set(
                        false,
                    )
                    projectionQueue.clear()
                }
            }

        try {
            val result =
                executor.perform(
                    startupSetup,
                    intent,
                    DirectCompanionTurn.ProgressObserver {
                        progress ->
                        val event =
                            when (
                                progress.phase
                            ) {
                                DirectCompanionTurn.ProgressPhase.CheckingRuntime,
                                DirectCompanionTurn.ProgressPhase.Reconnecting,
                                DirectCompanionTurn.ProgressPhase.Generating,
                                ->
                                    VexLifeProdServiceContract.SendStreamEvent(
                                        type = "PHASE",
                                        phase = progress.phase.name,
                                    )

                                DirectCompanionTurn.ProgressPhase.Reasoning ->
                                    VexLifeProdServiceContract.SendStreamEvent(
                                        type = "REASONING_DELTA",
                                        delta = progress.delta,
                                        accumulatedCharacters = progress.accumulatedCharacters,
                                    )

                                DirectCompanionTurn.ProgressPhase.Responding ->
                                    VexLifeProdServiceContract.SendStreamEvent(
                                        type = "CONTENT_DELTA",
                                        delta = progress.delta,
                                        accumulatedCharacters = progress.accumulatedCharacters,
                                    )

                                DirectCompanionTurn.ProgressPhase.Reaching,
                                DirectCompanionTurn.ProgressPhase.Reached,
                                ->
                                    VexLifeProdServiceContract.SendStreamEvent(
                                        type = "REACH",
                                        reach =
                                            progress.reach?.let {
                                                VexLifeProdServiceContract.CapabilityUse(
                                                    handle = it.handle,
                                                    action = it.action,
                                                    target = it.target,
                                                    state =
                                                        if (
                                                            progress.phase ==
                                                                DirectCompanionTurn.ProgressPhase.Reached
                                                        ) {
                                                            it.state.name
                                                        } else {
                                                            null
                                                        },
                                                )
                                            },
                                    )
                            }
                        offerProgress(
                            event,
                        )
                    },
                )

            offerTerminal(
                VexLifeProdServiceContract.SendStreamEvent(
                    type = "COMPLETED",
                    response =
                        sendResponse(
                            result,
                        ),
                ),
            )
        } catch (
            error: DirectCompanionTurn.TurnException,
        ) {
            offerTerminal(
                VexLifeProdServiceContract.SendStreamEvent(
                    type = "FAILED",
                    error =
                        VexLifeProdServiceContract.ErrorResponse(
                            schemaVersion = VexLifeProdServiceContract.ErrorSchema,
                            code = error.failureCode,
                            message = "direct Companion turn failed",
                            retrySameSendAttemptAllowed = error.retrySameSendAttemptAllowed,
                            exactNextSafeRoute = error.exactNextSafeRoute,
                        ),
                ),
            )
        } catch (
            _: Throwable,
        ) {
            offerTerminal(
                VexLifeProdServiceContract.SendStreamEvent(
                    type = "FAILED",
                    error =
                        VexLifeProdServiceContract.ErrorResponse(
                            schemaVersion = VexLifeProdServiceContract.ErrorSchema,
                            code = "SERVICE_FAILURE",
                            message = "VexLife local service failed without a completed product result",
                            retrySameSendAttemptAllowed = null,
                            exactNextSafeRoute = null,
                        ),
                ),
            )
        } finally {
            producerDone.set(
                true,
            )
            runCatching {
                    writer.get(
                        5,
                        TimeUnit.SECONDS,
                    )
                }
                .onFailure {
                    // Canonical execution is already terminal. Abandon only this presentation leg.
                    projectionOpen.set(
                        false,
                    )
                    writer.cancel(
                        true,
                    )
                    runCatching {
                        exchange.responseBody.close()
                    }
                }
        }
    }

    private fun humanReasoningProjection(
        result: DirectCompanionTurn.Result,
    ): VexLifeProdServiceContract.HumanReasoningProjection {
        val trace =
            result.runtimeObservation.reasoningTrace

        if (
            result.recoveredCompletedTurn
        ) {
            if (
                !trace.present
            ) {
                require(
                    trace.contentSha256 ==
                        null &&
                        trace.characterCount ==
                        0,
                ) {
                    "absent recovered reasoning trace carries unexpected fingerprint material"
                }

                return VexLifeProdServiceContract.HumanReasoningProjection(
                    state = "ABSENT",
                    text = null,
                    contentSha256 = null,
                    characterCount = 0,
                    persisted = false,
                )
            }

            return VexLifeProdServiceContract.HumanReasoningProjection(
                state = "UNAVAILABLE_AFTER_IDEMPOTENT_RECOVERY",
                text = null,
                contentSha256 =
                    requireNotNull(
                        trace.contentSha256,
                    ) {
                        "recovered present reasoning trace is missing its content hash"
                    },
                characterCount =
                    trace.characterCount,
                persisted = false,
            )
        }

        val reasoning =
            result.humanReasoning

        if (
            reasoning ==
                null
        ) {
            require(
                !trace.present &&
                    trace.contentSha256 ==
                    null &&
                    trace.characterCount ==
                    0,
            ) {
                "fresh human reasoning disappeared before local service projection"
            }

            return VexLifeProdServiceContract.HumanReasoningProjection(
                state = "ABSENT",
                text = null,
                contentSha256 = null,
                characterCount = 0,
                persisted = false,
            )
        }

        require(
            trace.present &&
                trace.contentSha256 !=
                null &&
                trace.characterCount ==
                reasoning.codePointCount(
                    0,
                    reasoning.length,
                ) &&
                !trace.rawPersisted &&
                trace.humanProjection ==
                "SEALED_EXPLICIT_OPEN_ONLY" &&
                trace.modelProjection ==
                "NONE" &&
                trace.trainingProjection ==
                "NONE",
        ) {
            "fresh human reasoning does not match the sealed runtime observation"
        }

        return VexLifeProdServiceContract.HumanReasoningProjection(
            state = "PRESENT_EPHEMERAL",
            text = reasoning,
            contentSha256 =
                trace.contentSha256,
            characterCount =
                trace.characterCount,
            persisted = false,
        )
    }

    private fun contextProjection(): VexLifeProdServiceContract.ContextProjection {
        val productHome =
            Path.of(
                startupSetup.productHome,
            )
        val snapshot =
            DirectConversationStore(
                productHome,
            )
                .current()
        val head =
            snapshot.head
                ?: return VexLifeProdServiceContract.ContextProjection(
                    schemaVersion =
                        VexLifeProdServiceContract.ContextProjectionSchema,
                    state =
                        VexLifeProdServiceContract.ContextStateEmpty,
                    currentConversationHeadSha256 =
                        null,
                    turnRef =
                        null,
                    contextPath =
                        null,
                    promptContext =
                        null,
                    effectsPerformed =
                        false,
                )
        val contextStore =
            DirectTurnContextStore(
                productHome,
            )
        val compatibility =
            contextStore.inspectPromptReceiptCompatibility(
                head.turnRef,
            )
        if (
            compatibility !=
                null &&
                !compatibility.readableByCurrentSource
        ) {
            return VexLifeProdServiceContract.ContextProjection(
                schemaVersion =
                    VexLifeProdServiceContract.ContextProjectionSchema,
                state =
                    VexLifeProdServiceContract.ContextStateHeldUnsupportedDurablePromptReceipt,
                currentConversationHeadSha256 =
                    head.conversationHeadSha256,
                turnRef =
                    head.turnRef,
                contextPath =
                    head.contextPath,
                promptContext =
                    null,
                heldReason =
                    VexLifeProdServiceContract.ContextHeldReasonPromptReceiptSchemaUnsupported,
                durablePromptReceiptSchema =
                    compatibility.promptReceiptSchema,
                effectsPerformed =
                    false,
            )
        }
        val record =
            contextStore
                .read(
                    head.turnRef,
                )
                ?: return VexLifeProdServiceContract.ContextProjection(
                    schemaVersion =
                        VexLifeProdServiceContract.ContextProjectionSchema,
                    state =
                        VexLifeProdServiceContract.ContextStateHistoricalWithoutProductEvidence,
                    currentConversationHeadSha256 =
                        head.conversationHeadSha256,
                    turnRef =
                        head.turnRef,
                    contextPath =
                        head.contextPath,
                    promptContext =
                        null,
                    effectsPerformed =
                        false,
                )
        require(
            record.turnRef ==
                head.turnRef &&
                record.contextSha256 ==
                head.contextSha256 &&
                record.contextPath ==
                head.contextPath &&
                record.requestEventSha256 ==
                head.requestEventSha256 &&
                record.responseEventSha256 ==
                head.responseEventSha256,
        ) {
            "current turn Context does not match the exact completed Conversation head"
        }

        return VexLifeProdServiceContract.ContextProjection(
            schemaVersion =
                VexLifeProdServiceContract.ContextProjectionSchema,
            state =
                VexLifeProdServiceContract.ContextStateAvailable,
            currentConversationHeadSha256 =
                head.conversationHeadSha256,
            turnRef =
                head.turnRef,
            contextPath =
                record.contextPath,
            promptContext =
                promptContextProjection(
                    record.promptContext,
                ),
            finalProviderRequestBodySha256 =
                record.invocationEvidence.requestBodySha256,
            effectsPerformed =
                false,
        )
    }

    private fun promptContextProjection(
        receipt: DirectPromptContext.Receipt,
    ): VexLifeProdServiceContract.PromptContextProjection {
        val capabilitySnapshot =
            receipt.capabilityFrameSnapshot
        val capabilityFrame =
            if (
                receipt.capabilityFrameIncluded &&
                capabilitySnapshot !=
                null
            ) {
                VexLifeProdServiceContract.CapabilityFrameProjection(
                    frameRef =
                        requireNotNull(
                            receipt.capabilityFrameRef,
                        ) {
                            "capability frame snapshot is missing its frame ref"
                        },
                    semanticFingerprint =
                        requireNotNull(
                            receipt.capabilityFrameFingerprint,
                        ) {
                            "capability frame snapshot is missing its fingerprint"
                        },
                    sourceRefs =
                        receipt.capabilityFrameSourceRefs,
                    systemMessageSha256 =
                        requireNotNull(
                            receipt.capabilityFrameSystemMessageSha256,
                        ) {
                            "capability frame snapshot is missing its system-message hash"
                        },
                    estimatedSystemMessageTokens =
                        receipt.capabilityFrameEstimatedSystemMessageTokens,
                    availableCapabilityRefs =
                        capabilitySnapshot.availableCapabilityRefs,
                    heldCapabilityRefs =
                        capabilitySnapshot.heldCapabilityRefs,
                    unavailableCapabilityRefs =
                        capabilitySnapshot.unavailableCapabilityRefs,
                    unknownCapabilityRefs =
                        capabilitySnapshot.unknownCapabilityRefs,
                    omittedCapabilityRefs =
                        capabilitySnapshot.omittedCapabilityRefs,
                    omissionReasons =
                        capabilitySnapshot.omissionReasons,
                    tokenBudget =
                        capabilitySnapshot.tokenBudget,
                    usedTokens =
                        capabilitySnapshot.usedTokens,
                    truncated =
                        capabilitySnapshot.truncated,
                    effectAuthorityGranted =
                        capabilitySnapshot.effectAuthorityGranted,
                )
            } else {
                null
            }

        return VexLifeProdServiceContract.PromptContextProjection(
            receiptRef =
                receipt.receiptRef,
            semanticFingerprint =
                receipt.semanticFingerprint,
            contextMode =
                receipt.contextMode,
            priorConversationHeadSha256 =
                receipt.priorConversationHeadSha256,
            selectedConversationEventRefs =
                receipt.selectedConversationEventRefs,
            providerMessageSetSha256 =
                receipt.providerMessageSetSha256,
            providerMaterializedInputTokenEstimate =
                receipt.providerMaterializedInputTokenEstimate,
            inputTokenBudget =
                receipt.inputTokenBudget,
            reservedOutputTokens =
                receipt.reservedOutputTokens,
            hardTokenLimit =
                receipt.hardTokenLimit,
            wholeHistoryEventEnumerationPerformed =
                receipt.wholeHistoryEventEnumerationPerformed,
            memoryContextRef =
                receipt.memoryContextRef,
            memoryContextFingerprint =
                receipt.memoryContextFingerprint,
            memorySourceRefs =
                receipt.memorySourceRefs,
            memoryIncluded =
                receipt.memoryIncluded,
            memorySelectionPerformed =
                receipt.memorySelectionPerformed,
            trainingSelectionPerformed =
                receipt.trainingSelectionPerformed,
            browserContextIncluded =
                receipt.browserContextIncluded,
            browserContextRef =
                receipt.browserContextRef,
            browserPageUrl =
                receipt.browserPageUrl,
            browserTitle =
                receipt.browserTitle,
            browserContextSha256 =
                receipt.browserContextSha256,
            browserContextCharacters =
                receipt.browserContextCharacters,
            browserContextCapture =
                receipt.browserContextCapture,
            workingContinuityIncluded =
                receipt.workingContinuityIncluded,
            workingContinuityContextRef =
                receipt.workingContinuityContextRef,
            workingContinuityContextFingerprint =
                receipt.workingContinuityContextFingerprint,
            capabilityFrameIncluded =
                receipt.capabilityFrameIncluded,
            capabilityFrame =
                capabilityFrame,
            replyLanguage = receipt.personContext?.replyLanguage,
            replyLanguageName = receipt.personContext?.replyLanguageName,
            privateConversationsNotRemembered = receipt.personContext?.privateConversations?.size ?: 0,
            foundationalField = receipt.foundationalField?.let(::foundationalFieldProjection),
            contextProfileRef = receipt.contextProfileRef,
            contextProfileRevision = receipt.contextProfileRevision,
            contextProfileFingerprint = receipt.contextProfileFingerprint,
            contextProfileFoundationMode = receipt.contextProfileFoundationMode,
            profileFoundationState = receipt.profileFoundationState,
            profileFoundationHeldReason = receipt.profileFoundationHeldReason,
            profileFoundationSystemMessageSha256 = receipt.profileFoundationSystemMessageSha256,
            profileFoundationEstimatedSystemMessageTokens =
                receipt.profileFoundationEstimatedSystemMessageTokens,
            profileFoundationSourceCharacters = receipt.profileFoundationSourceCharacters,
            contextComponents =
                receipt.contextComponents.map {
                    component ->
                    VexLifeProdServiceContract.ContextComponentProjection(
                        componentRef = component.componentRef,
                        stabilityClass = component.stabilityClass.name,
                        placementClass = component.placementClass.name,
                        logicalOrder = component.logicalOrder,
                        providerMessageIndex = component.providerMessageIndex,
                        sha256 = component.sha256,
                        sourceRefs = component.sourceRefs,
                    )
                },
            priorCompletedExchangeCount = receipt.priorCompletedExchangeCount,
            effectsPerformed =
                receipt.effectsPerformed,
        )
    }

    private fun conversationProjection(): VexLifeProdServiceContract.ConversationProjection {
        val snapshot =
            DirectConversationStore(
                Path.of(
                    startupSetup.productHome,
                ),
            )
                .current()

        val messages =
            snapshot.canonicalEvents
                .map {
                    event ->
                    VexLifeProdServiceContract.ConversationMessage(
                        eventRef = event.eventRef,
                        eventSha256 = event.eventSha256,
                        messageRef = event.messageRef,
                        turnRef = event.turnRef,
                        role =
                            when (
                                event.eventKind
                            ) {
                                "REQUEST" ->
                                    "human"

                                "RESPONSE" ->
                                    "vex"

                                else ->
                                    error(
                                        "canonical direct Conversation contains unsupported event kind",
                                    )
                            },
                        content = event.content,
                        sequence = event.sequence,
                        formedAt = event.formedAt,
                        model = event.model,
                    )
                }

        return VexLifeProdServiceContract.ConversationProjection(
            schemaVersion = VexLifeProdServiceContract.ConversationProjectionSchema,
            projectRef = DirectConversationIdentity.ProjectRef,
            threadRef = DirectConversationIdentity.ThreadRef,
            channelRef = DirectConversationIdentity.ChannelRef,
            currentConversationHeadSha256 =
                snapshot.head
                    ?.conversationHeadSha256,
            messages = messages,
            abandonedAttemptCount = snapshot.abandonedAttempts.size,
            effectsPerformed = false,
        )
    }

    private fun requestHostAdmitted(
        exchange: HttpExchange,
    ): Boolean {
        val expected =
            VexLifeProdServiceContract.Host +
                ":" +
                server.address.port
        val actual =
            exchange.requestHeaders
                .getFirst(
                    "Host",
                )
                ?.trim()
                ?.lowercase()

        return actual ==
            expected
    }

    /**
     * Browser-originated effect requests must remain same-origin with the exact Product presentation.
     * Non-browser local clients may omit browser provenance headers.
     */
    private fun browserEffectRequestAdmitted(
        exchange: HttpExchange,
    ): Boolean {
        if (
            exchange.requestMethod ==
                "GET" ||
            exchange.requestMethod ==
                "HEAD"
        ) {
            return true
        }

        val expectedOrigin =
            "http://" +
                VexLifeProdServiceContract.Host +
                ":" +
                server.address.port
        val origin =
            exchange.requestHeaders
                .getFirst(
                    "Origin",
                )
                ?.trim()

        if (
            origin !=
                null &&
            origin !=
                expectedOrigin
        ) {
            return false
        }

        val fetchSite =
            exchange.requestHeaders
                .getFirst(
                    "Sec-Fetch-Site",
                )
                ?.trim()
                ?.lowercase()

        return fetchSite ==
            null ||
            fetchSite ==
                "same-origin" ||
            fetchSite ==
                "none"
    }

    /**
     * Browser pages are source-bound manifestations of this exact Product service. A browser page
     * that survived a service replacement must not perform an Effect against the newer/different
     * service. Non-browser local clients retain their separate admission path.
     */
    private fun browserPresentationCurrentnessFailure(
        exchange: HttpExchange,
    ): String? {
        if (
            exchange.requestMethod ==
                "GET" ||
            exchange.requestMethod ==
                "HEAD"
        ) {
            return null
        }

        val browserOriginated =
            exchange.requestHeaders
                .getFirst(
                    "Origin",
                ) !=
                null ||
                exchange.requestHeaders
                    .getFirst(
                        "Sec-Fetch-Site",
                    ) !=
                null

        if (
            !browserOriginated
        ) {
            return null
        }

        val presentationSource =
            exchange.requestHeaders
                .getFirst(
                    VexLifeProdServiceContract.PresentationSourceHeader,
                )
                ?.trim()
                ?.takeIf(
                    String::isNotEmpty,
                )

        val serviceShort =
            sourceSha.take(
                8,
            )

        if (
            presentationSource ==
                null
        ) {
            return "This browser page is from an older VexLife presentation that does not identify its source. " +
                "The running VexLife service is $serviceShort. Reload this VexLife page to continue."
        }

        if (
            !SourceShaPattern.matches(
                presentationSource,
            )
        ) {
            return "This browser page reported an unrecognized VexLife source identity. " +
                "The running VexLife service is $serviceShort. Reload this VexLife page to continue."
        }

        if (
            presentationSource !=
                sourceSha
        ) {
            return "This browser page was generated by VexLife ${presentationSource.take(8)}, " +
                "but the running service is $serviceShort. They are different VexLife builds. " +
                "Reload this VexLife page to continue."
        }

        return null
    }

    private fun requireMethod(
        exchange: HttpExchange,
        expected: String,
    ) {
        require(
            exchange.requestMethod ==
                expected,
        ) {
            "route requires HTTP $expected"
        }
    }

    private fun requireJson(
        exchange: HttpExchange,
    ) {
        val contentType =
            exchange.requestHeaders
                .getFirst(
                    "content-type",
                )
                ?.substringBefore(
                    ";",
                )
                ?.trim()
                ?.lowercase()

        require(
            contentType ==
                "application/json",
        ) {
            "Send requires application/json"
        }
    }

    private fun requireContentType(
        exchange: HttpExchange,
        expected: String,
    ) {
        val contentType =
            exchange.requestHeaders
                .getFirst(
                    "content-type",
                )
                ?.substringBefore(
                    ";",
                )
                ?.trim()
                ?.lowercase()

        require(
            contentType ==
                expected,
        ) {
            "route requires content-type $expected"
        }
    }

    private fun requestBytes(
        exchange: HttpExchange,
        maximumBytes: Int,
    ): ByteArray {
        val bytes =
            exchange.requestBody.readNBytes(
                maximumBytes +
                    1,
            )
        require(
            bytes.size <=
                maximumBytes,
        ) {
            "request body exceeds bounded local service size"
        }
        return bytes
    }

    private class RequestJsonInvalid(
        cause: SerializationException,
    ) : RuntimeException(
            cause,
        )

    private inline fun <reified T> decodeRequest(
        exchange: HttpExchange,
    ): T =
        try {
            JsonCodec.decodeFromString<T>(
                requestText(
                    exchange,
                ),
            )
        } catch (
            error: SerializationException,
        ) {
            throw RequestJsonInvalid(
                error,
            )
        }

    private fun requestText(
        exchange: HttpExchange,
    ): String {
        val bytes =
            requestBytes(
                exchange = exchange,
                maximumBytes = MaxRequestBytes,
            )

        return bytes.toString(
            StandardCharsets.UTF_8,
        )
    }

    private inline fun <reified T> respondJson(
        exchange: HttpExchange,
        status: Int,
        value: T,
    ) {
        val bytes =
            JsonCodec
                .encodeToString(
                    value,
                )
                .toByteArray(
                    StandardCharsets.UTF_8,
                )

        exchange.responseHeaders.set(
            "content-type",
            "application/json; charset=utf-8",
        )
        exchange.responseHeaders.set(
            "cache-control",
            "no-store",
        )
        exchange.sendResponseHeaders(
            status,
            bytes.size
                .toLong(),
        )
        exchange.responseBody.write(
            bytes,
        )
    }

    private fun respondError(
        exchange: HttpExchange,
        status: Int,
        code: String,
        message: String,
    ) {
        respondJson(
            exchange = exchange,
            status = status,
            value =
                VexLifeProdServiceContract.ErrorResponse(
                    schemaVersion = VexLifeProdServiceContract.ErrorSchema,
                    code = code,
                    message = message,
                    retrySameSendAttemptAllowed = null,
                    exactNextSafeRoute = null,
                ),
        )
    }
}

// [VXG RealForever]
