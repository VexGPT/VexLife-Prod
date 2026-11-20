package vexlife.artifact

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import vexlife.home.CanonicalVexLifeHomeFixture

class VexLifePlainTextArtifactReaderTest {
    @Test
    fun strictUtf8FormsOneRawArtifactAndOneReadableProjection() {
        val parent =
            Files.createTempDirectory(
                "vexlife-plain-artifact-reader-",
            )
                .toAbsolutePath()
        val home =
            parent.resolve(
                "VexLife-Prod",
            )

        try {
            CanonicalVexLifeHomeFixture.form(
                home = home,
                deviceSuffix = "plain-reader",
                lineageSuffix = "plain-reader",
            )
            val store =
                VexLifeLocalArtifactStore(
                    home,
                )
            val ready =
                assertIs<VexLifePlainTextArtifactReader.Result.Ready>(
                    VexLifePlainTextArtifactReader.stage(
                        store = store,
                        rawName = "folder/notes.txt",
                        bytes =
                            "\uFEFFhello\nworld\n"
                                .toByteArray(
                                    StandardCharsets.UTF_8,
                                ),
                    ),
                )

            assertTrue(
                VexLifeLocalArtifact.isArtifactRef(
                    ready.original.artifactRef,
                ),
            )
            assertEquals(
                VexLifePlainTextArtifactReader.ConverterRef,
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
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun binaryBytesAreHeldButTheirExactRawArtifactMayRemainInCustody() {
        val parent =
            Files.createTempDirectory(
                "vexlife-plain-artifact-binary-",
            )
                .toAbsolutePath()
        val home =
            parent.resolve(
                "VexLife-Prod",
            )

        try {
            CanonicalVexLifeHomeFixture.form(
                home = home,
                deviceSuffix = "plain-binary",
                lineageSuffix = "plain-binary",
            )
            val store =
                VexLifeLocalArtifactStore(
                    home,
                )
            val held =
                assertIs<VexLifePlainTextArtifactReader.Result.Held>(
                    VexLifePlainTextArtifactReader.stage(
                        store = store,
                        rawName = "later.zip",
                        bytes =
                            byteArrayOf(
                                0x50,
                                0x4b,
                                0x03,
                                0x04,
                                0x00,
                                0xff.toByte(),
                            ),
                    ),
                )

            assertEquals(
                "workspace.status.attachment-refused",
                held.reasonKey,
            )
            assertTrue(
                held.artifactRefOrNull !=
                    null,
                "exact raw bytes may remain available for the later container reader",
            )
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }
}

// [VXG RealForever]
