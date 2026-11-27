package vexlife.attention

import kotlinx.serialization.Serializable

/**
 * Source-owned present-attention claims for Attention Convergence A3.
 *
 * This owner answers one narrow relationship question:
 *
 * ```text
 * A rightful source already says one current condition requires present attention.
 * How can Vex carry that requirement and an acknowledgement without inventing severity,
 * priority, source resolution, displacement, or effect authority?
 * ```
 *
 * A [SourceRequirement] is supplied by the rightful source/domain owner. Attention validates that
 * it still names the exact current A1 candidate and preserves the requirement as a no-effect claim.
 *
 * Working Focus displacement/resume is deliberately absent. That relationship is added only after
 * the rightful Working Focus owner is current.
 */
object VexLifeAttentionClaim {
    const val SurfaceSchema = "vexlife-prod.attention-claim-surface/v1"

    /**
     * Source-owned evidence that one exact current candidate requires present attention.
     *
     * [requirementStandingRef] stays opaque to Attention. It may represent domain-specific severity,
     * safety, urgency, or another source-owned standing. Attention does not interpret it.
     */
    @Serializable
    data class SourceRequirement(
        val requirementRef: String,
        val ownerRef: String,
        val sourceRef: String,
        val candidateRef: String,
        val candidateSourceBindingRefs: List<String>,
        val candidateSubjectScopeRefs: List<String>,
        val requirementBindingRefs: List<String>,
        val requirementStandingRef: String,
        val acknowledgementRequirementRefOrNull: String? = null,
    ) {
        init {
            requireRef(requirementRef, "requirementRef")
            requireRef(ownerRef, "ownerRef")
            requireRef(sourceRef, "sourceRef")
            requireRef(candidateRef, "candidateRef")
            acknowledgementRequirementRefOrNull?.let {
                requireRef(it, "acknowledgementRequirementRefOrNull")
            }
            requireRefs(
                candidateSourceBindingRefs,
                "candidateSourceBindingRefs",
                allowEmpty = false,
            )
            requireRefs(
                candidateSubjectScopeRefs,
                "candidateSubjectScopeRefs",
                allowEmpty = true,
            )
            requireRefs(
                requirementBindingRefs,
                "requirementBindingRefs",
                allowEmpty = false,
            )
            requireRef(requirementStandingRef, "requirementStandingRef")
        }
    }

    /**
     * Attention-owned projection of one current source requirement.
     *
     * This is not a notification and does not mean a human/model has already seen it. It is the
     * bounded relationship that may later be placed into present awareness by the rightful
     * presentation/context path.
     */
    @Serializable
    data class Claim(
        val requirementRef: String,
        val ownerRef: String,
        val sourceRef: String,
        val candidateRef: String,
        val relationshipRef: String,
        val subjectScopeRefs: List<String>,
        val candidateSourceBindingRefs: List<String>,
        val sourceStandingRef: String,
        val requirementBindingRefs: List<String>,
        val requirementStandingRef: String,
        val acknowledgementRequirementRefOrNull: String?,
        val deeperSourceRef: String,
        val reachability: VexLifeAttentionPerceptibility.Reachability,
    )

    /**
     * Exact acknowledgement evidence supplied by the rightful interaction/human-control owner.
     *
     * Attention may bind acknowledgement to a current claim, but acknowledgement does not mutate or
     * resolve the source condition.
     */
    @Serializable
    data class AcknowledgementObservation(
        val acknowledgementRef: String,
        val actorRef: String,
        val requirementRef: String,
        val requirementBindingRefs: List<String>,
        val acknowledgementRequirementRefOrNull: String?,
        val candidateSourceBindingRefs: List<String>,
        val subjectScopeRefs: List<String>,
    ) {
        init {
            requireRef(acknowledgementRef, "acknowledgementRef")
            requireRef(actorRef, "actorRef")
            requireRef(requirementRef, "requirementRef")
            acknowledgementRequirementRefOrNull?.let {
                requireRef(it, "acknowledgementRequirementRefOrNull")
            }
            requireRefs(
                requirementBindingRefs,
                "requirementBindingRefs",
                allowEmpty = false,
            )
            requireRefs(
                candidateSourceBindingRefs,
                "candidateSourceBindingRefs",
                allowEmpty = false,
            )
            requireRefs(
                subjectScopeRefs,
                "subjectScopeRefs",
                allowEmpty = true,
            )
        }
    }

    @Serializable
    data class AcknowledgedClaim(
        val claim: Claim,
        val acknowledgementRef: String,
        val actorRef: String,
        val sourceResolutionObservedByAcknowledgement: Boolean = false,
        val effectAuthorityGranted: Boolean = false,
    ) {
        init {
            require(!sourceResolutionObservedByAcknowledgement) {
                "attention acknowledgement cannot claim source resolution"
            }
            require(!effectAuthorityGranted) {
                "attention acknowledgement cannot grant effect authority"
            }
        }
    }

