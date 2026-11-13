package vexlife.home

import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.BasicFileAttributes
import java.util.UUID
import vexlife.contract.VexDurableContractFile
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
     * Form one fresh canonical VexLife Home from the portable bootstrap plan.
     *
     * This is not authority to replace or migrate an existing Home. The target must be absent.
     * Formation happens in one sibling staging root and publishes by atomic rename only after the
     * exact Home/device/model/receipt relationships re-observe successfully.
     */
    fun bootstrap(
        productHome: Path,
        plan: VexLifeProdHome.BootstrapPlan,
    ): Result {
        require(
            productHome.isAbsolute,
        ) {
            "canonical VexLife Home path must be absolute"
        }

        val home =
            productHome.normalize()

        if (
            Files.exists(
                home,
                LinkOption.NOFOLLOW_LINKS,
            )
        ) {
            return requireNotNull(
                observeExisting(
                    home,
                ),
            ) {
                "existing VexLife Home path is not one current canonical Home"
            }
        }

        val currentPlan =
            VexLifeProdHome.validateBootstrapPlan(
                plan,
            )
        val parent =
            requireNotNull(
                home.parent,
            ) {
                "canonical VexLife Home requires one parent directory"
            }
        requireRealDirectory(
            parent,
        )

        val staging =
            parent.resolve(
                "." +
                    home.fileName +
                    ".bootstrap-" +
                    UUID.randomUUID(),
            )
                .normalize()

        require(
            !Files.exists(
                staging,
                LinkOption.NOFOLLOW_LINKS,
            ),
        ) {
            "fresh VexLife Home staging coordinate already exists"
        }

        Files.createDirectory(
            staging,
        )
        requireRealDirectory(
            staging,
        )

        var published =
            false
        try {
            currentPlan.directories.forEach {
                relative ->
                VexOwnedPath.ensureDirectory(
                    staging,
                    VexOwnedPath.resolveInside(
                        staging,
                        relative,
                    ),
                )
            }

            VexDurableContractFile.write(
                ownedRoot = staging,
                path =
                    VexOwnedPath.resolveInside(
                        staging,
                        VexLifeProdHome.HomeManifestRelativePath,
                    ),
                value = currentPlan.manifest,
            )
            VexDurableContractFile.write(
                ownedRoot = staging,
                path =
                    VexOwnedPath.resolveInside(
                        staging,
                        "devices/${currentPlan.installation.deviceRef}.json",
                    ),
                value = currentPlan.installation,
            )
            VexDurableContractFile.write(
                ownedRoot = staging,
                path =
                    VexOwnedPath.resolveInside(
                        staging,
                        VexLifeProdHome.CanonicalModelConfigurationRelativePath,
                    ),
                value = currentPlan.canonicalModelConfiguration,
            )
            VexDurableContractFile.write(
                ownedRoot = staging,
                path =
                    VexOwnedPath.resolveInside(
                        staging,
                        VexLifeProdHome.ModelConfigurationRelativePath,
                    ),
                value = currentPlan.initialModelConfiguration,
            )
            VexDurableContractFile.write(
                ownedRoot = staging,
                path =
                    VexOwnedPath.resolveInside(
                        staging,
                        VexLifeProdHome.BootstrapReceiptRelativePath,
                    ),
                value = currentPlan.receipt,
            )

            val staged =
                requireNotNull(
                    observeExisting(
                        staging,
                    ),
                ) {
                    "fresh VexLife Home staging did not re-observe as canonical"
                }
            require(
                staged.manifest ==
                    currentPlan.manifest &&
                    staged.installation ==
                    currentPlan.installation &&
                    !staged.effectsPerformed,
            ) {
                "fresh VexLife Home staging identity differs from the admitted bootstrap plan"
            }

            try {
                Files.move(
                    staging,
                    home,
                    StandardCopyOption.ATOMIC_MOVE,
                )
            } catch (
                _: AtomicMoveNotSupportedException,
            ) {
                error(
                    "atomic fresh VexLife Home publication is unavailable for $home",
                )
            }
            published =
                true

            val formed =
                requireNotNull(
                    observeExisting(
                        home,
                    ),
                ) {
                    "published fresh VexLife Home did not re-observe as canonical"
                }

            return formed.copy(
                state = "FRESH_CANONICAL_VEXLIFE_HOME_FORMED",
                effectsPerformed = true,
            )
        } finally {
            if (
                !published &&
                Files.exists(
                    staging,
                    LinkOption.NOFOLLOW_LINKS,
                )
            ) {
                deleteOwnedTree(
                    staging,
                )
            }
        }
    }

    private fun deleteOwnedTree(
        root: Path,
    ) {
        requireRealDirectory(
            root,
        )
        Files.walk(
            root,
        )
            .use {
                stream ->
                stream
                    .toList()
                    .sortedByDescending {
                        it.nameCount
                    }
                    .forEach {
                        path ->
                        Files.deleteIfExists(
                            path,
                        )
                    }
            }
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
