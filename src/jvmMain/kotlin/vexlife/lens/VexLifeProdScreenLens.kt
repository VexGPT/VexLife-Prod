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
import vexlife.service.VexLifeProdServiceContract
import vexlife.vision.VexLifeVisionFamily
import vexlife.web.VexLifeWebP1
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

        val elements = buildJsonArray {
            for (element in blueprint.elements) {
                val coordinate = coordinates[element.ref]
                add(
                    buildJsonObject {
                        put("ref", element.ref)
                        put("kind", lensKind(element.kind))
                        put("label", humanLabel(element))
                        put("parentRef", element.parentRef)
                        put("symbol", symbols[element.ref]?.let { "VexLifeWorkspaceBlueprint.$it" })
                        put("declared", declaration(element, symbols))
                        put(
                            "intent",
                            when (val binding = coordinate?.binding) {
                                is VexWeb.Binding.Capability ->
                                    "capability ${binding.capabilityRef.value}"
                                is VexWeb.Binding.LocalInteraction -> "reveal ${binding.target.ref}"
                                else -> null
                            },
                        )
                        put("pathwayRef", elementPathway[element.ref])
                        put("style", coordinate?.presentation?.name)
                        put("slot", coordinate?.placement?.name)
                    }
                )
            }
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
                                "Shows VexLifeMemoryFamily's DisplayState from GET ${VexLifeProdServiceContract.MemoryPath}. No Controls until Memory formation has an owner.",
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

    private fun capabilityPathway(capabilityRef: String): JsonObject? =
        when (capabilityRef) {
            VexLifeWebP1.Refs.ConversationSendCapability.value -> sendPathway()
            VexLifeWebP1.Refs.VisionSelectedScopeCapability.value -> visionPathway()
            else -> null
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

    private fun localPathway(element: VexUiElementRef, targetRef: String): JsonObject {
        val suffix = element.ref.removePrefix("element.vexlife-workspace.")
        val n = "vexlife-prod.local.$suffix"
        val target = VexLifeWorkspaceBlueprint.elements.singleOrNull { it.ref == targetRef }
        val targetLabel = target?.let(::humanLabel) ?: targetRef
        return pathway(
            ref = "pathway.vexlife-prod.local.$suffix",
            label = "Reveal $targetLabel",
            disposition = "TYPED_PRESENTATION",
            summary =
                "A presentation-local interaction; it reveals another coordinate and decides nothing.",
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

    private fun lensKind(kind: VexUiElementRef.Kind): String =
        when (kind) {
            VexUiElementRef.Kind.Region -> "REGION"
            VexUiElementRef.Kind.Content -> "TEXT"
            VexUiElementRef.Kind.TextField -> "FIELD"
            VexUiElementRef.Kind.Control -> "BUTTON"
            VexUiElementRef.Kind.Collection -> "STACK"
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
