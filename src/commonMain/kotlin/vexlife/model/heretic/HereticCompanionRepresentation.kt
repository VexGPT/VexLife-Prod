package vexlife.model.heretic

/**
 * Source-owned model/provider representation configuration for the local Companion.
 *
 * Vex semantic owners stay elsewhere: Conversation, Foundational Field, Memory and capability
 * meanings do not become model-specific. This owner answers only how one offered model/provider
 * represents those already-owned meanings at the inference boundary, and which parts of that
 * representation have actually been qualified.
 *
 * Every offered model chooses one complete [Profile]. Representation may be shared where source
 * proves it is the same, but generation choices are explicit per model so a new model cannot
 * silently inherit another model's defaults from the Connection.
 */
object HereticCompanionRepresentation {
    /*
     * This owner describes representation only: message protocol, reasoning carriage, reach
     * carriage, and generation parameters for one offered model/provider relationship. It does not
     * own the provider process, Vex semantic capability meaning, Conversation, Memory, or runtime
     * lifecycle.
     */
    /** Product-owned output custody bound shared by the currently offered local Companion profiles. */
    const val ProductMaxOutputTokens = 16_000

    enum class MessageProtocol {
        OPENAI_CHAT,
        HARMONY,
    }

    enum class ReasoningRepresentation {
        REASONING_FIELDS,
        HARMONY_CHANNELS,
    }

    enum class CapabilityRepresentation {
        /**
         * Compatibility representation when a provider has no native tool channel or when that
         * exact model/provider native-call path has not earned qualification.
         */
        VEXLIFE_CONTROL_TEXT,

        /**
         * Provider-native tool declaration/call/result carriage. The exact wire dialect is owned by
         * [MessageProtocol]; capability meaning, admission and effects stay with VexLife.
         */
        PROVIDER_NATIVE_TOOLS,
    }

    /**
     * Standing of the provider-native tool carriage for this exact model/provider representation.
     *
     * Recognition is source evidence only. A chat template that looks native-tool capable does not
     * become ordinary Product carriage until that exact relationship earns qualification.
     */
    enum class ProviderNativeToolStanding {
        NOT_REPRESENTED,
        RECOGNIZED_HELD,
        QUALIFIED,
    }

    enum class ReachRepresentationStanding {
        /** This provider representation of Vex reach is qualified for ordinary Companion use. */
        QUALIFIED,

        /**
         * Semantic VexLife reach exists, but this model/provider representation has not earned
         * ordinary Product use. The prompt must not advertise reach as available while held.
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

    enum class SamplingBasis {
        /** Exact values carried from one pinned model-owned generation configuration. */
        PINNED_MODEL_FILE,

        /** Values explicitly published for this model or its exact source lineage. */
        PROVIDER_RECOMMENDATION,

        /**
         * Product-owned conservative defaults for an imported provider relationship that has not
         * earned model-specific sampling guidance yet.
         */
        PRODUCT_DEFAULT,
    }

    data class Sampling(
        val basis: SamplingBasis,
        /** Stable source coordinate for why these values were selected. */
        val sourceRef: String,
        val temperature: Double,
        val topP: Double,
        val topK: Int,
    ) {
        init {
            require(
                sourceRef.matches(
                    Regex(
                        "[a-z0-9][a-z0-9._:-]+",
                    ),
                ) &&
                    temperature >=
                    0.0 &&
                    topP in
                    0.0..1.0 &&
                    topK >=
                    0,
            ) {
                "Companion sampling configuration is invalid"
            }
        }
    }

    data class Generation(
        val sampling: Sampling,
        /**
         * VexLife-owned output bound. Provider recommendations do not own Product attention/custody.
         */
        val productMaxOutputTokens: Int,
        /** Provider-native reasoning control when the representation earns one. */
        val reasoningEffort: ReasoningEffort?,
    ) {
        init {
            require(
                productMaxOutputTokens >
                    0,
            ) {
                "Companion output-token bound must be positive"
            }
        }
    }

