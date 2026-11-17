package vexlife.platform.windows

import java.io.BufferedInputStream
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.UUID
import java.util.zip.CRC32
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream
import vexlife.artifact.DirectHttpsArtifactDelivery
import vexlife.filesystem.VexOwnedPath
import vexlife.home.VexLifeProdHomeFiles
import vexlife.model.heretic.HereticRuntimeRealization

/**
 * Product-owned materialization of exact Windows Heretic runtime dependencies.
 *
 * This owner does not choose a model, start a runtime, start the Product service, or grant
 * acquisition authority. It only observes or materializes the exact dependencies declared by one
 * already-selected [HereticRuntimeRealization.Value].
 */
object VexLifeWindowsRuntimeMaterialization {
    enum class FileState {
        ABSENT,
        VERIFIED,
        INVALID,
    }

    data class Observation(
        val realizationRef: String,
        val modelPath: String,
        val modelState: FileState,
        val runtimeArchivePath: String,
        val runtimeArchiveState: FileState,
        val runtimeImagePath: String,
        val runtimeImageState: FileState,
        val runtimeExecutablePath: String,
        val runtimeExecutableState: FileState,
        val effectsPerformed: Boolean,
    ) {
        val ready: Boolean
            get() =
                modelState == FileState.VERIFIED &&
                    runtimeArchiveState == FileState.VERIFIED &&
                    runtimeImageState == FileState.VERIFIED &&
                    runtimeExecutableState == FileState.VERIFIED
    }

    data class Result(
        val observation: Observation,
        val modelDisposition: String,
        val runtimeArchiveDisposition: String,
        val runtimeExtractionPerformed: Boolean,
    ) {
        init {
            require(observation.ready) {
                "Windows runtime materialization result requires all exact dependencies"
            }
        }
    }

    fun observe(
        productHome: Path,
        realization: HereticRuntimeRealization.Value =
            HereticRuntimeRealization.WindowsQwen4B,
    ): Observation {
        requireWindowsRealization(realization)
        val home = currentHome(productHome)
        val artifact = realization.artifact
        val binary = requireNotNull(realization.runtimeBinary) {
            "Windows runtime realization requires one runtime binary"
        }
        val modelFilename = requireNotNull(artifact.primaryFilename) {
            "Windows runtime realization requires one primary model file"
        }
        val modelBytes = requireNotNull(artifact.expectedPrimaryBytes) {
            "Windows runtime realization requires exact primary model bytes"
        }

        val modelRoot =
            VexOwnedPath.resolveInside(
                home,
                artifact.materializationRootRelativePath,
            )
        val modelPath =
            modelRoot.resolve(modelFilename).normalize()

        val runtimeRoot =
            VexOwnedPath.resolveInside(
                home,
                binary.materializationRootRelativePath,
            )
        val archivePath =
            runtimeRoot
                .resolve("archive")
                .resolve(binary.archiveFilename)
                .normalize()
        val imagePath =
            runtimeRoot.resolve("image").normalize()
        val executablePath =
            imagePath
                .resolve(binary.executableRelativePath)
                .normalize()

        val runtimeArchiveState =
            observeFile(
                archivePath,
                expectedBytes = binary.archiveBytes,
                expectedSha256 = binary.archiveSha256,
            )
        val runtimeImageState =
            if (runtimeArchiveState == FileState.VERIFIED) {
                observeRuntimeImage(
                    archivePath = archivePath,
                    imagePath = imagePath,
                    binary = binary,
                )
            } else if (
                Files.exists(
                    imagePath,
                    LinkOption.NOFOLLOW_LINKS,
                )
            ) {
                FileState.INVALID
            } else {
                FileState.ABSENT
            }

        return Observation(
            realizationRef = realization.realizationRef,
            modelPath = modelPath.toString(),
            modelState =
                observeFile(
                    modelPath,
                    expectedBytes = modelBytes,
                    expectedSha256 = artifact.contentSetSha256,
                ),
            runtimeArchivePath = archivePath.toString(),
            runtimeArchiveState = runtimeArchiveState,
            runtimeImagePath = imagePath.toString(),
            runtimeImageState = runtimeImageState,
            runtimeExecutablePath = executablePath.toString(),
            runtimeExecutableState =
                observeFile(
                    executablePath,
                    expectedBytes = null,
                    expectedSha256 = binary.executableSha256,
                ),
            effectsPerformed = false,
        )
    }

