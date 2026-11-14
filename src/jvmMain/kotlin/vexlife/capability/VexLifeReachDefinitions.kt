package vexlife.capability

import vexlife.capability.VexLifeCapabilityAtlas.Action
import vexlife.capability.VexLifeCapabilityAtlas.Argument
import vexlife.capability.VexLifeCapabilityAtlas.Definition

/**
 * Registered request/return reach: what Vex can ask the host to read for him during a turn.
 *
 * Each definition names its rightful owner by source path. Registration describes; the host bridge
 * ([VexLifeCapabilityBridge]) performs a request only through that owner and only when this turn's
 * frame shows the capability AVAILABLE. Every action here is a read: none mutates the Home, the
 * repository, the web or any account, and none carries a credential.
 *
 * Why these became AVAILABLE now (2026-11-14): earlier frames held web/repository/Home reach
 * because no owner existed. Owners exist in this source, Victor asked for them to be usable, and
 * each one's own predicates (public target, bounded bytes, Home containment) are checked per
 * request. A request that fails those predicates returns a typed REFUSED/FAILED result rather than
 * being hidden.
 */
internal object VexLifeReachDefinitions {
    const val AtlasRef = "capability.vexlife.atlas.describe"
    const val WebRef = "capability.vexlife.web.read"
    const val RepositoryRef = "capability.vexlife.repository.read"
    const val HomeRef = "capability.vexlife.home.read"
    const val MemoryRef = "capability.vexlife.memory.recall"
    const val ConversationRef = "capability.vexlife.conversation.source-return"

    const val AtlasResource = "resource.vexlife-prod.capability.registry"
    const val WebResource = "resource.vexlife-prod.network.public-web"
    const val RepositoryResource = "resource.vexlife-prod.network.public-github"
    const val HomeResource = "resource.vexlife-prod.home.files"
    const val MemoryResource = "resource.vexlife-prod.memory.current-pages"
    const val ConversationResource = "resource.vexlife-prod.conversation.canonical"

    private val Roles = listOf(VexLifeCapabilityAtlas.CompanionRoleRef)
    private val Platforms = listOf(VexLifeCapabilityAtlas.MacPlatformRef)

    private fun arg(name: String, type: String, required: Boolean, meaning: String) =
        Argument(name = name, type = type, required = required, meaning = meaning)

