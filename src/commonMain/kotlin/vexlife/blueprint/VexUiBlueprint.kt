package vexlife.blueprint

/** Static semantic UI structure. Runtime/provider instances remain outside this tree. */
data class VexUiElement(
    val ref: String,
    val kind: Kind,
    val children: List<VexUiElement> = emptyList(),
) {
    enum class Kind {
        Region,
        Content,
        TextField,
        Control,
        Collection,
    }

    fun flatten(): List<VexUiElement> =
        listOf(this) +
            children.flatMap(
                VexUiElement::flatten,
            )
}

/**
 * Rooted static Screen Blueprint.
 *
 * A Screen owns stable identity + structural placement only. Renderer/runtime currentness and
 * product semantic State remain separate owners.
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

    protected fun layout(
        block: VexUiScope.() -> Unit,
    ): List<VexUiElement> =
        VexUiScope(
            screenKey = screenKey,
            path = emptyList(),
        )
            .apply(
                block,
            )
            .build()

    abstract val content: List<VexUiElement>

    val elements: List<VexUiElement>
        get() =
            content.flatMap(
                VexUiElement::flatten,
            )
}

/** Nested identity authoring scope; child refs inherit their Screen + parent path. */
class VexUiScope internal constructor(
    private val screenKey: String,
    private val path: List<String>,
) {
    private val children =
        mutableListOf<VexUiElement>()

    fun region(
        id: String,
        block: VexUiScope.() -> Unit = {},
    ) =
        container(
            id = id,
            kind = VexUiElement.Kind.Region,
            block = block,
        )

    fun content(
        id: String,
    ) =
        leaf(
            id = id,
            kind = VexUiElement.Kind.Content,
        )

    fun textField(
        id: String,
    ) =
        leaf(
            id = id,
            kind = VexUiElement.Kind.TextField,
        )

    fun control(
        id: String,
    ) =
        leaf(
            id = id,
            kind = VexUiElement.Kind.Control,
        )

    fun collection(
        id: String,
    ) =
        leaf(
            id = id,
            kind = VexUiElement.Kind.Collection,
        )

    internal fun build(): List<VexUiElement> =
        children.toList()

    private fun container(
        id: String,
        kind: VexUiElement.Kind,
        block: VexUiScope.() -> Unit,
    ) {
        val segment =
            normalizeSegment(
                id,
            )
        val nextPath =
            path +
                segment
        val nested =
            VexUiScope(
                screenKey = screenKey,
                path = nextPath,
            )
                .apply(
                    block,
                )

        children +=
            VexUiElement(
                ref =
                    elementRef(
                        screenKey = screenKey,
                        path = nextPath,
                    ),
                kind = kind,
                children = nested.build(),
            )
    }

    private fun leaf(
        id: String,
        kind: VexUiElement.Kind,
    ) {
        val nextPath =
            path +
                normalizeSegment(
                    id,
                )

        children +=
            VexUiElement(
                ref =
                    elementRef(
                        screenKey = screenKey,
                        path = nextPath,
                    ),
                kind = kind,
            )
    }
}

private fun elementRef(
    screenKey: String,
    path: List<String>,
): String =
    "element.$screenKey.${path.joinToString(".")}"

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
