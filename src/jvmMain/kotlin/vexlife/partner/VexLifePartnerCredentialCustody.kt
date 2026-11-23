package vexlife.partner

import java.util.UUID

/**
 * Portable custody coordinator for one built-in Partner provider credential.
 *
 * The host-specific [ProtectedHostStore] owns secret persistence. This coordinator owns only the
 * provider/ref binding and the narrow lifecycle through which semantic Product code may store,
 * observe, resolve for one admitted execution callback, replace, or retire that protected secret.
 *
 * Permanent:
 *
 * SECRET VALUE != CREDENTIAL REF
 * CREDENTIAL REF != PROVIDER AUTH ACCEPTANCE
 * CREDENTIAL CUSTODY != PROVIDER CONNECTION
 * CREDENTIAL CUSTODY != MODEL USE AUTHORITY
 * WRONG PROVIDER != SECRET RESOLUTION
 */
object VexLifePartnerCredentialCustody {
    enum class Standing {
        PRESENT,
        ABSENT,
    }

    enum class Operation {
        STORED,
        REPLACED,
        RETIRED,
    }

    data class Coordinate(
        val providerRef: String,
        val credentialRef: String,
    ) {
        init {
            requireProvider(
                providerRef,
            )
            requireCredentialRef(
                credentialRef,
            )
        }
    }

    data class Observation(
        val coordinate: Coordinate,
        val standing: Standing,
    )

    data class MutationReceipt(
        val operation: Operation,
        val coordinate: Coordinate,
        val priorCredentialRef: String? = null,
        val secretProjected: Boolean = false,
    ) {
        init {
            require(
                !secretProjected,
            ) {
                "Partner credential mutation receipt may not project secret material"
            }
            priorCredentialRef
                ?.let(
                    ::requireCredentialRef,
                )
        }
    }

    /**
     * Host-protected secret persistence.
     *
     * Implementations must bind storage to the exact provider/ref pair and must not expose the
     * secret through logs, exceptions, argv, temporary files, durable Product JSON, or receipts.
     * [withSecret] supplies secret material only to its immediate callback.
     */
    interface ProtectedHostStore {
        fun store(
            coordinate: Coordinate,
            secret: CharArray,
        )

        fun contains(
            coordinate: Coordinate,
        ): Boolean

        fun <T> withSecret(
            coordinate: Coordinate,
            use: (CharArray) -> T,
        ): T

        /**
         * Delete one exact protected secret.
         *
         * Return true only when the secret is absent after this call. An already-absent coordinate
         * may therefore return true.
         */
        fun delete(
            coordinate: Coordinate,
        ): Boolean
    }

