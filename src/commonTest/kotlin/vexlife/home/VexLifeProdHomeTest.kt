package vexlife.home

import kotlin.test.Test
import kotlin.test.assertEquals
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
                        "device.vexlife-prod.00000000-0000-0000-0000-000000000001",
                    companionLineageRef =
                        "companion-lineage.vexlife-prod.00000000-0000-0000-0000-000000000002",
                ),
            createdAt = "2026-11-04T17:30:00-08:00",
        )

    @Test
    fun freshHomeIdentityIsSeparateFromLegacyVexLifeNamespace() {
        assertTrue(
            plan.manifest.homeRef.startsWith(
                "vex-home.prod.",
            ),
        )
        assertTrue(
            plan.installation.deviceRef.startsWith(
                "device.vexlife-prod.",
            ),
        )
        assertTrue(
            plan.installation.companionLineageRef.startsWith(
                "companion-lineage.vexlife-prod.",
            ),
        )

        assertFalse(
            plan.installation.deviceRef.startsWith(
                "device.vexlife.",
            ),
        )
        assertFalse(
            plan.installation.companionLineageRef.startsWith(
                "companion-lineage.vexlife.",
            ),
        )
    }

    @Test
    fun baseHomeCreatesOnlyCoordinatesOwnedByTheFoundation() {
        assertEquals(
            listOf(
                "config",
                "runtime",
                "recovery",
            ),
            plan.directories,
        )
        assertEquals(
            listOf(
                "config/home.json",
                "config/model.json",
                "recovery/bootstrap-receipt.json",
            ),
            plan.writes,
        )

        // Models, Conversations, Memory, Score, Dream, etc. are created by their rightful owners
        // when those capabilities are ported; the Home foundation does not pre-author them.
        assertFalse(
            plan.directories.any {
                it.startsWith(
                    "models",
                )
            },
        )
        assertFalse(
            plan.directories.any {
                it.startsWith(
                    "conversations",
                )
            },
        )
    }

    @Test
    fun initialModelStateCannotSilentlyDownloadFallbackOrActivate() {
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
    fun freshBootstrapImportsNothingAndDeletesNothing() {
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
    fun homeAndCompanionRefsCanBePassedAsOneBorrowedBindingScope() {
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
