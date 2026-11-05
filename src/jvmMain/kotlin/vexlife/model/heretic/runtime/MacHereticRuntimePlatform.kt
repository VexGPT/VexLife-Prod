package vexlife.model.heretic.runtime

import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import java.nio.file.Path
import java.time.Instant
import java.util.UUID
import vexlife.model.heretic.HereticProductIdentity
import vexlife.model.heretic.HereticRuntimeQualification
import vexlife.platform.macos.MacUserLaunchdProcess

/**
 * macOS physical realization for [HereticPersistentRuntime.Platform].
 *
 * Semantic runtime ownership remains in [HereticPersistentRuntime]. This class only answers the
 * delegated host questions: process census, launchd lifetime, port/health observation and exact
 * MLX qualification effects.
 */
class MacHereticRuntimePlatform(
    runtimeRoot: Path,
    private val launchdHost: MacUserLaunchdProcess.Host =
        MacUserLaunchdProcess.SystemHost,
    private val processInspector: ProcessInspector =
        SystemProcessInspector,
    private val commandRunner: HereticPythonEnvironment.Runner =
        HereticPythonEnvironment.SystemRunner,
    private val healthProbe: HealthProbe =
        SystemHealthProbe,
    private val cancellation: () -> Boolean = {
        false
    },
    private val clock: () -> Instant = {
        Instant.now()
    },
) : HereticPersistentRuntime.Platform {
    private val runtimeRoot =
        runtimeRoot
            .also {
                require(
                    it.isAbsolute,
                ) {
                    "Heretic runtime root must be absolute"
                }
            }
            .normalize()

    override val platform: String =
        when (
            System.getProperty(
                "os.name",
            )
                ?.lowercase()
        ) {
            "mac os x",
            "macos",
            "darwin",
            ->
                "darwin"

            else ->
                System
                    .getProperty(
                        "os.name",
                    )
                    ?.lowercase()
                    ?: "unknown"
        }

    override val architecture: String =
        when (
            System.getProperty(
                "os.arch",
            )
                ?.lowercase()
        ) {
            "aarch64",
            "arm64",
            ->
                "arm64"

            else ->
                System
                    .getProperty(
                        "os.arch",
                    )
                    ?.lowercase()
                    ?: "unknown"
        }

    interface ProcessInspector {
        fun alive(
            pid: Long,
        ): Boolean

        fun identity(
            pid: Long,
            args: List<String>,
        ): HereticPersistentRuntime.ProcessIdentity?

        fun matching(
            args: List<String>,
        ): List<HereticPersistentRuntime.ProcessIdentity>

        fun portOccupied(
            port: Int,
        ): Boolean
    }

    fun interface HealthProbe {
        fun healthy(
            origin: String,
        ): Boolean
    }

    object SystemProcessInspector :
        ProcessInspector {
        override fun alive(
            pid: Long,
        ): Boolean =
            ProcessHandle
                .of(
                    pid,
                )
                .map(
                    ProcessHandle::isAlive,
                )
                .orElse(
                    false,
                )

        override fun identity(
            pid: Long,
            args: List<String>,
        ): HereticPersistentRuntime.ProcessIdentity? {
            if (
                !alive(
                    pid,
                )
            ) {
                return null
            }

            val result =
                command(
                    listOf(
                        "/bin/ps",
                        "-ww",
                        "-p",
                        pid.toString(),
                        "-o",
                        "pid=,pgid=,command=",
                    ),
                )

            if (
                result.status !=
                    0
            ) {
                return null
            }

            val match =
                Regex(
                    "^\\s*(\\d+)\\s+(\\d+)\\s+(.*)$",
                )
                    .find(
                        result.stdout
                            .trim(),
                    )
                    ?: return null

            val observedPid =
                match.groupValues[
                    1
                ]
                    .toLong()
            val processGroupId =
                match.groupValues[
                    2
                ]
                    .toLong()
            val commandLine =
                match.groupValues[
                    3
                ]
            val commandMatches =
                commandLine.endsWith(
                    " " +
                        args.joinToString(
                            " ",
                        ),
                )

            return HereticPersistentRuntime.ProcessIdentity(
                pid = observedPid,
                processGroupId = processGroupId,
                exactOwnedDetachedGroup =
                    observedPid ==
                        pid &&
                        processGroupId ==
                        pid &&
                        commandMatches,
            )
        }

        override fun matching(
            args: List<String>,
        ): List<HereticPersistentRuntime.ProcessIdentity> {
            val result =
                command(
                    listOf(
                        "/bin/ps",
                        "-ww",
                        "-axo",
                        "pid=,pgid=,command=",
                    ),
                )

            require(
                result.status ==
                    0,
            ) {
                "unable to inspect MLX process census"
            }

            val tail =
                " " +
                    args.joinToString(
                        " ",
                    )

            return result.stdout
                .lineSequence()
                .mapNotNull {
                    line ->
                    val match =
                        Regex(
                            "^\\s*(\\d+)\\s+(\\d+)\\s+(.*)$",
                        )
                            .find(
                                line,
                            )
                            ?: return@mapNotNull null

                    if (
                        !match.groupValues[
                            3
                        ].endsWith(
                            tail,
                        )
                    ) {
                        return@mapNotNull null
                    }

                    val pid =
                        match.groupValues[
                            1
                        ]
                            .toLong()
                    val pgid =
                        match.groupValues[
                            2
                        ]
                            .toLong()

                    HereticPersistentRuntime.ProcessIdentity(
                        pid = pid,
                        processGroupId = pgid,
                        exactOwnedDetachedGroup =
                            pid ==
                                pgid,
                    )
                }
                .toList()
        }

        override fun portOccupied(
            port: Int,
        ): Boolean =
            try {
                Socket()
                    .use {
                        socket ->
                        socket.connect(
                            InetSocketAddress(
                                "127.0.0.1",
                                port,
                            ),
                            700,
                        )
                    }
                true
            } catch (
                _: Exception,
            ) {
                false
            }

        private data class Result(
            val status: Int,
            val stdout: String,
        )

        private fun command(
            command: List<String>,
        ): Result =
            try {
                val process =
                    ProcessBuilder(
                        command,
                    )
                        .start()
                val stdout =
                    process.inputStream
                        .bufferedReader()
                        .readText()
                process.errorStream
                    .bufferedReader()
                    .readText()
                Result(
                    status =
                        process.waitFor(),
                    stdout = stdout,
                )
            } catch (
                _: Exception,
            ) {
                Result(
                    status = -1,
                    stdout = "",
                )
            }
    }

    object SystemHealthProbe :
        HealthProbe {
        private val statusPattern =
            Regex(
                "\"status\"\\s*:\\s*\"ok\"",
            )

        override fun healthy(
            origin: String,
        ): Boolean =
            try {
                val connection =
                    (
                        URL(
                            "$origin/health",
                        )
                            .openConnection() as
                            HttpURLConnection
                    )
                        .also {
                            it.connectTimeout =
                                1_500
                            it.readTimeout =
                                1_500
                            it.requestMethod =
                                "GET"
                            it.useCaches =
                                false
                        }

                try {
                    if (
                        connection.responseCode !=
                            200
                    ) {
                        return false
                    }

                    statusPattern.containsMatchIn(
                        connection.inputStream
                            .bufferedReader()
                            .readText(),
                    )
                } finally {
                    connection.disconnect()
                }
            } catch (
                _: Exception,
            ) {
                false
            }
    }

    override fun environmentCurrent(
        binding: HereticPersistentRuntime.Binding,
    ): Boolean =
        try {
            val observation =
                HereticPythonEnvironment.observeCurrent(
                    environmentRoot =
                        Path.of(
                            binding.pythonEnvironmentRoot,
                        ),
                    runner = commandRunner,
                )

            observation !=
                null &&
                observation.pythonPath ==
                Path.of(
                    binding.pythonPath,
                )
                    .normalize()
                    .toString()
        } catch (
            _: Exception,
        ) {
            false
        }

    override fun processAlive(
        pid: Long,
    ): Boolean =
        processInspector.alive(
            pid,
        )

    override fun processIdentity(
        pid: Long,
        args: List<String>,
    ): HereticPersistentRuntime.ProcessIdentity? =
        processInspector.identity(
            pid,
            args,
        )

    override fun processSubstrateCurrent(
        receipt: MacUserLaunchdProcess.Receipt,
        expectedPid: Long,
    ): Boolean =
        try {
            val current =
                MacUserLaunchdProcess.validate(
                    receipt,
                )
            val observed =
                MacUserLaunchdProcess.observe(
                    current.toSpec(),
                    launchdHost,
                )

            current.pid ==
                expectedPid &&
                observed is
                MacUserLaunchdProcess.Observation.Running &&
                observed.pid ==
                expectedPid
        } catch (
            _: Exception,
        ) {
            false
        }

    override fun matchingProcesses(
        args: List<String>,
    ): List<HereticPersistentRuntime.ProcessIdentity> =
        processInspector.matching(
            args,
        )

    override fun portOccupied(
        port: Int,
    ): Boolean =
        processInspector.portOccupied(
            port,
        )

    override fun spawn(
        binding: HereticPersistentRuntime.Binding,
        args: List<String>,
    ): HereticPersistentRuntime.Spawned {
        val launchId =
            UUID
                .randomUUID()
                .toString()
                .lowercase()
        val plistId =
            UUID
                .randomUUID()
                .toString()
                .lowercase()

        val spec =
            MacUserLaunchdProcess.form(
                label =
                    "com.vexlife.prod.heretic.$launchId",
                plistPath =
                    runtimeRoot
                        .resolve(
                            "launchd/heretic-$plistId.plist",
                        )
                        .toString(),
                programPath =
                    binding.pythonPath,
                programArguments =
                    args,
                workingDirectory =
                    binding.pythonEnvironmentRoot,
                stdoutPath =
                    runtimeRoot
                        .resolve(
                            "mlx-lm-server.out.log",
                        )
                        .toString(),
                stderrPath =
                    runtimeRoot
                        .resolve(
                            "mlx-lm-server.err.log",
                        )
                        .toString(),
                environment =
                    inheritedEnvironment(),
                uid =
                    launchdHost.uid,
            )

        val substrate =
            MacUserLaunchdProcess.start(
                spec,
                launchdHost,
            )

        return HereticPersistentRuntime.Spawned(
            pid = substrate.pid,
            processSubstrate = substrate,
        )
    }

    override fun retire(
        receipt: HereticPersistentRuntime.Receipt,
    ): HereticPersistentRuntime.Retirement =
        retireSubstrate(
            receipt.runtime.processSubstrate,
            receipt.runtime.pid,
        )

    override fun retireProvisional(
        spawned: HereticPersistentRuntime.Spawned,
        args: List<String>,
    ): HereticPersistentRuntime.Retirement =
        retireSubstrate(
            spawned.processSubstrate,
            spawned.pid,
        )

    override fun waitUntilHealthy(
        binding: HereticPersistentRuntime.Binding,
        pid: Long,
    ) {
        val deadline =
            nowMs() +
                600_000L

        while (
            nowMs() <
            deadline
        ) {
            if (
                cancellationRequested()
            ) {
                error(
                    "Heretic runtime start was cancelled",
                )
            }
            require(
                processAlive(
                    pid,
                ),
            ) {
                "mlx_lm.server exited before startup readiness"
            }

            if (
                healthProbe.healthy(
                    HereticProductIdentity.Endpoint,
                )
            ) {
                return
            }

            sleep(
                500,
            )
        }

        error(
            "mlx_lm.server did not become healthy before bounded startup timeout",
        )
    }

    override fun qualify(
        binding: HereticPersistentRuntime.Binding,
    ): HereticRuntimeQualification {
        val script =
            """
            import json, pathlib, sys, urllib.request

            origin, model_path, request_model = sys.argv[1:4]

            def read_json(request, timeout):
                with urllib.request.urlopen(request, timeout=timeout) as response:
                    if response.status != 200:
                        raise RuntimeError("HTTP %s" % response.status)
                    return json.loads(response.read().decode("utf-8"))

            health = read_json(origin + "/health", 5)
            if health.get("status") != "ok":
                raise RuntimeError("health is not exact ok")

            models = read_json(origin + "/v1/models", 5)
            ids = [
                entry.get("id")
                for entry in models.get("data", [])
                if isinstance(entry, dict) and isinstance(entry.get("id"), str)
            ]
            exact = [
                value
                for value in ids
                if pathlib.Path(value).resolve() == pathlib.Path(model_path).resolve()
            ]
            if len(exact) != 1:
                raise RuntimeError("exact model materialization is not exposed exactly once")

            body = json.dumps({
                "model": request_model,
                "messages": [{"role": "user", "content": "Reply with OK."}],
                "max_tokens": 1,
                "temperature": 0,
                "stream": False,
                "chat_template_kwargs": {"enable_thinking": False},
            }).encode("utf-8")
            request = urllib.request.Request(
                origin + "/v1/chat/completions",
                data=body,
                headers={"Content-Type": "application/json"},
                method="POST",
            )
            completion = read_json(request, 180)
            if completion.get("model") != request_model:
                raise RuntimeError("completion model does not match exact request model")
            choices = completion.get("choices")
            content = (
                choices[0].get("message", {}).get("content")
                if isinstance(choices, list) and choices and isinstance(choices[0], dict)
                else None
            )
            if not isinstance(content, str) or not content.strip():
                raise RuntimeError("qualification completion returned empty content")

            print("QUALIFIED")
            """.trimIndent()

        val result =
            commandRunner.run(
                program =
                    binding.pythonPath,
                args =
                    listOf(
                        "-c",
                        script,
                        HereticProductIdentity.Endpoint,
                        binding.modelPath,
                        HereticProductIdentity.RequestModel,
                    ),
                timeoutMs =
                    180_000L,
            )

        require(
            result.status ==
                0 &&
                result.errorMessage ==
                null &&
                !result.timedOut &&
                result.stdout
                    .trim() ==
                "QUALIFIED",
        ) {
            "Heretic MLX runtime qualification failed: " +
                (
                    result.errorMessage
                        ?: result.stderr
                            .take(
                                4_000,
                            )
                )
        }

        return HereticRuntimeQualification.qualified()
    }

    override fun cancellationRequested(): Boolean =
        cancellation()

    override fun now(): String =
        clock()
            .toString()

    private fun retireSubstrate(
        receipt: MacUserLaunchdProcess.Receipt,
        expectedPid: Long,
    ): HereticPersistentRuntime.Retirement =
        try {
            val retired =
                MacUserLaunchdProcess.retire(
                    receipt = receipt,
                    host = launchdHost,
                    expectedPid = expectedPid,
                )

            HereticPersistentRuntime.Retirement(
                exactOwnershipVerified =
                    retired.exactOwnershipVerified,
                verifiedNotLive =
                    retired.verifiedNotLive,
            )
        } catch (
            _: Exception,
        ) {
            HereticPersistentRuntime.Retirement(
                exactOwnershipVerified = false,
                verifiedNotLive =
                    !processAlive(
                        expectedPid,
                    ),
            )
        }

    private fun inheritedEnvironment(): Map<String, String> =
        listOf(
            "HOME",
            "PATH",
            "TMPDIR",
            "LANG",
            "LC_ALL",
            "USER",
            "HF_HUB_CACHE",
        )
            .mapNotNull {
                key ->
                System.getenv(
                    key,
                )
                    ?.takeIf(
                        String::isNotEmpty,
                    )
                    ?.let {
                        value ->
                        key to
                            value
                    }
            }
            .toMap()

    private fun MacUserLaunchdProcess.Receipt.toSpec(): MacUserLaunchdProcess.Spec =
        MacUserLaunchdProcess.form(
            label = label,
            plistPath = plistPath,
            programPath = programPath,
            programArguments = programArguments,
            workingDirectory = workingDirectory,
            stdoutPath = stdoutPath,
            stderrPath = stderrPath,
            environment = emptyMap(),
            uid =
                domainTarget
                    .substringAfter(
                        "gui/",
                    )
                    .toLong(),
        )
}

// [VXG RealForever]