    data class Profile(
        val profileRef: String,
        val messageProtocol: MessageProtocol,
        val reasoningRepresentation: ReasoningRepresentation,
        val capabilityRepresentation: CapabilityRepresentation,
        val providerNativeToolStanding: ProviderNativeToolStanding,
        val reachRepresentationStanding: ReachRepresentationStanding,
        val coalesceLeadingSystemMessages: Boolean,
        val modelIdentity: String?,
        val generation: Generation,
    ) {
        init {
            require(
                profileRef.matches(
                    Regex(
                        "[a-z0-9][a-z0-9._-]+",
                    ),
                ),
            ) {
                "Companion representation profile ref is invalid"
            }
            require(
                modelIdentity ==
                    null ||
                    modelIdentity.isNotBlank(),
            ) {
                "Companion provider model identity must be absent or non-blank"
            }
            require(
                (
                    messageProtocol ==
                        MessageProtocol.HARMONY
                ) ==
                    (
                        reasoningRepresentation ==
                            ReasoningRepresentation.HARMONY_CHANNELS
                    ),
            ) {
                "Harmony message and reasoning representations must move together"
            }
            require(
                messageProtocol ==
                    MessageProtocol.HARMONY ||
                    generation.reasoningEffort ==
                    null,
            ) {
                "Reasoning-effort levels are currently earned only by the Harmony profile"
            }
            when (providerNativeToolStanding) {
                ProviderNativeToolStanding.NOT_REPRESENTED,
                ProviderNativeToolStanding.RECOGNIZED_HELD,
                ->
                    require(
                        capabilityRepresentation ==
                            CapabilityRepresentation.VEXLIFE_CONTROL_TEXT,
                    ) {
                        "unqualified provider-native tools must keep the qualified text-control carriage"
                    }

                ProviderNativeToolStanding.QUALIFIED ->
                    require(
                        capabilityRepresentation ==
                            CapabilityRepresentation.PROVIDER_NATIVE_TOOLS,
                    ) {
                        "qualified provider-native tools require provider-native carriage"
                    }
            }
        }
    }

    object Profiles {
        private fun qwen(
            profileRef: String,
            sampling: Sampling,
            capabilityRepresentation: CapabilityRepresentation =
                CapabilityRepresentation.PROVIDER_NATIVE_TOOLS,
            reachRepresentationStanding: ReachRepresentationStanding =
                ReachRepresentationStanding.QUALIFIED,
        ): Profile =
            Profile(
                profileRef = profileRef,
                messageProtocol = MessageProtocol.OPENAI_CHAT,
                reasoningRepresentation = ReasoningRepresentation.REASONING_FIELDS,
                capabilityRepresentation = capabilityRepresentation,
                providerNativeToolStanding =
                    if (
                        capabilityRepresentation ==
                            CapabilityRepresentation.PROVIDER_NATIVE_TOOLS
                    ) {
                        ProviderNativeToolStanding.QUALIFIED
                    } else {
                        ProviderNativeToolStanding.NOT_REPRESENTED
                    },
                reachRepresentationStanding = reachRepresentationStanding,
                coalesceLeadingSystemMessages = false,
                modelIdentity = null,
                generation =
                    Generation(
                        sampling = sampling,
                        productMaxOutputTokens = ProductMaxOutputTokens,
                        reasoningEffort = null,
                    ),
            )

        val Qwen27B =
            qwen(
                profileRef = "companion-profile.vexlife-prod.qwen27b.reasoning-fields.v1",
                sampling =
                    Sampling(
                        basis = SamplingBasis.PINNED_MODEL_FILE,
                        sourceRef = "source.vexlife-prod.qwen27b.generation-config.d0d0ed2e37cd",
                        temperature = 1.0,
                        topP = 0.95,
                        topK = 20,
                    ),
            )

        val Qwen9B =
            qwen(
                profileRef = "companion-profile.vexlife-prod.qwen9b.reasoning-fields.v1",
                sampling =
                    Sampling(
                        basis = SamplingBasis.PROVIDER_RECOMMENDATION,
                        sourceRef = "source.provider.foresee.qwen38-9b.sampling.v1",
                        temperature = 0.6,
                        topP = 0.95,
                        topK = 20,
                    ),
            )

