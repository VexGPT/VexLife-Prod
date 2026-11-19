package vexlife.text

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import vexlife.lens.VexLifeProdScreenLens
import vexlife.person.VexLifePersonSettings
import vexlife.web.VexLifeWorkspaceElements

class VexLifeStringsTest {
    private val root = Path.of(System.getProperty("user.dir"))

    @Test
    fun sharedModelFitWordsNameTheComputerNotOnePlatform() {
        assertEquals(
            "fits this computer",
            VexLifeStrings.text(
                "workspace.label.model-fits",
                "en",
            ),
        )
        assertEquals(
            "more than this computer has",
            VexLifeStrings.text(
                "workspace.label.model-tight",
                "en",
            ),
        )
    }

    @Test
    fun stringSourceIsCurrentWithItsManifest() {
        assertEquals(emptyList(), VexLifeStringsSource.check(root))
    }

    @Test
    fun everySourceFileIsAShippedScope() {
        assertEquals(VexLifeStringsSource.scopeNames(root), VexLifeStrings.Scopes.sorted())
    }

    @Test
    fun everyLocalizedScreenScopeIsCompleteInEveryLanguageVexLifeOffers() {
        for (scope in VexLifeStrings.ScreenLanguageScopes) {
            assertEquals(
                VexLifePersonSettings.ScreenLanguages.map { it.first },
                VexLifeStrings.catalog.completeLanguages(scope),
                "$scope is complete in every screen language VexLife offers",
            )
        }
    }

    @Test
    fun theSelectedLanguageIsTheMostExplicitCompleteOne() {
        assertEquals("ja", VexLifeStrings.selectLanguage("setup", "ja-JP", "fr", "de"))
        assertEquals("fr", VexLifeStrings.selectLanguage("setup", null, "fr", "de"))
        assertEquals("de", VexLifeStrings.selectLanguage("setup", "xx", null, "de_DE"))
        assertEquals("zh", VexLifeStrings.selectLanguage("setup", null, null, "zh-Hans-CN"))
        assertEquals("zh-Hant", VexLifeStrings.selectLanguage("setup", null, null, "zh-Hant-TW"))
        assertEquals("zh-Hant", VexLifeStrings.selectLanguage("setup", null, null, "zh_TW"))
        assertEquals("zh-Hant", VexLifeStrings.selectLanguage("setup", null, null, "zh-HK"))
        assertEquals("zh", VexLifeStrings.selectLanguage("setup", null, null, "zh-Hans-HK"))
        assertEquals("zh", VexLifeStrings.selectLanguage("setup", null, null, "zh-CN"))
        assertEquals("en", VexLifeStrings.selectLanguage("setup", null, null, null))
    }

    @Test
    fun everyWorkspaceKeyIsUsedAndEveryUsedWorkspaceKeyExists() {
        val literal = Regex("[\"'](workspace\\.(?:button|label|heading|description|status|placeholder|nav|legend)\\.[a-z0-9-]+)[\"']")
        val used =
            Files.walk(root.resolve("src")).use { stream ->
                stream
                    .filter { it.toString().endsWith(".kt") && "Test" !in it.fileName.toString() }
                    .toList()
                    .flatMap { file -> literal.findAll(Files.readString(file)).map { it.groupValues[1] }.toList() }
                    .toSet()
            }
        val defined = VexLifeStrings.catalog.keys.filter { it.startsWith("workspace.") }.toSet()
        assertEquals(emptySet(), used - defined, "workspace keys used but missing")
        assertEquals(emptySet(), defined - used, "workspace keys nothing shows")
    }

    @Test
    fun workspaceAndReleaseAlertDataKeysNameTheirOwnersRefs() {
        for (scope in listOf("workspace", "release-alerts")) {
            VexLifeStrings.catalog.keys
                .filter { it.startsWith("$scope.") }
                .mapNotNull { VexLifeStrings.catalog.dataKey(it) }
                .forEach { assertTrue(it in VexLifeWorkspaceElements.DataKeys, it) }
        }
    }

    @Test
    fun aLensNamespaceNamesARealScreenLensElement() {
        val elements = VexLifeProdScreenLens.elementRefs()
        val linked =
            VexLifeStrings.catalog.keys.mapNotNull { key ->
                VexLifeStrings.catalog.entry(key).namespaces["lens"]?.let { key to it }
            }
        assertTrue(linked.isNotEmpty(), "no key names its Screen Lens element")
        linked.forEach { (key, element) -> assertTrue(element in elements, "$key names $element") }
    }

    @Test
    fun everyKeyIsAnElementIdentityOnAtLeastOneSurface() {
        for (key in VexLifeStrings.catalog.keys) {
            assertTrue(VexLifeStrings.catalog.entry(key).surfaces.isNotEmpty(), key)
        }
    }
}

// [VXG RealForever]
