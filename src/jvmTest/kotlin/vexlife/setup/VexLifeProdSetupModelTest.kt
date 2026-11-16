package vexlife.setup

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import vexlife.home.CanonicalVexLifeHomeFixture
import vexlife.model.heretic.HereticModelCatalog
import vexlife.model.heretic.runtime.FileHereticModelConfigurationStore

/**
 * The setup window's model step: offered models in the window's language, and where a choice lives.
 */
class VexLifeProdSetupModelTest {
    private val gib = 1024L * 1024L * 1024L

    @Test
    fun beforeAHomeExistsTheChoiceIsCarriedAndTheMacsFitIsShown() {
        val parent = Files.createTempDirectory("vexlife-setup-model-").toAbsolutePath()
        try {
            val absent = parent.resolve("new-home")
            val first =
                VexLifeProdSetupModel.observe(
                    absent,
                    explicit = null,
                    language = "en",
                    totalMemoryBytes = 16 * gib,
                )
            assertEquals("heretic-9b", first.selected)
            assertFalse(first.chosen)
            assertEquals(
                HereticModelCatalog.Entries.map { it.handle },
                first.models.map { it.handle },
            )
            assertTrue(
                first.models
                    .first { it.handle == "heretic-9b" }
                    .line
                    .contains("recommended for this Mac")
            )
            assertTrue(
                first.models
                    .first { it.handle == "heretic-27b" }
                    .line
                    .contains("wants 32.0 GiB of memory")
            )
            assertTrue(
                first.models.first { it.handle == "heretic-4b" }.line.contains("fits this Mac")
            )

            val chosen = VexLifeProdSetupModel.choose(absent, "heretic-4b", "ja", 16 * gib)
            assertEquals("heretic-4b", chosen.selected)
            assertTrue(chosen.chosen)
            assertFalse(chosen.effectsPerformed, "nothing is written before a Home exists")
            assertTrue(
                chosen.models.first { it.handle == "heretic-4b" }.line.contains("この Mac で動作")
            )
        } finally {
            parent.toFile().deleteRecursively()
        }
    }

    @Test
    fun aMacBelowEveryModelIsNeverToldAModelIsRecommendedForIt() {
        val parent = Files.createTempDirectory("vexlife-setup-model-small-").toAbsolutePath()
        try {
            // A 7 GiB Mac (the hosted macOS runner) is below the smallest model's 8 GiB.
            val observed =
                VexLifeProdSetupModel.observe(
                    parent.resolve("new-home"),
                    explicit = null,
                    language = "en",
                    totalMemoryBytes = 7 * gib,
                )
            assertEquals("heretic-4b", observed.selected, "the smallest is still preselected")
            assertTrue(observed.models.none { it.recommended || it.fits })
            assertTrue(
                observed.models
                    .first { it.handle == "heretic-4b" }
                    .line
                    .contains("wants 8.0 GiB of memory")
            )
        } finally {
            parent.toFile().deleteRecursively()
        }
    }

    @Test
    fun onceAHomeExistsTheChoiceIsTheHomesOwn() {
        val parent = Files.createTempDirectory("vexlife-setup-model-home-").toAbsolutePath()
        val home = parent.resolve(".vexlife")
        try {
            val fixture = CanonicalVexLifeHomeFixture.form(home)
            val chosen = VexLifeProdSetupModel.choose(home, "heretic-9b", "en", 64 * gib)
            assertTrue(chosen.effectsPerformed)
            assertTrue(chosen.savedInHome)
            assertEquals(
                HereticModelCatalog.Models.Qwen9B,
                FileHereticModelConfigurationStore(home).selectedModel(fixture.manifest),
            )
            // Asked again with no window choice, the Home's choice wins over what fits the Mac.
            assertEquals(
                "heretic-9b",
                VexLifeProdSetupModel.observe(home, null, "en", 64 * gib).selected,
            )
        } finally {
            parent.toFile().deleteRecursively()
        }
    }
}
