package vexlife.memory

import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.security.MessageDigest
import kotlinx.serialization.Serializable
import vexlife.contract.VexContractJson

/**
 * Read-only Product projection of accepted semantic continuity that is current by Score membership
 * and current semantic authority, independently from Daily Memory consolidation freshness.
 *
 * This owner stores nothing and is not prompt placement. It exists so a later connector can consume
 * accepted meaning without pretending an older Daily snapshot is freshly consolidated.
 */
internal object VexLifeCurrentAcceptedSemanticProjection {
    const val Schema =
        "vexlife-prod.current-accepted-semantic-continuity/v1"
    const val TruthClass =
        "CURRENT_ACCEPTED_SEMANTIC_CONTINUITY"

    @Serializable
    data class Statement(
        val statementRef: String,
        val subjectRef: String,
        val semanticSubjectFingerprint: String,
        val summary: String,
        val summaryHash: String,
        val memoryRelation: String,
        val recordedStatementState: String,
        val effectiveState: String,
        val consentState: String,
        val semanticAcceptanceRef: String,
        val semanticAcceptanceSha256: String,
        val semanticAuthorityHeadSha256: String,
        val sourceConversationHeadSha256: String,
    )

    @Serializable
    private data class Core(
        val schemaVersion: String,
        val truthClass: String,
        val state: String,
        val homeRef: String,
        val deviceRef: String,
        val companionLineageRef: String,
        val threadRef: String,
        val observedConversationHeadSha256: String?,
        val sourceScoreConversationHeadSha256: String?,
        val scoreHeadSha256: String?,
        val semanticAuthorityHeadSha256: String?,
        val statements: List<Statement>,
        val effectsPerformed: Boolean,
    )

    @Serializable
    data class Projection(
        val schemaVersion: String,
        val truthClass: String,
        val state: String,
        val projectionRef: String,
        val semanticFingerprint: String,
        val homeRef: String,
        val deviceRef: String,
        val companionLineageRef: String,
        val threadRef: String,
        val observedConversationHeadSha256: String?,
        val sourceScoreConversationHeadSha256: String?,
        val scoreHeadSha256: String?,
        val semanticAuthorityHeadSha256: String?,
        val statements: List<Statement>,
        val sourceRefs: List<String>,
        val effectsPerformed: Boolean,
    )

    fun read(
        productHome: Path,
    ): Projection {
        val snapshot =
            VexLifeCurrentMemoryReader
                .readCurrentAcceptedSemanticSnapshot(
                    productHome,
                )
        val statements =
            snapshot.statements.map {
                value ->
                Statement(
                    statementRef = value.statementRef,
                    subjectRef = value.subjectRef,
                    semanticSubjectFingerprint =
                        value.semanticSubjectFingerprint,
                    summary = value.summary,
                    summaryHash = value.summaryHash,
                    memoryRelation = value.memoryRelation,
                    recordedStatementState =
                        value.recordedStatementState,
                    effectiveState = value.effectiveState,
                    consentState = value.consentState,
                    semanticAcceptanceRef =
                        value.semanticAcceptanceRef,
                    semanticAcceptanceSha256 =
                        value.semanticAcceptanceSha256,
                    semanticAuthorityHeadSha256 =
                        value.semanticAuthorityHeadSha256,
                    sourceConversationHeadSha256 =
                        value.sourceConversationHeadSha256,
                )
            }
        val state =
            if (
                statements.isEmpty()
            ) {
                "EMPTY"
            } else {
                "CURRENT"
            }
        require(
            (
                state ==
                    "EMPTY"
            ) ||
                (
                    state ==
                        "CURRENT" &&
                        snapshot.scoreHeadSha256 !=
                            null &&
                        snapshot.sourceScoreConversationHeadSha256 !=
                            null &&
                        snapshot.semanticAuthorityHeadSha256 !=
                            null
                ),
        ) {
            "current accepted semantic projection state is inconsistent"
        }

        val core =
            Core(
                schemaVersion = Schema,
                truthClass = TruthClass,
                state = state,
                homeRef = snapshot.homeRef,
                deviceRef = snapshot.deviceRef,
                companionLineageRef =
                    snapshot.companionLineageRef,
                threadRef = snapshot.threadRef,
                observedConversationHeadSha256 =
                    snapshot.observedConversationHeadSha256,
                sourceScoreConversationHeadSha256 =
                    snapshot.sourceScoreConversationHeadSha256,
                scoreHeadSha256 =
                    snapshot.scoreHeadSha256,
                semanticAuthorityHeadSha256 =
                    snapshot.semanticAuthorityHeadSha256,
                statements = statements,
                effectsPerformed = false,
            )
        val fingerprint =
            sha256(
                VexContractJson.canonicalEncode(
                    core,
                ),
            )
        val sourceRefs =
            buildList {
                snapshot.observedConversationHeadSha256
                    ?.let {
                        add(
                            "sha256:" +
                                it,
                        )
                    }
                snapshot.sourceScoreConversationHeadSha256
                    ?.let {
                        add(
                            "sha256:" +
                                it,
                        )
                    }
                snapshot.scoreHeadSha256
                    ?.let {
                        add(
                            "sha256:" +
                                it,
                        )
                    }
                snapshot.semanticAuthorityHeadSha256
                    ?.let {
                        add(
                            "sha256:" +
                                it,
                        )
                    }
                statements.forEach {
                    statement ->
                    add(
                        statement.statementRef,
                    )
                    add(
                        statement.semanticAcceptanceRef,
                    )
                }
            }
                .distinct()
                .sorted()

        return Projection(
            schemaVersion = core.schemaVersion,
            truthClass = core.truthClass,
            state = core.state,
            projectionRef =
                "projection.vexlife-prod.current-accepted-semantic." +
                    fingerprint.take(
                        32,
                    ),
            semanticFingerprint = fingerprint,
            homeRef = core.homeRef,
            deviceRef = core.deviceRef,
            companionLineageRef =
                core.companionLineageRef,
            threadRef = core.threadRef,
            observedConversationHeadSha256 =
                core.observedConversationHeadSha256,
            sourceScoreConversationHeadSha256 =
                core.sourceScoreConversationHeadSha256,
            scoreHeadSha256 =
                core.scoreHeadSha256,
            semanticAuthorityHeadSha256 =
                core.semanticAuthorityHeadSha256,
            statements = core.statements,
            sourceRefs = sourceRefs,
            effectsPerformed = false,
        )
    }

