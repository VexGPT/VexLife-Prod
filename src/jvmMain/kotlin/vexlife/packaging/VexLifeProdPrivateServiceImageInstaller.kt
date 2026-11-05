package vexlife.packaging

import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.PosixFilePermission
import java.util.UUID
import vexlife.filesystem.VexExclusiveFileLease
import vexlife.filesystem.VexOwnedPath
import vexlife.home.VexLifeProdHomeFiles

/**
 * Install one already-verified private service image into the established VexLife-Prod Home.
 *
 * This owner performs image materialization only. It does not create Home, choose source identity,
 * start/stop processes, or grant repair authority over an invalid existing installed image.
 */
object VexLifeProdPrivateServiceImageInstaller {
    private const val ImageBaseRelativePath =
        "runtime/product-service/images"

    private const val InstallLockRelativePath =
        "runtime/product-service/.image-install.lock"

    data class Result(
        val state: String,
        val sourceSha: String,
        val payloadSha256: String,
        val imagePath: String,
        val imageMutationPerformed: Boolean,
    )

    fun installedImagePath(
        productHome: Path,
        sourceSha: String,
    ): Path {
        require(
            sourceSha.matches(
                Regex(
                    "[0-9a-f]{40}",
                ),
            ),
        ) {
            "installed private service-image source SHA must be one exact Git commit"
        }

        val home =
            currentHomeRoot(
                productHome,
            )

        return VexOwnedPath.resolveInside(
            home,
            "$ImageBaseRelativePath/$sourceSha",
        )
    }

    fun install(
        sourceImageRoot: Path,
        productHome: Path,
        expectedSourceSha: String,
    ): Result {
        val source =
            VexLifeProdPrivateServiceImage.observe(
                imageRoot = sourceImageRoot,
                expectedSourceSha = expectedSourceSha,
            )
        val home =
            currentHomeRoot(
                productHome,
            )
        val target =
            installedImagePath(
                productHome = home,
                sourceSha = expectedSourceSha,
            )
        val imageBase =
            requireNotNull(
                target.parent,
            )
        VexOwnedPath.ensureDirectory(
            home,
            imageBase,
        )

        val installLock =
            VexOwnedPath.resolveInside(
                home,
                InstallLockRelativePath,
            )

        return VexExclusiveFileLease
            .acquire(
                installLock,
            )
            .use {
                installUnderLease(
                    sourceImageRoot = sourceImageRoot
                        .toAbsolutePath()
                        .normalize(),
                    source = source,
                    home = home,
                    imageBase = imageBase,
                    expectedSourceSha = expectedSourceSha,
                )
            }
    }

    private fun currentHomeRoot(
        productHome: Path,
    ): Path {
        val currentHome =
            requireNotNull(
                VexLifeProdHomeFiles.observeExisting(
                    productHome,
                ),
            ) {
                "private service-image installation requires one established current VexLife-Prod Home"
            }

        return VexOwnedPath.requireRoot(
            Path.of(
                currentHome.productHome,
            ),
            "VexLife-Prod Home",
        )
    }

