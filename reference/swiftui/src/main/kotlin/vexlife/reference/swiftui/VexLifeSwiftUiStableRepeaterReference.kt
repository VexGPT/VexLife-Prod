package vexlife.reference.swiftui

import vexlife.blueprint.VexUiElementRef
import vexlife.conversation.VexLifeConversationContract
import vexlife.workspace.VexLifeWorkspaceBlueprint

/**
 * SwiftUI target pressure for stable repeated runtime items.
 *
 * The Product Blueprint owns the static collection coordinate. Conversation owns durable item
 * identity / chronology. This reference owns only the target-generation question.
 *
 * It intentionally does not consume browser DOM mechanics or Android VexStableLazyColumn source.
 * Those remain independent evidence about the same repeated-item pressure.
 */
object VexLifeSwiftUiStableRepeaterReference {
    const val ManifestSchema =
        "vexlife.swiftui-stable-repeater-reference-manifest/v1"

    enum class Disposition {
        MAPPED,
        HELD,
        PLATFORM_SPECIFIC,
    }

    data class ItemCoordinate(
        val itemRef: String,
        val eventRef: String,
        val sequence: Long,
    ) {
        init {
            require(itemRef.isNotBlank()) {
                "stable repeated-item ref must not be blank"
            }
            require(eventRef.isNotBlank()) {
                "stable repeated-item event ref must not be blank"
            }
        }
    }

    data class Bundle(
        val swift: String,
        val semanticManifest: String,
    )

    /**
     * Current canonical durable Conversation rows already own [messageRef].
     *
     * These overloads intentionally compile against each admitted durable contract version rather
     * than teaching the presentation generator how a Conversation event is persisted.
     */
    fun itemRef(message: VexLifeConversationContract.RequestV1): String =
        message.messageRef

    fun itemRef(message: VexLifeConversationContract.ResponseV1): String =
        message.messageRef

    fun itemRef(message: VexLifeConversationContract.RequestV2): String =
        message.messageRef

    fun itemRef(message: VexLifeConversationContract.ResponseV2): String =
        message.messageRef

    fun itemCoordinate(message: VexLifeConversationContract.RequestV1): ItemCoordinate =
        ItemCoordinate(
            itemRef = message.messageRef,
            eventRef = message.eventRef,
            sequence = message.sequence,
        )

    fun itemCoordinate(message: VexLifeConversationContract.ResponseV1): ItemCoordinate =
        ItemCoordinate(
            itemRef = message.messageRef,
            eventRef = message.eventRef,
            sequence = message.sequence,
        )

    fun itemCoordinate(message: VexLifeConversationContract.RequestV2): ItemCoordinate =
        ItemCoordinate(
            itemRef = message.messageRef,
            eventRef = message.eventRef,
            sequence = message.sequence,
        )

    fun itemCoordinate(message: VexLifeConversationContract.ResponseV2): ItemCoordinate =
        ItemCoordinate(
            itemRef = message.messageRef,
            eventRef = message.eventRef,
            sequence = message.sequence,
        )

    fun stableItemRefs(items: List<ItemCoordinate>): Set<String> {
        val refs =
            items.map {
                it.itemRef
            }

        require(refs.distinct().size == refs.size) {
            "stable repeated-item refs must be unique: " +
                refs.groupingBy { it }
                    .eachCount()
                    .filterValues { count ->
                        count > 1
                    }
                    .keys
        }

        return refs.toSet()
    }

    fun generate(): Bundle {
        validateCurrentSource()

        return Bundle(
            swift = renderSwift(),
            semanticManifest = renderManifest(),
        )
    }

    private fun validateCurrentSource() {
        val collection =
            VexLifeWorkspaceBlueprint
                .ConversationTimeline

        require(
            collection.kind ==
                VexUiElementRef.Kind.Collection,
        ) {
            "ConversationTimeline must remain a Blueprint collection"
        }
        require(
            collection.parentRef ==
                VexLifeWorkspaceBlueprint.Conversation.ref,
        ) {
            "ConversationTimeline must remain owned by Conversation"
        }
    }

