package vexlife.web

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class VexWebAuthoringTest {
    private fun leaf(
        ref: String,
    ): VexWeb.Structural =
        VexWebAuthoring.structural(
            ref =
                VexWeb.StructuralRef.of(
                    ref,
                ),
            placement = VexWeb.Placement.FillAvailable,
            depth = VexWeb.Depth.Content,
            presentation = VexWeb.Presentation.Surface,
        )

    private fun overlay(): VexWeb.Structural =
        VexWebAuthoring.structural(
            ref =
                VexWeb.StructuralRef.of(
                    "vexweb.test.overlay",
                ),
            placement = VexWeb.Placement.Overlay,
            depth = VexWeb.Depth.Transient,
            presentation = VexWeb.Presentation.Surface,
        )

    @Test
    fun relationalAuthoringEmitsTheExactAcceptedVexWebObjects() {
        val seam =
            VexWeb.SeamRef.Vertical.of(
                "vexweb.test.seam.main",
            )
        val leftRef =
            VexWeb.StructuralRef.of(
                "vexweb.test.region.left",
            )
        val rightRef =
            VexWeb.StructuralRef.of(
                "vexweb.test.region.right",
            )
        val frameRef =
            VexWeb.StructuralRef.of(
                "vexweb.test.frame",
            )
        val left =
            leaf(
                "vexweb.test.left",
            )
        val right =
            leaf(
                "vexweb.test.right",
            )

        val direct =
            VexWeb.Frame(
                ref = frameRef,
                seams =
                    listOf(
                        VexWeb.Seam.Vertical(
                            ref = seam,
                            between =
                                VexWeb.XSpan(
                                    start = VexWeb.XBoundary.FrameStart,
                                    end = VexWeb.XBoundary.FrameEnd,
                                ),
                            allocation = VexWeb.Parameter.Allocation.Ratio.Equal,
                            span =
                                VexWeb.YSpan(
                                    top = VexWeb.YBoundary.FrameTop,
                                    bottom = VexWeb.YBoundary.FrameBottom,
                                ),
                        ),
                    ),
                regions =
                    listOf(
                        VexWeb.Region(
                            ref = leftRef,
                            bounds =
                                VexWeb.Bounds(
                                    start = VexWeb.XBoundary.FrameStart,
                                    end = VexWeb.XBoundary.Seam(
                                        seam,
                                    ),
                                    top = VexWeb.YBoundary.FrameTop,
                                    bottom = VexWeb.YBoundary.FrameBottom,
                                ),
                            content = left,
                        ),
                        VexWeb.Region(
                            ref = rightRef,
                            bounds =
                                VexWeb.Bounds(
                                    start = VexWeb.XBoundary.Seam(
                                        seam,
                                    ),
                                    end = VexWeb.XBoundary.FrameEnd,
                                    top = VexWeb.YBoundary.FrameTop,
                                    bottom = VexWeb.YBoundary.FrameBottom,
                                ),
                            content = right,
                        ),
                    ),
            )

        val authored =
            VexWebAuthoring.frame(
                frameRef,
            ) {
                verticalSeam(
                    ref = seam,
                    betweenStart = VexWeb.XBoundary.FrameStart,
                    betweenEnd = VexWeb.XBoundary.FrameEnd,
                    allocation = VexWeb.Parameter.Allocation.Ratio.Equal,
                    spanTop = VexWeb.YBoundary.FrameTop,
                    spanBottom = VexWeb.YBoundary.FrameBottom,
                )
                region(
                    ref = leftRef,
                    start = VexWeb.XBoundary.FrameStart,
                    end = VexWeb.XBoundary.Seam(
                        seam,
                    ),
                    top = VexWeb.YBoundary.FrameTop,
                    bottom = VexWeb.YBoundary.FrameBottom,
                    content = left,
                )
                region(
                    ref = rightRef,
                    start = VexWeb.XBoundary.Seam(
                        seam,
                    ),
                    end = VexWeb.XBoundary.FrameEnd,
                    top = VexWeb.YBoundary.FrameTop,
                    bottom = VexWeb.YBoundary.FrameBottom,
                    content = right,
                )
            }

        assertEquals(
            direct,
            authored,
        )

        val overlay =
            overlay()
        val directSurface =
            VexWeb.Surface(
                screenRef = "screen.test",
                content = direct,
                overlayRoot = overlay,
            )
        val authoredSurface =
            VexWebAuthoring.surface(
                "screen.test",
            ) {
                content(
                    authored,
                )
                overlay(
                    overlay,
                )
            }

        assertEquals(
            directSurface,
            authoredSurface,
        )
        assertEquals(
            directSurface.coordinates(),
            authoredSurface.coordinates(),
        )
    }

    @Test
    fun irregularThreeRegionsUseOneFullAndOnePartialSeam() {
        val vertical =
            VexWeb.SeamRef.Vertical.of(
                "vexweb.test.three.seam.vertical",
            )
        val horizontal =
            VexWeb.SeamRef.Horizontal.of(
                "vexweb.test.three.seam.horizontal",
            )
        val frame =
            VexWebAuthoring.frame(
                VexWeb.StructuralRef.of(
                    "vexweb.test.three.frame",
                ),
            ) {
                verticalSeam(
                    ref = vertical,
                    betweenStart = VexWeb.XBoundary.FrameStart,
                    betweenEnd = VexWeb.XBoundary.FrameEnd,
                    allocation = VexWeb.Parameter.Allocation.Ratio.Equal,
                    spanTop = VexWeb.YBoundary.FrameTop,
                    spanBottom = VexWeb.YBoundary.FrameBottom,
                )
                horizontalSeam(
                    ref = horizontal,
                    betweenTop = VexWeb.YBoundary.FrameTop,
                    betweenBottom = VexWeb.YBoundary.FrameBottom,
                    allocation = VexWeb.Parameter.Allocation.Ratio.Equal,
                    spanStart = VexWeb.XBoundary.Seam(
                        vertical,
                    ),
                    spanEnd = VexWeb.XBoundary.FrameEnd,
                )
                region(
                    ref = VexWeb.StructuralRef.of(
                        "vexweb.test.three.region.left",
                    ),
                    start = VexWeb.XBoundary.FrameStart,
                    end = VexWeb.XBoundary.Seam(
                        vertical,
                    ),
                    top = VexWeb.YBoundary.FrameTop,
                    bottom = VexWeb.YBoundary.FrameBottom,
                    content = leaf(
                        "vexweb.test.three.left",
                    ),
                )
                region(
                    ref = VexWeb.StructuralRef.of(
                        "vexweb.test.three.region.top-right",
                    ),
                    start = VexWeb.XBoundary.Seam(
                        vertical,
                    ),
                    end = VexWeb.XBoundary.FrameEnd,
                    top = VexWeb.YBoundary.FrameTop,
                    bottom = VexWeb.YBoundary.Seam(
                        horizontal,
                    ),
                    content = leaf(
                        "vexweb.test.three.top-right",
                    ),
                )
                region(
                    ref = VexWeb.StructuralRef.of(
                        "vexweb.test.three.region.bottom-right",
                    ),
                    start = VexWeb.XBoundary.Seam(
                        vertical,
                    ),
                    end = VexWeb.XBoundary.FrameEnd,
                    top = VexWeb.YBoundary.Seam(
                        horizontal,
                    ),
                    bottom = VexWeb.YBoundary.FrameBottom,
                    content = leaf(
                        "vexweb.test.three.bottom-right",
                    ),
                )
            }

        val surface =
            VexWebAuthoring.surface(
                "screen.test-three",
            ) {
                content(
                    frame,
                )
                overlay(
                    overlay(),
                )
            }

        assertEquals(
            3,
            surface.spatialRegions().size,
        )
    }

    @Test
    fun fourQuadrantsUseTheSameTwoSeamAuthoringLanguage() {
        val vertical =
            VexWeb.SeamRef.Vertical.of(
                "vexweb.test.four.seam.vertical",
            )
        val horizontal =
            VexWeb.SeamRef.Horizontal.of(
                "vexweb.test.four.seam.horizontal",
            )

        val frame =
            VexWebAuthoring.frame(
                VexWeb.StructuralRef.of(
                    "vexweb.test.four.frame",
                ),
            ) {
                verticalSeam(
                    ref = vertical,
                    betweenStart = VexWeb.XBoundary.FrameStart,
                    betweenEnd = VexWeb.XBoundary.FrameEnd,
                    allocation = VexWeb.Parameter.Allocation.Ratio.Equal,
                    spanTop = VexWeb.YBoundary.FrameTop,
                    spanBottom = VexWeb.YBoundary.FrameBottom,
                )
                horizontalSeam(
                    ref = horizontal,
                    betweenTop = VexWeb.YBoundary.FrameTop,
                    betweenBottom = VexWeb.YBoundary.FrameBottom,
                    allocation = VexWeb.Parameter.Allocation.Ratio.Equal,
                    spanStart = VexWeb.XBoundary.FrameStart,
                    spanEnd = VexWeb.XBoundary.FrameEnd,
                )

                val x =
                    listOf(
                        VexWeb.XBoundary.FrameStart to
                            VexWeb.XBoundary.Seam(
                                vertical,
                            ),
                        VexWeb.XBoundary.Seam(
                            vertical,
                        ) to
                            VexWeb.XBoundary.FrameEnd,
                    )
                val y =
                    listOf(
                        VexWeb.YBoundary.FrameTop to
                            VexWeb.YBoundary.Seam(
                                horizontal,
                            ),
                        VexWeb.YBoundary.Seam(
                            horizontal,
                        ) to
                            VexWeb.YBoundary.FrameBottom,
                    )

                var index =
                    0
                for (
                    (top, bottom) in
                    y
                ) {
                    for (
                        (start, end) in
                        x
                    ) {
                        index +=
                            1
                        region(
                            ref =
                                VexWeb.StructuralRef.of(
                                    "vexweb.test.four.region.$index",
                                ),
                            start = start,
                            end = end,
                            top = top,
                            bottom = bottom,
                            content =
                                leaf(
                                    "vexweb.test.four.content.$index",
                                ),
                        )
                    }
                }
            }

        val surface =
            VexWebAuthoring.surface(
                "screen.test-four",
            ) {
                content(
                    frame,
                )
                overlay(
                    overlay(),
                )
            }

        assertEquals(
            4,
            surface.spatialRegions().size,
        )
    }

    @Test
    fun surfaceAuthoringCannotHideMissingRootsOrInvalidInteractiveBinding() {
        assertFailsWith<IllegalArgumentException> {
            VexWebAuthoring.surface(
                "screen.missing",
            ) {
                content(
                    leaf(
                        "vexweb.test.missing.content",
                    ),
                )
            }
        }

        assertFailsWith<IllegalArgumentException> {
            VexWebAuthoring.structural(
                ref =
                    VexWeb.StructuralRef.of(
                        "vexweb.test.invalid-interactive",
                    ),
                placement = VexWeb.Placement.InFlow,
                depth = VexWeb.Depth.Content,
                presentation = VexWeb.Presentation.PrimaryAction,
                interactions =
                    setOf(
                        VexWeb.Interaction.Activate,
                    ),
            )
        }
    }
}

// [VXG RealForever]
