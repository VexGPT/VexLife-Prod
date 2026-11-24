package vexlife.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vexlife.model.heretic.HereticModelCatalog
import vexlife.model.heretic.HereticRuntimeConfiguration

/** What each model Control may do, by the model's state in this Home. */
class VexLifeModelsFamilyTest {
    private fun state(
        artifactPresence: VexLifeModelsFamily.ArtifactPresence,
        chosen: Boolean = false,
        targeted: Boolean = chosen,
        serving: Boolean = false,
        downloading: Boolean = false,
        switching: Boolean = false,
        imported: Boolean = false,
        runtimeConfiguration: HereticRuntimeConfiguration.Effective? =
            HereticRuntimeConfiguration.effective(
                HereticRuntimeConfiguration.empty(),
                HereticModelCatalog.Models.Qwen4B,
            ),
        runtimeConfigurationPending: Boolean = false,
        anyDownloadRunning: Boolean = downloading,
        anySwitchRunning: Boolean = switching,
    ) =
        VexLifeModelsFamily.State(
            handle = "heretic-4b",
            artifactPresence = artifactPresence,
            downloading = downloading,
            chosen = chosen,
            targeted = targeted,
            serving = serving,
            switching = switching,
            imported = imported,
            runtimeConfiguration = runtimeConfiguration,
            runtimeConfigurationPending = runtimeConfigurationPending,
            anyDownloadRunning = anyDownloadRunning,
            anySwitchRunning = anySwitchRunning,
        )

    private fun controls(state: VexLifeModelsFamily.State) =
        VexLifeModelsFamily.displayStateFor(state).controls.map { it.controlRef }

    @Test
    fun anAbsentModelCanBeSelectedOrDownloadedWithoutStartingIt() {
        val current =
            state(
                VexLifeModelsFamily.ArtifactPresence.Absent,
            )
        assertEquals(
            listOf(
                VexLifeModelsFamily.ControlSelect,
                VexLifeModelsFamily.ControlDownload,
            ),
            controls(current),
        )
        val selection =
            VexLifeModelsFamily.resolve(
                current,
                VexLifeModelsFamily.Select,
            )
        assertNull(selection?.effect)
        assertTrue(selection?.admittedState?.targeted == true)
        assertEquals(
            VexLifeModelsFamily.Effect.AcquireArtifacts,
            VexLifeModelsFamily.resolve(
                current,
                VexLifeModelsFamily.Download,
            )?.effect,
        )
        assertNull(
            VexLifeModelsFamily.resolve(
                current,
                VexLifeModelsFamily.Use,
            ),
        )
    }

    @Test
    fun aPresentTargetMustBeSelectedBeforeItCanBeStarted() {
        val unselected =
            state(
                VexLifeModelsFamily.ArtifactPresence.Present,
            )
        assertEquals(
            listOf(
                VexLifeModelsFamily.ControlSelect,
                VexLifeModelsFamily.ControlRemove,
            ),
            controls(unselected),
        )
        assertNull(
            VexLifeModelsFamily.resolve(
                unselected,
                VexLifeModelsFamily.Use,
            ),
        )

        val selected =
            state(
                VexLifeModelsFamily.ArtifactPresence.Present,
                targeted = true,
            )
        assertEquals(
            listOf(
                VexLifeModelsFamily.ControlUse,
                VexLifeModelsFamily.ControlUseRecommendedRuntimeConfiguration,
                VexLifeModelsFamily.ControlRemove,
            ),
            controls(selected),
        )
        val configurationChoices =
            VexLifeModelsFamily
                .displayStateFor(
                    selected,
                )
                .runtimeConfigurationChoices
        assertEquals(
            listOf(
                1,
                2,
                3,
            ),
            configurationChoices.map {
                it.promptCacheEntries
            },
        )
        assertEquals(
            listOf(
                "workspace.label.model-cache-level-1",
                "workspace.label.model-cache-level-2",
                "workspace.label.model-cache-level-3",
            ),
            configurationChoices.map {
                it.label.key
            },
        )
        assertEquals(
            listOf(
                null,
                "workspace.label.model-recommended",
                "workspace.label.model-experimental",
            ),
            configurationChoices.map {
                it.standing?.key
            },
        )
        assertEquals(
            VexLifeModelsFamily.Effect.SwitchModel,
            VexLifeModelsFamily.resolve(
                selected,
                VexLifeModelsFamily.Use,
            )?.effect,
        )
    }

