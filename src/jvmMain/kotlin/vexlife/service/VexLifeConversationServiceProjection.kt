package vexlife.service

import java.nio.file.Path
import vexlife.conversation.DirectConversationIdentity
import vexlife.conversation.DirectConversationStore
import vexlife.conversation.VexLifeConversationContract

/**
 * Read-only Product service projection of canonical Conversation history.
 *
 * Conversation owns exact event words/lineage. Local Artifact RequestBinding owns which resources
 * were sent with a REQUEST. This projector composes those existing truths for the local service
 * without mutating either owner or inspecting artifact bytes/projections during timeline formation.
 */
object VexLifeConversationServiceProjection {
    fun observe(
        productHome: Path,
    ): VexLifeProdServiceContract.ConversationProjection {
        val snapshot =
            DirectConversationStore(
                productHome,
            )
                .current()
        val sentResources =
            VexLifeSentResourceReview(
                productHome,
            )

        return VexLifeProdServiceContract.ConversationProjection(
            schemaVersion =
                VexLifeProdServiceContract.ConversationProjectionSchema,
            projectRef = DirectConversationIdentity.ProjectRef,
            threadRef = DirectConversationIdentity.ThreadRef,
            channelRef = DirectConversationIdentity.ChannelRef,
            currentConversationHeadSha256 =
                snapshot.head
                    ?.conversationHeadSha256,
            messages =
                snapshot.canonicalEvents.map {
                    event ->
                    val role =
                        when (
                            event.eventKind
                        ) {
                            VexLifeConversationContract.RequestKind ->
                                "human"

                            VexLifeConversationContract.ResponseKind ->
                                "vex"

                            else ->
                                error(
                                    "canonical direct Conversation contains unsupported event kind",
                                )
                        }

                    VexLifeProdServiceContract.ConversationMessage(
                        eventRef = event.eventRef,
                        eventSha256 = event.eventSha256,
                        messageRef = event.messageRef,
                        turnRef = event.turnRef,
                        role = role,
                        content = event.content,
                        sequence = event.sequence,
                        formedAt = event.formedAt,
                        model = event.model,
                        resources =
                            if (
                                role ==
                                    "human"
                            ) {
                                sentResources.timelineResources(
                                    event,
                                )
                            } else {
                                emptyList()
                            },
                    )
                },
            abandonedAttemptCount =
                snapshot.abandonedAttempts.size,
            effectsPerformed = false,
        )
    }
}

// [VXG RealForever]
