package vexlife.platform.windows

import vexlife.platform.VexLifePlatformRealization

/**
 * Windows host observation for selecting one exact VexLife Product target.
 *
 * This adapter observes host/process architecture only. It does not claim that a matching Product
 * realization exists, choose a model/runtime, download an artifact, or grant any effect authority.
 *
 * On Windows, PROCESSOR_ARCHITEW6432 is preferred when present because it identifies the native
 * 64-bit OS architecture while a 32-bit or emulated process may report a different process
 * architecture.
 */
object VexLifeWindowsHostObservation {
    enum class ArchitectureEvidence {
        PROCESSOR_ARCHITEW6432,
        PROCESSOR_ARCHITECTURE,
        JVM_OS_ARCH,
    }

    data class Raw(
        val osName: String,
        val jvmOsArch: String?,
        val processorArchitecture: String?,
        val processorArchitectureW6432: String?,
    )

    sealed interface Result {
        val raw: Raw

        data class Resolved(
            override val raw: Raw,
            val target: VexLifePlatformRealization.Target,
            val nativeArchitecture: VexLifePlatformRealization.Architecture,
            val processArchitecture: VexLifePlatformRealization.Architecture?,
            val architectureEvidence: ArchitectureEvidence,
        ) : Result

        data class UnsupportedHost(
            override val raw: Raw,
        ) : Result

        data class UnsupportedArchitecture(
            override val raw: Raw,
        ) : Result
    }

    fun current(): Result =
        resolve(
            Raw(
                osName = System.getProperty("os.name").orEmpty(),
                jvmOsArch = System.getProperty("os.arch"),
                processorArchitecture = System.getenv("PROCESSOR_ARCHITECTURE"),
                processorArchitectureW6432 = System.getenv("PROCESSOR_ARCHITEW6432"),
            ),
        )

    fun resolve(raw: Raw): Result {
        if (!raw.osName.contains("windows", ignoreCase = true)) {
            return Result.UnsupportedHost(raw)
        }

        val nativeCandidates =
            listOf(
                ArchitectureEvidence.PROCESSOR_ARCHITEW6432 to
                    raw.processorArchitectureW6432,
                ArchitectureEvidence.PROCESSOR_ARCHITECTURE to
                    raw.processorArchitecture,
                ArchitectureEvidence.JVM_OS_ARCH to raw.jvmOsArch,
            )

        val native =
            nativeCandidates
                .firstNotNullOfOrNull { (evidence, value) ->
                    normalizeArchitecture(value)?.let { architecture ->
                        evidence to architecture
                    }
                }
                ?: return Result.UnsupportedArchitecture(raw)

        val processArchitecture = normalizeArchitecture(raw.jvmOsArch)

        return Result.Resolved(
            raw = raw,
            target =
                VexLifePlatformRealization.Target(
                    platform = VexLifePlatformRealization.PlatformFamily.WINDOWS,
                    architecture = native.second,
                ),
            nativeArchitecture = native.second,
            processArchitecture = processArchitecture,
            architectureEvidence = native.first,
        )
    }

    private fun normalizeArchitecture(value: String?): VexLifePlatformRealization.Architecture? =
        when (value?.trim()?.lowercase()) {
            "amd64", "x64", "x86_64", "x86-64" ->
                VexLifePlatformRealization.Architecture.X86_64
            "arm64", "aarch64" -> VexLifePlatformRealization.Architecture.ARM64
            else -> null
        }
}

// [VXG RealForever]
