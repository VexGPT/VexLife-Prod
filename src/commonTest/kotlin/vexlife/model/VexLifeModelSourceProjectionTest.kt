package vexlife.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import vexlife.partner.VexLifePartnerAccountScope
import vexlife.partner.VexLifePartnerConnectionProjection
import vexlife.partner.VexLifePartnerSupportQualification

class VexLifeModelSourceProjectionTest {
    private fun connection(
        providerRef: String,
        modelRef: String,
        standing: VexLifePartnerConnectionProjection.Standing =
            VexLifePartnerConnectionProjection.Standing.READY_TO_ATTEMPT,
    ): VexLifePartnerConnectionProjection.Projection {
        val observation =
            when (
                standing
            ) {
                VexLifePartnerConnectionProjection.Standing.NEEDS_CREDENTIAL ->
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
                    )

                else ->
                    VexLifePartnerConnectionProjection.Observation(
                        providerRef = providerRef,
                        modelRef = modelRef,
                        credential =
                            VexLifePartnerConnectionProjection.CredentialStanding.ACCEPTED,
                        account =
                            VexLifePartnerConnectionProjection.AccountStanding.USABLE,
                        service =
                            VexLifePartnerConnectionProjection.ServiceStanding.READY,
                        modelAccess =
                            VexLifePartnerConnectionProjection.ModelAccessStanding.AVAILABLE,
                        invocation =
                            VexLifePartnerConnectionProjection.InvocationStanding.NEVER_QUALIFIED,
                    )
            }

