package vexlife.platform.windows

import java.io.BufferedInputStream
import java.io.InputStream
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.Locale
import java.util.UUID
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

    data class ModelArtifactResult(
        val observation: Observation,
        val disposition: String,
    ) {
        init {
            require(
                observation.modelState ==
                    FileState.VERIFIED,
            ) {
                "Windows model artifact result requires the exact declared model bytes"
            }
        }
    }

    enum class ModelRemovalState {
        ALREADY_ABSENT,
        REMOVED,
    }

    data class ModelRemoval(
        val state: ModelRemovalState,
        val modelPath: String,
        val removedFiles: Int,
        val freedBytes: Long,
    )

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

    /**
     * Materialize only the exact model artifact declared by this Windows realization.
     *
     * Settings -> Models owns a human "Download model" effect. Runtime archive/extraction remains a
     * separate prerequisite owned by full runtime convergence.
     */
    fun ensureModelArtifact(
        productHome: Path,
        allowAcquisition: Boolean,
        realization: HereticRuntimeRealization.Value =
            HereticRuntimeRealization.WindowsQwen4B,
        transport: DirectHttpsArtifactDelivery.Transport =
            DirectHttpsArtifactDelivery.JdkTransport,
        onProgress: (DirectHttpsArtifactDelivery.Progress) -> Unit = {},
    ): ModelArtifactResult {
        requireWindowsRealization(
            realization,
        )
        val before =
            observe(
                productHome = productHome,
                realization = realization,
            )
        require(
            before.modelState !=
                FileState.INVALID,
        ) {
            "existing Windows model artifact is not the exact declared realization"
        }

        if (
            before.modelState ==
                FileState.VERIFIED
        ) {
            return ModelArtifactResult(
                observation = before,
                disposition = "REUSED_VERIFIED_MODEL",
            )
        }

        require(
            allowAcquisition,
        ) {
            "Windows model artifact is missing and acquisition authority was not granted"
        }

        val home =
            currentHome(
                productHome,
            )
        val artifact =
            realization.artifact
        val modelUrl =
            requireNotNull(
                artifact.directHttpsUrl,
            ) {
                "Windows model realization lacks one exact HTTPS acquisition source"
            }
        val modelPath =
            Path.of(
                before.modelPath,
            )
        VexOwnedPath.ensureParent(
            home,
            modelPath,
        )

        val delivered =
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
                onProgress = onProgress,
            )

        val after =
            observe(
                productHome = home,
                realization = realization,
            )
        require(
            after.modelState ==
                FileState.VERIFIED
        ) {
            "Windows model artifact materialization did not converge to exact verified state"
        }

        return ModelArtifactResult(
            observation = after,
            disposition = delivered.disposition,
        )
    }

    /**
     * Remove only this realization's model bytes.
     *
     * The llama.cpp archive/image may be shared runtime infrastructure and is intentionally not
     * removed by a human "Remove model" Control.
     */
    fun removeModelArtifact(
        productHome: Path,
        realization: HereticRuntimeRealization.Value =
            HereticRuntimeRealization.WindowsQwen4B,
    ): ModelRemoval {
        requireWindowsRealization(
            realization,
        )
        val home =
            currentHome(
                productHome,
            )
        val observation =
            observe(
                productHome = home,
                realization = realization,
            )
        val modelPath =
            Path.of(
                observation.modelPath,
            )
        val partialPath =
            modelPath.resolveSibling(
                modelPath.fileName.toString() +
                    ".partial",
            )

        var removedFiles =
            0
        var freedBytes =
            0L

        for (
            path in
            listOf(
                modelPath,
                partialPath,
            )
        ) {
            if (
                !Files.exists(
                    path,
                    LinkOption.NOFOLLOW_LINKS,
                )
            ) {
                continue
            }
            require(
                Files.isRegularFile(
                    path,
                    LinkOption.NOFOLLOW_LINKS,
                ) &&
                    !Files.isSymbolicLink(
                        path,
                    ),
            ) {
                "Windows model removal refuses non-regular or symlink artifact path: $path"
            }
            freedBytes +=
                Files.size(
                    path,
                )
            Files.delete(
                path,
            )
            removedFiles +=
                1
        }

        val parent =
            modelPath.parent
        if (
            parent !=
                null &&
            Files.isDirectory(
                parent,
                LinkOption.NOFOLLOW_LINKS,
            ) &&
            !Files.isSymbolicLink(
                parent,
            )
        ) {
            val empty =
                Files.list(
                    parent,
                )
                    .use {
                        !it.findAny()
                            .isPresent
                    }
            if (
                empty
            ) {
                Files.delete(
                    parent,
                )
            }
        }

        return ModelRemoval(
            state =
                if (
                    removedFiles ==
                        0
                ) {
                    ModelRemovalState.ALREADY_ABSENT
                } else {
                    ModelRemovalState.REMOVED
                },
            modelPath = modelPath.toString(),
            removedFiles = removedFiles,
            freedBytes = freedBytes,
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
                        require(
                            seen.add(
                                normalizedName.lowercase(
                                    Locale.ROOT,
                                ),
                            ),
                        ) {
                            "Windows runtime archive contains a duplicate Windows path: $rawName"
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
                    linkedMapOf<String, Pair<Long, String>>()
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
                                        normalizedName.lowercase(
                                            Locale.ROOT,
                                        ),
                                    ),
                                ) {
                                    "Windows runtime archive contains a duplicate Windows path: " +
                                        entry.name
                                }

                                if (
                                    entry.isDirectory
                                ) {
                                    expectedDirectories +=
                                        normalizedName
                                } else {
                                    require(
                                        entry.size >= 0,
                                    ) {
                                        "Windows runtime archive entry lacks exact size: " +
                                            entry.name
                                    }
                                    expectedFiles[
                                        normalizedName
                                    ] =
                                        entry.size to
                                            zip.getInputStream(
                                                    entry,
                                                )
                                                .use(
                                                    ::sha256,
                                                )
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
                            sha256(
                                file,
                            ) ==
                            expected.second,
                    ) {
                        "Windows runtime image file drift: $relative"
                    }
                }

                val actualEntries =
                    Files.walk(
                            imagePath,
                        )
                        .use { stream ->
                            stream
                                .toList()
                        }

                actualEntries
                    .filter {
                        it !=
                            imagePath
                    }
                    .forEach { path ->
                        require(
                            !Files.isSymbolicLink(
                                path,
                            ) &&
                                (
                                    Files.isDirectory(
                                        path,
                                        LinkOption.NOFOLLOW_LINKS,
                                    ) ||
                                        Files.isRegularFile(
                                            path,
                                            LinkOption.NOFOLLOW_LINKS,
                                        )
                                ),
                        ) {
                            "Windows runtime image contains an unsupported filesystem entry"
                        }
                    }

                val actualFiles =
                    actualEntries
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
                        .toSet()

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

        val segments =
            normalizedName
                .split(
                    "/",
                )

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
                segments.none { segment ->
                    !windowsArchiveSegmentSafe(
                        segment,
                    )
                },
        ) {
            "Windows runtime archive contains an unsafe path: $rawName"
        }

        return normalizedName
    }

    private fun windowsArchiveSegmentSafe(
        segment: String,
    ): Boolean {
        if (
            segment.isBlank() ||
            segment ==
                "." ||
            segment ==
                ".." ||
            segment.contains(
                ":",
            ) ||
            segment.endsWith(
                ".",
            ) ||
            segment.endsWith(
                " ",
            )
        ) {
            return false
        }

        val deviceStem =
            segment
                .substringBefore(
                    ".",
                )
                .uppercase(
                    Locale.ROOT,
                )

        return deviceStem !in
            WindowsReservedDeviceNames
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
    ): String =
        Files.newInputStream(
                path,
            )
            .use(
                ::sha256,
            )

    private fun sha256(
        input: InputStream,
    ): String {
        val digest =
            MessageDigest.getInstance(
                "SHA-256",
            )
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
            digest.update(
                buffer,
                0,
                count,
            )
        }
        return digest
            .digest()
            .joinToString("") { byte ->
                "%02x".format(
                    byte.toInt() and
                        0xff,
                )
            }
    }

    private val WindowsReservedDeviceNames =
        buildSet {
            addAll(
                listOf(
                    "CON",
                    "PRN",
                    "AUX",
                    "NUL",
                ),
            )
            (1..9).forEach { index ->
                add(
                    "COM$index",
                )
                add(
                    "LPT$index",
                )
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
