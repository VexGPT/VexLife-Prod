package vexlife.capability

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Instant
import kotlinx.serialization.Serializable
import vexlife.contract.VexContractJson

/**
 * Product-owned Capability Atlas + compact self-frame for the current VexLife-Prod direct path.
 *
 * A frame reports current capability truth. It grants no execution authority and owns no effect.
 * Availability is the intersection of registered capability, active role, platform, project grant,
 * permission state and resource state.
 */
object VexLifeCapabilityAtlas {
    const val RegistrySchema =
        "vexlife-prod.capability-registry/v1"

    const val FrameSchema =
        "vexlife-prod.self-capability-frame/v1"

    const val CompanionRoleRef =
        "role.vex.companion"

    const val MacPlatformRef =
        "platform.macos"

    const val PermissionNone =
        "permission.none"

    enum class Stage {
        DISCOVERABLE,
        EXPLAINABLE,
        REQUESTABLE,
        ADMITTED,
        EXECUTABLE,
        COMPLETED,
    }

    enum class FrameState {
        AVAILABLE,
        HELD,
        UNAVAILABLE,
        UNKNOWN,
    }

    enum class PermissionState {
        GRANTED,
        DENIED,
        UNKNOWN,
    }

    enum class ResourceState {
        AVAILABLE,
        UNAVAILABLE,
        UNKNOWN,
    }

    @Serializable
    data class Definition(
        val capabilityRef: String,
        val purpose: String,
        val defaultStage: Stage,
        val roleRefs: List<String>,
        val platformRefs: List<String>,
        val permissionRef: String,
        val resourceRef: String,
        val effectClass: String,
        val resourceClass: String,
        val modelParticipationClass: String,
        val sourcePath: String,
        val priority: Int,
    )

    @Serializable
    private data class RegistryCore(
        val schemaVersion: String,
        val truthClass: String,
        val definitions: List<Definition>,
        val effectAuthorityGranted: Boolean,
    )

    @Serializable
    data class Registry(
        val schemaVersion: String,
        val truthClass: String,
        val definitions: List<Definition>,
        val effectAuthorityGranted: Boolean,
        val registryRef: String,
        val semanticFingerprint: String,
    )

    @Serializable
    data class PermissionFact(
        val permissionRef: String,
        val state: PermissionState,
        val sourceRefs: List<String>,
    )

    @Serializable
    data class ResourceFact(
        val resourceRef: String,
        val state: ResourceState,
        val sourceRefs: List<String>,
    )

    @Serializable
    data class CurrentContext(
        val homeRef: String,
        val companionLineageRef: String,
        val threadRef: String,
        val currentRequestEventRef: String,
        val promptReceiptRefOrNull: String? =
            null,
        val memoryContextRefOrNull: String? =
            null,
        val browserContextRefOrNull: String? =
            null,
        val workingContinuityContextRefOrNull: String? =
            null,
        val runtimeObservationRefOrNull: String? =
            null,
    )

    data class FrameInput(
        val activeRoleRef: String,
        val platformRef: String,
        val projectGrantedCapabilityRefs: List<String>,
        val relevantCapabilityRefs: List<String>,
        val permissionFacts: List<PermissionFact>,
        val resourceFacts: List<ResourceFact>,
        val currentContext: CurrentContext,
        val sourceRefs: List<String>,
        val tokenBudget: Int =
            1_200,
        val formedAt: Instant =
            Instant.now(),
    )

    @Serializable
    data class Entry(
        val capabilityRef: String,
        val purpose: String,
        val state: FrameState,
        val reasonCodes: List<String>,
        val canonicalStage: Stage,
        val permissionRef: String,
        val permissionState: PermissionState,
        val resourceRef: String,
        val resourceState: ResourceState,
        val effectClass: String,
        val resourceClass: String,
        val modelParticipationClass: String,
        val sourceRefs: List<String>,
        val effectAuthorityGranted: Boolean,
    )

    @Serializable
    data class HeldEntry(
        val capabilityRef: String,
        val reasonCodes: List<String>,
        val permissionRef: String,
        val resourceRef: String,
    )

    @Serializable
    data class Coverage(
        val tokenBudget: Int,
        val usedTokens: Int,
        val omittedCapabilityRefs: List<String>,
        val omissionReasons: List<String>,
        val truncated: Boolean,
    )

