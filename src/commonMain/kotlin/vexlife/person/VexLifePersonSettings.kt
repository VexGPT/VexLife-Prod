package vexlife.person

import kotlinx.serialization.Serializable

/**
 * Settings the person chooses that Vex is told about, kept in their Vex Home.
 *
 * Reply language is independent from the application's localization and from the microphone's spoken
 * language. Chinese reply script is explicit; both reply variants map back to one spoken Chinese
 * coordinate only at the speech-recognition adapter.
 */
object VexLifePersonSettings {
    const val Schema = "vexlife-prod.person-settings/v1"
    const val DefaultLanguage = "en"
    const val LegacyChinese = "zh"
    const val SimplifiedChinese = "zh-Hans"
    const val TraditionalChinese = "zh-Hant"

    /** Base language vocabulary used by current localization source and fixed microphone choices. */
    val Languages: List<Pair<String, String>> =
        listOf(
            "en" to "English",
            "ja" to "日本語",
            "zh" to "中文",
            "ko" to "한국어",
            "fr" to "Français",
            "de" to "Deutsch",
            "es" to "Español",
            "pt" to "Português",
            "ar" to "العربية",
            "hi" to "हिन्दी",
        )

    /** What the person may ask Vex to reply in. Script-distinct Chinese is one real choice here. */
    val ReplyLanguages: List<Pair<String, String>> =
        listOf(
            "en" to "English",
            "ja" to "日本語",
            SimplifiedChinese to "简体中文",
            TraditionalChinese to "繁體中文",
            "ko" to "한국어",
            "fr" to "Français",
            "de" to "Deutsch",
            "es" to "Español",
            "pt" to "Português",
            "ar" to "العربية",
            "hi" to "हिन्दी",
        )

    fun languageName(code: String): String =
        requireNotNull(Languages.firstOrNull { it.first == code }?.second) {
            "unsupported base language $code"
        }

    fun replyLanguageName(code: String): String =
        when (code) {
            LegacyChinese -> "繁體中文"
            else ->
                requireNotNull(ReplyLanguages.firstOrNull { it.first == code }?.second) {
                    "unsupported reply language $code"
                }
        }

    /** Existing Homes that stored unscripted zh preserve their current Traditional behavior. */
    fun canonicalReplyLanguage(code: String): String =
        if (code == LegacyChinese) TraditionalChinese else code.also(::replyLanguageName)

    /** The microphone hears one spoken Chinese language regardless of the requested writing script. */
    fun spokenLanguageCodeForReply(code: String): String =
        when (canonicalReplyLanguage(code)) {
            SimplifiedChinese,
            TraditionalChinese,
            -> "zh"
            else -> code
        }

    /** Temporary bridge until application-localization gets its own setting/owner. */
    fun localizationLanguageForReply(code: String): String =
        when (canonicalReplyLanguage(code)) {
            SimplifiedChinese,
            TraditionalChinese,
            -> "zh"
            else -> code
        }

    @Serializable
    data class Value(val schemaVersion: String, val replyLanguage: String) {
        init {
            require(schemaVersion == Schema) { "person settings schema is not current" }
            replyLanguageName(replyLanguage)
        }
    }

    val Default = Value(Schema, DefaultLanguage)
}

// [VXG RealForever]
