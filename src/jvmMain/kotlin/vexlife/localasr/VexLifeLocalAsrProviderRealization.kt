package vexlife.localasr

import java.nio.file.Path
import vexlife.platform.VexLifeJvmPlatformTarget
import vexlife.platform.VexLifePlatformRealization

/**
 * One concrete host realization of the semantic Local-ASR provider.
 *
 * This is deliberately substrate-neutral: a realization may use a desktop native process, Android
 * service/JNI, or another mechanism without changing VoiceInput, Local-ASR Family semantics,
 * Settings, Composer or Conversation.
 */
internal interface VexLifeLocalAsrProviderRealization {
    val realizationRef: String
    val target: VexLifePlatformRealization.Target
    val acquisitionSupported: Boolean

    fun observeProvider(
        productHome: Path,
    ): VexLifeLocalAsrProviderMaterialization.Observation

    fun ensureProvider(
        productHome: Path,
        allowRepair: Boolean = false,
    ): VexLifeLocalAsrProviderMaterialization.Result

    fun provider(
        productHome: Path,
        selection: VexLifeLocalAsrSelection.Value,
        runtime: VexLifeLocalAsrRuntime,
    ): VexLifeLocalAsrProvider
}

/**
 * The one exact target -> Local-ASR provider realization selection boundary.
 *
 * There is intentionally no fallback. Adding Android or another platform means registering one
 * real implementation after that target is truthfully observable; semantic consumers remain
 * unchanged.
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
            "local ASR has no provider realization for target " +
                target.platform.name.lowercase() +
                "-" +
                target.architecture.name.lowercase()
        }
    }

    fun all(): List<VexLifeLocalAsrProviderRealization> =
        Realizations.toList()
}

// [VXG RealForever]
