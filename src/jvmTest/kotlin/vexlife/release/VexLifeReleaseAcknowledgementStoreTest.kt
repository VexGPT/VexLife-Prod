package vexlife.release

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VexLifeReleaseAcknowledgementStoreTest {
    private fun stable(
        minor: Int,
        sourceCharacter: Char,
    ) =
        VexLifeReleaseIdentity.Value.Stable(
            version =
                VexLifeReleaseIdentity.SemanticVersion(
                    0,
                    minor,
                    0,
                ),
            sourceSha =
                VexLifeReleaseIdentity.SourceSha(
                    sourceCharacter
                        .toString()
                        .repeat(
                            40,
                        ),
                ),
        )

    @Test
    fun acknowledgementPersistsByExactReleaseIdentity() {
        val home =
            Files.createTempDirectory(
                "vexlife-release-ack-",
            )
                .toAbsolutePath()
        try {
            val store =
                VexLifeReleaseAcknowledgementStore(
                    home,
                )
            val first =
                stable(
                    5,
                    'a',
                )
            val second =
                stable(
                    6,
                    'b',
                )

            store.remember(
                first,
                VexLifeReleaseInboxFamily.AcknowledgementStanding.Viewed,
            )
            store.remember(
                second,
                VexLifeReleaseInboxFamily.AcknowledgementStanding.Dismissed,
            )
            store.remember(
                first,
                VexLifeReleaseInboxFamily.AcknowledgementStanding.Dismissed,
            )

            val read =
                VexLifeReleaseAcknowledgementStore(
                    home,
                )
                    .read()

            assertEquals(
                2,
                read.size,
            )
            assertEquals(
                VexLifeReleaseInboxFamily.AcknowledgementStanding.Dismissed,
                read.single {
                    it.identity ==
                        first
                }
                    .standing,
            )
            assertTrue(
                Files.isRegularFile(
                    home.resolve(
                        VexLifeReleaseAcknowledgementStore.RelativePath,
                    ),
                ),
            )
        } finally {
            home
                .toFile()
                .deleteRecursively()
        }
    }
}

// [VXG RealForever]
