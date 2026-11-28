package vexlife.companion

import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.security.MessageDigest
import java.time.Instant
import java.util.UUID
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import vexlife.artifact.VexLifeLocalArtifact
import vexlife.artifact.VexLifeLocalArtifactStore
import vexlife.artifact.VexLifeRequestArtifactManifest
import vexlife.logging.VexLogger
import vexlife.contract.VexContractJson
import vexlife.contract.VexDurableContractFile
import vexlife.conversation.DirectConversationIdentity
import vexlife.conversation.DirectConversationAttemptIndex
import vexlife.capability.VexLifeCapabilityAtlas
import vexlife.capability.VexLifeCapabilityBridge
import vexlife.capability.VexLifeCapabilityRequest
import vexlife.capability.VexLifeReachDefinitions
import vexlife.capability.reach.VexLifeSelfContext
import vexlife.conversation.DirectConversationStore
import vexlife.context.VexLifeContextProfile
import vexlife.filesystem.VexOwnedPath
import vexlife.foundational.VexLifeFoundationalField
import vexlife.local.VexLifeProdLocalSetup
import vexlife.local.VexLifeProdRuntimeSession
import vexlife.continuity.VexLifeContextRevisit
import vexlife.continuity.DirectConversationContextProjection
import vexlife.continuity.VexLifeContinuitySourceLocator
import vexlife.continuity.VexLifeWorkingContinuityOwner
import vexlife.continuity.VexLifeWorkingContinuity
import vexlife.memory.VexLifeCurrentMemoryHotPath
import vexlife.memory.VexLifeCurrentMemoryProviderSelection
import vexlife.memory.VexLifeCurrentMemoryReader
import vexlife.memory.VexLifeMemoryContract
import vexlife.model.heretic.HereticCompanionRepresentation
import vexlife.model.heretic.HereticTextControlCapabilityWire
import vexlife.model.heretic.HereticModelCatalog
import vexlife.model.heretic.HereticModelConfiguration
import vexlife.model.VexLifeInferenceSessionAdmission
import vexlife.model.VexLifeModelSourceProjection
import vexlife.model.VexLifeVisualInputQualification
import vexlife.model.heretic.HereticModelOfferings
import vexlife.partner.VexLifePartnerHostEffect
import vexlife.workspace.VexLifeWorkspaceEffectStore
import vexlife.workspace.VexLifeWorkspaceEffectsFamily
import vexlife.workspace.VexLifeWorkspaceCheckStore
import vexlife.workspace.VexLifeWorkspaceChecksFamily
import vexlife.workspace.VexLifeWorkspaceRunStore
import vexlife.workspace.VexLifeWorkspaceRunsFamily
import vexlife.capability.VexLifeWorkspaceTextCheck
import vexlife.capability.VexLifeWorkspaceJavaTest
import vexlife.capability.VexLifeWorkspaceJavaTestDefinitions

/**
 * One direct personal human -> Vex turn over the isolated VexLife-Prod local foundation.
 *
 * This owner composes existing owners; it does not re-own Home, runtime, prompt selection,
 * Conversation persistence, provider invocation or presentation.
 */
object DirectCompanionTurn {
    const val FailureCancelledByPerson =
        "TURN_CANCELLED_BY_PERSON"

    const val FailureReceiptSchema =
        "vexlife-prod.direct-companion-turn-failure/v1"

    private val BrowserCompanionInstanceRef =
        "instance.vexlife.browser-companion." +
            UUID.randomUUID()
                .toString()
                .lowercase()

    enum class ProgressPhase {
        CheckingRuntime,
        Reconnecting,
        Generating,
        Reasoning,
        Responding,
        /** Vex asked the host for something; [Progress.reach] names it. */
        Reaching,
        /** The host returned that request's result; [Progress.reach] carries its state. */
        Reached,
    }

    data class Progress(
        val phase: ProgressPhase,
        val delta: String? = null,
        val accumulatedCharacters: Int = 0,
        val reach: VexLifeCapabilityBridge.Use? = null,
    )

    fun interface ProgressObserver {
        fun observe(
            progress: Progress,
        )
    }

    data class Intent(
        val idempotencyKey: String,
        val content: String,
        /**
         * Exact READY resources snapshotted from the current Composer draft by the Product service.
         *
         * These are not part of human REQUEST content. They bind only after the canonical REQUEST is
         * durable and before any provider stage may run.
         */
        val requestAttachments: List<VexLifeLocalArtifact.RequestAttachment> =
            emptyList(),
        val browserContext: DirectPromptContext.BrowserContext? =
            null,
        /** Reply language and private gaps, composed by the service; see [DirectPromptContext.PersonContext]. */
        val personContext: DirectPromptContext.PersonContext? = null,
        /**
         * Context Profile captured by the service when this Send is accepted. It stays exact for
         * every provider stage in this turn; a later profile edit belongs to the next Send.
         */
        val contextProfile: VexLifeContextProfile.Effective? = null,
    ) {
        init {
            requireRef(
                idempotencyKey,
                "idempotencyKey",
            )
            require(
                content.isNotEmpty(),
            ) {
                "direct Companion turn content must be non-empty"
            }
            require(
                requestAttachments.size <=
                    VexLifeLocalArtifact.MaximumPerRequest,
            ) {
                "direct Companion turn request resources exceed the admitted attachment count"
            }
        }
    }

    data class VisualInterpretationRequest(
        val requestContent: String,
        val original: VexLifeLocalArtifact.OriginalArtifact,
        val projection: VexLifeLocalArtifact.StaticImageProjection,
        val bytes: ByteArray,
    )

    data class VisualInterpretationResult(
        /** Stable Product-owned interpretation policy/prompt identity. */
        val interpreterRef: String,
        val evidence: VexLifeVisualInputQualification.QualifiedRuntimeEvidence,
        val text: String,
    )

    fun interface VisualInterpreter {
        fun interpret(
            request: VisualInterpretationRequest,
        ): VisualInterpretationResult
    }

    data class ProviderRequestView(
        val stageIndex: Int,
        val requestBodySha256: String,
        val requestBody: String,
    )

    data class Result(
        val state: String,
        val recoveredCompletedTurn: Boolean,
        val modelCallPerformed: Boolean,
        val actualHttpCallPerformedNow: Boolean,
        val turnRef: String,
        val requestMessageRef: String,
        val responseMessageRef: String,
        val requestEventRef: String,
        val responseEventRef: String,
        val content: String,
        val model: String,
        val currentConversationHeadSha256: String,
        val contextPath: String,
        val promptContextReceipt: DirectPromptContext.Receipt,
        val providerObservation: CompanionInferenceObservation,
        val providerInvocationEvidence: CompanionInferenceInvocationEvidence,
        val localRuntimeObservation: DirectCompanionConnection.RuntimeObservation?,
        val localInvocationEvidence: DirectCompanionConnection.InvocationEvidence?,
        /**
         * Ephemeral response-local reasoning for the requesting human only.
         *
         * A recovered completed duplicate always returns null because raw reasoning is deliberately
         * absent from durable Conversation/Context evidence.
         */
        val humanReasoning: String? =
            null,
        /**
         * Exact provider requests for this live execution only. Recovered completed turns return
         * none because raw provider request bodies are deliberately not durable Context evidence.
         */
        val providerRequests: List<ProviderRequestView> = emptyList(),
        /** What Vex reached in this turn (handle, action, target, state) — never the content. */
        val capabilityUses: List<VexLifeCapabilityBridge.Use> = emptyList(),
    ) {
        val runtimeObservation: DirectCompanionConnection.RuntimeObservation
            get() =
                requireNotNull(
                    localRuntimeObservation,
                ) {
                    "Partner turn result has no Local runtime observation"
                }

        val invocationEvidence: DirectCompanionConnection.InvocationEvidence
            get() =
                requireNotNull(
                    localInvocationEvidence,
                ) {
                    "Partner turn result has no Local invocation evidence"
                }
    }

    @Serializable
    private data class FailureCore(
        val schemaVersion: String,
        val turnRef: String,
        val requestMessageRef: String,
        val responseMessageRef: String,
        val requestEventRefOrNull: String?,
        val requestEventSha256OrNull: String?,
        val lastValidConversationHeadSha256OrNull: String?,
        val failureCode: String,
        val requestDurablyRecorded: Boolean,
        val responseDurablyRecorded: Boolean,
        val contextDurablyRecorded: Boolean,
        val currentHeadCommitted: Boolean,
        val retrySameSendAttemptAllowed: Boolean,
        val resumePossible: Boolean,
        val exactNextSafeRoute: String,
        val formedAt: String,
    )

    @Serializable
    @ConsistentCopyVisibility
    data class FailureReceipt internal constructor(
        val schemaVersion: String,
        val failureRef: String,
        val semanticFingerprint: String,
        val turnRef: String,
        val requestMessageRef: String,
        val responseMessageRef: String,
        val requestEventRefOrNull: String?,
        val requestEventSha256OrNull: String?,
        val lastValidConversationHeadSha256OrNull: String?,
        val failureCode: String,
        val requestDurablyRecorded: Boolean,
        val responseDurablyRecorded: Boolean,
        val contextDurablyRecorded: Boolean,
        val currentHeadCommitted: Boolean,
        val retrySameSendAttemptAllowed: Boolean,
        val resumePossible: Boolean,
        val exactNextSafeRoute: String,
        val formedAt: String,
    )

    class TurnException(
        val failureCode: String,
        val failureReceipt: FailureReceipt?,
        val retrySameSendAttemptAllowed: Boolean,
        val exactNextSafeRoute: String,
        cause: Throwable,
    ) : IllegalStateException(
        "direct Companion turn failed: $failureCode",
        cause,
    )

    private class FailedSendAttemptException(
        val receipt: FailureReceipt?,
        val route: String,
    ) : IllegalStateException(
        "same failed send attempt must not replay; form a new idempotencyKey",
    )

    private class SourceReturnRequestHeldByTurnPolicyException :
        IllegalStateException(
            "Conversation source return is held by this turn's policy",
        )

    private class RepeatedSourceReturnRequestException :
        IllegalStateException(
            "final source-return inference cannot request Conversation source again in the same human turn",
        )

    private class CapabilityRequestHeldByTurnPolicyException :
        IllegalStateException(
            "capability effects are held by this turn's policy",
        )

    private class PartnerTextControlHeldException :
        IllegalStateException(
            "Partner text-turn stage does not admit source-return or capability control yet",
        )

    private class CapabilityRequestHeldByResourceHeadroomException :
        IllegalStateException(
            "capability reach is held after this turn exhausted its real exchange headroom",
        )

    private class CapabilityProviderBridgeHeldException :
        IllegalStateException(
            "the selected model's capability provider bridge is not qualified",
        )

    private enum class CapabilityRequestAdmission(
        val logValue: String,
    ) {
        ADMITTED("admitted"),
        HELD_BY_TURN_POLICY("held_by_turn_policy"),
        HELD_RESOURCE_HEADROOM("held_resource_headroom"),
        HELD_PROVIDER_BRIDGE("held_provider_bridge"),
    }

    private enum class SourceReturnRequestAdmission(
        val logValue: String,
    ) {
        ADMITTED("admitted"),
        HELD_BY_TURN_POLICY("held_by_turn_policy"),
        ALREADY_USED_THIS_TURN("already_used_this_turn"),
    }

    private enum class SourceReturnProgressMode {
        Pending,
        HumanAnswer,
        InternalControl,
    }

    /** Machine-only control headers; content that opens with one never reaches the person. */
    private val ControlHeaders =
        listOf(DirectConversationSourceReturn.RequestHeader, VexLifeCapabilityRequest.RequestHeader)

    private fun controlHeaderIndex(content: String): Int? =
        DirectConversationSourceReturn.controlHeaderIndexOrNull(content)
            ?: VexLifeCapabilityRequest.controlHeaderIndexOrNull(content)

    private class SourceReturnProgressGate(
        private val observer: ProgressObserver?,
    ) {
        // Only assistant content is gated while it may still become the machine-only source-return
        // control. Reasoning is the human's explicitly-open ephemeral thought stream and must remain
        // live as it arrives; buffering it until answer content begins recreates the exact lived
        // regression where several minutes of thinking appear as one late block.
        private val bufferedContent =
            mutableListOf<DirectCompanionConnection.Progress>()
        private val contentPrefix =
            StringBuilder()
        private var mode =
            SourceReturnProgressMode.Pending

        fun connectionObserver(): DirectCompanionConnection.ProgressObserver? =
            observer
                ?.let {
                    DirectCompanionConnection.ProgressObserver {
                        progress ->
                        observe(
                            progress,
                        )
                    }
                }

        fun finish(
            finalContent: String,
        ): Boolean {
            val controlLike =
                controlHeaderIndex(
                    finalContent,
                ) !=
                    null ||
                    ControlHeaders.any { header ->
                        finalContent.length <=
                            header.length &&
                            header.startsWith(
                                finalContent,
                            )
                    }

            if (
                mode ==
                    SourceReturnProgressMode.Pending
            ) {
                if (
                    controlLike
                ) {
                    mode =
                        SourceReturnProgressMode.InternalControl
                    bufferedContent.clear()
                } else {
                    mode =
                        SourceReturnProgressMode.HumanAnswer
                    flushContent()
                }
            }

            return controlLike
        }

        private fun observe(
            progress: DirectCompanionConnection.Progress,
        ) {
            when (
                mode
            ) {
                SourceReturnProgressMode.HumanAnswer ->
                    projectProviderProgress(
                        observer,
                        progress,
                    )

                SourceReturnProgressMode.InternalControl ->
                    Unit

                SourceReturnProgressMode.Pending ->
                    when (
                        progress.kind
                    ) {
                        DirectCompanionConnection.ProgressKind.Reasoning ->
                            projectProviderProgress(
                                observer,
                                progress,
                            )

                        DirectCompanionConnection.ProgressKind.Content -> {
                            bufferedContent +=
                                progress
                            contentPrefix.append(
                                progress.delta,
                            )
                            val observed =
                                contentPrefix.toString()
                            when {
                                controlHeaderIndex(
                                    observed,
                                ) !=
                                    null -> {
                                    mode =
                                        SourceReturnProgressMode.InternalControl
                                    bufferedContent.clear()
                                }

                                observed.length <=
                                    DirectConversationSourceReturn.MaximumControlPreambleCharacters +
                                        ControlHeaders.maxOf { it.length } ->
                                    Unit

                                else -> {
                                    mode =
                                        SourceReturnProgressMode.HumanAnswer
                                    flushContent()
                                }
                            }
                        }
                    }
            }
        }

        private fun flushContent() {
            val pending =
                bufferedContent.toList()
            bufferedContent.clear()
            pending.forEach {
                progress ->
                projectProviderProgress(
                    observer,
                    progress,
                )
            }
        }
    }

    internal data class TurnPolicy(
        val sourceReturnAllowed: Boolean = true,
        val capabilityRequestsAllowed: Boolean = true,
        val maximumMemoryTokens: Int = VexLifeMemoryContract.MaximumMemoryTokens,
    ) {
        init {
            require(
                maximumMemoryTokens in
                    0..VexLifeMemoryContract.MaximumMemoryTokens,
            ) {
                "direct Companion turn policy Memory cap is outside the Product Memory prompt range"
            }
        }
    }

    internal fun interface SemanticRelayFactory {
        fun form(
            messageRef: String,
            originatorRef: String,
            recipientRefs: List<String>,
        ): JsonObject
    }

