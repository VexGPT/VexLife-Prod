package vexlife.model.heretic.runtime

import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import vexlife.filesystem.VexOwnedPath
import vexlife.model.heretic.HereticProductIdentity

/**
 * Exact Python + mlx-lm environment owner for the Heretic runtime.
 *
 * It owns environment reuse/creation only. It does not own model bytes, runtime process lifetime,
 * Companion binding, or model activation.
 */
object HereticPythonEnvironment {
    const val InstallReportFilename =
        "vexlife-mlx-lm-install-report.json"

    const val EnvironmentDirectoryName =
        "python-3.14.7-mlx-lm-0.32.0"

    data class CommandResult(
        val status: Int?,
        val stdout: String,
        val stderr: String,
        val errorMessage: String? = null,
        val timedOut: Boolean = false,
    )

    fun interface Runner {
        fun run(
            program: String,
            args: List<String>,
            timeoutMs: Long,
        ): CommandResult
    }

    object SystemRunner : Runner {
        override fun run(
            program: String,
            args: List<String>,
            timeoutMs: Long,
        ): CommandResult =
            runCommand(
                program,
                args,
                timeoutMs,
            )
    }

    data class Probe(
        val pythonVersion: String,
        val mlxLmVersion: String,
        val prefix: String,
        val executable: String,
    )

    data class Result(
        val state: String,
        val pythonEnvironmentRoot: String,
        val pythonPath: String,
        val pythonVersion: String,
        val mlxLmVersion: String,
        val pipReportPath: String,
        val environmentMutationPerformed: Boolean,
    )

    fun environmentRoot(
        productHome: Path,
    ): Path {
        val home =
            VexOwnedPath.requireRoot(
                productHome,
                "VexLife-Prod Home",
            )

        return VexOwnedPath.resolveInside(
            home,
            "runtime/heretic/" +
                EnvironmentDirectoryName,
        )
    }

    fun pythonPath(
        environmentRoot: Path,
    ): Path {
        require(
            environmentRoot.isAbsolute,
        ) {
            "Heretic environment root must be absolute"
        }

        return environmentRoot
            .normalize()
            .resolve(
                "bin/python",
            )
    }

    /**
     * Fresh no-effect observation of one already-materialized environment.
     *
     * Missing returns null. Present-but-invalid fails closed. No repair/removal occurs here.
     */
    fun observeCurrent(
        productHome: Path,
        runner: Runner = SystemRunner,
    ): Result? {
        val home =
            VexOwnedPath.requireRoot(
                productHome,
                "VexLife-Prod Home",
            )
        val root =
            environmentRoot(
                home,
            )

        if (
            VexOwnedPath.observeDirectory(
                home,
                root,
            ) ==
            VexOwnedPath.DirectoryState.Missing
        ) {
            return null
        }

        requireRealDirectory(
            root,
            "environmentRoot",
        )

        val python =
            pythonPath(
                root,
            )
        val report =
            root.resolve(
                InstallReportFilename,
            )
        val probe =
            runtimeProbe(
                python,
                runner,
            )

        verifyProbe(
            probe,
            root,
            python,
        )
        requireRegularFile(
            report,
            "pip installation report",
        )

        return Result(
            state = "REUSED_VERIFIED",
            pythonEnvironmentRoot = root.toString(),
            pythonPath = python.toString(),
            pythonVersion = HereticProductIdentity.PythonVersion,
            mlxLmVersion = HereticProductIdentity.MlxLmVersion,
            pipReportPath = report.toString(),
            environmentMutationPerformed = false,
        )
    }

