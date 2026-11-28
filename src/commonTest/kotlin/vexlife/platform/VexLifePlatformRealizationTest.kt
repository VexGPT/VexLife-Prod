package vexlife.platform

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class VexLifePlatformRealizationTest {
    private val macArm64 =
        VexLifePlatformRealization.Entry(
            realizationRef = "realization.test.macos.arm64.001",
            target =
                VexLifePlatformRealization.Target(
                    platform = VexLifePlatformRealization.PlatformFamily.MACOS,
                    architecture = VexLifePlatformRealization.Architecture.ARM64,
                ),
            dependencies =
                VexLifePlatformRealization.DependencyRefs(
                    hostExecutionProfileRef = "profile.test.macos-arm64",
                    modelRuntimeAdapterRef = "adapter.test.macos-arm64",
                    modelRuntimeClass = "TEST_RUNTIME",
                    modelTransportClass = "TEST_TRANSPORT",
                    processSubstrateRef = "substrate.test.macos-arm64",
                    privateServiceImagePlatform = "darwin-arm64",
                ),
        )

    @Test
    fun catalogResolvesOnlyTheExactTargetAndNeverFallsBack() {
        val catalog =
            VexLifePlatformRealization.Catalog(
                listOf(
                    macArm64,
                ),
            )

        val supported =
            assertIs<VexLifePlatformRealization.Resolution.Supported>(
                catalog.resolve(
                    VexLifePlatformRealization.Target(
                        platform = VexLifePlatformRealization.PlatformFamily.MACOS,
                        architecture = VexLifePlatformRealization.Architecture.ARM64,
                    ),
                ),
            )
        assertEquals(macArm64, supported.entry)

        assertIs<VexLifePlatformRealization.Resolution.Unsupported>(
            catalog.resolve(
                VexLifePlatformRealization.Target(
                    platform = VexLifePlatformRealization.PlatformFamily.MACOS,
                    architecture = VexLifePlatformRealization.Architecture.X86_64,
                ),
            ),
        )
        assertIs<VexLifePlatformRealization.Resolution.Unsupported>(
            catalog.resolve(
                VexLifePlatformRealization.Target(
                    platform = VexLifePlatformRealization.PlatformFamily.WINDOWS,
                    architecture = VexLifePlatformRealization.Architecture.X86_64,
                ),
            ),
        )
    }

    @Test
    fun catalogRejectsTwoRealizationsClaimingTheSameExactTarget() {
        assertFailsWith<IllegalArgumentException> {
            VexLifePlatformRealization.Catalog(
                listOf(
                    macArm64,
                    macArm64.copy(
                        realizationRef = "realization.test.macos.arm64.002",
                    ),
                ),
            )
        }
    }
}

// [VXG RealForever]
