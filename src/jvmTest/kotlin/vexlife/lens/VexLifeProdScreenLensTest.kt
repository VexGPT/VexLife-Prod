package vexlife.lens

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import vexlife.context.VexLifeContextProfileFamily
import vexlife.person.VexLifeLanguageFamily
import vexlife.web.VexLifeWebP1
import vexlife.workspace.VexLifeWorkspaceApp
import vexlife.workspace.VexLifeWorkspaceBlueprint

class VexLifeProdScreenLensTest {
    @Test
    fun committedLensIsCurrent() {
        val committed = Path.of(VexLifeProdScreenLens.CommittedPath)
        assertTrue(Files.exists(committed), "run ./gradlew generateVexLifeProdScreenLens")
        assertEquals(
            VexLifeProdScreenLens.exportText(),
            Files.readString(committed),
            "${VexLifeProdScreenLens.CommittedPath} is stale; run ./gradlew generateVexLifeProdScreenLens",
        )
    }

    @Test
    fun everyBlueprintElementAppearsOnceAndEveryBoundPathwayExists() {
        val lens = VexLifeProdScreenLens.export()
        val screen = lens["screens"]!!.jsonArray.single().jsonObject
        val elements = screen["elements"]!!.jsonArray.map { it.jsonObject }
        val blueprintRefs = VexLifeWorkspaceBlueprint.elements.map { it.ref }.toSet()
        assertEquals(
            blueprintRefs,
            elements.map { it.string("ref") }.filter { it in blueprintRefs }.toSet(),
        )
        assertEquals(elements.size, elements.map { it.string("ref") }.toSet().size, "every element appears once")

        val pathways =
            lens["pathways"]!!
                .jsonArray
                .map { it.jsonObject }
                .associateBy { it.string("pathwayRef") }
        elements
            .mapNotNull { it["pathwayRef"]?.jsonPrimitive?.takeIf { p -> p.isString }?.content }
            .forEach {
                assertTrue(it in pathways, "element names undefined pathway $it")
            }
        assertEquals(
            "SCAE_FAMILY",
            pathways.getValue("pathway.vexlife-prod.vision.selected-scope").string("disposition"),
        )
        val send = elements.single { it.string("ref") == VexLifeWorkspaceBlueprint.Send.ref }
        assertEquals("pathway.vexlife-prod.conversation.send", send.string("pathwayRef"))
        assertEquals("VexLifeWorkspaceBlueprint.Send", send.string("symbol"))
        val sendPath = pathways.getValue("pathway.vexlife-prod.conversation.send")
        assertEquals("SCAE_FAMILY", sendPath.string("disposition"))
        assertTrue(
            sendPath.toString().contains("VexLifeConversationTurnFamily") &&
                sendPath.toString().contains("BeginTurn(attempt)") &&
                sendPath.toString().contains("active-turn truth"),
        )

        val stop = elements.single { it.string("ref") == VexLifeWorkspaceBlueprint.Stop.ref }
        assertEquals("capability conversation.cancel", stop.string("intent"))
        assertEquals("pathway.vexlife-prod.conversation.cancel", stop.string("pathwayRef"))
        assertEquals("VexLifeWorkspaceBlueprint.Stop", stop.string("symbol"))
        val cancel = pathways.getValue("pathway.vexlife-prod.conversation.cancel")
        assertEquals("SCAE_FAMILY", cancel.string("disposition"))
        assertTrue(
            cancel.toString().contains("VexLifeConversationTurnFamily") &&
                cancel.toString().contains("RequestCancel(attempt)") &&
                cancel.toString().contains("DirectCompanionCancellation") &&
                cancel.toString().contains("TURN_CANCELLED_BY_PERSON"),
        )

        assertTrue((lens["chrome"] as JsonArray).isEmpty())
    }