        val projection =
            VexLifePartnerConnectionProjection.project(
                observation,
            )
        require(
            standing ==
                projection.standing ||
                standing ==
                VexLifePartnerConnectionProjection.Standing.READY_TO_ATTEMPT,
        )
        return projection
    }

    private fun support(
        providerRef: String,
        modelRef: String,
        live: Boolean = false,
    ): VexLifePartnerSupportQualification.Projection =
        VexLifePartnerSupportQualification.project(
            VexLifePartnerSupportQualification.Evidence(
                providerRef = providerRef,
                modelRef = modelRef,
                contractFixtureProofRef =
                    "proof.partner.contract.fixture",
                adapterConformanceProofRef =
                    "proof.partner.adapter.fixture",
                vexRelationshipConformanceProofRef =
                    "proof.partner.vex.fixture",
                liveQualificationReceiptRef =
                    "receipt.partner.live.fixture".takeIf {
                        live
                    },
                liveQualificationCredentialRef =
                    "credential.partner.live.fixture".takeIf {
                        live
                    },
                liveQualificationAccountScope =
                    VexLifePartnerAccountScope.Selection(
                        providerRef = providerRef,
                    )
                        .takeIf {
                            live
                        },
            ),
        )

    @Test
    fun localAndPartnerArePresentationFiltersOverOneRowList() {
        val projection =
            VexLifeModelSourceProjection.project(
                local =
                    listOf(
                        VexLifeModelSourceProjection.LocalOffering(
                            handle = "heretic-4b",
                            modelRef = "model.heretic.4b",
                            targeted = true,
                            serving = true,
                        ),
                    ),
                partner =
                    listOf(
                        VexLifeModelSourceProjection.PartnerOffering(
                            handle = "openai-fixture",
                            providerRef = "provider.openai.api",
                            modelRef = "model.openai.fixture",
                            targeted = false,
                            serving = false,
                            connection =
                                connection(
                                    providerRef = "provider.openai.api",
                                    modelRef = "model.openai.fixture",
                                ),
                            support =
                                support(
                                    providerRef = "provider.openai.api",
                                    modelRef = "model.openai.fixture",
                                ),
                        ),
                    ),
            )

        assertEquals(
            listOf(
                VexLifeModelSourceProjection.SourceKind.LOCAL,
                VexLifeModelSourceProjection.SourceKind.PARTNER,
            ),
            projection.availableSources,
        )
        assertEquals(
            listOf(
                "heretic-4b",
            ),
            projection
                .rowsFor(
                    VexLifeModelSourceProjection.SourceKind.LOCAL,
                )
                .map {
                    it.handle
                },
        )
        assertEquals(
            listOf(
                "openai-fixture",
            ),
            projection
                .rowsFor(
                    VexLifeModelSourceProjection.SourceKind.PARTNER,
                )
                .map {
                    it.handle
                },
        )
    }

    @Test
    fun targetedAndServingSourceAreDerivedFromExactModelRows() {
        val projection =
            VexLifeModelSourceProjection.project(
                local =
                    listOf(
                        VexLifeModelSourceProjection.LocalOffering(
                            handle = "heretic-4b",
                            modelRef = "model.heretic.4b",
                            targeted = false,
                            serving = true,
                        ),
                    ),
                partner =
                    listOf(
                        VexLifeModelSourceProjection.PartnerOffering(
                            handle = "anthropic-fixture",
                            providerRef = "provider.anthropic.api",
                            modelRef = "model.anthropic.fixture",
                            targeted = true,
                            serving = false,
                            connection =
                                connection(
                                    providerRef = "provider.anthropic.api",
                                    modelRef = "model.anthropic.fixture",
                                ),
                            support =
                                support(
                                    providerRef = "provider.anthropic.api",
                                    modelRef = "model.anthropic.fixture",
                                ),
                        ),
                    ),
            )

        assertEquals(
            VexLifeModelSourceProjection.SourceKind.PARTNER,
            projection.targetedSource,
        )
        assertEquals(
            VexLifeModelSourceProjection.SourceKind.LOCAL,
            projection.servingSource,
        )
    }

    @Test
    fun partnerDisclosureStandingPassesThroughWithoutBecomingModelChoice() {
        val projection =
            VexLifeModelSourceProjection.project(
                local =
                    emptyList(),
                partner =
                    listOf(
                        VexLifeModelSourceProjection.PartnerOffering(
                            handle = "kimi-fixture",
                            providerRef = "provider.kimi.open-platform.international",
                            modelRef = "model.kimi.fixture",
                            targeted = false,
                            serving = false,
                            connection =
                                connection(
                                    providerRef = "provider.kimi.open-platform.international",
                                    modelRef = "model.kimi.fixture",
                                ),
                            support =
                                support(
                                    providerRef = "provider.kimi.open-platform.international",
                                    modelRef = "model.kimi.fixture",
                                    live = false,
                                ),
                        ),
                    ),
            )

        val row =
            projection.rows.single()
        assertEquals(
            VexLifePartnerSupportQualification.Standing.CONTRACT_PROVEN_LIVE_UNQUALIFIED,
            row.partnerSupportStanding,
        )
        assertTrue(
            row.liveValidationDisclosureRequired,
        )
        assertFalse(
            row.targeted,
        )
        assertFalse(
            row.serving,
        )
    }

    @Test
    fun liveQualificationRemovesDisclosureWithoutChangingSourceIdentity() {
        val providerRef =
            "provider.openai.api"
        val modelRef =
            "model.openai.fixture"
        val projection =
            VexLifeModelSourceProjection.project(
                local =
                    emptyList(),
                partner =
                    listOf(
                        VexLifeModelSourceProjection.PartnerOffering(
                            handle = "openai-fixture",
                            providerRef = providerRef,
                            modelRef = modelRef,
                            targeted = true,
                            serving = true,
                            connection =
                                connection(
                                    providerRef = providerRef,
                                    modelRef = modelRef,
                                ),
                            support =
                                support(
                                    providerRef = providerRef,
                                    modelRef = modelRef,
                                    live = true,
                                ),
                        ),
                    ),
            )

        val row =
            projection.rows.single()
        assertEquals(
            VexLifeModelSourceProjection.SourceKind.PARTNER,
            row.sourceKind,
        )
        assertEquals(
            providerRef,
            row.providerRef,
        )
        assertEquals(
            VexLifePartnerSupportQualification.Standing.LIVE_QUALIFIED,
            row.partnerSupportStanding,
        )
        assertFalse(
            row.liveValidationDisclosureRequired,
        )
    }

    @Test
    fun partnerRowRejectsConnectionOrSupportForAnotherModel() {
        assertFailsWith<IllegalArgumentException> {
            VexLifeModelSourceProjection.PartnerOffering(
                handle = "openai-fixture",
                providerRef = "provider.openai.api",
                modelRef = "model.openai.fixture",
                targeted = false,
                serving = false,
                connection =
                    connection(
                        providerRef = "provider.openai.api",
                        modelRef = "model.openai.other",
                    ),
                support =
                    support(
                        providerRef = "provider.openai.api",
                        modelRef = "model.openai.fixture",
                    ),
            )
        }

        assertFailsWith<IllegalArgumentException> {
            VexLifeModelSourceProjection.PartnerOffering(
                handle = "openai-fixture",
                providerRef = "provider.openai.api",
                modelRef = "model.openai.fixture",
                targeted = false,
                serving = false,
                connection =
                    connection(
                        providerRef = "provider.openai.api",
                        modelRef = "model.openai.fixture",
                    ),
                support =
                    support(
                        providerRef = "provider.anthropic.api",
                        modelRef = "model.openai.fixture",
                    ),
            )
        }
    }

    @Test
    fun multipleTargetedOrServingRowsAreRejectedInsteadOfInventingSourceChoice() {
        assertFailsWith<IllegalArgumentException> {
            VexLifeModelSourceProjection.project(
                local =
                    listOf(
                        VexLifeModelSourceProjection.LocalOffering(
                            handle = "local-a",
                            modelRef = "model.local.a",
                            targeted = true,
                            serving = false,
                        ),
                        VexLifeModelSourceProjection.LocalOffering(
                            handle = "local-b",
                            modelRef = "model.local.b",
                            targeted = true,
                            serving = false,
                        ),
                    ),
                partner =
                    emptyList(),
            )
        }

        assertFailsWith<IllegalArgumentException> {
            VexLifeModelSourceProjection.project(
                local =
                    listOf(
                        VexLifeModelSourceProjection.LocalOffering(
                            handle = "local-a",
                            modelRef = "model.local.a",
                            targeted = false,
                            serving = true,
                        ),
                    ),
                partner =
                    listOf(
                        VexLifeModelSourceProjection.PartnerOffering(
                            handle = "partner-a",
                            providerRef = "provider.openai.api",
                            modelRef = "model.partner.a",
                            targeted = false,
                            serving = true,
                            connection =
                                connection(
                                    providerRef = "provider.openai.api",
                                    modelRef = "model.partner.a",
                                ),
                            support =
                                support(
                                    providerRef = "provider.openai.api",
                                    modelRef = "model.partner.a",
                                ),
                        ),
                    ),
            )
        }
    }

    @Test
    fun handlesMustRemainUniqueAcrossLocalAndPartnerRows() {
        assertFailsWith<IllegalArgumentException> {
            VexLifeModelSourceProjection.project(
                local =
                    listOf(
                        VexLifeModelSourceProjection.LocalOffering(
                            handle = "same-handle",
                            modelRef = "model.local",
                            targeted = false,
                            serving = false,
                        ),
                    ),
                partner =
                    listOf(
                        VexLifeModelSourceProjection.PartnerOffering(
                            handle = "same-handle",
                            providerRef = "provider.openai.api",
                            modelRef = "model.partner",
                            targeted = false,
                            serving = false,
                            connection =
                                connection(
                                    providerRef = "provider.openai.api",
                                    modelRef = "model.partner",
                                ),
                            support =
                                support(
                                    providerRef = "provider.openai.api",
                                    modelRef = "model.partner",
                                ),
                        ),
                    ),
            )
        }
    }
}

// [VXG RealForever]
