package vexlife.model.heretic.runtime

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vexlife.model.VexLifeVisualInputQualification
import vexlife.model.heretic.HereticModelCatalog
import vexlife.model.heretic.HereticModelConfiguration
import vexlife.model.heretic.HereticRuntimeQualification
import vexlife.model.heretic.HereticRuntimeRealization
import vexlife.model.heretic.HereticRuntimeState
import vexlife.platform.python.ManagedPython

class VexLifeVisualPythonEnvironmentTest {
    private class FakeRunner :
        VexLifeVisualPythonEnvironment.Runner {
        val calls =
            mutableListOf<Pair<String, List<String>>>()

        override fun run(
            program: String,
            args: List<String>,
            timeoutMs: Long,
        ): VexLifeVisualPythonEnvironment.CommandResult {
            calls +=
                program to
                    args

            if (
                args.take(
                    2,
                ) ==
                    listOf(
                        "-m",
                        "venv",
                    )
            ) {
                val root =
                    Path.of(
                        args[2],
                    )
                Files.createDirectories(
                    root.resolve(
                        "bin",
                    ),
                )
                Files.writeString(
                    root.resolve(
                        "bin/python",
                    ),
                    "fake",
                )
                return pass()
            }

            if (
                args.take(
                    3,
                ) ==
                    listOf(
                        "-m",
                        "pip",
                        "install",
                    )
            ) {
                val report =
                    Path.of(
                        args[
                            args.indexOf(
                                "--report",
                            ) +
                                1
                        ],
                    )
                val requirements =
                    Path.of(
                        args[
                            args.indexOf(
                                "-r",
                            ) +
                                1
                        ],
                    )
                val installs =
                    Files.readAllLines(
                        requirements,
                        StandardCharsets.UTF_8,
                    )
                        .filter(
                            String::isNotBlank,
                        )
                        .joinToString(
                            ",",
                        ) {
                            line ->
                            val nameVersion =
                                line.substringBefore(
                                    " --hash=sha256:",
                                )
                            val name =
                                nameVersion.substringBeforeLast(
                                    "==",
                                )
                            val version =
                                nameVersion.substringAfterLast(
                                    "==",
                                )
                            val sha256 =
                                line.substringAfter(
                                    " --hash=sha256:",
                                )
                            "{\"metadata\":{\"name\":\"$name\",\"version\":\"$version\"}," +
                                "\"download_info\":{\"archive_info\":{\"hashes\":{\"sha256\":\"$sha256\"}}}}"
                        }
                Files.writeString(
                    report,
                    "{\"install\":[$installs]}",
                    StandardCharsets.UTF_8,
                )
                return pass()
            }

            if (
                args.firstOrNull() ==
                    "-c"
            ) {
                if (
                    args.getOrNull(
                        1,
                    )
                        ?.contains(
                            "mlx_vlm",
                        ) ==
                    true
                ) {
                    val python =
                        Path.of(
                            program,
                        )
                    val root =
                        python.parent.parent
                    return pass(
                        VexLifeVisualInputQualification.PythonVersion +
                            "\n" +
                            VexLifeVisualInputQualification.MlxVlmVersion +
                            "\n" +
                            VexLifeVisualInputQualification.MlxVersion +
                            "\n" +
                            VexLifeVisualInputQualification.TransformersVersion +
                            "\n" +
                            root +
                            "\n" +
                            python +
                            "\n",
                    )
                }

                return pass(
                    VexLifeVisualInputQualification.PythonVersion +
                        "\n" +
                        program +
                        "\n",
                )
            }

            error(
                "unexpected fake visual command: $program $args",
            )
        }

        private fun pass(
            stdout: String = "",
        ) =
            VexLifeVisualPythonEnvironment.CommandResult(
                status = 0,
                stdout = stdout,
                stderr = "",
            )
    }

    private fun managedPython(
        home: Path,
    ): ManagedPython.Result {
        val root =
            home.resolve(
                ManagedPython.PythonInstallRelativePath,
            )
                .normalize()
        val python =
            root.resolve(
                "cpython-3.14.7/bin/python3.14",
            )
        val uv =
            home.resolve(
                ManagedPython.UvToolRelativePath,
            )
                .resolve(
                    "uv",
                )
        Files.createDirectories(
            python.parent,
        )
        Files.writeString(
            python,
            "fake-managed-python",
        )
        Files.createDirectories(
            uv.parent,
        )
        Files.writeString(
            uv,
            "fake-uv",
        )
        return ManagedPython.Result(
            state = "REUSED_VERIFIED",
            pythonVersion =
                VexLifeVisualInputQualification.PythonVersion,
            pythonPath = python.toString(),
            pythonInstallRoot = root.toString(),
            uvVersion = ManagedPython.UvVersion,
            uvPath = uv.toString(),
            uvArchiveSha256 = ManagedPython.UvMacArm64ArchiveSha256,
            uvReleaseSourceCommit = ManagedPython.UvReleaseSourceCommit,
            uvAcquisitionPerformed = false,
            pythonInstallationPerformed = false,
            globalBinMutationPerformed = false,
        )
    }

