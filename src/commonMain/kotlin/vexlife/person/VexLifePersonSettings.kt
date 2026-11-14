package vexlife.person

import kotlinx.serialization.Serializable

/**
 * Settings the person chooses that Vex is told about, kept in their Vex Home.
 *
 * Today that is the language Vex replies in (and Speech reads in). It is a Home setting rather than
 * a browser preference because it is part of what Vex is given on every turn.
 */
object VexLifePersonSettings {
    const val Schema = "vexlife-prod.person-settings/v1"
    const val DefaultLanguage = "en"

    /** Language code and its own name, as the person reads it. Vextreme's ten languages. */
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

    fun languageName(code: String): String =
        requireNotNull(Languages.firstOrNull { it.first == code }?.second) {
            "unsupported language $code"
        }

    @Serializable
    data class Value(val schemaVersion: String, val replyLanguage: String) {
        init {
            require(schemaVersion == Schema) { "person settings schema is not current" }
            languageName(replyLanguage)
        }
    }

    val Default = Value(Schema, DefaultLanguage)
}

// [VXG RealForever]
