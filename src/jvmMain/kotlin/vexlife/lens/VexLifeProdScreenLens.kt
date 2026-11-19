package vexlife.lens

import java.nio.file.Files
import java.nio.file.Path
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import vexlife.blueprint.VexUiElementRef
import vexlife.continuity.VexLifePrivateModeFamily
import vexlife.context.VexLifeContextProfileFamily
import vexlife.person.VexLifeLanguageFamily
import vexlife.service.VexLifeProdServiceContract
import vexlife.vision.VexLifeVisionFamily
import vexlife.web.VexLifeWebP1
import vexlife.web.VexLifeWebP1ExperienceMarkup
import vexlife.web.VexLifeWebP1ExperienceStyle
import vexlife.web.VexWeb
import vexlife.workspace.VexLifeWorkspaceBlueprint

/**
 * Product-owned export of the VexLife-Prod workspace into VexBoard's `vexboard.screen-lens/v1`.
 *
 * VexBoard projects; it does not decide meaning. So everything here is read from Product source:
 * elements and their nesting from [VexLifeWorkspaceBlueprint], how each one is bound from the
 * VexWeb coordinates of [VexLifeWebP1.Surface], and the deeper pathway of each capability from the
 * typed constants of the owners it reaches (service paths, Family Controls). A renamed owner fails
 * to compile here instead of leaving the Lens describing something that no longer exists.
 *
 * The workspace is exported as it is realized, not as a list: [VexLifeWebP1.Surface] already authors
 * a browser workspace (frames divided by seams into regions, a seam handle, structural groups, an
 * overlay layer). Those structural nodes become Lens elements, every element sits in the VexWeb
 * container that realizes it, and the screen carries a `layout` with each frame's seams, their
 * allocations and each region's bounds. A Lens that reads the layout can draw the header, the
 * Browser and the Companion where the Product puts them; one that does not still has the tree.
 *
 * The output carries no commit or timestamp: VexWork pins the commit when it takes a build, so the
 * same source always exports the same bytes.
 */
object VexLifeProdScreenLens {
    const val Schema = "vexboard.screen-lens/v1"
    const val Generator = "vexlife-prod-blueprint-export/1"
    const val CommittedPath = "docs/lens/vexlife-prod.screen-lens.json"

    private const val Common = "common:"
    private const val Jvm = "jvm:"

    private val json = Json {
        prettyPrint = true
        prettyPrintIndent = "  "
    }

    fun exportText(): String = json.encodeToString(JsonObject.serializer(), export()) + "\n"

    /** Every element ref the Lens actually exports; a catalog key's `lens` namespace must name one of these. */
    fun elementRefs(): Set<String> =
        ((export()["screens"] as JsonArray).single() as JsonObject)["elements"]
            .let { it as JsonArray }
            .map { ((it as JsonObject)["ref"] as JsonPrimitive).content }
            .toSet()

    fun export(): JsonObject {
        val blueprint = VexLifeWorkspaceBlueprint
        val coordinates = VexLifeWebP1.Surface.coordinates().associateBy { it.ref }
        val symbols = blueprintSymbols()
        val pathways = mutableListOf<JsonObject>()
        val elementPathway = mutableMapOf<String, String>()

        fun bind(elementRef: String, pathway: JsonObject) {
            val ref = (pathway["pathwayRef"] as JsonPrimitive).content
            if (pathways.none { (it["pathwayRef"] as JsonPrimitive).content == ref })
                pathways += pathway
            elementPathway[elementRef] = ref
        }

        for (element in blueprint.elements) {
            when (val binding = coordinates[element.ref]?.binding) {
                is VexWeb.Binding.Capability ->
                    capabilityPathway(binding.capabilityRef.value)?.let { bind(element.ref, it) }
                is VexWeb.Binding.LocalInteraction ->
                    bind(element.ref, localPathway(element, binding.target.ref))
                else -> Unit
            }
        }

        val spatial = spatialRealization()
        for ((handleRef, pathway) in spatial.handlePathways) bind(handleRef, pathway)
        val blueprintByRef = blueprint.elements.associateBy { it.ref }
        val screenRef = blueprint.screenRef

        fun blueprintElement(element: VexUiElementRef): JsonObject {
            val coordinate = coordinates[element.ref]
            return buildJsonObject {
                put("ref", element.ref)
                put("kind", lensKind(element, coordinate))
                put("label", humanLabel(element))
                // The container that realizes it on screen; the Blueprint parent when VexWeb does not place it.
                put("parentRef", coordinate?.containerRef?.takeIf { it != screenRef } ?: element.parentRef)
                put("symbol", symbols[element.ref]?.let { "VexLifeWorkspaceBlueprint.$it" })
                put("declared", declaration(element, symbols))
                put(
                    "intent",
                    when (val binding = coordinate?.binding) {
                        is VexWeb.Binding.Capability ->
                            "capability ${binding.capabilityRef.value}"
                        is VexWeb.Binding.LocalInteraction ->
                            presentationIntent(element)
                                ?: "local ${binding.target.ref}"
                        else -> null
                    },
                )
                put("pathwayRef", elementPathway[element.ref])
                put("style", coordinate?.presentation?.name)
                put("slot", coordinate?.placement?.name)
                put("layer", coordinate?.depth?.name?.uppercase())
                interactions(coordinate?.interactions)?.let { put("interactions", it) }
            }
        }

        // VexWeb order is the realized order: structural nodes and Blueprint elements as the Surface
        // walks them, then any Blueprint element the Surface does not place.
        val elements = buildJsonArray {
            val emitted = mutableSetOf<String>()
            for (ref in spatial.order) {
                val element = blueprintByRef[ref]
                add(
                    if (element != null) blueprintElement(element)
                    else spatial.elements.getValue(ref).let { node ->
                        elementPathway[ref]?.let { JsonObject(node + ("pathwayRef" to JsonPrimitive(it))) } ?: node
                    }
                )
                emitted += ref
            }
            for (element in blueprint.elements) if (element.ref !in emitted) add(blueprintElement(element))
        }

        return buildJsonObject {
            put("schemaVersion", Schema)
            put("lensRef", "lens.vexlife-prod")
            put("title", "VexLife Prod · Screen Lens")
            put(
                "summary",
                "The VexLife workspace exported by VexLife-Prod from its own Blueprint, VexWeb coordinates and owner constants.",
            )
            put("entryScreenRef", blueprint.screenRef)
            put("entryElementRef", VexLifeWorkspaceBlueprint.Send.ref)
            put(
                "relatedBoardRefs",
                buildJsonArray {
                    add(JsonPrimitive("board.vexlife-prod.perceivability-alignment"))
                },
            )
            put(
                "sourceSnapshot",
                buildJsonObject {
                    put("repositoryRef", "repo.vexlife-prod")
                    put("repositoryUrl", "https://github.com/VexGPT/VexLife-Prod")
                    put(
                        "roots",
                        buildJsonObject {
                            put("common", "src/commonMain/kotlin/vexlife/")
                            put("jvm", "src/jvmMain/kotlin/vexlife/")
                        },
                    )
                    put(
                        "notes",
                        buildJsonArray {
                            add(
                                JsonPrimitive(
                                    "Exported by vexlife.lens.VexLifeProdScreenLens; VexWork pins the commit."
                                )
                            )
                            add(
                                JsonPrimitive(
                                    "Element labels are Blueprint ids made readable. Visible copy is dynamic and owned by each Family's DisplayState."
                                )
                            )
                        },
                    )
                },
            )
            put("chrome", JsonArray(emptyList()))
            put(
                "screens",
                buildJsonArray {
                    add(
                        buildJsonObject {
                            put("screenRef", blueprint.screenRef)
                            put("label", "VexLife workspace")
                            put("placement", "PRIMARY")
                            put("symbol", "VexLifeWorkspaceBlueprint")
                            put("source", Common + "workspace/VexLifeWorkspaceBlueprint.kt")
                            put("mapped", "MAPPED")
                            put(
                                "summary",
                                "Browser and Companion side by side: converse with Vex, show Vex a window, and see what Vex receives.",
                            )
                            put("elements", elements)
                            put("layout", spatial.layout)
                            put("view", staticPresentationView())
                            put("realization", presentationRealization())
                        }
                    )
                },
            )
            put("pathways", JsonArray(pathways))
            put(
                "annotations",
                buildJsonArray {
                    add(
                        buildJsonObject {
                            put("ref", "note.vexlife-prod.memory.projection-only")
                            put("subjectRef", VexLifeWorkspaceBlueprint.ContinuityMemory.ref)
                            put("text", "Projection only")
                            put(
                                "detail",
                                "Shows VexLifeMemoryFamily's DisplayState from GET ${VexLifeProdServiceContract.MemoryPath}. Memory Controls are not connected to ordinary Product UI/runtime yet.",
                            )
                        }
                    )
                },
            )
            put(
                "provenance",
                buildJsonObject {
                    put("generator", Generator)
                    put("exporter", Jvm + "lens/VexLifeProdScreenLens.kt")
                },
            )
        }
    }

