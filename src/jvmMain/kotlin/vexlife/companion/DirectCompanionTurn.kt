package vexlife.companion

import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.security.MessageDigest
import java.time.Instant
import kotlinx.serialization.Serializable
import vexlife.contract.VexContractJson
import vexlife.contract.VexDurableContractFile
import vexlife.conversation.DirectConversationStore
import vexlife.filesystem.VexOwnedPath
import vexlife.local.VexLifeProdLocalSetup

/**
 * One direct personal human -> Vex turn over the isolated VexLife-Prod local foundation.
 *
 * This owner composes existing owners; it does not re-own Home, runtime, prompt selection,
 * Conversation persistence, provider invocation or presentation.
 */
object DirectCompanionTurn {
    const val FailureReceiptSchema =
        "vexlife-prod.direct-companion-turn-failure/v1"

    data class Intent(
        val sendAttemptRef: String,
        val content: String,
    ) {
        init {
            requireRef(
                sendAttemptRef,
                "sendAttemptRef",
            )
            require(
                content.isNotEmpty(),
            ) {
                "direct Companion turn content must be non-empty"
            }
        }
    }

    data class Result(
        val state: String,
        val recoveredCompletedTurn: Boolean,
        val modelCallPerformed: Boolean,
        val actualHttpCallPerformedNow: Boolean,
        val turnRef: String,
        val requestMessageRef: String,
        val responseMessageRef: String,
        val requestEventRef: String,
        val responseEventRef: String,
        val content: String,
        val model: String,
        val currentConversationHeadSha256: String,
        val contextRef: String,
        val promptContextReceipt: DirectPromptContext.Receipt,
        val runtimeObservation: DirectCompanionConnection.RuntimeObservation,
        val invocationEvidence: DirectCompanionConnection.InvocationEvidence,
    )

    @Serializable
    private data class FailureCore(
        val schemaVersion: String,
        val turnRef: String,
        val requestMessageRef: String,
        val responseMessageRef: String,
        val requestEventRefOrNull: String?,
        val requestEventSha256OrNull: String?,
        val lastValidConversationHeadSha256OrNull: String?,
        val failureCode: String,
        val requestDurablyRecorded: Boolean,
        val responseDurablyRecorded: Boolean,
        val contextDurablyRecorded: Boolean,
        val currentHeadCommitted: Boolean,
        val formedAt: String,
    )

    @Serializable
    data class FailureReceipt(
        val schemaVersion: String,
        val failureRef: String,
        val semanticFingerprint: String,
        val turnRef: String,
        val requestMessageRef: String,
        val responseMessageRef: String,
        val requestEventRefOrNull: String?,
        val requestEventSha256OrNull: String?,
        val lastValidConversationHeadSha256OrNull: String?,
        val failureCode: String,
        val requestDurablyRecorded: Boolean,
        val responseDurablyRecorded: Boolean,
        val contextDurablyRecorded: Boolean,
        val currentHeadCommitted: Boolean,
        val formedAt: String,
    )

    class TurnException(
        val failureCode: String,
        val failureReceipt: FailureReceipt?,
        cause: Throwable,
    ) : IllegalStateException(
        "direct Companion turn failed: $failureCode",
        cause,
    )

    internal data class Hooks(
        val transport: DirectCompanionConnection.Transport =
            DirectCompanionConnection.SystemTransport,
        val now: () -> Instant = {
            Instant.now()
        },
    )

    fun perform(
        setup: VexLifeProdLocalSetup.Result,
        intent: Intent,
    ): Result =
        performWithHooks(
            setup = setup,
            intent = intent,
            hooks = Hooks(),
        )

