package vexlife.model.heretic

import vexlife.semantic.VexFamilyContract

/**
 * Semantic owner for the question:
 *
 * "What does EnsureAvailable mean for this exact declared Heretic runtime realization?"
 *
 * Static model/platform dependency relationships are owned by [HereticRuntimeRealization]. This
 * Family owns only dynamic State / Control / Effect meaning against one resolved realizationRef.
 * Runtime/process owners execute the emitted Effect; they do not rediscover which realization was
 * intended.
 */
object HereticRuntimeAvailability :
    VexFamilyContract<
        HereticRuntimeAvailability,
        HereticRuntimeAvailability.State,
        HereticRuntimeAvailability.Control,
        HereticRuntimeAvailability.Effect
    >() {

    enum class Source {
        BorrowedStableVexLife,
        VexLifeProdOwned,
    }

    enum class ProductOwnedMode {
        ObservationOnly,
        AcquisitionAllowed,
    }

    sealed interface State :
        VexFamilyContract.State<HereticRuntimeAvailability> {

        data class StableBorrowable(
            val evidence: HereticModelConfiguration.QualifiedRuntimeEvidence,
            val runtimeReceiptPath: String,
        ) : State {
            init {
                require(
                    runtimeReceiptPath ==
                        HereticProductIdentity.StableRuntimeReceiptRelativePath,
                ) {
                    "borrowable stable Heretic runtime must retain the stable VexLife receipt coordinate"
                }
            }
        }

        data object StablePredecessorUnavailable :
            State

        data class Qualified(
            val source: Source,
            val evidence: HereticModelConfiguration.QualifiedRuntimeEvidence,
            val runtimeReceiptPath: String,
        ) : State {
            init {
                val expectedPath =
                    when (source) {
                        Source.BorrowedStableVexLife ->
                            HereticProductIdentity.StableRuntimeReceiptRelativePath

                        Source.VexLifeProdOwned ->
                            HereticRuntimeRealization
                                .require(evidence.realizationRef)
                                .runtimeReceiptRelativePath
                    }

                require(runtimeReceiptPath == expectedPath) {
                    "qualified Heretic runtime source must retain its rightful receipt owner coordinate"
                }
            }
        }
    }

    sealed interface Control :
        VexFamilyContract.Control<HereticRuntimeAvailability> {

        data class EnsureAvailable(
            val realizationRef: String,
            val productOwnedMode: ProductOwnedMode,
        ) : Control {
            init {
                HereticRuntimeRealization.require(realizationRef)
            }
        }
    }

    sealed interface Effect :
        VexFamilyContract.Effect<HereticRuntimeAvailability> {

        data class EnsureProductOwnedRuntime(
            val realizationRef: String,
        ) : Effect

        data class ObserveCurrentProductOwnedRuntime(
            val realizationRef: String,
        ) : Effect
    }

    override fun act(
        currentState: State,
        control: Control,
    ): Act<
        HereticRuntimeAvailability,
        State,
        Effect
    > =
        when (control) {
            is Control.EnsureAvailable -> {
                val requested =
                    HereticRuntimeRealization.require(
                        control.realizationRef,
                    )

                when (currentState) {
                    is State.StableBorrowable ->
                        if (
                            currentState.evidence.realizationRef ==
                            requested.realizationRef
                        ) {
                            admit(
                                State.Qualified(
                                    source = Source.BorrowedStableVexLife,
                                    evidence = currentState.evidence,
                                    runtimeReceiptPath = currentState.runtimeReceiptPath,
                                ),
                            )
                        } else {
                            stay(
                                effect = effectFor(control),
                            )
                        }

                    State.StablePredecessorUnavailable ->
                        stay(
                            effect = effectFor(control),
                        )

                    is State.Qualified ->
                        if (
                            currentState.evidence.realizationRef ==
                            requested.realizationRef
                        ) {
                            stay()
                        } else {
                            stay(
                                effect = effectFor(control),
                            )
                        }
                }
            }
        }

    fun interpretProductOwnedQualified(
        evidence: HereticModelConfiguration.QualifiedRuntimeEvidence,
    ): State.Qualified {
        val realization =
            HereticRuntimeRealization.require(
                evidence.realizationRef,
            )

        return State.Qualified(
            source = Source.VexLifeProdOwned,
            evidence = evidence,
            runtimeReceiptPath = realization.runtimeReceiptRelativePath,
        )
    }

    private fun effectFor(
        control: Control.EnsureAvailable,
    ): Effect =
        when (control.productOwnedMode) {
            ProductOwnedMode.AcquisitionAllowed ->
                Effect.EnsureProductOwnedRuntime(
                    realizationRef = control.realizationRef,
                )

            ProductOwnedMode.ObservationOnly ->
                Effect.ObserveCurrentProductOwnedRuntime(
                    realizationRef = control.realizationRef,
                )
        }
}

// [VXG RealForever]
