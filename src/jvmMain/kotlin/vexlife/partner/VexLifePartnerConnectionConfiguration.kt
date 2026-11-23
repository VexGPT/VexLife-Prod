package vexlife.partner

/**
 * Crash-recoverable convergence across durable non-secret Partner settings and protected host
 * credential custody.
 *
 * The durable settings contract records transition intent before each cross-owner effect. Recovery
 * can therefore resume or roll back from source truth rather than guessing whether a secret was
 * stored/deleted.
 */
object VexLifePartnerConnectionConfiguration {
    enum class ResultState {
        CONNECTED,
        REPLACED,
        REMOVED,
        RECOVERED_CONNECTED,
        RECOVERED_REPLACED,
        RECOVERED_REMOVED,
        RECOVERED_ROLLED_BACK,
        RECOVERED_CLEANUP,
    }

    data class Result(
        val providerRef: String,
        val state: ResultState,
        val credentialRef: String?,
        val recoveryPerformed: Boolean,
    )

    class RecoveryRequiredException(
        val providerRef: String,
        val transitionKind: VexLifePartnerConnectionSettings.TransitionKind,
        message: String,
        cause: Throwable? = null,
    ) : IllegalStateException(
            message,
            cause,
        )

    interface SettingsPort {
        fun read(): VexLifePartnerConnectionSettings.Value

        fun write(
            value: VexLifePartnerConnectionSettings.Value,
        )
    }

    class DurableSettingsPort(
        private val store: VexLifePartnerConnectionSettingsStore,
    ) : SettingsPort {
        override fun read(): VexLifePartnerConnectionSettings.Value =
            store.read()

        override fun write(
            value: VexLifePartnerConnectionSettings.Value,
        ) {
            store.write(
                value,
            )
        }
    }

