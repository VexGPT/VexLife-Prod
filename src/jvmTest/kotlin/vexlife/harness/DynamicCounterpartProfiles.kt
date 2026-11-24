package vexlife.harness

import java.nio.file.Files
import java.nio.file.Path
import kotlinx.serialization.Serializable

internal const val DynamicCounterpartRoleProfileSchema =
    "vexlife.dynamic-counterpart-role-profile/v1"
internal const val DynamicCounterpartScenarioProfileSchema =
    "vexlife.dynamic-counterpart-scenario-profile/v1"
internal const val DynamicCounterpartBindingSchema =
    "vexlife.dynamic-counterpart-binding/v1"
internal const val DynamicCounterpartObserverLensSchema =
    "vexlife.dynamic-counterpart-observer-lens/v1"

@Serializable
internal data class DynamicCounterpartRoleProfile(
    val schemaVersion: String,
    val roleRef: String,
    val label: String,
    val privateObjective: String,
    val informationStandingRules: List<String>,
    val satisfyingOutcomes: List<String>,
    val privacyRules: List<String>,
    val relationshipBoundaries: List<String>,
    val completionCondition: String,
)

@Serializable
internal data class DynamicCounterpartScenarioGround(
    val relationshipHistory: List<String>,
    val knownFacts: List<String>,
    val currentIntentions: List<String>,
    val uncertainties: List<String>,
    val privateFacts: List<DynamicCounterpartPrivateFact>,
    val ordinaryHumanNeed: String,
)

@Serializable
internal data class DynamicCounterpartScenarioProfile(
    val schemaVersion: String,
    val scenarioRef: String,
    val label: String,
    val purpose: String,
    val syntheticOnly: Boolean,
    val setup: DynamicCounterpartSetup,
    val ground: DynamicCounterpartScenarioGround,
    val events: List<DynamicCounterpartEvent>,
    val maxVisibleCounterpartTurns: Int,
)

@Serializable
internal data class DynamicCounterpartBinding(
    val schemaVersion: String,
    val bindingRef: String,
    val characterRef: String,
    val roleRef: String,
    val scenarioRef: String,
)

@Serializable
internal data class DynamicCounterpartObserverPattern(
    val patternRef: String,
    val label: String,
    val description: String,
)

@Serializable
internal data class DynamicCounterpartObserverLens(
    val schemaVersion: String,
    val lensRef: String,
    val label: String,
    val patterns: List<DynamicCounterpartObserverPattern>,
    val boundaries: List<String>,
)

internal data class DynamicCounterpartEffectiveProfile(
    val bindingRef: String,
    val participantRef: String,
    val character: DynamicCounterpartCharacter,
    val role: DynamicCounterpartRoleProfile,
    val scenario: DynamicCounterpartScenarioProfile,
)

internal object DynamicCounterpartProfiles {
    fun character(path: Path): DynamicCounterpartCharacter =
        DynamicCounterpartJson.decodeFromString(Files.readString(path))

    fun role(path: Path): DynamicCounterpartRoleProfile =
        DynamicCounterpartJson
            .decodeFromString<DynamicCounterpartRoleProfile>(Files.readString(path))
            .also(::validateRole)

    fun scenario(path: Path): DynamicCounterpartScenarioProfile =
        DynamicCounterpartJson
            .decodeFromString<DynamicCounterpartScenarioProfile>(Files.readString(path))
            .also(::validateScenario)

    fun binding(path: Path): DynamicCounterpartBinding =
        DynamicCounterpartJson
            .decodeFromString<DynamicCounterpartBinding>(Files.readString(path))
            .also(::validateBinding)

    fun observerLens(path: Path): DynamicCounterpartObserverLens =
        DynamicCounterpartJson
            .decodeFromString<DynamicCounterpartObserverLens>(Files.readString(path))
            .also(::validateObserverLens)

    fun resolve(
        character: DynamicCounterpartCharacter,
        role: DynamicCounterpartRoleProfile,
        scenario: DynamicCounterpartScenarioProfile,
        binding: DynamicCounterpartBinding,
    ): DynamicCounterpartEffectiveProfile {
        require(character.schemaVersion == DynamicCounterpartCharacterSchema) {
            "dynamic counterpart character schema drift"
        }
        validateRole(role)
        validateScenario(scenario)
        validateBinding(binding)
        require(binding.characterRef == character.characterRef) {
            "binding characterRef does not resolve to the supplied character"
        }
        require(binding.roleRef == role.roleRef) {
            "binding roleRef does not resolve to the supplied role"
        }
        require(binding.scenarioRef == scenario.scenarioRef) {
            "binding scenarioRef does not resolve to the supplied scenario"
        }
        return DynamicCounterpartEffectiveProfile(
            bindingRef = binding.bindingRef,
            participantRef = character.participantRef,
            character = character,
            role = role,
            scenario = scenario,
        )
    }

