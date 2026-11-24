package vexlife.conversation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class VexLifeDirectMessageSemanticRelayTest {
    @Test
    fun humanMessageFormsValidReferenceOnlyUninterpretedRelay() {
        val messageRef =
            "message.self-development.request.test.001"
        val recipientRef =
            DirectConversationIdentity.CompanionRoleRef
        val relay =
            VexLifeDirectMessageSemanticRelay.form(
                messageRef = messageRef,
                originatorRef = "person.vexlife.test",
                originatorKind =
                    VexLifeDirectMessageSemanticRelay.OriginatorKind.HUMAN,
                recipientRefs =
                    listOf(
                        recipientRef,
                    ),
            )

        assertTrue(
            VexLifeSemanticRelayContract
                .validate(
                    relay = relay,
                    sourceMessageRef = messageRef,
                    recipientRefs =
                        listOf(
                            recipientRef,
                        ),
                )
                .ok,
        )
        assertEquals(
            "CANDIDATE",
            relay[
                "interpretationState"
            ]
                ?.jsonPrimitive
                ?.content,
        )
        assertEquals(
            "UNKNOWN",
            relay[
                "ambiguityState"
            ]
                ?.jsonPrimitive
                ?.content,
        )
        assertEquals(
            "NONE",
            relay[
                "targets"
            ]
                ?.jsonArray
                ?.single()
                ?.jsonObject
                ?.get(
                    "projectionMode",
                )
                ?.jsonPrimitive
                ?.content,
        )
        assertEquals(
            messageRef,
            relay[
                "sourceRefs"
            ]
                ?.jsonArray
                ?.single()
                ?.jsonPrimitive
                ?.content,
        )
        assertFalse(
            relay.containsKey(
                "content",
            ),
        )
        assertFalse(
            relay.toString()
                .contains(
                    "Hello from the human",
                ),
        )
    }

    @Test
    fun aiMessageKeepsExactRecipientsWithoutClaimingDeliveryOrUnderstanding() {
        val recipients =
            listOf(
                "person.vexlife.test",
                "role.vex.observer",
            )
        val relay =
            VexLifeDirectMessageSemanticRelay.form(
                messageRef =
                    "message.self-development.response.test.001",
                originatorRef =
                    DirectConversationIdentity.CompanionRoleRef,
                originatorKind =
                    VexLifeDirectMessageSemanticRelay.OriginatorKind.AI,
                recipientRefs = recipients,
            )

        assertEquals(
            recipients,
            relay[
                "recipientRefs"
            ]
                ?.jsonArray
                ?.map {
                    it.jsonPrimitive.content
                },
        )
        relay[
            "targets"
        ]
            ?.jsonArray
            ?.forEach {
                target ->
                assertEquals(
                    "NOT_DELIVERED",
                    target
                        .jsonObject[
                            "deliveryState"
                        ]
                        ?.jsonPrimitive
                        ?.content,
                )
                assertEquals(
                    "NOT_ASSESSED",
                    target
                        .jsonObject[
                            "understandingState"
                        ]
                        ?.jsonPrimitive
                        ?.content,
                )
            }
    }

    @Test
    fun duplicateRecipientsAreRejectedBeforeRelayFormation() {
        var failed =
            false
        try {
            VexLifeDirectMessageSemanticRelay.form(
                messageRef =
                    "message.self-development.request.test.002",
                originatorRef =
                    "person.vexlife.test",
                originatorKind =
                    VexLifeDirectMessageSemanticRelay.OriginatorKind.HUMAN,
                recipientRefs =
                    listOf(
                        DirectConversationIdentity.CompanionRoleRef,
                        DirectConversationIdentity.CompanionRoleRef,
                    ),
            )
        } catch (
            _: IllegalArgumentException,
        ) {
            failed =
                true
        }
        assertTrue(
            failed,
        )
    }
}

// [VXG RealForever]