    internal fun performWithHooks(
        setup: VexLifeProdLocalSetup.Result,
        intent: Intent,
        hooks: Hooks,
    ): Result {
        require(
            setup.state ==
                "LOCAL_HERETIC_FOUNDATION_READY",
        ) {
            "direct Companion turn requires one ready isolated local foundation"
        }

        val productHome =
            Path.of(
                setup.productHome,
            )
                .also {
                    require(
                        it.isAbsolute,
                    ) {
                        "local setup productHome must be absolute"
                    }
                }
                .normalize()

        val identity =
            turnIdentity(
                intent.sendAttemptRef,
            )
        val conversation =
            DirectConversationStore(
                productHome,
            )
        val contextStore =
            DirectTurnContextStore(
                productHome,
            )

        return try {
            conversation.withWriterLease {
                val before =
                    current()

                val recovered =
                    completedDuplicate(
                        snapshot = before,
                        contextStore = contextStore,
                        identity = identity,
                        content = intent.content,
                    )

                if (
                    recovered !=
                        null
                ) {
                    return@withWriterLease recovered
                }

                val prior =
                    immediatePriorCompletedTurn()
                var request: DirectConversationStore.Event? =
                    null
                var response: DirectConversationStore.Event? =
                    null
                var context: DirectTurnContextStore.Record? =
                    null

                try {
                    request =
                        beginRequest(
                            turnRef = identity.turnRef,
                            requestMessageRef = identity.requestMessageRef,
                            content = intent.content,
                            formedAt = hooks.now(),
                        )
                    val prompt =
                        DirectPromptContext.materialize(
                            prior = prior,
                            currentRequest = request,
                        )

                    DirectPromptContext.reverify(
                        materialization = prompt,
                        prior = immediatePriorCompletedTurn(),
                        currentRequest = request,
                    )

                    val inference =
                        DirectCompanionConnection.invoke(
                            runtime = setup.qualifiedRuntimeEvidence,
                            messages = prompt.messages,
                            transport = hooks.transport,
                            now = hooks.now,
                        )

                    response =
                        appendResponse(
                            request = request,
                            responseMessageRef = identity.responseMessageRef,
                            content = inference.content,
                            model = inference.model,
                            endpointOrigin = inference.invocationEvidence.endpointOrigin,
                            runtimeObservationRef = inference.runtimeObservation.observationRef,
                            invocationEvidenceRef = inference.invocationEvidence.invocationRef,
                            formedAt = hooks.now(),
                        )

                    context =
                        contextStore.record(
                            request = request,
                            response = response,
                            promptContext = prompt.receipt,
                            runtimeObservation = inference.runtimeObservation,
                            invocationEvidence = inference.invocationEvidence,
                            formedAt = hooks.now(),
                        )

                    val head =
                        commitCompletedTurn(
                            request = request,
                            response = response,
                            contextRef = context.contextRef,
                            contextSha256 = context.contextSha256,
                            formedAt = hooks.now(),
                        )

                    Result(
                        state = "TURN_COMPLETED",
                        recoveredCompletedTurn = false,
                        modelCallPerformed = true,
                        actualHttpCallPerformedNow = true,
                        turnRef = identity.turnRef,
                        requestMessageRef = identity.requestMessageRef,
                        responseMessageRef = identity.responseMessageRef,
                        requestEventRef = request.eventRef,
                        responseEventRef = response.eventRef,
                        content = response.content,
                        model =
                            requireNotNull(
                                response.model,
                            ),
                        currentConversationHeadSha256 = head.conversationHeadSha256,
                        contextRef = context.contextRef,
                        promptContextReceipt = prompt.receipt,
                        runtimeObservation = inference.runtimeObservation,
                        invocationEvidence = inference.invocationEvidence,
                    )
                } catch (
                    error: Throwable,
                ) {
                    val failure =
                        if (
                            request !=
                                null
                        ) {
                            writeFailureReceipt(
                                productHome = productHome,
                                identity = identity,
                                request = request,
                                lastValidHeadSha256 =
                                    before.head
                                        ?.conversationHeadSha256,
                                failureCode =
                                    failureCode(
                                        error,
                                    ),
                                responseDurablyRecorded =
                                    response !=
                                        null,
                                contextDurablyRecorded =
                                    context !=
                                        null,
                                formedAt = hooks.now(),
                            )
                        } else {
                            null
                        }

                    throw TurnException(
                        failureCode =
                            failureCode(
                                error,
                            ),
                        failureReceipt = failure,
                        cause = error,
                    )
                }
            }
        } catch (
            error: DirectConversationStore.IncompleteConversationException,
        ) {
            throw TurnException(
                failureCode = "CONVERSATION_RECOVERY_REQUIRED",
                failureReceipt = null,
                cause = error,
            )
        }
    }

    private data class Identity(
        val turnRef: String,
        val requestMessageRef: String,
        val responseMessageRef: String,
    )

    private fun turnIdentity(
        sendAttemptRef: String,
    ): Identity {
        val fingerprint =
            sha256(
                DirectConversationIdentity.ThreadRef +
                    "\u0000" +
                    sendAttemptRef,
            )

        return Identity(
            turnRef =
                "turn.vexlife-prod.self-development." +
                    fingerprint.take(
                        32,
                    ),
            requestMessageRef =
                "message.vexlife-prod.self-development.human." +
                    fingerprint.take(
                        32,
                    ),
            responseMessageRef =
                "message.vexlife-prod.self-development.companion." +
                    fingerprint.take(
                        32,
                    ),
        )
    }

