package vexlife.platform

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import vexlife.model.heretic.HereticProductIdentity
import vexlife.packaging.VexLifeProdPrivateServiceImage
import vexlife.platform.macos.MacUserLaunchdProcess

class VexLifeProdPlatformRealizationsTest {
    @Test
    fun appleSiliconRealizationReferencesTheCurrentSourceOwnersExactly() {
        val entry = VexLifeProdPlatformRealizations.MacAppleSilicon

        assertEquals(
            VexLifePlatformRealization.Target(
                platform = VexLifePlatformRealization.PlatformFamily.MACOS,
                architecture = VexLifePlatformRealization.Architecture.ARM64,
            ),
            entry.target,
        )
        assertEquals(
            HereticProductIdentity.HostExecutionProfileRef,
            entry.dependencies.hostExecutionProfileRef,
        )
        assertEquals(
            HereticProductIdentity.RuntimeAdapterRef,
            entry.dependencies.modelRuntimeAdapterRef,
        )
        assertEquals(
            HereticProductIdentity.RuntimeClass,
            entry.dependencies.modelRuntimeClass,
        )
        assertEquals(
            HereticProductIdentity.TransportClass,
            entry.dependencies.modelTransportClass,
        )
        assertEquals(
            MacUserLaunchdProcess.SubstrateRef,
            entry.dependencies.processSubstrateRef,
        )
        assertEquals(
            VexLifeProdPrivateServiceImage.Platform,
            entry.dependencies.privateServiceImagePlatform,
        )
    }

    @Test
    fun unregisteredTargetsRemainUnsupportedInsteadOfBorrowingTheMacRealization() {
        assertIs<VexLifePlatformRealization.Resolution.Unsupported>(
            VexLifeProdPlatformRealizations.Catalog.resolve(
                VexLifePlatformRealization.Target(
                    platform = VexLifePlatformRealization.PlatformFamily.MACOS,
                    architecture = VexLifePlatformRealization.Architecture.X86_64,
                ),
            ),
        )
        assertIs<VexLifePlatformRealization.Resolution.Unsupported>(
            VexLifeProdPlatformRealizations.Catalog.resolve(
                VexLifePlatformRealization.Target(
                    platform = VexLifePlatformRealization.PlatformFamily.WINDOWS,
                    architecture = VexLifePlatformRealization.Architecture.X86_64,
                ),
            ),
        )
    }
}

// [VXG RealForever]
