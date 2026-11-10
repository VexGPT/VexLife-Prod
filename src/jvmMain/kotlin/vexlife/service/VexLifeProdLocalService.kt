package vexlife.service

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import vexlife.browser.VexLifeProdBrowserContract
import vexlife.browser.VexLifeProdFrameHost
import vexlife.companion.DirectCompanionTurn
import vexlife.companion.DirectPromptContext
import vexlife.companion.DirectTurnContextStore
import vexlife.conversation.DirectConversationIdentity
import vexlife.conversation.DirectConversationStore
import vexlife.local.VexLifeProdLocalSetup
import vexlife.model.heretic.HereticRuntimeState
import vexlife.web.VexLifeWebP1RuntimeArtifacts
import vexlife.web.VexWebArtifacts

/**
 * Fixed loopback service boundary for the local VexLife implementation.
 *
 * Browser/presentation code consumes [VexLifeProdServiceContract]. This JVM realization owns only
 * local HTTP delivery and projection. It does not own Home, Conversation, Memory, prompt selection,
 * Heretic lifecycle, source update policy, process lifetime, or presentation semantics.
 */
class VexLifeProdLocalService private constructor(
    val sourceSha: String,
    private val startupSetup: VexLifeProdLocalSetup.Result,
    private val server: HttpServer,
    private val frameHost: VexLifeProdFrameHost,
    private val presentation: VexWebArtifacts.Bundle,
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

        /**
         * Ordinary product entry consumes the already-qualified Session at the fixed source-owned
         * loopback coordinate. Each new Send re-observes runtime currentness through the Session;
         * the service does not become a runtime or Conversation owner.
         */
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
                frameAddress =
                    InetSocketAddress(
                        VexLifeProdServiceContract.Host,
                        VexLifeProdBrowserContract.FrameHostPort,
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
                frameAddress =
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
            frameAddress: InetSocketAddress,
            turnExecutor: TurnExecutor,
        ): VexLifeProdLocalService {
            require(
                SourceShaPattern.matches(
                    sourceSha,
                ),
            ) {
                "VexLife service sourceSha must be one lowercase Git identity"
            }
            require(
                startupSetup.state ==
                    "LOCAL_HERETIC_FOUNDATION_READY" &&
                    startupSetup.qualifiedRuntimeEvidence.runtimeState ==
                    HereticRuntimeState.Qualified,
            ) {
                "VexLife service requires one qualified local runtime foundation"
            }

            val productHome =
                Path.of(
                    startupSetup.productHome,
                )
            require(
                productHome.isAbsolute,
            ) {
                "VexLife service requires one absolute canonical Home"
            }

            // Construction-time validation only. The Conversation store remains the durable owner.
            DirectConversationStore(
                productHome,
            )
                .current()

            val presentation =
                VexLifeWebP1RuntimeArtifacts.generate()
            val executor =
                Executors.newCachedThreadPool()
            var frameHost: VexLifeProdFrameHost? =
                null
            var server: HttpServer? =
                null

            try {
                frameHost =
                    VexLifeProdFrameHost.startAt(
                        frameAddress,
                    )
                server =
                    HttpServer.create(
                        address,
                        0,
                    )
                val service =
                    VexLifeProdLocalService(
                        sourceSha = sourceSha,
                        startupSetup = startupSetup,
                        server = server,
                        frameHost = frameHost,
                        presentation = presentation,
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
            } catch (
                error: Throwable,
            ) {
                server
                    ?.stop(
                        0,
                    )
                frameHost
                    ?.close()
                executor.shutdownNow()
                throw error
            }
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

    val frameOrigin: String
        get() =
            frameHost.origin

    override fun close() {
        server.stop(
            0,
        )
        frameHost.close()
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
                    message = "VexLife local service routes do not accept query parameters",
                )
                return
            }

            when (
                exchange.requestURI.path
            ) {
                VexLifeProdServiceContract.PresentationIndexPath ->
                    handlePresentation(
                        exchange = exchange,
                        artifactPath = "index.html",
                        contentType = "text/html; charset=utf-8",
                    )

                VexLifeProdServiceContract.PresentationCssPath ->
                    handlePresentation(
                        exchange = exchange,
                        artifactPath = "vexweb.css",
                        contentType = "text/css; charset=utf-8",
                    )

                VexLifeProdServiceContract.PresentationJavascriptPath ->
                    handlePresentation(
                        exchange = exchange,
                        artifactPath = "vexweb.js",
                        contentType = "text/javascript; charset=utf-8",
                    )

                VexLifeProdServiceContract.PresentationManifestPath ->
                    handlePresentation(
                        exchange = exchange,
                        artifactPath = "vexweb-manifest.json",
                        contentType = "application/json; charset=utf-8",
                    )

                VexLifeProdServiceContract.SourceCurrentnessPath ->
                    handleSourceCurrentness(
                        exchange,
                    )

                VexLifeProdServiceContract.ConversationPath ->
                    handleConversation(
                        exchange,
                    )

                VexLifeProdServiceContract.ContextPath ->
                    handleContext(
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
                        message = "VexLife local service route not found",
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
                message = "VexLife local service failed without a completed product result",
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
                    frameHostOrigin = frameOrigin,
                    runtimeCurrentness = "NOT_REOBSERVED_BY_SERVICE",
                    startupQualifiedRuntimeReceiptRef =
                        startupSetup.qualifiedRuntimeEvidence.runtimeReceiptRef,
                    currentConversationHeadSha256 =
                        conversation.currentConversationHeadSha256,
                    effectsPerformed = false,
                ),
        )
    }

    private fun handlePresentation(
        exchange: HttpExchange,
        artifactPath: String,
        contentType: String,
    ) {
        requireMethod(
            exchange,
            "GET",
        )

        val bytes =
            presentation
                .content(
                    artifactPath,
                )
                .toByteArray(
                    StandardCharsets.UTF_8,
                )

        exchange.responseHeaders.set(
            "content-type",
            contentType,
        )
        exchange.responseHeaders.set(
            "cache-control",
            "no-store",
        )
        exchange.sendResponseHeaders(
            200,
            bytes.size
                .toLong(),
        )
        exchange.responseBody.write(
            bytes,
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

    private fun handleContext(
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
                contextProjection(),
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
                    idempotencyKey = request.idempotencyKey,
                    content = request.content,
                    browserContext =
                        request.browserContext
                            ?.let {
                                DirectPromptContext.BrowserContext(
                                    pageUrl = it.pageUrl,
                                    title = it.title,
                                    text = it.text,
                                    capture = it.capture,
                                )
                            },
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
                    contextPath = result.contextPath,
                    promptContext =
                        promptContextProjection(
                            result.promptContextReceipt,
                        ),
                    humanReasoning =
                        humanReasoningProjection(
                            result,
                        ),
                ),
        )
    }

    private fun humanReasoningProjection(
        result: DirectCompanionTurn.Result,
    ): VexLifeProdServiceContract.HumanReasoningProjection {
        val trace =
            result.runtimeObservation.reasoningTrace

        if (
            result.recoveredCompletedTurn
        ) {
            if (
                !trace.present
            ) {
                require(
                    trace.contentSha256 ==
                        null &&
                        trace.characterCount ==
                        0,
                ) {
                    "absent recovered reasoning trace carries unexpected fingerprint material"
                }

                return VexLifeProdServiceContract.HumanReasoningProjection(
                    state = "ABSENT",
                    text = null,
                    contentSha256 = null,
                    characterCount = 0,
                    persisted = false,
                )
            }

            return VexLifeProdServiceContract.HumanReasoningProjection(
                state = "UNAVAILABLE_AFTER_IDEMPOTENT_RECOVERY",
                text = null,
                contentSha256 =
                    requireNotNull(
                        trace.contentSha256,
                    ) {
                        "recovered present reasoning trace is missing its content hash"
                    },
                characterCount =
                    trace.characterCount,
                persisted = false,
            )
        }

        val reasoning =
            result.humanReasoning

        if (
            reasoning ==
                null
        ) {
            require(
                !trace.present &&
                    trace.contentSha256 ==
                    null &&
                    trace.characterCount ==
                    0,
            ) {
                "fresh human reasoning disappeared before local service projection"
            }

            return VexLifeProdServiceContract.HumanReasoningProjection(
                state = "ABSENT",
                text = null,
                contentSha256 = null,
                characterCount = 0,
                persisted = false,
            )
        }

        require(
            trace.present &&
                trace.contentSha256 !=
                null &&
                trace.characterCount ==
                reasoning.codePointCount(
                    0,
                    reasoning.length,
                ) &&
                !trace.rawPersisted &&
                trace.humanProjection ==
                "SEALED_EXPLICIT_OPEN_ONLY" &&
                trace.modelProjection ==
                "NONE" &&
                trace.trainingProjection ==
                "NONE",
        ) {
            "fresh human reasoning does not match the sealed runtime observation"
        }

        return VexLifeProdServiceContract.HumanReasoningProjection(
            state = "PRESENT_EPHEMERAL",
            text = reasoning,
            contentSha256 =
                trace.contentSha256,
            characterCount =
                trace.characterCount,
            persisted = false,
        )
    }

    private fun contextProjection(): VexLifeProdServiceContract.ContextProjection {
        val productHome =
            Path.of(
                startupSetup.productHome,
            )
        val snapshot =
            DirectConversationStore(
                productHome,
            )
                .current()
        val head =
            snapshot.head
                ?: return VexLifeProdServiceContract.ContextProjection(
                    schemaVersion =
                        VexLifeProdServiceContract.ContextProjectionSchema,
                    currentConversationHeadSha256 =
                        null,
                    turnRef =
                        null,
                    contextPath =
                        null,
                    promptContext =
                        null,
                    effectsPerformed =
                        false,
                )
        val record =
            DirectTurnContextStore(
                productHome,
            )
                .read(
                    head.turnRef,
                )
                ?: return VexLifeProdServiceContract.ContextProjection(
                    schemaVersion =
                        VexLifeProdServiceContract.ContextProjectionSchema,
                    currentConversationHeadSha256 =
                        head.conversationHeadSha256,
                    turnRef =
                        head.turnRef,
                    contextPath =
                        head.contextPath,
                    promptContext =
                        null,
                    effectsPerformed =
                        false,
                )
        require(
            record.turnRef ==
                head.turnRef &&
                record.contextSha256 ==
                head.contextSha256 &&
                record.contextPath ==
                head.contextPath &&
                record.requestEventSha256 ==
                head.requestEventSha256 &&
                record.responseEventSha256 ==
                head.responseEventSha256,
        ) {
            "current turn Context does not match the exact completed Conversation head"
        }

        return VexLifeProdServiceContract.ContextProjection(
            schemaVersion =
                VexLifeProdServiceContract.ContextProjectionSchema,
            currentConversationHeadSha256 =
                head.conversationHeadSha256,
            turnRef =
                head.turnRef,
            contextPath =
                record.contextPath,
            promptContext =
                promptContextProjection(
                    record.promptContext,
                ),
            effectsPerformed =
                false,
        )
    }

    private fun promptContextProjection(
        receipt: DirectPromptContext.Receipt,
    ): VexLifeProdServiceContract.PromptContextProjection {
        val capabilitySnapshot =
            receipt.capabilityFrameSnapshot
        val capabilityFrame =
            if (
                receipt.capabilityFrameIncluded &&
                capabilitySnapshot !=
                null
            ) {
                VexLifeProdServiceContract.CapabilityFrameProjection(
                    frameRef =
                        requireNotNull(
                            receipt.capabilityFrameRef,
                        ) {
                            "capability frame snapshot is missing its frame ref"
                        },
                    semanticFingerprint =
                        requireNotNull(
                            receipt.capabilityFrameFingerprint,
                        ) {
                            "capability frame snapshot is missing its fingerprint"
                        },
                    sourceRefs =
                        receipt.capabilityFrameSourceRefs,
                    systemMessageSha256 =
                        requireNotNull(
                            receipt.capabilityFrameSystemMessageSha256,
                        ) {
                            "capability frame snapshot is missing its system-message hash"
                        },
                    estimatedSystemMessageTokens =
                        receipt.capabilityFrameEstimatedSystemMessageTokens,
                    availableCapabilityRefs =
                        capabilitySnapshot.availableCapabilityRefs,
                    heldCapabilityRefs =
                        capabilitySnapshot.heldCapabilityRefs,
                    unavailableCapabilityRefs =
                        capabilitySnapshot.unavailableCapabilityRefs,
                    unknownCapabilityRefs =
                        capabilitySnapshot.unknownCapabilityRefs,
                    omittedCapabilityRefs =
                        capabilitySnapshot.omittedCapabilityRefs,
                    omissionReasons =
                        capabilitySnapshot.omissionReasons,
                    tokenBudget =
                        capabilitySnapshot.tokenBudget,
                    usedTokens =
                        capabilitySnapshot.usedTokens,
                    truncated =
                        capabilitySnapshot.truncated,
                    effectAuthorityGranted =
                        capabilitySnapshot.effectAuthorityGranted,
                )
            } else {
                null
            }

        return VexLifeProdServiceContract.PromptContextProjection(
            receiptRef =
                receipt.receiptRef,
            semanticFingerprint =
                receipt.semanticFingerprint,
            contextMode =
                receipt.contextMode,
            priorConversationHeadSha256 =
                receipt.priorConversationHeadSha256,
            selectedConversationEventRefs =
                receipt.selectedConversationEventRefs,
            providerMessageSetSha256 =
                receipt.providerMessageSetSha256,
            providerMaterializedInputTokenEstimate =
                receipt.providerMaterializedInputTokenEstimate,
            inputTokenBudget =
                receipt.inputTokenBudget,
            reservedOutputTokens =
                receipt.reservedOutputTokens,
            hardTokenLimit =
                receipt.hardTokenLimit,
            wholeHistoryEventEnumerationPerformed =
                receipt.wholeHistoryEventEnumerationPerformed,
            memoryContextRef =
                receipt.memoryContextRef,
            memoryContextFingerprint =
                receipt.memoryContextFingerprint,
            memorySourceRefs =
                receipt.memorySourceRefs,
            memoryIncluded =
                receipt.memoryIncluded,
            memorySelectionPerformed =
                receipt.memorySelectionPerformed,
            trainingSelectionPerformed =
                receipt.trainingSelectionPerformed,
            browserContextIncluded =
                receipt.browserContextIncluded,
            browserContextRef =
                receipt.browserContextRef,
            browserPageUrl =
                receipt.browserPageUrl,
            browserTitle =
                receipt.browserTitle,
            browserContextSha256 =
                receipt.browserContextSha256,
            browserContextCharacters =
                receipt.browserContextCharacters,
            browserContextCapture =
                receipt.browserContextCapture,
            workingContinuityIncluded =
                receipt.workingContinuityIncluded,
            workingContinuityContextRef =
                receipt.workingContinuityContextRef,
            workingContinuityContextFingerprint =
                receipt.workingContinuityContextFingerprint,
            capabilityFrameIncluded =
                receipt.capabilityFrameIncluded,
            capabilityFrame =
                capabilityFrame,
            effectsPerformed =
                receipt.effectsPerformed,
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
