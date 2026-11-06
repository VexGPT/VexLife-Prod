package vexlife.continuity

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * Portable contract for the current VexLife continuity observation used by Score source intake.
 *
 * This owner only forms the semantic observation core from already-owned Conversation event facts.
 * It performs no filesystem read, Score write, Memory admission or provider effect.
 */
object VexLifeContinuityObservation {
    const val Schema =
        "vexlife.continuity-observation/v1"

    const val ConversationEpisodeRange =
        "CONVERSATION_EPISODE_RANGE"

    const val Current =
        "CURRENT"

    const val Private =
        "PRIVATE"

    @Serializable
    data class ConversationEventFact(
        val eventRef: String,
        val eventHash: String,
        val speakerRef: String,
        val recipientRefs: List<String>,
    )

    @Serializable
    data class SourceBinding(
        val sourceLineageRef: String,
        val rangeRef: String,
        val sourceHash: String,
    )

    @Serializable
    data class Core(
        val schemaVersion: String,
        val observationType: String,
        val sourceLineageRef: String,
        val sourceBindings: List<SourceBinding>,
        val sourceSpeakerRefs: List<String>,
        val sourceRecipientRefs: List<String>,
        val projectRef: String?,
        val threadRef: String?,
        val channelRef: String?,
        val turnRef: String?,
        val workNodeRef: String?,
        val formedByRef: String,
        val formedAt: String,
        val currentness: String,
        val visibility: String,
        val summaryRef: String?,
        val recurrenceBinding: JsonObject?,
        val rawSourceContentIncluded: Boolean,
        val rawSourcesRemainImmutable: Boolean,
        val rawSourcesRetrievableByRef: Boolean,
    )

    @Serializable
    data class Observation(
        val schemaVersion: String,
        val observationType: String,
        val sourceLineageRef: String,
        val sourceBindings: List<SourceBinding>,
        val sourceSpeakerRefs: List<String>,
        val sourceRecipientRefs: List<String>,
        val projectRef: String?,
        val threadRef: String?,
        val channelRef: String?,
        val turnRef: String?,
        val workNodeRef: String?,
        val formedByRef: String,
        val formedAt: String,
        val currentness: String,
        val visibility: String,
        val summaryRef: String?,
        val recurrenceBinding: JsonObject?,
        val rawSourceContentIncluded: Boolean,
        val rawSourcesRemainImmutable: Boolean,
        val rawSourcesRetrievableByRef: Boolean,
        val observationRef: String,
        val semanticFingerprint: String,
    )

    fun conversationEpisodeRangeCore(
        sourceLineageRef: String,
        sourceEvents: List<ConversationEventFact>,
        threadRef: String,
        formedByRef: String,
        formedAt: String,
    ): Core {
        require(
            sourceEvents.isNotEmpty(),
        ) {
            "continuity observation requires one or more exact source events"
        }

        val bindings =
            sourceEvents
                .map {
                    event ->
                    SourceBinding(
                        sourceLineageRef = sourceLineageRef,
                        rangeRef = event.eventRef,
                        sourceHash = event.eventHash,
                    )
                }
                .distinct()
                .sortedWith(
                    compareBy(
                        SourceBinding::sourceLineageRef,
                        SourceBinding::rangeRef,
                    ),
                )
        require(
            bindings.size ==
                sourceEvents.size,
        ) {
            "continuity observation source range must not contain duplicates"
        }

        return Core(
            schemaVersion = Schema,
            observationType = ConversationEpisodeRange,
            sourceLineageRef = sourceLineageRef,
            sourceBindings = bindings,
            sourceSpeakerRefs =
                sourceEvents
                    .map(
                        ConversationEventFact::speakerRef,
                    )
                    .distinct()
                    .sorted(),
            sourceRecipientRefs =
                sourceEvents
                    .flatMap(
                        ConversationEventFact::recipientRefs,
                    )
                    .distinct()
                    .sorted(),
            projectRef = null,
            threadRef = threadRef,
            channelRef = null,
            turnRef = null,
            workNodeRef = null,
            formedByRef = formedByRef,
            formedAt = formedAt,
            currentness = Current,
            visibility = Private,
            summaryRef = null,
            recurrenceBinding = null,
            rawSourceContentIncluded = false,
            rawSourcesRemainImmutable = true,
            rawSourcesRetrievableByRef = true,
        )
    }

    fun observationFromCore(
        core: Core,
        observationRef: String,
        semanticFingerprint: String,
    ): Observation =
        Observation(
            schemaVersion = core.schemaVersion,
            observationType = core.observationType,
            sourceLineageRef = core.sourceLineageRef,
            sourceBindings = core.sourceBindings,
            sourceSpeakerRefs = core.sourceSpeakerRefs,
            sourceRecipientRefs = core.sourceRecipientRefs,
            projectRef = core.projectRef,
            threadRef = core.threadRef,
            channelRef = core.channelRef,
            turnRef = core.turnRef,
            workNodeRef = core.workNodeRef,
            formedByRef = core.formedByRef,
            formedAt = core.formedAt,
            currentness = core.currentness,
            visibility = core.visibility,
            summaryRef = core.summaryRef,
            recurrenceBinding = core.recurrenceBinding,
            rawSourceContentIncluded = core.rawSourceContentIncluded,
            rawSourcesRemainImmutable = core.rawSourcesRemainImmutable,
            rawSourcesRetrievableByRef = core.rawSourcesRetrievableByRef,
            observationRef = observationRef,
            semanticFingerprint = semanticFingerprint,
        )
}

// [VXG RealForever]
