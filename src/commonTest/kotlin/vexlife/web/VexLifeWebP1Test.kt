package vexlife.web

import kotlin.test.Test
import kotlin.test.assertEquals
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
            45,
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
}

// [VXG RealForever]
