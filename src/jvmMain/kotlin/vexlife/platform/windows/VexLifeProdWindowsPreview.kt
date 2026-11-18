package vexlife.platform.windows

import java.awt.Desktop
import java.net.URI
import java.nio.file.Path
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
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
 * [VexLifeProdServiceProcess], waits for that exact process to report the canonical loopback doorway
 * ready, then opens that exact presentation in the person's browser. The preview process stays alive
 * only to retain the service lifetime until the person closes the preview control window.
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

        val selectedHome =
            homeOverride ?: defaultHome.resolve(".vexlife")
        require(selectedHome.isAbsolute) {
            "Windows lived preview Vex Home must be one absolute path"
        }
        val productHome =
            selectedHome.normalize()

        return LaunchPlan(
            sourceSha = exactSourceSha,
            productHome = productHome,
            productOrigin = VexLifeProdServiceContract.Origin,
        )
    }

    fun run(args: Array<String>) {
        require(args.isEmpty()) {
            "VexLife Windows lived preview takes no command-line arguments"
        }

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

        val serviceReady = CountDownLatch(1)
        val serviceFailure = AtomicReference<Throwable?>(null)
        val serviceThread =
            Thread(
                {
                    try {
                        VexLifeProdServiceProcess.runWithHooks(
                            arguments =
                                VexLifeProdServiceProcess.Arguments(
                                    sourceSha = plan.sourceSha,
                                    productHome = plan.productHome,
                                ),
                            hooks =
                                VexLifeProdServiceProcess.Hooks(
                                    reportReady = {
                                        serviceReady.countDown()
                                    },
                                ),
                        )
                    } catch (error: Throwable) {
                        serviceFailure.compareAndSet(null, error)
                        serviceReady.countDown()
                    }
                },
                "vexlife-prod-windows-preview-service",
            )
        serviceThread.isDaemon = false
        serviceThread.start()

        val deadline = System.nanoTime() + TimeUnit.MINUTES.toNanos(5)
        while (System.nanoTime() < deadline) {
            if (serviceReady.await(250, TimeUnit.MILLISECONDS)) {
                serviceFailure.get()?.let { error ->
                    showFailure(
                        "VexLife could not start.",
                        error.javaClass.simpleName + ": " + (error.message ?: "unknown failure"),
                    )
                    throw error
                }

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
        }

        showFailure(
            "VexLife is taking longer than expected to start.",
            "The exact Product service did not report ready within five minutes.",
        )
        error("VexLife Windows lived preview service readiness timed out")
    }

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
