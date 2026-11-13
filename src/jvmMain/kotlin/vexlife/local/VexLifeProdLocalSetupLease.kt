package vexlife.local

import java.nio.file.Files
import java.nio.file.Path
import vexlife.filesystem.VexExclusiveFileLease

/**
 * Stable setup-serialization coordinate for the ordinary VexLife-Prod local setup effect.
 *
 * OS file-lock mechanics are delegated to [VexExclusiveFileLease]. This owner answers only which
 * lock coordinate serializes one Prod Home setup.
 */
internal object VexLifeProdLocalSetupLease {
    private const val LockSuffix = ".setup.lock"

    fun acquire(productHome: Path): VexExclusiveFileLease.Held {
        val path = lockPath(productHome)

        // The lock sits beside the not-yet-existing Prod Home in ambient user space.
        Files.createDirectories(requireNotNull(path.parent))

        return try {
            VexExclusiveFileLease.acquire(path)
        } catch (error: IllegalArgumentException) {
            throw IllegalArgumentException(
                "another VexLife-Prod local setup is already in progress or the setup lease is invalid",
                error,
            )
        }
    }

    fun lockPath(productHome: Path): Path {
        require(productHome.isAbsolute) {
            "VexLife-Prod Home must be absolute"
        }
        val home = productHome.normalize()
        return requireNotNull(home.parent).resolve("." + home.fileName + LockSuffix)
    }
}

// [VXG RealForever]
