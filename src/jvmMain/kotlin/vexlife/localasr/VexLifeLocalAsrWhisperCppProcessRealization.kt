package vexlife.localasr

import java.nio.file.Path
import vexlife.artifact.DirectHttpsArtifactDelivery
import vexlife.platform.VexLifePlatformRealization

/**
 * Shared desktop/JVM whisper.cpp-process realization.
 *
 * Mac and Windows use this proven loopback-process substrate. Future platforms are not required to
 * extend it; they may implement [VexLifeLocalAsrProviderRealization] directly.
 */
internal abstract class VexLifeLocalAsrWhisperCppProcessRealization :
    VexLifeLocalAsrProviderRealization {

    abstract override val realizationRef: String
    abstract override val target: VexLifePlatformRealization.Target

    abstract val providerRootRelativePath: String
    abstract val executableRelativePath: String

    open val sourceIdentityRelativePath: String =
        "provider-source.txt"

    abstract val acquisition: ArchiveAcquisition?

    override val acquisitionSupported: Boolean
        get() =
            acquisition !=
                null

    abstract fun configureProcess(
        processBuilder: ProcessBuilder,
        executable: Path,
    )

    abstract fun executableCurrent(
        executable: Path,
    ): Boolean

    override fun observeProvider(
        productHome: Path,
    ): VexLifeLocalAsrProviderMaterialization.Observation =
        VexLifeLocalAsrProviderMaterialization.observe(
            productHome =
                productHome,
            realization =
                this,
        )

    override fun ensureProvider(
        productHome: Path,
        allowRepair: Boolean,
    ): VexLifeLocalAsrProviderMaterialization.Result =
        VexLifeLocalAsrProviderMaterialization.ensure(
            productHome =
                productHome,
            realization =
                this,
            allowRepair =
                allowRepair,
        )

    fun ensureProvider(
        productHome: Path,
        transport: DirectHttpsArtifactDelivery.Transport,
        allowRepair: Boolean = false,
    ): VexLifeLocalAsrProviderMaterialization.Result =
        VexLifeLocalAsrProviderMaterialization.ensure(
            productHome =
                productHome,
            realization =
                this,
            transport =
                transport,
            allowRepair =
                allowRepair,
        )

    override fun provider(
        productHome: Path,
        selection: VexLifeLocalAsrSelection.Value,
        runtime: VexLifeLocalAsrRuntime,
    ): VexLifeLocalAsrProvider =
        whisperCppServerProvider(
            productHome =
                productHome,
            selection =
                selection,
            runtime =
                runtime,
            realization =
                this,
        )

    data class ArchiveAcquisition(
        val releaseTag: String,
        val fileName: String,
        val url: String,
        val bytes: Long,
        val sha256: String,
        val extractedExecutableRelativePath: String,
    )

    companion object {
        const val ProviderSourceSha =
            "d1be6fde11ac6e0407606b4e42fe72d34add8037"
        const val ProviderVersion =
            "v1.9.5"
        const val ProviderBaseRootRelativePath =
            "runtime/vexlife-prod/local-asr/whispercpp/v1.9.5"
    }
}

// [VXG RealForever]
