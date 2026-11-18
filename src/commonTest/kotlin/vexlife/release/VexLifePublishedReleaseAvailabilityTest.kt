package vexlife.release

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class VexLifePublishedReleaseAvailabilityTest {
    private fun sha(character: Char): VexLifeReleaseIdentity.SourceSha =
        VexLifeReleaseIdentity.SourceSha(
            character.toString().repeat(40),
        )

    private fun stable(
        major: Int,
        minor: Int,
        patch: Int,
        sourceCharacter: Char,
    ): VexLifeReleaseIdentity.Value.Stable =
        VexLifeReleaseIdentity.Value.Stable(
            version =
                VexLifeReleaseIdentity.SemanticVersion(
                    major,
                    minor,
                    patch,
                ),
            sourceSha = sha(sourceCharacter),
        )

    private fun preview(
        sourceCharacter: Char,
    ): VexLifeReleaseIdentity.Value.Preview =
        VexLifeReleaseIdentity.Value.Preview(
            sourceSha = sha(sourceCharacter),
        )

    private fun published(
        identity: VexLifeReleaseIdentity.Value,
        publishedAtEpochMillis: Long,
    ): VexLifeReleaseIdentity.Published =
        VexLifeReleaseIdentity.Published(
            identity = identity,
            publishedAtEpochMillis = publishedAtEpochMillis,
            page =
                VexLifeReleaseIdentity.HttpsPage(
                    "https://github.com/VexGPT/VexLife-Prod/releases/example",
                ),
        )

    private fun currentObservation(
        observedAtEpochMillis: Long,
        vararg releases: VexLifeReleaseIdentity.Published,
    ): VexLifePublishedReleaseObservation.State.Current =
        VexLifePublishedReleaseObservation.State.Current(
            VexLifePublishedReleaseObservation.Snapshot(
                releases = releases.toList(),
                observedAtEpochMillis = observedAtEpochMillis,
            ),
        )

    @Test
    fun semanticVersionsCompareNumericallyInsteadOfLexically() {
        assertEquals(
            true,
            VexLifeReleaseIdentity.SemanticVersion(0, 10, 0) >
                VexLifeReleaseIdentity.SemanticVersion(0, 9, 9),
        )
    }

    @Test
    fun stableReleaseBecomesAvailableOnlyWhenPublishedVersionIsNewer() {
        val installed = stable(0, 4, 0, 'a')
        val same = published(stable(0, 4, 0, 'a'), 100)
        val newer = published(stable(0, 5, 0, 'b'), 200)

        assertIs<VexLifePublishedReleaseAvailability.State.Current>(
            VexLifePublishedReleaseAvailability.evaluate(
                installed = installed,
                observation = currentObservation(300, same),
            ),
        )

        val available =
            assertIs<VexLifePublishedReleaseAvailability.State.Available>(
                VexLifePublishedReleaseAvailability.evaluate(
                    installed = installed,
                    observation = currentObservation(300, same, newer),
                ),
            )

        assertEquals(
            newer,
            available.published,
        )
        assertEquals(
            VexLifePublishedReleaseAvailability.Currentness.Current,
            available.currentness,
        )
    }

    @Test
    fun sameStableVersionAtAnotherSourceIsHeldUnknown() {
        val state =
            assertIs<VexLifePublishedReleaseAvailability.State.Unknown>(
                VexLifePublishedReleaseAvailability.evaluate(
                    installed = stable(0, 4, 0, 'a'),
                    observation =
                        currentObservation(
                            300,
                            published(stable(0, 4, 0, 'b'), 200),
                        ),
                ),
            )

        assertEquals(
            VexLifePublishedReleaseAvailability.UnknownReason.StableIdentityConflict,
            state.reason,
        )
    }

    @Test
    fun stableCatalogBehindInstalledReleaseIsHeldUnknownInsteadOfClaimingCurrent() {
        val state =
            assertIs<VexLifePublishedReleaseAvailability.State.Unknown>(
                VexLifePublishedReleaseAvailability.evaluate(
                    installed = stable(0, 5, 0, 'b'),
                    observation =
                        currentObservation(
                            300,
                            published(stable(0, 4, 0, 'a'), 200),
                        ),
                ),
            )

        assertEquals(
            VexLifePublishedReleaseAvailability.UnknownReason.StableLineageBehindInstalled,
            state.reason,
        )
    }

    @Test
    fun unavailableRemoteTruthRetainsOnlyAnAlreadyKnownUpdate() {
        val installed = stable(0, 4, 0, 'a')
        val current = published(stable(0, 4, 0, 'a'), 100)
        val newer = published(stable(0, 5, 0, 'b'), 200)
        val priorUpdate =
            VexLifePublishedReleaseObservation.Snapshot(
                releases = listOf(current, newer),
                observedAtEpochMillis = 300,
            )

        val available =
            assertIs<VexLifePublishedReleaseAvailability.State.Available>(
                VexLifePublishedReleaseAvailability.evaluate(
                    installed = installed,
                    observation =
                        VexLifePublishedReleaseObservation.State.Unavailable(
                            attemptedAtEpochMillis = 400,
                            priorSnapshot = priorUpdate,
                        ),
                ),
            )

        assertEquals(
            VexLifePublishedReleaseAvailability.Currentness.StaleKnownUpdate,
            available.currentness,
        )

        val priorCurrent =
            VexLifePublishedReleaseObservation.Snapshot(
                releases = listOf(current),
                observedAtEpochMillis = 300,
            )
        val unknown =
            assertIs<VexLifePublishedReleaseAvailability.State.Unknown>(
                VexLifePublishedReleaseAvailability.evaluate(
                    installed = installed,
                    observation =
                        VexLifePublishedReleaseObservation.State.Unavailable(
                            attemptedAtEpochMillis = 400,
                            priorSnapshot = priorCurrent,
                        ),
                ),
            )

        assertEquals(
            VexLifePublishedReleaseAvailability.UnknownReason.PublishedTruthUnavailable,
            unknown.reason,
        )
    }

    @Test
    fun previewUsesPublishedLineageInsteadOfComparingItsZeroVersion() {
        val installed = preview('c')
        val installedPublished = published(installed, 100)
        val newer = published(preview('d'), 200)

        val available =
            assertIs<VexLifePublishedReleaseAvailability.State.Available>(
                VexLifePublishedReleaseAvailability.evaluate(
                    installed = installed,
                    observation = currentObservation(300, installedPublished, newer),
                ),
            )

        assertEquals(
            newer,
            available.published,
        )
    }

    @Test
    fun previewNotPresentInObservedPublicationLineageNeverInventsAnUpdate() {
        val state =
            assertIs<VexLifePublishedReleaseAvailability.State.Unknown>(
                VexLifePublishedReleaseAvailability.evaluate(
                    installed = preview('e'),
                    observation =
                        currentObservation(
                            300,
                            published(preview('c'), 100),
                            published(preview('d'), 200),
                        ),
                ),
            )

        assertEquals(
            VexLifePublishedReleaseAvailability.UnknownReason.PreviewLineageNotObserved,
            state.reason,
        )
    }
}

// [VXG RealForever]
