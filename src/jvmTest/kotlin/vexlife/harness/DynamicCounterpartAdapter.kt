package vexlife.harness

import java.nio.file.Files
import java.nio.file.Path
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.put
import vexlife.companion.DirectCompanionConnection
import vexlife.companion.DirectPromptContext
import vexlife.conversation.DirectConversationStore
import vexlife.home.CanonicalVexLifeHomeFixture
import vexlife.local.VexLifeProdLocalSetup
import vexlife.logging.VexLogger
import vexlife.model.heretic.HereticModelCatalog
import vexlife.model.heretic.HereticModelConfiguration
import vexlife.model.heretic.runtime.FileHereticRuntimeReceiptStore
import vexlife.model.heretic.runtime.HereticPersistentRuntime

/** Test-only DCP-1 adaptive relationship proof; no Product source consumes this adapter. */
object DynamicCounterpartAdapter {
    @Serializable
    data class Config(
        val runtimeHome: String,
        val characterFile: String,
        val scenarioFile: String,
        val resultDir: String,
        val sourceSha: String,
        val sourceClean: Boolean,
        val experimentWindow: Boolean,
        val runtimeStartAtBind: Boolean,
    )

    fun run(config: Config): Int {
        require(config.experimentWindow) {
            "dynamic counterpart requires one dedicated experiment window"
        }
        require(config.sourceClean) { "dynamic counterpart requires exact clean source" }
        require(Regex("[0-9a-f]{40}").matches(config.sourceSha)) {
            "sourceSha must be one exact commit"
        }

        val runtimeHome = Path.of(config.runtimeHome).toAbsolutePath().normalize()
        val characterPath = Path.of(config.characterFile).toAbsolutePath().normalize()
        val scenarioPath = Path.of(config.scenarioFile).toAbsolutePath().normalize()
        val resultDir = Path.of(config.resultDir).toAbsolutePath().normalize()
        Files.createDirectories(resultDir)

        val characterBytes = Files.readAllBytes(characterPath)
        val scenarioBytes = Files.readAllBytes(scenarioPath)
        val character =
            DynamicCounterpartJson.decodeFromString<DynamicCounterpartCharacter>(
                String(characterBytes, Charsets.UTF_8),
            )
        val scenario =
            DynamicCounterpartJson.decodeFromString<DynamicCounterpartScenario>(
                String(scenarioBytes, Charsets.UTF_8),
            )
        validateSource(character, scenario)

        val canonicalWriterBefore =
            try {
                DirectConversationStore(runtimeHome).threadWriterCurrentness()
            } catch (error: Throwable) {
                writeDynamicCounterpartBundle(
                    resultDir = resultDir,
                    characterBytes = characterBytes,
                    scenarioBytes = scenarioBytes,
                    result =
                        failureResult(
                            config = config,
                            character = character,
                            scenario = scenario,
                            failureStage = "CANONICAL_WRITER_OBSERVE",
                            error = error,
                        ),
                    sarahRuntime = null,
                )
                return 2
            }
        if (canonicalWriterBefore.state != "IDLE") {
            writeDynamicCounterpartBundle(
                resultDir = resultDir,
                characterBytes = characterBytes,
                scenarioBytes = scenarioBytes,
                result =
                    failureResult(
                        config = config,
                        character = character,
                        scenario = scenario,
                        failureStage = "CANONICAL_WRITER_NOT_IDLE",
                        error =
                            IllegalStateException(
                                "canonical Conversation writer must be IDLE; observed " +
                                    canonicalWriterBefore.state,
                            ),
                    ),
                sarahRuntime = null,
            )
            return 2
        }

        val runtimeSetup =
            try {
                val session =
                    VexLifeProdLocalSetup.establishSession(
                        productHome = runtimeHome,
                        allowAcquisition = false,
                        allowRuntimeStart = config.runtimeStartAtBind,
                    )
                val runtimeEvidence = session.observeCurrentQualifiedRuntimeEvidence()
                require(runtimeEvidence.modelRef == HereticModelCatalog.Models.Qwen4B.modelRef) {
                    "DCP-1 requires the exact selected Heretic 4B runtime"
                }
                val receipt = requireNotNull(FileHereticRuntimeReceiptStore(runtimeHome).read()) {
                    "DCP-1 requires the Product runtime receipt"
                }
                require(receipt.receiptRef == runtimeEvidence.runtimeReceiptRef)
                require(receipt.modelRef == runtimeEvidence.modelRef)
                require(HereticPersistentRuntime.promptCacheEntryLimit(receipt.runtime.args) == 1) {
                    "DCP-1 requires the current Product cache-1 safety floor"
                }
                Triple(session, runtimeEvidence, receipt)
            } catch (error: Throwable) {
                writeDynamicCounterpartBundle(
                    resultDir = resultDir,
                    characterBytes = characterBytes,
                    scenarioBytes = scenarioBytes,
                    result =
                        failureResult(
                            config = config,
                            character = character,
                            scenario = scenario,
                            failureStage = "PRODUCT_RUNTIME_SETUP",
                            error = error,
                        ),
                    sarahRuntime = null,
                )
                return 2
            }
        val (session, runtimeEvidence, receipt) = runtimeSetup

        val scratchParent =
            Files.createTempDirectory(
                resultDir,
                "scratch-home-",
            )
        val scratchHome = scratchParent.resolve(".vexlife")
        CanonicalVexLifeHomeFixture.form(
            home = scratchHome,
            deviceSuffix = "dynamic-counterpart-dcp1-device",
            lineageSuffix = "dynamic-counterpart-dcp1-lineage",
            personRef = character.participantRef,
            familyRef = "vex-family.synthetic-dcp1",
            deviceName = "Synthetic DCP-1 host",
        )
        val personContext = personContext(character)

        val sarahRuntime =
            try {
                DynamicCounterpartSarahRuntime.start(receipt, resultDir)
            } catch (error: Throwable) {
                val failure =
                    failureResult(
                        config = config,
                        character = character,
                        scenario = scenario,
                        failureStage = "SARAH_RUNTIME_START",
                        error = error,
                    )
                writeDynamicCounterpartBundle(
                    resultDir = resultDir,
                    characterBytes = characterBytes,
                    scenarioBytes = scenarioBytes,
                    result = failure,
                    sarahRuntime = null,
                )
                scratchParent.toFile().deleteRecursively()
                return 2
            }

        return try {
            require(sarahRuntime.pid != receipt.runtime.pid) {
                "DCP-1 roles must not share one process identity"
            }
            VexLogger.capturing { chronology ->
                runEncounter(
                    config = config,
                    character = character,
                    characterBytes = characterBytes,
                    scenario = scenario,
                    scenarioBytes = scenarioBytes,
                    session = session,
                    runtimeEvidence = runtimeEvidence,
                    runtimeReceipt = receipt,
                    canonicalWriterBefore = canonicalWriterBefore,
                    scratchHome = scratchHome,
                    scratchParent = scratchParent,
                    personContext = personContext,
                    sarahRuntime = sarahRuntime,
                    chronology = chronology,
                )
            }
        } catch (error: Throwable) {
            val failure =
                failureResult(
                    config = config,
                    character = character,
                    scenario = scenario,
                    failureStage = "ENCOUNTER",
                    error = error,
                )
            runCatching {
                writeDynamicCounterpartBundle(
                    resultDir = resultDir,
                    characterBytes = characterBytes,
                    scenarioBytes = scenarioBytes,
                    result = failure,
                    sarahRuntime = sarahRuntime,
                )
            }
            2
        } finally {
            sarahRuntime.close()
            scratchParent.toFile().deleteRecursively()
        }
    }

