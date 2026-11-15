package vexlife.model.heretic.runtime

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.security.MessageDigest
import vexlife.artifact.ExactArtifactFile
import vexlife.filesystem.VexOwnedPath
import vexlife.model.heretic.HereticArtifactCatalog
import vexlife.model.heretic.HereticModelCatalog
import vexlife.model.heretic.HereticProductIdentity

/**
 * Exact local materialization owner for the certified Heretic artifact catalog.
 *
 * This owner classifies local bytes, reuses only exact files, and delegates acquisition only when
 * the caller has already authorized it. It does not own network/provider policy or runtime start.
 */
object HereticArtifactMaterialization {
    const val Schema =
        "vexlife.heretic-runtime-establishment/v1"

    /** The certified model's folder; every catalog model names its own (HereticModelCatalog). */
    val ModelRootRelativePath: String =
        HereticModelCatalog.Default.modelRootRelativePath

    private val ProviderRevisionPattern =
        Regex(
            "\\.([0-9a-f]{40})$",
        )

    data class PlanMember(
        val roleRef: String,
        val artifactRef: String,
        val logicalPath: String,
        val sha256: String,
        val byteLength: Long,
    )

    data class Plan(
        val schemaVersion: String,
        val modelBundleRef: String,
        val modelProfileRef: String,
        val artifactSetRef: String,
        val contentSetSha256: String,
        val runtimeAdapterRef: String,
        val providerRevision: String,
        val memberCount: Int,
        val totalBytes: Long,
        val members: List<PlanMember>,
    )

    fun interface Acquirer {
        fun acquire(
            member: HereticArtifactCatalog.Member,
            finalPath: Path,
        )
    }

    data class Result(
        val state: String,
        val modelPath: String,
        val providerRevision: String,
        val contentSetSha256: String,
        val memberCount: Int,
        val totalBytes: Long,
        val reusedMemberCount: Int,
        val acquiredMemberCount: Int,
        val acquisitionPerformed: Boolean,
        val artifactMutationPerformed: Boolean,
    )

    data class PresenceObservation(
        val state: String,
        val modelPath: String,
        val providerRevision: String,
        val contentSetSha256: String,
        val memberCount: Int,
        val totalBytes: Long,
        val presentBySizeBytes: Long,
        val missingBytes: Long,
        val invalidSizeBytes: Long,
        val effectsPerformed: Boolean,
    )


    fun plan(
        model: HereticModelCatalog.Entry = HereticModelCatalog.Default,
    ): Plan {
        val descriptors =
            model.members

        require(
            descriptors.isNotEmpty(),
        ) {
            "Heretic artifact materialization requires a non-empty certified catalog"
        }

        val providerRevisions =
            descriptors
                .map {
                    providerRevision(
                        it.sourceRef,
                    )
                }
                .distinct()

        require(
            providerRevisions.size ==
                1,
        ) {
            "Heretic artifacts must bind one exact provider revision"
        }

        val members =
            descriptors
                .map {
                    PlanMember(
                        roleRef = it.roleRef,
                        artifactRef = it.artifactRef,
                        logicalPath = it.filename,
                        sha256 = it.sha256,
                        byteLength = it.expectedBytes,
                    )
                }
                .sortedWith(
                    Comparator {
                        left,
                        right,
                        ->
                        compareUtf8(
                            left.logicalPath,
                            right.logicalPath,
                        )
                    },
                )

        val actualContentSet =
            computeContentSetSha256(
                members,
            )

        require(
            actualContentSet ==
                model.identity.artifactContentSetSha256,
        ) {
            "Heretic artifact catalog does not reproduce the pinned content-set SHA-256 of ${model.handle}"
        }

        return Plan(
            schemaVersion = Schema,
            modelBundleRef = model.identity.modelBundleRef,
            modelProfileRef = model.identity.modelProfileRef,
            artifactSetRef = model.identity.artifactSetRef,
            contentSetSha256 = model.identity.artifactContentSetSha256,
            runtimeAdapterRef = model.identity.runtimeAdapterRef,
            providerRevision = providerRevisions.single(),
            memberCount = members.size,
            totalBytes =
                members.sumOf {
                    it.byteLength
                },
            members = members,
        )
    }