    /**
     * Product-owned static presentation for Screen Lens Surface projection.
     *
     * The Lens consumes the same HTML/CSS sources as the live VexLife workspace but never exports
     * Product JavaScript. Stable data-vex refs let VexBoard bind inspection to the Product's actual
     * rendered boundaries without becoming a second layout owner.
     */
    private fun staticPresentationView(): JsonObject =
        buildJsonObject {
            put("kind", "STATIC_HTML_CSS")
            put("html", VexLifeWebP1ExperienceMarkup.render())
            put("css", VexLifeWebP1ExperienceStyle.render())
            put("scriptPolicy", "NONE")
            put("htmlSource", Common + "web/VexLifeWebP1ExperienceMarkup.kt")
            put("cssSource", Common + "web/VexLifeWebP1ExperienceStyle.kt")
            put(
                "anchors",
                buildJsonObject {
                    put("screenAttribute", "data-vex-screen")
                    put("elementAttribute", "data-vex-ref")
                    put("structuralAttribute", "data-vex-structural-ref")
                    put("structuralParentAttribute", "data-vex-structural-parent")
                },
            )
        }

    /**
     * Runtime-owned presentation truth for the source-default Screen Lens expression.
     *
     * This is deliberately not Product semantic State. It projects only what
     * [VexLifeWebP1RuntimeArtifacts] already shows/hides in the browser:
     * one Browser app, one Companion view, bounded transient panels, and
     * source-owned conditional projections such as PrivateBanner.
     */
    private fun presentationRealization(): JsonObject {
        val source = Common + "web/VexLifeWebP1RuntimeArtifacts.kt"

        fun choice(
            condition: String,
            targetRef: String? = null,
            realizes: List<String> = emptyList(),
        ): JsonObject =
            buildJsonObject {
                put("condition", condition)
                if (targetRef != null) put("targetRef", targetRef)
                put(
                    "realizes",
                    buildJsonArray {
                        realizes.forEach { add(JsonPrimitive(it)) }
                    },
                )
            }

        fun selection(
            ref: String,
            subjectRef: String,
            defaultRef: String,
            choices: List<JsonObject>,
        ): JsonObject =
            buildJsonObject {
                put("ref", ref)
                put("kind", "SELECTION")
                put("subjectRef", subjectRef)
                put("owner", "VexLifeWebP1RuntimeArtifacts")
                put("defaultRef", defaultRef)
                put("exhaustive", true)
                put("source", source)
                put("choices", JsonArray(choices))
            }

        fun presentation(
            ref: String,
            subjectRef: String,
            condition: String,
        ): JsonObject =
            buildJsonObject {
                put("ref", ref)
                put("kind", "PRESENTATION")
                put("subjectRef", subjectRef)
                put("owner", "VexLifeWebP1RuntimeArtifacts")
                put("condition", condition)
                put("source", source)
            }

        fun conditional(
            ref: String,
            subjectRef: String,
            condition: String,
            realizes: String,
        ): JsonObject =
            buildJsonObject {
                put("ref", ref)
                put("kind", "BRANCH")
                put("subjectRef", subjectRef)
                put("owner", "VexLifeWebP1RuntimeArtifacts")
                put("exhaustive", false)
                put("source", source)
                put(
                    "choices",
                    buildJsonArray {
                        add(
                            choice(
                                condition = condition,
                                realizes = listOf(realizes),
                            ),
                        )
                    },
                )
            }

        return buildJsonObject {
            put("status", "PROJECTED")
            put(
                "entry",
                buildJsonObject {
                    put("symbol", "VexLifeWebP1RuntimeArtifacts")
                    put("source", source)
                },
            )
            put("rendersRegisteredTree", VexLifeWorkspaceBlueprint.screenRef)
            put(
                "realizers",
                buildJsonArray {
                    add(JsonPrimitive("VexLifeWebP1RuntimeArtifacts"))
                },
            )
            put(
                "presence",
                buildJsonArray {
                    // Browser runtime: one app at a time; Source Template is the fallback/default.
                    add(
                        selection(
                            ref = "presence.vexlife-workspace.browser.app-selection",
                            subjectRef = VexLifeWorkspaceBlueprint.Browser.ref,
                            defaultRef = VexLifeWorkspaceBlueprint.SourceTemplateFamily.ref,
                            choices =
                                listOf(
                                    choice(
                                        condition = "state.app === apps.sourceTemplate",
                                        targetRef = VexLifeWorkspaceBlueprint.SourceTemplateFamily.ref,
                                        realizes =
                                            listOf(
                                                VexLifeWorkspaceBlueprint.BrowserFrameStatus.ref,
                                                VexLifeWorkspaceBlueprint.BrowserViewport.ref,
                                            ),
                                    ),
                                    choice(
                                        condition = "state.app === apps.archives",
                                        targetRef = VexLifeWorkspaceBlueprint.Archives.ref,
                                    ),
                                    choice(
                                        condition = "state.app === apps.vexContext",
                                        targetRef = VexLifeWorkspaceBlueprint.VexContext.ref,
                                    ),
                                ),
                        ),
                    )

                    // Companion runtime: Conversation and Settings are mutually exclusive views.
                    add(
                        selection(
                            ref = "presence.vexlife-workspace.companion.content-selection",
                            subjectRef = VexLifeWebP1.Refs.CompanionContent.value,
                            defaultRef = VexLifeWorkspaceBlueprint.Conversation.ref,
                            choices =
                                listOf(
                                    choice(
                                        condition = "state.view === 'conversation'",
                                        targetRef = VexLifeWorkspaceBlueprint.Conversation.ref,
                                    ),
                                    choice(
                                        condition = "state.view === 'settings'",
                                        targetRef = VexLifeWorkspaceBlueprint.Settings.ref,
                                    ),
                                ),
                        ),
                    )

                    add(
                        presentation(
                            ref = "presence.vexlife-workspace.header.language-panel-open",
                            subjectRef = VexLifeWorkspaceBlueprint.LanguagePanel.ref,
                            condition = "state.languageOpen",
                        ),
                    )
                    add(
                        presentation(
                            ref = "presence.vexlife-workspace.header.release-alerts-panel-open",
                            subjectRef = VexLifeWorkspaceBlueprint.ReleaseAlertsPanel.ref,
                            condition = "state.releaseAlertsOpen",
                        ),
                    )
                    add(
                        presentation(
                            ref = "presence.vexlife-workspace.companion.context-inspector-open",
                            subjectRef = VexLifeWorkspaceBlueprint.ContextInspector.ref,
                            condition = "ui.inspector.open",
                        ),
                    )
                    add(
                        presentation(
                            ref = "presence.vexlife-workspace.snackbar-visible",
                            subjectRef = VexLifeWebP1.Refs.Snackbar.value,
                            condition = "!ui.snackbar.hidden",
                        ),
                    )

                    // These are semantic/data-conditioned projections, not presentation toggles.
                    add(
                        conditional(
                            ref = "presence.vexlife-workspace.companion.private-banner",
                            subjectRef = VexLifeWorkspaceBlueprint.Conversation.ref,
                            condition = "state.privateMode?.state === VexLifePrivateModeFamily.StatePrivate",
                            realizes = VexLifeWorkspaceBlueprint.PrivateBanner.ref,
                        ),
                    )
                    add(
                        conditional(
                            ref = "presence.vexlife-workspace.companion.context-receipt",
                            subjectRef = VexLifeWorkspaceBlueprint.Composer.ref,
                            condition = "state.includePage && state.browserContext != null",
                            realizes = VexLifeWorkspaceBlueprint.ContextReceipt.ref,
                        ),
                    )
                },
            )
        }
    }

