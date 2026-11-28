package vexlife.model.heretic.runtime

import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import java.nio.file.Path
import java.time.Instant
import java.util.Base64
import java.util.UUID
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import vexlife.capability.VexLifeCapabilityAtlas
import vexlife.capability.VexLifeCapabilityBridge
import vexlife.capability.VexLifeCapabilityRequest
import vexlife.continuity.VexLifeContinuity
import vexlife.filesystem.VexOwnedPath
import vexlife.model.heretic.HereticCompanionRepresentation
import vexlife.model.heretic.HereticProductIdentity
import vexlife.model.heretic.HereticRuntimeQualification
import vexlife.model.heretic.HereticTextControlCapabilityWire
import vexlife.platform.macos.MacUserLaunchdProcess

/**
 * macOS physical realization for [HereticPersistentRuntime.Platform].
 *
 * Semantic runtime ownership remains in [HereticPersistentRuntime]. This class only answers the
 * delegated host questions: process census, launchd lifetime, port/health observation and exact
 * MLX qualification effects.
 */
class MacHereticRuntimePlatform(
    productHome: Path,
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
    private val productHome =
        VexOwnedPath.requireRoot(
            productHome,
            "VexLife-Prod Home",
        )

    private val runtimeRoot =
        VexOwnedPath.resolveInside(
            this.productHome,
            VexLifeContinuity.ProductRuntimeRootRelativePath,
        )

    private val huggingFaceCacheRoot =
        runtimeRoot.resolve(
            "huggingface-cache",
        )

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
        fun observe(
            origin: String,
        ): HereticPersistentRuntime.GenerationReadiness
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
        private val okPattern =
            Regex(
                "\"status\"\\s*:\\s*\"ok\"",
            )
        private val unavailablePattern =
            Regex(
                "\"status\"\\s*:\\s*\"unavailable\"",
            )

        override fun observe(
            origin: String,
        ): HereticPersistentRuntime.GenerationReadiness =
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
                    val status =
                        connection.responseCode
                    val body =
                        (
                            if (
                                status in
                                    200..299
                            ) {
                                connection.inputStream
                            } else {
                                connection.errorStream
                            }
                        )
                            ?.bufferedReader()
                            ?.readText()
                            .orEmpty()

                    when {
                        status ==
                            200 &&
                            okPattern.containsMatchIn(
                                body,
                            ) ->
                            HereticPersistentRuntime.GenerationReadiness.Available

                        status ==
                            503 &&
                            unavailablePattern.containsMatchIn(
                                body,
                            ) ->
                            HereticPersistentRuntime.GenerationReadiness.Unavailable

                        else ->
                            HereticPersistentRuntime.GenerationReadiness.Unknown
                    }
                } finally {
                    connection.disconnect()
                }
            } catch (
                _: Exception,
            ) {
                HereticPersistentRuntime.GenerationReadiness.Unknown
            }
    }

    override fun environmentCurrent(
        binding: HereticPersistentRuntime.Binding,
    ): Boolean =
        try {
            val observation =
                HereticPythonEnvironment.observeCurrent(
                    productHome = productHome,
                    runner = commandRunner,
                )

            observation !=
                null &&
                observation.pythonEnvironmentRoot ==
                Path.of(
                    binding.pythonEnvironmentRoot,
                )
                    .normalize()
                    .toString() &&
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

    override fun generationReadiness(
        binding: HereticPersistentRuntime.Binding,
    ): HereticPersistentRuntime.GenerationReadiness {
        // Readiness is outside process/substrate currentness. The exact bound model endpoint owns
        // the observation; only explicit Unavailable may later authorize lifecycle recovery.
        return healthProbe.observe(
            binding.model.identity.endpoint,
        )
    }

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
        MacUserLaunchdProcess.receiptCurrent(
            receipt = receipt,
            host = launchdHost,
            expectedPid = expectedPid,
        )

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
        require(
            VexOwnedPath.observeDirectory(
                productHome,
                Path.of(
                    binding.modelPath,
                ),
            ) ==
                VexOwnedPath.DirectoryState.Present,
        ) {
            "Heretic model path is not inside one current owned Prod Home directory chain"
        }
        require(
            VexOwnedPath.observeDirectory(
                productHome,
                Path.of(
                    binding.pythonEnvironmentRoot,
                ),
            ) ==
                VexOwnedPath.DirectoryState.Present,
        ) {
            "Heretic Python environment is not inside one current owned Prod Home directory chain"
        }

        VexOwnedPath.ensureDirectory(
            productHome,
            runtimeRoot,
        )
        VexOwnedPath.ensureDirectory(
            productHome,
            runtimeRoot.resolve(
                "launchd",
            ),
        )
        VexOwnedPath.ensureDirectory(
            productHome,
            huggingFaceCacheRoot,
        )

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
                    inheritedEnvironment() +
                        (
                            "HF_HUB_CACHE" to
                                huggingFaceCacheRoot.toString()
                        ),
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

            when (
                healthProbe.observe(
                    binding.model.identity.endpoint,
                )
            ) {
                HereticPersistentRuntime.GenerationReadiness.Available ->
                    return

                HereticPersistentRuntime.GenerationReadiness.Unavailable ->
                    error(
                        "mlx_lm.server generation engine reported unavailable during startup",
                    )

                HereticPersistentRuntime.GenerationReadiness.Unknown ->
                    sleep(
                        500,
                    )
            }
        }

        error(
            "mlx_lm.server did not become healthy before bounded startup timeout",
        )
    }

    override fun qualify(
        binding: HereticPersistentRuntime.Binding,
    ): HereticRuntimeQualification {
        val probeNativeTools =
            binding.model.companionProfile.providerNativeToolStanding ==
                vexlife.model.heretic.HereticCompanionRepresentation.ProviderNativeToolStanding.RECOGNIZED_HELD
        val probeVexReach =
            binding.model.modelRef ==
                vexlife.model.heretic.HereticModelCatalog.Models.Qwen4B.modelRef &&
                binding.model.companionProfile.capabilityRepresentation ==
                vexlife.model.heretic.HereticCompanionRepresentation.CapabilityRepresentation.VEXLIFE_CONTROL_TEXT &&
                binding.model.companionProfile.reachRepresentationStanding ==
                vexlife.model.heretic.HereticCompanionRepresentation.ReachRepresentationStanding.HELD_PROVIDER_BRIDGE
        val qualificationScript =
            """
            import json, pathlib, sys, urllib.request

            origin, model_path, request_model = sys.argv[1:4]

            def read_json(request, timeout):
                with urllib.request.urlopen(request, timeout=timeout) as response:
                    body = response.read().decode("utf-8", errors="replace")
                    content_type = response.headers.get("Content-Type", "")
                    if response.status != 200:
                        raise RuntimeError(
                            "HTTP %s content-type=%s body=%r"
                            % (response.status, content_type, body[:512])
                        )
                    try:
                        return json.loads(body)
                    except json.JSONDecodeError as error:
                        raise RuntimeError(
                            "non-JSON HTTP 200 content-type=%s body=%r"
                            % (content_type, body[:512])
                        ) from error

            def post_completion(payload, timeout=180):
                request = urllib.request.Request(
                    origin + "/v1/chat/completions",
                    data=json.dumps(payload).encode("utf-8"),
                    headers={"Content-Type": "application/json"},
                    method="POST",
                )
                completion = read_json(request, timeout)
                if completion.get("model") != request_model:
                    raise RuntimeError("completion model does not match exact request model")
                return completion

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

            completion = post_completion({
                "model": request_model,
                "messages": [{"role": "user", "content": "Reply with OK."}],
                "max_tokens": 1,
                "temperature": 0,
                "stream": False,
                "chat_template_kwargs": {"enable_thinking": False},
            })
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
                        qualificationScript,
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

        val providerNativeToolsQualified =
            if (
                !probeNativeTools
            ) {
                false
            } else {
                val nativeToolProbeScript =
                    """
                    import json, sys, urllib.request

                    origin, request_model = sys.argv[1:3]

                    def read_json(request, timeout):
                        with urllib.request.urlopen(request, timeout=timeout) as response:
                            body = response.read().decode("utf-8", errors="replace")
                            content_type = response.headers.get("Content-Type", "")
                            if response.status != 200:
                                raise RuntimeError(
                                    "HTTP %s content-type=%s body=%r"
                                    % (response.status, content_type, body[:512])
                                )
                            try:
                                return json.loads(body)
                            except json.JSONDecodeError as error:
                                raise RuntimeError(
                                    "non-JSON HTTP 200 content-type=%s body=%r"
                                    % (content_type, body[:512])
                                ) from error

                    def post_completion(payload, timeout=120):
                        request = urllib.request.Request(
                            origin + "/v1/chat/completions",
                            data=json.dumps(payload).encode("utf-8"),
                            headers={"Content-Type": "application/json"},
                            method="POST",
                        )
                        completion = read_json(request, timeout)
                        if completion.get("model") != request_model:
                            raise RuntimeError("completion model does not match exact request model")
                        return completion

                    token = "vexlife-native-tool-roundtrip"
                    tool_name = "vexlife_qualification_echo"
                    user_message = {
                        "role": "user",
                        "content":
                            "Call vexlife_qualification_echo with token " + token +
                            ". Do not answer directly. After the tool result, reply exactly QUALIFIED.",
                    }
                    tools = [{
                        "type": "function",
                        "function": {
                            "name": tool_name,
                            "description": "Synthetic no-effect qualification tool.",
                            "parameters": {
                                "type": "object",
                                "properties": {
                                    "token": {"type": "string"},
                                },
                                "required": ["token"],
                                "additionalProperties": False,
                            },
                        },
                    }]

                    try:
                        first = post_completion({
                            "model": request_model,
                            "messages": [user_message],
                            "tools": tools,
                            "max_tokens": 64,
                            "temperature": 0,
                            "stream": False,
                            "chat_template_kwargs": {"enable_thinking": True},
                        })
                        first_choices = first.get("choices")
                        first_message = (
                            first_choices[0].get("message", {})
                            if isinstance(first_choices, list) and first_choices and isinstance(first_choices[0], dict)
                            else {}
                        )
                        tool_calls = first_message.get("tool_calls")
                        if not isinstance(tool_calls, list) or len(tool_calls) != 1:
                            raise RuntimeError("native-tool qualification did not return exactly one tool call")
                        tool_call = tool_calls[0]
                        if not isinstance(tool_call, dict):
                            raise RuntimeError("native-tool qualification tool call is not an object")
                        function = tool_call.get("function")
                        if not isinstance(function, dict) or function.get("name") != tool_name:
                            raise RuntimeError("native-tool qualification called the wrong function")
                        arguments = function.get("arguments")
                        if isinstance(arguments, str):
                            arguments = json.loads(arguments)
                        if not isinstance(arguments, dict) or arguments.get("token") != token:
                            raise RuntimeError("native-tool qualification arguments do not match")
                        tool_call_id = tool_call.get("id")
                        if not isinstance(tool_call_id, str) or not tool_call_id:
                            raise RuntimeError("native-tool qualification call id is missing")

                        assistant_message = {
                            "role": "assistant",
                            "content": first_message.get("content"),
                            "tool_calls": tool_calls,
                        }
                        second = post_completion({
                            "model": request_model,
                            "messages": [
                                user_message,
                                assistant_message,
                                {
                                    "role": "tool",
                                    "tool_call_id": tool_call_id,
                                    "content": json.dumps({"token": token}),
                                },
                            ],
                            "tools": tools,
                            "max_tokens": 16,
                            "temperature": 0,
                            "stream": False,
                            "chat_template_kwargs": {"enable_thinking": True},
                        })
                        second_choices = second.get("choices")
                        second_content = (
                            second_choices[0].get("message", {}).get("content")
                            if isinstance(second_choices, list) and second_choices and isinstance(second_choices[0], dict)
                            else None
                        )
                        if not isinstance(second_content, str) or second_content.strip() != "QUALIFIED":
                            raise RuntimeError("native-tool qualification continuation did not return QUALIFIED")

                        print("QUALIFIED_NATIVE_TOOLS")
                    except Exception as error:
                        print(
                            "NATIVE_TOOL_HELD:%s" % str(error)[:512],
                            file=sys.stderr,
                        )
                        print("HELD")
                    """.trimIndent()

                val probe =
                    commandRunner.run(
                        program =
                            binding.pythonPath,
                        args =
                            listOf(
                                "-c",
                                nativeToolProbeScript,
                                HereticProductIdentity.Endpoint,
                                HereticProductIdentity.RequestModel,
                            ),
                        timeoutMs =
                            300_000L,
                    )

                probe.status ==
                    0 &&
                    probe.errorMessage ==
                    null &&
                    !probe.timedOut &&
                    probe.stdout
                        .trim() ==
                    "QUALIFIED_NATIVE_TOOLS"
            }

        val vexReachQualified =
            if (
                !probeVexReach
            ) {
                false
            } else {
                val atlas =
                    requireNotNull(
                        VexLifeCapabilityAtlas.resolve(
                            "atlas",
                        ),
                    ) {
                        "Vex Reach qualification requires the current Atlas capability"
                    }
                val atlasList =
                    requireNotNull(
                        atlas.actions.singleOrNull {
                            it.action ==
                                "list"
                        },
                    ) {
                        "Vex Reach qualification requires the current Atlas list action"
                    }
                val atlasDescribe =
                    requireNotNull(
                        atlas.actions.singleOrNull {
                            it.action ==
                                "describe"
                        },
                    ) {
                        "Vex Reach qualification requires the current Atlas describe action"
                    }
                val derivedContinuationCapability =
                    requireNotNull(
                        VexLifeCapabilityAtlas
                            .definitions()
                            .singleOrNull {
                                definition ->
                                definition.actions
                                    .singleOrNull()
                                    ?.action ==
                                    "test"
                            },
                    ) {
                        "Vex Reach qualification requires one uniquely discoverable registered test capability"
                    }

                val estimateQualificationTokens: (String) -> Int = {
                    content ->
                    maxOf(
                        1,
                        (
                            content
                                .toByteArray()
                                .size +
                                3
                        ) /
                            4,
                    ) +
                        4
                }

                val qualificationSystemMessage =
                    "This is a synthetic no-effect VexLife text-control reach qualification. " +
                        "Use the real VexLife request/return relationship. " +
                        VexLifeCapabilityRequest.TextControlProviderInstruction +
                        " Start by asking capability " +
                        atlas.handle +
                        " for action " +
                        atlasList.action +
                        " with an empty arguments object. If the host returns an INVALID_REQUEST, NOT_AVAILABLE, " +
                        "or other capability result, read that owner feedback and correct your next request rather " +
                        "than pretending the earlier request succeeded. After a successful Atlas list result, find " +
                        "the registered capability whose action list is exactly test, then ask capability " +
                        atlas.handle +
                        " for action " +
                        atlasDescribe.action +
                        " with arguments containing only handle set to the capability handle you discovered. " +
                        "Do not add human-facing prose to a capability-request stage."
                val qualificationUserMessage =
                    "Use capability atlas to list the current capability registry, discover the one registered capability whose action list is exactly test, then use capability atlas to describe that discovered handle before answering. Discover the test-capability handle from the returned Atlas list; do not guess it from this request."

                val providerStageScript =
                    """
                    import base64, json, sys, urllib.request

                    origin, request_model, max_tokens, system_content, user_content, *exchange_content = sys.argv[1:]
                    if len(exchange_content) % 2 != 0:
                        raise RuntimeError("qualification exchange message pairing is invalid")

                    messages = [
                        {"role": "system", "content": system_content},
                        {"role": "user", "content": user_content},
                    ]
                    for index in range(0, len(exchange_content), 2):
                        messages.append({"role": "assistant", "content": exchange_content[index]})
                        messages.append({"role": "user", "content": exchange_content[index + 1]})

                    request = urllib.request.Request(
                        origin + "/v1/chat/completions",
                        data=json.dumps({
                            "model": request_model,
                            "messages": messages,
                            "max_tokens": int(max_tokens),
                            "temperature": 0,
                            "stream": False,
                            "chat_template_kwargs": {"enable_thinking": True},
                        }).encode("utf-8"),
                        headers={"Content-Type": "application/json"},
                        method="POST",
                    )
                    with urllib.request.urlopen(request, timeout=120) as response:
                        body = response.read().decode("utf-8", errors="replace")
                        content_type = response.headers.get("Content-Type", "")
                        if response.status != 200:
                            raise RuntimeError(
                                "HTTP %s content-type=%s body=%r"
                                % (response.status, content_type, body[:512])
                            )
                    try:
                        completion = json.loads(body)
                    except json.JSONDecodeError as error:
                        raise RuntimeError(
                            "non-JSON HTTP 200 content-type=%s body=%r"
                            % (content_type, body[:512])
                        ) from error
                    if completion.get("model") != request_model:
                        raise RuntimeError("completion model does not match exact request model")
                    choices = completion.get("choices")
                    content = (
                        choices[0].get("message", {}).get("content")
                        if isinstance(choices, list) and choices and isinstance(choices[0], dict)
                        else None
                    )
                    if not isinstance(content, str) or not content.strip():
                        raise RuntimeError("Vex Reach qualification returned no assistant content")
                    print(base64.b64encode(content.encode("utf-8")).decode("ascii"))
                    """.trimIndent()

                val exchanges =
                    mutableListOf<VexLifeCapabilityBridge.Exchange>()

                fun invokeQualificationStage(
                    maxTokens: Int,
                ): String? {
                    val args =
                        mutableListOf(
                            "-c",
                            providerStageScript,
                            HereticProductIdentity.Endpoint,
                            HereticProductIdentity.RequestModel,
                            maxTokens.toString(),
                            qualificationSystemMessage,
                            qualificationUserMessage,
                        )
                    exchanges.forEach {
                        exchange ->
                        args +=
                            exchange.assistantMessage.content
                        args +=
                            exchange.resultMessage.content
                    }
                    val stage =
                        commandRunner.run(
                            program =
                                binding.pythonPath,
                            args =
                                args,
                            timeoutMs =
                                120_000L,
                        )
                    if (
                        stage.status !=
                            0 ||
                        stage.errorMessage !=
                            null ||
                        stage.timedOut
                    ) {
                        return null
                    }
                    val encoded =
                        stage.stdout
                            .trim()
                    if (
                        encoded.isEmpty()
                    ) {
                        return null
                    }
                    return runCatching {
                        String(
                            Base64
                                .getDecoder()
                                .decode(
                                    encoded,
                                ),
                            Charsets.UTF_8,
                        )
                    }
                        .getOrNull()
                        ?.takeIf {
                            it.isNotBlank()
                        }
                }

                fun performQualificationRequest(
                    parsed: VexLifeCapabilityRequest.Parsed,
                    requestNumber: Int,
                ): VexLifeCapabilityBridge.Exchange {
                    val usedTokens =
                        exchanges.sumOf {
                            exchange ->
                            estimateQualificationTokens(
                                exchange.assistantMessage.content,
                            ) +
                                estimateQualificationTokens(
                                    exchange.resultMessage.content,
                                )
                        }
                    return VexLifeCapabilityBridge.perform(
                        parsed =
                            parsed,
                        admission =
                            VexLifeCapabilityBridge.Admission(
                                productHome =
                                    productHome,
                                availableCapabilityRefs =
                                    setOf(
                                        atlas.capabilityRef,
                                    ),
                                requestNumber =
                                    requestNumber,
                                tokensAvailable =
                                    maxOf(
                                        0,
                                        VexLifeCapabilityBridge.ExchangeAllowanceTokens -
                                            usedTokens,
                                    ),
                            ),
                        estimateTokens =
                            estimateQualificationTokens,
                    )
                }

                var atlasListed =
                    false
                var qualified =
                    false

                val seenControls =
                    mutableSetOf<String>()

                while (true) {
                    val usedTokens =
                        exchanges.sumOf {
                            exchange ->
                            estimateQualificationTokens(
                                exchange.assistantMessage.content,
                            ) +
                                estimateQualificationTokens(
                                    exchange.resultMessage.content,
                                )
                        }
                    if (
                        usedTokens >=
                            VexLifeCapabilityBridge.ExchangeAllowanceTokens
                    ) {
                        break
                    }

                    val content =
                        invokeQualificationStage(
                            maxTokens =
                                2048,
                        )
                            ?: break
                    val normalizedCapabilityControl =
                        HereticTextControlCapabilityWire.normalize(
                            profile = HereticCompanionRepresentation.Profiles.Qwen4B,
                            content = content,
                        )
                    val canonicalControl =
                        normalizedCapabilityControl.canonicalControl
                    if (
                        !seenControls.add(
                            canonicalControl,
                        )
                    ) {
                        break
                    }
                    val parsed =
                        VexLifeCapabilityRequest.parseOrNull(
                            canonicalControl,
                        )
                            ?: break
                    val call =
                        (
                            parsed as?
                                VexLifeCapabilityRequest.Parsed.Request
                        )
                            ?.call
                    val definition =
                        call
                            ?.let {
                                VexLifeCapabilityAtlas.resolve(
                                    it.capability,
                                )
                            }
                    val describedHandle =
                        (
                            call
                                ?.arguments
                                ?.get(
                                    "handle",
                                ) as? JsonPrimitive
                        )
                            ?.contentOrNull
                    val describedDefinition =
                        describedHandle
                            ?.let {
                                VexLifeCapabilityAtlas.resolve(
                                    it,
                                )
                            }

                    val isAtlasList =
                        definition
                            ?.capabilityRef ==
                            atlas.capabilityRef &&
                            call
                                ?.action ==
                            atlasList.action &&
                            call
                                ?.arguments
                                ?.isEmpty() ==
                            true
                    val isDerivedAtlasDescribe =
                        atlasListed &&
                            definition
                                ?.capabilityRef ==
                            atlas.capabilityRef &&
                            call
                                ?.action ==
                            atlasDescribe.action &&
                            describedDefinition
                                ?.capabilityRef ==
                            derivedContinuationCapability.capabilityRef &&
                            call
                                ?.arguments
                                ?.size ==
                            1

                    val exchange =
                        performQualificationRequest(
                            parsed =
                                parsed,
                            requestNumber =
                                exchanges.size +
                                    1,
                        )
                    exchanges +=
                        exchange

                    if (
                        exchange.use.state ==
                        VexLifeCapabilityBridge.ResultState.HELD_TOKEN_BUDGET
                    ) {
                        break
                    }

                    if (
                        isAtlasList &&
                        exchange.use.state ==
                        VexLifeCapabilityBridge.ResultState.RETURNED
                    ) {
                        atlasListed =
                            true
                    }
                    if (
                        isDerivedAtlasDescribe &&
                        exchange.use.state ==
                        VexLifeCapabilityBridge.ResultState.RETURNED
                    ) {
                        qualified =
                            true
                        break
                    }
                }

                qualified
            }

        return HereticRuntimeQualification.qualified(
            providerNativeToolsQualified =
                providerNativeToolsQualified,
            vexReachQualified =
                vexReachQualified,
        )
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
            environment = environment,
            uid =
                domainTarget
                    .substringAfter(
                        "gui/",
                    )
                    .toLong(),
        )
}

// [VXG RealForever]
