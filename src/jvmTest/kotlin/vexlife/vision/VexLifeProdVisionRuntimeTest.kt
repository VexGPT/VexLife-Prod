package vexlife.vision

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermission
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import vexlife.contract.VexContractJson
import vexlife.service.VexLifeProdServiceContract

class VexLifeProdVisionRuntimeTest {
    private class FakeTransport(
        responses: List<String>,
        override val stderrLimitExceeded: Boolean = false,
        override val stderrSnapshot: String = "",
    ) : VexLifeProdVisionRuntime.Transport {
        private val pending =
            ArrayDeque(
                responses,
            )

        val requests =
            mutableListOf<String>()

        var closeCalls =
            0

        override fun request(
            line: String,
            timeoutMs: Long,
        ): String {
            requests +=
                line
            return pending.removeFirst()
        }

        override fun close() {
            closeCalls +=
                1
        }
    }

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
            "persistent-test-runtime",
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

    private fun projection(
        state: String = "ENGAGED",
    ): VexLifeProdServiceContract.VisionSelectedScopeProjection =
        VexLifeProdServiceContract.VisionSelectedScopeProjection(
            schemaVersion =
                VexLifeProdServiceContract.VisionSelectedScopeProjectionSchema,
            scopeSessionRef = "session.macos-screen-selected-scope.product.test.001",
            state = state,
            selectedScopePresent =
                state ==
                    "ENGAGED" ||
                    state ==
                    "LOOKING",
            scopeIndicatorRequired =
                state ==
                    "ENGAGED" ||
                    state ==
                    "LOOKING",
            lookingIndicatorRequired =
                state ==
                    "LOOKING",
            lookRequestRefOrNull =
                if (
                    state ==
                        "LOOKING"
                ) {
                    "request.vision.look.test.001"
                } else {
                    null
                },
            safetyAuthorizationRefOrNull = null,
            safetyBindingRefOrNull = null,
            safetyReceiptFingerprintOrNull = null,
            safetyObservedAtOrNull = null,
            holdReasonOrNull =
                if (
                    state ==
                        "HELD"
                ) {
                    "SAFETY_NOT_CURRENT"
                } else {
                    null
                },
            endReasonOrNull =
                if (
                    state ==
                        "ENDED"
                ) {
                    "EXPLICIT_STOP"
                } else {
                    null
                },
            sensorActive = false,
            screenCaptureActive = false,
            screenCaptureAuthorized = false,
            captureExecutionAuthorized = false,
            requestReplayProtectionEstablished = false,
            captureExecutionRequiresSingleUseEffectReservation = true,
            activeObservation = false,
            rawBytesIncluded = false,
            rawRetentionAllowed = false,
            continuousCaptureAllowed = false,
            backgroundCaptureAllowed = false,
            visualModelInputAuthorized = false,
            memoryAccepted = false,
            trainingEligible = false,
            publicationEligible = false,
        )

    private fun response(
        commandRef: String,
        commandType: String,
        projectionOrNull: VexLifeProdServiceContract.VisionSelectedScopeProjection? =
            null,
        ok: Boolean =
            true,
        shutdownRequested: Boolean =
            commandType ==
                "SHUTDOWN",
        errorCodeOrNull: String? =
            null,
    ): String =
        VexContractJson.compactEncode(
            VexLifeProdServiceContract.VisionSelectedScopeRuntimeResponse(
                schemaVersion =
                    VexLifeProdServiceContract.VisionSelectedScopeRuntimeResponseSchema,
                commandRef = commandRef,
                commandType = commandType,
                ok = ok,
                projectionOrNull = projectionOrNull,
                shutdownRequested = shutdownRequested,
                errorCodeOrNull = errorCodeOrNull,
            ),
        )

