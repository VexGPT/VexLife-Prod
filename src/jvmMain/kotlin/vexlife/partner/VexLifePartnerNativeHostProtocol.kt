package vexlife.partner

import java.nio.CharBuffer
import java.nio.charset.StandardCharsets
import java.util.Base64
import kotlinx.serialization.Serializable
import vexlife.contract.VexContractJson

/**
 * Source-owned one-shot process protocol for a native Partner host helper.
 *
 * Secret bytes are never serialized into the JSON protocol. STORE writes exactly [StoreHeader.secretBytes]
 * raw bytes after the header line. EXECUTE writes exactly [ExecuteHeader.requestBodyBytes] raw UTF-8 body
 * bytes after the header line.
 *
 * Helper stdout is NDJSON and contains only non-secret observations/events.
 */
object VexLifePartnerNativeHostProtocol {
    const val Schema =
        "vexlife-prod.partner-native-host/v1"

    @Serializable
    enum class Operation {
        STORE,
        OBSERVE,
        DELETE,
        EXECUTE,
    }

    @Serializable
    data class StoreHeader(
        val schemaVersion: String = Schema,
        val operation: Operation = Operation.STORE,
        val providerRef: String,
        val credentialRef: String,
        val secretBytes: Int,
    ) {
        init {
            requireCoordinate(
                providerRef = providerRef,
                credentialRef = credentialRef,
            )
            require(
                secretBytes in
                    1..16_384,
            ) {
                "native Partner STORE requires one bounded secret byte count"
            }
        }
    }

    @Serializable
    data class ObserveHeader(
        val schemaVersion: String = Schema,
        val operation: Operation = Operation.OBSERVE,
        val providerRef: String,
        val credentialRef: String,
    ) {
        init {
            requireCoordinate(
                providerRef = providerRef,
                credentialRef = credentialRef,
            )
        }
    }

    @Serializable
    data class DeleteHeader(
        val schemaVersion: String = Schema,
        val operation: Operation = Operation.DELETE,
        val providerRef: String,
        val credentialRef: String,
    ) {
        init {
            requireCoordinate(
                providerRef = providerRef,
                credentialRef = credentialRef,
            )
        }
    }

    /**
     * Non-secret metadata for one provider execution.
     *
     * The native helper must resolve [providerRef] through its own compiled/source-generated built-in
     * provider allowlist and reject any mismatch with [expectedRequestUrl], dialect or public headers.
     */
    @Serializable
    data class ExecuteHeader(
        val schemaVersion: String = Schema,
        val operation: Operation = Operation.EXECUTE,
        val providerRef: String,
        val credentialRef: String,
        val expectedRequestUrl: String,
        val dialect: VexLifePartnerProviderCatalog.Dialect,
        val publicProtocolHeaders: Map<String, String>,
        val requestBodySha256: String,
        val requestBodyBytes: Int,
        val redirectPolicy: VexLifePartnerNetworkPlan.RedirectPolicy =
            VexLifePartnerNetworkPlan.RedirectPolicy.DENY,
    ) {
        init {
            requireCoordinate(
                providerRef = providerRef,
                credentialRef = credentialRef,
            )
            require(
                expectedRequestUrl.startsWith(
                    "https://",
                ),
            ) {
                "native Partner EXECUTE requires one expected HTTPS request URL"
            }
            require(
                requestBodySha256.matches(
                    Sha256,
                ),
            ) {
                "native Partner EXECUTE body digest must be SHA-256"
            }
            require(
                requestBodyBytes >
                    0,
            ) {
                "native Partner EXECUTE requires one non-empty request body"
            }
            require(
                publicProtocolHeaders.keys.none {
                    it.equals(
                        "authorization",
                        ignoreCase = true,
                    ) ||
                        it.equals(
                            "x-api-key",
                            ignoreCase = true,
                        )
                },
            ) {
                "native Partner protocol metadata may not carry credential-bearing headers"
            }
            require(
                redirectPolicy ==
                    VexLifePartnerNetworkPlan.RedirectPolicy.DENY,
            ) {
                "native Partner process protocol admits redirect denial only"
            }
        }
    }

    @Serializable
    enum class EventType {
        STORED,
        OBSERVED,
        DELETED,
        RESPONSE_START,
        BODY_CHUNK,
        COMPLETED,
        TRANSPORT_FAILURE,
    }

