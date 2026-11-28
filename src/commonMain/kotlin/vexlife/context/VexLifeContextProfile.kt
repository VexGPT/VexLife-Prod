package vexlife.context

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

/**
 * The person's portable choices for how VexLife constructs context around a turn.
 *
 * A profile is configuration only. It never contains Conversation, Memory pages, browser captures,
 * model reasoning, capability-return content, or any other personal context payload. Those truths
 * remain with their own owners.
 */
object VexLifeContextProfile {
    const val LegacySchema = "vexlife-prod.context-profile/v1"
    const val Schema = "vexlife-prod.context-profile/v2"
    const val LegacyPortableSchema = "vexlife-prod.context-profile-portable/v1"
    const val PortableSchema = "vexlife-prod.context-profile-portable/v2"
    const val ProductDefaultRef = "profile.vexlife.context.product-default"

    const val MaximumNameCharacters = 80
    const val MaximumDescriptionCharacters = 280
    const val MaximumFoundationTextCharacters = 16_000

    enum class FoundationMode {
        /** Keep the Product Foundational Field exactly as the Product owner supplies it. */
        PRODUCT_DEFAULT,

        /** Keep the Product field and append the person's portable foundation extension after it. */
        EXTEND_PRODUCT_DEFAULT,
    }

    @Serializable
    data class ProductFoundationIdentity(
        val version: String?,
        val bodySha256: String?,
    ) {
        init {
            bodySha256?.let {
                require(it.matches(Regex("[0-9a-f]{64}"))) {
                    "context profile Product foundation fingerprint must be one lowercase SHA-256"
                }
            }
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Serializable
    data class Definition(
        val schemaVersion: String = Schema,
        val name: String,
        val description: String = "",
        val foundationMode: FoundationMode = FoundationMode.PRODUCT_DEFAULT,
        val foundationText: String = "",
        /**
         * Optional human-selected ceiling for this profile's prompt input before the capability
         * exchange reserve. Null means use the current Product ceiling.
         *
         * The Profile owns the person's requested construction choice only. DirectPromptContext
         * remains the admission owner against the current Product ceiling and current turn shape.
         */
        @EncodeDefault(EncodeDefault.Mode.NEVER)
        val inputTokenBudget: Int? = null,
    ) {
        init {
            require(schemaVersion in setOf(LegacySchema, Schema)) {
                "context profile schema is not supported"
            }
            require(schemaVersion != LegacySchema || inputTokenBudget == null) {
                "legacy context profile schema cannot carry input token budget"
            }
            require(name.isNotBlank() && name.length <= MaximumNameCharacters) {
                "context profile name must be 1..$MaximumNameCharacters characters"
            }
            require(description.length <= MaximumDescriptionCharacters) {
                "context profile description is too long"
            }
            require(foundationText.length <= MaximumFoundationTextCharacters) {
                "context profile foundation text is too long"
            }
            inputTokenBudget?.let {
                require(it > 0) {
                    "context profile input token budget must be positive"
                }
            }
            when (foundationMode) {
                FoundationMode.PRODUCT_DEFAULT ->
                    require(foundationText.isBlank()) {
                        "Product-default context profile cannot carry personal foundation text"
                    }

                FoundationMode.EXTEND_PRODUCT_DEFAULT ->
                    require(foundationText.isNotBlank()) {
                        "foundation extension mode requires personal foundation text"
                    }
            }
        }
    }

    /**
     * Exact active revision captured when a human Send is accepted.
     *
     * Changing the profile while that turn runs applies to the next Send, never to a later provider
     * stage of the in-flight turn.
     */
    @Serializable
    data class Effective(
        val profileRef: String,
        val revision: Int,
        val definition: Definition,
        val basedOnProductFoundation: ProductFoundationIdentity,
        val fingerprint: String,
        val formedAt: String?,
        val productDefault: Boolean,
        val productFoundationChangedSinceSave: Boolean,
    ) {
        init {
            require(profileRef.matches(Regex("[a-z0-9](?:[a-z0-9._-]{0,220}[a-z0-9])?"))) {
                "context profile ref is not portable"
            }
            require(revision >= 0) { "context profile revision must be nonnegative" }
            require(fingerprint.matches(Regex("[0-9a-f]{64}"))) {
                "context profile fingerprint must be one lowercase SHA-256"
            }
            if (productDefault) {
                require(profileRef == ProductDefaultRef && revision == 0) {
                    "Product default context profile identity is invalid"
                }
            }
        }
    }

    /**
     * Shareable file shape. Deliberately configuration-only: no local profile ref, Conversation,
     * Memory content, reasoning, browser text, capability results, or Home path.
     */
    @OptIn(ExperimentalSerializationApi::class)
    @Serializable
    data class Portable(
        val schemaVersion: String = PortableSchema,
        val name: String,
        val description: String = "",
        val foundationMode: FoundationMode,
        val foundationText: String,
        @EncodeDefault(EncodeDefault.Mode.NEVER)
        val inputTokenBudget: Int? = null,
        val basedOnProductFoundation: ProductFoundationIdentity,
    ) {
        init {
            require(schemaVersion in setOf(LegacyPortableSchema, PortableSchema)) {
                "portable context profile schema is not supported"
            }
            require(schemaVersion != LegacyPortableSchema || inputTokenBudget == null) {
                "legacy portable context profile schema cannot carry input token budget"
            }
            definition()
        }

        /**
         * Importing a legacy portable profile currentizes only its configuration schema; its
         * Product-Foundation basis remains exact provenance.
         */
        fun definition(): Definition =
            Definition(
                schemaVersion = Schema,
                name = name,
                description = description,
                foundationMode = foundationMode,
                foundationText = foundationText,
                inputTokenBudget = inputTokenBudget,
            )
    }
}

// [VXG RealForever]
