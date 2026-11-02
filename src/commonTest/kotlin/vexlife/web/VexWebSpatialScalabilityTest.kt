package vexlife.web

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import vexlife.workspace.VexLifeWorkspaceBlueprint

class VexWebSpatialScalabilityTest {
    private fun leaf(
        ref: String,
    ): VexWeb.Structural =
        VexWeb.Structural(
            ref =
                VexWeb.StructuralRef.of(
                    ref,
                ),
            placement =
                VexWeb.Placement.FillAvailable,
            depth =
                VexWeb.Depth.Content,
            presentation =
                VexWeb.Presentation.Surface,
        )

    private fun surface(
        frame: VexWeb.Frame,
        overlaySuffix: String,
    ): VexWeb.Surface =
        VexWeb.Surface(
            screenRef =
                VexLifeWorkspaceBlueprint.screenRef,
            content =
                frame,
            overlayRoot =
                VexWeb.Structural(
                    ref =
                        VexWeb.StructuralRef.of(
                            "vexweb.test.$overlaySuffix.overlay",
                        ),
                    placement =
                        VexWeb.Placement.Overlay,
                    depth =
                        VexWeb.Depth.Transient,
                    presentation =
                        VexWeb.Presentation.Surface,
                ),
        )

    @Test
    fun twoRegionsUseOneSeamAndAWeightedAllocation() {
        val seam =
            VexWeb.SeamRef.Vertical.of(
                "vexweb.test.two.seam",
            )

        val frame =
            VexWeb.Frame(
                ref =
                    VexWeb.StructuralRef.of(
                        "vexweb.test.two.frame",
                    ),
                seams =
                    listOf(
                        VexWeb.Seam.Vertical(
                            ref =
                                seam,
                            between =
                                VexWeb.XSpan(
                                    start =
                                        VexWeb.XBoundary.FrameStart,
                                    end =
                                        VexWeb.XBoundary.FrameEnd,
                                ),
                            allocation =
                                VexWeb.Parameter.Allocation.Ratio.of(
                                    beforeParts =
                                        2,
                                    afterParts =
                                        1,
                                ),
                            span =
                                VexWeb.YSpan(
                                    top =
                                        VexWeb.YBoundary.FrameTop,
                                    bottom =
                                        VexWeb.YBoundary.FrameBottom,
                                ),
                        ),
                    ),
                regions =
                    listOf(
                        VexWeb.Region(
                            ref =
                                VexWeb.StructuralRef.of(
                                    "vexweb.test.two.region.one",
                                ),
                            bounds =
                                VexWeb.Bounds(
                                    start =
                                        VexWeb.XBoundary.FrameStart,
                                    end =
                                        VexWeb.XBoundary.Seam(
                                            seam,
                                        ),
                                    top =
                                        VexWeb.YBoundary.FrameTop,
                                    bottom =
                                        VexWeb.YBoundary.FrameBottom,
                                ),
                            content =
                                leaf(
                                    "vexweb.test.two.content.one",
                                ),
                        ),
                        VexWeb.Region(
                            ref =
                                VexWeb.StructuralRef.of(
                                    "vexweb.test.two.region.two",
                                ),
                            bounds =
                                VexWeb.Bounds(
                                    start =
                                        VexWeb.XBoundary.Seam(
                                            seam,
                                        ),
                                    end =
                                        VexWeb.XBoundary.FrameEnd,
                                    top =
                                        VexWeb.YBoundary.FrameTop,
                                    bottom =
                                        VexWeb.YBoundary.FrameBottom,
                                ),
                            content =
                                leaf(
                                    "vexweb.test.two.content.two",
                                ),
                        ),
                    ),
            )

        val ratio =
            frame
                .seams
                .single()
                .allocation as
                VexWeb.Parameter.Allocation.Ratio

        assertEquals(
            2,
            ratio.beforeParts,
        )
        assertEquals(
            1,
            ratio.afterParts,
        )
        assertEquals(
            2,
            surface(
                frame,
                "two",
            )
                .spatialRegions()
                .size,
        )
    }

