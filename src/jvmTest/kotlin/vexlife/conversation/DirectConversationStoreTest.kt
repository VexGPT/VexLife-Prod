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
    fun admissionSnapshotIsBoundToOneWriterLeaseAndConsumedOnce() {
        val parent =
            Files.createTempDirectory(
                "vexlife-prod-direct-conversation-admission-snapshot-",
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
            lateinit var foreignAdmission: DirectConversationStore.Snapshot

            store.withWriterLease {
                foreignAdmission =
                    currentForRequestAdmission(
                        "message.self-development.human.admission.foreign",
                    )
                assertNull(
                    foreignAdmission.head,
                )
            }

            store.withWriterLease {
                assertFailsWith<IllegalArgumentException> {
                    beginRequest(
                        turnRef = "turn.self-development.admission.foreign",
                        requestMessageRef = "message.self-development.human.admission.foreign",
                        content = "A snapshot from another lease must not write.",
                        admissionSnapshot = foreignAdmission,
                    )
                }

                val admission =
                    currentForRequestAdmission(
                        "message.self-development.human.admission.001",
                    )
                val request =
                    beginRequest(
                        turnRef = "turn.self-development.admission.001",
                        requestMessageRef = "message.self-development.human.admission.001",
                        content = "Use the exact admitted snapshot.",
                        admissionSnapshot = admission,
                    )
                val response =
                    appendResponse(
                        request = request,
                        responseMessageRef = "message.self-development.companion.admission.001",
                        content = "Bound to the exact writer lease.",
                        model = "default_model",
                        endpointOrigin = "http://127.0.0.1:8765",
                        runtimeObservationRef =
                            "observation.vexlife-prod.model-runtime.admission.001",
                        invocationEvidenceRef =
                            "evidence.vexlife-prod.model-runtime-invocation.admission.001",
                    )
                val context =
                    CanonicalLivedContextFixture.write(
                        productHome = productHome,
                        request = request,
                        response = response,
                        formedAt =
                            Instant.parse(
                                "2026-11-25T12:00:02Z",
                            ),
                    )
                val committed =
                    commitCompletedTurn(
                        request = request,
                        response = response,
                        contextSha256 = context.sha256,
                    )

                assertEquals(
                    response.eventHash,
                    committed.eventHash,
                )
                assertFailsWith<IllegalArgumentException> {
                    beginRequest(
                        turnRef = "turn.self-development.admission.replay",
                        requestMessageRef = "message.self-development.human.admission.001",
                        content = "A consumed admission capability must not replay.",
                        admissionSnapshot = admission,
                    )
                }
                val after =
                    current()
                assertEquals(
                    committed,
                    after.head,
                )
                assertEquals(
                    2,
                    after.events.size,
                    "rejected admission capabilities must not append canonical events",
                )
            }
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun volatileAttemptIndexBoundsCurrentMissCommittedFailedAndStaleHeadRecovery() {
        val parent =
            Files.createTempDirectory(
                "vexlife-prod-direct-conversation-attempt-index-",
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
            val source =
                DirectConversationStore(
                    productHome,
                )
            source.withWriterLease {
                val committedRequest =
                    beginRequest(
                        turnRef = "turn.attempt-index.committed.001",
                        requestMessageRef = "message.attempt-index.human.committed.001",
                        content = "Committed attempt",
                    )
                val committedResponse =
                    appendResponse(
                        request = committedRequest,
                        responseMessageRef = "message.attempt-index.vex.committed.001",
                        content = "Committed response",
                        model = "default_model",
                        endpointOrigin = "http://127.0.0.1:8765",
                        runtimeObservationRef = "observation.attempt-index.committed.001",
                        invocationEvidenceRef = "evidence.attempt-index.committed.001",
                    )
                val context =
                    CanonicalLivedContextFixture.write(
                        productHome = productHome,
                        request = committedRequest,
                        response = committedResponse,
                        formedAt =
                            Instant.parse(
                                "2026-11-25T13:00:02Z",
                            ),
                    )
                commitCompletedTurn(
                    request = committedRequest,
                    response = committedResponse,
                    contextSha256 = context.sha256,
                    formedAt =
                        Instant.parse(
                            "2026-11-25T13:00:03Z",
                        ),
                )
            }
            source.withWriterLease {
                beginRequest(
                    turnRef = "turn.attempt-index.failed.002",
                    requestMessageRef = "message.attempt-index.human.failed.002",
                    content = "Failed attempt",
                )
            }

            val index =
                DirectConversationAttemptIndex()
            val indexed =
                DirectConversationStore(
                    productHome = productHome,
                    attemptIndex = index,
                )
            val rebuilt =
                index.rebuild(
                    indexed.current(),
                )
            assertTrue(
                rebuilt.wholeHistoryEventEnumerationPerformed,
            )
            assertEquals(
                2,
                rebuilt.attemptCount,
            )

            val committed =
                indexed.requestEventForMessageRef(
                    "message.attempt-index.human.committed.001",
                )
            assertEquals(
                "Committed attempt",
                assertNotNull(
                    committed.requestEvent,
                )
                    .content,
            )
            assertFalse(
                committed.wholeHistoryEventEnumerationPerformed,
            )
            assertFalse(
                committed.derivedIndexRebuilt,
            )

            val failed =
                indexed.requestEventForMessageRef(
                    "message.attempt-index.human.failed.002",
                )
            assertEquals(
                "Failed attempt",
                assertNotNull(
                    failed.requestEvent,
                )
                    .content,
            )
            assertFalse(
                failed.wholeHistoryEventEnumerationPerformed,
            )

            val miss =
                indexed.requestEventForMessageRef(
                    "message.attempt-index.human.new.003",
                )
            assertNull(
                miss.requestEvent,
            )
            assertFalse(
                miss.wholeHistoryEventEnumerationPerformed,
                "current service-lifetime index miss must not enumerate lifetime Conversation",
            )

            indexed.withWriterLease {
                val admission =
                    currentForRequestAdmission(
                        "message.attempt-index.human.new.003",
                    )
                assertFalse(
                    admission.wholeHistoryEventEnumerationPerformed,
                )
                val request =
                    beginRequest(
                        turnRef = "turn.attempt-index.new.003",
                        requestMessageRef = "message.attempt-index.human.new.003",
                        content = "Indexed new attempt",
                        admissionSnapshot = admission,
                    )
                val response =
                    appendResponse(
                        request = request,
                        responseMessageRef = "message.attempt-index.vex.new.003",
                        content = "Indexed new response",
                        model = "default_model",
                        endpointOrigin = "http://127.0.0.1:8765",
                        runtimeObservationRef = "observation.attempt-index.new.003",
                        invocationEvidenceRef = "evidence.attempt-index.new.003",
                    )
                val context =
                    CanonicalLivedContextFixture.write(
                        productHome = productHome,
                        request = request,
                        response = response,
                        formedAt =
                            Instant.parse(
                                "2026-11-25T13:10:02Z",
                            ),
                    )
                commitCompletedTurn(
                    request = request,
                    response = response,
                    contextSha256 = context.sha256,
                    formedAt =
                        Instant.parse(
                            "2026-11-25T13:10:03Z",
                        ),
                )
            }

            val incrementallyCurrent =
                indexed.requestEventForMessageRef(
                    "message.attempt-index.human.new.003",
                )
            assertEquals(
                "Indexed new attempt",
                assertNotNull(
                    incrementallyCurrent.requestEvent,
                )
                    .content,
            )
            assertFalse(
                incrementallyCurrent.wholeHistoryEventEnumerationPerformed,
            )

            val external =
                DirectConversationStore(
                    productHome,
                )
            external.withWriterLease {
                val request =
                    beginRequest(
                        turnRef = "turn.attempt-index.external.004",
                        requestMessageRef = "message.attempt-index.human.external.004",
                        content = "External completed advance",
                    )
                val response =
                    appendResponse(
                        request = request,
                        responseMessageRef = "message.attempt-index.vex.external.004",
                        content = "External response",
                        model = "default_model",
                        endpointOrigin = "http://127.0.0.1:8765",
                        runtimeObservationRef = "observation.attempt-index.external.004",
                        invocationEvidenceRef = "evidence.attempt-index.external.004",
                    )
                val context =
                    CanonicalLivedContextFixture.write(
                        productHome = productHome,
                        request = request,
                        response = response,
                        formedAt =
                            Instant.parse(
                                "2026-11-25T13:20:02Z",
                            ),
                    )
                commitCompletedTurn(
                    request = request,
                    response = response,
                    contextSha256 = context.sha256,
                    formedAt =
                        Instant.parse(
                            "2026-11-25T13:20:03Z",
                        ),
                )
            }

            val staleFallback =
                indexed.requestEventForMessageRef(
                    "message.attempt-index.human.external.004",
                )
            assertEquals(
                "External completed advance",
                assertNotNull(
                    staleFallback.requestEvent,
                )
                    .content,
            )
            assertTrue(
                staleFallback.wholeHistoryEventEnumerationPerformed,
                "completed-head drift must fail closed to canonical full recovery",
            )
            assertTrue(
                staleFallback.derivedIndexRebuilt,
            )

            val afterRebuildMiss =
                indexed.requestEventForMessageRef(
                    "message.attempt-index.human.never-seen.005",
                )
            assertNull(
                afterRebuildMiss.requestEvent,
            )
            assertFalse(
                afterRebuildMiss.wholeHistoryEventEnumerationPerformed,
            )
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
                        formedAt =
                            Instant.parse(
                                "2026-11-12T00:00:16Z",
                            ),
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
                        formedAt =
                            Instant.parse(
                                "2026-11-12T00:00:17Z",
                            ),
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
                        formedAt =
                            Instant.parse(
                                "2026-11-12T00:00:18Z",
                            ),
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
                        formedAt =
                            Instant.parse(
                                "2026-11-12T00:00:19Z",
                            ),
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

                val newestResponse =
                    requireNotNull(
                        newerResponseOrNull,
                    )
                val returnedNewest =
                    exactCommittedConversationEvents(
                        listOf(
                            DirectConversationStore.EventAddress(
                                sequence = newestResponse.sequence,
                                eventRef = newestResponse.eventRef,
                                eventHash = newestResponse.eventHash,
                            ),
                        ),
                    )

                assertEquals(
                    listOf(
                        newestResponse,
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
