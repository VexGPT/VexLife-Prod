package vexlife.capability.reach

import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import vexlife.sources.VexLifeLocalSourceStore

/**
 * Read-only reach into folders the person granted ([vexlife.sources.VexLifeLocalSources]).
 *
 * Listing and reading reuse [VexLifeHomeReach]'s root-confined mechanics with the grant as root:
 * normalized relative paths, component-wise symlink refusal, bounded bytes and line windows. That
 * is shared filesystem custody, not Home meaning. [find] is this owner's own bounded search, so Vex
 * can locate the right file in a large folder before reading any of it.
 */
object VexLifeLocalSourceReach {
    const val MaximumVisitedEntries = 20_000
    const val MaximumMatches = 50
    const val MaximumMatchesPerFile = 3
    const val MaximumSearchedFileBytes = 1024 * 1024
    const val MaximumSnippetCharacters = 200

    /** Folders that are almost never what a person means and can be enormous. */
    val SkippedFolderNames =
        setOf(".git", ".hg", ".svn", "node_modules", ".gradle", "build", "target", "dist", ".venv", "venv", "__pycache__", ".idea", ".DS_Store")

    data class Match(val path: String, val line: Int?, val snippet: String?)

    sealed interface Found {
        data class Matches(val matches: List<Match>, val visited: Int, val complete: Boolean) : Found

        data class Refused(val reason: String) : Found
    }

    fun root(productHome: Path, sourceRef: String?): Path? =
        sourceRef?.let { VexLifeLocalSourceStore(productHome).root(it) }

    /**
     * Files under [relative] whose relative path contains [name], and/or text lines containing
     * [text], both case-insensitive. Symlinks are never followed; heavy tool folders are skipped.
     */
    fun find(
        root: Path,
        relative: String?,
        name: String?,
        text: String?,
    ): Found {
        val nameTerm = name?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }
        val textTerm = text?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }
        if (nameTerm == null && textTerm == null) return Found.Refused("find needs name or text.")

        val start =
            when (val listed = VexLifeHomeReach.list(root, relative)) {
                is VexLifeHomeReach.Outcome.Listed -> root.resolve(listed.path.takeUnless { it == "." } ?: "").normalize()
                is VexLifeHomeReach.Outcome.NotFound -> return Found.Refused(listed.reason)
                is VexLifeHomeReach.Outcome.Refused -> return Found.Refused(listed.reason)
                else -> return Found.Refused("path is not a folder.")
            }

        val matches = mutableListOf<Match>()
        var visited = 0
        var complete = true
        Files.walkFileTree(
            start,
            object : SimpleFileVisitor<Path>() {
                override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult =
                    if (dir != start && dir.fileName?.toString() in SkippedFolderNames) {
                        FileVisitResult.SKIP_SUBTREE
                    } else {
                        FileVisitResult.CONTINUE
                    }

                override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                    visited += 1
                    if (visited > MaximumVisitedEntries || matches.size >= MaximumMatches) {
                        complete = false
                        return FileVisitResult.TERMINATE
                    }
                    if (!attrs.isRegularFile) return FileVisitResult.CONTINUE
                    val display = root.relativize(file).toString()
                    val nameMatches = nameTerm == null || display.lowercase().contains(nameTerm)
                    if (!nameMatches) return FileVisitResult.CONTINUE
                    if (textTerm == null) {
                        matches += Match(display, null, null)
                    } else if (attrs.size() <= MaximumSearchedFileBytes) {
                        textMatches(file, display, textTerm).forEach {
                            if (matches.size < MaximumMatches) matches += it
                        }
                    }
                    return FileVisitResult.CONTINUE
                }

                override fun visitFileFailed(file: Path, exc: IOException): FileVisitResult = FileVisitResult.CONTINUE
            },
        )
        return Found.Matches(matches, visited.coerceAtMost(MaximumVisitedEntries), complete)
    }

    private fun textMatches(file: Path, display: String, term: String): List<Match> {
        val bytes = runCatching { Files.readAllBytes(file) }.getOrNull() ?: return emptyList()
        if (bytes.contains(0)) return emptyList()
        val text = String(bytes, StandardCharsets.UTF_8)
        return text
            .lineSequence()
            .withIndex()
            .filter { (_, line) -> line.lowercase().contains(term) }
            .take(MaximumMatchesPerFile)
            .map { (index, line) -> Match(display, index + 1, line.trim().take(MaximumSnippetCharacters)) }
            .toList()
    }
}

// [VXG RealForever]
