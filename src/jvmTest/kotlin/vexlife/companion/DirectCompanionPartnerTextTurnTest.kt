package vexlife.companion

import java.nio.file.Files
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import vexlife.conversation.CanonicalLivedContextFixture
import vexlife.conversation.DirectConversationIdentity
import vexlife.conversation.DirectConversationStore
import vexlife.conversation.VexLifeConversationContract
import vexlife.conversation.VexLifeSemanticRelayContract
import vexlife.home.CanonicalVexLifeHomeFixture
import vexlife.local.VexLifeProdLocalSetup
import vexlife.model.VexLifeInferenceSelection
import vexlife.model.VexLifeInferenceSessionAdmission
import vexlife.model.VexLifeModelSourceProjection
import vexlife.model.heretic.HereticModelCatalog
import vexlife.model.heretic.HereticModelConfiguration
import vexlife.model.heretic.HereticRuntimeQualification
import vexlife.model.heretic.HereticRuntimeState
import vexlife.partner.VexLifePartnerAccountScope
import vexlife.partner.VexLifePartnerHostEffect
import vexlife.partner.VexLifePartnerModelProfileCatalog

class DirectCompanionPartnerTextTurnTest {
    private fun productHome(): java.nio.file.Path {
        val parent =
            Files.createTempDirectory(
                "vexlife-prod-partner-text-turn-",
            )
                .toAbsolutePath()
        val home =
            parent.resolve(
                "VexLife-Prod",
            )
        CanonicalVexLifeHomeFixture.form(
            home = home,
            deviceSuffix = "partner-text-device",
            lineageSuffix = "partner-text-lineage",
        )
        return home
    }

    private fun setup(
        home: java.nio.file.Path,
    ): VexLifeProdLocalSetup.Result {
        val identity =
            HereticModelCatalog.Models.Qwen27B.identity
        return VexLifeProdLocalSetup.Result(
            state = "LOCAL_HERETIC_FOUNDATION_READY",
            productHome = home.toString(),
            homeDisposition = "EXISTING_HOME_PRESERVED",
            modelSelectionDisposition = "BOUND",
            managedPythonDisposition = "REUSED_VERIFIED",
            artifactDisposition = "REUSED_VERIFIED",
            environmentDisposition = "REUSED_VERIFIED",
            runtimeDisposition = "REUSED_EXACT_OWNED_MLX_RUNTIME",
            modelBindingDisposition = "BOUND_CURRENT",
            modelConfigurationState = HereticModelConfiguration.State.BoundProductModel,
            qualifiedRuntimeEvidence =
                HereticModelConfiguration.QualifiedRuntimeEvidence(
                    runtimeReceiptRef = "receipt.vexlife-prod.heretic.partner-text-fixture",
                    runtimeState = HereticRuntimeState.Qualified,
                    modelRef = identity.modelRef,
                    modelProfileRef = identity.modelProfileRef,
                    artifactRef = identity.artifactSetRef,
                    artifactContentSetSha256 = identity.artifactContentSetSha256,
                    runtimeAdapterRef = identity.runtimeAdapterRef,
                    runtimeClass = identity.runtimeClass,
                    endpoint = identity.endpoint,
                    requestModel = identity.requestModel,
                    qualification = HereticRuntimeQualification.qualified(),
                ),
            endpoint = identity.endpoint,
            requestModel = identity.requestModel,
        )
    }

    private fun qualifiedPartner(): VexLifeInferenceSessionAdmission.Qualified {
        val profile =
            VexLifePartnerModelProfileCatalog.OpenAiGpt62Sol
        return VexLifeInferenceSessionAdmission.Qualified(
            coordinate =
                VexLifeInferenceSelection.Coordinate(
                    handle = profile.profileRef,
                    modelRef = profile.modelRef,
                    sourceKind = VexLifeModelSourceProjection.SourceKind.PARTNER,
                    providerRef = profile.providerRef,
                ),
            profileRef = profile.profileRef,
            qualificationRef = "qualification.vexlife.partner.normal-turn.fixture",
            contextWindowTokens = null,
            partnerCredentialRef =
                "credential.vexlife.partner.normal-turn-fixture",
            partnerAccountScope =
                VexLifePartnerAccountScope.Selection(
                    providerRef = profile.providerRef,
                ),
        )
    }

