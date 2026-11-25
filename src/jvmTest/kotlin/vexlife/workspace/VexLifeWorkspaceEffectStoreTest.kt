package vexlife.workspace

import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import vexlife.home.CanonicalVexLifeHomeFixture
import vexlife.sources.VexLifeLocalSourceStore

class VexLifeWorkspaceEffectStoreTest {
    private fun withHome(
        block: (
            home: Path,
            source: Path,
            localSources: VexLifeLocalSourceStore,
        ) -> Unit,
    ) {
        val parent = Files.createTempDirectory("vexlife-workspace-effects-").toRealPath()
        val person = Files.createDirectories(parent.resolve("person"))
        val home = person.resolve("VexLife-Prod")
        val source = Files.createDirectories(person.resolve("project"))
        try {
            CanonicalVexLifeHomeFixture.form(
                home = home,
                deviceSuffix = "workspace-effects",
                lineageSuffix = "workspace-effects",
            )
            val localSources =
                VexLifeLocalSourceStore(
                    productHome = home,
                    personHome = person,
                    now = { Instant.parse("2026-11-24T20:00:00Z") },
                )
            assertIs<VexLifeLocalSourceStore.Outcome.Realized>(localSources.add(source))
            block(home, source, localSources)
        } finally {
            parent.toFile().deleteRecursively()
        }
    }

    @Test
    fun strongerGrantBindsTheExactLocalSourceGeneration() =
        withHome { home, source, localSources ->
            var now = Instant.parse("2026-11-24T20:01:00Z")
            val effects =
                VexLifeWorkspaceEffectStore(
                    productHome = home,
                    localSources = localSources,
                    now = { now },
                )
            val sourceGrant = localSources.grants().single()

            assertIs<VexLifeWorkspaceEffectStore.Outcome.Realized>(
                effects.grant(sourceGrant.sourceRef)
            )
            val first = assertNotNull(effects.currentGrant(sourceGrant.sourceRef))
            assertEquals(sourceGrant.grantedAt, first.sourceGrantedAt)

            assertIs<VexLifeLocalSourceStore.Outcome.Realized>(
                localSources.remove(sourceGrant.sourceRef)
            )
            assertNull(effects.currentGrant(sourceGrant.sourceRef))

            // The same physical path produces the same sourceRef, but a new Local Source grant
            // generation. Old effect authority must stay stale.
            now = Instant.parse("2026-11-24T20:02:00Z")
            val newLocalSources =
                VexLifeLocalSourceStore(
                    productHome = home,
                    personHome = source.parent,
                    now = { now },
                )
            assertIs<VexLifeLocalSourceStore.Outcome.Realized>(newLocalSources.add(source))
            val rebound =
                VexLifeWorkspaceEffectStore(
                    productHome = home,
                    localSources = newLocalSources,
                    now = { Instant.parse("2026-11-24T20:03:00Z") },
                )
            assertNull(rebound.currentGrant(sourceGrant.sourceRef))
            assertIs<VexLifeWorkspaceEffectStore.Outcome.Realized>(
                rebound.grant(sourceGrant.sourceRef)
            )
            val second = assertNotNull(rebound.currentGrant(sourceGrant.sourceRef))
            assertEquals(newLocalSources.grants().single().grantedAt, second.sourceGrantedAt)
            kotlin.test.assertNotEquals(first.grantRef, second.grantRef)
        }

    @Test
    fun revocationDoesNotRemoveTheReadableLocalSource() =
        withHome { home, _, localSources ->
            val effects =
                VexLifeWorkspaceEffectStore(
                    productHome = home,
                    localSources = localSources,
                    now = { Instant.parse("2026-11-24T20:01:00Z") },
                )
            val sourceRef = localSources.grants().single().sourceRef
            assertIs<VexLifeWorkspaceEffectStore.Outcome.Realized>(effects.grant(sourceRef))
            assertIs<VexLifeWorkspaceEffectStore.Outcome.Realized>(effects.revoke(sourceRef))
            assertNull(effects.currentGrant(sourceRef))
            assertNotNull(localSources.root(sourceRef))
            assertEquals(1, localSources.grants().size)
        }
}

// [VXG RealForever]
