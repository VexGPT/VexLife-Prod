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
        providerNativeToolsQualified: Boolean = false,
        vexReachQualified: Boolean = false,
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
            qualification =
                HereticRuntimeQualification.qualified(
                    providerNativeToolsQualified =
                        providerNativeToolsQualified,
                    vexReachQualified =
                        vexReachQualified,
                ),
        )
    }

    private fun project(
        profile: HereticCompanionRepresentation.Profile,
        providerNativeToolsQualified: Boolean = false,
        vexReachQualified: Boolean = false,
        model: HereticModelCatalog.Entry = HereticModelCatalog.Models.Qwen27B,
    ): VexLifeEffectiveModelCapabilities.Projection {
        return VexLifeEffectiveModelCapabilities.project(
            modelRef = model.modelRef,
            profile = profile,
            realization =
                HereticRuntimeRealization.require(
                    modelRef = model.modelRef,
                    target = HereticRuntimeRealization.MacArm64,
                ),
            qualifiedRuntime =
                qualifiedRuntime(
                    model = model,
                    providerNativeToolsQualified =
                        providerNativeToolsQualified,
                    vexReachQualified =
                        vexReachQualified,
                ),
        )
    }

    @Test
    fun importedQwenReachStaysHeldWithoutPerModelReachQualification() {
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
            VexLifeEffectiveModelCapabilities.Standing.HELD,
            capabilities.vexReach,
        )
    }

    @Test
    fun exactRuntimeNativeToolQualificationPromotesRecognizedImportToAvailable() {
        val profile =
            HereticCompanionRepresentation.Profiles.importedQwen(
                profileRef =
                    "companion-profile.vexlife-prod.imported.test.qwen3.qualified.v1",
                nativeToolCalling = true,
            )
        val capabilities =
            project(
                profile = profile,
                providerNativeToolsQualified = true,
            )

        assertEquals(
            HereticCompanionRepresentation.ProviderNativeToolStanding.RECOGNIZED_HELD,
            profile.providerNativeToolStanding,
        )
        assertEquals(
            VexLifeEffectiveModelCapabilities.Standing.AVAILABLE,
            capabilities.providerNativeTools,
        )
        assertEquals(
            VexLifeEffectiveModelCapabilities.Standing.HELD,
            capabilities.vexReach,
        )
    }

    @Test
    fun exactRuntimeVexReachQualificationPromotesHeld4BReachWithoutNativeTools() {
        val capabilities =
            project(
                profile = HereticCompanionRepresentation.Profiles.Qwen4B,
                vexReachQualified = true,
                model = HereticModelCatalog.Models.Qwen4B,
            )

        assertEquals(
            VexLifeEffectiveModelCapabilities.Standing.AVAILABLE,
            capabilities.vexReach,
        )
        assertEquals(
            VexLifeEffectiveModelCapabilities.Standing.NOT_APPLICABLE,
            capabilities.providerNativeTools,
            "4B Vex Reach qualification must not re-enable the superseded native-tool path",
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
    fun capabilityDisplayFactsKeepHumanMeaningWithTheCapabilityProjectionOwner() {
        val available =
            project(
                HereticCompanionRepresentation.Profiles.Qwen27B,
            )
                .displayFacts
                .associateBy {
                    it.label.key
                }

        val reach =
            requireNotNull(
                available[
                    "workspace.label.model-capability-vex-reach"
                ],
            )
        assertEquals(
            VexLifeEffectiveModelCapabilities.Standing.AVAILABLE,
            reach.standing,
        )
        assertEquals(
            "workspace.label.model-capability-vex-reach",
            reach.label.key,
        )
        assertEquals(
            "workspace.status.model-capability-available",
            reach.status.key,
        )

        val noNativeTools =
            project(
                HereticCompanionRepresentation.Profiles.importedQwen(
                    profileRef = "companion-profile.vexlife-prod.imported.test.display.no-tools.v1",
                    nativeToolCalling = false,
                ),
            )
                .displayFacts
                .associateBy {
                    it.label.key
                }[
                    "workspace.label.model-capability-provider-native-tools"
                ]
        assertEquals(
            VexLifeEffectiveModelCapabilities.Standing.NOT_APPLICABLE,
            noNativeTools?.standing,
        )
        assertEquals(
            "workspace.status.model-capability-not-applicable",
            noNativeTools?.status?.key,
        )

        val heldReach =
            project(
                profile = HereticCompanionRepresentation.Profiles.Qwen4B,
                model = HereticModelCatalog.Models.Qwen4B,
            )
                .displayFacts
                .associateBy {
                    it.label.key
                }[
                    "workspace.label.model-capability-vex-reach"
                ]
        assertEquals(
            VexLifeEffectiveModelCapabilities.Standing.HELD,
            heldReach?.standing,
        )
        assertEquals(
            "workspace.status.model-capability-held",
            heldReach?.status?.key,
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
