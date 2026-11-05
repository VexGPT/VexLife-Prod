package vexlife.model.heretic.runtime

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import java.security.MessageDigest
import vexlife.model.heretic.HereticArtifactCatalog
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

    const val ModelRootRelativePath =
        "models/heretic/qwen3.8-27b-mlx4"

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

    sealed interface Classification {
        val path: String

        data class Missing(
            override val path: String,
        ) : Classification

        data class InvalidSize(
            override val path: String,
            val bytes: Long,
            val expectedBytes: Long,
        ) : Classification

        data class InvalidHash(
            override val path: String,
            val bytes: Long,
            val actualSha256: String,
        ) : Classification

        data class VerifiedReusable(
            override val path: String,
            val bytes: Long,
            val actualSha256: String,
        ) : Classification
    }

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

    fun plan(): Plan {
        val descriptors =
            HereticArtifactCatalog.Members

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
                HereticProductIdentity.ArtifactContentSetSha256,
        ) {
            "Heretic artifact catalog does not reproduce the certified content-set SHA-256"
        }

        return Plan(
            schemaVersion = Schema,
            modelBundleRef = HereticProductIdentity.ModelBundleRef,
            modelProfileRef = HereticProductIdentity.ModelProfileRef,
            artifactSetRef = HereticProductIdentity.ArtifactSetRef,
            contentSetSha256 = HereticProductIdentity.ArtifactContentSetSha256,
            runtimeAdapterRef = HereticProductIdentity.RuntimeAdapterRef,
            providerRevision = providerRevisions.single(),
            memberCount = members.size,
            totalBytes =
                members.sumOf {
                    it.byteLength
                },
            members = members,
        )
    }

    fun classifyExactArtifact(
        finalPath: Path,
        expectedSha256: String,
        expectedBytes: Long,
    ): Classification {
        require(
            expectedSha256.matches(
                Regex(
                    "[0-9a-f]{64}",
                ),
            ),
        ) {
            "expectedSha256 must be lowercase SHA-256"
        }
        require(
            expectedBytes >
                0,
        ) {
            "expectedBytes must be positive"
        }

        if (
            !Files.exists(
                finalPath,
                LinkOption.NOFOLLOW_LINKS,
            )
        ) {
            return Classification.Missing(
                finalPath.toString(),
            )
        }

        val attributes =
            Files.readAttributes(
                finalPath,
                BasicFileAttributes::class.java,
                LinkOption.NOFOLLOW_LINKS,
            )

        require(
            attributes.isRegularFile &&
                !attributes.isSymbolicLink,
        ) {
            "artifact destination must be one regular non-symlink file"
        }

        if (
            attributes.size() !=
                expectedBytes
        ) {
            return Classification.InvalidSize(
                path = finalPath.toString(),
                bytes = attributes.size(),
                expectedBytes = expectedBytes,
            )
        }

        val actualSha256 =
            sha256File(
                finalPath,
            )

        if (
            actualSha256 !=
                expectedSha256
        ) {
            return Classification.InvalidHash(
                path = finalPath.toString(),
                bytes = attributes.size(),
                actualSha256 = actualSha256,
            )
        }

        return Classification.VerifiedReusable(
            path = finalPath.toString(),
            bytes = attributes.size(),
            actualSha256 = actualSha256,
        )
    }

    fun materializeCurrent(
        vexHome: Path,
        allowAcquisition: Boolean,
        acquirer: Acquirer,
    ): Result {
        require(
            vexHome.isAbsolute,
        ) {
            "Vex Home must be one absolute path"
        }

        val plan =
            plan()
        val home =
            vexHome.normalize()
        val modelPath =
            home
                .resolve(
                    ModelRootRelativePath,
                )
                .resolve(
                    plan.providerRevision,
                )
                .normalize()

        require(
            modelPath.startsWith(
                home,
            ),
        ) {
            "Heretic materialization path escaped Vex Home"
        }

        Files.createDirectories(
            modelPath,
        )

        val descriptors =
            HereticArtifactCatalog.Members
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
                modelPath
                    .resolve(
                        member.logicalPath,
                    )
                    .normalize()

            require(
                finalPath.parent ==
                    modelPath,
            ) {
                "Heretic materialization member escaped the certified model directory"
            }

            when (
                classifyExactArtifact(
                    finalPath = finalPath,
                    expectedSha256 = member.sha256,
                    expectedBytes = member.byteLength,
                )
            ) {
                is Classification.VerifiedReusable ->
                    reused +=
                        1

                else -> {
                    require(
                        allowAcquisition,
                    ) {
                        "Heretic artifact " +
                            member.logicalPath +
                            " is not exactly reusable and acquisition was not authorized"
                    }

                    acquirer.acquire(
                        descriptor,
                        finalPath,
                    )

                    val verified =
                        classifyExactArtifact(
                            finalPath = finalPath,
                            expectedSha256 = member.sha256,
                            expectedBytes = member.byteLength,
                        )

                    require(
                        verified is
                            Classification.VerifiedReusable,
                    ) {
                        "authorized acquisition did not produce exact Heretic artifact " +
                            member.logicalPath
                    }

                    acquired +=
                        1
                }
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

    private fun sha256File(
        path: Path,
    ): String {
        val digest =
            MessageDigest.getInstance(
                "SHA-256",
            )

        Files.newInputStream(
            path,
        )
            .use {
                input ->
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
