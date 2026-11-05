package vexlife.filesystem

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class VexExclusiveFileLeaseTest {
    @Test
    fun exactFileLeaseSerializesAndReleasesWithTheHandle() {
        val root =
            Files.createTempDirectory(
                "vex-exclusive-file-lease-",
            )
                .toAbsolutePath()
        val path =
            root.resolve(
                "owner.lock",
            )

        try {
            VexExclusiveFileLease
                .acquire(
                    path,
                )
                .use {
                    assertFailsWith<IllegalArgumentException> {
                        VexExclusiveFileLease.acquire(
                            path,
                        )
                    }
                }

            VexExclusiveFileLease
                .acquire(
                    path,
                )
                .use {
                    assertTrue(
                        Files.isRegularFile(
                            path,
                        ),
                    )
                }
        } finally {
            root
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun symlinkNeverBecomesLeaseAuthority() {
        val root =
            Files.createTempDirectory(
                "vex-exclusive-file-lease-link-",
            )
                .toAbsolutePath()
        val target =
            root.resolve(
                "target.lock",
            )
        val link =
            root.resolve(
                "owner.lock",
            )

        try {
            Files.writeString(
                target,
                "not-owned",
            )
            Files.createSymbolicLink(
                link,
                target,
            )

            assertFailsWith<IllegalArgumentException> {
                VexExclusiveFileLease.acquire(
                    link,
                )
            }
            assertTrue(
                Files.isSymbolicLink(
                    link,
                ),
            )
        } finally {
            Files.deleteIfExists(
                link,
            )
            root
                .toFile()
                .deleteRecursively()
        }
    }
}

// [VXG RealForever]