    internal data class PartnerTextTurn(
        val qualified: VexLifeInferenceSessionAdmission.Qualified,
        val executor: VexLifePartnerHostEffect.Executor,
        val requestSemanticRelay: SemanticRelayFactory,
        val responseSemanticRelay: SemanticRelayFactory,
    ) {
        init {
            require(
                qualified.coordinate.sourceKind ==
                    VexLifeModelSourceProjection.SourceKind.PARTNER,
            ) {
                "Partner text turn requires one exact qualified Partner serving session"
            }
        }
    }

    internal data class Hooks(
        val transport: DirectCompanionConnection.Transport =
            DirectCompanionConnection.SystemTransport,
        val now: () -> Instant = {
            Instant.now()
        },
        val currentRuntimeEvidence: (
            setup: VexLifeProdRuntimeSession.Foundation,
        ) -> HereticModelConfiguration.QualifiedRuntimeEvidence = {
            setup ->
            setup.qualifiedRuntimeEvidence
        },
        val reobserveSameRuntimeEvidence: (
            expected: HereticModelConfiguration.QualifiedRuntimeEvidence,
        ) -> HereticModelConfiguration.QualifiedRuntimeEvidence = {
            expected ->
            expected
        },
        val currentMemoryContext: (
            productHome: Path,
        ) -> VexLifeMemoryContract.Context = {
            productHome ->
            VexLifeCurrentMemoryReader.readCurrent(
                productHome,
            )
        },
        val memoryContextAtCap: (
            productHome: Path,
            memoryTokenCap: Int,
        ) -> VexLifeMemoryContract.Context = {
            productHome,
            memoryTokenCap,
            ->
            VexLifeCurrentMemoryReader.readCurrent(
                productHome = productHome,
                memoryTokenCap = memoryTokenCap,
            )
        },
        val memoryProviderSelectionAtCap: ((
            productHome: Path,
            memoryTokenCap: Int,
        ) -> VexLifeCurrentMemoryProviderSelection.Selection)? = null,
        val currentWorkspaceEffectGrants: (
            productHome: Path,
        ) -> List<VexLifeWorkspaceEffectsFamily.Grant> = {
            productHome ->
            VexLifeWorkspaceEffectStore(
                productHome,
            )
                .currentGrants()
        },
        val currentWorkspaceCheckGrants: (
            productHome: Path,
        ) -> List<VexLifeWorkspaceChecksFamily.Grant> = {
            productHome ->
            VexLifeWorkspaceCheckStore(
                productHome,
            )
                .currentGrants()
        },
        val currentWorkspaceCheckRuntimeRef: () -> String? = {
            null
        },
        val currentWorkspaceRunGrants: (
            productHome: Path,
        ) -> List<VexLifeWorkspaceRunsFamily.Grant> = {
            productHome ->
            VexLifeWorkspaceRunStore(
                productHome,
            )
                .currentGrants()
        },
        val currentWorkspaceJavaTestRuntimeRef: () -> String? = {
            null
        },
        val progressObserver: ProgressObserver? =
            null,
        val turnPolicy: TurnPolicy =
            TurnPolicy(),
        val cancellation: DirectCompanionCancellation? =
            null,
        /**
         * Deterministic integration-only Partner realization.
         *
         * Production keeps this absent until live Partner support/Relay owners are connected.
         */
        val partnerTextTurn: PartnerTextTurn? =
            null,
        val promptPreparation: VexLifePromptPreparation? =
            null,
        /** How capability owners reach the web/Home/Memory; replaced in tests. */
        val capabilityOwners: VexLifeCapabilityBridge.OwnerBindings = VexLifeCapabilityBridge.OwnerBindings(),
        /** The foundational field as its owner reads it now; re-read at each provider boundary. */
        val currentFoundationalField: () -> VexLifeFoundationalField.Reading = {
            VexLifeFoundationalField.readCurrent()
        },
        /**
         * Experiment-only effective input budget for continuity selection, set only by the
         * continuity-pressure harness adapter against a scratch Home. The Product never sets it:
         * null keeps the ordinary budget. It does not change the model's context window, the output
         * reserve or the hard limit; the prompt receipt records it beside both.
         */
        val experimentEffectiveInputTokenBudget: Int? = null,
        /**
         * Request-bound EvolveContext source preparation.
         *
         * When true, one explicit backward-looking request after a Working Continuity rollover may
         * prepare exact older Conversation source before the first provider inference. This grants
         * no semantic acceptance and performs no durable source mutation.
         */
        val conversationContextProjectionEnabled: Boolean = false,
        /**
         * Derived continuity source locator shared by the caller across related turns.
         *
         * null preserves the existing canonical full-scan route. This backend is only a candidate
         * locator; Conversation remains the source owner and every hit is exact-reopened there.
         */
        val continuitySourceLocatorBackend: VexLifeContinuitySourceLocator.Backend? = null,
        /**
         * Volatile exact-coordinate index for Product service send-attempt identity.
         *
         * This is acceleration only. Conversation remains canonical and every indexed hit is
         * source-reopened before it may affect duplicate/recovery behavior.
         */
        val conversationAttemptIndex: DirectConversationAttemptIndex? = null,
        /**
         * Qualified static-image interpreter. null means this turn has no visual-input realization.
         *
         * It may derive bounded request context only; it does not write Conversation or own the
         * selected Companion runtime.
         */
        val visualInterpreter: VisualInterpreter? = null,
        val commitWorkingContinuity: (
            productHome: Path,
            turnRef: String,
            prepared: DirectWorkingContinuity.Prepared,
            formedAt: Instant,
        ) -> Unit = {
            productHome,
            turnRef,
            prepared,
            formedAt,
            ->
            DirectWorkingContinuity.commitAfterCompletedTurn(
                productHome = productHome,
                turnRef = turnRef,
                prepared = prepared,
                formedAt = formedAt,
            )
            Unit
        },
    )

    private data class CompletedTurn(
        val result: Result,
        val turnRef: String,
        val workingContinuity: DirectWorkingContinuity.Prepared,
        val workingCycleRecovery: DirectWorkingContinuity.CycleRecovery?,
    )

    private data class ContinuityPromptSelection(
        val materialization: DirectPromptContext.Materialization,
        val memoryContext: VexLifeMemoryContract.Context,
        val promptResidentMemoryStatementRefs: Set<String>,
        val workspaceEffectGrants: List<VexLifeWorkspaceEffectsFamily.Grant>,
        val workspaceCheckGrants: List<VexLifeWorkspaceChecksFamily.Grant>,
        val workspaceCheckRuntimeRef: String?,
        val workspaceRunGrants: List<VexLifeWorkspaceRunsFamily.Grant>,
        val workspaceJavaTestRuntimeRef: String?,
    )

    private data class ProviderStage(
        val inference: CompanionInferenceConnection.Result,
        val sourceReturnRequest: DirectConversationSourceReturn.Request?,
        val capabilityRequest: VexLifeCapabilityRequest.Parsed?,
    )

    private data class FinalProviderTurn(
        val prompt: ContinuityPromptSelection,
        val inference: CompanionInferenceConnection.Result,
        val workingContinuity: DirectWorkingContinuity.Prepared,
        val sourceReturnTrace: DirectConversationSourceReturn.Trace?,
        val capabilityTrace: VexLifeCapabilityBridge.Trace?,
        val capabilityUses: List<VexLifeCapabilityBridge.Use>,
        val providerRequests: List<ProviderRequestView>,
    )

    fun perform(
        setup: VexLifeProdLocalSetup.Result,
        intent: Intent,
    ): Result =
        performWithHooks(
            setup =
                VexLifeProdRuntimeSession.Foundation(
                    state = setup.state,
                    productHome = setup.productHome,
                    qualifiedRuntimeEvidence = setup.qualifiedRuntimeEvidence,
                ),
            intent = intent,
            hooks =
                Hooks(
                    memoryProviderSelectionAtCap = {
                        productHome,
                        memoryTokenCap,
                        ->
                        VexLifeCurrentMemoryProviderSelection.read(
                            productHome = productHome,
                            memoryTokenCap = memoryTokenCap,
                        )
                    },
                ),
        )

    internal fun perform(
        session: VexLifeProdRuntimeSession,
        intent: Intent,
        progressObserver: ProgressObserver? =
            null,
        cancellation: DirectCompanionCancellation? =
            null,
        visualInterpreter: VisualInterpreter? =
            null,
        partnerTextTurn: PartnerTextTurn? =
            null,
        promptPreparation: VexLifePromptPreparation? =
            null,
        workspaceCheckRuntimeSource: VexLifeWorkspaceTextCheck.RuntimeSource? =
            null,
        workspaceJavaTestRuntimeSource: VexLifeWorkspaceJavaTest.RuntimeSource? =
            null,
        capabilityOwners: VexLifeCapabilityBridge.OwnerBindings =
            VexLifeCapabilityBridge.OwnerBindings(),
        continuitySourceLocatorBackend: VexLifeContinuitySourceLocator.Backend? = null,
        conversationAttemptIndex: DirectConversationAttemptIndex? = null,
        memoryHotPath: VexLifeCurrentMemoryHotPath? = null,
    ): Result =
        performWithHooks(
            setup = session.foundation,
            intent = intent,
            hooks =
                Hooks(
                    currentRuntimeEvidence = {
                        session.currentQualifiedRuntimeEvidence(
                            onRecovery = {
                                emitProgress(
                                    progressObserver,
                                    Progress(
                                        phase = ProgressPhase.Reconnecting,
                                    ),
                                )
                            },
                        )
                    },
                    reobserveSameRuntimeEvidence = {
                        session.observeCurrentQualifiedRuntimeEvidence()
                    },
                    currentMemoryContext = {
                        productHome,
                        ->
                        memoryHotPath
                            ?.readContext()
                            ?: VexLifeCurrentMemoryReader.readCurrent(
                                productHome,
                            )
                    },
                    memoryContextAtCap = {
                        productHome,
                        memoryTokenCap,
                        ->
                        memoryHotPath
                            ?.readContext(
                                memoryTokenCap,
                            )
                            ?: VexLifeCurrentMemoryReader.readCurrent(
                                productHome = productHome,
                                memoryTokenCap = memoryTokenCap,
                            )
                    },
                    memoryProviderSelectionAtCap = {
                        productHome,
                        memoryTokenCap,
                        ->
                        memoryHotPath
                            ?.readSelection(
                                memoryTokenCap,
                            )
                            ?: VexLifeCurrentMemoryProviderSelection.read(
                                productHome = productHome,
                                memoryTokenCap = memoryTokenCap,
                            )
                    },
                    progressObserver = progressObserver,
                    cancellation = cancellation,
                    visualInterpreter = visualInterpreter,
                    partnerTextTurn = partnerTextTurn,
                    promptPreparation =
                        promptPreparation,
                    currentWorkspaceCheckRuntimeRef = {
                        runCatching {
                            workspaceCheckRuntimeSource
                                ?.current()
                                ?.runtimeRef
                        }
                            .getOrNull()
                    },
                    currentWorkspaceJavaTestRuntimeRef = {
                        runCatching {
                            workspaceJavaTestRuntimeSource
                                ?.current()
                                ?.runtimeRef
                        }
                            .getOrNull()
                    },
                    capabilityOwners =
                        capabilityOwners,
                    conversationContextProjectionEnabled =
                        continuitySourceLocatorBackend != null,
                    continuitySourceLocatorBackend =
                        continuitySourceLocatorBackend,
                    conversationAttemptIndex =
                        conversationAttemptIndex,
                ),
        )

    internal fun performWithHooks(
        setup: VexLifeProdLocalSetup.Result,
        intent: Intent,
        hooks: Hooks,
    ): Result =
        performWithHooks(
            setup =
                VexLifeProdRuntimeSession.Foundation(
                    state = setup.state,
                    productHome = setup.productHome,
                    qualifiedRuntimeEvidence = setup.qualifiedRuntimeEvidence,
                ),
            intent = intent,
            hooks = hooks,
        )

