package vexlife.continuity

import kotlinx.serialization.Serializable

/**
 * Portable semantic shape for Vex's present working carry when exact Conversation material is about
 * to leave direct model presence.
 *
 * This contract does not decide provider invocation, persistence, currentness ownership, prompt
 * inclusion, Memory admission, relationship agreement or human-profile truth. It only distinguishes
 * a source-bound RELEASE from a source-bound Vex-self-relative CARRY.
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
        val observedSourceBindings: List<SourceBinding>,
        val disposition: String,
        val carryItems: List<CarryItem>,
        val releaseReason: String?,
        val durableMemoryAuthorityGranted: Boolean = false,
        val humanProfileAuthorityGranted: Boolean = false,
        val relationshipAgreementAuthorityGranted: Boolean = false,
        val canonicalConversationAuthorityGranted: Boolean = false,
        val memoryPromotionPerformed: Boolean = false,
        val effectsPerformed: Boolean = false,
    )

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

        val observed =
            requireBindings(
                value.observedSourceBindings,
                "observedSourceBindings",
            )
        require(
            observed ==
                value.observedSourceBindings,
        ) {
            "working carry observed source bindings must be canonical and duplicate-free"
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

        val observedByRef =
            observed.associateBy(
                SourceBinding::eventRef,
            )

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
                    observedByRef[
                        source.eventRef
                    ] ==
                        source
                },
            ) {
                "working carry item source must be part of the exact observed leaving range"
            }
        }

        when (
            value.disposition
        ) {
            Release -> {
                require(
                    value.carryItems.isEmpty() &&
                        value.releaseReason ==
                        ReleaseReasonNoPresentCarry,
                ) {
                    "RELEASE means no semantic carry remains from the observed range"
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

    /**
     * Preserve the exact observed Conversation order. Determinism must not reorder lived source.
     */
    private fun requireBindings(
        values: List<SourceBinding>,
        label: String,
    ): List<SourceBinding> {
        require(
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
                value.none(
                    Char::isWhitespace,
                ),
        ) {
            "$label must be one non-empty stable ref"
        }
    }
}

// [VXG RealForever]
