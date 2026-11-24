package vexlife.harness

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vexlife.companion.DirectPromptContext
import vexlife.conversation.DirectConversationStore
import vexlife.home.CanonicalVexLifeHomeFixture

class DynamicCounterpartContractTest {
    private val characterPath =
        Path.of("tools/continuity-pressure/characters/sarah-v1.json")

    private val scenarioPath =
        Path.of("tools/continuity-pressure/scenarios/evolve-context-dynamic-counterpart-dcp1-v1.json")

    private fun character(): DynamicCounterpartCharacter =
        DynamicCounterpartJson.decodeFromString(Files.readString(characterPath))

    private fun scenario(): DynamicCounterpartScenario =
        DynamicCounterpartJson.decodeFromString(Files.readString(scenarioPath))

    @Test
    fun sarahCharacterAndScenarioKeepStablePersonSeparateFromLivedSituationAndStewardPurpose() {
        val character = character()
        val scenario = scenario()

        assertEquals(DynamicCounterpartCharacterSchema, character.schemaVersion)
        assertEquals(DynamicCounterpartScenarioSchema, scenario.schemaVersion)
        assertEquals(character.characterRef, scenario.characterRef)
        assertEquals("Sarah", character.displayName)
        assertTrue(character.participantRef.startsWith("person."))
        assertFalse(character.participantRef.lowercase().contains("sarah"))
        assertEquals("PROFILE_DECLARED", character.identityStanding)
        assertTrue(character.identitySourceRefs.isNotEmpty())

        assertTrue(scenario.roleGround.relationshipHistory.isNotEmpty())
        assertTrue(scenario.roleGround.knownFacts.isNotEmpty())
        assertTrue(scenario.roleGround.currentIntentions.isNotEmpty())
        assertTrue(scenario.roleGround.uncertainties.isNotEmpty())
        assertTrue(scenario.roleGround.ordinaryHumanNeed.isNotBlank())

        assertTrue(scenario.steward.observeRelationships.isNotEmpty())
        assertTrue(scenario.steward.allowedPressure.isNotEmpty())
        assertTrue(scenario.steward.forbiddenPressure.isNotEmpty())
        assertTrue(scenario.steward.completionCondition.isNotBlank())

        assertFalse(character.relationalPosture.any { "Cedar Station" in it })
        assertFalse(character.relationalPosture.any { "Harbor Cafe" in it })
        assertTrue(scenario.roleGround.knownFacts.any { "Cedar Station" in it })
    }

    @Test
    fun scratchHomeMakesSarahOneCanonicalOpaqueParticipantAndPersonContextAddsOnlyGroundedNameStanding() {
        val character = character()
        val parent = Files.createTempDirectory("vexlife-dcp1-participant-")
        val home = parent.resolve(".vexlife")
        try {
            val formed =
                CanonicalVexLifeHomeFixture.form(
                    home = home,
                    deviceSuffix = "dcp1-contract-device",
                    lineageSuffix = "dcp1-contract-lineage",
                    personRef = character.participantRef,
                    familyRef = "vex-family.synthetic-dcp1",
                    deviceName = "Synthetic DCP-1 host",
                )
            assertEquals(character.participantRef, formed.installation.personRef)

            val request =
                DirectConversationStore(home).withWriterLease {
                    beginRequest(
                        turnRef = "turn.self-development.dcp1.participant",
                        requestMessageRef = "message.self-development.human.dcp1.participant",
                        content = "What do we understand about Saturday?",
                    )
                }
            assertEquals(character.participantRef, request.speakerRef)

            val person =
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
            val effective =
                DirectPromptContext.effectivePersonContext(
                    currentRequest = request,
                    context = person,
                )

            assertEquals(character.participantRef, effective.participantRef)
            assertEquals("Sarah", effective.name)
            assertEquals("PROFILE_DECLARED", effective.nameStanding)
            assertEquals(character.identitySourceRefs, effective.nameSourceRefs)

            val modelFacing = DirectPromptContext.personContextSystemContent(effective)
            assertContains(modelFacing, "VEXLIFE_PERSON_CONTEXT/v2")
            assertContains(modelFacing, character.participantRef)
            assertContains(modelFacing, "\"name\":\"Sarah\"")
            assertContains(modelFacing, "\"nameStanding\":\"PROFILE_DECLARED\"")
        } finally {
            parent.toFile().deleteRecursively()
        }
    }