        val Qwen4B =
            qwen(
                profileRef =
                    "companion-profile.vexlife-prod.qwen4b.reasoning-fields.text-control.v3",
                capabilityRepresentation = CapabilityRepresentation.VEXLIFE_CONTROL_TEXT,
                reachRepresentationStanding = ReachRepresentationStanding.HELD_PROVIDER_BRIDGE,
                sampling =
                    Sampling(
                        basis = SamplingBasis.PROVIDER_RECOMMENDATION,
                        sourceRef = "source.provider.qwen38-4b-lineage.sampling.v1",
                        temperature = 0.6,
                        topP = 0.95,
                        topK = 20,
                    ),
            )


        /**
         * OpenAI-chat representation for one Home-imported Qwen3 MLX model.
         *
         * Import inspection records whether the exact pinned chat template exposes Qwen's native
         * tool grammar. Recognition alone does not activate provider-native carriage: imported
         * models keep the qualified VexLife text-control fallback until that exact native-tool
         * relationship earns a separate qualification. VexLife's bridge still owns admission and
         * effects in either representation.
         */
        fun importedQwen(
            profileRef: String,
            nativeToolCalling: Boolean = false,
        ): Profile =
            Profile(
                profileRef = profileRef,
                messageProtocol = MessageProtocol.OPENAI_CHAT,
                reasoningRepresentation = ReasoningRepresentation.REASONING_FIELDS,
                capabilityRepresentation = CapabilityRepresentation.VEXLIFE_CONTROL_TEXT,
                providerNativeToolStanding =
                    if (nativeToolCalling) {
                        ProviderNativeToolStanding.RECOGNIZED_HELD
                    } else {
                        ProviderNativeToolStanding.NOT_REPRESENTED
                    },
                // The bridge owns admission/effects. Native-template recognition is retained as a
                // held adapter candidate; it does not change ordinary carriage by itself.
                reachRepresentationStanding = ReachRepresentationStanding.QUALIFIED,
                coalesceLeadingSystemMessages = false,
                modelIdentity = null,
                generation =
                    Generation(
                        sampling =
                            Sampling(
                                basis = SamplingBasis.PRODUCT_DEFAULT,
                                sourceRef = "source.vexlife-prod.imported-qwen3.sampling.v1",
                                temperature = 0.6,
                                topP = 0.95,
                                topK = 20,
                            ),
                        productMaxOutputTokens = ProductMaxOutputTokens,
                        reasoningEffort = null,
                    ),
            )

        /**
         * GPT-OSS speaks Harmony. OpenAI publishes temperature 1.0 and top-p 1.0 for gpt-oss;
         * VexLife therefore disables an inherited top-k filter instead of carrying Qwen's top-k 20.
         *
         * Medium reasoning effort is explicit here instead of remaining an invisible template
         * default. Capability actions are declared through the provider-native Harmony functions
         * channel; VexLife still owns admission, source reads and returned evidence.
         */
        val GptOss20B =
            Profile(
                profileRef = "companion-profile.vexlife-prod.gpt-oss20b.harmony.v1",
                messageProtocol = MessageProtocol.HARMONY,
                reasoningRepresentation = ReasoningRepresentation.HARMONY_CHANNELS,
                capabilityRepresentation = CapabilityRepresentation.PROVIDER_NATIVE_TOOLS,
                providerNativeToolStanding = ProviderNativeToolStanding.QUALIFIED,
                reachRepresentationStanding = ReachRepresentationStanding.QUALIFIED,
                coalesceLeadingSystemMessages = true,
                modelIdentity = "You are Vex.",
                generation =
                    Generation(
                        sampling =
                            Sampling(
                                basis = SamplingBasis.PROVIDER_RECOMMENDATION,
                                sourceRef = "source.provider.openai.gpt-oss.sampling.v1",
                                temperature = 1.0,
                                topP = 1.0,
                                topK = 0,
                            ),
                        productMaxOutputTokens = ProductMaxOutputTokens,
                        reasoningEffort = ReasoningEffort.MEDIUM,
                    ),
            )
    }
}

// [VXG RealForever]
