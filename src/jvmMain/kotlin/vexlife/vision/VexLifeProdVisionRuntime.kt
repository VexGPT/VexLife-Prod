package vexlife.vision

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.util.UUID
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlinx.serialization.Serializable
import vexlife.contract.VexContractJson
import vexlife.service.VexLifeProdServiceContract

/**
 * Product-side persistent process bridge to the already-carried selected-scope Vision runtime.
 *
 * Product owns only the lifetime of one child process and command correlation. It does not own the
 * selected-scope session state machine, native provider/window identity, ScreenCaptureKit, capture,
 * visual-model input, Conversation Context, Memory, training, or publication.
 */
object VexLifeProdVisionRuntime {
    private const val RuntimeExecutableSuffix =
        "/vision-runtime/bin/vexlife-vision-runtime"

    private const val CommandSchema =
        "vextreme.macos-screen-selected-scope-product-runtime-command/v1"

    internal const val CommandTimeoutMs =
        130_000L

    internal const val CloseWaitMs =
        5_000L

    internal const val MaxCommandBytes =
        4_096

    internal const val MaxStdoutLineBytes =
        65_536

    internal const val MaxStderrBytes =
        65_536

    @Serializable
    private data class RuntimeCommand(
        val schemaVersion: String,
        val commandRef: String,
        val commandType: String,
    )

    internal interface Transport :
        AutoCloseable {
        fun request(
            line: String,
            timeoutMs: Long,
        ): String

        val stderrLimitExceeded: Boolean

        val stderrSnapshot: String
    }

    internal fun interface TransportFactory {
        fun open(
            executable: Path,
        ): Transport
    }

    private class SystemTransport(
        executable: Path,
    ) : Transport {
        private val process =
            ProcessBuilder(
                executable.toString(),
            )
                .redirectErrorStream(
                    false,
                )
                .start()

        private val responseExecutor: ExecutorService =
            Executors.newSingleThreadExecutor {
                runnable ->
                Thread(
                    runnable,
                    "vexlife-prod-vision-runtime-response",
                )
                    .apply {
                        isDaemon =
                            true
                    }
            }

        private val stderrExecutor: ExecutorService =
            Executors.newSingleThreadExecutor {
                runnable ->
                Thread(
                    runnable,
                    "vexlife-prod-vision-runtime-stderr",
                )
                    .apply {
                        isDaemon =
                            true
                    }
            }

        private val stderrCapture =
            BoundedCapture(
                MaxStderrBytes,
            )

        private val stderrTask =
            stderrExecutor.submit {
                capture(
                    input = process.errorStream,
                    target = stderrCapture,
                )
            }

        @Volatile
        private var closed =
            false

        override val stderrLimitExceeded: Boolean
            get() =
                stderrCapture.limitExceeded

        override val stderrSnapshot: String
            get() =
                stderrCapture.text()

        override fun request(
            line: String,
            timeoutMs: Long,
        ): String {
            check(
                !closed &&
                    process.isAlive,
            ) {
                "VexLife-Prod persistent Vision runtime process is not alive"
            }
            check(
                line
                    .toByteArray(
                        StandardCharsets.UTF_8,
                    )
                    .size <=
                    MaxStdoutLineBytes &&
                    !line.contains(
                        '\n',
                    ) &&
                    !line.contains(
                        '\r',
                    ),
            ) {
                "VexLife-Prod persistent Vision runtime command is not one bounded line"
            }

            process.outputStream.write(
                (
                    line +
                        "\n"
                )
                    .toByteArray(
                        StandardCharsets.UTF_8,
                    ),
            )
            process.outputStream.flush()

            val responseFuture =
                responseExecutor.submit<String?> {
                    readBoundedLine(
                        process.inputStream,
                        MaxStdoutLineBytes,
                    )
                }

            val response =
                try {
                    responseFuture.get(
                        timeoutMs,
                        TimeUnit.MILLISECONDS,
                    )
                } catch (
                    error: TimeoutException,
                ) {
                    responseFuture.cancel(
                        true,
                    )
                    terminateProcess()
                    throw IllegalStateException(
                        "VexLife-Prod persistent Vision runtime command timed out",
                        error,
                    )
                }

            check(
                response !=
                    null,
            ) {
                "VexLife-Prod persistent Vision runtime closed before one response"
            }
            check(
                !stderrLimitExceeded,
            ) {
                "VexLife-Prod persistent Vision runtime stderr exceeded its bounded limit"
            }

            return response
        }

        override fun close() {
            if (
                closed
            ) {
                return
            }
            closed =
                true

            runCatching {
                process.outputStream.close()
            }

            if (
                process.isAlive
            ) {
                val exited =
                    runCatching {
                        process.waitFor(
                            CloseWaitMs,
                            TimeUnit.MILLISECONDS,
                        )
                    }
                        .getOrDefault(
                            false,
                        )

                if (
                    !exited &&
                    process.isAlive
                ) {
                    process.destroy()
                    val destroyed =
                        runCatching {
                            process.waitFor(
                                1,
                                TimeUnit.SECONDS,
                            )
                        }
                            .getOrDefault(
                                false,
                            )

                    if (
                        !destroyed &&
                        process.isAlive
                    ) {
                        process.destroyForcibly()
                        runCatching {
                            process.waitFor(
                                1,
                                TimeUnit.SECONDS,
                            )
                        }
                    }
                }
            }

            runCatching {
                process.inputStream.close()
            }
            runCatching {
                process.errorStream.close()
            }

            responseExecutor.shutdownNow()
            stderrExecutor.shutdownNow()

            runCatching {
                stderrTask.get(
                    1,
                    TimeUnit.SECONDS,
                )
            }
        }

        private fun terminateProcess() {
            if (
                process.isAlive
            ) {
                process.destroy()
                .also {
                    runCatching {
                        process.waitFor(
                            1,
                            TimeUnit.SECONDS,
                        )
                    }
                }

                if (
                    process.isAlive
                ) {
                    process.destroyForcibly()
                }
            }
        }
    }