    /**
     * A deterministic inspectable surface only.
     *
     * Lexical requirement identity order is not urgency, severity, priority, or presentation order.
     */
    @Serializable
    data class Surface(
        val schemaVersion: String = SurfaceSchema,
        val claims: List<Claim>,
        val effectAuthorityGranted: Boolean = false,
        val displacementPerformed: Boolean = false,
    ) {
        init {
            require(schemaVersion == SurfaceSchema) {
                "attention claim surface schema is not supported"
            }
            require(
                claims.map(Claim::requirementRef).distinct().size == claims.size,
            ) {
                "attention claim requirementRef must be unique"
            }
            require(!effectAuthorityGranted) {
                "attention claim surface cannot grant effect authority"
            }
            require(!displacementPerformed) {
                "attention claim core cannot displace Working Focus"
            }
        }
    }

    fun form(
        candidate: VexLifeAttentionPerceptibility.Candidate,
        requirement: SourceRequirement,
    ): Claim {
        require(requirement.ownerRef == candidate.ownerRef) {
            "attention requirement owner does not match current candidate owner"
        }
        require(requirement.sourceRef == candidate.sourceRef) {
            "attention requirement source does not match current candidate source"
        }
        require(requirement.candidateRef == candidate.candidateRef) {
            "attention requirement does not bind the current candidateRef"
        }
        requireSameRefs(
            expected = candidate.sourceBindingRefs,
            actual = requirement.candidateSourceBindingRefs,
            label = "attention requirement candidate source binding",
        )
        requireSameRefs(
            expected = candidate.subjectScopeRefs,
            actual = requirement.candidateSubjectScopeRefs,
            label = "attention requirement candidate subject scope",
        )

        return Claim(
            requirementRef = requirement.requirementRef,
            ownerRef = candidate.ownerRef,
            sourceRef = candidate.sourceRef,
            candidateRef = candidate.candidateRef,
            relationshipRef = candidate.relationshipRef,
            subjectScopeRefs = candidate.subjectScopeRefs,
            candidateSourceBindingRefs = candidate.sourceBindingRefs,
            sourceStandingRef = candidate.sourceStandingRef,
            requirementBindingRefs = requirement.requirementBindingRefs,
            requirementStandingRef = requirement.requirementStandingRef,
            acknowledgementRequirementRefOrNull =
                requirement.acknowledgementRequirementRefOrNull,
            deeperSourceRef = candidate.deeperSourceRef,
            reachability = candidate.reachability,
        )
    }

    fun formSurface(
        claims: List<Claim>,
    ): Surface =
        Surface(
            claims = claims.sortedBy(Claim::requirementRef),
        )

    fun acknowledge(
        claim: Claim,
        observation: AcknowledgementObservation,
    ): AcknowledgedClaim {
        require(observation.requirementRef == claim.requirementRef) {
            "attention acknowledgement does not bind the current requirementRef"
        }
        requireSameRefs(
            expected = claim.requirementBindingRefs,
            actual = observation.requirementBindingRefs,
            label = "attention acknowledgement requirement binding",
        )
        require(
            observation.acknowledgementRequirementRefOrNull ==
                claim.acknowledgementRequirementRefOrNull,
        ) {
            "attention acknowledgement requirement identity is stale or mismatched"
        }
        requireSameRefs(
            expected = claim.candidateSourceBindingRefs,
            actual = observation.candidateSourceBindingRefs,
            label = "attention acknowledgement candidate source binding",
        )
        requireSameRefs(
            expected = claim.subjectScopeRefs,
            actual = observation.subjectScopeRefs,
            label = "attention acknowledgement subject scope",
        )

        return AcknowledgedClaim(
            claim = claim,
            acknowledgementRef = observation.acknowledgementRef,
            actorRef = observation.actorRef,
        )
    }

    private fun requireSameRefs(
        expected: List<String>,
        actual: List<String>,
        label: String,
    ) {
        require(expected.toSet() == actual.toSet()) {
            "$label is stale or mismatched"
        }
    }

    private fun requireRefs(
        values: List<String>,
        label: String,
        allowEmpty: Boolean,
    ) {
        require(allowEmpty || values.isNotEmpty()) {
            "$label must not be empty"
        }
        require(values.distinct().size == values.size) {
            "$label must be unique"
        }
        values.forEachIndexed { index, value ->
            requireRef(value, "$label[$index]")
        }
    }

    private fun requireRef(
        value: String,
        label: String,
    ) {
        require(
            value.matches(
                Regex("[A-Za-z0-9][A-Za-z0-9._:/-]*"),
            ),
        ) {
            "$label must be one portable stable ref"
        }
    }
}

// [VXG RealForever]
