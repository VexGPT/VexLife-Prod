package vexlife.artifact

import vexlife.text.VexLifeText
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

/**
 * Forms one bounded readable projection from exact Local Artifact bytes.
 *
 * Plain UTF-8 text remains verbatim (apart from an optional BOM). ZIP / DOCX / XLSX / PPTX are
 * read in memory only; archive member paths never touch the filesystem. OOXML type is discovered
 * from canonical members rather than trusted from the display filename.
 */
object VexLifeLocalArtifactConversion {
    const val MaximumMembers =
        4_096
    const val MaximumInflatedBytes =
        64 * 1024 * 1024
    const val MaximumZipMemberTextBytes =
        2 * 1024 * 1024
    const val MaximumListedSkips =
        200
    const val MaximumSectionNameCharacters =
        240

    enum class Format(
        val converterRef: String,
    ) {
        TEXT(
            "converter.vexlife-prod.plain-text.v1",
        ),
        ZIP(
            "converter.vexlife-prod.zip-text.v1",
        ),
        DOCX(
            "converter.vexlife-prod.docx-text.v1",
        ),
        XLSX(
            "converter.vexlife-prod.xlsx-text.v1",
        ),
        PPTX(
            "converter.vexlife-prod.pptx-text.v1",
        ),
    }

    enum class Refusal {
        EMPTY,
        TOO_LARGE,
        NOT_TEXT,
        UNREADABLE,
    }

    sealed interface Result {
        data class Readable(
            val format: Format,
            val text: String,
        ) : Result {
            val converterRef: String
                get() =
                    format.converterRef
        }

        data class Held(
            val refusal: Refusal,
            val reason: VexLifeText,
        ) : Result
    }

    private class Unreadable(
        message: String,
    ) : Exception(
            message,
        )

    private class ProjectionTooLarge :
        Exception(
            "readable projection exceeds " +
                (
                    VexLifeLocalArtifact.MaximumProjectionBytes /
                        (1024 * 1024)
                ) +
                " MB",
        )

    /**
     * UTF-8 byte-counted builder so expansion is held before an enormous Kotlin String is formed.
     */
    private class BoundedText {
        private val value =
            StringBuilder()
        private var bytes =
            0

        fun append(
            text: String,
        ): BoundedText {
            val additional =
                text.toByteArray(
                    StandardCharsets.UTF_8,
                ).size
            if (
                bytes +
                    additional >
                VexLifeLocalArtifact.MaximumProjectionBytes
            ) {
                throw ProjectionTooLarge()
            }
            value.append(
                text,
            )
            bytes +=
                additional
            return this
        }

        fun append(
            character: Char,
        ): BoundedText =
            append(
                character.toString(),
            )

        override fun toString(): String =
            value.toString()
    }