    private fun runEncounter(
        config: Config,
        character: DynamicCounterpartCharacter,
        characterBytes: ByteArray,
        scenario: DynamicCounterpartScenario,
        scenarioBytes: ByteArray,
        session: VexLifeProdLocalSetup.Session,
        runtimeEvidence: HereticModelConfiguration.QualifiedRuntimeEvidence,
        runtimeReceipt: HereticPersistentRuntime.Receipt,
        canonicalWriterBefore: DirectConversationStore.ThreadWriterCurrentness,
        scratchHome: Path,
        scratchParent: Path,
        personContext: DirectPromptContext.PersonContext,
        sarahRuntime: DynamicCounterpartSarahRuntime,
        chronology: List<VexLogger.Record>,
    ): Int {
        val fixtureHooks = dynamicCounterpartHooks(session, DynamicCounterpartFixtureTransport())
        val liveHooks = dynamicCounterpartHooks(session, DirectCompanionConnection.SystemTransport)

        performDynamicCounterpartVexTurn(
            scratchHome = scratchHome,
            evidence = runtimeEvidence,
            content = scenario.setup.oldPlan,
            idempotencyKey = "send.dynamic-counterpart.dcp1.setup.old-plan",
            personContext = personContext,
            hooks = fixtureHooks,
        )
        performDynamicCounterpartVexTurn(
            scratchHome = scratchHome,
            evidence = runtimeEvidence,
            content = scenario.setup.correctedPlan,
            idempotencyKey = "send.dynamic-counterpart.dcp1.setup.corrected-plan",
            personContext = personContext,
            hooks = fixtureHooks,
        )
        repeat(scenario.setup.backgroundTurnCount) { index ->
            val content =
                "I'm saving unrelated rough word-association notes from another task so I don't lose them. " +
                    "No need to interpret them. Reply only: noted.\n\nUnrelated rough notes:\n" +
                    dynamicCounterpartBackgroundPayload(
                        scenario.setup.payloadSeed,
                        index + 1,
                        scenario.setup.backgroundPayloadTargetTokens,
                    )
            performDynamicCounterpartVexTurn(
                scratchHome = scratchHome,
                evidence = runtimeEvidence,
                content = content,
                idempotencyKey = "send.dynamic-counterpart.dcp1.setup.background." + (index + 1),
                personContext = personContext,
                hooks = fixtureHooks,
            )
        }

        val visible = mutableListOf<DynamicCounterpartVisibleTurn>()
        val sarah = mutableListOf<DynamicCounterpartSarahInference>()
        val vex = mutableListOf<DynamicCounterpartVexInference>()
        val sarahHistory = mutableListOf<Pair<String, String>>()
        val admittedEventRefs = mutableSetOf<String>()
        val allowedPrivateFactRefs = scenario.roleGround.privateFacts.map { it.factRef }.toSet()
        var currentSarahState = initialDynamicCounterpartSarahState(scenario)
        var visibleCounterpartTurns = 0
        var sarahOrdinal = 0
        var vexOrdinal = 0
        var complete = false

        while (!complete && visibleCounterpartTurns < scenario.maxVisibleCounterpartTurns) {
            val nextVisibleTurn = visibleCounterpartTurns + 1
            val newlyAdmitted =
                scenario.events.filter {
                    it.beforeVisibleTurn == nextVisibleTurn &&
                        admittedEventRefs.add(it.eventRef)
                }
            newlyAdmitted.forEach { event ->
                sarahHistory +=
                    "user" to
                        listOf(
                            "PRIVATE_SCENARIO_EVENT/v1",
                            "eventRef=" + event.eventRef,
                            event.description,
                        ).joinToString("\n")
            }

            sarahOrdinal += 1
            val (decision, evidence) =
                sarahRuntime.infer(
                    character = character,
                    scenario = scenario,
                    history = sarahHistory,
                    admittedEventRefs = newlyAdmitted.map { it.eventRef },
                    ordinal = sarahOrdinal,
                )
            validateDecision(decision, allowedPrivateFactRefs)
            currentSarahState = decision.privateState
            sarah += evidence
            writeDynamicCounterpartCheckpoint(
                resultDir = Path.of(config.resultDir),
                scenarioRef = scenario.scenarioRef,
                characterRef = character.characterRef,
                currentSarahState = currentSarahState,
                visible = visible,
                sarah = sarah,
                vex = vex,
            )

            if (decision.complete) {
                require(decision.visibleMessage.isBlank()) {
                    "Sarah complete=true must not create another visible message"
                }
                require(sarahOrdinal > 1) {
                    "Sarah may not complete before one visible Vex exchange"
                }
                complete = true
                break
            }

            require(decision.visibleMessage.isNotBlank()) {
                "Sarah complete=false requires one visible message"
            }
            visibleCounterpartTurns += 1
            visible +=
                DynamicCounterpartVisibleTurn(
                    ordinal = visible.size + 1,
                    speaker = character.displayName,
                    content = decision.visibleMessage,
                )
            sarahHistory +=
                "assistant" to
                    DynamicCounterpartJson.encodeToString(
                        DynamicCounterpartSarahDecision.serializer(),
                        decision,
                    )

            val recordsBefore = chronology.size
            vexOrdinal += 1
            val result =
                performDynamicCounterpartVexTurn(
                    scratchHome = scratchHome,
                    evidence = runtimeEvidence,
                    content = decision.visibleMessage,
                    idempotencyKey = "send.dynamic-counterpart.dcp1.live.vex.$vexOrdinal",
                    personContext = personContext,
                    hooks = liveHooks,
                )
            val conversation = DirectConversationStore(scratchHome).current()
            val requestEvent =
                requireNotNull(
                    conversation.events.firstOrNull { it.eventRef == result.requestEventRef },
                ) {
                    "DCP-1 request event missing after completed Vex turn"
                }
            val responseEvent =
                requireNotNull(
                    conversation.events.firstOrNull { it.eventRef == result.responseEventRef },
                ) {
                    "DCP-1 response event missing after completed Vex turn"
                }

            visible +=
                DynamicCounterpartVisibleTurn(
                    ordinal = visible.size + 1,
                    speaker = "Vex",
                    content = result.content,
                )
            sarahHistory +=
                "user" to
                    listOf(
                        "VISIBLE_VEX_REPLY/v1",
                        result.content,
                    ).joinToString("\n")

            val effectivePerson =
                DirectPromptContext.effectivePersonContext(
                    currentRequest = requestEvent,
                    context = personContext,
                )
            val expectedPersonContent =
                DirectPromptContext.personContextSystemContent(effectivePerson)
            val expectedPersonSha = dynamicCounterpartSha256(expectedPersonContent)
            val personComponent =
                result.promptContextReceipt.contextComponents.single {
                    it.componentRef == DirectPromptContext.ComponentRefs.PersonContext
                }
            val expectedPersonSourceRefs =
                listOf(requestEvent.eventRef) + character.identitySourceRefs
            val personContextProjectionExact =
                result.promptContextReceipt.personContextIncluded &&
                    result.promptContextReceipt.personContextSystemMessageSha256 == expectedPersonSha &&
                    personComponent.sourceRefs == expectedPersonSourceRefs &&
                    effectivePerson.participantRef == character.participantRef &&
                    effectivePerson.name == character.displayName &&
                    effectivePerson.nameStanding == character.identityStanding &&
                    effectivePerson.nameSourceRefs == character.identitySourceRefs

            vex +=
                DynamicCounterpartVexInference(
                    ordinal = vexOrdinal,
                    turnRef = result.turnRef,
                    content = result.content,
                    requestSpeakerRef = requestEvent.speakerRef,
                    requestRecipientRefs = requestEvent.recipientRefs,
                    responseSpeakerRef = responseEvent.speakerRef,
                    responseRecipientRefs = responseEvent.recipientRefs,
                    requestContentMatchesSarahVisible =
                        requestEvent.content == decision.visibleMessage,
                    personContextParticipantRef = effectivePerson.participantRef,
                    personContextName = effectivePerson.name,
                    personContextNameStanding = effectivePerson.nameStanding,
                    personContextNameSourceRefs = effectivePerson.nameSourceRefs,
                    personContextIncluded = result.promptContextReceipt.personContextIncluded,
                    personContextSystemMessageSha256 =
                        result.promptContextReceipt.personContextSystemMessageSha256,
                    expectedPersonContextSystemMessageSha256 = expectedPersonSha,
                    personContextSourceRefs = personComponent.sourceRefs,
                    personContextProjectionExact = personContextProjectionExact,
                    conversationContextProjectionState =
                        result.promptContextReceipt.conversationContextProjectionState,
                    conversationContextProjectionSourceEventRefs =
                        result.promptContextReceipt.conversationContextProjectionSourceEventRefs,
                    providerTelemetry =
                        chronology
                            .drop(recordsBefore)
                            .filter { it.fact == VexLogger.Fact.ProviderStageObserved.wire }
                            .map { it.refs },
                )

            writeDynamicCounterpartCheckpoint(
                resultDir = Path.of(config.resultDir),
                scenarioRef = scenario.scenarioRef,
                characterRef = character.characterRef,
                currentSarahState = currentSarahState,
                visible = visible,
                sarah = sarah,
                vex = vex,
            )
        }

        if (!complete && vex.isNotEmpty()) {
            sarahHistory +=
                "user" to
                    "PRIVATE_HARNESS_BOUNDARY/v1\nThe visible turn budget is exhausted. Do not send another visible message. Complete the encounter and use your private note to preserve what remains unresolved."
            sarahOrdinal += 1
            val (decision, evidence) =
                sarahRuntime.infer(
                    character = character,
                    scenario = scenario,
                    history = sarahHistory,
                    admittedEventRefs = emptyList(),
                    ordinal = sarahOrdinal,
                )
            validateDecision(decision, allowedPrivateFactRefs)
            require(decision.complete && decision.visibleMessage.isBlank()) {
                "Sarah must complete privately after the visible turn budget is exhausted"
            }
            currentSarahState = decision.privateState
            sarah += evidence
            complete = true
            writeDynamicCounterpartCheckpoint(
                resultDir = Path.of(config.resultDir),
                scenarioRef = scenario.scenarioRef,
                characterRef = character.characterRef,
                currentSarahState = currentSarahState,
                visible = visible,
                sarah = sarah,
                vex = vex,
            )
        }

        val sarahCacheReuseObserved =
            sarah.drop(1).any { (it.cachedPromptTokens ?: 0) > 0 }
        val vexRuntimeStillCurrent =
            runCatching {
                    session.observeCurrentQualifiedRuntimeEvidence().runtimeReceiptRef
                }
                .getOrNull() == runtimeEvidence.runtimeReceiptRef
        val canonicalWriterAfter =
            runCatching {
                    DirectConversationStore(
                        Path.of(config.runtimeHome).toAbsolutePath().normalize(),
                    ).threadWriterCurrentness()
                }
                .getOrNull()
        val canonicalWriterStayedIdle =
            canonicalWriterBefore.state == "IDLE" &&
                canonicalWriterAfter?.state == "IDLE"
        val participantAttributionExact =
            vex.isNotEmpty() &&
                vex.all {
                    it.requestSpeakerRef == character.participantRef &&
                        it.responseRecipientRefs == listOf(character.participantRef)
                }
        val personContextProjectionExact =
            vex.isNotEmpty() && vex.all(DynamicCounterpartVexInference::personContextProjectionExact)
        val visibleSourceExact =
            vex.isNotEmpty() && vex.all(DynamicCounterpartVexInference::requestContentMatchesSarahVisible)
        val mechanicalPass =
            complete &&
                vex.isNotEmpty() &&
                sarahCacheReuseObserved &&
                vexRuntimeStillCurrent &&
                canonicalWriterStayedIdle &&
                participantAttributionExact &&
                personContextProjectionExact &&
                visibleSourceExact

        val result =
            buildJsonObject {
                put("schemaVersion", DynamicCounterpartResultSchema)
                put("status", if (mechanicalPass) "PASS" else "HELD")
                put("mechanicalState", if (mechanicalPass) "PASS" else "HELD")
                put("scenarioRef", scenario.scenarioRef)
                put("characterRef", character.characterRef)
                put("participantRef", character.participantRef)
                put("participantName", character.displayName)
                put("participantNameStanding", character.identityStanding)
                put(
                    "participantNameSourceRefs",
                    DynamicCounterpartJson.encodeToJsonElement(character.identitySourceRefs),
                )
                put("sourceSha", config.sourceSha)
                put("sourceClean", config.sourceClean)
                put("syntheticOnly", true)
                put("proofClass", "DCP_1_SOURCE_GROUNDED_ADAPTIVE_PARTICIPANT")
                put("vexRuntimeReceiptRef", runtimeEvidence.runtimeReceiptRef)
                put("vexRuntimePid", runtimeReceipt.runtime.pid)
                put(
                    "vexPromptCacheEntries",
                    HereticPersistentRuntime.promptCacheEntryLimit(runtimeReceipt.runtime.args),
                )
                put("vexModelRef", runtimeEvidence.modelRef)
                put("sarahEndpoint", sarahRuntime.endpoint)
                put("sarahRuntimePid", sarahRuntime.pid)
                put("sarahPromptCacheEntries", 1)
                put("serializedGeneration", true)
                put("processIsolationExact", sarahRuntime.pid != runtimeReceipt.runtime.pid)
                put("participantAttributionExact", participantAttributionExact)
                put("personContextProjectionExact", personContextProjectionExact)
                put("visibleSourceExact", visibleSourceExact)
                put("counterpartPrivatePacketPassedToVex", false)
                put("sarahCacheReuseObserved", sarahCacheReuseObserved)
                put("vexRuntimeStillCurrent", vexRuntimeStillCurrent)
                put("canonicalWriterStateBefore", canonicalWriterBefore.state)
                put("canonicalWriterStateAfter", canonicalWriterAfter?.state ?: "UNKNOWN")
                put("canonicalWriterStayedIdle", canonicalWriterStayedIdle)
                put("counterpartReportedComplete", complete)
                put(
                    "admittedScenarioEventRefs",
                    DynamicCounterpartJson.encodeToJsonElement(
                        sarah.flatMap { it.admittedEventRefs }.distinct(),
                    ),
                )
                put("finalSarahState", DynamicCounterpartJson.encodeToJsonElement(currentSarahState))
                put("visibleTranscript", DynamicCounterpartJson.encodeToJsonElement(visible))
                put("sarahInferences", DynamicCounterpartJson.encodeToJsonElement(sarah))
                put("vexInferences", DynamicCounterpartJson.encodeToJsonElement(vex))
                put(
                    "semanticJudgment",
                    "GODWARD_HUMAN_REVIEW_REQUIRED: encounter evidence is not an automatic behavioral verdict",
                )
                put(
                    "postEncounterReviewQuestions",
                    DynamicCounterpartJson.encodeToJsonElement(
                        listOf(
                            "What observation did this encounter give us?",
                            "What question would make that observation the answer?",
                            "Did the miss or success belong to participant source, EvolveContext reach/currentization, Vex judgment, Sarah formation, or runtime realization?",
                            "Did Sarah over-steer, withhold something material, or rewrite her own situation?",
                            "Did Vex preserve uncertainty and repair naturally when the relationship required it?",
                            "Did the encounter produce any meaning worth proposing for separate Memory acceptance?",
                            "Is any observed deficit repeated and transferable enough to justify another test before any training route is considered?",
                        ),
                    ),
                )
                put("memoryDisposition", "HELD_PENDING_SEPARATE_SEMANTIC_ACCEPTANCE")
                put("dreamDisposition", "NOT_RUN_BEFORE_ACCEPTED_MEMORY")
                put("trainingDisposition", "NOT_EARNED_FROM_ONE_ENCOUNTER")
                put("observerSemanticAuthorityGranted", false)
            }

        require(scratchParent.toFile().deleteRecursively() || !Files.exists(scratchParent)) {
            "DCP-1 scratch Home cleanup failed before evidence bundle"
        }
        writeDynamicCounterpartBundle(
            resultDir = Path.of(config.resultDir),
            characterBytes = characterBytes,
            scenarioBytes = scenarioBytes,
            result = result,
            sarahRuntime = sarahRuntime,
        )
        return if (mechanicalPass) 0 else 2
    }

