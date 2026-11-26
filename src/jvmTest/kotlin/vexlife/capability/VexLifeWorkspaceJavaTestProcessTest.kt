package vexlife.capability

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class VexLifeWorkspaceJavaTestProcessTest {
    private fun withScratch(
        block: (
            scratch: Path,
            sourceRoot: Path,
        ) -> Unit,
    ) {
        val parent =
            Files.createTempDirectory(
                "vexlife-workspace-java-test-process-",
            )
                .toRealPath()
        val scratch =
            Files.createDirectories(
                parent.resolve(
                    "scratch",
                ),
            )
        val sourceRoot =
            Files.createDirectories(
                scratch.resolve(
                    "src",
                ),
            )
        Files.createDirectories(
            scratch.resolve(
                "classes",
            ),
        )
        Files.createDirectories(
            scratch.resolve(
                "empty-classpath",
            ),
        )
        Files.createDirectories(
            scratch.resolve(
                "tmp",
            ),
        )
        Files.createDirectories(
            scratch.resolve(
                "home",
            ),
        )
        Files.writeString(
            scratch.resolve(
                VexLifeWorkspaceJavaTest.SecurityPolicyFileName,
            ),
            VexLifeWorkspaceJavaTest.securityPolicy(
                scratch,
            ),
            StandardCharsets.UTF_8,
        )

        try {
            block(
                scratch,
                sourceRoot,
            )
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }

    private fun write(
        sourceRoot: Path,
        relative: String,
        content: String,
    ): VexLifeWorkspaceJavaTest.SourceObservation {
        val target =
            sourceRoot.resolve(
                relative,
            )
        Files.createDirectories(
            target.parent,
        )
        val bytes =
            content.toByteArray(
                StandardCharsets.UTF_8,
            )
        Files.write(
            target,
            bytes,
        )
        return VexLifeWorkspaceJavaTest.SourceObservation(
            path =
                relative,
            sha256 =
                VexLifeWorkspaceJavaTest.sha256(
                    bytes,
                ),
            bytes =
                bytes.size,
        )
    }



    @Test
    fun hostFailureDiagnosticIsBoundedAndSingleLine() {
        val diagnostic =
            VexLifeWorkspaceJavaTestProcess.hostFailureDiagnostic(
                "HOST_TEST",
                IllegalStateException(
                    "line one\nline two\tline three",
                ),
            )

        assertTrue(
            diagnostic.startsWith(
                "HOST_TEST:java.lang.IllegalStateException:",
            ),
        )
        assertFalse(
            diagnostic.contains(
                '\n',
            ),
        )
        assertFalse(
            diagnostic.contains(
                '\t',
            ),
        )
        assertTrue(
            diagnostic.length <=
                VexLifeWorkspaceJavaTestDefinitions.MaximumStderrBytes,
        )
    }

    @Test
    fun processStartFailureReturnsBoundedHostDiagnostic() {
        val working =
            Files.createTempDirectory(
                "vexlife-workspace-java-test-process-start-failure-",
            )
                .toRealPath()
        try {
            val result =
                VexLifeWorkspaceJavaTestProcess.runChild(
                    program =
                        working.resolve(
                            "missing-java",
                        ),
                    arguments =
                        emptyList(),
                    workingDirectory =
                        working,
                    timeoutMs =
                        1_000,
                )

            assertFalse(
                result.started,
            )
            assertEquals(
                null,
                result.exitCode,
            )
            assertTrue(
                result.stderr.startsWith(
                    "PROCESS_START_FAILED:",
                ),
            )
            assertTrue(
                result.stderr.length <=
                    VexLifeWorkspaceJavaTestDefinitions.MaximumStderrBytes,
            )
        } finally {
            working
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun compilerAndTestMainPassWithoutProjectToolchainOrPackageManager() =
        withScratch {
            scratch,
            sourceRoot,
            ->
            val calculator =
                write(
                    sourceRoot,
                    "src/Calculator.java",
                    "public final class Calculator { public static int add(int a, int b) { return a + b; } }\n",
                )
            val test =
                write(
                    sourceRoot,
                    "test/CalculatorTest.java",
                    """
                    public final class CalculatorTest {
                      public static void main(String[] args) {
                        if (Calculator.add(20, 22) != 42) throw new AssertionError("sum");
                        System.out.print("PASS");
                      }
                    }
                    """.trimIndent() +
                        "\n",
                )

            val result =
                VexLifeWorkspaceJavaTestProcess.run(
                    scratch =
                        scratch,
                    request =
                        VexLifeWorkspaceJavaTest.RunnerRequest(
                            sources =
                                listOf(
                                    calculator,
                                    test,
                                ),
                            testClass =
                                "CalculatorTest",
                        ),
                )

            assertEquals(
                VexLifeWorkspaceJavaTest.DispositionTestPassed,
                result.disposition,
                result.testStderr,
            )
            assertEquals(
                0,
                result.testExitCode,
            )
            assertFalse(
                result.testTimedOut,
            )
            assertEquals(
                "PASS",
                result.testStdout,
            )
        }

    @Test
    fun compilerFailureReturnsBoundedTypedDiagnostics() =
        withScratch {
            scratch,
            sourceRoot,
            ->
            val broken =
                write(
                    sourceRoot,
                    "src/Broken.java",
                    "public final class Broken { public static int value() { return ; } }\n",
                )

            val result =
                VexLifeWorkspaceJavaTestProcess.run(
                    scratch =
                        scratch,
                    request =
                        VexLifeWorkspaceJavaTest.RunnerRequest(
                            sources =
                                listOf(
                                    broken,
                                ),
                            testClass =
                                "Broken",
                        ),
                )

            assertEquals(
                VexLifeWorkspaceJavaTest.DispositionCompileFailed,
                result.disposition,
            )
            assertTrue(
                result.diagnostics.isNotEmpty(),
            )
            assertTrue(
                result.diagnostics.size <=
                    VexLifeWorkspaceJavaTestDefinitions.MaximumDiagnostics,
            )
            assertTrue(
                result.diagnostics.all {
                    it.message.length <=
                        VexLifeWorkspaceJavaTestDefinitions.MaximumDiagnosticCharacters
                },
            )
        }

    @Test
    fun failingTestReturnsTypedTestFailure() =
        withScratch {
            scratch,
            sourceRoot,
            ->
            val test =
                write(
                    sourceRoot,
                    "test/FailingTest.java",
                    """
                    public final class FailingTest {
                      public static void main(String[] args) {
                        throw new AssertionError("expected synthetic failure");
                      }
                    }
                    """.trimIndent() +
                        "\n",
                )

            val result =
                VexLifeWorkspaceJavaTestProcess.run(
                    scratch =
                        scratch,
                    request =
                        VexLifeWorkspaceJavaTest.RunnerRequest(
                            sources =
                                listOf(
                                    test,
                                ),
                            testClass =
                                "FailingTest",
                        ),
                )

            assertEquals(
                VexLifeWorkspaceJavaTest.DispositionTestFailed,
                result.disposition,
            )
            assertNotNull(
                result.testExitCode,
            )
            assertTrue(
                result.testExitCode !=
                    0,
            )
            assertTrue(
                result.testStderr.contains(
                    "expected synthetic failure",
                ),
            )
        }

    @Test
    fun javaPolicyDeniesOutsideReadWriteNetworkAndProcessExecution() =
        withScratch {
            scratch,
            sourceRoot,
            ->
            val outside =
                Files.createTempFile(
                    scratch.parent,
                    "outside-",
                    ".txt",
                )
                    .toRealPath()
            Files.writeString(
                outside,
                "outside sentinel",
            )
            val literal =
                outside
                    .toString()
                    .replace(
                        "\\",
                        "\\\\",
                    )
                    .replace(
                        "\"",
                        "\\\"",
                    )
            val probe =
                write(
                    sourceRoot,
                    "test/PolicyProbe.java",
                    """
                    import java.nio.file.Files;
                    import java.nio.file.Path;
                    import java.net.Socket;

                    public final class PolicyProbe {
                      interface Attempt { void run() throws Exception; }

                      private static void denied(Attempt attempt) throws Exception {
                        try {
                          attempt.run();
                          throw new AssertionError("protected effect unexpectedly succeeded");
                        } catch (SecurityException expected) {
                          // expected
                        }
                      }

                      public static void main(String[] args) throws Exception {
                        denied(() -> Files.readString(Path.of("$literal")));
                        denied(() -> Files.writeString(Path.of("$literal"), "mutated"));
                        denied(() -> {
                          try (Socket ignored = new Socket("127.0.0.1", 9)) {
                            // must never connect
                          }
                        });
                        denied(() -> new ProcessBuilder("/usr/bin/true").start());
                        System.out.print("POLICY_PASS");
                      }
                    }
                    """.trimIndent() +
                        "\n",
                )

            val result =
                VexLifeWorkspaceJavaTestProcess.run(
                    scratch =
                        scratch,
                    request =
                        VexLifeWorkspaceJavaTest.RunnerRequest(
                            sources =
                                listOf(
                                    probe,
                                ),
                            testClass =
                                "PolicyProbe",
                        ),
                )

            assertEquals(
                VexLifeWorkspaceJavaTest.DispositionTestPassed,
                result.disposition,
                result.testStderr,
            )
            assertEquals(
                "POLICY_PASS",
                result.testStdout,
            )
            assertEquals(
                "outside sentinel",
                Files.readString(
                    outside,
                ),
            )
        }

    @Test
    fun copiedSourceDigestDriftStopsBeforeCompilation() =
        withScratch {
            scratch,
            sourceRoot,
            ->
            val observation =
                write(
                    sourceRoot,
                    "src/Stable.java",
                    "public final class Stable {}\n",
                )
            Files.writeString(
                sourceRoot.resolve(
                    "src/Stable.java",
                ),
                "public final class Stable { int drift; }\n",
            )

            val result =
                VexLifeWorkspaceJavaTestProcess.run(
                    scratch =
                        scratch,
                    request =
                        VexLifeWorkspaceJavaTest.RunnerRequest(
                            sources =
                                listOf(
                                    observation,
                                ),
                            testClass =
                                "Stable",
                        ),
                )

            assertEquals(
                VexLifeWorkspaceJavaTest.DispositionSourceChanged,
                result.disposition,
            )
        }
}

// [VXG RealForever]
