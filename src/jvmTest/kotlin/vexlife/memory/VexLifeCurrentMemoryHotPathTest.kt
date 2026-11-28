package vexlife.memory

import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import vexlife.conversation.CanonicalLivedContextFixture
import vexlife.conversation.DirectConversationStore
import vexlife.home.CanonicalVexLifeHomeFixture

class VexLifeCurrentMemoryHotPathTest {
    @Test
    fun exactReuseAndOneTurnAdvanceStayBoundedWhileSkippedAdvanceRebuilds() {
        val parent =
            Files.createTempDirectory(
                "vexlife-current-memory-hot-path-",
            )
                .toAbsolutePath()
        val home =
            parent.resolve(
                ".vexlife",
            )

        try {
            CanonicalVexLifeHomeFixture.form(
                home,
            )
            completeTurn(
                home = home,
                suffix = "one",
                baseTime =
                    Instant.parse(
                        "2026-11-26T08:00:00Z",
                    ),
            )

            val hotPath =
                VexLifeCurrentMemoryHotPath(
                    home,
                )
            val first =
                hotPath.readSelection()
            assertFalse(
                first.dailyContext.included,
            )
            assertEquals(
                listOf(
                    "NO_COMMITTED_DAILY_MEMORY",
                ),
                first.dailyContext.reasons,
            )
            assertEquals(
                VexLifeCurrentMemoryProviderSelection.read(
                    home,
                ),
                first,
                "initial hot-path rebuild must equal canonical provider selection",
            )
            assertEquals(
                VexLifeCurrentMemoryHotPath.Diagnostics(
                    canonicalRebuildCount = 1,
                    directConversationAdvanceCount = 0,
                    exactReuseCount = 0,
                ),
                hotPath.diagnostics(),
            )

            hotPath.readSelection()
            assertEquals(
                VexLifeCurrentMemoryHotPath.Diagnostics(
                    canonicalRebuildCount = 1,
                    directConversationAdvanceCount = 0,
                    exactReuseCount = 1,
                ),
                hotPath.diagnostics(),
            )

            completeTurn(
                home = home,
                suffix = "two",
                baseTime =
                    Instant.parse(
                        "2026-11-26T08:10:00Z",
                    ),
            )
            val advanced =
                hotPath.readSelection()
            assertEquals(
                VexLifeCurrentMemoryProviderSelection.read(
                    home,
                ),
                advanced,
                "one-turn hot-path advance must equal fresh canonical provider selection",
            )
            assertEquals(
                VexLifeCurrentMemoryHotPath.Diagnostics(
                    canonicalRebuildCount = 1,
                    directConversationAdvanceCount = 1,
                    exactReuseCount = 1,
                ),
                hotPath.diagnostics(),
            )

            completeTurn(
                home = home,
                suffix = "three",
                baseTime =
                    Instant.parse(
                        "2026-11-26T08:20:00Z",
                    ),
            )
            completeTurn(
                home = home,
                suffix = "four",
                baseTime =
                    Instant.parse(
                        "2026-11-26T08:30:00Z",
                    ),
            )
            hotPath.readSelection()
            assertEquals(
                VexLifeCurrentMemoryHotPath.Diagnostics(
                    canonicalRebuildCount = 2,
                    directConversationAdvanceCount = 1,
                    exactReuseCount = 1,
                ),
                hotPath.diagnostics(),
            )
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }

    private fun completeTurn(
        home: Path,
        suffix: String,
        baseTime: Instant,
    ) {
        val store =
            DirectConversationStore(
                home,
            )
        store.withWriterLease(
            instanceRef =
                "instance.vexlife-prod.memory-hot-path." +
                    suffix,
        ) {
            val request =
                beginRequest(
                    turnRef =
                        "turn.vexlife-prod.memory-hot-path." +
                            suffix,
                    requestMessageRef =
                        "message.vexlife-prod.memory-hot-path.human." +
                            suffix,
                    content =
                        "Memory hot-path request " +
                            suffix,
                    formedAt =
                        baseTime,
                )
            val response =
                appendResponse(
                    request = request,
                    responseMessageRef =
                        "message.vexlife-prod.memory-hot-path.vex." +
                            suffix,
                    content =
                        "Memory hot-path response " +
                            suffix,
                    model =
                        "default_model",
                    endpointOrigin =
                        "http://127.0.0.1:8765",
                    runtimeObservationRef =
                        "observation.vexlife-prod.memory-hot-path." +
                            suffix,
                    invocationEvidenceRef =
                        "evidence.vexlife-prod.memory-hot-path." +
                            suffix,
                    formedAt =
                        baseTime.plusSeconds(
                            1,
                        ),
                )
            val context =
                CanonicalLivedContextFixture.write(
                    productHome = home,
                    request = request,
                    response = response,
                    formedAt =
                        baseTime.plusSeconds(
                            2,
                        ),
                )
            commitCompletedTurn(
                request = request,
                response = response,
                contextSha256 =
                    context.sha256,
                formedAt =
                    baseTime.plusSeconds(
                        3,
                    ),
            )
        }
    }
}

// [VXG RealForever]
