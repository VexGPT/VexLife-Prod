package vexlife.partner

import java.nio.charset.StandardCharsets
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.serialization.json.jsonObject
import vexlife.contract.VexContractJson

class VexLifePartnerNativeHostProtocolTest {
    private fun coordinate(
        providerRef: String =
            VexLifePartnerProviderCatalog.OpenAiRef,
    ): VexLifePartnerCredentialCustody.Coordinate =
        VexLifePartnerCredentialCustody.Coordinate(
            providerRef = providerRef,
            credentialRef =
                "credential.vexlife.partner.native-host-fixture",
        )

    @Test
    fun storeHeaderCarriesOnlyByteCountWhileSecretIsRawPayload() {
        val secret =
            "synthetic-provider-secret"
                .toCharArray()
        val (headerLine, payload) =
            VexLifePartnerNativeHostProtocol.storeHeader(
                coordinate = coordinate(),
                secret = secret,
            )
        val header =
            VexContractJson.Strict
                .parseToJsonElement(
                    headerLine.trimEnd(),
                )
                .jsonObject

        assertEquals(
            secret
                .concatToString()
                .toByteArray(
                    StandardCharsets.UTF_8,
                )
                .size,
            header
                .getValue("secretBytes")
                .toString()
                .toInt(),
        )
        assertFalse(
            headerLine.contains(
                "synthetic-provider-secret",
            ),
        )
        assertEquals(
            "synthetic-provider-secret",
            payload.toString(
                StandardCharsets.UTF_8,
            ),
        )
    }

    @Test
    fun executeHeaderCarriesNoCredentialValueAndBindsExactBodyLength() {
        val plan =
            VexLifePartnerNetworkPlan.form(
                providerRef =
                    VexLifePartnerProviderCatalog.AnthropicRef,
                credential =
                    coordinate(
                        VexLifePartnerProviderCatalog.AnthropicRef,
                    ),
                requestBody =
                    """{"model":"claude-fixture","stream":true}""",
            )
        val (headerLine, body) =
            VexLifePartnerNativeHostProtocol.executeHeader(
                plan,
            )

        assertFalse(
            headerLine.contains(
                "Bearer ",
                ignoreCase = true,
            ),
        )
        assertFalse(
            headerLine.contains(
                "x-api-key",
                ignoreCase = true,
            ),
        )
        assertEquals(
            plan.requestBody,
            body.toString(
                StandardCharsets.UTF_8,
            ),
        )
        assertTrue(
            headerLine.contains(
                "\"expectedRequestUrl\":\"https://api.anthropic.com/v1/messages\"",
            ),
        )
        assertTrue(
            headerLine.contains(
                "\"requestBodyBytes\":" +
                    body.size,
            ),
        )
    }

    @Test
    fun bodyChunkFramingRoundTripsArbitraryUtf8BytesWithoutSecretSemantics() {
        val bytes =
            "data: {\"type\":\"response.output_text.delta\",\"delta\":\"hello 世界\"}\n"
                .toByteArray(
                    StandardCharsets.UTF_8,
                )
        val event =
            VexLifePartnerNativeHostProtocol.bodyChunk(
                bytes = bytes,
                providerRef =
                    VexLifePartnerProviderCatalog.OpenAiRef,
                credentialRef =
                    coordinate().credentialRef,
            )

        val encoded =
            VexContractJson.compactEncode(
                event,
            )
        val decoded =
            VexLifePartnerNativeHostProtocol.decodeEvent(
                encoded,
            )

        assertEquals(
            VexLifePartnerNativeHostProtocol.EventType.BODY_CHUNK,
            decoded.type,
        )
        assertTrue(
            bytes.contentEquals(
                VexLifePartnerNativeHostProtocol.decodeBodyChunk(
                    decoded,
                ),
            ),
        )
    }

    @Test
    fun protocolRejectsCredentialBearingPublicHeaders() {
        kotlin.test.assertFailsWith<IllegalArgumentException> {
            VexLifePartnerNativeHostProtocol.ExecuteHeader(
                providerRef =
                    VexLifePartnerProviderCatalog.OpenAiRef,
                credentialRef =
                    coordinate().credentialRef,
                expectedRequestUrl =
                    VexLifePartnerProviderCatalog.OpenAi.inferenceUrl,
                dialect =
                    VexLifePartnerProviderCatalog.Dialect.OPENAI_RESPONSES,
                publicProtocolHeaders =
                    mapOf(
                        "Authorization" to
                            "Bearer should-not-be-here",
                    ),
                requestBodySha256 =
                    "a".repeat(
                        64,
                    ),
                requestBodyBytes =
                    2,
            )
        }
    }

    @Test
    fun redirectCannotBeRepresentedAsAcceptedResponseStart() {
        kotlin.test.assertFailsWith<IllegalArgumentException> {
            VexLifePartnerNativeHostProtocol.Event(
                type =
                    VexLifePartnerNativeHostProtocol.EventType.RESPONSE_START,
                providerRef =
                    VexLifePartnerProviderCatalog.KimiInternationalRef,
                credentialRef =
                    coordinate(
                        VexLifePartnerProviderCatalog.KimiInternationalRef,
                    ).credentialRef,
                httpStatus =
                    302,
            )
        }
    }

    @Test
    fun transportFailureEventCarriesNoHttpResponseOrBodyChunk() {
        val event =
            VexLifePartnerNativeHostProtocol.Event(
                type =
                    VexLifePartnerNativeHostProtocol.EventType.TRANSPORT_FAILURE,
                providerRef =
                    VexLifePartnerProviderCatalog.OpenAiRef,
                credentialRef =
                    coordinate().credentialRef,
                failureCode =
                    VexLifePartnerHostEffect.FailureCode.TLS_FAILURE,
                detail =
                    "TLS handshake failed",
            )

        assertEquals(
            null,
            event.httpStatus,
        )
        assertEquals(
            null,
            event.bodyChunkBase64,
        )
        assertFalse(
            event.secretProjected,
        )
        assertFalse(
            event.redirectFollowed,
        )
    }
}

// [VXG RealForever]
