package vexlife.platform.windows

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import vexlife.platform.VexLifePlatformRealization

class VexLifeWindowsHostObservationTest {
    @Test
    fun x64WindowsResolvesToExactWindowsX64Target() {
        val resolved =
            assertIs<VexLifeWindowsHostObservation.Result.Resolved>(
                VexLifeWindowsHostObservation.resolve(
                    VexLifeWindowsHostObservation.Raw(
                        osName = "Windows 11",
                        jvmOsArch = "amd64",
                        processorArchitecture = "AMD64",
                        processorArchitectureW6432 = null,
                    ),
                ),
            )

        assertEquals(
            VexLifePlatformRealization.Target(
                platform = VexLifePlatformRealization.PlatformFamily.WINDOWS,
                architecture = VexLifePlatformRealization.Architecture.X86_64,
            ),
            resolved.target,
        )
        assertEquals(
            VexLifeWindowsHostObservation.ArchitectureEvidence.PROCESSOR_ARCHITECTURE,
            resolved.architectureEvidence,
        )
    }

    @Test
    fun arm64WindowsResolvesToExactWindowsArm64Target() {
        val resolved =
            assertIs<VexLifeWindowsHostObservation.Result.Resolved>(
                VexLifeWindowsHostObservation.resolve(
                    VexLifeWindowsHostObservation.Raw(
                        osName = "Windows 11",
                        jvmOsArch = "aarch64",
                        processorArchitecture = "ARM64",
                        processorArchitectureW6432 = null,
                    ),
                ),
            )

        assertEquals(VexLifePlatformRealization.Architecture.ARM64, resolved.nativeArchitecture)
        assertEquals(
            VexLifePlatformRealization.Target(
                platform = VexLifePlatformRealization.PlatformFamily.WINDOWS,
                architecture = VexLifePlatformRealization.Architecture.ARM64,
            ),
            resolved.target,
        )
    }

    @Test
    fun emulatedX64ProcessOnArm64WindowsRoutesByNativeOsArchitecture() {
        val resolved =
            assertIs<VexLifeWindowsHostObservation.Result.Resolved>(
                VexLifeWindowsHostObservation.resolve(
                    VexLifeWindowsHostObservation.Raw(
                        osName = "Windows 11",
                        jvmOsArch = "amd64",
                        processorArchitecture = "AMD64",
                        processorArchitectureW6432 = "ARM64",
                    ),
                ),
            )

        assertEquals(VexLifePlatformRealization.Architecture.ARM64, resolved.nativeArchitecture)
        assertEquals(VexLifePlatformRealization.Architecture.X86_64, resolved.processArchitecture)
        assertEquals(
            VexLifeWindowsHostObservation.ArchitectureEvidence.PROCESSOR_ARCHITEW6432,
            resolved.architectureEvidence,
        )
    }

    @Test
    fun unsupportedWindowsArchitectureDoesNotGuess() {
        assertIs<VexLifeWindowsHostObservation.Result.UnsupportedArchitecture>(
            VexLifeWindowsHostObservation.resolve(
                VexLifeWindowsHostObservation.Raw(
                    osName = "Windows 10",
                    jvmOsArch = "x86",
                    processorArchitecture = "x86",
                    processorArchitectureW6432 = null,
                ),
            ),
        )
    }

    @Test
    fun nonWindowsHostDoesNotBecomeAWindowsTarget() {
        assertIs<VexLifeWindowsHostObservation.Result.UnsupportedHost>(
            VexLifeWindowsHostObservation.resolve(
                VexLifeWindowsHostObservation.Raw(
                    osName = "Mac OS X",
                    jvmOsArch = "aarch64",
                    processorArchitecture = null,
                    processorArchitectureW6432 = null,
                ),
            ),
        )
    }
}

// [VXG RealForever]
