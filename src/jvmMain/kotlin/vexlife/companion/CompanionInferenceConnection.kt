package vexlife.companion

import java.time.Instant
import kotlinx.serialization.Serializable
import vexlife.capability.VexLifeCapabilityAtlas
import vexlife.model.VexLifeModelSourceProjection
import vexlife.model.heretic.HereticModelCatalog
import vexlife.model.heretic.HereticModelConfiguration
import vexlife.partner.PartnerCapabilityExchange

private val CompanionInferenceSha256 =
    Regex(
        "[0-9a-f]{64}",
    )

/**
 * Durable provider-neutral observation for one normalized inference result.
 *
 * This value is public because canonical turn Results and Product evidence records expose it. The
 * inference implementation seam itself remains internal.
 */
@Serializable
data class CompanionInferenceObservation(
    val sourceKind: VexLifeModelSourceProjection.SourceKind,
    val providerRef: String?,
    val qualificationRef: String,
    val endpointOrigin: String,
    val modelRef: String,
    val profileRef: String,
    val observedAt: String,
    val responseBodySha256: String,
    val contentSha256: String,
    val contentCharacters: Int,
    val finishReasonOrNull: String?,
    val rawProviderResponsePersisted: Boolean,
    val rawReasoningPersisted: Boolean,
    val observationRef: String,
    val semanticFingerprint: String,
) {
    init {
        require(
            qualificationRef.isNotBlank() &&
                endpointOrigin.isNotBlank() &&
                modelRef.isNotBlank() &&
                profileRef.isNotBlank() &&
                observationRef.isNotBlank() &&
                semanticFingerprint.matches(CompanionInferenceSha256) &&
                responseBodySha256.matches(CompanionInferenceSha256) &&
                contentSha256.matches(CompanionInferenceSha256) &&
                contentCharacters >= 0 &&
                !rawProviderResponsePersisted &&
                !rawReasoningPersisted,
        ) {
            "Companion inference observation requires bounded normalized non-secret evidence"
        }
        when (sourceKind) {
            VexLifeModelSourceProjection.SourceKind.LOCAL ->
                require(providerRef == null) {
                    "Local inference observation may not invent an external provider"
                }

            VexLifeModelSourceProjection.SourceKind.PARTNER ->
                require(!providerRef.isNullOrBlank()) {
                    "Partner inference observation requires its exact provider"
                }
        }
    }
}

/**
 * Durable provider-neutral evidence for one physical provider stage.
 *
 * `loopbackOnly` is an observed fact: Local must be true and Partner must be false.
 */
@Serializable
data class CompanionInferenceInvocationEvidence(
    val sourceKind: VexLifeModelSourceProjection.SourceKind,
    val providerRef: String?,
    val qualificationRef: String,
    val endpointOrigin: String,
    val requestBodySha256: String,
    val responseBodySha256: String,
    val observationRef: String,
    val observationFingerprint: String,
    val httpStatus: Int,
    val actualHttpCall: Boolean,
    val loopbackOnly: Boolean,
    val providerRequestId: String? = null,
    val providerCallRef: String? = null,
    val formedAt: String,
    val invocationRef: String,
    val semanticFingerprint: String,
) {
    init {
        require(
            qualificationRef.isNotBlank() &&
                endpointOrigin.isNotBlank() &&
                requestBodySha256.matches(CompanionInferenceSha256) &&
                responseBodySha256.matches(CompanionInferenceSha256) &&
                observationRef.isNotBlank() &&
                observationFingerprint.matches(CompanionInferenceSha256) &&
                httpStatus in 100..599 &&
                actualHttpCall &&
                invocationRef.isNotBlank() &&
                semanticFingerprint.matches(CompanionInferenceSha256),
        ) {
            "Companion inference invocation evidence is incomplete"
        }
        when (sourceKind) {
            VexLifeModelSourceProjection.SourceKind.LOCAL ->
                require(providerRef == null && loopbackOnly) {
                    "Local inference invocation must remain exact loopback evidence"
                }

            VexLifeModelSourceProjection.SourceKind.PARTNER ->
                require(!providerRef.isNullOrBlank() && !loopbackOnly) {
                    "Partner inference invocation must remain exact external-provider evidence"
                }
        }
    }
}

