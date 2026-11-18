package vexlife.release

/**
 * Relationship owner for one question:
 *
 * "Does the VexLife installed on this machine have a newer intentionally published release in its
 * own channel?"
 *
 * It does not fetch GitHub, install software, own Product-entry service convergence, or render a
 * notification. It composes already-owned installed identity with already-observed publication truth.
 */
object VexLifePublishedReleaseAvailability {
    enum class Currentness {
        Current,
        StaleKnownUpdate,
    }

    enum class UnknownReason {
        NeverObserved,
        PublishedTruthUnavailable,
        NoPublishedReleaseInChannel,
        PreviewLineageNotObserved,
        PreviewOrderAmbiguous,
        StableIdentityConflict,
        StableLineageBehindInstalled,
    }

    sealed interface State {
        val installed: VexLifeReleaseIdentity.Value

        data class Current(
            override val installed: VexLifeReleaseIdentity.Value,
            val observedAtEpochMillis: Long,
        ) : State

        data class Available(
            override val installed: VexLifeReleaseIdentity.Value,
            val published: VexLifeReleaseIdentity.Published,
            val currentness: Currentness,
            val observedAtEpochMillis: Long,
        ) : State

        data class Unknown(
            override val installed: VexLifeReleaseIdentity.Value,
            val reason: UnknownReason,
            val lastSuccessfulObservationAtEpochMillis: Long?,
        ) : State
    }

    fun evaluate(
        installed: VexLifeReleaseIdentity.Value,
        observation: VexLifePublishedReleaseObservation.State,
    ): State =
        when (observation) {
            VexLifePublishedReleaseObservation.State.Unobserved ->
                State.Unknown(
                    installed = installed,
                    reason = UnknownReason.NeverObserved,
                    lastSuccessfulObservationAtEpochMillis = null,
                )

            is VexLifePublishedReleaseObservation.State.Current ->
                evaluateCurrent(
                    installed = installed,
                    snapshot = observation.snapshot,
                    currentness = Currentness.Current,
                )

            is VexLifePublishedReleaseObservation.State.Unavailable -> {
                val prior = observation.priorSnapshot
                if (prior == null) {
                    State.Unknown(
                        installed = installed,
                        reason = UnknownReason.PublishedTruthUnavailable,
                        lastSuccessfulObservationAtEpochMillis = null,
                    )
                } else {
                    when (
                        val priorAvailability =
                            evaluateCurrent(
                                installed = installed,
                                snapshot = prior,
                                currentness = Currentness.StaleKnownUpdate,
                            )
                    ) {
                        is State.Available -> priorAvailability
                        is State.Current,
                        is State.Unknown,
                        ->
                            State.Unknown(
                                installed = installed,
                                reason = UnknownReason.PublishedTruthUnavailable,
                                lastSuccessfulObservationAtEpochMillis = prior.observedAtEpochMillis,
                            )
                    }
                }
            }
        }

    private fun evaluateCurrent(
        installed: VexLifeReleaseIdentity.Value,
        snapshot: VexLifePublishedReleaseObservation.Snapshot,
        currentness: Currentness,
    ): State =
        when (installed) {
            is VexLifeReleaseIdentity.Value.Stable ->
                evaluateStable(
                    installed = installed,
                    snapshot = snapshot,
                    currentness = currentness,
                )

            is VexLifeReleaseIdentity.Value.Preview ->
                evaluatePreview(
                    installed = installed,
                    snapshot = snapshot,
                    currentness = currentness,
                )
        }

    private fun evaluateStable(
        installed: VexLifeReleaseIdentity.Value.Stable,
        snapshot: VexLifePublishedReleaseObservation.Snapshot,
        currentness: Currentness,
    ): State {
        val latest =
            snapshot.releases
                .filter { it.identity is VexLifeReleaseIdentity.Value.Stable }
                .maxByOrNull {
                    (it.identity as VexLifeReleaseIdentity.Value.Stable).version
                }
                ?: return State.Unknown(
                    installed = installed,
                    reason = UnknownReason.NoPublishedReleaseInChannel,
                    lastSuccessfulObservationAtEpochMillis = snapshot.observedAtEpochMillis,
                )
        val latestStable =
            latest.identity as VexLifeReleaseIdentity.Value.Stable

        return when {
            latestStable.version > installed.version ->
                State.Available(
                    installed = installed,
                    published = latest,
                    currentness = currentness,
                    observedAtEpochMillis = snapshot.observedAtEpochMillis,
                )

            latestStable.version == installed.version &&
                latestStable.sourceSha != installed.sourceSha ->
                State.Unknown(
                    installed = installed,
                    reason = UnknownReason.StableIdentityConflict,
                    lastSuccessfulObservationAtEpochMillis = snapshot.observedAtEpochMillis,
                )

            latestStable.version < installed.version ->
                State.Unknown(
                    installed = installed,
                    reason = UnknownReason.StableLineageBehindInstalled,
                    lastSuccessfulObservationAtEpochMillis = snapshot.observedAtEpochMillis,
                )

            else ->
                State.Current(
                    installed = installed,
                    observedAtEpochMillis = snapshot.observedAtEpochMillis,
                )
        }
    }

    private fun evaluatePreview(
        installed: VexLifeReleaseIdentity.Value.Preview,
        snapshot: VexLifePublishedReleaseObservation.Snapshot,
        currentness: Currentness,
    ): State {
        val previews =
            snapshot.releases
                .filter { it.identity is VexLifeReleaseIdentity.Value.Preview }
        if (previews.isEmpty()) {
            return State.Unknown(
                installed = installed,
                reason = UnknownReason.NoPublishedReleaseInChannel,
                lastSuccessfulObservationAtEpochMillis = snapshot.observedAtEpochMillis,
            )
        }

        val installedPublished =
            previews.singleOrNull {
                (it.identity as VexLifeReleaseIdentity.Value.Preview).sourceSha == installed.sourceSha
            }
                ?: return State.Unknown(
                    installed = installed,
                    reason = UnknownReason.PreviewLineageNotObserved,
                    lastSuccessfulObservationAtEpochMillis = snapshot.observedAtEpochMillis,
                )
        val latestPublishedAt =
            previews.maxOf {
                it.publishedAtEpochMillis
            }
        val newest =
            previews.filter {
                it.publishedAtEpochMillis == latestPublishedAt
            }
        if (newest.size != 1) {
            return State.Unknown(
                installed = installed,
                reason = UnknownReason.PreviewOrderAmbiguous,
                lastSuccessfulObservationAtEpochMillis = snapshot.observedAtEpochMillis,
            )
        }

        val latest = newest.single()
        val latestPreview =
            latest.identity as VexLifeReleaseIdentity.Value.Preview
        return if (latestPreview.sourceSha == installedPublished.identity.sourceSha) {
            State.Current(
                installed = installed,
                observedAtEpochMillis = snapshot.observedAtEpochMillis,
            )
        } else {
            State.Available(
                installed = installed,
                published = latest,
                currentness = currentness,
                observedAtEpochMillis = snapshot.observedAtEpochMillis,
            )
        }
    }
}

// [VXG RealForever]