    @Test
    fun threeRegionsUseTheSameLanguageWithOnePartialSeam() {
        val vertical =
            VexWeb.SeamRef.Vertical.of(
                "vexweb.test.three.seam.vertical",
            )
        val rightMiddle =
            VexWeb.SeamRef.Horizontal.of(
                "vexweb.test.three.seam.right-middle",
            )

        val frame =
            VexWeb.Frame(
                ref =
                    VexWeb.StructuralRef.of(
                        "vexweb.test.three.frame",
                    ),
                seams =
                    listOf(
                        VexWeb.Seam.Vertical(
                            ref =
                                vertical,
                            between =
                                VexWeb.XSpan(
                                    start =
                                        VexWeb.XBoundary.FrameStart,
                                    end =
                                        VexWeb.XBoundary.FrameEnd,
                                ),
                            allocation =
                                VexWeb.Parameter.Allocation.Ratio.Equal,
                            span =
                                VexWeb.YSpan(
                                    top =
                                        VexWeb.YBoundary.FrameTop,
                                    bottom =
                                        VexWeb.YBoundary.FrameBottom,
                                ),
                        ),
                        VexWeb.Seam.Horizontal(
                            ref =
                                rightMiddle,
                            between =
                                VexWeb.YSpan(
                                    top =
                                        VexWeb.YBoundary.FrameTop,
                                    bottom =
                                        VexWeb.YBoundary.FrameBottom,
                                ),
                            allocation =
                                VexWeb.Parameter.Allocation.Ratio.Equal,
                            span =
                                VexWeb.XSpan(
                                    start =
                                        VexWeb.XBoundary.Seam(
                                            vertical,
                                        ),
                                    end =
                                        VexWeb.XBoundary.FrameEnd,
                                ),
                        ),
                    ),
                regions =
                    listOf(
                        VexWeb.Region(
                            ref =
                                VexWeb.StructuralRef.of(
                                    "vexweb.test.three.region.one",
                                ),
                            bounds =
                                VexWeb.Bounds(
                                    start =
                                        VexWeb.XBoundary.FrameStart,
                                    end =
                                        VexWeb.XBoundary.Seam(
                                            vertical,
                                        ),
                                    top =
                                        VexWeb.YBoundary.FrameTop,
                                    bottom =
                                        VexWeb.YBoundary.FrameBottom,
                                ),
                            content =
                                leaf(
                                    "vexweb.test.three.content.one",
                                ),
                        ),
                        VexWeb.Region(
                            ref =
                                VexWeb.StructuralRef.of(
                                    "vexweb.test.three.region.two",
                                ),
                            bounds =
                                VexWeb.Bounds(
                                    start =
                                        VexWeb.XBoundary.Seam(
                                            vertical,
                                        ),
                                    end =
                                        VexWeb.XBoundary.FrameEnd,
                                    top =
                                        VexWeb.YBoundary.FrameTop,
                                    bottom =
                                        VexWeb.YBoundary.Seam(
                                            rightMiddle,
                                        ),
                                ),
                            content =
                                leaf(
                                    "vexweb.test.three.content.two",
                                ),
                        ),
                        VexWeb.Region(
                            ref =
                                VexWeb.StructuralRef.of(
                                    "vexweb.test.three.region.three",
                                ),
                            bounds =
                                VexWeb.Bounds(
                                    start =
                                        VexWeb.XBoundary.Seam(
                                            vertical,
                                        ),
                                    end =
                                        VexWeb.XBoundary.FrameEnd,
                                    top =
                                        VexWeb.YBoundary.Seam(
                                            rightMiddle,
                                        ),
                                    bottom =
                                        VexWeb.YBoundary.FrameBottom,
                                ),
                            content =
                                leaf(
                                    "vexweb.test.three.content.three",
                                ),
                        ),
                    ),
            )

        val horizontal =
            frame.seams
                .filterIsInstance<
                    VexWeb.Seam.Horizontal
                >()
                .single()

        assertEquals(
            VexWeb.XBoundary.Seam(
                vertical,
            ),
            horizontal.span.start,
        )
        assertEquals(
            VexWeb.XBoundary.FrameEnd,
            horizontal.span.end,
        )
        assertEquals(
            3,
            surface(
                frame,
                "three",
            )
                .spatialRegions()
                .size,
        )
    }


    @Test
    fun sameAxisSeamAllocationCyclesAreRejected() {
        val first =
            VexWeb.SeamRef.Vertical.of(
                "vexweb.test.cycle.seam.first",
            )
        val second =
            VexWeb.SeamRef.Vertical.of(
                "vexweb.test.cycle.seam.second",
            )

        assertFailsWith<IllegalArgumentException> {
            VexWeb.Frame(
                ref =
                    VexWeb.StructuralRef.of(
                        "vexweb.test.cycle.frame",
                    ),
                seams =
                    listOf(
                        VexWeb.Seam.Vertical(
                            ref =
                                first,
                            between =
                                VexWeb.XSpan(
                                    start =
                                        VexWeb.XBoundary.Seam(
                                            second,
                                        ),
                                    end =
                                        VexWeb.XBoundary.FrameEnd,
                                ),
                            allocation =
                                VexWeb.Parameter.Allocation.Ratio.Equal,
                            span =
                                VexWeb.YSpan(
                                    top =
                                        VexWeb.YBoundary.FrameTop,
                                    bottom =
                                        VexWeb.YBoundary.FrameBottom,
                                ),
                        ),
                        VexWeb.Seam.Vertical(
                            ref =
                                second,
                            between =
                                VexWeb.XSpan(
                                    start =
                                        VexWeb.XBoundary.FrameStart,
                                    end =
                                        VexWeb.XBoundary.Seam(
                                            first,
                                        ),
                                ),
                            allocation =
                                VexWeb.Parameter.Allocation.Ratio.Equal,
                            span =
                                VexWeb.YSpan(
                                    top =
                                        VexWeb.YBoundary.FrameTop,
                                    bottom =
                                        VexWeb.YBoundary.FrameBottom,
                                ),
                        ),
                    ),
                regions =
                    listOf(
                        VexWeb.Region(
                            ref =
                                VexWeb.StructuralRef.of(
                                    "vexweb.test.cycle.region",
                                ),
                            bounds =
                                VexWeb.Bounds(
                                    start =
                                        VexWeb.XBoundary.FrameStart,
                                    end =
                                        VexWeb.XBoundary.FrameEnd,
                                    top =
                                        VexWeb.YBoundary.FrameTop,
                                    bottom =
                                        VexWeb.YBoundary.FrameBottom,
                                ),
                            content =
                                leaf(
                                    "vexweb.test.cycle.content",
                                ),
                        ),
                    ),
            )
        }
    }

