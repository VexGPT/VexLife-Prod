package vexlife.conversation

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Closed validator for persisted reference-only VexLife Semantic Relay metadata.
 *
 * This family owns only Relay semantic validity. Conversation owns whether one valid Relay is
 * attached to a particular event. The composer/translation/runtime owners remain elsewhere.
 */
object VexLifeSemanticRelayContract {
    const val Schema =
        "vexlife.semantic-relay-reference/v1"

    private val InterpretationStates =
        setOf(
            "CANDIDATE",
            "CONFIRMED",
            "CORRECTED",
            "HELD",
        )

    private val AmbiguityStates =
        setOf(
            "CLEAR",
            "AMBIGUOUS",
            "DISPUTED",
            "UNKNOWN",
        )

    private val MaterialityStates =
        setOf(
            "ORDINARY",
            "MATERIAL",
        )

    private val OriginatorKinds =
        setOf(
            "HUMAN",
            "AI",
        )

    private val AdmissionStates =
        setOf(
            "ADMITTED",
            "REVIEW_REQUIRED",
            "UNAVAILABLE",
            "UNKNOWN",
        )

    private val LocalizationReadinessStates =
        setOf(
            "TRANSLATION_READY",
            "GLOSSARY_READY",
            "CANDIDATE",
            "UNAVAILABLE",
            "UNKNOWN",
        )

    private val RuntimeCurrentnessStates =
        setOf(
            "CURRENT",
            "STALE",
            "INVALID",
            "UNKNOWN",
        )

    private val DeliveryStates =
        setOf(
            "NOT_DELIVERED",
            "QUEUED",
            "DELIVERED",
            "FAILED",
        )

    private val AcknowledgementStates =
        setOf(
            "NOT_REQUESTED",
            "PENDING",
            "ACKNOWLEDGED",
            "UNKNOWN",
        )

    private val UnderstandingStates =
        setOf(
            "NOT_ASSESSED",
            "UNDERSTOOD",
            "MISUNDERSTOOD",
            "UNKNOWN",
        )

    private val SemanticEquivalenceStates =
        setOf(
            "NOT_CHECKED",
            "CONSISTENT",
            "PARTIAL",
            "CONTRADICTED",
            "UNKNOWN",
        )

    private val ProjectionModes =
        setOf(
            "MODEL_NATIVE",
            "LOCALIZATION_PIPELINE",
            "HUMAN_REVIEW",
            "NONE",
        )

    private val ForbiddenRelayFields =
        setOf(
            "rawText",
            "rawUtterance",
            "sourceText",
            "messageContent",
            "content",
            "translatedText",
            "generatedText",
            "generatedAnswer",
            "modelOutput",
            "providerResponse",
            "prompt",
            "conversationTranscript",
            "audio",
            "credential",
            "token",
            "originalContent",
        )

    data class Validation(
        val ok: Boolean,
        val errors: List<String>,
    )

    private data class RuntimeCapability(
        val capabilityRef: String?,
        val currentnessState: String,
        val multilingualOutput: Boolean,
        val supportedLanguageRefs: List<String>,
        val evidenceRefs: List<String>,
    )

