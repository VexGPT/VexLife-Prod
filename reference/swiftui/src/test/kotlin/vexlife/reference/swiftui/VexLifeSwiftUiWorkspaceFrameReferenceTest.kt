package vexlife.reference.swiftui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import vexlife.web.VexLifeWebP1

class VexLifeSwiftUiWorkspaceFrameReferenceTest {
    @Test
    fun generationIsDeterministicAndConsumesCurrentTypedFrameRelationships() {
        val first =
            VexLifeSwiftUiWorkspaceFrameReference.generate()
        val second =
            VexLifeSwiftUiWorkspaceFrameReference.generate()

        assertEquals(
            first,
            second,
        )
        assertTrue(first.swift.contains("GeometryReader { geometry in"))
        assertTrue(first.swift.contains("VStack(spacing: 0)"))
        assertTrue(first.swift.contains("HStack(spacing: 0)"))
        assertTrue(
            first.swift.contains(
                VexLifeWebP1
                    .Refs
                    .WorkspaceFrame
                    .value,
            ),
        )
        assertTrue(
            first.swift.contains(
                VexLifeWebP1
                    .Refs
                    .BrowserRegion
                    .value,
            ),
        )
        assertTrue(
            first.swift.contains(
                VexLifeWebP1
                    .Refs
                    .CompanionRegion
                    .value,
            ),
        )
    }

    @Test
    fun currentAllocationsBecomeTargetProportionsWithoutAuthoredPixels() {
        val swift =
            VexLifeSwiftUiWorkspaceFrameReference.generate()
                .swift

        assertTrue(
            swift.contains(
                "geometry.size.height * " +
                    (
                        1.0 /
                            12.0
                    ).toString(),
            ),
        )
        assertTrue(
            swift.contains(
                "geometry.size.height * " +
                    (
                        11.0 /
                            12.0
                    ).toString(),
            ),
        )
        assertTrue(
            swift.contains(
                "geometry.size.width * 0.5",
            ),
        )
    }

    @Test
    fun manifestHoldsUnprovedGenericAndInteractiveSpatialMechanics() {
        val manifest =
            VexLifeSwiftUiWorkspaceFrameReference.generate()
                .semanticManifest

        assertTrue(
            manifest.contains(
                "\"seamRef\":\"" +
                    VexLifeWebP1
                        .Refs
                        .HeaderBodySeam
                        .value +
                    "\"",
            ),
        )
        assertTrue(
            manifest.contains(
                "\"seamRef\":\"" +
                    VexLifeWebP1
                        .Refs
                        .BrowserCompanionSeam
                        .value +
                    "\"",
            ),
        )
        assertTrue(
            manifest.contains(
                "\"relationRef\":\"full-span-single-seam\",\"disposition\":\"MAPPED\"",
            ),
        )
        assertTrue(
            manifest.contains(
                "\"relationRef\":\"seam-resize-collapse\",\"disposition\":\"HELD\"",
            ),
        )
        assertTrue(
            manifest.contains(
                "\"relationRef\":\"partial-or-multi-seam-frame\",\"disposition\":\"HELD\"",
            ),
        )
    }
}

// [VXG RealForever]
