package vexlife.capability.reach

import java.nio.charset.StandardCharsets
import java.nio.file.Path
import vexlife.artifact.VexLifeLocalArtifact
import vexlife.artifact.VexLifeLocalArtifactStore

/**
 * Read-only reach into the exact artifacts bound to one durable Conversation REQUEST.
 *
 * There is deliberately no global/recent attachment listing. A staged file that the person has not
 * sent has no request binding and therefore has no route into this owner.
 */
object VexLifeAttachmentReach {
    const val DefaultReadLines = 400
    const val MaximumReadLines = 1_000
    const val MaximumReturnBytes = 48 * 1024

    private const val ResourceRefPrefix =
        "request-resource.vexlife-prod."
    private val Sha256 =
        Regex(
            "[0-9a-f]{64}",
        )

    data class Descriptor(
        val resourceRef: String,
        val displayName: String,
        val artifactRef: String,
        val projectionRef: String,
        val byteCount: Long,
        val lineCount: Int,
    )

    sealed interface Outcome {
        data class Listed(
            val requestEventHash: String,
            val resources: List<Descriptor>,
        ) : Outcome

        data class Lines(
            val requestEventHash: String,
            val descriptor: Descriptor,
            val text: String,
            val startLine: Int,
            val endLine: Int,
            val nextLine: Int?,
        ) : Outcome

        data class NotFound(
            val reason: String,
        ) : Outcome

        data class Refused(
            val reason: String,
        ) : Outcome
    }

    fun list(
        productHome: Path,
        requestEventHash: String,
    ): Outcome {
        if (
            !Sha256.matches(
                requestEventHash,
            )
        ) {
            return Outcome.Refused(
                "requestEventHash must be one exact Conversation REQUEST hash.",
            )
        }

        val resources =
            resources(
                productHome = productHome,
                requestEventHash = requestEventHash,
            )
                ?: return Outcome.NotFound(
                    "No local artifacts are bound to that Conversation REQUEST.",
                )

        return Outcome.Listed(
            requestEventHash = requestEventHash,
            resources = resources,
        )
    }

    fun read(
        productHome: Path,
        requestEventHash: String,
        resourceRef: String?,
        startLine: Int?,
        maxLines: Int?,
    ): Outcome {
        if (
            !Sha256.matches(
                requestEventHash,
            )
        ) {
            return Outcome.Refused(
                "requestEventHash must be one exact Conversation REQUEST hash.",
            )
        }

        val ordinal =
            resourceOrdinal(
                requestEventHash = requestEventHash,
                resourceRef = resourceRef,
            )
                ?: return Outcome.Refused(
                    "resourceRef must be one resource returned by attachments.list for this exact request.",
                )

        val first =
            startLine
                ?: 1
        val count =
            maxLines
                ?: DefaultReadLines
        if (
            first <
            1
        ) {
            return Outcome.Refused(
                "startLine must be 1 or more.",
            )
        }
        if (
            count !in
            1..MaximumReadLines
        ) {
            return Outcome.Refused(
                "maxLines must be 1-" +
                    MaximumReadLines +
                    ".",
            )
        }

        val store =
            VexLifeLocalArtifactStore(
                productHome,
            )
        val binding =
            store.bindingFor(
                requestEventHash,
            )
                ?: return Outcome.NotFound(
                    "No local artifacts are bound to that Conversation REQUEST.",
                )
        val attachment =
            binding.attachments
                .getOrNull(
                    ordinal - 1,
                )
                ?: return Outcome.NotFound(
                    "That request resource does not exist.",
                )
        val expectedResourceRef =
            resourceRefFor(
                requestEventHash = requestEventHash,
                ordinal = ordinal,
            )
        if (
            resourceRef !=
            expectedResourceRef
        ) {
            return Outcome.Refused(
                "resourceRef does not belong to this exact Conversation REQUEST.",
            )
        }

        val projection =
            store.projection(
                artifactRef = attachment.artifactRef,
                projectionRef = attachment.projectionRef,
            )
                ?: return Outcome.NotFound(
                    "The request resource projection is no longer in exact Product custody.",
                )
        if (
            first >
            projection.lineCount
        ) {
            return Outcome.Refused(
                "startLine " +
                    first +
                    " is past the last line (" +
                    projection.lineCount +
                    ").",
            )
        }

        val lines =
            projection.text
                .removeSuffix(
                    "\n",
                )
                .split(
                    '\n',
                )
        val selected =
            mutableListOf<String>()
        var usedBytes =
            0
        val lastExclusive =
            minOf(
                lines.size,
                first - 1 + count,
            )
        for (
            line in
            lines.subList(
                first - 1,
                lastExclusive,
            )
        ) {
            val addition =
                line.toByteArray(
                    StandardCharsets.UTF_8,
                ).size +
                    if (
                        selected.isEmpty()
                    ) {
                        0
                    } else {
                        1
                    }
            if (
                usedBytes +
                    addition >
                MaximumReturnBytes
            ) {
                break
            }
            selected +=
                line
            usedBytes +=
                addition
        }
        if (
            selected.isEmpty()
        ) {
            return Outcome.Refused(
                "Line " +
                    first +
                    " is larger than one bounded attachment read; no partial line was returned.",
            )
        }

        val endLine =
            first +
                selected.size -
                1
        return Outcome.Lines(
            requestEventHash = requestEventHash,
            descriptor =
                descriptor(
                    store = store,
                    requestEventHash = requestEventHash,
                    ordinal = ordinal,
                    attachment = attachment,
                ),
            text =
                selected.joinToString(
                    "\n",
                ),
            startLine = first,
            endLine = endLine,
            nextLine =
                (
                    endLine +
                        1
                ).takeIf {
                    it <=
                        projection.lineCount
                },
        )
    }

