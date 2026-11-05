package vexlife.companion

import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.security.MessageDigest
import java.time.Instant
import kotlinx.serialization.Serializable
import vexlife.contract.VexContractJson
import vexlife.contract.VexDurableContractFile
import vexlife.conversation.DirectConversationIdentity
import vexlife.conversation.DirectConversationStore
import vexlife.filesystem.VexOwnedPath
import vexlife.home.VexLifeProdHomeFiles

/**
 * Durable context/evidence record for one completed direct Companion turn.
 *
 * This owner binds "what Vex received" to the exact request/response and normalized provider
 * invocation. It persists no raw provider response, raw reasoning or raw reported model path.
 */
class DirectTurnContextStore(
    productHome: Path,
) {
    companion object {
        const val Schema =
            "vexlife-prod.direct-turn-context/v1"
    }

    @Serializable
    private data class RecordCore(
        val schemaVersion: String,
        val homeRef: String,
        val deviceRef: String,
        val companionLineageRef: String,
        val projectRef: String,
        val threadRef: String,
        val channelRef: String,
        val turnRef: String,
        val requestEventRef: String,
        val requestEventSha256: String,
        val responseEventRef: String,
        val responseEventSha256: String,
        val promptContext: DirectPromptContext.Receipt,
        val runtimeObservation: DirectCompanionConnection.RuntimeObservation,
        val invocationEvidence: DirectCompanionConnection.InvocationEvidence,
        val rawProviderResponsePersisted: Boolean,
        val rawReasoningPersisted: Boolean,
        val rawReportedModelPersisted: Boolean,
        val privacyClass: String,
        val formedAt: String,
    )

    @Serializable
    @ConsistentCopyVisibility
    data class Record internal constructor(
        val schemaVersion: String,
        val contextRef: String,
        val contextSha256: String,
        val homeRef: String,
        val deviceRef: String,
        val companionLineageRef: String,
        val projectRef: String,
        val threadRef: String,
        val channelRef: String,
        val turnRef: String,
        val requestEventRef: String,
        val requestEventSha256: String,
        val responseEventRef: String,
        val responseEventSha256: String,
        val promptContext: DirectPromptContext.Receipt,
        val runtimeObservation: DirectCompanionConnection.RuntimeObservation,
        val invocationEvidence: DirectCompanionConnection.InvocationEvidence,
        val rawProviderResponsePersisted: Boolean,
        val rawReasoningPersisted: Boolean,
        val rawReportedModelPersisted: Boolean,
        val privacyClass: String,
        val formedAt: String,
    )

    private val productHome =
        VexOwnedPath.requireRoot(
            productHome,
            "VexLife-Prod Home",
        )

    private val manifest =
        requireNotNull(
            VexLifeProdHomeFiles.observeExisting(
                this.productHome,
            ),
        ) {
            "direct turn context requires one current VexLife-Prod Home"
        }
            .manifest

    private val contextDirectory =
        VexOwnedPath.resolveInside(
            this.productHome,
            "context/" +
                manifest.currentCompanionLineageRef +
                "/" +
                DirectConversationIdentity.ThreadRef,
        )

    fun record(
        request: DirectConversationStore.Event,
        response: DirectConversationStore.Event,
        promptContext: DirectPromptContext.Receipt,
        runtimeObservation: DirectCompanionConnection.RuntimeObservation,
        invocationEvidence: DirectCompanionConnection.InvocationEvidence,
        formedAt: Instant = Instant.now(),
    ): Record {
        require(
            request.eventKind ==
                "REQUEST" &&
                response.eventKind ==
                "RESPONSE" &&
                request.turnRef ==
                response.turnRef &&
                response.priorEventSha256 ==
                request.eventSha256,
        ) {
            "direct turn context requires one exact request/response pair"
        }
        require(
            promptContext.currentRequestEventRef ==
                request.eventRef &&
                promptContext.currentRequestEventSha256 ==
                request.eventSha256,
        ) {
            "direct turn context prompt receipt does not bind the exact current request"
        }
        require(
            response.runtimeObservationRef ==
                runtimeObservation.observationRef &&
                response.invocationEvidenceRef ==
                invocationEvidence.invocationRef &&
                invocationEvidence.runtimeObservationRef ==
                runtimeObservation.observationRef &&
                invocationEvidence.runtimeObservationFingerprint ==
                runtimeObservation.semanticFingerprint &&
                invocationEvidence.responseBodySha256 ==
                runtimeObservation.responseBodySha256 &&
                invocationEvidence.actualHttpCall &&
                invocationEvidence.loopbackOnly,
        ) {
            "direct turn context provider evidence does not bind the exact response event"
        }
        require(
            !runtimeObservation.rawProviderResponsePersisted &&
                !runtimeObservation.reportedModelField.rawValuePersisted &&
                !runtimeObservation.reasoningTrace.rawPersisted,
        ) {
            "direct turn context may persist normalized provider evidence only"
        }

        val core =
            RecordCore(
                schemaVersion = Schema,
                homeRef = manifest.homeRef,
                deviceRef = manifest.currentDeviceRef,
                companionLineageRef = manifest.currentCompanionLineageRef,
                projectRef = DirectConversationIdentity.ProjectRef,
                threadRef = DirectConversationIdentity.ThreadRef,
                channelRef = DirectConversationIdentity.ChannelRef,
                turnRef = request.turnRef,
                requestEventRef = request.eventRef,
                requestEventSha256 = request.eventSha256,
                responseEventRef = response.eventRef,
                responseEventSha256 = response.eventSha256,
                promptContext = promptContext,
                runtimeObservation = runtimeObservation,
                invocationEvidence = invocationEvidence,
                rawProviderResponsePersisted = false,
                rawReasoningPersisted = false,
                rawReportedModelPersisted = false,
                privacyClass = "DEVICE_PRIVATE",
                formedAt = formedAt.toString(),
            )
        val hash =
            sha256(
                VexContractJson.encode(
                    core,
                ),
            )
        val value =
            Record(
                schemaVersion = core.schemaVersion,
                contextRef =
                    "context.vexlife-prod.direct." +
                        hash.take(
                            32,
                        ),
                contextSha256 = hash,
                homeRef = core.homeRef,
                deviceRef = core.deviceRef,
                companionLineageRef = core.companionLineageRef,
                projectRef = core.projectRef,
                threadRef = core.threadRef,
                channelRef = core.channelRef,
                turnRef = core.turnRef,
                requestEventRef = core.requestEventRef,
                requestEventSha256 = core.requestEventSha256,
                responseEventRef = core.responseEventRef,
                responseEventSha256 = core.responseEventSha256,
                promptContext = core.promptContext,
                runtimeObservation = core.runtimeObservation,
                invocationEvidence = core.invocationEvidence,
                rawProviderResponsePersisted = core.rawProviderResponsePersisted,
                rawReasoningPersisted = core.rawReasoningPersisted,
                rawReportedModelPersisted = core.rawReportedModelPersisted,
                privacyClass = core.privacyClass,
                formedAt = core.formedAt,
            )

        VexOwnedPath.ensureDirectory(
            productHome,
            contextDirectory,
        )
        val path =
            pathFor(
                request.turnRef,
            )
        val existing =
            VexDurableContractFile.read<Record>(
                ownedRoot = productHome,
                path = path,
            )

        if (
            existing !=
                null
        ) {
            require(
                existing ==
                    value,
            ) {
                "direct turn context address already contains different evidence"
            }
            return existing
        }

        VexDurableContractFile.write(
            ownedRoot = productHome,
            path = path,
            value = value,
        )

        return requireNotNull(
            read(
                request.turnRef,
            ),
        )
    }

    fun read(
        turnRef: String,
    ): Record? {
        requireRef(
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

        return VexDurableContractFile
            .read<Record>(
                ownedRoot = productHome,
                path =
                    pathFor(
                        turnRef,
                    ),
            )
            ?.also(
                ::validate,
            )
    }

    private fun validate(
        value: Record,
    ) {
        require(
            value.schemaVersion ==
                Schema &&
                value.homeRef ==
                manifest.homeRef &&
                value.deviceRef ==
                manifest.currentDeviceRef &&
                value.companionLineageRef ==
                manifest.currentCompanionLineageRef &&
                value.projectRef ==
                DirectConversationIdentity.ProjectRef &&
                value.threadRef ==
                DirectConversationIdentity.ThreadRef &&
                value.channelRef ==
                DirectConversationIdentity.ChannelRef &&
                value.privacyClass ==
                "DEVICE_PRIVATE" &&
                !value.rawProviderResponsePersisted &&
                !value.rawReasoningPersisted &&
                !value.rawReportedModelPersisted,
        ) {
            "direct turn context identity/privacy is invalid"
        }

        val core =
            RecordCore(
                schemaVersion = value.schemaVersion,
                homeRef = value.homeRef,
                deviceRef = value.deviceRef,
                companionLineageRef = value.companionLineageRef,
                projectRef = value.projectRef,
                threadRef = value.threadRef,
                channelRef = value.channelRef,
                turnRef = value.turnRef,
                requestEventRef = value.requestEventRef,
                requestEventSha256 = value.requestEventSha256,
                responseEventRef = value.responseEventRef,
                responseEventSha256 = value.responseEventSha256,
                promptContext = value.promptContext,
                runtimeObservation = value.runtimeObservation,
                invocationEvidence = value.invocationEvidence,
                rawProviderResponsePersisted = value.rawProviderResponsePersisted,
                rawReasoningPersisted = value.rawReasoningPersisted,
                rawReportedModelPersisted = value.rawReportedModelPersisted,
                privacyClass = value.privacyClass,
                formedAt = value.formedAt,
            )
        val hash =
            sha256(
                VexContractJson.encode(
                    core,
                ),
            )
        require(
            value.contextSha256 ==
                hash &&
                value.contextRef ==
                "context.vexlife-prod.direct." +
                    hash.take(
                        32,
                    ),
        ) {
            "direct turn context hash/address is invalid"
        }
        require(
            value.promptContext.currentRequestEventRef ==
                value.requestEventRef &&
                value.promptContext.currentRequestEventSha256 ==
                value.requestEventSha256 &&
                value.runtimeObservation.observationRef ==
                value.invocationEvidence.runtimeObservationRef &&
                value.runtimeObservation.semanticFingerprint ==
                value.invocationEvidence.runtimeObservationFingerprint,
        ) {
            "direct turn context internal evidence bindings are invalid"
        }
    }

    private fun pathFor(
        turnRef: String,
    ): Path {
        requireRef(
            turnRef,
            "turnRef",
        )

        return VexOwnedPath.resolveInside(
            contextDirectory,
            "$turnRef.json",
        )
    }

    private fun requireRef(
        value: String,
        label: String,
    ) {
        require(
            value.matches(
                Regex(
                    "[A-Za-z0-9][A-Za-z0-9._:-]{0,220}",
                ),
            ),
        ) {
            "$label must be one portable stable ref"
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
