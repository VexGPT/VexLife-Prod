package vexlife.web

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VexWebArtifactsTest {
    @Test
    fun currentVexLifeSurfaceGeneratesDeterministicSourceAddressableArtifacts() {
        val first =
            VexWebArtifacts.generate(
                VexLifeWebF0.Surface,
            )
        val second =
            VexWebArtifacts.generate(
                VexLifeWebF0.Surface,
            )

        assertEquals(
            first,
            second,
        )
        assertEquals(
            listOf(
                "index.html",
                "vexweb.css",
                "vexweb.js",
                "vexweb-manifest.json",
            ),
            first.paths,
        )
        assertTrue(
            first.html.contains(
                "data-vex-ref=\"${VexLifeWebF0.Refs.WorkspaceFrame.value}\"",
            ),
        )
        assertTrue(
            first.html.contains(
                "data-vex-ref=\"${VexLifeWebF0.BrowserViewport.identity.ref}\"",
            ),
        )
        assertTrue(
            first.html.contains(
                "data-vex-ref=\"${VexLifeWebF0.Send.identity.ref}\"",
            ),
        )
        assertTrue(
            first.html.contains(
                "data-vex-capability=\"conversation.send\"",
            ),
        )
        assertTrue(
            first.javascript.contains(
                "Capability ${'$'}{capability} is not connected yet.",
            ),
        )
        assertFalse(
            first.javascript.contains(
                "fetch(",
            ),
        )
    }

    @Test
    fun generatedGeometryConsumesTheRelationalFrameWithoutReplacingIt() {
        val artifacts =
            VexWebArtifacts.generate(
                VexLifeWebF0.Surface,
            )

        assertTrue(
            artifacts.css.contains(
                "[data-vex-ref=\"${VexLifeWebF0.Refs.BrowserRegion.value}\"]{left:0%;top:0%;width:50%;height:100%;}",
            ),
        )
        assertTrue(
            artifacts.css.contains(
                "[data-vex-ref=\"${VexLifeWebF0.Refs.CompanionRegion.value}\"]{left:50%;top:0%;width:50%;height:100%;}",
            ),
        )
        assertTrue(
            artifacts.semanticManifest.contains(
                "\"frameRef\":\"${VexLifeWebF0.Refs.WorkspaceFrame.value}\"",
            ),
        )
        assertTrue(
            artifacts.semanticManifest.contains(
                "\"ref\":\"${VexLifeWebF0.Refs.BrowserCompanionSeam.value}\",\"orientation\":\"VERTICAL\",\"position\":{\"numerator\":1,\"denominator\":2,\"percent\":\"50%\"}",
            ),
        )
    }

    @Test
    fun semanticManifestPreservesSourceParentContainerAndFallbackMeaning() {
        val manifest =
            VexWebArtifacts.generate(
                VexLifeWebF0.Surface,
            )
                .semanticManifest

        assertTrue(
            manifest.contains(
                "\"ref\":\"${VexLifeWebF0.BrowserPane.identity.ref}\",\"sourceParentRef\":null,\"containerRef\":\"${VexLifeWebF0.Refs.BrowserRegion.value}\"",
            ),
        )
        assertTrue(
            manifest.contains(
                "\"ref\":\"${VexLifeWebF0.Send.identity.ref}\"",
            ),
        )
        assertTrue(
            manifest.contains(
                "\"type\":\"UNBOUND\",\"fallback\":{\"type\":\"NOT_YET_IMPLEMENTED\",\"capabilityRef\":\"conversation.send\",\"feedbackTarget\":\"${VexLifeWebF0.Refs.Snackbar.value}\"}",
            ),
        )
    }

    @Test
    fun irregularThreeRegionGeometryUsesTheSameGenerator() {
        val vertical =
            VexWeb.SeamRef.Vertical.of(
                "vexweb.test.generated.seam.vertical",
            )
        val horizontal =
            VexWeb.SeamRef.Horizontal.of(
                "vexweb.test.generated.seam.horizontal",
            )
        val leaf =
            VexWebAuthoring.structural(
                ref = VexWeb.StructuralRef.of("vexweb.test.generated.leaf"),
                placement = VexWeb.Placement.FillAvailable,
                depth = VexWeb.Depth.Content,
                presentation = VexWeb.Presentation.Surface,
            )
        val frame =
            VexWebAuthoring.frame(
                VexWeb.StructuralRef.of("vexweb.test.generated.frame"),
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
                    spanStart = VexWeb.XBoundary.Seam(vertical),
                    spanEnd = VexWeb.XBoundary.FrameEnd,
                )
                region(
                    ref = VexWeb.StructuralRef.of("vexweb.test.generated.region.left"),
                    start = VexWeb.XBoundary.FrameStart,
                    end = VexWeb.XBoundary.Seam(vertical),
                    top = VexWeb.YBoundary.FrameTop,
                    bottom = VexWeb.YBoundary.FrameBottom,
                    content = leaf,
                )
                region(
                    ref = VexWeb.StructuralRef.of("vexweb.test.generated.region.top-right"),
                    start = VexWeb.XBoundary.Seam(vertical),
                    end = VexWeb.XBoundary.FrameEnd,
                    top = VexWeb.YBoundary.FrameTop,
                    bottom = VexWeb.YBoundary.Seam(horizontal),
                    content =
                        VexWebAuthoring.structural(
                            ref = VexWeb.StructuralRef.of("vexweb.test.generated.top-right"),
                            placement = VexWeb.Placement.FillAvailable,
                            depth = VexWeb.Depth.Content,
                            presentation = VexWeb.Presentation.Surface,
                        ),
                )
                region(
                    ref = VexWeb.StructuralRef.of("vexweb.test.generated.region.bottom-right"),
                    start = VexWeb.XBoundary.Seam(vertical),
                    end = VexWeb.XBoundary.FrameEnd,
                    top = VexWeb.YBoundary.Seam(horizontal),
                    bottom = VexWeb.YBoundary.FrameBottom,
                    content =
                        VexWebAuthoring.structural(
                            ref = VexWeb.StructuralRef.of("vexweb.test.generated.bottom-right"),
                            placement = VexWeb.Placement.FillAvailable,
                            depth = VexWeb.Depth.Content,
                            presentation = VexWeb.Presentation.Surface,
                        ),
                )
            }
        val surface =
            VexWebAuthoring.surface(
                "screen.generated-three",
            ) {
                content(frame)
                overlay(
                    VexWebAuthoring.structural(
                        ref = VexWeb.StructuralRef.of("vexweb.test.generated.overlay"),
                        placement = VexWeb.Placement.Overlay,
                        depth = VexWeb.Depth.Transient,
                        presentation = VexWeb.Presentation.Surface,
                    ),
                )
            }
        val css =
            VexWebArtifacts.generate(
                surface,
            )
                .css

        assertTrue(
            css.contains(
                "[data-vex-ref=\"vexweb.test.generated.region.left\"]{left:0%;top:0%;width:50%;height:100%;}",
            ),
        )
        assertTrue(
            css.contains(
                "[data-vex-ref=\"vexweb.test.generated.region.top-right\"]{left:50%;top:0%;width:50%;height:50%;}",
            ),
        )
        assertTrue(
            css.contains(
                "[data-vex-ref=\"vexweb.test.generated.region.bottom-right\"]{left:50%;top:50%;width:50%;height:50%;}",
            ),
        )
    }
}

// [VXG RealForever]