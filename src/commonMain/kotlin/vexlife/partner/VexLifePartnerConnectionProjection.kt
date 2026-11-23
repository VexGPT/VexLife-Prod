package vexlife.partner

import kotlinx.serialization.Serializable

/**
 * Read-only current connection projection for one Partner provider/model relationship.
 *
 * Future credential/network/account owners supply the observations. This projection does not store a
 * secret, perform a request, decide model support, grant model-use authority, or own retry policy.
 *
 * Keeping the dimensions separate prevents one generic "API error" from hiding materially different
 * current truths such as invalid credentials, exhausted billing, ordinary rate limiting, overload,
 * provider outage, or model access loss.
 */
object VexLifePartnerConnectionProjection {
    @Serializable
    enum class CredentialStanding {
        MISSING,
        PRESENT_UNVERIFIED,
        ACCEPTED,
        REJECTED,
    }

    @Serializable
    enum class AccountStanding {
        UNKNOWN,
        USABLE,
        BILLING_HELD,
        PERMISSION_HELD,
    }

    @Serializable
    enum class ServiceStanding {
        UNKNOWN,
        READY,
        RATE_LIMITED,
        OVERLOADED,
        UNAVAILABLE,
    }

    @Serializable
    enum class ModelAccessStanding {
        UNKNOWN,
        AVAILABLE,
        NO_ACCESS,
        DEPRECATED,
        RETIRED,
    }

    @Serializable
    enum class InvocationStanding {
        NEVER_QUALIFIED,
        QUALIFIED,
        LAST_INVOCATION_FAILED,
    }

    @Serializable
    enum class Standing {
        NEEDS_CREDENTIAL,
        CHECKING,
        CREDENTIAL_REJECTED,
        BILLING_HELD,
        PERMISSION_HELD,
        MODEL_NOT_ACCESSIBLE,
        MODEL_DEPRECATED,
        MODEL_RETIRED,
        RATE_LIMITED,
        PROVIDER_OVERLOADED,
        PROVIDER_UNAVAILABLE,
        READY_TO_ATTEMPT,
        QUALIFIED_SERVING,
        LAST_INVOCATION_FAILED,
    }

    @Serializable
    data class Observation(
        val providerRef: String,
        val modelRef: String,
        val credential: CredentialStanding,
        val account: AccountStanding,
        val service: ServiceStanding,
        val modelAccess: ModelAccessStanding,
        val invocation: InvocationStanding,
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
        }
    }

    @Serializable
    data class Projection(
        val providerRef: String,
        val modelRef: String,
        val credential: CredentialStanding,
        val account: AccountStanding,
        val service: ServiceStanding,
        val modelAccess: ModelAccessStanding,
        val invocation: InvocationStanding,
        val standing: Standing,
    )

    fun project(
        observation: Observation,
    ): Projection =
        Projection(
            providerRef = observation.providerRef,
            modelRef = observation.modelRef,
            credential = observation.credential,
            account = observation.account,
            service = observation.service,
            modelAccess = observation.modelAccess,
            invocation = observation.invocation,
            standing =
                standing(
                    observation,
                ),
        )

    private fun standing(
        observation: Observation,
    ): Standing =
        when {
            observation.credential ==
                CredentialStanding.MISSING ->
                Standing.NEEDS_CREDENTIAL

            observation.credential ==
                CredentialStanding.REJECTED ->
                Standing.CREDENTIAL_REJECTED

            observation.account ==
                AccountStanding.BILLING_HELD ->
                Standing.BILLING_HELD

            observation.account ==
                AccountStanding.PERMISSION_HELD ->
                Standing.PERMISSION_HELD

            observation.modelAccess ==
                ModelAccessStanding.NO_ACCESS ->
                Standing.MODEL_NOT_ACCESSIBLE

            observation.modelAccess ==
                ModelAccessStanding.DEPRECATED ->
                Standing.MODEL_DEPRECATED

            observation.modelAccess ==
                ModelAccessStanding.RETIRED ->
                Standing.MODEL_RETIRED

            observation.service ==
                ServiceStanding.RATE_LIMITED ->
                Standing.RATE_LIMITED

            observation.service ==
                ServiceStanding.OVERLOADED ->
                Standing.PROVIDER_OVERLOADED

            observation.service ==
                ServiceStanding.UNAVAILABLE ->
                Standing.PROVIDER_UNAVAILABLE

            observation.credential !=
                CredentialStanding.ACCEPTED ||
                observation.account !=
                AccountStanding.USABLE ||
                observation.service !=
                ServiceStanding.READY ||
                observation.modelAccess !=
                ModelAccessStanding.AVAILABLE ->
                Standing.CHECKING

            observation.invocation ==
                InvocationStanding.QUALIFIED ->
                Standing.QUALIFIED_SERVING

            observation.invocation ==
                InvocationStanding.LAST_INVOCATION_FAILED ->
                Standing.LAST_INVOCATION_FAILED

            else ->
                Standing.READY_TO_ATTEMPT
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
