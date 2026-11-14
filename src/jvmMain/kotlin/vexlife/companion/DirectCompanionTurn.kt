package vexlife.companion

import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.security.MessageDigest
import java.time.Instant
import java.util.UUID
import kotlinx.serialization.Serializable
import vexlife.logging.VexLogger
import vexlife.contract.VexContractJson
import vexlife.contract.VexDurableContractFile
import vexlife.conversation.DirectConversationIdentity
import vexlife.conversation.DirectConversationStore
import vexlife.filesystem.VexOwnedPath
import vexlife.foundational.VexLifeFoundationalField
import vexlife.local.VexLifeProdLocalSetup
import vexlife.memory.VexLifeCurrentMemoryReader
import vexlife.memory.VexLifeMemoryContract
import vexlife.model.heretic.HereticModelConfiguration

/**
 * One direct personal human -> Vex turn over the isolated VexLife-Prod local foundation.
 *
 * This owner composes existing owners; it does not re-own Home, runtime, prompt selection,
 * Conversation persistence, provider invocation or presentation.
 */
object DirectCompanionTurn {
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
    }

    data class Progress(
        val phase: ProgressPhase,
        val delta: String? = null,
        val accumulatedCharacters: Int = 0,
    )

    fun interface ProgressObserver {
        fun observe(
            progress: Progress,
        )
    }

    data class Intent(
        val idempotencyKey: String,
        val content: String,
        val browserContext: DirectPromptContext.BrowserContext? =
            null,
        /** Reply language and private gaps, composed by the service; see [DirectPromptContext.PersonContext]. */
        val personContext: DirectPromptContext.PersonContext? = null,
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
        }
    }

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
        val runtimeObservation: DirectCompanionConnection.RuntimeObservation,
        val invocationEvidence: DirectCompanionConnection.InvocationEvidence,
        /**
         * Ephemeral response-local reasoning for the requesting human only.
         *
         * A recovered completed duplicate always returns null because raw reasoning is deliberately
         * absent from durable Conversation/Context evidence.
         */
        val humanReasoning: String? =
            null,
    )

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

    private class RepeatedSourceReturnRequestException :
        IllegalStateException(
            "final source-return inference cannot request Conversation source again in the same human turn",
        )

    private enum class SourceReturnProgressMode {
        Pending,
        HumanAnswer,
        InternalControl,
    }

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
            val header =
                DirectConversationSourceReturn.RequestHeader
            val controlLike =
                DirectConversationSourceReturn.controlHeaderIndexOrNull(
                    finalContent,
                ) !=
                    null ||
                    (
                        finalContent.length <=
                            header.length &&
                        header.startsWith(
                            finalContent,
                        )
                    )

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
                            val header =
                                DirectConversationSourceReturn.RequestHeader
                            when {
                                DirectConversationSourceReturn.controlHeaderIndexOrNull(
                                    observed,
                                ) !=
                                    null -> {
                                    mode =
                                        SourceReturnProgressMode.InternalControl
                                    bufferedContent.clear()
                                }

                                observed.length <=
                                    DirectConversationSourceReturn.MaximumControlPreambleCharacters +
                                        header.length ->
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

    internal data class Hooks(
        val transport: DirectCompanionConnection.Transport =
            DirectCompanionConnection.SystemTransport,
        val now: () -> Instant = {
            Instant.now()
        },
        val currentRuntimeEvidence: (
            setup: VexLifeProdLocalSetup.Result,
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
        val progressObserver: ProgressObserver? =
            null,
        /** The foundational field as its owner reads it now; re-read at each provider boundary. */
        val currentFoundationalField: () -> VexLifeFoundationalField.Reading = {
            VexLifeFoundationalField.readCurrent()
        },
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
    )

    private data class ContinuityPromptSelection(
        val materialization: DirectPromptContext.Materialization,
        val memoryContext: VexLifeMemoryContract.Context,
    )

    private data class ProviderStage(
        val inference: DirectCompanionConnection.Result,
        val sourceReturnRequest: DirectConversationSourceReturn.Request?,
    )

    private data class FinalProviderTurn(
        val prompt: ContinuityPromptSelection,
        val inference: DirectCompanionConnection.Result,
        val workingContinuity: DirectWorkingContinuity.Prepared,
        val sourceReturnTrace: DirectConversationSourceReturn.Trace?,
    )

    fun perform(
        setup: VexLifeProdLocalSetup.Result,
        intent: Intent,
    ): Result =
        performWithHooks(
            setup = setup,
            intent = intent,
            hooks = Hooks(),
        )

    internal fun perform(
        session: VexLifeProdLocalSetup.Session,
        intent: Intent,
        progressObserver: ProgressObserver? =
            null,
    ): Result =
        performWithHooks(
            setup = session.result,
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
                    progressObserver = progressObserver,
                ),
        )

    internal fun performWithHooks(
        setup: VexLifeProdLocalSetup.Result,
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
            DirectConversationStore(
                productHome,
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
                    current()

                val recovered =
                    completedDuplicate(
                        snapshot = before,
                        contextStore = contextStore,
                        identity = identity,
                        content = intent.content,
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

                val completed =
                    try {
                    request =
                        beginRequest(
                            turnRef = identity.turnRef,
                            requestMessageRef = identity.requestMessageRef,
                            content = intent.content,
                            formedAt = hooks.now(),
                        )
                    // The request is durable now; from here every outcome must terminate as a
                    // completed head or a failure receipt (or be terminalized after a loss).
                    VexLogger.record(
                        VexLogger.Fact.SendAccepted,
                        mapOf(
                            "turnRef" to identity.turnRef,
                            "requestMessageRef" to identity.requestMessageRef,
                            "browserContext" to if (intent.browserContext != null) "included" else "none",
                        ),
                    )
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
                    emitProgress(
                        hooks.progressObserver,
                        Progress(
                            phase = ProgressPhase.Generating,
                        ),
                    )
                    val providerTurn =
                        performProviderTurn(
                            conversationOwner = this,
                            productHome = productHome,
                            setup = setup,
                            currentRequest = request,
                            workingContinuityContext =
                                workingSuccessorContext,
                            browserContext = intent.browserContext,
                            personContext = intent.personContext,
                            hooks = hooks,
                        )
                    val prompt =
                        providerTurn.prompt.materialization
                    val inference =
                        providerTurn.inference
                    val workingContinuity =
                        providerTurn.workingContinuity
                    val sourceReturnTrace =
                        providerTurn.sourceReturnTrace

                    response =
                        appendResponse(
                            request = request,
                            responseMessageRef = identity.responseMessageRef,
                            content = inference.content,
                            model = inference.model,
                            endpointOrigin = inference.invocationEvidence.endpointOrigin,
                            runtimeObservationRef = inference.runtimeObservation.observationRef,
                            invocationEvidenceRef = inference.invocationEvidence.invocationRef,
                            formedAt = hooks.now(),
                        )

                    context =
                        contextStore.record(
                            request = request,
                            response = response,
                            promptContext = prompt.receipt,
                            runtimeObservation = inference.runtimeObservation,
                            invocationEvidence = inference.invocationEvidence,
                            sourceReturnTrace = sourceReturnTrace,
                            formedAt = hooks.now(),
                        )

                    val head =
                        commitCompletedTurn(
                            request = request,
                            response = response,
                            contextSha256 = context.contextSha256,
                            formedAt = hooks.now(),
                        )

                    VexLogger.record(
                        VexLogger.Fact.TurnCompleted,
                        mapOf(
                            "turnRef" to identity.turnRef,
                            "model" to (response.model ?: "unknown"),
                            "sourceReturn" to if (sourceReturnTrace != null) "used" else "none",
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
                                runtimeObservation = inference.runtimeObservation,
                                invocationEvidence = inference.invocationEvidence,
                                humanReasoning = inference.sealedHumanReasoning,
                            ),
                        turnRef = request.turnRef,
                        workingContinuity = workingContinuity,
                    )
                } catch (
                    error: Throwable,
                ) {
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
                                        error,
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

                    VexLogger.record(
                        VexLogger.Fact.TurnFailed,
                        listOfNotNull(
                                "turnRef" to identity.turnRef,
                                "failureCode" to failureCode(error),
                                failure?.let { "failureRef" to it.failureRef },
                                "failureReceipt" to if (failure != null) "written" else "not-written",
                            )
                            .toMap(),
                        detail = error.javaClass.simpleName + ": " + (error.message ?: ""),
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
                                error,
                            ),
                        failureReceipt = failure,
                        retrySameSendAttemptAllowed = false,
                        exactNextSafeRoute = route,
                        cause = error,
                    )
                }

                try {
                    hooks.commitWorkingContinuity(
                        productHome,
                        completed.turnRef,
                        completed.workingContinuity,
                        hooks.now(),
                    )
                } catch (
                    _: Throwable,
                ) {
                    // The Conversation head is already committed. Working continuity is
                    // post-completion stewardship and must never retroactively become a failed send.
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
        setup: VexLifeProdLocalSetup.Result,
        currentRequest: DirectConversationStore.Event,
        workingContinuityContext: DirectWorkingContinuity.CurrentSuccessor?,
        browserContext: DirectPromptContext.BrowserContext?,
        personContext: DirectPromptContext.PersonContext?,
        hooks: Hooks,
    ): FinalProviderTurn {
        val initialMemoryAvailability =
            hooks.currentMemoryContext(
                productHome,
            )
        val initialRuntimeEvidence =
            hooks.currentRuntimeEvidence(
                setup,
            )
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
        val initialPrompt =
            materializeContinuityPrompt(
                conversationOwner = conversationOwner,
                productHome = productHome,
                prior = initialPrior,
                currentRequest = currentRequest,
                memoryAvailability =
                    currentInitialMemoryAvailability,
                memoryContextAtCap =
                    hooks.memoryContextAtCap,
                workingContinuityContext =
                    workingContinuityContext,
                browserContext = browserContext,
                personContext = personContext,
                runtimeEvidence = initialRuntimeEvidence,
                sourceReturnProtocolEnabled = true,
                foundationalField = hooks.currentFoundationalField(),
            )
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
            workingContinuityContext =
                workingContinuityContext,
            browserContext = browserContext,
            personContext = personContext,
            runtimeEvidence = initialRuntimeEvidence,
            sourceReturnProtocolEnabled = true,
            foundationalField = hooks.currentFoundationalField(),
        )
        val initialStage =
            invokeProviderStage(
                runtimeEvidence = initialRuntimeEvidence,
                messages =
                    initialPrompt.materialization.messages,
                hooks = hooks,
                sourceReturnRequestAllowed = true,
            )
        val sourceReturnRequest =
            initialStage.sourceReturnRequest
                ?: return FinalProviderTurn(
                    prompt = initialPrompt,
                    inference = initialStage.inference,
                    workingContinuity =
                        initialWorkingContinuity,
                    sourceReturnTrace = null,
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
            }
        val discovery =
            DirectConversationSourceReturn.discover(
                conversationOwner = conversationOwner,
                request = sourceReturnRequest,
                excludedEventRefs = excludedSourceRefs,
            )

        val finalMemoryAvailability =
            hooks.currentMemoryContext(
                productHome,
            )
        // Source return is a second provider stage of this same human turn, not a fresh
        // runtime-lifecycle boundary. Re-observe currentness without recovery and require the same
        // receipt/process identity already proved before the first provider call. A runtime that
        // disappeared or was replaced ends this turn instead of being silently substituted.
        val finalRuntimeEvidence =
            hooks.reobserveSameRuntimeEvidence(
                initialRuntimeEvidence,
            )
                .also {
                    current ->
                    require(
                        current.runtimeReceiptRef ==
                            initialRuntimeEvidence.runtimeReceiptRef,
                    ) {
                        "runtime changed during one direct Companion turn"
                    }
                }
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
                workingContinuityContext =
                    workingContinuityContext,
                browserContext = browserContext,
                personContext = personContext,
                runtimeEvidence = finalRuntimeEvidence,
                sourceReturnProtocolEnabled = false,
                sourceReturnDiscovery = discovery,
                foundationalField = hooks.currentFoundationalField(),
            )
        // Source return adds exact older Conversation; it must not change the ground Vex was given
        // when it asked for it.
        require(
            finalPrompt.materialization.receipt.foundationalField ==
                initialPrompt.materialization.receipt.foundationalField,
        ) {
            "foundational field changed between the two inferences of one human turn"
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
            workingContinuityContext =
                workingContinuityContext,
            browserContext = browserContext,
            personContext = personContext,
            runtimeEvidence = finalRuntimeEvidence,
            sourceReturnProtocolEnabled = false,
            sourceReturnDiscovery = discovery,
            foundationalField = hooks.currentFoundationalField(),
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
                inference = initialStage.inference,
                discovery = discovery,
                projection = projection,
            )
        val finalStage =
            invokeProviderStage(
                runtimeEvidence = finalRuntimeEvidence,
                messages =
                    finalPrompt.materialization.messages,
                hooks = hooks,
                sourceReturnRequestAllowed = false,
            )
        val sourceReturnTrace =
            DirectConversationSourceReturn.formTrace(
                attempt = attemptTrace,
                finalPrompt =
                    finalPrompt.materialization.receipt,
            )

        return FinalProviderTurn(
            prompt = finalPrompt,
            inference = finalStage.inference,
            workingContinuity =
                finalWorkingContinuity,
            sourceReturnTrace = sourceReturnTrace,
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
        workingContinuityContext: DirectWorkingContinuity.CurrentSuccessor?,
        browserContext: DirectPromptContext.BrowserContext?,
        personContext: DirectPromptContext.PersonContext?,
        runtimeEvidence: HereticModelConfiguration.QualifiedRuntimeEvidence,
        sourceReturnProtocolEnabled: Boolean,
        sourceReturnDiscovery: DirectConversationSourceReturn.Discovery? =
            null,
        foundationalField: VexLifeFoundationalField.Reading,
    ): ContinuityPromptSelection {
        var selectedMemoryAtCap: VexLifeMemoryContract.Context? =
            null
        val materialization =
            DirectPromptContext.materializeContinuity(
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
                workingContinuityContext = workingContinuityContext,
                browserContext = browserContext,
                personContext = personContext,
                runtimeEvidence = runtimeEvidence,
                sourceReturnProtocolEnabled =
                    sourceReturnProtocolEnabled,
                sourceReturnDiscovery = sourceReturnDiscovery,
                foundationalField = foundationalField,
                conversationExtentIncluded = true,
            )
        val selectedMemory =
            selectedMemoryAtCap
                ?: memoryAvailability
        require(
            materialization.receipt.memoryContextRef ==
                selectedMemory.memoryContextRef &&
                materialization.receipt.memoryContextFingerprint ==
                selectedMemory.semanticFingerprint,
        ) {
            "direct Companion continuity prompt does not bind the exact selected Memory projection"
        }

        return ContinuityPromptSelection(
            materialization = materialization,
            memoryContext = selectedMemory,
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
        workingContinuityContext: DirectWorkingContinuity.CurrentSuccessor?,
        browserContext: DirectPromptContext.BrowserContext?,
        personContext: DirectPromptContext.PersonContext?,
        runtimeEvidence: HereticModelConfiguration.QualifiedRuntimeEvidence,
        sourceReturnProtocolEnabled: Boolean,
        sourceReturnDiscovery: DirectConversationSourceReturn.Discovery? =
            null,
        foundationalField: VexLifeFoundationalField.Reading,
    ) {
        DirectPromptContext.reverifyContinuity(
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
            workingContinuityContext = workingContinuityContext,
            browserContext = browserContext,
            personContext = personContext,
            runtimeEvidence = runtimeEvidence,
            sourceReturnProtocolEnabled =
                sourceReturnProtocolEnabled,
            sourceReturnDiscovery = sourceReturnDiscovery,
            foundationalField = foundationalField,
            conversationExtentIncluded = true,
        )
    }

    private fun invokeProviderStage(
        runtimeEvidence: HereticModelConfiguration.QualifiedRuntimeEvidence,
        messages: List<CompanionProviderMessage>,
        hooks: Hooks,
        sourceReturnRequestAllowed: Boolean,
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
                "stage" to if (sourceReturnRequestAllowed) "initial" else "final",
                "messageCount" to messages.size.toString(),
                "approxChars" to messages.sumOf { it.content.length }.toString(),
            ),
        )
        val inference =
            DirectCompanionConnection.invoke(
                runtime = runtimeEvidence,
                messages = messages,
                transport = hooks.transport,
                progressObserver =
                    progressGate.connectionObserver(),
                now = hooks.now,
            )
        val controlLike =
            progressGate.finish(
                inference.content,
            )
        val sourceReturnRequest =
            DirectConversationSourceReturn.parseRequestOrNull(
                inference.content,
            )
        require(
            !controlLike ||
                sourceReturnRequest !=
                null,
        ) {
            "provider output resembles an incomplete Conversation source-return control request"
        }
        if (
            !sourceReturnRequestAllowed &&
            sourceReturnRequest !=
                null
        ) {
            throw RepeatedSourceReturnRequestException()
        }

        return ProviderStage(
            inference = inference,
            sourceReturnRequest = sourceReturnRequest,
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

    private fun turnIdentity(
        idempotencyKey: String,
    ): Identity {
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
        val requestMessageRef =
            "message.vexlife.conversation.human." +
                requestFingerprint.take(
                    32,
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
        snapshot: DirectConversationStore.Snapshot,
        contextStore: DirectTurnContextStore,
        identity: Identity,
        content: String,
        browserContext: DirectPromptContext.BrowserContext?,
        productHome: Path,
    ): Result? {
        val canonicalRequest =
            snapshot.canonicalEvents
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

            val response =
                requireNotNull(
                    snapshot.canonicalEvents
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
                        snapshot.head,
                    )
                        .conversationHeadSha256,
                contextPath = context.contextPath,
                promptContextReceipt = context.promptContext,
                runtimeObservation = context.runtimeObservation,
                invocationEvidence = context.invocationEvidence,
            )
        }

        val failed =
            snapshot.abandonedAttempts
                .firstOrNull {
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

        val receipt =
            readFailureReceipt(
                productHome = productHome,
                turnRef = failed.requestEvent.turnRef,
            )
        val route =
            receipt
                ?.exactNextSafeRoute
                ?: if (
                    snapshot.head ==
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

    private fun failureCode(
        error: Throwable,
    ): String =
        when (
            error
        ) {
            is DirectCompanionConnection.ConnectionException ->
                "CONNECTION_" +
                    error.code.name

            is RepeatedSourceReturnRequestException ->
                "SOURCE_RETURN_RECURSION_NOT_ADMITTED"

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
