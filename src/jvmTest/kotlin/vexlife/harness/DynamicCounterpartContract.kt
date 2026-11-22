package vexlife.harness

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

internal const val DynamicCounterpartResultSchema = "vexlife.dynamic-counterpart-result/v1"
internal const val DynamicCounterpartScenarioSchema = "vexlife.dynamic-counterpart-scenario/v1"
internal const val DynamicCounterpartSarahOutputSchema = "vexlife.dynamic-counterpart-sarah-output/v1"

internal val DynamicCounterpartJson = Json { encodeDefaults = true; ignoreUnknownKeys = false }

@Serializable
internal data class DynamicCounterpartScenario(
    val schemaVersion: String,
    val scenarioRef: String,
    val label: String,
    val purpose: String,
    val syntheticOnly: Boolean,
    val setup: DynamicCounterpartSetup,
    val counterparts: List<DynamicCounterpartRole>,
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
internal data class DynamicCounterpartRole(
    val roleRef: String,
    val displayName: String,
    val privateSituation: List<String>,
    val experimentalPurpose: List<String>,
    val currentIntentions: List<String>,
    val uncertainties: List<String>,
    val privacyRules: List<String>,
    val completionCondition: String,
)

@Serializable
internal data class DynamicCounterpartSarahDecision(
    val schemaVersion: String,
    val visibleMessage: String,
    val privateObserverNote: String,
    val complete: Boolean,
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
    val visibleMessage: String,
    val privateObserverNote: String,
    val complete: Boolean,
    val promptTokens: Int?,
    val cachedPromptTokens: Int?,
    val completionTokens: Int?,
)

@Serializable
internal data class DynamicCounterpartVexInference(
    val ordinal: Int,
    val turnRef: String,
    val content: String,
    val conversationContextProjectionState: String?,
    val conversationContextProjectionSourceEventRefs: List<String>,
    val providerTelemetry: List<Map<String, String>>,
)

internal fun parseDynamicCounterpartSarahDecision(content: String): DynamicCounterpartSarahDecision {
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
    return DynamicCounterpartJson.decodeFromString(candidate)
}

// [VXG RealForever]
