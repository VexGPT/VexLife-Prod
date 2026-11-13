package vexlife.publicentry

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import vexlife.entry.VexLifeProdEntryFamily

class VexLifeProdPublicEntryTest {
    private val release =
        VexLifeProdPublicEntry.ReleaseCoordinate(
            schemaVersion =
                "vexlife-prod.private-service-release/v1",
            repository =
                "VexGPT/VexLife-Prod",
            platform =
                "darwin-arm64",
            currentChannel =
                "github-release-latest",
            currentBootstrapAssetName =
                "vexlife-prod-bootstrap-darwin-arm64.command",
            currentBootstrapSha256AssetName =
                "vexlife-prod-bootstrap-darwin-arm64.command.sha256",
        )

    private val host =
        VexLifeProdPublicEntry.HostProfile(
            platform = "darwin",
            architecture = "arm64",
            chipModel = "Apple M4 Pro",
            minimumMemoryBytes =
                12L *
                    1024L *
                    1024L *
                    1024L,
        )

    @Test
    fun publicProjectionReusesEntryFamilyAndStableReleaseCoordinate() {
        val projection =
            VexLifeProdPublicEntry.project(
                release = release,
                hostProfile = host,
            )

        assertEquals(
            "NONE",
            projection.pageProductEffectClass,
        )
        assertEquals(
            "NONE",
            projection.executionAuthority,
        )
        assertFalse(
            projection.effectsPerformed,
        )
        assertEquals(
            VexLifeProdPublicEntry.StableHumanDoorway,
            projection.stableHumanDoorway,
        )
        assertEquals(
            VexLifeProdPublicEntry.CanonicalProductPage,
            projection.canonicalProductPage,
        )

        assertEquals(
            setOf(
                VexLifeProdEntryFamily.StateCurrent,
                VexLifeProdEntryFamily.StateUpdateAvailable,
                VexLifeProdEntryFamily.StateRepairRequired,
                VexLifeProdEntryFamily.StateUpdateRepairRequired,
                VexLifeProdEntryFamily.StateSetupRequired,
                VexLifeProdEntryFamily.StateHeldUnknown,
            ),
            projection.entryOptions
                .map {
                    it.stateRef
                }
                .toSet(),
        )

        val update =
            projection.entryOptions
                .single {
                    it.stateRef ==
                        VexLifeProdEntryFamily.StateUpdateAvailable
                }
        assertEquals(
            "Update & Restart",
            update.primaryLabel,
        )
        assertEquals(
            "Open Current",
            update.secondaryLabel,
        )

        val setup =
            projection.entryOptions
                .single {
                    it.stateRef ==
                        VexLifeProdEntryFamily.StateSetupRequired
                }
        assertEquals(
            "Set Up VexLife",
            setup.primaryLabel,
        )

        val held =
            projection.entryOptions
                .single {
                    it.stateRef ==
                        VexLifeProdEntryFamily.StateHeldUnknown
                }
        assertEquals(
            null,
            held.primaryLabel,
        )
        assertEquals(
            null,
            held.secondaryLabel,
        )
        assertEquals(
            "Close",
            held.cancelLabel,
        )
    }

    @Test
    fun stableMacCommandVerifiesCurrentBootstrapBeforeExecution() {
        val projection =
            VexLifeProdPublicEntry.project(
                release = release,
                hostProfile = host,
            )
        val command =
            projection.bootstrapCommand
                .joinToString(
                    "\n",
                )

        assertTrue(
            projection.currentBootstrapUrl ==
                "https://github.com/VexGPT/VexLife-Prod/releases/latest/download/vexlife-prod-bootstrap-darwin-arm64.command",
        )
        assertTrue(
            projection.currentBootstrapSha256Url ==
                "https://github.com/VexGPT/VexLife-Prod/releases/latest/download/vexlife-prod-bootstrap-darwin-arm64.command.sha256",
        )
        assertTrue(
            command.contains(
                "/usr/bin/shasum -a 256 -c vexlife-prod-bootstrap-darwin-arm64.command.sha256",
            ),
        )
        assertTrue(
            command.contains(
                "/bin/bash ./vexlife-prod-bootstrap-darwin-arm64.command",
            ),
        )
        assertFalse(
            command.contains(
                "vgong24/VexLife/main/setup-vexlife.command",
            ),
        )
    }

    @Test
    fun familyWorldsHaveExactlyOneCurrentVexLifeHome() {
        val projection =
            VexLifeProdPublicEntry.project(
                release = release,
                hostProfile = host,
            )

        assertEquals(
            1,
            projection.familyWorlds.count {
                it.current
            },
        )
        assertEquals(
            "VexLife",
            projection.familyWorlds
                .single {
                    it.current
                }
                .label,
        )
        assertEquals(
            VexLifeProdPublicEntry.CanonicalProductPage,
            projection.familyWorlds
                .single {
                    it.current
                }
                .href,
        )
    }
}

// [VXG RealForever]
