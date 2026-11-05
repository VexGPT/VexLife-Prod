package vexlife.home

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VexLifeProdHomeTest {
    private val plan =
        VexLifeProdHome.plan(
            personRef = "person.local-user",
            familyRef = "vex-family.local-user",
            deviceName = "Victor's Mac",
            platform = "darwin",
            architecture = "arm64",
            ids =
                VexLifeProdHome.InstallationIds(
                    deviceRef =
                        "device.vexlife.00000000-0000-0000-0000-000000000001",
                    companionLineageRef =
                        "companion-lineage.vexlife.00000000-0000-0000-0000-000000000002",
                ),
            createdAt = "2026-11-04T17:30:00-08:00",
        )

    @Test
    fun newImplementationPreservesCanonicalVexLifeHomeAndLineageIdentity() {
        assertEquals(
            "vexlife.home/v0",
            plan.manifest.schemaVersion,
        )
        assertEquals(
            "vex-home.${plan.installation.deviceRef}",
            plan.manifest.homeRef,
        )
        assertTrue(
            plan.installation.deviceRef.startsWith(
                "device.vexlife.",
            ),
        )
        assertTrue(
            plan.installation.companionLineageRef.startsWith(
                "companion-lineage.vexlife.",
            ),
        )

        assertFalse(
            plan.manifest.homeRef.startsWith(
                "vex-home.prod.",
            ),
        )
        assertFalse(
            plan.installation.deviceRef.startsWith(
                "device.vexlife-prod.",
            ),
        )
        assertFalse(
            plan.installation.companionLineageRef.startsWith(
                "companion-lineage.vexlife-prod.",
            ),
        )
    }

    @Test
    fun productRuntimeCoordinatesDoNotReplaceCanonicalHomeConfiguration() {
        assertEquals(
            listOf(
                "config",
                "devices",
                "runtime",
                "recovery",
            ),
            plan.directories,
        )
        assertEquals(
            "config/model.json",
            plan.manifest.modelConfigurationRef,
        )
        assertEquals(
            "runtime/vexlife-prod/model-binding.json",
            VexLifeProdHome.ModelConfigurationRelativePath,
        )
        assertTrue(
            plan.writes.contains(
                VexLifeProdHome.ModelConfigurationRelativePath,
            ),
        )
        assertFalse(
            plan.writes.contains(
                plan.manifest.modelConfigurationRef,
            ),
        )
    }

    @Test
    fun productRuntimeBindingCannotSilentlyDownloadFallbackOrActivate() {
        val model =
            plan.initialModelConfiguration

        assertEquals(
            "UNCONFIGURED",
            model.state,
        )
        assertFalse(
            model.automaticFallback,
        )
        assertFalse(
            model.automaticDownload,
        )
        assertFalse(
            model.automaticActivation,
        )
    }

    @Test
    fun bootstrapPlanPreservesExistingHumanContinuityAndDeletesNothing() {
        val receipt =
            plan.receipt

        assertFalse(
            receipt.legacyVexLifeHomeImported,
        )
        assertFalse(
            receipt.personalMemoryImported,
        )
        assertFalse(
            receipt.modelArtifactDownloaded,
        )
        assertFalse(
            receipt.existingDataDeleted,
        )
        assertEquals(
            "PRESERVE_AND_CLASSIFY",
            plan.manifest.existingHomePolicy,
        )
    }

    @Test
    fun repositoryProductNameCannotMintAnotherVexIdentity() {
        assertFailsWith<IllegalArgumentException> {
            VexLifeProdHome.InstallationIds(
                deviceRef =
                    "device.vexlife-prod.00000000-0000-0000-0000-000000000001",
                companionLineageRef =
                    "companion-lineage.vexlife-prod.00000000-0000-0000-0000-000000000002",
            )
        }
    }

    @Test
    fun homeAndCompanionRefsRemainOneBorrowedBindingScope() {
        val (
            homeRef,
            lineageRef,
        ) =
            VexLifeProdHome.bindingScope(
                plan.manifest,
            )

        assertEquals(
            plan.manifest.homeRef,
            homeRef,
        )
        assertEquals(
            plan.manifest.currentCompanionLineageRef,
            lineageRef,
        )
    }
}

// [VXG RealForever]
