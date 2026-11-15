package vexlife.conversation

import java.nio.file.Path
import java.time.Instant
import java.util.UUID
import kotlinx.serialization.Serializable
import vexlife.logging.VexLogger

/**
 * Performs [VexLifeThreadContextFamily] against one Home's Conversation.
 *
 * State is observed fresh from the Conversation owner on every call (its current head and context
 * horizon), so the person never sees a stale answer to "what is Vex given". The effect takes the
 * thread writer lease: a turn in flight finishes with the context it started with, and the horizon
 * applies from the next one.
 */
class VexLifeThreadContext(
    private val productHome: Path,
    private val clock: () -> Instant = Instant::now,
) {
    @Serializable
    data class Projection(
        val schemaVersion: String,
        val state: String,
        val exchanges: Long,
        /** The response sequence at which thread memory was cleared; null when it was not. */
        val horizonSequence: Long?,
        val clearedAt: String?,
        val display: VexLifeThreadContextFamily.DisplayState,
        val effectsPerformed: Boolean,
    )

    class Refused(val code: String, message: String) : IllegalStateException(message)

    private fun store() = DirectConversationStore(productHome)

    fun state(): VexLifeThreadContextFamily.State {
        val store = store()
        val head = store.current().head ?: return VexLifeThreadContextFamily.Empty
        val exchanges = (head.sequence + 1) / 2
        val horizon = store.contextHorizon() ?: return VexLifeThreadContextFamily.Whole(exchanges)
        return VexLifeThreadContextFamily.FromHorizon(
            exchangesBehind = horizon.exchangesBehind,
            exchangesSince = exchanges - horizon.exchangesBehind,
            clearedAt = horizon.formedAt,
        )
    }

    fun projection(effectsPerformed: Boolean = false): Projection {
        val state = state()
        val horizon = store().contextHorizon()
        return Projection(
            schemaVersion = Schema,
            state = state.stateRef,
            exchanges =
                when (state) {
                    VexLifeThreadContextFamily.Empty -> 0
                    is VexLifeThreadContextFamily.Whole -> state.exchanges
                    is VexLifeThreadContextFamily.FromHorizon ->
                        state.exchangesBehind + state.exchangesSince
                },
            horizonSequence = horizon?.horizonSequence,
            clearedAt = horizon?.formedAt,
            display = VexLifeThreadContextFamily.displayStateFor(state),
            effectsPerformed = effectsPerformed,
        )
    }

    fun control(controlRef: String): Projection {
        val control =
            VexLifeThreadContextFamily.controlForRef(controlRef)
                ?: throw Refused("UNKNOWN_CONTROL", "unknown thread context control")
        val act =
            VexLifeThreadContextFamily.resolve(state(), control)
                ?: throw Refused(
                    "CONTROL_NOT_LAWFUL",
                    "that control is not available in this state",
                )
        val effect = act.effect ?: return projection()
        try {
            store().withWriterLease(instanceRef = InstanceRefPrefix + UUID.randomUUID()) {
                when (effect) {
                    VexLifeThreadContextFamily.Effect.SetHorizon -> {
                        val horizon = setContextHorizon(clock())
                        VexLogger.record(
                            VexLogger.Fact.ThreadMemoryCleared,
                            mapOf(
                                "horizonSequence" to
                                    (horizon?.horizonSequence?.toString() ?: "none"),
                                "exchangesBehind" to (horizon?.exchangesBehind?.toString() ?: "0"),
                            ),
                        )
                    }
                    VexLifeThreadContextFamily.Effect.RemoveHorizon -> {
                        val removed = removeContextHorizon()
                        VexLogger.record(
                            VexLogger.Fact.ThreadMemoryRestored,
                            mapOf("removed" to removed.toString()),
                        )
                    }
                }
            }
        } catch (error: Refused) {
            throw error
        } catch (error: Exception) {
            // The lease is held by a turn in flight (or its recovery): nothing changed.
            throw Refused(
                "THREAD_BUSY",
                "Vex is in the middle of a turn; try again when it has answered",
            )
        }
        return projection(effectsPerformed = true)
    }

    companion object {
        const val Schema = "vexlife-prod.thread-context/v1"
        private const val InstanceRefPrefix = "instance.vexlife.thread-context."
    }
}

// [VXG RealForever]
