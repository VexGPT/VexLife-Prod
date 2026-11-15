package vexlife.capability

import java.nio.file.Path
import kotlinx.serialization.Serializable
import vexlife.contract.VexDurableContractFile
import vexlife.filesystem.VexOwnedPath
import vexlife.logging.VexLogger

/**
 * The Home's reach pacing setting ([VexLifeReachPacingFamily]): read once at the start of every
 * turn, changed only by the person in Settings → Debug. An unreadable or absent setting is the
 * default threshold.
 */
class VexLifeReachPacing(private val productHome: Path) {
    @Serializable
    data class Stored(
        val schemaVersion: String,
        val budget: Int,
        val counter: VexLifeReachPacingFamily.Counter,
    )

    @Serializable
    data class Projection(
        val schemaVersion: String,
        val budget: Int,
        val counter: VexLifeReachPacingFamily.Counter,
        val display: VexLifeReachPacingFamily.DisplayState,
        val effectsPerformed: Boolean,
    )

    class Refused(val code: String, message: String) : IllegalArgumentException(message)

    private val path: Path
        get() = VexOwnedPath.resolveInside(productHome, RelativePath)

    fun current(): VexLifeReachPacingFamily.State =
        runCatching {
            VexDurableContractFile.read<Stored>(productHome, path)
                ?.takeIf { it.schemaVersion == Schema }
                ?.let { VexLifeReachPacingFamily.State(it.budget, it.counter) }
        }
            .getOrNull() ?: VexLifeReachPacingFamily.Default

    fun projection(effectsPerformed: Boolean = false): Projection {
        val state = current()
        return Projection(
            schemaVersion = ProjectionSchema,
            budget = state.budget,
            counter = state.counter,
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
                    Stored(Schema, effect.state.budget, effect.state.counter),
                )
                VexLogger.record(
                    VexLogger.Fact.ReachPacingChosen,
                    mapOf(
                        "budget" to effect.state.budget.toString(),
                        "counter" to effect.state.counter.name,
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
