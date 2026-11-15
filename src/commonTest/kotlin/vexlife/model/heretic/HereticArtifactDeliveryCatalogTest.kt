package vexlife.model.heretic

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HereticArtifactDeliveryCatalogTest {
    @Test
    fun everyCertifiedArtifactHasExactlyOneProdOwnedFixedRevisionChannel() {
        assertEquals(
            HereticModelCatalog.Entries.sumOf { it.members.size },
            HereticArtifactDeliveryCatalog.Channels.size,
        )
        // Every catalog model's member is delivered from its own pinned repository and revision.
        for (entry in HereticModelCatalog.Entries) {
            for (member in entry.members) {
                val channel = HereticArtifactDeliveryCatalog.channelFor(member.artifactRef)
                assertEquals(
                    "https://huggingface.co/" + entry.providerRepository + "/resolve/" + entry.providerRevision +
                        "/" + member.filename + "?download=true",
                    channel.url,
                )
            }
        }

        for (
            member in
            HereticArtifactCatalog.Members
        ) {
            val channel =
                HereticArtifactDeliveryCatalog.channelFor(
                    member.artifactRef,
                )

            assertEquals(
                HereticArtifactDeliveryCatalog.TransportClass,
                channel.transportClass,
            )
            assertTrue(
                channel.channelRef.startsWith(
                    "channel.vexlife-prod.heretic.",
                ),
            )
            assertTrue(
                channel.url.startsWith(
                    "https://huggingface.co/donedynamics/Qwen3.8-27B-heretic-MLX-4bit/resolve/",
                ),
            )
            assertTrue(
                channel.url.contains(
                    "/808b82a2bd04b2bac362b5d0b7a1197aeea59e57/",
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
    fun deliveryIdentityRemainsSeparateFromArtifactByteIdentity() {
        val member =
            HereticArtifactCatalog.Members.first()
        val channel =
            HereticArtifactDeliveryCatalog.channelFor(
                member.artifactRef,
            )

        assertEquals(
            member.artifactRef,
            channel.artifactRef,
        )
        assertTrue(
            member.sha256.isNotBlank(),
        )
        assertTrue(
            channel.channelRef !=
                member.artifactRef,
        )
    }
}

// [VXG RealForever]
