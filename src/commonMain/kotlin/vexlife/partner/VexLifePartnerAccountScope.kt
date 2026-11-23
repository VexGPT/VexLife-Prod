package vexlife.partner

import kotlinx.serialization.Serializable

/**
 * Optional non-secret account/workspace scope for one built-in Partner provider.
 *
 * This relationship does not own credentials, billing/account standing, model access, or provider
 * support. It only answers which additional public request coordinate is required when the selected
 * credential is not already provider-scoped.
 */
object VexLifePartnerAccountScope {
    @Serializable
    data class Selection(
        val providerRef: String,
        val workspaceId: String? = null,
    ) {
        init {
            val provider =
                requireNotNull(
                    VexLifePartnerProviderCatalog.resolve(
                        providerRef,
                    ),
                ) {
                    "Partner account scope requires one source-owned built-in provider"
                }

            when (
                provider.accountScopeSelection
            ) {
                VexLifePartnerProviderCatalog.AccountScopeSelection.NONE ->
                    require(
                        workspaceId ==
                            null,
                    ) {
                        "this Partner provider does not admit a workspace account-scope coordinate"
                    }

                VexLifePartnerProviderCatalog.AccountScopeSelection.WORKSPACE_ID_IF_CREDENTIAL_NOT_SCOPED ->
                    workspaceId
                        ?.let(
                            ::requireWorkspaceId,
                        )
            }
        }
    }

    /**
     * Public protocol headers only.
     *
     * A null workspace ID is lawful for Anthropic when the credential itself is already scoped to the
     * intended workspace.
     */
    fun publicHeaders(
        selection: Selection,
    ): Map<String, String> =
        selection.workspaceId
            ?.let {
                mapOf(
                    "anthropic-workspace-id" to
                        it,
                )
            }
            .orEmpty()

    private fun requireWorkspaceId(
        value: String,
    ) {
        require(
            value.startsWith(
                "wrkspc_",
            ) &&
                value.length in
                9..256 &&
                value.none(
                    Char::isWhitespace,
                ) &&
                value.none {
                    it == '\n' ||
                        it == '\r' ||
                        it == '\u0000'
                },
        ) {
            "Anthropic workspace ID must be one bounded non-secret workspace coordinate"
        }
    }
}

// [VXG RealForever]
