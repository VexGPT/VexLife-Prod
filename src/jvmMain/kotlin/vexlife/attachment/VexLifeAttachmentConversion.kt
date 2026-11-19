package vexlife.attachment

import java.io.ByteArrayInputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.util.zip.ZipException
import java.util.zip.ZipInputStream
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLStreamConstants
import javax.xml.stream.XMLStreamException
import javax.xml.stream.XMLStreamReader
import vexlife.attachment.VexLifeTurnAttachment.Format
import vexlife.attachment.VexLifeTurnAttachment.Refusal

/**
 * Forms the readable text of one attachment from the person's bytes, once, at staging.
 *
 * Plain UTF-8 text is kept verbatim. A ZIP, DOCX, XLSX or PPTX container is read **in memory
 * only** (nothing is extracted to disk, so member paths never touch the filesystem), with bounded
 * member count and bounded total inflation. XML is parsed without DTDs or external entities.
 *
 * Container text opens with an index naming each section (archive member, sheet, slide) and the
 * line it starts on, so Vex can jump straight to it with `attachments.read(startLine=…)` instead
 * of reading from the top.
 */
object VexLifeAttachmentConversion {
    const val MaximumMembers = 4_096
    const val MaximumInflatedBytes = 64 * 1024 * 1024
    const val MaximumMemberTextBytes = 2 * 1024 * 1024
    const val MaximumListedSkips = 200

    sealed interface Result {
        data class Readable(val format: Format, val text: String) : Result

        data class Refused(val refusal: Refusal, val reason: String) : Result
    }

    private class Unreadable(message: String) : Exception(message)

    fun readable(
        name: String,
        bytes: ByteArray,
    ): Result {
        // Container magic outranks text detection: a ZIP header can itself decode as UTF-8.
        if (!isZip(bytes)) {
            val text = strictUtf8(bytes)?.removePrefix("\uFEFF") ?: return notText(name)
            if ('\u0000' in text) return notText(name)
            if (text.isEmpty()) return Result.Refused(Refusal.EMPTY, "$name is empty.")
            if (text.toByteArray(StandardCharsets.UTF_8).size > VexLifeTurnAttachment.MaximumTextBytes) {
                return tooMuchText(name)
            }
            return Result.Readable(Format.TEXT, text)
        }

        val format =
            when (name.substringAfterLast('.', "").lowercase()) {
                "docx" -> Format.DOCX
                "xlsx" -> Format.XLSX
                "pptx" -> Format.PPTX
                else -> Format.ZIP
            }
        return try {
            val members = inflate(bytes)
            val text =
                when (format) {
                    Format.DOCX -> docx(members)
                    Format.XLSX -> xlsx(members)
                    Format.PPTX -> pptx(members)
                    Format.ZIP -> zip(members)
                    Format.TEXT -> error("text is not a container")
                }
            when {
                text.isBlank() -> Result.Refused(Refusal.UNREADABLE, "$name holds no readable text.")
                text.toByteArray(StandardCharsets.UTF_8).size > VexLifeTurnAttachment.MaximumTextBytes ->
                    tooMuchText(name)
                else -> Result.Readable(format, text)
            }
        } catch (error: Unreadable) {
            Result.Refused(Refusal.UNREADABLE, "$name could not be read: ${error.message}")
        } catch (error: ZipException) {
            Result.Refused(Refusal.UNREADABLE, "$name could not be read as ${format.name}: ${error.message}")
        } catch (error: XMLStreamException) {
            Result.Refused(Refusal.UNREADABLE, "$name has damaged ${format.name} content.")
        }
    }

    private fun notText(name: String) =
        Result.Refused(
            Refusal.NOT_TEXT,
            "$name is not something VexLife can read yet. Plain text, ZIP, DOCX, XLSX and PPTX are supported.",
        )

    private fun tooMuchText(name: String) =
        Result.Refused(
            Refusal.TOO_LARGE,
            "$name holds more than ${VexLifeTurnAttachment.MaximumTextBytes / (1024 * 1024)} MB of text.",
        )

    private fun isZip(bytes: ByteArray): Boolean =
        bytes.size >= 4 && bytes[0] == 'P'.code.toByte() && bytes[1] == 'K'.code.toByte() &&
            bytes[2] == 3.toByte() && bytes[3] == 4.toByte()

