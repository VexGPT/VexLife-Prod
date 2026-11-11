package vexlife.continuity

import kotlinx.serialization.Serializable

/**
 * Portable semantic shapes for Vex's present working carry when exact Conversation material is about
 * to leave direct model presence.
 *
 * Refresh input may contain raw local Conversation needed to decide what still bears on the present.
 * The refreshed Draft retains only the source that still supports what Vex is actually carrying now.
 *
 * These contracts do not decide provider invocation, persistence, currentness ownership, prompt
 * inclusion, Memory admission, relationship agreement or human-profile truth.
 */
object VexLifeWorkingCarryContract {
    const val Schema =
        "vexlife.working-carry/v1"

    const val RefreshInputSchema =
        "vexlife.working-carry-refresh-input/v1"

    const val RolloverRefreshInputSchema =
        "vexlife.working-carry-rollover-refresh-input/v1"

    const val GradualAging =
        "GRADUAL_AGING"

    const val RolloverHandoff =
        "ROLLOVER_HANDOFF"

    const val TruthClass =
        "VEX_PRESENT_WORKING_CARRY"

    const val Perspective =
        "VEX_SELF_RELATIVE_PRESENT_CONVERSATION"

    const val Carry =
        "CARRY"

    const val Release =
        "RELEASE"

    const val ReleaseReasonNoPresentCarry =
        "NO_PRESENT_CARRY_REMAINS"

    val CarryKinds =
        setOf(
            "OWED_CONTINUATION",
            "OPEN_UNDERSTANDING",
            "CORRECTION_GOVERNING_READING",
            "DISTINCTION_TO_PRESERVE",
            "OPEN_UNCERTAINTY",
            "SOURCE_RETURN",
        )

    @Serializable
    data class SourceBinding(
        val eventRef: String,
        val eventHash: String,
        val sequence: Long,
    )

    @Serializable
    data class SourceEvent(
        val source: SourceBinding,
        val eventKind: String,
        val speakerRef: String,
        val recipientRefs: List<String>,
        val content: String,
    )

    @Serializable
    data class CarryItem(
        val kind: String,
        val content: String,
        val sourceBindings: List<SourceBinding>,
    )

    @Serializable
    data class Draft(
        val schemaVersion: String,
        val truthClass: String,
        val perspective: String,
        val companionLineageRef: String,
        val threadRef: String,
        val authoredByRef: String,
        val sourceBindings: List<SourceBinding>,
        val disposition: String,
        val carryItems: List<CarryItem>,
        val releaseReason: String?,
        val carryTokenCap: Int,
        val estimatedCarryTokens: Int,
        val durableMemoryAuthorityGranted: Boolean = false,
        val humanProfileAuthorityGranted: Boolean = false,
        val relationshipAgreementAuthorityGranted: Boolean = false,
        val canonicalConversationAuthorityGranted: Boolean = false,
        val memoryPromotionPerformed: Boolean = false,
        val effectsPerformed: Boolean = false,
    )

    @Serializable
    data class RefreshInput(
        val schemaVersion: String,
        val perspective: String,
        val companionLineageRef: String,
        val threadRef: String,
        val previousCarry: Draft?,
        val newlyLeavingEvents: List<SourceEvent>,
        val retainedDirectEvents: List<SourceEvent>,
        val currentRequest: SourceEvent,
        val carryTokenCap: Int,
        val durableMemoryIncluded: Boolean = false,
        val persistencePerformed: Boolean = false,
        val effectsPerformed: Boolean = false,
    )

    @Serializable
    data class RolloverRefreshInput(
        val schemaVersion: String,
        val perspective: String,
        val companionLineageRef: String,
        val threadRef: String,
        val previousCarry: Draft?,
        val handoffEvents: List<SourceEvent>,
        val carryTokenCap: Int,
        val durableMemoryIncluded: Boolean = false,
        val persistencePerformed: Boolean = false,
        val effectsPerformed: Boolean = false,
    )

