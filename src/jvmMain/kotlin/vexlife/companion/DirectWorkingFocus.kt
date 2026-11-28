package vexlife.companion

import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.security.MessageDigest
import kotlinx.serialization.Serializable
import vexlife.contract.VexContractJson
import vexlife.continuity.VexLifeWorkingFocus
import vexlife.continuity.VexLifeWorkingFocusOwner

/**
 * Read/currentness bridge and bounded provider projection for Working Focus.
 *
 * This owner does not persist Working Focus, admit prompt placement, mutate Conversation/Memory,
 * call a provider/model, or decide attention. It only:
 *
 * 1. observes the exact durable Working Focus owner for one lineage/thread;
 * 2. lets a later caller revalidate that exact observation; and
 * 3. projects ACTIVE focus into one caller-bounded declarative prompt representation.
 *
 * Durable Working Focus may contain more relationship/edge detail than one prompt should carry.
 * Projection therefore admits either all collection detail or none of it. It never includes a
 * lexical prefix of relationships/open edges, so deterministic identity order cannot silently
 * become semantic priority.
 */
internal object DirectWorkingFocus {
    private const val PromptSchema =
        "vexlife-prod.direct-working-focus-prompt/v1"
    private const val PromptHeader =
        "VEXLIFE_WORKING_FOCUS/v1"

    enum class CurrentStanding {
        ABSENT,
        ACTIVE,
        DISPLACED,
        CLEARED,
    }

    enum class ProjectionState {
        INCLUDED_FULL,
        INCLUDED_CORE_ONLY,
        HELD_TOKEN_BUDGET,
        NOT_ACTIVE,
    }

    @ConsistentCopyVisibility
    data class Observation internal constructor(
        val standing: CurrentStanding,
        internal val current: VexLifeWorkingFocusOwner.Current?,
    ) {
        val stateRefOrNull: String?
            get() = current?.stateRef

        val revisionRefOrNull: String?
            get() = current?.revisionRefOrNull

        val workingFocusRefOrNull: String?
            get() = current?.revisionOrNull?.workingFocusRef
    }

    @ConsistentCopyVisibility
    data class Projection internal constructor(
        val state: ProjectionState,
        val observedStanding: CurrentStanding,
        val workingFocusRefOrNull: String?,
        val stateRefOrNull: String?,
        val revisionRefOrNull: String?,
        val contentOrNull: String?,
        val contentSha256OrNull: String?,
        val estimatedTokens: Int,
        val requiredCoreTokens: Int,
        val detailsIncluded: Boolean,
        val heldRelationshipRefs: List<String>,
        val heldOpenEdgeRefs: List<String>,
        val sourceRefs: List<String>,
        val effectsPerformed: Boolean = false,
    ) {
        init {
            require(estimatedTokens >= 0)
            require(requiredCoreTokens >= 0)
            require(!effectsPerformed) {
                "Working Focus prompt projection cannot perform effects"
            }
            if (contentOrNull == null) {
                require(contentSha256OrNull == null && estimatedTokens == 0) {
                    "absent Working Focus prompt bytes cannot claim a digest/token cost"
                }
            } else {
                require(
                    contentSha256OrNull != null &&
                        contentSha256OrNull.matches(Regex("[0-9a-f]{64}")),
                ) {
                    "resident Working Focus prompt bytes require one exact SHA-256"
                }
            }
        }
    }

    @Serializable
    private data class PromptRelationship(
        val relationshipRef: String,
        val ownerRef: String,
        val standing: VexLifeWorkingFocus.Standing,
        val compactMeaning: String,
        val sourceRefs: List<String>,
    )

    @Serializable
    private data class PromptOpenEdge(
        val edgeRef: String,
        val ownerRef: String,
        val compactMeaning: String,
        val sourceRefs: List<String>,
    )

    @Serializable
    private data class PromptPacket(
        val schemaVersion: String = PromptSchema,
        val truthClass: String = "CURRENT_DECLARATIVE_WORKING_FOCUS",
        val workingFocusRef: String,
        val stateRef: String,
        val revisionRef: String,
        val subjectScopeRefs: List<String>,
        val objective: String,
        val currentQuestionOrNull: String?,
        val nextProbeOrNull: String?,
        val sourceRefs: List<String>,
        val retainedRelationshipRefs: List<String>,
        val openEdgeRefs: List<String>,
        val detailStanding: String,
        val retainedRelationships: List<PromptRelationship>,
        val openEdges: List<PromptOpenEdge>,
        val effectAuthorityGranted: Boolean = false,
    ) {
        init {
            require(!effectAuthorityGranted) {
                "Working Focus prompt projection cannot grant effect authority"
            }
        }
    }

