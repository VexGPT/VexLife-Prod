package vexlife.vision

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import vexlife.contract.VexContractJson
import vexlife.service.VexLifeProdServiceContract

/**
 * Product-side process bridge to the already-carried VexVision runtime executable.
 *
 * This owner does not implement ScreenCaptureKit, Safety, Security witness formation, Perception
 * observation formation, Memory, model input, or publication. It invokes exactly one source-bound
 * runtime executable and validates only the bounded top-level result needed by Product.
 */
object VexLifeProdVisionRuntime {
    private const val RuntimeExecutableSuffix =
        "/vision-runtime/bin/vexlife-vision-runtime"

    internal const val ExecutionTimeoutMs =
        130_000L

    internal const val MaxStdoutBytes =
        65_536

    internal const val MaxStderrBytes =
        65_536

    internal data class Execution(
        val exitCode: Int,
        val timedOut: Boolean,
        val stdout: String,
        val stderr: String,
        val stdoutLimitExceeded: Boolean = false,
        val stderrLimitExceeded: Boolean = false,
    )

    internal fun interface Executor {
        fun execute(
            executable: Path,
            requestRef: String,
        ): Execution
    }

    private data class Capture(
        val text: String,
        val limitExceeded: Boolean,
    )

    private object SystemExecutor : Executor {
        override fun execute(
            executable: Path,
            requestRef: String,
        ): Execution {
            val process =
                ProcessBuilder(
                    executable.toString(),
                    requestRef,
                )
                    .redirectErrorStream(
                        false,
                    )
                    .start()
            val readers =
                Executors.newFixedThreadPool(
                    2,
                )

            try {
                val stdoutFuture =
                    readers.submit<Capture> {
                        capture(
                            process.inputStream,
                            MaxStdoutBytes,
                        )
                    }
                val stderrFuture =
                    readers.submit<Capture> {
                        capture(
                            process.errorStream,
                            MaxStderrBytes,
                        )
                    }

                val finished =
                    process.waitFor(
                        ExecutionTimeoutMs,
                        TimeUnit.MILLISECONDS,
                    )

                if (
                    !finished
                ) {
                    process.destroy()
                    if (
                        !process.waitFor(
                            1,
                            TimeUnit.SECONDS,
                        )
                    ) {
                        process.destroyForcibly()
                        process.waitFor(
                            1,
                            TimeUnit.SECONDS,
                        )
                    }
                }

                val stdout =
                    stdoutFuture.get(
                        5,
                        TimeUnit.SECONDS,
                    )
                val stderr =
                    stderrFuture.get(
                        5,
                        TimeUnit.SECONDS,
                    )

                return Execution(
                    exitCode =
                        if (
                            finished
                        ) {
                            process.exitValue()
                        } else {
                            -1
                        },
                    timedOut =
                        !finished,
                    stdout = stdout.text,
                    stderr = stderr.text,
                    stdoutLimitExceeded = stdout.limitExceeded,
                    stderrLimitExceeded = stderr.limitExceeded,
                )
            } finally {
                readers.shutdownNow()
                process.inputStream.close()
                process.errorStream.close()
                process.outputStream.close()
            }
        }
    }

    fun lookNow(
        executable: Path,
        requestRef: String,
    ): VexLifeProdServiceContract.VisionRuntimeResult =
        lookNow(
            executable = executable,
            requestRef = requestRef,
            executor = SystemExecutor,
        )

    internal fun lookNow(
        executable: Path,
        requestRef: String,
        executor: Executor,
    ): VexLifeProdServiceContract.VisionRuntimeResult {
        VexLifeProdServiceContract.VisionLookNowRequest(
            requestRef = requestRef,
        )

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

        require(
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

        val execution =
            executor.execute(
                normalized,
                requestRef,
            )

        require(
            !execution.timedOut &&
                !execution.stdoutLimitExceeded &&
                !execution.stderrLimitExceeded &&
                execution.exitCode ==
                0 &&
                execution.stdout
                    .toByteArray(
                        StandardCharsets.UTF_8,
                    )
                    .size <=
                MaxStdoutBytes &&
                execution.stderr
                    .toByteArray(
                        StandardCharsets.UTF_8,
                    )
                    .size <=
                MaxStderrBytes,
        ) {
            "VexLife-Prod Vision runtime execution failed closed"
        }

        val lines =
            execution.stdout
                .lineSequence()
                .map(
                    String::trim,
                )
                .filter(
                    String::isNotEmpty,
                )
                .toList()

        require(
            lines.size ==
                1,
        ) {
            "VexLife-Prod Vision runtime must emit exactly one Product result"
        }

        val result =
            VexContractJson.decode<VexLifeProdServiceContract.VisionRuntimeResult>(
                lines.single(),
            )

        require(
            result.requestRef ==
                requestRef,
        ) {
            "VexLife-Prod Vision runtime result request identity drift"
        }

        return result
    }

    private fun capture(
        input: InputStream,
        maxBytes: Int,
    ): Capture {
        val bytes =
            ByteArrayOutputStream()
        val buffer =
            ByteArray(
                8 * 1024,
            )
        var observed =
            0L
        var exceeded =
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

            observed +=
                read

            if (
                observed >
                maxBytes
            ) {
                exceeded =
                    true
            }

            val remaining =
                maxBytes -
                    bytes.size()
            if (
                remaining >
                0
            ) {
                bytes.write(
                    buffer,
                    0,
                    minOf(
                        read,
                        remaining,
                    ),
                )
            }
        }

        return Capture(
            text =
                bytes.toString(
                    StandardCharsets.UTF_8,
                ),
            limitExceeded =
                exceeded,
        )
    }
}

// [VXG RealForever]
