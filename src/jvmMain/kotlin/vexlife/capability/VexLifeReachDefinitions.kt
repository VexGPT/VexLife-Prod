package vexlife.capability

import vexlife.capability.VexLifeCapabilityAtlas.Action
import vexlife.capability.VexLifeCapabilityAtlas.Argument
import vexlife.capability.VexLifeCapabilityAtlas.Definition
import vexlife.capability.VexLifeCapabilityAtlas.ValueSchema

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
    const val RevisitRef = "capability.vexlife.continuity.revisit"

    const val AtlasResource = "resource.vexlife-prod.capability.registry"
    const val WebResource = "resource.vexlife-prod.network.public-web"
    const val RepositoryResource = "resource.vexlife-prod.network.public-github"
    const val HomeResource = "resource.vexlife-prod.home.files"
    const val MemoryResource = "resource.vexlife-prod.memory.current-pages"
    const val ConversationResource = "resource.vexlife-prod.conversation.canonical"
    const val RevisitResource = "resource.vexlife-prod.continuity.revisit"

    const val RevisitExampleArguments =
        """{"queries":[{"terms":["launch","date"]},{"terms":["ship day"]},""" +
            """{"corrections":true,"window":{"lastHours":5,"lastTurns":20}}]}"""

    private val Roles = listOf(VexLifeCapabilityAtlas.CompanionRoleRef)
    private val Platforms = listOf(VexLifeCapabilityAtlas.MacPlatformRef)

    private val StringSchema = ValueSchema("string")
    private val IntegerSchema = ValueSchema("integer")
    private val NumberSchema = ValueSchema("number")
    private val BooleanSchema = ValueSchema("boolean")

    private fun valueSchema(type: String): ValueSchema =
        when {
            type.endsWith("[]") ->
                ValueSchema(
                    type = "array",
                    items = valueSchema(type.removeSuffix("[]")),
                )
            type == "string" -> StringSchema
            type == "integer" -> IntegerSchema
            type == "number" -> NumberSchema
            type == "boolean" -> BooleanSchema
            type == "object" -> ValueSchema("object")
            else -> error("unsupported capability schema shorthand: $type")
        }

    private val RevisitQuerySchema =
        ValueSchema(
            type = "object",
            properties =
                mapOf(
                    "terms" to ValueSchema(type = "array", items = StringSchema),
                    "seedRefs" to ValueSchema(type = "array", items = StringSchema),
                    "window" to
                        ValueSchema(
                            type = "object",
                            properties =
                                mapOf(
                                    "lastHours" to IntegerSchema,
                                    "lastTurns" to IntegerSchema,
                                ),
                        ),
                    "corrections" to BooleanSchema,
                    "speaker" to
                        ValueSchema(
                            type = "string",
                            enumValues = listOf("ANY", "HUMAN", "VEX"),
                        ),
                    "from" to IntegerSchema,
                ),
        )

    private fun arg(
        name: String,
        type: String,
        required: Boolean,
        meaning: String,
        schema: ValueSchema? = null,
    ) =
        Argument(
            name = name,
            schema = schema ?: valueSchema(type),
            required = required,
            meaning = meaning,
        )

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
                                    arg("url", "string", true, "An absolute http or https URL."),
                                    arg(
                                        "offset",
                                        "integer",
                                        false,
                                        "Character offset in the extracted readable text. Omit for the beginning; use nextOffset from a truncated result to continue the same page.",
                                    ),
                                ),
                            exampleArguments = """{"url":"https://example.com/"}""",
                            returns =
                                "Final URL, status, title and one bounded readable-text slice with returnedStart/returnedEnd and nextOffset when more of the current page remains.",
                        ),
                        Action(
                            action = "find",
                            purpose =
                                "Observe one public page once for this human turn, then scan that same observed text for several possible phrases before spending context on a large read.",
                            arguments =
                                listOf(
                                    arg("url", "string", true, "An absolute http or https URL."),
                                    arg(
                                        "queries",
                                        "string[]",
                                        true,
                                        "1-6 words or phrases to locate together in the same observed page snapshot.",
                                    ),
                                ),
                            exampleArguments =
                                """{"url":"https://example.com/guide","queries":["prompt cache","cached tokens"]}""",
                            returns =
                                "Bounded matching excerpts with character ranges, one turn-scoped sourceRef, observedAt, total readable characters and whether the network source was incomplete.",
                        ),
                        Action(
                            action = "search",
                            purpose =
                                "Search the public web and return result titles, URLs and snippets.",
                            arguments = listOf(arg("query", "string", true, "What to search for.")),
                            exampleArguments = """{"query":"mlx-lm server openai compatible"}""",
                            returns = "Up to 10 results; fetch or find within one result to read it.",
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
                                    arg(
                                        "offset",
                                        "integer",
                                        false,
                                        "Character offset inside the exact file observation from the first read in this human turn.",
                                    ),
                                ),
                            exampleArguments =
                                """{"repository":"VexGPT/VexLife-Prod","path":"README.md"}""",
                            returns =
                                "One bounded slice of the exact observed file with sourceRef, returnedStart/returnedEnd and nextOffset when more observed text remains.",
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
                                    ),
                                    arg(
                                        "offset",
                                        "integer",
                                        false,
                                        "Match offset from a prior result; omit for the first page.",
                                    ),
                                ),
                            exampleArguments = """{"terms":["birthday","Seattle"]}""",
                            returns =
                                "Matching pages with statement refs, state and summary, plus totalMatches and nextOffset/frontier when more matches remain.",
                        )
                    ),
            ),
            Definition(
                capabilityRef = ConversationRef,
                handle = "conversation",
                purpose =
                    "Return exact older turns of this Conversation by distinctive exact words. " +
                        "This route keeps its one-source-return-per-human-turn semantics and exact " +
                        "Conversation owner even when carried through a provider-native tool call.",
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
                actions =
                    listOf(
                        Action(
                            action = "return",
                            purpose =
                                "Reopen up to three matching older completed turns from this exact Conversation.",
                            arguments =
                                listOf(
                                    arg(
                                        "queryTerms",
                                        "string[]",
                                        true,
                                        "1-6 concise distinctive exact words or phrases. Any matching term may locate a turn; stronger multi-term matches rank first.",
                                    ),
                                    arg(
                                        "speakerScope",
                                        "string",
                                        false,
                                        "ANY (default), HUMAN, or VEX.",
                                        schema =
                                            ValueSchema(
                                                type = "string",
                                                enumValues =
                                                    listOf(
                                                        "ANY",
                                                        "HUMAN",
                                                        "VEX",
                                                    ),
                                            ),
                                    ),
                                ),
                            exampleArguments =
                                """{"queryTerms":["distinctive phrase"],"speakerScope":"ANY"}""",
                            returns =
                                "Exact matching human/Vex request-response pairs or an explicit NO_MATCH / HELD_TOKEN_BUDGET state.",
                        ),
                    ),
            ),
            Definition(
                capabilityRef = RevisitRef,
                handle = "continuity",
                purpose =
                    "Revisit older continuity in one request: several queries at once over this " +
                        "Conversation's exact turns, my current Memory and where the work stands. " +
                        "Exact words come back with when they were said, what is already in view " +
                        "(turns or Memory) is named instead of repeated, the statements the work " +
                        "names as current are resolved through Memory, and anything not returned " +
                        "is listed with how to ask for it.",
                defaultStage = VexLifeCapabilityAtlas.Stage.EXECUTABLE,
                roleRefs = Roles,
                platformRefs = Platforms,
                permissionRef = VexLifeCapabilityAtlas.PermissionNone,
                resourceRef = RevisitResource,
                effectClass = "READ_ONLY_LOCAL",
                resourceClass = "CONTINUITY_OWNERS",
                modelParticipationClass = "REQUEST_RETURN",
                sourcePath = "src/jvmMain/kotlin/vexlife/continuity/VexLifeContextRevisit.kt",
                priority = 45,
                actions =
                    listOf(
                        Action(
                            action = "revisit",
                            purpose =
                                "Run up to 6 queries together. The terms of one query are clues " +
                                    "that must all appear; different wordings of the same question " +
                                    "go in separate queries, because words are matched exactly.",
                            arguments =
                                listOf(
                                    arg(
                                        "queries",
                                        "object[]",
                                        true,
                                        "Each query object may carry terms (up to 6, all must appear), " +
                                            "seedRefs (turn or event refs), window {lastHours, lastTurns} " +
                                            "(either reaches), corrections: true, speaker ANY|HUMAN|VEX, " +
                                            "or from (an offset to read on in one seeded event).",
                                        schema =
                                            ValueSchema(
                                                type = "array",
                                                items = RevisitQuerySchema,
                                            ),
                                    ),
                                    arg(
                                        "scopes",
                                        "string[]",
                                        false,
                                        "CONVERSATION, MEMORY, WORKING_CONTINUITY; all when omitted.",
                                        schema =
                                            ValueSchema(
                                                type = "array",
                                                items =
                                                    ValueSchema(
                                                        type = "string",
                                                        enumValues =
                                                            listOf(
                                                                "CONVERSATION",
                                                                "MEMORY",
                                                                "WORKING_CONTINUITY",
                                                            ),
                                                    ),
                                            ),
                                    ),
                                    arg(
                                        "order",
                                        "string",
                                        false,
                                        "CURRENT_FIRST (default) or CHRONOLOGICAL.",
                                        schema =
                                            ValueSchema(
                                                type = "string",
                                                enumValues =
                                                    listOf(
                                                        "CURRENT_FIRST",
                                                        "CHRONOLOGICAL",
                                                    ),
                                            ),
                                    ),
                                    arg("maxTurns", "number", false, "1-12, default 6."),
                                    arg("maxTokens", "number", false, "500-8000, default 6000."),
                                ),
                            exampleArguments = RevisitExampleArguments,
                            returns =
                                "Where the work stands, corrections in the person's own words, " +
                                    "matching turns with every query that found them, Memory " +
                                    "pages, what was not returned and what was searched.",
                        )
                    ),
            ),
        )

    /**
     * Capabilities whose canonical fallback requests travel through [VexLifeCapabilityRequest].
     * Conversation source return keeps its dedicated semantic owner/control internally.
     */
    val EnvelopeCapabilityRefs: List<String> =
        listOf(AtlasRef, WebRef, RepositoryRef, HomeRef, MemoryRef, RevisitRef)

    /** Every read route declared to provider-native tool-capable shipped models. */
    val ProviderToolCapabilityRefs: List<String> =
        EnvelopeCapabilityRefs + ConversationRef
}

// [VXG RealForever]
