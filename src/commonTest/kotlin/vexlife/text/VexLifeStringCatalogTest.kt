package vexlife.text

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class VexLifeStringCatalogTest {
    private val source =
        """
        {
          "_meta": { "scope": "demo" },
          "demo.label.free": {
            "strings": {
              "en": { "text": "{amount} GiB free on {drive}" },
              "fr": { "text": "{amount} Gio libres sur {drive}" },
              "ja": { "text": "{drive} の空き {amount} GiB", "_stale": true }
            },
            "surfaces": ["demo.card"],
            "dataKey": "demo:state:FREE"
          },
          "demo.label.drive": {
            "strings": { "en": { "text": "the main drive" }, "fr": { "text": "le disque principal" } },
            "surfaces": ["demo.card", "demo.window"]
          }
        }
        """

    private val catalog = VexLifeStringCatalog.of(source)

    private val free =
        VexLifeText(
            key = "demo.label.free",
            gib = mapOf("amount" to (15L shl 29)),
            parts = mapOf("drive" to VexLifeText("demo.label.drive")),
        )

    @Test
    fun resolvesNestedPartsAndNumbersInTheSelectedLanguage() {
        assertEquals("7.5 GiB free on the main drive", catalog.resolve(free, "en"))
        assertEquals("7,5 Gio libres sur le disque principal", catalog.resolve(free, "fr"))
    }

    @Test
    fun staleOrMissingTranslationsShowEnglishAndAreNotComplete() {
        assertEquals("7.5 GiB free on the main drive", catalog.resolve(free, "ja"))
        assertEquals(listOf("en", "fr"), catalog.completeLanguages("demo"))
    }

    @Test
    fun keyIsTheElementIdentityAndMapsToItsSurfacesAndSemanticRef() {
        assertEquals(listOf("demo.label.drive"), catalog.keysOn("demo.window"))
        assertEquals(listOf("demo.label.free"), catalog.keysFor("demo:state:FREE"))
        assertEquals(null, catalog.dataKey("demo.label.drive"))
    }

    @Test
    fun aTextMustFillExactlyThePlaceholdersItsEnglishNames() {
        assertFailsWith<IllegalArgumentException> {
            catalog.resolve(VexLifeText("demo.label.free", args = mapOf("amount" to "1")), "en")
        }
    }

    @Test
    fun blockLevelProblemsRefuseTheSource() {
        fun entry(body: String) = """{ "_meta": { "scope": "demo" }, $body }"""
        val noEnglish =
            entry(
                """"demo.label.x": { "strings": { "fr": { "text": "x" } }, "surfaces": ["demo.card"] }"""
            )
        val wrongPlaceholder =
            entry(
                """"demo.label.x": { "strings": { "en": { "text": "{a}" }, "fr": { "text": "{b}" } }, "surfaces": ["demo.card"] }"""
            )
        val outsideScope =
            entry(
                """"other.label.x": { "strings": { "en": { "text": "x" } }, "surfaces": ["demo.card"] }"""
            )
        val noSurface = entry(""""demo.label.x": { "strings": { "en": { "text": "x" } } }""")
        val unknownNamespace =
            entry(
                """"demo.label.x": { "strings": { "en": { "text": "x" } }, "surfaces": ["demo.card"], "colour": "red" }"""
            )
        for (bad in
            listOf(noEnglish, wrongPlaceholder, outsideScope, noSurface, unknownNamespace)) {
            assertFailsWith<IllegalArgumentException> { VexLifeStringCatalog.parse(bad) }
        }
    }

    @Test
    fun gibUsesOneDecimalInTheLanguagesNumberStyle() {
        assertEquals("0.0", VexLifeStringCatalog.gibText(0, "en"))
        assertEquals("14.1", VexLifeStringCatalog.gibText(15_153_242_506L, "ja"))
        assertEquals("14,1", VexLifeStringCatalog.gibText(15_153_242_506L, "de"))
        assertEquals("0.5", VexLifeStringCatalog.gibText(1L shl 29, "en"))
    }
}

// [VXG RealForever]
