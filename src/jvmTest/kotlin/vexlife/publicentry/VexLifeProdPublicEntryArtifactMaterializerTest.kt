package vexlife.publicentry

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import vexlife.contract.VexContractJson
import vexlife.model.heretic.runtime.HereticMacHostProfile

class VexLifeProdPublicEntryArtifactMaterializerTest {
    @Test
    fun materializerConsumesReleaseContractAndFailsClosedOnDrift() {
        val root =
            Files.createTempDirectory(
                "vexlife-public-entry-test-",
            )
        val contractPath =
            root.resolve(
                "release-contract.json",
            )
        val output =
            root.resolve(
                "site",
            )

        try {
            val contract =
                VexLifeProdPublicEntryArtifactMaterializer.ReleaseContract(
                    schemaVersion =
                        "vexlife-prod.private-service-release/v1",
                    repository =
                        "VexGPT/VexLife-Prod",
                    platform =
                        "darwin-arm64",
                    currentChannel =
                        "github-release-latest",
                    releaseTagPrefix =
                        "private-service-image-",
                    archiveNamePrefix =
                        "vexlife-prod-service-image-darwin-arm64-",
                    archiveSuffix =
                        ".tar.gz",
                    receiptSuffix =
                        ".receipt.json",
                    bootstrapNamePrefix =
                        "vexlife-prod-bootstrap-darwin-arm64-",
                    bootstrapSuffix =
                        ".command",
                    currentBootstrapAssetName =
                        "vexlife-prod-bootstrap-darwin-arm64.command",
                    currentBootstrapSha256AssetName =
                        "vexlife-prod-bootstrap-darwin-arm64.command.sha256",
                    bootstrapTemplateRelativePath =
                        "packaging/bootstrap/vexlife-prod-bootstrap.command.template",
                    setupWindowRelativePath =
                        "setup/vexlife-prod-setup-window.applescript",
                    setupLauncherRelativePath =
                        "bin/vexlife-prod-setup",
                    serviceLauncherRelativePath =
                        "bin/vexlife-prod-service",
                    imageManifestRelativePath =
                        "service-image.json",
                )

            Files.writeString(
                contractPath,
                VexContractJson.encode(
                    contract,
                ),
                StandardCharsets.UTF_8,
            )

            val projection =
                VexLifeProdPublicEntryArtifactMaterializer.projectionFrom(
                    contractPath,
                )

            assertEquals(
                HereticMacHostProfile.RequiredChipModel,
                projection.hostProfile.chipModel,
            )
            assertEquals(
                HereticMacHostProfile.MinimumMemoryBytes,
                projection.hostProfile.minimumMemoryBytes,
            )

            VexLifeProdPublicEntryArtifactMaterializer.run(
                mode =
                    VexLifeProdPublicEntryArtifactMaterializer.Mode.Write,
                root = output,
                releaseContractPath = contractPath,
            )

            assertTrue(
                Files.exists(
                    output.resolve(
                        "index.html",
                    ),
                ),
            )
            assertTrue(
                Files.exists(
                    output.resolve(
                        "public-entry.json",
                    ),
                ),
            )

            VexLifeProdPublicEntryArtifactMaterializer.run(
                mode =
                    VexLifeProdPublicEntryArtifactMaterializer.Mode.Verify,
                root = output,
                releaseContractPath = contractPath,
            )

            Files.writeString(
                output.resolve(
                    "vexlife.css",
                ),
                "drift",
                StandardCharsets.UTF_8,
            )

            assertFailsWith<IllegalArgumentException> {
                VexLifeProdPublicEntryArtifactMaterializer.run(
                    mode =
                        VexLifeProdPublicEntryArtifactMaterializer.Mode.Verify,
                    root = output,
                    releaseContractPath = contractPath,
                )
            }

            Files.delete(
                output.resolve(
                    "vexlife.css",
                ),
            )
            VexLifeProdPublicEntryArtifactMaterializer.run(
                mode =
                    VexLifeProdPublicEntryArtifactMaterializer.Mode.Write,
                root = output,
                releaseContractPath = contractPath,
            )
            Files.writeString(
                output.resolve(
                    "extra.txt",
                ),
                "unowned",
                StandardCharsets.UTF_8,
            )

            assertFailsWith<IllegalArgumentException> {
                VexLifeProdPublicEntryArtifactMaterializer.run(
                    mode =
                        VexLifeProdPublicEntryArtifactMaterializer.Mode.Verify,
                    root = output,
                    releaseContractPath = contractPath,
                )
            }
        } finally {
            root.toFile()
                .deleteRecursively()
        }
    }
}

// [VXG RealForever]
