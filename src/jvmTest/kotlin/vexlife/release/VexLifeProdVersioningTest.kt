package vexlife.release

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse

/** [VexLife Prod versioning] Release identity must survive into the Mac distribution. */
class VexLifeProdVersioningTest {
    private val root: Path = Path.of("").toAbsolutePath()
    private val releaseContract =
        Files.readString(root.resolve("packaging/macos-app/release-contract.json"))
    private val buildSource = Files.readString(root.resolve("build.gradle.kts"))
    private val candidateProof =
        Files.readString(root.resolve("tools/release/prove-mac-candidate.sh"))
    private val releasePublisher =
        Files.readString(root.resolve("tools/release/publish-mac-release.sh"))
    private val releaseDocs = Files.readString(root.resolve("docs/release/RELEASES.md"))
    private val macProofWorkflow =
        Files.readString(root.resolve(".github/workflows/mac-app-proof.yml"))
    private val releaseWorkflow =
        Files.readString(root.resolve(".github/workflows/release.yml"))
    private val pendingPreviewPublisher =
        Files.readString(root.resolve("tools/release/publish-pending-previews.sh"))

    @Test
    fun distributionIdentityIsVersionedWhileInstalledProductIdentityRemainsCanonical() {
        assertContains(releaseContract, "\"schemaVersion\": \"vexlife-prod.macos-application-release/v2\"")
        assertContains(
            releaseContract,
            "\"candidateDiskImageNamePattern\": \"VexLife-Candidate-{sourceShort}-macOS-AppleSilicon.dmg\"",
        )
        assertContains(
            releaseContract,
            "\"previewDiskImageNamePattern\": \"VexLife-Preview-{sourceShort}-macOS-AppleSilicon.dmg\"",
        )
        assertContains(
            releaseContract,
            "\"stableDiskImageNamePattern\": \"VexLife-{version}-macOS-AppleSilicon.dmg\"",
        )
        assertContains(releaseContract, "\"applicationName\": \"VexLife.app\"")
        assertContains(releaseContract, "\"bundleIdentifier\": \"com.vexlife.prod\"")
        assertFalse(releaseContract.contains("VexLife Preview.app"))
    }

    @Test
    fun mountedVolumeCarriesTheSameReleaseIdentityAsTheDownloadedDistribution() {
        assertContains(
            releaseContract,
            "\"candidateDiskImageVolumeNamePattern\": \"VexLife Candidate {sourceShort}\"",
        )
        assertContains(
            releaseContract,
            "\"previewDiskImageVolumeNamePattern\": \"VexLife Preview {sourceShort}\"",
        )
        assertContains(
            releaseContract,
            "\"stableDiskImageVolumeNamePattern\": \"VexLife {version}\"",
        )
        assertContains(buildSource, "macApplicationDiskImageVolumeName")
        assertContains(candidateProof, "MOUNTED_DISTRIBUTION_VOLUME_IDENTITY")
        assertContains(candidateProof, "VolumeName")
    }

    @Test
    fun publicationCarriesPreviewOrStableIdentityThroughTheCanonicalCandidateProof() {
        assertContains(
            releasePublisher,
            "prove-mac-candidate.sh\" \"\$APP_VERSION\" \"\$CHANNEL",
        )
        assertContains(
            releasePublisher,
            "VexLife-Preview-\$SOURCE_SHORT-macOS-AppleSilicon.dmg",
        )
        assertContains(releasePublisher, "VexLife-\$VERSION-macOS-AppleSilicon.dmg")
        assertContains(candidateProof, "-PvexlifeDistributionChannel=\"\$DISTRIBUTION_CHANNEL\"")
        assertContains(
            macProofWorkflow,
            "VexLife-Candidate-*-macOS-AppleSilicon.dmg",
        )
    }

    @Test
    fun everyFirstParentMainCommitHasACatchUpSafeRelayPreviewVersion() {
        assertFalse(releaseWorkflow.contains("push:"))
        assertContains(
            pendingPreviewPublisher,
            "VEXLIFE_PREVIEW_BASELINE:-13606a471543ce9d9e4fecd5fb471971b581d6db",
        )
        assertContains(
            pendingPreviewPublisher,
            "rev-list --first-parent --reverse \"\$BASELINE..\$REMOTE_MAIN\"",
        )
        assertContains(
            pendingPreviewPublisher,
            "gh release view \"\$tag\"",
        )
        assertContains(
            pendingPreviewPublisher,
            "publish-mac-release.sh preview \"\" \"\$source_sha\"",
        )
        assertContains(pendingPreviewPublisher, "VEXLIFE_PREVIEW_QUEUE_ALREADY_PUBLISHED")
        assertContains(pendingPreviewPublisher, "VEXLIFE_PREVIEW_QUEUE_PUBLISHED")
        assertContains(pendingPreviewPublisher, "VEXLIFE_PREVIEW_QUEUE_FAILED")
        assertContains(releaseDocs, "MAIN SHA IDENTITY = DURABLE PREVIEW VERSION")
        assertContains(releaseDocs, "no Local Workspace dependency")
    }

    @Test
    fun productReceiptRecordsDistributionIdentityWithoutForkingProductIdentity() {
        assertContains(
            buildSource,
            "vexlife-prod.macos-application-archive-receipt/v2",
        )
        assertContains(buildSource, "\\\"distributionChannel\\\"")
        assertContains(buildSource, "\\\"distributionIdentity\\\"")
        assertContains(buildSource, "\\\"diskImageVolumeName\\\"")
        assertContains(buildSource, "VexLifeProdDistributionChannel")
        assertContains(buildSource, "VexLifeProdDistributionIdentity")
        assertContains(candidateProof, "APP_DISTRIBUTION_IDENTITY")
        assertContains(releaseDocs, "VERSIONED DISTRIBUTION != SECOND PRODUCT")
        assertContains(releaseDocs, "VexLife.app + com.vexlife.prod")
    }
}