    private class BoundedCapture(
        private val maxBytes: Int,
    ) {
        private val bytes =
            ByteArrayOutputStream()

        @Volatile
        var limitExceeded: Boolean =
            false
            private set

        @Synchronized
        fun accept(
            buffer: ByteArray,
            count: Int,
        ) {
            val remaining =
                maxBytes -
                    bytes.size()

            if (
                count >
                remaining
            ) {
                limitExceeded =
                    true
            }

            if (
                remaining >
                0
            ) {
                bytes.write(
                    buffer,
                    0,
                    minOf(
                        count,
                        remaining,
                    ),
                )
            }
        }

        @Synchronized
        fun text(): String =
            bytes.toString(
                StandardCharsets.UTF_8,
            )
    }

    class Session internal constructor(
        private val transport: Transport,
        private val commandRefSource: () -> String,
    ) : AutoCloseable {
        private val lock =
            Any()

        private var closed =
            false

        fun project(): VexLifeProdServiceContract.VisionSelectedScopeRuntimeResponse =
            command(
                commandType = "PROJECT",
                commandRef = nextCommandRef(),
            )

        fun engage(
            requestRef: String,
        ): VexLifeProdServiceContract.VisionSelectedScopeRuntimeResponse {
            VexLifeProdServiceContract.VisionScopeCommandRequest(
                requestRef = requestRef,
            )

            return command(
                commandType = "ENGAGE",
                commandRef = requestRef,
            )
        }

        fun stop(
            requestRef: String,
        ): VexLifeProdServiceContract.VisionSelectedScopeRuntimeResponse {
            VexLifeProdServiceContract.VisionScopeCommandRequest(
                requestRef = requestRef,
            )

            return command(
                commandType = "STOP",
                commandRef = requestRef,
            )
        }

        internal fun reconcile(): VexLifeProdServiceContract.VisionSelectedScopeRuntimeResponse =
            command(
                commandType = "RECONCILE",
                commandRef = nextCommandRef(),
            )

        override fun close() {
            synchronized(
                lock,
            ) {
                if (
                    closed
                ) {
                    return
                }

                var shutdownFailure: Throwable? =
                    null

                try {
                    val response =
                        commandLocked(
                            commandType = "SHUTDOWN",
                            commandRef = nextCommandRef(),
                        )

                    check(
                        response.ok &&
                            response.shutdownRequested,
                    ) {
                        "VexLife-Prod persistent Vision runtime rejected cooperative shutdown"
                    }
                } catch (
                    error: Throwable,
                ) {
                    shutdownFailure =
                        error
                } finally {
                    closed =
                        true
                    transport.close()
                }

                if (
                    shutdownFailure !=
                    null
                ) {
                    throw IllegalStateException(
                        "VexLife-Prod persistent Vision runtime did not close cooperatively",
                        shutdownFailure,
                    )
                }
            }
        }

        private fun command(
            commandType: String,
            commandRef: String,
        ): VexLifeProdServiceContract.VisionSelectedScopeRuntimeResponse =
            synchronized(
                lock,
            ) {
                commandLocked(
                    commandType = commandType,
                    commandRef = commandRef,
                )
            }

        private fun commandLocked(
            commandType: String,
            commandRef: String,
        ): VexLifeProdServiceContract.VisionSelectedScopeRuntimeResponse {
            check(
                !closed,
            ) {
                "VexLife-Prod persistent Vision runtime session is closed"
            }

            val line =
                VexContractJson.compactEncode(
                    RuntimeCommand(
                        schemaVersion = CommandSchema,
                        commandRef = commandRef,
                        commandType = commandType,
                    ),
                )

            check(
                line
                    .toByteArray(
                        StandardCharsets.UTF_8,
                    )
                    .size <=
                    MaxCommandBytes,
            ) {
                "VexLife-Prod persistent Vision runtime command exceeded the accepted SDK line bound"
            }

            val response =
                runCatching {
                    VexContractJson.decode<VexLifeProdServiceContract.VisionSelectedScopeRuntimeResponse>(
                        transport.request(
                            line = line,
                            timeoutMs = CommandTimeoutMs,
                        ),
                    )
                }
                    .getOrElse {
                        error ->
                        throw IllegalStateException(
                            "VexLife-Prod persistent Vision runtime response is not one valid bounded Product result",
                            error,
                        )
                    }

            check(
                response.commandRef ==
                    commandRef &&
                    response.commandType ==
                    commandType,
            ) {
                "VexLife-Prod persistent Vision runtime response identity drift"
            }
            check(
                !transport.stderrLimitExceeded,
            ) {
                "VexLife-Prod persistent Vision runtime emitted excessive stderr"
            }

            return response
        }

        private fun nextCommandRef(): String =
            "command.vexlife-prod.vision." +
                commandRefSource()
    }

