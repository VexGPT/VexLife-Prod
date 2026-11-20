package vexlife.capability.reach

import java.io.IOException
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import vexlife.artifact.VexLifeLocalArtifact
import vexlife.artifact.VexLifeLocalArtifactConversion

/**
 * Read-only reach into one already-granted Local Source folder.
 *
 * Every path is relative to the exact reobserved grant root. Symlinks are never followed. Reads
 * reuse the same bounded Local Artifact conversion rules for text / ZIP / DOCX / XLSX / PPTX but
 * do not copy source files into attachment custody.
 */
object VexLifeLocalSourceReach {
    const val MaximumListEntries = 200
    const val MaximumVisitedEntries = 20_000
    const val MaximumMatches = 50
    const val MaximumMatchesPerFile = 3
    const val MaximumSearchedFileBytes = 1024 * 1024
    const val MaximumSnippetCharacters = 240
    const val DefaultReadLines = 400
    const val MaximumReadLines = 1_000
    const val MaximumStartLine = 1_000_000
    const val MaximumReturnBytes = 48 * 1024

    val SkippedFolderNames =
        setOf(
            ".git",
            ".hg",
            ".svn",
            "node_modules",
            ".gradle",
            "build",
            "target",
            "dist",
            ".venv",
            "venv",
            "__pycache__",
            ".idea",
        )

    data class Entry(
        val name: String,
        val directory: Boolean,
        val bytes: Long?,
    )

    data class Match(
        val path: String,
        val line: Int?,
        val snippet: String?,
    )

    sealed interface Outcome {
        data class Listed(
            val path: String,
            val entries: List<Entry>,
            val totalEntries: Int,
            val truncated: Boolean,
        ) : Outcome

        data class Found(
            val matches: List<Match>,
            val visitedEntries: Int,
            val complete: Boolean,
        ) : Outcome

        data class Read(
            val path: String,
            val format: String,
            val text: String,
            val sourceBytes: Long,
            val startLine: Int,
            val endLine: Int,
            val nextLine: Int?,
        ) : Outcome

        data class NotFound(
            val reason: String,
        ) : Outcome

        data class Refused(
            val reason: String,
        ) : Outcome
    }

    fun list(
        root: Path,
        relative: String? = null,
    ): Outcome {
        val target =
            resolve(
                root = root,
                relative =
                    relative
                        ?: ".",
            )
        when (
            target
        ) {
            is Resolved.Problem ->
                return target.outcome

            is Resolved.Ready ->
                Unit
        }
        val path =
            (target as Resolved.Ready).path
        if (
            !Files.isDirectory(
                path,
                LinkOption.NOFOLLOW_LINKS,
            )
        ) {
            return Outcome.Refused(
                display(
                    root,
                    path,
                ) +
                    " is a file; read it instead.",
            )
        }

        val children =
            Files.list(
                path,
            )
                .use {
                    stream ->
                    stream
                        .map {
                            child ->
                            val attributes =
                                Files.readAttributes(
                                    child,
                                    BasicFileAttributes::class.java,
                                    LinkOption.NOFOLLOW_LINKS,
                                )
                            Entry(
                                name =
                                    child.fileName
                                        .toString() +
                                        when {
                                            attributes.isDirectory ->
                                                "/"

                                            attributes.isSymbolicLink ->
                                                " (link, not followed)"

                                            else ->
                                                ""
                                        },
                                directory =
                                    attributes.isDirectory,
                                bytes =
                                    if (
                                        attributes.isRegularFile
                                    ) {
                                        attributes.size()
                                    } else {
                                        null
                                    },
                            )
                        }
                        .toList()
                }
                .sortedWith(
                    compareBy<Entry> {
                        !it.directory
                    }
                        .thenBy {
                            it.name
                        },
                )

        return Outcome.Listed(
            path =
                display(
                    root,
                    path,
                ),
            entries =
                children.take(
                    MaximumListEntries,
                ),
            totalEntries =
                children.size,
            truncated =
                children.size >
                    MaximumListEntries,
        )
    }

