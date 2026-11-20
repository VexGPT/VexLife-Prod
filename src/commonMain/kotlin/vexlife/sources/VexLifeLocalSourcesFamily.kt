package vexlife.sources

import kotlinx.serialization.Serializable
import vexlife.semantic.VexFamilyContract
import vexlife.text.VexLifeText

/**
 * Semantic owner for folders the person explicitly grants VexLife permission to read across turns.
 *
 * This Family owns grant meaning and Controls. The JVM store owns filesystem admission/persistence;
 * the capability owner owns bounded read-only reach.
 *
 * Permanent:
 *
 * ```text
 * PICKED FOLDER != GRANTED FOLDER
 * LOCAL SOURCE != VEX HOME
 * LOCAL SOURCE != TURN ATTACHMENT
 * GRANTED != PRELOADED INTO PROMPT
 * READABLE != WRITABLE
 * ```
 */
object VexLifeLocalSourcesFamily :
    VexFamilyContract<
        VexLifeLocalSourcesFamily,
        VexLifeLocalSourcesFamily.State,
        VexLifeLocalSourcesFamily.Control,
        VexLifeLocalSourcesFamily.Effect,
    >() {
    const val MaximumGrants = 16
    const val SourceRefPrefix = "source.vexlife-prod.folder.sha256."

    private val Sha256 =
        Regex(
            "[0-9a-f]{64}",
        )

    @Serializable
    data class Grant(
        val sourceRef: String,
        val name: String,
        val path: String,
        val grantedAt: String,
    ) {
        init {
            require(
                isSourceRef(
                    sourceRef,
                ),
            ) {
                "local source ref is invalid"
            }
            require(
                name.isNotBlank() &&
                    path.isNotBlank() &&
                    grantedAt.isNotBlank(),
            ) {
                "local source grant is incomplete"
            }
        }
    }

    sealed interface State : VexFamilyContract.State<VexLifeLocalSourcesFamily> {
        val stateRef: String
        val grants: List<Grant>
    }

    data object Empty : State {
        override val stateRef: String =
            StateEmpty
        override val grants: List<Grant> =
            emptyList()
    }

    data class Granted(
        override val grants: List<Grant>,
    ) : State {
        override val stateRef: String =
            StateGranted

        init {
            require(
                grants.isNotEmpty() &&
                    grants.size <=
                    MaximumGrants &&
                    grants.map {
                        it.sourceRef
                    }
                        .distinct()
                        .size ==
                    grants.size,
            ) {
                "local source grants must be non-empty, unique and within the Product limit"
            }
        }
    }

    sealed interface Control : VexFamilyContract.Control<VexLifeLocalSourcesFamily> {
        val controlRef: String
    }

    data object AddFolder : Control {
        override val controlRef: String =
            ControlAddFolder
    }

    data class Remove(
        val sourceRef: String,
    ) : Control {
        init {
            require(
                isSourceRef(
                    sourceRef,
                ),
            ) {
                "local source Remove requires one exact source ref"
            }
        }

        override val controlRef: String =
            ControlRemove
    }

    sealed interface Effect : VexFamilyContract.Effect<VexLifeLocalSourcesFamily> {
        data object RequestFolderSelection : Effect

        data class RemoveGrant(
            val sourceRef: String,
        ) : Effect
    }

    @Serializable
    data class GrantDisplay(
        val sourceRef: String,
        val name: String,
        val path: String,
        val removeControlRef: String,
        val removeLabel: VexLifeText,
    )

    @Serializable
    data class DisplayState(
        val heading: VexLifeText,
        val description: VexLifeText,
        val addControlRef: String?,
        val addLabel: VexLifeText?,
        val grants: List<GrantDisplay>,
        val limitStatus: VexLifeText?,
    ) : VexFamilyContract.DisplayState<VexLifeLocalSourcesFamily, State> {
        init {
            require(
                (
                    addControlRef ==
                        null
                ) ==
                    (
                        addLabel ==
                            null
                    ),
            ) {
                "local sources Add Control and label must be present together"
            }
        }
    }

    override fun act(
        currentState: State,
        control: Control,
    ): Act<VexLifeLocalSourcesFamily, State, Effect>? =
        when (
            control
        ) {
            AddFolder ->
                if (
                    currentState.grants.size <
                    MaximumGrants
                ) {
                    stay(
                        Effect.RequestFolderSelection,
                    )
                } else {
                    null
                }

            is Remove ->
                currentState.grants
                    .singleOrNull {
                        it.sourceRef ==
                            control.sourceRef
                    }
                    ?.let {
                        stay(
                            Effect.RemoveGrant(
                                control.sourceRef,
                            ),
                        )
                    }
        }

    override fun displayStateFor(
        state: State,
    ): DisplayState {
        val canAdd =
            state.grants.size <
                MaximumGrants

        return DisplayState(
            heading =
                VexLifeText(
                    "workspace.heading.local-sources",
                ),
            description =
                VexLifeText(
                    "workspace.description.local-sources",
                ),
            addControlRef =
                ControlAddFolder.takeIf {
                    canAdd
                },
            addLabel =
                VexLifeText(
                    "workspace.button.local-sources-add",
                ).takeIf {
                    canAdd
                },
            grants =
                state.grants.map {
                    grant ->
                    GrantDisplay(
                        sourceRef = grant.sourceRef,
                        name = grant.name,
                        path = grant.path,
                        removeControlRef = ControlRemove,
                        removeLabel =
                            VexLifeText(
                                "workspace.button.local-sources-remove",
                            ),
                    )
                },
            limitStatus =
                VexLifeText(
                    "workspace.status.local-sources-refused-limit-reached",
                ).takeIf {
                    !canAdd
                },
        )
    }

    fun state(
        grants: List<Grant>,
    ): State =
        if (
            grants.isEmpty()
        ) {
            Empty
        } else {
            Granted(
                grants.toList(),
            )
        }

    fun controlForRef(
        controlRef: String,
        sourceRef: String? = null,
    ): Control? =
        when (
            controlRef
        ) {
            ControlAddFolder ->
                AddFolder

            ControlRemove ->
                sourceRef?.let(
                    ::Remove,
                )

            else ->
                null
        }

    fun sourceRefForSha256(
        sha256: String,
    ): String {
        require(
            Sha256.matches(
                sha256,
            ),
        ) {
            "local source identity requires one full lowercase SHA-256"
        }
        return SourceRefPrefix +
            sha256
    }

    fun sha256FromSourceRef(
        sourceRef: String,
    ): String? =
        sourceRef
            .takeIf(
                ::isSourceRef,
            )
            ?.removePrefix(
                SourceRefPrefix,
            )

    fun isSourceRef(
        value: String,
    ): Boolean =
        value.startsWith(
            SourceRefPrefix,
        ) &&
            Sha256.matches(
                value.removePrefix(
                    SourceRefPrefix,
                ),
            )

    const val StateEmpty =
        "EMPTY"

    const val StateGranted =
        "GRANTED"

    const val ControlAddFolder =
        "ADD_FOLDER"

    const val ControlRemove =
        "REMOVE_SOURCE"
}

// [VXG RealForever]
