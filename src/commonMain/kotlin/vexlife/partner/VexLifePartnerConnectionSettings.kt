package vexlife.partner

import kotlinx.serialization.Serializable

/**
 * Durable non-secret Partner connection coordinates kept in the Vex Home.
 *
 * This contract never stores a provider credential value. The protected host owner stores the
 * secret; this contract remembers only the opaque credentialRef needed to address that host-owned
 * secret plus any admitted non-secret provider account scope.
 */
object VexLifePartnerConnectionSettings {
    const val Schema =
        "vexlife-prod.partner-connections/v1"

    @Serializable
    data class Entry(
        val providerRef: String,
        val credentialRef: String,
        val accountScope: VexLifePartnerAccountScope.Selection,
    ) {
        init {
            requireNotNull(
                VexLifePartnerProviderCatalog.resolve(
                    providerRef,
                ),
            ) {
                "Partner connection settings require one source-owned built-in provider"
            }
            requireCredentialRef(
                credentialRef,
            )
            require(
                accountScope.providerRef ==
                    providerRef,
            ) {
                "Partner connection account scope must belong to the same provider"
            }
        }
    }

    @Serializable
    data class Value(
        val schemaVersion: String,
        val entries: List<Entry>,
    ) {
        init {
            require(
                schemaVersion ==
                    Schema,
            ) {
                "Partner connection settings schema is not current"
            }
            require(
                entries.map {
                    it.providerRef
                }
                    .distinct()
                    .size ==
                    entries.size,
            ) {
                "Partner connection settings may contain at most one current entry per provider"
            }
            require(
                entries ==
                    entries.sortedBy {
                        it.providerRef
                    },
            ) {
                "Partner connection settings entries must be in canonical provider order"
            }
        }
    }

    val Empty =
        Value(
            schemaVersion = Schema,
            entries = emptyList(),
        )

    fun form(
        entries: List<Entry>,
    ): Value =
        Value(
            schemaVersion = Schema,
            entries =
                entries.sortedBy {
                    it.providerRef
                },
        )

    fun entryFor(
        value: Value,
        providerRef: String,
    ): Entry? =
        value.entries.firstOrNull {
            it.providerRef ==
                providerRef
        }

    private fun requireCredentialRef(
        credentialRef: String,
    ) {
        require(
            credentialRef.startsWith(
                "credential.vexlife.partner.",
            ) &&
                credentialRef.length in
                    36..220 &&
                credentialRef.none(
                    Char::isWhitespace,
                ) &&
                credentialRef.none {
                    it == '\u0000' ||
                        it == '\n' ||
                        it == '\r'
                },
        ) {
            "Partner connection credentialRef must be one bounded opaque Product coordinate"
        }
    }
}

// [VXG RealForever]
