package vexlife.model.heretic.runtime

import vexlife.model.heretic.HereticProductIdentity

/**
 * Fresh Prod-owned host execution profile for the certified Heretic runtime.
 *
 * The eligibility meaning is faithfully ported from current VexLife's ordinary Heretic setup:
 * darwin/arm64, exact Apple M4 Pro, and at least 12 GiB physical memory.
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

    const val RequiredChipModel =
        "Apple M4 Pro"

    const val MinimumMemoryBytes =
        12L *
            1024L *
            1024L *
            1024L

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

                observation.chipModel !=
                    RequiredChipModel ->
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
