package vexlife.model.heretic.runtime

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import vexlife.model.heretic.HereticModelCatalog

/** Each catalog model lives in its own Home folder and can be freed without touching the others. */
class HereticModelMaterializationTest {
    private fun withHome(block: (Path) -> Unit) {
        val home = Files.createTempDirectory("vexlife-models-")
        try {
            block(home)
        } finally {
            home.toFile().deleteRecursively()
        }
    }

    @Test
    fun everyModelsPlanReproducesItsPinnedContentSet() {
        for (entry in HereticModelCatalog.Entries) {
            val plan = HereticArtifactMaterialization.plan(entry)
            assertEquals(
                entry.identity.artifactContentSetSha256,
                plan.contentSetSha256,
                entry.handle,
            )
            assertEquals(entry.providerRevision, plan.providerRevision, entry.handle)
            assertEquals(entry.totalBytes, plan.totalBytes, entry.handle)
        }
    }

    @Test
    fun presenceIsObservedInTheChosenModelsOwnFolder() = withHome { home ->
        val nine = HereticModelCatalog.Candidate9B
        val folder = home.resolve(nine.modelRootRelativePath).resolve(nine.providerRevision)
        Files.createDirectories(folder)
        val small = nine.members.first { it.filename == "config.json" }
        Files.write(folder.resolve(small.filename), ByteArray(small.expectedBytes.toInt()))
        val presence = HereticArtifactMaterialization.observePresence(home, nine)
        assertEquals("PARTIAL_BY_SIZE", presence.state)
        assertEquals(small.expectedBytes, presence.presentBySizeBytes)
        assertEquals(
            "ABSENT",
            HereticArtifactMaterialization.observePresence(home, HereticModelCatalog.Candidate4B)
                .state,
        )
    }

    @Test
    fun removingAModelFreesOnlyItsOwnFiles() = withHome { home ->
        val four = HereticModelCatalog.Candidate4B
        val nine = HereticModelCatalog.Candidate9B
        val fourFolder = home.resolve(four.modelRootRelativePath).resolve(four.providerRevision)
        val nineFolder = home.resolve(nine.modelRootRelativePath).resolve(nine.providerRevision)
        Files.createDirectories(fourFolder)
        Files.createDirectories(nineFolder)
        Files.writeString(fourFolder.resolve("config.json"), "{}")
        Files.writeString(fourFolder.resolve("model.safetensors.partial"), "partial")
        Files.writeString(fourFolder.resolve("notes-the-person-left.txt"), "keep me")
        Files.writeString(nineFolder.resolve("config.json"), "{}")

        val removal = HereticArtifactMaterialization.remove(home, four)

        assertEquals("REMOVED", removal.state)
        assertEquals(1, removal.removedFiles)
        assertEquals(2L + "partial".length, removal.freedBytes)
        assertFalse(Files.exists(fourFolder.resolve("config.json")))
        assertTrue(
            Files.exists(fourFolder.resolve("notes-the-person-left.txt")),
            "a file the catalog does not own stays",
        )
        assertTrue(Files.exists(nineFolder.resolve("config.json")), "another model is untouched")
        assertEquals(
            "ALREADY_ABSENT",
            HereticArtifactMaterialization.remove(home, HereticModelCatalog.Certified27B).state,
        )
    }
}
