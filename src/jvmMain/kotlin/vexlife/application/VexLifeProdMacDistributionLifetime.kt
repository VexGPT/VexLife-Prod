package vexlife.application

import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import javax.xml.parsers.DocumentBuilderFactory
import kotlinx.serialization.Serializable
import org.w3c.dom.Element
import org.w3c.dom.Node

/**
 * macOS lifetime owner for transient VexLife distribution mounts.
 *
 * This owner is deliberately narrower than installation, Product-service lifetime, Home, or model
 * runtime lifetime. It may detach only a mounted disk image that re-proves itself as a source-bound
 * VexLife distribution, and it never deletes the person's .dmg or .app files.
 */
object VexLifeProdMacDistributionLifetime {
    const val ResultSchema = "vexlife-prod.macos-distribution-lifetime/v1"
    const val DiskImageName = "VexLife-macOS-AppleSilicon.dmg"
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
        DetachedVerifiedRetiredMount,
        AlreadyAbsent,
        HeldUnverifiedDistribution,
        HeldIdentityChanged,
        HeldDetachFailure,
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
                        it.disposition == EntryDisposition.DetachedVerifiedRetiredMount
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
    )

    data class CommandResult(
        val status: Int,
        val stdout: String,
        val stderr: String,
    )

    interface Host {
        fun attachedImages(): List<AttachedImage>

        fun applicationIdentity(mountPoint: Path): ApplicationIdentity?

        fun detach(mountPoint: Path): CommandResult
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
            return parseHdiutilInfoPlist(stdout)
        }

        override fun applicationIdentity(mountPoint: Path): ApplicationIdentity? {
            val app = mountPoint.resolve(ApplicationName)
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
            if (bundleIdentifier != BundleIdentifier || !GitSha.matches(sourceSha)) return null
            return ApplicationIdentity(bundleIdentifier = bundleIdentifier, sourceSha = sourceSha)
        }

        override fun detach(mountPoint: Path): CommandResult =
            runCommand(
                listOf(
                    "/usr/bin/hdiutil",
                    "detach",
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

        private fun runCommand(command: List<String>): CommandResult {
            val process = ProcessBuilder(command).start()
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
                                mountPoint.toAbsolutePath().normalize(),
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
                        host = host,
                    )
                }

        return Result(
            state = ResultState.Reconciled,
            entries = entries,
            effectsPerformed =
                entries.any {
                    it.disposition == EntryDisposition.DetachedVerifiedRetiredMount
                },
        )
    }

    private fun reconcileMount(
        expectedImagePath: Path,
        mountPoint: Path,
        executionRoot: Path,
        host: Host,
    ): Entry {
        val identity =
            runCatching {
                    host.applicationIdentity(mountPoint)
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
                    host.applicationIdentity(mountPoint)
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

        val detached =
            runCatching {
                    host.detach(mountPoint)
                }
                .getOrElse { error ->
                    return held(
                        expectedImagePath,
                        mountPoint,
                        identity.sourceSha,
                        EntryDisposition.HeldDetachFailure,
                        "normal disk-image detach could not run: " + error.javaClass.simpleName,
                    )
                }

        if (detached.status != 0) {
            return held(
                expectedImagePath,
                mountPoint,
                identity.sourceSha,
                EntryDisposition.HeldDetachFailure,
                listOf(detached.stderr, detached.stdout)
                    .joinToString("\n")
                    .trim()
                    .ifBlank {
                        "hdiutil detach returned status " + detached.status
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
                        EntryDisposition.HeldDetachFailure,
                        "detach returned success but absence could not be re-observed",
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
                EntryDisposition.HeldDetachFailure,
                "detach returned success but the exact mount remains present",
            )
        }

        return Entry(
                imagePath = expectedImagePath.toString(),
                mountPoint = mountPoint.toString(),
                sourceSha = identity.sourceSha,
                disposition = EntryDisposition.DetachedVerifiedRetiredMount,
            )
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

    internal fun parseHdiutilInfoPlist(bytes: ByteArray): List<AttachedImage> {
        val factory =
            DocumentBuilderFactory.newInstance().apply {
                setFeature("http://xml.org/sax/features/external-general-entities", false)
                setFeature("http://xml.org/sax/features/external-parameter-entities", false)
                setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
                isXIncludeAware = false
                isExpandEntityReferences = false
            }
        val document =
            factory
                .newDocumentBuilder()
                .parse(ByteArrayInputStream(bytes))
        val root =
            document.documentElement.elementChildren().singleOrNull {
                it.tagName == "dict"
            } ?: error("hdiutil plist does not contain one root dictionary")
        val images =
            root.valueForKey("images")?.takeIf {
                it.tagName == "array"
            } ?: return emptyList()

        return images
            .elementChildren()
            .filter {
                it.tagName == "dict"
            }
            .mapNotNull { image ->
                val imagePath =
                    image.valueForKey("image-path")
                        ?.takeIf {
                            it.tagName == "string"
                        }
                        ?.textContent
                        ?.trim()
                        ?.takeIf {
                            it.isNotEmpty()
                        }
                        ?.let(Path::of)
                        ?.takeIf(Path::isAbsolute)
                        ?.normalize()
                        ?: return@mapNotNull null
                val entities =
                    image.valueForKey("system-entities")
                        ?.takeIf {
                            it.tagName == "array"
                        }
                        ?.elementChildren()
                        .orEmpty()
                val mounts =
                    entities
                        .filter {
                            it.tagName == "dict"
                        }
                        .mapNotNull { entity ->
                            entity.valueForKey("mount-point")
                                ?.takeIf {
                                    it.tagName == "string"
                                }
                                ?.textContent
                                ?.trim()
                                ?.takeIf {
                                    it.isNotEmpty()
                                }
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

    private fun Element.elementChildren(): List<Element> {
        val result = mutableListOf<Element>()
        for (index in 0 until childNodes.length) {
            val child = childNodes.item(index)
            if (child.nodeType == Node.ELEMENT_NODE) {
                result += child as Element
            }
        }
        return result
    }

    private fun Element.valueForKey(key: String): Element? {
        val children = elementChildren()
        var index = 0
        while (index + 1 < children.size) {
            if (children[index].tagName == "key" && children[index].textContent == key) {
                return children[index + 1]
            }
            index += 1
        }
        return null
    }
}

// [VXG RealForever]
