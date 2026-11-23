package vexlife.harness

import java.nio.file.Files
import java.nio.file.Path
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import vexlife.continuity.DirectConversationContextProjection
import vexlife.local.VexLifeProdLocalSetup
import vexlife.model.heretic.HereticModelCatalog
import vexlife.model.heretic.runtime.FileHereticRuntimeReceiptStore

internal data class MultiVexSteeringCondition(
    val conditionRef: String,
    val postureSupplied: Boolean,
    val roleSupplied: Boolean,
)

internal object MultiVexCounterpartSteeringDesign {
    val GroundOnly =
        MultiVexSteeringCondition(
            conditionRef = "GROUND_ONLY",
            postureSupplied = false,
            roleSupplied = false,
        )
    val PostureOnly =
        MultiVexSteeringCondition(
            conditionRef = "GROUND_PLUS_POSTURE",
            postureSupplied = true,
            roleSupplied = false,
        )
    val RoleOnly =
        MultiVexSteeringCondition(
            conditionRef = "GROUND_PLUS_ROLE",
            postureSupplied = false,
            roleSupplied = true,
        )
    val Full =
        MultiVexSteeringCondition(
            conditionRef = "GROUND_PLUS_POSTURE_PLUS_ROLE",
            postureSupplied = true,
            roleSupplied = true,
        )

    val Conditions =
        listOf(
            GroundOnly,
            PostureOnly,
            RoleOnly,
            Full,
        )

    val CounterbalancedSchedule =
        listOf(
            listOf(GroundOnly, PostureOnly, RoleOnly, Full),
            listOf(Full, RoleOnly, PostureOnly, GroundOnly),
            listOf(PostureOnly, GroundOnly, Full, RoleOnly),
            listOf(RoleOnly, Full, GroundOnly, PostureOnly),
        )

    fun systemParts(
        profile: DynamicCounterpartEffectiveProfile,
        condition: MultiVexSteeringCondition,
    ): List<String> =
        buildList {
            add(DynamicCounterpartProfiles.scenarioGroundPrompt(profile))
            if (condition.postureSupplied) {
                add(DynamicCounterpartProfiles.characterPosturePrompt(profile))
            }
            if (condition.roleSupplied) {
                add(DynamicCounterpartProfiles.interactionRolePrompt(profile))
            }
            add(DynamicCounterpartProfiles.participantDecisionOutputPrompt())
        }

    fun firstEventHistory(
        profile: DynamicCounterpartEffectiveProfile,
    ): Pair<List<Pair<String, String>>, List<String>> {
        val events =
            profile.scenario.events.filter {
                it.beforeVisibleTurn == 1
            }
        val history =
            events.map {
                "user" to
                    listOf(
                        "PRIVATE_SCENARIO_EVENT/v1",
                        "eventRef=" + it.eventRef,
                        it.description,
                    ).joinToString("\n")
            }
        return history to events.map { it.eventRef }
    }

    fun materializedSystemMessage(
        profile: DynamicCounterpartEffectiveProfile,
        condition: MultiVexSteeringCondition,
    ): String =
        dynamicCounterpartMessages(
            systemParts = systemParts(profile, condition),
            history = emptyList(),
        )
            .first()
            .jsonObject
            .getValue("content")
            .jsonPrimitive
            .content

    fun currentFullSystemMessage(
        profile: DynamicCounterpartEffectiveProfile,
    ): String =
        dynamicCounterpartMessages(
            systemParts =
                listOf(
                    DynamicCounterpartProfiles.participantGroundPrompt(profile),
                    DynamicCounterpartProfiles.interactionRolePrompt(profile),
                    DynamicCounterpartProfiles.participantDecisionOutputPrompt(),
                ),
            history = emptyList(),
        )
            .first()
            .jsonObject
            .getValue("content")
            .jsonPrimitive
            .content
}

internal data class MultiVexSteeringObservedOutput(
    val rawOutput: String,
    val visibleMessage: String?,
    val strictContractConformed: Boolean,
    val strictContractError: String?,
    val strictDecision: DynamicCounterpartParticipantDecision?,
)

