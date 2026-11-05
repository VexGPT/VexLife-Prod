package vexlife.filesystem

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VexOwnedPathTest {
    @Test
    fun missingOwnedDirectoryChainIsCreatedOneRealComponentAtATime() {
        val root =
            Files.createTempDirectory(
                "vex-owned-path-",
            )
                .toAbsolutePath()

        try {
            val target =
                root.resolve(
                    "runtime/heretic/launchd",
                )

            assertEquals(
                VexOwnedPath.DirectoryState.Missing,
                VexOwnedPath.observeDirectory(
                    root,
                    target,
                ),
            )

            VexOwnedPath.ensureDirectory(
                root,
                target,
            )

            assertEquals(
                VexOwnedPath.DirectoryState.Present,
                VexOwnedPath.observeDirectory(
                    root,
                    target,
                ),
            )
            assertTrue(
                Files.isDirectory(
                    target,
                ),
            )
        } finally {
            root
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun intermediateSymlinkNeverCountsAsOwnedContainment() {
        val parent =
            Files.createTempDirectory(
                "vex-owned-path-link-",
            )
                .toAbsolutePath()
        val root =
            parent.resolve(
                "VexLife-Prod",
            )
        val outside =
            parent.resolve(
                "outside",
            )

        try {
            Files.createDirectory(
                root,
            )
            Files.createDirectory(
                outside,
            )
            Files.createSymbolicLink(
                root.resolve(
                    "runtime",
                ),
                outside,
            )

            val target =
                root.resolve(
                    "runtime/heretic",
                )

            assertTrue(
                target.normalize()
                    .startsWith(
                        root,
                    ),
                "lexical containment alone would incorrectly admit this path",
            )

            assertFailsWith<IllegalArgumentException> {
                VexOwnedPath.observeDirectory(
                    root,
                    target,
                )
            }
            assertFailsWith<IllegalArgumentException> {
                VexOwnedPath.ensureDirectory(
                    root,
                    target,
                )
            }

            assertFalse(
                Files.exists(
                    outside.resolve(
                        "heretic",
                    ),
                ),
            )
        } finally {
            Files.deleteIfExists(
                root.resolve(
                    "runtime",
                ),
            )
            parent
                .toFile()
                .deleteRecursively()
        }
    }
}

// [VXG RealForever]