    fun find(
        root: Path,
        relative: String? = null,
        name: String? = null,
        text: String? = null,
    ): Outcome {
        val nameTerm =
            name
                ?.trim()
                ?.lowercase()
                ?.takeIf {
                    it.isNotEmpty()
                }
        val textTerm =
            text
                ?.trim()
                ?.lowercase()
                ?.takeIf {
                    it.isNotEmpty()
                }

        if (
            nameTerm ==
                null &&
            textTerm ==
                null
        ) {
            return Outcome.Refused(
                "find needs a file-name term, a text term, or both.",
            )
        }

        val target =
            resolve(
                root = root,
                relative =
                    relative
                        ?: ".",
            )
        when (
            target
        ) {
            is Resolved.Problem ->
                return target.outcome

            is Resolved.Ready ->
                Unit
        }
        val start =
            (target as Resolved.Ready).path
        if (
            !Files.isDirectory(
                start,
                LinkOption.NOFOLLOW_LINKS,
            )
        ) {
            return Outcome.Refused(
                "find start path is not a folder.",
            )
        }

        val matches =
            mutableListOf<Match>()
        var visited =
            0
        var complete =
            true

        Files.walkFileTree(
            start,
            object : SimpleFileVisitor<Path>() {
                override fun preVisitDirectory(
                    dir: Path,
                    attrs: BasicFileAttributes,
                ): FileVisitResult =
                    when {
                        Files.isSymbolicLink(
                            dir,
                        ) ->
                            FileVisitResult.SKIP_SUBTREE

                        dir !=
                            start &&
                            dir.fileName
                                ?.toString() in
                            SkippedFolderNames ->
                            FileVisitResult.SKIP_SUBTREE

                        else ->
                            FileVisitResult.CONTINUE
                    }

                override fun visitFile(
                    file: Path,
                    attrs: BasicFileAttributes,
                ): FileVisitResult {
                    visited +=
                        1
                    if (
                        visited >
                        MaximumVisitedEntries ||
                        matches.size >=
                        MaximumMatches
                    ) {
                        complete =
                            false
                        return FileVisitResult.TERMINATE
                    }

                    if (
                        !attrs.isRegularFile ||
                        Files.isSymbolicLink(
                            file,
                        )
                    ) {
                        return FileVisitResult.CONTINUE
                    }

                    val relativePath =
                        display(
                            root,
                            file,
                        )
                    if (
                        nameTerm !=
                            null &&
                        !relativePath
                            .lowercase()
                            .contains(
                                nameTerm,
                            )
                    ) {
                        return FileVisitResult.CONTINUE
                    }

                    if (
                        textTerm ==
                            null
                    ) {
                        matches +=
                            Match(
                                path = relativePath,
                                line = null,
                                snippet = null,
                            )
                        return FileVisitResult.CONTINUE
                    }

                    if (
                        attrs.size() >
                        MaximumSearchedFileBytes
                    ) {
                        return FileVisitResult.CONTINUE
                    }

                    textMatches(
                        file = file,
                        display = relativePath,
                        term = textTerm,
                    )
                        .forEach {
                            match ->
                            if (
                                matches.size <
                                MaximumMatches
                            ) {
                                matches +=
                                    match
                            }
                        }

                    return FileVisitResult.CONTINUE
                }

                override fun visitFileFailed(
                    file: Path,
                    exc: IOException,
                ): FileVisitResult =
                    FileVisitResult.CONTINUE
            },
        )

        return Outcome.Found(
            matches = matches,
            visitedEntries =
                visited.coerceAtMost(
                    MaximumVisitedEntries,
                ),
            complete = complete,
        )
    }

    fun read(
        root: Path,
        relative: String,
        startLine: Int? = null,
        maxLines: Int? = null,
    ): Outcome {
        val first =
            startLine
                ?: 1
        val count =
            maxLines
                ?: DefaultReadLines

        if (
            first !in
            1..MaximumStartLine
        ) {
            return Outcome.Refused(
                "startLine must be 1-" +
                    MaximumStartLine +
                    ".",
            )
        }
        if (
            count !in
            1..MaximumReadLines
        ) {
            return Outcome.Refused(
                "maxLines must be 1-" +
                    MaximumReadLines +
                    ".",
            )
        }

        val target =
            resolve(
                root = root,
                relative = relative,
            )
        when (
            target
        ) {
            is Resolved.Problem ->
                return target.outcome

            is Resolved.Ready ->
                Unit
        }
        val path =
            (target as Resolved.Ready).path
        val attributes =
            Files.readAttributes(
                path,
                BasicFileAttributes::class.java,
                LinkOption.NOFOLLOW_LINKS,
            )

        if (
            attributes.isDirectory
        ) {
            return Outcome.Refused(
                display(
                    root,
                    path,
                ) +
                    " is a folder; list it instead.",
            )
        }
        if (
            !attributes.isRegularFile
        ) {
            return Outcome.Refused(
                display(
                    root,
                    path,
                ) +
                    " is not a regular file.",
            )
        }
        if (
            attributes.size() >
            VexLifeLocalArtifact.MaximumOriginalBytes
        ) {
            return Outcome.Refused(
                display(
                    root,
                    path,
                ) +
                    " exceeds the current " +
                    (
                        VexLifeLocalArtifact.MaximumOriginalBytes /
                            (1024 * 1024)
                    ) +
                    " MB bounded source-read limit.",
            )
        }

        val bytes =
            Files.newInputStream(
                path,
                LinkOption.NOFOLLOW_LINKS,
            )
                .use {
                    input ->
                    input.readNBytes(
                        VexLifeLocalArtifact.MaximumOriginalBytes +
                            1,
                    )
                }

        val readable =
            when (
                val result =
                    VexLifeLocalArtifactConversion.readable(
                        displayName =
                            path.fileName
                                ?.toString()
                                ?: relative,
                        bytes = bytes,
                    )
            ) {
                is VexLifeLocalArtifactConversion.Result.Readable ->
                    result

                is VexLifeLocalArtifactConversion.Result.Held ->
                    return Outcome.Refused(
                        result.reason,
                    )
            }

        val lines =
            readable.text
                .removeSuffix(
                    "\n",
                )
                .split(
                    '\n',
                )

        if (
            first >
            lines.size
        ) {
            return Outcome.NotFound(
                display(
                    root,
                    path,
                ) +
                    " has fewer than " +
                    first +
                    " readable lines.",
            )
        }

        val selected =
            mutableListOf<String>()
        var usedBytes =
            0
        val lastExclusive =
            minOf(
                lines.size,
                first - 1 + count,
            )

        for (
            line in
            lines.subList(
                first - 1,
                lastExclusive,
            )
        ) {
            val addition =
                line.toByteArray(
                    StandardCharsets.UTF_8,
                ).size +
                    if (
                        selected.isEmpty()
                    ) {
                        0
                    } else {
                        1
                    }

            if (
                usedBytes +
                    addition >
                MaximumReturnBytes
            ) {
                break
            }

            selected +=
                line
            usedBytes +=
                addition
        }

        if (
            selected.isEmpty()
        ) {
            return Outcome.Refused(
                "The requested line is larger than one bounded source read; no partial line was returned.",
            )
        }

        val endLine =
            first +
                selected.size -
                1

        return Outcome.Read(
            path =
                display(
                    root,
                    path,
                ),
            format =
                readable.format.name,
            text =
                selected.joinToString(
                    "\n",
                ),
            sourceBytes =
                attributes.size(),
            startLine = first,
            endLine = endLine,
            nextLine =
                (
                    endLine +
                        1
                ).takeIf {
                    it <=
                        lines.size
                },
        )
    }