    @Serializable
    private data class FrameBudgetBase(
        val schemaVersion: String,
        val registryRef: String,
        val activeRoleRef: String,
        val platformRef: String,
        val currentContext: CurrentContext,
    )

    @Serializable
    private data class FrameCore(
        val schemaVersion: String,
        val truthClass: String,
        val registryRef: String,
        val registryFingerprint: String,
        val activeRoleRef: String,
        val platformRef: String,
        val currentContext: CurrentContext,
        val capabilityEntries: List<Entry>,
        val availableCapabilityRefs: List<String>,
        val heldCapabilityEntries: List<HeldEntry>,
        val unavailableCapabilityRefs: List<String>,
        val unknownCapabilityRefs: List<String>,
        val sourceRefs: List<String>,
        val coverage: Coverage,
        val effectAuthorityGranted: Boolean,
        val formedAt: String,
    )

    @Serializable
    data class Frame(
        val schemaVersion: String,
        val truthClass: String,
        val registryRef: String,
        val registryFingerprint: String,
        val activeRoleRef: String,
        val platformRef: String,
        val currentContext: CurrentContext,
        val capabilityEntries: List<Entry>,
        val availableCapabilityRefs: List<String>,
        val heldCapabilityEntries: List<HeldEntry>,
        val unavailableCapabilityRefs: List<String>,
        val unknownCapabilityRefs: List<String>,
        val sourceRefs: List<String>,
        val coverage: Coverage,
        val effectAuthorityGranted: Boolean,
        val formedAt: String,
        val selfCapabilityFrameRef: String,
        val semanticFingerprint: String,
    )

    private val Definitions =
        listOf(
            Definition(
                capabilityRef =
                    "capability.vexlife.context.memory-current",
                purpose =
                    "Use the exact current source-attributed Memory context already admitted by VexLifeCurrentMemoryReader without creating semantic acceptance or Memory mutation.",
                defaultStage =
                    Stage.EXECUTABLE,
                roleRefs =
                    listOf(
                        CompanionRoleRef,
                    ),
                platformRefs =
                    listOf(
                        MacPlatformRef,
                    ),
                permissionRef =
                    PermissionNone,
                resourceRef =
                    "resource.vexlife-prod.memory.current-context",
                effectClass =
                    "READ_ONLY_CONTEXT",
                resourceClass =
                    "MEMORY_CONTEXT",
                modelParticipationClass =
                    "SYSTEM_CONTEXT",
                sourcePath =
                    "src/jvmMain/kotlin/vexlife/memory/VexLifeCurrentMemoryReader.kt",
                priority =
                    10,
            ),
            Definition(
                capabilityRef =
                    "capability.vexlife.context.working-continuity",
                purpose =
                    "Use exact owner-current reference-only working coordinates carried across context rollover without treating them as semantic Memory or replayed Conversation text.",
                defaultStage =
                    Stage.EXECUTABLE,
                roleRefs =
                    listOf(
                        CompanionRoleRef,
                    ),
                platformRefs =
                    listOf(
                        MacPlatformRef,
                    ),
                permissionRef =
                    PermissionNone,
                resourceRef =
                    "resource.vexlife-prod.working-continuity.current-successor",
                effectClass =
                    "READ_ONLY_CONTEXT",
                resourceClass =
                    "WORKING_CONTINUITY_CONTEXT",
                modelParticipationClass =
                    "WORKING_CONTINUITY_CONTEXT",
                sourcePath =
                    "src/jvmMain/kotlin/vexlife/companion/DirectWorkingContinuity.kt",
                priority =
                    20,
            ),
            Definition(
                capabilityRef =
                    "capability.vexlife.context.browser-current",
                purpose =
                    "Use already-captured current admitted Vex-owned browser page text as untrusted reference material without claiming navigation or general-web authority.",
                defaultStage =
                    Stage.EXECUTABLE,
                roleRefs =
                    listOf(
                        CompanionRoleRef,
                    ),
                platformRefs =
                    listOf(
                        MacPlatformRef,
                    ),
                permissionRef =
                    PermissionNone,
                resourceRef =
                    "resource.vexlife-prod.browser.current-context",
                effectClass =
                    "READ_ONLY_CONTEXT",
                resourceClass =
                    "BROWSER_CONTEXT",
                modelParticipationClass =
                    "HUMAN_SUPPLIED_REFERENCE_CONTEXT",
                sourcePath =
                    "src/commonMain/kotlin/vexlife/browser/VexLifeProdBrowserContract.kt",
                priority =
                    30,
            ),
            Definition(
                capabilityRef =
                    "capability.vexlife.companion.local-inference",
                purpose =
                    "Participate in one direct local Companion inference through an already-qualified loopback Heretic runtime while provider invocation remains DirectCompanionConnection-owned.",
                defaultStage =
                    Stage.EXECUTABLE,
                roleRefs =
                    listOf(
                        CompanionRoleRef,
                    ),
                platformRefs =
                    listOf(
                        MacPlatformRef,
                    ),
                permissionRef =
                    PermissionNone,
                resourceRef =
                    "resource.vexlife-prod.heretic.qualified-runtime",
                effectClass =
                    "LOCAL_MODEL_INFERENCE",
                resourceClass =
                    "QUALIFIED_LOCAL_MODEL_RUNTIME",
                modelParticipationClass =
                    "CURRENT_MODEL_RUNTIME",
                sourcePath =
                    "src/jvmMain/kotlin/vexlife/companion/DirectCompanionConnection.kt",
                priority =
                    5,
            ),
        )
            .sortedBy {
                it.capabilityRef
            }

