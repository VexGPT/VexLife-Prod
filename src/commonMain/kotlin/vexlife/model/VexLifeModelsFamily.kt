package vexlife.model

import kotlinx.serialization.Serializable
import vexlife.semantic.VexFamilyContract
import vexlife.text.VexLifeText

/**
 * Product semantic owner for what the person can do with one offered model in Settings → Models.
 *
 * State is one model's relationship to this Home: whether its required artifacts are present, whether a download is
 * running, whether the Home chose it, whether the running runtime serves it.
 *
 * Lawfulness (what each Control may do, by State):
 * ```
 * Use        artifacts present, chosen/serving not both current -> SwitchModel
 * Download   artifacts absent or partial, nothing running       -> AcquireArtifacts (resumes a partial)
 * Redownload artifacts damaged, nothing running                 -> ReplaceArtifacts
 * Remove     artifacts here, not chosen, not serving, idle      -> RemoveArtifacts
 * ```
 *
 * Use means the person's intention is for this model to become the model serving Vex now. It stays
 * lawful when a prior switch failed (chosen but not serving), and for the currently-serving model
 * when another target is chosen (so the person can cancel/revert the pending relationship). The
 * Family owns this meaning; concrete runtime convergence remains with LocalSetup/runtime owners.
 */
object VexLifeModelsFamily :
    VexFamilyContract<
        VexLifeModelsFamily,
        VexLifeModelsFamily.State,
        VexLifeModelsFamily.Control,
        VexLifeModelsFamily.Effect,
    >() {
    enum class ArtifactPresence {
        ABSENT,
        PARTIAL,
        PRESENT,
        DAMAGED,
    }

    data class State(
        val handle: String,
        val artifactPresence: ArtifactPresence,
        val downloading: Boolean,
        val chosen: Boolean,
        val serving: Boolean,
        val switching: Boolean,
        /** Any model's download is running (one at a time per Home). */
        val anyDownloadRunning: Boolean,
        /** One model-switch convergence is active for this Home. */
        val anySwitchRunning: Boolean,
    ) : VexFamilyContract.State<VexLifeModelsFamily>

    sealed interface Control : VexFamilyContract.Control<VexLifeModelsFamily> {
        val controlRef: String
    }

    data object Use : Control {
        override val controlRef: String = ControlUse
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

        data object AcquireArtifacts : Effect

        /** Replace this model's own pinned artifact bytes through the materialization owner. */
        data object ReplaceArtifacts : Effect

        data object RemoveArtifacts : Effect
    }

    @Serializable data class ControlDisplay(val controlRef: String, val label: VexLifeText)

    /** Language-free: every word is a key the presentation resolves. */
    @Serializable
    data class DisplayState(
        val handle: String,
        val status: VexLifeText,
        val notes: List<VexLifeText>,
        val controls: List<ControlDisplay>,
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
            Use ->
                if (
                    currentState.artifactPresence == ArtifactPresence.PRESENT &&
                        !(currentState.chosen && currentState.serving) &&
                        !currentState.switching &&
                        idle
                ) {
                    stay(Effect.SwitchModel)
                } else {
                    null
                }
            Download ->
                if (
                    (currentState.artifactPresence == ArtifactPresence.ABSENT || currentState.artifactPresence == ArtifactPresence.PARTIAL) &&
                        idle
                )
                    stay(Effect.AcquireArtifacts)
                else null
            Redownload ->
                if (currentState.artifactPresence == ArtifactPresence.DAMAGED && idle && !currentState.serving)
                    stay(Effect.ReplaceArtifacts)
                else null
            Remove ->
                if (
                    currentState.artifactPresence != ArtifactPresence.ABSENT &&
                        !currentState.chosen &&
                        !currentState.serving &&
                        idle
                )
                    stay(Effect.RemoveArtifacts)
                else null
        }
    }

    override fun displayStateFor(state: State): DisplayState =
        DisplayState(
            handle = state.handle,
            status =
                when {
                    state.switching -> VexLifeText("workspace.status.model-switching")
                    state.downloading -> VexLifeText("workspace.status.model-downloading")
                    state.artifactPresence == ArtifactPresence.PRESENT ->
                        VexLifeText("workspace.status.model-on-this-mac")
                    state.artifactPresence == ArtifactPresence.PARTIAL ->
                        VexLifeText("workspace.status.model-partly-downloaded")
                    state.artifactPresence == ArtifactPresence.DAMAGED -> VexLifeText("workspace.status.model-damaged")
                    else -> VexLifeText("workspace.status.model-not-downloaded")
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
                listOf(Use, Download, Redownload, Remove)
                    .filter { act(state, it) != null }
                    .map { ControlDisplay(it.controlRef, VexLifeText(labelKey(it))) },
        )

    private fun labelKey(control: Control): String =
        when (control) {
            Use -> "workspace.button.model-use"
            Download -> "workspace.button.model-download"
            Redownload -> "workspace.button.model-redownload"
            Remove -> "workspace.button.model-remove"
        }

    fun controlForRef(controlRef: String): Control? =
        when (controlRef) {
            ControlUse -> Use
            ControlDownload -> Download
            ControlRedownload -> Redownload
            ControlRemove -> Remove
            else -> null
        }

    const val ControlUse = "USE_MODEL"
    const val ControlDownload = "DOWNLOAD_MODEL"
    const val ControlRedownload = "REDOWNLOAD_MODEL"
    const val ControlRemove = "REMOVE_MODEL"
}

// [VXG RealForever]
