package vexlife.local

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class VexLifeProdLocalSetupLeaseTest {
    @Test
    fun oneProdHomeAllowsOnlyOneConcurrentSetupLease() {
        val parent = Files.createTempDirectory("vexlife-prod-setup-lease-").toAbsolutePath()
        val productHome = parent.resolve("VexLife-Prod")

        try {
            VexLifeProdLocalSetupLease.acquire(productHome).use {
                assertFailsWith<IllegalArgumentException> {
                    VexLifeProdLocalSetupLease.acquire(productHome)
                }
            }

            VexLifeProdLocalSetupLease.acquire(productHome).use {
                // A released kernel lock is immediately reusable by the next setup process.
            }

            assertTrue(Files.isRegularFile(VexLifeProdLocalSetupLease.lockPath(productHome)))
        } finally {
            parent.toFile().deleteRecursively()
        }
    }

    @Test
    fun symlinkCanNeverBecomeSetupSerializationAuthority() {
        val parent = Files.createTempDirectory("vexlife-prod-setup-lease-link-").toAbsolutePath()
        val productHome = parent.resolve("VexLife-Prod")
        val target = parent.resolve("other.lock")
        val lockPath = VexLifeProdLocalSetupLease.lockPath(productHome)

        try {
            Files.writeString(target, "not-owned")
            Files.createSymbolicLink(lockPath, target)

            assertFailsWith<IllegalArgumentException> {
                VexLifeProdLocalSetupLease.acquire(productHome)
            }

            assertTrue(Files.isSymbolicLink(lockPath))
            assertTrue(Files.exists(target))
        } finally {
            Files.deleteIfExists(lockPath)
            parent.toFile().deleteRecursively()
        }
    }
}

// [VXG RealForever]
