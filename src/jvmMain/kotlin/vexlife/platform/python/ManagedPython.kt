package vexlife.platform.python

import java.nio.charset.StandardCharsets
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.attribute.PosixFilePermission
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import vexlife.artifact.DirectHttpsArtifactDelivery
import vexlife.artifact.ExactArtifactFile
import vexlife.filesystem.VexOwnedPath

/**
 * Private exact managed-Python bootstrap for VexLife-Prod.
 *
 * uv is an implementation detail of this owner, not a product/runtime semantic owner.
 */
object ManagedPython {
    const val UvVersion =
        "0.13.7"

    const val UvMacArm64ArchiveName =
        "uv-aarch64-apple-darwin.tar.gz"

    const val UvMacArm64ArchiveBytes =
        14_347_867L

    const val UvMacArm64ArchiveSha256 =
        "81b811b57e4624df7ed3aac148e0359100d9962f3d9dce00aec533dc2e086731"

    const val UvMacArm64ArchiveUrl =
        "https://github.com/astral-sh/uv/releases/download/0.13.7/uv-aarch64-apple-darwin.tar.gz"

    const val UvReleaseSourceCommit =
        "316c827e058c16b71d5c507a20729c2995a7bcce"

    const val PythonInstallRelativePath =
        "runtime/python/managed"

    const val UvToolRelativePath =
        "runtime/tools/uv/0.13.7"

