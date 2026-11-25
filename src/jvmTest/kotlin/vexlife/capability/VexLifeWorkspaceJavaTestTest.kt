package vexlife.capability

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import vexlife.contract.VexContractJson
import vexlife.home.CanonicalVexLifeHomeFixture
import vexlife.sources.VexLifeLocalSourceStore
import vexlife.workspace.VexLifeWorkspaceRunStore

class VexLifeWorkspaceJavaTestTest {
    private data class Fixture(
        val parent: Path,
        val home: Path,
        val source: Path,
        val sourceRef: String,
        val localSources: VexLifeLocalSourceStore,
        val runs: VexLifeWorkspaceRunStore,
    )

    private fun withFixture(
        grantRuns: Boolean =
            true,
        block: (
            Fixture,
        ) -> Unit,
    ) {
        val parent =
            Files.createTempDirectory(
                "vexlife-workspace-java-test-",
            )
                .toRealPath()
        val person =
            Files.createDirectories(
                parent.resolve(
                    "person",
                ),
            )
        val home =
            person.resolve(
                "VexLife-Prod",
            )
        val source =
            Files.createDirectories(
                person.resolve(
                    "project",
                ),
            )

        try {
            CanonicalVexLifeHomeFixture.form(
                home =
                    home,
                deviceSuffix =
                    "workspace-java-test",
                lineageSuffix =
                    "workspace-java-test",
            )
            val localSources =
                VexLifeLocalSourceStore(
                    productHome =
                        home,
                    personHome =
                        person,
                    now = {
                        Instant.parse(
                            "2026-11-25T18:00:00Z",
                        )
                    },
                )
            assertIs<VexLifeLocalSourceStore.Outcome.Realized>(
                localSources.add(
                    source,
                ),
            )
            val sourceRef =
                localSources
                    .grants()
                    .single()
                    .sourceRef
            val runs =
                VexLifeWorkspaceRunStore(
                    productHome =
                        home,
                    localSources =
                        localSources,
                    now = {
                        Instant.parse(
                            "2026-11-25T18:01:00Z",
                        )
                    },
                )
            if (
                grantRuns
            ) {
                assertIs<VexLifeWorkspaceRunStore.Outcome.Realized>(
                    runs.grant(
                        sourceRef,
                    ),
                )
            }
            block(
                Fixture(
                    parent =
                        parent,
                    home =
                        home,
                    source =
                        source,
                    sourceRef =
                        sourceRef,
                    localSources =
                        localSources,
                    runs =
                        runs,
                ),
            )
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }

    private fun runtime(
        suffix: Char =
            'a',
    ) =
        VexLifeWorkspaceJavaTest.RuntimeObservation(
            runtimeRef =
                VexLifeWorkspaceJavaTest.RuntimeRefPrefix +
                    suffix.toString()
                        .repeat(
                            64,
                        ),
            imageRoot =
                "/product/image-" +
                    suffix,
            javaPath =
                "/product/image-" +
                    suffix +
                    "/runtime/bin/java",
            classPath =
                "/product/image-" +
                    suffix +
                    "/lib/*",
            sandboxPath =
                "/usr/bin/sandbox-exec",
            sandboxSha256 =
                suffix.toString()
                    .repeat(
                        64,
                    ),
        )

    private fun request() =
        VexLifeWorkspaceJavaTest.Request(
            sources =
                listOf(
                    "src/Calculator.java",
                    "test/CalculatorTest.java",
                ),
            testClass =
                "CalculatorTest",
        )

    private fun writeProject(
        source: Path,
    ) {
        val src =
            Files.createDirectories(
                source.resolve(
                    "src",
                ),
            )
        val test =
            Files.createDirectories(
                source.resolve(
                    "test",
                ),
            )
        Files.writeString(
            src.resolve(
                "Calculator.java",
            ),
            "public final class Calculator { public static int add(int a, int b) { return a + b; } }\n",
        )
        Files.writeString(
            test.resolve(
                "CalculatorTest.java",
            ),
            "public final class CalculatorTest { public static void main(String[] args) { if (Calculator.add(2, 3) != 5) throw new AssertionError(); } }\n",
        )
    }

    @Test
    fun readableWorkspaceDoesNotBecomeProjectRunAuthorityWithoutSeparateGrant() =
        withFixture(
            grantRuns =
                false,
        ) {
            fixture ->
            writeProject(
                fixture.source,
            )
            val owner =
                VexLifeWorkspaceJavaTest(
                    productHome =
                        fixture.home,
                    localSources =
                        fixture.localSources,
                    workspaceRuns =
                        fixture.runs,
                    runtimeSource = {
                        runtime()
                    },
                    runner = {
                        error(
                            "runner must not execute without project-run authority",
                        )
                    },
                )

            val outcome =
                assertIs<VexLifeWorkspaceJavaTest.PlanOutcome.Refused>(
                    owner.plan(
                        operationRef =
                            "operation.test.no-run-grant",
                        sourceRef =
                            fixture.sourceRef,
                        request =
                            request(),
                    ),
                )
            assertEquals(
                VexLifeWorkspaceJavaTest.RefusedGrantNotCurrent,
                outcome.reason,
            )
        }

    @Test
    fun exactSourceCopiesReachOnlyProductSelectedSandboxRunner() =
        withFixture {
            fixture ->
            writeProject(
                fixture.source,
            )
            var observedSpec: VexLifeWorkspaceJavaTest.ProcessSpec? =
                null
            var observedRunnerRequest: VexLifeWorkspaceJavaTest.RunnerRequest? =
                null
            val expectedRuntime =
                runtime()
            val owner =
                VexLifeWorkspaceJavaTest(
                    productHome =
                        fixture.home,
                    localSources =
                        fixture.localSources,
                    workspaceRuns =
                        fixture.runs,
                    runtimeSource = {
                        expectedRuntime
                    },
                    runner = {
                        spec ->
                        observedSpec =
                            spec
                        val runnerRequest =
                            VexContractJson.decode<VexLifeWorkspaceJavaTest.RunnerRequest>(
                                spec.arguments.last(),
                            )
                        observedRunnerRequest =
                            runnerRequest
                        assertTrue(
                            spec.workingDirectory !=
                                fixture.source,
                        )
                        assertEquals(
                            VexLifeWorkspaceJavaTestDefinitions.ScratchDirectoryName,
                            spec.workingDirectory
                                .parent
                                .fileName
                                .toString(),
                        )
                        val copied =
                            Files.readString(
                                spec.workingDirectory.resolve(
                                    "src/src/Calculator.java",
                                ),
                            )
                        assertTrue(
                            copied.contains(
                                "return a + b",
                            ),
                        )
                        val profile =
                            spec.arguments[
                                spec.arguments.indexOf(
                                    "-p",
                                ) +
                                    1
                            ]
                        assertFalse(
                            profile.contains(
                                "(subpath \"" +
                                    fixture.source +
                                    "\")",
                            ),
                        )
                        assertTrue(
                            profile.contains(
                                spec.workingDirectory.toString(),
                            ),
                        )
                        VexLifeWorkspaceJavaTest.ProcessResult(
                            started =
                                true,
                            exitCode =
                                0,
                            stdout =
                                VexContractJson.canonicalEncode(
                                    VexLifeWorkspaceJavaTest.RunnerResult(
                                        disposition =
                                            VexLifeWorkspaceJavaTest.DispositionTestPassed,
                                        testExitCode =
                                            0,
                                    ),
                                ),
                            stderr =
                                "",
                            timedOut =
                                false,
                            stdoutTruncated =
                                false,
                            stderrTruncated =
                                false,
                        )
                    },
                )

            val plan =
                assertIs<VexLifeWorkspaceJavaTest.PlanOutcome.Ready>(
                    owner.plan(
                        operationRef =
                            "operation.test.exact-runner",
                        sourceRef =
                            fixture.sourceRef,
                        request =
                            request(),
                    ),
                )
                    .plan
            assertFalse(
                plan.externalEffectsExecuted,
            )
            assertTrue(
                plan.environment.isEmpty(),
            )

            val receipt =
                assertIs<VexLifeWorkspaceJavaTest.Outcome.Completed>(
                    owner.execute(
                        plan,
                    ),
                )
                    .receipt

            val spec =
                assertNotNull(
                    observedSpec,
                )
            assertEquals(
                expectedRuntime.sandboxPath,
                spec.program,
            )
            assertTrue(
                spec.environment.isEmpty(),
            )
            assertEquals(
                "-p",
                spec.arguments.first(),
            )
            assertEquals(
                expectedRuntime.javaPath,
                spec.arguments[2],
            )
            assertTrue(
                spec.arguments.contains(
                    VexLifeWorkspaceJavaTestDefinitions.ProcessMainClass,
                ),
            )
            assertFalse(
                spec.arguments.any {
                    it == "sh" ||
                        it == "bash" ||
                        it == "-c" ||
                        it == "gradle" ||
                        it == "npm"
                },
            )

            val runnerRequest =
                assertNotNull(
                    observedRunnerRequest,
                )
            assertEquals(
                request().sources,
                runnerRequest.sources.map {
                    it.path
                },
            )
            assertEquals(
                VexLifeWorkspaceJavaTest.DispositionTestPassed,
                receipt.disposition,
            )
            assertTrue(
                receipt.externalEffectsExecuted,
            )
            assertTrue(
                receipt.effectAuthorityGranted,
            )
            assertTrue(
                receipt.currentEffectAuthority,
            )
            assertTrue(
                receipt.cleanupComplete,
            )
            assertFalse(
                Files.exists(
                    spec.workingDirectory,
                ),
            )
            assertEquals(
                2,
                receipt.sources.size,
            )
        }

    @Test
    fun compileAndTestFailuresRemainUsefulCompletedReceipts() =
        withFixture {
            fixture ->
            writeProject(
                fixture.source,
            )
            val outcomes =
                listOf(
                    VexLifeWorkspaceJavaTest.RunnerResult(
                        disposition =
                            VexLifeWorkspaceJavaTest.DispositionCompileFailed,
                        diagnostics =
                            listOf(
                                VexLifeWorkspaceJavaTest.CompilerDiagnostic(
                                    kind =
                                        "ERROR",
                                    code =
                                        "compiler.err.test",
                                    sourcePath =
                                        "src/Calculator.java",
                                    line =
                                        1,
                                    column =
                                        1,
                                    message =
                                        "synthetic compile failure",
                                ),
                            ),
                    ),
                    VexLifeWorkspaceJavaTest.RunnerResult(
                        disposition =
                            VexLifeWorkspaceJavaTest.DispositionTestFailed,
                        testExitCode =
                            1,
                        testStderr =
                            "synthetic assertion failure",
                    ),
                )
            outcomes.forEachIndexed {
                index,
                expected,
                ->
                val owner =
                    VexLifeWorkspaceJavaTest(
                        productHome =
                            fixture.home,
                        localSources =
                            fixture.localSources,
                        workspaceRuns =
                            fixture.runs,
                        runtimeSource = {
                            runtime()
                        },
                        runner = {
                            VexLifeWorkspaceJavaTest.ProcessResult(
                                started =
                                    true,
                                exitCode =
                                    0,
                                stdout =
                                    VexContractJson.canonicalEncode(
                                        expected,
                                    ),
                                stderr =
                                    "",
                                timedOut =
                                    false,
                                stdoutTruncated =
                                    false,
                                stderrTruncated =
                                    false,
                            )
                        },
                    )
                val receipt =
                    assertIs<VexLifeWorkspaceJavaTest.Outcome.Completed>(
                        owner.test(
                            operationRef =
                                "operation.test.failure." +
                                    index,
                            sourceRef =
                                fixture.sourceRef,
                            request =
                                request(),
                        ),
                    )
                        .receipt
                assertEquals(
                    expected.disposition,
                    receipt.disposition,
                )
                assertTrue(
                    receipt.cleanupComplete,
                )
            }
        }

    @Test
    fun revocationAndRuntimeDriftStopBeforeProjectProcess() =
        withFixture {
            fixture ->
            writeProject(
                fixture.source,
            )
            var currentRuntime =
                runtime(
                    'a',
                )
            var runnerCalled =
                false
            val owner =
                VexLifeWorkspaceJavaTest(
                    productHome =
                        fixture.home,
                    localSources =
                        fixture.localSources,
                    workspaceRuns =
                        fixture.runs,
                    runtimeSource = {
                        currentRuntime
                    },
                    runner = {
                        runnerCalled =
                            true
                        error(
                            "runner must not execute after currentness drift",
                        )
                    },
                )

            val firstPlan =
                assertIs<VexLifeWorkspaceJavaTest.PlanOutcome.Ready>(
                    owner.plan(
                        operationRef =
                            "operation.test.revoke",
                        sourceRef =
                            fixture.sourceRef,
                        request =
                            request(),
                    ),
                )
                    .plan
            assertIs<VexLifeWorkspaceRunStore.Outcome.Realized>(
                fixture.runs.revoke(
                    fixture.sourceRef,
                ),
            )
            assertEquals(
                VexLifeWorkspaceJavaTest.RefusedGrantNotCurrent,
                assertIs<VexLifeWorkspaceJavaTest.Outcome.Refused>(
                    owner.execute(
                        firstPlan,
                    ),
                )
                    .reason,
            )
            assertFalse(
                runnerCalled,
            )

            assertIs<VexLifeWorkspaceRunStore.Outcome.Realized>(
                fixture.runs.grant(
                    fixture.sourceRef,
                ),
            )
            val secondPlan =
                assertIs<VexLifeWorkspaceJavaTest.PlanOutcome.Ready>(
                    owner.plan(
                        operationRef =
                            "operation.test.runtime-drift",
                        sourceRef =
                            fixture.sourceRef,
                        request =
                            request(),
                    ),
                )
                    .plan
            currentRuntime =
                runtime(
                    'b',
                )
            assertEquals(
                VexLifeWorkspaceJavaTest.RefusedRuntimeChanged,
                assertIs<VexLifeWorkspaceJavaTest.Outcome.Refused>(
                    owner.execute(
                        secondPlan,
                    ),
                )
                    .reason,
            )
            assertFalse(
                runnerCalled,
            )
        }

    @Test
    fun intermediateSymlinkSourceEscapeIsRejectedBeforeProjectProcess() =
        withFixture {
            fixture ->
            val outside =
                Files.createDirectories(
                    fixture.parent.resolve(
                        "outside",
                    ),
                )
            Files.writeString(
                outside.resolve(
                    "Escape.java",
                ),
                "public final class Escape {}\n",
            )
            Files.createDirectories(
                fixture.source.resolve(
                    "src",
                ),
            )
            Files.createSymbolicLink(
                fixture.source.resolve(
                    "src/escape",
                ),
                outside,
            )
            Files.createDirectories(
                fixture.source.resolve(
                    "test",
                ),
            )
            Files.writeString(
                fixture.source.resolve(
                    "test/CalculatorTest.java",
                ),
                "public final class CalculatorTest { public static void main(String[] args) {} }\n",
            )
            var runnerCalled =
                false
            val owner =
                VexLifeWorkspaceJavaTest(
                    productHome =
                        fixture.home,
                    localSources =
                        fixture.localSources,
                    workspaceRuns =
                        fixture.runs,
                    runtimeSource = {
                        runtime()
                    },
                    runner = {
                        runnerCalled =
                            true
                        error(
                            "runner must not execute for escaped source",
                        )
                    },
                )

            val outcome =
                owner.test(
                    operationRef =
                        "operation.test.symlink",
                    sourceRef =
                        fixture.sourceRef,
                    request =
                        VexLifeWorkspaceJavaTest.Request(
                            sources =
                                listOf(
                                    "src/escape/Escape.java",
                                    "test/CalculatorTest.java",
                                ),
                            testClass =
                                "CalculatorTest",
                        ),
                )
            assertEquals(
                VexLifeWorkspaceJavaTest.RefusedSourceCapture,
                assertIs<VexLifeWorkspaceJavaTest.Outcome.Refused>(
                    outcome,
                )
                    .reason,
            )
            assertFalse(
                runnerCalled,
            )
        }

    @Test
    fun residualScratchIsRecoveredAndRemoved() =
        withFixture {
            fixture ->
            writeProject(
                fixture.source,
            )
            val currentRuntime =
                runtime()
            val grant =
                assertNotNull(
                    fixture.runs.currentGrant(
                        fixture.sourceRef,
                    ),
                )
            val operationRef =
                "operation.test.residual"
            val operationName =
                sha256(
                    listOf(
                        operationRef,
                        grant.grantRef,
                        currentRuntime.runtimeRef,
                    )
                        .joinToString(
                            "\u0000",
                        ),
                )
                    .take(
                        24,
                    )
            val scratch =
                Files.createDirectories(
                    fixture.source.resolve(
                        VexLifeWorkspaceJavaTestDefinitions.ScratchDirectoryName,
                    )
                        .resolve(
                            operationName,
                        ),
                )
            Files.writeString(
                scratch.resolve(
                    "stale.txt",
                ),
                "stale",
            )

            val owner =
                VexLifeWorkspaceJavaTest(
                    productHome =
                        fixture.home,
                    localSources =
                        fixture.localSources,
                    workspaceRuns =
                        fixture.runs,
                    runtimeSource = {
                        currentRuntime
                    },
                    runner = {
                        VexLifeWorkspaceJavaTest.ProcessResult(
                            started =
                                true,
                            exitCode =
                                0,
                            stdout =
                                VexContractJson.canonicalEncode(
                                    VexLifeWorkspaceJavaTest.RunnerResult(
                                        disposition =
                                            VexLifeWorkspaceJavaTest.DispositionTestPassed,
                                        testExitCode =
                                            0,
                                    ),
                                ),
                            stderr =
                                "",
                            timedOut =
                                false,
                            stdoutTruncated =
                                false,
                            stderrTruncated =
                                false,
                        )
                    },
                )
            val receipt =
                assertIs<VexLifeWorkspaceJavaTest.Outcome.Completed>(
                    owner.test(
                        operationRef =
                            operationRef,
                        sourceRef =
                            fixture.sourceRef,
                        request =
                            request(),
                    ),
                )
                    .receipt
            assertTrue(
                receipt.scratchResidualRecovered,
            )
            assertTrue(
                receipt.cleanupComplete,
            )
            assertFalse(
                Files.exists(
                    scratch,
                ),
            )
        }

    private fun sha256(
        value: String,
    ): String =
        MessageDigest
            .getInstance(
                "SHA-256",
            )
            .digest(
                value.toByteArray(
                    StandardCharsets.UTF_8,
                ),
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

// [VXG RealForever]
