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
import vexlife.model.heretic.HereticRuntimeConfiguration
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
        serving: () -> String? = { HereticModelCatalog.Models.Qwen27B.modelRef },
        modelSwitchRealization: VexLifeModels.ModelSwitchRealization? = null,
        failDownload: Boolean = true,
    ) =
        VexLifeModels(
            productHome = home,
            servingModelRef = serving,
            totalMemoryBytes = { 16 * gib },
            modelSwitchRealization = modelSwitchRealization,
            acquirer = { _, _ ->
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
            assertEquals(false, certified.fitsThisMac)
            assertEquals(true, projection.models.first { it.handle == "heretic-9b" }.fitsThisMac)
            assertEquals(
                listOf(
                    VexLifeModelsFamily.ControlSelect,
                    VexLifeModelsFamily.ControlDownload,
                ),
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
    fun usingADownloadedModelConvergesTheServingModelNow() = withHome { home, fixture ->
        val nine = HereticModelCatalog.Models.Qwen9B
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

        var serving: String? = HereticModelCatalog.Models.Qwen27B.modelRef
        val phases = mutableListOf<VexLifeModels.SwitchPhase>()
        val modelSwitchRealization =
            VexLifeModels.ModelSwitchRealization {
                entry,
                observe,
                ->
                observe(VexLifeModels.SwitchPhase.WaitingForCurrentTurn)
                observe(VexLifeModels.SwitchPhase.PreparingTarget)
                observe(VexLifeModels.SwitchPhase.SwitchingRuntime)
                phases +=
                    listOf(
                        VexLifeModels.SwitchPhase.WaitingForCurrentTurn,
                        VexLifeModels.SwitchPhase.PreparingTarget,
                        VexLifeModels.SwitchPhase.SwitchingRuntime,
                    )
                serving = entry.modelRef
            }

        models(
            home = home,
            serving = { serving },
            modelSwitchRealization = modelSwitchRealization,
        )
            .use {
                owner ->
                val selected =
                    owner.control(
                        VexLifeModelsFamily.ControlSelect,
                        "heretic-9b",
                    )
                assertEquals(
                    VexLifeModels.OutcomeState.Selected.stateRef,
                    selected.lastOutcome?.state,
                )
                assertFalse(
                    selected.models.first { it.handle == "heretic-9b" }.chosen,
                )
                assertTrue(
                    selected.models.first { it.handle == "heretic-9b" }.targeted,
                )
                assertFalse(
                    selected.models.first { it.handle == "heretic-9b" }.serving,
                )
                assertEquals(
                    HereticModelCatalog.Models.Qwen27B.modelRef,
                    serving,
                )
                assertTrue(
                    phases.isEmpty(),
                )
                assertEquals(
                    HereticModelCatalog.Models.Qwen27B,
                    FileHereticModelConfigurationStore(home).selectedModel(fixture.manifest),
                )

                owner.control(
                    VexLifeModelsFamily.ControlUse,
                    "heretic-9b",
                )
                owner.awaitIdle()
                val after = owner.projection()
                assertEquals(VexLifeModels.OutcomeState.Switched.stateRef, after.lastOutcome?.state)
                assertFalse(after.switchPending)
                assertEquals(null, after.switching)
                assertTrue(after.models.first { it.handle == "heretic-9b" }.chosen)
                assertTrue(after.models.first { it.handle == "heretic-9b" }.serving)
                assertEquals(
                    listOf(
                        VexLifeModels.SwitchPhase.WaitingForCurrentTurn,
                        VexLifeModels.SwitchPhase.PreparingTarget,
                        VexLifeModels.SwitchPhase.SwitchingRuntime,
                    ),
                    phases,
                )
                assertEquals(
                    nine,
                    FileHereticModelConfigurationStore(home).selectedModel(fixture.manifest),
                )
                assertTrue(Files.exists(folder.resolve("config.json")))
            }
    }

    @Test
    fun removingAModelFreesItsFilesAndKeepsItOffered() = withHome { home, _ ->
        val four = HereticModelCatalog.Models.Qwen4B
        place(home, four, "config.json")
        models(home).use { owner ->
            val after = owner.control(VexLifeModelsFamily.ControlRemove, "heretic-4b")
            assertEquals(VexLifeModels.OutcomeState.Removed.stateRef, after.lastOutcome?.state)
            val row = after.models.first { it.handle == "heretic-4b" }
            assertEquals(0L, row.presentBytes)
            assertEquals(
                listOf(
                    VexLifeModelsFamily.ControlSelect,
                    VexLifeModelsFamily.ControlDownload,
                ),
                row.display.controls.map { it.controlRef },
            )
        }
    }

    @Test
    fun selectedModelConfigurationChangesWithoutStartingTheRuntime() =
        withHome { home, _ ->
            models(
                home = home,
            )
                .use {
                    owner ->
                    val before =
                        owner.projection()
                            .models
                            .first {
                                it.handle == "heretic-4b"
                            }
                    assertEquals(
                        1,
                        before.runtimeConfiguration.promptCacheEntries,
                    )
                    assertEquals(
                        1,
                        before.runtimeConfiguration.recommendedPromptCacheEntries,
                    )
                    assertEquals(
                        3,
                        before.runtimeConfiguration.maximumPromptCacheEntries,
                    )
                    assertEquals(
                        HereticRuntimeConfiguration.RecommendationHeldExperiment,
                        before.runtimeConfiguration.recommendationStanding,
                    )

                    owner.control(
                        VexLifeModelsFamily.ControlSelect,
                        "heretic-4b",
                    )
                    val experimental =
                        owner.control(
                            VexLifeModelsFamily.ControlSetPromptCacheEntries,
                            "heretic-4b",
                            promptCacheEntries = 2,
                        )
                    assertEquals(
                        2,
                        experimental.models
                            .first {
                                it.handle == "heretic-4b"
                            }
                            .runtimeConfiguration
                            .promptCacheEntries,
                    )
                    val configured =
                        owner.control(
                            VexLifeModelsFamily.ControlUseRecommendedRuntimeConfiguration,
                            "heretic-4b",
                        )
                    val four =
                        configured.models.first {
                            it.handle == "heretic-4b"
                        }

                    assertFalse(
                        four.chosen,
                    )
                    assertTrue(
                        four.targeted,
                    )
                    assertFalse(
                        four.serving,
                    )
                    assertEquals(
                        HereticRuntimeConfiguration.ModeExplicit,
                        four.runtimeConfiguration.mode,
                    )
                    assertEquals(
                        1,
                        four.runtimeConfiguration.promptCacheEntries,
                    )
                    assertEquals(
                        "heretic-27b",
                        configured.servingHandle,
                    )
                    assertEquals(
                        "heretic-4b",
                        configured.selectedHandle,
                    )
                    assertNull(
                        configured.switching,
                    )
                    val manifest =
                        requireNotNull(
                            vexlife.home.VexLifeProdHomeFiles.observeExisting(
                                home,
                            ),
                        ).manifest
                    assertEquals(
                        HereticModelCatalog.Models.Qwen27B,
                        FileHereticModelConfigurationStore(home).selectedModel(manifest),
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
            assertEquals(VexLifeModels.OutcomeState.Failed.stateRef, after.lastOutcome?.state)
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
