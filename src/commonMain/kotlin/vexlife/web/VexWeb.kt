package vexlife.web

import vexlife.blueprint.VexUiElementRef

/**
 * First compiler-visible browser-manifestation vocabulary pressured by the accepted completed-view
 * blueprint.
 *
 * VexWeb does not own product State. It relates existing Blueprint identity to manifestation
 * structure, layout/depth intent, interaction, capability binding and honest fallback behavior.
 *
 * This is intentionally pre-generator: HTML/CSS/JS do not appear in this vocabulary.
 */
object VexWeb {
    @JvmInline
    value class StructuralRef private constructor(
        val value: String,
    ) {
        companion object {
            fun of(
                value: String,
            ): StructuralRef {
                require(
                    value.matches(
                        Regex(
                            "vexweb\.[a-z0-9]+(?:[.-][a-z0-9]+)*",
                        ),
                    ),
                ) {
                    "VexWeb structural ref must be stable lowercase source identity: $value"
                }

                return StructuralRef(
                    value,
                )
            }
        }
    }

    @JvmInline
    value class CapabilityRef private constructor(
        val value: String,
    ) {
        companion object {
            fun of(
                value: String,
            ): CapabilityRef {
                require(
                    value.matches(
                        Regex(
                            "[a-z0-9]+(?:[.-][a-z0-9]+)*",
                        ),
                    ),
                ) {
                    "VexWeb capability ref must be stable lowercase source identity: $value"
                }

                return CapabilityRef(
                    value,
                )
            }
        }
    }

    sealed interface Identity {
        val ref: String

        @ConsistentCopyVisibility
        data class Blueprint internal constructor(
            val element: VexUiElementRef,
        ) :
            Identity {
            override val ref: String =
                element.ref
        }

        @ConsistentCopyVisibility
        data class Structural internal constructor(
            val structuralRef: StructuralRef,
        ) :
            Identity {
            override val ref: String =
                structuralRef.value
        }
    }

    /**
     * VexWeb-owned presentation input.
     *
     * VexWeb.Parameter and VexFamily.Parameter intentionally have different owners. This Parameter
     * names a browser-manifestation dimension, not product semantic State.
     */
    sealed interface Parameter {
        @ConsistentCopyVisibility
        data class SplitRatio private constructor(
            val leadingParts: Int,
            val trailingParts: Int,
        ) :
            Parameter {
            init {
                require(
                    leadingParts >
                        0,
                )
                require(
                    trailingParts >
                        0,
                )
            }

            val totalParts: Int =
                leadingParts +
                    trailingParts

            companion object {
                fun of(
                    leadingParts: Int,
                    trailingParts: Int,
                ): SplitRatio =
                    SplitRatio(
                        leadingParts =
                            leadingParts,
                        trailingParts =
                            trailingParts,
                    )

                val Equal =
                    SplitRatio(
                        leadingParts =
                            1,
                        trailingParts =
                            1,
                    )
            }
        }

        sealed interface SplitMode :
            Parameter {
            @ConsistentCopyVisibility
            data class Balanced internal constructor(
                val ratio: SplitRatio =
                    SplitRatio.Equal,
            ) :
                SplitMode

            data object LeadingOnly :
                SplitMode

            data object TrailingOnly :
                SplitMode
        }
    }

    /**
     * Relative authored depth.
     *
     * Declaration order expresses only the stable relative relationship. A renderer may choose
     * platform-specific stacking numbers; those numbers are not VexWeb source meaning.
     */
    enum class Depth {
        Content,
        Chrome,
        Transient,
        Modal,
        ;

        fun isAbove(
            other: Depth,
        ): Boolean =
            entries.indexOf(
                this,
            ) >
                entries.indexOf(
                    other,
                )
    }

    enum class Placement {
        InFlow,
        FillAvailable,
        Leading,
        Trailing,
        Overlay,
    }

    /**
     * Presentation intent, not CSS/widget vocabulary.
     *
     * Add roles only when completed-view pressure requires a distinction.
     */
    enum class Presentation {
        Surface,
        SplitHandle,
        PrimaryAction,
        TransientFeedback,
    }

    enum class Interaction {
        Activate,
        Resize,
        CollapseLeading,
        CollapseTrailing,
    }

