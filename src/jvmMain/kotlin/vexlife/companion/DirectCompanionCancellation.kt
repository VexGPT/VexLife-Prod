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
    class Cancelled internal constructor() :
        IllegalStateException(
            "direct Companion turn cancelled",
        )

    private val requested =
        AtomicBoolean(
            false,
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
            requested.get()
        ) {
            thread.interrupt()
        }
        return AutoCloseable {
            executionThread.compareAndSet(
                thread,
                null,
            )
            if (
                requested.get() &&
                    Thread.currentThread() ===
                    thread
            ) {
                Thread.interrupted()
            }
        }
    }

    fun request(): Boolean {
        val first =
            requested.compareAndSet(
                false,
                true,
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
        if (
            requested.get()
        ) {
            throw Cancelled()
        }
    }

    fun cancelledExceptionOrNull(): Cancelled? =
        Cancelled()
            .takeIf {
                requested.get()
            }

    fun onCancel(
        action: () -> Unit,
    ): AutoCloseable {
        if (
            requested.get()
        ) {
            runCatching {
                action()
            }
            return AutoCloseable {}
        }

        cleanupActions +=
            action

        if (
            requested.get() &&
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
            requested.get()
}

// [VXG RealForever]
