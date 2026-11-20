package vexlife.web

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import vexlife.workspace.VexLifeWorkspaceBlueprint

class VexLifeWebP1Test {
    @Test
    fun p1ManifestsEveryCurrentBlueprintCoordinateWithTypedCapabilityBinding() {
        val manifested =
            VexLifeWebP1.Surface
                .coordinates()
                .map {
                    it.ref
                }
                .toSet()
        val blueprint =
            VexLifeWorkspaceBlueprint
                .elements
                .map {
                    it.ref
                }
                .toSet()

        assertEquals(
            86,
            blueprint.size,
        )
        assertTrue(
            manifested.containsAll(
                blueprint,
            ),
        )

        val vexSummon =
            VexLifeWebP1.Surface
                .coordinates()
                .single {
                    it.ref ==
                        VexLifeWorkspaceBlueprint.VexSummon.ref
                }
        assertTrue(
            VexWeb.Interaction.Activate in
                vexSummon.interactions,
        )
        val vexSummonBinding =
            vexSummon.binding as
                VexWeb.Binding.LocalInteraction
        assertEquals(
            VexLifeWebP1.Companion.identity.ref,
            vexSummonBinding.target.ref,
        )

        val visionQuickAccess =
            VexLifeWebP1.Surface
                .coordinates()
                .single {
                    it.ref ==
                        VexLifeWorkspaceBlueprint.VisionQuickAccess.ref
                }
        assertTrue(
            VexWeb.Interaction.Activate in
                visionQuickAccess.interactions,
        )
        val visionQuickAccessBinding =
            visionQuickAccess.binding as
                VexWeb.Binding.LocalInteraction
        assertEquals(
            VexLifeWebP1.Vision.identity.ref,
            visionQuickAccessBinding.target.ref,
        )

        val triggerBinding =
            VexLifeWebP1.Surface.coordinates()
                .single { it.ref == VexLifeWorkspaceBlueprint.QuickActionsTrigger.ref }
                .binding as VexWeb.Binding.LocalInteraction
        assertEquals(VexLifeWorkspaceBlueprint.QuickActions.ref, triggerBinding.target.ref)

        val themeBinding =
            VexLifeWebP1.Surface.coordinates()
                .single { it.ref == VexLifeWorkspaceBlueprint.Theme.ref }
                .binding as VexWeb.Binding.PresentationAction
        assertEquals(VexLifeWebP1.Refs.ColorModeToggleAction, themeBinding.actionRef)

        val supportBinding =
            VexLifeWebP1.Surface.coordinates()
                .single { it.ref == VexLifeWorkspaceBlueprint.Support.ref }
                .binding as VexWeb.Binding.ExternalNavigation
        assertEquals(VexLifeWebP1.Refs.SupportHref, supportBinding.href)

        for (panelRef in listOf(
            VexLifeWorkspaceBlueprint.SpeechPanel.ref,
            VexLifeWorkspaceBlueprint.LanguagePanel.ref,
            VexLifeWorkspaceBlueprint.ReleaseAlertsPanel.ref,
        )) {
            val panel = VexLifeWebP1.Surface.coordinates().single { it.ref == panelRef }
            assertEquals(VexWeb.Placement.Overlay, panel.placement)
            assertEquals(VexWeb.Depth.Transient, panel.depth)
        }

        val speech =
            VexLifeWebP1.Surface.coordinates()
                .single { it.ref == VexLifeWorkspaceBlueprint.Speech.ref }
        val speechBinding = speech.binding as VexWeb.Binding.LocalInteraction
        assertEquals(VexLifeWorkspaceBlueprint.SpeechPanel.ref, speechBinding.target.ref)

        val language =
            VexLifeWebP1.Surface.coordinates()
                .single { it.ref == VexLifeWorkspaceBlueprint.Language.ref }
        val languageBinding = language.binding as VexWeb.Binding.LocalInteraction
        assertEquals(VexLifeWorkspaceBlueprint.LanguagePanel.ref, languageBinding.target.ref)

        val releaseAlerts =
            VexLifeWebP1.Surface
                .coordinates()
                .single {
                    it.ref ==
                        VexLifeWorkspaceBlueprint.ReleaseAlerts.ref
                }
        assertTrue(
            VexWeb.Interaction.Activate in
                releaseAlerts.interactions,
        )
        val releaseAlertsBinding =
            releaseAlerts.binding as
                VexWeb.Binding.LocalInteraction
        assertEquals(
            VexLifeWebP1.ReleaseAlertsPanel.identity.ref,
            releaseAlertsBinding.target.ref,
        )

        val vision =
            VexLifeWebP1.Surface
                .coordinates()
                .single {
                    it.ref ==
                        VexLifeWorkspaceBlueprint.Vision.ref
                }
        assertTrue(
            VexWeb.Interaction.Activate in
                vision.interactions,
        )
        assertTrue(
            vision.binding is
                VexWeb.Binding.Capability,
        )
        assertEquals(
            VexLifeWebP1.Refs.VisionSelectedScopeCapability,
            (
                vision.binding as
                    VexWeb.Binding.Capability
            ).capabilityRef,
        )

        val viewSwitch =
            VexLifeWebP1.Surface
                .coordinates()
                .single {
                    it.ref ==
                        VexLifeWorkspaceBlueprint.CompanionViewSwitch.ref
                }
        assertTrue(
            VexWeb.Interaction.Activate in
                viewSwitch.interactions,
        )
        val viewSwitchBinding =
            viewSwitch.binding as
                VexWeb.Binding.LocalInteraction
        assertEquals(
            VexLifeWebP1.Settings.identity.ref,
            viewSwitchBinding.target.ref,
        )

        val contextShare =
            VexLifeWebP1.Surface
                .coordinates()
                .single {
                    it.ref ==
                        VexLifeWorkspaceBlueprint.ContextShare.ref
                }
        assertTrue(
            VexWeb.Interaction.Activate in
                contextShare.interactions,
        )
        val contextShareBinding =
            contextShare.binding as
                VexWeb.Binding.LocalInteraction
        assertEquals(
            VexLifeWebP1.ContextReceipt.identity.ref,
            contextShareBinding.target.ref,
        )

        val jumpLatest =
            VexLifeWebP1.Surface
                .coordinates()
                .single {
                    it.ref ==
                        VexLifeWorkspaceBlueprint.ConversationJumpLatest.ref
                }
        assertTrue(
            VexWeb.Interaction.Activate in
                jumpLatest.interactions,
        )
        val jumpLatestBinding =
            jumpLatest.binding as
                VexWeb.Binding.LocalInteraction
        assertEquals(
            VexLifeWebP1.ConversationTimeline.identity.ref,
            jumpLatestBinding.target.ref,
        )

        val artifacts =
            VexWebArtifacts.generate(
                VexLifeWebP1.Surface,
            )
        assertTrue(
            artifacts.html.contains(
                "data-vex-binding=\"local-interaction\"",
            ),
        )
        assertTrue(
            artifacts.semanticManifest.contains(
                "\"type\":\"LOCAL_INTERACTION\",\"target\":\"${VexLifeWebP1.ConversationTimeline.identity.ref}\"",
            ),
        )

        val runtimeArtifacts =
            VexLifeWebP1RuntimeArtifacts.generate()
        assertTrue(runtimeArtifacts.javascript.contains("contextConversationCollapsedByEventRef"))
        assertTrue(runtimeArtifacts.javascript.contains("Collapse message"))
        assertTrue(runtimeArtifacts.javascript.contains("Show message"))
        assertTrue(runtimeArtifacts.javascript.contains("Reply language changed."))
        assertTrue(runtimeArtifacts.javascript.contains("read from Vex Home at Send"))
        assertTrue(runtimeArtifacts.javascript.contains("provided automatically · no reach required"))
        assertTrue(runtimeArtifacts.javascript.contains("providerRequestsByResponse"))
        assertTrue(runtimeArtifacts.javascript.contains("Model view · exact VexLife → runtime request"))
        assertTrue(runtimeArtifacts.javascript.contains("will not reconstruct old provider text from current sources"))
        assertTrue(runtimeArtifacts.javascript.contains("not the post-chat-template/tokenized model input"))
        assertTrue(runtimeArtifacts.javascript.contains("memory-current-accepted-semantic"))
        assertTrue(runtimeArtifacts.javascript.contains("does not infer aggregate Memory participation from Daily Memory alone"))
        assertTrue(runtimeArtifacts.javascript.contains("Context held for this completed turn."))

        val voiceInput =
            VexLifeWebP1.Surface
                .coordinates()
                .single {
                    it.ref ==
                        VexLifeWorkspaceBlueprint.VoiceInput.ref
                }
        assertTrue(
            VexWeb.Interaction.Activate in
                voiceInput.interactions,
        )
        val voiceInputBinding =
            voiceInput.binding as
                VexWeb.Binding.LocalInteraction
        assertEquals(
            VexLifeWebP1.Composer.identity.ref,
            voiceInputBinding.target.ref,
        )

        val send =
            VexLifeWebP1.Surface
                .coordinates()
                .single {
                    it.ref ==
                        VexLifeWorkspaceBlueprint.Send.ref
                }

        assertTrue(
            send.binding is
                VexWeb.Binding.Capability,
        )
        assertEquals(
            VexLifeWebP1.Refs.ConversationSendCapability,
            (
                send.binding as
                    VexWeb.Binding.Capability
            ).capabilityRef,
        )

        assertTrue(
            artifacts.semanticManifest.contains(
                "\"type\":\"CAPABILITY\",\"capabilityRef\":\"conversation.send\"",
            ),
        )
        assertTrue(
            artifacts.semanticManifest.contains(
                "\"type\":\"CAPABILITY\",\"capabilityRef\":\"vision.selected-scope\"",
            ),
        )
        assertTrue(
            artifacts.semanticManifest.contains(
                "\"type\":\"PRESENTATION_ACTION\",\"actionRef\":\"${VexLifeWebP1.Refs.ColorModeToggleAction}\"",
            ),
        )
        assertTrue(
            artifacts.semanticManifest.contains(
                "\"type\":\"EXTERNAL_NAVIGATION\",\"href\":\"${VexLifeWebP1.Refs.SupportHref}\"",
            ),
        )
    }