/**
 * Provider-neutral invocation boundary for one already-qualified Companion inference realization.
 *
 * Turn semantics stay above this boundary: Conversation/Memory/context selection, capability and
 * source-return admission, request budgets, canonical response commit and retry policy are not
 * realization concerns.
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
        val observation: CompanionInferenceObservation,
        val invocationEvidence: CompanionInferenceInvocationEvidence,
        val providerRequestBody: String,
        val measurement: DirectCompanionConnection.StageMeasurement,
        /**
         * Provider-native call identity awaiting one Vex-owned capability result.
         *
         * The pending value grants no effect authority. It exists only so the next provider stage
         * can preserve the exact provider call reference after the rightful capability bridge acts.
         */
        val pendingPartnerCapability: PartnerCapabilityExchange.Pending? = null,
        /** Exact legacy Local evidence retained only while canonical Local turn context uses it. */
        val localRuntimeObservation: DirectCompanionConnection.RuntimeObservation? = null,
        /** Exact legacy Local invocation evidence retained only while canonical Local Context uses it. */
        val localInvocationEvidence: DirectCompanionConnection.InvocationEvidence? = null,
    ) {
        init {
            require(
                invocationEvidence.sourceKind == observation.sourceKind &&
                    invocationEvidence.providerRef == observation.providerRef &&
                    invocationEvidence.qualificationRef == observation.qualificationRef &&
                    invocationEvidence.observationRef == observation.observationRef &&
                    invocationEvidence.observationFingerprint == observation.semanticFingerprint &&
                    invocationEvidence.responseBodySha256 == observation.responseBodySha256,
            ) {
                "Companion inference result evidence does not bind one exact observation"
            }

            when (observation.sourceKind) {
                VexLifeModelSourceProjection.SourceKind.LOCAL ->
                    require(
                        localRuntimeObservation != null &&
                            localInvocationEvidence != null &&
                            pendingPartnerCapability == null,
                    ) {
                        "Local inference result requires its exact legacy Local evidence only"
                    }

                VexLifeModelSourceProjection.SourceKind.PARTNER ->
                    require(
                        localRuntimeObservation == null &&
                            localInvocationEvidence == null,
                    ) {
                        "Partner inference result may not carry Local runtime evidence"
                    }
            }
        }

        fun requireLocalRuntimeObservation(): DirectCompanionConnection.RuntimeObservation =
            requireNotNull(localRuntimeObservation) {
                "canonical Local turn context requires Local runtime observation evidence"
            }

        fun requireLocalInvocationEvidence(): DirectCompanionConnection.InvocationEvidence =
            requireNotNull(localInvocationEvidence) {
                "canonical Local turn context requires Local invocation evidence"
            }
    }

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
                    observation =
                        CompanionInferenceObservation(
                            sourceKind = VexLifeModelSourceProjection.SourceKind.LOCAL,
                            providerRef = null,
                            qualificationRef = runtime.runtimeReceiptRef,
                            endpointOrigin = it.runtimeObservation.endpointOrigin,
                            modelRef = it.runtimeObservation.modelRef,
                            profileRef = it.runtimeObservation.modelProfileRef,
                            observedAt = it.runtimeObservation.observedAt,
                            responseBodySha256 = it.runtimeObservation.responseBodySha256,
                            contentSha256 = it.runtimeObservation.contentSha256,
                            contentCharacters = it.runtimeObservation.contentCharacters,
                            finishReasonOrNull = it.runtimeObservation.finishReasonOrNull,
                            rawProviderResponsePersisted =
                                it.runtimeObservation.rawProviderResponsePersisted,
                            rawReasoningPersisted =
                                it.runtimeObservation.reasoningTrace.rawPersisted,
                            observationRef = it.runtimeObservation.observationRef,
                            semanticFingerprint = it.runtimeObservation.semanticFingerprint,
                        ),
                    invocationEvidence =
                        CompanionInferenceInvocationEvidence(
                            sourceKind = VexLifeModelSourceProjection.SourceKind.LOCAL,
                            providerRef = null,
                            qualificationRef = runtime.runtimeReceiptRef,
                            endpointOrigin = it.invocationEvidence.endpointOrigin,
                            requestBodySha256 = it.invocationEvidence.requestBodySha256,
                            responseBodySha256 = it.invocationEvidence.responseBodySha256,
                            observationRef = it.invocationEvidence.runtimeObservationRef,
                            observationFingerprint =
                                it.invocationEvidence.runtimeObservationFingerprint,
                            httpStatus = it.invocationEvidence.httpStatus,
                            actualHttpCall = it.invocationEvidence.actualHttpCall,
                            loopbackOnly = it.invocationEvidence.loopbackOnly,
                            formedAt = it.invocationEvidence.formedAt,
                            invocationRef = it.invocationEvidence.invocationRef,
                            semanticFingerprint = it.invocationEvidence.semanticFingerprint,
                        ),
                    providerRequestBody = it.providerRequestBody,
                    measurement = it.measurement,
                    localRuntimeObservation = it.runtimeObservation,
                    localInvocationEvidence = it.invocationEvidence,
                )
            }
}

// [VXG RealForever]
