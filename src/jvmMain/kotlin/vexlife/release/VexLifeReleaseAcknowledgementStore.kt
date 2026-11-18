package vexlife.release

import java.nio.file.Path
import kotlinx.serialization.Serializable
import vexlife.contract.VexDurableContractFile
import vexlife.filesystem.VexOwnedPath

/** Durable, local-only acknowledgement of VexLife release communication. */
class VexLifeReleaseAcknowledgementStore(
    productHome: Path,
) {
    @Serializable
    private data class Record(
        val release: VexLifeReleaseIdentity.Coordinate,
        val standing: VexLifeReleaseInboxFamily.AcknowledgementStanding,
    )

    @Serializable
    private data class Value(
        val schemaVersion: String,
        val acknowledgements: List<Record>,
    ) {
        init {
            require(
                schemaVersion ==
                    Schema,
            ) {
                "release acknowledgement schema is not current"
            }
            require(
                acknowledgements
                    .map(
                        Record::release,
                    )
                    .distinct()
                    .size ==
                    acknowledgements.size,
            ) {
                "release acknowledgement file contains duplicate release identities"
            }
        }
    }

    private val productHome =
        VexOwnedPath.requireRoot(
            productHome,
            "canonical VexLife Home",
        )

    @Synchronized
    fun read(): List<VexLifeReleaseInboxFamily.Acknowledgement> =
        readValue()
            .acknowledgements
            .map {
                record ->
                VexLifeReleaseInboxFamily.Acknowledgement(
                    identity =
                        record.release.value(),
                    standing =
                        record.standing,
                )
            }

    @Synchronized
    fun remember(
        identity: VexLifeReleaseIdentity.Value,
        standing: VexLifeReleaseInboxFamily.AcknowledgementStanding,
    ) {
        val coordinate =
            VexLifeReleaseIdentity.coordinate(
                identity,
            )
        val retained =
            readValue()
                .acknowledgements
                .filterNot {
                    it.release ==
                        coordinate
                }
        val next =
            (
                retained +
                    Record(
                        release = coordinate,
                        standing = standing,
                    )
            )
                .sortedWith(
                    compareBy(
                        {
                            it.release.channel.name
                        },
                        {
                            it.release.semanticVersion
                                ?.toString()
                                .orEmpty()
                        },
                        {
                            it.release.sourceSha.value
                        },
                    ),
                )

        VexDurableContractFile.write(
            ownedRoot = productHome,
            path = path(),
            value =
                Value(
                    schemaVersion = Schema,
                    acknowledgements = next,
                ),
        )
    }

    private fun readValue(): Value =
        VexDurableContractFile.read<Value>(
            ownedRoot = productHome,
            path = path(),
        )
            ?: Value(
                schemaVersion = Schema,
                acknowledgements = emptyList(),
            )

    private fun path(): Path =
        VexOwnedPath.resolveInside(
            productHome,
            RelativePath,
        )

    companion object {
        const val Schema =
            "vexlife-prod.release-acknowledgements/v1"

        const val RelativePath =
            "runtime/vexlife-prod/release-alerts/acknowledgements.json"
    }
}

// [VXG RealForever]
