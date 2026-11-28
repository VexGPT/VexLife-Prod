package vexlife.sources

import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vexlife.home.CanonicalVexLifeHomeFixture

class VexLifeLocalSourceStoreTest {
    private fun withDirs(
        block: (
            vexHome: Path,
            personHome: Path,
        ) -> Unit,
    ) {
        val parent =
            Files.createTempDirectory(
                "vexlife-local-sources-",
            )
                .toRealPath()
        val person =
            Files.createDirectories(
                parent.resolve(
                    "person",
                ),
            )
        val vexHome =
            person.resolve(
                "VexLife-Prod",
            )

        try {
            CanonicalVexLifeHomeFixture.form(
                home = vexHome,
                deviceSuffix = "local-sources",
                lineageSuffix = "local-sources",
            )
            block(
                vexHome,
                person,
            )
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }

    private fun store(
        vexHome: Path,
        personHome: Path,
    ) =
        VexLifeLocalSourceStore(
            productHome = vexHome,
            personHome = personHome,
            now = {
                Instant.parse(
                    "2026-11-20T07:00:00Z",
                )
            },
        )

    @Test
    fun onlyAddAndRemoveChangeThePersistentGrantSet() =
        withDirs {
            vexHome,
            personHome,
            ->
            val project =
                Files.createDirectories(
                    personHome.resolve(
                        "Documents/Project",
                    ),
                )
            val sourceStore =
                store(
                    vexHome,
                    personHome,
                )

            assertTrue(
                sourceStore.grants()
                    .isEmpty(),
            )

            val added =
                assertIs<VexLifeLocalSourceStore.Outcome.Realized>(
                    sourceStore.add(
                        project,
                    ),
                )
                    .grants
                    .single()

            assertEquals(
                "Project",
                added.name,
            )
            assertTrue(
                VexLifeLocalSourcesFamily.isSourceRef(
                    added.sourceRef,
                ),
            )
            assertEquals(
                64,
                requireNotNull(
                    VexLifeLocalSourcesFamily.sha256FromSourceRef(
                        added.sourceRef,
                    ),
                )
                    .length,
            )
            assertEquals(
                listOf(
                    added,
                ),
                store(
                    vexHome,
                    personHome,
                )
                    .grants(),
                "grants survive a new store instance",
            )
            assertEquals(
                project.toRealPath(),
                sourceStore.root(
                    added.sourceRef,
                ),
            )

            assertEquals(
                VexLifeLocalSourceStore.RefusedAlreadyGranted,
                assertIs<VexLifeLocalSourceStore.Outcome.Refused>(
                    sourceStore.add(
                        project.resolve(
                            ".",
                        ),
                    ),
                )
                    .reasonKey,
            )

            assertIs<VexLifeLocalSourceStore.Outcome.Realized>(
                sourceStore.remove(
                    added.sourceRef,
                ),
            )
            assertTrue(
                sourceStore.grants()
                    .isEmpty(),
            )
            assertNull(
                sourceStore.root(
                    added.sourceRef,
                ),
            )
        }

    @Test
    fun broadLinkedAndVexHomeFoldersAreRefused() =
        withDirs {
            vexHome,
            personHome,
            ->
            val sourceStore =
                store(
                    vexHome,
                    personHome,
                )
            val file =
                Files.writeString(
                    personHome.resolve(
                        "a.txt",
                    ),
                    "x",
                )
            val target =
                Files.createDirectories(
                    personHome.resolve(
                        "real",
                    ),
                )
            val link =
                Files.createSymbolicLink(
                    personHome.resolve(
                        "link",
                    ),
                    target,
                )

            fun refusal(
                path: Path,
            ): String =
                assertIs<VexLifeLocalSourceStore.Outcome.Refused>(
                    sourceStore.add(
                        path,
                    ),
                )
                    .reasonKey

            assertEquals(
                VexLifeLocalSourceStore.RefusedNotFolder,
                refusal(
                    file,
                ),
            )
            assertEquals(
                VexLifeLocalSourceStore.RefusedLink,
                refusal(
                    link,
                ),
            )
            assertEquals(
                VexLifeLocalSourceStore.RefusedTooBroad,
                refusal(
                    personHome,
                ),
            )
            assertEquals(
                VexLifeLocalSourceStore.RefusedOverlapsVexHome,
                refusal(
                    vexHome,
                ),
            )
            assertEquals(
                VexLifeLocalSourceStore.RefusedOverlapsVexHome,
                refusal(
                    Files.createDirectories(
                        vexHome.resolve(
                            "runtime",
                        ),
                    ),
                ),
            )
            assertTrue(
                sourceStore.grants()
                    .isEmpty(),
            )
        }

    @Test
    fun missingOrReboundRootBecomesUnavailableRatherThanSilentlyChangingGrant() =
        withDirs {
            vexHome,
            personHome,
            ->
            val project =
                Files.createDirectories(
                    personHome.resolve(
                        "Project",
                    ),
                )
            val sourceStore =
                store(
                    vexHome,
                    personHome,
                )
            val grant =
                assertIs<VexLifeLocalSourceStore.Outcome.Realized>(
                    sourceStore.add(
                        project,
                    ),
                )
                    .grants
                    .single()

            project
                .toFile()
                .deleteRecursively()

            assertNull(
                sourceStore.root(
                    grant.sourceRef,
                ),
            )
            assertEquals(
                listOf(
                    grant,
                ),
                sourceStore.grants(),
                "grant history remains explicit even when the real folder is temporarily unavailable",
            )
        }
}

// [VXG RealForever]