    /**
     * Lightweight no-effect presence observation for human current-state awareness.
     *
     * This intentionally does not hash multi-GiB model members. Exact-byte qualification remains
     * owned by [materializeCurrent] / runtime qualification when an operation actually depends on
     * it. This projection answers only which expected files are presently absent, wrong-sized, or
     * present at the exact declared byte length.
     */
    fun observePresence(
        productHome: Path,
        model: HereticModelCatalog.Entry = HereticModelCatalog.Default,
    ): PresenceObservation {
        require(
            productHome.isAbsolute,
        ) {
            "VexLife-Prod Home must be one absolute path"
        }

        val plan =
            plan(model)
        val home =
            VexOwnedPath.requireRoot(
                productHome,
                "VexLife-Prod Home",
            )
        val modelPath =
            VexOwnedPath.resolveInside(
                home,
                "${model.modelRootRelativePath}/${plan.providerRevision}",
            )

        if (
            VexOwnedPath.observeDirectory(
                home,
                modelPath,
            ) ==
            VexOwnedPath.DirectoryState.Missing
        ) {
            return PresenceObservation(
                state = "ABSENT",
                modelPath = modelPath.toString(),
                providerRevision = plan.providerRevision,
                contentSetSha256 = plan.contentSetSha256,
                memberCount = plan.memberCount,
                totalBytes = plan.totalBytes,
                presentBySizeBytes = 0,
                missingBytes = plan.totalBytes,
                invalidSizeBytes = 0,
                effectsPerformed = false,
            )
        }

        var presentBySizeBytes =
            0L
        var missingBytes =
            0L
        var invalidSizeBytes =
            0L

        for (
            member in
            plan.members
        ) {
            val path =
                VexOwnedPath.resolveInside(
                    modelPath,
                    member.logicalPath,
                )
            require(
                path.parent ==
                    modelPath,
            ) {
                "Heretic presence member escaped the certified model directory"
            }

            when {
                !Files.exists(
                    path,
                    LinkOption.NOFOLLOW_LINKS,
                ) ->
                    missingBytes +=
                        member.byteLength

                Files.isSymbolicLink(
                    path,
                ) ||
                    !Files.isRegularFile(
                        path,
                        LinkOption.NOFOLLOW_LINKS,
                    ) ->
                    invalidSizeBytes +=
                        member.byteLength

                Files.size(
                    path,
                ) !=
                    member.byteLength ->
                    invalidSizeBytes +=
                        member.byteLength

                else ->
                    presentBySizeBytes +=
                        member.byteLength
            }
        }

        require(
            presentBySizeBytes +
                missingBytes +
                invalidSizeBytes ==
                plan.totalBytes,
        ) {
            "Heretic presence observation did not account for every expected byte"
        }

        return PresenceObservation(
            state =
                when {
                    invalidSizeBytes >
                        0 ->
                        "INVALID_SIZE"

                    presentBySizeBytes ==
                        0L ->
                        "ABSENT"

                    missingBytes >
                        0 ->
                        "PARTIAL_BY_SIZE"

                    else ->
                        "COMPLETE_BY_SIZE_NOT_HASHED"
                },
            modelPath = modelPath.toString(),
            providerRevision = plan.providerRevision,
            contentSetSha256 = plan.contentSetSha256,
            memberCount = plan.memberCount,
            totalBytes = plan.totalBytes,
            presentBySizeBytes = presentBySizeBytes,
            missingBytes = missingBytes,
            invalidSizeBytes = invalidSizeBytes,
            effectsPerformed = false,
        )
    }

