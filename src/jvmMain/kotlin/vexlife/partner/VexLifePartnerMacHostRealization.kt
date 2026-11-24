package vexlife.partner

import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import vexlife.packaging.VexLifeProdPrivateServiceImage

/**
 * Exact macOS realization of the source-owned native Partner host launcher.
 *
 * The helper path comes from private service-image observation, which already validates its exact
 * manifest path/SHA/executable identity. This realization re-checks the local executable boundary
 * before forming the fixed zero-argument process host.
 */
object VexLifePartnerMacHostRealization {
    data class LaunchPlan(
        val helperExecutable: Path,
    )

    fun plan(
        helperExecutablePath: String?,
    ): LaunchPlan {
        val helper =
            requireNotNull(
                helperExecutablePath
                    ?.takeIf(
                        String::isNotBlank,
                    )
                    ?.let(
                        Path::of,
                    ),
            ) {
                "macOS Partner host requires the observed private service-image helper path"
            }
                .toAbsolutePath()
                .normalize()

        require(
            Files.isRegularFile(
                helper,
                LinkOption.NOFOLLOW_LINKS,
            ) &&
                !Files.isSymbolicLink(
                    helper,
                ) &&
                Files.isExecutable(
                    helper,
                ),
        ) {
            "macOS Partner helper path must be one executable regular non-symlink file"
        }

        return LaunchPlan(
            helperExecutable = helper,
        )
    }

    fun fromObservation(
        observation: VexLifeProdPrivateServiceImage.Observation,
    ): LaunchPlan =
        plan(
            observation.partnerHostExecutablePathOrNull,
        )

    fun processHost(
        plan: LaunchPlan,
    ): VexLifePartnerNativeHostClient.SystemProcessHost =
        VexLifePartnerNativeHostClient.SystemProcessHost(
            executable = plan.helperExecutable,
        )
}

// [VXG RealForever]
