package vexlife.model.heretic.runtime

import java.nio.file.Path
import vexlife.contract.VexContractJson
import vexlife.contract.VexDurableContractFile
import vexlife.filesystem.VexOwnedPath
import vexlife.model.heretic.HereticModelCatalog
import vexlife.model.heretic.HereticModelOfferings
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
            value = VexContractJson.decode(text),
            modelForRef = ::offering,
        )
    }

    fun effective(
        model: HereticModelCatalog.Entry,
    ): HereticRuntimeConfiguration.Effective =
        HereticRuntimeConfiguration.effective(
            value = read(),
            model = model,
            modelForRef = ::offering,
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
                modelForRef = ::offering,
            ),
        )
        return effective(model)
    }

    /**
     * Persist today's recommendation as an explicit Home value.
     *
     * This is intentionally not a subscription to future Product recommendations.
     */
    fun useRecommended(
        model: HereticModelCatalog.Entry,
    ): HereticRuntimeConfiguration.Effective {
        val recommendation =
            HereticRuntimeConfiguration.recommendation(
                model,
            )
        return setPromptCacheEntries(
            model = model,
            promptCacheEntries = recommendation.promptCacheEntries,
        )
    }

    fun forgetModel(modelRef: String) {
        val current = read()
        if (current.models.none { it.modelRef == modelRef }) return
        write(
            current.copy(
                models =
                    current.models
                        .filterNot { it.modelRef == modelRef }
                        .sortedBy { it.modelRef },
            ),
        )
    }

    private fun write(value: HereticRuntimeConfiguration.Value) {
        VexDurableContractFile.write(
            ownedRoot = productHome,
            path = path(),
            value =
                HereticRuntimeConfiguration.validate(
                    value = value,
                    modelForRef = ::offering,
                ),
        )
    }

    private fun offering(modelRef: String): HereticModelCatalog.Entry? =
        HereticModelOfferings.entry(
            productHome,
            modelRef,
        )

    private fun path(): Path =
        VexOwnedPath.resolveInside(
            productHome,
            RelativePath,
        )
}

// [VXG RealForever]