    @Test
    fun theWorkspaceIsExportedAsVexWebRealizesIt() {
        val lens = VexLifeProdScreenLens.export()
        val screen = lens["screens"]!!.jsonArray.single().jsonObject
        val elements = screen["elements"]!!.jsonArray.map { it.jsonObject }.associateBy { it.string("ref") }
        val layout = screen["layout"]!!.jsonObject
        assertEquals("FRAME", layout.string("kind"))
        assertEquals(VexLifeWebP1.Refs.WorkspaceFrame.value, layout.string("root"))
        assertEquals(listOf(VexLifeWebP1.Refs.OverlayRoot.value), layout["overlays"]!!.jsonArray.map { it.jsonPrimitive.content })

        // Every VexWeb structural ref is a Lens element, named by its Refs constant.
        val structural =
            listOf(
                VexLifeWebP1.Refs.WorkspaceFrame to "FRAME",
                VexLifeWebP1.Refs.HeaderRegion to "REGION",
                VexLifeWebP1.Refs.BodyRegion to "REGION",
                VexLifeWebP1.Refs.BodyFrame to "FRAME",
                VexLifeWebP1.Refs.BrowserRegion to "REGION",
                VexLifeWebP1.Refs.CompanionRegion to "REGION",
                VexLifeWebP1.Refs.BrowserCompanionHandle to "HANDLE",
                VexLifeWebP1.Refs.CompanionHeader to "REGION",
                VexLifeWebP1.Refs.CompanionContent to "REGION",
                VexLifeWebP1.Refs.OverlayRoot to "REGION",
                VexLifeWebP1.Refs.Snackbar to "REGION",
            )
        for ((ref, kind) in structural) assertEquals(kind, elements.getValue(ref.value).string("kind"), ref.value)
        for (seam in listOf(VexLifeWebP1.Refs.HeaderBodySeam, VexLifeWebP1.Refs.BrowserCompanionSeam))
            assertEquals("SEAM", elements.getValue(seam.value).string("kind"))
        assertEquals("VexLifeWebP1.Refs.BrowserCompanionSeam", elements.getValue(VexLifeWebP1.Refs.BrowserCompanionSeam.value).string("symbol"))

        // Each frame's seams and regions are exported elements inside that frame; region bounds name
        // only the frame's own edges or seams; allocations are the authored ratios.
        val frames = layout["frames"]!!.jsonArray.map { it.jsonObject }.associateBy { it.string("ref") }
        assertEquals(setOf(VexLifeWebP1.Refs.WorkspaceFrame.value, VexLifeWebP1.Refs.BodyFrame.value), frames.keys)
        for ((frameRef, frame) in frames) {
            val seams = frame["seams"]!!.jsonArray.map { it.jsonObject }
            val seamRefs = seams.map { it.string("ref") }.toSet()
            seamRefs.forEach { assertEquals(frameRef, elements.getValue(it).string("parentRef")) }
            for (region in frame["regions"]!!.jsonArray.map { it.jsonObject }) {
                assertEquals(frameRef, elements.getValue(region.string("ref")).string("parentRef"))
                for (edge in listOf("start", "end", "top", "bottom")) {
                    val boundary = region.string(edge)
                    assertTrue(boundary.startsWith("FRAME_") || boundary in seamRefs, "$frameRef ${region.string("ref")} $edge = $boundary")
                }
            }
        }
        val headerSeam = frames.getValue(VexLifeWebP1.Refs.WorkspaceFrame.value)["seams"]!!.jsonArray.single().jsonObject
        assertEquals("HORIZONTAL", headerSeam.string("axis"))
        assertEquals(listOf(1, 11), listOf("before", "after").map { headerSeam["allocation"]!!.jsonObject.string(it).toInt() })
        val bodySeam = frames.getValue(VexLifeWebP1.Refs.BodyFrame.value)["seams"]!!.jsonArray.single().jsonObject
        assertEquals("VERTICAL", bodySeam.string("axis"))
        assertEquals(listOf(1, 1), listOf("before", "after").map { bodySeam["allocation"]!!.jsonObject.string(it).toInt() })

        // Elements sit in the VexWeb container that realizes them.
        fun path(ref: String): List<String> =
            generateSequence(ref) { elements.getValue(it)["parentRef"]?.jsonPrimitive?.takeIf { p -> p.isString }?.content }.toList().reversed()
        assertEquals(
            listOf(
                VexLifeWebP1.Refs.WorkspaceFrame.value,
                VexLifeWebP1.Refs.BodyRegion.value,
                VexLifeWebP1.Refs.BodyFrame.value,
                VexLifeWebP1.Refs.CompanionRegion.value,
                VexLifeWorkspaceBlueprint.Companion.ref,
                VexLifeWebP1.Refs.CompanionContent.value,
                VexLifeWorkspaceBlueprint.Conversation.ref,
                // VexWeb realizes Send inside the composer it acts on.
                VexLifeWorkspaceBlueprint.Composer.ref,
                VexLifeWorkspaceBlueprint.Send.ref,
            ),
            path(VexLifeWorkspaceBlueprint.Send.ref),
        )
        assertEquals(VexLifeWebP1.Refs.CompanionHeader.value, elements.getValue(VexLifeWorkspaceBlueprint.PrivateMode.ref).string("parentRef"))
        assertEquals("CHROME", elements.getValue(VexLifeWorkspaceBlueprint.Header.ref).string("layer"))
        assertEquals("TRANSIENT", elements.getValue(VexLifeWebP1.Refs.Snackbar.value).string("layer"))

        // The seam handle's interactions are presentation-local: a typed presentation pathway over
        // the seam and the regions it bounds, never a Family.
        val handle = elements.getValue(VexLifeWebP1.Refs.BrowserCompanionHandle.value)
        assertEquals(listOf("RESIZE", "COLLAPSE_BEFORE", "COLLAPSE_AFTER"), handle["interactions"]!!.jsonArray.map { it.jsonPrimitive.content })
        val pathway = lens["pathways"]!!.jsonArray.map { it.jsonObject }.single { it.string("pathwayRef") == handle.string("pathwayRef") }
        assertEquals("TYPED_PRESENTATION", pathway.string("disposition"))
        val symbols = pathway["nodes"]!!.jsonArray.map { it.jsonObject.string("symbol") }
        assertTrue(VexLifeWebP1.Refs.BrowserRegion.value in symbols && VexLifeWebP1.Refs.CompanionRegion.value in symbols, symbols.toString())
        assertTrue(lens["pathways"]!!.jsonArray.none { it.jsonObject.string("disposition") == "SCAE_FAMILY" && it.jsonObject.string("pathwayRef").contains("spatial") })

        // The semantic pathways are unchanged.
        assertEquals("pathway.vexlife-prod.conversation.private", elements.getValue(VexLifeWorkspaceBlueprint.PrivateMode.ref).string("pathwayRef"))
        assertEquals("pathway.vexlife-prod.vision.selected-scope", elements.getValue(VexLifeWorkspaceBlueprint.Vision.ref).string("pathwayRef"))
    }

