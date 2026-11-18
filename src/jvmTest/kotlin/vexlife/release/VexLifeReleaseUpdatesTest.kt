package vexlife.release

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class VexLifeReleaseUpdatesTest {
    private fun sha(
        character: Char,
    ) =
        VexLifeReleaseIdentity.SourceSha(
            character
                .toString()
                .repeat(
                    40,
                ),
        )

    private fun publishedPreview(
        character: Char,
        at: Long,
    ) =
        VexLifeReleaseIdentity.Published(
            identity =
                VexLifeReleaseIdentity.Value.Preview(
                    sha(
                        character,
                    ),
                ),
            publishedAtEpochMillis = at,
            page =
                VexLifeReleaseIdentity.HttpsPage(
                    "https://github.com/VexGPT/VexLife-Prod/releases/tag/private-service-image-" +
                        character
                            .toString()
                            .repeat(
                                40,
                            ),
                ),
        )

    @Test
    fun olderPublishedPreviewSeesNewerPreviewAndAcknowledgementSurvivesRestart() {
        val home =
            Files.createTempDirectory(
                "vexlife-release-updates-",
            )
                .toAbsolutePath()
        try {
            val installed =
                publishedPreview(
                    'a',
                    100,
                )
            val newer =
                publishedPreview(
                    'b',
                    200,
                )
            val snapshot =
                VexLifePublishedReleaseObservation.Snapshot(
                    releases =
                        listOf(
                            newer,
                            installed,
                        ),
                    observedAtEpochMillis = 300,
                )
            val observer =
                VexLifeReleaseUpdates.Observer {
                    _,
                    observedAt,
                    ->
                    VexLifePublishedReleaseObservation.State.Current(
                        snapshot.copy(
                            observedAtEpochMillis = observedAt,
                        ),
                    )
                }
            var now =
                400L

            val first =
                VexLifeReleaseUpdates(
                    sourceSha =
                        installed.identity.sourceSha.value,
                    productHome = home,
                    observer = observer,
                    nowEpochMillis = {
                        now
                    },
                )
            val unread =
                first.projection()

            assertEquals(
                1,
                unread.unreadCount,
            )
            assertEquals(
                newer.identity.sourceSha.value.take(
                    7,
                ),
                unread.items.single().releaseIdentifier,
            )

            val view =
                first.control(
                    controlRef =
                        VexLifeReleaseInboxFamily.ControlView,
                    release =
                        unread.items.single().release,
                )

            assertEquals(
                VexLifeReleaseUpdates.ControlOutcome.Realized,
                view.outcome,
            )
            assertNotNull(
                view.pageToOpenOrNull,
            )
            assertEquals(
                0,
                view.display.unreadCount,
            )

            now +=
                1
            val restarted =
                VexLifeReleaseUpdates(
                    sourceSha =
                        installed.identity.sourceSha.value,
                    productHome = home,
                    observer = observer,
                    nowEpochMillis = {
                        now
                    },
                )
            val afterRestart =
                restarted.projection()

            assertEquals(
                0,
                afterRestart.unreadCount,
            )
            assertEquals(
                VexLifeReleaseInboxFamily.KeyViewed,
                afterRestart.items.single().status.key,
            )
        } finally {
            home
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun failedReobservationKeepsAlreadyKnownUpdateWithoutClaimingRemoteCurrentness() {
        val home =
            Files.createTempDirectory(
                "vexlife-release-updates-stale-",
            )
                .toAbsolutePath()
        try {
            val installed =
                publishedPreview(
                    'c',
                    100,
                )
            val newer =
                publishedPreview(
                    'd',
                    200,
                )
            val snapshot =
                VexLifePublishedReleaseObservation.Snapshot(
                    releases =
                        listOf(
                            newer,
                            installed,
                        ),
                    observedAtEpochMillis = 300,
                )
            var calls =
                0
            var now =
                400L
            val runtime =
                VexLifeReleaseUpdates(
                    sourceSha =
                        installed.identity.sourceSha.value,
                    productHome = home,
                    observer =
                        VexLifeReleaseUpdates.Observer {
                            previous,
                            observedAt,
                            ->
                            calls +=
                                1
                            if (
                                calls ==
                                    1
                            ) {
                                VexLifePublishedReleaseObservation.State.Current(
                                    snapshot.copy(
                                        observedAtEpochMillis = observedAt,
                                    ),
                                )
                            } else {
                                VexLifePublishedReleaseObservation.State.Unavailable(
                                    attemptedAtEpochMillis = observedAt,
                                    priorSnapshot =
                                        (
                                            previous as
                                                VexLifePublishedReleaseObservation.State.Current
                                        )
                                            .snapshot,
                                )
                            }
                        },
                    nowEpochMillis = {
                        now
                    },
                )

            assertEquals(
                1,
                runtime.projection().unreadCount,
            )

            now +=
                VexLifePublishedReleaseRefreshPolicy.FreshnessWindowMillis
            val stale =
                runtime.projection(
                    VexLifePublishedReleaseRefreshPolicy.Trigger.FreshnessBoundaryReached,
                )

            assertEquals(
                1,
                stale.unreadCount,
            )
        } finally {
            home
                .toFile()
                .deleteRecursively()
        }
    }
}

// [VXG RealForever]
