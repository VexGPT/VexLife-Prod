package vexlife.platform

import vexlife.platform.windows.VexLifeWindowsHostObservation

/**
 * JVM host target observation for Product construction.
 *
 * This owner answers only which exact platform/architecture target the current JVM host reports.
 * It does not decide model meaning, runtime support, resource fit, artifact effects or lifecycle.
 * Downstream consumers receive [VexLifePlatformRealization.Target] and therefore do not branch on
 * Mac versus Windows themselves.
 */
object VexLifeJvmPlatformTarget {
    sealed interface Result {
        data class Resolved(
            val target: VexLifePlatformRealization.Target,
        ) : Result

        data class Unsupported(
            val osName: String,
            val architecture: String?,
        ) : Result
    }

    fun current(): Result =
        current(
            osName = System.getProperty("os.name").orEmpty(),
            jvmOsArch = System.getProperty("os.arch"),
            windowsObservation = VexLifeWindowsHostObservation::current,
        )

    fun requireCurrent(): VexLifePlatformRealization.Target =
        when (
            val result =
                current()
        ) {
            is Result.Resolved ->
                result.target

            is Result.Unsupported ->
                error(
                    "current JVM host has no VexLife Product target: " +
                        result.osName +
                        "/" +
                        (result.architecture ?: "unknown"),
                )
        }

    internal fun current(
        osName: String,
        jvmOsArch: String?,
        windowsObservation: () -> VexLifeWindowsHostObservation.Result,
    ): Result {
        if (
            osName.contains(
                "windows",
                ignoreCase = true,
            )
        ) {
            return when (
                val windows =
                    windowsObservation()
            ) {
                is VexLifeWindowsHostObservation.Result.Resolved ->
                    Result.Resolved(
                        windows.target,
                    )

                is VexLifeWindowsHostObservation.Result.UnsupportedHost,
                is VexLifeWindowsHostObservation.Result.UnsupportedArchitecture,
                ->
                    Result.Unsupported(
                        osName = osName,
                        architecture = jvmOsArch,
                    )
            }
        }

        if (
            osName.equals(
                "Mac OS X",
                ignoreCase = true,
            ) ||
            osName.equals(
                "macOS",
                ignoreCase = true,
            ) ||
            osName.equals(
                "Darwin",
                ignoreCase = true,
            )
        ) {
            val architecture =
                when (
                    jvmOsArch
                        ?.trim()
                        ?.lowercase()
                ) {
                    "arm64",
                    "aarch64",
                    ->
                        VexLifePlatformRealization.Architecture.ARM64

                    "x86_64",
                    "amd64",
                    ->
                        VexLifePlatformRealization.Architecture.X86_64

                    else ->
                        null
                }
                    ?: return Result.Unsupported(
                        osName = osName,
                        architecture = jvmOsArch,
                    )

            return Result.Resolved(
                VexLifePlatformRealization.Target(
                    platform =
                        VexLifePlatformRealization.PlatformFamily.MACOS,
                    architecture =
                        architecture,
                ),
            )
        }

        return Result.Unsupported(
            osName = osName,
            architecture = jvmOsArch,
        )
    }
}

// [VXG RealForever]
