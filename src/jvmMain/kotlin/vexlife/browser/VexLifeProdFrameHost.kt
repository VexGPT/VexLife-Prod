package vexlife.browser

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.io.ByteArrayOutputStream
import java.net.InetSocketAddress
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import vexlife.service.VexLifeProdServiceContract

/**
 * Isolated read-only frame host for the first VexLife-Prod nested-browser circuit.
 *
 * Admitted Vex-owned GitHub Pages hosts are projected onto distinct *.vex.localhost origins. Page
 * code therefore never shares the product-service origin. Upstream cookies are neither sent nor
 * returned, and browser form submission is not admitted by this first product boundary.
 */
class VexLifeProdFrameHost private constructor(
    private val server: HttpServer,
    private val executor: ExecutorService,
    private val client: HttpClient,
) : AutoCloseable {
    companion object {
        private const val MaxBodyBytes =
            16 * 1024 * 1024

        private const val RequestTimeoutSeconds =
            20L

        internal const val FrameContentSecurityPolicy =
            "default-src 'self' https://vgong24.github.io https://vexgpt.github.io data: blob:; " +
                "script-src 'self' 'unsafe-inline' https://vgong24.github.io https://vexgpt.github.io; " +
                "style-src 'self' 'unsafe-inline' https://vgong24.github.io https://vexgpt.github.io; " +
                "img-src 'self' https://vgong24.github.io https://vexgpt.github.io data: blob:; " +
                "font-src 'self' https://vgong24.github.io https://vexgpt.github.io data:; " +
                "connect-src 'self'; form-action 'none'; object-src 'none'; frame-src 'self'; base-uri 'none'; " +
                "frame-ancestors 'self' http://127.0.0.1:18210"

        internal fun projectAdmittedAbsoluteUrls(
            value: String,
        ): String =
            VexLifeProdBrowserContract.AdmittedHosts
                .fold(
                    value,
                ) {
                    projected,
                    upstreamHost,
                    ->
                    projected.replace(
                        "https://" +
                            upstreamHost,
                        "http://" +
                            upstreamHost +
                            VexLifeProdBrowserContract.FrameHostSuffix +
                            ":" +
                            VexLifeProdBrowserContract.FrameHostPort,
                    )
                }

        fun start(): VexLifeProdFrameHost =
            startAt(
                InetSocketAddress(
                    "127.0.0.1",
                    VexLifeProdBrowserContract.FrameHostPort,
                ),
            )

        internal fun startAt(
            address: InetSocketAddress,
        ): VexLifeProdFrameHost {
            val executor =
                Executors.newCachedThreadPool()
            val server =
                HttpServer.create(
                    address,
                    0,
                )
            val host =
                VexLifeProdFrameHost(
                    server = server,
                    executor = executor,
                    client =
                        HttpClient
                            .newBuilder()
                            .followRedirects(
                                HttpClient.Redirect.NEVER,
                            )
                            .connectTimeout(
                                Duration.ofSeconds(
                                    RequestTimeoutSeconds,
                                ),
                            )
                            .build(),
                )
            server.executor =
                executor
            server.createContext(
                "/",
            ) {
                exchange ->
                host.handle(
                    exchange,
                )
            }
            try {
                server.start()
            } catch (
                error: Throwable,
            ) {
                server.stop(
                    0,
                )
                executor.shutdownNow()
                throw error
            }

            return host
        }
    }

    val origin: String
        get() =
            "http://127.0.0.1:" +
                server.address.port

    override fun close() {
        server.stop(
            0,
        )
        executor.shutdownNow()
    }

    private fun handle(
        exchange: HttpExchange,
    ) {
        try {
            require(
                exchange.requestMethod ==
                    "GET" ||
                    exchange.requestMethod ==
                    "HEAD",
            ) {
                "VexLife-Prod frame host is read-only"
            }

            val frameHostname =
                requireNotNull(
                    exchange.requestHeaders
                        .getFirst(
                            "Host",
                        ),
                ) {
                    "VexLife-Prod frame host requires a Host header"
                }
                    .substringBefore(
                        ':',
                    )
                    .lowercase()
            val upstreamHost =
                VexLifeProdBrowserContract.admittedUpstreamHost(
                    frameHostname,
                )
            val target =
                buildString {
                    append(
                        "https://",
                    )
                    append(
                        upstreamHost,
                    )
                    append(
                        exchange.requestURI.rawPath
                            .ifEmpty {
                                "/"
                            },
                    )
                    exchange.requestURI.rawQuery
                        ?.let {
                            append(
                                '?',
                            )
                            append(
                                it,
                            )
                        }
                    exchange.requestURI.rawFragment
                        ?.let {
                            append(
                                '#',
                            )
                            append(
                                it,
                            )
                        }
                }
            VexLifeProdBrowserContract.admitPageUrl(
                target,
            )

            val request =
                HttpRequest
                    .newBuilder(
                        URI.create(
                            target,
                        ),
                    )
                    .timeout(
                        Duration.ofSeconds(
                            RequestTimeoutSeconds,
                        ),
                    )
                    .header(
                        "Accept",
                        exchange.requestHeaders
                            .getFirst(
                                "Accept",
                            )
                            ?: "text/html,application/xhtml+xml,*/*;q=0.8",
                    )
                    .header(
                        "Accept-Language",
                        exchange.requestHeaders
                            .getFirst(
                                "Accept-Language",
                            )
                            ?: "en-US,en;q=0.8",
                    )
                    .header(
                        "Accept-Encoding",
                        "identity",
                    )
                    .header(
                        "User-Agent",
                        "VexLife-Prod nested browser",
                    )
                    .method(
                        exchange.requestMethod,
                        HttpRequest.BodyPublishers.noBody(),
                    )
                    .build()
            val response =
                client.send(
                    request,
                    HttpResponse.BodyHandlers.ofInputStream(),
                )
            val status =
                response.statusCode()

            if (
                status in
                    300..399
            ) {
                response.body().close()
                val location =
                    response.headers()
                        .firstValue(
                            "location",
                        )
                        .orElse(
                            null,
                        )
                if (
                    location ==
                        null
                ) {
                    respondText(
                        exchange = exchange,
                        status = 502,
                        text = "Upstream redirect had no location.",
                    )
                    return
                }
                val redirected =
                    URI.create(
                        target,
                    )
                        .resolve(
                            location,
                        )
                        .toString()
                VexLifeProdBrowserContract.admitPageUrl(
                    redirected,
                )
                val local =
                    VexLifeProdBrowserContract.frameUrlFor(
                        redirected,
                    )
                exchange.responseHeaders.add(
                    "Location",
                    local,
                )
                exchange.responseHeaders.add(
                    "Cache-Control",
                    "no-store",
                )
                exchange.sendResponseHeaders(
                    status,
                    -1,
                )
                return
            }

            val contentType =
                response.headers()
                    .firstValue(
                        "content-type",
                    )
                    .orElse(
                        "application/octet-stream",
                    )
            val bytes =
                response.body()
                    .use {
                        readBounded(
                            it,
                            MaxBodyBytes,
                        )
                    }

            if (
                exchange.requestMethod ==
                    "HEAD"
            ) {
                exchange.responseHeaders.add(
                    "Content-Type",
                    contentType,
                )
                exchange.responseHeaders.add(
                    "Cache-Control",
                    "no-store",
                )
                exchange.sendResponseHeaders(
                    status,
                    -1,
                )
                return
            }

            val output =
                when {
                    contentType.contains(
                        "text/html",
                        ignoreCase = true,
                    ) ||
                        contentType.contains(
                            "application/xhtml+xml",
                            ignoreCase = true,
                        ) ->
                        injectReporter(
                            html =
                                bytes.toString(
                                    StandardCharsets.UTF_8,
                                ),
                            sourceUrl = target,
                        )
                            .toByteArray(
                                StandardCharsets.UTF_8,
                            )

                    contentType.contains(
                        "application/json",
                        ignoreCase = true,
                    ) ||
                        contentType.contains(
                            "+json",
                            ignoreCase = true,
                        ) ->
                        projectAdmittedAbsoluteUrls(
                            bytes.toString(
                                StandardCharsets.UTF_8,
                            ),
                        )
                            .toByteArray(
                                StandardCharsets.UTF_8,
                            )

                    else ->
                        bytes
                }

            exchange.responseHeaders.add(
                "Content-Type",
                contentType,
            )
            if (
                contentType.contains(
                    "text/html",
                    ignoreCase = true,
                ) ||
                contentType.contains(
                    "application/xhtml+xml",
                    ignoreCase = true,
                )
            ) {
                exchange.responseHeaders.add(
                    "Content-Security-Policy",
                    FrameContentSecurityPolicy,
                )
            }
            exchange.responseHeaders.add(
                "Cache-Control",
                "no-store",
            )
            exchange.responseHeaders.add(
                "X-Content-Type-Options",
                "nosniff",
            )
            exchange.responseHeaders.add(
                "Referrer-Policy",
                "no-referrer",
            )
            exchange.sendResponseHeaders(
                status,
                output.size.toLong(),
            )
            exchange.responseBody.use {
                it.write(
                    output,
                )
            }
        } catch (
            error: IllegalArgumentException,
        ) {
            respondText(
                exchange = exchange,
                status = 403,
                text = error.message
                    ?: "Frame request is not admitted.",
            )
        } catch (
            _: Throwable,
        ) {
            respondText(
                exchange = exchange,
                status = 502,
                text = "Nested browser page could not be reached.",
            )
        } finally {
            exchange.close()
        }
    }

    private fun injectReporter(
        html: String,
        sourceUrl: String,
    ): String {
        val withoutMetaCsp =
            html.replace(
                Regex(
                    """<meta\\b[^>]*http-equiv\\s*=\\s*[\"']?content-security-policy[^>]*>""",
                    RegexOption.IGNORE_CASE,
                ),
                "",
            )
        val script =
            reporterScript(
                sourceUrl,
            )
        val injection =
            "<script>" +
                script +
                "</script>"
        val head =
            Regex(
                """<head\\b[^>]*>""",
                RegexOption.IGNORE_CASE,
            )
                .find(
                    withoutMetaCsp,
                )

        return if (
            head !=
                null
        ) {
            val at =
                head.range.last +
                    1
            withoutMetaCsp.substring(
                0,
                at,
            ) +
                injection +
                withoutMetaCsp.substring(
                    at,
                )
        } else {
            injection +
                withoutMetaCsp
        }
    }

    private fun reporterScript(
        sourceUrl: String,
    ): String {
        val admittedHosts =
            VexLifeProdBrowserContract.AdmittedHosts
                .joinToString(
                    separator = ",",
                    prefix = "[",
                    postfix = "]",
                ) {
                    jsString(
                        it,
                    )
                }

        return """
            (() => {
              const SOURCE_URL = ${jsString(sourceUrl)};
              const PARENT_ORIGIN = ${jsString(VexLifeProdServiceContract.Origin)};
              const FRAME_SUFFIX = ${jsString(VexLifeProdBrowserContract.FrameHostSuffix)};
              const FRAME_PORT = ${VexLifeProdBrowserContract.FrameHostPort};
              const ADMITTED_HOSTS = $admittedHosts;
              const MESSAGE_TYPE = 'vexlife.browser-context/v1';
              const TEXT_LIMIT = ${VexLifeProdBrowserContract.RenderedTextLimit};

              const frameUrlFor = (target) => {
                const url = new URL(target);
                if (
                  url.protocol === 'http:' &&
                  url.port === String(FRAME_PORT) &&
                  url.hostname.endsWith(FRAME_SUFFIX)
                ) {
                  const upstreamHost = url.hostname.slice(0, -FRAME_SUFFIX.length);
                  return ADMITTED_HOSTS.includes(upstreamHost) ? url.href : null;
                }
                if (url.protocol !== 'https:' || url.port || !ADMITTED_HOSTS.includes(url.hostname)) return null;
                return 'http://' + url.hostname + FRAME_SUFFIX + ':' + FRAME_PORT + url.pathname + url.search + url.hash;
              };

              let reportTimer = null;
              const currentPageUrl = () => {
                if (!location.hostname.endsWith(FRAME_SUFFIX)) return SOURCE_URL;
                const upstreamHost = location.hostname.slice(0, -FRAME_SUFFIX.length);
                if (!ADMITTED_HOSTS.includes(upstreamHost)) return SOURCE_URL;
                return 'https://' + upstreamHost + location.pathname + location.search + location.hash;
              };

              const report = () => {
                reportTimer = null;
                const text = (document.body?.innerText || '').slice(0, TEXT_LIMIT);
                parent.postMessage({
                  type: MESSAGE_TYPE,
                  pageUrl: currentPageUrl(),
                  title: document.title || null,
                  text,
                  capture: 'RENDERED'
                }, PARENT_ORIGIN);
              };
              const scheduleReport = () => {
                if (reportTimer !== null) clearTimeout(reportTimer);
                reportTimer = setTimeout(report, 120);
              };

              document.addEventListener('click', (event) => {
                const element = event.target instanceof Element ? event.target : null;
                const anchor = element?.closest('a[href]');
                if (!anchor) return;
                const raw = anchor.getAttribute('href');
                if (!raw || raw.startsWith('#')) return;
                let target;
                try {
                  target = new URL(raw, currentPageUrl()).href;
                } catch {
                  event.preventDefault();
                  return;
                }
                const framed = frameUrlFor(target);
                event.preventDefault();
                if (framed) window.location.href = framed;
              }, true);

              document.addEventListener('submit', (event) => {
                event.preventDefault();
              }, true);

              window.addEventListener('hashchange', scheduleReport);
              if (document.readyState === 'loading') {
                document.addEventListener('DOMContentLoaded', () => {
                  report();
                  new MutationObserver(scheduleReport).observe(document.documentElement, {
                    subtree: true,
                    childList: true,
                    characterData: true
                  });
                }, { once: true });
              } else {
                report();
                new MutationObserver(scheduleReport).observe(document.documentElement, {
                  subtree: true,
                  childList: true,
                  characterData: true
                });
              }
            })();
        """.trimIndent()
    }

    private fun respondText(
        exchange: HttpExchange,
        status: Int,
        text: String,
    ) {
        val bytes =
            (text +
                "\n")
                .toByteArray(
                    StandardCharsets.UTF_8,
                )
        if (
            !exchange.responseHeaders
                .containsKey(
                    "Content-Type",
                )
        ) {
            exchange.responseHeaders.add(
                "Content-Type",
                "text/plain; charset=utf-8",
            )
        }
        exchange.responseHeaders.add(
            "Cache-Control",
            "no-store",
        )
        exchange.sendResponseHeaders(
            status,
            bytes.size.toLong(),
        )
        exchange.responseBody.use {
            it.write(
                bytes,
            )
        }
    }

    private fun readBounded(
        input: java.io.InputStream,
        limit: Int,
    ): ByteArray {
        val output =
            ByteArrayOutputStream()
        val buffer =
            ByteArray(
                64 * 1024,
            )
        while (
            true
        ) {
            val count =
                input.read(
                    buffer,
                )
            if (
                count <
                    0
            ) {
                break
            }
            require(
                output.size() +
                    count <=
                    limit,
            ) {
                "nested browser response exceeds the admitted body limit"
            }
            output.write(
                buffer,
                0,
                count,
            )
        }

        return output.toByteArray()
    }

    private fun jsString(
        value: String,
    ): String =
        "\"" +
            value
                .replace(
                    "\\",
                    "\\\\",
                )
                .replace(
                    "\"",
                    "\\\"",
                )
                .replace(
                    "\n",
                    "\\n",
                )
                .replace(
                    "\r",
                    "\\r",
                ) +
            "\""
}

// [VXG RealForever]