    sealed interface Fallback {
        @ConsistentCopyVisibility
        data class NotYetImplemented internal constructor(
            val capabilityRef: CapabilityRef,
            val feedbackTarget: StructuralRef,
        ) :
            Fallback
    }

    /**
     * What an interaction is currently connected to.
     *
     * LocalPresentation is explicitly not a product Family binding. Unbound is honest missing
     * capability state with an already-bound fallback target.
     */
    sealed interface Binding {
        data object None :
            Binding

        @ConsistentCopyVisibility
        data class LocalPresentation internal constructor(
            val target: StructuralRef,
        ) :
            Binding

        @ConsistentCopyVisibility
        data class Unbound internal constructor(
            val fallback: Fallback,
        ) :
            Binding
    }

    sealed interface Node {
        val identity: Identity
        val placement: Placement
        val depth: Depth
        val presentation: Presentation
        val interactions: Set<Interaction>
        val binding: Binding
        val children: List<Node>
    }

    @ConsistentCopyVisibility
    data class Element internal constructor(
        val element: VexUiElementRef,
        override val placement: Placement,
        override val depth: Depth,
        override val presentation: Presentation,
        override val interactions: Set<Interaction> =
            emptySet(),
        override val binding: Binding =
            Binding.None,
        override val children: List<Node> =
            emptyList(),
    ) :
        Node {
        override val identity: Identity =
            Identity.Blueprint(
                element,
            )
    }

    @ConsistentCopyVisibility
    data class Structural internal constructor(
        val ref: StructuralRef,
        override val placement: Placement,
        override val depth: Depth,
        override val presentation: Presentation,
        override val interactions: Set<Interaction> =
            emptySet(),
        override val binding: Binding =
            Binding.None,
        override val children: List<Node> =
            emptyList(),
    ) :
        Node {
        override val identity: Identity =
            Identity.Structural(
                ref,
            )
    }

    /**
     * One exact split relationship.
     *
     * Leading / handle / trailing are compiler-visible roles rather than child-order convention.
     */
    @ConsistentCopyVisibility
    data class Split internal constructor(
        val ref: StructuralRef,
        val mode: Parameter.SplitMode,
        val leading: Node,
        val handle: Node,
        val trailing: Node,
    ) :
        Node {
        override val identity: Identity =
            Identity.Structural(
                ref,
            )

        override val placement =
            Placement.FillAvailable

        override val depth =
            Depth.Content

        override val presentation =
            Presentation.Surface

        override val interactions =
            emptySet<Interaction>()

        override val binding =
            Binding.None

        override val children: List<Node> =
            listOf(
                leading,
                handle,
                trailing,
            )
    }

    @ConsistentCopyVisibility
    data class Surface internal constructor(
        val screenRef: String,
        val content: Node,
        val overlayRoot: Node,
    ) {
        fun coordinates(): List<Coordinate> {
            val coordinates =
                mutableListOf<Coordinate>()

            fun walk(
                node: Node,
                parentRef: String,
            ) {
                coordinates +=
                    Coordinate(
                        ref =
                            node.identity.ref,
                        parentRef =
                            parentRef,
                        placement =
                            node.placement,
                        depth =
                            node.depth,
                        presentation =
                            node.presentation,
                        interactions =
                            node.interactions,
                        binding =
                            node.binding,
                    )

                node.children.forEach {
                    walk(
                        node =
                            it,
                        parentRef =
                            node.identity.ref,
                    )
                }
            }

            walk(
                node =
                    content,
                parentRef =
                    screenRef,
            )
            walk(
                node =
                    overlayRoot,
                parentRef =
                    screenRef,
            )

            val refs =
                coordinates.map {
                    it.ref
                }

            require(
                refs.size ==
                    refs.toSet().size,
            ) {
                "VexWeb surface contains duplicate source identity"
            }

            return coordinates
        }
    }

    @ConsistentCopyVisibility
    data class Coordinate internal constructor(
        val ref: String,
        val parentRef: String,
        val placement: Placement,
        val depth: Depth,
        val presentation: Presentation,
        val interactions: Set<Interaction>,
        val binding: Binding,
    )
}

// [VXG RealForever]
