package vexlife.localasr

import java.nio.file.Path
import vexlife.contract.VexDurableContractFile
import vexlife.filesystem.VexOwnedPath

/** Home-backed Composer-output preference, separate from speech model/language and Vex reply language. */
class VexLifeLocalAsrComposerOutputStore(
    private val productHome: Path,
) {
    fun read(): VexLifeLocalAsrComposerOutput.Value =
        readOrNull()
            ?: VexLifeLocalAsrComposerOutput.Default

    fun readOrNull(): VexLifeLocalAsrComposerOutput.Value? =
        VexDurableContractFile.read<VexLifeLocalAsrComposerOutput.Value>(
            ownedRoot = home(),
            path = file(),
        )

    fun write(
        value: VexLifeLocalAsrComposerOutput.Value,
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
            "runtime/vexlife-prod/settings/local-asr-composer-output.json"
    }
}

// [VXG RealForever]
