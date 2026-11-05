package vexlife.conversation

import java.nio.file.Files
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vexlife.home.VexLifeProdHome
import vexlife.home.VexLifeProdHomeFiles

class DirectConversationStoreTest {
    private fun home(
        root: java.nio.file.Path,
    ) {
        VexLifeProdHomeFiles.bootstrap(
            productHome = root,
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
                    createdAt = "2026-11-05T03:30:00Z",
                ),
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
    fun incompleteDurableRequestHoldsOrdinaryContinuation() {
        val parent =
            Files.createTempDirectory(
                "vexlife-prod-direct-conversation-incomplete-",
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
                    turnRef = "turn.self-development.001",
                    requestMessageRef = "message.self-development.human.001",
                    content = "Hello Vex",
                )
            }

            assertFailsWith<DirectConversationStore.IncompleteConversationException> {
                store.current()
            }

            store.withWriterLease {
                assertFailsWith<DirectConversationStore.IncompleteConversationException> {
                    beginRequest(
                        turnRef = "turn.self-development.002",
                        requestMessageRef = "message.self-development.human.002",
                        content = "Second request",
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