    fun observeCurrent(
        productHome: Path,
        lineageRef: String,
        threadRef: String,
    ): Observation {
        val current =
            VexLifeWorkingFocusOwner(
                productHome,
            ).readCurrent(
                lineageRef = lineageRef,
                threadRef = threadRef,
            )
                ?: return Observation(
                    standing = CurrentStanding.ABSENT,
                    current = null,
                )

        val standing =
            when (current.state.state) {
                VexLifeWorkingFocusOwner.FocusState.ACTIVE ->
                    CurrentStanding.ACTIVE
                VexLifeWorkingFocusOwner.FocusState.DISPLACED ->
                    CurrentStanding.DISPLACED
                VexLifeWorkingFocusOwner.FocusState.CLEARED ->
                    CurrentStanding.CLEARED
            }

        return Observation(
            standing = standing,
            current = current,
        )
    }

    /**
     * Re-read exact current Working Focus and require byte/identity-equivalent owner truth.
     *
     * A later prompt/turn integration should call this at the provider boundary rather than treating
     * an earlier detached observation as authority.
     */
    fun requireCurrent(
        productHome: Path,
        lineageRef: String,
        threadRef: String,
        expected: Observation,
    ) {
        require(
            observeCurrent(
                productHome = productHome,
                lineageRef = lineageRef,
                threadRef = threadRef,
            ) == expected,
        ) {
            "Working Focus changed after observation"
        }
    }

    /**
     * Project ACTIVE Working Focus beneath one explicit caller-supplied token ceiling.
     *
     * FULL contains every retained relationship/open-edge compact meaning and its exact source
     * coordinates. CORE_ONLY keeps objective/question/next-probe, the focus-level exact source
     * coordinates and every relationship/edge identity, while holding all per-item detail/source
     * bodies together. If even CORE_ONLY does not fit, no Working
     * Focus bytes are returned and durable currentness is unchanged.
     */
    fun project(
        observation: Observation,
        availableTokens: Int,
        estimateTokens: (String) -> Int,
    ): Projection {
        require(availableTokens >= 0) {
            "Working Focus prompt availableTokens must be nonnegative"
        }

        if (
            observation.standing != CurrentStanding.ACTIVE
        ) {
            return Projection(
                state = ProjectionState.NOT_ACTIVE,
                observedStanding = observation.standing,
                workingFocusRefOrNull = observation.workingFocusRefOrNull,
                stateRefOrNull = observation.stateRefOrNull,
                revisionRefOrNull = observation.revisionRefOrNull,
                contentOrNull = null,
                contentSha256OrNull = null,
                estimatedTokens = 0,
                requiredCoreTokens = 0,
                detailsIncluded = false,
                heldRelationshipRefs = emptyList(),
                heldOpenEdgeRefs = emptyList(),
                sourceRefs = emptyList(),
            )
        }

        val current =
            requireNotNull(
                observation.current,
            ) {
                "ACTIVE Working Focus observation has no current owner state"
            }
        val revision =
            requireNotNull(
                current.revisionOrNull,
            ) {
                "ACTIVE Working Focus observation has no current revision"
            }
        val revisionRef =
            requireNotNull(
                current.revisionRefOrNull,
            ) {
                "ACTIVE Working Focus observation has no revision ref"
            }

        val relationshipRefs =
            revision.retainedRelationships
                .map(VexLifeWorkingFocus.Relationship::relationshipRef)
                .sorted()
        val openEdgeRefs =
            revision.openEdges
                .map(VexLifeWorkingFocus.OpenEdge::edgeRef)
                .sorted()

        val corePacket =
            promptPacket(
                current = current,
                revision = revision,
                revisionRef = revisionRef,
                detailStanding = "HELD_TOKEN_BUDGET",
                relationships = emptyList(),
                openEdges = emptyList(),
            )
        val coreContent =
            promptContent(
                corePacket,
            )
        val coreTokens =
            measuredTokens(
                coreContent,
                estimateTokens,
            )

        val fullPacket =
            promptPacket(
                current = current,
                revision = revision,
                revisionRef = revisionRef,
                detailStanding = "FULL",
                relationships =
                    revision.retainedRelationships
                        .sortedBy {
                            it.relationshipRef
                        }
                        .map {
                            PromptRelationship(
                                relationshipRef = it.relationshipRef,
                                ownerRef = it.ownerRef,
                                standing = it.standing,
                                compactMeaning = it.compactMeaning,
                                sourceRefs = it.sourceRefs.sorted(),
                            )
                        },
                openEdges =
                    revision.openEdges
                        .sortedBy {
                            it.edgeRef
                        }
                        .map {
                            PromptOpenEdge(
                                edgeRef = it.edgeRef,
                                ownerRef = it.ownerRef,
                                compactMeaning = it.compactMeaning,
                                sourceRefs = it.sourceRefs.sorted(),
                            )
                        },
            )
        val fullContent =
            promptContent(
                fullPacket,
            )
        val fullTokens =
            measuredTokens(
                fullContent,
                estimateTokens,
            )

        if (
            fullTokens <=
                availableTokens
        ) {
            return residentProjection(
                state = ProjectionState.INCLUDED_FULL,
                observation = observation,
                content = fullContent,
                estimatedTokens = fullTokens,
                requiredCoreTokens = coreTokens,
                detailsIncluded = true,
                heldRelationshipRefs = emptyList(),
                heldOpenEdgeRefs = emptyList(),
                sourceRefs =
                    (
                        listOf(
                            current.stateRef,
                            revisionRef,
                        ) +
                            revision.sourceRefs +
                            revision.retainedRelationships.flatMap {
                                it.sourceRefs
                            } +
                            revision.openEdges.flatMap {
                                it.sourceRefs
                            }
                    )
                        .distinct()
                        .sorted(),
            )
        }

        if (
            coreTokens <=
                availableTokens
        ) {
            return residentProjection(
                state = ProjectionState.INCLUDED_CORE_ONLY,
                observation = observation,
                content = coreContent,
                estimatedTokens = coreTokens,
                requiredCoreTokens = coreTokens,
                detailsIncluded = false,
                heldRelationshipRefs = relationshipRefs,
                heldOpenEdgeRefs = openEdgeRefs,
                sourceRefs =
                    (
                        listOf(
                            current.stateRef,
                            revisionRef,
                        ) +
                            revision.sourceRefs
                    )
                        .distinct()
                        .sorted(),
            )
        }

        return Projection(
            state = ProjectionState.HELD_TOKEN_BUDGET,
            observedStanding = observation.standing,
            workingFocusRefOrNull = revision.workingFocusRef,
            stateRefOrNull = current.stateRef,
            revisionRefOrNull = revisionRef,
            contentOrNull = null,
            contentSha256OrNull = null,
            estimatedTokens = 0,
            requiredCoreTokens = coreTokens,
            detailsIncluded = false,
            heldRelationshipRefs = relationshipRefs,
            heldOpenEdgeRefs = openEdgeRefs,
            sourceRefs =
                (
                    listOf(
                        current.stateRef,
                        revisionRef,
                    ) +
                        revision.sourceRefs
                )
                    .distinct()
                    .sorted(),
        )
    }