    private fun relay(
        messageRef: String,
        originatorRef: String,
        originatorKind: String,
        recipientRef: String,
        targetAudienceRef: String,
    ) =
        buildJsonObject {
            put(
                "schemaVersion",
                VexLifeSemanticRelayContract.Schema,
            )
            put(
                "relayRef",
                "relay.$messageRef",
            )
            put(
                "sourceMessageRef",
                messageRef,
            )
            put(
                "sourceLanguageRef",
                "language.unknown",
            )
            put(
                "sourceLocaleRef",
                JsonNull,
            )
            put(
                "preferredConversationLanguageRef",
                "language.en",
            )
            put(
                "requestedResponseLanguageRef",
                "language.en",
            )
            put(
                "uiLocaleRef",
                JsonNull,
            )
            put(
                "originatorRef",
                originatorRef,
            )
            put(
                "originatorKind",
                originatorKind,
            )
            put(
                "onBehalfOfOriginator",
                false,
            )
            put(
                "materiality",
                "ORDINARY",
            )
            put(
                "ambiguityState",
                "CLEAR",
            )
            put(
                "recipientRefs",
                buildJsonArray {
                    add(
                        JsonPrimitive(
                            recipientRef,
                        ),
                    )
                },
            )
            put(
                "intentRefs",
                buildJsonArray {
                    add(
                        JsonPrimitive(
                            "intent.vexlife.partner-text-turn.fixture",
                        ),
                    )
                },
            )
            put(
                "canonicalMeaningRefs",
                buildJsonArray {
                    add(
                        JsonPrimitive(
                            "meaning.vexlife.partner-text-turn.fixture",
                        ),
                    )
                },
            )
            put(
                "interpretationProjectionRef",
                "projection.vexlife.partner-text-turn.fixture",
            )
            put(
                "interpretationState",
                "CANDIDATE",
            )
            put(
                "confirmedByRef",
                JsonNull,
            )
            put(
                "confirmationReceiptRef",
                JsonNull,
            )
            put(
                "supersedesInterpretationProjectionRef",
                JsonNull,
            )
            put(
                "boundaryClassRef",
                "boundary.device-private.partner-text-turn",
            )
            put(
                "targets",
                buildJsonArray {
                    add(
                        buildJsonObject {
                            put(
                                "recipientRef",
                                recipientRef,
                            )
                            put(
                                "recipientPreferredLanguageRef",
                                "language.en",
                            )
                            put(
                                "targetLanguageRef",
                                "language.en",
                            )
                            put(
                                "targetAudienceRef",
                                targetAudienceRef,
                            )
                            put(
                                "runtimeCapability",
                                buildJsonObject {
                                    put(
                                        "capabilityRef",
                                        JsonNull,
                                    )
                                    put(
                                        "currentnessState",
                                        "UNKNOWN",
                                    )
                                    put(
                                        "multilingualOutput",
                                        false,
                                    )
                                    put(
                                        "supportedLanguageRefs",
                                        buildJsonArray {
                                        },
                                    )
                                    put(
                                        "evidenceRefs",
                                        buildJsonArray {
                                        },
                                    )
                                },
                            )
                            put(
                                "localeQualityState",
                                "UNKNOWN",
                            )
                            put(
                                "terminologyState",
                                "UNKNOWN",
                            )
                            put(
                                "authorityState",
                                "UNKNOWN",
                            )
                            put(
                                "localizationReadinessState",
                                "UNKNOWN",
                            )
                            put(
                                "humanReviewAvailable",
                                false,
                            )
                            put(
                                "projectionMode",
                                "NONE",
                            )
                            put(
                                "localizedProjectionRef",
                                JsonNull,
                            )
                            put(
                                "semanticEquivalenceState",
                                "NOT_CHECKED",
                            )
                            put(
                                "semanticDriftFindingRefs",
                                buildJsonArray {
                                },
                            )
                            put(
                                "deliveryState",
                                "NOT_DELIVERED",
                            )
                            put(
                                "acknowledgementState",
                                "NOT_REQUESTED",
                            )
                            put(
                                "understandingState",
                                "NOT_ASSESSED",
                            )
                        },
                    )
                },
            )
            put(
                "sourceRefs",
                buildJsonArray {
                    add(
                        JsonPrimitive(
                            "source.vexlife.partner-text-turn.fixture",
                        ),
                    )
                },
            )
            put(
                "evidenceRefs",
                buildJsonArray {
                    add(
                        JsonPrimitive(
                            "evidence.vexlife.partner-text-turn.fixture",
                        ),
                    )
                },
            )
            put(
                "authorityRefs",
                buildJsonArray {
                    add(
                        JsonPrimitive(
                            "authority.vexlife.partner-text-turn.fixture",
                        ),
                    )
                },
            )
        }
            .also {
                value ->
                val validation =
                    VexLifeSemanticRelayContract.validate(
                        relay = value,
                        sourceMessageRef = messageRef,
                        recipientRefs =
                            listOf(
                                recipientRef,
                            ),
                    )
                require(
                    validation.ok,
                ) {
                    validation.errors.joinToString()
                }
            }

