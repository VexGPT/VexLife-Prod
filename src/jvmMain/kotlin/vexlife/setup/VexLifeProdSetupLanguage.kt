package vexlife.setup

import java.nio.file.Path
import kotlinx.serialization.Serializable
import vexlife.home.VexLifeProdHomeFiles
import vexlife.person.VexLifePersonSettings
import vexlife.person.VexLifePersonSettingsStore
import vexlife.text.VexLifeStrings

/**
 * The person's language during setup: the first step of the Setup Journey.
 *
 * There is one selected-language authority, the person's settings in their Vex Home (the language
 * Vex replies in and Speech reads in). Before a Home exists, the setup window holds the choice and
 * passes it to every inspection and activation; once a canonical Home exists the choice is saved
 * into it, so setup, the workspace and Vex all agree from then on.
 *
 * Resolution, most explicit first: the language chosen in this window, the language saved in the
 * selected Home, the Mac's preferred language, English. Only languages whose setup text is
 * completely translated are offered or selected.
 */
object VexLifeProdSetupLanguage {
    const val Schema = "vexlife-prod.setup-language/v1"
    const val Scope = "setup"

    /** The native window's own words: every element on the `setup.window` surface. */
    const val WindowSurface = "setup.window"

    @Serializable data class Language(val code: String, val name: String)

    @Serializable
    data class Projection(
        val schemaVersion: String,
        val language: String,
        val languageChosen: Boolean,
        val savedInHome: Boolean,
        val languages: List<Language>,
        val window: Map<String, String>,
        val effectsPerformed: Boolean,
    ) {
        init {
            require(
                schemaVersion == Schema &&
                    languages.any { it.code == language } &&
                    window.isNotEmpty() &&
                    (!savedInHome || languageChosen)
            ) {
                "setup language projection is invalid"
            }
        }
    }

    fun offered(): List<Language> {
        val complete = VexLifeStrings.catalog.completeLanguages(Scope)
        return VexLifePersonSettings.Languages.filter { it.first in complete }
            .map { Language(it.first, it.second) }
    }

    /** The language saved in [productHome], when it is a canonical Vex Home with one saved. */
    fun saved(productHome: Path): String? = runCatching {
        if (
            VexLifeProdHomeFiles.observeLocation(productHome.normalize())
                is VexLifeProdHomeFiles.Location.Existing
        ) {
            VexLifePersonSettingsStore(productHome.normalize()).readOrNull()?.replyLanguage
        } else {
            null
        }
    }
        .getOrNull()

    fun resolve(productHome: Path, explicit: String?, system: String?): String =
        VexLifeStrings.selectLanguage(Scope, explicit, saved(productHome), system)

    /** No-effect: what setup will show in, and the window's words in that language. */
    fun observe(productHome: Path, explicit: String?, system: String?): Projection {
        val saved = saved(productHome)
        val language = VexLifeStrings.selectLanguage(Scope, explicit, saved, system)
        return Projection(
            schemaVersion = Schema,
            language = language,
            languageChosen = explicit != null || saved != null,
            savedInHome = saved == language,
            languages = offered(),
            window = window(language),
            effectsPerformed = false,
        )
    }

    fun window(language: String): Map<String, String> =
        VexLifeStrings.catalog.keysOn(WindowSurface).associateWith {
            VexLifeStrings.text(it, language)
        }

    /**
     * Saves [language] as the person's language when [productHome] is a canonical Vex Home. Returns
     * whether it is now saved there; before a Home exists nothing is written.
     */
    fun persist(productHome: Path, language: String): Boolean {
        require(offered().any { it.code == language }) { "language $language is not offered" }
        val home = productHome.normalize()
        if (VexLifeProdHomeFiles.observeLocation(home) !is VexLifeProdHomeFiles.Location.Existing) {
            return false
        }
        val store = VexLifePersonSettingsStore(home)
        if (store.readOrNull()?.replyLanguage != language) {
            store.write(VexLifePersonSettings.Value(VexLifePersonSettings.Schema, language))
        }
        return true
    }

    /**
     * The person chose [language] in the setup window: save it if the Home exists, then observe.
     */
    fun choose(productHome: Path, language: String): Projection {
        val code = VexLifeStrings.normalizedLanguage(language)
        val persisted = persist(productHome, code)
        return observe(productHome, explicit = code, system = null)
            .copy(effectsPerformed = persisted)
    }
}

// [VXG RealForever]
