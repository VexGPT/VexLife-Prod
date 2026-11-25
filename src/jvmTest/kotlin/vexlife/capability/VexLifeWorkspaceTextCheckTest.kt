package vexlife.capability

import java.nio.file.Files
import java.nio.file.Path
import java.io.File
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
import vexlife.workspace.VexLifeWorkspaceCheckStore

class VexLifeWorkspaceTextCheckTest {
    private data class Fixture(
        val parent: Path,
        val home: Path,
        val source: Path,
        val sourceRef: String,
        val localSources: VexLifeLocalSourceStore,
        val checks: VexLifeWorkspaceCheckStore,
    )

    private fun withFixture(
        grantChecks: Boolean =
            true,
        block: (
            Fixture,
        ) -> Unit,
    ) {
        val parent =
            Files.createTempDirectory(
                "vexlife-workspace-text-check-",
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
                    "workspace-text-check",
                lineageSuffix =
                    "workspace-text-check",
            )
            val localSources =
                VexLifeLocalSourceStore(
                    productHome =
                        home,
                    personHome =
                        person,
                    now = {
                        Instant.parse(
                            "2026-11-24T20:00:00Z",
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
            val checks =
                VexLifeWorkspaceCheckStore(
                    productHome =
                        home,
                    localSources =
                        localSources,
                    now = {
                        Instant.parse(
                            "2026-11-24T20:01:00Z",
                        )
                    },
                )
            if (
                grantChecks
            ) {
                assertIs<VexLifeWorkspaceCheckStore.Outcome.Realized>(
                    checks.grant(
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
                    checks =
                        checks,
                ),
            )
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }

    private fun runtime(
        suffix: String =
            "a",
    ) =
        VexLifeWorkspaceTextCheck.RuntimeObservation(
            runtimeRef =
                "image.vexlife-prod.private-service." +
                    suffix.repeat(
                        32,
                    ),
            javaPath =
                "/product/runtime/bin/java",
            classPath =
                "/product/lib/*",
        )

    @Test
    fun evaluatorReturnsUsefulFailureWithoutReturningSourceBody() =
        withFixture {
            fixture ->
            Files.writeString(
                fixture.source.resolve(
                    "app.txt",
                ),
                "alpha\nbeta\n",
            )
            val result =
                VexLifeWorkspaceTextCheck.evaluate(
                    root =
                        fixture.source,
                    request =
                        VexLifeWorkspaceTextCheck.Request(
                            relativePath =
                                "app.txt",
                            requiredLiterals =
                                listOf(
                                    "alpha",
                                    "gamma",
                                ),
                            forbiddenLiterals =
                                listOf(
                                    "beta",
                                ),
                        ),
                )

            assertEquals(
                VexLifeWorkspaceTextCheck.DispositionCheckFailed,
                result.disposition,
            )
            assertEquals(
                listOf(
                    VexLifeWorkspaceTextCheck.Failure(
                        VexLifeWorkspaceTextCheck.FailureRequiredMissing,
                        1,
                    ),
                    VexLifeWorkspaceTextCheck.Failure(
                        VexLifeWorkspaceTextCheck.FailureForbiddenPresent,
                        0,
                    ),
                ),
                result.failures,
            )
            assertFalse(
                VexContractJson.canonicalEncode(
                    result,
                )
                    .contains(
                        "alpha\\nbeta",
                    ),
            )
            assertFalse(
                result.contentReturned,
            )
            assertFalse(
                result.effectsPerformed,
            )
            assertNotNull(
                result.observedSha256,
            )
        }

    @Test
    fun readableSourceDoesNotBecomeProcessAuthorityWithoutSeparateCheckGrant() =
        withFixture(
            grantChecks =
                false,
        ) {
            fixture ->
            val owner =
                VexLifeWorkspaceTextCheck(
                    productHome =
                        fixture.home,
                    localSources =
                        fixture.localSources,
                    workspaceChecks =
                        fixture.checks,
                    runtimeSource = {
                        runtime()
                    },
                    runner = {
                        error(
                            "runner must not execute without check authority",
                        )
                    },
                )

            val outcome =
                assertIs<VexLifeWorkspaceTextCheck.PlanOutcome.Refused>(
                    owner.plan(
                        operationRef =
                            "operation.test.no-check-grant",
                        sourceRef =
                            fixture.sourceRef,
                        request =
                            VexLifeWorkspaceTextCheck.Request(
                                relativePath =
                                    "app.txt",
                            ),
                    ),
                )
            assertEquals(
                VexLifeWorkspaceTextCheck.RefusedGrantNotCurrent,
                outcome.reason,
            )
        }

    @Test
    fun planCompilesExactProductOwnedProcessAndFailedCheckIsUsefulEvidence() =
        withFixture {
            fixture ->
            Files.writeString(
                fixture.source.resolve(
                    "app.txt",
                ),
                "alpha\n",
            )
            var observedSpec: VexLifeWorkspaceTextCheck.ProcessSpec? =
                null
            val expectedRuntime =
                runtime()
            val owner =
                VexLifeWorkspaceTextCheck(
                    productHome =
                        fixture.home,
                    localSources =
                        fixture.localSources,
                    workspaceChecks =
                        fixture.checks,
                    runtimeSource = {
                        expectedRuntime
                    },
                    runner = {
                        spec ->
                        observedSpec =
                            spec
                        val request =
                            VexContractJson.decode<VexLifeWorkspaceTextCheck.Request>(
                                spec.arguments.last(),
                            )
                        val result =
                            VexLifeWorkspaceTextCheck.evaluate(
                                fixture.source,
                                request,
                            )
                        VexLifeWorkspaceTextCheck.ProcessResult(
                            started =
                                true,
                            exitCode =
                                0,
                            stdout =
                                VexContractJson.canonicalEncode(
                                    result,
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

            val request =
                VexLifeWorkspaceTextCheck.Request(
                    relativePath =
                        "app.txt",
                    requiredLiterals =
                        listOf(
                            "missing",
                        ),
                )
            val planned =
                assertIs<VexLifeWorkspaceTextCheck.PlanOutcome.Ready>(
                    owner.plan(
                        operationRef =
                            "operation.test.plan",
                        sourceRef =
                            fixture.sourceRef,
                        request =
                            request,
                    ),
                )
                    .plan

            assertFalse(
                planned.externalEffectsExecuted,
            )
            assertEquals(
                VexLifeWorkspaceCheckDefinitions.CommandRef,
                planned.commandRef,
            )
            assertEquals(
                fixture.sourceRef,
                planned.workingScopeRef,
            )
            assertTrue(
                planned.resourceLeaseRef.startsWith(
                    VexLifeWorkspaceTextCheck.ResourceLeaseRefPrefix,
                ),
            )
            assertTrue(
                planned.environment.isEmpty(),
            )

            val receipt =
                assertIs<VexLifeWorkspaceTextCheck.Outcome.Completed>(
                    owner.execute(
                        planned,
                    ),
                )
                    .receipt

            val spec =
                assertNotNull(
                    observedSpec,
                )
            assertEquals(
                expectedRuntime.javaPath,
                spec.program,
            )
            assertEquals(
                fixture.source,
                spec.workingDirectory,
            )
            assertTrue(
                spec.environment.isEmpty(),
            )
            assertEquals(
                listOf(
                    "-Dfile.encoding=UTF-8",
                    "-cp",
                    expectedRuntime.classPath,
                    VexLifeWorkspaceCheckDefinitions.ProcessMainClass,
                ),
                spec.arguments.dropLast(
                    1,
                ),
            )
            assertFalse(
                spec.arguments.any {
                    it == "sh" ||
                        it == "bash" ||
                        it == "-c"
                },
            )
            assertEquals(
                VexLifeWorkspaceTextCheck.DispositionCheckFailed,
                receipt.disposition,
            )
            assertEquals(
                0,
                receipt.exitCode,
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
            assertEquals(
                VexLifeWorkspaceTextCheck.FailureRequiredMissing,
                receipt.failures.single().code,
            )
        }

    @Test
    fun revocationBetweenPlanAndExecutionStopsBeforeProcessStart() =
        withFixture {
            fixture ->
            var runnerCalled =
                false
            val owner =
                VexLifeWorkspaceTextCheck(
                    productHome =
                        fixture.home,
                    localSources =
                        fixture.localSources,
                    workspaceChecks =
                        fixture.checks,
                    runtimeSource = {
                        runtime()
                    },
                    runner = {
                        runnerCalled =
                            true
                        error(
                            "runner must not execute after revocation",
                        )
                    },
                )
            val plan =
                assertIs<VexLifeWorkspaceTextCheck.PlanOutcome.Ready>(
                    owner.plan(
                        operationRef =
                            "operation.test.revoke-before-run",
                        sourceRef =
                            fixture.sourceRef,
                        request =
                            VexLifeWorkspaceTextCheck.Request(
                                relativePath =
                                    "app.txt",
                            ),
                    ),
                )
                    .plan
            assertIs<VexLifeWorkspaceCheckStore.Outcome.Realized>(
                fixture.checks.revoke(
                    fixture.sourceRef,
                ),
            )

            val refused =
                assertIs<VexLifeWorkspaceTextCheck.Outcome.Refused>(
                    owner.execute(
                        plan,
                    ),
                )
            assertEquals(
                VexLifeWorkspaceTextCheck.RefusedGrantNotCurrent,
                refused.reason,
            )
            assertFalse(
                runnerCalled,
            )
        }

    @Test
    fun systemRunnerExecutesTheProductOwnedCheckerChildWithoutShellOrAmbientEnvironment() =
        withFixture {
            fixture ->
            Files.writeString(
                fixture.source.resolve(
                    "app.txt",
                ),
                "alpha\n",
            )
            val request =
                VexLifeWorkspaceTextCheck.Request(
                    relativePath =
                        "app.txt",
                    requiredLiterals =
                        listOf(
                            "alpha",
                        ),
                )
            val launchRoot =
                Path.of(
                    System.getProperty(
                        "user.dir",
                    ),
                )
                    .toAbsolutePath()
                    .normalize()
            val classPath =
                System.getProperty(
                    "java.class.path",
                )
                    .split(
                        File.pathSeparator,
                    )
                    .joinToString(
                        File.pathSeparator,
                    ) {
                        entry ->
                        Path.of(
                            entry,
                        )
                            .let {
                                if (
                                    it.isAbsolute
                                ) {
                                    it
                                } else {
                                    launchRoot.resolve(
                                        it,
                                    )
                                }
                            }
                            .normalize()
                            .toString()
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

            val process =
                VexLifeWorkspaceTextCheck.SystemRunner.run(
                    VexLifeWorkspaceTextCheck.ProcessSpec(
                        program =
                            java.toString(),
                        arguments =
                            listOf(
                                "-Dfile.encoding=UTF-8",
                                "-cp",
                                classPath,
                                VexLifeWorkspaceCheckDefinitions.ProcessMainClass,
                                VexContractJson.canonicalEncode(
                                    request,
                                ),
                            ),
                        workingDirectory =
                            fixture.source,
                        environment =
                            emptyMap(),
                        timeoutMs =
                            VexLifeWorkspaceCheckDefinitions.TimeoutMs,
                        maximumStdoutBytes =
                            VexLifeWorkspaceCheckDefinitions.MaximumStdoutBytes,
                        maximumStderrBytes =
                            VexLifeWorkspaceCheckDefinitions.MaximumStderrBytes,
                    ),
                )

            assertTrue(
                process.started,
            )
            assertFalse(
                process.timedOut,
            )
            assertFalse(
                process.stdoutTruncated,
            )
            assertEquals(
                0,
                process.exitCode,
                process.stderr,
            )
            val result =
                VexContractJson.decode<VexLifeWorkspaceTextCheck.CheckResult>(
                    process.stdout.trim(),
                )
            assertEquals(
                VexLifeWorkspaceTextCheck.DispositionCheckCompleted,
                result.disposition,
            )
            assertFalse(
                result.contentReturned,
            )
            assertFalse(
                result.effectsPerformed,
            )
        }

    @Test
    fun runtimeDriftBetweenPlanAndExecutionStopsBeforeProcessStart() =
        withFixture {
            fixture ->
            var current =
                runtime(
                    "a",
                )
            var runnerCalled =
                false
            val owner =
                VexLifeWorkspaceTextCheck(
                    productHome =
                        fixture.home,
                    localSources =
                        fixture.localSources,
                    workspaceChecks =
                        fixture.checks,
                    runtimeSource = {
                        current
                    },
                    runner = {
                        runnerCalled =
                            true
                        error(
                            "runner must not execute after runtime drift",
                        )
                    },
                )
            val plan =
                assertIs<VexLifeWorkspaceTextCheck.PlanOutcome.Ready>(
                    owner.plan(
                        operationRef =
                            "operation.test.runtime-drift",
                        sourceRef =
                            fixture.sourceRef,
                        request =
                            VexLifeWorkspaceTextCheck.Request(
                                relativePath =
                                    "app.txt",
                            ),
                    ),
                )
                    .plan

            current =
                runtime(
                    "b",
                )

            val refused =
                assertIs<VexLifeWorkspaceTextCheck.Outcome.Refused>(
                    owner.execute(
                        plan,
                    ),
                )
            assertEquals(
                VexLifeWorkspaceTextCheck.RefusedRuntimeChanged,
                refused.reason,
            )
            assertFalse(
                runnerCalled,
            )
        }
}

// [VXG RealForever]
