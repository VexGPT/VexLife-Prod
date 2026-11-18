package vexlife.release

import java.nio.file.Path

/**
 * Runtime composition for release observation + installed identity + local acknowledgement.
 *
 * Semantic ownership stays with the common release owners. This object hosts the concrete
 * observation/re-observation and Home persistence needed by the local Product service.
 */
class VexLifeReleaseUpdates(
    sourceSha: String,
    productHome: Path,
    private val observer: Observer =
        ProductObserver,
    private val acknowledgements: VexLifeReleaseAcknowledgementStore =
        VexLifeReleaseAcknowledgementStore(
            productHome,
        ),
    private val nowEpochMillis: () -> Long = {
        System.currentTimeMillis()
    },
) {
    fun interface Observer {
        fun observe(
            previous: VexLifePublishedReleaseObservation.State,
            observedAtEpochMillis: Long,
        ): VexLifePublishedReleaseObservation.State
    }

    enum class ControlOutcome {
        Realized,
        NotLawfulNow,
    }

    data class ControlResult(
        val outcome: ControlOutcome,
        val display: VexLifeReleaseInboxFamily.DisplayState,
        val pageToOpenOrNull: VexLifeReleaseIdentity.HttpsPage?,
    )

    private val installedSourceSha =
        VexLifeReleaseIdentity.SourceSha(
            sourceSha,
        )

    private var observation: VexLifePublishedReleaseObservation.State =
        VexLifePublishedReleaseObservation.State.Unobserved

    @Synchronized
    fun projection(
        trigger: VexLifePublishedReleaseRefreshPolicy.Trigger =
            VexLifePublishedReleaseRefreshPolicy.Trigger.PresentationArrived,
    ): VexLifeReleaseInboxFamily.DisplayState {
        val now =
            nowEpochMillis()
        if (
            VexLifePublishedReleaseRefreshPolicy.shouldObserve(
                observation = observation,
                trigger = trigger,
                nowEpochMillis = now,
            )
        ) {
            observation =
                observer.observe(
                    observation,
                    now,
                )
        }

        return displayFromCurrentObservation()
    }

    @Synchronized
    fun control(
        controlRef: String,
        release: VexLifeReleaseIdentity.Coordinate,
    ): ControlResult {
        val state =
            familyStateFromCurrentObservation()
        val control =
            VexLifeReleaseInboxFamily.controlForRef(
                controlRef = controlRef,
                identity = release.value(),
            )
                ?: return notLawful(
                    state,
                )
        val act =
            VexLifeReleaseInboxFamily.resolve(
                currentState = state,
                control = control,
            )
                ?: return notLawful(
                    state,
                )

        val page =
            when (
                val effect =
                    act.effect
            ) {
                is VexLifeReleaseInboxFamily.Effect.ViewRelease -> {
                    acknowledgements.remember(
                        identity = effect.notice.identity,
                        standing =
                            VexLifeReleaseInboxFamily.AcknowledgementStanding.Viewed,
                    )
                    effect.notice.release.page
                }

                is VexLifeReleaseInboxFamily.Effect.DismissRelease -> {
                    acknowledgements.remember(
                        identity = effect.identity,
                        standing =
                            VexLifeReleaseInboxFamily.AcknowledgementStanding.Dismissed,
                    )
                    null
                }

                null ->
                    return notLawful(
                        state,
                    )
            }

        return ControlResult(
            outcome = ControlOutcome.Realized,
            display = displayFromCurrentObservation(),
            pageToOpenOrNull = page,
        )
    }

    private fun notLawful(
        state: VexLifeReleaseInboxFamily.State,
    ): ControlResult =
        ControlResult(
            outcome = ControlOutcome.NotLawfulNow,
            display =
                VexLifeReleaseInboxFamily.displayStateFor(
                    state,
                ),
            pageToOpenOrNull = null,
        )

    private fun displayFromCurrentObservation(): VexLifeReleaseInboxFamily.DisplayState =
        VexLifeReleaseInboxFamily.displayStateFor(
            familyStateFromCurrentObservation(),
        )

    private fun familyStateFromCurrentObservation(): VexLifeReleaseInboxFamily.State {
        val snapshot =
            when (
                val current =
                    observation
            ) {
                is VexLifePublishedReleaseObservation.State.Current ->
                    current.snapshot

                is VexLifePublishedReleaseObservation.State.Unavailable ->
                    current.priorSnapshot

                VexLifePublishedReleaseObservation.State.Unobserved ->
                    null
            }
                ?: return VexLifeReleaseInboxFamily.observe(
                    notices = emptyList(),
                    acknowledgements = acknowledgements.read(),
                    sourceCurrentness =
                        VexLifeReleaseInboxFamily.SourceCurrentness.Unavailable,
                )

        val installed =
            VexLifeInstalledReleaseIdentity.resolve(
                sourceSha = installedSourceSha,
                snapshot = snapshot,
            )
        if (
            installed !is
                VexLifeInstalledReleaseIdentity.State.Identified
        ) {
            return VexLifeReleaseInboxFamily.observe(
                notices = emptyList(),
                acknowledgements = acknowledgements.read(),
                sourceCurrentness =
                    VexLifeReleaseInboxFamily.SourceCurrentness.Unavailable,
            )
        }

        return when (
            val availability =
                VexLifePublishedReleaseAvailability.evaluate(
                    installed = installed.identity,
                    observation = observation,
                )
        ) {
            is VexLifePublishedReleaseAvailability.State.Available ->
                VexLifeReleaseInboxFamily.observe(
                    notices =
                        listOf(
                            VexLifeReleaseInboxFamily.Notice(
                                availability.published,
                            ),
                        ),
                    acknowledgements = acknowledgements.read(),
                    sourceCurrentness =
                        when (
                            availability.currentness
                        ) {
                            VexLifePublishedReleaseAvailability.Currentness.Current ->
                                VexLifeReleaseInboxFamily.SourceCurrentness.Current

                            VexLifePublishedReleaseAvailability.Currentness.StaleKnownUpdate ->
                                VexLifeReleaseInboxFamily.SourceCurrentness.Unavailable
                        },
                )

            is VexLifePublishedReleaseAvailability.State.Current ->
                VexLifeReleaseInboxFamily.observe(
                    notices = emptyList(),
                    acknowledgements = acknowledgements.read(),
                    sourceCurrentness =
                        VexLifeReleaseInboxFamily.SourceCurrentness.Current,
                )

            is VexLifePublishedReleaseAvailability.State.Unknown ->
                VexLifeReleaseInboxFamily.observe(
                    notices = emptyList(),
                    acknowledgements = acknowledgements.read(),
                    sourceCurrentness =
                        VexLifeReleaseInboxFamily.SourceCurrentness.Unavailable,
                )
        }
    }

    companion object {
        private val ProductProvider =
            VexLifeGitHubReleaseProvider()

        private val ProductObserver =
            Observer {
                previous,
                observedAtEpochMillis,
                ->
                ProductProvider.observe(
                    previous = previous,
                    observedAtEpochMillis = observedAtEpochMillis,
                )
            }
    }
}

// [VXG RealForever]
