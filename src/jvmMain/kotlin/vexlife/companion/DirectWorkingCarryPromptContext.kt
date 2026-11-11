package vexlife.companion

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import kotlinx.serialization.Serializable
import vexlife.continuity.VexLifeWorkingCarryContract
import vexlife.continuity.VexLifeWorkingCarryOwner
import vexlife.contract.VexContractJson

/**
 * Caller-bounded provider projection of one Vex present working carry.
 *
 * This owner allocates no prompt budget. The live turn supplies the exact remaining allowance after
 * current exact source selection. A carry is included only when its complete provider-ready message
 * fits that allowance. Omission from one prompt does not release, supersede or mutate the carry.
 */
object DirectWorkingCarryPromptContext {
    const val ProjectionSchema =
        "vexlife-prod.working-carry-prompt-projection/v1"

    private const val PacketSchema =
        "vexlife-prod.working-carry-prompt-packet/v1"

    const val CandidateSource =
        "TURN_CANDIDATE"

    const val CurrentRevisionSource =
        "CURRENT_REVISION"

    const val Included =
        "INCLUDED"

    const val Released =
        "RELEASED"

    const val HeldTokenBudget =
        "HELD_TOKEN_BUDGET"

    const val NoCurrentCarry =
        "NO_CURRENT_CARRY"

    @Serializable
    private data class ProviderPacket(
        val schemaVersion: String,
        val truthClass: String,
        val perspective: String,
        val sourceClass: String,
        val sourceRef: String,
        val sourceFingerprint: String,
        val draftSha256: String,
        val carry: VexLifeWorkingCarryContract.Draft,
        val durableMemoryAuthorityGranted: Boolean,
        val humanProfileAuthorityGranted: Boolean,
        val relationshipAgreementAuthorityGranted: Boolean,
        val canonicalConversationAuthorityGranted: Boolean,
        val effectsPerformed: Boolean,
    )

    data class Projection internal constructor(
        val schemaVersion: String,
        val state: String,
        val sourceClass: String?,
        val sourceRef: String?,
        val sourceFingerprint: String?,
        val draftSha256: String?,
        val carryDisposition: String?,
        val included: Boolean,
        val availableTokens: Int,
        val estimatedSystemMessageTokens: Int,
        val systemMessageSha256: String?,
        val systemContent: String?,
        val effectAuthorityGranted: Boolean,
        val effectsPerformed: Boolean,
    )

    fun projectCandidate(
        formation: DirectWorkingCarryFormation.Formation,
        availableTokens: Int,
    ): Projection {
        require(
            !formation.rawProviderOutputPersisted &&
                !formation.rawReasoningPersisted &&
                !formation.humanAnswerProduced,
        ) {
            "working carry prompt candidate cannot come from a human-answer or raw-persisting formation"
        }
        val draft =
            VexLifeWorkingCarryContract.requireDraftShape(
                formation.draft,
            )
        val draftSha =
            sha256(
                VexContractJson.canonicalEncode(
                    draft,
                ),
            )
        require(
            formation.binding.draftSha256 ==
                draftSha &&
                formation.binding.providerOutputSha256 ==
                formation.providerOutputSha256,
        ) {
            "working carry prompt candidate is detached from its exact formation binding"
        }

        return project(
            draft = draft,
            sourceClass = CandidateSource,
            sourceRef = formation.binding.formationRef,
            sourceFingerprint = formation.binding.formationFingerprint,
            availableTokens = availableTokens,
        )
    }

    fun projectCurrent(
        current: VexLifeWorkingCarryOwner.Current?,
        availableTokens: Int,
    ): Projection {
        requireAllowance(
            availableTokens,
        )
        if (
            current ==
                null
        ) {
            return Projection(
                schemaVersion = ProjectionSchema,
                state = NoCurrentCarry,
                sourceClass = null,
                sourceRef = null,
                sourceFingerprint = null,
                draftSha256 = null,
                carryDisposition = null,
                included = false,
                availableTokens = availableTokens,
                estimatedSystemMessageTokens = 0,
                systemMessageSha256 = null,
                systemContent = null,
                effectAuthorityGranted = false,
                effectsPerformed = false,
            )
        }

        val revision =
            current.revision
        val draft =
            VexLifeWorkingCarryContract.requireDraftShape(
                revision.carry,
            )
        val draftSha =
            sha256(
                VexContractJson.canonicalEncode(
                    draft,
                ),
            )
        require(
            revision.formationBinding.draftSha256 ==
                draftSha &&
                revision.revisionRef.isNotBlank() &&
                revision.semanticFingerprint.matches(
                    Regex(
                        "[0-9a-f]{64}",
                    ),
                ),
        ) {
            "working carry current revision is detached from its exact Draft or source identity"
        }

        return project(
            draft = draft,
            sourceClass = CurrentRevisionSource,
            sourceRef = revision.revisionRef,
            sourceFingerprint = revision.semanticFingerprint,
            availableTokens = availableTokens,
        )
    }

