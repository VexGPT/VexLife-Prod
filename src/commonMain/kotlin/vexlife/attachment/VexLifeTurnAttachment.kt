package vexlife.attachment

import kotlinx.serialization.Serializable

/**
 * A text file the person attached to one Conversation message.
 *
 * Meaning owned here:
 * - what an attachment is (one exact, content-addressed text resource the person chose);
 * - which bytes are admitted (bounded UTF-8 text without NUL);
 * - how a message names its attachments (the reference trailer), so the Conversation record keeps
 *   that the person attached it without the event schema learning a new field.
 *
 * Permanent distinctions:
 *
 * ```text
 * ATTACHED != ENTERED THE PROMPT        (Vex reads ranges through attachments.read)
 * STAGED   != SENT                      (only a Send that names the ref attaches it)
 * ATTACHMENT != LOCAL SOURCE GRANT      (no folder, no persistent reach outside these bytes)
 * PASTED IMAGE != VISION SCOPE          (images are not admitted by this owner at all)
 * ```
 */
object VexLifeTurnAttachment {
    const val RefPrefix = "attachment.vexlife-prod.text."
    const val MaximumBytes = 1024 * 1024
    const val MaximumPerMessage = 8
    const val MaximumNameCharacters = 160

    /** Opens the reference trailer appended to the person's message text. */
    const val TrailerHeading = "[Attached files — read with attachments.read]"

    private val RefPattern = Regex(Regex.escape(RefPrefix) + "[0-9a-f]{32}")

    enum class MediaKind {
        TEXT,
    }

    enum class Refusal {
        EMPTY,
        TOO_LARGE,
        NOT_TEXT,
        NAME_INVALID,
    }

    @Serializable
    data class Descriptor(
        val ref: String,
        val name: String,
        val mediaKind: MediaKind,
        val byteCount: Int,
        val lineCount: Int,
        val sha256: String,
    ) {
        init {
            require(isRef(ref)) { "attachment ref is not admitted" }
            require(admitName(name) == name) { "attachment name is not admitted" }
            require(byteCount in 1..MaximumBytes) { "attachment byte count is out of bounds" }
            require(lineCount >= 1) { "attachment line count must be positive" }
            require(sha256.matches(Regex("[0-9a-f]{64}"))) { "attachment sha256 is invalid" }
            require(ref == refFor(sha256)) { "attachment ref must derive from its content digest" }
        }
    }

    fun refFor(sha256: String): String = RefPrefix + sha256.take(32)

    fun isRef(value: String): Boolean = RefPattern.matches(value)

    /**
     * The display name a person's file may carry: the last path segment, without control
     * characters or the separators the trailer uses. Returns null when nothing admissible remains.
     */
    fun admitName(raw: String): String? {
        val lastSegment = raw.substringAfterLast('/').substringAfterLast('\\')
        val cleaned =
            lastSegment
                .filterNot { it.isISOControl() || it == '·' }
                .trim()
                .take(MaximumNameCharacters)
                .trim()
        return cleaned.takeIf { it.isNotEmpty() && it != "." && it != ".." }
    }

    /** Line count as Vex's line-range reads number them: a trailing newline does not open a line. */
    fun lineCount(text: String): Int =
        if (text.isEmpty()) 0 else text.removeSuffix("\n").count { it == '\n' } + 1

    /**
     * The exact message content for one Send: the person's own words, then one trailer line per
     * attachment. Deterministic, so a replayed Send with the same refs forms the same content.
     */
    fun messageContent(
        text: String,
        attachments: List<Descriptor>,
    ): String {
        require(attachments.size <= MaximumPerMessage) { "too many attachments for one message" }
        require(attachments.map { it.ref }.distinct().size == attachments.size) {
            "one message names each attachment once"
        }
        if (attachments.isEmpty()) return text
        val trailer =
            attachments.joinToString(separator = "\n", prefix = "$TrailerHeading\n") {
                "- ${it.name} · ${it.ref} · ${it.lineCount} lines · ${it.byteCount} bytes"
            }
        return if (text.isBlank()) trailer else text.trimEnd() + "\n\n" + trailer
    }

    /** Attachment refs a message names in its trailer, in order. */
    fun refsIn(content: String): List<String> {
        val start = content.lastIndexOf(TrailerHeading)
        if (start < 0) return emptyList()
        return content
            .substring(start + TrailerHeading.length)
            .lineSequence()
            .filter { it.startsWith("- ") }
            .mapNotNull { RefPattern.find(it)?.value }
            .distinct()
            .toList()
    }
}

// [VXG RealForever]
