package vexlife.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import vexlife.partner.VexLifePartnerConfiguredCurrentness
import vexlife.partner.VexLifePartnerConnectionProjection
import vexlife.partner.VexLifePartnerConnectionSettings
import vexlife.partner.VexLifePartnerCredentialCustody
import vexlife.partner.VexLifePartnerDeterministicSupport
import vexlife.partner.VexLifePartnerLiveQualification
import vexlife.partner.VexLifePartnerModelProfileCatalog
import vexlife.partner.VexLifePartnerProviderCatalog
import vexlife.partner.VexLifePartnerSupportQualification
import vexlife.partner.VexLifePartnerAccountScope

class VexLifePartnerModelsCurrentStateTest {
    private class FakeHost :
        VexLifePartnerCredentialCustody.ProtectedHostStore {
        private val present =
            mutableSetOf<VexLifePartnerCredentialCustody.Coordinate>()

        var storeCalls = 0
            private set
        var deleteCalls = 0
            private set
        var containsCalls = 0
            private set

        fun seed(
            coordinate: VexLifePartnerCredentialCustody.Coordinate,
        ) {
            present += coordinate
        }

        override fun store(
            coordinate: VexLifePartnerCredentialCustody.Coordinate,
            secret: CharArray,
        ) {
            storeCalls += 1
            present += coordinate
        }

        override fun contains(
            coordinate: VexLifePartnerCredentialCustody.Coordinate,
        ): Boolean {
            containsCalls += 1
            return coordinate in present
        }

        override fun delete(
            coordinate: VexLifePartnerCredentialCustody.Coordinate,
        ): Boolean {
            deleteCalls += 1
            present -= coordinate
            return true
        }
    }

    private fun heldSupport(): List<VexLifePartnerSupportQualification.Projection> =
        VexLifePartnerModelProfileCatalog
            .definitions()
            .map { profile ->
                VexLifePartnerSupportQualification.project(
                    VexLifePartnerSupportQualification.Evidence(
                        providerRef = profile.providerRef,
                        modelRef = profile.modelRef,
                        contractFixtureProofRef = null,
                        adapterConformanceProofRef = null,
                        vexRelationshipConformanceProofRef = null,
                        liveQualificationReceiptRef = null,
                    ),
                )
            }

    private fun qualified(
        profile: VexLifePartnerModelProfileCatalog.Definition,
        credentialRef: String,
    ): VexLifePartnerLiveQualification.Outcome.Qualified {
        val accountScope =
            VexLifePartnerAccountScope.Selection(
                providerRef = profile.providerRef,
            )
        val receipt =
            VexLifePartnerLiveQualification.Receipt(
                providerRef = profile.providerRef,
                modelRef = profile.modelRef,
                profileRef = profile.profileRef,
                credentialRef = credentialRef,
                accountScope = accountScope,
                requestBodySha256 = "a".repeat(64),
                responseBodySha256 = "b".repeat(64),
                providerRequestId = "request.partner-models-current-state.test",
                observedAt = "2026-11-24T23:30:00Z",
                receiptRef = "receipt.vexlife.partner.live.current-state-test",
            )
        return VexLifePartnerLiveQualification.Outcome.Qualified(
            receipt = receipt,
            connection =
                VexLifePartnerConnectionProjection.project(
                    VexLifePartnerConnectionProjection.Observation(
                        providerRef = profile.providerRef,
                        modelRef = profile.modelRef,
                        credential =
                            VexLifePartnerConnectionProjection.CredentialStanding.ACCEPTED,
                        account =
                            VexLifePartnerConnectionProjection.AccountStanding.USABLE,
                        service =
                            VexLifePartnerConnectionProjection.ServiceStanding.READY,
                        modelAccess =
                            VexLifePartnerConnectionProjection.ModelAccessStanding.AVAILABLE,
                        invocation =
                            VexLifePartnerConnectionProjection.InvocationStanding.QUALIFIED,
                    ),
                ),
            support =
                VexLifePartnerSupportQualification.acceptLiveQualification(
                    current =
                        VexLifePartnerDeterministicSupport.projection(
                            profile,
                        ),
                    live =
                        receipt.supportLiveQualification(),
                ),
        )
    }

