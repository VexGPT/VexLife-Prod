package vexlife.partner

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class VexLifePartnerAccountScopeTest {
    @Test
    fun anthropicScopedCredentialMayOmitWorkspaceHeader() {
        val selection =
            VexLifePartnerAccountScope.Selection(
                providerRef =
                    VexLifePartnerProviderCatalog.AnthropicRef,
            )

        assertTrue(
            VexLifePartnerAccountScope
                .publicHeaders(
                    selection,
                )
                .isEmpty(),
        )
    }

    @Test
    fun anthropicUnscopedCredentialMayCarryOneWorkspaceHeader() {
        val selection =
            VexLifePartnerAccountScope.Selection(
                providerRef =
                    VexLifePartnerProviderCatalog.AnthropicRef,
                workspaceId =
                    "wrkspc_fixture123",
            )

        assertEquals(
            mapOf(
                "anthropic-workspace-id" to
                    "wrkspc_fixture123",
            ),
            VexLifePartnerAccountScope.publicHeaders(
                selection,
            ),
        )
    }

    @Test
    fun workspaceIdIsRejectedForProvidersThatDoNotOwnThatRelationship() {
        listOf(
            VexLifePartnerProviderCatalog.OpenAiRef,
            VexLifePartnerProviderCatalog.KimiInternationalRef,
        ).forEach {
            providerRef ->
            assertFailsWith<IllegalArgumentException> {
                VexLifePartnerAccountScope.Selection(
                    providerRef = providerRef,
                    workspaceId =
                        "wrkspc_fixture123",
                )
            }
        }
    }

    @Test
    fun malformedWorkspaceCoordinateIsRejected() {
        listOf(
            "workspace",
            "wrkspc_",
            "wrkspc_bad value",
            "wrkspc_bad\nvalue",
        ).forEach {
            value ->
            assertFailsWith<IllegalArgumentException> {
                VexLifePartnerAccountScope.Selection(
                    providerRef =
                        VexLifePartnerProviderCatalog.AnthropicRef,
                    workspaceId =
                        value,
                )
            }
        }
    }
}

// [VXG RealForever]
