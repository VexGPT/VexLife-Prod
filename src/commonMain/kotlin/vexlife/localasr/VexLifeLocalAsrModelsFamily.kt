package vexlife.localasr

import vexlife.semantic.VexFamilyContract

/**
 * Product semantic owner for one offered local speech-recognition model/provider relationship.
 *
 * This Family answers only which human Control is lawful against the exact observed speech-model
 * and provider State, and which inert Effect that Control requests. It does not download files,
 * choose a platform realization, start whisper.cpp, write selection storage, log chronology, or
 * render Settings.
 *
 * Platform mechanics remain below [VexLifeLocalAsrProviderRealization]. The stable string refs stay
 * as wire/API coordinates so existing service projections remain compatible.
 */
object VexLifeLocalAsrModelsFamily :
    VexFamilyContract<
        VexLifeLocalAsrModelsFamily,
        VexLifeLocalAsrModelsFamily.State,
        VexLifeLocalAsrModelsFamily.Control,
        VexLifeLocalAsrModelsFamily.Effect,
    >() {

    const val ArtifactAbsent = "ABSENT"
    const val ArtifactPartial = "PARTIAL"
    const val ArtifactPresent = "PRESENT"
    const val ArtifactDamaged = "DAMAGED"

    data class State(
        val artifactState: String,
        val selected: Boolean,
        val downloading: Boolean,
        val anyDownloadRunning: Boolean,
        val providerCurrent: Boolean = true,
        val providerAcquisitionSupported: Boolean = false,
    ) : VexFamilyContract.State<VexLifeLocalAsrModelsFamily> {
        init {
            require(
                artifactState in
                    setOf(
                        ArtifactAbsent,
                        ArtifactPartial,
                        ArtifactPresent,
                        ArtifactDamaged,
                    ),
            ) {
                "unknown local ASR artifact state " +
                    artifactState
            }
        }

        val idle: Boolean
            get() =
                !downloading &&
                    !anyDownloadRunning
    }

    sealed interface Control :
        VexFamilyContract.Control<VexLifeLocalAsrModelsFamily> {
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

    data object InstallProvider : Control {
        override val controlRef: String = ControlInstallProvider
    }

    sealed interface Effect :
        VexFamilyContract.Effect<VexLifeLocalAsrModelsFamily>

    data object SelectSpeechModel : Effect

    data class AcquireSpeechModel(
        val replace: Boolean,
    ) : Effect

    data object RemoveSpeechModel : Effect

    data object AcquireSpeechProvider : Effect

    override fun act(
        currentState: State,
        control: Control,
    ): Act<
        VexLifeLocalAsrModelsFamily,
        State,
        Effect
    >? =
        when (control) {
            Use ->
                if (
                    currentState.artifactState ==
                        ArtifactPresent &&
                    !currentState.selected &&
                    currentState.idle
                ) {
                    stay(
                        SelectSpeechModel,
                    )
                } else {
                    null
                }

            Download ->
                if (
                    currentState.artifactState in
                        setOf(
                            ArtifactAbsent,
                            ArtifactPartial,
                        ) &&
                    currentState.idle
                ) {
                    stay(
                        AcquireSpeechModel(
                            replace =
                                false,
                        ),
                    )
                } else {
                    null
                }

            Redownload ->
                if (
                    currentState.artifactState ==
                        ArtifactDamaged &&
                    currentState.idle
                ) {
                    stay(
                        AcquireSpeechModel(
                            replace =
                                true,
                        ),
                    )
                } else {
                    null
                }

            Remove ->
                if (
                    currentState.artifactState ==
                        ArtifactPresent &&
                    !currentState.selected &&
                    currentState.idle
                ) {
                    stay(
                        RemoveSpeechModel,
                    )
                } else {
                    null
                }

            InstallProvider ->
                if (
                    currentState.artifactState ==
                        ArtifactPresent &&
                    currentState.selected &&
                    !currentState.providerCurrent &&
                    currentState.providerAcquisitionSupported &&
                    currentState.idle
                ) {
                    stay(
                        AcquireSpeechProvider,
                    )
                } else {
                    null
                }
        }

    fun lawfulControlValues(
        state: State,
    ): List<Control> =
        Controls.filter {
            resolve(
                currentState =
                    state,
                control =
                    it,
            ) !=
                null
        }

    /** Stable wire/API compatibility projection. */
    fun lawfulControls(
        state: State,
    ): List<String> =
        lawfulControlValues(
            state,
        )
            .map(
                Control::controlRef,
            )

    fun controlForRef(
        controlRef: String,
    ): Control? =
        ControlByRef[controlRef]

    fun labelKey(
        control: Control,
    ): String =
        when (control) {
            Use ->
                "workspace.button.model-use"

            Download ->
                "workspace.button.model-download"

            Redownload ->
                "workspace.button.model-redownload"

            Remove ->
                "workspace.button.model-remove"

            InstallProvider ->
                "workspace.button.local-asr-provider-install"
        }

    fun labelKey(
        controlRef: String,
    ): String =
        labelKey(
            requireNotNull(
                controlForRef(
                    controlRef,
                ),
            ) {
                "unknown local ASR model control " +
                    controlRef
            },
        )

    const val ControlUse = "USE_SPEECH_MODEL"
    const val ControlDownload = "DOWNLOAD_SPEECH_MODEL"
    const val ControlRedownload = "REDOWNLOAD_SPEECH_MODEL"
    const val ControlRemove = "REMOVE_SPEECH_MODEL"
    const val ControlInstallProvider = "INSTALL_SPEECH_PROVIDER"

    val ControlRefs: Set<String> =
        setOf(
            ControlUse,
            ControlDownload,
            ControlRedownload,
            ControlRemove,
            ControlInstallProvider,
        )

    private val Controls: List<Control> =
        listOf(
            Use,
            Download,
            Redownload,
            Remove,
            InstallProvider,
        )

    private val ControlByRef: Map<String, Control> =
        Controls.associateBy(
            Control::controlRef,
        )
}

// [VXG RealForever]
