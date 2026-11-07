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
            21,
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
            VexLifeWebP1.Composer.identity.ref,
            vexSummonBinding.target.ref,
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
    }

    @Test
    fun p1UsesNestedRelationalFramesForHeaderBodyAndBrowserCompanionGeometry() {
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
    }
}

// [VXG RealForever]