    fun readable(
        displayName: String,
        bytes: ByteArray,
    ): Result {
        if (
            bytes.isEmpty()
        ) {
            return Result.Held(
                refusal = Refusal.EMPTY,
                reason =
                        VexLifeText.text(
                            "workspace.status.attachment-empty",
                            "name" to displayName,
                        ),
            )
        }
        if (
            bytes.size >
            VexLifeLocalArtifact.MaximumOriginalBytes
        ) {
            return Result.Held(
                refusal = Refusal.TOO_LARGE,
                reason =
                    VexLifeText.text(
                        "workspace.status.attachment-too-large",
                        "name" to displayName,
                        "maximum" to
                            (
                                VexLifeLocalArtifact.MaximumOriginalBytes /
                                    (1024 * 1024)
                            ).toString(),
                    ),
            )
        }

        // Container magic outranks text detection: a ZIP header can itself decode as UTF-8.
        if (
            !isZip(
                bytes,
            )
        ) {
            val text =
                strictUtf8(
                    bytes,
                )
                    ?.removePrefix(
                        "\uFEFF",
                    )
                    ?: return notText(
                        displayName,
                    )
            if (
                '\u0000' in
                text
            ) {
                return notText(
                    displayName,
                )
            }
            if (
                text.isEmpty()
            ) {
                return Result.Held(
                    refusal = Refusal.EMPTY,
                    reason =
                        VexLifeText.text(
                            "workspace.status.attachment-empty",
                            "name" to displayName,
                        ),
                )
            }
            if (
                text.toByteArray(
                    StandardCharsets.UTF_8,
                ).size >
                VexLifeLocalArtifact.MaximumProjectionBytes
            ) {
                return tooMuchText(
                    displayName,
                )
            }
            return Result.Readable(
                format = Format.TEXT,
                text = text,
            )
        }

        return try {
            val members =
                inflate(
                    bytes,
                )
            val format =
                formatOf(
                    members,
                )
            val text =
                when (
                    format
                ) {
                    Format.DOCX ->
                        docx(
                            members,
                        )

                    Format.XLSX ->
                        xlsx(
                            members,
                        )

                    Format.PPTX ->
                        pptx(
                            members,
                        )

                    Format.ZIP ->
                        zip(
                            members,
                        )

                    Format.TEXT ->
                        error(
                            "text is not a container",
                        )
                }

            if (
                text.isBlank()
            ) {
                Result.Held(
                    refusal = Refusal.UNREADABLE,
                    reason =
                        VexLifeText.text(
                            "workspace.status.attachment-no-readable-text",
                            "name" to displayName,
                        ),
                )
            } else {
                Result.Readable(
                    format = format,
                    text = text,
                )
            }
        } catch (
            _: ProjectionTooLarge,
        ) {
            tooMuchText(
                displayName,
            )
        } catch (
            error: Unreadable,
        ) {
            Result.Held(
                refusal = Refusal.UNREADABLE,
                reason =
                    VexLifeText.text(
                        "workspace.status.attachment-read-failed",
                        "name" to displayName,
                    ),
            )
        } catch (
            error: ZipException,
        ) {
            Result.Held(
                refusal = Refusal.UNREADABLE,
                reason =
                    VexLifeText.text(
                        "workspace.status.attachment-zip-read-failed",
                        "name" to displayName,
                    ),
            )
        } catch (
            _: XMLStreamException,
        ) {
            Result.Held(
                refusal = Refusal.UNREADABLE,
                reason =
                    VexLifeText.text(
                        "workspace.status.attachment-office-xml-damaged",
                        "name" to displayName,
                    ),
            )
        }
    }

    private fun formatOf(
        members: Map<String, ByteArray>,
    ): Format =
        when {
            "word/document.xml" in
                members ->
                Format.DOCX

            "xl/workbook.xml" in
                members ||
                members.keys.any {
                    it.startsWith(
                        "xl/worksheets/",
                    )
                } ->
                Format.XLSX

            members.keys.any {
                SlidePath.matches(
                    it,
                )
            } ->
                Format.PPTX

            else ->
                Format.ZIP
        }

    private fun notText(
        displayName: String,
    ): Result.Held =
        Result.Held(
            refusal = Refusal.NOT_TEXT,
            reason =
                VexLifeText.text(
                    "workspace.status.attachment-not-readable-yet",
                    "name" to displayName,
                ),
        )

    private fun tooMuchText(
        displayName: String,
    ): Result.Held =
        Result.Held(
            refusal = Refusal.TOO_LARGE,
            reason =
                VexLifeText.text(
                    "workspace.status.attachment-readable-text-too-large",
                    "name" to displayName,
                    "maximum" to
                        (
                            VexLifeLocalArtifact.MaximumProjectionBytes /
                                (1024 * 1024)
                        ).toString(),
                ),
        )

    private fun isZip(
        bytes: ByteArray,
    ): Boolean =
        bytes.size >=
            4 &&
            bytes[
                0
            ] ==
            'P'.code.toByte() &&
            bytes[
                1
            ] ==
            'K'.code.toByte() &&
            bytes[
                2
            ] ==
            3.toByte() &&
            bytes[
                3
            ] ==
            4.toByte()

    /** Member name -> inflated bytes, in archive order. Directories are omitted. */
    private fun inflate(
        bytes: ByteArray,
    ): LinkedHashMap<String, ByteArray> {
        val members =
            LinkedHashMap<String, ByteArray>()
        var inflated =
            0L

        ZipInputStream(
            ByteArrayInputStream(
                bytes,
            ),
        )
            .use {
                zip ->
                while (
                    true
                ) {
                    val entry =
                        zip.nextEntry
                            ?: break
                    if (
                        entry.isDirectory
                    ) {
                        continue
                    }
                    if (
                        members.size >=
                        MaximumMembers
                    ) {
                        throw Unreadable(
                            "more than " +
                                MaximumMembers +
                                " files",
                        )
                    }
                    if (
                        entry.name in
                        members
                    ) {
                        throw Unreadable(
                            "duplicate archive member " +
                                displaySectionName(
                                    entry.name,
                                ),
                        )
                    }

                    val content =
                        readBounded(
                            input = zip,
                            remaining =
                                MaximumInflatedBytes -
                                    inflated,
                        )
                    inflated +=
                        content.size
                    members[
                        entry.name
                    ] =
                        content
                }
            }

        if (
            members.isEmpty()
        ) {
            throw Unreadable(
                "the archive is empty",
            )
        }
        return members
    }

