package vexlife.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** What each model Control may do, by the model's state in this Home. */
class VexLifeModelsFamilyTest {
    private fun state(
        files: VexLifeModelsFamily.Files,
        chosen: Boolean = false,
        serving: Boolean = false,
        downloading: Boolean = false,
        switching: Boolean = false,
        anyDownloadRunning: Boolean = downloading,
        anySwitchRunning: Boolean = switching,
    ) =
        VexLifeModelsFamily.State(
            "heretic-9b",
            files,
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
            controls(state(VexLifeModelsFamily.Files.ABSENT)),
        )
        assertEquals(
            VexLifeModelsFamily.Effect.AcquireModel,
            VexLifeModelsFamily.resolve(
                    state(VexLifeModelsFamily.Files.ABSENT),
                    VexLifeModelsFamily.Download,
                )
                ?.effect,
        )
    }

    @Test
    fun aPresentModelCanBeUsedUntilChosenAndServingAreBothCurrent() {
        assertEquals(
            listOf(VexLifeModelsFamily.ControlUse, VexLifeModelsFamily.ControlRemove),
            controls(state(VexLifeModelsFamily.Files.PRESENT)),
        )
        assertEquals(
            emptyList(),
            controls(state(VexLifeModelsFamily.Files.PRESENT, chosen = true, serving = true)),
        )
        assertEquals(
            listOf(VexLifeModelsFamily.ControlUse),
            controls(state(VexLifeModelsFamily.Files.PRESENT, serving = true)),
        )
        assertEquals(
            listOf(VexLifeModelsFamily.ControlUse),
            controls(state(VexLifeModelsFamily.Files.PRESENT, chosen = true)),
        )
        assertNull(
            VexLifeModelsFamily.resolve(
                state(VexLifeModelsFamily.Files.PRESENT, serving = true),
                VexLifeModelsFamily.Remove,
            )
        )
        assertNull(
            VexLifeModelsFamily.resolve(
                state(VexLifeModelsFamily.Files.PRESENT, chosen = true),
                VexLifeModelsFamily.Remove,
            )
        )
        assertEquals(
            VexLifeModelsFamily.Effect.SwitchModel,
            VexLifeModelsFamily.resolve(
                state(VexLifeModelsFamily.Files.PRESENT, chosen = true),
                VexLifeModelsFamily.Use,
            )?.effect,
        )
    }

    @Test
    fun aPartialModelResumesAndADamagedOneDownloadsAgain() {
        assertEquals(
            listOf(VexLifeModelsFamily.ControlDownload, VexLifeModelsFamily.ControlRemove),
            controls(state(VexLifeModelsFamily.Files.PARTIAL)),
        )
        assertEquals(
            listOf(VexLifeModelsFamily.ControlRedownload, VexLifeModelsFamily.ControlRemove),
            controls(state(VexLifeModelsFamily.Files.DAMAGED)),
        )
        assertEquals(
            VexLifeModelsFamily.Effect.ReplaceModel,
            VexLifeModelsFamily.resolve(
                    state(VexLifeModelsFamily.Files.DAMAGED),
                    VexLifeModelsFamily.Redownload,
                )
                ?.effect,
        )
    }

    @Test
    fun whileAnyDownloadRunsNoOtherFileEffectIsLawful() {
        val busy = state(VexLifeModelsFamily.Files.PARTIAL, anyDownloadRunning = true)
        assertEquals(emptyList(), controls(busy))
        assertEquals(
            emptyList(),
            controls(state(VexLifeModelsFamily.Files.PARTIAL, downloading = true)),
        )
        // Model-file mutation and runtime convergence never overlap.
        assertEquals(
            emptyList(),
            controls(state(VexLifeModelsFamily.Files.PRESENT, anyDownloadRunning = true)),
        )
        assertEquals(
            emptyList(),
            controls(state(VexLifeModelsFamily.Files.PRESENT, anySwitchRunning = true)),
        )
    }
}
