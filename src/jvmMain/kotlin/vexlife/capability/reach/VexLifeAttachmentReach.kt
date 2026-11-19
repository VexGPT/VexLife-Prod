package vexlife.capability.reach

import java.nio.charset.StandardCharsets
import java.nio.file.Path
import vexlife.attachment.VexLifeAttachmentStore
import vexlife.attachment.VexLifeTurnAttachment

/**
 * Read-only reach into files the person attached to Conversation messages.
 *
 * The attachment store owns which bytes exist; this owner only lists descriptors and returns
 * bounded line windows so a large attachment is traversed, never poured whole into one prompt.
 */
object VexLifeAttachmentReach {
    const val MaximumListEntries = 20
    const val DefaultReadLines = 400
    const val MaximumReadLines = 1_000
    const val MaximumReturnBytes = 48 * 1024

    sealed interface Outcome {
        data class Listed(val attachments: List<VexLifeTurnAttachment.Descriptor>) : Outcome

        data class Lines(
            val descriptor: VexLifeTurnAttachment.Descriptor,
            val text: String,
            val startLine: Int,
            val endLine: Int,
            val nextLine: Int?,
        ) : Outcome

        data class NotFound(val reason: String) : Outcome

        data class Refused(val reason: String) : Outcome
    }

    fun list(productHome: Path): Outcome =
        Outcome.Listed(VexLifeAttachmentStore(productHome).recent(MaximumListEntries))

    fun read(
        productHome: Path,
        ref: String?,
        startLine: Int?,
        maxLines: Int?,
    ): Outcome {
        if (ref == null || !VexLifeTurnAttachment.isRef(ref)) {
            return Outcome.Refused("ref must be an attachment ref from attachments.list or the message trailer.")
        }
        val first = startLine ?: 1
        val count = maxLines ?: DefaultReadLines
        if (first < 1) return Outcome.Refused("startLine must be 1 or more.")
        if (count !in 1..MaximumReadLines) return Outcome.Refused("maxLines must be 1-$MaximumReadLines.")

        val store = VexLifeAttachmentStore(productHome)
        val descriptor =
            store.descriptor(ref) ?: return Outcome.NotFound("No attachment $ref is stored on this computer.")
        val text = store.text(ref) ?: return Outcome.NotFound("No attachment $ref is stored on this computer.")
        if (first > descriptor.lineCount) {
            return Outcome.Refused("startLine $first is past the last line (${descriptor.lineCount}).")
        }

        val lines = text.removeSuffix("\n").split('\n')
        val selected = mutableListOf<String>()
        var usedBytes = 0
        for (line in lines.subList(first - 1, minOf(lines.size, first - 1 + count))) {
            val addition = line.toByteArray(StandardCharsets.UTF_8).size + if (selected.isEmpty()) 0 else 1
            if (usedBytes + addition > MaximumReturnBytes) break
            selected += line
            usedBytes += addition
        }
        if (selected.isEmpty()) {
            return Outcome.Refused("Line $first is larger than one bounded attachment read; no partial line was returned.")
        }
        val endLine = first + selected.size - 1
        return Outcome.Lines(
            descriptor = descriptor,
            text = selected.joinToString("\n"),
            startLine = first,
            endLine = endLine,
            nextLine = (endLine + 1).takeIf { it <= descriptor.lineCount },
        )
    }
}

// [VXG RealForever]
