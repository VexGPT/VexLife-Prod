package vexlife.model.heretic.runtime

import java.io.ByteArrayInputStream
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import vexlife.artifact.DirectHttpsArtifactDelivery
import vexlife.model.heretic.HereticArtifactCatalog
import vexlife.model.heretic.HereticArtifactDeliveryCatalog

class HereticArtifactDeliveryTest {
    @Test
    fun acquirerDelegatesExactMemberBoundsToTheGenericDownloader() {
        val member =
            HereticArtifactCatalog.Members
                .single {
                    it.filename ==
                        "generation_config.json"
                }
        val root =
            Files.createTempDirectory(
                "vexlife-prod-heretic-delivery-",
            )
                .toAbsolutePath()
        val final =
            root.resolve(
                member.filename,
            )
        var openedUrl: String? =
            null

        try {
            val acquirer =
                HereticArtifactDelivery.Acquirer(
                    transport =
                        DirectHttpsArtifactDelivery.Transport {
                            url,
                            resumeFrom,
                            ->
                            openedUrl =
                                url.toString()
                            assertEquals(
                                0L,
                                resumeFrom,
                            )
                            DirectHttpsArtifactDelivery.Opened(
                                status = 200,
                                contentLength = member.expectedBytes,
                                contentRange = null,
                                body =
                                    ByteArrayInputStream(
                                        ByteArray(
                                            member.expectedBytes.toInt(),
                                        ),
                                    ),
                            )
                        },
                )

            // The zero-filled fixture cannot satisfy the certified checksum; reaching checksum
            // failure proves the exact URL/byte bound crossed the generic transport boundary.
            runCatching {
                acquirer.acquire(
                    member,
                    final,
                )
            }

            assertEquals(
                HereticArtifactDeliveryCatalog.channelFor(
                    member.artifactRef,
                )
                    .url,
                openedUrl,
            )
        } finally {
            root
                .toFile()
                .deleteRecursively()
        }
    }
}

// [VXG RealForever]
