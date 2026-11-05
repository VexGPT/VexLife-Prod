package vexlife.model.heretic.runtime

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import vexlife.model.heretic.HereticProductIdentity

class HereticPythonEnvironmentTest {
    private class FakeRunner :
        HereticPythonEnvironment.Runner {
        val calls =
            mutableListOf<Pair<String, List<String>>>()
        var runtimePythonVersion =
            HereticProductIdentity.PythonVersion
        var failPip =
            false

        override fun run(
            program: String,
            args: List<String>,
            timeoutMs: Long,
        ): HereticPythonEnvironment.CommandResult {
            calls +=
                program to
                    args

            if (
                program ==
                    "python3.14" &&
                    args.firstOrNull() ==
                    "-c"
            ) {
                return HereticPythonEnvironment.CommandResult(
                    status = 0,
                    stdout =
                        HereticProductIdentity.PythonVersion +
                            "\n/usr/local/bin/python3.14\n",
                    stderr = "",
                )
            }

            if (
                program ==
                    "python3.14" &&
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

                return HereticPythonEnvironment.CommandResult(
                    status = 0,
                    stdout = "",
                    stderr = "",
                )
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
                if (
                    failPip
                ) {
                    return HereticPythonEnvironment.CommandResult(
                        status = 1,
                        stdout = "",
                        stderr = "pip failed",
                    )
                }

                val reportIndex =
                    args.indexOf(
                        "--report",
                    )
                Files.writeString(
                    Path.of(
                        args[
                            reportIndex +
                                1
                        ],
                    ),
                    "{}",
                )

                return HereticPythonEnvironment.CommandResult(
                    status = 0,
                    stdout = "",
                    stderr = "",
                )
            }

            if (
                args.firstOrNull() ==
                    "-c"
            ) {
                val python =
                    Path.of(
                        program,
                    )
                val root =
                    python.parent.parent

                return HereticPythonEnvironment.CommandResult(
                    status = 0,
                    stdout =
                        runtimePythonVersion +
                            "\n" +
                            HereticProductIdentity.MlxLmVersion +
                            "\n" +
                            root +
                            "\n" +
                            python +
                            "\n",
                    stderr = "",
                )
            }

            error(
                "unexpected fake command: $program $args",
            )
        }
    }

