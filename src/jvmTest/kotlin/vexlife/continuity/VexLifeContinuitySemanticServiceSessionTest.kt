package vexlife.continuity

import java.nio.file.Files
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import vexlife.model.heretic.HereticRuntimeRealization

class VexLifeContinuitySemanticServiceSessionTest {
    @Test
    fun exactMacPrerequisitesConstructOneDenseBackendAndCloseItsWorker() {
        val home =
            Files.createTempDirectory(
                "vexlife-semantic-service-",
            )
                .toAbsolutePath()
        val closed =
            AtomicBoolean(
                false,
            )

        try {
            val artifacts =
                artifactObservation(
                    home,
                    VexLifeBgeM3ArtifactMaterialization.PresenceState.PRESENT,
                )
            val environment =
                environment(
                    home,
                )
            var starts =
                0

            val session =
                VexLifeContinuitySemanticServiceSession.establish(
                    productHome =
                        home,
                    hooks =
                        VexLifeContinuitySemanticServiceSession.Hooks(
                            currentTarget = {
                                HereticRuntimeRealization.MacArm64
                            },
                            observeArtifacts = {
                                artifacts
                            },
                            observeMacEnvironment = {
                                environment
                            },
                            startMacRuntime = {
                                productHome,
                                observedArtifacts,
                                observedEnvironment,
                                ->
                                starts +=
                                    1
                                VexLifeBgeM3MacRuntime.start(
                                    binding =
                                        VexLifeBgeM3MacRuntime.bind(
                                            productHome =
                                                productHome,
                                            artifacts =
                                                observedArtifacts,
                                            environment =
                                                observedEnvironment,
                                        ),
                                    launcher =
                                        VexLifeBgeM3MacRuntime.Launcher {
                                            FakeWorker(
                                                closed,
                                            )
                                        },
                                )
                            },
                        ),
                )

            assertEquals(
                VexLifeContinuitySemanticServiceSession.DenseCurrentState,
                session.state,
            )
            val backend =
                assertIs<VexLifeContinuitySourceLocator.SemanticIndexedBackend>(
                    session.backend,
                )
            assertEquals(
                VexLifeBgeM3DenseIdentity.RepresentationBindingRef +
                    "." +
                    VexLifeVolatileDenseSemanticBackend.DenseRankingBindingRef,
                backend.semanticRepresentationBindingRef,
            )
            assertEquals(
                1,
                starts,
            )
            assertTrue(
                !closed.get(),
            )

            session.close()
            assertTrue(
                closed.get(),
                "service semantic Session close must terminate the one supporting worker",
            )
        } finally {
            home
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun missingMacArtifactKeepsLexicalBackendWithoutStartingWorker() {
        val home =
            Files.createTempDirectory(
                "vexlife-semantic-service-missing-",
            )
                .toAbsolutePath()

        try {
            var environmentReads =
                0
            var starts =
                0
            val session =
                VexLifeContinuitySemanticServiceSession.establish(
                    productHome =
                        home,
                    hooks =
                        VexLifeContinuitySemanticServiceSession.Hooks(
                            currentTarget = {
                                HereticRuntimeRealization.MacArm64
                            },
                            observeArtifacts = {
                                artifactObservation(
                                    home,
                                    VexLifeBgeM3ArtifactMaterialization.PresenceState.ABSENT,
                                )
                            },
                            observeMacEnvironment = {
                                environmentReads +=
                                    1
                                environment(
                                    home,
                                )
                            },
                            startMacRuntime = {
                                _,
                                _,
                                _,
                                ->
                                starts +=
                                    1
                                error(
                                    "missing artifact must not start semantic runtime",
                                )
                            },
                        ),
                )

            assertEquals(
                VexLifeContinuitySemanticServiceSession.LexicalFallbackState,
                session.state,
            )
            assertIs<VexLifeContinuitySourceLocator.InMemoryBackend>(
                session.backend,
            )
            assertEquals(
                0,
                environmentReads,
            )
            assertEquals(
                0,
                starts,
            )
            session.close()
        } finally {
            home
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun windowsDoesNotBorrowMacBgeQualification() {
        val home =
            Files.createTempDirectory(
                "vexlife-semantic-service-windows-",
            )
                .toAbsolutePath()

        try {
            var macReads =
                0
            val session =
                VexLifeContinuitySemanticServiceSession.establish(
                    productHome =
                        home,
                    hooks =
                        VexLifeContinuitySemanticServiceSession.Hooks(
                            currentTarget = {
                                HereticRuntimeRealization.WindowsX64
                            },
                            observeArtifacts = {
                                macReads +=
                                    1
                                artifactObservation(
                                    home,
                                    VexLifeBgeM3ArtifactMaterialization.PresenceState.PRESENT,
                                )
                            },
                            observeMacEnvironment = {
                                macReads +=
                                    1
                                environment(
                                    home,
                                )
                            },
                        ),
                )

            assertEquals(
                VexLifeContinuitySemanticServiceSession.LexicalFallbackState,
                session.state,
            )
            assertEquals(
                0,
                macReads,
                "Windows semantic construction must not borrow Mac artifact/environment qualification",
            )
        } finally {
            home
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun supportingWorkerStartFailureFallsBackRatherThanBecomingSourceAbsence() {
        val home =
            Files.createTempDirectory(
                "vexlife-semantic-service-worker-held-",
            )
                .toAbsolutePath()

        try {
            val session =
                VexLifeContinuitySemanticServiceSession.establish(
                    productHome =
                        home,
                    hooks =
                        VexLifeContinuitySemanticServiceSession.Hooks(
                            currentTarget = {
                                HereticRuntimeRealization.MacArm64
                            },
                            observeArtifacts = {
                                artifactObservation(
                                    home,
                                    VexLifeBgeM3ArtifactMaterialization.PresenceState.PRESENT,
                                )
                            },
                            observeMacEnvironment = {
                                environment(
                                    home,
                                )
                            },
                            startMacRuntime = {
                                _,
                                _,
                                _,
                                ->
                                error(
                                    "qualified supporting worker unavailable",
                                )
                            },
                        ),
                )

            assertEquals(
                VexLifeContinuitySemanticServiceSession.LexicalFallbackState,
                session.state,
            )
            assertIs<VexLifeContinuitySourceLocator.InMemoryBackend>(
                session.backend,
            )
        } finally {
            home
                .toFile()
                .deleteRecursively()
        }
    }

    private class FakeWorker(
        private val closed: AtomicBoolean,
    ) :
        VexLifeBgeM3MacRuntime.Worker {
        override val ready =
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

        override fun encode(
            requestRef: String,
            texts: List<String>,
        ): List<FloatArray> =
            texts.map {
                FloatArray(
                    VexLifeBgeM3DenseIdentity.Dimension,
                ) {
                    index ->
                    if (
                        index ==
                        0
                    ) {
                        1f
                    } else {
                        0f
                    }
                }
            }

        override fun close() {
            closed.set(
                true,
            )
        }
    }

    private fun artifactObservation(
        home: java.nio.file.Path,
        state: VexLifeBgeM3ArtifactMaterialization.PresenceState,
    ): VexLifeBgeM3ArtifactMaterialization.Observation {
        val present =
            state ==
                VexLifeBgeM3ArtifactMaterialization.PresenceState.PRESENT
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
                },
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
}

// [VXG RealForever]
