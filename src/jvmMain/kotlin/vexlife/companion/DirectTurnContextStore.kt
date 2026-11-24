package vexlife.companion

import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.security.MessageDigest
import java.time.Instant
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import vexlife.capability.VexLifeCapabilityBridge
import vexlife.contract.VexContractJson
import vexlife.contract.VexDurableContractFile
import vexlife.conversation.DirectConversationIdentity
import vexlife.conversation.DirectConversationStore
import vexlife.conversation.VexLifeConversationContract
import vexlife.filesystem.VexOwnedPath
import vexlife.home.VexLifeProdHomeFiles
import vexlife.model.VexLifeModelSourceProjection

/**
 * Context/evidence owner for one completed direct Companion turn.
 *
 * The canonical durable context keeps the exact lived VexLife
 * vexlife.lived-companion-context/v1 shape under context/<lineage>/<thread>/.
 *
 * Product-only normalized runtime/invocation evidence remains beneath
 * runtime/vexlife-prod/turn-evidence/. The canonical context references that evidence; it does not
 * absorb product-runtime fields into the canonical Conversation contract.
 */
class DirectTurnContextStore(
    productHome: Path,
) {
    companion object {
        const val Schema =
            VexLifeConversationContract.ContextSchema

        const val RuntimeEvidenceSchemaV1 =
            "vexlife-prod.turn-runtime-evidence/v1"

        const val RuntimeEvidenceSchema =
            "vexlife-prod.turn-runtime-evidence/v2"

        /** v2 plus the capability exchange trace; written only when a turn made requests. */
        const val RuntimeEvidenceSchemaV3 =
            "vexlife-prod.turn-runtime-evidence/v3"

        /** Current evidence: prior truth plus one bounded final provider-stage measurement. */
        const val RuntimeEvidenceSchemaV4 =
            "vexlife-prod.turn-runtime-evidence/v4"


        /** Provider-neutral evidence for one deterministic external Partner text turn. */
        const val ProviderRuntimeEvidenceSchemaV5 =
            "vexlife-prod.turn-provider-evidence/v5"
    }

    @OptIn(
        ExperimentalSerializationApi::class,
    )
    @Serializable
    private data class RuntimeEvidenceCore(
        val schemaVersion: String,
        val truthClass: String,
        val turnRef: String,
        val requestEventRef: String,
        val requestEventHash: String,
        val responseEventRef: String,
        val responseEventHash: String,
        val promptContext: DirectPromptContext.Receipt,
        val runtimeObservation: DirectCompanionConnection.RuntimeObservation,
        val invocationEvidence: DirectCompanionConnection.InvocationEvidence,
        @EncodeDefault(
            EncodeDefault.Mode.NEVER,
        )
        val providerMeasurement: ProviderMeasurement? =
            null,
        @EncodeDefault(
            EncodeDefault.Mode.NEVER,
        )
        val sourceReturnTrace: DirectConversationSourceReturn.Trace? =
            null,
        @EncodeDefault(
            EncodeDefault.Mode.NEVER,
        )
        val capabilityTrace: VexLifeCapabilityBridge.Trace? =
            null,
        val rawProviderResponsePersisted: Boolean,
        val rawReasoningPersisted: Boolean,
        val rawReportedModelPersisted: Boolean,
        val formedAt: String,
    )

    @OptIn(
        ExperimentalSerializationApi::class,
    )
    @Serializable
    private data class RuntimeEvidence(
        val schemaVersion: String,
        val truthClass: String,
        val turnRef: String,
        val requestEventRef: String,
        val requestEventHash: String,
        val responseEventRef: String,
        val responseEventHash: String,
        val promptContext: DirectPromptContext.Receipt,
        val runtimeObservation: DirectCompanionConnection.RuntimeObservation,
        val invocationEvidence: DirectCompanionConnection.InvocationEvidence,
        @EncodeDefault(
            EncodeDefault.Mode.NEVER,
        )
        val providerMeasurement: ProviderMeasurement? =
            null,
        @EncodeDefault(
            EncodeDefault.Mode.NEVER,
        )
        val sourceReturnTrace: DirectConversationSourceReturn.Trace? =
            null,
        @EncodeDefault(
            EncodeDefault.Mode.NEVER,
        )
        val capabilityTrace: VexLifeCapabilityBridge.Trace? =
            null,
        val rawProviderResponsePersisted: Boolean,
        val rawReasoningPersisted: Boolean,
        val rawReportedModelPersisted: Boolean,
        val formedAt: String,
        val evidenceRef: String,
        val evidenceSha256: String,
    )

    @Serializable
    private data class ProviderRuntimeEvidenceCoreV5(
        val schemaVersion: String,
        val truthClass: String,
        val turnRef: String,
        val requestEventRef: String,
        val requestEventHash: String,
        val responseEventRef: String,
        val responseEventHash: String,
        val promptContext: DirectPromptContext.Receipt,
        val providerObservation: CompanionInferenceObservation,
        val providerInvocationEvidence: CompanionInferenceInvocationEvidence,
        val providerMeasurement: ProviderMeasurement,
        val rawProviderResponsePersisted: Boolean,
        val rawReasoningPersisted: Boolean,
        val rawReportedModelPersisted: Boolean,
        val formedAt: String,
    )

    @Serializable
    private data class ProviderRuntimeEvidenceV5(
        val schemaVersion: String,
        val truthClass: String,
        val turnRef: String,
        val requestEventRef: String,
        val requestEventHash: String,
        val responseEventRef: String,
        val responseEventHash: String,
        val promptContext: DirectPromptContext.Receipt,
        val providerObservation: CompanionInferenceObservation,
        val providerInvocationEvidence: CompanionInferenceInvocationEvidence,
        val providerMeasurement: ProviderMeasurement,
        val rawProviderResponsePersisted: Boolean,
        val rawReasoningPersisted: Boolean,
        val rawReportedModelPersisted: Boolean,
        val formedAt: String,
        val evidenceRef: String,
        val evidenceSha256: String,
    )

    @Serializable
    data class ProviderMeasurement(
        val stop: String,
        val finishReason: String?,
        val promptTokens: Int?,
        val cachedPromptTokens: Int?,
        val prefillTokens: Int?,
        val completionTokens: Int?,
        val firstTokenMillis: Long?,
        val firstAnswerMillis: Long?,
        val totalMillis: Long,
    )

    data class PromptReceiptCompatibility(
        val runtimeEvidenceSchema: String,
        val promptReceiptSchema: String,
        val readableByCurrentSource: Boolean,
    )

    @ConsistentCopyVisibility
    data class Record internal constructor(
        val schemaVersion: String,
        val contextPath: String,
        val contextSha256: String,
        val contextSourceRefs: List<String>,
        val homeRef: String,
        val deviceRef: String,
        val companionLineageRef: String,
        val threadRef: String,
        val turnRef: String,
        val requestEventRef: String,
        val requestEventSha256: String,
        val responseEventRef: String,
        val responseEventSha256: String,
        val runtimeEvidenceRef: String,
        val promptContext: DirectPromptContext.Receipt,
        val providerObservation: CompanionInferenceObservation,
        val providerInvocationEvidence: CompanionInferenceInvocationEvidence,
        val localRuntimeObservation: DirectCompanionConnection.RuntimeObservation?,
        val localInvocationEvidence: DirectCompanionConnection.InvocationEvidence?,
        val providerMeasurement: ProviderMeasurement?,
        val sourceReturnTrace: DirectConversationSourceReturn.Trace?,
        val capabilityTrace: VexLifeCapabilityBridge.Trace? = null,
        val rawProviderResponsePersisted: Boolean,
        val rawReasoningPersisted: Boolean,
        val rawReportedModelPersisted: Boolean,
        val privacyClass: String,
        val formedAt: String,
    ) {
        /** Source-compatible Local view for legacy callers; Partner records intentionally have none. */
        val runtimeObservation: DirectCompanionConnection.RuntimeObservation
            get() =
                requireNotNull(
                    localRuntimeObservation,
                ) {
                    "Partner turn evidence has no Local runtime observation"
                }

        /** Source-compatible Local view for legacy callers; Partner records intentionally have none. */
        val invocationEvidence: DirectCompanionConnection.InvocationEvidence
            get() =
                requireNotNull(
                    localInvocationEvidence,
                ) {
                    "Partner turn evidence has no Local invocation evidence"
                }
    }

    private val productHome =
        VexOwnedPath.requireRoot(
            productHome,
            "canonical VexLife Home",
        )

    private val home =
        requireNotNull(
            VexLifeProdHomeFiles.observeExisting(
                this.productHome,
            ),
        ) {
            "direct turn context requires one current canonical VexLife Home"
        }

    private val manifest =
        home.manifest

    private val contextDirectory =
        VexOwnedPath.resolveInside(
            this.productHome,
            "context/" +
                manifest.currentCompanionLineageRef +
                "/" +
                DirectConversationIdentity.ThreadRef,
        )

    private val runtimeEvidenceDirectory =
        VexOwnedPath.resolveInside(
            this.productHome,
            "runtime/vexlife-prod/turn-evidence",
        )

    /**
     * Inspect only the durable envelope needed to decide whether this source generation may decode
     * the nested prompt receipt. This does not reinterpret newer fields, weaken strict JSON, or
     * claim that unsupported evidence is otherwise valid.
     */
    fun inspectPromptReceiptCompatibility(
        turnRef: String,
    ): PromptReceiptCompatibility? {
        requireSafeRef(
            turnRef,
            "turnRef",
        )

        val canonicalText =
            VexDurableContractFile.readText(
                ownedRoot = productHome,
                path =
                    pathFor(
                        turnRef,
                    ),
            )
                ?: return null
        val canonical =
            decodeAndValidateCanonicalContext(
                canonicalText,
            )
        val productRuntimeEvidenceRefs =
            canonical.contextSourceRefs
                .filter {
                    it.startsWith(
                        "evidence.vexlife-prod.turn-runtime.",
                    )
                }
        if (
            productRuntimeEvidenceRefs.isEmpty()
        ) {
            return null
        }
        require(
            productRuntimeEvidenceRefs.size ==
                1
        ) {
            "product-created canonical turn context must reference exactly one runtime evidence sidecar"
        }
        val expectedEvidenceRef =
            productRuntimeEvidenceRefs.single()
        val text =
            requireNotNull(
                VexDurableContractFile.readText(
                    ownedRoot = productHome,
                    path =
                        runtimeEvidencePath(
                            turnRef,
                        ),
                ),
            ) {
                "product-created canonical turn context is missing its runtime evidence sidecar"
            }
        val objectValue =
            VexContractJson.Strict
                .parseToJsonElement(
                    text,
                )
                .jsonObject
        require(
            objectValue[
                "turnRef"
            ]
                ?.jsonPrimitive
                ?.content ==
                canonical.turnRef &&
                objectValue[
                    "evidenceRef"
                ]
                    ?.jsonPrimitive
                    ?.content ==
                expectedEvidenceRef,
        ) {
            "product runtime evidence envelope does not bind the canonical turn context"
        }
        val runtimeEvidenceSchema =
            requireNotNull(
                objectValue[
                    "schemaVersion"
                ]
                    ?.jsonPrimitive
                    ?.content,
            ) {
                "product runtime turn evidence has no schemaVersion"
            }
        val promptObject =
            objectValue[
                "promptContext"
            ] as? JsonObject
                ?: error(
                    "product runtime turn evidence has no promptContext object",
                )
        val promptReceiptSchema =
            requireNotNull(
                promptObject[
                    "schemaVersion"
                ]
                    ?.jsonPrimitive
                    ?.content,
            ) {
                "product runtime turn evidence promptContext has no schemaVersion"
            }

        return PromptReceiptCompatibility(
            runtimeEvidenceSchema = runtimeEvidenceSchema,
            promptReceiptSchema = promptReceiptSchema,
            readableByCurrentSource =
                promptReceiptSchema in
                    setOf(
                        DirectPromptContext.ReceiptSchemaV2,
                        DirectPromptContext.ReceiptSchemaV3,
                        DirectPromptContext.ReceiptSchemaV4,
                        DirectPromptContext.ReceiptSchemaV5,
                        DirectPromptContext.ReceiptSchemaV6,
                        DirectPromptContext.ReceiptSchemaV7,
                        DirectPromptContext.ReceiptSchemaV8,
                        DirectPromptContext.ReceiptSchemaV9,
                        DirectPromptContext.ReceiptSchema,
                    ),
        )
    }

    fun record(
        request: DirectConversationStore.Event,
        response: DirectConversationStore.Event,
        promptContext: DirectPromptContext.Receipt,
        runtimeObservation: DirectCompanionConnection.RuntimeObservation,
        invocationEvidence: DirectCompanionConnection.InvocationEvidence,
        providerMeasurement: DirectCompanionConnection.StageMeasurement? =
            null,
        sourceReturnTrace: DirectConversationSourceReturn.Trace? =
            null,
        capabilityTrace: VexLifeCapabilityBridge.Trace? = null,
        formedAt: Instant = Instant.now(),
    ): Record {
        validateTurnInputs(
            request = request,
            response = response,
            promptContext = promptContext,
            runtimeObservation = runtimeObservation,
            invocationEvidence = invocationEvidence,
        )
        requireSourceReturnTrace(
            sourceReturnTrace = sourceReturnTrace,
            finalPrompt = promptContext,
        )

        val evidence =
            formRuntimeEvidence(
                request = request,
                response = response,
                promptContext = promptContext,
                runtimeObservation = runtimeObservation,
                invocationEvidence = invocationEvidence,
                providerMeasurement = providerMeasurement,
                sourceReturnTrace = sourceReturnTrace,
                capabilityTrace = capabilityTrace,
                formedAt = formedAt,
            )
        writeRuntimeEvidence(
            evidence,
        )

        val sourceRefs =
            canonicalContextSourceRefs(
                promptContext = promptContext,
                runtimeEvidenceRef = evidence.evidenceRef,
                requestEventRef = request.eventRef,
                responseEventRef = response.eventRef,
            )
        val promptJson =
            VexContractJson.Strict
                .parseToJsonElement(
                    VexContractJson.compactEncode(
                        promptContext,
                    ),
                )
                .jsonObject
        val core =
            VexLifeConversationContract.ContextCore(
                schemaVersion = VexLifeConversationContract.ContextSchema,
                homeRef = manifest.homeRef,
                deviceRef = manifest.currentDeviceRef,
                companionLineageRef = manifest.currentCompanionLineageRef,
                instanceRef = request.instanceRef,
                threadRef = request.threadRef,
                turnRef = request.turnRef,
                contextSourceRefs = sourceRefs,
                requestEventHash = request.eventHash,
                responseEventHash = response.eventHash,
                promptContextMaterializationReceipt = promptJson,
                privacyClass = VexLifeConversationContract.PrivacyClass,
                formedAt = formedAt.toString(),
            )
                .let(
                    VexLifeConversationContract::validateContextCore,
                )
        val hash =
            semanticHash(
                core,
            )
        val canonical =
            VexLifeConversationContract.Context(
                schemaVersion = core.schemaVersion,
                homeRef = core.homeRef,
                deviceRef = core.deviceRef,
                companionLineageRef = core.companionLineageRef,
                instanceRef = core.instanceRef,
                threadRef = core.threadRef,
                turnRef = core.turnRef,
                contextSourceRefs = core.contextSourceRefs,
                requestEventHash = core.requestEventHash,
                responseEventHash = core.responseEventHash,
                promptContextMaterializationReceipt = core.promptContextMaterializationReceipt,
                privacyClass = core.privacyClass,
                formedAt = core.formedAt,
                serializedContextSha256 = hash,
            )
                .let(
                    VexLifeConversationContract::validateContext,
                )

        VexOwnedPath.ensureDirectory(
            productHome,
            contextDirectory,
        )
        val contextPath =
            pathFor(
                request.turnRef,
            )
        val existingText =
            VexDurableContractFile.readText(
                ownedRoot = productHome,
                path = contextPath,
            )

        if (
            existingText ==
                null
        ) {
            VexDurableContractFile.write(
                ownedRoot = productHome,
                path = contextPath,
                value = canonical,
            )
        } else {
            val existing =
                decodeAndValidateCanonicalContext(
                    existingText,
                )
            require(
                existing ==
                    canonical,
            ) {
                "canonical turn context address already contains different evidence"
            }
        }

        return requireNotNull(
            read(
                request.turnRef,
            ),
        )
    }

    fun recordPartnerText(
        request: DirectConversationStore.Event,
        response: DirectConversationStore.Event,
        promptContext: DirectPromptContext.Receipt,
        providerObservation: CompanionInferenceObservation,
        providerInvocationEvidence: CompanionInferenceInvocationEvidence,
        providerMeasurement: DirectCompanionConnection.StageMeasurement,
        formedAt: Instant = Instant.now(),
    ): Record {
        validatePartnerTurnInputs(
            request = request,
            response = response,
            promptContext = promptContext,
            providerObservation = providerObservation,
            providerInvocationEvidence = providerInvocationEvidence,
        )

        val evidence =
            formProviderRuntimeEvidenceV5(
                request = request,
                response = response,
                promptContext = promptContext,
                providerObservation = providerObservation,
                providerInvocationEvidence = providerInvocationEvidence,
                providerMeasurement = providerMeasurement,
                formedAt = formedAt,
            )
        writeProviderRuntimeEvidenceV5(
            evidence,
        )

        val sourceRefs =
            canonicalContextSourceRefs(
                promptContext = promptContext,
                runtimeEvidenceRef = evidence.evidenceRef,
                requestEventRef = request.eventRef,
                responseEventRef = response.eventRef,
            )
        val promptJson =
            VexContractJson.Strict
                .parseToJsonElement(
                    VexContractJson.compactEncode(
                        promptContext,
                    ),
                )
                .jsonObject
        val core =
            VexLifeConversationContract.ContextCore(
                schemaVersion = VexLifeConversationContract.ContextSchema,
                homeRef = manifest.homeRef,
                deviceRef = manifest.currentDeviceRef,
                companionLineageRef = manifest.currentCompanionLineageRef,
                instanceRef = request.instanceRef,
                threadRef = request.threadRef,
                turnRef = request.turnRef,
                contextSourceRefs = sourceRefs,
                requestEventHash = request.eventHash,
                responseEventHash = response.eventHash,
                promptContextMaterializationReceipt = promptJson,
                privacyClass = VexLifeConversationContract.PrivacyClass,
                formedAt = formedAt.toString(),
            )
                .let(
                    VexLifeConversationContract::validateContextCore,
                )
        val hash =
            semanticHash(
                core,
            )
        val canonical =
            VexLifeConversationContract.Context(
                schemaVersion = core.schemaVersion,
                homeRef = core.homeRef,
                deviceRef = core.deviceRef,
                companionLineageRef = core.companionLineageRef,
                instanceRef = core.instanceRef,
                threadRef = core.threadRef,
                turnRef = core.turnRef,
                contextSourceRefs = core.contextSourceRefs,
                requestEventHash = core.requestEventHash,
                responseEventHash = core.responseEventHash,
                promptContextMaterializationReceipt = core.promptContextMaterializationReceipt,
                privacyClass = core.privacyClass,
                formedAt = core.formedAt,
                serializedContextSha256 = hash,
            )
                .let(
                    VexLifeConversationContract::validateContext,
                )

        VexOwnedPath.ensureDirectory(
            productHome,
            contextDirectory,
        )
        val contextPath =
            pathFor(
                request.turnRef,
            )
        val existingText =
            VexDurableContractFile.readText(
                ownedRoot = productHome,
                path = contextPath,
            )
        if (
            existingText ==
                null
        ) {
            VexDurableContractFile.write(
                ownedRoot = productHome,
                path = contextPath,
                value = canonical,
            )
        } else {
            val existing =
                decodeAndValidateCanonicalContext(
                    existingText,
                )
            require(
                existing ==
                    canonical,
            ) {
                "canonical turn context address already contains different evidence"
            }
        }

        return requireNotNull(
            read(
                request.turnRef,
            ),
        )
    }

    fun read(
        turnRef: String,
    ): Record? {
        requireSafeRef(
            turnRef,
            "turnRef",
        )

        if (
            VexOwnedPath.observeDirectory(
                productHome,
                contextDirectory,
            ) ==
            VexOwnedPath.DirectoryState.Missing
        ) {
            return null
        }

        val canonicalPath =
            pathFor(
                turnRef,
            )
        val canonicalText =
            VexDurableContractFile.readText(
                ownedRoot = productHome,
                path = canonicalPath,
            )
                ?: return null
        val canonical =
            decodeAndValidateCanonicalContext(
                canonicalText,
            )
        val productRuntimeEvidenceRefs =
            canonical.contextSourceRefs
                .filter {
                    it.startsWith(
                        "evidence.vexlife-prod.turn-runtime.",
                    )
                }
        if (
            productRuntimeEvidenceRefs.isEmpty()
        ) {
            return null
        }
        require(
            productRuntimeEvidenceRefs.size ==
                1
        ) {
            "product-created canonical turn context must reference exactly one runtime evidence sidecar"
        }

        val evidenceText =
            requireNotNull(
                VexDurableContractFile.readText(
                    ownedRoot = productHome,
                    path =
                        runtimeEvidencePath(
                            turnRef,
                        ),
                ),
            ) {
                "product-created canonical turn context is missing its runtime evidence sidecar"
            }
        val evidenceSchema =
            requireNotNull(
                VexContractJson.Strict
                    .parseToJsonElement(
                        evidenceText,
                    )
                    .jsonObject[
                        "schemaVersion"
                    ]
                    ?.jsonPrimitive
                    ?.content,
            ) {
                "product runtime evidence sidecar has no schemaVersion"
            }

        return if (
            evidenceSchema ==
                ProviderRuntimeEvidenceSchemaV5
        ) {
            val evidence =
                VexContractJson.decode<ProviderRuntimeEvidenceV5>(
                    evidenceText,
                )
            validateProviderRuntimeEvidenceV5(
                evidence,
            )
            require(
                evidence.turnRef ==
                    canonical.turnRef &&
                    evidence.requestEventHash ==
                    canonical.requestEventHash &&
                    evidence.responseEventHash ==
                    canonical.responseEventHash &&
                    evidence.evidenceRef ==
                    productRuntimeEvidenceRefs.single() &&
                    canonical.contextSourceRefs.contains(
                        evidence.evidenceRef,
                    ) &&
                    canonical.promptContextMaterializationReceipt ==
                    VexContractJson.Strict
                        .parseToJsonElement(
                            VexContractJson.compactEncode(
                                evidence.promptContext,
                            ),
                        )
                        .jsonObject,
            ) {
                "canonical turn context and Partner provider evidence do not bind the same turn"
            }

            Record(
                schemaVersion = canonical.schemaVersion,
                contextPath =
                    relativeContextPath(
                        turnRef,
                    ),
                contextSha256 = canonical.serializedContextSha256,
                contextSourceRefs = canonical.contextSourceRefs,
                homeRef = canonical.homeRef,
                deviceRef = canonical.deviceRef,
                companionLineageRef = canonical.companionLineageRef,
                threadRef = canonical.threadRef,
                turnRef = canonical.turnRef,
                requestEventRef = evidence.requestEventRef,
                requestEventSha256 = evidence.requestEventHash,
                responseEventRef = evidence.responseEventRef,
                responseEventSha256 = evidence.responseEventHash,
                runtimeEvidenceRef = evidence.evidenceRef,
                promptContext = evidence.promptContext,
                providerObservation = evidence.providerObservation,
                providerInvocationEvidence = evidence.providerInvocationEvidence,
                localRuntimeObservation = null,
                localInvocationEvidence = null,
                providerMeasurement = evidence.providerMeasurement,
                sourceReturnTrace = null,
                capabilityTrace = null,
                rawProviderResponsePersisted = evidence.rawProviderResponsePersisted,
                rawReasoningPersisted = evidence.rawReasoningPersisted,
                rawReportedModelPersisted = evidence.rawReportedModelPersisted,
                privacyClass = canonical.privacyClass,
                formedAt = canonical.formedAt,
            )
        } else {
            val evidence =
                VexContractJson.decode<RuntimeEvidence>(
                    evidenceText,
                )
            validateRuntimeEvidence(
                evidence,
            )
            require(
                evidence.turnRef ==
                    canonical.turnRef &&
                    evidence.requestEventHash ==
                    canonical.requestEventHash &&
                    evidence.responseEventHash ==
                    canonical.responseEventHash &&
                    evidence.evidenceRef ==
                    productRuntimeEvidenceRefs.single() &&
                    canonical.contextSourceRefs.contains(
                        evidence.evidenceRef,
                    ) &&
                    canonical.promptContextMaterializationReceipt ==
                    VexContractJson.Strict
                        .parseToJsonElement(
                            VexContractJson.compactEncode(
                                evidence.promptContext,
                            ),
                        )
                        .jsonObject,
            ) {
                "canonical turn context and product runtime evidence do not bind the same turn"
            }

            Record(
                schemaVersion = canonical.schemaVersion,
                contextPath =
                    relativeContextPath(
                        turnRef,
                    ),
                contextSha256 = canonical.serializedContextSha256,
                contextSourceRefs = canonical.contextSourceRefs,
                homeRef = canonical.homeRef,
                deviceRef = canonical.deviceRef,
                companionLineageRef = canonical.companionLineageRef,
                threadRef = canonical.threadRef,
                turnRef = canonical.turnRef,
                requestEventRef = evidence.requestEventRef,
                requestEventSha256 = evidence.requestEventHash,
                responseEventRef = evidence.responseEventRef,
                responseEventSha256 = evidence.responseEventHash,
                runtimeEvidenceRef = evidence.evidenceRef,
                promptContext = evidence.promptContext,
                providerObservation =
                    localProviderObservation(
                        evidence.runtimeObservation,
                    ),
                providerInvocationEvidence =
                    localProviderInvocationEvidence(
                        evidence.invocationEvidence,
                    ),
                localRuntimeObservation = evidence.runtimeObservation,
                localInvocationEvidence = evidence.invocationEvidence,
                providerMeasurement = evidence.providerMeasurement,
                sourceReturnTrace = evidence.sourceReturnTrace,
                capabilityTrace = evidence.capabilityTrace,
                rawProviderResponsePersisted = evidence.rawProviderResponsePersisted,
                rawReasoningPersisted = evidence.rawReasoningPersisted,
                rawReportedModelPersisted = evidence.rawReportedModelPersisted,
                privacyClass = canonical.privacyClass,
                formedAt = canonical.formedAt,
            )
        }

    private fun validateTurnInputs(
        request: DirectConversationStore.Event,
        response: DirectConversationStore.Event,
        promptContext: DirectPromptContext.Receipt,
        runtimeObservation: DirectCompanionConnection.RuntimeObservation,
        invocationEvidence: DirectCompanionConnection.InvocationEvidence,
    ) {
        require(
            request.eventKind ==
                VexLifeConversationContract.RequestKind &&
                response.eventKind ==
                VexLifeConversationContract.ResponseKind &&
                request.turnRef ==
                response.turnRef &&
                request.instanceRef ==
                response.instanceRef &&
                response.priorEventHash ==
                request.eventHash,
        ) {
            "direct turn context requires one exact canonical request/response pair"
        }
        require(
            request.homeRef ==
                manifest.homeRef &&
                response.homeRef ==
                manifest.homeRef &&
                request.deviceRef ==
                manifest.currentDeviceRef &&
                response.deviceRef ==
                manifest.currentDeviceRef &&
                request.companionLineageRef ==
                manifest.currentCompanionLineageRef &&
                response.companionLineageRef ==
                manifest.currentCompanionLineageRef &&
                request.threadRef ==
                DirectConversationIdentity.ThreadRef &&
                response.threadRef ==
                DirectConversationIdentity.ThreadRef,
        ) {
            "direct turn context requires the admitted canonical Home/thread identity"
        }
        require(
            promptContext.currentRequestEventRef ==
                request.eventRef &&
                promptContext.currentRequestEventSha256 ==
                request.eventHash,
        ) {
            "direct turn context prompt receipt does not bind the exact current request"
        }
        require(
            invocationEvidence.runtimeObservationRef ==
                runtimeObservation.observationRef &&
                invocationEvidence.runtimeObservationFingerprint ==
                runtimeObservation.semanticFingerprint &&
                invocationEvidence.responseBodySha256 ==
                runtimeObservation.responseBodySha256 &&
                invocationEvidence.actualHttpCall &&
                invocationEvidence.loopbackOnly,
        ) {
            "direct turn context provider evidence does not bind the exact runtime observation"
        }
        require(
            !runtimeObservation.rawProviderResponsePersisted &&
                !runtimeObservation.reportedModelField.rawValuePersisted &&
                !runtimeObservation.reasoningTrace.rawPersisted,
        ) {
            "direct turn context may persist normalized provider evidence only"
        }
    }

    private fun requireSourceReturnTrace(
        sourceReturnTrace: DirectConversationSourceReturn.Trace?,
        finalPrompt: DirectPromptContext.Receipt,
    ): Boolean {
        if (
            sourceReturnTrace ==
                null
        ) {
            require(
                finalPrompt.sourceReturnState ==
                    null
            ) {
                "source-return final prompt requires one compact source-return trace"
            }
            return true
        }

        require(
            finalPrompt.sourceReturnState !=
                null &&
                sourceReturnTrace ==
                DirectConversationSourceReturn.formTrace(
                    attempt = sourceReturnTrace.attempt,
                    finalPrompt = finalPrompt,
                ) &&
                sourceReturnTrace.attempt.sourceReturnState ==
                finalPrompt.sourceReturnState &&
                sourceReturnTrace.attempt.returnedSourceEventRefs ==
                finalPrompt.sourceReturnSourceEventRefs &&
                sourceReturnTrace.attempt.sourceReturnSystemMessageSha256 ==
                finalPrompt.sourceReturnSystemMessageSha256,
        ) {
            "source-return trace does not bind the exact final prompt/source projection"
        }
        return true
    }

    private fun validatePartnerTurnInputs(
        request: DirectConversationStore.Event,
        response: DirectConversationStore.Event,
        promptContext: DirectPromptContext.Receipt,
        providerObservation: CompanionInferenceObservation,
        providerInvocationEvidence: CompanionInferenceInvocationEvidence,
    ) {
        require(
            request.schemaVersion ==
                VexLifeConversationContract.EventSchemaV2 &&
                response.schemaVersion ==
                VexLifeConversationContract.EventSchemaV3 &&
                request.eventKind ==
                VexLifeConversationContract.RequestKind &&
                response.eventKind ==
                VexLifeConversationContract.ResponseKind &&
                request.turnRef ==
                response.turnRef &&
                request.instanceRef ==
                response.instanceRef &&
                response.priorEventHash ==
                request.eventHash,
        ) {
            "Partner turn context requires one canonical v2 request/v3 response pair"
        }
        require(
            request.homeRef ==
                manifest.homeRef &&
                response.homeRef ==
                manifest.homeRef &&
                request.deviceRef ==
                manifest.currentDeviceRef &&
                response.deviceRef ==
                manifest.currentDeviceRef &&
                request.companionLineageRef ==
                manifest.currentCompanionLineageRef &&
                response.companionLineageRef ==
                manifest.currentCompanionLineageRef &&
                request.threadRef ==
                DirectConversationIdentity.ThreadRef &&
                response.threadRef ==
                DirectConversationIdentity.ThreadRef,
        ) {
            "Partner turn context requires the admitted canonical Home/thread identity"
        }
        require(
            promptContext.currentRequestEventRef ==
                request.eventRef &&
                promptContext.currentRequestEventSha256 ==
                request.eventHash,
        ) {
            "Partner turn context prompt receipt does not bind the exact current request"
        }
        require(
            providerObservation.sourceKind ==
                VexLifeModelSourceProjection.SourceKind.PARTNER &&
                providerInvocationEvidence.sourceKind ==
                VexLifeModelSourceProjection.SourceKind.PARTNER &&
                providerObservation.providerRef ==
                response.providerRef &&
                providerObservation.profileRef ==
                response.endpointProfileRef &&
                providerObservation.endpointOrigin ==
                response.sanitizedEndpointOrigin &&
                providerObservation.modelRef ==
                response.model &&
                providerInvocationEvidence.providerRef ==
                providerObservation.providerRef &&
                providerInvocationEvidence.qualificationRef ==
                providerObservation.qualificationRef &&
                providerInvocationEvidence.observationRef ==
                providerObservation.observationRef &&
                providerInvocationEvidence.observationFingerprint ==
                providerObservation.semanticFingerprint &&
                providerInvocationEvidence.responseBodySha256 ==
                providerObservation.responseBodySha256 &&
                providerInvocationEvidence.actualHttpCall &&
                !providerInvocationEvidence.loopbackOnly,
        ) {
            "Partner turn context provider evidence does not bind the exact external observation"
        }
        require(
            !providerObservation.rawProviderResponsePersisted &&
                !providerObservation.rawReasoningPersisted,
        ) {
            "Partner turn context may persist normalized provider evidence only"
        }
    }

    private fun formProviderRuntimeEvidenceV5(
        request: DirectConversationStore.Event,
        response: DirectConversationStore.Event,
        promptContext: DirectPromptContext.Receipt,
        providerObservation: CompanionInferenceObservation,
        providerInvocationEvidence: CompanionInferenceInvocationEvidence,
        providerMeasurement: DirectCompanionConnection.StageMeasurement,
        formedAt: Instant,
    ): ProviderRuntimeEvidenceV5 {
        val core =
            ProviderRuntimeEvidenceCoreV5(
                schemaVersion = ProviderRuntimeEvidenceSchemaV5,
                truthClass = "PRODUCT_PROVIDER_TURN_EVIDENCE",
                turnRef = request.turnRef,
                requestEventRef = request.eventRef,
                requestEventHash = request.eventHash,
                responseEventRef = response.eventRef,
                responseEventHash = response.eventHash,
                promptContext = promptContext,
                providerObservation = providerObservation,
                providerInvocationEvidence = providerInvocationEvidence,
                providerMeasurement =
                    normalizeProviderMeasurement(
                        providerMeasurement,
                    ),
                rawProviderResponsePersisted = false,
                rawReasoningPersisted = false,
                rawReportedModelPersisted = false,
                formedAt = formedAt.toString(),
            )
        val hash =
            semanticHash(
                core,
            )
        return ProviderRuntimeEvidenceV5(
            schemaVersion = core.schemaVersion,
            truthClass = core.truthClass,
            turnRef = core.turnRef,
            requestEventRef = core.requestEventRef,
            requestEventHash = core.requestEventHash,
            responseEventRef = core.responseEventRef,
            responseEventHash = core.responseEventHash,
            promptContext = core.promptContext,
            providerObservation = core.providerObservation,
            providerInvocationEvidence = core.providerInvocationEvidence,
            providerMeasurement = core.providerMeasurement,
            rawProviderResponsePersisted = core.rawProviderResponsePersisted,
            rawReasoningPersisted = core.rawReasoningPersisted,
            rawReportedModelPersisted = core.rawReportedModelPersisted,
            formedAt = core.formedAt,
            evidenceRef =
                "evidence.vexlife-prod.turn-runtime." +
                    hash.take(
                        32,
                    ),
            evidenceSha256 = hash,
        )
            .also(
                ::validateProviderRuntimeEvidenceV5,
            )
    }

    private fun validateProviderRuntimeEvidenceV5(
        value: ProviderRuntimeEvidenceV5,
    ) {
        require(
            value.schemaVersion ==
                ProviderRuntimeEvidenceSchemaV5 &&
                value.truthClass ==
                "PRODUCT_PROVIDER_TURN_EVIDENCE" &&
                value.providerObservation.sourceKind ==
                VexLifeModelSourceProjection.SourceKind.PARTNER &&
                value.providerInvocationEvidence.sourceKind ==
                VexLifeModelSourceProjection.SourceKind.PARTNER &&
                value.providerInvocationEvidence.observationRef ==
                value.providerObservation.observationRef &&
                value.providerInvocationEvidence.observationFingerprint ==
                value.providerObservation.semanticFingerprint &&
                value.providerInvocationEvidence.responseBodySha256 ==
                value.providerObservation.responseBodySha256 &&
                value.providerInvocationEvidence.actualHttpCall &&
                !value.providerInvocationEvidence.loopbackOnly &&
                !value.rawProviderResponsePersisted &&
                !value.rawReasoningPersisted &&
                !value.rawReportedModelPersisted,
        ) {
            "Partner provider turn evidence identity/privacy is invalid"
        }
        requireSafeRef(
            value.turnRef,
            "Partner provider evidence turnRef",
        )
        validateProviderMeasurement(
            value.providerMeasurement,
        )
        val core =
            ProviderRuntimeEvidenceCoreV5(
                schemaVersion = value.schemaVersion,
                truthClass = value.truthClass,
                turnRef = value.turnRef,
                requestEventRef = value.requestEventRef,
                requestEventHash = value.requestEventHash,
                responseEventRef = value.responseEventRef,
                responseEventHash = value.responseEventHash,
                promptContext = value.promptContext,
                providerObservation = value.providerObservation,
                providerInvocationEvidence = value.providerInvocationEvidence,
                providerMeasurement = value.providerMeasurement,
                rawProviderResponsePersisted = value.rawProviderResponsePersisted,
                rawReasoningPersisted = value.rawReasoningPersisted,
                rawReportedModelPersisted = value.rawReportedModelPersisted,
                formedAt = value.formedAt,
            )
        require(
            semanticHash(
                core,
            ) ==
                value.evidenceSha256 &&
                value.evidenceRef ==
                "evidence.vexlife-prod.turn-runtime." +
                value.evidenceSha256.take(
                    32,
                ),
        ) {
            "Partner provider turn evidence fingerprint is invalid"
        }
    }

    private fun localProviderObservation(
        runtimeObservation: DirectCompanionConnection.RuntimeObservation,
    ): CompanionInferenceObservation =
        CompanionInferenceObservation(
            sourceKind = VexLifeModelSourceProjection.SourceKind.LOCAL,
            providerRef = null,
            qualificationRef = runtimeObservation.runtimeReceiptRef,
            endpointOrigin = runtimeObservation.endpointOrigin,
            modelRef = runtimeObservation.modelRef,
            profileRef = runtimeObservation.modelProfileRef,
            observedAt = runtimeObservation.observedAt,
            responseBodySha256 = runtimeObservation.responseBodySha256,
            contentSha256 = runtimeObservation.contentSha256,
            contentCharacters = runtimeObservation.contentCharacters,
            finishReasonOrNull = runtimeObservation.finishReasonOrNull,
            rawProviderResponsePersisted = runtimeObservation.rawProviderResponsePersisted,
            rawReasoningPersisted = runtimeObservation.reasoningTrace.rawPersisted,
            observationRef = runtimeObservation.observationRef,
            semanticFingerprint = runtimeObservation.semanticFingerprint,
        )

    private fun localProviderInvocationEvidence(
        invocationEvidence: DirectCompanionConnection.InvocationEvidence,
    ): CompanionInferenceInvocationEvidence =
        CompanionInferenceInvocationEvidence(
            sourceKind = VexLifeModelSourceProjection.SourceKind.LOCAL,
            providerRef = null,
            qualificationRef = invocationEvidence.runtimeReceiptRef,
            endpointOrigin = invocationEvidence.endpointOrigin,
            requestBodySha256 = invocationEvidence.requestBodySha256,
            responseBodySha256 = invocationEvidence.responseBodySha256,
            observationRef = invocationEvidence.runtimeObservationRef,
            observationFingerprint = invocationEvidence.runtimeObservationFingerprint,
            httpStatus = invocationEvidence.httpStatus,
            actualHttpCall = invocationEvidence.actualHttpCall,
            loopbackOnly = invocationEvidence.loopbackOnly,
            formedAt = invocationEvidence.formedAt,
            invocationRef = invocationEvidence.invocationRef,
            semanticFingerprint = invocationEvidence.semanticFingerprint,
        )

    private fun formRuntimeEvidence(
        request: DirectConversationStore.Event,
        response: DirectConversationStore.Event,
        promptContext: DirectPromptContext.Receipt,
        runtimeObservation: DirectCompanionConnection.RuntimeObservation,
        invocationEvidence: DirectCompanionConnection.InvocationEvidence,
        providerMeasurement: DirectCompanionConnection.StageMeasurement?,
        sourceReturnTrace: DirectConversationSourceReturn.Trace?,
        capabilityTrace: VexLifeCapabilityBridge.Trace?,
        formedAt: Instant,
    ): RuntimeEvidence {
        val core =
            RuntimeEvidenceCore(
                schemaVersion =
                    if (providerMeasurement != null) {
                        RuntimeEvidenceSchemaV4
                    } else if (capabilityTrace != null) {
                        RuntimeEvidenceSchemaV3
                    } else {
                        RuntimeEvidenceSchema
                    },
                truthClass = "PRODUCT_RUNTIME_TURN_EVIDENCE",
                turnRef = request.turnRef,
                requestEventRef = request.eventRef,
                requestEventHash = request.eventHash,
                responseEventRef = response.eventRef,
                responseEventHash = response.eventHash,
                promptContext = promptContext,
                runtimeObservation = runtimeObservation,
                invocationEvidence = invocationEvidence,
                providerMeasurement =
                    providerMeasurement
                        ?.let(
                            ::normalizeProviderMeasurement,
                        ),
                sourceReturnTrace = sourceReturnTrace,
                capabilityTrace = capabilityTrace,
                rawProviderResponsePersisted = false,
                rawReasoningPersisted = false,
                rawReportedModelPersisted = false,
                formedAt = formedAt.toString(),
            )
        val hash =
            semanticHash(
                core,
            )

        return RuntimeEvidence(
            schemaVersion = core.schemaVersion,
            truthClass = core.truthClass,
            turnRef = core.turnRef,
            requestEventRef = core.requestEventRef,
            requestEventHash = core.requestEventHash,
            responseEventRef = core.responseEventRef,
            responseEventHash = core.responseEventHash,
            promptContext = core.promptContext,
            runtimeObservation = core.runtimeObservation,
            invocationEvidence = core.invocationEvidence,
            providerMeasurement = core.providerMeasurement,
            sourceReturnTrace = core.sourceReturnTrace,
            capabilityTrace = core.capabilityTrace,
            rawProviderResponsePersisted = core.rawProviderResponsePersisted,
            rawReasoningPersisted = core.rawReasoningPersisted,
            rawReportedModelPersisted = core.rawReportedModelPersisted,
            formedAt = core.formedAt,
            evidenceRef =
                "evidence.vexlife-prod.turn-runtime." +
                    hash.take(
                        32,
                    ),
            evidenceSha256 = hash,
        )
            .also(
                ::validateRuntimeEvidence,
            )
    }

    private fun normalizeProviderMeasurement(
        measurement: DirectCompanionConnection.StageMeasurement,
    ): ProviderMeasurement =
        ProviderMeasurement(
            stop = measurement.stop.wire,
            finishReason = measurement.finishReason,
            promptTokens = measurement.promptTokens,
            cachedPromptTokens = measurement.cachedPromptTokens,
            prefillTokens = measurement.prefillTokens,
            completionTokens = measurement.completionTokens,
            firstTokenMillis = measurement.firstTokenMillis,
            firstAnswerMillis = measurement.firstAnswerMillis,
            totalMillis = measurement.totalMillis,
        )
            .also(
                ::validateProviderMeasurement,
            )

    private fun validateProviderMeasurement(
        value: ProviderMeasurement,
    ) {
        require(
            value.stop in
                DirectCompanionConnection.StageStop.entries
                    .map {
                        it.wire
                    }
                    .toSet() &&
                listOfNotNull(
                    value.promptTokens,
                    value.cachedPromptTokens,
                    value.prefillTokens,
                    value.completionTokens,
                )
                    .all {
                        it >= 0
                    } &&
                listOfNotNull(
                    value.firstTokenMillis,
                    value.firstAnswerMillis,
                    value.totalMillis,
                )
                    .all {
                        it >= 0L
                    } &&
                value.prefillTokens ==
                value.promptTokens
                    ?.let {
                        prompt ->
                        prompt - (value.cachedPromptTokens ?: 0)
                    } &&
                (
                    value.promptTokens == null ||
                        value.cachedPromptTokens == null ||
                        value.cachedPromptTokens <= value.promptTokens
                    )
        ) {
            "product runtime turn provider measurement is invalid"
        }
    }

    private fun validateRuntimeEvidence(
        value: RuntimeEvidence,
    ) {
        require(
            value.schemaVersion in
                setOf(
                    RuntimeEvidenceSchemaV1,
                    RuntimeEvidenceSchema,
                    RuntimeEvidenceSchemaV3,
                    RuntimeEvidenceSchemaV4,
                ) &&
                value.truthClass ==
                "PRODUCT_RUNTIME_TURN_EVIDENCE" &&
                !value.rawProviderResponsePersisted &&
                !value.rawReasoningPersisted &&
                !value.rawReportedModelPersisted,
        ) {
            "product runtime turn evidence identity/privacy is invalid"
        }
        requireSafeRef(
            value.turnRef,
            "runtime evidence turnRef",
        )
        val core =
            RuntimeEvidenceCore(
                schemaVersion = value.schemaVersion,
                truthClass = value.truthClass,
                turnRef = value.turnRef,
                requestEventRef = value.requestEventRef,
                requestEventHash = value.requestEventHash,
                responseEventRef = value.responseEventRef,
                responseEventHash = value.responseEventHash,
                promptContext = value.promptContext,
                runtimeObservation = value.runtimeObservation,
                invocationEvidence = value.invocationEvidence,
                providerMeasurement = value.providerMeasurement,
                sourceReturnTrace = value.sourceReturnTrace,
                capabilityTrace = value.capabilityTrace,
                rawProviderResponsePersisted = value.rawProviderResponsePersisted,
                rawReasoningPersisted = value.rawReasoningPersisted,
                rawReportedModelPersisted = value.rawReportedModelPersisted,
                formedAt = value.formedAt,
            )
        require(
            (
                value.schemaVersion ==
                    RuntimeEvidenceSchemaV1 &&
                    value.providerMeasurement ==
                    null &&
                    value.sourceReturnTrace ==
                    null &&
                    value.capabilityTrace ==
                    null
                ) ||
                (
                    value.schemaVersion ==
                        RuntimeEvidenceSchema &&
                    value.providerMeasurement == null &&
                    value.capabilityTrace == null &&
                    requireSourceReturnTrace(
                        sourceReturnTrace =
                            value.sourceReturnTrace,
                        finalPrompt =
                            value.promptContext,
                    )
                ) ||
                (
                    // v3 exists only because the turn made capability requests: the trace is present,
                    // and the prompt that carried them kept room for them.
                    value.schemaVersion ==
                        RuntimeEvidenceSchemaV3 &&
                    value.providerMeasurement == null &&
                    value.capabilityTrace != null &&
                    value.promptContext.capabilityExchangeAllowanceTokens > 0 &&
                    requireSourceReturnTrace(
                        sourceReturnTrace =
                            value.sourceReturnTrace,
                        finalPrompt =
                            value.promptContext,
                    )
                ) ||
                (
                    value.schemaVersion ==
                        RuntimeEvidenceSchemaV4 &&
                    value.providerMeasurement != null &&
                    (
                        value.capabilityTrace == null ||
                            value.promptContext.capabilityExchangeAllowanceTokens > 0
                        ) &&
                    requireSourceReturnTrace(
                        sourceReturnTrace =
                            value.sourceReturnTrace,
                        finalPrompt =
                            value.promptContext,
                    )
                ),
        ) {
            "product runtime turn evidence source-return trace is invalid for its schema"
        }

        value.providerMeasurement
            ?.let(
                ::validateProviderMeasurement,
            )

        val hash =
            semanticHash(
                core,
            )
        require(
            value.evidenceSha256 ==
                hash &&
                value.evidenceRef ==
                "evidence.vexlife-prod.turn-runtime." +
                    hash.take(
                        32,
                    ) &&
                value.promptContext.currentRequestEventRef ==
                value.requestEventRef &&
                value.promptContext.currentRequestEventSha256 ==
                value.requestEventHash &&
                value.invocationEvidence.runtimeObservationRef ==
                value.runtimeObservation.observationRef &&
                value.invocationEvidence.runtimeObservationFingerprint ==
                value.runtimeObservation.semanticFingerprint &&
                value.invocationEvidence.responseBodySha256 ==
                value.runtimeObservation.responseBodySha256 &&
                value.invocationEvidence.actualHttpCall &&
                value.invocationEvidence.loopbackOnly,
        ) {
            "product runtime turn evidence hash/bindings are invalid"
        }
    }

    private fun decodeAndValidateCanonicalContext(
        text: String,
    ): VexLifeConversationContract.Context {
        val objectValue =
            VexContractJson.Strict
                .parseToJsonElement(
                    text,
                )
                .jsonObject
        val observedHash =
            requireNotNull(
                objectValue[
                    "serializedContextSha256"
                ]
                    ?.jsonPrimitive
                    ?.content,
            ) {
                "canonical turn context has no serializedContextSha256"
            }
        val coreObject =
            JsonObject(
                objectValue.filterKeys {
                    it !=
                        "serializedContextSha256"
                },
            )
        require(
            sha256(
                VexContractJson.canonicalEncodeElement(
                    coreObject,
                ),
            ) ==
                observedHash,
        ) {
            "canonical turn context hash does not match its exact durable JSON preimage"
        }

        val value =
            VexContractJson.decode<VexLifeConversationContract.Context>(
                text,
            )
                .let(
                    VexLifeConversationContract::validateContext,
                )
        require(
            value.serializedContextSha256 ==
                observedHash &&
                value.homeRef ==
                manifest.homeRef &&
                value.deviceRef ==
                manifest.currentDeviceRef &&
                value.companionLineageRef ==
                manifest.currentCompanionLineageRef &&
                value.threadRef ==
                DirectConversationIdentity.ThreadRef,
        ) {
            "canonical turn context identity is invalid"
        }

        return value
    }

    private fun writeRuntimeEvidence(
        value: RuntimeEvidence,
    ) {
        VexOwnedPath.ensureDirectory(
            productHome,
            runtimeEvidenceDirectory,
        )
        val path =
            runtimeEvidencePath(
                value.turnRef,
            )
        val existing =
            VexDurableContractFile.read<RuntimeEvidence>(
                ownedRoot = productHome,
                path = path,
            )

        if (
            existing ==
                null
        ) {
            VexDurableContractFile.write(
                ownedRoot = productHome,
                path = path,
                value = value,
            )
        } else {
            validateRuntimeEvidence(
                existing,
            )
            require(
                existing ==
                    value,
            ) {
                "product runtime turn evidence address already contains different bytes"
            }
        }
    }

    private fun writeProviderRuntimeEvidenceV5(
        value: ProviderRuntimeEvidenceV5,
    ) {
        VexOwnedPath.ensureDirectory(
            productHome,
            runtimeEvidenceDirectory,
        )
        val path =
            runtimeEvidencePath(
                value.turnRef,
            )
        val existingText =
            VexDurableContractFile.readText(
                ownedRoot = productHome,
                path = path,
            )
        if (
            existingText ==
                null
        ) {
            VexDurableContractFile.write(
                ownedRoot = productHome,
                path = path,
                value = value,
            )
        } else {
            val existing =
                VexContractJson.decode<ProviderRuntimeEvidenceV5>(
                    existingText,
                )
            validateProviderRuntimeEvidenceV5(
                existing,
            )
            require(
                existing ==
                    value,
            ) {
                "Partner provider turn evidence address already contains different bytes"
            }
        }
    }

    private fun canonicalContextSourceRefs(
        promptContext: DirectPromptContext.Receipt,
        runtimeEvidenceRef: String,
        requestEventRef: String,
        responseEventRef: String,
    ): List<String> {
        val preceding =
            (
                promptContext.selectedConversationEventRefs +
                    promptContext.conversationContextProjectionSourceEventRefs +
                    promptContext.sourceReturnSourceEventRefs +
                    promptContext.memorySourceRefs +
                    promptContext.workingContinuitySourceRefs +
                    promptContext.capabilityFrameSourceRefs +
                    listOfNotNull(
                        promptContext.successorPriorResponseEventRef,
                        promptContext.memoryAvailabilityContextRef,
                        promptContext.workingContinuityContextRef,
                        promptContext.workingContinuityCheckpointRef,
                        promptContext.workingContinuitySuccessorClaimRef,
                        promptContext.capabilityFrameRef,
                        promptContext.foundationalField?.bodyRef,
                    ) +
                    listOf(
                        promptContext.receiptRef,
                        runtimeEvidenceRef,
                    )
            )
                .filter {
                    it !=
                        requestEventRef &&
                        it !=
                        responseEventRef
                }
                .distinct()
                .sorted()

        return preceding +
            listOf(
                requestEventRef,
                responseEventRef,
            )
    }

    private fun pathFor(
        turnRef: String,
    ): Path =
        VexOwnedPath.resolveInside(
            productHome,
            relativeContextPath(
                turnRef,
            ),
        )

    private fun relativeContextPath(
        turnRef: String,
    ): String {
        requireSafeRef(
            turnRef,
            "turnRef",
        )
        return "context/" +
            manifest.currentCompanionLineageRef +
            "/" +
            DirectConversationIdentity.ThreadRef +
            "/" +
            turnRef +
            ".json"
    }

    private fun runtimeEvidencePath(
        turnRef: String,
    ): Path {
        requireSafeRef(
            turnRef,
            "turnRef",
        )
        return VexOwnedPath.resolveInside(
            runtimeEvidenceDirectory,
            "$turnRef.json",
        )
    }

    private inline fun <reified T> semanticHash(
        value: T,
    ): String =
        sha256(
            VexContractJson.canonicalEncode(
                value,
            ),
        )

    private fun requireSafeRef(
        value: String,
        label: String,
    ) {
        require(
            value.matches(
                Regex(
                    "[a-z0-9](?:[a-z0-9._-]{0,126}[a-z0-9])?",
                ),
            ),
        ) {
            "$label must be one lowercase portable canonical ref"
        }
    }

    private fun sha256(
        value: String,
    ): String =
        MessageDigest
            .getInstance(
                "SHA-256",
            )
            .digest(
                value.toByteArray(
                    StandardCharsets.UTF_8,
                ),
            )
            .joinToString(
                "",
            ) {
                "%02x".format(
                    it.toInt() and
                        0xff,
                )
            }
}

// [VXG RealForever]
