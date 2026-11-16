package vexlife.capability

import java.nio.file.Path
import kotlinx.serialization.Serializable
import vexlife.contract.VexDurableContractFile
import vexlife.filesystem.VexOwnedPath
import vexlife.logging.VexLogger

/**
 * The Home's reach pacing setting ([VexLifeReachPacingFamily]): read once at the start of every
 * turn, changed only by the person in Settings → Debug.
 *
 * Absence means the person has never selected an experiment value, so the pre-experiment default
 * applies. A present but unreadable, obsolete or invalid record is not the default: collapsing that
 * condition would make storage failure look like a human choice.
 */
class VexLifeReachPacing(private val productHome: Path) {
    @Serializable
    data class Stored(
        val schemaVersion: String,
        val budget: Int,
        val counter: VexLifeReachPacingFamily.Counter,
        val framing: VexLifeReachPacingFamily.Framing = VexLifeReachPacingFamily.Framing.STANDARD,
    )

    @Serializable
    data class Projection(
        val schemaVersion: String,
        val budget: Int,
        val counter: VexLifeReachPacingFamily.Counter,
        val framing: VexLifeReachPacingFamily.Framing,
        val display: VexLifeReachPacingFamily.DisplayState,
        val effectsPerformed: Boolean,
    )

    class Refused(val code: String, message: String) : IllegalArgumentException(message)

    private val path: Path
        get() = VexOwnedPath.resolveInside(productHome, RelativePath)

    fun current(): VexLifeReachPacingFamily.State {
        val stored =
            VexDurableContractFile.read<Stored>(productHome, path)
                ?: return VexLifeReachPacingFamily.Default
        require(stored.schemaVersion == Schema) {
            "reach pacing setting schema is not current"
        }
        return VexLifeReachPacingFamily.State(stored.budget, stored.counter, stored.framing)
    }

    fun projection(effectsPerformed: Boolean = false): Projection {
        val state = current()
        return Projection(
            schemaVersion = ProjectionSchema,
            budget = state.budget,
            counter = state.counter,
            framing = state.framing,
            display = VexLifeReachPacingFamily.displayStateFor(state),
            effectsPerformed = effectsPerformed,
        )
    }

    fun control(controlRef: String): Projection {
        val control =
            VexLifeReachPacingFamily.controlForRef(controlRef)
                ?: throw Refused("UNKNOWN_CONTROL", "unknown reach pacing control")
        val act = VexLifeReachPacingFamily.resolve(current(), control) ?: return projection()
        when (val effect = act.effect) {
            is VexLifeReachPacingFamily.Effect.Save -> {
                VexDurableContractFile.write(
                    productHome,
                    path,
                    Stored(Schema, effect.state.budget, effect.state.counter, effect.state.framing),
                )
                VexLogger.record(
                    VexLogger.Fact.ReachPacingChosen,
                    mapOf(
                        "budget" to effect.state.budget.toString(),
                        "counter" to effect.state.counter.name,
                        "framing" to effect.state.framing.name,
                    ),
                )
            }
            null -> Unit
        }
        return projection(effectsPerformed = true)
    }

    companion object {
        const val Schema = "vexlife-prod.reach-pacing/v1"
        const val ProjectionSchema = "vexlife-prod.reach-pacing-projection/v1"
        const val RelativePath = "runtime/vexlife-prod/reach-pacing.json"
    }
}

// [VXG RealForever]
