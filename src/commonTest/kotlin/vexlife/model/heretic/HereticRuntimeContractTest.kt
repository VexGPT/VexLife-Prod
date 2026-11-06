package vexlife.model.heretic

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import vexlife.contract.VexContractJson

class HereticRuntimeContractTest {
    @Test
    fun qualificationMeansHealthExactModelAndActualChatWithoutLivedConversationMutation() {
        val qualification =
            HereticRuntimeQualification.qualified()

        assertTrue(
            qualification.healthAndExactModelVerified,
        )
        assertTrue(
            qualification.actualChatCompletionPerformed,
        )
        assertTrue(
            qualification.qualificationInferenceEffect,
        )
        assertFalse(
            qualification.qualificationOutputPersisted,
        )
        assertFalse(
            qualification.livedConversationEffect,
        )
    }

    @Test
    fun exactOwnedObservationRequiresMoreThanPidMatching() {
        val proof =
            HereticRuntimeObservation.OwnershipProof(
                ownerReceiptRef = "receipt.vexlife-prod.heretic.test",
                runtimeIdentityRef = "runtime.vexlife.heretic.process.4242",
                executableIdentityRef = "executable.vexlife.heretic.python.test",
                processGroupIdentityRef = "process-group.vexlife.heretic.4242",
            )

        val observation =
            HereticRuntimeObservation.Value(
                ownershipState =
                    HereticRuntimeObservation.OwnershipState.ExactOwned,
                lifecycleState =
                    HereticRuntimeObservation.LifecycleState.Current,
                lifecycleEvidenceRefOrNull = null,
                ownershipProof = proof,
                sourceRefs =
                    listOf(
                        "source.vexlife.heretic.test",
                        proof.ownerReceiptRef,
                    ),
            )

        assertTrue(
            observation.ownershipProof
                ?.exactOwnedProcessInstance ==
                true,
        )
        assertFalse(
            observation.ownershipProof
                ?.pidMatchOnly ==
                true,
        )
    }

    @Test
    fun ambiguousRuntimeCannotCarryExactOwnershipProof() {
        val proof =
            HereticRuntimeObservation.OwnershipProof(
                ownerReceiptRef = "receipt.vexlife-prod.heretic.test",
                runtimeIdentityRef = "runtime.vexlife.heretic.process.4242",
                executableIdentityRef = "executable.vexlife.heretic.python.test",
                processGroupIdentityRef = "process-group.vexlife.heretic.4242",
            )

        assertFailsWith<IllegalArgumentException> {
            HereticRuntimeObservation.Value(
                ownershipState =
                    HereticRuntimeObservation.OwnershipState.Ambiguous,
                lifecycleState =
                    HereticRuntimeObservation.LifecycleState.Unknown,
                lifecycleEvidenceRefOrNull = null,
                ownershipProof = proof,
                sourceRefs =
                    listOf(
                        "source.vexlife.heretic.test",
                    ),
            )
        }
    }

    @Test
    fun abandonedRuntimeRequiresCurrentNotLiveEvidence() {
        assertFailsWith<IllegalArgumentException> {
            HereticRuntimeObservation.Value(
                ownershipState =
                    HereticRuntimeObservation.OwnershipState.Unowned,
                lifecycleState =
                    HereticRuntimeObservation.LifecycleState.Abandoned,
                lifecycleEvidenceRefOrNull = null,
                ownershipProof = null,
                sourceRefs =
                    listOf(
                        "source.vexlife.heretic.test",
                    ),
            )
        }

        val abandoned =
            HereticRuntimeObservation.Value(
                ownershipState =
                    HereticRuntimeObservation.OwnershipState.Unowned,
                lifecycleState =
                    HereticRuntimeObservation.LifecycleState.Abandoned,
                lifecycleEvidenceRefOrNull =
                    "evidence.vexlife.heretic.not-live.test",
                ownershipProof = null,
                sourceRefs =
                    listOf(
                        "source.vexlife.heretic.test",
                    ),
            )

        assertEquals(
            HereticRuntimeObservation.LifecycleState.Abandoned,
            abandoned.lifecycleState,
        )
    }

    @Test
    fun staleRuntimeRequiresExactOwnershipAndSourceStaleEvidence() {
        val proof =
            HereticRuntimeObservation.OwnershipProof(
                ownerReceiptRef = "receipt.vexlife-prod.heretic.test",
                runtimeIdentityRef = "runtime.vexlife.heretic.process.4242",
                executableIdentityRef = "executable.vexlife.heretic.python.test",
                processGroupIdentityRef = "process-group.vexlife.heretic.4242",
            )

        val stale =
            HereticRuntimeObservation.Value(
                ownershipState =
                    HereticRuntimeObservation.OwnershipState.ExactOwned,
                lifecycleState =
                    HereticRuntimeObservation.LifecycleState.Stale,
                lifecycleEvidenceRefOrNull =
                    "evidence.vexlife.heretic.source-stale.test",
                ownershipProof = proof,
                sourceRefs =
                    listOf(
                        "source.vexlife.heretic.test",
                        proof.ownerReceiptRef,
                    ),
            )

        assertEquals(
            HereticRuntimeObservation.LifecycleState.Stale,
            stale.lifecycleState,
        )
    }

    @Test
    fun runtimeReceiptEffectBoundaryDoesNotGrantAdjacentAuthorities() {
        val effects =
            HereticRuntimeEffectBoundary.QualifiedRuntime

        assertTrue(
            effects.modelRuntime,
        )
        assertFalse(
            effects.providerNetwork,
        )
        assertFalse(
            effects.modelDownload,
        )
        assertFalse(
            effects.artifactMutation,
        )
        assertFalse(
            effects.training,
        )
        assertFalse(
            effects.weightMutation,
        )
        assertFalse(
            effects.memoryMutation,
        )
        assertFalse(
            effects.activation,
        )
        assertFalse(
            effects.succession,
        )
        assertFalse(
            effects.publication,
        )
        assertFalse(
            effects.repositoryMutation,
        )
        assertFalse(
            effects.homeMutation,
        )
    }

    @Test
    fun runtimeStateWireValuesRemainCompatibleWithCurrentVexLifeReceipts() {
        assertEquals(
            "PERSISTENT_MLX_RUNTIME_STARTING",
            HereticRuntimeState.Starting.wireValue,
        )
        assertEquals(
            "PERSISTENT_MLX_RUNTIME_QUALIFIED",
            HereticRuntimeState.Qualified.wireValue,
        )
    }
    @Test
    fun runtimeStateUsesStableWireIdentityInDurableJson() {
        assertEquals(
            "\"PERSISTENT_MLX_RUNTIME_QUALIFIED\"",
            VexContractJson.encode(
                HereticRuntimeState.Qualified,
            ),
        )
        assertEquals(
            HereticRuntimeState.Starting,
            VexContractJson.decode<HereticRuntimeState>(
                "\"PERSISTENT_MLX_RUNTIME_STARTING\"",
            ),
        )
    }
}

// [VXG RealForever]
