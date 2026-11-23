package vexlife.partner

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VexLifePartnerCredentialCustodyTest {
    private class FakeProtectedHostStore :
        VexLifePartnerCredentialCustody.ProtectedHostStore {
        private val values =
            linkedMapOf<
                VexLifePartnerCredentialCustody.Coordinate,
                CharArray,
            >()

        var resolutionCallbacks: Int =
            0
            private set

        val deleteFailures =
            mutableSetOf<VexLifePartnerCredentialCustody.Coordinate>()

        override fun store(
            coordinate: VexLifePartnerCredentialCustody.Coordinate,
            secret: CharArray,
        ) {
            require(
                coordinate !in values,
            )
            values[
                coordinate
            ] =
                secret.copyOf()
        }

        override fun contains(
            coordinate: VexLifePartnerCredentialCustody.Coordinate,
        ): Boolean =
            coordinate in
                values

        override fun <T> withSecret(
            coordinate: VexLifePartnerCredentialCustody.Coordinate,
            use: (CharArray) -> T,
        ): T {
            val value =
                requireNotNull(
                    values[
                        coordinate
                    ],
                )
            resolutionCallbacks +=
                1
            return use(
                value.copyOf(),
            )
        }

        override fun delete(
            coordinate: VexLifePartnerCredentialCustody.Coordinate,
        ): Boolean {
            if (
                coordinate in
                    deleteFailures
            ) {
                return false
            }
            values
                .remove(
                    coordinate,
                )
                ?.fill(
                    '\u0000',
                )
            return coordinate !in
                values
        }
    }

    private fun custodian(
        host: FakeProtectedHostStore,
        refs: Iterator<String> =
            listOf(
                "credential.vexlife.partner.fixture-0001",
                "credential.vexlife.partner.fixture-0002",
                "credential.vexlife.partner.fixture-0003",
            )
                .iterator(),
    ): VexLifePartnerCredentialCustody.Custodian =
        VexLifePartnerCredentialCustody.Custodian(
            host = host,
            nextCredentialRef = {
                refs.next()
            },
        )

    @Test
    fun storeReturnsOnlyOpaqueRefAndPresentStanding() {
        val host =
            FakeProtectedHostStore()
        val secret =
            "synthetic-secret-value"
                .toCharArray()
        val receipt =
            custodian(
                host,
            )
                .store(
                    providerRef =
                        VexLifePartnerProviderCatalog.OpenAiRef,
                    secret = secret,
                )

        assertEquals(
            VexLifePartnerCredentialCustody.Operation.STORED,
            receipt.operation,
        )
        assertEquals(
            "credential.vexlife.partner.fixture-0001",
            receipt.coordinate.credentialRef,
        )
        assertFalse(
            receipt.secretProjected,
        )
        assertFalse(
            receipt.toString()
                .contains(
                    "synthetic-secret-value",
                ),
        )
        assertTrue(
            host.contains(
                receipt.coordinate,
            ),
        )
    }

    @Test
    fun exactProviderBindingMustMatchBeforeResolutionCallbackRuns() {
        val host =
            FakeProtectedHostStore()
        val custody =
            custodian(
                host,
            )
        val receipt =
            custody.store(
                providerRef =
                    VexLifePartnerProviderCatalog.OpenAiRef,
                secret =
                    "openai-fixture-secret"
                        .toCharArray(),
            )

        assertFailsWith<IllegalArgumentException> {
            custody.withCredential(
                providerRef =
                    VexLifePartnerProviderCatalog.AnthropicRef,
                credentialRef =
                    receipt.coordinate.credentialRef,
            ) {
                error(
                    "wrong provider must not receive secret",
                )
            }
        }
        assertEquals(
            0,
            host.resolutionCallbacks,
        )
    }

    @Test
    fun executionReceivesEphemeralCopyThatIsZeroedAfterReturn() {
        val host =
            FakeProtectedHostStore()
        val custody =
            custodian(
                host,
            )
        val receipt =
            custody.store(
                providerRef =
                    VexLifePartnerProviderCatalog.KimiInternationalRef,
                secret =
                    "kimi-fixture-secret"
                        .toCharArray(),
            )
        lateinit var observed: CharArray

        val length =
            custody.withCredential(
                providerRef =
                    receipt.coordinate.providerRef,
                credentialRef =
                    receipt.coordinate.credentialRef,
            ) { secret ->
                observed =
                    secret
                assertEquals(
                    "kimi-fixture-secret",
                    secret.concatToString(),
                )
                secret.size
            }

        assertEquals(
            "kimi-fixture-secret".length,
            length,
        )
        assertTrue(
            observed.all {
                it == '\u0000'
            },
        )
        assertEquals(
            1,
            host.resolutionCallbacks,
        )
    }

    @Test
    fun replaceMintsNewRefAndMakesPriorSecretUnresolvable() {
        val host =
            FakeProtectedHostStore()
        val custody =
            custodian(
                host,
            )
        val prior =
            custody.store(
                providerRef =
                    VexLifePartnerProviderCatalog.AnthropicRef,
                secret =
                    "old-fixture-secret"
                        .toCharArray(),
            )
        val replaced =
            custody.replace(
                providerRef =
                    prior.coordinate.providerRef,
                credentialRef =
                    prior.coordinate.credentialRef,
                replacement =
                    "new-fixture-secret"
                        .toCharArray(),
            )

        assertEquals(
            VexLifePartnerCredentialCustody.Operation.REPLACED,
            replaced.operation,
        )
        assertEquals(
            prior.coordinate.credentialRef,
            replaced.priorCredentialRef,
        )
        assertFalse(
            host.contains(
                prior.coordinate,
            ),
        )
        assertTrue(
            host.contains(
                replaced.coordinate,
            ),
        )

        assertFailsWith<IllegalArgumentException> {
            custody.withCredential(
                providerRef =
                    prior.coordinate.providerRef,
                credentialRef =
                    prior.coordinate.credentialRef,
            ) {
                Unit
            }
        }

        val current =
            custody.withCredential(
                providerRef =
                    replaced.coordinate.providerRef,
                credentialRef =
                    replaced.coordinate.credentialRef,
            ) {
                it.concatToString()
            }
        assertEquals(
            "new-fixture-secret",
            current,
        )
    }

    @Test
    fun failedReplacementRetiresStagedNewSecretBeforeFailing() {
        val host =
            FakeProtectedHostStore()
        val custody =
            custodian(
                host,
            )
        val prior =
            custody.store(
                providerRef =
                    VexLifePartnerProviderCatalog.OpenAiRef,
                secret =
                    "old-fixture-secret"
                        .toCharArray(),
            )
        host.deleteFailures +=
            prior.coordinate

        assertFailsWith<IllegalStateException> {
            custody.replace(
                providerRef =
                    prior.coordinate.providerRef,
                credentialRef =
                    prior.coordinate.credentialRef,
                replacement =
                    "new-fixture-secret"
                        .toCharArray(),
            )
        }

        assertTrue(
            host.contains(
                prior.coordinate,
            ),
        )
        assertFalse(
            host.contains(
                VexLifePartnerCredentialCustody.Coordinate(
                    providerRef =
                        prior.coordinate.providerRef,
                    credentialRef =
                        "credential.vexlife.partner.fixture-0002",
                ),
            ),
        )
    }

    @Test
    fun retireMakesExactCredentialAbsentAndUnresolvable() {
        val host =
            FakeProtectedHostStore()
        val custody =
            custodian(
                host,
            )
        val stored =
            custody.store(
                providerRef =
                    VexLifePartnerProviderCatalog.OpenAiRef,
                secret =
                    "retire-fixture-secret"
                        .toCharArray(),
            )

        val retired =
            custody.retire(
                providerRef =
                    stored.coordinate.providerRef,
                credentialRef =
                    stored.coordinate.credentialRef,
            )

        assertEquals(
            VexLifePartnerCredentialCustody.Operation.RETIRED,
            retired.operation,
        )
        assertEquals(
            VexLifePartnerCredentialCustody.Standing.ABSENT,
            custody.observe(
                providerRef =
                    stored.coordinate.providerRef,
                credentialRef =
                    stored.coordinate.credentialRef,
            ).standing,
        )
        assertFailsWith<IllegalArgumentException> {
            custody.withCredential(
                providerRef =
                    stored.coordinate.providerRef,
                credentialRef =
                    stored.coordinate.credentialRef,
            ) {
                Unit
            }
        }
    }

    @Test
    fun unsupportedProviderCannotMintCredentialCustody() {
        val host =
            FakeProtectedHostStore()

        assertFailsWith<IllegalArgumentException> {
            custodian(
                host,
            )
                .store(
                    providerRef =
                        "provider.custom.unformed",
                    secret =
                        "fixture-secret"
                            .toCharArray(),
                )
        }
    }
}

// [VXG RealForever]
