package vexlife.partner

import java.nio.charset.StandardCharsets
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import vexlife.contract.VexContractJson

class VexLifePartnerNativeHostClientTest {
    private class FakeProcessHost :
        VexLifePartnerNativeHostClient.ProcessHost {
        data class Call(
            val headerLine: String,
            val payload: ByteArray,
            val timeoutMs: Long,
        )

        val calls =
            mutableListOf<Call>()
        var responder: (Call) -> VexLifePartnerNativeHostClient.ProcessResult = {
            error(
                "fake native Partner host responder is not configured",
            )
        }

        override fun run(
            headerLine: String,
            payload: ByteArray,
            timeoutMs: Long,
        ): VexLifePartnerNativeHostClient.ProcessResult {
            val call =
                Call(
                    headerLine = headerLine,
                    payload = payload.copyOf(),
                    timeoutMs = timeoutMs,
                )
            calls +=
                call
            return responder(
                call,
            )
        }
    }

    private fun coordinate(
        providerRef: String =
            VexLifePartnerProviderCatalog.OpenAiRef,
    ): VexLifePartnerCredentialCustody.Coordinate =
        VexLifePartnerCredentialCustody.Coordinate(
            providerRef = providerRef,
            credentialRef =
                "credential.vexlife.partner.native-client-fixture",
        )

    private fun eventLine(
        event: VexLifePartnerNativeHostProtocol.Event,
    ): String =
        VexContractJson.compactEncode(
            event,
        )

    private fun plan(): VexLifePartnerNetworkPlan.Plan =
        VexLifePartnerNetworkPlan.form(
            providerRef =
                VexLifePartnerProviderCatalog.OpenAiRef,
            credential =
                coordinate(),
            requestBody =
                """{"model":"fixture","stream":true}""",
        )

    @Test
    fun storeSendsSecretOnlyAsRawPayloadNotHeader() {
        val host =
            FakeProcessHost()
        host.responder = {
            call ->
            val header =
                VexContractJson.decode<VexLifePartnerNativeHostProtocol.StoreHeader>(
                    call.headerLine.trimEnd(),
                )
            VexLifePartnerNativeHostClient.ProcessResult(
                exitCode = 0,
                stdoutLines =
                    listOf(
                        eventLine(
                            VexLifePartnerNativeHostProtocol.Event(
                                type =
                                    VexLifePartnerNativeHostProtocol.EventType.STORED,
                                providerRef =
                                    header.providerRef,
                                credentialRef =
                                    header.credentialRef,
                                present =
                                    true,
                            ),
                        ),
                    ),
            )
        }
        val client =
            VexLifePartnerNativeHostClient(
                host,
            )

        client.store(
            coordinate = coordinate(),
            secret =
                "synthetic-secret"
                    .toCharArray(),
        )

        val call =
            host.calls.single()
        assertFalse(
            call.headerLine.contains(
                "synthetic-secret",
            ),
        )
        assertEquals(
            "synthetic-secret",
            String(
                call.payload,
                StandardCharsets.UTF_8,
            ),
        )
    }

    @Test
    fun observeAndDeleteUseNoPayloadAndReturnExactPresence() {
        val host =
            FakeProcessHost()
        var present =
            true
        host.responder = {
            call ->
            val element =
                VexContractJson.Strict
                    .parseToJsonElement(
                        call.headerLine.trimEnd(),
                    )
                    .jsonObject
            val operation =
                element
                    .getValue("operation")
                    .jsonPrimitive
                    .content
            val providerRef =
                element
                    .getValue("providerRef")
                    .jsonPrimitive
                    .content
            val credentialRef =
                element
                    .getValue("credentialRef")
                    .jsonPrimitive
                    .content

            when (
                operation
            ) {
                "OBSERVE" ->
                    VexLifePartnerNativeHostClient.ProcessResult(
                        exitCode = 0,
                        stdoutLines =
                            listOf(
                                eventLine(
                                    VexLifePartnerNativeHostProtocol.Event(
                                        type =
                                            VexLifePartnerNativeHostProtocol.EventType.OBSERVED,
                                        providerRef = providerRef,
                                        credentialRef = credentialRef,
                                        present = present,
                                    ),
                                ),
                            ),
                    )

                "DELETE" -> {
                    present =
                        false
                    VexLifePartnerNativeHostClient.ProcessResult(
                        exitCode = 0,
                        stdoutLines =
                            listOf(
                                eventLine(
                                    VexLifePartnerNativeHostProtocol.Event(
                                        type =
                                            VexLifePartnerNativeHostProtocol.EventType.DELETED,
                                        providerRef = providerRef,
                                        credentialRef = credentialRef,
                                        present = false,
                                    ),
                                ),
                            ),
                    )
                }

                else ->
                    error(
                        "unexpected fake operation",
                    )
            }
        }

        val client =
            VexLifePartnerNativeHostClient(
                host,
            )
        assertTrue(
            client.contains(
                coordinate(),
            ),
        )
        assertTrue(
            client.delete(
                coordinate(),
            ),
        )
        assertFalse(
            client.contains(
                coordinate(),
            ),
        )
        assertTrue(
            host.calls.all {
                it.payload.isEmpty()
            },
        )
    }

