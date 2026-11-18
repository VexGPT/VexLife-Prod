package vexlife.release

/**
 * Resolves the locally known source SHA back to one intentionally published release identity.
 *
 * This is deliberately fail-closed: source alone can identify a release only when publication truth
 * has exactly one channel identity for that source. If Preview and Stable were both cut from the same
 * source, local application-version metadata is required before update comparison may continue.
 */
object VexLifeInstalledReleaseIdentity {
    enum class UnknownReason {
        SourceNotPublished,
        AmbiguousPublicationIdentity,
    }

    sealed interface State {
        data class Identified(
            val identity: VexLifeReleaseIdentity.Value,
        ) : State

        data class Unknown(
            val reason: UnknownReason,
        ) : State
    }

    fun resolve(
        sourceSha: VexLifeReleaseIdentity.SourceSha,
        snapshot: VexLifePublishedReleaseObservation.Snapshot,
    ): State {
        val matches =
            snapshot.releases
                .map(
                    VexLifeReleaseIdentity.Published::identity,
                )
                .filter {
                    it.sourceSha ==
                        sourceSha
                }
                .distinct()

        return when (matches.size) {
            0 ->
                State.Unknown(
                    UnknownReason.SourceNotPublished,
                )

            1 ->
                State.Identified(
                    matches.single(),
                )

            else ->
                State.Unknown(
                    UnknownReason.AmbiguousPublicationIdentity,
                )
        }
    }
}

// [VXG RealForever]