    @Test
    fun languageIdentityAndSpatialRealizationCompose() {
        val lens = VexLifeProdScreenLens.export()
        val screen = lens["screens"]!!.jsonArray.single().jsonObject
        assertEquals("FRAME", screen["layout"]!!.jsonObject.string("kind"))

        val pathways =
            lens["pathways"]!!
                .jsonArray
                .map { it.jsonObject }
                .associateBy { it.string("pathwayRef") }

        fun controlLabel(pathwayRef: String): String =
            pathways
                .getValue(pathwayRef)["nodes"]!!
                .jsonArray
                .map { it.jsonObject }
                .single { it.string("role") == "CONTROL" }
                .string("label")

        assertEquals(
            VexLifeLanguageFamily.ControlChooseReplyLanguage,
            controlLabel("pathway.vexlife-prod.person.reply-language"),
        )
        assertEquals(
            listOf(
                VexLifeLanguageFamily.ControlChooseScreenLanguage,
                VexLifeLanguageFamily.ControlFollowReplyLanguage,
            ).joinToString(" · "),
            controlLabel("pathway.vexlife-prod.person.screen-language"),
        )
        assertTrue(
            VexLifeWebP1.Refs.BrowserCompanionHandle.value in VexLifeProdScreenLens.elementRefs(),
            "elementRefs must include structural elements after spatial export",
        )
    }

