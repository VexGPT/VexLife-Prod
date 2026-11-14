package vexlife.capability.reach

import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import vexlife.contract.VexContractJson

/**
 * Read-only owner of public GitHub repository reach: describe, tree, read.
 *
 * Vocabulary from the accepted provider-independent repository operations (READ / QUERY): describe
 * and tree are QUERY, read is READ. No credential is held here, so only public repositories are
 * reachable and GitHub's unauthenticated rate limit applies; both facts come back in the result
 * instead of being guessed. Writes, pushes, issues and pull requests are not this owner's.
 *
 * Every byte travels through [VexLifeWebReach]'s admitted transport (api.github.com and
 * raw.githubusercontent.com are public hosts), so the same target checks apply.
 */
object VexLifeRepositoryReach {
    const val MaximumTreeEntries = 400

    data class Coordinates(
        val owner: String,
        val name: String,
        val ref: String?,
        val path: String?,
    ) {
        val fullName: String
            get() = "$owner/$name"
    }

    sealed interface Outcome {
        data class Described(
            val repository: String,
            val description: String?,
            val defaultBranch: String,
            val language: String?,
            val pushedAt: String?,
            val visibility: String?,
            val topLevel: List<String>,
        ) : Outcome

        data class Listed(
            val repository: String,
            val ref: String,
            val path: String?,
            val entries: List<String>,
            val totalMatching: Int,
            val truncated: Boolean,
        ) : Outcome

        data class Read(
            val repository: String,
            val ref: String,
            val path: String,
            val text: String,
            val bytes: Int,
            val bytesTruncated: Boolean,
        ) : Outcome

        data class NotFound(val reason: String) : Outcome

        data class Refused(val reason: String) : Outcome

        data class Failed(val reason: String) : Outcome
    }

    private val Segment = Regex("[A-Za-z0-9_.-]+")

    /** Reads `owner/name`, or a github.com URL that may carry `/tree|blob/<ref>/<path>`. */
    fun coordinates(repository: String): Coordinates? {
        val value = repository.trim().removeSuffix("/").removeSuffix(".git")
        if (value.startsWith("http://") || value.startsWith("https://")) {
            val uri = runCatching { URI(value) }.getOrNull() ?: return null
            if (uri.host?.lowercase() != "github.com" && uri.host?.lowercase() != "www.github.com")
                return null
            val parts = uri.path.trim('/').split('/').filter { it.isNotEmpty() }
            if (parts.size < 2 || !parts[0].matches(Segment) || !parts[1].matches(Segment))
                return null
            val ref =
                if (parts.size >= 4 && (parts[2] == "tree" || parts[2] == "blob")) parts[3]
                else null
            val path = if (parts.size >= 5 && ref != null) parts.drop(4).joinToString("/") else null
            return Coordinates(parts[0], parts[1].removeSuffix(".git"), ref, path)
        }
        val parts = value.split('/')
        if (parts.size != 2 || !parts[0].matches(Segment) || !parts[1].matches(Segment)) return null
        return Coordinates(parts[0], parts[1], null, null)
    }

    fun describe(
        repository: String,
        transport: VexLifeWebReach.Transport = VexLifeWebReach.SystemTransport,
        resolver: VexLifeWebReach.Resolver = VexLifeWebReach.SystemResolver,
    ): Outcome {
        val target =
            coordinates(repository)
                ?: return Outcome.Refused("repository must be owner/name or a github.com URL")
        val meta = api(target, "", transport, resolver)
        val repo = (meta as? Api.Json)?.value as? JsonObject ?: return meta.failure()
        val branch = repo.string("default_branch") ?: "HEAD"
        val top =
            when (val tree = api(target, "/git/trees/" + encodePath(branch), transport, resolver)) {
                is Api.Json ->
                    ((tree.value as? JsonObject)?.get("tree") as? JsonArray)
                        ?.mapNotNull { it as? JsonObject }
                        ?.map { entry ->
                            entry.string("path").orEmpty() +
                                if (entry.string("type") == "tree") "/" else ""
                        }
                        .orEmpty()
                else -> emptyList()
            }
        return Outcome.Described(
            repository = repo.string("full_name") ?: target.fullName,
            description = repo.string("description"),
            defaultBranch = branch,
            language = repo.string("language"),
            pushedAt = repo.string("pushed_at"),
            visibility = repo.string("visibility"),
            topLevel = top.sorted().take(MaximumTreeEntries),
        )
    }

    fun tree(
        repository: String,
        ref: String?,
        path: String?,
        transport: VexLifeWebReach.Transport = VexLifeWebReach.SystemTransport,
        resolver: VexLifeWebReach.Resolver = VexLifeWebReach.SystemResolver,
    ): Outcome {
        val target =
            coordinates(repository)
                ?: return Outcome.Refused("repository must be owner/name or a github.com URL")
        val resolvedRef =
            ref?.takeIf { it.isNotBlank() }
                ?: target.ref
                ?: defaultBranch(target, transport, resolver)
                ?: return Outcome.NotFound(
                    "repository ${target.fullName} was not found or is not public"
                )
        val prefix = (path ?: target.path)?.trim('/')?.takeIf { it.isNotEmpty() }
        val tree =
            api(
                target,
                "/git/trees/" + encodePath(resolvedRef) + "?recursive=1",
                transport,
                resolver,
            )
        val value = (tree as? Api.Json)?.value as? JsonObject ?: return tree.failure()
        val all =
            (value["tree"] as? JsonArray)
                ?.mapNotNull { it as? JsonObject }
                ?.mapNotNull { entry ->
                    val entryPath = entry.string("path") ?: return@mapNotNull null
                    if (prefix != null && entryPath != prefix && !entryPath.startsWith("$prefix/"))
                        return@mapNotNull null
                    when (entry.string("type")) {
                        "tree" -> "$entryPath/"
                        "blob" ->
                            entryPath +
                                (entry["size"]?.jsonPrimitive?.longOrNull?.let { "  ($it bytes)" }
                                    ?: "")
                        else -> null
                    }
                }
                .orEmpty()
                .sorted()
        val upstreamTruncated = (value["truncated"] as? JsonPrimitive)?.contentOrNull == "true"
        return Outcome.Listed(
            repository = target.fullName,
            ref = resolvedRef,
            path = prefix,
            entries = all.take(MaximumTreeEntries),
            totalMatching = all.size,
            truncated = upstreamTruncated || all.size > MaximumTreeEntries,
        )
    }

