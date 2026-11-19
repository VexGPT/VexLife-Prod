package vexlife.localasr

import java.nio.file.Path
import vexlife.platform.VexLifeJvmPlatformTarget
import vexlife.platform.VexLifePlatformRealization

/**
 * One concrete native realization of the semantic whisper.cpp Local-ASR provider.
 *
 * The interface owns only target-specific provider mechanics. VoiceInput meaning, Local-ASR
 * selection, Composer semantics, Conversation, logging and presentation remain outside this
 * relationship.
 *
 * Adding another platform should add another implementation and register it in
 * [VexLifeLocalAsrProviderRealizationResolver]; consumers must not learn a new platform branch.
 */
internal interface VexLifeLocalAsrProviderRealization {
    val realizationRef: String
    val target: VexLifePlatformRealization.Target
    val providerRootRelativePath: String
    val executableRelativePath: String
    val sourceIdentityRelativePath: String
    val acquisition: ArchiveAcquisition?

    val acquisitionSupported: Boolean
        get() =
            acquisition !=
                null

    /**
     * Apply only the host mechanics required to start this exact provider image.
     *
     * The caller already owns process lifetime and the selected model/runtime request.
     */
    fun configureProcess(
        processBuilder: ProcessBuilder,
        executable: Path,
    )

    /** Lightweight readiness predicate; consequential digest verification stays in acquisition. */
    fun executableCurrent(
        executable: Path,
    ): Boolean

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

/**
 * The one target -> provider-realization selection boundary.
 *
 * Exact target matching only. There is no nearest-platform or architecture fallback. A new platform
 * joins by contributing one implementation here; VoiceInput/Local-ASR consumers remain unchanged.
 */
internal object VexLifeLocalAsrProviderRealizationResolver {
    private val Realizations: List<VexLifeLocalAsrProviderRealization> =
        listOf(
            VexLifeLocalAsrMacArm64ProviderRealization,
            VexLifeLocalAsrWindowsX64ProviderRealization,
        )

    init {
        require(
            Realizations
                .map(
                    VexLifeLocalAsrProviderRealization::realizationRef,
                )
                .distinct()
                .size ==
                Realizations.size,
        ) {
            "local ASR provider realization refs must be unique"
        }
        require(
            Realizations
                .map(
                    VexLifeLocalAsrProviderRealization::target,
                )
                .distinct()
                .size ==
                Realizations.size,
        ) {
            "local ASR provider realization targets must be unique"
        }
    }

    fun resolve(
        target: VexLifePlatformRealization.Target,
    ): VexLifeLocalAsrProviderRealization? =
        Realizations.singleOrNull {
            it.target ==
                target
        }

    fun current(): VexLifeLocalAsrProviderRealization {
        val target =
            VexLifeJvmPlatformTarget.requireCurrent()
        return requireNotNull(
            resolve(
                target,
            ),
        ) {
            "local ASR has no whisper.cpp provider realization for target " +
                target.platform.name.lowercase() +
                "-" +
                target.architecture.name.lowercase()
        }
    }

    fun all(): List<VexLifeLocalAsrProviderRealization> =
        Realizations.toList()
}

// [VXG RealForever]