    private fun project(
        draft: VexLifeWorkingCarryContract.Draft,
        sourceClass: String,
        sourceRef: String,
        sourceFingerprint: String,
        availableTokens: Int,
    ): Projection {
        requireAllowance(
            availableTokens,
        )
        require(
            sourceClass in
                setOf(
                    CandidateSource,
                    CurrentRevisionSource,
                ) &&
                sourceRef.isNotBlank() &&
                sourceFingerprint.matches(
                    Regex(
                        "[0-9a-f]{64}",
                    ),
                ),
        ) {
            "working carry prompt source identity is invalid"
        }

        if (
            draft.disposition ==
                VexLifeWorkingCarryContract.Release
        ) {
            return Projection(
                schemaVersion = ProjectionSchema,
                state = Released,
                sourceClass = sourceClass,
                sourceRef = sourceRef,
                sourceFingerprint = sourceFingerprint,
                draftSha256 =
                    sha256(
                        VexContractJson.canonicalEncode(
                            draft,
                        ),
                    ),
                carryDisposition = draft.disposition,
                included = false,
                availableTokens = availableTokens,
                estimatedSystemMessageTokens = 0,
                systemMessageSha256 = null,
                systemContent = null,
                effectAuthorityGranted = false,
                effectsPerformed = false,
            )
        }

        require(
            draft.disposition ==
                VexLifeWorkingCarryContract.Carry,
        ) {
            "working carry prompt projection received an unknown disposition"
        }

        val draftSha =
            sha256(
                VexContractJson.canonicalEncode(
                    draft,
                ),
            )
        val packet =
            ProviderPacket(
                schemaVersion = PacketSchema,
                truthClass = VexLifeWorkingCarryContract.TruthClass,
                perspective = VexLifeWorkingCarryContract.Perspective,
                sourceClass = sourceClass,
                sourceRef = sourceRef,
                sourceFingerprint = sourceFingerprint,
                draftSha256 = draftSha,
                carry = draft,
                durableMemoryAuthorityGranted = false,
                humanProfileAuthorityGranted = false,
                relationshipAgreementAuthorityGranted = false,
                canonicalConversationAuthorityGranted = false,
                effectsPerformed = false,
            )
        val systemContent =
            listOf(
                "VEXLIFE_PRESENT_WORKING_CARRY/v1",
                "This is Vex's current self-relative working carry, not durable Memory, a profile of the human, a relationship agreement, or canonical Conversation truth.",
                "Current human input and exact current Conversation source outrank this carry on conflict. Preserve unresolved conflict instead of inventing reconciliation.",
                "A source reference names where the carry came from; do not invent source content that is not directly present.",
                VexContractJson.compactEncode(
                    packet,
                ),
            )
                .joinToString(
                    "\n",
                )
        val estimate =
            DirectPromptContext.estimatedMessageTokens(
                systemContent,
            )
        val hash =
            sha256(
                systemContent,
            )
        val included =
            estimate <=
                availableTokens

        return Projection(
            schemaVersion = ProjectionSchema,
            state =
                if (
                    included
                ) {
                    Included
                } else {
                    HeldTokenBudget
                },
            sourceClass = sourceClass,
            sourceRef = sourceRef,
            sourceFingerprint = sourceFingerprint,
            draftSha256 = draftSha,
            carryDisposition = draft.disposition,
            included = included,
            availableTokens = availableTokens,
            estimatedSystemMessageTokens = estimate,
            systemMessageSha256 = hash,
            systemContent =
                if (
                    included
                ) {
                    systemContent
                } else {
                    null
                },
            effectAuthorityGranted = false,
            effectsPerformed = false,
        )
    }

    private fun requireAllowance(
        value: Int,
    ) {
        require(
            value >=
                0 &&
                value <=
                DirectPromptContext.InputTokenBudget,
        ) {
            "working carry prompt allowance is outside the admitted input token budget"
        }
    }

    private fun sha256(
        value: String,
    ): String =
        MessageDigest
            .getInstance(
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