    private fun presentationIntent(element: VexUiElementRef): String? =
        when (element.ref) {
            VexLifeWorkspaceBlueprint.CompanionViewSwitch.ref ->
                "swapTo(container = CompanionContent, child = Settings)"
            VexLifeWorkspaceBlueprint.Language.ref ->
                "toggle(LanguagePanel)"
            VexLifeWorkspaceBlueprint.ReleaseAlerts.ref ->
                "toggle(ReleaseAlertsPanel)"
            else -> null
        }

    private class SpatialRealization(
        /** Every VexWeb node ref in Surface walk order, seams right after their frame. */
        val order: List<String>,
        /** Lens elements for the structural nodes (Blueprint elements are exported from the Blueprint). */
        val elements: Map<String, JsonObject>,
        val layout: JsonObject,
        val handlePathways: List<Pair<String, JsonObject>>,
    )

    /**
     * The workspace as [VexLifeWebP1.Surface] realizes it: frames, their seams and regions, chrome,
     * structural groups and the overlay layer, each named by its `VexLifeWebP1.Refs` constant.
     */
    private fun spatialRealization(): SpatialRealization {
        val surface = VexLifeWebP1.Surface
        val names = structuralSymbols()
        val coordinates = surface.coordinates().associateBy { it.ref }
        val order = mutableListOf<String>()
        val elements = linkedMapOf<String, JsonObject>()
        val frames = mutableListOf<JsonObject>()
        val handlePathways = mutableListOf<Pair<String, JsonObject>>()

        fun label(ref: String): String =
            names[ref]?.replace(Regex("(?<=[a-z])(?=[A-Z])"), " ")?.lowercase()?.replaceFirstChar { it.uppercase() }
                ?: ref.substringAfterLast('.')

        fun structural(
            ref: String,
            kind: String,
            declared: String,
            parentRef: String?,
            coordinate: VexWeb.Coordinate?,
            intent: String? = null,
        ): JsonObject = buildJsonObject {
            put("ref", ref)
            put("kind", kind)
            put("label", label(ref))
            put("parentRef", parentRef)
            put("symbol", names[ref]?.let { "VexLifeWebP1.Refs.$it" })
            put("declared", declared)
            put("intent", intent)
            put("pathwayRef", null as String?)
            put("style", coordinate?.presentation?.name)
            put("slot", coordinate?.placement?.name)
            put("layer", coordinate?.depth?.name?.uppercase())
            interactions(coordinate?.interactions)?.let { put("interactions", it) }
        }

        fun refOf(ref: String) = names[ref]?.let { "Refs.$it" } ?: "\"$ref\""

        fun x(boundary: VexWeb.XBoundary): String =
            when (boundary) {
                VexWeb.XBoundary.FrameStart -> "FRAME_START"
                VexWeb.XBoundary.FrameEnd -> "FRAME_END"
                is VexWeb.XBoundary.Seam -> boundary.ref.value
            }

        fun y(boundary: VexWeb.YBoundary): String =
            when (boundary) {
                VexWeb.YBoundary.FrameTop -> "FRAME_TOP"
                VexWeb.YBoundary.FrameBottom -> "FRAME_BOTTOM"
                is VexWeb.YBoundary.Seam -> boundary.ref.value
            }

        fun allocation(value: VexWeb.Parameter.Allocation): JsonObject =
            when (value) {
                is VexWeb.Parameter.Allocation.Ratio ->
                    buildJsonObject {
                        put("before", value.beforeParts)
                        put("after", value.afterParts)
                    }
            }

        fun walk(node: VexWeb.Node) {
            val ref = node.identity.ref
            val coordinate = coordinates[ref]
            val parent = coordinate?.containerRef?.takeIf { it != surface.screenRef }
            order += ref
            when (node) {
                is VexWeb.Element -> Unit
                is VexWeb.Frame -> {
                    elements[ref] = structural(ref, "FRAME", "frame(${refOf(ref)})", parent, coordinate)
                    for (seam in node.seams) {
                        val seamRef = seam.ref.value
                        val vertical = seam is VexWeb.Seam.Vertical
                        order += seamRef
                        elements[seamRef] =
                            structural(
                                seamRef,
                                "SEAM",
                                "${if (vertical) "verticalSeam" else "horizontalSeam"}(${refOf(seamRef)})",
                                ref,
                                null,
                            )
                    }
                    frames +=
                        buildJsonObject {
                            put("ref", ref)
                            put(
                                "seams",
                                buildJsonArray {
                                    for (seam in node.seams) add(
                                        buildJsonObject {
                                            put("ref", seam.ref.value)
                                            when (seam) {
                                                is VexWeb.Seam.Vertical -> {
                                                    put("axis", "VERTICAL")
                                                    put("between", buildJsonArray { add(JsonPrimitive(x(seam.between.start))); add(JsonPrimitive(x(seam.between.end))) })
                                                    put("span", buildJsonArray { add(JsonPrimitive(y(seam.span.top))); add(JsonPrimitive(y(seam.span.bottom))) })
                                                }
                                                is VexWeb.Seam.Horizontal -> {
                                                    put("axis", "HORIZONTAL")
                                                    put("between", buildJsonArray { add(JsonPrimitive(y(seam.between.top))); add(JsonPrimitive(y(seam.between.bottom))) })
                                                    put("span", buildJsonArray { add(JsonPrimitive(x(seam.span.start))); add(JsonPrimitive(x(seam.span.end))) })
                                                }
                                            }
                                            put("allocation", allocation(seam.allocation))
                                        }
                                    )
                                },
                            )
                            put(
                                "regions",
                                buildJsonArray {
                                    for (region in node.regions) add(
                                        buildJsonObject {
                                            put("ref", region.ref.value)
                                            put("start", x(region.bounds.start))
                                            put("end", x(region.bounds.end))
                                            put("top", y(region.bounds.top))
                                            put("bottom", y(region.bounds.bottom))
                                        }
                                    )
                                },
                            )
                            put(
                                "chrome",
                                buildJsonArray {
                                    for (chrome in node.chrome) add(
                                        buildJsonObject {
                                            put("ref", chrome.identity.ref)
                                            put("seamRef", (chrome.binding as? VexWeb.Binding.LocalSpatial)?.target?.value)
                                        }
                                    )
                                },
                            )
                        }
                }
                is VexWeb.Region ->
                    elements[ref] = structural(ref, "REGION", "region(${refOf(ref)})", parent, coordinate)
                is VexWeb.Structural -> {
                    val binding = node.binding
                    if (node.presentation == VexWeb.Presentation.SeamHandle && binding is VexWeb.Binding.LocalSpatial) {
                        elements[ref] =
                            structural(ref, "HANDLE", "structural(${refOf(ref)})", parent, coordinate, intent = "local spatial ${binding.target.value}")
                        handlePathways += ref to spatialPathway(ref, label(ref), binding.target.value, label(binding.target.value), node.interactions)
                    } else {
                        elements[ref] = structural(ref, "REGION", "structural(${refOf(ref)})", parent, coordinate)
                    }
                }
            }
            node.children.forEach { walk(it) }
        }
        walk(surface.content)
        walk(surface.overlayRoot)

        val layout = buildJsonObject {
            put("kind", "FRAME")
            put("symbol", "VexLifeWebP1.Surface")
            put("source", Common + "web/VexLifeWebP1.kt")
            put("root", surface.content.identity.ref)
            put("frames", JsonArray(frames))
            put("overlays", buildJsonArray { add(JsonPrimitive(surface.overlayRoot.identity.ref)) })
        }
        return SpatialRealization(order, elements, layout, handlePathways)
    }