    fun registry(): Registry {
        Definitions.forEach(
            ::validateDefinition,
        )
        val core =
            RegistryCore(
                schemaVersion =
                    RegistrySchema,
                truthClass =
                    "PRODUCT_OWNED_CAPABILITY_REGISTRY",
                definitions =
                    Definitions,
                effectAuthorityGranted =
                    false,
            )
        val fingerprint =
            semanticHash(
                core,
            )
        return Registry(
            schemaVersion =
                core.schemaVersion,
            truthClass =
                core.truthClass,
            definitions =
                core.definitions,
            effectAuthorityGranted =
                core.effectAuthorityGranted,
            registryRef =
                "registry.vexlife-prod.capabilities." +
                    fingerprint.take(
                        32,
                    ),
            semanticFingerprint =
                fingerprint,
        )
    }

    fun describe(
        capabilityRef: String,
    ): Definition {
        requireRef(
            capabilityRef,
            "capabilityRef",
        )
        return requireNotNull(
            Definitions.firstOrNull {
                it.capabilityRef ==
                    capabilityRef
            },
        ) {
            "capability is not registered in VexLife-Prod"
        }
    }

    fun formFrame(
        input: FrameInput,
    ): Frame {
        requireRef(
            input.activeRoleRef,
            "activeRoleRef",
        )
        requireRef(
            input.platformRef,
            "platformRef",
        )
        validateCurrentContext(
            input.currentContext,
        )
        require(
            input.tokenBudget >=
                256,
        ) {
            "self-capability frame tokenBudget must be >= 256"
        }

        val grants =
            canonicalRefs(
                input.projectGrantedCapabilityRefs,
                "projectGrantedCapabilityRefs",
            )
                .toSet()
        val relevant =
            canonicalRefs(
                input.relevantCapabilityRefs,
                "relevantCapabilityRefs",
            )
        require(
            relevant.isNotEmpty(),
        ) {
            "self-capability frame requires at least one relevant registered capability"
        }
        val definitionsByRef =
            Definitions.associateBy {
                it.capabilityRef
            }
        relevant.forEach {
            require(
                definitionsByRef.containsKey(
                    it,
                ),
            ) {
                "self-capability frame relevant capability is not registered: $it"
            }
        }

        val permissionFacts =
            canonicalPermissionFacts(
                input.permissionFacts,
            )
        val resourceFacts =
            canonicalResourceFacts(
                input.resourceFacts,
            )
        val baseSourceRefs =
            canonicalRefs(
                input.sourceRefs,
                "sourceRefs",
            )

        val candidates =
            relevant
                .map {
                    definitionsByRef
                        .getValue(
                            it,
                        )
                }
                .sortedWith(
                    compareBy<Definition> {
                        it.priority
                    }
                        .thenBy {
                            it.capabilityRef
                        },
                )
                .map {
                    definition ->
                    formEntry(
                        definition =
                            definition,
                        activeRoleRef =
                            input.activeRoleRef,
                        platformRef =
                            input.platformRef,
                        projectGranted =
                            grants.contains(
                                definition.capabilityRef,
                            ),
                        permissionFact =
                            permissionFacts[
                                definition.permissionRef
                            ],
                        resourceFact =
                            resourceFacts[
                                definition.resourceRef
                            ],
                        baseSourceRefs =
                            baseSourceRefs,
                    )
                }

        val registry =
            registry()
        val formedAt =
            input.formedAt.toString()
        val baseTokenCost =
            estimatedTokens(
                FrameBudgetBase(
                    schemaVersion =
                        FrameSchema,
                    registryRef =
                        registry.registryRef,
                    activeRoleRef =
                        input.activeRoleRef,
                    platformRef =
                        input.platformRef,
                    currentContext =
                        input.currentContext,
                ),
            )

        require(
            baseTokenCost <=
                input.tokenBudget,
        ) {
            "self-capability frame tokenBudget is too small for the frame header/current context"
        }

        var usedTokens =
            baseTokenCost
        val selected =
            mutableListOf<Entry>()
        val omitted =
            mutableListOf<String>()

        candidates.forEach {
            entry ->
            val cost =
                estimatedTokens(
                    entry,
                )
            if (
                usedTokens +
                    cost <=
                input.tokenBudget
            ) {
                selected +=
                    entry
                usedTokens +=
                    cost
            } else {
                omitted +=
                    entry.capabilityRef
            }
        }

        val sortedSelected =
            selected.sortedBy {
                it.capabilityRef
            }
        val sourceRefs =
            (
                baseSourceRefs +
                    sortedSelected.flatMap {
                        it.sourceRefs
                    } +
                    listOf(
                        registry.registryRef,
                    )
            )
                .distinct()
                .sorted()
        val coverage =
            Coverage(
                tokenBudget =
                    input.tokenBudget,
                usedTokens =
                    usedTokens,
                omittedCapabilityRefs =
                    omitted.sorted(),
                omissionReasons =
                    omitted
                        .sorted()
                        .map {
                            "TOKEN_BUDGET"
                        },
                truncated =
                    omitted.isNotEmpty(),
            )
        val core =
            FrameCore(
                schemaVersion =
                    FrameSchema,
                truthClass =
                    "BOUNDED_SOURCE_BOUND_SELF_CAPABILITY_FRAME",
                registryRef =
                    registry.registryRef,
                registryFingerprint =
                    registry.semanticFingerprint,
                activeRoleRef =
                    input.activeRoleRef,
                platformRef =
                    input.platformRef,
                currentContext =
                    input.currentContext,
                capabilityEntries =
                    sortedSelected,
                availableCapabilityRefs =
                    refsInState(
                        sortedSelected,
                        FrameState.AVAILABLE,
                    ),
                heldCapabilityEntries =
                    sortedSelected
                        .filter {
                            it.state ==
                                FrameState.HELD
                        }
                        .map {
                            HeldEntry(
                                capabilityRef =
                                    it.capabilityRef,
                                reasonCodes =
                                    it.reasonCodes,
                                permissionRef =
                                    it.permissionRef,
                                resourceRef =
                                    it.resourceRef,
                            )
                        },
                unavailableCapabilityRefs =
                    refsInState(
                        sortedSelected,
                        FrameState.UNAVAILABLE,
                    ),
                unknownCapabilityRefs =
                    refsInState(
                        sortedSelected,
                        FrameState.UNKNOWN,
                    ),
                sourceRefs =
                    sourceRefs,
                coverage =
                    coverage,
                effectAuthorityGranted =
                    false,
                formedAt =
                    formedAt,
            )
        val fingerprint =
            semanticHash(
                core,
            )

        return Frame(
            schemaVersion =
                core.schemaVersion,
            truthClass =
                core.truthClass,
            registryRef =
                core.registryRef,
            registryFingerprint =
                core.registryFingerprint,
            activeRoleRef =
                core.activeRoleRef,
            platformRef =
                core.platformRef,
            currentContext =
                core.currentContext,
            capabilityEntries =
                core.capabilityEntries,
            availableCapabilityRefs =
                core.availableCapabilityRefs,
            heldCapabilityEntries =
                core.heldCapabilityEntries,
            unavailableCapabilityRefs =
                core.unavailableCapabilityRefs,
            unknownCapabilityRefs =
                core.unknownCapabilityRefs,
            sourceRefs =
                core.sourceRefs,
            coverage =
                core.coverage,
            effectAuthorityGranted =
                core.effectAuthorityGranted,
            formedAt =
                core.formedAt,
            selfCapabilityFrameRef =
                "frame.vexlife-prod.self-capability." +
                    fingerprint.take(
                        32,
                    ),
            semanticFingerprint =
                fingerprint,
        )
    }

