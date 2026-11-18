package vexlife.platform.windows

import java.io.PrintWriter
import java.io.StringWriter
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.time.OffsetDateTime
import javax.swing.JOptionPane
import javax.swing.SwingUtilities
import vexlife.model.heretic.HereticRuntimeRealization
import vexlife.platform.VexLifeJvmPlatformTarget
import vexlife.platform.VexLifePlatformRealization
import vexlife.service.VexLifeProdServiceContract

object VexLifeProdWindowsPreview {
    const val SourceShaProperty = "vexlife.prod.sourceSha"
    const val HomeEnvironment = "VEXLIFE_HOME"

    private val SourceShaPattern = Regex("[0-9a-f]{40}")

    data class LaunchPlan(
        val sourceSha: String,
        val productHome: Path,
        val productOrigin: String,
    )

    internal fun plan(
        sourceSha: String?,
        userHome: Path?,
        homeOverride: Path?,
        target: VexLifePlatformRealization.Target,
    ): LaunchPlan {
        require(target == HereticRuntimeRealization.WindowsX64) {
            "Windows lived preview requires the Windows x64 Product target"
        }

        val exactSourceSha =
            requireNotNull(sourceSha?.takeIf(SourceShaPattern::matches)) {
                "Windows lived preview requires one exact source SHA"
            }
        val defaultHome =
            requireNotNull(userHome) {
                "Windows lived preview requires the current user Home"
            }
                .toAbsolutePath()
                .normalize()

        val selectedHome = homeOverride ?: defaultHome.resolve(".vexlife")
        require(selectedHome.isAbsolute) {
            "Windows lived preview Vex Home must be one absolute path"
        }

        return LaunchPlan(
            sourceSha = exactSourceSha,
            productHome = selectedHome.normalize(),
            productOrigin = VexLifeProdServiceContract.Origin,
        )
    }

    fun run(args: Array<String>) {
        try {
            require(args.isEmpty()) {
                "VexLife Windows lived preview takes no command-line arguments"
            }

            StartupDiagnostics.record("ENTRY")

            val userHome =
                System.getProperty("user.home")
                    ?.takeIf(String::isNotBlank)
                    ?.let(Path::of)
            val homeOverride =
                System.getenv(HomeEnvironment)
                    ?.takeIf(String::isNotBlank)
                    ?.let(Path::of)

            val plan =
                plan(
                    sourceSha = System.getProperty(SourceShaProperty),
                    userHome = userHome,
                    homeOverride = homeOverride,
                    target = VexLifeJvmPlatformTarget.requireCurrent(),
                )

            StartupDiagnostics.record(
                "PLAN home=" + plan.productHome + " source=" + plan.sourceSha,
            )
            VexLifeProdWindowsSetupWindow.open(plan)
        } catch (error: Throwable) {
            fatalStartup(
                phase = "MAIN",
                error = error,
            )
        }
    }

    internal fun fatalStartup(
        phase: String,
        error: Throwable,
    ) {
        val log =
            StartupDiagnostics.recordFailure(
                phase = phase,
                error = error,
            )
        val detail =
            buildString {
                append(error.javaClass.simpleName)
                append(": ")
                append(error.message ?: "VexLife stopped before setup could open.")
                append("\n\nStartup log:\n")
                append(log)
            }

        runCatching {
            if (SwingUtilities.isEventDispatchThread()) {
                JOptionPane.showMessageDialog(
                    null,
                    detail,
                    "VexLife could not open",
                    JOptionPane.ERROR_MESSAGE,
                )
            } else {
                SwingUtilities.invokeAndWait {
                    JOptionPane.showMessageDialog(
                        null,
                        detail,
                        "VexLife could not open",
                        JOptionPane.ERROR_MESSAGE,
                    )
                }
            }
        }

        System.err.println("VEXLIFE_STARTUP_FAILURE\t" + phase + "\t" + log)
        error.printStackTrace(System.err)
        System.err.flush()
    }

    internal object StartupDiagnostics {
        private const val StartupLogName = "windows-startup.log"

        private fun logPath(): Path {
            val localAppData =
                System.getenv("LOCALAPPDATA")
                    ?.takeIf(String::isNotBlank)
                    ?.let(Path::of)
            val base =
                localAppData
                    ?: System.getProperty("user.home")
                        ?.takeIf(String::isNotBlank)
                        ?.let(Path::of)
                        ?.resolve("AppData")
                        ?.resolve("Local")
                    ?: Path.of(System.getProperty("java.io.tmpdir"))
            return base
                .resolve("VexLife")
                .resolve("logs")
                .resolve(StartupLogName)
                .toAbsolutePath()
                .normalize()
        }

        fun record(message: String): Path =
            write(
                OffsetDateTime.now().toString() + "\t" + message + "\n",
            )

        fun recordFailure(
            phase: String,
            error: Throwable,
        ): Path {
            val trace =
                StringWriter().also { writer ->
                    error.printStackTrace(PrintWriter(writer))
                }
                    .toString()
            return write(
                buildString {
                    append(OffsetDateTime.now())
                    append("\tFATAL\t")
                    append(phase)
                    append("\t")
                    append(error.javaClass.name)
                    append("\t")
                    append(error.message ?: "")
                    append("\n")
                    append("sourceSha=")
                    append(System.getProperty(SourceShaProperty).orEmpty())
                    append("\n")
                    append("os.name=")
                    append(System.getProperty("os.name").orEmpty())
                    append("\n")
                    append("os.arch=")
                    append(System.getProperty("os.arch").orEmpty())
                    append("\n")
                    append("java.version=")
                    append(System.getProperty("java.version").orEmpty())
                    append("\n")
                    append("user.home=")
                    append(System.getProperty("user.home").orEmpty())
                    append("\n")
                    append(trace)
                    append("\n")
                },
            )
        }

        private fun write(line: String): Path {
            val primary = logPath()
            return runCatching {
                    Files.createDirectories(requireNotNull(primary.parent))
                    Files.writeString(
                        primary,
                        line,
                        StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND,
                    )
                    primary
                }
                .getOrElse {
                    val fallback =
                        Path.of(System.getProperty("java.io.tmpdir"))
                            .resolve("VexLife-windows-startup.log")
                            .toAbsolutePath()
                            .normalize()
                    runCatching {
                        Files.writeString(
                            fallback,
                            line,
                            StandardCharsets.UTF_8,
                            StandardOpenOption.CREATE,
                            StandardOpenOption.APPEND,
                        )
                    }
                    fallback
                }
        }
    }
}

fun main(args: Array<String>) {
    VexLifeProdWindowsPreview.run(args)
}

// [VXG RealForever]
