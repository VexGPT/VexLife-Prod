package vexlife.model.heretic

import kotlinx.serialization.Serializable

/**
 * Home + Companion-lineage scoped selection/binding relationship for one offered Heretic model.
 *
 * Selection and qualified runtime binding are deliberately different truths:
 *
 * ```
 * EstablishmentPending
 *   selected model identity
 *   realizationRef = null
 *
 * BoundProductModel
 *   selected model identity
 *   + one exact current HereticRuntimeRealization
 *   + one admitted runtime receipt owner
 * ```
 *
 * Selecting a model grants no authority to download, activate, replace another model or start a
 * process. Qualified runtime/process truth remains transient evidence owned by the runtime owner.
 */
object HereticModelConfiguration {
    const val Schema =
        "vexlife.heretic-model-configuration/v2"

    const val LegacySchema =
        "vexlife.heretic-model-configuration/v1"

    @Serializable
    enum class State(
        val wireValue: String,
    ) {
        EstablishmentPending(
            "HERETIC_ESTABLISHMENT_PENDING",
        ),
        BoundProductModel(
            "BOUND_HERETIC_PRODUCT_MODEL",
        ),
    }

    @Serializable
    data class BindingScope(
        val homeRef: String,
        val companionLineageRef: String,
    ) {
        init {
            requirePortableRef(
                homeRef,
                "homeRef",
            )
            requirePortableRef(
                companionLineageRef,
                "companionLineageRef",
            )
        }
    }

    /**
     * Transient exact runtime truth required to admit [State.BoundProductModel].
     *
     * Older Mac callers that do not yet name [realizationRef] explicitly resolve to the exact current
     * Mac ARM64 realization for [modelRef]. New cross-target owners should pass the exact realization
     * ref they qualified.
     */
    @ConsistentCopyVisibility
    data class QualifiedRuntimeEvidence internal constructor(
        val runtimeReceiptRef: String,
        val runtimeState: HereticRuntimeState,
        val modelRef: String,
        val realizationRef: String =
            HereticRuntimeRealization
                .require(
                    modelRef = modelRef,
                    target = HereticRuntimeRealization.MacArm64,
                )
                .realizationRef,
        val modelProfileRef: String,
        val artifactRef: String,
        val artifactContentSetSha256: String,
        val runtimeAdapterRef: String,
        val runtimeClass: String,
        val endpoint: String,
        val requestModel: String,
        val qualification: HereticRuntimeQualification,
    ) {
        init {
            requirePortableRef(
                runtimeReceiptRef,
                "runtimeReceiptRef",
            )
            requirePortableRef(
                realizationRef,
                "realizationRef",
            )
            require(
                runtimeState ==
                    HereticRuntimeState.Qualified,
            ) {
                "Heretic model binding requires a qualified runtime"
            }
            val realization =
                HereticRuntimeRealization.Entries
                    .singleOrNull {
                        it.realizationRef ==
                            realizationRef
                    }
            require(
                realization != null &&
                    modelRef ==
                    realization.modelRef &&
                    modelProfileRef ==
                    realization.modelProfileRef &&
                    artifactRef ==
                    realization.artifact.artifactRef &&
                    artifactContentSetSha256 ==
                    realization.artifact.contentSetSha256 &&
                    runtimeAdapterRef ==
                    realization.runtime.runtimeAdapterRef &&
                    runtimeClass ==
                    realization.runtime.runtimeClass &&
                    endpoint ==
                    realization.runtime.endpoint &&
                    requestModel ==
                    realization.runtime.requestModel,
            ) {
                "qualified runtime evidence does not match one exact Heretic runtime realization"
            }
        }
    }

