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
                val head =
                    commitCompletedTurn(
                        request = request,
                        response = response,
                        contextRef = "context.vexlife-prod.direct.test",
                        contextSha256 = "c".repeat(
                            64,
                        ),
                        formedAt = Instant.parse(
                            "2026-11-05T03:31:02Z",
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
                assertFailsWith<IllegalArgumentException> {
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
