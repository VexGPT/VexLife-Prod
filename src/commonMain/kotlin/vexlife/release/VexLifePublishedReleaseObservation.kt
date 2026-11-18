package vexlife.release

/**
 * Observation boundary for intentionally published VexLife releases.
 *
 * GitHub/Pages representation is converted before this boundary. This type owns no network work and
 * does not decide whether a Product update exists.
 */
object VexLifePublishedReleaseObservation {
    data class Snapshot(
        val releases: List<VexLifeReleaseIdentity.Published>,
        val observedAtEpochMillis: Long,
    ) {
        init {
            require(observedAtEpochMillis >= 0) {
                "published-release observation requires a non-negative observation instant"
            }
        }
    }

    sealed interface State {
        data object Unobserved : State

        data class Current(
            val snapshot: Snapshot,
        ) : State

        data class Unavailable(
            val attemptedAtEpochMillis: Long,
            val priorSnapshot: Snapshot?,
        ) : State {
            init {
                require(attemptedAtEpochMillis >= 0) {
                    "unavailable published-release observation requires a non-negative attempt instant"
                }
                require(
                    priorSnapshot == null || attemptedAtEpochMillis >= priorSnapshot.observedAtEpochMillis,
                ) {
                    "published-release retry cannot precede its retained successful observation"
                }
            }
        }
    }
}

// [VXG RealForever]