    fun ensure(
        bootstrapPython: String,
        productHome: Path,
        allowCreation: Boolean,
        runner: Runner = SystemRunner,
    ): Result {
        require(
            bootstrapPython.isNotBlank(),
        ) {
            "bootstrapPython must be one non-empty command or path"
        }

        val home =
            VexOwnedPath.requireRoot(
                productHome,
                "VexLife-Prod Home",
            )
        val root =
            environmentRoot(
                home,
            )
        val python =
            pythonPath(
                root,
            )
        val report =
            root.resolve(
                InstallReportFilename,
            )

        val existed =
            VexOwnedPath.observeDirectory(
                home,
                root,
            ) ==
                VexOwnedPath.DirectoryState.Present

        if (
            existed
        ) {
            // Identity/topology is checked before any repair removal. A symlink or non-directory
            // never becomes deletion authority.
            try {
                return requireNotNull(
                    observeCurrent(
                        home,
                        runner,
                    ),
                )
            } catch (
                error: Exception,
            ) {
                if (
                    !allowCreation
                ) {
                    throw error
                }
                removeRealDirectoryTree(
                    root,
                )
            }
        } else if (
            !allowCreation
        ) {
            error(
                "Heretic Python/mlx-lm environment is not locally reusable and creation was not authorized",
            )
        }

        VexOwnedPath.ensureDirectory(
            home,
            requireNotNull(
                root.parent,
            ),
        )

        val bootstrap =
            bootstrapProbe(
                bootstrapPython,
                runner,
            )

        require(
            bootstrap.first ==
                HereticProductIdentity.PythonVersion,
        ) {
            "bootstrap Python must be exactly " +
                HereticProductIdentity.PythonVersion
        }

        try {
            requireCommandPass(
                runner.run(
                    program = bootstrapPython,
                    args =
                        listOf(
                            "-m",
                            "venv",
                            root.toString(),
                        ),
                    timeoutMs = 120_000,
                ),
                "Heretic virtual environment creation",
            )

            requireRealDirectory(
                root,
                "environmentRoot",
            )

            requireCommandPass(
                runner.run(
                    program = python.toString(),
                    args =
                        listOf(
                            "-m",
                            "pip",
                            "install",
                            "--only-binary=:all:",
                            "--disable-pip-version-check",
                            "--no-input",
                            "--report",
                            report.toString(),
                            "mlx-lm==" +
                                HereticProductIdentity.MlxLmVersion,
                        ),
                    timeoutMs = 1_800_000,
                ),
                "binary-only mlx-lm installation",
            )

            val probe =
                runtimeProbe(
                    python,
                    runner,
                )
            verifyProbe(
                probe,
                root,
                python,
            )
            requireRegularFile(
                report,
                "pip installation report",
            )

            return Result(
                state = "CREATED_VERIFIED",
                pythonEnvironmentRoot = root.toString(),
                pythonPath = python.toString(),
                pythonVersion = HereticProductIdentity.PythonVersion,
                mlxLmVersion = HereticProductIdentity.MlxLmVersion,
                pipReportPath = report.toString(),
                environmentMutationPerformed = true,
            )
        } catch (
            error: Exception,
        ) {
            if (
                Files.exists(
                    root,
                    LinkOption.NOFOLLOW_LINKS,
                )
            ) {
                if (
                    Files.isSymbolicLink(
                        root,
                    )
                ) {
                    Files.deleteIfExists(
                        root,
                    )
                } else {
                    removeRealDirectoryTree(
                        root,
                    )
                }
            }
            throw error
        }
    }

    private fun runtimeProbe(
        pythonPath: Path,
        runner: Runner,
    ): Probe {
        val script =
            "import importlib.metadata as md,sys\n" +
                "import mlx_lm\n" +
                "import mlx_lm.server\n" +
                "print('%d.%d.%d'%sys.version_info[:3])\n" +
                "print(md.version('mlx-lm'))\n" +
                "print(sys.prefix)\n" +
                "print(sys.executable)"

        val output =
            requireCommandPass(
                runner.run(
                    program = pythonPath.toString(),
                    args =
                        listOf(
                            "-c",
                            script,
                        ),
                    timeoutMs = 60_000,
                ),
                "Heretic Python environment probe",
            )

        val lines =
            output
                .lineSequence()
                .map(
                    String::trim,
                )
                .filter(
                    String::isNotEmpty,
                )
                .toList()

        require(
            lines.size ==
                4,
        ) {
            "Heretic Python environment probe returned an invalid result"
        }

        return Probe(
            pythonVersion = lines[0],
            mlxLmVersion = lines[1],
            prefix = lines[2],
            executable = lines[3],
        )
    }

    private fun bootstrapProbe(
        bootstrapPython: String,
        runner: Runner,
    ): Pair<String, String> {
        val script =
            "import sys\n" +
                "print('%d.%d.%d'%sys.version_info[:3])\n" +
                "print(sys.executable)"

        val output =
            requireCommandPass(
                runner.run(
                    program = bootstrapPython,
                    args =
                        listOf(
                            "-c",
                            script,
                        ),
                    timeoutMs = 30_000,
                ),
                "bootstrap Python probe",
            )

        val lines =
            output
                .lineSequence()
                .map(
                    String::trim,
                )
                .filter(
                    String::isNotEmpty,
                )
                .toList()

        require(
            lines.size ==
                2,
        ) {
            "bootstrap Python probe returned an invalid result"
        }

        return lines[0] to
            lines[1]
    }

