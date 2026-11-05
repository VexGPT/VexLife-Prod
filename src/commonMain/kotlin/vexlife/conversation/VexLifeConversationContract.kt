package vexlife.conversation

import kotlinx.serialization.Serializable
import vexlife.continuity.VexLifeContinuity

/**
 * Exact portable contract for the lived VexLife direct Conversation event/head chain.
 *
 * These shapes intentionally mirror current VexLife at the bound reference revision. They are not a
 * new VexLife-Prod Conversation vocabulary. JVM persistence may change implementation mechanics only
 * after proving that its bytes, semantic-hash preimages and lineage rules remain compatible.
 */
object VexLifeConversationContract {
    const val EventSchemaV1 =
        VexLifeContinuity.ConversationEventSchemaV1

    const val EventSchemaV2 =
        VexLifeContinuity.ConversationEventSchemaV2

    const val HeadSchema =
        VexLifeContinuity.ConversationHeadSchema

    const val ContextSchema =
        VexLifeContinuity.ConversationContextSchema

    const val PrivacyClass =
        "DEVICE_PRIVATE"

    const val RequestKind =
        "REQUEST"

    const val ResponseKind =
        "RESPONSE"

    @Serializable
    data class RequestCoreV1(
        val schemaVersion: String,
        val eventRef: String,
        val eventKind: String,
        val homeRef: String,
        val deviceRef: String,
        val companionLineageRef: String,
        val instanceRef: String,
        val threadRef: String,
        val channelRef: String,
        val turnRef: String,
        val messageRef: String,
        val speakerRef: String,
        val recipientRefs: List<String>,
        val sequence: Long,
        val priorEventHash: String?,
        val content: String,
        val contentHash: String,
        val privacyClass: String,
        val formedAt: String,
    )

    @Serializable
    data class RequestV1(
        val schemaVersion: String,
        val eventRef: String,
        val eventKind: String,
        val homeRef: String,
        val deviceRef: String,
        val companionLineageRef: String,
        val instanceRef: String,
        val threadRef: String,
        val channelRef: String,
        val turnRef: String,
        val messageRef: String,
        val speakerRef: String,
        val recipientRefs: List<String>,
        val sequence: Long,
        val priorEventHash: String?,
        val content: String,
        val contentHash: String,
        val privacyClass: String,
        val formedAt: String,
        val eventHash: String,
    )

    @Serializable
    data class ResponseCoreV1(
        val schemaVersion: String,
        val eventRef: String,
        val eventKind: String,
        val homeRef: String,
        val deviceRef: String,
        val companionLineageRef: String,
        val instanceRef: String,
        val threadRef: String,
        val channelRef: String,
        val turnRef: String,
        val messageRef: String,
        val speakerRef: String,
        val recipientRefs: List<String>,
        val sequence: Long,
        val priorEventHash: String,
        val content: String,
        val contentHash: String,
        val endpointProfileRef: String,
        val sanitizedEndpointOrigin: String,
        val modelNameOrBoundedTestProfileRef: String,
        val privacyClass: String,
        val formedAt: String,
    )

    @Serializable
    data class ResponseV1(
        val schemaVersion: String,
        val eventRef: String,
        val eventKind: String,
        val homeRef: String,
        val deviceRef: String,
        val companionLineageRef: String,
        val instanceRef: String,
        val threadRef: String,
        val channelRef: String,
        val turnRef: String,
        val messageRef: String,
        val speakerRef: String,
        val recipientRefs: List<String>,
        val sequence: Long,
        val priorEventHash: String,
        val content: String,
        val contentHash: String,
        val endpointProfileRef: String,
        val sanitizedEndpointOrigin: String,
        val modelNameOrBoundedTestProfileRef: String,
        val privacyClass: String,
        val formedAt: String,
        val eventHash: String,
    )

    @Serializable
    data class HeadCore(
        val schemaVersion: String,
        val homeRef: String,
        val deviceRef: String,
        val companionLineageRef: String,
        val instanceRef: String,
        val threadRef: String,
        val turnRef: String,
        val requestMessageRef: String,
        val responseMessageRef: String,
        val eventHash: String,
        val contextSha256: String,
        val contextPath: String,
        val sequence: Long,
        val priorConversationHeadSha256: String?,
        val formedAt: String,
    )

    @Serializable
    data class Head(
        val schemaVersion: String,
        val homeRef: String,
        val deviceRef: String,
        val companionLineageRef: String,
        val instanceRef: String,
        val threadRef: String,
        val turnRef: String,
        val requestMessageRef: String,
        val responseMessageRef: String,
        val eventHash: String,
        val contextSha256: String,
        val contextPath: String,
        val sequence: Long,
        val priorConversationHeadSha256: String?,
        val formedAt: String,
        val conversationHeadSha256: String,
    )

