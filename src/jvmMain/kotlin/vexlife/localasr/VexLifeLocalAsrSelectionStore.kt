package vexlife.localasr

import java.nio.file.Path
import vexlife.contract.VexDurableContractFile
import vexlife.filesystem.VexOwnedPath

/** Home-backed selection only; provider/model installation and currentness remain outside it. */
class VexLifeLocalAsrSelectionStore(
    private val productHome: Path,
) {
    fun read(): VexLifeLocalAsrSelection.Value =
        readOrNull()
            ?: VexLifeLocalAsrSelection.Default

    fun readOrNull(): VexLifeLocalAsrSelection.Value? =
        VexDurableContractFile.read<VexLifeLocalAsrSelection.Value>(
            ownedRoot = home(),
            path = file(),
        )

    fun write(value: VexLifeLocalAsrSelection.Value) {
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
            "runtime/vexlife-prod/settings/local-asr.json"
    }
}

// [VXG RealForever]
