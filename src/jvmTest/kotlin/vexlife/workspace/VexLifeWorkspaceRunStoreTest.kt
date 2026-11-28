package vexlife.workspace

import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import vexlife.home.CanonicalVexLifeHomeFixture
import vexlife.sources.VexLifeLocalSourceStore

class VexLifeWorkspaceRunStoreTest {
    private fun withHome(
        block: (
            home: Path,
            source: Path,
            localSources: VexLifeLocalSourceStore,
        ) -> Unit,
    ) {
        val parent =
            Files.createTempDirectory(
                "vexlife-workspace-runs-",
            )
                .toRealPath()
        val person =
            Files.createDirectories(
                parent.resolve(
                    "person",
                ),
            )
        val home =
            person.resolve(
                "VexLife-Prod",
            )
        val source =
            Files.createDirectories(
                person.resolve(
                    "project",
                ),
            )

        try {
            CanonicalVexLifeHomeFixture.form(
                home =
                    home,
                deviceSuffix =
                    "workspace-runs",
                lineageSuffix =
                    "workspace-runs",
            )
            val localSources =
                VexLifeLocalSourceStore(
                    productHome =
                        home,
                    personHome =
                        person,
                    now = {
                        Instant.parse(
                            "2026-11-24T20:00:00Z",
                        )
                    },
                )
            assertIs<VexLifeLocalSourceStore.Outcome.Realized>(
                localSources.add(
                    source,
                ),
            )
            block(
                home,
                source,
                localSources,
            )
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun runGrantBindsTheExactLocalSourceGeneration() =
        withHome {
            home,
            source,
            localSources,
            ->
            var now =
                Instant.parse(
                    "2026-11-24T20:01:00Z",
                )
            val runs =
                VexLifeWorkspaceRunStore(
                    productHome =
                        home,
                    localSources =
                        localSources,
                    now = {
                        now
                    },
                )
            val sourceGrant =
                localSources
                    .grants()
                    .single()

            assertIs<VexLifeWorkspaceRunStore.Outcome.Realized>(
                runs.grant(
                    sourceGrant.sourceRef,
                ),
            )
            val first =
                assertNotNull(
                    runs.currentGrant(
                        sourceGrant.sourceRef,
                    ),
                )
            assertEquals(
                sourceGrant.grantedAt,
                first.sourceGrantedAt,
            )

            assertIs<VexLifeLocalSourceStore.Outcome.Realized>(
                localSources.remove(
                    sourceGrant.sourceRef,
                ),
            )
            assertNull(
                runs.currentGrant(
                    sourceGrant.sourceRef,
                ),
            )

            now =
                Instant.parse(
                    "2026-11-24T20:02:00Z",
                )
            val reboundSources =
                VexLifeLocalSourceStore(
                    productHome =
                        home,
                    personHome =
                        source.parent,
                    now = {
                        now
                    },
                )
            assertIs<VexLifeLocalSourceStore.Outcome.Realized>(
                reboundSources.add(
                    source,
                ),
            )
            val rebound =
                VexLifeWorkspaceRunStore(
                    productHome =
                        home,
                    localSources =
                        reboundSources,
                    now = {
                        Instant.parse(
                            "2026-11-24T20:03:00Z",
                        )
                    },
                )
            assertNull(
                rebound.currentGrant(
                    sourceGrant.sourceRef,
                ),
            )
            assertIs<VexLifeWorkspaceRunStore.Outcome.Realized>(
                rebound.grant(
                    sourceGrant.sourceRef,
                ),
            )
            val second =
                assertNotNull(
                    rebound.currentGrant(
                        sourceGrant.sourceRef,
                    ),
                )
            assertEquals(
                reboundSources
                    .grants()
                    .single()
                    .grantedAt,
                second.sourceGrantedAt,
            )
            assertNotEquals(
                first.grantRef,
                second.grantRef,
            )
        }

    @Test
    fun revokingRunsKeepsReadWriteAndCheckRelationshipsIndependent() =
        withHome {
            home,
            _,
            localSources,
            ->
            val runs =
                VexLifeWorkspaceRunStore(
                    productHome =
                        home,
                    localSources =
                        localSources,
                    now = {
                        Instant.parse(
                            "2026-11-24T20:01:00Z",
                        )
                    },
                )
            val sourceRef =
                localSources
                    .grants()
                    .single()
                    .sourceRef

            assertIs<VexLifeWorkspaceRunStore.Outcome.Realized>(
                runs.grant(
                    sourceRef,
                ),
            )
            assertIs<VexLifeWorkspaceRunStore.Outcome.Realized>(
                runs.revoke(
                    sourceRef,
                ),
            )
            assertNull(
                runs.currentGrant(
                    sourceRef,
                ),
            )
            assertNotNull(
                localSources.root(
                    sourceRef,
                ),
            )
            assertEquals(
                1,
                localSources
                    .grants()
                    .size,
            )
        }
}

// [VXG RealForever]
