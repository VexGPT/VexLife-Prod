package vexlife.capability

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.StringWriter
import java.net.URI
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.concurrent.TimeUnit
import javax.tools.Diagnostic
import javax.tools.DiagnosticCollector
import javax.tools.JavaFileObject
import javax.tools.ToolProvider
import kotlin.concurrent.thread
import vexlife.contract.VexContractJson
import vexlife.filesystem.VexOwnedPath

/**
 * Product-owned runner used only inside [VexLifeWorkspaceJavaTest]'s outer macOS sandbox.
 *
 * It compiles already-captured workspace source copies with annotation processing and implicit
 * source discovery disabled, then launches exactly one compiled test main under a Java 21 security
 * policy. The outer owner remains responsible for Run authority, source capture, sandbox formation
 * and scratch cleanup.
 */
internal object VexLifeWorkspaceJavaTestProcess {
    private data class Captured(
        val text: String,
        val truncated: Boolean,
    )

    private data class ChildResult(
        val started: Boolean,
        val exitCode: Int?,
        val stdout: String,
        val stderr: String,
        val timedOut: Boolean,
        val stdoutTruncated: Boolean,
        val stderrTruncated: Boolean,
    )

    fun run(
        scratch: Path,
        request: VexLifeWorkspaceJavaTest.RunnerRequest,
    ): VexLifeWorkspaceJavaTest.RunnerResult {
        val root =
            VexOwnedPath.requireRoot(
                scratch,
                "workspace Java-test runner scratch",
            )
        val sourceRoot =
            VexOwnedPath.resolveInside(
                root,
                "src",
            )
        val classes =
            VexOwnedPath.resolveInside(
                root,
                "classes",
            )
        val emptyClasspath =
            VexOwnedPath.resolveInside(
                root,
                "empty-classpath",
            )
        val temporary =
            VexOwnedPath.resolveInside(
                root,
                "tmp",
            )
        val home =
            VexOwnedPath.resolveInside(
                root,
                "home",
            )
        val policy =
            VexOwnedPath.resolveInside(
                root,
                VexLifeWorkspaceJavaTest.SecurityPolicyFileName,
            )

        if (
            !listOf(
                sourceRoot,
                classes,
                emptyClasspath,
                temporary,
                home,
            )
                .all {
                    Files.isDirectory(
                        it,
                        LinkOption.NOFOLLOW_LINKS,
                    )
                } ||
            !Files.isRegularFile(
                policy,
                LinkOption.NOFOLLOW_LINKS,
            )
        ) {
            return hostFailed()
        }

        val sourcePaths =
            request.sources.map {
                observation ->
                val path =
                    VexOwnedPath.resolveInside(
                        sourceRoot,
                        observation.path,
                    )
                val bytes =
                    VexLifeWorkspaceJavaTest.readNoFollow(
                        path,
                        VexLifeWorkspaceJavaTestDefinitions.MaximumSourceBytes,
                    )
                        ?: return VexLifeWorkspaceJavaTest.RunnerResult(
                            disposition =
                                VexLifeWorkspaceJavaTest.DispositionSourceChanged,
                        )
                if (
                    bytes.size !=
                        observation.bytes ||
                    VexLifeWorkspaceJavaTest.sha256(
                        bytes,
                    ) !=
                    observation.sha256
                ) {
                    return VexLifeWorkspaceJavaTest.RunnerResult(
                        disposition =
                            VexLifeWorkspaceJavaTest.DispositionSourceChanged,
                    )
                }
                path
            }

        val compiler =
            ToolProvider.getSystemJavaCompiler()
                ?: return hostFailed()
        val diagnostics =
            DiagnosticCollector<JavaFileObject>()
        val compilerOutput =
            StringWriter()
        val compiled =
            runCatching {
                    compiler
                        .getStandardFileManager(
                            diagnostics,
                            null,
                            StandardCharsets.UTF_8,
                        )
                        .use {
                            fileManager ->
                            val units =
                                fileManager.getJavaFileObjectsFromPaths(
                                    sourcePaths,
                                )
                            compiler
                                .getTask(
                                    compilerOutput,
                                    fileManager,
                                    diagnostics,
                                    listOf(
                                        "--release",
                                        "21",
                                        "-proc:none",
                                        "-implicit:none",
                                        "-encoding",
                                        "UTF-8",
                                        "-g:none",
                                        "-classpath",
                                        emptyClasspath.toString(),
                                        "-sourcepath",
                                        emptyClasspath.toString(),
                                        "-d",
                                        classes.toString(),
                                    ),
                                    null,
                                    units,
                                )
                                .call()
                        }
                }
                .getOrElse {
                    return hostFailed(
                        diagnostics =
                            diagnostics(
                                diagnostics,
                                sourceRoot,
                                compilerOutput.toString(),
                            ),
                    )
                }

        val compileDiagnostics =
            diagnostics(
                diagnostics,
                sourceRoot,
                compilerOutput.toString(),
            )
        if (
            !compiled
        ) {
            return VexLifeWorkspaceJavaTest.RunnerResult(
                disposition =
                    VexLifeWorkspaceJavaTest.DispositionCompileFailed,
                diagnostics =
                    compileDiagnostics,
            )
        }

        val java =
            Path.of(
                System.getProperty(
                    "java.home",
                ),
                "bin",
                "java",
            )
                .toAbsolutePath()
                .normalize()
        if (
            !Files.isExecutable(
                java,
            )
        ) {
            return hostFailed(
                diagnostics =
                    compileDiagnostics,
            )
        }

        val child =
            runChild(
                program =
                    java,
                arguments =
                    listOf(
                        "-Dfile.encoding=UTF-8",
                        "-Djava.security.manager=default",
                        "-Djava.security.policy==" +
                            policy
                                .toUri()
                                .toASCIIString(),
                        "-Djava.io.tmpdir=" +
                            temporary,
                        "-Duser.home=" +
                            home,
                        "-XX:-UsePerfData",
                        "-Xmx64m",
                        "-XX:ActiveProcessorCount=1",
                        "-ea",
                        "-cp",
                        classes.toString(),
                        request.testClass,
                    ),
                workingDirectory =
                    root,
                timeoutMs =
                    VexLifeWorkspaceJavaTestDefinitions.ChildTimeoutMs,
            )
        if (
            !child.started
        ) {
            return hostFailed(
                diagnostics =
                    compileDiagnostics,
            )
        }
        if (
            child.timedOut
        ) {
            return VexLifeWorkspaceJavaTest.RunnerResult(
                disposition =
                    VexLifeWorkspaceJavaTest.DispositionTimeout,
                diagnostics =
                    compileDiagnostics,
                testTimedOut =
                    true,
                testStdout =
                    child.stdout,
                testStderr =
                    child.stderr,
                stdoutTruncated =
                    child.stdoutTruncated,
                stderrTruncated =
                    child.stderrTruncated,
            )
        }

        val passed =
            child.exitCode ==
                0 &&
                !child.stdoutTruncated &&
                !child.stderrTruncated
        return VexLifeWorkspaceJavaTest.RunnerResult(
            disposition =
                if (
                    passed
                ) {
                    VexLifeWorkspaceJavaTest.DispositionTestPassed
                } else {
                    VexLifeWorkspaceJavaTest.DispositionTestFailed
                },
            diagnostics =
                compileDiagnostics,
            testExitCode =
                child.exitCode,
            testStdout =
                child.stdout,
            testStderr =
                child.stderr,
            stdoutTruncated =
                child.stdoutTruncated,
            stderrTruncated =
                child.stderrTruncated,
        )
    }

