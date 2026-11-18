package vexlife.local

import java.nio.file.Path
import vexlife.model.heretic.HereticRuntimeRealization
import vexlife.platform.VexLifeJvmPlatformTarget
import vexlife.platform.windows.VexLifeProdWindowsRuntimeSession

/**
 * Construction seam from one exact runtime realization to one portable Product runtime Session.
 *
 * It owns only dependency construction/routing. Mac and Windows runtime semantics remain with their
 * concrete owners.
 */
internal object VexLifeProdRuntimeSessionFactory {
    fun establishCurrent(
        productHome: Path,
        allowAcquisition: Boolean,
        allowRuntimeStart: Boolean,
        progress: VexLifeProdSetupProgress = VexLifeProdSetupProgress.Silent,
    ): VexLifeProdRuntimeSession {
        val manifest =
            requireNotNull(
                vexlife.home.VexLifeProdHomeFiles.observeExisting(
                    productHome,
                ),
            ) {
                "runtime Session factory requires one existing canonical Vex Home"
            }
                .manifest
        val selected =
            vexlife.model.heretic.runtime
                .FileHereticModelConfigurationStore(
                    productHome,
                )
                .selectedModel(
                    manifest,
                )
        val resolver =
            HereticRuntimeRealization.Resolver(
                HereticRuntimeRealization.TargetSource(
                    VexLifeJvmPlatformTarget::requireCurrent,
                ),
            )
        val realization =
            when (
                val resolution =
                    resolver.resolve(
                        selected,
                    )
            ) {
                is HereticRuntimeRealization.Resolution.Supported ->
                    resolution.realization

                is HereticRuntimeRealization.Resolution.Unsupported ->
                    error(
                        "selected Product model has no runtime realization on current target " +
                            HereticRuntimeRealization.targetRef(
                                resolution.target,
                            ),
                    )
            }

        return establishExact(
            productHome = productHome,
            realization = realization,
            allowAcquisition = allowAcquisition,
            allowRuntimeStart = allowRuntimeStart,
            progress = progress,
        )
    }

    fun establishExact(
        productHome: Path,
        realization: HereticRuntimeRealization.Value,
        allowAcquisition: Boolean,
        allowRuntimeStart: Boolean,
        progress: VexLifeProdSetupProgress = VexLifeProdSetupProgress.Silent,
    ): VexLifeProdRuntimeSession {
        require(
            realization.target ==
                VexLifeJvmPlatformTarget.requireCurrent(),
        ) {
            "runtime Session realization target does not match the current JVM host"
        }

        return when (
            realization.target
        ) {
            HereticRuntimeRealization.MacArm64 ->
                VexLifeProdLocalSetup.establishSession(
                    productHome = productHome,
                    allowAcquisition = allowAcquisition,
                    allowRuntimeStart = allowRuntimeStart,
                    progress = progress,
                )

            HereticRuntimeRealization.WindowsX64 ->
                VexLifeProdWindowsRuntimeSession.establish(
                    productHome = productHome,
                    realization = realization,
                    allowAcquisition = allowAcquisition,
                    allowRuntimeStart = allowRuntimeStart,
                    progress = progress,
                )

            else ->
                error(
                    "runtime Session has no Product realization for target " +
                        HereticRuntimeRealization.targetRef(
                            realization.target,
                        ),
                )
        }
    }
}

// [VXG RealForever]
