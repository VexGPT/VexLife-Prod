package vexlife.continuity

import java.nio.file.Path
import vexlife.artifact.DirectHttpsArtifactDelivery
import vexlife.artifact.ExactArtifactFile
import vexlife.filesystem.VexOwnedPath

/**
 * Exact local artifact custody for the selected BGE-M3 dense continuity representation.
 *
 * This owner answers only:
 * - which seven provider-revision members are part of the qualified representation;
 * - where those bytes live inside VexLife-Prod Home;
 * - whether each local member exactly matches its observed qualification bytes;
 * - how already-authorized missing members are delegated to generic HTTPS delivery.
 *
 * It does not choose the representation, own Python/ONNX, start a worker, mutate Conversation /
 * Memory, or decide persistence for semantic vectors.
 */
object VexLifeBgeM3ArtifactMaterialization {
    const val ArtifactRef =
        "artifact.vexlife-prod.continuity.bge-m3-dense.6a3fd5fa10d7"

    const val RootRelativePath =
        "runtime/continuity/semantic/bge-m3/" +
            VexLifeBgeM3DenseIdentity.Revision

    data class Member(
        val relativePath: String,
        val expectedBytes: Long,
        val expectedSha256: String,
    ) {
        init {
            require(
                relativePath.startsWith(
                    "onnx/",
                ) &&
                    ".." !in
                    relativePath &&
                    expectedBytes >
                    0 &&
                    expectedSha256.matches(
                        Regex(
                            "[0-9a-f]{64}",
                        ),
                    ),
            ) {
                "BGE-M3 artifact member identity is invalid"
            }
        }

        val sourceUrl: String
            get() =
                "https://huggingface.co/" +
                    VexLifeBgeM3DenseIdentity.Repository +
                    "/resolve/" +
                    VexLifeBgeM3DenseIdentity.Revision +
                    "/" +
                    relativePath
    }

    val Members: List<Member> =
        listOf(
            Member(
                relativePath =
                    "onnx/model.onnx",
                expectedBytes =
                    724_923L,
                expectedSha256 =
                    "f84251230831afb359ab26d9fd37d5936d4d9bb5d1d5410e66442f630f24435b",
            ),
            Member(
                relativePath =
                    "onnx/model.onnx_data",
                expectedBytes =
                    2_266_820_608L,
                expectedSha256 =
                    "1eebfb28493f67bba03ce0ef64bfdc7fc5a3bd9d7493f818bb1d78cd798416b4",
            ),
            Member(
                relativePath =
                    "onnx/config.json",
                expectedBytes =
                    698L,
                expectedSha256 =
                    "f24afd5de914fba8c668426c43d208a1a54022500c63b2c160be20891686fce8",
            ),
            Member(
                relativePath =
                    "onnx/tokenizer.json",
                expectedBytes =
                    17_082_821L,
                expectedSha256 =
                    "6710678b12670bc442b99edc952c4d996ae309a7020c1fa0096dd245c2faf790",
            ),
            Member(
                relativePath =
                    "onnx/tokenizer_config.json",
                expectedBytes =
                    1_173L,
                expectedSha256 =
                    "7e4c1cc848840aeccdd763458c18dd525eb0f795c992e00ebe9c28554e7db2d4",
            ),
            Member(
                relativePath =
                    "onnx/special_tokens_map.json",
                expectedBytes =
                    964L,
                expectedSha256 =
                    "8c785abebea9ae3257b61681b4e6fd8365ceafde980c21970d001e834cf10835",
            ),
            Member(
                relativePath =
                    "onnx/sentencepiece.bpe.model",
                expectedBytes =
                    5_069_051L,
                expectedSha256 =
                    "cfc8146abe2a0488e9e2a0c56de7952f7c11ab059eca145a0a727afce0db2865",
            ),
        )
            .also {
                members ->
                require(
                    members.map(
                        Member::relativePath,
                    )
                        .distinct()
                        .size ==
                        members.size
                ) {
                    "BGE-M3 artifact member paths must be unique"
                }
            }

