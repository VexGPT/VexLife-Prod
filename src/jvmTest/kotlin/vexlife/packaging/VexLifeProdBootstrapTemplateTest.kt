package vexlife.packaging

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertTrue

class VexLifeProdBootstrapTemplateTest {
    @Test
    fun nativeSetupBundleIsSignedOnlyAfterProductMetadataBinding() {
        val template =
            Files.readString(
                Path.of(
                    System.getProperty(
                        "user.dir",
                    ),
                    "packaging/bootstrap/vexlife-prod-bootstrap.command.template",
                ),
            )

        val toolIndex =
            template.indexOf(
                "/usr/bin/codesign /usr/bin/open",
            )
        val finalMetadataIndex =
            template.indexOf(
                "/usr/bin/plutil -insert VexLifeProdSetupLauncher",
            )
        val signIndex =
            template.indexOf(
                "/usr/bin/codesign --force --sign - \"$WINDOW_APP\"",
            )
        val verifyIndex =
            template.indexOf(
                "/usr/bin/codesign --verify --strict \"$WINDOW_APP\"",
            )
        val openIndex =
            template.indexOf(
                "/usr/bin/open -W \"$WINDOW_APP\"",
            )

        assertTrue(
            toolIndex >=
                0,
            "bootstrap must require codesign before it is needed",
        )
        assertTrue(
            finalMetadataIndex >=
                0 &&
                signIndex >
                finalMetadataIndex,
            "native setup app must be signed only after Product-owned Info.plist metadata is bound",
        )
        assertTrue(
            verifyIndex >
                signIndex,
            "native setup app signature must be verified after final signing",
        )
        assertTrue(
            openIndex >
                verifyIndex,
            "LaunchServices must see only the final verified signed app bundle",
        )
    }
}

// [VXG RealForever]
