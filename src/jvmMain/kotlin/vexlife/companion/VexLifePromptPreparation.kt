package vexlife.companion

import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import vexlife.logging.VexLogger
import vexlife.model.heretic.HereticModelCatalog
import vexlife.model.heretic.HereticModelConfiguration

/**
 * In-memory owner of reusable provider-prefix preparation for the exact current local runtime session.
 *
 * This owner retains only preparation/currentness of one exact provider prefix. It never owns
 * Foundation, Context Profile, Memory, Conversation, the human Send, runtime lifetime or
 * presentation. The semantic prompt owner forms the prefix; the provider adapter realizes cache
 * work; a real turn must re-materialize and prove exact currentness before consuming readiness.
 */
internal class VexLifePromptPreparation(
    private val providerPrefill: ProviderPrefill =
        ProviderPrefill {
            runtime,
            systemContent,
            cancellation,
            ->
            DirectCompanionConnection.prefillStableSystemPrefix(
                runtime = runtime,
                systemContent = systemContent,
                cancellation = cancellation,
            )
        },
    private val maximumPreparationMillis: Long = MaximumPreparationMillis,
    private val humanTurnWaitMillis: Long = HumanTurnWaitMillis,
    private val cancellationDrainMillis: Long = CancellationDrainMillis,
) : AutoCloseable {
    companion object {
        /**
         * Provider-prefix preparation remains best-effort. Lived provider evidence may tune these bounded timings;
         * timers never create READY_REUSABLE.
         */
        const val MaximumPreparationMillis = 30_000L
        const val HumanTurnWaitMillis = 30_000L
        const val CancellationDrainMillis = 5_000L
    }

    internal fun interface ProviderPrefill {
        fun perform(
            runtime: HereticModelConfiguration.QualifiedRuntimeEvidence,
            systemContent: String,
            cancellation: DirectCompanionCancellation,
        ): DirectCompanionConnection.SystemPrefixPrefillResult
    }

    internal fun interface PrefixProvider {
        fun current(
            runtime: HereticModelConfiguration.QualifiedRuntimeEvidence,
        ): DirectPromptContext.ProviderPreparationPrefix
    }

    internal data class Binding(
        val runtimeReceiptRef: String,
        val modelRef: String,
        val realizationRef: String,
        val modelProfileRef: String,
        val companionProfileRef: String,
        val systemMessageSha256: String,
        val foundationalFieldRef: String?,
        val foundationalFieldVersion: String?,
        val foundationalBodySha256: String?,
        val contextProfileRef: String?,
        val contextProfileFingerprint: String?,
        val memoryContextRef: String?,
        val memoryContextFingerprint: String?,
        val capabilityRequestsAdmitted: Boolean,
        val providerNativeTools: Boolean,
        val leadingMessage: CompanionProviderMessage,
    )

    internal sealed interface State {
        val stateRef: String

        data object Cold : State {
            override val stateRef = "COLD"
        }

        data class Preparing(
            val binding: Binding,
        ) : State {
            override val stateRef = "PREPARING"
        }

        data class ReadyReusable(
            val binding: Binding,
            val provider: DirectCompanionConnection.SystemPrefixPrefillResult.Performed,
        ) : State {
            override val stateRef = "READY_REUSABLE"
        }

        data class Held(
            val binding: Binding?,
            val reasonRef: String,
        ) : State {
            override val stateRef = "HELD"
        }

        data class Failed(
            val binding: Binding?,
            val failureClass: String,
        ) : State {
            override val stateRef = "FAILED"
        }

        data class Invalidated(
            val binding: Binding?,
            val reasonRef: String,
        ) : State {
            override val stateRef = "INVALIDATED"
        }

        data class Consumed(
            val binding: Binding,
        ) : State {
            override val stateRef = "CONSUMED"
        }
    }

    internal data class Validation(
        val runtime: HereticModelConfiguration.QualifiedRuntimeEvidence,
        val leadingMessage: CompanionProviderMessage?,
        val foundationalFieldRef: String?,
        val foundationalFieldVersion: String?,
        val foundationalBodySha256: String?,
        val contextProfileRef: String?,
        val contextProfileFingerprint: String?,
        val memoryContextRef: String?,
        val memoryContextFingerprint: String?,
        val capabilityRequestsAdmitted: Boolean,
        val providerNativeTools: Boolean,
    )

    internal sealed interface Decision {
        val reusable: Boolean

        data class Reusable(
            val binding: Binding,
            val provider: DirectCompanionConnection.SystemPrefixPrefillResult.Performed,
        ) : Decision {
            override val reusable = true
        }

        data class Cold(
            val stateRef: String,
            val reasonRef: String,
        ) : Decision {
            override val reusable = false
        }
    }

    private data class Attempt(
        val binding: Binding,
        val completion: CountDownLatch =
            CountDownLatch(
                1,
            ),
        val cancellation: DirectCompanionCancellation =
            DirectCompanionCancellation(),
        val cancellationReason: AtomicReference<String?> =
            AtomicReference(
                null,
            ),
    )

    private val state =
        AtomicReference<State>(
            State.Cold,
        )
    private val currentAttempt =
        AtomicReference<Attempt?>(
            null,
        )
    private val executor =
        AtomicReference<ExecutorService?>(
            null,
        )
    private val prefixProvider =
        AtomicReference<PrefixProvider?>(
            null,
        )

    init {
        require(
            maximumPreparationMillis > 0L &&
                humanTurnWaitMillis >= 0L &&
                cancellationDrainMillis > 0L,
        ) {
            "prompt preparation timing bounds are invalid"
        }
    }

    internal fun state(): State =
        state.get()

    internal fun attach(
        executor: ExecutorService,
        prefixProvider: PrefixProvider,
    ) {
        val priorExecutor =
            this.executor.get()
        require(
            priorExecutor == null ||
                priorExecutor ===
                executor,
        ) {
            "prompt preparation execution custody cannot change while the service is alive"
        }
        this.executor.compareAndSet(
            null,
            executor,
        )

        val priorProvider =
            this.prefixProvider.get()
        require(
            priorProvider == null ||
                priorProvider ===
                prefixProvider,
        ) {
            "prompt preparation prefix owner cannot change while the service is alive"
        }
        this.prefixProvider.compareAndSet(
            null,
            prefixProvider,
        )
    }

    /**
     * Start one best-effort preparation generation for the exact current local runtime Session.
     *
     * This may be called after Product startup and again after later model/runtime convergence.
     * Terminal prior generations are provenance only; they do not prevent the current Session from
     * preparing a fresh exact prefix.
     */
    internal fun startCurrent(
        runtime: HereticModelConfiguration.QualifiedRuntimeEvidence,
    ) {
        val currentExecutor =
            requireNotNull(
                executor.get(),
            ) {
                "prompt preparation execution custody is not attached"
            }
        val currentPrefixProvider =
            requireNotNull(
                prefixProvider.get(),
            ) {
                "prompt preparation prefix owner is not attached"
            }
        try {
            start(
                executor = currentExecutor,
                runtime = runtime,
                prefix =
                    currentPrefixProvider.current(
                        runtime,
                    ),
            )
        } catch (
            error: Throwable,
        ) {
            failBeforeStart(
                error,
            )
        }
    }

    /**
     * Whether the next human turn still has one current preparation generation that may become
     * reusable. This is not limited to Product boot; a successful model/runtime switch can re-arm it.
     */
    internal fun nextTurnPreparationPending(): Boolean =
        when (
            state.get()
        ) {
            State.Cold,
            is State.Preparing,
            is State.ReadyReusable,
            ->
                true

            is State.Held,
            is State.Failed,
            is State.Invalidated,
            is State.Consumed,
            ->
                false
        }

    /** Preparation formation is best-effort; failure never controls Product availability. */
    internal fun failBeforeStart(
        error: Throwable,
    ) {
        val failed =
            State.Failed(
                binding = null,
                failureClass = error.javaClass.simpleName,
            )
        while (
            true
        ) {
            val current =
                state.get()
            if (
                current is State.Preparing ||
                current is State.ReadyReusable
            ) {
                return
            }
            if (
                state.compareAndSet(
                    current,
                    failed,
                )
            ) {
                log(
                    failed,
                )
                return
            }
        }
    }

    internal fun start(
        executor: ExecutorService,
        runtime: HereticModelConfiguration.QualifiedRuntimeEvidence,
        prefix: DirectPromptContext.ProviderPreparationPrefix,
    ) {
        this.executor.compareAndSet(
            null,
            executor,
        )
        val message =
            prefix.messages.singleOrNull()
        val entry =
            HereticModelCatalog.entry(
                runtime.modelRef,
            )
        if (
            message == null ||
                message.role != "system" ||
                entry == null
        ) {
            val held =
                State.Held(
                    binding = null,
                    reasonRef =
                        when {
                            message == null ->
                                "PREPARATION_PREFIX_NOT_SINGLE_MESSAGE"

                            message.role != "system" ->
                                "PREPARATION_PREFIX_NOT_SYSTEM"

                            else ->
                                "MODEL_NOT_PRODUCT_CATALOG"
                        },
                )
            while (
                true
            ) {
                val current =
                    state.get()
                if (
                    current is State.Preparing ||
                    current is State.ReadyReusable
                ) {
                    return
                }
                if (
                    state.compareAndSet(
                        current,
                        held,
                    )
                ) {
                    log(
                        held,
                    )
                    return
                }
            }
        }

        val field =
            prefix.foundationalField
        val binding =
            Binding(
                runtimeReceiptRef = runtime.runtimeReceiptRef,
                modelRef = runtime.modelRef,
                realizationRef = runtime.realizationRef,
                modelProfileRef = runtime.modelProfileRef,
                companionProfileRef = entry.companionProfile.profileRef,
                systemMessageSha256 = prefix.systemMessageSha256,
                foundationalFieldRef = field?.fieldRef,
                foundationalFieldVersion = field?.fieldVersion,
                foundationalBodySha256 = field?.bodySha256,
                contextProfileRef = prefix.contextProfileRef,
                contextProfileFingerprint = prefix.contextProfileFingerprint,
                memoryContextRef = prefix.memoryContextRef,
                memoryContextFingerprint = prefix.memoryContextFingerprint,
                capabilityRequestsAdmitted = prefix.capabilityRequestsAdmitted,
                providerNativeTools = prefix.providerNativeTools,
                leadingMessage = message,
            )
        val attempt =
            Attempt(
                binding = binding,
            )
        val completion =
            attempt.completion
        val cancellation =
            attempt.cancellation
        val cancellationReason =
            attempt.cancellationReason
        val preparing =
            State.Preparing(
                binding,
            )
        val admitted =
            synchronized(
                this,
            ) {
                val current =
                    state.get()
                if (
                    current is State.Preparing ||
                    current is State.ReadyReusable
                ) {
                    false
                } else {
                    // Publish the attempt fence before PREPARING becomes observable. A human turn
                    // may therefore never observe PREPARING without the exact generation it must
                    // wait on or invalidate.
                    currentAttempt.set(
                        attempt,
                    )
                    state.set(
                        preparing,
                    )
                    true
                }
            }
        if (
            !admitted
        ) {
            return
        }
        log(
            preparing,
        )

        try {
            executor.submit {
                try {
                    cancellation
                        .bindCurrentThread()
                        .use {
                            val provider =
                                providerPrefill.perform(
                                    runtime = runtime,
                                    systemContent = message.content,
                                    cancellation = cancellation,
                                )
                            when (
                                provider
                            ) {
                                is DirectCompanionConnection.SystemPrefixPrefillResult.Performed -> {
                                    require(
                                        provider.runtimeReceiptRef ==
                                            binding.runtimeReceiptRef &&
                                            provider.modelRef ==
                                            binding.modelRef &&
                                            provider.systemMessageSha256 ==
                                            binding.systemMessageSha256,
                                    ) {
                                        "provider prefill result does not bind the exact prompt preparation"
                                    }
                                    val ready =
                                        State.ReadyReusable(
                                            binding = binding,
                                            provider = provider,
                                        )
                                    if (
                                        state.compareAndSet(
                                            preparing,
                                            ready,
                                        )
                                    ) {
                                        log(
                                            ready,
                                        )
                                    }
                                }

                                is DirectCompanionConnection.SystemPrefixPrefillResult.Held -> {
                                    val held =
                                        State.Held(
                                            binding = binding,
                                            reasonRef = provider.reasonRef,
                                        )
                                    if (
                                        state.compareAndSet(
                                            preparing,
                                            held,
                                        )
                                    ) {
                                        log(
                                            held,
                                        )
                                    }
                                }
                            }
                        }
                } catch (
                    _: DirectCompanionCancellation.Cancelled,
                ) {
                    val invalidated =
                        State.Invalidated(
                            binding = binding,
                            reasonRef =
                                cancellationReason.get()
                                    ?: "PREPARATION_CANCELLED",
                        )
                    if (
                        state.compareAndSet(
                            preparing,
                            invalidated,
                        )
                    ) {
                        log(
                            invalidated,
                        )
                    }
                } catch (
                    error: Throwable,
                ) {
                    val failed =
                        State.Failed(
                            binding = binding,
                            failureClass = error.javaClass.simpleName,
                        )
                    if (
                        state.compareAndSet(
                            preparing,
                            failed,
                        )
                    ) {
                        log(
                            failed,
                        )
                    }
                } finally {
                    completion.countDown()
                }
            }
        } catch (
            error: Throwable,
        ) {
            val failed =
                State.Failed(
                    binding = binding,
                    failureClass = error.javaClass.simpleName,
                )
            state.compareAndSet(
                preparing,
                failed,
            )
            completion.countDown()
            log(
                state.get(),
            )
            return
        }

        try {
            executor.submit {
                try {
                    if (
                        !completion.await(
                            maximumPreparationMillis,
                            TimeUnit.MILLISECONDS,
                        )
                    ) {
                        requestCancellation(
                            attempt,
                            "MAXIMUM_LIFETIME",
                        )
                    }
                } catch (
                    interrupted: InterruptedException,
                ) {
                    Thread.currentThread().interrupt()
                }
            }
        } catch (
            _: Throwable,
        ) {
            // The worker is already admitted. Do not pretend it completed merely because the
            // watchdog could not start; cancel it and let the worker's own finally block publish
            // the terminal completion signal before any human turn may proceed.
            requestCancellation(
                attempt,
                "WATCHDOG_START_FAILED",
            )
        }
    }

    /**
     * Called only after the human REQUEST is canonical and the real prompt has been materialized and
     * reverified. A warm candidate is consumed only when every bound owner still agrees exactly.
     */
    internal fun awaitAndConsume(
        validation: Validation,
    ): Decision {
        var current =
            state.get()
        if (
            current is
                State.Preparing
        ) {
            val attempt =
                requireNotNull(
                    currentAttempt.get(),
                ) {
                    "preparing prompt state has no active attempt"
                }
            require(
                attempt.binding ==
                    current.binding,
            ) {
                "preparing prompt attempt does not bind current preparation state"
            }
            try {
                if (
                    !attempt.completion.await(
                        humanTurnWaitMillis,
                        TimeUnit.MILLISECONDS,
                    )
                ) {
                    requestCancellation(
                        attempt,
                        "HUMAN_TURN_WAIT_TIMEOUT",
                    )
                    if (
                        !attempt.completion.await(
                            cancellationDrainMillis,
                            TimeUnit.MILLISECONDS,
                        )
                    ) {
                        throw IllegalStateException(
                            "prompt preparation did not yield after bounded cancellation",
                        )
                    }
                }
            } catch (
                interrupted: InterruptedException,
            ) {
                Thread.currentThread().interrupt()
                requestCancellation(
                    attempt,
                    "HUMAN_TURN_INTERRUPTED",
                )
                throw IllegalStateException(
                    "human turn was interrupted while waiting for prompt preparation",
                    interrupted,
                )
            }
            current =
                state.get()
        }

        return when (
            current
        ) {
            is State.ReadyReusable -> {
                val reason =
                    mismatchReason(
                        binding = current.binding,
                        validation = validation,
                    )
                if (
                    reason !=
                        null
                ) {
                    val invalidated =
                        State.Invalidated(
                            binding = current.binding,
                            reasonRef = reason,
                        )
                    state.compareAndSet(
                        current,
                        invalidated,
                    )
                    log(
                        state.get(),
                    )
                    Decision.Cold(
                        stateRef = state.get().stateRef,
                        reasonRef = reason,
                    )
                } else {
                    val consumed =
                        State.Consumed(
                            current.binding,
                        )
                    if (
                        state.compareAndSet(
                            current,
                            consumed,
                        )
                    ) {
                        log(
                            consumed,
                        )
                        Decision.Reusable(
                            binding = current.binding,
                            provider = current.provider,
                        )
                    } else {
                        Decision.Cold(
                            stateRef = state.get().stateRef,
                            reasonRef = "PREPARATION_ALREADY_CONSUMED",
                        )
                    }
                }
            }

            is State.Held ->
                Decision.Cold(
                    stateRef = current.stateRef,
                    reasonRef = current.reasonRef,
                )

            is State.Failed ->
                Decision.Cold(
                    stateRef = current.stateRef,
                    reasonRef = current.failureClass,
                )

            is State.Invalidated ->
                Decision.Cold(
                    stateRef = current.stateRef,
                    reasonRef = current.reasonRef,
                )

            is State.Consumed ->
                Decision.Cold(
                    stateRef = current.stateRef,
                    reasonRef = "PREPARATION_ALREADY_CONSUMED",
                )

            State.Cold ->
                Decision.Cold(
                    stateRef = current.stateRef,
                    reasonRef = "PREPARATION_NOT_STARTED",
                )

            is State.Preparing ->
                Decision.Cold(
                    stateRef = current.stateRef,
                    reasonRef = "PREPARATION_NOT_TERMINAL",
                )
        }
    }

    internal fun invalidateAndAwait(
        reasonRef: String,
    ) {
        val current =
            state.get()
        when (
            current
        ) {
            is State.Preparing -> {
                val attempt =
                    requireNotNull(
                        currentAttempt.get(),
                    ) {
                        "preparing prompt state has no active attempt"
                    }
                require(
                    attempt.binding ==
                        current.binding,
                ) {
                    "preparing prompt attempt does not bind current preparation state"
                }
                requestCancellation(
                    attempt,
                    reasonRef,
                )
                try {
                    if (
                        !attempt.completion.await(
                            cancellationDrainMillis,
                            TimeUnit.MILLISECONDS,
                        )
                    ) {
                        throw IllegalStateException(
                            "prompt preparation did not retire after invalidation",
                        )
                    }
                } catch (
                    interrupted: InterruptedException,
                ) {
                    Thread.currentThread().interrupt()
                    throw IllegalStateException(
                        "prompt preparation invalidation was interrupted",
                        interrupted,
                    )
                }

                // Provider completion may race the cancellation request:
                //
                // PREPARING
                // -> cancellation requested
                // -> provider returns successfully
                // -> READY_REUSABLE
                // -> completion fence opens
                //
                // Reconcile only this exact generation after the drain so a raced reusable result
                // cannot survive MODEL_SWITCH. Never invalidate a newer generation that may have
                // started after this attempt became terminal.
                while (
                    true
                ) {
                    val terminal =
                        state.get()
                    if (
                        terminal !is
                            State.ReadyReusable ||
                        terminal.binding !=
                            attempt.binding
                    ) {
                        break
                    }
                    val invalidated =
                        State.Invalidated(
                            binding = terminal.binding,
                            reasonRef = reasonRef,
                        )
                    if (
                        state.compareAndSet(
                            terminal,
                            invalidated,
                        )
                    ) {
                        log(
                            invalidated,
                        )
                        break
                    }
                }
            }

            is State.ReadyReusable -> {
                val invalidated =
                    State.Invalidated(
                        binding = current.binding,
                        reasonRef = reasonRef,
                    )
                if (
                    state.compareAndSet(
                        current,
                        invalidated,
                    )
                ) {
                    log(
                        invalidated,
                    )
                }
            }

            else ->
                Unit
        }
    }

    override fun close() {
        invalidateAndAwait(
            "SERVICE_CLOSE",
        )
    }

    private fun requestCancellation(
        attempt: Attempt,
        reasonRef: String,
    ) {
        attempt.cancellationReason.compareAndSet(
            null,
            reasonRef,
        )
        attempt.cancellation.request()
    }

    private fun mismatchReason(
        binding: Binding,
        validation: Validation,
    ): String? {
        val runtime =
            validation.runtime
        return when {
            runtime.runtimeReceiptRef !=
                binding.runtimeReceiptRef ->
                "RUNTIME_RECEIPT_CHANGED"

            runtime.modelRef !=
                binding.modelRef ->
                "MODEL_CHANGED"

            runtime.realizationRef !=
                binding.realizationRef ->
                "RUNTIME_REALIZATION_CHANGED"

            runtime.modelProfileRef !=
                binding.modelProfileRef ->
                "MODEL_PROFILE_CHANGED"

            HereticModelCatalog.entry(
                runtime.modelRef,
            )
                ?.companionProfile
                ?.profileRef !=
                binding.companionProfileRef ->
                "PROVIDER_REPRESENTATION_CHANGED"

            validation.leadingMessage !=
                binding.leadingMessage ->
                "PROVIDER_PREFIX_CHANGED"

            validation.foundationalFieldRef !=
                binding.foundationalFieldRef ||
                validation.foundationalFieldVersion !=
                binding.foundationalFieldVersion ||
                validation.foundationalBodySha256 !=
                binding.foundationalBodySha256 ->
                "FOUNDATION_CHANGED"

            validation.contextProfileRef !=
                binding.contextProfileRef ||
                validation.contextProfileFingerprint !=
                binding.contextProfileFingerprint ->
                "CONTEXT_PROFILE_CHANGED"

            validation.memoryContextRef !=
                binding.memoryContextRef ||
                validation.memoryContextFingerprint !=
                binding.memoryContextFingerprint ->
                "MEMORY_CHANGED"

            validation.capabilityRequestsAdmitted !=
                binding.capabilityRequestsAdmitted ||
                validation.providerNativeTools !=
                binding.providerNativeTools ->
                "CAPABILITY_REPRESENTATION_CHANGED"

            else ->
                null
        }
    }

    private fun log(
        current: State,
    ) {
        val binding =
            when (
                current
            ) {
                is State.Preparing ->
                    current.binding

                is State.ReadyReusable ->
                    current.binding

                is State.Held ->
                    current.binding

                is State.Failed ->
                    current.binding

                is State.Invalidated ->
                    current.binding

                is State.Consumed ->
                    current.binding

                State.Cold ->
                    null
            }
        val refs =
            buildMap {
                put(
                    "state",
                    current.stateRef,
                )
                binding
                    ?.let {
                        put(
                            "runtimeReceiptRef",
                            it.runtimeReceiptRef,
                        )
                        put(
                            "modelRef",
                            it.modelRef,
                        )
                        put(
                            "prefixSha",
                            it.systemMessageSha256,
                        )
                    }
                when (
                    current
                ) {
                    is State.Held ->
                        put(
                            "reason",
                            current.reasonRef,
                        )

                    is State.Failed ->
                        put(
                            "failureClass",
                            current.failureClass,
                        )

                    is State.Invalidated ->
                        put(
                            "reason",
                            current.reasonRef,
                        )

                    is State.ReadyReusable ->
                        put(
                            "providerTotalMs",
                            current.provider.totalMillis.toString(),
                        )

                    else ->
                        Unit
                }
            }
        VexLogger.record(
            VexLogger.Fact.PromptPreparationObserved,
            refs,
        )
    }
}

// [VXG RealForever]
