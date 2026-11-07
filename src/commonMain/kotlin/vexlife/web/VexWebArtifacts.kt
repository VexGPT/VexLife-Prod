package vexlife.web

import vexlife.blueprint.VexUiElementRef

/**
 * Deterministic browser-artifact realization of one validated [VexWeb.Surface].
 *
 * VexWeb remains the source of manifestation meaning. This generator maps that accepted meaning to
 * browser syntax and emits a semantic manifest that preserves the source relationships. It does not
 * own product state, provider/runtime behavior, Conversation, or browser-context admission.
 */
object VexWebArtifacts {
    const val ManifestSchema =
        "vexweb.generated-semantic-manifest/v1"

    data class Bundle(
        val html: String,
        val css: String,
        val javascript: String,
        val semanticManifest: String,
    ) {
        fun content(
            path: String,
        ): String =
            when (path) {
                "index.html" -> html
                "vexweb.css" -> css
                "vexweb.js" -> javascript
                "vexweb-manifest.json" -> semanticManifest
                else -> error("Unknown generated VexWeb artifact: $path")
            }

        val paths: List<String> =
            listOf(
                "index.html",
                "vexweb.css",
                "vexweb.js",
                "vexweb-manifest.json",
            )
    }

    fun generate(
        surface: VexWeb.Surface,
    ): Bundle {
        val coordinates =
            surface.coordinates()
        val geometry =
            GeometryResolver.resolve(
                surface,
            )
        val nodes =
            nodeByRef(
                surface,
            )

        return Bundle(
            html = renderHtml(surface),
            css = renderCss(surface, geometry),
            javascript = renderJavascript(),
            semanticManifest =
                renderManifest(
                    surface = surface,
                    coordinates = coordinates,
                    geometry = geometry,
                    nodes = nodes,
                ),
        )
    }

    @ConsistentCopyVisibility
    private data class Fraction private constructor(
        val numerator: Long,
        val denominator: Long,
    ) : Comparable<Fraction> {
        init {
            require(denominator > 0L)
        }

        operator fun plus(
            other: Fraction,
        ): Fraction =
            of(
                numerator * other.denominator + other.numerator * denominator,
                denominator * other.denominator,
            )

        operator fun minus(
            other: Fraction,
        ): Fraction =
            of(
                numerator * other.denominator - other.numerator * denominator,
                denominator * other.denominator,
            )

        operator fun times(
            other: Fraction,
        ): Fraction =
            of(
                numerator * other.numerator,
                denominator * other.denominator,
            )

        override fun compareTo(
            other: Fraction,
        ): Int =
            (numerator * other.denominator)
                .compareTo(
                    other.numerator * denominator,
                )

        fun percent(): String {
            val scaled =
                numerator *
                    PercentScale /
                    denominator
            val whole =
                scaled /
                    PercentDigits
            val decimals =
                (scaled % PercentDigits)
                    .toString()
                    .padStart(
                        6,
                        '0',
                    )
                    .trimEnd(
                        '0',
                    )

            return if (
                decimals.isEmpty()
            ) {
                "$whole%"
            } else {
                "$whole.$decimals%"
            }
        }

        fun json(): String =
            "{\"numerator\":$numerator,\"denominator\":$denominator,\"percent\":\"${percent()}\"}"

        companion object {
            private const val PercentDigits =
                1_000_000L
            private const val PercentScale =
                100L *
                    PercentDigits

            val Zero =
                of(
                    0,
                    1,
                )
            val One =
                of(
                    1,
                    1,
                )

            fun ratio(
                beforeParts: Int,
                totalParts: Int,
            ): Fraction =
                of(
                    beforeParts.toLong(),
                    totalParts.toLong(),
                )

            private fun of(
                numerator: Long,
                denominator: Long,
            ): Fraction {
                require(denominator != 0L)
                val sign =
                    if (
                        denominator <
                        0L
                    ) {
                        -1L
                    } else {
                        1L
                    }
                val divisor =
                    gcd(
                        kotlin.math.abs(
                            numerator,
                        ),
                        kotlin.math.abs(
                            denominator,
                        ),
                    )

                return Fraction(
                    numerator = numerator / divisor * sign,
                    denominator = denominator / divisor * sign,
                )
            }

            private fun gcd(
                left: Long,
                right: Long,
            ): Long {
                var a =
                    left
                var b =
                    right

                while (
                    b !=
                    0L
                ) {
                    val next =
                        a %
                            b
                    a =
                        b
                    b =
                        next
                }

                return if (
                    a ==
                    0L
                ) {
                    1L
                } else {
                    a
                }
            }
        }
    }

