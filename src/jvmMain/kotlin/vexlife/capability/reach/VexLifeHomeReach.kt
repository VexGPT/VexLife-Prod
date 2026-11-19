package vexlife.capability.reach

import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.name

/**
 * Read-only owner of Vex's reach into his own Home: list a folder, read a text file.
 *
 * Containment: every path is relative to the Home, normalized, and walked component by component
 * without following a symlink, so nothing outside the Home is reachable through this owner.
 * Effects: none. It opens files for reading only and never creates, moves or deletes.
 *
 * Model weights and the managed Python environment are listed like any folder but are not text; a
 * read of a binary file returns its size and type instead of bytes.
 */
object VexLifeHomeReach {
    const val MaximumListEntries = 200
    const val MaximumReadBytes = 1024 * 1024
    const val MaximumReadLines = 1_000
    const val MaximumStartLine = 1_000_000

    data class Entry(val name: String, val directory: Boolean, val bytes: Long?)

    sealed interface Outcome {
        data class Listed(
            val path: String,
            val entries: List<Entry>,
            val totalEntries: Int,
            val truncated: Boolean,
        ) : Outcome

        data class Read(
            val path: String,
            val text: String,
            val bytes: Long,
            val bytesTruncated: Boolean,
        ) : Outcome

        data class ReadLines(
            val path: String,
            val text: String,
            val bytes: Long,
            val startLine: Int,
            val endLine: Int,
            val nextLine: Int?,
        ) : Outcome

        data class NotText(val path: String, val bytes: Long) : Outcome

        data class NotFound(val reason: String) : Outcome

        data class Refused(val reason: String) : Outcome
    }

