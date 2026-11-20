package vexlife.artifact

import vexlife.conversation.DirectConversationStore
import vexlife.conversation.VexLifeConversationContract

/**
 * Small host-authored discovery projection for Local Artifacts bound to one exact REQUEST.
 *
 * The projection names what is available without carrying file content. It is prompt metadata, not
 * Conversation content, Memory, capability authority, or proof that any file statement is true.
 */
object VexLifeRequestArtifactManifest {
    const val Header =
        "VEXLIFE_CURRENT_REQUEST_ARTIFACTS/v1"

    const val MaximumManifestCharacters =
        8 * 1024

    data class Projection(
        val requestEventRef: String,
        val requestEventHash: String,
        val attachmentCount: Int,
        val systemContent: String,
        val sourceRefs: List<String>,
    ) {
        init {
            require(
                attachmentCount in
                    1..VexLifeLocalArtifact.MaximumPerRequest &&
                    systemContent.startsWith(
                        Header,
                    ) &&
                    systemContent.length <=
                    MaximumManifestCharacters &&
                    sourceRefs.isNotEmpty(),
            ) {
                "request artifact manifest projection is invalid"
            }
        }
    }

    fun project(
        store: VexLifeLocalArtifactStore,
        request: DirectConversationStore.Event,
    ): Projection? {
        require(
            request.eventKind ==
                VexLifeConversationContract.RequestKind,
        ) {
            "request artifact manifest requires one canonical Conversation REQUEST"
        }

        val binding =
            store.bindingFor(
                request.eventHash,
            )
                ?: return null

        require(
            binding.requestEventRef ==
                request.eventRef &&
                binding.requestEventHash ==
                request.eventHash &&
                binding.requestContentHash ==
                request.contentHash &&
                binding.turnRef ==
                request.turnRef &&
                binding.requestMessageRef ==
                request.messageRef,
        ) {
            "request artifact binding does not belong to the exact current REQUEST"
        }

        val lines =
            binding.attachments.map {
                attachment ->
                val projection =
                    requireNotNull(
                        store.projection(
                            artifactRef =
                                attachment.artifactRef,
                            projectionRef =
                                attachment.projectionRef,
                        ),
                    ) {
                        "request artifact manifest projection is not in exact Product custody"
                    }

                "- " +
                    attachment.displayName +
                    " · " +
                    projection.lineCount +
                    " lines · " +
                    projection.byteCount +
                    " readable bytes · " +
                    projection.converterRef
            }

        val content =
            buildList {
                add(
                    Header,
                )
                add(
                    "requestEventRef=" +
                        request.eventRef,
                )
                add(
                    "count=" +
                        binding.attachments.size,
                )
                add(
                    "effectAuthorityGranted=false",
                )
                add(
                    "The person's exact request has local file resources. Their file contents are not in this prompt.",
                )
                add(
                    "When the attachments capability is available this turn, use attachments.list to get request-local resource refs, then attachments.read for bounded readable text.",
                )
                addAll(
                    lines,
                )
            }
                .joinToString(
                    "\n",
                )

        require(
            content.length <=
                MaximumManifestCharacters,
        ) {
            "request artifact manifest exceeded its bounded discovery projection"
        }

        return Projection(
            requestEventRef =
                request.eventRef,
            requestEventHash =
                request.eventHash,
            attachmentCount =
                binding.attachments.size,
            systemContent =
                content,
            sourceRefs =
                (
                    listOf(
                        request.eventRef,
                    ) +
                        binding.attachments.flatMap {
                            listOf(
                                it.artifactRef,
                                it.projectionRef,
                            )
                        }
                )
                    .distinct(),
        )
    }
}

// [VXG RealForever]