    val TotalBytes: Long =
        Members.sumOf(
            Member::expectedBytes,
        )

    enum class MemberState {
        MISSING,
        INVALID_SIZE,
        INVALID_HASH,
        VERIFIED,
    }

    data class MemberObservation(
        val member: Member,
        val state: MemberState,
        val actualBytes: Long?,
        val actualSha256: String?,
    )

    enum class PresenceState {
        ABSENT,
        PARTIAL,
        PRESENT,
        DAMAGED,
    }

    data class Observation(
        val artifactRef: String,
        val root: String,
        val state: PresenceState,
        val totalBytes: Long,
        val verifiedBytes: Long,
        val members: List<MemberObservation>,
        val effectsPerformed: Boolean = false,
    ) {
        init {
            require(
                artifactRef ==
                    ArtifactRef &&
                    totalBytes ==
                    TotalBytes &&
                    verifiedBytes in
                    0..totalBytes &&
                    members.size ==
                    Members.size &&
                    !effectsPerformed
            ) {
                "BGE-M3 artifact observation is invalid"
            }
        }
    }

    data class Result(
        val observation: Observation,
        val acquiredMemberCount: Int,
        val artifactMutationPerformed: Boolean,
    ) {
        init {
            require(
                observation.state ==
                    PresenceState.PRESENT &&
                    acquiredMemberCount in
                    0..Members.size &&
                    artifactMutationPerformed ==
                    (
                        acquiredMemberCount >
                            0
                    )
            ) {
                "BGE-M3 artifact materialization result is invalid"
            }
        }
    }

    fun modelRoot(
        productHome: Path,
    ): Path =
        VexOwnedPath.resolveInside(
            VexOwnedPath.requireRoot(
                productHome,
                "VexLife-Prod Home",
            ),
            RootRelativePath,
        )

    fun observe(
        productHome: Path,
    ): Observation {
        val home =
            VexOwnedPath.requireRoot(
                productHome,
                "VexLife-Prod Home",
            )
        val root =
            modelRoot(
                home,
            )

        if (
            VexOwnedPath.observeDirectory(
                home,
                root,
            ) ==
            VexOwnedPath.DirectoryState.Missing
        ) {
            return absentObservation(
                root,
            )
        }

        val onnxRoot =
            root.resolve(
                "onnx",
            )
        if (
            VexOwnedPath.observeDirectory(
                home,
                onnxRoot,
            ) ==
            VexOwnedPath.DirectoryState.Missing
        ) {
            return absentObservation(
                root,
            )
        }

        val observed =
            Members.map {
                member ->
                val path =
                    root.resolve(
                        member.relativePath,
                    )
                when (
                    val state =
                        ExactArtifactFile.classify(
                            path = path,
                            expectedSha256 =
                                member.expectedSha256,
                            expectedBytes =
                                member.expectedBytes,
                        )
                ) {
                    is ExactArtifactFile.Classification.Missing ->
                        MemberObservation(
                            member = member,
                            state =
                                MemberState.MISSING,
                            actualBytes =
                                null,
                            actualSha256 =
                                null,
                        )

                    is ExactArtifactFile.Classification.InvalidSize ->
                        MemberObservation(
                            member = member,
                            state =
                                MemberState.INVALID_SIZE,
                            actualBytes =
                                state.bytes,
                            actualSha256 =
                                null,
                        )

                    is ExactArtifactFile.Classification.InvalidHash ->
                        MemberObservation(
                            member = member,
                            state =
                                MemberState.INVALID_HASH,
                            actualBytes =
                                state.bytes,
                            actualSha256 =
                                state.actualSha256,
                        )

                    is ExactArtifactFile.Classification.VerifiedReusable ->
                        MemberObservation(
                            member = member,
                            state =
                                MemberState.VERIFIED,
                            actualBytes =
                                state.bytes,
                            actualSha256 =
                                state.actualSha256,
                        )
                }
            }

        val damaged =
            observed.any {
                it.state ==
                    MemberState.INVALID_SIZE ||
                    it.state ==
                    MemberState.INVALID_HASH
            }
        val verified =
            observed.filter {
                it.state ==
                    MemberState.VERIFIED
            }
        val state =
            when {
                damaged ->
                    PresenceState.DAMAGED

                verified.size ==
                    Members.size ->
                    PresenceState.PRESENT

                verified.isEmpty() ->
                    PresenceState.ABSENT

                else ->
                    PresenceState.PARTIAL
            }

        return Observation(
            artifactRef =
                ArtifactRef,
            root =
                root.toString(),
            state =
                state,
            totalBytes =
                TotalBytes,
            verifiedBytes =
                verified.sumOf {
                    it.member.expectedBytes
                },
            members =
                observed,
        )
    }