    private fun promptPacket(
        current: VexLifeWorkingFocusOwner.Current,
        revision: VexLifeWorkingFocusOwner.FocusRevision,
        revisionRef: String,
        detailStanding: String,
        relationships: List<PromptRelationship>,
        openEdges: List<PromptOpenEdge>,
    ): PromptPacket =
        PromptPacket(
            workingFocusRef = revision.workingFocusRef,
            stateRef = current.stateRef,
            revisionRef = revisionRef,
            subjectScopeRefs = revision.subjectScopeRefs,
            objective = revision.objective,
            currentQuestionOrNull = revision.currentQuestionOrNull,
            nextProbeOrNull = revision.nextProbeOrNull,
            sourceRefs = revision.sourceRefs.sorted(),
            retainedRelationshipRefs =
                revision.retainedRelationships
                    .map(VexLifeWorkingFocus.Relationship::relationshipRef)
                    .sorted(),
            openEdgeRefs =
                revision.openEdges
                    .map(VexLifeWorkingFocus.OpenEdge::edgeRef)
                    .sorted(),
            detailStanding = detailStanding,
            retainedRelationships = relationships,
            openEdges = openEdges,
        )

    private fun promptContent(
        packet: PromptPacket,
    ): String =
        listOf(
            PromptHeader,
            "This is Vex's bounded declarative current work. It is not Conversation, semantic Memory, source truth, private reasoning, Attention priority, or effect authority.",
            "OBSERVED / HYPOTHESIS / UNRESOLVED standing belongs only to the retained relationship that names it. Exact source remains separately owned and may be reopened when needed. Source refs are addresses, not read or effect authority.",
            VexContractJson.canonicalEncode(
                packet,
            ),
        )
            .joinToString(
                "\n",
            )

    private fun residentProjection(
        state: ProjectionState,
        observation: Observation,
        content: String,
        estimatedTokens: Int,
        requiredCoreTokens: Int,
        detailsIncluded: Boolean,
        heldRelationshipRefs: List<String>,
        heldOpenEdgeRefs: List<String>,
        sourceRefs: List<String>,
    ): Projection =
        Projection(
            state = state,
            observedStanding = observation.standing,
            workingFocusRefOrNull = observation.workingFocusRefOrNull,
            stateRefOrNull = observation.stateRefOrNull,
            revisionRefOrNull = observation.revisionRefOrNull,
            contentOrNull = content,
            contentSha256OrNull = sha256(content),
            estimatedTokens = estimatedTokens,
            requiredCoreTokens = requiredCoreTokens,
            detailsIncluded = detailsIncluded,
            heldRelationshipRefs = heldRelationshipRefs,
            heldOpenEdgeRefs = heldOpenEdgeRefs,
            sourceRefs = sourceRefs,
        )

    private fun measuredTokens(
        content: String,
        estimateTokens: (String) -> Int,
    ): Int =
        estimateTokens(
            content,
        )
            .also {
                require(it >= 0) {
                    "Working Focus prompt token estimator returned a negative value"
                }
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
                "%02x".format(it)
            }
}

// [VXG RealForever]
