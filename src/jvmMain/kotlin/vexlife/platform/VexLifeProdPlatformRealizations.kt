package vexlife.platform

import vexlife.model.heretic.HereticProductIdentity
import vexlife.packaging.VexLifeProdPrivateServiceImage
import vexlife.platform.macos.MacUserLaunchdProcess

/**
 * Current VexLife-Prod target realization catalog.
 *
 * This is deliberately only a composition map over existing owners. It does not replace
 * HereticMacHostProfile, MacHereticRuntimePlatform, MacUserLaunchdProcess, the private image, or
 * their currentness/effect responsibilities.
 *
 * The first entry describes the exact Apple-silicon realization already present on the #138 source
 * line. Missing Windows/Intel/mobile entries remain unsupported instead of falling back to Mac.
 */
object VexLifeProdPlatformRealizations {
    val MacAppleSilicon =
        VexLifePlatformRealization.Entry(
            realizationRef = "realization.vexlife-prod.macos.arm64.001",
            target =
                VexLifePlatformRealization.Target(
                    platform = VexLifePlatformRealization.PlatformFamily.MACOS,
                    architecture = VexLifePlatformRealization.Architecture.ARM64,
                ),
            dependencies =
                VexLifePlatformRealization.DependencyRefs(
                    hostExecutionProfileRef = HereticProductIdentity.HostExecutionProfileRef,
                    modelRuntimeAdapterRef = HereticProductIdentity.RuntimeAdapterRef,
                    modelRuntimeClass = HereticProductIdentity.RuntimeClass,
                    modelTransportClass = HereticProductIdentity.TransportClass,
                    processSubstrateRef = MacUserLaunchdProcess.SubstrateRef,
                    privateServiceImagePlatform = VexLifeProdPrivateServiceImage.Platform,
                ),
        )

    val Catalog =
        VexLifePlatformRealization.Catalog(
            entries =
                listOf(
                    MacAppleSilicon,
                ),
        )
}

// [VXG RealForever]
