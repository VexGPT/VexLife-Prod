package vexlife.person

import java.nio.file.Path
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import vexlife.contract.VexContractJson
import vexlife.contract.VexDurableContractFile
import vexlife.filesystem.VexOwnedPath

/** The person's settings file in their Vex Home; absent means the defaults. */
class VexLifePersonSettingsStore(private val productHome: Path) {
    fun read(): VexLifePersonSettings.Value = readOrNull() ?: VexLifePersonSettings.Default

    /**
     * The saved settings, or null when the person has not chosen any yet. A v1 file (before the
     * screen language had its own choice) reads as v2 following the reply language; it is
     * rewritten as v2 on the next write.
     */
    fun readOrNull(): VexLifePersonSettings.Value? {
        val text = VexDurableContractFile.readText(ownedRoot = home(), path = file()) ?: return null
        val schema = VexContractJson.Strict.parseToJsonElement(text).jsonObject["schemaVersion"]?.jsonPrimitive?.content
        return if (schema == VexLifePersonSettings.SchemaV1) {
            VexContractJson.decode<VexLifePersonSettings.ValueV1>(text).current()
        } else {
            VexContractJson.decode<VexLifePersonSettings.Value>(text)
        }
    }

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
