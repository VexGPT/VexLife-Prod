package vexlife.localasr

import kotlinx.serialization.Serializable

/**
 * What Local ASR places into the editable Composer.
 *
 * This is separate from spoken-language selection and from Vex reply language. As-spoken preserves
 * multilingual/code-switched dictation. Translate-to-English uses Whisper's native translation task
 * and is therefore offered only by a multilingual speech model.
 */
object VexLifeLocalAsrComposerOutput {
    const val Schema = "vexlife-prod.local-asr-composer-output/v1"

    const val AsSpoken = "AS_SPOKEN"
    const val TranslateToEnglish = "TRANSLATE_TO_ENGLISH"

    val Modes =
        setOf(
            AsSpoken,
            TranslateToEnglish,
        )

    @Serializable
    data class Value(
        val schemaVersion: String,
        val mode: String,
    ) {
        init {
            require(schemaVersion == Schema) {
                "local ASR Composer-output schema is not current"
            }
            require(mode in Modes) {
                "local ASR Composer-output mode is unsupported"
            }
        }
    }

    val Default =
        Value(
            schemaVersion = Schema,
            mode = AsSpoken,
        )

    fun supportedBy(
        modelRef: String,
        mode: String,
    ): Boolean =
        when (mode) {
            AsSpoken -> true
            TranslateToEnglish ->
                modelRef ==
                    VexLifeLocalAsrSelection.WhisperBaseMultilingualModelRef
            else -> false
        }

    fun labelKey(mode: String): String =
        when (mode) {
            AsSpoken -> "workspace.label.local-asr-output-as-spoken"
            TranslateToEnglish -> "workspace.label.local-asr-output-translate-english"
            else -> error("unknown local ASR Composer-output mode $mode")
        }
}

// [VXG RealForever]
