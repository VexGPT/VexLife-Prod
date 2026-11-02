package vexlife.web

import vexlife.blueprint.VexScreenBlueprint
import vexlife.blueprint.VexUiElementRef

/**
 * F1 authoring surface for the already-accepted F0 vocabulary.
 *
 * This layer owns no new manifestation meaning. It only makes F0 relationships compositional while
 * preserving explicit roles and compiler-visible source identity.
 */
@DslMarker
internal annotation class VexWebDsl

@VexWebDsl
internal sealed interface VexWebNodeSink {
    fun accept(
        node: VexWeb.Node,
    )
}

@VexWebDsl
internal class VexWebSingleNodeScope :
    VexWebNodeSink {
    private var node:
        VexWeb.Node? =
        null

    override fun accept(
        node:
            VexWeb.Node,
    ) {
        check(
            this.node ==
                null,
        ) {
            "VexWeb scope accepts exactly one node"
        }

        this.node =
            node
    }

    fun build(
        role:
            String,
    ):
        VexWeb.Node =
        requireNotNull(
            node,
        ) {
            "VexWeb $role requires exactly one node"
        }
}

@VexWebDsl
internal class VexWebChildrenScope :
    VexWebNodeSink {
    private val nodes =
        mutableListOf<VexWeb.Node>()

    override fun accept(
        node:
            VexWeb.Node,
    ) {
        nodes +=
            node
    }

    fun build():
        List<VexWeb.Node> =
        nodes.toList()
}

@VexWebDsl
internal class VexWebSurfaceScope(
    private val screen:
        VexScreenBlueprint,
) {
    private var content:
        VexWeb.Node? =
        null
    private var overlayRoot:
        VexWeb.Node? =
        null

    fun content(
        block:
            VexWebSingleNodeScope.() -> Unit,
    ) {
        check(
            content ==
                null,
        ) {
            "VexWeb Surface content is already authored"
        }

        content =
            VexWebSingleNodeScope()
                .apply(
                    block,
                )
                .build(
                    role =
                        "Surface.content",
                )
    }

    fun overlayRoot(
        block:
            VexWebSingleNodeScope.() -> Unit,
    ) {
        check(
            overlayRoot ==
                null,
        ) {
            "VexWeb Surface overlayRoot is already authored"
        }

        overlayRoot =
            VexWebSingleNodeScope()
                .apply(
                    block,
                )
                .build(
                    role =
                        "Surface.overlayRoot",
                )
    }

    fun build():
        VexWeb.Surface =
        VexWeb.Surface(
            screenRef =
                screen.screenRef,
            content =
                requireNotNull(
                    content,
                ) {
                    "VexWeb Surface requires content"
                },
            overlayRoot =
                requireNotNull(
                    overlayRoot,
                ) {
                    "VexWeb Surface requires overlayRoot"
                },
        )
}

@VexWebDsl
internal class VexWebSplitScope {
    private var leading:
        VexWeb.Node? =
        null
    private var handle:
        VexWeb.Node? =
        null
    private var trailing:
        VexWeb.Node? =
        null

    fun leading(
        block:
            VexWebSingleNodeScope.() -> Unit,
    ) {
        check(
            leading ==
                null,
        ) {
            "VexWeb Split.leading is already authored"
        }

        leading =
            VexWebSingleNodeScope()
                .apply(
                    block,
                )
                .build(
                    role =
                        "Split.leading",
                )
    }

    fun handle(
        block:
            VexWebSingleNodeScope.() -> Unit,
    ) {
        check(
            handle ==
                null,
        ) {
            "VexWeb Split.handle is already authored"
        }

        handle =
            VexWebSingleNodeScope()
                .apply(
                    block,
                )
                .build(
                    role =
                        "Split.handle",
                )
    }

    fun trailing(
        block:
            VexWebSingleNodeScope.() -> Unit,
    ) {
        check(
            trailing ==
                null,
        ) {
            "VexWeb Split.trailing is already authored"
        }

        trailing =
            VexWebSingleNodeScope()
                .apply(
                    block,
                )
                .build(
                    role =
                        "Split.trailing",
                )
    }

    fun build(
        ref:
            VexWeb.StructuralRef,
        mode:
            VexWeb.Parameter.SplitMode,
    ):
        VexWeb.Split =
        VexWeb.Split(
            ref =
                ref,
            mode =
                mode,
            leading =
                requireNotNull(
                    leading,
                ) {
                    "VexWeb Split requires leading"
                },
            handle =
                requireNotNull(
                    handle,
                ) {
                    "VexWeb Split requires handle"
                },
            trailing =
                requireNotNull(
                    trailing,
                ) {
                    "VexWeb Split requires trailing"
                },
        )
}

