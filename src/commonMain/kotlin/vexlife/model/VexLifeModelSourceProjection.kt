package vexlife.model

import kotlinx.serialization.Serializable
import vexlife.partner.VexLifePartnerConnectionProjection
import vexlife.partner.VexLifePartnerSupportQualification

/**
 * Read-only presentation projection for the human "Local / Partner" model-source filter.
 *
 * This object does not own model selection, model Use, provider connection, credential state,
 * runtime convergence, model discovery, or support qualification. It only projects already-owned
 * model offerings into one source-aware list.
 *
 * Permanent:
 *
 * UI SOURCE FILTER != DURABLE MODEL CHOICE
 * TARGETED MODEL -> DERIVES TARGETED SOURCE
 * SERVING MODEL  -> DERIVES SERVING SOURCE
 * PARTNER PROVIDER REF != MODEL SELECTION AUTHORITY
 */
object VexLifeModelSourceProjection {
    @Serializable
    enum class SourceKind {
        LOCAL,
        PARTNER,
    }

    @Serializable
    data class LocalOffering(
        val handle: String,
        val modelRef: String,
        val targeted: Boolean,
        val serving: Boolean,
    ) {
        init {
            requireRef(
                handle,
                "handle",
            )
            requireRef(
                modelRef,
                "modelRef",
            )
        }
    }

    @Serializable
    data class PartnerOffering(
        val handle: String,
        val providerRef: String,
        val modelRef: String,
        val targeted: Boolean,
        val serving: Boolean,
        val connection: VexLifePartnerConnectionProjection.Projection,
        val support: VexLifePartnerSupportQualification.Projection,
    ) {
        init {
            requireRef(
                handle,
                "handle",
            )
            requireRef(
                providerRef,
                "providerRef",
            )
            requireRef(
                modelRef,
                "modelRef",
            )
            require(
                connection.providerRef ==
                    providerRef &&
                    connection.modelRef ==
                    modelRef,
            ) {
                "Partner model source row requires connection truth for the exact provider/model"
            }
            require(
                support.providerRef ==
                    providerRef &&
                    support.modelRef ==
                    modelRef,
            ) {
                "Partner model source row requires support truth for the exact provider/model"
            }
        }
    }

    @Serializable
    data class Row(
        val handle: String,
        val modelRef: String,
        val sourceKind: SourceKind,
        val providerRef: String?,
        val targeted: Boolean,
        val serving: Boolean,
        val partnerConnectionStanding: VexLifePartnerConnectionProjection.Standing?,
        val partnerSupportStanding: VexLifePartnerSupportQualification.Standing?,
        val liveValidationDisclosureRequired: Boolean,
    ) {
        init {
            when (
                sourceKind
            ) {
                SourceKind.LOCAL ->
                    require(
                        providerRef ==
                            null &&
                            partnerConnectionStanding ==
                            null &&
                            partnerSupportStanding ==
                            null &&
                            !liveValidationDisclosureRequired,
                    ) {
                        "Local model source row may not carry Partner-only state"
                    }

                SourceKind.PARTNER ->
                    require(
                        providerRef !=
                            null &&
                            partnerConnectionStanding !=
                            null &&
                            partnerSupportStanding !=
                            null,
                    ) {
                        "Partner model source row requires provider/connection/support projection"
                    }
            }
        }
    }

    @Serializable
    data class Projection(
        val rows: List<Row>,
        val availableSources: List<SourceKind>,
        val targetedSource: SourceKind?,
        val servingSource: SourceKind?,
    ) {
        init {
            require(
                availableSources ==
                    SourceKind.entries.filter { source ->
                        rows.any {
                            it.sourceKind ==
                                source
                        }
                    },
            ) {
                "available source filters must be derived from current model rows"
            }
        }

        fun rowsFor(
            sourceKind: SourceKind,
        ): List<Row> =
            rows.filter {
                it.sourceKind ==
                    sourceKind
            }
    }

    fun project(
        local: List<LocalOffering>,
        partner: List<PartnerOffering>,
    ): Projection {
        val rows =
            local.map { offering ->
                Row(
                    handle = offering.handle,
                    modelRef = offering.modelRef,
                    sourceKind = SourceKind.LOCAL,
                    providerRef = null,
                    targeted = offering.targeted,
                    serving = offering.serving,
                    partnerConnectionStanding = null,
                    partnerSupportStanding = null,
                    liveValidationDisclosureRequired = false,
                )
            } +
                partner.map { offering ->
                    Row(
                        handle = offering.handle,
                        modelRef = offering.modelRef,
                        sourceKind = SourceKind.PARTNER,
                        providerRef = offering.providerRef,
                        targeted = offering.targeted,
                        serving = offering.serving,
                        partnerConnectionStanding =
                            offering.connection.standing,
                        partnerSupportStanding =
                            offering.support.standing,
                        liveValidationDisclosureRequired =
                            offering.support.liveValidationDisclosureRequired,
                    )
                }

        require(
            rows.map {
                it.handle
            }
                .distinct()
                .size ==
                rows.size,
        ) {
            "Models source projection requires globally unique current row handles"
        }

        val targeted =
            rows.filter {
                it.targeted
            }
        require(
            targeted.size <=
                1,
        ) {
            "Models source projection may not invent multiple targeted models"
        }

        val serving =
            rows.filter {
                it.serving
            }
        require(
            serving.size <=
                1,
        ) {
            "Models source projection may not invent multiple serving models"
        }

        return Projection(
            rows = rows,
            availableSources =
                SourceKind.entries.filter { source ->
                    rows.any {
                        it.sourceKind ==
                            source
                    }
                },
            targetedSource =
                targeted
                    .singleOrNull()
                    ?.sourceKind,
            servingSource =
                serving
                    .singleOrNull()
                    ?.sourceKind,
        )
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
            "$field must be one bounded non-blank reference"
        }
    }
}

// [VXG RealForever]
