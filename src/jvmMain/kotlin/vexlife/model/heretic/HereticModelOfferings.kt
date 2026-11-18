package vexlife.model.heretic

import java.nio.file.Path
import vexlife.model.heretic.runtime.FileHereticModelImportStore

/**
 * Read-only current model choices for one Vex Home.
 *
 * Product source owns [HereticModelCatalog]. The Home owns its explicit imports. This projection
 * composes those truths without turning either source into a mutable global catalog.
 */
object HereticModelOfferings {
    fun entries(productHome: Path): List<HereticModelCatalog.Entry> {
        val builtIn = HereticModelCatalog.Entries
        val imported = FileHereticModelImportStore(productHome).entries()
        val all = builtIn + imported

        require(all.map { it.modelRef }.distinct().size == all.size) {
            "current model offerings contain duplicate model refs"
        }
        require(all.map { it.handle }.distinct().size == all.size) {
            "current model offerings contain duplicate handles"
        }
        require(all.map { it.modelRootRelativePath }.distinct().size == all.size) {
            "current model offerings contain duplicate model folders"
        }
        require(all.map { it.identity.artifactContentSetSha256 }.distinct().size == all.size) {
            "current model offerings contain duplicate content sets"
        }
        return all
    }

    fun entry(
        productHome: Path,
        modelRef: String,
    ): HereticModelCatalog.Entry? =
        entries(productHome).firstOrNull { it.modelRef == modelRef }

    fun requireEntry(
        productHome: Path,
        modelRef: String,
    ): HereticModelCatalog.Entry =
        requireNotNull(entry(productHome, modelRef)) {
            "model $modelRef is not offered by this Vex Home"
        }

    fun byHandle(
        productHome: Path,
        handle: String,
    ): HereticModelCatalog.Entry? =
        entries(productHome).firstOrNull { it.handle == handle }
}

// [VXG RealForever]
