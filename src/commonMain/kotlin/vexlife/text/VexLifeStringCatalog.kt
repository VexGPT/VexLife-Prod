package vexlife.text

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import vexlife.person.VexLifePersonSettings

/**
 * The keyed string source for VexLife's human-readable text, and the one place that turns a
 * [VexLifeText] into words.
 *
 * Source files follow Vextreme's shape (`docs/architecture/06-i18n.md`): one file per scope, a
 * `_meta.scope`, and for each key an element object whose `strings` namespace holds `{ "en": {
 * "text": … }, "ja": { "text": … }, … }`. English is canonical source.
 *
 * The key is the element's identity, not only its words. Its object is extensible: each system that
 * needs to find the element attaches its own namespace and reads only that one.
 * - `surfaces` (required): the presentations that show this element (`setup.card`, `setup.window`).
 * - `dataKey`: the semantic ref the element presents, as `owner:kind:REF`
 *   (`entry:control:CONVERGE_DESIRED`), so a button's words map to the Control it carries.
 * - `testId`, `designToken`, `lens`: reserved for automation, design tokens and VexBoard Screen
 *   Lens addresses, as those systems attach.
 *
 * Integrity is checked when a source is read, at the same severity Vextreme uses:
 * - BLOCK: a key without English, a key outside its scope or the key convention, a language that is
 *   not one of VexLife's languages, or a translation whose `{placeholders}` differ from English.
 * - A translation marked `_stale` (its English changed after it was written) is never shown; the
 *   English text is shown instead until it is translated again.
 *
 * A language is *complete* for a scope only when every key in it has a current translation.
 * Choosers offer complete languages only; partial translation is not presented as finished.
 */
class VexLifeStringCatalog private constructor(private val sources: List<Source>) {
    data class Variant(val text: String, val ariaLabel: String?, val stale: Boolean)

    data class Entry(
        val key: String,
        val strings: Map<String, Variant>,
        val surfaces: List<String>,
        val dataKey: String?,
        val namespaces: Map<String, String>,
    ) {
        val english: Variant
            get() = requireNotNull(strings[SourceLanguage])
    }

    data class Source(val scope: String, val entries: Map<String, Entry>)

    private val entries: Map<String, Entry> = buildMap {
        sources.forEach { source ->
            source.entries.forEach { (key, entry) ->
                require(put(key, entry) == null) { "string key is defined twice: $key" }
            }
        }
    }

    val scopes: List<String>
        get() = sources.map { it.scope }

    val keys: Set<String>
        get() = entries.keys

    fun entry(key: String): Entry = requireNotNull(entries[key]) { "unknown string key: $key" }

    fun contains(key: String): Boolean = key in entries

    /** Every element key a presentation [surface] shows. */
    fun keysOn(surface: String): List<String> =
        entries.values.filter { surface in it.surfaces }.map { it.key }.sorted()

    /** Every element key that presents the semantic ref [dataKey]. */
    fun keysFor(dataKey: String): List<String> =
        entries.values.filter { it.dataKey == dataKey }.map { it.key }.sorted()

    fun dataKey(key: String): String? = entry(key).dataKey

    /** The words for [key] in [language]: its current translation, otherwise English. */
    fun text(key: String, language: String): String = variant(key, language).text

    fun ariaLabel(key: String, language: String): String? = variant(key, language).ariaLabel

    private fun variant(key: String, language: String): Variant {
        val entry = entry(key)
        val translated = entry.strings[language]
        return if (translated != null && !translated.stale) translated else entry.english
    }

    fun resolve(text: VexLifeText, language: String): String {
        val template = text(text.key, language)
        val expected = VexLifeText.placeholdersIn(entry(text.key).english.text)
        require(text.filledNames == expected) {
            "text ${text.key} fills ${text.filledNames.sorted()} but its English names ${expected.sorted()}"
        }
        return VexLifeText.Placeholder.replace(template) { match ->
            val name = match.groupValues[1]
            text.args[name]
                ?: text.gib[name]?.let { gibText(it, language) }
                ?: text.parts[name]?.let { resolve(it, language) }
                ?: error("text ${text.key} has no value for {$name}")
        }
    }

    /** English plus every language with a current translation of every key in [scope]. */
    fun completeLanguages(scope: String): List<String> {
        val source =
            requireNotNull(sources.firstOrNull { it.scope == scope }) {
                "unknown string scope: $scope"
            }
        return VexLifePersonSettings.Languages.map { it.first }
            .filter { language ->
                language == SourceLanguage ||
                    source.entries.values.all { entry ->
                        entry.strings[language]?.let { !it.stale } == true
                    }
            }
    }

    fun withSource(json: String): VexLifeStringCatalog = VexLifeStringCatalog(sources + parse(json))

