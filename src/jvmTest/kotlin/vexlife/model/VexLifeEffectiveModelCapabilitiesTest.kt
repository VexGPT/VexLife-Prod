package vexlife.model

import kotlin.test.Test
import kotlin.test.assertEquals
import vexlife.model.heretic.HereticCompanionRepresentation
import vexlife.model.heretic.HereticModelCatalog
import vexlife.model.heretic.HereticModelConfiguration
import vexlife.model.heretic.HereticRuntimeQualification
import vexlife.model.heretic.HereticRuntimeRealization
import vexlife.model.heretic.HereticRuntimeState

class VexLifeEffectiveModelCapabilitiesTest {
    private fun qualifiedRuntime(
        model: HereticModelCatalog.Entry,
    ): HereticModelConfiguration.QualifiedRuntimeEvidence {
        val realization =
            HereticRuntimeRealization.require(
                modelRef = model.modelRef,
                target = HereticRuntimeRealization.MacArm64,
            )
        return HereticModelConfiguration.QualifiedRuntimeEvidence(
            runtimeReceiptRef = "receipt.vexlife-prod.model-adaptation.capabilities",
            runtimeState = HereticRuntimeState.Qualified,
            modelRef = realization.modelRef,
            realizationRef = realization.realizationRef,
            modelProfileRef = realization.modelProfileRef,
            artifactRef = realization.artifact.artifactRef,
            artifactContentSetSha256 = realization.artifact.contentSetSha256,
            runtimeAdapterRef = realization.runtime.runtimeAdapterRef,
            runtimeClass = realization.runtime.runtimeClass,
            endpoint = realization.runtime.endpoint,
            requestModel = realization.runtime.requestModel,
            qualification = HereticRuntimeQualification.qualified(),
        )
    }

    private fun project(
        profile: HereticCompanionRepresentation.Profile,
    ): VexLifeEffectiveModelCapabilities.Projection {
        val model =
            HereticModelCatalog.Models.Qwen27B
        return VexLifeEffectiveModelCapabilities.project(
            modelRef = model.modelRef,
            profile = profile,
            realization =
                HereticRuntimeRealization.require(
                    modelRef = model.modelRef,
                    target = HereticRuntimeRealization.MacArm64,
                ),
            qualifiedRuntime = qualifiedRuntime(model),
        )
    }

    @Test
    fun recognizedImportedNativeToolsStayHeldWhileQualifiedTextFallbackRemainsAvailable() {
        val profile =
            HereticCompanionRepresentation.Profiles.importedQwen(
                profileRef = "companion-profile.vexlife-prod.imported.test.qwen3.v1",
                nativeToolCalling = true,
            )
        val capabilities =
            project(
                profile,
            )

        assertEquals(
            HereticCompanionRepresentation.CapabilityRepresentation.VEXLIFE_CONTROL_TEXT,
            profile.capabilityRepresentation,
        )
        assertEquals(
            HereticCompanionRepresentation.ProviderNativeToolStanding.RECOGNIZED_HELD,
            profile.providerNativeToolStanding,
        )
        assertEquals(
            VexLifeEffectiveModelCapabilities.Standing.HELD,
            capabilities.providerNativeTools,
        )
        assertEquals(
            VexLifeEffectiveModelCapabilities.Standing.AVAILABLE,
            capabilities.vexReach,
        )
    }

    @Test
    fun qualifiedBuiltInNativeToolCarriageRemainsAvailable() {
        val capabilities =
            project(
                HereticCompanionRepresentation.Profiles.Qwen27B,
            )

        assertEquals(
            VexLifeEffectiveModelCapabilities.Standing.AVAILABLE,
            capabilities.providerNativeTools,
        )
    }

    @Test
    fun absentImportedNativeToolGrammarRemainsNotApplicable() {
        val profile =
            HereticCompanionRepresentation.Profiles.importedQwen(
                profileRef = "companion-profile.vexlife-prod.imported.test.no-tools.v1",
                nativeToolCalling = false,
            )
        val capabilities =
            project(
                profile,
            )

        assertEquals(
            HereticCompanionRepresentation.ProviderNativeToolStanding.NOT_REPRESENTED,
            profile.providerNativeToolStanding,
        )
        assertEquals(
            VexLifeEffectiveModelCapabilities.Standing.NOT_APPLICABLE,
            capabilities.providerNativeTools,
        )
    }
}

// [VXG RealForever]