    class Custodian(
        private val host: ProtectedHostStore,
        private val nextCredentialRef: () -> String = {
            "credential.vexlife.partner." +
                UUID.randomUUID()
                    .toString()
                    .lowercase()
        },
    ) {
        fun store(
            providerRef: String,
            secret: CharArray,
        ): MutationReceipt {
            requireProvider(
                providerRef,
            )
            requireSecret(
                secret,
            )
            val coordinate =
                Coordinate(
                    providerRef = providerRef,
                    credentialRef =
                        nextCredentialRef()
                            .also(
                                ::requireCredentialRef,
                            ),
                )
            require(
                !host.contains(
                    coordinate,
                ),
            ) {
                "new Partner credentialRef already exists in protected host custody"
            }

            val temporary =
                secret.copyOf()
            try {
                host.store(
                    coordinate = coordinate,
                    secret = temporary,
                )
            } finally {
                temporary.fill(
                    '\u0000',
                )
            }

            require(
                host.contains(
                    coordinate,
                ),
            ) {
                "protected host store did not retain the new Partner credential"
            }

            return MutationReceipt(
                operation = Operation.STORED,
                coordinate = coordinate,
            )
        }

        fun observe(
            providerRef: String,
            credentialRef: String,
        ): Observation {
            val coordinate =
                Coordinate(
                    providerRef = providerRef,
                    credentialRef = credentialRef,
                )
            return Observation(
                coordinate = coordinate,
                standing =
                    if (
                        host.contains(
                            coordinate,
                        )
                    ) {
                        Standing.PRESENT
                    } else {
                        Standing.ABSENT
                    },
            )
        }

        /**
         * Resolve one exact protected credential only for the immediate execution callback.
         *
         * The callback receives an ephemeral copy. This coordinator zeroes that copy before return
         * or failure. The caller must not retain/copy it beyond the admitted provider effect.
         */
        fun <T> withCredential(
            providerRef: String,
            credentialRef: String,
            use: (CharArray) -> T,
        ): T {
            val coordinate =
                Coordinate(
                    providerRef = providerRef,
                    credentialRef = credentialRef,
                )
            require(
                host.contains(
                    coordinate,
                ),
            ) {
                "Partner credential is not present in protected host custody"
            }

            return host.withSecret(
                coordinate,
            ) { protected ->
                requireSecret(
                    protected,
                )
                val ephemeral =
                    protected.copyOf()
                try {
                    use(
                        ephemeral,
                    )
                } finally {
                    ephemeral.fill(
                        '\u0000',
                    )
                }
            }
        }

        /**
         * Replace by forming a new opaque ref first, then retiring the prior ref.
         *
         * If retirement of the prior ref cannot be proved, the newly stored secret is retired
         * again and replacement fails. No successful receipt may leave both refs current.
         */
        fun replace(
            providerRef: String,
            credentialRef: String,
            replacement: CharArray,
        ): MutationReceipt {
            val prior =
                Coordinate(
                    providerRef = providerRef,
                    credentialRef = credentialRef,
                )
            require(
                host.contains(
                    prior,
                ),
            ) {
                "Partner credential replacement requires one present prior credential"
            }

            val stored =
                store(
                    providerRef = providerRef,
                    secret = replacement,
                )
            val next =
                stored.coordinate

            val priorRetired =
                runCatching {
                    host.delete(
                        prior,
                    )
                }
                    .getOrDefault(
                        false,
                    )
            if (
                !priorRetired ||
                    host.contains(
                        prior,
                    )
            ) {
                runCatching {
                    host.delete(
                        next,
                    )
                }
                error(
                    "Partner credential replacement could not retire the prior protected credential",
                )
            }

            return MutationReceipt(
                operation = Operation.REPLACED,
                coordinate = next,
                priorCredentialRef = prior.credentialRef,
            )
        }

        fun retire(
            providerRef: String,
            credentialRef: String,
        ): MutationReceipt {
            val coordinate =
                Coordinate(
                    providerRef = providerRef,
                    credentialRef = credentialRef,
                )
            require(
                host.delete(
                    coordinate,
                ) &&
                    !host.contains(
                        coordinate,
                    ),
            ) {
                "protected host store did not retire the Partner credential"
            }

            return MutationReceipt(
                operation = Operation.RETIRED,
                coordinate = coordinate,
            )
        }
    }

    private fun requireProvider(
        providerRef: String,
    ) {
        requireNotNull(
            VexLifePartnerProviderCatalog.resolve(
                providerRef,
            ),
        ) {
            "Partner credential custody requires one source-owned built-in providerRef"
        }
    }

    private fun requireCredentialRef(
        credentialRef: String,
    ) {
        require(
            credentialRef.matches(
                Regex(
                    "credential\\.vexlife\\.partner\\.[a-z0-9][a-z0-9._-]{7,180}",
                ),
            ),
        ) {
            "Partner credentialRef must be one bounded opaque Product coordinate"
        }
    }

    private fun requireSecret(
        secret: CharArray,
    ) {
        require(
            secret.isNotEmpty() &&
                secret.size <=
                16_384 &&
                secret.none {
                    it == '\u0000'
                },
        ) {
            "Partner credential secret must be one bounded non-empty value"
        }
    }
}

// [VXG RealForever]
