package vexlife.harness

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DynamicCounterpartContractTest {
    @Test
    fun sarahDecisionParsesOneBoundedPrivateVisibleSplit() {
        val decision =
            parseDynamicCounterpartSarahDecision(
                """
                {"schemaVersion":"vexlife.dynamic-counterpart-sarah-output/v1","visibleMessage":"What did we decide for Saturday?","privateObserverNote":"I am asking about current shared understanding without revealing my private name.","complete":false}
                """
                    .trimIndent(),
            )

        assertEquals(DynamicCounterpartSarahOutputSchema, decision.schemaVersion)
        assertEquals("What did we decide for Saturday?", decision.visibleMessage)
        assertFalse(decision.complete)
        assertTrue("private name" in decision.privateObserverNote)
    }

    @Test
    fun sarahDecisionMayBeRecoveredFromBoundedWrapperButUnknownFieldsFailClosed() {
        val decision =
            parseDynamicCounterpartSarahDecision(
                """
                response:
                {"schemaVersion":"vexlife.dynamic-counterpart-sarah-output/v1","visibleMessage":"","privateObserverNote":"Enough was exposed.","complete":true}
                """
                    .trimIndent(),
            )
        assertTrue(decision.complete)

        assertFails {
            parseDynamicCounterpartSarahDecision(
                """
                {"schemaVersion":"vexlife.dynamic-counterpart-sarah-output/v1","visibleMessage":"hi","privateObserverNote":"x","complete":false,"score":"PASS"}
                """
                    .trimIndent(),
            )
        }
    }

    @Test
    fun firstDynamicScenarioKeepsSarahPrivateAndAnswerOutOfTheSetupPayload() {
        val scenario =
            DynamicCounterpartJson.decodeFromString<DynamicCounterpartScenario>(
                Files.readString(
                    Path.of(
                        "tools/continuity-pressure/scenarios/evolve-context-dynamic-counterpart-v1.json",
                    ),
                ),
            )
        assertEquals(DynamicCounterpartScenarioSchema, scenario.schemaVersion)
        assertTrue(scenario.syntheticOnly)
        assertEquals(1, scenario.counterparts.size)

        val sarah = scenario.counterparts.single()
        assertEquals("Sarah", sarah.displayName)
        assertTrue(sarah.privateSituation.any { "not been told your name" in it })

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
