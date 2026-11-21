package vexlife.reference.swiftui

import vexlife.blueprint.VexUiElementRef
import vexlife.web.VexLifeWebP1
import vexlife.web.VexWeb
import vexlife.workspace.VexLifeWorkspaceBlueprint

/**
 * SwiftUI reference adapter pressure over the current compiler-visible Quick Actions relationships.
 *
 * This is deliberately not an iOS application and does not make VexWeb portable authority. It asks
 * whether current typed identity / interaction / binding relationships are strong enough to generate
 * native SwiftUI source plus a semantic manifest without copying Compose or browser syntax.
 */
object VexLifeSwiftUiQuickActionsReference {
    const val ManifestSchema = "vexlife.swiftui-reference-manifest/v1"

    data class Bundle(
        val swift: String,
        val semanticManifest: String,
    )

    private data class ActionSource(
        val caseName: String,
        val element: VexUiElementRef,
        val node: VexWeb.Element,
    )

    private val Actions =
        listOf(
            ActionSource(
                caseName = "vexSummon",
                element = VexLifeWorkspaceBlueprint.VexSummon,
                node = VexLifeWebP1.VexSummon,
            ),
            ActionSource(
                caseName = "vision",
                element = VexLifeWorkspaceBlueprint.VisionQuickAccess,
                node = VexLifeWebP1.VisionQuickAccess,
            ),
            ActionSource(
                caseName = "speech",
                element = VexLifeWorkspaceBlueprint.Speech,
                node = VexLifeWebP1.Speech,
            ),
            ActionSource(
                caseName = "language",
                element = VexLifeWorkspaceBlueprint.Language,
                node = VexLifeWebP1.Language,
            ),
            ActionSource(
                caseName = "theme",
                element = VexLifeWorkspaceBlueprint.Theme,
                node = VexLifeWebP1.Theme,
            ),
            ActionSource(
                caseName = "releaseAlerts",
                element = VexLifeWorkspaceBlueprint.ReleaseAlerts,
                node = VexLifeWebP1.ReleaseAlerts,
            ),
            ActionSource(
                caseName = "support",
                element = VexLifeWorkspaceBlueprint.Support,
                node = VexLifeWebP1.Support,
            ),
        )

    fun generate(): Bundle {
        validateCurrentSource()

        return Bundle(
            swift = renderSwift(),
            semanticManifest = renderManifest(),
        )
    }

    private fun validateCurrentSource() {
        val root = VexLifeWebP1.QuickActions
        require(root.identity.ref == VexLifeWorkspaceBlueprint.QuickActions.ref) {
            "SwiftUI Quick Actions reference root drifted from current Blueprint identity"
        }

        val expectedChildren =
            Actions.map { it.element.ref } +
                VexLifeWorkspaceBlueprint.QuickActionsTrigger.ref
        val actualChildren =
            root.children.map { it.identity.ref }

        require(actualChildren == expectedChildren) {
            "SwiftUI Quick Actions reference must be updated for current Product child order: " +
                actualChildren
        }

        Actions.forEach { action ->
            require(action.node.identity.ref == action.element.ref) {
                "SwiftUI Quick Actions action drifted: " + action.caseName
            }
            require(VexWeb.Interaction.Activate in action.node.interactions) {
                "SwiftUI Quick Actions action is no longer activatable: " + action.caseName
            }
            require(isReferenceBinding(action.node.binding)) {
                "SwiftUI Quick Actions action has unsupported binding: " + action.caseName
            }
        }

        val trigger = VexLifeWebP1.QuickActionsTrigger
        require(trigger.identity.ref == VexLifeWorkspaceBlueprint.QuickActionsTrigger.ref)
        require(VexWeb.Interaction.Activate in trigger.interactions)
        require(trigger.binding is VexWeb.Binding.LocalInteraction)
        require(
            trigger.binding.target.ref ==
                VexLifeWorkspaceBlueprint.QuickActions.ref
        ) {
            "Quick Actions trigger must continue targeting the Quick Actions collection"
        }
    }

    private fun isReferenceBinding(binding: VexWeb.Binding): Boolean =
        when (binding) {
            is VexWeb.Binding.LocalInteraction,
            is VexWeb.Binding.PresentationAction,
            is VexWeb.Binding.ExternalNavigation,
            ->
                true

            else ->
                false
        }

