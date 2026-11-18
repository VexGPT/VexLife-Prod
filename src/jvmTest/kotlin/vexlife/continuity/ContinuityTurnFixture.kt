package vexlife.continuity

import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.time.Instant
import vexlife.conversation.CanonicalLivedContextFixture
import vexlife.conversation.DirectConversationStore
import vexlife.home.CanonicalVexLifeHomeFixture

/** A real canonical Home with real committed turns at chosen human times, for continuity tests. */
class ContinuityTurnFixture : AutoCloseable {
    val parent: Path = Files.createTempDirectory("vexlife-prod-continuity-").toAbsolutePath()
    val home: Path = parent.resolve(".vexlife").also { CanonicalVexLifeHomeFixture.form(it) }
    val store = DirectConversationStore(home)

    data class Line(val human: String, val vex: String, val at: Instant)

    /** Commit [lines] as completed turns; returns each turn's (request, response). */
    fun commit(
        vararg lines: Line
    ): List<Pair<DirectConversationStore.Event, DirectConversationStore.Event>> =
        store.withWriterLease {
            lines.map { line ->
                val index = (store.current().canonicalEvents.size / 2) + 1
                val suffix = "%03d".format(index)
                val request =
                    beginRequest(
                        turnRef = "turn.continuity.$suffix",
                        requestMessageRef = "message.continuity.human.$suffix",
                        content = line.human,
                        formedAt = line.at,
                    )
                val response =
                    appendResponse(
                        request = request,
                        responseMessageRef = "message.continuity.companion.$suffix",
                        content = line.vex,
                        model = "default_model",
                        endpointOrigin = "http://127.0.0.1:8765",
                        runtimeObservationRef = "observation.continuity.$suffix",
                        invocationEvidenceRef = "evidence.continuity.$suffix",
                        formedAt = line.at.plusSeconds(1),
                    )
                val context =
                    CanonicalLivedContextFixture.write(
                        productHome = home,
                        request = request,
                        response = response,
                        formedAt = line.at.plusSeconds(2),
                    )
                commitCompletedTurn(
                    request = request,
                    response = response,
                    contextSha256 = context.sha256,
                    formedAt = line.at.plusSeconds(3),
                )
                request to response
            }
        }

    /**
     * Every file under the Home with its bytes' hash: equal before and after means nothing was
     * written.
     */
    fun homeTree(): Map<String, String> =
        Files.walk(home).use { paths ->
            paths
                .filter { Files.isRegularFile(it) }
                .toList()
                .associate { path ->
                    home.relativize(path).toString() to
                        MessageDigest.getInstance("SHA-256")
                            .digest(Files.readAllBytes(path))
                            .joinToString("") { "%02x".format(it) }
                }
        }

    override fun close() {
        parent.toFile().deleteRecursively()
    }
}

// [VXG RealForever]
