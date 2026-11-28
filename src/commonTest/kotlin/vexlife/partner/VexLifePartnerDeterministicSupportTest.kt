package vexlife.partner

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VexLifePartnerDeterministicSupportTest {
    @Test
    fun everySourceOwnedProfileIsContractProvenButNotLiveQualified() {
        val profiles =
            VexLifePartnerModelProfileCatalog.definitions()
        val support =
            VexLifePartnerDeterministicSupport.projections()

        assertEquals(
            profiles.map {
                it.providerRef to it.modelRef
            },
            support.map {
                it.providerRef to it.modelRef
            },
        )
        support.forEach {
            projection ->
            assertEquals(
                VexLifePartnerSupportQualification.Standing.CONTRACT_PROVEN_LIVE_UNQUALIFIED,
                projection.standing,
            )
            assertTrue(
                projection.liveValidationDisclosureRequired,
            )
            assertNull(
                projection.liveQualificationReceiptRef,
            )
            assertNull(
                projection.liveQualificationCredentialRef,
            )
            assertNull(
                projection.liveQualificationAccountScope,
            )
        }
    }

    @Test
    fun deterministicSupportUsesAcceptedEvidenceRefsOnly() {
        val refs =
            listOf(
                VexLifePartnerDeterministicSupport.ContractFixtureProofRef,
                VexLifePartnerDeterministicSupport.AdapterConformanceProofRef,
                VexLifePartnerDeterministicSupport.VexRelationshipConformanceProofRef,
            )

        assertEquals(
            listOf(
                "github.pr.vexlife-prod.578.review.6033378878",
                "github.pr.vexlife-prod.578.review.6033378878",
                "github.pr.vexlife-prod.614.review.6047658156",
            ),
            refs,
        )
        assertTrue(
            refs.all {
                it.isNotBlank() &&
                    it.length <=
                    512 &&
                    it.none(
                        Char::isWhitespace,
                    )
            },
        )
        assertFalse(
            refs.any {
                "live" in
                    it.lowercase()
            },
        )
    }

    @Test
    fun exactProfileProjectionDoesNotCreateAnotherInventory() {
        val profile =
            VexLifePartnerModelProfileCatalog.AnthropicClaudeSonnet55
        val projection =
            VexLifePartnerDeterministicSupport.projection(
                profile,
            )

        assertEquals(
            profile.providerRef,
            projection.providerRef,
        )
        assertEquals(
            profile.modelRef,
            projection.modelRef,
        )
        assertEquals(
            VexLifePartnerSupportQualification.Standing.CONTRACT_PROVEN_LIVE_UNQUALIFIED,
            projection.standing,
        )
    }
}

// [VXG RealForever]
