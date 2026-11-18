package vexlife.localasr

import kotlinx.serialization.Serializable

/**
 * The person's selected local speech-to-text realization.
 *
 * This is deliberately domain-local rather than a generic configuration foundation.
 * Selection is separate from provider/model currentness and from one active VoiceInput session.
 */
object VexLifeLocalAsrSelection {
    const val Schema = "vexlife-prod.local-asr-selection/v1"

    const val WhisperCppProviderRef = "provider.local-asr.whispercpp"
    const val WhisperBaseEnglishModelRef = "model.local-asr.whisper.base.en"
    const val WhisperBaseMultilingualModelRef = "model.local-asr.whisper.base"

    @Serializable
    data class Option(
        val providerRef: String,
        val modelRef: String,
        val providerLabel: String,
        val modelLabel: String,
        val languageCodes: List<String>,
        val modelBytes: Long,
        val standing: String,
    )

    val WhisperBaseEnglish =
        Option(
            providerRef = WhisperCppProviderRef,
            modelRef = WhisperBaseEnglishModelRef,
            providerLabel = "whisper.cpp",
            modelLabel = "Whisper Base English",
            languageCodes = listOf("en"),
            modelBytes = 147_964_211L,
            standing = "QUALIFIED_A002_A003",
        )

    val WhisperBaseMultilingual =
        Option(
            providerRef = WhisperCppProviderRef,
            modelRef = WhisperBaseMultilingualModelRef,
            providerLabel = "whisper.cpp",
            modelLabel = "Whisper Base Multilingual",
            languageCodes =
                listOf(
                    "en",
                    "ja",
                    "zh",
                    "ko",
                    "fr",
                    "de",
                    "es",
                    "pt",
                    "ar",
                    "hi",
                ),
            modelBytes = 147_951_465L,
            standing = "CANDIDATE_MULTILINGUAL_A001",
        )

    /** Only combinations with lived provider evidence are exposed as selectable. */
    val Options: List<Option> =
        listOf(
            WhisperBaseEnglish,
            WhisperBaseMultilingual,
        )

    @Serializable
    data class Value(
        val schemaVersion: String,
        val providerRef: String,
        val modelRef: String,
    ) {
        init {
            require(schemaVersion == Schema) { "local ASR selection schema is not current" }
            require(option(providerRef, modelRef) != null) {
                "local ASR selection is not in the qualified Product catalog"
            }
        }
    }

    val Default =
        Value(
            schemaVersion = Schema,
            providerRef = WhisperBaseEnglish.providerRef,
            modelRef = WhisperBaseEnglish.modelRef,
        )

    fun option(
        providerRef: String,
        modelRef: String,
    ): Option? =
        Options.firstOrNull {
            it.providerRef == providerRef &&
                it.modelRef == modelRef
        }

    fun option(value: Value): Option =
        requireNotNull(
            option(
                value.providerRef,
                value.modelRef,
            ),
        )
}

// [VXG RealForever]
