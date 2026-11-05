package vexlife.model.heretic

import kotlinx.serialization.Serializable

/**
 * Home + Companion-lineage scoped selection/binding relationship for the certified Heretic model.
 *
 * [BindingScope] carries refs supplied by those rightful owners; this package does not define Home
 * or Companion lineage semantics. Selecting Heretic is not authority to download, activate,
 * replace another model, or start a process. Those effects belong to later runtime/platform owners.
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
            require(
                modelRef ==
                    HereticProductIdentity.ModelRef &&
                    modelProfileRef ==
                    HereticProductIdentity.ModelProfileRef &&
                    artifactRef ==
                    HereticProductIdentity.ArtifactSetRef &&
                    artifactContentSetSha256 ==
                    HereticProductIdentity.ArtifactContentSetSha256 &&
                    runtimeAdapterRef ==
                    HereticProductIdentity.RuntimeAdapterRef &&
                    runtimeClass ==
                    HereticProductIdentity.RuntimeClass &&
                    endpoint ==
                    HereticProductIdentity.Endpoint &&
                    requestModel ==
                    HereticProductIdentity.RequestModel,
            ) {
                "qualified runtime evidence does not match the certified Heretic product identity"
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
    ): Value =
        form(
            homeIdentity = homeIdentity,
            state = State.EstablishmentPending,
        )

    fun bound(
        homeIdentity: BindingScope,
        qualifiedRuntime: QualifiedRuntimeEvidence,
    ): Value {
        require(
            qualifiedRuntime.runtimeState ==
                HereticRuntimeState.Qualified,
        ) {
            "Heretic product model cannot bind without current qualified runtime evidence"
        }

        return form(
            homeIdentity = homeIdentity,
            state = State.BoundProductModel,
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
        require(
            value.modelRef ==
                HereticProductIdentity.ModelRef &&
                value.modelProfileRef ==
                HereticProductIdentity.ModelProfileRef &&
                value.artifactRef ==
                HereticProductIdentity.ArtifactSetRef &&
                value.artifactContentSetSha256 ==
                HereticProductIdentity.ArtifactContentSetSha256 &&
                value.runtimeAdapterRef ==
                HereticProductIdentity.RuntimeAdapterRef &&
                value.runtimeClass ==
                HereticProductIdentity.RuntimeClass &&
                value.endpoint ==
                HereticProductIdentity.Endpoint &&
                value.requestModel ==
                HereticProductIdentity.RequestModel &&
                value.runtimeReceiptPath ==
                HereticProductIdentity.RuntimeReceiptRelativePath,
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

    private fun form(
        homeIdentity: BindingScope,
        state: State,
    ): Value =
        validate(
            value =
                Value(
                    schemaVersion = Schema,
                    state = state,
                    homeRef = homeIdentity.homeRef,
                    companionLineageRef = homeIdentity.companionLineageRef,
                    modelRef = HereticProductIdentity.ModelRef,
                    modelProfileRef = HereticProductIdentity.ModelProfileRef,
                    artifactRef = HereticProductIdentity.ArtifactSetRef,
                    artifactContentSetSha256 = HereticProductIdentity.ArtifactContentSetSha256,
                    runtimeAdapterRef = HereticProductIdentity.RuntimeAdapterRef,
                    runtimeClass = HereticProductIdentity.RuntimeClass,
                    endpoint = HereticProductIdentity.Endpoint,
                    requestModel = HereticProductIdentity.RequestModel,
                    runtimeReceiptPath = HereticProductIdentity.RuntimeReceiptRelativePath,
                    automaticFallback = false,
                    automaticDownload = false,
                    automaticActivation = false,
                ),
            homeIdentity = homeIdentity,
        )
}

// [VXG RealForever]
