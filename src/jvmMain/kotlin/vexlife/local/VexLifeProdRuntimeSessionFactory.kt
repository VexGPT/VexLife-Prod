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
    /**
     * Long-lived Product service construction.
     *
     * Mac keeps its existing reuse-only service-process boundary. Windows service process owns the
     * native llama.cpp child and may start it from already-materialized exact dependencies.
     */
    fun establishForServiceProcess(
        productHome: Path,
    ): VexLifeProdRuntimeSession {
        val realization =
            selectedRealization(
                productHome,
            )
        return establishExact(
            productHome = productHome,
            realization = realization,
            allowAcquisition = false,
            allowRuntimeStart =
                realization.target ==
                    HereticRuntimeRealization.WindowsX64,
        )
    }

    fun establishCurrent(
        productHome: Path,
        allowAcquisition: Boolean,
        allowRuntimeStart: Boolean,
        progress: VexLifeProdSetupProgress = VexLifeProdSetupProgress.Silent,
    ): VexLifeProdRuntimeSession {
        val realization =
            selectedRealization(
                productHome,
            )

        return establishExact(
            productHome = productHome,
            realization = realization,
            allowAcquisition = allowAcquisition,
            allowRuntimeStart = allowRuntimeStart,
            progress = progress,
        )
    }

    private fun selectedRealization(
        productHome: Path,
    ): HereticRuntimeRealization.Value {
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
        return when (
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
