package vexlife.conversation

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import vexlife.continuity.VexLifeContinuity

/**
 * Exact portable contract for the lived VexLife direct Conversation event/head/context chain.
 *
 * These shapes mirror current VexLife at [VexLifeContinuity.ReferenceRevision]. They are not a new
 * VexLife-Prod Conversation vocabulary. JVM persistence may replace implementation mechanics only
 * while these durable identities, semantic-hash preimages and causal bindings remain compatible.
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

    /**
     * Canonical bounded context. The nested prompt receipt is intentionally represented as JSON:
     * current VexLife owns the outer context contract while prompt-materialization owners may evolve
     * their own exact receipt schema without making Conversation re-own prompt semantics.
     */
    @Serializable
    data class ContextCore(
        val schemaVersion: String,
        val homeRef: String,
        val deviceRef: String,
        val companionLineageRef: String,
        val instanceRef: String,
        val threadRef: String,
        val turnRef: String,
        val contextSourceRefs: List<String>,
        val requestEventHash: String,
        val responseEventHash: String,
        val promptContextMaterializationReceipt: JsonObject? = null,
        val privacyClass: String,
        val formedAt: String,
    )

    @Serializable
    data class Context(
        val schemaVersion: String,
        val homeRef: String,
        val deviceRef: String,
        val companionLineageRef: String,
        val instanceRef: String,
        val threadRef: String,
        val turnRef: String,
        val contextSourceRefs: List<String>,
        val requestEventHash: String,
        val responseEventHash: String,
        val promptContextMaterializationReceipt: JsonObject? = null,
        val privacyClass: String,
        val formedAt: String,
        val serializedContextSha256: String,
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
            value.schemaVersion == EventSchemaV1 &&
                value.eventKind == RequestKind &&
                value.privacyClass == PrivacyClass &&
                value.content.isNotEmpty() &&
                value.sequence >= 0,
        ) {
            "canonical VexLife request event semantics are invalid"
        }
        validateIdentity(
            homeRef = value.homeRef,
            deviceRef = value.deviceRef,
            companionLineageRef = value.companionLineageRef,
        )
        requireNonEmpty(
            value.eventRef,
            value.channelRef,
            value.messageRef,
            value.speakerRef,
            *value.recipientRefs.toTypedArray(),
        )
        requireSafeRefs(
            value.instanceRef,
            value.threadRef,
            value.turnRef,
        )
        requireSha256(
            value.contentHash,
            "contentHash",
        )
        value.priorEventHash?.let {
            requireSha256(
                it,
                "priorEventHash",
            )
        }
        require(
            value.formedAt.isNotEmpty(),
        ) {
            "canonical VexLife request event requires one formedAt value"
        }

        return value
    }

    fun validateResponseCore(
        value: ResponseCoreV1,
    ): ResponseCoreV1 {
        require(
            value.schemaVersion == EventSchemaV1 &&
                value.eventKind == ResponseKind &&
                value.privacyClass == PrivacyClass &&
                value.content.isNotEmpty() &&
                value.sequence >= 1,
        ) {
            "canonical VexLife response event semantics are invalid"
        }
        validateIdentity(
            homeRef = value.homeRef,
            deviceRef = value.deviceRef,
            companionLineageRef = value.companionLineageRef,
        )
        requireNonEmpty(
            value.eventRef,
            value.channelRef,
            value.messageRef,
            value.speakerRef,
            value.endpointProfileRef,
            value.modelNameOrBoundedTestProfileRef,
            *value.recipientRefs.toTypedArray(),
        )
        requireSafeRefs(
            value.instanceRef,
            value.threadRef,
            value.turnRef,
        )
        requireSha256(
            value.priorEventHash,
            "priorEventHash",
        )
        requireSha256(
            value.contentHash,
            "contentHash",
        )
        requireNumericLoopbackOrigin(
            value.sanitizedEndpointOrigin,
        )
        require(
            value.formedAt.isNotEmpty(),
        ) {
            "canonical VexLife response event requires one formedAt value"
        }

        return value
    }

    fun validateContextCore(
        value: ContextCore,
    ): ContextCore {
        require(
            value.schemaVersion == ContextSchema &&
                value.privacyClass == PrivacyClass &&
                value.contextSourceRefs.size >= 2 &&
                value.formedAt.isNotEmpty(),
        ) {
            "canonical VexLife bounded context semantics are invalid"
        }
        validateIdentity(
            homeRef = value.homeRef,
            deviceRef = value.deviceRef,
            companionLineageRef = value.companionLineageRef,
        )
        requireSafeRefs(
            value.instanceRef,
            value.threadRef,
            value.turnRef,
        )
        requireNonEmpty(
            *value.contextSourceRefs.toTypedArray(),
        )
        requireSha256(
            value.requestEventHash,
            "requestEventHash",
        )
        requireSha256(
            value.responseEventHash,
            "responseEventHash",
        )

        return value
    }

    fun validateContext(
        value: Context,
    ): Context {
        validateContextCore(
            ContextCore(
                schemaVersion = value.schemaVersion,
                homeRef = value.homeRef,
                deviceRef = value.deviceRef,
                companionLineageRef = value.companionLineageRef,
                instanceRef = value.instanceRef,
                threadRef = value.threadRef,
                turnRef = value.turnRef,
                contextSourceRefs = value.contextSourceRefs,
                requestEventHash = value.requestEventHash,
                responseEventHash = value.responseEventHash,
                promptContextMaterializationReceipt = value.promptContextMaterializationReceipt,
                privacyClass = value.privacyClass,
                formedAt = value.formedAt,
            ),
        )
        requireSha256(
            value.serializedContextSha256,
            "serializedContextSha256",
        )

        return value
    }

    fun validateHeadCore(
        value: HeadCore,
    ): HeadCore {
        require(
            value.schemaVersion == HeadSchema &&
                value.sequence >= 1 &&
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
        requireSafeRefs(
            value.instanceRef,
            value.threadRef,
            value.turnRef,
        )
        requireNonEmpty(
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
        value.priorConversationHeadSha256?.let {
            requireSha256(
                it,
                "priorConversationHeadSha256",
            )
        }

        return value
    }

    fun validateHead(
        value: Head,
    ): Head {
        validateHeadCore(
            HeadCore(
                schemaVersion = value.schemaVersion,
                homeRef = value.homeRef,
                deviceRef = value.deviceRef,
                companionLineageRef = value.companionLineageRef,
                instanceRef = value.instanceRef,
                threadRef = value.threadRef,
                turnRef = value.turnRef,
                requestMessageRef = value.requestMessageRef,
                responseMessageRef = value.responseMessageRef,
                eventHash = value.eventHash,
                contextSha256 = value.contextSha256,
                contextPath = value.contextPath,
                sequence = value.sequence,
                priorConversationHeadSha256 = value.priorConversationHeadSha256,
                formedAt = value.formedAt,
            ),
        )
        requireSha256(
            value.conversationHeadSha256,
            "conversationHeadSha256",
        )

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

    private fun requireSafeRefs(
        vararg values: String,
    ) {
        require(
            values.isNotEmpty() &&
                values.all {
                    value ->
                    value.matches(
                        Regex(
                            "[a-z0-9](?:[a-z0-9._-]{0,126}[a-z0-9])?",
                        ),
                    )
                },
        ) {
            "canonical VexLife safe refs must remain lowercase portable path segments"
        }
    }

    private fun requireNonEmpty(
        vararg values: String,
    ) {
        require(
            values.isNotEmpty() &&
                values.all(
                    String::isNotEmpty,
                ),
        ) {
            "canonical VexLife Conversation reference strings must be non-empty"
        }
    }

    private fun requireNumericLoopbackOrigin(
        value: String,
    ) {
        require(
            value.matches(
                Regex(
                    "http://(?:127\\.0\\.0\\.1|\\[::1]):[0-9]+",
                ),
            ),
        ) {
            "canonical VexLife response endpoint must remain one numeric-loopback HTTP origin"
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
