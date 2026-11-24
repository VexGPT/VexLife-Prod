package vexlife.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import vexlife.partner.VexLifePartnerConnectionProjection

class VexLifePartnerEffectiveModelCapabilitiesTest {
    private fun connection(
        providerRef: String = "provider.partner.fixture",
        modelRef: String = "model.partner.fixture",
        credential: VexLifePartnerConnectionProjection.CredentialStanding =
            VexLifePartnerConnectionProjection.CredentialStanding.ACCEPTED,
        account: VexLifePartnerConnectionProjection.AccountStanding =
            VexLifePartnerConnectionProjection.AccountStanding.USABLE,
        service: VexLifePartnerConnectionProjection.ServiceStanding =
            VexLifePartnerConnectionProjection.ServiceStanding.READY,
        modelAccess: VexLifePartnerConnectionProjection.ModelAccessStanding =
            VexLifePartnerConnectionProjection.ModelAccessStanding.AVAILABLE,
        invocation: VexLifePartnerConnectionProjection.InvocationStanding =
            VexLifePartnerConnectionProjection.InvocationStanding.NEVER_QUALIFIED,
    ): VexLifePartnerConnectionProjection.Projection =
        VexLifePartnerConnectionProjection.project(
            VexLifePartnerConnectionProjection.Observation(
                providerRef = providerRef,
                modelRef = modelRef,
                credential = credential,
                account = account,
                service = service,
                modelAccess = modelAccess,
                invocation = invocation,
            ),
        )

    private fun represented(
        qualificationRef: String? =
            "proof.partner.capability.fixture",
    ): VexLifePartnerEffectiveModelCapabilities.CapabilityEvidence =
        VexLifePartnerEffectiveModelCapabilities.CapabilityEvidence(
            represented = true,
            qualificationRef = qualificationRef,
        )

    private fun absent():
        VexLifePartnerEffectiveModelCapabilities.CapabilityEvidence =
        VexLifePartnerEffectiveModelCapabilities.CapabilityEvidence(
            represented = false,
            qualificationRef = null,
        )

    private fun profile(
        providerRef: String = "provider.partner.fixture",
        modelRef: String = "model.partner.fixture",
        vexReach: VexLifePartnerEffectiveModelCapabilities.CapabilityEvidence =
            represented(),
        tools: VexLifePartnerEffectiveModelCapabilities.CapabilityEvidence =
            represented(),
        reasoning: VexLifePartnerEffectiveModelCapabilities.CapabilityEvidence =
            absent(),
        visual: VexLifePartnerEffectiveModelCapabilities.CapabilityEvidence =
            VexLifePartnerEffectiveModelCapabilities.CapabilityEvidence(
                represented = true,
                qualificationRef = null,
            ),
    ): VexLifePartnerEffectiveModelCapabilities.ProfileEvidence =
        VexLifePartnerEffectiveModelCapabilities.ProfileEvidence(
            providerRef = providerRef,
            modelRef = modelRef,
            profileRef = "profile.partner.fixture",
            vexReach = vexReach,
            providerNativeTools = tools,
            reasoningEffortControl = reasoning,
            visualInput = visual,
        )

    @Test
    fun contractQualifiedCapabilityMayBeAvailableBeforeAnyLiveProviderInvocation() {
        val projection =
            VexLifePartnerEffectiveModelCapabilities.project(
                profile =
                    profile(),
                connection =
                    connection(
                        invocation =
                            VexLifePartnerConnectionProjection.InvocationStanding.NEVER_QUALIFIED,
                    ),
            )

        assertEquals(
            VexLifeEffectiveModelCapabilities.Standing.AVAILABLE,
            projection.vexReach,
        )
        assertEquals(
            VexLifeEffectiveModelCapabilities.Standing.AVAILABLE,
            projection.providerNativeTools,
        )
        assertEquals(
            VexLifeEffectiveModelCapabilities.Standing.NOT_APPLICABLE,
            projection.reasoningEffortControl,
        )
        assertEquals(
            VexLifeEffectiveModelCapabilities.Standing.HELD,
            projection.visualInput,
        )
    }

    @Test
    fun lastInvocationFailureDoesNotEraseStillCurrentConnectionCapabilityFacts() {
        val projection =
            VexLifePartnerEffectiveModelCapabilities.project(
                profile =
                    profile(),
                connection =
                    connection(
                        invocation =
                            VexLifePartnerConnectionProjection.InvocationStanding.LAST_INVOCATION_FAILED,
                    ),
            )

        assertEquals(
            VexLifeEffectiveModelCapabilities.Standing.AVAILABLE,
            projection.vexReach,
        )
        assertEquals(
            VexLifePartnerConnectionProjection.Standing.LAST_INVOCATION_FAILED,
            projection.connectionStanding,
        )
    }

