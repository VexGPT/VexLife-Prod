package vexlife.platform.python

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import vexlife.artifact.DirectHttpsArtifactDelivery

class ManagedPythonTest {
    private class Runner(
        private val uvPath: Path,
        private val pythonPath: Path,
    ) : ManagedPython.Runner {
        val calls =
            mutableListOf<Pair<String, List<String>>>()

        override fun run(
            program: String,
            args: List<String>,
            environment: Map<String, String>,
            timeoutMs: Long,
        ): ManagedPython.CommandResult {
            calls +=
                program to
                    args

            return when {
                program ==
                    uvPath.toString() &&
                    args ==
                    listOf(
                        "--version",
                    ) ->
                    ManagedPython.CommandResult(
                        status = 0,
                        stdout =
                            "uv " +
                                ManagedPython.UvVersion +
                                "\n",
                        stderr = "",
                    )

                program ==
                    uvPath.toString() &&
                    args.take(
                        3,
                    ) ==
                    listOf(
                        "python",
                        "find",
                        "3.14.7",
                    ) ->
                    ManagedPython.CommandResult(
                        status = 0,
                        stdout =
                            pythonPath.toString() +
                                "\n",
                        stderr = "",
                    )

                program ==
                    pythonPath.toString() &&
                    args.firstOrNull() ==
                    "-c" ->
                    ManagedPython.CommandResult(
                        status = 0,
                        stdout =
                            "3.14.7\n" +
                                pythonPath
                                    .toRealPath()
                                    .toString() +
                                "\n",
                        stderr = "",
                    )

                else ->
                    error(
                        "unexpected managed Python fake command: $program $args",
                    )
            }
        }
    }

