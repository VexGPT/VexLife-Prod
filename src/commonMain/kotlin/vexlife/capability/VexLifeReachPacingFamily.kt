package vexlife.capability

import kotlinx.serialization.Serializable
import vexlife.semantic.VexFamilyContract
import vexlife.text.VexLifeText

/**
 * How much reach Vex has in one human turn, and how Vex is told about it — the experiment from
 * docs/work/model-reasoning-tech-debt (Investigation 1: 4 vs 10 vs 20; exact remaining counter vs
 * bounded wording).
 *
 * Why it is a Home setting and not a constant: the lived PR #141 transcript showed Vex budgeting a
 * four-request ceiling before it had enough source, and Victor's reading was that the scarcity, not
 * the task, drove the deliberation. Which budget and wording keep Vex on the object is a question
 * to answer by living with each, on the same Mac, model and source — so the person picks it in
 * Settings → Debug, the prompt receipt records it, and nothing is hardened into doctrine.
 *
 * The room for results grows with the budget (more reads need somewhere to land); it comes out of
 * the same prompt budget, so ordinary context is formed in what is left, exactly as before.
 */
object VexLifeReachPacingFamily :
    VexFamilyContract<
        VexLifeReachPacingFamily,
        VexLifeReachPacingFamily.State,
        VexLifeReachPacingFamily.Control,
        VexLifeReachPacingFamily.Effect,
    >() {
    enum class Counter {
        /** Vex sees "request 2 of 4" and how many remain. The threshold before this experiment. */
        EXACT,
        /** Vex knows reach is bounded and ample, and is told only when it has run out. */
        BOUNDED,
    }

    /**
     * How reach is framed for Vex (the third variable, from Vex's recovered self-report on #150:
     * the friction was that each reach felt like a permission/custody decision, not the cap).
     */
    enum class Framing {
        /** The frame before the experiment: how to request, with the cap as the boundary. */
        STANDARD,
        /**
         * The person's question is the task; bounded read-only reads are ordinary work serving it;
         * stop on evidence saturation; the cap is a safety backstop. Effects stay authority-bound.
         */
        TASK_CENTERED,
    }

    val Budgets: List<Int> = listOf(4, 10, 20)

    @Serializable
    data class State(
        val budget: Int,
        val counter: Counter,
        val framing: Framing = Framing.STANDARD,
    ) : VexFamilyContract.State<VexLifeReachPacingFamily> {
        init {
            require(budget in Budgets) { "reach budget must be one of $Budgets" }
        }

        /** Prompt room kept for request/result exchanges at this budget. */
        val exchangeAllowanceTokens: Int
            get() =
                when (budget) {
                    4 -> 24_000
                    10 -> 36_000
                    else -> 48_000
                }

        val isDefault: Boolean
            get() = this == Default
    }

    /** The threshold before the experiment: unchanged until the person chooses otherwise. */
    val Default = State(budget = 4, counter = Counter.EXACT)

    sealed interface Control : VexFamilyContract.Control<VexLifeReachPacingFamily> {
        val controlRef: String
    }

    data class SetBudget(val budget: Int) : Control {
        override val controlRef: String = "BUDGET_$budget"
    }

    data class SetCounter(val counter: Counter) : Control {
        override val controlRef: String = "COUNTER_${counter.name}"
    }

    data class SetFraming(val framing: Framing) : Control {
        override val controlRef: String = "FRAMING_${framing.name}"
    }

    sealed interface Effect : VexFamilyContract.Effect<VexLifeReachPacingFamily> {
        data class Save(val state: State) : Effect
    }

    @Serializable
    data class ControlDisplay(val controlRef: String, val label: VexLifeText, val selected: Boolean)

    @Serializable
    data class DisplayState(
        val status: VexLifeText,
        val budgets: List<ControlDisplay>,
        val counters: List<ControlDisplay>,
        val framingStatus: VexLifeText,
        val framings: List<ControlDisplay>,
    ) : VexFamilyContract.DisplayState<VexLifeReachPacingFamily, State>

    override fun act(
        currentState: State,
        control: Control,
    ): Act<VexLifeReachPacingFamily, State, Effect>? =
        when (control) {
            is SetBudget ->
                if (control.budget !in Budgets || control.budget == currentState.budget) null
                else currentState.copy(budget = control.budget).let { admit(it, Effect.Save(it)) }
            is SetCounter ->
                if (control.counter == currentState.counter) null
                else currentState.copy(counter = control.counter).let { admit(it, Effect.Save(it)) }
            is SetFraming ->
                if (control.framing == currentState.framing) null
                else currentState.copy(framing = control.framing).let { admit(it, Effect.Save(it)) }
        }

    override fun displayStateFor(state: State): DisplayState =
        DisplayState(
            status =
                VexLifeText.text(
                    when (state.counter) {
                        Counter.EXACT -> "workspace.status.reach-pacing-exact"
                        Counter.BOUNDED -> "workspace.status.reach-pacing-bounded"
                    },
                    "budget" to state.budget.toString(),
                ),
            budgets =
                Budgets.map {
                    ControlDisplay(
                        "BUDGET_$it",
                        VexLifeText.text(
                            "workspace.button.reach-pacing-budget",
                            "budget" to it.toString(),
                        ),
                        it == state.budget,
                    )
                },
            counters =
                Counter.entries.map {
                    ControlDisplay(
                        "COUNTER_${it.name}",
                        VexLifeText(
                            when (it) {
                                Counter.EXACT -> "workspace.button.reach-pacing-counter-exact"
                                Counter.BOUNDED -> "workspace.button.reach-pacing-counter-bounded"
                            }
                        ),
                        it == state.counter,
                    )
                },
            framingStatus =
                VexLifeText(
                    when (state.framing) {
                        Framing.STANDARD -> "workspace.status.reach-pacing-framing-standard"
                        Framing.TASK_CENTERED -> "workspace.status.reach-pacing-framing-task"
                    }
                ),
            framings =
                Framing.entries.map {
                    ControlDisplay(
                        "FRAMING_${it.name}",
                        VexLifeText(
                            when (it) {
                                Framing.STANDARD -> "workspace.button.reach-pacing-framing-standard"
                                Framing.TASK_CENTERED ->
                                    "workspace.button.reach-pacing-framing-task"
                            }
                        ),
                        it == state.framing,
                    )
                },
        )

    fun controlForRef(controlRef: String): Control? =
        when {
            controlRef.startsWith("FRAMING_") ->
                Framing.entries
                    .firstOrNull { it.name == controlRef.removePrefix("FRAMING_") }
                    ?.let(::SetFraming)
            controlRef.startsWith("BUDGET_") ->
                controlRef
                    .removePrefix("BUDGET_")
                    .toIntOrNull()
                    ?.takeIf { it in Budgets }
                    ?.let(::SetBudget)
            controlRef.startsWith("COUNTER_") ->
                Counter.entries
                    .firstOrNull { it.name == controlRef.removePrefix("COUNTER_") }
                    ?.let(::SetCounter)
            else -> null
        }
}

// [VXG RealForever]