    @Test
    fun rateLimitedConnectionHoldsRepresentedQualifiedCapabilities() {
        val projection =
            VexLifePartnerEffectiveModelCapabilities.project(
                profile =
                    profile(),
                connection =
                    connection(
                        service =
                            VexLifePartnerConnectionProjection.ServiceStanding.RATE_LIMITED,
                    ),
            )

        assertEquals(
            VexLifeEffectiveModelCapabilities.Standing.HELD,
            projection.vexReach,
        )
        assertEquals(
            VexLifeEffectiveModelCapabilities.Standing.HELD,
            projection.providerNativeTools,
        )
    }

    @Test
    fun representedButUnqualifiedCapabilityStaysHeld() {
        val projection =
            VexLifePartnerEffectiveModelCapabilities.project(
                profile =
                    profile(
                        reasoning =
                            represented(
                                qualificationRef = null,
                            ),
                    ),
                connection =
                    connection(),
            )

        assertEquals(
            VexLifeEffectiveModelCapabilities.Standing.HELD,
            projection.reasoningEffortControl,
        )
    }

    @Test
    fun absentRepresentationIsNotApplicableAndCannotCarryQualification() {
        assertFailsWith<IllegalArgumentException> {
            VexLifePartnerEffectiveModelCapabilities.CapabilityEvidence(
                represented = false,
                qualificationRef = "proof.should-not-exist",
            )
        }

        val projection =
            VexLifePartnerEffectiveModelCapabilities.project(
                profile =
                    profile(
                        reasoning =
                            absent(),
                    ),
                connection =
                    connection(),
            )

        assertEquals(
            VexLifeEffectiveModelCapabilities.Standing.NOT_APPLICABLE,
            projection.reasoningEffortControl,
        )
    }

    @Test
    fun visualInputNeedsItsOwnProductQualificationRatherThanModelMarketing() {
        val held =
            VexLifePartnerEffectiveModelCapabilities.project(
                profile =
                    profile(
                        visual =
                            represented(
                                qualificationRef = null,
                            ),
                    ),
                connection =
                    connection(),
            )
        assertEquals(
            VexLifeEffectiveModelCapabilities.Standing.HELD,
            held.visualInput,
        )

        val qualified =
            VexLifePartnerEffectiveModelCapabilities.project(
                profile =
                    profile(
                        visual =
                            represented(
                                qualificationRef =
                                    "proof.partner.visual-input.fixture",
                            ),
                    ),
                connection =
                    connection(),
            )
        assertEquals(
            VexLifeEffectiveModelCapabilities.Standing.AVAILABLE,
            qualified.visualInput,
        )
    }

    @Test
    fun connectionMustBelongToTheExactProviderModelProfile() {
        assertFailsWith<IllegalArgumentException> {
            VexLifePartnerEffectiveModelCapabilities.project(
                profile =
                    profile(
                        providerRef = "provider.partner.a",
                    ),
                connection =
                    connection(
                        providerRef = "provider.partner.b",
                    ),
            )
        }

        assertFailsWith<IllegalArgumentException> {
            VexLifePartnerEffectiveModelCapabilities.project(
                profile =
                    profile(
                        modelRef = "model.partner.a",
                    ),
                connection =
                    connection(
                        modelRef = "model.partner.b",
                    ),
            )
        }
    }

    @Test
    fun billingOrCredentialHoldsDoNotBecomeCapabilityAvailability() {
        val billing =
            VexLifePartnerEffectiveModelCapabilities.project(
                profile =
                    profile(),
                connection =
                    connection(
                        account =
                            VexLifePartnerConnectionProjection.AccountStanding.BILLING_HELD,
                    ),
            )
        assertEquals(
            VexLifeEffectiveModelCapabilities.Standing.HELD,
            billing.vexReach,
        )

        val credential =
            VexLifePartnerEffectiveModelCapabilities.project(
                profile =
                    profile(),
                connection =
                    connection(
                        credential =
                            VexLifePartnerConnectionProjection.CredentialStanding.REJECTED,
                    ),
            )
        assertEquals(
            VexLifeEffectiveModelCapabilities.Standing.HELD,
            credential.vexReach,
        )
    }
}

// [VXG RealForever]
