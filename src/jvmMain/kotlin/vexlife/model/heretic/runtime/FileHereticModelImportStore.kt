package vexlife.model.heretic.runtime

import java.nio.file.Path
import vexlife.contract.VexContractJson
import vexlife.contract.VexDurableContractFile
import vexlife.filesystem.VexOwnedPath
import vexlife.model.heretic.HereticModelCatalog
import vexlife.model.heretic.HereticModelImport

/**
 * Durable Home-owned list of model sources the person explicitly added.
 *
 * This store owns only that history/choice. Model bytes remain owned by
 * [HereticArtifactMaterialization], and deleting bytes never silently deletes this record.
 */
class FileHereticModelImportStore(
    productHome: Path,
) {
    companion object {
        const val RelativePath = "models/heretic/imports.json"
    }

    data class AddResult(
        val record: HereticModelImport.Record,
        val writePerformed: Boolean,
    )

    data class ForgetResult(
        val handle: String,
        val writePerformed: Boolean,
    )

    private val productHome =
        VexOwnedPath.requireRoot(
            productHome,
            "canonical VexLife Home",
        )

    fun read(): HereticModelImport.Value {
        val text =
            VexDurableContractFile.readText(
                ownedRoot = productHome,
                path = path(),
            )
                ?: return HereticModelImport.empty()
        require(VexContractJson.schemaVersion(text) == HereticModelImport.Schema) {
            "VexLife-Prod Heretic model-import schema is not admitted"
        }
        return HereticModelImport.validate(
            VexContractJson.decode(text),
        )
    }

    fun entries(): List<HereticModelCatalog.Entry> =
        read()
            .models
            .map { record ->
                HereticModelImport.entry(record).also { entry ->
                    // The stored member list must reproduce the identity it claims before it can
                    // participate in any existing artifact/runtime relationship.
                    HereticArtifactMaterialization.plan(entry)
                }
            }

    fun record(handle: String): HereticModelImport.Record? =
        read().models.singleOrNull { it.handle == handle }

    fun add(record: HereticModelImport.Record): AddResult {
        val candidate = HereticModelImport.validate(record)
        val candidateEntry =
            HereticModelImport.entry(candidate).also {
                HereticArtifactMaterialization.plan(it)
            }

        require(
            HereticModelCatalog.Entries.none {
                it.modelRef == candidateEntry.modelRef ||
                    it.handle == candidateEntry.handle ||
                    it.modelRootRelativePath == candidateEntry.modelRootRelativePath ||
                    it.identity.artifactContentSetSha256 ==
                        candidateEntry.identity.artifactContentSetSha256
            },
        ) {
            "imported model conflicts with one Product-catalog model"
        }

        val current = read()
        current.models.singleOrNull { it.importRef == candidate.importRef }?.let { existing ->
            require(existing == candidate) {
                "import ref already names different model facts"
            }
            return AddResult(
                record = existing,
                writePerformed = false,
            )
        }

        require(
            current.models.none {
                it.modelRef == candidate.modelRef ||
                    it.handle == candidate.handle ||
                    it.modelRootRelativePath == candidate.modelRootRelativePath ||
                    it.artifactContentSetSha256 == candidate.artifactContentSetSha256
            },
        ) {
            "imported model conflicts with one model already added to this Home"
        }

        val next =
            HereticModelImport.Value(
                models =
                    (current.models + candidate)
                        .sortedBy { it.modelRef },
            )
                .let(HereticModelImport::validate)
        write(next)
        return AddResult(
            record = candidate,
            writePerformed = true,
        )
    }

    /**
     * Forget removes the saved source from My Models only after its artifact bytes are absent.
     *
     * The Product intentionally keeps "Remove files" and "Forget model" as separate human effects.
     */
    fun forget(handle: String): ForgetResult {
        val current = read()
        val record =
            current.models.singleOrNull { it.handle == handle }
                ?: return ForgetResult(
                    handle = handle,
                    writePerformed = false,
                )
        val entry = HereticModelImport.entry(record)
        val presence =
            HereticArtifactMaterialization.observePresence(
                productHome = productHome,
                model = entry,
            )
        require(
            presence.state == HereticArtifactMaterialization.PresenceState.Absent,
        ) {
            "imported model files must be removed before the model can be forgotten"
        }

        write(
            HereticModelImport.Value(
                models =
                    current.models
                        .filterNot { it.importRef == record.importRef }
                        .sortedBy { it.modelRef },
            ),
        )
        return ForgetResult(
            handle = handle,
            writePerformed = true,
        )
    }

    private fun write(value: HereticModelImport.Value) {
        VexDurableContractFile.write(
            ownedRoot = productHome,
            path = path(),
            value = HereticModelImport.validate(value),
        )
    }

    private fun path(): Path =
        VexOwnedPath.resolveInside(
            productHome,
            RelativePath,
        )
}

// [VXG RealForever]
