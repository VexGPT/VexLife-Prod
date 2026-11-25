package vexlife.partner

/**
 * Source-owned current deterministic support evidence for built-in Partner model profiles.
 *
 * This owner does not perform provider access, inspect credentials, grant model-use authority, or
 * claim live provider qualification. It only binds the current Product-supported profile catalog to
 * already-accepted deterministic proof that remains byte-applicable to the current source.
 *
 * Permanent:
 *
 * DETERMINISTIC SUPPORT EVIDENCE != LIVE PROVIDER QUALIFICATION
 * ACCEPTED PROOF REF != MODEL USE AUTHORITY
 * SOURCE-SUPPORTED PROFILE != CURRENT ACCOUNT/MODEL ACCESS
 */
object VexLifePartnerDeterministicSupport {
    const val ContractFixtureProofRef =
        "github.pr.vexlife-prod.578.review.6033378878"

    const val AdapterConformanceProofRef =
        "github.pr.vexlife-prod.578.review.6033378878"

    const val VexRelationshipConformanceProofRef =
        "github.pr.vexlife-prod.614.review.6047658156"

    fun projections(): List<VexLifePartnerSupportQualification.Projection> =
        VexLifePartnerModelProfileCatalog
            .definitions()
            .map(
                ::projection,
            )

    fun projection(
        profile: VexLifePartnerModelProfileCatalog.Definition,
    ): VexLifePartnerSupportQualification.Projection =
        VexLifePartnerSupportQualification.project(
            VexLifePartnerSupportQualification.Evidence(
                providerRef = profile.providerRef,
                modelRef = profile.modelRef,
                contractFixtureProofRef = ContractFixtureProofRef,
                adapterConformanceProofRef = AdapterConformanceProofRef,
                vexRelationshipConformanceProofRef = VexRelationshipConformanceProofRef,
            ),
        )
}

// [VXG RealForever]
