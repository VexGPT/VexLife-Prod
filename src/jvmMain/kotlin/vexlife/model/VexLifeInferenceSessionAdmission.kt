package vexlife.model

import kotlinx.serialization.Serializable
import vexlife.model.heretic.HereticModelConfiguration
import vexlife.partner.VexLifePartnerAccountScope
import vexlife.partner.VexLifePartnerConfiguredCurrentness
import vexlife.partner.VexLifePartnerConnectionProjection
import vexlife.partner.VexLifePartnerModelProfileCatalog
import vexlife.partner.VexLifePartnerSupportQualification

/**
 * Read-only admission for the exact inference realization currently serving one Vex Home.
 *
 * The staged target is human intent. The serving coordinate is the realization a Companion turn may
 * actually use. This owner does not switch models, invoke a provider, persist model choice, mutate
 * Home/Conversation/Memory, or grant provider fallback.
 *
 * Permanent:
 *
 * TARGETED INFERENCE != SERVING INFERENCE
 * SERVING COORDINATE != QUALIFIED SERVING SESSION
 * PARTNER READY TO ATTEMPT != PARTNER QUALIFIED SERVING
 * CONTRACT PROVEN != LIVE QUALIFIED
 */
object VexLifeInferenceSessionAdmission {
    @Serializable
    enum class Standing {
        HELD,
        QUALIFIED,
    }

    @Serializable
    enum class HoldReason {
        NO_SERVING_INFERENCE,
        LOCAL_RUNTIME_EVIDENCE_MISSING,
        LOCAL_RUNTIME_IDENTITY_MISMATCH,
        PARTNER_CURRENTNESS_MISSING,
        PARTNER_CONNECTION_NOT_QUALIFIED,
        PARTNER_SUPPORT_NOT_LIVE_QUALIFIED,
        PARTNER_QUALIFICATION_RECEIPT_MISSING,
        PARTNER_QUALIFICATION_CURRENTNESS_MISMATCH,
        PARTNER_PROFILE_IDENTITY_MISMATCH,
    }

    @Serializable
    data class Qualified(
        val coordinate: VexLifeInferenceSelection.Coordinate,
        val profileRef: String,
        val qualificationRef: String,
        val contextWindowTokens: Int? = null,
        /**
         * Exact non-secret protected-host credential coordinate admitted for this serving session.
         *
         * Local sessions carry neither Partner field. Partner sessions carry both so a credential
         * rotation/account-scope change cannot be paired with a qualification formed for another
         * configured connection.
         */
        val partnerCredentialRef: String? = null,
        val partnerAccountScope: VexLifePartnerAccountScope.Selection? = null,
    ) {
        init {
            requireRef(
                profileRef,
                "profileRef",
            )
            requireRef(
                qualificationRef,
                "qualificationRef",
            )
            require(
                contextWindowTokens == null ||
                    contextWindowTokens >
                    0,
            ) {
                "qualified inference context window must be absent or positive"
            }

            when (
                coordinate.sourceKind
            ) {
                VexLifeModelSourceProjection.SourceKind.LOCAL ->
                    require(
                        partnerCredentialRef ==
                            null &&
                            partnerAccountScope ==
                            null,
                    ) {
                        "Local qualified inference may not carry Partner execution coordinates"
                    }

                VexLifeModelSourceProjection.SourceKind.PARTNER -> {
                    val providerRef =
                        requireNotNull(
                            coordinate.providerRef,
                        )
                    val credentialRef =
                        requireNotNull(
                            partnerCredentialRef,
                        ) {
                            "Partner qualified inference requires the exact current credential ref"
                        }
                    requireRef(
                        credentialRef,
                        "partnerCredentialRef",
                    )
                    require(
                        partnerAccountScope
                            ?.providerRef ==
                            providerRef,
                    ) {
                        "Partner qualified inference account scope must bind the exact provider"
                    }
                }
            }
        }
    }

    @Serializable
    data class Projection(
        val serving: VexLifeInferenceSelection.Coordinate?,
        val standing: Standing,
        val holdReason: HoldReason?,
        val qualified: Qualified?,
    ) {
        init {
            when (
                standing
            ) {
                Standing.HELD ->
                    require(
                        holdReason !=
                            null &&
                            qualified ==
                            null,
                    ) {
                        "held inference admission requires one hold reason and no qualified session"
                    }

                Standing.QUALIFIED ->
                    require(
                        holdReason ==
                            null &&
                            qualified !=
                            null &&
                            qualified.coordinate ==
                            serving,
                    ) {
                        "qualified inference admission must bind the exact serving coordinate"
                    }
            }
        }
    }

    fun project(
        current: VexLifeInferenceCurrentState.Projection,
        local: VexLifeModels.Projection,
        localRuntimeEvidence: HereticModelConfiguration.QualifiedRuntimeEvidence?,
        partner: VexLifePartnerModelsCurrentState.Projection,
    ): Projection {
        val serving =
            current.inference.serving
                ?: return held(
                    serving = null,
                    reason = HoldReason.NO_SERVING_INFERENCE,
                )

        return when (
            serving.sourceKind
        ) {
            VexLifeModelSourceProjection.SourceKind.LOCAL ->
                local(
                    serving = serving,
                    local = local,
                    evidence = localRuntimeEvidence,
                )

            VexLifeModelSourceProjection.SourceKind.PARTNER ->
                partnerAdmission(
                    serving = serving,
                    partner = partner,
                )
        }
    }