    @Test
    fun emptyHomeFormsAllSourceSupportedRowsAsNeedingCredential() {
        val host = FakeHost()
        val projection =
            VexLifePartnerModelsCurrentState.project(
                input =
                    VexLifePartnerModelsCurrentState.Input(
                        settings = VexLifePartnerConnectionSettings.Empty,
                        support = heldSupport(),
                    ),
                host = host,
            )

        assertEquals(
            VexLifePartnerModelProfileCatalog.definitions()
                .map { it.modelRef },
            projection.offerings.map { it.modelRef },
        )
        projection.offerings.forEach { offering ->
            assertEquals(
                VexLifePartnerConnectionProjection.Standing.NEEDS_CREDENTIAL,
                offering.connection.standing,
            )
            assertEquals(
                VexLifePartnerSupportQualification.Standing.HELD,
                offering.support.standing,
            )
            assertFalse(offering.targeted)
            assertFalse(offering.serving)
        }
        assertEquals(0, host.containsCalls)
        assertEquals(0, host.storeCalls)
        assertEquals(0, host.deleteCalls)
    }

    @Test
    fun configuredPresentProviderBecomesCheckingWhileOthersNeedCredential() {
        val profile =
            VexLifePartnerModelProfileCatalog.OpenAiGpt62Sol
        val entry =
            VexLifePartnerConnectionSettings.Entry(
                providerRef = profile.providerRef,
                credentialRef =
                    "credential.vexlife.partner.openai-model-state-0001",
                accountScope =
                    VexLifePartnerAccountScope.Selection(
                        providerRef = profile.providerRef,
                    ),
            )
        val host = FakeHost()
        host.seed(
            VexLifePartnerCredentialCustody.Coordinate(
                providerRef = entry.providerRef,
                credentialRef = entry.credentialRef,
            ),
        )

        val projection =
            VexLifePartnerModelsCurrentState.project(
                input =
                    VexLifePartnerModelsCurrentState.Input(
                        settings =
                            VexLifePartnerConnectionSettings.form(
                                listOf(entry),
                            ),
                        support = heldSupport(),
                    ),
                host = host,
            )

        val openAi =
            projection.offerings.single {
                it.providerRef == profile.providerRef
            }
        assertEquals(
            VexLifePartnerConnectionProjection.Standing.CHECKING,
            openAi.connection.standing,
        )
        assertEquals(
            VexLifePartnerConnectionProjection.CredentialStanding.PRESENT_UNVERIFIED,
            openAi.connection.credential,
        )
        projection.offerings
            .filter {
                it.providerRef != profile.providerRef
            }
            .forEach {
                assertEquals(
                    VexLifePartnerConnectionProjection.Standing.NEEDS_CREDENTIAL,
                    it.connection.standing,
                )
            }
        assertEquals(1, host.containsCalls)
        assertEquals(0, host.storeCalls)
        assertEquals(0, host.deleteCalls)
    }

    @Test
    fun danglingCredentialRefRemainsDiagnosableAndModelNeedsCredential() {
        val profile =
            VexLifePartnerModelProfileCatalog.AnthropicClaudeSonnet55
        val entry =
            VexLifePartnerConnectionSettings.Entry(
                providerRef = profile.providerRef,
                credentialRef =
                    "credential.vexlife.partner.anthropic-model-state-0001",
                accountScope =
                    VexLifePartnerAccountScope.Selection(
                        providerRef = profile.providerRef,
                        workspaceId = "wrkspc_model_state_0001",
                    ),
            )
        val host = FakeHost()

        val projection =
            VexLifePartnerModelsCurrentState.project(
                input =
                    VexLifePartnerModelsCurrentState.Input(
                        settings =
                            VexLifePartnerConnectionSettings.form(
                                listOf(entry),
                            ),
                        support = heldSupport(),
                    ),
                host = host,
            )

        val providerState =
            projection.configuredProviders.single {
                it.providerRef == profile.providerRef
            }
        assertEquals(
            VexLifePartnerConfiguredCurrentness.CoordinateStanding.DANGLING_REF,
            providerState.coordinateStanding,
        )
        assertEquals(
            entry.credentialRef,
            providerState.credentialRef,
        )

        val offering =
            projection.offerings.single {
                it.providerRef == profile.providerRef
            }
        assertEquals(
            VexLifePartnerConnectionProjection.Standing.NEEDS_CREDENTIAL,
            offering.connection.standing,
        )
        assertEquals(0, host.storeCalls)
        assertEquals(0, host.deleteCalls)
    }