    @Test
    fun persistentRuntimeProjectsThroughOneBoundChildTransport() {
        val parent =
            Files.createTempDirectory(
                "vexlife-prod-vision-persistent-",
            )
                .toAbsolutePath()
        val executable =
            runtimeExecutable(
                parent,
            )
        val commandRef =
            "command.vexlife-prod.vision.project-test"
        val transport =
            FakeTransport(
                listOf(
                    response(
                        commandRef = commandRef,
                        commandType = "PROJECT",
                    ),
                    response(
                        commandRef = "command.vexlife-prod.vision.shutdown-test",
                        commandType = "SHUTDOWN",
                    ),
                ),
            )

        try {
            val runtime =
                VexLifeProdVisionRuntime.open(
                    executable = executable,
                    transportFactory =
                        VexLifeProdVisionRuntime.TransportFactory {
                            observed ->
                            assertEquals(
                                executable,
                                observed,
                            )
                            transport
                        },
                    commandRefSource = {
                        if (
                            transport.requests.isEmpty()
                        ) {
                            "project-test"
                        } else {
                            "shutdown-test"
                        }
                    },
                )

            val projected =
                runtime.project()

            assertEquals(
                commandRef,
                projected.commandRef,
            )
            assertEquals(
                "PROJECT",
                projected.commandType,
            )
            assertEquals(
                null,
                projected.projectionOrNull,
            )
            assertFalse(
                projected.shutdownRequested,
            )
            assertEquals(
                "{\"schemaVersion\":\"vextreme.macos-screen-selected-scope-product-runtime-command/v1\",\"commandRef\":\"$commandRef\",\"commandType\":\"PROJECT\"}",
                transport.requests.single(),
            )

            runtime.close()

            assertEquals(
                2,
                transport.requests.size,
            )
            assertEquals(
                1,
                transport.closeCalls,
            )
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun engageAndStopPreserveCallerCorrelationWithoutOwningScopeIdentity() {
        val parent =
            Files.createTempDirectory(
                "vexlife-prod-vision-engage-stop-",
            )
                .toAbsolutePath()
        val executable =
            runtimeExecutable(
                parent,
            )
        val engageRef =
            "request.vexlife-prod.vision.engage.test.001"
        val stopRef =
            "request.vexlife-prod.vision.stop.test.001"
        val shutdownRef =
            "command.vexlife-prod.vision.shutdown-test"
        val transport =
            FakeTransport(
                listOf(
                    response(
                        commandRef = engageRef,
                        commandType = "ENGAGE",
                        projectionOrNull = projection(),
                    ),
                    response(
                        commandRef = stopRef,
                        commandType = "STOP",
                        projectionOrNull =
                            projection(
                                "ENDED",
                            ),
                    ),
                    response(
                        commandRef = shutdownRef,
                        commandType = "SHUTDOWN",
                        projectionOrNull =
                            projection(
                                "ENDED",
                            ),
                    ),
                ),
            )

        try {
            val runtime =
                VexLifeProdVisionRuntime.open(
                    executable = executable,
                    transportFactory =
                        VexLifeProdVisionRuntime.TransportFactory {
                            transport
                        },
                    commandRefSource = {
                        "shutdown-test"
                    },
                )

            val engaged =
                runtime.engage(
                    engageRef,
                )
            assertEquals(
                "ENGAGED",
                engaged.projectionOrNull
                    ?.state,
            )
            assertEquals(
                "session.macos-screen-selected-scope.product.test.001",
                engaged.projectionOrNull
                    ?.scopeSessionRef,
            )
            assertFalse(
                requireNotNull(
                    engaged.projectionOrNull,
                )
                    .screenCaptureActive,
            )

            val stopped =
                runtime.stop(
                    stopRef,
                )
            assertEquals(
                "ENDED",
                stopped.projectionOrNull
                    ?.state,
            )

            assertTrue(
                transport.requests[0]
                    .contains(
                        "\"commandRef\":\"$engageRef\"",
                    ),
            )
            assertTrue(
                transport.requests[1]
                    .contains(
                        "\"commandRef\":\"$stopRef\"",
                    ),
            )

            runtime.close()
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun responseIdentityDriftFailsClosed() {
        val parent =
            Files.createTempDirectory(
                "vexlife-prod-vision-response-drift-",
            )
                .toAbsolutePath()
        val executable =
            runtimeExecutable(
                parent,
            )
        val transport =
            FakeTransport(
                listOf(
                    response(
                        commandRef = "command.vexlife-prod.vision.other",
                        commandType = "PROJECT",
                    ),
                    response(
                        commandRef = "command.vexlife-prod.vision.shutdown-test",
                        commandType = "SHUTDOWN",
                    ),
                ),
            )

        try {
            val runtime =
                VexLifeProdVisionRuntime.open(
                    executable = executable,
                    transportFactory =
                        VexLifeProdVisionRuntime.TransportFactory {
                            transport
                        },
                    commandRefSource = {
                        if (
                            transport.requests.isEmpty()
                        ) {
                            "project-test"
                        } else {
                            "shutdown-test"
                        }
                    },
                )

            assertFailsWith<IllegalStateException> {
                runtime.project()
            }

            runtime.close()
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun cooperativeShutdownFailureStillClosesOwnedTransport() {
        val parent =
            Files.createTempDirectory(
                "vexlife-prod-vision-shutdown-failure-",
            )
                .toAbsolutePath()
        val executable =
            runtimeExecutable(
                parent,
            )
        val shutdownRef =
            "command.vexlife-prod.vision.shutdown-test"
        val transport =
            FakeTransport(
                listOf(
                    response(
                        commandRef = shutdownRef,
                        commandType = "SHUTDOWN",
                        ok = false,
                        shutdownRequested = true,
                        errorCodeOrNull =
                            "MACOS_SCREEN_SELECTED_SCOPE_HOST_CUSTODY_STOP_CLOSED_WITHOUT_RELEASE",
                    ),
                ),
            )

        try {
            val runtime =
                VexLifeProdVisionRuntime.open(
                    executable = executable,
                    transportFactory =
                        VexLifeProdVisionRuntime.TransportFactory {
                            transport
                        },
                    commandRefSource = {
                        "shutdown-test"
                    },
                )

            assertFailsWith<IllegalStateException> {
                runtime.close()
            }
            assertEquals(
                1,
                transport.requests.size,
            )
            assertEquals(
                1,
                transport.closeCalls,
            )
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun oversizedInternalCommandIsRejectedBeforeTransportMutation() {
        val parent =
            Files.createTempDirectory(
                "vexlife-prod-vision-command-bound-",
            )
                .toAbsolutePath()
        val executable =
            runtimeExecutable(
                parent,
            )
        val shutdownRef =
            "command.vexlife-prod.vision.shutdown-test"
        val transport =
            FakeTransport(
                listOf(
                    response(
                        commandRef = shutdownRef,
                        commandType = "SHUTDOWN",
                    ),
                ),
            )
        var commandRefCalls =
            0

        try {
            val runtime =
                VexLifeProdVisionRuntime.open(
                    executable = executable,
                    transportFactory =
                        VexLifeProdVisionRuntime.TransportFactory {
                            transport
                        },
                    commandRefSource = {
                        commandRefCalls +=
                            1
                        if (
                            commandRefCalls ==
                                1
                        ) {
                            "x".repeat(
                                VexLifeProdVisionRuntime.MaxCommandBytes,
                            )
                        } else {
                            "shutdown-test"
                        }
                    },
                )

            assertFailsWith<IllegalStateException> {
                runtime.project()
            }
            assertEquals(
                0,
                transport.requests.size,
            )

            runtime.close()
            assertEquals(
                1,
                transport.requests.size,
            )
            assertTrue(
                transport.requests.single()
                    .contains(
                        "\"commandRef\":\"$shutdownRef\"",
                    ),
            )
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }
}

// [VXG RealForever]