    /**
     * The seam handle's pathway: a presentation-local spatial interaction. It resizes or collapses
     * the regions either side of its seam and decides nothing; no Family owns it.
     */
    private fun spatialPathway(
        handleRef: String,
        handleLabel: String,
        seamRef: String,
        seamLabel: String,
        interactions: Set<VexWeb.Interaction>,
    ): JsonObject {
        val frame =
            buildList<VexWeb.Frame> {
                fun collect(node: VexWeb.Node) {
                    if (node is VexWeb.Frame) add(node)
                    node.children.forEach(::collect)
                }
                collect(VexLifeWebP1.Surface.content)
            }.single { frame -> frame.seams.any { it.ref.value == seamRef } }
        val names = structuralSymbols()
        fun name(ref: String) =
            names[ref]?.replace(Regex("(?<=[a-z])(?=[A-Z])"), " ")?.lowercase()?.replaceFirstChar { it.uppercase() } ?: ref
        val before =
            frame.regions.filter { region ->
                (region.bounds.end as? VexWeb.XBoundary.Seam)?.ref?.value == seamRef ||
                    (region.bounds.bottom as? VexWeb.YBoundary.Seam)?.ref?.value == seamRef
            }.map { it.ref.value }
        val after =
            frame.regions.filter { region ->
                (region.bounds.start as? VexWeb.XBoundary.Seam)?.ref?.value == seamRef ||
                    (region.bounds.top as? VexWeb.YBoundary.Seam)?.ref?.value == seamRef
            }.map { it.ref.value }
        val n = "vexlife-prod.spatial.${handleRef.removePrefix("vexweb.vexlife-workspace.p1.")}"
        val verbs = interactions.map { it.name.replace(Regex("(?<=[a-z])(?=[A-Z])"), " ").lowercase() }
        val nodes =
            buildList {
                add(node("$n.intent", "BEHAVIOR", "INTENT", verbs.joinToString(" · ").replaceFirstChar { it.uppercase() }, "VexWeb.Binding.LocalSpatial", Common + "web/VexLifeWebP1.kt"))
                add(node("$n.seam", "BEHAVIOR", "NAVIGATION", seamLabel, seamRef, Common + "web/VexLifeWebP1.kt"))
                before.forEach { add(node("$n.before.${it.substringAfterLast('.')}", "BEHAVIOR", "NAVIGATION", name(it), it, Common + "web/VexLifeWebP1.kt")) }
                after.forEach { add(node("$n.after.${it.substringAfterLast('.')}", "BEHAVIOR", "NAVIGATION", name(it), it, Common + "web/VexLifeWebP1.kt")) }
            }
        val edges =
            buildList {
                add(edge("$n.intent", "$n.seam"))
                if (VexWeb.Interaction.CollapseBefore in interactions || VexWeb.Interaction.Resize in interactions)
                    before.forEach { add(edge("$n.seam", "$n.before.${it.substringAfterLast('.')}", label = if (VexWeb.Interaction.CollapseBefore in interactions) "resize · collapse before" else "resize")) }
                if (VexWeb.Interaction.CollapseAfter in interactions || VexWeb.Interaction.Resize in interactions)
                    after.forEach { add(edge("$n.seam", "$n.after.${it.substringAfterLast('.')}", label = if (VexWeb.Interaction.CollapseAfter in interactions) "resize · collapse after" else "resize")) }
            }
        return pathway(
            ref = "pathway.$n",
            label = "Resize or collapse ${(before + after).joinToString(" / ") { name(it) }}",
            disposition = "TYPED_PRESENTATION",
            summary =
                "$handleLabel moves or collapses its seam. A presentation-local spatial interaction: it decides nothing and no Family owns it.",
            returns = null,
            returnRef = null,
            nodes = nodes,
            edges = edges,
        )
    }

