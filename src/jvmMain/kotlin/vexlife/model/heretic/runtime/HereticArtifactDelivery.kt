package vexlife.model.heretic.runtime

import java.nio.file.Path
import vexlife.artifact.DirectHttpsArtifactDelivery
import vexlife.model.heretic.HereticArtifactCatalog

/**
 * Prod-owned delivery relationship for the certified Heretic catalog.
 *
 * URLs are reconstructed from one exact provider repository + provider revision + certified
 * filename. Channel refs are re-addressed into the VexLife-Prod namespace.
 */
object HereticArtifactDelivery {
    const val TransportClass =
        "DIRECT_HTTPS_FILE_V1"

    const val ProviderRepository =
        "donedynamics/Qwen3.8-27B-heretic-MLX-4bit"

    data class Channel(
        val channelRef: String,
        val transportClass: String,
        val url: String,
    )

    fun channelFor(
        member: HereticArtifactCatalog.Member,
    ): Channel {
        val providerRevision =
            requireNotNull(
                Regex(
                    "\\.([0-9a-f]{40})$",
                )
                    .find(
                        member.sourceRef,
                    )
                    ?.groupValues
                    ?.get(
                        1,
                    ),
            ) {
                "Heretic provider source ref must end in one exact revision"
            }

        val role =
            member.roleRef
                .substringAfter(
                    ".member.",
                    missingDelimiterValue = "",
                )

        require(
            role.isNotBlank(),
        ) {
            "Heretic artifact role ref must expose one member role"
        }

        return Channel(
            channelRef =
                "channel.vexlife-prod.heretic.qwen3.8-27b-mlx4." +
                    role +
                    ".hf-fixed.001",
            transportClass = TransportClass,
            url =
                "https://huggingface.co/" +
                    ProviderRepository +
                    "/resolve/" +
                    providerRevision +
                    "/" +
                    member.filename +
                    "?download=true",
        )
    }

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
                channelFor(
                    member,
                )

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
