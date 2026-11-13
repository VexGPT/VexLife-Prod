package vexlife.vision

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermission
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import vexlife.contract.VexContractJson
import vexlife.service.VexLifeProdServiceContract

class VexLifeProdVisionRuntimeTest {
    private fun runtimeExecutable(
        parent: Path,
    ): Path {
        val executable =
            parent.resolve(
                "vision-runtime/bin/vexlife-vision-runtime",
            )
        Files.createDirectories(
            requireNotNull(
                executable.parent,
            ),
        )
        Files.writeString(
            executable,
            "test-runtime",
        )
        Files.setPosixFilePermissions(
            executable,
            setOf(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE,
                PosixFilePermission.OWNER_EXECUTE,
            ),
        )
        return executable
    }

    private fun held(
        requestRef: String,
    ): VexLifeProdServiceContract.VisionRuntimeResult =
        VexLifeProdServiceContract.VisionRuntimeResult(
            schemaVersion =
                VexLifeProdServiceContract.VisionRuntimeResultSchema,
            state = "HELD_SAFETY_NOT_CURRENT",
            requestRef = requestRef,
            sessionRefOrNull = null,
            connectionIdOrNull = null,
            selectionMode = "SINGLE_WINDOW",
            osOwnsSelectionUi = true,
            helperLaunched = false,
            projectionOrNull = null,
            activeSharingWitnessOrNull = null,
            activeVisualObservationOrNull = null,
            rawBytesIncluded = false,
            rawRetentionAllowed = false,
            visualModelInputAuthorized = false,
            memoryAccepted = false,
            trainingEligible = false,
            publicationEligible = false,
            continuousCaptureAllowed = false,
            backgroundCaptureAllowed = false,
            persistentHostPermissionEstablished = false,
        )

    @Test
    fun exactCarriedRuntimeInvocationReturnsOneBoundResult() {
        val parent =
            Files.createTempDirectory(
                "vexlife-prod-vision-runtime-",
            )
                .toAbsolutePath()
        val executable =
            runtimeExecutable(
                parent,
            )
        val requestRef =
            "request.vexlife-prod.vision.test.001"
        var calls =
            0

        try {
            val result =
                VexLifeProdVisionRuntime.lookNow(
                    executable = executable,
                    requestRef = requestRef,
                    executor =
                        VexLifeProdVisionRuntime.Executor {
                            observedExecutable,
                            observedRequestRef,
                            ->
                            calls +=
                                1
                            assertEquals(
                                executable,
                                observedExecutable,
                            )
                            assertEquals(
                                requestRef,
                                observedRequestRef,
                            )
                            VexLifeProdVisionRuntime.Execution(
                                exitCode = 0,
                                timedOut = false,
                                stdout =
                                    VexContractJson.compactEncode(
                                        held(
                                            requestRef,
                                        ),
                                    ) +
                                        "\n",
                                stderr = "",
                            )
                        },
                )

            assertEquals(
                1,
                calls,
            )
            assertEquals(
                "HELD_SAFETY_NOT_CURRENT",
                result.state,
            )
            assertEquals(
                requestRef,
                result.requestRef,
            )
            assertFalse(
                result.helperLaunched,
            )
            assertNull(
                result.sessionRefOrNull,
            )
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun runtimeResultMustRemainSingleExactAndRequestBound() {
        val parent =
            Files.createTempDirectory(
                "vexlife-prod-vision-runtime-invalid-",
            )
                .toAbsolutePath()
        val executable =
            runtimeExecutable(
                parent,
            )
        val requestRef =
            "request.vexlife-prod.vision.test.002"
        val valid =
            VexContractJson.compactEncode(
                held(
                    requestRef,
                ),
            )

        try {
            assertFailsWith<IllegalStateException> {
                VexLifeProdVisionRuntime.lookNow(
                    executable = executable,
                    requestRef = requestRef,
                    executor =
                        VexLifeProdVisionRuntime.Executor {
                            _,
                            _,
                            ->
                            VexLifeProdVisionRuntime.Execution(
                                exitCode = 0,
                                timedOut = false,
                                stdout =
                                    valid +
                                        "\n" +
                                        valid +
                                        "\n",
                                stderr = "",
                            )
                        },
                )
            }

            assertFailsWith<IllegalStateException> {
                VexLifeProdVisionRuntime.lookNow(
                    executable = executable,
                    requestRef = requestRef,
                    executor =
                        VexLifeProdVisionRuntime.Executor {
                            _,
                            _,
                            ->
                            VexLifeProdVisionRuntime.Execution(
                                exitCode = 0,
                                timedOut = false,
                                stdout =
                                    VexContractJson.compactEncode(
                                        held(
                                            "request.vexlife-prod.vision.other",
                                        ),
                                    ),
                                stderr = "",
                            )
                        },
                )
            }

            assertFailsWith<IllegalStateException> {
                VexLifeProdVisionRuntime.lookNow(
                    executable = executable,
                    requestRef = requestRef,
                    executor =
                        VexLifeProdVisionRuntime.Executor {
                            _,
                            _,
                            ->
                            VexLifeProdVisionRuntime.Execution(
                                exitCode = 70,
                                timedOut = false,
                                stdout = "",
                                stderr = "runtime failed",
                            )
                        },
                )
            }
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }
}

// [VXG RealForever]