    private fun interactions(values: Set<VexWeb.Interaction>?): JsonArray? =
        values?.takeIf { it.isNotEmpty() }?.let { set ->
            buildJsonArray {
                VexWeb.Interaction.entries.filter { it in set }.forEach {
                    add(JsonPrimitive(it.name.replace(Regex("(?<=[a-z])(?=[A-Z])"), "_").uppercase()))
                }
            }
        }

    /** `VexLifeWebP1.Refs` property names by structural or seam ref value. */
    private fun structuralSymbols(): Map<String, String> =
        VexLifeWebP1.Refs::class
            .java
            .methods
            .filter { it.parameterCount == 0 && it.name.startsWith("get") }
            .mapNotNull { method ->
                when (val value = method.invoke(VexLifeWebP1.Refs)) {
                    is VexWeb.StructuralRef -> value.value to method.name.removePrefix("get")
                    is VexWeb.SeamRef -> value.value to method.name.removePrefix("get")
                    else -> null
                }
            }
            .toMap()

    private fun capabilityPathway(capabilityRef: String): JsonObject? =
        when (capabilityRef) {
            VexLifeWebP1.Refs.ConversationSendCapability.value -> sendPathway()
            VexLifeWebP1.Refs.VisionSelectedScopeCapability.value -> visionPathway()
            VexLifeWebP1.Refs.ConversationPrivateCapability.value -> privatePathway()
            VexLifeWebP1.Refs.ContextProfileUseCapability.value ->
                contextProfilePathway(
                    "use",
                    "Use saved Context Profile",
                    "VexLifeWebP1.Refs.ContextProfileUseCapability",
                    VexLifeContextProfileFamily.ControlUse,
                    "SelectSaved",
                )
            VexLifeWebP1.Refs.ContextProfileSaveCapability.value ->
                contextProfilePathway(
                    "save",
                    "Save Context Profile",
                    "VexLifeWebP1.Refs.ContextProfileSaveCapability",
                    VexLifeContextProfileFamily.ControlSave,
                    "Persist",
                )
            VexLifeWebP1.Refs.ContextProfileSaveAsCapability.value ->
                contextProfilePathway(
                    "save-as",
                    "Save as a new Context Profile",
                    "VexLifeWebP1.Refs.ContextProfileSaveAsCapability",
                    VexLifeContextProfileFamily.ControlSaveAs,
                    "PersistAsNew",
                )
            VexLifeWebP1.Refs.ContextProfileRestoreCapability.value ->
                contextProfilePathway(
                    "restore-product-default",
                    "Restore VexLife default Context Profile",
                    "VexLifeWebP1.Refs.ContextProfileRestoreCapability",
                    VexLifeContextProfileFamily.ControlRestoreDefault,
                    "SelectProductDefault",
                )
            VexLifeWebP1.Refs.ContextProfileImportCapability.value ->
                contextProfilePathway(
                    "import",
                    "Import a Context Profile",
                    "VexLifeWebP1.Refs.ContextProfileImportCapability",
                    VexLifeContextProfileFamily.ControlImport,
                    "ImportAsNew",
                )
            VexLifeWebP1.Refs.ReplyLanguageCapability.value ->
                languagePathway("reply-language", "Choose what Vex replies in", listOf(VexLifeLanguageFamily.ControlChooseReplyLanguage))
            VexLifeWebP1.Refs.ScreenLanguageCapability.value ->
                languagePathway(
                    "screen-language",
                    "Choose what VexLife appears in",
                    listOf(VexLifeLanguageFamily.ControlChooseScreenLanguage, VexLifeLanguageFamily.ControlFollowReplyLanguage),
                )
            else -> null
        }

    /** One Context Profile surface Control reaches exactly one Control in its existing Family. */
    private fun contextProfilePathway(
        choice: String,
        label: String,
        capabilitySymbol: String,
        controlRef: String,
        effectLabel: String,
    ): JsonObject {
        val n = "vexlife-prod.context-profile.$choice"
        return pathway(
            ref = "pathway.vexlife-prod.context-profile.$choice",
            label = label,
            disposition = "SCAE_FAMILY",
            summary =
                "VexLifeContextProfileFamily decides which configuration constructs the next Vex turn; " +
                    "context content keeps its existing owners.",
            returns = "the re-observed Context Profile DisplayState",
            returnRef = "$n.projection",
            nodes =
                listOf(
                    node(
                        "$n.intent",
                        "BEHAVIOR",
                        "INTENT",
                        "context-profile.$choice",
                        capabilitySymbol,
                        Common + "web/VexLifeWebP1.kt",
                    ),
                    node(
                        "$n.dispatch",
                        "BEHAVIOR",
                        "DISPATCH",
                        "POST ${VexLifeProdServiceContract.ContextProfilesControlPath}",
                        "VexLifeProdLocalService",
                        Jvm + "service/VexLifeProdLocalService.kt",
                    ),
                    node(
                        "$n.control",
                        "BEHAVIOR",
                        "CONTROL",
                        controlRef,
                        "VexLifeContextProfileFamily.Control",
                        Common + "context/VexLifeContextProfileFamily.kt",
                    ),
                    node(
                        "$n.state",
                        "BEHAVIOR",
                        "STATE",
                        "${VexLifeContextProfileFamily.StateProductDefault} · ${VexLifeContextProfileFamily.StateSaved}",
                        "VexLifeContextProfileFamily.State",
                        Common + "context/VexLifeContextProfileFamily.kt",
                    ),
                    node(
                        "$n.act",
                        "BEHAVIOR",
                        "ACT",
                        "VexLifeContextProfileFamily.act",
                        "VexLifeContextProfileFamily",
                        Common + "context/VexLifeContextProfileFamily.kt",
                    ),
                    node(
                        "$n.effect",
                        "BEHAVIOR",
                        "EFFECT",
                        effectLabel,
                        "VexLifeContextProfileFamily.Effect",
                        Common + "context/VexLifeContextProfileFamily.kt",
                    ),
                    node(
                        "$n.store",
                        "DEPTH",
                        "STORE",
                        "Context Profile revisions in the Vex Home",
                        "VexLifeContextProfileStore",
                        Jvm + "context/VexLifeContextProfileStore.kt",
                    ),
                    node(
                        "$n.observation",
                        "DEPTH",
                        "OBSERVATION",
                        "stateFor()",
                        "VexLifeContextProfileFamily.stateFor",
                        Common + "context/VexLifeContextProfileFamily.kt",
                    ),
                    node(
                        "$n.projection",
                        "BEHAVIOR",
                        "PROJECTION",
                        "Context Profile",
                        "VexLifeContextProfileFamily.DisplayState",
                        Common + "context/VexLifeContextProfileFamily.kt",
                    ),
                ),
            edges =
                listOf(
                    edge("$n.intent", "$n.dispatch"),
                    edge("$n.dispatch", "$n.control"),
                    edge("$n.state", "$n.act", "DATA", "current State"),
                    edge("$n.control", "$n.act"),
                    edge("$n.act", "$n.effect"),
                    edge("$n.effect", "$n.store"),
                    edge("$n.store", "$n.observation", "RETURN", "re-read effective profile"),
                    edge("$n.observation", "$n.projection"),
                ),
        )
    }