    fun materializeCurrent(
        productHome: Path,
        allowAcquisition: Boolean,
        acquirer: Acquirer,
        model: HereticModelCatalog.Entry = HereticModelCatalog.Default,
    ): Result {
        require(
            productHome.isAbsolute,
        ) {
            "VexLife-Prod Home must be one absolute path"
        }

        val plan =
            plan(model)
        val home =
            VexOwnedPath.requireRoot(
                productHome,
                "VexLife-Prod Home",
            )
        val modelPath =
            VexOwnedPath.resolveInside(
                home,
                "${model.modelRootRelativePath}/${plan.providerRevision}",
            )

        val modelDirectoryState =
            VexOwnedPath.observeDirectory(
                home,
                modelPath,
            )

        if (
            modelDirectoryState ==
                VexOwnedPath.DirectoryState.Missing
        ) {
            require(
                allowAcquisition,
            ) {
                "Heretic model directory is absent and acquisition was not authorized"
            }

            VexOwnedPath.ensureDirectory(
                home,
                modelPath,
            )
        }

        val descriptors =
            model.members
                .associateBy {
                    it.artifactRef
                }

        var reused =
            0
        var acquired =
            0

        for (
            member in
            plan.members
        ) {
            val descriptor =
                requireNotNull(
                    descriptors[
                        member.artifactRef
                    ],
                ) {
                    "Heretic materialization descriptor disappeared for " +
                        member.artifactRef
                }
            val finalPath =
                VexOwnedPath.resolveInside(
                    modelPath,
                    member.logicalPath,
                )

            require(
                finalPath.parent ==
                    modelPath,
            ) {
                "Heretic materialization member escaped the certified model directory"
            }

            when (
                val local =
                    ExactArtifactFile.classify(
                        path = finalPath,
                        expectedSha256 = member.sha256,
                        expectedBytes = member.byteLength,
                    )
            ) {
                is ExactArtifactFile.Classification.VerifiedReusable ->
                    reused +=
                        1

                is ExactArtifactFile.Classification.Missing -> {
                    require(
                        allowAcquisition,
                    ) {
                        "Heretic artifact " +
                            member.logicalPath +
                            " is missing and acquisition was not authorized"
                    }

                    acquirer.acquire(
                        descriptor,
                        finalPath,
                    )

                    val verified =
                        ExactArtifactFile.classify(
                            path = finalPath,
                            expectedSha256 = member.sha256,
                            expectedBytes = member.byteLength,
                        )

                    require(
                        verified is
                            ExactArtifactFile.Classification.VerifiedReusable,
                    ) {
                        "authorized acquisition did not produce exact Heretic artifact " +
                            member.logicalPath
                    }

                    acquired +=
                        1
                }

                is ExactArtifactFile.Classification.InvalidSize,
                is ExactArtifactFile.Classification.InvalidHash,
                ->
                    error(
                        "existing Heretic artifact failed exact verification and requires an explicit repair owner before reacquisition: " +
                            member.logicalPath +
                            " (" +
                            local::class.simpleName +
                            ")",
                    )
            }
        }

        require(
            reused +
                acquired ==
                plan.memberCount,
        ) {
            "Heretic materialization did not account for every certified member"
        }

        return Result(
            state = "HERETIC_ARTIFACTS_MATERIALIZED_EXACT",
            modelPath = modelPath.toString(),
            providerRevision = plan.providerRevision,
            contentSetSha256 = plan.contentSetSha256,
            memberCount = plan.memberCount,
            totalBytes = plan.totalBytes,
            reusedMemberCount = reused,
            acquiredMemberCount = acquired,
            acquisitionPerformed =
                acquired >
                    0,
            artifactMutationPerformed =
                acquired >
                    0,
        )
    }

    data class Removal(
        val state: String,
        val modelPath: String,
        val removedFiles: Int,
        val freedBytes: Long,
    )

