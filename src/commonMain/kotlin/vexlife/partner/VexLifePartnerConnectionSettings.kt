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
    enum class TransitionKind {
        STAGE_CONNECT,
        STAGE_REPLACE,
        CLEANUP_REPLACED,
        REMOVE,
    }

    @Serializable
    data class Transition(
        val providerRef: String,
        val kind: TransitionKind,
        val stagedCredentialRef: String? = null,
        val stagedAccountScope: VexLifePartnerAccountScope.Selection? = null,
        val retiringCredentialRefs: List<String> = emptyList(),
    ) {
        init {
            requireNotNull(
                VexLifePartnerProviderCatalog.resolve(
                    providerRef,
                ),
            ) {
                "Partner connection transition requires one source-owned built-in provider"
            }
            stagedCredentialRef
                ?.let(
                    ::requireCredentialRef,
                )
            stagedAccountScope
                ?.let {
                    require(
                        it.providerRef ==
                            providerRef,
                    ) {
                        "staged Partner account scope must belong to the transition provider"
                    }
                }
            retiringCredentialRefs.forEach(
                ::requireCredentialRef,
            )
            require(
                retiringCredentialRefs.distinct().size ==
                    retiringCredentialRefs.size,
            ) {
                "Partner connection transition retiring refs must be unique"
            }

            when (
                kind
            ) {
                TransitionKind.STAGE_CONNECT,
                TransitionKind.STAGE_REPLACE,
                ->
                    require(
                        stagedCredentialRef !=
                            null &&
                            stagedAccountScope !=
                            null &&
                            retiringCredentialRefs.isEmpty(),
                    ) {
                        "staging Partner transition requires staged ref/scope and no retiring refs"
                    }

                TransitionKind.CLEANUP_REPLACED,
                TransitionKind.REMOVE,
                ->
                    require(
                        stagedCredentialRef ==
                            null &&
                            stagedAccountScope ==
                            null &&
                            retiringCredentialRefs.isNotEmpty(),
                    ) {
                        "retiring Partner transition requires retiring refs and no staged ref/scope"
                    }
            }
        }
    }

    @Serializable
    data class Value(
        val schemaVersion: String,
        val entries: List<Entry>,
        val transitions: List<Transition> = emptyList(),
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
            require(
                transitions.map {
                    it.providerRef
                }
                    .distinct()
                    .size ==
                    transitions.size,
            ) {
                "Partner connection settings may contain at most one transition per provider"
            }
            require(
                transitions ==
                    transitions.sortedBy {
                        it.providerRef
                    },
            ) {
                "Partner connection transitions must be in canonical provider order"
            }

            val entriesByProvider =
                entries.associateBy {
                    it.providerRef
                }
            transitions.forEach { transition ->
                val current =
                    entriesByProvider[
                        transition.providerRef
                    ]
                when (
                    transition.kind
                ) {
                    TransitionKind.STAGE_CONNECT ->
                        require(
                            current ==
                                null
                        ) {
                            "STAGE_CONNECT requires no current Partner entry"
                        }

                    TransitionKind.STAGE_REPLACE ->
                        require(
                            current !=
                                null &&
                                transition.stagedCredentialRef !=
                                current.credentialRef
                        ) {
                            "STAGE_REPLACE requires one different current Partner credential"
                        }

                    TransitionKind.CLEANUP_REPLACED ->
                        require(
                            current !=
                                null &&
                                current.credentialRef !in
                                transition.retiringCredentialRefs
                        ) {
                            "CLEANUP_REPLACED requires a new current ref distinct from retiring refs"
                        }

                    TransitionKind.REMOVE ->
                        require(
                            current !=
                                null &&
                                transition.retiringCredentialRefs ==
                                listOf(
                                    current.credentialRef,
                                )
                        ) {
                            "REMOVE must retire the exact current Partner credential"
                        }
                }
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
        transitions: List<Transition> = emptyList(),
    ): Value =
        Value(
            schemaVersion = Schema,
            entries =
                entries.sortedBy {
                    it.providerRef
                },
            transitions =
                transitions.sortedBy {
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

    fun transitionFor(
        value: Value,
        providerRef: String,
    ): Transition? =
        value.transitions.firstOrNull {
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