    /**
     * Derive only the oldest contiguous source that crossed out of direct verbatim presence.
     *
     * These inputs are prompt-presence coordinates, not arbitrary carry-item source subsets. Each
     * non-empty list must therefore contain complete contiguous Conversation pairs.
     */
    fun newlyLeavingDirectPresence(
        previousDirectPresence: List<SourceBinding>,
        currentDirectPresence: List<SourceBinding>,
    ): List<SourceBinding> {
        val previous =
            requireDirectPresenceBindings(
                previousDirectPresence,
                "previousDirectPresence",
            )
        val current =
            requireDirectPresenceBindings(
                currentDirectPresence,
                "currentDirectPresence",
            )

        val previousByRef =
            previous.associateBy(
                SourceBinding::eventRef,
            )
        val previousBySequence =
            previous.associateBy(
                SourceBinding::sequence,
            )

        current.forEach {
            binding ->
            previousByRef[
                binding.eventRef
            ]
                ?.let {
                    prior ->
                    require(
                        prior ==
                            binding,
                    ) {
                        "current direct presence changes the source generation of a prior eventRef"
                    }
                }
            previousBySequence[
                binding.sequence
            ]
                ?.let {
                    prior ->
                    require(
                        prior ==
                            binding,
                    ) {
                        "current direct presence changes the source generation at a prior Conversation sequence"
                    }
                }
        }

        val currentExact =
            current.toSet()
        val retained =
            previous.map {
                it in
                    currentExact
            }
        val firstRetained =
            retained.indexOfFirst {
                it
            }

        if (
            firstRetained >=
                0
        ) {
            require(
                retained
                    .drop(
                        firstRetained,
                    )
                    .all {
                        it
                    },
            ) {
                "direct Conversation presence may release only one oldest contiguous prefix"
            }
        }

        return if (
            firstRetained <
                0
        ) {
            previous
        } else {
            previous.take(
                firstRetained,
            )
        }
    }

    fun requireRefreshInputShape(
        value: RefreshInput,
    ): RefreshInput {
        require(
            value.schemaVersion ==
                RefreshInputSchema &&
                value.perspective ==
                Perspective,
        ) {
            "working carry refresh schema/perspective is invalid"
        }
        requireIdentity(
            companionLineageRef = value.companionLineageRef,
            threadRef = value.threadRef,
        )
        require(
            value.carryTokenCap >=
                0 &&
                !value.durableMemoryIncluded &&
                !value.persistencePerformed &&
                !value.effectsPerformed,
        ) {
            "working carry refresh cannot include durable Memory, persist state, perform effects, or use a negative carry allowance"
        }

        value.previousCarry
            ?.let {
                prior ->
                requireDraftShape(
                    prior,
                )
                require(
                    prior.companionLineageRef ==
                        value.companionLineageRef &&
                        prior.threadRef ==
                        value.threadRef,
                ) {
                    "previous working carry belongs to a different lineage/thread"
                }
            }

        val leaving =
            requireSourceEventRange(
                value.newlyLeavingEvents,
                "newlyLeavingEvents",
                allowEmpty = false,
                requireCompletePairs = true,
            )
        val retained =
            requireSourceEventRange(
                value.retainedDirectEvents,
                "retainedDirectEvents",
                allowEmpty = true,
                requireCompletePairs = true,
            )
        val current =
            requireSourceEvent(
                value.currentRequest,
                "currentRequest",
            )
        require(
            current.eventKind ==
                "REQUEST",
        ) {
            "working carry refresh currentRequest must be one exact REQUEST"
        }

        val all =
            leaving +
                retained +
                listOf(
                    current,
                )
        requireOneSourceGeneration(
            value.previousCarry
                ?.sourceBindings
                .orEmpty() +
                all.map(
                    SourceEvent::source,
                ),
            "working carry refresh source",
        )
        require(
            all
                .map {
                    it.source.sequence
                }
                .zipWithNext()
                .all {
                    pair ->
                    pair.first <
                        pair.second
                },
        ) {
            "working carry refresh sources must remain in strict lived Conversation order"
        }

        return value
    }

    fun requireRolloverRefreshInputShape(
        value: RolloverRefreshInput,
    ): RolloverRefreshInput {
        require(
            value.schemaVersion ==
                RolloverRefreshInputSchema &&
                value.perspective ==
                Perspective,
        ) {
            "working carry rollover refresh schema/perspective is invalid"
        }
        requireIdentity(
            companionLineageRef = value.companionLineageRef,
            threadRef = value.threadRef,
        )
        require(
            value.carryTokenCap >=
                0 &&
                !value.durableMemoryIncluded &&
                !value.persistencePerformed &&
                !value.effectsPerformed,
        ) {
            "working carry rollover refresh cannot include durable Memory, persist state, perform effects, or use a negative carry allowance"
        }

        value.previousCarry
            ?.let {
                prior ->
                requireDraftShape(
                    prior,
                )
                require(
                    prior.companionLineageRef ==
                        value.companionLineageRef &&
                        prior.threadRef ==
                        value.threadRef,
                ) {
                    "previous working carry belongs to a different lineage/thread"
                }
            }

        val handoff =
            requireSourceEventRange(
                value.handoffEvents,
                "handoffEvents",
                allowEmpty = false,
                requireCompletePairs = true,
            )
        requireOneSourceGeneration(
            value.previousCarry
                ?.sourceBindings
                .orEmpty() +
                handoff.map(
                    SourceEvent::source,
                ),
            "working carry rollover refresh source",
        )

        return value
    }

