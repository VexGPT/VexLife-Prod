package vexlife.companion

import java.time.Instant
import vexlife.capability.VexLifeCapabilityAtlas
import vexlife.model.heretic.HereticModelCatalog
import vexlife.model.heretic.HereticModelConfiguration

/**
 * Provider-neutral invocation boundary for one already-qualified Companion inference realization.
 *
 * Turn semantics stay above this boundary: Conversation/Memory/context selection, capability and
 * source-return admission, request budgets, canonical response commit and retry policy are not
 * realization concerns.
 *
 * The first realization is deliberately an extraction only: [LocalHereticInference] delegates to
 * the existing [DirectCompanionConnection] unchanged. A Partner realization must satisfy this same
 * stage contract rather than bypassing the Vex-owned turn loop.
 */
internal interface CompanionInferenceConnection {
    data class Request(
        val messages: List<CompanionProviderMessage>,
        val capabilityOffer: List<VexLifeCapabilityAtlas.Definition>,
        val conversationCacheBoundaryMessageCount: Int?,
        val progressObserver: DirectCompanionConnection.ProgressObserver?,
        val cancellation: DirectCompanionCancellation?,
    )

    data class Result(
        val content: String,
        val model: String,
        val sealedHumanReasoning: String?,
        val runtimeObservation: DirectCompanionConnection.RuntimeObservation,
        val invocationEvidence: DirectCompanionConnection.InvocationEvidence,
        val providerRequestBody: String,
        val measurement: DirectCompanionConnection.StageMeasurement,
    )

    fun invoke(
        request: Request,
    ): Result
}

/**
 * Exact Local Heretic realization of [CompanionInferenceConnection].
 *
 * Runtime/model currentness is decided by the caller's rightful owners before construction. This
 * adapter owns no runtime lifecycle and performs no extra inference; it only delegates the one
 * admitted provider stage to the existing Local Connection.
 */
internal class LocalHereticInference(
    private val runtime: HereticModelConfiguration.QualifiedRuntimeEvidence,
    private val model: HereticModelCatalog.Entry,
    private val transport: DirectCompanionConnection.Transport,
    private val now: () -> Instant,
    private val monotonicNanos: () -> Long = System::nanoTime,
) : CompanionInferenceConnection {
    override fun invoke(
        request: CompanionInferenceConnection.Request,
    ): CompanionInferenceConnection.Result =
        DirectCompanionConnection
            .invoke(
                runtime = runtime,
                messages = request.messages,
                transport = transport,
                progressObserver = request.progressObserver,
                now = now,
                monotonicNanos = monotonicNanos,
                capabilityOffer = request.capabilityOffer,
                model = model,
                conversationCacheBoundaryMessageCount =
                    request.conversationCacheBoundaryMessageCount,
                cancellation = request.cancellation,
            )
            .let {
                CompanionInferenceConnection.Result(
                    content = it.content,
                    model = it.model,
                    sealedHumanReasoning = it.sealedHumanReasoning,
                    runtimeObservation = it.runtimeObservation,
                    invocationEvidence = it.invocationEvidence,
                    providerRequestBody = it.providerRequestBody,
                    measurement = it.measurement,
                )
            }
}

// [VXG RealForever]
