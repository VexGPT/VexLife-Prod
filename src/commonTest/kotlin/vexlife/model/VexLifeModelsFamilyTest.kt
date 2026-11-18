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
        runtimeConfiguration: HereticRuntimeConfiguration.Effective =
            HereticRuntimeConfiguration.effective(
                HereticRuntimeConfiguration.empty(),
                HereticModelCatalog.Models.Qwen4B,
            ),
        runtimeConfigurationPending: Boolean = false,
        anyDownloadRunning: Boolean = downloading,
        anySwitchRunning: Boolean = switching,
    ) =
        VexLifeModelsFamily.State(
            handle = "heretic-9b",
            artifactPresence = artifactPresence,
            downloading = downloading,
            chosen = chosen,
            targeted = targeted,
            serving = serving,
            switching = switching,
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
            ),
            controls(selected),
        )
        assertEquals(
            listOf(
                1,
                2,
                3,
            ),
            VexLifeModelsFamily
                .displayStateFor(
                    selected,
                )
                .runtimeConfigurationChoices
                .map {
                    it.promptCacheEntries
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
    }

    @Test
    fun followRecommendedIsDistinctFromAnExplicitValueThatCurrentlyMatchesIt() {
        val explicit =
            state(
                artifactPresence = VexLifeModelsFamily.ArtifactPresence.Present,
                targeted = true,
            )
        assertTrue(
            VexLifeModelsFamily.ControlUseRecommendedRuntimeConfiguration in controls(explicit),
        )

        val recommended =
            explicit.copy(
                runtimeConfiguration =
                    HereticRuntimeConfiguration.effective(
                        HereticRuntimeConfiguration.withRecommended(
                            value = HereticRuntimeConfiguration.empty(),
                            model = HereticModelCatalog.Models.Qwen4B,
                        ),
                        model = HereticModelCatalog.Models.Qwen4B,
                    ),
            )
        assertTrue(
            VexLifeModelsFamily.ControlUseRecommendedRuntimeConfiguration !in controls(recommended),
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