    @Test
    fun fourRegionsUseTwoCrossingSeamsWithoutChangingTheLanguage() {
        val vertical =
            VexWeb.SeamRef.Vertical.of(
                "vexweb.test.four.seam.vertical",
            )
        val horizontal =
            VexWeb.SeamRef.Horizontal.of(
                "vexweb.test.four.seam.horizontal",
            )

        val frame =
            VexWeb.Frame(
                ref =
                    VexWeb.StructuralRef.of(
                        "vexweb.test.four.frame",
                    ),
                seams =
                    listOf(
                        VexWeb.Seam.Vertical(
                            ref =
                                vertical,
                            between =
                                VexWeb.XSpan(
                                    start =
                                        VexWeb.XBoundary.FrameStart,
                                    end =
                                        VexWeb.XBoundary.FrameEnd,
                                ),
                            allocation =
                                VexWeb.Parameter.Allocation.Ratio.Equal,
                            span =
                                VexWeb.YSpan(
                                    top =
                                        VexWeb.YBoundary.FrameTop,
                                    bottom =
                                        VexWeb.YBoundary.FrameBottom,
                                ),
                        ),
                        VexWeb.Seam.Horizontal(
                            ref =
                                horizontal,
                            between =
                                VexWeb.YSpan(
                                    top =
                                        VexWeb.YBoundary.FrameTop,
                                    bottom =
                                        VexWeb.YBoundary.FrameBottom,
                                ),
                            allocation =
                                VexWeb.Parameter.Allocation.Ratio.Equal,
                            span =
                                VexWeb.XSpan(
                                    start =
                                        VexWeb.XBoundary.FrameStart,
                                    end =
                                        VexWeb.XBoundary.FrameEnd,
                                ),
                        ),
                    ),
                regions =
                    listOf(
                        VexWeb.Region(
                            ref =
                                VexWeb.StructuralRef.of(
                                    "vexweb.test.four.region.one",
                                ),
                            bounds =
                                VexWeb.Bounds(
                                    start =
                                        VexWeb.XBoundary.FrameStart,
                                    end =
                                        VexWeb.XBoundary.Seam(
                                            vertical,
                                        ),
                                    top =
                                        VexWeb.YBoundary.FrameTop,
                                    bottom =
                                        VexWeb.YBoundary.Seam(
                                            horizontal,
                                        ),
                                ),
                            content =
                                leaf(
                                    "vexweb.test.four.content.one",
                                ),
                        ),
                        VexWeb.Region(
                            ref =
                                VexWeb.StructuralRef.of(
                                    "vexweb.test.four.region.two",
                                ),
                            bounds =
                                VexWeb.Bounds(
                                    start =
                                        VexWeb.XBoundary.Seam(
                                            vertical,
                                        ),
                                    end =
                                        VexWeb.XBoundary.FrameEnd,
                                    top =
                                        VexWeb.YBoundary.FrameTop,
                                    bottom =
                                        VexWeb.YBoundary.Seam(
                                            horizontal,
                                        ),
                                ),
                            content =
                                leaf(
                                    "vexweb.test.four.content.two",
                                ),
                        ),
                        VexWeb.Region(
                            ref =
                                VexWeb.StructuralRef.of(
                                    "vexweb.test.four.region.three",
                                ),
                            bounds =
                                VexWeb.Bounds(
                                    start =
                                        VexWeb.XBoundary.FrameStart,
                                    end =
                                        VexWeb.XBoundary.Seam(
                                            vertical,
                                        ),
                                    top =
                                        VexWeb.YBoundary.Seam(
                                            horizontal,
                                        ),
                                    bottom =
                                        VexWeb.YBoundary.FrameBottom,
                                ),
                            content =
                                leaf(
                                    "vexweb.test.four.content.three",
                                ),
                        ),
                        VexWeb.Region(
                            ref =
                                VexWeb.StructuralRef.of(
                                    "vexweb.test.four.region.four",
                                ),
                            bounds =
                                VexWeb.Bounds(
                                    start =
                                        VexWeb.XBoundary.Seam(
                                            vertical,
                                        ),
                                    end =
                                        VexWeb.XBoundary.FrameEnd,
                                    top =
                                        VexWeb.YBoundary.Seam(
                                            horizontal,
                                        ),
                                    bottom =
                                        VexWeb.YBoundary.FrameBottom,
                                ),
                            content =
                                leaf(
                                    "vexweb.test.four.content.four",
                                ),
                        ),
                    ),
            )

        assertEquals(
            4,
            surface(
                frame,
                "four",
            )
                .spatialRegions()
                .size,
        )
    }
}

// [VXG RealForever]
