package vexlife.localasr

import kotlinx.serialization.Serializable
import vexlife.person.VexLifePersonSettings

/**
 * The person's microphone-language preference.
 *
 * This is intentionally separate from both Vex reply language and the selected ASR model artifact:
 * a person may speak one language and ask Vex to reply in another.
 */
object VexLifeLocalAsrSpokenLanguage {
    const val Schema = "vexlife-prod.local-asr-spoken-language/v1"

    const val FollowReplyLanguage = "FOLLOW_REPLY_LANGUAGE"
    const val AutoDetect = "AUTO_DETECT"
    const val Fixed = "FIXED"

    val Modes =
        setOf(
            FollowReplyLanguage,
            AutoDetect,
            Fixed,
        )

    @Serializable
    data class Value(
        val schemaVersion: String,
        val mode: String,
        val languageCode: String? = null,
    ) {
        init {
            require(schemaVersion == Schema) {
                "local ASR spoken-language schema is not current"
            }
            require(mode in Modes) {
                "local ASR spoken-language mode is unsupported"
            }
            when (mode) {
                FollowReplyLanguage,
                AutoDetect,
                -> require(languageCode == null) {
                    mode + " must not persist one fixed language"
                }

                Fixed -> {
                    requireNotNull(languageCode) {
                        "fixed local ASR spoken language requires languageCode"
                    }
                    VexLifePersonSettings.languageName(
                        languageCode,
                    )
                }
            }
        }
    }

    val Default =
        Value(
            schemaVersion = Schema,
            mode = FollowReplyLanguage,
        )

    fun resolvedLanguageCode(
        value: Value,
        replyLanguage: String,
    ): String =
        when (value.mode) {
            FollowReplyLanguage ->
                replyLanguage

            AutoDetect ->
                "auto"

            Fixed ->
                requireNotNull(
                    value.languageCode,
                )

            else ->
                error(
                    "unsupported local ASR spoken-language mode " +
                        value.mode,
                )
        }
}

// [VXG RealForever]
