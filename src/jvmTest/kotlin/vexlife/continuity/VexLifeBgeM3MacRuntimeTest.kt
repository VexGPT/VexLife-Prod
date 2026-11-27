package vexlife.continuity

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class VexLifeBgeM3MacRuntimeTest {
    @Test
    fun oneWorkerSessionPreservesSourceBatchingAndQueryFormation() {
        val home =
            Files.createTempDirectory(
                "vexlife-bge-runtime-",
            )
                .toAbsolutePath()

        try {
            val binding =
                binding(
                    home,
                )
            val worker =
                RecordingWorker(
                    ready =
                        qualifiedReady(),
                )

            val session =
                VexLifeBgeM3MacRuntime.start(
                    binding =
                        binding,
                    launcher =
                        VexLifeBgeM3MacRuntime.Launcher {
                            observed ->
                            assertEquals(
                                binding,
                                observed,
                            )
                            worker
                        },
                )

            assertEquals(
                VexLifeBgeM3DenseIdentity.RepresentationBindingRef,
                session.representationBindingRef,
            )
            assertEquals(
                VexLifeBgeM3DenseIdentity.Dimension,
                session.dimension,
            )

            val sources =
                (0 until
                    65)
                    .map {
                        index ->
                        "canonical source $index"
                    }
            val sourceVectors =
                session.encodeSources(
                    sources,
                )
            assertEquals(
                65,
                sourceVectors.size,
            )
            assertEquals(
                listOf(
                    64,
                    1,
                ),
                worker.calls
                    .take(
                        2,
                    )
                    .map {
                        it.second.size
                    },
                "source encoding must reuse one worker while respecting the bounded worker batch",
            )

            val query =
                session.encodeQuery(
                    listOf(
                        "bird-shaped",
                        "identifier",
                    ),
                )
            assertEquals(
                VexLifeBgeM3DenseIdentity.Dimension,
                query.size,
            )
            assertEquals(
                listOf(
                    "bird-shaped identifier",
                ),
                worker.calls
                    .last()
                    .second,
                "one ContextRevisit query's clue terms must become one representation-bound query text",
            )

            assertEquals(
                3,
                worker.calls.size,
            )
            assertTrue(
                !worker.closed,
            )

            session.close()
            assertTrue(
                worker.closed,
                "session close owns the one worker lifetime",
            )

            session.close()
            assertTrue(
                worker.closed,
                "runtime close must be idempotent",
            )
        } finally {
            home
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun unqualifiedWorkerShapeIsRejectedAndClosed() {
        val home =
            Files.createTempDirectory(
                "vexlife-bge-runtime-invalid-",
            )
                .toAbsolutePath()

        try {
            val worker =
                RecordingWorker(
                    ready =
                        qualifiedReady()
                            .copy(
                                dimension =
                                    768,
                            ),
                )

            assertFailsWith<IllegalArgumentException> {
                VexLifeBgeM3MacRuntime.start(
                    binding =
                        binding(
                            home,
                        ),
                    launcher =
                        VexLifeBgeM3MacRuntime.Launcher {
                            worker
                        },
                )
            }
            assertTrue(
                worker.closed,
                "a worker that fails qualification must not remain live",
            )
        } finally {
            home
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun runtimeBindingRejectsNonPresentArtifactCustody() {
        val home =
            Files.createTempDirectory(
                "vexlife-bge-runtime-binding-",
            )
                .toAbsolutePath()

        try {
            assertFailsWith<IllegalArgumentException> {
                VexLifeBgeM3MacRuntime.bind(
                    productHome =
                        home,
                    artifacts =
                        artifactObservation(
                            home =
                                home,
                            state =
                                VexLifeBgeM3ArtifactMaterialization.PresenceState.PARTIAL,
                        ),
                    environment =
                        environment(
                            home,
                        ),
                )
            }
        } finally {
            home
                .toFile()
                .deleteRecursively()
        }
    }

    private data class Call(
        val ref: String,
        val texts: List<String>,
    )

    private class RecordingWorker(
        override val ready: VexLifeBgeM3MacRuntime.Ready,
    ) :
        VexLifeBgeM3MacRuntime.Worker {
        val calls =
            mutableListOf<Pair<String, List<String>>>()

        var closed =
            false
            private set

        override fun encode(
            requestRef: String,
            texts: List<String>,
        ): List<FloatArray> {
            check(
                !closed,
            )
            calls +=
                requestRef to
                    texts.toList()

            return texts.mapIndexed {
                index,
                _,
                ->
                FloatArray(
                    VexLifeBgeM3DenseIdentity.Dimension,
                ) {
                    coordinate ->
                    if (
                        coordinate ==
                        index %
                            VexLifeBgeM3DenseIdentity.Dimension
                    ) {
                        1f
                    } else {
                        0f
                    }
                }
            }
        }

        override fun close() {
            closed =
                true
        }
    }

    private fun binding(
        home: java.nio.file.Path,
    ): VexLifeBgeM3MacRuntime.Binding =
        VexLifeBgeM3MacRuntime.bind(
            productHome =
                home,
            artifacts =
                artifactObservation(
                    home =
                        home,
                    state =
                        VexLifeBgeM3ArtifactMaterialization.PresenceState.PRESENT,
                ),
            environment =
                environment(
                    home,
                ),
        )

    private fun artifactObservation(
        home: java.nio.file.Path,
        state: VexLifeBgeM3ArtifactMaterialization.PresenceState,
    ): VexLifeBgeM3ArtifactMaterialization.Observation {
        val present =
            state ==
                VexLifeBgeM3ArtifactMaterialization.PresenceState.PRESENT
        val members =
            VexLifeBgeM3ArtifactMaterialization.Members.map {
                member ->
                VexLifeBgeM3ArtifactMaterialization.MemberObservation(
                    member =
                        member,
                    state =
                        if (
                            present
                        ) {
                            VexLifeBgeM3ArtifactMaterialization.MemberState.VERIFIED
                        } else {
                            VexLifeBgeM3ArtifactMaterialization.MemberState.MISSING
                        },
                    actualBytes =
                        if (
                            present
                        ) {
                            member.expectedBytes
                        } else {
                            null
                        },
                    actualSha256 =
                        if (
                            present
                        ) {
                            member.expectedSha256
                        } else {
                            null
                        },
                )
            }

        return VexLifeBgeM3ArtifactMaterialization.Observation(
            artifactRef =
                VexLifeBgeM3ArtifactMaterialization.ArtifactRef,
            root =
                VexLifeBgeM3ArtifactMaterialization
                    .modelRoot(
                        home,
                    )
                    .toString(),
            state =
                state,
            totalBytes =
                VexLifeBgeM3ArtifactMaterialization.TotalBytes,
            verifiedBytes =
                if (
                    present
                ) {
                    VexLifeBgeM3ArtifactMaterialization.TotalBytes
                } else {
                    0
                },
            members =
                members,
        )
    }

    private fun environment(
        home: java.nio.file.Path,
    ): VexLifeBgeM3MacPythonEnvironment.Result {
        val root =
            VexLifeBgeM3MacPythonEnvironment.environmentRoot(
                home,
            )

        return VexLifeBgeM3MacPythonEnvironment.Result(
            state =
                "REUSED_VERIFIED",
            environmentRoot =
                root.toString(),
            pythonPath =
                VexLifeBgeM3MacPythonEnvironment
                    .pythonPath(
                        root,
                    )
                    .toString(),
            requirementsPath =
                root.resolve(
                    VexLifeBgeM3MacPythonEnvironment.RequirementsFilename,
                )
                    .toString(),
            requirementsSha256 =
                VexLifeBgeM3MacPythonEnvironment.RequirementsSha256,
            pipReportPath =
                root.resolve(
                    VexLifeBgeM3MacPythonEnvironment.InstallReportFilename,
                )
                    .toString(),
            pipReportSha256 =
                "a".repeat(
                    64,
                ),
            pythonVersion =
                VexLifeBgeM3MacPythonEnvironment.PythonVersion,
            numpyVersion =
                VexLifeBgeM3MacPythonEnvironment.NumPyVersion,
            onnxRuntimeVersion =
                VexLifeBgeM3MacPythonEnvironment.OnnxRuntimeVersion,
            transformersVersion =
                VexLifeBgeM3MacPythonEnvironment.TransformersVersion,
            huggingFaceHubVersion =
                VexLifeBgeM3MacPythonEnvironment.HuggingFaceHubVersion,
            environmentFingerprintSha256 =
                "b".repeat(
                    64,
                ),
            environmentMutationPerformed =
                false,
        )
    }

    private fun qualifiedReady(): VexLifeBgeM3MacRuntime.Ready =
        VexLifeBgeM3MacRuntime.Ready(
            pid =
                42,
            dimension =
                VexLifeBgeM3DenseIdentity.Dimension,
            providers =
                listOf(
                    VexLifeBgeM3DenseIdentity.ExecutionProvider,
                ),
            inputNames =
                listOf(
                    "input_ids",
                    "attention_mask",
                ),
        )
}

// [VXG RealForever]
