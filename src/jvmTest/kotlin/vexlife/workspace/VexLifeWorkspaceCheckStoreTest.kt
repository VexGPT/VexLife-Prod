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

class VexLifeWorkspaceCheckStoreTest {
    private fun withHome(
        block: (
            home: Path,
            source: Path,
            localSources: VexLifeLocalSourceStore,
        ) -> Unit,
    ) {
        val parent =
            Files.createTempDirectory(
                "vexlife-workspace-checks-",
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
                    "workspace-checks",
                lineageSuffix =
                    "workspace-checks",
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
    fun checkGrantBindsTheExactLocalSourceGeneration() =
        withHome {
            home,
            source,
            localSources,
            ->
            var now =
                Instant.parse(
                    "2026-11-24T20:01:00Z",
                )
            val checks =
                VexLifeWorkspaceCheckStore(
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

            assertIs<VexLifeWorkspaceCheckStore.Outcome.Realized>(
                checks.grant(
                    sourceGrant.sourceRef,
                ),
            )
            val first =
                assertNotNull(
                    checks.currentGrant(
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
                checks.currentGrant(
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
                VexLifeWorkspaceCheckStore(
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
            assertIs<VexLifeWorkspaceCheckStore.Outcome.Realized>(
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
    fun revokingChecksKeepsReadAndWriteRelationshipsIndependent() =
        withHome {
            home,
            _,
            localSources,
            ->
            val checks =
                VexLifeWorkspaceCheckStore(
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

            assertIs<VexLifeWorkspaceCheckStore.Outcome.Realized>(
                checks.grant(
                    sourceRef,
                ),
            )
            assertIs<VexLifeWorkspaceCheckStore.Outcome.Realized>(
                checks.revoke(
                    sourceRef,
                ),
            )
            assertNull(
                checks.currentGrant(
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
