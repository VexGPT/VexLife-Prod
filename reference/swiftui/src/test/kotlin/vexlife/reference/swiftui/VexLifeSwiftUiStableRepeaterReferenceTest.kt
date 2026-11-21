package vexlife.reference.swiftui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import vexlife.blueprint.VexUiElementRef
import vexlife.workspace.VexLifeWorkspaceBlueprint

class VexLifeSwiftUiStableRepeaterReferenceTest {
    @Test
    fun currentConversationTimelineIsACompilerVisibleCollectionCoordinate() {
        assertEquals(
            VexUiElementRef.Kind.Collection,
            VexLifeWorkspaceBlueprint
                .ConversationTimeline
                .kind,
        )
        assertEquals(
            VexLifeWorkspaceBlueprint.Conversation.ref,
            VexLifeWorkspaceBlueprint
                .ConversationTimeline
                .parentRef,
        )
    }

    @Test
    fun generationIsDeterministicAndUsesStableSwiftUiIdentity() {
        val first =
            VexLifeSwiftUiStableRepeaterReference.generate()
        val second =
            VexLifeSwiftUiStableRepeaterReference.generate()

        assertEquals(
            first,
            second,
        )
        assertTrue(first.swift.contains("public struct VexConversationTimelineItem: Identifiable"))
        assertTrue(first.swift.contains("self.id = messageRef"))
        assertTrue(first.swift.contains("ForEach(items) { item in"))
        assertTrue(first.swift.contains(".id(item.id)"))
        assertTrue(first.swift.contains(".accessibilityIdentifier(item.id)"))
        assertTrue(
            first.swift.contains(
                VexLifeWorkspaceBlueprint
                    .ConversationTimeline
                    .ref,
            ),
        )
    }

    @Test
    fun featureOwnedItemRefsFailClosedOnBlankOrDuplicateIdentity() {
        assertFailsWith<IllegalArgumentException> {
            VexLifeSwiftUiStableRepeaterReference.ItemCoordinate(
                itemRef = "",
                eventRef = "event.1",
                sequence = 1,
            )
        }

        val repeated =
            listOf(
                VexLifeSwiftUiStableRepeaterReference.ItemCoordinate(
                    itemRef = "message.same",
                    eventRef = "event.1",
                    sequence = 1,
                ),
                VexLifeSwiftUiStableRepeaterReference.ItemCoordinate(
                    itemRef = "message.same",
                    eventRef = "event.2",
                    sequence = 2,
                ),
            )

        assertFailsWith<IllegalArgumentException> {
            VexLifeSwiftUiStableRepeaterReference.stableItemRefs(
                repeated,
            )
        }
    }

    @Test
    fun manifestMapsOnlyEarnedSwiftUiRelationshipsAndHoldsTheRest() {
        val manifest =
            VexLifeSwiftUiStableRepeaterReference.generate()
                .semanticManifest

        assertTrue(
            manifest.contains(
                "\"itemIdentityOwner\":\"VexLifeConversationContract.messageRef\"",
            ),
        )
        assertTrue(
            manifest.contains(
                "\"relationRef\":\"stable-item-identity\",\"disposition\":\"MAPPED\"",
            ),
        )
        assertTrue(
            manifest.contains(
                "\"relationRef\":\"requested-visible-item\",\"disposition\":\"HELD\"",
            ),
        )
        assertTrue(
            manifest.contains(
                "\"relationRef\":\"visible-item-projection\",\"disposition\":\"HELD\"",
            ),
        )
        assertTrue(
            manifest.contains(
                "\"relationRef\":\"content-type-reuse-hint\",\"disposition\":\"PLATFORM_SPECIFIC\"",
            ),
        )
        assertTrue(
            manifest.contains(
                "\"relationRef\":\"scroll-lock\",\"disposition\":\"HELD\"",
            ),
        )
    }

    @Test
    fun stableItemRefSetPreservesFeatureIdentityWithoutOrderingOwnership() {
        val items =
            listOf(
                VexLifeSwiftUiStableRepeaterReference.ItemCoordinate(
                    itemRef = "message.b",
                    eventRef = "event.b",
                    sequence = 2,
                ),
                VexLifeSwiftUiStableRepeaterReference.ItemCoordinate(
                    itemRef = "message.a",
                    eventRef = "event.a",
                    sequence = 1,
                ),
            )

        assertEquals(
            setOf(
                "message.b",
                "message.a",
            ),
            VexLifeSwiftUiStableRepeaterReference.stableItemRefs(
                items,
            ),
        )
    }
}

// [VXG RealForever]
