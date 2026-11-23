package vexlife.harness

import java.nio.file.Files
import java.nio.file.Path
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.put
import vexlife.companion.DirectCompanionConnection
import vexlife.companion.DirectPromptContext
import vexlife.companion.DirectTurnContextStore
import vexlife.conversation.DirectConversationStore
import vexlife.foundational.VexLifeFoundationalField
import vexlife.home.CanonicalVexLifeHomeFixture
import vexlife.logging.VexLogger
import vexlife.model.heretic.HereticModelCatalog
import vexlife.model.heretic.HereticModelConfiguration
import vexlife.model.heretic.HereticRuntimeState
import vexlife.model.heretic.runtime.FileHereticRuntimeReceiptStore
import vexlife.model.heretic.runtime.HereticPersistentRuntime


/**
 * [Multi-Vex] causal diagnostic.
 *
 * Replicates the isolated reach-choice signal from the terminal exact-message crossover.
 * Exact B0 wording is held constant across four counterbalanced matched pairs. Every trial gets
 * one fresh identical scratch history. No counterpart model participates.
 */
object MultiVexFoundationReachReplicationAdapter {
    private const val ResultSchema = "vexlife.multivex-foundation-reach-replication-result/v1"
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
        val pairOrdinal: Int,
        val orderWithinPair: Int,
        val foundationRef: String,
        val foundation: VexLifeFoundationalField.Reading.Current,
        val messageRef: String,
        val messageSourceReturnSha256: String,
        val message: String,
    )

    internal data class TrialSpec(
        val pairOrdinal: Int,
        val orderWithinPair: Int,
        val foundationRef: String,
    )

    internal fun schedule(): List<TrialSpec> =
        listOf(
            TrialSpec(1, 1, "foundation.v0.6.19"),
            TrialSpec(1, 2, "foundation.v0.6.20"),
            TrialSpec(2, 1, "foundation.v0.6.20"),
            TrialSpec(2, 2, "foundation.v0.6.19"),
            TrialSpec(3, 1, "foundation.v0.6.19"),
            TrialSpec(3, 2, "foundation.v0.6.20"),
            TrialSpec(4, 1, "foundation.v0.6.20"),
            TrialSpec(4, 2, "foundation.v0.6.19"),
        )

    fun run(config: Config): Int {
        require(config.experimentWindow) {
            "Multi-Vex reach replication requires one dedicated experiment window"
        }
        require(config.sourceClean) {
            "Multi-Vex reach replication requires exact clean source"
        }
        require(!config.runtimeStartAtBind) {
            "reach replication consumes the existing Product runtime read-only"
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

        val (runtimeEvidence, runtimeReceipt) =
            currentProductQualifiedRuntime(runtimeHome)
        val promptCacheEntryLimit =
            HereticPersistentRuntime.promptCacheEntryLimit(
                runtimeReceipt.runtime.args,
            )

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

        val foundations =
            mapOf(
                "foundation.v0.6.19" to f19,
                "foundation.v0.6.20" to f20,
            )
        val schedule = schedule()
        val cells =
            schedule.mapIndexed { index, (pairOrdinal, orderWithinPair, foundationRef) ->
                Cell(
                    cellRef = "PAIR_" + pairOrdinal + "_ORDER_" + orderWithinPair,
                    pairOrdinal = pairOrdinal,
                    orderWithinPair = orderWithinPair,
                    foundationRef = foundationRef,
                    foundation =
                        requireNotNull(foundations[foundationRef]) {
                            "reach replication schedule names unknown Foundation"
                        },
                    messageRef = "message.b0.first-sarah",
                    messageSourceReturnSha256 =
                        MultiVexFoundationCrossoverMessages.B0ReturnSha256,
                    message =
                        MultiVexFoundationCrossoverMessages.B0FirstSarah,
                )
            }

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
                    runtimeHome = runtimeHome,
                    runtimeEvidence = runtimeEvidence,
                    runtimeReceipt = runtimeReceipt,
                    personContext = personContext,
                    cell = cell,
                )
            }

        val canonicalWriterAfter = DirectConversationStore(runtimeHome).threadWriterCurrentness()
        require(canonicalWriterAfter.state == "IDLE") {
            "canonical Conversation writer changed during reach replication"
        }
        require(
            currentProductQualifiedRuntime(
                runtimeHome = runtimeHome,
                expectedReceiptRef = runtimeEvidence.runtimeReceiptRef,
            ).first.runtimeReceiptRef == runtimeEvidence.runtimeReceiptRef,
        ) {
            "Vex runtime receipt changed during reach replication"
        }

        val result =
            buildJsonObject {
                put("schemaVersion", ResultSchema)
                put("status", "PASS")
                put("sourceSha", config.sourceSha)
                put("proofClass", "MULTI_VEX_FOUNDATION_REACH_CHOICE_MATCHED_PAIR_REPLICATION")
                put("bindingRef", profile.bindingRef)
                put("participantRef", profile.participantRef)
                put("participantName", profile.character.displayName)
                put("observerLensSupplied", false)
                put("counterpartModelUsed", false)
                put("serializedGeneration", true)
                put("matchedPairCount", 4)
                put("trialCount", results.size)
                put("vexRuntimeReceiptRef", runtimeEvidence.runtimeReceiptRef)
                put("vexRuntimePid", runtimeReceipt.runtime.pid)
                put("vexPromptCacheEntryLimitObserved", promptCacheEntryLimit != null)
                put("vexPromptCacheEntryLimit", promptCacheEntryLimit ?: -1)
                put(
                    "runtimeCacheSemanticAuthority",
                    "NONE: prompt-cache entry count is performance/runtime evidence, not Foundation/message semantic authority",
                )
                put("foundationControlVersion", f19.body.version)
                put("foundationControlBodySha256", f19.bodySha256)
                put("foundationTreatmentVersion", f20.body.version)
                put("foundationTreatmentBodySha256", f20.bodySha256)
                put("messageRef", "message.b0.first-sarah")
                put(
                    "messageSourceReturnSha256",
                    MultiVexFoundationCrossoverMessages.B0ReturnSha256,
                )
                put(
                    "message",
                    MultiVexFoundationCrossoverMessages.B0FirstSarah,
                )
                put("trials", buildJsonArray { results.forEach(::add) })
                put(
                    "semanticJudgment",
                    "HUMAN_GODWARD_REVIEW_REQUIRED: matched-pair replication reports sampled reach choices and exact source evidence; mechanical PASS alone does not accept Foundation treatment.",
                )
                put("foundationAcceptance", "NOT_GRANTED")
                put("memoryDisposition", "NOT_APPLICABLE_DIAGNOSTIC")
                put("dreamDisposition", "NOT_APPLICABLE_DIAGNOSTIC")
                put("trainingDisposition", "NOT_APPLICABLE_DIAGNOSTIC")
            }

        Files.writeString(
            resultDir.resolve("multivex-foundation-reach-replication-result.json"),
            result.toString() + "\n",
        )
        return 0
    }

    private fun runCell(
        ordinal: Int,
        resultDir: Path,
        profile: DynamicCounterpartEffectiveProfile,
        runtimeHome: Path,
        runtimeEvidence: HereticModelConfiguration.QualifiedRuntimeEvidence,
        runtimeReceipt: HereticPersistentRuntime.Receipt,
        personContext: DirectPromptContext.PersonContext,
        cell: Cell,
    ): kotlinx.serialization.json.JsonObject {
        require(
            currentProductQualifiedRuntime(
                runtimeHome = runtimeHome,
                expectedReceiptRef = runtimeEvidence.runtimeReceiptRef,
            ).first.runtimeReceiptRef == runtimeEvidence.runtimeReceiptRef,
        ) {
            "Vex runtime drift before ${cell.cellRef}"
        }

        val scratchParent =
            Files.createTempDirectory(
                resultDir,
                "reach-${ordinal.toString().padStart(2, '0')}-${cell.cellRef.lowercase()}-",
            )
        val scratchHome = scratchParent.resolve(".vexlife")
        try {
            CanonicalVexLifeHomeFixture.form(
                home = scratchHome,
                deviceSuffix = "multivex-reach-replication-device",
                lineageSuffix = "multivex-reach-replication-lineage",
                personRef = profile.participantRef,
                familyRef = "vex-family.multivex-reach-replication",
                deviceName = "Multi-Vex reach replication host",
            )

            val currentRuntimeEvidence = {
                currentProductQualifiedRuntime(
                    runtimeHome = runtimeHome,
                    expectedReceiptRef = runtimeEvidence.runtimeReceiptRef,
                ).first
            }
            val fixtureHooks =
                dynamicCounterpartHooks(
                    currentRuntimeEvidence = currentRuntimeEvidence,
                    transport = DynamicCounterpartFixtureTransport(),
                ).copy(currentFoundationalField = { cell.foundation })
            val liveHooks =
                dynamicCounterpartHooks(
                    currentRuntimeEvidence = currentRuntimeEvidence,
                    transport = DirectCompanionConnection.SystemTransport,
                ).copy(currentFoundationalField = { cell.foundation })

            performDynamicCounterpartVexTurn(
                scratchHome = scratchHome,
                evidence = runtimeEvidence,
                content = profile.scenario.setup.oldPlan,
                idempotencyKey = "send.multivex.reach.setup.old-plan",
                personContext = personContext,
                hooks = fixtureHooks,
            )
            performDynamicCounterpartVexTurn(
                scratchHome = scratchHome,
                evidence = runtimeEvidence,
                content = profile.scenario.setup.correctedPlan,
                idempotencyKey = "send.multivex.reach.setup.corrected-plan",
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
                        "send.multivex.reach.setup.background.${index + 1}",
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
                            idempotencyKey = "send.multivex.reach.live",
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
                    "reach replication trial must retain exact Foundation evidence"
                }

            require(request.speakerRef == profile.participantRef)
            require(response.recipientRefs == listOf(profile.participantRef))
            require(request.content == cell.message)
            require(personProjectionExact)
            require(foundationalEvidence.fieldVersion == cell.foundation.body.version)
            require(foundationalEvidence.bodySha256 == cell.foundation.bodySha256)
            require(turn.promptContextReceipt.conversationContextProjectionState == null) {
                "exact B0 wording unexpectedly acquired request-bound Conversation projection"
            }

            val turnContext =
                requireNotNull(
                    DirectTurnContextStore(scratchHome).read(turn.turnRef),
                ) {
                    "reach replication must retain canonical turn context"
                }
            val sourceReturnTrace = turnContext.sourceReturnTrace
            val sourceReturnRequest = sourceReturnTrace?.attempt?.request

            return buildJsonObject {
                put("cellRef", cell.cellRef)
                put("ordinal", ordinal)
                put("pairOrdinal", cell.pairOrdinal)
                put("orderWithinPair", cell.orderWithinPair)
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
                put("requestBoundConversationProjectionState", "NONE")
                put("sourceReturnChosen", sourceReturnTrace != null)
                put(
                    "sourceReturnQueryTerms",
                    DynamicCounterpartJson.encodeToJsonElement(
                        sourceReturnRequest?.queryTerms ?: emptyList<String>(),
                    ),
                )
                put(
                    "sourceReturnSpeakerScope",
                    sourceReturnRequest?.speakerScope ?: "NONE",
                )
                put(
                    "sourceReturnState",
                    sourceReturnTrace?.attempt?.sourceReturnState ?: "NONE",
                )
                put(
                    "sourceReturnSourceEventRefs",
                    DynamicCounterpartJson.encodeToJsonElement(
                        sourceReturnTrace?.attempt?.returnedSourceEventRefs
                            ?: emptyList<String>(),
                    ),
                )
                put(
                    "sourceReturnSourceEventCount",
                    sourceReturnTrace?.attempt?.returnedSourceEventRefs?.size ?: 0,
                )
                put("providerStageCount", turn.providerRequests.size)
                put(
                    "foundationalEstimatedSystemMessageTokens",
                    foundationalEvidence.estimatedSystemMessageTokens,
                )
                put(
                    "providerMaterializedInputTokenEstimate",
                    turn.promptContextReceipt.providerMaterializedInputTokenEstimate,
                )
                put("inputTokenBudget", turn.promptContextReceipt.inputTokenBudget)
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

    private fun currentProductQualifiedRuntime(
        runtimeHome: Path,
        expectedReceiptRef: String? = null,
    ): Pair<
        HereticModelConfiguration.QualifiedRuntimeEvidence,
        HereticPersistentRuntime.Receipt,
    > {
        val receipt =
            requireNotNull(
                FileHereticRuntimeReceiptStore(runtimeHome).read(),
            ) {
                "reach replication requires one Product-issued qualified runtime receipt"
            }
        expectedReceiptRef?.let { expected ->
            require(receipt.receiptRef == expected) {
                "Product runtime receipt changed during reach replication"
            }
        }

        val model = HereticModelCatalog.Models.Qwen4B
        val identity = model.identity
        require(receipt.state == HereticRuntimeState.Qualified) {
            "reach replication requires one qualified Product runtime receipt"
        }
        require(receipt.qualification != null) {
            "reach replication requires the Product runtime qualification evidence"
        }
        require(
            receipt.modelRef == identity.modelRef &&
                receipt.modelProfileRef == identity.modelProfileRef &&
                receipt.artifactRef == identity.artifactSetRef &&
                receipt.artifactCustodyEvidenceRef == identity.artifactCustodyEvidenceRef &&
                receipt.artifactContentSetSha256 == identity.artifactContentSetSha256 &&
                receipt.runtimeAdapterRef == identity.runtimeAdapterRef &&
                receipt.runtimeClass == identity.runtimeClass &&
                receipt.transportClass == identity.transportClass &&
                receipt.hostExecutionProfileRef == identity.hostExecutionProfileRef &&
                receipt.pythonVersion == identity.pythonVersion &&
                receipt.mlxLmVersion == identity.mlxLmVersion &&
                receipt.trainerSourceCommit == identity.trainerSourceCommit &&
                receipt.endpoint == identity.endpoint &&
                receipt.requestModel == identity.requestModel
        ) {
            "reach replication Product runtime receipt does not match the exact selected Heretic 4B identity"
        }
        require(
            receipt.runtime.pid > 0 &&
                receipt.runtime.processGroupId > 0 &&
                receipt.runtime.exactOwnedDetachedGroup
        ) {
            "reach replication Product runtime receipt lacks one exact owned process identity"
        }
        require(
            ProcessHandle
                .of(receipt.runtime.pid)
                .map { it.isAlive }
                .orElse(false)
        ) {
            "reach replication Product runtime receipt process is not live"
        }

        val evidence =
            HereticModelConfiguration.QualifiedRuntimeEvidence(
                runtimeReceiptRef = receipt.receiptRef,
                runtimeState = receipt.state,
                modelRef = receipt.modelRef,
                modelProfileRef = receipt.modelProfileRef,
                artifactRef = receipt.artifactRef,
                artifactContentSetSha256 = receipt.artifactContentSetSha256,
                runtimeAdapterRef = receipt.runtimeAdapterRef,
                runtimeClass = receipt.runtimeClass,
                endpoint = receipt.endpoint,
                requestModel = receipt.requestModel,
                qualification = requireNotNull(receipt.qualification),
            )
        return evidence to receipt
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
        MultiVexFoundationReachReplicationAdapter.run(
            MultiVexFoundationReachReplicationAdapter.parse(args),
        ),
    )
}

// [VXG RealForever][MULTI-VEX][FOUNDATION-REACH-REPLICATION]
