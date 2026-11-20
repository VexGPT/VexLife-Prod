package vexlife.text

import vexlife.person.VexLifePersonSettings

/**
 * The string catalog shipped inside VexLife, read from `localization/strings/source/` on the
 * classpath, and the one selected-language rule every JVM presentation uses.
 */
object VexLifeStrings {
    const val SourceResourceRoot = "/strings/source/"

    /** Every shipped source scope, in load order. A test keeps this equal to the files on disk. */
    val Scopes = listOf("release-alerts", "setup", "windows-setup", "workspace")

    /**
     * Human-selectable screen-language scopes. Windows setup is intentionally excluded while its
     * source remains English-only; its own source metadata holds full localization as a later
     * release gate.
     */
    val ScreenLanguageScopes = listOf("release-alerts", "setup", "workspace")

    val catalog: VexLifeStringCatalog by lazy {
        Scopes.fold(VexLifeStringCatalog.Empty) { catalog, scope ->
            catalog.withSource(sourceText(scope))
        }
    }

    fun sourceText(scope: String): String {
        val resource = SourceResourceRoot + scope + ".json"
        return requireNotNull(VexLifeStrings::class.java.getResourceAsStream(resource)) {
                "string source $resource is missing from this build"
            }
            .use { it.readBytes().toString(Charsets.UTF_8) }
    }

    fun resolve(text: VexLifeText, language: String): String = catalog.resolve(text, language)

    fun text(key: String, language: String): String = catalog.text(key, language)

    /**
     * The language to show [scope] in: the first of [candidates] that VexLife supports and that is
     * complete for [scope]; otherwise English. Candidates are given most-explicit first (the
     * person's choice now, then their saved choice, then the system's preferred language).
     */
    fun selectLanguage(scope: String, vararg candidates: String?): String {
        val complete = catalog.completeLanguages(scope)
        return candidates.asSequence().filterNotNull().map(::normalizedLanguage).firstOrNull {
            it in complete
        } ?: VexLifePersonSettings.DefaultLanguage
    }

    /**
     * `ja-JP`, `zh_Hans_CN` or `pt-BR` name the same screen language as `ja`, `zh` or `pt`. Chinese
     * keeps its script: `zh-Hant`, `zh_TW`, `zh-HK` and `zh-MO` (without an explicit `Hans`) read
     * Traditional, so they name `zh-Hant`; every other Chinese tag names Simplified `zh`.
     */
    fun normalizedLanguage(value: String): String {
        val parts = value.trim().lowercase().split('-', '_').filter { it.isNotEmpty() }
        val base = parts.firstOrNull() ?: return ""
        if (base != VexLifePersonSettings.LegacyChinese) return base
        val traditional =
            "hant" in parts || ("hans" !in parts && parts.any { it in TraditionalRegions })
        return if (traditional) VexLifePersonSettings.TraditionalChinese else base
    }

    private val TraditionalRegions = setOf("tw", "hk", "mo")
}

// [VXG RealForever]
