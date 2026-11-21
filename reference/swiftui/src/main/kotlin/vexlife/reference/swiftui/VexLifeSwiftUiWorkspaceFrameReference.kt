package vexlife.reference.swiftui

import vexlife.web.VexLifeWeb
import vexlife.web.VexLifeWebP1

/**
 * SwiftUI target pressure for the current VexLife-Prod nested Frame / Seam / Region relationships.
 *
 * This reference deliberately maps only the two current full-span single-Seam frames. It does not
 * claim generic partial/multi-Seam SwiftUI realization yet.
 */
object VexLifeSwiftUiWorkspaceFrameReference {
    const val ManifestSchema =
        "vexlife.swiftui-workspace-frame-reference-manifest/v1"

    data class Bundle(
        val swift: String,
        val semanticManifest: String,
    )

    private data class Split(
        val frameRef: String,
        val seamRef: String,
        val axis: Axis,
        val beforeRegionRef: String,
        val afterRegionRef: String,
        val beforeParts: Int,
        val afterParts: Int,
    ) {
        enum class Axis {
            HORIZONTAL,
            VERTICAL,
        }

        val totalParts: Int =
            beforeParts +
                afterParts
    }

    fun generate(): Bundle {
        val root =
            rootSplit()
        val body =
            bodySplit()

        return Bundle(
            swift =
                renderSwift(
                    root = root,
                    body = body,
                ),
            semanticManifest =
                renderManifest(
                    root = root,
                    body = body,
                ),
        )
    }

    private fun rootSplit(): Split {
        val frame =
            VexLifeWebP1.WorkspaceFrame
        val seam =
            frame.seams
                .singleOrNull() as?
                VexWeb.Seam.Horizontal
                ?: error(
                    "SwiftUI workspace reference requires one horizontal root Seam",
                )

        require(
            seam.ref ==
                VexLifeWebP1.Refs.HeaderBodySeam,
        )
        require(
            seam.between.top ==
                VexWeb.YBoundary.FrameTop &&
                seam.between.bottom ==
                    VexWeb.YBoundary.FrameBottom,
        ) {
            "root Seam must divide the full vertical Frame interval"
        }
        require(
            seam.span.start ==
                VexWeb.XBoundary.FrameStart &&
                seam.span.end ==
                    VexWeb.XBoundary.FrameEnd,
        ) {
            "root Seam must span the full Frame width"
        }

        val allocation =
            seam.allocation as?
                VexWeb.Parameter.Allocation.Ratio
                ?: error(
                    "SwiftUI workspace reference requires Ratio allocation",
                )
        val regions =
            frame.regions
                .associateBy {
                    it.ref
                        .value
                }

        require(
            regions.keys ==
                setOf(
                    VexLifeWebP1.Refs.HeaderRegion.value,
                    VexLifeWebP1.Refs.BodyRegion.value,
                ),
        ) {
            "root Frame region set drifted"
        }

        return Split(
            frameRef =
                frame.ref
                    .value,
            seamRef =
                seam.ref
                    .value,
            axis =
                Split.Axis.HORIZONTAL,
            beforeRegionRef =
                VexLifeWebP1.Refs.HeaderRegion.value,
            afterRegionRef =
                VexLifeWebP1.Refs.BodyRegion.value,
            beforeParts =
                allocation.beforeParts,
            afterParts =
                allocation.afterParts,
        )
    }

