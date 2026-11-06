package vexlife.model.heretic

import vexlife.semantic.VexFamilyContract

/**
 * Semantic owner for the question:
 *
 * "What does EnsureAvailable mean against the exact current Heretic runtime relationship?"
 *
 * This Family does not observe processes, read receipts, start MLX, or qualify a runtime. Those
 * facts/effects remain with StableVexLifeHereticRuntimeCompatibility and HereticPersistentRuntime.
 * It owns only the cross-owner decision that was previously implicit in LocalSetup sequencing:
 *
 * - borrow exact qualified stable-VexLife evidence when it is currently borrowable;
 * - otherwise request the product-owned runtime owner, either observation-only or with acquisition
 *   authority.
 *
 * Borrowing qualified evidence never transfers process ownership.
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
                    when (
                        source
                    ) {
                        Source.BorrowedStableVexLife ->
                            HereticProductIdentity.StableRuntimeReceiptRelativePath

                        Source.VexLifeProdOwned ->
                            HereticProductIdentity.ProductRuntimeReceiptRelativePath
                    }

                require(
                    runtimeReceiptPath ==
                        expectedPath,
                ) {
                    "qualified Heretic runtime source must retain its rightful receipt owner coordinate"
                }
            }
        }
    }

    sealed interface Control :
        VexFamilyContract.Control<HereticRuntimeAvailability> {

        data class EnsureAvailable(
            val productOwnedMode: ProductOwnedMode,
        ) : Control
    }

    sealed interface Effect :
        VexFamilyContract.Effect<HereticRuntimeAvailability> {

        data object EnsureProductOwnedRuntime :
            Effect

        data object ObserveCurrentProductOwnedRuntime :
            Effect
    }

    override fun act(
        currentState: State,
        control: Control,
    ): Act<
        HereticRuntimeAvailability,
        State,
        Effect
    > =
        when (
            control
        ) {
            is Control.EnsureAvailable ->
                when (
                    currentState
                ) {
                    is State.StableBorrowable ->
                        admit(
                            State.Qualified(
                                source =
                                    Source.BorrowedStableVexLife,
                                evidence =
                                    currentState.evidence,
                                runtimeReceiptPath =
                                    currentState.runtimeReceiptPath,
                            ),
                        )

                    State.StablePredecessorUnavailable ->
                        stay(
                            effect =
                                when (
                                    control.productOwnedMode
                                ) {
                                    ProductOwnedMode.AcquisitionAllowed ->
                                        Effect.EnsureProductOwnedRuntime

                                    ProductOwnedMode.ObservationOnly ->
                                        Effect.ObserveCurrentProductOwnedRuntime
                                },
                        )

                    is State.Qualified ->
                        stay()
                }
        }

    /**
     * Interpret the rightful product-runtime owner's qualified observation after one Family-authored
     * product-owned Effect has been realized.
     */
    fun interpretProductOwnedQualified(
        evidence: HereticModelConfiguration.QualifiedRuntimeEvidence,
    ): State.Qualified =
        State.Qualified(
            source =
                Source.VexLifeProdOwned,
            evidence =
                evidence,
            runtimeReceiptPath =
                HereticProductIdentity.ProductRuntimeReceiptRelativePath,
        )
}

// [VXG RealForever]
