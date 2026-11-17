package vexlife.model.heretic

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HereticRuntimeRealizationTest {
    private val scope =
        HereticModelConfiguration.BindingScope(
            homeRef = "vex-home.test.runtime-realization",
            companionLineageRef = "companion-lineage.test.runtime-realization",
        )

    @Test
    fun everyCurrentCatalogModelHasDeliberateMacCoverage() {
        HereticModelCatalog.Entries.forEach { model ->
            val realization =
                HereticRuntimeRealization.require(
                    modelRef = model.modelRef,
                    target = HereticRuntimeRealization.MacArm64,
                )

            assertEquals(model.modelRef, realization.modelRef)
            assertEquals(model.identity.modelProfileRef, realization.modelProfileRef)
            assertEquals(
                HereticRuntimeRealization.MacArm64,
                realization.target,
            )
        }
    }

    @Test
    fun windows4BResolvesByExactModelAndTarget() {
        val model = HereticModelCatalog.Candidate4B
        val realization =
            HereticRuntimeRealization.require(
                modelRef = model.modelRef,
                target = HereticRuntimeRealization.WindowsX64,
            )

        assertEquals(HereticRuntimeRealization.Windows4B.realizationRef, realization.realizationRef)
        assertEquals(model.modelRef, realization.modelRef)
        assertEquals(
            "model-Q4_K_M.gguf",
            realization.artifact.primaryFilename,
        )
        assertEquals(
            1,
            realization.qualification.maxTokens,
        )
        assertEquals(
            0.0,
            realization.qualification.temperature,
        )
        assertEquals(
            false,
            realization.qualification.enableThinking,
        )
    }

    @Test
    fun MlxAndLlamaCppShareOneQualificationSequenceContract() {
        val mac =
            HereticRuntimeRealization.require(
                modelRef = HereticModelCatalog.Candidate4B.modelRef,
                target = HereticRuntimeRealization.MacArm64,
            )
        val windows =
            HereticRuntimeRealization.Windows4B

        assertEquals(
            mac.qualification.healthPath,
            windows.qualification.healthPath,
        )
        assertEquals(
            mac.qualification.modelsPath,
            windows.qualification.modelsPath,
        )
        assertEquals(
            mac.qualification.completionPath,
            windows.qualification.completionPath,
        )
        assertEquals(
            mac.qualification.maxTokens,
            windows.qualification.maxTokens,
        )
        assertEquals(
            mac.qualification.temperature,
            windows.qualification.temperature,
        )
        assertEquals(
            mac.qualification.enableThinking,
            windows.qualification.enableThinking,
        )
        assertEquals(
            mac.qualification.stream,
            windows.qualification.stream,
        )
        assertEquals(
            HereticRuntimeRealization.ModelExposure.EXACT_MATERIALIZATION_PATH,
            mac.qualification.modelExposure,
        )
        assertEquals(
            HereticRuntimeRealization.ModelExposure.EXACT_REQUEST_MODEL_ALIAS,
            windows.qualification.modelExposure,
        )
    }

    @Test
    fun WindowsNativeRuntimeMakesPythonEnvironmentExplicitlyNotRequired() {
        val environment =
            HereticRuntimeRealization.Windows4B.dependencies.environment

        assertIs<HereticRuntimeRealization.Requirement.NotRequired>(environment)
    }

    @Test
    fun unsupportedModelTargetPairIsAnExplicitAbsence() {
        assertNull(
            HereticRuntimeRealization.resolve(
                modelRef = HereticModelCatalog.Candidate9B.modelRef,
                target = HereticRuntimeRealization.WindowsX64,
            ),
        )
    }

    @Test
    fun existingMacRealizationRemainsAdmittedWithoutBehaviorChange() {
        val model = HereticModelCatalog.Candidate4B
        val realization =
            HereticRuntimeRealization.require(
                modelRef = model.modelRef,
                target = HereticRuntimeRealization.MacArm64,
            )
        val artifact = realization.artifact
        val runtime = realization.runtime

        val evidence =
            HereticModelConfiguration.QualifiedRuntimeEvidence(
                realizationRef = realization.realizationRef,
                runtimeReceiptRef = "receipt.test.mac.4b",
                runtimeState = HereticRuntimeState.Qualified,
                modelRef = model.modelRef,
                modelProfileRef = model.identity.modelProfileRef,
                artifactRef = artifact.artifactRef,
                artifactContentSetSha256 = artifact.contentSetSha256,
                runtimeAdapterRef = runtime.runtimeAdapterRef,
                runtimeClass = runtime.runtimeClass,
                endpoint = runtime.endpoint,
                requestModel = runtime.requestModel,
                qualification = HereticRuntimeQualification.qualified(),
            )

        val bound =
            HereticModelConfiguration.bound(
                homeIdentity = scope,
                qualifiedRuntime = evidence,
                runtimeReceiptPath = realization.runtimeReceiptRelativePath,
            )

        assertTrue(HereticModelConfiguration.productIdentityCurrent(bound))
        assertEquals(realization.realizationRef, bound.realizationRef)
    }

    @Test
    fun currentHeretic4BMayBindThroughWindowsRealization() {
        val model = HereticModelCatalog.Candidate4B
        val realization = HereticRuntimeRealization.Windows4B
        val artifact = realization.artifact
        val runtime = realization.runtime

        val evidence =
            HereticModelConfiguration.QualifiedRuntimeEvidence(
                realizationRef = realization.realizationRef,
                runtimeReceiptRef = "receipt.test.windows.4b",
                runtimeState = HereticRuntimeState.Qualified,
                modelRef = model.modelRef,
                modelProfileRef = model.identity.modelProfileRef,
                artifactRef = artifact.artifactRef,
                artifactContentSetSha256 = artifact.contentSetSha256,
                runtimeAdapterRef = runtime.runtimeAdapterRef,
                runtimeClass = runtime.runtimeClass,
                endpoint = runtime.endpoint,
                requestModel = runtime.requestModel,
                qualification = HereticRuntimeQualification.qualified(),
            )

        val bound =
            HereticModelConfiguration.bound(
                homeIdentity = scope,
                qualifiedRuntime = evidence,
                runtimeReceiptPath = realization.runtimeReceiptRelativePath,
            )

        assertEquals(model.modelRef, bound.modelRef)
        assertEquals(realization.realizationRef, bound.realizationRef)
        assertEquals(artifact.artifactRef, bound.artifactRef)
        assertEquals(runtime.runtimeAdapterRef, bound.runtimeAdapterRef)
        assertTrue(HereticModelConfiguration.productIdentityCurrent(bound))
    }

    @Test
    fun windows4BRealizationCannotBeAttachedToAnotherOfferedModel() {
        val model = HereticModelCatalog.Candidate9B
        val realization = HereticRuntimeRealization.Windows4B
        val artifact = realization.artifact
        val runtime = realization.runtime

        assertFailsWith<IllegalArgumentException> {
            HereticModelConfiguration.QualifiedRuntimeEvidence(
                realizationRef = realization.realizationRef,
                runtimeReceiptRef = "receipt.test.windows.wrong-model",
                runtimeState = HereticRuntimeState.Qualified,
                modelRef = model.modelRef,
                modelProfileRef = model.identity.modelProfileRef,
                artifactRef = artifact.artifactRef,
                artifactContentSetSha256 = artifact.contentSetSha256,
                runtimeAdapterRef = runtime.runtimeAdapterRef,
                runtimeClass = runtime.runtimeClass,
                endpoint = runtime.endpoint,
                requestModel = runtime.requestModel,
                qualification = HereticRuntimeQualification.qualified(),
            )
        }
    }
}

// [VXG RealForever]
