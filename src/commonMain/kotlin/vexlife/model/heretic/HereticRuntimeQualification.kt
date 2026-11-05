package vexlife.model.heretic

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
@Serializable
data class HereticRuntimeQualification private constructor(
    val healthAndExactModelVerified: Boolean,
    val actualChatCompletionPerformed: Boolean,
    val loadResultRef: String?,
    val qualificationInferenceEffect: Boolean,
    val qualificationOutputPersisted: Boolean,
    val livedConversationEffect: Boolean,
) {
    companion object {
        fun qualified(
            loadResultRef: String? = null,
        ): HereticRuntimeQualification =
            HereticRuntimeQualification(
                healthAndExactModelVerified = true,
                actualChatCompletionPerformed = true,
                loadResultRef = loadResultRef,
                qualificationInferenceEffect = true,
                qualificationOutputPersisted = false,
                livedConversationEffect = false,
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
    }
}

// [VXG RealForever]
