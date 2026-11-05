package vexlife.home

import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.FileVisitResult
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.BasicFileAttributes
import java.util.UUID
import vexlife.contract.VexDurableContractFile

/**
 * Filesystem realization of [VexLifeProdHome].
 *
 * New Home formation is whole-root atomic. Existing Home presence grants no overwrite/repair effect:
 * it is strictly read/classified and returned to the caller.
 */
object VexLifeProdHomeFiles {
    data class Result(
        val state: String,
        val productHome: String,
        val manifest: VexLifeProdHome.Manifest,
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

    fun newInstallationIds(): VexLifeProdHome.InstallationIds =
        VexLifeProdHome.InstallationIds(
            deviceRef =
                "device.vexlife-prod." +
                    UUID.randomUUID()
                        .toString()
                        .lowercase(),
            companionLineageRef =
                "companion-lineage.vexlife-prod." +
                    UUID.randomUUID()
                        .toString()
                        .lowercase(),
        )

    fun bootstrap(
        productHome: Path,
        plan: VexLifeProdHome.BootstrapPlan,
    ): Result {
        require(
            productHome.isAbsolute,
        ) {
            "VexLife-Prod Home path must be absolute"
        }

        val home =
            productHome.normalize()

        if (
            Files.exists(
                home,
                LinkOption.NOFOLLOW_LINKS,
            )
        ) {
            requireRealDirectory(
                home,
            )

            val manifest =
                requireNotNull(
                    VexDurableContractFile.read<VexLifeProdHome.Manifest>(
                        home.resolve(
                            VexLifeProdHome.HomeManifestRelativePath,
                        ),
                    ),
                ) {
                    "existing VexLife-Prod Home has no current manifest"
                }

            require(
                manifest.schemaVersion ==
                    VexLifeProdHome.HomeSchema &&
                    manifest.existingHomePolicy ==
                    VexLifeProdHome.ExistingHomePolicy,
            ) {
                "existing VexLife-Prod Home manifest is not current"
            }

            return Result(
                state = "EXISTING_HOME_PRESERVED",
                productHome = home.toString(),
                manifest = manifest,
                effectsPerformed = false,
            )
        }

        val parent =
            requireNotNull(
                home.parent,
            ) {
                "VexLife-Prod Home requires one parent directory"
            }

        // The parent (for example ~/Library/Application Support) is ambient OS/user space.
        // Ensure it exists, but do not rewrite its permissions as though Prod owned it.
        Files.createDirectories(
            parent,
        )

        val staging =
            parent.resolve(
                ".${home.fileName}.bootstrap-${UUID.randomUUID()}",
            )

        require(
            !Files.exists(
                staging,
                LinkOption.NOFOLLOW_LINKS,
            ),
        ) {
            "VexLife-Prod Home bootstrap staging path already exists"
        }

        try {
            VexDurableContractFile.secureDirectory(
                staging,
            )

            for (
                relative in
                plan.directories
            ) {
                val directory =
                    resolveInside(
                        staging,
                        relative,
                    )
                VexDurableContractFile.secureDirectory(
                    directory,
                )
            }

            VexDurableContractFile.write(
                resolveInside(
                    staging,
                    VexLifeProdHome.HomeManifestRelativePath,
                ),
                plan.manifest,
            )
            VexDurableContractFile.write(
                resolveInside(
                    staging,
                    VexLifeProdHome.ModelConfigurationRelativePath,
                ),
                plan.initialModelConfiguration,
            )
            VexDurableContractFile.write(
                resolveInside(
                    staging,
                    VexLifeProdHome.BootstrapReceiptRelativePath,
                ),
                plan.receipt,
            )

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
                    "atomic VexLife-Prod Home formation is unavailable at $parent",
                )
            }

            requireRealDirectory(
                home,
            )

            val manifest =
                requireNotNull(
                    VexDurableContractFile.read<VexLifeProdHome.Manifest>(
                        home.resolve(
                            VexLifeProdHome.HomeManifestRelativePath,
                        ),
                    ),
                )

            return Result(
                state = "NEW_HOME_FORMED",
                productHome = home.toString(),
                manifest = manifest,
                effectsPerformed = true,
            )
        } finally {
            if (
                Files.exists(
                    staging,
                    LinkOption.NOFOLLOW_LINKS,
                )
            ) {
                removeOwnedDirectoryTree(
                    staging,
                )
            }
        }
    }

    private fun resolveInside(
        root: Path,
        relative: String,
    ): Path {
        require(
            relative.isNotBlank() &&
                !Path.of(
                    relative,
                ).isAbsolute,
        ) {
            "VexLife-Prod Home relative path must be non-empty and relative"
        }

        val resolved =
            root.resolve(
                relative,
            )
                .normalize()

        require(
            resolved.startsWith(
                root,
            ),
        ) {
            "VexLife-Prod Home relative path escaped the Home root"
        }

        return resolved
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
            "VexLife-Prod Home must be one real non-symlink directory"
        }
    }

    private fun removeOwnedDirectoryTree(
        root: Path,
    ) {
        requireRealDirectory(
            root,
        )

        Files.walkFileTree(
            root,
            object :
                SimpleFileVisitor<Path>() {
                override fun visitFile(
                    file: Path,
                    attrs: BasicFileAttributes,
                ): FileVisitResult {
                    Files.delete(
                        file,
                    )
                    return FileVisitResult.CONTINUE
                }

                override fun postVisitDirectory(
                    directory: Path,
                    error: java.io.IOException?,
                ): FileVisitResult {
                    if (
                        error !=
                            null
                    ) {
                        throw error
                    }
                    Files.delete(
                        directory,
                    )
                    return FileVisitResult.CONTINUE
                }
            },
        )
    }
}

// [VXG RealForever]
