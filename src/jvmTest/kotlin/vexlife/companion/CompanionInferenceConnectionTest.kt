package vexlife.companion

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import vexlife.model.VexLifeModelSourceProjection
import vexlife.model.heretic.HereticModelCatalog
import vexlife.model.heretic.HereticModelConfiguration
import vexlife.model.heretic.HereticRuntimeQualification
import vexlife.model.heretic.HereticRuntimeRealization
import vexlife.model.heretic.HereticRuntimeState

class CompanionInferenceConnectionTest {
    private fun qualifiedRuntime(): HereticModelConfiguration.QualifiedRuntimeEvidence {
        val model =
            HereticModelCatalog.Models.Qwen4B
        val realization =
            HereticRuntimeRealization.require(
                modelRef = model.modelRef,
                target = HereticRuntimeRealization.MacArm64,
            )
        return HereticModelConfiguration.QualifiedRuntimeEvidence(
            runtimeReceiptRef =
                "receipt.vexlife-prod.inference-connection-parity",
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
            contextWindowTokens =
                realization.runtime.livedLaunchProfile
                    ?.contextTokens,
        )
    }

    private fun monotonic(): () -> Long {
        var value =
            1_000_000_000L
        return {
            value.also {
                value +=
                    125_000_000L
            }
        }
    }

    @Test
    fun localExtractionPreservesExactRequestResultEvidenceAndConversationCacheBoundary() {
        val runtime =
            qualifiedRuntime()
        val model =
            HereticModelCatalog.Models.Qwen4B
        val messages =
            listOf(
                CompanionProviderMessage(
                    role = "system",
                    content = "Stable system",
                ),
                CompanionProviderMessage(
                    role = "user",
                    content = "Prior request",
                ),
                CompanionProviderMessage(
                    role = "assistant",
                    content = "Prior response",
                ),
                CompanionProviderMessage(
                    role = "user",
                    content = "Current request",
                ),
            )
        val now = {
            Instant.parse(
                "2026-11-24T20:00:00Z",
            )
        }
        val body =
            """
            {
              "model": "default_model",
              "choices": [
                {
                  "finish_reason": "stop",
                  "message": {
                    "role": "assistant",
                    "content": "Current response"
                  }
                }
              ]
            }
            """.trimIndent()

        var directCall: DirectCompanionConnection.HttpCall? =
            null
        val direct =
            DirectCompanionConnection.invoke(
                runtime = runtime,
                model = model,
                messages = messages,
                conversationCacheBoundaryMessageCount = 3,
                transport =
                    DirectCompanionConnection.Transport {
                        observed ->
                        directCall =
                            observed
                        DirectCompanionConnection.HttpResult(
                            status = 200,
                            body = body,
                        )
                    },
                now = now,
                monotonicNanos = monotonic(),
            )

        var seamCall: DirectCompanionConnection.HttpCall? =
            null
        val seam =
            LocalHereticInference(
                runtime = runtime,
                model = model,
                transport =
                    DirectCompanionConnection.Transport {
                        observed ->
                        seamCall =
                            observed
                        DirectCompanionConnection.HttpResult(
                            status = 200,
                            body = body,
                        )
                    },
                now = now,
                monotonicNanos = monotonic(),
            )
                .invoke(
                    CompanionInferenceConnection.Request(
                        messages = messages,
                        capabilityOffer = emptyList(),
                        conversationCacheBoundaryMessageCount = 3,
                        progressObserver = null,
                        cancellation = null,
                    ),
                )

        assertEquals(
            requireNotNull(
                directCall,
            )
                .uri,
            requireNotNull(
                seamCall,
            )
                .uri,
        )
        assertEquals(
            requireNotNull(
                directCall,
            )
                .body,
            requireNotNull(
                seamCall,
            )
                .body,
        )
        assertEquals(
            direct.content,
            seam.content,
        )
        assertEquals(
            direct.model,
            seam.model,
        )
        assertEquals(
            direct.sealedHumanReasoning,
            seam.sealedHumanReasoning,
        )
        assertEquals(
            direct.runtimeObservation,
            seam.localRuntimeObservation,
        )
        assertEquals(
            direct.invocationEvidence,
            seam.localInvocationEvidence,
        )
        assertEquals(
            VexLifeModelSourceProjection.SourceKind.LOCAL,
            seam.observation.sourceKind,
        )
        assertEquals(
            direct.runtimeObservation.observationRef,
            seam.observation.observationRef,
        )
        assertEquals(
            direct.runtimeObservation.semanticFingerprint,
            seam.observation.semanticFingerprint,
        )
        assertEquals(
            direct.invocationEvidence.requestBodySha256,
            seam.invocationEvidence.requestBodySha256,
        )
        assertEquals(
            direct.invocationEvidence.invocationRef,
            seam.invocationEvidence.invocationRef,
        )
        assertEquals(
            true,
            seam.invocationEvidence.loopbackOnly,
        )
        assertEquals(
            direct.providerRequestBody,
            seam.providerRequestBody,
        )
        assertEquals(
            direct.measurement,
            seam.measurement,
        )
    }
}

// [VXG RealForever]
