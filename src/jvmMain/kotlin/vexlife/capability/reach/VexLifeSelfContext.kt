package vexlife.capability.reach

import java.nio.file.Path
import vexlife.companion.DirectCompanionConnection
import vexlife.companion.DirectPromptContext
import vexlife.companion.DirectWorkingFocus

/**
 * Read-only Self-Inspection projection for one active human turn.
 *
 * This is not a self store and owns none of the values it reports. The turn binds the exact prompt
 * receipt, the Working Focus observation used to build that prompt, and the provider-stage
 * measurement that already occurred. This adapter reads current Working Focus only to compare that
 * rightful owner with the prompt-bound observation.
 *
 * It deliberately does not return prompt/provider bodies, human content, Memory bodies, raw
 * reasoning, or source contents. Source refs are coordinates only and grant no read/effect authority.
 */
internal object VexLifeSelfContext {
    data class TurnContext(
        val companionLineageRef: String,
        val threadRef: String,
        val promptReceipt: DirectPromptContext.Receipt,
        val workingFocusObservation: DirectWorkingFocus.Observation,
        val providerMeasurement: DirectCompanionConnection.StageMeasurement?,
    )

    data class Result(
        val text: String,
        val sourceRefs: List<String>,
        val workingFocusCurrentness: String,
        val effectsPerformed: Boolean = false,
    ) {
        init {
            require(!effectsPerformed) {
                "self context cannot perform effects"
            }
        }
    }

    fun inspect(
        productHome: Path,
        context: TurnContext,
    ): Result {
        val receipt = context.promptReceipt
        val currentFocus =
            DirectWorkingFocus.observeCurrent(
                productHome = productHome,
                lineageRef = context.companionLineageRef,
                threadRef = context.threadRef,
            )
        val focusCurrentness =
            if (currentFocus == context.workingFocusObservation) {
                "CURRENT"
            } else {
                "CHANGED_SINCE_PROMPT"
            }
        val focus = receipt.workingFocusSnapshot

        val text =
            buildString {
                appendLine("Self context for this human turn")
                appendLine("prompt receipt: ${receipt.receiptRef}")
                appendLine("prompt schema: ${receipt.schemaVersion}")
                appendLine("context mode: ${receipt.contextMode}")
                appendLine(
                    "provider input estimate: ${receipt.providerMaterializedInputTokenEstimate} tokens " +
                        "of ${receipt.inputTokenBudget}; reserved output ${receipt.reservedOutputTokens}; " +
                        "hard limit ${receipt.hardTokenLimit}",
                )
                appendLine("components:")
                receipt.contextComponents
                    .sortedBy {
                        it.logicalOrder
                    }
                    .forEach {
                        component ->
                        appendLine(
                            "- ${component.componentRef} | ${component.stabilityClass.name} | " +
                                "${component.placementClass.name} | message ${component.providerMessageIndex} | " +
                                "sha256 ${component.sha256}",
                        )
                    }

                appendLine("Working Focus:")
                appendLine("- currentness against owner now: $focusCurrentness")
                if (focus == null) {
                    appendLine("- receipt predates Working Focus self-context evidence")
                } else {
                    appendLine("- observed standing: ${focus.observedStanding}")
                    appendLine("- projection: ${focus.projectionState}")
                    appendLine("- workingFocusRef: ${focus.workingFocusRefOrNull ?: "NONE"}")
                    appendLine("- stateRef: ${focus.stateRefOrNull ?: "NONE"}")
                    appendLine("- revisionRef: ${focus.revisionRefOrNull ?: "NONE"}")
                    appendLine("- resident content sha256: ${focus.contentSha256OrNull ?: "NONE"}")
                    appendLine("- estimated tokens: ${focus.estimatedTokens}")
                    appendLine("- required core tokens: ${focus.requiredCoreTokens}")
                    appendLine("- details included: ${focus.detailsIncluded}")
                    appendLine(
                        "- held relationship refs: " +
                            focus.heldRelationshipRefs.ifEmpty { listOf("NONE") }.joinToString(", "),
                    )
                    appendLine(
                        "- held open-edge refs: " +
                            focus.heldOpenEdgeRefs.ifEmpty { listOf("NONE") }.joinToString(", "),
                    )
                    appendLine(
                        "- source coordinates: " +
                            focus.sourceRefs.ifEmpty { listOf("NONE") }.joinToString(", "),
                    )
                }

                appendLine("provider stage already observed:")
                val measurement = context.providerMeasurement
                if (measurement == null) {
                    appendLine("- measurement: NONE")
                } else {
                    appendLine("- stop: ${measurement.stop.wire}")
                    appendLine("- finish reason: ${measurement.finishReason ?: "NONE"}")
                    appendLine("- prompt tokens: ${measurement.promptTokens ?: "UNKNOWN"}")
                    appendLine("- cached prompt tokens: ${measurement.cachedPromptTokens ?: "UNKNOWN"}")
                    appendLine("- prefill tokens: ${measurement.prefillTokens ?: "UNKNOWN"}")
                    appendLine("- completion tokens: ${measurement.completionTokens ?: "UNKNOWN"}")
                    appendLine("- first token ms: ${measurement.firstTokenMillis ?: "UNKNOWN"}")
                    appendLine("- first answer ms: ${measurement.firstAnswerMillis ?: "UNKNOWN"}")
                    appendLine("- total ms: ${measurement.totalMillis}")
                }
                append(
                    "This read reports owner/receipt/measurement evidence only. " +
                        "It contains no prompt body, provider body, private reasoning, Memory body, or source contents.",
                )
            }

        return Result(
            text = text,
            sourceRefs =
                (
                    listOf(
                        receipt.receiptRef,
                    ) +
                        focus
                            ?.sourceRefs
                            .orEmpty()
                )
                    .distinct()
                    .sorted(),
            workingFocusCurrentness = focusCurrentness,
        )
    }
}

// [VXG RealForever]
