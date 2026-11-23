package vexlife.partner

import java.util.UUID

/**
 * Portable custody coordinator for one built-in Partner provider credential.
 *
 * The host-specific [ProtectedHostStore] owns secret persistence. This coordinator owns only the
 * provider/ref binding and the narrow lifecycle through which semantic Product code may store,
 * observe, replace, or retire that protected secret. Production inference secret resolution belongs
 * wholly inside the host effect owner; this portable JVM-facing contract has no secret-read method.
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
    /** Cross-platform bound: Windows CRED_MAX_CREDENTIAL_BLOB_SIZE = 5 * 512 bytes. */
    const val MaximumSecretBytes =
        2_560
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
     * Production execution resolves the secret inside the protected host effect owner. This
     * portable JVM-facing custody port deliberately has no secret-read method.
     */
    interface ProtectedHostStore {
        fun store(
            coordinate: Coordinate,
            secret: CharArray,
        )

        fun contains(
            coordinate: Coordinate,
        ): Boolean

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
        fun allocate(
            providerRef: String,
        ): Coordinate {
            requireProvider(
                providerRef,
            )
            return Coordinate(
                providerRef = providerRef,
                credentialRef =
                    nextCredentialRef()
                        .also(
                            ::requireCredentialRef,
                        ),
            )
        }

        fun store(
            coordinate: Coordinate,
            secret: CharArray,
        ): MutationReceipt {
            requireSecret(
                secret,
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

        fun store(
            providerRef: String,
            secret: CharArray,
        ): MutationReceipt =
            store(
                coordinate =
                    allocate(
                        providerRef,
                    ),
                secret = secret,
            )

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
                val stagedRetired =
                    runCatching {
                        host.delete(
                            next,
                        ) &&
                            !host.contains(
                                next,
                            )
                    }
                        .getOrDefault(
                            false,
                        )
                require(
                    stagedRetired,
                ) {
                    "Partner credential replacement rollback could not retire the staged protected credential"
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

    private fun utf8ByteCount(
        secret: CharArray,
    ): Int {
        val encoded =
            java.nio.charset.StandardCharsets.UTF_8
                .newEncoder()
                .encode(
                    java.nio.CharBuffer.wrap(
                        secret,
                    ),
                )
        val count =
            encoded.remaining()
        if (
            encoded.hasArray()
        ) {
            encoded
                .array()
                .fill(
                    0,
                )
        }
        return count
    }

    private fun requireSecret(
        secret: CharArray,
    ) {
        require(
            secret.isNotEmpty() &&
                utf8ByteCount(
                    secret,
                ) <=
                MaximumSecretBytes &&
                secret.none {
                    it == '\u0000'
                },
        ) {
            "Partner credential secret must be one bounded non-empty value"
        }
    }
}

// [VXG RealForever]
