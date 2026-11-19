package vexlife.platform.windows

import com.sun.management.OperatingSystemMXBean
import java.lang.management.ManagementFactory
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import vexlife.home.VexLifeProdHomeFiles
import vexlife.platform.VexLifePlatformRealization

/**
 * Read-only Windows readiness projection before a Windows Product realization exists.
 *
 * This owner answers only current host/Home facts plus which Windows realization leaves are still
 * unavailable. It does not form a Home, choose/download a model, start a runtime/service, install
 * VexLife, or publish a release.
 *
 * The existing VexLifeProdCurrentState owner remains the current Mac Product convergence owner. This
 * projection is intentionally smaller until Windows runtime/service/native-setup owners actually
 * exist; it does not pretend that "host observed" means "Product supported".
 */
object VexLifeWindowsReadiness {
    const val Schema =
        "vexlife-prod.windows-readiness/v1"

    const val RuntimeUnavailable =
        "WINDOWS_RUNTIME_REALIZATION_UNAVAILABLE"

    const val ServiceUnavailable =
        "WINDOWS_PRODUCT_SERVICE_UNAVAILABLE"

    const val NativeSetupUnavailable =
        "WINDOWS_NATIVE_SETUP_UNAVAILABLE"

    const val PackageUnavailable =
        "WINDOWS_PACKAGE_UNAVAILABLE"

    data class Projection(
        val schemaVersion: String,
        val observedAt: String,
        val platform: String,
        val architecture: String,
        val processArchitecture: String?,
        val architectureEvidence: String,
        val totalMemoryBytes: Long?,
        val freeStorageBytes: Long?,
        val homePath: String,
        val homeDisposition: String,
        val homeRef: String?,
        val deviceRef: String?,
        val companionLineageRef: String?,
        val runtimeState: String,
        val serviceState: String,
        val nativeSetupState: String,
        val packageState: String,
        val effectsPerformed: Boolean,
    ) {
        init {
            val homeIdentityPresent =
                homeRef != null ||
                    deviceRef != null ||
                    companionLineageRef != null

            require(
                schemaVersion == Schema &&
                    observedAt.isNotBlank() &&
                    platform == "WINDOWS" &&
                    architecture in setOf("X86_64", "ARM64") &&
                    architectureEvidence.isNotBlank() &&
                    homePath.isNotBlank() &&
                    homeDisposition.isNotBlank() &&
                    runtimeState == RuntimeUnavailable &&
                    serviceState == ServiceUnavailable &&
                    nativeSetupState == NativeSetupUnavailable &&
                    packageState == PackageUnavailable &&
                    !effectsPerformed,
            ) {
                "Windows readiness projection fixed boundary is invalid"
            }

            require(
                !homeIdentityPresent ||
                    (
                        !homeRef.isNullOrBlank() &&
                            !deviceRef.isNullOrBlank() &&
                            !companionLineageRef.isNullOrBlank()
                    ),
            ) {
                "Windows readiness Home identity must be all-present or all-absent"
            }

            require(
                totalMemoryBytes == null ||
                    totalMemoryBytes >= 0,
            ) {
                "Windows readiness total memory cannot be negative"
            }
            require(
                freeStorageBytes == null ||
                    freeStorageBytes >= 0,
            ) {
                "Windows readiness free storage cannot be negative"
            }
        }
    }

    internal data class Hooks(
        val observeHost: () -> VexLifeWindowsHostObservation.Result =
            VexLifeWindowsHostObservation::current,
        val observeHome: (Path) -> VexLifeProdHomeFiles.Location =
            VexLifeProdHomeFiles::observeLocation,
        val totalMemoryBytes: () -> Long? =
            ::currentTotalMemoryBytes,
        val freeStorageBytes: (Path) -> Long? =
            ::currentFreeStorageBytes,
        val now: () -> Instant =
            Instant::now,
    )

    fun observe(
        productHome: Path,
    ): Projection =
        observeWithHooks(
            productHome = productHome,
            hooks = Hooks(),
        )

    internal fun observeWithHooks(
        productHome: Path,
        hooks: Hooks,
    ): Projection {
        require(
            productHome.isAbsolute,
        ) {
            "Windows readiness requires one absolute selected/proposed Vex Home coordinate"
        }

        val selectedHome =
            productHome
                .normalize()

        val host =
            when (
                val observed =
                    hooks.observeHost()
            ) {
                is VexLifeWindowsHostObservation.Result.Resolved ->
                    observed

                is VexLifeWindowsHostObservation.Result.UnsupportedHost ->
                    error(
                        "Windows readiness requires a Windows host",
                    )

                is VexLifeWindowsHostObservation.Result.UnsupportedArchitecture ->
                    error(
                        "Windows readiness cannot resolve the host architecture",
                    )
            }

        require(
            host.target.platform ==
                VexLifePlatformRealization.PlatformFamily.WINDOWS,
        ) {
            "Windows readiness host observation resolved a non-Windows target"
        }

        val home =
            hooks.observeHome(
                selectedHome,
            )
        require(
            Path.of(
                home.productHome,
            )
                .normalize() ==
                selectedHome,
        ) {
            "Windows readiness Home observer changed the selected coordinate"
        }

        val identity =
            when (
                home
            ) {
                is VexLifeProdHomeFiles.Location.Existing ->
                    Triple(
                        home.home.manifest.homeRef,
                        home.home.installation.deviceRef,
                        home.home.installation.companionLineageRef,
                    )

                is VexLifeProdHomeFiles.Location.Formable,
                is VexLifeProdHomeFiles.Location.Held,
                ->
                    Triple(
                        null,
                        null,
                        null,
                    )
            }

        return Projection(
            schemaVersion = Schema,
            observedAt =
                hooks
                    .now()
                    .toString(),
            platform = "WINDOWS",
            architecture =
                host
                    .nativeArchitecture
                    .name,
            processArchitecture =
                host
                    .processArchitecture
                    ?.name,
            architectureEvidence =
                host
                    .architectureEvidence
                    .name,
            totalMemoryBytes =
                hooks.totalMemoryBytes(),
            freeStorageBytes =
                hooks.freeStorageBytes(
                    selectedHome,
                ),
            homePath =
                selectedHome
                    .toString(),
            homeDisposition =
                home
                    .disposition
                    .wireValue,
            homeRef = identity.first,
            deviceRef = identity.second,
            companionLineageRef = identity.third,
            runtimeState = RuntimeUnavailable,
            serviceState = ServiceUnavailable,
            nativeSetupState = NativeSetupUnavailable,
            packageState = PackageUnavailable,
            effectsPerformed = false,
        )
    }

    private fun currentTotalMemoryBytes(): Long? =
        runCatching {
                (
                    ManagementFactory
                        .getOperatingSystemMXBean() as? OperatingSystemMXBean
                )
                    ?.totalMemorySize
                    ?.takeIf {
                        it >= 0
                    }
            }
            .getOrNull()

    private fun currentFreeStorageBytes(
        productHome: Path,
    ): Long? =
        runCatching {
                var observed: Path? =
                    productHome
                while (
                    observed != null &&
                    !Files.exists(
                        observed,
                    )
                ) {
                    observed =
                        observed.parent
                }

                Files
                    .getFileStore(
                        requireNotNull(
                            observed,
                        ),
                    )
                    .usableSpace
            }
            .getOrNull()
}

// [VXG RealForever]