    private fun formEntry(
        definition: Definition,
        activeRoleRef: String,
        platformRef: String,
        projectGranted: Boolean,
        permissionFact: PermissionFact?,
        resourceFact: ResourceFact?,
        baseSourceRefs: List<String>,
    ): Entry {
        val roleAdmitted =
            definition.roleRefs.contains(
                activeRoleRef,
            )
        val platformAdmitted =
            definition.platformRefs.contains(
                platformRef,
            )
        val permissionState =
            if (
                definition.permissionRef ==
                    PermissionNone
            ) {
                PermissionState.GRANTED
            } else {
                permissionFact
                    ?.state
                    ?: PermissionState.UNKNOWN
            }
        val resourceState =
            resourceFact
                ?.state
                ?: ResourceState.UNKNOWN

        val reasons =
            mutableListOf<String>()
        val state =
            when {
                !roleAdmitted -> {
                    reasons +=
                        "ROLE_NOT_ADMITTED"
                    FrameState.UNAVAILABLE
                }

                !platformAdmitted -> {
                    reasons +=
                        "PLATFORM_NOT_ADMITTED"
                    FrameState.UNAVAILABLE
                }

                !projectGranted -> {
                    reasons +=
                        "PROJECT_GRANT_HELD"
                    FrameState.HELD
                }

                permissionState ==
                    PermissionState.DENIED -> {
                    reasons +=
                        "PERMISSION_DENIED"
                    FrameState.HELD
                }

                permissionState ==
                    PermissionState.UNKNOWN -> {
                    reasons +=
                        "PERMISSION_UNKNOWN"
                    FrameState.UNKNOWN
                }

                resourceState ==
                    ResourceState.UNAVAILABLE -> {
                    reasons +=
                        "RESOURCE_UNAVAILABLE"
                    FrameState.HELD
                }

                resourceState ==
                    ResourceState.UNKNOWN -> {
                    reasons +=
                        "RESOURCE_UNKNOWN"
                    FrameState.UNKNOWN
                }

                else -> {
                    reasons +=
                        "CURRENTLY_AVAILABLE"
                    FrameState.AVAILABLE
                }
            }

        val sourceRefs =
            (
                baseSourceRefs +
                    listOf(
                        "source-path:" +
                            definition.sourcePath,
                    ) +
                    permissionFact
                        ?.sourceRefs
                        .orEmpty() +
                    resourceFact
                        ?.sourceRefs
                        .orEmpty()
            )
                .distinct()
                .sorted()

        return Entry(
            capabilityRef =
                definition.capabilityRef,
            purpose =
                definition.purpose,
            state =
                state,
            reasonCodes =
                reasons.sorted(),
            canonicalStage =
                definition.defaultStage,
            permissionRef =
                definition.permissionRef,
            permissionState =
                permissionState,
            resourceRef =
                definition.resourceRef,
            resourceState =
                resourceState,
            effectClass =
                definition.effectClass,
            resourceClass =
                definition.resourceClass,
            modelParticipationClass =
                definition.modelParticipationClass,
            sourceRefs =
                sourceRefs,
            effectAuthorityGranted =
                false,
        )
    }

