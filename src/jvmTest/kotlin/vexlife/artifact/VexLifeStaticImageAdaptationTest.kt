package vexlife.artifact

import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vexlife.home.CanonicalVexLifeHomeFixture

class VexLifeStaticImageAdaptationTest {
    private fun image(
        format: String,
        width: Int,
        height: Int,
    ): ByteArray {
        val output =
            ByteArrayOutputStream()
        val buffered =
            BufferedImage(
                width,
                height,
                BufferedImage.TYPE_INT_RGB,
            )
        assertTrue(
            ImageIO.write(
                buffered,
                format,
                output,
            ),
            "JDK image writer must form the synthetic fixture",
        )
        return output.toByteArray()
    }

    @Test
    fun pngAndJpegAreRecognizedFromBytesWithoutFilenameTrust() {
        listOf(
            Triple(
                image(
                    format = "png",
                    width = 2,
                    height = 3,
                ),
                VexLifeStaticImageAdaptation.Format.PNG,
                "renamed.bin",
            ),
            Triple(
                image(
                    format = "jpeg",
                    width = 4,
                    height = 5,
                ),
                VexLifeStaticImageAdaptation.Format.JPEG,
                "also-renamed.txt",
            ),
        )
            .forEach {
                (bytes, expectedFormat, displayName) ->
                val adapted =
                    assertIs<VexLifeStaticImageAdaptation.Result.Adapted>(
                        VexLifeStaticImageAdaptation.adapt(
                            displayName = displayName,
                            bytes = bytes,
                        ),
                    )

                assertEquals(
                    expectedFormat,
                    adapted.value.format,
                )
                assertEquals(
                    VexLifeStaticImageAdaptation.AdapterRef,
                    adapted.value.adapterRef,
                )
                assertTrue(
                    adapted.value.widthPixels >
                        0 &&
                        adapted.value.heightPixels >
                        0,
                )
            }
    }

    @Test
    fun filenameCannotSpoofImageAdmission() {
        val held =
            assertIs<VexLifeStaticImageAdaptation.Result.Held>(
                VexLifeStaticImageAdaptation.adapt(
                    displayName = "looks-like-an-image.png",
                    bytes =
                        "not an image"
                            .toByteArray(
                                StandardCharsets.UTF_8,
                            ),
                ),
            )

        assertEquals(
            VexLifeStaticImageAdaptation.Refusal.NOT_IMAGE,
            held.refusal,
        )
    }

    @Test
    fun damagedRecognizedSignatureIsHeld() {
        val held =
            assertIs<VexLifeStaticImageAdaptation.Result.Held>(
                VexLifeStaticImageAdaptation.adapt(
                    displayName = "damaged.png",
                    bytes =
                        byteArrayOf(
                            0x89.toByte(),
                            0x50,
                            0x4e,
                            0x47,
                            0x0d,
                            0x0a,
                            0x1a,
                            0x0a,
                            1,
                            2,
                            3,
                        ),
                ),
            )

        assertEquals(
            VexLifeStaticImageAdaptation.Refusal.UNREADABLE,
            held.refusal,
        )
    }

    @Test
    fun readerPreservesRecognizedImageBytesWithoutPretendingModelReachExists() {
        val parent =
            Files.createTempDirectory(
                "vexlife-static-image-reader-",
            )
                .toAbsolutePath()
        val home =
            parent.resolve(
                "VexLife-Prod",
            )

        try {
            CanonicalVexLifeHomeFixture.form(
                home = home,
                deviceSuffix = "static-image-reader",
                lineageSuffix = "static-image-reader",
            )
            val store =
                VexLifeLocalArtifactStore(
                    home,
                )
            val bytes =
                image(
                    format = "png",
                    width = 7,
                    height = 9,
                )
            val adapted =
                assertIs<VexLifeLocalArtifactReader.Result.ImageAdapted>(
                    VexLifeLocalArtifactReader.stage(
                        store = store,
                        rawName = "actually-an-image.txt",
                        bytes = bytes,
                    ),
                )

            assertEquals(
                VexLifeStaticImageAdaptation.Format.PNG,
                adapted.adaptation.format,
            )
            assertEquals(
                7,
                adapted.adaptation.widthPixels,
            )
            assertEquals(
                9,
                adapted.adaptation.heightPixels,
            )
            assertTrue(
                adapted.message.contains(
                    "no qualified visual-input path",
                ),
            )
            assertTrue(
                assertNotNull(
                    store.rawBytes(
                        adapted.original.artifactRef,
                    ),
                )
                    .contentEquals(
                        bytes,
                    ),
                "static image adaptation must preserve the exact original bytes",
            )
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun oversizedImageIsHeldBeforeRawCustodyOrAdaptation() {
        val parent =
            Files.createTempDirectory(
                "vexlife-static-image-oversized-",
            )
                .toAbsolutePath()
        val home =
            parent.resolve(
                "VexLife-Prod",
            )

        try {
            CanonicalVexLifeHomeFixture.form(
                home = home,
                deviceSuffix = "static-image-oversized",
                lineageSuffix = "static-image-oversized",
            )
            val store =
                VexLifeLocalArtifactStore(
                    home,
                )
            val bytes =
                ByteArray(
                    VexLifeLocalArtifact.MaximumOriginalBytes +
                        1,
                )
            byteArrayOf(
                0x89.toByte(),
                0x50,
                0x4e,
                0x47,
                0x0d,
                0x0a,
                0x1a,
                0x0a,
            )
                .copyInto(
                    bytes,
                )

            val held =
                assertIs<VexLifeLocalArtifactReader.Result.Held>(
                    VexLifeLocalArtifactReader.stage(
                        store = store,
                        rawName = "too-large.png",
                        bytes = bytes,
                    ),
                )

            assertEquals(
                VexLifeLocalArtifactConversion.Refusal.TOO_LARGE,
                held.refusal,
            )
            assertNull(
                held.artifactRefOrNull,
                "over-bound image bytes must not enter Local Artifact custody",
            )
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }
}

// [VXG RealForever]