    @Test
    fun companionCompositionMakesHeadingContentFeedAndContextBoundariesSourceVisible() {
        val coordinates =
            VexLifeWebP1.Surface
                .coordinates()
                .associateBy {
                    it.ref
                }

        assertEquals(
            VexLifeWebP1.Companion.identity.ref,
            coordinates
                .getValue(
                    VexLifeWebP1.Refs.CompanionHeader.value,
                )
                .containerRef,
        )
        assertEquals(
            VexLifeWebP1.Companion.identity.ref,
            coordinates
                .getValue(
                    VexLifeWebP1.Refs.CompanionContent.value,
                )
                .containerRef,
        )
        assertEquals(
            VexLifeWebP1.Refs.CompanionContent.value,
            coordinates
                .getValue(
                    VexLifeWebP1.Conversation.identity.ref,
                )
                .containerRef,
        )
        assertEquals(
            VexLifeWebP1.Refs.CompanionContent.value,
            coordinates
                .getValue(
                    VexLifeWebP1.Settings.identity.ref,
                )
                .containerRef,
        )
        assertEquals(
            VexLifeWebP1.Composer.identity.ref,
            coordinates
                .getValue(
                    VexLifeWebP1.ContextReceipt.identity.ref,
                )
                .containerRef,
        )
        assertEquals(
            VexLifeWebP1.Composer.identity.ref,
            coordinates
                .getValue(
                    VexLifeWebP1.ContextShare.identity.ref,
                )
                .containerRef,
        )
        assertEquals(
            VexLifeWebP1.Composer.identity.ref,
            coordinates
                .getValue(
                    VexLifeWebP1.VoiceInput.identity.ref,
                )
                .containerRef,
        )
        assertEquals(
            VexLifeWebP1.SettingsContext.identity.ref,
            coordinates
                .getValue(
                    VexLifeWebP1.ContextWindow.identity.ref,
                )
                .containerRef,
        )
    }