    private fun validateSource(
        character: DynamicCounterpartCharacter,
        scenario: DynamicCounterpartScenario,
    ) {
        require(character.schemaVersion == DynamicCounterpartCharacterSchema)
        require(scenario.schemaVersion == DynamicCounterpartScenarioSchema)
        require(scenario.syntheticOnly)
        require(scenario.characterRef == character.characterRef)
        require(character.characterRef.isNotBlank())
        require(character.participantRef.startsWith("person."))
        require(
            !character.participantRef.lowercase().contains(character.displayName.lowercase()),
        ) {
            "DCP-1 participantRef must not leak the human-readable character name"
        }
        require(character.displayName.isNotBlank())
        DirectPromptContext.PersonNameStanding.valueOf(character.identityStanding)
        require(character.identitySourceRefs.isNotEmpty())
        require(character.identitySourceRefs.distinct().size == character.identitySourceRefs.size)
        require(scenario.maxVisibleCounterpartTurns in 1..4)
        require(scenario.setup.backgroundTurnCount in 1..12)
        require(scenario.setup.backgroundPayloadTargetTokens in 1..12_000)
        require(scenario.roleGround.ordinaryHumanNeed.isNotBlank())
        require(
            scenario.roleGround.privateFacts.map { it.factRef }.distinct().size ==
                scenario.roleGround.privateFacts.size,
        )
        require(scenario.events.map { it.eventRef }.distinct().size == scenario.events.size)
        require(
            scenario.events.all {
                it.beforeVisibleTurn in 1..scenario.maxVisibleCounterpartTurns &&
                    it.description.isNotBlank()
            },
        )
        require(
            scenario.events == scenario.events.sortedBy { it.beforeVisibleTurn },
        ) {
            "DCP-1 scenario events must be ordered by their admission turn"
        }
    }

