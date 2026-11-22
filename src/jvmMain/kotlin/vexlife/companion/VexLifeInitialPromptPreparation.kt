package vexlife.companion

import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import vexlife.logging.VexLogger
import vexlife.model.heretic.HereticModelCatalog
import vexlife.model.heretic.HereticModelConfiguration

/**
 * In-memory owner of the first reusable provider prefix prepared after Product startup.
 *
 * This owner retains only preparation/currentness of one exact provider prefix. It never owns
 * Foundation, Context Profile, Memory, Conversation, the human Send, runtime lifetime or
 * presentation. The semantic prompt owner forms the prefix; the provider adapter realizes cache
 * work; a real turn must re-materialize and prove exact currentness before consuming readiness.
 */
internal class VexLifeInitialPromptPreparation(
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
         * Initial source candidate only. Lived provider evidence may tune these bounded timings;
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

    private val state =
        AtomicReference<State>(
            State.Cold,
        )
    private val completion =
        CountDownLatch(
            1,
        )
    private val cancellation =
        DirectCompanionCancellation()
    private val cancellationReason =
        AtomicReference<String?>(
            null,
        )

    init {
        require(
            maximumPreparationMillis > 0L &&
                humanTurnWaitMillis >= 0L &&
                cancellationDrainMillis > 0L,
        ) {
            "initial prompt preparation timing bounds are invalid"
        }
    }

    internal fun state(): State =
        state.get()

    /** Startup candidate formation is best-effort; failure never controls Product availability. */
    internal fun failBeforeStart(
        error: Throwable,
    ) {
        val failed =
            State.Failed(
                binding = null,
                failureClass = error.javaClass.simpleName,
            )
        if (
            state.compareAndSet(
                State.Cold,
                failed,
            )
        ) {
            completion.countDown()
            log(
                failed,
            )
        }
    }

    internal fun start(
        executor: ExecutorService,
        runtime: HereticModelConfiguration.QualifiedRuntimeEvidence,
        prefix: DirectPromptContext.InitialBootPrefix,
    ) {
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
                                "INITIAL_PREFIX_NOT_SINGLE_MESSAGE"

                            message.role != "system" ->
                                "INITIAL_PREFIX_NOT_SYSTEM"

                            else ->
                                "MODEL_NOT_PRODUCT_CATALOG"
                        },
                )
            if (
                state.compareAndSet(
                    State.Cold,
                    held,
                )
            ) {
                completion.countDown()
                log(
                    held,
                )
            }
            return
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
        val preparing =
            State.Preparing(
                binding,
            )
        if (
            !state.compareAndSet(
                State.Cold,
                preparing,
            )
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
                                        "provider prefill result does not bind the exact initial prompt preparation"
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

            executor.submit {
                try {
                    if (
                        !completion.await(
                            maximumPreparationMillis,
                            TimeUnit.MILLISECONDS,
                        )
                    ) {
                        requestCancellation(
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
            try {
                if (
                    !completion.await(
                        humanTurnWaitMillis,
                        TimeUnit.MILLISECONDS,
                    )
                ) {
                    requestCancellation(
                        "HUMAN_TURN_WAIT_TIMEOUT",
                    )
                    if (
                        !completion.await(
                            cancellationDrainMillis,
                            TimeUnit.MILLISECONDS,
                        )
                    ) {
                        throw IllegalStateException(
                            "initial prompt preparation did not yield after bounded cancellation",
                        )
                    }
                }
            } catch (
                interrupted: InterruptedException,
            ) {
                Thread.currentThread().interrupt()
                requestCancellation(
                    "HUMAN_TURN_INTERRUPTED",
                )
                throw IllegalStateException(
                    "human turn was interrupted while waiting for initial prompt preparation",
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
                requestCancellation(
                    reasonRef,
                )
                try {
                    if (
                        !completion.await(
                            cancellationDrainMillis,
                            TimeUnit.MILLISECONDS,
                        )
                    ) {
                        throw IllegalStateException(
                            "initial prompt preparation did not retire after invalidation",
                        )
                    }
                } catch (
                    interrupted: InterruptedException,
                ) {
                    Thread.currentThread().interrupt()
                    throw IllegalStateException(
                        "initial prompt preparation invalidation was interrupted",
                        interrupted,
                    )
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
        reasonRef: String,
    ) {
        cancellationReason.compareAndSet(
            null,
            reasonRef,
        )
        cancellation.request()
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
