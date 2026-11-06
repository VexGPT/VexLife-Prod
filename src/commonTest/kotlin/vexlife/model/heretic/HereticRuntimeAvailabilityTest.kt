package vexlife.model.heretic

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HereticRuntimeAvailabilityTest {
    private fun evidence(
        receiptRef: String,
    ): HereticModelConfiguration.QualifiedRuntimeEvidence =
        HereticModelConfiguration.QualifiedRuntimeEvidence(
            runtimeReceiptRef = receiptRef,
            runtimeState = HereticRuntimeState.Qualified,
            modelRef = HereticProductIdentity.ModelRef,
            modelProfileRef = HereticProductIdentity.ModelProfileRef,
            artifactRef = HereticProductIdentity.ArtifactSetRef,
            artifactContentSetSha256 = HereticProductIdentity.ArtifactContentSetSha256,
            runtimeAdapterRef = HereticProductIdentity.RuntimeAdapterRef,
            runtimeClass = HereticProductIdentity.RuntimeClass,
            endpoint = HereticProductIdentity.Endpoint,
            requestModel = HereticProductIdentity.RequestModel,
            qualification = HereticRuntimeQualification.qualified(),
        )

    @Test
    fun exactBorrowableStableRuntimeSatisfiesAvailabilityWithoutProductEffect() {
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
                            productOwnedMode =
                                HereticRuntimeAvailability.ProductOwnedMode.AcquisitionAllowed,
                        ),
                ),
            )

        val qualified =
            assertIs<HereticRuntimeAvailability.State.Qualified>(
                act.admittedState,
            )

        assertNull(
            act.effect,
        )
        assertEquals(
            HereticRuntimeAvailability.Source.BorrowedStableVexLife,
            qualified.source,
        )
        assertTrue(
            qualified.evidence ===
                borrowed,
        )
        assertEquals(
            HereticProductIdentity.StableRuntimeReceiptRelativePath,
            qualified.runtimeReceiptPath,
        )
    }

    @Test
    fun unavailableStablePredecessorRequestsOnlyTheLawfulProductOwnedEffect() {
        val acquisition =
            assertNotNull(
                HereticRuntimeAvailability.resolve(
                    currentState =
                        HereticRuntimeAvailability.State.StablePredecessorUnavailable,
                    control =
                        HereticRuntimeAvailability.Control.EnsureAvailable(
                            productOwnedMode =
                                HereticRuntimeAvailability.ProductOwnedMode.AcquisitionAllowed,
                        ),
                ),
            )

        assertNull(
            acquisition.admittedState,
        )
        assertEquals(
            HereticRuntimeAvailability.Effect.EnsureProductOwnedRuntime,
            acquisition.effect,
        )

        val observationOnly =
            assertNotNull(
                HereticRuntimeAvailability.resolve(
                    currentState =
                        HereticRuntimeAvailability.State.StablePredecessorUnavailable,
                    control =
                        HereticRuntimeAvailability.Control.EnsureAvailable(
                            productOwnedMode =
                                HereticRuntimeAvailability.ProductOwnedMode.ObservationOnly,
                        ),
                ),
            )

        assertNull(
            observationOnly.admittedState,
        )
        assertEquals(
            HereticRuntimeAvailability.Effect.ObserveCurrentProductOwnedRuntime,
            observationOnly.effect,
        )
    }

    @Test
    fun productOwnedObservationBecomesQualifiedWithoutRelabelingReceiptCustody() {
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
        assertTrue(
            qualified.evidence ===
                product,
        )
        assertEquals(
            HereticProductIdentity.ProductRuntimeReceiptRelativePath,
            qualified.runtimeReceiptPath,
        )

        val repeated =
            assertNotNull(
                HereticRuntimeAvailability.resolve(
                    currentState = qualified,
                    control =
                        HereticRuntimeAvailability.Control.EnsureAvailable(
                            productOwnedMode =
                                HereticRuntimeAvailability.ProductOwnedMode.ObservationOnly,
                        ),
                ),
            )

        assertNull(
            repeated.admittedState,
        )
        assertNull(
            repeated.effect,
        )
    }
}

// [VXG RealForever]