    private fun resources(
        productHome: Path,
        requestEventHash: String,
    ): List<Descriptor>? {
        val store =
            VexLifeLocalArtifactStore(
                productHome,
            )
        val binding =
            store.bindingFor(
                requestEventHash,
            )
                ?: return null

        return binding.attachments.mapIndexed {
            index,
            attachment,
            ->
            descriptor(
                store = store,
                requestEventHash = requestEventHash,
                ordinal = index + 1,
                attachment = attachment,
            )
        }
    }

    private fun descriptor(
        store: VexLifeLocalArtifactStore,
        requestEventHash: String,
        ordinal: Int,
        attachment: VexLifeLocalArtifact.RequestAttachment,
    ): Descriptor {
        val projection =
            requireNotNull(
                store.projection(
                    artifactRef = attachment.artifactRef,
                    projectionRef = attachment.projectionRef,
                ),
            ) {
                "request resource projection is not in exact Product custody"
            }

        return Descriptor(
            resourceRef =
                resourceRefFor(
                    requestEventHash = requestEventHash,
                    ordinal = ordinal,
                ),
            displayName = attachment.displayName,
            artifactRef = attachment.artifactRef,
            projectionRef = attachment.projectionRef,
            byteCount = projection.byteCount,
            lineCount = projection.lineCount,
        )
    }

    private fun resourceRefFor(
        requestEventHash: String,
        ordinal: Int,
    ): String {
        require(
            Sha256.matches(
                requestEventHash,
            ) &&
                ordinal in
                1..VexLifeLocalArtifact.MaximumPerRequest,
        ) {
            "request resource identity is invalid"
        }
        return ResourceRefPrefix +
            requestEventHash +
            "." +
            ordinal
    }

    private fun resourceOrdinal(
        requestEventHash: String,
        resourceRef: String?,
    ): Int? {
        if (
            resourceRef ==
            null
        ) {
            return null
        }
        val prefix =
            ResourceRefPrefix +
                requestEventHash +
                "."
        if (
            !resourceRef.startsWith(
                prefix,
            )
        ) {
            return null
        }
        val ordinal =
            resourceRef
                .removePrefix(
                    prefix,
                )
                .toIntOrNull()
                ?: return null
        return ordinal.takeIf {
            it in
                1..VexLifeLocalArtifact.MaximumPerRequest
        }
    }
}

// [VXG RealForever]