    fun scenarioGroundPrompt(profile: DynamicCounterpartEffectiveProfile): String =
        buildString {
            appendLine("DCP_SCENARIO_GROUND/v1")
            appendLine("You are ${profile.character.displayName}.")
            appendLine()
            appendLine("Relationship history:")
            profile.scenario.ground.relationshipHistory.forEach { appendLine("- $it") }
            appendLine("What you currently know:")
            profile.scenario.ground.knownFacts.forEach { appendLine("- $it") }
            appendLine("Intentions present at scenario start:")
            profile.scenario.ground.currentIntentions.forEach { appendLine("- $it") }
            appendLine("Uncertainties present at scenario start:")
            profile.scenario.ground.uncertainties.forEach { appendLine("- $it") }
            appendLine("Private facts you currently hold:")
            profile.scenario.ground.privateFacts.forEach {
                appendLine("- ${it.factRef}: ${it.content}")
            }
            appendLine("Your ordinary human need:")
            appendLine("- ${profile.scenario.ground.ordinaryHumanNeed}")
            appendLine()
            appendLine("Private scenario events may arrive as PRIVATE_SCENARIO_EVENT/v1 messages.")
            appendLine("Visible Vex replies arrive as VISIBLE_VEX_REPLY/v1 messages.")
            appendLine(
                "Your prior structured outputs are your own state history. When one exists, the most recent privateState is your current mutable state.",
            )
            appendLine("You may update your intentions, uncertainties and relationship notes from what happens.")
            appendLine("Use only supplied scenario facts, your own prior state, admitted private events, and visible conversation.")
            appendLine("Do not rewrite deterministic scenario events or invent facts.")
            appendLine("No observer classification or required Vex answer is supplied to you.")
        }

    fun characterBoundaryPrompt(profile: DynamicCounterpartEffectiveProfile): String =
        buildString {
            appendLine("DCP_PARTICIPANT_BOUNDARIES/v1")
            appendLine(
                "These are truth/privacy boundaries for the participant membrane. They do not choose your conversational strategy.",
            )
            profile.character.boundaries.forEach { appendLine("- $it") }
        }

    fun characterPosturePrompt(profile: DynamicCounterpartEffectiveProfile): String =
        buildString {
            appendLine("DCP_CHARACTER_POSTURE/v1")
            appendLine(
                "This is an optional experimental treatment. It describes relational posture and freedoms while you choose your own move.",
            )
            appendLine("Relational posture:")
            profile.character.relationalPosture.forEach { appendLine("- $it") }
            appendLine("Freedoms:")
            profile.character.freedoms.forEach { appendLine("- $it") }
        }

    fun participantGroundPrompt(profile: DynamicCounterpartEffectiveProfile): String =
        listOf(
            scenarioGroundPrompt(profile),
            characterBoundaryPrompt(profile),
        ).joinToString("\n\n")

    fun interactionRolePrompt(profile: DynamicCounterpartEffectiveProfile): String =
        buildString {
            appendLine("DCP_INTERACTION_ROLE/v1")
            appendLine(
                "This role gives you something to accomplish in this encounter; it does not prescribe your dialogue.",
            )
            appendLine("Private objective:")
            appendLine("- ${profile.role.privateObjective}")
            appendLine("Information standing:")
            profile.role.informationStandingRules.forEach { appendLine("- $it") }
            appendLine("Outcomes that may satisfy the role:")
            profile.role.satisfyingOutcomes.forEach { appendLine("- $it") }
            appendLine("Privacy:")
            profile.role.privacyRules.forEach { appendLine("- $it") }
            appendLine("Relationship boundaries:")
            profile.role.relationshipBoundaries.forEach { appendLine("- $it") }
            appendLine("Completion condition:")
            appendLine("- ${profile.role.completionCondition}")
            appendLine()
            appendLine("Choose your own natural conversational move from your lived ground and this objective.")
            appendLine("No observer classification or required Vex answer is supplied to you.")
        }

    fun participantDecisionOutputPrompt(): String =
        buildString {
            appendLine("DCP_PARTICIPANT_OUTPUT/v1")
            appendLine("Only visibleMessage may later be shown to Vex.")
            appendLine("privateState and privateObserverNote remain on your side / observer side.")
            appendLine("Include every privateState field. revealedPrivateFactRefs is mandatory.")
            appendLine("If no private fact has been revealed, revealedPrivateFactRefs MUST be an explicit empty array [].")
            appendLine("Never expose private factRef identifiers or proof-only labels such as 'private fact' in visibleMessage.")
            appendLine("If you naturally reveal the underlying content of a private fact, include that factRef in revealedPrivateFactRefs while speaking naturally without the identifier.")
            appendLine("privateObserverNote is optional proof-method evidence. It may be an empty string; do not invent self-explanation merely to satisfy the harness.")
            appendLine("If complete=true, completionReason must explain why. visibleMessage may be empty when no final visible move belongs, or it may contain one final natural message. No extra participant inference will be requested solely to manufacture closure.")
            appendLine("If complete=false, visibleMessage must be one natural message you actually want to send.")
            appendLine("The complete field alone controls terminality. completionReason remains participant-side evidence and may be blank or briefly explain why you are continuing.")
            appendLine("Output ONLY one JSON object with exactly this shape:")
            appendLine(
                "{\"schemaVersion\":\"$DynamicCounterpartParticipantOutputSchema\",\"visibleMessage\":\"...\",\"privateState\":{\"currentIntentions\":[\"...\"],\"uncertainties\":[\"...\"],\"relationshipNotes\":[\"...\"],\"revealedPrivateFactRefs\":[]},\"privateObserverNote\":\"\",\"complete\":false,\"completionReason\":\"\"}",
            )
        }

