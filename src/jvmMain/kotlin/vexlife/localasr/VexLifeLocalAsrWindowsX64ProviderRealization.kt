package vexlife.localasr

import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import vexlife.model.heretic.HereticRuntimeRealization

/**
 * Windows / x64 CPU whisper.cpp provider mechanics.
 *
 * Lived Windows VoiceInput depends on this realization owning both of these relationships:
 *
 * - the exact pinned provider is materialized under Vex Home from the declared release archive;
 * - whisper-server.exe runs with its executable parent as cwd so the released DLL image resolves.
 *
 * These mechanics are intentionally different from Mac's bundled-provider + inherited-cwd contract.
 * Preserve the shared VoiceInput/Composer/Send semantics without moving either platform's substrate
 * rules into shared Local-ASR code.
 */
internal object VexLifeLocalAsrWindowsX64ProviderRealization :
    VexLifeLocalAsrWhisperCppProcessRealization() {

    override val realizationRef: String =
        "realization.vexlife-prod.local-asr.whispercpp.windows-x64.cpu.b5454.v1.9.5"

    override val target =
        HereticRuntimeRealization.WindowsX64

    override val providerRootRelativePath: String =
        VexLifeLocalAsrWhisperCppProcessRealization.ProviderBaseRootRelativePath +
            "/providers/windows-x64-cpu-b5454"

    override val executableRelativePath: String =
        "image/Release/whisper-server.exe"

    override val sourceIdentityRelativePath: String =
        "provider-source.txt"

    override val acquisition =
        VexLifeLocalAsrWhisperCppProcessRealization.ArchiveAcquisition(
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
            extractedExecutableRelativePath =
                "Release/whisper-server.exe",
        )

    override fun configureProcess(
        processBuilder: ProcessBuilder,
        executable: Path,
    ) {
        processBuilder.directory(
            requireNotNull(
                executable.parent,
            )
                .toAbsolutePath()
                .normalize()
                .toFile(),
        )
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
            )
}

// [VXG RealForever]
