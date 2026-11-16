package vexlife.model.heretic

/**
 * Source-owned provider/model representation configuration for the local Companion.
 *
 * Vex semantic owners stay elsewhere: Conversation, Foundational Field, Memory and capability
 * meanings do not become model-specific. This owner answers only how one offered model/provider
 * represents those already-owned meanings at the inference boundary, and which parts of that
 * representation have actually been qualified.
 *
 * Every field is explicit. Adding a model must choose one complete [Profile]; no model silently
 * inherits sampling, reasoning, capability or message-format defaults from the Connection.
 */
object HereticCompanionProvider {
    enum class MessageProtocol {
        OPENAI_CHAT,
        HARMONY,
    }

    enum class ReasoningRepresentation {
        REASONING_FIELDS,
        HARMONY_CHANNELS,
    }

    enum class CapabilityRepresentation {
        /** Existing VexLife machine-control reply: VEXLIFE_CAPABILITY_REQUEST/v1. */
        VEXLIFE_CONTROL_TEXT,

        /** Provider-native Harmony function/tool representation. */
        HARMONY_FUNCTIONS,
    }

    enum class CapabilityStanding {
        /** This representation is source + lived qualified for ordinary Companion use. */
        QUALIFIED,

        /**
         * Semantic VexLife reach exists, but this model's provider-native bridge is not yet
         * qualified. The prompt must not advertise reach as available while held.
         */
        HELD_PROVIDER_BRIDGE,
    }

    enum class ReasoningEffort(
        val wireValue: String,
    ) {
        LOW("low"),
        MEDIUM("medium"),
        HIGH("high"),
    }

    enum class GenerationBasis {
        /** Exact settings carried from the certified model's pinned generation configuration. */
        PINNED_MODEL_GENERATION_CONFIG,

        /** Explicit settings chosen from the model/provider's published deployment guidance. */
        PROVIDER_RECOMMENDATION,
    }

    data class Generation(
        val basis: GenerationBasis,
        val temperature: Double,
        val topP: Double,
        val topK: Int,
        val maxOutputTokens: Int,
        val reasoningEffort: ReasoningEffort?,
    ) {
        init {
            require(temperature >= 0.0 && topP in 0.0..1.0 && topK >= 0 && maxOutputTokens > 0) {
                "Companion generation profile is invalid"
            }
        }
    }

    data class Profile(
        val profileRef: String,
        val messageProtocol: MessageProtocol,
        val reasoningRepresentation: ReasoningRepresentation,
        val capabilityRepresentation: CapabilityRepresentation,
        val capabilityStanding: CapabilityStanding,
        val coalesceLeadingSystemMessages: Boolean,
        val modelIdentity: String?,
        val generation: Generation,
    ) {
        init {
            require(profileRef.matches(Regex("[a-z0-9][a-z0-9._-]+"))) {
                "Companion provider profile ref is invalid"
            }
            require(modelIdentity == null || modelIdentity.isNotBlank()) {
                "Companion provider model identity must be absent or non-blank"
            }
            require(
                (messageProtocol == MessageProtocol.HARMONY) ==
                    (reasoningRepresentation == ReasoningRepresentation.HARMONY_CHANNELS)
            ) {
                "Harmony message and reasoning representations must move together"
            }
            require(
                messageProtocol == MessageProtocol.HARMONY ||
                    generation.reasoningEffort == null
            ) {
                "Reasoning-effort levels are currently earned only by the Harmony profile"
            }
        }
    }

    object Profiles {
        /**
         * Current Qwen relationship, unchanged: VexLife's text-control reach is qualified and the
         * certified generation settings remain explicit here rather than hidden in the Connection.
         */
        val Qwen =
            Profile(
                profileRef = "companion-profile.vexlife-prod.qwen.reasoning-fields.v1",
                messageProtocol = MessageProtocol.OPENAI_CHAT,
                reasoningRepresentation = ReasoningRepresentation.REASONING_FIELDS,
                capabilityRepresentation = CapabilityRepresentation.VEXLIFE_CONTROL_TEXT,
                capabilityStanding = CapabilityStanding.QUALIFIED,
                coalesceLeadingSystemMessages = false,
                modelIdentity = null,
                generation =
                    Generation(
                        basis = GenerationBasis.PINNED_MODEL_GENERATION_CONFIG,
                        temperature = 1.0,
                        topP = 0.95,
                        topK = 20,
                        maxOutputTokens = 16_000,
                        reasoningEffort = null,
                    ),
            )

        /**
         * GPT-OSS speaks Harmony. OpenAI's published gpt-oss guidance recommends temperature 1.0
         * and top-p 1.0; top-k is therefore disabled instead of inheriting Qwen's top-k 20.
         *
         * Harmony-native function carriage is the intended capability representation, but the
         * Product's pinned mlx-lm 0.32.0 bridge has not yet been lived-qualified for that mapping.
         * Reach therefore stays HELD here rather than being falsely advertised through Qwen's
         * text-control convention.
         */
        val GptOssHarmony =
            Profile(
                profileRef = "companion-profile.vexlife-prod.gpt-oss.harmony.v1",
                messageProtocol = MessageProtocol.HARMONY,
                reasoningRepresentation = ReasoningRepresentation.HARMONY_CHANNELS,
                capabilityRepresentation = CapabilityRepresentation.HARMONY_FUNCTIONS,
                capabilityStanding = CapabilityStanding.HELD_PROVIDER_BRIDGE,
                coalesceLeadingSystemMessages = true,
                modelIdentity = "You are Vex.",
                generation =
                    Generation(
                        basis = GenerationBasis.PROVIDER_RECOMMENDATION,
                        temperature = 1.0,
                        topP = 1.0,
                        topK = 0,
                        maxOutputTokens = 16_000,
                        reasoningEffort = ReasoningEffort.MEDIUM,
                    ),
            )
    }
}

// [VXG RealForever]