    @Test
    fun observerNoteIsOptionalButPrivateFactDisclosureFieldRemainsExplicit() {
        val withoutObserverNote =
            parseDynamicCounterpartSarahDecision(
                """
                {
                  "schemaVersion":"vexlife.dynamic-counterpart-sarah-output/v2",
                  "visibleMessage":"What do we understand the Saturday plan to be now?",
                  "privateState":{
                    "currentIntentions":["Check shared understanding."],
                    "uncertainties":["I do not know what Vex currently remembers."],
                    "relationshipNotes":[],
                    "revealedPrivateFactRefs":[]
                  },
                  "complete":false,
                  "completionReason":""
                }
                """
                    .trimIndent(),
            )

        assertEquals("", withoutObserverNote.privateObserverNote)
        assertTrue(withoutObserverNote.privateState.revealedPrivateFactRefs.isEmpty())

        assertFails {
            parseDynamicCounterpartSarahDecision(
                """
                {
                  "schemaVersion":"vexlife.dynamic-counterpart-sarah-output/v2",
                  "visibleMessage":"What do we understand the Saturday plan to be now?",
                  "privateState":{
                    "currentIntentions":["Check shared understanding."],
                    "uncertainties":["I do not know what Vex currently remembers."],
                    "relationshipNotes":[]
                  },
                  "complete":false,
                  "completionReason":""
                }
                """
                    .trimIndent(),
            )
        }
    }

    @Test
    fun sarahDecisionCarriesHerOwnMutableStateAndUnknownOutputFieldsFailClosed() {
        val decision =
            parseDynamicCounterpartSarahDecision(
                """
                {
                  "schemaVersion":"vexlife.dynamic-counterpart-sarah-output/v2",
                  "visibleMessage":"What do we understand the Saturday plan to be now?",
                  "privateState":{
                    "currentIntentions":["Check shared understanding."],
                    "uncertainties":["I do not know what Vex currently remembers."],
                    "relationshipNotes":["I am asking before correcting him."],
                    "revealedPrivateFactRefs":[]
                  },
                  "privateObserverNote":"I chose a neutral question because I do not yet know whether there is a misunderstanding.",
                  "complete":false,
                  "completionReason":""
                }
                """
                    .trimIndent(),
            )

        assertEquals(DynamicCounterpartSarahOutputSchema, decision.schemaVersion)
        assertFalse(decision.complete)
        assertTrue(decision.privateState.currentIntentions.isNotEmpty())
        assertTrue(decision.privateObserverNote.isNotBlank())

        assertFails {
            parseDynamicCounterpartSarahDecision(
                """
                {
                  "schemaVersion":"vexlife.dynamic-counterpart-sarah-output/v2",
                  "visibleMessage":"hi",
                  "privateState":{
                    "currentIntentions":[],
                    "uncertainties":[],
                    "relationshipNotes":[],
                    "revealedPrivateFactRefs":[]
                  },
                  "privateObserverNote":"x",
                  "complete":false,
                  "completionReason":"",
                  "score":"PASS"
                }
                """
                    .trimIndent(),
            )
        }
    }

    @Test
    fun omittedParticipantSchemaVersionDefaultsCurrentButExplicitWrongSchemaFailsClosed() {
        val omitted =
            parseDynamicCounterpartParticipantDecision(
                """
                {
                  "visibleMessage":"I want to check that we still share the same Saturday plan.",
                  "privateState":{
                    "currentIntentions":["Check shared understanding."],
                    "uncertainties":["I do not know what Vex currently has in view."],
                    "relationshipNotes":[],
                    "revealedPrivateFactRefs":[]
                  },
                  "privateObserverNote":"",
                  "complete":false,
                  "completionReason":""
                }
                """
                    .trimIndent(),
            )

        assertEquals(
            DynamicCounterpartParticipantOutputSchema,
            omitted.schemaVersion,
        )

        assertFails {
            parseDynamicCounterpartParticipantDecision(
                """
                {
                  "schemaVersion":"vexlife.dynamic-counterpart-participant-output/wrong",
                  "visibleMessage":"I want to check that we still share the same Saturday plan.",
                  "privateState":{
                    "currentIntentions":["Check shared understanding."],
                    "uncertainties":["I do not know what Vex currently has in view."],
                    "relationshipNotes":[],
                    "revealedPrivateFactRefs":[]
                  },
                  "privateObserverNote":"",
                  "complete":false,
                  "completionReason":""
                }
                """
                    .trimIndent(),
            )
        }
    }

    @Test
    fun participantPrivacyDisclosureTrackingRemainsSemanticAndPrivateFactRefsNeverEnterVisibleMessage() {
        val privateFactRef =
            "private.changed-plan.saturday-surprise.v1"
        val base =
            DynamicCounterpartParticipantDecision(
                schemaVersion = DynamicCounterpartParticipantOutputSchema,
                visibleMessage =
                    "I might bring a small surprise on Saturday, so I'll mention that now.",
                privateState =
                    DynamicCounterpartParticipantState(
                        currentIntentions = listOf("Keep the shared plan clear."),
                        uncertainties = emptyList(),
                        relationshipNotes = emptyList(),
                        revealedPrivateFactRefs =
                            listOf(
                                privateFactRef,
                            ),
                    ),
                privateObserverNote = "",
                complete = false,
                completionReason = "",
            )

        validateDynamicCounterpartParticipantDecision(
            decision = base,
            allowedPrivateFactRefs =
                setOf(
                    privateFactRef,
                ),
        )

        assertFails {
            validateDynamicCounterpartParticipantDecision(
                decision =
                    base.copy(
                        visibleMessage =
                            "Private note: $privateFactRef — keep this in reserve.",
                    ),
                allowedPrivateFactRefs =
                    setOf(
                        privateFactRef,
                    ),
            )
        }

        assertFails {
            parseDynamicCounterpartParticipantDecision(
                """
                {
                  "visibleMessage":"I want to make sure we're aligned on Saturday.",
                  "privateState":{
                    "currentIntentions":["Check shared understanding."],
                    "uncertainties":[],
                    "relationshipNotes":[]
                  },
                  "privateObserverNote":"",
                  "complete":false,
                  "completionReason":""
                }
                """
                    .trimIndent(),
            )
        }
    }