    /**
     * Durable v2 Product binding.
     *
     * The pre-v2 fields remain present so legacy Homes can migrate without losing exact historical
     * coordinates. Their meaning is now explicit:
     * - pending: catalog projection for the selected model, [realizationRef] is null;
     * - bound: exact realization-owned coordinates, [realizationRef] is non-null.
     */
    @Serializable
    data class Value(
        val schemaVersion: String,
        val state: State,
        val homeRef: String,
        val companionLineageRef: String,
        val modelRef: String,
        val modelProfileRef: String,
        val realizationRef: String? = null,
        val artifactRef: String,
        val artifactContentSetSha256: String,
        val runtimeAdapterRef: String,
        val runtimeClass: String,
        val endpoint: String,
        val requestModel: String,
        val runtimeReceiptPath: String,
        val automaticFallback: Boolean,
        val automaticDownload: Boolean,
        val automaticActivation: Boolean,
    )

    /** Exact v1 shape retained only for read migration. */
    @Serializable
    data class LegacyValueV1(
        val schemaVersion: String,
        val state: State,
        val homeRef: String,
        val companionLineageRef: String,
        val modelRef: String,
        val modelProfileRef: String,
        val artifactRef: String,
        val artifactContentSetSha256: String,
        val runtimeAdapterRef: String,
        val runtimeClass: String,
        val endpoint: String,
        val requestModel: String,
        val runtimeReceiptPath: String,
        val automaticFallback: Boolean,
        val automaticDownload: Boolean,
        val automaticActivation: Boolean,
    )

    fun pending(
        homeIdentity: BindingScope,
        model: HereticModelCatalog.Entry = HereticModelCatalog.Default,
    ): Value =
        formPending(
            homeIdentity = homeIdentity,
            model = model,
        )

    /** The catalog model this value names. */
    fun selected(
        value: Value,
    ): HereticModelCatalog.Entry =
        HereticModelCatalog.requireEntry(
            value.modelRef,
        )

    /** The exact bound realization, or null while selection is pending / stale. */
    fun boundRealization(
        value: Value,
    ): HereticRuntimeRealization.Value? =
        value.realizationRef
            ?.let { ref ->
                HereticRuntimeRealization.Entries
                    .singleOrNull {
                        it.realizationRef ==
                            ref
                    }
            }
            ?.takeIf { realization ->
                value.state ==
                    State.BoundProductModel &&
                    boundFieldsMatch(
                        value = value,
                        realization = realization,
                    )
            }

    fun bound(
        homeIdentity: BindingScope,
        qualifiedRuntime: QualifiedRuntimeEvidence,
        runtimeReceiptPath: String =
            HereticProductIdentity.StableRuntimeReceiptRelativePath,
    ): Value {
        require(
            qualifiedRuntime.runtimeState ==
                HereticRuntimeState.Qualified,
        ) {
            "Heretic product model cannot bind without current qualified runtime evidence"
        }

        val realization =
            requireNotNull(
                HereticRuntimeRealization.Entries
                    .singleOrNull {
                        it.realizationRef ==
                            qualifiedRuntime.realizationRef
                    },
            ) {
                "Heretic product model binding requires one current runtime realization"
            }

        require(
            runtimeReceiptPath in
                admittedRuntimeReceiptPaths,
        ) {
            "Heretic product model binding requires one admitted runtime receipt owner"
        }

        return validate(
            value =
                Value(
                    schemaVersion = Schema,
                    state = State.BoundProductModel,
                    homeRef = homeIdentity.homeRef,
                    companionLineageRef = homeIdentity.companionLineageRef,
                    modelRef = qualifiedRuntime.modelRef,
                    modelProfileRef = qualifiedRuntime.modelProfileRef,
                    realizationRef = realization.realizationRef,
                    artifactRef = qualifiedRuntime.artifactRef,
                    artifactContentSetSha256 =
                        qualifiedRuntime.artifactContentSetSha256,
                    runtimeAdapterRef = qualifiedRuntime.runtimeAdapterRef,
                    runtimeClass = qualifiedRuntime.runtimeClass,
                    endpoint = qualifiedRuntime.endpoint,
                    requestModel = qualifiedRuntime.requestModel,
                    runtimeReceiptPath = runtimeReceiptPath,
                    automaticFallback = false,
                    automaticDownload = false,
                    automaticActivation = false,
                ),
            homeIdentity = homeIdentity,
        )
    }