    private fun renderSwift(): String {
        val cases =
            Actions.joinToString(
                separator = "\n",
            ) { action ->
                "    case " +
                    action.caseName +
                    " = " +
                    swiftString(action.element.ref)
            }
        val bindingCases =
            Actions.joinToString(
                separator = "\n",
            ) { action ->
                "        case ." +
                    action.caseName +
                    ":\n" +
                    "            return " +
                    swiftBinding(action.node.binding)
            }
        val stringFields =
            Actions.joinToString(
                separator = "\n",
            ) { action ->
                "    public let " + action.caseName + ": String"
            }
        val stringParameters =
            Actions.joinToString(
                separator = ",\n",
            ) { action ->
                "        " + action.caseName + ": String"
            }
        val stringAssignments =
            Actions.joinToString(
                separator = "\n",
            ) { action ->
                "        self." + action.caseName + " = " + action.caseName
            }
        val labelCases =
            Actions.joinToString(
                separator = "\n",
            ) { action ->
                "        case ." +
                    action.caseName +
                    ":\n" +
                    "            return " +
                    action.caseName
            }

        return buildString {
            appendLine("// Generated from current VexLife Kotlin relationship source. Do not edit by hand.")
            appendLine("import SwiftUI")
            appendLine()
            appendLine("public enum VexQuickActionsIdentity {")
            appendLine(
                "    public static let componentRef = " +
                    swiftString(VexLifeWorkspaceBlueprint.QuickActions.ref)
            )
            appendLine(
                "    public static let triggerRef = " +
                    swiftString(VexLifeWorkspaceBlueprint.QuickActionsTrigger.ref)
            )
            appendLine("}")
            appendLine()
            appendLine("public enum VexQuickActionBinding: Equatable, Sendable {")
            appendLine("    case localInteraction(targetRef: String)")
            appendLine("    case presentationAction(actionRef: String)")
            appendLine("    case externalNavigation(href: String)")
            appendLine("}")
            appendLine()
            appendLine("public enum VexQuickAction: String, CaseIterable, Identifiable, Sendable {")
            appendLine(cases)
            appendLine()
            appendLine("    public var id: String { rawValue }")
            appendLine()
            appendLine("    public var binding: VexQuickActionBinding {")
            appendLine("        switch self {")
            appendLine(bindingCases)
            appendLine("        }")
            appendLine("    }")
            appendLine("}")
            appendLine()
            appendLine("public struct VexQuickActionsStrings: Sendable {")
            appendLine("    public let trigger: String")
            appendLine(stringFields)
            appendLine()
            appendLine("    public init(")
            appendLine("        trigger: String,")
            appendLine(stringParameters)
            appendLine("    ) {")
            appendLine("        self.trigger = trigger")
            appendLine(stringAssignments)
            appendLine("    }")
            appendLine()
            appendLine("    public func label(for action: VexQuickAction) -> String {")
            appendLine("        switch action {")
            appendLine(labelCases)
            appendLine("        }")
            appendLine("    }")
            appendLine("}")
            appendLine()
            appendLine("public struct VexQuickActionsReferenceView: View {")
            appendLine("    public let strings: VexQuickActionsStrings")
            appendLine("    public let onActivate: (VexQuickAction) -> Void")
            appendLine()
            appendLine("    public init(")
            appendLine("        strings: VexQuickActionsStrings,")
            appendLine("        onActivate: @escaping (VexQuickAction) -> Void")
            appendLine("    ) {")
            appendLine("        self.strings = strings")
            appendLine("        self.onActivate = onActivate")
            appendLine("    }")
            appendLine()
            appendLine("    public var body: some View {")
            appendLine("        Menu {")
            appendLine("            ForEach(VexQuickAction.allCases) { action in")
            appendLine("                Button {")
            appendLine("                    onActivate(action)")
            appendLine("                } label: {")
            appendLine("                    Text(verbatim: strings.label(for: action))")
            appendLine("                }")
            appendLine("                .accessibilityIdentifier(action.rawValue)")
            appendLine("            }")
            appendLine("        } label: {")
            appendLine("            Text(verbatim: strings.trigger)")
            appendLine("                .accessibilityIdentifier(VexQuickActionsIdentity.triggerRef)")
            appendLine("        }")
            appendLine("        .accessibilityIdentifier(VexQuickActionsIdentity.componentRef)")
            appendLine("    }")
            appendLine("}")
        }
    }

