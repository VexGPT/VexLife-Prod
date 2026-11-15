package vexlife.capability.reach

import java.io.ByteArrayOutputStream
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.time.Duration

/**
 * Read-only owner of public web reach: fetch one URL, or search.
 *
 * Owns: target admission (public http(s) hosts only, re-checked on every redirect hop), bounded
 * bytes, readable-text extraction. Owns no credential, cookie, form submission, upload or write;
 * every request is an unauthenticated GET. The text it returns was written by other people and is
 * marked untrusted by the bridge.
 *
 * Why the public-address check exists: this process runs on Victor's Mac beside loopback services
 * (the Product service, the Heretic runtime) and his LAN. A URL Vex chooses must never become a way
 * to reach those. Checked: loopback, link-local, site-local/private, CGNAT, multicast, unspecified,
 * IPv6 unique-local. Known limit: a DNS answer can change between this check and the connection.
 */
object VexLifeWebReach {
    const val MaximumResponseBytes = 2 * 1024 * 1024
    const val MaximumRedirects = 5
    const val SearchProviderRef = "search-provider.duckduckgo-html"
    private const val UserAgent = "VexLife-Prod/1 (+https://github.com/VexGPT/VexLife-Prod)"

    data class Request(val uri: URI, val headers: Map<String, String>)

    data class Response(
        val status: Int,
        val headers: Map<String, List<String>>,
        val body: ByteArray,
        val truncated: Boolean,
    ) {
        fun header(name: String): String? =
            headers.entries
                .firstOrNull { it.key.equals(name, ignoreCase = true) }
                ?.value
                ?.firstOrNull()
    }

    /** One unauthenticated GET without following redirects; the caller follows them. */
    fun interface Transport {
        fun get(request: Request, maxBytes: Int): Response
    }

    fun interface Resolver {
        fun resolve(host: String): List<InetAddress>
    }

    object SystemResolver : Resolver {
        override fun resolve(host: String): List<InetAddress> =
            InetAddress.getAllByName(host).toList()
    }

    object SystemTransport : Transport {
        private val client: HttpClient by lazy {
            HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .connectTimeout(Duration.ofSeconds(10))
                .build()
        }