    private data class Rect(
        val sourceStart: String,
        val sourceEnd: String,
        val sourceTop: String,
        val sourceBottom: String,
        val start: Fraction,
        val end: Fraction,
        val top: Fraction,
        val bottom: Fraction,
    )

    private data class SeamGeometry(
        val orientation: String,
        val sourceBetweenStart: String,
        val sourceBetweenEnd: String,
        val allocationBeforeParts: Int,
        val allocationAfterParts: Int,
        val sourceSpanStart: String,
        val sourceSpanEnd: String,
        val position: Fraction,
        val spanStart: Fraction,
        val spanEnd: Fraction,
    )

    private data class FrameGeometry(
        val frameRef: String,
        val seams: Map<String, SeamGeometry>,
        val regions: Map<String, Rect>,
    )

    private object GeometryResolver {
        fun resolve(
            surface: VexWeb.Surface,
        ): Map<String, FrameGeometry> {
            val result =
                linkedMapOf<String, FrameGeometry>()

            fun walk(
                node: VexWeb.Node,
            ) {
                if (
                    node is
                    VexWeb.Frame
                ) {
                    val geometry =
                        resolveFrame(
                            node,
                        )
                    check(
                        result.put(
                            node.ref.value,
                            geometry,
                        ) ==
                            null,
                    ) {
                        "duplicate generated Frame identity"
                    }
                }

                node.children.forEach(
                    ::walk,
                )
            }

            walk(
                surface.content,
            )
            walk(
                surface.overlayRoot,
            )

            return result
        }

