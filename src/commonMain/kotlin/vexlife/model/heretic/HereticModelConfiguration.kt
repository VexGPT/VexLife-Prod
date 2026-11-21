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
        val realizationRef: String =
            HereticModelCatalog
                .entry(
                    modelRef,
                )
                ?.let {
                    HereticRuntimeRealization.require(
                        modelRef = modelRef,
                        target = HereticRuntimeRealization.MacArm64,
                    )
                        .realizationRef
                }
                ?: error(
                    "Home-imported qualified runtime evidence must name its exact realizationRef",
                ),
        val modelProfileRef: String,
        val artifactRef: String,
        val artifactContentSetSha256: String,
        val runtimeAdapterRef: String,
        val runtimeClass: String,
        val endpoint: String,
        val requestModel: String,
        val qualification: HereticRuntimeQualification,
        /**
         * Exact lower runtime context-window bound when the qualified realization establishes one.
         *
         * Null means this evidence does not claim a runtime-specific lower bound. Product prompt
         * policy may still keep its own conservative ceiling; null never means unlimited.
         */
        val contextWindowTokens: Int? = null,
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
            requirePortableRef(modelRef, "modelRef")
            requirePortableRef(modelProfileRef, "modelProfileRef")
            requirePortableRef(artifactRef, "artifactRef")
            requireSha256(artifactContentSetSha256, "artifactContentSetSha256")
            requirePortableRef(runtimeAdapterRef, "runtimeAdapterRef")
            require(runtimeClass.isNotBlank() && endpoint.isNotBlank() && requestModel.isNotBlank()) {
                "qualified runtime evidence requires one complete model/runtime identity"
            }
            require(contextWindowTokens == null || contextWindowTokens > 0) {
                "qualified runtime context window must be absent or positive"
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
    fun selected(value: Value): HereticModelCatalog.Entry = HereticModelCatalog.requireEntry(value.modelRef)

    fun bound(
        homeIdentity: BindingScope,
        qualifiedRuntime: QualifiedRuntimeEvidence,
        runtimeReceiptPath: String =
            HereticProductIdentity.StableRuntimeReceiptRelativePath,
        model: HereticModelCatalog.Entry =
            HereticModelCatalog.requireEntry(
                qualifiedRuntime.modelRef,
            ),
    ): Value {
        require(
            qualifiedRuntime.runtimeState ==
                HereticRuntimeState.Qualified,
        ) {
            "Heretic product model cannot bind without current qualified runtime evidence"
        }

        val realization =
            requireNotNull(
                HereticRuntimeRealization.resolveByRef(
                    model = model,
                    realizationRef = qualifiedRuntime.realizationRef,
                ),
            ) {
                "Heretic product model binding requires one exact realization of the selected offering"
            }

        require(
            runtimeReceiptPath in
                admittedReceiptPathsFor(
                    realization,
                ),
        ) {
            "Heretic product model binding requires one admitted runtime receipt owner"
        }

        require(
            qualifiedRuntime.modelRef ==
                realization.modelRef &&
                qualifiedRuntime.modelProfileRef ==
                realization.modelProfileRef &&
                qualifiedRuntime.artifactRef ==
                realization.artifact.artifactRef &&
                qualifiedRuntime.artifactContentSetSha256 ==
                realization.artifact.contentSetSha256 &&
                qualifiedRuntime.runtimeAdapterRef ==
                realization.runtime.runtimeAdapterRef &&
                qualifiedRuntime.runtimeClass ==
                realization.runtime.runtimeClass &&
                qualifiedRuntime.endpoint ==
                realization.runtime.endpoint &&
                qualifiedRuntime.requestModel ==
                realization.runtime.requestModel,
        ) {
            "qualified runtime evidence does not match the selected offering's exact realization"
        }

        return validate(
            value =
                Value(
                    schemaVersion = Schema,
                    state = State.BoundProductModel,
                    homeRef = homeIdentity.homeRef,
                    companionLineageRef = homeIdentity.companionLineageRef,
                    modelRef = realization.modelRef,
                    modelProfileRef = realization.modelProfileRef,
                    realizationRef = realization.realizationRef,
                    artifactRef = realization.artifact.artifactRef,
                    artifactContentSetSha256 = realization.artifact.contentSetSha256,
                    runtimeAdapterRef = realization.runtime.runtimeAdapterRef,
                    runtimeClass = realization.runtime.runtimeClass,
                    endpoint = realization.runtime.endpoint,
                    requestModel = realization.runtime.requestModel,
                    runtimeReceiptPath = runtimeReceiptPath,
                    automaticFallback = false,
                    automaticDownload = false,
                    automaticActivation = false,
                ),
            homeIdentity = homeIdentity,
            model = model,
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
    ): Boolean =
        HereticModelCatalog.entry(value.modelRef)
            ?.let { model ->
                productIdentityCurrent(
                    value = value,
                    model = model,
                )
            }
            ?: false

    fun productIdentityCurrent(
        value: Value,
        model: HereticModelCatalog.Entry,
    ): Boolean {
        if (
            value.modelRef !=
                model.modelRef ||
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
                val realizationRef =
                    value.realizationRef
                        ?: return false
                val realization =
                    HereticRuntimeRealization.resolveByRef(
                        model = model,
                        realizationRef = realizationRef,
                    )
                        ?: return false
                boundFieldsMatch(
                    value = value,
                    realization = realization,
                ) &&
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
        val model =
            requireNotNull(
                HereticModelCatalog.entry(
                    value.modelRef,
                ),
            ) {
                "Heretic model configuration references an unoffered Product-catalog model"
            }
        return validate(
            value = value,
            homeIdentity = homeIdentity,
            model = model,
        )
    }

    fun validate(
        value: Value,
        homeIdentity: BindingScope,
        model: HereticModelCatalog.Entry,
    ): Value {
        val scoped =
            validateSelectionScope(
                value = value,
                homeIdentity = homeIdentity,
            )
        require(
            productIdentityCurrent(
                value = scoped,
                model = model,
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

    fun migrateLegacy(
        value: LegacyValueV1,
        model: HereticModelCatalog.Entry,
    ): Value {
        require(
            value.schemaVersion ==
                LegacySchema,
        ) {
            "legacy Heretic model configuration schema drift"
        }
        require(
            value.modelRef ==
                model.modelRef,
        ) {
            "legacy Heretic model configuration does not match the current Home offering"
        }

        val realizationRef =
            if (
                value.state ==
                    State.BoundProductModel
            ) {
                listOfNotNull(
                    HereticRuntimeRealization.resolve(
                        model = model,
                        target = HereticRuntimeRealization.MacArm64,
                    ),
                    HereticRuntimeRealization.resolve(
                        model = model,
                        target = HereticRuntimeRealization.WindowsX64,
                    ),
                )
                    .singleOrNull {
                        legacyBoundFieldsMatch(
                            value = value,
                            realization = it,
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
            artifactContentSetSha256 = value.artifactContentSetSha256,
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
                    artifactContentSetSha256 = model.identity.artifactContentSetSha256,
                    runtimeAdapterRef = model.identity.runtimeAdapterRef,
                    runtimeClass = model.identity.runtimeClass,
                    endpoint = model.identity.endpoint,
                    requestModel = model.identity.requestModel,
                    runtimeReceiptPath = HereticProductIdentity.StableRuntimeReceiptRelativePath,
                    automaticFallback = false,
                    automaticDownload = false,
                    automaticActivation = false,
                ),
            homeIdentity = homeIdentity,
            model = model,
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
