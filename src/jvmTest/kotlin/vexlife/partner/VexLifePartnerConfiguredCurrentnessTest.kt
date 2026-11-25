package vexlife.partner

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VexLifePartnerConfiguredCurrentnessTest {
    private class FakeProtectedHost :
        VexLifePartnerCredentialCustody.ProtectedHostStore {
        private val present =
            mutableSetOf<VexLifePartnerCredentialCustody.Coordinate>()

        var storeCalls: Int =
            0
            private set
        var deleteCalls: Int =
            0
            private set
        var containsCalls: Int =
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
            storeCalls +=
                1
            present +=
                coordinate
        }

        override fun contains(
            coordinate: VexLifePartnerCredentialCustody.Coordinate,
        ): Boolean {
            containsCalls +=
                1
            return coordinate in
                present
        }

        override fun delete(
            coordinate: VexLifePartnerCredentialCustody.Coordinate,
        ): Boolean {
            deleteCalls +=
                1
            present -=
                coordinate
            return true
        }
    }

    private fun entry(
        providerRef: String,
        credentialRef: String,
        workspaceId: String? = null,
    ): VexLifePartnerConnectionSettings.Entry =
        VexLifePartnerConnectionSettings.Entry(
            providerRef = providerRef,
            credentialRef = credentialRef,
            accountScope =
                VexLifePartnerAccountScope.Selection(
                    providerRef = providerRef,
                    workspaceId = workspaceId,
                ),
        )

    @Test
    fun absentSettingsProjectEveryBuiltInProviderAsNotConfigured() {
        val host =
            FakeProtectedHost()
        val states =
            VexLifePartnerConfiguredCurrentness.observe(
                settings =
                    VexLifePartnerConnectionSettings.Empty,
                host = host,
            )

        assertEquals(
            VexLifePartnerProviderCatalog.definitions()
                .map {
                    it.providerRef
                },
            states.map {
                it.providerRef
            },
        )
        states.forEach {
            assertEquals(
                VexLifePartnerConfiguredCurrentness.CoordinateStanding.NOT_CONFIGURED,
                it.coordinateStanding,
            )
            assertEquals(
                VexLifePartnerConnectionProjection.CredentialStanding.MISSING,
                it.credentialStanding,
            )
            assertNull(
                it.credentialRef,
            )
        }
        assertEquals(
            0,
            host.containsCalls,
        )
        assertEquals(
            0,
            host.storeCalls,
        )
        assertEquals(
            0,
            host.deleteCalls,
        )
    }

    @Test
    fun configuredRefWithProtectedSecretIsPresentButUnverified() {
        val configured =
            entry(
                providerRef =
                    VexLifePartnerProviderCatalog.AnthropicRef,
                credentialRef =
                    "credential.vexlife.partner.anthropic-currentness-0001",
                workspaceId =
                    "wrkspc_currentness_0001",
            )
        val host =
            FakeProtectedHost()
        host.seed(
            VexLifePartnerCredentialCustody.Coordinate(
                providerRef = configured.providerRef,
                credentialRef = configured.credentialRef,
            ),
        )

        val state =
            VexLifePartnerConfiguredCurrentness.observe(
                settings =
                    VexLifePartnerConnectionSettings.form(
                        listOf(
                            configured,
                        ),
                    ),
                host = host,
            )
                .single {
                    it.providerRef ==
                        configured.providerRef
                }

        assertEquals(
            VexLifePartnerConfiguredCurrentness.CoordinateStanding.PRESENT,
            state.coordinateStanding,
        )
        assertEquals(
            VexLifePartnerConnectionProjection.CredentialStanding.PRESENT_UNVERIFIED,
            state.credentialStanding,
        )
        assertEquals(
            configured.credentialRef,
            state.credentialRef,
        )
        assertEquals(
            "wrkspc_currentness_0001",
            state.accountScope?.workspaceId,
        )
        assertEquals(
            1,
            host.containsCalls,
        )
        assertEquals(
            0,
            host.storeCalls,
        )
        assertEquals(
            0,
            host.deleteCalls,
        )
    }

    @Test
    fun configuredRefWithMissingProtectedSecretStaysVisibleAsDangling() {
        val configured =
            entry(
                providerRef =
                    VexLifePartnerProviderCatalog.OpenAiRef,
                credentialRef =
                    "credential.vexlife.partner.openai-currentness-0001",
            )
        val host =
            FakeProtectedHost()

        val state =
            VexLifePartnerConfiguredCurrentness.observe(
                settings =
                    VexLifePartnerConnectionSettings.form(
                        listOf(
                            configured,
                        ),
                    ),
                host = host,
            )
                .single {
                    it.providerRef ==
                        configured.providerRef
                }

        assertEquals(
            VexLifePartnerConfiguredCurrentness.CoordinateStanding.DANGLING_REF,
            state.coordinateStanding,
        )
        assertEquals(
            VexLifePartnerConnectionProjection.CredentialStanding.MISSING,
            state.credentialStanding,
        )
        assertEquals(
            configured.credentialRef,
            state.credentialRef,
        )
        assertFalse(
            host.deleteCalls >
                0,
        )
    }

    @Test
    fun initialConnectionDoesNotReplayHistoricalAccountOrModelCurrentness() {
        val provider =
            VexLifePartnerConfiguredCurrentness.ProviderState(
                providerRef =
                    VexLifePartnerProviderCatalog.KimiInternationalRef,
                credentialRef =
                    "credential.vexlife.partner.kimi-currentness-0001",
                accountScope =
                    VexLifePartnerAccountScope.Selection(
                        providerRef =
                            VexLifePartnerProviderCatalog.KimiInternationalRef,
                    ),
                coordinateStanding =
                    VexLifePartnerConfiguredCurrentness.CoordinateStanding.PRESENT,
                credentialStanding =
                    VexLifePartnerConnectionProjection.CredentialStanding.PRESENT_UNVERIFIED,
            )

        val connection =
            VexLifePartnerConfiguredCurrentness.initialConnection(
                provider = provider,
                modelRef = "kimi-k3",
            )

        assertEquals(
            VexLifePartnerConnectionProjection.CredentialStanding.PRESENT_UNVERIFIED,
            connection.credential,
        )
        assertEquals(
            VexLifePartnerConnectionProjection.AccountStanding.UNKNOWN,
            connection.account,
        )
        assertEquals(
            VexLifePartnerConnectionProjection.ServiceStanding.UNKNOWN,
            connection.service,
        )
        assertEquals(
            VexLifePartnerConnectionProjection.ModelAccessStanding.UNKNOWN,
            connection.modelAccess,
        )
        assertEquals(
            VexLifePartnerConnectionProjection.InvocationStanding.NEVER_QUALIFIED,
            connection.invocation,
        )
        assertEquals(
            VexLifePartnerConnectionProjection.Standing.CHECKING,
            connection.standing,
        )
    }

    @Test
    fun missingOrDanglingCredentialProjectsNeedsCredentialForModel() {
        listOf(
            VexLifePartnerConfiguredCurrentness.ProviderState(
                providerRef =
                    VexLifePartnerProviderCatalog.OpenAiRef,
                credentialRef = null,
                accountScope = null,
                coordinateStanding =
                    VexLifePartnerConfiguredCurrentness.CoordinateStanding.NOT_CONFIGURED,
                credentialStanding =
                    VexLifePartnerConnectionProjection.CredentialStanding.MISSING,
            ),
            VexLifePartnerConfiguredCurrentness.ProviderState(
                providerRef =
                    VexLifePartnerProviderCatalog.OpenAiRef,
                credentialRef =
                    "credential.vexlife.partner.openai-currentness-0001",
                accountScope =
                    VexLifePartnerAccountScope.Selection(
                        providerRef =
                            VexLifePartnerProviderCatalog.OpenAiRef,
                    ),
                coordinateStanding =
                    VexLifePartnerConfiguredCurrentness.CoordinateStanding.DANGLING_REF,
                credentialStanding =
                    VexLifePartnerConnectionProjection.CredentialStanding.MISSING,
            ),
        ).forEach { provider ->
            val connection =
                VexLifePartnerConfiguredCurrentness.initialConnection(
                    provider = provider,
                    modelRef = "gpt-6.2-sol",
                )
            assertEquals(
                VexLifePartnerConnectionProjection.Standing.NEEDS_CREDENTIAL,
                connection.standing,
            )
        }
    }

    @Test
    fun observationNeverRepairsOrMutatesHostCustody() {
        val configured =
            entry(
                providerRef =
                    VexLifePartnerProviderCatalog.KimiInternationalRef,
                credentialRef =
                    "credential.vexlife.partner.kimi-currentness-0002",
            )
        val host =
            FakeProtectedHost()

        VexLifePartnerConfiguredCurrentness.observe(
            settings =
                VexLifePartnerConnectionSettings.form(
                    listOf(
                        configured,
                    ),
                ),
            host = host,
        )

        assertTrue(
            host.containsCalls >
                0,
        )
        assertEquals(
            0,
            host.storeCalls,
        )
        assertEquals(
            0,
            host.deleteCalls,
        )
    }
}

// [VXG RealForever]
