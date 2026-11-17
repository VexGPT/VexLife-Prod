package vexlife.model.heretic.runtime

import java.nio.file.Path
import vexlife.contract.VexContractJson
import vexlife.contract.VexDurableContractFile
import vexlife.filesystem.VexOwnedPath
import vexlife.model.heretic.HereticModelCatalog
import vexlife.model.heretic.HereticRuntimeConfiguration

/** Durable per-model runtime configuration beneath the Product-owned runtime scope. */
class FileHereticRuntimeConfigurationStore(
    productHome: Path,
) {
    companion object {
        const val RelativePath = "runtime/vexlife-prod/heretic-runtime-configuration.json"
    }

    private val productHome =
        VexOwnedPath.requireRoot(
            productHome,
            "canonical VexLife Home",
        )

    fun read(): HereticRuntimeConfiguration.Value {
        val path = path()
        val text =
            VexDurableContractFile.readText(
                ownedRoot = productHome,
                path = path,
            )
                ?: return HereticRuntimeConfiguration.empty()
        require(VexContractJson.schemaVersion(text) == HereticRuntimeConfiguration.Schema) {
            "VexLife-Prod Heretic runtime configuration schema is not admitted"
        }
        return HereticRuntimeConfiguration.validate(
            VexContractJson.decode(text),
        )
    }

    fun effective(
        model: HereticModelCatalog.Entry,
    ): HereticRuntimeConfiguration.Effective =
        HereticRuntimeConfiguration.effective(
            value = read(),
            model = model,
        )

    fun setPromptCacheEntries(
        model: HereticModelCatalog.Entry,
        promptCacheEntries: Int,
    ): HereticRuntimeConfiguration.Effective {
        write(
            HereticRuntimeConfiguration.withPromptCacheEntries(
                value = read(),
                model = model,
                promptCacheEntries = promptCacheEntries,
            ),
        )
        return effective(model)
    }

    fun useRecommended(
        model: HereticModelCatalog.Entry,
    ): HereticRuntimeConfiguration.Effective {
        write(
            HereticRuntimeConfiguration.withRecommended(
                value = read(),
                model = model,
            ),
        )
        return effective(model)
    }

    private fun write(value: HereticRuntimeConfiguration.Value) {
        VexDurableContractFile.write(
            ownedRoot = productHome,
            path = path(),
            value = HereticRuntimeConfiguration.validate(value),
        )
    }

    private fun path(): Path =
        VexOwnedPath.resolveInside(
            productHome,
            RelativePath,
        )
}

// [VXG RealForever]
