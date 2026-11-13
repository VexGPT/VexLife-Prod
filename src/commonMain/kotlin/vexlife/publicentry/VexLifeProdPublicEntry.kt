package vexlife.publicentry

import kotlinx.serialization.Serializable
import vexlife.entry.VexLifeProdEntryFamily

/**
 * Read-only public-entry projection for the VexLife Product.
 *
 * This object does not own Product-entry semantics, release publication, local Product state, or
 * GitHub Pages deployment. It composes already-owned truth into the smallest stable public doorway:
 *
 * Product-entry Family
 * + source-owned release coordinate
 * + accepted host profile
 * -> deterministic public explanation / bootstrap coordinate
 */
object VexLifeProdPublicEntry {
    const val Schema =
        "vexlife-prod.public-entry/v1"

    const val PageRef =
        "page.vexlife.public-entry"

    const val StableHumanDoorway =
        "https://vgong24.github.io/VexLife/"

    const val CanonicalProductPage =
        "https://vexgpt.github.io/VexLife-Prod/"

    const val VextremeHome =
        "https://vgong24.github.io/Vextreme/pages/terrain-map.html"

    const val VexWorkHome =
        "https://vexgpt.github.io/VexWork/"

    private const val PublicProjectionSourceSha =
        "0000000000000000000000000000000000000000"

    @Serializable
    data class ReleaseCoordinate(
        val schemaVersion: String,
        val repository: String,
        val platform: String,
        val currentChannel: String,
        val currentBootstrapAssetName: String,
        val currentBootstrapSha256AssetName: String,
    ) {
        init {
            require(
                schemaVersion ==
                    "vexlife-prod.private-service-release/v1" &&
                    repository ==
                    "VexGPT/VexLife-Prod" &&
                    platform ==
                    "darwin-arm64" &&
                    currentChannel ==
                    "github-release-latest" &&
                    currentBootstrapAssetName.isNotBlank() &&
                    currentBootstrapSha256AssetName ==
                    currentBootstrapAssetName +
                    ".sha256",
            ) {
                "public entry requires the accepted VexLife-Prod current release coordinate"
            }
        }
    }

    @Serializable
    data class HostProfile(
        val platform: String,
        val architecture: String,
        val chipModel: String,
        val minimumMemoryBytes: Long,
    ) {
        init {
            require(
                platform ==
                    "darwin" &&
                    architecture ==
                    "arm64" &&
                    chipModel.isNotBlank() &&
                    minimumMemoryBytes >
                    0,
            ) {
                "public entry host profile is invalid"
            }
        }
    }

    @Serializable
    data class FamilyWorld(
        val label: String,
        val href: String,
        val current: Boolean,
    )

    @Serializable
    data class EntryOption(
        val stateRef: String,
        val title: String,
        val detail: String,
        val primaryLabel: String?,
        val secondaryLabel: String?,
        val cancelLabel: String,
    )

    @Serializable
    data class Projection(
        val schemaVersion: String,
        val pageRef: String,
        val title: String,
        val tagline: String,
        val stableHumanDoorway: String,
        val canonicalProductPage: String,
        val familyWorlds: List<FamilyWorld>,
        val pageProductEffectClass: String,
        val executionAuthority: String,
        val releaseCoordinate: ReleaseCoordinate,
        val currentBootstrapUrl: String,
        val currentBootstrapSha256Url: String,
        val bootstrapCommand: List<String>,
        val hostProfile: HostProfile,
        val entryOptions: List<EntryOption>,
        val effectsPerformed: Boolean,
    ) {
        init {
            require(
                schemaVersion ==
                    Schema &&
                    pageRef ==
                    PageRef &&
                    title.isNotBlank() &&
                    tagline.isNotBlank() &&
                    stableHumanDoorway ==
                    StableHumanDoorway &&
                    canonicalProductPage ==
                    CanonicalProductPage &&
                    familyWorlds.count {
                        it.current
                    } ==
                    1 &&
                    pageProductEffectClass ==
                    "NONE" &&
                    executionAuthority ==
                    "NONE" &&
                    bootstrapCommand.isNotEmpty() &&
                    entryOptions.map {
                        it.stateRef
                    }
                        .toSet()
                        .size ==
                    entryOptions.size &&
                    !effectsPerformed,
            ) {
                "VexLife public-entry projection is invalid"
            }
        }
    }

