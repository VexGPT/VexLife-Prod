package vexlife.service

import java.nio.file.Files
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import vexlife.local.VexLifeProdLocalSetup
import vexlife.model.heretic.HereticModelConfiguration
import vexlife.model.heretic.HereticProductIdentity
import vexlife.model.heretic.HereticRuntimeQualification
import vexlife.model.heretic.HereticRuntimeState
import vexlife.model.heretic.runtime.HereticMacHostProfile

class VexLifeProdCurrentStateTest {
    @Test
    fun currentStateComposesFreshHostAndStorageWithoutPerformingEffects() {
        val home =
            Files.createTempDirectory(
                "vexlife-prod-current-state-",
            )
                .toAbsolutePath()

        try {
            val runtime =
                HereticModelConfiguration.QualifiedRuntimeEvidence(
                    runtimeReceiptRef = "receipt.vexlife-prod.current-state.test",
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
            val setup =
                VexLifeProdLocalSetup.Result(
                    state = "LOCAL_HERETIC_FOUNDATION_READY",
                    productHome = home.toString(),
                    homeDisposition = "EXISTING_CURRENT",
                    modelSelectionDisposition = "HERETIC_ALREADY_BOUND",
                    managedPythonDisposition = "REUSED",
                    artifactDisposition = "HERETIC_ARTIFACTS_MATERIALIZED_EXACT",
                    environmentDisposition = "REUSED",
                    runtimeDisposition = "REUSED",
                    modelBindingDisposition = "HERETIC_ALREADY_BOUND",
                    modelConfigurationState = HereticModelConfiguration.State.BoundProductModel,
                    qualifiedRuntimeEvidence = runtime,
                    endpoint = runtime.endpoint,
                    requestModel = runtime.requestModel,
                )

            val projection =
                VexLifeProdCurrentState.observeWithHooks(
                    setup = setup,
                    hooks =
                        VexLifeProdCurrentState.Hooks(
                            observeHost = {
                                HereticMacHostProfile.evaluate(
                                    HereticMacHostProfile.Observation(
                                        platform = "darwin",
                                        architecture = "arm64",
                                        chipModel = "Apple M4 Pro",
                                        totalMemoryBytes = 36L * 1024L * 1024L * 1024L,
                                    ),
                                )
                            },
                            observeFreeStorageBytes = {
                                100L * 1024L * 1024L * 1024L
                            },
                            now = {
                                Instant.parse(
                                    "2026-11-12T18:00:00Z",
                                )
                            },
                        ),
                )

            assertEquals(
                "ELIGIBLE",
                projection.hostEligibilityState,
            )
            assertEquals(
                36L * 1024L * 1024L * 1024L,
                projection.totalMemoryBytes,
            )
            assertEquals(
                HereticMacHostProfile.MinimumMemoryBytes,
                projection.modelMinimumMemoryBytes,
            )
            assertEquals(
                100L * 1024L * 1024L * 1024L,
                projection.freeStorageBytes,
            )
            assertEquals(
                HereticProductIdentity.ModelRef,
                projection.activeModelRef,
            )
            assertEquals(
                "VERIFIED_AT_SETUP",
                projection.modelArtifactState,
            )
            assertEquals(
                15_153_242_506L,
                projection.modelArtifactTotalBytes,
            )
            assertEquals(
                projection.modelArtifactTotalBytes,
                projection.modelArtifactVerifiedBytes,
            )
            assertEquals(
                "STARTUP_QUALIFIED_NOT_REOBSERVED",
                projection.runtimeCurrentness,
            )
            assertFalse(
                projection.effectsPerformed,
            )
        } finally {
            home
                .toFile()
                .deleteRecursively()
        }
    }
}

// [VXG RealForever]
