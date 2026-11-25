package vexlife.partner

import kotlin.test.Test
import kotlin.test.assertEquals

class VexLifePartnerConnectionProjectionTest {
    private fun projection(
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
                providerRef = "provider.openai.api",
                modelRef = "model.partner.fixture",
                credential = credential,
                account = account,
                service = service,
                modelAccess = modelAccess,
                invocation = invocation,
            ),
        )

    @Test
    fun missingAndRejectedCredentialsRemainDifferentTruths() {
        assertEquals(
            VexLifePartnerConnectionProjection.Standing.NEEDS_CREDENTIAL,
            projection(
                credential =
                    VexLifePartnerConnectionProjection.CredentialStanding.MISSING,
            ).standing,
        )
        assertEquals(
            VexLifePartnerConnectionProjection.Standing.CREDENTIAL_REJECTED,
            projection(
                credential =
                    VexLifePartnerConnectionProjection.CredentialStanding.REJECTED,
            ).standing,
        )
    }

    @Test
    fun billingAndPermissionHoldsDoNotCollapseIntoCredentialFailure() {
        assertEquals(
            VexLifePartnerConnectionProjection.Standing.BILLING_HELD,
            projection(
                account =
                    VexLifePartnerConnectionProjection.AccountStanding.BILLING_HELD,
            ).standing,
        )
        assertEquals(
            VexLifePartnerConnectionProjection.Standing.PERMISSION_HELD,
            projection(
                account =
                    VexLifePartnerConnectionProjection.AccountStanding.PERMISSION_HELD,
            ).standing,
        )
    }

    @Test
    fun modelAccessLifecycleKeepsNoAccessDeprecatedAndRetiredDistinct() {
        assertEquals(
            VexLifePartnerConnectionProjection.Standing.MODEL_NOT_ACCESSIBLE,
            projection(
                modelAccess =
                    VexLifePartnerConnectionProjection.ModelAccessStanding.NO_ACCESS,
            ).standing,
        )
        assertEquals(
            VexLifePartnerConnectionProjection.Standing.MODEL_DEPRECATED,
            projection(
                modelAccess =
                    VexLifePartnerConnectionProjection.ModelAccessStanding.DEPRECATED,
            ).standing,
        )
        assertEquals(
            VexLifePartnerConnectionProjection.Standing.MODEL_RETIRED,
            projection(
                modelAccess =
                    VexLifePartnerConnectionProjection.ModelAccessStanding.RETIRED,
            ).standing,
        )
    }

    @Test
    fun serviceRateLimitOverloadAndOutageRemainDifferent() {
        assertEquals(
            VexLifePartnerConnectionProjection.Standing.RATE_LIMITED,
            projection(
                service =
                    VexLifePartnerConnectionProjection.ServiceStanding.RATE_LIMITED,
            ).standing,
        )
        assertEquals(
            VexLifePartnerConnectionProjection.Standing.PROVIDER_OVERLOADED,
            projection(
                service =
                    VexLifePartnerConnectionProjection.ServiceStanding.OVERLOADED,
            ).standing,
        )
        assertEquals(
            VexLifePartnerConnectionProjection.Standing.PROVIDER_UNAVAILABLE,
            projection(
                service =
                    VexLifePartnerConnectionProjection.ServiceStanding.UNAVAILABLE,
            ).standing,
        )
    }

    @Test
    fun unresolvedCurrentnessIsCheckingEvenIfAnOlderInvocationWasQualified() {
        assertEquals(
            VexLifePartnerConnectionProjection.Standing.CHECKING,
            projection(
                account =
                    VexLifePartnerConnectionProjection.AccountStanding.UNKNOWN,
                invocation =
                    VexLifePartnerConnectionProjection.InvocationStanding.QUALIFIED,
            ).standing,
        )
        assertEquals(
            VexLifePartnerConnectionProjection.Standing.CHECKING,
            projection(
                credential =
                    VexLifePartnerConnectionProjection.CredentialStanding.PRESENT_UNVERIFIED,
            ).standing,
        )
    }

    @Test
    fun fullyUsableConnectionDistinguishesFirstAttemptQualificationAndLastFailure() {
        assertEquals(
            VexLifePartnerConnectionProjection.Standing.READY_TO_ATTEMPT,
            projection().standing,
        )
        assertEquals(
            VexLifePartnerConnectionProjection.Standing.QUALIFIED_SERVING,
            projection(
                invocation =
                    VexLifePartnerConnectionProjection.InvocationStanding.QUALIFIED,
            ).standing,
        )
        assertEquals(
            VexLifePartnerConnectionProjection.Standing.LAST_INVOCATION_FAILED,
            projection(
                invocation =
                    VexLifePartnerConnectionProjection.InvocationStanding.LAST_INVOCATION_FAILED,
            ).standing,
        )
    }

    @Test
    fun projectionPreservesExactProviderAndModelCoordinates() {
        val projection =
            projection()

        assertEquals(
            "provider.openai.api",
            projection.providerRef,
        )
        assertEquals(
            "model.partner.fixture",
            projection.modelRef,
        )
    }
}

// [VXG RealForever]
