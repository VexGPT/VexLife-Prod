package vexlife.model

import kotlinx.serialization.Serializable
import vexlife.partner.VexLifePartnerConnectionProjection

/**
 * Read-only effective capability projection for one exact Partner provider/model relationship.
 *
 * This object does not decide which capabilities a provider/model represents. A rightful
 * source-owned Partner profile supplies that evidence. This projection only joins:
 *
 * - exact provider/model/profile identity;
 * - representation evidence;
 * - Product qualification evidence for each represented capability;
 * - current Partner connection dimensions.
 *
 * Permanent:
 *
 * PROVIDER MODEL NAME != CAPABILITY
 * REPRESENTED != QUALIFIED
 * CONNECTION READY != CAPABILITY QUALIFIED
 * CAPABILITY PROJECTION != CAPABILITY AUTHORITY
 * EFFECTIVE CAPABILITIES != FINAL SUPPORT STANDING
 */
object VexLifePartnerEffectiveModelCapabilities {
    @Serializable
    data class CapabilityEvidence(
        val represented: Boolean,
        val qualificationRef: String?,
    ) {
        init {
            qualificationRef
                ?.let {
                    requireRef(
                        it,
                        "qualificationRef",
                    )
                }
            require(
                represented ||
                    qualificationRef ==
                    null,
            ) {
                "Partner capability may not carry qualification evidence when representation is absent"
            }
        }
    }

    @Serializable
    data class ProfileEvidence(
        val providerRef: String,
        val modelRef: String,
        val profileRef: String,
        val vexReach: CapabilityEvidence,
        val providerNativeTools: CapabilityEvidence,
        val reasoningEffortControl: CapabilityEvidence,
        val visualInput: CapabilityEvidence,
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
            requireRef(
                profileRef,
                "profileRef",
            )
        }
    }

    @Serializable
    data class Projection(
        val providerRef: String,
        val modelRef: String,
        val profileRef: String,
        val connectionStanding: VexLifePartnerConnectionProjection.Standing,
        val vexReach: VexLifeEffectiveModelCapabilities.Standing,
        val providerNativeTools: VexLifeEffectiveModelCapabilities.Standing,
        val reasoningEffortControl: VexLifeEffectiveModelCapabilities.Standing,
        val visualInput: VexLifeEffectiveModelCapabilities.Standing,
    )

    fun project(
        profile: ProfileEvidence,
        connection: VexLifePartnerConnectionProjection.Projection,
    ): Projection {
        require(
            connection.providerRef ==
                profile.providerRef &&
                connection.modelRef ==
                profile.modelRef,
        ) {
            "Partner effective capabilities require connection truth for the exact provider/model"
        }

        val connectionReady =
            connection.credential ==
                VexLifePartnerConnectionProjection.CredentialStanding.ACCEPTED &&
                connection.account ==
                    VexLifePartnerConnectionProjection.AccountStanding.USABLE &&
                connection.service ==
                    VexLifePartnerConnectionProjection.ServiceStanding.READY &&
                connection.modelAccess ==
                    VexLifePartnerConnectionProjection.ModelAccessStanding.AVAILABLE

        fun effective(
            evidence: CapabilityEvidence,
        ): VexLifeEffectiveModelCapabilities.Standing =
            when {
                !evidence.represented ->
                    VexLifeEffectiveModelCapabilities.Standing.NOT_APPLICABLE

                evidence.qualificationRef ==
                    null ->
                    VexLifeEffectiveModelCapabilities.Standing.HELD

                !connectionReady ->
                    VexLifeEffectiveModelCapabilities.Standing.HELD

                else ->
                    VexLifeEffectiveModelCapabilities.Standing.AVAILABLE
            }

        return Projection(
            providerRef = profile.providerRef,
            modelRef = profile.modelRef,
            profileRef = profile.profileRef,
            connectionStanding = connection.standing,
            vexReach =
                effective(
                    profile.vexReach,
                ),
            providerNativeTools =
                effective(
                    profile.providerNativeTools,
                ),
            reasoningEffortControl =
                effective(
                    profile.reasoningEffortControl,
                ),
            visualInput =
                effective(
                    profile.visualInput,
                ),
        )
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