    /**
     * Converts one v1 Home record into the current in-memory contract without writing anything.
     *
     * Pending v1 selections remain pending and do not acquire a realization claim. A bound v1 record
     * receives a realization ref only when its persisted coordinates identify exactly one current
     * realization. Stale historical coordinates remain readable at selection scope and are therefore
     * replaceable by explicit convergence instead of being invented as current.
     */
    fun migrateLegacy(
        value: LegacyValueV1,
    ): Value {
        require(
            value.schemaVersion ==
                LegacySchema,
        ) {
            "legacy Heretic model configuration schema drift"
        }

        val realizationRef =
            if (
                value.state ==
                    State.BoundProductModel
            ) {
                HereticRuntimeRealization.Entries
                    .singleOrNull { realization ->
                        legacyBoundFieldsMatch(
                            value = value,
                            realization = realization,
                        )
                    }
                    ?.realizationRef
            } else {
                null
            }

        return Value(
            schemaVersion = Schema,
            state = value.state,
            homeRef = value.homeRef,
            companionLineageRef = value.companionLineageRef,
            modelRef = value.modelRef,
            modelProfileRef = value.modelProfileRef,
            realizationRef = realizationRef,
            artifactRef = value.artifactRef,
            artifactContentSetSha256 =
                value.artifactContentSetSha256,
            runtimeAdapterRef = value.runtimeAdapterRef,
            runtimeClass = value.runtimeClass,
            endpoint = value.endpoint,
            requestModel = value.requestModel,
            runtimeReceiptPath = value.runtimeReceiptPath,
            automaticFallback = value.automaticFallback,
            automaticDownload = value.automaticDownload,
            automaticActivation = value.automaticActivation,
        )
    }

    /**
     * Scope/authority validation for convergence and explicit model replacement.
     *
     * Stale Product identity does not become current here. This proves only exact Home/Companion
     * scope and absence of hidden fallback/download/start authority.
     */
    fun validateSelectionScope(
        value: Value,
        homeIdentity: BindingScope,
    ): Value {
        require(
            value.schemaVersion ==
                Schema,
        ) {
            "Heretic model configuration schema drift"
        }
        require(
            value.homeRef ==
                homeIdentity.homeRef,
        ) {
            "Heretic model configuration Home identity drift"
        }
        require(
            value.companionLineageRef ==
                homeIdentity.companionLineageRef,
        ) {
            "Heretic model configuration Companion lineage drift"
        }
        require(
            !value.automaticFallback &&
                !value.automaticDownload &&
                !value.automaticActivation,
        ) {
            "Heretic model configuration cannot grant fallback/download/activation authority"
        }
        return value
    }

    fun productIdentityCurrent(
        value: Value,
    ): Boolean {
        val model =
            HereticModelCatalog.entry(
                value.modelRef,
            )
                ?: return false

        if (
            value.modelProfileRef !=
                model.identity.modelProfileRef
        ) {
            return false
        }

        return when (
            value.state
        ) {
            State.EstablishmentPending ->
                value.realizationRef ==
                    null &&
                    value.artifactRef ==
                    model.identity.artifactSetRef &&
                    value.artifactContentSetSha256 ==
                    model.identity.artifactContentSetSha256 &&
                    value.runtimeAdapterRef ==
                    model.identity.runtimeAdapterRef &&
                    value.runtimeClass ==
                    model.identity.runtimeClass &&
                    value.endpoint ==
                    model.identity.endpoint &&
                    value.requestModel ==
                    model.identity.requestModel &&
                    value.runtimeReceiptPath ==
                    HereticProductIdentity.StableRuntimeReceiptRelativePath

            State.BoundProductModel -> {
                val realization =
                    boundRealization(
                        value,
                    )
                        ?: return false

                value.runtimeReceiptPath in
                    admittedReceiptPathsFor(
                        realization,
                    )
            }
        }
    }

