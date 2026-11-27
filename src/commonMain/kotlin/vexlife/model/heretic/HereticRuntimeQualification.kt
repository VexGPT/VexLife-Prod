package vexlife.model.heretic

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

/**
 * Meaning of a successful Heretic runtime qualification.
 *
 * Current VexLife earns this only after:
 *  1. /health returns exact status=ok,
 *  2. /v1/models exposes the exact model materialization exactly once,
 *  3. one real non-streaming chat completion returns non-empty content for requestModel=default_model.
 *
 * Performing those checks is a platform/runtime effect. This common contract only represents the
 * resulting truth.
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
@ConsistentCopyVisibility
data class HereticRuntimeQualification private constructor(
    val healthAndExactModelVerified: Boolean,
    val actualChatCompletionPerformed: Boolean,
    val loadResultRef: String?,
    val qualificationInferenceEffect: Boolean,
    val qualificationOutputPersisted: Boolean,
    val livedConversationEffect: Boolean,
    /**
     * Optional qualification of provider-native tool carriage on this exact runtime.
     *
     * Absence does not fail the model runtime; it means native-tool carriage remains held and the
     * narrower already-qualified representation stays in use.
     */
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val providerNativeToolsQualificationRef: String? = null,
    /**
     * Optional qualification of this exact model/runtime's Vex text-control reach relationship.
     *
     * This is deliberately independent of provider-native tool qualification. It proves only that
     * the model can participate in the Product-owned bounded text-control request/feedback loop,
     * including the narrowly admitted structured protocol/version representation of the same
     * request meaning, recover from malformed or otherwise rejected request form through the same
     * Bridge result a real turn would receive, complete one Atlas list, and derive one Atlas
     * describe request from the returned registry. The synthetic human-visible request itself names
     * that two-hop goal, and the provider receives the same request-owned text-control instruction
     * as an ordinary Product turn. Capability admission/effects remain Atlas/Bridge truth.
     */
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val vexReachQualificationRef: String? = null,
) {
    companion object {
        const val ProviderNativeToolsQualificationRef =
            "qualification.vexlife-prod.provider-native-tools.openai-function-roundtrip.v1"

        const val VexReachQualificationRef =
            "qualification.vexlife-prod.vex-reach.text-control-shared-grammar-recoverable-derived-continuation.v1"

        fun qualified(
            loadResultRef: String? = null,
            providerNativeToolsQualified: Boolean = false,
            vexReachQualified: Boolean = false,
        ): HereticRuntimeQualification =
            HereticRuntimeQualification(
                healthAndExactModelVerified = true,
                actualChatCompletionPerformed = true,
                loadResultRef = loadResultRef,
                qualificationInferenceEffect = true,
                qualificationOutputPersisted = false,
                livedConversationEffect = false,
                providerNativeToolsQualificationRef =
                    ProviderNativeToolsQualificationRef.takeIf {
                        providerNativeToolsQualified
                    },
                vexReachQualificationRef =
                    VexReachQualificationRef.takeIf {
                        vexReachQualified
                    },
            )
    }

    init {
        require(
            healthAndExactModelVerified,
        ) {
            "Heretic qualification requires exact health + model identity"
        }
        require(
            actualChatCompletionPerformed,
        ) {
            "Heretic qualification requires one actual chat completion"
        }
        require(
            qualificationInferenceEffect,
        ) {
            "Heretic qualification must disclose its bounded inference effect"
        }
        require(
            !qualificationOutputPersisted,
        ) {
            "Heretic qualification output must not become persisted product conversation"
        }
        require(
            !livedConversationEffect,
        ) {
            "Heretic qualification must not become a lived Conversation effect"
        }
        loadResultRef?.let {
            requirePortableRef(
                it,
                "loadResultRef",
            )
        }
        providerNativeToolsQualificationRef?.let {
            require(
                it ==
                    ProviderNativeToolsQualificationRef,
            ) {
                "provider-native tool qualification ref is not admitted"
            }
        }
        vexReachQualificationRef?.let {
            require(
                it ==
                    VexReachQualificationRef,
            ) {
                "Vex reach qualification ref is not admitted"
            }
        }
    }
}

// [VXG RealForever]