    @Test
    fun p1RestoresPeerBrowserAndCompanionRegionsAroundOneSeam() {
        val regions =
            VexLifeWebP1.Surface
                .spatialRegions()

        assertEquals(
            setOf(
                VexLifeWebP1.Refs.HeaderRegion.value,
                VexLifeWebP1.Refs.BodyRegion.value,
                VexLifeWebP1.Refs.BrowserRegion.value,
                VexLifeWebP1.Refs.CompanionRegion.value,
            ),
            regions
                .map {
                    it.ref
                }
                .toSet(),
        )

        val artifacts =
            VexWebArtifacts.generate(
                VexLifeWebP1.Surface,
            )

        assertTrue(
            artifacts.css.contains(
                "[data-vex-ref=\"${VexLifeWebP1.Refs.HeaderRegion.value}\"]{left:0%;top:0%;width:100%;height:8.333333%;}",
            ),
        )
        assertTrue(
            artifacts.css.contains(
                "[data-vex-ref=\"${VexLifeWebP1.Refs.BodyRegion.value}\"]{left:0%;top:8.333333%;width:100%;height:91.666666%;}",
            ),
        )
        assertTrue(
            artifacts.css.contains(
                "[data-vex-ref=\"${VexLifeWebP1.Refs.BrowserRegion.value}\"]{left:0%;top:0%;width:50%;height:100%;}",
            ),
        )
        assertTrue(
            artifacts.css.contains(
                "[data-vex-ref=\"${VexLifeWebP1.Refs.CompanionRegion.value}\"]{left:50%;top:0%;width:50%;height:100%;}",
            ),
        )
        assertTrue(
            artifacts.semanticManifest.contains(
                "\"ref\":\"${VexLifeWebP1.Refs.BodyRegion.value}\",\"startBoundary\":\"FRAME_START\",\"endBoundary\":\"FRAME_END\",\"topBoundary\":\"SEAM:${VexLifeWebP1.Refs.HeaderBodySeam.value}\",\"bottomBoundary\":\"FRAME_BOTTOM\",\"start\":{\"numerator\":0,\"denominator\":1,\"percent\":\"0%\"},\"end\":{\"numerator\":1,\"denominator\":1,\"percent\":\"100%\"},\"top\":{\"numerator\":1,\"denominator\":12,\"percent\":\"8.333333%\"},\"bottom\":{\"numerator\":1,\"denominator\":1,\"percent\":\"100%\"}",
            ),
        )
        assertTrue(
            artifacts.semanticManifest.contains(
                "\"ref\":\"${VexLifeWebP1.Refs.BrowserRegion.value}\",\"startBoundary\":\"FRAME_START\",\"endBoundary\":\"SEAM:${VexLifeWebP1.Refs.BrowserCompanionSeam.value}\"",
            ),
        )
        assertTrue(
            artifacts.semanticManifest.contains(
                "\"ref\":\"${VexLifeWebP1.Refs.CompanionRegion.value}\",\"nodeKind\":\"REGION\",\"blueprintKind\":null,\"sourceParentRef\":null,\"containerRef\":\"${VexLifeWebP1.Refs.BodyFrame.value}\",\"placement\":\"FillAvailable\",\"depth\":\"Content\"",
            ),
        )
    }

