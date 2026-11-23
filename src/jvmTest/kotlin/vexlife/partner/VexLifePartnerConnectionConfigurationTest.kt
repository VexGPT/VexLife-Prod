package vexlife.partner

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VexLifePartnerConnectionConfigurationTest {
    private class FakeSettings(
        initial: VexLifePartnerConnectionSettings.Value =
            VexLifePartnerConnectionSettings.Empty,
    ) : VexLifePartnerConnectionConfiguration.SettingsPort {
        var value: VexLifePartnerConnectionSettings.Value =
            initial
            private set
        var writes: Int =
            0
            private set
        val failOnWrites =
            mutableSetOf<Int>()

        override fun read(): VexLifePartnerConnectionSettings.Value =
            value

        override fun write(
            value: VexLifePartnerConnectionSettings.Value,
        ) {
            writes +=
                1
            if (
                writes in
                    failOnWrites
            ) {
                failOnWrites -=
                    writes
                error(
                    "synthetic settings write failure",
                )
            }
            this.value =
                value
        }
    }

    private class FakeHost :
        VexLifePartnerCredentialCustody.ProtectedHostStore {
        private val secrets =
            linkedMapOf<
                VexLifePartnerCredentialCustody.Coordinate,
                CharArray,
            >()
        val failDeleteOnce =
            mutableSetOf<VexLifePartnerCredentialCustody.Coordinate>()

        override fun store(
            coordinate: VexLifePartnerCredentialCustody.Coordinate,
            secret: CharArray,
        ) {
            require(
                coordinate !in
                    secrets,
            )
            secrets[
                coordinate
            ] =
                secret.copyOf()
        }

        override fun contains(
            coordinate: VexLifePartnerCredentialCustody.Coordinate,
        ): Boolean =
            coordinate in
                secrets

        override fun delete(
            coordinate: VexLifePartnerCredentialCustody.Coordinate,
        ): Boolean {
            if (
                coordinate in
                    failDeleteOnce
            ) {
                failDeleteOnce -=
                    coordinate
                return false
            }
            secrets
                .remove(
                    coordinate,
                )
                ?.fill(
                    '\u0000',
                )
            return coordinate !in
                secrets
        }
    }

    private fun coordinator(
        settings: FakeSettings,
        host: FakeHost,
    ): VexLifePartnerConnectionConfiguration.Coordinator {
        val refs =
            listOf(
                "credential.vexlife.partner.config-00000001",
                "credential.vexlife.partner.config-00000002",
                "credential.vexlife.partner.config-00000003",
                "credential.vexlife.partner.config-00000004",
            )
                .iterator()
        return VexLifePartnerConnectionConfiguration.Coordinator(
            settings = settings,
            custody =
                VexLifePartnerCredentialCustody.Custodian(
                    host = host,
                    nextCredentialRef = {
                        refs.next()
                    },
                ),
        )
    }

    private fun scope(
        providerRef: String,
    ): VexLifePartnerAccountScope.Selection =
        VexLifePartnerAccountScope.Selection(
            providerRef = providerRef,
        )

    @Test
    fun preallocationCollisionFailsBeforeDurableTransition() {
        val settings =
            FakeSettings()
        val host =
            FakeHost()
        val colliding =
            VexLifePartnerCredentialCustody.Coordinate(
                providerRef =
                    VexLifePartnerProviderCatalog.OpenAiRef,
                credentialRef =
                    "credential.vexlife.partner.config-00000001",
            )
        host.store(
            coordinate = colliding,
            secret =
                "existing-secret"
                    .toCharArray(),
        )
        val coordinator =
            coordinator(
                settings,
                host,
            )
        val incoming =
            "incoming-secret"
                .toCharArray()

        assertFailsWith<IllegalArgumentException> {
            coordinator.connect(
                providerRef =
                    colliding.providerRef,
                accountScope =
                    scope(
                        colliding.providerRef,
                    ),
                secret = incoming,
            )
        }

        assertTrue(
            settings.value.entries.isEmpty(),
        )
        assertTrue(
            settings.value.transitions.isEmpty(),
        )
        assertEquals(
            0,
            settings.writes,
        )
        assertTrue(
            incoming.all {
                it == '\u0000'
            },
        )
    }

    @Test
    fun configuredCurrentnessRefusesPendingTransitionUntilRecovery() {
        val staged =
            VexLifePartnerConnectionSettings.Transition(
                providerRef =
                    VexLifePartnerProviderCatalog.OpenAiRef,
                kind =
                    VexLifePartnerConnectionSettings.TransitionKind.STAGE_CONNECT,
                stagedCredentialRef =
                    "credential.vexlife.partner.config-00000001",
                stagedAccountScope =
                    scope(
                        VexLifePartnerProviderCatalog.OpenAiRef,
                    ),
            )
        val settings =
            VexLifePartnerConnectionSettings.form(
                entries = emptyList(),
                transitions =
                    listOf(
                        staged,
                    ),
            )

        assertFailsWith<IllegalArgumentException> {
            VexLifePartnerConfiguredCurrentness.observe(
                settings = settings,
                host = FakeHost(),
            )
        }
    }

    @Test
    fun connectPersistsTransitionBeforeSecretAndEndsCurrent() {
        val settings =
            FakeSettings()
        val host =
            FakeHost()
        val coordinator =
            coordinator(
                settings,
                host,
            )
        val secret =
            "connect-fixture-secret"
                .toCharArray()

        val result =
            coordinator.connect(
                providerRef =
                    VexLifePartnerProviderCatalog.OpenAiRef,
                accountScope =
                    scope(
                        VexLifePartnerProviderCatalog.OpenAiRef,
                    ),
                secret = secret,
            )

        assertEquals(
            VexLifePartnerConnectionConfiguration.ResultState.CONNECTED,
            result.state,
        )
        assertTrue(
            secret.all {
                it == '\u0000'
            },
        )
        assertTrue(
            settings.value.transitions.isEmpty(),
        )
        val entry =
            requireNotNull(
                VexLifePartnerConnectionSettings.entryFor(
                    settings.value,
                    VexLifePartnerProviderCatalog.OpenAiRef,
                ),
            )
        assertEquals(
            result.credentialRef,
            entry.credentialRef,
        )
        assertTrue(
            host.contains(
                VexLifePartnerCredentialCustody.Coordinate(
                    providerRef = entry.providerRef,
                    credentialRef = entry.credentialRef,
                ),
            ),
        )
    }

    @Test
    fun failedFinalConnectCommitRecoversWithoutSecretReplay() {
        val settings =
            FakeSettings()
        settings.failOnWrites +=
            2
        val host =
            FakeHost()
        val coordinator =
            coordinator(
                settings,
                host,
            )
        val secret =
            "connect-recovery-secret"
                .toCharArray()

        assertFailsWith<VexLifePartnerConnectionConfiguration.RecoveryRequiredException> {
            coordinator.connect(
                providerRef =
                    VexLifePartnerProviderCatalog.OpenAiRef,
                accountScope =
                    scope(
                        VexLifePartnerProviderCatalog.OpenAiRef,
                    ),
                secret = secret,
            )
        }

        val pending =
            requireNotNull(
                VexLifePartnerConnectionSettings.transitionFor(
                    settings.value,
                    VexLifePartnerProviderCatalog.OpenAiRef,
                ),
            )
        assertEquals(
            VexLifePartnerConnectionSettings.TransitionKind.STAGE_CONNECT,
            pending.kind,
        )
        assertTrue(
            host.contains(
                VexLifePartnerCredentialCustody.Coordinate(
                    providerRef = pending.providerRef,
                    credentialRef =
                        requireNotNull(
                            pending.stagedCredentialRef,
                        ),
                ),
            ),
        )

        val recovered =
            coordinator.recover()
        assertEquals(
            listOf(
                VexLifePartnerConnectionConfiguration.ResultState.RECOVERED_CONNECTED,
            ),
            recovered.map {
                it.state
            },
        )
        assertTrue(
            settings.value.transitions.isEmpty(),
        )
        assertEquals(
            pending.stagedCredentialRef,
            VexLifePartnerConnectionSettings.entryFor(
                settings.value,
                VexLifePartnerProviderCatalog.OpenAiRef,
            )?.credentialRef,
        )
    }

    @Test
    fun stagedConnectWithoutSecretRollsBackToUnconfigured() {
        val staged =
            VexLifePartnerConnectionSettings.Transition(
                providerRef =
                    VexLifePartnerProviderCatalog.KimiInternationalRef,
                kind =
                    VexLifePartnerConnectionSettings.TransitionKind.STAGE_CONNECT,
                stagedCredentialRef =
                    "credential.vexlife.partner.config-00000001",
                stagedAccountScope =
                    scope(
                        VexLifePartnerProviderCatalog.KimiInternationalRef,
                    ),
            )
        val settings =
            FakeSettings(
                VexLifePartnerConnectionSettings.form(
                    entries = emptyList(),
                    transitions =
                        listOf(
                            staged,
                        ),
                ),
            )
        val coordinator =
            coordinator(
                settings,
                FakeHost(),
            )

        val recovered =
            coordinator.recover()

        assertEquals(
            VexLifePartnerConnectionConfiguration.ResultState.RECOVERED_ROLLED_BACK,
            recovered.single().state,
        )
        assertTrue(
            settings.value.entries.isEmpty(),
        )
        assertTrue(
            settings.value.transitions.isEmpty(),
        )
    }

    @Test
    fun replacementCanRecoverAfterNewSecretStoredBeforeSwitchCommit() {
        val settings =
            FakeSettings()
        val host =
            FakeHost()
        val coordinator =
            coordinator(
                settings,
                host,
            )
        coordinator.connect(
            providerRef =
                VexLifePartnerProviderCatalog.AnthropicRef,
            accountScope =
                VexLifePartnerAccountScope.Selection(
                    providerRef =
                        VexLifePartnerProviderCatalog.AnthropicRef,
                    workspaceId =
                        "wrkspc_config_0001",
                ),
            secret =
                "old-secret"
                    .toCharArray(),
        )
        val old =
            requireNotNull(
                VexLifePartnerConnectionSettings.entryFor(
                    settings.value,
                    VexLifePartnerProviderCatalog.AnthropicRef,
                ),
            )
        settings.failOnWrites +=
            settings.writes +
                2

        assertFailsWith<VexLifePartnerConnectionConfiguration.RecoveryRequiredException> {
            coordinator.replace(
                providerRef =
                    VexLifePartnerProviderCatalog.AnthropicRef,
                accountScope =
                    VexLifePartnerAccountScope.Selection(
                        providerRef =
                            VexLifePartnerProviderCatalog.AnthropicRef,
                        workspaceId =
                            "wrkspc_config_0002",
                    ),
                replacement =
                    "new-secret"
                        .toCharArray(),
            )
        }

        val staged =
            requireNotNull(
                VexLifePartnerConnectionSettings.transitionFor(
                    settings.value,
                    VexLifePartnerProviderCatalog.AnthropicRef,
                ),
            )
        assertEquals(
            VexLifePartnerConnectionSettings.TransitionKind.STAGE_REPLACE,
            staged.kind,
        )
        assertEquals(
            old.credentialRef,
            VexLifePartnerConnectionSettings.entryFor(
                settings.value,
                old.providerRef,
            )?.credentialRef,
        )

        val recovered =
            coordinator.recover()
        assertEquals(
            VexLifePartnerConnectionConfiguration.ResultState.RECOVERED_REPLACED,
            recovered.single().state,
        )
        val current =
            requireNotNull(
                VexLifePartnerConnectionSettings.entryFor(
                    settings.value,
                    old.providerRef,
                ),
            )
        assertEquals(
            staged.stagedCredentialRef,
            current.credentialRef,
        )
        assertFalse(
            host.contains(
                VexLifePartnerCredentialCustody.Coordinate(
                    providerRef = old.providerRef,
                    credentialRef = old.credentialRef,
                ),
            ),
        )
        assertTrue(
            settings.value.transitions.isEmpty(),
        )
    }

    @Test
    fun failedOldCredentialCleanupRemainsDurableAndRecoveryFinishesIt() {
        val settings =
            FakeSettings()
        val host =
            FakeHost()
        val coordinator =
            coordinator(
                settings,
                host,
            )
        coordinator.connect(
            providerRef =
                VexLifePartnerProviderCatalog.OpenAiRef,
            accountScope =
                scope(
                    VexLifePartnerProviderCatalog.OpenAiRef,
                ),
            secret =
                "old-cleanup-secret"
                    .toCharArray(),
        )
        val old =
            requireNotNull(
                VexLifePartnerConnectionSettings.entryFor(
                    settings.value,
                    VexLifePartnerProviderCatalog.OpenAiRef,
                ),
            )
        val oldCoordinate =
            VexLifePartnerCredentialCustody.Coordinate(
                providerRef = old.providerRef,
                credentialRef = old.credentialRef,
            )
        host.failDeleteOnce +=
            oldCoordinate

        assertFailsWith<VexLifePartnerConnectionConfiguration.RecoveryRequiredException> {
            coordinator.replace(
                providerRef = old.providerRef,
                accountScope =
                    scope(
                        old.providerRef,
                    ),
                replacement =
                    "new-cleanup-secret"
                        .toCharArray(),
            )
        }

        assertEquals(
            VexLifePartnerConnectionSettings.TransitionKind.CLEANUP_REPLACED,
            VexLifePartnerConnectionSettings.transitionFor(
                settings.value,
                old.providerRef,
            )?.kind,
        )
        assertTrue(
            host.contains(
                oldCoordinate,
            ),
        )

        val recovered =
            coordinator.recover()
        assertEquals(
            VexLifePartnerConnectionConfiguration.ResultState.RECOVERED_CLEANUP,
            recovered.single().state,
        )
        assertFalse(
            host.contains(
                oldCoordinate,
            ),
        )
        assertTrue(
            settings.value.transitions.isEmpty(),
        )
    }

    @Test
    fun accountScopeUpdatePreservesCredentialAndDoesNotTouchProtectedHost() {
        val settings =
            FakeSettings()
        val host =
            FakeHost()
        val coordinator =
            coordinator(
                settings,
                host,
            )
        coordinator.connect(
            providerRef =
                VexLifePartnerProviderCatalog.AnthropicRef,
            accountScope =
                VexLifePartnerAccountScope.Selection(
                    providerRef =
                        VexLifePartnerProviderCatalog.AnthropicRef,
                    workspaceId =
                        "wrkspc_scope_0001",
                ),
            secret =
                "scope-secret"
                    .toCharArray(),
        )
        val before =
            requireNotNull(
                VexLifePartnerConnectionSettings.entryFor(
                    settings.value,
                    VexLifePartnerProviderCatalog.AnthropicRef,
                ),
            )

        val result =
            coordinator.updateAccountScope(
                providerRef = before.providerRef,
                accountScope =
                    VexLifePartnerAccountScope.Selection(
                        providerRef = before.providerRef,
                        workspaceId =
                            "wrkspc_scope_0002",
                    ),
            )

        val after =
            requireNotNull(
                VexLifePartnerConnectionSettings.entryFor(
                    settings.value,
                    before.providerRef,
                ),
            )
        assertEquals(
            VexLifePartnerConnectionConfiguration.ResultState.SCOPE_UPDATED,
            result.state,
        )
        assertEquals(
            before.credentialRef,
            after.credentialRef,
        )
        assertEquals(
            "wrkspc_scope_0002",
            after.accountScope.workspaceId,
        )
        assertTrue(
            host.contains(
                VexLifePartnerCredentialCustody.Coordinate(
                    providerRef = before.providerRef,
                    credentialRef = before.credentialRef,
                ),
            ),
        )
        assertTrue(
            settings.value.transitions.isEmpty(),
        )
    }

    @Test
    fun removalTransitionSurvivesFailedDeleteAndRecoveryCompletes() {
        val settings =
            FakeSettings()
        val host =
            FakeHost()
        val coordinator =
            coordinator(
                settings,
                host,
            )
        coordinator.connect(
            providerRef =
                VexLifePartnerProviderCatalog.KimiInternationalRef,
            accountScope =
                scope(
                    VexLifePartnerProviderCatalog.KimiInternationalRef,
                ),
            secret =
                "remove-secret"
                    .toCharArray(),
        )
        val current =
            requireNotNull(
                VexLifePartnerConnectionSettings.entryFor(
                    settings.value,
                    VexLifePartnerProviderCatalog.KimiInternationalRef,
                ),
            )
        val coordinate =
            VexLifePartnerCredentialCustody.Coordinate(
                providerRef = current.providerRef,
                credentialRef = current.credentialRef,
            )
        host.failDeleteOnce +=
            coordinate

        assertFailsWith<VexLifePartnerConnectionConfiguration.RecoveryRequiredException> {
            coordinator.remove(
                current.providerRef,
            )
        }

        assertEquals(
            VexLifePartnerConnectionSettings.TransitionKind.REMOVE,
            VexLifePartnerConnectionSettings.transitionFor(
                settings.value,
                current.providerRef,
            )?.kind,
        )
        assertTrue(
            host.contains(
                coordinate,
            ),
        )

        val recovered =
            coordinator.recover()
        assertEquals(
            VexLifePartnerConnectionConfiguration.ResultState.RECOVERED_REMOVED,
            recovered.single().state,
        )
        assertFalse(
            host.contains(
                coordinate,
            ),
        )
        assertEquals(
            null,
            VexLifePartnerConnectionSettings.entryFor(
                settings.value,
                current.providerRef,
            ),
        )
        assertTrue(
            settings.value.transitions.isEmpty(),
        )
    }
}

// [VXG RealForever]
