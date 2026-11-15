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
        anyDownloadRunning: Boolean = downloading,
    ) =
        VexLifeModelsFamily.State(
            "heretic-9b",
            files,
            downloading,
            chosen,
            serving,
            anyDownloadRunning,
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
    fun aPresentModelCanBeUsedOrRemovedUnlessItIsChosenOrServing() {
        assertEquals(
            listOf(VexLifeModelsFamily.ControlUse, VexLifeModelsFamily.ControlRemove),
            controls(state(VexLifeModelsFamily.Files.PRESENT)),
        )
        assertEquals(
            emptyList(),
            controls(state(VexLifeModelsFamily.Files.PRESENT, chosen = true, serving = true)),
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
        // A present model may still be chosen while another one downloads.
        assertEquals(
            listOf(VexLifeModelsFamily.ControlUse),
            controls(state(VexLifeModelsFamily.Files.PRESENT, anyDownloadRunning = true)),
        )
    }
}
