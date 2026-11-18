package vexlife.platform

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import vexlife.platform.windows.VexLifeWindowsHostObservation

class VexLifeJvmPlatformTargetTest {
    @Test
    fun macArm64ResolvesWithoutAConsumerPlatformBranch() {
        val result =
            assertIs<VexLifeJvmPlatformTarget.Result.Resolved>(
                VexLifeJvmPlatformTarget.current(
                    osName = "Mac OS X",
                    jvmOsArch = "aarch64",
                    windowsObservation = {
                        error(
                            "Mac resolution must not consult the Windows observer",
                        )
                    },
                ),
            )

        assertEquals(
            VexLifePlatformRealization.Target(
                platform =
                    VexLifePlatformRealization.PlatformFamily.MACOS,
                architecture =
                    VexLifePlatformRealization.Architecture.ARM64,
            ),
            result.target,
        )
    }

    @Test
    fun windowsDelegatesNativeArchitectureObservationToItsRightfulOwner() {
        val target =
            VexLifePlatformRealization.Target(
                platform =
                    VexLifePlatformRealization.PlatformFamily.WINDOWS,
                architecture =
                    VexLifePlatformRealization.Architecture.X86_64,
            )
        val raw =
            VexLifeWindowsHostObservation.Raw(
                osName = "Windows 11",
                jvmOsArch = "amd64",
                processorArchitecture = "AMD64",
                processorArchitectureW6432 = null,
            )

        val result =
            assertIs<VexLifeJvmPlatformTarget.Result.Resolved>(
                VexLifeJvmPlatformTarget.current(
                    osName = raw.osName,
                    jvmOsArch = raw.jvmOsArch,
                    windowsObservation = {
                        VexLifeWindowsHostObservation.Result.Resolved(
                            raw = raw,
                            target = target,
                            nativeArchitecture =
                                VexLifePlatformRealization.Architecture.X86_64,
                            processArchitecture =
                                VexLifePlatformRealization.Architecture.X86_64,
                            architectureEvidence =
                                VexLifeWindowsHostObservation.ArchitectureEvidence.PROCESSOR_ARCHITECTURE,
                        )
                    },
                ),
            )

        assertEquals(
            target,
            result.target,
        )
    }

    @Test
    fun unsupportedJvmHostIsExplicit() {
        assertIs<VexLifeJvmPlatformTarget.Result.Unsupported>(
            VexLifeJvmPlatformTarget.current(
                osName = "Linux",
                jvmOsArch = "amd64",
                windowsObservation = {
                    error(
                        "unsupported non-Windows host must not consult Windows",
                    )
                },
            ),
        )
    }
}

// [VXG RealForever]
