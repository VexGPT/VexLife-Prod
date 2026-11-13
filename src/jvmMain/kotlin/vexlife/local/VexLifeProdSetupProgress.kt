package vexlife.local

/**
 * Chronology of setup facts as their rightful owners learn them, projected for a waiting human.
 *
 * This owns no State and decides nothing. A progress line never proves that a step succeeded: the
 * native window still re-observes current truth after the process ends. Lines are written with the
 * predecessor's `[VexLife] ` prefix so the already-lived AppKit progress panel parser applies
 * unchanged.
 *
 * Long-lived consumers (the Product service process) use [Silent].
 */
interface VexLifeProdSetupProgress {
    fun phase(phase: Phase)

    fun artifactBytes(completedBytes: Long, totalBytes: Long)

    enum class Phase(val line: String) {
        VerifyingProduct("Checking this copy of VexLife…"),
        FormingHome("Creating your Vex Home…"),
        CheckingHome("Checking your Vex Home…"),
        PreparingPython("Preparing VexLife's private Python…"),
        CheckingModelFiles("Checking the local model files…"),
        PreparingModelRuntime("Preparing the local model runtime…"),
        StartingLocalModel("Starting the local model. This can take a few minutes…"),
        InstallingProduct("Installing this version of VexLife into your Home…"),
        StartingVexLife("Starting VexLife…"),
        ConfirmingVexLife("Confirming VexLife is running…"),
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
        private val clockMs: () -> Long = System::currentTimeMillis,
        private val minimumIntervalMs: Long = 1_000L,
    ) : VexLifeProdSetupProgress {
        private var lastEmittedAtMs = Long.MIN_VALUE
        private var lastPercent = -1

        override fun phase(phase: Phase) {
            sink(LinePrefix + phase.line)
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
                    "Downloading the local model — " +
                    gib(bounded) +
                    " of " +
                    gib(totalBytes) +
                    " GiB (" +
                    percent +
                    "%). Keep this window open; an interrupted download resumes next time."
            )
        }
    }

    companion object {
        const val LinePrefix = "[VexLife] "

        fun standardError(): VexLifeProdSetupProgress {
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
                }
            )
        }

        internal fun gib(bytes: Long): String {
            val tenths = (bytes * 10L + GiB / 2L) / GiB
            return "${tenths / 10L}.${tenths % 10L}"
        }

        private const val GiB = 1024L * 1024L * 1024L
    }
}

// [VXG RealForever]
