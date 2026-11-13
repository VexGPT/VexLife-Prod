package vexlife.setup

import java.io.IOException
import java.net.ConnectException
import java.net.SocketException
import java.net.UnknownHostException
import java.net.http.HttpTimeoutException
import kotlinx.serialization.Serializable

/**
 * Human projection of an Effect that did not complete.
 *
 * This owns no State and never predicts a successor State: the activation still re-observes and the
 * window renders whatever is true now. It only replaces a raw JVM stack trace with a bounded
 * explanation of what did not happen, what was preserved, and what the person can do next. The
 * technical detail is kept (bounded) for diagnosis.
 */
object VexLifeProdSetupFailure {
    const val StateChanged =
        "STATE_CHANGED_BEFORE_EFFECT"
    const val SetupAlreadyRunning =
        "SETUP_ALREADY_RUNNING"
    const val StorageExhausted =
        "STORAGE_EXHAUSTED"
    const val NetworkUnavailable =
        "NETWORK_UNAVAILABLE"
    const val PrerequisiteMissing =
        "PREREQUISITE_MISSING"
    const val HomePreserved =
        "HOME_PRESERVED"
    const val StoppedSafely =
        "STOPPED_SAFELY"

    private const val TechnicalLimit =
        1_500

    @Serializable
    data class Failure(
        val failureRef: String,
        val title: String,
        val detail: String,
        val technicalDetail: String?,
    ) {
        init {
            require(
                failureRef.isNotBlank() &&
                    title.isNotBlank() &&
                    detail.isNotBlank() &&
                    (technicalDetail?.length ?: 0) <=
                    TechnicalLimit,
            ) {
                "setup failure projection is invalid"
            }
        }
    }

    /**
     * The Control was offered from an earlier observation, but the fresh observation taken
     * immediately before the Effect no longer admits it. Nothing was attempted.
     */
    fun stateChanged(): Failure =
        Failure(
            failureRef = StateChanged,
            title = "Something changed before VexLife acted.",
            detail =
                "VexLife looked again right before acting, and that choice no longer fits what is on " +
                    "this Mac now. Nothing was changed. Here is what is true now.",
            technicalDetail = null,
        )

    fun classify(
        error: Throwable,
    ): Failure {
        val chain =
            generateSequence(
                error,
            ) {
                it.cause?.takeIf { cause -> cause !== it }
            }.take(
                8,
            ).toList()
        val messages =
            chain.joinToString(
                " | ",
            ) {
                (it.message ?: "")
            }
        val technical =
            chain
                .joinToString(
                    "\ncaused by ",
                ) {
                    it.javaClass.simpleName +
                        ": " +
                        (it.message ?: "")
                }
                .let {
                    if (
                        it.length >
                        TechnicalLimit
                    ) {
                        it.takeLast(
                            TechnicalLimit,
                        )
                    } else {
                        it
                    }
                }

        fun failure(
            ref: String,
            title: String,
            detail: String,
        ) =
            Failure(
                failureRef = ref,
                title = title,
                detail = detail,
                technicalDetail = technical,
            )

        return when {
            messages.contains(
                "already in progress",
            ) ->
                failure(
                    SetupAlreadyRunning,
                    "Another VexLife setup is already running for this Home.",
                    "Only one setup can change a Vex Home at a time. Let the other one finish or cancel it, " +
                        "then choose Check Again.",
                )

            messages.contains(
                "No space left on device",
                ignoreCase = true,
            ) ->
                failure(
                    StorageExhausted,
                    "The drive ran out of space.",
                    "VexLife stopped before finishing. Files that finished downloading were kept, and an " +
                        "unfinished download resumes where it stopped. Free up space, then try again.",
                )

            chain.any {
                it is UnknownHostException ||
                    it is ConnectException ||
                    it is HttpTimeoutException ||
                    it is SocketException
            } ||
                messages.contains(
                    "download failed: HTTP",
                ) ->
                failure(
                    NetworkUnavailable,
                    "VexLife couldn't download what it needed.",
                    "Check your internet connection and try again. Files that finished downloading were " +
                        "kept, and an unfinished download resumes where it stopped.",
                )

            messages.contains(
                "not authorized",
            ) ||
                messages.contains(
                    "creation was not authorized",
                ) ->
                failure(
                    PrerequisiteMissing,
                    "Something VexLife needs is missing from your Home.",
                    "VexLife started without downloading anything, as you chose, and found a missing piece. " +
                        "Nothing was deleted. Choose Check Again to see what needs to be set up.",
                )

            messages.contains(
                "preserved and cannot be formed",
            ) ->
                failure(
                    HomePreserved,
                    "VexLife left that folder as it was.",
                    "The selected location could not safely become a Vex Home, so nothing was created " +
                        "there. Choose Check Again to see why, or choose another location.",
                )

            else ->
                failure(
                    StoppedSafely,
                    "VexLife stopped before finishing.",
                    "Nothing in your Vex Home was deleted. You can try again. If it keeps happening, the " +
                        "technical detail below explains where it stopped.",
                )
        }.let {
            failure ->
            if (
                error is IOException &&
                failure.failureRef ==
                StoppedSafely
            ) {
                failure.copy(
                    title = "VexLife couldn't read or write a file it needed.",
                )
            } else {
                failure
            }
        }
    }
}

// [VXG RealForever]
