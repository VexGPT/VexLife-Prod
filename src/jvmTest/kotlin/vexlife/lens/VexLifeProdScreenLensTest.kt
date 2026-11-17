package vexlife.lens

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import vexlife.workspace.VexLifeWorkspaceBlueprint

class VexLifeProdScreenLensTest {
    @Test
    fun committedLensIsCurrent() {
        val committed = Path.of(VexLifeProdScreenLens.CommittedPath)
        assertTrue(Files.exists(committed), "run ./gradlew generateVexLifeProdScreenLens")
        assertEquals(
            VexLifeProdScreenLens.exportText(),
            Files.readString(committed),
            "${VexLifeProdScreenLens.CommittedPath} is stale; run ./gradlew generateVexLifeProdScreenLens",
        )
    }

    @Test
    fun everyBlueprintElementAppearsOnceAndEveryBoundPathwayExists() {
        val lens = VexLifeProdScreenLens.export()
        val screen = lens["screens"]!!.jsonArray.single().jsonObject
        val elements = screen["elements"]!!.jsonArray.map { it.jsonObject }
        assertEquals(
            VexLifeWorkspaceBlueprint.elements.map { it.ref },
            elements.map { it.string("ref") },
        )

        val pathways =
            lens["pathways"]!!
                .jsonArray
                .map { it.jsonObject }
                .associateBy { it.string("pathwayRef") }
        elements
            .mapNotNull { it["pathwayRef"]?.jsonPrimitive?.takeIf { p -> p.isString }?.content }
            .forEach {
                assertTrue(it in pathways, "element names undefined pathway $it")
            }
        assertEquals(
            "SCAE_FAMILY",
            pathways.getValue("pathway.vexlife-prod.vision.selected-scope").string("disposition"),
        )
        val send = elements.single { it.string("ref") == VexLifeWorkspaceBlueprint.Send.ref }
        assertEquals("pathway.vexlife-prod.conversation.send", send.string("pathwayRef"))
        assertEquals("VexLifeWorkspaceBlueprint.Send", send.string("symbol"))
        val sendPath = pathways.getValue("pathway.vexlife-prod.conversation.send")
        assertEquals("SCAE_FAMILY", sendPath.string("disposition"))
        assertTrue(
            sendPath.toString().contains("VexLifeConversationTurnFamily") &&
                sendPath.toString().contains("BeginTurn(attempt)") &&
                sendPath.toString().contains("active-turn truth"),
        )

        val stop = elements.single { it.string("ref") == VexLifeWorkspaceBlueprint.Stop.ref }
        assertEquals("capability conversation.cancel", stop.string("intent"))
        assertEquals("pathway.vexlife-prod.conversation.cancel", stop.string("pathwayRef"))
        assertEquals("VexLifeWorkspaceBlueprint.Stop", stop.string("symbol"))
        val cancel = pathways.getValue("pathway.vexlife-prod.conversation.cancel")
        assertEquals("SCAE_FAMILY", cancel.string("disposition"))
        assertTrue(
            cancel.toString().contains("VexLifeConversationTurnFamily") &&
                cancel.toString().contains("RequestCancel(attempt)") &&
                cancel.toString().contains("DirectCompanionCancellation") &&
                cancel.toString().contains("TURN_CANCELLED_BY_PERSON"),
        )

        assertTrue((lens["chrome"] as JsonArray).isEmpty())
    }

    private fun JsonObject.string(key: String) = getValue(key).jsonPrimitive.content
}

// [VXG RealForever]
