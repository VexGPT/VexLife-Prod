package vexlife.capability

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.time.Instant
import javax.tools.ToolProvider
import vexlife.home.CanonicalVexLifeHomeFixture
import vexlife.packaging.VexLifeProdPrivateServiceImage
import vexlife.sources.VexLifeLocalSourceStore
import vexlife.workspace.VexLifeWorkspaceRunStore

/**
 * Mac-only Product-owned proof adapter for C3-R1.
 *
 * This is not an ordinary Vex capability path. It proves the exact private service image and the
 * macOS sandbox boundary before R1 can be accepted. R2 later owns ordinary-turn integration.
 */
object VexLifeWorkspaceJavaTestMacProbe {
    fun run(
        imageRoot: Path,
        sourceSha: String,
    ) {
        require(
            System.getProperty(
                "os.name",
            ) ==
                "Mac OS X",
        ) {
            "workspace Java-test Mac proof requires macOS"
        }
        require(
            System.getProperty(
                "os.arch",
            ) in
                setOf(
                    "aarch64",
                    "arm64",
                ),
        ) {
            "workspace Java-test Mac proof requires Apple Silicon"
        }

        val observedImage =
            VexLifeProdPrivateServiceImage.observe(
                imageRoot =
                    imageRoot,
                expectedSourceSha =
                    sourceSha,
            )
        require(
            VexLifeWorkspaceJavaTest.RequiredCompilerModule in
                observedImage.runtimeModules,
        ) {
            "workspace Java-test Mac proof requires jdk.compiler in the exact private image"
        }

        val runtimeSource =
            VexLifeWorkspaceJavaTest.PrivateServiceImageRuntimeSource(
                imageRoot =
                    imageRoot,
                expectedSourceSha =
                    sourceSha,
            )
        val runtime =
            requireNotNull(
                runtimeSource.current(),
            ) {
                "workspace Java-test runtime is not current on this Mac"
            }

        proveOwnerPass(
            runtimeSource =
                runtimeSource,
            runtime =
                runtime,
        )
        proveMacSandbox(
            imageRoot =
                imageRoot,
            runtime =
                runtime,
        )
    }