    fun ensure(
        productHome: Path,
        allowAcquisition: Boolean,
        transport: DirectHttpsArtifactDelivery.Transport =
            DirectHttpsArtifactDelivery.JdkTransport,
        onProgress:
            (
                member: Member,
                progress: DirectHttpsArtifactDelivery.Progress,
            ) -> Unit = {
                _,
                _,
                ->
            },
    ): Result {
        val home =
            VexOwnedPath.requireRoot(
                productHome,
                "VexLife-Prod Home",
            )
        val before =
            observe(
                home,
            )

        if (
            before.state ==
            PresenceState.PRESENT
        ) {
            return Result(
                observation =
                    before,
                acquiredMemberCount =
                    0,
                artifactMutationPerformed =
                    false,
            )
        }

        require(
            before.state !=
                PresenceState.DAMAGED
        ) {
            "existing BGE-M3 artifact bytes are damaged; repair/removal requires a separate explicit effect"
        }
        require(
            allowAcquisition,
        ) {
            "BGE-M3 artifact set is not exact and acquisition was not authorized"
        }

        val root =
            modelRoot(
                home,
            )
        VexOwnedPath.ensureDirectory(
            home,
            root,
        )
        val onnxRoot =
            root.resolve(
                "onnx",
            )
        VexOwnedPath.ensureDirectory(
            home,
            onnxRoot,
        )

        var acquired =
            0

        before.members.forEach {
            observed ->
            if (
                observed.state !=
                MemberState.MISSING
            ) {
                return@forEach
            }

            val member =
                observed.member
            val destination =
                root.resolve(
                    member.relativePath,
                )
            DirectHttpsArtifactDelivery.download(
                request =
                    DirectHttpsArtifactDelivery.Request(
                        url =
                            member.sourceUrl,
                        expectedSha256 =
                            member.expectedSha256,
                        expectedBytes =
                            member.expectedBytes,
                    ),
                finalPath =
                    destination,
                transport =
                    transport,
                onProgress = {
                    progress ->
                    onProgress(
                        member,
                        progress,
                    )
                },
            )
            acquired +=
                1
        }

        val after =
            observe(
                home,
            )
        require(
            after.state ==
                PresenceState.PRESENT
        ) {
            "BGE-M3 artifact acquisition did not converge the exact seven-member set"
        }

        return Result(
            observation =
                after,
            acquiredMemberCount =
                acquired,
            artifactMutationPerformed =
                acquired >
                    0,
        )
    }

    private fun absentObservation(
        root: Path,
    ): Observation =
        Observation(
            artifactRef =
                ArtifactRef,
            root =
                root.toString(),
            state =
                PresenceState.ABSENT,
            totalBytes =
                TotalBytes,
            verifiedBytes =
                0,
            members =
                Members.map {
                    member ->
                    MemberObservation(
                        member = member,
                        state =
                            MemberState.MISSING,
                        actualBytes =
                            null,
                        actualSha256 =
                            null,
                    )
                },
        )
}

// [VXG RealForever]