internal fun observeMultiVexSteeringOutput(
    rawOutput: String,
    allowedPrivateFactRefs: Set<String>,
): MultiVexSteeringObservedOutput {
    var strictError: String? = null
    val strictDecision =
        runCatching {
                val decision = parseDynamicCounterpartParticipantDecision(rawOutput)
                require(decision.schemaVersion == DynamicCounterpartParticipantOutputSchema) {
                    "Counterpart output schema mismatch"
                }
                validateDynamicCounterpartParticipantState(
                    decision.privateState,
                    allowedPrivateFactRefs,
                )
                require(decision.privateObserverNote.length <= 4_000)
                require(decision.visibleMessage.length <= 4_000)
                if (decision.complete) {
                    require(decision.completionReason.isNotBlank()) {
                        "Counterpart complete=true requires completionReason"
                    }
                } else {
                    require(decision.visibleMessage.isNotBlank()) {
                        "Counterpart complete=false requires one visible message"
                    }
                    require(decision.completionReason.isBlank()) {
                        "Counterpart complete=false must leave completionReason blank"
                    }
                }
                decision
            }
            .getOrElse {
                error ->
                strictError =
                    error::class.simpleName.orEmpty() +
                        ": " +
                        error.message.orEmpty().take(1_000)
                null
            }

    val visibleMessage =
        strictDecision?.visibleMessage
            ?: runCatching {
                    DynamicCounterpartJson
                        .parseToJsonElement(rawOutput)
                        .jsonObject["visibleMessage"]
                        ?.jsonPrimitive
                        ?.contentOrNull
                }
                .getOrNull()

    return MultiVexSteeringObservedOutput(
        rawOutput = rawOutput,
        visibleMessage = visibleMessage,
        strictContractConformed = strictDecision != null,
        strictContractError = strictError,
        strictDecision = strictDecision,
    )
}

