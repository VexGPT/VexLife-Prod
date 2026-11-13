package vexlife.model.heretic.runtime

/**
 * Pre-acquisition free-space requirement for the certified Heretic product route on the volume that
 * holds the selected Vex Home.
 *
 * Question owned: "may model/runtime acquisition begin here without predictably running out of
 * space?" The answer is a no-effect comparison; it never reserves, deletes or moves anything.
 *
 * Model bytes are exact (source-owned catalog minus members already complete by size). The runtime
 * allowance covers the private managed Python, the mlx-lm environment, the uv cache, the installed
 * Product image and working headroom. That allowance is a deliberately conservative source estimate
 * and still needs lived measurement on a fresh Mac; it must not be presented as an exact figure.
 *
 * Unknown free space is not proof of insufficiency: the hold applies only to an observed shortfall.
 * Delivery still fails closed and resumes from verified partial bytes if the disk fills later.
 */
object HereticSetupStorage {
    const val RuntimeAllowanceBytes =
        3L *
            1024L *
            1024L *
            1024L

    data class Requirement(
        val missingModelBytes: Long,
        val requiredFreeBytes: Long,
        val observedFreeBytes: Long?,
    ) {
        val shortfallObserved: Boolean =
            observedFreeBytes !=
                null &&
                observedFreeBytes <
                requiredFreeBytes

        init {
            require(
                missingModelBytes >=
                    0 &&
                    requiredFreeBytes ==
                    missingModelBytes +
                    RuntimeAllowanceBytes,
            ) {
                "Heretic setup storage requirement is invalid"
            }
        }
    }

    fun requirement(
        missingModelBytes: Long,
        observedFreeBytes: Long?,
    ): Requirement {
        val missing =
            missingModelBytes.coerceAtLeast(
                0L,
            )
        return Requirement(
            missingModelBytes = missing,
            requiredFreeBytes =
                missing +
                    RuntimeAllowanceBytes,
            observedFreeBytes = observedFreeBytes,
        )
    }
}

// [VXG RealForever]