    /** Both language choices share one Family; each element's pathway names only its own Controls. */
    private fun languagePathway(choice: String, label: String, controls: List<String>): JsonObject {
        val n = "vexlife-prod.$choice"
        return pathway(
            ref = "pathway.vexlife-prod.person.$choice",
            label = label,
            disposition = "SCAE_FAMILY",
            summary =
                "VexLifeLanguageFamily decides; a Control names one language choice and the other is never moved.",
            returns = "the re-observed language choices",
            returnRef = "$n.projection",
            nodes =
                listOf(
                    node(
                        "$n.intent",
                        "BEHAVIOR",
                        "INTENT",
                        "person.$choice",
                        if (choice == "reply-language") {
                            "VexLifeWebP1.Refs.ReplyLanguageCapability"
                        } else {
                            "VexLifeWebP1.Refs.ScreenLanguageCapability"
                        },
                        Common + "web/VexLifeWebP1.kt",
                    ),
                    node(
                        "$n.dispatch",
                        "BEHAVIOR",
                        "DISPATCH",
                        "POST ${VexLifeProdServiceContract.SettingsPath}",
                        "VexLifeProdLocalService",
                        Jvm + "service/VexLifeProdLocalService.kt",
                    ),
                    node(
                        "$n.control",
                        "BEHAVIOR",
                        "CONTROL",
                        controls.joinToString(" · "),
                        "VexLifeLanguageFamily.Control",
                        Common + "person/VexLifeLanguageFamily.kt",
                    ),
                    node(
                        "$n.state",
                        "BEHAVIOR",
                        "STATE",
                        "${VexLifeLanguageFamily.StateFollowingReplies} · ${VexLifeLanguageFamily.StateScreenChosen}",
                        "VexLifeLanguageFamily.State",
                        Common + "person/VexLifeLanguageFamily.kt",
                    ),
                    node("$n.act", "BEHAVIOR", "ACT", "VexLifeLanguageFamily.act", "VexLifeLanguageFamily", Common + "person/VexLifeLanguageFamily.kt"),
                    node("$n.effect", "BEHAVIOR", "EFFECT", "SaveChoices", "VexLifeLanguageFamily.Effect", Common + "person/VexLifeLanguageFamily.kt"),
                    node(
                        "$n.store",
                        "DEPTH",
                        "STORE",
                        "Person settings in the Vex Home",
                        "VexLifePersonSettingsStore",
                        Jvm + "person/VexLifePersonSettingsStore.kt",
                        "runtime/vexlife-prod/settings/person.json; both choices, one owner.",
                    ),
                    node(
                        "$n.projection",
                        "BEHAVIOR",
                        "PROJECTION",
                        "Language choices",
                        "VexLifeLanguageFamily.DisplayState",
                        Common + "person/VexLifeLanguageFamily.kt",
                    ),
                ),
            edges =
                listOf(
                    edge("$n.intent", "$n.dispatch"),
                    edge("$n.dispatch", "$n.control"),
                    edge("$n.state", "$n.act", "DATA", "current State"),
                    edge("$n.control", "$n.act"),
                    edge("$n.act", "$n.effect"),
                    edge("$n.effect", "$n.store"),
                    edge("$n.store", "$n.projection", "RETURN"),
                ),
        )
    }

    private fun sendPathway(): JsonObject {
        val p = "pathway.vexlife-prod.conversation.send"
        val n = "vexlife-prod.send"
        return pathway(
            ref = p,
            label = "Send to Vex",
            disposition = "DIRECT_REGISTRATION",
            summary =
                "The service route calls the turn owner directly; no Conversation Family has earned this meaning yet.",
            returns = "the canonical Conversation projection",
            returnRef = "$n.projection",
            nodes =
                listOf(
                    node(
                        "$n.intent",
                        "BEHAVIOR",
                        "INTENT",
                        "conversation.send",
                        "VexLifeWebP1.Refs.ConversationSendCapability",
                        Common + "web/VexLifeWebP1.kt",
                    ),
                    node(
                        "$n.dispatch",
                        "BEHAVIOR",
                        "DISPATCH",
                        "POST ${VexLifeProdServiceContract.ConversationSendPath}",
                        "VexLifeProdLocalService",
                        Jvm + "service/VexLifeProdLocalService.kt",
                    ),
                    node(
                        "$n.turn",
                        "DEPTH",
                        "METHOD",
                        "DirectCompanionTurn",
                        "DirectCompanionTurn",
                        Jvm + "companion/DirectCompanionTurn.kt",
                        "One canonical turn. The request is appended to Conversation before inference.",
                    ),
                    node(
                        "$n.prompt",
                        "DEPTH",
                        "REALIZATION",
                        "Prompt materialization",
                        "DirectPromptContext",
                        Jvm + "companion/DirectPromptContext.kt",
                        "Selects recent completed turns and current Memory within the token budget and records exactly what Vex received.",
                    ),
                    node(
                        "$n.field",
                        "DEPTH",
                        "STORE",
                        "Foundational field",
                        "VexLifeFoundationalField",
                        Jvm + "foundational/VexLifeFoundationalField.kt",
                        "The versioned body the same kernel is selected from on every turn. Readable at GET ${VexLifeProdServiceContract.FoundationalFieldPath}.",
                    ),
                    node(
                        "$n.memory",
                        "DEPTH",
                        "STORE",
                        "Current Memory",
                        "VexLifeCurrentMemoryReader",
                        Jvm + "memory/VexLifeCurrentMemoryReader.kt",
                        "Included only when the Living Journal projection is CURRENT.",
                    ),
                    node(
                        "$n.runtime",
                        "DEPTH",
                        "CONNECTION",
                        "Local Heretic runtime",
                        "HereticPersistentRuntime",
                        Jvm + "model/heretic/runtime/HereticPersistentRuntime.kt",
                    ),
                    node(
                        "$n.conversation",
                        "DEPTH",
                        "STORE",
                        "Canonical Conversation",
                        "DirectConversationStore",
                        Jvm + "conversation/DirectConversationStore.kt",
                    ),
                    node(
                        "$n.projection",
                        "BEHAVIOR",
                        "PROJECTION",
                        "Conversation projection",
                        "VexLifeProdServiceContract.ConversationProjection",
                        Common + "service/VexLifeProdServiceContract.kt",
                    ),
                ),
            edges =
                listOf(
                    edge("$n.intent", "$n.dispatch"),
                    edge("$n.dispatch", "$n.turn"),
                    edge("$n.turn", "$n.prompt"),
                    edge("$n.field", "$n.prompt", "DATA", "foundational field"),
                    edge("$n.memory", "$n.prompt", "DATA", "current Memory"),
                    edge("$n.prompt", "$n.runtime"),
                    edge("$n.runtime", "$n.conversation", label = "response committed"),
                    edge("$n.conversation", "$n.projection", "RETURN"),
                ),
        )
    }

