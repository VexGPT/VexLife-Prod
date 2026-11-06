package vexlife.companion

import java.nio.file.Files
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vexlife.browser.VexLifeProdBrowserContract
import vexlife.continuity.VexLifeWorkingContinuityOwner
import vexlife.conversation.DirectConversationStore
import vexlife.home.CanonicalVexLifeHomeFixture
import vexlife.local.VexLifeProdLocalSetup
import vexlife.memory.VexLifeMemoryContextFixture
import vexlife.memory.VexLifeMemoryContract
import vexlife.model.heretic.HereticModelConfiguration
import vexlife.model.heretic.HereticProductIdentity
import vexlife.model.heretic.HereticRuntimeQualification
import vexlife.model.heretic.HereticRuntimeState

class DirectCompanionTurnTest {
    private fun productHome(): java.nio.file.Path {
        val parent =
            Files.createTempDirectory(
                "vexlife-prod-direct-turn-",
            )
                .toAbsolutePath()
        val home =
            parent.resolve(
                "VexLife-Prod",
            )
        CanonicalVexLifeHomeFixture.form(
            home = home,
            deviceSuffix = "turn-device",
            lineageSuffix = "turn-lineage",
        )
        return home
    }

    private fun setup(
        home: java.nio.file.Path,
    ): VexLifeProdLocalSetup.Result =
        VexLifeProdLocalSetup.Result(
            state = "LOCAL_HERETIC_FOUNDATION_READY",
            productHome = home.toString(),
            homeDisposition = "EXISTING_HOME_PRESERVED",
            modelSelectionDisposition = "BOUND",
            managedPythonDisposition = "REUSED_VERIFIED",
            artifactDisposition = "REUSED_VERIFIED",
            environmentDisposition = "REUSED_VERIFIED",
            runtimeDisposition = "REUSED_EXACT_OWNED_MLX_RUNTIME",
            modelBindingDisposition = "BOUND_CURRENT",
            modelConfigurationState = HereticModelConfiguration.State.BoundProductModel,
            qualifiedRuntimeEvidence =
                HereticModelConfiguration.QualifiedRuntimeEvidence(
                    runtimeReceiptRef = "receipt.vexlife-prod.heretic.turn",
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
                ),
            endpoint = HereticProductIdentity.Endpoint,
            requestModel = HereticProductIdentity.RequestModel,
        )