    /**
     * Frees a catalog model's files from the Home so the space can be used again; the model stays
     * offered and can be downloaded again. Removes only that model's own pinned member files and
     * then its empty folders — nothing else under the Home, and never through a link.
     *
     * Whether removing is lawful now (not the model the running runtime serves) is the caller's
     * Control; this owner performs the exact removal it is asked for.
     */
    fun remove(
        productHome: Path,
        model: HereticModelCatalog.Entry,
    ): Removal {
        require(productHome.isAbsolute) { "VexLife-Prod Home must be one absolute path" }
        val home = VexOwnedPath.requireRoot(productHome, "VexLife-Prod Home")
        val root = VexOwnedPath.resolveInside(home, model.modelRootRelativePath)
        val modelPath = VexOwnedPath.resolveInside(home, "${model.modelRootRelativePath}/${model.providerRevision}")
        if (VexOwnedPath.observeDirectory(home, modelPath) == VexOwnedPath.DirectoryState.Missing) {
            return Removal("ALREADY_ABSENT", modelPath.toString(), 0, 0)
        }
        var removed = 0
        var freed = 0L
        for (member in model.members) {
            val path = VexOwnedPath.resolveInside(modelPath, member.filename)
            require(path.parent == modelPath) { "Heretic removal member escaped the model directory" }
            if (Files.exists(path, LinkOption.NOFOLLOW_LINKS) && !Files.isSymbolicLink(path) && Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
                freed += Files.size(path)
                Files.delete(path)
                removed++
            }
            // A paused download leaves a sibling ".partial" file (DirectHttpsArtifactDelivery); it is
            // this model's own bytes too.
            val partial = modelPath.resolve(member.filename + ".partial")
            if (Files.isRegularFile(partial, LinkOption.NOFOLLOW_LINKS) && !Files.isSymbolicLink(partial)) {
                freed += Files.size(partial)
                Files.delete(partial)
            }
        }
        for (directory in listOf(modelPath, root)) {
            val empty = Files.list(directory).use { it.findFirst().isEmpty }
            if (empty) Files.delete(directory) else break
        }
        return Removal("REMOVED", modelPath.toString(), removed, freed)
    }

    fun computeContentSetSha256(
        members: List<PlanMember>,
    ): String {
        require(
            members.isNotEmpty(),
        ) {
            "Heretic content-set members must not be empty"
        }

        val ordered =
            members
                .sortedWith(
                    Comparator {
                        left,
                        right,
                        ->
                        compareUtf8(
                            left.logicalPath,
                            right.logicalPath,
                        )
                    },
                )

        require(
            ordered
                .map {
                    it.logicalPath
                }
                .distinct()
                .size ==
                ordered.size,
        ) {
            "Heretic content-set logical paths must be unique"
        }

        val digest =
            MessageDigest.getInstance(
                "SHA-256",
            )

        for (
            member in
            ordered
        ) {
            require(
                member.logicalPath.isNotBlank() &&
                    !member.logicalPath.contains(
                        "/",
                    ) &&
                    !member.logicalPath.contains(
                        "\\",
                    ),
            ) {
                "Heretic model member path must be one safe path segment"
            }
            require(
                member.sha256.matches(
                    Regex(
                        "[0-9a-f]{64}",
                    ),
                ),
            )
            require(
                member.byteLength >=
                    0,
            )

            digest.update(
                member.logicalPath.toByteArray(
                    StandardCharsets.UTF_8,
                ),
            )
            digest.update(
                byteArrayOf(
                    0,
                ),
            )
            digest.update(
                member.sha256.toByteArray(
                    StandardCharsets.US_ASCII,
                ),
            )
            digest.update(
                byteArrayOf(
                    0,
                ),
            )
            digest.update(
                member.byteLength
                    .toString()
                    .toByteArray(
                        StandardCharsets.US_ASCII,
                    ),
            )
            digest.update(
                "\n".toByteArray(
                    StandardCharsets.UTF_8,
                ),
            )
        }

        return digest
            .digest()
            .joinToString(
                "",
            ) {
                byte ->
                "%02x".format(
                    byte.toInt() and
                        0xff,
                )
            }
    }

    private fun providerRevision(
        sourceRef: String,
    ): String =
        requireNotNull(
            ProviderRevisionPattern
                .find(
                    sourceRef,
                )
                ?.groupValues
                ?.get(
                    1,
                ),
        ) {
            "Heretic provider source ref must end in one exact 40-hex revision"
        }

    private fun compareUtf8(
        left: String,
        right: String,
    ): Int {
        val leftBytes =
            left.toByteArray(
                StandardCharsets.UTF_8,
            )
        val rightBytes =
            right.toByteArray(
                StandardCharsets.UTF_8,
            )
        val common =
            minOf(
                leftBytes.size,
                rightBytes.size,
            )

        for (
            index in
            0 until
                common
        ) {
            val a =
                leftBytes[
                    index
                ].toInt() and
                    0xff
            val b =
                rightBytes[
                    index
                ].toInt() and
                    0xff

            if (
                a !=
                    b
            ) {
                return a -
                    b
            }
        }

        return leftBytes.size -
            rightBytes.size
    }
}

// [VXG RealForever]
