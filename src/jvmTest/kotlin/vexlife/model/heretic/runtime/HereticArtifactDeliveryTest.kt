package vexlife.model.heretic.runtime

import java.io.ByteArrayInputStream
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import vexlife.artifact.DirectHttpsArtifactDelivery
import vexlife.model.heretic.HereticArtifactCatalog

class HereticArtifactDeliveryTest {
    @Test
    fun everyCertifiedMemberResolvesToOneProdOwnedFixedHttpsChannel() {
        for (
            member in
            HereticArtifactCatalog.Members
        ) {
            val channel =
                HereticArtifactDelivery.channelFor(
                    member,
                )

            assertTrue(
                channel.channelRef.startsWith(
                    "channel.vexlife-prod.heretic.qwen3.8-27b-mlx4.",
                ),
            )
            assertEquals(
                HereticArtifactDelivery.TransportClass,
                channel.transportClass,
            )
            assertTrue(
                channel.url.startsWith(
                    "https://huggingface.co/" +
                        HereticArtifactDelivery.ProviderRepository +
                        "/resolve/" +
                        "808b82a2bd04b2bac362b5d0b7a1197aeea59e57/",
                ),
            )
            assertTrue(
                channel.url.endsWith(
                    "/" +
                        member.filename +
                        "?download=true",
                ),
            )
        }
    }

    @Test
    fun currentConfigMemberMatchesTheAcceptedVexLifeDeliveryUrl() {
        val member =
            HereticArtifactCatalog.Members
                .single {
                    it.filename ==
                        "config.json"
                }

        assertEquals(
            "https://huggingface.co/donedynamics/Qwen3.8-27B-heretic-MLX-4bit/resolve/" +
                "808b82a2bd04b2bac362b5d0b7a1197aeea59e57/config.json?download=true",
            HereticArtifactDelivery.channelFor(
                member,
            )
                .url,
        )
    }

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
                HereticArtifactDelivery.channelFor(
                    member,
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