    fun validate(
        relay: JsonObject,
        sourceMessageRef: String,
        recipientRefs: List<String>,
    ): Validation {
        val errors =
            mutableListOf<String>()

        scanForbiddenRelayFields(
            relay,
            "semanticRelay",
            errors,
        )

        if (
            relay.stringOrNull(
                "schemaVersion",
            ) !=
            Schema
        ) {
            errors +=
                "semantic relay schemaVersion is invalid"
        }

        for (
            field in
            listOf(
                "relayRef",
                "sourceMessageRef",
                "sourceLanguageRef",
                "preferredConversationLanguageRef",
                "requestedResponseLanguageRef",
                "originatorRef",
                "interpretationProjectionRef",
                "boundaryClassRef",
            )
        ) {
            requireRef(
                relay[
                    field
                ],
                field,
                errors,
            )
        }

        for (
            field in
            listOf(
                "recipientRefs",
                "intentRefs",
                "canonicalMeaningRefs",
                "sourceRefs",
                "evidenceRefs",
                "authorityRefs",
            )
        ) {
            requireRefs(
                relay[
                    field
                ],
                field,
                errors,
            )
        }

        val originatorKind =
            relay.stringOrNull(
                "originatorKind",
            )
        val interpretationState =
            relay.stringOrNull(
                "interpretationState",
            )
        val ambiguityState =
            relay.stringOrNull(
                "ambiguityState",
            )
        val materiality =
            relay.stringOrNull(
                "materiality",
            )
        val onBehalf =
            relay.booleanOrNull(
                "onBehalfOfOriginator",
            )

        if (
            originatorKind !in
            OriginatorKinds
        ) {
            errors +=
                "originatorKind is invalid"
        }
        if (
            interpretationState !in
            InterpretationStates ||
            interpretationState ==
            "HELD"
        ) {
            errors +=
                "persisted relay interpretationState must be CANDIDATE, CONFIRMED, or CORRECTED"
        }
        if (
            ambiguityState !in
            AmbiguityStates
        ) {
            errors +=
                "ambiguityState is invalid"
        }
        if (
            materiality !in
            MaterialityStates
        ) {
            errors +=
                "materiality is invalid"
        }
        if (
            onBehalf ==
            null
        ) {
            errors +=
                "onBehalfOfOriginator must be boolean"
        }

        for (
            field in
            listOf(
                "sourceLocaleRef",
                "uiLocaleRef",
            )
        ) {
            val value =
                relay[
                    field
                ]
            if (
                value !=
                null &&
                value !is
                JsonNull &&
                !isRef(
                    value,
                )
            ) {
                errors +=
                    "$field is invalid"
            }
        }

        val targets =
            relay.arrayOrNull(
                "targets",
            )
        val sourceLanguageRef =
            relay.stringOrNull(
                "sourceLanguageRef",
            )
        val mustConfirm =
            originatorKind ==
            "HUMAN" &&
                onBehalf ==
                true &&
                materiality ==
                "MATERIAL" &&
                (
                    ambiguityState !=
                    "CLEAR" ||
                        (
                            targets
                                ?.mapNotNull {
                                    it as?
                                    JsonObject
                                }
                                ?.any {
                                    it.stringOrNull(
                                        "targetLanguageRef",
                                    ) !=
                                    sourceLanguageRef
                                } ==
                            true
                        )
                )

        if (
            mustConfirm &&
            interpretationState !in
            setOf(
                "CONFIRMED",
                "CORRECTED",
            )
        ) {
            errors +=
                "material human on-behalf relay lacks exact confirmation/correction"
        }

        if (
            interpretationState in
            setOf(
                "CONFIRMED",
                "CORRECTED",
            )
        ) {
            if (
                !isRef(
                    relay[
                        "confirmedByRef"
                    ],
                ) ||
                !isRef(
                    relay[
                        "confirmationReceiptRef"
                    ],
                )
            ) {
                errors +=
                    "confirmed/corrected relay lacks confirmation refs"
            }
            if (
                originatorKind ==
                "HUMAN" &&
                relay.stringOrNull(
                    "confirmedByRef",
                ) !=
                relay.stringOrNull(
                    "originatorRef",
                )
            ) {
                errors +=
                    "human on-behalf relay was not confirmed by originator"
            }
        }

        if (
            interpretationState ==
            "CORRECTED"
        ) {
            val supersedes =
                relay[
                    "supersedesInterpretationProjectionRef"
                ]
            if (
                !isRef(
                    supersedes,
                ) ||
                supersedes
                    ?.jsonPrimitive
                    ?.contentOrNull ==
                relay.stringOrNull(
                    "interpretationProjectionRef",
                )
            ) {
                errors +=
                    "corrected relay lacks distinct superseded projection ref"
            }
        } else {
            val supersedes =
                relay[
                    "supersedesInterpretationProjectionRef"
                ]
            if (
                supersedes !=
                null &&
                supersedes !is
                JsonNull
            ) {
                errors +=
                    "non-corrected relay cannot supersede an interpretation projection"
            }
        }

        if (
            targets ==
            null ||
            targets.isEmpty()
        ) {
            errors +=
                "targets must be non-empty"
        }

        val recipientSet =
            relay.stringListOrEmpty(
                "recipientRefs",
            )
                .toSet()
        val targetKeys =
            mutableSetOf<String>()

        for (
            (
                index,
                element,
            ) in
            (
                targets
                    ?: JsonArray(
                        emptyList(),
                    )
            )
                .withIndex()
        ) {
            val target =
                element as?
                JsonObject
            val path =
                "targets[$index]"

            if (
                target ==
                null
            ) {
                errors +=
                    "$path must be an object"
                continue
            }

            for (
                field in
                listOf(
                    "recipientRef",
                    "recipientPreferredLanguageRef",
                    "targetLanguageRef",
                    "targetAudienceRef",
                )
            ) {
                requireRef(
                    target[
                        field
                    ],
                    "$path.$field",
                    errors,
                )
            }

            val targetRecipient =
                target.stringOrNull(
                    "recipientRef",
                )
            val targetLanguage =
                target.stringOrNull(
                    "targetLanguageRef",
                )
            if (
                targetRecipient !in
                recipientSet
            ) {
                errors +=
                    "$path.recipientRef is not declared"
            }

            val targetKey =
                (
                    targetRecipient
                        ?: ""
                    ) +
                    "::" +
                    (
                        targetLanguage
                            ?: ""
                        )
            if (
                !targetKeys.add(
                    targetKey,
                )
            ) {
                errors +=
                    "$path duplicates a recipient/language target"
            }

            val projectionMode =
                target.stringOrNull(
                    "projectionMode",
                )
            if (
                projectionMode !in
                ProjectionModes
            ) {
                errors +=
                    "$path.projectionMode is invalid"
            }

            for (
                field in
                listOf(
                    "localeQualityState",
                    "terminologyState",
                    "authorityState",
                )
            ) {
                if (
                    target.stringOrNull(
                        field,
                    ) !in
                    AdmissionStates
                ) {
                    errors +=
                        "$path.$field is invalid"
                }
            }

            if (
                target.stringOrNull(
                    "localizationReadinessState",
                ) !in
                LocalizationReadinessStates
            ) {
                errors +=
                    "$path.localizationReadinessState is invalid"
            }
            if (
                target.booleanOrNull(
                    "humanReviewAvailable",
                ) ==
                null
            ) {
                errors +=
                    "$path.humanReviewAvailable is invalid"
            }
            if (
                target.stringOrNull(
                    "deliveryState",
                ) !in
                DeliveryStates
            ) {
                errors +=
                    "$path.deliveryState is invalid"
            }
            if (
                target.stringOrNull(
                    "acknowledgementState",
                ) !in
                AcknowledgementStates
            ) {
                errors +=
                    "$path.acknowledgementState is invalid"
            }
            if (
                target.stringOrNull(
                    "understandingState",
                ) !in
                UnderstandingStates
            ) {
                errors +=
                    "$path.understandingState is invalid"
            }
            if (
                target.stringOrNull(
                    "semanticEquivalenceState",
                ) !in
                SemanticEquivalenceStates
            ) {
                errors +=
                    "$path.semanticEquivalenceState is invalid"
            }

            requireRefs(
                target[
                    "semanticDriftFindingRefs"
                ],
                "$path.semanticDriftFindingRefs",
                errors,
                allowEmpty = true,
            )

            val runtimeCapability =
                target[
                    "runtimeCapability"
                ] as?
                JsonObject
            if (
                runtimeCapability !=
                null
            ) {
                val capabilityRef =
                    runtimeCapability[
                        "capabilityRef"
                    ]
                if (
                    capabilityRef !=
                        null &&
                    capabilityRef !is
                        JsonNull &&
                    !isRef(
                        capabilityRef,
                    )
                ) {
                    errors +=
                        "$path.runtime capability ref is invalid"
                }

                for (
                    field in
                    listOf(
                        "supportedLanguageRefs",
                        "evidenceRefs",
                    )
                ) {
                    val rawRefs =
                        runtimeCapability[
                            field
                        ]
                    if (
                        rawRefs is
                        JsonArray &&
                        rawRefs.any {
                            !isRef(
                                it,
                            )
                        }
                    ) {
                        errors +=
                            "$path.runtime capability " +
                                if (
                                    field ==
                                    "supportedLanguageRefs"
                                ) {
                                    "supported languages must be an array of stable references"
                                } else {
                                    "evidence must be an array of stable references"
                                }
                    }
                }
            }

            val capability =
                normalizedCapability(
                    target[
                        "runtimeCapability"
                    ],
                )
            if (
                capability.currentnessState !in
                RuntimeCurrentnessStates
            ) {
                errors +=
                    "$path.runtime capability currentness is invalid"
            }
            if (
                capability.capabilityRef !=
                null &&
                capability.capabilityRef.isEmpty()
            ) {
                errors +=
                    "$path.runtime capability ref is invalid"
            }
            if (
                capability.supportedLanguageRefs.any(
                    String::isEmpty,
                )
            ) {
                errors +=
                    "$path.runtime capability supported languages must be an array of stable references"
            }
            if (
                capability.evidenceRefs.any(
                    String::isEmpty,
                )
            ) {
                errors +=
                    "$path.runtime capability evidence must be an array of stable references"
            }

            val derivedProjectionMode =
                projectionMode(
                    target = target,
                    capability = capability,
                )
            if (
                projectionMode in
                ProjectionModes &&
                projectionMode !=
                derivedProjectionMode
            ) {
                errors +=
                    "$path.projectionMode does not match current target evidence; expected $derivedProjectionMode"
            }

            if (
                projectionMode ==
                "MODEL_NATIVE" &&
                !(
                    capability.currentnessState ==
                    "CURRENT" &&
                        capability.multilingualOutput &&
                        targetLanguage in
                        capability.supportedLanguageRefs &&
                        capability.evidenceRefs.isNotEmpty() &&
                        target.stringOrNull(
                            "localeQualityState",
                        ) ==
                        "ADMITTED" &&
                        target.stringOrNull(
                            "terminologyState",
                        ) ==
                        "ADMITTED" &&
                        target.stringOrNull(
                            "authorityState",
                        ) ==
                        "ADMITTED"
                    )
            ) {
                errors +=
                    "$path.MODEL_NATIVE lacks current explicit multilingual capability and owner evidence"
            }

            val deliveryState =
                target.stringOrNull(
                    "deliveryState",
                )
            val acknowledgementState =
                target.stringOrNull(
                    "acknowledgementState",
                )
            val understandingState =
                target.stringOrNull(
                    "understandingState",
                )
            if (
                deliveryState !=
                "DELIVERED" &&
                acknowledgementState ==
                "ACKNOWLEDGED"
            ) {
                errors +=
                    "$path acknowledges before delivery"
            }
            if (
                acknowledgementState !=
                "ACKNOWLEDGED" &&
                understandingState in
                setOf(
                    "UNDERSTOOD",
                    "MISUNDERSTOOD",
                )
            ) {
                errors +=
                    "$path records understanding before acknowledgement"
            }
        }

        for (
            recipientRef in
            recipientSet
        ) {
            val hasTarget =
                targets
                    ?.mapNotNull {
                        it as?
                        JsonObject
                    }
                    ?.any {
                        it.stringOrNull(
                            "recipientRef",
                        ) ==
                        recipientRef
                    } ==
                true
            if (
                !hasTarget
            ) {
                errors +=
                    "recipientRef $recipientRef has no target"
            }
        }

        if (
            relay.stringOrNull(
                "sourceMessageRef",
            ) !=
            sourceMessageRef
        ) {
            errors +=
                "semantic relay sourceMessageRef does not match event/message source identity"
        }

        if (
            relay.stringListOrEmpty(
                "recipientRefs",
            )
                .distinct()
                .toSet() !=
            recipientRefs
                .distinct()
                .toSet()
        ) {
            errors +=
                "semantic relay recipientRefs do not match event/message recipients"
        }

        return Validation(
            ok =
                errors.isEmpty(),
            errors =
                errors.distinct(),
        )
    }

