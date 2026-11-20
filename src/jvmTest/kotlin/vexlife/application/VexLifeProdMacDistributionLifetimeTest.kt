package vexlife.application

import java.nio.charset.StandardCharsets
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import vexlife.application.VexLifeProdMacDistributionLifetime.ApplicationIdentity
import vexlife.application.VexLifeProdMacDistributionLifetime.AttachedImage
import vexlife.application.VexLifeProdMacDistributionLifetime.CommandResult
import vexlife.application.VexLifeProdMacDistributionLifetime.EntryDisposition
import vexlife.application.VexLifeProdMacDistributionLifetime.Host
import vexlife.application.VexLifeProdMacDistributionLifetime.ResultState

class VexLifeProdMacDistributionLifetimeTest {
    private val sourceA = "1111111111111111111111111111111111111111"
    private val sourceB = "2222222222222222222222222222222222222222"

    private class FakeHost(
        initialImages: List<AttachedImage>,
        private val identity: (Path, Int) -> ApplicationIdentity?,
        private val detachStatus: Int = 0,
    ) : Host {
        var images = initialImages
        val detached = mutableListOf<Path>()
        private val identityCalls = mutableMapOf<Path, Int>()

        override fun attachedImages(): List<AttachedImage> = images

        override fun applicationIdentity(mountPoint: Path): ApplicationIdentity? {
            val call = (identityCalls[mountPoint] ?: 0) + 1
            identityCalls[mountPoint] = call
            return identity(mountPoint, call)
        }

        override fun detach(mountPoint: Path): CommandResult {
            detached += mountPoint
            if (detachStatus == 0) {
                images =
                    images.map { image ->
                        image.copy(
                            mountPoints =
                                image.mountPoints.filterNot {
                                    it == mountPoint
                                }
                        )
                    }
            }
            return CommandResult(
                status = detachStatus,
                stdout = if (detachStatus == 0) "ejected" else "",
                stderr = if (detachStatus == 0) "" else "busy",
            )
        }
    }

    @Test
    fun startupRetainsTheExecutingDistributionDetachesExactStaleVexLifeAndIgnoresOthers() {
        val currentMount = Path.of("/Volumes/VexLife")
        val staleMount = Path.of("/Volumes/VexLife 1")
        val otherMount = Path.of("/Volumes/OBS Studio")
        val host =
            FakeHost(
                initialImages =
                    listOf(
                        AttachedImage(
                            imagePath = Path.of("/Users/vexlife/Downloads/current.dmg"),
                            mountPoints = listOf(currentMount),
                        ),
                        AttachedImage(
                            imagePath = Path.of("/Users/vexlife/Downloads/older-test.dmg"),
                            mountPoints = listOf(staleMount),
                        ),
                        AttachedImage(
                            imagePath = Path.of("/Users/vexlife/Downloads/obs.dmg"),
                            mountPoints = listOf(otherMount),
                        ),
                    ),
                identity = { mount, _ ->
                    when (mount) {
                        currentMount -> ApplicationIdentity("com.vexlife.prod", sourceB)
                        staleMount -> ApplicationIdentity("com.vexlife.prod", sourceA)
                        else -> null
                    }
                },
            )

        val result =
            VexLifeProdMacDistributionLifetime.reconcile(
                currentImageRoot =
                    currentMount.resolve(
                        "VexLife.app/Contents/Resources/private-service-image",
                    ),
                host = host,
            )

        assertEquals(ResultState.Reconciled, result.state)
        assertTrue(result.effectsPerformed)
        assertEquals(listOf(staleMount), host.detached)
        assertEquals(
            setOf(
                EntryDisposition.RetainedCurrentExecution,
                EntryDisposition.DetachedVerifiedRetiredMount,
            ),
            result.entries.map { it.disposition }.toSet(),
        )
        assertTrue(
            result.entries.none {
                it.mountPoint == otherMount.toString()
            },
            "an unrelated mounted application is outside the VexLife distribution owner",
        )
    }