    @Test
    fun exactPartnerTargetAndServingCoordinatesPassThroughWithoutEffects() {
        val target =
            VexLifePartnerModelProfileCatalog.OpenAiGpt62Sol
        val serving =
            VexLifePartnerModelProfileCatalog.KimiK3
        val host = FakeHost()

        val projection =
            VexLifePartnerModelsCurrentState.project(
                input =
                    VexLifePartnerModelsCurrentState.Input(
                        settings = VexLifePartnerConnectionSettings.Empty,
                        support = heldSupport(),
                        targeted =
                            VexLifePartnerModelInventory.Coordinate(
                                providerRef = target.providerRef,
                                modelRef = target.modelRef,
                            ),
                        serving =
                            VexLifePartnerModelInventory.Coordinate(
                                providerRef = serving.providerRef,
                                modelRef = serving.modelRef,
                            ),
                    ),
                host = host,
            )

        assertEquals(
            target.modelRef,
            projection.offerings.single { it.targeted }.modelRef,
        )
        assertEquals(
            serving.modelRef,
            projection.offerings.single { it.serving }.modelRef,
        )
        assertEquals(0, host.storeCalls)
        assertEquals(0, host.deleteCalls)
    }

    @Test
    fun exactCurrentCredentialReprojectsProcessLiveQualificationForServingOnly() {
        val profile =
            VexLifePartnerModelProfileCatalog.OpenAiGpt62Sol
        val credentialRef =
            "credential.vexlife.partner.current-state-live-0001"
        val entry =
            VexLifePartnerConnectionSettings.Entry(
                providerRef = profile.providerRef,
                credentialRef = credentialRef,
                accountScope =
                    VexLifePartnerAccountScope.Selection(
                        providerRef = profile.providerRef,
                    ),
            )
        val host =
            FakeHost()
                .also {
                    it.seed(
                        VexLifePartnerCredentialCustody.Coordinate(
                            providerRef = profile.providerRef,
                            credentialRef = credentialRef,
                        ),
                    )
                }
        val projection =
            VexLifePartnerModelsCurrentState.project(
                input =
                    VexLifePartnerModelsCurrentState.Input(
                        settings =
                            VexLifePartnerConnectionSettings.form(
                                listOf(
                                    entry,
                                ),
                            ),
                        support =
                            VexLifePartnerDeterministicSupport.projections(),
                        serving =
                            VexLifePartnerModelInventory.Coordinate(
                                providerRef = profile.providerRef,
                                modelRef = profile.modelRef,
                            ),
                        liveQualifiedServing =
                            qualified(
                                profile = profile,
                                credentialRef = credentialRef,
                            ),
                    ),
                host = host,
            )

        val serving =
            projection.offerings.single {
                it.serving
            }
        assertEquals(
            VexLifePartnerConnectionProjection.Standing.QUALIFIED_SERVING,
            serving.connection.standing,
        )
        assertEquals(
            VexLifePartnerSupportQualification.Standing.LIVE_QUALIFIED,
            serving.support.standing,
        )
        assertEquals(
            credentialRef,
            serving.support.liveQualificationCredentialRef,
        )
    }