    private fun projectionMode(
        target: JsonObject,
        capability: RuntimeCapability,
    ): String {
        val ownerStates =
            listOf(
                target.stringOrNull(
                    "localeQualityState",
                ),
                target.stringOrNull(
                    "terminologyState",
                ),
                target.stringOrNull(
                    "authorityState",
                ),
            )

        if (
            ownerStates.any {
                it ==
                    "UNAVAILABLE" ||
                    it ==
                    "UNKNOWN"
            }
        ) {
            return "NONE"
        }
        if (
            ownerStates.any {
                it ==
                    "REVIEW_REQUIRED"
            }
        ) {
            return if (
                target.booleanOrNull(
                    "humanReviewAvailable",
                ) ==
                true
            ) {
                "HUMAN_REVIEW"
            } else {
                "NONE"
            }
        }

        val admitted =
            ownerStates.all {
                it ==
                    "ADMITTED"
            }

        if (
            admitted &&
            capability.currentnessState ==
            "CURRENT" &&
            capability.multilingualOutput &&
            target.stringOrNull(
                "targetLanguageRef",
            )
                ?.let {
                    it in
                        capability.supportedLanguageRefs
                } ==
            true &&
            capability.evidenceRefs.isNotEmpty()
        ) {
            return "MODEL_NATIVE"
        }
        if (
            admitted &&
            target.stringOrNull(
                "localizationReadinessState",
            ) ==
            "TRANSLATION_READY"
        ) {
            return "LOCALIZATION_PIPELINE"
        }
        if (
            admitted &&
            target.booleanOrNull(
                "humanReviewAvailable",
            ) ==
            true
        ) {
            return "HUMAN_REVIEW"
        }

        return "NONE"
    }

