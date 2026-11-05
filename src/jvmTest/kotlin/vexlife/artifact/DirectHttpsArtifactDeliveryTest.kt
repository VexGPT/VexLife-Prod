package vexlife.artifact

import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.security.MessageDigest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DirectHttpsArtifactDeliveryTest {
    private fun sha256(
        bytes: ByteArray,
    ): String =
        MessageDigest
            .getInstance(
                "SHA-256",
            )
            .digest(
                bytes,
            )
            .joinToString(
                "",
            ) {
                "%02x".format(
                    it.toInt() and
                        0xff,
                )
            }

    @Test
    fun freshDownloadVerifiesBeforeAtomicFinalization() {
        val root =
            Files.createTempDirectory(
                "vexlife-prod-download-",
            )
                .toAbsolutePath()
        val final =
            root.resolve(
                "artifact.bin",
            )
        val bytes =
            "certified".toByteArray(
                StandardCharsets.UTF_8,
            )
        val seen =
            mutableListOf<Long>()

        try {
            val result =
                DirectHttpsArtifactDelivery.download(
                    request =
                        DirectHttpsArtifactDelivery.Request(
                            url = "https://example.invalid/artifact.bin?token=not-recorded",
                            expectedSha256 =
                                sha256(
                                    bytes,
                                ),
                            expectedBytes =
                                bytes.size.toLong(),
                        ),
                    finalPath = final,
                    transport =
                        DirectHttpsArtifactDelivery.Transport {
                            _,
                            resumeFrom,
                            ->
                            seen +=
                                resumeFrom
                            DirectHttpsArtifactDelivery.Opened(
                                status = 200,
                                contentLength = bytes.size.toLong(),
                                contentRange = null,
                                body =
                                    ByteArrayInputStream(
                                        bytes,
                                    ),
                            )
                        },
                )

            assertEquals(
                listOf(
                    0L,
                ),
                seen,
            )
            assertEquals(
                "DOWNLOADED_AND_VERIFIED",
                result.disposition,
            )
            assertEquals(
                bytes.toList(),
                Files.readAllBytes(
                    final,
                )
                    .toList(),
            )
            assertEquals(
                "https://example.invalid/artifact.bin",
                result.recordedSourceUrl,
            )
            assertFalse(
                Files.exists(
                    root.resolve(
                        "artifact.bin.partial",
                    ),
                ),
            )
        } finally {
            root
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun exactRangeResumeAppendsAndVerifiesTheWholeArtifact() {
        val root =
            Files.createTempDirectory(
                "vexlife-prod-download-resume-",
            )
                .toAbsolutePath()
        val final =
            root.resolve(
                "artifact.bin",
            )
        val partial =
            root.resolve(
                "artifact.bin.partial",
            )
        val full =
            "certified".toByteArray(
                StandardCharsets.UTF_8,
            )
        val prefix =
            "cert".toByteArray(
                StandardCharsets.UTF_8,
            )
        val suffix =
            "ified".toByteArray(
                StandardCharsets.UTF_8,
            )

        try {
            Files.write(
                partial,
                prefix,
            )

            val result =
                DirectHttpsArtifactDelivery.download(
                    request =
                        DirectHttpsArtifactDelivery.Request(
                            url = "https://example.invalid/artifact.bin",
                            expectedSha256 =
                                sha256(
                                    full,
                                ),
                            expectedBytes =
                                full.size.toLong(),
                        ),
                    finalPath = final,
                    transport =
                        DirectHttpsArtifactDelivery.Transport {
                            _,
                            resumeFrom,
                            ->
                            assertEquals(
                                prefix.size.toLong(),
                                resumeFrom,
                            )
                            DirectHttpsArtifactDelivery.Opened(
                                status = 206,
                                contentLength = suffix.size.toLong(),
                                contentRange =
                                    "bytes ${prefix.size}-${full.size - 1}/${full.size}",
                                body =
                                    ByteArrayInputStream(
                                        suffix,
                                    ),
                            )
                        },
                )

            assertEquals(
                "RESUMED_AND_VERIFIED",
                result.disposition,
            )
            assertEquals(
                full.toList(),
                Files.readAllBytes(
                    final,
                )
                    .toList(),
            )
        } finally {
            root
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun serverIgnoringRangeRestartsFromZeroInsteadOfDuplicatingBytes() {
        val root =
            Files.createTempDirectory(
                "vexlife-prod-download-range-ignore-",
            )
                .toAbsolutePath()
        val final =
            root.resolve(
                "artifact.bin",
            )
        val partial =
            root.resolve(
                "artifact.bin.partial",
            )
        val full =
            "certified".toByteArray(
                StandardCharsets.UTF_8,
            )

        try {
            Files.writeString(
                partial,
                "cert",
            )

            val result =
                DirectHttpsArtifactDelivery.download(
                    request =
                        DirectHttpsArtifactDelivery.Request(
                            url = "https://example.invalid/artifact.bin",
                            expectedSha256 =
                                sha256(
                                    full,
                                ),
                            expectedBytes =
                                full.size.toLong(),
                        ),
                    finalPath = final,
                    transport =
                        DirectHttpsArtifactDelivery.Transport {
                            _,
                            resumeFrom,
                            ->
                            assertEquals(
                                4L,
                                resumeFrom,
                            )
                            DirectHttpsArtifactDelivery.Opened(
                                status = 200,
                                contentLength = full.size.toLong(),
                                contentRange = null,
                                body =
                                    ByteArrayInputStream(
                                        full,
                                    ),
                            )
                        },
                )

            assertEquals(
                "DOWNLOADED_AND_VERIFIED",
                result.disposition,
            )
            assertEquals(
                full.toList(),
                Files.readAllBytes(
                    final,
                )
                    .toList(),
            )
        } finally {
            root
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun checksumMismatchRemovesCorruptPartialAndNeverFormsFinal() {
        val root =
            Files.createTempDirectory(
                "vexlife-prod-download-hash-",
            )
                .toAbsolutePath()
        val final =
            root.resolve(
                "artifact.bin",
            )
        val wrong =
            "wrong".toByteArray(
                StandardCharsets.UTF_8,
            )

        try {
            assertFailsWith<IllegalStateException> {
                DirectHttpsArtifactDelivery.download(
                    request =
                        DirectHttpsArtifactDelivery.Request(
                            url = "https://example.invalid/artifact.bin",
                            expectedSha256 =
                                "0".repeat(
                                    64,
                                ),
                            expectedBytes =
                                wrong.size.toLong(),
                        ),
                    finalPath = final,
                    transport =
                        DirectHttpsArtifactDelivery.Transport {
                            _,
                            _,
                            ->
                            DirectHttpsArtifactDelivery.Opened(
                                status = 200,
                                contentLength = wrong.size.toLong(),
                                contentRange = null,
                                body =
                                    ByteArrayInputStream(
                                        wrong,
                                    ),
                            )
                        },
                )
            }

            assertFalse(
                Files.exists(
                    final,
                ),
            )
            assertFalse(
                Files.exists(
                    root.resolve(
                        "artifact.bin.partial",
                    ),
                ),
            )
        } finally {
            root
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun invalidExistingFinalIsNeverOverwrittenByDelivery() {
        val root =
            Files.createTempDirectory(
                "vexlife-prod-download-existing-",
            )
                .toAbsolutePath()
        val final =
            root.resolve(
                "artifact.bin",
            )
        var opened =
            false

        try {
            Files.writeString(
                final,
                "wrong",
            )

            assertFailsWith<IllegalStateException> {
                DirectHttpsArtifactDelivery.download(
                    request =
                        DirectHttpsArtifactDelivery.Request(
                            url = "https://example.invalid/artifact.bin",
                            expectedSha256 =
                                "0".repeat(
                                    64,
                                ),
                            expectedBytes = 5,
                        ),
                    finalPath = final,
                    transport =
                        DirectHttpsArtifactDelivery.Transport {
                            _,
                            _,
                            ->
                            opened =
                                true
                            error(
                                "transport should not be reached",
                            )
                        },
                )
            }

            assertFalse(
                opened,
            )
            assertEquals(
                "wrong",
                Files.readString(
                    final,
                ),
            )
        } finally {
            root
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun credentialedOrNonHttpsUrlIsRejectedBeforeTransport() {
        for (
            url in
            listOf(
                "http://example.invalid/a",
                "https://user:password@example.invalid/a",
            )
        ) {
            val root =
                Files.createTempDirectory(
                    "vexlife-prod-download-url-",
                )
                    .toAbsolutePath()

            try {
                assertFailsWith<IllegalArgumentException> {
                    DirectHttpsArtifactDelivery.download(
                        request =
                            DirectHttpsArtifactDelivery.Request(
                                url = url,
                                expectedSha256 =
                                    "0".repeat(
                                        64,
                                    ),
                                expectedBytes = 1,
                            ),
                        finalPath =
                            root.resolve(
                                "a",
                            ),
                        transport =
                            DirectHttpsArtifactDelivery.Transport {
                                _,
                                _,
                                ->
                                error(
                                    "transport should not be reached",
                                )
                            },
                    )
                }
            } finally {
                root
                    .toFile()
                    .deleteRecursively()
            }
        }
    }
}

// [VXG RealForever]