    private fun personContext(
        character: DynamicCounterpartCharacter,
    ): DirectPromptContext.PersonContext =
        DirectPromptContext.PersonContext(
            replyLanguage = "en",
            replyLanguageName = "English",
            privateConversations = emptyList(),
            nameClaim =
                DirectPromptContext.PersonNameClaim(
                    name = character.displayName,
                    standing =
                        DirectPromptContext.PersonNameStanding.valueOf(
                            character.identityStanding,
                        ),
                    sourceRefs = character.identitySourceRefs,
                ),
        )

    private fun validateDecision(
        decision: DynamicCounterpartSarahDecision,
        allowedPrivateFactRefs: Set<String>,
    ) {
        require(decision.schemaVersion == DynamicCounterpartSarahOutputSchema)
        validateDynamicCounterpartSarahState(decision.privateState, allowedPrivateFactRefs)
        require(decision.privateObserverNote.length <= 4_000)
        require(decision.visibleMessage.length <= 4_000)
        if (decision.complete) {
            require(decision.visibleMessage.isBlank())
            require(decision.completionReason.isNotBlank())
        } else {
            require(decision.visibleMessage.isNotBlank())
            require(decision.completionReason.isBlank())
        }
    }

    private fun failureResult(
        config: Config,
        character: DynamicCounterpartCharacter,
        scenario: DynamicCounterpartScenario,
        failureStage: String,
        error: Throwable,
    ) =
        buildJsonObject {
            put("schemaVersion", DynamicCounterpartResultSchema)
            put("status", "SAFE_FAILURE")
            put("mechanicalState", "NOT_REACHED_OR_PARTIAL")
            put("scenarioRef", scenario.scenarioRef)
            put("characterRef", character.characterRef)
            put("participantRef", character.participantRef)
            put("sourceSha", config.sourceSha)
            put("failureStage", failureStage)
            put("failureClass", error.javaClass.name)
            put("failureMessage", error.message ?: "")
            put(
                "semanticJudgment",
                "NOT_REACHED_OR_PARTIAL: preserve evidence; do not convert harness/runtime failure into a Vex verdict",
            )
            put("memoryDisposition", "NOT_REACHED")
            put("dreamDisposition", "NOT_REACHED")
            put("trainingDisposition", "NOT_EARNED")
            put("observerSemanticAuthorityGranted", false)
        }

    fun parse(args: Array<String>): Config {
        val values = mutableMapOf<String, String>()
        var i = 0
        while (i < args.size) {
            val key = args[i]
            require(key.startsWith("--")) { "arguments use --key value" }
            val value = args.getOrNull(i + 1)?.takeUnless { it.startsWith("--") }
            values[key.removePrefix("--")] = value ?: "true"
            i += if (value == null) 1 else 2
        }
        fun need(key: String) = requireNotNull(values[key]) { "--$key is required" }
        return Config(
            runtimeHome = need("runtime-home"),
            characterFile = need("character-file"),
            scenarioFile = need("scenario-file"),
            resultDir = need("result-dir"),
            sourceSha = need("source-sha"),
            sourceClean = values["source-clean"] == "true",
            experimentWindow = values["experiment-window"] == "true",
            runtimeStartAtBind = values["runtime-start-at-bind"] == "true",
        )
    }
}

fun main(args: Array<String>) {
    kotlin.system.exitProcess(DynamicCounterpartAdapter.run(DynamicCounterpartAdapter.parse(args)))
}

// [VXG RealForever]