        private fun resolveFrame(
            frame: VexWeb.Frame,
        ): FrameGeometry {
            val vertical =
                frame.seams
                    .filterIsInstance<VexWeb.Seam.Vertical>()
                    .associateBy {
                        it.ref
                    }
            val horizontal =
                frame.seams
                    .filterIsInstance<VexWeb.Seam.Horizontal>()
                    .associateBy {
                        it.ref
                    }
            val xCache =
                mutableMapOf<VexWeb.SeamRef.Vertical, Fraction>()
            val yCache =
                mutableMapOf<VexWeb.SeamRef.Horizontal, Fraction>()

            fun allocation(
                value: VexWeb.Parameter.Allocation,
            ): Fraction =
                when (value) {
                    is VexWeb.Parameter.Allocation.Ratio ->
                        Fraction.ratio(
                            value.beforeParts,
                            value.totalParts,
                        )
                }

            fun xBoundaryRef(
                boundary: VexWeb.XBoundary,
            ): String =
                when (boundary) {
                    VexWeb.XBoundary.FrameStart -> "FRAME_START"
                    VexWeb.XBoundary.FrameEnd -> "FRAME_END"
                    is VexWeb.XBoundary.Seam -> "SEAM:${boundary.ref.value}"
                }

            fun yBoundaryRef(
                boundary: VexWeb.YBoundary,
            ): String =
                when (boundary) {
                    VexWeb.YBoundary.FrameTop -> "FRAME_TOP"
                    VexWeb.YBoundary.FrameBottom -> "FRAME_BOTTOM"
                    is VexWeb.YBoundary.Seam -> "SEAM:${boundary.ref.value}"
                }

            fun ratioParts(
                value: VexWeb.Parameter.Allocation,
            ): Pair<Int, Int> =
                when (value) {
                    is VexWeb.Parameter.Allocation.Ratio ->
                        value.beforeParts to
                            value.afterParts
                }

            fun resolveX(
                boundary: VexWeb.XBoundary,
            ): Fraction =
                when (boundary) {
                    VexWeb.XBoundary.FrameStart -> Fraction.Zero
                    VexWeb.XBoundary.FrameEnd -> Fraction.One
                    is VexWeb.XBoundary.Seam ->
                        xCache.getOrPut(
                            boundary.ref,
                        ) {
                            val seam =
                                vertical.getValue(
                                    boundary.ref,
                                )
                            val start =
                                resolveX(
                                    seam.between.start,
                                )
                            val end =
                                resolveX(
                                    seam.between.end,
                                )

                            start +
                                (end - start) *
                                allocation(
                                    seam.allocation,
                                )
                        }
                }

            fun resolveY(
                boundary: VexWeb.YBoundary,
            ): Fraction =
                when (boundary) {
                    VexWeb.YBoundary.FrameTop -> Fraction.Zero
                    VexWeb.YBoundary.FrameBottom -> Fraction.One
                    is VexWeb.YBoundary.Seam ->
                        yCache.getOrPut(
                            boundary.ref,
                        ) {
                            val seam =
                                horizontal.getValue(
                                    boundary.ref,
                                )
                            val top =
                                resolveY(
                                    seam.between.top,
                                )
                            val bottom =
                                resolveY(
                                    seam.between.bottom,
                                )

                            top +
                                (bottom - top) *
                                allocation(
                                    seam.allocation,
                                )
                        }
                }

            val seamGeometry =
                linkedMapOf<String, SeamGeometry>()
            frame.seams.forEach { seam ->
                when (seam) {
                    is VexWeb.Seam.Vertical ->
                        seamGeometry[
                            seam.ref.value
                        ] =
                            ratioParts(
                                seam.allocation,
                            ).let { (beforeParts, afterParts) ->
                                SeamGeometry(
                                    orientation = "VERTICAL",
                                    sourceBetweenStart = xBoundaryRef(seam.between.start),
                                    sourceBetweenEnd = xBoundaryRef(seam.between.end),
                                    allocationBeforeParts = beforeParts,
                                    allocationAfterParts = afterParts,
                                    sourceSpanStart = yBoundaryRef(seam.span.top),
                                    sourceSpanEnd = yBoundaryRef(seam.span.bottom),
                                    position =
                                        resolveX(
                                            VexWeb.XBoundary.Seam(
                                                seam.ref,
                                            ),
                                        ),
                                    spanStart = resolveY(seam.span.top),
                                    spanEnd = resolveY(seam.span.bottom),
                                )
                            }

                    is VexWeb.Seam.Horizontal ->
                        seamGeometry[
                            seam.ref.value
                        ] =
                            ratioParts(
                                seam.allocation,
                            ).let { (beforeParts, afterParts) ->
                                SeamGeometry(
                                    orientation = "HORIZONTAL",
                                    sourceBetweenStart = yBoundaryRef(seam.between.top),
                                    sourceBetweenEnd = yBoundaryRef(seam.between.bottom),
                                    allocationBeforeParts = beforeParts,
                                    allocationAfterParts = afterParts,
                                    sourceSpanStart = xBoundaryRef(seam.span.start),
                                    sourceSpanEnd = xBoundaryRef(seam.span.end),
                                    position =
                                        resolveY(
                                            VexWeb.YBoundary.Seam(
                                                seam.ref,
                                            ),
                                        ),
                                    spanStart = resolveX(seam.span.start),
                                    spanEnd = resolveX(seam.span.end),
                                )
                            }
                }
            }

            val regions =
                linkedMapOf<String, Rect>()
            frame.regions.forEach { region ->
                regions[
                    region.ref.value
                ] =
                    Rect(
                        sourceStart = xBoundaryRef(region.bounds.start),
                        sourceEnd = xBoundaryRef(region.bounds.end),
                        sourceTop = yBoundaryRef(region.bounds.top),
                        sourceBottom = yBoundaryRef(region.bounds.bottom),
                        start = resolveX(region.bounds.start),
                        end = resolveX(region.bounds.end),
                        top = resolveY(region.bounds.top),
                        bottom = resolveY(region.bounds.bottom),
                    )
            }

            return FrameGeometry(
                frameRef = frame.ref.value,
                seams = seamGeometry,
                regions = regions,
            )
        }
    }

    private fun nodeByRef(
        surface: VexWeb.Surface,
    ): Map<String, VexWeb.Node> {
        val result =
            linkedMapOf<String, VexWeb.Node>()

        fun walk(
            node: VexWeb.Node,
        ) {
            check(
                result.put(
                    node.identity.ref,
                    node,
                ) ==
                    null,
            ) {
                "duplicate generated Node identity"
            }

            node.children.forEach(
                ::walk,
            )
        }

        walk(
            surface.content,
        )
        walk(
            surface.overlayRoot,
        )

        return result
    }

