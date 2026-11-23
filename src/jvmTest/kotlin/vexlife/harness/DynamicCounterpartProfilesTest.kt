package vexlife.harness

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class DynamicCounterpartProfilesTest {
    private val characterPath =
        Path.of("tools/continuity-pressure/characters/sarah-v1.json")
    private val miraCharacterPath =
        Path.of("tools/continuity-pressure/characters/mira-v1.json")
    private val scenarioPath =
        Path.of("tools/continuity-pressure/scenario-profiles/changed-plan-v1.json")
    private val bindingPath =
        Path.of("tools/continuity-pressure/bindings/sarah-changed-plan-v1.json")
    private val miraBindingPath =
        Path.of("tools/continuity-pressure/bindings/mira-changed-plan-v1.json")
    private val ordinaryRolePath =
        Path.of("tools/continuity-pressure/roles/ordinary-shared-understanding-v1.json")
    private val observerLensPath =
        Path.of(
            "tools/continuity-pressure/observer-lenses/relationship-pressure-patterns-v1.json",
        )
    private val legacyScenarioPath =
        Path.of("tools/continuity-pressure/scenarios/evolve-context-dynamic-counterpart-dcp1-v1.json")

    private val rolePaths =
        listOf(
            ordinaryRolePath,
            Path.of("tools/continuity-pressure/roles/affinity-material-access-v1.json"),
            Path.of("tools/continuity-pressure/roles/borrowed-standing-request-v1.json"),
            Path.of("tools/continuity-pressure/roles/unusual-coherent-request-v1.json"),
            Path.of("tools/continuity-pressure/roles/portable-artifact-reuse-v1.json"),
        )

    @Test
    fun bindingComposesWhoFunctionAndSituationWithoutDuplicatingParticipantIdentity() {
        val character = DynamicCounterpartProfiles.character(characterPath)
        val role = DynamicCounterpartProfiles.role(ordinaryRolePath)
        val scenario = DynamicCounterpartProfiles.scenario(scenarioPath)
        val binding = DynamicCounterpartProfiles.binding(bindingPath)

        val effective =
            DynamicCounterpartProfiles.resolve(
                character = character,
                role = role,
                scenario = scenario,
                binding = binding,
            )

        assertEquals(binding.bindingRef, effective.bindingRef)
        assertEquals(character.participantRef, effective.participantRef)
        assertSame(character, effective.character)
        assertSame(role, effective.role)
        assertSame(scenario, effective.scenario)

        val bindingSource = Files.readString(bindingPath)
        assertFalse("\"participantRef\"" in bindingSource)
        assertFalse("\"displayName\"" in bindingSource)
    }

    @Test
    fun characterCanSwapWithoutChangingRoleOrScenario() {
        val sarah = DynamicCounterpartProfiles.character(characterPath)
        val mira = DynamicCounterpartProfiles.character(miraCharacterPath)
        val role = DynamicCounterpartProfiles.role(ordinaryRolePath)
        val scenario = DynamicCounterpartProfiles.scenario(scenarioPath)
        val sarahBinding = DynamicCounterpartProfiles.binding(bindingPath)
        val miraBinding = DynamicCounterpartProfiles.binding(miraBindingPath)

        val sarahEffective =
            DynamicCounterpartProfiles.resolve(
                sarah,
                role,
                scenario,
                sarahBinding,
            )
        val miraEffective =
            DynamicCounterpartProfiles.resolve(
                mira,
                role,
                scenario,
                miraBinding,
            )

        assertEquals("character.vexlife.dynamic-counterpart.person-c.v1", mira.characterRef)
        assertEquals("person.synthetic.c39a72e1", mira.participantRef)
        assertEquals("Mira", mira.displayName)
        assertEquals(
            listOf("source.character.dynamic-counterpart.person-c.v1"),
            mira.identitySourceRefs,
        )
        assertEquals(sarah.identityStanding, mira.identityStanding)
        assertEquals(sarah.relationalPosture, mira.relationalPosture)
        assertEquals(sarah.freedoms, mira.freedoms)
        assertEquals(sarah.boundaries, mira.boundaries)

        assertEquals(sarahEffective.role, miraEffective.role)
        assertEquals(sarahEffective.scenario, miraEffective.scenario)
        assertEquals(sarahBinding.roleRef, miraBinding.roleRef)
        assertEquals(sarahBinding.scenarioRef, miraBinding.scenarioRef)
        assertTrue(
            DynamicCounterpartProfiles.participantGroundPrompt(miraEffective)
                .contains("You are Mira."),
        )
        assertFalse(
            DynamicCounterpartProfiles.participantGroundPrompt(miraEffective)
                .contains("You are Sarah."),
        )
    }

    @Test
    fun reusableRolesDoNotCarryCharacterNamesOrChangedPlanFacts() {
        rolePaths.forEach { path ->
            val role = DynamicCounterpartProfiles.role(path)
            val source = Files.readString(path)

            assertTrue(role.privateObjective.isNotBlank())
            assertFalse("Sarah" in source)
            assertFalse("Mika" in source)
            assertFalse("Zookayy" in source)
            assertFalse("Harbor Cafe" in source)
            assertFalse("Cedar Station" in source)
            assertFalse("\"dialogue\"" in source)
            assertFalse("\"requiredVexAnswer\"" in source)
            assertFalse("\"passFail\"" in source)
        }
    }

    @Test
    fun changedPlanScenarioOwnsSituationButNoParticipantIdentityOrObserverRubric() {
        val scenario = DynamicCounterpartProfiles.scenario(scenarioPath)
        val source = Files.readString(scenarioPath)

        assertFalse("Sarah" in source)
        assertFalse("\"characterRef\"" in source)
        assertFalse("\"roleRef\"" in source)
        assertFalse("flattery" in source.lowercase())
        assertFalse("performed innocence" in source.lowercase())

        assertTrue(
            scenario.ground.knownFacts.any {
                "Cedar Station at 10:00 a.m." in it
            },
        )
        assertTrue(
            scenario.ground.knownFacts.any {
                "Harbor Cafe at 9:00 a.m. is historical and superseded." in it
            },
        )
    }

    @Test
    fun changedPlanProfilePreservesLegacySituationWhileMovingIdentityAndStewardOut() {
        val legacy =
            DynamicCounterpartJson.decodeFromString<DynamicCounterpartScenario>(
                Files.readString(legacyScenarioPath),
            )
        val current = DynamicCounterpartProfiles.scenario(scenarioPath)

        assertEquals(legacy.setup, current.setup)
        assertEquals(legacy.roleGround.relationshipHistory, current.ground.relationshipHistory)
        assertEquals(
            legacy.roleGround.knownFacts.filterNot { it == "Your name is Sarah." },
            current.ground.knownFacts,
        )
        assertEquals(legacy.roleGround.currentIntentions, current.ground.currentIntentions)
        assertEquals(legacy.roleGround.uncertainties, current.ground.uncertainties)
        assertEquals(
            legacy.roleGround.privateFacts.map(DynamicCounterpartPrivateFact::content),
            current.ground.privateFacts.map(DynamicCounterpartPrivateFact::content),
        )
        assertEquals(legacy.roleGround.ordinaryHumanNeed, current.ground.ordinaryHumanNeed)
        assertEquals(
            legacy.events.map(DynamicCounterpartEvent::description),
            current.events.map(DynamicCounterpartEvent::description),
        )
        assertEquals(legacy.maxVisibleCounterpartTurns, current.maxVisibleCounterpartTurns)
    }

    @Test
    fun observerLensStaysOutsideParticipantAndRolePrompts() {
        val character = DynamicCounterpartProfiles.character(characterPath)
        val role = DynamicCounterpartProfiles.role(ordinaryRolePath)
        val scenario = DynamicCounterpartProfiles.scenario(scenarioPath)
        val binding = DynamicCounterpartProfiles.binding(bindingPath)
        val lens = DynamicCounterpartProfiles.observerLens(observerLensPath)
        val effective = DynamicCounterpartProfiles.resolve(character, role, scenario, binding)

        val participantPrompt = DynamicCounterpartProfiles.participantGroundPrompt(effective)
        val rolePrompt = DynamicCounterpartProfiles.interactionRolePrompt(effective)
        val modelFacing = participantPrompt + "\n" + rolePrompt

        lens.patterns.forEach { pattern ->
            assertFalse(pattern.label in modelFacing)
            assertFalse(pattern.description in modelFacing)
        }
        assertFalse(lens.label in modelFacing)
        assertFalse("PASS / FAIL" in modelFacing)
    }

    @Test
    fun participantMayMarkOneFinalVisibleMessageCompleteWithoutRequiringAnotherInference() {
        val decision =
            parseDynamicCounterpartParticipantDecision(
                """
                {
                  "schemaVersion":"vexlife.dynamic-counterpart-participant-output/v1",
                  "visibleMessage":"Thanks, we're aligned. See you Saturday.",
                  "privateState":{
                    "currentIntentions":[],
                    "uncertainties":[],
                    "relationshipNotes":["The ordinary need is satisfied."],
                    "revealedPrivateFactRefs":[]
                  },
                  "privateObserverNote":"",
                  "complete":true,
                  "completionReason":"Shared understanding is confirmed."
                }
                """
                    .trimIndent(),
            )

        assertTrue(decision.complete)
        assertTrue(decision.visibleMessage.isNotBlank())
        assertTrue(decision.completionReason.isNotBlank())
    }

    @Test
    fun encounterMayEndByParticipantCompletionOrByVisibleTurnBudgetWithoutExtraInference() {
        assertEquals(
            "PARTICIPANT_COMPLETE",
            dynamicCounterpartEncounterTermination(
                participantReportedComplete = true,
                visibleParticipantTurns = 1,
                maxVisibleCounterpartTurns = 3,
            ),
        )
        assertEquals(
            "VISIBLE_TURN_BUDGET_EXHAUSTED",
            dynamicCounterpartEncounterTermination(
                participantReportedComplete = false,
                visibleParticipantTurns = 3,
                maxVisibleCounterpartTurns = 3,
            ),
        )
        assertEquals(
            "NOT_TERMINAL",
            dynamicCounterpartEncounterTermination(
                participantReportedComplete = false,
                visibleParticipantTurns = 2,
                maxVisibleCounterpartTurns = 3,
            ),
        )
    }

    @Test
    fun counterpartTransportUsesExactlyOneOrderedSystemMessage() {
        val messages =
            dynamicCounterpartMessages(
                systemParts =
                    listOf(
                        "PARTICIPANT_GROUND",
                        "INTERACTION_ROLE",
                        "OUTPUT_CONTRACT",
                    ),
                history =
                    listOf(
                        "user" to "PRIVATE_SCENARIO_EVENT/v1",
                        "assistant" to "previous participant decision",
                        "user" to "VISIBLE_VEX_REPLY/v1",
                    ),
            )

        val systemMessages =
            messages.filter {
                it.jsonObject.getValue("role").jsonPrimitive.content == "system"
            }

        assertEquals(1, systemMessages.size)
        assertEquals(
            "PARTICIPANT_GROUND\n\nINTERACTION_ROLE\n\nOUTPUT_CONTRACT",
            systemMessages.single().jsonObject.getValue("content").jsonPrimitive.content,
        )
        assertEquals(
            listOf("system", "user", "assistant", "user"),
            messages.map {
                it.jsonObject.getValue("role").jsonPrimitive.content
            },
        )
    }

    @Test
    fun genericParticipantOutputContractDoesNotCarrySarahIdentityAndParsesExplicitDisclosureState() {
        val prompt = DynamicCounterpartProfiles.participantDecisionOutputPrompt()

        assertTrue(DynamicCounterpartParticipantOutputSchema in prompt)
        assertFalse("Sarah" in prompt)

        val decision =
            parseDynamicCounterpartParticipantDecision(
                """
                {
                  "schemaVersion":"vexlife.dynamic-counterpart-participant-output/v1",
                  "visibleMessage":"What do you have for Saturday now?",
                  "privateState":{
                    "currentIntentions":["Check shared understanding."],
                    "uncertainties":["I do not know what Vex currently has in view."],
                    "relationshipNotes":[],
                    "revealedPrivateFactRefs":[]
                  },
                  "complete":false,
                  "completionReason":""
                }
                """
                    .trimIndent(),
            )

        assertEquals("", decision.privateObserverNote)
        assertTrue(decision.privateState.revealedPrivateFactRefs.isEmpty())
        validateDynamicCounterpartParticipantState(
            decision.privateState,
            allowedPrivateFactRefs = setOf("private.changed-plan.saturday-surprise.v1"),
        )
    }

    @Test
    fun rolePromptCarriesObjectiveWithoutHumanFacingClassificationLabel() {
        val character = DynamicCounterpartProfiles.character(characterPath)
        val role =
            DynamicCounterpartProfiles.role(
                Path.of("tools/continuity-pressure/roles/affinity-material-access-v1.json"),
            )
        val scenario = DynamicCounterpartProfiles.scenario(scenarioPath)
        val binding =
            DynamicCounterpartProfiles.binding(bindingPath)
                .copy(
                    bindingRef = "binding.dynamic-counterpart.sarah.changed-plan.affinity.v1",
                    roleRef = role.roleRef,
                )
        val effective = DynamicCounterpartProfiles.resolve(character, role, scenario, binding)
        val prompt = DynamicCounterpartProfiles.interactionRolePrompt(effective)

        assertTrue(role.privateObjective in prompt)
        assertFalse(role.label in prompt)
        assertFalse(role.roleRef in prompt)
    }
}

// [VXG RealForever]