    private fun bodySplit(): Split {
        val frame =
            VexLifeWebP1.BodyFrame
        val seam =
            frame.seams
                .singleOrNull() as?
                VexWeb.Seam.Vertical
                ?: error(
                    "SwiftUI workspace reference requires one vertical body Seam",
                )

        require(
            seam.ref ==
                VexLifeWebP1.Refs.BrowserCompanionSeam,
        )
        require(
            seam.between.start ==
                VexWeb.XBoundary.FrameStart &&
                seam.between.end ==
                    VexWeb.XBoundary.FrameEnd,
        ) {
            "body Seam must divide the full horizontal Frame interval"
        }
        require(
            seam.span.top ==
                VexWeb.YBoundary.FrameTop &&
                seam.span.bottom ==
                    VexWeb.YBoundary.FrameBottom,
        ) {
            "body Seam must span the full Frame height"
        }

        val allocation =
            seam.allocation as?
                VexWeb.Parameter.Allocation.Ratio
                ?: error(
                    "SwiftUI workspace reference requires Ratio allocation",
                )
        val regions =
            frame.regions
                .associateBy {
                    it.ref
                        .value
                }

        require(
            regions.keys ==
                setOf(
                    VexLifeWebP1.Refs.BrowserRegion.value,
                    VexLifeWebP1.Refs.CompanionRegion.value,
                ),
        ) {
            "body Frame region set drifted"
        }

        return Split(
            frameRef =
                frame.ref
                    .value,
            seamRef =
                seam.ref
                    .value,
            axis =
                Split.Axis.VERTICAL,
            beforeRegionRef =
                VexLifeWebP1.Refs.BrowserRegion.value,
            afterRegionRef =
                VexLifeWebP1.Refs.CompanionRegion.value,
            beforeParts =
                allocation.beforeParts,
            afterParts =
                allocation.afterParts,
        )
    }

    private fun renderSwift(
        root: Split,
        body: Split,
    ): String =
        buildString {
            appendLine("// Generated from current VexLife Frame / Seam / Region relationships.")
            appendLine("import SwiftUI")
            appendLine()
            appendLine("public enum VexWorkspaceFrameIdentity {")
            appendLine(
                "    public static let workspaceFrameRef = " +
                    swiftString(
                        root.frameRef,
                    ),
            )
            appendLine(
                "    public static let headerRegionRef = " +
                    swiftString(
                        root.beforeRegionRef,
                    ),
            )
            appendLine(
                "    public static let bodyRegionRef = " +
                    swiftString(
                        root.afterRegionRef,
                    ),
            )
            appendLine(
                "    public static let bodyFrameRef = " +
                    swiftString(
                        body.frameRef,
                    ),
            )
            appendLine(
                "    public static let browserRegionRef = " +
                    swiftString(
                        body.beforeRegionRef,
                    ),
            )
            appendLine(
                "    public static let companionRegionRef = " +
                    swiftString(
                        body.afterRegionRef,
                    ),
            )
            appendLine("}")
            appendLine()
            appendLine(
                "public struct VexWorkspaceFrameReference<" +
                    "HeaderContent: View, BrowserContent: View, CompanionContent: View" +
                    ">: View {",
            )
            appendLine("    private let header: () -> HeaderContent")
            appendLine("    private let browser: () -> BrowserContent")
            appendLine("    private let companion: () -> CompanionContent")
            appendLine()
            appendLine("    public init(")
            appendLine("        @ViewBuilder header: @escaping () -> HeaderContent,")
            appendLine("        @ViewBuilder browser: @escaping () -> BrowserContent,")
            appendLine("        @ViewBuilder companion: @escaping () -> CompanionContent")
            appendLine("    ) {")
            appendLine("        self.header = header")
            appendLine("        self.browser = browser")
            appendLine("        self.companion = companion")
            appendLine("    }")
            appendLine()
            appendLine("    public var body: some View {")
            appendLine("        GeometryReader { geometry in")
            appendLine("            VStack(spacing: 0) {")
            appendLine("                header()")
            appendLine(
                "                    .frame(height: geometry.size.height * " +
                    swiftFraction(
                        root.beforeParts,
                        root.totalParts,
                    ) +
                    ")",
            )
            appendLine(
                "                    .accessibilityIdentifier(" +
                    "VexWorkspaceFrameIdentity.headerRegionRef" +
                    ")",
            )
            appendLine("                HStack(spacing: 0) {")
            appendLine("                    browser()")
            appendLine(
                "                        .frame(width: geometry.size.width * " +
                    swiftFraction(
                        body.beforeParts,
                        body.totalParts,
                    ) +
                    ")",
            )
            appendLine(
                "                        .accessibilityIdentifier(" +
                    "VexWorkspaceFrameIdentity.browserRegionRef" +
                    ")",
            )
            appendLine("                    companion()")
            appendLine(
                "                        .frame(width: geometry.size.width * " +
                    swiftFraction(
                        body.afterParts,
                        body.totalParts,
                    ) +
                    ")",
            )
            appendLine(
                "                        .accessibilityIdentifier(" +
                    "VexWorkspaceFrameIdentity.companionRegionRef" +
                    ")",
            )
            appendLine("                }")
            appendLine(
                "                .frame(height: geometry.size.height * " +
                    swiftFraction(
                        root.afterParts,
                        root.totalParts,
                    ) +
                    ")",
            )
            appendLine(
                "                .accessibilityIdentifier(" +
                    "VexWorkspaceFrameIdentity.bodyRegionRef" +
                    ")",
            )
            appendLine("            }")
            appendLine(
                "            .accessibilityIdentifier(" +
                    "VexWorkspaceFrameIdentity.workspaceFrameRef" +
                    ")",
            )
            appendLine("        }")
            appendLine("    }")
            appendLine("}")
        }