    private fun renderHtml(
        surface: VexWeb.Surface,
    ): String =
        buildString {
            append("<!doctype html>\n")
            append("<html lang=\"en\">\n")
            append("<head>\n")
            append("  <meta charset=\"utf-8\">\n")
            append("  <meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">\n")
            append("  <link rel=\"stylesheet\" href=\"vexweb.css\">\n")
            append("</head>\n")
            append("<body>\n")
            append("  <main data-vex-screen=\"")
            append(html(surface.screenRef))
            append("\">\n")
            renderNode(
                node = surface.content,
                indent = "    ",
            )
            renderNode(
                node = surface.overlayRoot,
                indent = "    ",
            )
            append("  </main>\n")
            append("  <script type=\"module\" src=\"vexweb.js\"></script>\n")
            append("</body>\n")
            append("</html>\n")
        }

    private fun StringBuilder.renderNode(
        node: VexWeb.Node,
        indent: String,
    ) {
        val tag =
            tagFor(
                node,
            )
        val attrs =
            nodeAttributes(
                node,
            )

        append(indent)
        append('<')
        append(tag)
        attrs.forEach { (name, value) ->
            append(' ')
            append(name)
            append("=\"")
            append(html(value))
            append('"')
        }
        if (
            node.presentation ==
            VexWeb.Presentation.TransientFeedback
        ) {
            append(" hidden")
        }
        if (
            node is
            VexWeb.Element
        ) {
            append(" data-vex-blueprint-kind=\"")
            append(
                node.element.kind.name
                    .lowercase(),
            )
            append('"')
        }
        append('>')

        val generatedPrimitive =
            generatedPrimitive(
                node,
            )
        val children =
            node.children

        if (
            generatedPrimitive ==
                null &&
            children.isEmpty()
        ) {
            append("</")
            append(tag)
            append(">\n")
            return
        }

        append('\n')
        if (
            generatedPrimitive !=
                null
        ) {
            append(indent)
            append("  ")
            append(generatedPrimitive)
            append('\n')
        }
        children.forEach {
            renderNode(
                node = it,
                indent = indent + "  ",
            )
        }
        append(indent)
        append("</")
        append(tag)
        append(">\n")
    }

    private fun tagFor(
        node: VexWeb.Node,
    ): String =
        when (node) {
            is VexWeb.Region ->
                "section"

            is VexWeb.Element ->
                when (node.element.kind) {
                    VexUiElementRef.Kind.Region -> "section"
                    VexUiElementRef.Kind.Content,
                    VexUiElementRef.Kind.TextField,
                    VexUiElementRef.Kind.Control,
                    VexUiElementRef.Kind.Collection,
                    ->
                        "div"
                }

            is VexWeb.Frame,
            is VexWeb.Structural,
            ->
                "div"
        }

    private fun generatedPrimitive(
        node: VexWeb.Node,
    ): String? {
        if (
            node !is
            VexWeb.Element
        ) {
            return null
        }

        return when (node.element.kind) {
            VexUiElementRef.Kind.TextField ->
                "<textarea data-vex-generated-part=\"text-field\"></textarea>"

            VexUiElementRef.Kind.Control ->
                "<button type=\"button\" data-vex-generated-part=\"control\"></button>"

            VexUiElementRef.Kind.Region,
            VexUiElementRef.Kind.Content,
            VexUiElementRef.Kind.Collection,
            ->
                null
        }
    }

    private fun nodeAttributes(
        node: VexWeb.Node,
    ): List<Pair<String, String>> {
        val attributes =
            mutableListOf(
                "data-vex-ref" to node.identity.ref,
                "data-vex-kind" to
                    when (node) {
                        is VexWeb.Frame -> "frame"
                        is VexWeb.Region -> "region"
                        is VexWeb.Element -> "element"
                        is VexWeb.Structural -> "structural"
                    },
                "data-vex-depth" to node.depth.name.lowercase(),
                "data-vex-placement" to node.placement.name.lowercase(),
                "data-vex-presentation" to node.presentation.name.lowercase(),
            )

        node.identity.sourceParentRef?.let {
            attributes +=
                "data-vex-source-parent" to
                    it
        }

        if (
            node.interactions.isNotEmpty()
        ) {
            attributes +=
                "data-vex-interactions" to
                    node.interactions
                        .map {
                            it.name
                                .lowercase()
                        }
                        .sorted()
                        .joinToString(
                            ",",
                        )
        }

        when (
            val binding =
                node.binding
        ) {
            VexWeb.Binding.None ->
                attributes +=
                    "data-vex-binding" to
                    "none"

            is VexWeb.Binding.LocalSpatial -> {
                attributes +=
                    "data-vex-binding" to
                    "local-spatial"
                attributes +=
                    "data-vex-spatial-target" to
                    binding.target.value
            }

            is VexWeb.Binding.LocalInteraction -> {
                attributes +=
                    "data-vex-binding" to
                    "local-interaction"
                attributes +=
                    "data-vex-interaction-target" to
                    binding.target.ref
            }

            is VexWeb.Binding.Capability -> {
                attributes +=
                    "data-vex-binding" to
                    "capability"
                attributes +=
                    "data-vex-capability" to
                    binding.capabilityRef.value
            }

            is VexWeb.Binding.Unbound -> {
                attributes +=
                    "data-vex-binding" to
                    "unbound"
                when (
                    val fallback =
                        binding.fallback
                ) {
                    is VexWeb.Fallback.NotYetImplemented -> {
                        attributes +=
                            "data-vex-capability" to
                            fallback.capabilityRef.value
                        attributes +=
                            "data-vex-feedback-target" to
                            fallback.feedbackTarget.value
                    }
                }
            }
        }

        return attributes
    }

    private fun renderCss(
        surface: VexWeb.Surface,
        geometry: Map<String, FrameGeometry>,
    ): String =
        buildString {
            append("/* Generated from VexWeb relationships. Do not edit by hand. */\n")
            append("html,body{margin:0;width:100%;height:100%;}\n")
            append("body{font-family:system-ui,-apple-system,sans-serif;}\n")
            append("[data-vex-screen]{position:relative;width:100vw;height:100vh;overflow:hidden;}\n")
            append("[data-vex-kind=\"frame\"]{position:relative;width:100%;height:100%;}\n")
            append("[data-vex-kind=\"region\"]{position:absolute;overflow:hidden;}\n")
            append("[data-vex-placement=\"fillavailable\"]{width:100%;height:100%;}\n")
            append("[data-vex-placement=\"overlay\"]{position:absolute;}\n")
            append(selector(surface.overlayRoot.identity.ref))
            append("{inset:0;pointer-events:none;}\n")
            append(selector(surface.overlayRoot.identity.ref))
            append(" [data-vex-interactions]{pointer-events:auto;}\n")
            append("[data-vex-generated-part=\"text-field\"]{box-sizing:border-box;width:100%;min-height:2.5rem;}\n")
            append("[data-vex-generated-part=\"control\"]{cursor:pointer;}\n")
            append("[data-vex-depth=\"content\"]{z-index:0;}\n")
            append("[data-vex-depth=\"chrome\"]{z-index:10;}\n")
            append("[data-vex-depth=\"transient\"]{z-index:20;}\n")
            append("[data-vex-depth=\"modal\"]{z-index:30;}\n")
            append("[data-vex-presentation=\"primaryaction\"]{cursor:pointer;}\n")
            append("[data-vex-presentation=\"transientfeedback\"]{position:absolute;inset:auto 1rem 1rem 1rem;}\n")

            geometry.values.forEach { frame ->
                frame.regions.forEach { (ref, rect) ->
                    append(selector(ref))
                    append("{left:")
                    append(rect.start.percent())
                    append(";top:")
                    append(rect.top.percent())
                    append(";width:")
                    append((rect.end - rect.start).percent())
                    append(";height:")
                    append((rect.bottom - rect.top).percent())
                    append(";}\n")
                }

                frame.seams.forEach { (seamRef, seam) ->
                    val handles =
                        localSpatialNodes(
                            surface,
                            seamRef,
                        )
                    handles.forEach { handleRef ->
                        append(selector(handleRef))
                        if (
                            seam.orientation ==
                            "VERTICAL"
                        ) {
                            append("{position:absolute;left:calc(")
                            append(seam.position.percent())
                            append(" - 2px);top:")
                            append(seam.spanStart.percent())
                            append(";width:4px;height:")
                            append((seam.spanEnd - seam.spanStart).percent())
                            append(";cursor:col-resize;}\n")
                        } else {
                            append("{position:absolute;top:calc(")
                            append(seam.position.percent())
                            append(" - 2px);left:")
                            append(seam.spanStart.percent())
                            append(";height:4px;width:")
                            append((seam.spanEnd - seam.spanStart).percent())
                            append(";cursor:row-resize;}\n")
                        }
                    }
                }
            }
        }

    private fun localSpatialNodes(
        surface: VexWeb.Surface,
        seamRef: String,
    ): List<String> =
        surface.coordinates()
            .filter {
                val binding =
                    it.binding
                binding is VexWeb.Binding.LocalSpatial &&
                    binding.target.value ==
                    seamRef
            }
            .map {
                it.ref
            }

    private fun renderJavascript(): String =
        """// Generated from VexWeb bindings. Do not edit by hand.
const root = document.querySelector('[data-vex-screen]');
root?.addEventListener('click', (event) => {
  const eventElement = event.target instanceof Element ? event.target : null;
  const control = eventElement?.closest('[data-vex-binding="unbound"]');
  if (!control || !root.contains(control)) return;
  const targetRef = control.dataset.vexFeedbackTarget;
  const capability = control.dataset.vexCapability;
  if (!targetRef || !capability) return;
  const target = [...root.querySelectorAll('[data-vex-ref]')].find((node) => node.dataset.vexRef === targetRef);
  if (!target) return;
  target.textContent = `Capability ${'$'}{capability} is not connected yet.`;
  target.hidden = false;
  target.dataset.vexFeedbackState = 'shown';
});
"""

    private fun renderManifest(
        surface: VexWeb.Surface,
        coordinates: List<VexWeb.Coordinate>,
        geometry: Map<String, FrameGeometry>,
        nodes: Map<String, VexWeb.Node>,
    ): String =
        buildString {
            append("{\n")
            append("  \"schemaVersion\":\"")
            append(json(ManifestSchema))
            append("\",\n")
            append("  \"screenRef\":\"")
            append(json(surface.screenRef))
            append("\",\n")
            append("  \"contentRef\":\"")
            append(json(surface.content.identity.ref))
            append("\",\n")
            append("  \"overlayRootRef\":\"")
            append(json(surface.overlayRoot.identity.ref))
            append("\",\n")
            append("  \"artifacts\":[\"index.html\",\"vexweb.css\",\"vexweb.js\",\"vexweb-manifest.json\"],\n")
            append("  \"coordinates\":[\n")
            coordinates.forEachIndexed { index, coordinate ->
                val node =
                    nodes.getValue(
                        coordinate.ref,
                    )
                append("    {")
                append("\"ref\":\"")
                append(json(coordinate.ref))
                append("\",\"nodeKind\":\"")
                append(nodeKind(node))
                append("\",\"blueprintKind\":")
                append(
                    if (
                        node is
                        VexWeb.Element
                    ) {
                        "\"" +
                            node.element.kind.name +
                            "\""
                    } else {
                        "null"
                    },
                )
                append(",\"sourceParentRef\":")
                append(jsonOrNull(coordinate.sourceParentRef))
                append(",\"containerRef\":\"")
                append(json(coordinate.containerRef))
                append("\",\"placement\":\"")
                append(coordinate.placement.name)
                append("\",\"depth\":\"")
                append(coordinate.depth.name)
                append("\",\"presentation\":\"")
                append(coordinate.presentation.name)
                append("\",\"interactions\":[")
                append(
                    coordinate.interactions
                        .map {
                            it.name
                        }
                        .sorted()
                        .joinToString(
                            ",",
                        ) {
                            "\"${json(it)}\""
                        },
                )
                append("],\"binding\":")
                append(bindingJson(coordinate.binding))
                append('}')
                if (
                    index !=
                    coordinates.lastIndex
                ) {
                    append(',')
                }
                append('\n')
            }
            append("  ],\n")
            append("  \"frames\":[\n")
            geometry.values.forEachIndexed { frameIndex, frame ->
                append("    {\"frameRef\":\"")
                append(json(frame.frameRef))
                append("\",\"seams\":[")
                frame.seams.entries.forEachIndexed { seamIndex, (ref, seam) ->
                    if (
                        seamIndex >
                        0
                    ) {
                        append(',')
                    }
                    append("{\"ref\":\"")
                    append(json(ref))
                    append("\",\"orientation\":\"")
                    append(seam.orientation)
                    append("\",\"betweenStart\":\"")
                    append(json(seam.sourceBetweenStart))
                    append("\",\"betweenEnd\":\"")
                    append(json(seam.sourceBetweenEnd))
                    append("\",\"allocation\":{\"beforeParts\":")
                    append(seam.allocationBeforeParts)
                    append(",\"afterParts\":")
                    append(seam.allocationAfterParts)
                    append("},\"spanStartBoundary\":\"")
                    append(json(seam.sourceSpanStart))
                    append("\",\"spanEndBoundary\":\"")
                    append(json(seam.sourceSpanEnd))
                    append("\",\"position\":")
                    append(seam.position.json())
                    append(",\"spanStart\":")
                    append(seam.spanStart.json())
                    append(",\"spanEnd\":")
                    append(seam.spanEnd.json())
                    append('}')
                }
                append("],\"regions\":[")
                frame.regions.entries.forEachIndexed { regionIndex, (ref, rect) ->
                    if (
                        regionIndex >
                        0
                    ) {
                        append(',')
                    }
                    append("{\"ref\":\"")
                    append(json(ref))
                    append("\",\"startBoundary\":\"")
                    append(json(rect.sourceStart))
                    append("\",\"endBoundary\":\"")
                    append(json(rect.sourceEnd))
                    append("\",\"topBoundary\":\"")
                    append(json(rect.sourceTop))
                    append("\",\"bottomBoundary\":\"")
                    append(json(rect.sourceBottom))
                    append("\",\"start\":")
                    append(rect.start.json())
                    append(",\"end\":")
                    append(rect.end.json())
                    append(",\"top\":")
                    append(rect.top.json())
                    append(",\"bottom\":")
                    append(rect.bottom.json())
                    append('}')
                }
                append("]}")
                if (
                    frameIndex !=
                    geometry.values.size -
                    1
                ) {
                    append(',')
                }
                append('\n')
            }
            append("  ]\n")
            append("}\n")
        }

    private fun nodeKind(
        node: VexWeb.Node,
    ): String =
        when (node) {
            is VexWeb.Frame -> "FRAME"
            is VexWeb.Region -> "REGION"
            is VexWeb.Element -> "BLUEPRINT_ELEMENT"
            is VexWeb.Structural -> "STRUCTURAL"
        }

    private fun bindingJson(
        binding: VexWeb.Binding,
    ): String =
        when (binding) {
            VexWeb.Binding.None ->
                "{\"type\":\"NONE\"}"

            is VexWeb.Binding.LocalSpatial ->
                "{\"type\":\"LOCAL_SPATIAL\",\"target\":\"${json(binding.target.value)}\"}"

            is VexWeb.Binding.LocalInteraction ->
                "{\"type\":\"LOCAL_INTERACTION\",\"target\":\"${json(binding.target.ref)}\"}"

            is VexWeb.Binding.Capability ->
                "{\"type\":\"CAPABILITY\",\"capabilityRef\":\"${json(binding.capabilityRef.value)}\"}"

            is VexWeb.Binding.Unbound ->
                when (
                    val fallback =
                        binding.fallback
                ) {
                    is VexWeb.Fallback.NotYetImplemented ->
                        "{\"type\":\"UNBOUND\",\"fallback\":{\"type\":\"NOT_YET_IMPLEMENTED\",\"capabilityRef\":\"${json(fallback.capabilityRef.value)}\",\"feedbackTarget\":\"${json(fallback.feedbackTarget.value)}\"}}"
                }
        }

    private fun selector(
        ref: String,
    ): String =
        "[data-vex-ref=\"$ref\"]"

    private fun html(
        value: String,
    ): String =
        value
            .replace("&", "&amp;")
            .replace("\"", "&quot;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")

    private fun jsonOrNull(
        value: String?,
    ): String =
        value
            ?.let {
                "\"${json(it)}\""
            }
            ?: "null"

    private fun json(
        value: String,
    ): String =
        buildString {
            value.forEach { character ->
                when (character) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> append(character)
                }
            }
        }
}

// [VXG RealForever]
