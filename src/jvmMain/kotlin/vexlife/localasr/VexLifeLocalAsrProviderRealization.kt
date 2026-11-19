package vexlife.localasr

import vexlife.model.heretic.HereticRuntimeRealization
import vexlife.platform.VexLifeJvmPlatformTarget
import vexlife.platform.VexLifePlatformRealization

/**
 * Exact native whisper.cpp provider realization for the current Product platform.
 *
 * Local-ASR selection owns which recognizer/model the person chose. This owner answers only the
 * concrete provider-image question for that selection. The same semantic whisper.cpp provider may
 * therefore retain different package/process mechanics on macOS and Windows without leaking those
 * mechanics into VoiceInput or VexWeb.
 */
internal object VexLifeLocalAsrProviderRealization {
    const val ProviderSourceSha =
        "d1be6fde11ac6e0407606b4e42fe72d34add8037"
    const val ProviderVersion =
        "v1.9.5"
    const val ProviderBaseRootRelativePath =
        "runtime/vexlife-prod/local-asr/whispercpp/v1.9.5"

    data class WindowsArchive(
        val releaseTag: String,
        val fileName: String,
        val url: String,
        val bytes: Long,
        val sha256: String,
        val extractedRootRelativePath: String,
    )

    data class Value(
        val realizationRef: String,
        val target: VexLifePlatformRealization.Target,
        val providerRootRelativePath: String,
        val executableRelativePath: String,
        val sourceIdentityRelativePath: String,
        val libraryPathEnvironmentVariable: String?,
        val windowsArchive: WindowsArchive?,
    ) {
        val acquisitionSupported: Boolean
            get() =
                windowsArchive !=
                    null
    }

    val MacArm64 =
        Value(
            realizationRef =
                "realization.vexlife-prod.local-asr.whispercpp.macos-arm64.v1.9.5",
            target =
                HereticRuntimeRealization.MacArm64,
            providerRootRelativePath =
                ProviderBaseRootRelativePath,
            executableRelativePath =
                "bin/whisper-server",
            sourceIdentityRelativePath =
                "provider-source.txt",
            libraryPathEnvironmentVariable =
                "DYLD_LIBRARY_PATH",
            windowsArchive =
                null,
        )

    val WindowsX64 =
        Value(
            realizationRef =
                "realization.vexlife-prod.local-asr.whispercpp.windows-x64.cpu.b5454.v1.9.5",
            target =
                HereticRuntimeRealization.WindowsX64,
            providerRootRelativePath =
                ProviderBaseRootRelativePath +
                    "/providers/windows-x64-cpu-b5454",
            executableRelativePath =
                "image/Release/whisper-server.exe",
            sourceIdentityRelativePath =
                "provider-source.txt",
            libraryPathEnvironmentVariable =
                null,
            windowsArchive =
                WindowsArchive(
                    releaseTag =
                        "b5454",
                    fileName =
                        "whisper-bin-x64.zip",
                    url =
                        "https://github.com/ggml-org/whisper.cpp/releases/download/b5454/whisper-bin-x64.zip",
                    bytes =
                        8_928_640L,
                    sha256 =
                        "6ba69e3482d7826214f90a6a9c84ca07782aec1e1d0c6a7c30c994fd5d816ccb",
                    extractedRootRelativePath =
                        "Release",
                ),
        )

    fun current(): Value =
        when (
            val target =
                VexLifeJvmPlatformTarget.requireCurrent()
        ) {
            HereticRuntimeRealization.MacArm64 ->
                MacArm64

            HereticRuntimeRealization.WindowsX64 ->
                WindowsX64

            else ->
                error(
                    "local ASR has no whisper.cpp provider realization for target " +
                        HereticRuntimeRealization.targetRef(
                            target,
                        ),
                )
        }
}

// [VXG RealForever]
