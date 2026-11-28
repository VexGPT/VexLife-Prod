package vexlife.artifact

import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import vexlife.home.CanonicalVexLifeHomeFixture

class VexLifeLocalArtifactReaderTest {
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

    @Test
    fun plainTextFormsOneRawArtifactAndOneReadableProjection() {
        val parent =
            Files.createTempDirectory(
                "vexlife-local-artifact-reader-text-",
            )
                .toAbsolutePath()
        val home =
            parent.resolve(
                "VexLife-Prod",
            )

        try {
            CanonicalVexLifeHomeFixture.form(
                home = home,
                deviceSuffix = "local-reader-text",
                lineageSuffix = "local-reader-text",
            )
            val store =
                VexLifeLocalArtifactStore(
                    home,
                )
            val ready =
                assertIs<VexLifeLocalArtifactReader.Result.Ready>(
                    VexLifeLocalArtifactReader.stage(
                        store = store,
                        rawName = "folder/notes.txt",
                        bytes =
                            "\uFEFFhello\nworld\n"
                                .toByteArray(
                                    StandardCharsets.UTF_8,
                                ),
                    ),
                )

            assertEquals(
                VexLifeLocalArtifactConversion.Format.TEXT,
                ready.format,
            )
            assertEquals(
                "converter.vexlife-prod.plain-text.v1",
                ready.projection.converterRef,
            )
            assertEquals(
                "hello\nworld\n",
                ready.projection.text,
            )
            assertEquals(
                2,
                ready.projection.lineCount,
            )
            assertTrue(
                store.rawBytes(
                    ready.original.artifactRef,
                )!!
                    .contentEquals(
                        "\uFEFFhello\nworld\n"
                            .toByteArray(
                                StandardCharsets.UTF_8,
                            ),
                    ),
                "raw custody must preserve the person's exact bytes even when the projection removes a BOM",
            )
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun officeContainerKeepsRawBytesAndBindsItsExactConverterProjection() {
        val parent =
            Files.createTempDirectory(
                "vexlife-local-artifact-reader-docx-",
            )
                .toAbsolutePath()
        val home =
            parent.resolve(
                "VexLife-Prod",
            )

        try {
            CanonicalVexLifeHomeFixture.form(
                home = home,
                deviceSuffix = "local-reader-docx",
                lineageSuffix = "local-reader-docx",
            )
            val store =
                VexLifeLocalArtifactStore(
                    home,
                )
            val document =
                """<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"><w:body>
                <w:p><w:r><w:t>Quarterly evidence</w:t></w:r></w:p>
                </w:body></w:document>"""
                    .trimIndent()
            val bytes =
                zipOf(
                    "word/document.xml" to
                        document.toByteArray(),
                )

            val ready =
                assertIs<VexLifeLocalArtifactReader.Result.Ready>(
                    VexLifeLocalArtifactReader.stage(
                        store = store,
                        rawName = "renamed-file.bin",
                        bytes = bytes,
                    ),
                )

            assertEquals(
                VexLifeLocalArtifactConversion.Format.DOCX,
                ready.format,
            )
            assertEquals(
                "converter.vexlife-prod.docx-text.v1",
                ready.projection.converterRef,
            )
            assertTrue(
                ready.projection.text.contains(
                    "Quarterly evidence",
                ),
            )
            assertTrue(
                assertNotNull(
                    store.rawBytes(
                        ready.original.artifactRef,
                    ),
                )
                    .contentEquals(
                        bytes,
                    ),
            )
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun currentlyUnreadableBytesRemainRawButCannotBecomeAReadyDraftProjection() {
        val parent =
            Files.createTempDirectory(
                "vexlife-local-artifact-reader-held-",
            )
                .toAbsolutePath()
        val home =
            parent.resolve(
                "VexLife-Prod",
            )

        try {
            CanonicalVexLifeHomeFixture.form(
                home = home,
                deviceSuffix = "local-reader-held",
                lineageSuffix = "local-reader-held",
            )
            val store =
                VexLifeLocalArtifactStore(
                    home,
                )
            val bytes =
                "%PDF-1.7\n"
                    .toByteArray() +
                    byteArrayOf(
                        0,
                        0xff.toByte(),
                    )

            val held =
                assertIs<VexLifeLocalArtifactReader.Result.Held>(
                    VexLifeLocalArtifactReader.stage(
                        store = store,
                        rawName = "later.pdf",
                        bytes = bytes,
                    ),
                )

            assertEquals(
                VexLifeLocalArtifactConversion.Refusal.NOT_TEXT,
                held.refusal,
            )
            val artifactRef =
                assertNotNull(
                    held.artifactRefOrNull,
                )
            assertTrue(
                store.rawBytes(
                    artifactRef,
                )!!
                    .contentEquals(
                        bytes,
                    ),
                "a later earned PDF reader may reuse exact raw custody without pretending PDF is readable now",
            )
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }
}

// [VXG RealForever]
