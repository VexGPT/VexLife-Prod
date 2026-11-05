package vexlife.model.heretic

/**
 * Home-scoped selection/binding relationship for the certified Heretic product model.
 *
 * Selecting Heretic is not authority to download, activate, replace another model, or start a
 * process. Those effects belong to later runtime/platform owners.
 */
object HereticModelConfiguration {
    const val Schema =
        "vexlife.heretic-model-configuration/v1"

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

    data class HomeIdentity(
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
        homeIdentity: HomeIdentity,
    ): Value =
        form(
            homeIdentity = homeIdentity,
            state = State.EstablishmentPending,
        )

    fun bound(
        homeIdentity: HomeIdentity,
    ): Value =
        form(
            homeIdentity = homeIdentity,
            state = State.BoundProductModel,
        )

    fun validate(
        value: Value,
        homeIdentity: HomeIdentity,
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
        homeIdentity: HomeIdentity,
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
