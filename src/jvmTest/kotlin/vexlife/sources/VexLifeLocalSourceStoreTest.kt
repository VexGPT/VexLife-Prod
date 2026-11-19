package vexlife.sources

import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import vexlife.sources.VexLifeLocalSourceStore.Outcome
import vexlife.sources.VexLifeLocalSources.Refusal

class VexLifeLocalSourceStoreTest {
    private fun withDirs(block: (vexHome: Path, person: Path) -> Unit) {
        val parent = Files.createTempDirectory("vexlife-local-sources-").toRealPath()
        val vexHome = Files.createDirectories(parent.resolve("person/VexLife"))
        try {
            block(vexHome, parent.resolve("person"))
        } finally {
            parent.toFile().deleteRecursively()
        }
    }

    private fun store(vexHome: Path, person: Path) =
        VexLifeLocalSourceStore(vexHome, personHome = person, now = { Instant.parse("2026-11-19T00:00:00Z") })

    @Test
    fun onlyAddAndRemoveChangeTheGrantedSetAndItPersists() = withDirs { vexHome, person ->
        val project = Files.createDirectories(person.resolve("Documents/Project"))
        val store = store(vexHome, person)
        assertEquals(emptyList(), store.grants())

        val added = assertIs<Outcome.Realized>(store.add(project.toString())).grants.single()
        assertEquals("Project", added.name)
        assertEquals(project.toString(), added.path)
        assertEquals(listOf(added), store(vexHome, person).grants(), "grants survive a new store")
        assertEquals(project, store.root(added.ref))

        assertEquals(Refusal.ALREADY_GRANTED, assertIs<Outcome.Refused>(store.add("$project/")).refusal)
        assertIs<Outcome.Realized>(store.remove(added.ref))
        assertEquals(emptyList(), store.grants())
        assertNull(store.root(added.ref))
        assertEquals(Refusal.NOT_GRANTED, assertIs<Outcome.Refused>(store.remove(added.ref)).refusal)
    }

    @Test
    fun foldersThatAreTooBroadLinkedOrOverlappingTheVexHomeAreRefused() = withDirs { vexHome, person ->
        val store = store(vexHome, person)
        fun refusal(path: String) = assertIs<Outcome.Refused>(store.add(path)).refusal
        val file = Files.writeString(person.resolve("a.txt"), "x")
        val target = Files.createDirectories(person.resolve("real"))
        val link = Files.createSymbolicLink(person.resolve("link"), target)

        assertEquals(Refusal.NOT_ABSOLUTE, refusal("Documents"))
        assertEquals(Refusal.NOT_A_FOLDER, refusal(file.toString()))
        assertEquals(Refusal.NOT_A_FOLDER, refusal(person.resolve("missing").toString()))
        assertEquals(Refusal.LINK, refusal(link.toString()))
        assertEquals(Refusal.TOO_BROAD, refusal("/"))
        assertEquals(Refusal.TOO_BROAD, refusal(person.toString()))
        assertEquals(Refusal.OVERLAPS_VEX_HOME, refusal(vexHome.toString()))
        assertEquals(Refusal.OVERLAPS_VEX_HOME, refusal(Files.createDirectories(vexHome.resolve("runtime")).toString()))
        assertEquals(emptyList(), store.grants())
    }

    @Test
    fun theGrantLimitHolds() = withDirs { vexHome, person ->
        val store = store(vexHome, person)
        repeat(VexLifeLocalSources.MaximumGrants) {
            assertIs<Outcome.Realized>(store.add(Files.createDirectories(person.resolve("f$it")).toString()))
        }
        val extra = Files.createDirectories(person.resolve("one-more")).toString()
        assertEquals(Refusal.LIMIT_REACHED, assertIs<Outcome.Refused>(store.add(extra)).refusal)
    }
}

// [VXG RealForever]
