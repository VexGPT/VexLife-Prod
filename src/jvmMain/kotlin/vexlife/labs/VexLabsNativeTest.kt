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
import vexlife.conversation.DirectConversationIdentity
import vexlife.companion.DirectPromptContext
import vexlife.companion.DirectTurnContextStore
import vexlife.context.VexLifeContextProfile
import vexlife.contract.VexContractJson
import vexlife.conversation.DirectConversationStore
import vexlife.filesystem.VexOwnedPath
import vexlife.foundational.VexLifeFoundationalField
import vexlife.home.VexLifeProdHome
import vexlife.home.VexLifeProdHomeFiles
import vexlife.memory.VexLifeCurrentMemoryProviderSelection
import vexlife.memory.VexLifeCurrentMemoryReader
import vexlife.memory.VexLifeMemoryContract
import vexlife.model.heretic.HereticModelCatalog
import vexlife.model.heretic.HereticModelConfiguration
import vexlife.model.heretic.runtime.HereticExecutionTopology
import vexlife.service.VexLifeProdServiceContract

/**
 * Native, scratch-only configuration laboratory over the real local model runtime.
 *
 * The Lab deliberately has no authority over the person's canonical Conversation or Memory. It
 * shadows only the canonical identity + optional current Conversation into a temporary directory,
 * reads current Memory from the real Home without mutation, and submits one ordinary provider-ready
 * prompt through [DirectCompanionConnection]. Lab turns are durable only inside the disposable
 * scratch root so multi-turn experiments behave like a conversation until Clear/Reload/service
 * close deletes them.
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

    private data class Scratch(
        val root: Path,
        val seedConversationHeadSha256: String?,
        val seededMessageCount: Int,
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
                    replaceScratch(
                        seedConversation = false,
                    )
                }

                ControlReloadCurrentConversation -> {
                    conversationPreset =
                        "CURRENT_SNAPSHOT"
                    replaceScratch(
                        seedConversation = true,
                    )
                }

                ControlRestoreDefaults -> {
                    conversationPreset =
                        "CURRENT_SNAPSHOT"
                    memoryMode =
                        "CURRENT"
                    replaceScratch(
                        seedConversation = true,
                    )
                }

                ControlUseCurrentMemory ->
                    memoryMode =
                        "CURRENT"

                ControlUseNoMemory ->
                    memoryMode =
                        "NONE"

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
                        "CURRENT" &&
                    request.maximumMemoryTokens >
                        0
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
            val model =
                requireNotNull(
                    HereticModelCatalog.entry(
                        runtime.modelRef,
                    ),
                ) {
                    "Vex Labs runtime model is not offered by the current Product"
                }
            val contextStore =
                DirectTurnContextStore(
                    currentScratch.root,
                )
            val store =
                DirectConversationStore(
                    currentScratch.root,
                )
            val effectiveProfile =
                labProfile(
                    requestedInputTokenBudget =
                        request.inputTokenBudget,
                )
            val memoryAvailable =
                if (
                    request.memoryMode ==
                        "CURRENT" &&
                    request.maximumMemoryTokens >
                        0
                ) {
                    VexLifeCurrentMemoryReader.readCurrent(
                        productHome,
                    )
                } else {
                    null
                }

            val completed =
                store.withWriterLease(
                    instanceRef =
                        LabInstanceRef,
                ) {
                    val prior =
                        immediatePriorCompletedTurn()
                    val suffix =
                        UUID.randomUUID()
                            .toString()
                            .lowercase()
                    val currentRequest =
                        beginRequest(
                            turnRef =
                                "turn.vex-labs." +
                                    suffix,
                            requestMessageRef =
                                "message.vex-labs.human." +
                                    suffix,
                            content = request.content,
                        )
                    val prompt =
                        DirectPromptContext.materializeContinuityAt(
                            conversationOwner = this,
                            prior = prior,
                            currentRequest = currentRequest,
                            memoryAvailabilityContext = memoryAvailable,
                            memoryContextAtCap =
                                if (
                                    memoryAvailable !=
                                        null
                                ) {
                                    {
                                        memoryTokenCap ->
                                        VexLifeCurrentMemoryReader.readCurrent(
                                            productHome =
                                                productHome,
                                            memoryTokenCap =
                                                memoryTokenCap,
                                        )
                                    }
                                } else {
                                    null
                                },
                            workingContinuityContext = null,
                            browserContext = null,
                            personContext = personContext(),
                            runtimeEvidence = runtime,
                            sourceReturnProtocolEnabled = false,
                            sourceReturnDiscovery = null,
                            foundationalField =
                                VexLifeFoundationalField.readCurrent(),
                            contextProfile = effectiveProfile,
                            conversationExtentIncluded = true,
                            capabilityRequestsAdmitted = false,
                            experimentEffectiveInputTokenBudget = null,
                            memoryProviderSelectionAtCap =
                                if (
                                    memoryAvailable !=
                                        null
                                ) {
                                    {
                                        memoryTokenCap ->
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
                            maximumMemoryTokens =
                                request.maximumMemoryTokens,
                        )
                    val inference =
                        DirectCompanionConnection.invoke(
                            runtime = runtime,
                            messages = prompt.messages,
                            capabilityOffer =
                                emptyList(),
                            model = model,
                        )
                    val response =
                        appendResponse(
                            request = currentRequest,
                            responseMessageRef =
                                "message.vex-labs.companion." +
                                    suffix,
                            content = inference.content,
                            model = inference.model,
                            endpointOrigin =
                                inference.invocationEvidence.endpointOrigin,
                            runtimeObservationRef =
                                inference.runtimeObservation.observationRef,
                            invocationEvidenceRef =
                                inference.invocationEvidence.invocationRef,
                        )
                    val context =
                        contextStore.record(
                            request = currentRequest,
                            response = response,
                            promptContext = prompt.receipt,
                            runtimeObservation = inference.runtimeObservation,
                            invocationEvidence = inference.invocationEvidence,
                        )
                    commitCompletedTurn(
                        request = currentRequest,
                        response = response,
                        contextSha256 = context.contextSha256,
                    )

                    Triple(
                        prompt,
                        inference,
                        current().canonicalEvents.size,
                    )
                }

            val prompt =
                completed.first
            val inference =
                completed.second

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

            exchanges +=
                VexLifeProdServiceContract.VexLabsExchange(
                    human = request.content,
                    vex = inference.content,
                    sealedHumanReasoning =
                        inference.sealedHumanReasoning,
                    promptInputTokenBudget =
                        prompt.receipt.inputTokenBudget,
                    memoryIncluded =
                        prompt.receipt.memoryIncluded,
                    selectedConversationEventCount =
                        prompt.receipt.selectedConversationEventRefs.size,
                    providerRequestBodySha256 =
                        inference.invocationEvidence.requestBodySha256,
                )

            VexLifeProdServiceContract.VexLabsSendResult(
                schemaVersion =
                    VexLifeProdServiceContract.VexLabsSendResultSchema,
                state = "LAB_TURN_COMPLETED",
                reply = inference.content,
                sealedHumanReasoning =
                    inference.sealedHumanReasoning,
                providerRequestBody =
                    inference.providerRequestBody,
                providerRequestBodySha256 =
                    inference.invocationEvidence.requestBodySha256,
                promptInputTokenBudget =
                    prompt.receipt.inputTokenBudget,
                promptReservedOutputTokens =
                    prompt.receipt.reservedOutputTokens,
                promptHardTokenLimit =
                    prompt.receipt.hardTokenLimit,
                memoryIncluded =
                    prompt.receipt.memoryIncluded,
                memoryContextRef =
                    prompt.receipt.memoryContextRef,
                selectedConversationEventCount =
                    prompt.receipt.selectedConversationEventRefs.size,
                projection =
                    projectionLocked(
                        scratchConversationMessageCount =
                            completed.third,
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
            HereticExecutionTopology.observe(
                productHome =
                    productHome,
                model =
                    requireNotNull(
                        HereticModelCatalog.entry(
                            currentRuntimeEvidence()
                                .modelRef,
                        ),
                    ),
            )
        val effectiveProfileBudget =
            profile.definition.inputTokenBudget
                ?: DirectPromptContext.InputTokenBudget
        val fields =
            listOf(
                VexLifeProdServiceContract.VexLabsField(
                    fieldRef =
                        ProfileFieldRef,
                    group = "CONTINUITY",
                    label = "Active context for this profile",
                    explanation =
                        "How much prompt input this Context Profile may admit before VexLife reserves room for capability exchanges and the model response. Lower values make older Conversation and Memory leave the active prompt sooner without deleting them.",
                    currentValue =
                        effectiveProfileBudget.toString(),
                    inheritedDefaultValue =
                        DirectPromptContext.InputTokenBudget.toString(),
                    recommendedValue = null,
                    minimumValue = "1",
                    maximumValue =
                        DirectPromptContext.InputTokenBudget.toString(),
                    unit = "tokens",
                    provenance = "CONTEXT_PROFILE",
                    currentChangeBoundary = "NEXT_SEND",
                    ownerRef = "VexLifeContextProfile",
                    effectiveState = "ACTIVE",
                    userMutability = "USER_EDITABLE",
                    userApplyBoundary = "NEXT_SEND",
                    controlRef = null,
                    increaseConsequence =
                        "Keeps more Conversation and Memory directly resident when room exists.",
                    decreaseConsequence =
                        "Makes context pressure arrive sooner while older durable source remains available.",
                    relatedFieldRefs =
                        listOf(
                            "field.vexlife-prod.continuity.input-token-budget",
                            MemoryFieldRef,
                        ),
                    labEditability = "EDITABLE_LAB_ONLY",
                    labEditDetail =
                        "Change it here to test the next Lab turn. Nothing is saved to your real Context Profile.",
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
                        when (
                            labEditability
                        ) {
                            "EDITABLE_LAB_ONLY" ->
                                "This value can be lowered temporarily for a scratch Lab turn without changing durable Product policy."

                            "EDITABLE_PRODUCT_CONTROL_RESTART" ->
                                "A real Product Control exists, but it applies at runtime restart. Vex Labs does not restart or rewrite your real runtime while testing."

                            "READ_ONLY" ->
                                "Inspectable here, but its rightful owner is read-only from this surface."

                            else ->
                                "Known to VexLife, but no qualified edit path exists yet. Labs shows it instead of hiding it."
                        }
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
            effectsPerformed = false,
        )
    }

    private fun labField(
        field: HereticExecutionTopology.FieldDescriptor,
        labEditability: String,
        labEditDetail: String,
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

    private fun ensureScratch(): Scratch {
        val existing =
            scratch
        if (
            existing !=
                null
        ) {
            return existing
        }
        return replaceScratch(
            seedConversation =
                conversationPreset ==
                    "CURRENT_SNAPSHOT",
        )
    }

    private fun replaceScratch(
        seedConversation: Boolean,
    ): Scratch {
        scratch
            ?.root
            ?.let(
                ::deleteScratch,
            )
        exchanges.clear()

        val current =
            requireNotNull(
                VexLifeProdHomeFiles.observeExisting(
                    productHome,
                ),
            ) {
                "Vex Labs requires one current canonical Product Home"
            }
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