    private fun qualifiedRuntime(
        model: HereticModelCatalog.Entry,
    ): HereticModelConfiguration.QualifiedRuntimeEvidence {
        val realization =
            HereticRuntimeRealization.require(
                modelRef = model.modelRef,
                target = HereticRuntimeRealization.MacArm64,
            )
        return HereticModelConfiguration.QualifiedRuntimeEvidence(
            runtimeReceiptRef =
                "receipt.vexlife-prod.visual-python-environment-test",
            runtimeState = HereticRuntimeState.Qualified,
            realizationRef = realization.realizationRef,
            modelRef = model.modelRef,
            modelProfileRef = realization.modelProfileRef,
            artifactRef = realization.artifact.artifactRef,
            artifactContentSetSha256 =
                realization.artifact.contentSetSha256,
            runtimeAdapterRef =
                realization.runtime.runtimeAdapterRef,
            runtimeClass = realization.runtime.runtimeClass,
            endpoint = realization.runtime.endpoint,
            requestModel = realization.runtime.requestModel,
            qualification =
                HereticRuntimeQualification.qualified(),
        )
    }

    @Test
    fun authorizedCreationUsesExactHashLockAndFormsCurrent4BVisualEvidence() {
        val home =
            Files.createTempDirectory(
                "vexlife-visual-python-create-",
            )
                .toAbsolutePath()
        val runner =
            FakeRunner()
        try {
            val result =
                assertNotNull(
                    VexLifeVisualPythonEnvironment.ensure(
                        managedPython = managedPython(
                            home,
                        ),
                        productHome = home,
                        allowCreation = true,
                        runner = runner,
                    ),
                )

            assertEquals(
                "CREATED_VERIFIED",
                result.state,
            )
            assertEquals(
                VexLifeVisualPythonEnvironment.RequirementsSha256,
                result.requirementsSha256,
            )
            assertEquals(
                64,
                result.environmentFingerprintSha256.length,
            )
            assertTrue(
                result.environmentMutationPerformed,
            )
            val pip =
                runner.calls.single {
                    (_, args) ->
                    args.take(
                        3,
                    ) ==
                        listOf(
                            "-m",
                            "pip",
                            "install",
                        )
                }
            assertTrue(
                "--require-hashes" in
                    pip.second,
            )
            assertTrue(
                "--only-binary=:all:" in
                    pip.second,
            )

            val evidence =
                assertNotNull(
                    VexLifeVisualPythonEnvironment.qualifyCurrent(
                        productHome = home,
                        currentTextRuntime =
                            qualifiedRuntime(
                                HereticModelCatalog.Models.Qwen4B,
                            ),
                        runner = runner,
                    ),
                )
            assertEquals(
                result.environmentFingerprintSha256,
                evidence.environmentFingerprintSha256,
            )
            assertEquals(
                HereticModelCatalog.Models.Qwen4B.modelRef,
                evidence.modelRef,
            )
        } finally {
            home
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun missingEnvironmentWithoutCreationAuthorityIsAnOrdinaryVisualHold() {
        val home =
            Files.createTempDirectory(
                "vexlife-visual-python-missing-",
            )
                .toAbsolutePath()
        val runner =
            FakeRunner()
        try {
            assertNull(
                VexLifeVisualPythonEnvironment.ensure(
                    managedPython = managedPython(
                        home,
                    ),
                    productHome = home,
                    allowCreation = false,
                    runner = runner,
                ),
            )
            assertTrue(
                runner.calls.isEmpty(),
            )
        } finally {
            home
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun exactVisualEnvironmentNeverPromotesAnotherServingModel() {
        val home =
            Files.createTempDirectory(
                "vexlife-visual-python-model-",
            )
                .toAbsolutePath()
        val runner =
            FakeRunner()
        try {
            VexLifeVisualPythonEnvironment.ensure(
                managedPython = managedPython(
                    home,
                ),
                productHome = home,
                allowCreation = true,
                runner = runner,
            )
            assertNull(
                VexLifeVisualPythonEnvironment.qualifyCurrent(
                    productHome = home,
                    currentTextRuntime =
                        qualifiedRuntime(
                            HereticModelCatalog.Models.Qwen27B,
                        ),
                    runner = runner,
                ),
            )
        } finally {
            home
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun tamperedSourceLockFailsCurrentness() {
        val home =
            Files.createTempDirectory(
                "vexlife-visual-python-tamper-",
            )
                .toAbsolutePath()
        val runner =
            FakeRunner()
        try {
            val result =
                assertNotNull(
                    VexLifeVisualPythonEnvironment.ensure(
                        managedPython = managedPython(
                            home,
                        ),
                        productHome = home,
                        allowCreation = true,
                        runner = runner,
                    ),
                )
            Files.writeString(
                Path.of(
                    result.requirementsPath,
                ),
                "tampered\n",
                StandardCharsets.UTF_8,
            )
            assertFailsWith<IllegalArgumentException> {
                VexLifeVisualPythonEnvironment.observeCurrent(
                    productHome = home,
                    runner = runner,
                )
            }
        } finally {
            home
                .toFile()
                .deleteRecursively()
        }
    }
}

// [VXG RealForever]
