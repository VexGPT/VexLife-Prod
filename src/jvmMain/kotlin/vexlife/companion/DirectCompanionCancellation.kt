package vexlife.companion

import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * Cooperative cancellation for one admitted Direct Companion turn.
 *
 * This is request/turn custody only. It may interrupt the thread currently realizing that turn and
 * close registered request-local resources such as one active provider response stream. It does not
 * own or terminate the persistent Heretic runtime process.
 */
class DirectCompanionCancellation {
    enum class Reason {
        PERSON_REQUESTED,
    }

    class Cancelled internal constructor(
        val reason: Reason,
    ) : IllegalStateException(
        "direct Companion turn cancelled: ${reason.name}",
    )

    private val requested =
        AtomicReference<Reason?>(
            null,
        )

    private val executionThread =
        AtomicReference<Thread?>(
            null,
        )

    private val cleanupActions =
        CopyOnWriteArraySet<() -> Unit>()

    fun bindCurrentThread(): AutoCloseable {
        val thread =
            Thread.currentThread()
        check(
            executionThread.compareAndSet(
                null,
                thread,
            ),
        ) {
            "Direct Companion cancellation is already bound to another execution thread"
        }
        if (
            requested.get() !=
            null
        ) {
            thread.interrupt()
        }
        return AutoCloseable {
            executionThread.compareAndSet(
                thread,
                null,
            )
            if (
                requested.get() !=
                    null &&
                    Thread.currentThread() ===
                    thread
            ) {
                Thread.interrupted()
            }
        }
    }

    fun request(
        reason: Reason =
            Reason.PERSON_REQUESTED,
    ): Boolean {
        val first =
            requested.compareAndSet(
                null,
                reason,
            )
        if (
            first
        ) {
            cleanupActions.forEach {
                action ->
                runCatching {
                    action()
                }
            }
            executionThread
                .get()
                ?.interrupt()
        }
        return first
    }

    fun checkpoint() {
        requested
            .get()
            ?.let {
                throw Cancelled(
                    it,
                )
            }
    }

    fun cancelledExceptionOrNull(): Cancelled? =
        requested
            .get()
            ?.let(
                ::Cancelled,
            )

    fun onCancel(
        action: () -> Unit,
    ): AutoCloseable {
        if (
            requested.get() !=
            null
        ) {
            runCatching {
                action()
            }
            return AutoCloseable {}
        }

        cleanupActions +=
            action

        if (
            requested.get() !=
            null &&
                cleanupActions.remove(
                    action,
                )
        ) {
            runCatching {
                action()
            }
        }

        return AutoCloseable {
            cleanupActions.remove(
                action,
            )
        }
    }

    val cancellationRequested: Boolean
        get() =
            requested.get() !=
                null
}

// [VXG RealForever]