    @Test
    fun vexContextProfileControlsStayDistinctAndInputComponentsRemainReadOnly() {
        val lens = VexLifeProdScreenLens.export()
        val screen = lens["screens"]!!.jsonArray.single().jsonObject
        val elements =
            screen["elements"]!!
                .jsonArray
                .map { it.jsonObject }
                .associateBy { it.string("ref") }
        val pathways =
            lens["pathways"]!!
                .jsonArray
                .map { it.jsonObject }
                .associateBy { it.string("pathwayRef") }

        val expectedControls =
            mapOf(
                VexLifeWorkspaceBlueprint.VexContextProfileUse.ref to
                    ("pathway.vexlife-prod.context-profile.use" to VexLifeContextProfileFamily.ControlUse),
                VexLifeWorkspaceBlueprint.VexContextProfileSave.ref to
                    ("pathway.vexlife-prod.context-profile.save" to VexLifeContextProfileFamily.ControlSave),
                VexLifeWorkspaceBlueprint.VexContextProfileSaveAs.ref to
                    ("pathway.vexlife-prod.context-profile.save-as" to VexLifeContextProfileFamily.ControlSaveAs),
                VexLifeWorkspaceBlueprint.VexContextProfileRestore.ref to
                    ("pathway.vexlife-prod.context-profile.restore-product-default" to VexLifeContextProfileFamily.ControlRestoreDefault),
                VexLifeWorkspaceBlueprint.VexContextProfileImport.ref to
                    ("pathway.vexlife-prod.context-profile.import" to VexLifeContextProfileFamily.ControlImport),
            )

        expectedControls.forEach { (elementRef, expected) ->
            val (pathwayRef, controlRef) = expected
            assertEquals(pathwayRef, elements.getValue(elementRef).string("pathwayRef"), elementRef)
            val pathway = pathways.getValue(pathwayRef)
            assertEquals("SCAE_FAMILY", pathway.string("disposition"), pathwayRef)
            assertEquals(
                controlRef,
                pathway["nodes"]!!
                    .jsonArray
                    .map { it.jsonObject }
                    .single { it.string("role") == "CONTROL" }
                    .string("label"),
                pathwayRef,
            )
        }

        for (
            ref in
                listOf(
                    VexLifeWorkspaceBlueprint.VexContextInputFoundation.ref,
                    VexLifeWorkspaceBlueprint.VexContextInputMemory.ref,
                    VexLifeWorkspaceBlueprint.VexContextInputContinuity.ref,
                    VexLifeWorkspaceBlueprint.VexContextInputConversation.ref,
                    VexLifeWorkspaceBlueprint.VexContextInputBrowser.ref,
                    VexLifeWorkspaceBlueprint.VexContextInputRequest.ref,
                    VexLifeWorkspaceBlueprint.VexContextInputComponents.ref,
                )
        ) {
            val element = elements.getValue(ref)
            assertEquals(VexLifeWorkspaceBlueprint.VexContextInput.ref, element.string("parentRef"), ref)
            assertEquals("REGION", element.string("kind"), ref)
            assertEquals(JsonNull, element["pathwayRef"], "$ref remains read-only presentation")
        }

        val html = screen["view"]!!.jsonObject.string("html")
        expectedControls.keys.forEach { assertTrue("""data-vex-ref="$it"""" in html, it) }
        assertTrue(
            """data-vex-ref="${VexLifeWorkspaceBlueprint.VexContextInputComponents.ref}"""" in html,
        )
    }

    @Test
    fun staticPresentationViewKeepsProductOwnedGeometryAndStableIdentity() {
        val lens = VexLifeProdScreenLens.export()
        val screen = lens["screens"]!!.jsonArray.single().jsonObject
        val view = screen["view"]!!.jsonObject
        val anchors = view["anchors"]!!.jsonObject

        assertEquals("STATIC_HTML_CSS", view.string("kind"))
        assertEquals("NONE", view.string("scriptPolicy"))
        assertEquals("data-vex-screen", anchors.string("screenAttribute"))
        assertEquals("data-vex-ref", anchors.string("elementAttribute"))
        assertEquals("data-vex-structural-ref", anchors.string("structuralAttribute"))
        assertEquals("data-vex-structural-parent", anchors.string("structuralParentAttribute"))
        assertTrue("javascript" !in view, "Screen Lens projection must not export Product JavaScript")

        val colorMode = view["colorMode"]!!.jsonObject
        assertEquals("VexLifeWebP1RuntimeArtifacts", colorMode.string("owner"))
        assertEquals("common:web/VexLifeWebP1RuntimeArtifacts.kt", colorMode.string("source"))
        assertEquals("data-theme", colorMode.string("attribute"))
        assertEquals("vexlife-prod.presentation.theme", colorMode.string("storageKey"))
        assertEquals("SYSTEM", colorMode.string("defaultRef"))
        val colorOptions =
            colorMode["options"]!!
                .jsonArray
                .map { it.jsonObject }
                .associateBy { it.string("ref") }
        assertEquals(setOf("SYSTEM", "LIGHT", "DARK"), colorOptions.keys)
        assertEquals(JsonNull, colorOptions.getValue("SYSTEM")["attributeValue"])
        assertEquals("light", colorOptions.getValue("LIGHT").string("attributeValue"))
        assertEquals("dark", colorOptions.getValue("DARK").string("attributeValue"))

        val stateBindings =
            view["stateBindings"]!!
                .jsonArray
                .map { it.jsonObject }
                .associateBy { it.string("presenceRef") }
        val inspectorBinding =
            stateBindings.getValue("presence.vexlife-workspace.companion.context-inspector-open")
        assertEquals("OPEN", inspectorBinding.string("value"))
        val inspectorOperation = inspectorBinding["operations"]!!.jsonArray.single().jsonObject
        assertEquals("SET_ATTRIBUTE", inspectorOperation.string("op"))
        assertEquals(VexLifeWorkspaceBlueprint.ContextInspector.ref, inspectorOperation.string("targetRef"))
        assertEquals("open", inspectorOperation.string("attribute"))

        val privateBinding =
            stateBindings.getValue("presence.vexlife-workspace.companion.private-banner")
        assertEquals("0", privateBinding.string("value"))
        val privateOperation = privateBinding["operations"]!!.jsonArray.single().jsonObject
        assertEquals(VexLifeWorkspaceBlueprint.screenRef, privateOperation.string("targetRef"))
        assertEquals("data-vex-private", privateOperation.string("attribute"))
        assertEquals("true", privateOperation.string("value"))

        val interactionBindings = view["interactionBindings"]!!.jsonArray.map { it.jsonObject }
        val split = interactionBindings.single { it.string("kind") == "SPLIT" }
        assertEquals(VexLifeWebP1.Refs.BrowserCompanionHandle.value, split.string("elementRef"))
        assertEquals(VexLifeWebP1.Refs.BodyFrame.value, split.string("targetRef"))
        assertEquals(VexLifeWebP1.Refs.BrowserRegion.value, split.string("beforeRef"))
        assertEquals(VexLifeWebP1.Refs.CompanionRegion.value, split.string("afterRef"))
        assertEquals("--before", split.string("styleProperty"))
        assertEquals("data-collapsed", split.string("collapsedAttribute"))
        assertEquals("before", split.string("collapsedBeforeValue"))
        assertEquals("after", split.string("collapsedAfterValue"))

        val select = interactionBindings.single { it.string("kind") == "SELECT" }
        assertEquals(VexLifeWorkspaceBlueprint.AppSelector.ref, select.string("elementRef"))
        assertEquals("presence.vexlife-workspace.browser.app-selection", select.string("presenceRef"))
        assertEquals(
            setOf(
                VexLifeWorkspaceApp.SourceTemplate.key to VexLifeWorkspaceBlueprint.SourceTemplateFamily.ref,
                VexLifeWorkspaceApp.Archives.key to VexLifeWorkspaceBlueprint.Archives.ref,
                VexLifeWorkspaceApp.VexContext.key to VexLifeWorkspaceBlueprint.VexContext.ref,
                VexLifeWorkspaceApp.Browser.key to VexLifeWorkspaceBlueprint.BrowserViewport.ref,
            ),
            select["options"]!!.jsonArray.map { it.jsonObject.string("value") to it.jsonObject.string("outcome") }.toSet(),
        )

        val toggle = interactionBindings.single { it.string("kind") == "TOGGLE" }
        assertEquals(VexLifeWorkspaceBlueprint.QuickActions.ref, toggle.string("elementRef"))
        assertEquals(VexLifeWorkspaceBlueprint.QuickActions.ref, toggle.string("targetRef"))
        assertEquals("presence.vexlife-workspace.header.quick-actions-open", toggle.string("presenceRef"))
        assertEquals("data-open", toggle.string("attribute"))
        assertEquals("false", toggle.string("closedValue"))
        assertEquals("true", toggle.string("openValue"))

        val html = view.string("html")
        val css = view.string("css")
        assertTrue("""data-vex-ref="${VexLifeWorkspaceBlueprint.PrivateMode.ref}"""" in html)
        assertTrue("""data-vex-structural-ref="${VexLifeWebP1.Refs.WorkspaceFrame.value}"""" in html)
        assertTrue("""data-vex-structural-ref="${VexLifeWebP1.Refs.BodyFrame.value}"""" in html)
        assertTrue("""data-vex-structural-ref="${VexLifeWebP1.Refs.BrowserRegion.value}"""" in html)
        assertTrue("""data-vex-structural-ref="${VexLifeWebP1.Refs.CompanionRegion.value}"""" in html)
        assertTrue("""data-vex-structural-parent="${VexLifeWebP1.Refs.CompanionContent.value}"""" in html)
        assertTrue(".workspace {" in css)
        assertTrue(".frame {" in css)
        assertTrue(".companion-head {" in css)
        assertTrue(".conversation {" in css)
    }

    @Test
    fun runtimePresentationIsProjectedAsExpressionsInsteadOfSimultaneousContent() {
        val lens = VexLifeProdScreenLens.export()
        val screen = lens["screens"]!!.jsonArray.single().jsonObject
        val elements =
            screen["elements"]!!
                .jsonArray
                .map { it.jsonObject }
                .associateBy { it.string("ref") }
        val realization = screen["realization"]!!.jsonObject

        assertEquals("PROJECTED", realization.string("status"))
        assertEquals(VexLifeWorkspaceBlueprint.screenRef, realization.string("rendersRegisteredTree"))

        val presence =
            realization["presence"]!!
                .jsonArray
                .map { it.jsonObject }
                .associateBy { it.string("ref") }

        val quickActions = presence.getValue("presence.vexlife-workspace.header.quick-actions-open")
        assertEquals("PRESENTATION", quickActions.string("kind"))
        assertEquals(
            setOf(
                VexLifeWorkspaceBlueprint.Speech.ref,
                VexLifeWorkspaceBlueprint.Language.ref,
                VexLifeWorkspaceBlueprint.VisionQuickAccess.ref,
                VexLifeWorkspaceBlueprint.ReleaseAlerts.ref,
            ),
            quickActions["realizes"]!!.jsonArray.map { it.jsonPrimitive.content }.toSet(),
        )

        val browser = presence.getValue("presence.vexlife-workspace.browser.app-selection")
        assertEquals("SELECTION", browser.string("kind"))
        assertEquals(VexLifeWorkspaceBlueprint.Browser.ref, browser.string("subjectRef"))
        assertEquals(VexLifeWorkspaceBlueprint.SourceTemplateFamily.ref, browser.string("defaultRef"))
        assertEquals(
            setOf(
                VexLifeWorkspaceApp.SourceTemplate.key to VexLifeWorkspaceBlueprint.SourceTemplateFamily.ref,
                VexLifeWorkspaceApp.Archives.key to VexLifeWorkspaceBlueprint.Archives.ref,
                VexLifeWorkspaceApp.VexContext.key to VexLifeWorkspaceBlueprint.VexContext.ref,
                VexLifeWorkspaceApp.Browser.key to VexLifeWorkspaceBlueprint.BrowserViewport.ref,
            ),
            screen["view"]!!.jsonObject["interactionBindings"]!!.jsonArray
                .map { it.jsonObject }
                .single { it.string("kind") == "SELECT" }["options"]!!.jsonArray
                .map { it.jsonObject.string("value") to it.jsonObject.string("outcome") }
                .toSet(),
        )
        assertEquals(
            setOf(
                VexLifeWorkspaceBlueprint.SourceTemplateFamily.ref,
                VexLifeWorkspaceBlueprint.Archives.ref,
                VexLifeWorkspaceBlueprint.VexContext.ref,
                VexLifeWorkspaceBlueprint.BrowserViewport.ref,
            ),
            browser["choices"]!!.jsonArray.mapNotNull {
                it.jsonObject["targetRef"]?.jsonPrimitive?.content
            }.toSet(),
        )

        val companion = presence.getValue("presence.vexlife-workspace.companion.content-selection")
        assertEquals("SELECTION", companion.string("kind"))
        assertEquals(VexLifeWebP1.Refs.CompanionContent.value, companion.string("subjectRef"))
        assertEquals(VexLifeWorkspaceBlueprint.Conversation.ref, companion.string("defaultRef"))
        assertEquals(
            setOf(
                VexLifeWorkspaceBlueprint.Conversation.ref,
                VexLifeWorkspaceBlueprint.Settings.ref,
            ),
            companion["choices"]!!.jsonArray.mapNotNull {
                it.jsonObject["targetRef"]?.jsonPrimitive?.content
            }.toSet(),
        )

        for (
            ref in
                listOf(
                    "presence.vexlife-workspace.header.language-panel-open",
                    "presence.vexlife-workspace.header.release-alerts-panel-open",
                    "presence.vexlife-workspace.companion.context-inspector-open",
                    "presence.vexlife-workspace.snackbar-visible",
                )
        ) {
            assertEquals("PRESENTATION", presence.getValue(ref).string("kind"), ref)
        }
        assertEquals(
            "BRANCH",
            presence.getValue("presence.vexlife-workspace.companion.private-banner").string("kind"),
        )
        assertEquals(
            "BRANCH",
            presence.getValue("presence.vexlife-workspace.companion.context-receipt").string("kind"),
        )

        assertEquals("OVERLAY", elements.getValue(VexLifeWorkspaceBlueprint.LanguagePanel.ref).string("slot").uppercase())
        assertEquals("TRANSIENT", elements.getValue(VexLifeWorkspaceBlueprint.LanguagePanel.ref).string("layer"))
        assertEquals("OVERLAY", elements.getValue(VexLifeWorkspaceBlueprint.ReleaseAlertsPanel.ref).string("slot").uppercase())
        assertEquals("TRANSIENT", elements.getValue(VexLifeWorkspaceBlueprint.ReleaseAlertsPanel.ref).string("layer"))
        assertEquals("DIALOG", elements.getValue(VexLifeWorkspaceBlueprint.ContextInspector.ref).string("kind"))
        assertEquals("OVERLAY", elements.getValue(VexLifeWorkspaceBlueprint.ContextInspector.ref).string("slot").uppercase())
        assertEquals("MODAL", elements.getValue(VexLifeWorkspaceBlueprint.ContextInspector.ref).string("layer"))

        assertEquals(
            "swapTo(container = CompanionContent, child = Settings)",
            elements.getValue(VexLifeWorkspaceBlueprint.CompanionViewSwitch.ref).string("intent"),
        )
        assertEquals(
            "toggle(LanguagePanel)",
            elements.getValue(VexLifeWorkspaceBlueprint.Language.ref).string("intent"),
        )
        assertEquals(
            "toggle(ReleaseAlertsPanel)",
            elements.getValue(VexLifeWorkspaceBlueprint.ReleaseAlerts.ref).string("intent"),
        )
    }

    private fun JsonObject.string(key: String) = getValue(key).jsonPrimitive.content
}

// [VXG RealForever]