    private fun readBounded(
        input: InputStream,
        remaining: Long,
    ): ByteArray {
        val output =
            java.io.ByteArrayOutputStream()
        val chunk =
            ByteArray(
                64 * 1024,
            )
        var total =
            0L

        while (
            true
        ) {
            val read =
                input.read(
                    chunk,
                )
            if (
                read <
                0
            ) {
                break
            }
            total +=
                read
            if (
                total >
                remaining
            ) {
                throw Unreadable(
                    "it expands beyond " +
                        (
                            MaximumInflatedBytes /
                                (1024 * 1024)
                        ) +
                        " MB",
                )
            }
            output.write(
                chunk,
                0,
                read,
            )
        }
        return output.toByteArray()
    }

    private fun zip(
        members: Map<String, ByteArray>,
    ): String {
        val sections =
            mutableListOf<Pair<String, String>>()
        val skipped =
            mutableListOf<String>()

        for (
            (path, content) in
            members
        ) {
            val name =
                displaySectionName(
                    path,
                )
            val text =
                strictUtf8(
                    content,
                )
                    ?.removePrefix(
                        "\uFEFF",
                    )
            when {
                text ==
                    null ||
                    '\u0000' in
                    text ->
                    skipped +=
                        name +
                            " (not text)"

                content.size >
                    MaximumZipMemberTextBytes ->
                    skipped +=
                        name +
                            " (over " +
                            (
                                MaximumZipMemberTextBytes /
                                    (1024 * 1024)
                            ) +
                            " MB)"

                text.isBlank() ->
                    skipped +=
                        name +
                            " (empty)"

                else ->
                    sections +=
                        name to
                            text
            }
        }

        if (
            sections.isEmpty()
        ) {
            throw Unreadable(
                "none of its " +
                    members.size +
                    " files is text",
            )
        }

        val notes =
            skipped
                .take(
                    MaximumListedSkips,
                )
                .map {
                    "Not read: " +
                        it
                } +
                if (
                    skipped.size >
                    MaximumListedSkips
                ) {
                    listOf(
                        "Not read: " +
                            (
                                skipped.size -
                                    MaximumListedSkips
                            ) +
                            " more",
                    )
                } else {
                    emptyList()
                }

        return assemble(
            title =
                "ZIP archive · " +
                    members.size +
                    " files · " +
                    sections.size +
                    " read as text",
            sections = sections,
            notes = notes,
        )
    }

    private fun docx(
        members: Map<String, ByteArray>,
    ): String {
        val document =
            members[
                "word/document.xml"
            ]
                ?: throw Unreadable(
                    "it has no Word document body",
                )
        val text =
            BoundedText()

        xml(
            document,
        ) {
            reader ->
            when (
                reader.eventType
            ) {
                XMLStreamConstants.START_ELEMENT ->
                    when (
                        reader.localName
                    ) {
                        "tab" ->
                            text.append(
                                '\t',
                            )

                        "br",
                        "cr",
                        ->
                            text.append(
                                '\n',
                            )

                        "t" ->
                            text.append(
                                reader.elementText,
                            )
                    }

                XMLStreamConstants.END_ELEMENT ->
                    if (
                        reader.localName ==
                        "p"
                    ) {
                        text.append(
                            '\n',
                        )
                    }
            }
        }

        return assemble(
            title = "Word document",
            sections =
                listOf(
                    "Document" to
                        text.toString(),
                ),
            notes = emptyList(),
        )
    }