    private val SystemTransportFactory =
        TransportFactory {
            executable ->
            SystemTransport(
                executable,
            )
        }

    fun open(
        executable: Path,
    ): Session =
        open(
            executable = executable,
            transportFactory = SystemTransportFactory,
            commandRefSource = {
                UUID.randomUUID()
                    .toString()
            },
        )

    internal fun open(
        executable: Path,
        transportFactory: TransportFactory,
        commandRefSource: () -> String,
    ): Session {
        val normalized =
            requireExecutable(
                executable,
            )

        return Session(
            transport = transportFactory.open(
                normalized,
            ),
            commandRefSource = commandRefSource,
        )
    }

    private fun requireExecutable(
        executable: Path,
    ): Path {
        val normalized =
            executable
                .toAbsolutePath()
                .normalize()
        val portable =
            normalized
                .toString()
                .replace(
                    '\\',
                    '/',
                )

        check(
            portable.endsWith(
                RuntimeExecutableSuffix,
            ) &&
                Files.isRegularFile(
                    normalized,
                    LinkOption.NOFOLLOW_LINKS,
                ) &&
                !Files.isSymbolicLink(
                    normalized,
                ) &&
                Files.isExecutable(
                    normalized,
                ),
        ) {
            "VexLife-Prod Vision runtime executable is not one exact executable carried by the private image"
        }

        return normalized
    }

    private fun readBoundedLine(
        input: InputStream,
        maxBytes: Int,
    ): String? {
        val bytes =
            ByteArrayOutputStream()

        while (
            true
        ) {
            val value =
                input.read()

            if (
                value <
                0
            ) {
                return if (
                    bytes.size() ==
                    0
                ) {
                    null
                } else {
                    bytes.toString(
                        StandardCharsets.UTF_8,
                    )
                }
            }

            if (
                value ==
                '\n'.code
            ) {
                return bytes.toString(
                    StandardCharsets.UTF_8,
                )
            }

            if (
                value ==
                '\r'.code
            ) {
                continue
            }

            check(
                bytes.size() <
                    maxBytes,
            ) {
                "VexLife-Prod persistent Vision runtime response exceeded one bounded line"
            }

            bytes.write(
                value,
            )
        }
    }

    private fun capture(
        input: InputStream,
        target: BoundedCapture,
    ) {
        val buffer =
            ByteArray(
                8 * 1024,
            )

        while (
            true
        ) {
            val read =
                runCatching {
                    input.read(
                        buffer,
                    )
                }
                    .getOrElse {
                        return
                    }

            if (
                read <
                0
            ) {
                return
            }

            target.accept(
                buffer = buffer,
                count = read,
            )
        }
    }
}

// [VXG RealForever]
