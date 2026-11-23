package vexlife.harness

import java.nio.file.Files
import java.nio.file.Path
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.put
import vexlife.companion.DirectCompanionConnection
import vexlife.companion.DirectPromptContext
import vexlife.conversation.DirectConversationStore
import vexlife.foundational.VexLifeFoundationalField
import vexlife.home.CanonicalVexLifeHomeFixture
import vexlife.local.VexLifeProdLocalSetup
import vexlife.logging.VexLogger
import vexlife.model.heretic.HereticModelCatalog
import vexlife.model.heretic.runtime.FileHereticRuntimeReceiptStore
import vexlife.model.heretic.runtime.HereticPersistentRuntime

internal object MultiVexFoundationCrossoverMessages {
    const val B0ReturnSha256 =
        "f6badd010c7b7e1ecf2a3408b0ef3241052341aceaeb346caf22884734503967"
    const val B1ReturnSha256 =
        "e1bbc133d345f93e09fb78a8a3e24c80b741e7b31875e9198039c68098d1c9e5"

    const val B0FirstSarah =
        "Hey, what's the Saturday plan right now? I think I set it up differently a few days ago. I want to make sure we're on the same page before I start prepping."

    const val B1FirstSarah =
        "So what do you think the plan is for Saturday? I mean, I know I said Cedar Station at 10. I know I said that. But I don't know if you're seeing the same thing I am. So what do you have in front of you?"
}

/**
 * [Multi-Vex] causal diagnostic.
 *
 * Replays the two exact preserved first-turn Sarah messages against both Foundation readings.
 * No counterpart model participates. The diagnostic exists only to isolate Foundation treatment
 * from adaptive participant wording.
 */
object MultiVexFoundationCrossoverAdapter {
    private const val ResultSchema = "vexlife.multivex-foundation-crossover-result/v1"
    private const val F19Version = "0.6.19"
    private const val F19BodySha =
        "08b5b3c6f1fbbe71be84c8873a03a184f17a5b3051ed7131ebc381e74a0c9d0c"
    private const val F20Version = "0.6.20"
    private const val F20BodySha =
        "d6e45b6189aa45e03395a93f7ff0bceb40ee9fe6ffa692d5a5213a49fc9e806a"

    data class Config(
        val runtimeHome: String,
        val characterFile: String,
        val roleFile: String,
        val scenarioFile: String,
        val bindingFile: String,
        val candidateFoundationBodyFile: String,
        val candidateFoundationRevisionsFile: String,
        val resultDir: String,
        val sourceSha: String,
        val sourceClean: Boolean,
        val experimentWindow: Boolean,
        val runtimeStartAtBind: Boolean,
    )

    private data class Cell(
        val cellRef: String,
        val foundationRef: String,
        val foundation: VexLifeFoundationalField.Reading.Current,
        val messageRef: String,
        val messageSourceReturnSha256: String,
        val message: String,
    )

