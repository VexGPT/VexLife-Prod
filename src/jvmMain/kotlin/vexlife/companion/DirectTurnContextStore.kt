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
        val runtimeObservation: DirectCompanionConnection.RuntimeObservation,
        val invocationEvidence: DirectCompanionConnection.InvocationEvidence,
        val sourceReturnTrace: DirectConversationSourceReturn.Trace?,
        val capabilityTrace: VexLifeCapabilityBridge.Trace? = null,
        val rawProviderResponsePersisted: Boolean,
        val rawReasoningPersisted: Boolean,
        val rawReportedModelPersisted: Boolean,
        val privacyClass: String,
        val formedAt: String,
    )

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

    fun record(
        request: DirectConversationStore.Event,
        response: DirectConversationStore.Event,
        promptContext: DirectPromptContext.Receipt,
        runtimeObservation: DirectCompanionConnection.RuntimeObservation,
        invocationEvidence: DirectCompanionConnection.InvocationEvidence,
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

        val evidence =
            requireNotNull(
                VexDurableContractFile.read<RuntimeEvidence>(
                    ownedRoot = productHome,
                    path =
                        runtimeEvidencePath(
                            turnRef,
                        ),
                ),
            ) {
                "product-created canonical turn context is missing its runtime evidence sidecar"
            }
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

        return Record(
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
            runtimeObservation = evidence.runtimeObservation,
            invocationEvidence = evidence.invocationEvidence,
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

    private fun formRuntimeEvidence(
        request: DirectConversationStore.Event,
        response: DirectConversationStore.Event,
        promptContext: DirectPromptContext.Receipt,
        runtimeObservation: DirectCompanionConnection.RuntimeObservation,
        invocationEvidence: DirectCompanionConnection.InvocationEvidence,
        sourceReturnTrace: DirectConversationSourceReturn.Trace?,
        capabilityTrace: VexLifeCapabilityBridge.Trace?,
        formedAt: Instant,
    ): RuntimeEvidence {
        val core =
            RuntimeEvidenceCore(
                schemaVersion = if (capabilityTrace != null) RuntimeEvidenceSchemaV3 else RuntimeEvidenceSchema,
                truthClass = "PRODUCT_RUNTIME_TURN_EVIDENCE",
                turnRef = request.turnRef,
                requestEventRef = request.eventRef,
                requestEventHash = request.eventHash,
                responseEventRef = response.eventRef,
                responseEventHash = response.eventHash,
                promptContext = promptContext,
                runtimeObservation = runtimeObservation,
                invocationEvidence = invocationEvidence,
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

    private fun validateRuntimeEvidence(
        value: RuntimeEvidence,
    ) {
        require(
            value.schemaVersion in
                setOf(
                    RuntimeEvidenceSchemaV1,
                    RuntimeEvidenceSchema,
                    RuntimeEvidenceSchemaV3,
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
                    value.sourceReturnTrace ==
                    null
                ) ||
                (
                    value.schemaVersion ==
                        RuntimeEvidenceSchema &&
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
                    value.capabilityTrace != null &&
                    value.promptContext.capabilityExchangeAllowanceTokens > 0 &&
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

    private fun canonicalContextSourceRefs(
        promptContext: DirectPromptContext.Receipt,
        runtimeEvidenceRef: String,
        requestEventRef: String,
        responseEventRef: String,
    ): List<String> {
        val preceding =
            (
                promptContext.selectedConversationEventRefs +
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
