package vexlife.attachment

import kotlinx.serialization.Serializable
import vexlife.artifact.VexLifeLocalArtifact
import vexlife.semantic.VexFamilyContract
import vexlife.text.VexLifeText

/**
 * Semantic owner for files currently staged in the human's unsent Composer draft.
 *
 * This Family owns only pre-Send meaning. Artifact byte custody, conversion, Conversation REQUEST
 * formation and model reach are separate owners.
 *
 * ```text
 * STAGED != SENT
 * SENT != READ
 * READY DRAFT ITEM != REQUEST RESOURCE BINDING
 * REMOVE FROM DRAFT != DELETE ORIGINAL ARTIFACT
 * ```
 */
object VexLifeAttachmentDraftFamily :
    VexFamilyContract<
        VexLifeAttachmentDraftFamily,
        VexLifeAttachmentDraftFamily.State,
        VexLifeAttachmentDraftFamily.Control,
        VexLifeAttachmentDraftFamily.Effect,
    >() {
    @Serializable
    enum class ItemStatus {
        STAGING,
        READY,
        HELD,
    }

    @Serializable
    data class Item(
        val draftRef: String,
        val displayName: String,
        val status: ItemStatus,
        val artifactRef: String? = null,
        val projectionRef: String? = null,
        val completedBytes: Long = 0,
        val totalBytesOrNull: Long? = null,
        val holdReasonKeyOrNull: String? = null,
    ) {
        init {
            require(
                draftRef.matches(
                    Regex(
                        "[A-Za-z0-9][A-Za-z0-9._:-]{0,220}",
                    ),
                ),
            ) {
                "attachment draft ref must be one portable stable ref"
            }
            require(
                VexLifeLocalArtifact.admitDisplayName(
                    displayName,
                ) ==
                    displayName,
            ) {
                "attachment draft display name is not admitted"
            }
            require(
                completedBytes >=
                    0 &&
                    (
                        totalBytesOrNull ==
                            null ||
                            (
                                totalBytesOrNull >
                                    0 &&
                                    completedBytes <=
                                    totalBytesOrNull
                            )
                    ),
            ) {
                "attachment draft byte progress is invalid"
            }

            when (
                status
            ) {
                ItemStatus.STAGING -> {
                    require(
                        artifactRef ==
                            null &&
                            projectionRef ==
                            null &&
                            holdReasonKeyOrNull ==
                            null,
                    ) {
                        "staging attachment cannot claim artifact/projection/hold truth"
                    }
                }

                ItemStatus.READY -> {
                    require(
                        artifactRef !=
                            null &&
                            VexLifeLocalArtifact.isArtifactRef(
                                artifactRef,
                            ) &&
                            projectionRef !=
                            null &&
                            VexLifeLocalArtifact.isProjectionRef(
                                projectionRef,
                            ) &&
                            holdReasonKeyOrNull ==
                            null,
                    ) {
                        "ready attachment requires exact artifact/projection identity"
                    }
                }

                ItemStatus.HELD -> {
                    require(
                        projectionRef ==
                            null &&
                            !holdReasonKeyOrNull.isNullOrBlank(),
                    ) {
                        "held attachment requires a reason and cannot claim a readable projection"
                    }
                    require(
                        artifactRef ==
                            null ||
                            VexLifeLocalArtifact.isArtifactRef(
                                artifactRef,
                            ),
                    ) {
                        "held attachment artifact identity is invalid"
                    }
                }
            }
        }
    }

    sealed interface State : VexFamilyContract.State<VexLifeAttachmentDraftFamily> {
        val stateRef: String
        val items: List<Item>
    }

    data object Empty : State {
        override val stateRef: String =
            StateEmpty
        override val items: List<Item> =
            emptyList()
    }

    data class Draft(
        override val items: List<Item>,
    ) : State {
        override val stateRef: String =
            StateDraft

        init {
            require(
                items.isNotEmpty() &&
                    items.size <=
                    VexLifeLocalArtifact.MaximumPerRequest &&
                    items.map {
                        it.draftRef
                    }
                        .distinct()
                        .size ==
                    items.size,
            ) {
                "attachment draft items must be non-empty, unique and within the request limit"
            }
        }
    }

    sealed interface Control : VexFamilyContract.Control<VexLifeAttachmentDraftFamily> {
        val controlRef: String
    }

    data object AddFiles : Control {
        override val controlRef: String =
            ControlAddFiles
    }

    data class CancelStage(
        val draftRef: String,
    ) : Control {
        override val controlRef: String =
            ControlCancelStage
    }

    data class Remove(
        val draftRef: String,
    ) : Control {
        override val controlRef: String =
            ControlRemove
    }

    sealed interface Effect : VexFamilyContract.Effect<VexLifeAttachmentDraftFamily> {
        data object RequestFiles : Effect

        data class CancelStage(
            val draftRef: String,
        ) : Effect

        data class RemoveFromDraft(
            val draftRef: String,
        ) : Effect
    }

    @Serializable
    data class ItemDisplay(
        val draftRef: String,
        val displayName: String,
        val status: VexLifeText,
        val controlRef: String?,
        val controlLabel: VexLifeText?,
        val completedBytes: Long,
        val totalBytesOrNull: Long?,
    ) {
        init {
            require(
                (controlRef == null) ==
                    (controlLabel == null),
            ) {
                "attachment draft item Control and label must be present together"
            }
        }
    }

    @Serializable
    data class DisplayState(
        val addControlRef: String?,
        val addLabel: VexLifeText?,
        val limitStatus: VexLifeText?,
        val items: List<ItemDisplay>,
        val requestResourcesReady: Boolean,
    ) : VexFamilyContract.DisplayState<VexLifeAttachmentDraftFamily, State> {
        init {
            require(
                (addControlRef == null) ==
                    (addLabel == null),
            ) {
                "attachment draft Add Control and label must be present together"
            }
        }
    }

    override fun act(
        currentState: State,
        control: Control,
    ): Act<VexLifeAttachmentDraftFamily, State, Effect>? =
        when (
            control
        ) {
            AddFiles ->
                if (
                    currentState.items.size <
                    VexLifeLocalArtifact.MaximumPerRequest
                ) {
                    stay(
                        Effect.RequestFiles,
                    )
                } else {
                    null
                }

            is CancelStage ->
                currentState.items
                    .singleOrNull {
                        it.draftRef ==
                            control.draftRef &&
                            it.status ==
                            ItemStatus.STAGING
                    }
                    ?.let {
                        stay(
                            Effect.CancelStage(
                                draftRef =
                                    control.draftRef,
                            ),
                        )
                    }

            is Remove ->
                currentState.items
                    .singleOrNull {
                        it.draftRef ==
                            control.draftRef &&
                            it.status !=
                            ItemStatus.STAGING
                    }
                    ?.let {
                        stay(
                            Effect.RemoveFromDraft(
                                draftRef =
                                    control.draftRef,
                            ),
                        )
                    }
        }

    override fun displayStateFor(
        state: State,
    ): DisplayState {
        val canAdd =
            state.items.size <
                VexLifeLocalArtifact.MaximumPerRequest

        return DisplayState(
            addControlRef =
                ControlAddFiles.takeIf {
                    canAdd
                },
            addLabel =
                VexLifeText(
                    "workspace.button.attach",
                ).takeIf {
                    canAdd
                },
            limitStatus =
                VexLifeText(
                    "workspace.status.attachment-limit",
                ).takeIf {
                    !canAdd
                },
            items =
                state.items.map {
                    item ->
                    val control =
                        when (
                            item.status
                        ) {
                            ItemStatus.STAGING ->
                                ControlCancelStage

                            ItemStatus.READY,
                            ItemStatus.HELD,
                            ->
                                ControlRemove
                        }
                    ItemDisplay(
                        draftRef = item.draftRef,
                        displayName = item.displayName,
                        status =
                            VexLifeText(
                                when (
                                    item.status
                                ) {
                                    ItemStatus.STAGING ->
                                        "workspace.status.attachment-staging"

                                    ItemStatus.READY ->
                                        "workspace.status.attachment-ready"

                                    ItemStatus.HELD ->
                                        requireNotNull(
                                            item.holdReasonKeyOrNull,
                                        )
                                },
                            ),
                        controlRef = control,
                        controlLabel =
                            VexLifeText(
                                if (
                                    control ==
                                        ControlCancelStage
                                ) {
                                    "workspace.button.attachment-cancel"
                                } else {
                                    "workspace.button.attachment-remove"
                                },
                            ),
                        completedBytes = item.completedBytes,
                        totalBytesOrNull = item.totalBytesOrNull,
                    )
                },
            requestResourcesReady =
                requestAttachments(
                    state,
                ) !=
                null,
        )
    }

    /**
     * Snapshot the exact resource bindings that may accompany Send.
     *
     * null means at least one staged item is not ready and Send must not silently omit it.
     */
    fun requestAttachments(
        state: State,
    ): List<VexLifeLocalArtifact.RequestAttachment>? {
        if (
            state.items.any {
                it.status !=
                    ItemStatus.READY
            }
        ) {
            return null
        }

        return state.items.map {
            item ->
            VexLifeLocalArtifact.RequestAttachment(
                artifactRef =
                    requireNotNull(
                        item.artifactRef,
                    ),
                projectionRef =
                    requireNotNull(
                        item.projectionRef,
                    ),
                displayName =
                    item.displayName,
            )
        }
    }

    fun state(
        items: List<Item>,
    ): State =
        if (
            items.isEmpty()
        ) {
            Empty
        } else {
            Draft(
                items.toList(),
            )
        }

    fun controlForRef(
        controlRef: String,
        draftRef: String? = null,
    ): Control? =
        when (
            controlRef
        ) {
            ControlAddFiles ->
                AddFiles

            ControlCancelStage ->
                draftRef?.let(
                    ::CancelStage,
                )

            ControlRemove ->
                draftRef?.let(
                    ::Remove,
                )

            else ->
                null
        }

    const val StateEmpty =
        "EMPTY"

    const val StateDraft =
        "DRAFT"

    const val ControlAddFiles =
        "ADD_FILES"

    const val ControlCancelStage =
        "CANCEL_STAGE"

    const val ControlRemove =
        "REMOVE_ATTACHMENT"

    val ItemStatusRefs: Set<String> =
        ItemStatus.entries
            .map(
                ItemStatus::name,
            )
            .toSet()

    val ControlRefs: Set<String> =
        setOf(
            ControlAddFiles,
            ControlCancelStage,
            ControlRemove,
        )
}

// [VXG RealForever]
