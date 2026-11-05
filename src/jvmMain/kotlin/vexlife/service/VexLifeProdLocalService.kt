package vexlife.service

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.io.ByteArrayOutputStream
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import vexlife.companion.DirectCompanionTurn
import vexlife.conversation.DirectConversationIdentity
import vexlife.conversation.DirectConversationStore
import vexlife.local.VexLifeProdLocalSetup
import vexlife.model.heretic.HereticRuntimeState

/**
 * Fixed loopback service boundary for the isolated VexLife-Prod product.
 *
 * Browser/presentation code consumes [VexLifeProdServiceContract]. This JVM realization owns only
 * local HTTP delivery and projection. It does not own Home, Conversation, prompt selection, Heretic
 * lifecycle, source update policy, process lifetime, or presentation semantics.
 */
class VexLifeProdLocalService private constructor(
    val sourceSha: String,
    private val startupSetup: VexLifeProdLocalSetup.Result,
    private val server: HttpServer,
    private val executor: ExecutorService,
    private val turnExecutor: TurnExecutor,
) : AutoCloseable {
    companion object {
        private const val MaxRequestBytes =
            256 * 1024

        private val SourceShaPattern =
            Regex(
                "[0-9a-f]{40,64}",
            )

        private val JsonCodec =
            Json {
                encodeDefaults = true
                explicitNulls = true
                ignoreUnknownKeys = false
                isLenient = false
                allowSpecialFloatingPointValues = false
                allowStructuredMapKeys = false
            }

        internal fun start(
            sourceSha: String,
            session: VexLifeProdLocalSetup.Session,
        ): VexLifeProdLocalService =
            startAt(
                sourceSha = sourceSha,
                startupSetup = session.result,
                address =
                    InetSocketAddress(
                        VexLifeProdServiceContract.Host,
                        VexLifeProdServiceContract.Port,
                    ),
                turnExecutor =
                    TurnExecutor {
                        _,
                        intent,
                        ->
                        DirectCompanionTurn.perform(
                            session = session,
                            intent = intent,
                        )
                    },
            )

        internal fun startForTest(
            sourceSha: String,
            setup: VexLifeProdLocalSetup.Result,
            turnExecutor: TurnExecutor,
        ): VexLifeProdLocalService =
            startAt(
                sourceSha = sourceSha,
                startupSetup = setup,
                address =
                    InetSocketAddress(
                        VexLifeProdServiceContract.Host,
                        0,
                    ),
                turnExecutor = turnExecutor,
            )

        private fun startAt(
            sourceSha: String,
            startupSetup: VexLifeProdLocalSetup.Result,
            address: InetSocketAddress,
            turnExecutor: TurnExecutor,
        ): VexLifeProdLocalService {
            require(
                SourceShaPattern.matches(
                    sourceSha,
                ),
            ) {
                "VexLife-Prod service sourceSha must be one lowercase Git identity"
            }
            require(
                startupSetup.state ==
                    "LOCAL_HERETIC_FOUNDATION_READY" &&
                    startupSetup.qualifiedRuntimeEvidence.runtimeState ==
                    HereticRuntimeState.Qualified,
            ) {
                "VexLife-Prod service requires one qualified isolated local foundation"
            }

            val productHome =
                Path.of(
                    startupSetup.productHome,
                )
            require(
                productHome.isAbsolute,
            ) {
                "VexLife-Prod service requires one absolute product Home"
            }

            // Construction-time validation only. The Conversation store remains the durable owner.
            DirectConversationStore(
                productHome,
            )
                .current()

            val executor =
                Executors.newCachedThreadPool()
            val server =
                HttpServer.create(
                    address,
                    0,
                )

            val service =
                VexLifeProdLocalService(
                    sourceSha = sourceSha,
                    startupSetup = startupSetup,
                    server = server,
                    executor = executor,
                    turnExecutor = turnExecutor,
                )

            server.executor =
                executor
            server.createContext(
                "/",
            ) {
                exchange ->
                service.handle(
                    exchange,
                )
            }
            server.start()

            return service
        }
    }

    internal fun interface TurnExecutor {
        fun perform(
            setup: VexLifeProdLocalSetup.Result,
            intent: DirectCompanionTurn.Intent,
        ): DirectCompanionTurn.Result
    }

    val origin: String
        get() =
            "http://" +
                VexLifeProdServiceContract.Host +
                ":" +
                server.address.port

    val port: Int
        get() =
            server.address.port

    override fun close() {
        server.stop(
            0,
        )
        executor.shutdownNow()
    }

    private fun handle(
        exchange: HttpExchange,
    ) {
        try {
            if (
                exchange.requestURI.rawQuery !=
                null
            ) {
                respondError(
                    exchange = exchange,
                    status = 400,
                    code = "QUERY_NOT_ADMITTED",
                    message = "VexLife-Prod local service routes do not accept query parameters",
                )
                return
            }

            when (
                exchange.requestURI.path
            ) {
                VexLifeProdServiceContract.SourceCurrentnessPath ->
                    handleSourceCurrentness(
                        exchange,
                    )

                VexLifeProdServiceContract.ConversationPath ->
                    handleConversation(
                        exchange,
                    )

                VexLifeProdServiceContract.ConversationSendPath ->
                    handleSend(
                        exchange,
                    )

                else ->
                    respondError(
                        exchange = exchange,
                        status = 404,
                        code = "ROUTE_NOT_FOUND",
                        message = "VexLife-Prod local service route not found",
                    )
            }
        } catch (
            error: DirectCompanionTurn.TurnException,
        ) {
            respondJson(
                exchange = exchange,
                status = 409,
                value =
                    VexLifeProdServiceContract.ErrorResponse(
                        schemaVersion = VexLifeProdServiceContract.ErrorSchema,
                        code = error.failureCode,
                        message = "direct Companion turn failed",
                        retrySameSendAttemptAllowed = error.retrySameSendAttemptAllowed,
                        exactNextSafeRoute = error.exactNextSafeRoute,
                    ),
            )
        } catch (
            error: SerializationException,
        ) {
            respondError(
                exchange = exchange,
                status = 400,
                code = "REQUEST_JSON_INVALID",
                message = "request JSON does not match the current service contract",
            )
        } catch (
            error: IllegalArgumentException,
        ) {
            respondError(
                exchange = exchange,
                status = 400,
                code = "REQUEST_NOT_ADMITTED",
                message = error.message
                    ?: "request is not admitted",
            )
        } catch (
            _: Throwable,
        ) {
            respondError(
                exchange = exchange,
                status = 500,
                code = "SERVICE_FAILURE",
                message = "VexLife-Prod local service failed without a completed product result",
            )
        } finally {
            exchange.close()
        }
    }

    private fun handleSourceCurrentness(
        exchange: HttpExchange,
    ) {
        requireMethod(
            exchange,
            "GET",
        )

        val conversation =
            conversationProjection()

        respondJson(
            exchange = exchange,
            status = 200,
            value =
                VexLifeProdServiceContract.SourceCurrentness(
                    schemaVersion = VexLifeProdServiceContract.SourceCurrentnessSchema,
                    serviceRef = VexLifeProdServiceContract.ServiceRef,
                    productRef = VexLifeProdServiceContract.ProductRef,
                    serviceState = "SOURCE_BOUND_LISTENING",
                    sourceSha = sourceSha,
                    processPid = ProcessHandle.current().pid(),
                    runtimeCurrentness = "NOT_REOBSERVED_BY_SERVICE",
                    startupQualifiedRuntimeReceiptRef =
                        startupSetup.qualifiedRuntimeEvidence.runtimeReceiptRef,
                    currentConversationHeadSha256 =
                        conversation.currentConversationHeadSha256,
                    effectsPerformed = false,
                ),
        )
    }

    private fun handleConversation(
        exchange: HttpExchange,
    ) {
        requireMethod(
            exchange,
            "GET",
        )

        respondJson(
            exchange = exchange,
            status = 200,
            value =
                conversationProjection(),
        )
    }

    private fun handleSend(
        exchange: HttpExchange,
    ) {
        requireMethod(
            exchange,
            "POST",
        )
        requireJson(
            exchange,
        )

        val request =
            JsonCodec.decodeFromString<VexLifeProdServiceContract.SendRequest>(
                requestText(
                    exchange,
                ),
            )

        val result =
            turnExecutor.perform(
                startupSetup,
                DirectCompanionTurn.Intent(
                    sendAttemptRef = request.sendAttemptRef,
                    content = request.content,
                ),
            )

        respondJson(
            exchange = exchange,
            status = 200,
            value =
                VexLifeProdServiceContract.SendResponse(
                    schemaVersion = VexLifeProdServiceContract.SendResponseSchema,
                    state = result.state,
                    recoveredCompletedTurn = result.recoveredCompletedTurn,
                    modelCallPerformed = result.modelCallPerformed,
                    actualHttpCallPerformedNow = result.actualHttpCallPerformedNow,
                    turnRef = result.turnRef,
                    requestMessageRef = result.requestMessageRef,
                    responseMessageRef = result.responseMessageRef,
                    requestEventRef = result.requestEventRef,
                    responseEventRef = result.responseEventRef,
                    content = result.content,
                    model = result.model,
                    currentConversationHeadSha256 = result.currentConversationHeadSha256,
                    contextRef = result.contextRef,
                    promptContext =
                        VexLifeProdServiceContract.PromptContextProjection(
                            receiptRef = result.promptContextReceipt.receiptRef,
                            semanticFingerprint = result.promptContextReceipt.semanticFingerprint,
                            contextMode = result.promptContextReceipt.contextMode,
                            priorConversationHeadSha256 =
                                result.promptContextReceipt.priorConversationHeadSha256,
                            selectedConversationEventRefs =
                                result.promptContextReceipt.selectedConversationEventRefs,
                            providerMessageSetSha256 =
                                result.promptContextReceipt.providerMessageSetSha256,
                            providerMaterializedInputTokenEstimate =
                                result.promptContextReceipt.providerMaterializedInputTokenEstimate,
                            inputTokenBudget = result.promptContextReceipt.inputTokenBudget,
                            reservedOutputTokens = result.promptContextReceipt.reservedOutputTokens,
                            hardTokenLimit = result.promptContextReceipt.hardTokenLimit,
                            wholeHistoryEventEnumerationPerformed =
                                result.promptContextReceipt.wholeHistoryEventEnumerationPerformed,
                            memoryIncluded = result.promptContextReceipt.memoryIncluded,
                            memorySelectionPerformed =
                                result.promptContextReceipt.memorySelectionPerformed,
                            trainingSelectionPerformed =
                                result.promptContextReceipt.trainingSelectionPerformed,
                            effectsPerformed = result.promptContextReceipt.effectsPerformed,
                        ),
                ),
        )
    }

    private fun conversationProjection(): VexLifeProdServiceContract.ConversationProjection {
        val snapshot =
            DirectConversationStore(
                Path.of(
                    startupSetup.productHome,
                ),
            )
                .current()

        val messages =
            snapshot.canonicalEvents
                .map {
                    event ->
                    VexLifeProdServiceContract.ConversationMessage(
                        eventRef = event.eventRef,
                        eventSha256 = event.eventSha256,
                        messageRef = event.messageRef,
                        turnRef = event.turnRef,
                        role =
                            when (
                                event.eventKind
                            ) {
                                "REQUEST" ->
                                    "human"

                                "RESPONSE" ->
                                    "vex"

                                else ->
                                    error(
                                        "canonical direct Conversation contains unsupported event kind",
                                    )
                            },
                        content = event.content,
                        sequence = event.sequence,
                        formedAt = event.formedAt,
                        model = event.model,
                    )
                }

        return VexLifeProdServiceContract.ConversationProjection(
            schemaVersion = VexLifeProdServiceContract.ConversationProjectionSchema,
            projectRef = DirectConversationIdentity.ProjectRef,
            threadRef = DirectConversationIdentity.ThreadRef,
            channelRef = DirectConversationIdentity.ChannelRef,
            currentConversationHeadSha256 =
                snapshot.head
                    ?.conversationHeadSha256,
            messages = messages,
            abandonedAttemptCount = snapshot.abandonedAttempts.size,
            effectsPerformed = false,
        )
    }

    private fun requireMethod(
        exchange: HttpExchange,
        expected: String,
    ) {
        require(
            exchange.requestMethod ==
                expected,
        ) {
            "route requires HTTP $expected"
        }
    }

    private fun requireJson(
        exchange: HttpExchange,
    ) {
        val contentType =
            exchange.requestHeaders
                .getFirst(
                    "content-type",
                )
                ?.substringBefore(
                    ";",
                )
                ?.trim()
                ?.lowercase()

        require(
            contentType ==
                "application/json",
        ) {
            "Send requires application/json"
        }
    }

    private fun requestText(
        exchange: HttpExchange,
    ): String {
        val bytes =
            exchange.requestBody.readNBytes(
                MaxRequestBytes +
                    1,
            )

        require(
            bytes.size <=
                MaxRequestBytes,
        ) {
            "request body exceeds bounded local service size"
        }

        return bytes.toString(
            StandardCharsets.UTF_8,
        )
    }

    private inline fun <reified T> respondJson(
        exchange: HttpExchange,
        status: Int,
        value: T,
    ) {
        val bytes =
            JsonCodec
                .encodeToString(
                    value,
                )
                .toByteArray(
                    StandardCharsets.UTF_8,
                )

        exchange.responseHeaders.set(
            "content-type",
            "application/json; charset=utf-8",
        )
        exchange.responseHeaders.set(
            "cache-control",
            "no-store",
        )
        exchange.sendResponseHeaders(
            status,
            bytes.size
                .toLong(),
        )
        exchange.responseBody.write(
            bytes,
        )
    }

    private fun respondError(
        exchange: HttpExchange,
        status: Int,
        code: String,
        message: String,
    ) {
        respondJson(
            exchange = exchange,
            status = status,
            value =
                VexLifeProdServiceContract.ErrorResponse(
                    schemaVersion = VexLifeProdServiceContract.ErrorSchema,
                    code = code,
                    message = message,
                    retrySameSendAttemptAllowed = null,
                    exactNextSafeRoute = null,
                ),
        )
    }
}

// [VXG RealForever]