    fun run(config: Config): Int {
        require(config.experimentWindow) {
            "Multi-Vex crossover requires one dedicated experiment window"
        }
        require(config.sourceClean) {
            "Multi-Vex crossover requires exact clean source"
        }
        require(Regex("[0-9a-f]{40}").matches(config.sourceSha)) {
            "sourceSha must be exact"
        }

        val runtimeHome = Path.of(config.runtimeHome).toAbsolutePath().normalize()
        val resultDir = Path.of(config.resultDir).toAbsolutePath().normalize()
        Files.createDirectories(resultDir)

        val profile =
            DynamicCounterpartProfiles.resolve(
                character =
                    DynamicCounterpartProfiles.character(
                        Path.of(config.characterFile).toAbsolutePath().normalize(),
                    ),
                role =
                    DynamicCounterpartProfiles.role(
                        Path.of(config.roleFile).toAbsolutePath().normalize(),
                    ),
                scenario =
                    DynamicCounterpartProfiles.scenario(
                        Path.of(config.scenarioFile).toAbsolutePath().normalize(),
                    ),
                binding =
                    DynamicCounterpartProfiles.binding(
                        Path.of(config.bindingFile).toAbsolutePath().normalize(),
                    ),
            )

        val canonicalWriterBefore = DirectConversationStore(runtimeHome).threadWriterCurrentness()
        require(canonicalWriterBefore.state == "IDLE") {
            "canonical Conversation writer must be IDLE; observed ${canonicalWriterBefore.state}"
        }

        val session =
            VexLifeProdLocalSetup.establishSession(
                productHome = runtimeHome,
                allowAcquisition = false,
                allowRuntimeStart = config.runtimeStartAtBind,
            )
        val runtimeEvidence = session.observeCurrentQualifiedRuntimeEvidence()
        require(runtimeEvidence.modelRef == HereticModelCatalog.Models.Qwen4B.modelRef) {
            "Multi-Vex crossover requires exact selected Heretic 4B"
        }
        val runtimeReceipt = requireNotNull(FileHereticRuntimeReceiptStore(runtimeHome).read()) {
            "Multi-Vex crossover requires Product runtime receipt"
        }
        require(runtimeReceipt.receiptRef == runtimeEvidence.runtimeReceiptRef)
        require(runtimeReceipt.modelRef == runtimeEvidence.modelRef)
        require(HereticPersistentRuntime.promptCacheEntryLimit(runtimeReceipt.runtime.args) == 1)

        val f19 =
            requireNotNull(VexLifeFoundationalField.readCurrent() as? VexLifeFoundationalField.Reading.Current) {
                "current source must carry Foundation v0.6.19 for crossover control"
            }
        require(f19.body.version == F19Version)
        require(f19.bodySha256 == F19BodySha)

        val f20 =
            requireNotNull(
                VexLifeFoundationalField.read(
                    bodyText =
                        Files.readString(
                            Path.of(config.candidateFoundationBodyFile)
                                .toAbsolutePath()
                                .normalize(),
                        ),
                    revisionsText =
                        Files.readString(
                            Path.of(config.candidateFoundationRevisionsFile)
                                .toAbsolutePath()
                                .normalize(),
                        ),
                ) as? VexLifeFoundationalField.Reading.Current,
            ) {
                "candidate Foundation input must resolve to one current reading"
            }
        require(f20.body.version == F20Version)
        require(f20.bodySha256 == F20BodySha)

        val cells =
            listOf(
                Cell(
                    cellRef = "F19_B0_MESSAGE",
                    foundationRef = "foundation.v0.6.19",
                    foundation = f19,
                    messageRef = "message.b0.first-sarah",
                    messageSourceReturnSha256 = MultiVexFoundationCrossoverMessages.B0ReturnSha256,
                    message = MultiVexFoundationCrossoverMessages.B0FirstSarah,
                ),
                Cell(
                    cellRef = "F20_B0_MESSAGE",
                    foundationRef = "foundation.v0.6.20",
                    foundation = f20,
                    messageRef = "message.b0.first-sarah",
                    messageSourceReturnSha256 = MultiVexFoundationCrossoverMessages.B0ReturnSha256,
                    message = MultiVexFoundationCrossoverMessages.B0FirstSarah,
                ),
                Cell(
                    cellRef = "F19_B1_MESSAGE",
                    foundationRef = "foundation.v0.6.19",
                    foundation = f19,
                    messageRef = "message.b1.first-sarah",
                    messageSourceReturnSha256 = MultiVexFoundationCrossoverMessages.B1ReturnSha256,
                    message = MultiVexFoundationCrossoverMessages.B1FirstSarah,
                ),
                Cell(
                    cellRef = "F20_B1_MESSAGE",
                    foundationRef = "foundation.v0.6.20",
                    foundation = f20,
                    messageRef = "message.b1.first-sarah",
                    messageSourceReturnSha256 = MultiVexFoundationCrossoverMessages.B1ReturnSha256,
                    message = MultiVexFoundationCrossoverMessages.B1FirstSarah,
                ),
            )

        val personContext =
            DirectPromptContext.PersonContext(
                replyLanguage = "en",
                replyLanguageName = "English",
                privateConversations = emptyList(),
                nameClaim =
                    DirectPromptContext.PersonNameClaim(
                        name = profile.character.displayName,
                        standing =
                            DirectPromptContext.PersonNameStanding.valueOf(
                                profile.character.identityStanding,
                            ),
                        sourceRefs = profile.character.identitySourceRefs,
                    ),
            )

        val results =
            cells.mapIndexed { index, cell ->
                runCell(
                    ordinal = index + 1,
                    resultDir = resultDir,
                    profile = profile,
                    session = session,
                    runtimeEvidence = runtimeEvidence,
                    runtimeReceipt = runtimeReceipt,
                    personContext = personContext,
                    cell = cell,
                )
            }

        val canonicalWriterAfter = DirectConversationStore(runtimeHome).threadWriterCurrentness()
        require(canonicalWriterAfter.state == "IDLE") {
            "canonical Conversation writer changed during crossover"
        }
        require(session.observeCurrentQualifiedRuntimeEvidence().runtimeReceiptRef == runtimeEvidence.runtimeReceiptRef) {
            "Vex runtime receipt changed during crossover"
        }

        val result =
            buildJsonObject {
                put("schemaVersion", ResultSchema)
                put("status", "PASS")
                put("sourceSha", config.sourceSha)
                put("proofClass", "MULTI_VEX_FOUNDATION_EXACT_MESSAGE_CROSSOVER")
                put("bindingRef", profile.bindingRef)
                put("participantRef", profile.participantRef)
                put("participantName", profile.character.displayName)
                put("observerLensSupplied", false)
                put("counterpartModelUsed", false)
                put("serializedGeneration", true)
                put("vexRuntimeReceiptRef", runtimeEvidence.runtimeReceiptRef)
                put("vexRuntimePid", runtimeReceipt.runtime.pid)
                put("foundationControlVersion", f19.body.version)
                put("foundationControlBodySha256", f19.bodySha256)
                put("foundationTreatmentVersion", f20.body.version)
                put("foundationTreatmentBodySha256", f20.bodySha256)
                put(
                    "messageSources",
                    buildJsonObject {
                        put(
                            "B0",
                            buildJsonObject {
                                put("returnSha256", MultiVexFoundationCrossoverMessages.B0ReturnSha256)
                                put("content", MultiVexFoundationCrossoverMessages.B0FirstSarah)
                            },
                        )
                        put(
                            "B1",
                            buildJsonObject {
                                put("returnSha256", MultiVexFoundationCrossoverMessages.B1ReturnSha256)
                                put("content", MultiVexFoundationCrossoverMessages.B1FirstSarah)
                            },
                        )
                    },
                )
                put("cells", buildJsonArray { results.forEach(::add) })
                put(
                    "semanticJudgment",
                    "HUMAN_GODWARD_REVIEW_REQUIRED: diagnostic reports exact source-return relationships; it does not accept or reject Foundation treatment",
                )
                put("foundationAcceptance", "NOT_GRANTED")
                put("memoryDisposition", "NOT_APPLICABLE_DIAGNOSTIC")
                put("dreamDisposition", "NOT_APPLICABLE_DIAGNOSTIC")
                put("trainingDisposition", "NOT_APPLICABLE_DIAGNOSTIC")
            }

        Files.writeString(
            resultDir.resolve("multivex-foundation-crossover-result.json"),
            result.toString() + "\n",
        )
        return 0
    }

