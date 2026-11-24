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
