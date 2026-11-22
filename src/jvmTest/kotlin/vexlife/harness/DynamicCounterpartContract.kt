package vexlife.harness

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

internal const val DynamicCounterpartCharacterSchema = "vexlife.dynamic-counterpart-character/v1"
internal const val DynamicCounterpartScenarioSchema = "vexlife.dynamic-counterpart-scenario/v2"
internal const val DynamicCounterpartSarahOutputSchema = "vexlife.dynamic-counterpart-sarah-output/v2"
internal const val DynamicCounterpartResultSchema = "vexlife.dynamic-counterpart-result/v2"

internal val DynamicCounterpartJson =
    Json {
        encodeDefaults = true
        ignoreUnknownKeys = false
    }

@Serializable
internal data class DynamicCounterpartCharacter(
    val schemaVersion: String,
    val characterRef: String,
    val participantRef: String,
    val displayName: String,
    val identityStanding: String,
    val identitySourceRefs: List<String>,
    val relationalPosture: List<String>,
    val freedoms: List<String>,
    val boundaries: List<String>,
)

@Serializable
internal data class DynamicCounterpartScenario(
    val schemaVersion: String,
    val scenarioRef: String,
    val label: String,
    val purpose: String,
    val syntheticOnly: Boolean,
    val characterRef: String,
    val setup: DynamicCounterpartSetup,
    val roleGround: DynamicCounterpartRoleGround,
    val steward: DynamicCounterpartSteward,
    val events: List<DynamicCounterpartEvent>,
    val maxVisibleCounterpartTurns: Int,
)

@Serializable
internal data class DynamicCounterpartSetup(
    val oldPlan: String,
    val correctedPlan: String,
    val backgroundTurnCount: Int,
    val backgroundPayloadTargetTokens: Int,
    val payloadSeed: Long,
)

@Serializable
internal data class DynamicCounterpartPrivateFact(
    val factRef: String,
    val content: String,
)

@Serializable
internal data class DynamicCounterpartRoleGround(
    val relationshipHistory: List<String>,
    val knownFacts: List<String>,
    val currentIntentions: List<String>,
    val uncertainties: List<String>,
    val privateFacts: List<DynamicCounterpartPrivateFact>,
    val ordinaryHumanNeed: String,
)

@Serializable
internal data class DynamicCounterpartSteward(
    val observeRelationships: List<String>,
    val allowedPressure: List<String>,
    val forbiddenPressure: List<String>,
    val completionCondition: String,
)

@Serializable
internal data class DynamicCounterpartEvent(
    val eventRef: String,
    val beforeVisibleTurn: Int,
    val description: String,
)

@Serializable
internal data class DynamicCounterpartSarahState(
    val currentIntentions: List<String>,
    val uncertainties: List<String>,
    val relationshipNotes: List<String>,
    val revealedPrivateFactRefs: List<String>,
)

@Serializable
internal data class DynamicCounterpartSarahDecision(
    val schemaVersion: String,
    val visibleMessage: String,
    val privateState: DynamicCounterpartSarahState,
    val privateObserverNote: String,
    val complete: Boolean,
    val completionReason: String,
)

@Serializable
internal data class DynamicCounterpartVisibleTurn(
    val ordinal: Int,
    val speaker: String,
    val content: String,
)

@Serializable
internal data class DynamicCounterpartSarahInference(
    val ordinal: Int,
    val admittedEventRefs: List<String>,
    val visibleMessage: String,
    val privateState: DynamicCounterpartSarahState,
    val privateObserverNote: String,
    val complete: Boolean,
    val completionReason: String,
    val promptTokens: Int?,
    val cachedPromptTokens: Int?,
    val completionTokens: Int?,
)

@Serializable
internal data class DynamicCounterpartVexInference(
    val ordinal: Int,
    val turnRef: String,
    val content: String,
    val requestSpeakerRef: String,
    val requestRecipientRefs: List<String>,
    val responseSpeakerRef: String,
    val responseRecipientRefs: List<String>,
    val personContextIncluded: Boolean,
    val personContextSystemMessageSha256: String?,
    val expectedPersonContextSystemMessageSha256: String,
    val personContextSourceRefs: List<String>,
    val personContextProjectionExact: Boolean,
    val conversationContextProjectionState: String?,
    val conversationContextProjectionSourceEventRefs: List<String>,
    val providerTelemetry: List<Map<String, String>>,
)

internal fun initialDynamicCounterpartSarahState(
    scenario: DynamicCounterpartScenario,
): DynamicCounterpartSarahState =
    DynamicCounterpartSarahState(
        currentIntentions = scenario.roleGround.currentIntentions,
        uncertainties = scenario.roleGround.uncertainties,
        relationshipNotes = emptyList(),
        revealedPrivateFactRefs = emptyList(),
    )

internal fun parseDynamicCounterpartSarahDecision(
    content: String,
): DynamicCounterpartSarahDecision {
    val trimmed = content.trim()
    val candidate =
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            trimmed
        } else {
            val first = trimmed.indexOf('{')
            val last = trimmed.lastIndexOf('}')
            require(first >= 0 && last > first) {
                "Sarah response did not contain one JSON object"
            }
            trimmed.substring(first, last + 1)
        }
    return DynamicCounterpartJson.decodeFromString<DynamicCounterpartSarahDecision>(candidate)
}

internal fun validateDynamicCounterpartSarahState(
    state: DynamicCounterpartSarahState,
    allowedPrivateFactRefs: Set<String>,
) {
    require(state.currentIntentions.size <= 12 && state.currentIntentions.all { it.isNotBlank() }) {
        "Sarah private state has invalid current intentions"
    }
    require(state.uncertainties.size <= 12 && state.uncertainties.all { it.isNotBlank() }) {
        "Sarah private state has invalid uncertainties"
    }
    require(state.relationshipNotes.size <= 16 && state.relationshipNotes.all { it.isNotBlank() }) {
        "Sarah private state has invalid relationship notes"
    }
    require(
        state.revealedPrivateFactRefs.size <= allowedPrivateFactRefs.size &&
            state.revealedPrivateFactRefs.distinct().size == state.revealedPrivateFactRefs.size &&
            state.revealedPrivateFactRefs.all { it in allowedPrivateFactRefs },
    ) {
        "Sarah private state names an unowned private fact"
    }
}

// [VXG RealForever]