    private fun installUnderLease(
        sourceImageRoot: Path,
        source: VexLifeProdPrivateServiceImage.Observation,
        home: Path,
        imageBase: Path,
        expectedSourceSha: String,
    ): Result {
        if (
            sourceImageRoot ==
                target
        ) {
            return result(
                state = "REUSED_ALREADY_INSTALLED_EXACT_IMAGE",
                observation = source,
                imagePath = target,
                imageMutationPerformed = false,
            )
        }

        if (
            Files.exists(
                target,
                LinkOption.NOFOLLOW_LINKS,
            )
        ) {
            val existing =
                VexLifeProdPrivateServiceImage.observe(
                    imageRoot = target,
                    expectedSourceSha = expectedSourceSha,
                )
            require(
                existing.payloadSha256 ==
                    source.payloadSha256,
            ) {
                "existing installed private service image has different payload identity"
            }

            return result(
                state = "REUSED_VERIFIED_INSTALLED_IMAGE",
                observation = existing,
                imagePath = target,
                imageMutationPerformed = false,
            )
        }

        val staging =
            VexOwnedPath.resolveInside(
                imageBase,
                "." +
                    expectedSourceSha +
                    ".staging-" +
                    UUID.randomUUID(),
            )

        require(
            !Files.exists(
                staging,
                LinkOption.NOFOLLOW_LINKS,
            ),
        ) {
            "private service-image staging path already exists"
        }

        try {
            copyImage(
                source = sourceImageRoot,
                staging = staging,
            )

            val staged =
                VexLifeProdPrivateServiceImage.observe(
                    imageRoot = staging,
                    expectedSourceSha = expectedSourceSha,
                )
            require(
                staged.payloadSha256 ==
                    source.payloadSha256,
            ) {
                "staged private service image differs from verified source image"
            }

            try {
                Files.move(
                    staging,
                    target,
                    StandardCopyOption.ATOMIC_MOVE,
                )
            } catch (
                _: AtomicMoveNotSupportedException,
            ) {
                error(
                    "atomic private service-image installation is unavailable",
                )
            }

            val installed =
                VexLifeProdPrivateServiceImage.observe(
                    imageRoot = target,
                    expectedSourceSha = expectedSourceSha,
                )
            require(
                installed.payloadSha256 ==
                    source.payloadSha256,
            ) {
                "installed private service image differs after atomic publication"
            }

            return result(
                state = "INSTALLED_VERIFIED_PRIVATE_SERVICE_IMAGE",
                observation = installed,
                imagePath = target,
                imageMutationPerformed = true,
            )
        } finally {
            if (
                Files.exists(
                    staging,
                    LinkOption.NOFOLLOW_LINKS,
                )
            ) {
                deleteTreeNoFollow(
                    staging,
                )
            }
        }
    }

    private fun copyImage(
        source: Path,
        staging: Path,
    ) {
        val sourceRoot =
            VexOwnedPath.requireRoot(
                source,
                "private service-image source",
            )

        Files.createDirectory(
            staging,
        )
        copyPermissions(
            sourceRoot,
            staging,
        )

        Files.walk(
            sourceRoot,
        )
            .use {
                stream ->
                stream
                    .sorted()
                    .forEach {
                        sourcePath ->
                        if (
                            sourcePath ==
                                sourceRoot
                        ) {
                            return@forEach
                        }

                        val relative =
                            sourceRoot.relativize(
                                sourcePath,
                            )
                        val destination =
                            staging
                                .resolve(
                                    relative,
                                )
                                .normalize()

                        require(
                            destination.startsWith(
                                staging,
                            ),
                        ) {
                            "private service-image copy escaped staging root"
                        }

                        when {
                            Files.isSymbolicLink(
                                sourcePath,
                            ) -> {
                                Files.createSymbolicLink(
                                    destination,
                                    Files.readSymbolicLink(
                                        sourcePath,
                                    ),
                                )
                            }

                            Files.isDirectory(
                                sourcePath,
                                LinkOption.NOFOLLOW_LINKS,
                            ) -> {
                                Files.createDirectory(
                                    destination,
                                )
                                copyPermissions(
                                    sourcePath,
                                    destination,
                                )
                            }

                            Files.isRegularFile(
                                sourcePath,
                                LinkOption.NOFOLLOW_LINKS,
                            ) -> {
                                Files.copy(
                                    sourcePath,
                                    destination,
                                    StandardCopyOption.COPY_ATTRIBUTES,
                                )
                                copyPermissions(
                                    sourcePath,
                                    destination,
                                )
                            }

                            else ->
                                error(
                                    "private service-image source contains unsupported entry: " +
                                        relative,
                                )
                        }
                    }
            }
    }

    private fun deleteTreeNoFollow(
        root: Path,
    ) {
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

    private fun copyPermissions(
        source: Path,
        destination: Path,
    ) {
        try {
            val permissions =
                Files.getPosixFilePermissions(
                    source,
                    LinkOption.NOFOLLOW_LINKS,
                )
            Files.setPosixFilePermissions(
                destination,
                permissions,
            )
        } catch (
            _: UnsupportedOperationException,
        ) {
            // Product host is POSIX/macOS. This keeps source tests usable on non-POSIX hosts.
        }
    }

    private fun result(
        state: String,
        observation: VexLifeProdPrivateServiceImage.Observation,
        imagePath: Path,
        imageMutationPerformed: Boolean,
    ): Result =
        Result(
            state = state,
            sourceSha = observation.sourceSha,
            payloadSha256 = observation.payloadSha256,
            imagePath = imagePath.toString(),
            imageMutationPerformed = imageMutationPerformed,
        )
}

// [VXG RealForever]
