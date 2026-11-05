package vexlife.local

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
 * Cross-process serialization for the ordinary VexLife-Prod local setup effect.
 *
 * The lock file is a stable product coordinate beside the Prod Home. The operating-system file
 * lock, not file presence or a remembered PID, owns serialization. Process exit releases the lock.
 *
 * This owner grants no Home/model/runtime repair authority.
 */
internal object VexLifeProdLocalSetupLease {
    private const val LockSuffix =
        ".setup.lock"

    internal class Held private[local] constructor(
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
        productHome: Path,
    ): Held {
        require(
            productHome.isAbsolute,
        ) {
            "VexLife-Prod Home must be absolute before setup serialization"
        }

        val home =
            productHome.normalize()
        val parent =
            requireNotNull(
                home.parent,
            ) {
                "VexLife-Prod Home requires one parent directory"
            }

        Files.createDirectories(
            parent,
        )

        val lockPath =
            parent.resolve(
                "." +
                    home.fileName +
                    LockSuffix,
            )

        if (
            Files.exists(
                lockPath,
                LinkOption.NOFOLLOW_LINKS,
            )
        ) {
            requireRegularFile(
                lockPath,
            )
        }

        val channel =
            FileChannel.open(
                lockPath,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                LinkOption.NOFOLLOW_LINKS,
            )

        try {
            requireRegularFile(
                lockPath,
            )
            secureFile(
                lockPath,
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
                "another VexLife-Prod local setup is already in progress"
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

    fun lockPath(
        productHome: Path,
    ): Path {
        require(
            productHome.isAbsolute,
        ) {
            "VexLife-Prod Home must be absolute"
        }
        val home =
            productHome.normalize()
        return requireNotNull(
            home.parent,
        )
            .resolve(
                "." +
                    home.fileName +
                    LockSuffix,
            )
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
            "VexLife-Prod setup lock must be one regular non-symlink file"
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
