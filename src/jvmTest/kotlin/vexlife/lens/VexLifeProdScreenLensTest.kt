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
import vexlife.web.VexLifeWebP1ExperienceMarkup
import vexlife.web.VexLifeWebP1ExperienceStyle
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

        val attach = elements.single { it.string("ref") == VexLifeWorkspaceBlueprint.Attach.ref }
        assertEquals(
            "pathway.vexlife-prod.attachment.draft",
            attach.string("pathwayRef"),
        )
        val attachmentPath =
            pathways.getValue(
                "pathway.vexlife-prod.attachment.draft",
            )
        assertEquals(
            "SCAE_FAMILY",
            attachmentPath.string(
                "disposition",
            ),
        )
        assertTrue(
            attachmentPath.toString().contains(
                "VexLifeAttachmentDraftFamily",
            ) &&
                attachmentPath.toString().contains(
                    "VexLifeLocalArtifactStore",
                ) &&
                attachmentPath.toString().contains(
                    "READY snapshot -> exact durable REQUEST",
                ),
        )

        val localSources =
            elements.single {
                it.string(
                    "ref",
                ) ==
                    VexLifeWorkspaceBlueprint.LocalSourcesAdd.ref
            }
        assertEquals(
            "pathway.vexlife-prod.local-sources.manage",
            localSources.string(
                "pathwayRef",
            ),
        )
        val localSourcesPath =
            pathways.getValue(
                "pathway.vexlife-prod.local-sources.manage",
            )
        assertEquals(
            "SCAE_FAMILY",
            localSourcesPath.string(
                "disposition",
            ),
        )
        assertTrue(
            localSourcesPath.toString().contains(
                "VexLifeLocalSourcesFamily",
            ) &&
                localSourcesPath.toString().contains(
                    "VexLifeLocalFolderPicker",
                ) &&
                localSourcesPath.toString().contains(
                    "VexLifeLocalSourceReach",
                ),
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
    fun staticPresentationViewUsesCurrentProductMarkupAndStyleWithoutJavaScript() {
        val lens = VexLifeProdScreenLens.export()
        val screen = lens["screens"]!!.jsonArray.single().jsonObject
        val view = screen["view"]!!.jsonObject
        val anchors = view["anchors"]!!.jsonObject

        assertEquals("STATIC_HTML_CSS", view.string("kind"))
        assertEquals("NONE", view.string("scriptPolicy"))
        assertEquals(VexLifeWebP1ExperienceMarkup.render(), view.string("html"))
        assertEquals(VexLifeWebP1ExperienceStyle.render(), view.string("css"))
        assertEquals("data-vex-screen", anchors.string("screenAttribute"))
        assertEquals("data-vex-ref", anchors.string("elementAttribute"))
        assertEquals("data-vex-structural-ref", anchors.string("structuralAttribute"))
        assertEquals("data-vex-structural-parent", anchors.string("structuralParentAttribute"))
        val html = view.string("html")
        assertTrue("""data-vex-ref="${VexLifeWorkspaceBlueprint.Theme.ref}"""" in html)
        assertTrue("""data-vex-ref="${VexLifeWorkspaceBlueprint.Support.ref}"""" in html)
        assertTrue("""data-vex-ref="${VexLifeWorkspaceBlueprint.QuickActionsTrigger.ref}"""" in html)
        assertTrue("""data-vex-ref="${VexLifeWorkspaceBlueprint.SpeechPanel.ref}"""" in html)
        assertTrue("""data-vex-ref="${VexLifeWorkspaceBlueprint.LanguagePanel.ref}"""" in html)
        assertTrue("""id="speech" aria-label="Speech" data-open="false"""" in html)
        assertTrue("""id="language-panel" aria-label="Reply language" data-open="false"""" in html)
        assertTrue("""id="release-alerts-panel" aria-labelledby="release-alerts-title" data-open="false"""" in html)
        assertTrue("""id="quick-actions" aria-label="Quick actions" data-open="false"""" in html)
        assertTrue("🌀" in html)
        assertTrue(
            listOf(
                VexLifeWorkspaceBlueprint.SpeechEnabled.ref,
                VexLifeWorkspaceBlueprint.SpeechVoice.ref,
                VexLifeWorkspaceBlueprint.SpeechReadThoughts.ref,
                VexLifeWorkspaceBlueprint.SpeechRate.ref,
                VexLifeWorkspaceBlueprint.SpeechPrevious.ref,
                VexLifeWorkspaceBlueprint.SpeechPlayPause.ref,
                VexLifeWorkspaceBlueprint.SpeechStop.ref,
                VexLifeWorkspaceBlueprint.SpeechNext.ref,
                VexLifeWorkspaceBlueprint.SpeechChooseStart.ref,
            ).all { ref -> """data-vex-ref="$ref"""" in html },
        )
        assertTrue("javascript" !in view, "Screen Lens projection must not export Product JavaScript")
    }

    @Test
    fun currentRuntimePresentationMapsToExpressionsAndInteractions() {
        val lens = VexLifeProdScreenLens.export()
        val screen = lens["screens"]!!.jsonArray.single().jsonObject
        val view = screen["view"]!!.jsonObject
        val realization = screen["realization"]!!.jsonObject
        val presence =
            realization["presence"]!!.jsonArray
                .map { it.jsonObject }
                .associateBy { it.string("ref") }

        val browser = presence.getValue("presence.vexlife-workspace.browser.app-selection")
        assertEquals("SELECTION", browser.string("kind"))
        assertEquals(VexLifeWorkspaceBlueprint.Browser.ref, browser.string("subjectRef"))
        assertEquals(VexLifeWorkspaceBlueprint.SourceTemplateFamily.ref, browser.string("defaultRef"))
        assertEquals(
            setOf(
                VexLifeWorkspaceBlueprint.SourceTemplateFamily.ref,
                VexLifeWorkspaceBlueprint.Archives.ref,
                VexLifeWorkspaceBlueprint.VexContext.ref,
                VexLifeWorkspaceBlueprint.BrowserViewport.ref,
            ),
            browser["choices"]!!.jsonArray.mapNotNull { it.jsonObject["targetRef"]?.jsonPrimitive?.content }.toSet(),
        )

        val companion = presence.getValue("presence.vexlife-workspace.companion.content-selection")
        assertEquals("SELECTION", companion.string("kind"))
        assertEquals(VexLifeWebP1.Refs.CompanionContent.value, companion.string("subjectRef"))
        assertEquals(VexLifeWorkspaceBlueprint.Conversation.ref, companion.string("defaultRef"))
        assertEquals(
            setOf(VexLifeWorkspaceBlueprint.Conversation.ref, VexLifeWorkspaceBlueprint.Settings.ref),
            companion["choices"]!!.jsonArray.map { it.jsonObject.string("targetRef") }.toSet(),
        )

        val quickActions = presence.getValue("presence.vexlife-workspace.header.quick-actions-open")
        assertEquals("PRESENTATION", quickActions.string("kind"))
        assertEquals(
            setOf(
                VexLifeWorkspaceBlueprint.Speech.ref,
                VexLifeWorkspaceBlueprint.Language.ref,
                VexLifeWorkspaceBlueprint.VisionQuickAccess.ref,
                VexLifeWorkspaceBlueprint.Theme.ref,
                VexLifeWorkspaceBlueprint.ReleaseAlerts.ref,
                VexLifeWorkspaceBlueprint.Support.ref,
            ),
            quickActions["realizes"]!!.jsonArray.map { it.jsonPrimitive.content }.toSet(),
        )

        val quickActionsRef = "presence.vexlife-workspace.header.quick-actions-open"
        val nestedPresentations =
            listOf(
                Triple(
                    "presence.vexlife-workspace.header.speech-panel-open",
                    VexLifeWorkspaceBlueprint.SpeechPanel.ref,
                    "speech.open",
                ),
                Triple(
                    "presence.vexlife-workspace.header.language-panel-open",
                    VexLifeWorkspaceBlueprint.LanguagePanel.ref,
                    "state.languageOpen",
                ),
                Triple(
                    "presence.vexlife-workspace.header.release-alerts-panel-open",
                    VexLifeWorkspaceBlueprint.ReleaseAlertsPanel.ref,
                    "state.releaseAlertsOpen",
                ),
            )
        for ((ref, subjectRef, condition) in nestedPresentations) {
            val presentation = presence.getValue(ref)
            assertEquals("PRESENTATION", presentation.string("kind"))
            assertEquals(subjectRef, presentation.string("subjectRef"))
            assertEquals(condition, presentation.string("condition"))
            assertEquals(quickActionsRef, presentation["gate"]!!.jsonObject.string("presenceRef"))
        }

        val interactionBindings = view["interactionBindings"]!!.jsonArray.map { it.jsonObject }
        assertEquals(setOf("SPLIT", "SELECT", "TOGGLE"), interactionBindings.map { it.string("kind") }.toSet())
        val select = interactionBindings.single { it.string("kind") == "SELECT" }
        assertEquals(VexLifeWorkspaceBlueprint.AppSelector.ref, select.string("elementRef"))
        assertEquals(
            setOf(
                VexLifeWorkspaceApp.SourceTemplate.key to VexLifeWorkspaceBlueprint.SourceTemplateFamily.ref,
                VexLifeWorkspaceApp.Archives.key to VexLifeWorkspaceBlueprint.Archives.ref,
                VexLifeWorkspaceApp.VexContext.key to VexLifeWorkspaceBlueprint.VexContext.ref,
                VexLifeWorkspaceApp.Browser.key to VexLifeWorkspaceBlueprint.BrowserViewport.ref,
            ),
            select["options"]!!.jsonArray.map { it.jsonObject.string("value") to it.jsonObject.string("outcome") }.toSet(),
        )

        val toggles =
            interactionBindings
                .filter { it.string("kind") == "TOGGLE" }
                .associateBy { it.string("elementRef") }
        assertEquals(
            setOf(
                VexLifeWorkspaceBlueprint.QuickActionsTrigger.ref,
                VexLifeWorkspaceBlueprint.Speech.ref,
                VexLifeWorkspaceBlueprint.Language.ref,
                VexLifeWorkspaceBlueprint.ReleaseAlerts.ref,
            ),
            toggles.keys,
        )
        fun assertToggle(
            elementRef: String,
            targetRef: String,
            presenceRef: String,
        ) {
            val toggle = toggles.getValue(elementRef)
            assertEquals(targetRef, toggle.string("targetRef"))
            assertEquals(presenceRef, toggle.string("presenceRef"))
            assertEquals("data-open", toggle.string("attribute"))
            assertEquals("false", toggle.string("closedValue"))
            assertEquals("true", toggle.string("openValue"))
        }
        assertToggle(
            VexLifeWorkspaceBlueprint.QuickActionsTrigger.ref,
            VexLifeWorkspaceBlueprint.QuickActions.ref,
            quickActionsRef,
        )
        assertToggle(
            VexLifeWorkspaceBlueprint.Speech.ref,
            VexLifeWorkspaceBlueprint.SpeechPanel.ref,
            "presence.vexlife-workspace.header.speech-panel-open",
        )
        assertToggle(
            VexLifeWorkspaceBlueprint.Language.ref,
            VexLifeWorkspaceBlueprint.LanguagePanel.ref,
            "presence.vexlife-workspace.header.language-panel-open",
        )
        assertToggle(
            VexLifeWorkspaceBlueprint.ReleaseAlerts.ref,
            VexLifeWorkspaceBlueprint.ReleaseAlertsPanel.ref,
            "presence.vexlife-workspace.header.release-alerts-panel-open",
        )

        val colorMode = view["colorMode"]!!.jsonObject
        assertEquals("SYSTEM", colorMode.string("defaultRef"))
        assertEquals("data-theme", colorMode.string("attribute"))
        assertEquals("vexlife-prod.presentation.theme", colorMode.string("storageKey"))
        val colorOptions = colorMode["options"]!!.jsonArray.map { it.jsonObject }.associateBy { it.string("ref") }
        assertEquals(setOf("SYSTEM", "LIGHT", "DARK"), colorOptions.keys)
        assertEquals(JsonNull, colorOptions.getValue("SYSTEM")["attributeValue"])
        assertEquals("light", colorOptions.getValue("LIGHT").string("attributeValue"))
        assertEquals("dark", colorOptions.getValue("DARK").string("attributeValue"))

        val stateBindingList = view["stateBindings"]!!.jsonArray.map { it.jsonObject }
        val stateBindings = stateBindingList.associateBy { it.string("presenceRef") + "=" + it.string("value") }
        assertEquals(
            "OPEN",
            stateBindings.getValue("presence.vexlife-workspace.companion.context-inspector-open=OPEN").string("value"),
        )
        assertEquals(
            "0",
            stateBindings.getValue("presence.vexlife-workspace.companion.private-banner=0").string("value"),
        )

        fun assertPresentationConsequences(
            presenceRef: String,
            targetRef: String,
        ) {
            val open = stateBindings.getValue("$presenceRef=OPEN")
            val openOperations = open["operations"]!!.jsonArray.map { it.jsonObject }
            assertTrue(
                openOperations.any {
                    it.string("op") == "REMOVE_ATTRIBUTE" &&
                        it.string("targetRef") == targetRef &&
                        it.string("attribute") == "hidden"
                },
            )
            assertTrue(
                openOperations.any {
                    it.string("op") == "SET_ATTRIBUTE" &&
                        it.string("targetRef") == targetRef &&
                        it.string("attribute") == "data-open" &&
                        it.string("value") == "true"
                },
            )

            val closed = stateBindings.getValue("$presenceRef=CLOSED")
            val closedOperations = closed["operations"]!!.jsonArray.map { it.jsonObject }
            assertTrue(
                closedOperations.any {
                    it.string("op") == "SET_ATTRIBUTE" &&
                        it.string("targetRef") == targetRef &&
                        it.string("attribute") == "hidden"
                },
            )
            assertTrue(
                closedOperations.any {
                    it.string("op") == "SET_ATTRIBUTE" &&
                        it.string("targetRef") == targetRef &&
                        it.string("attribute") == "data-open" &&
                        it.string("value") == "false"
                },
            )
        }

        assertPresentationConsequences(
            "presence.vexlife-workspace.header.speech-panel-open",
            VexLifeWorkspaceBlueprint.SpeechPanel.ref,
        )
        assertPresentationConsequences(
            "presence.vexlife-workspace.header.language-panel-open",
            VexLifeWorkspaceBlueprint.LanguagePanel.ref,
        )
        assertPresentationConsequences(
            "presence.vexlife-workspace.header.release-alerts-panel-open",
            VexLifeWorkspaceBlueprint.ReleaseAlertsPanel.ref,
        )

        val elements = screen["elements"]!!.jsonArray.map { it.jsonObject }.associateBy { it.string("ref") }
        assertEquals(
            "presentation ${VexLifeWebP1.Refs.ColorModeToggleAction}",
            elements.getValue(VexLifeWorkspaceBlueprint.Theme.ref).string("intent"),
        )
        assertEquals(
            "external ${VexLifeWebP1.Refs.SupportHref}",
            elements.getValue(VexLifeWorkspaceBlueprint.Support.ref).string("intent"),
        )
        assertEquals(
            "local ${VexLifeWorkspaceBlueprint.QuickActions.ref}",
            elements.getValue(VexLifeWorkspaceBlueprint.QuickActionsTrigger.ref).string("intent"),
        )
        assertEquals(
            "toggle(SpeechPanel)",
            elements.getValue(VexLifeWorkspaceBlueprint.Speech.ref).string("intent"),
        )
        assertEquals(
            "toggle(LanguagePanel)",
            elements.getValue(VexLifeWorkspaceBlueprint.Language.ref).string("intent"),
        )
        assertEquals(
            "toggle(ReleaseAlertsPanel)",
            elements.getValue(VexLifeWorkspaceBlueprint.ReleaseAlerts.ref).string("intent"),
        )

        val speechActions =
            mapOf(
                VexLifeWorkspaceBlueprint.SpeechEnabled.ref to VexLifeWebP1.Refs.SpeechEnabledToggleAction,
                VexLifeWorkspaceBlueprint.SpeechVoice.ref to VexLifeWebP1.Refs.SpeechVoiceChangeAction,
                VexLifeWorkspaceBlueprint.SpeechReadThoughts.ref to VexLifeWebP1.Refs.SpeechReadThoughtsChangeAction,
                VexLifeWorkspaceBlueprint.SpeechRate.ref to VexLifeWebP1.Refs.SpeechRateChangeAction,
                VexLifeWorkspaceBlueprint.SpeechPrevious.ref to VexLifeWebP1.Refs.SpeechPreviousAction,
                VexLifeWorkspaceBlueprint.SpeechPlayPause.ref to VexLifeWebP1.Refs.SpeechPlayPauseAction,
                VexLifeWorkspaceBlueprint.SpeechStop.ref to VexLifeWebP1.Refs.SpeechStopAction,
                VexLifeWorkspaceBlueprint.SpeechNext.ref to VexLifeWebP1.Refs.SpeechNextAction,
                VexLifeWorkspaceBlueprint.SpeechChooseStart.ref to VexLifeWebP1.Refs.SpeechChooseStartAction,
            )
        val pathways = lens["pathways"]!!.jsonArray.map { it.jsonObject }.associateBy { it.string("pathwayRef") }
        for ((ref, actionRef) in speechActions) {
            val element = elements.getValue(ref)
            assertEquals("action $actionRef", element.string("intent"))
            val pathwayRef = element.string("pathwayRef")
            val pathway = pathways.getValue(pathwayRef)
            assertEquals("DIRECT_REGISTRATION", pathway.string("disposition"))
            assertTrue(
                pathway["nodes"]!!.jsonArray
                    .map { it.jsonObject }
                    .any { it.string("label") == actionRef && it.string("role") == "DISPATCH" },
            )
        }
        assertTrue("CHANGE" in elements.getValue(VexLifeWorkspaceBlueprint.SpeechVoice.ref)["interactions"]!!.jsonArray.map { it.jsonPrimitive.content })
        assertTrue("CHANGE" in elements.getValue(VexLifeWorkspaceBlueprint.SpeechReadThoughts.ref)["interactions"]!!.jsonArray.map { it.jsonPrimitive.content })
        assertTrue("CHANGE" in elements.getValue(VexLifeWorkspaceBlueprint.SpeechRate.ref)["interactions"]!!.jsonArray.map { it.jsonPrimitive.content })
    }

    private fun JsonObject.string(key: String) = getValue(key).jsonPrimitive.content
}

// [VXG RealForever]
