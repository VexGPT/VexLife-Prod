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

    @Test
    fun beforeAHomeExistsTheChoiceIsCarriedAndTargetFitIsShown() {
        val parent = Files.createTempDirectory("vexlife-setup-model-").toAbsolutePath()
        try {
            val absent = parent.resolve("new-home")
            val first =
                VexLifeProdSetupModel.observe(
                    absent,
                    explicit = null,
                    language = "en",
                    target = HereticRuntimeRealization.MacArm64,
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
                    .contains("recommended for this device")
            )
            assertTrue(
                first.models
                    .first { it.handle == "heretic-27b" }
                    .line
                    .contains("wants 32.0 GiB of memory")
            )
            assertTrue(
                first.models.first { it.handle == "heretic-4b" }.line.contains("fits this device")
            )

            val chosen =
                VexLifeProdSetupModel.choose(
                    productHome = absent,
                    handle = "heretic-4b",
                    language = "ja",
                    target = HereticRuntimeRealization.MacArm64,
                    totalMemoryBytes = 16 * gib,
                )
            assertEquals("heretic-4b", chosen.selected)
            assertTrue(chosen.chosen)
            assertFalse(chosen.effectsPerformed, "nothing is written before a Home exists")
            assertTrue(
                chosen.models.first { it.handle == "heretic-4b" }.line.contains("このデバイスで動作")
            )
        } finally {
            parent.toFile().deleteRecursively()
        }
    }

    @Test
    fun aHostBelowEveryModelIsNeverToldAModelIsRecommendedForIt() {
        val parent = Files.createTempDirectory("vexlife-setup-model-small-").toAbsolutePath()
        try {
            // A 7 GiB Mac (the hosted macOS runner) is below the smallest model's 8 GiB.
            val observed =
                VexLifeProdSetupModel.observe(
                    parent.resolve("new-home"),
                    explicit = null,
                    language = "en",
                    target = HereticRuntimeRealization.MacArm64,
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
    fun WindowsRecommendationUsesOnlyModelsWithWindowsRealizations() {
        val parent =
            Files.createTempDirectory(
                "vexlife-setup-model-windows-target-",
            )
                .toAbsolutePath()
        try {
            val observed =
                VexLifeProdSetupModel.observe(
                    productHome =
                        parent.resolve(
                            "new-home",
                        ),
                    explicit = null,
                    language = "en",
                    target = HereticRuntimeRealization.WindowsX64,
                    totalMemoryBytes = 64 * gib,
                )

            assertEquals(
                HereticRuntimeRealization.WindowsX64.targetRef,
                observed.targetRef,
            )
            assertEquals(
                "heretic-4b",
                observed.selected,
            )
            assertTrue(
                observed.models
                    .first {
                        it.handle == "heretic-4b"
                    }
                    .realizableOnTarget,
            )
            assertFalse(
                observed.models
                    .first {
                        it.handle == "heretic-9b"
                    }
                    .realizableOnTarget,
            )
            assertFalse(
                observed.models
                    .first {
                        it.handle == "heretic-27b"
                    }
                    .realizableOnTarget,
            )
            assertTrue(
                observed.models
                    .first {
                        it.handle == "heretic-9b"
                    }
                    .line
                    .contains(
                        "not supported on this device",
                    ),
            )

            val rejected =
                runCatching {
                    VexLifeProdSetupModel.choose(
                        productHome =
                            parent.resolve(
                                "new-home",
                            ),
                        handle = "heretic-9b",
                        language = "en",
                        target = HereticRuntimeRealization.WindowsX64,
                        totalMemoryBytes = 64 * gib,
                    )
                }
            assertTrue(
                rejected.isFailure,
            )
        } finally {
            parent
                .toFile()
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
                    target = HereticRuntimeRealization.MacArm64,
                    totalMemoryBytes = 64 * gib,
                )
            assertTrue(chosen.effectsPerformed)
            assertTrue(chosen.savedInHome)
            assertEquals(
                HereticModelCatalog.Candidate9B,
                FileHereticModelConfigurationStore(home).selectedModel(fixture.manifest),
            )
            // Asked again with no window choice, the Home's choice wins over what fits the Mac.
            assertEquals(
                "heretic-9b",
                VexLifeProdSetupModel
                    .observe(
                        productHome = home,
                        explicit = null,
                        language = "en",
                        target = HereticRuntimeRealization.MacArm64,
                        totalMemoryBytes = 64 * gib,
                    )
                    .selected,
            )
        } finally {
            parent.toFile().deleteRecursively()
        }
    }
}
