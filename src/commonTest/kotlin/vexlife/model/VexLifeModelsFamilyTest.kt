package vexlife.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** What each model Control may do, by the model's state in this Home. */
class VexLifeModelsFamilyTest {
    private fun state(
        artifactPresence: VexLifeModelsFamily.ArtifactPresence,
        chosen: Boolean = false,
        serving: Boolean = false,
        downloading: Boolean = false,
        switching: Boolean = false,
        anyDownloadRunning: Boolean = downloading,
        anySwitchRunning: Boolean = switching,
    ) =
        VexLifeModelsFamily.State(
            "heretic-9b",
            artifactPresence,
            downloading,
            chosen,
            serving,
            switching,
            anyDownloadRunning,
            anySwitchRunning,
        )

    private fun controls(state: VexLifeModelsFamily.State) =
        VexLifeModelsFamily.displayStateFor(state).controls.map { it.controlRef }

    @Test
    fun anAbsentModelCanOnlyBeDownloaded() {
        assertEquals(
            listOf(VexLifeModelsFamily.ControlDownload),
            controls(state(VexLifeModelsFamily.ArtifactPresence.ABSENT)),
        )
        assertEquals(
            VexLifeModelsFamily.Effect.AcquireArtifacts,
            VexLifeModelsFamily.resolve(
                    state(VexLifeModelsFamily.ArtifactPresence.ABSENT),
                    VexLifeModelsFamily.Download,
                )
                ?.effect,
        )
    }

    @Test
    fun aPresentModelCanBeUsedUntilChosenAndServingAreBothCurrent() {
        assertEquals(
            listOf(VexLifeModelsFamily.ControlUse, VexLifeModelsFamily.ControlRemove),
            controls(state(VexLifeModelsFamily.ArtifactPresence.PRESENT)),
        )
        assertEquals(
            emptyList(),
            controls(state(VexLifeModelsFamily.ArtifactPresence.PRESENT, chosen = true, serving = true)),
        )
        assertEquals(
            listOf(VexLifeModelsFamily.ControlUse),
            controls(state(VexLifeModelsFamily.ArtifactPresence.PRESENT, serving = true)),
        )
        assertEquals(
            listOf(VexLifeModelsFamily.ControlUse),
            controls(state(VexLifeModelsFamily.ArtifactPresence.PRESENT, chosen = true)),
        )
        assertNull(
            VexLifeModelsFamily.resolve(
                state(VexLifeModelsFamily.ArtifactPresence.PRESENT, serving = true),
                VexLifeModelsFamily.Remove,
            )
        )
        assertNull(
            VexLifeModelsFamily.resolve(
                state(VexLifeModelsFamily.ArtifactPresence.PRESENT, chosen = true),
                VexLifeModelsFamily.Remove,
            )
        )
        assertEquals(
            VexLifeModelsFamily.Effect.SwitchModel,
            VexLifeModelsFamily.resolve(
                state(VexLifeModelsFamily.ArtifactPresence.PRESENT, chosen = true),
                VexLifeModelsFamily.Use,
            )?.effect,
        )
    }

    @Test
    fun aPartialModelResumesAndADamagedOneDownloadsAgain() {
        assertEquals(
            listOf(VexLifeModelsFamily.ControlDownload, VexLifeModelsFamily.ControlRemove),
            controls(state(VexLifeModelsFamily.ArtifactPresence.PARTIAL)),
        )
        assertEquals(
            listOf(VexLifeModelsFamily.ControlRedownload, VexLifeModelsFamily.ControlRemove),
            controls(state(VexLifeModelsFamily.ArtifactPresence.DAMAGED)),
        )
        assertEquals(
            VexLifeModelsFamily.Effect.ReplaceArtifacts,
            VexLifeModelsFamily.resolve(
                    state(VexLifeModelsFamily.ArtifactPresence.DAMAGED),
                    VexLifeModelsFamily.Redownload,
                )
                ?.effect,
        )
    }

    @Test
    fun whileAnyDownloadRunsNoOtherFileEffectIsLawful() {
        val busy = state(VexLifeModelsFamily.ArtifactPresence.PARTIAL, anyDownloadRunning = true)
        assertEquals(emptyList(), controls(busy))
        assertEquals(
            emptyList(),
            controls(state(VexLifeModelsFamily.ArtifactPresence.PARTIAL, downloading = true)),
        )
        // Model-file mutation and runtime convergence never overlap.
        assertEquals(
            emptyList(),
            controls(state(VexLifeModelsFamily.ArtifactPresence.PRESENT, anyDownloadRunning = true)),
        )
        assertEquals(
            emptyList(),
            controls(state(VexLifeModelsFamily.ArtifactPresence.PRESENT, anySwitchRunning = true)),
        )
    }
}