    @Test
    fun executeReassemblesChunksAndReturnsProviderHttpErrorAsHttpResult() {
        val host =
            FakeProcessHost()
        host.responder = {
            call ->
            val header =
                VexContractJson.decode<VexLifePartnerNativeHostProtocol.ExecuteHeader>(
                    call.headerLine.trimEnd(),
                )
            val body =
                """{"error":{"type":"rate_limit_error","message":"fixture"}}"""
            val bytes =
                body.toByteArray(
                    StandardCharsets.UTF_8,
                )
            val first =
                bytes.copyOfRange(
                    0,
                    bytes.size /
                        2,
                )
            val second =
                bytes.copyOfRange(
                    bytes.size /
                        2,
                    bytes.size,
                )
            val digest =
                java.security.MessageDigest.getInstance(
                    "SHA-256",
                )
                    .digest(
                        bytes,
                    )
                    .joinToString(
                        "",
                    ) {
                        "%02x".format(
                            it.toInt() and
                                0xff,
                        )
                    }

            VexLifePartnerNativeHostClient.ProcessResult(
                exitCode = 0,
                stdoutLines =
                    listOf(
                        eventLine(
                            VexLifePartnerNativeHostProtocol.Event(
                                type =
                                    VexLifePartnerNativeHostProtocol.EventType.RESPONSE_START,
                                providerRef = header.providerRef,
                                credentialRef = header.credentialRef,
                                httpStatus = 429,
                                retryAfter = "2",
                                providerRequestId = "req_fixture",
                            ),
                        ),
                        eventLine(
                            VexLifePartnerNativeHostProtocol.bodyChunk(
                                bytes = first,
                                providerRef = header.providerRef,
                                credentialRef = header.credentialRef,
                            ),
                        ),
                        eventLine(
                            VexLifePartnerNativeHostProtocol.bodyChunk(
                                bytes = second,
                                providerRef = header.providerRef,
                                credentialRef = header.credentialRef,
                            ),
                        ),
                        eventLine(
                            VexLifePartnerNativeHostProtocol.Event(
                                type =
                                    VexLifePartnerNativeHostProtocol.EventType.COMPLETED,
                                providerRef = header.providerRef,
                                credentialRef = header.credentialRef,
                                responseBodySha256 = digest,
                            ),
                        ),
                    ),
            )
        }

        val result =
            assertIs<VexLifePartnerHostEffect.Result.Http>(
                VexLifePartnerNativeHostClient(
                    host,
                )
                    .execute(
                        plan(),
                    ),
            )

        assertEquals(
            429,
            result.status,
        )
        assertEquals(
            "2",
            result.retryAfter,
        )
        assertEquals(
            "req_fixture",
            result.providerRequestId,
        )
        assertTrue(
            result.body.contains(
                "rate_limit_error",
            ),
        )
    }

    @Test
    fun terminalTransportFailureDoesNotBecomeProviderHttpResponse() {
        val host =
            FakeProcessHost()
        host.responder = {
            call ->
            val header =
                VexContractJson.decode<VexLifePartnerNativeHostProtocol.ExecuteHeader>(
                    call.headerLine.trimEnd(),
                )
            VexLifePartnerNativeHostClient.ProcessResult(
                exitCode = 0,
                stdoutLines =
                    listOf(
                        eventLine(
                            VexLifePartnerNativeHostProtocol.Event(
                                type =
                                    VexLifePartnerNativeHostProtocol.EventType.TRANSPORT_FAILURE,
                                providerRef = header.providerRef,
                                credentialRef = header.credentialRef,
                                failureCode =
                                    VexLifePartnerHostEffect.FailureCode.TLS_FAILURE,
                                detail =
                                    "TLS fixture failure",
                            ),
                        ),
                    ),
            )
        }

        val result =
            assertIs<VexLifePartnerHostEffect.Result.TransportFailure>(
                VexLifePartnerNativeHostClient(
                    host,
                )
                    .execute(
                        plan(),
                    ),
            )
        assertEquals(
            VexLifePartnerHostEffect.FailureCode.TLS_FAILURE,
            result.code,
        )
        assertEquals(
            null,
            result.receipt.httpStatus,
        )
    }

    @Test
    fun mismatchedProviderEventIsRejected() {
        val host =
            FakeProcessHost()
        host.responder = {
            VexLifePartnerNativeHostClient.ProcessResult(
                exitCode = 0,
                stdoutLines =
                    listOf(
                        eventLine(
                            VexLifePartnerNativeHostProtocol.Event(
                                type =
                                    VexLifePartnerNativeHostProtocol.EventType.TRANSPORT_FAILURE,
                                providerRef =
                                    VexLifePartnerProviderCatalog.AnthropicRef,
                                credentialRef =
                                    coordinate().credentialRef,
                                failureCode =
                                    VexLifePartnerHostEffect.FailureCode.HOST_UNAVAILABLE,
                                detail =
                                    "wrong provider fixture",
                            ),
                        ),
                    ),
            )
        }

        kotlin.test.assertFailsWith<IllegalArgumentException> {
            VexLifePartnerNativeHostClient(
                host,
            )
                .execute(
                    plan(),
                )
        }
    }
}

// [VXG RealForever]