    internal fun performWithHooks(
        setup: VexLifeProdRuntimeSession.Foundation,
        intent: Intent,
        hooks: Hooks,
    ): Result {
        require(
            setup.state ==
                "LOCAL_HERETIC_FOUNDATION_READY",
        ) {
            "direct Companion turn requires one ready isolated local foundation"
        }

        val productHome =
            Path.of(
                setup.productHome,
            )
                .also {
                    require(
                        it.isAbsolute,
                    ) {
                        "local setup productHome must be absolute"
                    }
                }
                .normalize()

        val identity =
            turnIdentity(
                intent.idempotencyKey,
            )
        val conversation =
            hooks.conversationAttemptIndex
                ?.let {
                    attemptIndex ->
                    DirectConversationStore(
                        productHome = productHome,
                        attemptIndex = attemptIndex,
                    )
                }
                ?: DirectConversationStore(
                    productHome = productHome,
                )
        val contextStore =
            DirectTurnContextStore(
                productHome,
            )

        return try {
            conversation.withWriterLease(
                instanceRef = BrowserCompanionInstanceRef,
            ) {
                val before =
                    currentForRequestAdmission(
                        identity.requestMessageRef,
                    )

                val recovered =
                    completedDuplicate(
                        admission = before,
                        contextStore = contextStore,
                        identity = identity,
                        content = intent.content,
                        requestAttachments = intent.requestAttachments,
                        browserContext = intent.browserContext,
                        productHome = productHome,
                    )

                if (
                    recovered !=
                        null
                ) {
                    return@withWriterLease recovered
                }

                val prior =
                    immediatePriorCompletedTurn()
                var request: DirectConversationStore.Event? =
                    null
                var response: DirectConversationStore.Event? =
                    null
                var context: DirectTurnContextStore.Record? =
                    null
                val partnerTextTurn =
                    hooks.partnerTextTurn
                require(
                    partnerTextTurn ==
                        null ||
                        intent.requestAttachments.isEmpty(),
                ) {
                    "Partner text-turn stage does not admit Local Artifact request attachments"
                }

                val completed =
                    try {
                    request =
                        if (
                            partnerTextTurn ==
                                null
                        ) {
                            beginRequest(
                                turnRef = identity.turnRef,
                                requestMessageRef = identity.requestMessageRef,
                                content = intent.content,
                                formedAt = hooks.now(),
                                admissionSnapshot = before,
                            )
                        } else {
                            beginRequestV2(
                                turnRef = identity.turnRef,
                                requestMessageRef = identity.requestMessageRef,
                                content = intent.content,
                                semanticRelay =
                                    partnerTextTurn.requestSemanticRelay.form(
                                        identity.requestMessageRef,
                                        canonicalPersonRef(),
                                        listOf(
                                            DirectConversationIdentity.CompanionRoleRef,
                                        ),
                                    ),
                                formedAt = hooks.now(),
                                admissionSnapshot = before,
                            )
                        }
                    // The person's exact REQUEST words are durable before any resource relation
                    // exists. Bind only the service-snapshotted READY resources to this exact event.
                    if (
                        intent.requestAttachments.isNotEmpty()
                    ) {
                        val artifactStore =
                            VexLifeLocalArtifactStore(
                                productHome,
                            )
                        artifactStore.bindRequest(
                            request = request,
                            attachments = intent.requestAttachments,
                        )
                        materializeVisualInterpretations(
                            store = artifactStore,
                            request = request,
                            requestContent = intent.content,
                            attachments = intent.requestAttachments,
                            interpreter = hooks.visualInterpreter,
                        )
                    }

                    // The request is durable now; from here every outcome must terminate as a
                    // completed head or a failure receipt (or be terminalized after a loss).
                    VexLogger.record(
                        VexLogger.Fact.SendAccepted,
                        mapOf(
                            "turnRef" to identity.turnRef,
                            "requestMessageRef" to identity.requestMessageRef,
                            "browserContext" to if (intent.browserContext != null) "included" else "none",
                            "requestAttachments" to intent.requestAttachments.size.toString(),
                        ),
                    )
                    hooks.cancellation?.checkpoint()
                    emitProgress(
                        hooks.progressObserver,
                        Progress(
                            phase = ProgressPhase.CheckingRuntime,
                        ),
                    )
                    val workingSuccessorContext =
                        DirectWorkingContinuity.resolveSuccessorContext(
                            productHome = productHome,
                            prior = prior,
                            currentRequest = request,
                            issuedAt = hooks.now(),
                        )
                    val workingFocusObservation =
                        DirectWorkingFocus.observeCurrent(
                            productHome = productHome,
                            lineageRef = request.companionLineageRef,
                            threadRef = request.threadRef,
                        )
                    val workingCycleRecovery =
                        resolveCycleRecovery(
                            productHome = productHome,
                            conversationOwner = this,
                            prior = prior,
                            currentRequest = request,
                            workingFocusObservation =
                                workingFocusObservation,
                        )
                    emitProgress(
                        hooks.progressObserver,
                        Progress(
                            phase = ProgressPhase.Generating,
                        ),
                    )
                    val providerTurn =
                        if (
                            partnerTextTurn ==
                                null
                        ) {
                            performProviderTurn(
                                conversationOwner = this,
                                productHome = productHome,
                                setup = setup,
                                currentRequest = request,
                                workingContinuityContext =
                                    workingSuccessorContext,
                                workingCycleRecovery =
                                    workingCycleRecovery,
                                workingFocusObservation =
                                    workingFocusObservation,
                                browserContext = intent.browserContext,
                                personContext = intent.personContext,
                                contextProfile = intent.contextProfile,
                                hooks = hooks,
                            )
                        } else {
                            performPartnerTextTurn(
                                conversationOwner = this,
                                productHome = productHome,
                                currentRequest = request,
                                workingContinuityContext =
                                    workingSuccessorContext,
                                workingCycleRecovery =
                                    workingCycleRecovery,
                                workingFocusObservation =
                                    workingFocusObservation,
                                browserContext = intent.browserContext,
                                personContext = intent.personContext,
                                contextProfile = intent.contextProfile,
                                partner = partnerTextTurn,
                                hooks = hooks,
                            )
                        }
                    val prompt =
                        providerTurn.prompt.materialization
                    val inference =
                        providerTurn.inference
                    val workingContinuity =
                        providerTurn.workingContinuity
                    val sourceReturnTrace =
                        providerTurn.sourceReturnTrace
                    val capabilityTrace =
                        providerTurn.capabilityTrace

                    hooks.cancellation?.checkpoint()
                    response =
                        if (
                            partnerTextTurn ==
                                null
                        ) {
                            appendResponse(
                                request = request,
                                responseMessageRef = identity.responseMessageRef,
                                content = inference.content,
                                model = inference.model,
                                endpointOrigin = inference.invocationEvidence.endpointOrigin,
                                runtimeObservationRef = inference.observation.observationRef,
                                invocationEvidenceRef = inference.invocationEvidence.invocationRef,
                                formedAt = hooks.now(),
                            )
                        } else {
                            appendExternalResponseV3(
                                request = request,
                                responseMessageRef = identity.responseMessageRef,
                                content = inference.content,
                                model = inference.model,
                                providerRef =
                                    requireNotNull(
                                        inference.observation.providerRef,
                                    ),
                                endpointProfileRef = inference.observation.profileRef,
                                endpointOrigin = inference.observation.endpointOrigin,
                                semanticRelay =
                                    partnerTextTurn.responseSemanticRelay.form(
                                        identity.responseMessageRef,
                                        DirectConversationIdentity.CompanionRoleRef,
                                        listOf(
                                            canonicalPersonRef(),
                                        ),
                                    ),
                                observationRef = inference.observation.observationRef,
                                invocationEvidenceRef = inference.invocationEvidence.invocationRef,
                                formedAt = hooks.now(),
                            )
                        }

                    context =
                        if (
                            partnerTextTurn ==
                                null
                        ) {
                            contextStore.record(
                                request = request,
                                response = response,
                                promptContext = prompt.receipt,
                                runtimeObservation = inference.requireLocalRuntimeObservation(),
                                invocationEvidence = inference.requireLocalInvocationEvidence(),
                                providerMeasurement = inference.measurement,
                                sourceReturnTrace = sourceReturnTrace,
                                capabilityTrace = capabilityTrace,
                                formedAt = hooks.now(),
                            )
                        } else {
                            require(
                                sourceReturnTrace ==
                                    null &&
                                    capabilityTrace ==
                                    null,
                            ) {
                                "Partner text-turn v5 may not persist source-return/capability trace"
                            }
                            contextStore.recordPartnerText(
                                request = request,
                                response = response,
                                promptContext = prompt.receipt,
                                providerObservation = inference.observation,
                                providerInvocationEvidence = inference.invocationEvidence,
                                providerMeasurement = inference.measurement,
                                formedAt = hooks.now(),
                            )
                        }

                    hooks.cancellation?.checkpoint()
                    val head =
                        commitCompletedTurn(
                            request = request,
                            response = response,
                            contextSha256 = context.contextSha256,
                            formedAt = hooks.now(),
                        )

                    hooks.continuitySourceLocatorBackend
                        ?.let {
                            backend ->
                            runCatching {
                                VexLifeContinuitySourceLocator.catchUpCommittedTurn(
                                    committedHead = head,
                                    request = request,
                                    response = response,
                                    backend = backend,
                                )
                            }
                        }

                    VexLogger.record(
                        VexLogger.Fact.TurnCompleted,
                        mapOf(
                            "turnRef" to identity.turnRef,
                            "model" to (response.model ?: "unknown"),
                            "sourceReturn" to if (sourceReturnTrace != null) "used" else "none",
                            "capabilityRequests" to (capabilityTrace?.entries?.size ?: 0).toString(),
                        ),
                    )
                    CompletedTurn(
                        result =
                            Result(
                        state = "TURN_COMPLETED",
                        recoveredCompletedTurn = false,
                        modelCallPerformed = true,
                        actualHttpCallPerformedNow = true,
                        turnRef = identity.turnRef,
                        requestMessageRef = identity.requestMessageRef,
                        responseMessageRef = identity.responseMessageRef,
                        requestEventRef = request.eventRef,
                        responseEventRef = response.eventRef,
                        content = response.content,
                        model =
                            requireNotNull(
                                response.model,
                            ),
                        currentConversationHeadSha256 = head.conversationHeadSha256,
                        contextPath = context.contextPath,
                                promptContextReceipt = prompt.receipt,
                                providerObservation = inference.observation,
                                providerInvocationEvidence = inference.invocationEvidence,
                                localRuntimeObservation = inference.localRuntimeObservation,
                                localInvocationEvidence = inference.localInvocationEvidence,
                                humanReasoning = inference.sealedHumanReasoning,
                                providerRequests = providerTurn.providerRequests,
                                capabilityUses = providerTurn.capabilityUses,
                            ),
                        turnRef = request.turnRef,
                        workingContinuity = workingContinuity,
                        workingCycleRecovery =
                            workingCycleRecovery,
                    )
                } catch (
                    error: Throwable,
                ) {
                    val terminalError =
                        hooks.cancellation?.cancelledExceptionOrNull()
                            ?: error
                    val failure =
                        if (
                            request !=
                                null
                        ) {
                            writeFailureReceipt(
                                productHome = productHome,
                                identity = identity,
                                request = request,
                                lastValidHeadSha256 =
                                    before.head
                                        ?.conversationHeadSha256,
                                failureCode =
                                    failureCode(
                                        terminalError,
                                    ),
                                responseDurablyRecorded =
                                    response !=
                                        null,
                                contextDurablyRecorded =
                                    context !=
                                        null,
                                formedAt = hooks.now(),
                            )
                        } else {
                            null
                        }

                    if (
                        error is
                            DirectPromptContext.AdmissionException
                    ) {
                        VexLogger.record(
                            VexLogger.Fact.PromptAdmissionHeld,
                            mapOf(
                                "turnRef" to
                                    identity.turnRef,
                                "reason" to
                                    error.reason.name,
                                "requestedInputTokens" to
                                    error.requestedInputTokenBudget.toString(),
                                "minimumInputTokens" to
                                    error.minimumInputTokenBudget.toString(),
                                "fixedContextTokens" to
                                    error.fixedContextTokens.toString(),
                                "immediatePriorTokens" to
                                    error.immediatePriorTokens.toString(),
                                "capabilityReserveTokens" to
                                    error.capabilityExchangeAllowanceTokens.toString(),
                            ),
                        )
                    }
                    VexLogger.record(
                        VexLogger.Fact.TurnFailed,
                        listOfNotNull(
                                "turnRef" to identity.turnRef,
                                "failureCode" to failureCode(terminalError),
                                failure?.let { "failureRef" to it.failureRef },
                                "failureReceipt" to if (failure != null) "written" else "not-written",
                            )
                            .toMap(),
                        detail = terminalError.javaClass.simpleName + ": " + (terminalError.message ?: ""),
                    )
                    val route =
                        failure
                            ?.exactNextSafeRoute
                            ?: exactFailureRoute(
                                lastValidHeadSha256 =
                                    before.head
                                        ?.conversationHeadSha256,
                            )

                    throw TurnException(
                        failureCode =
                            failureCode(
                                terminalError,
                            ),
                        failureReceipt = failure,
                        retrySameSendAttemptAllowed = false,
                        exactNextSafeRoute = route,
                        cause = terminalError,
                    )
                }

                val plan = completed.workingContinuity.plan?.state?.name ?: "NONE"
                try {
                    hooks.commitWorkingContinuity(
                        productHome,
                        completed.turnRef,
                        completed.workingContinuity,
                        hooks.now(),
                    )
                    DirectWorkingContinuity.clearCurrentCycleAfterCompletedTurn(
                        productHome = productHome,
                        turnRef = completed.turnRef,
                        prepared = completed.workingContinuity,
                        consumedRecoveryOrNull =
                            completed.workingCycleRecovery,
                    )
                    VexLogger.record(
                        VexLogger.Fact.WorkingContinuityCommitted,
                        mapOf("turnRef" to completed.turnRef, "plan" to plan),
                    )
                } catch (
                    error: Throwable,
                ) {
                    // The Conversation head is already committed. Working continuity is
                    // post-completion stewardship and must never retroactively become a failed send.
                    VexLogger.record(
                        VexLogger.Fact.WorkingContinuityCommitFailed,
                        mapOf("turnRef" to completed.turnRef, "plan" to plan),
                        detail = error.javaClass.simpleName,
                    )
                }

                completed.result
            }
        } catch (
            error: FailedSendAttemptException,
        ) {
            throw TurnException(
                failureCode = "FAILED_SEND_ATTEMPT_REQUIRES_NEW_SEND_ATTEMPT",
                failureReceipt = error.receipt,
                retrySameSendAttemptAllowed = false,
                exactNextSafeRoute = error.route,
                cause = error,
            )
        }
    }

