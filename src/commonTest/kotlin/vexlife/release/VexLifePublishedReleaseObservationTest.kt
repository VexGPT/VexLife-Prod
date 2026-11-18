package vexlife.release

import kotlin.test.Test
import kotlin.test.assertFailsWith

class VexLifePublishedReleaseObservationTest {
    private fun sha(character: Char) =
        VexLifeReleaseIdentity.SourceSha(
            character.toString().repeat(40),
        )

    private fun published(
        identity: VexLifeReleaseIdentity.Value,
    ) =
        VexLifeReleaseIdentity.Published(
            identity = identity,
            publishedAtEpochMillis = 100,
            page =
                VexLifeReleaseIdentity.HttpsPage(
                    "https://github.com/VexGPT/VexLife-Prod/releases/example",
                ),
        )

    @Test
    fun twoStableSourcesCannotClaimOneSemanticVersion() {
        assertFailsWith<IllegalArgumentException> {
            VexLifePublishedReleaseObservation.Snapshot(
                releases =
                    listOf(
                        published(
                            VexLifeReleaseIdentity.Value.Stable(
                                version =
                                    VexLifeReleaseIdentity.SemanticVersion(
                                        0,
                                        5,
                                        0,
                                    ),
                                sourceSha = sha('a'),
                            ),
                        ),
                        published(
                            VexLifeReleaseIdentity.Value.Stable(
                                version =
                                    VexLifeReleaseIdentity.SemanticVersion(
                                        0,
                                        5,
                                        0,
                                    ),
                                sourceSha = sha('b'),
                            ),
                        ),
                    ),
                observedAtEpochMillis = 200,
            )
        }
    }

    @Test
    fun duplicatePreviewSourceCannotAppearTwice() {
        assertFailsWith<IllegalArgumentException> {
            VexLifePublishedReleaseObservation.Snapshot(
                releases =
                    listOf(
                        published(
                            VexLifeReleaseIdentity.Value.Preview(
                                sha('c'),
                            ),
                        ),
                        published(
                            VexLifeReleaseIdentity.Value.Preview(
                                sha('c'),
                            ),
                        ),
                    ),
                observedAtEpochMillis = 200,
            )
        }
    }
}

// [VXG RealForever]
