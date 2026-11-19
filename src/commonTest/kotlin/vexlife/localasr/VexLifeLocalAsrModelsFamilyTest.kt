package vexlife.localasr

import kotlin.test.Test
import kotlin.test.assertEquals

class VexLifeLocalAsrModelsFamilyTest {
    @Test
    fun controlsFollowArtifactPresenceSelectionAndOneDownloadAtATime() {
        assertEquals(
            listOf(
                VexLifeLocalAsrModelsFamily.ControlDownload,
            ),
            VexLifeLocalAsrModelsFamily.lawfulControls(
                VexLifeLocalAsrModelsFamily.State(
                    artifactState =
                        VexLifeLocalAsrModelsFamily.ArtifactAbsent,
                    selected =
                        false,
                    downloading =
                        false,
                    anyDownloadRunning =
                        false,
                ),
            ),
        )
        assertEquals(
            listOf(
                VexLifeLocalAsrModelsFamily.ControlUse,
                VexLifeLocalAsrModelsFamily.ControlRemove,
            ),
            VexLifeLocalAsrModelsFamily.lawfulControls(
                VexLifeLocalAsrModelsFamily.State(
                    artifactState =
                        VexLifeLocalAsrModelsFamily.ArtifactPresent,
                    selected =
                        false,
                    downloading =
                        false,
                    anyDownloadRunning =
                        false,
                ),
            ),
        )
        assertEquals(
            emptyList(),
            VexLifeLocalAsrModelsFamily.lawfulControls(
                VexLifeLocalAsrModelsFamily.State(
                    artifactState =
                        VexLifeLocalAsrModelsFamily.ArtifactPresent,
                    selected =
                        true,
                    downloading =
                        false,
                    anyDownloadRunning =
                        false,
                ),
            ),
        )
        assertEquals(
            listOf(
                VexLifeLocalAsrModelsFamily.ControlRedownload,
            ),
            VexLifeLocalAsrModelsFamily.lawfulControls(
                VexLifeLocalAsrModelsFamily.State(
                    artifactState =
                        VexLifeLocalAsrModelsFamily.ArtifactDamaged,
                    selected =
                        true,
                    downloading =
                        false,
                    anyDownloadRunning =
                        false,
                ),
            ),
        )
        assertEquals(
            emptyList(),
            VexLifeLocalAsrModelsFamily.lawfulControls(
                VexLifeLocalAsrModelsFamily.State(
                    artifactState =
                        VexLifeLocalAsrModelsFamily.ArtifactAbsent,
                    selected =
                        false,
                    downloading =
                        false,
                    anyDownloadRunning =
                        true,
                ),
            ),
        )
    }

    @Test
    fun selectedPresentModelMayAcquireOneMissingPlatformSpeechProvider() {
        assertEquals(
            listOf(
                VexLifeLocalAsrModelsFamily.ControlInstallProvider,
            ),
            VexLifeLocalAsrModelsFamily.lawfulControls(
                VexLifeLocalAsrModelsFamily.State(
                    artifactState =
                        VexLifeLocalAsrModelsFamily.ArtifactPresent,
                    selected =
                        true,
                    downloading =
                        false,
                    anyDownloadRunning =
                        false,
                    providerCurrent =
                        false,
                    providerAcquisitionSupported =
                        true,
                ),
            ),
        )

        assertEquals(
            emptyList(),
            VexLifeLocalAsrModelsFamily.lawfulControls(
                VexLifeLocalAsrModelsFamily.State(
                    artifactState =
                        VexLifeLocalAsrModelsFamily.ArtifactPresent,
                    selected =
                        true,
                    downloading =
                        false,
                    anyDownloadRunning =
                        false,
                    providerCurrent =
                        false,
                    providerAcquisitionSupported =
                        false,
                ),
            ),
        )
    }


    @Test
    fun typedControlsResolveToInertFamilyEffectsWithoutOwningRealization() {
        val absent =
            VexLifeLocalAsrModelsFamily.State(
                artifactState =
                    VexLifeLocalAsrModelsFamily.ArtifactAbsent,
                selected =
                    false,
                downloading =
                    false,
                anyDownloadRunning =
                    false,
            )
        assertEquals(
            VexLifeLocalAsrModelsFamily.AcquireSpeechModel(
                replace =
                    false,
            ),
            VexLifeLocalAsrModelsFamily.resolve(
                currentState =
                    absent,
                control =
                    VexLifeLocalAsrModelsFamily.Download,
            )
                ?.effect,
        )

        val presentUnselected =
            absent.copy(
                artifactState =
                    VexLifeLocalAsrModelsFamily.ArtifactPresent,
            )
        assertEquals(
            VexLifeLocalAsrModelsFamily.SelectSpeechModel,
            VexLifeLocalAsrModelsFamily.resolve(
                currentState =
                    presentUnselected,
                control =
                    VexLifeLocalAsrModelsFamily.Use,
            )
                ?.effect,
        )
        assertEquals(
            VexLifeLocalAsrModelsFamily.RemoveSpeechModel,
            VexLifeLocalAsrModelsFamily.resolve(
                currentState =
                    presentUnselected,
                control =
                    VexLifeLocalAsrModelsFamily.Remove,
            )
                ?.effect,
        )

        val providerMissing =
            presentUnselected.copy(
                selected =
                    true,
                providerCurrent =
                    false,
                providerAcquisitionSupported =
                    true,
            )
        assertEquals(
            VexLifeLocalAsrModelsFamily.AcquireSpeechProvider,
            VexLifeLocalAsrModelsFamily.resolve(
                currentState =
                    providerMissing,
                control =
                    VexLifeLocalAsrModelsFamily.InstallProvider,
            )
                ?.effect,
        )
        assertEquals(
            VexLifeLocalAsrModelsFamily.InstallProvider,
            VexLifeLocalAsrModelsFamily.controlForRef(
                VexLifeLocalAsrModelsFamily.ControlInstallProvider,
            ),
        )
    }

}

// [VXG RealForever]
