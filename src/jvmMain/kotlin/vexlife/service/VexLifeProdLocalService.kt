package vexlife.service

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.io.ByteArrayOutputStream
import java.net.InetSocketAddress
import java.net.URLDecoder
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
import vexlife.artifact.VexLifeLocalArtifact
import vexlife.text.VexLifeText
import vexlife.artifact.VexLifeLocalArtifactStore
import vexlife.artifact.VexLifeLocalArtifactReader
import vexlife.attachment.VexLifeAttachmentDraftFamily
import vexlife.attachment.VexLifeAttachmentDraftRuntime
import vexlife.diagnostics.VexLifeDiagnostics
import vexlife.logging.VexLogger
import vexlife.text.VexLifeStrings
import vexlife.browser.VexLifeProdBrowserContract
import vexlife.browser.VexLifeProdFrameHost
import vexlife.companion.DirectCompanionCancellation
import vexlife.companion.DirectCompanionConnection
import vexlife.companion.DirectCompanionTurn
import vexlife.companion.DirectPromptContext
import vexlife.companion.DirectTurnContextStore
import vexlife.companion.VexLifePromptPreparation
import vexlife.conversation.DirectConversationIdentity
import vexlife.conversation.DirectConversationStore
import vexlife.conversation.VexLifeConversationContract
import vexlife.conversation.VexLifeConversationTurnFamily
import vexlife.foundational.VexLifeFoundationalField
import vexlife.local.VexLifeProdLocalSetup
import vexlife.local.VexLifeProdMacRuntimeHandoff
import vexlife.local.VexLifeProdRuntimeSession
import vexlife.local.VexLifeProdRuntimeSessionFactory
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
import vexlife.labs.VexLabsNativeTest
import vexlife.model.VexLifeInferenceCurrentState
import vexlife.model.VexLifeInferenceSelection
import vexlife.model.VexLifeInferenceSessionAdmission
import vexlife.model.VexLifeModelSourceProjection
import vexlife.model.VexLifeModels
import vexlife.model.VexLifeModelsFamily
import vexlife.model.VexLifePartnerModelsCurrentState
import vexlife.model.VexLifeVisualInputQualification
import vexlife.model.heretic.HereticCompanionRepresentation
import vexlife.model.heretic.HereticModelConfiguration
import vexlife.model.heretic.HereticModelOfferings
import vexlife.model.heretic.runtime.VexLifeVisualInterpreter
import vexlife.model.heretic.runtime.VexLifeVisualPythonEnvironment
import vexlife.memory.VexLifeCurrentMemoryProviderSelection
import vexlife.partner.VexLifePartnerConnectionConfiguration
import vexlife.partner.VexLifePartnerMacHostRealization
import vexlife.partner.VexLifePartnerNativeHostClient
import vexlife.memory.VexLifeCurrentMemoryReader
import vexlife.continuity.VexLifePrivateConversation
import vexlife.continuity.VexLifePrivateModeFamily
import vexlife.continuity.VexLifeWorkingContinuityOwner
import vexlife.context.VexLifeContextProfile
import vexlife.context.VexLifeContextProfileFamily
import vexlife.context.VexLifeContextProfileStore
import vexlife.memory.VexLifeDailyMemoryDream
import vexlife.person.VexLifeLanguageFamily
import vexlife.person.VexLifePersonSettings
import vexlife.person.VexLifePersonSettingsStore
import vexlife.memory.VexLifeMemoryContract
import vexlife.release.VexLifeReleaseIdentity
import vexlife.release.VexLifeReleaseInboxFamily
import vexlife.release.VexLifeReleaseUpdates
import vexlife.memory.VexLifeMemoryFamily
import vexlife.sources.VexLifeLocalFolderPicker
import vexlife.sources.VexLifeLocalSourceStore
import vexlife.sources.VexLifeLocalSourcesFamily
import vexlife.workspace.VexLifeWorkspaceEffectStore
import vexlife.workspace.VexLifeWorkspaceEffectsFamily
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
    private val startupSetup: VexLifeProdRuntimeSession.Foundation,
    private val server: HttpServer,
    private val frameHost: VexLifeProdFrameHost,
    private val presentation: VexWebArtifacts.Bundle,
    private val executor: ExecutorService,
    private val turnExecutor: TurnExecutor,
    private val streamingTurnExecutor: StreamingTurnExecutor?,
    private val partnerTurnExecutor: PartnerTurnExecutor?,
    private val streamingPartnerTurnExecutor: StreamingPartnerTurnExecutor?,
    private val dreamExecutor: DreamExecutor?,
    private val visionRuntime: VisionRuntime?,
    private val currentStateObserver: (() -> VexLifeProdServiceContract.CurrentStateProjection)?,
    private val memoryReader: (Path) -> VexLifeMemoryContract.Context,
    private val currentQualifiedRuntimeEvidence: () -> HereticModelConfiguration.QualifiedRuntimeEvidence,
    private val currentQualifiedVisualInputEvidence:
        () -> VexLifeVisualInputQualification.QualifiedRuntimeEvidence?,
    private val servingModelRef: () -> String?,
    private val servingRuntimeReceiptRef: () -> String,
    private val servingPromptCacheEntryLimit: () -> Int?,
    private val modelSwitchRealization: VexLifeModels.ModelSwitchRealization?,
    private val partnerSwitchRealization: VexLifeModels.PartnerSwitchRealization?,
    private val partnerInference: VexLifeProdPartnerInference?,
    private val partnerConnectionLiveEffectsEnabled: Boolean,
    private val localFolderPicker: VexLifeLocalFolderPicker,
    private val runtimeSessionClose: (() -> Unit)? = null,
    private val runtimeSessionRelinquishForRestart: (() -> Boolean)? = null,
    private val initialPromptPreparation: VexLifePromptPreparation? = null,
    private val releaseAlertsProjection: () -> VexLifeReleaseInboxFamily.DisplayState,
    private val releaseAlertsControl:
        (
            String,
            VexLifeReleaseIdentity.Coordinate,
        ) -> VexLifeReleaseUpdates.ControlResult,
    privateCompletionOverride: VexLifePrivateConversation.Completion? = null,
) : AutoCloseable {
    private data class ActiveTurn(
        val attempt: VexLifeConversationTurnFamily.Attempt,
        val cancellation: DirectCompanionCancellation,
        /**
         * True only when this active human turn began while the one initial Prompt Preparation
         * window was still pending/reusable. It is not a second preparation owner.
         */
        val initialPromptPreparationTurn: Boolean,
    )

    private class PartnerServingNotCurrent(
        val reason: VexLifeInferenceSessionAdmission.HoldReason,
    ) : IllegalStateException(
            "Partner serving admission is not current: " +
                reason.name,
        )

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

    private val vexLabs =
        VexLabsNativeTest(
            productHome =
                Path.of(
                    startupSetup.productHome,
                ),
            currentRuntimeEvidence =
                currentQualifiedRuntimeEvidence,
            activeContextProfile = {
                contextProfiles.readActive(
                    productFoundationIdentity(),
                )
            },
            personContext =
                ::personContext,
            currentProductConversationHead = {
                DirectConversationStore(
                    Path.of(
                        startupSetup.productHome,
                    ),
                )
                    .current()
                    .head
                    ?.conversationHeadSha256
            },
        )

    private fun localAsrProvider(
        selection: VexLifeLocalAsrSelection.Value,
    ): VexLifeLocalAsrProvider =
        VexLifeLocalAsrProvider.fromHome(
            productHome = localAsrProductHome,
            selection = selection,
            runtime = localAsrRuntime,
        )

    private val activeTurn =
        AtomicReference<ActiveTurn?>(
            null,
        )

    private val localArtifactStore =
        VexLifeLocalArtifactStore(
            Path.of(
                startupSetup.productHome,
            ),
        )

    private val sentResourceReview by lazy {
        VexLifeSentResourceReview(
            Path.of(
                startupSetup.productHome,
            ),
        )
    }

    private val attachmentDraft =
        VexLifeAttachmentDraftRuntime()

    private val localSources =
        VexLifeLocalSourceStore(
            Path.of(
                startupSetup.productHome,
            ),
        )

    private val workspaceEffects =
        VexLifeWorkspaceEffectStore(
            productHome =
                Path.of(
                    startupSetup.productHome,
                ),
            localSources =
                localSources,
        )

    private val localSourcesMutationLock =
        Any()

    private val diagnostics =
        VexLifeDiagnostics(home = Path.of(startupSetup.productHome), sourceSha = sourceSha).also {
            it.startObserving()
        }

    /** Settings → Models projects the current serving Session; runtime effects stay outside it. */
    private val models =
        VexLifeModels(
            productHome = Path.of(startupSetup.productHome),
            servingModelRef = servingModelRef,
            currentQualifiedRuntimeEvidence = currentQualifiedRuntimeEvidence,
            currentQualifiedVisualInputEvidence = currentQualifiedVisualInputEvidence,
            totalMemoryBytes = {
                currentTotalMemoryBytes()
            },
            runtimeRealizations =
                vexlife.model.heretic.HereticRuntimeRealization.Resolver(
                    vexlife.model.heretic.HereticRuntimeRealization.TargetSource(
                        vexlife.platform.VexLifeJvmPlatformTarget::requireCurrent,
                    ),
                ),
            modelSwitchRealization = modelSwitchRealization,
            partnerSwitchRealization = partnerSwitchRealization,
        )

    private val inferenceControlLock =
        Any()

    private fun sourceAwareModelsProjection(): VexLifeInferenceCurrentState.Projection {
        val composition =
            partnerInference

        while (
            true
        ) {
            val stagedBefore =
                models.stagedInferenceTarget()
            val partnerServingBefore =
                models.partnerServing()
            val local =
                models.projection()
            val stagedAfter =
                models.stagedInferenceTarget()
            val partnerServingAfter =
                models.partnerServing()

            if (
                stagedBefore !=
                    stagedAfter ||
                partnerServingBefore !=
                    partnerServingAfter
            ) {
                continue
            }

            return if (
                composition !=
                null
            ) {
                composition.observe(
                    local = local,
                    stagedTarget = stagedAfter,
                    partnerServing = partnerServingAfter,
                    localRuntimeEvidence =
                        runCatching {
                            currentQualifiedRuntimeEvidence()
                        }
                            .getOrNull(),
                )
                    .current
            } else {
                VexLifeInferenceCurrentState.project(
                    productHome =
                        Path.of(
                            startupSetup.productHome,
                        ),
                    local = local,
                    partner =
                        VexLifePartnerModelsCurrentState.Projection(
                            configuredProviders = emptyList(),
                            connectionByProfileRef = emptyMap(),
                            offerings = emptyList(),
                        ),
                    stagedTargetOrNull =
                        stagedAfter
                            ?.takeIf {
                                it.sourceKind ==
                                    VexLifeModelSourceProjection.SourceKind.LOCAL
                            },
                )
            }
        }
    }


    private fun sourceAwareModelControl(
        controlRef: String,
        handle: String,
    ): VexLifeInferenceCurrentState.Projection =
        synchronized(
            inferenceControlLock,
        ) {
            val current =
                sourceAwareModelsProjection()
            val target =
                VexLifeInferenceSelection.requireTarget(
                    models = current.models,
                    handle = handle,
                )

            when (
                controlRef
            ) {
                VexLifeModelsFamily.ControlSelect -> {
                    when (
                        target.sourceKind
                    ) {
                        VexLifeModelSourceProjection.SourceKind.LOCAL ->
                            models.control(
                                controlRef = controlRef,
                                handle = handle,
                            )

                        VexLifeModelSourceProjection.SourceKind.PARTNER -> {
                            require(
                                !current.models.rows
                                    .single {
                                        it.handle ==
                                            handle
                                    }
                                    .targeted,
                            ) {
                                "Partner Select requires a row that is not already targeted"
                            }
                            require(
                                !models.switchInProgress(),
                            ) {
                                "Partner Select is unavailable while a model switch is active"
                            }
                            models.stageInferenceTarget(
                                models = current.models,
                                handle = handle,
                            )
                        }
                    }
                }

                VexLifeModelsFamily.ControlUse -> {
                    val staged =
                        requireNotNull(
                            models.stagedInferenceTarget(),
                        ) {
                            "source-aware Use requires one staged inference target"
                        }
                    require(
                        staged ==
                            target,
                    ) {
                        "source-aware Use handle does not match the exact staged inference target"
                    }

                    when (
                        target.sourceKind
                    ) {
                        VexLifeModelSourceProjection.SourceKind.LOCAL ->
                            models.control(
                                controlRef = controlRef,
                                handle = handle,
                            )

                        VexLifeModelSourceProjection.SourceKind.PARTNER ->
                            models.useStagedPartnerInference(
                                models = current.models,
                                controlRef = controlRef,
                            )
                    }
                }

                else ->
                    throw IllegalArgumentException(
                        "source-aware Models control admits only Select or Use",
                    )
            }

            sourceAwareModelsProjection()
        }

    private fun partnerConnectionAvailability():
        VexLifeProdServiceContract.PartnerConnectionAvailabilityProjection =
        VexLifeProdServiceContract.PartnerConnectionAvailabilityProjection(
            schemaVersion =
                VexLifeProdServiceContract.PartnerConnectionAvailabilitySchema,
            state =
                if (
                    partnerConnectionLiveEffectsEnabled
                ) {
                    VexLifeProdServiceContract.PartnerConnectionAvailabilityReady
                } else {
                    VexLifeProdServiceContract.PartnerConnectionAvailabilityHeldFunding
                },
            connectEnabled =
                partnerConnectionLiveEffectsEnabled,
            liveProviderTestingFunded =
                partnerConnectionLiveEffectsEnabled,
            effectsPerformed = false,
        )

    private fun handlePartnerConnectionControl(
        exchange: HttpExchange,
    ) {
        requireMethod(
            exchange,
            "POST",
        )
        if (
            !partnerConnectionLiveEffectsEnabled
        ) {
            respondError(
                exchange = exchange,
                status = 409,
                code = "PARTNER_CONNECTION_LIVE_TESTING_HELD",
                message =
                    "Partner API connection is built but live provider testing is paused for this phase while funding is unavailable.",
            )
            return
        }
        val composition =
            requireNotNull(
                partnerInference,
            ) {
                "Partner connection configuration is unavailable on this service realization"
            }
        val controlWire =
            requireNotNull(
                exchange.requestHeaders
                    .getFirst(
                        VexLifeProdServiceContract.PartnerConnectionControlHeader,
                    )
                    ?.trim()
                    ?.takeIf(
                        String::isNotEmpty,
                    ),
            ) {
                "Partner connection control requires one control header"
            }
        val control =
            requireNotNull(
                VexLifeProdServiceContract.PartnerConnectionControl.forWire(
                    controlWire,
                ),
            ) {
                "Partner connection control is not recognized"
            }
        val providerRef =
            exchange.requestHeaders
                .getFirst(
                    VexLifeProdServiceContract.PartnerConnectionProviderRefHeader,
                )
                ?.trim()
                ?.takeIf(
                    String::isNotEmpty,
                )
        val workspaceId =
            exchange.requestHeaders
                .getFirst(
                    VexLifeProdServiceContract.PartnerConnectionWorkspaceIdHeader,
                )
                ?.trim()
                ?.takeIf(
                    String::isNotEmpty,
                )

        val results =
            try {
                when (
                    control
                ) {
                    VexLifeProdServiceContract.PartnerConnectionControl.Connect -> {
                        val provider =
                            requireNotNull(
                                providerRef,
                            ) {
                                "Partner connect requires one providerRef header"
                            }
                        val secret =
                            readPartnerConnectionSecret(
                                exchange,
                            )
                        try {
                            listOf(
                                composition.connect(
                                    providerRef = provider,
                                    workspaceId = workspaceId,
                                    secret = secret,
                                ),
                            )
                        } finally {
                            secret.fill(
                                '\u0000',
                            )
                        }
                    }

                    VexLifeProdServiceContract.PartnerConnectionControl.Replace -> {
                        val provider =
                            requireNotNull(
                                providerRef,
                            ) {
                                "Partner replace requires one providerRef header"
                            }
                        val secret =
                            readPartnerConnectionSecret(
                                exchange,
                            )
                        try {
                            listOf(
                                composition.replace(
                                    providerRef = provider,
                                    workspaceId = workspaceId,
                                    secret = secret,
                                ),
                            )
                        } finally {
                            secret.fill(
                                '\u0000',
                            )
                        }
                    }

                    VexLifeProdServiceContract.PartnerConnectionControl.Remove -> {
                        require(
                            workspaceId ==
                                null,
                        ) {
                            "Partner remove does not accept an account-scope header"
                        }
                        requireEmptyRequestBody(
                            exchange,
                        )
                        listOf(
                            composition.remove(
                                requireNotNull(
                                    providerRef,
                                ) {
                                    "Partner remove requires one providerRef header"
                                },
                            ),
                        )
                    }

                    VexLifeProdServiceContract.PartnerConnectionControl.Recover -> {
                        require(
                            providerRef ==
                                null &&
                                workspaceId ==
                                null,
                        ) {
                            "Partner recover does not accept provider/account headers"
                        }
                        requireEmptyRequestBody(
                            exchange,
                        )
                        composition.recoverConnections()
                    }

                    VexLifeProdServiceContract.PartnerConnectionControl.UpdateScope -> {
                        requireEmptyRequestBody(
                            exchange,
                        )
                        listOf(
                            composition.updateAccountScope(
                                providerRef =
                                    requireNotNull(
                                        providerRef,
                                    ) {
                                        "Partner account-scope update requires one providerRef header"
                                    },
                                workspaceId = workspaceId,
                            ),
                        )
                    }
                }
            } catch (
                recovery: VexLifePartnerConnectionConfiguration.RecoveryRequiredException,
            ) {
                respondError(
                    exchange = exchange,
                    status = 409,
                    code = "PARTNER_CONNECTION_RECOVERY_REQUIRED",
                    message =
                        "Partner connection recovery is required for " +
                            recovery.providerRef +
                            " (" +
                            recovery.transitionKind.name +
                            ").",
                )
                return
            }

        respondJson(
            exchange = exchange,
            status = 200,
            value =
                VexLifeProdServiceContract.PartnerConnectionControlResponse(
                    schemaVersion =
                        VexLifeProdServiceContract.PartnerConnectionControlSchema,
                    control = control.wire,
                    results =
                        results.map {
                            result ->
                            VexLifeProdServiceContract.PartnerConnectionControlResult(
                                providerRef = result.providerRef,
                                state = result.state.name,
                                credentialRef = result.credentialRef,
                                recoveryPerformed = result.recoveryPerformed,
                            )
                        },
                    effectsPerformed =
                        results.isNotEmpty(),
                ),
        )
    }

    private fun readPartnerConnectionSecret(
        exchange: HttpExchange,
    ): CharArray {
        requireContentType(
            exchange,
            "application/octet-stream",
        )
        val bytes =
            requestBytes(
                exchange,
                vexlife.partner.VexLifePartnerCredentialCustody.MaximumSecretBytes,
            )
        require(
            bytes.isNotEmpty(),
        ) {
            "Partner credential secret must not be empty"
        }

        return try {
            val decoder =
                StandardCharsets.UTF_8
                    .newDecoder()
                    .onMalformedInput(
                        java.nio.charset.CodingErrorAction.REPORT,
                    )
                    .onUnmappableCharacter(
                        java.nio.charset.CodingErrorAction.REPORT,
                    )
            val buffer =
                decoder.decode(
                    java.nio.ByteBuffer.wrap(
                        bytes,
                    ),
                )
            val secret =
                CharArray(
                    buffer.remaining(),
                )
            buffer.get(
                secret,
            )
            for (
                index in
                0 until
                    buffer.limit()
            ) {
                buffer.put(
                    index,
                    '\u0000',
                )
            }
            secret
        } finally {
            bytes.fill(
                0,
            )
        }
    }

    private fun requireEmptyRequestBody(
        exchange: HttpExchange,
    ) {
        require(
            requestBytes(
                exchange,
                0,
            )
                .isEmpty(),
        ) {
            "Partner connection control does not accept a request body"
        }
    }

    private fun currentPartnerTextTurnOrNull(): DirectCompanionTurn.PartnerTextTurn? {
        val serving =
            models.partnerServing()
                ?: return null
        val composition =
            requireNotNull(
                partnerInference,
            ) {
                "Partner serving truth requires one current Partner service realization"
            }
        val snapshot =
            composition.observe(
                local = models.projection(),
                stagedTarget = models.stagedInferenceTarget(),
                partnerServing = serving,
                localRuntimeEvidence =
                    runCatching {
                        currentQualifiedRuntimeEvidence()
                    }
                        .getOrNull(),
            )

        if (
            snapshot.admission.standing !=
            VexLifeInferenceSessionAdmission.Standing.QUALIFIED
        ) {
            throw PartnerServingNotCurrent(
                requireNotNull(
                    snapshot.admission.holdReason,
                ),
            )
        }
        require(
            snapshot.admission.qualified
                ?.coordinate ==
                serving.coordinate,
        ) {
            "Partner serving admission does not bind Models-owned serving truth"
        }

        return composition.partnerTextTurn(
            snapshot,
        )
    }

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
        /**
         * Release gate for paid external-provider qualification.
         *
         * The Product/UI/service relationship is wired while this is false, but no credential body
         * is read and no protected-host/provider effect is admitted from the human connection route.
         * Reopening this gate requires an intentional source change after live-provider testing is
         * funded and ready to resume.
         */
        private const val PartnerConnectionLiveEffectsEnabledForThisRelease =
            false

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

        private fun currentTarget(): vexlife.platform.VexLifePlatformRealization.Target =
            vexlife.platform.VexLifeJvmPlatformTarget.requireCurrent()

        private fun currentTotalMemoryBytes(): Long? =
            when (
                currentTarget()
            ) {
                vexlife.model.heretic.HereticRuntimeRealization.MacArm64 ->
                    runCatching {
                            vexlife.model.heretic.runtime.HereticMacHostProfile
                                .observeCurrent()
                                .observation
                                .totalMemoryBytes
                        }
                        .getOrNull()

                vexlife.model.heretic.HereticRuntimeRealization.WindowsX64 ->
                    vexlife.platform.windows.VexLifeProdWindowsCurrentState.totalMemoryBytes()

                else ->
                    null
            }

        private fun observeCurrentState(
            productHome: Path,
        ): VexLifeProdServiceContract.CurrentStateProjection =
            when (
                currentTarget()
            ) {
                vexlife.model.heretic.HereticRuntimeRealization.MacArm64 ->
                    VexLifeProdCurrentState.observeCurrentMac(
                        productHome,
                    )

                vexlife.model.heretic.HereticRuntimeRealization.WindowsX64 ->
                    vexlife.platform.windows.VexLifeProdWindowsCurrentState.observe(
                        productHome,
                    )

                else ->
                    error(
                        "VexLife-Prod service has no current-state projector for target " +
                            vexlife.model.heretic.HereticRuntimeRealization.targetRef(
                                currentTarget(),
                            ),
                    )
            }

        private val ProductDreamExecutor =
            object : DreamExecutor {
                override fun observe(
                    setup: VexLifeProdRuntimeSession.Foundation,
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
                    setup: VexLifeProdRuntimeSession.Foundation,
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
            session: VexLifeProdRuntimeSession,
            visionRuntimeExecutablePathOrNull: String?,
            partnerHostExecutablePathOrNull: String? = null,
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

            val initialPromptPreparation =
                VexLifePromptPreparation()

            fun <T> withCurrentSession(
                block: (VexLifeProdRuntimeSession) -> T,
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

            fun visualInterpreterFor(
                current: VexLifeProdRuntimeSession,
            ): DirectCompanionTurn.VisualInterpreter =
                VexLifeVisualInterpreter(
                    productHome =
                        Path.of(
                            current.foundation.productHome,
                        ),
                    currentVisualRuntime = {
                        requireNotNull(
                            VexLifeVisualPythonEnvironment.observeQualifiedCurrent(
                                productHome =
                                    Path.of(
                                        current.foundation.productHome,
                                    ),
                                currentTextRuntime =
                                    current.observeCurrentQualifiedRuntimeEvidence(),
                            ),
                        ) {
                            "static-image interpretation lost its exact qualified visual runtime"
                        }
                    },
                )

            val partnerInference =
                partnerHostExecutablePathOrNull
                    ?.let {
                        helperPath ->
                        require(
                            vexlife.platform.VexLifeJvmPlatformTarget.requireCurrent() ==
                                vexlife.model.heretic.HereticRuntimeRealization.MacArm64,
                        ) {
                            "current Product source admits a packaged Partner helper only on macOS"
                        }
                        val plan =
                            VexLifePartnerMacHostRealization.plan(
                                helperPath,
                            )
                        val client =
                            VexLifePartnerNativeHostClient(
                                VexLifePartnerMacHostRealization.processHost(
                                    plan,
                                ),
                            )
                        VexLifeProdPartnerInference(
                            productHome =
                                Path.of(
                                    session.foundation.productHome,
                                ),
                            host = client,
                            executor = client,
                        )
                    }

            val modelSwitchRealization =
                VexLifeModels.ModelSwitchRealization {
                    realization,
                    observe,
                    ->
                    require(
                        realization.target ==
                            vexlife.platform.VexLifeJvmPlatformTarget.requireCurrent(),
                    ) {
                        "model switch realization target does not match the current JVM host"
                    }
                    observe(
                        VexLifeModels.SwitchPhase.WaitingForCurrentTurn,
                    )
                    initialPromptPreparation.invalidateAndAwait(
                        "MODEL_SWITCH",
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
                            VexLifeProdRuntimeSessionFactory.establishExact(
                                productHome =
                                    Path.of(
                                        current.foundation.productHome,
                                    ),
                                realization = realization,
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
                            next.foundation.qualifiedRuntimeEvidence.realizationRef ==
                                realization.realizationRef,
                        ) {
                            "model switch qualified a runtime other than the selected realization"
                        }
                        sessionRef.set(
                            next,
                        )
                        initialPromptPreparation.startCurrent(
                            next.foundation.qualifiedRuntimeEvidence,
                        )
                    } finally {
                        write.unlock()
                    }
                }

            val partnerSwitchRealization =
                partnerInference
                    ?.let {
                        composition ->
                        VexLifeModels.PartnerSwitchRealization {
                            target,
                            observe,
                            ->
                            observe(
                                VexLifeModels.SwitchPhase.WaitingForCurrentTurn,
                            )
                            initialPromptPreparation.invalidateAndAwait(
                                "MODEL_SWITCH",
                            )
                            val write =
                                sessionFence.writeLock()
                            write.lock()
                            try {
                                observe(
                                    VexLifeModels.SwitchPhase.PreparingTarget,
                                )
                                composition.qualify(
                                    target,
                                )
                            } finally {
                                write.unlock()
                            }
                        }
                    }

            return startAt(
                sourceSha = sourceSha,
                startupSetup = session.foundation,
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
                                visualInterpreter =
                                    visualInterpreterFor(
                                        current,
                                    ),
                                initialPromptPreparation =
                                    initialPromptPreparation,
                            )
                        }
                    },
                streamingTurnExecutor =
                    StreamingTurnExecutor {
                        _,
                        intent,
                        observer,
                        cancellation,
                        ->
                        withCurrentSession {
                            current ->
                            DirectCompanionTurn.perform(
                                session = current,
                                intent = intent,
                                progressObserver = observer,
                                cancellation = cancellation,
                                visualInterpreter =
                                    visualInterpreterFor(
                                        current,
                                    ),
                                initialPromptPreparation =
                                    initialPromptPreparation,
                            )
                        }
                    },
                partnerTurnExecutor =
                    PartnerTurnExecutor {
                        _,
                        intent,
                        partnerTextTurn,
                        ->
                        withCurrentSession {
                            current ->
                            DirectCompanionTurn.perform(
                                session = current,
                                intent = intent,
                                partnerTextTurn = partnerTextTurn,
                                initialPromptPreparation =
                                    initialPromptPreparation,
                            )
                        }
                    },
                streamingPartnerTurnExecutor =
                    StreamingPartnerTurnExecutor {
                        _,
                        intent,
                        partnerTextTurn,
                        observer,
                        cancellation,
                        ->
                        withCurrentSession {
                            current ->
                            DirectCompanionTurn.perform(
                                session = current,
                                intent = intent,
                                progressObserver = observer,
                                cancellation = cancellation,
                                partnerTextTurn = partnerTextTurn,
                                initialPromptPreparation =
                                    initialPromptPreparation,
                            )
                        }
                    },
                dreamExecutor = ProductDreamExecutor,
                currentStateObserver = {
                    observeCurrentState(
                        Path.of(
                            sessionRef
                                .get()
                                .foundation
                                .productHome,
                        ),
                    )
                },
                visionRuntime = visionRuntime,
                servingModelRef = {
                    sessionRef
                        .get()
                        .foundation
                        .qualifiedRuntimeEvidence
                        .modelRef
                },
                currentQualifiedRuntimeEvidence = {
                    withCurrentSession {
                        current ->
                        current.currentQualifiedRuntimeEvidence()
                    }
                },
                currentQualifiedVisualInputEvidence = {
                    withCurrentSession {
                        current ->
                        VexLifeVisualPythonEnvironment.qualifyCurrent(
                            productHome =
                                Path.of(
                                    current.foundation.productHome,
                                ),
                            currentTextRuntime =
                                current.observeCurrentQualifiedRuntimeEvidence(),
                        )
                    }
                },
                servingRuntimeReceiptRef = {
                    sessionRef
                        .get()
                        .foundation
                        .qualifiedRuntimeEvidence
                        .runtimeReceiptRef
                },
                servingPromptCacheEntryLimit = {
                    sessionRef
                        .get()
                        .foundation
                        .promptCacheEntryLimit
                },
                modelSwitchRealization = modelSwitchRealization,
                partnerSwitchRealization = partnerSwitchRealization,
                partnerInference = partnerInference,
                partnerConnectionLiveEffectsEnabled =
                    PartnerConnectionLiveEffectsEnabledForThisRelease,
                initialPromptPreparation = initialPromptPreparation,
                runtimeSessionClose = {
                    sessionRef
                        .get()
                        .close()
                },
                runtimeSessionRelinquishForRestart = {
                    if (
                        vexlife.platform.VexLifeJvmPlatformTarget.requireCurrent() ==
                            vexlife.model.heretic.HereticRuntimeRealization.MacArm64
                    ) {
                        VexLifeProdMacRuntimeHandoff.consumeForServiceClose(
                            productHome =
                                Path.of(
                                    sessionRef
                                        .get()
                                        .foundation
                                        .productHome,
                                ),
                            serviceSourceSha =
                                sourceSha,
                        )
                    } else {
                        false
                    }
                },
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
            partnerTurnExecutor: PartnerTurnExecutor? =
                null,
            streamingPartnerTurnExecutor: StreamingPartnerTurnExecutor? =
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
            currentQualifiedRuntimeEvidence: (() -> HereticModelConfiguration.QualifiedRuntimeEvidence)? = null,
            currentQualifiedVisualInputEvidence:
                (() -> VexLifeVisualInputQualification.QualifiedRuntimeEvidence?)? =
                null,
            servingRuntimeReceiptRef: (() -> String)? = null,
            servingPromptCacheEntryLimit: (() -> Int?)? = null,
            modelSwitchRealization: VexLifeModels.ModelSwitchRealization? = null,
            partnerSwitchRealization: VexLifeModels.PartnerSwitchRealization? = null,
            partnerInference: VexLifeProdPartnerInference? = null,
            partnerConnectionLiveEffectsEnabled: Boolean = false,
            initialPromptPreparation: VexLifePromptPreparation? = null,
            localFolderPicker: VexLifeLocalFolderPicker =
                VexLifeLocalFolderPicker.SystemDesktop,
            runtimeSessionClose: (() -> Unit)? = null,
            runtimeSessionRelinquishForRestart: (() -> Boolean)? = null,
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
                startupSetup =
                    VexLifeProdRuntimeSession.Foundation(
                        state = setup.state,
                        productHome = setup.productHome,
                        qualifiedRuntimeEvidence = setup.qualifiedRuntimeEvidence,
                        promptCacheEntryLimit = setup.promptCacheEntryLimit,
                    ),
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
                partnerTurnExecutor = partnerTurnExecutor,
                streamingPartnerTurnExecutor = streamingPartnerTurnExecutor,
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
                currentQualifiedRuntimeEvidence =
                    currentQualifiedRuntimeEvidence
                        ?: {
                            setup.qualifiedRuntimeEvidence
                        },
                currentQualifiedVisualInputEvidence =
                    currentQualifiedVisualInputEvidence
                        ?: {
                            null
                        },
                servingRuntimeReceiptRef =
                    servingRuntimeReceiptRef
                        ?: {
                            setup.qualifiedRuntimeEvidence.runtimeReceiptRef
                        },
                servingPromptCacheEntryLimit =
                    servingPromptCacheEntryLimit
                        ?: {
                            setup.promptCacheEntryLimit
                        },
                modelSwitchRealization = modelSwitchRealization,
                partnerSwitchRealization = partnerSwitchRealization,
                partnerInference = partnerInference,
                partnerConnectionLiveEffectsEnabled =
                    partnerConnectionLiveEffectsEnabled,
                initialPromptPreparation = initialPromptPreparation,
                localFolderPicker = localFolderPicker,
                runtimeSessionClose = runtimeSessionClose,
                runtimeSessionRelinquishForRestart =
                    runtimeSessionRelinquishForRestart,
                releaseAlertsProjection = releaseAlertsProjection,
                releaseAlertsControl = releaseAlertsControl,
            )

        private fun startAt(
            sourceSha: String,
            startupSetup: VexLifeProdRuntimeSession.Foundation,
            address: InetSocketAddress,
            frameAddress: InetSocketAddress,
            turnExecutor: TurnExecutor,
            streamingTurnExecutor: StreamingTurnExecutor?,
            partnerTurnExecutor: PartnerTurnExecutor? = null,
            streamingPartnerTurnExecutor: StreamingPartnerTurnExecutor? = null,
            dreamExecutor: DreamExecutor?,
            visionRuntime: VisionRuntime?,
            currentStateObserver: (() -> VexLifeProdServiceContract.CurrentStateProjection)?,
            memoryReader: (Path) -> VexLifeMemoryContract.Context = ProductMemoryReader,
            privateCompletion: VexLifePrivateConversation.Completion? = null,
            servingModelRef: () -> String? = {
                startupSetup.qualifiedRuntimeEvidence.modelRef
            },
            currentQualifiedRuntimeEvidence: () -> HereticModelConfiguration.QualifiedRuntimeEvidence = {
                startupSetup.qualifiedRuntimeEvidence
            },
            currentQualifiedVisualInputEvidence:
                () -> VexLifeVisualInputQualification.QualifiedRuntimeEvidence? = {
                    null
                },
            servingRuntimeReceiptRef: () -> String = {
                startupSetup.qualifiedRuntimeEvidence.runtimeReceiptRef
            },
            servingPromptCacheEntryLimit: () -> Int? = {
                startupSetup.promptCacheEntryLimit
            },
            modelSwitchRealization: VexLifeModels.ModelSwitchRealization? = null,
            partnerSwitchRealization: VexLifeModels.PartnerSwitchRealization? = null,
            partnerInference: VexLifeProdPartnerInference? = null,
            partnerConnectionLiveEffectsEnabled: Boolean =
                PartnerConnectionLiveEffectsEnabledForThisRelease,
            initialPromptPreparation: VexLifePromptPreparation? = null,
            localFolderPicker: VexLifeLocalFolderPicker =
                VexLifeLocalFolderPicker.SystemDesktop,
            runtimeSessionClose: (() -> Unit)? = null,
            runtimeSessionRelinquishForRestart: (() -> Boolean)? = null,
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
                        partnerTurnExecutor = partnerTurnExecutor,
                        streamingPartnerTurnExecutor = streamingPartnerTurnExecutor,
                        dreamExecutor = dreamExecutor,
                        visionRuntime = visionRuntime,
                        currentStateObserver = currentStateObserver,
                        memoryReader = memoryReader,
                        currentQualifiedRuntimeEvidence = currentQualifiedRuntimeEvidence,
                        currentQualifiedVisualInputEvidence =
                            currentQualifiedVisualInputEvidence,
                        servingModelRef = servingModelRef,
                        servingRuntimeReceiptRef = servingRuntimeReceiptRef,
                        servingPromptCacheEntryLimit = servingPromptCacheEntryLimit,
                        modelSwitchRealization = modelSwitchRealization,
                        partnerSwitchRealization = partnerSwitchRealization,
                        partnerInference = partnerInference,
                        partnerConnectionLiveEffectsEnabled =
                            partnerConnectionLiveEffectsEnabled,
                        initialPromptPreparation = initialPromptPreparation,
                        localFolderPicker = localFolderPicker,
                        runtimeSessionClose = runtimeSessionClose,
                        runtimeSessionRelinquishForRestart =
                            runtimeSessionRelinquishForRestart,
                        releaseAlertsProjection = realizedReleaseAlertsProjection,
                        releaseAlertsControl = realizedReleaseAlertsControl,
                        privateCompletionOverride = privateCompletion,
                    )

                initialPromptPreparation
                    ?.attach(
                        executor = executor,
                        prefixProvider =
                            VexLifePromptPreparation.PrefixProvider {
                                runtime ->
                                service.currentPromptPreparationPrefix(
                                    runtime,
                                )
                            },
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
                service.startPromptPreparation()

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
            setup: VexLifeProdRuntimeSession.Foundation,
            intent: DirectCompanionTurn.Intent,
        ): DirectCompanionTurn.Result
    }

    internal fun interface StreamingTurnExecutor {
        fun perform(
            setup: VexLifeProdRuntimeSession.Foundation,
            intent: DirectCompanionTurn.Intent,
            progressObserver: DirectCompanionTurn.ProgressObserver,
            cancellation: DirectCompanionCancellation,
        ): DirectCompanionTurn.Result
    }

    internal fun interface PartnerTurnExecutor {
        fun perform(
            setup: VexLifeProdRuntimeSession.Foundation,
            intent: DirectCompanionTurn.Intent,
            partnerTextTurn: DirectCompanionTurn.PartnerTextTurn,
        ): DirectCompanionTurn.Result
    }

    internal fun interface StreamingPartnerTurnExecutor {
        fun perform(
            setup: VexLifeProdRuntimeSession.Foundation,
            intent: DirectCompanionTurn.Intent,
            partnerTextTurn: DirectCompanionTurn.PartnerTextTurn,
            progressObserver: DirectCompanionTurn.ProgressObserver,
            cancellation: DirectCompanionCancellation,
        ): DirectCompanionTurn.Result
    }

    internal interface DreamExecutor {
        fun observe(
            setup: VexLifeProdRuntimeSession.Foundation,
        ): VexLifeProdServiceContract.MemoryDreamProjection

        fun perform(
            setup: VexLifeProdRuntimeSession.Foundation,
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
        runCatching { initialPromptPreparation?.close() }
        runCatching { vexLabs.close() }
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
        val activeTurnBlocksRuntimeRelinquish =
            activeTurn.get() !=
                null
        executor.shutdownNow()
        val runtimeReleaseRelinquished =
            !activeTurnBlocksRuntimeRelinquish &&
                runCatching {
                    runtimeSessionRelinquishForRestart
                        ?.invoke() ==
                        true
                }
                    .getOrDefault(
                        false,
                    )
        var runtimeCloseFailure: Throwable? =
            null
        if (
            !runtimeReleaseRelinquished
        ) {
            try {
                runtimeSessionClose
                    ?.invoke()
            } catch (
                error: Throwable,
            ) {
                runtimeCloseFailure =
                    error
            }
        }

        if (
            runtimeCloseFailure !=
                null
        ) {
            throw IllegalStateException(
                "VexLife-Prod runtime Session did not close with the service",
                runtimeCloseFailure,
            )
        }

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

                VexLifeProdServiceContract.ConversationTurnPath ->
                    handleConversationTurn(
                        exchange,
                    )

                VexLifeProdServiceContract.ConversationPath ->
                    handleConversation(
                        exchange,
                    )

                VexLifeProdServiceContract.ConversationResourceMetadataPath ->
                    handleConversationResourceMetadata(
                        exchange,
                    )

                VexLifeProdServiceContract.ConversationResourceContentPath ->
                    handleConversationResourceContent(
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

                VexLifeProdServiceContract.VexLabsPath -> {
                    requireMethod(
                        exchange,
                        "GET",
                    )
                    respondJson(
                        exchange = exchange,
                        status = 200,
                        value =
                            vexLabs.projection(),
                    )
                }

                VexLifeProdServiceContract.VexLabsControlPath -> {
                    requireMethod(
                        exchange,
                        "POST",
                    )
                    requireJson(
                        exchange,
                    )
                    respondJson(
                        exchange = exchange,
                        status = 200,
                        value =
                            vexLabs.control(
                                decodeRequest(
                                    exchange,
                                ),
                            ),
                    )
                }

                VexLifeProdServiceContract.VexLabsSendPath -> {
                    requireMethod(
                        exchange,
                        "POST",
                    )
                    requireJson(
                        exchange,
                    )
                    try {
                        respondJson(
                            exchange = exchange,
                            status = 200,
                            value =
                                vexLabs.send(
                                    decodeRequest(
                                        exchange,
                                    ),
                                ),
                        )
                    } catch (
                        unavailable: DirectCompanionConnection.ConnectionException,
                    ) {
                        respondError(
                            exchange,
                            503,
                            "VEX_LABS_MODEL_UNAVAILABLE",
                            "The Lab could not reach the current native Vex runtime. Your real Conversation and Memory were not changed.",
                        )
                    }
                }

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

                VexLifeProdServiceContract.ModelsSourcesPath -> {
                    requireMethod(exchange, "GET")
                    respondJson(
                        exchange = exchange,
                        status = 200,
                        value =
                            sourceAwareModelsProjection(),
                    )
                }

                VexLifeProdServiceContract.ModelsSourcesControlPath -> {
                    requireMethod(exchange, "POST")
                    requireJson(exchange)
                    val request = decodeRequest<ModelControlRequest>(exchange)
                    require(
                        request.promptCacheEntries ==
                            null,
                    ) {
                        "source-aware Models control does not own Local runtime configuration"
                    }
                    respondJson(
                        exchange = exchange,
                        status = 200,
                        value =
                            sourceAwareModelControl(
                                controlRef = request.controlRef,
                                handle = request.handle,
                            ),
                    )
                }

                VexLifeProdServiceContract.PartnerConnectionsPath -> {
                    requireMethod(
                        exchange,
                        "GET",
                    )
                    respondJson(
                        exchange = exchange,
                        status = 200,
                        value = partnerConnectionAvailability(),
                    )
                }

                VexLifeProdServiceContract.PartnerConnectionsControlPath ->
                    handlePartnerConnectionControl(
                        exchange,
                    )

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

                VexLifeProdServiceContract.ConversationCancelPath ->
                    handleConversationCancel(
                        exchange,
                    )

                VexLifeProdServiceContract.AttachmentDraftPath ->
                    handleAttachmentDraft(
                        exchange,
                    )

                VexLifeProdServiceContract.AttachmentStagePath ->
                    handleAttachmentStage(
                        exchange,
                    )

                VexLifeProdServiceContract.AttachmentControlPath ->
                    handleAttachmentControl(
                        exchange,
                    )

                VexLifeProdServiceContract.LocalSourcesPath ->
                    handleLocalSources(
                        exchange,
                    )

                VexLifeProdServiceContract.LocalSourcesControlPath ->
                    handleLocalSourcesControl(
                        exchange,
                    )

                VexLifeProdServiceContract.WorkspaceEffectsPath ->
                    handleWorkspaceEffects(
                        exchange,
                    )

                VexLifeProdServiceContract.WorkspaceEffectsControlPath ->
                    handleWorkspaceEffectsControl(
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

        // Service readiness/currentness must depend only on canonical Conversation ownership,
        // never on optional historical resource projection. A malformed or unavailable resource
        // sidecar is presentation history truth and must not keep a healthy source-bound service
        // from becoming current.
        val currentConversationHeadSha256 =
            DirectConversationStore(
                Path.of(
                    startupSetup.productHome,
                ),
            )
                .current()
                .head
                ?.conversationHeadSha256

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
                        currentConversationHeadSha256,
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
            scopeState = VexLifeVisionFamily.ScopeState.fromWire(projected?.projectionOrNull?.state),
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

    private fun conversationTurnState():
        VexLifeConversationTurnFamily.State {
        val active =
            activeTurn.get()
        return VexLifeConversationTurnFamily.observe(
            activeAttemptRef =
                active
                    ?.attempt
                    ?.ref,
            cancellationRequested =
                active
                    ?.cancellation
                    ?.cancellationRequested
                    ?: false,
        )
    }

    private fun followUpReadinessProjection(
        active: ActiveTurn?,
    ): VexLifeProdServiceContract.FollowUpReadinessProjection {
        val productHome =
            Path.of(
                startupSetup.productHome,
            )
        val promptCacheEntryLimit =
            servingPromptCacheEntryLimit()
        val currentRuntimeReceiptRef =
            servingRuntimeReceiptRef()
        val latestCompletedResponse =
            DirectConversationStore(
                productHome,
            )
                .current()
                .canonicalEvents
                .lastOrNull {
                    it.eventKind ==
                        VexLifeConversationContract.ResponseKind
                }
        val lastCompletedEvidence =
            latestCompletedResponse
                ?.turnRef
                ?.let {
                    turnRef ->
                    DirectTurnContextStore(
                        productHome,
                    )
                        .read(
                            turnRef,
                        )
                }
                ?.takeIf {
                    it.runtimeObservation.runtimeReceiptRef ==
                        currentRuntimeReceiptRef
                }
        val measurement =
            lastCompletedEvidence
                ?.providerMeasurement
                ?.let {
                    observed ->
                    VexLifeProdServiceContract.ProviderMeasurementProjection(
                        stop = observed.stop,
                        finishReason = observed.finishReason,
                        promptTokens = observed.promptTokens,
                        cachedPromptTokens = observed.cachedPromptTokens,
                        prefillTokens = observed.prefillTokens,
                        completionTokens = observed.completionTokens,
                        firstTokenMillis = observed.firstTokenMillis,
                        firstAnswerMillis = observed.firstAnswerMillis,
                        totalMillis = observed.totalMillis,
                    )
                }
        val successorResetPending =
            latestCompletedResponse
                ?.let {
                    response ->
                    VexLifeWorkingContinuityOwner(
                        productHome,
                    )
                        .readCurrent(
                            lineageRef = response.companionLineageRef,
                            threadRef = response.threadRef,
                        )
                        ?.commitReceipt
                        ?.successorContextRequired
                } ==
            true
        val state =
            when {
                promptCacheEntryLimit == null ->
                    VexLifeProdServiceContract.FollowUpReadinessUnavailable

                promptCacheEntryLimit < 2 ->
                    VexLifeProdServiceContract.FollowUpReadinessLowerMemory

                active != null ->
                    VexLifeProdServiceContract.FollowUpReadinessPreparing

                successorResetPending ->
                    VexLifeProdServiceContract.FollowUpReadinessSuccessorReset

                measurement != null ->
                    VexLifeProdServiceContract.FollowUpReadinessFasterEnabled

                else ->
                    VexLifeProdServiceContract.FollowUpReadinessNotYetPrepared
            }
        return VexLifeProdServiceContract.FollowUpReadinessProjection(
            state = state,
            lastCompletedProviderMeasurement = measurement,
        )
    }

    private fun conversationTurnProjection():
        VexLifeProdServiceContract.ConversationTurnProjection {
        val state =
            conversationTurnState()
        val active =
            activeTurn.get()
        return VexLifeProdServiceContract.ConversationTurnProjection(
            schemaVersion =
                VexLifeProdServiceContract.ConversationTurnProjectionSchema,
            state = state.stateRef,
            display =
                VexLifeConversationTurnFamily.displayStateFor(
                    state,
                ),
            effectsPerformed = false,
            initialPromptPreparationPendingForNextTurn =
                initialPromptPreparation
                    ?.nextTurnPreparationPending() ==
                    true,
            initialPromptPreparationForActiveTurn =
                active
                    ?.initialPromptPreparationTurn ==
                    true,
            followUpReadiness =
                followUpReadinessProjection(
                    active,
                ),
        )
    }

    private fun handleConversationTurn(
        exchange: HttpExchange,
    ) {
        requireMethod(
            exchange,
            "GET",
        )
        respondJson(
            exchange = exchange,
            status = 200,
            value = conversationTurnProjection(),
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

    private data class ConversationResourceRequestIdentity(
        val requestSequence: Long,
        val requestEventRef: String,
        val requestEventHash: String,
        val resourceRef: String,
    )

    private fun conversationResourceRequestIdentity(
        exchange: HttpExchange,
    ): ConversationResourceRequestIdentity {
        fun requiredHeader(
            name: String,
        ): String =
            exchange.requestHeaders
                .getFirst(
                    name,
                )
                ?.trim()
                ?.takeIf(
                    String::isNotEmpty,
                )
                ?: throw IllegalArgumentException(
                    "historical resource review requires $name",
                )

        val requestSequence =
            requiredHeader(
                VexLifeProdServiceContract.ConversationResourceSequenceHeader,
            )
                .toLongOrNull()
                ?.takeIf {
                    it >=
                        0
                }
                ?: throw IllegalArgumentException(
                    "historical resource review requires one non-negative request sequence",
                )
        val requestEventRef =
            requiredHeader(
                VexLifeProdServiceContract.ConversationResourceEventRefHeader,
            )
        val requestEventHash =
            requiredHeader(
                VexLifeProdServiceContract.ConversationResourceEventHashHeader,
            )
        require(
            requestEventHash.matches(
                Regex(
                    "[0-9a-f]{64}",
                ),
            ),
        ) {
            "historical resource review request event hash must be one lowercase SHA-256"
        }
        val resourceRef =
            requiredHeader(
                VexLifeProdServiceContract.ConversationResourceRefHeader,
            )
        VexLifeProdServiceContract.requirePortableRef(
            resourceRef,
            "resourceRef",
        )

        return ConversationResourceRequestIdentity(
            requestSequence = requestSequence,
            requestEventRef = requestEventRef,
            requestEventHash = requestEventHash,
            resourceRef = resourceRef,
        )
    }

    private fun handleConversationResourceMetadata(
        exchange: HttpExchange,
    ) {
        requireMethod(
            exchange,
            "GET",
        )
        val identity =
            conversationResourceRequestIdentity(
                exchange,
            )
        when (
            val lookup =
                sentResourceReview.metadata(
                    requestSequence = identity.requestSequence,
                    requestEventRef = identity.requestEventRef,
                    requestEventHash = identity.requestEventHash,
                    resourceRef = identity.resourceRef,
                )
        ) {
            is VexLifeSentResourceReview.Lookup.Found ->
                respondJson(
                    exchange = exchange,
                    status = 200,
                    value = lookup.value,
                )

            is VexLifeSentResourceReview.Lookup.NotFound ->
                respondError(
                    exchange = exchange,
                    status = 404,
                    code = "CONVERSATION_RESOURCE_NOT_FOUND",
                    message = lookup.reason,
                )

            is VexLifeSentResourceReview.Lookup.Refused ->
                respondError(
                    exchange = exchange,
                    status = 409,
                    code = "CONVERSATION_RESOURCE_NOT_ADMITTED",
                    message = lookup.reason,
                )
        }
    }

    private fun handleConversationResourceContent(
        exchange: HttpExchange,
    ) {
        requireMethod(
            exchange,
            "GET",
        )
        val identity =
            conversationResourceRequestIdentity(
                exchange,
            )
        when (
            val lookup =
                sentResourceReview.content(
                    requestSequence = identity.requestSequence,
                    requestEventRef = identity.requestEventRef,
                    requestEventHash = identity.requestEventHash,
                    resourceRef = identity.resourceRef,
                )
        ) {
            is VexLifeSentResourceReview.Lookup.Found ->
                respondBytes(
                    exchange = exchange,
                    status = 200,
                    bytes = lookup.value.bytes,
                    contentType = lookup.value.contentType,
                )

            is VexLifeSentResourceReview.Lookup.NotFound ->
                respondError(
                    exchange = exchange,
                    status = 404,
                    code = "CONVERSATION_RESOURCE_NOT_FOUND",
                    message = lookup.reason,
                )

            is VexLifeSentResourceReview.Lookup.Refused ->
                respondError(
                    exchange = exchange,
                    status = 409,
                    code = "CONVERSATION_RESOURCE_NOT_AVAILABLE",
                    message = lookup.reason,
                )
        }
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

    /**
     * Re-form the exact current stable provider prefix for one qualified local runtime Session.
     *
     * The prefix is always read from current Foundation / Context Profile / Memory / representation
     * owners. Prompt preparation may cache those exact bytes, but it never becomes their authority.
     */
    private fun currentPromptPreparationPrefix(
        runtime: HereticModelConfiguration.QualifiedRuntimeEvidence,
    ): DirectPromptContext.InitialBootPrefix {
        val productHome =
            Path.of(
                startupSetup.productHome,
            )
        val entry =
            HereticModelOfferings.requireEntry(
                productHome,
                runtime.modelRef,
            )
        val providerNativeTools =
            entry.companionProfile.capabilityRepresentation ==
                HereticCompanionRepresentation.CapabilityRepresentation.PROVIDER_NATIVE_TOOLS
        val capabilityRequestsAdmitted =
            entry.companionProfile.reachRepresentationStanding ==
                HereticCompanionRepresentation.ReachRepresentationStanding.QUALIFIED
        return DirectPromptContext.projectInitialBootPrefix(
            foundationalField =
                VexLifeFoundationalField.readCurrent(),
            contextProfile =
                contextProfiles.readActive(
                    productFoundationIdentity(),
                ),
            memoryProviderSelection =
                VexLifeCurrentMemoryProviderSelection.read(
                    productHome = productHome,
                    memoryTokenCap =
                        VexLifeMemoryContract.MaximumMemoryTokens,
                ),
            capabilityRequestsAdmitted =
                capabilityRequestsAdmitted,
            providerNativeTools =
                providerNativeTools,
        )
    }

    /** Best-effort preparation never controls Product availability. */
    private fun startPromptPreparation() {
        initialPromptPreparation
            ?.startCurrent(
                currentQualifiedRuntimeEvidence(),
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

    /** Observes the person's language choices as VexLifeLanguageFamily State, fresh from the Home. */
    private fun languageState(): VexLifeLanguageFamily.State =
        VexLifeLanguageFamily.stateFor(personSettings.read())

    private fun settingsProjection(): VexLifeProdServiceContract.SettingsProjection {
        val state = languageState()
        val display = VexLifeLanguageFamily.displayStateFor(state)
        return VexLifeProdServiceContract.SettingsProjection(
            schemaVersion = VexLifeProdServiceContract.SettingsSchema,
            replyLanguage = display.replyLanguage,
            languages =
                VexLifePersonSettings.ReplyLanguages.map {
                    VexLifeProdServiceContract.SettingsLanguage(
                        it.first,
                        it.second,
                    )
                },
            screenLanguage = (state as? VexLifeLanguageFamily.ScreenChosen)?.screenLanguage,
            screenLanguageShown = VexLifeStrings.selectLanguage("workspace", display.screenLanguage),
            screenLanguages =
                offeredScreenLanguages().map {
                    VexLifeProdServiceContract.SettingsLanguage(it.first, it.second)
                },
        )
    }

    /** Screen languages every scope is complete in: the only ones the screens may be set to. */
    private fun offeredScreenLanguages(): List<Pair<String, String>> {
        val complete = VexLifeStrings.ScreenLanguageScopes.map { VexLifeStrings.catalog.completeLanguages(it).toSet() }
        return VexLifePersonSettings.ScreenLanguages.filter { language -> complete.all { language.first in it } }
    }

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
            VexLifePersonSettings.screenLanguageFor(
                runCatching { personSettings.read() }.getOrDefault(VexLifePersonSettings.Default),
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
                // One update names one choice; VexLifeLanguageFamily decides how it moves, and the
                // other choice is never part of the Control.
                val control =
                    when (val screen = update.screenLanguage) {
                        null -> VexLifeLanguageFamily.ChooseReplyLanguage(requireNotNull(update.replyLanguage))
                        VexLifePersonSettings.FollowReplyLanguage -> VexLifeLanguageFamily.FollowReplyLanguage
                        else -> {
                            require(offeredScreenLanguages().any { it.first == screen }) {
                                "screen language $screen is not complete"
                            }
                            VexLifeLanguageFamily.ChooseScreenLanguage(screen)
                        }
                    }
                val act =
                    requireNotNull(VexLifeLanguageFamily.resolve(languageState(), control)) {
                        "language control is not lawful now"
                    }
                act.admittedState?.let { admitted ->
                    when (act.effect) {
                        VexLifeLanguageFamily.Effect.SaveChoices ->
                            personSettings.write(VexLifeLanguageFamily.valueFor(admitted))
                        null -> Unit
                    }
                }
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
    private fun handleConversationCancel(
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
            JsonCodec.decodeFromString<VexLifeProdServiceContract.ConversationCancelRequest>(
                requestText(
                    exchange,
                ),
            )
        val attempt =
            VexLifeConversationTurnFamily.Attempt(
                request.attemptRef,
            )
        val act =
            VexLifeConversationTurnFamily.resolve(
                currentState =
                    conversationTurnState(),
                control =
                    VexLifeConversationTurnFamily.Stop(
                        attempt,
                    ),
            )
        val effect =
            act
                ?.effect as?
                VexLifeConversationTurnFamily.RequestCancel

        if (
            effect ==
                null
        ) {
            respondJson(
                exchange = exchange,
                status = 200,
                value =
                    VexLifeProdServiceContract.ConversationCancelResponse(
                        schemaVersion =
                            VexLifeProdServiceContract.ConversationCancelResponseSchema,
                        controlRef =
                            VexLifeConversationTurnFamily.ControlStop,
                        attemptRef = attempt.ref,
                        outcome =
                            VexLifeProdServiceContract.ConversationControlOutcome.NOT_LAWFUL_NOW,
                        turn = conversationTurnProjection(),
                        effectsPerformed = false,
                    ),
            )
            return
        }

        val active =
            activeTurn.get()
        val realized =
            active !=
                null &&
                active.attempt ==
                effect.attempt &&
                active.cancellation.request()

        val projection =
            conversationTurnProjection()
        val outcome =
            if (
                realized
            ) {
                VexLifeProdServiceContract.ConversationControlOutcome.REALIZED
            } else {
                VexLifeProdServiceContract.ConversationControlOutcome.NOT_LAWFUL_NOW
            }

        respondJson(
            exchange = exchange,
            status = 200,
            value =
                VexLifeProdServiceContract.ConversationCancelResponse(
                    schemaVersion =
                        VexLifeProdServiceContract.ConversationCancelResponseSchema,
                    controlRef =
                        VexLifeConversationTurnFamily.ControlStop,
                    attemptRef = attempt.ref,
                    outcome = outcome,
                    turn = projection,
                    effectsPerformed = realized,
                ),
        )
    }

    private fun localSourcesProjection(): VexLifeProdServiceContract.LocalSourcesProjection {
        val state =
            localSources.state()
        return VexLifeProdServiceContract.LocalSourcesProjection(
            schemaVersion =
                VexLifeProdServiceContract.LocalSourcesProjectionSchema,
            state = state.stateRef,
            display =
                VexLifeLocalSourcesFamily.displayStateFor(
                    state,
                ),
            grants = state.grants,
            effectsPerformed = false,
        )
    }

    private fun handleLocalSources(
        exchange: HttpExchange,
    ) {
        requireMethod(
            exchange,
            "GET",
        )
        respondJson(
            exchange = exchange,
            status = 200,
            value = localSourcesProjection(),
        )
    }

    private fun handleLocalSourcesControl(
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
            decodeRequest<VexLifeProdServiceContract.LocalSourcesControlRequest>(
                exchange,
            )
        val control =
            requireNotNull(
                VexLifeLocalSourcesFamily.controlForRef(
                    controlRef = request.controlRef,
                    sourceRef = request.sourceRef,
                ),
            )

        fun response(
            outcome: VexLifeProdServiceContract.LocalSourcesControlOutcome,
            reasonKey: String? = null,
            message: String? = null,
            sourceRef: String? = request.sourceRef,
        ) {
            respondJson(
                exchange = exchange,
                status = 200,
                value =
                    VexLifeProdServiceContract.LocalSourcesControlResponse(
                        schemaVersion =
                            VexLifeProdServiceContract.LocalSourcesControlResponseSchema,
                        controlRef = request.controlRef,
                        sourceRef = sourceRef,
                        outcome = outcome,
                        reasonKey = reasonKey,
                        message = message,
                        sources = localSourcesProjection(),
                        grantMutationPerformed =
                            outcome ==
                                VexLifeProdServiceContract.LocalSourcesControlOutcome.REALIZED,
                    ),
            )
        }

        when (
            control
        ) {
            VexLifeLocalSourcesFamily.AddFolder -> {
                val addLawful =
                    synchronized(
                        localSourcesMutationLock,
                    ) {
                        VexLifeLocalSourcesFamily.resolve(
                            currentState = localSources.state(),
                            control = control,
                        ) !=
                            null
                    }
                if (
                    !addLawful
                ) {
                    response(
                        outcome =
                            VexLifeProdServiceContract.LocalSourcesControlOutcome.NOT_LAWFUL_NOW,
                        reasonKey =
                            VexLifeLocalSourceStore.RefusedLimitReached,
                    )
                    return
                }

                when (
                    val picked =
                        localFolderPicker.chooseFolder()
                ) {
                    VexLifeLocalFolderPicker.Result.Cancelled ->
                        response(
                            outcome =
                                VexLifeProdServiceContract.LocalSourcesControlOutcome.CANCELLED,
                            reasonKey =
                                "workspace.status.local-sources-cancelled",
                        )

                    is VexLifeLocalFolderPicker.Result.Unavailable ->
                        response(
                            outcome =
                                VexLifeProdServiceContract.LocalSourcesControlOutcome.UNAVAILABLE,
                            reasonKey =
                                "workspace.status.local-sources-picker-unavailable",
                            message = picked.reason,
                        )

                    is VexLifeLocalFolderPicker.Result.Selected -> {
                        val outcome =
                            synchronized(
                                localSourcesMutationLock,
                            ) {
                                val current =
                                    localSources.state()
                                if (
                                    VexLifeLocalSourcesFamily.resolve(
                                        currentState = current,
                                        control = control,
                                    ) ==
                                        null
                                ) {
                                    null
                                } else {
                                    localSources.add(
                                        picked.path,
                                    )
                                }
                            }

                        when (
                            outcome
                        ) {
                            null ->
                                response(
                                    outcome =
                                        VexLifeProdServiceContract.LocalSourcesControlOutcome.NOT_LAWFUL_NOW,
                                    reasonKey =
                                        VexLifeLocalSourceStore.RefusedLimitReached,
                                )

                            is VexLifeLocalSourceStore.Outcome.Realized -> {
                                val added =
                                    outcome.grants.lastOrNull()
                                response(
                                    outcome =
                                        VexLifeProdServiceContract.LocalSourcesControlOutcome.REALIZED,
                                    reasonKey =
                                        "workspace.status.local-sources-added",
                                    sourceRef =
                                        added
                                            ?.sourceRef,
                                )
                            }

                            is VexLifeLocalSourceStore.Outcome.Refused ->
                                response(
                                    outcome =
                                        VexLifeProdServiceContract.LocalSourcesControlOutcome.REFUSED,
                                    reasonKey =
                                        outcome.reasonKey,
                                )
                        }
                    }
                }
            }

            is VexLifeLocalSourcesFamily.Remove -> {
                val effect =
                    synchronized(
                        localSourcesMutationLock,
                    ) {
                        VexLifeLocalSourcesFamily.resolve(
                            currentState = localSources.state(),
                            control = control,
                        )
                            ?.effect
                    }
                if (
                    effect !is
                    VexLifeLocalSourcesFamily.Effect.RemoveGrant
                ) {
                    response(
                        outcome =
                            VexLifeProdServiceContract.LocalSourcesControlOutcome.NOT_LAWFUL_NOW,
                        reasonKey =
                            VexLifeLocalSourceStore.RefusedNotGranted,
                    )
                    return
                }

                val outcome =
                    synchronized(
                        localSourcesMutationLock,
                    ) {
                        localSources.remove(
                            effect.sourceRef,
                        )
                    }
                when (
                    outcome
                ) {
                    is VexLifeLocalSourceStore.Outcome.Realized ->
                        response(
                            outcome =
                                VexLifeProdServiceContract.LocalSourcesControlOutcome.REALIZED,
                            reasonKey =
                                "workspace.status.local-sources-removed",
                        )

                    is VexLifeLocalSourceStore.Outcome.Refused ->
                        response(
                            outcome =
                                VexLifeProdServiceContract.LocalSourcesControlOutcome.REFUSED,
                            reasonKey =
                                outcome.reasonKey,
                        )
                }
            }
        }
    }

    private fun workspaceEffectsProjection(): VexLifeProdServiceContract.WorkspaceEffectsProjection {
        val state =
            workspaceEffects.state()
        return VexLifeProdServiceContract.WorkspaceEffectsProjection(
            schemaVersion =
                VexLifeProdServiceContract.WorkspaceEffectsProjectionSchema,
            state = state.stateRef,
            grants = state.grants,
            effectsPerformed = false,
        )
    }

    private fun handleWorkspaceEffects(
        exchange: HttpExchange,
    ) {
        requireMethod(
            exchange,
            "GET",
        )
        respondJson(
            exchange = exchange,
            status = 200,
            value = workspaceEffectsProjection(),
        )
    }

    private fun handleWorkspaceEffectsControl(
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
            decodeRequest<VexLifeProdServiceContract.WorkspaceEffectsControlRequest>(
                exchange,
            )
        val control =
            when (
                request.controlRef
            ) {
                VexLifeWorkspaceEffectsFamily.ControlAllow ->
                    VexLifeWorkspaceEffectsFamily.Allow(
                        request.sourceRef,
                    )

                VexLifeWorkspaceEffectsFamily.ControlRevoke ->
                    VexLifeWorkspaceEffectsFamily.Revoke(
                        request.sourceRef,
                    )

                else ->
                    error(
                        "Workspace Effects Control is unknown",
                    )
            }

        fun response(
            outcome: VexLifeProdServiceContract.WorkspaceEffectsControlOutcome,
            reasonKey: String? = null,
        ) {
            respondJson(
                exchange = exchange,
                status = 200,
                value =
                    VexLifeProdServiceContract.WorkspaceEffectsControlResponse(
                        schemaVersion =
                            VexLifeProdServiceContract.WorkspaceEffectsControlResponseSchema,
                        controlRef = request.controlRef,
                        sourceRef = request.sourceRef,
                        outcome = outcome,
                        reasonKey = reasonKey,
                        effects = workspaceEffectsProjection(),
                        grantMutationPerformed =
                            outcome ==
                                VexLifeProdServiceContract.WorkspaceEffectsControlOutcome.REALIZED,
                    ),
            )
        }

        val semanticEffect =
            synchronized(
                localSourcesMutationLock,
            ) {
                VexLifeWorkspaceEffectsFamily.resolve(
                    currentState =
                        workspaceEffects.state(),
                    control =
                        control,
                )
                    ?.effect
            }
        if (
            semanticEffect ==
                null
        ) {
            response(
                outcome =
                    VexLifeProdServiceContract.WorkspaceEffectsControlOutcome.NOT_LAWFUL_NOW,
                reasonKey =
                    if (
                        control is
                            VexLifeWorkspaceEffectsFamily.Allow
                    ) {
                        "workspace.status.workspace-effects-already-granted"
                    } else {
                        "workspace.status.workspace-effects-not-granted"
                    },
            )
            return
        }

        val realized =
            synchronized(
                localSourcesMutationLock,
            ) {
                when (
                    semanticEffect
                ) {
                    is VexLifeWorkspaceEffectsFamily.Effect.GrantWorkspace ->
                        workspaceEffects.grant(
                            semanticEffect.sourceRef,
                        )

                    is VexLifeWorkspaceEffectsFamily.Effect.RevokeWorkspace ->
                        workspaceEffects.revoke(
                            semanticEffect.sourceRef,
                        )
                }
            }

        when (
            realized
        ) {
            is VexLifeWorkspaceEffectStore.Outcome.Realized ->
                response(
                    outcome =
                        VexLifeProdServiceContract.WorkspaceEffectsControlOutcome.REALIZED,
                    reasonKey =
                        if (
                            control is
                                VexLifeWorkspaceEffectsFamily.Allow
                        ) {
                            "workspace.status.workspace-effects-granted"
                        } else {
                            "workspace.status.workspace-effects-revoked"
                        },
                )

            is VexLifeWorkspaceEffectStore.Outcome.Refused ->
                response(
                    outcome =
                        VexLifeProdServiceContract.WorkspaceEffectsControlOutcome.REFUSED,
                    reasonKey =
                        when (
                            realized.reason
                        ) {
                            VexLifeWorkspaceEffectStore.RefusedSourceNotCurrent ->
                                "workspace.status.workspace-effects-source-not-current"

                            VexLifeWorkspaceEffectStore.RefusedAlreadyGranted ->
                                "workspace.status.workspace-effects-already-granted"

                            VexLifeWorkspaceEffectStore.RefusedLimitReached ->
                                "workspace.status.workspace-effects-limit-reached"

                            else ->
                                "workspace.status.workspace-effects-not-granted"
                        },
                )
        }
    }

    private fun attachmentDraftProjection(): VexLifeProdServiceContract.AttachmentDraftProjection {
        val state =
            attachmentDraft.state()
        return VexLifeProdServiceContract.AttachmentDraftProjection(
            schemaVersion =
                VexLifeProdServiceContract.AttachmentDraftProjectionSchema,
            state = state.stateRef,
            display =
                VexLifeAttachmentDraftFamily.displayStateFor(
                    state,
                ),
            items = state.items,
            effectsPerformed = false,
        )
    }

    private fun attachmentDraftMutableNow(): Boolean =
        conversationTurnState() ==
            VexLifeConversationTurnFamily.Ready &&
            !attachmentDraft.sendLeased() &&
            privateConversation.state() !is
                VexLifePrivateModeFamily.Private

    private fun handleAttachmentDraft(
        exchange: HttpExchange,
    ) {
        requireMethod(
            exchange,
            "GET",
        )
        respondJson(
            exchange = exchange,
            status = 200,
            value = attachmentDraftProjection(),
        )
    }

    private fun handleAttachmentControl(
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
            decodeRequest<VexLifeProdServiceContract.AttachmentControlRequest>(
                exchange,
            )
        val control =
            requireNotNull(
                VexLifeAttachmentDraftFamily.controlForRef(
                    controlRef = request.controlRef,
                    draftRef = request.draftRef,
                ),
            )
        val effect =
            if (
                attachmentDraftMutableNow()
            ) {
                VexLifeAttachmentDraftFamily.resolve(
                    currentState = attachmentDraft.state(),
                    control = control,
                )
                    ?.effect
            } else {
                null
            }

        val realized =
            when (
                effect
            ) {
                is VexLifeAttachmentDraftFamily.Effect.CancelStage ->
                    attachmentDraft.cancelStage(
                        effect.draftRef,
                    )

                is VexLifeAttachmentDraftFamily.Effect.RemoveFromDraft ->
                    attachmentDraft.remove(
                        effect.draftRef,
                    )

                VexLifeAttachmentDraftFamily.Effect.RequestFiles,
                null,
                ->
                    false
            }

        respondJson(
            exchange = exchange,
            status = 200,
            value =
                VexLifeProdServiceContract.AttachmentControlResponse(
                    schemaVersion =
                        VexLifeProdServiceContract.AttachmentControlResponseSchema,
                    controlRef = request.controlRef,
                    draftRef = request.draftRef,
                    outcome =
                        if (
                            realized
                        ) {
                            VexLifeProdServiceContract.AttachmentControlOutcome.REALIZED
                        } else {
                            VexLifeProdServiceContract.AttachmentControlOutcome.NOT_LAWFUL_NOW
                        },
                    draft = attachmentDraftProjection(),
                    effectsPerformed = realized,
                ),
        )
    }

    private fun handleAttachmentStage(
        exchange: HttpExchange,
    ) {
        requireMethod(
            exchange,
            "POST",
        )
        requireContentType(
            exchange = exchange,
            expected = "application/octet-stream",
        )

        if (
            !attachmentDraftMutableNow()
        ) {
            respondError(
                exchange = exchange,
                status = 409,
                code = "ATTACHMENT_DRAFT_NOT_EDITABLE_NOW",
                message = "Files can be changed only while the canonical Conversation Composer is ready.",
            )
            return
        }

        val draftRef =
            exchange.requestHeaders
                .getFirst(
                    VexLifeProdServiceContract.AttachmentDraftRefHeader,
                )
                ?.trim()
                .orEmpty()
        VexLifeProdServiceContract.requirePortableRef(
            draftRef,
            "draftRef",
        )

        val rawName =
            exchange.requestHeaders
                .getFirst(
                    VexLifeProdServiceContract.AttachmentNameHeader,
                )
                ?.let {
                    URLDecoder.decode(
                        it,
                        StandardCharsets.UTF_8,
                    )
                }
                .orEmpty()
        val contentLength =
            exchange.requestHeaders
                .getFirst(
                    "content-length",
                )
                ?.toLongOrNull()
                ?.takeIf {
                    it >
                        0
                }

        if (
            contentLength !=
                null &&
            contentLength >
                VexLifeLocalArtifact.MaximumOriginalBytes
        ) {
            respondError(
                exchange = exchange,
                status = 413,
                code = "ATTACHMENT_TOO_LARGE",
                message =
                    "This file is larger than the current " +
                        (
                            VexLifeLocalArtifact.MaximumOriginalBytes /
                                (1024 * 1024)
                        ) +
                        " MB Local Artifacts bound.",
            )
            return
        }

        val admission =
            attachmentDraft.beginStage(
                draftRef = draftRef,
                rawName = rawName,
                totalBytesOrNull = contentLength,
            )
                ?: run {
                    respondError(
                        exchange = exchange,
                        status = 409,
                        code = "ATTACHMENT_DRAFT_NOT_EDITABLE_NOW",
                        message = "Files cannot change while this draft is being sent or while its file limit is reached.",
                    )
                    return
                }
        val output =
            ByteArrayOutputStream()
        val buffer =
            ByteArray(
                64 * 1024,
            )
        var total =
            0L

        try {
            while (
                true
            ) {
                if (
                    attachmentDraft.cancellationRequested(
                        draftRef,
                    )
                ) {
                    attachmentDraft.cancelled(
                        draftRef,
                    )
                    respondJson(
                        exchange = exchange,
                        status = 200,
                        value =
                            VexLifeProdServiceContract.AttachmentStageResponse(
                                schemaVersion =
                                    VexLifeProdServiceContract.AttachmentStageResponseSchema,
                                draftRef = draftRef,
                                outcome =
                                    VexLifeProdServiceContract.AttachmentStageOutcome.CANCELLED,
                                message =
                                    VexLifeText(
                                        "workspace.status.attachment-staging-cancelled",
                                    ),
                                draft = attachmentDraftProjection(),
                            ),
                    )
                    return
                }

                val read =
                    exchange.requestBody.read(
                        buffer,
                    )
                if (
                    read <
                    0
                ) {
                    break
                }

                total +=
                    read
                if (
                    total >
                    VexLifeLocalArtifact.MaximumOriginalBytes
                ) {
                    attachmentDraft.held(
                        draftRef = draftRef,
                        artifactRefOrNull = null,
                        reasonKey = "workspace.status.attachment-refused",
                        completedBytes =
                            VexLifeLocalArtifact.MaximumOriginalBytes.toLong(),
                    )
                    respondJson(
                        exchange = exchange,
                        status = 200,
                        value =
                            VexLifeProdServiceContract.AttachmentStageResponse(
                                schemaVersion =
                                    VexLifeProdServiceContract.AttachmentStageResponseSchema,
                                draftRef = draftRef,
                                outcome =
                                    VexLifeProdServiceContract.AttachmentStageOutcome.HELD,
                                message =
                                    VexLifeText.text(
                                        "workspace.status.attachment-too-large",
                                        "name" to admission.displayName,
                                        "maximum" to
                                            (
                                                VexLifeLocalArtifact.MaximumOriginalBytes /
                                                    (1024 * 1024)
                                            ).toString(),
                                    ),
                                draft = attachmentDraftProjection(),
                            ),
                    )
                    return
                }

                output.write(
                    buffer,
                    0,
                    read,
                )
                attachmentDraft.progress(
                    draftRef = draftRef,
                    completedBytes = total,
                )
            }

            if (
                attachmentDraft.cancellationRequested(
                    draftRef,
                )
            ) {
                attachmentDraft.cancelled(
                    draftRef,
                )
                respondJson(
                    exchange = exchange,
                    status = 200,
                    value =
                        VexLifeProdServiceContract.AttachmentStageResponse(
                            schemaVersion =
                                VexLifeProdServiceContract.AttachmentStageResponseSchema,
                            draftRef = draftRef,
                            outcome =
                                VexLifeProdServiceContract.AttachmentStageOutcome.CANCELLED,
                            message =
                                    VexLifeText(
                                        "workspace.status.attachment-staging-cancelled",
                                    ),
                            draft = attachmentDraftProjection(),
                        ),
                )
                return
            }

            val bytes =
                output.toByteArray()
            when (
                val staged =
                    VexLifeLocalArtifactReader.stage(
                        store = localArtifactStore,
                        rawName = admission.displayName,
                        bytes = bytes,
                    )
            ) {
                is VexLifeLocalArtifactReader.Result.Ready -> {
                    attachmentDraft.ready(
                        draftRef = draftRef,
                        artifactRef = staged.original.artifactRef,
                        projectionRef = staged.projection.projectionRef,
                        completedBytes = bytes.size.toLong(),
                    )
                    respondJson(
                        exchange = exchange,
                        status = 200,
                        value =
                            VexLifeProdServiceContract.AttachmentStageResponse(
                                schemaVersion =
                                    VexLifeProdServiceContract.AttachmentStageResponseSchema,
                                draftRef = draftRef,
                                outcome =
                                    VexLifeProdServiceContract.AttachmentStageOutcome.READY,
                                draft = attachmentDraftProjection(),
                            ),
                    )
                }

                is VexLifeLocalArtifactReader.Result.ImageAdapted -> {
                    if (
                        currentVisualInputEvidenceOrNull() !=
                        null
                    ) {
                        attachmentDraft.ready(
                            draftRef = draftRef,
                            artifactRef = staged.original.artifactRef,
                            projectionRef = staged.projection.projectionRef,
                            completedBytes = bytes.size.toLong(),
                        )
                        respondJson(
                            exchange = exchange,
                            status = 200,
                            value =
                                VexLifeProdServiceContract.AttachmentStageResponse(
                                    schemaVersion =
                                        VexLifeProdServiceContract.AttachmentStageResponseSchema,
                                    draftRef = draftRef,
                                    outcome =
                                        VexLifeProdServiceContract.AttachmentStageOutcome.READY,
                                    draft = attachmentDraftProjection(),
                                ),
                        )
                    } else {
                        attachmentDraft.held(
                            draftRef = draftRef,
                            artifactRefOrNull = staged.original.artifactRef,
                            reasonKey = staged.reasonKey,
                            completedBytes = bytes.size.toLong(),
                        )
                        respondJson(
                            exchange = exchange,
                            status = 200,
                            value =
                                VexLifeProdServiceContract.AttachmentStageResponse(
                                    schemaVersion =
                                        VexLifeProdServiceContract.AttachmentStageResponseSchema,
                                    draftRef = draftRef,
                                    outcome =
                                        VexLifeProdServiceContract.AttachmentStageOutcome.HELD,
                                    message = staged.message,
                                    draft = attachmentDraftProjection(),
                                ),
                        )
                    }
                }

                is VexLifeLocalArtifactReader.Result.Held -> {
                    attachmentDraft.held(
                        draftRef = draftRef,
                        artifactRefOrNull = staged.artifactRefOrNull,
                        reasonKey = staged.reasonKey,
                        completedBytes = bytes.size.toLong(),
                    )
                    respondJson(
                        exchange = exchange,
                        status = 200,
                        value =
                            VexLifeProdServiceContract.AttachmentStageResponse(
                                schemaVersion =
                                    VexLifeProdServiceContract.AttachmentStageResponseSchema,
                                draftRef = draftRef,
                                outcome =
                                    VexLifeProdServiceContract.AttachmentStageOutcome.HELD,
                                message = staged.message,
                                draft = attachmentDraftProjection(),
                            ),
                    )
                }
            }
        } catch (
            error: Throwable,
        ) {
            attachmentDraft.cancelled(
                draftRef,
            )
            throw error
        }
    }

    private fun currentVisualInputEvidenceOrNull(): VexLifeVisualInputQualification.QualifiedRuntimeEvidence? =
        runCatching {
            currentQualifiedVisualInputEvidence()
                ?.let(
                    VexLifeVisualInputQualification::validate,
                )
        }
            .getOrNull()

    private fun requestUsesStaticImage(
        attachments: List<VexLifeLocalArtifact.RequestAttachment>,
    ): Boolean =
        attachments.any {
            attachment ->
            when (
                requireNotNull(
                    localArtifactStore.requestProjection(
                        artifactRef = attachment.artifactRef,
                        projectionRef = attachment.projectionRef,
                    ),
                ) {
                    "READY request resource projection is not in exact Product custody"
                }
            ) {
                is VexLifeLocalArtifact.StaticImageProjection ->
                    true

                is VexLifeLocalArtifact.DerivedProjection ->
                    false
            }
        }

    /**
     * Existing durable request resources for one idempotency identity.
     *
     * null means this idempotency key has never formed a durable REQUEST. An empty list means a
     * durable REQUEST exists and owns no Local Artifacts.
     */
    private fun durableRequestAttachments(
        idempotencyKey: String,
    ): List<VexLifeLocalArtifact.RequestAttachment>? {
        val requestMessageRef =
            DirectCompanionTurn.requestMessageRefFor(
                idempotencyKey,
            )
        val request =
            DirectConversationStore(
                Path.of(
                    startupSetup.productHome,
                ),
            )
                .current()
                .events
                .firstOrNull {
                    it.eventKind ==
                        VexLifeConversationContract.RequestKind &&
                        it.messageRef ==
                        requestMessageRef
                }
                ?: return null

        return localArtifactStore
            .bindingFor(
                request.eventHash,
            )
            ?.attachments
            .orEmpty()
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

        val partnerConnectionLease =
            if (
                models.partnerServing() !=
                null
            ) {
                requireNotNull(
                    partnerInference,
                ) {
                    "Partner serving requires one current Partner service realization"
                }
                    .currentConnectionLease()
            } else {
                null
            }

        try {
        val partnerTextTurn =
            try {
                currentPartnerTextTurnOrNull()
            } catch (
                held: PartnerServingNotCurrent,
            ) {
                respondError(
                    exchange = exchange,
                    status = 409,
                    code = "PARTNER_SERVING_NOT_CURRENT",
                    message =
                        "The selected Partner model is no longer qualified for this Send (" +
                            held.reason.name +
                            "). Select or use a current model before sending.",
                )
                return
            }

        val draftLeaseAttachments =
            attachmentDraft.beginSend()
        if (
            draftLeaseAttachments ==
                null
        ) {
            respondError(
                exchange = exchange,
                status = 409,
                code = "ATTACHMENT_DRAFT_NOT_READY",
                message = "Finish or remove each selected file before sending.",
            )
            return
        }

        val durableAttachments =
            durableRequestAttachments(
                request.idempotencyKey,
            )
        val requestAttachments =
            if (
                durableAttachments !=
                    null &&
                draftLeaseAttachments.isEmpty()
            ) {
                durableAttachments
            } else {
                draftLeaseAttachments
            }

        if (
            durableAttachments ==
                null &&
                partnerTextTurn !=
                    null &&
                requestAttachments.isNotEmpty()
        ) {
            attachmentDraft.releaseSend(
                draftLeaseAttachments,
            )
            respondError(
                exchange = exchange,
                status = 409,
                code = "PARTNER_TEXT_ATTACHMENTS_NOT_ADMITTED",
                message =
                    "The current Partner text stage does not accept selected Local Artifacts. The draft was preserved.",
            )
            return
        }

        if (
            durableAttachments ==
                null &&
                requestUsesStaticImage(
                    requestAttachments,
                ) &&
                currentVisualInputEvidenceOrNull() ==
                null
        ) {
            attachmentDraft.releaseSend(
                draftLeaseAttachments,
            )
            respondError(
                exchange = exchange,
                status = 409,
                code = "ATTACHMENT_VISUAL_INPUT_NOT_CURRENT",
                message =
                    "Image interpretation is not currently available for the selected local model/runtime. The draft was preserved.",
            )
            return
        }

        val intent =
            sendIntent(
                request = request,
                requestAttachments = requestAttachments,
            )
        val streaming =
            if (
                partnerTextTurn ==
                null
            ) {
                streamingTurnExecutor
            } else {
                val partnerExecutor =
                    requireNotNull(
                        streamingPartnerTurnExecutor,
                    ) {
                        "Partner serving requires one streaming Partner turn executor"
                    }
                StreamingTurnExecutor {
                    setup,
                    currentIntent,
                    observer,
                    cancellation,
                    ->
                    partnerExecutor.perform(
                        setup = setup,
                        intent = currentIntent,
                        partnerTextTurn = partnerTextTurn,
                        progressObserver = observer,
                        cancellation = cancellation,
                    )
                }
            }
        if (
            acceptsEventStream(
                exchange,
            ) &&
                streaming !=
                null
        ) {
            val attempt =
                VexLifeConversationTurnFamily.Attempt(
                    request.idempotencyKey,
                )
            val begin =
                VexLifeConversationTurnFamily.resolve(
                    currentState =
                        conversationTurnState(),
                    control =
                        VexLifeConversationTurnFamily.Send(
                            attempt,
                        ),
                )
                    ?.effect as?
                    VexLifeConversationTurnFamily.BeginTurn

            if (
                begin ==
                    null
            ) {
                attachmentDraft.releaseSend(
                    draftLeaseAttachments,
                )
                respondError(
                    exchange = exchange,
                    status = 409,
                    code = "CONVERSATION_SEND_NOT_LAWFUL_NOW",
                    message = "A new Conversation Send is not lawful in the current turn state.",
                )
                return
            }

            handleStreamingSend(
                exchange = exchange,
                intent = intent,
                draftLeaseAttachments = draftLeaseAttachments,
                begin = begin,
                executor = streaming,
            )
            return
        }

        var draftCompleted =
            false
        try {
            val result =
                if (
                    partnerTextTurn ==
                    null
                ) {
                    turnExecutor.perform(
                        startupSetup,
                        intent,
                    )
                } else {
                    requireNotNull(
                        partnerTurnExecutor,
                    ) {
                        "Partner serving requires one Partner turn executor"
                    }
                        .perform(
                            setup = startupSetup,
                            intent = intent,
                            partnerTextTurn = partnerTextTurn,
                        )
                }
            check(
                attachmentDraft.completeSend(
                    draftLeaseAttachments,
                ),
            ) {
                "completed canonical Send did not own the exact attachment draft lease"
            }
            draftCompleted =
                true

            respondJson(
                exchange = exchange,
                status = 200,
                value =
                    sendResponse(
                        result,
                    ),
            )
        } finally {
            if (
                !draftCompleted
            ) {
                attachmentDraft.releaseSend(
                    draftLeaseAttachments,
                )
            }
        }
        } finally {
            partnerConnectionLease
                ?.close()
        }
    }

    private fun sendIntent(
        request: VexLifeProdServiceContract.SendRequest,
        requestAttachments: List<VexLifeLocalArtifact.RequestAttachment>,
    ): DirectCompanionTurn.Intent =
        DirectCompanionTurn.Intent(
            idempotencyKey = request.idempotencyKey,
            content = request.content,
            requestAttachments = requestAttachments,
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
        draftLeaseAttachments: List<VexLifeLocalArtifact.RequestAttachment>,
        begin: VexLifeConversationTurnFamily.BeginTurn,
        executor: StreamingTurnExecutor,
    ) {
        require(
            intent.idempotencyKey ==
                begin.attempt.ref,
        ) {
            "Conversation Send realization must preserve the Family attempt identity"
        }

        val cancellation =
            DirectCompanionCancellation()
        val active =
            ActiveTurn(
                attempt = begin.attempt,
                cancellation = cancellation,
                initialPromptPreparationTurn =
                    initialPromptPreparation
                        ?.nextTurnPreparationPending() ==
                        true,
            )
        if (
            !activeTurn.compareAndSet(
                null,
                active,
            )
        ) {
            attachmentDraft.releaseSend(
                    draftLeaseAttachments,
                )
            respondError(
                exchange = exchange,
                status = 409,
                code = "CONVERSATION_SEND_NOT_LAWFUL_NOW",
                message = "Another Conversation turn is already active.",
            )
            return
        }

        check(
            conversationTurnState() ==
                VexLifeConversationTurnFamily.Active(
                    begin.attempt,
                ),
        ) {
            "active Send realization did not return through ConversationTurn Family observation"
        }

        val executionBinding =
            cancellation.bindCurrentThread()
        var draftCompleted =
            false

        fun retireActiveTurn() {
            activeTurn.compareAndSet(
                active,
                null,
            )
        }

        try {
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
                    cancellation,
                )

            check(
                attachmentDraft.completeSend(
                    draftLeaseAttachments,
                ),
            ) {
                "completed streaming Send did not own the exact attachment draft lease"
            }
            draftCompleted =
                true
            retireActiveTurn()
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
            retireActiveTurn()
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
            retireActiveTurn()
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
            // The canonical turn is terminal now. Clear only the interrupt issued by this exact
            // human cancellation so the presentation writer can still deliver the terminal event.
            if (
                cancellation.cancellationRequested
            ) {
                Thread.interrupted()
            }
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
        } finally {
            if (
                !draftCompleted
            ) {
                attachmentDraft.releaseSend(
                    draftLeaseAttachments,
                )
            }
            retireActiveTurn()
            executionBinding.close()
        }
    }

    private fun humanReasoningProjection(
        result: DirectCompanionTurn.Result,
    ): VexLifeProdServiceContract.HumanReasoningProjection {
        val localRuntimeObservation =
            result.localRuntimeObservation
        if (
            localRuntimeObservation ==
            null
        ) {
            require(
                result.providerObservation.sourceKind ==
                    vexlife.model.VexLifeModelSourceProjection.SourceKind.PARTNER &&
                    result.localInvocationEvidence ==
                    null &&
                    result.humanReasoning ==
                    null &&
                    !result.providerObservation.rawReasoningPersisted
            ) {
                "Partner Send reasoning projection requires source-neutral no-raw-reasoning evidence"
            }

            return VexLifeProdServiceContract.HumanReasoningProjection(
                state = "ABSENT",
                text = null,
                contentSha256 = null,
                characterCount = 0,
                persisted = false,
            )
        }

        val trace =
            localRuntimeObservation.reasoningTrace

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

    private fun conversationProjection(): VexLifeProdServiceContract.ConversationProjection =
        VexLifeConversationServiceProjection.observe(
            Path.of(
                startupSetup.productHome,
            ),
        )

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
     * that survived a service replacement must not observe Product state or perform an Effect
     * against the newer/different service. Presentation assets remain freely loadable so a current
     * service can supply the replacement page; non-browser local clients retain their separate
     * admission path.
     */
    private fun browserPresentationCurrentnessFailure(
        exchange: HttpExchange,
    ): String? {
        if (
            !exchange.requestURI.path.startsWith(
                "/api/v1/",
            )
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

    private fun respondBytes(
        exchange: HttpExchange,
        status: Int,
        bytes: ByteArray,
        contentType: String,
    ) {
        require(
            bytes.isNotEmpty() &&
                contentType.isNotBlank(),
        ) {
            "byte response requires content and content type"
        }
        exchange.responseHeaders.set(
            "content-type",
            contentType,
        )
        exchange.responseHeaders.set(
            "cache-control",
            "no-store",
        )
        exchange.responseHeaders.set(
            "x-content-type-options",
            "nosniff",
        )
        exchange.sendResponseHeaders(
            status,
            bytes.size.toLong(),
        )
        exchange.responseBody.write(
            bytes,
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
