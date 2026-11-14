package vexlife.text

import kotlinx.serialization.Serializable

/**
 * A piece of human-readable text named by its string key, not by its words.
 *
 * VexArchitecture's localization law: no display string is hardcoded. A Family's DisplayState
 * carries [VexLifeText]; the concrete presentation resolves it in the person's selected language
 * through [VexLifeStringCatalog]. The key is `{scope}.{element-type}.{semantic-name}`.
 *
 * Placeholders in the text (`{name}`) are filled from exactly one of:
 * - [args]: literal values that are not translated (a folder path, a chip name, a count);
 * - [gib]: byte counts, shown as GiB with one decimal in the language's number style;
 * - [parts]: nested keyed text, resolved in the same language. A composite sentence is a
 *   translatable template of parts, so each language decides its own order and spacing.
 */
@Serializable
data class VexLifeText(
    val key: String,
    val args: Map<String, String> = emptyMap(),
    val gib: Map<String, Long> = emptyMap(),
    val parts: Map<String, VexLifeText> = emptyMap(),
) {
    init {
        require(KeyPattern.matches(key)) {
            "text key must be {scope}.{element-type}.{semantic-name}: $key"
        }
        val names = args.keys + gib.keys + parts.keys
        require(names.size == args.size + gib.size + parts.size) {
            "a placeholder is filled twice in $key"
        }
        require(names.all { PlaceholderName.matches(it) }) {
            "placeholder names must be lowerCamelCase in $key"
        }
    }

    /** Every placeholder name this text fills. */
    val filledNames: Set<String>
        get() = args.keys + gib.keys + parts.keys

    companion object {
        val ElementTypes =
            setOf(
                "button",
                "label",
                "heading",
                "description",
                "nav",
                "legend",
                "status",
                "placeholder",
            )

        val KeyPattern =
            Regex(
                "[a-z0-9]+(?:-[a-z0-9]+)*(?:\\.[a-z0-9]+(?:-[a-z0-9]+)*)*\\.(?:" +
                    ElementTypes.joinToString("|") +
                    ")\\.[a-z0-9]+(?:-[a-z0-9]+)*"
            )

        val PlaceholderName = Regex("[a-z][a-zA-Z0-9]*")

        val Placeholder = Regex("\\{([a-z][a-zA-Z0-9]*)\\}")

        fun placeholdersIn(text: String): Set<String> =
            Placeholder.findAll(text).map { it.groupValues[1] }.toSet()

        fun text(key: String, vararg args: Pair<String, String>): VexLifeText =
            VexLifeText(key = key, args = args.toMap())
    }
}

// [VXG RealForever]