    @Test
    fun exactPrivateManagedPythonIsReusedWithoutNetworkOrInstallation() {
        val home =
            Files.createTempDirectory(
                "vexlife-prod-managed-python-reuse-",
            )
                .toAbsolutePath()
        val installRoot =
            home.resolve(
                ManagedPython.PythonInstallRelativePath,
            )
        val uvRoot =
            home.resolve(
                ManagedPython.UvToolRelativePath,
            )
        val cacheRoot =
            home.resolve(
                ManagedPython.UvCacheRelativePath,
            )
        val uv =
            uvRoot.resolve(
                "uv",
            )
        val python =
            installRoot.resolve(
                "cpython-3.14.7/bin/python3.14",
            )
        var downloads =
            0
        var extractions =
            0

        try {
            Files.createDirectories(
                uv.parent,
            )
            Files.createDirectories(
                cacheRoot,
            )
            Files.createDirectories(
                python.parent,
            )
            Files.writeString(
                uv,
                "fake-uv",
            )
            Files.writeString(
                python,
                "fake-python",
            )

            val runner =
                Runner(
                    uvPath = uv,
                    pythonPath = python,
                )

            val result =
                ManagedPython.ensure(
                    productHome = home,
                    pythonVersion = "3.14.7",
                    allowAcquisition = false,
                    downloader =
                        ManagedPython.Downloader {
                            _,
                            _,
                            ->
                            downloads +=
                                1
                            error(
                                "download must not run",
                            )
                        },
                    extractor =
                        ManagedPython.ArchiveExtractor {
                            _,
                            _,
                            ->
                            extractions +=
                                1
                            error(
                                "extract must not run",
                            )
                        },
                    runner = runner,
                )

            assertEquals(
                "REUSED_VERIFIED",
                result.state,
            )
            assertEquals(
                python.toString(),
                result.pythonPath,
            )
            assertFalse(
                result.uvAcquisitionPerformed,
            )
            assertFalse(
                result.pythonInstallationPerformed,
            )
            assertFalse(
                result.globalBinMutationPerformed,
            )
            assertEquals(
                0,
                downloads,
            )
            assertEquals(
                0,
                extractions,
            )
            assertEquals(
                3,
                runner.calls.size,
            )
        } finally {
            home
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun reuseOnlyMissingPrerequisitesPerformNoFilesystemFormation() {
        val home =
            Files.createTempDirectory(
                "vexlife-prod-managed-python-held-",
            )
                .toAbsolutePath()
        var downloads =
            0
        var commands =
            0

        try {
            assertFailsWith<IllegalArgumentException> {
                ManagedPython.ensure(
                    productHome = home,
                    pythonVersion = "3.14.7",
                    allowAcquisition = false,
                    downloader =
                        ManagedPython.Downloader {
                            _,
                            _,
                            ->
                            downloads +=
                                1
                            error(
                                "download must not run",
                            )
                        },
                    extractor =
                        ManagedPython.ArchiveExtractor {
                            _,
                            _,
                            ->
                            error(
                                "extract must not run",
                            )
                        },
                    runner =
                        ManagedPython.Runner {
                            _,
                            _,
                            _,
                            _,
                            ->
                            commands +=
                                1
                            error(
                                "command must not run",
                            )
                        },
                )
            }

            assertEquals(
                0,
                downloads,
            )
            assertEquals(
                0,
                commands,
            )
            assertFalse(
                Files.exists(
                    home.resolve(
                        "runtime",
                    ),
                ),
            )
        } finally {
            home
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun symlinkedRuntimeParentCannotRedirectManagedPythonEffects() {
        val parent =
            Files.createTempDirectory(
                "vexlife-prod-managed-python-link-",
            )
                .toAbsolutePath()
        val home =
            parent.resolve(
                "VexLife-Prod",
            )
        val outside =
            parent.resolve(
                "outside",
            )
        var downloads =
            0

        try {
            Files.createDirectory(
                home,
            )
            Files.createDirectory(
                outside,
            )
            Files.createSymbolicLink(
                home.resolve(
                    "runtime",
                ),
                outside,
            )

            assertFailsWith<IllegalArgumentException> {
                ManagedPython.ensure(
                    productHome = home,
                    pythonVersion = "3.14.7",
                    allowAcquisition = true,
                    downloader =
                        ManagedPython.Downloader {
                            _,
                            _,
                            ->
                            downloads +=
                                1
                            DirectHttpsArtifactDelivery.Result(
                                state = "VERIFIED",
                                disposition = "TEST",
                                path = "",
                                bytes = 0,
                                actualSha256 = "",
                                recordedSourceUrl = "",
                            )
                        },
                    extractor =
                        ManagedPython.ArchiveExtractor {
                            _,
                            _,
                            ->
                            error(
                                "extract must not run",
                            )
                        },
                    runner =
                        ManagedPython.Runner {
                            _,
                            _,
                            _,
                            _,
                            ->
                            error(
                                "command must not run",
                            )
                        },
                )
            }

            assertEquals(
                0,
                downloads,
            )
            assertFalse(
                Files.exists(
                    outside.resolve(
                        "python",
                    ),
                ),
            )
            assertFalse(
                Files.exists(
                    outside.resolve(
                        "tools",
                    ),
                ),
            )
        } finally {
            Files.deleteIfExists(
                home.resolve(
                    "runtime",
                ),
            )
            parent
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun archiveEntryValidationRejectsTraversalAndAbsolutePaths() {
        for (
            entries in
            listOf(
                listOf(
                    "../uv",
                ),
                listOf(
                    "/tmp/uv",
                ),
                listOf(
                    "uv-aarch64/../uv",
                ),
            )
        ) {
            assertFailsWith<IllegalArgumentException> {
                ManagedPython.validateArchiveEntries(
                    entries,
                )
            }
        }

        ManagedPython.validateArchiveEntries(
            listOf(
                "uv-aarch64-apple-darwin/uv",
                "uv-aarch64-apple-darwin/uvx",
            ),
        )
        assertTrue(
            true,
        )
    }
}

// [VXG RealForever]
