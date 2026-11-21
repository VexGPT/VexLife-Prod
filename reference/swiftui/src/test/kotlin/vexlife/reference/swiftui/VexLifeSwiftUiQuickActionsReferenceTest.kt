package vexlife.reference.swiftui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import vexlife.web.VexLifeWebP1
import vexlife.web.VexWeb
import vexlife.web.VexWebAuthoring
import vexlife.workspace.VexLifeWorkspaceBlueprint

class VexLifeSwiftUiQuickActionsReferenceTest {
    @Test
    fun generationIsDeterministicAndUsesNativeSwiftUiStructure() {
        val first =
            VexLifeSwiftUiQuickActionsReference.generate()
        val second =
            VexLifeSwiftUiQuickActionsReference.generate()

        assertEquals(
            first,
            second,
        )
        assertTrue(first.swift.contains("import SwiftUI"))
        assertTrue(first.swift.contains("public struct VexQuickActionsReferenceView: View"))
        assertTrue(first.swift.contains("Menu {"))
        assertTrue(first.swift.contains("ForEach(VexQuickAction.allCases)"))
        assertTrue(first.swift.contains("Text(verbatim: strings.label(for: action))"))
        assertTrue(first.swift.contains(".accessibilityIdentifier(action.rawValue)"))
        assertTrue(
            first.swift.contains(
                "public static let componentRef = \"" +
                    VexLifeWorkspaceBlueprint.QuickActions.ref +
                    "\""
            )
        )
        assertTrue(
            first.swift.contains(
                "public static let triggerRef = \"" +
                    VexLifeWorkspaceBlueprint.QuickActionsTrigger.ref +
                    "\""
            )
        )
    }

    @Test
    fun generatedSourceCarriesCurrentBlueprintIdentityWithoutHardcodedDisplayWords() {
        val swift =
            VexLifeSwiftUiQuickActionsReference.generate()
                .swift

        listOf(
                VexLifeWorkspaceBlueprint.VexSummon,
                VexLifeWorkspaceBlueprint.VisionQuickAccess,
                VexLifeWorkspaceBlueprint.Speech,
                VexLifeWorkspaceBlueprint.Language,
                VexLifeWorkspaceBlueprint.Theme,
                VexLifeWorkspaceBlueprint.ReleaseAlerts,
                VexLifeWorkspaceBlueprint.Support,
                VexLifeWorkspaceBlueprint.QuickActionsTrigger,
            )
            .forEach { element ->
                assertTrue(
                    swift.contains(element.ref),
                    element.ref,
                )
            }

        assertFalse(swift.contains("Text(\"Support\")"))
        assertFalse(swift.contains("Text(\"Language\")"))
        assertFalse(swift.contains("Text(\"Theme\")"))
    }

    @Test
    fun semanticManifestPreservesCurrentBindingsForReverseProjection() {
        val manifest =
            VexLifeSwiftUiQuickActionsReference.generate()
                .semanticManifest

        assertTrue(
            manifest.contains(
                "\"componentRef\":\"" +
                    VexLifeWorkspaceBlueprint.QuickActions.ref +
                    "\""
            )
        )
        assertTrue(
            manifest.contains(
                "\"targetRef\":\"" +
                    VexLifeWorkspaceBlueprint.Companion.ref +
                    "\""
            )
        )
        assertTrue(
            manifest.contains(
                "\"targetRef\":\"" +
                    VexLifeWorkspaceBlueprint.Vision.ref +
                    "\""
            )
        )
        assertTrue(
            manifest.contains(
                "\"targetRef\":\"" +
                    VexLifeWorkspaceBlueprint.SpeechPanel.ref +
                    "\""
            )
        )
        assertTrue(
            manifest.contains(
                "\"targetRef\":\"" +
                    VexLifeWorkspaceBlueprint.LanguagePanel.ref +
                    "\""
            )
        )
        assertTrue(
            manifest.contains(
                "\"actionRef\":\"" +
                    VexLifeWebP1.Refs.ColorModeToggleAction +
                    "\""
            )
        )
        assertTrue(
            manifest.contains(
                "\"targetRef\":\"" +
                    VexLifeWorkspaceBlueprint.ReleaseAlertsPanel.ref +
                    "\""
            )
        )
        assertTrue(
            manifest.contains(
                "\"href\":\"" +
                    VexLifeWebP1.Refs.SupportHref +
                    "\""
            )
        )
        assertTrue(
            manifest.contains(
                "\"target\":{\"framework\":\"SWIFTUI\",\"component\":\"Menu\"}"
            )
        )
        assertTrue(
            manifest.contains(
                "\"targetRole\":\"MENU_LABEL\",\"bindingRealization\":\"NATIVE_MENU_PRESENTATION\""
            )
        )
        assertTrue(
            manifest.contains(
                "\"targetRole\":\"MENU_ACTION\",\"bindingRealization\":\"HOST_DISPATCH\""
            )
        )
    }

    @Test
    fun unknownBindingKindFailsClosedInsteadOfGuessingATargetCallback() {
        assertFailsWith<IllegalStateException> {
            VexLifeSwiftUiQuickActionsReference.requireReferenceBinding(
                binding =
                    VexWebAuthoring.capability(
                        VexWeb.CapabilityRef.of(
                            "component-library.test.unsupported",
                        ),
                    ),
                coordinate = "unsupported",
            )
        }
    }

    @Test
    fun currentQuickActionsChildOrderIsPartOfTheReferenceInputContract() {
        assertEquals(
            listOf(
                VexLifeWorkspaceBlueprint.VexSummon.ref,
                VexLifeWorkspaceBlueprint.VisionQuickAccess.ref,
                VexLifeWorkspaceBlueprint.Speech.ref,
                VexLifeWorkspaceBlueprint.Language.ref,
                VexLifeWorkspaceBlueprint.Theme.ref,
                VexLifeWorkspaceBlueprint.ReleaseAlerts.ref,
                VexLifeWorkspaceBlueprint.Support.ref,
                VexLifeWorkspaceBlueprint.QuickActionsTrigger.ref,
            ),
            VexLifeWebP1.QuickActions.children.map {
                it.identity.ref
            },
        )
    }
}

// [VXG RealForever]
