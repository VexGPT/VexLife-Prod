package vexlife.model.heretic

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HereticRuntimeAvailabilityTest {
    private val macRealization =
        HereticRuntimeRealization.legacyMacV1(
            HereticProductIdentity.ModelRef,
        )

    private fun evidence(
        receiptRef: String,
    ): HereticModelConfiguration.QualifiedRuntimeEvidence =
        HereticModelConfiguration.QualifiedRuntimeEvidence(
            realizationRef = macRealization.realizationRef,
            runtimeReceiptRef = receiptRef,
            runtimeState = HereticRuntimeState.Qualified,
            modelRef = HereticProductIdentity.ModelRef,
            modelProfileRef = HereticProductIdentity.ModelProfileRef,
            artifactRef = macRealization.artifact.artifactRef,
            artifactContentSetSha256 = macRealization.artifact.contentSetSha256,
            runtimeAdapterRef = macRealization.runtime.runtimeAdapterRef,
            runtimeClass = macRealization.runtime.runtimeClass,
            endpoint = macRealization.runtime.endpoint,
            requestModel = macRealization.runtime.requestModel,
            qualification = HereticRuntimeQualification.qualified(),
        )

    @Test
    fun exactBorrowableStableRuntimeSatisfiesMatchingRealizationWithoutProductEffect() {
        val borrowed =
            evidence(
                "receipt.vexlife.heretic-persistent-runtime.stable.test",
            )

        val act =
            assertNotNull(
                HereticRuntimeAvailability.resolve(
                    currentState =
                        HereticRuntimeAvailability.State.StableBorrowable(
                            evidence = borrowed,
                            runtimeReceiptPath =
                                HereticProductIdentity.StableRuntimeReceiptRelativePath,
                        ),
                    control =
                        HereticRuntimeAvailability.Control.EnsureAvailable(
                            realizationRef = macRealization.realizationRef,
                            productOwnedMode =
                                HereticRuntimeAvailability.ProductOwnedMode.AcquisitionAllowed,
                        ),
                ),
            )

        val qualified =
            assertIs<HereticRuntimeAvailability.State.Qualified>(
                act.admittedState,
            )

        assertNull(act.effect)
        assertEquals(
            HereticRuntimeAvailability.Source.BorrowedStableVexLife,
            qualified.source,
        )
        assertTrue(qualified.evidence === borrowed)
    }

    @Test
    fun qualifiedMacRuntimeCannotSatisfyWindowsRealizationRequest() {
        val borrowed =
            evidence(
                "receipt.vexlife.heretic-persistent-runtime.stable.test",
            )
        val windows = HereticRuntimeRealization.Windows4B

        val act =
            assertNotNull(
                HereticRuntimeAvailability.resolve(
                    currentState =
                        HereticRuntimeAvailability.State.StableBorrowable(
                            evidence = borrowed,
                            runtimeReceiptPath =
                                HereticProductIdentity.StableRuntimeReceiptRelativePath,
                        ),
                    control =
                        HereticRuntimeAvailability.Control.EnsureAvailable(
                            realizationRef = windows.realizationRef,
                            productOwnedMode =
                                HereticRuntimeAvailability.ProductOwnedMode.AcquisitionAllowed,
                        ),
                ),
            )

        assertNull(act.admittedState)
        val effect =
            assertIs<HereticRuntimeAvailability.Effect.EnsureProductOwnedRuntime>(
                act.effect,
            )
        assertEquals(windows.realizationRef, effect.realizationRef)
    }

    @Test
    fun unavailablePredecessorRequestsExactRealizationEffect() {
        val acquisition =
            assertNotNull(
                HereticRuntimeAvailability.resolve(
                    currentState =
                        HereticRuntimeAvailability.State.StablePredecessorUnavailable,
                    control =
                        HereticRuntimeAvailability.Control.EnsureAvailable(
                            realizationRef = macRealization.realizationRef,
                            productOwnedMode =
                                HereticRuntimeAvailability.ProductOwnedMode.AcquisitionAllowed,
                        ),
                ),
            )

        val acquireEffect =
            assertIs<HereticRuntimeAvailability.Effect.EnsureProductOwnedRuntime>(
                acquisition.effect,
            )
        assertEquals(macRealization.realizationRef, acquireEffect.realizationRef)

        val observationOnly =
            assertNotNull(
                HereticRuntimeAvailability.resolve(
                    currentState =
                        HereticRuntimeAvailability.State.StablePredecessorUnavailable,
                    control =
                        HereticRuntimeAvailability.Control.EnsureAvailable(
                            realizationRef = macRealization.realizationRef,
                            productOwnedMode =
                                HereticRuntimeAvailability.ProductOwnedMode.ObservationOnly,
                        ),
                ),
            )

        val observeEffect =
            assertIs<HereticRuntimeAvailability.Effect.ObserveCurrentProductOwnedRuntime>(
                observationOnly.effect,
            )
        assertEquals(macRealization.realizationRef, observeEffect.realizationRef)
    }

    @Test
    fun productOwnedObservationKeepsRealizationOwnedReceiptCoordinate() {
        val product =
            evidence(
                "receipt.vexlife-prod.heretic-persistent-runtime.product.test",
            )

        val qualified =
            HereticRuntimeAvailability.interpretProductOwnedQualified(
                product,
            )

        assertEquals(
            HereticRuntimeAvailability.Source.VexLifeProdOwned,
            qualified.source,
        )
        assertTrue(qualified.evidence === product)
        assertEquals(
            macRealization.runtimeReceiptRelativePath,
            qualified.runtimeReceiptPath,
        )

        val repeated =
            assertNotNull(
                HereticRuntimeAvailability.resolve(
                    currentState = qualified,
                    control =
                        HereticRuntimeAvailability.Control.EnsureAvailable(
                            realizationRef = macRealization.realizationRef,
                            productOwnedMode =
                                HereticRuntimeAvailability.ProductOwnedMode.ObservationOnly,
                        ),
                ),
            )

        assertNull(repeated.admittedState)
        assertNull(repeated.effect)
    }
}

// [VXG RealForever]