    private fun performProviderTurn(
        conversationOwner: DirectConversationStore.Locked,
        productHome: Path,
        setup: VexLifeProdRuntimeSession.Foundation,
        currentRequest: DirectConversationStore.Event,
        workingContinuityContext: DirectWorkingContinuity.CurrentSuccessor?,
        workingCycleRecovery: DirectWorkingContinuity.CycleRecovery?,
        workingFocusObservation: DirectWorkingFocus.Observation,
        browserContext: DirectPromptContext.BrowserContext?,
        personContext: DirectPromptContext.PersonContext?,
        contextProfile: VexLifeContextProfile.Effective?,
        hooks: Hooks,
    ): FinalProviderTurn {
        val initialMemoryAvailability =
            hooks.currentMemoryContext(
                productHome,
            )
        val initialWorkspaceEffectGrants =
            hooks.currentWorkspaceEffectGrants(
                productHome,
            )
        val initialWorkspaceCheckGrants =
            hooks.currentWorkspaceCheckGrants(
                productHome,
            )
        val initialWorkspaceCheckRuntimeRef =
            hooks.currentWorkspaceCheckRuntimeRef()
        val initialWorkspaceRunGrants =
            hooks.currentWorkspaceRunGrants(
                productHome,
            )
        val initialWorkspaceJavaTestRuntimeRef =
            hooks.currentWorkspaceJavaTestRuntimeRef()
        val initialRuntimeEvidence =
            hooks.currentRuntimeEvidence(
                setup,
            )
        val companionEntry =
            HereticModelOfferings.requireEntry(
                productHome,
                initialRuntimeEvidence.modelRef,
            )
        val companionProfile =
            companionEntry.companionProfile
        val capabilityProviderBridgeQualified =
            HereticCompanionRepresentation.vexReachActive(
                profile = companionProfile,
                qualification = initialRuntimeEvidence.qualification,
            )
        val providerNativeTools =
            HereticCompanionRepresentation.providerNativeToolsActive(
                profile = companionProfile,
                qualification = initialRuntimeEvidence.qualification,
            )
        val capabilityRequestsPromptAdmitted =
            hooks.turnPolicy.capabilityRequestsAllowed &&
                capabilityProviderBridgeQualified
        val initialPrior =
            conversationOwner.immediatePriorCompletedTurn()
        DirectWorkingContinuity.requireSuccessorContextCurrent(
            productHome = productHome,
            prior = initialPrior,
            currentRequest = currentRequest,
            expected = workingContinuityContext,
        )
        val currentInitialMemoryAvailability =
            hooks.currentMemoryContext(
                productHome,
            )
                .also {
                    current ->
                    require(
                        current ==
                            initialMemoryAvailability,
                    ) {
                        "VexLife Memory availability changed before initial provider invocation"
                    }
                }
        val ordinarySourceReturnProtocolEnabled =
            !providerNativeTools &&
                hooks.turnPolicy.sourceReturnAllowed

        fun materializeInitialPrompt(
            sourceReturnProtocolEnabled: Boolean,
            sourceReturnDiscovery: DirectConversationSourceReturn.Discovery? = null,
            conversationContextPreparation: DirectConversationContextProjection.Prepared? = null,
        ): ContinuityPromptSelection =
            materializeContinuityPrompt(
                conversationOwner = conversationOwner,
                productHome = productHome,
                prior = initialPrior,
                currentRequest = currentRequest,
                memoryAvailability =
                    currentInitialMemoryAvailability,
                memoryContextAtCap =
                    hooks.memoryContextAtCap,
                memoryProviderSelectionAtCap =
                    hooks.memoryProviderSelectionAtCap,
                workingContinuityContext =
                    workingContinuityContext,
                workingCycleRecovery =
                    workingCycleRecovery,
                workingFocusObservation =
                    workingFocusObservation,
                browserContext = browserContext,
                personContext = personContext,
                runtimeEvidence = initialRuntimeEvidence,
                sourceReturnProtocolEnabled =
                    sourceReturnProtocolEnabled,
                sourceReturnDiscovery =
                    sourceReturnDiscovery,
                conversationContextPreparation =
                    conversationContextPreparation,
                foundationalField = hooks.currentFoundationalField(),
                contextProfile = contextProfile,
                capabilityRequestsAdmitted = capabilityRequestsPromptAdmitted,
                providerNativeTools = providerNativeTools,
                workspaceEffectGrants = initialWorkspaceEffectGrants,
                workspaceCheckGrants = initialWorkspaceCheckGrants,
                workspaceCheckRuntimeRef = initialWorkspaceCheckRuntimeRef,
                workspaceRunGrants = initialWorkspaceRunGrants,
                workspaceJavaTestRuntimeRef = initialWorkspaceJavaTestRuntimeRef,
                experimentEffectiveInputTokenBudget = hooks.experimentEffectiveInputTokenBudget,
                maximumMemoryTokens = hooks.turnPolicy.maximumMemoryTokens,
            )

        val ordinaryInitialPrompt =
            materializeInitialPrompt(
                sourceReturnProtocolEnabled =
                    ordinarySourceReturnProtocolEnabled,
            )
        val conversationContextPreparation =
            if (
                hooks.conversationContextProjectionEnabled &&
                workingContinuityContext !=
                    null
            ) {
                val receipt =
                    ordinaryInitialPrompt.materialization.receipt
                DirectConversationContextProjection.prepare(
                    conversationOwner = conversationOwner,
                    currentRequest = currentRequest,
                    excludedEventRefs =
                        (
                            receipt.selectedConversationEventRefs +
                                listOfNotNull(
                                    receipt.successorPriorResponseEventRef,
                                ) +
                                receipt.sourceReturnSourceEventRefs
                        )
                            .toSet(),
                    locatorBackend =
                        hooks.continuitySourceLocatorBackend,
                    sourceLifecycle = {
                        conversationRecallSourceLifecycle(
                            productHome,
                        )
                    },
                )
            } else {
                null
            }
        val initialSourceReturnProtocolEnabled =
            ordinarySourceReturnProtocolEnabled
        val initialPrompt =
            if (
                conversationContextPreparation ==
                    null
            ) {
                ordinaryInitialPrompt
            } else {
                materializeInitialPrompt(
                    sourceReturnProtocolEnabled =
                        initialSourceReturnProtocolEnabled,
                    conversationContextPreparation =
                        conversationContextPreparation,
                )
            }
        val initialWorkingContinuity =
            DirectWorkingContinuity.prepare(
                productHome = productHome,
                currentRequest = currentRequest,
                promptContext =
                    initialPrompt.materialization.receipt,
                memoryContext =
                    initialPrompt.memoryContext,
                priorSuccessorContext =
                    workingContinuityContext,
            )
        reverifyContinuityPrompt(
            selection = initialPrompt,
            conversationOwner = conversationOwner,
            productHome = productHome,
            prior =
                conversationOwner.immediatePriorCompletedTurn(),
            currentRequest = currentRequest,
            memoryAvailability =
                hooks.currentMemoryContext(
                    productHome,
                )
                    .also {
                        current ->
                        require(
                            current ==
                                initialMemoryAvailability,
                        ) {
                            "VexLife Memory availability changed at the initial provider boundary"
                        }
                    },
            memoryContextAtCap =
                hooks.memoryContextAtCap,
            memoryProviderSelectionAtCap =
                hooks.memoryProviderSelectionAtCap,
            workingContinuityContext =
                workingContinuityContext,
            workingCycleRecovery =
                workingCycleRecovery,
            workingFocusObservation =
                workingFocusObservation,
            browserContext = browserContext,
            personContext = personContext,
            runtimeEvidence = initialRuntimeEvidence,
            sourceReturnProtocolEnabled =
                initialSourceReturnProtocolEnabled,
            conversationContextPreparation =
                conversationContextPreparation,
            foundationalField = hooks.currentFoundationalField(),
            contextProfile = contextProfile,
            capabilityRequestsAdmitted = capabilityRequestsPromptAdmitted,
            providerNativeTools = providerNativeTools,
            experimentEffectiveInputTokenBudget = hooks.experimentEffectiveInputTokenBudget,
            maximumMemoryTokens = hooks.turnPolicy.maximumMemoryTokens,
        )
        val initialReceipt =
            initialPrompt.materialization.receipt
        hooks.promptPreparation
            ?.awaitAndConsume(
                VexLifePromptPreparation.Validation(
                    runtime = initialRuntimeEvidence,
                    leadingMessage =
                        initialPrompt.materialization.messages
                            .firstOrNull(),
                    foundationalFieldRef =
                        initialReceipt.foundationalField
                            ?.fieldRef,
                    foundationalFieldVersion =
                        initialReceipt.foundationalField
                            ?.fieldVersion,
                    foundationalBodySha256 =
                        initialReceipt.foundationalField
                            ?.bodySha256,
                    contextProfileRef =
                        initialReceipt.contextProfileRef,
                    contextProfileFingerprint =
                        initialReceipt.contextProfileFingerprint,
                    memoryContextRef =
                        initialReceipt.memoryContextRef,
                    memoryContextFingerprint =
                        initialReceipt.memoryContextFingerprint,
                    capabilityRequestsAdmitted =
                        capabilityRequestsPromptAdmitted,
                    providerNativeTools =
                        providerNativeTools,
                ),
            )
        // One human turn may take several provider stages: capability requests (bounded by the
        // bridge) and at most one Conversation source return. All stages share one runtime binding,
        // one foundational field and one prompt base; each later stage is the base plus the
        // request/result exchanges so far, so Vex continues the same reply with what came back.
        var base = initialPrompt
        var baseWorkingContinuity = initialWorkingContinuity
        var baseMemoryAvailability = initialMemoryAvailability
        var baseSourceReturnProtocolEnabled =
            initialSourceReturnProtocolEnabled
        var baseDiscovery: DirectConversationSourceReturn.Discovery? =
            null
        val baseConversationContextPreparation =
            conversationContextPreparation
        var stageRuntimeEvidence = initialRuntimeEvidence
        var sourceReturnTrace: DirectConversationSourceReturn.Trace? = null
        val exchanges = mutableListOf<VexLifeCapabilityBridge.Exchange>()
        val providerRequests = mutableListOf<ProviderRequestView>()
        val capabilityReadSession = VexLifeCapabilityBridge.TurnReadSession()
        // Exact current-turn custody for continuity reach: this turn binds which Conversation,
        // Memory and working position belong to it; the bridge only admits the request. Full owner
        // snapshots remain cached once per turn. A supported indexed revisit may instead perform one
        // bounded exact Conversation-neighborhood read for that distinct revisit request.
        val turnSources =
            VexLifeContextRevisit.TurnSources(
                readConversation = { conversationOwner.current() },
                memory = { baseMemoryAvailability },
                readWorking = {
                    val position = baseWorkingContinuity.workingSet
                    val committed =
                        VexLifeWorkingContinuityOwner(productHome)
                            .readCurrent(currentRequest.companionLineageRef, currentRequest.threadRef)
                    VexLifeContextRevisit.WorkingDirectory(
                        checkpointRef = committed?.checkpoint?.checkpointRef,
                        disposition = committed?.commitReceipt?.disposition,
                        cursorEventRef = committed?.checkpoint?.workingSet?.cursorEventRef,
                        openLoopRefs = position.openLoopRefs,
                        currentStatementRefs = position.currentStatementRefs,
                        activeWorkNodeRefs = position.activeWorkNodeRefs,
                        currentIntentReceiptRefs = position.currentIntentReceiptRefs,
                    )
                },
                promptResidentEventRefs = {
                    val receipt = base.materialization.receipt
                    (receipt.selectedConversationEventRefs +
                            listOfNotNull(receipt.successorPriorResponseEventRef) +
                            receipt.conversationContextProjectionSourceEventRefs +
                            receipt.sourceReturnSourceEventRefs)
                        .toSet()
                },
                promptResidentStatementRefs = {
                    base.promptResidentMemoryStatementRefs
                },
                now = hooks.now,
                readConversationView =
                    hooks.continuitySourceLocatorBackend
                        ?.let {
                            locatorBackend ->
                            {
                                revisitRequest,
                                lifecycle,
                                ->
                                if (
                                    !VexLifeContextRevisit
                                        .supportsIndexedConversationView(
                                            revisitRequest,
                                        )
                                ) {
                                    null
                                } else {
                                    val neighborhoodQueries =
                                        revisitRequest.queries.map {
                                            query ->
                                            VexLifeContinuitySourceLocator.NeighborhoodQuery(
                                                queryTerms =
                                                    query.terms,
                                                speakerScope =
                                                    when (
                                                        query.speaker
                                                    ) {
                                                        VexLifeContextRevisit.Speaker.ANY ->
                                                            VexLifeContinuitySourceLocator.SpeakerScope.ANY

                                                        VexLifeContextRevisit.Speaker.HUMAN ->
                                                            VexLifeContinuitySourceLocator.SpeakerScope.HUMAN

                                                        VexLifeContextRevisit.Speaker.VEX ->
                                                            VexLifeContinuitySourceLocator.SpeakerScope.VEX
                                                    },
                                                queryRef =
                                                    query.ref,
                                                seedRefs =
                                                    query.seedRefs
                                                        .toSet(),
                                                typedEdges =
                                                    if (
                                                        query.corrections
                                                    ) {
                                                        setOf(
                                                            VexLifeContinuitySourceLocator.TypedEdgeKind.CORRECTS_PREVIOUS,
                                                        )
                                                    } else {
                                                        emptySet()
                                                    },
                                            )
                                        }
                                    val lifecycleBindingRef =
                                        lifecycle
                                            ?.bindingRef
                                            ?: VexLifeContinuitySourceLocator.UnboundSourceLifecycleBindingRef
                                    var outcome =
                                        runCatching {
                                            VexLifeContinuitySourceLocator.locateNeighborhood(
                                                conversationOwner =
                                                    conversationOwner,
                                                queries =
                                                    neighborhoodQueries,
                                                backend =
                                                    locatorBackend,
                                                sourceLifecycleBindingRef =
                                                    lifecycleBindingRef,
                                            )
                                        }
                                            .getOrNull()
                                    if (
                                        outcome is
                                        VexLifeContinuitySourceLocator.NeighborhoodOutcome.FallbackRequired &&
                                        outcome.reason in
                                        setOf(
                                            VexLifeContinuitySourceLocator.FallbackReason.ABSENT,
                                            VexLifeContinuitySourceLocator.FallbackReason.STALE,
                                            VexLifeContinuitySourceLocator.FallbackReason.CORRUPT,
                                            VexLifeContinuitySourceLocator.FallbackReason.FINGERPRINT_BINDING_MISMATCH,
                                            VexLifeContinuitySourceLocator.FallbackReason.SOURCE_LIFECYCLE_BINDING_MISMATCH,
                                            VexLifeContinuitySourceLocator.FallbackReason.SEMANTIC_INDEX_STALE,
                                            VexLifeContinuitySourceLocator.FallbackReason.SEMANTIC_REPRESENTATION_BINDING_MISMATCH,
                                            VexLifeContinuitySourceLocator.FallbackReason.SEMANTIC_INDEX_CORRUPT,
                                        )
                                    ) {
                                        val rebuilt =
                                            runCatching {
                                                VexLifeContinuitySourceLocator.rebuild(
                                                    conversationOwner =
                                                        conversationOwner,
                                                    backend =
                                                        locatorBackend,
                                                    sourceLifecycleBindingRef =
                                                        lifecycleBindingRef,
                                                    excludedEventRefs =
                                                        lifecycle
                                                            ?.blockedEventRefs
                                                            .orEmpty(),
                                                )
                                            }
                                                .isSuccess
                                        if (
                                            rebuilt
                                        ) {
                                            outcome =
                                                runCatching {
                                                    VexLifeContinuitySourceLocator.locateNeighborhood(
                                                        conversationOwner =
                                                            conversationOwner,
                                                        queries =
                                                            neighborhoodQueries,
                                                        backend =
                                                            locatorBackend,
                                                        sourceLifecycleBindingRef =
                                                            lifecycleBindingRef,
                                                    )
                                                }
                                                    .getOrNull()
                                        }
                                    }
                                    when (
                                        val ready =
                                            outcome
                                    ) {
                                        is VexLifeContinuitySourceLocator.NeighborhoodOutcome.Ready ->
                                            VexLifeContextRevisit.ConversationView(
                                                turns =
                                                    ready.turns,
                                                totalCompletedTurnCount =
                                                    ready.totalCompletedTurnCount,
                                                conversationEventsRead =
                                                    ready.exactEventFileReadCount,
                                                exactHeadFileReadCount =
                                                    ready.exactHeadFileReadCount,
                                                wholeHistoryHeadTraversalPerformed =
                                                    ready.wholeHistoryHeadTraversalPerformed,
                                                derivedEntryScanCount =
                                                    ready.derivedEntryScanCount,
                                                derivedPostingLookupCount =
                                                    ready.derivedPostingLookupCount,
                                                derivedRefLookupCount =
                                                    ready.derivedRefLookupCount,
                                                derivedTypedEdgeLookupCount =
                                                    ready.derivedTypedEdgeLookupCount,
                                                derivedSemanticLookupCount =
                                                    ready.derivedSemanticLookupCount,
                                                semanticCandidateMatches =
                                                    ready.semanticMatches.map {
                                                        semanticMatch ->
                                                        VexLifeContextRevisit.SemanticCandidateMatch(
                                                            queryRef =
                                                                semanticMatch.queryRef,
                                                            turnRef =
                                                                semanticMatch.turnRef,
                                                        )
                                                    },
                                            )

                                        else ->
                                            null
                                    }
                                }
                            }
                        },
                readConversationLifecycle = {
                    VexLifeCurrentMemoryReader
                        .readConversationRecallMembrane(
                            productHome,
                        )
                        .let {
                            membrane ->
                            VexLifeContextRevisit.ConversationLifecycle(
                                bindingRef =
                                    membrane.bindingRef,
                                blockedEventRefs =
                                    membrane.blockedEventRefs,
                                historicalCurrentizedEventRefs =
                                    membrane.historicalCurrentizedEventRefs,
                            )
                        }
                },
            )

        fun exchangeMessages(): List<CompanionProviderMessage> =
            exchanges.flatMap { listOf(it.assistantMessage, it.resultMessage) }

        // A later stage of the same human turn is not a new runtime lifecycle boundary: observe
        // currentness without recovery and require the exact runtime already proved for stage one.
        fun sameRuntime(): HereticModelConfiguration.QualifiedRuntimeEvidence =
            hooks.reobserveSameRuntimeEvidence(initialRuntimeEvidence).also { current ->
                require(current == initialRuntimeEvidence) {
                    "runtime evidence changed during one direct Companion turn"
                }
            }

        while (true) {
            hooks.cancellation?.checkpoint()
            val sourceReturnRequestAdmission =
                when {
                    !hooks.turnPolicy.sourceReturnAllowed ->
                        SourceReturnRequestAdmission.HELD_BY_TURN_POLICY

                    sourceReturnTrace !=
                        null ||
                        baseDiscovery !=
                            null ->
                        SourceReturnRequestAdmission.ALREADY_USED_THIS_TURN

                    else ->
                        SourceReturnRequestAdmission.ADMITTED
                }
            val capabilityRequestAdmission =
                when {
                    !hooks.turnPolicy.capabilityRequestsAllowed ->
                        CapabilityRequestAdmission.HELD_BY_TURN_POLICY

                    !capabilityProviderBridgeQualified ->
                        CapabilityRequestAdmission.HELD_PROVIDER_BRIDGE

                    exchanges
                        .lastOrNull()
                        ?.entry
                        ?.state ==
                        VexLifeCapabilityBridge.ResultState.HELD_TOKEN_BUDGET ->
                        CapabilityRequestAdmission.HELD_RESOURCE_HEADROOM

                    else ->
                        CapabilityRequestAdmission.ADMITTED
                }
            DirectWorkingFocus.requireCurrent(
                productHome = productHome,
                lineageRef = currentRequest.companionLineageRef,
                threadRef = currentRequest.threadRef,
                expected = workingFocusObservation,
            )
            val stage =
                invokeProviderStage(
                    runtimeEvidence = stageRuntimeEvidence,
                    model = companionEntry,
                    messages = base.materialization.messages + exchangeMessages(),
                    conversationCacheBoundaryMessageCount =
                        DirectPromptContext.conversationHistoryProviderBoundaryMessageCount(
                            base.materialization.receipt,
                        ),
                    hooks = hooks,
                    sourceReturnRequestAdmission =
                        sourceReturnRequestAdmission,
                    capabilityRequestAdmission =
                        capabilityRequestAdmission,
                    capabilityOffer =
                        if (
                            providerNativeTools &&
                            capabilityRequestAdmission ==
                            CapabilityRequestAdmission.ADMITTED
                        ) {
                            offeredCapabilities(
                                base.materialization.receipt.capabilityFrameSnapshot,
                            )
                        } else {
                            emptyList()
                        },
                )
            if (
                exchanges.isNotEmpty()
            ) {
                val nextMove =
                    when {
                        stage.capabilityRequest !=
                            null ->
                            VexLifeWorkingContinuity.CycleNextMove.REACH

                        stage.sourceReturnRequest !=
                            null ->
                            VexLifeWorkingContinuity.CycleNextMove.SOURCE_RETURN

                        else ->
                            VexLifeWorkingContinuity.CycleNextMove.FINAL_ANSWER
                    }
                DirectWorkingContinuity.checkpointCapabilityCycle(
                    productHome = productHome,
                    currentRequest = currentRequest,
                    workingFocusObservation =
                        workingFocusObservation,
                    capabilityEntry =
                        exchanges.last().entry,
                    phase =
                        VexLifeWorkingContinuity.CyclePhase.INTEGRATED,
                    nextMoveOrNull = nextMove,
                    nextControlSha256OrNull =
                        if (
                            nextMove ==
                                VexLifeWorkingContinuity.CycleNextMove.FINAL_ANSWER
                        ) {
                            null
                        } else {
                            sha256(
                                stage.inference.content,
                            )
                        },
                    formedAt = hooks.now(),
                )
            }
            hooks.cancellation?.checkpoint()

            providerRequests +=
                ProviderRequestView(
                    stageIndex = providerRequests.size + 1,
                    requestBodySha256 = stage.inference.invocationEvidence.requestBodySha256,
                    requestBody = stage.inference.providerRequestBody,
                )

            val capabilityRequest = stage.capabilityRequest
            if (capabilityRequest != null) {
                hooks.cancellation?.checkpoint()
                val pendingUse = VexLifeCapabilityBridge.describeUse(capabilityRequest)
                emitProgress(hooks.progressObserver, Progress(phase = ProgressPhase.Reaching, reach = pendingUse))
                val usedTokens = exchangeMessages().sumOf { DirectPromptContext.estimatedMessageTokens(it.content) }
                val exchange =
                    VexLifeCapabilityBridge.perform(
                        parsed = capabilityRequest,
                        admission =
                            VexLifeCapabilityBridge.Admission(
                                productHome = productHome,
                                requestEventHash = currentRequest.eventHash,
                                availableCapabilityRefs =
                                    base.materialization.receipt.capabilityFrameSnapshot
                                        ?.availableCapabilityRefs
                                        .orEmpty()
                                        .toSet(),
                                requestNumber = exchanges.size + 1,
                                tokensAvailable =
                                    base.materialization.receipt.capabilityExchangeAllowanceTokens - usedTokens,
                                readSession = capabilityReadSession,
                                turnSources = turnSources,
                                selfContext =
                                    VexLifeSelfContext.TurnContext(
                                        companionLineageRef =
                                            currentRequest.companionLineageRef,
                                        threadRef =
                                            currentRequest.threadRef,
                                        promptReceipt =
                                            base.materialization.receipt,
                                        workingFocusObservation =
                                            workingFocusObservation,
                                        providerMeasurement =
                                            stage.inference.measurement,
                                    ),
                            ),
                        ownerBindings = hooks.capabilityOwners,
                        estimateTokens = DirectPromptContext::estimatedMessageTokens,
                    )
                turnSources
                    .conversationLifecycleBindingRefOrNull()
                    ?.let {
                        expectedLifecycleBinding ->
                        val currentLifecycleBinding =
                            VexLifeCurrentMemoryReader
                                .readConversationRecallMembrane(
                                    productHome,
                                )
                                .bindingRef
                        require(
                            currentLifecycleBinding ==
                                expectedLifecycleBinding
                        ) {
                            "Conversation revisit lifecycle changed before capability return could re-enter provider context"
                        }
                    }
                exchanges += exchange
                DirectWorkingContinuity.checkpointCapabilityCycle(
                    productHome = productHome,
                    currentRequest = currentRequest,
                    workingFocusObservation =
                        workingFocusObservation,
                    capabilityEntry = exchange.entry,
                    phase =
                        VexLifeWorkingContinuity.CyclePhase.AWAITING_INTEGRATION,
                    recoveredPriorFingerprintOrNull =
                        if (
                            exchange.entry.requestNumber ==
                                1
                        ) {
                            workingCycleRecovery
                                ?.cycle
                                ?.semanticFingerprint
                        } else {
                            null
                        },
                    formedAt = hooks.now(),
                )
                hooks.cancellation?.checkpoint()
                emitProgress(hooks.progressObserver, Progress(phase = ProgressPhase.Reached, reach = exchange.use))

                stageRuntimeEvidence = sameRuntime()
                // The base prompt is reused, so it must still be current at this provider boundary.
                reverifyContinuityPrompt(
                    selection = base,
                    conversationOwner = conversationOwner,
                    productHome = productHome,
                    prior = conversationOwner.immediatePriorCompletedTurn(),
                    currentRequest = currentRequest,
                    memoryAvailability =
                        hooks.currentMemoryContext(productHome).also { current ->
                            require(current == baseMemoryAvailability) {
                                "VexLife Memory availability changed between provider stages of one turn"
                            }
                        },
                    memoryContextAtCap = hooks.memoryContextAtCap,
                    memoryProviderSelectionAtCap = hooks.memoryProviderSelectionAtCap,
                    workingContinuityContext = workingContinuityContext,
                    workingCycleRecovery = workingCycleRecovery,
                    workingFocusObservation = workingFocusObservation,
                    browserContext = browserContext,
                    personContext = personContext,
                    runtimeEvidence = stageRuntimeEvidence,
                    sourceReturnProtocolEnabled = baseSourceReturnProtocolEnabled,
                    sourceReturnDiscovery = baseDiscovery,
                    conversationContextPreparation =
                        baseConversationContextPreparation,
                    foundationalField = hooks.currentFoundationalField(),
            contextProfile = contextProfile,
                    capabilityRequestsAdmitted = capabilityRequestsPromptAdmitted,
                    providerNativeTools = providerNativeTools,
                    experimentEffectiveInputTokenBudget = hooks.experimentEffectiveInputTokenBudget,
                    maximumMemoryTokens = hooks.turnPolicy.maximumMemoryTokens,
                )
                continue
            }

            val sourceReturnRequest = stage.sourceReturnRequest
            if (sourceReturnRequest == null) {
                hooks.cancellation?.checkpoint()
                return FinalProviderTurn(
                    prompt = base,
                    inference = stage.inference,
                    workingContinuity = baseWorkingContinuity,
                    sourceReturnTrace = sourceReturnTrace,
                    capabilityTrace =
                        exchanges.takeIf { it.isNotEmpty() }?.let { done -> VexLifeCapabilityBridge.trace(done.map { it.entry }) },
                    capabilityUses = exchanges.map { it.use },
                    providerRequests = providerRequests.toList(),
                )
            }

            val sourceReturnLifecycle =
                VexLifeCurrentMemoryReader.readConversationRecallMembrane(
                    productHome,
                )
            val excludedSourceRefs =
                buildSet {
                    addAll(
                        initialPrompt.materialization.receipt.selectedConversationEventRefs,
                    )
                    initialPrompt.materialization.receipt.successorPriorResponseEventRef
                        ?.let(
                            ::add,
                        )
                    addAll(
                        initialPrompt.materialization.receipt
                            .conversationContextProjectionSourceEventRefs,
                    )
                    addAll(
                        sourceReturnLifecycle.blockedEventRefs,
                    )
                }
            val discovery =
                DirectConversationSourceReturn.discover(
                    conversationOwner = conversationOwner,
                    request = sourceReturnRequest,
                    excludedEventRefs = excludedSourceRefs,
                    sourceLifecycleBindingRef =
                        sourceReturnLifecycle.bindingRef,
                    historicalCurrentizedEventRefs =
                        sourceReturnLifecycle.historicalCurrentizedEventRefs,
                )

            val finalMemoryAvailability =
                hooks.currentMemoryContext(
                    productHome,
                )
            // Source return is a later provider stage of this same human turn, not a fresh
            // runtime-lifecycle boundary. A runtime that disappeared or was replaced ends this turn
            // instead of being silently substituted.
            val finalRuntimeEvidence = sameRuntime()
            val finalPrior =
                conversationOwner.immediatePriorCompletedTurn()
            DirectWorkingContinuity.requireSuccessorContextCurrent(
                productHome = productHome,
                prior = finalPrior,
                currentRequest = currentRequest,
                expected = workingContinuityContext,
            )
            val currentFinalMemoryAvailability =
                hooks.currentMemoryContext(
                    productHome,
                )
                    .also {
                        current ->
                        require(
                            current ==
                                finalMemoryAvailability,
                        ) {
                            "VexLife Memory availability changed before final source-return provider invocation"
                        }
                    }
            val finalPrompt =
                materializeContinuityPrompt(
                    conversationOwner = conversationOwner,
                    productHome = productHome,
                    prior = finalPrior,
                    currentRequest = currentRequest,
                    memoryAvailability =
                        currentFinalMemoryAvailability,
                    memoryContextAtCap =
                        hooks.memoryContextAtCap,
                    memoryProviderSelectionAtCap =
                        hooks.memoryProviderSelectionAtCap,
                    workingContinuityContext =
                        workingContinuityContext,
                    workingCycleRecovery =
                        workingCycleRecovery,
                    workingFocusObservation =
                        workingFocusObservation,
                    browserContext = browserContext,
                    personContext = personContext,
                    runtimeEvidence = finalRuntimeEvidence,
                    sourceReturnProtocolEnabled = false,
                    sourceReturnDiscovery = discovery,
                    conversationContextPreparation =
                        baseConversationContextPreparation,
                    foundationalField = hooks.currentFoundationalField(),
            contextProfile = contextProfile,
                    capabilityRequestsAdmitted = capabilityRequestsPromptAdmitted,
                    providerNativeTools = providerNativeTools,
                    workspaceEffectGrants = initialWorkspaceEffectGrants,
                    experimentEffectiveInputTokenBudget = hooks.experimentEffectiveInputTokenBudget,
                    maximumMemoryTokens = hooks.turnPolicy.maximumMemoryTokens,
                )
            // Source return adds exact older Conversation; it must not change the ground Vex was given
            // when it asked for it.
            require(
                finalPrompt.materialization.receipt.foundationalField ==
                    initialPrompt.materialization.receipt.foundationalField &&
                    finalPrompt.materialization.receipt.contextProfileRef ==
                    initialPrompt.materialization.receipt.contextProfileRef &&
                    finalPrompt.materialization.receipt.contextProfileFingerprint ==
                    initialPrompt.materialization.receipt.contextProfileFingerprint &&
                    finalPrompt.materialization.receipt.profileFoundationSystemMessageSha256 ==
                    initialPrompt.materialization.receipt.profileFoundationSystemMessageSha256,
            ) {
                "foundational field changed between the two inferences of one human turn, or Context Profile changed between those inferences"
            }
            val finalWorkingContinuity =
                DirectWorkingContinuity.prepare(
                    productHome = productHome,
                    currentRequest = currentRequest,
                    promptContext =
                        finalPrompt.materialization.receipt,
                    memoryContext =
                        finalPrompt.memoryContext,
                    priorSuccessorContext =
                        workingContinuityContext,
                )
            reverifyContinuityPrompt(
                selection = finalPrompt,
                conversationOwner = conversationOwner,
                productHome = productHome,
                prior =
                    conversationOwner.immediatePriorCompletedTurn(),
                currentRequest = currentRequest,
                memoryAvailability =
                    hooks.currentMemoryContext(
                        productHome,
                    )
                        .also {
                            current ->
                            require(
                                current ==
                                    finalMemoryAvailability,
                            ) {
                                "VexLife Memory availability changed at the final source-return provider boundary"
                            }
                        },
                memoryContextAtCap =
                    hooks.memoryContextAtCap,
                memoryProviderSelectionAtCap =
                    hooks.memoryProviderSelectionAtCap,
                workingContinuityContext =
                    workingContinuityContext,
                workingCycleRecovery =
                    workingCycleRecovery,
                workingFocusObservation =
                    workingFocusObservation,
                browserContext = browserContext,
                personContext = personContext,
                runtimeEvidence = finalRuntimeEvidence,
                sourceReturnProtocolEnabled = false,
                sourceReturnDiscovery = discovery,
                conversationContextPreparation =
                    baseConversationContextPreparation,
                foundationalField = hooks.currentFoundationalField(),
                contextProfile = contextProfile,
                capabilityRequestsAdmitted = capabilityRequestsPromptAdmitted,
                providerNativeTools = providerNativeTools,
                experimentEffectiveInputTokenBudget = hooks.experimentEffectiveInputTokenBudget,
                maximumMemoryTokens = hooks.turnPolicy.maximumMemoryTokens,
            )

            val sourceReturnTokens =
                finalPrompt.materialization.receipt.sourceReturnEstimatedSystemMessageTokens
            require(
                sourceReturnTokens >
                    0,
            ) {
                "final source-return prompt has no exact source-return projection"
            }
            val projection =
                DirectConversationSourceReturn.project(
                    discovery = discovery,
                    availableTokens = sourceReturnTokens,
                )
            require(
                projection.state ==
                    finalPrompt.materialization.receipt.sourceReturnState &&
                    projection.sourceEventRefs ==
                    finalPrompt.materialization.receipt.sourceReturnSourceEventRefs &&
                    projection.systemMessageSha256 ==
                    finalPrompt.materialization.receipt.sourceReturnSystemMessageSha256,
            ) {
                "source-return attempt projection differs from the exact final prompt projection"
            }
            val attemptTrace =
                DirectConversationSourceReturn.formAttemptTrace(
                    initialPrompt =
                        initialPrompt.materialization.receipt,
                    inference = stage.inference,
                    discovery = discovery,
                    projection = projection,
                )
            sourceReturnTrace =
                DirectConversationSourceReturn.formTrace(
                    attempt = attemptTrace,
                    finalPrompt =
                        finalPrompt.materialization.receipt,
                )
            base = finalPrompt
            baseWorkingContinuity = finalWorkingContinuity
            baseMemoryAvailability = finalMemoryAvailability
            baseSourceReturnProtocolEnabled = false
            baseDiscovery = discovery
            stageRuntimeEvidence = finalRuntimeEvidence
        }
    }