internal fun vexWebSurface(
    screen:
        VexScreenBlueprint,
    block:
        VexWebSurfaceScope.() -> Unit,
):
    VexWeb.Surface =
    VexWebSurfaceScope(
        screen,
    )
        .apply(
            block,
        )
        .build()

internal fun VexWebNodeSink.element(
    element:
        VexUiElementRef,
    placement:
        VexWeb.Placement,
    depth:
        VexWeb.Depth,
    presentation:
        VexWeb.Presentation,
    block:
        VexWebChildrenScope.() -> Unit =
        {},
) {
    val children =
        VexWebChildrenScope()
            .apply(
                block,
            )
            .build()

    accept(
        VexWeb.Element(
            element =
                element,
            placement =
                placement,
            depth =
                depth,
            presentation =
                presentation,
            children =
                children,
        ),
    )
}

internal fun VexWebNodeSink.interactiveElement(
    element:
        VexUiElementRef,
    placement:
        VexWeb.Placement,
    depth:
        VexWeb.Depth,
    presentation:
        VexWeb.Presentation,
    interactions:
        Set<VexWeb.Interaction>,
    binding:
        VexWeb.Binding,
    block:
        VexWebChildrenScope.() -> Unit =
        {},
) {
    require(
        interactions.isNotEmpty(),
    ) {
        "interactiveElement requires at least one Interaction"
    }
    require(
        binding !is
            VexWeb.Binding.None,
    ) {
        "interactiveElement requires an explicit non-None Binding"
    }

    val children =
        VexWebChildrenScope()
            .apply(
                block,
            )
            .build()

    accept(
        VexWeb.Element(
            element =
                element,
            placement =
                placement,
            depth =
                depth,
            presentation =
                presentation,
            interactions =
                interactions,
            binding =
                binding,
            children =
                children,
        ),
    )
}

internal fun VexWebNodeSink.structural(
    ref:
        VexWeb.StructuralRef,
    placement:
        VexWeb.Placement,
    depth:
        VexWeb.Depth,
    presentation:
        VexWeb.Presentation,
    block:
        VexWebChildrenScope.() -> Unit =
        {},
) {
    val children =
        VexWebChildrenScope()
            .apply(
                block,
            )
            .build()

    accept(
        VexWeb.Structural(
            ref =
                ref,
            placement =
                placement,
            depth =
                depth,
            presentation =
                presentation,
            children =
                children,
        ),
    )
}

internal fun VexWebNodeSink.interactiveStructural(
    ref:
        VexWeb.StructuralRef,
    placement:
        VexWeb.Placement,
    depth:
        VexWeb.Depth,
    presentation:
        VexWeb.Presentation,
    interactions:
        Set<VexWeb.Interaction>,
    binding:
        VexWeb.Binding,
    block:
        VexWebChildrenScope.() -> Unit =
        {},
) {
    require(
        interactions.isNotEmpty(),
    ) {
        "interactiveStructural requires at least one Interaction"
    }
    require(
        binding !is
            VexWeb.Binding.None,
    ) {
        "interactiveStructural requires an explicit non-None Binding"
    }

    val children =
        VexWebChildrenScope()
            .apply(
                block,
            )
            .build()

    accept(
        VexWeb.Structural(
            ref =
                ref,
            placement =
                placement,
            depth =
                depth,
            presentation =
                presentation,
            interactions =
                interactions,
            binding =
                binding,
            children =
                children,
        ),
    )
}

internal fun VexWebNodeSink.split(
    ref:
        VexWeb.StructuralRef,
    mode:
        VexWeb.Parameter.SplitMode,
    block:
        VexWebSplitScope.() -> Unit,
) {
    accept(
        VexWebSplitScope()
            .apply(
                block,
            )
            .build(
                ref =
                    ref,
                mode =
                    mode,
            ),
    )
}

internal fun VexWeb.Surface.requireNode(
    ref:
        VexWeb.StructuralRef,
):
    VexWeb.Node =
    requireNode(
        ref.value,
    )

internal fun VexWeb.Surface.requireNode(
    element:
        VexUiElementRef,
):
    VexWeb.Node =
    requireNode(
        element.ref,
    )

private fun VexWeb.Surface.requireNode(
    ref:
        String,
):
    VexWeb.Node {
    fun walk(
        node:
            VexWeb.Node,
    ):
        VexWeb.Node? {
        if (
            node.identity.ref ==
            ref
        ) {
            return node
        }

        node.children.forEach {
            val found =
                walk(
                    it,
                )

            if (
                found !=
                null
            ) {
                return found
            }
        }

        return null
    }

    return walk(
        content,
    )
        ?: walk(
            overlayRoot,
        )
        ?: error(
            "VexWeb Surface does not contain node: $ref",
        )
}

// [VXG RealForever]
