package vexlife.home

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import vexlife.contract.VexDurableContractFile

class VexLifeProdHomeFilesTest {
    private fun plan(
        deviceSuffix: String,
        lineageSuffix: String,
    ): VexLifeProdHome.BootstrapPlan =
        VexLifeProdHome.plan(
            personRef = "person.local-user",
            familyRef = "vex-family.local-user",
            deviceName = "Test Mac",
            platform = "darwin",
            architecture = "arm64",
            ids =
                VexLifeProdHome.InstallationIds(
                    deviceRef =
                        "device.vexlife-prod.$deviceSuffix",
                    companionLineageRef =
                        "companion-lineage.vexlife-prod.$lineageSuffix",
                ),
            createdAt = "2026-11-04T17:30:00-08:00",
        )

    @Test
    fun firstBootstrapFormsWholeIsolatedHomeAndSecondRunPreservesIt() {
        val userHome =
            Files.createTempDirectory(
                "vexlife-prod-user-home-",
            )
                .toAbsolutePath()
        val productHome =
            VexLifeProdHomeFiles.defaultMacRoot(
                userHome,
            )
        val firstPlan =
            plan(
                "00000000-0000-0000-0000-000000000001",
                "00000000-0000-0000-0000-000000000002",
            )

        try {
            val formed =
                VexLifeProdHomeFiles.bootstrap(
                    productHome,
                    firstPlan,
                )

            assertEquals(
                "NEW_HOME_FORMED",
                formed.state,
            )
            assertTrue(
                formed.effectsPerformed,
            )
            assertTrue(
                Files.isDirectory(
                    productHome,
                ),
            )

            for (
                relative in
                firstPlan.directories
            ) {
                assertTrue(
                    Files.isDirectory(
                        productHome.resolve(
                            relative,
                        ),
                    ),
                    relative,
                )
            }

            val storedManifest =
                VexDurableContractFile.read<VexLifeProdHome.Manifest>(
                    ownedRoot = productHome,
                    path =
                        productHome.resolve(
                            VexLifeProdHome.HomeManifestRelativePath,
                        ),
                )
            assertEquals(
                firstPlan.manifest,
                storedManifest,
            )

            val secondPlan =
                plan(
                    "00000000-0000-0000-0000-000000000099",
                    "00000000-0000-0000-0000-000000000098",
                )
            val preserved =
                VexLifeProdHomeFiles.bootstrap(
                    productHome,
                    secondPlan,
                )

            assertEquals(
                "EXISTING_HOME_PRESERVED",
                preserved.state,
            )
            assertFalse(
                preserved.effectsPerformed,
            )
            assertEquals(
                firstPlan.manifest,
                preserved.manifest,
            )
            assertEquals(
                firstPlan.manifest,
                VexDurableContractFile.read<VexLifeProdHome.Manifest>(
                    ownedRoot = productHome,
                    path =
                        productHome.resolve(
                            VexLifeProdHome.HomeManifestRelativePath,
                        ),
                ),
            )
        } finally {
            userHome
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun existingDirectoryWithoutCurrentManifestHoldsWithoutInventingBootstrap() {
        val productHome =
            Files.createTempDirectory(
                "vexlife-prod-partial-home-",
            )
                .toAbsolutePath()

        try {
            assertFailsWith<IllegalArgumentException> {
                VexLifeProdHomeFiles.bootstrap(
                    productHome,
                    plan(
                        "00000000-0000-0000-0000-000000000001",
                        "00000000-0000-0000-0000-000000000002",
                    ),
                )
            }

            assertFalse(
                Files.exists(
                    productHome.resolve(
                        VexLifeProdHome.HomeManifestRelativePath,
                    ),
                ),
            )
        } finally {
            productHome
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun existingHomeWithSymlinkedRequiredDirectoryFailsClosed() {
        val parent =
            Files.createTempDirectory(
                "vexlife-prod-home-subdir-link-",
            )
                .toAbsolutePath()
        val productHome =
            parent.resolve(
                "VexLife-Prod",
            )
        val outside =
            parent.resolve(
                "outside",
            )

        try {
            Files.createDirectories(
                productHome,
            )
            Files.createDirectory(
                outside,
            )
            Files.createSymbolicLink(
                productHome.resolve(
                    "config",
                ),
                outside,
            )
            Files.createDirectory(
                productHome.resolve(
                    "runtime",
                ),
            )
            Files.createDirectory(
                productHome.resolve(
                    "recovery",
                ),
            )

            assertFailsWith<IllegalArgumentException> {
                VexLifeProdHomeFiles.bootstrap(
                    productHome,
                    plan(
                        "00000000-0000-0000-0000-000000000001",
                        "00000000-0000-0000-0000-000000000002",
                    ),
                )
            }

            assertFalse(
                Files.exists(
                    outside.resolve(
                        "home.json",
                    ),
                ),
            )
        } finally {
            Files.deleteIfExists(
                productHome.resolve(
                    "config",
                ),
            )
            parent
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun existingSymlinkNeverBecomesHomeBootstrapAuthority() {
        val parent =
            Files.createTempDirectory(
                "vexlife-prod-home-link-",
            )
                .toAbsolutePath()
        val target =
            parent.resolve(
                "real-target",
            )
        val productHome =
            parent.resolve(
                "VexLife-Prod",
            )

        try {
            Files.createDirectories(
                target,
            )
            Files.createSymbolicLink(
                productHome,
                target,
            )

            assertFailsWith<IllegalArgumentException> {
                VexLifeProdHomeFiles.bootstrap(
                    productHome,
                    plan(
                        "00000000-0000-0000-0000-000000000001",
                        "00000000-0000-0000-0000-000000000002",
                    ),
                )
            }

            assertTrue(
                Files.isSymbolicLink(
                    productHome,
                ),
            )
        } finally {
            Files.deleteIfExists(
                productHome,
            )
            parent
                .toFile()
                .deleteRecursively()
        }
    }
}

// [VXG RealForever]
