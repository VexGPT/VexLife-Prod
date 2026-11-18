package vexlife.platform.windows

import java.nio.file.Path
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import vexlife.home.VexLifeProdHome
import vexlife.home.VexLifeProdHomeFiles

class VexLifeWindowsReadinessTest {
    @Test
    fun formableHomeProjectsCurrentHostTruthAndKeepsWindowsRealizationUnavailable() {
        val home =
            Path.of(
                System.getProperty(
                    "user.home",
                ),
            )
                .toAbsolutePath()
                .normalize()
                .resolve(
                    ".vexlife-readiness-test",
                )

        val projection =
            VexLifeWindowsReadiness.observeWithHooks(
                productHome = home,
                hooks =
                    VexLifeWindowsReadiness.Hooks(
                        observeHost = {
                            VexLifeWindowsHostObservation.resolve(
                                VexLifeWindowsHostObservation.Raw(
                                    osName = "Windows 11",
                                    jvmOsArch = "amd64",
                                    processorArchitecture = "AMD64",
                                    processorArchitectureW6432 = null,
                                ),
                            )
                        },
                        observeHome = {
                            selected ->
                            VexLifeProdHomeFiles.Location.Formable(
                                productHome =
                                    selected
                                        .toString(),
                                disposition =
                                    VexLifeProdHome.LocationDisposition.Absent,
                                nearestExistingAncestor =
                                    requireNotNull(
                                        selected.parent,
                                    )
                                        .toString(),
                                missingAncestorCount = 0,
                            )
                        },
                        totalMemoryBytes = {
                            32L *
                                1024L *
                                1024L *
                                1024L
                        },
                        freeStorageBytes = {
                            512L *
                                1024L *
                                1024L *
                                1024L
                        },
                        now = {
                            Instant.parse(
                                "2026-11-17T09:00:00Z",
                            )
                        },
                    ),
            )

        assertEquals(
            VexLifeWindowsReadiness.Schema,
            projection.schemaVersion,
        )
        assertEquals(
            "WINDOWS",
            projection.platform,
        )
        assertEquals(
            "X86_64",
            projection.architecture,
        )
        assertEquals(
            "X86_64",
            projection.processArchitecture,
        )
        assertEquals(
            "PROCESSOR_ARCHITECTURE",
            projection.architectureEvidence,
        )
        assertEquals(
            32L *
                1024L *
                1024L *
                1024L,
            projection.totalMemoryBytes,
        )
        assertEquals(
            512L *
                1024L *
                1024L *
                1024L,
            projection.freeStorageBytes,
        )
        assertEquals(
            VexLifeProdHome.LocationDisposition.Absent.wireValue,
            projection.homeDisposition,
        )
        assertNull(
            projection.homeRef,
        )
        assertNull(
            projection.deviceRef,
        )
        assertNull(
            projection.companionLineageRef,
        )
        assertEquals(
            VexLifeWindowsReadiness.RuntimeUnavailable,
            projection.runtimeState,
        )
        assertEquals(
            VexLifeWindowsReadiness.ServiceUnavailable,
            projection.serviceState,
        )
        assertEquals(
            VexLifeWindowsReadiness.NativeSetupUnavailable,
            projection.nativeSetupState,
        )
        assertEquals(
            VexLifeWindowsReadiness.PackageUnavailable,
            projection.packageState,
        )
        assertFalse(
            projection.effectsPerformed,
        )
    }

    @Test
    fun nonWindowsObservationCannotBecomeWindowsReadiness() {
        val home =
            Path.of(
                System.getProperty(
                    "user.home",
                ),
            )
                .toAbsolutePath()
                .normalize()
                .resolve(
                    ".vexlife-readiness-test",
                )

        assertFailsWith<IllegalStateException> {
            VexLifeWindowsReadiness.observeWithHooks(
                productHome = home,
                hooks =
                    VexLifeWindowsReadiness.Hooks(
                        observeHost = {
                            VexLifeWindowsHostObservation.resolve(
                                VexLifeWindowsHostObservation.Raw(
                                    osName = "Mac OS X",
                                    jvmOsArch = "aarch64",
                                    processorArchitecture = null,
                                    processorArchitectureW6432 = null,
                                ),
                            )
                        },
                    ),
            )
        }
    }
}

// [VXG RealForever]
