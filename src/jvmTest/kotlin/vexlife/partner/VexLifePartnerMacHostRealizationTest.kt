package vexlife.partner

import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermission
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class VexLifePartnerMacHostRealizationTest {
    @Test
    fun exactObservedHelperFormsZeroArgumentProcessHostPlan() {
        val root =
            Files.createTempDirectory(
                "vexlife-partner-macos-host-realization-",
            )
                .toAbsolutePath()
        val helper =
            root.resolve(
                "vexlife-partner-host",
            )

        try {
            Files.writeString(
                helper,
                "fixture",
            )
            runCatching {
                Files.setPosixFilePermissions(
                    helper,
                    setOf(
                        PosixFilePermission.OWNER_READ,
                        PosixFilePermission.OWNER_WRITE,
                        PosixFilePermission.OWNER_EXECUTE,
                    ),
                )
            }

            if (
                Files.isExecutable(
                    helper,
                )
            ) {
                val plan =
                    VexLifePartnerMacHostRealization.plan(
                        helper.toString(),
                    )
                assertEquals(
                    helper,
                    plan.helperExecutable,
                )
            }
        } finally {
            root
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun missingHelperIsRejectedBeforeProcessLaunch() {
        val root =
            Files.createTempDirectory(
                "vexlife-partner-macos-host-missing-",
            )
                .toAbsolutePath()
        try {
            assertFailsWith<IllegalArgumentException> {
                VexLifePartnerMacHostRealization.plan(
                    root
                        .resolve(
                            "missing-helper",
                        )
                        .toString(),
                )
            }
        } finally {
            root
                .toFile()
                .deleteRecursively()
        }
    }
}

// [VXG RealForever]
