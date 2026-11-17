package vexlife.model.heretic

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class HereticRuntimeRealizationTest {
    private val scope =
        HereticModelConfiguration.BindingScope(
            homeRef = "vex-home.test.windows",
            companionLineageRef = "companion-lineage.test.windows",
        )

    @Test
    fun existingMacRealizationRemainsAdmittedWithoutBehaviorChange() {
        val model =
            HereticModelCatalog.Candidate4B
        val identity =
            model.identity

        val evidence =
            HereticModelConfiguration.QualifiedRuntimeEvidence(
                runtimeReceiptRef = "receipt.test.mac.4b",
                runtimeState = HereticRuntimeState.Qualified,
                modelRef = model.modelRef,
                modelProfileRef = identity.modelProfileRef,
                artifactRef = identity.artifactSetRef,
                artifactContentSetSha256 = identity.artifactContentSetSha256,
                runtimeAdapterRef = identity.runtimeAdapterRef,
                runtimeClass = identity.runtimeClass,
                endpoint = identity.endpoint,
                requestModel = identity.requestModel,
                qualification = HereticRuntimeQualification.qualified(),
            )

        val bound =
            HereticModelConfiguration.bound(
                homeIdentity = scope,
                qualifiedRuntime = evidence,
            )

        assertTrue(
            HereticModelConfiguration.productIdentityCurrent(
                bound,
            ),
        )
        assertEquals(
            identity.runtimeAdapterRef,
            bound.runtimeAdapterRef,
        )
    }

    @Test
    fun currentHeretic4BMayBindThroughWindowsPatientZeroRealization() {
        val model =
            HereticModelCatalog.Candidate4B
        val realization =
            HereticRuntimeRealization.Windows4BPatientZero

        val evidence =
            HereticModelConfiguration.QualifiedRuntimeEvidence(
                runtimeReceiptRef = "receipt.test.windows.4b",
                runtimeState = HereticRuntimeState.Qualified,
                modelRef = model.modelRef,
                modelProfileRef = model.identity.modelProfileRef,
                artifactRef = realization.artifactRef,
                artifactContentSetSha256 = realization.artifactContentSetSha256,
                runtimeAdapterRef = realization.runtimeAdapterRef,
                runtimeClass = realization.runtimeClass,
                endpoint = realization.endpoint,
                requestModel = realization.requestModel,
                qualification = HereticRuntimeQualification.qualified(),
            )

        val bound =
            HereticModelConfiguration.bound(
                homeIdentity = scope,
                qualifiedRuntime = evidence,
            )

        assertEquals(
            model.modelRef,
            bound.modelRef,
        )
        assertEquals(
            realization.artifactRef,
            bound.artifactRef,
        )
        assertEquals(
            realization.runtimeAdapterRef,
            bound.runtimeAdapterRef,
        )
        assertEquals(
            realization.runtimeClass,
            bound.runtimeClass,
        )
        assertEquals(
            realization.endpoint,
            bound.endpoint,
        )
        assertEquals(
            realization.requestModel,
            bound.requestModel,
        )
        assertTrue(
            HereticModelConfiguration.productIdentityCurrent(
                bound,
            ),
        )
    }

    @Test
    fun windows4BRealizationCannotBeAttachedToAnotherOfferedModel() {
        val model =
            HereticModelCatalog.Candidate9B
        val realization =
            HereticRuntimeRealization.Windows4BPatientZero

        assertFailsWith<IllegalArgumentException> {
            HereticModelConfiguration.QualifiedRuntimeEvidence(
                runtimeReceiptRef = "receipt.test.windows.wrong-model",
                runtimeState = HereticRuntimeState.Qualified,
                modelRef = model.modelRef,
                modelProfileRef = model.identity.modelProfileRef,
                artifactRef = realization.artifactRef,
                artifactContentSetSha256 = realization.artifactContentSetSha256,
                runtimeAdapterRef = realization.runtimeAdapterRef,
                runtimeClass = realization.runtimeClass,
                endpoint = realization.endpoint,
                requestModel = realization.requestModel,
                qualification = HereticRuntimeQualification.qualified(),
            )
        }
    }
}

// [VXG RealForever]
