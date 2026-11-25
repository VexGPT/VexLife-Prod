package vexlife.partner

import java.nio.charset.StandardCharsets
import java.security.MessageDigest

/**
 * Portable JVM-facing contract for one protected Partner host effect.
 *
 * The platform helper owns:
 * - protected credential lookup;
 * - authentication-header formation;
 * - exact HTTPS execution;
 * - redirect refusal;
 * - secret lifetime.
 *
 * The JVM sends only [VexLifePartnerNetworkPlan.Plan] and receives only non-secret response/evidence.
 *
 * Permanent:
 *
 * HOST EFFECT != SECRET READ API
 * HTTP ERROR RESPONSE != TRANSPORT FAILURE
 * REDIRECT != HTTP RESPONSE TO FOLLOW
 * HOST RECEIPT != PROVIDER SEMANTIC AUTHORITY
 */
object VexLifePartnerHostEffect {
    enum class FailureCode {
        CREDENTIAL_NOT_PRESENT,
        CREDENTIAL_PROVIDER_MISMATCH,
        DNS_FAILURE,
        CONNECT_FAILURE,
        TLS_FAILURE,
        TIMEOUT,
        REDIRECT_REJECTED,
        HOST_UNAVAILABLE,
    }

    data class Receipt(
        val providerRef: String,
        val credentialRef: String,
        val requestUrl: String,
        val requestBodySha256: String,
        val actualNetworkEffect: Boolean,
        val httpStatus: Int?,
        val responseBodySha256: String?,
        val redirectFollowed: Boolean = false,
        val secretProjected: Boolean = false,
    ) {
        init {
            require(
                !secretProjected,
            ) {
                "Partner host receipt may not project secret material"
            }
            require(
                !redirectFollowed,
            ) {
                "Partner host receipt may not claim a followed redirect"
            }
            require(
                requestBodySha256.matches(
                    Sha256,
                ),
            ) {
                "Partner host receipt request digest must be SHA-256"
            }
            responseBodySha256
                ?.let {
                    require(
                        it.matches(
                            Sha256,
                        ),
                    ) {
                        "Partner host receipt response digest must be SHA-256"
                    }
                }
            httpStatus
                ?.let {
                    require(
                        it in
                            100..599 &&
                            it !in
                            300..399,
                    ) {
                        "Partner host receipt may not represent a followed/accepted redirect response"
                    }
                }
        }
    }

    sealed interface Result {
        val receipt: Receipt

        /**
         * Any non-redirect HTTP response, including 4xx/5xx.
         *
         * Provider error bodies belong to the P1 codec/A5 currentness path rather than being
         * collapsed into host transport failure.
         */
        data class Http(
            val status: Int,
            val body: String,
            val retryAfter: String? = null,
            val providerRequestId: String? = null,
            override val receipt: Receipt,
        ) : Result {
            init {
                require(
                    status in
                        100..599 &&
                        status !in
                        300..399,
                ) {
                    "Partner host HTTP result may not expose redirects as accepted responses"
                }
                require(
                    receipt.actualNetworkEffect &&
                        receipt.httpStatus ==
                        status &&
                        receipt.responseBodySha256 ==
                        sha256(
                            body,
                        ),
                ) {
                    "Partner host HTTP receipt must bind the exact returned status/body"
                }
                retryAfter
                    ?.let {
                        require(
                            it.length <=
                                256 &&
                                '\n' !in
                                it &&
                                '\r' !in
                                it,
                        ) {
                            "Partner host retry-after observation must be one bounded header value"
                        }
                    }
                providerRequestId
                    ?.let {
                        require(
                            it.length <=
                                512 &&
                                '\n' !in
                                it &&
                                '\r' !in
                                it,
                        ) {
                            "Partner host request-id observation must be one bounded header value"
                        }
                    }
            }
        }

        data class TransportFailure(
            val code: FailureCode,
            val detail: String,
            override val receipt: Receipt,
        ) : Result {
            init {
                require(
                    detail.isNotBlank() &&
                        detail.length <=
                        1_024,
                ) {
                    "Partner host transport failure detail must be bounded"
                }
                require(
                    receipt.httpStatus ==
                        null &&
                        receipt.responseBodySha256 ==
                        null,
                ) {
                    "transport failure receipt may not claim an HTTP response"
                }
            }
        }
    }

    /**
     * Implemented by the platform-specific protected host effect.
     *
     * There is intentionally no credential-returning method.
     */
    fun interface Executor {
        fun execute(
            plan: VexLifePartnerNetworkPlan.Plan,
        ): Result
    }

    /**
     * Verify the host helper returned evidence for exactly the plan the JVM admitted.
     */
    fun requireExact(
        plan: VexLifePartnerNetworkPlan.Plan,
        result: Result,
    ): Result {
        val receipt =
            result.receipt
        require(
            receipt.providerRef ==
                plan.providerRef &&
                receipt.credentialRef ==
                plan.credentialRef &&
                receipt.requestUrl ==
                plan.requestUrl &&
                receipt.requestBodySha256 ==
                plan.requestBodySha256 &&
                !receipt.redirectFollowed &&
                !receipt.secretProjected,
        ) {
            "Partner host effect receipt does not bind the exact admitted network plan"
        }

        return result
    }

    fun receiptForHttp(
        plan: VexLifePartnerNetworkPlan.Plan,
        status: Int,
        body: String,
    ): Receipt =
        Receipt(
            providerRef = plan.providerRef,
            credentialRef = plan.credentialRef,
            requestUrl = plan.requestUrl,
            requestBodySha256 = plan.requestBodySha256,
            actualNetworkEffect = true,
            httpStatus = status,
            responseBodySha256 =
                sha256(
                    body,
                ),
        )

    fun receiptForTransportFailure(
        plan: VexLifePartnerNetworkPlan.Plan,
        networkAttempted: Boolean,
    ): Receipt =
        Receipt(
            providerRef = plan.providerRef,
            credentialRef = plan.credentialRef,
            requestUrl = plan.requestUrl,
            requestBodySha256 = plan.requestBodySha256,
            actualNetworkEffect = networkAttempted,
            httpStatus = null,
            responseBodySha256 = null,
        )

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

    private val Sha256 =
        Regex(
            "[0-9a-f]{64}",
        )
}

// [VXG RealForever]
