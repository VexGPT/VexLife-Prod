package vexlife.service

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import vexlife.model.VexLifeInferenceSelection
import vexlife.model.VexLifeInferenceSessionAdmission
import vexlife.model.VexLifeModelSourceProjection
import vexlife.model.VexLifeModels
import vexlife.partner.VexLifePartnerAccountScope
import vexlife.partner.VexLifePartnerConnectionSettings
import vexlife.partner.VexLifePartnerConnectionSettingsStore
import vexlife.partner.VexLifePartnerCredentialCustody
import vexlife.partner.VexLifePartnerHostEffect
import vexlife.partner.VexLifePartnerModelProfileCatalog
import vexlife.partner.VexLifePartnerNetworkPlan

class VexLifeProdPartnerInferenceTest {
    private class FakeHost(
        private var responseBody: String,
    ) : VexLifePartnerCredentialCustody.ProtectedHostStore,
        VexLifePartnerHostEffect.Executor {
        private val present =
            mutableSetOf<VexLifePartnerCredentialCustody.Coordinate>()

        var executeCalls =
            0
            private set

        fun seed(
            coordinate: VexLifePartnerCredentialCustody.Coordinate,
        ) {
            present +=
                coordinate
        }

        override fun store(
            coordinate: VexLifePartnerCredentialCustody.Coordinate,
            secret: CharArray,
        ) {
            present +=
                coordinate
        }

        override fun contains(
            coordinate: VexLifePartnerCredentialCustody.Coordinate,
        ): Boolean =
            coordinate in
                present

        override fun delete(
            coordinate: VexLifePartnerCredentialCustody.Coordinate,
        ): Boolean {
            present -=
                coordinate
            return true
        }

        override fun execute(
            plan: VexLifePartnerNetworkPlan.Plan,
        ): VexLifePartnerHostEffect.Result {
            executeCalls +=
                1
            return VexLifePartnerHostEffect.Result.Http(
                status = 200,
                body = responseBody,
                providerRequestId =
                    "request.partner-service-composition.test",
                receipt =
                    VexLifePartnerHostEffect.receiptForHttp(
                        plan = plan,
                        status = 200,
                        body = responseBody,
                    ),
            )
        }
    }

    private fun emptyLocal(): VexLifeModels.Projection =
        VexLifeModels.Projection(
            schemaVersion = VexLifeModels.Schema,
            models = emptyList(),
            selectedHandle = null,
            servingHandle = null,
            totalMemoryBytes = null,
            freeStorageBytes = null,
            downloading = null,
            switching = null,
            switchPending = false,
            lastOutcome = null,
            modelImport = null,
            effectsPerformed = false,
        )

    private fun openAiTextResponse(): String =
        """
        event: response.output_text.delta
        data: {"type":"response.output_text.delta","delta":"OK"}

        event: response.completed
        data: {"type":"response.completed","response":{"id":"resp_partner_service_test","status":"completed"}}

        """.trimIndent()

