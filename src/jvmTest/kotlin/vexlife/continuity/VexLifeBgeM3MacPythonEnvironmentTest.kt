package vexlife.continuity

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VexLifeBgeM3MacPythonEnvironmentTest {
    @Test
    fun sourceOwnedLockInitializesAndMissingEnvironmentStaysHeld() {
        val home =
            Files.createTempDirectory(
                "vexlife-bge-env-",
            )
                .toAbsolutePath()

        try {
            val root =
                VexLifeBgeM3MacPythonEnvironment.environmentRoot(
                    home,
                )

            assertTrue(
                root.startsWith(
                    home,
                ),
            )
            assertEquals(
                "python-3.14.7-bge-m3-onnx-cpu-a001",
                root.fileName.toString(),
            )
            assertEquals(
                "6f8ac7a80f51130da147f8e0755c724961b7f897ea86de9487bc600674707510",
                VexLifeBgeM3MacPythonEnvironment.RequirementsSha256,
            )
            assertNull(
                VexLifeBgeM3MacPythonEnvironment.observeCurrent(
                    home,
                ),
            )
            assertTrue(
                Files.notExists(
                    root,
                ),
                "read-only environment observation must not create runtime directories",
            )
        } finally {
            home
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun presentButUnqualifiedEnvironmentFailsClosedRatherThanBecomingMissing() {
        val home =
            Files.createTempDirectory(
                "vexlife-bge-env-invalid-",
            )
                .toAbsolutePath()

        try {
            val root =
                VexLifeBgeM3MacPythonEnvironment.environmentRoot(
                    home,
                )
            Files.createDirectories(
                root,
            )

            assertFailsWith<IllegalArgumentException> {
                VexLifeBgeM3MacPythonEnvironment.observeCurrent(
                    home,
                )
            }
            assertTrue(
                Files.isDirectory(
                    root,
                ),
                "read-only currentness must not repair or remove an invalid environment",
            )
        } finally {
            home
                .toFile()
                .deleteRecursively()
        }
    }
}

// [VXG RealForever]