    private fun canonicalPermissionFacts(
        values: List<PermissionFact>,
    ): Map<String, PermissionFact> {
        val output =
            linkedMapOf<String, PermissionFact>()
        values.forEach {
            value ->
            requireRef(
                value.permissionRef,
                "permissionFact.permissionRef",
            )
            val canonical =
                value.copy(
                    sourceRefs =
                        canonicalRefs(
                            value.sourceRefs,
                            "permissionFact.sourceRefs",
                        ),
                )
            require(
                output.put(
                    canonical.permissionRef,
                    canonical,
                ) ==
                    null,
            ) {
                "permission facts must be unique by permissionRef"
            }
        }
        return output
    }

    private fun canonicalResourceFacts(
        values: List<ResourceFact>,
    ): Map<String, ResourceFact> {
        val output =
            linkedMapOf<String, ResourceFact>()
        values.forEach {
            value ->
            requireRef(
                value.resourceRef,
                "resourceFact.resourceRef",
            )
            val canonical =
                value.copy(
                    sourceRefs =
                        canonicalRefs(
                            value.sourceRefs,
                            "resourceFact.sourceRefs",
                        ),
                )
            require(
                output.put(
                    canonical.resourceRef,
                    canonical,
                ) ==
                    null,
            ) {
                "resource facts must be unique by resourceRef"
            }
        }
        return output
    }

