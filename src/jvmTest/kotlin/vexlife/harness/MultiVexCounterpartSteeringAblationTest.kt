package vexlife.harness

import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MultiVexCounterpartSteeringAblationTest {
    private fun profile(): DynamicCounterpartEffectiveProfile =
        DynamicCounterpartProfiles.resolve(
            character =
                DynamicCounterpartProfiles.character(
                    Path.of("tools/continuity-pressure/characters/sarah-v1.json"),
                ),
            role =
                DynamicCounterpartProfiles.role(
                    Path.of("tools/continuity-pressure/roles/ordinary-shared-understanding-v1.json"),
                ),
            scenario =
                DynamicCounterpartProfiles.scenario(
                    Path.of("tools/continuity-pressure/scenario-profiles/changed-plan-v1.json"),
                ),
            binding =
                DynamicCounterpartProfiles.binding(
                    Path.of("tools/continuity-pressure/bindings/sarah-changed-plan-v1.json"),
                ),
        )

    @Test
    fun scheduleIsBalancedAcrossFourConditionsAndFourPositions() {
        val schedule = MultiVexCounterpartSteeringDesign.CounterbalancedSchedule

        assertEquals(4, schedule.size)
        assertTrue(schedule.all { it.size == 4 })

        val all = schedule.flatten()
        MultiVexCounterpartSteeringDesign.Conditions.forEach { condition ->
            assertEquals(4, all.count { it == condition })
            val positions =
                schedule.mapIndexedNotNull { _, round ->
                    round.indexOf(condition).takeIf { it >= 0 }
                }
            assertEquals(setOf(0, 1, 2, 3), positions.toSet())
        }
    }

    @Test
    fun groundIsConstantWhilePostureAndRoleAreIndependentFactors() {
        val profile = profile()
        val groundOnly =
            MultiVexCounterpartSteeringDesign.systemParts(
                profile,
                MultiVexCounterpartSteeringDesign.GroundOnly,
            )
        val postureOnly =
            MultiVexCounterpartSteeringDesign.systemParts(
                profile,
                MultiVexCounterpartSteeringDesign.PostureOnly,
            )
        val roleOnly =
            MultiVexCounterpartSteeringDesign.systemParts(
                profile,
                MultiVexCounterpartSteeringDesign.RoleOnly,
            )
        val full =
            MultiVexCounterpartSteeringDesign.systemParts(
                profile,
                MultiVexCounterpartSteeringDesign.Full,
            )

        val ground = DynamicCounterpartProfiles.scenarioGroundPrompt(profile)
        val posture = DynamicCounterpartProfiles.characterPosturePrompt(profile)
        val role = DynamicCounterpartProfiles.interactionRolePrompt(profile)
        val output = DynamicCounterpartProfiles.participantDecisionOutputPrompt()

        assertEquals(listOf(ground, output), groundOnly)
        assertEquals(listOf(ground, posture, output), postureOnly)
        assertEquals(listOf(ground, role, output), roleOnly)
        assertEquals(listOf(ground, posture, role, output), full)

        assertFalse("Relational posture:" in ground)
        assertFalse("Private objective:" in ground)
        assertTrue("Intentions present at scenario start:" in ground)
        assertTrue("Your ordinary human need:" in ground)
    }

    @Test
    fun fullAblationConditionMaterializesExactExplicitPostureRoleTreatmentBytes() {
        val profile = profile()

        assertEquals(
            MultiVexCounterpartSteeringDesign.fullTreatmentSystemMessage(profile),
            MultiVexCounterpartSteeringDesign.materializedSystemMessage(
                profile,
                MultiVexCounterpartSteeringDesign.Full,
            ),
        )
    }

    @Test
    fun methodObservationRecoversVisibleMoveWithoutWeakeningStrictEncounterState() {
        val malformedButObservable =
            """
            {
              "schemaVersion":"vexlife.dynamic-counterpart-participant-output/v1",
              "visibleMessage":"What do you have for Saturday right now?",
              "privateState":{
                "currentIntentions":["check shared plan"],
                "uncertainties":["what Vex currently has"],
                "relationshipNotes":"Vex may be relying on an older view",
                "revealedPrivateFactRefs":[]
              },
              "privateObserverNote":"",
              "complete":false,
              "completionReason":""
            }
            """
                .trimIndent()

        val observed =
            observeMultiVexSteeringOutput(
                rawOutput = malformedButObservable,
                allowedPrivateFactRefs =
                    setOf("private.changed-plan.saturday-surprise.v1"),
            )

        assertFalse(observed.strictContractConformed)
        assertTrue(observed.strictContractError.orEmpty().contains("relationshipNotes"))
        assertEquals(
            "What do you have for Saturday right now?",
            observed.visibleMessage,
        )

        kotlin.test.assertFails {
            parseDynamicCounterpartParticipantDecision(malformedButObservable)
        }
    }

    @Test
    fun neutralParticipantBaselineIsGroundPlusTruthPrivacyBoundariesWithoutPostureOrRole() {
        val profile = profile()
        val neutral = DynamicCounterpartProfiles.participantGroundPrompt(profile)
        val expected =
            listOf(
                DynamicCounterpartProfiles.scenarioGroundPrompt(profile),
                DynamicCounterpartProfiles.characterBoundaryPrompt(profile),
            ).joinToString("\n\n")

        assertEquals(expected, neutral)
        assertTrue("DCP_PARTICIPANT_BOUNDARIES/v1" in neutral)
        assertFalse("DCP_CHARACTER_POSTURE/v1" in neutral)
        assertFalse("DCP_INTERACTION_ROLE/v1" in neutral)
        assertTrue("Intentions present at scenario start:" in neutral)
        assertTrue("Your ordinary human need:" in neutral)
        assertTrue("Do not fabricate scenario events" in neutral)
        assertTrue("Do not steer Vex toward a target phrase" in neutral)
    }

    @Test
    fun firstPrivateEventIsConstantAcrossEveryCondition() {
        val profile = profile()
        val (history, admittedRefs) =
            MultiVexCounterpartSteeringDesign.firstEventHistory(profile)

        assertEquals(
            listOf("event.changed-plan.need-check-shared-plan.v1"),
            admittedRefs,
        )
        assertEquals(1, history.size)
        assertTrue("PRIVATE_SCENARIO_EVENT/v1" in history.single().second)
        assertTrue(
            "you want to check what you and Vex currently understand the plan to be" in
                history.single().second,
        )
    }
}

// [VXG RealForever][MULTI-VEX][COUNTERPART-STEERING-ABLATION]