    private fun local(
        serving: VexLifeInferenceSelection.Coordinate,
        local: VexLifeModels.Projection,
        evidence: HereticModelConfiguration.QualifiedRuntimeEvidence?,
    ): Projection {
        val currentEvidence =
            evidence
                ?: return held(
                    serving = serving,
                    reason = HoldReason.LOCAL_RUNTIME_EVIDENCE_MISSING,
                )
        val row =
            local.models
                .singleOrNull {
                    it.handle ==
                        serving.handle &&
                        it.serving &&
                        it.capabilities.modelRef ==
                        serving.modelRef
                }
                ?: return held(
                    serving = serving,
                    reason = HoldReason.LOCAL_RUNTIME_IDENTITY_MISMATCH,
                )
        val capabilities =
            row.capabilities

        if (
            capabilities.runtime !=
            VexLifeEffectiveModelCapabilities.RuntimeStanding.QUALIFIED_SERVING ||
            capabilities.qualifiedRuntimeReceiptRef !=
            currentEvidence.runtimeReceiptRef
        ) {
            return held(
                serving = serving,
                reason = HoldReason.LOCAL_RUNTIME_IDENTITY_MISMATCH,
            )
        }

        return qualified(
            serving = serving,
            profileRef = capabilities.profileRef,
            qualificationRef = currentEvidence.runtimeReceiptRef,
            contextWindowTokens = currentEvidence.contextWindowTokens,
        )
    }

    private fun partnerAdmission(
        serving: VexLifeInferenceSelection.Coordinate,
        partner: VexLifePartnerModelsCurrentState.Projection,
    ): Projection {
        val providerRef =
            requireNotNull(
                serving.providerRef,
            ) {
                "Partner serving coordinate requires providerRef"
            }
        val configuredProvider =
            partner.configuredProviders
                .singleOrNull {
                    it.providerRef ==
                        providerRef &&
                        it.coordinateStanding ==
                        VexLifePartnerConfiguredCurrentness.CoordinateStanding.PRESENT &&
                        it.credentialRef !=
                        null &&
                        it.accountScope !=
                        null
                }
                ?: return held(
                    serving = serving,
                    reason = HoldReason.PARTNER_CURRENTNESS_MISSING,
                )
        val offering =
            partner.offerings
                .singleOrNull {
                    it.handle ==
                        serving.handle &&
                        it.providerRef ==
                        providerRef &&
                        it.modelRef ==
                        serving.modelRef
                }
                ?: return held(
                    serving = serving,
                    reason = HoldReason.PARTNER_CURRENTNESS_MISSING,
                )

        if (
            offering.connection.standing !=
            VexLifePartnerConnectionProjection.Standing.QUALIFIED_SERVING
        ) {
            return held(
                serving = serving,
                reason = HoldReason.PARTNER_CONNECTION_NOT_QUALIFIED,
            )
        }

        if (
            offering.support.standing !=
            VexLifePartnerSupportQualification.Standing.LIVE_QUALIFIED
        ) {
            return held(
                serving = serving,
                reason = HoldReason.PARTNER_SUPPORT_NOT_LIVE_QUALIFIED,
            )
        }

        val qualificationRef =
            offering.support.liveQualificationReceiptRef
                ?: return held(
                    serving = serving,
                    reason = HoldReason.PARTNER_QUALIFICATION_RECEIPT_MISSING,
                )
        if (
            offering.support.liveQualificationCredentialRef !=
                configuredProvider.credentialRef ||
            offering.support.liveQualificationAccountScope !=
                configuredProvider.accountScope
        ) {
            return held(
                serving = serving,
                reason = HoldReason.PARTNER_QUALIFICATION_CURRENTNESS_MISMATCH,
            )
        }
        val profile =
            VexLifePartnerModelProfileCatalog.resolve(
                providerRef = providerRef,
                modelRef = serving.modelRef,
            )
                ?: return held(
                    serving = serving,
                    reason = HoldReason.PARTNER_PROFILE_IDENTITY_MISMATCH,
                )

        if (
            profile.profileRef !=
            serving.handle
        ) {
            return held(
                serving = serving,
                reason = HoldReason.PARTNER_PROFILE_IDENTITY_MISMATCH,
            )
        }

        return qualified(
            serving = serving,
            profileRef = profile.profileRef,
            qualificationRef = qualificationRef,
            contextWindowTokens = null,
            partnerCredentialRef =
                requireNotNull(
                    configuredProvider.credentialRef,
                ),
            partnerAccountScope =
                requireNotNull(
                    configuredProvider.accountScope,
                ),
        )
    }

    private fun held(
        serving: VexLifeInferenceSelection.Coordinate?,
        reason: HoldReason,
    ): Projection =
        Projection(
            serving = serving,
            standing = Standing.HELD,
            holdReason = reason,
            qualified = null,
        )

    private fun qualified(
        serving: VexLifeInferenceSelection.Coordinate,
        profileRef: String,
        qualificationRef: String,
        contextWindowTokens: Int?,
        partnerCredentialRef: String? = null,
        partnerAccountScope: VexLifePartnerAccountScope.Selection? = null,
    ): Projection =
        Projection(
            serving = serving,
            standing = Standing.QUALIFIED,
            holdReason = null,
            qualified =
                Qualified(
                    coordinate = serving,
                    profileRef = profileRef,
                    qualificationRef = qualificationRef,
                    contextWindowTokens = contextWindowTokens,
                    partnerCredentialRef = partnerCredentialRef,
                    partnerAccountScope = partnerAccountScope,
                ),
        )

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
