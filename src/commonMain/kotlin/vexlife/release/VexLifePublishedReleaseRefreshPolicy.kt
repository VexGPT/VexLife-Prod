package vexlife.release

/**
 * Bounded re-observation policy for published release truth.
 *
 * Product/presentation events create opportunities to re-observe. They never decide update meaning.
 * This policy does not own timers, jobs, focus/online listeners, or network execution.
 */
object VexLifePublishedReleaseRefreshPolicy {
    const val FreshnessWindowMillis: Long =
        12L * 60L * 60L * 1000L

    const val UnavailableRetryWindowMillis: Long =
        15L * 60L * 1000L

    enum class Trigger {
        PresentationArrived,
        PresentationBecameVisible,
        NetworkBecameOnline,
        FreshnessBoundaryReached,
        HumanRequested,
    }

    fun shouldObserve(
        observation: VexLifePublishedReleaseObservation.State,
        trigger: Trigger,
        nowEpochMillis: Long,
    ): Boolean {
        require(nowEpochMillis >= 0) {
            "published-release refresh time must be non-negative"
        }

        if (trigger == Trigger.HumanRequested) {
            return true
        }

        return when (observation) {
            VexLifePublishedReleaseObservation.State.Unobserved -> true

            is VexLifePublishedReleaseObservation.State.Current ->
                elapsedAtLeast(
                    nowEpochMillis = nowEpochMillis,
                    sinceEpochMillis = observation.snapshot.observedAtEpochMillis,
                    windowMillis = FreshnessWindowMillis,
                )

            is VexLifePublishedReleaseObservation.State.Unavailable ->
                if (trigger == Trigger.NetworkBecameOnline) {
                    true
                } else {
                    elapsedAtLeast(
                        nowEpochMillis = nowEpochMillis,
                        sinceEpochMillis = observation.attemptedAtEpochMillis,
                        windowMillis = UnavailableRetryWindowMillis,
                    )
                }
        }
    }

    fun nextObservationDueAtEpochMillis(
        observation: VexLifePublishedReleaseObservation.State,
    ): Long? =
        when (observation) {
            VexLifePublishedReleaseObservation.State.Unobserved -> null

            is VexLifePublishedReleaseObservation.State.Current ->
                observation.snapshot.observedAtEpochMillis +
                    FreshnessWindowMillis

            is VexLifePublishedReleaseObservation.State.Unavailable -> null
        }

    private fun elapsedAtLeast(
        nowEpochMillis: Long,
        sinceEpochMillis: Long,
        windowMillis: Long,
    ): Boolean =
        nowEpochMillis >= sinceEpochMillis &&
            nowEpochMillis - sinceEpochMillis >= windowMillis
}

// [VXG RealForever]