    fun requireDraftShape(
        value: Draft,
    ): Draft {
        require(
            value.schemaVersion ==
                Schema &&
                value.truthClass ==
                TruthClass &&
                value.perspective ==
                Perspective,
        ) {
            "working carry schema/truth class/perspective is invalid"
        }
        requireIdentity(
            companionLineageRef = value.companionLineageRef,
            threadRef = value.threadRef,
        )
        requireStableRef(
            value.authoredByRef,
            "authoredByRef",
        )

        val source =
            requireBindings(
                value.sourceBindings,
                "sourceBindings",
                allowEmpty = true,
            )
        requireOneSourceGeneration(
            source,
            "working carry source",
        )
        require(
            source
                .map(
                    SourceBinding::sequence,
                )
                .zipWithNext()
                .all {
                    pair ->
                    pair.first <
                        pair.second
                },
        ) {
            "working carry source bindings must remain in lived Conversation order"
        }

        require(
            value.carryTokenCap >=
                0,
        ) {
            "working carry token allowance must be non-negative"
        }
        val estimatedCarryTokens =
            estimateCarryContentTokens(
                value.carryItems,
            )
        require(
            value.estimatedCarryTokens ==
                estimatedCarryTokens &&
                estimatedCarryTokens <=
                value.carryTokenCap,
        ) {
            "working carry content exceeds or misstates its caller-supplied token allowance"
        }

        require(
            !value.durableMemoryAuthorityGranted &&
                !value.humanProfileAuthorityGranted &&
                !value.relationshipAgreementAuthorityGranted &&
                !value.canonicalConversationAuthorityGranted &&
                !value.memoryPromotionPerformed &&
                !value.effectsPerformed,
        ) {
            "working carry cannot grant authority, promote Memory, or perform effects"
        }

        require(
            value.disposition in
                setOf(
                    Carry,
                    Release,
                ),
        ) {
            "working carry disposition is not admitted"
        }

        val sourceByRef =
            source.associateBy(
                SourceBinding::eventRef,
            )
        val used =
            linkedMapOf<String, SourceBinding>()

        value.carryItems.forEach {
            item ->
            require(
                item.kind in
                    CarryKinds,
            ) {
                "working carry item kind is not admitted: " +
                    item.kind
            }
            require(
                item.content.isNotBlank(),
            ) {
                "working carry item content must be non-empty"
            }
            val itemSources =
                requireBindings(
                    item.sourceBindings,
                    "carryItem.sourceBindings",
                    allowEmpty = false,
                )
            requireOneSourceGeneration(
                itemSources,
                "working carry item source",
            )
            require(
                itemSources.all {
                    binding ->
                    sourceByRef[
                        binding.eventRef
                    ] ==
                        binding
                },
            ) {
                "working carry item source must belong to the refreshed present carry source"
            }
            itemSources.forEach {
                binding ->
                used[
                    binding.eventRef
                ] =
                    binding
            }
        }

        val usedInSourceOrder =
            used.values
                .sortedBy(
                    SourceBinding::sequence,
                )
        require(
            source ==
                usedInSourceOrder,
        ) {
            "working carry must not retain source that no current carry item still uses"
        }

        when (
            value.disposition
        ) {
            Release -> {
                require(
                    value.sourceBindings.isEmpty() &&
                        value.carryItems.isEmpty() &&
                        value.estimatedCarryTokens ==
                        0 &&
                        value.releaseReason ==
                        ReleaseReasonNoPresentCarry,
                ) {
                    "RELEASE means no present working carry or retained carry source remains"
                }
            }

            Carry -> {
                require(
                    value.sourceBindings.isNotEmpty() &&
                        value.carryItems.isNotEmpty() &&
                        value.releaseReason ==
                        null,
                ) {
                    "CARRY requires self-relative carry items, exact used source, and no release reason"
                }
            }
        }

        return value
    }