    private fun diagnostics(
        collector: DiagnosticCollector<JavaFileObject>,
        sourceRoot: Path,
        compilerOutput: String,
    ): List<VexLifeWorkspaceJavaTest.CompilerDiagnostic> {
        val values =
            collector
                .diagnostics
                .take(
                    VexLifeWorkspaceJavaTestDefinitions.MaximumDiagnostics,
                )
                .map {
                    diagnostic ->
                    VexLifeWorkspaceJavaTest.CompilerDiagnostic(
                        kind =
                            diagnostic.kind.name,
                        code =
                            diagnostic.code
                                .orEmpty()
                                .take(
                                    256,
                                ),
                        sourcePath =
                            diagnostic
                                .source
                                ?.toUri()
                                ?.let {
                                    sourcePath(
                                        sourceRoot,
                                        it,
                                    )
                                },
                        line =
                            diagnostic.lineNumber,
                        column =
                            diagnostic.columnNumber,
                        message =
                            diagnostic
                                .getMessage(
                                    java.util.Locale.ROOT,
                                )
                                .take(
                                    VexLifeWorkspaceJavaTestDefinitions.MaximumDiagnosticCharacters,
                                ),
                    )
                }
                .toMutableList()
        if (
            compilerOutput.isNotBlank() &&
            values.size <
            VexLifeWorkspaceJavaTestDefinitions.MaximumDiagnostics
        ) {
            values +=
                VexLifeWorkspaceJavaTest.CompilerDiagnostic(
                    kind =
                        Diagnostic.Kind.OTHER.name,
                    code =
                        "COMPILER_OUTPUT",
                    sourcePath =
                        null,
                    line =
                        Diagnostic.NOPOS,
                    column =
                        Diagnostic.NOPOS,
                    message =
                        compilerOutput
                            .trim()
                            .take(
                                VexLifeWorkspaceJavaTestDefinitions.MaximumDiagnosticCharacters,
                            ),
                )
        }
        return values
    }

