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
        /** The response sequence at which the thread context horizon was set; null when absent. */
        val horizonSequence: Long?,
        val clearedAt: String?,
        val display: VexLifeThreadContextFamily.DisplayState,
        val effectsPerformed: Boolean,
    )

    class Refused(val code: String, message: String) : IllegalStateException(message)

    private fun store() = DirectConversationStore(productHome)

    private fun state(
        snapshot: DirectConversationStore.Snapshot,
        horizon: DirectConversationStore.ContextHorizon?,
    ): VexLifeThreadContextFamily.State {
        val head = snapshot.head ?: return VexLifeThreadContextFamily.Empty
        val exchanges = (head.sequence + 1) / 2
        if (horizon == null) return VexLifeThreadContextFamily.Whole(exchanges)
        return VexLifeThreadContextFamily.FromHorizon(
            exchangesBehind = horizon.exchangesBehind,
            exchangesSince = exchanges - horizon.exchangesBehind,
            clearedAt = horizon.formedAt,
        )
    }

    fun state(): VexLifeThreadContextFamily.State {
        val store = store()
        return state(
            snapshot = store.current(),
            horizon = store.contextHorizon(),
        )
    }

    private fun projection(
        snapshot: DirectConversationStore.Snapshot,
        horizon: DirectConversationStore.ContextHorizon?,
        effectsPerformed: Boolean,
    ): Projection {
        val state = state(snapshot, horizon)
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

    fun projection(effectsPerformed: Boolean = false): Projection {
        val store = store()
        return projection(
            snapshot = store.current(),
            horizon = store.contextHorizon(),
            effectsPerformed = effectsPerformed,
        )
    }

    fun control(controlRef: String): Projection {
        val control =
            VexLifeThreadContextFamily.controlForRef(controlRef)
                ?: throw Refused("UNKNOWN_CONTROL", "unknown thread context control")
        val store = store()

        try {
            return store.withWriterLease(instanceRef = InstanceRefPrefix + UUID.randomUUID()) {
                // Resolve Control against exact current State while the Conversation writer boundary
                // that makes that State stable is held.
                val currentState =
                    state(
                        snapshot = current(),
                        horizon = contextHorizon(),
                    )
                val act =
                    VexLifeThreadContextFamily.resolve(currentState, control)
                        ?: throw Refused(
                            "CONTROL_NOT_LAWFUL",
                            "that control is not available in this state",
                        )
                val effect = act.effect
                when (effect) {
                    VexLifeThreadContextFamily.Effect.SetHorizon -> {
                        val horizon = setContextHorizon(clock())
                        VexLogger.record(
                            VexLogger.Fact.ThreadContextHorizonSet,
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
                            VexLogger.Fact.ThreadContextHorizonRemoved,
                            mapOf("removed" to removed.toString()),
                        )
                    }
                    null -> Unit
                }

                projection(
                    snapshot = current(),
                    horizon = contextHorizon(),
                    effectsPerformed = effect != null,
                )
            }
        } catch (error: Refused) {
            throw error
        } catch (error: Exception) {
            // Only observed writer ownership becomes a thread-control refusal. Other failures keep
            // their real identity instead of being mislabeled as "Vex is answering."
            when (runCatching { store.threadWriterCurrentness() }.getOrNull()?.state) {
                "ACTIVE" ->
                    throw Refused(
                        "THREAD_BUSY",
                        "Vex is in the middle of a turn; try again when it has answered",
                    )

                "RECOVERY_REQUIRED" ->
                    throw Refused(
                        "THREAD_RECOVERY_REQUIRED",
                        "the prior thread writer must be recovered before thread context can change",
                    )

                "BLOCKED" ->
                    throw Refused(
                        "THREAD_WRITER_UNVERIFIABLE",
                        "thread writer ownership cannot be verified, so thread context was not changed",
                    )

                else -> throw error
            }
        }
    }

    companion object {
        const val Schema = "vexlife-prod.thread-context/v1"
        private const val InstanceRefPrefix = "instance.vexlife.thread-context."
    }
}

// [VXG RealForever]
