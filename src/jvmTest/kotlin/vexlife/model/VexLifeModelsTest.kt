package vexlife.model

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vexlife.home.CanonicalVexLifeHomeFixture
import vexlife.model.heretic.HereticModelCatalog
import vexlife.model.heretic.runtime.FileHereticModelConfigurationStore
import vexlife.model.heretic.runtime.HereticArtifactMaterialization

/**
 * Settings → Models against a real Home: choices persist, lawfulness is re-resolved, files are
 * exact.
 */
class VexLifeModelsTest {
    private val gib = 1024L * 1024L * 1024L

    private fun withHome(block: (Path, CanonicalVexLifeHomeFixture.Formed) -> Unit) {
        val parent = Files.createTempDirectory("vexlife-models-settings-").toAbsolutePath()
        val home = parent.resolve(".vexlife")
        try {
            val fixture = CanonicalVexLifeHomeFixture.form(home)
            FileHereticModelConfigurationStore(home).selectHeretic(fixture.manifest)
            block(home, fixture)
        } finally {
            parent.toFile().deleteRecursively()
        }
    }

    private fun place(home: Path, entry: HereticModelCatalog.Entry, filename: String) {
        val folder = home.resolve(entry.modelRootRelativePath).resolve(entry.providerRevision)
        Files.createDirectories(folder)
        val member = entry.members.first { it.filename == filename }
        Files.write(folder.resolve(filename), ByteArray(member.expectedBytes.toInt()))
    }

    private fun models(
        home: Path,
        serving: String? = HereticModelCatalog.Certified27B.modelRef,
        failDownload: Boolean = true,
    ) =
        VexLifeModels(
            productHome = home,
            servingModelRef = { serving },
            totalMemoryBytes = { 16 * gib },
            acquirer = { _ ->
                HereticArtifactMaterialization.Acquirer { _, _ ->
                    if (failDownload) error("network unavailable in test")
                }
            },
        )

    @Test
    fun everyOfferedModelIsListedWithWhatTheHomeHasOfIt() = withHome { home, _ ->
        models(home).use { owner ->
            val projection = owner.projection()
            assertEquals(
                HereticModelCatalog.Entries.map { it.handle },
                projection.models.map { it.handle },
            )
            val certified = projection.models.first { it.handle == "heretic-27b" }
            assertTrue(certified.chosen && certified.serving)
            assertFalse(certified.fitsThisMac)
            assertTrue(projection.models.first { it.handle == "heretic-9b" }.fitsThisMac)
            assertEquals(
                listOf(VexLifeModelsFamily.ControlDownload),
                projection.models
                    .first { it.handle == "heretic-4b" }
                    .display
                    .controls
                    .map { it.controlRef },
            )
            assertFalse(projection.switchPending)
        }
    }

    @Test
    fun usingADownloadedModelSavesTheChoiceForTheNextStart() = withHome { home, fixture ->
        val nine = HereticModelCatalog.Candidate9B
        // Every member present by size: the model counts as downloaded.
        nine.members
            .filter { it.expectedBytes < 50_000_000 }
            .forEach { place(home, nine, it.filename) }
        val folder = home.resolve(nine.modelRootRelativePath).resolve(nine.providerRevision)
        nine.members
            .filter { it.expectedBytes >= 50_000_000 }
            .forEach { member ->
                java.io.RandomAccessFile(folder.resolve(member.filename).toFile(), "rw").use {
                    it.setLength(member.expectedBytes)
                }
            }
        models(home).use { owner ->
            val after = owner.control(VexLifeModelsFamily.ControlUse, "heretic-9b")
            assertEquals("CHOSEN", after.lastOutcome?.state)
            assertTrue(after.switchPending)
            assertEquals(
                nine,
                FileHereticModelConfigurationStore(home).selectedModel(fixture.manifest),
            )
            // The 27B the runtime still serves cannot be removed; the chosen 9B cannot either.
            assertFalse(
                VexLifeModelsFamily.ControlRemove in
                    after.models
                        .first { it.handle == "heretic-27b" }
                        .display
                        .controls
                        .map { it.controlRef }
            )
            val refused = owner.control(VexLifeModelsFamily.ControlRemove, "heretic-9b")
            assertEquals("NOT_LAWFUL_NOW", refused.lastOutcome?.state)
            assertTrue(Files.exists(folder.resolve("config.json")))
        }
    }

    @Test
    fun removingAModelFreesItsFilesAndKeepsItOffered() = withHome { home, _ ->
        val four = HereticModelCatalog.Candidate4B
        place(home, four, "config.json")
        models(home).use { owner ->
            val after = owner.control(VexLifeModelsFamily.ControlRemove, "heretic-4b")
            assertEquals("REMOVED", after.lastOutcome?.state)
            val row = after.models.first { it.handle == "heretic-4b" }
            assertEquals(0L, row.presentBytes)
            assertEquals(
                listOf(VexLifeModelsFamily.ControlDownload),
                row.display.controls.map { it.controlRef },
            )
        }
    }

    @Test
    fun aDownloadThatCannotFinishSaysSoAndLeavesNothingRunning() = withHome { home, _ ->
        models(home).use { owner ->
            owner.control(VexLifeModelsFamily.ControlDownload, "heretic-4b")
            owner.awaitIdle()
            val after = owner.projection()
            assertNull(after.downloading)
            assertEquals("FAILED", after.lastOutcome?.state)
            // The model can be downloaded again; nothing about the failure is hidden.
            assertTrue(
                VexLifeModelsFamily.ControlDownload in
                    after.models
                        .first { it.handle == "heretic-4b" }
                        .display
                        .controls
                        .map { it.controlRef }
            )
        }
    }
}
