package vexlife.context

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.security.MessageDigest
import java.time.Instant
import java.util.UUID
import kotlinx.serialization.Serializable
import vexlife.contract.VexContractJson
import vexlife.contract.VexDurableContractFile
import vexlife.filesystem.VexOwnedPath

/**
 * Durable Home owner for named Context Profile revisions and the active selection.
 *
 * Revisions are immutable and content-addressed. head.json says which revision is current for one
 * local profile; active.json says which profile should construct the next accepted Send. Product
 * default is synthetic and needs no persisted profile file.
 */
class VexLifeContextProfileStore(
    productHome: Path,
) {
    companion object {
        const val RootRelativePath = "runtime/vexlife-prod/settings/context-profiles"
        const val RevisionSchema = "vexlife-prod.context-profile-revision/v1"
        const val HeadSchema = "vexlife-prod.context-profile-head/v1"
        const val ActiveSchema = "vexlife-prod.context-profile-active/v1"
    }

    @Serializable
    private data class RevisionCore(
        val schemaVersion: String,
        val profileRef: String,
        val revision: Int,
        val definition: VexLifeContextProfile.Definition,
        val basedOnProductFoundation: VexLifeContextProfile.ProductFoundationIdentity,
        val priorFingerprint: String?,
        val formedAt: String,
    )

    @Serializable
    data class Revision(
        val schemaVersion: String,
        val profileRef: String,
        val revision: Int,
        val definition: VexLifeContextProfile.Definition,
        val basedOnProductFoundation: VexLifeContextProfile.ProductFoundationIdentity,
        val priorFingerprint: String?,
        val formedAt: String,
        val fingerprint: String,
    )

    @Serializable
    private data class Head(
        val schemaVersion: String,
        val profileRef: String,
        val revision: Int,
        val fingerprint: String,
    )

    @Serializable
    private data class Active(
        val schemaVersion: String,
        val profileRef: String,
    )

    private val home = VexOwnedPath.requireRoot(productHome, "canonical VexLife Home")
    private val root = VexOwnedPath.resolveInside(home, RootRelativePath)
    private val profiles = VexOwnedPath.resolveInside(root, "profiles")
    private val activePath = VexOwnedPath.resolveInside(root, "active.json")

    fun directory(): Path = root

    fun readActive(
        productFoundation: VexLifeContextProfile.ProductFoundationIdentity,
    ): VexLifeContextProfile.Effective {
        val active =
            VexDurableContractFile.read<Active>(
                ownedRoot = home,
                path = activePath,
            )
                ?.also(::validateActive)

        if (
            active == null ||
                active.profileRef == VexLifeContextProfile.ProductDefaultRef
        ) {
            return productDefault(productFoundation)
        }

        return effective(
            revision = readCurrentRevision(active.profileRef),
            productFoundation = productFoundation,
        )
    }

    fun listSaved(
        productFoundation: VexLifeContextProfile.ProductFoundationIdentity,
    ): List<VexLifeContextProfile.Effective> {
        if (
            VexOwnedPath.observeDirectory(
                home,
                profiles,
            ) == VexOwnedPath.DirectoryState.Missing
        ) {
            return emptyList()
        }

        return Files.list(profiles).use { stream ->
            stream
                .filter {
                    Files.isDirectory(
                        it,
                        LinkOption.NOFOLLOW_LINKS,
                    ) &&
                        !Files.isSymbolicLink(it)
                }
                .map {
                    path ->
                    effective(
                        revision = readCurrentRevision(path.fileName.toString()),
                        productFoundation = productFoundation,
                    )
                }
                .sorted(
                    compareBy<VexLifeContextProfile.Effective> {
                        it.definition.name.lowercase()
                    }.thenBy {
                        it.profileRef
                    },
                )
                .toList()
        }
    }

    fun save(
        profileRef: String?,
        definition: VexLifeContextProfile.Definition,
        productFoundation: VexLifeContextProfile.ProductFoundationIdentity,
        formedAt: Instant = Instant.now(),
    ): VexLifeContextProfile.Effective {
        val ref =
            profileRef
                ?: (
                    "profile.vexlife.context." +
                        UUID.randomUUID()
                            .toString()
                            .lowercase()
                )
        requireProfileRef(ref)

        val existing =
            if (profileRef == null) {
                null
            } else {
                readHeadOrNull(ref)
                    ?: throw IllegalArgumentException(
                        "context profile $ref does not exist",
                    )
            }

        val core =
            RevisionCore(
                schemaVersion = RevisionSchema,
                profileRef = ref,
                revision = existing?.revision?.plus(1) ?: 1,
                definition = definition,
                basedOnProductFoundation = productFoundation,
                priorFingerprint = existing?.fingerprint,
                formedAt = formedAt.toString(),
            )
        val fingerprint =
            sha256(
                VexContractJson.canonicalEncode(
                    core,
                ),
            )
        val revision =
            Revision(
                schemaVersion = core.schemaVersion,
                profileRef = core.profileRef,
                revision = core.revision,
                definition = core.definition,
                basedOnProductFoundation = core.basedOnProductFoundation,
                priorFingerprint = core.priorFingerprint,
                formedAt = core.formedAt,
                fingerprint = fingerprint,
            )
                .also(::validateRevision)

        val profileRoot = profileRoot(ref)
        val revisions = VexOwnedPath.resolveInside(profileRoot, "revisions")
        VexOwnedPath.ensureDirectory(home, root)
        VexOwnedPath.ensureDirectory(home, profiles)
        VexOwnedPath.ensureDirectory(home, profileRoot)
        VexOwnedPath.ensureDirectory(home, revisions)

        writeImmutableOrRequireExact(
            path =
                VexOwnedPath.resolveInside(
                    revisions,
                    "$fingerprint.json",
                ),
            value = revision,
        )
        VexDurableContractFile.write(
            ownedRoot = home,
            path = VexOwnedPath.resolveInside(profileRoot, "head.json"),
            value =
                Head(
                    schemaVersion = HeadSchema,
                    profileRef = ref,
                    revision = revision.revision,
                    fingerprint = fingerprint,
                ),
        )
        select(ref)

        return effective(
            revision = revision,
            productFoundation = productFoundation,
        )
    }

    fun importAsNew(
        portable: VexLifeContextProfile.Portable,
        productFoundation: VexLifeContextProfile.ProductFoundationIdentity,
        formedAt: Instant = Instant.now(),
    ): VexLifeContextProfile.Effective =
        save(
            profileRef = null,
            definition = portable.definition(),
            productFoundation = productFoundation,
            formedAt = formedAt,
        )

    fun restoreProductDefault(
        productFoundation: VexLifeContextProfile.ProductFoundationIdentity,
    ): VexLifeContextProfile.Effective {
        VexOwnedPath.ensureDirectory(home, root)
        select(VexLifeContextProfile.ProductDefaultRef)
        return productDefault(productFoundation)
    }

    fun portable(
        effective: VexLifeContextProfile.Effective,
    ): VexLifeContextProfile.Portable =
        VexLifeContextProfile.Portable(
            name = effective.definition.name,
            description = effective.definition.description,
            foundationMode = effective.definition.foundationMode,
            foundationText = effective.definition.foundationText,
            processRetention = effective.definition.processRetention,
            basedOnProductFoundation = effective.basedOnProductFoundation,
        )

    private fun productDefault(
        productFoundation: VexLifeContextProfile.ProductFoundationIdentity,
    ): VexLifeContextProfile.Effective {
        val definition =
            VexLifeContextProfile.Definition(
                name = "VexLife Default",
            )
        val fingerprint =
            sha256(
                VexContractJson.canonicalEncode(
                    VexLifeContextProfile.Portable(
                        name = definition.name,
                        description = definition.description,
                        foundationMode = definition.foundationMode,
                        foundationText = definition.foundationText,
                        processRetention = definition.processRetention,
                        basedOnProductFoundation = productFoundation,
                    ),
                ),
            )
        return VexLifeContextProfile.Effective(
            profileRef = VexLifeContextProfile.ProductDefaultRef,
            revision = 0,
            definition = definition,
            basedOnProductFoundation = productFoundation,
            fingerprint = fingerprint,
            formedAt = null,
            productDefault = true,
            productFoundationChangedSinceSave = false,
        )
    }

    private fun effective(
        revision: Revision,
        productFoundation: VexLifeContextProfile.ProductFoundationIdentity,
    ): VexLifeContextProfile.Effective =
        VexLifeContextProfile.Effective(
            profileRef = revision.profileRef,
            revision = revision.revision,
            definition = revision.definition,
            basedOnProductFoundation = revision.basedOnProductFoundation,
            fingerprint = revision.fingerprint,
            formedAt = revision.formedAt,
            productDefault = false,
            productFoundationChangedSinceSave =
                revision.basedOnProductFoundation !=
                    productFoundation,
        )

    private fun select(profileRef: String) {
        require(
            profileRef == VexLifeContextProfile.ProductDefaultRef ||
                readHeadOrNull(profileRef) != null,
        ) {
            "active context profile must be Product default or one saved profile"
        }
        VexDurableContractFile.write(
            ownedRoot = home,
            path = activePath,
            value =
                Active(
                    schemaVersion = ActiveSchema,
                    profileRef = profileRef,
                ),
        )
    }

    private fun readCurrentRevision(
        profileRef: String,
    ): Revision {
        val head =
            requireNotNull(
                readHeadOrNull(profileRef),
            ) {
                "saved context profile $profileRef has no current head"
            }
        return requireNotNull(
            VexDurableContractFile.read<Revision>(
                ownedRoot = home,
                path =
                    VexOwnedPath.resolveInside(
                        VexOwnedPath.resolveInside(
                            profileRoot(profileRef),
                            "revisions",
                        ),
                        "${head.fingerprint}.json",
                    ),
            ),
        ) {
            "saved context profile $profileRef is missing its current revision"
        }
            .also {
                validateRevision(it)
                require(
                    it.profileRef == head.profileRef &&
                        it.revision == head.revision &&
                        it.fingerprint == head.fingerprint,
                ) {
                    "saved context profile head does not bind its exact revision"
                }
            }
    }

    private fun readHeadOrNull(
        profileRef: String,
    ): Head? {
        requireProfileRef(profileRef)
        return VexDurableContractFile.read<Head>(
            ownedRoot = home,
            path =
                VexOwnedPath.resolveInside(
                    profileRoot(profileRef),
                    "head.json",
                ),
        )
            ?.also(::validateHead)
    }

    private fun profileRoot(
        profileRef: String,
    ): Path {
        requireProfileRef(profileRef)
        return VexOwnedPath.resolveInside(
            profiles,
            profileRef,
        )
    }

    private fun validateActive(
        value: Active,
    ) {
        require(
            value.schemaVersion == ActiveSchema,
        ) {
            "context profile active selection schema is not current"
        }
        requireProfileRef(value.profileRef)
    }

    private fun validateHead(
        value: Head,
    ) {
        require(
            value.schemaVersion == HeadSchema &&
                value.revision >= 1 &&
                value.fingerprint.matches(Regex("[0-9a-f]{64}")),
        ) {
            "context profile head is invalid"
        }
        requireProfileRef(value.profileRef)
    }

    private fun validateRevision(
        value: Revision,
    ) {
        require(
            value.schemaVersion == RevisionSchema &&
                value.revision >= 1 &&
                value.formedAt.isNotBlank() &&
                value.fingerprint.matches(Regex("[0-9a-f]{64}")),
        ) {
            "context profile revision identity is invalid"
        }
        requireProfileRef(value.profileRef)
        value.priorFingerprint?.let {
            require(it.matches(Regex("[0-9a-f]{64}"))) {
                "context profile prior fingerprint is invalid"
            }
        }
        val core =
            RevisionCore(
                schemaVersion = value.schemaVersion,
                profileRef = value.profileRef,
                revision = value.revision,
                definition = value.definition,
                basedOnProductFoundation = value.basedOnProductFoundation,
                priorFingerprint = value.priorFingerprint,
                formedAt = value.formedAt,
            )
        require(
            sha256(
                VexContractJson.canonicalEncode(
                    core,
                ),
            ) == value.fingerprint,
        ) {
            "context profile revision fingerprint is invalid"
        }
    }

    private inline fun <reified T> writeImmutableOrRequireExact(
        path: Path,
        value: T,
    ) {
        val existing =
            VexDurableContractFile.read<T>(
                ownedRoot = home,
                path = path,
            )
        if (existing != null) {
            require(existing == value) {
                "context profile immutable address already contains different truth"
            }
            return
        }
        VexDurableContractFile.write(
            ownedRoot = home,
            path = path,
            value = value,
        )
    }

    private fun requireProfileRef(
        value: String,
    ) {
        require(
            value.matches(
                Regex(
                    "[a-z0-9](?:[a-z0-9._-]{0,220}[a-z0-9])?",
                ),
            ),
        ) {
            "context profile ref is not portable"
        }
    }

    private fun sha256(
        value: String,
    ): String =
        MessageDigest.getInstance("SHA-256")
            .digest(
                value.toByteArray(
                    StandardCharsets.UTF_8,
                ),
            )
            .joinToString("") {
                "%02x".format(
                    it.toInt() and 0xff,
                )
            }
}

// [VXG RealForever]