    private fun refsInState(
        entries: List<Entry>,
        state: FrameState,
    ): List<String> =
        entries
            .filter {
                it.state ==
                    state
            }
            .map {
                it.capabilityRef
            }
            .sorted()

    private fun validateDefinition(
        value: Definition,
    ) {
        requireRef(
            value.capabilityRef,
            "definition.capabilityRef",
        )
        require(
            value.purpose.isNotBlank(),
        ) {
            "definition purpose must be non-empty"
        }
        require(
            value.roleRefs.isNotEmpty() &&
                value.roleRefs ==
                value.roleRefs
                    .distinct()
                    .sorted(),
        ) {
            "definition roleRefs must be canonical"
        }
        require(
            value.platformRefs.isNotEmpty() &&
                value.platformRefs ==
                value.platformRefs
                    .distinct()
                    .sorted(),
        ) {
            "definition platformRefs must be canonical"
        }
        requireRef(
            value.permissionRef,
            "definition.permissionRef",
        )
        requireRef(
            value.resourceRef,
            "definition.resourceRef",
        )
        require(
            value.effectClass.isNotBlank() &&
                value.resourceClass.isNotBlank() &&
                value.modelParticipationClass.isNotBlank() &&
                value.sourcePath.isNotBlank() &&
                value.priority >=
                0,
        ) {
            "definition capability metadata is incomplete"
        }
    }

    private fun validateCurrentContext(
        value: CurrentContext,
    ) {
        requireRef(
            value.homeRef,
            "currentContext.homeRef",
        )
        requireRef(
            value.companionLineageRef,
            "currentContext.companionLineageRef",
        )
        requireRef(
            value.threadRef,
            "currentContext.threadRef",
        )
        requireRef(
            value.currentRequestEventRef,
            "currentContext.currentRequestEventRef",
        )
        listOf(
            "promptReceiptRefOrNull" to
                value.promptReceiptRefOrNull,
            "memoryContextRefOrNull" to
                value.memoryContextRefOrNull,
            "browserContextRefOrNull" to
                value.browserContextRefOrNull,
            "workingContinuityContextRefOrNull" to
                value.workingContinuityContextRefOrNull,
            "runtimeObservationRefOrNull" to
                value.runtimeObservationRefOrNull,
        )
            .forEach {
                (
                    label,
                    ref,
                ) ->
                ref
                    ?.let {
                        requireRef(
                            it,
                            "currentContext.$label",
                        )
                    }
            }
    }

    private fun canonicalRefs(
        values: List<String>,
        label: String,
    ): List<String> {
        values.forEach {
            requireRef(
                it,
                label,
            )
        }
        return values
            .distinct()
            .sorted()
    }

    private fun requireRef(
        value: String,
        label: String,
    ) {
        require(
            value.matches(
                Regex(
                    "[A-Za-z0-9][A-Za-z0-9._:/-]*",
                ),
            ),
        ) {
            "$label must be one stable ref"
        }
    }

    private inline fun <reified T> estimatedTokens(
        value: T,
    ): Int {
        val bytes =
            VexContractJson
                .canonicalEncode(
                    value,
                )
                .toByteArray(
                    StandardCharsets.UTF_8,
                )
                .size
        return maxOf(
            1,
            (
                bytes +
                    3
            ) /
                4,
        ) +
            4
    }

    private inline fun <reified T> semanticHash(
        value: T,
    ): String =
        MessageDigest
            .getInstance(
                "SHA-256",
            )
            .digest(
                VexContractJson
                    .canonicalEncode(
                        value,
                    )
                    .toByteArray(
                        StandardCharsets.UTF_8,
                    ),
            )
            .joinToString(
                "",
            ) {
                "%02x".format(
                    it.toInt() and
                        0xff,
                )
            }
}

// [VXG RealForever]
