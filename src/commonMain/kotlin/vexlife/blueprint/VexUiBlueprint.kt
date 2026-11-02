package vexlife.blueprint

/**
 * Stable static UI coordinate.
 *
 * The ref and parent relationship are authored by one Screen Blueprint. Runtime/provider instances
 * remain outside this static registry.
 */
@ConsistentCopyVisibility
data class VexUiElementRef internal constructor(
    val ref: String,
    val kind: Kind,
    val parentRef: String?,
) {
    enum class Kind {
        Region,
        Content,
        TextField,
        Control,
        Collection,
    }
}

/**
 * Rooted static Screen Blueprint with compiler-visible element handles.
 *
 * Directly referenced UI relationships are Kotlin properties on the concrete Screen Blueprint. A
 * renderer therefore consumes e.g. Workspace.BrowserViewport rather than reconstructing a string
 * path from convention.
 *
 * Screen Blueprint owns stable identity + structural placement only. Renderer/runtime currentness
 * and product semantic State remain separate owners.
 */
abstract class VexScreenBlueprint(
    screenKey: String,
) {
    val screenKey: String =
        normalizeSegment(
            screenKey,
        )

    val screenRef: String =
        "screen.$screenKey"

    private val declaredElements =
        mutableListOf<VexUiElementRef>()

    val elements: List<VexUiElementRef>
        get() =
            declaredElements.toList()

    fun requireElement(
        ref: String,
    ): VexUiElementRef =
        elements.singleOrNull {
            it.ref ==
                ref
        }
            ?: error(
                "Unknown Blueprint element ref: $ref",
            )

    protected fun region(
        id: String,
    ): VexUiElementRef =
        declare(
            parent = null,
            id = id,
            kind = VexUiElementRef.Kind.Region,
        )

    protected fun VexUiElementRef.region(
        id: String,
    ): VexUiElementRef =
        declare(
            parent = this,
            id = id,
            kind = VexUiElementRef.Kind.Region,
        )

    protected fun VexUiElementRef.content(
        id: String,
    ): VexUiElementRef =
        declare(
            parent = this,
            id = id,
            kind = VexUiElementRef.Kind.Content,
        )

    protected fun VexUiElementRef.textField(
        id: String,
    ): VexUiElementRef =
        declare(
            parent = this,
            id = id,
            kind = VexUiElementRef.Kind.TextField,
        )

    protected fun VexUiElementRef.control(
        id: String,
    ): VexUiElementRef =
        declare(
            parent = this,
            id = id,
            kind = VexUiElementRef.Kind.Control,
        )

    protected fun VexUiElementRef.collection(
        id: String,
    ): VexUiElementRef =
        declare(
            parent = this,
            id = id,
            kind = VexUiElementRef.Kind.Collection,
        )

    private fun declare(
        parent: VexUiElementRef?,
        id: String,
        kind: VexUiElementRef.Kind,
    ): VexUiElementRef {
        val segment =
            normalizeSegment(
                id,
            )
        val parentPath =
            parent
                ?.ref
                ?.removePrefix(
                    "element.$screenKey.",
                )
                ?.takeIf {
                    it !=
                        parent.ref
                }
        val path =
            if (
                parentPath ==
                null
            ) {
                segment
            } else {
                "$parentPath.$segment"
            }
        val ref =
            "element.$screenKey.$path"

        require(
            declaredElements.none {
                it.ref ==
                    ref
            },
        ) {
            "Duplicate Blueprint element ref: $ref"
        }

        return VexUiElementRef(
            ref = ref,
            kind = kind,
            parentRef =
                parent
                    ?.ref,
        )
            .also(
                declaredElements::add,
            )
    }
}

internal fun normalizeSegment(
    value: String,
): String {
    val normalized =
        value
            .trim()
            .lowercase()

    require(
        normalized.matches(
            Regex(
                "[a-z0-9]+(?:-[a-z0-9]+)*",
            ),
        ),
    ) {
        "Vex UI segment must be lowercase kebab-case: $value"
    }

    return normalized
}

// [VXG RealForever]
