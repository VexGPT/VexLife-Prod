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
                "/usr/bin/codesign --force --sign - \"\$WINDOW_APP\"",
            )
        val verifyIndex =
            template.indexOf(
                "/usr/bin/codesign --verify --strict \"\$WINDOW_APP\"",
            )
        val openIndex =
            template.indexOf(
                "/usr/bin/open -W \"\$WINDOW_APP\"",
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

    @Test
    fun nativeSetupWindowUsesAppleScriptObjCErrorOutParameters() {
        val source =
            Files.readString(
                Path.of(
                    System.getProperty(
                        "user.dir",
                    ),
                    "packaging/private-service-image/setup/vexlife-prod-setup-window.applescript",
                ),
            )

        assertTrue(
            source.contains(
                "set {expression, expressionError} to current application's NSRegularExpression's regularExpressionWithPattern:",
            ) &&
                source.contains(
                    "|error|:(reference)",
                ),
            "NSRegularExpression NSError out parameter must use the AppleScriptObjC reference return shape",
        )
        assertTrue(
            source.contains(
                "set {launched, launchError} to task's launchAndReturnError:(reference)",
            ),
            "NSTask NSError out parameter must use the AppleScriptObjC reference return shape",
        )
        assertTrue(
            source.contains(
                "set {jsonObject, parseError} to current application's NSJSONSerialization's JSONObjectWithData:outData options:0 |error|:(reference)",
            ),
            "NSJSONSerialization NSError out parameter must use the AppleScriptObjC reference return shape",
        )
        for (
            forbidden in
            listOf(
                "set expressionError to reference",
                "|error|:expressionError",
                "set launchError to reference",
                "launchAndReturnError:launchError",
                "set parseError to reference",
                "|error|:parseError",
            )
        ) {
            assertTrue(
                !source.contains(
                    forbidden,
                ),
                "native setup window must not restore the malformed AppleScriptObjC NSError bridge: $forbidden",
            )
        }
    }


    @Test
    fun nativeSetupWindowCarriesHumanSelectedHomeIntoProductEntry() {
        val source =
            Files.readString(
                Path.of(
                    System.getProperty(
                        "user.dir",
                    ),
                    "packaging/private-service-image/setup/vexlife-prod-setup-window.applescript",
                ),
            )

        assertTrue(
            source.contains(
                "Choose your Vex Home",
            ),
        )
        assertTrue(
            source.contains(
                "~/.vexlife is only the default suggestion",
            ),
        )
        assertTrue(
            source.contains(
                "\"--inspect-front-door\", \"--home\", selectedHomePath",
            ),
        )
        assertTrue(
            source.contains(
                "\"--activate-front-door-control\", controlRef, \"--home\", selectedHomePath",
            ),
        )
    }


    @Test
    fun nativeSetupWindowDoesNotLeakJsonNullAsVisibleOrActionableText() {
        val source =
            Files.readString(
                Path.of(
                    System.getProperty(
                        "user.dir",
                    ),
                    "packaging/private-service-image/setup/vexlife-prod-setup-window.applescript",
                ),
            )

        assertTrue(
            source.contains(
                "if value is (current application's NSNull's null()) then return fallbackValue",
            ),
            "JSON null must resolve to the caller fallback before AppleScript text coercion",
        )
        // Buttons now come only from the Product-formed card (whose Kotlin invariants reject blank
        // labels / unknown action refs). The window still drops any button whose label or ref
        // resolved to the empty fallback instead of rendering it.
        assertTrue(
            source.contains(
                "if labelText is not \"\" and refText is not \"\" then",
            ),
            "null Product controls must collapse to no button rather than a visible fake control",
        )
        assertTrue(
            !source.contains(
                "primaryControlRef",
            ),
            "native presentation must not read DisplayState control fields directly",
        )
    }

}

// [VXG RealForever]
