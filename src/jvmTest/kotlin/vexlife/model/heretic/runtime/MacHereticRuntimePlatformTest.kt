package vexlife.model.heretic.runtime

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import vexlife.model.heretic.HereticProductIdentity
import vexlife.platform.macos.MacUserLaunchdProcess

class MacHereticRuntimePlatformTest {
    private class LaunchdHost :
        MacUserLaunchdProcess.Host {
        override val uid = 501L
        var present = false
        var running = false
        var pid = 5151L
        var observedPlist = ""
        var observedPlistPath = ""

        override fun launchctl(
            args: List<String>,
        ): MacUserLaunchdProcess.CommandResult =
            when (args.first()) {
                "print" ->
                    if (!present) {
                        MacUserLaunchdProcess.CommandResult(
                            113,
                            "",
                            "Could not find service",
                        )
                    } else {
                        MacUserLaunchdProcess.CommandResult(
                            0,
                            if (running) "pid = $pid\n" else "state = exited\n",
                            "",
                        )
                    }

                "bootstrap" -> {
                    observedPlistPath =
                        args[2]
                    observedPlist =
                        Files.readString(
                            java.nio.file.Path.of(
                                args[2],
                            ),
                        )
                    present = true
                    running = true
                    MacUserLaunchdProcess.CommandResult(
                        0,
                        "",
                        "",
                    )
                }

                "bootout" -> {
                    present = false
                    running = false
                    MacUserLaunchdProcess.CommandResult(
                        0,
                        "",
                        "",
                    )
                }

                else ->
                    error(
                        "unexpected launchctl args: $args",
                    )
            }

        override fun processAlive(
            pid: Long,
        ): Boolean =
            present &&
                running &&
                pid ==
                this.pid
    }

    private class Inspector :
        MacHereticRuntimePlatform.ProcessInspector {
        val alive = mutableSetOf<Long>()
        var occupied = false

        override fun alive(
            pid: Long,
        ): Boolean =
            pid in
                alive

        override fun identity(
            pid: Long,
            args: List<String>,
        ): HereticPersistentRuntime.ProcessIdentity? =
            if (
                pid in
                    alive
            ) {
                HereticPersistentRuntime.ProcessIdentity(
                    pid = pid,
                    processGroupId = pid,
                    exactOwnedDetachedGroup = true,
                )
            } else {
                null
            }

        override fun matching(
            args: List<String>,
        ): List<HereticPersistentRuntime.ProcessIdentity> =
            emptyList()

        override fun portOccupied(
            port: Int,
        ): Boolean =
            occupied
    }

    private class Runner :
        HereticPythonEnvironment.Runner {
        val calls =
            mutableListOf<Pair<String, List<String>>>()

        override fun run(
            program: String,
            args: List<String>,
            timeoutMs: Long,
        ): HereticPythonEnvironment.CommandResult {
            calls +=
                program to
                    args

            return if (
                args.firstOrNull() ==
                    "-c" &&
                    args.size ==
                    2
            ) {
                val python =
                    java.nio.file.Path.of(
                        program,
                    )
                val root =
                    python.parent.parent

                HereticPythonEnvironment.CommandResult(
                    status = 0,
                    stdout =
                        HereticProductIdentity.PythonVersion +
                            "\n" +
                            HereticProductIdentity.MlxLmVersion +
                            "\n" +
                            root +
                            "\n" +
                            python +
                            "\n",
                    stderr = "",
                )
            } else {
                HereticPythonEnvironment.CommandResult(
                    status = 0,
                    stdout = "QUALIFIED\n",
                    stderr = "",
                )
            }
        }
    }

