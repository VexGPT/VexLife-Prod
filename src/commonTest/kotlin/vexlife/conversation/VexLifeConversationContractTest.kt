package vexlife.conversation

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import vexlife.contract.VexContractJson

class VexLifeConversationContractTest {
    private val deviceRef =
        "device.vexlife.00000000-0000-0000-0000-000000000001"
    private val lineageRef =
        "companion-lineage.vexlife.00000000-0000-0000-0000-000000000002"
    private val homeRef =
        "vex-home.$deviceRef"

    private fun request(): VexLifeConversationContract.RequestCoreV1 =
        VexLifeConversationContract.RequestCoreV1(
            schemaVersion = VexLifeConversationContract.EventSchemaV1,
            eventRef = "event.vexlife.request.00000000-0000-0000-0000-000000000003",
            eventKind = VexLifeConversationContract.RequestKind,
            homeRef = homeRef,
            deviceRef = deviceRef,
            companionLineageRef = lineageRef,
            instanceRef = "instance.vexlife-prod.local-service",
            threadRef = DirectConversationIdentity.ThreadRef,
            channelRef = DirectConversationIdentity.ChannelRef,
            turnRef = "turn.vexlife.direct.001",
            messageRef = "message.vexlife.direct.request.001",
            speakerRef = "person.local-user",
            recipientRefs =
                listOf(
                    lineageRef,
                ),
            sequence = 0,
            priorEventHash = null,
            content = "Hello Vex",
            contentHash = "a".repeat(
                64,
            ),
            privacyClass = VexLifeConversationContract.PrivacyClass,
            formedAt = "2026-11-05T12:00:00.000Z",
        )

    @Test
    fun requestPreimageUsesTheCurrentVexLifeKeysWithoutProductVocabulary() {
        val current =
            VexLifeConversationContract.validateRequestCore(
                request(),
            )
        val text =
            VexContractJson.canonicalEncode(
                current,
            )
        val keys =
            Json.parseToJsonElement(
                text,
            )
                .jsonObject
                .keys

        assertEquals(
            setOf(
                "schemaVersion",
                "eventRef",
                "eventKind",
                "homeRef",
                "deviceRef",
                "companionLineageRef",
                "instanceRef",
                "threadRef",
                "channelRef",
                "turnRef",
                "messageRef",
                "speakerRef",
                "recipientRefs",
                "sequence",
                "priorEventHash",
                "content",
                "contentHash",
                "privacyClass",
                "formedAt",
            ),
            keys,
        )
        assertFalse(
            text.contains(
                "projectRef",
            ),
        )
        assertFalse(
            text.contains(
                "eventSha256",
            ),
        )
        assertFalse(
            text.contains(
                "vexlife-prod.direct-conversation",
            ),
        )
    }

    @Test
    fun responseAndHeadPreserveTheSameHomeLineageAndEventChain() {
        val response =
            VexLifeConversationContract.validateResponseCore(
                VexLifeConversationContract.ResponseCoreV1(
                    schemaVersion = VexLifeConversationContract.EventSchemaV1,
                    eventRef = "event.vexlife.response.00000000-0000-0000-0000-000000000004",
                    eventKind = VexLifeConversationContract.ResponseKind,
                    homeRef = homeRef,
                    deviceRef = deviceRef,
                    companionLineageRef = lineageRef,
                    instanceRef = "instance.vexlife-prod.local-service",
                    threadRef = DirectConversationIdentity.ThreadRef,
                    channelRef = DirectConversationIdentity.ChannelRef,
                    turnRef = "turn.vexlife.direct.001",
                    messageRef = "message.vexlife.direct.response.001",
                    speakerRef = lineageRef,
                    recipientRefs =
                        listOf(
                            "person.local-user",
                        ),
                    sequence = 1,
                    priorEventHash = "b".repeat(
                        64,
                    ),
                    content = "Hello Victor",
                    contentHash = "c".repeat(
                        64,
                    ),
                    endpointProfileRef = "endpoint-profile.vexlife.heretic.local",
                    sanitizedEndpointOrigin = "http://127.0.0.1:8080",
                    modelNameOrBoundedTestProfileRef = "qwen3-heretic",
                    privacyClass = VexLifeConversationContract.PrivacyClass,
                    formedAt = "2026-11-05T12:00:01.000Z",
                ),
            )
        val head =
            VexLifeConversationContract.validateHeadCore(
                VexLifeConversationContract.HeadCore(
                    schemaVersion = VexLifeConversationContract.HeadSchema,
                    homeRef = homeRef,
                    deviceRef = deviceRef,
                    companionLineageRef = lineageRef,
                    instanceRef = response.instanceRef,
                    threadRef = response.threadRef,
                    turnRef = response.turnRef,
                    requestMessageRef = "message.vexlife.direct.request.001",
                    responseMessageRef = response.messageRef,
                    eventHash = "d".repeat(
                        64,
                    ),
                    contextSha256 = "e".repeat(
                        64,
                    ),
                    contextPath =
                        "context/$lineageRef/${DirectConversationIdentity.ThreadRef}/${response.turnRef}.json",
                    sequence = response.sequence,
                    priorConversationHeadSha256 = null,
                    formedAt = "2026-11-05T12:00:02.000Z",
                ),
            )

        assertEquals(
            homeRef,
            head.homeRef,
        )
        assertEquals(
            lineageRef,
            head.companionLineageRef,
        )
        assertEquals(
            response.sequence,
            head.sequence,
        )
        assertTrue(
            VexContractJson.canonicalEncode(
                response,
            )
                .contains(
                    "\"sanitizedEndpointOrigin\":\"http://127.0.0.1:8080\"",
                ),
        )
    }

    @Test
    fun prodNamespacedHumanIdentityIsRejected() {
        assertFailsWith<IllegalArgumentException> {
            VexLifeConversationContract.validateRequestCore(
                request()
                    .copy(
                        homeRef = "vex-home.prod.001",
                        deviceRef = "device.vexlife-prod.001",
                        companionLineageRef = "companion-lineage.vexlife-prod.001",
                    ),
            )
        }
    }
}

// [VXG RealForever]
