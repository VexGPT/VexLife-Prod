package vexlife.filesystem

import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes

/**
 * Filesystem custody for paths below one already-owned real directory.
 *
 * Lexical containment alone is insufficient because an intermediate directory may be a symlink.
 * This utility walks every existing directory component with NOFOLLOW_LINKS and creates missing
 * owned directories one component at a time.
 *
 * It owns no domain schema, repair policy, or deletion authority.
 */
object VexOwnedPath {
    enum class DirectoryState {
        Present,
        Missing,
    }

    fun requireRoot(
        root: Path,
        label: String = "owned root",
    ): Path {
        require(
            root.isAbsolute,
        ) {
            "$label must be absolute"
        }

        val normalized =
            root.normalize()

        requireRealDirectory(
            normalized,
            label,
        )

        return normalized
    }

    fun resolveInside(
        root: Path,
        relative: String,
    ): Path {
        val ownedRoot =
            requireRoot(
                root,
            )
        val relativePath =
            Path.of(
                relative,
            )

        require(
            relative.isNotBlank() &&
                !relativePath.isAbsolute,
        ) {
            "owned relative path must be non-empty and relative"
        }

        val resolved =
            ownedRoot
                .resolve(
                    relativePath,
                )
                .normalize()

        require(
            resolved.startsWith(
                ownedRoot,
            ),
        ) {
            "owned relative path escaped its root"
        }

        return resolved
    }

    /**
     * Observe the directory chain without creating anything.
     *
     * Missing means the first missing component and everything below it are absent. Any present
     * component must be a real non-symlink directory.
     */
    fun observeDirectory(
        root: Path,
        directory: Path,
    ): DirectoryState {
        val (
            ownedRoot,
            target,
        ) =
            normalizeInside(
                root,
                directory,
            )

        var current =
            ownedRoot

        for (
            segment in
            ownedRoot.relativize(
                target,
            )
        ) {
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
                return DirectoryState.Missing
            }

            requireRealDirectory(
                current,
                "owned directory component",
            )
        }

        return DirectoryState.Present
    }

    /**
     * Create missing directory components below [root] without ever traversing an existing symlink.
     */
    fun ensureDirectory(
        root: Path,
        directory: Path,
    ): Path {
        val (
            ownedRoot,
            target,
        ) =
            normalizeInside(
                root,
                directory,
            )

        var current =
            ownedRoot

        for (
            segment in
            ownedRoot.relativize(
                target,
            )
        ) {
            current =
                current.resolve(
                    segment,
                )

            if (
                Files.exists(
                    current,
                    LinkOption.NOFOLLOW_LINKS,
                )
            ) {
                requireRealDirectory(
                    current,
                    "owned directory component",
                )
            } else {
                Files.createDirectory(
                    current,
                )
                requireRealDirectory(
                    current,
                    "new owned directory component",
                )
            }
        }

        return target
    }

    fun observeParent(
        root: Path,
        file: Path,
    ): DirectoryState =
        observeDirectory(
            root = root,
            directory =
                requireNotNull(
                    file
                        .normalize()
                        .parent,
                ) {
                    "owned file requires one parent directory"
                },
        )

    fun ensureParent(
        root: Path,
        file: Path,
    ): Path =
        ensureDirectory(
            root = root,
            directory =
                requireNotNull(
                    file
                        .normalize()
                        .parent,
                ) {
                    "owned file requires one parent directory"
                },
        )

    private fun normalizeInside(
        root: Path,
        target: Path,
    ): Pair<Path, Path> {
        val ownedRoot =
            requireRoot(
                root,
            )

        require(
            target.isAbsolute,
        ) {
            "owned target path must be absolute"
        }

        val normalizedTarget =
            target.normalize()

        require(
            normalizedTarget ==
                ownedRoot ||
                normalizedTarget.startsWith(
                    ownedRoot,
                ),
        ) {
            "owned target path escaped its root"
        }

        return ownedRoot to
            normalizedTarget
    }

    private fun requireRealDirectory(
        path: Path,
        label: String,
    ) {
        require(
            Files.exists(
                path,
                LinkOption.NOFOLLOW_LINKS,
            ),
        ) {
            "$label is missing: $path"
        }

        val attributes =
            Files.readAttributes(
                path,
                BasicFileAttributes::class.java,
                LinkOption.NOFOLLOW_LINKS,
            )

        require(
            attributes.isDirectory &&
                !attributes.isSymbolicLink,
        ) {
            "$label must be one real non-symlink directory: $path"
        }
    }
}

// [VXG RealForever]
