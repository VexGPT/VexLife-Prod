package vexlife.home

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VexLifeProdHomeFilesTest {
    @Test
    fun defaultMacRootRemainsTheCanonicalVexLifeHome() {
        val userHome =
            Files.createTempDirectory(
                "vexlife-user-home-",
            )
                .toAbsolutePath()

        try {
            assertEquals(
                userHome.resolve(
                    ".vexlife",
                ),
                VexLifeProdHomeFiles.defaultMacRoot(
                    userHome,
                ),
            )
        } finally {
            userHome
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun existingCanonicalHomeAndLineageAreObservedWithoutMutation() {
        val parent =
            Files.createTempDirectory(
                "vexlife-canonical-home-",
            )
                .toAbsolutePath()
        val home =
            parent.resolve(
                ".vexlife",
            )

        try {
            val fixture =
                CanonicalVexLifeHomeFixture.form(
                    home,
                )
            val manifestBefore =
                Files.readString(
                    home.resolve(
                        "config/home.json",
                    ),
                )
            val deviceBefore =
                Files.readString(
                    home.resolve(
                        "devices/${fixture.installation.deviceRef}.json",
                    ),
                )
            val modelBefore =
                Files.readString(
                    home.resolve(
                        "config/model.json",
                    ),
                )

            val observed =
                requireNotNull(
                    VexLifeProdHomeFiles.observeExisting(
                        home,
                    ),
                )

            assertEquals(
                "EXISTING_CANONICAL_VEXLIFE_HOME_BOUND",
                observed.state,
            )
            assertFalse(
                observed.effectsPerformed,
            )
            assertEquals(
                fixture.manifest,
                observed.manifest,
            )
            assertEquals(
                fixture.installation,
                observed.installation,
            )
            assertEquals(
                manifestBefore,
                Files.readString(
                    home.resolve(
                        "config/home.json",
                    ),
                ),
            )
            assertEquals(
                deviceBefore,
                Files.readString(
                    home.resolve(
                        "devices/${fixture.installation.deviceRef}.json",
                    ),
                ),
            )
            assertEquals(
                modelBefore,
                Files.readString(
                    home.resolve(
                        "config/model.json",
                    ),
                ),
            )
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun absentCanonicalHomeFormsExactIdentityWithoutParallelNamespace() {
        val parent =
            Files.createTempDirectory(
                "vexlife-home-absent-",
            )
                .toAbsolutePath()
        val home =
            parent.resolve(
                ".vexlife",
            )
        val plan =
            VexLifeProdHome.plan(
                personRef = "person.local-user",
                familyRef = "vex-family.local-user",
                deviceName = "Test Mac",
                platform = "darwin",
                architecture = "arm64",
                ids =
                    VexLifeProdHome.InstallationIds(
                        deviceRef =
                            "device.vexlife.00000000-0000-0000-0000-000000000001",
                        companionLineageRef =
                            "companion-lineage.vexlife.00000000-0000-0000-0000-000000000002",
                    ),
                createdAt = "2026-11-04T17:30:00-08:00",
            )

        try {
            val formed =
                VexLifeProdHomeFiles.bootstrap(
                    home,
                    plan,
                )

            assertEquals(
                "FRESH_CANONICAL_VEXLIFE_HOME_FORMED",
                formed.state,
            )
            assertTrue(
                formed.effectsPerformed,
            )
            assertEquals(
                plan.manifest,
                formed.manifest,
            )
            assertEquals(
                plan.installation,
                formed.installation,
            )
            assertTrue(
                Files.isRegularFile(
                    home.resolve(
                        VexLifeProdHome.CanonicalModelConfigurationRelativePath,
                    ),
                ),
            )
            assertTrue(
                Files.isRegularFile(
                    home.resolve(
                        VexLifeProdHome.ModelConfigurationRelativePath,
                    ),
                ),
            )
            assertTrue(
                Files.isRegularFile(
                    home.resolve(
                        VexLifeProdHome.BootstrapReceiptRelativePath,
                    ),
                ),
            )
            assertTrue(
                Files.readString(
                    home.resolve(
                        VexLifeProdHome.CanonicalModelConfigurationRelativePath,
                    ),
                )
                    .contains(
                        VexLifeProdHome.CanonicalModelConfigurationSchema,
                    ),
            )
            assertFalse(
                formed.manifest.homeRef.startsWith(
                    "vex-home.prod.",
                ),
            )

            val rerun =
                VexLifeProdHomeFiles.bootstrap(
                    home,
                    plan,
                )
            assertEquals(
                "EXISTING_CANONICAL_VEXLIFE_HOME_BOUND",
                rerun.state,
            )
            assertFalse(
                rerun.effectsPerformed,
            )
            assertEquals(
                formed.manifest,
                rerun.manifest,
            )
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun mismatchedCurrentDeviceAndCompanionLineageFailClosed() {
        val parent =
            Files.createTempDirectory(
                "vexlife-home-identity-mismatch-",
            )
                .toAbsolutePath()
        val home =
            parent.resolve(
                ".vexlife",
            )

        try {
            val fixture =
                CanonicalVexLifeHomeFixture.form(
                    home,
                )
            val devicePath =
                home.resolve(
                    "devices/${fixture.installation.deviceRef}.json",
                )
            Files.writeString(
                devicePath,
                Files.readString(
                    devicePath,
                )
                    .replace(
                        fixture.installation.companionLineageRef,
                        "companion-lineage.vexlife.00000000-0000-0000-0000-000000000099",
                    ),
            )

            assertFailsWith<IllegalArgumentException> {
                VexLifeProdHomeFiles.observeExisting(
                    home,
                )
            }
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun existingSymlinkNeverBecomesCanonicalHomeAuthority() {
        val parent =
            Files.createTempDirectory(
                "vexlife-home-link-",
            )
                .toAbsolutePath()
        val target =
            parent.resolve(
                "real-target",
            )
        val home =
            parent.resolve(
                ".vexlife",
            )

        try {
            Files.createDirectories(
                target,
            )
            Files.createSymbolicLink(
                home,
                target,
            )

            assertFailsWith<IllegalArgumentException> {
                VexLifeProdHomeFiles.observeExisting(
                    home,
                )
            }

            assertTrue(
                Files.isSymbolicLink(
                    home,
                ),
            )
        } finally {
            Files.deleteIfExists(
                home,
            )
            parent
                .toFile()
                .deleteRecursively()
        }
    }
}

// [VXG RealForever]