    private fun performPartnerTextTurn(
        conversationOwner: DirectConversationStore.Locked,
        productHome: Path,
        currentRequest: DirectConversationStore.Event,
        workingContinuityContext: DirectWorkingContinuity.CurrentSuccessor?,
        workingCycleRecovery: DirectWorkingContinuity.CycleRecovery?,
        workingFocusObservation: DirectWorkingFocus.Observation,
        browserContext: DirectPromptContext.BrowserContext?,
        personContext: DirectPromptContext.PersonContext?,
        contextProfile: VexLifeContextProfile.Effective?,
        partner: PartnerTextTurn,
        hooks: Hooks,
    ): FinalProviderTurn {
        val initialMemoryAvailability =
            hooks.currentMemoryContext(
                productHome,
            )
        val initialPrior =
            conversationOwner.immediatePriorCompletedTurn()
        DirectWorkingContinuity.requireSuccessorContextCurrent(
            productHome = productHome,
            prior = initialPrior,
            currentRequest = currentRequest,
            expected = workingContinuityContext,
        )
        val currentMemoryAvailability =
            hooks.currentMemoryContext(
                productHome,
            )
                .also {
                    current ->
                    require(
                        current ==
                            initialMemoryAvailability,
                    ) {
                        "VexLife Memory availability changed before Partner provider invocation"
                    }
                }

        val prompt =
            materializeContinuityPrompt(
                conversationOwner = conversationOwner,
                productHome = productHome,
                prior = initialPrior,
                currentRequest = currentRequest,
                memoryAvailability = currentMemoryAvailability,
                memoryContextAtCap = hooks.memoryContextAtCap,
                memoryProviderSelectionAtCap = hooks.memoryProviderSelectionAtCap,
                workingContinuityContext = workingContinuityContext,
                workingCycleRecovery = workingCycleRecovery,
                    workingFocusObservation = workingFocusObservation,
                browserContext = browserContext,
                personContext = personContext,
                runtimeEvidence = null,
                sourceReturnProtocolEnabled = false,
                sourceReturnDiscovery = null,
                conversationContextPreparation = null,
                foundationalField = hooks.currentFoundationalField(),
                contextProfile = contextProfile,
                capabilityRequestsAdmitted = false,
                providerNativeTools = false,
                experimentEffectiveInputTokenBudget = hooks.experimentEffectiveInputTokenBudget,
                maximumMemoryTokens = hooks.turnPolicy.maximumMemoryTokens,
            )
        val workingContinuity =
            DirectWorkingContinuity.prepare(
                productHome = productHome,
                currentRequest = currentRequest,
                promptContext = prompt.materialization.receipt,
                memoryContext = prompt.memoryContext,
                priorSuccessorContext = workingContinuityContext,
            )
        reverifyContinuityPrompt(
            selection = prompt,
            conversationOwner = conversationOwner,
            productHome = productHome,
            prior = conversationOwner.immediatePriorCompletedTurn(),
            currentRequest = currentRequest,
            memoryAvailability =
                hooks.currentMemoryContext(
                    productHome,
                )
                    .also {
                        current ->
                        require(
                            current ==
                                initialMemoryAvailability,
                        ) {
                            "VexLife Memory availability changed at the Partner provider boundary"
                        }
                    },
            memoryContextAtCap = hooks.memoryContextAtCap,
            memoryProviderSelectionAtCap = hooks.memoryProviderSelectionAtCap,
            workingContinuityContext = workingContinuityContext,
            workingCycleRecovery = workingCycleRecovery,
                    workingFocusObservation = workingFocusObservation,
            browserContext = browserContext,
            personContext = personContext,
            runtimeEvidence = null,
            sourceReturnProtocolEnabled = false,
            sourceReturnDiscovery = null,
            conversationContextPreparation = null,
            foundationalField = hooks.currentFoundationalField(),
            contextProfile = contextProfile,
            capabilityRequestsAdmitted = false,
            providerNativeTools = false,
            experimentEffectiveInputTokenBudget = hooks.experimentEffectiveInputTokenBudget,
            maximumMemoryTokens = hooks.turnPolicy.maximumMemoryTokens,
        )

        hooks.cancellation
            ?.checkpoint()
        VexLogger.record(
            VexLogger.Fact.ProviderInvocationRequested,
            mapOf(
                "qualificationRef" to partner.qualified.qualificationRef,
                "sourceKind" to "PARTNER",
                "stage" to "initial",
                "sourceReturnRequests" to "held_partner_text_stage",
                "capabilityRequests" to "held_partner_text_stage",
                "messageCount" to prompt.materialization.messages.size.toString(),
                "conversationCacheBoundaryMessages" to "NONE",
                "approxChars" to
                    prompt.materialization.messages
                        .sumOf {
                            it.content.length
                        }
                        .toString(),
            ),
        )
        val progressGate =
            SourceReturnProgressGate(
                hooks.progressObserver,
            )
        val inference =
            PartnerInference(
                qualified = partner.qualified,
                executor = partner.executor,
                now = hooks.now,
            )
                .invoke(
                    CompanionInferenceConnection.Request(
                        messages = prompt.materialization.messages,
                        capabilityOffer = emptyList(),
                        conversationCacheBoundaryMessageCount = null,
                        progressObserver = progressGate.connectionObserver(),
                        cancellation = hooks.cancellation,
                    ),
                )
        hooks.cancellation
            ?.checkpoint()
        val controlLike =
            progressGate.finish(
                inference.content,
            )
        if (
            controlLike ||
            inference.pendingPartnerCapability !=
            null
        ) {
            throw PartnerTextControlHeldException()
        }

        VexLogger.record(
            VexLogger.Fact.ProviderStageObserved,
            mapOf(
                "qualificationRef" to partner.qualified.qualificationRef,
                "sourceKind" to "PARTNER",
            ) +
                inference.measurement.refs(),
        )
        return FinalProviderTurn(
            prompt = prompt,
            inference = inference,
            workingContinuity = workingContinuity,
            sourceReturnTrace = null,
            capabilityTrace = null,
            capabilityUses = emptyList(),
            providerRequests =
                listOf(
                    ProviderRequestView(
                        stageIndex = 1,
                        requestBodySha256 = inference.invocationEvidence.requestBodySha256,
                        requestBody = inference.providerRequestBody,
                    ),
                ),
        )
    }