    companion object {
        const val SourceLanguage = "en"

        private val Reader = Json { isLenient = false }

        val Empty = VexLifeStringCatalog(emptyList())

        fun of(vararg json: String): VexLifeStringCatalog = VexLifeStringCatalog(json.map(::parse))

        /** Reads one source file, failing closed on any BLOCK-level problem. */
        fun parse(json: String): Source {
            val root = Reader.parseToJsonElement(json).jsonObject
            val meta = requireNotNull(root["_meta"]) { "string source has no _meta" }.jsonObject
            val scope =
                requireNotNull(meta["scope"]?.jsonPrimitive?.content) {
                    "string source has no _meta.scope"
                }
            val languages = VexLifePersonSettings.Languages.map { it.first }.toSet()
            val entries =
                root.entries
                    .filter { it.key != "_meta" }
                    .associate { (key, value) ->
                        require(VexLifeText.KeyPattern.matches(key)) {
                            "BLOCK: key does not follow {scope}.{element-type}.{semantic-name}: $key"
                        }
                        require(key.startsWith("$scope.")) {
                            "BLOCK: key $key is outside scope $scope"
                        }
                        val element = value.jsonObject
                        element.keys.forEach { namespace ->
                            require(namespace in Namespaces) {
                                "BLOCK: $key carries unknown namespace $namespace (known: $Namespaces)"
                            }
                        }
                        val strings =
                            requireNotNull(element["strings"]) {
                                    "BLOCK: $key has no strings"
                                }
                                .jsonObject
                        val surfaces =
                            (element["surfaces"] as? JsonArray)
                                ?.map { (it as? JsonPrimitive)?.content.orEmpty() }
                                .orEmpty()
                        require(
                            surfaces.isNotEmpty() &&
                                surfaces.all {
                                    SurfacePattern.matches(it) && it.startsWith("$scope.")
                                }
                        ) {
                            "BLOCK: $key must name the $scope.* surfaces that show it"
                        }
                        val dataKey = (element["dataKey"] as? JsonPrimitive)?.content
                        require(dataKey == null || DataKeyPattern.matches(dataKey)) {
                            "BLOCK: $key dataKey must be owner:kind:REF"
                        }
                        val namespaces =
                            element
                                .filterKeys { it !in setOf("strings", "surfaces", "dataKey") }
                                .mapValues { (namespace, raw) ->
                                    requireNotNull((raw as? JsonPrimitive)?.content) {
                                        "BLOCK: $key $namespace must be a string"
                                    }
                                }
                        val variants =
                            strings.entries.associate { (language, variant) ->
                                require(language in languages) {
                                    "BLOCK: $key has a translation in unsupported language $language"
                                }
                                language to variant(key, language, variant.jsonObject)
                            }
                        val english =
                            requireNotNull(variants[SourceLanguage]) {
                                "BLOCK: $key has no English text"
                            }
                        require(english.text.isNotBlank()) { "BLOCK: $key has blank English text" }
                        val names = VexLifeText.placeholdersIn(english.text)
                        variants.forEach { (language, variant) ->
                            require(VexLifeText.placeholdersIn(variant.text) == names) {
                                "BLOCK: $key in $language names different placeholders than English"
                            }
                            require(variant.text.isNotBlank()) {
                                "BLOCK: $key in $language is blank"
                            }
                        }
                        key to Entry(key, variants, surfaces, dataKey, namespaces)
                    }
            return Source(scope, entries)
        }

        private fun variant(key: String, language: String, value: JsonObject): Variant {
            val allowed = setOf("text", "aria-label", "_stale")
            require(value.keys.all { it in allowed }) {
                "BLOCK: $key in $language carries fields other than ${allowed.sorted()}"
            }
            val text =
                requireNotNull((value["text"] as? JsonPrimitive)?.content) {
                    "BLOCK: $key in $language has no text"
                }
            return Variant(
                text = text,
                ariaLabel = (value["aria-label"] as? JsonPrimitive)?.content,
                stale = (value["_stale"] as? JsonPrimitive)?.booleanOrNull == true,
            )
        }

        /** The namespaces an element object may carry; adding one is a deliberate change here. */
        val Namespaces = setOf("strings", "surfaces", "dataKey", "testId", "designToken", "lens")

        val SurfacePattern = Regex("[a-z0-9]+(?:-[a-z0-9]+)*(?:\\.[a-z0-9]+(?:-[a-z0-9]+)*)+")

        val DataKeyPattern =
            Regex("[a-z0-9]+(?:-[a-z0-9]+)*:[a-z0-9]+(?:-[a-z0-9]+)*:[A-Za-z0-9_]+")

        /** Binary GiB with one decimal, in the language's decimal style. */
        fun gibText(bytes: Long, language: String): String {
            val gibibyte = 1024L * 1024L * 1024L
            val tenths = (bytes.coerceAtLeast(0L) * 10L + gibibyte / 2L) / gibibyte
            val separator = if (language in DecimalCommaLanguages) "," else "."
            return "${tenths / 10L}$separator${tenths % 10L}"
        }

        private val DecimalCommaLanguages = setOf("fr", "de", "es", "pt")
    }
}

// [VXG RealForever]
