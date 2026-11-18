package vexlife.model

import kotlinx.serialization.Serializable
import vexlife.semantic.VexFamilyContract
import vexlife.model.heretic.HereticRuntimeConfiguration
import vexlife.text.VexLifeText

/**
 * Product semantic owner for what the person can do with one offered model in Settings → Models.
 *
 * State is one model's relationship to this Home: whether its required artifacts are present,
 * whether a download is running, whether the Home chose it, and whether the running runtime serves it.
 *
 * Lawfulness keeps target choice, launch configuration and activation distinct:
 * ```
 * Select      stage the Settings target only; no Home/runtime effect
 * Configure   persist launch configuration for the staged target; no runtime activation
 * Use/Start   staged + artifacts present + not current, or current config pending -> SwitchModel
 * Download    artifacts absent or partial, nothing running -> AcquireArtifacts
 * Redownload  artifacts damaged, nothing running           -> ReplaceArtifacts
 * Remove      artifacts here, not chosen, not serving, idle -> RemoveArtifacts
 * ```
 *
 * Use is now an explicit activation Control for an already-selected target. The Family owns that
 * human meaning; concrete runtime convergence remains with LocalSetup/runtime owners.
 */
object VexLifeModelsFamily :
    VexFamilyContract<
        VexLifeModelsFamily,
        VexLifeModelsFamily.State,
        VexLifeModelsFamily.Control,
        VexLifeModelsFamily.Effect,
    >() {
    sealed interface ArtifactPresence {
        val stateRef: String
        val statusKey: String

        data object Absent : ArtifactPresence {
            override val stateRef = "ABSENT"
            override val statusKey = "workspace.status.model-not-downloaded"
        }

        data object Partial : ArtifactPresence {
            override val stateRef = "PARTIAL"
            override val statusKey = "workspace.status.model-partly-downloaded"
        }

        data object Present : ArtifactPresence {
            override val stateRef = "PRESENT"
            override val statusKey = "workspace.status.model-on-this-computer"
        }

        data object Damaged : ArtifactPresence {
            override val stateRef = "DAMAGED"
            override val statusKey = "workspace.status.model-damaged"
        }
    }

    data class State(
        val handle: String,
        val artifactPresence: ArtifactPresence,
        val downloading: Boolean,
        val chosen: Boolean,
        /** Settings-stage target. This may differ from the durable Home choice until Start. */
        val targeted: Boolean,
        val serving: Boolean,
        val switching: Boolean,
        /**
         * Exact effective launch configuration when the selected runtime realization participates
         * in this configuration relationship. Null means configuration is not applicable here.
         */
        val runtimeConfiguration: HereticRuntimeConfiguration.Effective?,
        /** The selected/serving model has configured launch values not active in the current runtime. */
        val runtimeConfigurationPending: Boolean = false,
        /** Any model's download is running (one at a time per Home). */
        val anyDownloadRunning: Boolean,
        /** One model-switch convergence is active for this Home. */
        val anySwitchRunning: Boolean,
    ) : VexFamilyContract.State<VexLifeModelsFamily>

    sealed interface Control : VexFamilyContract.Control<VexLifeModelsFamily> {
        val controlRef: String
    }

    data object Select : Control {
        override val controlRef: String = ControlSelect
    }

    /** Explicitly start/restart the already-selected target through the runtime owner. */
    data object Use : Control {
        override val controlRef: String = ControlUse
    }

    data object UseRecommendedRuntimeConfiguration : Control {
        override val controlRef: String = ControlUseRecommendedRuntimeConfiguration
    }

    data class SetPromptCacheEntries(
        val entries: Int,
    ) : Control {
        override val controlRef: String = ControlSetPromptCacheEntries

        init {
            HereticRuntimeConfiguration.requirePromptCacheEntries(entries)
        }
    }

    data object Download : Control {
        override val controlRef: String = ControlDownload
    }

    data object Redownload : Control {
        override val controlRef: String = ControlRedownload
    }

    data object Remove : Control {
        override val controlRef: String = ControlRemove
    }

    sealed interface Effect : VexFamilyContract.Effect<VexLifeModelsFamily> {
        data object SwitchModel : Effect

        data object UseRecommendedRuntimeConfiguration : Effect

        data class SetPromptCacheEntries(
            val entries: Int,
        ) : Effect

        data object AcquireArtifacts : Effect

        /** Replace this model's own pinned artifact bytes through the materialization owner. */
        data object ReplaceArtifacts : Effect

        data object RemoveArtifacts : Effect
    }

    @Serializable data class ControlDisplay(val controlRef: String, val label: VexLifeText)

    /**
     * One human-facing choice for the existing runtime-configuration relationship.
     *
     * [controlRef] is null when choosing this value would not change current semantic meaning.
     * The presentation renders this projection; it does not classify numeric cache values itself.
     */
    @Serializable
    data class RuntimeConfigurationChoiceDisplay(
        val promptCacheEntries: Int,
        val label: VexLifeText,
        val standing: VexLifeText,
        val selected: Boolean,
        val controlRef: String?,
    )

    /** Language-free: every word is a key the presentation resolves. */
    @Serializable
    data class DisplayState(
        val handle: String,
        val status: VexLifeText,
        val notes: List<VexLifeText>,
        val controls: List<ControlDisplay>,
        val runtimeConfigurationApplicable: Boolean,
        val runtimeConfigurationSummary: VexLifeText?,
        val runtimeConfigurationRecommendation: VexLifeText?,
        val runtimeConfigurationChoices: List<RuntimeConfigurationChoiceDisplay>,
    ) : VexFamilyContract.DisplayState<VexLifeModelsFamily, State>

    override fun act(
        currentState: State,
        control: Control,
    ): Act<VexLifeModelsFamily, State, Effect>? {
        val idle =
            !currentState.downloading &&
                !currentState.anyDownloadRunning &&
                !currentState.anySwitchRunning
        return when (control) {
            Select ->
                if (
                    !currentState.targeted &&
                        idle
                ) {
                    admit(
                        currentState.copy(
                            targeted = true,
                        ),
                    )
                } else {
                    null
                }
            Use ->
                if (
                    currentState.artifactPresence == ArtifactPresence.Present &&
                        currentState.targeted &&
                        (
                            !currentState.chosen ||
                                !currentState.serving ||
                                currentState.runtimeConfigurationPending
                        ) &&
                        !currentState.switching &&
                        idle
                ) {
                    stay(Effect.SwitchModel)
                } else {
                    null
                }
            UseRecommendedRuntimeConfiguration -> {
                val runtimeConfiguration =
                    currentState.runtimeConfiguration
                        ?: return null
                if (
                    currentState.targeted &&
                        (
                            runtimeConfiguration.promptCacheEntries !=
                                runtimeConfiguration.recommendedPromptCacheEntries ||
                                runtimeConfiguration.mode ==
                                    HereticRuntimeConfiguration.ModeLegacyRecommended
                        ) &&
                        idle
                ) {
                    stay(Effect.UseRecommendedRuntimeConfiguration)
                } else {
                    null
                }
            }
            is SetPromptCacheEntries -> {
                val runtimeConfiguration =
                    currentState.runtimeConfiguration
                        ?: return null
                if (
                    currentState.targeted &&
                        control.entries in
                            runtimeConfiguration.minimumPromptCacheEntries..
                                runtimeConfiguration.maximumPromptCacheEntries &&
                        (
                            runtimeConfiguration.mode !=
                                HereticRuntimeConfiguration.ModeExplicit ||
                                runtimeConfiguration.promptCacheEntries != control.entries
                        ) &&
                        idle
                ) {
                    stay(Effect.SetPromptCacheEntries(control.entries))
                } else {
                    null
                }
            }
            Download ->
                if (
                    (
                        currentState.artifactPresence == ArtifactPresence.Absent ||
                            currentState.artifactPresence == ArtifactPresence.Partial
                    ) &&
                        idle
                )
                    stay(Effect.AcquireArtifacts)
                else null
            Redownload ->
                if (
                    currentState.artifactPresence == ArtifactPresence.Damaged &&
                        idle &&
                        !currentState.serving
                )
                    stay(Effect.ReplaceArtifacts)
                else null
            Remove ->
                if (
                    currentState.artifactPresence != ArtifactPresence.Absent &&
                        !currentState.chosen &&
                        !currentState.targeted &&
                        !currentState.serving &&
                        idle
                )
                    stay(Effect.RemoveArtifacts)
                else null
        }
    }

    override fun displayStateFor(state: State): DisplayState {
        val runtimeConfiguration =
            state.runtimeConfiguration
        val currentLevel =
            runtimeConfiguration
                ?.let {
                    VexLifeText(
                        cacheLevelKey(
                            it.promptCacheEntries,
                        ),
                    )
                }
        val recommendedLevel =
            runtimeConfiguration
                ?.let {
                    VexLifeText(
                        cacheLevelKey(
                            it.recommendedPromptCacheEntries,
                        ),
                    )
                }

        return DisplayState(
            handle = state.handle,
            status =
                when {
                    state.switching -> VexLifeText("workspace.status.model-switching")
                    state.downloading -> VexLifeText("workspace.status.model-downloading")
                    else -> VexLifeText(state.artifactPresence.statusKey)
                },
            notes =
                listOfNotNull(
                    VexLifeText("workspace.label.model-serving").takeIf { state.serving },
                    VexLifeText("workspace.label.model-chosen-pending").takeIf {
                        state.chosen && !state.serving
                    },
                    VexLifeText("workspace.label.model-chosen").takeIf {
                        state.chosen && state.serving
                    },
                ),
            controls =
                listOf(Select, Use, UseRecommendedRuntimeConfiguration, Download, Redownload, Remove)
                    .filter { act(state, it) != null }
                    .map {
                        ControlDisplay(
                            controlRef = it.controlRef,
                            label =
                                VexLifeText(
                                    labelKey(
                                        control = it,
                                        state = state,
                                    ),
                                ),
                        )
                    },
            runtimeConfigurationApplicable =
                runtimeConfiguration !=
                    null,
            runtimeConfigurationSummary =
                runtimeConfiguration
                    ?.let {
                        if (
                            it.promptCacheEntries ==
                                it.recommendedPromptCacheEntries
                        ) {
                            VexLifeText(
                                key = "workspace.label.model-cache-current-recommended",
                                parts = mapOf("current" to requireNotNull(currentLevel)),
                            )
                        } else {
                            VexLifeText(
                                key = "workspace.label.model-cache-current",
                                parts =
                                    mapOf(
                                        "current" to requireNotNull(currentLevel),
                                        "recommended" to requireNotNull(recommendedLevel),
                                    ),
                            )
                        }
                    },
            runtimeConfigurationRecommendation =
                runtimeConfiguration
                    ?.let {
                        VexLifeText(
                            it.recommendationReasonKey,
                        )
                    },
            runtimeConfigurationChoices =
                runtimeConfiguration
                    ?.let {
                        configuration ->
                        (
                            configuration.minimumPromptCacheEntries..
                                configuration.maximumPromptCacheEntries
                        ).map { value ->
                            val control =
                                SetPromptCacheEntries(
                                    value,
                                )
                            RuntimeConfigurationChoiceDisplay(
                                promptCacheEntries = value,
                                label = VexLifeText(cacheLevelKey(value)),
                                standing =
                                    VexLifeText(
                                        choiceStandingKey(
                                            value = value,
                                            runtimeConfiguration = configuration,
                                        ),
                                    ),
                                selected = value == configuration.promptCacheEntries,
                                controlRef =
                                    control.controlRef.takeIf {
                                        act(
                                            state,
                                            control,
                                        ) != null
                                    },
                            )
                        }
                    }
                    .orEmpty(),
        )
    }

    private fun labelKey(
        control: Control,
        state: State,
    ): String =
        when (control) {
            Select -> "workspace.button.model-select"
            Use ->
                if (
                    state.serving &&
                        state.runtimeConfigurationPending
                ) {
                    "workspace.button.model-apply-restart"
                } else {
                    "workspace.button.model-start"
                }
            UseRecommendedRuntimeConfiguration -> "workspace.button.model-use-recommended"
            is SetPromptCacheEntries -> "workspace.button.model-set-prompt-cache"
            Download -> "workspace.button.model-download"
            Redownload -> "workspace.button.model-redownload"
            Remove -> "workspace.button.model-remove"
        }

    private fun cacheLevelKey(value: Int): String =
        when (value) {
            1 -> "workspace.label.model-cache-level-1"
            2 -> "workspace.label.model-cache-level-2"
            3 -> "workspace.label.model-cache-level-3"
            else ->
                error(
                    "runtime configuration value $value has no human-facing Models level",
                )
        }

    private fun choiceStandingKey(
        value: Int,
        runtimeConfiguration: HereticRuntimeConfiguration.Effective,
    ): String =
        when {
            value == runtimeConfiguration.recommendedPromptCacheEntries ->
                "workspace.label.model-recommended"

            runtimeConfiguration.recommendationStanding ==
                HereticRuntimeConfiguration.RecommendationHeldExperiment &&
                value > runtimeConfiguration.recommendedPromptCacheEntries ->
                "workspace.label.model-experimental"

            else ->
                error(
                    "runtime configuration value $value has no admitted human-facing standing",
                )
        }

    fun controlForRef(
        controlRef: String,
        promptCacheEntries: Int? = null,
    ): Control? =
        when (controlRef) {
            ControlSelect -> Select
            ControlUse -> Use
            ControlUseRecommendedRuntimeConfiguration -> UseRecommendedRuntimeConfiguration
            ControlSetPromptCacheEntries ->
                promptCacheEntries?.let(::SetPromptCacheEntries)
            ControlDownload -> Download
            ControlRedownload -> Redownload
            ControlRemove -> Remove
            else -> null
        }

    const val ControlSelect = "SELECT_MODEL"
    const val ControlUse = "USE_MODEL"
    const val ControlUseRecommendedRuntimeConfiguration = "USE_RECOMMENDED_RUNTIME_CONFIGURATION"
    const val ControlSetPromptCacheEntries = "SET_PROMPT_CACHE_ENTRIES"
    const val ControlDownload = "DOWNLOAD_MODEL"
    const val ControlRedownload = "REDOWNLOAD_MODEL"
    const val ControlRemove = "REMOVE_MODEL"
}

// [VXG RealForever]
