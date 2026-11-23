package vexlife.partner

import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * JVM client for the one-shot native Partner host helper.
 *
 * The helper executable receives no credential/provider values in argv. All operation metadata is
 * protocol stdin; STORE secret bytes follow the non-secret JSON header and are zeroed after write.
 * Helper stderr is drained and discarded rather than becoming Product logs/evidence.
 */
class VexLifePartnerNativeHostClient(
    private val processHost: ProcessHost,
) : VexLifePartnerCredentialCustody.ProtectedHostStore,
    VexLifePartnerHostEffect.Executor {
    data class ProcessResult(
        val exitCode: Int,
        val stdoutLines: List<String>,
    )

    fun interface ProcessHost {
        fun run(
            headerLine: String,
            payload: ByteArray,
            timeoutMs: Long,
        ): ProcessResult
    }

    class SystemProcessHost(
        executable: Path,
    ) : ProcessHost {
        private val executable =
            executable
                .toAbsolutePath()
                .normalize()
                .also {
                    require(
                        Files.isRegularFile(
                            it,
                            LinkOption.NOFOLLOW_LINKS,
                        ) &&
                            !Files.isSymbolicLink(
                                it,
                            ) &&
                            Files.isExecutable(
                                it,
                            ),
                    ) {
                        "Partner native helper must be one executable regular non-symlink file"
                    }
                }

        override fun run(
            headerLine: String,
            payload: ByteArray,
            timeoutMs: Long,
        ): ProcessResult {
            require(
                timeoutMs >
                    0,
            )
            val process =
                ProcessBuilder(
                    executable.toString(),
                )
                    .start()

            val stderrDrainer =
                Thread {
                    runCatching {
                        process
                            .errorStream
                            .use {
                                input ->
                                input.transferTo(
                                    OutputStream.nullOutputStream(),
                                )
                            }
                    }
                }
                    .apply {
                        isDaemon =
                            true
                        name =
                            "vexlife-partner-host-stderr"
                        start()
                    }
            val stdoutLines =
                java.util.Collections.synchronizedList(
                    mutableListOf<String>(),
                )
            val stdoutFailure =
                AtomicReference<Throwable?>(null)
            val stdoutDrainer =
                Thread {
                    try {
                        process
                            .inputStream
                            .bufferedReader(
                                StandardCharsets.UTF_8,
                            )
                            .use {
                                reader ->
                                while (
                                    true
                                ) {
                                    val line =
                                        reader.readLine()
                                            ?: break
                                    stdoutLines +=
                                        line
                                }
                            }
                    } catch (
                        error: Throwable,
                    ) {
                        stdoutFailure.set(
                            error,
                        )
                    }
                }
                    .apply {
                        isDaemon =
                            true
                        name =
                            "vexlife-partner-host-stdout"
                        start()
                    }

            try {
                process
                    .outputStream
                    .use {
                        output ->
                        output.write(
                            headerLine.toByteArray(
                                StandardCharsets.UTF_8,
                            ),
                        )
                        output.write(
                            payload,
                        )
                        output.flush()
                    }

                val completed =
                    process.waitFor(
                        timeoutMs,
                        TimeUnit.MILLISECONDS,
                    )
                if (
                    !completed
                ) {
                    process.destroyForcibly()
                    process.waitFor(
                        5,
                        TimeUnit.SECONDS,
                    )
                    error(
                        "Partner native helper exceeded its bounded execution timeout",
                    )
                }

                stdoutDrainer.join(
                    5_000,
                )
                stderrDrainer.join(
                    5_000,
                )
                require(
                    !stdoutDrainer.isAlive,
                ) {
                    "Partner native helper stdout did not close after process exit"
                }
                stdoutFailure
                    .get()
                    ?.let {
                        throw IllegalStateException(
                            "Partner native helper stdout could not be read",
                            it,
                        )
                    }

                return ProcessResult(
                    exitCode =
                        process.exitValue(),
                    stdoutLines =
                        synchronized(
                            stdoutLines,
                        ) {
                            stdoutLines.toList()
                        },
                )
            } finally {
                if (
                    process.isAlive
                ) {
                    process.destroyForcibly()
                }
            }
        }
    }

    override fun store(
        coordinate: VexLifePartnerCredentialCustody.Coordinate,
        secret: CharArray,
    ) {
        val (header, secretBytes) =
            VexLifePartnerNativeHostProtocol.storeHeader(
                coordinate = coordinate,
                secret = secret,
            )
        try {
            val result =
                processHost.run(
                    headerLine = header,
                    payload = secretBytes,
                    timeoutMs = CustodyTimeoutMs,
                )
            val event =
                singleEvent(
                    result = result,
                    providerRef = coordinate.providerRef,
                    credentialRef = coordinate.credentialRef,
                )
            require(
                event.type ==
                    VexLifePartnerNativeHostProtocol.EventType.STORED &&
                    event.present ==
                    true,
            ) {
                "Partner native helper did not prove credential storage"
            }
        } finally {
            secretBytes.fill(
                0,
            )
        }
    }

    override fun contains(
        coordinate: VexLifePartnerCredentialCustody.Coordinate,
    ): Boolean {
        val header =
            VexContractJsonLine.encode(
                VexLifePartnerNativeHostProtocol.ObserveHeader(
                    providerRef = coordinate.providerRef,
                    credentialRef = coordinate.credentialRef,
                ),
            )
        val result =
            processHost.run(
                headerLine = header,
                payload = EmptyPayload,
                timeoutMs = CustodyTimeoutMs,
            )
        val event =
            singleEvent(
                result = result,
                providerRef = coordinate.providerRef,
                credentialRef = coordinate.credentialRef,
            )
        require(
            event.type ==
                VexLifePartnerNativeHostProtocol.EventType.OBSERVED &&
                event.present !=
                null,
        ) {
            "Partner native helper returned invalid credential observation"
        }
        return requireNotNull(
            event.present,
        )
    }

    override fun delete(
        coordinate: VexLifePartnerCredentialCustody.Coordinate,
    ): Boolean {
        val header =
            VexContractJsonLine.encode(
                VexLifePartnerNativeHostProtocol.DeleteHeader(
                    providerRef = coordinate.providerRef,
                    credentialRef = coordinate.credentialRef,
                ),
            )
        val result =
            processHost.run(
                headerLine = header,
                payload = EmptyPayload,
                timeoutMs = CustodyTimeoutMs,
            )
        val event =
            singleEvent(
                result = result,
                providerRef = coordinate.providerRef,
                credentialRef = coordinate.credentialRef,
            )
        require(
            event.type ==
                VexLifePartnerNativeHostProtocol.EventType.DELETED &&
                event.present ==
                false,
        ) {
            "Partner native helper did not prove credential retirement"
        }
        return true
    }

    override fun execute(
        plan: VexLifePartnerNetworkPlan.Plan,
    ): VexLifePartnerHostEffect.Result {
        val (header, bodyBytes) =
            VexLifePartnerNativeHostProtocol.executeHeader(
                plan,
            )
        val process =
            try {
                processHost.run(
                    headerLine = header,
                    payload = bodyBytes,
                    timeoutMs = RequestTimeoutMs,
                )
            } finally {
                bodyBytes.fill(
                    0,
                )
            }

        require(
            process.exitCode ==
                0,
        ) {
            "Partner native helper failed without a valid terminal protocol result"
        }
        require(
            process.stdoutLines.isNotEmpty(),
        ) {
            "Partner native helper returned no protocol events"
        }

        val events =
            process.stdoutLines.map(
                VexLifePartnerNativeHostProtocol::decodeEvent,
            )
        events.forEach {
            requireEventCoordinate(
                event = it,
                providerRef = plan.providerRef,
                credentialRef = plan.credentialRef,
            )
        }

        val failure =
            events
                .filter {
                    it.type ==
                        VexLifePartnerNativeHostProtocol.EventType.TRANSPORT_FAILURE
                }
                .singleOrNull()
        if (
            failure !=
                null
        ) {
            require(
                events.last() ==
                    failure &&
                    events.none {
                        it.type ==
                            VexLifePartnerNativeHostProtocol.EventType.COMPLETED
                    },
            ) {
                "Partner native helper transport failure must be terminal"
            }
            return VexLifePartnerHostEffect.requireExact(
                plan = plan,
                result =
                    VexLifePartnerHostEffect.Result.TransportFailure(
                        code =
                            requireNotNull(
                                failure.failureCode,
                            ),
                        detail =
                            requireNotNull(
                                failure.detail,
                            ),
                        receipt =
                            VexLifePartnerHostEffect.receiptForTransportFailure(
                                plan = plan,
                                networkAttempted =
                                    failure.failureCode !in
                                        setOf(
                                            VexLifePartnerHostEffect.FailureCode.CREDENTIAL_NOT_PRESENT,
                                            VexLifePartnerHostEffect.FailureCode.CREDENTIAL_PROVIDER_MISMATCH,
                                            VexLifePartnerHostEffect.FailureCode.HOST_UNAVAILABLE,
                                        ),
                            ),
                    ),
            )
        }

        val start =
            events
                .filter {
                    it.type ==
                        VexLifePartnerNativeHostProtocol.EventType.RESPONSE_START
                }
                .singleOrNull()
                ?: error(
                    "Partner native helper response has no single RESPONSE_START event",
                )
        val completed =
            events
                .filter {
                    it.type ==
                        VexLifePartnerNativeHostProtocol.EventType.COMPLETED
                }
                .singleOrNull()
                ?: error(
                    "Partner native helper response has no single COMPLETED event",
                )
        require(
            events.last() ==
                completed,
        ) {
            "Partner native helper COMPLETED event must be terminal"
        }

        val bodyBytesObserved =
            events
                .filter {
                    it.type ==
                        VexLifePartnerNativeHostProtocol.EventType.BODY_CHUNK
                }
                .map(
                    VexLifePartnerNativeHostProtocol::decodeBodyChunk,
                )
        val total =
            bodyBytesObserved.sumOf {
                it.size
            }
        val bodyBytes =
            ByteArray(
                total,
            )
        var offset =
            0
        bodyBytesObserved.forEach {
            chunk ->
            chunk.copyInto(
                destination = bodyBytes,
                destinationOffset = offset,
            )
            offset +=
                chunk.size
            chunk.fill(
                0,
            )
        }

        val body =
            String(
                bodyBytes,
                StandardCharsets.UTF_8,
            )
        bodyBytes.fill(
            0,
        )

        require(
            sha256(
                body,
            ) ==
                completed.responseBodySha256,
        ) {
            "Partner native helper response body does not match its terminal digest"
        }

        return VexLifePartnerHostEffect.requireExact(
            plan = plan,
            result =
                VexLifePartnerHostEffect.Result.Http(
                    status =
                        requireNotNull(
                            start.httpStatus,
                        ),
                    body = body,
                    retryAfter = start.retryAfter,
                    providerRequestId = start.providerRequestId,
                    receipt =
                        VexLifePartnerHostEffect.receiptForHttp(
                            plan = plan,
                            status =
                                requireNotNull(
                                    start.httpStatus,
                                ),
                            body = body,
                        ),
                ),
        )
    }

    private fun singleEvent(
        result: ProcessResult,
        providerRef: String,
        credentialRef: String,
    ): VexLifePartnerNativeHostProtocol.Event {
        require(
            result.exitCode ==
                0 &&
                result.stdoutLines.size ==
                1,
        ) {
            "Partner native helper custody operation did not return one successful protocol event"
        }
        val event =
            VexLifePartnerNativeHostProtocol.decodeEvent(
                result.stdoutLines.single(),
            )
        requireEventCoordinate(
            event = event,
            providerRef = providerRef,
            credentialRef = credentialRef,
        )
        return event
    }

    private fun requireEventCoordinate(
        event: VexLifePartnerNativeHostProtocol.Event,
        providerRef: String,
        credentialRef: String,
    ) {
        require(
            event.providerRef ==
                providerRef &&
                event.credentialRef ==
                credentialRef &&
                !event.secretProjected &&
                !event.redirectFollowed,
        ) {
            "Partner native helper event escaped its exact provider/credential coordinate"
        }
    }

    private fun sha256(
        value: String,
    ): String =
        MessageDigest.getInstance(
            "SHA-256",
        )
            .digest(
                value.toByteArray(
                    StandardCharsets.UTF_8,
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

    private object VexContractJsonLine {
        inline fun <reified T> encode(
            value: T,
        ): String =
            vexlife.contract.VexContractJson.compactEncode(
                value,
            ) +
                "\n"
    }

    private companion object {
        const val CustodyTimeoutMs =
            30_000L
        const val RequestTimeoutMs =
            620_000L

        val EmptyPayload =
            ByteArray(
                0,
            )
    }
}

// [VXG RealForever]