    private fun normalizedCapability(
        element: JsonElement?,
    ): RuntimeCapability {
        val value =
            element as?
            JsonObject
                ?: return RuntimeCapability(
                    capabilityRef = null,
                    currentnessState = "UNKNOWN",
                    multilingualOutput = false,
                    supportedLanguageRefs = emptyList(),
                    evidenceRefs = emptyList(),
                )

        return RuntimeCapability(
            capabilityRef =
                value.stringOrNull(
                    "capabilityRef",
                ),
            currentnessState =
                value.stringOrNull(
                    "currentnessState",
                )
                    ?: "UNKNOWN",
            multilingualOutput =
                value.booleanOrNull(
                    "multilingualOutput",
                ) ==
                true,
            supportedLanguageRefs =
                value.stringListOrEmpty(
                    "supportedLanguageRefs",
                ),
            evidenceRefs =
                value.stringListOrEmpty(
                    "evidenceRefs",
                ),
        )
    }

    private fun scanForbiddenRelayFields(
        element: JsonElement,
        path: String,
        errors: MutableList<String>,
    ) {
        when (
            element
        ) {
            is JsonArray ->
                element.forEachIndexed {
                    index,
                    child,
                    ->
                    scanForbiddenRelayFields(
                        child,
                        "$path[$index]",
                        errors,
                    )
                }

            is JsonObject ->
                for (
                    (
                        key,
                        child,
                    ) in
                    element
                ) {
                    if (
                        key in
                        ForbiddenRelayFields
                    ) {
                        errors +=
                            "$path.$key is forbidden in reference-only semantic relay metadata"
                    } else {
                        scanForbiddenRelayFields(
                            child,
                            "$path.$key",
                            errors,
                        )
                    }
                }

            else ->
                Unit
        }
    }