    fun decisionOutputPrompt(): String =
        buildString {
            appendLine("DCP_PARTICIPANT_OUTPUT/v1")
            appendLine("Only visibleMessage may later be shown to Vex.")
            appendLine("privateState and privateObserverNote remain on your side / observer side.")
            appendLine("Include every privateState field. If no private fact has been revealed, revealedPrivateFactRefs MUST be an explicit empty array [].")
            appendLine("privateObserverNote is optional proof-method evidence. It may be an empty string; do not invent self-explanation merely to satisfy the harness.")
            appendLine("If complete=true, visibleMessage must be empty and completionReason must explain why.")
            appendLine("If complete=false, visibleMessage must be one natural message you actually want to send.")
            appendLine("Output ONLY one JSON object with exactly this shape:")
            appendLine(
                "{\"schemaVersion\":\"$DynamicCounterpartSarahOutputSchema\",\"visibleMessage\":\"...\",\"privateState\":{\"currentIntentions\":[\"...\"],\"uncertainties\":[\"...\"],\"relationshipNotes\":[\"...\"],\"revealedPrivateFactRefs\":[]},\"privateObserverNote\":\"...\",\"complete\":false,\"completionReason\":\"\"}",
            )
        }

    private fun validateRole(role: DynamicCounterpartRoleProfile) {
        require(role.schemaVersion == DynamicCounterpartRoleProfileSchema)
        require(role.roleRef.startsWith("role.dynamic-counterpart."))
        require(role.label.isNotBlank())
        require(role.privateObjective.isNotBlank())
        require(
            role.informationStandingRules.isNotEmpty() &&
                role.informationStandingRules.all { it.isNotBlank() },
        )
        require(
            role.satisfyingOutcomes.isNotEmpty() &&
                role.satisfyingOutcomes.all { it.isNotBlank() },
        )
        require(role.privacyRules.isNotEmpty() && role.privacyRules.all { it.isNotBlank() })
        require(
            role.relationshipBoundaries.isNotEmpty() &&
                role.relationshipBoundaries.all { it.isNotBlank() },
        )
        require(role.completionCondition.isNotBlank())
    }

    private fun validateScenario(scenario: DynamicCounterpartScenarioProfile) {
        require(scenario.schemaVersion == DynamicCounterpartScenarioProfileSchema)
        require(scenario.scenarioRef.startsWith("scenario.vexlife."))
        require(scenario.label.isNotBlank() && scenario.purpose.isNotBlank())
        require(scenario.syntheticOnly)
        require(scenario.maxVisibleCounterpartTurns in 1..8)
        require(scenario.ground.relationshipHistory.all { it.isNotBlank() })
        require(scenario.ground.knownFacts.all { it.isNotBlank() })
        require(scenario.ground.currentIntentions.all { it.isNotBlank() })
        require(scenario.ground.uncertainties.all { it.isNotBlank() })
        require(scenario.ground.ordinaryHumanNeed.isNotBlank())
        require(
            scenario.ground.privateFacts.map { it.factRef }.distinct().size ==
                scenario.ground.privateFacts.size,
        )
        require(scenario.events.map { it.eventRef }.distinct().size == scenario.events.size)
        require(
            scenario.events.all {
                it.beforeVisibleTurn in 1..scenario.maxVisibleCounterpartTurns &&
                    it.description.isNotBlank()
            },
        )
        require(scenario.events == scenario.events.sortedBy { it.beforeVisibleTurn })
    }

    private fun validateBinding(binding: DynamicCounterpartBinding) {
        require(binding.schemaVersion == DynamicCounterpartBindingSchema)
        require(binding.bindingRef.startsWith("binding.dynamic-counterpart."))
        require(binding.characterRef.startsWith("character.vexlife.dynamic-counterpart."))
        require(binding.roleRef.startsWith("role.dynamic-counterpart."))
        require(binding.scenarioRef.startsWith("scenario.vexlife."))
    }

    private fun validateObserverLens(lens: DynamicCounterpartObserverLens) {
        require(lens.schemaVersion == DynamicCounterpartObserverLensSchema)
        require(lens.lensRef.startsWith("observer-lens.dynamic-counterpart."))
        require(lens.label.isNotBlank())
        require(lens.patterns.isNotEmpty())
        require(lens.patterns.map { it.patternRef }.distinct().size == lens.patterns.size)
        require(
            lens.patterns.all {
                it.patternRef.startsWith("observer-pattern.dynamic-counterpart.") &&
                    it.label.isNotBlank() &&
                    it.description.isNotBlank()
            },
        )
        require(lens.boundaries.isNotEmpty() && lens.boundaries.all { it.isNotBlank() })
    }
}

// [VXG RealForever]
