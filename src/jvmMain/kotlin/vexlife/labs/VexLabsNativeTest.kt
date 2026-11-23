package vexlife.labs

import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.time.Instant
import java.util.UUID
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import vexlife.companion.DirectCompanionConnection
import vexlife.companion.DirectCompanionTurn
import vexlife.conversation.DirectConversationIdentity
import vexlife.companion.DirectPromptContext
import vexlife.context.VexLifeContextProfile
import vexlife.contract.VexContractJson
import vexlife.conversation.DirectConversationStore
import vexlife.filesystem.VexOwnedPath
import vexlife.home.VexLifeProdHome
import vexlife.home.VexLifeProdHomeFiles
import vexlife.local.VexLifeProdRuntimeSession
import vexlife.memory.VexLifeCurrentMemoryProviderSelection
import vexlife.memory.VexLifeCurrentMemoryReader
import vexlife.memory.VexLifeMemoryContract
import vexlife.model.heretic.HereticModelCatalog
import vexlife.model.heretic.HereticModelConfiguration
import vexlife.model.heretic.runtime.HereticExecutionTopology
import vexlife.service.VexLifeProdServiceContract
import vexlife.text.VexLifeText

/**
 * Native, scratch-only configuration laboratory over the real local model runtime.
 *
 * The Lab deliberately has no authority over the person's canonical Conversation or Memory. It
 * shadows only the canonical identity + optional current Conversation into a temporary directory,
 * reads current Memory from the real Home without mutation, and delegates each scratch turn to
 * [DirectCompanionTurn], the same Conversation engine used by the canonical Product. Lab turns are
 * durable only inside the disposable scratch root so multi-turn experiments inherit the same
 * idempotency, continuity, failure and commit semantics until Clear/Reload/service close deletes them.
 *
 * This is an experiment surface, not a second settings owner. Product Controls and Context Profiles
 * remain the only path that can make a configuration durable outside the Lab.
 */
