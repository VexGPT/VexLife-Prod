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
    private const val FullMode =
        "FULL"

    private const val SubstrateMatrixMode =
        "SUBSTRATE_MATRIX"

    fun run(
        imageRoot: Path,
        sourceSha: String,
        mode: String = FullMode,
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

        when (
            mode
        ) {
            SubstrateMatrixMode -> {
                proveSandboxSubstrateMatrix(
                    runtime =
                        runtime,
                )
                return
            }

            FullMode -> Unit

            else ->
                error(
                    "unsupported C3-R1 Mac proof mode: $mode",
                )
        }

        proveSandboxSubstrate(
            runtime =
                runtime,
        )
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

    private fun proveSandboxSubstrate(
        runtime: VexLifeWorkspaceJavaTest.RuntimeObservation,
    ) {
        val scratch =
            Files.createTempDirectory(
                "vexlife-workspace-java-test-mac-substrate-",
            )
                .toRealPath()
        try {
            val canary =
                Path.of(
                    "/usr/bin/true",
                )
                    .toRealPath()
            val commonProfile =
                """
                |(version 1)
                |(deny default)
                |(allow process-fork)
                |(allow signal (target self))
                |(allow sysctl-read)
                |(allow file-read-metadata)
                |(allow process-exec (literal "${canary}"))
                """.trimMargin()
            val globalProfile =
                commonProfile +
                    "\n(allow file-read*)\n"
            val narrowProfile =
                commonProfile +
                    """
                    |
                    |(allow file-read*
                    |  (subpath "/usr")
                    |  (subpath "/System")
                    |  (literal "/dev/null")
                    |  (literal "/dev/random")
                    |  (literal "/dev/urandom"))
                    """.trimMargin() +
                    "\n"

            fun run(
                profile: String,
            ): VexLifeWorkspaceJavaTest.ProcessResult =
                VexLifeWorkspaceJavaTest.SystemRunner.run(
                    VexLifeWorkspaceJavaTest.ProcessSpec(
                        program =
                            runtime.sandboxPath,
                        arguments =
                            listOf(
                                "-p",
                                profile,
                                canary.toString(),
                            ),
                        workingDirectory =
                            scratch,
                        environment =
                            emptyMap(),
                        timeoutMs =
                            5_000,
                        maximumStdoutBytes =
                            4_096,
                        maximumStderrBytes =
                            4_096,
                    ),
                )

            val global =
                run(
                    globalProfile,
                )
            val narrow =
                run(
                    narrowProfile,
                )
            val osVersion =
                System.getProperty(
                    "os.version",
                )

            require(
                global.started &&
                    !global.timedOut &&
                    global.exitCode ==
                    0
            ) {
                "macOS sandbox substrate global-read canary failed: " +
                    "osVersion=" +
                    osVersion +
                    " exit=" +
                    global.exitCode +
                    " stdout=" +
                    global.stdout +
                    " stderr=" +
                    global.stderr
            }
            require(
                narrow.started &&
                    !narrow.timedOut &&
                    narrow.exitCode ==
                    0
            ) {
                "macOS sandbox substrate narrow-read canary failed: " +
                    "osVersion=" +
                    osVersion +
                    " globalExit=" +
                    global.exitCode +
                    " narrowExit=" +
                    narrow.exitCode +
                    " stdout=" +
                    narrow.stdout +
                    " stderr=" +
                    narrow.stderr
            }
        } finally {
            scratch
                .toFile()
                .deleteRecursively()
        }
    }


    private fun proveSandboxSubstrateMatrix(
        runtime: VexLifeWorkspaceJavaTest.RuntimeObservation,
    ) {
        val parent =
            Files.createTempDirectory(
                "vexlife-workspace-java-test-mac-platform-matrix-",
            )
                .toRealPath()
        val scratch =
            Files.createDirectories(
                parent.resolve(
                    "scratch",
                ),
            )
        val outside =
            parent.resolve(
                "outside-person-sentinel.txt",
            )
        val outsideWrite =
            parent.resolve(
                "outside-write.txt",
            )
        Files.writeString(
            outside,
            "OUTSIDE_PERSON_SENTINEL",
        )

        try {
            fun executable(
                value: String,
            ): Path =
                Path.of(
                    value,
                )
                    .toRealPath()
                    .also {
                        require(
                            Files.isExecutable(
                                it,
                            ),
                        ) {
                            "required A012 canary executable unavailable: $it"
                        }
                    }

            val trueCanary =
                executable(
                    "/usr/bin/true",
                )
            val catCanary =
                executable(
                    "/bin/cat",
                )
            val touchCanary =
                executable(
                    "/usr/bin/touch",
                )
            val execParentCanary =
                executable(
                    "/usr/bin/env",
                )
            val idCanary =
                executable(
                    "/usr/bin/id",
                )
            val networkCanary =
                executable(
                    "/usr/bin/nc",
                )

            fun literal(
                path: Path,
            ): String =
                path
                    .toString()
                    .replace(
                        "\\",
                        "\\\\",
                    )
                    .replace(
                        "\"",
                        "\\\"",
                    )

            val scratchLiteral =
                literal(
                    scratch,
                )
            val canaryReadRules =
                listOf(
                    trueCanary,
                    catCanary,
                    touchCanary,
                    execParentCanary,
                    networkCanary,
                )
                    .joinToString(
                        "\n",
                    ) {
                        "(allow file-read* (literal \"" +
                            literal(
                                it,
                            ) +
                            "\"))"
                    }
            val canaryExecRules =
                listOf(
                    trueCanary,
                    catCanary,
                    touchCanary,
                    execParentCanary,
                    networkCanary,
                )
                    .joinToString(
                        "\n",
                    ) {
                        "(allow process-exec (literal \"" +
                            literal(
                                it,
                            ) +
                            "\"))"
                    }

            val commonProfile =
                """
                |(version 1)
                |(deny default)
                |(allow process-fork)
                |(allow signal (target self))
                |(allow sysctl-read)
                |(allow file-read-metadata)
                |$canaryExecRules
                |$canaryReadRules
                |(allow file-read* file-write* (subpath "$scratchLiteral"))
                |(allow file-read*
                |  (subpath "/usr")
                |  (subpath "/System")
                |  (literal "/dev/null")
                |  (literal "/dev/random")
                |  (literal "/dev/urandom"))
                |(allow file-write* (literal "/dev/null"))
                """.trimMargin() +
                    "\n"

            val executableMapping =
                """
                |(allow file-map-executable
                |  (subpath "/Library/Apple/System/Library/Frameworks")
                |  (subpath "/Library/Apple/System/Library/PrivateFrameworks")
                |  (subpath "/Library/Apple/usr/lib")
                |  (subpath "/System/Library/Extensions")
                |  (subpath "/System/Library/Frameworks")
                |  (subpath "/System/Library/PrivateFrameworks")
                |  (subpath "/System/Library/SubFrameworks")
                |  (subpath "/System/iOSSupport/System/Library/Frameworks")
                |  (subpath "/System/iOSSupport/System/Library/PrivateFrameworks")
                |  (subpath "/System/iOSSupport/System/Library/SubFrameworks")
                |  (subpath "/usr/lib"))
                |(allow file-read* file-test-existence
                |  (subpath "/Library/Apple/System/Library/Frameworks")
                |  (subpath "/Library/Apple/System/Library/PrivateFrameworks")
                |  (subpath "/Library/Apple/usr/lib")
                |  (subpath "/System/Library/Frameworks")
                |  (subpath "/System/Library/PrivateFrameworks")
                |  (subpath "/System/Library/SubFrameworks")
                |  (subpath "/System/iOSSupport/System/Library/Frameworks")
                |  (subpath "/System/iOSSupport/System/Library/PrivateFrameworks")
                |  (subpath "/System/iOSSupport/System/Library/SubFrameworks")
                |  (subpath "/usr/lib"))
                """.trimMargin() +
                    "\n"

            val platformFilesystem =
                """
                |(allow file-read* file-test-existence
                |  (subpath "/Library/Apple")
                |  (subpath "/Library/Filesystems/NetFSPlugins")
                |  (subpath "/Library/Preferences/Logging")
                |  (subpath "/private/var/db/DarwinDirectory/local/recordStore.data")
                |  (subpath "/private/var/db/timezone")
                |  (subpath "/usr/lib")
                |  (subpath "/usr/share")
                |  (subpath "/Library/Preferences")
                |  (subpath "/var/db")
                |  (subpath "/private/var/db"))
                |(allow file-read-metadata file-test-existence
                |  (literal "/etc")
                |  (literal "/tmp")
                |  (literal "/var")
                |  (literal "/private/etc/localtime"))
                |(allow file-read-metadata file-test-existence
                |  (path-ancestors "/System/Volumes/Data/private"))
                |(allow file-read* file-test-existence
                |  (literal "/"))
                |(allow file-read*
                |  (subpath "/etc"))
                |(allow file-read*
                |  (subpath "/private/etc"))
                |(allow file-read-metadata
                |  (subpath "/var"))
                |(allow file-read-metadata
                |  (subpath "/private/var"))
                |(allow file-read* file-test-existence
                |  (literal "/dev/autofs_nowait")
                |  (literal "/dev/random")
                |  (literal "/dev/urandom")
                |  (literal "/private/etc/master.passwd")
                |  (literal "/private/etc/passwd")
                |  (literal "/private/etc/protocols")
                |  (literal "/private/etc/services"))
                |(allow file-read* file-test-existence file-write-data
                |  (literal "/dev/null")
                |  (literal "/dev/zero"))
                """.trimMargin() +
                    "\n"

            val platformRuntime =
                """
                |(allow process-info* (target same-sandbox))
                |(allow signal (target same-sandbox))
                |(allow system-mac-syscall (mac-policy-name "vnguard"))
                |(allow system-mac-syscall
                |  (require-all
                |    (mac-policy-name "Sandbox")
                |    (mac-syscall-number 67)))
                |(allow system-fsctl (fsctl-command FSIOC_CAS_BSDFLAGS))
                |(allow sysctl-write
                |  (sysctl-name "kern.grade_cputype"))
                |(allow iokit-open
                |  (iokit-registry-entry-class "RootDomainUserClient"))
                |(allow mach-lookup
                |  (global-name "com.apple.system.opendirectoryd.libinfo"))
                |(allow mach-lookup
                |  (global-name "com.apple.PowerManagement.control"))
                |(allow ipc-posix-sem)
                """.trimMargin() +
                    "\n"

            data class Candidate(
                val name: String,
                val profile: String,
            )

            val candidates =
                listOf(
                    Candidate(
                        name =
                            "M1_NARROW_CONTROL",
                        profile =
                            commonProfile,
                    ),
                    Candidate(
                        name =
                            "M2_EXECUTABLE_MAPPING",
                        profile =
                            commonProfile +
                                executableMapping,
                    ),
                    Candidate(
                        name =
                            "M3_PLATFORM_FILESYSTEM",
                        profile =
                            commonProfile +
                                executableMapping +
                                platformFilesystem,
                    ),
                    Candidate(
                        name =
                            "M4_PLATFORM_RUNTIME",
                        profile =
                            commonProfile +
                                executableMapping +
                                platformFilesystem +
                                platformRuntime,
                    ),
                )

            fun run(
                profile: String,
                program: Path,
                arguments: List<String> =
                    emptyList(),
                timeoutMs: Long =
                    5_000,
            ): VexLifeWorkspaceJavaTest.ProcessResult =
                VexLifeWorkspaceJavaTest.SystemRunner.run(
                    VexLifeWorkspaceJavaTest.ProcessSpec(
                        program =
                            runtime.sandboxPath,
                        arguments =
                            listOf(
                                "-p",
                                profile,
                                program.toString(),
                            ) +
                                arguments,
                        workingDirectory =
                            scratch,
                        environment =
                            emptyMap(),
                        timeoutMs =
                            timeoutMs,
                        maximumStdoutBytes =
                            8_192,
                        maximumStderrBytes =
                            8_192,
                    ),
                )

            fun startedNormally(
                result: VexLifeWorkspaceJavaTest.ProcessResult,
            ): Boolean =
                result.started &&
                    !result.timedOut &&
                    result.exitCode ==
                    0

            fun deniedWithoutAbort(
                result: VexLifeWorkspaceJavaTest.ProcessResult,
            ): Boolean =
                result.started &&
                    !result.timedOut &&
                    result.exitCode !=
                    null &&
                    result.exitCode !=
                    0 &&
                    result.exitCode !=
                    134

            data class SafetyResult(
                val outsideReadDenied: Boolean,
                val outsideWriteDenied: Boolean,
                val childControlPass: Boolean,
                val childDenied: Boolean,
                val networkControlPass: Boolean,
                val networkDenied: Boolean,
            ) {
                val safe: Boolean
                    get() =
                        outsideReadDenied &&
                            outsideWriteDenied &&
                            childControlPass &&
                            childDenied &&
                            networkControlPass &&
                            networkDenied

                fun render(): String =
                    "readDenied=" +
                        outsideReadDenied +
                        ",writeDenied=" +
                        outsideWriteDenied +
                        ",childControl=" +
                        childControlPass +
                        ",childDenied=" +
                        childDenied +
                        ",networkControl=" +
                        networkControlPass +
                        ",networkDenied=" +
                        networkDenied
            }

            fun safetyResult(
                candidate: Candidate,
            ): SafetyResult {
                val read =
                    run(
                        candidate.profile,
                        catCanary,
                        listOf(
                            outside.toString(),
                        ),
                    )
                Files.deleteIfExists(
                    outsideWrite,
                )
                val write =
                    run(
                        candidate.profile,
                        touchCanary,
                        listOf(
                            outsideWrite.toString(),
                        ),
                    )
                val childControl =
                    run(
                        candidate.profile,
                        execParentCanary,
                    )
                val child =
                    run(
                        candidate.profile,
                        execParentCanary,
                        listOf(
                            idCanary.toString(),
                        ),
                    )
                val localNetworkControlProfile =
                    candidate.profile +
                        """
                        |(allow network-bind (local ip "localhost:*"))
                        |(allow network-inbound (local ip "localhost:*"))
                        |(allow system-socket
                        |  (require-all
                        |    (socket-domain AF_SYSTEM)
                        |    (socket-protocol 2)))
                        |(allow mach-lookup
                        |  (global-name "com.apple.bsd.dirhelper")
                        |  (global-name "com.apple.system.opendirectoryd.membership")
                        |  (global-name "com.apple.SecurityServer")
                        |  (global-name "com.apple.networkd")
                        |  (global-name "com.apple.ocspd")
                        |  (global-name "com.apple.trustd.agent")
                        |  (global-name "com.apple.SystemConfiguration.DNSConfiguration")
                        |  (global-name "com.apple.SystemConfiguration.configd"))
                        |(allow sysctl-read
                        |  (sysctl-name-regex #"^net.routetable"))
                        """.trimMargin() +
                        "\n"
                val networkControl =
                    run(
                        localNetworkControlProfile,
                        networkCanary,
                        listOf(
                            "-n",
                            "-l",
                            "127.0.0.1",
                            "54321",
                        ),
                        timeoutMs =
                            750,
                    )
                val network =
                    run(
                        candidate.profile,
                        networkCanary,
                        listOf(
                            "-n",
                            "-l",
                            "127.0.0.1",
                            "54321",
                        ),
                        timeoutMs =
                            1_500,
                    )

                return SafetyResult(
                    outsideReadDenied =
                        deniedWithoutAbort(
                            read,
                        ) &&
                            !read.stdout.contains(
                                "OUTSIDE_PERSON_SENTINEL",
                            ),
                    outsideWriteDenied =
                        deniedWithoutAbort(
                            write,
                        ) &&
                            !Files.exists(
                                outsideWrite,
                            ),
                    childControlPass =
                        startedNormally(
                            childControl,
                        ),
                    childDenied =
                        deniedWithoutAbort(
                            child,
                        ),
                    networkControlPass =
                        networkControl.started &&
                            networkControl.timedOut,
                    networkDenied =
                        deniedWithoutAbort(
                            network,
                        ),
                )
            }

            val globalProfile =
                commonProfile +
                    "(allow file-read*)\n"
            val global =
                run(
                    globalProfile,
                    trueCanary,
                )
            require(
                startedNormally(
                    global,
                )
            ) {
                "A012 global-read control failed: osVersion=" +
                    System.getProperty(
                        "os.version",
                    ) +
                    " exit=" +
                    global.exitCode +
                    " stdout=" +
                    global.stdout +
                    " stderr=" +
                    global.stderr
            }

            data class Observation(
                val candidate: Candidate,
                val startup: VexLifeWorkspaceJavaTest.ProcessResult,
                val safety: SafetyResult?,
            ) {
                val safe: Boolean
                    get() =
                        startedNormally(
                            startup,
                        ) &&
                            safety?.safe ==
                            true
            }

            val observations =
                candidates.map {
                    candidate ->
                    val startup =
                        run(
                            candidate.profile,
                            trueCanary,
                        )
                    Observation(
                        candidate =
                            candidate,
                        startup =
                            startup,
                        safety =
                            if (
                                startedNormally(
                                    startup,
                                )
                            ) {
                                safetyResult(
                                    candidate,
                                )
                            } else {
                                null
                            },
                    )
                }
            val selected =
                observations.firstOrNull {
                    it.safe
                }

            fun Observation.render(): String =
                candidate.name +
                    "{exit=" +
                    startup.exitCode +
                    ",timedOut=" +
                    startup.timedOut +
                    ",safe=" +
                    safe +
                    ",predicates=" +
                    (
                        safety?.render() ?:
                            "NOT_RUN"
                    ) +
                    ",stderr=" +
                    startup.stderr +
                    "}"

            requireNotNull(
                selected,
            ) {
                "A012 no safe platform-runtime baseline: osVersion=" +
                    System.getProperty(
                        "os.version",
                    ) +
                    " observations=" +
                    observations.joinToString(
                        ";",
                    ) {
                        it.render()
                    }
            }

            println(
                "VEXLIFE_WORKSPACE_JAVA_TEST_MAC_SUBSTRATE_MATRIX_PASS " +
                    "osVersion=" +
                    System.getProperty(
                        "os.version",
                    ) +
                    " selected=" +
                    selected.candidate.name +
                    " observations=" +
                    observations.joinToString(
                        ";",
                    ) {
                        it.render()
                    },
            )
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
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
                "workspace Java-test Mac owner proof receipt mismatch: " +
                    "disposition=" + receipt.disposition +
                    " exit=" + receipt.testExitCode +
                    " stdout=" + receipt.testStdout +
                    " stderr=" + receipt.testStderr +
                    " cleanup=" + receipt.cleanupComplete +
                    " currentEffectAuthority=" + receipt.currentEffectAuthority +
                    " sandboxMatches=" + (receipt.sandboxSha256 == runtime.sandboxSha256)
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
        args.size in
            2..3,
    ) {
        "usage: <private-service-image-root> <source-sha> [FULL|SUBSTRATE_MATRIX]"
    }
    val mode =
        args.getOrNull(
            2,
        ) ?: "FULL"
    VexLifeWorkspaceJavaTestMacProbe.run(
        imageRoot =
            Path.of(
                args[0],
            )
                .toAbsolutePath()
                .normalize(),
        sourceSha =
            args[1],
        mode =
            mode,
    )
    println(
        "VEXLIFE_WORKSPACE_JAVA_TEST_MAC_PROBE_PASS mode=$mode",
    )
}

// [VXG RealForever]