    private fun sourcePath(
        sourceRoot: Path,
        uri: URI,
    ): String? =
        runCatching {
                val absolute =
                    Path.of(
                        uri,
                    )
                        .toAbsolutePath()
                        .normalize()
                val relative =
                    sourceRoot
                        .toAbsolutePath()
                        .normalize()
                        .relativize(
                            absolute,
                        )
                        .toString()
                        .replace(
                            java.io.File.separatorChar,
                            '/',
                        )
                relative.takeIf(
                    VexLifeWorkspaceJavaTest::validJavaPath,
                )
            }
            .getOrNull()

    private fun runChild(
        program: Path,
        arguments: List<String>,
        workingDirectory: Path,
        timeoutMs: Long,
    ): ChildResult =
        try {
            val builder =
                ProcessBuilder(
                    listOf(
                        program.toString(),
                    ) +
                        arguments,
                )
                    .directory(
                        workingDirectory
                            .toFile(),
                    )
            builder
                .environment()
                .clear()

            val process =
                builder.start()
            var stdout =
                Captured(
                    "",
                    false,
                )
            var stderr =
                Captured(
                    "",
                    false,
                )
            val stdoutThread =
                thread(
                    start =
                        true,
                    name =
                        "vexlife-workspace-java-test-child-stdout",
                ) {
                    stdout =
                        capture(
                            process.inputStream,
                            VexLifeWorkspaceJavaTestDefinitions.MaximumStdoutBytes,
                        )
                }
            val stderrThread =
                thread(
                    start =
                        true,
                    name =
                        "vexlife-workspace-java-test-child-stderr",
                ) {
                    stderr =
                        capture(
                            process.errorStream,
                            VexLifeWorkspaceJavaTestDefinitions.MaximumStderrBytes,
                        )
                }

            val completed =
                process.waitFor(
                    timeoutMs,
                    TimeUnit.MILLISECONDS,
                )
            if (
                !completed
            ) {
                process
                    .toHandle()
                    .descendants()
                    .forEach {
                        it.destroyForcibly()
                    }
                process.destroyForcibly()
                process.waitFor()
            }

            stdoutThread.join()
            stderrThread.join()

            ChildResult(
                started =
                    true,
                exitCode =
                    if (
                        completed
                    ) {
                        process.exitValue()
                    } else {
                        null
                    },
                stdout =
                    stdout.text,
                stderr =
                    stderr.text,
                timedOut =
                    !completed,
                stdoutTruncated =
                    stdout.truncated,
                stderrTruncated =
                    stderr.truncated,
            )
        } catch (
            _: Exception,
        ) {
            ChildResult(
                started =
                    false,
                exitCode =
                    null,
                stdout =
                    "",
                stderr =
                    "",
                timedOut =
                    false,
                stdoutTruncated =
                    false,
                stderrTruncated =
                    false,
            )
        }

    private fun capture(
        input: InputStream,
        maximumBytes: Int,
    ): Captured {
        val output =
            ByteArrayOutputStream(
                minOf(
                    maximumBytes,
                    8_192,
                ),
            )
        val buffer =
            ByteArray(
                4_096,
            )
        var truncated =
            false
        while (
            true
        ) {
            val read =
                input.read(
                    buffer,
                )
            if (
                read <
                0
            ) {
                break
            }
            val room =
                maximumBytes -
                    output.size()
            if (
                room >
                    0
            ) {
                output.write(
                    buffer,
                    0,
                    minOf(
                        room,
                        read,
                    ),
                )
            }
            if (
                read >
                    room
            ) {
                truncated =
                    true
            }
        }
        return Captured(
            text =
                String(
                    output.toByteArray(),
                    StandardCharsets.UTF_8,
                ),
            truncated =
                truncated,
        )
    }

    private fun hostFailed(
        diagnostics: List<VexLifeWorkspaceJavaTest.CompilerDiagnostic> =
            emptyList(),
    ): VexLifeWorkspaceJavaTest.RunnerResult =
        VexLifeWorkspaceJavaTest.RunnerResult(
            disposition =
                VexLifeWorkspaceJavaTest.DispositionHostFailed,
            diagnostics =
                diagnostics,
        )
}

fun main(args: Array<String>) {
    val result =
        runCatching {
                require(
                    args.size ==
                        1,
                ) {
                    "workspace Java-test runner expects one canonical request"
                }
                val request =
                    VexContractJson.decode<VexLifeWorkspaceJavaTest.RunnerRequest>(
                        args.single(),
                    )
                VexLifeWorkspaceJavaTestProcess.run(
                    scratch =
                        Path.of(
                            "",
                        )
                            .toAbsolutePath()
                            .normalize(),
                    request =
                        request,
                )
            }
            .getOrElse {
                VexLifeWorkspaceJavaTest.RunnerResult(
                    disposition =
                        VexLifeWorkspaceJavaTest.DispositionHostFailed,
                )
            }

    print(
        VexContractJson.canonicalEncode(
            result,
        ),
    )
}

// [VXG RealForever]
