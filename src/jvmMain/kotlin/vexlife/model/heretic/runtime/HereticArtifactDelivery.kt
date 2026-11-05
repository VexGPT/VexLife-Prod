package vexlife.model.heretic.runtime

import java.nio.file.Path
import vexlife.artifact.DirectHttpsArtifactDelivery
import vexlife.model.heretic.HereticArtifactCatalog
import vexlife.model.heretic.HereticArtifactDeliveryCatalog

/**
 * JVM realization of the current Prod-owned Heretic delivery catalog.
 *
 * Channel identity and provider topology are source-owned by [HereticArtifactDeliveryCatalog].
 * This layer only binds an admitted catalog channel to exact HTTPS transport mechanics.
 */
object HereticArtifactDelivery {
    class Acquirer(
        private val transport: DirectHttpsArtifactDelivery.Transport =
            DirectHttpsArtifactDelivery.JdkTransport,
        private val onProgress: (
            member: HereticArtifactCatalog.Member,
            progress: DirectHttpsArtifactDelivery.Progress,
        ) -> Unit = {
            _,
            _,
            ->
        },
    ) : HereticArtifactMaterialization.Acquirer {
        override fun acquire(
            member: HereticArtifactCatalog.Member,
            finalPath: Path,
        ) {
            val channel =
                HereticArtifactDeliveryCatalog.channelFor(
                    member.artifactRef,
                )

            require(
                channel.transportClass ==
                    HereticArtifactDeliveryCatalog.TransportClass,
            ) {
                "Heretic delivery channel is not admitted by this transport realization"
            }

            DirectHttpsArtifactDelivery.download(
                request =
                    DirectHttpsArtifactDelivery.Request(
                        url = channel.url,
                        expectedSha256 = member.sha256,
                        expectedBytes = member.expectedBytes,
                        maxBytes = member.expectedBytes,
                    ),
                finalPath = finalPath,
                transport = transport,
                onProgress = {
                    progress ->
                    onProgress(
                        member,
                        progress,
                    )
                },
            )
        }
    }
}

// [VXG RealForever]