    @Test
    fun changedVexLifeIdentityIsHeldInsteadOfDetached() {
        val mount = Path.of("/Volumes/VexLife 9")
        val host =
            FakeHost(
                initialImages =
                    listOf(
                        AttachedImage(
                            imagePath = Path.of("/Users/vexlife/Downloads/test.dmg"),
                            mountPoints = listOf(mount),
                        )
                    ),
                identity = { _, call ->
                    ApplicationIdentity(
                        "com.vexlife.prod",
                        if (call <= 2) sourceA else sourceB,
                    )
                },
            )

        val result =
            VexLifeProdMacDistributionLifetime.reconcile(
                currentImageRoot = Path.of("/Applications/VexLife.app/Contents/Resources/private-service-image"),
                host = host,
            )

        assertFalse(result.effectsPerformed)
        assertTrue(host.detached.isEmpty())
        assertEquals(EntryDisposition.HeldIdentityChanged, result.entries.single().disposition)
    }

    @Test
    fun busyDistributionIsHeldWithoutForceDetach() {
        val mount = Path.of("/Volumes/VexLife 4")
        val host =
            FakeHost(
                initialImages =
                    listOf(
                        AttachedImage(
                            imagePath = Path.of("/Users/vexlife/Downloads/test.dmg"),
                            mountPoints = listOf(mount),
                        )
                    ),
                identity = { _, _ -> ApplicationIdentity("com.vexlife.prod", sourceA) },
                detachStatus = 16,
            )

        val result =
            VexLifeProdMacDistributionLifetime.reconcile(
                currentImageRoot = Path.of("/Applications/VexLife.app/Contents/Resources/private-service-image"),
                host = host,
            )

        assertFalse(result.effectsPerformed)
        assertEquals(listOf(mount), host.detached)
        assertEquals(EntryDisposition.HeldDetachFailure, result.entries.single().disposition)
        assertTrue(result.entries.single().detail?.contains("busy") == true)
    }

    @Test
    fun unavailableDiskImageObservationDoesNotBlockProductStartup() {
        val host =
            object : Host {
                override fun attachedImages(): List<AttachedImage> =
                    error("disk image service unavailable")

                override fun applicationIdentity(mountPoint: Path): ApplicationIdentity? =
                    error("not reached")

                override fun detach(mountPoint: Path): CommandResult =
                    error("not reached")
            }

        val result =
            VexLifeProdMacDistributionLifetime.reconcile(
                currentImageRoot = Path.of("/Applications/VexLife.app/Contents/Resources/private-service-image"),
                host = host,
            )

        assertEquals(ResultState.HeldObservation, result.state)
        assertFalse(result.effectsPerformed)
        assertTrue(result.entries.isEmpty())
    }

    @Test
    fun hdiutilPlistParserAcceptsAppleDoctypeAndKeepsImageMountRelationships() {
        val plist =
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
            <plist version="1.0">
              <dict>
                <key>images</key>
                <array>
                  <dict>
                    <key>image-path</key>
                    <string>/Users/vexlife/Downloads/VexLife-test.dmg</string>
                    <key>system-entities</key>
                    <array>
                      <dict>
                        <key>dev-entry</key>
                        <string>/dev/disk42s1</string>
                        <key>mount-point</key>
                        <string>/Volumes/VexLife 42</string>
                      </dict>
                    </array>
                  </dict>
                </array>
              </dict>
            </plist>
            """.trimIndent()

        val parsed =
            VexLifeProdMacDistributionLifetime.parseHdiutilInfoPlist(
                plist.toByteArray(StandardCharsets.UTF_8),
            )

        assertEquals(
            listOf(
                AttachedImage(
                    imagePath = Path.of("/Users/vexlife/Downloads/VexLife-test.dmg"),
                    mountPoints = listOf(Path.of("/Volumes/VexLife 42")),
                )
            ),
            parsed,
        )
    }
}

// [VXG RealForever]