    private fun pptx(
        members: Map<String, ByteArray>,
    ): String {
        val slides =
            members.keys
                .mapNotNull {
                    path ->
                    SlidePath
                        .matchEntire(
                            path,
                        )
                        ?.let {
                            match ->
                            match.groupValues[
                                1
                            ].toInt() to
                                path
                        }
                }
                .sortedBy {
                    it.first
                }

        if (
            slides.isEmpty()
        ) {
            throw Unreadable(
                "it has no slides",
            )
        }

        val sections =
            slides.map {
                (number, path) ->
                val text =
                    BoundedText()
                xml(
                    members.getValue(
                        path,
                    ),
                ) {
                    reader ->
                    when (
                        reader.eventType
                    ) {
                        XMLStreamConstants.START_ELEMENT ->
                            when (
                                reader.localName
                            ) {
                                "br" ->
                                    text.append(
                                        '\n',
                                    )

                                "t" ->
                                    text.append(
                                        reader.elementText,
                                    )
                            }

                        XMLStreamConstants.END_ELEMENT ->
                            if (
                                reader.localName ==
                                "p"
                            ) {
                                text.append(
                                    '\n',
                                )
                            }
                    }
                }
                "Slide " +
                    number to
                    text.toString()
            }

        return assemble(
            title =
                "PowerPoint presentation · " +
                    slides.size +
                    " slides",
            sections = sections,
            notes = emptyList(),
        )
    }

    private fun xlsx(
        members: Map<String, ByteArray>,
    ): String {
        val shared =
            mutableListOf<String>()

        members[
            "xl/sharedStrings.xml"
        ]
            ?.let {
                bytes ->
                var current: StringBuilder? =
                    null
                xml(
                    bytes,
                ) {
                    reader ->
                    when (
                        reader.eventType
                    ) {
                        XMLStreamConstants.START_ELEMENT ->
                            when (
                                reader.localName
                            ) {
                                "si" ->
                                    current =
                                        StringBuilder()

                                "t" ->
                                    current
                                        ?.append(
                                            reader.elementText,
                                        )
                            }

                        XMLStreamConstants.END_ELEMENT ->
                            if (
                                reader.localName ==
                                "si"
                            ) {
                                shared +=
                                    current
                                        ?.toString()
                                        .orEmpty()
                            }
                    }
                }
            }

        val sheets =
            workbookSheets(
                members,
            )
        if (
            sheets.isEmpty()
        ) {
            throw Unreadable(
                "it has no worksheets",
            )
        }

        val sections =
            sheets.mapNotNull {
                (sheetName, path) ->
                members[
                    path
                ]
                    ?.let {
                        sheetName to
                            sheetText(
                                bytes = it,
                                shared = shared,
                            )
                    }
            }

        return assemble(
            title =
                "Excel workbook · " +
                    sections.size +
                    " sheets",
            sections = sections,
            notes = emptyList(),
        )
    }

    /** Sheet display name -> part path, in workbook order. */
    private fun workbookSheets(
        members: Map<String, ByteArray>,
    ): List<Pair<String, String>> {
        val targets =
            mutableMapOf<String, String>()

        members[
            "xl/_rels/workbook.xml.rels"
        ]
            ?.let {
                bytes ->
                xml(
                    bytes,
                ) {
                    reader ->
                    if (
                        reader.eventType ==
                            XMLStreamConstants.START_ELEMENT &&
                        reader.localName ==
                            "Relationship"
                    ) {
                        val id =
                            reader.getAttributeValue(
                                null,
                                "Id",
                            )
                        val target =
                            reader.getAttributeValue(
                                null,
                                "Target",
                            )
                        if (
                            id !=
                                null &&
                            target !=
                                null &&
                            !target.contains(
                                "://",
                            )
                        ) {
                            targets[
                                id
                            ] =
                                officePartPath(
                                    base = "xl",
                                    target = target,
                                )
                        }
                    }
                }
            }

        val named =
            mutableListOf<Pair<String, String>>()
        members[
            "xl/workbook.xml"
        ]
            ?.let {
                bytes ->
                xml(
                    bytes,
                ) {
                    reader ->
                    if (
                        reader.eventType ==
                            XMLStreamConstants.START_ELEMENT &&
                        reader.localName ==
                            "sheet"
                    ) {
                        val name =
                            displaySectionName(
                                reader.getAttributeValue(
                                    null,
                                    "name",
                                )
                                    ?: "Sheet",
                            )
                        val relation =
                            (
                                0 until
                                    reader.attributeCount
                            )
                                .firstOrNull {
                                    reader.getAttributeLocalName(
                                        it,
                                    ) ==
                                        "id"
                                }
                                ?.let {
                                    reader.getAttributeValue(
                                        it,
                                    )
                                }
                        targets[
                            relation
                        ]
                            ?.let {
                                named +=
                                    name to
                                        it
                            }
                    }
                }
            }

        if (
            named.isNotEmpty()
        ) {
            return named
        }

        return members.keys
            .mapNotNull {
                path ->
                SheetPath
                    .matchEntire(
                        path,
                    )
                    ?.let {
                        match ->
                        match.groupValues[
                            1
                        ].toInt() to
                            path
                    }
            }
            .sortedBy {
                it.first
            }
            .map {
                (number, path) ->
                "Sheet " +
                    number to
                    path
            }
    }

