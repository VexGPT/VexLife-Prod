package vexlife.platform.windows

import java.awt.Desktop
import java.net.HttpURLConnection
import java.net.URI
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicReference
import javax.swing.JOptionPane
import vexlife.model.heretic.HereticRuntimeRealization
import vexlife.platform.VexLifeJvmPlatformTarget
import vexlife.platform.VexLifePlatformRealization
import vexlife.service.VexLifeProdServiceContract
import vexlife.service.VexLifeProdServiceProcess

/**
 * Human-facing Windows lived-preview entry.
 *
 * This is intentionally not a second Product/service owner. It starts the already-owned portable
 * [VexLifeProdServiceProcess], waits for the canonical loopback doorway, then opens that exact
 * presentation in the person's browser. The preview process stays alive only to retain the service
 * lifetime until the person closes the preview control window.
 *
 * Final installer/service-lifetime semantics remain a later acceptance question informed by lived
 * Windows evidence from this exact Product path.
 */
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
        userHome: String?,
        homeOverride: String?,
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
            requireNotNull(userHome?.takeIf(String::isNotBlank)) {
                "Windows lived preview requires the current user Home"
            }

        val productHome =
            homeOverride
                ?.takeIf(String::isNotBlank)
                ?.let(Path::of)
                ?: Path.of(defaultHome, ".vexlife")

        require(productHome.isAbsolute) {
            "Windows lived preview Vex Home must be one absolute path"
        }

        return LaunchPlan(
            sourceSha = exactSourceSha,
            productHome = productHome.normalize(),
            productOrigin = VexLifeProdServiceContract.Origin,
        )
    }

    fun run(args: Array<String>) {
        require(args.isEmpty()) {
            "VexLife Windows lived preview takes no command-line arguments"
        }

        val plan =
            plan(
                sourceSha = System.getProperty(SourceShaProperty),
                userHome = System.getProperty("user.home"),
                homeOverride = System.getenv(HomeEnvironment),
                target = VexLifeJvmPlatformTarget.requireCurrent(),
            )

        val serviceFailure = AtomicReference<Throwable?>(null)
        val serviceThread =
            Thread(
                {
                    try {
                        VexLifeProdServiceProcess.run(
                            arrayOf(
                                "--source-sha",
                                plan.sourceSha,
                                "--home",
                                plan.productHome.toString(),
                            )
                        )
                    } catch (error: Throwable) {
                        serviceFailure.compareAndSet(null, error)
                    }
                },
                "vexlife-prod-windows-preview-service",
            )
        serviceThread.isDaemon = false
        serviceThread.start()

        val deadline = System.nanoTime() + 300_000_000_000L
        while (System.nanoTime() < deadline) {
            serviceFailure.get()?.let { error ->
                showFailure(
                    "VexLife could not start.",
                    error.javaClass.simpleName + ": " + (error.message ?: "unknown failure"),
                )
                throw error
            }

            if (serviceReady()) {
                openProduct(plan.productOrigin)
                JOptionPane.showMessageDialog(
                    null,
                    "VexLife is running in your browser.\n\n" +
                        "Keep this preview window open while you use VexLife. " +
                        "Close it when you want to stop the Windows preview.",
                    "VexLife",
                    JOptionPane.INFORMATION_MESSAGE,
                )
                System.exit(0)
            }

            Thread.sleep(250)
        }

        showFailure(
            "VexLife is taking longer than expected to start.",
            "The local Product service did not become reachable within five minutes.",
        )
        error("VexLife Windows lived preview service readiness timed out")
    }

    private fun serviceReady(): Boolean =
        runCatching {
                val connection =
                    URI.create(
                            VexLifeProdServiceContract.Origin +
                                VexLifeProdServiceContract.SourceCurrentnessPath
                        )
                        .toURL()
                        .openConnection() as HttpURLConnection
                try {
                    connection.requestMethod = "GET"
                    connection.connectTimeout = 500
                    connection.readTimeout = 500
                    connection.instanceFollowRedirects = false
                    connection.responseCode == 200
                } finally {
                    connection.disconnect()
                }
            }
            .getOrDefault(false)

    private fun openProduct(origin: String) {
        val opened =
            runCatching {
                    require(Desktop.isDesktopSupported()) {
                        "Windows desktop integration is unavailable"
                    }
                    val desktop = Desktop.getDesktop()
                    require(desktop.isSupported(Desktop.Action.BROWSE)) {
                        "Windows browser launch is unavailable"
                    }
                    desktop.browse(URI.create(origin))
                }
                .isSuccess

        if (!opened) {
            JOptionPane.showMessageDialog(
                null,
                "VexLife is ready. Open this address in your browser:\n\n$origin",
                "VexLife",
                JOptionPane.INFORMATION_MESSAGE,
            )
        }
    }

    private fun showFailure(title: String, detail: String) {
        runCatching {
            JOptionPane.showMessageDialog(
                null,
                detail,
                title,
                JOptionPane.ERROR_MESSAGE,
            )
        }
    }
}

fun main(args: Array<String>) {
    VexLifeProdWindowsPreview.run(args)
}

// [VXG RealForever]
