package vexlife.release

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VexLifePublishedReleaseRefreshPolicyTest {
    private val emptySnapshot =
        VexLifePublishedReleaseObservation.Snapshot(
            releases = emptyList(),
            observedAtEpochMillis = 1_000,
        )

    @Test
    fun firstPresentationObservationIsAlwaysEarned() {
        assertTrue(
            VexLifePublishedReleaseRefreshPolicy.shouldObserve(
                observation = VexLifePublishedReleaseObservation.State.Unobserved,
                trigger =
                    VexLifePublishedReleaseRefreshPolicy.Trigger.PresentationArrived,
                nowEpochMillis = 1_000,
            ),
        )
    }

    @Test
    fun focusDoesNotTurnFreshPublishedTruthIntoPolling() {
        val current =
            VexLifePublishedReleaseObservation.State.Current(
                emptySnapshot,
            )

        assertFalse(
            VexLifePublishedReleaseRefreshPolicy.shouldObserve(
                observation = current,
                trigger =
                    VexLifePublishedReleaseRefreshPolicy.Trigger.PresentationBecameVisible,
                nowEpochMillis = 2_000,
            ),
        )

        assertTrue(
            VexLifePublishedReleaseRefreshPolicy.shouldObserve(
                observation = current,
                trigger =
                    VexLifePublishedReleaseRefreshPolicy.Trigger.PresentationBecameVisible,
                nowEpochMillis =
                    emptySnapshot.observedAtEpochMillis +
                        VexLifePublishedReleaseRefreshPolicy.FreshnessWindowMillis,
            ),
        )
    }

    @Test
    fun networkReturnCanRetryUnavailableTruthImmediately() {
        val unavailable =
            VexLifePublishedReleaseObservation.State.Unavailable(
                attemptedAtEpochMillis = 2_000,
                priorSnapshot = emptySnapshot,
            )

        assertFalse(
            VexLifePublishedReleaseRefreshPolicy.shouldObserve(
                observation = unavailable,
                trigger =
                    VexLifePublishedReleaseRefreshPolicy.Trigger.PresentationBecameVisible,
                nowEpochMillis = 2_001,
            ),
        )
        assertTrue(
            VexLifePublishedReleaseRefreshPolicy.shouldObserve(
                observation = unavailable,
                trigger =
                    VexLifePublishedReleaseRefreshPolicy.Trigger.NetworkBecameOnline,
                nowEpochMillis = 2_001,
            ),
        )
    }

    @Test
    fun unavailableTruthDoesNotScheduleAnOfflineRetryLoop() {
        val unavailable =
            VexLifePublishedReleaseObservation.State.Unavailable(
                attemptedAtEpochMillis = 2_000,
                priorSnapshot = emptySnapshot,
            )

        assertNull(
            VexLifePublishedReleaseRefreshPolicy.nextObservationDueAtEpochMillis(
                unavailable,
            ),
        )
    }

    @Test
    fun successfulTruthSchedulesOnlyItsNextFreshnessBoundary() {
        val current =
            VexLifePublishedReleaseObservation.State.Current(
                emptySnapshot,
            )

        assertEquals(
            emptySnapshot.observedAtEpochMillis +
                VexLifePublishedReleaseRefreshPolicy.FreshnessWindowMillis,
            VexLifePublishedReleaseRefreshPolicy.nextObservationDueAtEpochMillis(
                current,
            ),
        )
    }

    @Test
    fun humanCheckAlwaysRequestsFreshObservation() {
        assertTrue(
            VexLifePublishedReleaseRefreshPolicy.shouldObserve(
                observation =
                    VexLifePublishedReleaseObservation.State.Current(
                        emptySnapshot,
                    ),
                trigger = VexLifePublishedReleaseRefreshPolicy.Trigger.HumanRequested,
                nowEpochMillis = 1_001,
            ),
        )
    }
}

// [VXG RealForever]