    @Serializable
    data class Event(
        val schemaVersion: String = Schema,
        val type: EventType,
        val providerRef: String,
        val credentialRef: String,
        val present: Boolean? = null,
        val httpStatus: Int? = null,
        val retryAfter: String? = null,
        val providerRequestId: String? = null,
        val bodyChunkBase64: String? = null,
        val responseBodySha256: String? = null,
        val failureCode: VexLifePartnerHostEffect.FailureCode? = null,
        val detail: String? = null,
        val redirectFollowed: Boolean = false,
        val secretProjected: Boolean = false,
    ) {
        init {
            requireCoordinate(
                providerRef = providerRef,
                credentialRef = credentialRef,
            )
            require(
                !redirectFollowed,
            ) {
                "native Partner helper event may not claim a followed redirect"
            }
            require(
                !secretProjected,
            ) {
                "native Partner helper event may not project secret material"
            }
            httpStatus
                ?.let {
                    require(
                        it in
                            100..599 &&
                            it !in
                            300..399,
                    ) {
                        "native Partner helper event may not accept redirects as HTTP responses"
                    }
                }
            responseBodySha256
                ?.let {
                    require(
                        it.matches(
                            Sha256,
                        ),
                    )
                }
            bodyChunkBase64
                ?.let {
                    require(
                        it.length <=
                            4 * 1024 * 1024,
                    ) {
                        "native Partner body-chunk event exceeds the bounded frame size"
                    }
                    runCatching {
                        Base64
                            .getDecoder()
                            .decode(
                                it,
                            )
                    }
                        .getOrElse {
                            throw IllegalArgumentException(
                                "native Partner body chunk is not valid Base64",
                                it,
                            )
                        }
                }

            when (
                type
            ) {
                EventType.STORED,
                EventType.DELETED,
                ->
                    require(
                        present !=
                            null &&
                            httpStatus ==
                            null &&
                            bodyChunkBase64 ==
                            null &&
                            failureCode ==
                            null
                    )

                EventType.OBSERVED ->
                    require(
                        present !=
                            null &&
                            httpStatus ==
                            null &&
                            bodyChunkBase64 ==
                            null &&
                            failureCode ==
                            null
                    )

                EventType.RESPONSE_START ->
                    require(
                        httpStatus !=
                            null &&
                            bodyChunkBase64 ==
                            null &&
                            failureCode ==
                            null
                    )

                EventType.BODY_CHUNK ->
                    require(
                        bodyChunkBase64 !=
                            null &&
                            httpStatus ==
                            null &&
                            failureCode ==
                            null
                    )

                EventType.COMPLETED ->
                    require(
                        responseBodySha256 !=
                            null &&
                            failureCode ==
                            null
                    )

                EventType.TRANSPORT_FAILURE ->
                    require(
                        failureCode !=
                            null &&
                            !detail.isNullOrBlank() &&
                            httpStatus ==
                            null &&
                            bodyChunkBase64 ==
                            null
                    )
            }
        }
    }

    fun storeHeader(
        coordinate: VexLifePartnerCredentialCustody.Coordinate,
        secret: CharArray,
    ): Pair<String, ByteArray> {
        require(
            secret.isNotEmpty(),
        ) {
            "native Partner STORE requires one non-empty secret"
        }
        val encoded =
            StandardCharsets.UTF_8
                .newEncoder()
                .encode(
                    CharBuffer.wrap(
                        secret,
                    ),
                )
        val bytes =
            ByteArray(
                encoded.remaining(),
            )
        encoded.get(
            bytes,
        )
        if (
            encoded.hasArray()
        ) {
            encoded
                .array()
                .fill(
                    0,
                )
        }
        val header =
            StoreHeader(
                providerRef = coordinate.providerRef,
                credentialRef = coordinate.credentialRef,
                secretBytes = bytes.size,
            )
        return line(
            header,
        ) to
            bytes
    }

    fun executeHeader(
        plan: VexLifePartnerNetworkPlan.Plan,
    ): Pair<String, ByteArray> {
        val bytes =
            plan.requestBody.toByteArray(
                StandardCharsets.UTF_8,
            )
        val header =
            ExecuteHeader(
                providerRef = plan.providerRef,
                credentialRef = plan.credentialRef,
                expectedRequestUrl = plan.requestUrl,
                dialect = plan.dialect,
                publicProtocolHeaders = plan.protocolHeaders,
                requestBodySha256 = plan.requestBodySha256,
                requestBodyBytes = bytes.size,
                redirectPolicy = plan.redirectPolicy,
            )
        return line(
            header,
        ) to
            bytes
    }

    fun decodeEvent(
        line: String,
    ): Event =
        VexContractJson.decode(
            line,
        )

    fun bodyChunk(
        bytes: ByteArray,
        providerRef: String,
        credentialRef: String,
    ): Event =
        Event(
            type = EventType.BODY_CHUNK,
            providerRef = providerRef,
            credentialRef = credentialRef,
            bodyChunkBase64 =
                Base64
                    .getEncoder()
                    .encodeToString(
                        bytes,
                    ),
        )

    fun decodeBodyChunk(
        event: Event,
    ): ByteArray {
        require(
            event.type ==
                EventType.BODY_CHUNK,
        )
        return Base64
            .getDecoder()
            .decode(
                requireNotNull(
                    event.bodyChunkBase64,
                ),
            )
    }

    private inline fun <reified T> line(
        value: T,
    ): String =
        VexContractJson.compactEncode(
            value,
        ) +
            "\n"

    private fun requireCoordinate(
        providerRef: String,
        credentialRef: String,
    ) {
        requireNotNull(
            VexLifePartnerProviderCatalog.resolve(
                providerRef,
            ),
        ) {
            "native Partner protocol requires one source-owned built-in provider"
        }
        VexLifePartnerCredentialCustody.Coordinate(
            providerRef = providerRef,
            credentialRef = credentialRef,
        )
    }

    private val Sha256 =
        Regex(
            "[0-9a-f]{64}",
        )
}

// [VXG RealForever]