    private fun binding(
        root: java.nio.file.Path,
    ): HereticPersistentRuntime.Binding {
        val environment =
            HereticPythonEnvironment.environmentRoot(
                root,
            )
        val python =
            HereticPythonEnvironment.pythonPath(
                environment,
            )

        Files.createDirectories(
            python.parent,
        )
        Files.writeString(
            python,
            "fake",
        )
        Files.createDirectory(
            root.resolve(
                "model",
            ),
        )
        Files.writeString(
            environment.resolve(
                HereticPythonEnvironment.InstallReportFilename,
            ),
            "{}",
        )

        return HereticPersistentRuntime.bind(
            authorityRef = "authority.vexlife-prod.heretic-runtime.test",
            materialization =
                HereticArtifactMaterialization.Result(
                    state = "HERETIC_ARTIFACTS_MATERIALIZED_EXACT",
                    modelPath =
                        root
                            .resolve(
                                "model",
                            )
                            .toString(),
                    providerRevision = "8".repeat(40),
                    contentSetSha256 = HereticProductIdentity.ArtifactContentSetSha256,
                    memberCount = 11,
                    totalBytes = 15_153_242_506L,
                    reusedMemberCount = 11,
                    acquiredMemberCount = 0,
                    acquisitionPerformed = false,
                    artifactMutationPerformed = false,
                ),
            environment =
                HereticPythonEnvironment.Result(
                    state = "REUSED_VERIFIED",
                    pythonEnvironmentRoot = environment.toString(),
                    pythonPath = python.toString(),
                    pythonVersion = HereticProductIdentity.PythonVersion,
                    mlxLmVersion = HereticProductIdentity.MlxLmVersion,
                    pipReportPath =
                        environment
                            .resolve(
                                HereticPythonEnvironment.InstallReportFilename,
                            )
                            .toString(),
                    environmentMutationPerformed = false,
                ),
        )
    }

