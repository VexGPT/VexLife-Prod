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

        assertEquals(
            VexLifePartnerCredentialCustody.Standing.ABSENT,
            custody.observe(
                providerRef =
                    prior.coordinate.providerRef,
                credentialRef =
                    prior.coordinate.credentialRef,
            ).standing,
        )
        assertEquals(
            VexLifePartnerCredentialCustody.Standing.PRESENT,
            custody.observe(
                providerRef =
                    replaced.coordinate.providerRef,
                credentialRef =
                    replaced.coordinate.credentialRef,
            ).standing,
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
