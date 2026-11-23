package vexlife.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import vexlife.partner.VexLifePartnerConnectionProjection
import vexlife.partner.VexLifePartnerModelProfileCatalog
import vexlife.partner.VexLifePartnerSupportQualification

class VexLifePartnerModelInventoryTest {
    private fun connection(
        providerRef: String,
        modelRef: String,
    ): VexLifePartnerConnectionProjection.Projection =
        VexLifePartnerConnectionProjection.project(
            VexLifePartnerConnectionProjection.Observation(
                providerRef = providerRef,
                modelRef = modelRef,
                credential =
                    VexLifePartnerConnectionProjection.CredentialStanding.MISSING,
                account =
                    VexLifePartnerConnectionProjection.AccountStanding.UNKNOWN,
                service =
                    VexLifePartnerConnectionProjection.ServiceStanding.UNKNOWN,
                modelAccess =
                    VexLifePartnerConnectionProjection.ModelAccessStanding.UNKNOWN,
                invocation =
                    VexLifePartnerConnectionProjection.InvocationStanding.NEVER_QUALIFIED,
            ),
        )

    private fun heldSupport(
        providerRef: String,
        modelRef: String,
    ): VexLifePartnerSupportQualification.Projection =
        VexLifePartnerSupportQualification.project(
            VexLifePartnerSupportQualification.Evidence(
                providerRef = providerRef,
                modelRef = modelRef,
                contractFixtureProofRef = null,
                adapterConformanceProofRef = null,
                vexRelationshipConformanceProofRef = null,
                liveQualificationReceiptRef = null,
            ),
        )

    private fun completeInput(
        targeted: VexLifePartnerModelInventory.Coordinate? = null,
        serving: VexLifePartnerModelInventory.Coordinate? = null,
    ): VexLifePartnerModelInventory.Input {
        val definitions =
            VexLifePartnerModelProfileCatalog.definitions()
        return VexLifePartnerModelInventory.Input(
            connections =
                definitions.map {
                    connection(
                        providerRef = it.providerRef,
                        modelRef = it.modelRef,
                    )
                },
            support =
                definitions.map {
                    heldSupport(
                        providerRef = it.providerRef,
                        modelRef = it.modelRef,
                    )
                },
            targeted = targeted,
            serving = serving,
        )
    }

    @Test
    fun inventoryComesOnlyFromSourceOwnedProfileCatalog() {
        val offerings =
            VexLifePartnerModelInventory.offerings(
                completeInput(),
            )

        assertEquals(
            VexLifePartnerModelProfileCatalog.definitions()
                .map {
                    it.modelRef
                },
            offerings.map {
                it.modelRef
            },
        )
        assertEquals(
            VexLifePartnerModelProfileCatalog.definitions()
                .map {
                    it.profileRef
                },
            offerings.map {
                it.handle
            },
        )
    }

    @Test
    fun providerVisibleUnknownModelCannotCreateInventoryRow() {
        val input =
            completeInput()
        val withUnknown =
            input.copy(
                connections =
                    input.connections +
                        connection(
                            providerRef = "provider.openai.api",
                            modelRef = "some-provider-visible-model",
                        ),
                support =
                    input.support +
                        heldSupport(
                            providerRef = "provider.openai.api",
                            modelRef = "some-provider-visible-model",
                        ),
            )

        assertTrue(
            VexLifePartnerModelInventory.offerings(
                withUnknown,
            )
                .none {
                    it.modelRef ==
                        "some-provider-visible-model"
                },
        )
    }

    @Test
    fun missingConnectionOrSupportTruthIsRejectedInsteadOfInvented() {
        val input =
            completeInput()

        assertFailsWith<IllegalArgumentException> {
            VexLifePartnerModelInventory.offerings(
                input.copy(
                    connections =
                        input.connections.dropLast(
                            1,
                        ),
                ),
            )
        }

        assertFailsWith<IllegalArgumentException> {
            VexLifePartnerModelInventory.offerings(
                input.copy(
                    support =
                        input.support.dropLast(
                            1,
                        ),
                ),
            )
        }
    }

    @Test
    fun targetAndServingAreExactProviderModelCoordinates() {
        val openAi =
            VexLifePartnerModelProfileCatalog.OpenAiGpt62Sol
        val kimi =
            VexLifePartnerModelProfileCatalog.KimiK3
        val offerings =
            VexLifePartnerModelInventory.offerings(
                completeInput(
                    targeted =
                        VexLifePartnerModelInventory.Coordinate(
                            providerRef = openAi.providerRef,
                            modelRef = openAi.modelRef,
                        ),
                    serving =
                        VexLifePartnerModelInventory.Coordinate(
                            providerRef = kimi.providerRef,
                            modelRef = kimi.modelRef,
                        ),
                ),
            )

        assertEquals(
            openAi.modelRef,
            offerings.single {
                it.targeted
            }.modelRef,
        )
        assertEquals(
            kimi.modelRef,
            offerings.single {
                it.serving
            }.modelRef,
        )
    }

    @Test
    fun unsupportedTargetOrServingCoordinateIsRejected() {
        assertFailsWith<IllegalArgumentException> {
            VexLifePartnerModelInventory.offerings(
                completeInput(
                    targeted =
                        VexLifePartnerModelInventory.Coordinate(
                            providerRef = "provider.openai.api",
                            modelRef = "unsupported-model",
                        ),
                ),
            )
        }
        assertFailsWith<IllegalArgumentException> {
            VexLifePartnerModelInventory.offerings(
                completeInput(
                    serving =
                        VexLifePartnerModelInventory.Coordinate(
                            providerRef = "provider.anthropic.api",
                            modelRef = "unsupported-model",
                        ),
                ),
            )
        }
    }

    @Test
    fun heldSupportStillProducesVisibleSourceSupportedRowWithoutSelectingIt() {
        val offering =
            VexLifePartnerModelInventory.offerings(
                completeInput(),
            )
                .first()

        assertEquals(
            VexLifePartnerSupportQualification.Standing.HELD,
            offering.support.standing,
        )
        assertFalse(
            offering.targeted,
        )
        assertFalse(
            offering.serving,
        )
    }

    @Test
    fun duplicatedConnectionOrSupportTruthIsRejected() {
        val input =
            completeInput()
        assertFailsWith<IllegalArgumentException> {
            VexLifePartnerModelInventory.offerings(
                input.copy(
                    connections =
                        input.connections +
                            input.connections.first(),
                ),
            )
        }
        assertFailsWith<IllegalArgumentException> {
            VexLifePartnerModelInventory.offerings(
                input.copy(
                    support =
                        input.support +
                            input.support.first(),
                ),
            )
        }
    }
}

// [VXG RealForever]
