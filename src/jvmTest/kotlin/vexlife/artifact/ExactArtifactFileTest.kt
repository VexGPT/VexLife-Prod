package vexlife.artifact

import java.nio.file.Files
import java.security.MessageDigest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ExactArtifactFileTest {
    private fun sha256(
        text: String,
    ): String =
        MessageDigest
            .getInstance(
                "SHA-256",
            )
            .digest(
                text.toByteArray(),
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
    fun missingSizeHashAndExactAreDistinctNoEffectObservations() {
        val root =
            Files.createTempDirectory(
                "vexlife-prod-exact-artifact-",
            )
                .toAbsolutePath()
        val file =
            root.resolve(
                "artifact.bin",
            )
        val exact =
            "exact"

        try {
            assertIs<ExactArtifactFile.Classification.Missing>(
                ExactArtifactFile.classify(
                    file,
                    sha256(
                        exact,
                    ),
                    exact.length.toLong(),
                ),
            )

            Files.writeString(
                file,
                "too-long",
            )
            assertIs<ExactArtifactFile.Classification.InvalidSize>(
                ExactArtifactFile.classify(
                    file,
                    sha256(
                        exact,
                    ),
                    exact.length.toLong(),
                ),
            )

            Files.writeString(
                file,
                "other",
            )
            assertIs<ExactArtifactFile.Classification.InvalidHash>(
                ExactArtifactFile.classify(
                    file,
                    sha256(
                        exact,
                    ),
                    exact.length.toLong(),
                ),
            )

            Files.writeString(
                file,
                exact,
            )
            val verified =
                assertIs<ExactArtifactFile.Classification.VerifiedReusable>(
                    ExactArtifactFile.classify(
                        file,
                        sha256(
                            exact,
                        ),
                        exact.length.toLong(),
                    ),
                )

            assertEquals(
                exact.length.toLong(),
                verified.bytes,
            )
            assertEquals(
                sha256(
                    exact,
                ),
                verified.actualSha256,
            )
        } finally {
            root
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun symlinkAndDirectoryCannotMasqueradeAsArtifactFiles() {
        val root =
            Files.createTempDirectory(
                "vexlife-prod-exact-artifact-policy-",
            )
                .toAbsolutePath()
        val target =
            root.resolve(
                "target.bin",
            )
        val link =
            root.resolve(
                "link.bin",
            )

        try {
            Files.writeString(
                target,
                "x",
            )
            Files.createSymbolicLink(
                link,
                target,
            )

            assertFailsWith<IllegalArgumentException> {
                ExactArtifactFile.classify(
                    link,
                    sha256(
                        "x",
                    ),
                    1,
                )
            }
            assertFailsWith<IllegalArgumentException> {
                ExactArtifactFile.classify(
                    root,
                    sha256(
                        "x",
                    ),
                    1,
                )
            }

            assertTrue(
                Files.isSymbolicLink(
                    link,
                ),
            )
        } finally {
            Files.deleteIfExists(
                link,
            )
            root
                .toFile()
                .deleteRecursively()
        }
    }
}

// [VXG RealForever]
