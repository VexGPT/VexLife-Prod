package vexlife.companion

import java.util.concurrent.AbstractExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import vexlife.foundational.VexLifeFoundationalFieldFixture
import vexlife.memory.VexLifeMemoryContextFixture
import vexlife.model.heretic.HereticModelCatalog
import vexlife.model.heretic.HereticModelConfiguration
import vexlife.model.heretic.HereticRuntimeQualification
import vexlife.model.heretic.HereticRuntimeRealization
import vexlife.model.heretic.HereticRuntimeState

class VexLifePromptPreparationTest {
    private fun runtime(
        receiptRef: String =
            "receipt.vexlife-prod.prompt-preparation.test",
    ): HereticModelConfiguration.QualifiedRuntimeEvidence {
        val model =
            HereticModelCatalog.Models.Qwen4B
        val realization =
            requireNotNull(
                HereticRuntimeRealization.resolve(
                    model = model,
                    target = HereticRuntimeRealization.MacArm64,
                ),
            )
        return HereticModelConfiguration.QualifiedRuntimeEvidence(
            runtimeReceiptRef = receiptRef,
            runtimeState = HereticRuntimeState.Qualified,
            modelRef = realization.modelRef,
            realizationRef = realization.realizationRef,
            modelProfileRef = realization.modelProfileRef,
            artifactRef = realization.artifact.artifactRef,
            artifactContentSetSha256 = realization.artifact.contentSetSha256,
            runtimeAdapterRef = realization.runtime.runtimeAdapterRef,
            runtimeClass = realization.runtime.runtimeClass,
            endpoint = realization.runtime.endpoint,
            requestModel = realization.runtime.requestModel,
            qualification = HereticRuntimeQualification.qualified(),
            contextWindowTokens =
                realization.runtime.livedLaunchProfile
                    ?.contextTokens,
        )
    }

    private fun prefix(): DirectPromptContext.InitialBootPrefix =
        DirectPromptContext.projectInitialBootPrefix(
            foundationalField =
                VexLifeFoundationalFieldFixture.shipped,
            memoryContext =
                VexLifeMemoryContextFixture.current(),
            capabilityRequestsAdmitted =
                false,
            providerNativeTools =
                false,
        )

    private fun validation(
        runtime: HereticModelConfiguration.QualifiedRuntimeEvidence,
        prefix: DirectPromptContext.InitialBootPrefix,
        leadingMessage: CompanionProviderMessage? =
            prefix.messages.single(),
        memoryFingerprint: String? =
            prefix.memoryContextFingerprint,
    ): VexLifePromptPreparation.Validation =
        VexLifePromptPreparation.Validation(
            runtime = runtime,
            leadingMessage = leadingMessage,
            foundationalFieldRef =
                prefix.foundationalField
                    ?.fieldRef,
            foundationalFieldVersion =
                prefix.foundationalField
                    ?.fieldVersion,
            foundationalBodySha256 =
                prefix.foundationalField
                    ?.bodySha256,
            contextProfileRef =
                prefix.contextProfileRef,
            contextProfileFingerprint =
                prefix.contextProfileFingerprint,
            memoryContextRef =
                prefix.memoryContextRef,
            memoryContextFingerprint =
                memoryFingerprint,
            capabilityRequestsAdmitted =
                prefix.capabilityRequestsAdmitted,
            providerNativeTools =
                prefix.providerNativeTools,
        )

    private fun performed(
        runtime: HereticModelConfiguration.QualifiedRuntimeEvidence,
        prefix: DirectPromptContext.InitialBootPrefix,
    ): DirectCompanionConnection.SystemPrefixPrefillResult.Performed =
        DirectCompanionConnection.SystemPrefixPrefillResult.Performed(
            runtimeReceiptRef = runtime.runtimeReceiptRef,
            modelRef = runtime.modelRef,
            systemMessageSha256 = prefix.systemMessageSha256,
            requestBodySha256 = "a".repeat(
                64,
            ),
            promptTokens = null,
            cachedPromptTokens = null,
            completionTokens = null,
            totalMillis = 25L,
        )

