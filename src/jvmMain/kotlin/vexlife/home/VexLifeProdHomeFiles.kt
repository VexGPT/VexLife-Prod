package vexlife.home

import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import java.util.UUID
import vexlife.contract.VexDurableContractFile
import vexlife.filesystem.VexOwnedPath

/**
 * Read-only admission of the canonical lived VexLife Home for VexLife-Prod.
 *
 * New Beginnings may create product-runtime records beneath the admitted Home, but this owner does
 * not form a second Home, rewrite canonical identity, or infer that an absent Home is permission to
 * mint another Vex/Companion lineage.
 */
object VexLifeProdHomeFiles {
    data class Result(
        val state: String,
        val productHome: String,
        val manifest: VexLifeProdHome.Manifest,
        val installation: VexLifeProdHome.Installation,
        val effectsPerformed: Boolean,
    )

    fun defaultMacRoot(
        userHome: Path,
    ): Path {
        require(
            userHome.isAbsolute,
        ) {
            "user Home must be absolute"
        }

        return userHome
            .normalize()
            .resolve(
                VexLifeProdHome.DefaultMacRelativeRoot,
            )
            .normalize()
    }

    /**
     * Canonical-namespace identity formation retained only for explicit future bootstrap proof.
     * Ordinary VexLife-Prod setup never calls this when an established Home is absent.
     */
    fun newInstallationIds(): VexLifeProdHome.InstallationIds =
        VexLifeProdHome.InstallationIds(
            deviceRef =
                "device.vexlife." +
                    UUID.randomUUID()
                        .toString()
                        .lowercase(),
            companionLineageRef =
                "companion-lineage.vexlife." +
                    UUID.randomUUID()
                        .toString()
                        .lowercase(),
        )

    fun observeExisting(
        productHome: Path,
    ): Result? {
        require(
            productHome.isAbsolute,
        ) {
            "canonical VexLife Home path must be absolute"
        }

        val home =
            productHome.normalize()

        if (
            !Files.exists(
                home,
                LinkOption.NOFOLLOW_LINKS,
            )
        ) {
            return null
        }

        requireRealDirectory(
            home,
        )

        for (
            relative in
            VexLifeProdHome.BaseDirectories
        ) {
            val directory =
                VexOwnedPath.resolveInside(
                    home,
                    relative,
                )
            require(
                VexOwnedPath.observeDirectory(
                    home,
                    directory,
                ) ==
                    VexOwnedPath.DirectoryState.Present,
            ) {
                "canonical VexLife Home is missing required directory: $relative"
            }
        }

        val manifest =
            requireNotNull(
                VexDurableContractFile.read<VexLifeProdHome.Manifest>(
                    ownedRoot = home,
                    path =
                        VexOwnedPath.resolveInside(
                            home,
                            VexLifeProdHome.HomeManifestRelativePath,
                        ),
                ),
            ) {
                "canonical VexLife Home has no current manifest"
            }
                .let(
                    VexLifeProdHome::validateManifest,
                )

        val installationPath =
            VexOwnedPath.resolveInside(
                home,
                "devices/${manifest.currentDeviceRef}.json",
            )
        val installation =
            requireNotNull(
                VexDurableContractFile.read<VexLifeProdHome.Installation>(
                    ownedRoot = home,
                    path = installationPath,
                ),
            ) {
                "canonical VexLife Home has no current device installation record"
            }
                .let(
                    VexLifeProdHome::validateInstallation,
                )

        require(
            installation.deviceRef ==
                manifest.currentDeviceRef &&
                installation.companionLineageRef ==
                manifest.currentCompanionLineageRef &&
                installation.familyRef ==
                manifest.familyRef,
        ) {
            "canonical VexLife Home and current device/Companion lineage disagree"
        }

        return Result(
            state = "EXISTING_CANONICAL_VEXLIFE_HOME_BOUND",
            productHome = home.toString(),
            manifest = manifest,
            installation = installation,
            effectsPerformed = false,
        )
    }

    /**
     * Preserve the historical method boundary while refusing the old parallel-Home behavior.
     *
     * Fresh Home formation must be ported from the accepted VexLife bootstrap as its own exact
     * candidate. Until then, absence holds setup before any identity or filesystem mutation.
     */
    fun bootstrap(
        productHome: Path,
        plan: VexLifeProdHome.BootstrapPlan,
    ): Result {
        observeExisting(
            productHome,
        )
            ?.let {
                return it
            }

        VexLifeProdHome.validateBootstrapPlan(
            plan,
        )

        error(
            "canonical VexLife Home is absent; VexLife-Prod must not mint a parallel Home or Companion lineage",
        )
    }

    private fun requireRealDirectory(
        path: Path,
    ) {
        val attributes =
            Files.readAttributes(
                path,
                BasicFileAttributes::class.java,
                LinkOption.NOFOLLOW_LINKS,
            )

        require(
            attributes.isDirectory &&
                !attributes.isSymbolicLink,
        ) {
            "canonical VexLife Home must be one real non-symlink directory"
        }
    }
}

// [VXG RealForever]