    fun read(
        repository: String,
        path: String,
        ref: String?,
        transport: VexLifeWebReach.Transport = VexLifeWebReach.SystemTransport,
        resolver: VexLifeWebReach.Resolver = VexLifeWebReach.SystemResolver,
    ): Outcome {
        val target =
            coordinates(repository)
                ?: return Outcome.Refused("repository must be owner/name or a github.com URL")
        val filePath = path.trim().trim('/')
        if (
            filePath.isEmpty() ||
                filePath.split('/').any { it == ".." || it == "." || it.isEmpty() }
        ) {
            return Outcome.Refused("path must be a file path inside the repository")
        }
        val resolvedRef = ref?.takeIf { it.isNotBlank() } ?: target.ref ?: "HEAD"
        val url =
            "https://raw.githubusercontent.com/${target.owner}/${target.name}/" +
                encodePath(resolvedRef) +
                "/" +
                encodePath(filePath)
        return when (
            val fetched =
                VexLifeWebReach.fetch(url, transport, resolver, accept = "text/plain,*/*;q=0.5")
        ) {
            is VexLifeWebReach.Outcome.Fetched ->
                Outcome.Read(
                    repository = target.fullName,
                    ref = resolvedRef,
                    path = filePath,
                    // raw.githubusercontent.com serves every file as text/plain; HTML files arrive
                    // verbatim.
                    text = fetched.page.text,
                    bytes = fetched.page.bytesRead,
                    bytesTruncated = fetched.page.bytesTruncated,
                )
            is VexLifeWebReach.Outcome.Failed ->
                if (fetched.status == 404)
                    Outcome.NotFound(
                        "$filePath was not found at $resolvedRef in ${target.fullName}"
                    )
                else Outcome.Failed(fetched.reason)
            is VexLifeWebReach.Outcome.Refused -> Outcome.Refused(fetched.reason)
            is VexLifeWebReach.Outcome.Found -> Outcome.Failed("unexpected search result")
        }
    }

    private fun defaultBranch(
        target: Coordinates,
        transport: VexLifeWebReach.Transport,
        resolver: VexLifeWebReach.Resolver,
    ): String? =
        ((api(target, "", transport, resolver) as? Api.Json)?.value as? JsonObject)?.string(
            "default_branch"
        )

    private sealed interface Api {
        data class Json(val value: kotlinx.serialization.json.JsonElement) : Api

        data class Problem(val outcome: Outcome) : Api
    }

    private fun Api.failure(): Outcome =
        when (this) {
            is Api.Problem -> outcome
            is Api.Json -> Outcome.Failed("GitHub answered with an unexpected shape")
        }

    private fun api(
        target: Coordinates,
        suffix: String,
        transport: VexLifeWebReach.Transport,
        resolver: VexLifeWebReach.Resolver,
    ): Api {
        val url = "https://api.github.com/repos/${target.owner}/${target.name}$suffix"
        return when (
            val fetched =
                VexLifeWebReach.fetch(
                    url,
                    transport,
                    resolver,
                    accept = "application/vnd.github+json",
                    extraHeaders = mapOf("X-GitHub-Api-Version" to "2022-11-28"),
                )
        ) {
            is VexLifeWebReach.Outcome.Fetched ->
                runCatching {
                        Api.Json(VexContractJson.Strict.parseToJsonElement(fetched.page.text))
                    }
                    .getOrElse {
                        Api.Problem(Outcome.Failed("GitHub answered with text that is not JSON"))
                    }
            is VexLifeWebReach.Outcome.Failed ->
                Api.Problem(
                    when (fetched.status) {
                        404 -> Outcome.NotFound("${target.fullName} was not found or is not public")
                        403,
                        429 ->
                            Outcome.Failed(
                                "GitHub's unauthenticated rate limit is used up for now (HTTP ${fetched.status})"
                            )
                        else -> Outcome.Failed(fetched.reason)
                    }
                )
            is VexLifeWebReach.Outcome.Refused -> Api.Problem(Outcome.Refused(fetched.reason))
            is VexLifeWebReach.Outcome.Found ->
                Api.Problem(Outcome.Failed("unexpected search result"))
        }
    }

    private fun JsonObject.string(name: String): String? =
        (this[name] as? JsonPrimitive)?.takeIf { it.isString }?.contentOrNull

    private fun encode(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20")

    /** Branch names and paths keep their slashes; each segment is encoded. */
    private fun encodePath(value: String): String =
        value.split('/').joinToString("/") { encode(it) }
}

// [VXG RealForever]
