package vexlife.site

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import kotlinx.serialization.Serializable
import vexlife.contract.VexContractJson
import vexlife.model.heretic.HereticModelCatalog
import vexlife.model.heretic.runtime.HereticMacHostProfile

/**
 * What the public VexLife home page says about running VexLife, formed from the Product's own
 * owners at deploy time — never restated by hand in the page.
 *
 * Owns only the projection: offered models (from [HereticModelCatalog]) and the Mac the Product
 * admits (from [HereticMacHostProfile]). Releases are listed by the Pages workflow from GitHub
 * Releases; this owns none of that.
 */
object VexLifeSiteData {
    const val Schema = "vexlife-prod.site-data/v1"

    @Serializable
    data class Model(
        val handle: String,
        val label: String,
        val downloadBytes: Long,
        val recommendedMemoryBytes: Long,
        val standing: String,
        val startsByDefault: Boolean,
        val providerRepository: String,
        val providerRevision: String,
    )

    @Serializable data class Mac(val chip: String, val minimumMemoryBytes: Long)

    @Serializable
    data class Data(
        val schemaVersion: String,
        val sourceSha: String?,
        val mac: Mac,
        val models: List<Model>,
    )

    fun form(sourceSha: String?): Data =
        Data(
            schemaVersion = Schema,
            sourceSha = sourceSha?.takeIf { it.matches(Regex("[0-9a-f]{40}")) },
            mac =
                Mac(
                    chip = HereticMacHostProfile.RequiredChipModel,
                    minimumMemoryBytes = HereticMacHostProfile.MinimumMemoryBytes,
                ),
            models =
                HereticModelCatalog.Entries.map {
                    Model(
                        handle = it.handle,
                        label = it.humanLabel,
                        downloadBytes = it.totalBytes,
                        recommendedMemoryBytes = it.recommendedMemoryBytes,
                        standing = it.standing.name,
                        startsByDefault =
                            it == HereticModelCatalog.recommendedFor(it.recommendedMemoryBytes),
                        providerRepository = it.providerRepository,
                        providerRevision = it.providerRevision,
                    )
                },
        )

    fun write(directory: Path, sourceSha: String?) {
        Files.createDirectories(directory)
        Files.writeString(
            directory.resolve("models.json"),
            VexContractJson.encode(form(sourceSha)) + "\n",
            StandardCharsets.UTF_8,
        )
    }
}

/** `write <directory> [sourceSha]` */
fun main(args: Array<String>) {
    require(args.size >= 2 && args[0] == "write") { "usage: write <directory> [sourceSha]" }
    VexLifeSiteData.write(Path.of(args[1]), args.getOrNull(2))
}

// [VXG RealForever]
