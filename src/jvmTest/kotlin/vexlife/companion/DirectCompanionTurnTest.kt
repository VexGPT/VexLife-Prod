package vexlife.companion

import java.nio.file.Files
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import vexlife.home.VexLifeProdHome
import vexlife.home.VexLifeProdHomeFiles
import vexlife.local.VexLifeProdLocalSetup
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
        VexLifeProdHomeFiles.bootstrap(
            productHome = home,
            plan =
                VexLifeProdHome.plan(
                    personRef = VexLifeProdHome.DefaultPersonRef,
                    familyRef = VexLifeProdHome.DefaultFamilyRef,
                    deviceName = "Test Mac",
                    platform = "darwin",
                    architecture = "arm64",
                    ids =
                        VexLifeProdHome.InstallationIds(
                            deviceRef = "device.vexlife-prod.turn-device",
                            companionLineageRef = "companion-lineage.vexlife-prod.turn-lineage",
                        ),
                    createdAt = "2026-11-05T05:00:00Z",
                ),
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
                            sendAttemptRef = "send.self-development.001",
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
                            sendAttemptRef = "send.self-development.002",
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
    fun exactCompletedSendRetryReturnsDurableResultWithoutAnotherModelCall() {
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
                sendAttemptRef = "send.self-development.retry",
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
    fun providerFailureLeavesRequestDurableAndRecordsFirstFailureForExplicitRecovery() {
        val home =
            productHome()

        try {
            val error =
                assertFailsWith<DirectCompanionTurn.TurnException> {
                    DirectCompanionTurn.performWithHooks(
                        setup = setup(
                            home,
                        ),
                        intent =
                            DirectCompanionTurn.Intent(
                                sendAttemptRef = "send.self-development.failure",
                                content = "Please answer",
                            ),
                        hooks =
                            DirectCompanionTurn.Hooks(
                                transport =
                                    DirectCompanionConnection.Transport {
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

            assertFailsWith<DirectConversationStore.IncompleteConversationException> {
                DirectConversationStore(
                    home,
                )
                    .current()
            }
        } finally {
            home.parent
                .toFile()
                .deleteRecursively()
        }
    }
}

// [VXG RealForever]
