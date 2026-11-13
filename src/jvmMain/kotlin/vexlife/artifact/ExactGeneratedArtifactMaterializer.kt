package vexlife.artifact

import java.nio.charset.StandardCharsets
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.StandardCopyOption

/**
 * Construction-only custody for deterministic generated text artifacts.
 *
 * Callers own artifact meaning and exact expected members. This helper owns only:
 *
 * - one bounded output root;
 * - exact member-set equality;
 * - no symlink root/member;
 * - atomic replacement of generated text members where supported;
 * - byte-for-byte verification against caller-provided content.
 *
 * It is intentionally not a deployment, publication, semantic, or release owner.
 */
internal object ExactGeneratedArtifactMaterializer {
    enum class Mode {
        Write,
        Verify,
    }

    fun run(
        mode: Mode,
        root: Path,
        contentByPath: Map<String, String>,
    ) {
        require(
            contentByPath.isNotEmpty(),
        ) {
            "generated artifact set cannot be empty"
        }

        contentByPath.keys.forEach(
            ::requireFlatMember,
        )

        val target =
            root
                .toAbsolutePath()
                .normalize()

        require(
            !Files.exists(
                target,
                LinkOption.NOFOLLOW_LINKS,
            ) ||
                !Files.isSymbolicLink(
                    target,
                ),
        ) {
            "generated artifact root cannot be a symlink"
        }

        when (
            mode
        ) {
            Mode.Write ->
                write(
                    root = target,
                    contentByPath = contentByPath,
                )

            Mode.Verify ->
                verify(
                    root = target,
                    contentByPath = contentByPath,
                )
        }
    }

    private fun write(
        root: Path,
        contentByPath: Map<String, String>,
    ) {
        Files.createDirectories(
            root,
        )
        requireNoUnownedMembers(
            root = root,
            expectedPaths = contentByPath.keys,
        )

        contentByPath.forEach {
            (
                name,
                content,
            ) ->
            writeExact(
                target =
                    root.resolve(
                        name,
                    ),
                content = content,
            )
        }

        verify(
            root = root,
            contentByPath = contentByPath,
        )
    }

    private fun verify(
        root: Path,
        contentByPath: Map<String, String>,
    ) {
        require(
            Files.isDirectory(
                root,
                LinkOption.NOFOLLOW_LINKS,
            ) &&
                !Files.isSymbolicLink(
                    root,
                ),
        ) {
            "generated artifact root is unavailable or unsafe"
        }

        val observed =
            Files.list(
                root,
            )
                .use {
                    stream ->
                    stream
                        .map {
                            it.fileName
                                .toString()
                        }
                        .toList()
                        .toSet()
                }

        require(
            observed ==
                contentByPath.keys,
        ) {
            "generated artifact membrane drift: expected=" +
                contentByPath.keys +
                " observed=" +
                observed
        }

        contentByPath.forEach {
            (
                name,
                expected,
            ) ->
            val path =
                root.resolve(
                    name,
                )

            require(
                Files.isRegularFile(
                    path,
                    LinkOption.NOFOLLOW_LINKS,
                ) &&
                    !Files.isSymbolicLink(
                        path,
                    ),
            ) {
                "generated artifact is not one regular file: " +
                    name
            }

            val observedText =
                Files.readString(
                    path,
                    StandardCharsets.UTF_8,
                )

            require(
                observedText ==
                    expected,
            ) {
                "generated artifact drift: " +
                    name
            }
        }
    }

    private fun requireNoUnownedMembers(
        root: Path,
        expectedPaths: Set<String>,
    ) {
        val observed =
            Files.list(
                root,
            )
                .use {
                    stream ->
                    stream
                        .map {
                            it.fileName
                                .toString()
                        }
                        .toList()
                        .toSet()
                }

        require(
            observed.all {
                it in
                    expectedPaths
            },
        ) {
            "generated artifact root contains unowned files: " +
                observed
        }
    }

    private fun writeExact(
        target: Path,
        content: String,
    ) {
        val forming =
            target.resolveSibling(
                "." +
                    target.fileName +
                    ".forming",
            )

        require(
            !Files.exists(
                forming,
                LinkOption.NOFOLLOW_LINKS,
            ),
        ) {
            "stale generated-artifact formation file exists: " +
                forming.fileName
        }

        Files.writeString(
            forming,
            content,
            StandardCharsets.UTF_8,
        )

        try {
            Files.move(
                forming,
                target,
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING,
            )
        } catch (
            _: AtomicMoveNotSupportedException,
        ) {
            Files.move(
                forming,
                target,
                StandardCopyOption.REPLACE_EXISTING,
            )
        }
    }

    private fun requireFlatMember(
        path: String,
    ) {
        require(
            path.isNotBlank() &&
                '/' !in
                path &&
                '\\' !in
                path &&
                path !=
                "." &&
                path !=
                "..",
        ) {
            "generated artifact member must be one flat relative filename: " +
                path
        }
    }
}

// [VXG RealForever]