    fun ensure(
        productHome: Path,
        allowAcquisition: Boolean,
        realization: HereticRuntimeRealization.Value =
            HereticRuntimeRealization.WindowsQwen4B,
        transport: DirectHttpsArtifactDelivery.Transport =
            DirectHttpsArtifactDelivery.JdkTransport,
    ): Result {
        requireWindowsRealization(realization)
        val before = observe(productHome, realization)

        require(before.modelState != FileState.INVALID) {
            "existing Windows model artifact is not the exact declared realization"
        }
        require(before.runtimeArchiveState != FileState.INVALID) {
            "existing Windows runtime archive is not the exact declared realization"
        }
        require(before.runtimeImageState != FileState.INVALID) {
            "existing Windows runtime image is not the exact declared realization"
        }
        require(before.runtimeExecutableState != FileState.INVALID) {
            "existing Windows runtime executable is not the exact declared realization"
        }

        if (before.ready) {
            return Result(
                observation = before,
                modelDisposition = "REUSED_VERIFIED_MODEL",
                runtimeArchiveDisposition = "REUSED_VERIFIED_RUNTIME_ARCHIVE",
                runtimeExtractionPerformed = false,
            )
        }

        require(allowAcquisition) {
            "Windows runtime dependencies are missing and acquisition authority was not granted"
        }

        val home = currentHome(productHome)
        val artifact = realization.artifact
        val binary = requireNotNull(realization.runtimeBinary)
        val modelUrl = requireNotNull(artifact.directHttpsUrl) {
            "Windows model realization lacks one exact HTTPS acquisition source"
        }

        val modelPath = Path.of(before.modelPath)
        val archivePath = Path.of(before.runtimeArchivePath)

        val modelDisposition =
            if (before.modelState == FileState.VERIFIED) {
                "REUSED_VERIFIED_MODEL"
            } else {
                VexOwnedPath.ensureParent(home, modelPath)
                DirectHttpsArtifactDelivery.download(
                    request =
                        DirectHttpsArtifactDelivery.Request(
                            url = modelUrl,
                            expectedSha256 = artifact.contentSetSha256,
                            expectedBytes =
                                requireNotNull(
                                    artifact.expectedPrimaryBytes,
                                ),
                        ),
                    finalPath = modelPath,
                    transport = transport,
                ).disposition
            }

        val archiveDisposition =
            if (before.runtimeArchiveState == FileState.VERIFIED) {
                "REUSED_VERIFIED_RUNTIME_ARCHIVE"
            } else {
                VexOwnedPath.ensureParent(home, archivePath)
                DirectHttpsArtifactDelivery.download(
                    request =
                        DirectHttpsArtifactDelivery.Request(
                            url = binary.archiveUrl,
                            expectedSha256 = binary.archiveSha256,
                            expectedBytes = binary.archiveBytes,
                        ),
                    finalPath = archivePath,
                    transport = transport,
                ).disposition
            }

        var extracted = false
        val afterDownloads = observe(home, realization)

        if (afterDownloads.runtimeImageState == FileState.ABSENT) {
            extractRuntimeArchive(
                home = home,
                archivePath = Path.of(afterDownloads.runtimeArchivePath),
                imagePath = Path.of(afterDownloads.runtimeImagePath),
                binary = binary,
            )
            extracted = true
        }

        val after = observe(home, realization)
        require(after.ready) {
            "Windows runtime dependency materialization did not converge to exact ready state"
        }

        return Result(
            observation = after,
            modelDisposition = modelDisposition,
            runtimeArchiveDisposition = archiveDisposition,
            runtimeExtractionPerformed = extracted,
        )
    }

