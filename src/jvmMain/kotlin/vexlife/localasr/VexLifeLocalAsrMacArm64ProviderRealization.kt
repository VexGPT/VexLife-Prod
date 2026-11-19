package vexlife.localasr

import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import vexlife.model.heretic.HereticRuntimeRealization

/** macOS / Apple-silicon whisper.cpp provider mechanics. */
internal object VexLifeLocalAsrMacArm64ProviderRealization :
    VexLifeLocalAsrProviderRealization {

    override val realizationRef: String =
        "realization.vexlife-prod.local-asr.whispercpp.macos-arm64.v1.9.5"

    override val target =
        HereticRuntimeRealization.MacArm64

    override val providerRootRelativePath: String =
        VexLifeLocalAsrWhisperCppProcessRealization.ProviderBaseRootRelativePath

    override val executableRelativePath: String =
        "bin/whisper-server"

    override val sourceIdentityRelativePath: String =
        "provider-source.txt"

    override val acquisition:
        VexLifeLocalAsrWhisperCppProcessRealization.ArchiveAcquisition? =
        null

    override fun configureProcess(
        processBuilder: ProcessBuilder,
        executable: Path,
    ) {
        val providerBin =
            requireNotNull(
                executable.parent,
            )
                .toAbsolutePath()
                .normalize()
        processBuilder.directory(
            providerBin.toFile(),
        )
        processBuilder
            .environment()["DYLD_LIBRARY_PATH"] =
            providerBin.toString()
    }

    override fun executableCurrent(
        executable: Path,
    ): Boolean =
        Files.isRegularFile(
            executable,
            LinkOption.NOFOLLOW_LINKS,
        ) &&
            !Files.isSymbolicLink(
                executable,
            ) &&
            Files.isExecutable(
                executable,
            )
}

// [VXG RealForever]
