package vexlife.person

import java.nio.file.Path
import vexlife.contract.VexDurableContractFile
import vexlife.filesystem.VexOwnedPath

/** The person's settings file in their Vex Home; absent means the defaults. */
class VexLifePersonSettingsStore(private val productHome: Path) {
    fun read(): VexLifePersonSettings.Value =
        VexDurableContractFile.read<VexLifePersonSettings.Value>(ownedRoot = home(), path = file())
            ?: VexLifePersonSettings.Default

    fun write(value: VexLifePersonSettings.Value) {
        VexDurableContractFile.write(ownedRoot = home(), path = file(), value = value)
    }

    private fun home(): Path = VexOwnedPath.requireRoot(productHome, "canonical VexLife Home")

    private fun file(): Path = VexOwnedPath.resolveInside(home(), RelativePath)

    companion object {
        const val RelativePath = "runtime/vexlife-prod/settings/person.json"
    }
}

// [VXG RealForever]
