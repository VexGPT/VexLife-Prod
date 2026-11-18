package vexlife.release

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class VexLifeInstalledReleaseIdentityTest {
    private fun sha(character: Char) =
        VexLifeReleaseIdentity.SourceSha(
            character.toString().repeat(40),
        )

    private fun published(
        identity: VexLifeReleaseIdentity.Value,
        at: Long,
    ) =
        VexLifeReleaseIdentity.Published(
            identity = identity,
            publishedAtEpochMillis = at,
            page =
                VexLifeReleaseIdentity.HttpsPage(
                    "https://github.com/VexGPT/VexLife-Prod/releases/example",
                ),
        )

    @Test
    fun exactPublishedSourceIdentifiesItsPreviewChannel() {
        val source = sha('a')
        val preview =
            VexLifeReleaseIdentity.Value.Preview(
                source,
            )
        val snapshot =
            VexLifePublishedReleaseObservation.Snapshot(
                releases =
                    listOf(
                        published(
                            preview,
                            100,
                        ),
                    ),
                observedAtEpochMillis = 200,
            )

        assertEquals(
            preview,
            assertIs<VexLifeInstalledReleaseIdentity.State.Identified>(
                VexLifeInstalledReleaseIdentity.resolve(
                    sourceSha = source,
                    snapshot = snapshot,
                ),
            )
                .identity,
        )
    }

    @Test
    fun unpublishedSourceIsHeldUnknown() {
        val state =
            assertIs<VexLifeInstalledReleaseIdentity.State.Unknown>(
                VexLifeInstalledReleaseIdentity.resolve(
                    sourceSha = sha('b'),
                    snapshot =
                        VexLifePublishedReleaseObservation.Snapshot(
                            releases = emptyList(),
                            observedAtEpochMillis = 200,
                        ),
                ),
            )

        assertEquals(
            VexLifeInstalledReleaseIdentity.UnknownReason.SourceNotPublished,
            state.reason,
        )
    }

    @Test
    fun sameSourcePublishedAsPreviewAndStableIsAmbiguousWithoutLocalVersionMetadata() {
        val source = sha('c')
        val snapshot =
            VexLifePublishedReleaseObservation.Snapshot(
                releases =
                    listOf(
                        published(
                            VexLifeReleaseIdentity.Value.Preview(
                                source,
                            ),
                            100,
                        ),
                        published(
                            VexLifeReleaseIdentity.Value.Stable(
                                version =
                                    VexLifeReleaseIdentity.SemanticVersion(
                                        0,
                                        5,
                                        0,
                                    ),
                                sourceSha = source,
                            ),
                            200,
                        ),
                    ),
                observedAtEpochMillis = 300,
            )

        val state =
            assertIs<VexLifeInstalledReleaseIdentity.State.Unknown>(
                VexLifeInstalledReleaseIdentity.resolve(
                    sourceSha = source,
                    snapshot = snapshot,
                ),
            )

        assertEquals(
            VexLifeInstalledReleaseIdentity.UnknownReason.AmbiguousPublicationIdentity,
            state.reason,
        )
    }
}

// [VXG RealForever]