    private fun requestArtifactManifest(
        productHome: Path,
        currentRequest: DirectConversationStore.Event,
        runtimeContextWindowTokens: Int?,
        contextProfile: VexLifeContextProfile.Effective?,
        capabilityRequestsAdmitted: Boolean,
        experimentEffectiveInputTokenBudget: Int?,
    ): VexLifeRequestArtifactManifest.Projection? {
        val requestedGrossInputTokenBudget =
            experimentEffectiveInputTokenBudget
                ?: contextProfile
                    ?.definition
                    ?.inputTokenBudget
                ?: DirectPromptContext.InputTokenBudget
        val maximumInlineContentTokens =
            VexLifeCompanionAttentionBudget.directArtifactTokenCeiling(
                runtimeContextWindowTokens =
                    runtimeContextWindowTokens,
                requestedGrossInputTokenBudget =
                    requestedGrossInputTokenBudget,
                capabilityRequestsAdmitted =
                    capabilityRequestsAdmitted,
            )

        return VexLifeRequestArtifactManifest.project(
            store =
                VexLifeLocalArtifactStore(
                    productHome,
                ),
            request =
                currentRequest,
            maximumInlineContentTokens =
                maximumInlineContentTokens,
            estimateTokens =
                DirectPromptContext::estimatedMessageTokens,
        )
    }

    private fun materializeContinuityPrompt(
        conversationOwner: DirectConversationStore.Locked,
        productHome: Path,
        prior: DirectConversationStore.ImmediatePriorCompletedTurn?,
        currentRequest: DirectConversationStore.Event,
        memoryAvailability: VexLifeMemoryContract.Context,
        memoryContextAtCap: (
            productHome: Path,
            memoryTokenCap: Int,
        ) -> VexLifeMemoryContract.Context,
        memoryProviderSelectionAtCap: ((
            productHome: Path,
            memoryTokenCap: Int,
        ) -> VexLifeCurrentMemoryProviderSelection.Selection)?,
        workingContinuityContext: DirectWorkingContinuity.CurrentSuccessor?,
        workingCycleRecovery: DirectWorkingContinuity.CycleRecovery? =
            null,
        workingFocusObservation: DirectWorkingFocus.Observation,
        browserContext: DirectPromptContext.BrowserContext?,
        personContext: DirectPromptContext.PersonContext?,
        runtimeEvidence: HereticModelConfiguration.QualifiedRuntimeEvidence?,
        sourceReturnProtocolEnabled: Boolean,
        sourceReturnDiscovery: DirectConversationSourceReturn.Discovery? =
            null,
        conversationContextPreparation: DirectConversationContextProjection.Prepared? =
            null,
        foundationalField: VexLifeFoundationalField.Reading,
        contextProfile: VexLifeContextProfile.Effective?,
        // Ordinary direct turns admit capability requests; the prompt keeps their room free.
        capabilityRequestsAdmitted: Boolean = true,
        providerNativeTools: Boolean = true,
        workspaceEffectGrants: List<VexLifeWorkspaceEffectsFamily.Grant> =
            emptyList(),
        workspaceCheckGrants: List<VexLifeWorkspaceChecksFamily.Grant> =
            emptyList(),
        workspaceCheckRuntimeRef: String? =
            null,
        workspaceRunGrants: List<VexLifeWorkspaceRunsFamily.Grant> =
            emptyList(),
        workspaceJavaTestRuntimeRef: String? =
            null,
        experimentEffectiveInputTokenBudget: Int? = null,
        maximumMemoryTokens: Int = VexLifeMemoryContract.MaximumMemoryTokens,
    ): ContinuityPromptSelection {
        var selectedMemoryAtCap: VexLifeMemoryContract.Context? =
            null
        var selectedMemoryProviderSelection: VexLifeCurrentMemoryProviderSelection.Selection? =
            null
        val requestArtifactManifest =
            requestArtifactManifest(
                productHome =
                    productHome,
                currentRequest =
                    currentRequest,
                runtimeContextWindowTokens =
                    runtimeEvidence?.contextWindowTokens,
                contextProfile =
                    contextProfile,
                capabilityRequestsAdmitted =
                    capabilityRequestsAdmitted,
                experimentEffectiveInputTokenBudget =
                    experimentEffectiveInputTokenBudget,
            )
        val materialization =
            DirectPromptContext.materializeContinuityAt(
                conversationOwner = conversationOwner,
                prior = prior,
                currentRequest = currentRequest,
                memoryAvailabilityContext = memoryAvailability,
                memoryContextAtCap = {
                    memoryTokenCap ->
                    memoryContextAtCap(
                        productHome,
                        memoryTokenCap,
                    )
                        .also {
                            selectedMemoryAtCap =
                                it
                        }
                },
                memoryProviderSelectionAtCap =
                    memoryProviderSelectionAtCap
                        ?.let {
                            select ->
                            {
                                memoryTokenCap ->
                                select(
                                    productHome,
                                    memoryTokenCap,
                                )
                                    .also {
                                        selectedMemoryProviderSelection =
                                            it
                                    }
                            }
                        },
                workingContinuityContext = workingContinuityContext,
                workingCycleRecovery =
                    workingCycleRecovery,
                    workingFocusObservation = workingFocusObservation,
                browserContext = browserContext,
                personContext = personContext,
                runtimeEvidence = runtimeEvidence,
                requestArtifactManifest = requestArtifactManifest,
                sourceReturnProtocolEnabled =
                    sourceReturnProtocolEnabled,
                sourceReturnDiscovery = sourceReturnDiscovery,
                conversationContextPreparation =
                    conversationContextPreparation,
                foundationalField = foundationalField,
            contextProfile = contextProfile,
                conversationExtentIncluded = true,
                capabilityRequestsAdmitted = capabilityRequestsAdmitted,
                providerNativeTools = providerNativeTools,
                workspaceEffectGrants = workspaceEffectGrants,
                workspaceCheckGrants = workspaceCheckGrants,
                workspaceCheckRuntimeRef = workspaceCheckRuntimeRef,
                workspaceRunGrants = workspaceRunGrants,
                workspaceJavaTestRuntimeRef = workspaceJavaTestRuntimeRef,
                experimentEffectiveInputTokenBudget = experimentEffectiveInputTokenBudget,
                maximumMemoryTokens = maximumMemoryTokens,
            )
        val selectedMemory =
            selectedMemoryProviderSelection
                ?.dailyContext
                ?: selectedMemoryAtCap
                ?: memoryAvailability
        require(
            materialization.receipt.memoryContextRef ==
                selectedMemory.memoryContextRef &&
                materialization.receipt.memoryContextFingerprint ==
                selectedMemory.semanticFingerprint,
        ) {
            "direct Companion continuity prompt does not bind the exact selected Memory projection"
        }

        val promptResidentMemoryStatementRefs =
            buildSet {
                if (
                    selectedMemory.included
                ) {
                    selectedMemory.pages.forEach {
                        add(it.statementRef)
                    }
                }
                selectedMemoryProviderSelection
                    ?.currentAcceptedSemanticBlock
                    ?.takeIf {
                        it.included
                    }
                    ?.statementRefs
                    ?.forEach {
                        add(it)
                    }
            }

        return ContinuityPromptSelection(
            materialization = materialization,
            memoryContext = selectedMemory,
            promptResidentMemoryStatementRefs = promptResidentMemoryStatementRefs,
            workspaceEffectGrants = workspaceEffectGrants,
            workspaceCheckGrants = workspaceCheckGrants,
            workspaceCheckRuntimeRef = workspaceCheckRuntimeRef,
            workspaceRunGrants = workspaceRunGrants,
            workspaceJavaTestRuntimeRef = workspaceJavaTestRuntimeRef,
        )
    }

    private fun conversationRecallSourceLifecycle(
        productHome: Path,
    ): DirectConversationContextProjection.SourceLifecycle =
        VexLifeCurrentMemoryReader
            .readConversationRecallMembrane(
                productHome,
            )
            .let {
                membrane ->
                DirectConversationContextProjection.SourceLifecycle(
                    bindingRef =
                        membrane.bindingRef,
                    blockedEventRefs =
                        membrane.blockedEventRefs,
                    historicalCurrentizedEventRefs =
                        membrane.historicalCurrentizedEventRefs,
                )
            }

