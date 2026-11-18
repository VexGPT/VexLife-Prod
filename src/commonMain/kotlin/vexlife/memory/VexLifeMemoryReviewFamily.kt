package vexlife.memory

import kotlinx.serialization.Serializable
import vexlife.consideration.VexLifeConsiderationContract
import vexlife.semantic.VexFamilyContract

/**
 * Semantic owner for one pending Memory-review decision.
 *
 * Long-term Memory currentness remains [VexLifeMemoryFamily]. This sibling Family exists because
 * "Memory is current/behind" and "one candidate awaits human review" are independent facts.
 *
 * It owns only the meaning of Keep / Not now / Decline for one exact pending candidate. Persistence,
 * semantic acceptance, Score mutation, standing consent and presentation are separate owners.
 */
object VexLifeMemoryReviewFamily :
    VexFamilyContract<
        VexLifeMemoryReviewFamily,
        VexLifeMemoryReviewFamily.State,
        VexLifeMemoryReviewFamily.Control,
        VexLifeMemoryReviewFamily.Effect,
    >() {
    @Serializable
    data class CandidateRef(
        val value: String,
    ) : VexFamilyContract.Parameter<VexLifeMemoryReviewFamily> {
        init {
            require(
                value.matches(
                    Regex(
                        "review\\.vexlife-prod\\.memory\\.[0-9a-f]{32}",
                    ),
                ),
            ) {
                "Memory review candidate ref is invalid"
            }
        }
    }

    sealed interface State : VexFamilyContract.State<VexLifeMemoryReviewFamily>

    data object Idle : State

    data class Pending(
        val candidateRef: CandidateRef,
        val summary: String,
        val candidateClass: VexLifeConsiderationContract.MemoryCandidateClass,
    ) : State {
        init {
            require(
                summary.isNotBlank() &&
                    summary.length <=
                        VexLifeConsiderationContract.MaximumCandidateSummaryCharacters,
            ) {
                "pending Memory review summary is invalid"
            }
        }
    }

    sealed interface Control : VexFamilyContract.Control<VexLifeMemoryReviewFamily> {
        val controlRef: String
        val candidateRef: CandidateRef
    }

    data class Keep(
        override val candidateRef: CandidateRef,
    ) : Control {
        override val controlRef: String = ControlKeep
    }

    data class NotNow(
        override val candidateRef: CandidateRef,
    ) : Control {
        override val controlRef: String = ControlNotNow
    }

    data class Decline(
        override val candidateRef: CandidateRef,
    ) : Control {
        override val controlRef: String = ControlDecline
    }

    sealed interface Effect : VexFamilyContract.Effect<VexLifeMemoryReviewFamily> {
        val candidateRef: CandidateRef

        data class Accept(
            override val candidateRef: CandidateRef,
        ) : Effect

        data class Defer(
            override val candidateRef: CandidateRef,
        ) : Effect

        data class Dismiss(
            override val candidateRef: CandidateRef,
        ) : Effect
    }

    override fun act(
        currentState: State,
        control: Control,
    ): Act<
        VexLifeMemoryReviewFamily,
        State,
        Effect
    >? {
        val pending =
            currentState as? Pending
                ?: return null
        if (
            pending.candidateRef !=
            control.candidateRef
        ) {
            return null
        }

        return when (
            control
        ) {
            is Keep ->
                stay(
                    Effect.Accept(
                        control.candidateRef,
                    ),
                )

            is NotNow ->
                stay(
                    Effect.Defer(
                        control.candidateRef,
                    ),
                )

            is Decline ->
                stay(
                    Effect.Dismiss(
                        control.candidateRef,
                    ),
                )
        }
    }

    fun controlForRef(
        controlRef: String,
        candidateRef: CandidateRef,
    ): Control? =
        when (
            controlRef
        ) {
            ControlKeep ->
                Keep(
                    candidateRef,
                )

            ControlNotNow ->
                NotNow(
                    candidateRef,
                )

            ControlDecline ->
                Decline(
                    candidateRef,
                )

            else ->
                null
        }

    const val ControlKeep =
        "KEEP"
    const val ControlNotNow =
        "NOT_NOW"
    const val ControlDecline =
        "DECLINE"
}

// [VXG RealForever]