    @Test
    fun verifiedEnvironmentIsReusedWithoutMutation() {
        val home =
            Files.createTempDirectory(
                "vexlife-heretic-env-home-",
            )
                .toAbsolutePath()
        val root =
            HereticPythonEnvironment.environmentRoot(
                home,
            )
        val python =
            HereticPythonEnvironment.pythonPath(
                root,
            )
        val runner =
            FakeRunner()

        try {
            Files.createDirectories(
                python.parent,
            )
            Files.writeString(
                python,
                "fake",
            )
            Files.writeString(
                root.resolve(
                    HereticPythonEnvironment.InstallReportFilename,
                ),
                "{}",
            )

            val result =
                HereticPythonEnvironment.ensure(
                    bootstrapPython = "python3.14",
                    environmentRoot = root,
                    allowCreation = false,
                    runner = runner,
                )

            assertEquals(
                "REUSED_VERIFIED",
                result.state,
            )
            assertFalse(
                result.environmentMutationPerformed,
            )
            assertEquals(
                1,
                runner.calls.size,
            )
        } finally {
            home
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun missingEnvironmentCannotCreateWithoutAuthorization() {
        val home =
            Files.createTempDirectory(
                "vexlife-heretic-env-held-",
            )
                .toAbsolutePath()
        val root =
            HereticPythonEnvironment.environmentRoot(
                home,
            )
        val runner =
            FakeRunner()

        try {
            assertFailsWith<IllegalStateException> {
                HereticPythonEnvironment.ensure(
                    bootstrapPython = "python3.14",
                    environmentRoot = root,
                    allowCreation = false,
                    runner = runner,
                )
            }
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
    fun authorizedCreationRequiresExactBootstrapAndRetainedPipReport() {
        val home =
            Files.createTempDirectory(
                "vexlife-heretic-env-create-",
            )
                .toAbsolutePath()
        val root =
            HereticPythonEnvironment.environmentRoot(
                home,
            )
        val runner =
            FakeRunner()

        try {
            val result =
                HereticPythonEnvironment.ensure(
                    bootstrapPython = "python3.14",
                    environmentRoot = root,
                    allowCreation = true,
                    runner = runner,
                )

            assertEquals(
                "CREATED_VERIFIED",
                result.state,
            )
            assertTrue(
                result.environmentMutationPerformed,
            )
            assertTrue(
                Files.isRegularFile(
                    Path.of(
                        result.pipReportPath,
                    ),
                ),
            )
            assertTrue(
                runner.calls.any {
                    (_, args) ->
                    args.contains(
                        "mlx-lm==" +
                            HereticProductIdentity.MlxLmVersion,
                    )
                },
            )
            assertTrue(
                runner.calls.any {
                    (_, args) ->
                    args.contains(
                        "--only-binary=:all:",
                    )
                },
            )
        } finally {
            home
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun existingSymlinkNeverBecomesRepairDeletionAuthority() {
        val home =
            Files.createTempDirectory(
                "vexlife-heretic-env-symlink-",
            )
                .toAbsolutePath()
        val root =
            HereticPythonEnvironment.environmentRoot(
                home,
            )
        val target =
            Files.createTempDirectory(
                "vexlife-heretic-env-target-",
            )
        val runner =
            FakeRunner()

        try {
            Files.createDirectories(
                root.parent,
            )
            Files.createSymbolicLink(
                root,
                target,
            )

            assertFailsWith<IllegalArgumentException> {
                HereticPythonEnvironment.ensure(
                    bootstrapPython = "python3.14",
                    environmentRoot = root,
                    allowCreation = true,
                    runner = runner,
                )
            }

            assertTrue(
                Files.isSymbolicLink(
                    root,
                ),
            )
            assertTrue(
                Files.exists(
                    target,
                ),
            )
            assertTrue(
                runner.calls.isEmpty(),
            )
        } finally {
            Files.deleteIfExists(
                root,
            )
            target
                .toFile()
                .deleteRecursively()
            home
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun failedAuthorizedCreationCleansOnlyTheRealEnvironmentDirectory() {
        val home =
            Files.createTempDirectory(
                "vexlife-heretic-env-cleanup-",
            )
                .toAbsolutePath()
        val root =
            HereticPythonEnvironment.environmentRoot(
                home,
            )
        val runner =
            FakeRunner()
                .also {
                    it.failPip = true
                }

        try {
            assertFailsWith<IllegalArgumentException> {
                HereticPythonEnvironment.ensure(
                    bootstrapPython = "python3.14",
                    environmentRoot = root,
                    allowCreation = true,
                    runner = runner,
                )
            }

            assertFalse(
                Files.exists(
                    root,
                ),
            )
        } finally {
            home
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun wrongExistingRuntimeVersionIsHeldWithoutCreationAuthority() {
        val home =
            Files.createTempDirectory(
                "vexlife-heretic-env-version-",
            )
                .toAbsolutePath()
        val root =
            HereticPythonEnvironment.environmentRoot(
                home,
            )
        val python =
            HereticPythonEnvironment.pythonPath(
                root,
            )
        val runner =
            FakeRunner()
                .also {
                    it.runtimePythonVersion =
                        "3.14.6"
                }

        try {
            Files.createDirectories(
                python.parent,
            )
            Files.writeString(
                python,
                "fake",
            )
            Files.writeString(
                root.resolve(
                    HereticPythonEnvironment.InstallReportFilename,
                ),
                "{}",
            )

            assertFailsWith<IllegalArgumentException> {
                HereticPythonEnvironment.ensure(
                    bootstrapPython = "python3.14",
                    environmentRoot = root,
                    allowCreation = false,
                    runner = runner,
                )
            }

            assertTrue(
                Files.exists(
                    root,
                ),
            )
        } finally {
            home
                .toFile()
                .deleteRecursively()
        }
    }
}

// [VXG RealForever]
