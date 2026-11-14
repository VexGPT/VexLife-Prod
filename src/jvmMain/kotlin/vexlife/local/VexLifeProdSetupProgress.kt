package vexlife.local

import vexlife.person.VexLifePersonSettings
import vexlife.text.VexLifeStrings
import vexlife.text.VexLifeText

/**
 * Chronology of setup facts as their rightful owners learn them, projected for a waiting human.
 *
 * This owns no State and decides nothing. A progress line never proves that a step succeeded: the
 * native window still re-observes current truth after the process ends. Lines are in the person's
 * selected language (keyed string source). They are written with the predecessor's `[VexLife] `
 * prefix so the already-lived AppKit progress panel parser applies unchanged.
 *
 * Long-lived consumers (the Product service process) use [Silent].
 */
interface VexLifeProdSetupProgress {
    fun phase(phase: Phase)

    fun artifactBytes(completedBytes: Long, totalBytes: Long)

    /** Each phase names its line by string key (`setup.status.progress-*`), never by words. */
    enum class Phase(val key: String) {
        VerifyingProduct("setup.status.progress-verifying-product"),
        FormingHome("setup.status.progress-forming-home"),
        CheckingHome("setup.status.progress-checking-home"),
        PreparingPython("setup.status.progress-preparing-python"),
        CheckingModelFiles("setup.status.progress-checking-model-files"),
        PreparingModelRuntime("setup.status.progress-preparing-model-runtime"),
        StartingLocalModel("setup.status.progress-starting-local-model"),
        InstallingProduct("setup.status.progress-installing-product"),
        StartingVexLife("setup.status.progress-starting-vexlife"),
        ConfirmingVexLife("setup.status.progress-confirming-vexlife"),
    }

    object Silent : VexLifeProdSetupProgress {
        override fun phase(phase: Phase) = Unit

        override fun artifactBytes(completedBytes: Long, totalBytes: Long) = Unit
    }

    /**
     * Line projection with bounded volume: byte progress is emitted at most once per
     * [minimumIntervalMs] unless the whole-percent value changed, and always at completion.
     */
    class Lines(
        private val sink: (String) -> Unit,
        private val language: String = VexLifePersonSettings.DefaultLanguage,
        private val clockMs: () -> Long = System::currentTimeMillis,
        private val minimumIntervalMs: Long = 1_000L,
    ) : VexLifeProdSetupProgress {
        private var lastEmittedAtMs = Long.MIN_VALUE
        private var lastPercent = -1

        override fun phase(phase: Phase) {
            sink(LinePrefix + VexLifeStrings.text(phase.key, language))
        }

        override fun artifactBytes(completedBytes: Long, totalBytes: Long) {
            if (totalBytes <= 0) {
                return
            }
            val bounded = completedBytes.coerceIn(0L, totalBytes)
            val percent = ((bounded * 100L) / totalBytes).toInt()
            val now = clockMs()
            val complete = bounded == totalBytes
            if (
                !complete &&
                    percent == lastPercent &&
                    lastEmittedAtMs != Long.MIN_VALUE &&
                    now - lastEmittedAtMs < minimumIntervalMs
            ) {
                return
            }
            lastEmittedAtMs = now
            lastPercent = percent
            sink(
                LinePrefix +
                    VexLifeStrings.resolve(
                        VexLifeText(
                            key = "setup.status.progress-downloading",
                            args = mapOf("percent" to percent.toString()),
                            gib = mapOf("done" to bounded, "total" to totalBytes),
                        ),
                        language,
                    )
            )
        }
    }

    companion object {
        const val LinePrefix = "[VexLife] "

        fun standardError(
            language: String = VexLifePersonSettings.DefaultLanguage
        ): VexLifeProdSetupProgress {
            val utf8 =
                java.io.PrintStream(
                    java.io.FileOutputStream(java.io.FileDescriptor.err),
                    true,
                    Charsets.UTF_8,
                )
            return Lines(
                sink = { line ->
                    utf8.println(line)
                    utf8.flush()
                },
                language = language,
            )
        }
    }
}

// [VXG RealForever]