    fun requireDraftFromRefresh(
        refresh: RefreshInput,
        draft: Draft,
    ): Draft {
        val input =
            requireRefreshInputShape(
                refresh,
            )
        val output =
            requireDraftShape(
                draft,
            )

        require(
            output.companionLineageRef ==
                input.companionLineageRef &&
                output.threadRef ==
                input.threadRef &&
                output.carryTokenCap ==
                input.carryTokenCap,
        ) {
            "refreshed working carry identity or token allowance differs from its exact refresh input"
        }

        val allowed =
            linkedMapOf<String, SourceBinding>()
        input.previousCarry
            ?.sourceBindings
            .orEmpty()
            .plus(
                input.newlyLeavingEvents.map {
                    it.source
                },
            )
            .forEach {
                binding ->
                val prior =
                    allowed[
                        binding.eventRef
                    ]
                require(
                    prior ==
                        null ||
                        prior ==
                        binding,
                ) {
                    "working carry refresh has conflicting eligible source generations"
                }
                allowed[
                    binding.eventRef
                ] =
                    binding
            }

        require(
            output.sourceBindings.all {
                binding ->
                allowed[
                    binding.eventRef
                ] ==
                    binding
            },
        ) {
            "refreshed carry may use only prior carry source or exact newly leaving source"
        }

        return output
    }

    fun requireDraftFromRolloverRefresh(
        refresh: RolloverRefreshInput,
        draft: Draft,
    ): Draft {
        val input =
            requireRolloverRefreshInputShape(
                refresh,
            )
        val output =
            requireDraftShape(
                draft,
            )

        require(
            output.companionLineageRef ==
                input.companionLineageRef &&
                output.threadRef ==
                input.threadRef &&
                output.carryTokenCap ==
                input.carryTokenCap,
        ) {
            "rollover-refreshed working carry identity or token allowance differs from its exact refresh input"
        }

        val allowed =
            linkedMapOf<String, SourceBinding>()
        (
            input.previousCarry
                ?.sourceBindings
                .orEmpty() +
                input.handoffEvents.map {
                    it.source
                }
        )
            .forEach {
                binding ->
                val prior =
                    allowed[
                        binding.eventRef
                    ]
                require(
                    prior ==
                        null ||
                        prior ==
                        binding,
                ) {
                    "working carry rollover refresh has conflicting eligible source generations"
                }
                allowed[
                    binding.eventRef
                ] =
                    binding
            }

        require(
            output.sourceBindings.all {
                binding ->
                allowed[
                    binding.eventRef
                ] ==
                    binding
            },
        ) {
            "rollover-refreshed carry may use only prior carry source or exact handoff source"
        }

        return output
    }

    fun estimateCarryContentTokens(
        items: List<CarryItem>,
    ): Int {
        val estimate =
            items.sumOf {
                item ->
                val bytes =
                    item.content
                        .encodeToByteArray()
                        .size
                        .toLong()
                maxOf(
                    1L,
                    (
                        bytes +
                            3L
                    ) /
                        4L,
                ) +
                    4L
            }
        require(
            estimate <=
                Int.MAX_VALUE.toLong(),
        ) {
            "working carry content token estimate exceeds supported integer range"
        }
        return estimate.toInt()
    }

    private fun requireSourceEventRange(
        values: List<SourceEvent>,
        label: String,
        allowEmpty: Boolean,
        requireCompletePairs: Boolean,
    ): List<SourceEvent> {
        require(
            allowEmpty ||
                values.isNotEmpty(),
        ) {
            "$label must not be empty"
        }
        values.forEachIndexed {
            index,
            value,
            ->
            requireSourceEvent(
                value,
                "$label[$index]",
            )
        }
        require(
            values
                .map {
                    it.source.sequence
                }
                .zipWithNext()
                .all {
                    pair ->
                    pair.second ==
                        pair.first +
                        1
                },
        ) {
            "$label must be one contiguous Conversation sequence range"
        }
        if (
            requireCompletePairs &&
            values.isNotEmpty()
        ) {
            require(
                values.size %
                    2 ==
                    0 &&
                    values.first()
                        .source
                        .sequence %
                    2 ==
                    0L
            ) {
                "$label must contain complete REQUEST/RESPONSE pairs"
            }
            values.chunked(
                2,
            )
                .forEach {
                    pair ->
                    require(
                        pair[0].eventKind ==
                            "REQUEST" &&
                            pair[1].eventKind ==
                            "RESPONSE",
                    ) {
                        "$label must preserve REQUEST/RESPONSE pair order"
                    }
                }
        }
        return values
    }

