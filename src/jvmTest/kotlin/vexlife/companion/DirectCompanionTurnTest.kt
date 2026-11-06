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
    fun successfulHighPressureTurnProducesDurableRolloverCheckpointForNextTurn() {
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
                                "send.self-development.rollover-producer",
                            content =
                                requestContent,
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
                                                    "content": "Rollover producer response"
                                                  }
                                                }
                                              ]
                                            }
                                            """.trimIndent(),
                                    )
                                },
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
                result.state,
            )

            val continuity =
                assertNotNull(
                    DirectWorkingContinuity.read(
                        productHome = home,
                        turnRef = result.turnRef,
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

            val ownerCurrent =
                assertNotNull(
                    VexLifeWorkingContinuityOwner(
                        home,
                    )
                        .readCurrent(
                            lineageRef =
                                DirectConversationStore(
                                    home,
                                )
                                    .current()
                                    .head
                                    ?.companionLineageRef
                                    ?: error(
                                        "completed turn lost lineage",
                                    ),
                            threadRef =
                                DirectConversationStore(
                                    home,
                                )
                                    .current()
                                    .head
                                    ?.threadRef
                                    ?: error(
                                        "completed turn lost thread",
                                    ),
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
                result.requestEventRef,
                ownerCurrent.checkpoint.workingSet.cursorEventRef,
            )
            assertTrue(
                ownerCurrent.checkpoint.workingSet.currentStatementRefs.contains(
                    "statement.score.continuity.001",
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
