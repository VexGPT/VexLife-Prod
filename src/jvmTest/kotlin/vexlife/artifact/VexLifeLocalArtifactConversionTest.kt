package vexlife.artifact

import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class VexLifeLocalArtifactConversionTest {
    private fun zipOf(
        vararg members: Pair<String, ByteArray>,
    ): ByteArray {
        val output =
            ByteArrayOutputStream()
        ZipOutputStream(
            output,
        )
            .use {
                zip ->
                members.forEach {
                    (name, bytes) ->
                    zip.putNextEntry(
                        ZipEntry(
                            name,
                        ),
                    )
                    zip.write(
                        bytes,
                    )
                    zip.closeEntry()
                }
            }
        return output.toByteArray()
    }

    private fun readable(
        name: String,
        bytes: ByteArray,
    ) =
        assertIs<VexLifeLocalArtifactConversion.Result.Readable>(
            VexLifeLocalArtifactConversion.readable(
                displayName = name,
                bytes = bytes,
            ),
        )

    private fun held(
        name: String,
        bytes: ByteArray,
    ) =
        assertIs<VexLifeLocalArtifactConversion.Result.Held>(
            VexLifeLocalArtifactConversion.readable(
                displayName = name,
                bytes = bytes,
            ),
        )

    private fun assertIndexLinesAreExact(
        text: String,
        sections: Map<String, String>,
    ) {
        val lines =
            text
                .removeSuffix(
                    "\n",
                )
                .split(
                    '\n',
                )
        sections.forEach {
            (name, firstLine) ->
            val entry =
                lines.first {
                    it.startsWith(
                        "- " +
                            name +
                            " · line ",
                    )
                }
            val start =
                entry
                    .substringAfter(
                        " · line ",
                    )
                    .substringBefore(
                        " · ",
                    )
                    .toInt()
            assertEquals(
                firstLine,
                lines[
                    start -
                        1
                ],
                "index line for " +
                    name,
            )
            assertEquals(
                "=== " +
                    name +
                    " ===",
                lines[
                    start -
                        2
                ],
            )
        }
    }

    @Test
    fun plainTextIsKeptVerbatim() {
        val result =
            readable(
                "a.txt",
                "hello\nworld\n"
                    .toByteArray(),
            )
        assertEquals(
            VexLifeLocalArtifactConversion.Format.TEXT,
            result.format,
        )
        assertEquals(
            "hello\nworld\n",
            result.text,
        )
    }

    @Test
    fun zipBecomesIndexedTextOfOnlyReadableMembers() {
        val result =
            readable(
                "project.zip",
                zipOf(
                    "src/a.kt" to
                        "fun a() = 1\nfun b() = 2\n"
                            .toByteArray(),
                    "README.md" to
                        "# Title\nbody\n"
                            .toByteArray(),
                    "logo.png" to
                        byteArrayOf(
                            0x89.toByte(),
                            0x50,
                            0x4e,
                            0x47,
                            0,
                        ),
                ),
            )

        assertEquals(
            VexLifeLocalArtifactConversion.Format.ZIP,
            result.format,
        )
        assertTrue(
            result.text.startsWith(
                "ZIP archive · 3 files · 2 read as text\n",
            ),
        )
        assertTrue(
            result.text.contains(
                "Not read: logo.png (not text)",
            ),
        )
        assertIndexLinesAreExact(
            result.text,
            mapOf(
                "src/a.kt" to
                    "fun a() = 1",
                "README.md" to
                    "# Title",
            ),
        )
    }

    @Test
    fun officeTypeComesFromCanonicalMembersNotTheFilenameSuffix() {
        val document =
            """<?xml version="1.0" encoding="UTF-8"?>
            <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"><w:body>
            <w:p><w:r><w:t>Renamed Word body</w:t></w:r></w:p>
            </w:body></w:document>"""
                .trimIndent()

        val result =
            readable(
                "mystery.data",
                zipOf(
                    "word/document.xml" to
                        document.toByteArray(),
                ),
            )

        assertEquals(
            VexLifeLocalArtifactConversion.Format.DOCX,
            result.format,
        )
        assertEquals(
            "converter.vexlife-prod.docx-text.v1",
            result.converterRef,
        )
        assertTrue(
            result.text.contains(
                "Renamed Word body",
            ),
        )
    }

    @Test
    fun docxBecomesParagraphText() {
        val document =
            """<?xml version="1.0" encoding="UTF-8"?>
            <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"><w:body>
            <w:p><w:r><w:t>First</w:t></w:r><w:r><w:t xml:space="preserve"> paragraph</w:t></w:r></w:p>
            <w:p><w:r><w:t>Second</w:t><w:tab/><w:t>tabbed</w:t></w:r></w:p>
            </w:body></w:document>"""
                .trimIndent()
        val result =
            readable(
                "Report.docx",
                zipOf(
                    "word/document.xml" to
                        document.toByteArray(),
                ),
            )

        assertEquals(
            VexLifeLocalArtifactConversion.Format.DOCX,
            result.format,
        )
        assertTrue(
            result.text.contains(
                "First paragraph\nSecond\ttabbed",
            ),
        )
        assertIndexLinesAreExact(
            result.text,
            mapOf(
                "Document" to
                    "First paragraph",
            ),
        )
    }

    @Test
    fun xlsxBecomesTabSeparatedRowsPerNamedSheet() {
        val workbook =
            """<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"
                 xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
               <sheets><sheet name="Budget" sheetId="1" r:id="rId1"/><sheet name="Notes" sheetId="2" r:id="rId2"/></sheets>
               </workbook>"""
                .trimIndent()
        val rels =
            """<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
               <Relationship Id="rId1" Target="worksheets/sheet1.xml" Type="x"/>
               <Relationship Id="rId2" Target="/xl/worksheets/sheet2.xml" Type="x"/>
               </Relationships>"""
                .trimIndent()
        val shared =
            """<sst xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
               <si><t>Item</t></si><si><t>Cost</t></si><si><r><t>Ren</t></r><r><t>t</t></r></si></sst>"""
                .trimIndent()
        val sheet1 =
            """<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>
               <row r="1"><c r="A1" t="s"><v>0</v></c><c r="B1" t="s"><v>1</v></c></row>
               <row r="2"><c r="A2" t="s"><v>2</v></c><c r="C2"><v>1200</v></c></row>
               </sheetData></worksheet>"""
                .trimIndent()
        val sheet2 =
            """<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>
               <row r="1"><c r="A1" t="inlineStr"><is><t>Paid monthly</t></is></c><c r="B1" t="b"><v>1</v></c></row>
               </sheetData></worksheet>"""
                .trimIndent()

        val result =
            readable(
                "money.xlsx",
                zipOf(
                    "xl/workbook.xml" to
                        workbook.toByteArray(),
                    "xl/_rels/workbook.xml.rels" to
                        rels.toByteArray(),
                    "xl/sharedStrings.xml" to
                        shared.toByteArray(),
                    "xl/worksheets/sheet1.xml" to
                        sheet1.toByteArray(),
                    "xl/worksheets/sheet2.xml" to
                        sheet2.toByteArray(),
                ),
            )

        assertEquals(
            VexLifeLocalArtifactConversion.Format.XLSX,
            result.format,
        )
        assertTrue(
            result.text.contains(
                "Item\tCost\nRent\t\t1200\n",
            ),
        )
        assertIndexLinesAreExact(
            result.text,
            mapOf(
                "Budget" to
                    "Item\tCost",
                "Notes" to
                    "Paid monthly\tTRUE",
            ),
        )
    }

    @Test
    fun pptxBecomesSlidesInNumericOrder() {
        fun slide(
            text: String,
        ) =
            """<p:sld xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main"
                 xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main">
               <a:p><a:r><a:t>$text</a:t></a:r></a:p></p:sld>"""
                .trimIndent()
                .toByteArray()

        val result =
            readable(
                "deck.pptx",
                zipOf(
                    "ppt/slides/slide10.xml" to
                        slide(
                            "Ten",
                        ),
                    "ppt/slides/slide2.xml" to
                        slide(
                            "Two",
                        ),
                ),
            )

        assertEquals(
            VexLifeLocalArtifactConversion.Format.PPTX,
            result.format,
        )
        assertTrue(
            result.text.indexOf(
                "=== Slide 2 ===",
            ) <
                result.text.indexOf(
                    "=== Slide 10 ===",
                ),
        )
        assertIndexLinesAreExact(
            result.text,
            mapOf(
                "Slide 2" to
                    "Two",
                "Slide 10" to
                    "Ten",
            ),
        )
    }

    @Test
    fun externalEntitiesAreNeverResolved() {
        val hostile =
            """<?xml version="1.0"?><!DOCTYPE d [<!ENTITY x SYSTEM "file:///etc/hostname">]>
            <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"><w:body>
            <w:p><w:r><w:t>&x;</w:t></w:r></w:p></w:body></w:document>"""
                .trimIndent()
        val result =
            VexLifeLocalArtifactConversion.readable(
                displayName = "x.docx",
                bytes =
                    zipOf(
                        "word/document.xml" to
                            hostile.toByteArray(),
                    ),
            )

        if (
            result is
            VexLifeLocalArtifactConversion.Result.Readable
        ) {
            assertFalse(
                result.text.contains(
                    "root:",
                ),
                "external entity content must never enter the derived projection",
            )
        }
    }

    @Test
    fun damagedEmptyOrForeignContainersAreHeldNotCrashed() {
        assertEquals(
            VexLifeLocalArtifactConversion.Refusal.UNREADABLE,
            held(
                "broken.docx",
                zipOf(
                    "word/document.xml" to
                        "<broken"
                            .toByteArray(),
                ),
            ).refusal,
        )
        assertEquals(
            VexLifeLocalArtifactConversion.Refusal.UNREADABLE,
            held(
                "a.zip",
                zipOf(
                    "a.bin" to
                        byteArrayOf(
                            0,
                            1,
                            2,
                        ),
                ),
            ).refusal,
        )
        assertEquals(
            VexLifeLocalArtifactConversion.Refusal.UNREADABLE,
            held(
                "a.zip",
                byteArrayOf(
                    'P'.code.toByte(),
                    'K'.code.toByte(),
                    3,
                    4,
                    9,
                    9,
                ),
            ).refusal,
        )
        assertEquals(
            VexLifeLocalArtifactConversion.Refusal.NOT_TEXT,
            held(
                "a.pdf",
                "%PDF-1.7\n"
                    .toByteArray() +
                    byteArrayOf(
                        0,
                        0xff.toByte(),
                    ),
            ).refusal,
        )
    }

    @Test
    fun inflationIsBoundedBeforeItCanExhaustMemory() {
        val bomb =
            zipOf(
                "zeros.txt" to
                    ByteArray(
                        VexLifeLocalArtifactConversion.MaximumInflatedBytes +
                            1,
                    ),
            )
        assertTrue(
            bomb.size <
                1024 * 1024,
            "fixture must be a small archive that inflates hugely",
        )
        assertEquals(
            VexLifeLocalArtifactConversion.Refusal.UNREADABLE,
            held(
                "bomb.zip",
                bomb,
            ).refusal,
        )
    }

    @Test
    fun spreadsheetSemanticExpansionIsHeldBeforeProjectionMemoryRunsAway() {
        val repeated =
            "R".repeat(
                2_048,
            )
        val shared =
            """<sst xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><si><t>$repeated</t></si></sst>"""
                .toByteArray()
        val cells =
            buildString {
                append(
                    """<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>""",
                )
                for (
                    row in
                    1..5_000
                ) {
                    append(
                        "<row r=\"" +
                            row +
                            "\"><c r=\"A" +
                            row +
                            "\" t=\"s\"><v>0</v></c></row>",
                    )
                }
                append(
                    "</sheetData></worksheet>",
                )
            }
                .toByteArray()

        val result =
            held(
                "amplify.xlsx",
                zipOf(
                    "xl/sharedStrings.xml" to
                        shared,
                    "xl/worksheets/sheet1.xml" to
                        cells,
                ),
            )

        assertEquals(
            VexLifeLocalArtifactConversion.Refusal.TOO_LARGE,
            result.refusal,
        )
    }
}

// [VXG RealForever]