    private fun completedDuplicate(
        snapshot: DirectConversationStore.Snapshot,
        contextStore: DirectTurnContextStore,
        identity: Identity,
        content: String,
    ): Result? {
        val index =
            snapshot.events.indexOfFirst {
                it.messageRef ==
                    identity.requestMessageRef
            }

        if (
            index <
                0
        ) {
            return null
        }

        val request =
            snapshot.events[
                index
            ]
        require(
            request.eventKind ==
                "REQUEST" &&
                request.turnRef ==
                identity.turnRef &&
                request.content ==
                content,
        ) {
            "sendAttemptRef already identifies a different durable human request"
        }

        val response =
            snapshot.events
                .getOrNull(
                    index +
                        1,
                )
        require(
            response !=
                null &&
                response.eventKind ==
                "RESPONSE" &&
                response.turnRef ==
                identity.turnRef &&
                response.messageRef ==
                identity.responseMessageRef &&
                response.priorEventSha256 ==
                request.eventSha256,
        ) {
            "sendAttemptRef identifies an incomplete or mismatched durable turn"
        }

        val context =
            requireNotNull(
                contextStore.read(
                    identity.turnRef,
                ),
            ) {
                "completed duplicate turn has no exact durable context evidence"
            }
        val head =
            requireNotNull(
                snapshot.head,
            )

        return Result(
            state = "TURN_COMPLETED",
            recoveredCompletedTurn = true,
            modelCallPerformed = false,
            actualHttpCallPerformedNow = false,
            turnRef = identity.turnRef,
            requestMessageRef = identity.requestMessageRef,
            responseMessageRef = identity.responseMessageRef,
            requestEventRef = request.eventRef,
            responseEventRef = response.eventRef,
            content = response.content,
            model =
                requireNotNull(
                    response.model,
                ),
            currentConversationHeadSha256 = head.conversationHeadSha256,
            contextRef = context.contextRef,
            promptContextReceipt = context.promptContext,
            runtimeObservation = context.runtimeObservation,
            invocationEvidence = context.invocationEvidence,
        )
    }

    private fun writeFailureReceipt(
        productHome: Path,
        identity: Identity,
        request: DirectConversationStore.Event,
        lastValidHeadSha256: String?,
        failureCode: String,
        responseDurablyRecorded: Boolean,
        contextDurablyRecorded: Boolean,
        formedAt: Instant,
    ): FailureReceipt? {
        return try {
            val directory =
                VexOwnedPath.resolveInside(
                    productHome,
                    "recovery/direct-companion",
                )
            VexOwnedPath.ensureDirectory(
                productHome,
                directory,
            )
            val core =
                FailureCore(
                    schemaVersion = FailureReceiptSchema,
                    turnRef = identity.turnRef,
                    requestMessageRef = identity.requestMessageRef,
                    responseMessageRef = identity.responseMessageRef,
                    requestEventRefOrNull = request.eventRef,
                    requestEventSha256OrNull = request.eventSha256,
                    lastValidConversationHeadSha256OrNull = lastValidHeadSha256,
                    failureCode = failureCode,
                    requestDurablyRecorded = true,
                    responseDurablyRecorded = responseDurablyRecorded,
                    contextDurablyRecorded = contextDurablyRecorded,
                    currentHeadCommitted = false,
                    formedAt = formedAt.toString(),
                )
            val fingerprint =
                sha256(
                    VexContractJson.encode(
                        core,
                    ),
                )
            val receipt =
                FailureReceipt(
                    schemaVersion = core.schemaVersion,
                    failureRef =
                        "failure.vexlife-prod.direct-companion." +
                            fingerprint.take(
                                32,
                            ),
                    semanticFingerprint = fingerprint,
                    turnRef = core.turnRef,
                    requestMessageRef = core.requestMessageRef,
                    responseMessageRef = core.responseMessageRef,
                    requestEventRefOrNull = core.requestEventRefOrNull,
                    requestEventSha256OrNull = core.requestEventSha256OrNull,
                    lastValidConversationHeadSha256OrNull = core.lastValidConversationHeadSha256OrNull,
                    failureCode = core.failureCode,
                    requestDurablyRecorded = core.requestDurablyRecorded,
                    responseDurablyRecorded = core.responseDurablyRecorded,
                    contextDurablyRecorded = core.contextDurablyRecorded,
                    currentHeadCommitted = core.currentHeadCommitted,
                    formedAt = core.formedAt,
                )
            val path =
                VexOwnedPath.resolveInside(
                    directory,
                    identity.turnRef +
                        ".json",
                )
            val existing =
                VexDurableContractFile.read<FailureReceipt>(
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
                    value = receipt,
                )
                receipt
            } else {
                // Preserve the first substantive failure for this exact send attempt.
                existing
            }
        } catch (
            _: Exception,
        ) {
            null
        }
    }

    private fun failureCode(
        error: Throwable,
    ): String =
        when (
            error
        ) {
            is DirectCompanionConnection.ConnectionException ->
                "CONNECTION_" +
                    error.code.name

            is DirectConversationStore.IncompleteConversationException ->
                "CONVERSATION_RECOVERY_REQUIRED"

            is IllegalArgumentException ->
                "ADMISSION_FAILED"

            else ->
                "TURN_FAILED"
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
