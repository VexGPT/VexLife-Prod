package vexlife.setup

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import vexlife.home.CanonicalVexLifeHomeFixture
import vexlife.model.heretic.HereticModelCatalog
import vexlife.model.heretic.HereticRuntimeRealization
import vexlife.model.heretic.runtime.FileHereticModelConfigurationStore

/**
 * The setup window's model step: offered models in the window's language, and where a choice lives.
 */
class VexLifeProdSetupModelTest {
    private val gib = 1024L * 1024L * 1024L

    private fun resolver(
        target: vexlife.platform.VexLifePlatformRealization.Target =
            HereticRuntimeRealization.MacArm64,
    ): HereticRuntimeRealization.Resolver =
        HereticRuntimeRealization.Resolver(
            HereticRuntimeRealization.TargetSource {
                target
            },
        )

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
                    runtimeRealizations = resolver(),
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

            val chosen =
                VexLifeProdSetupModel.choose(
                    productHome = absent,
                    handle = "heretic-4b",
                    language = "ja",
                    totalMemoryBytes = 16 * gib,
                    runtimeRealizations = resolver(),
                )
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
                    runtimeRealizations = resolver(),
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
    fun windowsRecommendationUsesOnlyItsExactRealizationCoverage() {
        val parent =
            Files.createTempDirectory(
                "vexlife-setup-model-windows-",
            )
                .toAbsolutePath()
        try {
            val observed =
                VexLifeProdSetupModel.observe(
                    productHome =
                        parent.resolve(
                            "new-home",
                        ),
                    explicit =
                        null,
                    language =
                        "en",
                    totalMemoryBytes =
                        32 * gib,
                    runtimeRealizations =
                        resolver(
                            HereticRuntimeRealization.WindowsX64,
                        ),
                )

            assertEquals(
                "heretic-4b",
                observed.selected,
            )
            assertEquals(
                listOf(
                    "heretic-4b",
                ),
                observed.models.map {
                    it.handle
                },
            )
        } finally {
            parent.toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun onceAHomeExistsTheChoiceIsTheHomesOwn() {
        val parent = Files.createTempDirectory("vexlife-setup-model-home-").toAbsolutePath()
        val home = parent.resolve(".vexlife")
        try {
            val fixture = CanonicalVexLifeHomeFixture.form(home)
            val chosen =
                VexLifeProdSetupModel.choose(
                    productHome = home,
                    handle = "heretic-9b",
                    language = "en",
                    totalMemoryBytes = 64 * gib,
                    runtimeRealizations = resolver(),
                )
            assertTrue(chosen.effectsPerformed)
            assertTrue(chosen.savedInHome)
            assertEquals(
                HereticModelCatalog.Models.Qwen9B,
                FileHereticModelConfigurationStore(home).selectedModel(fixture.manifest),
            )
            // Asked again with no window choice, the Home's choice wins over what fits the Mac.
            assertEquals(
                "heretic-9b",
                VexLifeProdSetupModel.observe(
                    productHome = home,
                    explicit = null,
                    language = "en",
                    totalMemoryBytes = 64 * gib,
                    runtimeRealizations = resolver(),
                ).selected,
            )
        } finally {
            parent.toFile().deleteRecursively()
        }
    }
}
