package vexlife.application

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * macOS lifetime owner for transient VexLife distribution mounts.
 *
 * This owner is deliberately narrower than installation, Product-service lifetime, Home, or model
 * runtime lifetime. It may normally eject only a mounted disk image that re-proves itself as a
 * source-bound VexLife distribution, and it never deletes the person's .dmg or .app files.
 */
object VexLifeProdMacDistributionLifetime {
    const val ResultSchema = "vexlife-prod.macos-distribution-lifetime/v1"
    const val ApplicationName = "VexLife.app"
    const val BundleIdentifier = "com.vexlife.prod"

    private val GitSha = Regex("[0-9a-f]{40}")

    @Serializable
    enum class ResultState {
        Reconciled,
        HeldObservation,
    }

    @Serializable
    enum class EntryDisposition {
        RetainedCurrentExecution,
        EjectedVerifiedRetiredMount,
        AlreadyAbsent,
        RetainedNotProvenRetired,
        HeldUnverifiedDistribution,
        HeldIdentityChanged,
        HeldEjectFailure,
    }

    @Serializable
    data class Entry(
        val imagePath: String,
        val mountPoint: String,
        val sourceSha: String?,
        val disposition: EntryDisposition,
        val detail: String? = null,
    )

    @Serializable
    data class Result(
        val schemaVersion: String = ResultSchema,
        val state: ResultState,
        val entries: List<Entry>,
        val effectsPerformed: Boolean,
        val detail: String? = null,
    ) {
        init {
            require(schemaVersion == ResultSchema) {
                "Mac distribution lifetime result schema is invalid"
            }
            require(
                effectsPerformed ==
                    entries.any {
                        it.disposition == EntryDisposition.EjectedVerifiedRetiredMount
                    }
            ) {
                "Mac distribution lifetime effect accounting is inconsistent"
            }
        }
    }

    data class AttachedImage(
        val imagePath: Path,
        val mountPoints: List<Path>,
    )

    data class ApplicationIdentity(
        val bundleIdentifier: String,
        val sourceSha: String,
        val buildVersion: Long,
    ) {
        init {
            require(bundleIdentifier == BundleIdentifier && GitSha.matches(sourceSha) && buildVersion > 0) {
                "VexLife application identity is invalid"
            }
        }
    }

    data class CommandResult(
        val status: Int,
        val stdout: String,
        val stderr: String,
    )

    interface Host {
        fun attachedImages(): List<AttachedImage>

        fun applicationIdentity(applicationBundle: Path): ApplicationIdentity?

        fun eject(mountPoint: Path): CommandResult
    }

    object SystemHost : Host {
        override fun attachedImages(): List<AttachedImage> {
            val process =
                ProcessBuilder(
                        "/usr/bin/hdiutil",
                        "info",
                        "-plist",
                    )
                    .start()
            val stdout = process.inputStream.readBytes()
            val stderr = process.errorStream.readBytes().toString(StandardCharsets.UTF_8)
            val status = process.waitFor()
            require(status == 0) {
                "hdiutil info failed: " + stderr.trim()
            }
            val converted =
                runCommandWithInput(
                    command =
                        listOf(
                            "/usr/bin/plutil",
                            "-convert",
                            "json",
                            "-o",
                            "-",
                            "-",
                        ),
                    input = stdout,
                )
            require(converted.status == 0) {
                "plutil could not project hdiutil plist as JSON: " + converted.stderr.trim()
            }
            return parseHdiutilInfoJson(converted.stdout)
        }

        override fun applicationIdentity(applicationBundle: Path): ApplicationIdentity? {
            val app = applicationBundle.toAbsolutePath().normalize()
            val infoPlist = app.resolve("Contents/Info.plist")
            if (
                !Files.isDirectory(app, LinkOption.NOFOLLOW_LINKS) ||
                    Files.isSymbolicLink(app) ||
                    !Files.isRegularFile(infoPlist, LinkOption.NOFOLLOW_LINKS) ||
                    Files.isSymbolicLink(infoPlist)
            ) {
                return null
            }

            val bundleIdentifier = plistString(infoPlist, "CFBundleIdentifier") ?: return null
            val sourceSha = plistString(infoPlist, "VexLifeProdExpectedSourceSha") ?: return null
            val buildVersion = plistString(infoPlist, "CFBundleVersion")?.toLongOrNull() ?: return null
            if (bundleIdentifier != BundleIdentifier || !GitSha.matches(sourceSha) || buildVersion <= 0) return null
            return ApplicationIdentity(
                bundleIdentifier = bundleIdentifier,
                sourceSha = sourceSha,
                buildVersion = buildVersion,
            )
        }

        override fun eject(mountPoint: Path): CommandResult =
            runCommand(
                listOf(
                    "/usr/sbin/diskutil",
                    "eject",
                    mountPoint.toString(),
                )
            )

