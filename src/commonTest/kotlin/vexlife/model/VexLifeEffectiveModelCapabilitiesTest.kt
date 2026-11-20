package vexlife.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import vexlife.model.heretic.HereticModelCatalog
import vexlife.model.heretic.HereticModelConfiguration
import vexlife.model.heretic.HereticRuntimeQualification
import vexlife.model.heretic.HereticRuntimeRealization
import vexlife.model.heretic.HereticRuntimeState

class VexLifeEffectiveModelCapabilitiesTest {
    @Test
    fun visualInputBecomesAvailableOnlyWhenTextAndVisualRealizationsAreBothExactAndCurrent() {
        val model =
            HereticModelCatalog.Models.Qwen4B
        val realization =
            assertNotNull(
                HereticRuntimeRealization.resolve(
                    model = model,
                    target = HereticRuntimeRealization.MacArm64,
                ),
            )
        val runtime =
            HereticModelConfiguration.QualifiedRuntimeEvidence(
                runtimeReceiptRef =
                    "receipt.vexlife-prod.heretic.capabilities-test",
                runtimeState =
                    HereticRuntimeState.Qualified,
                realizationRef = realization.realizationRef,
                modelRef = model.modelRef,
                modelProfileRef = realization.modelProfileRef,
                artifactRef = realization.artifact.artifactRef,
                artifactContentSetSha256 =
                    realization.artifact.contentSetSha256,
                runtimeAdapterRef = realization.runtime.runtimeAdapterRef,
                runtimeClass = realization.runtime.runtimeClass,
                endpoint = realization.runtime.endpoint,
                requestModel = realization.runtime.requestModel,
                qualification =
                    HereticRuntimeQualification.qualified(),
            )

        val held =
            VexLifeEffectiveModelCapabilities.project(
                modelRef = model.modelRef,
                profile = model.companionProfile,
                realization = realization,
                qualifiedRuntime = runtime,
            )
        assertEquals(
            VexLifeEffectiveModelCapabilities.Standing.HELD,
            held.visualInput,
        )

        val visual =
            VexLifeVisualInputQualification.QualifiedRuntimeEvidence(
                evidenceRef =
                    "evidence.vexlife-prod.visual.capabilities-test.001",
                modelRef = model.modelRef,
                artifactRef = realization.artifact.artifactRef,
                artifactContentSetSha256 =
                    realization.artifact.contentSetSha256,
                environmentFingerprintSha256 =
                    "d".repeat(
                        64,
                    ),
            )
        val available =
            VexLifeEffectiveModelCapabilities.project(
                modelRef = model.modelRef,
                profile = model.companionProfile,
                realization = realization,
                qualifiedRuntime = runtime,
                qualifiedVisualInput = visual,
            )
        assertEquals(
            VexLifeEffectiveModelCapabilities.Standing.AVAILABLE,
            available.visualInput,
        )

        val notServing =
            VexLifeEffectiveModelCapabilities.project(
                modelRef = model.modelRef,
                profile = model.companionProfile,
                realization = realization,
                qualifiedRuntime = null,
                qualifiedVisualInput = visual,
            )
        assertEquals(
            VexLifeEffectiveModelCapabilities.Standing.HELD,
            notServing.visualInput,
        )
    }
}

// [VXG RealForever]