    @Test
    fun twoTurnsUseImmediatePriorContextAndSecondSendRemainsCanonical() {
        val home =
            productHome()
        var calls =
            0
        val transport =
            DirectCompanionConnection.Transport {
                call ->
                calls +=
                    1
                val content =
                    if (
                        calls ==
                            1
                    ) {
                        "First response"
                    } else {
                        assertTrue(
                            call.body.contains(
                                "First request",
                            ),
                        )
                        assertTrue(
                            call.body.contains(
                                "First response",
                            ),
                        )
                        assertTrue(
                            call.body.contains(
                                "Second request",
                            ),
                        )
                        "Second response"
                    }

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
                                "content": "$content"
                              }
                            }
                          ]
                        }
                        """.trimIndent(),
                )
            }

        try {
            val first =
                DirectCompanionTurn.performWithHooks(
                    setup = setup(
                        home,
                    ),
                    intent =
                        DirectCompanionTurn.Intent(
                            idempotencyKey = "send.self-development.001",
                            content = "First request",
                        ),
                    hooks =
                        DirectCompanionTurn.Hooks(
                            transport = transport,
                            now = {
                                Instant.parse(
                                    "2026-11-05T05:01:00Z",
                                )
                            },
                        ),
                )
            val second =
                DirectCompanionTurn.performWithHooks(
                    setup = setup(
                        home,
                    ),
                    intent =
                        DirectCompanionTurn.Intent(
                            idempotencyKey = "send.self-development.002",
                            content = "Second request",
                        ),
                    hooks =
                        DirectCompanionTurn.Hooks(
                            transport = transport,
                            now = {
                                Instant.parse(
                                    "2026-11-05T05:02:00Z",
                                )
                            },
                        ),
                )

            assertEquals(
                2,
                calls,
            )
            assertEquals(
                "First response",
                first.content,
            )
            assertEquals(
                "Second response",
                second.content,
            )
            assertEquals(
                "IMMEDIATE_PRIOR_COMPLETED_TURN",
                second.promptContextReceipt.contextMode,
            )
            assertEquals(
                2,
                second.promptContextReceipt.selectedConversationEventRefs.size,
            )
            assertTrue(
                first.currentConversationHeadSha256 !=
                    second.currentConversationHeadSha256,
            )
        } finally {
            home.parent
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun successfulHighPressureTurnRollsIntoWorkingCheckpointSuccessorOnNextRealTurn() {
        val home =
            productHome()
        val memory =
            VexLifeMemoryContextFixture.current()
        val requestTokenTarget =
            DirectPromptContext.InputTokenBudget -
                DirectPromptContext.ReservedOutputTokens +
                100 -
                memory.estimatedSystemMessageTokens
        val requestContent =
            "x".repeat(
                (
                    requestTokenTarget -
                        4
                ) *
                    4,
            )
        var providerCalls =
            0
        val transport =
            DirectCompanionConnection.Transport {
                call ->
                providerCalls +=
                    1
                when (
                    providerCalls
                ) {
                    1 ->
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
                                        "content": "Rollover producer response"
                                      }
                                    }
                                  ]
                                }
                                """.trimIndent(),
                        )

                    else -> {
                        assertTrue(
                            call.body.contains(
                                "VEXLIFE_CURRENT_MEMORY_CONTEXT/v1",
                            ),
                        )
                        assertTrue(
                            call.body.contains(
                                "VEXLIFE_WORKING_CONTINUITY_CONTEXT/v1",
                            ),
                        )
                        assertTrue(
                            call.body.contains(
                                "Successor request after rollover",
                            ),
                        )
                        assertTrue(
                            call.body.contains(
                                "statement.score.continuity.001",
                            ),
                        )
                        assertFalse(
                            call.body.contains(
                                "Rollover producer response",
                            ),
                        )
                        assertFalse(
                            call.body.contains(
                                "x".repeat(
                                    1024,
                                ),
                            ),
                        )

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
                                        "content": "Successor response without prior-pair replay"
                                      }
                                    }
                                  ]
                                }
                                """.trimIndent(),
                        )
                    }
                }
            }

        try {
            val first =
                DirectCompanionTurn.performWithHooks(
                    setup =
                        setup(
                            home,
                        ),
                    intent =
                        DirectCompanionTurn.Intent(
                            idempotencyKey =
                                "send.self-development.rollover-producer",
                            content =
                                requestContent,
                        ),
                    hooks =
                        DirectCompanionTurn.Hooks(
                            transport = transport,
                            currentMemoryContext = {
                                memory
                            },
                            now = {
                                Instant.parse(
                                    "2026-11-06T11:10:00Z",
                                )
                            },
                        ),
                )

            assertEquals(
                1,
                providerCalls,
            )
            assertEquals(
                "TURN_COMPLETED",
                first.state,
            )

            val continuity =
                assertNotNull(
                    DirectWorkingContinuity.read(
                        productHome = home,
                        turnRef = first.turnRef,
                    ),
                )
            assertEquals(
                "ROLLOVER_COMMITTED",
                continuity.disposition,
            )
            assertTrue(
                continuity.effects.postCommitReceiptPersisted,
            )
            assertTrue(
                continuity.effects.workingCheckpointPersisted,
            )
            assertTrue(
                continuity.effects.workingHeadMutated,
            )

            val conversationAfterFirst =
                DirectConversationStore(
                    home,
                )
                    .current()
            val firstHead =
                assertNotNull(
                    conversationAfterFirst.head,
                )
            val owner =
                VexLifeWorkingContinuityOwner(
                    home,
                )
            val ownerCurrent =
                assertNotNull(
                    owner.readCurrent(
                        lineageRef =
                            firstHead.companionLineageRef,
                        threadRef =
                            firstHead.threadRef,
                    ),
                )
            assertEquals(
                "ROLLOVER_COMMITTED",
                ownerCurrent.commitReceipt.disposition,
            )
            assertTrue(
                ownerCurrent.commitReceipt.successorContextRequired,
            )
            assertEquals(
                first.requestEventRef,
                ownerCurrent.checkpoint.workingSet.cursorEventRef,
            )
            assertTrue(
                ownerCurrent.checkpoint.workingSet.currentStatementRefs.contains(
                    "statement.score.continuity.001",
                ),
            )

            val second =
                DirectCompanionTurn.performWithHooks(
                    setup =
                        setup(
                            home,
                        ),
                    intent =
                        DirectCompanionTurn.Intent(
                            idempotencyKey =
                                "send.self-development.rollover-successor",
                            content =
                                "Successor request after rollover",
                        ),
                    hooks =
                        DirectCompanionTurn.Hooks(
                            transport = transport,
                            currentMemoryContext = {
                                memory
                            },
                            now = {
                                Instant.parse(
                                    "2026-11-06T11:11:00Z",
                                )
                            },
                        ),
                )

            assertEquals(
                2,
                providerCalls,
            )
            assertEquals(
                "Successor response without prior-pair replay",
                second.content,
            )
            assertEquals(
                "WORKING_CHECKPOINT_SUCCESSOR",
                second.promptContextReceipt.contextMode,
            )
            assertTrue(
                second.promptContextReceipt.selectedConversationEventRefs.isEmpty(),
            )
            assertTrue(
                second.promptContextReceipt.selectedConversationEventSha256.isEmpty(),
            )
            assertTrue(
                second.promptContextReceipt.workingContinuityIncluded,
            )
            assertNotNull(
                second.promptContextReceipt.workingContinuityContextRef,
            )
            assertNotNull(
                second.promptContextReceipt.workingContinuityContextFingerprint,
            )
            assertNotNull(
                second.promptContextReceipt.workingContinuityCheckpointRef,
            )
            assertNotNull(
                second.promptContextReceipt.workingContinuitySuccessorClaimRef,
            )

            val secondContext =
                assertNotNull(
                    DirectTurnContextStore(
                        home,
                    )
                        .read(
                            second.turnRef,
                        ),
                )
            assertTrue(
                secondContext.contextSourceRefs.contains(
                    requireNotNull(
                        second.promptContextReceipt.workingContinuityContextRef,
                    ),
                ),
            )
            assertTrue(
                secondContext.contextSourceRefs.contains(
                    requireNotNull(
                        second.promptContextReceipt.workingContinuityCheckpointRef,
                    ),
                ),
            )
            assertTrue(
                secondContext.contextSourceRefs.contains(
                    requireNotNull(
                        second.promptContextReceipt.workingContinuitySuccessorClaimRef,
                    ),
                ),
            )
        } finally {
            home.parent
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun failedWorkingSuccessorDoesNotBlockFreshRequestScopedSuccessorAttempt() {
        val home =
            productHome()
        val memory =
            VexLifeMemoryContextFixture.current()
        val requestTokenTarget =
            DirectPromptContext.InputTokenBudget -
                DirectPromptContext.ReservedOutputTokens +
                100 -
                memory.estimatedSystemMessageTokens
        val highPressureRequest =
            "y".repeat(
                (
                    requestTokenTarget -
                        4
                ) *
                    4,
            )
        var calls =
            0
        val transport =
            DirectCompanionConnection.Transport {
                call ->
                calls +=
                    1
                when (
                    calls
                ) {
                    1 ->
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
                                        "content": "Rollover recovery baseline"
                                      }
                                    }
                                  ]
                                }
                                """.trimIndent(),
                        )

                    2 -> {
                        assertTrue(
                            call.body.contains(
                                "VEXLIFE_WORKING_CONTINUITY_CONTEXT/v1",
                            ),
                        )
                        assertTrue(
                            call.body.contains(
                                "Failed successor request",
                            ),
                        )
                        DirectCompanionConnection.HttpResult(
                            status = 503,
                            body = "",
                        )
                    }

                    else -> {
                        assertTrue(
                            call.body.contains(
                                "VEXLIFE_WORKING_CONTINUITY_CONTEXT/v1",
                            ),
                        )
                        assertTrue(
                            call.body.contains(
                                "Fresh successor recovery request",
                            ),
                        )
                        assertFalse(
                            call.body.contains(
                                "Failed successor request",
                            ),
                        )
                        assertFalse(
                            call.body.contains(
                                "Rollover recovery baseline",
                            ),
                        )
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
                                        "content": "Fresh request-scoped successor recovered"
                                      }
                                    }
                                  ]
                                }
                                """.trimIndent(),
                        )
                    }
                }
            }

        try {
            val first =
                DirectCompanionTurn.performWithHooks(
                    setup =
                        setup(
                            home,
                        ),
                    intent =
                        DirectCompanionTurn.Intent(
                            idempotencyKey =
                                "send.self-development.rollover-recovery-base",
                            content =
                                highPressureRequest,
                        ),
                    hooks =
                        DirectCompanionTurn.Hooks(
                            transport = transport,
                            currentMemoryContext = {
                                memory
                            },
                        ),
                )

            val firstHead =
                assertNotNull(
                    DirectConversationStore(
                        home,
                    )
                        .current()
                        .head,
                )
            val owner =
                VexLifeWorkingContinuityOwner(
                    home,
                )
            assertEquals(
                "ROLLOVER_COMMITTED",
                assertNotNull(
                    owner.readCurrent(
                        lineageRef =
                            firstHead.companionLineageRef,
                        threadRef =
                            firstHead.threadRef,
                    ),
                )
                    .commitReceipt
                    .disposition,
            )

            assertFailsWith<DirectCompanionTurn.TurnException> {
                DirectCompanionTurn.performWithHooks(
                    setup =
                        setup(
                            home,
                        ),
                    intent =
                        DirectCompanionTurn.Intent(
                            idempotencyKey =
                                "send.self-development.rollover-recovery-failed",
                            content =
                                "Failed successor request",
                        ),
                    hooks =
                        DirectCompanionTurn.Hooks(
                            transport = transport,
                            currentMemoryContext = {
                                memory
                            },
                        ),
                )
            }

            val afterFailure =
                DirectConversationStore(
                    home,
                )
                    .current()
            assertEquals(
                first.currentConversationHeadSha256,
                afterFailure.head
                    ?.conversationHeadSha256,
            )
            val failedRequest =
                afterFailure.abandonedAttempts
                    .single()
                    .requestEvent
            val failedBinding =
                VexLifeMemoryContract.CurrentRequestBinding(
                    eventRef =
                        failedRequest.eventRef,
                    eventHash =
                        failedRequest.eventHash,
                    sequence =
                        failedRequest.sequence,
                )
            val failedContext =
                assertNotNull(
                    owner.readSuccessorContext(
                        lineageRef =
                            firstHead.companionLineageRef,
                        threadRef =
                            firstHead.threadRef,
                        currentRequestEventBinding =
                            failedBinding,
                    ),
                )

            val recovered =
                DirectCompanionTurn.performWithHooks(
                    setup =
                        setup(
                            home,
                        ),
                    intent =
                        DirectCompanionTurn.Intent(
                            idempotencyKey =
                                "send.self-development.rollover-recovery-fresh",
                            content =
                                "Fresh successor recovery request",
                        ),
                    hooks =
                        DirectCompanionTurn.Hooks(
                            transport = transport,
                            currentMemoryContext = {
                                memory
                            },
                        ),
                )

            assertEquals(
                3,
                calls,
            )
            assertEquals(
                "Fresh request-scoped successor recovered",
                recovered.content,
            )
            assertEquals(
                "WORKING_CHECKPOINT_SUCCESSOR",
                recovered.promptContextReceipt.contextMode,
            )
            assertTrue(
                recovered.promptContextReceipt.selectedConversationEventRefs.isEmpty(),
            )

            val afterRecovery =
                DirectConversationStore(
                    home,
                )
                    .current()
            val recoveredRequest =
                afterRecovery.canonicalEvents
                    .single {
                        it.eventRef ==
                            recovered.requestEventRef
                    }
            val recoveredBinding =
                VexLifeMemoryContract.CurrentRequestBinding(
                    eventRef =
                        recoveredRequest.eventRef,
                    eventHash =
                        recoveredRequest.eventHash,
                    sequence =
                        recoveredRequest.sequence,
                )
            val recoveredContext =
                assertNotNull(
                    owner.readSuccessorContext(
                        lineageRef =
                            firstHead.companionLineageRef,
                        threadRef =
                            firstHead.threadRef,
                        currentRequestEventBinding =
                            recoveredBinding,
                    ),
                )

            assertTrue(
                failedContext.successorContextRef !=
                    recoveredContext.successorContextRef,
            )
            assertEquals(
                failedRequest.eventRef,
                failedContext.currentRequestEventBinding.eventRef,
            )
            assertEquals(
                recovered.requestEventRef,
                recoveredContext.currentRequestEventBinding.eventRef,
            )
        } finally {
            home.parent
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun postCompletionContinuityExceptionCannotRetroactivelyFailCompletedTurn() {
        val home =
            productHome()
        var providerCalls =
            0

        try {
            val result =
                DirectCompanionTurn.performWithHooks(
                    setup =
                        setup(
                            home,
                        ),
                    intent =
                        DirectCompanionTurn.Intent(
                            idempotencyKey =
                                "send.self-development.continuity-post-commit-isolation",
                            content =
                                "Complete this turn even if post-completion continuity breaks.",
                        ),
                    hooks =
                        DirectCompanionTurn.Hooks(
                            transport =
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
                                                    "content": "Completed before continuity stewardship."
                                                  }
                                                }
                                              ]
                                            }
                                            """.trimIndent(),
                                    )
                                },
                            commitWorkingContinuity = {
                                _,
                                _,
                                _,
                                _,
                                ->
                                error(
                                    "synthetic post-completion continuity failure",
                                )
                            },
                        ),
                )

            assertEquals(
                1,
                providerCalls,
            )
            assertEquals(
                "TURN_COMPLETED",
                result.state,
            )
            assertEquals(
                "Completed before continuity stewardship.",
                result.content,
            )

            val current =
                DirectConversationStore(
                    home,
                )
                    .current()
            assertEquals(
                result.currentConversationHeadSha256,
                current.head
                    ?.conversationHeadSha256,
            )
            assertEquals(
                2,
                current.canonicalEvents.size,
            )
            assertTrue(
                current.abandonedAttempts.isEmpty(),
            )

            assertFalse(
                Files.exists(
                    home
                        .resolve(
                            "recovery/direct-companion",
                        )
                        .resolve(
                            result.turnRef +
                                ".json",
                        ),
                ),
            )
            assertNull(
                DirectWorkingContinuity.read(
                    productHome = home,
                    turnRef = result.turnRef,
                ),
            )
        } finally {
            home.parent
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun exactCompletedSendRetryReturnsDurableResultWithoutAnotherModelCall() {
        val home =
            productHome()
        var calls =
            0
        var runtimeObservations =
            0
        val transport =
            DirectCompanionConnection.Transport {
                calls +=
                    1
                DirectCompanionConnection.HttpResult(
                    status = 200,
                    body =
                        """
                        {
                          "choices": [
                            {
                              "message": {
                                "role": "assistant",
                                "content": "Canonical response"
                              }
                            }
                          ]
                        }
                        """.trimIndent(),
                )
            }
        val intent =
            DirectCompanionTurn.Intent(
                idempotencyKey = "send.self-development.retry",
                content = "One request",
            )

        try {
            val first =
                DirectCompanionTurn.performWithHooks(
                    setup = setup(
                        home,
                    ),
                    intent = intent,
                    hooks =
                        DirectCompanionTurn.Hooks(
                            transport = transport,
                            currentRuntimeEvidence = {
                                currentSetup ->
                                runtimeObservations +=
                                    1
                                currentSetup.qualifiedRuntimeEvidence
                            },
                        ),
                )
            val recovered =
                DirectCompanionTurn.performWithHooks(
                    setup = setup(
                        home,
                    ),
                    intent = intent,
                    hooks =
                        DirectCompanionTurn.Hooks(
                            transport = transport,
                            currentRuntimeEvidence = {
                                currentSetup ->
                                runtimeObservations +=
                                    1
                                currentSetup.qualifiedRuntimeEvidence
                            },
                        ),
                )

            assertEquals(
                1,
                calls,
            )
            assertEquals(
                1,
                runtimeObservations,
                "exact durable duplicate must not require current runtime observation",
            )
            assertFalse(
                first.recoveredCompletedTurn,
            )
            assertTrue(
                recovered.recoveredCompletedTurn,
            )
            assertFalse(
                recovered.modelCallPerformed,
            )
            assertFalse(
                recovered.actualHttpCallPerformedNow,
            )
            assertEquals(
                first.content,
                recovered.content,
            )
        } finally {
            home.parent
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun newSendRequiresFreshRuntimeCurrentnessBeforeProviderEffect() {
        val home =
            productHome()
        var providerCalls =
            0
        var runtimeObservations =
            0

        try {
            val failed =
                assertFailsWith<DirectCompanionTurn.TurnException> {
                    DirectCompanionTurn.performWithHooks(
                        setup = setup(
                            home,
                        ),
                        intent =
                            DirectCompanionTurn.Intent(
                                idempotencyKey = "send.self-development.runtime-held",
                                content = "Do not call the provider",
                            ),
                        hooks =
                            DirectCompanionTurn.Hooks(
                                transport =
                                    DirectCompanionConnection.Transport {
                                        providerCalls +=
                                            1
                                        error(
                                            "provider must not run without current runtime evidence",
                                        )
                                    },
                                currentRuntimeEvidence = {
                                    _ ->
                                    runtimeObservations +=
                                        1
                                    error(
                                        "runtime currentness held",
                                    )
                                },
                            ),
                    )
                }

            assertEquals(
                1,
                runtimeObservations,
            )
            assertEquals(
                0,
                providerCalls,
            )
            assertFalse(
                failed.retrySameSendAttemptAllowed,
            )

            val current =
                DirectConversationStore(
                    home,
                )
                    .current()
            assertEquals(
                null,
                current.head,
            )
            assertEquals(
                1,
                current.abandonedAttempts.size,
            )
        } finally {
            home.parent
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun providerFailurePreservesRequestSuppressesSameAttemptAndAllowsNewAttempt() {
        val home =
            productHome()
        var calls =
            0
        val failedIntent =
            DirectCompanionTurn.Intent(
                idempotencyKey = "send.self-development.failure",
                content = "Please answer",
            )

        try {
            val error =
                assertFailsWith<DirectCompanionTurn.TurnException> {
                    DirectCompanionTurn.performWithHooks(
                        setup = setup(
                            home,
                        ),
                        intent = failedIntent,
                        hooks =
                            DirectCompanionTurn.Hooks(
                                transport =
                                    DirectCompanionConnection.Transport {
                                        calls +=
                                            1
                                        DirectCompanionConnection.HttpResult(
                                            status = 503,
                                            body = "",
                                        )
                                    },
                            ),
                    )
                }

            val receipt =
                assertNotNull(
                    error.failureReceipt,
                )
            assertEquals(
                "CONNECTION_EndpointHttpError",
                receipt.failureCode,
            )
            assertTrue(
                receipt.requestDurablyRecorded,
            )
            assertFalse(
                receipt.responseDurablyRecorded,
            )
            assertFalse(
                receipt.currentHeadCommitted,
            )
            assertFalse(
                receipt.retrySameSendAttemptAllowed,
            )
            assertEquals(
                "FORM_NEW_SEND_ATTEMPT_AND_RETRY",
                receipt.exactNextSafeRoute,
            )

            val failedState =
                DirectConversationStore(
                    home,
                )
                    .current()
            assertNull(
                failedState.head,
            )
            assertEquals(
                1,
                failedState.abandonedAttempts.size,
            )

            val sameAttempt =
                assertFailsWith<DirectCompanionTurn.TurnException> {
                    DirectCompanionTurn.performWithHooks(
                        setup = setup(
                            home,
                        ),
                        intent = failedIntent,
                        hooks =
                            DirectCompanionTurn.Hooks(
                                transport =
                                    DirectCompanionConnection.Transport {
                                        calls +=
                                            1
                                        error(
                                            "same failed send must not reach the provider",
                                        )
                                    },
                            ),
                    )
                }

            assertEquals(
                "FAILED_SEND_ATTEMPT_REQUIRES_NEW_SEND_ATTEMPT",
                sameAttempt.failureCode,
            )
            assertFalse(
                sameAttempt.retrySameSendAttemptAllowed,
            )
            assertEquals(
                "FORM_NEW_SEND_ATTEMPT_AND_RETRY",
                sameAttempt.exactNextSafeRoute,
            )
            assertEquals(
                1,
                calls,
            )

            val recovered =
                DirectCompanionTurn.performWithHooks(
                    setup = setup(
                        home,
                    ),
                    intent =
                        DirectCompanionTurn.Intent(
                            idempotencyKey = "send.self-development.recovery-new",
                            content = "Please answer again",
                        ),
                    hooks =
                        DirectCompanionTurn.Hooks(
                            transport =
                                DirectCompanionConnection.Transport {
                                    calls +=
                                        1
                                    DirectCompanionConnection.HttpResult(
                                        status = 200,
                                        body =
                                            """
                                            {
                                              "choices": [
                                                {
                                                  "message": {
                                                    "role": "assistant",
                                                    "content": "Recovered on a new turn."
                                                  }
                                                }
                                              ]
                                            }
                                            """.trimIndent(),
                                    )
                                },
                        ),
                )

            assertEquals(
                "Recovered on a new turn.",
                recovered.content,
            )
            assertEquals(
                2,
                calls,
            )

            val current =
                DirectConversationStore(
                    home,
                )
                    .current()
            assertNotNull(
                current.head,
            )
            assertEquals(
                2,
                current.canonicalEvents.size,
            )
            assertEquals(
                1,
                current.abandonedAttempts.size,
            )
            assertEquals(
                "Please answer",
                current.abandonedAttempts
                    .single()
                    .requestEvent
                    .content,
            )
        } finally {
            home.parent
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun failedFollowUpResumesLastValidHeadOnlyWithANewSendAttempt() {
        val home =
            productHome()
        val setup =
            setup(
                home,
            )
        var calls =
            0
        val transport =
            DirectCompanionConnection.Transport {
                call ->
                calls +=
                    1

                when (
                    calls
                ) {
                    1 ->
                        DirectCompanionConnection.HttpResult(
                            status = 200,
                            body =
                                """
                                {
                                  "choices": [
                                    {
                                      "message": {
                                        "role": "assistant",
                                        "content": "Canonical first response"
                                      }
                                    }
                                  ]
                                }
                                """.trimIndent(),
                        )

                    2 ->
                        DirectCompanionConnection.HttpResult(
                            status = 503,
                            body = "",
                        )

                    else -> {
                        assertTrue(
                            call.body.contains(
                                "Canonical first request",
                            ),
                        )
                        assertTrue(
                            call.body.contains(
                                "Canonical first response",
                            ),
                        )
                        assertTrue(
                            call.body.contains(
                                "Fresh recovery request",
                            ),
                        )
                        assertFalse(
                            call.body.contains(
                                "Failed follow-up request",
                            ),
                        )

                        DirectCompanionConnection.HttpResult(
                            status = 200,
                            body =
                                """
                                {
                                  "choices": [
                                    {
                                      "message": {
                                        "role": "assistant",
                                        "content": "Recovery continued from the last valid head"
                                      }
                                    }
                                  ]
                                }
                                """.trimIndent(),
                        )
                    }
                }
            }

        try {
            val first =
                DirectCompanionTurn.performWithHooks(
                    setup = setup,
                    intent =
                        DirectCompanionTurn.Intent(
                            idempotencyKey = "send.self-development.canonical-first",
                            content = "Canonical first request",
                        ),
                    hooks =
                        DirectCompanionTurn.Hooks(
                            transport = transport,
                        ),
                )

            val failed =
                assertFailsWith<DirectCompanionTurn.TurnException> {
                    DirectCompanionTurn.performWithHooks(
                        setup = setup,
                        intent =
                            DirectCompanionTurn.Intent(
                                idempotencyKey = "send.self-development.failed-followup",
                                content = "Failed follow-up request",
                            ),
                        hooks =
                            DirectCompanionTurn.Hooks(
                                transport = transport,
                            ),
                    )
                }

            val failure =
                assertNotNull(
                    failed.failureReceipt,
                )
            assertEquals(
                first.currentConversationHeadSha256,
                failure.lastValidConversationHeadSha256OrNull,
            )
            assertTrue(
                failure.resumePossible,
            )
            assertEquals(
                "RESUME_LAST_VALID_HEAD_THEN_FORM_NEW_SEND_ATTEMPT",
                failure.exactNextSafeRoute,
            )

            val recovered =
                DirectCompanionTurn.performWithHooks(
                    setup = setup,
                    intent =
                        DirectCompanionTurn.Intent(
                            idempotencyKey = "send.self-development.fresh-recovery",
                            content = "Fresh recovery request",
                        ),
                    hooks =
                        DirectCompanionTurn.Hooks(
                            transport = transport,
                        ),
                )

            assertEquals(
                "Recovery continued from the last valid head",
                recovered.content,
            )
            assertEquals(
                "IMMEDIATE_PRIOR_COMPLETED_TURN",
                recovered.promptContextReceipt.contextMode,
            )
            assertEquals(
                3,
                calls,
            )

            val current =
                DirectConversationStore(
                    home,
                )
                    .current()
            assertEquals(
                4,
                current.canonicalEvents.size,
            )
            assertEquals(
                1,
                current.abandonedAttempts.size,
            )
            assertEquals(
                "Failed follow-up request",
                current.abandonedAttempts
                    .single()
                    .requestEvent
                    .content,
            )
        } finally {
            home.parent
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun completedDuplicateRequiresTheExactSameBrowserContext() {
        val home =
            productHome()
        var calls =
            0
        val transport =
            DirectCompanionConnection.Transport {
                calls +=
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
                                "content": "Browser-aware response"
                              }
                            }
                          ]
                        }
                        """.trimIndent(),
                )
            }
        val browserContext =
            DirectPromptContext.BrowserContext(
                pageUrl = VexLifeProdBrowserContract.InitialPageUrl,
                title = "Vextreme Terrain",
                text = "Current terrain text",
                capture = VexLifeProdBrowserContract.CaptureRendered,
            )
        val intent =
            DirectCompanionTurn.Intent(
                idempotencyKey = "send.self-development.browser-exact",
                content = "What is on this page?",
                browserContext = browserContext,
            )

        try {
            val first =
                DirectCompanionTurn.performWithHooks(
                    setup = setup(
                        home,
                    ),
                    intent = intent,
                    hooks =
                        DirectCompanionTurn.Hooks(
                            transport = transport,
                        ),
                )
            val duplicate =
                DirectCompanionTurn.performWithHooks(
                    setup = setup(
                        home,
                    ),
                    intent = intent,
                    hooks =
                        DirectCompanionTurn.Hooks(
                            transport = transport,
                        ),
                )

            assertEquals(
                1,
                calls,
            )
            assertFalse(
                first.recoveredCompletedTurn,
            )
            assertTrue(
                duplicate.recoveredCompletedTurn,
            )
            assertTrue(
                duplicate.promptContextReceipt.browserContextIncluded,
            )
            assertEquals(
                first.promptContextReceipt.browserContextSha256,
                duplicate.promptContextReceipt.browserContextSha256,
            )

            assertFailsWith<IllegalArgumentException> {
                DirectCompanionTurn.performWithHooks(
                    setup = setup(
                        home,
                    ),
                    intent =
                        intent.copy(
                            browserContext =
                                browserContext.copy(
                                    text = "Different terrain text",
                                ),
                        ),
                    hooks =
                        DirectCompanionTurn.Hooks(
                            transport = transport,
                        ),
                )
            }
            assertEquals(
                1,
                calls,
            )
        } finally {
            home.parent
                .toFile()
                .deleteRecursively()
        }
    }

}

// [VXG RealForever]
