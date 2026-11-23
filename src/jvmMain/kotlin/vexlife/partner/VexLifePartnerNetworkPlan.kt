package vexlife.partner

import java.nio.charset.StandardCharsets
import java.security.MessageDigest

/**
 * No-effect request plan for one built-in Partner inference.
 *
 * The plan binds source-owned provider topology to an opaque credential coordinate and an
 * already-formed provider request body. It performs no network call and never resolves a secret.
 *
 * Permanent:
 *
 * NETWORK PLAN != NETWORK EFFECT
 * CREDENTIAL REF != CREDENTIAL VALUE
 * BUILT-IN PROVIDER != USER-SUPPLIED HOST
 * REDIRECT != AUTHORIZATION TO FORWARD A SECRET
 */
object VexLifePartnerNetworkPlan {
    enum class Method {
        POST,
    }

    enum class RedirectPolicy {
        DENY,
    }

    data class Plan(
        val providerRef: String,
        val credentialRef: String,
        val origin: String,
        val inferencePath: String,
        val requestUrl: String,
        val dialect: VexLifePartnerProviderCatalog.Dialect,
        val credentialCarriage: VexLifePartnerProviderCatalog.CredentialCarriage,
        val accountScopeSelection: VexLifePartnerProviderCatalog.AccountScopeSelection,
        val method: Method,
        val protocolHeaders: Map<String, String>,
        /**
         * Ephemeral provider request body.
         *
         * Durable provider invocation evidence should retain [requestBodySha256], not this body.
         */
        val requestBody: String,
        val requestBodySha256: String,
        val redirectPolicy: RedirectPolicy,
    ) {
        init {
            require(
                requestUrl ==
                    origin +
                    inferencePath,
            ) {
                "Partner network plan URL must equal its exact source-owned origin/path"
            }
            require(
                requestBody.isNotBlank(),
            ) {
                "Partner network plan requires one non-empty provider request body"
            }
            require(
                requestBodySha256.matches(
                    Regex(
                        "[0-9a-f]{64}",
                    ),
                ),
            ) {
                "Partner network plan request-body digest must be SHA-256"
            }
            require(
                protocolHeaders.keys.none {
                    it.equals(
                        "authorization",
                        ignoreCase = true,
                    ) ||
                        it.equals(
                            "x-api-key",
                            ignoreCase = true,
                        )
                },
            ) {
                "credential-bearing headers do not belong in the no-effect Partner network plan"
            }
            require(
                redirectPolicy ==
                    RedirectPolicy.DENY,
            ) {
                "built-in Partner requests must deny redirects before credential injection"
            }
        }
    }

    fun form(
        providerRef: String,
        credential: VexLifePartnerCredentialCustody.Coordinate,
        requestBody: String,
    ): Plan {
        val provider =
            requireNotNull(
                VexLifePartnerProviderCatalog.resolve(
                    providerRef,
                ),
            ) {
                "Partner network plan requires one source-owned built-in provider"
            }
        require(
            credential.providerRef ==
                provider.providerRef,
        ) {
            "Partner credential coordinate does not belong to the selected provider"
        }
        require(
            requestBody.isNotBlank(),
        ) {
            "Partner network plan requires one provider request body"
        }

        val headers =
            linkedMapOf(
                "content-type" to "application/json",
                "accept" to "text/event-stream, application/json",
            )
        provider.requiredProtocolHeaders.forEach {
            (name, value) ->
            val normalized =
                name.lowercase()
            require(
                normalized !in
                    headers,
            ) {
                "Partner provider protocol header collides with a Product-owned base header"
            }
            headers[
                normalized
            ] =
                value
        }

        return Plan(
            providerRef = provider.providerRef,
            credentialRef = credential.credentialRef,
            origin = provider.origin,
            inferencePath = provider.inferencePath,
            requestUrl = provider.inferenceUrl,
            dialect = provider.dialect,
            credentialCarriage = provider.credentialCarriage,
            accountScopeSelection = provider.accountScopeSelection,
            method = Method.POST,
            protocolHeaders = headers.toMap(),
            requestBody = requestBody,
            requestBodySha256 =
                sha256(
                    requestBody,
                ),
            redirectPolicy = RedirectPolicy.DENY,
        )
    }

    private fun sha256(
        value: String,
    ): String =
        MessageDigest.getInstance(
            "SHA-256",
        )
            .digest(
                value.toByteArray(
                    StandardCharsets.UTF_8,
                ),
            )
            .joinToString(
                "",
            ) {
                "%02x".format(
                    it.toInt() and
                        0xff,
                )
            }
}

// [VXG RealForever]
