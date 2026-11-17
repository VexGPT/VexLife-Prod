package vexlife.model.heretic

import kotlinx.serialization.Serializable

/**
 * Home + Companion-lineage scoped selection/binding relationship for one catalog Heretic model.
 *
 * [BindingScope] carries refs supplied by those rightful owners; this package does not define Home
 * or Companion lineage semantics. Selecting a model is not authority to download, activate,
 * replace another model, or start a process. Those effects belong to later runtime/platform owners.
 *
 * The value names exactly one [HereticModelCatalog] entry. Its schema is unchanged from the
 * single-model era, so a Home bound to the certified 27B reads exactly as it always did; a Home that
 * selected another catalog model carries that model's identity in the same fields.
 */
object HereticModelConfiguration {
    const val Schema =
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
     * Transient current-runtime truth required to admit BOUND_HERETIC_PRODUCT_MODEL.
     *
     * The persistent runtime owner forms this from one exact qualified receipt. Configuration
     * consumes the evidence but does not persist or become owner of runtime/process truth.
     */
    @ConsistentCopyVisibility
    data class QualifiedRuntimeEvidence internal constructor(
        val runtimeReceiptRef: String,
        val runtimeState: HereticRuntimeState,
        val modelRef: String,
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
            require(
                runtimeState ==
                    HereticRuntimeState.Qualified,
            ) {
                "Heretic model binding requires a qualified runtime"
            }
            val model =
                HereticModelCatalog.entry(
                    modelRef,
                )
            require(
                model != null &&
                    modelProfileRef ==
                    model.identity.modelProfileRef &&
                    HereticRuntimeRealization.admits(
                        model = model,
                        artifactRef = artifactRef,
                        artifactContentSetSha256 = artifactContentSetSha256,
                        runtimeAdapterRef = runtimeAdapterRef,
                        runtimeClass = runtimeClass,
                        endpoint = endpoint,
                        requestModel = requestModel,
                    ),
            ) {
                "qualified runtime evidence does not match one admitted realization of an offered Heretic model"
            }
        }
    }

    @Serializable
    data class Value(
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
        form(
            homeIdentity = homeIdentity,
            state = State.EstablishmentPending,
            model = model,
        )

    /** The catalog model this value names. */
    fun selected(value: Value): HereticModelCatalog.Entry = HereticModelCatalog.requireEntry(value.modelRef)

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

        require(
            runtimeReceiptPath in
                admittedRuntimeReceiptPaths,
        ) {
            "Heretic product model binding requires one admitted runtime receipt owner"
        }

        return form(
            homeIdentity = homeIdentity,
            state = State.BoundProductModel,
            runtimeReceiptPath = runtimeReceiptPath,
            model = HereticModelCatalog.requireEntry(qualifiedRuntime.modelRef),
            qualifiedRuntime = qualifiedRuntime,
        )
    }

    /**
     * Scope/authority validation for convergence and explicit model replacement.
     *
     * A stale Product identity does not become current here. This proves only that the record
     * belongs to this exact Home/Companion scope and carries no hidden fallback/download/start
     * authority.
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
        val identity =
            HereticModelCatalog.entry(
                value.modelRef,
            )
                ?.identity
                ?: return false

        return value.modelProfileRef ==
            identity.modelProfileRef &&
            HereticRuntimeRealization.admits(
                model =
                    requireNotNull(
                        HereticModelCatalog.entry(
                            value.modelRef,
                        ),
                    ),
                artifactRef = value.artifactRef,
                artifactContentSetSha256 = value.artifactContentSetSha256,
                runtimeAdapterRef = value.runtimeAdapterRef,
                runtimeClass = value.runtimeClass,
                endpoint = value.endpoint,
                requestModel = value.requestModel,
            ) &&
            value.runtimeReceiptPath in
            admittedRuntimeReceiptPaths
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

    private val admittedRuntimeReceiptPaths =
        setOf(
            HereticProductIdentity.StableRuntimeReceiptRelativePath,
            HereticProductIdentity.ProductRuntimeReceiptRelativePath,
            HereticRuntimeRealization.Windows4BPatientZeroReceiptRelativePath,
        )

    private fun form(
        homeIdentity: BindingScope,
        state: State,
        runtimeReceiptPath: String =
            HereticProductIdentity.StableRuntimeReceiptRelativePath,
        model: HereticModelCatalog.Entry = HereticModelCatalog.Default,
        qualifiedRuntime: QualifiedRuntimeEvidence? =
            null,
    ): Value {
        qualifiedRuntime?.let {
            require(
                it.modelRef ==
                    model.modelRef &&
                    it.modelProfileRef ==
                    model.identity.modelProfileRef,
            ) {
                "bound Heretic runtime realization must serve the selected Product model"
            }
        }

        return validate(
            value =
                Value(
                    schemaVersion = Schema,
                    state = state,
                    homeRef = homeIdentity.homeRef,
                    companionLineageRef = homeIdentity.companionLineageRef,
                    modelRef = model.identity.modelRef,
                    modelProfileRef = model.identity.modelProfileRef,
                    artifactRef =
                        qualifiedRuntime
                            ?.artifactRef
                            ?: model.identity.artifactSetRef,
                    artifactContentSetSha256 =
                        qualifiedRuntime
                            ?.artifactContentSetSha256
                            ?: model.identity.artifactContentSetSha256,
                    runtimeAdapterRef =
                        qualifiedRuntime
                            ?.runtimeAdapterRef
                            ?: model.identity.runtimeAdapterRef,
                    runtimeClass =
                        qualifiedRuntime
                            ?.runtimeClass
                            ?: model.identity.runtimeClass,
                    endpoint =
                        qualifiedRuntime
                            ?.endpoint
                            ?: model.identity.endpoint,
                    requestModel =
                        qualifiedRuntime
                            ?.requestModel
                            ?: model.identity.requestModel,
                    runtimeReceiptPath = runtimeReceiptPath,
                    automaticFallback = false,
                    automaticDownload = false,
                    automaticActivation = false,
                ),
            homeIdentity = homeIdentity,
        )
    }
}

// [VXG RealForever]