    @Test
    fun stagedUnusedModelCanRemoveFilesAndImportedHistorySeparately() {
        val present =
            state(
                artifactPresence = VexLifeModelsFamily.ArtifactPresence.Present,
                targeted = true,
                imported = true,
            )
        val remove =
            VexLifeModelsFamily.resolve(
                present,
                VexLifeModelsFamily.Remove,
            )
        assertEquals(
            VexLifeModelsFamily.Effect.RemoveArtifacts,
            remove?.effect,
        )
        assertEquals(
            false,
            remove?.admittedState?.targeted,
        )

        val absent =
            present.copy(
                artifactPresence = VexLifeModelsFamily.ArtifactPresence.Absent,
            )
        assertEquals(
            listOf(
                VexLifeModelsFamily.ControlDownload,
                VexLifeModelsFamily.ControlForgetImportedModel,
            ),
            controls(absent),
        )
        val forget =
            VexLifeModelsFamily.resolve(
                absent,
                VexLifeModelsFamily.ForgetImportedModel,
            )
        assertEquals(
            VexLifeModelsFamily.Effect.ForgetImportedModel,
            forget?.effect,
        )
        assertEquals(
            false,
            forget?.admittedState?.targeted,
        )
    }

    @Test
    fun currentServingModelRestartsOnlyWhenItsConfigurationIsPending() {
        val current =
            state(
                VexLifeModelsFamily.ArtifactPresence.Present,
                chosen = true,
                serving = true,
            )
        assertEquals(
            listOf(
                VexLifeModelsFamily.ControlUseRecommendedRuntimeConfiguration,
            ),
            controls(current),
        )
        assertNull(
            VexLifeModelsFamily.resolve(
                current,
                VexLifeModelsFamily.Use,
            ),
        )

        val changed =
            current.copy(
                runtimeConfigurationPending = true,
            )
        assertEquals(
            listOf(
                VexLifeModelsFamily.ControlUse,
                VexLifeModelsFamily.ControlUseRecommendedRuntimeConfiguration,
            ),
            controls(changed),
        )
        assertEquals(
            VexLifeModelsFamily.Effect.SwitchModel,
            VexLifeModelsFamily.resolve(
                changed,
                VexLifeModelsFamily.Use,
            )?.effect,
        )
    }

    @Test
    fun aRealizationWithoutRuntimeConfigurationShowsAndAdmitsNoConfigurationControls() {
        val selected =
            state(
                artifactPresence = VexLifeModelsFamily.ArtifactPresence.Present,
                targeted = true,
                runtimeConfiguration = null,
            )
        val display =
            VexLifeModelsFamily.displayStateFor(
                selected,
            )

        assertEquals(
            false,
            display.runtimeConfigurationApplicable,
        )
        assertNull(
            display.runtimeConfigurationSummary,
        )
        assertNull(
            display.runtimeConfigurationRecommendation,
        )
        assertEquals(
            emptyList(),
            display.runtimeConfigurationChoices,
        )
        assertEquals(
            VexLifeModelsFamily.Effect.UseRecommendedRuntimeConfiguration,
            VexLifeModelsFamily.resolve(
                selected,
                VexLifeModelsFamily.UseRecommendedRuntimeConfiguration,
            )?.effect,
        )
        assertNull(
            VexLifeModelsFamily.resolve(
                selected,
                VexLifeModelsFamily.SetPromptCacheEntries(
                    2,
                ),
            ),
        )
        assertEquals(
            VexLifeModelsFamily.Effect.SwitchModel,
            VexLifeModelsFamily.resolve(
                selected,
                VexLifeModelsFamily.Use,
            )
                ?.effect,
        )
    }

