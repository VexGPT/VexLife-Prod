package vexlife.platform.windows

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import vexlife.model.heretic.HereticRuntimeRealization

class VexLifeProdWindowsPreviewTest {
    private val sourceSha = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"

    @Test
    fun defaultsToCanonicalHomeAndCanonicalProductOrigin() {
        val userHome =
            Files.createTempDirectory("vexlife-preview-user-home-")
                .toAbsolutePath()

        val plan =
            VexLifeProdWindowsPreview.plan(
                sourceSha = sourceSha,
                userHome = userHome,
                homeOverride = null,
                target = HereticRuntimeRealization.WindowsX64,
            )

        assertEquals(
            userHome.resolve(".vexlife"),
            plan.productHome,
        )
        assertEquals(
            sourceSha,
            plan.sourceSha,
        )
        assertEquals(
            "http://127.0.0.1:18210",
            plan.productOrigin,
        )
    }

    @Test
    fun explicitHomeOverrideRemainsOneAbsoluteCoordinate() {
        val userHome =
            Files.createTempDirectory("vexlife-preview-user-home-")
                .toAbsolutePath()
        val alternate =
            Files.createTempDirectory("vexlife-preview-alternate-home-")
                .toAbsolutePath()

        val plan =
            VexLifeProdWindowsPreview.plan(
                sourceSha = sourceSha,
                userHome = userHome,
                homeOverride = alternate,
                target = HereticRuntimeRealization.WindowsX64,
            )

        assertEquals(
            alternate,
            plan.productHome,
        )
    }

    @Test
    fun refusesNonWindowsTargetOrUnboundSource() {
        val userHome =
            Files.createTempDirectory("vexlife-preview-user-home-")
                .toAbsolutePath()

        assertFailsWith<IllegalArgumentException> {
            VexLifeProdWindowsPreview.plan(
                sourceSha = sourceSha,
                userHome = userHome,
                homeOverride = null,
                target = HereticRuntimeRealization.MacArm64,
            )
        }

        assertFailsWith<IllegalArgumentException> {
            VexLifeProdWindowsPreview.plan(
                sourceSha = "not-a-source-sha",
                userHome = userHome,
                homeOverride = null,
                target = HereticRuntimeRealization.WindowsX64,
            )
        }
    }
}

// [VXG RealForever]