    private fun extractRuntimeArchive(
        home: Path,
        archivePath: Path,
        imagePath: Path,
        binary: HereticRuntimeRealization.RuntimeBinaryProfile,
    ) {
        require(
            !Files.exists(
                imagePath,
                LinkOption.NOFOLLOW_LINKS,
            ),
        ) {
            "Windows runtime image already exists but is incomplete; explicit repair is required"
        }

        val staging =
            imagePath.resolveSibling(
                imagePath.fileName.toString() +
                    ".staging-" +
                    UUID.randomUUID(),
            )

        VexOwnedPath.ensureDirectory(
            home,
            requireNotNull(staging.parent),
        )
        Files.createDirectory(staging)

        try {
            val seen = mutableSetOf<String>()

            ZipInputStream(
                    BufferedInputStream(
                        Files.newInputStream(
                            archivePath,
                        ),
                    ),
                )
                .use { zip ->
                    while (true) {
                        val entry = zip.nextEntry ?: break
                        val rawName = entry.name
                        val normalizedName =
                            normalizedArchiveEntry(
                                rawName = rawName,
                                directory = entry.isDirectory,
                            )
                        require(seen.add(normalizedName)) {
                            "Windows runtime archive contains a duplicate path: $rawName"
                        }

                        val destination =
                            staging
                                .resolve(normalizedName)
                                .normalize()
                        require(destination.startsWith(staging)) {
                            "Windows runtime archive path escaped staging root"
                        }

                        if (entry.isDirectory) {
                            VexOwnedPath.ensureDirectory(
                                home,
                                destination,
                            )
                        } else {
                            VexOwnedPath.ensureParent(
                                home,
                                destination,
                            )
                            Files.newOutputStream(destination).use { output ->
                                zip.copyTo(output)
                            }
                        }

                        zip.closeEntry()
                    }
                }

            val executable =
                staging
                    .resolve(binary.executableRelativePath)
                    .normalize()
            require(executable.startsWith(staging)) {
                "Windows runtime executable escaped extracted image"
            }
            require(
                observeFile(
                    executable,
                    expectedBytes = null,
                    expectedSha256 = binary.executableSha256,
                ) ==
                    FileState.VERIFIED,
            ) {
                "extracted Windows runtime executable does not match the pinned realization"
            }
            require(
                observeRuntimeImage(
                    archivePath = archivePath,
                    imagePath = staging,
                    binary = binary,
                ) ==
                    FileState.VERIFIED,
            ) {
                "extracted Windows runtime image does not match the exact release archive"
            }

            try {
                Files.move(
                    staging,
                    imagePath,
                    StandardCopyOption.ATOMIC_MOVE,
                )
            } catch (_: AtomicMoveNotSupportedException) {
                error(
                    "atomic Windows runtime-image publication is unavailable",
                )
            }
        } finally {
            if (
                Files.exists(
                    staging,
                    LinkOption.NOFOLLOW_LINKS,
                )
            ) {
                deleteTreeNoFollow(staging)
            }
        }
    }

    private fun observeRuntimeImage(
        archivePath: Path,
        imagePath: Path,
        binary: HereticRuntimeRealization.RuntimeBinaryProfile,
    ): FileState {
        if (
            !Files.exists(
                imagePath,
                LinkOption.NOFOLLOW_LINKS,
            )
        ) {
            return FileState.ABSENT
        }
        if (
            !Files.isDirectory(
                imagePath,
                LinkOption.NOFOLLOW_LINKS,
            ) ||
            Files.isSymbolicLink(
                imagePath,
            )
        ) {
            return FileState.INVALID
        }

        return runCatching {
                val expectedFiles =
                    linkedMapOf<String, Pair<Long, Long>>()
                val expectedDirectories =
                    linkedSetOf<String>()
                val seen =
                    linkedSetOf<String>()

                ZipFile(
                        archivePath.toFile(),
                    )
                    .use { zip ->
                        zip.entries()
                            .asSequence()
                            .forEach { entry ->
                                val normalizedName =
                                    normalizedArchiveEntry(
                                        rawName = entry.name,
                                        directory = entry.isDirectory,
                                    )
                                require(
                                    seen.add(
                                        normalizedName,
                                    ),
                                ) {
                                    "Windows runtime archive contains a duplicate path: " +
                                        entry.name
                                }

                                if (
                                    entry.isDirectory
                                ) {
                                    expectedDirectories +=
                                        normalizedName
                                } else {
                                    require(
                                        entry.size >= 0 &&
                                            entry.crc >= 0,
                                    ) {
                                        "Windows runtime archive entry lacks exact size/CRC: " +
                                            entry.name
                                    }
                                    expectedFiles[
                                        normalizedName
                                    ] =
                                        entry.size to
                                            entry.crc
                                }
                            }
                    }

                expectedDirectories.forEach { relative ->
                    val directory =
                        imagePath
                            .resolve(
                                relative,
                            )
                            .normalize()
                    require(
                        directory.startsWith(
                            imagePath,
                        ) &&
                            Files.isDirectory(
                                directory,
                                LinkOption.NOFOLLOW_LINKS,
                            ) &&
                            !Files.isSymbolicLink(
                                directory,
                            ),
                    ) {
                        "Windows runtime image directory drift: $relative"
                    }
                }

                expectedFiles.forEach { (relative, expected) ->
                    val file =
                        imagePath
                            .resolve(
                                relative,
                            )
                            .normalize()
                    require(
                        file.startsWith(
                            imagePath,
                        ) &&
                            Files.isRegularFile(
                                file,
                                LinkOption.NOFOLLOW_LINKS,
                            ) &&
                            !Files.isSymbolicLink(
                                file,
                            ) &&
                            Files.size(
                                file,
                            ) ==
                            expected.first &&
                            crc32(
                                file,
                            ) ==
                            expected.second,
                    ) {
                        "Windows runtime image file drift: $relative"
                    }
                }

                val actualFiles =
                    Files.walk(
                            imagePath,
                        )
                        .use { stream ->
                            stream
                                .filter { path ->
                                    Files.isRegularFile(
                                        path,
                                        LinkOption.NOFOLLOW_LINKS,
                                    )
                                }
                                .map { path ->
                                    imagePath
                                        .relativize(
                                            path,
                                        )
                                        .toString()
                                        .replace(
                                            '\\',
                                            '/',
                                        )
                                }
                                .toList()
                                .toSet()
                        }

                require(
                    actualFiles ==
                        expectedFiles.keys,
                ) {
                    "Windows runtime image contains files outside the exact release archive"
                }

                val executable =
                    imagePath
                        .resolve(
                            binary.executableRelativePath,
                        )
                        .normalize()
                require(
                    executable.startsWith(
                        imagePath,
                    ) &&
                        observeFile(
                            executable,
                            expectedBytes = null,
                            expectedSha256 = binary.executableSha256,
                        ) ==
                        FileState.VERIFIED,
                ) {
                    "Windows runtime executable does not match the pinned release identity"
                }
            }
            .fold(
                onSuccess = {
                    FileState.VERIFIED
                },
                onFailure = {
                    FileState.INVALID
                },
            )
    }

