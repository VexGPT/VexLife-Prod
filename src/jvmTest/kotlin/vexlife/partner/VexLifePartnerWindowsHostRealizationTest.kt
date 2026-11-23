package vexlife.partner

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermission
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class VexLifePartnerWindowsHostRealizationTest {
    @Test
    fun packagedHelperAndFixedPowerShellCommandFormOneLaunchPlan() {
        val root =
            Files.createTempDirectory(
                "vexlife-partner-windows-host-realization-",
            )
                .toAbsolutePath()
        try {
            val powershell =
                root
                    .resolve("System32")
                    .resolve("WindowsPowerShell")
                    .resolve("v1.0")
                    .resolve("powershell.exe")
            Files.createDirectories(
                requireNotNull(
                    powershell.parent,
                ),
            )
            Files.writeString(
                powershell,
                "fixture",
            )
            runCatching {
                Files.setPosixFilePermissions(
                    powershell,
                    setOf(
                        PosixFilePermission.OWNER_READ,
                        PosixFilePermission.OWNER_WRITE,
                        PosixFilePermission.OWNER_EXECUTE,
                    ),
                )
            }

            val helper =
                root
                    .resolve("app")
                    .resolve("partner-host")
                    .resolve("vexlife-partner-host.ps1")
            Files.createDirectories(
                requireNotNull(
                    helper.parent,
                ),
            )
            Files.writeString(
                helper,
                "fixture",
            )

            if (
                Files.isExecutable(
                    powershell,
                )
            ) {
                val plan =
                    VexLifePartnerWindowsHostRealization.plan(
                        helperPathProperty =
                            helper.toString(),
                        systemRoot =
                            root.toString(),
                    )

                assertEquals(
                    powershell,
                    plan.powershellExecutable,
                )
                assertEquals(
                    helper,
                    plan.helperScript,
                )
                assertEquals(
                    listOf(
                        "-NoProfile",
                        "-NonInteractive",
                        "-ExecutionPolicy",
                        "Bypass",
                        "-File",
                        helper.toString(),
                    ),
                    plan.fixedArguments,
                )
                assertTrue(
                    plan.fixedArguments.none {
                        it.contains(
                            "provider.",
                        ) ||
                            it.contains(
                                "credential.",
                            )
                    },
                )
            }
        } finally {
            root
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun missingPackagedHelperIsRejectedBeforeProcessLaunch() {
        val root =
            Files.createTempDirectory(
                "vexlife-partner-windows-host-missing-",
            )
                .toAbsolutePath()
        try {
            val powershell =
                root
                    .resolve("System32")
                    .resolve("WindowsPowerShell")
                    .resolve("v1.0")
                    .resolve("powershell.exe")
            Files.createDirectories(
                requireNotNull(
                    powershell.parent,
                ),
            )
            Files.writeString(
                powershell,
                "fixture",
            )
            runCatching {
                Files.setPosixFilePermissions(
                    powershell,
                    setOf(
                        PosixFilePermission.OWNER_READ,
                        PosixFilePermission.OWNER_WRITE,
                        PosixFilePermission.OWNER_EXECUTE,
                    ),
                )
            }

            if (
                Files.isExecutable(
                    powershell,
                )
            ) {
                assertFailsWith<IllegalArgumentException> {
                    VexLifePartnerWindowsHostRealization.plan(
                        helperPathProperty =
                            root
                                .resolve(
                                    "missing-helper.ps1",
                                )
                                .toString(),
                        systemRoot =
                            root.toString(),
                    )
                }
            }
        } finally {
            root
                .toFile()
                .deleteRecursively()
        }
    }
}

// [VXG RealForever]
