package vexlife.partner

import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path

/**
 * Exact Windows realization of the source-owned native Partner host launcher.
 *
 * jpackage supplies [HelperPathProperty] through its runtime APPDIR macro. This owner resolves only
 * that packaged helper path plus the current Windows PowerShell executable. Provider/credential/
 * request data never becomes launcher argv.
 */
object VexLifePartnerWindowsHostRealization {
    const val HelperPathProperty =
        "vexlife.prod.partnerHost"

    data class LaunchPlan(
        val powershellExecutable: Path,
        val helperScript: Path,
        val fixedArguments: List<String>,
    )

    fun plan(
        helperPathProperty: String?,
        systemRoot: String?,
    ): LaunchPlan {
        val helper =
            requireNotNull(
                helperPathProperty
                    ?.takeIf(
                        String::isNotBlank,
                    )
                    ?.let(
                        Path::of,
                    ),
            ) {
                "Windows Partner host requires the jpackage-projected helper path"
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
                ),
        ) {
            "Windows Partner helper path must resolve to one regular non-symlink packaged script"
        }

        val root =
            requireNotNull(
                systemRoot
                    ?.takeIf(
                        String::isNotBlank,
                    )
                    ?.let(
                        Path::of,
                    ),
            ) {
                "Windows Partner host requires the current Windows system root"
            }
                .toAbsolutePath()
                .normalize()
        val powershell =
            root
                .resolve(
                    "System32",
                )
                .resolve(
                    "WindowsPowerShell",
                )
                .resolve(
                    "v1.0",
                )
                .resolve(
                    "powershell.exe",
                )
                .normalize()
        require(
            Files.isRegularFile(
                powershell,
                LinkOption.NOFOLLOW_LINKS,
            ) &&
                !Files.isSymbolicLink(
                    powershell,
                ) &&
                Files.isExecutable(
                    powershell,
                ),
        ) {
            "Windows Partner host requires the source-owned Windows PowerShell launcher"
        }

        return LaunchPlan(
            powershellExecutable = powershell,
            helperScript = helper,
            fixedArguments =
                listOf(
                    "-NoProfile",
                    "-NonInteractive",
                    "-ExecutionPolicy",
                    "Bypass",
                    "-File",
                    helper.toString(),
                ),
        )
    }

    fun processHost(
        plan: LaunchPlan,
    ): VexLifePartnerNativeHostClient.SystemProcessHost =
        VexLifePartnerNativeHostClient.SystemProcessHost(
            executable = plan.powershellExecutable,
            fixedArguments = plan.fixedArguments,
        )

    fun current(): VexLifePartnerNativeHostClient.SystemProcessHost =
        processHost(
            plan(
                helperPathProperty =
                    System.getProperty(
                        HelperPathProperty,
                    ),
                systemRoot =
                    System.getenv(
                        "SystemRoot",
                    ),
            ),
        )
}

// [VXG RealForever]
