package vexlife.text

import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import vexlife.person.VexLifePersonSettings

class VexLifeStringsTest {
    private val root = Path.of(System.getProperty("user.dir"))

    @Test
    fun stringSourceIsCurrentWithItsManifest() {
        assertEquals(emptyList(), VexLifeStringsSource.check(root))
    }

    @Test
    fun everySourceFileIsAShippedScope() {
        assertEquals(VexLifeStringsSource.scopeNames(root), VexLifeStrings.Scopes.sorted())
    }

    @Test
    fun setupIsCompleteInEveryLanguageVexLifeOffers() {
        assertEquals(
            VexLifePersonSettings.Languages.map { it.first },
            VexLifeStrings.catalog.completeLanguages("setup"),
        )
    }

    @Test
    fun theSelectedLanguageIsTheMostExplicitCompleteOne() {
        assertEquals("ja", VexLifeStrings.selectLanguage("setup", "ja-JP", "fr", "de"))
        assertEquals("fr", VexLifeStrings.selectLanguage("setup", null, "fr", "de"))
        assertEquals("de", VexLifeStrings.selectLanguage("setup", "xx", null, "de_DE"))
        assertEquals("zh", VexLifeStrings.selectLanguage("setup", null, null, "zh-Hans-CN"))
        assertEquals("en", VexLifeStrings.selectLanguage("setup", null, null, null))
    }

    @Test
    fun everyKeyIsAnElementIdentityOnAtLeastOneSurface() {
        for (key in VexLifeStrings.catalog.keys) {
            assertTrue(VexLifeStrings.catalog.entry(key).surfaces.isNotEmpty(), key)
        }
    }
}

// [VXG RealForever]
