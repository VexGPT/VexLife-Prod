package vexlife.setup

import java.nio.file.Path
import vexlife.home.VexLifeProdHomeFiles
import vexlife.local.VexLifeProdLocalSetup
import vexlife.local.VexLifeProdRuntimeSession
import vexlife.local.VexLifeProdSetupProgress
import vexlife.model.heretic.HereticRuntimeRealization
import vexlife.service.VexLifeProdMacServiceLifetime
import vexlife.service.VexLifeProdServiceLifetime

/**
 * Platform realization consumed by the shared Product setup process.
 *
 * Setup owns sequencing and authorization. Platform implementations own how Home eligibility,
 * runtime establishment, and long-lived service lifetime are realized for one exact target.
 */
internal interface VexLifeProdSetupPlatform {
    val target: HereticRuntimeRealization.Target

    fun formFreshHome(
        productHome: Path,
    ): VexLifeProdHomeFiles.Result

    fun establishSession(
        productHome: Path,
        allowAcquisition: Boolean,
        allowRuntimeStart: Boolean,
        progress: VexLifeProdSetupProgress,
    ): VexLifeProdRuntimeSession

    fun startService(
        productHome: Path,
        expectedSourceSha: String,
    ): VexLifeProdServiceLifetime
}

/**
 * Existing macOS behavior behind the shared setup platform seam.
 *
 * No Mac semantics move into shared setup: this adapter delegates to the current Mac owners.
 */
internal object VexLifeProdMacSetupPlatform :
    VexLifeProdSetupPlatform {
    override val target =
        HereticRuntimeRealization.MacArm64

    override fun formFreshHome(
        productHome: Path,
    ): VexLifeProdHomeFiles.Result =
        VexLifeProdLocalSetup.formFreshHome(
            productHome,
        )

    override fun establishSession(
        productHome: Path,
        allowAcquisition: Boolean,
        allowRuntimeStart: Boolean,
        progress: VexLifeProdSetupProgress,
    ): VexLifeProdRuntimeSession =
        VexLifeProdLocalSetup.establishSession(
            productHome = productHome,
            allowAcquisition = allowAcquisition,
            allowRuntimeStart = allowRuntimeStart,
            progress = progress,
        )

    override fun startService(
        productHome: Path,
        expectedSourceSha: String,
    ): VexLifeProdServiceLifetime =
        VexLifeProdMacServiceLifetime.startOrReuse(
            productHome = productHome,
            desiredSourceSha = expectedSourceSha,
        )
}

// [VXG RealForever]
