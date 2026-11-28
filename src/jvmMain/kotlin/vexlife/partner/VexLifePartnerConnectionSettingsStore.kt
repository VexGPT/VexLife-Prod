package vexlife.partner

import java.nio.file.Path
import vexlife.contract.VexDurableContractFile
import vexlife.filesystem.VexOwnedPath

/**
 * Atomic Home-owned persistence for non-secret Partner connection coordinates.
 *
 * The protected host credential owner remains separate. This store cannot create, read, replace, or
 * delete provider secret bytes.
 */
class VexLifePartnerConnectionSettingsStore(
    private val productHome: Path,
) {
    fun read(): VexLifePartnerConnectionSettings.Value =
        readOrNull()
            ?: VexLifePartnerConnectionSettings.Empty

    fun readOrNull(): VexLifePartnerConnectionSettings.Value? =
        VexDurableContractFile.read(
            ownedRoot = home(),
            path = file(),
        )

    fun write(
        value: VexLifePartnerConnectionSettings.Value,
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
            "runtime/vexlife-prod/settings/partner-connections.json"
    }
}

// [VXG RealForever]