    private fun reverifyContinuityPrompt(
        selection: ContinuityPromptSelection,
        conversationOwner: DirectConversationStore.Locked,
        productHome: Path,
        prior: DirectConversationStore.ImmediatePriorCompletedTurn?,
        currentRequest: DirectConversationStore.Event,
        memoryAvailability: VexLifeMemoryContract.Context,
        memoryContextAtCap: (
            productHome: Path,
            memoryTokenCap: Int,
        ) -> VexLifeMemoryContract.Context,
        memoryProviderSelectionAtCap: ((
            productHome: Path,
            memoryTokenCap: Int,
        ) -> VexLifeCurrentMemoryProviderSelection.Selection)?,
        workingContinuityContext: DirectWorkingContinuity.CurrentSuccessor?,
        workingCycleRecovery: DirectWorkingContinuity.CycleRecovery? =
            null,
        workingFocusObservation: DirectWorkingFocus.Observation,
        browserContext: DirectPromptContext.BrowserContext?,
        personContext: DirectPromptContext.PersonContext?,
        runtimeEvidence: HereticModelConfiguration.QualifiedRuntimeEvidence?,
        sourceReturnProtocolEnabled: Boolean,
        sourceReturnDiscovery: DirectConversationSourceReturn.Discovery? =
            null,
        conversationContextPreparation: DirectConversationContextProjection.Prepared? =
            null,
        foundationalField: VexLifeFoundationalField.Reading,
        contextProfile: VexLifeContextProfile.Effective?,
        capabilityRequestsAdmitted: Boolean = true,
        providerNativeTools: Boolean = true,
        experimentEffectiveInputTokenBudget: Int? = null,
        maximumMemoryTokens: Int = VexLifeMemoryContract.MaximumMemoryTokens,
    ) {
        val expectedRecallLifecycleBindings =
            listOfNotNull(
                conversationContextPreparation
                    ?.sourceLifecycleBindingRef,
                sourceReturnDiscovery
                    ?.sourceLifecycleBindingRef,
            )
                .distinct()
        if (
            expectedRecallLifecycleBindings.isNotEmpty()
        ) {
            val currentRecallLifecycleBinding =
                VexLifeCurrentMemoryReader
                    .readConversationRecallMembrane(
                        productHome,
                    )
                    .bindingRef
            require(
                expectedRecallLifecycleBindings.all {
                    it ==
                        currentRecallLifecycleBinding
                },
            ) {
                "Conversation recall lifecycle changed before the provider boundary"
            }
        }

        DirectWorkingFocus.requireCurrent(
            productHome = productHome,
            lineageRef = currentRequest.companionLineageRef,
            threadRef = currentRequest.threadRef,
            expected = workingFocusObservation,
        )

        val requestArtifactManifest =
            requestArtifactManifest(
                productHome =
                    productHome,
                currentRequest =
                    currentRequest,
                runtimeContextWindowTokens =
                    runtimeEvidence?.contextWindowTokens,
                contextProfile =
                    contextProfile,
                capabilityRequestsAdmitted =
                    capabilityRequestsAdmitted,
                experimentEffectiveInputTokenBudget =
                    experimentEffectiveInputTokenBudget,
            )
        DirectPromptContext.reverifyContinuityAt(
            materialization =
                selection.materialization,
            conversationOwner = conversationOwner,
            prior = prior,
            currentRequest = currentRequest,
            memoryAvailabilityContext = memoryAvailability,
            memoryContextAtCap = {
                memoryTokenCap ->
                memoryContextAtCap(
                    productHome,
                    memoryTokenCap,
                )
            },
            memoryProviderSelectionAtCap =
                memoryProviderSelectionAtCap
                    ?.let {
                        select ->
                        {
                            memoryTokenCap ->
                            select(
                                productHome,
                                memoryTokenCap,
                            )
                        }
                    },
            workingContinuityContext = workingContinuityContext,
            workingCycleRecovery =
                workingCycleRecovery,
                    workingFocusObservation = workingFocusObservation,
            browserContext = browserContext,
            personContext = personContext,
            runtimeEvidence = runtimeEvidence,
            requestArtifactManifest = requestArtifactManifest,
            sourceReturnProtocolEnabled =
                sourceReturnProtocolEnabled,
            sourceReturnDiscovery = sourceReturnDiscovery,
            conversationContextPreparation =
                conversationContextPreparation,
            foundationalField = foundationalField,
            contextProfile = contextProfile,
            conversationExtentIncluded = true,
            capabilityRequestsAdmitted = capabilityRequestsAdmitted,
            providerNativeTools = providerNativeTools,
            workspaceEffectGrants = selection.workspaceEffectGrants,
            workspaceCheckGrants = selection.workspaceCheckGrants,
            workspaceCheckRuntimeRef = selection.workspaceCheckRuntimeRef,
            workspaceRunGrants = selection.workspaceRunGrants,
            workspaceJavaTestRuntimeRef = selection.workspaceJavaTestRuntimeRef,
            experimentEffectiveInputTokenBudget = experimentEffectiveInputTokenBudget,
            maximumMemoryTokens = maximumMemoryTokens,
        )
    }

    /**
     * Provider-native tools are a projection of the same current Capability Frame. This helper
     * creates no capability meaning and grants no effect authority.
     */
    private fun offeredCapabilities(
        frame: DirectPromptContext.CapabilityFrameSnapshot?,
    ): List<VexLifeCapabilityAtlas.Definition> =
        frame
            ?.availableCapabilityRefs
            .orEmpty()
            .mapNotNull {
                VexLifeCapabilityAtlas.resolve(
                    it,
                )
            }
            .filter {
                it.actions.isNotEmpty()
            }

    /** Provider-stage diagnostics only: counts/timing/enums, never provider text. */
    private fun recordProviderStage(
        runtimeEvidence: HereticModelConfiguration.QualifiedRuntimeEvidence,
        measurement: DirectCompanionConnection.StageMeasurement,
    ) {
        VexLogger.record(
            VexLogger.Fact.ProviderStageObserved,
            mapOf(
                "runtimeReceiptRef" to runtimeEvidence.runtimeReceiptRef,
            ) +
                measurement.refs(),
        )
    }

    private fun invokeProviderStage(
        runtimeEvidence: HereticModelConfiguration.QualifiedRuntimeEvidence,
        model: HereticModelCatalog.Entry,
        messages: List<CompanionProviderMessage>,
        conversationCacheBoundaryMessageCount: Int?,
        hooks: Hooks,
        sourceReturnRequestAdmission: SourceReturnRequestAdmission,
        capabilityRequestAdmission: CapabilityRequestAdmission,
        capabilityOffer: List<VexLifeCapabilityAtlas.Definition>,
    ): ProviderStage {
        val progressGate =
            SourceReturnProgressGate(
                hooks.progressObserver,
            )
        // Recorded before the call so a provider that never answers still leaves its request in
        // the chronology. The stage says whether a source-return request is still admitted.
        VexLogger.record(
            VexLogger.Fact.ProviderInvocationRequested,
            mapOf(
                "runtimeReceiptRef" to runtimeEvidence.runtimeReceiptRef,
                "stage" to
                    if (
                        sourceReturnRequestAdmission ==
                        SourceReturnRequestAdmission.ALREADY_USED_THIS_TURN
                    ) {
                        "final"
                    } else {
                        "initial"
                    },
                "sourceReturnRequests" to sourceReturnRequestAdmission.logValue,
                "capabilityRequests" to capabilityRequestAdmission.logValue,
                "messageCount" to messages.size.toString(),
                "conversationCacheBoundaryMessages" to
                    (conversationCacheBoundaryMessageCount?.toString() ?: "NONE"),
                "approxChars" to messages.sumOf { it.content.length }.toString(),
            ),
        )
        val inference =
            try {
                LocalHereticInference(
                    runtime = runtimeEvidence,
                    model = model,
                    transport = hooks.transport,
                    now = hooks.now,
                )
                    .invoke(
                        CompanionInferenceConnection.Request(
                            messages = messages,
                            capabilityOffer = capabilityOffer,
                            conversationCacheBoundaryMessageCount =
                                conversationCacheBoundaryMessageCount,
                            progressObserver =
                                progressGate.connectionObserver(),
                            cancellation = hooks.cancellation,
                        ),
                    )
            } catch (
                failure: DirectCompanionConnection.ConnectionException,
            ) {
                failure.measurement
                    ?.let {
                        recordProviderStage(
                            runtimeEvidence = runtimeEvidence,
                            measurement = it,
                        )
                    }
                throw failure
            }
        recordProviderStage(
            runtimeEvidence = runtimeEvidence,
            measurement = inference.measurement,
        )
        val controlLike =
            progressGate.finish(
                inference.content,
            )
        val normalizedCapabilityControl =
            HereticTextControlCapabilityWire.normalize(
                profile = model.companionProfile,
                content = inference.content,
            )
        val capabilityRequest =
            VexLifeCapabilityRequest.parseOrNull(
                normalizedCapabilityControl.canonicalControl,
            )
        val sourceReturnRequest =
            if (capabilityRequest != null) {
                null
            } else {
                DirectConversationSourceReturn.parseRequestOrNull(
                    inference.content,
                )
            }
        require(
            !controlLike ||
                sourceReturnRequest !=
                null ||
                capabilityRequest != null,
        ) {
            "provider output resembles an incomplete Conversation source-return control request"
        }
        if (
            capabilityRequest !=
                null
        ) {
            when (
                capabilityRequestAdmission
            ) {
                CapabilityRequestAdmission.ADMITTED ->
                    Unit

                CapabilityRequestAdmission.HELD_BY_TURN_POLICY ->
                    throw CapabilityRequestHeldByTurnPolicyException()

                CapabilityRequestAdmission.HELD_RESOURCE_HEADROOM ->
                    throw CapabilityRequestHeldByResourceHeadroomException()

                CapabilityRequestAdmission.HELD_PROVIDER_BRIDGE ->
                    throw CapabilityProviderBridgeHeldException()
            }
        }
        if (
            sourceReturnRequest !=
                null
        ) {
            when (
                sourceReturnRequestAdmission
            ) {
                SourceReturnRequestAdmission.ADMITTED ->
                    Unit

                SourceReturnRequestAdmission.HELD_BY_TURN_POLICY ->
                    throw SourceReturnRequestHeldByTurnPolicyException()

                SourceReturnRequestAdmission.ALREADY_USED_THIS_TURN ->
                    throw RepeatedSourceReturnRequestException()
            }
        }

        return ProviderStage(
            inference = inference,
            sourceReturnRequest = sourceReturnRequest,
            capabilityRequest = capabilityRequest,
        )
    }

    private fun projectProviderProgress(
        observer: ProgressObserver?,
        progress: DirectCompanionConnection.Progress,
    ) {
        emitProgress(
            observer,
            Progress(
                phase =
                    when (
                        progress.kind
                    ) {
                        DirectCompanionConnection.ProgressKind.Reasoning ->
                            ProgressPhase.Reasoning

                        DirectCompanionConnection.ProgressKind.Content ->
                            ProgressPhase.Responding
                    },
                delta = progress.delta,
                accumulatedCharacters =
                    progress.accumulatedCharacters,
            ),
        )
    }

    private fun emitProgress(
        observer: ProgressObserver?,
        progress: Progress,
    ) {
        try {
            observer
                ?.observe(
                    progress,
                )
        } catch (
            _: Throwable,
        ) {
            // Human presentation progress is intentionally non-authoritative. Failure to project a
            // transient delta must never corrupt or retroactively fail the canonical turn.
        }
    }

    @Serializable
    private data class BrowserRequestIdentityCore(
        val surface: String,
        val projectRef: String,
        val threadRef: String,
        val channelRef: String,
        val idempotencyKey: String,
    )

    @Serializable
    private data class BrowserResponseIdentityCore(
        val requestMessageRef: String,
        val speakerRef: String,
    )

    private data class Identity(
        val turnRef: String,
        val requestMessageRef: String,
        val responseMessageRef: String,
    )

    internal fun requestMessageRefFor(
        idempotencyKey: String,
    ): String {
        require(
            idempotencyKey.matches(
                Regex(
                    "[A-Za-z0-9][A-Za-z0-9._:-]{0,220}",
                ),
            ),
        ) {
            "direct Companion idempotencyKey must be one portable stable ref"
        }
        val requestFingerprint =
            semanticHash(
                BrowserRequestIdentityCore(
                    surface = "reference.browser.direct-companion",
                    projectRef = DirectConversationIdentity.ProjectRef,
                    threadRef = DirectConversationIdentity.ThreadRef,
                    channelRef = DirectConversationIdentity.ChannelRef,
                    idempotencyKey = idempotencyKey,
                ),
            )
        return "message.vexlife.conversation.human." +
            requestFingerprint.take(
                32,
            )
    }

    private fun turnIdentity(
        idempotencyKey: String,
    ): Identity {
        val requestMessageRef =
            requestMessageRefFor(
                idempotencyKey,
            )
        val responseFingerprint =
            semanticHash(
                BrowserResponseIdentityCore(
                    requestMessageRef = requestMessageRef,
                    speakerRef = DirectConversationIdentity.CompanionRoleRef,
                ),
            )

        return Identity(
            turnRef =
                "turn.vexlife.browser-companion." +
                    UUID.randomUUID()
                        .toString()
                        .lowercase(),
            requestMessageRef = requestMessageRef,
            responseMessageRef =
                "message.vexlife.conversation.companion." +
                    responseFingerprint.take(
                        32,
                    ),
        )
    }

    private fun completedDuplicate(
        admission: DirectConversationStore.RequestAdmission,
        contextStore: DirectTurnContextStore,
        identity: Identity,
        content: String,
        requestAttachments: List<VexLifeLocalArtifact.RequestAttachment>,
        browserContext: DirectPromptContext.BrowserContext?,
        productHome: Path,
    ): Result? {
        val canonicalRequest =
            admission.matchingCanonicalEvents
                .firstOrNull {
                    it.messageRef ==
                        identity.requestMessageRef
                }

        if (
            canonicalRequest !=
                null
        ) {
            require(
                canonicalRequest.eventKind ==
                    "REQUEST" &&
                    canonicalRequest.content ==
                    content,
            ) {
                "idempotencyKey already identifies a different durable human request"
            }
            requireRequestAttachmentsMatch(
                productHome = productHome,
                request = canonicalRequest,
                expected = requestAttachments,
            )

            val response =
                requireNotNull(
                    admission.matchingCanonicalEvents
                        .firstOrNull {
                            it.eventKind ==
                                "RESPONSE" &&
                                it.turnRef ==
                                canonicalRequest.turnRef &&
                                it.messageRef ==
                                identity.responseMessageRef &&
                                it.priorEventSha256 ==
                                canonicalRequest.eventSha256
                        },
                ) {
                    "canonical completed send is missing its exact response"
                }

            val context =
                requireNotNull(
                    contextStore.read(
                        canonicalRequest.turnRef,
                    ),
                ) {
                    "completed duplicate turn has no exact durable context evidence"
                }

            require(
                DirectPromptContext.browserContextMatches(
                    receipt = context.promptContext,
                    context = browserContext,
                ),
            ) {
                "idempotencyKey already identifies a completed turn with different browser context"
            }

            return Result(
                state = "TURN_COMPLETED",
                recoveredCompletedTurn = true,
                modelCallPerformed = false,
                actualHttpCallPerformedNow = false,
                turnRef = canonicalRequest.turnRef,
                requestMessageRef = canonicalRequest.messageRef,
                responseMessageRef = response.messageRef,
                requestEventRef = canonicalRequest.eventRef,
                responseEventRef = response.eventRef,
                content = response.content,
                model =
                    requireNotNull(
                        response.model,
                    ),
                currentConversationHeadSha256 =
                    requireNotNull(
                        admission.head,
                    )
                        .conversationHeadSha256,
                contextPath = context.contextPath,
                promptContextReceipt = context.promptContext,
                providerObservation = context.providerObservation,
                providerInvocationEvidence = context.providerInvocationEvidence,
                localRuntimeObservation = context.localRuntimeObservation,
                localInvocationEvidence = context.localInvocationEvidence,
                capabilityUses =
                    context.capabilityTrace
                        ?.entries
                        ?.map { VexLifeCapabilityBridge.Use(it.handle, it.action, it.target, it.state) }
                        .orEmpty(),
            )
        }

        val failed =
            admission.matchingAbandonedAttempt
                ?.takeIf {
                    it.requestEvent.messageRef ==
                        identity.requestMessageRef
                }

        if (
            failed ==
                null
        ) {
            return null
        }

        require(
            failed.requestEvent.messageRef ==
                identity.requestMessageRef &&
                failed.requestEvent.content ==
                content,
        ) {
            "idempotencyKey already identifies different failed durable evidence"
        }
        requireRequestAttachmentsMatch(
            productHome = productHome,
            request = failed.requestEvent,
            expected = requestAttachments,
        )

        val receipt =
            readFailureReceipt(
                productHome = productHome,
                turnRef = failed.requestEvent.turnRef,
            )
        val route =
            receipt
                ?.exactNextSafeRoute
                ?: if (
                    admission.head ==
                        null
                ) {
                    "FORM_NEW_SEND_ATTEMPT_AND_RETRY"
                } else {
                    "RESUME_LAST_VALID_HEAD_THEN_FORM_NEW_SEND_ATTEMPT"
                }

        throw FailedSendAttemptException(
            receipt = receipt,
            route = route,
        )
    }

