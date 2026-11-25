package vexlife.capability

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.ByteBuffer
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
import kotlinx.serialization.Serializable
import vexlife.contract.VexContractJson
import vexlife.filesystem.VexOwnedPath
import vexlife.packaging.VexLifeProdPrivateServiceImage
import vexlife.sources.VexLifeLocalSourceStore
import vexlife.workspace.VexLifeWorkspaceRunStore

/**
 * Effect owner for one typed Java project-test request.
 *
 * Workspace-authored code never runs in the Product service process. Product source captures exact
 * admitted .java bytes, copies only those bytes into deterministic scratch under the granted
 * workspace, and starts the source-bound Product Java image through a Product-owned macOS sandbox
 * profile. The sandboxed Product runner compiles those copies with annotation processing disabled,
 * then runs the selected test main with an additional Java 21 SecurityManager policy.
 *
 * Permanent:
 *
 * ```text
 * WORKSPACE RUN AUTHORITY != WORKSPACE WRITE AUTHORITY
 * WORKSPACE RUN AUTHORITY != PRODUCT-OWNED CHECK AUTHORITY
 * REGISTERED JAVA TEST != GENERIC SHELL
 * PROJECT TEST CODE != PRODUCT SERVICE PROCESS
 * PROJECT TEST CODE != WORKSPACE SOURCE MUTATION
 * ```
 */
