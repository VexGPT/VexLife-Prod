package vexlife.memory

import java.nio.file.Files
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vexlife.conversation.CanonicalLivedContextFixture
import vexlife.conversation.DirectConversationIdentity
import vexlife.conversation.DirectConversationStore
import vexlife.filesystem.VexOwnedPath
import vexlife.home.CanonicalVexLifeHomeFixture

class VexLifeCurrentMemoryReaderTest {
    @Test
    fun noCommittedScoreReturnsCurrentEmptyCoordinatesIndependentlyOfMemory() {
        val parent =
            Files.createTempDirectory(
                "vexlife-score-coordinates-empty-",
            )
                .toAbsolutePath()
        val home =
            parent.resolve(
                ".vexlife",
            )

        try {
            val formed =
                CanonicalVexLifeHomeFixture.form(
                    home,
                )

            val current =
                VexLifeCurrentMemoryReader.readCurrentScoreCoordinates(
                    home,
                )

            assertEquals(
                formed.manifest.homeRef,
                current.homeRef,
            )
            assertEquals(
                formed.manifest.currentDeviceRef,
                current.deviceRef,
            )
            assertEquals(
                formed.manifest.currentCompanionLineageRef,
                current.companionLineageRef,
            )
            assertEquals(
                DirectConversationIdentity.ThreadRef,
                current.threadRef,
            )
            assertNull(
                current.scoreHeadSha256,
            )
            assertNull(
                current.sourceConversationHeadSha256,
            )
            assertNull(
                current.semanticAuthorityHeadSha256,
            )
            assertTrue(
                current.currentStatementRefs.isEmpty(),
            )
            assertTrue(
                current.openLoopRefs.isEmpty(),
            )
            assertTrue(
                current.sourceRefs.isEmpty(),
            )
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun durableScoreMaterialWithoutCurrentHeadFailsClosedInsteadOfBecomingEmptyCoordinates() {
        val parent =
            Files.createTempDirectory(
                "vexlife-score-coordinates-orphan-",
            )
                .toAbsolutePath()
        val home =
            parent.resolve(
                ".vexlife",
            )

        try {
            val formed =
                CanonicalVexLifeHomeFixture.form(
                    home,
                )
            val eventsRoot =
                VexOwnedPath.resolveInside(
                    home,
                    "score/" +
                        formed.manifest.currentCompanionLineageRef +
                        "/" +
                        DirectConversationIdentity.ThreadRef +
                        "/events",
                )
            VexOwnedPath.ensureDirectory(
                home,
                eventsRoot,
            )
            Files.writeString(
                eventsRoot.resolve(
                    "orphan.json",
                ),
                "{}\n",
            )

            assertFailsWith<IllegalArgumentException> {
                VexLifeCurrentMemoryReader.readCurrentScoreCoordinates(
                    home,
                )
            }
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun promptFrontierTreatsHistoricalConversationSourceAsHeldNotCorrupt() {
        val historical =
            "a".repeat(
                64,
            )
        val current =
            "b".repeat(
                64,
            )

        assertEquals(
            "HELD",
            VexLifeCurrentMemoryReader.promptFrontierCurrentness(
                sourceConversationHeadSha256 = historical,
                currentConversationHeadSha256 = current,
            ),
        )
        assertEquals(
            "CURRENT",
            VexLifeCurrentMemoryReader.promptFrontierCurrentness(
                sourceConversationHeadSha256 = current,
                currentConversationHeadSha256 = current,
            ),
        )
    }

    @Test
    fun noCommittedDailyMemoryReturnsTruthfulHeldContextWithoutProviderContent() {
        val parent =
            Files.createTempDirectory(
                "vexlife-memory-reader-held-",
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

            val current =
                VexLifeCurrentMemoryReader.readCurrent(
                    home,
                )

            assertEquals(
                "HELD",
                current.projectionState,
            )
            assertEquals(
                "HELD",
                current.projectionCurrentness,
            )
            assertFalse(
                current.included,
            )
            assertEquals(
                listOf(
                    "NO_COMMITTED_DAILY_MEMORY",
                ),
                current.reasons,
            )
            assertTrue(
                current.pages.isEmpty(),
            )
            assertNull(
                current.systemMessageSha256,
            )
            assertEquals(
                0,
                current.estimatedSystemMessageTokens,
            )
            assertEquals(
                VexLifeMemoryContract.MaximumMemoryTokens,
                current.memoryTokenCap,
            )
            assertEquals(
                listOf(
                    VexLifeCurrentMemoryReader.LivingJournalMemoryOwnerRef,
                    VexLifeCurrentMemoryReader.LivingJournalDailyRecordOwnerRef,
                )
                    .sorted(),
                current.sourceOwnerRefs,
            )
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun promptMemoryCapMayYieldToTheCurrentTurnWithoutChangingMemoryOwnership() {
        val parent =
            Files.createTempDirectory(
                "vexlife-memory-reader-bounded-cap-",
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

            val zero =
                VexLifeCurrentMemoryReader.readCurrent(
                    productHome = home,
                    memoryTokenCap = 0,
                )
            val bounded =
                VexLifeCurrentMemoryReader.readCurrent(
                    productHome = home,
                    memoryTokenCap = 4_096,
                )

            assertEquals(
                0,
                zero.memoryTokenCap,
            )
            assertEquals(
                4_096,
                bounded.memoryTokenCap,
            )
            assertFalse(
                zero.included,
            )
            assertFalse(
                bounded.included,
            )
            assertEquals(
                listOf(
                    "NO_COMMITTED_DAILY_MEMORY",
                ),
                zero.reasons,
            )
            assertEquals(
                zero.reasons,
                bounded.reasons,
            )

            assertFailsWith<IllegalArgumentException> {
                VexLifeCurrentMemoryReader.readCurrent(
                    productHome = home,
                    memoryTokenCap = -1,
                )
            }
            assertFailsWith<IllegalArgumentException> {
                VexLifeCurrentMemoryReader.readCurrent(
                    productHome = home,
                    memoryTokenCap =
                        VexLifeMemoryContract.MaximumMemoryTokens +
                            1,
                )
            }
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun presentMalformedDailyHeadFailsClosedInsteadOfBecomingEmptyMemory() {
        val parent =
            Files.createTempDirectory(
                "vexlife-memory-reader-malformed-daily-",
            )
                .toAbsolutePath()
        val home =
            parent.resolve(
                ".vexlife",
            )

        try {
            val formed =
                CanonicalVexLifeHomeFixture.form(
                    home,
                )
            val conversation =
                DirectConversationStore(
                    home,
                )
            conversation.withWriterLease(
                instanceRef = "instance.vexlife.memory-reader.test",
            ) {
                val request =
                    beginRequest(
                        turnRef = "turn.vexlife.memory-reader.test",
                        requestMessageRef = "message.vexlife.memory-reader.human.test",
                        content = "Memory source request",
                        formedAt = Instant.parse(
                            "2026-11-05T20:00:00Z",
                        ),
                    )
                val response =
                    appendResponse(
                        request = request,
                        responseMessageRef = "message.vexlife.memory-reader.companion.test",
                        content = "Memory source response",
                        model = "default_model",
                        endpointOrigin = "http://127.0.0.1:8765",
                        runtimeObservationRef = "observation.vexlife.memory-reader.runtime.test",
                        invocationEvidenceRef = "evidence.vexlife.memory-reader.invocation.test",
                        formedAt = Instant.parse(
                            "2026-11-05T20:00:01Z",
                        ),
                    )
                val context =
                    CanonicalLivedContextFixture.write(
                        productHome = home,
                        request = request,
                        response = response,
                        formedAt = Instant.parse(
                            "2026-11-05T20:00:02Z",
                        ),
                    )
                commitCompletedTurn(
                    request = request,
                    response = response,
                    contextSha256 = context.sha256,
                    formedAt = Instant.parse(
                        "2026-11-05T20:00:03Z",
                    ),
                )
            }

            val dailyRoot =
                VexOwnedPath.resolveInside(
                    home,
                    "daily-memory-dream/" +
                        formed.manifest.currentCompanionLineageRef +
                        "/thread.self-development.open-conversation",
                )
            VexOwnedPath.ensureDirectory(
                home,
                dailyRoot,
            )
            Files.writeString(
                dailyRoot.resolve(
                    "head.json",
                ),
                "{}\n",
            )

            assertFailsWith<IllegalArgumentException> {
                VexLifeCurrentMemoryReader.readCurrent(
                    home,
                )
            }
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }
}

// [VXG RealForever]