    fun list(home: Path, relative: String?): Outcome {
        val target =
            resolve(home, relative ?: ".")
                ?: return Outcome.Refused("the path must stay inside the Home")
        val (path, problem) = target
        problem?.let {
            return it
        }
        if (!Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS))
            return Outcome.Refused("${display(home, path)} is a file; read it instead")
        val children =
            Files.list(path)
                .use { stream ->
                    stream
                        .map { child ->
                            val attributes =
                                Files.readAttributes(
                                    child,
                                    BasicFileAttributes::class.java,
                                    LinkOption.NOFOLLOW_LINKS,
                                )
                            Entry(
                                name =
                                    child.name +
                                        if (attributes.isDirectory) "/"
                                        else if (attributes.isSymbolicLink) " (link, not followed)"
                                        else "",
                                directory = attributes.isDirectory,
                                bytes = if (attributes.isRegularFile) attributes.size() else null,
                            )
                        }
                        .toList()
                }
                .sortedWith(compareBy<Entry> { !it.directory }.thenBy { it.name })
        return Outcome.Listed(
            path = display(home, path),
            entries = children.take(MaximumListEntries),
            totalEntries = children.size,
            truncated = children.size > MaximumListEntries,
        )
    }

    fun read(home: Path, relative: String): Outcome {
        val target =
            resolve(home, relative) ?: return Outcome.Refused("the path must stay inside the Home")
        val (path, problem) = target
        problem?.let {
            return it
        }
        val attributes =
            Files.readAttributes(path, BasicFileAttributes::class.java, LinkOption.NOFOLLOW_LINKS)
        if (attributes.isDirectory)
            return Outcome.Refused("${display(home, path)} is a folder; list it instead")
        if (!attributes.isRegularFile)
            return Outcome.Refused("${display(home, path)} is not a regular file")
        val size = attributes.size()
        val bytes =
            Files.newInputStream(path, LinkOption.NOFOLLOW_LINKS).use { input ->
                input.readNBytes(MaximumReadBytes)
            }
        if (bytes.take(4096).any { it == 0.toByte() })
            return Outcome.NotText(display(home, path), size)
        val decoder =
            StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPLACE)
                .onUnmappableCharacter(CodingErrorAction.REPLACE)
        return Outcome.Read(
            path = display(home, path),
            text = decoder.decode(ByteBuffer.wrap(bytes)).toString(),
            bytes = size,
            bytesTruncated = size > bytes.size,
        )
    }

    /**
     * Read a bounded line window without loading the entire file.
     *
     * Line coordinates are one-based. This is the continuation route for large text/log files whose
     * first-byte read would otherwise stop at [MaximumReadBytes].
     */
    fun readLines(
        home: Path,
        relative: String,
        startLine: Int = 1,
        maxLines: Int = 400,
    ): Outcome {
        if (startLine !in 1..MaximumStartLine) {
            return Outcome.Refused("startLine must be 1-$MaximumStartLine")
        }
        if (maxLines !in 1..MaximumReadLines) {
            return Outcome.Refused("maxLines must be 1-$MaximumReadLines")
        }

        val target =
            resolve(home, relative) ?: return Outcome.Refused("the path must stay inside the Home")
        val (path, problem) = target
        problem?.let {
            return it
        }
        val attributes =
            Files.readAttributes(path, BasicFileAttributes::class.java, LinkOption.NOFOLLOW_LINKS)
        if (attributes.isDirectory)
            return Outcome.Refused("${display(home, path)} is a folder; list it instead")
        if (!attributes.isRegularFile)
            return Outcome.Refused("${display(home, path)} is not a regular file")

        val size = attributes.size()
        val probe =
            Files.newInputStream(path, LinkOption.NOFOLLOW_LINKS).use { input ->
                input.readNBytes(4096)
            }
        if (probe.any { it == 0.toByte() }) {
            return Outcome.NotText(display(home, path), size)
        }

        val decoder =
            StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPLACE)
                .onUnmappableCharacter(CodingErrorAction.REPLACE)

        return Files.newInputStream(path, LinkOption.NOFOLLOW_LINKS).use { input ->
            InputStreamReader(input, decoder).buffered().use { reader ->
                var lineNumber = 1
                while (lineNumber < startLine) {
                    if (reader.readLine() == null) {
                        return@use Outcome.NotFound(
                            "${display(home, path)} has fewer than $startLine lines",
                        )
                    }
                    lineNumber += 1
                }

                val lines = mutableListOf<String>()
                while (lines.size < maxLines) {
                    val line = reader.readLine() ?: break
                    lines += line
                }
                val hasMore = reader.readLine() != null
                val endLine =
                    if (lines.isEmpty()) {
                        startLine - 1
                    } else {
                        startLine + lines.size - 1
                    }
                Outcome.ReadLines(
                    path = display(home, path),
                    text = lines.joinToString("\n"),
                    bytes = size,
                    startLine = startLine,
                    endLine = endLine,
                    nextLine =
                        if (hasMore) {
                            endLine + 1
                        } else {
                            null
                        },
                )
            }
        }
    }

    /** The real path for [relative], or why it cannot be used; null when it escapes the Home. */
    private fun resolve(home: Path, relative: String): Pair<Path, Outcome?>? {
        val root = home.toAbsolutePath().normalize()
        val cleaned = relative.trim().removePrefix("~/").removePrefix("/").ifEmpty { "." }
        val candidate = root.resolve(cleaned).normalize()
        if (candidate != root && !candidate.startsWith(root)) return null
        var current = root
        for (segment in root.relativize(candidate)) {
            if (segment.toString().isEmpty()) continue
            current = current.resolve(segment)
            if (!Files.exists(current, LinkOption.NOFOLLOW_LINKS)) {
                return candidate to Outcome.NotFound("${display(root, candidate)} does not exist")
            }
            if (Files.isSymbolicLink(current)) {
                return candidate to
                    Outcome.Refused("${display(root, current)} is a link; links are not followed")
            }
        }
        return candidate to null
    }

    private fun display(home: Path, path: Path): String {
        val relative = home.toAbsolutePath().normalize().relativize(path).toString()
        return relative.ifEmpty { "." }
    }
}

// [VXG RealForever]
