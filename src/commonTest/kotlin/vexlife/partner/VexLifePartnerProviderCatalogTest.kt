package vexlife.partner

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VexLifePartnerProviderCatalogTest {
    @Test
    fun builtInProvidersOwnExactCurrentHttpsOriginsAndInferencePaths() {
        val openAi =
            VexLifePartnerProviderCatalog.OpenAi
        val anthropic =
            VexLifePartnerProviderCatalog.Anthropic
        val kimi =
            VexLifePartnerProviderCatalog.KimiInternational

        assertEquals(
            "https://api.openai.com/v1/responses",
            openAi.inferenceUrl,
        )
        assertEquals(
            VexLifePartnerProviderCatalog.Dialect.OPENAI_RESPONSES,
            openAi.dialect,
        )

        assertEquals(
            "https://api.anthropic.com/v1/messages",
            anthropic.inferenceUrl,
        )
        assertEquals(
            VexLifePartnerProviderCatalog.Dialect.ANTHROPIC_MESSAGES,
            anthropic.dialect,
        )
        assertEquals(
            "2026-10-21",
            anthropic.requiredProtocolHeaders[
                "anthropic-version"
            ],
        )
        assertEquals(
            VexLifePartnerProviderCatalog.AccountScopeSelection.WORKSPACE_ID_IF_CREDENTIAL_NOT_SCOPED,
            anthropic.accountScopeSelection,
        )

        assertEquals(
            "https://api.moonshot.ai/v1/chat/completions",
            kimi.inferenceUrl,
        )
        assertEquals(
            VexLifePartnerProviderCatalog.Dialect.OPENAI_CHAT_COMPLETIONS,
            kimi.dialect,
        )
    }

    @Test
    fun currentBuiltInsUseBearerCredentialCarriageWithoutContainingSecrets() {
        VexLifePartnerProviderCatalog
            .definitions()
            .forEach { provider ->
                assertEquals(
                    VexLifePartnerProviderCatalog.CredentialCarriage.AUTHORIZATION_BEARER,
                    provider.credentialCarriage,
                )
                assertTrue(
                    provider.requiredProtocolHeaders.keys.none { key ->
                        key.equals(
                            "authorization",
                            ignoreCase = true,
                        ) ||
                            key.equals(
                                "x-api-key",
                                ignoreCase = true,
                            )
                    },
                )
            }
    }

    @Test
    fun providerRefsAreUniqueAndUnknownProviderDoesNotBecomeCustomEndpoint() {
        val definitions =
            VexLifePartnerProviderCatalog.definitions()

        assertEquals(
            definitions.size,
            definitions
                .map {
                    it.providerRef
                }
                .distinct()
                .size,
        )
        assertNull(
            VexLifePartnerProviderCatalog.resolve(
                "provider.custom.unformed",
            ),
        )
    }

    @Test
    fun kimiOpenPlatformInternationalDoesNotCollapseIntoKimiCode() {
        assertEquals(
            "provider.kimi.open-platform.international",
            VexLifePartnerProviderCatalog.KimiInternational.providerRef,
        )
        assertTrue(
            VexLifePartnerProviderCatalog
                .definitions()
                .none {
                    it.providerRef.contains(
                        "kimi-code",
                    )
                },
        )
    }
}

// [VXG RealForever]
