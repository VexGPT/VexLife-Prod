package vexlife.memory

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VexLifeCurrentAcceptedSemanticPromptBlockTest {
    @Test
    fun emptyProjectionFormsNoProviderContent() {
        val block =
            VexLifeCurrentAcceptedSemanticPromptBlock.form(
                projection = projection(statements = emptyList())
            )

        assertFalse(block.included)
        assertNull(block.content)
        assertNull(block.contentSha256)
        assertEquals(0, block.estimatedContentTokens)
        assertTrue(block.statementRefs.isEmpty())
        assertFalse(block.effectsPerformed)
    }

    @Test
    fun acceptedStatementFormsMemoryOwnedProviderBlock() {
        val statement =
            statement(
                suffix = "a",
                summary = "Victor uses tight to mean compact and causally exact.",
            )
        val projection = projection(statements = listOf(statement))
        val block = VexLifeCurrentAcceptedSemanticPromptBlock.form(projection)

        assertTrue(block.included)
        assertEquals(listOf(statement.statementRef), block.statementRefs)
        val content = assertNotNull(block.content)
        assertTrue(content.startsWith(VexLifeCurrentAcceptedSemanticPromptBlock.Header))
        assertTrue(content.contains(statement.summary))
        assertTrue(content.contains("not proof of subjective recall"))
        assertTrue(block.estimatedContentTokens > 0)
    }

    @Test
    fun exactDailyStatementRefIsExcludedWithoutSemanticGuessing() {
        val first =
            statement(
                suffix = "a",
                summary = "Victor uses tight to mean compact and causally exact.",
            )
        val second =
            statement(
                suffix = "b",
                summary = "Victor prefers the explanation to preserve causal ordering.",
            )
        val block =
            VexLifeCurrentAcceptedSemanticPromptBlock.form(
                projection = projection(statements = listOf(first, second)),
                excludedStatementRefs = setOf(first.statementRef, "statement.not-in-projection"),
            )

        assertEquals(listOf(first.statementRef), block.excludedStatementRefs)
        assertEquals(listOf(second.statementRef), block.statementRefs)
        val content = assertNotNull(block.content)
        assertFalse(content.contains(first.summary))
        assertTrue(content.contains(second.summary))
    }

    @Test
    fun tokenCapBoundsProviderContentWithoutChangingProjection() {
        val first = statement(suffix = "a", summary = "A short accepted statement.")
        val second =
            statement(
                suffix = "b",
                summary =
                    "Another accepted statement whose exact identity remains available even if the provider block cannot fit it.",
            )
        val projection = projection(statements = listOf(first, second))
        val unrestricted = VexLifeCurrentAcceptedSemanticPromptBlock.form(projection)
        val bounded =
            VexLifeCurrentAcceptedSemanticPromptBlock.form(
                projection = projection,
                memoryTokenCap = 1,
            )

        assertTrue(unrestricted.included)
        assertFalse(bounded.included)
        assertTrue(
            bounded.tokenBoundedOutStatementRefs.containsAll(
                listOf(first.statementRef, second.statementRef)
            )
        )
        assertEquals(
            listOf(first, second),
            projection.statements,
            "provider bounding must not rewrite the accepted-semantic projection",
        )
    }

    private fun projection(
        statements: List<VexLifeCurrentAcceptedSemanticProjection.Statement>
    ): VexLifeCurrentAcceptedSemanticProjection.Projection {
        val current = statements.isNotEmpty()
        return VexLifeCurrentAcceptedSemanticProjection.Projection(
            schemaVersion = VexLifeCurrentAcceptedSemanticProjection.Schema,
            truthClass = VexLifeCurrentAcceptedSemanticProjection.TruthClass,
            state =
                if (current) {
                    "CURRENT"
                } else {
                    "EMPTY"
                },
            projectionRef = "projection.vexlife-prod.current-accepted-semantic." + "a".repeat(32),
            semanticFingerprint = "1".repeat(64),
            homeRef = "home.vexlife.test",
            deviceRef = "device.vexlife.test",
            companionLineageRef = "companion-lineage.vexlife.test",
            threadRef = "thread.vexlife.direct.default",
            observedConversationHeadSha256 = "2".repeat(64),
            sourceScoreConversationHeadSha256 =
                if (current) {
                    "3".repeat(64)
                } else {
                    null
                },
            scoreHeadSha256 =
                if (current) {
                    "4".repeat(64)
                } else {
                    null
                },
            semanticAuthorityHeadSha256 =
                if (current) {
                    "5".repeat(64)
                } else {
                    null
                },
            statements = statements,
            sourceRefs = emptyList(),
            effectsPerformed = false,
        )
    }

    private fun statement(
        suffix: String,
        summary: String,
    ): VexLifeCurrentAcceptedSemanticProjection.Statement =
        VexLifeCurrentAcceptedSemanticProjection.Statement(
            statementRef = "statement.vexlife-prod.memory-review." + suffix.repeat(32),
            subjectRef = "subject.shared-language." + suffix,
            semanticSubjectFingerprint = "6".repeat(64),
            summary = summary,
            summaryHash = "7".repeat(64),
            memoryRelation = "SHARED_RELATIONSHIP_HISTORY",
            recordedStatementState = "HUMAN_CONFIRMED",
            effectiveState = "HUMAN_CONFIRMED",
            consentState = "PERMITTED",
            semanticAcceptanceRef = "score-semantic-acceptance." + suffix.repeat(32),
            semanticAcceptanceSha256 = "8".repeat(64),
            semanticAuthorityHeadSha256 = "9".repeat(64),
            sourceConversationHeadSha256 = "a".repeat(64),
        )
}

// [VXG RealForever]
