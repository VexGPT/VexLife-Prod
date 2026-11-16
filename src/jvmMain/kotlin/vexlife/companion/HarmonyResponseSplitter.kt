package vexlife.companion

/**
 * Splits an OpenAI harmony response stream into thinking and answer.
 *
 * Why it exists: gpt-oss writes its reply as harmony messages —
 * `<|channel|>analysis<|message|>…<|end|><|start|>assistant<|channel|>final<|message|>…` — and the
 * Product-pinned mlx-lm server returns that text inline in `content` instead of separating it the
 * way it separates Qwen's `<think>`. Without this, the person would read the markup and the
 * analysis as Vex's answer, and a capability request in the final channel would not be found.
 *
 * Owns: recognizing harmony markers across arbitrary chunk boundaries and classifying each piece of
 * text by channel. `final` (and text outside any channel) is the answer; `analysis` and
 * `commentary` are thinking. A message addressed to a tool (`to=functions.web_fetch`) is a
 * [Kind.ToolCall] carrying that recipient: gpt-oss's own way of asking for reach, which the
 * Connection hands to the bridge as a capability request ([HarmonyCapabilityTools]). Markers
 * themselves are never emitted. A stream that does not open with a marker is passed through as
 * answer, so a server that already separates harmony keeps working.
 */
class HarmonyResponseSplitter {
    enum class Kind {
        Thinking,
        Answer,
        /** The arguments of a call to [Piece.recipient], e.g. `functions.web_fetch`. */
        ToolCall,
    }

    data class Piece(val kind: Kind, val text: String, val recipient: String? = null)

    private enum class Mode {
        Undetermined,
        PassThrough,
        Header,
        Message,
        Done,
    }

    private val pending = StringBuilder()
    private var mode = Mode.Undetermined
    private var channel: Kind = Kind.Answer
    private var recipient: String? = null

    fun accept(delta: String): List<Piece> {
        if (delta.isEmpty()) return emptyList()
        pending.append(delta)
        return drain(final = false)
    }

    /** The stream ended: anything still held is classified now. */
    fun finish(): List<Piece> = drain(final = true)

    private fun drain(final: Boolean): List<Piece> {
        val out = mutableListOf<Piece>()
        fun emit(kind: Kind, text: String) {
            if (text.isEmpty()) return
            val to = recipient.takeIf { kind == Kind.ToolCall }
            val last = out.lastOrNull()
            if (last != null && last.kind == kind && last.recipient == to) {
                out[out.size - 1] = last.copy(text = last.text + text)
            } else out += Piece(kind, text, to)
        }
        while (true) {
            when (mode) {
                Mode.Undetermined -> {
                    val leading = pending.trimStart()
                    when {
                        leading.isEmpty() -> {
                            if (final) pending.setLength(0)
                            return out
                        }
                        // The generation prompt ends `<|start|>assistant`, so a first message
                        // addressed to a tool opens with its recipient instead of a marker.
                        leading.startsWith("<|") || leading.startsWith(RecipientPrefix) ->
                            mode = Mode.Header
                        ("<|".startsWith(leading) || RecipientPrefix.startsWith(leading)) && !final ->
                            return out
                        else -> mode = Mode.PassThrough
                    }
                }
                Mode.PassThrough -> {
                    emit(Kind.Answer, pending.toString())
                    pending.setLength(0)
                    return out
                }
                Mode.Done -> {
                    pending.setLength(0)
                    return out
                }
                Mode.Header -> {
                    val text = pending.toString()
                    val open = text.indexOf(Message)
                    if (open < 0) {
                        if (final) {
                            pending.setLength(0)
                            return out
                        }
                        // A header is a few words; text this long without a message marker is
                        // not harmony after all.
                        if (text.length > MaximumHeaderCharacters) {
                            mode = Mode.PassThrough
                            continue
                        }
                        return out
                    }
                    val header = text.substring(0, open)
                    // `to=assistant` heads a tool's own output, never a call Vex makes.
                    recipient =
                        Recipient.find(header)?.groupValues?.get(1)?.takeUnless {
                            it == "assistant" || it == "user"
                        }
                    channel =
                        when {
                            recipient != null -> Kind.ToolCall
                            else ->
                                when (ChannelName.find(header)?.groupValues?.get(1)?.lowercase()) {
                                    "analysis",
                                    "commentary" -> Kind.Thinking
                                    else -> Kind.Answer
                                }
                        }
                    pending.delete(0, open + Message.length)
                    mode = Mode.Message
                }
                Mode.Message -> {
                    val text = pending.toString()
                    val marker = text.indexOf("<|")
                    if (marker < 0) {
                        // A trailing `<` may be the start of the next marker.
                        val hold = if (!final && text.endsWith("<")) 1 else 0
                        emit(channel, text.substring(0, text.length - hold))
                        pending.delete(0, text.length - hold)
                        return out
                    }
                    emit(channel, text.substring(0, marker))
                    pending.delete(0, marker)
                    val rest = pending.toString()
                    val ending = Endings.firstOrNull { rest.startsWith(it) }
                    val opening = Openings.firstOrNull { rest.startsWith(it) }
                    when {
                        ending != null -> {
                            pending.delete(0, ending.length)
                            mode = if (ending == End) Mode.Header else Mode.Done
                        }
                        opening != null -> mode = Mode.Header
                        !final && Markers.any { it.startsWith(rest) } -> return out
                        else -> {
                            // `<|` that opens no marker is ordinary text.
                            emit(channel, "<|")
                            pending.delete(0, 2)
                        }
                    }
                }
            }
        }
    }

    private companion object {
        const val Message = "<|message|>"
        const val End = "<|end|>"
        val Endings = listOf(End, "<|return|>", "<|call|>")
        val Openings = listOf("<|start|>", "<|channel|>")
        val Markers = Endings + Openings + listOf(Message, "<|constrain|>")
        val ChannelName = Regex("""<\|channel\|>\s*([A-Za-z_]+)""")
        const val RecipientPrefix = "to="
        val Recipient = Regex("""(?:^|[\s>])to=([A-Za-z0-9_.\-]+)""")
        const val MaximumHeaderCharacters = 256
    }
}

// [VXG RealForever]
