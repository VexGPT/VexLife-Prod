package vexlife.platform

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class VexLifeWindowsDownloadRouteTest {
    private val x64Target =
        VexLifePlatformRealization.Target(
            platform = VexLifePlatformRealization.PlatformFamily.WINDOWS,
            architecture = VexLifePlatformRealization.Architecture.X86_64,
        )

    private val arm64Target =
        VexLifePlatformRealization.Target(
            platform = VexLifePlatformRealization.PlatformFamily.WINDOWS,
            architecture = VexLifePlatformRealization.Architecture.ARM64,
        )

    private val x64Artifact =
        VexLifeWindowsDownloadRoute.Artifact(
            artifactRef = "artifact.vexlife-prod.windows.x86-64.test",
            target = x64Target,
            assetName = "VexLife-Windows-x64.exe",
            sha256 = "1".repeat(64),
        )

    private val arm64Artifact =
        VexLifeWindowsDownloadRoute.Artifact(
            artifactRef = "artifact.vexlife-prod.windows.arm64.test",
            target = arm64Target,
            assetName = "VexLife-Windows-arm64.exe",
            sha256 = "2".repeat(64),
        )

    @Test
    fun x64TargetSelectsOnlyTheX64Artifact() {
        val resolved =
            assertIs<VexLifeWindowsDownloadRoute.Resolution.Available>(
                VexLifeWindowsDownloadRoute.Catalog(
                    listOf(
                        x64Artifact,
                        arm64Artifact,
                    ),
                ).resolve(x64Target),
            )

        assertEquals(x64Artifact, resolved.artifact)
    }

    @Test
    fun arm64TargetSelectsOnlyTheArm64Artifact() {
        val resolved =
            assertIs<VexLifeWindowsDownloadRoute.Resolution.Available>(
                VexLifeWindowsDownloadRoute.Catalog(
                    listOf(
                        x64Artifact,
                        arm64Artifact,
                    ),
                ).resolve(arm64Target),
            )

        assertEquals(arm64Artifact, resolved.artifact)
    }

    @Test
    fun missingArm64ArtifactNeverBorrowsX64() {
        assertIs<VexLifeWindowsDownloadRoute.Resolution.Unavailable>(
            VexLifeWindowsDownloadRoute.Catalog(
                listOf(
                    x64Artifact,
                ),
            ).resolve(arm64Target),
        )
    }

    @Test
    fun nonWindowsTargetIsRejectedAtTheWindowsRoutingBoundary() {
        assertFailsWith<IllegalArgumentException> {
            VexLifeWindowsDownloadRoute.Catalog(
                listOf(
                    x64Artifact,
                ),
            ).resolve(
                VexLifePlatformRealization.Target(
                    platform = VexLifePlatformRealization.PlatformFamily.MACOS,
                    architecture = VexLifePlatformRealization.Architecture.ARM64,
                ),
            )
        }
    }

    @Test
    fun duplicateTargetCannotCreateAmbiguousDownloadSelection() {
        assertFailsWith<IllegalArgumentException> {
            VexLifeWindowsDownloadRoute.Catalog(
                listOf(
                    x64Artifact,
                    x64Artifact.copy(
                        artifactRef = "artifact.vexlife-prod.windows.x86-64.other",
                        assetName = "Other-x64.exe",
                        sha256 = "3".repeat(64),
                    ),
                ),
            )
        }
    }
}

// [VXG RealForever]
