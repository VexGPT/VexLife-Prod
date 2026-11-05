package vexlife.model.heretic.runtime

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import vexlife.home.VexLifeProdHome
import vexlife.home.VexLifeProdHomeFiles
import vexlife.model.heretic.HereticModelConfiguration
import vexlife.model.heretic.HereticProductIdentity
import vexlife.model.heretic.HereticRuntimeQualification
import vexlife.model.heretic.HereticRuntimeState

class FileHereticModelConfigurationStoreTest {
    private fun plan(): VexLifeProdHome.BootstrapPlan =
        VexLifeProdHome.plan(
            personRef = "person.local-user",
            familyRef = "vex-family.local-user",
            deviceName = "Test Mac",
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

    private fun qualifiedRuntime(): HereticModelConfiguration.QualifiedRuntimeEvidence =
        HereticModelConfiguration.QualifiedRuntimeEvidence(
            runtimeReceiptRef = "receipt.vexlife-prod.heretic.test",
            runtimeState = HereticRuntimeState.Qualified,
            modelRef = HereticProductIdentity.ModelRef,
            modelProfileRef = HereticProductIdentity.ModelProfileRef,
            artifactRef = HereticProductIdentity.ArtifactSetRef,
            artifactContentSetSha256 = HereticProductIdentity.ArtifactContentSetSha256,
            runtimeAdapterRef = HereticProductIdentity.RuntimeAdapterRef,
            runtimeClass = HereticProductIdentity.RuntimeClass,
            endpoint = HereticProductIdentity.Endpoint,
            requestModel = HereticProductIdentity.RequestModel,
            qualification = HereticRuntimeQualification.qualified(),
        )

    @Test
    fun freshHomeTransitionsExplicitlyFromUnconfiguredToPendingToBound() {
        val parent =
            Files.createTempDirectory(
                "vexlife-prod-model-config-",
            )
                .toAbsolutePath()
        val home =
            parent.resolve(
                "VexLife-Prod",
            )

        try {
            val bootstrap =
                VexLifeProdHomeFiles.bootstrap(
                    home,
                    plan(),
                )
            val store =
                FileHereticModelConfigurationStore(
                    home,
                )

            assertIs<FileHereticModelConfigurationStore.Snapshot.Unconfigured>(
                store.read(
                    bootstrap.manifest,
                ),
            )

            assertFailsWith<IllegalArgumentException> {
                store.bindQualified(
                    manifest = bootstrap.manifest,
                    qualifiedRuntime = qualifiedRuntime(),
                )
            }

            val selected =
                store.selectHeretic(
                    bootstrap.manifest,
                )

            assertEquals(
                "HERETIC_SELECTED_PENDING",
                selected.state,
            )
            assertTrue(
                selected.writePerformed,
            )
            assertEquals(
                HereticModelConfiguration.State.EstablishmentPending,
                selected.value.state,
            )

            val selectedAgain =
                store.selectHeretic(
                    bootstrap.manifest,
                )

            assertEquals(
                "HERETIC_ALREADY_SELECTED_PENDING",
                selectedAgain.state,
            )
            assertFalse(
                selectedAgain.writePerformed,
            )

            val bound =
                store.bindQualified(
                    manifest = bootstrap.manifest,
                    qualifiedRuntime = qualifiedRuntime(),
                )

            assertEquals(
                "HERETIC_BOUND_TO_QUALIFIED_RUNTIME",
                bound.state,
            )
            assertTrue(
                bound.writePerformed,
            )
            assertTrue(
                HereticModelConfiguration.isBound(
                    bound.value,
                ),
            )

            val rebound =
                store.bindQualified(
                    manifest = bootstrap.manifest,
                    qualifiedRuntime = qualifiedRuntime(),
                )

            assertEquals(
                "HERETIC_ALREADY_BOUND",
                rebound.state,
            )
            assertFalse(
                rebound.writePerformed,
            )

            val current =
                assertIs<FileHereticModelConfigurationStore.Snapshot.Heretic>(
                    store.read(
                        bootstrap.manifest,
                    ),
                )

            assertEquals(
                HereticModelConfiguration.State.BoundProductModel,
                current.value.state,
            )
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }
}

// [VXG RealForever]