    fun validate(
        value: Value,
        homeIdentity: BindingScope,
    ): Value {
        val scoped =
            validateSelectionScope(
                value = value,
                homeIdentity = homeIdentity,
            )
        require(
            productIdentityCurrent(
                scoped,
            ),
        ) {
            "Heretic model configuration product identity drift"
        }
        return scoped
    }

    fun isBound(
        value: Value,
    ): Boolean =
        value.state ==
            State.BoundProductModel

    private val admittedRuntimeReceiptPaths: Set<String> =
        (
            HereticRuntimeRealization.Entries
                .map {
                    it.runtimeReceiptRelativePath
                } +
                HereticProductIdentity.StableRuntimeReceiptRelativePath
        )
            .toSet()

    private fun admittedReceiptPathsFor(
        realization: HereticRuntimeRealization.Value,
    ): Set<String> =
        buildSet {
            add(
                realization.runtimeReceiptRelativePath,
            )
            if (
                realization.target ==
                    HereticRuntimeRealization.MacArm64
            ) {
                add(
                    HereticProductIdentity.StableRuntimeReceiptRelativePath,
                )
            }
        }

    private fun formPending(
        homeIdentity: BindingScope,
        model: HereticModelCatalog.Entry,
    ): Value =
        validate(
            value =
                Value(
                    schemaVersion = Schema,
                    state = State.EstablishmentPending,
                    homeRef = homeIdentity.homeRef,
                    companionLineageRef = homeIdentity.companionLineageRef,
                    modelRef = model.identity.modelRef,
                    modelProfileRef = model.identity.modelProfileRef,
                    realizationRef = null,
                    artifactRef = model.identity.artifactSetRef,
                    artifactContentSetSha256 =
                        model.identity.artifactContentSetSha256,
                    runtimeAdapterRef = model.identity.runtimeAdapterRef,
                    runtimeClass = model.identity.runtimeClass,
                    endpoint = model.identity.endpoint,
                    requestModel = model.identity.requestModel,
                    runtimeReceiptPath =
                        HereticProductIdentity.StableRuntimeReceiptRelativePath,
                    automaticFallback = false,
                    automaticDownload = false,
                    automaticActivation = false,
                ),
            homeIdentity = homeIdentity,
        )

    private fun boundFieldsMatch(
        value: Value,
        realization: HereticRuntimeRealization.Value,
    ): Boolean =
        value.modelRef ==
            realization.modelRef &&
            value.modelProfileRef ==
            realization.modelProfileRef &&
            value.artifactRef ==
            realization.artifact.artifactRef &&
            value.artifactContentSetSha256 ==
            realization.artifact.contentSetSha256 &&
            value.runtimeAdapterRef ==
            realization.runtime.runtimeAdapterRef &&
            value.runtimeClass ==
            realization.runtime.runtimeClass &&
            value.endpoint ==
            realization.runtime.endpoint &&
            value.requestModel ==
            realization.runtime.requestModel

    private fun legacyBoundFieldsMatch(
        value: LegacyValueV1,
        realization: HereticRuntimeRealization.Value,
    ): Boolean =
        value.modelRef ==
            realization.modelRef &&
            value.modelProfileRef ==
            realization.modelProfileRef &&
            value.artifactRef ==
            realization.artifact.artifactRef &&
            value.artifactContentSetSha256 ==
            realization.artifact.contentSetSha256 &&
            value.runtimeAdapterRef ==
            realization.runtime.runtimeAdapterRef &&
            value.runtimeClass ==
            realization.runtime.runtimeClass &&
            value.endpoint ==
            realization.runtime.endpoint &&
            value.requestModel ==
            realization.runtime.requestModel &&
            value.runtimeReceiptPath in
            admittedReceiptPathsFor(
                realization,
            )
}

// [VXG RealForever]