        private fun plistString(path: Path, key: String): String? {
            val result =
                runCommand(
                    listOf(
                        "/usr/bin/plutil",
                        "-extract",
                        key,
                        "raw",
                        "-o",
                        "-",
                        path.toString(),
                    )
                )
            return result.stdout.trim().takeIf { result.status == 0 && it.isNotEmpty() }
        }

        private fun runCommand(command: List<String>): CommandResult =
            runCommandWithInput(command = command, input = null)

        private fun runCommandWithInput(
            command: List<String>,
            input: ByteArray?,
        ): CommandResult {
            val process = ProcessBuilder(command).start()
            if (input != null) {
                process.outputStream.use {
                    it.write(input)
                }
            } else {
                process.outputStream.close()
            }
            val stdout = process.inputStream.readBytes().toString(StandardCharsets.UTF_8)
            val stderr = process.errorStream.readBytes().toString(StandardCharsets.UTF_8)
            return CommandResult(
                status = process.waitFor(),
                stdout = stdout,
                stderr = stderr,
            )
        }
    }

    fun reconcile(
        currentImageRoot: Path,
        host: Host = SystemHost,
    ): Result {
        val executionRoot = currentImageRoot.toAbsolutePath().normalize()
        val currentApplication =
            enclosingApplication(executionRoot)
                ?.let { app ->
                    runCatching {
                            host.applicationIdentity(app)
                        }
                        .getOrNull()
                }
                ?: return Result(
                    state = ResultState.HeldObservation,
                    entries = emptyList(),
                    effectsPerformed = false,
                    detail = "current setup process is not executing from one source-bound VexLife.app",
                )
        val initial =
            runCatching {
                    host.attachedImages()
                }
                .getOrElse { error ->
                    return Result(
                        state = ResultState.HeldObservation,
                        entries = emptyList(),
                        effectsPerformed = false,
                        detail = error.javaClass.simpleName + ": " + (error.message ?: ""),
                    )
                }

        val entries =
            initial
                .flatMap { image ->
                    image.mountPoints.map { mountPoint ->
                        image.imagePath to mountPoint
                    }
                }
                .filter { (_, mountPoint) ->
                    runCatching {
                            host.applicationIdentity(
                                mountPoint.toAbsolutePath().normalize().resolve(ApplicationName),
                            )
                        }
                        .getOrNull() != null
                }
                .sortedBy { (_, mountPoint) -> mountPoint.toString() }
                .map { (imagePath, mountPoint) ->
                    reconcileMount(
                        expectedImagePath = imagePath.toAbsolutePath().normalize(),
                        mountPoint = mountPoint.toAbsolutePath().normalize(),
                        executionRoot = executionRoot,
                        currentApplication = currentApplication,
                        host = host,
                    )
                }

        return Result(
            state = ResultState.Reconciled,
            entries = entries,
            effectsPerformed =
                entries.any {
                    it.disposition == EntryDisposition.EjectedVerifiedRetiredMount
                },
        )
    }