    fun validateRequestCore(
        value: RequestCoreV1,
    ): RequestCoreV1 {
        require(
            value.schemaVersion ==
                EventSchemaV1 &&
                value.eventKind ==
                RequestKind &&
                value.privacyClass ==
                PrivacyClass &&
                value.content.isNotEmpty() &&
                value.sequence >=
                0 &&
                value.sequence %
                2 ==
                0L,
        ) {
            "canonical VexLife request event semantics are invalid"
        }
        validateIdentity(
            homeRef = value.homeRef,
            deviceRef = value.deviceRef,
            companionLineageRef = value.companionLineageRef,
        )
        requireRefs(
            value.eventRef,
            value.instanceRef,
            value.threadRef,
            value.channelRef,
            value.turnRef,
            value.messageRef,
            value.speakerRef,
            *value.recipientRefs.toTypedArray(),
        )
        requireSha256(
            value.contentHash,
            "contentHash",
        )
        value.priorEventHash
            ?.let {
                requireSha256(
                    it,
                    "priorEventHash",
                )
            }

        return value
    }

    fun validateResponseCore(
        value: ResponseCoreV1,
    ): ResponseCoreV1 {
        require(
            value.schemaVersion ==
                EventSchemaV1 &&
                value.eventKind ==
                ResponseKind &&
                value.privacyClass ==
                PrivacyClass &&
                value.content.isNotEmpty() &&
                value.sequence >=
                1 &&
                value.sequence %
                2 ==
                1L,
        ) {
            "canonical VexLife response event semantics are invalid"
        }
        validateIdentity(
            homeRef = value.homeRef,
            deviceRef = value.deviceRef,
            companionLineageRef = value.companionLineageRef,
        )
        requireRefs(
            value.eventRef,
            value.instanceRef,
            value.threadRef,
            value.channelRef,
            value.turnRef,
            value.messageRef,
            value.speakerRef,
            value.endpointProfileRef,
            value.modelNameOrBoundedTestProfileRef,
            *value.recipientRefs.toTypedArray(),
        )
        requireSha256(
            value.priorEventHash,
            "priorEventHash",
        )
        requireSha256(
            value.contentHash,
            "contentHash",
        )
        require(
            value.sanitizedEndpointOrigin.startsWith(
                "http://127.0.0.1:",
            ) ||
                value.sanitizedEndpointOrigin.startsWith(
                    "http://[::1]:",
                ),
        ) {
            "canonical VexLife response endpoint must remain numeric loopback"
        }

        return value
    }

    fun validateHeadCore(
        value: HeadCore,
    ): HeadCore {
        require(
            value.schemaVersion ==
                HeadSchema &&
                value.sequence >=
                1 &&
                value.sequence %
                2 ==
                1L &&
                value.contextPath.isNotEmpty() &&
                value.formedAt.isNotEmpty(),
        ) {
            "canonical VexLife Conversation head semantics are invalid"
        }
        validateIdentity(
            homeRef = value.homeRef,
            deviceRef = value.deviceRef,
            companionLineageRef = value.companionLineageRef,
        )
        requireRefs(
            value.instanceRef,
            value.threadRef,
            value.turnRef,
            value.requestMessageRef,
            value.responseMessageRef,
        )
        requireSha256(
            value.eventHash,
            "eventHash",
        )
        requireSha256(
            value.contextSha256,
            "contextSha256",
        )
        value.priorConversationHeadSha256
            ?.let {
                requireSha256(
                    it,
                    "priorConversationHeadSha256",
                )
            }

        return value
    }

    private fun validateIdentity(
        homeRef: String,
        deviceRef: String,
        companionLineageRef: String,
    ) {
        require(
            deviceRef.startsWith(
                "device.vexlife.",
            ) &&
                companionLineageRef.startsWith(
                    "companion-lineage.vexlife.",
                ) &&
                homeRef ==
                "vex-home.$deviceRef",
        ) {
            "canonical VexLife Conversation identity is not bound to the admitted Home/device/lineage"
        }
    }

    private fun requireRefs(
        vararg values: String,
    ) {
        require(
            values.isNotEmpty() &&
                values.all {
                    value ->
                    value.matches(
                        Regex(
                            "[A-Za-z0-9][A-Za-z0-9._:/-]*",
                        ),
                    )
                },
        ) {
            "canonical VexLife Conversation refs must remain portable and non-empty"
        }
    }

    private fun requireSha256(
        value: String,
        label: String,
    ) {
        require(
            value.matches(
                Regex(
                    "[0-9a-f]{64}",
                ),
            ),
        ) {
            "$label must be one lowercase SHA-256"
        }
    }
}

// [VXG RealForever]
