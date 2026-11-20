package vexlife.application

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VexLifeProdMacDistributionLifetimeTest {
    private val currentSource = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
    private val olderSource = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"
    private val newerSource = "cccccccccccccccccccccccccccccccccccccccc"

    private class FakeHost(
        attached: List<VexLifeProdMacDistributionLifetime.AttachedImage>,
        private val identities: Map<Path, VexLifeProdMacDistributionLifetime.ApplicationIdentity>,
        private val ejectStatuses: Map<Path, Int> = emptyMap(),
    ) : VexLifeProdMacDistributionLifetime.Host {
        private val current = attached.toMutableList()
        val ejected = mutableListOf<Path>()

        override fun attachedImages(): List<VexLifeProdMacDistributionLifetime.AttachedImage> =
            current.map { image ->
                image.copy(mountPoints = image.mountPoints.toList())
            }

        override fun applicationIdentity(
            applicationBundle: Path,
        ): VexLifeProdMacDistributionLifetime.ApplicationIdentity? =
            identities[applicationBundle.toAbsolutePath().normalize()]

        override fun eject(
            mountPoint: Path,
        ): VexLifeProdMacDistributionLifetime.CommandResult {
            val normalized = mountPoint.toAbsolutePath().normalize()
            val status = ejectStatuses[normalized] ?: 0
            if (status == 0) {
                ejected += normalized
                current.replaceAll { image ->
                    image.copy(
                        mountPoints =
                            image.mountPoints.filter {
                                it.toAbsolutePath().normalize() != normalized
                            },
                    )
                }
            }
            return VexLifeProdMacDistributionLifetime.CommandResult(
                status = status,
                stdout = "",
                stderr = if (status == 0) "" else "busy",
            )
        }
    }

    private fun identity(
        sourceSha: String,
        buildVersion: Long,
    ) =
        VexLifeProdMacDistributionLifetime.ApplicationIdentity(
            bundleIdentifier = VexLifeProdMacDistributionLifetime.BundleIdentifier,
            sourceSha = sourceSha,
            buildVersion = buildVersion,
        )

    private fun appAt(root: String): Path =
        Path.of(root, VexLifeProdMacDistributionLifetime.ApplicationName)
            .toAbsolutePath()
            .normalize()

    private fun imageRoot(app: Path): Path =
        app.resolve("Contents/Resources/private-service-image")

    @Test
    fun enclosingApplicationFindsTheRunningBundleAndNothingOutsideOne() {
        val app = appAt("/Applications")
        assertEquals(
            app,
            VexLifeProdMacDistributionLifetime.enclosingApplication(imageRoot(app)),
        )
        assertNull(
            VexLifeProdMacDistributionLifetime.enclosingApplication(
                Path.of("/tmp/private-service-image"),
            ),
        )
    }

    @Test
    fun startupRetainsItsOwnDmgAndEjectsOlderVerifiedVexLifeMounts() {
        val currentMount = Path.of("/Volumes/VexLife")
        val olderMount = Path.of("/Volumes/VexLife 1")
        val currentApp = appAt(currentMount.toString())
        val olderApp = appAt(olderMount.toString())
        val host =
            FakeHost(
                attached =
                    listOf(
                        VexLifeProdMacDistributionLifetime.AttachedImage(
                            imagePath = Path.of("/Users/test/Downloads/current.dmg"),
                            mountPoints = listOf(currentMount),
                        ),
                        VexLifeProdMacDistributionLifetime.AttachedImage(
                            imagePath = Path.of("/Users/test/Downloads/old.dmg"),
                            mountPoints = listOf(olderMount),
                        ),
                    ),
                identities =
                    mapOf(
                        currentApp to identity(currentSource, 200),
                        olderApp to identity(olderSource, 150),
                    ),
            )

        val result =
            VexLifeProdMacDistributionLifetime.reconcile(
                currentImageRoot = imageRoot(currentApp),
                host = host,
            )

        assertTrue(result.effectsPerformed)
        assertEquals(listOf(olderMount.toAbsolutePath().normalize()), host.ejected)
        assertEquals(
            VexLifeProdMacDistributionLifetime.EntryDisposition.RetainedCurrentExecution,
            result.entries
                .single {
                    it.mountPoint == currentMount.toAbsolutePath().normalize().toString()
                }
                .disposition,
        )
        assertEquals(
            VexLifeProdMacDistributionLifetime.EntryDisposition.EjectedVerifiedRetiredMount,
            result.entries
                .single {
                    it.mountPoint == olderMount.toAbsolutePath().normalize().toString()
                }
                .disposition,
        )
    }

    @Test
    fun installedCurrentAppEjectsSameSourceDuplicatesButRetainsPossibleNewerUpdate() {
        val installedApp = appAt("/Applications")
        val duplicateMount = Path.of("/Volumes/VexLife")
        val newerMount = Path.of("/Volumes/VexLife 1")
        val host =
            FakeHost(
                attached =
                    listOf(
                        VexLifeProdMacDistributionLifetime.AttachedImage(
                            imagePath = Path.of("/Users/test/Downloads/same-source.dmg"),
                            mountPoints = listOf(duplicateMount),
                        ),
                        VexLifeProdMacDistributionLifetime.AttachedImage(
                            imagePath = Path.of("/Users/test/Downloads/newer-source.dmg"),
                            mountPoints = listOf(newerMount),
                        ),
                    ),
                identities =
                    mapOf(
                        installedApp to identity(currentSource, 200),
                        appAt(duplicateMount.toString()) to identity(currentSource, 200),
                        appAt(newerMount.toString()) to identity(newerSource, 201),
                    ),
            )

        val result =
            VexLifeProdMacDistributionLifetime.reconcile(
                currentImageRoot = imageRoot(installedApp),
                host = host,
            )

        assertEquals(listOf(duplicateMount.toAbsolutePath().normalize()), host.ejected)
        assertEquals(
            VexLifeProdMacDistributionLifetime.EntryDisposition.EjectedVerifiedRetiredMount,
            result.entries
                .single {
                    it.mountPoint == duplicateMount.toAbsolutePath().normalize().toString()
                }
                .disposition,
        )
        assertEquals(
            VexLifeProdMacDistributionLifetime.EntryDisposition.RetainedNotProvenRetired,
            result.entries
                .single {
                    it.mountPoint == newerMount.toAbsolutePath().normalize().toString()
                }
                .disposition,
        )
    }

    @Test
    fun unverifiedForeignMountIsNeverAReconciliationCandidate() {
        val installedApp = appAt("/Applications")
        val foreignMount = Path.of("/Volumes/Other")
        val host =
            FakeHost(
                attached =
                    listOf(
                        VexLifeProdMacDistributionLifetime.AttachedImage(
                            imagePath = Path.of("/Users/test/Downloads/other.dmg"),
                            mountPoints = listOf(foreignMount),
                        )
                    ),
                identities = mapOf(installedApp to identity(currentSource, 200)),
            )

        val result =
            VexLifeProdMacDistributionLifetime.reconcile(
                currentImageRoot = imageRoot(installedApp),
                host = host,
            )

        assertFalse(result.effectsPerformed)
        assertTrue(result.entries.isEmpty())
        assertTrue(host.ejected.isEmpty())
    }

    @Test
    fun normalEjectFailureIsHeldAndNeverEscalatedToForceOrKill() {
        val installedApp = appAt("/Applications")
        val oldMount = Path.of("/Volumes/VexLife")
        val normalizedOld = oldMount.toAbsolutePath().normalize()
        val host =
            FakeHost(
                attached =
                    listOf(
                        VexLifeProdMacDistributionLifetime.AttachedImage(
                            imagePath = Path.of("/Users/test/Downloads/old.dmg"),
                            mountPoints = listOf(oldMount),
                        )
                    ),
                identities =
                    mapOf(
                        installedApp to identity(currentSource, 200),
                        appAt(oldMount.toString()) to identity(olderSource, 100),
                    ),
                ejectStatuses = mapOf(normalizedOld to 16),
            )

        val result =
            VexLifeProdMacDistributionLifetime.reconcile(
                currentImageRoot = imageRoot(installedApp),
                host = host,
            )

        assertFalse(result.effectsPerformed)
        assertTrue(host.ejected.isEmpty())
        assertEquals(
            VexLifeProdMacDistributionLifetime.EntryDisposition.HeldEjectFailure,
            result.entries.single().disposition,
        )
    }

    @Test
    fun noCurrentApplicationIdentityMeansNoCleanupAuthority() {
        val mount = Path.of("/Volumes/VexLife")
        val host =
            FakeHost(
                attached =
                    listOf(
                        VexLifeProdMacDistributionLifetime.AttachedImage(
                            imagePath = Path.of("/Users/test/Downloads/old.dmg"),
                            mountPoints = listOf(mount),
                        )
                    ),
                identities =
                    mapOf(
                        appAt(mount.toString()) to identity(olderSource, 100),
                    ),
            )

        val result =
            VexLifeProdMacDistributionLifetime.reconcile(
                currentImageRoot = Path.of("/tmp/private-service-image"),
                host = host,
            )

        assertEquals(VexLifeProdMacDistributionLifetime.ResultState.HeldObservation, result.state)
        assertFalse(result.effectsPerformed)
        assertTrue(host.ejected.isEmpty())
    }

    @Test
    fun nativeBootInvokesDistributionReconciliationBeforeHomeAndProductEntry() {
        val source =
            Files.readString(
                Path.of(
                    System.getProperty("user.dir"),
                    "packaging/private-service-image/setup/vexlife-prod-setup-window.applescript",
                )
            )
        val reconcile =
            source.indexOf(
                "my runProductEntry({\"--reconcile-distribution-lifetime\"}, false)"
            )
        val chooseHome =
            source.indexOf(
                "set selectedHomePath to my chooseHome(defaultHome)"
            )
        val inspect =
            source.indexOf(
                "my runProductEntry(my withModel({\"--inspect-front-door\""
            )

        assertTrue(reconcile >= 0, "Mac startup must invoke the named distribution-lifetime effect")
        assertTrue(chooseHome > reconcile, "distribution reconciliation must happen before Home choice")
        assertTrue(inspect > chooseHome, "Product-entry observation must follow boot reconciliation")
    }

    @Test
    fun hdiutilJsonProjectionKeepsImageToMountRelationships() {
        val parsed =
            VexLifeProdMacDistributionLifetime.parseHdiutilInfoJson(
                """
                {
                  "images": [
                    {
                      "image-path": "/Users/test/Downloads/VexLife-macOS-AppleSilicon.dmg",
                      "system-entities": [
                        {"dev-entry": "/dev/disk4", "mount-point": "/Volumes/VexLife"},
                        {"dev-entry": "/dev/disk4s2"}
                      ]
                    },
                    {
                      "image-path": "/Users/test/Downloads/OBS.dmg",
                      "system-entities": [
                        {"dev-entry": "/dev/disk5", "mount-point": "/Volumes/OBS"}
                      ]
                    }
                  ]
                }
                """.trimIndent(),
            )

        assertEquals(2, parsed.size)
        assertEquals(
            listOf(Path.of("/Volumes/VexLife")),
            parsed[0].mountPoints,
        )
        assertEquals(
            Path.of("/Users/test/Downloads/OBS.dmg"),
            parsed[1].imagePath,
        )
    }
}

// [VXG RealForever]