    const val UvCacheRelativePath =
        "runtime/cache/uv"

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
            environment: Map<String, String>,
            timeoutMs: Long,
        ): CommandResult
    }

    object SystemRunner : Runner {
        override fun run(
            program: String,
            args: List<String>,
            environment: Map<String, String>,
            timeoutMs: Long,
        ): CommandResult =
            runCommand(
                program,
                args,
                environment,
                timeoutMs,
            )
    }

    fun interface Downloader {
        fun download(
            request: DirectHttpsArtifactDelivery.Request,
            finalPath: Path,
        ): DirectHttpsArtifactDelivery.Result
    }

    object SystemDownloader : Downloader {
        override fun download(
            request: DirectHttpsArtifactDelivery.Request,
            finalPath: Path,
        ): DirectHttpsArtifactDelivery.Result =
            DirectHttpsArtifactDelivery.download(
                request = request,
                finalPath = finalPath,
            )
    }

    fun interface ArchiveExtractor {
        fun extractUv(
            archive: Path,
            stagingDirectory: Path,
        ): Path
    }

    object SystemArchiveExtractor : ArchiveExtractor {
        override fun extractUv(
            archive: Path,
            stagingDirectory: Path,
        ): Path {
            val list =
                requireCommandPass(
                    runCommand(
                        program = "/usr/bin/tar",
                        args =
                            listOf(
                                "-tzf",
                                archive.toString(),
                            ),
                        environment = emptyMap(),
                        timeoutMs = 60_000,
                    ),
                    "uv archive listing",
                )

            val entries =
                list
                    .lineSequence()
                    .map(
                        String::trim,
                    )
                    .filter(
                        String::isNotEmpty,
                    )
                    .toList()

            validateArchiveEntries(
                entries,
            )

            VexOwnedPath.requireRoot(
                stagingDirectory,
                "uv extraction staging directory",
            )

            requireCommandPass(
                runCommand(
                    program = "/usr/bin/tar",
                    args =
                        listOf(
                            "-xzf",
                            archive.toString(),
                            "-C",
                            stagingDirectory.toString(),
                        ),
                    environment = emptyMap(),
                    timeoutMs = 60_000,
                ),
                "uv archive extraction",
            )

            val candidates =
                Files.walk(
                    stagingDirectory,
                )
                    .use {
                        stream ->
                        stream
                            .filter {
                                path ->
                                path.fileName
                                    ?.toString() ==
                                    "uv" &&
                                    Files.isRegularFile(
                                        path,
                                        LinkOption.NOFOLLOW_LINKS,
                                    ) &&
                                    !Files.isSymbolicLink(
                                        path,
                                    )
                            }
                            .toList()
                    }

            require(
                candidates.size ==
                    1,
            ) {
                "uv archive must contain exactly one regular uv executable"
            }

            return candidates.single()
        }
    }

    data class Result(
        val state: String,
        val pythonVersion: String,
        val pythonPath: String,
        val pythonInstallRoot: String,
        val uvVersion: String,
        val uvPath: String,
        val uvArchiveSha256: String,
        val uvReleaseSourceCommit: String,
        val uvAcquisitionPerformed: Boolean,
        val pythonInstallationPerformed: Boolean,
        val globalBinMutationPerformed: Boolean,
    )

    fun ensure(
        productHome: Path,
        pythonVersion: String,
        allowAcquisition: Boolean,
        downloader: Downloader = SystemDownloader,
        extractor: ArchiveExtractor = SystemArchiveExtractor,
        runner: Runner = SystemRunner,
    ): Result {
        val home =
            VexOwnedPath.requireRoot(
                productHome,
                "VexLife-Prod Home",
            )
        require(
            pythonVersion.matches(
                Regex(
                    "\\d+\\.\\d+\\.\\d+",
                ),
            ),
        ) {
            "managed Python version must be one exact patch version"
        }

        val pythonInstallRoot =
            VexOwnedPath.resolveInside(
                home,
                PythonInstallRelativePath,
            )
        val uvToolRoot =
            VexOwnedPath.resolveInside(
                home,
                UvToolRelativePath,
            )
        val uvCacheRoot =
            VexOwnedPath.resolveInside(
                home,
                UvCacheRelativePath,
            )

        val uvToolState =
            VexOwnedPath.observeDirectory(
                home,
                uvToolRoot,
            )
        val uvCacheState =
            VexOwnedPath.observeDirectory(
                home,
                uvCacheRoot,
            )
        val pythonInstallState =
            VexOwnedPath.observeDirectory(
                home,
                pythonInstallRoot,
            )

        if (
            allowAcquisition
        ) {
            VexOwnedPath.ensureDirectory(
                home,
                uvToolRoot,
            )
            VexOwnedPath.ensureDirectory(
                home,
                uvCacheRoot,
            )
            VexOwnedPath.ensureDirectory(
                home,
                pythonInstallRoot,
            )
        } else {
            require(
                uvToolState ==
                    VexOwnedPath.DirectoryState.Present &&
                    uvCacheState ==
                    VexOwnedPath.DirectoryState.Present &&
                    pythonInstallState ==
                    VexOwnedPath.DirectoryState.Present,
            ) {
                "managed Python prerequisites are not exactly reusable and acquisition was not authorized"
            }
        }

        val uvBinary =
            uvToolRoot.resolve(
                "uv",
            )
        val uvArchive =
            uvToolRoot.resolve(
                UvMacArm64ArchiveName,
            )

        var uvAcquisitionPerformed =
            false

        if (
            Files.exists(
                uvBinary,
                LinkOption.NOFOLLOW_LINKS,
            )
        ) {
            requireRealFile(
                uvBinary,
                "uv binary",
            )
            verifyUvVersion(
                uvBinary,
                runner,
            )
        } else {
            require(
                allowAcquisition,
            ) {
                "exact uv bootstrap tool is missing and acquisition was not authorized"
            }

            when (
                ExactArtifactFile.classify(
                    path = uvArchive,
                    expectedSha256 = UvMacArm64ArchiveSha256,
                    expectedBytes = UvMacArm64ArchiveBytes,
                )
            ) {
                is ExactArtifactFile.Classification.VerifiedReusable ->
                    Unit

                is ExactArtifactFile.Classification.Missing -> {
                    downloader.download(
                        request =
                            DirectHttpsArtifactDelivery.Request(
                                url = UvMacArm64ArchiveUrl,
                                expectedSha256 = UvMacArm64ArchiveSha256,
                                expectedBytes = UvMacArm64ArchiveBytes,
                                maxBytes = UvMacArm64ArchiveBytes,
                            ),
                        finalPath = uvArchive,
                    )
                    uvAcquisitionPerformed =
                        true
                }

                else ->
                    error(
                        "existing uv release archive is not exact; repair must be explicit",
                    )
            }

            require(
                ExactArtifactFile.classify(
                    path = uvArchive,
                    expectedSha256 = UvMacArm64ArchiveSha256,
                    expectedBytes = UvMacArm64ArchiveBytes,
                ) is
                    ExactArtifactFile.Classification.VerifiedReusable,
            ) {
                "uv release archive did not become exact"
            }

            val staging =
                VexOwnedPath.resolveInside(
                    uvToolRoot,
                    ".extract-" +
                        UUID.randomUUID(),
                )

            VexOwnedPath.ensureDirectory(
                uvToolRoot,
                staging,
            )

            try {
                val extracted =
                    extractor.extractUv(
                        uvArchive,
                        staging,
                    )

                require(
                    extracted.startsWith(
                        staging,
                    ),
                ) {
                    "uv extractor returned a path outside the private staging directory"
                }
                requireRealFile(
                    extracted,
                    "extracted uv binary",
                )

                val temporary =
                    uvToolRoot.resolve(
                        ".uv.partial-" +
                            UUID.randomUUID(),
                    )

                Files.copy(
                    extracted,
                    temporary,
                )
                makeOwnerExecutable(
                    temporary,
                )

                try {
                    Files.move(
                        temporary,
                        uvBinary,
                        StandardCopyOption.ATOMIC_MOVE,
                    )
                } catch (
                    _: AtomicMoveNotSupportedException,
                ) {
                    Files.deleteIfExists(
                        temporary,
                    )
                    error(
                        "atomic uv tool formation is unavailable",
                    )
                }

                makeOwnerExecutable(
                    uvBinary,
                )
                verifyUvVersion(
                    uvBinary,
                    runner,
                )
            } finally {
                if (
                    Files.exists(
                        staging,
                        LinkOption.NOFOLLOW_LINKS,
                    )
                ) {
                    removeOwnedDirectoryTree(
                        staging,
                    )
                }
            }
        }

        val environment =
            inheritedEnvironment() +
                mapOf(
                    "UV_PYTHON_INSTALL_DIR" to
                        pythonInstallRoot.toString(),
                    "UV_CACHE_DIR" to
                        uvCacheRoot.toString(),
                )

        val alreadyInstalled =
            findManagedPython(
                uvBinary = uvBinary,
                pythonVersion = pythonVersion,
                pythonInstallRoot = pythonInstallRoot,
                environment = environment,
                runner = runner,
                required = false,
            )

        var pythonInstallationPerformed =
            false

        val python =
            if (
                alreadyInstalled !=
                    null
            ) {
                alreadyInstalled
            } else {
                require(
                    allowAcquisition,
                ) {
                    "exact managed Python $pythonVersion is missing and acquisition was not authorized"
                }

                requireCommandPass(
                    runner.run(
                        program = uvBinary.toString(),
                        args =
                            listOf(
                                "python",
                                "install",
                                pythonVersion,
                                "--no-bin",
                            ),
                        environment = environment,
                        timeoutMs = 1_800_000,
                    ),
                    "exact managed Python installation",
                )

                pythonInstallationPerformed =
                    true

                requireNotNull(
                    findManagedPython(
                        uvBinary = uvBinary,
                        pythonVersion = pythonVersion,
                        pythonInstallRoot = pythonInstallRoot,
                        environment = environment,
                        runner = runner,
                        required = true,
                    ),
                )
            }

        verifyPython(
            pythonPath = python,
            pythonVersion = pythonVersion,
            installRoot = pythonInstallRoot,
            environment = environment,
            runner = runner,
        )

        return Result(
            state =
                if (
                    pythonInstallationPerformed
                ) {
                    "INSTALLED_VERIFIED"
                } else {
                    "REUSED_VERIFIED"
                },
            pythonVersion = pythonVersion,
            pythonPath = python.toString(),
            pythonInstallRoot = pythonInstallRoot.toString(),
            uvVersion = UvVersion,
            uvPath = uvBinary.toString(),
            uvArchiveSha256 = UvMacArm64ArchiveSha256,
            uvReleaseSourceCommit = UvReleaseSourceCommit,
            uvAcquisitionPerformed = uvAcquisitionPerformed,
            pythonInstallationPerformed = pythonInstallationPerformed,
            globalBinMutationPerformed = false,
        )
    }

    fun validateArchiveEntries(
        entries: List<String>,
    ) {
        require(
            entries.isNotEmpty(),
        ) {
            "uv release archive must not be empty"
        }

        for (
            entry in
            entries
        ) {
            val normalized =
                entry.replace(
                    '\\',
                    '/',
                )
            require(
                !normalized.startsWith(
                    "/",
                ),
            ) {
                "uv archive contained an absolute path"
            }
            require(
                normalized
                    .split(
                        '/',
                    )
                    .none {
                        it ==
                            ".."
                    },
            ) {
                "uv archive contained parent traversal"
            }
        }
    }

    private fun findManagedPython(
        uvBinary: Path,
        pythonVersion: String,
        pythonInstallRoot: Path,
        environment: Map<String, String>,
        runner: Runner,
        required: Boolean,
    ): Path? {
        val result =
            runner.run(
                program = uvBinary.toString(),
                args =
                    listOf(
                        "python",
                        "find",
                        pythonVersion,
                        "--managed-python",
                    ),
                environment = environment,
                timeoutMs = 60_000,
            )

        if (
            result.status !=
                0 ||
                result.errorMessage !=
                null ||
                result.timedOut
        ) {
            if (
                required
            ) {
                error(
                    "uv could not locate exact managed Python $pythonVersion after installation",
                )
            }
            return null
        }

        val value =
            result.stdout
                .lineSequence()
                .map(
                    String::trim,
                )
                .firstOrNull(
                    String::isNotEmpty,
                )
                ?: if (
                    required
                ) {
                    error(
                        "uv python find returned no executable path",
                    )
                } else {
                    return null
                }

        val path =
            Path.of(
                value,
            )

        require(
            path.isAbsolute,
        ) {
            "uv python find must return one absolute executable path"
        }

        val normalized =
            path.normalize()

        require(
            normalized.startsWith(
                pythonInstallRoot,
            ),
        ) {
            "managed Python executable escaped the VexLife-Prod install root"
        }

        return normalized
    }

    private fun verifyUvVersion(
        uvBinary: Path,
        runner: Runner,
    ) {
        val result =
            requireCommandPass(
                runner.run(
                    program = uvBinary.toString(),
                    args =
                        listOf(
                            "--version",
                        ),
                    environment = emptyMap(),
                    timeoutMs = 30_000,
                ),
                "uv version probe",
            )

        require(
            result
                .trim()
                .startsWith(
                    "uv $UvVersion",
                ),
        ) {
            "uv binary version does not match pinned $UvVersion"
        }
    }

    private fun verifyPython(
        pythonPath: Path,
        pythonVersion: String,
        installRoot: Path,
        environment: Map<String, String>,
        runner: Runner,
    ) {
        require(
            Files.exists(
                pythonPath,
            ),
        ) {
            "managed Python executable does not exist"
        }

        val script =
            "import os,sys\n" +
                "print('%d.%d.%d'%sys.version_info[:3])\n" +
                "print(os.path.realpath(sys.executable))"

        val output =
            requireCommandPass(
                runner.run(
                    program = pythonPath.toString(),
                    args =
                        listOf(
                            "-c",
                            script,
                        ),
                    environment = environment,
                    timeoutMs = 30_000,
                ),
                "managed Python probe",
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
                2 &&
                lines[0] ==
                pythonVersion,
        ) {
            "managed Python version does not match exact requested patch version"
        }

        val executableRealPath =
            Path.of(
                lines[1],
            )
                .normalize()
        val installRealPath =
            installRoot
                .toRealPath()

        require(
            executableRealPath.startsWith(
                installRealPath,
            ),
        ) {
            "managed Python real executable escaped its VexLife-Prod install root"
        }
    }

    private fun requireCommandPass(
        result: CommandResult,
        label: String,
    ): String {
        require(
            result.status ==
                0 &&
                result.errorMessage ==
                null &&
                !result.timedOut,
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

    private fun requireRealFile(
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
            attributes.isRegularFile &&
                !attributes.isSymbolicLink,
        ) {
            "$label must be one regular non-symlink file"
        }
    }

    private fun makeOwnerExecutable(
        path: Path,
    ) {
        try {
            Files.setPosixFilePermissions(
                path,
                setOf(
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE,
                    PosixFilePermission.OWNER_EXECUTE,
                ),
            )
        } catch (
            _: UnsupportedOperationException,
        ) {
            require(
                path.toFile()
                    .setExecutable(
                        true,
                        true,
                    ),
            ) {
                "uv binary could not be made executable"
            }
        }
    }

    private fun removeOwnedDirectoryTree(
        root: Path,
    ) {
        require(
            Files.isDirectory(
                root,
                LinkOption.NOFOLLOW_LINKS,
            ) &&
                !Files.isSymbolicLink(
                    root,
                ),
        ) {
            "managed Python private staging root must be one real directory"
        }

        Files.walk(
            root,
        )
            .sorted(
                Comparator.reverseOrder(),
            )
            .use {
                paths ->
                paths.forEach(
                    Files::delete,
                )
            }
    }

    private fun inheritedEnvironment(): Map<String, String> =
        listOf(
            "HOME",
            "PATH",
            "TMPDIR",
            "LANG",
            "LC_ALL",
            "USER",
            "SSL_CERT_FILE",
            "SSL_CERT_DIR",
            "HTTPS_PROXY",
            "HTTP_PROXY",
            "NO_PROXY",
        )
            .mapNotNull {
                key ->
                System.getenv(
                    key,
                )
                    ?.takeIf(
                        String::isNotEmpty,
                    )
                    ?.let {
                        value ->
                        key to
                            value
                    }
            }
            .toMap()

    private fun runCommand(
        program: String,
        args: List<String>,
        environment: Map<String, String>,
        timeoutMs: Long,
    ): CommandResult =
        try {
            val builder =
                ProcessBuilder(
                    listOf(
                        program,
                    ) +
                        args,
                )

            builder
                .environment()
                .putAll(
                    environment,
                )

            val process =
                builder.start()

            var stdout =
                ""
            var stderr =
                ""

            val stdoutThread =
                thread(
                    start = true,
                    name = "vexlife-prod-managed-python-stdout",
                ) {
                    stdout =
                        process.inputStream
                            .bufferedReader(
                                StandardCharsets.UTF_8,
                            )
                            .readText()
                }
            val stderrThread =
                thread(
                    start = true,
                    name = "vexlife-prod-managed-python-stderr",
                ) {
                    stderr =
                        process.errorStream
                            .bufferedReader(
                                StandardCharsets.UTF_8,
                            )
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