    @Test
    fun freshEnvironmentObservationUsesTheBoundPythonWithoutRepair() {
        val root =
            Files.createTempDirectory(
                "vexlife-prod-mac-heretic-env-",
            )
                .toAbsolutePath()
        val runner =
            Runner()
        val platform =
            MacHereticRuntimePlatform(
                productHome = root,
                launchdHost =
                    LaunchdHost(),
                processInspector =
                    Inspector(),
                commandRunner =
                    runner,
                healthProbe =
                    MacHereticRuntimePlatform.HealthProbe {
                        true
                    },
            )

        try {
            val current =
                binding(
                    root,
                )

            assertTrue(
                platform.environmentCurrent(
                    current,
                ),
            )
            assertEquals(
                current.pythonPath,
                runner.calls
                    .single()
                    .first,
            )
        } finally {
            root
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun spawnAndProvisionalRetireUseTheGenericLaunchdSubstrate() {
        val root =
            Files.createTempDirectory(
                "vexlife-prod-mac-heretic-spawn-",
            )
                .toAbsolutePath()
        val host =
            LaunchdHost()
        val inspector =
            Inspector()
        val platform =
            MacHereticRuntimePlatform(
                productHome = root,
                launchdHost =
                    host,
                processInspector =
                    inspector,
                commandRunner =
                    Runner(),
                healthProbe =
                    MacHereticRuntimePlatform.HealthProbe {
                        true
                    },
            )

        try {
            val current =
                binding(
                    root,
                )
            val args =
                HereticPersistentRuntime.serverArgs(
                    current,
                )
            val spawned =
                platform.spawn(
                    current,
                    args,
                )

            inspector.alive +=
                spawned.pid

            assertEquals(
                host.pid,
                spawned.pid,
            )
            assertTrue(
                host.observedPlist.contains(
                    "<string>" +
                        current.pythonPath +
                        "</string>",
                ),
            )
            assertTrue(
                host.observedPlist.contains(
                    "<string>mlx_lm.server</string>",
                ),
            )
            assertTrue(
                host.observedPlist.contains(
                    "<string>com.vexlife.prod.heretic.",
                ),
            )
            val productRuntimeRoot =
                root.resolve(
                    "runtime/vexlife-prod",
                )
            assertTrue(
                java.nio.file.Path.of(
                    host.observedPlistPath,
                )
                    .startsWith(
                        productRuntimeRoot.resolve(
                            "launchd",
                        ),
                    ),
            )
            val huggingFaceCache =
                productRuntimeRoot.resolve(
                    "huggingface-cache",
                )
            assertTrue(
                Files.isDirectory(
                    huggingFaceCache,
                ),
            )
            assertTrue(
                host.observedPlist.contains(
                    "<key>HF_HUB_CACHE</key>",
                ),
            )
            assertTrue(
                host.observedPlist.contains(
                    "<string>" +
                        huggingFaceCache +
                        "</string>",
                ),
            )
            assertFalse(
                host.observedPlist.contains(
                    "<string>com.vexlife.heretic.",
                ),
            )
            assertTrue(
                platform.processSubstrateCurrent(
                    spawned.processSubstrate,
                    spawned.pid,
                ),
            )

            val retired =
                platform.retireProvisional(
                    spawned,
                    args,
                )

            assertTrue(
                retired.exactOwnershipVerified,
            )
            assertTrue(
                retired.verifiedNotLive,
            )
            assertFalse(
                host.present,
            )
        } finally {
            root
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun symlinkedRuntimeParentCannotRedirectLaunchdFilesOutsideProdHome() {
        val parent =
            Files.createTempDirectory(
                "vexlife-prod-mac-heretic-runtime-link-",
            )
                .toAbsolutePath()
        val root =
            parent.resolve(
                "VexLife-Prod",
            )
        val outside =
            parent.resolve(
                "outside",
            )
        val host =
            LaunchdHost()

        Files.createDirectory(
            root,
        )
        Files.createDirectory(
            outside,
        )
        val current =
            binding(
                root,
            )

        // The binding fixture must be formed while the runtime tree is still owned by Home.
        // Only then introduce the hostile alias so this test measures spawn containment rather
        // than allowing fixture construction itself to follow the symlink.
        root.resolve(
            "runtime",
        )
            .toFile()
            .deleteRecursively()
        Files.createSymbolicLink(
            root.resolve(
                "runtime",
            ),
            outside,
        )

        val platform =
            MacHereticRuntimePlatform(
                productHome = root,
                launchdHost = host,
                processInspector = Inspector(),
                commandRunner = Runner(),
                healthProbe =
                    MacHereticRuntimePlatform.HealthProbe {
                        true
                    },
            )

        try {
            val args =
                HereticPersistentRuntime.serverArgs(
                    current,
                )

            assertFailsWith<IllegalArgumentException> {
                platform.spawn(
                    current,
                    args,
                )
            }
            assertFalse(
                host.present,
            )
            assertFalse(
                Files.exists(
                    outside.resolve(
                        "vexlife-prod",
                    ),
                ),
            )
        } finally {
            Files.deleteIfExists(
                root.resolve(
                    "runtime",
                ),
            )
            parent
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun qualificationUsesTheBoundPythonAndCurrentThreePartProbe() {
        val root =
            Files.createTempDirectory(
                "vexlife-prod-mac-heretic-qualify-",
            )
                .toAbsolutePath()
        val runner =
            Runner()
        val platform =
            MacHereticRuntimePlatform(
                productHome = root,
                launchdHost =
                    LaunchdHost(),
                processInspector =
                    Inspector(),
                commandRunner =
                    runner,
                healthProbe =
                    MacHereticRuntimePlatform.HealthProbe {
                        true
                    },
            )

        try {
            val current =
                binding(
                    root,
                )
            val qualification =
                platform.qualify(
                    current,
                )
            val call =
                runner.calls.single()

            assertEquals(
                current.pythonPath,
                call.first,
            )
            assertTrue(
                call.second.contains(
                    HereticProductIdentity.Endpoint,
                ),
            )
            assertTrue(
                call.second.contains(
                    current.modelPath,
                ),
            )
            assertTrue(
                call.second.contains(
                    HereticProductIdentity.RequestModel,
                ),
            )

            val script =
                call.second[
                    1
                ]

            for (
                required in
                listOf(
                    "/health",
                    "/v1/models",
                    "/v1/chat/completions",
                    "Reply with OK.",
                    "enable_thinking",
                    "non-JSON HTTP 200",
                    "content-type=",
                    "body=%r",
                )
            ) {
                assertTrue(
                    script.contains(
                        required,
                    ),
                    required,
                )
            }
            assertTrue(
                qualification.actualChatCompletionPerformed,
            )
            assertFalse(
                qualification.livedConversationEffect,
            )
        } finally {
            root
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun readinessRequiresLiveProcessAndExactHealth() {
        val root =
            Files.createTempDirectory(
                "vexlife-prod-mac-heretic-health-",
            )
                .toAbsolutePath()
        val inspector =
            Inspector()
        val healthCalls =
            mutableListOf<String>()
        val platform =
            MacHereticRuntimePlatform(
                productHome = root,
                launchdHost =
                    LaunchdHost(),
                processInspector =
                    inspector,
                commandRunner =
                    Runner(),
                healthProbe =
                    MacHereticRuntimePlatform.HealthProbe {
                        origin ->
                        healthCalls +=
                            origin
                        true
                    },
            )

        try {
            val current =
                binding(
                    root,
                )
            inspector.alive +=
                5151

            platform.waitUntilHealthy(
                current,
                5151,
            )

            assertEquals(
                listOf(
                    HereticProductIdentity.Endpoint,
                ),
                healthCalls,
            )
        } finally {
            root
                .toFile()
                .deleteRecursively()
        }
    }
}

// [VXG RealForever]
