package vexlife.artifact

import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import java.security.MessageDigest

/**
 * Generic exact local-file observation for content-addressed artifacts.
 *
 * This owner performs no download, deletion, repair, or replacement effect.
 */
object ExactArtifactFile {
    sealed interface Classification {
        val path: String

        data class Missing(
            override val path: String,
        ) : Classification

        data class InvalidSize(
            override val path: String,
            val bytes: Long,
            val expectedBytes: Long,
        ) : Classification

        data class InvalidHash(
            override val path: String,
            val bytes: Long,
            val actualSha256: String,
        ) : Classification

        data class VerifiedReusable(
            override val path: String,
            val bytes: Long,
            val actualSha256: String,
        ) : Classification
    }

    fun classify(
        path: Path,
        expectedSha256: String,
        expectedBytes: Long,
    ): Classification {
        require(
            path.isAbsolute,
        ) {
            "artifact path must be absolute"
        }
        require(
            expectedSha256.matches(
                Regex(
                    "[0-9a-f]{64}",
                ),
            ),
        ) {
            "expectedSha256 must be lowercase SHA-256"
        }
        require(
            expectedBytes >
                0,
        ) {
            "expectedBytes must be positive"
        }

        val normalized =
            path.normalize()

        if (
            !Files.exists(
                normalized,
                LinkOption.NOFOLLOW_LINKS,
            )
        ) {
            return Classification.Missing(
                normalized.toString(),
            )
        }

        val attributes =
            Files.readAttributes(
                normalized,
                BasicFileAttributes::class.java,
                LinkOption.NOFOLLOW_LINKS,
            )

        require(
            attributes.isRegularFile &&
                !attributes.isSymbolicLink,
        ) {
            "artifact path must be one regular non-symlink file"
        }

        if (
            attributes.size() !=
                expectedBytes
        ) {
            return Classification.InvalidSize(
                path = normalized.toString(),
                bytes = attributes.size(),
                expectedBytes = expectedBytes,
            )
        }

        val actualSha256 =
            sha256(
                normalized,
            )

        return if (
            actualSha256 ==
                expectedSha256
        ) {
            Classification.VerifiedReusable(
                path = normalized.toString(),
                bytes = attributes.size(),
                actualSha256 = actualSha256,
            )
        } else {
            Classification.InvalidHash(
                path = normalized.toString(),
                bytes = attributes.size(),
                actualSha256 = actualSha256,
            )
        }
    }

    fun sha256(
        path: Path,
    ): String {
        val digest =
            MessageDigest.getInstance(
                "SHA-256",
            )

        Files.newInputStream(
            path,
        )
            .use {
                input ->
                val buffer =
                    ByteArray(
                        1024 *
                            1024,
                    )

                while (
                    true
                ) {
                    val count =
                        input.read(
                            buffer,
                        )
                    if (
                        count <
                            0
                    ) {
                        break
                    }

                    digest.update(
                        buffer,
                        0,
                        count,
                    )
                }
            }

        return digest
            .digest()
            .joinToString(
                "",
            ) {
                "%02x".format(
                    it.toInt() and
                        0xff,
                )
            }
    }
}

// [VXG RealForever]
