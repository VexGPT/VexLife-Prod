package vexlife.conversation

import java.nio.file.Files
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vexlife.home.CanonicalVexLifeHomeFixture

class DirectConversationStoreTest {
    private fun home(
        root: java.nio.file.Path,
    ) {
        CanonicalVexLifeHomeFixture.form(
            home = root,
            deviceSuffix = "test-device",
            lineageSuffix = "test-lineage",
        )
    }

    @Test
    fun requestIsDurableBeforeResponseAndHeadAdvancesOnlyAfterCompletePair() {
        val parent =
            Files.createTempDirectory(
                "vexlife-prod-direct-conversation-",
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
                    current().head,
                )

                val request =
                    beginRequest(
                        turnRef = "turn.self-development.001",
                        requestMessageRef = "message.self-development.human.001",
                        content = "Hello Vex",
                        formedAt = Instant.parse(
                            "2026-11-05T03:31:00Z",
                        ),
                    )

                val eventFiles =
                    Files.walk(
                        productHome.resolve(
                            "conversations",
                        ),
                    )
                        .use {
                            stream ->
                            stream
                                .filter(
                                    Files::isRegularFile,
                                )
                                .map {
                                    it.fileName
                                        .toString()
                                }
                                .toList()
                        }

                assertTrue(
                    eventFiles.any {
                        it.contains(
                            request.eventSha256,
                        )
                    },
                )
                assertTrue(
                    eventFiles.none {
                        it ==
                            "head.json"
                    },
                )

                val response =
                    appendResponse(
                        request = request,
                        responseMessageRef = "message.self-development.companion.001",
                        content = "Hello human.",
                        model = "default_model",
                        endpointOrigin = "http://127.0.0.1:8765",
                        runtimeObservationRef = "observation.vexlife-prod.model-runtime.test",
                        invocationEvidenceRef = "evidence.vexlife-prod.model-runtime-invocation.test",
                        formedAt = Instant.parse(
                            "2026-11-05T03:31:01Z",
                        ),
                    )
                val context =
                    CanonicalLivedContextFixture.write(
                        productHome = productHome,
                        request = request,
                        response = response,
                        formedAt = Instant.parse(
                            "2026-11-05T03:31:02Z",
                        ),
                    )
                val head =
                    commitCompletedTurn(
                        request = request,
                        response = response,
                        contextSha256 = context.sha256,
                        formedAt = Instant.parse(
                            "2026-11-05T03:31:03Z",
                        ),
                    )

                assertEquals(
                    1L,
                    head.sequence,
                )

                val snapshot =
                    current()
                assertEquals(
                    2,
                    snapshot.events.size,
                )
                assertEquals(
                    head,
                    snapshot.head,
                )
                assertEquals(
                    listOf(
                        "REQUEST",
                        "RESPONSE",
                    ),
                    snapshot.events.map {
                        it.eventKind
                    },
                )
            }
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun recentCompletedConversationWalksBackwardOnlyUntilTheBudgetBoundary() {
        val parent =
            Files.createTempDirectory(
                "vexlife-prod-direct-conversation-recent-",
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
                val olderRequest =
                    beginRequest(
                        turnRef = "turn.self-development.recent.001",
                        requestMessageRef = "message.self-development.human.recent.001",
                        content = "Older request",
                    )
                val olderResponse =
                    appendResponse(
                        request = olderRequest,
                        responseMessageRef = "message.self-development.companion.recent.001",
                        content = "Older response",
                        model = "default_model",
                        endpointOrigin = "http://127.0.0.1:8765",
                        runtimeObservationRef = "observation.vexlife-prod.model-runtime.recent.001",
                        invocationEvidenceRef = "evidence.vexlife-prod.model-runtime-invocation.recent.001",
                    )
                val olderContext =
                    CanonicalLivedContextFixture.write(
                        productHome = productHome,
                        request = olderRequest,
                        response = olderResponse,
                    )
                commitCompletedTurn(
                    request = olderRequest,
                    response = olderResponse,
                    contextSha256 = olderContext.sha256,
                )

                val newerRequest =
                    beginRequest(
                        turnRef = "turn.self-development.recent.002",
                        requestMessageRef = "message.self-development.human.recent.002",
                        content = "Newer request",
                    )
                val newerResponse =
                    appendResponse(
                        request = newerRequest,
                        responseMessageRef = "message.self-development.companion.recent.002",
                        content = "Newer response",
                        model = "default_model",
                        endpointOrigin = "http://127.0.0.1:8765",
                        runtimeObservationRef = "observation.vexlife-prod.model-runtime.recent.002",
                        invocationEvidenceRef = "evidence.vexlife-prod.model-runtime-invocation.recent.002",
                    )
                val newerContext =
                    CanonicalLivedContextFixture.write(
                        productHome = productHome,
                        request = newerRequest,
                        response = newerResponse,
                    )
                commitCompletedTurn(
                    request = newerRequest,
                    response = newerResponse,
                    contextSha256 = newerContext.sha256,
                )

                val bounded =
                    recentCompletedConversation(
                        availableTokens = 50,
                        estimatedMessageTokens = {
                            content ->
                            if (
                                content.startsWith(
                                    "Older",
                                )
                            ) {
                                100
                            } else {
                                10
                            }
                        },
                    )

                assertEquals(
                    listOf(
                        "Newer request",
                        "Newer response",
                    ),
                    bounded.events.map {
                        it.content
                    },
                )
                assertEquals(
                    4,
                    bounded.exactEventFileReadCount,
                )
                assertEquals(
                    2,
                    bounded.exactHeadFileReadCount,
                )
                assertTrue(
                    !bounded.wholeHistoryEventEnumerationPerformed,
                )

                val complete =
                    recentCompletedConversation(
                        availableTokens = 1_000,
                        estimatedMessageTokens = {
                            10
                        },
                    )

                assertEquals(
                    listOf(
                        "Older request",
                        "Older response",
                        "Newer request",
                        "Newer response",
                    ),
                    complete.events.map {
                        it.content
                    },
                )
                assertEquals(
                    4,
                    complete.exactEventFileReadCount,
                )
                assertEquals(
                    2,
                    complete.exactHeadFileReadCount,
                )
                assertTrue(
                    complete.wholeHistoryEventEnumerationPerformed,
                )
            }
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun exactConversationReturnResolvesOnlyCurrentCommittedSourceAddresses() {
        val parent =
            Files.createTempDirectory(
                "vexlife-prod-direct-conversation-return-",
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

            var olderRequestOrNull: DirectConversationStore.Event? =
                null
            var olderResponseOrNull: DirectConversationStore.Event? =
                null
            var newerResponseOrNull: DirectConversationStore.Event? =
                null

            store.withWriterLease {
                val olderRequest =
                    beginRequest(
                        turnRef = "turn.self-development.return.001",
                        requestMessageRef = "message.self-development.human.return.001",
                        content = "Older source request",
                    )
                olderRequestOrNull =
                    olderRequest
                val olderResponse =
                    appendResponse(
                        request = olderRequest,
                        responseMessageRef = "message.self-development.companion.return.001",
                        content = "Older source response",
                        model = "default_model",
                        endpointOrigin = "http://127.0.0.1:8765",
                        runtimeObservationRef = "observation.vexlife-prod.model-runtime.return.001",
                        invocationEvidenceRef = "evidence.vexlife-prod.model-runtime-invocation.return.001",
                    )
                olderResponseOrNull =
                    olderResponse
                val olderContext =
                    CanonicalLivedContextFixture.write(
                        productHome = productHome,
                        request = olderRequest,
                        response = olderResponse,
                    )
                commitCompletedTurn(
                    request = olderRequest,
                    response = olderResponse,
                    contextSha256 = olderContext.sha256,
                )

                val newerRequest =
                    beginRequest(
                        turnRef = "turn.self-development.return.002",
                        requestMessageRef = "message.self-development.human.return.002",
                        content = "Newer source request",
                    )
                val newerResponse =
                    appendResponse(
                        request = newerRequest,
                        responseMessageRef = "message.self-development.companion.return.002",
                        content = "Newer source response",
                        model = "default_model",
                        endpointOrigin = "http://127.0.0.1:8765",
                        runtimeObservationRef = "observation.vexlife-prod.model-runtime.return.002",
                        invocationEvidenceRef = "evidence.vexlife-prod.model-runtime-invocation.return.002",
                    )
                newerResponseOrNull =
                    newerResponse
                val newerContext =
                    CanonicalLivedContextFixture.write(
                        productHome = productHome,
                        request = newerRequest,
                        response = newerResponse,
                    )
                commitCompletedTurn(
                    request = newerRequest,
                    response = newerResponse,
                    contextSha256 = newerContext.sha256,
                )

                val older =
                    listOf(
                        requireNotNull(
                            olderRequestOrNull,
                        ),
                        requireNotNull(
                            olderResponseOrNull,
                        ),
                    )
                val returnedOlder =
                    exactCommittedConversationEvents(
                        older.map {
                            event ->
                            DirectConversationStore.EventAddress(
                                sequence = event.sequence,
                                eventRef = event.eventRef,
                                eventHash = event.eventHash,
                            )
                        },
                    )

                assertEquals(
                    older,
                    returnedOlder.events,
                )
                assertEquals(
                    2,
                    returnedOlder.exactEventFileReadCount,
                )
                assertEquals(
                    2,
                    returnedOlder.exactHeadFileReadCount,
                )
                assertTrue(
                    returnedOlder.wholeHistoryHeadTraversalPerformed,
                )
                assertTrue(
                    !returnedOlder.effectsPerformed,
                )

                val newerResponse =
                    requireNotNull(
                        newerResponseOrNull,
                    )
                val returnedNewest =
                    exactCommittedConversationEvents(
                        listOf(
                            DirectConversationStore.EventAddress(
                                sequence = newerResponse.sequence,
                                eventRef = newerResponse.eventRef,
                                eventHash = newerResponse.eventHash,
                            ),
                        ),
                    )

                assertEquals(
                    listOf(
                        newerResponse,
                    ),
                    returnedNewest.events,
                )
                assertEquals(
                    2,
                    returnedNewest.exactEventFileReadCount,
                )
                assertEquals(
                    1,
                    returnedNewest.exactHeadFileReadCount,
                )
                assertTrue(
                    !returnedNewest.wholeHistoryHeadTraversalPerformed,
                )

                assertFailsWith<IllegalArgumentException> {
                    exactCommittedConversationEvents(
                        listOf(
                            DirectConversationStore.EventAddress(
                                sequence = newerResponse.sequence,
                                eventRef = "event.vexlife.response.substituted",
                                eventHash = newerResponse.eventHash,
                            ),
                        ),
                    )
                }

                val failedRequest =
                    beginRequest(
                        turnRef = "turn.self-development.return.failed.003",
                        requestMessageRef = "message.self-development.human.return.failed.003",
                        content = "Off-head source must not return as committed.",
                    )

                assertFailsWith<IllegalArgumentException> {
                    exactCommittedConversationEvents(
                        listOf(
                            DirectConversationStore.EventAddress(
                                sequence = failedRequest.sequence,
                                eventRef = failedRequest.eventRef,
                                eventHash = failedRequest.eventHash,
                            ),
                        ),
                    )
                }
            }
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun offHeadFailedRequestRemainsDurableWhileNewTurnMayBranchFromLastValidHead() {
        val parent =
            Files.createTempDirectory(
                "vexlife-prod-direct-conversation-failed-branch-",
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
                beginRequest(
                    turnRef = "turn.self-development.failed.001",
                    requestMessageRef = "message.self-development.human.failed.001",
                    content = "Failed request",
                )
            }

            val failedState =
                store.current()
            assertNull(
                failedState.head,
            )
            assertTrue(
                failedState.canonicalEvents.isEmpty(),
            )
            assertEquals(
                1,
                failedState.abandonedAttempts.size,
            )
            assertEquals(
                "Failed request",
                failedState.abandonedAttempts
                    .single()
                    .requestEvent
                    .content,
            )

            store.withWriterLease {
                val replacement =
                    beginRequest(
                        turnRef = "turn.self-development.new.002",
                        requestMessageRef = "message.self-development.human.new.002",
                        content = "New admitted request",
                    )

                assertEquals(
                    0L,
                    replacement.sequence,
                )
                assertNull(
                    replacement.priorEventSha256,
                )
            }

            val twoBranches =
                store.current()
            assertEquals(
                2,
                twoBranches.abandonedAttempts.size,
            )
            assertTrue(
                twoBranches.canonicalEvents.isEmpty(),
            )
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun threadWriterLeaseCoversTheWholeDirectTurn() {
        val parent =
            Files.createTempDirectory(
                "vexlife-prod-direct-conversation-lock-",
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
            val first =
                DirectConversationStore(
                    productHome,
                )
            val second =
                DirectConversationStore(
                    productHome,
                )

            first.withWriterLease {
                assertFailsWith<IllegalStateException> {
                    second.withWriterLease {
                        current()
                    }
                }
            }
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun symlinkedConversationDirectoryCannotRedirectDurableEvents() {
        val parent =
            Files.createTempDirectory(
                "vexlife-prod-direct-conversation-link-",
            )
                .toAbsolutePath()
        val productHome =
            parent.resolve(
                "VexLife-Prod",
            )
        val outside =
            parent.resolve(
                "outside",
            )

        try {
            home(
                productHome,
            )
            Files.createDirectory(
                outside,
            )
            Files.createSymbolicLink(
                productHome.resolve(
                    "conversations",
                ),
                outside,
            )

            val store =
                DirectConversationStore(
                    productHome,
                )

            assertFailsWith<IllegalArgumentException> {
                store.withWriterLease {
                    beginRequest(
                        turnRef = "turn.self-development.001",
                        requestMessageRef = "message.self-development.human.001",
                        content = "Hello",
                    )
                }
            }

            assertTrue(
                Files.list(
                    outside,
                )
                    .use {
                        !it.findAny()
                            .isPresent
                    },
            )
        } finally {
            Files.deleteIfExists(
                productHome.resolve(
                    "conversations",
                ),
            )
            parent
                .toFile()
                .deleteRecursively()
        }
    }
}

// [VXG RealForever]
