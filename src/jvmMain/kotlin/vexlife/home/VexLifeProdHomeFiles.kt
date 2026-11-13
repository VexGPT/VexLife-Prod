package vexlife.home

import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Path
import java.nio.file.StandardCopyOption
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

    /**
     * No-effect classification of one human-selected Home coordinate.
     *
     * Ordinary held conditions are values, not exceptions: the human surface must be able to say
     * "this folder is being preserved" instead of rendering a stack trace.
     */
    sealed interface Location {
        val productHome: String
        val disposition: VexLifeProdHome.LocationDisposition

        /**
         * Formation may target this coordinate. [nearestExistingAncestor] is the directory whose
         * volume will hold the Home; [missingAncestorCount] counts folders formation must also create
         * between that ancestor and the Home itself (0 when the parent already exists).
         */
        data class Formable(
            override val productHome: String,
            override val disposition: VexLifeProdHome.LocationDisposition,
            val nearestExistingAncestor: String,
            val missingAncestorCount: Int,
        ) : Location {
            init {
                require(
                    disposition.formationAllowed &&
                        missingAncestorCount >=
                        0,
                ) {
                    "formable Home location requires a formation-admitting disposition"
                }
            }
        }

        data class Existing(
            val home: Result,
        ) : Location {
            override val productHome: String =
                home.productHome
            override val disposition: VexLifeProdHome.LocationDisposition =
                VexLifeProdHome.LocationDisposition.Canonical
        }

        data class Held(
            override val productHome: String,
            override val disposition: VexLifeProdHome.LocationDisposition,
            val detail: String,
        ) : Location {
            init {
                require(
                    disposition.held &&
                        detail.isNotBlank(),
                ) {
                    "held Home location requires a held disposition and bounded detail"
                }
            }
        }
    }

    fun observeLocation(
        productHome: Path,
    ): Location {
        require(
            productHome.isAbsolute,
        ) {
            "selected Vex Home path must be absolute"
        }
        val home =
            productHome.normalize()
        val homeText =
            home.toString()

        val attributes =
            try {
                Files.readAttributes(
                    home,
                    BasicFileAttributes::class.java,
                    LinkOption.NOFOLLOW_LINKS,
                )
            } catch (
                _: java.nio.file.NoSuchFileException,
            ) {
                null
            } catch (
                error: java.io.IOException,
            ) {
                // ENOTDIR: a file sits where a parent folder would have to be.
                val blocking =
                    generateSequence(
                        home.parent,
                    ) {
                        it.parent
                    }.firstOrNull {
                        Files.exists(
                            it,
                            LinkOption.NOFOLLOW_LINKS,
                        )
                    }
                return if (
                    blocking !=
                        null &&
                    !Files.isDirectory(
                        blocking,
                    )
                ) {
                    Location.Held(
                        productHome = homeText,
                        disposition = VexLifeProdHome.LocationDisposition.HeldNotDirectory,
                        detail =
                            "something that is not a folder is in the way at $blocking",
                    )
                } else {
                    Location.Held(
                        productHome = homeText,
                        disposition = VexLifeProdHome.LocationDisposition.HeldLocationUnreadable,
                        detail =
                            "the selected location could not be read: " +
                                (error.message ?: error.javaClass.simpleName),
                    )
                }
            }

        if (
            attributes ==
                null
        ) {
            var ancestor =
                home.parent
            var missing =
                0
            while (
                ancestor !=
                    null &&
                !Files.exists(
                    ancestor,
                    LinkOption.NOFOLLOW_LINKS,
                )
            ) {
                missing += 1
                ancestor =
                    ancestor.parent
            }
            if (
                ancestor ==
                    null ||
                !Files.isDirectory(
                    ancestor,
                )
            ) {
                return Location.Held(
                    productHome = homeText,
                    disposition = VexLifeProdHome.LocationDisposition.HeldNotDirectory,
                    detail =
                        "something that is not a folder is in the way at " +
                            (ancestor ?: home.root),
                )
            }
            if (
                !Files.isWritable(
                    ancestor,
                )
            ) {
                return Location.Held(
                    productHome = homeText,
                    disposition = VexLifeProdHome.LocationDisposition.HeldLocationNotWritable,
                    detail =
                        "this Mac user cannot create folders inside $ancestor",
                )
            }
            if (
                missing ==
                0
            ) {
                linkedParent(
                    home,
                    homeText,
                )?.let {
                    return it
                }
            }
            return Location.Formable(
                productHome = homeText,
                disposition = VexLifeProdHome.LocationDisposition.Absent,
                nearestExistingAncestor = ancestor.toString(),
                missingAncestorCount = missing,
            )
        }

        if (
            attributes.isSymbolicLink
        ) {
            return Location.Held(
                productHome = homeText,
                disposition = VexLifeProdHome.LocationDisposition.HeldSymbolicLink,
                detail =
                    "the selected location is a link to somewhere else; choose the real folder instead",
            )
        }

        if (
            !attributes.isDirectory
        ) {
            return Location.Held(
                productHome = homeText,
                disposition = VexLifeProdHome.LocationDisposition.HeldNotDirectory,
                detail =
                    "the selected location is a file, not a folder",
            )
        }

        val empty =
            try {
                Files.list(
                    home,
                )
                    .use {
                        entries ->
                        !entries
                            .iterator()
                            .hasNext()
                    }
            } catch (
                error: java.io.IOException,
            ) {
                return Location.Held(
                    productHome = homeText,
                    disposition = VexLifeProdHome.LocationDisposition.HeldLocationUnreadable,
                    detail =
                        "the selected folder could not be read: " +
                            (error.message ?: error.javaClass.simpleName),
                )
            }

        if (
            empty
        ) {
            linkedParent(
                home,
                homeText,
            )?.let {
                return it
            }
            val parent =
                home.parent
            if (
                parent ==
                    null ||
                !Files.isWritable(
                    parent,
                )
            ) {
                return Location.Held(
                    productHome = homeText,
                    disposition = VexLifeProdHome.LocationDisposition.HeldLocationNotWritable,
                    detail =
                        "VexLife forms a Home beside the selected folder and then moves it into place, " +
                            "but this Mac user cannot write inside ${parent ?: home.root}",
                )
            }
            return Location.Formable(
                productHome = homeText,
                disposition = VexLifeProdHome.LocationDisposition.EmptyDirectory,
                nearestExistingAncestor = homeText,
                missingAncestorCount = 0,
            )
        }

        if (
            !Files.exists(
                home.resolve(
                    VexLifeProdHome.HomeManifestRelativePath,
                ),
                LinkOption.NOFOLLOW_LINKS,
            )
        ) {
            return Location.Held(
                productHome = homeText,
                disposition = VexLifeProdHome.LocationDisposition.HeldNoncanonicalContent,
                detail =
                    "the selected folder already contains files that are not a Vex Home",
            )
        }

        return try {
            Location.Existing(
                requireNotNull(
                    observeExisting(
                        home,
                    ),
                ),
            )
        } catch (
            error: Exception,
        ) {
            Location.Held(
                productHome = homeText,
                disposition = VexLifeProdHome.LocationDisposition.HeldInvalidCanonicalRecords,
                detail =
                    "the selected folder looks like a Vex Home but its identity records did not validate: " +
                        (error.message ?: error.javaClass.simpleName),
            )
        }
    }

    /**
     * Formation stages beside the Home and requires that parent to be a real directory. A parent
     * reached through a link (for example macOS `/tmp` -> `/private/tmp`) is held with the real
     * location named, instead of failing midway through the Home-only effect.
     */
    private fun linkedParent(
        home: Path,
        homeText: String,
    ): Location.Held? {
        val parent =
            home.parent
                ?: return null
        if (
            !Files.isSymbolicLink(
                parent,
            )
        ) {
            return null
        }
        val real =
            runCatching {
                parent.toRealPath()
            }.getOrNull()
        return Location.Held(
            productHome = homeText,
            disposition = VexLifeProdHome.LocationDisposition.HeldSymbolicLink,
            detail =
                "the folder that would contain it ($parent) is a link" +
                    (
                        real?.let {
                            "; use the real location " +
                                it.resolve(
                                    home.fileName,
                                )
                        } ?: ""
                    ),
        )
    }

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

        when (
            val location =
                observeLocation(
                    home,
                )
        ) {
            is Location.Existing ->
                return location.home

            is Location.Held ->
                throw IllegalArgumentException(
                    "selected Vex Home is preserved and cannot be formed: " +
                        location.disposition.wireValue +
                        " — " +
                        location.detail,
                )

            is Location.Formable ->
                Unit
        }
        // An EmptyDirectory target is replaced by the atomic rename below. POSIX rename(2) replaces
        // an empty directory and fails with ENOTEMPTY if anything appeared inside it meanwhile, so
        // the empty-folder admission cannot overwrite content created after observation.

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