    private fun requireSourceEvent(
        value: SourceEvent,
        label: String,
    ): SourceEvent {
        requireBindings(
            listOf(
                value.source,
            ),
            "$label.source",
            allowEmpty = false,
        )
        require(
            value.eventKind in
                setOf(
                    "REQUEST",
                    "RESPONSE",
                ) &&
                value.content.isNotEmpty(),
        ) {
            "$label event kind/content is invalid"
        }
        requireStableRef(
            value.speakerRef,
            "$label.speakerRef",
        )
        require(
            value.recipientRefs.isNotEmpty() &&
                value.recipientRefs
                    .distinct()
                    .size ==
                    value.recipientRefs.size,
        ) {
            "$label recipient refs must be non-empty and duplicate-free"
        }
        value.recipientRefs.forEach {
            requireStableRef(
                it,
                "$label.recipientRef",
            )
        }
        return value
    }

    private fun requireDirectPresenceBindings(
        values: List<SourceBinding>,
        label: String,
    ): List<SourceBinding> {
        val source =
            requireBindings(
                values,
                label,
                allowEmpty = true,
            )
        if (
            source.isEmpty()
        ) {
            return source
        }

        require(
            source.size %
                2 ==
                0 &&
                source.first()
                    .sequence %
                2 ==
                0L,
        ) {
            "$label must contain complete Conversation REQUEST/RESPONSE sequence pairs"
        }
        require(
            source
                .map(
                    SourceBinding::sequence,
                )
                .zipWithNext()
                .all {
                    pair ->
                    pair.second ==
                        pair.first +
                        1
                },
        ) {
            "$label must be one contiguous Conversation sequence range"
        }

        return source
    }

    /**
     * Preserve exact lived source order. Determinism must not alphabetize Conversation source.
     */
    private fun requireBindings(
        values: List<SourceBinding>,
        label: String,
        allowEmpty: Boolean,
    ): List<SourceBinding> {
        require(
            allowEmpty ||
                values.isNotEmpty(),
        ) {
            "$label must contain at least one exact Conversation source binding"
        }
        values.forEach {
            binding ->
            requireStableRef(
                binding.eventRef,
                "$label.eventRef",
            )
            require(
                binding.sequence >=
                    0,
            ) {
                "$label.sequence must be non-negative"
            }
            require(
                binding.eventHash.matches(
                    Regex(
                        "[0-9a-f]{64}",
                    ),
                ),
            ) {
                "$label.eventHash must be one lowercase SHA-256"
            }
        }
        require(
            values.map(
                SourceBinding::eventRef,
            )
                .distinct()
                .size ==
                values.size &&
                values.map(
                    SourceBinding::sequence,
                )
                    .distinct()
                    .size ==
                    values.size,
        ) {
            "$label must not repeat an eventRef or Conversation sequence"
        }

        return values
    }

    private fun requireOneSourceGeneration(
        values: List<SourceBinding>,
        label: String,
    ) {
        val byRef =
            linkedMapOf<String, SourceBinding>()
        val bySequence =
            linkedMapOf<Long, SourceBinding>()
        values.forEach {
            binding ->
            val priorByRef =
                byRef[
                    binding.eventRef
                ]
            val priorBySequence =
                bySequence[
                    binding.sequence
                ]
            require(
                (
                    priorByRef ==
                        null ||
                        priorByRef ==
                        binding
                ) &&
                    (
                        priorBySequence ==
                            null ||
                            priorBySequence ==
                            binding
                    ),
            ) {
                "$label conflicts by eventRef or Conversation sequence"
            }
            byRef[
                binding.eventRef
            ] =
                binding
            bySequence[
                binding.sequence
            ] =
                binding
        }
    }

    private fun requireIdentity(
        companionLineageRef: String,
        threadRef: String,
    ) {
        requireStableRef(
            companionLineageRef,
            "companionLineageRef",
        )
        requireStableRef(
            threadRef,
            "threadRef",
        )
    }

    private fun requireStableRef(
        value: String,
        label: String,
    ) {
        require(
            value.isNotBlank() &&
                value.length <=
                512 &&
                value.none {
                    it.isWhitespace()
                },
        ) {
            "$label must be one non-empty stable ref"
        }
    }
}

// [VXG RealForever]