        override fun get(request: Request, maxBytes: Int): Response {
            val builder =
                HttpRequest.newBuilder(request.uri)
                    .timeout(Duration.ofSeconds(25))
                    .GET()
                    .header("User-Agent", UserAgent)
            request.headers.forEach { (name, value) -> builder.header(name, value) }
            val response = client.send(builder.build(), HttpResponse.BodyHandlers.ofInputStream())
            val output = ByteArrayOutputStream()
            var truncated = false
            response.body().use { input ->
                val buffer = ByteArray(16 * 1024)
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    val room = maxBytes - output.size()
                    if (read > room) {
                        output.write(buffer, 0, room.coerceAtLeast(0))
                        truncated = true
                        break
                    }
                    output.write(buffer, 0, read)
                }
            }
            return Response(
                response.statusCode(),
                response.headers().map(),
                output.toByteArray(),
                truncated,
            )
        }
    }

    data class Page(
        val requestedUrl: String,
        val finalUrl: String,
        val status: Int,
        val contentType: String,
        val title: String?,
        val text: String,
        val bytesRead: Int,
        val bytesTruncated: Boolean,
        val redirects: List<String>,
    )

    data class SearchResult(val title: String, val url: String, val snippet: String)

    sealed interface Outcome {
        data class Fetched(val page: Page) : Outcome

        data class Found(
            val query: String,
            val results: List<SearchResult>,
            val providerRef: String,
        ) : Outcome

        /** The target was not admitted (not public, not http(s), unsupported content). */
        data class Refused(val reason: String) : Outcome

        /** The target was admitted but reading it did not succeed. */
        data class Failed(val reason: String, val status: Int? = null) : Outcome
    }

    fun fetch(
        url: String,
        transport: Transport = SystemTransport,
        resolver: Resolver = SystemResolver,
        accept: String =
            "text/html,text/plain,text/markdown,application/json,application/xml;q=0.9,*/*;q=0.1",
        extraHeaders: Map<String, String> = emptyMap(),
    ): Outcome {
        var current =
            runCatching { URI(url.trim()).normalize() }.getOrNull()
                ?: return Outcome.Refused("not a valid URL")
        val redirects = mutableListOf<String>()
        repeat(MaximumRedirects + 1) {
            admit(current, resolver)?.let {
                return Outcome.Refused(it)
            }
            val response =
                try {
                    transport.get(
                        Request(current, mapOf("Accept" to accept) + extraHeaders),
                        MaximumResponseBytes,
                    )
                } catch (error: Exception) {
                    return Outcome.Failed(
                        "could not read the page: " + (error.message ?: error.javaClass.simpleName)
                    )
                }
            if (response.status in 300..399) {
                val location =
                    response.header("location")
                        ?: return Outcome.Failed("redirect without a location", response.status)
                val next =
                    runCatching { current.resolve(location.trim()).normalize() }.getOrNull()
                        ?: return Outcome.Failed(
                            "redirect to an unreadable location",
                            response.status,
                        )
                redirects += next.toString()
                current = next
                return@repeat
            }
            if (response.status !in 200..299) {
                return Outcome.Failed(
                    "the server answered HTTP ${response.status}",
                    response.status,
                )
            }
            val contentType =
                response.header("content-type")?.lowercase() ?: "application/octet-stream"
            val charset = charsetOf(contentType)
            val kind = contentType.substringBefore(';').trim()
            val text =
                when {
                    kind == "text/html" || kind == "application/xhtml+xml" ->
                        VexLifeHtmlText.extract(String(response.body, charset), current)
                    kind.startsWith("text/") ||
                        kind in TextualTypes ||
                        kind.endsWith("+json") ||
                        kind.endsWith("+xml") ->
                        VexLifeHtmlText.Extracted(
                            title = null,
                            text = String(response.body, charset),
                        )
                    looksTextual(response.body) ->
                        VexLifeHtmlText.Extracted(
                            title = null,
                            text = String(response.body, StandardCharsets.UTF_8),
                        )
                    else -> return Outcome.Refused("content type $kind is not readable text")
                }
            return Outcome.Fetched(
                Page(
                    requestedUrl = url,
                    finalUrl = current.toString(),
                    status = response.status,
                    contentType = kind,
                    title = text.title,
                    text = text.text,
                    bytesRead = response.body.size,
                    bytesTruncated = response.truncated,
                    redirects = redirects,
                )
            )
        }
        return Outcome.Failed("more than $MaximumRedirects redirects")
    }

    /**
     * Searches through DuckDuckGo's HTML endpoint, which needs no account or key. It is a public
     * page shaped for people, so a change of shape or a bot check returns [Outcome.Failed] rather
     * than invented results.
     */
    fun search(
        query: String,
        transport: Transport = SystemTransport,
        resolver: Resolver = SystemResolver,
    ): Outcome {
        val trimmed = query.trim()
        if (trimmed.isEmpty() || trimmed.length > 400)
            return Outcome.Refused("the query must be 1-400 characters")
        val uri =
            URI(
                "https://html.duckduckgo.com/html/?q=" +
                    URLEncoder.encode(trimmed, StandardCharsets.UTF_8)
            )
        admit(uri, resolver)?.let {
            return Outcome.Refused(it)
        }
        val response =
            try {
                transport.get(Request(uri, mapOf("Accept" to "text/html")), MaximumResponseBytes)
            } catch (error: Exception) {
                return Outcome.Failed(
                    "could not reach the search provider: " +
                        (error.message ?: error.javaClass.simpleName)
                )
            }
        if (response.status != 200)
            return Outcome.Failed(
                "the search provider answered HTTP ${response.status}",
                response.status,
            )
        val html =
            String(response.body, charsetOf(response.header("content-type")?.lowercase() ?: ""))
        val results = parseDuckDuckGo(html)
        if (results.isEmpty() && !html.contains("No results", ignoreCase = true)) {
            return Outcome.Failed("the search provider returned a page without readable results")
        }
        return Outcome.Found(query = trimmed, results = results, providerRef = SearchProviderRef)
    }

    internal fun parseDuckDuckGo(html: String): List<SearchResult> {
        val anchors =
            Regex(
                """<a[^>]*class="[^"]*result__a[^"]*"[^>]*href="([^"]+)"[^>]*>(.*?)</a>""",
                RegexOption.DOT_MATCHES_ALL,
            )
        val snippets =
            Regex(
                """class="[^"]*result__snippet[^"]*"[^>]*>(.*?)</(?:a|div|td)>""",
                RegexOption.DOT_MATCHES_ALL,
            )
        val snippetTexts =
            snippets.findAll(html).map { VexLifeHtmlText.inline(it.groupValues[1]) }.toList()
        return anchors
            .findAll(html)
            .mapIndexedNotNull { index, match ->
                val url =
                    resultUrl(VexLifeHtmlText.decodeEntities(match.groupValues[1]))
                        ?: return@mapIndexedNotNull null
                SearchResult(
                    title = VexLifeHtmlText.inline(match.groupValues[2]),
                    url = url,
                    snippet = snippetTexts.getOrElse(index) { "" },
                )
            }
            .distinctBy { it.url }
            .take(10)
            .toList()
    }

    private fun resultUrl(href: String): String? {
        val absolute = if (href.startsWith("//")) "https:$href" else href
        val uri = runCatching { URI(absolute) }.getOrNull() ?: return null
        if (uri.host?.endsWith("duckduckgo.com") == true && uri.path == "/l/") {
            val target =
                uri.rawQuery
                    ?.split('&')
                    ?.firstOrNull { it.startsWith("uddg=") }
                    ?.removePrefix("uddg=")
                    ?.let { URLDecoder.decode(it, StandardCharsets.UTF_8) }
            return target?.takeIf { it.startsWith("http://") || it.startsWith("https://") }
        }
        if (uri.host?.endsWith("duckduckgo.com") == true) return null
        return absolute.takeIf { it.startsWith("http://") || it.startsWith("https://") }
    }

    /** Null when [uri] may be read; otherwise why not. */
    fun admit(uri: URI, resolver: Resolver = SystemResolver): String? {
        val scheme = uri.scheme?.lowercase()
        if (scheme != "http" && scheme != "https") return "only http and https URLs can be read"
        if (uri.rawUserInfo != null) return "URLs carrying credentials are not read"
        val host = uri.host?.trimEnd('.')?.lowercase() ?: return "the URL has no host"
        if (uri.port != -1 && uri.port != 80 && uri.port != 443)
            return "only the standard web ports are read"
        if (
            host == "localhost" ||
                host.endsWith(".localhost") ||
                host.endsWith(".local") ||
                host.endsWith(".internal")
        ) {
            return "local network hosts are not read"
        }
        val addresses =
            try {
                resolver.resolve(host)
            } catch (_: Exception) {
                return "the host name does not resolve"
            }
        if (addresses.isEmpty()) return "the host name does not resolve"
        if (addresses.any { !isPublic(it) })
            return "the host resolves to a private or local address"
        return null
    }

    internal fun isPublic(address: InetAddress): Boolean {
        if (
            address.isLoopbackAddress ||
                address.isAnyLocalAddress ||
                address.isLinkLocalAddress ||
                address.isSiteLocalAddress ||
                address.isMulticastAddress
        ) {
            return false
        }
        val bytes = address.address.map { it.toInt() and 0xff }
        return when (address) {
            is Inet4Address ->
                !(bytes[0] == 0 ||
                    bytes[0] == 10 ||
                    (bytes[0] == 100 && bytes[1] in 64..127) ||
                    (bytes[0] == 169 && bytes[1] == 254) ||
                    (bytes[0] == 172 && bytes[1] in 16..31) ||
                    (bytes[0] == 192 && bytes[1] == 168) ||
                    (bytes[0] == 198 && bytes[1] in 18..19) ||
                    bytes[0] >= 224)
            is Inet6Address ->
                (bytes[0] and 0xfe) != 0xfc && !(bytes[0] == 0xfe && (bytes[1] and 0xc0) == 0x80)
            else -> false
        }
    }

    private val TextualTypes =
        setOf(
            "application/json",
            "application/xml",
            "application/javascript",
            "application/x-yaml",
            "application/yaml",
            "application/toml",
        )

    private fun charsetOf(contentType: String): Charset =
        Regex("charset=([A-Za-z0-9._-]+)").find(contentType)?.groupValues?.get(1)?.let {
            runCatching { Charset.forName(it) }.getOrNull()
        } ?: StandardCharsets.UTF_8

    private fun looksTextual(body: ByteArray): Boolean {
        if (body.isEmpty()) return true
        // NUL bytes do not occur in text encodings this reader decodes; they do in images, archives
        // and PDFs.
        return body.take(4096).none { it == 0.toByte() }
    }
}

// [VXG RealForever]