    private fun officePartPath(
        base: String,
        target: String,
    ): String {
        val parts =
            mutableListOf<String>()
        val combined =
            if (
                target.startsWith(
                    "/",
                )
            ) {
                target.removePrefix(
                    "/",
                )
            } else {
                base +
                    "/" +
                    target
            }
        for (
            part in
            combined.split(
                '/',
            )
        ) {
            when (
                part
            ) {
                "",
                ".",
                ->
                    Unit

                ".." ->
                    if (
                        parts.isNotEmpty()
                    ) {
                        parts.removeAt(
                            parts.lastIndex,
                        )
                    }

                else ->
                    parts +=
                        part
            }
        }
        return parts.joinToString(
            "/",
        )
    }

    private fun sheetText(
        bytes: ByteArray,
        shared: List<String>,
    ): String {
        val rows =
            BoundedText()
        var row =
            sortedMapOf<Int, String>()
        var cellColumn =
            0
        var cellType: String? =
            null
        var value: String? =
            null

        xml(
            bytes,
        ) {
            reader ->
            when (
                reader.eventType
            ) {
                XMLStreamConstants.START_ELEMENT ->
                    when (
                        reader.localName
                    ) {
                        "row" ->
                            row =
                                sortedMapOf()

                        "c" -> {
                            cellColumn =
                                column(
                                    reader.getAttributeValue(
                                        null,
                                        "r",
                                    ),
                                )
                                    ?: (
                                        row.keys
                                            .maxOrNull()
                                            ?.plus(
                                                1,
                                            )
                                            ?: 0
                                    )
                            cellType =
                                reader.getAttributeValue(
                                    null,
                                    "t",
                                )
                            value =
                                null
                        }

                        "v" ->
                            value =
                                reader.elementText

                        "t" ->
                            value =
                                (
                                    value
                                        ?: ""
                                ) +
                                reader.elementText
                    }

                XMLStreamConstants.END_ELEMENT ->
                    when (
                        reader.localName
                    ) {
                        "c" ->
                            value
                                ?.let {
                                    raw ->
                                    row[
                                        cellColumn
                                    ] =
                                        when (
                                            cellType
                                        ) {
                                            "s" ->
                                                raw
                                                    .trim()
                                                    .toIntOrNull()
                                                    ?.let {
                                                        shared.getOrNull(
                                                            it,
                                                        )
                                                    }
                                                    ?: raw

                                            "b" ->
                                                if (
                                                    raw.trim() ==
                                                    "1"
                                                ) {
                                                    "TRUE"
                                                } else {
                                                    "FALSE"
                                                }

                                            else ->
                                                raw
                                        }
                                            .replace(
                                                '\t',
                                                ' ',
                                            )
                                            .replace(
                                                '\n',
                                                ' ',
                                            )
                                }

                        "row" ->
                            if (
                                row.isNotEmpty()
                            ) {
                                val last =
                                    row.lastKey()
                                rows
                                    .append(
                                        (
                                            0..last
                                        )
                                            .joinToString(
                                                "\t",
                                            ) {
                                                row[
                                                    it
                                                ]
                                                    .orEmpty()
                                            },
                                    )
                                    .append(
                                        '\n',
                                    )
                            }
                    }
            }
        }

        return rows.toString()
    }

    /** Zero-based column of an A1-style reference, or null when absent. */
    private fun column(
        reference: String?,
    ): Int? {
        val letters =
            reference
                ?.takeWhile {
                    it.isLetter()
                }
                ?.uppercase()
                ?.takeIf {
                    it.isNotEmpty()
                }
                ?: return null

        return letters.fold(
            0,
        ) {
            total,
            letter,
            ->
            total *
                26 +
                (
                    letter -
                        'A' +
                        1
                )
        } -
            1
    }