    fun project(
        release: ReleaseCoordinate,
        hostProfile: HostProfile,
    ): Projection {
        val bootstrapUrl =
            "https://github.com/" +
                release.repository +
                "/releases/latest/download/" +
                release.currentBootstrapAssetName
        val bootstrapSha256Url =
            "https://github.com/" +
                release.repository +
                "/releases/latest/download/" +
                release.currentBootstrapSha256AssetName

        return Projection(
            schemaVersion = Schema,
            pageRef = PageRef,
            title = "VexLife",
            tagline =
                "Your local AI companion, built to continue with you.",
            stableHumanDoorway = StableHumanDoorway,
            canonicalProductPage = CanonicalProductPage,
            familyWorlds =
                listOf(
                    FamilyWorld(
                        label = "Vextreme",
                        href = VextremeHome,
                        current = false,
                    ),
                    FamilyWorld(
                        label = "VexLife",
                        href = CanonicalProductPage,
                        current = true,
                    ),
                    FamilyWorld(
                        label = "VexWork",
                        href = VexWorkHome,
                        current = false,
                    ),
                ),
            pageProductEffectClass = "NONE",
            executionAuthority = "NONE",
            releaseCoordinate = release,
            currentBootstrapUrl = bootstrapUrl,
            currentBootstrapSha256Url = bootstrapSha256Url,
            bootstrapCommand =
                bootstrapCommand(
                    release = release,
                    bootstrapUrl = bootstrapUrl,
                    bootstrapSha256Url = bootstrapSha256Url,
                ),
            hostProfile = hostProfile,
            entryOptions =
                entryStates()
                    .map {
                        state ->
                        val display =
                            VexLifeProdEntryFamily.Semantics
                                .displayStateFor(
                                    state,
                                )
                        EntryOption(
                            stateRef = state.stateRef,
                            title = display.title,
                            detail = display.detail,
                            primaryLabel = display.primaryLabel,
                            secondaryLabel = display.secondaryLabel,
                            cancelLabel = display.cancelLabel,
                        )
                    },
            effectsPerformed = false,
        )
    }

    private fun entryStates(): List<VexLifeProdEntryFamily.EntryState> =
        listOf(
            VexLifeProdEntryFamily.Current(
                PublicProjectionSourceSha,
            ),
            VexLifeProdEntryFamily.UpdateAvailable(
                PublicProjectionSourceSha,
            ),
            VexLifeProdEntryFamily.RepairRequired(
                PublicProjectionSourceSha,
            ),
            VexLifeProdEntryFamily.UpdateRepairRequired(
                PublicProjectionSourceSha,
            ),
            VexLifeProdEntryFamily.SetupRequired(
                PublicProjectionSourceSha,
            ),
            VexLifeProdEntryFamily.HeldUnknown(
                desiredSourceSha =
                    PublicProjectionSourceSha,
                detail =
                    "Public explanation only; no host ownership observation is performed here.",
            ),
        )

    private fun bootstrapCommand(
        release: ReleaseCoordinate,
        bootstrapUrl: String,
        bootstrapSha256Url: String,
    ): List<String> =
        listOf(
            "set -euo pipefail",
            "TMP_ROOT=\"\$(/usr/bin/mktemp -d \"\${TMPDIR:-/tmp}/vexlife-prod-entry.XXXXXX\")\"",
            "trap '/bin/rm -rf -- \"\$TMP_ROOT\"' EXIT HUP INT TERM",
            "cd \"\$TMP_ROOT\"",
            "/usr/bin/curl --fail --location --proto '=https' --tlsv1.2 --output " +
                release.currentBootstrapAssetName +
                " \"" +
                bootstrapUrl +
                "\"",
            "/usr/bin/curl --fail --location --proto '=https' --tlsv1.2 --output " +
                release.currentBootstrapSha256AssetName +
                " \"" +
                bootstrapSha256Url +
                "\"",
            "/usr/bin/shasum -a 256 -c " +
                release.currentBootstrapSha256AssetName,
            "/bin/bash ./" +
                release.currentBootstrapAssetName,
        )
}

// [VXG RealForever]
