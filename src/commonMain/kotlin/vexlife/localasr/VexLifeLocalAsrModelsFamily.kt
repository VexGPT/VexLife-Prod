package vexlife.localasr

/**
 * Human Controls for one offered local speech-recognition model.
 *
 * This mirrors the storage semantics of Settings → Models without importing LLM runtime lifecycle:
 * speech-model Use affects the next dictation session and requires no service restart.
 */
object VexLifeLocalAsrModelsFamily {
    const val ArtifactAbsent = "ABSENT"
    const val ArtifactPartial = "PARTIAL"
    const val ArtifactPresent = "PRESENT"
    const val ArtifactDamaged = "DAMAGED"

    const val ControlUse = "USE_SPEECH_MODEL"
    const val ControlDownload = "DOWNLOAD_SPEECH_MODEL"
    const val ControlRedownload = "REDOWNLOAD_SPEECH_MODEL"
    const val ControlRemove = "REMOVE_SPEECH_MODEL"

    data class State(
        val artifactState: String,
        val selected: Boolean,
        val downloading: Boolean,
        val anyDownloadRunning: Boolean,
    )

    fun lawfulControls(
        state: State,
    ): List<String> {
        val idle =
            !state.downloading &&
                !state.anyDownloadRunning
        return buildList {
            if (
                state.artifactState ==
                    ArtifactPresent &&
                !state.selected &&
                idle
            ) {
                add(
                    ControlUse,
                )
            }
            if (
                (
                    state.artifactState ==
                        ArtifactAbsent ||
                        state.artifactState ==
                        ArtifactPartial
                ) &&
                idle
            ) {
                add(
                    ControlDownload,
                )
            }
            if (
                state.artifactState ==
                    ArtifactDamaged &&
                idle
            ) {
                add(
                    ControlRedownload,
                )
            }
            if (
                state.artifactState ==
                    ArtifactPresent &&
                !state.selected &&
                idle
            ) {
                add(
                    ControlRemove,
                )
            }
        }
    }

    fun labelKey(
        controlRef: String,
    ): String =
        when (controlRef) {
            ControlUse ->
                "workspace.button.model-use"

            ControlDownload ->
                "workspace.button.model-download"

            ControlRedownload ->
                "workspace.button.model-redownload"

            ControlRemove ->
                "workspace.button.model-remove"

            else ->
                error(
                    "unknown local ASR model control " +
                        controlRef,
                )
        }
}

// [VXG RealForever]
