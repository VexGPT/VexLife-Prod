package vexlife.model.heretic

import kotlinx.serialization.Serializable

/**
 * Home + Companion-lineage scoped selection/binding relationship for one catalog Heretic model.
 *
 * Model selection and runtime realization are distinct. A pending selection names the Product model
 * only; a bound selection also names the exact source-owned runtime realization that qualified.
 *
 * v1 records created before realizationRef remain readable. Their only admitted bound compatibility
 * is the source-known Mac relationship resolved by modelRef -> exact Mac target; platform/runtime
 * identity is never inferred by inspecting artifact or class strings.
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
            requirePortableRef(homeRef, "homeRef")
            requirePortableRef(companionLineageRef, "companionLineageRef")
        }
    }

    /**
     * Transient current-runtime truth required to admit BOUND_HERETIC_PRODUCT_MODEL.
     *
     * The runtime owner supplies the realizationRef explicitly. Configuration validates that exact
     * map entry; it does not identify a realization by comparing runtime/artifact fields.
     */
    @ConsistentCopyVisibility
    data class QualifiedRuntimeEvidence internal constructor(
        val realizationRef: String,
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
            HereticModelConfiguration.requireCurrentQualifiedRuntime(
                this,
            )
        }
    }

    /**
     * Revalidates qualified runtime truth against the current source-owned realization topology.
     *
     * Shared consumers call this instead of comparing evidence to one catalog entry's historical
     * platform fields. The model catalog answers what the person chose; the realization map answers
     * how that model is implemented on one exact target.
     */
    internal fun requireCurrentQualifiedRuntime(
        evidence: QualifiedRuntimeEvidence,
    ): QualifiedRuntimeEvidence {
        requirePortableRef(evidence.realizationRef, "realizationRef")
        requirePortableRef(evidence.runtimeReceiptRef, "runtimeReceiptRef")
        require(
            evidence.runtimeState == HereticRuntimeState.Qualified,
        ) {
            "Heretic model binding requires a qualified runtime"
        }

        val realization =
            HereticRuntimeRealization.require(
                evidence.realizationRef,
            )
        val model =
            HereticModelCatalog.entry(
                evidence.modelRef,
            )
        val artifact = realization.artifact
        val runtime = realization.runtime

        require(
            model != null &&
                realization.modelRef == evidence.modelRef &&
                realization.modelProfileRef == evidence.modelProfileRef &&
                model.identity.modelProfileRef == evidence.modelProfileRef &&
                artifact.artifactRef == evidence.artifactRef &&
                artifact.contentSetSha256 == evidence.artifactContentSetSha256 &&
                runtime.runtimeAdapterRef == evidence.runtimeAdapterRef &&
                runtime.runtimeClass == evidence.runtimeClass &&
                runtime.endpoint == evidence.endpoint &&
                runtime.requestModel == evidence.requestModel,
        ) {
            "qualified runtime evidence does not match its declared Heretic realization"
        }

        return evidence
    }

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

    fun pending(
        homeIdentity: BindingScope,
        model: HereticModelCatalog.Entry = HereticModelCatalog.Default,
    ): Value =
        form(
            homeIdentity = homeIdentity,
            state = State.EstablishmentPending,
            model = model,
        )

    fun selected(
        value: Value,
    ): HereticModelCatalog.Entry =
        HereticModelCatalog.requireEntry(
            value.modelRef,
        )

    fun bound(
        homeIdentity: BindingScope,
        qualifiedRuntime: QualifiedRuntimeEvidence,
        runtimeReceiptPath: String =
            HereticProductIdentity.StableRuntimeReceiptRelativePath,
    ): Value {
        require(
            runtimeReceiptPath in admittedRuntimeReceiptPaths,
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

    fun validateSelectionScope(
        value: Value,
        homeIdentity: BindingScope,
    ): Value {
        require(value.schemaVersion == Schema) {
            "Heretic model configuration schema drift"
        }
        require(value.homeRef == homeIdentity.homeRef) {
            "Heretic model configuration Home identity drift"
        }
        require(value.companionLineageRef == homeIdentity.companionLineageRef) {
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

        if (value.modelProfileRef != model.identity.modelProfileRef) {
            return false
        }

        if (value.state == State.EstablishmentPending) {
            // Pending is a Product model choice, not a platform/runtime choice. Its legacy schema
            // fields remain non-authoritative for execution, but they must still describe the
            // selected catalog model exactly so stale/cross-model metadata cannot masquerade as
            // one current Product choice.
            val identity = model.identity
            return value.realizationRef == null &&
                value.artifactRef == identity.artifactSetRef &&
                value.artifactContentSetSha256 == identity.artifactContentSetSha256 &&
                value.runtimeAdapterRef == identity.runtimeAdapterRef &&
                value.runtimeClass == identity.runtimeClass &&
                value.endpoint == identity.endpoint &&
                value.requestModel == identity.requestModel &&
                value.runtimeReceiptPath in admittedRuntimeReceiptPaths
        }

        val realization =
            if (value.realizationRef == null) {
                // Source-known v1 compatibility only; no field-based platform inference.
                HereticRuntimeRealization.legacyMacV1(
                    value.modelRef,
                )
            } else {
                runCatching {
                        HereticRuntimeRealization.require(
                            value.realizationRef,
                        )
                    }
                    .getOrNull()
                    ?: return false
            }

        val artifact = realization.artifact
        val runtime = realization.runtime

        val exactRealization =
            realization.modelRef == value.modelRef &&
                realization.modelProfileRef == value.modelProfileRef &&
                artifact.artifactRef == value.artifactRef &&
                artifact.contentSetSha256 == value.artifactContentSetSha256 &&
                runtime.runtimeAdapterRef == value.runtimeAdapterRef &&
                runtime.runtimeClass == value.runtimeClass &&
                runtime.endpoint == value.endpoint &&
                runtime.requestModel == value.requestModel

        val borrowedStableMacReceipt =
            value.runtimeReceiptPath ==
                HereticProductIdentity.StableRuntimeReceiptRelativePath &&
                realization.target == HereticRuntimeRealization.MacArm64 &&
                realization.modelRef == HereticProductIdentity.ModelRef

        val receiptCurrent =
            value.runtimeReceiptPath == realization.runtimeReceiptRelativePath ||
                borrowedStableMacReceipt

        return exactRealization && receiptCurrent
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
        require(productIdentityCurrent(scoped)) {
            "Heretic model configuration product identity drift"
        }
        return scoped
    }

    fun isBound(
        value: Value,
    ): Boolean =
        value.state == State.BoundProductModel

    private val admittedRuntimeReceiptPaths =
        HereticRuntimeRealization.receiptPaths() +
            HereticProductIdentity.StableRuntimeReceiptRelativePath

    private fun form(
        homeIdentity: BindingScope,
        state: State,
        runtimeReceiptPath: String =
            HereticProductIdentity.StableRuntimeReceiptRelativePath,
        model: HereticModelCatalog.Entry = HereticModelCatalog.Default,
        qualifiedRuntime: QualifiedRuntimeEvidence? = null,
    ): Value {
        qualifiedRuntime?.let {
            require(
                it.modelRef == model.modelRef &&
                    it.modelProfileRef == model.identity.modelProfileRef,
            ) {
                "bound Heretic runtime realization must serve the selected Product model"
            }
        }

        val value =
            Value(
                schemaVersion = Schema,
                state = state,
                homeRef = homeIdentity.homeRef,
                companionLineageRef = homeIdentity.companionLineageRef,
                modelRef = model.identity.modelRef,
                modelProfileRef = model.identity.modelProfileRef,
                realizationRef = qualifiedRuntime?.realizationRef,
                // v1 keeps these fields present for schema compatibility. While pending, they are
                // non-authoritative selection metadata. Bound values come from the exact resolved
                // realization evidence.
                artifactRef =
                    qualifiedRuntime?.artifactRef
                        ?: model.identity.artifactSetRef,
                artifactContentSetSha256 =
                    qualifiedRuntime?.artifactContentSetSha256
                        ?: model.identity.artifactContentSetSha256,
                runtimeAdapterRef =
                    qualifiedRuntime?.runtimeAdapterRef
                        ?: model.identity.runtimeAdapterRef,
                runtimeClass =
                    qualifiedRuntime?.runtimeClass
                        ?: model.identity.runtimeClass,
                endpoint =
                    qualifiedRuntime?.endpoint
                        ?: model.identity.endpoint,
                requestModel =
                    qualifiedRuntime?.requestModel
                        ?: model.identity.requestModel,
                runtimeReceiptPath = runtimeReceiptPath,
                automaticFallback = false,
                automaticDownload = false,
                automaticActivation = false,
            )

        return validate(
            value = value,
            homeIdentity = homeIdentity,
        )
    }
}

// [VXG RealForever]