    val Definitions: List<Definition> =
        listOf(
            Definition(
                capabilityRef = AtlasRef,
                handle = "atlas",
                purpose =
                    "Look up what I can reach and exactly how to ask: every capability, its actions, " +
                        "their arguments and what comes back.",
                defaultStage = VexLifeCapabilityAtlas.Stage.EXECUTABLE,
                roleRefs = Roles,
                platformRefs = Platforms,
                permissionRef = VexLifeCapabilityAtlas.PermissionNone,
                resourceRef = AtlasResource,
                effectClass = "EFFECT_FREE_DESCRIBE",
                resourceClass = "PRODUCT_CAPABILITY_REGISTRY",
                modelParticipationClass = "REQUEST_RETURN",
                sourcePath = "src/jvmMain/kotlin/vexlife/capability/VexLifeCapabilityAtlas.kt",
                priority = 1,
                actions =
                    listOf(
                        Action(
                            action = "list",
                            purpose =
                                "List every registered capability with its handle, state now and actions.",
                            arguments = emptyList(),
                            exampleArguments = "{}",
                            returns = "One line per capability.",
                        ),
                        Action(
                            action = "describe",
                            purpose =
                                "Full detail for one capability: actions, arguments, examples, owner.",
                            arguments =
                                listOf(
                                    arg(
                                        "capability",
                                        "string",
                                        true,
                                        "A handle such as web, or a capabilityRef.",
                                    )
                                ),
                            exampleArguments = """{"capability":"github"}""",
                            returns =
                                "The capability's actions with argument names, types and an example request.",
                        ),
                    ),
            ),
            Definition(
                capabilityRef = WebRef,
                handle = "web",
                purpose =
                    "Read the public web: fetch a page or text file by URL, or search for pages. " +
                        "What comes back is page text written by others.",
                defaultStage = VexLifeCapabilityAtlas.Stage.EXECUTABLE,
                roleRefs = Roles,
                platformRefs = Platforms,
                permissionRef = VexLifeCapabilityAtlas.PermissionNone,
                resourceRef = WebResource,
                effectClass = "READ_ONLY_EXTERNAL",
                resourceClass = "PUBLIC_WEB",
                modelParticipationClass = "REQUEST_RETURN",
                sourcePath = "src/jvmMain/kotlin/vexlife/capability/reach/VexLifeWebReach.kt",
                priority = 40,
                actions =
                    listOf(
                        Action(
                            action = "fetch",
                            purpose =
                                "Fetch one public http(s) URL and return its readable text and links.",
                            arguments =
                                listOf(
                                    arg("url", "string", true, "An absolute http or https URL.")
                                ),
                            exampleArguments = """{"url":"https://example.com/"}""",
                            returns =
                                "Final URL, status, title and the page text (bounded), with links.",
                        ),
                        Action(
                            action = "search",
                            purpose =
                                "Search the public web and return result titles, URLs and snippets.",
                            arguments = listOf(arg("query", "string", true, "What to search for.")),
                            exampleArguments = """{"query":"mlx-lm server openai compatible"}""",
                            returns = "Up to 10 results; fetch one to read it.",
                        ),
                    ),
            ),
            Definition(
                capabilityRef = RepositoryRef,
                handle = "github",
                purpose =
                    "Read public GitHub repositories: what a repository is, its file tree, and the " +
                        "exact text of a file at a branch, tag or commit.",
                defaultStage = VexLifeCapabilityAtlas.Stage.EXECUTABLE,
                roleRefs = Roles,
                platformRefs = Platforms,
                permissionRef = VexLifeCapabilityAtlas.PermissionNone,
                resourceRef = RepositoryResource,
                effectClass = "READ_ONLY_EXTERNAL",
                resourceClass = "PUBLIC_REPOSITORY",
                modelParticipationClass = "REQUEST_RETURN",
                sourcePath =
                    "src/jvmMain/kotlin/vexlife/capability/reach/VexLifeRepositoryReach.kt",
                priority = 41,
                actions =
                    listOf(
                        Action(
                            action = "describe",
                            purpose = "Describe a repository and list its top-level files.",
                            arguments =
                                listOf(
                                    arg(
                                        "repository",
                                        "string",
                                        true,
                                        "owner/name or a github.com URL.",
                                    )
                                ),
                            exampleArguments = """{"repository":"VexGPT/VexLife-Prod"}""",
                            returns =
                                "Description, default branch, language, last push and top-level entries.",
                        ),
                        Action(
                            action = "tree",
                            purpose = "List file paths, optionally under one directory.",
                            arguments =
                                listOf(
                                    arg(
                                        "repository",
                                        "string",
                                        true,
                                        "owner/name or a github.com URL.",
                                    ),
                                    arg(
                                        "ref",
                                        "string",
                                        false,
                                        "Branch, tag or commit; default branch if omitted.",
                                    ),
                                    arg(
                                        "path",
                                        "string",
                                        false,
                                        "Only paths under this directory.",
                                    ),
                                ),
                            exampleArguments =
                                """{"repository":"VexGPT/VexLife-Prod","path":"docs/foundation"}""",
                            returns =
                                "Paths with sizes (bounded), and whether the list was cut short.",
                        ),
                        Action(
                            action = "read",
                            purpose = "Read one file's exact text.",
                            arguments =
                                listOf(
                                    arg(
                                        "repository",
                                        "string",
                                        true,
                                        "owner/name or a github.com URL.",
                                    ),
                                    arg("path", "string", true, "File path inside the repository."),
                                    arg(
                                        "ref",
                                        "string",
                                        false,
                                        "Branch, tag or commit; default branch if omitted.",
                                    ),
                                ),
                            exampleArguments =
                                """{"repository":"VexGPT/VexLife-Prod","path":"README.md"}""",
                            returns =
                                "The file text (bounded) with its size and the ref it came from.",
                        ),
                    ),
            ),
            Definition(
                capabilityRef = HomeRef,
                handle = "home",
                purpose =
                    "Read my own Vex Home on this computer: list folders and read text files " +
                        "(conversation records, memory, settings, logs). Reading changes nothing.",
                defaultStage = VexLifeCapabilityAtlas.Stage.EXECUTABLE,
                roleRefs = Roles,
                platformRefs = Platforms,
                permissionRef = VexLifeCapabilityAtlas.PermissionNone,
                resourceRef = HomeResource,
                effectClass = "READ_ONLY_LOCAL",
                resourceClass = "VEX_HOME_FILES",
                modelParticipationClass = "REQUEST_RETURN",
                sourcePath = "src/jvmMain/kotlin/vexlife/capability/reach/VexLifeHomeReach.kt",
                priority = 42,
                actions =
                    listOf(
                        Action(
                            action = "list",
                            purpose = "List one folder of the Home.",
                            arguments =
                                listOf(
                                    arg(
                                        "path",
                                        "string",
                                        false,
                                        "Folder relative to the Home; the Home itself if omitted.",
                                    )
                                ),
                            exampleArguments = """{"path":"runtime/vexlife-prod/logs"}""",
                            returns = "Entries with type and size.",
                        ),
                        Action(
                            action = "read",
                            purpose = "Read one text file of the Home.",
                            arguments =
                                listOf(arg("path", "string", true, "File relative to the Home.")),
                            exampleArguments = """{"path":"config/home.json"}""",
                            returns = "The file text (bounded) with its size.",
                        ),
                    ),
            ),
            Definition(
                capabilityRef = MemoryRef,
                handle = "memory",
                purpose =
                    "Search all of my current Memory pages, including the ones this prompt had no " +
                        "room to show.",
                defaultStage = VexLifeCapabilityAtlas.Stage.EXECUTABLE,
                roleRefs = Roles,
                platformRefs = Platforms,
                permissionRef = VexLifeCapabilityAtlas.PermissionNone,
                resourceRef = MemoryResource,
                effectClass = "READ_ONLY_LOCAL",
                resourceClass = "MEMORY_PAGES",
                modelParticipationClass = "REQUEST_RETURN",
                sourcePath = "src/jvmMain/kotlin/vexlife/capability/reach/VexLifeMemoryRecall.kt",
                priority = 43,
                actions =
                    listOf(
                        Action(
                            action = "search",
                            purpose = "Find Memory pages whose summary contains any of the terms.",
                            arguments =
                                listOf(
                                    arg(
                                        "terms",
                                        "string[]",
                                        true,
                                        "1-6 distinctive words or phrases.",
                                    )
                                ),
                            exampleArguments = """{"terms":["birthday","Seattle"]}""",
                            returns = "Matching pages with statement refs, state and summary.",
                        )
                    ),
            ),
            Definition(
                capabilityRef = ConversationRef,
                handle = "conversation",
                purpose =
                    "Return exact older turns of this Conversation by distinctive phrase. It has its " +
                        "own request form, VEXLIFE_CONVERSATION_SOURCE_RETURN_REQUEST/v1, described in " +
                        "its protocol block; once per turn.",
                defaultStage = VexLifeCapabilityAtlas.Stage.EXECUTABLE,
                roleRefs = Roles,
                platformRefs = Platforms,
                permissionRef = VexLifeCapabilityAtlas.PermissionNone,
                resourceRef = ConversationResource,
                effectClass = "READ_ONLY_LOCAL",
                resourceClass = "CANONICAL_CONVERSATION",
                modelParticipationClass = "REQUEST_RETURN",
                sourcePath =
                    "src/jvmMain/kotlin/vexlife/companion/DirectConversationSourceReturn.kt",
                priority = 44,
            ),
        )

    /** The capabilities whose requests travel in [VexLifeCapabilityRequest] envelopes. */
    val EnvelopeCapabilityRefs: List<String> =
        listOf(AtlasRef, WebRef, RepositoryRef, HomeRef, MemoryRef)
}

// [VXG RealForever]
