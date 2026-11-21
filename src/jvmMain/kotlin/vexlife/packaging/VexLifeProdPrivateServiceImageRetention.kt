package vexlife.packaging

import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path

/**
 * Bounded storage owner for already-installed private Product service images.
 *
 * A source-bound image remains self-contained. This owner does not split Java or JAR dependencies
 * into a second shared runtime. It reclaims older installed images only after one exact current
 * image has already been installed and the caller has proved the Product service current.
 *
 * One verified retired image may remain pinned as the stable Mac Java provider. Retaining that
 * exact provider image preserves the already-working Java executable path across Product source
 * rotation; it does not make the provider image semantically current Product source.
 *
 * Deletion authority is intentionally narrower than directory discovery: an older source directory
 * is reclaimable only when the ordinary private-image verifier can still prove its complete
 * manifest/artifact/runtime/payload identity. Unknown or invalid entries are held in place.
 */
object VexLifeProdPrivateServiceImageRetention {
    private val GitSha = Regex("[0-9a-f]{40}")

    enum class EntryDisposition {
        RetainedCurrent,
        RetainedJavaProvider,
        ReclaimedVerifiedRetiredImage,
        HeldUnrecognizedEntry,
        HeldUnverifiedImage,
        HeldDeleteFailure,
    }

    data class Entry(
        val name: String,
        val sourceSha: String?,
        val disposition: EntryDisposition,
        val bytes: Long?,
    )

    data class Result(
        val currentSourceSha: String,
        val currentPayloadSha256: String,
        val runtimeModules: List<String>,
        val entries: List<Entry>,
        val reclaimedBytes: Long,
        val effectsPerformed: Boolean,
    ) {
        val reclaimedSourceShas: List<String> =
            entries
                .filter { it.disposition == EntryDisposition.ReclaimedVerifiedRetiredImage }
                .mapNotNull(Entry::sourceSha)
    }

    fun reclaimRetiredVerifiedImages(
        productHome: Path,
        currentSourceSha: String,
        currentPayloadSha256: String,
        javaProviderSourceSha: String? = null,
    ): Result {
        require(GitSha.matches(currentSourceSha)) {
            "private service-image retention requires one exact current source SHA"
        }
        require(currentPayloadSha256.matches(Regex("[0-9a-f]{64}"))) {
            "private service-image retention requires one exact current payload SHA-256"
        }
        require(
            javaProviderSourceSha ==
                null ||
                GitSha.matches(
                    javaProviderSourceSha,
                ),
        ) {
            "private service-image retention Java provider source must be one exact Git SHA"
        }

        val currentImage =
            VexLifeProdPrivateServiceImageInstaller.installedImagePath(
                productHome = productHome,
                sourceSha = currentSourceSha,
            )
        val current =
            VexLifeProdPrivateServiceImage.observe(
                imageRoot = currentImage,
                expectedSourceSha = currentSourceSha,
            )
        require(current.payloadSha256 == currentPayloadSha256) {
            "current private service image differs before dependency retention"
        }

        val imageBase =
            requireNotNull(currentImage.parent) {
                "current private service image has no image-base parent"
            }
        val entries =
            Files.list(imageBase).use { stream ->
                stream.sorted().toList()
            }

        val observations =
            entries.map { entry ->
                when {
                    entry == currentImage ->
                        Entry(
                            name = entry.fileName.toString(),
                            sourceSha = currentSourceSha,
                            disposition = EntryDisposition.RetainedCurrent,
                            bytes = treeBytes(entry),
                        )

                    javaProviderSourceSha !=
                            null &&
                        entry.fileName
                            .toString() ==
                            javaProviderSourceSha ->
                        retainVerifiedJavaProvider(
                            entry,
                        )

                    !GitSha.matches(entry.fileName.toString()) ||
                        Files.isSymbolicLink(entry) ||
                        !Files.isDirectory(entry, LinkOption.NOFOLLOW_LINKS) ->
                        Entry(
                            name = entry.fileName.toString(),
                            sourceSha = null,
                            disposition = EntryDisposition.HeldUnrecognizedEntry,
                            bytes = treeBytesOrNull(entry),
                        )

                    else ->
                        reclaimVerifiedRetiredImage(entry)
                }
            }

        val after =
            VexLifeProdPrivateServiceImage.observe(
                imageRoot = currentImage,
                expectedSourceSha = currentSourceSha,
            )
        require(after.payloadSha256 == currentPayloadSha256) {
            "current private service image changed during dependency retention"
        }

        val reclaimedBytes =
            observations
                .filter { it.disposition == EntryDisposition.ReclaimedVerifiedRetiredImage }
                .sumOf { it.bytes ?: 0L }

        return Result(
            currentSourceSha = currentSourceSha,
            currentPayloadSha256 = currentPayloadSha256,
            runtimeModules = current.runtimeModules,
            entries = observations,
            reclaimedBytes = reclaimedBytes,
            effectsPerformed = reclaimedBytes > 0,
        )
    }

    private fun retainVerifiedJavaProvider(entry: Path): Entry {
        val sourceSha =
            entry.fileName
                .toString()
        val verified =
            runCatching {
                    VexLifeProdPrivateServiceImage.observe(
                        imageRoot = entry,
                        expectedSourceSha = sourceSha,
                    )
                }
                .getOrNull()
                ?: return Entry(
                    name = sourceSha,
                    sourceSha = sourceSha,
                    disposition = EntryDisposition.HeldUnverifiedImage,
                    bytes = treeBytesOrNull(entry),
                )

        return Entry(
            name = sourceSha,
            sourceSha = verified.sourceSha,
            disposition = EntryDisposition.RetainedJavaProvider,
            bytes = treeBytes(entry),
        )
    }

    private fun reclaimVerifiedRetiredImage(entry: Path): Entry {
        val sourceSha = entry.fileName.toString()
        val verified =
            runCatching {
                    VexLifeProdPrivateServiceImage.observe(
                        imageRoot = entry,
                        expectedSourceSha = sourceSha,
                    )
                }
                .getOrNull()
                ?: return Entry(
                    name = sourceSha,
                    sourceSha = sourceSha,
                    disposition = EntryDisposition.HeldUnverifiedImage,
                    bytes = treeBytesOrNull(entry),
                )

        val bytes = treeBytes(entry)
        val deleted =
            runCatching {
                    deleteTreeNoFollow(entry)
                    !Files.exists(entry, LinkOption.NOFOLLOW_LINKS)
                }
                .getOrDefault(false)

        return Entry(
            name = sourceSha,
            sourceSha = verified.sourceSha,
            disposition =
                if (deleted) {
                    EntryDisposition.ReclaimedVerifiedRetiredImage
                } else {
                    EntryDisposition.HeldDeleteFailure
                },
            bytes = bytes,
        )
    }

    private fun treeBytesOrNull(root: Path): Long? =
        runCatching {
                treeBytes(root)
            }
            .getOrNull()

    private fun treeBytes(root: Path): Long =
        when {
            Files.isRegularFile(root, LinkOption.NOFOLLOW_LINKS) -> Files.size(root)
            !Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS) -> 0L
            else ->
                Files.walk(root).use { stream ->
                    stream
                        .filter { Files.isRegularFile(it, LinkOption.NOFOLLOW_LINKS) }
                        .mapToLong(Files::size)
                        .sum()
                }
        }

    private fun deleteTreeNoFollow(root: Path) {
        Files.walk(root).use { stream ->
            stream
                .toList()
                .sortedByDescending(Path::getNameCount)
                .forEach(Files::deleteIfExists)
        }
    }
}

// [VXG RealForever]
