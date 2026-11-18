package vexlife.memory

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import kotlinx.serialization.Serializable
import vexlife.companion.DirectPromptContext
import vexlife.contract.VexContractJson

/**
 * Memory-owned provider serialization for current accepted semantic continuity.
 *
 * This object does not choose prompt placement. It takes the already-validated read-only projection
 * from [VexLifeCurrentAcceptedSemanticProjection], excludes exact statement refs already supplied
 * by another Memory surface (for example a current Daily block), and forms one bounded provider
 * block.
 *
 * Exact statement identity is the only dedupe key. This owner does not infer semantic equivalence
 * or rank statements.
 */
internal object VexLifeCurrentAcceptedSemanticPromptBlock {
    const val Schema = "vexlife-prod.current-accepted-semantic-prompt-block/v1"
    const val Header = "VEXLIFE_CURRENT_ACCEPTED_SEMANTIC/v1"

    @Serializable
    private data class Payload(
        val schemaVersion: String,
        val truthClass: String,
        val sourceProjectionRef: String,
        val sourceProjectionSemanticFingerprint: String,
        val observedConversationHeadSha256: String?,
        val sourceScoreConversationHeadSha256: String?,
        val scoreHeadSha256: String?,
        val semanticAuthorityHeadSha256: String?,
        val statements: List<VexLifeCurrentAcceptedSemanticProjection.Statement>,
    )

    @Serializable
    private data class Core(
        val schemaVersion: String,
        val sourceProjectionRef: String,
        val sourceProjectionSemanticFingerprint: String,
        val statementRefs: List<String>,
        val excludedStatementRefs: List<String>,
        val boundedOutStatementRefs: List<String>,
        val tokenBoundedOutStatementRefs: List<String>,
        val memoryTokenCap: Int,
        val contentSha256: String?,
        val estimatedContentTokens: Int,
    )

    data class Block(
        val schemaVersion: String,
        val blockRef: String,
        val semanticFingerprint: String,
        val sourceProjectionRef: String,
        val sourceProjectionSemanticFingerprint: String,
        val statementRefs: List<String>,
        val excludedStatementRefs: List<String>,
        val boundedOutStatementRefs: List<String>,
        val tokenBoundedOutStatementRefs: List<String>,
        val memoryTokenCap: Int,
        val content: String?,
        val contentSha256: String?,
        val estimatedContentTokens: Int,
        val sourceRefs: List<String>,
        val included: Boolean,
        val effectsPerformed: Boolean = false,
    )