    private fun requireRequestAttachmentsMatch(
        productHome: Path,
        request: DirectConversationStore.Event,
        expected: List<VexLifeLocalArtifact.RequestAttachment>,
    ) {
        val actual =
            VexLifeLocalArtifactStore(
                productHome,
            )
                .bindingFor(
                    request.eventHash,
                )
                ?.attachments
                .orEmpty()
        require(
            actual ==
                expected,
        ) {
            "idempotencyKey already identifies a durable human request with different local artifacts"
        }
    }

    private fun writeFailureReceipt(
        productHome: Path,
        identity: Identity,
        request: DirectConversationStore.Event,
        lastValidHeadSha256: String?,
        failureCode: String,
        responseDurablyRecorded: Boolean,
        contextDurablyRecorded: Boolean,
        formedAt: Instant,
    ): FailureReceipt? {
        return try {
            val directory =
                VexOwnedPath.resolveInside(
                    productHome,
                    "recovery/direct-companion",
                )
            VexOwnedPath.ensureDirectory(
                productHome,
                directory,
            )
            val core =
                FailureCore(
                    schemaVersion = FailureReceiptSchema,
                    turnRef = request.turnRef,
                    requestMessageRef = request.messageRef,
                    responseMessageRef = identity.responseMessageRef,
                    requestEventRefOrNull = request.eventRef,
                    requestEventSha256OrNull = request.eventSha256,
                    lastValidConversationHeadSha256OrNull = lastValidHeadSha256,
                    failureCode = failureCode,
                    requestDurablyRecorded = true,
                    responseDurablyRecorded = responseDurablyRecorded,
                    contextDurablyRecorded = contextDurablyRecorded,
                    currentHeadCommitted = false,
                    retrySameSendAttemptAllowed = false,
                    resumePossible =
                        lastValidHeadSha256 !=
                            null,
                    exactNextSafeRoute =
                        exactFailureRoute(
                            lastValidHeadSha256,
                        ),
                    formedAt = formedAt.toString(),
                )
            val fingerprint =
                sha256(
                    VexContractJson.encode(
                        core,
                    ),
                )
            val receipt =
                FailureReceipt(
                    schemaVersion = core.schemaVersion,
                    failureRef =
                        "failure.vexlife-prod.direct-companion." +
                            fingerprint.take(
                                32,
                            ),
                    semanticFingerprint = fingerprint,
                    turnRef = core.turnRef,
                    requestMessageRef = core.requestMessageRef,
                    responseMessageRef = core.responseMessageRef,
                    requestEventRefOrNull = core.requestEventRefOrNull,
                    requestEventSha256OrNull = core.requestEventSha256OrNull,
                    lastValidConversationHeadSha256OrNull = core.lastValidConversationHeadSha256OrNull,
                    failureCode = core.failureCode,
                    requestDurablyRecorded = core.requestDurablyRecorded,
                    responseDurablyRecorded = core.responseDurablyRecorded,
                    contextDurablyRecorded = core.contextDurablyRecorded,
                    currentHeadCommitted = core.currentHeadCommitted,
                    retrySameSendAttemptAllowed = core.retrySameSendAttemptAllowed,
                    resumePossible = core.resumePossible,
                    exactNextSafeRoute = core.exactNextSafeRoute,
                    formedAt = core.formedAt,
                )
            validateFailureReceipt(
                receipt,
            )

            val path =
                VexOwnedPath.resolveInside(
                    directory,
                    request.turnRef +
                        ".json",
                )
            val existing =
                VexDurableContractFile.read<FailureReceipt>(
                    ownedRoot = productHome,
                    path = path,
                )
            if (
                existing ==
                    null
            ) {
                VexDurableContractFile.write(
                    ownedRoot = productHome,
                    path = path,
                    value = receipt,
                )
                receipt
            } else {
                // Preserve the first substantive failure for this exact send attempt.
                validateFailureReceipt(
                    existing,
                )
                existing
            }
        } catch (
            _: Exception,
        ) {
            null
        }
    }

    private fun resolveCycleRecovery(
        productHome: Path,
        conversationOwner: DirectConversationStore.Locked,
        prior: DirectConversationStore.ImmediatePriorCompletedTurn?,
        currentRequest: DirectConversationStore.Event,
        workingFocusObservation: DirectWorkingFocus.Observation,
    ): DirectWorkingContinuity.CycleRecovery? {
        val owner =
            VexLifeWorkingContinuityOwner(
                productHome,
            )
        val cycle =
            owner.readCurrentCycle(
                lineageRef =
                    currentRequest.companionLineageRef,
                threadRef =
                    currentRequest.threadRef,
            )
                ?: return null

        // A crash may leave the current-cycle slot behind after the turn actually committed.
        // That cycle is completed stewardship residue, not an unfinished recovery candidate.
        if (
            prior
                ?.head
                ?.turnRef ==
                cycle.turnRef
        ) {
            owner.clearCurrentCycle(
                lineageRef = cycle.lineageRef,
                threadRef = cycle.threadRef,
                turnRef = cycle.turnRef,
                expectedFingerprint =
                    cycle.semanticFingerprint,
            )
            return null
        }

        val requestMessageRef =
            cycle.requestMessageRefOrNull
                ?: return null
        val failedRequest =
            requireNotNull(
                conversationOwner
                    .requestEventForMessageRef(
                        requestMessageRef,
                    )
                    .requestEvent,
            ) {
                "working continuity current cycle failed request is not readable from Conversation"
            }
        require(
            failedRequest.eventRef ==
                cycle.requestEventRef &&
                failedRequest.eventHash ==
                cycle.requestEventSha256 &&
                failedRequest.messageRef ==
                requestMessageRef &&
                failedRequest.turnRef ==
                cycle.turnRef
        ) {
            "working continuity current cycle no longer binds its failed Conversation request"
        }

        val stillBranchesFromCurrentHead =
            if (
                prior ==
                    null
            ) {
                failedRequest.sequence ==
                    0L &&
                    failedRequest.priorEventHash ==
                    null
            } else {
                failedRequest.sequence ==
                    prior.head.sequence +
                        1 &&
                    failedRequest.priorEventHash ==
                    prior.responseEvent.eventHash
            }

        if (
            !stillBranchesFromCurrentHead
        ) {
            owner.clearCurrentCycle(
                lineageRef = cycle.lineageRef,
                threadRef = cycle.threadRef,
                turnRef = cycle.turnRef,
                expectedFingerprint =
                    cycle.semanticFingerprint,
            )
            return null
        }

        val failure =
            readFailureReceipt(
                productHome = productHome,
                turnRef = cycle.turnRef,
            )
        if (
            failure !=
                null
        ) {
            require(
                failure.requestEventRefOrNull ==
                    failedRequest.eventRef &&
                    failure.requestEventSha256OrNull ==
                    failedRequest.eventHash &&
                    failure.requestMessageRef ==
                    failedRequest.messageRef
            ) {
                "working continuity recovery FailureReceipt does not bind the exact failed request"
            }

            if (
                failure.lastValidConversationHeadSha256OrNull !=
                    prior
                        ?.head
                        ?.conversationHeadSha256
            ) {
                owner.clearCurrentCycle(
                    lineageRef = cycle.lineageRef,
                    threadRef = cycle.threadRef,
                    turnRef = cycle.turnRef,
                    expectedFingerprint =
                        cycle.semanticFingerprint,
                )
                return null
            }
        }

        return DirectWorkingContinuity.formCycleRecovery(
            cycle = cycle,
            failedRequest = failedRequest,
            currentRequest = currentRequest,
            workingFocusObservation =
                workingFocusObservation,
            failureRefOrNull =
                failure?.failureRef,
            failureFingerprintOrNull =
                failure?.semanticFingerprint,
            failureCodeOrNull =
                failure?.failureCode,
            lastValidConversationHeadSha256OrNull =
                failure
                    ?.lastValidConversationHeadSha256OrNull
                    ?: prior
                        ?.head
                        ?.conversationHeadSha256,
        )
    }

    private fun readFailureReceipt(
        productHome: Path,
        turnRef: String,
    ): FailureReceipt? {
        val directory =
            VexOwnedPath.resolveInside(
                productHome,
                "recovery/direct-companion",
            )

        if (
            VexOwnedPath.observeDirectory(
                productHome,
                directory,
            ) ==
            VexOwnedPath.DirectoryState.Missing
        ) {
            return null
        }

        val receipt =
            VexDurableContractFile.read<FailureReceipt>(
                ownedRoot = productHome,
                path =
                    VexOwnedPath.resolveInside(
                        directory,
                        turnRef +
                            ".json",
                    ),
            )
                ?: return null

        validateFailureReceipt(
            receipt,
        )

        return receipt
    }

    private fun validateFailureReceipt(
        receipt: FailureReceipt,
    ) {
        require(
            receipt.schemaVersion ==
                FailureReceiptSchema &&
                !receipt.retrySameSendAttemptAllowed &&
                !receipt.currentHeadCommitted &&
                receipt.requestDurablyRecorded,
        ) {
            "direct Companion failure receipt shape/authority is invalid"
        }
        requireRef(
            receipt.turnRef,
            "failure.turnRef",
        )
        requireRef(
            receipt.requestMessageRef,
            "failure.requestMessageRef",
        )
        requireRef(
            receipt.responseMessageRef,
            "failure.responseMessageRef",
        )
        receipt.requestEventRefOrNull?.let {
            requireRef(
                it,
                "failure.requestEventRef",
            )
        }
        receipt.requestEventSha256OrNull?.let {
            require(
                it.matches(
                    Regex(
                        "[0-9a-f]{64}",
                    ),
                ),
            )
        }
        receipt.lastValidConversationHeadSha256OrNull?.let {
            require(
                it.matches(
                    Regex(
                        "[0-9a-f]{64}",
                    ),
                ),
            )
        }
        require(
            receipt.exactNextSafeRoute ==
                exactFailureRoute(
                    receipt.lastValidConversationHeadSha256OrNull,
                ) &&
                receipt.resumePossible ==
                (
                    receipt.lastValidConversationHeadSha256OrNull !=
                        null
                ),
        ) {
            "direct Companion failure receipt recovery route is not canonical"
        }

        val core =
            FailureCore(
                schemaVersion = receipt.schemaVersion,
                turnRef = receipt.turnRef,
                requestMessageRef = receipt.requestMessageRef,
                responseMessageRef = receipt.responseMessageRef,
                requestEventRefOrNull = receipt.requestEventRefOrNull,
                requestEventSha256OrNull = receipt.requestEventSha256OrNull,
                lastValidConversationHeadSha256OrNull = receipt.lastValidConversationHeadSha256OrNull,
                failureCode = receipt.failureCode,
                requestDurablyRecorded = receipt.requestDurablyRecorded,
                responseDurablyRecorded = receipt.responseDurablyRecorded,
                contextDurablyRecorded = receipt.contextDurablyRecorded,
                currentHeadCommitted = receipt.currentHeadCommitted,
                retrySameSendAttemptAllowed = receipt.retrySameSendAttemptAllowed,
                resumePossible = receipt.resumePossible,
                exactNextSafeRoute = receipt.exactNextSafeRoute,
                formedAt = receipt.formedAt,
            )
        val expected =
            sha256(
                VexContractJson.encode(
                    core,
                ),
            )

        require(
            receipt.semanticFingerprint ==
                expected &&
                receipt.failureRef ==
                "failure.vexlife-prod.direct-companion." +
                    expected.take(
                        32,
                    ),
        ) {
            "direct Companion first-failure receipt integrity is invalid"
        }
    }

    private fun exactFailureRoute(
        lastValidHeadSha256: String?,
    ): String =
        if (
            lastValidHeadSha256 ==
                null
        ) {
            "FORM_NEW_SEND_ATTEMPT_AND_RETRY"
        } else {
            "RESUME_LAST_VALID_HEAD_THEN_FORM_NEW_SEND_ATTEMPT"
        }

    private fun materializeVisualInterpretations(
        store: VexLifeLocalArtifactStore,
        request: DirectConversationStore.Event,
        requestContent: String,
        attachments: List<VexLifeLocalArtifact.RequestAttachment>,
        interpreter: VisualInterpreter?,
    ) {
        attachments.forEach {
            attachment ->
            val projection =
                requireNotNull(
                    store.requestProjection(
                        artifactRef = attachment.artifactRef,
                        projectionRef = attachment.projectionRef,
                    ),
                ) {
                    "bound request resource projection is not in exact Product custody"
                }
            if (
                projection is
                    VexLifeLocalArtifact.StaticImageProjection
            ) {
                if (
                    store.visualInterpretationFor(
                        requestEventHash = request.eventHash,
                        projectionRef = projection.projectionRef,
                    ) !=
                    null
                ) {
                    return@forEach
                }
                val currentInterpreter =
                    requireNotNull(
                        interpreter,
                    ) {
                        "static-image request resource has no qualified visual-input realization"
                    }
                val original =
                    requireNotNull(
                        store.original(
                            attachment.artifactRef,
                        ),
                    )
                val bytes =
                    requireNotNull(
                        store.rawBytes(
                            attachment.artifactRef,
                        ),
                    )
                val interpreted =
                    currentInterpreter.interpret(
                        VisualInterpretationRequest(
                            requestContent = requestContent,
                            original = original,
                            projection = projection,
                            bytes = bytes,
                        ),
                    )
                VexLifeVisualInputQualification.validate(
                    interpreted.evidence,
                )
                store.storeVisualInterpretation(
                    request = request,
                    attachment = attachment,
                    interpreterRef = interpreted.interpreterRef,
                    evidence = interpreted.evidence,
                    text = interpreted.text,
                )
            }
        }
    }

    private fun failureCode(
        error: Throwable,
    ): String =
        when (
            error
        ) {
            is DirectCompanionCancellation.Cancelled ->
                FailureCancelledByPerson

            is DirectCompanionConnection.ConnectionException ->
                "CONNECTION_" +
                    error.code.name

            is SourceReturnRequestHeldByTurnPolicyException ->
                "SOURCE_RETURN_REQUEST_HELD_BY_TURN_POLICY"

            is RepeatedSourceReturnRequestException ->
                "SOURCE_RETURN_RECURSION_NOT_ADMITTED"

            is CapabilityRequestHeldByTurnPolicyException ->
                "CAPABILITY_REQUEST_HELD_BY_TURN_POLICY"

            is CapabilityRequestHeldByResourceHeadroomException ->
                "CAPABILITY_REACH_RESOURCE_HEADROOM_HELD"

            is CapabilityProviderBridgeHeldException ->
                "CAPABILITY_PROVIDER_BRIDGE_HELD"

            is IllegalArgumentException ->
                "ADMISSION_FAILED"

            else ->
                "TURN_FAILED"
        }

    private fun requireRef(
        value: String,
        label: String,
    ) {
        require(
            value.matches(
                Regex(
                    "[A-Za-z0-9][A-Za-z0-9._:-]{0,220}",
                ),
            ),
        ) {
            "$label must be one portable stable ref"
        }
    }

    private inline fun <reified T> semanticHash(
        value: T,
    ): String =
        sha256(
            VexContractJson.canonicalEncode(
                value,
            ),
        )

    private fun sha256(
        value: String,
    ): String =
        MessageDigest
            .getInstance(
                "SHA-256",
            )
            .digest(
                value.toByteArray(
                    StandardCharsets.UTF_8,
                ),
            )
            .joinToString(
                "",
            ) {
                "%02x".format(
                    it.toInt() and
                        0xff,
                )
            }
}

// [VXG RealForever]
