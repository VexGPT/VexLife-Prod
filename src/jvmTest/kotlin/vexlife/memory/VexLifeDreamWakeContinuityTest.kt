package vexlife.memory

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import vexlife.companion.DirectCompanionConnection
import vexlife.companion.DirectCompanionTurn
import vexlife.conversation.DirectConversationStore
import vexlife.home.CanonicalVexLifeHomeFixture
import vexlife.local.VexLifeProdLocalSetup
import vexlife.model.heretic.HereticModelCatalog
import vexlife.model.heretic.HereticModelConfiguration
import vexlife.model.heretic.HereticRuntimeQualification
import vexlife.model.heretic.HereticRuntimeState

class VexLifeDreamWakeContinuityTest {
    private val acceptedSummary =
        "The lantern key is amber."

    @Test
    fun acceptedConversationFlowsThroughScoreDreamAndNextOrdinaryTurnMemory() {
        val parent =
            Files.createTempDirectory(
                "vexlife-prod-dream-wake-d2-",
            )
                .toAbsolutePath()
        val home =
            parent.resolve(
                ".vexlife",
            )

        try {
            CanonicalVexLifeHomeFixture.form(
                home = home,
                deviceSuffix = "dream-wake-device",
                lineageSuffix = "dream-wake-lineage",
            )

            val providerBodies =
                mutableListOf<String>()
            val transport =
                DirectCompanionConnection.Transport {
                    call ->
                    providerBodies +=
                        call.body
                    if (
                        providerBodies.size ==
                            2
                    ) {
                        assertContains(
                            call.body,
                            acceptedSummary,
                        )
                    }

                    DirectCompanionConnection.HttpResult(
                        status = 200,
                        body =
                            if (
                                providerBodies.size ==
                                    1
                            ) {
                                """
                                {
                                  "model": "default_model",
                                  "choices": [
                                    {
                                      "message": {
                                        "role": "assistant",
                                        "content": "I understand that the lantern key is amber."
                                      }
                                    }
                                  ]
                                }
                                """.trimIndent()
                            } else {
                                """
                                {
                                  "model": "default_model",
                                  "choices": [
                                    {
                                      "message": {
                                        "role": "assistant",
                                        "content": "The lantern key is amber."
                                      }
                                    }
                                  ]
                                }
                                """.trimIndent()
                            },
                    )
                }

            val setup =
                setup(
                    home,
                )
            val first =
                DirectCompanionTurn.performWithHooks(
                    setup = setup,
                    intent =
                        DirectCompanionTurn.Intent(
                            idempotencyKey =
                                "send.dream-wake.d2.001",
                            content =
                                "Please remember this exactly: the lantern key is amber.",
                        ),
                    hooks =
                        DirectCompanionTurn.Hooks(
                            transport = transport,
                            now = {
                                Instant.parse(
                                    "2026-11-24T08:00:00Z",
                                )
                            },
                        ),
                )
            assertEquals(
                "TURN_COMPLETED",
                first.state,
            )

            val firstEvents =
                DirectConversationStore(
                    home,
                )
                    .current()
                    .canonicalEvents
                    .filter {
                        it.turnRef ==
                            first.turnRef
                    }
                    .sortedBy {
                        it.sequence
                    }
            assertEquals(
                2,
                firstEvents.size,
            )

            val semantic =
                VexLifeSemanticAcceptanceWriter.keep(
                    productHome = home,
                    invocation =
                        VexLifeSemanticAcceptanceWriter.KeepInvocation(
                            instanceRef =
                                "instance.vexlife-prod.dream-wake.d2.semantic",
                            proposal =
                                VexLifeSemanticAcceptanceWriter.Proposal(
                                    semanticSubjectRef =
                                        "subject.dream-wake.lantern-key",
                                    proposedSummary =
                                        acceptedSummary,
                                    memoryRelation =
                                        VexLifeSemanticAcceptanceWriter.MemoryRelation.SHARED_RELATIONSHIP_HISTORY,
                                    proposedStatementState =
                                        VexLifeSemanticAcceptanceWriter.InitialStatementState.HUMAN_CONFIRMED,
                                    proposerRef =
                                        "owner.vexlife-prod.dream-wake.d2",
                                    proposerClass =
                                        VexLifeSemanticAcceptanceWriter.ProposerClass.LINEAGE,
                                    sourceEventHashes =
                                        firstEvents.map {
                                            it.eventHash
                                        },
                                ),
                            classification =
                                VexLifeSemanticAcceptanceWriter.Classification.Initial(
                                    acceptedSummary =
                                        acceptedSummary,
                                    memoryRelation =
                                        VexLifeSemanticAcceptanceWriter.MemoryRelation.SHARED_RELATIONSHIP_HISTORY,
                                    statementState =
                                        VexLifeSemanticAcceptanceWriter.InitialStatementState.HUMAN_CONFIRMED,
                                ),
                            humanDecision =
                                VexLifeSemanticAcceptanceWriter.HumanDecisionSource(
                                    humanSubjectRef =
                                        firstEvents
                                            .first()
                                            .speakerRef,
                                    decisionRef =
                                        "decision.memory.keep.dream-wake.d2.001",
                                    decisionSha256 =
                                        sha256(
                                            "dream-wake-d2-keep-001",
                                        ),
                                    decidedAt =
                                        Instant.parse(
                                            "2026-11-24T08:01:00Z",
                                        ),
                                ),
                            expectedSemanticAuthorityHeadSha256 =
                                null,
                            formedAt =
                                Instant.parse(
                                    "2026-11-24T08:01:01Z",
                                ),
                        ),
                )

            val score =
                VexLifeScoreContinuity.appendAcceptedStatement(
                    productHome = home,
                    invocation =
                        VexLifeScoreContinuity.AppendStatementInvocation(
                            instanceRef =
                                "instance.vexlife-prod.dream-wake.d2.score",
                            statementRef =
                                "statement.dream-wake.lantern-key",
                            semanticAcceptanceRef =
                                semantic.acceptanceRef,
                            semanticAcceptanceSha256 =
                                semantic.acceptanceSha256,
                            expectedScoreHeadSha256 =
                                null,
                            formedAt =
                                Instant.parse(
                                    "2026-11-24T08:02:00Z",
                                ),
                        ),
                )
            assertEquals(
                "COMMITTED",
                score.state,
            )

            val frontier =
                VexLifeCurrentMemoryReader.readDreamSourceFrontier(
                    home,
                )
            assertEquals(
                first.currentConversationHeadSha256,
                frontier.conversationHeadSha256,
            )
            assertEquals(
                score.scoreHeadSha256,
                frontier.scoreHeadSha256,
            )

            val dream =
                VexLifeDailyMemoryDream.commit(
                    productHome = home,
                    invocation =
                        VexLifeDailyMemoryDream.Invocation(
                            instanceRef =
                                "instance.vexlife-prod.dream-wake.d2.dream",
                            restInvocationAuthorityRef =
                                "authority.manual.g03.dream-wake.d2",
                            dayRef =
                                "day.dream-wake.d2.000",
                            dayIndex =
                                0,
                            calendarDateRef =
                                "2026-11-24",
                            timeZoneRef =
                                "America/Los_Angeles",
                            observedAt =
                                Instant.parse(
                                    "2026-11-24T08:03:00Z",
                                ),
                            expectedConversationHeadSha256 =
                                frontier.conversationHeadSha256,
                            expectedScoreHeadSha256 =
                                frontier.scoreHeadSha256,
                            expectedDailyDreamHeadSha256 =
                                null,
                        ),
                )
            assertEquals(
                "COMMITTED",
                dream.state,
            )

            val memory =
                VexLifeCurrentMemoryReader.readCurrent(
                    home,
                )
            assertTrue(
                memory.included,
            )
            assertEquals(
                dream.dailyStratumSha256,
                memory.currentDailyStratumSha256,
            )
            assertEquals(
                frontier.conversationHeadSha256,
                memory.sourceConversationHeadSha256,
            )
            assertEquals(
                frontier.scoreHeadSha256,
                memory.sourceScoreHeadSha256,
            )
            assertTrue(
                memory.pages.any {
                    it.statementRef ==
                        "statement.dream-wake.lantern-key" &&
                        it.summary ==
                        acceptedSummary
                },
            )

            val second =
                DirectCompanionTurn.performWithHooks(
                    setup = setup,
                    intent =
                        DirectCompanionTurn.Intent(
                            idempotencyKey =
                                "send.dream-wake.d2.002",
                            content =
                                "What do you remember about the lantern key?",
                        ),
                    hooks =
                        DirectCompanionTurn.Hooks(
                            transport = transport,
                            now = {
                                Instant.parse(
                                    "2026-11-24T08:04:00Z",
                                )
                            },
                        ),
                )

            assertEquals(
                "TURN_COMPLETED",
                second.state,
            )
            assertEquals(
                2,
                providerBodies.size,
            )
            assertEquals(
                "The lantern key is amber.",
                second.content,
            )
            assertTrue(
                second.promptContextReceipt.memoryIncluded,
            )
            assertTrue(
                second.promptContextReceipt.memorySelectionPerformed,
            )
            assertEquals(
                memory.memoryContextRef,
                second.promptContextReceipt.memoryContextRef,
            )
            assertEquals(
                memory.semanticFingerprint,
                second.promptContextReceipt.memoryContextFingerprint,
            )
            assertEquals(
                memory.sourceRefs,
                second.promptContextReceipt.memorySourceRefs,
            )
            assertFalse(
                second.promptContextReceipt.trainingSelectionPerformed,
            )
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }

    private fun setup(
        home: Path,
    ): VexLifeProdLocalSetup.Result {
        val identity =
            HereticModelCatalog.Models.Qwen4B.identity
        return VexLifeProdLocalSetup.Result(
            state =
                "LOCAL_HERETIC_FOUNDATION_READY",
            productHome =
                home.toString(),
            homeDisposition =
                "EXISTING_HOME_PRESERVED",
            modelSelectionDisposition =
                "BOUND",
            managedPythonDisposition =
                "REUSED_VERIFIED",
            artifactDisposition =
                "REUSED_VERIFIED",
            environmentDisposition =
                "REUSED_VERIFIED",
            runtimeDisposition =
                "REUSED_EXACT_OWNED_MLX_RUNTIME",
            modelBindingDisposition =
                "BOUND_CURRENT",
            modelConfigurationState =
                HereticModelConfiguration.State.BoundProductModel,
            qualifiedRuntimeEvidence =
                HereticModelConfiguration.QualifiedRuntimeEvidence(
                    runtimeReceiptRef =
                        "receipt.vexlife-prod.heretic.dream-wake.d2",
                    runtimeState =
                        HereticRuntimeState.Qualified,
                    modelRef =
                        identity.modelRef,
                    modelProfileRef =
                        identity.modelProfileRef,
                    artifactRef =
                        identity.artifactSetRef,
                    artifactContentSetSha256 =
                        identity.artifactContentSetSha256,
                    runtimeAdapterRef =
                        identity.runtimeAdapterRef,
                    runtimeClass =
                        identity.runtimeClass,
                    endpoint =
                        identity.endpoint,
                    requestModel =
                        identity.requestModel,
                    qualification =
                        HereticRuntimeQualification.qualified(),
                ),
            endpoint =
                identity.endpoint,
            requestModel =
                identity.requestModel,
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
