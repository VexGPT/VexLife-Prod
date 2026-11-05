package vexlife.model.heretic

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HereticProductModelTest {
    private val home =
        HereticProductModel.HomeIdentity(
            homeRef = "home.vexlife.test",
            companionLineageRef = "lineage.vexlife.test",
        )

    @Test
    fun certifiedProductIdentityIsOneReadableContract() {
        val identity =
            HereticProductModel.CurrentIdentity

        assertEquals(
            "model-bundle.vexlife.heretic.qwen3.8-27b.mlx4.329658ee8f27.001",
            identity.modelBundleRef,
        )
        assertEquals(
            "model.devex.heretic.qwen3.8-27b.mlx4.329658ee8f27",
            identity.modelRef,
        )
        assertEquals(
            "model-profile.devex.qwen38-27b-heretic-mlx4.certified.20260928A",
            identity.modelProfileRef,
        )
        assertEquals(
            "artifact.devex.qwen3.8-27b-heretic-mlx-4bit.808b82a2",
            identity.artifactSetRef,
        )
        assertEquals(
            "329658ee8f270056bc29d8a6cfd6e9f1b7d9d14c659452c9e4dd46fc7c78d780",
            identity.artifactContentSetSha256,
        )
        assertEquals(
            "adapter.runtime.mlx.macos-victor.001",
            identity.runtimeAdapterRef,
        )
        assertEquals(
            "profile.repository.vexlife.macos-v1",
            identity.hostExecutionProfileRef,
        )
        assertFalse(
            identity.hostExecutionProfileRef.contains(
                "vextreme-sdk",
            ),
        )
        assertEquals(
            "3.14.7",
            identity.pythonVersion,
        )
        assertEquals(
            "0.32.0",
            identity.mlxLmVersion,
        )
        assertEquals(
            "http://127.0.0.1:8765",
            identity.endpoint,
        )
        assertEquals(
            "default_model",
            identity.requestModel,
        )
    }

    @Test
    fun certifiedArtifactCatalogIsCompleteAndByteExact() {
        val members =
            HereticProductModel.CertifiedArtifactMembers

        assertEquals(
            11,
            members.size,
        )
        assertEquals(
            15_153_242_506L,
            members.sumOf {
                it.expectedBytes
            },
        )
        assertEquals(
            3,
            members.count {
                it.filename.matches(
                    Regex(
                        "model-\\d{5}-of-00003\\.safetensors",
                    ),
                )
            },
        )
        assertTrue(
            members.all {
                it.sourceRef ==
                    HereticProductModel.ProviderSourceRef &&
                    it.licenseRef ==
                    HereticProductModel.LicenseRef
            },
        )
        assertEquals(
            members.size,
            members
                .map {
                    it.sha256
                }
                .distinct()
                .size,
        )
    }

    @Test
    fun configurationSelectionCannotSilentlyGrantRuntimeEffects() {
        val pending =
            HereticProductModel.pendingConfiguration(
                home,
            )

        assertEquals(
            HereticProductModel.ConfigurationState.EstablishmentPending,
            pending.state,
        )
        assertFalse(
            pending.automaticFallback,
        )
        assertFalse(
            pending.automaticDownload,
        )
        assertFalse(
            pending.automaticActivation,
        )
        assertFalse(
            HereticProductModel.isBound(
                pending,
            ),
        )

        val bound =
            HereticProductModel.boundConfiguration(
                home,
            )

        assertEquals(
            HereticProductModel.ConfigurationState.BoundProductModel,
            bound.state,
        )
        assertTrue(
            HereticProductModel.isBound(
                bound,
            ),
        )
    }

    @Test
    fun configurationRemainsHomeAndCompanionLineageScoped() {
        val configured =
            HereticProductModel.boundConfiguration(
                home,
            )

        assertEquals(
            home.homeRef,
            configured.homeRef,
        )
        assertEquals(
            home.companionLineageRef,
            configured.companionLineageRef,
        )
        assertEquals(
            HereticProductModel.ArtifactSetRef,
            configured.artifactRef,
        )
        assertEquals(
            HereticProductModel.RuntimeReceiptRelativePath,
            configured.runtimeReceiptPath,
        )
    }
}

// [VXG RealForever]
