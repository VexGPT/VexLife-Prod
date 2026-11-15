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
            val identity = HereticModelCatalog.entry(modelRef)?.identity
            require(
                identity != null &&
                    modelProfileRef ==
                    identity.modelProfileRef &&
                    artifactRef ==
                    identity.artifactSetRef &&
                    artifactContentSetSha256 ==
                    identity.artifactContentSetSha256 &&
                    runtimeAdapterRef ==
                    identity.runtimeAdapterRef &&
                    runtimeClass ==
                    identity.runtimeClass &&
                    endpoint ==
                    identity.endpoint &&
                    requestModel ==
                    identity.requestModel,
            ) {
                "qualified runtime evidence does not match one offered Heretic model identity"
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
        )
    }

    fun validate(
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
        val identity = HereticModelCatalog.entry(value.modelRef)?.identity
        require(
            identity != null &&
                value.modelProfileRef ==
                identity.modelProfileRef &&
                value.artifactRef ==
                identity.artifactSetRef &&
                value.artifactContentSetSha256 ==
                identity.artifactContentSetSha256 &&
                value.runtimeAdapterRef ==
                identity.runtimeAdapterRef &&
                value.runtimeClass ==
                identity.runtimeClass &&
                value.endpoint ==
                identity.endpoint &&
                value.requestModel ==
                identity.requestModel &&
                value.runtimeReceiptPath in
                admittedRuntimeReceiptPaths,
        ) {
            "Heretic model configuration product identity drift"
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

    fun isBound(
        value: Value,
    ): Boolean =
        value.state ==
            State.BoundProductModel

    private val admittedRuntimeReceiptPaths =
        setOf(
            HereticProductIdentity.StableRuntimeReceiptRelativePath,
            HereticProductIdentity.ProductRuntimeReceiptRelativePath,
        )

    private fun form(
        homeIdentity: BindingScope,
        state: State,
        runtimeReceiptPath: String =
            HereticProductIdentity.StableRuntimeReceiptRelativePath,
        model: HereticModelCatalog.Entry = HereticModelCatalog.Default,
    ): Value =
        validate(
            value =
                Value(
                    schemaVersion = Schema,
                    state = state,
                    homeRef = homeIdentity.homeRef,
                    companionLineageRef = homeIdentity.companionLineageRef,
                    modelRef = model.identity.modelRef,
                    modelProfileRef = model.identity.modelProfileRef,
                    artifactRef = model.identity.artifactSetRef,
                    artifactContentSetSha256 = model.identity.artifactContentSetSha256,
                    runtimeAdapterRef = model.identity.runtimeAdapterRef,
                    runtimeClass = model.identity.runtimeClass,
                    endpoint = model.identity.endpoint,
                    requestModel = model.identity.requestModel,
                    runtimeReceiptPath = runtimeReceiptPath,
                    automaticFallback = false,
                    automaticDownload = false,
                    automaticActivation = false,
                ),
            homeIdentity = homeIdentity,
        )
}

// [VXG RealForever]
