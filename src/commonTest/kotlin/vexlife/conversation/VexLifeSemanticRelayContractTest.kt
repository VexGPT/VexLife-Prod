package vexlife.conversation

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VexLifeSemanticRelayContractTest {
    private fun relay(): JsonObject =
        buildJsonObject {
            put(
                "schemaVersion",
                VexLifeSemanticRelayContract.Schema,
            )
            put(
                "relayRef",
                "relay.vexlife.test",
            )
            put(
                "sourceMessageRef",
                "message.vexlife.test",
            )
            put(
                "sourceLanguageRef",
                "language.en",
            )
            put(
                "sourceLocaleRef",
                kotlinx.serialization.json.JsonNull,
            )
            put(
                "preferredConversationLanguageRef",
                "language.en",
            )
            put(
                "requestedResponseLanguageRef",
                "language.en",
            )
            put(
                "uiLocaleRef",
                kotlinx.serialization.json.JsonNull,
            )
            put(
                "originatorRef",
                "person.local-user",
            )
            put(
                "originatorKind",
                "HUMAN",
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
                "CLEAR",
            )
            put(
                "recipientRefs",
                buildJsonArray {
                    add(
                        JsonPrimitive(
                            "role.vex.companion",
                        ),
                    )
                },
            )
            put(
                "intentRefs",
                buildJsonArray {
                    add(
                        JsonPrimitive(
                            "intent.test",
                        ),
                    )
                },
            )
            put(
                "canonicalMeaningRefs",
                buildJsonArray {
                    add(
                        JsonPrimitive(
                            "meaning.test",
                        ),
                    )
                },
            )
            put(
                "interpretationProjectionRef",
                "interpretation.test",
            )
            put(
                "interpretationState",
                "CANDIDATE",
            )
            put(
                "confirmedByRef",
                kotlinx.serialization.json.JsonNull,
            )
            put(
                "confirmationReceiptRef",
                kotlinx.serialization.json.JsonNull,
            )
            put(
                "supersedesInterpretationProjectionRef",
                kotlinx.serialization.json.JsonNull,
            )
            put(
                "boundaryClassRef",
                "boundary.test",
            )
            put(
                "targets",
                buildJsonArray {
                    add(
                        buildJsonObject {
                            put(
                                "recipientRef",
                                "role.vex.companion",
                            )
                            put(
                                "recipientPreferredLanguageRef",
                                "language.en",
                            )
                            put(
                                "targetLanguageRef",
                                "language.en",
                            )
                            put(
                                "targetAudienceRef",
                                "audience.vex",
                            )
                            put(
                                "runtimeCapability",
                                buildJsonObject {
                                    put(
                                        "capabilityRef",
                                        kotlinx.serialization.json.JsonNull,
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
                                        buildJsonArray {
                                        },
                                    )
                                    put(
                                        "evidenceRefs",
                                        buildJsonArray {
                                        },
                                    )
                                },
                            )
                            put(
                                "localeQualityState",
                                "ADMITTED",
                            )
                            put(
                                "terminologyState",
                                "ADMITTED",
                            )
                            put(
                                "authorityState",
                                "ADMITTED",
                            )
                            put(
                                "localizationReadinessState",
                                "TRANSLATION_READY",
                            )
                            put(
                                "humanReviewAvailable",
                                false,
                            )
                            put(
                                "projectionMode",
                                "LOCALIZATION_PIPELINE",
                            )
                            put(
                                "localizedProjectionRef",
                                kotlinx.serialization.json.JsonNull,
                            )
                            put(
                                "semanticEquivalenceState",
                                "NOT_CHECKED",
                            )
                            put(
                                "semanticDriftFindingRefs",
                                buildJsonArray {
                                },
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
                        },
                    )
                },
            )
            put(
                "sourceRefs",
                buildJsonArray {
                    add(
                        JsonPrimitive(
                            "source.test",
                        ),
                    )
                },
            )
            put(
                "evidenceRefs",
                buildJsonArray {
                    add(
                        JsonPrimitive(
                            "evidence.test",
                        ),
                    )
                },
            )
            put(
                "authorityRefs",
                buildJsonArray {
                    add(
                        JsonPrimitive(
                            "authority.test",
                        ),
                    )
                },
            )
        }

    @Test
    fun storedReferenceOnlyRelayValidatesAgainstEventIdentity() {
        val result =
            VexLifeSemanticRelayContract.validate(
                relay = relay(),
                sourceMessageRef = "message.vexlife.test",
                recipientRefs =
                    listOf(
                        "role.vex.companion",
                    ),
            )

        assertTrue(
            result.ok,
            result.errors.joinToString(),
        )
    }

    @Test
    fun storedRelayRejectsNonStringReferences() {
        val base =
            relay()
        val target =
            (
                base[
                    "targets"
                ] as
                JsonArray
                )[0] as
                JsonObject
        val capability =
            target[
                "runtimeCapability"
            ] as
                JsonObject

        val numericRelayRef =
            JsonObject(
                base +
                    (
                        "relayRef" to
                            JsonPrimitive(
                                42,
                            )
                        ),
            )
        val numericCapabilityRef =
            JsonObject(
                base +
                    (
                        "targets" to
                            buildJsonArray {
                                add(
                                    JsonObject(
                                        target +
                                            (
                                                "runtimeCapability" to
                                                    JsonObject(
                                                        capability +
                                                            (
                                                                "capabilityRef" to
                                                                    JsonPrimitive(
                                                                        42,
                                                                    )
                                                                ),
                                                    )
                                                ),
                                    ),
                                )
                            }
                        ),
            )
        val numericCapabilityEvidenceRef =
            JsonObject(
                base +
                    (
                        "targets" to
                            buildJsonArray {
                                add(
                                    JsonObject(
                                        target +
                                            (
                                                "runtimeCapability" to
                                                    JsonObject(
                                                        capability +
                                                            (
                                                                "evidenceRefs" to
                                                                    buildJsonArray {
                                                                        add(
                                                                            JsonPrimitive(
                                                                                42,
                                                                            ),
                                                                        )
                                                                    }
                                                                ),
                                                    )
                                                ),
                                    ),
                                )
                            }
                        ),
            )

        for (
            value in
            listOf(
                numericRelayRef,
                numericCapabilityRef,
                numericCapabilityEvidenceRef,
            )
        ) {
            assertFalse(
                VexLifeSemanticRelayContract.validate(
                    relay = value,
                    sourceMessageRef = "message.vexlife.test",
                    recipientRefs =
                        listOf(
                            "role.vex.companion",
                        ),
                )
                    .ok,
            )
        }
    }

    @Test
    fun storedRelayRejectsRawContentAndWrongEventBinding() {
        val withRawContent =
            JsonObject(
                relay() +
                    (
                        "content" to
                            JsonPrimitive(
                                "must not be persisted",
                            )
                        ),
            )

        assertFalse(
            VexLifeSemanticRelayContract.validate(
                relay = withRawContent,
                sourceMessageRef = "message.vexlife.test",
                recipientRefs =
                    listOf(
                        "role.vex.companion",
                    ),
            )
                .ok,
        )
        assertFalse(
            VexLifeSemanticRelayContract.validate(
                relay = relay(),
                sourceMessageRef = "message.vexlife.other",
                recipientRefs =
                    listOf(
                        "role.vex.companion",
                    ),
            )
                .ok,
        )
    }
}

// [VXG RealForever]
