package vexlife.partner

import kotlinx.serialization.Serializable

/**
 * Source-owned built-in Partner provider topology.
 *
 * The ordinary person chooses a provider; they do not type the provider's host or wire protocol.
 * This catalog carries only public connection topology and protocol requirements. It contains no
 * credential value, account identity, model support claim, connection state, or effect authority.
 *
 * Current contract ground at formation (2026-11-23):
 * - OpenAI Responses: https://api.openai.com/v1/responses
 * - Claude API: https://api.anthropic.com/v1/messages, API version 2026-10-21
 * - Kimi Open Platform international: https://api.moonshot.ai/v1/chat/completions
 *
 * Kimi Code is a separate product/credential relationship and is deliberately not represented here.
 */
object VexLifePartnerProviderCatalog {
    @Serializable
    enum class Dialect {
        OPENAI_RESPONSES,
        ANTHROPIC_MESSAGES,
        OPENAI_CHAT_COMPLETIONS,
    }

    @Serializable
    enum class CredentialCarriage {
        AUTHORIZATION_BEARER,
    }

    @Serializable
    enum class AccountScopeSelection {
        NONE,
        WORKSPACE_ID_IF_CREDENTIAL_NOT_SCOPED,
    }

    @Serializable
    data class Definition(
        val providerRef: String,
        val origin: String,
        val inferencePath: String,
        val dialect: Dialect,
        val credentialCarriage: CredentialCarriage,
        /**
         * Public fixed protocol headers only. Secret-bearing headers never belong here.
         */
        val requiredProtocolHeaders: Map<String, String>,
        val accountScopeSelection: AccountScopeSelection,
    ) {
        init {
            require(
                providerRef.isNotBlank() &&
                    providerRef.none(
                        Char::isWhitespace,
                    ),
            ) {
                "Partner provider ref must be one non-blank coordinate"
            }
            require(
                origin.startsWith(
                    "https://",
                ) &&
                    !origin.endsWith(
                        "/",
                    ) &&
                    origin.substringAfter(
                        "https://",
                    )
                        .none {
                            it == '/' ||
                                it == '?' ||
                                it == '#'
                        },
            ) {
                "built-in Partner origin must be one exact HTTPS origin without path/query/fragment"
            }
            require(
                inferencePath.startsWith(
                    "/",
                ) &&
                    !inferencePath.contains(
                        '?',
                    ) &&
                    !inferencePath.contains(
                        '#',
                    ),
            ) {
                "built-in Partner inference path must be one absolute path"
            }
            require(
                requiredProtocolHeaders.keys.none { key ->
                    key.equals(
                        "authorization",
                        ignoreCase = true,
                    ) ||
                        key.equals(
                            "x-api-key",
                            ignoreCase = true,
                        )
                },
            ) {
                "secret-bearing credential headers do not belong in the public provider catalog"
            }
        }

        val inferenceUrl: String
            get() =
                origin +
                    inferencePath
    }

    const val OpenAiRef =
        "provider.openai.api"

    const val AnthropicRef =
        "provider.anthropic.api"

    const val KimiInternationalRef =
        "provider.kimi.open-platform.international"

    val OpenAi =
        Definition(
            providerRef = OpenAiRef,
            origin = "https://api.openai.com",
            inferencePath = "/v1/responses",
            dialect = Dialect.OPENAI_RESPONSES,
            credentialCarriage = CredentialCarriage.AUTHORIZATION_BEARER,
            requiredProtocolHeaders =
                emptyMap(),
            accountScopeSelection =
                AccountScopeSelection.NONE,
        )

    val Anthropic =
        Definition(
            providerRef = AnthropicRef,
            origin = "https://api.anthropic.com",
            inferencePath = "/v1/messages",
            dialect = Dialect.ANTHROPIC_MESSAGES,
            credentialCarriage = CredentialCarriage.AUTHORIZATION_BEARER,
            requiredProtocolHeaders =
                mapOf(
                    "anthropic-version" to "2026-10-21",
                ),
            accountScopeSelection =
                AccountScopeSelection.WORKSPACE_ID_IF_CREDENTIAL_NOT_SCOPED,
        )

    val KimiInternational =
        Definition(
            providerRef = KimiInternationalRef,
            origin = "https://api.moonshot.ai",
            inferencePath = "/v1/chat/completions",
            dialect = Dialect.OPENAI_CHAT_COMPLETIONS,
            credentialCarriage = CredentialCarriage.AUTHORIZATION_BEARER,
            requiredProtocolHeaders =
                emptyMap(),
            accountScopeSelection =
                AccountScopeSelection.NONE,
        )

    private val Definitions =
        listOf(
            OpenAi,
            Anthropic,
            KimiInternational,
        )

    fun definitions(): List<Definition> =
        Definitions

    fun resolve(
        providerRef: String,
    ): Definition? =
        Definitions.firstOrNull {
            it.providerRef ==
                providerRef
        }
}

// [VXG RealForever]
