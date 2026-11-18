package vexlife.site

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import vexlife.model.heretic.HereticModelCatalog

/** The home page's requirements come from the Product's owners, not from page copy. */
class VexLifeSiteDataTest {
    @Test
    fun everyOfferedModelAndTheAdmittedMacAreProjectedAsTheProductKnowsThem() {
        val data = VexLifeSiteData.form("a".repeat(40))
        assertEquals(HereticModelCatalog.Entries.map { it.handle }, data.models.map { it.handle })
        for ((model, entry) in data.models.zip(HereticModelCatalog.Entries)) {
            assertEquals(entry.totalBytes, model.downloadBytes)
            assertEquals(entry.recommendedMemoryBytes, model.recommendedMemoryBytes)
            assertEquals(entry.standing.name, model.standing)
        }
        assertEquals(
            HereticModelCatalog.Entries.minOf { requireNotNull(it.recommendedMemoryBytes) },
            data.mac.minimumMemoryBytes,
        )
        assertNull(VexLifeSiteData.form("not-a-sha").sourceSha)
    }

    @Test
    fun theDeployStepWritesOneModelsFile() {
        val directory = Files.createTempDirectory("vexlife-site-data-")
        try {
            VexLifeSiteData.write(directory, "b".repeat(40))
            val written =
                vexlife.contract.VexContractJson.decode<VexLifeSiteData.Data>(
                    Files.readString(directory.resolve("models.json"))
                )
            assertEquals(VexLifeSiteData.form("b".repeat(40)), written)
        } finally {
            directory.toFile().deleteRecursively()
        }
    }
}
