package vexlife.web

import vexlife.blueprint.VexUiElementRef

/**
 * Ergonomic authoring over the accepted relational VexWeb vocabulary.
 *
 * This layer introduces no manifestation meaning. It only collects authored Frame / Seam / Region
 * relationships and forms the same [VexWeb] objects whose constructors/validators remain authority.
 */
object VexWebAuthoring {
    @DslMarker
    annotation class Dsl

    fun surface(
        screenRef: String,
        block: SurfaceScope.() -> Unit,
    ): VexWeb.Surface {
        val surface =
            SurfaceScope(
                screenRef,
            )
                .apply(
                    block,
                )
                .build()

        // Surface validation remains owned by the accepted VexWeb relationship model.
        surface.coordinates()

        return surface
    }

    @Dsl
    class SurfaceScope internal constructor(
        private val screenRef: String,
    ) {
        private var contentNode: VexWeb.Node? =
            null
        private var overlayNode: VexWeb.Node? =
            null

        fun content(
            node: VexWeb.Node,
        ) {
            require(
                contentNode ==
                    null,
            ) {
                "VexWeb authoring Surface requires exactly one content root"
            }
            contentNode =
                node
        }

        fun overlay(
            node: VexWeb.Node,
        ) {
            require(
                overlayNode ==
                    null,
            ) {
                "VexWeb authoring Surface requires exactly one overlay root"
            }
            overlayNode =
                node
        }

        internal fun build(): VexWeb.Surface =
            VexWeb.Surface(
                screenRef = screenRef,
                content =
                    requireNotNull(
                        contentNode,
                    ) {
                        "VexWeb authoring Surface is missing its content root"
                    },
                overlayRoot =
                    requireNotNull(
                        overlayNode,
                    ) {
                        "VexWeb authoring Surface is missing its overlay root"
                    },
            )
    }

    fun frame(
        ref: VexWeb.StructuralRef,
        block: FrameScope.() -> Unit,
    ): VexWeb.Frame =
        FrameScope(
            ref,
        )
            .apply(
                block,
            )
            .build()

    @Dsl
    class FrameScope internal constructor(
        private val ref: VexWeb.StructuralRef,
    ) {
        private val seams =
            mutableListOf<VexWeb.Seam>()
        private val regions =
            mutableListOf<VexWeb.Region>()
        private val chrome =
            mutableListOf<VexWeb.Node>()

        fun verticalSeam(
            ref: VexWeb.SeamRef.Vertical,
            betweenStart: VexWeb.XBoundary,
            betweenEnd: VexWeb.XBoundary,
            allocation: VexWeb.Parameter.Allocation,
            spanTop: VexWeb.YBoundary,
            spanBottom: VexWeb.YBoundary,
        ) {
            seams +=
                VexWeb.Seam.Vertical(
                    ref = ref,
                    between =
                        VexWeb.XSpan(
                            start = betweenStart,
                            end = betweenEnd,
                        ),
                    allocation = allocation,
                    span =
                        VexWeb.YSpan(
                            top = spanTop,
                            bottom = spanBottom,
                        ),
                )
        }

        fun horizontalSeam(
            ref: VexWeb.SeamRef.Horizontal,
            betweenTop: VexWeb.YBoundary,
            betweenBottom: VexWeb.YBoundary,
            allocation: VexWeb.Parameter.Allocation,
            spanStart: VexWeb.XBoundary,
            spanEnd: VexWeb.XBoundary,
        ) {
            seams +=
                VexWeb.Seam.Horizontal(
                    ref = ref,
                    between =
                        VexWeb.YSpan(
                            top = betweenTop,
                            bottom = betweenBottom,
                        ),
                    allocation = allocation,
                    span =
                        VexWeb.XSpan(
                            start = spanStart,
                            end = spanEnd,
                        ),
                )
        }

        fun region(
            ref: VexWeb.StructuralRef,
            start: VexWeb.XBoundary,
            end: VexWeb.XBoundary,
            top: VexWeb.YBoundary,
            bottom: VexWeb.YBoundary,
            content: VexWeb.Node,
        ) {
            regions +=
                VexWeb.Region(
                    ref = ref,
                    bounds =
                        VexWeb.Bounds(
                            start = start,
                            end = end,
                            top = top,
                            bottom = bottom,
                        ),
                    content = content,
                )
        }

        fun chrome(
            node: VexWeb.Node,
        ) {
            chrome +=
                node
        }

        internal fun build(): VexWeb.Frame =
            VexWeb.Frame(
                ref = ref,
                seams = seams.toList(),
                regions = regions.toList(),
                chrome = chrome.toList(),
            )
    }

    fun element(
        element: VexUiElementRef,
        placement: VexWeb.Placement,
        depth: VexWeb.Depth,
        presentation: VexWeb.Presentation,
        interactions: Set<VexWeb.Interaction> =
            emptySet(),
        binding: VexWeb.Binding =
            VexWeb.Binding.None,
        children: List<VexWeb.Node> =
            emptyList(),
    ): VexWeb.Element =
        VexWeb.Element(
            element = element,
            placement = placement,
            depth = depth,
            presentation = presentation,
            interactions = interactions,
            binding = binding,
            children = children,
        )

    fun structural(
        ref: VexWeb.StructuralRef,
        placement: VexWeb.Placement,
        depth: VexWeb.Depth,
        presentation: VexWeb.Presentation,
        interactions: Set<VexWeb.Interaction> =
            emptySet(),
        binding: VexWeb.Binding =
            VexWeb.Binding.None,
        children: List<VexWeb.Node> =
            emptyList(),
    ): VexWeb.Structural =
        VexWeb.Structural(
            ref = ref,
            placement = placement,
            depth = depth,
            presentation = presentation,
            interactions = interactions,
            binding = binding,
            children = children,
        )

    fun localSpatial(
        target: VexWeb.SeamRef,
    ): VexWeb.Binding.LocalSpatial =
        VexWeb.Binding.LocalSpatial(
            target,
        )

    fun localInteraction(
        target: VexWeb.Identity,
    ): VexWeb.Binding.LocalInteraction =
        VexWeb.Binding.LocalInteraction(
            target,
        )

    fun capability(
        capabilityRef: VexWeb.CapabilityRef,
    ): VexWeb.Binding.Capability =
        VexWeb.Binding.Capability(
            capabilityRef,
        )

    fun unbound(
        capabilityRef: VexWeb.CapabilityRef,
        feedbackTarget: VexWeb.StructuralRef,
    ): VexWeb.Binding.Unbound =
        VexWeb.Binding.Unbound(
            fallback =
                VexWeb.Fallback.NotYetImplemented(
                    capabilityRef = capabilityRef,
                    feedbackTarget = feedbackTarget,
                ),
        )
}

// [VXG RealForever]
