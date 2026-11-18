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
}

// [VXG RealForever]