    @Test
    fun processProjectionUsesExistingSendFactsInsteadOfProviderStages() {
        val javascript =
            VexLifeWebP1RuntimeArtifacts
                .generate()
                .javascript

        assertTrue(
            javascript.contains(
                "liveProcessEntries: []",
            ),
        )
        assertTrue(
            javascript.contains(
                "processEntriesByResponse: new Map()",
            ),
        )
        assertTrue(
            javascript.contains(
                "appendProcessReasoning(event.delta);",
            ),
        )
        assertTrue(
            javascript.contains(
                "projectProcessReach(use);",
            ),
        )
        assertTrue(
            javascript.contains(
                "const completedProcess =",
            ) &&
                javascript.contains(
                    "state.processEntriesByResponse.set(responseKey, completedProcess)",
                ),
            "completed presentation must attach the process snapshot selected before transient live state is cleared",
        )
        assertTrue(
            javascript.contains(
                "label.textContent = expanded ? 'hide process' : 'view process';",
            ),
        )
        assertFalse(
            javascript.contains(
                "providerStageIndex",
            ),
            "the browser must project meaningful consequences rather than expose provider invocation numbering",
        )
        assertFalse(
            javascript.contains(
                "providerModelRef",
            ),
            "model identity remains diagnostics/runtime truth rather than primary process UX",
        )
    }

    @Test
    fun ambiguousStreamEndingReconcilesExactSendBeforeShowingUnresolved() {
        val javascript =
            VexLifeWebP1RuntimeArtifacts
                .generate()
                .javascript

        val reconciliation =
            javascript.indexOf(
                "await reconcileSendOutcome(pending)",
            )
        val unresolved =
            javascript.indexOf(
                "setCompanionStatus('Unavailable', 'Send outcome unresolved', '')",
            )

        assertTrue(
            javascript.contains(
                "headers: { accept: 'application/json', 'content-type': 'application/json' }",
            ),
        )
        assertTrue(
            javascript.contains(
                "const interruptedProcess = snapshotLiveProcess()",
            ) &&
                javascript.contains(
                    "await completeCanonicalSend(reconciled, false, interruptedProcess)",
                ),
            "ambiguous stream reconciliation must carry forward the process the human already observed",
        )
        assertTrue(
            reconciliation >=
                0,
        )
        assertTrue(
            unresolved >
                reconciliation,
        )
    }

}

// [VXG RealForever]