    private fun requireRef(
        element: JsonElement?,
        name: String,
        errors: MutableList<String>,
    ) {
        if (
            !isRef(
                element,
            )
        ) {
            errors +=
                "$name must be a non-empty stable reference"
        }
    }

    private fun requireRefs(
        element: JsonElement?,
        name: String,
        errors: MutableList<String>,
        allowEmpty: Boolean =
            false,
    ) {
        val values =
            element as?
            JsonArray

        if (
            values ==
            null ||
            (
                !allowEmpty &&
                values.isEmpty()
                ) ||
            values.any {
                !isRef(
                    it,
                )
            }
        ) {
            errors +=
                "$name must be an array of stable references"
        }
    }

    private fun isRef(
        element: JsonElement?,
    ): Boolean {
        val primitive =
            element as?
            JsonPrimitive
                ?: return false
        return primitive.isString &&
            primitive.content.isNotEmpty()
    }

    private fun JsonObject.stringOrNull(
        name: String,
    ): String? {
        val value =
            this[
                name
            ]
                ?: return null
        if (
            value is
            JsonNull
        ) {
            return null
        }
        val primitive =
            value as?
            JsonPrimitive
                ?: return null
        return primitive.contentOrNull
    }

    private fun JsonObject.booleanOrNull(
        name: String,
    ): Boolean? =
        (
            this[
                name
            ] as?
            JsonPrimitive
            )
            ?.booleanOrNull

    private fun JsonObject.arrayOrNull(
        name: String,
    ): JsonArray? =
        this[
            name
        ] as?
        JsonArray

    private fun JsonObject.stringListOrEmpty(
        name: String,
    ): List<String> =
        arrayOrNull(
            name,
        )
            ?.mapNotNull {
                (
                    it as?
                    JsonPrimitive
                    )
                    ?.contentOrNull
            }
            ?: emptyList()
}

// [VXG RealForever]
