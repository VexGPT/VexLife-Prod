package vexlife.continuity

/**
 * Portable identity and coordinate map for the lived VexLife continuity that New Beginnings must
 * preserve while replacing implementation machinery.
 *
 * This object owns no filesystem, provider, Memory-selection, Conversation-write, or presentation
 * effect. It makes the already-earned relationships addressable so those owners cannot be silently
 * replaced by a repository/product-name change.
 */
object VexLifeContinuity {
    const val ReferenceRepository =
        "vgong24/VexLife"

    const val ReferenceRevision =
        "7d59753687ff12726b0d309ee2dde028b5f89e8a"

    const val CanonicalMacHomeRelativeRoot =
        ".vexlife"

    const val HomeManifestRelativePath =
        "config/home.json"

    const val DeviceDirectoryRelativePath =
        "devices"

    const val ConversationRootRelativePath =
        "conversations"

    const val ContextRootRelativePath =
        "context"

    const val ScoreRootRelativePath =
        "score"

    const val DailyMemoryRootRelativePath =
        "daily-memory-dream"

    const val ProductRuntimeRootRelativePath =
        "runtime/vexlife-prod"

    const val ProductModelBindingRelativePath =
        "$ProductRuntimeRootRelativePath/model-binding.json"

    const val ProductServiceRootRelativePath =
        "runtime/product-service"

    const val HomeSchema =
        "vexlife.home/v0"

    const val DeviceInstallationSchema =
        "vexlife.device-installation/v0"

    const val ConversationEventSchemaV1 =
        "vexlife.lived-companion-event/v1"

    const val ConversationEventSchemaV2 =
        "vexlife.lived-companion-event/v2"

    const val ConversationHeadSchema =
        "vexlife.lived-companion-head/v1"

    const val ConversationContextSchema =
        "vexlife.lived-companion-context/v1"

    const val ScoreContextContract =
        "contract.multivex.score.live-semantic-acceptance.v1"

    const val DailyMemoryContract =
        "contract.multivex.g03.daily-memory-only-dream/v1"

    const val LivingJournalMemoryProjectionSchema =
        "vexlife.living-journal.memory-projection/v1"

    const val PromptMemoryPacketSchema =
        "vexlife.prompt-context-memory-packet/v1"

    const val PromptSelectionSchema =
        "vexlife.browser-prompt-context-runtime-selection/v3"

    data class HomeIdentity(
        val homeRef: String,
        val familyRef: String,
        val deviceRef: String,
        val companionLineageRef: String,
    ) {
        init {
            requirePortableRef(
                homeRef,
                "homeRef",
            )
            requirePortableRef(
                familyRef,
                "familyRef",
            )
            requirePortableRef(
                deviceRef,
                "deviceRef",
            )
            requirePortableRef(
                companionLineageRef,
                "companionLineageRef",
            )
            require(
                deviceRef.startsWith(
                    "device.vexlife.",
                ),
            ) {
                "deviceRef must preserve the canonical VexLife device namespace"
            }
            require(
                companionLineageRef.startsWith(
                    "companion-lineage.vexlife.",
                ),
            ) {
                "companionLineageRef must preserve the canonical VexLife lineage namespace"
            }
            require(
                homeRef ==
                    "vex-home.$deviceRef",
            ) {
                "homeRef must remain the canonical VexLife Home identity derived from the current device"
            }
        }
    }

    data class ThreadCoordinates(
        val homeManifest: String,
        val deviceRecord: String,
        val conversationRoot: String,
        val conversationEvents: String,
        val conversationHeads: String,
        val conversationHead: String,
        val contextRoot: String,
        val scoreRoot: String,
        val scoreHead: String,
        val dailyMemoryRoot: String,
        val dailyMemoryHead: String,
        val productRuntimeRoot: String,
        val productModelBinding: String,
        val productServiceRoot: String,
    )

    fun coordinates(
        identity: HomeIdentity,
        threadRef: String,
    ): ThreadCoordinates {
        requirePortableRef(
            threadRef,
            "threadRef",
        )

        val lineage =
            identity.companionLineageRef
        val conversation =
            "$ConversationRootRelativePath/$lineage/$threadRef"
        val context =
            "$ContextRootRelativePath/$lineage/$threadRef"
        val score =
            "$ScoreRootRelativePath/$lineage/$threadRef"
        val daily =
            "$DailyMemoryRootRelativePath/$lineage/$threadRef"

        return ThreadCoordinates(
            homeManifest = HomeManifestRelativePath,
            deviceRecord = "$DeviceDirectoryRelativePath/${identity.deviceRef}.json",
            conversationRoot = conversation,
            conversationEvents = "$conversation/events",
            conversationHeads = "$conversation/heads",
            conversationHead = "$conversation/head.json",
            contextRoot = context,
            scoreRoot = score,
            scoreHead = "$score/head.json",
            dailyMemoryRoot = daily,
            dailyMemoryHead = "$daily/head.json",
            productRuntimeRoot = ProductRuntimeRootRelativePath,
            productModelBinding = ProductModelBindingRelativePath,
            productServiceRoot = ProductServiceRootRelativePath,
        )
    }

    private fun requirePortableRef(
        value: String,
        label: String,
    ) {
        require(
            value.matches(
                Regex(
                    "[a-z0-9](?:[a-z0-9._-]{0,220}[a-z0-9])?",
                ),
            ),
        ) {
            "$label must be one lowercase portable ref"
        }
    }
}

// [VXG RealForever]
