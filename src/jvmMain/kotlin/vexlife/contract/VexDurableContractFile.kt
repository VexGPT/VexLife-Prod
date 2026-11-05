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

/**
 * Atomic filesystem realization for one strict durable Vex contract.
 *
 * Domain source owns the schema and meaning. This owner only proves the path is one regular
 * non-symlink file, encodes/decodes through [VexContractJson], and performs private atomic writes.
 */
object VexDurableContractFile {
    inline fun <reified T> read(
        path: Path,
    ): T? {
        require(
            path.isAbsolute,
        ) {
            "durable contract path must be absolute"
        }

        if (
            !Files.exists(
                path,
                LinkOption.NOFOLLOW_LINKS,
            )
        ) {
            return null
        }

        requireRegularFile(
            path,
        )

        return VexContractJson.decode(
            Files.readString(
                path,
                StandardCharsets.UTF_8,
            ),
        )
    }

    inline fun <reified T> write(
        path: Path,
        value: T,
    ) {
        require(
            path.isAbsolute,
        ) {
            "durable contract path must be absolute"
        }

        if (
            Files.exists(
                path,
                LinkOption.NOFOLLOW_LINKS,
            )
        ) {
            requireRegularFile(
                path,
            )
        }

        secureDirectory(
            requireNotNull(
                path.parent,
            ) {
                "durable contract path requires a parent directory"
            },
        )

        val temporary =
            path.resolveSibling(
                "${path.fileName}.partial-${UUID.randomUUID()}",
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
                    path,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING,
                )
            } catch (
                _: AtomicMoveNotSupportedException,
            ) {
                error(
                    "atomic durable contract replacement is unavailable for $path",
                )
            }
            secureFile(
                path,
            )
        } finally {
            Files.deleteIfExists(
                temporary,
            )
        }
    }

    fun delete(
        path: Path,
    ): Boolean {
        require(
            path.isAbsolute,
        ) {
            "durable contract path must be absolute"
        }

        if (
            !Files.exists(
                path,
                LinkOption.NOFOLLOW_LINKS,
            )
        ) {
            return false
        }

        requireRegularFile(
            path,
        )

        return Files.deleteIfExists(
            path,
        )
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

    @PublishedApi
    internal fun secureDirectory(
        path: Path,
    ) {
        Files.createDirectories(
            path,
        )
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