    private fun verifyProbe(
        probe: Probe,
        environmentRoot: Path,
        pythonPath: Path,
    ) {
        require(
            probe.pythonVersion ==
                HereticProductIdentity.PythonVersion &&
                probe.mlxLmVersion ==
                HereticProductIdentity.MlxLmVersion,
        ) {
            "Python/mlx-lm environment versions differ from the certified Heretic product identity"
        }
        require(
            Path.of(
                probe.prefix,
            )
                .normalize() ==
                environmentRoot &&
                Path.of(
                    probe.executable,
                )
                    .normalize() ==
                pythonPath,
        ) {
            "Python probe does not bind the selected environment root/executable"
        }
    }

    private fun requireCommandPass(
        result: CommandResult,
        label: String,
    ): String {
        require(
            result.errorMessage ==
                null &&
                !result.timedOut &&
                result.status ==
                0,
        ) {
            "$label failed: " +
                (
                    result.errorMessage
                        ?: result.stderr
                            .take(
                                4_000,
                            )
                )
        }

        return result.stdout
    }

    private fun requireRealDirectory(
        path: Path,
        label: String,
    ) {
        val attributes =
            Files.readAttributes(
                path,
                BasicFileAttributes::class.java,
                LinkOption.NOFOLLOW_LINKS,
            )

        require(
            attributes.isDirectory &&
                !attributes.isSymbolicLink,
        ) {
            "$label must be one real non-symlink directory"
        }
    }

    private fun requireRegularFile(
        path: Path,
        label: String,
    ) {
        require(
            Files.exists(
                path,
                LinkOption.NOFOLLOW_LINKS,
            ),
        ) {
            "$label is missing"
        }

        val attributes =
            Files.readAttributes(
                path,
                BasicFileAttributes::class.java,
                LinkOption.NOFOLLOW_LINKS,
            )

        require(
            attributes.isRegularFile &&
                !attributes.isSymbolicLink,
        ) {
            "$label must be one regular non-symlink file"
        }
    }

    private fun removeRealDirectoryTree(
        root: Path,
    ) {
        requireRealDirectory(
            root,
            "environmentRoot",
        )

        Files.walkFileTree(
            root,
            object :
                SimpleFileVisitor<Path>() {
                override fun visitFile(
                    file: Path,
                    attrs: BasicFileAttributes,
                ): FileVisitResult {
                    Files.delete(
                        file,
                    )
                    return FileVisitResult.CONTINUE
                }

                override fun postVisitDirectory(
                    directory: Path,
                    error: java.io.IOException?,
                ): FileVisitResult {
                    if (
                        error !=
                            null
                    ) {
                        throw error
                    }
                    Files.delete(
                        directory,
                    )
                    return FileVisitResult.CONTINUE
                }
            },
        )
    }

    private fun runCommand(
        program: String,
        args: List<String>,
        timeoutMs: Long,
    ): CommandResult =
        try {
            val process =
                ProcessBuilder(
                    listOf(
                        program,
                    ) +
                        args,
                )
                    .start()

            var stdout =
                ""
            var stderr =
                ""

            val stdoutThread =
                thread(
                    start = true,
                    name = "vexlife-heretic-command-stdout",
                ) {
                    stdout =
                        process.inputStream
                            .bufferedReader()
                            .readText()
                }
            val stderrThread =
                thread(
                    start = true,
                    name = "vexlife-heretic-command-stderr",
                ) {
                    stderr =
                        process.errorStream
                            .bufferedReader()
                            .readText()
                }

            val completed =
                process.waitFor(
                    timeoutMs,
                    TimeUnit.MILLISECONDS,
                )

            if (
                !completed
            ) {
                process.destroyForcibly()
                process.waitFor()
            }

            stdoutThread.join()
            stderrThread.join()

            CommandResult(
                status =
                    if (
                        completed
                    ) {
                        process.exitValue()
                    } else {
                        null
                    },
                stdout = stdout,
                stderr = stderr,
                timedOut =
                    !completed,
            )
        } catch (
            error: Exception,
        ) {
            CommandResult(
                status = null,
                stdout = "",
                stderr = "",
                errorMessage =
                    error.message
                        ?: error::class.simpleName
                        ?: "command failed",
            )
        }
}

// [VXG RealForever]