    @Test
    fun onlyTheSelectedTargetCanChangeRuntimeConfiguration() {
        val unselected =
            state(
                VexLifeModelsFamily.ArtifactPresence.Present,
            )
        assertNull(
            VexLifeModelsFamily.resolve(
                unselected,
                VexLifeModelsFamily.UseRecommendedRuntimeConfiguration,
            ),
        )
        assertNull(
            VexLifeModelsFamily.resolve(
                unselected,
                VexLifeModelsFamily.SetPromptCacheEntries(1),
            ),
        )

        val selected =
            unselected.copy(
                targeted = true,
            )
        assertNull(
            VexLifeModelsFamily.resolve(
                selected,
                VexLifeModelsFamily.UseRecommendedRuntimeConfiguration,
            ),
        )
        assertNull(
            VexLifeModelsFamily.resolve(
                selected,
                VexLifeModelsFamily.SetPromptCacheEntries(1),
            ),
        )
        assertEquals(
            VexLifeModelsFamily.Effect.SetPromptCacheEntries(2),
            VexLifeModelsFamily.resolve(
                selected,
                VexLifeModelsFamily.SetPromptCacheEntries(2),
            )?.effect,
        )
        assertNull(
            VexLifeModelsFamily.resolve(
                selected,
                VexLifeModelsFamily.SetPromptCacheEntries(4),
            ),
        )

        val explicitTwo =
            selected.copy(
                runtimeConfiguration =
                    HereticRuntimeConfiguration.effective(
                        HereticRuntimeConfiguration.withPromptCacheEntries(
                            value = HereticRuntimeConfiguration.empty(),
                            model = HereticModelCatalog.Models.Qwen4B,
                            promptCacheEntries = 2,
                        ),
                        model = HereticModelCatalog.Models.Qwen4B,
                    ),
            )
        assertNull(
            VexLifeModelsFamily.resolve(
                explicitTwo,
                VexLifeModelsFamily.UseRecommendedRuntimeConfiguration,
            ),
        )
    }

    @Test
    fun legacyRecommendedCandidateCanMigrateToTheExplicitRecommendedValue() {
        val legacy =
            state(
                artifactPresence = VexLifeModelsFamily.ArtifactPresence.Present,
                targeted = true,
                runtimeConfiguration =
                    HereticRuntimeConfiguration.effective(
                        HereticRuntimeConfiguration.withLegacyRecommended(
                            value = HereticRuntimeConfiguration.empty(),
                            model = HereticModelCatalog.Models.Qwen4B,
                        ),
                        model = HereticModelCatalog.Models.Qwen4B,
                    ),
            )
        assertTrue(
            VexLifeModelsFamily.ControlUseRecommendedRuntimeConfiguration in controls(legacy),
        )
        assertEquals(
            VexLifeModelsFamily.Effect.UseRecommendedRuntimeConfiguration,
            VexLifeModelsFamily.resolve(
                legacy,
                VexLifeModelsFamily.UseRecommendedRuntimeConfiguration,
            )?.effect,
        )
    }

    @Test
    fun aPartialModelResumesAndADamagedOneDownloadsAgain() {
        assertEquals(
            listOf(
                VexLifeModelsFamily.ControlSelect,
                VexLifeModelsFamily.ControlDownload,
                VexLifeModelsFamily.ControlRemove,
            ),
            controls(
                state(
                    VexLifeModelsFamily.ArtifactPresence.Partial,
                ),
            ),
        )
        assertEquals(
            listOf(
                VexLifeModelsFamily.ControlSelect,
                VexLifeModelsFamily.ControlRedownload,
                VexLifeModelsFamily.ControlRemove,
            ),
            controls(
                state(
                    VexLifeModelsFamily.ArtifactPresence.Damaged,
                ),
            ),
        )
        assertEquals(
            VexLifeModelsFamily.Effect.ReplaceArtifacts,
            VexLifeModelsFamily.resolve(
                state(
                    VexLifeModelsFamily.ArtifactPresence.Damaged,
                ),
                VexLifeModelsFamily.Redownload,
            )?.effect,
        )
    }

    @Test
    fun whileAnyDownloadOrSwitchRunsNoOtherEffectIsLawful() {
        val busy =
            state(
                VexLifeModelsFamily.ArtifactPresence.Partial,
                anyDownloadRunning = true,
            )
        assertEquals(
            emptyList(),
            controls(
                busy,
            ),
        )
        assertEquals(
            emptyList(),
            controls(
                state(
                    VexLifeModelsFamily.ArtifactPresence.Partial,
                    downloading = true,
                ),
            ),
        )
        assertEquals(
            emptyList(),
            controls(
                state(
                    VexLifeModelsFamily.ArtifactPresence.Present,
                    anySwitchRunning = true,
                ),
            ),
        )
    }
}

// [VXG RealForever]
