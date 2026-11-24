package vexlife.conversation

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Non-interpretive production composer for the reference-only Semantic Relay attached to one direct
 * canonical message.
 *
 * The canonical Conversation already owns the message content and participant identity before this
 * composer runs. This owner therefore forms only the minimum reference relationship needed to say:
 *
 * - this exact message is the source object;
 * - these exact recipients are the delivery targets;
 * - no language/localization/runtime interpretation has been established here;
 * - no raw content is duplicated into Relay metadata.
 *
 * The message-bound intent/meaning refs below are addresses for unresolved source meaning, not a
 * semantic classification of the person's words.
 *
 * Permanent:
 *
 * MESSAGE SOURCE IDENTITY != INTERPRETED MEANING
 * REFERENCE-ONLY RELAY != TRANSLATION
 * REFERENCE-ONLY RELAY != PROVIDER AUTHORITY
 * RELAY METADATA != RAW MESSAGE CONTENT
 */
object VexLifeDirectMessageSemanticRelay {
    enum class OriginatorKind(
        val wireValue: String,
    ) {
        HUMAN(
            "HUMAN",
        ),
        AI(
            "AI",
        ),
    }

    fun form(
        messageRef: String,
        originatorRef: String,
        originatorKind: OriginatorKind,
        recipientRefs: List<String>,
    ): JsonObject {
        require(
            messageRef.isNotBlank() &&
                originatorRef.isNotBlank() &&
                recipientRefs.isNotEmpty() &&
                recipientRefs.none(
                    String::isBlank,
                ) &&
                recipientRefs.distinct().size ==
                recipientRefs.size,
        ) {
            "direct Semantic Relay requires exact non-empty message/originator/recipient refs"
        }

        val relay =
            buildJsonObject {
                put(
                    "schemaVersion",
                    VexLifeSemanticRelayContract.Schema,
                )
                put(
                    "relayRef",
                    "relay.$messageRef",
                )
                put(
                    "sourceMessageRef",
                    messageRef,
                )
                put(
                    "sourceLanguageRef",
                    UnknownLanguageRef,
                )
                put(
                    "sourceLocaleRef",
                    JsonNull,
                )
                put(
                    "preferredConversationLanguageRef",
                    UnknownLanguageRef,
                )
                put(
                    "requestedResponseLanguageRef",
                    UnknownLanguageRef,
                )
                put(
                    "uiLocaleRef",
                    JsonNull,
                )
                put(
                    "originatorRef",
                    originatorRef,
                )
                put(
                    "originatorKind",
                    originatorKind.wireValue,
                )
                put(
                    "onBehalfOfOriginator",
                    false,
                )
                put(
                    "materiality",
                    "ORDINARY",
                )
                put(
                    "ambiguityState",
                    "UNKNOWN",
                )
                put(
                    "recipientRefs",
                    JsonArray(
                        recipientRefs.map(
                            ::JsonPrimitive,
                        ),
                    ),
                )
                put(
                    "intentRefs",
                    JsonArray(
                        listOf(
                            JsonPrimitive(
                                "intent.uninterpreted.$messageRef",
                            ),
                        ),
                    ),
                )
                put(
                    "canonicalMeaningRefs",
                    JsonArray(
                        listOf(
                            JsonPrimitive(
                                "meaning.uninterpreted.$messageRef",
                            ),
                        ),
                    ),
                )
                put(
                    "interpretationProjectionRef",
                    "projection.semantic-relay.uninterpreted.$messageRef",
                )
                put(
                    "interpretationState",
                    "CANDIDATE",
                )
                put(
                    "confirmedByRef",
                    JsonNull,
                )
                put(
                    "confirmationReceiptRef",
                    JsonNull,
                )
                put(
                    "supersedesInterpretationProjectionRef",
                    JsonNull,
                )
                put(
                    "boundaryClassRef",
                    DirectMessageBoundaryRef,
                )
                put(
                    "targets",
                    JsonArray(
                        recipientRefs.map {
                            recipientRef ->
                            buildJsonObject {
                                put(
                                    "recipientRef",
                                    recipientRef,
                                )
                                put(
                                    "recipientPreferredLanguageRef",
                                    UnknownLanguageRef,
                                )
                                put(
                                    "targetLanguageRef",
                                    UnknownLanguageRef,
                                )
                                put(
                                    "targetAudienceRef",
                                    "audience.$recipientRef",
                                )
                                put(
                                    "runtimeCapability",
                                    buildJsonObject {
                                        put(
                                            "capabilityRef",
                                            JsonNull,
                                        )
                                        put(
                                            "currentnessState",
                                            "UNKNOWN",
                                        )
                                        put(
                                            "multilingualOutput",
                                            false,
                                        )
                                        put(
                                            "supportedLanguageRefs",
                                            JsonArray(
                                                emptyList(),
                                            ),
                                        )
                                        put(
                                            "evidenceRefs",
                                            JsonArray(
                                                emptyList(),
                                            ),
                                        )
                                    },
                                )
                                put(
                                    "localeQualityState",
                                    "UNKNOWN",
                                )
                                put(
                                    "terminologyState",
                                    "UNKNOWN",
                                )
                                put(
                                    "authorityState",
                                    "UNKNOWN",
                                )
                                put(
                                    "localizationReadinessState",
                                    "UNKNOWN",
                                )
                                put(
                                    "humanReviewAvailable",
                                    false,
                                )
                                put(
                                    "projectionMode",
                                    "NONE",
                                )
                                put(
                                    "localizedProjectionRef",
                                    JsonNull,
                                )
                                put(
                                    "semanticEquivalenceState",
                                    "NOT_CHECKED",
                                )
                                put(
                                    "semanticDriftFindingRefs",
                                    JsonArray(
                                        emptyList(),
                                    ),
                                )
                                put(
                                    "deliveryState",
                                    "NOT_DELIVERED",
                                )
                                put(
                                    "acknowledgementState",
                                    "NOT_REQUESTED",
                                )
                                put(
                                    "understandingState",
                                    "NOT_ASSESSED",
                                )
                            }
                        },
                    ),
                )
                put(
                    "sourceRefs",
                    JsonArray(
                        listOf(
                            JsonPrimitive(
                                messageRef,
                            ),
                        ),
                    ),
                )
                put(
                    "evidenceRefs",
                    JsonArray(
                        listOf(
                            JsonPrimitive(
                                "evidence.semantic-relay.source-message.$messageRef",
                            ),
                        ),
                    ),
                )
                put(
                    "authorityRefs",
                    JsonArray(
                        listOf(
                            JsonPrimitive(
                                ConversationAuthorityRef,
                            ),
                        ),
                    ),
                )
            }

        val validation =
            VexLifeSemanticRelayContract.validate(
                relay = relay,
                sourceMessageRef = messageRef,
                recipientRefs = recipientRefs,
            )
        require(
            validation.ok,
        ) {
            "formed direct Semantic Relay is invalid: " +
                validation.errors.joinToString()
        }

        return relay
    }

    private const val UnknownLanguageRef =
        "language.unknown"

    private const val DirectMessageBoundaryRef =
        "boundary.device-private.direct-message"

    private const val ConversationAuthorityRef =
        "authority.vexlife.conversation.direct-message"
}

// [VXG RealForever]