    @Test
    fun qualifiedPartnerServingProjectsExactAdmissionAndProductionRelayFactories() {
        val home =
            Files.createTempDirectory(
                "vexlife-partner-service-composition-",
            )
                .toAbsolutePath()
        try {
            val profile =
                VexLifePartnerModelProfileCatalog.OpenAiGpt62Sol
            val credentialRef =
                "credential.vexlife.partner.service-composition-0001"
            VexLifePartnerConnectionSettingsStore(
                home,
            )
                .write(
                    VexLifePartnerConnectionSettings.form(
                        listOf(
                            VexLifePartnerConnectionSettings.Entry(
                                providerRef = profile.providerRef,
                                credentialRef = credentialRef,
                                accountScope =
                                    VexLifePartnerAccountScope.Selection(
                                        providerRef = profile.providerRef,
                                    ),
                            ),
                        ),
                    ),
                )
            val host =
                FakeHost(
                    openAiTextResponse(),
                )
                    .also {
                        it.seed(
                            VexLifePartnerCredentialCustody.Coordinate(
                                providerRef = profile.providerRef,
                                credentialRef = credentialRef,
                            ),
                        )
                    }
            val composition =
                VexLifeProdPartnerInference(
                    productHome = home,
                    host = host,
                    executor = host,
                )
            val target =
                VexLifeInferenceSelection.Coordinate(
                    handle = profile.profileRef,
                    modelRef = profile.modelRef,
                    sourceKind =
                        VexLifeModelSourceProjection.SourceKind.PARTNER,
                    providerRef = profile.providerRef,
                )
            val qualified =
                composition.qualify(
                    target,
                )
            assertEquals(
                1,
                host.executeCalls,
            )
            val serving =
                VexLifeModels.PartnerServing(
                    coordinate = target,
                    qualification =
                        qualified as
                            vexlife.partner.VexLifePartnerLiveQualification.Outcome.Qualified,
                )
            val snapshot =
                composition.observe(
                    local = emptyLocal(),
                    stagedTarget = target,
                    partnerServing = serving,
                    localRuntimeEvidence = null,
                )

            assertEquals(
                VexLifeInferenceSessionAdmission.Standing.QUALIFIED,
                snapshot.admission.standing,
            )
            assertEquals(
                target,
                snapshot.admission.qualified
                    ?.coordinate,
            )
            val partnerTurn =
                composition.partnerTextTurn(
                    snapshot,
                )
            assertEquals(
                target,
                partnerTurn.qualified.coordinate,
            )
            assertTrue(
                partnerTurn.requestSemanticRelay
                    .form(
                        "message.self-development.request.partner-service-test",
                        "person.vexlife.test",
                        listOf(
                            "role.vex.companion",
                        ),
                    )
                    .isNotEmpty(),
            )
        } finally {
            home.toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun credentialRotationMakesPriorServingQualificationNonCurrent() {
        val home =
            Files.createTempDirectory(
                "vexlife-partner-service-currentness-",
            )
                .toAbsolutePath()
        try {
            val profile =
                VexLifePartnerModelProfileCatalog.OpenAiGpt62Sol
            val priorCredentialRef =
                "credential.vexlife.partner.service-prior-0001"
            val currentCredentialRef =
                "credential.vexlife.partner.service-current-0002"
            val host =
                FakeHost(
                    openAiTextResponse(),
                )
            host.seed(
                VexLifePartnerCredentialCustody.Coordinate(
                    providerRef = profile.providerRef,
                    credentialRef = priorCredentialRef,
                ),
            )
            val settings =
                VexLifePartnerConnectionSettingsStore(
                    home,
                )
            settings.write(
                VexLifePartnerConnectionSettings.form(
                    listOf(
                        VexLifePartnerConnectionSettings.Entry(
                            providerRef = profile.providerRef,
                            credentialRef = priorCredentialRef,
                            accountScope =
                                VexLifePartnerAccountScope.Selection(
                                    providerRef = profile.providerRef,
                                ),
                        ),
                    ),
                ),
            )
            val composition =
                VexLifeProdPartnerInference(
                    productHome = home,
                    host = host,
                    executor = host,
                )
            val target =
                VexLifeInferenceSelection.Coordinate(
                    handle = profile.profileRef,
                    modelRef = profile.modelRef,
                    sourceKind =
                        VexLifeModelSourceProjection.SourceKind.PARTNER,
                    providerRef = profile.providerRef,
                )
            val qualified =
                composition.qualify(
                    target,
                ) as
                    vexlife.partner.VexLifePartnerLiveQualification.Outcome.Qualified
            val serving =
                VexLifeModels.PartnerServing(
                    coordinate = target,
                    qualification = qualified,
                )

            settings.write(
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
            )
            host.seed(
                VexLifePartnerCredentialCustody.Coordinate(
                    providerRef = profile.providerRef,
                    credentialRef = currentCredentialRef,
                ),
            )

            val observed =
                composition.observe(
                    local = emptyLocal(),
                    stagedTarget = target,
                    partnerServing = serving,
                    localRuntimeEvidence = null,
                )
            assertEquals(
                VexLifeInferenceSessionAdmission.Standing.HELD,
                observed.admission.standing,
            )
            assertEquals(
                VexLifeInferenceSessionAdmission.HoldReason.PARTNER_CONNECTION_NOT_QUALIFIED,
                observed.admission.holdReason,
            )
            assertFailsWith<IllegalArgumentException> {
                composition.partnerTextTurn(
                    observed,
                )
            }
        } finally {
            home.toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun unavailableServiceRealizationCannotPerformPartnerQualification() {
        val home =
            Files.createTempDirectory(
                "vexlife-partner-service-unavailable-",
            )
                .toAbsolutePath()
        try {
            val profile =
                VexLifePartnerModelProfileCatalog.OpenAiGpt62Sol
            val composition =
                VexLifeProdPartnerInference.unavailable(
                    home,
                )
            val target =
                VexLifeInferenceSelection.Coordinate(
                    handle = profile.profileRef,
                    modelRef = profile.modelRef,
                    sourceKind =
                        VexLifeModelSourceProjection.SourceKind.PARTNER,
                    providerRef = profile.providerRef,
                )

            assertFailsWith<IllegalArgumentException> {
                composition.qualify(
                    target,
                )
            }
        } finally {
            home.toFile()
                .deleteRecursively()
        }
    }
}

// [VXG RealForever]