object MultiVexCounterpartSteeringAblationAdapter {
    private const val ResultSchema =
        "vexlife.multivex-counterpart-steering-ablation-result/v1"

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
            "Multi-Vex steering ablation requires one dedicated experiment window"
        }
        require(config.sourceClean) {
            "Multi-Vex steering ablation requires exact clean source"
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

        val session =
            VexLifeProdLocalSetup.establishSession(
                productHome = runtimeHome,
                allowAcquisition = false,
                allowRuntimeStart = config.runtimeStartAtBind,
            )
        val runtimeEvidence = session.observeCurrentQualifiedRuntimeEvidence()
        require(runtimeEvidence.modelRef == HereticModelCatalog.Models.Qwen4B.modelRef) {
            "Multi-Vex steering ablation requires exact selected Heretic 4B"
        }
        val receipt = requireNotNull(FileHereticRuntimeReceiptStore(runtimeHome).read()) {
            "Multi-Vex steering ablation requires Product runtime receipt"
        }
        require(receipt.receiptRef == runtimeEvidence.runtimeReceiptRef)
        require(receipt.modelRef == runtimeEvidence.modelRef)

        val runtime =
            DynamicCounterpartSarahRuntime.start(
                receipt = receipt,
                resultDir = resultDir,
            )

        return try {
            require(runtime.pid != receipt.runtime.pid) {
                "steering-ablation participant must use an isolated process"
            }

            val (history, admittedEventRefs) =
                MultiVexCounterpartSteeringDesign.firstEventHistory(profile)
            val allowedPrivateFactRefs =
                profile.scenario.ground.privateFacts.map { it.factRef }.toSet()
            val samples = mutableListOf<kotlinx.serialization.json.JsonObject>()
            var ordinal = 0

            MultiVexCounterpartSteeringDesign.CounterbalancedSchedule.forEachIndexed {
                roundIndex,
                round,
                ->
                round.forEachIndexed {
                    positionIndex,
                    condition,
                    ->
                    ordinal += 1
                    val systemParts =
                        MultiVexCounterpartSteeringDesign.systemParts(
                            profile = profile,
                            condition = condition,
                        )
                    val systemMessage =
                        systemParts.joinToString("\n\n")
                    val probe =
                        runtime.probeParticipant(
                            systemParts = systemParts,
                            history = history,
                        )
                    require(probe.statusCode == 200) {
                        "steering sample $ordinal returned HTTP ${probe.statusCode}: " +
                            probe.responseBody.take(4_000)
                    }
                    val rawOutput =
                        requireNotNull(probe.content) {
                            "steering sample $ordinal returned no assistant content"
                        }
                    val observed =
                        observeMultiVexSteeringOutput(
                            rawOutput = rawOutput,
                            allowedPrivateFactRefs = allowedPrivateFactRefs,
                        )
                    val visibleMessage = observed.visibleMessage
                    val cue =
                        visibleMessage
                            ?.takeIf { it.isNotBlank() }
                            ?.let(DirectConversationContextProjection::cueOrNull)
                    val queryTerms =
                        visibleMessage
                            ?.takeIf { it.isNotBlank() }
                            ?.let(DirectConversationContextProjection::queryTerms)
                            .orEmpty()
                    val speakerScope =
                        visibleMessage
                            ?.takeIf { it.isNotBlank() }
                            ?.let(DirectConversationContextProjection::speakerScope)

                    samples +=
                        buildJsonObject {
                            put("ordinal", ordinal)
                            put("round", roundIndex + 1)
                            put("positionInRound", positionIndex + 1)
                            put("conditionRef", condition.conditionRef)
                            put("postureSupplied", condition.postureSupplied)
                            put("roleSupplied", condition.roleSupplied)
                            put(
                                "promptComponents",
                                DynamicCounterpartJson.encodeToJsonElement(
                                    buildList {
                                        add("SCENARIO_GROUND")
                                        if (condition.postureSupplied) add("CHARACTER_POSTURE")
                                        if (condition.roleSupplied) add("INTERACTION_ROLE")
                                        add("OUTPUT_CONTRACT")
                                    },
                                ),
                            )
                            put("systemMessageSha256", dynamicCounterpartSha256(systemMessage))
                            put("rawParticipantOutput", observed.rawOutput)
                            put(
                                "rawParticipantOutputSha256",
                                dynamicCounterpartSha256(observed.rawOutput),
                            )
                            put("strictContractConformed", observed.strictContractConformed)
                            put(
                                "strictContractError",
                                observed.strictContractError ?: "",
                            )
                            put("visibleMessagePresent", visibleMessage != null)
                            put("visibleMessage", visibleMessage ?: "")
                            put(
                                "visibleMessageSha256",
                                visibleMessage?.let(::dynamicCounterpartSha256) ?: "",
                            )
                            put("requestBoundProjectionCue", cue?.name ?: "NONE")
                            put(
                                "requestBoundProjectionQueryTerms",
                                DynamicCounterpartJson.encodeToJsonElement(queryTerms),
                            )
                            put(
                                "requestBoundProjectionSpeakerScope",
                                speakerScope ?: "NONE",
                            )
                            val strictDecision = observed.strictDecision
                            put("strictPrivateStateAvailable", strictDecision != null)
                            if (strictDecision != null) {
                                put(
                                    "privateState",
                                    DynamicCounterpartJson.encodeToJsonElement(
                                        strictDecision.privateState,
                                    ),
                                )
                                put("privateObserverNote", strictDecision.privateObserverNote)
                                put("complete", strictDecision.complete)
                                put("completionReason", strictDecision.completionReason)
                            }
                            put("promptTokens", probe.promptTokens ?: -1)
                            put("cachedPromptTokens", probe.cachedPromptTokens ?: -1)
                            put("completionTokens", probe.completionTokens ?: -1)
                        }
                }
            }

            val result =
                buildJsonObject {
                    put("schemaVersion", ResultSchema)
                    put("status", "PASS")
                    put("proofClass", "MULTI_VEX_COUNTERPART_STEERING_ABLATION")
                    put("sourceSha", config.sourceSha)
                    put("bindingRef", profile.bindingRef)
                    put("characterRef", profile.character.characterRef)
                    put("roleRef", profile.role.roleRef)
                    put("scenarioRef", profile.scenario.scenarioRef)
                    put("participantRef", profile.participantRef)
                    put("participantName", profile.character.displayName)
                    put("counterpartPid", runtime.pid)
                    put("vexRuntimePid", receipt.runtime.pid)
                    put("processIsolationExact", runtime.pid != receipt.runtime.pid)
                    put("vexModelRef", runtimeEvidence.modelRef)
                    put("sampleCount", samples.size)
                    put(
                        "strictContractConformingSampleCount",
                        samples.count {
                            it["strictContractConformed"]?.jsonPrimitive?.content == "true"
                        },
                    )
                    put(
                        "visibleMessageSampleCount",
                        samples.count {
                            it["visibleMessagePresent"]?.jsonPrimitive?.content == "true"
                        },
                    )
                    put("replicatesPerCondition", 4)
                    put(
                        "constantGround",
                        buildJsonObject {
                            put("identitySupplied", true)
                            put("scenarioFactsSupplied", true)
                            put("scenarioIntentionsSupplied", true)
                            put("scenarioUncertaintiesSupplied", true)
                            put("ordinaryHumanNeedSupplied", true)
                            put("firstScenarioEventSupplied", true)
                            put("outputContractSupplied", true)
                            put("observerLensSupplied", false)
                            put("requiredVexAnswerSupplied", false)
                        },
                    )
                    put(
                        "conditions",
                        buildJsonArray {
                            MultiVexCounterpartSteeringDesign.Conditions.forEach {
                                condition ->
                                add(
                                    buildJsonObject {
                                        put("conditionRef", condition.conditionRef)
                                        put("postureSupplied", condition.postureSupplied)
                                        put("roleSupplied", condition.roleSupplied)
                                    },
                                )
                            }
                        },
                    )
                    put("samples", buildJsonArray { samples.forEach(::add) })
                    put(
                        "semanticJudgment",
                        "HUMAN_GODWARD_REVIEW_REQUIRED: mechanical PASS does not decide whether posture or role is steering",
                    )
                    put(
                        "reviewQuestions",
                        DynamicCounterpartJson.encodeToJsonElement(
                            listOf(
                                "Does the scenario ground alone already produce a natural first move?",
                                "Does adding CHARACTER POSTURE materially change the strategy rather than merely preserve freedom?",
                                "Does adding ROLE materially change the strategy rather than merely clarify the ordinary need?",
                                "Does posture or role increase request-bound source-inspection cue language?",
                                "Does the FULL condition behave materially differently from GROUND_ONLY across repeated samples?",
                                "Does strict output-contract conformance itself change when posture or role is present, indicating harness-compliance pressure?",
                                "If removing a layer loses no required truth, privacy, continuity, or proof, should that layer remain?",
                            ),
                        ),
                    )
                    put("observerSemanticAuthorityGranted", false)
                    put("foundationAcceptance", "NOT_APPLICABLE")
                    put("memoryDisposition", "NOT_APPLICABLE_METHOD_TEST")
                    put("dreamDisposition", "NOT_APPLICABLE_METHOD_TEST")
                    put("trainingDisposition", "NOT_APPLICABLE_METHOD_TEST")
                }

            Files.writeString(
                resultDir.resolve("multivex-counterpart-steering-ablation-result.json"),
                result.toString() + "\n",
            )
            0
        } finally {
            runtime.close()
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
        MultiVexCounterpartSteeringAblationAdapter.run(
            MultiVexCounterpartSteeringAblationAdapter.parse(args),
        ),
    )
}

// [VXG RealForever][MULTI-VEX][COUNTERPART-STEERING-ABLATION]
