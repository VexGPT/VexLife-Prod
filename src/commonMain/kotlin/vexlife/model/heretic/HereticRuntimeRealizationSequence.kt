package vexlife.model.heretic

/**
 * Ordered, platform-neutral realization recipe.
 *
 * [HereticRuntimeRealization] answers which exact dependencies belong to one model/target pair.
 * This owner answers the next question a reader otherwise has to reconstruct from Mac/Windows
 * implementation classes: "in what order does one declared realization become a Product runtime?"
 *
 * Every realization has the same stages. Platform differences appear only as the declared ref at a
 * stage or an explicit NOT_REQUIRED reason. Execution remains with the rightful platform/runtime
 * owners; this source performs no effects.
 */
object HereticRuntimeRealizationSequence {
    const val Schema =
        "vexlife-prod.heretic-runtime-realization-sequence/v1"

    enum class Stage {
        PRODUCT_MODEL_SELECTION,
        REALIZATION_RESOLUTION,
        ARTIFACT,
        ENVIRONMENT,
        RUNTIME_BINARY,
        PROCESS,
        QUALIFICATION_HEALTH,
        QUALIFICATION_MODEL_EXPOSURE,
        QUALIFICATION_COMPLETION,
        RECEIPT,
        PRODUCT_BINDING,
        RUNTIME_SESSION,
    }

    enum class RequirementClass {
        REQUIRED,
        NOT_REQUIRED,
        INTRINSIC,
    }

    data class Step(
        val ordinal: Int,
        val stage: Stage,
        val requirementClass: RequirementClass,
        val ref: String,
        val ownerRef: String? = null,
    )

    data class Plan(
        val schemaVersion: String,
        val realizationRef: String,
        val modelRef: String,
        val targetRef: String,
        val steps: List<Step>,
        val externalEffectsPerformed: Boolean,
    ) {
        init {
            require(schemaVersion == Schema)
            require(steps.map(Step::ordinal) == steps.indices.map { it + 1 })
            require(steps.map(Step::stage) == Stage.entries)
            require(!externalEffectsPerformed)
        }
    }

    fun plan(
        realization: HereticRuntimeRealization.Value,
    ): Plan {
        val qualification =
            realization.dependencies.qualification
        val process =
            realization.dependencies.process

        val steps =
            listOf(
                intrinsic(
                    Stage.PRODUCT_MODEL_SELECTION,
                    realization.modelRef,
                ),
                intrinsic(
                    Stage.REALIZATION_RESOLUTION,
                    realization.realizationRef,
                ),
                dependency(
                    Stage.ARTIFACT,
                    realization.dependencies.artifact,
                    requiredRef = { it.artifactRef },
                ),
                dependency(
                    Stage.ENVIRONMENT,
                    realization.dependencies.environment,
                    requiredRef = { it.environmentRef },
                ),
                dependency(
                    Stage.RUNTIME_BINARY,
                    realization.dependencies.runtimeBinary,
                    requiredRef = { it.binaryRef },
                ),
                dependency(
                    Stage.PROCESS,
                    process,
                    requiredRef = { it.processSubstrateRef },
                    ownerRef = { it.lifetimeOwnerRef },
                ),
                dependency(
                    Stage.QUALIFICATION_HEALTH,
                    qualification,
                    requiredRef = { it.qualificationRef + ":health:" + it.healthPath },
                ),
                dependency(
                    Stage.QUALIFICATION_MODEL_EXPOSURE,
                    qualification,
                    requiredRef = {
                        it.qualificationRef +
                            ":models:" +
                            it.modelsPath +
                            ":" +
                            it.modelExposure.name
                    },
                ),
                dependency(
                    Stage.QUALIFICATION_COMPLETION,
                    qualification,
                    requiredRef = {
                        it.qualificationRef +
                            ":completion:" +
                            it.completionPath
                    },
                ),
                intrinsic(
                    Stage.RECEIPT,
                    realization.runtimeReceiptRelativePath,
                    ownerRef =
                        when (process) {
                            is HereticRuntimeRealization.Requirement.Required ->
                                process.value.lifetimeOwnerRef
                            is HereticRuntimeRealization.Requirement.NotRequired ->
                                null
                        },
                ),
                intrinsic(
                    Stage.PRODUCT_BINDING,
                    HereticModelConfiguration.Schema,
                ),
                intrinsic(
                    Stage.RUNTIME_SESSION,
                    "vexlife-prod.runtime-session",
                ),
            )
                .mapIndexed {
                    index,
                    step,
                    ->
                    step.copy(
                        ordinal = index + 1,
                    )
                }

        return Plan(
            schemaVersion = Schema,
            realizationRef = realization.realizationRef,
            modelRef = realization.modelRef,
            targetRef = realization.target.targetRef,
            steps = steps,
            externalEffectsPerformed = false,
        )
    }

    fun plan(
        realizationRef: String,
    ): Plan =
        plan(
            HereticRuntimeRealization.require(
                realizationRef,
            ),
        )

    private fun intrinsic(
        stage: Stage,
        ref: String,
        ownerRef: String? = null,
    ): Step =
        Step(
            ordinal = 0,
            stage = stage,
            requirementClass = RequirementClass.INTRINSIC,
            ref = ref,
            ownerRef = ownerRef,
        )

    private fun <T> dependency(
        stage: Stage,
        requirement: HereticRuntimeRealization.Requirement<T>,
        requiredRef: (T) -> String,
        ownerRef: ((T) -> String?)? = null,
    ): Step =
        when (requirement) {
            is HereticRuntimeRealization.Requirement.Required ->
                Step(
                    ordinal = 0,
                    stage = stage,
                    requirementClass = RequirementClass.REQUIRED,
                    ref = requiredRef(requirement.value),
                    ownerRef = ownerRef?.invoke(requirement.value),
                )

            is HereticRuntimeRealization.Requirement.NotRequired ->
                Step(
                    ordinal = 0,
                    stage = stage,
                    requirementClass = RequirementClass.NOT_REQUIRED,
                    ref = requirement.reasonRef,
                )
        }
}

// [VXG RealForever]
