package vexlife.model.heretic

/**
 * Portable projection of the current Heretic host-resource observation.
 *
 * Platform code proves these states from process/lifetime evidence. This contract prevents a
 * consumer from reducing ownership to "a PID exists" or lifecycle currentness to "the port answers".
 */
object HereticRuntimeObservation {
    const val Schema =
        "vexlife.host-resource-observation/v1"

    const val TruthClass =
        "SOURCE_BOUND_HOST_RESOURCE_OBSERVATION"

    const val ResourceRef =
        "resource.vexlife-prod.heretic.persistent-runtime"

    const val ResourceClass =
        "HERETIC_PERSISTENT_MLX_RUNTIME"

    enum class OwnershipState {
        Unowned,
        Unknown,
        Ambiguous,
        ExactOwned,
    }

    enum class LifecycleState {
        Current,
        Stale,
        Abandoned,
        Unknown,
    }

    data class OwnershipProof(
        val ownerReceiptRef: String,
        val runtimeIdentityRef: String,
        val executableIdentityRef: String,
        val processGroupIdentityRef: String,
        val currentness: String = "CURRENT",
        val pidMatchOnly: Boolean = false,
        val exactOwnedProcessInstance: Boolean = true,
    ) {
        init {
            requirePortableRef(
                ownerReceiptRef,
                "ownerReceiptRef",
            )
            requirePortableRef(
                runtimeIdentityRef,
                "runtimeIdentityRef",
            )
            requirePortableRef(
                executableIdentityRef,
                "executableIdentityRef",
            )
            requirePortableRef(
                processGroupIdentityRef,
                "processGroupIdentityRef",
            )
            require(
                currentness ==
                    "CURRENT",
            ) {
                "Heretic runtime ownership proof must be current"
            }
            require(
                !pidMatchOnly,
            ) {
                "Heretic runtime ownership cannot be proved by PID match only"
            }
            require(
                exactOwnedProcessInstance,
            ) {
                "Heretic runtime ownership proof must bind one exact process instance"
            }
        }
    }

    data class Value(
        val schemaVersion: String = Schema,
        val truthClass: String = TruthClass,
        val resourceRef: String = ResourceRef,
        val resourceClass: String = ResourceClass,
        val ownershipState: OwnershipState,
        val lifecycleState: LifecycleState,
        val lifecycleEvidenceRefOrNull: String?,
        val currentness: String = "CURRENT",
        val ownershipProof: OwnershipProof?,
        val sourceRefs: List<String>,
    ) {
        init {
            require(
                schemaVersion ==
                    Schema &&
                    truthClass ==
                    TruthClass &&
                    resourceRef ==
                    ResourceRef &&
                    resourceClass ==
                    ResourceClass,
            ) {
                "Heretic runtime observation identity drift"
            }
            require(
                currentness ==
                    "CURRENT",
            ) {
                "Heretic runtime observation must report current observation currentness"
            }
            require(
                sourceRefs.isNotEmpty(),
            ) {
                "Heretic runtime observation requires source refs"
            }
            sourceRefs.forEach {
                requirePortableRef(
                    it,
                    "sourceRef",
                )
            }
            lifecycleEvidenceRefOrNull?.let {
                requirePortableRef(
                    it,
                    "lifecycleEvidenceRef",
                )
            }

            when (
                ownershipState
            ) {
                OwnershipState.ExactOwned ->
                    require(
                        ownershipProof !=
                            null,
                    ) {
                        "Exact-owned Heretic runtime requires exact ownership proof"
                    }

                OwnershipState.Unowned,
                OwnershipState.Unknown,
                OwnershipState.Ambiguous,
                ->
                    require(
                        ownershipProof ==
                            null,
                    ) {
                        "Non-owned/ambiguous Heretic runtime cannot carry exact ownership proof"
                    }
            }

            if (
                lifecycleState ==
                    LifecycleState.Abandoned
            ) {
                require(
                    ownershipState ==
                        OwnershipState.Unowned &&
                        lifecycleEvidenceRefOrNull !=
                        null,
                ) {
                    "Abandoned Heretic runtime requires current not-live evidence"
                }
            }

            if (
                lifecycleState ==
                    LifecycleState.Stale
            ) {
                require(
                    ownershipState ==
                        OwnershipState.ExactOwned &&
                        lifecycleEvidenceRefOrNull !=
                        null,
                ) {
                    "Stale Heretic runtime requires exact ownership + source-stale evidence"
                }
            }
        }
    }
}

// [VXG RealForever]
