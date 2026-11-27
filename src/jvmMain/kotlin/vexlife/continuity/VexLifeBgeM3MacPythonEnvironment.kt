package vexlife.continuity

import java.nio.charset.StandardCharsets
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.StandardOpenOption
import java.nio.file.attribute.BasicFileAttributes
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import vexlife.filesystem.VexOwnedPath
import vexlife.platform.python.ManagedPython

/**
 * Exact Mac-arm64 Python + ONNX environment for BGE-M3 continuity retrieval.
 *
 * Environment custody is separate from representation identity, model bytes, worker lifetime and
 * semantic acceptance. The lock below is formed from the exact pip installation report returned by
 * the Mac runtime-custody qualification.
 */
object VexLifeBgeM3MacPythonEnvironment {
    const val PythonVersion =
        "3.14.7"

    const val NumPyVersion =
        "2.5.5"

    const val OnnxRuntimeVersion =
        "1.32.0"

    const val TransformersVersion =
        "5.24.1"

    const val HuggingFaceHubVersion =
        "2.5.0"

    const val RequirementsFilename =
        "vexlife-bge-m3-mac-a001.lock"

    const val InstallReportFilename =
        "vexlife-bge-m3-mac-a001-pip-report.json"

    const val RequirementsSha256 =
        "6f8ac7a80f51130da147f8e0755c724961b7f897ea86de9487bc600674707510"

    const val EnvironmentDirectoryName =
        "python-3.14.7-bge-m3-onnx-cpu-a001"

    private val RequirementsText =
        listOf(
            "numpy==2.5.5 --hash=sha256:229743571f901a082f031f511dc4f926b4b4a714075d98b1ff2af62d836365ac",
            "onnxruntime==1.32.0 --hash=sha256:ab327b736552324af8d2a38bef5a65ed773f67184a36ff642beec7f5b4e9b03f",
            "transformers==5.24.1 --hash=sha256:505b39d3e45a3715f3acb81b858eb245ce1a462373b7f99e26f0dcd41ed4cd0f",
            "huggingface_hub==2.5.0 --hash=sha256:f8565260f2031f5a62301d01d8d80bf6f889390f3c3f1b1cfe6d7e10b5a02db8",
            "click==8.5.1 --hash=sha256:8bb5376d2006950386f8c6427613334a2acadc5f559cb80e638d00b4df957e37",
            "hf-xet==1.8.0 --hash=sha256:3e8934784425781f0f779b91044df441f3a7af4dab47375d7e3aeaecbb8eecb4",
            "httpx2==2.16.2 --hash=sha256:f05c13aade4c45dbf1c9060647a1058f0acad62398fad96e741269edb33d46f5",
            "httpcore2==2.16.2 --hash=sha256:16051ae2a42f776007543f32de9349a317b23d2cbb4e1456b4bac0e88b87419a",
            "tokenizers==0.23.3 --hash=sha256:68649e97d5b43c44c031d8d848874a6eecae8f8fe40ea989aa777a5a83aca716",
            "anyio==4.16.0 --hash=sha256:c66ae634e0b8228396b801741beb3294326d4b6102f7bba4041c0dfb58c29026",
            "filelock==4.3.1 --hash=sha256:79e43df8e9fe509cd83a3899a7c186c8077cbbe4d200a1ed3705c2f3bb6bc487",
            "fsspec==2026.9.0 --hash=sha256:8dd6e646e99ea382bd85f97a45e6b526a442d79423a7dc673f1e2756d05fcb5f",
            "h11==0.16.0 --hash=sha256:63cf8bbe7522de3bf65932da54b31cb6c86",
            "idna==3.20 --hash=sha256:ab7ae7122974553370f0bdb919e1a960b2cd1bc1ef0276416d896db81c14582c",
            "packaging==26.4 --hash=sha256:81383611a09c1bc0b08b76e0fd5829d7c6824c65b27a6aee339da133f4233ea2",
            "protobuf==7.36.2 --hash=sha256:cbc70b17ee27e28894c7fee8bb04be1abead49e936bc70eb60052531eee2079e",
            "PyYAML==6.0.4 --hash=sha256:7335809813ec838d9cd334b1744cda33deb141c9ef23a9c0df371c37980d3851",
            "regex==2026.11.25 --hash=sha256:390db23deb9b207af2f995866f6547754fea5509a5708f7cf03a451e7213e32d",
            "safetensors==0.8.0 --hash=sha256:c80201d22cbf405b80647a60ada77bba06c8fba2da2743ba1e89cdcc39a81f25",
            "tqdm==4.70.1 --hash=sha256:c293e525e6fef9c20e8728fd4612df02a0aa31bb5fe91ecd93e123b1b7bffa73",
            "truststore==0.10.5 --hash=sha256:9aaaedaefaf06d8b206278cf8b5012bc897f485a874503501e12d776df78951c",
            "typing_extensions==4.16.0 --hash=sha256:481caa481374e813c1b176ada14e97f1f67a4539ce9cfeb3f350d78d6370c2e8",
            "flatbuffers==25.12.19 --hash=sha256:7634f50c427838bb021c2d66a3d1168e9d199b0607e6329399f04846d42e20b4",
            "typer==0.29.3 --hash=sha256:1fd37275046faecc86caa2e0e313978c10db71533fc0fe1a48f4e03a60efc23c",
            "annotated-doc==0.0.7 --hash=sha256:6bc31bbbec6dfaaedc1676dbc11dbda96fe22a2b5c400e9ebad113fec44a9dc6",
            "pydantic==2.14.4 --hash=sha256:653a2ad9f6d8858529c77c72717d0ea991d6c6837fe683246b2f4b2bbfc3fadc",
            "pydantic_core==2.50.5 --hash=sha256:e8fe516a4b3b06e4ff83b9adee4a786d28a617e5fb5fe90bda49fb6f52a5ac19",
            "annotated-types==0.8.0 --hash=sha256:f072f4d804ea359e4eaf198b1af7a8b0943881a87f31bb764f8bf219bb9419e0",
            "rich==15.0.0 --hash=sha256:33bd4ef74232fb73fe9279a257718407f169c09b78a87ad3d296f548e27de0bb",
            "Pygments==2.21.0 --hash=sha256:2363c69b61c4a97c838da3b130dcd6468f4848992b21a82f2a63ec34377137d9",
            "markdown-it-py==4.3.0 --hash=sha256:7aa1511a46c39138de78d9eba92ce47e63124d8003fc98b3a351909e04239efa",
            "mdurl==0.1.2 --hash=sha256:84008a41e51615a49fc9966191ff91509e3c40b939176e643fd50a5c2196b8f8",
            "shellingham==1.5.4 --hash=sha256:7ecfff8f2fd72616f7481040475a65b2bf8af90a56c89140852d1120324e8686",
            "typing-inspection==0.4.5 --hash=sha256:bc210535442b39e1bf2805e0c7a17dfd6a4ace760c84c7f7d16245d23a6b1c39",
        )
            .joinToString(
                separator =
                    "\n",
                postfix =
                    "\n",
            )