    /**
     * One readable text: a heading, an index of sections with exact one-based content start lines,
     * notes, then each section under `=== name ===`.
     */
    internal fun assemble(
        title: String,
        sections: List<Pair<String, String>>,
        notes: List<String>,
    ): String {
        val bodies =
            sections
                .map {
                    (name, text) ->
                    displaySectionName(
                        name,
                    ) to
                        text.trimEnd(
                            '\n',
                            ' ',
                            '\t',
                        )
                }
                .filter {
                    it.second.isNotBlank()
                }

        fun header(
            starts: List<Int>,
        ): String {
            val output =
                BoundedText()
            output
                .append(
                    title,
                )
                .append(
                    '\n',
                )
            if (
                bodies.isNotEmpty()
            ) {
                output.append(
                    "Index (section · starts at line · lines):\n",
                )
                bodies.forEachIndexed {
                    index,
                    (name, text),
                    ->
                    output
                        .append(
                            "- ",
                        )
                        .append(
                            name,
                        )
                        .append(
                            " · line ",
                        )
                        .append(
                            starts[
                                index
                            ].toString(),
                        )
                        .append(
                            " · ",
                        )
                        .append(
                            lineCount(
                                text,
                            ).toString(),
                        )
                        .append(
                            " lines\n",
                        )
                }
            }
            notes.forEach {
                output
                    .append(
                        it,
                    )
                    .append(
                        '\n',
                    )
            }
            return output.toString()
        }

        val headerLines =
            lineCount(
                header(
                    bodies.map {
                        0
                    },
                ),
            )
        val starts =
            mutableListOf<Int>()
        var line =
            headerLines +
                1
        for (
            (_, text) in
            bodies
        ) {
            line +=
                1
            starts +=
                line +
                    1
            line +=
                1 +
                    lineCount(
                        text,
                    )
        }

        val output =
            BoundedText()
        output.append(
            header(
                starts,
            ),
        )
        bodies.forEach {
            (name, text) ->
            output
                .append(
                    '\n',
                )
                .append(
                    "=== ",
                )
                .append(
                    name,
                )
                .append(
                    " ===\n",
                )
                .append(
                    text,
                )
                .append(
                    '\n',
                )
        }
        return output.toString()
    }

    private fun displaySectionName(
        raw: String,
    ): String {
        val cleaned =
            raw
                .filterNot {
                    it.isISOControl()
                }
                .trim()
                .ifEmpty {
                    "(unnamed)"
                }
        return if (
            cleaned.length <=
            MaximumSectionNameCharacters
        ) {
            cleaned
        } else {
            cleaned.take(
                MaximumSectionNameCharacters -
                    1,
            ) +
                "…"
        }
    }

    private fun xml(
        bytes: ByteArray,
        visit: (XMLStreamReader) -> Unit,
    ) {
        val factory =
            XMLInputFactory
                .newFactory()
                .apply {
                    setProperty(
                        XMLInputFactory.SUPPORT_DTD,
                        false,
                    )
                    setProperty(
                        XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES,
                        false,
                    )
                    setProperty(
                        XMLInputFactory.IS_NAMESPACE_AWARE,
                        true,
                    )
                }
        val reader =
            factory.createXMLStreamReader(
                ByteArrayInputStream(
                    bytes,
                ),
            )
        try {
            while (
                reader.hasNext()
            ) {
                reader.next()
                visit(
                    reader,
                )
            }
        } finally {
            reader.close()
        }
    }

    internal fun strictUtf8(
        bytes: ByteArray,
    ): String? =
        try {
            StandardCharsets.UTF_8
                .newDecoder()
                .onMalformedInput(
                    CodingErrorAction.REPORT,
                )
                .onUnmappableCharacter(
                    CodingErrorAction.REPORT,
                )
                .decode(
                    ByteBuffer.wrap(
                        bytes,
                    ),
                )
                .toString()
        } catch (
            _: CharacterCodingException,
        ) {
            null
        }

    internal fun lineCount(
        text: String,
    ): Int =
        if (
            text.isEmpty()
        ) {
            0
        } else {
            text
                .removeSuffix(
                    "\n",
                )
                .count {
                    it ==
                        '\n'
                } +
                1
        }

    private val SlidePath =
        Regex(
            """ppt/slides/slide(\d+)\.xml""",
        )

    private val SheetPath =
        Regex(
            """xl/worksheets/sheet(\d+)\.xml""",
        )
}

// [VXG RealForever]
