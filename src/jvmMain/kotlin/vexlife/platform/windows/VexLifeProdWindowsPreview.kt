package vexlife.platform.windows

import java.nio.file.Path
import vexlife.model.heretic.HereticRuntimeRealization
import vexlife.platform.VexLifeJvmPlatformTarget
import vexlife.platform.VexLifePlatformRealization
import vexlife.service.VexLifeProdServiceContract

/**
 * Human-facing Windows lived release entry.
 *
 * The executable begins at the same Product-entry relationship as the Mac application: observe the
 * selected Vex Home, let the portable Entry Family decide which Controls are lawful, and require
 * separate human choices for Home formation and model/runtime setup. Windows-specific acquisition
 * and runtime realization stay behind their existing owners.
 *
 * For this pre-merge lived preview the executable process itself retains Product-service/runtime
 * custody. Final Windows installer/background-service lifetime remains a later decision informed by
 * this lived experience.
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

        VexLifeProdWindowsSetupWindow.open(plan)
    }
}

fun main(args: Array<String>) {
    VexLifeProdWindowsPreview.run(args)
}

// [VXG RealForever]