    private fun normalizedArchiveEntry(
        rawName: String,
        directory: Boolean,
    ): String {
        val normalizedName =
            if (
                directory
            ) {
                rawName.removeSuffix(
                    "/",
                )
            } else {
                rawName
            }

        require(
            normalizedName.isNotBlank() &&
                !normalizedName.startsWith(
                    "/",
                ) &&
                !normalizedName.startsWith(
                    "\\",
                ) &&
                !normalizedName.contains(
                    "\\",
                ) &&
                !normalizedName.contains(
                    "//",
                ) &&
                normalizedName
                    .split(
                        "/",
                    )
                    .none { segment ->
                        segment ==
                            ".." ||
                            segment.isBlank()
                    },
        ) {
            "Windows runtime archive contains an unsafe path: $rawName"
        }

        return normalizedName
    }

    private fun crc32(
        path: Path,
    ): Long {
        val crc =
            CRC32()
        Files.newInputStream(
                path,
            )
            .use { input ->
                val buffer =
                    ByteArray(
                        1024 *
                            1024,
                    )
                while (
                    true
                ) {
                    val count =
                        input.read(
                            buffer,
                        )
                    if (
                        count <
                        0
                    ) {
                        break
                    }
                    crc.update(
                        buffer,
                        0,
                        count,
                    )
                }
            }
        return crc.value
    }

    private fun observeFile(
        path: Path,
        expectedBytes: Long?,
        expectedSha256: String,
    ): FileState {
        if (
            !Files.exists(
                path,
                LinkOption.NOFOLLOW_LINKS,
            )
        ) {
            return FileState.ABSENT
        }

        if (
            !Files.isRegularFile(
                path,
                LinkOption.NOFOLLOW_LINKS,
            ) ||
            Files.isSymbolicLink(path)
        ) {
            return FileState.INVALID
        }

        if (
            expectedBytes != null &&
            Files.size(path) != expectedBytes
        ) {
            return FileState.INVALID
        }

        return if (sha256(path) == expectedSha256) {
            FileState.VERIFIED
        } else {
            FileState.INVALID
        }
    }

    private fun currentHome(
        productHome: Path,
    ): Path {
        val observed =
            requireNotNull(
                VexLifeProdHomeFiles.observeExisting(
                    productHome,
                ),
            ) {
                "Windows runtime materialization requires one existing canonical Vex Home"
            }

        return VexOwnedPath.requireRoot(
            Path.of(observed.productHome),
            "VexLife-Prod Home",
        )
    }

    private fun requireWindowsRealization(
        realization: HereticRuntimeRealization.Value,
    ) {
        require(
            realization.target ==
                HereticRuntimeRealization.WindowsX64,
        ) {
            "Windows runtime materialization requires the exact Windows x64 target"
        }
        require(realization.runtimeBinary != null) {
            "Windows runtime materialization requires an explicit runtime-binary dependency"
        }
    }

    private fun sha256(
        path: Path,
    ): String {
        val digest =
            MessageDigest.getInstance(
                "SHA-256",
            )
        Files.newInputStream(path).use { input ->
            val buffer = ByteArray(1024 * 1024)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest
            .digest()
            .joinToString("") { byte ->
                "%02x".format(byte.toInt() and 0xff)
            }
    }

    private fun deleteTreeNoFollow(
        root: Path,
    ) {
        Files.walk(root).use { stream ->
            stream
                .toList()
                .sortedByDescending(Path::getNameCount)
                .forEach(Files::deleteIfExists)
        }
    }
}

// [VXG RealForever]