    fun form(
        projection: VexLifeCurrentAcceptedSemanticProjection.Projection,
        excludedStatementRefs: Set<String> = emptySet(),
        memoryTokenCap: Int = VexLifeMemoryContract.MaximumMemoryTokens,
    ): Block {
        require(memoryTokenCap in 0..VexLifeMemoryContract.MaximumMemoryTokens) {
            "current accepted semantic prompt token cap is outside the admitted Memory range"
        }
        require(
            projection.schemaVersion == VexLifeCurrentAcceptedSemanticProjection.Schema &&
                projection.truthClass == VexLifeCurrentAcceptedSemanticProjection.TruthClass &&
                projection.state in setOf("EMPTY", "CURRENT") &&
                !projection.effectsPerformed
        ) {
            "current accepted semantic projection identity is invalid for provider serialization"
        }
        val projectionRefs =
            projection.statements.map(
                VexLifeCurrentAcceptedSemanticProjection.Statement::statementRef
            )
        require(projectionRefs.distinct().size == projectionRefs.size) {
            "current accepted semantic projection has duplicate statement refs"
        }
        if (projection.state == "EMPTY") {
            require(projection.statements.isEmpty()) {
                "empty current accepted semantic projection cannot carry statements"
            }
        } else {
            require(projection.statements.isNotEmpty()) {
                "current accepted semantic projection requires one statement"
            }
        }

        val excluded =
            excludedStatementRefs
                .filter {
                    it in projectionRefs
                }
                .distinct()
                .sorted()
        val available =
            projection.statements
                .filter {
                    it.statementRef !in excluded
                }
                .sortedBy {
                    it.statementRef
                }
        val pageSelected = available.take(VexLifeMemoryContract.MaximumPages)
        val boundedOut =
            available
                .drop(VexLifeMemoryContract.MaximumPages)
                .map {
                    it.statementRef
                }
                .sorted()

        val includedStatements = mutableListOf<VexLifeCurrentAcceptedSemanticProjection.Statement>()
        val tokenBoundedOut = mutableListOf<String>()

        for (statement in pageSelected) {
            val candidate = includedStatements + statement
            val content = providerContent(projection = projection, statements = candidate)
            if (DirectPromptContext.estimatedMessageTokens(content) > memoryTokenCap) {
                tokenBoundedOut += statement.statementRef
            } else {
                includedStatements += statement
            }
        }

        val content =
            includedStatements
                .takeIf {
                    it.isNotEmpty()
                }
                ?.let {
                    providerContent(projection = projection, statements = includedStatements)
                }
        val estimatedContentTokens = content?.let(DirectPromptContext::estimatedMessageTokens) ?: 0
        require(estimatedContentTokens <= memoryTokenCap) {
            "formed current accepted semantic provider block exceeds its Memory token cap"
        }
        val contentSha256 = content?.let(::sha256)
        val statementRefs = includedStatements.map {
            it.statementRef
        }
        val core =
            Core(
                schemaVersion = Schema,
                sourceProjectionRef = projection.projectionRef,
                sourceProjectionSemanticFingerprint = projection.semanticFingerprint,
                statementRefs = statementRefs,
                excludedStatementRefs = excluded,
                boundedOutStatementRefs = boundedOut,
                tokenBoundedOutStatementRefs = tokenBoundedOut.distinct().sorted(),
                memoryTokenCap = memoryTokenCap,
                contentSha256 = contentSha256,
                estimatedContentTokens = estimatedContentTokens,
            )
        val fingerprint = sha256(VexContractJson.canonicalEncode(core))
        val sourceRefs = buildList {
            add(projection.projectionRef)
            projection.observedConversationHeadSha256?.let {
                add("sha256:" + it)
            }
            projection.sourceScoreConversationHeadSha256?.let {
                add("sha256:" + it)
            }
            projection.scoreHeadSha256?.let {
                add("sha256:" + it)
            }
            projection.semanticAuthorityHeadSha256?.let {
                add("sha256:" + it)
            }
            includedStatements.forEach { statement ->
                add(statement.statementRef)
                add(statement.semanticAcceptanceRef)
            }
        }
            .distinct()
            .sorted()

        return Block(
            schemaVersion = Schema,
            blockRef =
                "prompt-block.vexlife-prod.current-accepted-semantic." + fingerprint.take(32),
            semanticFingerprint = fingerprint,
            sourceProjectionRef = projection.projectionRef,
            sourceProjectionSemanticFingerprint = projection.semanticFingerprint,
            statementRefs = statementRefs,
            excludedStatementRefs = excluded,
            boundedOutStatementRefs = boundedOut,
            tokenBoundedOutStatementRefs = tokenBoundedOut.distinct().sorted(),
            memoryTokenCap = memoryTokenCap,
            content = content,
            contentSha256 = contentSha256,
            estimatedContentTokens = estimatedContentTokens,
            sourceRefs = sourceRefs,
            included = content != null,
            effectsPerformed = false,
        )
    }

    private fun providerContent(
        projection: VexLifeCurrentAcceptedSemanticProjection.Projection,
        statements: List<VexLifeCurrentAcceptedSemanticProjection.Statement>,
    ): String {
        require(statements.isNotEmpty()) {
            "provider content requires at least one accepted semantic statement"
        }
        val payload =
            Payload(
                schemaVersion = Schema,
                truthClass = VexLifeCurrentAcceptedSemanticProjection.TruthClass,
                sourceProjectionRef = projection.projectionRef,
                sourceProjectionSemanticFingerprint = projection.semanticFingerprint,
                observedConversationHeadSha256 = projection.observedConversationHeadSha256,
                sourceScoreConversationHeadSha256 = projection.sourceScoreConversationHeadSha256,
                scoreHeadSha256 = projection.scoreHeadSha256,
                semanticAuthorityHeadSha256 = projection.semanticAuthorityHeadSha256,
                statements = statements,
            )
        return listOf(
                Header,
                "These are current human-authorized, source-attributed continuity statements. They are not proof of subjective recall or original wording by this model.",
                "Use them as bounded continuity context. If they conflict with current source or the human, preserve the conflict rather than inventing reconciliation.",
                VexContractJson.compactEncode(payload),
            )
            .joinToString("\n")
    }

    private fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(StandardCharsets.UTF_8))
            .joinToString("") {
                "%02x".format(it.toInt() and 0xff)
            }
}

// [VXG RealForever]
