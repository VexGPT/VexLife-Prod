package vexlife.sources

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.security.MessageDigest
import java.time.Instant
import kotlinx.serialization.Serializable
import vexlife.contract.VexDurableContractFile
import vexlife.filesystem.VexOwnedPath
import vexlife.home.VexLifeProdHomeFiles

/**
 * Durable custody for folders the person explicitly granted to Local Sources.
 *
 * Observing or picking a folder never mutates this store. Only [add] and [remove] change the grant
 * set. Stored paths are canonical real directories; reach revalidates them before every use.
 */
class VexLifeLocalSourceStore(
    productHome: Path,
    private val personHome: Path? =
        System.getProperty(
            "user.home",
        )
            ?.takeIf {
                it.isNotBlank()
            }
            ?.let(
                Path::of,
            ),
    private val now: () -> Instant =
        Instant::now,
) {
    @Serializable
    private data class Persisted(
        val schemaVersion: String,
        val grants: List<VexLifeLocalSourcesFamily.Grant>,
    ) {
        init {
            require(
                schemaVersion ==
                    Schema &&
                    grants.size <=
                    VexLifeLocalSourcesFamily.MaximumGrants &&
                    grants.map {
                        it.sourceRef
                    }
                        .distinct()
                        .size ==
                    grants.size,
            ) {
                "local sources persisted grant set is invalid"
            }
        }
    }

    sealed interface Outcome {
        data class Realized(
            val grants: List<VexLifeLocalSourcesFamily.Grant>,
        ) : Outcome

        data class Refused(
            val reasonKey: String,
            val grants: List<VexLifeLocalSourcesFamily.Grant>,
        ) : Outcome
    }

    private val home =
        VexOwnedPath.requireRoot(
            productHome,
            "canonical VexLife Home",
        )

    init {
        requireNotNull(
            VexLifeProdHomeFiles.observeExisting(
                home,
            ),
        ) {
            "Local Sources require one canonical VexLife Home"
        }
    }

    fun grants(): List<VexLifeLocalSourcesFamily.Grant> =
        VexDurableContractFile.read<Persisted>(
            ownedRoot = home,
            path = file(),
        )
            ?.grants
            .orEmpty()

    fun state(): VexLifeLocalSourcesFamily.State =
        VexLifeLocalSourcesFamily.state(
            grants(),
        )

    /**
     * Reobserve the exact real folder for [sourceRef].
     *
     * A moved, removed, linked, or otherwise changed root is unavailable rather than silently
     * rebound to another directory.
     */
    fun root(
        sourceRef: String,
    ): Path? {
        if (
            !VexLifeLocalSourcesFamily.isSourceRef(
                sourceRef,
            )
        ) {
            return null
        }

        val grant =
            grants()
                .singleOrNull {
                    it.sourceRef ==
                        sourceRef
                }
                ?: return null
        val recorded =
            runCatching {
                Path.of(
                    grant.path,
                )
            }
                .getOrNull()
                ?: return null
        if (
            !recorded.isAbsolute ||
            Files.isSymbolicLink(
                recorded,
            ) ||
            !Files.isDirectory(
                recorded,
                LinkOption.NOFOLLOW_LINKS,
            )
        ) {
            return null
        }

        val real =
            runCatching {
                recorded.toRealPath()
            }
                .getOrNull()
                ?: return null

        return real.takeIf {
            it ==
                recorded.normalize()
        }
    }

    fun add(
        selectedPath: Path,
    ): Outcome {
        val current =
            grants()

        fun refused(
            key: String,
        ): Outcome =
            Outcome.Refused(
                reasonKey = key,
                grants = current,
            )

        if (
            !selectedPath.isAbsolute
        ) {
            return refused(
                RefusedNotAbsolute,
            )
        }

        val normalized =
            selectedPath.normalize()
        if (
            Files.isSymbolicLink(
                normalized,
            )
        ) {
            return refused(
                RefusedLink,
            )
        }
        if (
            !Files.isDirectory(
                normalized,
                LinkOption.NOFOLLOW_LINKS,
            )
        ) {
            return refused(
                RefusedNotFolder,
            )
        }

        val real =
            runCatching {
                normalized.toRealPath()
            }
                .getOrNull()
                ?: return refused(
                    RefusedNotFolder,
                )
        val noFollowReal =
            runCatching {
                normalized.toRealPath(
                    LinkOption.NOFOLLOW_LINKS,
                )
            }
                .getOrNull()
                ?: return refused(
                    RefusedNotFolder,
                )

        if (
            real !=
            noFollowReal
        ) {
            return refused(
                RefusedLink,
            )
        }

        val person =
            personHome
                ?.let {
                    runCatching {
                        it.toRealPath()
                    }
                        .getOrNull()
                }
        if (
            real.parent ==
                null ||
            (
                person !=
                    null &&
                    real ==
                    person
            )
        ) {
            return refused(
                RefusedTooBroad,
            )
        }

        val vexHome =
            home.toRealPath()
        if (
            real.startsWith(
                vexHome,
            ) ||
            vexHome.startsWith(
                real,
            )
        ) {
            return refused(
                RefusedOverlapsVexHome,
            )
        }

        val sourceRef =
            sourceRefFor(
                real,
            )
        if (
            current.any {
                it.sourceRef ==
                    sourceRef
            }
        ) {
            return refused(
                RefusedAlreadyGranted,
            )
        }
        if (
            current.size >=
            VexLifeLocalSourcesFamily.MaximumGrants
        ) {
            return refused(
                RefusedLimitReached,
            )
        }

        val next =
            current +
                VexLifeLocalSourcesFamily.Grant(
                    sourceRef = sourceRef,
                    name =
                        real.fileName
                            ?.toString()
                            ?.takeIf {
                                it.isNotBlank()
                            }
                            ?: real.toString(),
                    path = real.toString(),
                    grantedAt = now().toString(),
                )

        write(
            next,
        )
        return Outcome.Realized(
            grants = next,
        )
    }

    fun remove(
        sourceRef: String,
    ): Outcome {
        val current =
            grants()
        if (
            current.none {
                it.sourceRef ==
                    sourceRef
            }
        ) {
            return Outcome.Refused(
                reasonKey = RefusedNotGranted,
                grants = current,
            )
        }

        val next =
            current.filterNot {
                it.sourceRef ==
                    sourceRef
            }
        write(
            next,
        )
        return Outcome.Realized(
            grants = next,
        )
    }

    private fun write(
        grants: List<VexLifeLocalSourcesFamily.Grant>,
    ) {
        VexDurableContractFile.write(
            ownedRoot = home,
            path = file(),
            value =
                Persisted(
                    schemaVersion = Schema,
                    grants = grants,
                ),
        )
        require(
            grants() ==
                grants,
        ) {
            "Local Sources did not reobserve the exact persisted grant set"
        }
    }

    private fun file(): Path =
        VexOwnedPath.resolveInside(
            home,
            RelativePath,
        )

    private fun sourceRefFor(
        real: Path,
    ): String {
        val digest =
            MessageDigest
                .getInstance(
                    "SHA-256",
                )
                .digest(
                    real
                        .toString()
                        .toByteArray(
                            StandardCharsets.UTF_8,
                        ),
                )
                .joinToString(
                    "",
                ) {
                    "%02x".format(
                        it.toInt() and
                            0xff,
                    )
                }
        return VexLifeLocalSourcesFamily.sourceRefForSha256(
            digest,
        )
    }

    companion object {
        const val Schema =
            "vexlife-prod.local-sources/v1"

        const val RelativePath =
            "runtime/vexlife-prod/settings/local-sources.json"

        const val RefusedNotAbsolute =
            "workspace.status.local-sources-refused-not-absolute"

        const val RefusedNotFolder =
            "workspace.status.local-sources-refused-not-a-folder"

        const val RefusedLink =
            "workspace.status.local-sources-refused-link"

        const val RefusedTooBroad =
            "workspace.status.local-sources-refused-too-broad"

        const val RefusedOverlapsVexHome =
            "workspace.status.local-sources-refused-overlaps-vex-home"

        const val RefusedAlreadyGranted =
            "workspace.status.local-sources-refused-already-granted"

        const val RefusedLimitReached =
            "workspace.status.local-sources-refused-limit-reached"

        const val RefusedNotGranted =
            "workspace.status.local-sources-refused-not-granted"
    }
}

// [VXG RealForever]
