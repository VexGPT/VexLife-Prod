package vexlife.capability

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.OpenOption
import java.nio.file.Path
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
import vexlife.workspace.VexLifeWorkspaceCheckStore

/**
 * Product-owned process owner for one registered declarative workspace text check.
 *
 * Model/provider input never selects an executable, shell string, classpath, working directory,
 * environment key or timeout. Those are compiled from Product source and current host/source/grant
 * observations.
 */
class VexLifeWorkspaceTextCheck internal constructor(
    productHome: Path,
    private val localSources: VexLifeLocalSourceStore =
        VexLifeLocalSourceStore(
            productHome,
        ),
    private val workspaceChecks: VexLifeWorkspaceCheckStore =
        VexLifeWorkspaceCheckStore(
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
            VexLifeWorkspaceCheckDefinitions.CommandRef,
        val relativePath: String,
        val requiredLiterals: List<String> =
            emptyList(),
        val forbiddenLiterals: List<String> =
            emptyList(),
    ) {
        init {
            require(
                schemaVersion ==
                    RequestSchema &&
                    commandRef ==
                    VexLifeWorkspaceCheckDefinitions.CommandRef &&
                    relativePath.isNotBlank() &&
                    relativePath.length <=
                    VexLifeWorkspaceCheckDefinitions.MaximumPathCharacters &&
                    '\u0000' !in
                    relativePath &&
                    '\\' !in
                    relativePath &&
                    !relativePath.startsWith(
                        "/",
                    ) &&
                    relativePath
                        .split(
                            "/",
                        )
                        .all {
                            it.isNotBlank() &&
                                it !=
                                "." &&
                                it !=
                                ".."
                        } &&
                    requiredLiterals.size +
                    forbiddenLiterals.size <=
                    VexLifeWorkspaceCheckDefinitions.MaximumAssertions &&
                    requiredLiterals ==
                    requiredLiterals.distinct() &&
                    forbiddenLiterals ==
                    forbiddenLiterals.distinct() &&
                    (
                        requiredLiterals +
                            forbiddenLiterals
                    )
                        .all {
                            it.isNotEmpty() &&
                                it.length <=
                                VexLifeWorkspaceCheckDefinitions.MaximumLiteralCharacters &&
                                '\u0000' !in
                                it
                        },
            ) {
                "workspace text-check request is invalid"
            }
        }
    }

    @Serializable
    data class Failure(
        val code: String,
        val assertionIndex: Int? =
            null,
    )

    @Serializable
    data class CheckResult(
        val schemaVersion: String,
        val commandRef: String,
        val disposition: String,
        val relativePath: String,
        val observedSha256: String?,
        val bytes: Int?,
        val failures: List<Failure>,
        val contentReturned: Boolean,
        val effectsPerformed: Boolean,
    ) {
        init {
            require(
                schemaVersion ==
                    ResultSchema &&
                    commandRef ==
                    VexLifeWorkspaceCheckDefinitions.CommandRef &&
                    disposition in
                    setOf(
                        DispositionCheckCompleted,
                        DispositionCheckFailed,
                    ) &&
                    contentReturned ==
                    false &&
                    effectsPerformed ==
                    false &&
                    (
                        disposition ==
                            DispositionCheckCompleted
                    ) ==
                    failures.isEmpty(),
            ) {
                "workspace text-check result is invalid"
            }
        }
    }

    data class RuntimeObservation(
        val runtimeRef: String,
        val javaPath: String,
        val classPath: String,
    ) {
        init {
            require(
                runtimeRef.isNotBlank() &&
                    Path.of(
                        javaPath,
                    )
                        .isAbsolute &&
                    classPath.isNotBlank(),
            ) {
                "workspace check runtime observation is invalid"
            }
        }
    }

    fun interface RuntimeSource {
        fun current(): RuntimeObservation?
    }

    class PrivateServiceImageRuntimeSource(
        private val imageRoot: Path,
        private val expectedSourceSha: String,
    ) : RuntimeSource {
        override fun current(): RuntimeObservation {
            val observation =
                VexLifeProdPrivateServiceImage.observe(
                    imageRoot =
                        imageRoot,
                    expectedSourceSha =
                        expectedSourceSha,
                )
            return RuntimeObservation(
                runtimeRef =
                    observation.imageRef,
                javaPath =
                    observation.javaPath,
                classPath =
                    imageRoot
                        .resolve(
                            "lib/*",
                        )
                        .normalize()
                        .toString(),
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
        val checkGrantRef: String,
        val runtimeRef: String,
        val javaPath: String,
        val classPath: String,
        val workingDirectory: String,
        val request: Request,
        val timeoutMs: Long,
        val maximumStdoutBytes: Int,
        val maximumStderrBytes: Int,
        val environment: Map<String, String>,
        val externalEffectsExecuted: Boolean,
    ) {
        init {
            require(
                schemaVersion ==
                    PlanSchema &&
                    processRef ==
                    VexLifeWorkspaceCheckDefinitions.ProcessRef &&
                    commandRef ==
                    VexLifeWorkspaceCheckDefinitions.CommandRef &&
                    operationRef.isNotBlank() &&
                    workingScopeRef.isNotBlank() &&
                    resourceLeaseRef.startsWith(
                        ResourceLeaseRefPrefix,
                    ) &&
                    sourceGrantedAt.isNotBlank() &&
                    checkGrantRef.isNotBlank() &&
                    runtimeRef.isNotBlank() &&
                    Path.of(
                        javaPath,
                    )
                        .isAbsolute &&
                    Path.of(
                        workingDirectory,
                    )
                        .isAbsolute &&
                    timeoutMs ==
                    VexLifeWorkspaceCheckDefinitions.TimeoutMs &&
                    maximumStdoutBytes ==
                    VexLifeWorkspaceCheckDefinitions.MaximumStdoutBytes &&
                    maximumStderrBytes ==
                    VexLifeWorkspaceCheckDefinitions.MaximumStderrBytes &&
                    environment.isEmpty() &&
                    !externalEffectsExecuted,
            ) {
                "workspace text-check plan is invalid"
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
        val checkGrantRef: String,
        val runtimeRef: String,
        val relativePath: String,
        val disposition: String,
        val exitCode: Int?,
        val timedOut: Boolean,
        val stdout: String,
        val stderr: String,
        val stdoutTruncated: Boolean,
        val stderrTruncated: Boolean,
        val observedSha256: String?,
        val bytes: Int?,
        val failures: List<Failure>,
        val externalEffectsExecuted: Boolean,
        val effectAuthorityGranted: Boolean,
        val currentEffectAuthority: Boolean,
    ) {
        init {
            require(
                schemaVersion ==
                    ReceiptSchema &&
                    processRef ==
                    VexLifeWorkspaceCheckDefinitions.ProcessRef &&
                    commandRef ==
                    VexLifeWorkspaceCheckDefinitions.CommandRef &&
                    disposition in
                    setOf(
                        DispositionCheckCompleted,
                        DispositionCheckFailed,
                        DispositionTimeout,
                        DispositionProcessFailed,
                    ) &&
                    externalEffectsExecuted &&
                    effectAuthorityGranted &&
                    (
                        disposition ==
                            DispositionTimeout
                    ) ==
                    timedOut,
            ) {
                "workspace text-check receipt is invalid"
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
                            "vexlife-workspace-check-stdout",
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
                            "vexlife-workspace-check-stderr",
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

        VexLifeWorkspaceCheckDefinitions.requireCommand(
            request.commandRef,
        )

        val grant =
            workspaceChecks.currentGrant(
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
                    VexLifeWorkspaceCheckDefinitions.ProcessRef,
                commandRef =
                    request.commandRef,
                operationRef =
                    operationRef,
                workingScopeRef =
                    sourceRef,
                resourceLeaseRef =
                    resourceLeaseRef(
                        grantRef =
                            grant.grantRef,
                        sourceGrantedAt =
                            grant.sourceGrantedAt,
                        runtimeRef =
                            runtime.runtimeRef,
                    ),
                sourceGrantedAt =
                    grant.sourceGrantedAt,
                checkGrantRef =
                    grant.grantRef,
                runtimeRef =
                    runtime.runtimeRef,
                javaPath =
                    runtime.javaPath,
                classPath =
                    runtime.classPath,
                workingDirectory =
                    root.toString(),
                request =
                    request,
                timeoutMs =
                    VexLifeWorkspaceCheckDefinitions.TimeoutMs,
                maximumStdoutBytes =
                    VexLifeWorkspaceCheckDefinitions.MaximumStdoutBytes,
                maximumStderrBytes =
                    VexLifeWorkspaceCheckDefinitions.MaximumStderrBytes,
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
            workspaceChecks.currentGrant(
                plan.workingScopeRef,
            )
                ?: return Outcome.Refused(
                    RefusedGrantNotCurrent,
                )
        if (
            grant.grantRef !=
                plan.checkGrantRef ||
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
            runtime.javaPath !=
                plan.javaPath ||
            runtime.classPath !=
                plan.classPath
        ) {
            return Outcome.Refused(
                RefusedRuntimeChanged,
            )
        }

        val result =
            runner.run(
                ProcessSpec(
                    program =
                        plan.javaPath,
                    arguments =
                        listOf(
                            "-Dfile.encoding=UTF-8",
                            "-cp",
                            plan.classPath,
                            VexLifeWorkspaceCheckDefinitions.ProcessMainClass,
                            VexContractJson.canonicalEncode(
                                plan.request,
                            ),
                        ),
                    workingDirectory =
                        root,
                    environment =
                        plan.environment,
                    timeoutMs =
                        plan.timeoutMs,
                    maximumStdoutBytes =
                        plan.maximumStdoutBytes,
                    maximumStderrBytes =
                        plan.maximumStderrBytes,
                ),
            )

        if (
            !result.started
        ) {
            return Outcome.Refused(
                RefusedProcessNotStarted,
            )
        }

        val currentAuthority =
            workspaceChecks
                .currentGrant(
                    plan.workingScopeRef,
                )
                ?.let {
                    it.grantRef ==
                        plan.checkGrantRef &&
                        it.sourceGrantedAt ==
                        plan.sourceGrantedAt
                }
                ?: false

        if (
            result.timedOut
        ) {
            return Outcome.Completed(
                receipt(
                    plan =
                        plan,
                    result =
                        result,
                    disposition =
                        DispositionTimeout,
                    evaluation =
                        null,
                    currentAuthority =
                        currentAuthority,
                ),
            )
        }

        if (
            result.exitCode !=
                0 ||
            result.stdoutTruncated
        ) {
            return Outcome.Completed(
                receipt(
                    plan =
                        plan,
                    result =
                        result,
                    disposition =
                        DispositionProcessFailed,
                    evaluation =
                        null,
                    currentAuthority =
                        currentAuthority,
                ),
            )
        }

        val evaluation =
            runCatching {
                    VexContractJson.decode<CheckResult>(
                        result.stdout.trim(),
                    )
                }
                .getOrNull()
        if (
            evaluation ==
                null ||
            evaluation.commandRef !=
                plan.commandRef ||
            evaluation.relativePath !=
                plan.request.relativePath
        ) {
            return Outcome.Completed(
                receipt(
                    plan =
                        plan,
                    result =
                        result,
                    disposition =
                        DispositionProcessFailed,
                    evaluation =
                        null,
                    currentAuthority =
                        currentAuthority,
                ),
            )
        }

        return Outcome.Completed(
            receipt(
                plan =
                    plan,
                result =
                    result,
                disposition =
                    evaluation.disposition,
                evaluation =
                    evaluation,
                currentAuthority =
                    currentAuthority,
            ),
        )
    }

    fun check(
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

    private fun receipt(
        plan: Plan,
        result: ProcessResult,
        disposition: String,
        evaluation: CheckResult?,
        currentAuthority: Boolean,
    ): Receipt =
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
            checkGrantRef =
                plan.checkGrantRef,
            runtimeRef =
                plan.runtimeRef,
            relativePath =
                plan.request.relativePath,
            disposition =
                disposition,
            exitCode =
                result.exitCode,
            timedOut =
                result.timedOut,
            stdout =
                result.stdout,
            stderr =
                result.stderr,
            stdoutTruncated =
                result.stdoutTruncated,
            stderrTruncated =
                result.stderrTruncated,
            observedSha256 =
                evaluation?.observedSha256,
            bytes =
                evaluation?.bytes,
            failures =
                evaluation?.failures.orEmpty(),
            externalEffectsExecuted =
                true,
            effectAuthorityGranted =
                true,
            currentEffectAuthority =
                currentAuthority,
        )

    companion object {
        const val RequestSchema =
            "vexlife-prod.workspace-text-check-request/v1"

        const val ResultSchema =
            "vexlife-prod.workspace-text-check-result/v1"

        const val PlanSchema =
            "vexlife-prod.typed-command-plan/v1"

        const val ReceiptSchema =
            "vexlife-prod.typed-command-receipt/v1"

        const val ResourceLeaseRefPrefix =
            "lease.vexlife-prod.workspace-check.sha256."

        const val DispositionCheckCompleted =
            "CHECK_COMPLETED"

        const val DispositionCheckFailed =
            "CHECK_FAILED"

        const val DispositionTimeout =
            "TIMEOUT"

        const val DispositionProcessFailed =
            "PROCESS_FAILED"

        const val FailureFileMissing =
            "FILE_MISSING"

        const val FailureNotRegular =
            "FILE_NOT_REGULAR"

        const val FailurePathNotConfined =
            "PATH_NOT_CONFINED"

        const val FailureFileTooLarge =
            "FILE_TOO_LARGE"

        const val FailureNotUtf8 =
            "FILE_NOT_UTF8"

        const val FailureRequiredMissing =
            "REQUIRED_LITERAL_MISSING"

        const val FailureForbiddenPresent =
            "FORBIDDEN_LITERAL_PRESENT"

        const val RefusedOperationRef =
            "WORKSPACE_CHECK_OPERATION_REF_INVALID"

        const val RefusedGrantNotCurrent =
            "WORKSPACE_CHECK_GRANT_NOT_CURRENT"

        const val RefusedGrantGenerationChanged =
            "WORKSPACE_CHECK_GRANT_GENERATION_CHANGED"

        const val RefusedSourceNotCurrent =
            "WORKSPACE_CHECK_SOURCE_NOT_CURRENT"

        const val RefusedWorkingScopeChanged =
            "WORKSPACE_CHECK_WORKING_SCOPE_CHANGED"

        const val RefusedRuntimeNotCurrent =
            "WORKSPACE_CHECK_RUNTIME_NOT_CURRENT"

        const val RefusedRuntimeChanged =
            "WORKSPACE_CHECK_RUNTIME_CHANGED"

        const val RefusedProcessNotStarted =
            "WORKSPACE_CHECK_PROCESS_NOT_STARTED"

        const val MaximumOperationRefCharacters =
            256

        internal fun evaluate(
            root: Path,
            request: Request,
        ): CheckResult {
            val ownedRoot =
                VexOwnedPath.requireRoot(
                    root,
                    "workspace text-check root",
                )
            val target =
                VexOwnedPath.resolveInside(
                    ownedRoot,
                    request.relativePath,
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
                    .getOrElse {
                        return failed(
                            request,
                            Failure(
                                FailurePathNotConfined,
                            ),
                        )
                    }
            if (
                parentState ==
                    VexOwnedPath.DirectoryState.Missing
            ) {
                return failed(
                    request,
                    Failure(
                        FailureFileMissing,
                    ),
                )
            }

            val attributes =
                try {
                    Files.readAttributes(
                        target,
                        BasicFileAttributes::class.java,
                        LinkOption.NOFOLLOW_LINKS,
                    )
                } catch (
                    _: java.nio.file.NoSuchFileException,
                ) {
                    return failed(
                        request,
                        Failure(
                            FailureFileMissing,
                        ),
                    )
                }

            if (
                !attributes.isRegularFile ||
                attributes.isSymbolicLink
            ) {
                return failed(
                    request,
                    Failure(
                        FailureNotRegular,
                    ),
                )
            }

            if (
                attributes.size() >
                    VexLifeWorkspaceCheckDefinitions.MaximumInputBytes
            ) {
                return failed(
                    request,
                    Failure(
                        FailureFileTooLarge,
                    ),
                )
            }

            val bytes =
                readBounded(
                    target,
                    VexLifeWorkspaceCheckDefinitions.MaximumInputBytes,
                )
                    ?: return failed(
                        request,
                        Failure(
                            FailureFileTooLarge,
                        ),
                    )

            val text =
                try {
                    StandardCharsets.UTF_8
                        .newDecoder()
                        .onMalformedInput(
                            CodingErrorAction.REPORT,
                        )
                        .onUnmappableCharacter(
                            CodingErrorAction.REPORT,
                        )
                        .decode(
                            ByteBuffer.wrap(
                                bytes,
                            ),
                        )
                        .toString()
                } catch (
                    _: java.nio.charset.CharacterCodingException,
                ) {
                    return failed(
                        request,
                        Failure(
                            FailureNotUtf8,
                        ),
                    )
                }

            val failures =
                buildList {
                    request.requiredLiterals
                        .forEachIndexed {
                            index,
                            literal,
                            ->
                            if (
                                literal !in
                                    text
                            ) {
                                add(
                                    Failure(
                                        code =
                                            FailureRequiredMissing,
                                        assertionIndex =
                                            index,
                                    ),
                                )
                            }
                        }

                    request.forbiddenLiterals
                        .forEachIndexed {
                            index,
                            literal,
                            ->
                            if (
                                literal in
                                    text
                            ) {
                                add(
                                    Failure(
                                        code =
                                            FailureForbiddenPresent,
                                        assertionIndex =
                                            index,
                                    ),
                                )
                            }
                        }
                }

            return CheckResult(
                schemaVersion =
                    ResultSchema,
                commandRef =
                    request.commandRef,
                disposition =
                    if (
                        failures.isEmpty()
                    ) {
                        DispositionCheckCompleted
                    } else {
                        DispositionCheckFailed
                    },
                relativePath =
                    request.relativePath,
                observedSha256 =
                    sha256(
                        bytes,
                    ),
                bytes =
                    bytes.size,
                failures =
                    failures,
                contentReturned =
                    false,
                effectsPerformed =
                    false,
            )
        }

        private fun failed(
            request: Request,
            failure: Failure,
        ): CheckResult =
            CheckResult(
                schemaVersion =
                    ResultSchema,
                commandRef =
                    request.commandRef,
                disposition =
                    DispositionCheckFailed,
                relativePath =
                    request.relativePath,
                observedSha256 =
                    null,
                bytes =
                    null,
                failures =
                    listOf(
                        failure,
                    ),
                contentReturned =
                    false,
                effectsPerformed =
                    false,
            )

        private fun readBounded(
            path: Path,
            maximumBytes: Int,
        ): ByteArray? {
            val output =
                ByteArrayOutputStream(
                    minOf(
                        maximumBytes,
                        8_192,
                    ),
                )
            val options =
                setOf<OpenOption>(
                    StandardOpenOption.READ,
                    LinkOption.NOFOLLOW_LINKS,
                )

            Files.newByteChannel(
                path,
                options,
            )
                .use {
                    channel ->
                    val buffer =
                        ByteBuffer.allocate(
                            4_096,
                        )
                    while (
                        true
                    ) {
                        buffer.clear()
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
                    }
                }

            return output.toByteArray()
        }

        private fun resourceLeaseRef(
            grantRef: String,
            sourceGrantedAt: String,
            runtimeRef: String,
        ): String =
            ResourceLeaseRefPrefix +
                sha256(
                    listOf(
                        grantRef,
                        sourceGrantedAt,
                        runtimeRef,
                    )
                        .joinToString(
                            "\u0000",
                        )
                        .toByteArray(
                            StandardCharsets.UTF_8,
                        ),
                )

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
    }
}

// [VXG RealForever]