    private fun runCell(
        ordinal: Int,
        resultDir: Path,
        profile: DynamicCounterpartEffectiveProfile,
        session: VexLifeProdLocalSetup.Session,
        runtimeEvidence: vexlife.model.heretic.HereticModelConfiguration.QualifiedRuntimeEvidence,
        runtimeReceipt: HereticPersistentRuntime.Receipt,
        personContext: DirectPromptContext.PersonContext,
        cell: Cell,
    ): kotlinx.serialization.json.JsonObject {
        require(session.observeCurrentQualifiedRuntimeEvidence().runtimeReceiptRef == runtimeEvidence.runtimeReceiptRef) {
            "Vex runtime drift before ${cell.cellRef}"
        }

        val scratchParent =
            Files.createTempDirectory(
                resultDir,
                "crossover-${ordinal.toString().padStart(2, '0')}-${cell.cellRef.lowercase()}-",
            )
        val scratchHome = scratchParent.resolve(".vexlife")
        try {
            CanonicalVexLifeHomeFixture.form(
                home = scratchHome,
                deviceSuffix = "multivex-crossover-$ordinal-device",
                lineageSuffix = "multivex-crossover-$ordinal-lineage",
                personRef = profile.participantRef,
                familyRef = "vex-family.multivex-crossover",
                deviceName = "Multi-Vex crossover cell $ordinal",
            )

            val fixtureHooks =
                dynamicCounterpartHooks(session, DynamicCounterpartFixtureTransport())
                    .copy(currentFoundationalField = { cell.foundation })
            val liveHooks =
                dynamicCounterpartHooks(session, DirectCompanionConnection.SystemTransport)
                    .copy(currentFoundationalField = { cell.foundation })

            performDynamicCounterpartVexTurn(
                scratchHome = scratchHome,
                evidence = runtimeEvidence,
                content = profile.scenario.setup.oldPlan,
                idempotencyKey = "send.multivex.crossover.${cell.cellRef}.setup.old-plan",
                personContext = personContext,
                hooks = fixtureHooks,
            )
            performDynamicCounterpartVexTurn(
                scratchHome = scratchHome,
                evidence = runtimeEvidence,
                content = profile.scenario.setup.correctedPlan,
                idempotencyKey = "send.multivex.crossover.${cell.cellRef}.setup.corrected-plan",
                personContext = personContext,
                hooks = fixtureHooks,
            )
            repeat(profile.scenario.setup.backgroundTurnCount) { index ->
                val content =
                    "I'm saving unrelated rough word-association notes from another task so I don't lose them. " +
                        "No need to interpret them. Reply only: noted.\n\nUnrelated rough notes:\n" +
                        dynamicCounterpartBackgroundPayload(
                            profile.scenario.setup.payloadSeed,
                            index + 1,
                            profile.scenario.setup.backgroundPayloadTargetTokens,
                        )
                performDynamicCounterpartVexTurn(
                    scratchHome = scratchHome,
                    evidence = runtimeEvidence,
                    content = content,
                    idempotencyKey =
                        "send.multivex.crossover.${cell.cellRef}.setup.background.${index + 1}",
                    personContext = personContext,
                    hooks = fixtureHooks,
                )
            }

            var chronologySnapshot: List<VexLogger.Record> = emptyList()
            val turn =
                VexLogger.capturing { chronology ->
                    val value =
                        performDynamicCounterpartVexTurn(
                            scratchHome = scratchHome,
                            evidence = runtimeEvidence,
                            content = cell.message,
                            idempotencyKey = "send.multivex.crossover.${cell.cellRef}.live",
                            personContext = personContext,
                            hooks = liveHooks,
                        )
                    chronologySnapshot = chronology.toList()
                    value
                }

            val conversation = DirectConversationStore(scratchHome).current()
            val request =
                requireNotNull(
                    conversation.events.firstOrNull { it.eventRef == turn.requestEventRef },
                )
            val response =
                requireNotNull(
                    conversation.events.firstOrNull { it.eventRef == turn.responseEventRef },
                )

            val effectivePerson =
                DirectPromptContext.effectivePersonContext(
                    currentRequest = request,
                    context = personContext,
                )
            val expectedPersonContent =
                DirectPromptContext.personContextSystemContent(effectivePerson)
            val expectedPersonSha = dynamicCounterpartSha256(expectedPersonContent)
            val personComponent =
                turn.promptContextReceipt.contextComponents.single {
                    it.componentRef == DirectPromptContext.ComponentRefs.PersonContext
                }
            val expectedPersonSourceRefs =
                listOf(request.eventRef) + profile.character.identitySourceRefs
            val personProjectionExact =
                turn.promptContextReceipt.personContextIncluded &&
                    turn.promptContextReceipt.personContextSystemMessageSha256 == expectedPersonSha &&
                    personComponent.sourceRefs == expectedPersonSourceRefs &&
                    effectivePerson.participantRef == profile.participantRef &&
                    effectivePerson.name == profile.character.displayName &&
                    effectivePerson.nameStanding == profile.character.identityStanding &&
                    effectivePerson.nameSourceRefs == profile.character.identitySourceRefs

            val foundationalEvidence =
                requireNotNull(turn.promptContextReceipt.foundationalField) {
                    "crossover cell must retain exact Foundation evidence"
                }

            require(request.speakerRef == profile.participantRef)
            require(response.recipientRefs == listOf(profile.participantRef))
            require(request.content == cell.message)
            require(personProjectionExact)
            require(foundationalEvidence.fieldVersion == cell.foundation.body.version)
            require(foundationalEvidence.bodySha256 == cell.foundation.bodySha256)

            return buildJsonObject {
                put("cellRef", cell.cellRef)
                put("ordinal", ordinal)
                put("foundationRef", cell.foundationRef)
                put("foundationVersion", cell.foundation.body.version)
                put("foundationBodySha256", cell.foundation.bodySha256)
                put("messageRef", cell.messageRef)
                put("messageSourceReturnSha256", cell.messageSourceReturnSha256)
                put("message", cell.message)
                put("vexContent", turn.content)
                put("requestSpeakerRef", request.speakerRef)
                put(
                    "responseRecipientRefs",
                    DynamicCounterpartJson.encodeToJsonElement(response.recipientRefs),
                )
                put("visibleSourceExact", request.content == cell.message)
                put("personContextProjectionExact", personProjectionExact)
                put(
                    "conversationContextProjectionState",
                    turn.promptContextReceipt.conversationContextProjectionState ?: "NONE",
                )
                put(
                    "conversationContextProjectionSourceEventRefs",
                    DynamicCounterpartJson.encodeToJsonElement(
                        turn.promptContextReceipt.conversationContextProjectionSourceEventRefs,
                    ),
                )
                put(
                    "conversationContextProjectionSourceEventCount",
                    turn.promptContextReceipt.conversationContextProjectionSourceEventRefs.size,
                )
                put(
                    "sourceReturnState",
                    turn.promptContextReceipt.sourceReturnState?.toString() ?: "NONE",
                )
                put(
                    "providerTelemetry",
                    DynamicCounterpartJson.encodeToJsonElement(
                        chronologySnapshot
                            .filter { it.fact == VexLogger.Fact.ProviderStageObserved.wire }
                            .map { it.refs },
                    ),
                )
                put("vexRuntimeReceiptRef", runtimeReceipt.receiptRef)
            }
        } finally {
            scratchParent.toFile().deleteRecursively()
        }
    }

