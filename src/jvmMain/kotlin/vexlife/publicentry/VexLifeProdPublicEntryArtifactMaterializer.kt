package vexlife.publicentry

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import kotlinx.serialization.Serializable
import vexlife.artifact.ExactGeneratedArtifactMaterializer
import vexlife.contract.VexContractJson
import vexlife.model.heretic.runtime.HereticMacHostProfile

internal object VexLifeProdPublicEntryArtifactMaterializer {
    enum class Mode {
        Write,
        Verify,
    }

    @Serializable
    internal data class ReleaseContract(
        val schemaVersion: String,
        val repository: String,
        val platform: String,
        val currentChannel: String,
        val releaseTagPrefix: String,
        val archiveNamePrefix: String,
        val archiveSuffix: String,
        val receiptSuffix: String,
        val bootstrapNamePrefix: String,
        val bootstrapSuffix: String,
        val currentBootstrapAssetName: String,
        val currentBootstrapSha256AssetName: String,
        val bootstrapTemplateRelativePath: String,
        val setupWindowRelativePath: String,
        val setupLauncherRelativePath: String,
        val serviceLauncherRelativePath: String,
        val imageManifestRelativePath: String,
    )

    fun run(
        mode: Mode,
        root: Path,
        releaseContractPath: Path,
    ) {
        val projection =
            projectionFrom(
                releaseContractPath,
            )
        val bundle =
            VexLifeProdPublicEntryArtifacts.generate(
                projection,
            )

        ExactGeneratedArtifactMaterializer.run(
            mode =
                when (
                    mode
                ) {
                    Mode.Write ->
                        ExactGeneratedArtifactMaterializer.Mode.Write

                    Mode.Verify ->
                        ExactGeneratedArtifactMaterializer.Mode.Verify
                },
            root = root,
            contentByPath =
                bundle.paths
                    .associateWith(
                        bundle::content,
                    ),
        )
    }

    internal fun projectionFrom(
        releaseContractPath: Path,
    ): VexLifeProdPublicEntry.Projection {
        val path =
            releaseContractPath
                .toAbsolutePath()
                .normalize()

        require(
            Files.isRegularFile(
                path,
                LinkOption.NOFOLLOW_LINKS,
            ) &&
                !Files.isSymbolicLink(
                    path,
                ),
        ) {
            "VexLife public entry requires one regular source-owned release contract"
        }

        val releaseContract =
            VexContractJson.decode<ReleaseContract>(
                Files.readString(
                    path,
                    StandardCharsets.UTF_8,
                ),
            )

        val release =
            VexLifeProdPublicEntry.ReleaseCoordinate(
                schemaVersion =
                    releaseContract.schemaVersion,
                repository =
                    releaseContract.repository,
                platform =
                    releaseContract.platform,
                currentChannel =
                    releaseContract.currentChannel,
                currentBootstrapAssetName =
                    releaseContract.currentBootstrapAssetName,
                currentBootstrapSha256AssetName =
                    releaseContract.currentBootstrapSha256AssetName,
            )

        val hostProfile =
            VexLifeProdPublicEntry.HostProfile(
                platform =
                    HereticMacHostProfile.RequiredPlatform,
                architecture =
                    HereticMacHostProfile.RequiredArchitecture,
                chipModel =
                    HereticMacHostProfile.RequiredChipModel,
                minimumMemoryBytes =
                    HereticMacHostProfile.MinimumMemoryBytes,
            )

        return VexLifeProdPublicEntry.project(
            release = release,
            hostProfile = hostProfile,
        )
    }
}

fun main(
    args: Array<String>,
) {
    require(
        args.size ==
            3,
    ) {
        "usage: <write|verify> <output-directory> <release-contract>"
    }

    val mode =
        when (
            args[
                0
            ]
        ) {
            "write" ->
                VexLifeProdPublicEntryArtifactMaterializer.Mode.Write

            "verify" ->
                VexLifeProdPublicEntryArtifactMaterializer.Mode.Verify

            else ->
                error(
                    "unknown VexLife public-entry materializer mode: " +
                        args[
                            0
                        ],
                )
        }

    VexLifeProdPublicEntryArtifactMaterializer.run(
        mode = mode,
        root =
            Path.of(
                args[
                    1
                ],
            ),
        releaseContractPath =
            Path.of(
                args[
                    2
                ],
            ),
    )
}

// [VXG RealForever]
