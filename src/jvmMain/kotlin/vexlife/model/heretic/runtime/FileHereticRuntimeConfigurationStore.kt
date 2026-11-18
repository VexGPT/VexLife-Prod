package vexlife.model.heretic.runtime

import java.nio.file.Path
import vexlife.contract.VexContractJson
import vexlife.contract.VexDurableContractFile
import vexlife.filesystem.VexOwnedPath
import vexlife.model.heretic.HereticModelCatalog
import vexlife.model.heretic.HereticModelOfferings
import vexlife.model.heretic.HereticRuntimeConfiguration
import vexlife.model.heretic.HereticRuntimeRealization

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
        realization: HereticRuntimeRealization.Value,
    ): HereticRuntimeConfiguration.Applicability =
        HereticRuntimeConfiguration.resolve(
            value = read(),
            model = model,
            realization = realization,
        )

    fun requireEffective(
        model: HereticModelCatalog.Entry,
        realization: HereticRuntimeRealization.Value,
    ): HereticRuntimeConfiguration.Effective =
        when (
            val applicability =
                effective(
                    model = model,
                    realization = realization,
                )
        ) {
            is HereticRuntimeConfiguration.Applicability.Applicable ->
                applicability.effective

            is HereticRuntimeConfiguration.Applicability.NotApplicable ->
                error(
                    "runtime configuration is not applicable to realization " +
                        applicability.realizationRef,
                )
        }

    fun setPromptCacheEntries(
        model: HereticModelCatalog.Entry,
        realization: HereticRuntimeRealization.Value,
        promptCacheEntries: Int,
    ): HereticRuntimeConfiguration.Effective {
        requireEffective(
            model = model,
            realization = realization,
        )
        write(
            HereticRuntimeConfiguration.withPromptCacheEntries(
                value = read(),
                model = model,
                promptCacheEntries = promptCacheEntries,
                modelForRef = ::offering,
            ),
        )
        return requireEffective(
            model = model,
            realization = realization,
        )
    }

    /**
     * Persist today's recommendation as an explicit Home value.
     *
     * This is intentionally not a subscription to future Product recommendations.
     */
    fun useRecommended(
        model: HereticModelCatalog.Entry,
        realization: HereticRuntimeRealization.Value,
    ): HereticRuntimeConfiguration.Effective {
        requireEffective(
            model = model,
            realization = realization,
        )
        val recommendation =
            HereticRuntimeConfiguration.recommendation(
                model,
            )
        return setPromptCacheEntries(
            model = model,
            realization = realization,
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