    class Coordinator(
        private val settings: SettingsPort,
        private val custody: VexLifePartnerCredentialCustody.Custodian,
    ) {
        private val lock =
            Any()

        fun connect(
            providerRef: String,
            accountScope: VexLifePartnerAccountScope.Selection,
            secret: CharArray,
        ): Result =
            synchronized(
                lock,
            ) {
                try {
                    require(
                        accountScope.providerRef ==
                            providerRef,
                    ) {
                        "Partner connection scope does not belong to the selected provider"
                    }
                    val before =
                        settings.read()
                    require(
                        VexLifePartnerConnectionSettings.entryFor(
                            before,
                            providerRef,
                        ) ==
                            null &&
                            VexLifePartnerConnectionSettings.transitionFor(
                                before,
                                providerRef,
                            ) ==
                            null,
                    ) {
                        "Partner connect requires one unconfigured provider with no pending transition"
                    }

                    val coordinate =
                        custody.allocate(
                            providerRef,
                        )
                    val staged =
                        VexLifePartnerConnectionSettings.Transition(
                            providerRef = providerRef,
                            kind =
                                VexLifePartnerConnectionSettings.TransitionKind.STAGE_CONNECT,
                            stagedCredentialRef = coordinate.credentialRef,
                            stagedAccountScope = accountScope,
                        )
                    val pending =
                        withTransition(
                            before,
                            staged,
                        )
                    settings.write(
                        pending,
                    )

                    try {
                        custody.store(
                            coordinate = coordinate,
                            secret = secret,
                        )
                    } catch (
                        error: Throwable,
                    ) {
                        throw RecoveryRequiredException(
                            providerRef = providerRef,
                            transitionKind = staged.kind,
                            message =
                                "Partner credential store did not complete; durable STAGE_CONNECT remains for recovery",
                            cause = error,
                        )
                    }

                    val current =
                        VexLifePartnerConnectionSettings.Entry(
                            providerRef = providerRef,
                            credentialRef = coordinate.credentialRef,
                            accountScope = accountScope,
                        )
                    try {
                        settings.write(
                            VexLifePartnerConnectionSettings.form(
                                entries =
                                    before.entries +
                                        current,
                                transitions =
                                    pending.transitions.filterNot {
                                        it.providerRef ==
                                            providerRef
                                    },
                            ),
                        )
                    } catch (
                        error: Throwable,
                    ) {
                        throw RecoveryRequiredException(
                            providerRef = providerRef,
                            transitionKind = staged.kind,
                            message =
                                "Partner secret is protected but durable STAGE_CONNECT must be finalized by recovery",
                            cause = error,
                        )
                    }

                    Result(
                        providerRef = providerRef,
                        state = ResultState.CONNECTED,
                        credentialRef = coordinate.credentialRef,
                        recoveryPerformed = false,
                    )
                } finally {
                    secret.fill(
                        '\u0000',
                    )
                }
            }

        fun replace(
            providerRef: String,
            accountScope: VexLifePartnerAccountScope.Selection,
            replacement: CharArray,
        ): Result =
            synchronized(
                lock,
            ) {
                try {
                    require(
                        accountScope.providerRef ==
                            providerRef,
                    ) {
                        "Partner replacement scope does not belong to the selected provider"
                    }
                    val before =
                        settings.read()
                    val prior =
                        requireNotNull(
                            VexLifePartnerConnectionSettings.entryFor(
                                before,
                                providerRef,
                            ),
                        ) {
                            "Partner replacement requires one current configured credential"
                        }
                    require(
                        VexLifePartnerConnectionSettings.transitionFor(
                            before,
                            providerRef,
                        ) ==
                            null,
                    ) {
                        "Partner replacement requires no pending transition"
                    }

                    val next =
                        custody.allocate(
                            providerRef,
                        )
                    val staged =
                        VexLifePartnerConnectionSettings.Transition(
                            providerRef = providerRef,
                            kind =
                                VexLifePartnerConnectionSettings.TransitionKind.STAGE_REPLACE,
                            stagedCredentialRef = next.credentialRef,
                            stagedAccountScope = accountScope,
                        )
                    val pending =
                        withTransition(
                            before,
                            staged,
                        )
                    settings.write(
                        pending,
                    )

                    try {
                        custody.store(
                            coordinate = next,
                            secret = replacement,
                        )
                    } catch (
                        error: Throwable,
                    ) {
                        throw RecoveryRequiredException(
                            providerRef = providerRef,
                            transitionKind = staged.kind,
                            message =
                                "Partner replacement secret store did not complete; STAGE_REPLACE remains",
                            cause = error,
                        )
                    }

                    val nextEntry =
                        VexLifePartnerConnectionSettings.Entry(
                            providerRef = providerRef,
                            credentialRef = next.credentialRef,
                            accountScope = accountScope,
                        )
                    val cleanup =
                        VexLifePartnerConnectionSettings.Transition(
                            providerRef = providerRef,
                            kind =
                                VexLifePartnerConnectionSettings.TransitionKind.CLEANUP_REPLACED,
                            retiringCredentialRefs =
                                listOf(
                                    prior.credentialRef,
                                ),
                        )
                    val switched =
                        VexLifePartnerConnectionSettings.form(
                            entries =
                                before.entries
                                    .filterNot {
                                        it.providerRef ==
                                            providerRef
                                    } +
                                    nextEntry,
                            transitions =
                                pending.transitions
                                    .filterNot {
                                        it.providerRef ==
                                            providerRef
                                    } +
                                    cleanup,
                        )
                    try {
                        settings.write(
                            switched,
                        )
                    } catch (
                        error: Throwable,
                    ) {
                        throw RecoveryRequiredException(
                            providerRef = providerRef,
                            transitionKind = staged.kind,
                            message =
                                "Partner replacement secret is staged; STAGE_REPLACE remains for recovery",
                            cause = error,
                        )
                    }

                    try {
                        custody.retire(
                            providerRef = providerRef,
                            credentialRef = prior.credentialRef,
                        )
                    } catch (
                        error: Throwable,
                    ) {
                        throw RecoveryRequiredException(
                            providerRef = providerRef,
                            transitionKind = cleanup.kind,
                            message =
                                "Partner replacement is current but prior credential cleanup remains",
                            cause = error,
                        )
                    }

                    settings.write(
                        withoutTransition(
                            switched,
                            providerRef,
                        ),
                    )

                    Result(
                        providerRef = providerRef,
                        state = ResultState.REPLACED,
                        credentialRef = next.credentialRef,
                        recoveryPerformed = false,
                    )
                } finally {
                    replacement.fill(
                        '\u0000',
                    )
                }
            }

        fun remove(
            providerRef: String,
        ): Result =
            synchronized(
                lock,
            ) {
                val before =
                    settings.read()
                val current =
                    requireNotNull(
                        VexLifePartnerConnectionSettings.entryFor(
                            before,
                            providerRef,
                        ),
                    ) {
                        "Partner removal requires one current configured credential"
                    }
                require(
                    VexLifePartnerConnectionSettings.transitionFor(
                        before,
                        providerRef,
                    ) ==
                        null,
                ) {
                    "Partner removal requires no pending transition"
                }

                val removing =
                    VexLifePartnerConnectionSettings.Transition(
                        providerRef = providerRef,
                        kind =
                            VexLifePartnerConnectionSettings.TransitionKind.REMOVE,
                        retiringCredentialRefs =
                            listOf(
                                current.credentialRef,
                            ),
                    )
                val pending =
                    withTransition(
                        before,
                        removing,
                    )
                settings.write(
                    pending,
                )

                try {
                    custody.retire(
                        providerRef = providerRef,
                        credentialRef = current.credentialRef,
                    )
                } catch (
                    error: Throwable,
                ) {
                    throw RecoveryRequiredException(
                        providerRef = providerRef,
                        transitionKind = removing.kind,
                        message =
                            "Partner removal transition remains until protected credential retirement completes",
                        cause = error,
                    )
                }

                settings.write(
                    VexLifePartnerConnectionSettings.form(
                        entries =
                            before.entries.filterNot {
                                it.providerRef ==
                                    providerRef
                            },
                        transitions =
                            pending.transitions.filterNot {
                                it.providerRef ==
                                    providerRef
                            },
                    ),
                )

                Result(
                    providerRef = providerRef,
                    state = ResultState.REMOVED,
                    credentialRef = null,
                    recoveryPerformed = false,
                )
            }

        fun recover(): List<Result> =
            synchronized(
                lock,
            ) {
                val results =
                    mutableListOf<Result>()

                while (
                    true
                ) {
                    val current =
                        settings.read()
                    val transition =
                        current.transitions.firstOrNull()
                            ?: break

                    val result =
                        recoverOne(
                            current = current,
                            transition = transition,
                        )
                    results +=
                        result
                }

                results
            }

        private fun recoverOne(
            current: VexLifePartnerConnectionSettings.Value,
            transition: VexLifePartnerConnectionSettings.Transition,
        ): Result {
            val providerRef =
                transition.providerRef

            return when (
                transition.kind
            ) {
                VexLifePartnerConnectionSettings.TransitionKind.STAGE_CONNECT -> {
                    val staged =
                        stagedCoordinate(
                            transition,
                        )
                    val observation =
                        custody.observe(
                            providerRef = providerRef,
                            credentialRef = staged.credentialRef,
                        )
                    if (
                        observation.standing ==
                            VexLifePartnerCredentialCustody.Standing.ABSENT
                    ) {
                        settings.write(
                            withoutTransition(
                                current,
                                providerRef,
                            ),
                        )
                        Result(
                            providerRef = providerRef,
                            state = ResultState.RECOVERED_ROLLED_BACK,
                            credentialRef = null,
                            recoveryPerformed = true,
                        )
                    } else {
                        val entry =
                            VexLifePartnerConnectionSettings.Entry(
                                providerRef = providerRef,
                                credentialRef = staged.credentialRef,
                                accountScope =
                                    requireNotNull(
                                        transition.stagedAccountScope,
                                    ),
                            )
                        settings.write(
                            VexLifePartnerConnectionSettings.form(
                                entries =
                                    current.entries +
                                        entry,
                                transitions =
                                    current.transitions.filterNot {
                                        it.providerRef ==
                                            providerRef
                                    },
                            ),
                        )
                        Result(
                            providerRef = providerRef,
                            state = ResultState.RECOVERED_CONNECTED,
                            credentialRef = staged.credentialRef,
                            recoveryPerformed = true,
                        )
                    }
                }

                VexLifePartnerConnectionSettings.TransitionKind.STAGE_REPLACE -> {
                    val prior =
                        requireNotNull(
                            VexLifePartnerConnectionSettings.entryFor(
                                current,
                                providerRef,
                            ),
                        )
                    val staged =
                        stagedCoordinate(
                            transition,
                        )
                    val observation =
                        custody.observe(
                            providerRef = providerRef,
                            credentialRef = staged.credentialRef,
                        )
                    if (
                        observation.standing ==
                            VexLifePartnerCredentialCustody.Standing.ABSENT
                    ) {
                        settings.write(
                            withoutTransition(
                                current,
                                providerRef,
                            ),
                        )
                        Result(
                            providerRef = providerRef,
                            state = ResultState.RECOVERED_ROLLED_BACK,
                            credentialRef = prior.credentialRef,
                            recoveryPerformed = true,
                        )
                    } else {
                        val nextEntry =
                            VexLifePartnerConnectionSettings.Entry(
                                providerRef = providerRef,
                                credentialRef = staged.credentialRef,
                                accountScope =
                                    requireNotNull(
                                        transition.stagedAccountScope,
                                    ),
                            )
                        val cleanup =
                            VexLifePartnerConnectionSettings.Transition(
                                providerRef = providerRef,
                                kind =
                                    VexLifePartnerConnectionSettings.TransitionKind.CLEANUP_REPLACED,
                                retiringCredentialRefs =
                                    listOf(
                                        prior.credentialRef,
                                    ),
                            )
                        val switched =
                            VexLifePartnerConnectionSettings.form(
                                entries =
                                    current.entries
                                        .filterNot {
                                            it.providerRef ==
                                                providerRef
                                        } +
                                        nextEntry,
                                transitions =
                                    current.transitions
                                        .filterNot {
                                            it.providerRef ==
                                                providerRef
                                        } +
                                        cleanup,
                            )
                        settings.write(
                            switched,
                        )
                        finishCleanup(
                            switched,
                            cleanup,
                        )
                        Result(
                            providerRef = providerRef,
                            state = ResultState.RECOVERED_REPLACED,
                            credentialRef = staged.credentialRef,
                            recoveryPerformed = true,
                        )
                    }
                }

                VexLifePartnerConnectionSettings.TransitionKind.CLEANUP_REPLACED -> {
                    val currentEntry =
                        requireNotNull(
                            VexLifePartnerConnectionSettings.entryFor(
                                current,
                                providerRef,
                            ),
                        )
                    finishCleanup(
                        current,
                        transition,
                    )
                    Result(
                        providerRef = providerRef,
                        state = ResultState.RECOVERED_CLEANUP,
                        credentialRef = currentEntry.credentialRef,
                        recoveryPerformed = true,
                    )
                }

                VexLifePartnerConnectionSettings.TransitionKind.REMOVE -> {
                    val entry =
                        requireNotNull(
                            VexLifePartnerConnectionSettings.entryFor(
                                current,
                                providerRef,
                            ),
                        )
                    try {
                        custody.retire(
                            providerRef = providerRef,
                            credentialRef = entry.credentialRef,
                        )
                    } catch (
                        error: Throwable,
                    ) {
                        throw RecoveryRequiredException(
                            providerRef = providerRef,
                            transitionKind = transition.kind,
                            message =
                                "Partner REMOVE recovery could not prove protected credential retirement",
                            cause = error,
                        )
                    }
                    settings.write(
                        VexLifePartnerConnectionSettings.form(
                            entries =
                                current.entries.filterNot {
                                    it.providerRef ==
                                        providerRef
                                },
                            transitions =
                                current.transitions.filterNot {
                                    it.providerRef ==
                                        providerRef
                                },
                        ),
                    )
                    Result(
                        providerRef = providerRef,
                        state = ResultState.RECOVERED_REMOVED,
                        credentialRef = null,
                        recoveryPerformed = true,
                    )
                }
            }
        }

        private fun finishCleanup(
            current: VexLifePartnerConnectionSettings.Value,
            cleanup: VexLifePartnerConnectionSettings.Transition,
        ) {
            cleanup.retiringCredentialRefs.forEach { credentialRef ->
                try {
                    custody.retire(
                        providerRef = cleanup.providerRef,
                        credentialRef = credentialRef,
                    )
                } catch (
                    error: Throwable,
                ) {
                    throw RecoveryRequiredException(
                        providerRef = cleanup.providerRef,
                        transitionKind = cleanup.kind,
                        message =
                            "Partner replaced-credential cleanup remains pending",
                        cause = error,
                    )
                }
            }
            settings.write(
                withoutTransition(
                    current,
                    cleanup.providerRef,
                ),
            )
        }

        private fun stagedCoordinate(
            transition: VexLifePartnerConnectionSettings.Transition,
        ): VexLifePartnerCredentialCustody.Coordinate =
            VexLifePartnerCredentialCustody.Coordinate(
                providerRef = transition.providerRef,
                credentialRef =
                    requireNotNull(
                        transition.stagedCredentialRef,
                    ),
            )

        private fun withTransition(
            current: VexLifePartnerConnectionSettings.Value,
            transition: VexLifePartnerConnectionSettings.Transition,
        ): VexLifePartnerConnectionSettings.Value =
            VexLifePartnerConnectionSettings.form(
                entries = current.entries,
                transitions =
                    current.transitions
                        .filterNot {
                            it.providerRef ==
                                transition.providerRef
                        } +
                        transition,
            )

        private fun withoutTransition(
            current: VexLifePartnerConnectionSettings.Value,
            providerRef: String,
        ): VexLifePartnerConnectionSettings.Value =
            VexLifePartnerConnectionSettings.form(
                entries = current.entries,
                transitions =
                    current.transitions.filterNot {
                        it.providerRef ==
                            providerRef
                    },
            )
    }
}

// [VXG RealForever]
