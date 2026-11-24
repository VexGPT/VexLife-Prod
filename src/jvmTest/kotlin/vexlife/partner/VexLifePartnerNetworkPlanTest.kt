package vexlife.partner

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import vexlife.companion.CompanionProviderMessage
import vexlife.partner.ExternalProviderWire.PreparedRequest

class VexLifePartnerNetworkPlanTest {
    private fun prepared(
        model: String,
    ): PreparedRequest =
        PreparedRequest(
            model = model,
            messages =
                listOf(
                    CompanionProviderMessage(
                        role = "system",
                        content = "You are Vex.",
                    ),
                    CompanionProviderMessage(
                        role = "user",
                        content = "Hello.",
                    ),
                ),
            maxOutputTokens = 256,
        )

    private fun coordinate(
        providerRef: String,
        suffix: String,
    ): VexLifePartnerCredentialCustody.Coordinate =
        VexLifePartnerCredentialCustody.Coordinate(
            providerRef = providerRef,
            credentialRef =
                "credential.vexlife.partner." +
                    suffix,
        )

    @Test
    fun openAiPlanBindsExactBuiltInOriginWithoutSecretHeader() {
        val body =
            OpenAiResponsesWire.encodeRequest(
                prepared(
                    "gpt-fixture",
                ),
            )
        val plan =
            VexLifePartnerNetworkPlan.form(
                providerRef =
                    VexLifePartnerProviderCatalog.OpenAiRef,
                credential =
                    coordinate(
                        VexLifePartnerProviderCatalog.OpenAiRef,
                        "openai-fixture-0001",
                    ),
                requestBody = body,
            )

        assertEquals(
            "https://api.openai.com/v1/responses",
            plan.requestUrl,
        )
        assertEquals(
            VexLifePartnerProviderCatalog.Dialect.OPENAI_RESPONSES,
            plan.dialect,
        )
        assertEquals(
            VexLifePartnerNetworkPlan.Method.POST,
            plan.method,
        )
        assertEquals(
            VexLifePartnerNetworkPlan.RedirectPolicy.DENY,
            plan.redirectPolicy,
        )
        assertEquals(
            "application/json",
            plan.protocolHeaders[
                "content-type"
            ],
        )
        assertFalse(
            plan.protocolHeaders.keys.any {
                it.equals(
                    "authorization",
                    ignoreCase = true,
                ) ||
                    it.equals(
                        "x-api-key",
                        ignoreCase = true,
                    )
            },
        )
        assertTrue(
            plan.requestBodySha256.matches(
                Regex(
                    "[0-9a-f]{64}",
                ),
            ),
        )
        assertEquals(
            body,
            plan.requestBody,
        )
    }

    @Test
    fun anthropicPlanCarriesOnlyPublicProtocolVersionBeforeCredentialResolution() {
        val plan =
            VexLifePartnerNetworkPlan.form(
                providerRef =
                    VexLifePartnerProviderCatalog.AnthropicRef,
                credential =
                    coordinate(
                        VexLifePartnerProviderCatalog.AnthropicRef,
                        "anthropic-fixture-0001",
                    ),
                requestBody =
                    AnthropicMessagesWire.encodeRequest(
                        prepared(
                            "claude-fixture",
                        ),
                    ),
            )

        assertEquals(
            "https://api.anthropic.com/v1/messages",
            plan.requestUrl,
        )
        assertEquals(
            "2026-10-21",
            plan.protocolHeaders[
                "anthropic-version"
            ],
        )
        assertEquals(
            VexLifePartnerProviderCatalog.AccountScopeSelection.WORKSPACE_ID_IF_CREDENTIAL_NOT_SCOPED,
            plan.accountScopeSelection,
        )
        assertFalse(
            plan.protocolHeaders.containsKey(
                "authorization",
            ),
        )
    }