    internal fun reobserve(
        projection: Projection,
        observedConversationHeadSha256: String?,
    ): Projection {
        require(
            projection.schemaVersion ==
                Schema &&
                projection.truthClass ==
                    TruthClass &&
                projection.state in
                    setOf(
                        "EMPTY",
                        "CURRENT",
                    ) &&
                !projection.effectsPerformed
        ) {
            "current accepted semantic projection identity is invalid for re-observation"
        }
        observedConversationHeadSha256
            ?.let {
                require(
                    it.matches(
                        Regex(
                            "^[0-9a-f]{64}$",
                        ),
                    )
                ) {
                    "observed Conversation head must be one SHA-256"
                }
            }
        if (
            projection.observedConversationHeadSha256 ==
                observedConversationHeadSha256
        ) {
            return projection
        }

        val core =
            Core(
                schemaVersion =
                    projection.schemaVersion,
                truthClass =
                    projection.truthClass,
                state =
                    projection.state,
                homeRef =
                    projection.homeRef,
                deviceRef =
                    projection.deviceRef,
                companionLineageRef =
                    projection.companionLineageRef,
                threadRef =
                    projection.threadRef,
                observedConversationHeadSha256 =
                    observedConversationHeadSha256,
                sourceScoreConversationHeadSha256 =
                    projection.sourceScoreConversationHeadSha256,
                scoreHeadSha256 =
                    projection.scoreHeadSha256,
                semanticAuthorityHeadSha256 =
                    projection.semanticAuthorityHeadSha256,
                statements =
                    projection.statements,
                effectsPerformed = false,
            )
        val fingerprint =
            sha256(
                VexContractJson.canonicalEncode(
                    core,
                ),
            )
        val sourceRefs =
            buildList {
                observedConversationHeadSha256
                    ?.let {
                        add(
                            "sha256:" +
                                it,
                        )
                    }
                projection.sourceScoreConversationHeadSha256
                    ?.let {
                        add(
                            "sha256:" +
                                it,
                        )
                    }
                projection.scoreHeadSha256
                    ?.let {
                        add(
                            "sha256:" +
                                it,
                        )
                    }
                projection.semanticAuthorityHeadSha256
                    ?.let {
                        add(
                            "sha256:" +
                                it,
                        )
                    }
                projection.statements.forEach {
                    statement ->
                    add(
                        statement.statementRef,
                    )
                    add(
                        statement.semanticAcceptanceRef,
                    )
                }
            }
                .distinct()
                .sorted()

        return Projection(
            schemaVersion =
                core.schemaVersion,
            truthClass =
                core.truthClass,
            state =
                core.state,
            projectionRef =
                "projection.vexlife-prod.current-accepted-semantic." +
                    fingerprint.take(
                        32,
                    ),
            semanticFingerprint =
                fingerprint,
            homeRef =
                core.homeRef,
            deviceRef =
                core.deviceRef,
            companionLineageRef =
                core.companionLineageRef,
            threadRef =
                core.threadRef,
            observedConversationHeadSha256 =
                core.observedConversationHeadSha256,
            sourceScoreConversationHeadSha256 =
                core.sourceScoreConversationHeadSha256,
            scoreHeadSha256 =
                core.scoreHeadSha256,
            semanticAuthorityHeadSha256 =
                core.semanticAuthorityHeadSha256,
            statements =
                core.statements,
            sourceRefs =
                sourceRefs,
            effectsPerformed = false,
        )
    }

    private fun sha256(
        value: String,
    ): String =
        MessageDigest
            .getInstance(
                "SHA-256",
            )
            .digest(
                value.toByteArray(
                    StandardCharsets.UTF_8,
                ),
            )
            .joinToString(
                "",
            ) {
                "%02x".format(
                    it.toInt() and
                        0xff,
                )
            }
}

// [VXG RealForever]
