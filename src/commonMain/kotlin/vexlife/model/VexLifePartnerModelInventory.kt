package vexlife.model

import kotlinx.serialization.Serializable
import vexlife.partner.VexLifePartnerConnectionProjection
import vexlife.partner.VexLifePartnerModelProfileCatalog
import vexlife.partner.VexLifePartnerSupportQualification

/**
 * Read-only Partner model inventory adapter for Settings -> Models.
 *
 * Source-supported profiles come only from [VexLifePartnerModelProfileCatalog]. Provider/account
 * observations may change standing for those rows; they may not create Product model profiles.
 */
object VexLifePartnerModelInventory {
    @Serializable
    data class Coordinate(
        val providerRef: String,
        val modelRef: String,
    ) {
        init {
            requireRef(providerRef, "providerRef")
            requireRef(modelRef, "modelRef")
        }
    }

    data class Input(
        val connections: List<VexLifePartnerConnectionProjection.Projection>,
        val support: List<VexLifePartnerSupportQualification.Projection>,
        val targeted: Coordinate? = null,
        val serving: Coordinate? = null,
    )

    fun offerings(
        input: Input,
    ): List<VexLifeModelSourceProjection.PartnerOffering> {
        val connectionByCoordinate =
            uniqueByCoordinate(
                values = input.connections,
                providerRef = { it.providerRef },
                modelRef = { it.modelRef },
                label = "Partner connection",
            )
        val supportByCoordinate =
            uniqueByCoordinate(
                values = input.support,
                providerRef = { it.providerRef },
                modelRef = { it.modelRef },
                label = "Partner support",
            )

        val catalog =
            VexLifePartnerModelProfileCatalog.definitions()
        val supportedCoordinates =
            catalog
                .map {
                    Coordinate(
                        providerRef = it.providerRef,
                        modelRef = it.modelRef,
                    )
                }
                .toSet()

        input.targeted
            ?.let {
                require(
                    it in supportedCoordinates,
                ) {
                    "targeted Partner model must be one source-supported profile"
                }
            }
        input.serving
            ?.let {
                require(
                    it in supportedCoordinates,
                ) {
                    "serving Partner model must be one source-supported profile"
                }
            }

        return catalog.map { definition ->
            val coordinate =
                Coordinate(
                    providerRef = definition.providerRef,
                    modelRef = definition.modelRef,
                )
            val connection =
                requireNotNull(
                    connectionByCoordinate[
                        coordinate
                    ],
                ) {
                    "Partner inventory requires current connection truth for " +
                        definition.profileRef
                }
            val support =
                requireNotNull(
                    supportByCoordinate[
                        coordinate
                    ],
                ) {
                    "Partner inventory requires current support truth for " +
                        definition.profileRef
                }

            VexLifeModelSourceProjection.PartnerOffering(
                handle = definition.profileRef,
                providerRef = definition.providerRef,
                modelRef = definition.modelRef,
                targeted =
                    input.targeted ==
                        coordinate,
                serving =
                    input.serving ==
                        coordinate,
                connection = connection,
                support = support,
            )
        }
    }

    private fun <T> uniqueByCoordinate(
        values: List<T>,
        providerRef: (T) -> String,
        modelRef: (T) -> String,
        label: String,
    ): Map<Coordinate, T> {
        val result =
            linkedMapOf<Coordinate, T>()
        values.forEach { value ->
            val coordinate =
                Coordinate(
                    providerRef = providerRef(value),
                    modelRef = modelRef(value),
                )
            require(
                result.put(
                    coordinate,
                    value,
                ) ==
                    null,
            ) {
                label +
                    " truth is duplicated for " +
                    coordinate
            }
        }
        return result
    }

    private fun requireRef(
        value: String,
        field: String,
    ) {
        require(
            value.isNotBlank() &&
                value.length <=
                512 &&
                value.none(
                    Char::isWhitespace,
                ),
        ) {
            field +
                " must be one bounded non-blank reference"
        }
    }
}

// [VXG RealForever]