    init {
        require(
            sha256(
                RequirementsText.toByteArray(
                    StandardCharsets.UTF_8,
                ),
            ) ==
                RequirementsSha256
        ) {
            "BGE-M3 Mac requirements lock does not match qualified source bytes"
        }
    }

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

    object SystemRunner :
        Runner {
        override fun run(
            program: String,
            args: List<String>,
            timeoutMs: Long,
        ): CommandResult =
            runCommand(
                program = program,
                args = args,
                timeoutMs = timeoutMs,
            )
    }

    data class Probe(
        val pythonVersion: String,
        val numpyVersion: String,
        val onnxRuntimeVersion: String,
        val transformersVersion: String,
        val huggingFaceHubVersion: String,
        val cpuExecutionProviderAvailable: Boolean,
        val prefix: String,
        val executable: String,
    )

    data class Result(
        val state: String,
        val environmentRoot: String,
        val pythonPath: String,
        val requirementsPath: String,
        val requirementsSha256: String,
        val pipReportPath: String,
        val pipReportSha256: String,
        val pythonVersion: String,
        val numpyVersion: String,
        val onnxRuntimeVersion: String,
        val transformersVersion: String,
        val huggingFaceHubVersion: String,
        val environmentFingerprintSha256: String,
        val environmentMutationPerformed: Boolean,
    )

    fun environmentRoot(
        productHome: Path,
    ): Path =
        VexOwnedPath.resolveInside(
            VexOwnedPath.requireRoot(
                productHome,
                "VexLife-Prod Home",
            ),
            "runtime/continuity/semantic/bge-m3/environment/" +
                EnvironmentDirectoryName,
        )

    fun pythonPath(
        environmentRoot: Path,
    ): Path {
        require(
            environmentRoot.isAbsolute,
        ) {
            "BGE-M3 Mac environment root must be absolute"
        }

        return environmentRoot
            .normalize()
            .resolve(
                "bin/python",
            )
    }

