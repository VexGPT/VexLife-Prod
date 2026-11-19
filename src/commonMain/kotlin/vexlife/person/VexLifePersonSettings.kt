package vexlife.person

import kotlinx.serialization.Serializable

/**
 * Settings the person chooses, kept in their Vex Home. Two language choices live here, each with
 * one responsibility, and neither moves the other:
 *
 * ~~~text
 * replyLanguage    what Vex replies in; Vex is told it on every message, Speech reads in it, and
 *                  dictation hears its spoken language            (ReplyLanguages)
 * screenLanguage   what VexLife's own screens are written in: setup, workspace, release alerts
 *                  (ScreenLanguages, complete ones only); absent = the same as the reply language
 * ~~~
 *
 * [VexLifeLanguageFamily] owns how the two choices move; [screenLanguageFor] is the one place that
 * resolves which language the screens use. Chinese reply
 * script is explicit; both reply variants map back to one spoken Chinese coordinate only at the
 * speech-recognition adapter.
 */
object VexLifePersonSettings {
    const val Schema = "vexlife-prod.person-settings/v2"
    const val SchemaV1 = "vexlife-prod.person-settings/v1"

    /** The screen-language choice that means "the same as Vex's replies" (stored as absent). */
    const val FollowReplyLanguage = VexLifeLanguageFamily.ControlFollowReplyLanguage
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

    /**
     * What VexLife's own screens can be written in. Reading is script-distinct, so Chinese is two
     * screen languages here (catalog `zh` holds Simplified, `zh-Hant` Traditional) even though the
     * microphone hears one spoken Chinese ([Languages]).
     */
    val ScreenLanguages: List<Pair<String, String>> =
        Languages.flatMap {
            if (it.first == LegacyChinese) {
                listOf(LegacyChinese to "简体中文", TraditionalChinese to "繁體中文")
            } else {
                listOf(it)
            }
        }

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

    /**
     * The screen language a reply language implies when the person has not chosen one. The script
     * the person reads in survives: a Traditional Chinese reader (including a legacy `zh` Home, which
     * [canonicalReplyLanguage] keeps Traditional) is shown Traditional screens, never Simplified.
     */
    fun localizationLanguageForReply(code: String): String =
        when (canonicalReplyLanguage(code)) {
            SimplifiedChinese -> LegacyChinese
            TraditionalChinese -> TraditionalChinese
            else -> code
        }

    /**
     * The reply language a screen language suggests, used only to seed a Home that has no reply
     * language yet (the first language a person picks in setup). Simplified screens suggest
     * Simplified replies; the catalog's unscripted `zh` is never stored as a reply language,
     * because a stored `zh` reply means Traditional ([canonicalReplyLanguage]).
     */
    fun replyLanguageForScreen(code: String): String =
        when (code) {
            LegacyChinese -> SimplifiedChinese
            else -> code.also(::replyLanguageName)
        }

    fun screenLanguageName(code: String): String =
        requireNotNull(ScreenLanguages.firstOrNull { it.first == code }?.second) {
            "unsupported screen language $code"
        }

    /** The language VexLife's screens use for [value]: the chosen one, or the reply language's. */
    fun screenLanguageFor(value: Value): String =
        value.screenLanguage ?: localizationLanguageForReply(value.replyLanguage)

    @Serializable
    data class Value(
        val schemaVersion: String,
        val replyLanguage: String,
        val screenLanguage: String? = null,
    ) {
        init {
            require(schemaVersion == Schema) { "person settings schema is not current" }
            replyLanguageName(replyLanguage)
            screenLanguage?.let(::screenLanguageName)
        }
    }

    /** Homes saved before the screen language had its own choice; they follow the reply language. */
    @Serializable
    data class ValueV1(val schemaVersion: String, val replyLanguage: String) {
        init {
            require(schemaVersion == SchemaV1) { "person settings v1 schema expected" }
        }

        fun current(): Value = Value(Schema, replyLanguage, screenLanguage = null)
    }

    val Default = Value(Schema, DefaultLanguage)
}

// [VXG RealForever]