    @Test
    fun anthropicUnscopedCredentialMayAddOneNonSecretWorkspaceHeader() {
        val plan =
            VexLifePartnerNetworkPlan.form(
                providerRef =
                    VexLifePartnerProviderCatalog.AnthropicRef,
                credential =
                    coordinate(
                        VexLifePartnerProviderCatalog.AnthropicRef,
                        "anthropic-fixture-0001",
                    ),
                requestBody =
                    AnthropicMessagesWire.encodeRequest(
                        prepared(
                            "claude-fixture",
                        ),
                    ),
                accountScope =
                    VexLifePartnerAccountScope.Selection(
                        providerRef =
                            VexLifePartnerProviderCatalog.AnthropicRef,
                        workspaceId =
                            "wrkspc_fixture123",
                    ),
            )

        assertEquals(
            "wrkspc_fixture123",
            plan.protocolHeaders[
                "anthropic-workspace-id"
            ],
        )
        assertFalse(
            plan.protocolHeaders.containsKey(
                "authorization",
            ),
        )
    }

    @Test
    fun accountScopeCannotCrossProviderBoundary() {
        assertFailsWith<IllegalArgumentException> {
            VexLifePartnerNetworkPlan.form(
                providerRef =
                    VexLifePartnerProviderCatalog.OpenAiRef,
                credential =
                    coordinate(
                        VexLifePartnerProviderCatalog.OpenAiRef,
                        "openai-fixture-0001",
                    ),
                requestBody =
                    "{}",
                accountScope =
                    VexLifePartnerAccountScope.Selection(
                        providerRef =
                            VexLifePartnerProviderCatalog.AnthropicRef,
                        workspaceId =
                            "wrkspc_fixture123",
                    ),
            )
        }
    }

    @Test
    fun kimiPlanUsesInternationalOpenPlatformNotKimiCode() {
        val plan =
            VexLifePartnerNetworkPlan.form(
                providerRef =
                    VexLifePartnerProviderCatalog.KimiInternationalRef,
                credential =
                    coordinate(
                        VexLifePartnerProviderCatalog.KimiInternationalRef,
                        "kimi-fixture-0001",
                    ),
                requestBody =
                    KimiChatWire.encodeRequest(
                        prepared(
                            "kimi-fixture",
                        ),
                    ),
            )

        assertEquals(
            "https://api.moonshot.ai/v1/chat/completions",
            plan.requestUrl,
        )
        assertEquals(
            VexLifePartnerProviderCatalog.Dialect.OPENAI_CHAT_COMPLETIONS,
            plan.dialect,
        )
        assertFalse(
            plan.requestUrl.contains(
                "/coding/",
            ),
        )
    }

    @Test
    fun credentialCoordinateCannotCrossProviderBoundary() {
        assertFailsWith<IllegalArgumentException> {
            VexLifePartnerNetworkPlan.form(
                providerRef =
                    VexLifePartnerProviderCatalog.AnthropicRef,
                credential =
                    coordinate(
                        VexLifePartnerProviderCatalog.OpenAiRef,
                        "openai-fixture-0001",
                    ),
                requestBody =
                    "{}",
            )
        }
    }

    @Test
    fun requestPlanRefusesEmptyBodyBeforeAnyNetworkEffectExists() {
        assertFailsWith<IllegalArgumentException> {
            VexLifePartnerNetworkPlan.form(
                providerRef =
                    VexLifePartnerProviderCatalog.OpenAiRef,
                credential =
                    coordinate(
                        VexLifePartnerProviderCatalog.OpenAiRef,
                        "openai-fixture-0001",
                    ),
                requestBody =
                    "   ",
            )
        }
    }

    @Test
    fun opaqueCredentialRefNeverEntersProviderRequestBody() {
        val credential =
            coordinate(
                VexLifePartnerProviderCatalog.OpenAiRef,
                "openai-fixture-0001",
            )
        val plan =
            VexLifePartnerNetworkPlan.form(
                providerRef =
                    credential.providerRef,
                credential = credential,
                requestBody =
                    OpenAiResponsesWire.encodeRequest(
                        prepared(
                            "gpt-fixture",
                        ),
                    ),
            )

        assertFalse(
            plan.requestBody.contains(
                credential.credentialRef,
            ),
        )
    }
}

// [VXG RealForever]