class VexLifeWorkspaceJavaTest internal constructor(
    productHome: Path,
    private val localSources: VexLifeLocalSourceStore =
        VexLifeLocalSourceStore(
            productHome,
        ),
    private val workspaceRuns: VexLifeWorkspaceRunStore =
        VexLifeWorkspaceRunStore(
            productHome =
                productHome,
            localSources =
                localSources,
        ),
    private val runtimeSource: RuntimeSource,
    private val runner: Runner =
        SystemRunner,
) {
    @Serializable
    data class Request(
        val schemaVersion: String =
            RequestSchema,
        val commandRef: String =
            VexLifeWorkspaceJavaTestDefinitions.CommandRef,
        val sources: List<String>,
        val testClass: String,
    ) {
        init {
            require(
                schemaVersion ==
                    RequestSchema &&
                    commandRef ==
                    VexLifeWorkspaceJavaTestDefinitions.CommandRef &&
                    sources.isNotEmpty() &&
                    sources.size <=
                    VexLifeWorkspaceJavaTestDefinitions.MaximumSources &&
                    sources ==
                    sources.distinct() &&
                    sources.all(
                        ::validJavaPath,
                    ) &&
                    testClass.length <=
                    VexLifeWorkspaceJavaTestDefinitions.MaximumTestClassCharacters &&
                    TestClass.matches(
                        testClass,
                    ),
            ) {
                "workspace Java-test request is invalid"
            }
        }
    }

    @Serializable
    data class SourceObservation(
        val path: String,
        val sha256: String,
        val bytes: Int,
    ) {
        init {
            require(
                validJavaPath(
                    path,
                ) &&
                    Sha256.matches(
                        sha256,
                    ) &&
                    bytes >=
                    0,
            ) {
                "workspace Java-test source observation is invalid"
            }
        }
    }

    @Serializable
    data class CompilerDiagnostic(
        val kind: String,
        val code: String,
        val sourcePath: String?,
        val line: Long,
        val column: Long,
        val message: String,
    )

    @Serializable
    data class RunnerRequest(
        val schemaVersion: String =
            RunnerRequestSchema,
        val commandRef: String =
            VexLifeWorkspaceJavaTestDefinitions.CommandRef,
        val sources: List<SourceObservation>,
        val testClass: String,
    ) {
        init {
            require(
                schemaVersion ==
                    RunnerRequestSchema &&
                    commandRef ==
                    VexLifeWorkspaceJavaTestDefinitions.CommandRef &&
                    sources.isNotEmpty() &&
                    sources.size <=
                    VexLifeWorkspaceJavaTestDefinitions.MaximumSources &&
                    sources.map(
                        SourceObservation::path,
                    ) ==
                    sources.map(
                        SourceObservation::path,
                    )
                        .distinct() &&
                    TestClass.matches(
                        testClass,
                    ),
            ) {
                "workspace Java-test runner request is invalid"
            }
        }
    }

    @Serializable
    data class RunnerResult(
        val schemaVersion: String =
            RunnerResultSchema,
        val commandRef: String =
            VexLifeWorkspaceJavaTestDefinitions.CommandRef,
        val disposition: String,
        val diagnostics: List<CompilerDiagnostic> =
            emptyList(),
        val testExitCode: Int? =
            null,
        val testTimedOut: Boolean =
            false,
        val testStdout: String =
            "",
        val testStderr: String =
            "",
        val stdoutTruncated: Boolean =
            false,
        val stderrTruncated: Boolean =
            false,
    ) {
        init {
            require(
                schemaVersion ==
                    RunnerResultSchema &&
                    commandRef ==
                    VexLifeWorkspaceJavaTestDefinitions.CommandRef &&
                    disposition in
                    RunnerDispositions &&
                    diagnostics.size <=
                    VexLifeWorkspaceJavaTestDefinitions.MaximumDiagnostics &&
                    diagnostics.all {
                        it.message.length <=
                            VexLifeWorkspaceJavaTestDefinitions.MaximumDiagnosticCharacters
                    } &&
                    (
                        disposition ==
                            DispositionTimeout
                    ) ==
                    testTimedOut,
            ) {
                "workspace Java-test runner result is invalid"
            }
        }
    }

    data class RuntimeObservation(
        val runtimeRef: String,
        val imageRoot: String,
        val javaPath: String,
        val classPath: String,
        val sandboxPath: String,
        val sandboxSha256: String,
    ) {
        init {
            require(
                runtimeRef.startsWith(
                    RuntimeRefPrefix,
                ) &&
                    Path.of(
                        imageRoot,
                    )
                        .isAbsolute &&
                    Path.of(
                        javaPath,
                    )
                        .isAbsolute &&
                    classPath.isNotBlank() &&
                    Path.of(
                        sandboxPath,
                    )
                        .isAbsolute &&
                    Sha256.matches(
                        sandboxSha256,
                    ),
            ) {
                "workspace Java-test runtime observation is invalid"
            }
        }
    }

    fun interface RuntimeSource {
        fun current(): RuntimeObservation?
    }

    class PrivateServiceImageRuntimeSource(
        private val imageRoot: Path,
        private val expectedSourceSha: String,
        private val sandboxPath: Path =
            Path.of(
                "/usr/bin/sandbox-exec",
            ),
    ) : RuntimeSource {
        override fun current(): RuntimeObservation? {
            val observation =
                VexLifeProdPrivateServiceImage.observe(
                    imageRoot =
                        imageRoot,
                    expectedSourceSha =
                        expectedSourceSha,
                )
            if (
                RequiredCompilerModule !in
                    observation.runtimeModules
            ) {
                return null
            }

            val sandbox =
                sandboxPath
                    .toAbsolutePath()
                    .normalize()
            val sandboxAttributes =
                runCatching {
                        Files.readAttributes(
                            sandbox,
                            BasicFileAttributes::class.java,
                            LinkOption.NOFOLLOW_LINKS,
                        )
                    }
                    .getOrNull()
                    ?: return null
            if (
                !sandboxAttributes.isRegularFile ||
                sandboxAttributes.isSymbolicLink ||
                !Files.isExecutable(
                    sandbox,
                )
            ) {
                return null
            }

            val sandboxSha =
                sha256(
                    readNoFollow(
                        sandbox,
                        MaximumSandboxExecutableBytes,
                    )
                        ?: return null,
                )
            val image =
                imageRoot
                    .toAbsolutePath()
                    .normalize()
            val runtimeRef =
                RuntimeRefPrefix +
                    sha256(
                        listOf(
                            observation.imageRef,
                            sandbox.toString(),
                            sandboxSha,
                        )
                            .joinToString(
                                "\u0000",
                            )
                            .toByteArray(
                                StandardCharsets.UTF_8,
                            ),
                    )

            return RuntimeObservation(
                runtimeRef =
                    runtimeRef,
                imageRoot =
                    image.toString(),
                javaPath =
                    observation.javaPath,
                classPath =
                    image
                        .resolve(
                            "lib/*",
                        )
                        .toString(),
                sandboxPath =
                    sandbox.toString(),
                sandboxSha256 =
                    sandboxSha,
            )
        }
    }

    data class Plan(
        val schemaVersion: String,
        val processRef: String,
        val commandRef: String,
        val operationRef: String,
        val workingScopeRef: String,
        val resourceLeaseRef: String,
        val sourceGrantedAt: String,
        val runGrantRef: String,
        val runtimeRef: String,
        val imageRoot: String,
        val javaPath: String,
        val classPath: String,
        val sandboxPath: String,
        val sandboxSha256: String,
        val workingDirectory: String,
        val request: Request,
        val timeoutMs: Long,
        val environment: Map<String, String>,
        val externalEffectsExecuted: Boolean,
    ) {
        init {
            require(
                schemaVersion ==
                    PlanSchema &&
                    processRef ==
                    VexLifeWorkspaceJavaTestDefinitions.ProcessRef &&
                    commandRef ==
                    VexLifeWorkspaceJavaTestDefinitions.CommandRef &&
                    operationRef.isNotBlank() &&
                    operationRef.length <=
                    MaximumOperationRefCharacters &&
                    workingScopeRef.isNotBlank() &&
                    resourceLeaseRef.startsWith(
                        ResourceLeaseRefPrefix,
                    ) &&
                    sourceGrantedAt.isNotBlank() &&
                    runGrantRef.isNotBlank() &&
                    runtimeRef.startsWith(
                        RuntimeRefPrefix,
                    ) &&
                    Path.of(
                        imageRoot,
                    )
                        .isAbsolute &&
                    Path.of(
                        javaPath,
                    )
                        .isAbsolute &&
                    Path.of(
                        sandboxPath,
                    )
                        .isAbsolute &&
                    Sha256.matches(
                        sandboxSha256,
                    ) &&
                    Path.of(
                        workingDirectory,
                    )
                        .isAbsolute &&
                    timeoutMs ==
                    VexLifeWorkspaceJavaTestDefinitions.TimeoutMs &&
                    environment.isEmpty() &&
                    !externalEffectsExecuted,
            ) {
                "workspace Java-test plan is invalid"
            }
        }
    }

    sealed interface PlanOutcome {
        data class Ready(
            val plan: Plan,
        ) : PlanOutcome

        data class Refused(
            val reason: String,
        ) : PlanOutcome
    }

    @Serializable
    data class Receipt(
        val schemaVersion: String,
        val processRef: String,
        val commandRef: String,
        val operationRef: String,
        val workingScopeRef: String,
        val resourceLeaseRef: String,
        val runGrantRef: String,
        val runtimeRef: String,
        val sandboxSha256: String,
        val sources: List<SourceObservation>,
        val testClass: String,
        val disposition: String,
        val diagnostics: List<CompilerDiagnostic>,
        val testExitCode: Int?,
        val testTimedOut: Boolean,
        val testStdout: String,
        val testStderr: String,
        val stdoutTruncated: Boolean,
        val stderrTruncated: Boolean,
        val scratchResidualRecovered: Boolean,
        val cleanupComplete: Boolean,
        val externalEffectsExecuted: Boolean,
        val effectAuthorityGranted: Boolean,
        val currentEffectAuthority: Boolean,
    ) {
        init {
            require(
                schemaVersion ==
                    ReceiptSchema &&
                    processRef ==
                    VexLifeWorkspaceJavaTestDefinitions.ProcessRef &&
                    commandRef ==
                    VexLifeWorkspaceJavaTestDefinitions.CommandRef &&
                    disposition in
                    ReceiptDispositions &&
                    Sha256.matches(
                        sandboxSha256,
                    ) &&
                    sources.isNotEmpty() &&
                    externalEffectsExecuted &&
                    effectAuthorityGranted &&
                    (
                        disposition ==
                            DispositionTimeout
                    ) ==
                    testTimedOut,
            ) {
                "workspace Java-test receipt is invalid"
            }
        }
    }

    sealed interface Outcome {
        data class Completed(
            val receipt: Receipt,
        ) : Outcome

        data class Refused(
            val reason: String,
        ) : Outcome
    }

    internal data class ProcessSpec(
        val program: String,
        val arguments: List<String>,
        val workingDirectory: Path,
        val environment: Map<String, String>,
        val timeoutMs: Long,
        val maximumStdoutBytes: Int,
        val maximumStderrBytes: Int,
    )

    internal data class ProcessResult(
        val started: Boolean,
        val exitCode: Int?,
        val stdout: String,
        val stderr: String,
        val timedOut: Boolean,
        val stdoutTruncated: Boolean,
        val stderrTruncated: Boolean,
    )

    internal fun interface Runner {
        fun run(
            spec: ProcessSpec,
        ): ProcessResult
    }

    internal object SystemRunner : Runner {
        override fun run(
            spec: ProcessSpec,
        ): ProcessResult =
            try {
                val builder =
                    ProcessBuilder(
                        listOf(
                            spec.program,
                        ) +
                            spec.arguments,
                    )
                        .directory(
                            spec.workingDirectory.toFile(),
                        )
                builder
                    .environment()
                    .clear()
                builder
                    .environment()
                    .putAll(
                        spec.environment,
                    )

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
                            "vexlife-workspace-java-test-stdout",
                    ) {
                        stdout =
                            capture(
                                process.inputStream,
                                spec.maximumStdoutBytes,
                            )
                    }
                val stderrThread =
                    thread(
                        start =
                            true,
                        name =
                            "vexlife-workspace-java-test-stderr",
                    ) {
                        stderr =
                            capture(
                                process.errorStream,
                                spec.maximumStderrBytes,
                            )
                    }

                val completed =
                    process.waitFor(
                        spec.timeoutMs,
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

                ProcessResult(
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
                ProcessResult(
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

        private data class Captured(
            val text: String,
            val truncated: Boolean,
        )

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
    }

    fun plan(
        operationRef: String,
        sourceRef: String,
        request: Request,
    ): PlanOutcome {
        if (
            operationRef.isBlank() ||
            operationRef.length >
                MaximumOperationRefCharacters
        ) {
            return PlanOutcome.Refused(
                RefusedOperationRef,
            )
        }

        VexLifeWorkspaceJavaTestDefinitions.requireCommand(
            request.commandRef,
        )

        val grant =
            workspaceRuns.currentGrant(
                sourceRef,
            )
                ?: return PlanOutcome.Refused(
                    RefusedGrantNotCurrent,
                )
        val root =
            localSources.root(
                sourceRef,
            )
                ?: return PlanOutcome.Refused(
                    RefusedSourceNotCurrent,
                )
        val runtime =
            runCatching {
                    runtimeSource.current()
                }
                .getOrNull()
                ?: return PlanOutcome.Refused(
                    RefusedRuntimeNotCurrent,
                )

        return PlanOutcome.Ready(
            Plan(
                schemaVersion =
                    PlanSchema,
                processRef =
                    VexLifeWorkspaceJavaTestDefinitions.ProcessRef,
                commandRef =
                    request.commandRef,
                operationRef =
                    operationRef,
                workingScopeRef =
                    sourceRef,
                resourceLeaseRef =
                    ResourceLeaseRefPrefix +
                        sha256(
                            listOf(
                                grant.grantRef,
                                grant.sourceGrantedAt,
                                runtime.runtimeRef,
                            )
                                .joinToString(
                                    "\u0000",
                                )
                                .toByteArray(
                                    StandardCharsets.UTF_8,
                                ),
                        ),
                sourceGrantedAt =
                    grant.sourceGrantedAt,
                runGrantRef =
                    grant.grantRef,
                runtimeRef =
                    runtime.runtimeRef,
                imageRoot =
                    runtime.imageRoot,
                javaPath =
                    runtime.javaPath,
                classPath =
                    runtime.classPath,
                sandboxPath =
                    runtime.sandboxPath,
                sandboxSha256 =
                    runtime.sandboxSha256,
                workingDirectory =
                    root.toString(),
                request =
                    request,
                timeoutMs =
                    VexLifeWorkspaceJavaTestDefinitions.TimeoutMs,
                environment =
                    emptyMap(),
                externalEffectsExecuted =
                    false,
            ),
        )
    }

    fun execute(
        plan: Plan,
    ): Outcome {
        val grant =
            workspaceRuns.currentGrant(
                plan.workingScopeRef,
            )
                ?: return Outcome.Refused(
                    RefusedGrantNotCurrent,
                )
        if (
            grant.grantRef !=
                plan.runGrantRef ||
            grant.sourceGrantedAt !=
                plan.sourceGrantedAt
        ) {
            return Outcome.Refused(
                RefusedGrantGenerationChanged,
            )
        }

        val root =
            localSources.root(
                plan.workingScopeRef,
            )
                ?: return Outcome.Refused(
                    RefusedSourceNotCurrent,
                )
        if (
            root.toString() !=
                plan.workingDirectory
        ) {
            return Outcome.Refused(
                RefusedWorkingScopeChanged,
            )
        }

        val runtime =
            runCatching {
                    runtimeSource.current()
                }
                .getOrNull()
                ?: return Outcome.Refused(
                    RefusedRuntimeNotCurrent,
                )
        if (
            runtime.runtimeRef !=
                plan.runtimeRef ||
            runtime.imageRoot !=
                plan.imageRoot ||
            runtime.javaPath !=
                plan.javaPath ||
            runtime.classPath !=
                plan.classPath ||
            runtime.sandboxPath !=
                plan.sandboxPath ||
            runtime.sandboxSha256 !=
                plan.sandboxSha256
        ) {
            return Outcome.Refused(
                RefusedRuntimeChanged,
            )
        }

        val captured =
            captureSources(
                root =
                    root,
                request =
                    plan.request,
            )
                ?: return Outcome.Refused(
                    RefusedSourceCapture,
                )

        val scratch =
            prepareScratch(
                root =
                    root,
                operationRef =
                    plan.operationRef,
                runGrantRef =
                    plan.runGrantRef,
                runtimeRef =
                    plan.runtimeRef,
            )
                ?: return Outcome.Refused(
                    RefusedScratchNotReady,
                )

        var cleanupComplete =
            false
        try {
            val sourceRoot =
                scratch.path.resolve(
                    "src",
                )
            val classes =
                scratch.path.resolve(
                    "classes",
                )
            val emptyClasspath =
                scratch.path.resolve(
                    "empty-classpath",
                )
            val temporary =
                scratch.path.resolve(
                    "tmp",
                )
            val home =
                scratch.path.resolve(
                    "home",
                )
            listOf(
                sourceRoot,
                classes,
                emptyClasspath,
                temporary,
                home,
            )
                .forEach(
                    Files::createDirectories,
                )

            captured.forEach {
                source ->
                val target =
                    VexOwnedPath.resolveInside(
                        sourceRoot,
                        source.observation.path,
                    )
                Files.createDirectories(
                    target.parent,
                )
                Files.write(
                    target,
                    source.bytes,
                    StandardOpenOption.CREATE_NEW,
                    StandardOpenOption.WRITE,
                )
            }

            Files.writeString(
                scratch.path.resolve(
                    SecurityPolicyFileName,
                ),
                securityPolicy(
                    scratch.path,
                ),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW,
            )

            val runnerRequest =
                RunnerRequest(
                    sources =
                        captured.map {
                            it.observation
                        },
                    testClass =
                        plan.request.testClass,
                )
            val profile =
                sandboxProfile(
                    imageRoot =
                        Path.of(
                            plan.imageRoot,
                        ),
                    scratch =
                        scratch.path,
                    javaPath =
                        Path.of(
                            plan.javaPath,
                        ),
                )
            val process =
                runner.run(
                    ProcessSpec(
                        program =
                            plan.sandboxPath,
                        arguments =
                            listOf(
                                "-p",
                                profile,
                                plan.javaPath,
                                "-Dfile.encoding=UTF-8",
                                "-Djava.io.tmpdir=" +
                                    temporary,
                                "-Duser.home=" +
                                    home,
                                "-XX:-UsePerfData",
                                "-Xmx192m",
                                "-XX:ActiveProcessorCount=2",
                                "-cp",
                                plan.classPath,
                                VexLifeWorkspaceJavaTestDefinitions.ProcessMainClass,
                                VexContractJson.canonicalEncode(
                                    runnerRequest,
                                ),
                            ),
                        workingDirectory =
                            scratch.path,
                        environment =
                            plan.environment,
                        timeoutMs =
                            plan.timeoutMs,
                        maximumStdoutBytes =
                            MaximumProtocolStdoutBytes,
                        maximumStderrBytes =
                            MaximumProtocolStderrBytes,
                    ),
                )

            val currentAuthority =
                workspaceRuns
                    .currentGrant(
                        plan.workingScopeRef,
                    )
                    ?.let {
                        it.grantRef ==
                            plan.runGrantRef &&
                            it.sourceGrantedAt ==
                            plan.sourceGrantedAt
                    }
                    ?: false

            if (
                !process.started
            ) {
                return Outcome.Refused(
                    RefusedProcessNotStarted,
                )
            }

            val result =
                if (
                    process.timedOut
                ) {
                    RunnerResult(
                        disposition =
                            DispositionTimeout,
                        testTimedOut =
                            true,
                    )
                } else if (
                    process.exitCode !=
                        0 ||
                    process.stdoutTruncated
                ) {
                    RunnerResult(
                        disposition =
                            DispositionHostFailed,
                        testStderr =
                            process.stderr.take(
                                VexLifeWorkspaceJavaTestDefinitions.MaximumStderrBytes,
                            ),
                        stderrTruncated =
                            process.stderrTruncated,
                    )
                } else {
                    runCatching {
                            VexContractJson.decode<RunnerResult>(
                                process.stdout.trim(),
                            )
                        }
                        .getOrNull()
                        ?: RunnerResult(
                            disposition =
                                DispositionHostFailed,
                        )
                }

            cleanupComplete =
                cleanupScratch(
                    scratch.path,
                )
            val disposition =
                if (
                    cleanupComplete
                ) {
                    result.disposition
                } else {
                    DispositionHostFailed
                }

            return Outcome.Completed(
                Receipt(
                    schemaVersion =
                        ReceiptSchema,
                    processRef =
                        plan.processRef,
                    commandRef =
                        plan.commandRef,
                    operationRef =
                        plan.operationRef,
                    workingScopeRef =
                        plan.workingScopeRef,
                    resourceLeaseRef =
                        plan.resourceLeaseRef,
                    runGrantRef =
                        plan.runGrantRef,
                    runtimeRef =
                        plan.runtimeRef,
                    sandboxSha256 =
                        plan.sandboxSha256,
                    sources =
                        captured.map {
                            it.observation
                        },
                    testClass =
                        plan.request.testClass,
                    disposition =
                        disposition,
                    diagnostics =
                        result.diagnostics,
                    testExitCode =
                        result.testExitCode,
                    testTimedOut =
                        result.testTimedOut,
                    testStdout =
                        result.testStdout,
                    testStderr =
                        result.testStderr,
                    stdoutTruncated =
                        result.stdoutTruncated,
                    stderrTruncated =
                        result.stderrTruncated,
                    scratchResidualRecovered =
                        scratch.recoveredResidual,
                    cleanupComplete =
                        cleanupComplete,
                    externalEffectsExecuted =
                        true,
                    effectAuthorityGranted =
                        true,
                    currentEffectAuthority =
                        currentAuthority,
                ),
            )
        } finally {
            if (
                !cleanupComplete
            ) {
                cleanupComplete =
                    cleanupScratch(
                        scratch.path,
                    )
            }
            removeScratchParentIfEmpty(
                scratch.path.parent,
            )
        }
    }

    fun test(
        operationRef: String,
        sourceRef: String,
        request: Request,
    ): Outcome =
        when (
            val planned =
                plan(
                    operationRef =
                        operationRef,
                    sourceRef =
                        sourceRef,
                    request =
                        request,
                )
        ) {
            is PlanOutcome.Ready ->
                execute(
                    planned.plan,
                )

            is PlanOutcome.Refused ->
                Outcome.Refused(
                    planned.reason,
                )
        }

    private data class CapturedSource(
        val observation: SourceObservation,
        val bytes: ByteArray,
    )

    private data class Scratch(
        val path: Path,
        val recoveredResidual: Boolean,
    )

    private fun captureSources(
        root: Path,
        request: Request,
    ): List<CapturedSource>? {
        val ownedRoot =
            VexOwnedPath.requireRoot(
                root,
                "workspace Java-test root",
            )
        var total =
            0
        val sources =
            mutableListOf<CapturedSource>()
        request.sources.forEach {
            relative ->
            val target =
                VexOwnedPath.resolveInside(
                    ownedRoot,
                    relative,
                )
            val parentState =
                runCatching {
                        VexOwnedPath.observeParent(
                            root =
                                ownedRoot,
                            file =
                                target,
                        )
                    }
                    .getOrNull()
                    ?: return null
            if (
                parentState !=
                    VexOwnedPath.DirectoryState.Present
            ) {
                return null
            }
            val attributes =
                runCatching {
                        Files.readAttributes(
                            target,
                            BasicFileAttributes::class.java,
                            LinkOption.NOFOLLOW_LINKS,
                        )
                    }
                    .getOrNull()
                    ?: return null
            if (
                !attributes.isRegularFile ||
                attributes.isSymbolicLink ||
                attributes.size() >
                VexLifeWorkspaceJavaTestDefinitions.MaximumSourceBytes
            ) {
                return null
            }
            val bytes =
                readNoFollow(
                    target,
                    VexLifeWorkspaceJavaTestDefinitions.MaximumSourceBytes,
                )
                    ?: return null
            total +=
                bytes.size
            if (
                total >
                    VexLifeWorkspaceJavaTestDefinitions.MaximumSourceBytes
            ) {
                return null
            }
            sources +=
                CapturedSource(
                    observation =
                        SourceObservation(
                            path =
                                relative,
                            sha256 =
                                sha256(
                                    bytes,
                                ),
                            bytes =
                                bytes.size,
                        ),
                    bytes =
                        bytes,
                )
        }
        return sources
    }

    private fun prepareScratch(
        root: Path,
        operationRef: String,
        runGrantRef: String,
        runtimeRef: String,
    ): Scratch? =
        runCatching {
                val ownedRoot =
                    VexOwnedPath.requireRoot(
                        root,
                        "workspace Java-test scratch root",
                    )
                val parent =
                    VexOwnedPath.resolveInside(
                        ownedRoot,
                        VexLifeWorkspaceJavaTestDefinitions.ScratchDirectoryName,
                    )
                if (
                    Files.exists(
                        parent,
                        LinkOption.NOFOLLOW_LINKS,
                    )
                ) {
                    val attributes =
                        Files.readAttributes(
                            parent,
                            BasicFileAttributes::class.java,
                            LinkOption.NOFOLLOW_LINKS,
                        )
                    require(
                        attributes.isDirectory &&
                            !attributes.isSymbolicLink,
                    ) {
                        "workspace Java-test scratch parent is not an owned directory"
                    }
                } else {
                    Files.createDirectory(
                        parent,
                    )
                }

                val operationName =
                    sha256(
                        listOf(
                            operationRef,
                            runGrantRef,
                            runtimeRef,
                        )
                            .joinToString(
                                "\u0000",
                            )
                            .toByteArray(
                                StandardCharsets.UTF_8,
                            ),
                    )
                        .take(
                            24,
                        )
                val scratch =
                    VexOwnedPath.resolveInside(
                        parent,
                        operationName,
                    )
                val recovered =
                    Files.exists(
                        scratch,
                        LinkOption.NOFOLLOW_LINKS,
                    )
                if (
                    recovered
                ) {
                    require(
                        cleanupScratch(
                            scratch,
                        ),
                    ) {
                        "workspace Java-test residual scratch could not be recovered"
                    }
                }
                Files.createDirectory(
                    scratch,
                )
                Scratch(
                    path =
                        scratch,
                    recoveredResidual =
                        recovered,
                )
            }
            .getOrNull()

    companion object {
        const val RequestSchema =
            "vexlife-prod.workspace-java-test-request/v1"

        const val RunnerRequestSchema =
            "vexlife-prod.workspace-java-test-runner-request/v1"

        const val RunnerResultSchema =
            "vexlife-prod.workspace-java-test-runner-result/v1"

        const val PlanSchema =
            "vexlife-prod.sandboxed-command-plan/v1"

        const val ReceiptSchema =
            "vexlife-prod.sandboxed-command-receipt/v1"

        const val RequiredCompilerModule =
            "jdk.compiler"

        const val ResourceLeaseRefPrefix =
            "lease.vexlife-prod.workspace-run.sha256."

        const val RuntimeRefPrefix =
            "runtime.vexlife-prod.workspace-java-test.sha256."

        const val DispositionTestPassed =
            "TEST_PASSED"

        const val DispositionCompileFailed =
            "COMPILE_FAILED"

        const val DispositionTestFailed =
            "TEST_FAILED"

        const val DispositionSourceChanged =
            "SOURCE_CHANGED"

        const val DispositionTimeout =
            "TIMEOUT"

        const val DispositionHostFailed =
            "HOST_FAILED"

        val RunnerDispositions =
            setOf(
                DispositionTestPassed,
                DispositionCompileFailed,
                DispositionTestFailed,
                DispositionSourceChanged,
                DispositionTimeout,
                DispositionHostFailed,
            )

        val ReceiptDispositions =
            RunnerDispositions

        const val RefusedOperationRef =
            "WORKSPACE_RUN_OPERATION_REF_INVALID"

        const val RefusedGrantNotCurrent =
            "WORKSPACE_RUN_GRANT_NOT_CURRENT"

        const val RefusedGrantGenerationChanged =
            "WORKSPACE_RUN_GRANT_GENERATION_CHANGED"

        const val RefusedSourceNotCurrent =
            "WORKSPACE_RUN_SOURCE_NOT_CURRENT"

        const val RefusedWorkingScopeChanged =
            "WORKSPACE_RUN_WORKING_SCOPE_CHANGED"

        const val RefusedRuntimeNotCurrent =
            "WORKSPACE_RUN_RUNTIME_NOT_CURRENT"

        const val RefusedRuntimeChanged =
            "WORKSPACE_RUN_RUNTIME_CHANGED"

        const val RefusedSourceCapture =
            "WORKSPACE_RUN_SOURCE_CAPTURE_FAILED"

        const val RefusedScratchNotReady =
            "WORKSPACE_RUN_SCRATCH_NOT_READY"

        const val RefusedProcessNotStarted =
            "WORKSPACE_RUN_PROCESS_NOT_STARTED"

        const val MaximumOperationRefCharacters =
            256

        const val MaximumSandboxExecutableBytes =
            4_194_304

        const val MaximumProtocolStdoutBytes =
            524_288

        const val MaximumProtocolStderrBytes =
            32_768

        const val SecurityPolicyFileName =
            "java-test.policy"

        private val Sha256 =
            Regex(
                "[0-9a-f]{64}",
            )

        private val TestClass =
            Regex(
                "[A-Za-z_$][A-Za-z0-9_$]*(?:\\.[A-Za-z_$][A-Za-z0-9_$]*)*",
            )

        internal fun validJavaPath(
            value: String,
        ): Boolean =
            value.isNotBlank() &&
                value.length <=
                VexLifeWorkspaceJavaTestDefinitions.MaximumPathCharacters &&
                value.endsWith(
                    ".java",
                ) &&
                '\u0000' !in
                value &&
                '\\' !in
                value &&
                !value.startsWith(
                    "/",
                ) &&
                value
                    .split(
                        "/",
                    )
                    .all {
                        it.isNotBlank() &&
                            it !=
                            "." &&
                            it !=
                            ".."
                    }

        internal fun sandboxProfile(
            imageRoot: Path,
            scratch: Path,
            javaPath: Path,
        ): String {
            val image =
                sbpl(
                    imageRoot
                        .toAbsolutePath()
                        .normalize()
                        .toString(),
                )
            val scratchPath =
                sbpl(
                    scratch
                        .toAbsolutePath()
                        .normalize()
                        .toString(),
                )
            val java =
                sbpl(
                    javaPath
                        .toAbsolutePath()
                        .normalize()
                        .toString(),
                )
            return """
                |(version 1)
                |(deny default)
                |(allow process-fork)
                |(allow signal (target self))
                |(allow sysctl-read)
                |(allow file-read-metadata)
                |(allow file-read*
                |  (subpath "$image")
                |  (subpath "$scratchPath")
                |  (subpath "/System/Library")
                |  (subpath "/usr/lib")
                |  (subpath "/usr/share/icu")
                |  (subpath "/usr/share/zoneinfo")
                |  (subpath "/private/var/db/timezone")
                |  (literal "/dev/null")
                |  (literal "/dev/random")
                |  (literal "/dev/urandom"))
                |(allow file-write*
                |  (subpath "$scratchPath")
                |  (literal "/dev/null"))
                |(allow process-exec (literal "$java"))
                |(allow mach-lookup
                |  (global-name "com.apple.system.logger")
                |  (global-name "com.apple.system.opendirectoryd.libinfo"))
            """.trimMargin()
        }

        internal fun securityPolicy(
            scratch: Path,
        ): String {
            val path =
                policyPath(
                    scratch
                        .toAbsolutePath()
                        .normalize()
                        .toString(),
                )
            return """
                |grant {
                |  permission java.io.FilePermission "$path/-", "read,write,delete";
                |  permission java.util.PropertyPermission "*", "read";
                |};
            """.trimMargin() +
                "\n"
        }

        internal fun cleanupScratch(
            path: Path,
        ): Boolean =
            runCatching {
                    if (
                        !Files.exists(
                            path,
                            LinkOption.NOFOLLOW_LINKS,
                        )
                    ) {
                        return@runCatching
                    }
                    Files.walkFileTree(
                        path,
                        object : SimpleFileVisitor<Path>() {
                            override fun visitFile(
                                file: Path,
                                attrs: BasicFileAttributes,
                            ): FileVisitResult {
                                Files.deleteIfExists(
                                    file,
                                )
                                return FileVisitResult.CONTINUE
                            }

                            override fun postVisitDirectory(
                                dir: Path,
                                exc: java.io.IOException?,
                            ): FileVisitResult {
                                if (
                                    exc !=
                                        null
                                ) {
                                    throw exc
                                }
                                Files.deleteIfExists(
                                    dir,
                                )
                                return FileVisitResult.CONTINUE
                            }
                        },
                    )
                }
                .isSuccess

        private fun removeScratchParentIfEmpty(
            parent: Path,
        ) {
            runCatching {
                Files.deleteIfExists(
                    parent,
                )
            }
        }

        internal fun readNoFollow(
            path: Path,
            maximumBytes: Int,
        ): ByteArray? =
            runCatching {
                    Files.newByteChannel(
                        path,
                        setOf(
                            StandardOpenOption.READ,
                            LinkOption.NOFOLLOW_LINKS,
                        ),
                    )
                        .use {
                            channel ->
                            val output =
                                ByteArrayOutputStream()
                            val buffer =
                                ByteBuffer.allocate(
                                    8_192,
                                )
                            while (
                                true
                            ) {
                                val read =
                                    channel.read(
                                        buffer,
                                    )
                                if (
                                    read <
                                    0
                                ) {
                                    break
                                }
                                if (
                                    output.size() +
                                        read >
                                    maximumBytes
                                ) {
                                    return null
                                }
                                output.write(
                                    buffer.array(),
                                    0,
                                    read,
                                )
                                buffer.clear()
                            }
                            output.toByteArray()
                        }
                }
                .getOrNull()

        internal fun sha256(
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

        private fun sbpl(
            value: String,
        ): String =
            value
                .replace(
                    "\\",
                    "\\\\",
                )
                .replace(
                    "\"",
                    "\\\"",
                )

        private fun policyPath(
            value: String,
        ): String =
            value
                .replace(
                    "\\",
                    "\\\\",
                )
                .replace(
                    "\"",
                    "\\\"",
                )
    }
}

// [VXG RealForever]
