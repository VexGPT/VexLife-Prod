package vexlife.continuity

import kotlinx.serialization.Serializable

/**
 * Portable semantic shape for a refreshed Vex present working carry when exact Conversation material
 * is about to leave direct model presence.
 *
 * This contract does not decide provider invocation, persistence, currentness ownership, prompt
 * inclusion, Memory admission, relationship agreement or human-profile truth. It only distinguishes
 * a source-bound RELEASE from a source-bound Vex-self-relative CARRY. The draft is a refreshed
 * present state, not an append-only semantic ledger.
 */
object VexLifeWorkingCarryContract {
    const val Schema =
        "vexlife.working-carry/v1"

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
        val previousCarrySourceBindings: List<SourceBinding>,
        val leavingSourceBindings: List<SourceBinding>,
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
        requireStableRef(
            value.companionLineageRef,
            "companionLineageRef",
        )
        requireStableRef(
            value.threadRef,
            "threadRef",
        )
        requireStableRef(
            value.authoredByRef,
            "authoredByRef",
        )

        val previous =
            requireBindings(
                value.previousCarrySourceBindings,
                "previousCarrySourceBindings",
                allowEmpty = true,
            )
        require(
            previous ==
                value.previousCarrySourceBindings,
        ) {
            "working carry previous source bindings must preserve exact order and remain duplicate-free"
        }
        val leaving =
            requireBindings(
                value.leavingSourceBindings,
                "leavingSourceBindings",
                allowEmpty = false,
            )
        require(
            leaving ==
                value.leavingSourceBindings,
        ) {
            "working carry leaving source bindings must preserve exact order and remain duplicate-free"
        }

        val availableByRef =
            linkedMapOf<String, SourceBinding>()
        val availableBySequence =
            linkedMapOf<Long, SourceBinding>()
        (
            previous +
                leaving
        )
            .forEach {
                binding ->
                val priorByRef =
                    availableByRef[
                        binding.eventRef
                    ]
                val priorBySequence =
                    availableBySequence[
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
                    "working carry source generations conflict by eventRef or Conversation sequence"
                }
                availableByRef[
                    binding.eventRef
                ] =
                    binding
                availableBySequence[
                    binding.sequence
                ] =
                    binding
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
            val sources =
                requireBindings(
                    item.sourceBindings,
                    "carryItem.sourceBindings",
                    allowEmpty = false,
                )
            require(
                sources ==
                    item.sourceBindings,
            ) {
                "working carry item source bindings must be canonical and duplicate-free"
            }
            require(
                sources.all {
                    source ->
                    availableByRef[
                        source.eventRef
                    ] ==
                        source
                },
            ) {
                "working carry item source must come from the prior present carry or exact newly leaving range"
            }
        }

        when (
            value.disposition
        ) {
            Release -> {
                require(
                    value.carryItems.isEmpty() &&
                        value.estimatedCarryTokens ==
                        0 &&
                        value.releaseReason ==
                        ReleaseReasonNoPresentCarry,
                ) {
                    "RELEASE means no present working carry remains after refreshing prior carry with the leaving range"
                }
            }

            Carry -> {
                require(
                    value.carryItems.isNotEmpty() &&
                        value.releaseReason ==
                        null,
                ) {
                    "CARRY requires one or more self-relative carry items and no release reason"
                }
            }
        }

        return value
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
     * Preserve the exact observed Conversation order. Determinism must not reorder lived source.
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
                values.size,
        ) {
            "$label must not repeat an eventRef"
        }

        return values
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