    private fun swiftBinding(binding: VexWeb.Binding): String =
        when (binding) {
            is VexWeb.Binding.LocalInteraction ->
                ".localInteraction(targetRef: " +
                    swiftString(binding.target.ref) +
                    ")"

            is VexWeb.Binding.PresentationAction ->
                ".presentationAction(actionRef: " +
                    swiftString(binding.actionRef) +
                    ")"

            is VexWeb.Binding.ExternalNavigation ->
                ".externalNavigation(href: " +
                    swiftString(binding.href) +
                    ")"

            else ->
                error("unsupported SwiftUI Quick Actions reference binding: " + binding)
        }

    private fun renderManifest(): String =
        buildString {
            append("{\n")
            append("  \"schemaVersion\":")
            append(jsonString(ManifestSchema))
            append(",\n")
            append("  \"screenRef\":")
            append(jsonString(VexLifeWorkspaceBlueprint.screenRef))
            append(",\n")
            append("  \"componentRef\":")
            append(jsonString(VexLifeWorkspaceBlueprint.QuickActions.ref))
            append(",\n")
            append("  \"target\":{\"framework\":\"SWIFTUI\",\"component\":\"Menu\"},\n")
            append("  \"trigger\":")
            append(
                manifestNode(
                    node = VexLifeWebP1.QuickActionsTrigger,
                    targetRole = "MENU_LABEL",
                    bindingRealization = "NATIVE_MENU_PRESENTATION",
                )
            )
            append(",\n")
            append("  \"actions\":[\n")
            Actions.forEachIndexed { index, action ->
                append("    {")
                append("\"case\":")
                append(jsonString(action.caseName))
                append(",\"node\":")
                append(
                    manifestNode(
                        node = action.node,
                        targetRole = "MENU_ACTION",
                        bindingRealization = "HOST_DISPATCH",
                    )
                )
                append("}")
                if (index != Actions.lastIndex) {
                    append(",")
                }
                append("\n")
            }
            append("  ]\n")
            append("}\n")
        }

    private fun manifestNode(
        node: VexWeb.Element,
        targetRole: String,
        bindingRealization: String,
    ): String =
        buildString {
            append("{")
            append("\"ref\":")
            append(jsonString(node.identity.ref))
            append(",\"sourceParentRef\":")
            append(
                node.identity.sourceParentRef
                    ?.let(::jsonString)
                    ?: "null"
            )
            append(",\"placement\":")
            append(jsonString(node.placement.name))
            append(",\"depth\":")
            append(jsonString(node.depth.name))
            append(",\"presentation\":")
            append(jsonString(node.presentation.name))
            append(",\"interactions\":[")
            node.interactions
                .map { it.name }
                .sorted()
                .forEachIndexed { index, interaction ->
                    if (index > 0) {
                        append(",")
                    }
                    append(jsonString(interaction))
                }
            append("],\"binding\":")
            append(manifestBinding(node.binding))
            append(",\"targetRole\":")
            append(jsonString(targetRole))
            append(",\"bindingRealization\":")
            append(jsonString(bindingRealization))
            append("}")
        }

    private fun manifestBinding(binding: VexWeb.Binding): String =
        when (binding) {
            is VexWeb.Binding.LocalInteraction ->
                "{\"type\":\"LOCAL_INTERACTION\",\"targetRef\":" +
                    jsonString(binding.target.ref) +
                    "}"

            is VexWeb.Binding.PresentationAction ->
                "{\"type\":\"PRESENTATION_ACTION\",\"actionRef\":" +
                    jsonString(binding.actionRef) +
                    "}"

            is VexWeb.Binding.ExternalNavigation ->
                "{\"type\":\"EXTERNAL_NAVIGATION\",\"href\":" +
                    jsonString(binding.href) +
                    "}"

            else ->
                error("unsupported SwiftUI Quick Actions reference binding: " + binding)
        }

    private fun swiftString(value: String): String =
        "\"" +
            value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n") +
            "\""

    private fun jsonString(value: String): String =
        "\"" +
            value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n") +
            "\""
}

// [VXG RealForever]