    @Test
    fun watchdogSubmissionFailureCancelsAdmittedWorkerBeforeHumanColdFallback() {
        val runtime =
            runtime()
        val prefix =
            prefix()
        val firstSubmission =
            AtomicBoolean(
                true,
            )
        val executor =
            object :
                AbstractExecutorService() {
                private val shutdown =
                    AtomicBoolean(
                        false,
                    )

                override fun execute(
                    command: Runnable,
                ) {
                    if (
                        !firstSubmission.compareAndSet(
                            true,
                            false,
                        )
                    ) {
                        throw RejectedExecutionException(
                            "synthetic watchdog rejection",
                        )
                    }
                    Thread(
                        command,
                        "prompt-preparation-worker-test",
                    )
                        .start()
                }

                override fun shutdown() {
                    shutdown.set(
                        true,
                    )
                }

                override fun shutdownNow(): MutableList<Runnable> {
                    shutdown.set(
                        true,
                    )
                    return mutableListOf()
                }

                override fun isShutdown(): Boolean =
                    shutdown.get()

                override fun isTerminated(): Boolean =
                    shutdown.get()

                override fun awaitTermination(
                    timeout: Long,
                    unit: TimeUnit,
                ): Boolean =
                    true
            }

        try {
            val preparation =
                VexLifePromptPreparation(
                    providerPrefill =
                        VexLifePromptPreparation.ProviderPrefill {
                            _,
                            _,
                            cancellation,
                            ->
                            while (
                                true
                            ) {
                                try {
                                    Thread.sleep(
                                        25L,
                                    )
                                } catch (
                                    _: InterruptedException,
                                ) {
                                    cancellation.checkpoint()
                                }
                                cancellation.checkpoint()
                            }
                            @Suppress(
                                "UNREACHABLE_CODE",
                            )
                            DirectCompanionConnection.SystemPrefixPrefillResult.Held(
                                "UNREACHABLE",
                            )
                        },
                    humanTurnWaitMillis =
                        1_000L,
                    cancellationDrainMillis =
                        1_000L,
                )
            preparation.start(
                executor = executor,
                runtime = runtime,
                prefix = prefix,
            )

            val decision =
                preparation.awaitAndConsume(
                    validation(
                        runtime = runtime,
                        prefix = prefix,
                    ),
                )

            assertEquals(
                "WATCHDOG_START_FAILED",
                assertIs<VexLifePromptPreparation.Decision.Cold>(
                        decision,
                    )
                    .reasonRef,
            )
            assertIs<VexLifePromptPreparation.State.Invalidated>(
                preparation.state(),
            )
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun exactPreparedPrefixIsConsumedOnce() {
        val runtime =
            runtime()
        val prefix =
            prefix()
        val calls =
            AtomicInteger(
                0,
            )
        val executor =
            Executors.newCachedThreadPool()

        try {
            val preparation =
                VexLifePromptPreparation(
                    providerPrefill =
                        VexLifePromptPreparation.ProviderPrefill {
                            current,
                            _,
                            _,
                            ->
                            calls.incrementAndGet()
                            performed(
                                runtime = current,
                                prefix = prefix,
                            )
                        },
                )
            preparation.start(
                executor = executor,
                runtime = runtime,
                prefix = prefix,
            )

            assertTrue(
                preparation.firstLocalTurnPending(),
                "Cold/Preparing/Ready preparation still belongs to the one first local turn",
            )

            val first =
                preparation.awaitAndConsume(
                    validation(
                        runtime = runtime,
                        prefix = prefix,
                    ),
                )
            assertIs<VexLifePromptPreparation.Decision.Reusable>(
                first,
            )
            assertEquals(
                1,
                calls.get(),
            )
            assertIs<VexLifePromptPreparation.State.Consumed>(
                preparation.state(),
            )
            assertFalse(
                preparation.firstLocalTurnPending(),
                "Consumed preparation must not label later turns as first-local-start work",
            )

            val second =
                preparation.awaitAndConsume(
                    validation(
                        runtime = runtime,
                        prefix = prefix,
                    ),
                )
            assertFalse(
                second.reusable,
            )
            assertEquals(
                "PREPARATION_ALREADY_CONSUMED",
                assertIs<VexLifePromptPreparation.Decision.Cold>(
                        second,
                    )
                    .reasonRef,
            )
        } finally {
            executor.shutdownNow()
            executor.awaitTermination(
                5,
                TimeUnit.SECONDS,
            )
        }
    }

    @Test
    fun providerPrefixDriftInvalidatesAndFallsBackCold() {
        val runtime =
            runtime()
        val prefix =
            prefix()
        val executor =
            Executors.newCachedThreadPool()

        try {
            val preparation =
                VexLifePromptPreparation(
                    providerPrefill =
                        VexLifePromptPreparation.ProviderPrefill {
                            current,
                            _,
                            _,
                            ->
                            performed(
                                runtime = current,
                                prefix = prefix,
                            )
                        },
                )
            preparation.start(
                executor = executor,
                runtime = runtime,
                prefix = prefix,
            )

            val decision =
                preparation.awaitAndConsume(
                    validation(
                        runtime = runtime,
                        prefix = prefix,
                        leadingMessage =
                            CompanionProviderMessage(
                                role = "system",
                                content =
                                    prefix.messages.single().content +
                                        "\nchanged after startup",
                            ),
                    ),
                )
            assertEquals(
                "PROVIDER_PREFIX_CHANGED",
                assertIs<VexLifePromptPreparation.Decision.Cold>(
                        decision,
                    )
                    .reasonRef,
            )
            assertIs<VexLifePromptPreparation.State.Invalidated>(
                preparation.state(),
            )
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun memoryCurrentnessDriftInvalidatesEvenWhenProviderBytesMatch() {
        val runtime =
            runtime()
        val prefix =
            prefix()
        val executor =
            Executors.newCachedThreadPool()

        try {
            val preparation =
                VexLifePromptPreparation(
                    providerPrefill =
                        VexLifePromptPreparation.ProviderPrefill {
                            current,
                            _,
                            _,
                            ->
                            performed(
                                runtime = current,
                                prefix = prefix,
                            )
                        },
                )
            preparation.start(
                executor = executor,
                runtime = runtime,
                prefix = prefix,
            )

            val decision =
                preparation.awaitAndConsume(
                    validation(
                        runtime = runtime,
                        prefix = prefix,
                        memoryFingerprint =
                            "b".repeat(
                                64,
                            ),
                    ),
                )
            assertEquals(
                "MEMORY_CHANGED",
                assertIs<VexLifePromptPreparation.Decision.Cold>(
                        decision,
                    )
                    .reasonRef,
            )
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun runtimeReceiptDriftInvalidatesPreparedCache() {
        val runtime =
            runtime()
        val prefix =
            prefix()
        val executor =
            Executors.newCachedThreadPool()

        try {
            val preparation =
                VexLifePromptPreparation(
                    providerPrefill =
                        VexLifePromptPreparation.ProviderPrefill {
                            current,
                            _,
                            _,
                            ->
                            performed(
                                runtime = current,
                                prefix = prefix,
                            )
                        },
                )
            preparation.start(
                executor = executor,
                runtime = runtime,
                prefix = prefix,
            )

            val decision =
                preparation.awaitAndConsume(
                    validation(
                        runtime =
                            runtime(
                                "receipt.vexlife-prod.prompt-preparation.changed",
                            ),
                        prefix = prefix,
                    ),
                )
            assertEquals(
                "RUNTIME_RECEIPT_CHANGED",
                assertIs<VexLifePromptPreparation.Decision.Cold>(
                        decision,
                    )
                    .reasonRef,
            )
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun providerHoldNeverClaimsReusableReadiness() {
        val runtime =
            runtime()
        val prefix =
            prefix()
        val executor =
            Executors.newCachedThreadPool()

        try {
            val preparation =
                VexLifePromptPreparation(
                    providerPrefill =
                        VexLifePromptPreparation.ProviderPrefill {
                            _,
                            _,
                            _,
                            ->
                            DirectCompanionConnection.SystemPrefixPrefillResult.Held(
                                "PROVIDER_PREFIX_REPRESENTATION_NOT_ADMITTED",
                            )
                        },
                )
            preparation.start(
                executor = executor,
                runtime = runtime,
                prefix = prefix,
            )
            val decision =
                preparation.awaitAndConsume(
                    validation(
                        runtime = runtime,
                        prefix = prefix,
                    ),
                )

            assertEquals(
                "PROVIDER_PREFIX_REPRESENTATION_NOT_ADMITTED",
                assertIs<VexLifePromptPreparation.Decision.Cold>(
                        decision,
                    )
                    .reasonRef,
            )
            assertIs<VexLifePromptPreparation.State.Held>(
                preparation.state(),
            )
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun humanTurnTimeoutCancelsPreparationBeforeColdFallback() {
        val runtime =
            runtime()
        val prefix =
            prefix()
        val executor =
            Executors.newCachedThreadPool()

        try {
            val preparation =
                VexLifePromptPreparation(
                    providerPrefill =
                        VexLifePromptPreparation.ProviderPrefill {
                            _,
                            _,
                            cancellation,
                            ->
                            while (
                                true
                            ) {
                                try {
                                    Thread.sleep(
                                        25L,
                                    )
                                } catch (
                                    _: InterruptedException,
                                ) {
                                    cancellation.checkpoint()
                                }
                                cancellation.checkpoint()
                            }
                            @Suppress(
                                "UNREACHABLE_CODE",
                            )
                            DirectCompanionConnection.SystemPrefixPrefillResult.Held(
                                "UNREACHABLE",
                            )
                        },
                    maximumPreparationMillis =
                        5_000L,
                    humanTurnWaitMillis =
                        25L,
                    cancellationDrainMillis =
                        1_000L,
                )
            preparation.start(
                executor = executor,
                runtime = runtime,
                prefix = prefix,
            )

            val decision =
                preparation.awaitAndConsume(
                    validation(
                        runtime = runtime,
                        prefix = prefix,
                    ),
                )
            assertEquals(
                "HUMAN_TURN_WAIT_TIMEOUT",
                assertIs<VexLifePromptPreparation.Decision.Cold>(
                        decision,
                    )
                    .reasonRef,
            )
            assertTrue(
                preparation.state() is
                    VexLifePromptPreparation.State.Invalidated,
            )
        } finally {
            executor.shutdownNow()
        }
    }
}

// [VXG RealForever]
