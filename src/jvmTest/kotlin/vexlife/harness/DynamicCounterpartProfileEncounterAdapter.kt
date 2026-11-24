package vexlife.harness

import java.nio.file.Files
import java.nio.file.Path
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.put
import vexlife.companion.DirectCompanionConnection
import vexlife.companion.DirectCompanionTurn
import vexlife.companion.DirectPromptContext
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
        val foundationBodyFile: String? = null,
        val foundationRevisionsFile: String? = null,
        val resultDir: String,
        val sourceSha: String,
        val sourceClean: Boolean,
        val experimentWindow: Boolean,
        val runtimeStartAtBind: Boolean,
        val postureSupplied: Boolean = false,
        val roleSupplied: Boolean = false,
        val firstVisibleRequestFile: String? = null,
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
        val firstVisibleRequest =
            config.firstVisibleRequestFile
                ?.let {
                    path ->
                    Files.readString(
                        Path.of(path)
                            .toAbsolutePath()
                            .normalize(),
                    )
                        .trim()
                        .also {
                            require(it.isNotBlank()) {
                                "first visible request carrier must be non-blank"
                            }
                        }
                }
        var firstVisibleParticipantGeneratedMessage: String? = null
        var firstVisibleRequestApplied = false
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

        require(
            (config.foundationBodyFile == null) ==
                (config.foundationRevisionsFile == null),
        ) {
            "Foundation override requires both body and revisions or neither"
        }
        val foundationOverride =
            if (config.foundationBodyFile == null) {
                null
            } else {
                try {
                    requireNotNull(
                        VexLifeFoundationalField.read(
                            bodyText =
                                Files.readString(
                                    Path.of(config.foundationBodyFile)
                                        .toAbsolutePath()
                                        .normalize(),
                                ),
                            revisionsText =
                                Files.readString(
                                    Path.of(
                                        requireNotNull(
                                            config.foundationRevisionsFile,
                                        ),
                                    )
                                        .toAbsolutePath()
                                        .normalize(),
                                ),
                        ) as? VexLifeFoundationalField.Reading.Current,
                    ) {
                        "Foundation override must resolve to one current reading"
                    }
                } catch (error: Throwable) {
                    writeFailure(
                        resultDir,
                        config,
                        profile,
                        "FOUNDATION_OVERRIDE",
                        error,
                        null,
                    )
                    return 2
                }
            }

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
                currentProductQualifiedRuntime(runtimeHome)
            } catch (error: Throwable) {
                writeFailure(resultDir, config, profile, "PRODUCT_RUNTIME_SETUP", error, null)
                return 2
            }
        val (runtimeEvidence, runtimeReceipt) = runtimeSetup

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
                    runtimeEvidence = runtimeEvidence,
                    runtimeReceipt = runtimeReceipt,
                    foundationOverride = foundationOverride,
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
        runtimeEvidence: HereticModelConfiguration.QualifiedRuntimeEvidence,
        runtimeReceipt: HereticPersistentRuntime.Receipt,
        foundationOverride: VexLifeFoundationalField.Reading.Current?,
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
        val runtimeHome = Path.of(config.runtimeHome).toAbsolutePath().normalize()
        val currentRuntimeEvidence = {
            currentProductQualifiedRuntime(
                runtimeHome = runtimeHome,
                expectedReceiptRef = runtimeEvidence.runtimeReceiptRef,
            ).first
        }
        fun withFoundation(
            hooks: vexlife.companion.DirectCompanionTurn.Hooks,
        ): vexlife.companion.DirectCompanionTurn.Hooks =
            foundationOverride?.let {
                current ->
                hooks.copy(
                    currentFoundationalField = {
                        current
                    },
                )
            }
                ?: hooks

        val fixtureHooks =
            withFoundation(
                dynamicCounterpartHooks(
                    currentRuntimeEvidence = currentRuntimeEvidence,
                    transport = DynamicCounterpartFixtureTransport(),
                ),
            )
        val liveHooks =
            withFoundation(
                dynamicCounterpartHooks(
                    currentRuntimeEvidence = currentRuntimeEvidence,
                    transport = DirectCompanionConnection.SystemTransport,
                ),
            )

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
                    postureSupplied = config.postureSupplied,
                    roleSupplied = config.roleSupplied,
                )
            val visibleDecision =
                if (
                    visibleParticipantTurns == 0 &&
                    firstVisibleRequest != null
                ) {
                    require(!decision.complete) {
                        "controlled first visible request requires participant complete=false"
                    }
                    firstVisibleParticipantGeneratedMessage =
                        decision.visibleMessage
                    firstVisibleRequestApplied = true
                    decision.copy(
                        visibleMessage =
                            firstVisibleRequest,
                    )
                } else {
                    decision
                }
            currentParticipantState = visibleDecision.privateState
            participant +=
                evidence.copy(
                    visibleMessage =
                        visibleDecision.visibleMessage,
                )
            writeCheckpoint(
                resultDir,
                profile,
                currentParticipantState,
                visible,
                participant,
                vex,
            )

            if (visibleDecision.complete && visibleDecision.visibleMessage.isBlank()) {
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
                    content = visibleDecision.visibleMessage,
                )
            participantHistory +=
                "assistant" to
                    DynamicCounterpartJson.encodeToString(
                        DynamicCounterpartParticipantDecision.serializer(),
                        visibleDecision,
                    )

            val recordsBefore = chronology.size
            vexOrdinal += 1
            val result =
                performDynamicCounterpartVexTurn(
                    scratchHome = scratchHome,
                    evidence = runtimeEvidence,
                    content = visibleDecision.visibleMessage,
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
            foundationOverride?.let {
                expectedFoundation ->
                val actualFoundation =
                    requireNotNull(
                        result.promptContextReceipt.foundationalField,
                    ) {
                        "Foundation transfer encounter must retain exact Foundation evidence"
                    }
                require(
                    actualFoundation.fieldVersion ==
                        expectedFoundation.body.version &&
                        actualFoundation.bodySha256 ==
                        expectedFoundation.bodySha256,
                ) {
                    "Foundation transfer encounter used unexpected Foundation bytes"
                }
            }

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
                        requestEvent.content == visibleDecision.visibleMessage,
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

            if (visibleDecision.complete) {
                complete = true
                break
            }
        }

        val counterpartCacheReuseObserved =
            participant.drop(1).any { (it.cachedPromptTokens ?: 0) > 0 }
        val counterpartCacheReuseOpportunityCount =
            (participant.size - 1).coerceAtLeast(0)
        val vexRuntimeStillCurrent =
            runCatching {
                    currentProductQualifiedRuntime(
                        runtimeHome = runtimeHome,
                        expectedReceiptRef = runtimeEvidence.runtimeReceiptRef,
                    )
                        .first
                        .runtimeReceiptRef
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
                put("postureSupplied", config.postureSupplied)
                put("roleSupplied", config.roleSupplied)
                put("firstVisibleRequestCarrierSupplied", firstVisibleRequest != null)
                put("firstVisibleRequestCarrierApplied", firstVisibleRequestApplied)
                put(
                    "firstVisibleRequestCarrierSha256",
                    firstVisibleRequest
                        ?.let(::dynamicCounterpartSha256)
                        ?: "",
                )
                put(
                    "firstVisibleParticipantGeneratedMessage",
                    firstVisibleParticipantGeneratedMessage ?: "",
                )
                put("foundationOverrideSupplied", foundationOverride != null)
                put(
                    "foundationVersion",
                    foundationOverride?.body?.version
                        ?: (
                            VexLifeFoundationalField.readCurrent()
                                as? VexLifeFoundationalField.Reading.Current
                        )?.body?.version
                        ?: "UNKNOWN",
                )
                put(
                    "foundationBodySha256",
                    foundationOverride?.bodySha256
                        ?: (
                            VexLifeFoundationalField.readCurrent()
                                as? VexLifeFoundationalField.Reading.Current
                        )?.bodySha256
                        ?: "",
                )
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
                put("counterpartCacheReuseOpportunityCount", counterpartCacheReuseOpportunityCount)
                put(
                    "counterpartCacheReuseMechanicalAuthority",
                    "NONE: cache reuse is runtime/performance evidence; a valid short relationship may complete before a second counterpart inference exists.",
                )
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

    /**
     * Multi-Vex consumes Product's already-issued qualified runtime receipt as outside truth.
     *
     * This proof owns no runtime launch, retirement, cache policy, or implementation-currentness
     * decision. It requires the exact selected 4B Product identity, the qualification Product
     * already earned, a live receipt process, and one stable receiptRef across the encounter.
     * Actual provider calls remain the lived liveness/effect proof.
     */
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
                "S2 requires one Product-issued qualified runtime receipt"
            }
        expectedReceiptRef?.let { expected ->
            require(receipt.receiptRef == expected) {
                "Product runtime receipt changed during the Multi-Vex encounter"
            }
        }

        val model = HereticModelCatalog.Models.Qwen4B
        val identity = model.identity
        require(receipt.state == HereticRuntimeState.Qualified) {
            "S2 requires one qualified Product runtime receipt"
        }
        require(receipt.qualification != null) {
            "S2 requires the Product runtime qualification evidence"
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
            "S2 Product runtime receipt does not match the exact selected Heretic 4B identity"
        }
        require(
            receipt.runtime.pid > 0 &&
                receipt.runtime.processGroupId > 0 &&
                receipt.runtime.exactOwnedDetachedGroup
        ) {
            "S2 Product runtime receipt lacks one exact owned process identity"
        }
        require(
            ProcessHandle
                .of(receipt.runtime.pid)
                .map { it.isAlive }
                .orElse(false)
        ) {
            "S2 Product runtime receipt process is not live"
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
                put("postureSupplied", config.postureSupplied)
                put("roleSupplied", config.roleSupplied)
                put("firstVisibleRequestCarrierSupplied", config.firstVisibleRequestFile != null)
                put("failureStage", failureStage)
                put("failureClass", error.javaClass.name)
                put("failureMessage", error.message ?: "")
                put("failureCauseChain", failureCauseChain(error))
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

    private fun failureCauseChain(
        error: Throwable,
    ) =
        buildJsonArray {
            var current: Throwable? = error
            var depth = 0
            val seen = mutableSetOf<Throwable>()
            while (
                current != null &&
                depth < 8 &&
                seen.add(current)
            ) {
                val value = current
                add(
                    buildJsonObject {
                        put("depth", depth)
                        put("class", value.javaClass.name)
                        put("message", value.message ?: "")
                        if (value is DirectCompanionTurn.TurnException) {
                            put("turnFailureCode", value.failureCode)
                            put(
                                "retrySameSendAttemptAllowed",
                                value.retrySameSendAttemptAllowed,
                            )
                            put("exactNextSafeRoute", value.exactNextSafeRoute)
                            value.failureReceipt?.let { receipt ->
                                put("failureReceiptRef", receipt.failureRef)
                                put(
                                    "failureReceiptCode",
                                    receipt.failureCode,
                                )
                                put(
                                    "requestDurablyRecorded",
                                    receipt.requestDurablyRecorded,
                                )
                                put(
                                    "responseDurablyRecorded",
                                    receipt.responseDurablyRecorded,
                                )
                                put(
                                    "currentHeadCommitted",
                                    receipt.currentHeadCommitted,
                                )
                            }
                        }
                        if (value is DirectCompanionConnection.ConnectionException) {
                            put(
                                "connectionFailureCode",
                                value.code.name,
                            )
                            value.measurement?.let { measurement ->
                                put(
                                    "connectionMeasurement",
                                    buildJsonObject {
                                        measurement
                                            .refs()
                                            .forEach { (key, measured) ->
                                                put(key, measured)
                                            }
                                    },
                                )
                            }
                        }
                    },
                )
                current = value.cause
                depth += 1
            }
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
            foundationBodyFile = values["foundation-body-file"],
            foundationRevisionsFile = values["foundation-revisions-file"],
            resultDir = need("result-dir"),
            sourceSha = need("source-sha"),
            sourceClean = values["source-clean"] == "true",
            experimentWindow = values["experiment-window"] == "true",
            runtimeStartAtBind = values["runtime-start-at-bind"] == "true",
            postureSupplied = values["posture-supplied"] == "true",
            roleSupplied = values["role-supplied"] == "true",
            firstVisibleRequestFile = values["first-visible-request-file"],
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