    private fun reconcileMount(
        expectedImagePath: Path,
        mountPoint: Path,
        executionRoot: Path,
        currentApplication: ApplicationIdentity,
        host: Host,
    ): Entry {
        val identity =
            runCatching {
                    host.applicationIdentity(mountPoint.resolve(ApplicationName))
                }
                .getOrNull()
                ?: return held(
                    expectedImagePath,
                    mountPoint,
                    null,
                    EntryDisposition.HeldUnverifiedDistribution,
                    "mounted image does not re-prove one source-bound VexLife.app",
                )

        if (executionRoot.startsWith(mountPoint)) {
            return Entry(
                    imagePath = expectedImagePath.toString(),
                    mountPoint = mountPoint.toString(),
                    sourceSha = identity.sourceSha,
                    disposition = EntryDisposition.RetainedCurrentExecution,
                )
        }

        if (
            identity.sourceSha != currentApplication.sourceSha &&
                identity.buildVersion >= currentApplication.buildVersion
        ) {
            return Entry(
                imagePath = expectedImagePath.toString(),
                mountPoint = mountPoint.toString(),
                sourceSha = identity.sourceSha,
                disposition = EntryDisposition.RetainedNotProvenRetired,
                detail =
                    "mounted VexLife build " +
                        identity.buildVersion +
                        " is not older than running build " +
                        currentApplication.buildVersion,
            )
        }

        val current =
            runCatching {
                    host.attachedImages()
                }
                .getOrElse { error ->
                    return held(
                        expectedImagePath,
                        mountPoint,
                        identity.sourceSha,
                        EntryDisposition.HeldIdentityChanged,
                        "disk-image currentness could not be re-observed: " +
                            error.javaClass.simpleName,
                    )
                }

        val currentMappings =
            current.flatMap { image ->
                image.mountPoints
                    .filter {
                        it.toAbsolutePath().normalize() == mountPoint
                    }
                    .map {
                        image.imagePath.toAbsolutePath().normalize()
                    }
            }

        if (currentMappings.isEmpty()) {
            return Entry(
                    imagePath = expectedImagePath.toString(),
                    mountPoint = mountPoint.toString(),
                    sourceSha = identity.sourceSha,
                    disposition = EntryDisposition.AlreadyAbsent,
                )
        }

        if (currentMappings.size != 1 || currentMappings.single() != expectedImagePath) {
            return held(
                expectedImagePath,
                mountPoint,
                identity.sourceSha,
                EntryDisposition.HeldIdentityChanged,
                "mount no longer maps to the exact observed VexLife disk image",
            )
        }

        val currentIdentity =
            runCatching {
                    host.applicationIdentity(mountPoint.resolve(ApplicationName))
                }
                .getOrNull()
        if (currentIdentity != identity) {
            return held(
                expectedImagePath,
                mountPoint,
                identity.sourceSha,
                EntryDisposition.HeldIdentityChanged,
                "mounted VexLife application identity changed before detach",
            )
        }

        val ejected =
            runCatching {
                    host.eject(mountPoint)
                }
                .getOrElse { error ->
                    return held(
                        expectedImagePath,
                        mountPoint,
                        identity.sourceSha,
                        EntryDisposition.HeldEjectFailure,
                        "normal disk-image eject could not run: " + error.javaClass.simpleName,
                    )
                }

        if (ejected.status != 0) {
            return held(
                expectedImagePath,
                mountPoint,
                identity.sourceSha,
                EntryDisposition.HeldEjectFailure,
                listOf(ejected.stderr, ejected.stdout)
                    .joinToString("\n")
                    .trim()
                    .ifBlank {
                        "diskutil eject returned status " + ejected.status
                    },
            )
        }

        val stillMounted =
            runCatching {
                    host.attachedImages()
                }
                .getOrElse {
                    return held(
                        expectedImagePath,
                        mountPoint,
                        identity.sourceSha,
                        EntryDisposition.HeldEjectFailure,
                        "eject returned success but absence could not be re-observed",
                    )
                }
                .any { image ->
                    image.mountPoints.any {
                        it.toAbsolutePath().normalize() == mountPoint
                    }
                }

        if (stillMounted) {
            return held(
                expectedImagePath,
                mountPoint,
                identity.sourceSha,
                EntryDisposition.HeldEjectFailure,
                "eject returned success but the exact mount remains present",
            )
        }

        return Entry(
                imagePath = expectedImagePath.toString(),
                mountPoint = mountPoint.toString(),
                sourceSha = identity.sourceSha,
                disposition = EntryDisposition.EjectedVerifiedRetiredMount,
            )
    }

    internal fun enclosingApplication(imageRoot: Path): Path? {
        var cursor: Path? = imageRoot.toAbsolutePath().normalize()
        while (cursor != null) {
            if (cursor.fileName?.toString() == ApplicationName) return cursor
            cursor = cursor.parent
        }
        return null
    }

    private fun held(
        imagePath: Path,
        mountPoint: Path,
        sourceSha: String?,
        disposition: EntryDisposition,
        detail: String,
    ): Entry =
        Entry(
                imagePath = imagePath.toString(),
                mountPoint = mountPoint.toString(),
                sourceSha = sourceSha,
                disposition = disposition,
                detail = detail,
            )

    internal fun parseHdiutilInfoJson(source: String): List<AttachedImage> {
        val root =
            Json.parseToJsonElement(source) as? JsonObject
                ?: error("hdiutil JSON does not contain one root object")
        val images = root["images"] as? JsonArray ?: return emptyList()

        return images.mapNotNull { value ->
            val image = value as? JsonObject ?: return@mapNotNull null
            val imagePath =
                image["image-path"]
                    ?.jsonPrimitive
                    ?.content
                    ?.takeIf { it.isNotBlank() }
                    ?.let(Path::of)
                    ?.takeIf(Path::isAbsolute)
                    ?.normalize()
                    ?: return@mapNotNull null
            val mounts =
                (image["system-entities"] as? JsonArray)
                    .orEmpty()
                    .mapNotNull { entityValue ->
                        val entity = entityValue as? JsonObject ?: return@mapNotNull null
                        entity["mount-point"]
                            ?.jsonPrimitive
                            ?.content
                            ?.takeIf { it.isNotBlank() }
                            ?.let(Path::of)
                            ?.takeIf(Path::isAbsolute)
                            ?.normalize()
                    }
                    .distinct()

            AttachedImage(
                imagePath = imagePath,
                mountPoints = mounts,
            )
        }
    }

}

// [VXG RealForever]
