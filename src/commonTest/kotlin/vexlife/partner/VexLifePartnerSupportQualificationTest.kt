package vexlife.partner

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VexLifePartnerSupportQualificationTest {
    private fun evidence(
        contract: String? = null,
        adapter: String? = null,
        relationship: String? = null,
        live: String? = null,
    ): VexLifePartnerSupportQualification.Evidence =
        VexLifePartnerSupportQualification.Evidence(
            providerRef = "provider.openai.api",
            modelRef = "model.partner.fixture",
            contractFixtureProofRef = contract,
            adapterConformanceProofRef = adapter,
            vexRelationshipConformanceProofRef = relationship,
            liveQualificationReceiptRef = live,
            liveQualificationCredentialRef =
                "credential.partner.fixture".takeIf {
                    live != null
                },
            liveQualificationAccountScope =
                VexLifePartnerAccountScope.Selection(
                    providerRef = "provider.openai.api",
                )
                    .takeIf {
                        live != null
                    },
        )

    @Test
    fun incompleteDeterministicProofRemainsHeld() {
        val projection =
            VexLifePartnerSupportQualification.project(
                evidence(
                    contract = "proof.partner.p0",
                    adapter = "proof.partner.p1",
                ),
            )

        assertEquals(
            VexLifePartnerSupportQualification.Standing.HELD,
            projection.standing,
        )
        assertFalse(
            projection.liveValidationDisclosureRequired,
        )
    }

    @Test
    fun completeDeterministicProofWithoutLiveReceiptRequiresDisclosure() {
        val projection =
            VexLifePartnerSupportQualification.project(
                evidence(
                    contract = "proof.partner.p0",
                    adapter = "proof.partner.p1",
                    relationship = "proof.partner.p2",
                ),
            )

        assertEquals(
            VexLifePartnerSupportQualification.Standing.CONTRACT_PROVEN_LIVE_UNQUALIFIED,
            projection.standing,
        )
        assertTrue(
            projection.liveValidationDisclosureRequired,
        )
        assertEquals(
            null,
            projection.liveQualificationReceiptRef,
        )
    }

    @Test
    fun exactLiveQualificationUpgradesWithoutChangingProviderOrModelIdentity() {
        val projection =
            VexLifePartnerSupportQualification.project(
                evidence(
                    contract = "proof.partner.p0",
                    adapter = "proof.partner.p1",
                    relationship = "proof.partner.p2",
                    live = "receipt.partner.live.openai.fixture",
                ),
            )

        assertEquals(
            VexLifePartnerSupportQualification.Standing.LIVE_QUALIFIED,
            projection.standing,
        )
        assertFalse(
            projection.liveValidationDisclosureRequired,
        )
        assertEquals(
            "provider.openai.api",
            projection.providerRef,
        )
        assertEquals(
            "model.partner.fixture",
            projection.modelRef,
        )
        assertEquals(
            "receipt.partner.live.openai.fixture",
            projection.liveQualificationReceiptRef,
        )
        assertEquals(
            "credential.partner.fixture",
            projection.liveQualificationCredentialRef,
        )
        assertEquals(
            "provider.openai.api",
            projection.liveQualificationAccountScope?.providerRef,
        )
    }

    @Test
    fun contractProvenProjectionAcceptsOneExactCurrentLiveQualification() {
        val current =
            VexLifePartnerSupportQualification.project(
                evidence(
                    contract = "proof.partner.p0",
                    adapter = "proof.partner.p1",
                    relationship = "proof.partner.p2",
                ),
            )

        val qualified =
            VexLifePartnerSupportQualification.acceptLiveQualification(
                current = current,
                live =
                    VexLifePartnerSupportQualification.LiveQualification(
                        providerRef = "provider.openai.api",
                        modelRef = "model.partner.fixture",
                        credentialRef = "credential.partner.current",
                        accountScope =
                            VexLifePartnerAccountScope.Selection(
                                providerRef = "provider.openai.api",
                            ),
                        receiptRef = "receipt.partner.live.current",
                    ),
            )

        assertEquals(
            VexLifePartnerSupportQualification.Standing.LIVE_QUALIFIED,
            qualified.standing,
        )
        assertFalse(
            qualified.liveValidationDisclosureRequired,
        )
        assertEquals(
            "credential.partner.current",
            qualified.liveQualificationCredentialRef,
        )
        assertEquals(
            "receipt.partner.live.current",
            qualified.liveQualificationReceiptRef,
        )
    }

    @Test
    fun liveQualificationCannotUpgradeAnotherProviderOrModel() {
        val current =
            VexLifePartnerSupportQualification.project(
                evidence(
                    contract = "proof.partner.p0",
                    adapter = "proof.partner.p1",
                    relationship = "proof.partner.p2",
                ),
            )

        assertFailsWith<IllegalArgumentException> {
            VexLifePartnerSupportQualification.acceptLiveQualification(
                current = current,
                live =
                    VexLifePartnerSupportQualification.LiveQualification(
                        providerRef = VexLifePartnerProviderCatalog.KimiInternationalRef,
                        modelRef = "model.partner.fixture",
                        credentialRef = "credential.partner.other",
                        accountScope =
                            VexLifePartnerAccountScope.Selection(
                                providerRef = VexLifePartnerProviderCatalog.KimiInternationalRef,
                            ),
                        receiptRef = "receipt.partner.live.other",
                    ),
            )
        }
    }

    @Test
    fun liveReceiptCannotBypassDeterministicProofBody() {
        assertFailsWith<IllegalArgumentException> {
            evidence(
                live = "receipt.partner.live.openai.fixture",
            )
        }
    }

    @Test
    fun supportEvidenceRequiresBoundedReferenceShapedValues() {
        assertFailsWith<IllegalArgumentException> {
            VexLifePartnerSupportQualification.Evidence(
                providerRef = "provider.openai.api",
                modelRef = "model.partner.fixture",
                contractFixtureProofRef = "proof partner p0",
                adapterConformanceProofRef = "proof.partner.p1",
                vexRelationshipConformanceProofRef = "proof.partner.p2",
            )
        }
    }
}

// [VXG RealForever]
