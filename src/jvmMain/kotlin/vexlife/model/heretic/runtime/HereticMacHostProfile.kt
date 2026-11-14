package vexlife.model.heretic.runtime

import vexlife.model.heretic.HereticProductIdentity

/**
 * Fresh Prod-owned host execution profile for the local Heretic runtime.
 *
 * Eligibility: darwin/arm64 on Apple silicon (any M-series chip, which MLX runs on) with at least
 * the memory the smallest offered catalog model wants.
 *
 * History: until 2026-11-14 this profile required one exact chip (Apple M4 Pro) and 12 GiB, the
 * ported certification envelope of the 27B on Victor's Mac. With a model catalog whose smallest
 * model needs 8 GiB, that pin became a stale guard that kept ordinary Macs out entirely. Whether a
 * particular model fits this Mac is now answered per model ([HereticModelCatalog.Entry.recommendedMemoryBytes]),
 * not by refusing the Mac. The profile ref is unchanged so existing runtime receipts stay exact.
 *
 * Bootstrap Python is intentionally not part of host hardware eligibility; it is owned by
 * [HereticPythonEnvironment].
 */
object HereticMacHostProfile {
    const val Ref: String =
        HereticProductIdentity.HostExecutionProfileRef

    const val RequiredPlatform =
        "darwin"

    const val RequiredArchitecture =
        "arm64"

    /** Shown to the person as what this Mac needs; evaluated as an "Apple M…" chip name. */
    const val RequiredChipModel =
        "Apple silicon (M1 or later)"

    private const val AppleSiliconChipPrefix =
        "Apple M"

    /** What the smallest offered model wants; a larger model's own need is judged per model. */
    val MinimumMemoryBytes: Long =
        vexlife.model.heretic.HereticModelCatalog.Entries.minOf { it.recommendedMemoryBytes }

    data class Observation(
        val platform: String,
        val architecture: String,
        val chipModel: String?,
        val totalMemoryBytes: Long?,
    )

    enum class State {
        Eligible,
        Unsupported,
    }

    data class Result(
        val profileRef: String,
        val state: State,
        val reason: String?,
        val observation: Observation,
        val effectsPerformed: Boolean,
    )

    fun evaluate(
        observation: Observation,
    ): Result {
        val reason =
            when {
                observation.platform !=
                    RequiredPlatform ->
                    "PLATFORM_UNSUPPORTED"

                observation.architecture !=
                    RequiredArchitecture ->
                    "ARCHITECTURE_UNSUPPORTED"

                observation.chipModel?.startsWith(AppleSiliconChipPrefix) !=
                    true ->
                    "CHIP_MODEL_UNSUPPORTED"

                observation.totalMemoryBytes ==
                    null ->
                    "SYSTEM_MEMORY_UNKNOWN"

                observation.totalMemoryBytes <
                    MinimumMemoryBytes ->
                    "INSUFFICIENT_SYSTEM_MEMORY"

                else ->
                    null
            }

        return Result(
            profileRef = Ref,
            state =
                if (
                    reason ==
                        null
                ) {
                    State.Eligible
                } else {
                    State.Unsupported
                },
            reason = reason,
            observation = observation,
            effectsPerformed = false,
        )
    }

    fun observeCurrent(): Result =
        evaluate(
            Observation(
                platform =
                    currentPlatform(),
                architecture =
                    currentArchitecture(),
                chipModel =
                    sysctl(
                        "machdep.cpu.brand_string",
                    ),
                totalMemoryBytes =
                    sysctl(
                        "hw.memsize",
                    )
                        ?.toLongOrNull(),
            ),
        )

    private fun currentPlatform(): String =
        when (
            System.getProperty(
                "os.name",
            )
                ?.lowercase()
        ) {
            "mac os x",
            "macos",
            "darwin",
            ->
                "darwin"

            else ->
                System
                    .getProperty(
                        "os.name",
                    )
                    ?.lowercase()
                    ?: "unknown"
        }

    private fun currentArchitecture(): String =
        when (
            System.getProperty(
                "os.arch",
            )
                ?.lowercase()
        ) {
            "aarch64",
            "arm64",
            ->
                "arm64"

            else ->
                System
                    .getProperty(
                        "os.arch",
                    )
                    ?.lowercase()
                    ?: "unknown"
        }

    private fun sysctl(
        key: String,
    ): String? =
        try {
            val process =
                ProcessBuilder(
                    "/usr/sbin/sysctl",
                    "-n",
                    key,
                )
                    .start()
            val stdout =
                process.inputStream
                    .bufferedReader()
                    .readText()
                    .trim()
            process.errorStream
                .bufferedReader()
                .readText()

            if (
                process.waitFor() ==
                    0 &&
                    stdout.isNotBlank()
            ) {
                stdout
            } else {
                null
            }
        } catch (
            _: Exception,
        ) {
            null
        }
}

// [VXG RealForever]
