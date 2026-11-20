package vexlife.localasr

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import vexlife.model.heretic.HereticRuntimeRealization

/**
 * macOS / Apple-silicon whisper.cpp provider mechanics.
 *
 * Mac VoiceInput runs through the browser/Product-service circuit, while native provider bytes are
 * resolved from Vex Home. The application bundle may carry a source-bound provider as release
 * evidence, but downloaded-app quarantine must not make that nested executable the runtime owner.
 * Speech-model bytes and provider runtime custody therefore remain separate from presentation.
 */
internal object VexLifeLocalAsrMacArm64ProviderRealization :
    VexLifeLocalAsrWhisperCppProcessRealization() {

    const val PrivateServiceImageRootProperty =
        "vexlife.privateServiceImageRoot"

    const val BundledProviderRootRelativePath =
        "local-asr-provider/whispercpp/v1.9.5"

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

    override fun observeProvider(
        productHome: Path,
    ): VexLifeLocalAsrProviderMaterialization.Observation =
        super.observeProvider(
            productHome,
        )

    internal fun observeBundledProvider(
        privateServiceImageRoot: Path?,
    ): VexLifeLocalAsrProviderMaterialization.Observation {
        if (
            privateServiceImageRoot ==
                null
        ) {
            return VexLifeLocalAsrProviderMaterialization.Observation(
                realizationRef =
                    realizationRef,
                providerRootPath =
                    "",
                executablePath =
                    "",
                sourceIdentityPath =
                    "",
                sourceSha =
                    null,
                archivePath =
                    null,
                state =
                    VexLifeLocalAsrProviderMaterialization.State.ABSENT,
                effectsPerformed =
                    false,
            )
        }

        val imageRoot =
            privateServiceImageRoot
                .toAbsolutePath()
                .normalize()
        val providerRoot =
            imageRoot
                .resolve(
                    BundledProviderRootRelativePath,
                )
                .normalize()
        val executable =
            providerRoot
                .resolve(
                    executableRelativePath,
                )
                .normalize()
        val sourceIdentity =
            providerRoot
                .resolve(
                    sourceIdentityRelativePath,
                )
                .normalize()

        require(
            providerRoot.startsWith(
                imageRoot,
            ) &&
                executable.startsWith(
                    providerRoot,
                ) &&
                sourceIdentity.startsWith(
                    providerRoot,
                ),
        ) {
            "bundled Mac local ASR provider escaped the private service image"
        }

        val sourceSha =
            if (
                Files.isRegularFile(
                    sourceIdentity,
                    LinkOption.NOFOLLOW_LINKS,
                ) &&
                !Files.isSymbolicLink(
                    sourceIdentity,
                )
            ) {
                runCatching {
                        Files.readString(
                            sourceIdentity,
                            StandardCharsets.UTF_8,
                        )
                            .trim()
                    }
                    .getOrNull()
            } else {
                null
            }

        val executableExists =
            Files.exists(
                executable,
                LinkOption.NOFOLLOW_LINKS,
            )
        val sourceExists =
            Files.exists(
                sourceIdentity,
                LinkOption.NOFOLLOW_LINKS,
            )
        val sourceCurrent =
            sourceSha ==
                providerSourceRef
        val executableCurrent =
            executableCurrent(
                executable,
            )

        val state =
            when {
                sourceCurrent &&
                    executableCurrent ->
                    VexLifeLocalAsrProviderMaterialization.State.VERIFIED

                !sourceExists &&
                    !executableExists ->
                    VexLifeLocalAsrProviderMaterialization.State.ABSENT

                else ->
                    VexLifeLocalAsrProviderMaterialization.State.INVALID
            }

        return VexLifeLocalAsrProviderMaterialization.Observation(
            realizationRef =
                realizationRef,
            providerRootPath =
                providerRoot.toString(),
            executablePath =
                executable.toString(),
            sourceIdentityPath =
                sourceIdentity.toString(),
            sourceSha =
                sourceSha,
            archivePath =
                null,
            state =
                state,
            effectsPerformed =
                false,
        )
    }

    override fun ensureProvider(
        productHome: Path,
        allowRepair: Boolean,
    ): VexLifeLocalAsrProviderMaterialization.Result =
        super.ensureProvider(
            productHome = productHome,
            allowRepair = allowRepair,
        )

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

        // Preserve the lived-qualified Mac launch shape: the Product service owns the child cwd.
        // Mac only contributes its native-library lookup path. Windows owns a different cwd rule
        // inside its own realization because its released DLL image requires it.
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