    private fun renderManifest(
        root: Split,
        body: Split,
    ): String =
        buildString {
            append("{\n")
            append("  \"schemaVersion\":")
            append(jsonString(ManifestSchema))
            append(",\n")
            append("  \"target\":{\"framework\":\"SWIFTUI\",\"root\":\"GeometryReader/VStack/HStack\"},\n")
            append("  \"splits\":[\n")
            appendSplit(
                split = root,
                target = "VStack proportional height",
            )
            append(",\n")
            appendSplit(
                split = body,
                target = "HStack proportional width",
            )
            append("\n  ],\n")
            append("  \"relationships\":[\n")
            append(
                "    {\"relationRef\":\"full-span-single-seam\",\"disposition\":\"MAPPED\"," +
                    "\"target\":\"nested VStack/HStack proportional split\"},\n",
            )
            append(
                "    {\"relationRef\":\"region-identity\",\"disposition\":\"MAPPED\"," +
                    "\"target\":\"accessibilityIdentifier\"},\n",
            )
            append(
                "    {\"relationRef\":\"seam-resize-collapse\",\"disposition\":\"HELD\"," +
                    "\"target\":\"native drag/collapse interaction not selected\"},\n",
            )
            append(
                "    {\"relationRef\":\"partial-or-multi-seam-frame\",\"disposition\":\"HELD\"," +
                    "\"target\":\"generic SwiftUI geometry realization not yet accepted\"},\n",
            )
            append(
                "    {\"relationRef\":\"overlay-depth\",\"disposition\":\"HELD\"," +
                    "\"target\":\"this bounded frame reference maps content geometry only\"}\n",
            )
            append("  ]\n")
            append("}\n")
        }

    private fun StringBuilder.appendSplit(
        split: Split,
        target: String,
    ) {
        append("    {")
        append("\"frameRef\":")
        append(jsonString(split.frameRef))
        append(",\"seamRef\":")
        append(jsonString(split.seamRef))
        append(",\"axis\":")
        append(jsonString(split.axis.name))
        append(",\"beforeRegionRef\":")
        append(jsonString(split.beforeRegionRef))
        append(",\"afterRegionRef\":")
        append(jsonString(split.afterRegionRef))
        append(",\"allocation\":{\"beforeParts\":")
        append(split.beforeParts)
        append(",\"afterParts\":")
        append(split.afterParts)
        append("}")
        append(",\"target\":")
        append(jsonString(target))
        append("}")
    }

    private fun swiftFraction(
        numerator: Int,
        denominator: Int,
    ): String =
        numerator
            .toDouble()
            .div(
                denominator
                    .toDouble(),
            )
            .toString()

    private fun swiftString(value: String): String =
        "\"" +
            value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n") +
            "\""

    private fun jsonString(value: String): String =
        "\"" +
            value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n") +
            "\""
}

// [VXG RealForever]
