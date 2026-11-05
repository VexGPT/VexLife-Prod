package vexlife.filesystem

import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.channels.OverlappingFileLockException
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.attribute.PosixFilePermission

/**
 * Generic same-host cross-process exclusion over one exact regular file.
 *
 * The OS file lock owns concurrency. File presence and remembered PIDs do not. Callers own the
 * semantic coordinate and must decide where the lease belongs.
 */
object VexExclusiveFileLease {
    class Held internal constructor(
        private val channel: FileChannel,
        private val lock: FileLock,
    ) : AutoCloseable {
        private var closed =
            false

        override fun close() {
            if (
                closed
            ) {
                return
            }
            closed =
                true
            try {
                lock.release()
            } finally {
                channel.close()
            }
        }
    }

    fun acquire(
        path: Path,
    ): Held {
        require(
            path.isAbsolute,
        ) {
            "exclusive lease path must be absolute"
        }

        val normalized =
            path.normalize()
        val parent =
            requireNotNull(
                normalized.parent,
            ) {
                "exclusive lease requires one parent directory"
            }

        requireRealDirectory(
            parent,
            "exclusive lease parent",
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

        val channel =
            FileChannel.open(
                normalized,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                LinkOption.NOFOLLOW_LINKS,
            )

        try {
            requireRegularFile(
                normalized,
            )
            secureFile(
                normalized,
            )

            val lock =
                try {
                    channel.tryLock()
                } catch (
                    _: OverlappingFileLockException,
                ) {
                    null
                }

            require(
                lock !=
                    null,
            ) {
                "exclusive lease is already held"
            }

            return Held(
                channel = channel,
                lock = lock,
            )
        } catch (
            error: Exception,
        ) {
            channel.close()
            throw error
        }
    }

    private fun requireRegularFile(
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
            "exclusive lease must be one regular non-symlink file"
        }
    }

    private fun requireRealDirectory(
        path: Path,
        label: String,
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
            "$label must be one real non-symlink directory"
        }
    }

    private fun secureFile(
        path: Path,
    ) {
        try {
            Files.setPosixFilePermissions(
                path,
                setOf(
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE,
                ),
            )
        } catch (
            _: UnsupportedOperationException,
        ) {
            // Non-POSIX development hosts may not expose Unix permissions. macOS does.
        }
    }
}

// [VXG RealForever]
