package vexlife.setup

import java.nio.file.Path
import kotlinx.serialization.Serializable
import vexlife.home.VexLifeProdHomeFiles
import vexlife.model.heretic.HereticModelCatalog
import vexlife.model.heretic.runtime.FileHereticModelConfigurationStore
import vexlife.text.VexLifeStrings
import vexlife.text.VexLifeText

/**
 * The person's model choice during setup.
 *
 * One selected-model authority: the Home's model configuration
 * (FileHereticModelConfigurationStore). Before a Home exists, the setup window holds the choice and
 * passes it to every inspection and activation; once the Home exists the choice is saved into it
 * (as HERETIC_ESTABLISHMENT_PENDING for that model), and Setup's ordinary Start/Set Up then
 * downloads and starts exactly that model.
 *
 * Resolution, most explicit first: the model chosen in this window, the model the Home chose, the
 * largest offered model that fits this Mac's memory. Choosing is not downloading and not starting.
 */
object VexLifeProdSetupModel {
    const val Schema = "vexlife-prod.setup-model/v1"

    @Serializable
    data class Model(
        val handle: String,
        val label: String,
        /** One line in the window's language: name, size, and how it fits this Mac. */
        val line: String,
        val totalBytes: Long,
        val fits: Boolean,
        val recommended: Boolean,
        val standing: String,
    )

    @Serializable
    data class Projection(
        val schemaVersion: String,
        val selected: String,
        val chosen: Boolean,
        val savedInHome: Boolean,
        val models: List<Model>,
        val effectsPerformed: Boolean,
    ) {
        init {
            require(
                schemaVersion == Schema &&
                    models.any { it.handle == selected } &&
                    (!savedInHome || chosen)
            ) {
                "setup model projection is invalid"
            }
        }
    }

    /** The model [productHome] chose, when it is a canonical Vex Home that chose one. */
    fun saved(productHome: Path): HereticModelCatalog.Entry? = runCatching {
        val location = VexLifeProdHomeFiles.observeLocation(productHome.normalize())
        if (location !is VexLifeProdHomeFiles.Location.Existing) return@runCatching null
        when (
            val snapshot =
                FileHereticModelConfigurationStore(productHome.normalize())
                    .read(location.home.manifest)
        ) {
            is FileHereticModelConfigurationStore.Snapshot.Heretic ->
                HereticModelCatalog.entry(snapshot.value.modelRef)
            is FileHereticModelConfigurationStore.Snapshot.Unconfigured -> null
        }
    }
        .getOrNull()

    fun resolve(
        productHome: Path,
        explicit: String?,
        totalMemoryBytes: Long?,
    ): HereticModelCatalog.Entry =
        explicit?.let(HereticModelCatalog::byHandle)
            ?: saved(productHome)
            ?: HereticModelCatalog.recommendedFor(totalMemoryBytes)

    /** No-effect: the offered models in [language], and which one setup will use. */
    fun observe(
        productHome: Path,
        explicit: String?,
        language: String,
        totalMemoryBytes: Long?,
    ): Projection {
        val saved = saved(productHome)
        val selected =
            explicit?.let(HereticModelCatalog::byHandle)
                ?: saved
                ?: HereticModelCatalog.recommendedFor(totalMemoryBytes)
        val recommended = HereticModelCatalog.recommendedFor(totalMemoryBytes)
        return Projection(
            schemaVersion = Schema,
            selected = selected.handle,
            chosen = explicit != null || saved != null,
            savedInHome = saved == selected,
            models =
                HereticModelCatalog.Entries.map { entry ->
                    val fits = HereticModelCatalog.fits(entry, totalMemoryBytes)
                    val fit =
                        when {
                            entry == recommended && totalMemoryBytes != null ->
                                VexLifeText("setup.status.model-recommended")
                            fits -> VexLifeText("setup.status.model-fits")
                            else ->
                                VexLifeText(
                                    key = "setup.status.model-needs-memory",
                                    gib = mapOf("memory" to entry.recommendedMemoryBytes),
                                )
                        }
                    Model(
                        handle = entry.handle,
                        label = entry.humanLabel,
                        line =
                            VexLifeStrings.resolve(
                                VexLifeText(
                                    key = "setup.label.model-choice",
                                    args = mapOf("model" to entry.humanLabel),
                                    gib = mapOf("size" to entry.totalBytes),
                                    parts = mapOf("fit" to fit),
                                ),
                                language,
                            ),
                        totalBytes = entry.totalBytes,
                        fits = fits,
                        recommended = entry == recommended,
                        standing = entry.standing.name,
                    )
                },
            effectsPerformed = false,
        )
    }

    /**
     * Saves [model] as the Home's choice when [productHome] is a canonical Vex Home. Returns
     * whether it is now saved there; before a Home exists nothing is written.
     */
    fun persist(productHome: Path, model: HereticModelCatalog.Entry): Boolean {
        val home = productHome.normalize()
        val location = VexLifeProdHomeFiles.observeLocation(home)
        if (location !is VexLifeProdHomeFiles.Location.Existing) return false
        FileHereticModelConfigurationStore(home).selectModel(location.home.manifest, model)
        return true
    }

    /** The person chose [handle] in the setup window: save it if the Home exists, then observe. */
    fun choose(
        productHome: Path,
        handle: String,
        language: String,
        totalMemoryBytes: Long?,
    ): Projection {
        val model =
            requireNotNull(HereticModelCatalog.byHandle(handle)) { "model $handle is not offered" }
        val persisted = persist(productHome, model)
        return observe(
                productHome,
                explicit = handle,
                language = language,
                totalMemoryBytes = totalMemoryBytes,
            )
            .copy(effectsPerformed = persisted)
    }
}

// [VXG RealForever]