    private fun resource(
        relative: String,
    ): String =
        requireNotNull(
            DirectCompanionPartnerTextTurnTest::class.java.getResourceAsStream(
                "/vexlife/partner/provider-contract-fixtures/" +
                    relative,
            ),
        )
            .bufferedReader()
            .use {
                it.readText()
            }

    @Test
    fun qualifiedPartnerTextStageCompletesCanonicalV2V3TurnAndRecoversWithoutReplay() {
        val home =
            productHome()
        val setup =
            setup(
                home,
            )
        val qualified =
            qualifiedPartner()
        val body =
            resource(
                "openai/responses-text.stream.txt",
            )
        var hostCalls =
            0
        val executor =
            VexLifePartnerHostEffect.Executor {
                plan ->
                hostCalls +=
                    1
                VexLifePartnerHostEffect.Result.Http(
                    status = 200,
                    body = body,
                    providerRequestId = "req_normal_turn_fixture",
                    receipt =
                        VexLifePartnerHostEffect.receiptForHttp(
                            plan = plan,
                            status = 200,
                            body = body,
                        ),
                )
            }
        val idempotencyKey =
            "send.self-development.partner-text-turn.001"
        val identity =
            DirectCompanionTurn.turnIdentityForTest(
                idempotencyKey,
            )
        val requestRelay =
            relay(
                messageRef = identity.requestMessageRef,
                originatorRef = "person.local-user",
                originatorKind = "HUMAN",
                recipientRef = DirectConversationIdentity.CompanionRoleRef,
                targetAudienceRef = "audience.vex",
            )
        val responseRelay =
            relay(
                messageRef = identity.responseMessageRef,
                originatorRef = DirectConversationIdentity.CompanionRoleRef,
                originatorKind = "AI",
                recipientRef = "person.local-user",
                targetAudienceRef = "audience.person",
            )
        val partner =
            DirectCompanionTurn.PartnerTextTurn(
                qualified = qualified,
                executor = executor,
                requestSemanticRelay = requestRelay,
                responseSemanticRelay = responseRelay,
            )
        val intent =
            DirectCompanionTurn.Intent(
                idempotencyKey = idempotencyKey,
                content = "Say hello through the admitted Partner realization.",
                personContext =
                    DirectPromptContext.PersonContext(
                        replyLanguage = "en",
                        replyLanguageName = "English",
                        privateConversations = emptyList(),
                    ),
            )
        val hooks =
            DirectCompanionTurn.Hooks(
                partnerTextTurn = partner,
                now = {
                    Instant.parse(
                        "2026-11-24T22:00:00Z",
                    )
                },
            )

        try {
            val first =
                DirectCompanionTurn.performWithHooks(
                    setup = setup,
                    intent = intent,
                    hooks = hooks,
                )

            assertEquals(
                1,
                hostCalls,
            )
            assertEquals(
                "TURN_COMPLETED",
                first.state,
            )
            assertFalse(
                first.recoveredCompletedTurn,
            )
            assertEquals(
                "Hello from Vex",
                first.content,
            )
            assertEquals(
                VexLifeModelSourceProjection.SourceKind.PARTNER,
                first.providerObservation.sourceKind,
            )
            assertEquals(
                qualified.coordinate.providerRef,
                first.providerObservation.providerRef,
            )
            assertFalse(
                first.providerInvocationEvidence.loopbackOnly,
            )
            assertEquals(
                "req_normal_turn_fixture",
                first.providerInvocationEvidence.providerRequestId,
            )
            assertNull(
                first.localRuntimeObservation,
            )
            assertNull(
                first.localInvocationEvidence,
            )
            assertNull(
                first.promptContextReceipt.capabilityFrameSnapshot,
            )
            assertEquals(
                0,
                first.promptContextReceipt.capabilityExchangeAllowanceTokens,
            )
            assertFalse(
                first.promptContextReceipt.sourceReturnProtocolEnabled,
            )

            val snapshot =
                DirectConversationStore(
                    home,
                )
                    .current()
            val request =
                assertNotNull(
                    snapshot.events.firstOrNull {
                        it.eventRef ==
                            first.requestEventRef
                    },
                )
            val response =
                assertNotNull(
                    snapshot.events.firstOrNull {
                        it.eventRef ==
                            first.responseEventRef
                    },
                )
            assertEquals(
                VexLifeConversationContract.EventSchemaV2,
                request.schemaVersion,
            )
            assertEquals(
                VexLifeConversationContract.EventSchemaV3,
                response.schemaVersion,
            )
            assertEquals(
                qualified.coordinate.providerRef,
                response.providerRef,
            )
            assertEquals(
                qualified.profileRef,
                response.endpointProfileRef,
            )
            assertEquals(
                first.providerObservation.endpointOrigin,
                response.sanitizedEndpointOrigin,
            )
            assertEquals(
                qualified.coordinate.modelRef,
                response.model,
            )
            assertEquals(
                identity.requestMessageRef,
                request.semanticRelay
                    ?.get(
                        "sourceMessageRef",
                    )
                    ?.let {
                        (it as JsonPrimitive).content
                    },
            )
            assertEquals(
                identity.responseMessageRef,
                response.semanticRelay
                    ?.get(
                        "sourceMessageRef",
                    )
                    ?.let {
                        (it as JsonPrimitive).content
                    },
            )

            val context =
                assertNotNull(
                    DirectTurnContextStore(
                        home,
                    )
                        .read(
                            first.turnRef,
                        ),
                )
            assertEquals(
                DirectTurnContextStore.ProviderRuntimeEvidenceSchemaV5,
                DirectTurnContextStore(
                    home,
                )
                    .inspectPromptReceiptCompatibility(
                        first.turnRef,
                    )
                    ?.runtimeEvidenceSchema,
            )
            assertEquals(
                first.providerObservation,
                context.providerObservation,
            )
            assertEquals(
                first.providerInvocationEvidence,
                context.providerInvocationEvidence,
            )
            assertNull(
                context.localRuntimeObservation,
            )
            assertNull(
                context.localInvocationEvidence,
            )

            val recovered =
                DirectCompanionTurn.performWithHooks(
                    setup = setup,
                    intent = intent,
                    hooks = hooks,
                )
            assertEquals(
                1,
                hostCalls,
            )
            assertTrue(
                recovered.recoveredCompletedTurn,
            )
            assertFalse(
                recovered.modelCallPerformed,
            )
            assertEquals(
                first.currentConversationHeadSha256,
                recovered.currentConversationHeadSha256,
            )
            assertEquals(
                first.providerObservation,
                recovered.providerObservation,
            )
            assertEquals(
                first.providerInvocationEvidence,
                recovered.providerInvocationEvidence,
            )
            assertNull(
                recovered.localRuntimeObservation,
            )
            assertNull(
                recovered.localInvocationEvidence,
            )
        } finally {
            home.parent
                .toFile()
                .deleteRecursively()
        }
    }
}

// [VXG RealForever]