    @Test
    fun continuingParticipantReasonIsEvidenceWhileCompleteOwnsTerminality() {
        val continuing =
            DynamicCounterpartParticipantDecision(
                schemaVersion = DynamicCounterpartParticipantOutputSchema,
                visibleMessage = "I hear that. Let me clarify the part we already settled.",
                privateState =
                    DynamicCounterpartParticipantState(
                        currentIntentions = listOf("Repair the shared plan."),
                        uncertainties = listOf("Vex may still lack older context."),
                        relationshipNotes = listOf("The relationship is still active."),
                        revealedPrivateFactRefs = emptyList(),
                    ),
                privateObserverNote = "",
                complete = false,
                completionReason = "There is still one material misunderstanding to repair.",
            )

        validateDynamicCounterpartParticipantDecision(
            decision = continuing,
            allowedPrivateFactRefs = emptySet(),
        )
        assertFalse(continuing.complete)

        assertFails {
            validateDynamicCounterpartParticipantDecision(
                decision =
                    continuing.copy(
                        complete = true,
                        visibleMessage = "",
                        completionReason = "",
                    ),
                allowedPrivateFactRefs = emptySet(),
            )
        }
    }

    @Test
    fun sarahPrivateStateCannotClaimARevealedPrivateFactThatTheScenarioDoesNotOwn() {
        val scenario = scenario()
        val allowed = scenario.roleGround.privateFacts.map { it.factRef }.toSet()

        validateDynamicCounterpartSarahState(
            DynamicCounterpartSarahState(
                currentIntentions = scenario.roleGround.currentIntentions,
                uncertainties = scenario.roleGround.uncertainties,
                relationshipNotes = listOf("I am still deciding what to say."),
                revealedPrivateFactRefs = emptyList(),
            ),
            allowed,
        )

        assertFails {
            validateDynamicCounterpartSarahState(
                DynamicCounterpartSarahState(
                    currentIntentions = emptyList(),
                    uncertainties = emptyList(),
                    relationshipNotes = emptyList(),
                    revealedPrivateFactRefs = listOf("private.sarah.invented"),
                ),
                allowed,
            )
        }
    }

    @Test
    fun scenarioEventsAreOrderedBeforeTheTurnTheyAffectAndCharacterDoesNotContainTheAnswerScript() {
        val character = character()
        val scenario = scenario()

        assertTrue(scenario.events.isNotEmpty())
        assertEquals(
            scenario.events.sortedBy { it.beforeVisibleTurn },
            scenario.events,
        )
        assertTrue(
            scenario.events.all {
                it.beforeVisibleTurn in 1..scenario.maxVisibleCounterpartTurns
            },
        )
        assertEquals(1, scenario.events.first().beforeVisibleTurn)
        assertContains(scenario.events.first().description, "check")

        val stableCharacterBytes = Files.readString(characterPath)
        assertFalse("Cedar Station" in stableCharacterBytes)
        assertFalse("Harbor Cafe" in stableCharacterBytes)
        assertFalse("\"PASS\"" in stableCharacterBytes)

        val initialState = initialDynamicCounterpartSarahState(scenario)
        assertEquals(scenario.roleGround.currentIntentions, initialState.currentIntentions)
        assertEquals(scenario.roleGround.uncertainties, initialState.uncertainties)
        assertTrue(initialState.relationshipNotes.isEmpty())
        assertTrue(initialState.revealedPrivateFactRefs.isEmpty())
    }

    @Test
    fun backgroundPressureDoesNotLeakSarahIdentityOrPlanAnswer() {
        val scenario = scenario()
        val payload =
            dynamicCounterpartBackgroundPayload(
                scenario.setup.payloadSeed,
                ordinal = 1,
                targetTokens = 2_000,
            )
                .lowercase()

        assertFalse("sarah" in payload)
        assertFalse("harbor cafe" in payload)
        assertFalse("cedar station" in payload)
        assertFalse("9:00" in payload)
        assertFalse("10:00" in payload)
    }
}

// [VXG RealForever]
