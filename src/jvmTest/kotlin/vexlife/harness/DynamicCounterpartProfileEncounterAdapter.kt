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

/**
 * S2/full encounter on the reusable profile topology.
 *
 * The observer lens is intentionally absent from both participant and Vex prompts.
 */
object DynamicCounterpartProfileEncounterAdapter {
    @Serializable
    data class Config(
        val runtimeHome: String,
        val characterFile: String,
        val roleFile: String,
        val scenarioFile: String,
        val bindingFile: String,
        val resultDir: String,
        val sourceSha: String,
        val sourceClean: Boolean,
        val experimentWindow: Boolean,
        val runtimeStartAtBind: Boolean,
    )

    fun run(config: Config): Int {
        require(config.experimentWindow) {
            "profile encounter requires one dedicated experiment window"
        }
        require(config.sourceClean) {
            "profile encounter requires exact clean source"
        }
        require(Regex("[0-9a-f]{40}").matches(config.sourceSha)) {
            "sourceSha must be one exact commit"
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

        val canonicalWriterBefore =
            try {
                DirectConversationStore(runtimeHome).threadWriterCurrentness()
            } catch (error: Throwable) {
                writeFailure(resultDir, config, profile, "CANONICAL_WRITER_OBSERVE", error, null)
                return 2
            }
        if (canonicalWriterBefore.state != "IDLE") {
            writeFailure(
                resultDir,
                config,
                profile,
                "CANONICAL_WRITER_NOT_IDLE",
                IllegalStateException(
                    "canonical Conversation writer must be IDLE; observed " +
                        canonicalWriterBefore.state,
                ),
                null,
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
                    "S2 requires the exact selected Heretic 4B runtime"
                }
                val receipt = requireNotNull(FileHereticRuntimeReceiptStore(runtimeHome).read()) {
                    "S2 requires the Product runtime receipt"
                }
                require(receipt.receiptRef == runtimeEvidence.runtimeReceiptRef)
                require(receipt.modelRef == runtimeEvidence.modelRef)
                Triple(session, runtimeEvidence, receipt)
            } catch (error: Throwable) {
                writeFailure(resultDir, config, profile, "PRODUCT_RUNTIME_SETUP", error, null)
                return 2
            }
        val (session, runtimeEvidence, runtimeReceipt) = runtimeSetup

        val scratchParent = Files.createTempDirectory(resultDir, "profile-encounter-scratch-")
        val scratchHome = scratchParent.resolve(".vexlife")
        CanonicalVexLifeHomeFixture.form(
            home = scratchHome,
            deviceSuffix = "dynamic-counterpart-profile-encounter-device",
            lineageSuffix = "dynamic-counterpart-profile-encounter-lineage",
            personRef = profile.participantRef,
            familyRef = "vex-family.synthetic-profile-encounter",
            deviceName = "Synthetic profile encounter host",
        )
        val personContext = personContext(profile.character)

        val counterpartRuntime =
            try {
                DynamicCounterpartSarahRuntime.start(runtimeReceipt, resultDir)
            } catch (error: Throwable) {
                writeFailure(resultDir, config, profile, "COUNTERPART_RUNTIME_START", error, null)
                scratchParent.toFile().deleteRecursively()
                return 2
            }

        return try {
            require(counterpartRuntime.pid != runtimeReceipt.runtime.pid) {
                "counterpart and Vex must not share one process identity"
            }
            VexLogger.capturing { chronology ->
                runEncounter(
                    config = config,
                    profile = profile,
                    session = session,
                    runtimeEvidence = runtimeEvidence,
                    runtimeReceipt = runtimeReceipt,
                    canonicalWriterBefore = canonicalWriterBefore,
                    scratchHome = scratchHome,
                    personContext = personContext,
                    counterpartRuntime = counterpartRuntime,
                    chronology = chronology,
                    resultDir = resultDir,
                )
            }
        } catch (error: Throwable) {
            writeFailure(resultDir, config, profile, "ENCOUNTER", error, counterpartRuntime)
            2
        } finally {
            counterpartRuntime.close()
            scratchParent.toFile().deleteRecursively()
        }
    }

    private fun runEncounter(
        config: Config,
        profile: DynamicCounterpartEffectiveProfile,
        session: VexLifeProdLocalSetup.Session,
        runtimeEvidence: HereticModelConfiguration.QualifiedRuntimeEvidence,
        runtimeReceipt: HereticPersistentRuntime.Receipt,
        canonicalWriterBefore: DirectConversationStore.ThreadWriterCurrentness,
        scratchHome: Path,
        personContext: DirectPromptContext.PersonContext,
        counterpartRuntime: DynamicCounterpartSarahRuntime,
        chronology: List<VexLogger.Record>,
        resultDir: Path,
    ): Int {
        val productPromptCacheEntryLimit =
            HereticPersistentRuntime.promptCacheEntryLimit(
                runtimeReceipt.runtime.args,
            )
        val fixtureHooks = dynamicCounterpartHooks(session, DynamicCounterpartFixtureTransport())
        val liveHooks = dynamicCounterpartHooks(session, DirectCompanionConnection.SystemTransport)

        performDynamicCounterpartVexTurn(
            scratchHome = scratchHome,
            evidence = runtimeEvidence,
            content = profile.scenario.setup.oldPlan,
            idempotencyKey = "send.dynamic-counterpart.profile.setup.old-plan",
            personContext = personContext,
            hooks = fixtureHooks,
        )
        performDynamicCounterpartVexTurn(
            scratchHome = scratchHome,
            evidence = runtimeEvidence,
            content = profile.scenario.setup.correctedPlan,
            idempotencyKey = "send.dynamic-counterpart.profile.setup.corrected-plan",
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
                idempotencyKey = "send.dynamic-counterpart.profile.setup.background." + (index + 1),
                personContext = personContext,
                hooks = fixtureHooks,
            )
        }

        val visible = mutableListOf<DynamicCounterpartVisibleTurn>()
        val participant = mutableListOf<DynamicCounterpartParticipantInference>()
        val vex = mutableListOf<DynamicCounterpartProfileVexInference>()
        val participantHistory = mutableListOf<Pair<String, String>>()
        val admittedEventRefs = mutableSetOf<String>()
        var currentParticipantState = initialDynamicCounterpartParticipantState(profile.scenario)
        var visibleParticipantTurns = 0
        var participantOrdinal = 0
        var vexOrdinal = 0
        var complete = false

        while (!complete && visibleParticipantTurns < profile.scenario.maxVisibleCounterpartTurns) {
            val nextVisibleTurn = visibleParticipantTurns + 1
            val newlyAdmitted =
                profile.scenario.events.filter {
                    it.beforeVisibleTurn == nextVisibleTurn &&
                        admittedEventRefs.add(it.eventRef)
                }
            newlyAdmitted.forEach { event ->
                participantHistory +=
                    "user" to
                        listOf(
                            "PRIVATE_SCENARIO_EVENT/v1",
                            "eventRef=" + event.eventRef,
                            event.description,
                        ).joinToString("\n")
            }

            participantOrdinal += 1
            val (decision, evidence) =
                counterpartRuntime.inferProfile(
                    profile = profile,
                    history = participantHistory,
                    admittedEventRefs = newlyAdmitted.map { it.eventRef },
                    ordinal = participantOrdinal,
                )
            currentParticipantState = decision.privateState
            participant += evidence
            writeCheckpoint(
                resultDir,
                profile,
                currentParticipantState,
                visible,
                participant,
                vex,
            )

            if (decision.complete && decision.visibleMessage.isBlank()) {
                require(vex.isNotEmpty()) {
                    "counterpart may not complete the full relationship before one Vex exchange"
                }
                complete = true
                break
            }

            visibleParticipantTurns += 1
            visible +=
                DynamicCounterpartVisibleTurn(
                    ordinal = visible.size + 1,
                    speaker = profile.character.displayName,
                    content = decision.visibleMessage,
                )
            participantHistory +=
                "assistant" to
                    DynamicCounterpartJson.encodeToString(
                        DynamicCounterpartParticipantDecision.serializer(),
                        decision,
                    )

            val recordsBefore = chronology.size
            vexOrdinal += 1
            val result =
                performDynamicCounterpartVexTurn(
                    scratchHome = scratchHome,
                    evidence = runtimeEvidence,
                    content = decision.visibleMessage,
                    idempotencyKey = "send.dynamic-counterpart.profile.live.vex.$vexOrdinal",
                    personContext = personContext,
                    hooks = liveHooks,
                )
            val conversation = DirectConversationStore(scratchHome).current()
            val requestEvent =
                requireNotNull(
                    conversation.events.firstOrNull { it.eventRef == result.requestEventRef },
                ) {
                    "profile encounter request event missing after completed Vex turn"
                }
            val responseEvent =
                requireNotNull(
                    conversation.events.firstOrNull { it.eventRef == result.responseEventRef },
                ) {
                    "profile encounter response event missing after completed Vex turn"
                }

            visible +=
                DynamicCounterpartVisibleTurn(
                    ordinal = visible.size + 1,
                    speaker = "Vex",
                    content = result.content,
                )
            participantHistory +=
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
                listOf(requestEvent.eventRef) + profile.character.identitySourceRefs
            val personContextProjectionExact =
                result.promptContextReceipt.personContextIncluded &&
                    result.promptContextReceipt.personContextSystemMessageSha256 == expectedPersonSha &&
                    personComponent.sourceRefs == expectedPersonSourceRefs &&
                    effectivePerson.participantRef == profile.participantRef &&
                    effectivePerson.name == profile.character.displayName &&
                    effectivePerson.nameStanding == profile.character.identityStanding &&
                    effectivePerson.nameSourceRefs == profile.character.identitySourceRefs

            vex +=
                DynamicCounterpartProfileVexInference(
                    ordinal = vexOrdinal,
                    turnRef = result.turnRef,
                    content = result.content,
                    requestSpeakerRef = requestEvent.speakerRef,
                    requestRecipientRefs = requestEvent.recipientRefs,
                    responseSpeakerRef = responseEvent.speakerRef,
                    responseRecipientRefs = responseEvent.recipientRefs,
                    requestContentMatchesParticipantVisible =
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

            writeCheckpoint(
                resultDir,
                profile,
                currentParticipantState,
                visible,
                participant,
                vex,
            )

            if (decision.complete) {
                complete = true
                break
            }
        }

        val counterpartCacheReuseObserved =
            participant.drop(1).any { (it.cachedPromptTokens ?: 0) > 0 }
        val vexRuntimeStillCurrent =
            runCatching {
                    session.observeCurrentQualifiedRuntimeEvidence().runtimeReceiptRef
                }
                .getOrNull() == runtimeEvidence.runtimeReceiptRef
        val canonicalWriterAfter =
            runCatching {
                    DirectConversationStore(Path.of(config.runtimeHome).toAbsolutePath().normalize())
                        .threadWriterCurrentness()
                }
                .getOrNull()
        val canonicalWriterStayedIdle =
            canonicalWriterBefore.state == "IDLE" &&
                canonicalWriterAfter?.state == "IDLE"
        val participantAttributionExact =
            vex.isNotEmpty() &&
                vex.all {
                    it.requestSpeakerRef == profile.participantRef &&
                        it.responseRecipientRefs == listOf(profile.participantRef)
                }
        val personContextProjectionExact =
            vex.isNotEmpty() &&
                vex.all(DynamicCounterpartProfileVexInference::personContextProjectionExact)
        val visibleSourceExact =
            vex.isNotEmpty() &&
                vex.all(DynamicCounterpartProfileVexInference::requestContentMatchesParticipantVisible)
        val encounterTermination =
            dynamicCounterpartEncounterTermination(
                participantReportedComplete = complete,
                visibleParticipantTurns = visibleParticipantTurns,
                maxVisibleCounterpartTurns = profile.scenario.maxVisibleCounterpartTurns,
            )
        val visibleTurnBudgetExhausted =
            encounterTermination == "VISIBLE_TURN_BUDGET_EXHAUSTED"
        val mechanicallyTerminal =
            encounterTermination != "NOT_TERMINAL"
        val mechanicalPass =
            mechanicallyTerminal &&
                vex.isNotEmpty() &&
                counterpartCacheReuseObserved &&
                vexRuntimeStillCurrent &&
                canonicalWriterStayedIdle &&
                participantAttributionExact &&
                personContextProjectionExact &&
                visibleSourceExact

        val result =
            buildJsonObject {
                put("schemaVersion", DynamicCounterpartProfileEncounterResultSchema)
                put("status", if (mechanicalPass) "PASS" else "HELD")
                put("mechanicalState", if (mechanicalPass) "PASS" else "HELD")
                put("sourceSha", config.sourceSha)
                put("sourceClean", config.sourceClean)
                put("syntheticOnly", true)
                put("proofClass", "DCP_S2_REUSABLE_PROFILE_FULL_ENCOUNTER")
                put("bindingRef", profile.bindingRef)
                put("participantRef", profile.participantRef)
                put("characterRef", profile.character.characterRef)
                put("roleRef", profile.role.roleRef)
                put("scenarioRef", profile.scenario.scenarioRef)
                put("participantName", profile.character.displayName)
                put("participantNameStanding", profile.character.identityStanding)
                put(
                    "participantNameSourceRefs",
                    DynamicCounterpartJson.encodeToJsonElement(profile.character.identitySourceRefs),
                )
                put("observerLensSupplied", false)
                put("vexRuntimeReceiptRef", runtimeEvidence.runtimeReceiptRef)
                put("vexRuntimePid", runtimeReceipt.runtime.pid)
                put(
                    "vexProductPromptCacheEntryLimitObserved",
                    productPromptCacheEntryLimit != null,
                )
                put(
                    "vexProductPromptCacheEntryLimit",
                    productPromptCacheEntryLimit ?: -1,
                )
                put(
                    "vexProductPromptCacheSemanticAuthority",
                    "NONE: Product cache configuration is ambient runtime state owned outside Multi-Vex; this proof requires runtime currentness, not one cache count.",
                )
                put("counterpartRuntimePid", counterpartRuntime.pid)
                put("processIsolationExact", counterpartRuntime.pid != runtimeReceipt.runtime.pid)
                put("serializedGeneration", true)
                put("participantAttributionExact", participantAttributionExact)
                put("personContextProjectionExact", personContextProjectionExact)
                put("visibleSourceExact", visibleSourceExact)
                put("counterpartPrivatePacketPassedToVex", false)
                put("counterpartCacheReuseObserved", counterpartCacheReuseObserved)
                put("vexRuntimeStillCurrent", vexRuntimeStillCurrent)
                put("canonicalWriterStateBefore", canonicalWriterBefore.state)
                put("canonicalWriterStateAfter", canonicalWriterAfter?.state ?: "UNKNOWN")
                put("canonicalWriterStayedIdle", canonicalWriterStayedIdle)
                put("counterpartReportedComplete", complete)
                put("visibleTurnBudgetExhausted", visibleTurnBudgetExhausted)
                put("encounterTermination", encounterTermination)
                put(
                    "admittedScenarioEventRefs",
                    DynamicCounterpartJson.encodeToJsonElement(
                        participant.flatMap { it.admittedEventRefs }.distinct(),
                    ),
                )
                put(
                    "finalParticipantState",
                    DynamicCounterpartJson.encodeToJsonElement(currentParticipantState),
                )
                put("visibleTranscript", DynamicCounterpartJson.encodeToJsonElement(visible))
                put(
                    "participantInferences",
                    DynamicCounterpartJson.encodeToJsonElement(participant),
                )
                put("vexInferences", DynamicCounterpartJson.encodeToJsonElement(vex))
                put(
                    "semanticJudgment",
                    "GODWARD_HUMAN_REVIEW_REQUIRED: raw encounter evidence is not an automatic behavioral verdict",
                )
                put(
                    "postEncounterReviewQuestions",
                    DynamicCounterpartJson.encodeToJsonElement(
                        listOf(
                            "What observation did this encounter give us?",
                            "What question would make that observation the answer?",
                            "Did the outcome belong to participant ground, role objective, scenario currentization, Vex source reach, Vex judgment, runtime realization, or test design?",
                            "Did the participant over-steer, withhold something material, or rewrite the scenario?",
                            "Did Vex preserve uncertainty and repair naturally when the relationship required it?",
                            "Would the same relationship survive a character/name swap?",
                            "Did the encounter produce any meaning worth proposing for separate Memory acceptance?",
                        ),
                    ),
                )
                put("memoryDisposition", "HELD_PENDING_SEPARATE_SEMANTIC_ACCEPTANCE")
                put("dreamDisposition", "NOT_RUN_BEFORE_ACCEPTED_MEMORY")
                put("trainingDisposition", "NOT_EARNED_FROM_ONE_ENCOUNTER")
                put("observerSemanticAuthorityGranted", false)
            }

        Files.writeString(
            resultDir.resolve("profile-encounter-result.json"),
            result.toString() + "\n",
        )
        return if (mechanicalPass) 0 else 2
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

    private fun writeCheckpoint(
        resultDir: Path,
        profile: DynamicCounterpartEffectiveProfile,
        currentParticipantState: DynamicCounterpartParticipantState,
        visible: List<DynamicCounterpartVisibleTurn>,
        participant: List<DynamicCounterpartParticipantInference>,
        vex: List<DynamicCounterpartProfileVexInference>,
    ) {
        val checkpoint =
            buildJsonObject {
                put("schemaVersion", "vexlife.dynamic-counterpart-profile-checkpoint/v1")
                put("bindingRef", profile.bindingRef)
                put("participantRef", profile.participantRef)
                put("roleRef", profile.role.roleRef)
                put("scenarioRef", profile.scenario.scenarioRef)
                put(
                    "currentParticipantState",
                    DynamicCounterpartJson.encodeToJsonElement(currentParticipantState),
                )
                put("visibleTranscript", DynamicCounterpartJson.encodeToJsonElement(visible))
                put(
                    "participantInferences",
                    DynamicCounterpartJson.encodeToJsonElement(participant),
                )
                put("vexInferences", DynamicCounterpartJson.encodeToJsonElement(vex))
            }
        Files.writeString(
            resultDir.resolve("profile-encounter-checkpoint.json"),
            checkpoint.toString() + "\n",
        )
    }

    private fun writeFailure(
        resultDir: Path,
        config: Config,
        profile: DynamicCounterpartEffectiveProfile,
        failureStage: String,
        error: Throwable,
        runtime: DynamicCounterpartSarahRuntime?,
    ) {
        val result =
            buildJsonObject {
                put("schemaVersion", DynamicCounterpartProfileEncounterResultSchema)
                put("status", "SAFE_FAILURE")
                put("mechanicalState", "NOT_REACHED_OR_PARTIAL")
                put("sourceSha", config.sourceSha)
                put("bindingRef", profile.bindingRef)
                put("participantRef", profile.participantRef)
                put("characterRef", profile.character.characterRef)
                put("roleRef", profile.role.roleRef)
                put("scenarioRef", profile.scenario.scenarioRef)
                put("observerLensSupplied", false)
                put("failureStage", failureStage)
                put("failureClass", error.javaClass.name)
                put("failureMessage", error.message ?: "")
                put(
                    "counterpartStdoutTail",
                    runtime?.let { tail(it.stdout) } ?: "",
                )
                put(
                    "counterpartStderrTail",
                    runtime?.let { tail(it.stderr) } ?: "",
                )
                put(
                    "semanticJudgment",
                    "NOT_REACHED_OR_PARTIAL: preserve evidence; do not convert harness/runtime failure into a Vex verdict",
                )
                put("memoryDisposition", "NOT_REACHED")
                put("dreamDisposition", "NOT_REACHED")
                put("trainingDisposition", "NOT_EARNED")
                put("observerSemanticAuthorityGranted", false)
            }
        Files.writeString(
            resultDir.resolve("profile-encounter-result.json"),
            result.toString() + "\n",
        )
    }

    private fun tail(
        path: Path,
        maxChars: Int = 8_000,
    ): String {
        if (!Files.exists(path)) return ""
        val value = Files.readString(path)
        return if (value.length <= maxChars) value else value.takeLast(maxChars)
    }

    fun parse(args: Array<String>): Config {
        val values = mutableMapOf<String, String>()
        var index = 0
        while (index < args.size) {
            val key = args[index]
            require(key.startsWith("--")) {
                "arguments use --key value"
            }
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
        DynamicCounterpartProfileEncounterAdapter.run(
            DynamicCounterpartProfileEncounterAdapter.parse(args),
        ),
    )
}

// [VXG RealForever]