    private fun visionPathway(): JsonObject {
        val p = "pathway.vexlife-prod.vision.selected-scope"
        val n = "vexlife-prod.vision"
        val controls =
            listOf(VexLifeVisionFamily.Engage, VexLifeVisionFamily.Stop).joinToString(" · ") {
                it.controlRef
            }
        val states =
            listOf(
                    VexLifeVisionFamily.StateHeld,
                    VexLifeVisionFamily.StateReady,
                    VexLifeVisionFamily.StateSelecting,
                    VexLifeVisionFamily.StateEngaged,
                    VexLifeVisionFamily.StateLooking,
                )
                .joinToString(" · ")
        return pathway(
            ref = p,
            label = "Choose or release a window",
            disposition = "SCAE_FAMILY",
            summary =
                "VexLifeVisionFamily decides against a fresh observation; an unlawful Control never reaches the runtime.",
            returns = "a re-observed Vision DisplayState",
            returnRef = "$n.projection",
            nodes =
                listOf(
                    node(
                        "$n.intent",
                        "BEHAVIOR",
                        "INTENT",
                        "vision.selected-scope",
                        "VexLifeWebP1.Refs.VisionSelectedScopeCapability",
                        Common + "web/VexLifeWebP1.kt",
                    ),
                    node(
                        "$n.dispatch",
                        "BEHAVIOR",
                        "DISPATCH",
                        "POST ${VexLifeProdServiceContract.VisionControlPath}",
                        "VexLifeProdLocalService",
                        Jvm + "service/VexLifeProdLocalService.kt",
                    ),
                    node(
                        "$n.control",
                        "BEHAVIOR",
                        "CONTROL",
                        controls,
                        "VexLifeVisionFamily.Control",
                        Common + "vision/VexLifeVisionFamily.kt",
                    ),
                    node(
                        "$n.state",
                        "BEHAVIOR",
                        "STATE",
                        states,
                        "VexLifeVisionFamily.State",
                        Common + "vision/VexLifeVisionFamily.kt",
                        "Re-observed from PROJECT before the Act.",
                    ),
                    node(
                        "$n.act",
                        "BEHAVIOR",
                        "ACT",
                        "VexLifeVisionFamily.act",
                        "VexLifeVisionFamily",
                        Common + "vision/VexLifeVisionFamily.kt",
                        "No lawful Act: 409 NOT_LAWFUL_NOW.",
                    ),
                    node(
                        "$n.effect",
                        "BEHAVIOR",
                        "EFFECT",
                        "EngageScope · StopScope",
                        "VexLifeVisionFamily.Effect",
                        Common + "vision/VexLifeVisionFamily.kt",
                    ),
                    node(
                        "$n.runtime",
                        "DEPTH",
                        "CONNECTION",
                        "Vision runtime child",
                        "VexLifeProdVisionRuntime",
                        Jvm + "vision/VexLifeProdVisionRuntime.kt",
                    ),
                    node(
                        "$n.picker",
                        "DEPTH",
                        "PARTNER",
                        "macOS window picker",
                        "SDK selected-scope session",
                        null,
                        "The person chooses one window. Nothing is captured.",
                    ),
                    node(
                        "$n.observation",
                        "DEPTH",
                        "OBSERVATION",
                        "Selected-scope projection",
                        "VexLifeProdServiceContract.VisionSelectedScopeProjection",
                        Common + "service/VexLifeProdServiceContract.kt",
                        "Content-absent: no pixels, no frames.",
                    ),
                    node(
                        "$n.projection",
                        "BEHAVIOR",
                        "PROJECTION",
                        "Vision DisplayState",
                        "VexLifeVisionFamily.DisplayState",
                        Common + "vision/VexLifeVisionFamily.kt",
                    ),
                ),
            edges =
                listOf(
                    edge("$n.intent", "$n.dispatch"),
                    edge("$n.dispatch", "$n.control"),
                    edge("$n.state", "$n.act", "DATA", "current State"),
                    edge("$n.control", "$n.act"),
                    edge("$n.act", "$n.effect"),
                    edge("$n.effect", "$n.runtime"),
                    edge("$n.runtime", "$n.picker"),
                    edge("$n.picker", "$n.observation"),
                    edge("$n.observation", "$n.projection", "RETURN", "re-observed"),
                ),
        )
    }

    private fun privatePathway(): JsonObject {
        val n = "vexlife-prod.private"
        return pathway(
            ref = "pathway.vexlife-prod.conversation.private",
            label = "Go private or leave private mode",
            disposition = "SCAE_FAMILY",
            summary =
                "VexLifePrivateModeFamily decides; a private conversation is never written, only its interval and the notes both agreed to keep.",
            returns = "a re-observed Private mode DisplayState",
            returnRef = "$n.projection",
            nodes =
                listOf(
                    node(
                        "$n.intent",
                        "BEHAVIOR",
                        "INTENT",
                        "conversation.private",
                        "VexLifeWebP1.Refs.ConversationPrivateCapability",
                        Common + "web/VexLifeWebP1.kt",
                    ),
                    node(
                        "$n.dispatch",
                        "BEHAVIOR",
                        "DISPATCH",
                        "POST ${VexLifeProdServiceContract.PrivateControlPath}",
                        "VexLifeProdLocalService",
                        Jvm + "service/VexLifeProdLocalService.kt",
                    ),
                    node(
                        "$n.control",
                        "BEHAVIOR",
                        "CONTROL",
                        "${VexLifePrivateModeFamily.ControlBegin} · ${VexLifePrivateModeFamily.ControlEnd}",
                        "VexLifePrivateModeFamily.Control",
                        Common + "continuity/VexLifePrivateModeFamily.kt",
                    ),
                    node(
                        "$n.state",
                        "BEHAVIOR",
                        "STATE",
                        "${VexLifePrivateModeFamily.StateKept} · ${VexLifePrivateModeFamily.StatePrivate}",
                        "VexLifePrivateModeFamily.State",
                        Common + "continuity/VexLifePrivateModeFamily.kt",
                    ),
                    node(
                        "$n.act",
                        "BEHAVIOR",
                        "ACT",
                        "VexLifePrivateModeFamily.act",
                        "VexLifePrivateModeFamily",
                        Common + "continuity/VexLifePrivateModeFamily.kt",
                    ),
                    node(
                        "$n.effect",
                        "BEHAVIOR",
                        "EFFECT",
                        "BeginPrivate · EndPrivate",
                        "VexLifePrivateModeFamily.Effect",
                        Common + "continuity/VexLifePrivateModeFamily.kt",
                    ),
                    node(
                        "$n.session",
                        "DEPTH",
                        "EXECUTION",
                        "In-memory private conversation",
                        "VexLifePrivateConversation",
                        Jvm + "continuity/VexLifePrivateConversation.kt",
                        "Held only in the running service; never written.",
                    ),
                    node(
                        "$n.interval",
                        "DEPTH",
                        "STORE",
                        "Private interval record",
                        "VexLifePrivateConversation.IntervalRecord",
                        Jvm + "continuity/VexLifePrivateConversation.kt",
                        "When, how many exchanges, and the kept notes.",
                    ),
                    node(
                        "$n.projection",
                        "BEHAVIOR",
                        "PROJECTION",
                        "Private mode DisplayState",
                        "VexLifePrivateModeFamily.DisplayState",
                        Common + "continuity/VexLifePrivateModeFamily.kt",
                    ),
                ),
            edges =
                listOf(
                    edge("$n.intent", "$n.dispatch"),
                    edge("$n.dispatch", "$n.control"),
                    edge("$n.state", "$n.act", "DATA", "current State"),
                    edge("$n.control", "$n.act"),
                    edge("$n.act", "$n.effect"),
                    edge("$n.effect", "$n.session"),
                    edge("$n.session", "$n.interval", label = "on leave"),
                    edge("$n.interval", "$n.projection", "RETURN", "re-observed"),
                ),
        )
    }

