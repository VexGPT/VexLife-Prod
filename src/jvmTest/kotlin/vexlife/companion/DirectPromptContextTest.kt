package vexlife.companion

import java.nio.file.Files
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vexlife.conversation.DirectConversationStore
import vexlife.home.VexLifeProdHome
import vexlife.home.VexLifeProdHomeFiles

class DirectPromptContextTest {
    private fun home(
        productHome: java.nio.file.Path,
    ) {
        VexLifeProdHomeFiles.bootstrap(
            productHome = productHome,
            plan =
                VexLifeProdHome.plan(
                    personRef = VexLifeProdHome.DefaultPersonRef,
                    familyRef = VexLifeProdHome.DefaultFamilyRef,
                    deviceName = "Test Mac",
                    platform = "darwin",
                    architecture = "arm64",
                    ids =
                        VexLifeProdHome.InstallationIds(
                            deviceRef = "device.vexlife-prod.test-device",
                            companionLineageRef = "companion-lineage.vexlife-prod.test-lineage",
                        ),
                    createdAt = "2026-11-05T04:00:00Z",
                ),
        )
    }

    @Test
    fun firstTurnMaterializesOnlyTheCurrentDurableRequest() {
        val parent =
            Files.createTempDirectory(
                "vexlife-prod-direct-prompt-first-",
            )
                .toAbsolutePath()
        val productHome =
            parent.resolve(
                "VexLife-Prod",
            )

        try {
            home(
                productHome,
            )
            val store =
                DirectConversationStore(
                    productHome,
                )

            store.withWriterLease {
                assertNull(
                    immediatePriorCompletedTurn(),
                )

                val request =
                    beginRequest(
                        turnRef = "turn.self-development.001",
                        requestMessageRef = "message.self-development.human.001",
                        content = "Hello Vex",
                    )
                val materialized =
                    DirectPromptContext.materialize(
                        prior = null,
                        currentRequest = request,
                    )

                assertEquals(
                    listOf(
                        "user",
                    ),
                    materialized.messages.map {
                        it.role
                    },
                )
                assertEquals(
                    listOf(
                        "Hello Vex",
                    ),
                    materialized.messages.map {
                        it.content
                    },
                )
                assertEquals(
                    "FIRST_TURN",
                    materialized.receipt.contextMode,
                )
                assertTrue(
                    materialized.receipt.selectedConversationEventRefs.isEmpty(),
                )
                assertFalse(
                    materialized.receipt.wholeHistoryEventEnumerationPerformed,
                )
                assertFalse(
                    materialized.receipt.memoryIncluded,
                )
            }
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun continueSelectsExactlyTheImmediatePriorCompletedPair() {
        val parent =
            Files.createTempDirectory(
                "vexlife-prod-direct-prompt-continue-",
            )
                .toAbsolutePath()
        val productHome =
            parent.resolve(
                "VexLife-Prod",
            )

        try {
            home(
                productHome,
            )
            val store =
                DirectConversationStore(
                    productHome,
                )

            store.withWriterLease {
                val first =
                    beginRequest(
                        turnRef = "turn.self-development.001",
                        requestMessageRef = "message.self-development.human.001",
                        content = "First request",
                        formedAt = Instant.parse(
                            "2026-11-05T04:01:00Z",
                        ),
                    )
                completeResponse(
                    request = first,
                    responseMessageRef = "message.self-development.companion.001",
                    content = "First response",
                    model = "default_model",
                    endpointOrigin = "http://127.0.0.1:8765",
                    runtimeObservationRef = "observation.vexlife-prod.model-runtime.first",
                    invocationEvidenceRef = "evidence.vexlife-prod.model-runtime-invocation.first",
                    formedAt = Instant.parse(
                        "2026-11-05T04:01:01Z",
                    ),
                )
            }

            store.withWriterLease {
                val prior =
                    requireNotNull(
                        immediatePriorCompletedTurn(),
                    )
                val second =
                    beginRequest(
                        turnRef = "turn.self-development.002",
                        requestMessageRef = "message.self-development.human.002",
                        content = "Second request",
                    )
                val materialized =
                    DirectPromptContext.materialize(
                        prior = prior,
                        currentRequest = second,
                    )

                assertEquals(
                    listOf(
                        "user",
                        "assistant",
                        "user",
                    ),
                    materialized.messages.map {
                        it.role
                    },
                )
                assertEquals(
                    listOf(
                        "First request",
                        "First response",
                        "Second request",
                    ),
                    materialized.messages.map {
                        it.content
                    },
                )
                assertEquals(
                    2,
                    materialized.receipt.selectedConversationEventRefs.size,
                )
                assertEquals(
                    prior.head.conversationHeadSha256,
                    materialized.receipt.priorConversationHeadSha256,
                )
                assertFalse(
                    materialized.receipt.wholeHistoryEventEnumerationPerformed,
                )

                DirectPromptContext.reverify(
                    materialization = materialized,
                    prior =
                        requireNotNull(
                            immediatePriorCompletedTurn(),
                        ),
                    currentRequest = second,
                )
            }
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun oversizedProviderMaterializationFailsBeforeConnection() {
        val parent =
            Files.createTempDirectory(
                "vexlife-prod-direct-prompt-budget-",
            )
                .toAbsolutePath()
        val productHome =
            parent.resolve(
                "VexLife-Prod",
            )

        try {
            home(
                productHome,
            )
            val store =
                DirectConversationStore(
                    productHome,
                )

            store.withWriterLease {
                val request =
                    beginRequest(
                        turnRef = "turn.self-development.large",
                        requestMessageRef = "message.self-development.human.large",
                        content =
                            "x".repeat(
                                DirectPromptContext.InputTokenBudget *
                                    4,
                            ),
                    )

                assertFailsWith<IllegalArgumentException> {
                    DirectPromptContext.materialize(
                        prior = null,
                        currentRequest = request,
                    )
                }
            }
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }
}

// [VXG RealForever]
