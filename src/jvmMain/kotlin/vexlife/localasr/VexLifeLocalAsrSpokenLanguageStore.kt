package vexlife.localasr

import java.nio.file.Path
import vexlife.contract.VexDurableContractFile
import vexlife.filesystem.VexOwnedPath

/** Home-backed microphone-language preference, separate from model selection and Vex reply language. */
class VexLifeLocalAsrSpokenLanguageStore(
    private val productHome: Path,
) {
    fun read(): VexLifeLocalAsrSpokenLanguage.Value =
        readOrNull()
            ?: VexLifeLocalAsrSpokenLanguage.Default

    fun readOrNull(): VexLifeLocalAsrSpokenLanguage.Value? =
        VexDurableContractFile.read<VexLifeLocalAsrSpokenLanguage.Value>(
            ownedRoot = home(),
            path = file(),
        )

    fun write(
        value: VexLifeLocalAsrSpokenLanguage.Value,
    ) {
        VexDurableContractFile.write(
            ownedRoot = home(),
            path = file(),
            value = value,
        )
    }

    private fun home(): Path =
        VexOwnedPath.requireRoot(
            productHome,
            "canonical VexLife Home",
        )

    private fun file(): Path =
        VexOwnedPath.resolveInside(
            home(),
            RelativePath,
        )

    companion object {
        const val RelativePath =
            "runtime/vexlife-prod/settings/local-asr-spoken-language.json"
    }
}

// [VXG RealForever]