    private fun proveOwnerPass(
        runtimeSource: VexLifeWorkspaceJavaTest.RuntimeSource,
        runtime: VexLifeWorkspaceJavaTest.RuntimeObservation,
    ) {
        val parent =
            Files.createTempDirectory(
                "vexlife-workspace-java-test-mac-owner-",
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
                    "workspace-java-test-mac",
                lineageSuffix =
                    "workspace-java-test-mac",
            )
            val localSources =
                VexLifeLocalSourceStore(
                    productHome =
                        home,
                    personHome =
                        person,
                    now = {
                        Instant.parse(
                            "2026-11-25T20:00:00Z",
                        )
                    },
                )
            require(
                localSources.add(
                    source,
                ) is
                    VexLifeLocalSourceStore.Outcome.Realized,
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
                            "2026-11-25T20:01:00Z",
                        )
                    },
                )
            require(
                runs.grant(
                    sourceRef,
                ) is
                    VexLifeWorkspaceRunStore.Outcome.Realized,
            )

            val src =
                Files.createDirectories(
                    source.resolve(
                        "src",
                    ),
                )
            val tests =
                Files.createDirectories(
                    source.resolve(
                        "test",
                    ),
                )
            val calculator =
                src.resolve(
                    "Calculator.java",
                )
            val test =
                tests.resolve(
                    "CalculatorTest.java",
                )
            Files.writeString(
                calculator,
                "public final class Calculator { public static int add(int a, int b) { return a + b; } }\n",
            )
            Files.writeString(
                test,
                """
                public final class CalculatorTest {
                  public static void main(String[] args) {
                    if (Calculator.add(19, 23) != 42) throw new AssertionError("sum");
                    System.out.print("OWNER_PASS");
                  }
                }
                """.trimIndent() +
                    "\n",
            )
            val before =
                mapOf(
                    calculator to
                        sha256(
                            calculator,
                        ),
                    test to
                        sha256(
                            test,
                        ),
                )

            val owner =
                VexLifeWorkspaceJavaTest(
                    productHome =
                        home,
                    localSources =
                        localSources,
                    workspaceRuns =
                        runs,
                    runtimeSource =
                        runtimeSource,
                )
            val outcome =
                owner.test(
                    operationRef =
                        "operation.vexlife-prod.c3-r1.mac-owner-pass",
                    sourceRef =
                        sourceRef,
                    request =
                        VexLifeWorkspaceJavaTest.Request(
                            sources =
                                listOf(
                                    "src/Calculator.java",
                                    "test/CalculatorTest.java",
                                ),
                            testClass =
                                "CalculatorTest",
                        ),
                )
            val receipt =
                requireNotNull(
                    (
                        outcome as?
                            VexLifeWorkspaceJavaTest.Outcome.Completed
                    )
                        ?.receipt,
                ) {
                    "workspace Java-test Mac owner proof did not complete"
                }
            require(
                receipt.disposition ==
                    VexLifeWorkspaceJavaTest.DispositionTestPassed &&
                    receipt.testExitCode ==
                    0 &&
                    receipt.testStdout ==
                    "OWNER_PASS" &&
                    receipt.cleanupComplete &&
                    receipt.currentEffectAuthority &&
                    receipt.sandboxSha256 ==
                    runtime.sandboxSha256
            ) {
                "workspace Java-test Mac owner proof did not return the exact PASS receipt"
            }
            before.forEach {
                (
                    path,
                    digest,
                ),
                ->
                require(
                    sha256(
                        path,
                    ) ==
                        digest,
                ) {
                    "workspace Java-test mutated admitted project source: $path"
                }
            }
            require(
                !Files.exists(
                    source.resolve(
                        VexLifeWorkspaceJavaTestDefinitions.ScratchDirectoryName,
                    ),
                ),
            ) {
                "workspace Java-test left scratch after successful Mac owner proof"
            }
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }

    private fun proveMacSandbox(
        imageRoot: Path,
        runtime: VexLifeWorkspaceJavaTest.RuntimeObservation,
    ) {
        val parent =
            Files.createTempDirectory(
                "vexlife-workspace-java-test-mac-sandbox-",
            )
                .toRealPath()
        val scratch =
            Files.createDirectories(
                parent.resolve(
                    "scratch",
                ),
            )
        val classes =
            Files.createDirectories(
                scratch.resolve(
                    "classes",
                ),
            )
        val source =
            scratch.resolve(
                "SandboxProbe.java",
            )
        val outside =
            parent.resolve(
                "outside-sentinel.txt",
            )
        Files.writeString(
            outside,
            "outside sentinel",
        )
        try {
            Files.writeString(
                source,
                """
                import java.nio.file.Files;
                import java.nio.file.Path;
                import java.net.ServerSocket;

                public final class SandboxProbe {
                  interface Attempt { void run() throws Exception; }

                  private static boolean denied(Attempt attempt) {
                    try {
                      attempt.run();
                      return false;
                    } catch (Throwable denied) {
                      return true;
                    }
                  }

                  public static void main(String[] args) throws Exception {
                    Path outside = Path.of(args[0]);
                    boolean readDenied = denied(() -> Files.readString(outside));
                    boolean writeDenied = denied(() -> Files.writeString(outside, "mutated"));
                    boolean networkDenied = denied(() -> {
                      try (ServerSocket ignored = new ServerSocket(0)) {
                        // Opening a local listener succeeds without sandbox confinement.
                      }
                    });
                    boolean processDenied = denied(() -> {
                      Process process = new ProcessBuilder("/usr/bin/true").start();
                      process.waitFor();
                    });
                    System.out.print(
                        "read=" + readDenied +
                        " write=" + writeDenied +
                        " network=" + networkDenied +
                        " process=" + processDenied
                    );
                    if (!(readDenied && writeDenied && networkDenied && processDenied)) {
                      System.exit(23);
                    }
                  }
                }
                """.trimIndent() +
                    "\n",
            )

            val compiler =
                requireNotNull(
                    ToolProvider.getSystemJavaCompiler(),
                ) {
                    "Mac sandbox proof requires a full Java 21 compiler"
                }
            val compiled =
                compiler.run(
                    null,
                    null,
                    null,
                    "--release",
                    "21",
                    "-proc:none",
                    "-d",
                    classes.toString(),
                    source.toString(),
                )
            require(
                compiled ==
                    0,
            ) {
                "Mac sandbox proof helper did not compile"
            }

            val profile =
                VexLifeWorkspaceJavaTest.sandboxProfile(
                    imageRoot =
                        imageRoot,
                    scratch =
                        scratch,
                    javaPath =
                        Path.of(
                            runtime.javaPath,
                        ),
                )
            require(
                !profile.contains(
                    "(subpath \"" +
                        parent +
                        "\")",
                ) &&
                    profile.contains(
                        "(subpath \"" +
                            scratch +
                            "\")",
                    ),
            ) {
                "Mac sandbox proof profile widened beyond scratch"
            }

            val result =
                VexLifeWorkspaceJavaTest.SystemRunner.run(
                    VexLifeWorkspaceJavaTest.ProcessSpec(
                        program =
                            runtime.sandboxPath,
                        arguments =
                            listOf(
                                "-p",
                                profile,
                                runtime.javaPath,
                                "-Dfile.encoding=UTF-8",
                                "-XX:-UsePerfData",
                                "-Xmx64m",
                                "-XX:ActiveProcessorCount=1",
                                "-cp",
                                classes.toString(),
                                "SandboxProbe",
                                outside.toString(),
                            ),
                        workingDirectory =
                            scratch,
                        environment =
                            emptyMap(),
                        timeoutMs =
                            20_000,
                        maximumStdoutBytes =
                            16_384,
                        maximumStderrBytes =
                            16_384,
                    ),
                )

            require(
                result.started &&
                    !result.timedOut &&
                    !result.stdoutTruncated &&
                    !result.stderrTruncated &&
                    result.exitCode ==
                    0 &&
                    result.stdout ==
                    "read=true write=true network=true process=true"
            ) {
                "macOS sandbox did not deny every synthetic escape: " +
                    "exit=" +
                    result.exitCode +
                    " stdout=" +
                    result.stdout +
                    " stderr=" +
                    result.stderr
            }
            require(
                Files.readString(
                    outside,
                ) ==
                    "outside sentinel",
            ) {
                "macOS sandbox allowed the synthetic outside write"
            }
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }

    private fun sha256(
        path: Path,
    ): String =
        MessageDigest
            .getInstance(
                "SHA-256",
            )
            .digest(
                Files.readAllBytes(
                    path,
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

fun main(args: Array<String>) {
    require(
        args.size ==
            2,
    ) {
        "usage: <private-service-image-root> <source-sha>"
    }
    VexLifeWorkspaceJavaTestMacProbe.run(
        imageRoot =
            Path.of(
                args[0],
            )
                .toAbsolutePath()
                .normalize(),
        sourceSha =
            args[1],
    )
    println(
        "VEXLIFE_WORKSPACE_JAVA_TEST_MAC_PROBE_PASS",
    )
}

// [VXG RealForever]
