package vexlife.reference.swiftui

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

/**
 * Proof-only materialization boundary for the deterministic SwiftUI reference generators.
 *
 * The generators remain the target-relationship owners. This writer only places their exact output
 * into one build directory so an Apple toolchain can typecheck the same bytes rather than a copied
 * or reconstructed source sample.
 */
internal object VexLifeSwiftUiReferenceMaterializer {
    fun materialize(outputRoot: Path): List<Path> {
        val root = outputRoot.toAbsolutePath().normalize()
        Files.createDirectories(root)

        val quickActions = VexLifeSwiftUiQuickActionsReference.generate()
        val stableRepeater = VexLifeSwiftUiStableRepeaterReference.generate()
        val workspaceFrame = VexLifeSwiftUiWorkspaceFrameReference.generate()

        val artifacts =
            listOf(
                "VexQuickActionsReference.swift" to quickActions.swift,
                "VexQuickActionsReference.manifest.json" to quickActions.semanticManifest,
                "VexStableConversationTimelineReference.swift" to stableRepeater.swift,
                "VexStableConversationTimelineReference.manifest.json" to
                    stableRepeater.semanticManifest,
                "VexWorkspaceFrameReference.swift" to workspaceFrame.swift,
                "VexWorkspaceFrameReference.manifest.json" to workspaceFrame.semanticManifest,
            )

        return artifacts.map { (name, content) ->
            val target = root.resolve(name).normalize()
            require(target.parent == root) {
                "SwiftUI reference materialization escaped its output root: $name"
            }
            Files.writeString(
                target,
                content,
                StandardCharsets.UTF_8,
            )
            target
        }
    }
}

fun main(args: Array<String>) {
    require(args.size == 1) {
        "usage: VexLifeSwiftUiReferenceMaterializer <output-directory>"
    }

    VexLifeSwiftUiReferenceMaterializer
        .materialize(
            Path.of(args.single()),
        )
        .forEach { path ->
            println(path)
        }
}

// [VXG RealForever]
