package vexlife.text

import vexlife.person.VexLifePersonSettings

/**
 * The string catalog shipped inside VexLife, read from `localization/strings/source/` on the
 * classpath, and the one selected-language rule every JVM presentation uses.
 */
object VexLifeStrings {
    const val SourceResourceRoot = "/strings/source/"

    /** Every source scope, in load order. A test keeps this equal to the files on disk. */
    val Scopes = listOf("setup", "workspace")

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

    /** `ja-JP`, `zh_Hans_CN` or `pt-BR` name the same language as `ja`, `zh` or `pt`. */
    fun normalizedLanguage(value: String): String = value.trim().lowercase().split('-', '_').first()
}

// [VXG RealForever]