    /** Member name -> inflated bytes, in archive order. Directories are omitted. */
    private fun inflate(bytes: ByteArray): LinkedHashMap<String, ByteArray> {
        val members = LinkedHashMap<String, ByteArray>()
        var inflated = 0L
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (entry.isDirectory) continue
                if (members.size >= MaximumMembers) throw Unreadable("more than $MaximumMembers files")
                val content = readBounded(zip, MaximumInflatedBytes - inflated)
                inflated += content.size
                members[entry.name] = content
            }
        }
        if (members.isEmpty()) throw Unreadable("the archive is empty")
        return members
    }

    private fun readBounded(input: InputStream, remaining: Long): ByteArray {
        val buffer = java.io.ByteArrayOutputStream()
        val chunk = ByteArray(64 * 1024)
        var total = 0L
        while (true) {
            val read = input.read(chunk)
            if (read < 0) break
            total += read
            if (total > remaining) {
                throw Unreadable("it expands beyond ${MaximumInflatedBytes / (1024 * 1024)} MB")
            }
            buffer.write(chunk, 0, read)
        }
        return buffer.toByteArray()
    }

    private fun zip(members: Map<String, ByteArray>): String {
        val sections = mutableListOf<Pair<String, String>>()
        val skipped = mutableListOf<String>()
        for ((path, content) in members) {
            val text = strictUtf8(content)?.removePrefix("\uFEFF")
            when {
                text == null || '\u0000' in text -> skipped += "$path (not text)"
                content.size > MaximumMemberTextBytes -> skipped += "$path (over ${MaximumMemberTextBytes / (1024 * 1024)} MB)"
                text.isBlank() -> skipped += "$path (empty)"
                else -> sections += path to text
            }
        }
        if (sections.isEmpty()) throw Unreadable("none of its ${members.size} files is text")
        val notes =
            skipped.take(MaximumListedSkips).map { "Not read: $it" } +
                if (skipped.size > MaximumListedSkips) {
                    listOf("Not read: ${skipped.size - MaximumListedSkips} more")
                } else {
                    emptyList()
                }
        return assemble("ZIP archive · ${members.size} files · ${sections.size} read as text", sections, notes)
    }

    private fun docx(members: Map<String, ByteArray>): String {
        val document = members["word/document.xml"] ?: throw Unreadable("it has no Word document body")
        val text = StringBuilder()
        xml(document) { reader ->
            when (reader.eventType) {
                XMLStreamConstants.START_ELEMENT ->
                    when (reader.localName) {
                        "tab" -> text.append('\t')
                        "br", "cr" -> text.append('\n')
                        "t" -> text.append(reader.elementText)
                    }
                XMLStreamConstants.END_ELEMENT -> if (reader.localName == "p") text.append('\n')
            }
        }
        return assemble("Word document", listOf("Document" to text.toString()), emptyList())
    }

    private fun pptx(members: Map<String, ByteArray>): String {
        val slides =
            members.keys
                .mapNotNull { path ->
                    Regex("""ppt/slides/slide(\d+)\.xml""").matchEntire(path)?.let { it.groupValues[1].toInt() to path }
                }
                .sortedBy { it.first }
        if (slides.isEmpty()) throw Unreadable("it has no slides")
        val sections =
            slides.map { (number, path) ->
                val text = StringBuilder()
                xml(members.getValue(path)) { reader ->
                    when (reader.eventType) {
                        XMLStreamConstants.START_ELEMENT ->
                            when (reader.localName) {
                                "br" -> text.append('\n')
                                "t" -> text.append(reader.elementText)
                            }
                        XMLStreamConstants.END_ELEMENT -> if (reader.localName == "p") text.append('\n')
                    }
                }
                "Slide $number" to text.toString()
            }
        return assemble("PowerPoint presentation · ${slides.size} slides", sections, emptyList())
    }

    private fun xlsx(members: Map<String, ByteArray>): String {
        val shared = mutableListOf<String>()
        members["xl/sharedStrings.xml"]?.let { bytes ->
            var current: StringBuilder? = null
            xml(bytes) { reader ->
                when (reader.eventType) {
                    XMLStreamConstants.START_ELEMENT ->
                        when (reader.localName) {
                            "si" -> current = StringBuilder()
                            "t" -> current?.append(reader.elementText)
                        }
                    XMLStreamConstants.END_ELEMENT -> if (reader.localName == "si") shared += current.toString()
                }
            }
        }
        val sheets = workbookSheets(members)
        if (sheets.isEmpty()) throw Unreadable("it has no worksheets")
        val sections =
            sheets.mapNotNull { (sheetName, path) ->
                members[path]?.let { sheetName to sheetText(it, shared) }
            }
        return assemble("Excel workbook · ${sections.size} sheets", sections, emptyList())
    }

    /** Sheet display name -> part path, in workbook order. */
    private fun workbookSheets(members: Map<String, ByteArray>): List<Pair<String, String>> {
        val targets = mutableMapOf<String, String>()
        members["xl/_rels/workbook.xml.rels"]?.let { bytes ->
            xml(bytes) { reader ->
                if (reader.eventType == XMLStreamConstants.START_ELEMENT && reader.localName == "Relationship") {
                    val id = reader.getAttributeValue(null, "Id")
                    val target = reader.getAttributeValue(null, "Target")
                    if (id != null && target != null) {
                        targets[id] = if (target.startsWith("/")) target.removePrefix("/") else "xl/$target"
                    }
                }
            }
        }
        val named = mutableListOf<Pair<String, String>>()
        members["xl/workbook.xml"]?.let { bytes ->
            xml(bytes) { reader ->
                if (reader.eventType == XMLStreamConstants.START_ELEMENT && reader.localName == "sheet") {
                    val name = reader.getAttributeValue(null, "name") ?: "Sheet"
                    val relation =
                        (0 until reader.attributeCount)
                            .firstOrNull { reader.getAttributeLocalName(it) == "id" }
                            ?.let { reader.getAttributeValue(it) }
                    targets[relation]?.let { named += name to it }
                }
            }
        }
        if (named.isNotEmpty()) return named
        return members.keys
            .mapNotNull { path ->
                Regex("""xl/worksheets/sheet(\d+)\.xml""").matchEntire(path)?.let { it.groupValues[1].toInt() to path }
            }
            .sortedBy { it.first }
            .map { (number, path) -> "Sheet $number" to path }
    }

    private fun sheetText(bytes: ByteArray, shared: List<String>): String {
        val rows = StringBuilder()
        var row = sortedMapOf<Int, String>()
        var cellColumn = 0
        var cellType: String? = null
        var value: String? = null
        xml(bytes) { reader ->
            when (reader.eventType) {
                XMLStreamConstants.START_ELEMENT ->
                    when (reader.localName) {
                        "row" -> row = sortedMapOf()
                        "c" -> {
                            cellColumn = column(reader.getAttributeValue(null, "r")) ?: (row.keys.maxOrNull()?.plus(1) ?: 0)
                            cellType = reader.getAttributeValue(null, "t")
                            value = null
                        }
                        "v" -> value = reader.elementText
                        "t" -> value = (value ?: "") + reader.elementText
                    }
                XMLStreamConstants.END_ELEMENT ->
                    when (reader.localName) {
                        "c" ->
                            value?.let { raw ->
                                row[cellColumn] =
                                    when (cellType) {
                                        "s" -> raw.trim().toIntOrNull()?.let { shared.getOrNull(it) } ?: raw
                                        "b" -> if (raw.trim() == "1") "TRUE" else "FALSE"
                                        else -> raw
                                    }.replace('\t', ' ').replace('\n', ' ')
                            }
                        "row" ->
                            if (row.isNotEmpty()) {
                                val last = row.lastKey()
                                rows.append((0..last).joinToString("\t") { row[it].orEmpty() }).append('\n')
                            }
                    }
            }
        }
        return rows.toString()
    }

    /** Zero-based column of an A1-style reference, or null when absent. */
    private fun column(reference: String?): Int? {
        val letters = reference?.takeWhile { it.isLetter() }?.uppercase()?.takeIf { it.isNotEmpty() } ?: return null
        return letters.fold(0) { total, letter -> total * 26 + (letter - 'A' + 1) } - 1
    }

    /**
     * One readable text: a heading, an index of sections with the line each starts on, notes, then
     * each section under `=== name ===`. Section start lines are exact for `attachments.read`.
     */
    internal fun assemble(
        title: String,
        sections: List<Pair<String, String>>,
        notes: List<String>,
    ): String {
        val bodies = sections.map { (name, text) -> name to text.trimEnd('\n', ' ', '\t') }.filter { it.second.isNotBlank() }
        fun header(starts: List<Int>): String =
            buildString {
                append(title).append('\n')
                if (bodies.isNotEmpty()) {
                    append("Index (section · starts at line · lines):\n")
                    bodies.forEachIndexed { index, (name, text) ->
                        append("- ").append(name).append(" · line ").append(starts[index])
                            .append(" · ").append(VexLifeTurnAttachment.lineCount(text)).append(" lines\n")
                    }
                }
                notes.forEach { append(it).append('\n') }
            }
        val headerLines = VexLifeTurnAttachment.lineCount(header(bodies.map { 0 }))
        val starts = mutableListOf<Int>()
        var line = headerLines + 1
        for ((_, text) in bodies) {
            line += 1 // blank separator
            starts += line + 1 // first content line, after the === name === line
            line += 1 + VexLifeTurnAttachment.lineCount(text)
        }
        return buildString {
            append(header(starts))
            bodies.forEach { (name, text) ->
                append('\n').append("=== ").append(name).append(" ===\n").append(text).append('\n')
            }
        }
    }

    private fun xml(bytes: ByteArray, visit: (XMLStreamReader) -> Unit) {
        val factory =
            XMLInputFactory.newFactory().apply {
                setProperty(XMLInputFactory.SUPPORT_DTD, false)
                setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false)
                setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, true)
            }
        val reader = factory.createXMLStreamReader(ByteArrayInputStream(bytes))
        try {
            while (reader.hasNext()) {
                reader.next()
                visit(reader)
            }
        } finally {
            reader.close()
        }
    }

    internal fun strictUtf8(bytes: ByteArray): String? =
        try {
            StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes))
                .toString()
        } catch (_: CharacterCodingException) {
            null
        }
}

// [VXG RealForever]