    @Test
    fun credentialRotationDropsPriorLiveQualificationBackToUnqualifiedCurrentness() {
        val profile =
            VexLifePartnerModelProfileCatalog.OpenAiGpt62Sol
        val priorCredentialRef =
            "credential.vexlife.partner.current-state-prior-0001"
        val currentCredentialRef =
            "credential.vexlife.partner.current-state-current-0002"
        val host =
            FakeHost()
                .also {
                    it.seed(
                        VexLifePartnerCredentialCustody.Coordinate(
                            providerRef = profile.providerRef,
                            credentialRef = currentCredentialRef,
                        ),
                    )
                }
        val projection =
            VexLifePartnerModelsCurrentState.project(
                input =
                    VexLifePartnerModelsCurrentState.Input(
                        settings =
                            VexLifePartnerConnectionSettings.form(
                                listOf(
                                    VexLifePartnerConnectionSettings.Entry(
                                        providerRef = profile.providerRef,
                                        credentialRef = currentCredentialRef,
                                        accountScope =
                                            VexLifePartnerAccountScope.Selection(
                                                providerRef = profile.providerRef,
                                            ),
                                    ),
                                ),
                            ),
                        support =
                            VexLifePartnerDeterministicSupport.projections(),
                        serving =
                            VexLifePartnerModelInventory.Coordinate(
                                providerRef = profile.providerRef,
                                modelRef = profile.modelRef,
                            ),
                        liveQualifiedServing =
                            qualified(
                                profile = profile,
                                credentialRef = priorCredentialRef,
                            ),
                    ),
                host = host,
            )

        val serving =
            projection.offerings.single {
                it.serving
            }
        assertEquals(
            VexLifePartnerConnectionProjection.Standing.CHECKING,
            serving.connection.standing,
        )
        assertEquals(
            VexLifePartnerSupportQualification.Standing.CONTRACT_PROVEN_LIVE_UNQUALIFIED,
            serving.support.standing,
        )
    }

    @Test
    fun liveQualificationCannotBeProjectedForAnotherServingCoordinate() {
        val liveProfile =
            VexLifePartnerModelProfileCatalog.OpenAiGpt62Sol
        val servingProfile =
            VexLifePartnerModelProfileCatalog.KimiK3

        assertFailsWith<IllegalArgumentException> {
            VexLifePartnerModelsCurrentState.project(
                input =
                    VexLifePartnerModelsCurrentState.Input(
                        settings =
                            VexLifePartnerConnectionSettings.Empty,
                        support =
                            VexLifePartnerDeterministicSupport.projections(),
                        serving =
                            VexLifePartnerModelInventory.Coordinate(
                                providerRef = servingProfile.providerRef,
                                modelRef = servingProfile.modelRef,
                            ),
                        liveQualifiedServing =
                            qualified(
                                profile = liveProfile,
                                credentialRef =
                                    "credential.vexlife.partner.current-state-mismatch-0001",
                            ),
                    ),
                host = FakeHost(),
            )
        }
    }

    @Test
    fun connectionMapIsKeyedBySourceOwnedProfileIdentity() {
        val projection =
            VexLifePartnerModelsCurrentState.project(
                input =
                    VexLifePartnerModelsCurrentState.Input(
                        settings = VexLifePartnerConnectionSettings.Empty,
                        support = heldSupport(),
                    ),
                host = FakeHost(),
            )

        assertEquals(
            VexLifePartnerModelProfileCatalog.definitions()
                .map { it.profileRef }
                .toSet(),
            projection.connectionByProfileRef.keys,
        )
        assertTrue(
            projection.connectionByProfileRef.values.all {
                it.account ==
                    VexLifePartnerConnectionProjection.AccountStanding.UNKNOWN &&
                    it.service ==
                    VexLifePartnerConnectionProjection.ServiceStanding.UNKNOWN &&
                    it.modelAccess ==
                    VexLifePartnerConnectionProjection.ModelAccessStanding.UNKNOWN
            },
        )
    }
}

// [VXG RealForever]