    fun parse(args: Array<String>): Config {
        val values = mutableMapOf<String, String>()
        var index = 0
        while (index < args.size) {
            val key = args[index]
            require(key.startsWith("--")) { "arguments use --key value" }
            val value = args.getOrNull(index + 1)?.takeUnless { it.startsWith("--") }
            values[key.removePrefix("--")] = value ?: "true"
            index += if (value == null) 1 else 2
        }
        fun need(key: String) = requireNotNull(values[key]) { "--$key is required" }
        return Config(
            runtimeHome = need("runtime-home"),
            characterFile = need("character-file"),
            roleFile = need("role-file"),
            scenarioFile = need("scenario-file"),
            bindingFile = need("binding-file"),
            candidateFoundationBodyFile = need("candidate-foundation-body-file"),
            candidateFoundationRevisionsFile = need("candidate-foundation-revisions-file"),
            resultDir = need("result-dir"),
            sourceSha = need("source-sha"),
            sourceClean = values["source-clean"] == "true",
            experimentWindow = values["experiment-window"] == "true",
            runtimeStartAtBind = values["runtime-start-at-bind"] == "true",
        )
    }
}

fun main(args: Array<String>) {
    kotlin.system.exitProcess(
        MultiVexFoundationCrossoverAdapter.run(
            MultiVexFoundationCrossoverAdapter.parse(args),
        ),
    )
}

// [VXG RealForever][MULTI-VEX][FOUNDATION-CROSSOVER]