class VexLabsNativeTest(
    private val productHome: Path,
    private val currentRuntimeEvidence: () -> HereticModelConfiguration.QualifiedRuntimeEvidence,
    private val activeContextProfile: () -> VexLifeContextProfile.Effective,
    private val personContext: () -> DirectPromptContext.PersonContext,
    private val currentProductConversationHead: () -> String?,
    private val transport: DirectCompanionConnection.Transport =
        DirectCompanionConnection.SystemTransport,
    private val executionTopology: () -> HereticExecutionTopology.Projection = {
        val runtime =
            currentRuntimeEvidence()
        HereticExecutionTopology.observe(
            productHome =
                productHome,
            model =
                requireNotNull(
                    HereticModelCatalog.entry(
                        runtime.modelRef,
                    ),
                ),
        )
    },
) : AutoCloseable {
    companion object {
        const val ControlClearConversation =
            "control.vexlife-prod.vex-labs.clear-conversation"

        const val ControlReloadCurrentConversation =
            "control.vexlife-prod.vex-labs.reload-current-conversation"

        const val ControlRestoreDefaults =
            "control.vexlife-prod.vex-labs.restore-defaults"

        const val ControlUseCurrentMemory =
            "control.vexlife-prod.vex-labs.memory-current"

        const val ControlUseNoMemory =
            "control.vexlife-prod.vex-labs.memory-none"

        private const val ProfileFieldRef =
            "field.vexlife-prod.context-profile.input-token-budget"

        private const val MemoryFieldRef =
            "field.vexlife-prod.continuity.maximum-memory-tokens"

        private const val PromptCacheFieldRef =
            "field.vexlife-prod.runtime.prompt-cache-entries"

        private const val LabInstanceRef =
            "instance.vexlife-prod.vex-labs.native-test"
    }

    private data class SourceHomeIdentity(
        val homeRef: String,
        val familyRef: String,
        val deviceRef: String,
        val companionLineageRef: String,
    )

    private data class Scratch(
        val root: Path,
        val seedConversationHeadSha256: String?,
        val seededMessageCount: Int,
        val sourceHomeIdentity: SourceHomeIdentity,
    )

    private val lock =
        ReentrantLock(
            true,
        )

    private var scratch: Scratch? =
        null

    private var conversationPreset =
        "CURRENT_SNAPSHOT"

    private var memoryMode =
        "CURRENT"

    private var lastAdmission: VexLifeProdServiceContract.VexLabsAdmission? =
        null

    private val exchanges =
        mutableListOf<VexLifeProdServiceContract.VexLabsExchange>()

    init {
        require(productHome.isAbsolute) {
            "Vex Labs requires one absolute canonical Product Home"
        }
    }

    fun projection(): VexLifeProdServiceContract.VexLabsProjection =
        lock.withLock {
            ensureScratch()
            projectionLocked()
        }

    fun control(
        request: VexLifeProdServiceContract.VexLabsControlRequest,
    ): VexLifeProdServiceContract.VexLabsControlResult =
        lock.withLock {
            when (
                request.controlRef
            ) {
                ControlClearConversation -> {
                    conversationPreset =
                        "EMPTY"
                    lastAdmission =
                        null
                    replaceScratch(
                        seedConversation = false,
                    )
                }

                ControlReloadCurrentConversation -> {
                    conversationPreset =
                        "CURRENT_SNAPSHOT"
                    lastAdmission =
                        null
                    replaceScratch(
                        seedConversation = true,
                    )
                }

                ControlRestoreDefaults -> {
                    conversationPreset =
                        "CURRENT_SNAPSHOT"
                    memoryMode =
                        "CURRENT"
                    lastAdmission =
                        null
                    replaceScratch(
                        seedConversation = true,
                    )
                }

                ControlUseCurrentMemory -> {
                    memoryMode =
                        "CURRENT"
                    lastAdmission =
                        null
                }

                ControlUseNoMemory -> {
                    memoryMode =
                        "NONE"
                    lastAdmission =
                        null
                }

                else ->
                    throw IllegalArgumentException(
                        "unknown Vex Labs native-test Control",
                    )
            }

            VexLifeProdServiceContract.VexLabsControlResult(
                schemaVersion =
                    VexLifeProdServiceContract.VexLabsControlResultSchema,
                controlRef = request.controlRef,
                outcome = "REALIZED_IN_LAB_ONLY",
                projection = projectionLocked(),
                labEffectPerformed = true,
            )
        }

    fun send(
        request: VexLifeProdServiceContract.VexLabsSendRequest,
    ): VexLifeProdServiceContract.VexLabsSendResult =
        lock.withLock {
            val currentScratch =
                ensureScratch()
            memoryMode =
                request.memoryMode

            require(
                request.inputTokenBudget <=
                    DirectPromptContext.InputTokenBudget,
            ) {
                "Vex Labs input budget cannot exceed the Product input ceiling"
            }
            require(
                request.maximumMemoryTokens <=
                    VexLifeMemoryContract.MaximumMemoryTokens,
            ) {
                "Vex Labs Memory cap cannot exceed the Product Memory prompt maximum"
            }

            val realConversationHeadBefore =
                currentProductConversationHead()
            val realMemoryBefore =
                if (
                    request.memoryMode ==
                        "CURRENT"
                ) {
                    VexLifeCurrentMemoryReader
                        .readCurrent(
                            productHome,
                        )
                        .semanticFingerprint
                } else {
                    null
                }

            val runtime =
                currentRuntimeEvidence()
            val effectiveProfile =
                labProfile(
                    requestedInputTokenBudget =
                        request.inputTokenBudget,
                )
            val scratchFoundation =
                VexLifeProdRuntimeSession.Foundation(
                    state =
                        VexLifeProdRuntimeSession.ReadyState,
                    productHome =
                        currentScratch.root.toString(),
                    qualifiedRuntimeEvidence =
                        runtime,
                )
            val useCurrentMemory =
                request.memoryMode ==
                    "CURRENT"

            val result =
                try {
                    DirectCompanionTurn.performWithHooks(
                                        setup =
                                            scratchFoundation,
                                        intent =
                                            DirectCompanionTurn.Intent(
                                                idempotencyKey =
                                                    "send.vex-labs." +
                                                        UUID.randomUUID()
                                                            .toString()
                                                            .lowercase(),
                                                content =
                                                    request.content,
                                                browserContext = null,
                                                personContext =
                                                    personContext(),
                                                contextProfile =
                                                    effectiveProfile,
                                            ),
                                        hooks =
                                            DirectCompanionTurn.Hooks(
                                                transport =
                                                    transport,
                                                currentRuntimeEvidence = {
                                                    currentRuntimeEvidence()
                                                },
                                                reobserveSameRuntimeEvidence = {
                                                    currentRuntimeEvidence()
                                                },
                                                currentMemoryContext = {
                                                    scratchHome ->
                                                    if (
                                                        useCurrentMemory
                                                    ) {
                                                        VexLifeCurrentMemoryReader.readCurrent(
                                                            productHome,
                                                        )
                                                    } else {
                                                        VexLifeCurrentMemoryReader.readCurrent(
                                                            scratchHome,
                                                        )
                                                    }
                                                },
                                                memoryContextAtCap = {
                                                    scratchHome,
                                                    memoryTokenCap,
                                                    ->
                                                    if (
                                                        useCurrentMemory
                                                    ) {
                                                        VexLifeCurrentMemoryReader.readCurrent(
                                                            productHome =
                                                                productHome,
                                                            memoryTokenCap =
                                                                memoryTokenCap,
                                                        )
                                                    } else {
                                                        VexLifeCurrentMemoryReader.readCurrent(
                                                            productHome =
                                                                scratchHome,
                                                            memoryTokenCap =
                                                                memoryTokenCap,
                                                        )
                                                    }
                                                },
                                                memoryProviderSelectionAtCap =
                                                    if (
                                                        useCurrentMemory
                                                    ) {
                                                        {
                                                            _,
                                                            memoryTokenCap,
                                                            ->
                                                            VexLifeCurrentMemoryProviderSelection.read(
                                                                productHome =
                                                                    productHome,
                                                                memoryTokenCap =
                                                                    memoryTokenCap,
                                                            )
                                                        }
                                                    } else {
                                                        null
                                                    },
                                                turnPolicy =
                                                    DirectCompanionTurn.TurnPolicy(
                                                        sourceReturnAllowed =
                                                            false,
                                                        capabilityRequestsAllowed =
                                                            false,
                                                        maximumMemoryTokens =
                                                            request.maximumMemoryTokens,
                                                    ),
                                            ),
                                    )
                } catch (
                    error: DirectCompanionTurn.TurnException,
                ) {
                    val admission =
                        error.cause as?
                            DirectPromptContext.AdmissionException
                    if (
                        admission ==
                            null
                    ) {
                        throw error
                    }

                    require(
                        currentProductConversationHead() ==
                            realConversationHeadBefore,
                    ) {
                        "Vex Labs changed the real Conversation head while prompt admission was held"
                    }
                    if (
                        realMemoryBefore !=
                            null
                    ) {
                        require(
                            VexLifeCurrentMemoryReader
                                .readCurrent(
                                    productHome,
                                )
                                .semanticFingerprint ==
                                realMemoryBefore,
                        ) {
                            "Vex Labs changed current durable Memory while prompt admission was held"
                        }
                    }

                    val projectedAdmission =
                        VexLifeProdServiceContract.VexLabsAdmission(
                            reason =
                                admission.reason.name,
                            requestedInputTokenBudget =
                                admission.requestedInputTokenBudget,
                            minimumInputTokenBudget =
                                admission.minimumInputTokenBudget,
                            fixedContextTokens =
                                admission.fixedContextTokens,
                            immediatePriorTokens =
                                admission.immediatePriorTokens,
                            capabilityExchangeAllowanceTokens =
                                admission.capabilityExchangeAllowanceTokens,
                        )
                    lastAdmission =
                        projectedAdmission

                    return@withLock VexLifeProdServiceContract.VexLabsSendResult(
                        schemaVersion =
                            VexLifeProdServiceContract.VexLabsSendResultSchema,
                        state =
                            "HELD_CONTEXT_BUDGET",
                        reply =
                            null,
                        sealedHumanReasoning =
                            null,
                        providerRequestBody =
                            null,
                        providerRequestBodySha256 =
                            null,
                        promptInputTokenBudget =
                            request.inputTokenBudget,
                        maximumMemoryTokens =
                            request.maximumMemoryTokens,
                        memoryMode =
                            request.memoryMode,
                        promptReservedOutputTokens =
                            null,
                        promptHardTokenLimit =
                            null,
                        memoryIncluded =
                            false,
                        memoryContextRef =
                            null,
                        selectedConversationEventCount =
                            0,
                        admission =
                            projectedAdmission,
                        projection =
                            projectionLocked(),
                        realConversationMutationPerformed =
                            false,
                        realMemoryMutationPerformed =
                            false,
                        labConversationMutationPerformed =
                            true,
                    )
                }

            require(
                currentProductConversationHead() ==
                    realConversationHeadBefore,
            ) {
                "Vex Labs changed the real Conversation head"
            }
            if (
                realMemoryBefore !=
                    null
            ) {
                require(
                    VexLifeCurrentMemoryReader
                        .readCurrent(
                            productHome,
                        )
                        .semanticFingerprint ==
                        realMemoryBefore,
                ) {
                    "Vex Labs changed current durable Memory"
                }
            }

            lastAdmission =
                null

            val providerRequest =
                requireNotNull(
                    result.providerRequests.singleOrNull(),
                ) {
                    "Vex Labs first slice requires exactly one native provider request because Source Return and external capability requests are held"
                }
            val prompt =
                result.promptContextReceipt
            val scratchMessageCount =
                DirectConversationStore(
                    currentScratch.root,
                )
                    .current()
                    .canonicalEvents
                    .size

            exchanges +=
                VexLifeProdServiceContract.VexLabsExchange(
                    human = request.content,
                    vex = result.content,
                    sealedHumanReasoning =
                        result.humanReasoning,
                    promptInputTokenBudget =
                        prompt.inputTokenBudget,
                    maximumMemoryTokens =
                        request.maximumMemoryTokens,
                    memoryMode =
                        request.memoryMode,
                    memoryIncluded =
                        prompt.memoryIncluded,
                    selectedConversationEventCount =
                        prompt.selectedConversationEventRefs.size,
                    providerRequestBodySha256 =
                        providerRequest.requestBodySha256,
                )

            VexLifeProdServiceContract.VexLabsSendResult(
                schemaVersion =
                    VexLifeProdServiceContract.VexLabsSendResultSchema,
                state = "LAB_TURN_COMPLETED",
                reply = result.content,
                sealedHumanReasoning =
                    result.humanReasoning,
                providerRequestBody =
                    providerRequest.requestBody,
                providerRequestBodySha256 =
                    providerRequest.requestBodySha256,
                promptInputTokenBudget =
                    prompt.inputTokenBudget,
                maximumMemoryTokens =
                    request.maximumMemoryTokens,
                memoryMode =
                    request.memoryMode,
                promptReservedOutputTokens =
                    prompt.reservedOutputTokens,
                promptHardTokenLimit =
                    prompt.hardTokenLimit,
                memoryIncluded =
                    prompt.memoryIncluded,
                memoryContextRef =
                    prompt.memoryContextRef,
                selectedConversationEventCount =
                    prompt.selectedConversationEventRefs.size,
                admission =
                    null,
                projection =
                    projectionLocked(
                        scratchConversationMessageCount =
                            scratchMessageCount,
                    ),
                realConversationMutationPerformed = false,
                realMemoryMutationPerformed = false,
                labConversationMutationPerformed = true,
            )
        }

    private fun projectionLocked(
        scratchConversationMessageCount: Int? =
            null,
    ): VexLifeProdServiceContract.VexLabsProjection {
        val currentScratch =
            requireNotNull(
                scratch,
            ) {
                "Vex Labs scratch Conversation is not formed"
            }
        val profile =
            activeContextProfile()
        val topology =
            executionTopology()
        val effectiveProfileBudget =
            profile.definition.inputTokenBudget
                ?: DirectPromptContext.InputTokenBudget
        val fields =
            listOf(
                VexLifeProdServiceContract.VexLabsField(
                    fieldRef =
                        ProfileFieldRef,
                    group = "CONTINUITY",
                    label =
                        VexLifeText(
                            "workspace.label.execution-field-context-profile-input-token-budget",
                        ),
                    explanation =
                        VexLifeText(
                            "workspace.description.execution-field-context-profile-input-token-budget",
                        ),
                    currentValue =
                        effectiveProfileBudget.toString(),
                    inheritedDefaultValue =
                        DirectPromptContext.InputTokenBudget.toString(),
                    recommendedValue = null,
                    minimumValue = "1",
                    maximumValue =
                        DirectPromptContext.InputTokenBudget.toString(),
                    unit =
                        VexLifeText(
                            "workspace.label.execution-unit-tokens",
                        ),
                    provenance = "CONTEXT_PROFILE",
                    currentChangeBoundary = "NEXT_SEND",
                    ownerRef = "VexLifeContextProfile",
                    effectiveState = "ACTIVE",
                    userMutability = "USER_EDITABLE",
                    userApplyBoundary = "NEXT_SEND",
                    controlRef = null,
                    increaseConsequence =
                        VexLifeText(
                            "workspace.description.execution-field-context-profile-input-token-budget-increase",
                        ),
                    decreaseConsequence =
                        VexLifeText(
                            "workspace.description.execution-field-context-profile-input-token-budget-decrease",
                        ),
                    relatedFieldRefs =
                        listOf(
                            "field.vexlife-prod.continuity.input-token-budget",
                            MemoryFieldRef,
                        ),
                    labEditability = "EDITABLE_LAB_ONLY",
                    labEditDetail =
                        VexLifeText(
                            "workspace.description.vex-labs-edit-context-profile-active",
                        ),
                    labObservedMinimumValue =
                        lastAdmission
                            ?.minimumInputTokenBudget
                            ?.toString(),
                ),
            ) +
                topology.fieldCatalog.fields.map {
                    field ->
                    val labEditability =
                        when (
                            field.fieldRef
                        ) {
                            MemoryFieldRef ->
                                "EDITABLE_LAB_ONLY"

                            PromptCacheFieldRef ->
                                "EDITABLE_PRODUCT_CONTROL_RESTART"

                            else ->
                                when (
                                    field.userMutability
                                ) {
                                    HereticExecutionTopology.UserMutability.READ_ONLY_MODEL_ARTIFACT,
                                    HereticExecutionTopology.UserMutability.READ_ONLY_PRODUCT_POLICY,
                                    ->
                                        "READ_ONLY"

                                    HereticExecutionTopology.UserMutability.HELD_UNTIL_QUALIFIED ->
                                        "HELD"

                                    HereticExecutionTopology.UserMutability.USER_EDITABLE ->
                                        "HELD"
                                }
                        }
                    val detail =
                        VexLifeText(
                            when (
                                labEditability
                            ) {
                                "EDITABLE_LAB_ONLY" ->
                                    "workspace.description.vex-labs-edit-lab-only"

                                "EDITABLE_PRODUCT_CONTROL_RESTART" ->
                                    "workspace.description.vex-labs-edit-product-restart"

                                "READ_ONLY" ->
                                    "workspace.description.vex-labs-edit-read-only"

                                else ->
                                    "workspace.description.vex-labs-edit-held"
                            },
                        )
                    labField(
                        field = field,
                        labEditability = labEditability,
                        labEditDetail = detail,
                    )
                }

        val observedScratchCount =
            scratchConversationMessageCount
                ?: DirectConversationStore(
                    currentScratch.root,
                )
                    .withWriterLease(
                        instanceRef =
                            LabInstanceRef,
                    ) {
                        current().canonicalEvents.size
                    }

        return VexLifeProdServiceContract.VexLabsProjection(
            schemaVersion =
                VexLifeProdServiceContract.VexLabsProjectionSchema,
            state = "READY",
            modelRef = topology.modelRef,
            modelProfileRef = topology.modelProfileRef,
            fieldCatalogSchemaVersion =
                topology.fieldCatalog.schemaVersion,
            fields = fields,
            defaults =
                VexLifeProdServiceContract.VexLabsDraftDefaults(
                    inputTokenBudget =
                        effectiveProfileBudget,
                    maximumMemoryTokens =
                        VexLifeMemoryContract.MaximumMemoryTokens,
                    memoryMode = "CURRENT",
                    conversationPreset =
                        "CURRENT_SNAPSHOT",
                ),
            conversationPreset =
                conversationPreset,
            memoryMode =
                memoryMode,
            seedConversationHeadSha256 =
                currentScratch.seedConversationHeadSha256,
            currentProductConversationHeadSha256 =
                currentProductConversationHead(),
            exchanges =
                exchanges.toList(),
            scratchConversationMessageCount =
                observedScratchCount,
            lastAdmission =
                lastAdmission,
            effectsPerformed = false,
        )
    }

    private fun labField(
        field: HereticExecutionTopology.FieldDescriptor,
        labEditability: String,
        labEditDetail: VexLifeText,
    ): VexLifeProdServiceContract.VexLabsField =
        VexLifeProdServiceContract.VexLabsField(
            fieldRef = field.fieldRef,
            group = field.group.name,
            label = field.label,
            explanation = field.explanation,
            currentValue = field.currentValue,
            inheritedDefaultValue =
                field.inheritedDefaultValue,
            recommendedValue =
                field.recommendedValue,
            minimumValue =
                field.minimumValue,
            maximumValue =
                field.maximumValue,
            unit = field.unit,
            provenance = field.provenance.name,
            currentChangeBoundary =
                field.currentChangeBoundary.name,
            ownerRef = field.ownerRef,
            effectiveState = field.effectiveState.name,
            userMutability = field.userMutability.name,
            userApplyBoundary =
                field.userApplyBoundary
                    ?.name,
            controlRef = field.controlRef,
            increaseConsequence =
                field.increaseConsequence,
            decreaseConsequence =
                field.decreaseConsequence,
            relatedFieldRefs =
                field.relatedFieldRefs,
            labEditability = labEditability,
            labEditDetail = labEditDetail,
            labObservedMinimumValue =
                null,
        )

    private fun labProfile(
        requestedInputTokenBudget: Int,
    ): VexLifeContextProfile.Effective {
        val active =
            activeContextProfile()
        val definition =
            active.definition.copy(
                schemaVersion =
                    VexLifeContextProfile.Schema,
                inputTokenBudget =
                    requestedInputTokenBudget,
            )
        return VexLifeContextProfile.Effective(
            profileRef =
                "profile.vexlife.context.vex-labs-native-test",
            revision = 0,
            definition = definition,
            basedOnProductFoundation =
                active.basedOnProductFoundation,
            fingerprint =
                sha256(
                    VexContractJson.canonicalEncode(
                        definition,
                    ),
                ),
            formedAt = null,
            productDefault = false,
            productFoundationChangedSinceSave =
                active.productFoundationChangedSinceSave,
        )
    }

    private fun currentSourceHome(): VexLifeProdHomeFiles.Result =
        requireNotNull(
            VexLifeProdHomeFiles.observeExisting(
                productHome,
            ),
        ) {
            "Vex Labs requires one current canonical Product Home"
        }

    private fun sourceHomeIdentity(
        current: VexLifeProdHomeFiles.Result,
    ): SourceHomeIdentity =
        SourceHomeIdentity(
            homeRef = current.manifest.homeRef,
            familyRef = current.manifest.familyRef,
            deviceRef = current.installation.deviceRef,
            companionLineageRef = current.installation.companionLineageRef,
        )

    private fun ensureScratch(): Scratch {
        val current =
            currentSourceHome()
        val currentIdentity =
            sourceHomeIdentity(
                current,
            )
        val existing =
            scratch
        if (
            existing !=
                null &&
            existing.sourceHomeIdentity ==
                currentIdentity
        ) {
            return existing
        }
        return replaceScratch(
            seedConversation =
                conversationPreset ==
                    "CURRENT_SNAPSHOT",
            current = current,
        )
    }

    private fun replaceScratch(
        seedConversation: Boolean,
        current: VexLifeProdHomeFiles.Result =
            currentSourceHome(),
    ): Scratch {
        lastAdmission =
            null
        scratch
            ?.root
            ?.let(
                ::deleteScratch,
            )
        exchanges.clear()

        val currentIdentity =
            sourceHomeIdentity(
                current,
            )
        val root =
            Files.createTempDirectory(
                "vexlife-labs-native-test-",
            )
                .toAbsolutePath()
                .normalize()

        VexLifeProdHome.BaseDirectories.forEach {
            relative ->
            VexOwnedPath.ensureDirectory(
                root,
                VexOwnedPath.resolveInside(
                    root,
                    relative,
                ),
            )
        }
        copyFile(
            from =
                VexOwnedPath.resolveInside(
                    productHome,
                    VexLifeProdHome.HomeManifestRelativePath,
                ),
            to =
                VexOwnedPath.resolveInside(
                    root,
                    VexLifeProdHome.HomeManifestRelativePath,
                ),
        )
        copyFile(
            from =
                VexOwnedPath.resolveInside(
                    productHome,
                    "devices/${current.manifest.currentDeviceRef}.json",
                ),
            to =
                VexOwnedPath.resolveInside(
                    root,
                    "devices/${current.manifest.currentDeviceRef}.json",
                ),
        )

        val sourceConversationBefore =
            DirectConversationStore(
                productHome,
            )
                .current()

        if (
            seedConversation
        ) {
            val lineage =
                current.manifest.currentCompanionLineageRef
            copyTreeIfPresent(
                from =
                    VexOwnedPath.resolveInside(
                        productHome,
                        "conversations/$lineage/${DirectConversationIdentity.ThreadRef}",
                    ),
                to =
                    VexOwnedPath.resolveInside(
                        root,
                        "conversations/$lineage/${DirectConversationIdentity.ThreadRef}",
                    ),
            )
            copyTreeIfPresent(
                from =
                    VexOwnedPath.resolveInside(
                        productHome,
                        "context/$lineage/${DirectConversationIdentity.ThreadRef}",
                    ),
                to =
                    VexOwnedPath.resolveInside(
                        root,
                        "context/$lineage/${DirectConversationIdentity.ThreadRef}",
                    ),
            )
        }

        val sourceConversationAfter =
            DirectConversationStore(
                productHome,
            )
                .current()
        require(
            sourceConversationBefore.head
                ?.conversationHeadSha256 ==
                sourceConversationAfter.head
                    ?.conversationHeadSha256 &&
                sourceConversationBefore.canonicalEvents.map {
                    it.eventHash
                } ==
                sourceConversationAfter.canonicalEvents.map {
                    it.eventHash
                },
        ) {
            "real Conversation changed while Vex Labs was taking its scratch snapshot; reload the Lab from the new current Conversation"
        }

        require(
            sourceHomeIdentity(
                currentSourceHome(),
            ) ==
                currentIdentity,
        ) {
            "canonical VexLife Home identity changed while Vex Labs was taking its scratch snapshot"
        }

        requireNotNull(
            VexLifeProdHomeFiles.observeExisting(
                root,
            ),
        ) {
            "Vex Labs scratch identity did not re-observe as a canonical shadow"
        }

        val snapshot =
            DirectConversationStore(
                root,
            )
                .withWriterLease(
                    instanceRef =
                        LabInstanceRef,
                ) {
                    current()
                }
        val next =
            Scratch(
                root = root,
                seedConversationHeadSha256 =
                    snapshot.head
                        ?.conversationHeadSha256,
                seededMessageCount =
                    snapshot.canonicalEvents.size,
                sourceHomeIdentity =
                    currentIdentity,
            )
        scratch =
            next
        return next
    }

    private fun copyFile(
        from: Path,
        to: Path,
    ) {
        require(
            Files.isRegularFile(
                from,
                LinkOption.NOFOLLOW_LINKS,
            )
        ) {
            "Vex Labs source identity file is missing"
        }
        require(
            !Files.isSymbolicLink(
                from,
            )
        ) {
            "Vex Labs does not copy linked identity files"
        }
        Files.createDirectories(
            requireNotNull(
                to.parent,
            ),
        )
        Files.copy(
            from,
            to,
            StandardCopyOption.COPY_ATTRIBUTES,
        )
    }

    private fun copyTreeIfPresent(
        from: Path,
        to: Path,
    ) {
        if (
            !Files.exists(
                from,
                LinkOption.NOFOLLOW_LINKS,
            )
        ) {
            return
        }
        require(
            Files.isDirectory(
                from,
                LinkOption.NOFOLLOW_LINKS,
            ) &&
                !Files.isSymbolicLink(
                    from,
                )
        ) {
            "Vex Labs source tree must be one real directory"
        }
        Files.walk(
            from,
        )
            .use {
                stream ->
                stream.forEach {
                    source ->
                    require(
                        !Files.isSymbolicLink(
                            source,
                        )
                    ) {
                        "Vex Labs does not copy symbolic links into the scratch Home"
                    }
                    val relative =
                        from.relativize(
                            source,
                        )
                    val target =
                        to.resolve(
                            relative,
                        )
                    if (
                        Files.isDirectory(
                            source,
                            LinkOption.NOFOLLOW_LINKS,
                        )
                    ) {
                        Files.createDirectories(
                            target,
                        )
                    } else {
                        require(
                            Files.isRegularFile(
                                source,
                                LinkOption.NOFOLLOW_LINKS,
                            )
                        )
                        Files.createDirectories(
                            requireNotNull(
                                target.parent,
                            ),
                        )
                        Files.copy(
                            source,
                            target,
                            StandardCopyOption.COPY_ATTRIBUTES,
                        )
                    }
                }
            }
    }

    private fun deleteScratch(
        root: Path,
    ) {
        if (
            !Files.exists(
                root,
                LinkOption.NOFOLLOW_LINKS,
            )
        ) {
            return
        }
        require(
            Files.isDirectory(
                root,
                LinkOption.NOFOLLOW_LINKS,
            ) &&
                !Files.isSymbolicLink(
                    root,
                )
        ) {
            "Vex Labs scratch root must remain one real directory"
        }
        Files.walk(
            root,
        )
            .use {
                stream ->
                stream
                    .toList()
                    .sortedByDescending {
                        it.nameCount
                    }
                    .forEach {
                        Files.deleteIfExists(
                            it,
                        )
                    }
            }
    }

    private fun sha256(
        value: String,
    ): String =
        MessageDigest
            .getInstance(
                "SHA-256",
            )
            .digest(
                value.toByteArray(
                    Charsets.UTF_8,
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

    override fun close() {
        lock.withLock {
            scratch
                ?.root
                ?.let(
                    ::deleteScratch,
                )
            scratch =
                null
            exchanges.clear()
        }
    }
}

// [VXG RealForever]
