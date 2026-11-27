package vexlife.attention

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Instant
import vexlife.contract.VexContractJson
import vexlife.continuity.VexLifeWorkingFocusOwner

/**
 * Stateless composition between source-owned Attention claims and Working Focus custody.
 *
 * Attention owns whether one exact source condition currently requires present awareness.
 * Working Focus owns the durable ACTIVE / DISPLACED / CLEARED state and exact resume law.
 *
 * This relationship only binds those owners together. It does not invent source significance,
 * acknowledge anything, resolve source conditions, read source bodies, admit prompt bytes, or grant
 * model/effect authority.
 *
 * This object is intentionally internal until a later caller/admission relationship is earned.
 */
internal object VexLifeAttentionWorkingFocusConvergence {
    const val EffectOwnerRef =
        "owner.vexlife-prod.attention-working-focus-convergence.001"

    enum class DisplacementStanding {
        NO_ACTIVE_WORKING_FOCUS,
        DISPLACED,
    }

    data class DisplacementResult(
        val standing: DisplacementStanding,
        val claimBindingRef: String,
        val workingFocusStateRefOrNull: String?,
        val workingFocusRevisionRefOrNull: String?,
        val displacementPerformedNow: Boolean,
    )

    data class ResumeResult(
        val claimBindingRef: String,
        val workingFocusStateRef: String,
        val workingFocusRevisionRef: String,
        val resumePerformedNow: Boolean,
    )

    /**
     * Displace only if one ACTIVE Working Focus exists.
     *
     * The exact Attention claim is reduced to one canonical digest ref for Working Focus cause
     * binding. No source body or private reasoning enters Working Focus custody.
     *
     * A repeated exact claim is idempotent through [VexLifeWorkingFocusOwner]. A different claim
     * while another displacement is current fails closed because Working Focus v1 admits no nested
     * displacement.
     */
    fun displaceForClaim(
        owner: VexLifeWorkingFocusOwner,
        lineageRef: String,
        threadRef: String,
        claim: VexLifeAttentionClaim.Claim,
        formedAt: Instant = Instant.now(),
    ): DisplacementResult {
        val claimBindingRef = claimBindingRef(claim)
        val before = owner.readCurrent(lineageRef, threadRef)
        if (before == null || before.state.state == VexLifeWorkingFocusOwner.FocusState.CLEARED) {
            return DisplacementResult(
                standing = DisplacementStanding.NO_ACTIVE_WORKING_FOCUS,
                claimBindingRef = claimBindingRef,
                workingFocusStateRefOrNull = before?.stateRef,
                workingFocusRevisionRefOrNull = before?.revisionRefOrNull,
                displacementPerformedNow = false,
            )
        }

        val displaced =
            owner.displace(
                lineageRef = lineageRef,
                threadRef = threadRef,
                causeOwnerRef = EffectOwnerRef,
                causeRef = claim.requirementRef,
                causeBindingRefs = listOf(claimBindingRef),
                formedAt = formedAt,
            )

        return DisplacementResult(
            standing = DisplacementStanding.DISPLACED,
            claimBindingRef = claimBindingRef,
            workingFocusStateRefOrNull = displaced.current.stateRef,
            workingFocusRevisionRefOrNull = displaced.current.revisionRefOrNull,
            displacementPerformedNow = displaced.effectPerformedNow,
        )
    }

    /**
     * Return one exact displaced Working Focus only after the rightful source has retired the exact
     * Attention claim that caused that displacement.
     *
     * Acknowledgement is intentionally not accepted by this API. Source retirement and Working
     * Focus source revalidation are both required.
     */
    fun resumeAfterSourceRetirement(
        owner: VexLifeWorkingFocusOwner,
        lineageRef: String,
        threadRef: String,
        displacedStateRef: String,
        retiredClaim: VexLifeAttentionClaim.RetiredClaim,
        revalidatedWorkingFocusSourceBindingRefs: List<String>,
        formedAt: Instant = Instant.now(),
    ): ResumeResult {
        val current =
            requireNotNull(
                owner.readCurrent(
                    lineageRef,
                    threadRef,
                ),
            ) {
                "attention/working-focus resume requires current Working Focus custody"
            }
        require(current.state.state == VexLifeWorkingFocusOwner.FocusState.DISPLACED) {
            "attention/working-focus resume requires current DISPLACED Working Focus"
        }
        require(current.stateRef == displacedStateRef) {
            "attention/working-focus resume displacement is stale or mismatched"
        }

        val expectedClaimBindingRef =
            claimBindingRef(
                retiredClaim.claim,
            )
        require(current.state.causeOwnerRef == EffectOwnerRef) {
            "attention/working-focus displacement belongs to another owner"
        }
        require(current.state.causeRef == retiredClaim.claim.requirementRef) {
            "attention/working-focus retirement does not bind the displaced requirement"
        }
        require(current.state.causeBindingRefs == listOf(expectedClaimBindingRef)) {
            "attention/working-focus retirement does not bind the exact displaced claim"
        }

        val resumed =
            owner.resume(
                lineageRef = lineageRef,
                threadRef = threadRef,
                displacedStateRef = displacedStateRef,
                releaseOwnerRef = retiredClaim.claim.ownerRef,
                releaseRef = retiredClaim.retirementRef,
                releaseBindingRefs = retiredClaim.retirementBindingRefs,
                revalidatedSourceBindingRefs =
                    revalidatedWorkingFocusSourceBindingRefs,
                formedAt = formedAt,
            )

        return ResumeResult(
            claimBindingRef = expectedClaimBindingRef,
            workingFocusStateRef = resumed.current.stateRef,
            workingFocusRevisionRef =
                requireNotNull(
                    resumed.current.revisionRefOrNull,
                ) {
                    "resumed Working Focus must retain one exact revision"
                },
            resumePerformedNow = resumed.effectPerformedNow,
        )
    }

    private fun claimBindingRef(
        claim: VexLifeAttentionClaim.Claim,
    ): String =
        "binding.vexlife-prod.attention-claim." +
            sha256(
                VexContractJson.canonicalEncode(
                    claim,
                ),
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
                "%02x".format(it)
            }
}

// [VXG RealForever]
