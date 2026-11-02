package vexlife.web

import vexlife.blueprint.VexUiElementRef

/**
 * Compiler-visible browser-manifestation vocabulary.
 *
 * Spatial meaning is relational: Frames establish local space, Seams establish named boundaries,
 * Regions bind their edges to Frame edges / Seams, and optional Handles manipulate Seams.
 *
 * VexWeb does not own product State and does not encode HTML/CSS/JS.
 */
object VexWeb {
    @ConsistentCopyVisibility
    data class StructuralRef private constructor(
        val value: String,
    ) {
        companion object {
            fun of(
                value: String,
            ): StructuralRef {
                requireSourceRef(
                    value,
                    "structural",
                )

                return StructuralRef(
                    value,
                )
            }
        }
    }

    @ConsistentCopyVisibility
    data class CapabilityRef private constructor(
        val value: String,
    ) {
        companion object {
            fun of(
                value: String,
            ): CapabilityRef {
                require(
                    value.matches(
                        Regex(
                            """[a-z0-9]+(?:[.-][a-z0-9]+)*""",
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

    sealed interface SeamRef {
        val value: String

        @ConsistentCopyVisibility
        data class Vertical private constructor(
            override val value: String,
        ) :
            SeamRef {
            companion object {
                fun of(
                    value: String,
                ): Vertical {
                    requireSourceRef(
                        value,
                        "vertical seam",
                    )

                    return Vertical(
                        value,
                    )
                }
            }
        }

        @ConsistentCopyVisibility
        data class Horizontal private constructor(
            override val value: String,
        ) :
            SeamRef {
            companion object {
                fun of(
                    value: String,
                ): Horizontal {
                    requireSourceRef(
                        value,
                        "horizontal seam",
                    )

                    return Horizontal(
                        value,
                    )
                }
            }
        }
    }

    sealed interface Identity {
        val ref: String
        val sourceParentRef: String?

        @ConsistentCopyVisibility
        data class Blueprint internal constructor(
            val element: VexUiElementRef,
        ) :
            Identity {
            override val ref: String =
                element.ref

            override val sourceParentRef: String? =
                element.parentRef
        }

        @ConsistentCopyVisibility
        data class Structural internal constructor(
            val structuralRef: StructuralRef,
        ) :
            Identity {
            override val ref: String =
                structuralRef.value

            override val sourceParentRef: String? =
                null
        }
    }

    sealed interface Parameter {
        sealed interface Allocation :
            Parameter {
            @ConsistentCopyVisibility
            data class Ratio private constructor(
                val beforeParts: Int,
                val afterParts: Int,
            ) :
                Allocation {
                init {
                    require(
                        beforeParts >
                            0,
                    )
                    require(
                        afterParts >
                            0,
                    )
                }

                val totalParts: Int =
                    beforeParts +
                        afterParts

                companion object {
                    fun of(
                        beforeParts: Int,
                        afterParts: Int,
                    ): Ratio =
                        Ratio(
                            beforeParts =
                                beforeParts,
                            afterParts =
                                afterParts,
                        )

                    val Equal =
                        Ratio(
                            beforeParts =
                                1,
                            afterParts =
                                1,
                        )
                }
            }
        }
    }

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

    enum class Presentation {
        Surface,
        SeamHandle,
        PrimaryAction,
        TransientFeedback,
    }

    enum class Interaction {
        Activate,
        Resize,
        CollapseBefore,
        CollapseAfter,
    }

    sealed interface Fallback {
        @ConsistentCopyVisibility
        data class NotYetImplemented internal constructor(
            val capabilityRef: CapabilityRef,
            val feedbackTarget: StructuralRef,
        ) :
            Fallback
    }

    sealed interface Binding {
        data object None :
            Binding

        @ConsistentCopyVisibility
        data class LocalSpatial internal constructor(
            val target: SeamRef,
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
        init {
            require(
                interactions.isEmpty() ||
                    binding !is Binding.None,
            ) {
                "Interactive VexWeb Blueprint element requires an explicit binding"
            }
        }

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
        init {
            require(
                interactions.isEmpty() ||
                    binding !is Binding.None,
            ) {
                "Interactive VexWeb structural node requires an explicit binding"
            }
        }

        override val identity: Identity =
            Identity.Structural(
                ref,
            )
    }

    sealed interface XBoundary {
        data object FrameStart :
            XBoundary

        data object FrameEnd :
            XBoundary

        @ConsistentCopyVisibility
        data class Seam internal constructor(
            val ref: SeamRef.Vertical,
        ) :
            XBoundary
    }

    sealed interface YBoundary {
        data object FrameTop :
            YBoundary

        data object FrameBottom :
            YBoundary

        @ConsistentCopyVisibility
        data class Seam internal constructor(
            val ref: SeamRef.Horizontal,
        ) :
            YBoundary
    }

    @ConsistentCopyVisibility
    data class XSpan internal constructor(
        val start: XBoundary,
        val end: XBoundary,
    )

    @ConsistentCopyVisibility
    data class YSpan internal constructor(
        val top: YBoundary,
        val bottom: YBoundary,
    )

    @ConsistentCopyVisibility
    data class Bounds internal constructor(
        val start: XBoundary,
        val end: XBoundary,
        val top: YBoundary,
        val bottom: YBoundary,
    )

    sealed interface Seam {
        val ref: SeamRef
        val allocation: Parameter.Allocation

        @ConsistentCopyVisibility
        data class Vertical internal constructor(
            override val ref: SeamRef.Vertical,
            val between: XSpan,
            override val allocation: Parameter.Allocation,
            val span: YSpan,
        ) :
            Seam

        @ConsistentCopyVisibility
        data class Horizontal internal constructor(
            override val ref: SeamRef.Horizontal,
            val between: YSpan,
            override val allocation: Parameter.Allocation,
            val span: XSpan,
        ) :
            Seam
    }

    @ConsistentCopyVisibility
    data class Region internal constructor(
        val ref: StructuralRef,
        val bounds: Bounds,
        val content: Node,
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

        override val children =
            listOf(
                content,
            )
    }

    @ConsistentCopyVisibility
    data class Frame internal constructor(
        val ref: StructuralRef,
        val seams: List<Seam>,
        val regions: List<Region>,
        val chrome: List<Node> =
            emptyList(),
    ) :
        Node {
        init {
            require(
                regions.isNotEmpty(),
            ) {
                "VexWeb Frame requires at least one Region"
            }

            val verticalRefs =
                seams
                    .filterIsInstance<Seam.Vertical>()
                    .map {
                        it.ref
                    }
                    .toSet()
            val horizontalRefs =
                seams
                    .filterIsInstance<Seam.Horizontal>()
                    .map {
                        it.ref
                    }
                    .toSet()
            val seamValues =
                seams.map {
                    it.ref.value
                }

            require(
                seamValues.size ==
                    seamValues.toSet().size,
            ) {
                "VexWeb Frame contains duplicate Seam identity"
            }

            fun xKnown(
                boundary: XBoundary,
            ): Boolean =
                when (boundary) {
                    XBoundary.FrameStart,
                    XBoundary.FrameEnd,
                    ->
                        true

                    is XBoundary.Seam ->
                        boundary.ref in
                            verticalRefs
                }

            fun yKnown(
                boundary: YBoundary,
            ): Boolean =
                when (boundary) {
                    YBoundary.FrameTop,
                    YBoundary.FrameBottom,
                    ->
                        true

                    is YBoundary.Seam ->
                        boundary.ref in
                            horizontalRefs
                }

            seams.forEach { seam ->
                when (seam) {
                    is Seam.Vertical -> {
                        require(
                            xKnown(
                                seam.between.start,
                            ) &&
                                xKnown(
                                    seam.between.end,
                                ) &&
                                yKnown(
                                    seam.span.top,
                                ) &&
                                yKnown(
                                    seam.span.bottom,
                                ),
                        ) {
                            "VexWeb vertical Seam references a boundary outside its Frame"
                        }

                        require(
                            seam.between.start !=
                                XBoundary.Seam(
                                    seam.ref,
                                ) &&
                                seam.between.end !=
                                XBoundary.Seam(
                                    seam.ref,
                                ),
                        ) {
                            "VexWeb vertical Seam cannot position itself from itself"
                        }
                    }

                    is Seam.Horizontal -> {
                        require(
                            yKnown(
                                seam.between.top,
                            ) &&
                                yKnown(
                                    seam.between.bottom,
                                ) &&
                                xKnown(
                                    seam.span.start,
                                ) &&
                                xKnown(
                                    seam.span.end,
                                ),
                        ) {
                            "VexWeb horizontal Seam references a boundary outside its Frame"
                        }

                        require(
                            seam.between.top !=
                                YBoundary.Seam(
                                    seam.ref,
                                ) &&
                                seam.between.bottom !=
                                YBoundary.Seam(
                                    seam.ref,
                                ),
                        ) {
                            "VexWeb horizontal Seam cannot position itself from itself"
                        }
                    }
                }
            }

            regions.forEach { region ->
                require(
                    xKnown(
                        region.bounds.start,
                    ) &&
                        xKnown(
                            region.bounds.end,
                        ) &&
                        yKnown(
                            region.bounds.top,
                        ) &&
                        yKnown(
                            region.bounds.bottom,
                        ),
                ) {
                    "VexWeb Region references a boundary outside its Frame"
                }
            }

            val verticalByRef =
                seams
                    .filterIsInstance<Seam.Vertical>()
                    .associateBy {
                        it.ref
                    }
            val horizontalByRef =
                seams
                    .filterIsInstance<Seam.Horizontal>()
                    .associateBy {
                        it.ref
                    }

            fun verticalDependencies(
                seam: Seam.Vertical,
            ): Set<SeamRef.Vertical> =
                listOf(
                    seam.between.start,
                    seam.between.end,
                )
                    .mapNotNull {
                        (it as? XBoundary.Seam)
                            ?.ref
                    }
                    .toSet()

            fun horizontalDependencies(
                seam: Seam.Horizontal,
            ): Set<SeamRef.Horizontal> =
                listOf(
                    seam.between.top,
                    seam.between.bottom,
                )
                    .mapNotNull {
                        (it as? YBoundary.Seam)
                            ?.ref
                    }
                    .toSet()

            fun validateVertical(
                ref: SeamRef.Vertical,
                visiting: MutableSet<SeamRef.Vertical>,
                visited: MutableSet<SeamRef.Vertical>,
            ) {
                if (
                    ref in
                    visited
                ) {
                    return
                }

                require(
                    ref !in
                        visiting,
                ) {
                    "VexWeb vertical Seam allocation contains a dependency cycle"
                }

                visiting +=
                    ref

                verticalDependencies(
                    verticalByRef
                        .getValue(
                            ref,
                        ),
                ).forEach {
                    validateVertical(
                        ref =
                            it,
                        visiting =
                            visiting,
                        visited =
                            visited,
                    )
                }

                visiting -=
                    ref
                visited +=
                    ref
            }

            fun validateHorizontal(
                ref: SeamRef.Horizontal,
                visiting: MutableSet<SeamRef.Horizontal>,
                visited: MutableSet<SeamRef.Horizontal>,
            ) {
                if (
                    ref in
                    visited
                ) {
                    return
                }

                require(
                    ref !in
                        visiting,
                ) {
                    "VexWeb horizontal Seam allocation contains a dependency cycle"
                }

                visiting +=
                    ref

                horizontalDependencies(
                    horizontalByRef
                        .getValue(
                            ref,
                        ),
                ).forEach {
                    validateHorizontal(
                        ref =
                            it,
                        visiting =
                            visiting,
                        visited =
                            visited,
                    )
                }

                visiting -=
                    ref
                visited +=
                    ref
            }

            val visitedVertical =
                mutableSetOf<SeamRef.Vertical>()
            verticalByRef.keys.forEach {
                validateVertical(
                    ref =
                        it,
                    visiting =
                        mutableSetOf(),
                    visited =
                        visitedVertical,
                )
            }

            val visitedHorizontal =
                mutableSetOf<SeamRef.Horizontal>()
            horizontalByRef.keys.forEach {
                validateHorizontal(
                    ref =
                        it,
                    visiting =
                        mutableSetOf(),
                    visited =
                        visitedHorizontal,
                )
            }
        }

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
            regions +
                chrome
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
                containerRef: String,
            ) {
                coordinates +=
                    Coordinate(
                        ref =
                            node.identity.ref,
                        sourceParentRef =
                            node.identity.sourceParentRef,
                        containerRef =
                            containerRef,
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
                        containerRef =
                            node.identity.ref,
                    )
                }
            }

            walk(
                node =
                    content,
                containerRef =
                    screenRef,
            )
            walk(
                node =
                    overlayRoot,
                containerRef =
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

            val byRef =
                coordinates.associateBy {
                    it.ref
                }
            val seamRefs =
                linkedMapOf<
                    String,
                    SeamRef
                >()

            fun collectSeams(
                node: Node,
            ) {
                if (
                    node is
                    Frame
                ) {
                    node.seams.forEach { seam ->
                        require(
                            seam.ref.value !in
                                seamRefs,
                        ) {
                            "VexWeb Surface contains duplicate Seam identity"
                        }

                        seamRefs[
                            seam.ref.value
                        ] =
                            seam.ref
                    }
                }

                node.children.forEach {
                    collectSeams(
                        it,
                    )
                }
            }

            collectSeams(
                content,
            )
            collectSeams(
                overlayRoot,
            )

            require(
                seamRefs.keys.none {
                    it in
                        byRef
                },
            ) {
                "VexWeb Seam identity must not collide with Node identity"
            }

            coordinates.forEach { coordinate ->
                when (
                    val authoredBinding =
                        coordinate.binding
                ) {
                    is Binding.LocalSpatial ->
                        require(
                            authoredBinding.target.value in
                                seamRefs,
                        ) {
                            "VexWeb spatial binding target is outside this Surface: " +
                                authoredBinding.target.value
                        }

                    is Binding.Unbound ->
                        when (
                            val fallback =
                                authoredBinding.fallback
                        ) {
                            is Fallback.NotYetImplemented -> {
                                val target =
                                    byRef[
                                        fallback
                                            .feedbackTarget
                                            .value
                                    ]
                                        ?: error(
                                            "VexWeb fallback target is outside this Surface: " +
                                                fallback.feedbackTarget.value,
                                        )

                                require(
                                    target.depth ==
                                        Depth.Transient ||
                                        target.depth ==
                                            Depth.Modal,
                                ) {
                                    "VexWeb feedback target must live on a transient/modal depth plane"
                                }
                            }
                        }

                    Binding.None ->
                        Unit
                }
            }

            return coordinates
        }

        fun spatialRegions(): List<RegionCoordinate> {
            val regions =
                mutableListOf<RegionCoordinate>()

            fun walk(
                node: Node,
            ) {
                if (
                    node is
                    Frame
                ) {
                    node.regions.forEach { region ->
                        regions +=
                            RegionCoordinate(
                                ref =
                                    region.ref.value,
                                frameRef =
                                    node.ref.value,
                                bounds =
                                    region.bounds,
                                contentRef =
                                    region.content
                                        .identity
                                        .ref,
                            )
                    }
                }

                node.children.forEach {
                    walk(
                        it,
                    )
                }
            }

            walk(
                content,
            )
            walk(
                overlayRoot,
            )

            return regions
        }
    }

    @ConsistentCopyVisibility
    data class Coordinate internal constructor(
        val ref: String,
        val sourceParentRef: String?,
        val containerRef: String,
        val placement: Placement,
        val depth: Depth,
        val presentation: Presentation,
        val interactions: Set<Interaction>,
        val binding: Binding,
    )

    @ConsistentCopyVisibility
    data class RegionCoordinate internal constructor(
        val ref: String,
        val frameRef: String,
        val bounds: Bounds,
        val contentRef: String,
    )

    private fun requireSourceRef(
        value: String,
        kind: String,
    ) {
        require(
            value.matches(
                Regex(
                    """vexweb\.[a-z0-9]+(?:[.-][a-z0-9]+)*""",
                ),
            ),
        ) {
            "VexWeb $kind ref must be stable lowercase source identity: $value"
        }
    }
}

// [VXG RealForever]
