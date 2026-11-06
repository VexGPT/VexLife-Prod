package vexlife.conversation

import kotlin.test.Test
import kotlin.test.assertEquals

class DirectConversationIdentityTest {
    @Test
    fun firstPersonalConversationAddressMatchesCurrentEarnedVexLifeRelationship() {
        assertEquals(
            "project.self-development",
            DirectConversationIdentity.ProjectRef,
        )
        assertEquals(
            "thread.self-development.open-conversation",
            DirectConversationIdentity.ThreadRef,
        )
        assertEquals(
            "channel.self-development.companion",
            DirectConversationIdentity.ChannelRef,
        )
    }
}

// [VXG RealForever]
