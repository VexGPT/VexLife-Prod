package vexlife.setup

import java.io.IOException
import java.net.ConnectException
import java.net.SocketException
import java.net.UnknownHostException
import java.net.http.HttpTimeoutException
import kotlinx.serialization.Serializable
import vexlife.text.VexLifeStrings

/**
 * Human projection of an Effect that did not complete.
 *
 * This owns no State and never predicts a successor State: the activation still re-observes and the
 * window renders whatever is true now. It only replaces a raw JVM stack trace with a bounded
 * explanation of what did not happen, what was preserved, and what the person can do next. The
 * technical detail is kept (bounded, in English, as evidence) for diagnosis; the explanation is in
 * the person's selected language.
 */
object VexLifeProdSetupFailure {
    const val StateChanged = "STATE_CHANGED_BEFORE_EFFECT"
    const val SetupAlreadyRunning = "SETUP_ALREADY_RUNNING"
    const val StorageExhausted = "STORAGE_EXHAUSTED"
    const val NetworkUnavailable = "NETWORK_UNAVAILABLE"
    const val PrerequisiteMissing = "PREREQUISITE_MISSING"
    const val HomePreserved = "HOME_PRESERVED"
    const val StoppedSafely = "STOPPED_SAFELY"

    private const val TechnicalLimit = 1_500

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
                    (technicalDetail?.length ?: 0) <= TechnicalLimit
            ) {
                "setup failure projection is invalid"
            }
        }
    }

    /**
     * The Control was offered from an earlier observation, but the fresh observation taken
     * immediately before the Effect no longer admits it. Nothing was attempted.
     */
    fun stateChanged(language: String): Failure =
        Failure(
            failureRef = StateChanged,
            title = VexLifeStrings.text("setup.heading.failure-state-changed", language),
            detail = VexLifeStrings.text("setup.description.failure-state-changed", language),
            technicalDetail = null,
        )

    fun classify(error: Throwable, language: String): Failure {
        val chain =
            generateSequence(error) {
                    it.cause?.takeIf { cause -> cause !== it }
                }
                .take(8)
                .toList()
        val messages =
            chain.joinToString(" | ") {
                (it.message ?: "")
            }
        val technical =
            chain
                .joinToString("\ncaused by ") {
                    it.javaClass.simpleName + ": " + (it.message ?: "")
                }
                .let {
                    if (it.length > TechnicalLimit) {
                        it.takeLast(TechnicalLimit)
                    } else {
                        it
                    }
                }

        fun failure(ref: String, headingKey: String, descriptionKey: String) =
            Failure(
                failureRef = ref,
                title = VexLifeStrings.text(headingKey, language),
                detail = VexLifeStrings.text(descriptionKey, language),
                technicalDetail = technical,
            )

        return when {
            messages.contains("already in progress") ->
                failure(
                    SetupAlreadyRunning,
                    "setup.heading.failure-already-running",
                    "setup.description.failure-already-running",
                )

            messages.contains("No space left on device", ignoreCase = true) ->
                failure(
                    StorageExhausted,
                    "setup.heading.failure-storage-exhausted",
                    "setup.description.failure-storage-exhausted",
                )

            chain.any {
                it is UnknownHostException ||
                    it is ConnectException ||
                    it is HttpTimeoutException ||
                    it is SocketException
            } || messages.contains("download failed: HTTP") ->
                failure(
                    NetworkUnavailable,
                    "setup.heading.failure-network",
                    "setup.description.failure-network",
                )

            messages.contains("not authorized") ||
                messages.contains("creation was not authorized") ->
                failure(
                    PrerequisiteMissing,
                    "setup.heading.failure-prerequisite-missing",
                    "setup.description.failure-prerequisite-missing",
                )

            messages.contains("preserved and cannot be formed") ->
                failure(
                    HomePreserved,
                    "setup.heading.failure-home-preserved",
                    "setup.description.failure-home-preserved",
                )

            else ->
                failure(
                    StoppedSafely,
                    "setup.heading.failure-stopped",
                    "setup.description.failure-stopped",
                )
        }.let { failure ->
            if (error is IOException && failure.failureRef == StoppedSafely) {
                failure.copy(
                    title = VexLifeStrings.text("setup.heading.failure-file-access", language)
                )
            } else {
                failure
            }
        }
    }
}

// [VXG RealForever]