    private fun renderSwift(): String =
        buildString {
            appendLine("// Generated from current VexLife typed relationships. Do not edit by hand.")
            appendLine("import SwiftUI")
            appendLine()
            appendLine("public enum VexConversationTimelineIdentity {")
            appendLine(
                "    public static let collectionRef = " +
                    swiftString(
                        VexLifeWorkspaceBlueprint
                            .ConversationTimeline
                            .ref,
                    ),
            )
            appendLine("}")
            appendLine()
            appendLine("public struct VexConversationTimelineItem: Identifiable, Sendable {")
            appendLine("    public let id: String")
            appendLine("    public let eventRef: String")
            appendLine("    public let sequence: Int64")
            appendLine()
            appendLine("    public init(messageRef: String, eventRef: String, sequence: Int64) {")
            appendLine("        precondition(!messageRef.isEmpty, \"stable repeated-item ref must not be blank\")")
            appendLine("        precondition(!eventRef.isEmpty, \"stable repeated-item event ref must not be blank\")")
            appendLine("        self.id = messageRef")
            appendLine("        self.eventRef = eventRef")
            appendLine("        self.sequence = sequence")
            appendLine("    }")
            appendLine("}")
            appendLine()
            appendLine("public struct VexStableConversationTimeline<RowContent: View>: View {")
            appendLine("    public let items: [VexConversationTimelineItem]")
            appendLine("    private let rowContent: (VexConversationTimelineItem) -> RowContent")
            appendLine()
            appendLine("    public init(")
            appendLine("        items: [VexConversationTimelineItem],")
            appendLine("        @ViewBuilder rowContent: @escaping (VexConversationTimelineItem) -> RowContent")
            appendLine("    ) {")
            appendLine("        precondition(")
            appendLine("            Set(items.map(\\.id)).count == items.count,")
            appendLine("            \"stable repeated-item refs must be unique\"")
            appendLine("        )")
            appendLine("        self.items = items")
            appendLine("        self.rowContent = rowContent")
            appendLine("    }")
            appendLine()
            appendLine("    public var body: some View {")
            appendLine("        ScrollView {")
            appendLine("            LazyVStack {")
            appendLine("                ForEach(items) { item in")
            appendLine("                    rowContent(item)")
            appendLine("                        .id(item.id)")
            appendLine("                        .accessibilityIdentifier(item.id)")
            appendLine("                }")
            appendLine("            }")
            appendLine("        }")
            appendLine(
                "        .accessibilityIdentifier(" +
                    "VexConversationTimelineIdentity.collectionRef" +
                    ")",
            )
            appendLine("    }")
            appendLine("}")
        }

    private fun renderManifest(): String =
        buildString {
            append("{\n")
            append("  \"schemaVersion\":")
            append(jsonString(ManifestSchema))
            append(",\n")
            append("  \"source\":{")
            append("\"collectionRef\":")
            append(
                jsonString(
                    VexLifeWorkspaceBlueprint
                        .ConversationTimeline
                        .ref,
                ),
            )
            append(",\"parentRef\":")
            append(
                jsonString(
                    VexLifeWorkspaceBlueprint
                        .Conversation
                        .ref,
                ),
            )
            append(",\"itemIdentityOwner\":\"VexLifeConversationContract.messageRef\"")
            append(",\"eventCoordinate\":\"VexLifeConversationContract.eventRef\"")
            append(",\"orderCoordinate\":\"VexLifeConversationContract.sequence\"")
            append("},\n")
            append("  \"target\":{")
            append("\"framework\":\"SWIFTUI\"")
            append(",\"container\":\"ScrollView/LazyVStack/ForEach\"")
            append(",\"itemIdentity\":\"Identifiable.id\"")
            append("},\n")
            append("  \"relationships\":[\n")
            manifestRelationship(
                relationRef = "stable-item-identity",
                disposition = Disposition.MAPPED,
                source = "feature-owned messageRef",
                target = "Identifiable.id + ForEach identity + View.id",
            )
            append(",\n")
            manifestRelationship(
                relationRef = "collection-identity",
                disposition = Disposition.MAPPED,
                source =
                    VexLifeWorkspaceBlueprint
                        .ConversationTimeline
                        .ref,
                target = "container accessibilityIdentifier",
            )
            append(",\n")
            manifestRelationship(
                relationRef = "duplicate-item-ref-failure",
                disposition = Disposition.MAPPED,
                source = "stable repeated-item contract",
                target = "Swift initializer precondition",
            )
            append(",\n")
            manifestRelationship(
                relationRef = "caller-owned-order",
                disposition = Disposition.MAPPED,
                source = "Conversation chronology / supplied item order",
                target = "ForEach consumes supplied order without sorting",
            )
            append(",\n")
            manifestRelationship(
                relationRef = "requested-visible-item",
                disposition = Disposition.HELD,
                source = "Android VexStableLazyColumn requestedVisibleItemRef pressure",
                target = "SwiftUI target API / deployment floor not selected",
            )
            append(",\n")
            manifestRelationship(
                relationRef = "visible-item-projection",
                disposition = Disposition.HELD,
                source = "Android onVisibleItemRefsChanged + browser viewport continuity pressure",
                target = "SwiftUI visibility observation semantics not yet accepted",
            )
            append(",\n")
            manifestRelationship(
                relationRef = "content-type-reuse-hint",
                disposition = Disposition.PLATFORM_SPECIFIC,
                source = "Android contentType optimization",
                target = "no portable SwiftUI mapping claimed",
            )
            append(",\n")
            manifestRelationship(
                relationRef = "scroll-lock",
                disposition = Disposition.HELD,
                source = "Android userScrollEnabled pressure",
                target = "target OS / interaction contract not yet selected",
            )
            append("\n  ]\n")
            append("}\n")
        }

    private fun StringBuilder.manifestRelationship(
        relationRef: String,
        disposition: Disposition,
        source: String,
        target: String,
    ) {
        append("    {")
        append("\"relationRef\":")
        append(jsonString(relationRef))
        append(",\"disposition\":")
        append(jsonString(disposition.name))
        append(",\"source\":")
        append(jsonString(source))
        append(",\"target\":")
        append(jsonString(target))
        append("}")
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
