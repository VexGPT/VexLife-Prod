package vexlife.contract

import java.nio.charset.StandardCharsets
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.attribute.PosixFilePermission
import java.util.UUID
import vexlife.filesystem.VexOwnedPath

/**
 * Atomic filesystem realization for one strict durable Vex contract.
 *
 * Domain source owns schema and meaning. This owner only proves that the contract path stays below
 * one caller-supplied owned root, encodes/decodes through [VexContractJson], and performs private
 * atomic writes. Every existing directory component below the owned root is checked without
 * following symlinks.
 */
object VexDurableContractFile {
    inline fun <reified T> read(
        ownedRoot: Path,
        path: Path,
    ): T? =
        readText(
            ownedRoot = ownedRoot,
            path = path,
        )
            ?.let {
                text ->
                VexContractJson.decode<T>(
                    text,
                )
            }

    fun readText(
        ownedRoot: Path,
        path: Path,
    ): String? {
        val root =
            VexOwnedPath.requireRoot(
                ownedRoot,
                "durable contract owned root",
            )
        val normalized =
            normalizedFile(
                path,
            )

        if (
            VexOwnedPath.observeParent(
                root,
                normalized,
            ) ==
            VexOwnedPath.DirectoryState.Missing
        ) {
            return null
        }

        if (
            !Files.exists(
                normalized,
                LinkOption.NOFOLLOW_LINKS,
            )
        ) {
            return null
        }

        requireRegularFile(
            normalized,
        )

        return Files.readString(
            normalized,
            StandardCharsets.UTF_8,
        )
    }

    inline fun <reified T> write(
        ownedRoot: Path,
        path: Path,
        value: T,
    ) {
        val root =
            VexOwnedPath.requireRoot(
                ownedRoot,
                "durable contract owned root",
            )
        val normalized =
            normalizedFile(
                path,
            )

        VexOwnedPath.ensureParent(
            root,
            normalized,
        )

        if (
            Files.exists(
                normalized,
                LinkOption.NOFOLLOW_LINKS,
            )
        ) {
            requireRegularFile(
                normalized,
            )
        }

        val temporary =
            normalized.resolveSibling(
                "${normalized.fileName}.partial-${UUID.randomUUID()}",
            )

        try {
            Files.writeString(
                temporary,
                VexContractJson.encode(
                    value,
                ) +
                    "\n",
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE,
            )
            secureFile(
                temporary,
            )
            try {
                Files.move(
                    temporary,
                    normalized,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING,
                )
            } catch (
                _: AtomicMoveNotSupportedException,
            ) {
                error(
                    "atomic durable contract replacement is unavailable for $normalized",
                )
            }
            secureFile(
                normalized,
            )
        } finally {
            Files.deleteIfExists(
                temporary,
            )
        }
    }

    fun delete(
        ownedRoot: Path,
        path: Path,
    ): Boolean {
        val root =
            VexOwnedPath.requireRoot(
                ownedRoot,
                "durable contract owned root",
            )
        val normalized =
            normalizedFile(
                path,
            )

        if (
            VexOwnedPath.observeParent(
                root,
                normalized,
            ) ==
            VexOwnedPath.DirectoryState.Missing
        ) {
            return false
        }

        if (
            !Files.exists(
                normalized,
                LinkOption.NOFOLLOW_LINKS,
            )
        ) {
            return false
        }

        requireRegularFile(
            normalized,
        )

        return Files.deleteIfExists(
            normalized,
        )
    }

    @PublishedApi
    internal fun normalizedFile(
        path: Path,
    ): Path {
        require(
            path.isAbsolute,
        ) {
            "durable contract path must be absolute"
        }
        return path.normalize()
    }

    @PublishedApi
    internal fun requireRegularFile(
        path: Path,
    ) {
        val attributes =
            Files.readAttributes(
                path,
                BasicFileAttributes::class.java,
                LinkOption.NOFOLLOW_LINKS,
            )

        require(
            attributes.isRegularFile &&
                !attributes.isSymbolicLink,
        ) {
            "durable contract path must be one regular non-symlink file: $path"
        }
    }

    internal fun secureDirectory(
        path: Path,
    ) {
        if (
            Files.exists(
                path,
                LinkOption.NOFOLLOW_LINKS,
            )
        ) {
            requireRealDirectory(
                path,
            )
        } else {
            Files.createDirectory(
                path,
            )
            requireRealDirectory(
                path,
            )
        }

        setPosixPermissionsIfSupported(
            path,
            setOf(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE,
                PosixFilePermission.OWNER_EXECUTE,
            ),
        )
    }

    @PublishedApi
    internal fun secureFile(
        path: Path,
    ) {
        setPosixPermissionsIfSupported(
            path,
            setOf(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE,
            ),
        )
    }

    private fun requireRealDirectory(
        path: Path,
    ) {
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
            "durable contract directory must be one real non-symlink directory: $path"
        }
    }

    private fun setPosixPermissionsIfSupported(
        path: Path,
        permissions: Set<PosixFilePermission>,
    ) {
        try {
            Files.setPosixFilePermissions(
                path,
                permissions,
            )
        } catch (
            _: UnsupportedOperationException,
        ) {
            // Non-POSIX test hosts may not expose Unix permissions. macOS does.
        }
    }
}

// [VXG RealForever]
