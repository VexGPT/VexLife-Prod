package vexlife.partner

import kotlinx.serialization.Serializable

/**
 * Read-only support qualification projection for one external Partner provider/model relationship.
 *
 * This projection does not own provider connection, credentials, billing, model availability,
 * inference, model selection, release authority, or presentation wording.
 *
 * It answers only which proof threshold has been crossed:
 *
 * P0 documented contract fidelity
 * + P1 adapter/wire conformance
 * + P2 Vex relationship conformance
 * -> CONTRACT_PROVEN_LIVE_UNQUALIFIED
 *
 * + one accepted exact live provider/model qualification receipt
 * -> LIVE_QUALIFIED
 *
 * Permanent:
 * CONTRACT_PROVEN != LIVE_QUALIFIED
 * LIVE_UNQUALIFIED != BROKEN
 * SUPPORT PROJECTION != RELEASE AUTHORITY
 * SUPPORT PROJECTION != MODEL USE AUTHORITY
 */
object VexLifePartnerSupportQualification {
    @Serializable
    enum class Standing {
        HELD,
        CONTRACT_PROVEN_LIVE_UNQUALIFIED,
        LIVE_QUALIFIED,
    }

    @Serializable
    data class Evidence(
        val providerRef: String,
        val modelRef: String,
        val contractFixtureProofRef: String?,
        val adapterConformanceProofRef: String?,
        val vexRelationshipConformanceProofRef: String?,
        val liveQualificationReceiptRef: String? = null,
    ) {
        init {
            requireRef(
                providerRef,
                "providerRef",
            )
            requireRef(
                modelRef,
                "modelRef",
            )
            requireOptionalRef(
                contractFixtureProofRef,
                "contractFixtureProofRef",
            )
            requireOptionalRef(
                adapterConformanceProofRef,
                "adapterConformanceProofRef",
            )
            requireOptionalRef(
                vexRelationshipConformanceProofRef,
                "vexRelationshipConformanceProofRef",
            )
            requireOptionalRef(
                liveQualificationReceiptRef,
                "liveQualificationReceiptRef",
            )

            require(
                liveQualificationReceiptRef == null ||
                    contractProofComplete,
            ) {
                "live Partner qualification cannot bypass contract/adapter/Vex conformance proof"
            }
        }

        val contractProofComplete: Boolean
            get() =
                contractFixtureProofRef != null &&
                    adapterConformanceProofRef != null &&
                    vexRelationshipConformanceProofRef != null
    }

    @Serializable
    data class Projection(
        val providerRef: String,
        val modelRef: String,
        val standing: Standing,
        /**
         * True only when Product support proof is complete but an accepted live provider/model
         * qualification receipt does not yet exist.
         *
         * Presentation may use this to project a small accessible information disclosure.
         * It does not prescribe words or UI geometry.
         */
        val liveValidationDisclosureRequired: Boolean,
        val liveQualificationReceiptRef: String?,
    )

    fun project(
        evidence: Evidence,
    ): Projection {
        val standing =
            when {
                evidence.liveQualificationReceiptRef != null ->
                    Standing.LIVE_QUALIFIED

                evidence.contractProofComplete ->
                    Standing.CONTRACT_PROVEN_LIVE_UNQUALIFIED

                else ->
                    Standing.HELD
            }

        return Projection(
            providerRef = evidence.providerRef,
            modelRef = evidence.modelRef,
            standing = standing,
            liveValidationDisclosureRequired =
                standing ==
                    Standing.CONTRACT_PROVEN_LIVE_UNQUALIFIED,
            liveQualificationReceiptRef =
                evidence.liveQualificationReceiptRef,
        )
    }

    private fun requireOptionalRef(
        value: String?,
        field: String,
    ) {
        value
            ?.let {
                requireRef(
                    it,
                    field,
                )
            }
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