    private sealed interface Resolved {
        data class Ready(
            val path: Path,
        ) : Resolved

        data class Problem(
            val outcome: Outcome,
        ) : Resolved
    }

    private fun resolve(
        root: Path,
        relative: String,
    ): Resolved {
        val stableRoot =
            root
                .toAbsolutePath()
                .normalize()

        val requested =
            runCatching {
                Path.of(
                    relative.trim()
                        .ifEmpty {
                            "."
                        },
                )
            }
                .getOrNull()
                ?: return Resolved.Problem(
                    Outcome.Refused(
                        "source path is invalid.",
                    ),
                )

        if (
            requested.isAbsolute
        ) {
            return Resolved.Problem(
                Outcome.Refused(
                    "source paths must be relative to the granted folder.",
                ),
            )
        }

        val candidate =
            stableRoot
                .resolve(
                    requested,
                )
                .normalize()

        if (
            candidate !=
                stableRoot &&
            !candidate.startsWith(
                stableRoot,
            )
        ) {
            return Resolved.Problem(
                Outcome.Refused(
                    "source path must stay inside the granted folder.",
                ),
            )
        }

        var current =
            stableRoot
        for (
            segment in
            stableRoot.relativize(
                candidate,
            )
        ) {
            if (
                segment
                    .toString()
                    .isEmpty()
            ) {
                continue
            }
            current =
                current.resolve(
                    segment,
                )

            if (
                !Files.exists(
                    current,
                    LinkOption.NOFOLLOW_LINKS,
                )
            ) {
                return Resolved.Problem(
                    Outcome.NotFound(
                        display(
                            stableRoot,
                            candidate,
                        ) +
                            " does not exist.",
                    ),
                )
            }
            if (
                Files.isSymbolicLink(
                    current,
                )
            ) {
                return Resolved.Problem(
                    Outcome.Refused(
                        display(
                            stableRoot,
                            current,
                        ) +
                            " is a link; links are not followed.",
                    ),
                )
            }
        }

        return Resolved.Ready(
            candidate,
        )
    }

    private fun textMatches(
        file: Path,
        display: String,
        term: String,
    ): List<Match> {
        val bytes =
            runCatching {
                Files.readAllBytes(
                    file,
                )
            }
                .getOrNull()
                ?: return emptyList()

        val text =
            try {
                StandardCharsets.UTF_8
                    .newDecoder()
                    .onMalformedInput(
                        CodingErrorAction.REPORT,
                    )
                    .onUnmappableCharacter(
                        CodingErrorAction.REPORT,
                    )
                    .decode(
                        java.nio.ByteBuffer.wrap(
                            bytes,
                        ),
                    )
                    .toString()
            } catch (
                _: CharacterCodingException,
            ) {
                return emptyList()
            }

        return text
            .lineSequence()
            .withIndex()
            .filter {
                (_, line) ->
                line
                    .lowercase()
                    .contains(
                        term,
                    )
            }
            .take(
                MaximumMatchesPerFile,
            )
            .map {
                (index, line) ->
                Match(
                    path = display,
                    line = index + 1,
                    snippet =
                        line
                            .trim()
                            .take(
                                MaximumSnippetCharacters,
                            ),
                )
            }
            .toList()
    }

    private fun display(
        root: Path,
        path: Path,
    ): String {
        val relative =
            root
                .toAbsolutePath()
                .normalize()
                .relativize(
                    path,
                )
                .toString()
        return relative.ifEmpty {
            "."
        }
    }
}

// [VXG RealForever]
