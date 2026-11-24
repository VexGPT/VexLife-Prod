package vexlife.model

import kotlinx.serialization.Serializable
import vexlife.model.heretic.HereticModelConfiguration
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
        PARTNER_PROFILE_IDENTITY_MISMATCH,
    }

    @Serializable
    data class Qualified(
        val coordinate: VexLifeInferenceSelection.Coordinate,
        val profileRef: String,
        val qualificationRef: String,
        val contextWindowTokens: Int? = null,
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
                    evidence = localRuntimeEvidence,
                )

            VexLifeModelSourceProjection.SourceKind.PARTNER ->
                partner(
                    serving = serving,
                    partner = partner,
                )
        }
    }

    private fun local(
        serving: VexLifeInferenceSelection.Coordinate,
        evidence: HereticModelConfiguration.QualifiedRuntimeEvidence?,
    ): Projection {
        val currentEvidence =
            evidence
                ?: return held(
                    serving = serving,
                    reason = HoldReason.LOCAL_RUNTIME_EVIDENCE_MISSING,
                )

        if (
            currentEvidence.modelRef !=
            serving.modelRef
        ) {
            return held(
                serving = serving,
                reason = HoldReason.LOCAL_RUNTIME_IDENTITY_MISMATCH,
            )
        }

        return qualified(
            serving = serving,
            profileRef = currentEvidence.modelProfileRef,
            qualificationRef = currentEvidence.runtimeReceiptRef,
            contextWindowTokens = currentEvidence.contextWindowTokens,
        )
    }

    private fun partner(
        serving: VexLifeInferenceSelection.Coordinate,
        partner: VexLifePartnerModelsCurrentState.Projection,
    ): Projection {
        val providerRef =
            requireNotNull(
                serving.providerRef,
            ) {
                "Partner serving coordinate requires providerRef"
            }
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