    /** Missing is held capability. Present-but-invalid environment truth fails closed. */
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
            "BGE-M3 Mac environment root",
        )

        val python =
            pythonPath(
                root,
            )
        val requirements =
            root.resolve(
                RequirementsFilename,
            )
        val report =
            root.resolve(
                InstallReportFilename,
            )

        requireRegularFile(
            requirements,
            "BGE-M3 requirements lock",
        )
        val lockBytes =
            Files.readAllBytes(
                requirements,
            )
        require(
            lockBytes.contentEquals(
                RequirementsText.toByteArray(
                    StandardCharsets.UTF_8,
                ),
            ) &&
                sha256(
                    lockBytes,
                ) ==
                RequirementsSha256
        ) {
            "BGE-M3 requirements lock differs from qualified source"
        }

        requireRegularFile(
            report,
            "BGE-M3 pip installation report",
        )
        validatePipReport(
            report,
        )
        val reportSha256 =
            sha256(
                Files.readAllBytes(
                    report,
                ),
            )

        val probe =
            runtimeProbe(
                python,
                runner,
            )
        verifyProbe(
            probe = probe,
            environmentRoot = root,
            pythonPath = python,
        )

        val fingerprint =
            sha256(
                listOf(
                    RequirementsSha256,
                    reportSha256,
                    probe.pythonVersion,
                    probe.numpyVersion,
                    probe.onnxRuntimeVersion,
                    probe.transformersVersion,
                    probe.huggingFaceHubVersion,
                    VexLifeBgeM3DenseIdentity.ExecutionProvider,
                    root.toString(),
                    python.toString(),
                )
                    .joinToString(
                        separator =
                            "\n",
                        postfix =
                            "\n",
                    )
                    .toByteArray(
                        StandardCharsets.UTF_8,
                    ),
            )

        return Result(
            state =
                "REUSED_VERIFIED",
            environmentRoot =
                root.toString(),
            pythonPath =
                python.toString(),
            requirementsPath =
                requirements.toString(),
            requirementsSha256 =
                RequirementsSha256,
            pipReportPath =
                report.toString(),
            pipReportSha256 =
                reportSha256,
            pythonVersion =
                probe.pythonVersion,
            numpyVersion =
                probe.numpyVersion,
            onnxRuntimeVersion =
                probe.onnxRuntimeVersion,
            transformersVersion =
                probe.transformersVersion,
            huggingFaceHubVersion =
                probe.huggingFaceHubVersion,
            environmentFingerprintSha256 =
                fingerprint,
            environmentMutationPerformed =
                false,
        )
    }

    fun ensure(
        managedPython: ManagedPython.Result,
        productHome: Path,
        allowCreation: Boolean,
        runner: Runner = SystemRunner,
    ): Result? {
        val home =
            VexOwnedPath.requireRoot(
                productHome,
                "VexLife-Prod Home",
            )
        val bootstrapPython =
            validateManagedPython(
                home = home,
                managedPython = managedPython,
            )
        val root =
            environmentRoot(
                home,
            )

        if (
            Files.exists(
                root,
                LinkOption.NOFOLLOW_LINKS,
            )
        ) {
            try {
                return observeCurrent(
                    productHome =
                        home,
                    runner =
                        runner,
                )
            } catch (
                error: Exception,
            ) {
                if (
                    !allowCreation
                ) {
                    throw error
                }

                require(
                    VexOwnedPath.observeDirectory(
                        home,
                        root,
                    ) ==
                        VexOwnedPath.DirectoryState.Present
                )
                removeRealDirectoryTree(
                    root,
                )
            }
        } else if (
            !allowCreation
        ) {
            return null
        }

        VexOwnedPath.ensureDirectory(
            home,
            requireNotNull(
                root.parent,
            ),
        )

        val bootstrap =
            bootstrapProbe(
                bootstrapPython =
                    bootstrapPython,
                runner =
                    runner,
            )
        require(
            bootstrap.first ==
                PythonVersion &&
                Path.of(
                    bootstrap.second,
                )
                    .normalize() ==
                bootstrapPython
        ) {
            "managed bootstrap Python does not match the BGE-M3 Mac prerequisite"
        }

        val python =
            pythonPath(
                root,
            )
        val requirements =
            root.resolve(
                RequirementsFilename,
            )
        val report =
            root.resolve(
                InstallReportFilename,
            )

        try {
            requireCommandPass(
                runner.run(
                    program =
                        bootstrapPython.toString(),
                    args =
                        listOf(
                            "-m",
                            "venv",
                            root.toString(),
                        ),
                    timeoutMs =
                        120_000,
                ),
                "BGE-M3 virtual environment creation",
            )

            requireRealDirectory(
                root,
                "BGE-M3 Mac environment root",
            )

            Files.write(
                requirements,
                RequirementsText.toByteArray(
                    StandardCharsets.UTF_8,
                ),
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE,
            )

            requireCommandPass(
                runner.run(
                    program =
                        python.toString(),
                    args =
                        listOf(
                            "-m",
                            "pip",
                            "install",
                            "--isolated",
                            "--only-binary=:all:",
                            "--disable-pip-version-check",
                            "--no-input",
                            "--require-hashes",
                            "--report",
                            report.toString(),
                            "-r",
                            requirements.toString(),
                        ),
                    timeoutMs =
                        1_800_000,
                ),
                "hash-locked BGE-M3 Python environment installation",
            )

            val observed =
                requireNotNull(
                    observeCurrent(
                        productHome =
                            home,
                        runner =
                            runner,
                    ),
                )

            return observed.copy(
                state =
                    "CREATED_VERIFIED",
                environmentMutationPerformed =
                    true,
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
            """
import importlib.metadata as md
import os
import sys
import onnxruntime as ort
print('%d.%d.%d' % sys.version_info[:3])
print(md.version('numpy'))
print(md.version('onnxruntime'))
print(md.version('transformers'))
print(md.version('huggingface-hub'))
print('CPU' if 'CPUExecutionProvider' in ort.get_available_providers() else 'MISSING')
print(os.path.realpath(sys.prefix))
print(os.path.realpath(sys.executable))
""".trimIndent()

        val output =
            requireCommandPass(
                runner.run(
                    program =
                        pythonPath.toString(),
                    args =
                        listOf(
                            "-c",
                            script,
                        ),
                    timeoutMs =
                        60_000,
                ),
                "BGE-M3 Python environment probe",
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
                8
        ) {
            "BGE-M3 Python environment probe returned an invalid result"
        }

        return Probe(
            pythonVersion =
                lines[0],
            numpyVersion =
                lines[1],
            onnxRuntimeVersion =
                lines[2],
            transformersVersion =
                lines[3],
            huggingFaceHubVersion =
                lines[4],
            cpuExecutionProviderAvailable =
                lines[5] ==
                    "CPU",
            prefix =
                lines[6],
            executable =
                lines[7],
        )
    }

    private fun verifyProbe(
        probe: Probe,
        environmentRoot: Path,
        pythonPath: Path,
    ) {
        require(
            probe.pythonVersion ==
                PythonVersion &&
                probe.numpyVersion ==
                NumPyVersion &&
                probe.onnxRuntimeVersion ==
                OnnxRuntimeVersion &&
                probe.transformersVersion ==
                TransformersVersion &&
                probe.huggingFaceHubVersion ==
                HuggingFaceHubVersion &&
                probe.cpuExecutionProviderAvailable
        ) {
            "BGE-M3 Python environment package/runtime identity differs from qualification"
        }

        require(
            Path.of(
                probe.prefix,
            )
                .normalize() ==
                environmentRoot
                    .toRealPath() &&
                Path.of(
                    probe.executable,
                )
                    .normalize() ==
                pythonPath
                    .toRealPath()
        ) {
            "BGE-M3 Python environment probe escaped the selected environment"
        }
    }

    private fun bootstrapProbe(
        bootstrapPython: Path,
        runner: Runner,
    ): Pair<String, String> {
        val script =
            """
import os
import sys
print('%d.%d.%d' % sys.version_info[:3])
print(os.path.realpath(sys.executable))
""".trimIndent()

        val output =
            requireCommandPass(
                runner.run(
                    program =
                        bootstrapPython.toString(),
                    args =
                        listOf(
                            "-c",
                            script,
                        ),
                    timeoutMs =
                        30_000,
                ),
                "BGE-M3 managed Python bootstrap probe",
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
                2
        )

        return lines[0] to
            lines[1]
    }

    private fun validateManagedPython(
        home: Path,
        managedPython: ManagedPython.Result,
    ): Path {
        val managedRoot =
            VexOwnedPath.resolveInside(
                home,
                ManagedPython.PythonInstallRelativePath,
            )
        val python =
            Path.of(
                managedPython.pythonPath,
            )
                .normalize()

        require(
            (
                managedPython.state ==
                    "REUSED_VERIFIED" ||
                    managedPython.state ==
                    "INSTALLED_VERIFIED"
            ) &&
                managedPython.pythonVersion ==
                PythonVersion &&
                managedPython.uvVersion ==
                ManagedPython.UvVersion &&
                !managedPython.globalBinMutationPerformed &&
                Path.of(
                    managedPython.pythonInstallRoot,
                )
                    .normalize() ==
                managedRoot &&
                python.startsWith(
                    managedRoot,
                )
        ) {
            "BGE-M3 environment requires one exact VexLife managed Python result"
        }

        requireRegularFile(
            python,
            "BGE-M3 managed bootstrap Python",
        )

        return python
    }

    private fun validatePipReport(
        report: Path,
    ) {
        val install =
            requireNotNull(
                Json
                    .parseToJsonElement(
                        Files.readString(
                            report,
                            StandardCharsets.UTF_8,
                        ),
                    )
                    .jsonObject[
                        "install"
                    ],
            )
                .jsonArray

        val actual =
            install.associate {
                entry ->
                val value =
                    entry.jsonObject
                val metadata =
                    requireNotNull(
                        value[
                            "metadata"
                        ],
                    )
                        .jsonObject
                val name =
                    requireNotNull(
                        metadata[
                            "name"
                        ],
                    )
                        .jsonPrimitive
                        .content
                val version =
                    requireNotNull(
                        metadata[
                            "version"
                        ],
                    )
                        .jsonPrimitive
                        .content
                val sha256 =
                    requireNotNull(
                        requireNotNull(
                            requireNotNull(
                                requireNotNull(
                                    value[
                                        "download_info"
                                    ],
                                )
                                    .jsonObject[
                                        "archive_info"
                                    ],
                            )
                                .jsonObject[
                                    "hashes"
                                ],
                        )
                            .jsonObject[
                                "sha256"
                            ],
                    )
                        .jsonPrimitive
                        .content

                normalizePackageName(
                    name,
                ) to
                    (
                        version to
                            sha256
                    )
            }

        require(
            actual.size ==
                install.size
        ) {
            "BGE-M3 pip report contains duplicate normalized package names"
        }

        val expected =
            RequirementsText
                .lineSequence()
                .filter(
                    String::isNotBlank,
                )
                .associate {
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
                    val hash =
                        line.substringAfter(
                            " --hash=sha256:",
                        )

                    normalizePackageName(
                        name,
                    ) to
                        (
                            version to
                                hash
                        )
                }

        require(
            actual ==
                expected
        ) {
            "BGE-M3 pip installation report differs from the exact qualified package set"
        }
    }

    private fun normalizePackageName(
        value: String,
    ): String =
        value
            .trim()
            .lowercase()
            .replace(
                '_',
                '-',
            )
            .replace(
                '.',
                '-',
            )

    private fun requireCommandPass(
        result: CommandResult,
        label: String,
    ): String {
        require(
            result.status ==
                0 &&
                result.errorMessage ==
                null &&
                !result.timedOut
        ) {
            "$label failed: " +
                (
                    result.errorMessage
                        ?: result.stderr.take(
                            4_000,
                        )
                )
        }

        return result.stdout
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
                !attributes.isSymbolicLink
        ) {
            "$label must be one regular non-symlink file"
        }
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
                !attributes.isSymbolicLink
        ) {
            "$label must be one real non-symlink directory"
        }
    }

    private fun removeRealDirectoryTree(
        root: Path,
    ) {
        requireRealDirectory(
            root,
            "BGE-M3 environment root",
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

    private fun sha256(
        bytes: ByteArray,
    ): String =
        MessageDigest
            .getInstance(
                "SHA-256",
            )
            .digest(
                bytes,
            )
            .joinToString(
                "",
            ) {
                "%02x".format(
                    it.toInt() and
                        0xff,
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
                    start =
                        true,
                    name =
                        "vexlife-bge-environment-stdout",
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
                    start =
                        true,
                    name =
                        "vexlife-bge-environment-stderr",
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
                stdout =
                    stdout,
                stderr =
                    stderr,
                timedOut =
                    !completed,
            )
        } catch (
            error: Exception,
        ) {
            CommandResult(
                status =
                    null,
                stdout =
                    "",
                stderr =
                    "",
                errorMessage =
                    error.message
                        ?: error::class.simpleName
                        ?: "command failed",
            )
        }
}

// [VXG RealForever]
