package vexlife.partner

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class VexLifePartnerConnectionSettingsTest {
    private fun entry(
        providerRef: String,
        credentialRef: String,
        workspaceId: String? = null,
    ): VexLifePartnerConnectionSettings.Entry =
        VexLifePartnerConnectionSettings.Entry(
            providerRef = providerRef,
            credentialRef = credentialRef,
            accountScope =
                VexLifePartnerAccountScope.Selection(
                    providerRef = providerRef,
                    workspaceId = workspaceId,
                ),
        )

    @Test
    fun formCanonicalizesProviderOrderWithoutChangingOpaqueRefs() {
        val anthropic =
            entry(
                providerRef =
                    VexLifePartnerProviderCatalog.AnthropicRef,
                credentialRef =
                    "credential.vexlife.partner.anthropic-fixture-0001",
                workspaceId =
                    "wrkspc_fixture_0001",
            )
        val openAi =
            entry(
                providerRef =
                    VexLifePartnerProviderCatalog.OpenAiRef,
                credentialRef =
                    "credential.vexlife.partner.openai-fixture-0001",
            )

        val value =
            VexLifePartnerConnectionSettings.form(
                listOf(
                    openAi,
                    anthropic,
                ),
            )

        assertEquals(
            listOf(
                VexLifePartnerProviderCatalog.AnthropicRef,
                VexLifePartnerProviderCatalog.OpenAiRef,
            ),
            value.entries.map {
                it.providerRef
            },
        )
        assertEquals(
            "credential.vexlife.partner.openai-fixture-0001",
            VexLifePartnerConnectionSettings.entryFor(
                value,
                VexLifePartnerProviderCatalog.OpenAiRef,
            )?.credentialRef,
        )
        assertNull(
            VexLifePartnerConnectionSettings.entryFor(
                value,
                VexLifePartnerProviderCatalog.KimiInternationalRef,
            ),
        )
    }

    @Test
    fun duplicateProviderEntriesAreRejected() {
        val first =
            entry(
                providerRef =
                    VexLifePartnerProviderCatalog.OpenAiRef,
                credentialRef =
                    "credential.vexlife.partner.openai-fixture-0001",
            )
        val second =
            entry(
                providerRef =
                    VexLifePartnerProviderCatalog.OpenAiRef,
                credentialRef =
                    "credential.vexlife.partner.openai-fixture-0002",
            )

        assertFailsWith<IllegalArgumentException> {
            VexLifePartnerConnectionSettings.Value(
                schemaVersion =
                    VexLifePartnerConnectionSettings.Schema,
                entries =
                    listOf(
                        first,
                        second,
                    ),
            )
        }
    }

    @Test
    fun accountScopeMustBelongToTheSameProvider() {
        assertFailsWith<IllegalArgumentException> {
            VexLifePartnerConnectionSettings.Entry(
                providerRef =
                    VexLifePartnerProviderCatalog.OpenAiRef,
                credentialRef =
                    "credential.vexlife.partner.openai-fixture-0001",
                accountScope =
                    VexLifePartnerAccountScope.Selection(
                        providerRef =
                            VexLifePartnerProviderCatalog.AnthropicRef,
                        workspaceId =
                            "wrkspc_fixture_0001",
                    ),
            )
        }
    }

    @Test
    fun workspaceScopeRemainsProviderSpecific() {
        assertFailsWith<IllegalArgumentException> {
            entry(
                providerRef =
                    VexLifePartnerProviderCatalog.OpenAiRef,
                credentialRef =
                    "credential.vexlife.partner.openai-fixture-0001",
                workspaceId =
                    "wrkspc_fixture_0001",
            )
        }
        assertFailsWith<IllegalArgumentException> {
            entry(
                providerRef =
                    VexLifePartnerProviderCatalog.KimiInternationalRef,
                credentialRef =
                    "credential.vexlife.partner.kimi-fixture-0001",
                workspaceId =
                    "wrkspc_fixture_0001",
            )
        }
    }

    @Test
    fun transitionStateMustMatchTheCurrentProviderEntry() {
        val current =
            entry(
                providerRef =
                    VexLifePartnerProviderCatalog.OpenAiRef,
                credentialRef =
                    "credential.vexlife.partner.openai-fixture-0001",
            )
        val staged =
            VexLifePartnerConnectionSettings.Transition(
                providerRef =
                    VexLifePartnerProviderCatalog.OpenAiRef,
                kind =
                    VexLifePartnerConnectionSettings.TransitionKind.STAGE_CONNECT,
                stagedCredentialRef =
                    "credential.vexlife.partner.openai-fixture-0002",
                stagedAccountScope =
                    VexLifePartnerAccountScope.Selection(
                        providerRef =
                            VexLifePartnerProviderCatalog.OpenAiRef,
                    ),
            )

        assertFailsWith<IllegalArgumentException> {
            VexLifePartnerConnectionSettings.form(
                entries =
                    listOf(
                        current,
                    ),
                transitions =
                    listOf(
                        staged,
                    ),
            )
        }

        assertFailsWith<IllegalArgumentException> {
            VexLifePartnerConnectionSettings.form(
                entries =
                    emptyList(),
                transitions =
                    listOf(
                        staged.copy(
                            kind =
                                VexLifePartnerConnectionSettings.TransitionKind.STAGE_REPLACE,
                        ),
                    ),
            )
        }
    }

    @Test
    fun cleanupAndRemoveMustNameOnlyPriorOrCurrentCredentialRefs() {
        val current =
            entry(
                providerRef =
                    VexLifePartnerProviderCatalog.KimiInternationalRef,
                credentialRef =
                    "credential.vexlife.partner.kimi-fixture-0002",
            )

        assertFailsWith<IllegalArgumentException> {
            VexLifePartnerConnectionSettings.form(
                entries =
                    listOf(
                        current,
                    ),
                transitions =
                    listOf(
                        VexLifePartnerConnectionSettings.Transition(
                            providerRef =
                                current.providerRef,
                            kind =
                                VexLifePartnerConnectionSettings.TransitionKind.CLEANUP_REPLACED,
                            retiringCredentialRefs =
                                listOf(
                                    current.credentialRef,
                                ),
                        ),
                    ),
            )
        }

        assertFailsWith<IllegalArgumentException> {
            VexLifePartnerConnectionSettings.form(
                entries =
                    listOf(
                        current,
                    ),
                transitions =
                    listOf(
                        VexLifePartnerConnectionSettings.Transition(
                            providerRef =
                                current.providerRef,
                            kind =
                                VexLifePartnerConnectionSettings.TransitionKind.REMOVE,
                            retiringCredentialRefs =
                                listOf(
                                    "credential.vexlife.partner.kimi-fixture-old",
                                ),
                        ),
                    ),
            )
        }
    }

    @Test
    fun formCanonicalizesTransitionOrder() {
        val openAi =
            VexLifePartnerConnectionSettings.Transition(
                providerRef =
                    VexLifePartnerProviderCatalog.OpenAiRef,
                kind =
                    VexLifePartnerConnectionSettings.TransitionKind.STAGE_CONNECT,
                stagedCredentialRef =
                    "credential.vexlife.partner.openai-fixture-0002",
                stagedAccountScope =
                    VexLifePartnerAccountScope.Selection(
                        providerRef =
                            VexLifePartnerProviderCatalog.OpenAiRef,
                    ),
            )
        val kimi =
            VexLifePartnerConnectionSettings.Transition(
                providerRef =
                    VexLifePartnerProviderCatalog.KimiInternationalRef,
                kind =
                    VexLifePartnerConnectionSettings.TransitionKind.STAGE_CONNECT,
                stagedCredentialRef =
                    "credential.vexlife.partner.kimi-fixture-0002",
                stagedAccountScope =
                    VexLifePartnerAccountScope.Selection(
                        providerRef =
                            VexLifePartnerProviderCatalog.KimiInternationalRef,
                    ),
            )

        assertEquals(
            listOf(
                VexLifePartnerProviderCatalog.KimiInternationalRef,
                VexLifePartnerProviderCatalog.OpenAiRef,
            )
                .sorted(),
            VexLifePartnerConnectionSettings.form(
                entries =
                    emptyList(),
                transitions =
                    listOf(
                        openAi,
                        kimi,
                    ),
            )
                .transitions
                .map {
                    it.providerRef
                },
        )
    }

    @Test
    fun unknownProviderAndUnboundedCredentialRefsAreRejected() {
        assertFailsWith<IllegalArgumentException> {
            entry(
                providerRef =
                    "provider.custom.unformed",
                credentialRef =
                    "credential.vexlife.partner.custom-fixture-0001",
            )
        }
        assertFailsWith<IllegalArgumentException> {
            entry(
                providerRef =
                    VexLifePartnerProviderCatalog.OpenAiRef,
                credentialRef =
                    "secret",
            )
        }
    }
}

// [VXG RealForever]