    private fun localPathway(element: VexUiElementRef, targetRef: String): JsonObject {
        val suffix = element.ref.removePrefix("element.vexlife-workspace.")
        val n = "vexlife-prod.local.$suffix"
        val target = VexLifeWorkspaceBlueprint.elements.singleOrNull { it.ref == targetRef }
        val targetLabel = target?.let(::humanLabel) ?: targetRef
        return pathway(
            ref = "pathway.vexlife-prod.local.$suffix",
            label = "Presentation over $targetLabel",
            disposition = "TYPED_PRESENTATION",
            summary =
                "A presentation-local interaction over another coordinate; it decides nothing.",
            returns = null,
            returnRef = null,
            nodes =
                listOf(
                    node(
                        "$n.intent",
                        "BEHAVIOR",
                        "INTENT",
                        "Activate",
                        "VexWeb.Binding.LocalInteraction",
                        Common + "web/VexLifeWebP1.kt",
                    ),
                    node(
                        "$n.reveal",
                        "BEHAVIOR",
                        "NAVIGATION",
                        targetLabel,
                        targetRef,
                        Common + "workspace/VexLifeWorkspaceBlueprint.kt",
                    ),
                ),
            edges = listOf(edge("$n.intent", "$n.reveal")),
        )
    }

    private fun pathway(
        ref: String,
        label: String,
        disposition: String,
        summary: String,
        returns: String?,
        returnRef: String?,
        nodes: List<JsonObject>,
        edges: List<JsonObject>,
    ): JsonObject = buildJsonObject {
        put("pathwayRef", ref)
        put("label", label)
        put("disposition", disposition)
        put("summary", summary)
        put("returns", returns)
        put("returnRef", returnRef)
        put("nodes", JsonArray(nodes))
        put("edges", JsonArray(edges))
    }

    private fun node(
        ref: String,
        face: String,
        role: String,
        label: String,
        symbol: String?,
        source: String?,
        summary: String? = null,
    ): JsonObject = buildJsonObject {
        put("ref", ref)
        put("face", face)
        put("role", role)
        put("label", label)
        put("symbol", symbol)
        put("summary", summary)
        put("source", source)
    }

    private fun edge(
        from: String,
        to: String,
        kind: String = "FLOW",
        label: String? = null,
    ): JsonObject = buildJsonObject {
        put("from", from)
        put("to", to)
        put("label", label)
        put("kind", kind)
    }

    private fun lensKind(
        element: VexUiElementRef,
        coordinate: VexWeb.Coordinate?,
    ): String =
        if (
            coordinate?.placement == VexWeb.Placement.Overlay &&
            coordinate.depth == VexWeb.Depth.Modal
        ) {
            "DIALOG"
        } else {
            when (element.kind) {
                VexUiElementRef.Kind.Region -> "REGION"
                VexUiElementRef.Kind.Content -> "TEXT"
                VexUiElementRef.Kind.TextField -> "FIELD"
                VexUiElementRef.Kind.Control -> "BUTTON"
                VexUiElementRef.Kind.Collection -> "STACK"
            }
        }

    private fun humanLabel(element: VexUiElementRef): String =
        element.ref.substringAfterLast('.').replace('-', ' ').replaceFirstChar { it.uppercase() }

    private fun declaration(element: VexUiElementRef, symbols: Map<String, String>): String {
        val builder =
            when (element.kind) {
                VexUiElementRef.Kind.Region -> "region"
                VexUiElementRef.Kind.Content -> "content"
                VexUiElementRef.Kind.TextField -> "textField"
                VexUiElementRef.Kind.Control -> "control"
                VexUiElementRef.Kind.Collection -> "collection"
            }
        val id = element.ref.substringAfterLast('.')
        val parent = element.parentRef?.let { symbols[it] }
        return if (parent == null) "$builder(\"$id\")" else "$parent.$builder(\"$id\")"
    }

    /** Blueprint property names by element ref, read from the object's own getters. */
    private fun blueprintSymbols(): Map<String, String> =
        VexLifeWorkspaceBlueprint::class
            .java
            .methods
            .filter {
                it.parameterCount == 0 &&
                    it.returnType == VexUiElementRef::class.java &&
                    it.name.startsWith("get")
            }
            .associate {
                (it.invoke(VexLifeWorkspaceBlueprint) as VexUiElementRef).ref to
                    it.name.removePrefix("get")
            }
}

/**
 * `write <repo root>` regenerates the committed Lens; `verify <repo root>` fails when it is stale.
 */
fun main(args: Array<String>) {
    require(args.size == 2 && args[0] in setOf("write", "verify")) {
        "usage: <write|verify> <repository root>"
    }
    val path = Path.of(args[1]).resolve(VexLifeProdScreenLens.CommittedPath)
    val text = VexLifeProdScreenLens.exportText()
    if (args[0] == "write") {
        Files.createDirectories(path.parent)
        Files.writeString(path, text)
    } else {
        check(Files.exists(path) && Files.readString(path) == text) {
            "${VexLifeProdScreenLens.CommittedPath} is stale; run generateVexLifeProdScreenLens"
        }
    }
}

// [VXG RealForever]
