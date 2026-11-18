package vexlife.platform.windows

import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import vexlife.model.heretic.HereticRuntimeRealization

class VexLifeProdWindowsPreviewTest {
    @Test
    fun defaultsToCanonicalHomeAndCanonicalProductOrigin() {
        val plan =
            VexLifeProdWindowsPreview.plan(
                sourceSha = "470c02c9ac268118b0a5f5cf8b34261f096c9899",
                userHome = "C:\\Users\\Victor",
                homeOverride = null,
                target = HereticRuntimeRealization.WindowsX64,
            )

        assertEquals(
            Path.of("C:\\Users\\Victor", ".vexlife"),
            plan.productHome,
        )
        assertEquals(
            "470c02c9ac268118b0a5f5cf8b34261f096c9899",
            plan.sourceSha,
        )
        assertEquals(
            "http://127.0.0.1:18210",
            plan.productOrigin,
        )
    }

    @Test
    fun explicitHomeOverrideRemainsOneAbsoluteCoordinate() {
        val plan =
            VexLifeProdWindowsPreview.plan(
                sourceSha = "470c02c9ac268118b0a5f5cf8b34261f096c9899",
                userHome = "C:\\Users\\Victor",
                homeOverride = "D:\\Vex\\Home",
                target = HereticRuntimeRealization.WindowsX64,
            )

        assertEquals(
            Path.of("D:\\Vex\\Home"),
            plan.productHome,
        )
    }

    @Test
    fun refusesNonWindowsTargetOrUnboundSource() {
        assertFailsWith<IllegalArgumentException> {
            VexLifeProdWindowsPreview.plan(
                sourceSha = "470c02c9ac268118b0a5f5cf8b34261f096c9899",
                userHome = "C:\\Users\\Victor",
                homeOverride = null,
                target = HereticRuntimeRealization.MacArm64,
            )
        }

        assertFailsWith<IllegalArgumentException> {
            VexLifeProdWindowsPreview.plan(
                sourceSha = "not-a-source-sha",
                userHome = "C:\\Users\\Victor",
                homeOverride = null,
                target = HereticRuntimeRealization.WindowsX64,
            )
        }
    }
}

// [VXG RealForever]
