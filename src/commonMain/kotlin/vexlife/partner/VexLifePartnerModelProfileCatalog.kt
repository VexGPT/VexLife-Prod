package vexlife.partner

import kotlinx.serialization.Serializable

/**
 * Source-owned initial Partner model/profile inventory.
 *
 * This catalog says only which exact provider/model representations this VexLife version is prepared
 * to understand. It does not claim account access, billing, invocation qualification, live provider
 * validation, or model-use authority.
 */
object VexLifePartnerModelProfileCatalog {
    @Serializable
    data class Definition(
        val providerRef: String,
        val modelRef: String,
        val profileRef: String,
        val dialect: VexLifePartnerProviderCatalog.Dialect,
        val providerNativeToolsRepresented: Boolean,
        val reasoningEffortControlRepresented: Boolean,
        val visualInputRepresented: Boolean,
        val clientToolsOnly: Boolean,
        val sourceRefs: List<String>,
    ) {
        init {
            requireRef(providerRef, "providerRef")
            requireRef(modelRef, "modelRef")
            requireRef(profileRef, "profileRef")
            require(sourceRefs.isNotEmpty()) {
                "Partner model profile requires at least one public source reference"
            }
            require(
                sourceRefs.all {
                    it.startsWith("https://")
                },
            ) {
                "Partner model profile source refs must be HTTPS public sources"
            }

            val provider =
                requireNotNull(
                    VexLifePartnerProviderCatalog.resolve(providerRef),
                ) {
                    "Partner model profile requires one source-owned built-in provider"
                }
            require(provider.dialect == dialect) {
                "Partner model profile dialect must match the built-in provider topology"
            }
            require(clientToolsOnly) {
                "initial Partner profiles admit Vex-owned client tools only"
            }
        }
    }

    val OpenAiGpt62Sol =
        Definition(
            providerRef = VexLifePartnerProviderCatalog.OpenAiRef,
            modelRef = "gpt-6.2-sol",
            profileRef = "profile.vexlife.partner.openai.gpt-6.2-sol.v1",
            dialect = VexLifePartnerProviderCatalog.Dialect.OPENAI_RESPONSES,
            providerNativeToolsRepresented = true,
            reasoningEffortControlRepresented = true,
            visualInputRepresented = true,
            clientToolsOnly = true,
            sourceRefs =
                listOf(
                    "https://developers.openai.com/api/docs/models",
                    "https://developers.openai.com/api/docs/guides/model-selection",
                ),
        )

    val AnthropicClaudeSonnet55 =
        Definition(
            providerRef = VexLifePartnerProviderCatalog.AnthropicRef,
            modelRef = "claude-sonnet-5-5",
            profileRef = "profile.vexlife.partner.anthropic.claude-sonnet-5-5.v1",
            dialect = VexLifePartnerProviderCatalog.Dialect.ANTHROPIC_MESSAGES,
            providerNativeToolsRepresented = true,
            reasoningEffortControlRepresented = true,
            visualInputRepresented = true,
            clientToolsOnly = true,
            sourceRefs =
                listOf(
                    "https://platform.claude.com/docs/en/models/overview",
                    "https://platform.claude.com/docs/en/claude_api_primer",
                ),
        )

    val KimiK3 =
        Definition(
            providerRef = VexLifePartnerProviderCatalog.KimiInternationalRef,
            modelRef = "kimi-k3",
            profileRef = "profile.vexlife.partner.kimi.k3.v1",
            dialect = VexLifePartnerProviderCatalog.Dialect.OPENAI_CHAT_COMPLETIONS,
            providerNativeToolsRepresented = true,
            reasoningEffortControlRepresented = true,
            visualInputRepresented = true,
            clientToolsOnly = true,
            sourceRefs =
                listOf(
                    "https://www.kimi.ai/help/kimi-api/api-model-selection",
                    "https://www.kimi.ai/help/kimi-api/api-troubleshooting",
                ),
        )

    private val Definitions =
        listOf(
            OpenAiGpt62Sol,
            AnthropicClaudeSonnet55,
            KimiK3,
        )

    fun definitions(): List<Definition> =
        Definitions

    fun resolve(
        providerRef: String,
        modelRef: String,
    ): Definition? =
        Definitions.firstOrNull {
            it.providerRef == providerRef &&
                it.modelRef == modelRef
        }

    private fun requireRef(
        value: String,
        field: String,
    ) {
        require(
            value.isNotBlank() &&
                value.length <= 512 &&
                value.none(Char::isWhitespace),
        ) {
            "$field must be one bounded non-blank reference"
        }
    }
}

// [VXG RealForever]
