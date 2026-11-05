package vexlife.model.heretic

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HereticProductModelTest {
    private val home =
        HereticModelConfiguration.BindingScope(
            homeRef = "home.vexlife.test",
            companionLineageRef = "lineage.vexlife.test",
        )

    @Test
    fun certifiedProductIdentityIsOneReadableContract() {
        val identity =
            HereticProductIdentity.Current

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
            "profile.repository.vexlife-prod.macos-v1",
            identity.hostExecutionProfileRef,
        )
        assertFalse(
            identity.hostExecutionProfileRef.contains(
                "vextreme-sdk",
            ),
        )
        assertFalse(
            identity.hostExecutionProfileRef ==
                "profile.repository.vexlife.macos-v1",
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
            HereticArtifactCatalog.Members

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
                    HereticArtifactCatalog.ProviderSourceRef &&
                    it.licenseRef ==
                    HereticArtifactCatalog.LicenseRef
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
            HereticModelConfiguration.pending(
                home,
            )

        assertEquals(
            HereticModelConfiguration.State.EstablishmentPending,
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
            HereticModelConfiguration.isBound(
                pending,
            ),
        )

        val bound =
            HereticModelConfiguration.bound(
                home,
            )

        assertEquals(
            HereticModelConfiguration.State.BoundProductModel,
            bound.state,
        )
        assertTrue(
            HereticModelConfiguration.isBound(
                bound,
            ),
        )
    }

    @Test
    fun configurationRemainsHomeAndCompanionLineageScoped() {
        val configured =
            HereticModelConfiguration.bound(
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
            HereticProductIdentity.ArtifactSetRef,
            configured.artifactRef,
        )
        assertEquals(
            HereticProductIdentity.RuntimeReceiptRelativePath,
            configured.runtimeReceiptPath,
        )
    }
}

// [VXG RealForever]
